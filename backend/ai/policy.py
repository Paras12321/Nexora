SAFE_AUTOMATION_ACTIONS = {
    "recommend_energy_savings",
    "home_insight_summary",
    "device_idle_recommendation",
}


class AIActionPolicy:
    """Deterministic gate that keeps consequential actions approval-gated."""

    @staticmethod
    def validate(decision: str | None, requires_approval: bool) -> bool:
        if not decision:
            return True
        if decision in SAFE_AUTOMATION_ACTIONS:
            return True
        return bool(requires_approval) is False or decision is None
