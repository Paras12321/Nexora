"""AI service orchestration.

This module coordinates the complete AI pipeline:
1. Authenticate user
2. Build authorized context
3. Validate request
4. Call provider
5. Validate response
6. Apply policy/safety checks
7. Log decision
8. Return stable result
"""

import os
from pathlib import Path
from typing import Any, Dict, Optional

from django.utils import timezone

from core.models import Home, DecisionLog, ActivityLog
from ai.models import AIAnalysisResult
from ai.context import ContextBuilder
from ai.exceptions import (
    ContextBuildingError,
    UnauthorizedContextAccess,
    ProviderUnavailableError,
    InvalidAIResponseError,
)
from ai.policy import AIActionPolicy
from ai.providers import AIProvider, UnifiedAIResult, get_provider
from ai.schemas import StructuredAIResponse, PolicyStatus


class PromptTemplateManager:
    """Load versioned prompt templates from the repository."""

    TEMPLATE_ROOT = Path(__file__).resolve().parent / "templates" / "prompts"

    def load(self, prompt_type: str) -> str:
        template_path = self.TEMPLATE_ROOT / f"{prompt_type}_v1.txt"
        if not template_path.exists():
            raise ValueError(f"Prompt template not found for {prompt_type}")
        return template_path.read_text(encoding="utf-8")

    def render(self, prompt_type: str, context: Dict[str, Any]) -> str:
        template = self.load(prompt_type)
        safe_context = self._sanitize_context(context)
        return template.format(**safe_context)

    @staticmethod
    def _sanitize_context(context: Dict[str, Any]) -> Dict[str, Any]:
        sensitive_keys = {"password", "secret", "token", "api_key", "client_secret", "oauth", "email", "phone"}
        cleaned = {}
        for key, value in (context or {}).items():
            normalized = str(key).lower()
            if any(blocker in normalized for blocker in sensitive_keys):
                cleaned[key] = "[redacted]"
                continue
            cleaned[key] = value
        return cleaned

    @staticmethod
    def schema_for(prompt_type: str) -> Dict[str, Any]:
        return {
            "type": "object",
            "required": ["content", "status"],
            "properties": {
                "content": {"type": "string"},
                "status": {"type": "string", "enum": ["ok", "error"]},
                "decision": {"type": ["string", "null"]},
                "requires_approval": {"type": "boolean"},
                "confidence": {"type": "number"},
            },
        }


class AIService:
    """
    Central AI orchestration service.

    Coordinates authentication, context building, provider selection,
    response validation, policy enforcement, and logging.
    """

    def __init__(self, provider: Optional[AIProvider] = None, prompt_manager: Optional[PromptTemplateManager] = None):
        self.provider = provider or self._build_default_provider()
        self.prompt_manager = prompt_manager or PromptTemplateManager()

    @staticmethod
    def _build_default_provider() -> AIProvider:
        """Build the configured AI provider (default: mock)."""
        return get_provider()

    def analyze_message(
        self,
        user_id: int,
        message: str,
        home_id: Optional[int] = None,
    ) -> StructuredAIResponse:
        """
        Analyze a natural language message from an authenticated user.

        This is the primary API for handling user AI requests.

        Args:
            user_id: Authenticated user ID
            message: Natural language query/command
            home_id: Optional home to scope request to

        Returns:
            StructuredAIResponse with intent, entities, actions, confidence

        Raises:
            ContextBuildingError: Cannot build authorized context
            UnauthorizedContextAccess: User cannot access the home
        """
        # 1. Build authorized context
        try:
            context = ContextBuilder.build(user_id, home_id)
        except (ContextBuildingError, UnauthorizedContextAccess) as exc:
            return self._error_response(str(exc))

        # 2. Call provider
        try:
            response = self.provider.analyze_message(
                message,
                context.to_dict() if hasattr(context, 'to_dict') else {
                    'user_id': context.user_id,
                    'home_id': context.home_id,
                    'devices': context.devices,
                    'rooms': context.rooms,
                    'security_state': context.security_state,
                    'presence_state': context.presence_state,
                }
            )
        except Exception as exc:
            return self._error_response(f"AI service unavailable: {str(exc)}")

        # 3. Apply policy/safety checks
        response = self._apply_policy(response, context.home_id)

        # 4. Persist result
        self._persist_analysis(user_id, context.home_id, message, response)

        return response

    def analyze(self, prompt_type: str, context: Optional[Dict[str, Any]] = None) -> UnifiedAIResult:
        """
        Analyze using template-based prompt (legacy energy analysis).

        This maintains backward compatibility with existing prompt templates.
        """
        context = context or {}
        try:
            prompt = self.prompt_manager.render(prompt_type, context)
            result = self._invoke_with_retries(prompt_type, prompt, self.prompt_manager.schema_for(prompt_type))
            return self._persist_result(prompt_type, context, result)
        except Exception as exc:
            safe_result = UnifiedAIResult(
                prompt_type=prompt_type,
                content="AI analysis is temporarily unavailable. Please try again in a moment.",
                status="error",
                metadata={
                    "provider": self.provider.name,
                    "error_type": type(exc).__name__,
                    "safe_failure": True,
                },
            )
            return self._persist_result(prompt_type, context, safe_result, failed=True)

    def _invoke_with_retries(self, prompt_type: str, prompt: str, schema: Dict[str, Any]) -> UnifiedAIResult:
        """Invoke provider with retry logic."""
        last_error = None
        for attempt in range(self.provider.max_retries + 1):
            try:
                return self.provider.generate(prompt_type=prompt_type, prompt=prompt, schema=schema)
            except Exception as exc:
                last_error = exc
                if attempt >= self.provider.max_retries:
                    raise
        raise last_error

    def _apply_policy(self, response: StructuredAIResponse, home_id: Optional[int]) -> StructuredAIResponse:
        """Apply policy/safety layer to AI response."""
        # If response suggests sensitive actions, require confirmation
        sensitive_actions = {"lock", "unlock", "disarm", "arm"}
        for action in response.proposed_actions:
            if action.action_type.value in sensitive_actions:
                response.policy_status = PolicyStatus.REQUIRES_CONFIRMATION
                response.requires_confirmation = True

        # Validate policy for each proposed action
        if response.proposed_actions:
            for action in response.proposed_actions:
                # Safety check: ensure action is valid
                if not action.device_id and action.action_type.value != "no_action":
                    response.policy_status = PolicyStatus.REJECTED
                    response.message = "Could not identify the target device. Please specify which device you want to control."

        return response

    def _persist_analysis(
        self,
        user_id: int,
        home_id: Optional[int],
        message: str,
        response: StructuredAIResponse
    ) -> None:
        """Persist AI analysis to database."""
        home = None
        if home_id:
            try:
                home = Home.objects.get(id=home_id)
            except Home.DoesNotExist:
                pass

        analysis = AIAnalysisResult.objects.create(
            prompt_type="message_analysis",
            provider=response.provider,
            status="ok",
            home=home,
            request_payload={
                "user_id": user_id,
                "message": message,
                "home_id": home_id,
            },
            response_payload=response.to_dict(),
        )

        # If response requires confirmation and proposes actions, create decision log
        if response.requires_confirmation and response.proposed_actions and home:
            action_names = ", ".join(a.action_type.value for a in response.proposed_actions)
            decision_log = DecisionLog.objects.create(
                home=home,
                source="ai:message",
                decision=action_names,
                reason=response.message,
                status=DecisionLog.Status.PENDING,
                timestamp=timezone.now(),
            )
            analysis.decision_log = decision_log
            analysis.save(update_fields=["decision_log"])

    def _persist_result(
        self,
        prompt_type: str,
        context: Dict[str, Any],
        result: UnifiedAIResult,
        failed: bool = False
    ) -> UnifiedAIResult:
        """Persist template-based analysis result (legacy)."""
        home = None
        home_id = context.get("home_id")
        if home_id is not None:
            try:
                home = Home.objects.get(pk=home_id)
            except Home.DoesNotExist:
                home = None

        analysis = AIAnalysisResult.objects.create(
            prompt_type=prompt_type,
            provider=self.provider.name,
            status=result.status,
            home=home,
            request_payload={"context": context},
            response_payload=result.as_dict(),
        )

        if result.decision and result.requires_approval and home is not None:
            policy_ok = AIActionPolicy.validate(result.decision, result.requires_approval)
            if not policy_ok:
                result.status = "error"
                result.content = "This recommendation requires explicit approval before any consequential action can be taken."
                result.requires_approval = False
                result.metadata["policy_blocked"] = True
            else:
                decision_log = DecisionLog.objects.create(
                    home=home,
                    source=f"ai:{self.provider.name}",
                    decision=result.decision,
                    reason=result.content,
                    status=DecisionLog.Status.PENDING,
                    timestamp=timezone.now(),
                )
                analysis.decision_log = decision_log
                analysis.save(update_fields=["decision_log"])

        return result

    def _error_response(self, error_message: str) -> StructuredAIResponse:
        """Create a safe error response."""
        from ai.schemas import IntentType
        return StructuredAIResponse(
            message=error_message or "An error occurred while processing your request. Please try again.",
            intent=IntentType.UNSUPPORTED,
            confidence=0.0,
            entities=[],
            proposed_actions=[],
            policy_status=PolicyStatus.INFORMATIONAL,
            requires_confirmation=False,
            provider=self.provider.name,
        )
