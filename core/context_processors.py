from django.conf import settings
from django.core.cache import cache


def supabase_config(request):
    """Exposes the public Supabase URL/anon key to every template (needed for supabase-js)."""
    return {
        'SUPABASE_URL': settings.SUPABASE_URL,
        'SUPABASE_ANON_KEY': settings.SUPABASE_ANON_KEY,
    }


def push_config(request):
    """Exposes the public VAPID key to every template (needed by push-notifications.js to subscribe)."""
    return {'VAPID_PUBLIC_KEY': settings.VAPID_PUBLIC_KEY}


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


CATEGORY_TREE_CACHE_KEY = 'core:nav_category_tree'
# Re-read on literally every page view (nav dropdown) but only changes when a
# Super Admin edits categories — cached with a short TTL as a safety net, and
# cleared immediately by signals.py on save so admin edits still show up right away.
CONTEXT_CACHE_TTL = 300


def category_tree(request):
    """
    Active top-level categories (with their active subcategories prefetched)
    for the header nav dropdown and footer "Categories" list — see
    partials/nav_categories.html. Replaces what used to be hardcoded links,
    so Super Admin category/subcategory changes cascade there automatically.
    """
    top_categories = cache.get(CATEGORY_TREE_CACHE_KEY)
    if top_categories is None:
        from django.db.models import Prefetch
        from .models import Category
        top_categories = list(
            Category.objects.filter(parent=None, is_active=True)
            .prefetch_related(Prefetch('children', queryset=Category.objects.filter(is_active=True).order_by('order', 'label')))
            .order_by('order', 'label')
        )
        cache.set(CATEGORY_TREE_CACHE_KEY, top_categories, CONTEXT_CACHE_TTL)
    return {'nav_category_tree': top_categories}


