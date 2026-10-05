"""
Self-service account deactivation — the one implementation shared by the web
"Deactivate My Account" page (core.views.account_deactivate_confirm) and the
API's POST /api/v1/auth/me/deactivate/, so both clients deactivate an account
identically.

Deactivating keeps the account and everything it posted: the user, profile,
listings, reviews and comments stay in the database and stay public. It only
stamps Profile.deactivated_at, stops push notifications to the user's
devices, and signs them out. Signing in again — by password, Google, or an
app token — reactivates the account automatically (see
core.signals.reactivate_on_login and core.api.authentication).

Permanent deletion is no longer self-service: users request it by email and
staff run `manage.py delete_account`, which uses core.account_deletion.
"""
from django.db import transaction
from django.utils import timezone

from .models import LoginHistory, MobileDevice, Profile, PushSubscription


def deactivate_account(user, ip_address=None, user_agent=''):
    """Marks `user`'s account deactivated. The caller signs the session out."""
    with transaction.atomic():
        profile, _ = Profile.objects.get_or_create(user=user)
        profile.deactivated_at = timezone.now()
        profile.save(update_fields=['deactivated_at'])
        # A deactivated account shouldn't keep getting notifications; the
        # devices re-register themselves after the next sign-in.
        PushSubscription.objects.filter(user=user).delete()
        MobileDevice.objects.filter(user=user).delete()
        LoginHistory.objects.create(
            user=user, event_type='deactivate', ip_address=ip_address, user_agent=(user_agent or '')[:300],
        )
    return profile


def reactivate_account(user, ip_address=None, user_agent=''):
    """
    Clears a deactivation when the user signs in again. Returns True if the
    account was deactivated (so callers can welcome them back), else False.
    One UPDATE either way, so it's cheap enough to run on every sign-in and
    every token-authenticated API request.
    """
    reactivated = Profile.objects.filter(user=user, deactivated_at__isnull=False).update(deactivated_at=None)
    if reactivated:
        LoginHistory.objects.create(
            user=user, event_type='reactivate', ip_address=ip_address, user_agent=(user_agent or '')[:300],
        )
    return bool(reactivated)
