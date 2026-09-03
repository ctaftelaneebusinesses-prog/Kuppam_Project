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
    """Returns the Supabase auth user for a valid access token, or raises SupabaseAuthError."""
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
