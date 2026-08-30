# API Contract

## Overview

This document defines the initial backend API contract for the NEXORA project.

## Base URL

- Local development: http://localhost:8000/api

## Health

### GET /api/health/

Returns backend availability.

#### Response

```json
{
  "status": "ok",
  "service": "nexora-backend"
}
```

## Notes

- This file is intentionally a skeleton for the hackathon backend foundation.
- Business endpoints and authentication routes will be added later.
