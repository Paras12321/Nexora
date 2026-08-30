from unittest.mock import patch

from django.test import TestCase

from ai.providers import MockAIProvider, UnifiedAIResult
from ai.service import AIService


class AIServiceTests(TestCase):
    def test_mock_provider_returns_structured_result(self):
        service = AIService(provider=MockAIProvider())
        result = service.analyze(
            prompt_type="bill_analysis",
            context={
                "home_id": 1,
                "bill_amount": 220.0,
                "average_bill": 180.0,
                "usage_kwh": 850,
                "billing_period": "2026-08",
            },
        )

        self.assertIsInstance(result, UnifiedAIResult)
        self.assertEqual(result.prompt_type, "bill_analysis")
        self.assertIn("recommendation", result.content.lower())

    @patch("ai.service.MockAIProvider.generate")
    def test_service_uses_provider_and_handles_failures(self, mock_generate):
        mock_generate.side_effect = RuntimeError("provider unavailable")
        service = AIService(provider=MockAIProvider())

        result = service.analyze(prompt_type="energy_explanation", context={"home_id": 1})

        self.assertIsNotNone(result)
        self.assertEqual(result.status, "error")
        self.assertIn("temporarily unavailable", result.content.lower())
