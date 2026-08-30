from django.contrib.auth import get_user_model
from django.contrib.auth.tokens import default_token_generator
from django.db import IntegrityError
from django.http import JsonResponse
from django.utils.encoding import force_bytes, force_str
from django.utils.http import urlsafe_base64_decode, urlsafe_base64_encode
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from core.models import Home, HomeMember
from core.permissions import IsHomeMember, IsHomeOwner
from core.serializers import (
    HomeSerializer,
    HomeMemberSerializer,
    InviteMemberSerializer,
    LoginSerializer,
    PasswordResetConfirmSerializer,
    PasswordResetRequestSerializer,
    RegisterSerializer,
    UserSerializer,
)

User = get_user_model()


# ---------------------------------------------------------------------------
# Health (preserved from BE1)
# ---------------------------------------------------------------------------

def healthcheck(request):
    return JsonResponse({
        "status": "ok",
        "service": "nexora-backend",
    })


# ---------------------------------------------------------------------------
# Auth views
# ---------------------------------------------------------------------------

class RegisterView(APIView):
    """Create a new user account and return an auth token."""

    permission_classes = [AllowAny]

    def post(self, request):
        serializer = RegisterSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()
        token, _ = Token.objects.get_or_create(user=user)
        return Response(
            {
                "token": token.key,
                "user": UserSerializer(user).data,
            },
            status=status.HTTP_201_CREATED,
        )


class LoginView(APIView):
    """Authenticate with email + password and return an auth token."""

    permission_classes = [AllowAny]

    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.validated_data["user"]
        token, _ = Token.objects.get_or_create(user=user)
        return Response(
            {
                "token": token.key,
                "user": UserSerializer(user).data,
            },
            status=status.HTTP_200_OK,
        )


class LogoutView(APIView):
    """Delete the caller's auth token (server-side revocation)."""

    permission_classes = [IsAuthenticated]

    def post(self, request):
        # Delete the token associated with the current request
        Token.objects.filter(user=request.user).delete()
        return Response({"detail": "Logged out."}, status=status.HTTP_200_OK)


class MeView(APIView):
    """Return the profile of the currently authenticated user."""

    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response(UserSerializer(request.user).data, status=status.HTTP_200_OK)


class PasswordResetRequestView(APIView):
    """Generate a password-reset token for the given email.

    In production the token would be emailed; for the MVP it is returned
    directly in the response body.
    """

    permission_classes = [AllowAny]

    def post(self, request):
        serializer = PasswordResetRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        email = serializer.validated_data["email"].lower().strip()
        try:
            user = User.objects.get(email=email)
        except User.DoesNotExist:
            # Return 200 regardless to prevent email enumeration
            return Response(
                {"detail": "If that email is registered, a reset link has been sent."},
                status=status.HTTP_200_OK,
            )
        uid = urlsafe_base64_encode(force_bytes(user.pk))
        token = default_token_generator.make_token(user)
        return Response(
            {
                "detail": "If that email is registered, a reset link has been sent.",
                "uid": uid,
                "token": token,
            },
            status=status.HTTP_200_OK,
        )


class PasswordResetConfirmView(APIView):
    """Validate the reset token and set a new password."""

    permission_classes = [AllowAny]

    def post(self, request):
        serializer = PasswordResetConfirmSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            uid = force_str(urlsafe_base64_decode(serializer.validated_data["uid"]))
            user = User.objects.get(pk=uid)
        except (TypeError, ValueError, OverflowError, User.DoesNotExist):
            return Response(
                {"detail": "Invalid reset link."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        token = serializer.validated_data["token"]
        if not default_token_generator.check_token(user, token):
            return Response(
                {"detail": "Invalid or expired reset token."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        user.set_password(serializer.validated_data["new_password"])
        user.save()
        # Revoke any existing auth tokens so the user must log in again
        Token.objects.filter(user=user).delete()
        return Response(
            {"detail": "Password has been reset."},
            status=status.HTTP_200_OK,
        )


# ---------------------------------------------------------------------------
# Home views
# ---------------------------------------------------------------------------

class HomeListCreateView(APIView):
    """List the authenticated user's homes or create a new one."""

    permission_classes = [IsAuthenticated]

    def get(self, request):
        home_ids = HomeMember.objects.filter(user=request.user).values_list(
            "home_id", flat=True
        )
        homes = Home.objects.filter(id__in=home_ids)
        return Response(HomeSerializer(homes, many=True).data, status=status.HTTP_200_OK)

    def post(self, request):
        serializer = HomeSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        home = serializer.save(owner=request.user)
        # Automatically add the creator as owner member
        HomeMember.objects.create(home=home, user=request.user, role=HomeMember.Role.OWNER)
        return Response(HomeSerializer(home).data, status=status.HTTP_201_CREATED)


class HomeDetailView(APIView):
    """Retrieve, update, or delete a specific home."""

    permission_classes = [IsAuthenticated]

    def _get_home(self, home_id, user):
        """Return (home, error_response). error_response is None on success."""
        try:
            home = Home.objects.get(pk=home_id)
        except Home.DoesNotExist:
            return None, Response(
                {"detail": "Home not found."},
                status=status.HTTP_404_NOT_FOUND,
            )
        if not IsHomeMember().has_object_permission(None, None, home) and not HomeMember.objects.filter(home=home, user=user).exists():
            return None, Response(
                {"detail": "You are not a member of this home."},
                status=status.HTTP_403_FORBIDDEN,
            )
        return home, None

    def get(self, request, home_id):
        try:
            home = Home.objects.get(pk=home_id)
        except Home.DoesNotExist:
            return Response({"detail": "Home not found."}, status=status.HTTP_404_NOT_FOUND)
        if not HomeMember.objects.filter(home=home, user=request.user).exists():
            return Response({"detail": "You are not a member of this home."}, status=status.HTTP_403_FORBIDDEN)
        return Response(HomeSerializer(home).data, status=status.HTTP_200_OK)

    def put(self, request, home_id):
        try:
            home = Home.objects.get(pk=home_id)
        except Home.DoesNotExist:
            return Response({"detail": "Home not found."}, status=status.HTTP_404_NOT_FOUND)
        if home.owner != request.user:
            return Response({"detail": "Only the owner can update this home."}, status=status.HTTP_403_FORBIDDEN)
        serializer = HomeSerializer(home, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(HomeSerializer(home).data, status=status.HTTP_200_OK)

    def delete(self, request, home_id):
        try:
            home = Home.objects.get(pk=home_id)
        except Home.DoesNotExist:
            return Response({"detail": "Home not found."}, status=status.HTTP_404_NOT_FOUND)
        if home.owner != request.user:
            return Response({"detail": "Only the owner can delete this home."}, status=status.HTTP_403_FORBIDDEN)
        home.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


# ---------------------------------------------------------------------------
# HomeMember views
# ---------------------------------------------------------------------------

class HomeMemberListCreateView(APIView):
    """List members of a home or invite a new member."""

    permission_classes = [IsAuthenticated]

    def get(self, request, home_id):
        try:
            home = Home.objects.get(pk=home_id)
        except Home.DoesNotExist:
            return Response({"detail": "Home not found."}, status=status.HTTP_404_NOT_FOUND)
        if not HomeMember.objects.filter(home=home, user=request.user).exists():
            return Response({"detail": "You are not a member of this home."}, status=status.HTTP_403_FORBIDDEN)
        members = HomeMember.objects.filter(home=home).select_related("user")
        return Response(HomeMemberSerializer(members, many=True).data, status=status.HTTP_200_OK)

    def post(self, request, home_id):
        try:
            home = Home.objects.get(pk=home_id)
        except Home.DoesNotExist:
            return Response({"detail": "Home not found."}, status=status.HTTP_404_NOT_FOUND)
        if home.owner != request.user:
            return Response({"detail": "Only the owner can invite members."}, status=status.HTTP_403_FORBIDDEN)
        serializer = InviteMemberSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        email = serializer.validated_data["email"]
        invitee = User.objects.get(email=email)
        if HomeMember.objects.filter(home=home, user=invitee).exists():
            return Response({"detail": "User is already a member of this home."}, status=status.HTTP_400_BAD_REQUEST)
        member = HomeMember.objects.create(home=home, user=invitee, role=HomeMember.Role.MEMBER)
        return Response(HomeMemberSerializer(member).data, status=status.HTTP_201_CREATED)


class HomeMemberRemoveView(APIView):
    """Remove a member from a home (owner only)."""

    permission_classes = [IsAuthenticated]

    def delete(self, request, home_id, member_id):
        try:
            home = Home.objects.get(pk=home_id)
        except Home.DoesNotExist:
            return Response({"detail": "Home not found."}, status=status.HTTP_404_NOT_FOUND)
        if home.owner != request.user:
            return Response({"detail": "Only the owner can remove members."}, status=status.HTTP_403_FORBIDDEN)
        try:
            membership = HomeMember.objects.get(pk=member_id, home=home)
        except HomeMember.DoesNotExist:
            return Response({"detail": "Member not found."}, status=status.HTTP_404_NOT_FOUND)
        if membership.user == home.owner:
            return Response({"detail": "The owner cannot be removed."}, status=status.HTTP_400_BAD_REQUEST)
        membership.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class LeaveHomeView(APIView):
    """Allow a member (not the owner) to leave a home."""

    permission_classes = [IsAuthenticated]

    def post(self, request, home_id):
        try:
            home = Home.objects.get(pk=home_id)
        except Home.DoesNotExist:
            return Response({"detail": "Home not found."}, status=status.HTTP_404_NOT_FOUND)
        if home.owner == request.user:
            return Response({"detail": "The owner cannot leave the home."}, status=status.HTTP_400_BAD_REQUEST)
        try:
            membership = HomeMember.objects.get(home=home, user=request.user)
        except HomeMember.DoesNotExist:
            return Response({"detail": "You are not a member of this home."}, status=status.HTTP_403_FORBIDDEN)
        membership.delete()
        return Response({"detail": "You have left the home."}, status=status.HTTP_200_OK)
