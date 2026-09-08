import json

from django.conf import settings
from django.core.cache import cache


def supabase_config(request):
    """Exposes the public Supabase URL/anon key to every template (needed for supabase-js)."""
    return {
        'SUPABASE_URL': settings.SUPABASE_URL,
        'SUPABASE_ANON_KEY': settings.SUPABASE_ANON_KEY,
    }


def location(request):
    from .location_service import active_location
    current = active_location(request)
    return {
        'current_location': current,
        'current_location_data': json.dumps({
            'cityId': current.pk,
            'city': current.name,
            'state': current.state,
            'latitude': float(current.latitude) if current.latitude is not None else None,
            'longitude': float(current.longitude) if current.longitude is not None else None,
        }) if current else 'null',
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


def pending_admin_requests(request):
    """
    Pending Content Provider request count for the "Content Requests" sidebar
    badge — visible to whoever can actually review them (see
    core.decorators.content_providers_required, the same gate used on
    dashboard_admin_requests/dashboard_admin_request_detail): Super Admin,
    City Admin, or a Sub Admin granted view_content_providers. Queried fresh
    on every request (no caching) so it — and the badge disappearing at
    zero — reflects approve/reject/changes-requested decisions immediately
    on the next page load, the same way unread_message_count above does.
    """
    profile = getattr(request.user, 'profile', None) if request.user.is_authenticated else None
    if not profile:
        return {}
    allowed = profile.is_super_admin or profile.is_city_admin or profile.has_permission('view_content_providers')
    if not allowed:
        return {}
    from .models import AdminRequest, AdminRequestStatus
    return {'pending_admin_request_count': AdminRequest.objects.filter(status=AdminRequestStatus.PENDING).count()}


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


