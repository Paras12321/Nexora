"""Context builder for authorized AI requests.

This module is responsible for safely retrieving and constructing context
that can be safely passed to the AI layer. It enforces authorization
and excludes sensitive information.
"""

from typing import List, Dict, Any, Optional

from django.contrib.auth import get_user_model
from django.db.models import Q

from core.models import (
    Home,
    HomeMember,
    Room,
    Device,
    SecurityEvent,
    PresenceEvent,
)
from ai.exceptions import ContextBuildingError, UnauthorizedContextAccess
from ai.schemas import AIRequestContext

User = get_user_model()


class ContextBuilder:
    """Builds authorized context for AI requests from a user."""

    @staticmethod
    def build(user_id: int, home_id: Optional[int] = None) -> AIRequestContext:
        """
        Build authorized context for a user, optionally scoped to a specific home.

        Args:
            user_id: Authenticated user ID
            home_id: Optional home to scope context to. If None, uses user's primary home.

        Returns:
            AIRequestContext with authorized data

        Raises:
            ContextBuildingError: If context cannot be built
            UnauthorizedContextAccess: If user doesn't have access to the requested home
        """
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            raise ContextBuildingError(f"User {user_id} not found")

        # Determine which homes the user has access to
        accessible_homes = ContextBuilder._get_accessible_homes(user)
        if not accessible_homes:
            raise ContextBuildingError(f"User {user_id} has no accessible homes")

        # If home_id is specified, verify user has access
        if home_id is not None:
            if home_id not in accessible_homes:
                raise UnauthorizedContextAccess(
                    f"User {user_id} does not have access to home {home_id}"
                )
            primary_home_id = home_id
        else:
            # Use the first accessible home as primary
            primary_home_id = accessible_homes[0]

        try:
            home = Home.objects.get(id=primary_home_id)
        except Home.DoesNotExist:
            raise ContextBuildingError(f"Home {primary_home_id} not found")

        # Build authorized device and room lists
        devices = ContextBuilder._get_authorized_devices(home)
        rooms = ContextBuilder._get_authorized_rooms(home)

        # Get current security and presence state
        security_state = ContextBuilder._get_latest_security_state(home)
        presence_state = ContextBuilder._get_latest_presence_state(home)

        return AIRequestContext(
            user_id=user_id,
            home_id=primary_home_id,
            accessible_homes=accessible_homes,
            devices=devices,
            rooms=rooms,
            security_state=security_state,
            presence_state=presence_state,
        )

    @staticmethod
    def _get_accessible_homes(user: User) -> List[int]:
        """Get list of home IDs accessible to the user."""
        # User is a member or owner of homes via HomeMember
        memberships = HomeMember.objects.filter(user=user).values_list(
            "home_id", flat=True
        )
        return list(memberships)

    @staticmethod
    def _get_authorized_devices(home: Home) -> List[Dict[str, Any]]:
        """Get list of devices in a home (safe representation for AI)."""
        devices = Device.objects.filter(home=home).select_related("room")
        device_list = []
        for device in devices:
            device_dict = {
                "id": device.id,
                "name": device.name,
                "type": device.device_type,
                "room_id": device.room_id if device.room else None,
                "room_name": device.room.name if device.room else None,
                "status": device.status,
                "manufacturer": device.manufacturer,
                "model": device.model,
                # Explicitly NOT including serial_number or any sensitive data
            }
            device_list.append(device_dict)
        return device_list

    @staticmethod
    def _get_authorized_rooms(home: Home) -> List[Dict[str, Any]]:
        """Get list of rooms in a home (safe representation for AI)."""
        rooms = Room.objects.filter(home=home)
        room_list = []
        for room in rooms:
            room_dict = {
                "id": room.id,
                "name": room.name,
                "description": room.description,
                "device_count": room.devices.count(),
            }
            room_list.append(room_dict)
        return room_list

    @staticmethod
    def _get_latest_security_state(home: Home) -> Optional[str]:
        """Get the most recent security event state for the home."""
        latest_event = (
            SecurityEvent.objects.filter(home=home)
            .order_by("-timestamp")
            .first()
        )
        return latest_event.mode if latest_event else None

    @staticmethod
    def _get_latest_presence_state(home: Home) -> Optional[str]:
        """Get the most recent presence event state for the home."""
        latest_event = (
            PresenceEvent.objects.filter(home=home)
            .order_by("-timestamp")
            .first()
        )
        return latest_event.state if latest_event else None
