from django.contrib.auth import authenticate, get_user_model
from django.contrib.auth.password_validation import validate_password
from rest_framework import serializers

from core.models import (
    Device,
    DeviceCapability,
    GoogleDevice,
    Home,
    HomeMember,
    Room,
    RoomMember,
    RoomPreference,
)

User = get_user_model()


class RegisterSerializer(serializers.Serializer):
    """Validates registration input and creates a new user."""

    email = serializers.EmailField()
    password = serializers.CharField(write_only=True, min_length=8)
    first_name = serializers.CharField(max_length=150)
    last_name = serializers.CharField(max_length=150)

    def validate_email(self, value):
        email = value.lower().strip()
        if User.objects.filter(email=email).exists():
            raise serializers.ValidationError("A user with this email already exists.")
        return email

    def validate_password(self, value):
        validate_password(value)
        return value

    def create(self, validated_data):
        return User.objects.create_user(**validated_data)


class LoginSerializer(serializers.Serializer):
    """Validates login credentials."""

    email = serializers.EmailField()
    password = serializers.CharField(write_only=True)

    def validate(self, attrs):
        email = attrs["email"].lower().strip()
        password = attrs["password"]
        user = authenticate(email=email, password=password)
        if user is None:
            raise serializers.ValidationError("Invalid email or password.")
        if not user.is_active:
            raise serializers.ValidationError("This account is inactive.")
        attrs["user"] = user
        return attrs


class UserSerializer(serializers.ModelSerializer):
    """Read-only representation of a user."""

    class Meta:
        model = User
        fields = ["id", "email", "first_name", "last_name", "date_joined"]
        read_only_fields = fields


class PasswordResetRequestSerializer(serializers.Serializer):
    """Accepts an email for password reset request."""

    email = serializers.EmailField()


class PasswordResetConfirmSerializer(serializers.Serializer):
    """Validates and applies password reset."""

    uid = serializers.CharField()
    token = serializers.CharField()
    new_password = serializers.CharField(write_only=True, min_length=8)

    def validate_new_password(self, value):
        validate_password(value)
        return value


class HomeSerializer(serializers.ModelSerializer):
    """Serializer for Home create / read / update."""

    owner_email = serializers.EmailField(source="owner.email", read_only=True)

    class Meta:
        model = Home
        fields = ["id", "name", "owner", "owner_email", "created_at", "updated_at"]
        read_only_fields = ["id", "owner", "owner_email", "created_at", "updated_at"]


class HomeMemberSerializer(serializers.ModelSerializer):
    """Read-only representation of a home member."""

    email = serializers.EmailField(source="user.email", read_only=True)
    first_name = serializers.CharField(source="user.first_name", read_only=True)
    last_name = serializers.CharField(source="user.last_name", read_only=True)

    class Meta:
        model = HomeMember
        fields = ["id", "email", "first_name", "last_name", "role", "joined_at"]
        read_only_fields = fields


class InviteMemberSerializer(serializers.Serializer):
    """Validates an invitation to add a user to a home."""

    email = serializers.EmailField()

    def validate_email(self, value):
        email = value.lower().strip()
        try:
            User.objects.get(email=email)
        except User.DoesNotExist:
            raise serializers.ValidationError("No user with this email exists.")
        return email


class RoomSerializer(serializers.ModelSerializer):
    """Serializer for room data."""

    home_id = serializers.IntegerField(source="home.id", read_only=True)

    class Meta:
        model = Room
        fields = ["id", "home_id", "name", "description", "created_at", "updated_at"]
        read_only_fields = ["id", "home_id", "created_at", "updated_at"]


class RoomMemberSerializer(serializers.ModelSerializer):
    """Serializer for room assignment to a HomeMember."""

    member_id = serializers.IntegerField(source="home_member.id", read_only=True)
    email = serializers.EmailField(source="home_member.user.email", read_only=True)
    role = serializers.CharField(source="home_member.role", read_only=True)

    class Meta:
        model = RoomMember
        fields = ["id", "member_id", "email", "role", "is_primary", "assigned_at"]
        read_only_fields = fields


class AssignRoomMemberSerializer(serializers.Serializer):
    """Assign a home member to a room."""

    member_id = serializers.IntegerField()

    def validate_member_id(self, value):
        try:
            membership = HomeMember.objects.get(pk=value)
        except HomeMember.DoesNotExist:
            raise serializers.ValidationError("Home member not found.")
        self.context["home_member"] = membership
        return value


class RoomPreferenceSerializer(serializers.ModelSerializer):
    """Serializer for room-scoped preference configuration."""

    class Meta:
        model = RoomPreference
        fields = ["id", "room", "preferences", "created_at", "updated_at"]
        read_only_fields = ["id", "room", "created_at", "updated_at"]

    def validate_preferences(self, value):
        if not isinstance(value, dict):
            raise serializers.ValidationError("Preferences must be a JSON object.")
        return value


class GoogleDeviceSerializer(serializers.ModelSerializer):
    """Metadata for a Google Home device mapping."""

    class Meta:
        model = GoogleDevice
        fields = ["id", "google_device_id", "structure_id", "name", "status", "metadata", "created_at", "updated_at"]
        read_only_fields = fields


class DeviceSerializer(serializers.ModelSerializer):
    """Device metadata and Google mapping payload."""

    room_id = serializers.PrimaryKeyRelatedField(source="room", queryset=Room.objects.all(), required=False, allow_null=True)
    google_mapping = GoogleDeviceSerializer(read_only=True)

    class Meta:
        model = Device
        fields = [
            "id",
            "home",
            "room_id",
            "name",
            "device_type",
            "manufacturer",
            "model",
            "serial_number",
            "status",
            "google_mapping",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "home", "google_mapping", "created_at", "updated_at"]

    def create(self, validated_data):
        device = Device.objects.create(**validated_data)
        if not hasattr(device, "google_mapping"):
            google_id = f"google-device-{device.pk}-{device.name.lower().replace(' ', '-')}"
            GoogleDevice.objects.create(
                device=device,
                google_device_id=google_id,
                name=device.name,
                status=device.status,
            )
        return device


class DeviceCapabilitySerializer(serializers.ModelSerializer):
    """Capability metadata for a device."""

    class Meta:
        model = DeviceCapability
        fields = ["id", "device", "capability_type", "name", "supported", "metadata", "created_at", "updated_at"]
        read_only_fields = ["id", "device", "created_at", "updated_at"]

    def validate_metadata(self, value):
        if value is None:
            return {}
        if not isinstance(value, dict):
            raise serializers.ValidationError("Capability metadata must be a JSON object.")
        return value
