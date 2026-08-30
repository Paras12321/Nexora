from django.db import models

from core.models import Home


class AIAnalysisResult(models.Model):
    """Persist AI analyses and any issued consequential decisions."""

    class Status(models.TextChoices):
        OK = "ok", "OK"
        ERROR = "error", "Error"

    prompt_type = models.CharField(max_length=80)
    provider = models.CharField(max_length=80, default="mock")
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.OK)
    home = models.ForeignKey(Home, on_delete=models.SET_NULL, null=True, blank=True, related_name="ai_analysis_results")
    request_payload = models.JSONField(default=dict, blank=True)
    response_payload = models.JSONField(default=dict, blank=True)
    decision_log = models.ForeignKey(
        "core.DecisionLog",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="ai_analysis_results",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "nexora_ai_analysis_result"
        ordering = ["-created_at"]

    def __str__(self):
        return f"{self.prompt_type} [{self.status}]"
