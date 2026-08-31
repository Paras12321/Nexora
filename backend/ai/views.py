"""
AI API endpoints.

Provides the main interface for Android and other clients to access AI services.
"""

from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from ai.serializers import AIAnalysisRequestSerializer, AIAnalysisResponseSerializer
from ai.service import AIService
from ai.exceptions import ContextBuildingError, UnauthorizedContextAccess


class AIAnalysisView(APIView):
    """
    API endpoint for AI analysis of natural language requests.

    POST /api/ai/analyze/
    Request: {"message": "Turn off the bedroom light", "home_id": 1 (optional)}
    Response: StructuredAIResponse with intent, entities, proposed_actions, policy_status
    """

    permission_classes = [IsAuthenticated]

    def post(self, request):
        """
        Analyze a natural language message from the authenticated user.

        The response includes:
        - message: AI response text
        - intent: Recognized intent type
        - entities: Recognized entities (devices, rooms, etc.)
        - proposed_actions: Structured actions the AI recommends
        - policy_status: Whether action is approved/requires confirmation/rejected
        - confidence: Confidence level (0.0-1.0)
        """
        # Validate request
        serializer = AIAnalysisRequestSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        message = serializer.validated_data["message"]
        home_id = serializer.validated_data.get("home_id")

        # Run AI analysis
        service = AIService()
        try:
            response = service.analyze_message(
                user_id=request.user.id,
                message=message,
                home_id=home_id,
            )
        except (ContextBuildingError, UnauthorizedContextAccess) as exc:
            return Response(
                {"detail": str(exc)},
                status=status.HTTP_403_FORBIDDEN
            )
        except Exception as exc:
            return Response(
                {"detail": "An error occurred processing your request. Please try again."},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

        # Return structured response
        response_data = response.to_dict()
        return Response(response_data, status=status.HTTP_200_OK)


class AILegacyAnalysisView(APIView):
    """
    Legacy endpoint for template-based AI analysis (energy, bills, etc.).

    This endpoint maintains backward compatibility with existing prompt templates.
    New requests should use AIAnalysisView instead.
    """

    permission_classes = [IsAuthenticated]

    def post(self, request):
        """
        Analyze using a versioned prompt template.

        Request: {"prompt_type": "bill_analysis", "context": {...}}
        Response: Unified result with content, decision, confidence
        """
        prompt_type = request.data.get("prompt_type")
        context = request.data.get("context", {})

        if not prompt_type:
            return Response(
                {"detail": "prompt_type is required."},
                status=status.HTTP_400_BAD_REQUEST
            )

        service = AIService()
        try:
            result = service.analyze(prompt_type=prompt_type, context=context)
            return Response({
                "status": result.status,
                "content": result.content,
                "decision": result.decision,
                "requires_approval": result.requires_approval,
                "confidence": result.confidence,
            }, status=status.HTTP_200_OK)
        except ValueError as exc:
            return Response(
                {"detail": str(exc)},
                status=status.HTTP_400_BAD_REQUEST
            )
        except Exception as exc:
            return Response(
                {"detail": "AI service unavailable."},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
