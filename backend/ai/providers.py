import os
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Any, Dict, Optional

from ai.schemas import StructuredAIResponse, IntentType, PolicyStatus, ActionType, Entity, ProposedAction


@dataclass
class UnifiedAIResult:
    """
    Legacy unified result format for backward compatibility with existing prompt templates.
    New implementations should use StructuredAIResponse from schemas.py.
    """
    prompt_type: str
    content: str
    status: str = "ok"
    metadata: Dict[str, Any] = field(default_factory=dict)
    decision: Optional[str] = None
    requires_approval: bool = False
    confidence: float = 0.0

    def as_dict(self):
        return {
            "prompt_type": self.prompt_type,
            "status": self.status,
            "content": self.content,
            "metadata": self.metadata,
            "decision": self.decision,
            "requires_approval": self.requires_approval,
            "confidence": self.confidence,
        }


class AIProvider(ABC):
    """
    Abstract base class for AI providers.

    The provider interface is the contract between Django and the LLM backend.
    All LLM calls go through this interface, allowing providers to be swapped
    without modifying the rest of the application.

    To implement a new provider (e.g., Gemini, OpenAI):
    1. Create a new file: ai/providers/{provider_name}.py
    2. Implement a class inheriting from AIProvider
    3. Implement the generate() and analyze_message() methods
    4. Set environment variable: AI_PROVIDER={provider_name}

    The service layer will automatically select the provider based on AI_PROVIDER env var.
    """

    name = "base"
    timeout_seconds = 15
    max_retries = 2

    @abstractmethod
    def generate(self, prompt_type: str, prompt: str, schema: Dict[str, Any]) -> UnifiedAIResult:
        """
        Generate AI output for a versioned prompt template.

        Args:
            prompt_type: e.g., "bill_analysis", "energy_explanation"
            prompt: Rendered prompt text to send to LLM
            schema: JSON schema for validation

        Returns:
            UnifiedAIResult with content, decision, confidence
        """
        raise NotImplementedError

    @abstractmethod
    def analyze_message(self, message: str, context: Dict[str, Any]) -> StructuredAIResponse:
        """
        Analyze a natural language user message with structured context.

        This is the primary method for handling general AI queries from the app.

        Args:
            message: Natural language query from user
            context: AIRequestContext as dict (user_id, home_id, devices, rooms, etc.)

        Returns:
            StructuredAIResponse with intent, entities, proposed actions, confidence
        """
        raise NotImplementedError


class MockAIProvider(AIProvider):
    """
    Default mock provider used for local development and testing without LLM credentials.

    This provider returns deterministic, realistic responses for representative requests.
    It is NOT a real LLM—it is a finite state machine designed for development/testing.

    Covers scenarios like:
    - "Turn off the bedroom light"
    - "What devices are currently on?"
    - "Turn on the living room light"
    - "I'm going to sleep"
    - Informational questions
    - Ambiguous/unsupported requests
    """

    name = "mock"

    def generate(self, prompt_type: str, prompt: str, schema: Dict[str, Any]) -> UnifiedAIResult:
        """
        Generate response for template-based prompt.
        Used for legacy energy analysis and reporting.
        """
        default_responses = {
            "bill_analysis": {
                "content": "Your August bill is 22% higher than your recent average. The increase is primarily due to HVAC runtime and evening appliance use. Recommendation: reduce non-essential cooling and evening appliance loads.",
                "decision": "recommend_energy_savings",
                "requires_approval": True,
                "confidence": 0.91,
            },
            "energy_explanation": {
                "content": "Peak energy use was concentrated in the late afternoon and evening when the AC and kitchen loads overlapped.",
                "decision": None,
                "requires_approval": False,
                "confidence": 0.89,
            },
            "anomaly_explanation": {
                "content": "The reading spike was unusual for this home and likely reflects a temporary device cycle rather than a persistent issue.",
                "decision": None,
                "requires_approval": False,
                "confidence": 0.84,
            },
            "automation_recommendation": {
                "content": "Consider turning off non-essential loads when no one is home and pre-cooling the house before arrival.",
                "decision": "arm_home_optimization",
                "requires_approval": True,
                "confidence": 0.88,
            },
            "home_insights": {
                "content": "This week shows a stable baseline, with a few efficiency opportunities in evening cooling and laundry timing.",
                "decision": None,
                "requires_approval": False,
                "confidence": 0.86,
            },
        }

        payload = default_responses.get(prompt_type, {
            "content": "No insight available for this request.",
            "decision": None,
            "requires_approval": False,
            "confidence": 0.0,
        })

        return UnifiedAIResult(
            prompt_type=prompt_type,
            content=payload["content"],
            status="ok",
            metadata={
                "provider": self.name,
                "schema_version": "v1",
                "prompt_length": len(prompt),
            },
            decision=payload.get("decision"),
            requires_approval=bool(payload.get("requires_approval")),
            confidence=float(payload.get("confidence", 0.0)),
        )

    def analyze_message(self, message: str, context: Dict[str, Any]) -> StructuredAIResponse:
        """
        Analyze a natural language message.
        Mock provider uses pattern matching to simulate intent recognition.
        """
        msg_lower = message.lower().strip()

        # Pattern-based intent detection
        if any(phrase in msg_lower for phrase in ["turn on", "turn off", "switch", "light"]):
            return self._handle_device_control(msg_lower, context)
        elif any(phrase in msg_lower for phrase in ["what ", "how many", "which", "status", "are on"]):
            return self._handle_information_query(msg_lower, context)
        elif any(phrase in msg_lower for phrase in ["sleep", "away", "home", "leaving", "arriving"]):
            return self._handle_home_mode(msg_lower, context)
        else:
            return self._handle_unsupported(msg_lower, context)

    def _handle_device_control(self, message: str, context: Dict[str, Any]) -> StructuredAIResponse:
        """Handle device control requests (turn on/off, set level, etc.)"""
        devices = context.get("devices", [])

        # Extract action
        action = "turn_off" if "off" in message else "turn_on"
        action_type = ActionType.TURN_OFF if "off" in message else ActionType.TURN_ON

        # Try to identify device
        device_name = None
        device_id = None
        for keyword in ["bedroom", "living room", "kitchen", "bathroom", "light", "lamp"]:
            if keyword in message:
                device_name = keyword
                # Try to find matching device in context
                for dev in devices:
                    if keyword.lower() in dev.get("name", "").lower() or keyword.lower() in dev.get("room_name", "").lower():
                        device_id = dev["id"]
                        break
                break

        if device_id:
            proposed_action = ProposedAction(
                action_type=action_type,
                device_id=device_id,
                reason=f"User requested to {action} this device."
            )
            message_text = f"I'll turn {action.split('_')[1]} the {device_name} for you."
            policy_status = PolicyStatus.REQUIRES_CONFIRMATION
        else:
            message_text = "I found your request to control a device, but I couldn't identify which one. Could you specify the device or room?"
            proposed_action = ProposedAction(action_type=ActionType.NO_ACTION)
            policy_status = PolicyStatus.INFORMATIONAL

        return StructuredAIResponse(
            message=message_text,
            intent=IntentType.DEVICE_CONTROL,
            confidence=0.87,
            entities=[Entity(type="action", name=action)],
            proposed_actions=[proposed_action],
            policy_status=policy_status,
            requires_confirmation=(policy_status == PolicyStatus.REQUIRES_CONFIRMATION),
            provider=self.name,
        )

    def _handle_information_query(self, message: str, context: Dict[str, Any]) -> StructuredAIResponse:
        """Handle information queries (what devices are on, status, etc.)"""
        devices = context.get("devices", [])
        on_devices = [d for d in devices if d.get("status", "").lower() == "on"]

        if "on" in message and on_devices:
            device_names = ", ".join(d["name"] for d in on_devices)
            message_text = f"Currently {len(on_devices)} device(s) are on: {device_names}."
        elif "off" in message:
            off_devices = [d for d in devices if d.get("status", "").lower() == "off"]
            device_names = ", ".join(d["name"] for d in off_devices) if off_devices else "none"
            message_text = f"The following devices are off: {device_names}."
        else:
            message_text = f"Your home has {len(devices)} device(s). {len(on_devices)} are currently on."

        return StructuredAIResponse(
            message=message_text,
            intent=IntentType.INFORMATION_QUERY,
            confidence=0.92,
            entities=[Entity(type="device_status", name="device_state")],
            proposed_actions=[],
            policy_status=PolicyStatus.INFORMATIONAL,
            requires_confirmation=False,
            provider=self.name,
        )

    def _handle_home_mode(self, message: str, context: Dict[str, Any]) -> StructuredAIResponse:
        """Handle home mode changes (sleep, away, home)"""
        if "sleep" in message:
            message_text = "I recommend turning off lights and lowering the temperature for sleep mode. Confirm to enable?"
            intent = IntentType.SETTING_CHANGE
        elif "away" in message or "leaving" in message:
            message_text = "Ready to arm away mode? I'll secure your home and optimize energy settings."
            intent = IntentType.SETTING_CHANGE
        else:
            message_text = "Home mode recognized. Ready to adjust settings."
            intent = IntentType.SETTING_CHANGE

        return StructuredAIResponse(
            message=message_text,
            intent=intent,
            confidence=0.85,
            entities=[Entity(type="home_mode", name="mode_change")],
            proposed_actions=[],
            policy_status=PolicyStatus.REQUIRES_CONFIRMATION,
            requires_confirmation=True,
            provider=self.name,
        )

    def _handle_unsupported(self, message: str, context: Dict[str, Any]) -> StructuredAIResponse:
        """Handle unsupported or ambiguous requests"""
        return StructuredAIResponse(
            message="I'm not sure how to help with that request. I can control devices, provide information about your home, or adjust modes.",
            intent=IntentType.AMBIGUOUS,
            confidence=0.45,
            entities=[],
            proposed_actions=[],
            policy_status=PolicyStatus.INFORMATIONAL,
            requires_confirmation=False,
            provider=self.name,
        )


def get_provider(provider_name: Optional[str] = None) -> AIProvider:
    """
    Factory function to get the configured AI provider.

    Args:
        provider_name: Optional provider name. If None, uses AI_PROVIDER env var (default: "mock")

    Returns:
        Initialized provider instance

    Raises:
        ValueError: If provider is not found or not implemented
    """
    name = provider_name or os.getenv("AI_PROVIDER", "mock").lower()

    if name == "mock":
        return MockAIProvider()

    # Future providers can be added here:
    # elif name == "gemini":
    #     from ai.providers.gemini import GeminiProvider
    #     return GeminiProvider()
    # elif name == "openai":
    #     from ai.providers.openai import OpenAIProvider
    #     return OpenAIProvider()

    raise ValueError(f"Unknown AI provider: {name}. Only 'mock' is available.")
