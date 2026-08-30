from django.conf import settings
from django.contrib.auth.models import AbstractBaseUser, BaseUserManager, PermissionsMixin
from django.db import models
from django.utils import timezone


class UserManager(BaseUserManager):
    """Manager for the custom email-based User model."""

    def create_user(self, email, password=None, **extra_fields):
        if not email:
            raise ValueError("An email address is required.")
        email = self.normalize_email(email)
        extra_fields.setdefault("is_active", True)
        extra_fields.setdefault("is_staff", False)
        extra_fields.setdefault("is_superuser", False)
        user = self.model(email=email, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, password=None, **extra_fields):
        extra_fields.setdefault("is_staff", True)
        extra_fields.setdefault("is_superuser", True)
        if extra_fields.get("is_staff") is not True:
            raise ValueError("Superuser must have is_staff=True.")
        if extra_fields.get("is_superuser") is not True:
            raise ValueError("Superuser must have is_superuser=True.")
        return self.create_user(email, password, **extra_fields)


class User(AbstractBaseUser, PermissionsMixin):
    """Custom user model that uses email as the unique identifier."""

    email = models.EmailField(unique=True)
    first_name = models.CharField(max_length=150, blank=True)
    last_name = models.CharField(max_length=150, blank=True)
    is_active = models.BooleanField(default=True)
    is_staff = models.BooleanField(default=False)
    date_joined = models.DateTimeField(default=timezone.now)

    objects = UserManager()

    USERNAME_FIELD = "email"
    REQUIRED_FIELDS = ["first_name", "last_name"]

    class Meta:
        db_table = "nexora_user"

    def __str__(self):
        return self.email


class Home(models.Model):
    """A household managed through NEXORA."""

    name = models.CharField(max_length=255)
    owner = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="owned_homes",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "nexora_home"

    def __str__(self):
        return self.name


class HomeMember(models.Model):
    """Links a user to a home with a specific role."""

    class Role(models.TextChoices):
        OWNER = "owner", "Owner"
        MEMBER = "member", "Member"

    home = models.ForeignKey(Home, on_delete=models.CASCADE, related_name="members")
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="home_memberships",
    )
    role = models.CharField(max_length=10, choices=Role.choices, default=Role.MEMBER)
    joined_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "nexora_home_member"
        constraints = [
            models.UniqueConstraint(fields=["home", "user"], name="unique_home_user"),
        ]

    def __str__(self):
        return f"{self.user.email} in {self.home.name} ({self.role})"


class Room(models.Model):
    """A room within a home with optional member assignment and preferences."""

    home = models.ForeignKey(Home, on_delete=models.CASCADE, related_name="rooms")
    name = models.CharField(max_length=255)
    description = models.TextField(blank=True, default="")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "nexora_room"
        constraints = [
            models.UniqueConstraint(fields=["home", "name"], name="unique_home_room_name"),
        ]

    def __str__(self):
        return f"{self.home.name}: {self.name}"


class RoomMember(models.Model):
    """Member assignment for a room within a home."""

    room = models.ForeignKey(Room, on_delete=models.CASCADE, related_name="member_assignments")
    home_member = models.ForeignKey(
        HomeMember,
        on_delete=models.CASCADE,
        related_name="room_assignments",
    )
    assigned_at = models.DateTimeField(auto_now_add=True)
    is_primary = models.BooleanField(default=False)

    class Meta:
        db_table = "nexora_room_member"
        constraints = [
            models.UniqueConstraint(fields=["room", "home_member"], name="unique_room_home_member"),
            models.UniqueConstraint(
                fields=["room"],
                condition=models.Q(is_primary=True),
                name="unique_primary_room_member",
            ),
        ]

    def __str__(self):
        return f"{self.home_member.user.email} -> {self.room.name}"


class RoomPreference(models.Model):
    """Per-room preference configuration stored as JSON for the room."""

    room = models.OneToOneField(Room, on_delete=models.CASCADE, related_name="preferences")
    preferences = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "nexora_room_preference"

    def __str__(self):
        return f"Preferences for {self.room.name}"


class Device(models.Model):
    """A smart device associated with a home and optionally a room."""

    home = models.ForeignKey(Home, on_delete=models.CASCADE, related_name="devices")
    room = models.ForeignKey(Room, on_delete=models.SET_NULL, null=True, blank=True, related_name="devices")
    name = models.CharField(max_length=255)
    device_type = models.CharField(max_length=80)
    manufacturer = models.CharField(max_length=120, blank=True, default="")
    model = models.CharField(max_length=120, blank=True, default="")
    serial_number = models.CharField(max_length=150, blank=True, default="")
    status = models.CharField(max_length=30, default="offline")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "nexora_device"
        constraints = [
            models.UniqueConstraint(
                fields=["home", "serial_number"],
                condition=models.Q(serial_number__gt=""),
                name="unique_device_home_serial_number",
            ),
        ]

    def __str__(self):
        return f"{self.home.name}: {self.name} ({self.device_type})"


class GoogleDevice(models.Model):
    """Google Home integration metadata for a NEXORA device."""

    device = models.OneToOneField(Device, on_delete=models.CASCADE, related_name="google_mapping")
    google_device_id = models.CharField(max_length=255, unique=True)
    structure_id = models.CharField(max_length=255, blank=True, default="")
    name = models.CharField(max_length=255, blank=True, default="")
    status = models.CharField(max_length=30, default="unknown")
    metadata = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "nexora_google_device"

    def __str__(self):
        return self.google_device_id


class DeviceCapability(models.Model):
    """Capability metadata describing one supported or advertised feature."""

    device = models.ForeignKey(Device, on_delete=models.CASCADE, related_name="capabilities")
    capability_type = models.CharField(max_length=80)
    name = models.CharField(max_length=120)
    supported = models.BooleanField(default=True)
    metadata = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "nexora_device_capability"
        constraints = [
            models.UniqueConstraint(fields=["device", "capability_type"], name="unique_device_capability_type"),
        ]

    def __str__(self):
        return f"{self.device.name}: {self.name}"
