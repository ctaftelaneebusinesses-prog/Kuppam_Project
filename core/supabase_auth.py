"""
Server-side verification of Supabase-issued Google sign-ins.

The frontend runs the actual Google OAuth dance via supabase-js. Once
Supabase hands back a session, we take its access_token and ask the
Supabase Auth API who it belongs to — this is the only step that matters
for security, since it proves the token is genuine without us having to
manage JWT secrets or key rotation locally.
"""
import re

from django.conf import settings
from django.contrib.auth import get_user_model
from django.utils import timezone
from supabase import create_client


class SupabaseAuthError(Exception):
    pass


_client = None


def get_supabase_client():
    global _client
    if _client is None:
        if not settings.SUPABASE_URL or not settings.SUPABASE_ANON_KEY:
            raise SupabaseAuthError('Supabase is not configured (SUPABASE_URL / SUPABASE_ANON_KEY missing).')
        _client = create_client(settings.SUPABASE_URL, settings.SUPABASE_ANON_KEY)
    return _client


def fetch_supabase_user(access_token):
    """
    Returns the Supabase auth user for a valid access token, or raises SupabaseAuthError.

    Phase 5.5 note — this call goes over the network to Supabase's Auth API
    on every single authenticated request (no caching, no local JWT
    verification). Two things were deliberately NOT done here, both
    documented rather than guessed at:

    1. Local JWT verification: would need the exact signing algorithm,
       issuer, audience, JWKS/public-key source, key-rotation behavior, and
       Supabase project configuration confirmed first — none of that could
       be verified in the Phase 5.5 session (this sandbox's outbound network
       reaches the Supabase Postgres pooler host fine but times out reaching
       the Supabase REST/Auth API host, *.supabase.co, so even the JWKS
       endpoint couldn't be inspected live), and no JWT secret or public key
       material exists anywhere in this project's config to work from
       either. Per the explicit rule this was reviewed under: if it isn't
       verified, don't change authentication.

    2. Short-TTL result caching (NOT implemented, no measured benefit could
       be established without reaching the real endpoint — but specified in
       case a future session has working connectivity):
         - Cache key: a hash (not the raw token — never log or key on it
           directly) of the access_token, e.g. sha256(access_token).
         - Value: the (id, email, user_metadata) fields actually used by
           resolve_supabase_identity() below — nothing else.
         - TTL: short, e.g. 30-60s — well under a Supabase access token's
           typical lifetime, so this only dedupes bursts of calls for the
           *same* token in a short window (e.g. a page loading several API
           calls at once), not a cache users could still be "valid" on long
           after a revoke.
         - Invalidation: none needed beyond TTL expiry — there is no
           explicit revoke-webhook from Supabase to invalidate on, so the
           bound on staleness is the TTL itself, not an event.
         - Security implication to weigh before ever adding this: a
           revoked/expired token could still be accepted by this app for up
           to the TTL. profile.is_blocked / is_suspended enforcement is
           unaffected either way (IsActiveAccount checks the live Django
           Profile row every request, never cached) — only the "is this
           Supabase token itself still good" check would be briefly stale.
    """
    if not access_token:
        raise SupabaseAuthError('Missing access token.')

    client = get_supabase_client()
    try:
        response = client.auth.get_user(access_token)
    except Exception as exc:
        raise SupabaseAuthError('Invalid or expired sign-in session.') from exc

    if response is None or response.user is None:
        raise SupabaseAuthError('Invalid or expired sign-in session.')

    return response.user


_admin_client_instance = None


def _admin_client():
    """
    A Supabase client authorized with the service-role key, for Admin API
    calls only (never the anon-key client `get_supabase_client()` uses for
    verifying end-user tokens). Same service-role-for-server-only-privileged-
    ops pattern as core.storage.SupabaseMediaStorage's upload/delete client
    — this key must never reach a browser or the Android app.
    """
    global _admin_client_instance
    if _admin_client_instance is None:
        if not settings.SUPABASE_URL or not settings.SUPABASE_SERVICE_ROLE_KEY:
            raise SupabaseAuthError('Supabase admin operations require SUPABASE_SERVICE_ROLE_KEY.')
        _admin_client_instance = create_client(settings.SUPABASE_URL, settings.SUPABASE_SERVICE_ROLE_KEY)
    return _admin_client_instance


def delete_supabase_user(supabase_uid):
    """
    Permanently deletes the Supabase Auth identity for `supabase_uid` via
    the Admin API. Called by core.account_deletion once the local Django
    account is gone, so the same Google/Supabase identity can't sign back in
    and have resolve_supabase_identity() silently recreate the "deleted"
    account (it would otherwise find no Profile with that supabase_uid, and
    just make a new one — the account would appear un-deleted after all).
    """
    if not supabase_uid:
        return
    client = _admin_client()
    try:
        client.auth.admin.delete_user(supabase_uid)
    except Exception as exc:
        raise SupabaseAuthError(f'Failed to delete Supabase Auth identity {supabase_uid}.') from exc


def unique_username(base_text):
    """Slugifies `base_text` (typically an email or Supabase uid) into a free Django username."""
    User = get_user_model()
    base = re.sub(r'[^\w.@+-]', '', (base_text or 'user').split('@')[0])[:30] or 'user'
    username = base
    n = 1
    while User.objects.filter(username=username).exists():
        n += 1
        username = f'{base}{n}'[:150]
    return username


def resolve_supabase_identity(supabase_user):
    """
    Finds or creates the local auth.User + Profile for a verified Supabase
    user, mirroring their name/avatar from Supabase metadata. Shared by the
    web OAuth callback (core.views.auth_callback_api) and the API's bearer-
    token authentication (core.api.authentication) so both entry points
    resolve identity identically instead of keeping two copies of this logic.

    Returns (user, profile). Does not check profile.is_blocked — callers
    decide what to do with a blocked account (log out vs. reject the request).
    """
    from .models import Profile  # local import: avoids a module-load-order cycle with models.py

    User = get_user_model()
    email = supabase_user.email or ''
    metadata = supabase_user.user_metadata or {}
    full_name = metadata.get('full_name') or metadata.get('name') or ''
    avatar_url = metadata.get('avatar_url') or metadata.get('picture') or ''

    profile = Profile.objects.select_related('user').filter(supabase_uid=supabase_user.id).first()
    if profile:
        user = profile.user
    else:
        user = User.objects.filter(email=email).first() if email else None
        if user is None:
            user = User.objects.create(username=unique_username(email or supabase_user.id), email=email)
        profile, _ = Profile.objects.get_or_create(user=user)
        profile.supabase_uid = supabase_user.id

    if full_name and not profile.full_name:
        profile.full_name = full_name
    if avatar_url and not profile.profile_photo:
        profile.profile_photo_url = avatar_url
    profile.last_active_at = timezone.now()
    profile.save()
    return user, profile
