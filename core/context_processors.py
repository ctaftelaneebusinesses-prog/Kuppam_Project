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


def unread_messages(request):
    """Unread Contact Us message count for the Super Admin dashboard sidebar badge."""
    profile = getattr(request.user, 'profile', None) if request.user.is_authenticated else None
    if not profile or not profile.is_super_admin:
        return {}
    from .models import ContactMessage
    return {'unread_message_count': ContactMessage.objects.filter(is_read=False).count()}


def category_tree(request):
    """
    Active top-level categories (with their active subcategories prefetched)
    for the header nav dropdown and footer "Categories" list — see
    partials/nav_categories.html. Replaces what used to be hardcoded links,
    so Super Admin category/subcategory changes cascade there automatically.
    """
    from django.db.models import Prefetch
    from .models import Category
    top_categories = (
        Category.objects.filter(parent=None, is_active=True)
        .prefetch_related(Prefetch('children', queryset=Category.objects.filter(is_active=True).order_by('order', 'label')))
        .order_by('order', 'label')
    )
    return {'nav_category_tree': top_categories}


def site_theme(request):
    """
    Site-wide palette default/enforce flags (see Super Admin's Site Theme
    panel), read on every page load so base.html's anti-flash script and
    palette-switcher.js can apply them before a visitor's own localStorage
    choice is considered.
    """
    from .models import SiteSettings
    settings_obj = SiteSettings.load()
    return {
        'site_default_palette': settings_obj.default_palette,
        'site_enforce_palette': settings_obj.enforce_palette,
    }
