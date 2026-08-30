from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APIClient

from core.models import Home, HomeMember

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
