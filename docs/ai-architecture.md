# AI Architecture

## Overview

The NEXORA cloud AI layer sits between Django and the LLM provider. Django remains the system of record for user data, approvals, and deterministic policy enforcement. The AI service is intentionally isolated to a single orchestration boundary so the provider can change without impacting the rest of the app.

## Responsibilities

- Accept structured requests from Django or internal services.
- Render versioned prompt templates from the repository.
- Validate inputs against a strict schema.
- Hide provider-specific logic behind the provider abstraction.
- Record AI results in the database.
- Queue approval-required decisions into the DecisionLog workflow.
- Fail safely without exposing raw provider details to end users.

## Provider abstraction

The provider contract lives in `ai/providers.py` and exposes a single `generate` method. The default implementation is a mock provider, which allows local testing without an LLM key. A production provider can be added by implementing the same interface and selecting it via the `AI_PROVIDER` environment variable.

## Prompt templates

All prompt templates are stored under `backend/ai/templates/prompts/` and versioned with names like `bill_analysis_v1.txt`.

This ensures the prompt contract is reviewable in source control and changeable without altering code logic in the service layer.

## Structured schemas

Inputs and outputs use JSON-safe objects with clearly defined field names. This keeps provider output predictable and reduces the chance of unstructured model text being treated as a command.

## Safety rules

- No provider secret is committed to the repository.
- Only minimal context is sent to the AI layer.
- Sensitive values are redacted before prompt rendering.
- Providers are wrapped with timeout and retry limits.
- Raw provider exceptions are converted to safe user-facing failure messages.
- All consequential actions must pass through DecisionLog approval policy before execution.

## Persistence

AI analysis records are persisted in the `AIAnalysisResult` model and linked to the related home and, when needed, a `DecisionLog` entry.

## Decision flow

1. Django builds a structured request.
2. AI service renders the prompt template.
3. Provider returns structured output.
4. Django validates and persists the response.
5. If the result is consequential and approval-required, it is written to DecisionLog and remains pending until user approval.
