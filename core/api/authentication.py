from rest_framework.authentication import BaseAuthentication
from rest_framework.authentication import SessionAuthentication as _DRFSessionAuthentication
from rest_framework.exceptions import AuthenticationFailed

from ..supabase_auth import SupabaseAuthError, fetch_supabase_user, resolve_supabase_identity


class SessionAuthentication(_DRFSessionAuthentication):
    """
    DRF's own SessionAuthentication checks `user.is_active` to decide
    whether to "authenticate" the request — and Django's AnonymousUser has
    `is_active = True`, so a plain anonymous request ends up authenticating
    as anonymous rather than declining. That makes every anonymous request
    look like a "successful" authentication to DRF's permission_denied()
    logic, which then answers 403 instead of 401. Checking `is_authenticated`
    instead (False for AnonymousUser) makes an anonymous request correctly
    decline authentication, so unauthenticated requests get a 401 like every
    other client expects, and CSRF is still enforced for anyone who is
    actually logged in.
    """

    def authenticate(self, request):
        user = getattr(request._request, 'user', None)
        if not user or not user.is_authenticated:
            return None
        self.enforce_csrf(request)
        return (user, None)


class SupabaseTokenAuthentication(BaseAuthentication):
    """
    Authenticates a request carrying `Authorization: Bearer <supabase access
    token>` — the path a client with no shared browser cookie jar (a future
    native Android screen) uses, as opposed to the web's existing
    session-cookie flow. Reuses the exact same server-side token
    verification and identity resolution as the web OAuth callback
    (core.supabase_auth) so a token authenticates identically for either
    client and never trusts a client-asserted user id.

    A blocked account fails authentication outright (401) rather than
    authenticating as a blocked user — every view built on top of this
    class can assume request.user is never a blocked account.
    """
    keyword = 'Bearer'

    def authenticate(self, request):
        header = request.META.get('HTTP_AUTHORIZATION', '')
        if not header.startswith(f'{self.keyword} '):
            return None
        token = header[len(self.keyword) + 1:].strip()
        if not token:
            return None

        try:
            supabase_user = fetch_supabase_user(token)
        except SupabaseAuthError as exc:
            raise AuthenticationFailed(str(exc))

        user, profile = resolve_supabase_identity(supabase_user)
        if profile.is_blocked:
            raise AuthenticationFailed('This account has been blocked. Contact support if you think this is a mistake.')

        return (user, token)

    def authenticate_header(self, request):
        return self.keyword
