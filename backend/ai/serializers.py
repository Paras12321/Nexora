"""Serializers for AI request and response validation."""

from rest_framework import serializers


class AIAnalysisRequestSerializer(serializers.Serializer):
    """Validates AI analysis request from Android/client."""

    message = serializers.CharField(
        max_length=1000,
        min_length=1,
        help_text="Natural language request or query"
    )
    home_id = serializers.IntegerField(
        required=False,
        allow_null=True,
        help_text="Optional home ID to scope the request. Uses user's primary home if not specified."
    )

    def validate_message(self, value):
        """Ensure message is not just whitespace."""
        if not value.strip():
            raise serializers.ValidationError("Message cannot be empty.")
        return value.strip()


class ProposedActionSerializer(serializers.Serializer):
    """Serializes a proposed action."""

    action_type = serializers.CharField(max_length=50)
    device_id = serializers.IntegerField(required=False, allow_null=True)
    room_id = serializers.IntegerField(required=False, allow_null=True)
    parameters = serializers.JSONField(required=False, default=dict)
    reason = serializers.CharField(max_length=500, required=False, default="")


class EntitySerializer(serializers.Serializer):
    """Serializes a recognized entity."""

    type = serializers.CharField(max_length=50)
    name = serializers.CharField(max_length=255)
    id = serializers.IntegerField(required=False, allow_null=True)


class AIAnalysisResponseSerializer(serializers.Serializer):
    """Serializes AI analysis response."""

    message = serializers.CharField()
    intent = serializers.CharField()
    confidence = serializers.FloatField()
    entities = EntitySerializer(many=True)
    proposed_actions = ProposedActionSerializer(many=True)
    policy_status = serializers.CharField()
    requires_confirmation = serializers.BooleanField()
    provider = serializers.CharField()
    decision_log_id = serializers.IntegerField(required=False, allow_null=True)
