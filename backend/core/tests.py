from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APIClient

from core.models import Device, DeviceCapability, GoogleDevice, Home, HomeMember, Room, RoomMember, RoomPreference

User = get_user_model()


class HealthEndpointTests(TestCase):
    def test_health_endpoint(self):
        response = self.client.get(reverse("healthcheck"))
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ok", "service": "nexora-backend"})


class AuthTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.register_url = reverse("register")
        self.login_url = reverse("login")
        self.logout_url = reverse("logout")
        self.me_url = reverse("me")
        self.user_data = {
            "email": "test@example.com",
            "password": "StrongPassword123!",
            "first_name": "Test",
            "last_name": "User",
        }

    def test_register_success(self):
        response = self.client.post(self.register_url, self.user_data)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertIn("token", response.data)
        self.assertEqual(response.data["user"]["email"], self.user_data["email"])

    def test_register_duplicate_email(self):
        User.objects.create_user(**self.user_data)
        response = self.client.post(self.register_url, self.user_data)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_login_success(self):
        User.objects.create_user(**self.user_data)
        response = self.client.post(self.login_url, {
            "email": self.user_data["email"],
            "password": self.user_data["password"],
        })
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("token", response.data)

    def test_login_invalid_credentials(self):
        User.objects.create_user(**self.user_data)
        response = self.client.post(self.login_url, {
            "email": self.user_data["email"],
            "password": "WrongPassword!",
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_logout(self):
        user = User.objects.create_user(**self.user_data)
        response = self.client.post(self.login_url, {
            "email": self.user_data["email"],
            "password": self.user_data["password"],
        })
        token = response.data["token"]
        
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {token}")
        response = self.client.post(self.logout_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        
        response = self.client.get(self.me_url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_me_unauthenticated(self):
        response = self.client.get(self.me_url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


class HomeTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.owner = User.objects.create_user(
            email="owner@example.com", password="StrongPassword123!", first_name="Owner", last_name="User"
        )
        self.member = User.objects.create_user(
            email="member@example.com", password="StrongPassword123!", first_name="Member", last_name="User"
        )
        self.other = User.objects.create_user(
            email="other@example.com", password="StrongPassword123!", first_name="Other", last_name="User"
        )
        
        # Authenticate as owner by default
        response = self.client.post(reverse("login"), {"email": "owner@example.com", "password": "StrongPassword123!"})
        self.owner_token = response.data["token"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {self.owner_token}")

    def test_create_home(self):
        url = reverse("home-list-create")
        response = self.client.post(url, {"name": "My Home"})
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["name"], "My Home")
        self.assertEqual(Home.objects.count(), 1)
        self.assertEqual(HomeMember.objects.count(), 1)
        self.assertEqual(HomeMember.objects.first().role, HomeMember.Role.OWNER)

    def test_list_homes(self):
        home = Home.objects.create(name="My Home", owner=self.owner)
        HomeMember.objects.create(home=home, user=self.owner, role=HomeMember.Role.OWNER)
        
        url = reverse("home-list-create")
        response = self.client.get(url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data), 1)

    def test_home_detail_access(self):
        home = Home.objects.create(name="My Home", owner=self.owner)
        HomeMember.objects.create(home=home, user=self.owner, role=HomeMember.Role.OWNER)
        
        url = reverse("home-detail", args=[home.id])
        response = self.client.get(url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        # Test unauthorized access
        self.client.credentials() # clear auth
        response = self.client.post(reverse("login"), {"email": "other@example.com", "password": "StrongPassword123!"})
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {response.data['token']}")
        
        response = self.client.get(url)
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_invite_member(self):
        home = Home.objects.create(name="My Home", owner=self.owner)
        HomeMember.objects.create(home=home, user=self.owner, role=HomeMember.Role.OWNER)
        
        url = reverse("home-member-list-create", args=[home.id])
        response = self.client.post(url, {"email": "member@example.com"})
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(HomeMember.objects.count(), 2)

    def test_leave_home(self):
        home = Home.objects.create(name="My Home", owner=self.owner)
        HomeMember.objects.create(home=home, user=self.owner, role=HomeMember.Role.OWNER)
        HomeMember.objects.create(home=home, user=self.member, role=HomeMember.Role.MEMBER)
        
        # Switch to member
        response = self.client.post(reverse("login"), {"email": "member@example.com", "password": "StrongPassword123!"})
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {response.data['token']}")
        
        url = reverse("home-leave", args=[home.id])
        response = self.client.post(url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(HomeMember.objects.count(), 1)


class BE4Tests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.owner = User.objects.create_user(
            email="owner_be4@example.com", password="StrongPassword123!", first_name="Owner", last_name="User"
        )
        self.home = Home.objects.create(name="BE4 Home", owner=self.owner)
        HomeMember.objects.create(home=self.home, user=self.owner, role=HomeMember.Role.OWNER)

        response = self.client.post(reverse("login"), {"email": "owner_be4@example.com", "password": "StrongPassword123!"})
        self.token = response.data["token"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {self.token}")

    def test_presence_event(self):
        url = reverse("presence-event-list-create", args=[self.home.id])
        response = self.client.post(url, {"state": "away"})
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["state"], "away")

    def test_security_event_valid(self):
        url = reverse("security-event-list-create", args=[self.home.id])
        response = self.client.post(url, {"mode": "armed_home"})
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["mode"], "armed_home")

    def test_security_event_invalid(self):
        url = reverse("security-event-list-create", args=[self.home.id])
        response = self.client.post(url, {"mode": "invalid_mode"})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_decision_log_approval(self):
        # AI proposes an action
        url = reverse("decision-log-list-create", args=[self.home.id])
        response = self.client.post(url, {
            "source": "AI_Agent",
            "decision": "turn_off_lights",
            "reason": "Room is empty"
        })
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["status"], "pending_approval")
        log_id = response.data["id"]

        # Approve the action
        approve_url = reverse("decision-log-approve", args=[self.home.id, log_id])
        response = self.client.post(approve_url, {"action": "approve"})
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        # Verify it was logged in ActivityLog
        from core.models import ActivityLog
        self.assertTrue(ActivityLog.objects.filter(home=self.home, action__contains="turn_off_lights").exists())


class RoomAndDeviceTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.owner = User.objects.create_user(
            email="owner@example.com", password="StrongPassword123!", first_name="Owner", last_name="User"
        )
        self.member = User.objects.create_user(
            email="member@example.com", password="StrongPassword123!", first_name="Member", last_name="User"
        )
        self.home = Home.objects.create(name="My Home", owner=self.owner)
        self.owner_membership = HomeMember.objects.create(home=self.home, user=self.owner, role=HomeMember.Role.OWNER)
        self.member_membership = HomeMember.objects.create(home=self.home, user=self.member, role=HomeMember.Role.MEMBER)
        response = self.client.post(reverse("login"), {"email": "owner@example.com", "password": "StrongPassword123!"})
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {response.data['token']}")

    def test_room_assignment_and_preference_are_room_scoped(self):
        room_url = reverse("room-list-create", args=[self.home.id])
        response = self.client.post(room_url, {"name": "Living Room"})
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        room = Room.objects.get(home=self.home, name="Living Room")
        assign_url = reverse("room-member-list-create", args=[self.home.id, room.id])
        response = self.client.post(assign_url, {"member_id": self.member_membership.id})
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(RoomMember.objects.filter(room=room, home_member=self.member_membership).count(), 1)

        pref_url = reverse("room-preference-list-create", args=[self.home.id, room.id])
        response = self.client.post(pref_url, {
            "preferences": {
                "temperature_c": 21.5,
                "lighting_mode": "warm",
                "eco_mode": True,
            }
        }, format="json")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(RoomPreference.objects.filter(room=room).count(), 1)
        self.assertTrue(RoomPreference.objects.get(room=room).preferences["eco_mode"])

    def test_device_metadata_and_google_mapping_are_created_for_home_devices(self):
        room = Room.objects.create(home=self.home, name="Bedroom")
        device_url = reverse("device-list-create", args=[self.home.id])
        response = self.client.post(device_url, {
            "name": "Bedroom Lamp",
            "room_id": room.id,
            "device_type": "light",
            "manufacturer": "Google",
            "model": "Nest Mini Lamp",
            "serial_number": "GL-001",
            "status": "online",
        })
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        device = Device.objects.get(name="Bedroom Lamp", home=self.home)

        self.assertEqual(device.room_id, room.id)
        self.assertTrue(device.google_mapping is not None)

        capability_url = reverse("device-capability-list-create", args=[self.home.id, device.id])
        response = self.client.post(capability_url, {
            "capability_type": "brightness",
            "name": "Brightness",
            "supported": True,
            "metadata": {"min": 0, "max": 100}
        }, format="json")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(DeviceCapability.objects.filter(device=device).count(), 1)
