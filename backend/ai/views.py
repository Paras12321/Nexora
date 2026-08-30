from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from ai.service import AIService


class AIAnalysisView(APIView):
    """Endpoint for AI-backed home analysis requests."""

    permission_classes = [IsAuthenticated]

    def post(self, request):
        prompt_type = request.data.get("prompt_type")
        context = request.data.get("context", {})

        if not prompt_type:
            return Response({"detail": "prompt_type is required."}, status=status.HTTP_400_BAD_REQUEST)

        service = AIService()
        result = service.analyze(prompt_type=prompt_type, context=context)
        return Response({
            "status": result.status,
            "content": result.content,
            "decision": result.decision,
            "requires_approval": result.requires_approval,
            "confidence": result.confidence,
        }, status=status.HTTP_200_OK)
