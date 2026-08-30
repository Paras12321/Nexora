import os
from pathlib import Path
from typing import Any, Dict, Optional

from ai.models import AIAnalysisResult
from ai.policy import AIActionPolicy
from ai.providers import AIProvider, MockAIProvider, UnifiedAIResult


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
    """Provider-backed AI orchestration service."""

    def __init__(self, provider: Optional[AIProvider] = None, prompt_manager: Optional[PromptTemplateManager] = None):
        self.provider = provider or self._build_default_provider()
        self.prompt_manager = prompt_manager or PromptTemplateManager()

    @staticmethod
    def _build_default_provider() -> AIProvider:
        provider_name = os.getenv("AI_PROVIDER", "mock").lower()
        if provider_name == "mock":
            return MockAIProvider()
        return MockAIProvider()

    def analyze(self, prompt_type: str, context: Optional[Dict[str, Any]] = None):
        context = context or {}
        try:
            prompt = self.prompt_manager.render(prompt_type, context)
            result = self._invoke_with_retries(prompt_type, prompt, self.prompt_manager.schema_for(prompt_type))
            return self._persist_result(prompt_type, context, result)
        except Exception as exc:  # pragma: no cover - safety net for provider failures
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

    def _invoke_with_retries(self, prompt_type: str, prompt: str, schema: Dict[str, Any]):
        last_error = None
        for attempt in range(self.provider.max_retries + 1):
            try:
                return self.provider.generate(prompt_type=prompt_type, prompt=prompt, schema=schema)
            except Exception as exc:  # pragma: no cover - exact behavior covered by tests via direct error
                last_error = exc
                if attempt >= self.provider.max_retries:
                    raise
        raise last_error

    def _persist_result(self, prompt_type: str, context: Dict[str, Any], result: UnifiedAIResult, failed: bool = False):
        home = None
        home_id = context.get("home_id")
        if home_id is not None:
            from core.models import Home
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
            from django.utils import timezone
            from core.models import DecisionLog

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
