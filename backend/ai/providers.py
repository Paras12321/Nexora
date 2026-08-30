import json
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Any, Dict, Optional


@dataclass
class UnifiedAIResult:
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
    """Provider abstraction so the LLM backend can be replaced without changing app logic."""

    name = "base"
    timeout_seconds = 15
    max_retries = 2

    @abstractmethod
    def generate(self, prompt_type: str, prompt: str, schema: Dict[str, Any]) -> UnifiedAIResult:
        raise NotImplementedError


class MockAIProvider(AIProvider):
    """Default mock provider used for tests and local development without a real LLM key."""

    name = "mock"

    def generate(self, prompt_type: str, prompt: str, schema: Dict[str, Any]) -> UnifiedAIResult:
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
