# API Contract

## Overview

This document defines the backend API contract for the NEXORA project.
Authentication is token-based. All protected endpoints require the following header:
`Authorization: Token <your_token>`

## Base URL

- Local development: `http://localhost:8000/api`

---

## Health

### GET /api/health/
Returns backend availability.
**Response (200 OK):**
```json
{
  "status": "ok",
  "service": "nexora-backend"
}
```

---

## Authentication

### POST /api/auth/register/
Register a new user.
**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "StrongPassword123!",
  "first_name": "Alice",
  "last_name": "Smith"
}
```
**Response (201 Created):**
```json
{
  "token": "abcdef123456...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "first_name": "Alice",
    "last_name": "Smith",
    "date_joined": "2026-08-30T10:00:00Z"
  }
}
```

### POST /api/auth/login/
Log in to get a token.
**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "StrongPassword123!"
}
```
**Response (200 OK):** Same as register response.

### POST /api/auth/logout/
Revoke the current token. Requires Auth.
**Response (200 OK):** `{"detail": "Logged out."}`

### GET /api/auth/me/
Get current user profile. Requires Auth.
**Response (200 OK):** User object.

### POST /api/auth/password-reset/
Request a password reset link.
**Request Body:** `{"email": "user@example.com"}`
**Response (200 OK):** `{"detail": "...", "uid": "...", "token": "..."}`

### POST /api/auth/password-reset/confirm/
Confirm password reset.
**Request Body:**
```json
{
  "uid": "...",
  "token": "...",
  "new_password": "NewStrongPassword123!"
}
```
**Response (200 OK):** `{"detail": "Password has been reset."}`

---

## Homes

### GET /api/homes/
List all homes the user is a member of. Requires Auth.
**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "My Home",
    "owner": 1,
    "owner_email": "user@example.com",
    "created_at": "...",
    "updated_at": "..."
  }
]
```

### POST /api/homes/
Create a new home. The creator becomes the owner. Requires Auth.
**Request Body:** `{"name": "My Home"}`
**Response (201 Created):** Home object.

### GET /api/homes/<id>/
Get details of a specific home. Requires Auth (must be member).
**Response (200 OK):** Home object.

### PUT /api/homes/<id>/
Update a home. Requires Auth (must be owner).
**Request Body:** `{"name": "Updated Home"}`
**Response (200 OK):** Home object.

### DELETE /api/homes/<id>/
Delete a home. Requires Auth (must be owner).
**Response (204 No Content)**

---

## Home Members

### GET /api/homes/<id>/members/
List members of a home. Requires Auth (must be member).
**Response (200 OK):**
```json
[
  {
    "id": 1,
    "email": "user@example.com",
    "first_name": "Alice",
    "last_name": "Smith",
    "role": "owner",
    "joined_at": "..."
  }
]
```

### POST /api/homes/<id>/members/
Invite a user to a home. Requires Auth (must be owner).
**Request Body:** `{"email": "friend@example.com"}`
**Response (201 Created):** HomeMember object.

### DELETE /api/homes/<id>/members/<member_id>/
Remove a member. Requires Auth (must be owner).
**Response (204 No Content)**

### POST /api/homes/<id>/leave/
Leave a home. Requires Auth (must be member, cannot be owner).
**Response (200 OK):** `{"detail": "You have left the home."}`

---

## Presence & Security (BE4)

### GET /api/homes/<id>/presence/
Get the latest 50 presence events. Requires Auth.

### POST /api/homes/<id>/presence/
Create a presence event.
**Request Body:** `{"state": "home|away|unknown", "source": "system"}`
**Response (201 Created):** PresenceEvent object.

### GET /api/homes/<id>/security/
Get the latest 50 security events. Requires Auth.

### POST /api/homes/<id>/security/
Change security mode. Must be one of the deterministic choices.
**Request Body:** `{"mode": "disarmed|armed_home|armed_away", "source": "user"}`
**Response (201 Created):** SecurityEvent object.

---

## Energy & Billing (BE4)

### GET /api/homes/<id>/energy/
List energy usage records.

### POST /api/homes/<id>/energy/
Create an energy usage record.
**Request Body:** `{"start_time": "...", "end_time": "...", "usage_kwh": "..."}`

### GET /api/homes/<id>/bills/
List electricity bills.

### POST /api/homes/<id>/bills/
Submit a new electricity bill.
**Request Body:** `{"billing_period_start": "...", "billing_period_end": "...", "amount": "...", "usage_kwh": "..."}`

---

## AI Analysis

### POST /api/ai/analyze/
Analyze a natural language request from the user using the configured AI provider.

**Current Provider:** Mock (deterministic, no API credentials required)

**Request Body:**
```json
{
  "message": "Turn off the bedroom light",
  "home_id": 1
}
```

**Parameters:**
- `message` (required, string): Natural language request or query from user
- `home_id` (optional, integer): Scopes the request to a specific home. If not provided, uses the user's primary home.

**Response (200 OK):**
```json
{
  "message": "I'll turn off the bedroom light for you.",
  "intent": "device_control",
  "confidence": 0.87,
  "entities": [
    {
      "type": "action",
      "name": "turn_off",
      "id": null
    }
  ],
  "proposed_actions": [
    {
      "action_type": "turn_off",
      "device_id": 5,
      "room_id": 2,
      "parameters": {},
      "reason": "User requested to turn off this device."
    }
  ],
  "policy_status": "requires_confirmation",
  "requires_confirmation": true,
  "provider": "mock",
  "decision_log_id": null
}
```

**Response Fields:**
- `message`: User-facing response text
- `intent`: Recognized intent type (`information_query`, `device_control`, `automation_query`, `ambiguous`, `unsupported`)
- `confidence`: 0.0-1.0 confidence level of the analysis
- `entities`: Recognized entities (devices, rooms, actions)
- `proposed_actions`: Structured actions the AI recommends (device_id, action_type, parameters)
- `policy_status`: `approved`, `requires_confirmation`, `rejected`, or `informational`
- `requires_confirmation`: Boolean indicating if user must approve before action executes
- `provider`: Name of the AI provider (currently "mock")
- `decision_log_id`: If an approval-gated decision was created, its ID

**Error Responses:**
- `400 Bad Request`: Missing or invalid message
- `401 Unauthorized`: No valid token provided
- `403 Forbidden`: User does not have access to the specified home
- `500 Internal Server Error`: AI service unavailable

**Notes:**
- All requests must be authenticated with a valid token
- The AI layer cannot bypass policy/safety checks
- Sensitive actions (lock/unlock/disarm/arm) automatically require confirmation
- All AI analyses are logged for audit purposes
- Django remains the authority for database access and action authorization

### POST /api/ai/legacy/analyze/
Legacy endpoint for template-based AI analysis (energy reports, bill analysis, etc.)

**Deprecated:** New clients should use `POST /api/ai/analyze/` instead. This endpoint maintains backward compatibility with existing energy analysis workflows.

**Request Body:**
```json
{
  "prompt_type": "bill_analysis",
  "context": {
    "home_id": 1,
    "bill_amount": 220.0,
    "average_bill": 180.0,
    "usage_kwh": 850,
    "billing_period": "2026-08"
  }
}
```

**Response (200 OK):**
```json
{
  "status": "ok",
  "content": "Your August bill is 22% higher than your recent average. Recommendation: reduce non-essential cooling and evening appliance loads.",
  "decision": "recommend_energy_savings",
  "requires_approval": true,
  "confidence": 0.91
}
```

**Supported prompt_type values:**
- `bill_analysis`: Analyze electricity bill and trends
- `energy_explanation`: Explain energy usage patterns
- `anomaly_explanation`: Explain unusual readings
- `automation_recommendation`: Recommend automation rules
- `home_insights`: Provide general home insights

---

## Logs (BE4)

### GET /api/homes/<id>/activity-logs/
List activity logs.

### POST /api/homes/<id>/activity-logs/
Log an action/event.

### GET /api/homes/<id>/decision-logs/
List AI or system decision logs.

### POST /api/homes/<id>/decision-logs/
Propose a decision (e.g. from AI) which goes into `pending_approval` state.
**Request Body:** `{"source": "AI_Agent", "decision": "arm_system", "reason": "No one is home"}`

### POST /api/homes/<id>/decision-logs/<log_id>/approve/
Approve or reject a pending decision.
**Request Body:** `{"action": "approve|reject"}`
**Response (200 OK):** `{"detail": "Decision approved and executed."}`
