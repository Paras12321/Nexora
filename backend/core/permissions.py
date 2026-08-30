from rest_framework.permissions import BasePermission

from core.models import HomeMember


class IsHomeOwner(BasePermission):
    """Allow access only to the owner of the Home."""

    def has_object_permission(self, request, view, obj):
        # obj is a Home instance
        return obj.owner == request.user


class IsHomeMember(BasePermission):
    """Allow access to any member (including owner) of the Home."""

    def has_object_permission(self, request, view, obj):
        # obj is a Home instance
        return HomeMember.objects.filter(home=obj, user=request.user).exists()
