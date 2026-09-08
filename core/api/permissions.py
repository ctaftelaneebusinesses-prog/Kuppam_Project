from rest_framework.permissions import BasePermission

from ..models import Profile


def get_profile(user):
    """Same get-or-create-on-first-touch pattern as onboarding_required (core/decorators.py)."""
    if not user or not user.is_authenticated:
        return None
    profile, _ = Profile.objects.get_or_create(user=user)
    return profile


class IsActiveAccount(BasePermission):
    """
    Blocks a blocked account from the API entirely — mirrors
    onboarding_required's is_blocked check (forced logout) on the web.
    Anonymous requests pass through untouched; each view's own permission
    classes (typically paired with IsAuthenticated) decide whether
    anonymous access is allowed at all.

    Deliberately does NOT check is_suspended here: on the web, suspension
    only restricts listing management (Profile.can_manage_category,
    core.views._can_manage_post), not ordinary community actions like
    favoriting/liking/commenting or reading notifications — a suspended
    admin keeps doing those exactly like any other signed-in user. The
    listing-management endpoints already re-derive and enforce is_suspended
    themselves via those same shared functions, so this class blocking it
    too would over-restrict actions the web never restricts.
    """
    message = 'This account has been blocked.'

    def has_permission(self, request, view):
        if not request.user.is_authenticated:
            return True
        profile = get_profile(request.user)
        return not profile.is_blocked
