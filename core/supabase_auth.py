"""
Server-side verification of Supabase-issued Google sign-ins.

The frontend runs the actual Google OAuth dance via supabase-js. Once
Supabase hands back a session, we take its access_token and ask the
Supabase Auth API who it belongs to — this is the only step that matters
for security, since it proves the token is genuine without us having to
manage JWT secrets or key rotation locally.
"""
from django.conf import settings
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
