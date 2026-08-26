from django.shortcuts import render

EXEMPT_PATH_PREFIXES = ('/admin/', '/static/', '/media/', '/signin/', '/login/', '/auth/')


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

        from .models import PlatformSettings
        settings_obj = PlatformSettings.load()
        if settings_obj.maintenance_mode:
            return render(request, 'maintenance.html', {'message': settings_obj.maintenance_message}, status=503)

        return self.get_response(request)
