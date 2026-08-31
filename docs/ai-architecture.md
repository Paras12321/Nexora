# AI Architecture

## Overview

The NEXORA AI layer provides intelligent home automation analysis and control recommendations. The system is designed to be provider-agnostic: Django serves as the system of record for all data, authorization, and policy enforcement, while the AI provider is pluggable and can be replaced without rewriting the rest of the application.

**Current Implementation:** Mock provider (deterministic, no external API required)

## Architecture Diagram

```
Android App / Client
        |
        | POST /api/ai/analyze/
        |
        v
Django REST Framework
        |
        v
Authentication & Authorization
        |
        v
Context Builder (ContextBuilder)
        - Authorizes user
        - Retrieves devices, rooms, security state
        - Excludes sensitive data
        |
        v
AI Service (AIService)
        |
        +-- Provider Selection (based on AI_PROVIDER env var)
        |
        v
AI Provider (AIProvider interface)
        |
        +---- MockAIProvider (current)
        |
        +---- [Future: GeminiProvider]
        |
        +---- [Future: OpenAIProvider]
        |
        v
Structured Response (StructuredAIResponse)
        - intent
        - entities
        - proposed_actions
        - confidence
        |
        v
Policy/Safety Layer
        - Checks for sensitive actions
        - Enforces confirmation requirements
        - Applies business rules
        |
        v
Decision Logging (DecisionLog)
        - Approval-required decisions go to pending
        - Audit trail for all AI analyses
        |
        v
JSON Response to Client
```

## Core Components

### 1. Context Builder (`ai/context.py`)

Safely retrieves and constructs authorized context for AI requests.

**Responsibilities:**
- Verify user authentication
- Check authorization (user has access to home)
- Retrieve devices, rooms, device states
- Get latest security/presence state
- **Exclude sensitive data** (passwords, tokens, serial numbers)

**Example Context:**
```python
AIRequestContext(
    user_id=42,
    home_id=1,
    accessible_homes=[1],
    devices=[
        {"id": 1, "name": "bedroom_light", "type": "light", "status": "off", ...},
        {"id": 2, "name": "thermostat", "type": "thermostat", "status": "on", ...},
    ],
    rooms=[
        {"id": 1, "name": "Bedroom", "device_count": 2},
    ],
    security_state="disarmed",
    presence_state="home",
)
```

### 2. Provider Abstraction (`ai/providers.py`)

All LLM interaction goes through the `AIProvider` base class. This enables swapping providers without modifying the rest of the application.

**Base Class Methods:**
```python
class AIProvider(ABC):
    def generate(self, prompt_type: str, prompt: str, schema: Dict) -> UnifiedAIResult:
        """Legacy template-based analysis (energy reports)."""
        
    def analyze_message(self, message: str, context: Dict) -> StructuredAIResponse:
        """New message-based analysis (natural language requests)."""
```

### 3. Mock Provider (`ai/providers.py`)

Deterministic implementation for development/testing. Returns realistic responses without API credentials.

**Covers:**
- Device control requests ("Turn on the bedroom light")
- Information queries ("What devices are on?")
- Home mode changes ("I'm going to sleep")
- Unsupported/ambiguous requests

### 4. Structured Schemas (`ai/schemas.py`)

Strong typing for AI inputs and outputs.

**IntentType:** Information query, device control, automation query, setting change, analytics request, ambiguous, unsupported

**PolicyStatus:** Approved, requires_confirmation, rejected, informational

**StructuredAIResponse:**
```python
@dataclass
class StructuredAIResponse:
    message: str              # User-facing response
    intent: IntentType        # Recognized intent
    confidence: float         # 0.0-1.0
    entities: List[Entity]    # Recognized entities
    proposed_actions: List[ProposedAction]  # Structured actions
    policy_status: PolicyStatus             # Policy decision
    requires_confirmation: bool             # User approval needed?
```

### 5. AI Service (`ai/service.py`)

Central orchestration service coordinating the complete pipeline.

**Primary Method:**
```python
def analyze_message(
    user_id: int,
    message: str,
    home_id: Optional[int] = None,
) -> StructuredAIResponse:
    # 1. Build authorized context
    # 2. Call provider
    # 3. Apply policy/safety checks
    # 4. Persist to database
    # 5. Return structured response
```

### 6. Policy/Safety Layer

Enforces business rules on AI output.

**Policy Rules:**
- Sensitive actions (lock, unlock, disarm, arm) require confirmation
- Unidentified targets are rejected
- All consequential actions require explicit user approval before execution
- All decisions are logged for audit

### 7. Request Serializers (`ai/serializers.py`)

Validate API input/output.

**AIAnalysisRequestSerializer:**
- `message` (required): Natural language request
- `home_id` (optional): Scope to specific home

## Provider Integration Guide

### Implementing a New Provider

To add a real LLM (Gemini, OpenAI, Claude, etc.), create a new provider:

**File:** `ai/providers/gemini.py`
```python
from ai.providers import AIProvider
from ai.schemas import StructuredAIResponse

class GeminiProvider(AIProvider):
    name = "gemini"
    
    def __init__(self):
        self.api_key = os.getenv("AI_API_KEY")
        self.model = os.getenv("AI_MODEL", "gemini-2.0-flash")
    
    def generate(self, prompt_type: str, prompt: str, schema: Dict) -> UnifiedAIResult:
        # Call Gemini API for template-based analysis
        pass
    
    def analyze_message(self, message: str, context: Dict) -> StructuredAIResponse:
        # Call Gemini API for natural language analysis
        pass
```

**Update:** `ai/providers.py` get_provider()
```python
def get_provider(provider_name: Optional[str] = None) -> AIProvider:
    name = provider_name or os.getenv("AI_PROVIDER", "mock").lower()
    
    if name == "mock":
        return MockAIProvider()
    elif name == "gemini":
        from ai.providers.gemini import GeminiProvider
        return GeminiProvider()
    
    raise ValueError(f"Unknown provider: {name}")
```

**Configuration:** `.env`
```
AI_PROVIDER=gemini
AI_API_KEY=your-gemini-api-key
AI_MODEL=gemini-2.0-flash
```

### Key Requirements for New Providers

1. **No Direct Database Access:** Receive context, don't query database
2. **No Command Execution:** Return structured recommendations, not executable commands
3. **Respect Schema:** Return `StructuredAIResponse` matching defined schema
4. **Handle Errors Gracefully:** Never raise raw exceptions to client
5. **Timeout Support:** Respect timeout_seconds configuration
6. **Retry Logic:** Support max_retries configuration
7. **Sensitive Data:** Never log or store passwords, tokens, API keys, or user secrets

## Request Flow

### 1. User Sends Natural Language Request

```
POST /api/ai/analyze/
{
    "message": "Turn off the bedroom light",
    "home_id": 1
}
```

### 2. Django Authenticates & Validates

- Verify token
- Validate request schema
- Extract user ID and home ID

### 3. Context Builder Constructs Authorized Context

```python
context = ContextBuilder.build(user_id=42, home_id=1)
# Throws UnauthorizedContextAccess if user doesn't have access
```

### 4. AI Service Calls Provider

```python
response = provider.analyze_message(
    message="Turn off the bedroom light",
    context=context.to_dict()
)
```

### 5. Policy/Safety Layer Evaluates

- Check if action requires confirmation
- Verify device targets exist
- Apply business rules

### 6. Logging & Decision Creation

- Store in AIAnalysisResult
- If requires confirmation, create DecisionLog entry (status=PENDING)

### 7. Return to Client

```json
{
    "message": "I'll turn off the bedroom light for you.",
    "intent": "device_control",
    "confidence": 0.87,
    "policy_status": "requires_confirmation",
    "requires_confirmation": true,
    "proposed_actions": [{
        "action_type": "turn_off",
        "device_id": 5,
        ...
    }]
}
```

## Configuration

### Environment Variables

```
# AI Provider
AI_PROVIDER=mock                    # mock, gemini, openai, etc.
AI_API_KEY=                         # API key for real providers (leave empty for mock)
AI_MODEL=                           # Model name for real providers
AI_TIMEOUT_SECONDS=15               # Provider timeout
AI_MAX_RETRIES=2                    # Number of retries on failure
```

### Default Behavior

- If `AI_PROVIDER` is not set or is invalid, defaults to `mock`
- Mock provider requires no API credentials
- Mock provider returns deterministic responses

## Security Considerations

1. **No Credentials in Repo:**
   - `.env.example` shows the structure but never includes real values
   - Real `.env` file is in `.gitignore`

2. **Context Sanitization:**
   - ContextBuilder excludes sensitive fields (passwords, serial numbers, secrets)
   - Prompt templates sanitize context before sending to LLM

3. **Authorization Enforcement:**
   - Every request is authenticated
   - Users can only access homes they belong to
   - Devices and rooms are filtered by home authorization

4. **Policy Enforcement:**
   - Sensitive actions require explicit confirmation
   - No action is auto-executed based on AI recommendation alone
   - All decisions are logged for audit

5. **Error Handling:**
   - Provider errors never leak to client
   - Safe failure messages instead of stack traces
   - Server-side logging includes diagnostic info (without secrets)

## Testing

**Test Coverage:**
- ✅ Mock provider returns realistic responses
- ✅ Context builder enforces authorization
- ✅ API endpoints validate input
- ✅ Unauthenticated requests are rejected
- ✅ Policy layer enforces confirmation requirements
- ✅ Sensitive actions are protected
- ✅ Error responses are safe
- ✅ Audit logging captures decisions
- ✅ No secret leakage in responses

**Run Tests:**
```bash
python manage.py test ai --verbosity 2
```

## Future Enhancements

1. **Real LLM Providers:**
   - Google Gemini
   - OpenAI GPT
   - Anthropic Claude
   - Local models (Ollama, LLaMA)

2. **Advanced Intent Recognition:**
   - Multi-step automation sequences
   - Natural language constraints ("unless it's raining")
   - Temporal patterns ("every morning at 7am")

3. **User Preferences:**
   - Learn user preferences from past decisions
   - Personalized response patterns

4. **Device Actions:**
   - Execute approved actions automatically
   - Provide feedback loop

5. **Batch Processing:**
   - Handle multiple device commands in one request

6. **Analytics:**
   - Track intent distribution
   - Monitor provider performance
   - Error rate analysis

## Limitations & Notes

### Current Implementation (Mock Provider)

- Pattern-matching based intent recognition (not trained NLP)
- Limited entity extraction (bedroom, light, etc.)
- No multi-step sequences
- Deterministic responses (same input = same output)
- No learning from user feedback

### Roadmap

- Real LLM integration will enable:
  - Sophisticated natural language understanding
  - Contextual reasoning
  - Multi-step sequences
  - Learning from corrections
  - Better error handling

## API Contract

See [api-contract.md](api-contract.md) for complete endpoint documentation:
- `POST /api/ai/analyze/` - Main AI endpoint
- `POST /api/ai/legacy/analyze/` - Legacy energy analysis

## Support

For questions or issues:
1. Check the test cases in `ai/tests.py`
2. Review the provider implementation in `ai/providers.py`
3. Inspect the context builder in `ai/context.py`
4. Check environment configuration in `settings.py`
