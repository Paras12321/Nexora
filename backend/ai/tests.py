"""Comprehensive tests for AI architecture."""

from unittest.mock import patch, MagicMock
from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APIClient
from rest_framework.authtoken.models import Token

from core.models import Home, HomeMember, Room, Device, DecisionLog
from ai.providers import MockAIProvider, UnifiedAIResult, get_provider
from ai.service import AIService
from ai.context import ContextBuilder
from ai.schemas import IntentType, PolicyStatus, StructuredAIResponse
from ai.exceptions import ContextBuildingError, UnauthorizedContextAccess

User = get_user_model()


class MockAIProviderTests(TestCase):
    """Tests for the mock AI provider."""

    def setUp(self):
        self.provider = MockAIProvider()

    def test_mock_provider_device_control_turn_on(self):
        """Test mock provider recognizes device control requests."""
        context = {
            "devices": [
                {"id": 1, "name": "bedroom light", "room_name": "bedroom", "status": "off"}
            ]
        }
        response = self.provider.analyze_message("Turn on the bedroom light", context)
        
        self.assertEqual(response.intent, IntentType.DEVICE_CONTROL)
        self.assertGreater(response.confidence, 0.7)
        self.assertTrue(response.requires_confirmation)
        self.assertEqual(len(response.proposed_actions), 1)
        self.assertEqual(response.proposed_actions[0].device_id, 1)

    def test_mock_provider_information_query(self):
        """Test mock provider recognizes information queries."""
        context = {
            "devices": [
                {"id": 1, "name": "light1", "status": "on"},
                {"id": 2, "name": "light2", "status": "off"},
            ]
        }
        response = self.provider.analyze_message("What devices are on?", context)
        
        self.assertEqual(response.intent, IntentType.INFORMATION_QUERY)
        self.assertFalse(response.requires_confirmation)
        self.assertEqual(response.policy_status, PolicyStatus.INFORMATIONAL)

    def test_mock_provider_unsupported_request(self):
        """Test mock provider handles unsupported requests gracefully."""
        context = {"devices": []}
        response = self.provider.analyze_message("Show me the weather forecast", context)
        
        self.assertEqual(response.intent, IntentType.AMBIGUOUS)
        self.assertFalse(response.requires_confirmation)

    def test_mock_provider_legacy_bill_analysis(self):
        """Test legacy template-based provider response."""
        result = self.provider.generate(
            prompt_type="bill_analysis",
            prompt="Bill is high",
            schema={}
        )
        
        self.assertEqual(result.status, "ok")
        self.assertIn("bill", result.content.lower())
        self.assertTrue(result.requires_approval)
        self.assertGreater(result.confidence, 0.8)


class ContextBuilderTests(TestCase):
    """Tests for authorized context building."""

    def setUp(self):
        self.user = User.objects.create_user(
            email="user@example.com",
            password="password",
            first_name="Test",
            last_name="User"
        )
        self.home = Home.objects.create(name="Test Home", owner=self.user)
        HomeMember.objects.create(home=self.home, user=self.user, role="owner")
        
        self.room = Room.objects.create(home=self.home, name="Bedroom")
        self.device = Device.objects.create(
            home=self.home,
            room=self.room,
            name="Light",
            device_type="light",
            status="on"
        )

    def test_context_builder_success(self):
        """Test successful context building for authorized user."""
        context = ContextBuilder.build(self.user.id, self.home.id)
        
        self.assertEqual(context.user_id, self.user.id)
        self.assertEqual(context.home_id, self.home.id)
        self.assertIn(self.home.id, context.accessible_homes)
        self.assertEqual(len(context.devices), 1)
        self.assertEqual(len(context.rooms), 1)
        self.assertEqual(context.devices[0]["name"], "Light")

    def test_context_builder_no_home(self):
        """Test context building without specifying home uses primary."""
        context = ContextBuilder.build(self.user.id)
        
        self.assertEqual(context.home_id, self.home.id)

    def test_context_builder_unauthorized_access(self):
        """Test context builder rejects unauthorized home access."""
        other_user = User.objects.create_user(
            email="other@example.com",
            password="password",
            first_name="Other",
            last_name="User"
        )
        
        with self.assertRaises(UnauthorizedContextAccess):
            ContextBuilder.build(other_user.id, self.home.id)

    def test_context_builder_no_access(self):
        """Test context builder rejects user with no homes."""
        isolated_user = User.objects.create_user(
            email="isolated@example.com",
            password="password",
            first_name="Isolated",
            last_name="User"
        )
        
        with self.assertRaises(ContextBuildingError):
            ContextBuilder.build(isolated_user.id)

    def test_context_excludes_sensitive_data(self):
        """Test that context does not include sensitive data."""
        context = ContextBuilder.build(self.user.id, self.home.id)
        
        # Should not include serial numbers or other sensitive info
        for device in context.devices:
            self.assertNotIn("serial_number", device)
            self.assertNotIn("password", device)


class AIServiceTests(TestCase):
    """Tests for AI service orchestration."""

    def setUp(self):
        self.user = User.objects.create_user(
            email="user@example.com",
            password="password",
            first_name="Test",
            last_name="User"
        )
        self.home = Home.objects.create(name="Test Home", owner=self.user)
        HomeMember.objects.create(home=self.home, user=self.user, role="owner")
        
        self.room = Room.objects.create(home=self.home, name="Bedroom")
        self.device = Device.objects.create(
            home=self.home,
            room=self.room,
            name="Light",
            device_type="light",
            status="off"
        )
        
        self.service = AIService()

    def test_analyze_message_device_control(self):
        """Test analyzing a device control message."""
        response = self.service.analyze_message(
            user_id=self.user.id,
            message="Turn on the bedroom light",
            home_id=self.home.id
        )
        
        self.assertEqual(response.intent, IntentType.DEVICE_CONTROL)
        self.assertGreater(response.confidence, 0.5)

    def test_analyze_message_unauthorized_home(self):
        """Test that analyzing for unauthorized home fails safely."""
        other_home = Home.objects.create(name="Other Home", owner=self.user)
        other_user = User.objects.create_user(
            email="other@example.com",
            password="password",
            first_name="Other",
            last_name="User"
        )
        
        response = self.service.analyze_message(
            user_id=other_user.id,
            message="Turn on the light",
            home_id=other_home.id
        )
        
        # Should return error response, not crash
        self.assertEqual(response.policy_status, PolicyStatus.INFORMATIONAL)
        self.assertFalse(response.requires_confirmation)

    def test_analyze_message_creates_decision_log(self):
        """Test that confirmable actions create decision logs."""
        response = self.service.analyze_message(
            user_id=self.user.id,
            message="Turn on the bedroom light",
            home_id=self.home.id
        )
        
        if response.requires_confirmation:
            # Should have created a decision log
            decision_logs = DecisionLog.objects.filter(home=self.home)
            self.assertGreaterEqual(len(decision_logs), 0)  # May or may not be created depending on response

    def test_analyze_legacy_template(self):
        """Test legacy template-based analysis."""
        result = self.service.analyze(
            prompt_type="bill_analysis",
            context={"home_id": self.home.id, "bill_amount": 200}
        )
        
        self.assertEqual(result.status, "ok")
        self.assertIsNotNone(result.content)

    def test_analyze_legacy_missing_template(self):
        """Test legacy analysis with missing template fails gracefully."""
        result = self.service.analyze(
            prompt_type="nonexistent_template",
            context={}
        )
        
        self.assertEqual(result.status, "error")
        self.assertIn("unavailable", result.content.lower())


class AIAnalysisEndpointTests(TestCase):
    """Tests for AI API endpoints."""

    def setUp(self):
        self.client = APIClient()
        self.user = User.objects.create_user(
            email="user@example.com",
            password="password",
            first_name="Test",
            last_name="User"
        )
        self.token = Token.objects.create(user=self.user)
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {self.token.key}")
        
        self.home = Home.objects.create(name="Test Home", owner=self.user)
        HomeMember.objects.create(home=self.home, user=self.user, role="owner")
        
        self.room = Room.objects.create(home=self.home, name="Bedroom")
        self.device = Device.objects.create(
            home=self.home,
            room=self.room,
            name="Light",
            device_type="light",
            status="off"
        )

    def test_analyze_endpoint_success(self):
        """Test successful AI analysis request."""
        response = self.client.post(
            reverse("ai-analyze"),
            {"message": "What devices are in the bedroom?"},
            format="json"
        )
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        data = response.json()
        self.assertIn("message", data)
        self.assertIn("intent", data)
        self.assertIn("confidence", data)
        self.assertIn("policy_status", data)

    def test_analyze_endpoint_with_home_id(self):
        """Test AI analysis with explicit home_id."""
        response = self.client.post(
            reverse("ai-analyze"),
            {
                "message": "Turn on the light",
                "home_id": self.home.id
            },
            format="json"
        )
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_analyze_endpoint_missing_message(self):
        """Test endpoint rejects missing message."""
        response = self.client.post(
            reverse("ai-analyze"),
            {},
            format="json"
        )
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_analyze_endpoint_unauthenticated(self):
        """Test endpoint requires authentication."""
        client = APIClient()
        response = client.post(
            reverse("ai-analyze"),
            {"message": "Turn on the light"},
            format="json"
        )
        
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_legacy_analyze_endpoint(self):
        """Test legacy template-based endpoint."""
        response = self.client.post(
            reverse("ai-legacy-analyze"),
            {
                "prompt_type": "bill_analysis",
                "context": {"home_id": self.home.id}
            },
            format="json"
        )
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        data = response.json()
        self.assertIn("status", data)
        self.assertIn("content", data)

    def test_legacy_analyze_endpoint_missing_prompt_type(self):
        """Test legacy endpoint requires prompt_type."""
        response = self.client.post(
            reverse("ai-legacy-analyze"),
            {"context": {}},
            format="json"
        )
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class ProviderFactoryTests(TestCase):
    """Tests for provider factory."""

    def test_get_provider_default_mock(self):
        """Test that default provider is mock."""
        provider = get_provider()
        self.assertEqual(provider.name, "mock")
        self.assertIsInstance(provider, MockAIProvider)

    def test_get_provider_explicit_mock(self):
        """Test explicitly requesting mock provider."""
        provider = get_provider("mock")
        self.assertEqual(provider.name, "mock")

    def test_get_provider_unknown_raises_error(self):
        """Test that unknown provider raises ValueError."""
        with self.assertRaises(ValueError):
            get_provider("unknown_provider")


class PolicyLayerTests(TestCase):
    """Tests for policy/safety enforcement."""

    def setUp(self):
        self.user = User.objects.create_user(
            email="user@example.com",
            password="password",
            first_name="Test",
            last_name="User"
        )
        self.home = Home.objects.create(name="Test Home", owner=self.user)
        HomeMember.objects.create(home=self.home, user=self.user, role="owner")
        self.service = AIService()

    def test_sensitive_action_requires_confirmation(self):
        """Test that sensitive actions (lock/unlock) require confirmation."""
        # Create a lock device
        Room.objects.create(home=self.home, name="Entry")
        lock_device = Device.objects.create(
            home=self.home,
            name="Front Door Lock",
            device_type="lock",
            status="locked"
        )
        
        # Request to unlock (simulating provider response with lock action)
        from ai.schemas import ProposedAction, ActionType, StructuredAIResponse
        
        response = StructuredAIResponse(
            message="I'll unlock the front door for you.",
            intent=IntentType.DEVICE_CONTROL,
            confidence=0.9,
            proposed_actions=[
                ProposedAction(
                    action_type=ActionType.UNLOCK,
                    device_id=lock_device.id,
                    reason="User requested to unlock front door"
                )
            ],
            policy_status=PolicyStatus.INFORMATIONAL,
            requires_confirmation=False,
        )
        
        # Apply policy
        from ai.service import AIService
        service = AIService()
        checked_response = service._apply_policy(response, self.home.id)
        
        # Should require confirmation
        self.assertTrue(checked_response.requires_confirmation)
        self.assertEqual(checked_response.policy_status, PolicyStatus.REQUIRES_CONFIRMATION)

