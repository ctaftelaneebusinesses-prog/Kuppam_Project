"""
Shared self-service account/data deletion, used by both the web view
(core.views.account_delete) and the API endpoint (core.api.views.me's DELETE
method) so there is exactly one implementation of what "delete my account"
actually does — see the ACCOUNT DELETION IMPLEMENTATION REPORT for the full
per-model dependency map this follows.

Summary of what happens to a deleted user's data:

  Deleted outright (via Django's cascade, since none of it has any value or
  meaning to anyone but the account owner):
    Profile (+ its profile_photo file, deleted explicitly below — Django
    doesn't delete FileField files on cascade), Like, Favorite,
    PushSubscription, LoginHistory, Notification, AdminRequest,
    AdminCategoryPermission, AdminCityPermission, UserPermission.

  Detached, not deleted (user set to NULL — see the 0048 migration):
    Comment, Review — someone else's replies/the listing's rating average
    depend on these still existing; only the personal link is removed.
    Report — kept as moderation trail, same reasoning as AuditLog.actor
    (already SET_NULL) which this mirrors.

  Left alone entirely (already correctly decoupled from the account by the
  existing architecture, not something this feature needed to change):
    Business/Property/Job/Event/News/Project/Scholarship/LostFound listings
    (ListingMixin.owner is already on_delete=SET_NULL — a listing an owner
    created stays live and public, same as when staff removes a user via
    the existing dashboard_users_bulk_delete flow). PostImage/PostVideo
    (uploaded_by is already SET_NULL). AuditLog (actor is already SET_NULL).
    A user who also wants their own listings gone must delete them
    individually from My Listings first — this endpoint does not do that
    for them, since a listing is community content, not private data.

  Deleted from Supabase, not just locally:
    The Supabase Auth identity itself (delete_supabase_user), so the same
    Google account can't sign back in and have resolve_supabase_identity()
    quietly recreate the "deleted" account.
"""
import logging

from django.db import transaction

from .models import Profile
from .supabase_auth import SupabaseAuthError, delete_supabase_user

logger = logging.getLogger(__name__)


class AccountDeletionError(Exception):
    """Raised when `user` has already been deleted (e.g. a duplicate/replayed request)."""


def delete_user_account(user):
    """
    Permanently deletes `user`'s OneTownCity account and the personal data
    listed above. Idempotent against being called twice with the same
    (by-then-stale) `user` instance: Django sets `user.pk` to None once
    `user.delete()` succeeds, so a second call raises AccountDeletionError
    instead of re-running (or erroring on) a delete against a row that's
    already gone.
    """
    if not user.pk:
        raise AccountDeletionError('This account has already been deleted.')

    profile = Profile.objects.filter(user=user).first()
    supabase_uid = profile.supabase_uid if profile else None

    with transaction.atomic():
        if profile and profile.profile_photo:
            profile.profile_photo.delete(save=False)
        user.delete()

    if supabase_uid:
        try:
            delete_supabase_user(supabase_uid)
        except SupabaseAuthError:
            # The local account is already gone — that's the source of truth
            # for the app itself, so a Supabase-side failure here shouldn't
            # (and structurally can't, since user.delete() already
            # committed) undo it. Logged so it can be cleaned up manually;
            # worst case the orphaned Supabase identity can only ever
            # recreate a *blank* new local account on next sign-in, not
            # restore anything about the deleted one.
            logger.exception(
                'Local account deletion for supabase_uid=%s committed, but its Supabase Auth '
                'identity could not be removed.', supabase_uid,
            )
