from django.urls import path

from ai.views import AIAnalysisView, AILegacyAnalysisView

urlpatterns = [
    # New natural language interface
    path("analyze/", AIAnalysisView.as_view(), name="ai-analyze"),
    # Legacy template-based analysis (for energy reports, etc.)
    path("legacy/analyze/", AILegacyAnalysisView.as_view(), name="ai-legacy-analyze"),
]
