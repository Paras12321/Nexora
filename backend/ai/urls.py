from django.urls import path

from ai.views import AIAnalysisView

urlpatterns = [
    path("analyze/", AIAnalysisView.as_view(), name="ai-analyze"),
]
