from django.core.cache import cache
from django.shortcuts import render

EXEMPT_PATH_PREFIXES = ('/admin/', '/static/', '/media/', '/signin/', '/login/', '/auth/')

#: (maintenance_mode, maintenance_message), cached so every page view doesn't
#: pay a round trip to the remote database just to learn "not in maintenance".
#: Cleared by signals.py whenever PlatformSettings is saved; the TTL only
#: bounds how long other gunicorn workers' local caches can lag behind.
MAINTENANCE_CACHE_KEY = 'core:maintenance_state'
MAINTENANCE_CACHE_TTL = 60


def maintenance_state():
    state = cache.get(MAINTENANCE_CACHE_KEY)
    if state is None:
        from .models import PlatformSettings
        settings_obj = PlatformSettings.load()
        state = (settings_obj.maintenance_mode, settings_obj.maintenance_message)
        cache.set(MAINTENANCE_CACHE_KEY, state, MAINTENANCE_CACHE_TTL)
    return state


class MaintenanceModeMiddleware:
    """
    When PlatformSettings.maintenance_mode is on, shows every visitor a
    maintenance page instead of the site. Super Admin, Django's own /admin/,
    static/media files, and the sign-in/auth routes stay reachable so
    maintenance mode can always be turned back off.
    """

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        if request.path.startswith(EXEMPT_PATH_PREFIXES):
            return self.get_response(request)

        profile = getattr(request.user, 'profile', None) if request.user.is_authenticated else None
        if profile is not None and profile.is_super_admin:
            return self.get_response(request)

        maintenance_mode, maintenance_message = maintenance_state()
        if maintenance_mode:
            return render(request, 'maintenance.html', {'message': maintenance_message}, status=503)

        return self.get_response(request)
