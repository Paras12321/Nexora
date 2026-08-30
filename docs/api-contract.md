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
