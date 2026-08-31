"""Custom exceptions for the AI module."""


class AIException(Exception):
    """Base exception for AI-related errors."""

    pass


class ContextBuildingError(AIException):
    """Raised when context cannot be built for an AI request."""

    pass


class UnauthorizedContextAccess(AIException):
    """Raised when user attempts to access context from a home they don't have access to."""

    pass


class ProviderUnavailableError(AIException):
    """Raised when the AI provider is unavailable or fails."""

    pass


class InvalidAIResponseError(AIException):
    """Raised when the AI provider returns malformed or invalid data."""

    pass


class PolicyRejectionError(AIException):
    """Raised when a proposed action is rejected by the policy layer."""

    pass
