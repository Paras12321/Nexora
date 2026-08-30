from django.contrib.auth import authenticate, get_user_model
from django.contrib.auth.password_validation import validate_password
from rest_framework import serializers

from core.models import Home, HomeMember

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
