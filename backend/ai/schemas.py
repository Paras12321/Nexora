"""Structured schemas for AI requests and responses."""

from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Dict, List, Optional


class IntentType(str, Enum):
    """Types of intents that the AI can infer from user requests."""

    INFORMATION_QUERY = "information_query"  # "What devices are on?"
    DEVICE_CONTROL = "device_control"  # "Turn on the light"
    AUTOMATION_QUERY = "automation_query"  # "Should I enable this automation?"
    SETTING_CHANGE = "setting_change"  # "Change the temperature"
    ANALYTICS_REQUEST = "analytics_request"  # "Show me my energy usage"
    AMBIGUOUS = "ambiguous"  # Cannot determine intent
    UNSUPPORTED = "unsupported"  # Request not supported


class PolicyStatus(str, Enum):
    """Policy decision on whether an action can proceed."""

    APPROVED = "approved"  # Can execute immediately
    REQUIRES_CONFIRMATION = "requires_confirmation"  # Must be approved by user
    REJECTED = "rejected"  # Policy forbids this action
    INFORMATIONAL = "informational"  # No action, just response


class ActionType(str, Enum):
    """Types of actions the AI can propose."""

    TURN_ON = "turn_on"
    TURN_OFF = "turn_off"
    SET_LEVEL = "set_level"
    SET_TEMPERATURE = "set_temperature"
    LOCK = "lock"
    UNLOCK = "unlock"
    DISARM = "disarm"
    ARM = "arm"
    NO_ACTION = "no_action"


@dataclass
class Entity:
    """A recognized entity (device, room, user, etc.) in the user's request."""

    type: str  # "device", "room", "home", "user", etc.
    name: str  # User-visible name
    id: Optional[int] = None  # Database ID if resolvable


@dataclass
class ProposedAction:
    """A structured action that the AI proposes."""

    action_type: ActionType
    device_id: Optional[int] = None
    room_id: Optional[int] = None
    parameters: Dict[str, Any] = field(default_factory=dict)  # e.g. {"level": 75, "temperature": 72}
    reason: str = ""  # Why this action is proposed


@dataclass
class AIRequestContext:
    """Authoritative context for an AI request built by the context builder."""

    user_id: int
    home_id: Optional[int] = None  # Primary home, or None if not available
    accessible_homes: List[int] = field(default_factory=list)  # Home IDs user has access to
    devices: List[Dict[str, Any]] = field(default_factory=list)  # Authorized devices
    rooms: List[Dict[str, Any]] = field(default_factory=list)  # Authorized rooms
    security_state: Optional[str] = None  # "disarmed", "armed_home", "armed_away"
    presence_state: Optional[str] = None  # "home", "away", "unknown"

    def to_dict(self) -> Dict[str, Any]:
        """Convert to JSON-serializable dict."""
        return {
            "user_id": self.user_id,
            "home_id": self.home_id,
            "accessible_homes": self.accessible_homes,
            "devices": self.devices,
            "rooms": self.rooms,
            "security_state": self.security_state,
            "presence_state": self.presence_state,
        }


@dataclass
class StructuredAIResponse:
    """Structured response returned by the AI pipeline."""

    # Core fields
    message: str  # User-facing response text
    intent: IntentType  # Inferred intent
    confidence: float  # 0.0-1.0 confidence in this analysis

    # Recognition
    entities: List[Entity] = field(default_factory=list)  # Recognized entities
    proposed_actions: List[ProposedAction] = field(default_factory=list)  # Proposed actions

    # Policy & Safety
    policy_status: PolicyStatus = PolicyStatus.INFORMATIONAL
    requires_confirmation: bool = False

    # Metadata
    provider: str = "mock"  # Which provider generated this
    provider_metadata: Dict[str, Any] = field(default_factory=dict)  # Additional provider info
    decision_log_id: Optional[int] = None  # If created a decision log entry

    def to_dict(self) -> Dict[str, Any]:
        """Convert to JSON-serializable dict."""
        return {
            "message": self.message,
            "intent": self.intent.value,
            "confidence": self.confidence,
            "entities": [
                {
                    "type": e.type,
                    "name": e.name,
                    "id": e.id,
                }
                for e in self.entities
            ],
            "proposed_actions": [
                {
                    "action_type": a.action_type.value,
                    "device_id": a.device_id,
                    "room_id": a.room_id,
                    "parameters": a.parameters,
                    "reason": a.reason,
                }
                for a in self.proposed_actions
            ],
            "policy_status": self.policy_status.value,
            "requires_confirmation": self.requires_confirmation,
            "provider": self.provider,
            "decision_log_id": self.decision_log_id,
        }
