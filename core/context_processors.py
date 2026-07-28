from django.conf import settings


def supabase_config(request):
    """Exposes the public Supabase URL/anon key to every template (needed for supabase-js)."""
    return {
        'SUPABASE_URL': settings.SUPABASE_URL,
        'SUPABASE_ANON_KEY': settings.SUPABASE_ANON_KEY,
    }


def notifications(request):
    """Unread notification count/list for the bell icon in the navbar."""
    if not request.user.is_authenticated:
        return {}
    qs = request.user.notifications.all()
    return {
        'unread_notification_count': qs.filter(is_read=False).count(),
        'recent_notifications': qs[:8],
    }
