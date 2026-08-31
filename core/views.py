# from django.shortcuts import render, redirect
# from django.contrib.auth import login, logout
# from django.contrib.auth.decorators import login_required
# from django.contrib import messages
# from django.core.mail import send_mail
# from django.conf import settings

# from .forms import AdminLoginForm, ContactForm

# # Category definitions used across the homepage, search, and (later) listing pages.
# CATEGORIES = [
#     {'name': 'Real Estate', 'icon': 'bi-house-door', 'emoji': '🏠', 'slug': 'real-estate'},
#     {'name': 'Shops', 'icon': 'bi-shop', 'emoji': '🛍', 'slug': 'shops'},
#     {'name': 'Jobs', 'icon': 'bi-briefcase', 'emoji': '💼', 'slug': 'jobs'},
#     {'name': 'Restaurants', 'icon': 'bi-cup-hot', 'emoji': '🍽', 'slug': 'restaurants'},
#     {'name': 'Hospitals', 'icon': 'bi-hospital', 'emoji': '🏥', 'slug': 'hospitals'},
#     {'name': 'Education', 'icon': 'bi-mortarboard', 'emoji': '🎓', 'slug': 'education'},
#     {'name': 'Transport', 'icon': 'bi-bus-front', 'emoji': '🚖', 'slug': 'transport'},
#     {'name': 'News', 'icon': 'bi-newspaper', 'emoji': '📰', 'slug': 'news'},
# ]


# def home(request):
#     """
#     Homepage: hero section, search box, category grid.
#     """
#     context = {
#         'page_title': 'OneTownCity - Your Local Information Portal',
#         'categories': CATEGORIES,
#     }
#     return render(request, 'home.html', context)


# def search(request):
#     """
#     Search results page. Actual data filtering will be wired up once
#     category models exist (Phase 3+). For now this safely handles the
#     query and shows what categories match by name.
#     """
#     query = request.GET.get('q', '').strip()
#     category = request.GET.get('category', '').strip()

#     matched_categories = []
#     if query:
#         matched_categories = [
#             c for c in CATEGORIES if query.lower() in c['name'].lower()
#         ]

#     context = {
#         'page_title': f'Search Results for "{query}"' if query else 'Search - OneTownCity',
#         'query': query,
#         'category': category,
#         'categories': CATEGORIES,
#         'matched_categories': matched_categories,
#     }
#     return render(request, 'search_results.html', context)


# def admin_login(request):
#     """
#     Staff-only login. Regular visitors never need an account —
#     this is exclusively for the admin dashboard (built in a later phase).
#     """
#     if request.user.is_authenticated:
#         return redirect('core:home')

#     if request.method == 'POST':
#         form = AdminLoginForm(request, data=request.POST)
#         if form.is_valid():
#             user = form.get_user()
#             login(request, user)
#             messages.success(request, f'Welcome back, {user.username}!')
#             next_url = request.POST.get('next') or request.GET.get('next')
#             return redirect(next_url or '/admin/')
#         else:
#             messages.error(request, 'Invalid username or password, or this account does not have admin access.')
#     else:
#         form = AdminLoginForm(request)

#     context = {
#         'page_title': 'Admin Login - OneTownCity',
#         'form': form,
#     }
#     return render(request, 'login.html', context)


# @login_required(login_url='core:admin_login')
# def admin_logout(request):
#     logout(request)
#     messages.info(request, 'You have been logged out successfully.')
#     return redirect('core:home')


# def contact(request):
#     """
#     Contact form. Sends via Django's email backend (console backend
#     in development — prints to terminal instead of a real inbox).
#     """
#     if request.method == 'POST':
#         form = ContactForm(request.POST)
#         if form.is_valid():
#             name = form.cleaned_data['name']
#             email = form.cleaned_data['email']
#             subject = form.cleaned_data['subject']
#             message = form.cleaned_data['message']

#             full_message = f"From: {name} <{email}>\n\n{message}"

#             send_mail(
#                 subject=f'[OneTownCity Contact] {subject}',
#                 message=full_message,
#                 from_email=settings.DEFAULT_FROM_EMAIL,
#                 recipient_list=[settings.CONTACT_RECEIVER_EMAIL],
#                 fail_silently=False,
#             )
#             messages.success(request, 'Thank you for reaching out! We will get back to you soon.')
#             return redirect('core:contact')
#     else:
#         form = ContactForm()

#     context = {
#         'page_title': 'Contact Us - OneTownCity',
#         'form': form,
#     }
#     return render(request, 'contact.html', context)

import json
import re
from datetime import timedelta
from urllib.parse import urlencode

from django.contrib import messages
from django.contrib.auth import get_user_model, login, logout
from django.contrib.auth.decorators import login_required
from django.contrib.contenttypes.models import ContentType
from django.conf import settings
from django.core.exceptions import ValidationError
from django.core.mail import send_mail
from django.core.paginator import Paginator
from django.core.serializers.json import DjangoJSONEncoder
from django.core.validators import validate_email
from django.db import DatabaseError
from django.db.models import Count, F, Prefetch, ProtectedError, Q
from django.db.models.functions import TruncDate
from django.http import Http404, HttpResponse, JsonResponse
from django.shortcuts import get_object_or_404, redirect, render
from django.urls import reverse
from django.utils import timezone
from django.utils.http import url_has_allowed_host_and_scheme
from django.utils.safestring import mark_safe
from django.views.decorators.csrf import ensure_csrf_cookie
from django.views.decorators.http import require_POST

from .decorators import (
    city_admin_or_super_required, content_providers_required, content_review_required, excel_upload_allowed,
    onboarding_required, posts_dashboard_required, super_admin_required,
)
from .excel_utils import UPLOAD_CONFIGS, ExcelValidationError, build_sample_workbook, process_excel_upload
from .export_utils import build_posts_pdf, build_posts_workbook, build_users_pdf, build_users_workbook
from .forms import (
    AdminLoginForm, AdminRequestForm, AdminRequestReviewForm, CategoryForm, CityAdminForm, CommentForm,
    ContactForm, ContentProviderForm, ExcelUploadForm, LISTING_SUBMIT_FORMS, PasswordLoginForm,
    PlatformSettingsForm, ProfileCompletionForm, RegisterForm, ReportForm, ReviewForm, SubAdminForm,
)
from .models import (
    AdminCategoryPermission, AdminCityPermission, AdminRequest, AdminRequestStatus, AuditLog, Business,
    Category, CityModule, Comment, ContactMessage, Event, Favorite, Intent, Job, Like, ListingStatus,
    LoginHistory, News, NewsletterSubscriber, Notification, Permission, PlatformModule, PlatformSettings,
    PostImage, PostVideo, PostView, Profile, Project, Property, PushSubscription, Report, Review,
    RolePermission, Share, UserPermission, UserRole, Location, unique_slug_for,
)
from .location_service import active_location, reverse_geocode, save_location, search_cities, serialize_location
from .push import notify, notify_bulk
from .supabase_auth import SupabaseAuthError, fetch_supabase_user

User = get_user_model()

LISTING_MODELS = {
    'business': Business,
    'property': Property,
    'job': Job,
    'event': Event,
    'news': News,
    'project': Project,
}

#: Status filter options for moderator-facing screens (Listing Approvals,
#: Posts) — excludes Draft, which _scope_listing_qs already keeps out of
#: every moderator queryset, so offering it as a filter would just be a
#: guaranteed-empty option.
MODERATOR_STATUS_CHOICES = [choice for choice in ListingStatus.choices if choice[0] != ListingStatus.DRAFT]

# Gallery upload limits, shared by the Super Admin Posts dashboard and the
# owner-facing "My Listings" gallery manager.
GALLERY_IMAGE_TYPES = {'image/jpeg', 'image/png', 'image/webp'}
GALLERY_IMAGE_MAX_BYTES = 8 * 1024 * 1024
GALLERY_VIDEO_TYPES = {'video/mp4', 'video/webm', 'video/quicktime'}
GALLERY_VIDEO_MAX_BYTES = 50 * 1024 * 1024


def _validate_gallery_files(files, allowed_types, max_bytes, kind):
    """Returns (valid_files, error_messages) for a list of uploaded gallery files."""
    valid, errors = [], []
    for f in files:
        if f.content_type not in allowed_types:
            errors.append(f'"{f.name}" is not a supported {kind} format.')
        elif f.size > max_bytes:
            errors.append(f'"{f.name}" is larger than {max_bytes // (1024 * 1024)}MB.')
        else:
            valid.append(f)
    return valid, errors


def _can_moderate_posts(profile):
    """
    Super Admin's global moderation rights (view/enable/disable/feature/
    delete any post, platform-wide). A Content Provider (profile.is_admin —
    displayed as "Admin (Content Provider)") never gets this: they can only
    ever reach their own listings, via the obj.owner_id check in
    _can_manage_post — "Content Provider cannot: Modify another Content
    Provider's content" is enforced right here. City Admin/Sub Admin get
    their own city-scoped equivalent through _can_manage_city_post instead.
    """
    return profile.is_super_admin


def _can_manage_city_post(profile, obj):
    """City Admin (or a Sub Admin they've granted content-moderation rights
    to, for this listing's type) may manage any listing within their own
    assigned city/cities — except a Draft, which stays private to its owner
    until they submit it (see _scope_listing_qs)."""
    if obj.status == ListingStatus.DRAFT:
        return False
    if not (profile.is_city_admin or profile.is_sub_admin):
        return False
    if profile.is_sub_admin:
        model_key = type(obj).__name__.lower()
        allowed = (
            profile.has_permission('manage_city_content') or profile.has_permission('review_content')
            or profile.has_content_permission('edit', model_key) or profile.has_content_permission('delete', model_key)
        )
        if not allowed:
            return False
    return obj.city_id in profile.managed_city_ids()


def _can_manage_post(profile, obj):
    """Whether this profile may edit/delete this specific listing and its gallery."""
    return _can_moderate_posts(profile) or _can_manage_city_post(profile, obj) or obj.owner_id == profile.user_id


def _sub_admin_can_view_content(profile, model_key=None):
    """
    Whether a Sub Admin may see (not necessarily add/edit/approve) content of
    `model_key` at all — the type-specific view_businesses/view_events/
    view_announcements key, or a blanket review_content/manage_city_content/
    view_content grant. Shared by _scope_listing_qs (queryset-level
    filtering) and _sub_admin_dashboard (which stat cards to show) so the
    two can never disagree about what's visible.
    """
    return (
        profile.has_permission('review_content') or profile.has_permission('manage_city_content')
        or profile.has_content_permission('view', model_key)
    )


def _scope_listing_qs(request, qs, model_key=None):
    """
    Applies the same visibility scoping everywhere a listing queryset needs
    it (Pending Listings, the Posts dashboard + its export/count views):
    Super Admin sees everything; City Admin sees their city/cities; a
    permitted Sub Admin sees their city/cities too, but only once granted
    view access to `model_key` (see _sub_admin_can_view_content); everyone
    else (Content Provider) sees only what they own. A Draft is never
    included for any of the moderator branches — it's the owner's
    unsubmitted work-in-progress, not something a reviewer should see (or be
    able to filter into view) before it's actually been submitted.
    """
    profile = request.profile
    if profile.is_super_admin:
        return qs.exclude(status=ListingStatus.DRAFT)
    if profile.is_city_admin:
        return qs.filter(city_id__in=profile.managed_city_ids()).exclude(status=ListingStatus.DRAFT)
    if profile.is_sub_admin and _sub_admin_can_view_content(profile, model_key):
        return qs.filter(city_id__in=profile.managed_city_ids()).exclude(status=ListingStatus.DRAFT)
    return qs.filter(owner=request.user)


def _safe_next(request, fallback):
    """Validates a POSTed `next` redirect target so it can't be used for an open redirect."""
    next_url = request.POST.get('next')
    if next_url and url_has_allowed_host_and_scheme(next_url, allowed_hosts={request.get_host()}, require_https=request.is_secure()):
        return next_url
    return fallback


def _public_qs(model_cls, request=None):
    """Base queryset for anything shown to the public: active AND approved."""
    qs = model_cls.objects.filter(is_active=True, status=ListingStatus.APPROVED)
    location = active_location(request) if request is not None else None
    return qs.filter(city=location) if location else qs


def _ld_json(data):
    """
    Serializes a dict to a JSON-LD payload for embedding in a
    <script type="application/ld+json"> tag. Escapes <, >, & the same way
    Django's json_script does — listing text (name/description/etc.) is
    owner-submitted and could otherwise contain a literal "</script>" that
    breaks out of the tag.
    """
    json_str = json.dumps(data, cls=DjangoJSONEncoder)
    return mark_safe(json_str.replace('&', '\\u0026').replace('<', '\\u003c').replace('>', '\\u003e'))


def _detail_qs(request, model_cls):
    """
    Queryset used to look up a single listing for its detail page. Super
    admin can open any listing regardless of status/active state (e.g. to
    preview a pending submission); everyone else only sees public rows.
    """
    profile = getattr(request.user, 'profile', None) if request.user.is_authenticated else None
    if profile and profile.is_super_admin:
        return model_cls.objects.all()
    return _public_qs(model_cls, request)


def location_search(request):
    query = request.GET.get('q', '')
    try:
        cities = search_cities(query)
        payload = [{
            **serialize_location(city, source='manual_selection'),
            'state': city.state,
        } for city in cities]
    except DatabaseError:
        return JsonResponse({'error': 'City search is temporarily unavailable.'}, status=503)
    return JsonResponse(payload, safe=False)


@require_POST
def location_select(request):
    try:
        payload = json.loads(request.body or '{}')
        city = Location.objects.get(pk=payload.get('cityId'), kind=Location.Kind.CITY, is_active=True)
    except (ValueError, TypeError, Location.DoesNotExist, json.JSONDecodeError):
        return JsonResponse({'error': 'Please choose a supported city.'}, status=400)
    return JsonResponse(save_location(request, city, source='manual_selection'))


@require_POST
def location_reverse_geocode(request):
    try:
        payload = json.loads(request.body or '{}')
        result = reverse_geocode(payload['latitude'], payload['longitude'])
    except (KeyError, TypeError, ValueError, json.JSONDecodeError, ValidationError, OSError):
        return JsonResponse({'error': 'We could not resolve that location. Please choose a city manually.'}, status=400)
    return JsonResponse(result)


def _bump_views(request, model_cls, pk):
    """
    Counts one view per browsing session per listing. Without this, a single
    visitor reloading the page, hitting back/forward, or the browser's own
    link-hover prefetching each fired another unconditional increment, so
    "1 real visit" could show up as 3-4 views.
    """
    seen = request.session.setdefault('viewed_listings', [])
    key = f'{model_cls.__name__}:{pk}'
    if key in seen:
        return
    seen.append(key)
    request.session.modified = True

    model_cls.objects.filter(pk=pk).update(view_count=F('view_count') + 1)
    PostView.objects.create(content_type=ContentType.objects.get_for_model(model_cls), object_id=pk)


def _community_context(request, obj):
    """Like/favorite state, comments, reviews, and forms shared by every listing detail page."""
    ct = ContentType.objects.get_for_model(obj)
    user = request.user if request.user.is_authenticated else None
    is_liked = bool(user) and Like.objects.filter(content_type=ct, object_id=obj.pk, user=user).exists()
    is_favorited = bool(user) and Favorite.objects.filter(content_type=ct, object_id=obj.pk, user=user).exists()
    comments = (
        Comment.objects.filter(content_type=ct, object_id=obj.pk, parent__isnull=True)
        .select_related('user').prefetch_related('replies__user')
    )
    reviews = Review.objects.filter(content_type=ct, object_id=obj.pk).select_related('user')

    return {
        'model_key': obj._meta.model_name,
        'is_liked': is_liked,
        'is_favorited': is_favorited,
        'comments': comments,
        'reviews': reviews,
        'comment_form': CommentForm(),
        'review_form': ReviewForm(),
        'report_form': ReportForm(),
        'share_platforms': Share.PLATFORM_CHOICES,
        'public_url': request.build_absolute_uri(obj.get_absolute_url()),
    }


# Business.category values that have their own dedicated directory page
# (Restaurants, Hospitals & Healthcare, Education, Transport). Every other
# Business.category value (retail, grocery, clothing, electronics, hardware,
# salon, automobile, stationery, jewellery, other, ...) falls back to the
# general "Businesses" listing — so a listing only ever shows up on the one
# page that matches its category, never on every page at once. Businesses
# is deliberately one page: car garages, textile/clothing shops, stationery
# shops, supermarkets, etc. all show there as filterable sub-category tags
# (see GENERAL_BUSINESS_CATEGORY_CHOICES + business_list's 'category' filter)
# rather than each getting a separate top-level page.
#
# A directory whose 'categories' has more than one value (Education,
# Hospitals, Restaurants) shows a sub-category filter on its page — e.g.
# Education is one page/category, with School and College & University as
# filterable tags within it, not separate top-level pages.
DIRECTORY_CATEGORIES = {
    'restaurants': {'categories': ['restaurant', 'bakery'], 'label': 'Restaurants & Food', 'icon': 'bi-cup-hot'},
    'hospitals': {'categories': ['hospital', 'pharmacy'], 'label': 'Hospitals & Healthcare', 'icon': 'bi-hospital'},
    'education': {'categories': ['school', 'college'], 'label': 'Education', 'icon': 'bi-mortarboard'},
    'transport': {'categories': ['transport'], 'label': 'Transport', 'icon': 'bi-bus-front'},
}

#: Union of every category value claimed by a dedicated directory page —
#: the general Businesses page excludes all of these so it only shows
#: the true leftover/general listings (automobile, hardware, salon, etc).
_DIRECTORY_BUSINESS_CATEGORIES = {
    cat for config in DIRECTORY_CATEGORIES.values() for cat in config['categories']
}

#: Category choices for the general Businesses page's filter dropdown —
#: every Business sub-category NOT already covered by a dedicated directory
#: page (car garages, textile/clothing shops, stationery shops, grocery
#: stores, salons, jewellery, etc.) — all live together on one page, each
#: tagged with its own sub-category.
GENERAL_BUSINESS_CATEGORY_CHOICES = [
    (key, label) for key, label in Business.CATEGORY_CHOICES
    if key not in _DIRECTORY_BUSINESS_CATEGORIES
]


# Category definitions for the search page's category picker/chips (search()
# below) — matched against SEARCH_CATEGORY_REDIRECT by slug. The homepage
# Services grid used to read this same list but is now DB-driven from the
# Category model (see home() and Category.listing_count) so Super Admins can
# manage it from the "Manage Categories" dashboard instead of editing code.
CATEGORIES = [
    {
        'name': 'Property Listing', 'icon': 'bi-house-door', 'slug': 'real-estate',
        'image': 'images/services/real-estate.jpg',
        'description': 'Find houses, apartments, plots, villas, rental properties, and commercial spaces available in your location.',
        'count_fn': lambda: _public_qs(Property).count(),
    },
    {
        'name': 'Nearby Shops', 'icon': 'bi-shop', 'slug': 'shops',
        'image': 'images/services/business.jpg',
        'description': 'Explore car garages, clothing and textile shops, stationery shops, supermarkets, salons, and other local businesses across the city.',
        'count_fn': lambda: _public_qs(Business).exclude(category__in=_DIRECTORY_BUSINESS_CATEGORIES).count(),
    },
    {
        'name': 'Jobs', 'icon': 'bi-briefcase', 'slug': 'jobs',
        'image': 'images/services/jobs.jpg',
        'description': 'Browse job openings from local shops, offices, and companies hiring across the city.',
        'count_fn': lambda: _public_qs(Job).count(),
    },
    {
        'name': 'Events', 'icon': 'bi-calendar-event', 'slug': 'events',
        'image': 'images/services/events.jpg',
        'description': 'Stay updated with upcoming festivals, exhibitions, public events, and local programs.',
        'count_fn': lambda: _public_qs(Event).count(),
    },
    {
        'name': 'Restaurants', 'icon': 'bi-cup-hot', 'slug': 'restaurants',
        'image': 'images/services/restaurants.jpg',
        'description': 'Discover the best restaurants, cafés, bakeries, and food outlets in your location.',
        'count_fn': lambda: _public_qs(Business).filter(category__in=DIRECTORY_CATEGORIES['restaurants']['categories']).count(),
    },
    {
        'name': 'Hospitals & Healthcare', 'icon': 'bi-hospital', 'slug': 'hospitals',
        'image': 'images/services/hospitals.jpg',
        'description': 'Find hospitals, clinics, diagnostic centers, pharmacies, and emergency healthcare services.',
        'count_fn': lambda: _public_qs(Business).filter(category__in=DIRECTORY_CATEGORIES['hospitals']['categories']).count(),
    },
    {
        'name': 'Education', 'icon': 'bi-mortarboard', 'slug': 'education',
        'image': 'images/services/education.jpg',
        'description': 'Explore schools, colleges, universities, and other educational institutions in your location.',
        'count_fn': lambda: _public_qs(Business).filter(category__in=DIRECTORY_CATEGORIES['education']['categories']).count(),
    },
    {
        'name': 'Transport', 'icon': 'bi-bus-front', 'slug': 'transport',
        'image': 'images/services/transport.jpg',
        'description': 'Find buses, taxis, auto services, logistics, and transportation facilities.',
        'count_fn': lambda: _public_qs(Business).filter(category='transport').count(),
    },
    {
        'name': 'OneTownCity News', 'icon': 'bi-newspaper', 'slug': 'news',
        'image': 'images/services/news.jpg',
        'description': 'Catch up on local announcements, civic updates, and news from across the city.',
        'count_fn': lambda: _public_qs(News).count(),
    },
    {
        'name': 'Upcoming Projects', 'icon': 'bi-cone-striped', 'slug': 'projects',
        'image': 'images/services/upcoming-projects.jpg',
        'description': 'Track planned and ongoing civic and infrastructure projects shaping your city.',
        'count_fn': lambda: _public_qs(Project).count(),
    },
]


def service_worker(request):
    """
    Serves static/js/sw.js at the site root (/sw.js) instead of under
    /static/js/ — a service worker's default registration scope is the
    directory it's served from, and Web Push needs it covering the whole
    site, not just /static/js/.
    """
    sw_path = settings.STATICFILES_DIRS[0] / 'js' / 'sw.js'
    return HttpResponse(sw_path.read_text(encoding='utf-8'), content_type='application/javascript')


def robots_txt(request):
    """
    Tells search engine crawlers which parts of the site are worth indexing
    (public listings/pages) vs. which aren't (auth-gated dashboard, uploads,
    and account flows) and points them at the sitemap.
    """
    lines = [
        'User-agent: *',
        'Disallow: /dashboard/',
        'Disallow: /uploads/',
        'Disallow: /admin/',
        'Disallow: /login/',
        'Disallow: /signin/',
        'Disallow: /register/',
        'Disallow: /welcome/',
        'Disallow: /favorites/',
        'Disallow: /notifications/',
        f"Sitemap: {request.build_absolute_uri('/sitemap.xml')}",
    ]
    return HttpResponse('\n'.join(lines), content_type='text/plain')


def home(request):
    """
    Homepage: hero section, search box, category grid, featured businesses.
    """
    # Fall back to the latest listings whenever nothing has been marked
    # "Featured" yet, so these sections never render as blank gaps on the
    # homepage while admins are still curating featured picks.
    featured_businesses = _public_qs(Business, request).filter(is_featured=True)[:6] \
        or _public_qs(Business, request).order_by('-created_at')[:6]
    featured_properties = _public_qs(Property, request).filter(is_featured=True)[:6] \
        or _public_qs(Property, request).order_by('-created_at')[:6]
    featured_jobs = _public_qs(Job, request).filter(is_featured=True)[:6] \
        or _public_qs(Job, request).order_by('-created_at')[:6]
    featured_events = _public_qs(Event, request).filter(is_featured=True, event_date__gte=timezone.localdate())[:6] \
        or _public_qs(Event, request).filter(event_date__gte=timezone.localdate()).order_by('event_date')[:6]
    featured_news = _public_qs(News, request).filter(is_featured=True)[:6] \
        or _public_qs(News, request).order_by('-created_at')[:6]
    featured_projects = _public_qs(Project, request).filter(is_featured=True)[:6] \
        or _public_qs(Project, request).order_by('-created_at')[:6]

    stats = {
        'businesses': _public_qs(Business, request).count(),
        'properties': _public_qs(Property, request).count(),
        'jobs': _public_qs(Job, request).count(),
        'users': get_user_model().objects.count(),
    }

    # Reuses the same cached lookup the navbar's category_tree context
    # processor already computes (see core/context_processors.py) instead of
    # re-querying Category here — home.html never reads .children on these,
    # so the separate prefetch this used to run was pure waste on top of it.
    from .context_processors import category_tree
    current = active_location(request)
    context = {
        'page_title': f'OneTownCity {current.name}' if current else 'OneTownCity — Visual Local Engine & Discovery Portal',
        'categories': category_tree(request)['nav_category_tree'],
        'featured_businesses': featured_businesses,
        'featured_properties': featured_properties,
        'featured_jobs': featured_jobs,
        'featured_events': featured_events,
        'featured_news': featured_news,
        'featured_projects': featured_projects,
        'stats': stats,
    }
    return render(request, 'home.html', context)


def city_home(request, city_slug):
    city = get_object_or_404(Location, slug=city_slug, kind=Location.Kind.CITY, is_active=True)
    save_location(request, city, source='manual_selection')
    return home(request)


# A category picked in the header/hero search box goes straight to that
# type's own listing page (which already has full search + pagination),
# instead of duplicating filtering logic here.
SEARCH_CATEGORY_REDIRECT = {
    'real-estate': 'core:property_list',
    'shops': 'core:business_list',
    'jobs': 'core:job_list',
    'events': 'core:event_list',
    'news': 'core:news_list',
    'restaurants': 'core:restaurant_list',
    'hospitals': 'core:hospital_list',
    'education': 'core:education_list',
    'transport': 'core:transport_list',
    'projects': 'core:project_list',
}

SEARCH_RESULT_LIMIT = 6

_SEARCH_FILTERS = {
    'business': lambda q: Q(name__icontains=q) | Q(description__icontains=q) | Q(address__icontains=q) | Q(category__icontains=q),
    'property': lambda q: Q(title__icontains=q) | Q(description__icontains=q) | Q(location__icontains=q),
    'job': lambda q: Q(job_title__icontains=q) | Q(company__icontains=q) | Q(description__icontains=q) | Q(location__icontains=q),
    'event': lambda q: Q(title__icontains=q) | Q(description__icontains=q) | Q(location__icontains=q),
    'news': lambda q: Q(title__icontains=q) | Q(content__icontains=q),
    'project': lambda q: Q(title__icontains=q) | Q(description__icontains=q) | Q(location__icontains=q),
}


def search(request):
    """
    Universal search: with no category picked, this queries every listing
    type at once and shows a few top matches per category, each linking
    through to that type's full (paginated) list page for the rest.
    """
    query = request.GET.get('q', '').strip()
    category = request.GET.get('category', '').strip()

    if category and category in SEARCH_CATEGORY_REDIRECT:
        target = reverse(SEARCH_CATEGORY_REDIRECT[category])
        if query:
            target += '?' + urlencode({'q': query})
        return redirect(target)

    results = {}
    total_results = 0

    if query:
        def _section(key, label, icon, qs, list_url_name, card_partial, item_key):
            qs = qs.filter(
                _SEARCH_FILTERS[key](query)
            )
            count = qs.count()
            return {
                'label': label,
                'icon': icon,
                'items': qs[:SEARCH_RESULT_LIMIT],
                'count': count,
                'card_partial': card_partial,
                'item_key': item_key,
                'view_all_url': reverse(list_url_name) + '?' + urlencode({'q': query}),
            }

        sections = [
            _section(
                'business', 'Nearby Shops', 'bi-shop',
                _public_qs(Business, request).exclude(category__in=_DIRECTORY_BUSINESS_CATEGORIES),
                'core:business_list', 'partials/business_card.html', 'business',
            ),
        ]
        for directory_key, config in DIRECTORY_CATEGORIES.items():
            sections.append(_section(
                'business', config['label'], config['icon'],
                _public_qs(Business, request).filter(category__in=config['categories']),
                SEARCH_CATEGORY_REDIRECT[directory_key], 'partials/business_card.html', 'business',
            ))
        sections += [
            _section('property', 'Properties', 'bi-house-door', _public_qs(Property, request), 'core:property_list', 'partials/property_card.html', 'property'),
            _section('job', 'Jobs', 'bi-briefcase', _public_qs(Job, request), 'core:job_list', 'partials/job_card.html', 'job'),
            _section('event', 'Events', 'bi-calendar-event', _public_qs(Event, request), 'core:event_list', 'partials/event_card.html', 'event'),
            _section('news', 'News', 'bi-newspaper', _public_qs(News, request), 'core:news_list', 'partials/news_card.html', 'article'),
            _section('project', 'Upcoming Projects', 'bi-cone-striped', _public_qs(Project, request), 'core:project_list', 'partials/project_card.html', 'project'),
        ]
        results = [s for s in sections if s['count']]
        total_results = sum(s['count'] for s in sections)

    context = {
        'page_title': f'Search Results for "{query}"' if query else 'Search - OneTownCity',
        'query': query,
        'category': category,
        'categories': CATEGORIES,
        'results': results,
        'total_results': total_results,
    }
    return render(request, 'search_result.html', context)


def business_list(request):
    """
    General Businesses listing page — every Business record EXCEPT the ones
    covered by a dedicated directory page (Restaurants, Hospitals &
    Healthcare, Education, Transport). Car garages, clothing/textile shops,
    stationery shops, supermarkets, salons, etc. all live on this one page,
    each tagged with its own sub-category (see `category_choices` in the
    context) — so a listing only ever appears on the one page that matches
    its category. Supports search (by name or category) and pagination.
    """
    businesses = _public_qs(Business, request).exclude(category__in=_DIRECTORY_BUSINESS_CATEGORIES)

    query = request.GET.get('q', '').strip()
    category = request.GET.get('category', '').strip()

    if query:
        businesses = businesses.filter(
            Q(name__icontains=query) | Q(category__icontains=query)
        )

    if category in dict(GENERAL_BUSINESS_CATEGORY_CHOICES):
        businesses = businesses.filter(category=category)
    else:
        category = ''

    paginator = Paginator(businesses, 9)
    page_number = request.GET.get('page')
    page_obj = paginator.get_page(page_number)

    context = {
        'page_title': 'Businesses - OneTownCity',
        'page_obj': page_obj,
        'query': query,
        'selected_category': category,
        'selected_category_label': dict(GENERAL_BUSINESS_CATEGORY_CHOICES).get(category, ''),
        'category_choices': GENERAL_BUSINESS_CATEGORY_CHOICES,
        'total_results': businesses.count(),
    }
    return render(request, 'business_list.html', context)


def business_detail(request, slug):
    """
    Detail page for a single business listing — the canonical, shareable
    public URL (e.g. /businesses/sri-medicals/).
    """
    business = get_object_or_404(_detail_qs(request, Business), slug=slug)
    _bump_views(request, Business, business.pk)
    related_businesses = _public_qs(Business, request).filter(category=business.category).exclude(pk=business.pk)[:3]

    schema = {
        '@context': 'https://schema.org',
        '@type': 'LocalBusiness',
        'name': business.name,
        'image': business.display_image,
        'url': request.build_absolute_uri(business.get_absolute_url()),
        'telephone': business.phone_number,
        'address': {
            '@type': 'PostalAddress',
            'streetAddress': business.address,
            'addressLocality': 'Kuppam',
            'addressRegion': 'Andhra Pradesh',
            'addressCountry': 'IN',
        },
    }
    if business.description:
        schema['description'] = business.description
    if business.website:
        schema['sameAs'] = business.website
    if business.review_count:
        schema['aggregateRating'] = {
            '@type': 'AggregateRating',
            'ratingValue': str(business.avg_rating),
            'reviewCount': business.review_count,
        }

    context = {
        'page_title': f'{business.name} - OneTownCity',
        'business': business,
        'related_businesses': related_businesses,
        'schema_json': _ld_json(schema),
        **_community_context(request, business),
    }
    return render(request, 'business_detail.html', context)


def directory_list(request, category):
    """
    Listing page for a fixed-category slice of Business records (Restaurants,
    Hospitals & Healthcare, Education, Transport, Shopping). Cards link to
    the regular business_detail page, which already displays the right
    category badge and related listings.

    When a directory groups more than one Business.category value together
    (e.g. Education = School + College & University), the page also shows a
    sub-category filter so visitors can narrow down to just one tag without
    that tag needing its own separate top-level page.
    """
    config = DIRECTORY_CATEGORIES.get(category)
    if config is None:
        raise Http404('Unknown directory category')

    businesses = _public_qs(Business, request).filter(category__in=config['categories'])

    subcategory_choices = [
        (key, label) for key, label in Business.CATEGORY_CHOICES
        if key in config['categories']
    ] if len(config['categories']) > 1 else []

    query = request.GET.get('q', '').strip()
    subcategory = request.GET.get('type', '').strip()

    if subcategory in dict(subcategory_choices):
        businesses = businesses.filter(category=subcategory)
    else:
        subcategory = ''

    if query:
        businesses = businesses.filter(
            Q(name__icontains=query) | Q(address__icontains=query)
        )

    paginator = Paginator(businesses, 9)
    page_number = request.GET.get('page')
    page_obj = paginator.get_page(page_number)

    context = {
        'page_title': f"{config['label']} - OneTownCity",
        'page_obj': page_obj,
        'query': query,
        'total_results': businesses.count(),
        'directory_label': config['label'],
        'directory_icon': config['icon'],
        'directory_key': category,
        'subcategory_choices': subcategory_choices,
        'selected_subcategory': subcategory,
    }
    return render(request, 'directory_list.html', context)


def property_list(request):
    """
    Properties listing page with search (by title or location), filter
    by type, and pagination.
    """
    properties = _public_qs(Property, request)

    query = request.GET.get('q', '').strip()
    property_type = request.GET.get('type', '').strip()

    if query:
        properties = properties.filter(
            Q(title__icontains=query) | Q(location__icontains=query)
        )

    if property_type:
        properties = properties.filter(property_type=property_type)

    paginator = Paginator(properties, 9)
    page_number = request.GET.get('page')
    page_obj = paginator.get_page(page_number)

    context = {
        'page_title': 'Properties - OneTownCity',
        'page_obj': page_obj,
        'query': query,
        'selected_type': property_type,
        'type_choices': Property.PROPERTY_TYPE_CHOICES,
        'total_results': properties.count(),
    }
    return render(request, 'property_list.html', context)


def property_detail(request, slug):
    """
    Detail page for a single property listing.
    """
    property_obj = get_object_or_404(_detail_qs(request, Property), slug=slug)
    _bump_views(request, Property, property_obj.pk)
    related_properties = _public_qs(Property, request).filter(property_type=property_obj.property_type).exclude(pk=property_obj.pk)[:3]

    schema = {
        '@context': 'https://schema.org',
        '@type': 'Product',
        'name': property_obj.title,
        'image': property_obj.display_image,
        'description': property_obj.description or property_obj.title,
        'url': request.build_absolute_uri(property_obj.get_absolute_url()),
        'offers': {
            '@type': 'Offer',
            'price': str(property_obj.price),
            'priceCurrency': 'INR',
            'availability': 'https://schema.org/InStock',
            'url': request.build_absolute_uri(property_obj.get_absolute_url()),
        },
    }

    context = {
        'page_title': f'{property_obj.title} - OneTownCity',
        'property': property_obj,
        'related_properties': related_properties,
        'schema_json': _ld_json(schema),
        **_community_context(request, property_obj),
    }
    return render(request, 'property_detail.html', context)


def job_list(request):
    """
    Jobs listing page with search (by job title, company or location)
    and pagination.
    """
    jobs = _public_qs(Job, request)

    query = request.GET.get('q', '').strip()

    if query:
        jobs = jobs.filter(
            Q(job_title__icontains=query) | Q(company__icontains=query) | Q(location__icontains=query)
        )

    paginator = Paginator(jobs, 9)
    page_number = request.GET.get('page')
    page_obj = paginator.get_page(page_number)

    context = {
        'page_title': 'Jobs - OneTownCity',
        'page_obj': page_obj,
        'query': query,
        'total_results': jobs.count(),
    }
    return render(request, 'job_list.html', context)


def job_detail(request, slug):
    """
    Detail page for a single job listing.
    """
    job = get_object_or_404(_detail_qs(request, Job), slug=slug)
    _bump_views(request, Job, job.pk)
    related_jobs = _public_qs(Job, request).filter(company=job.company).exclude(pk=job.pk)[:3]

    schema = {
        '@context': 'https://schema.org',
        '@type': 'JobPosting',
        'title': job.job_title,
        'description': job.description or job.job_title,
        'datePosted': job.created_at.date().isoformat(),
        'hiringOrganization': {
            '@type': 'Organization',
            'name': job.company,
        },
        'jobLocation': {
            '@type': 'Place',
            'address': {
                '@type': 'PostalAddress',
                'addressLocality': job.location or 'Kuppam',
                'addressRegion': 'Andhra Pradesh',
                'addressCountry': 'IN',
            },
        },
    }

    context = {
        'page_title': f'{job.job_title} at {job.company} - OneTownCity',
        'job': job,
        'related_jobs': related_jobs,
        'schema_json': _ld_json(schema),
        **_community_context(request, job),
    }
    return render(request, 'job_detail.html', context)


def event_list(request):
    """
    Events listing page with search (by title or location) and pagination.
    Upcoming events are shown first (model default ordering).
    """
    events = _public_qs(Event, request)

    query = request.GET.get('q', '').strip()

    if query:
        events = events.filter(
            Q(title__icontains=query) | Q(location__icontains=query)
        )

    paginator = Paginator(events, 9)
    page_number = request.GET.get('page')
    page_obj = paginator.get_page(page_number)

    context = {
        'page_title': 'Events - OneTownCity',
        'page_obj': page_obj,
        'query': query,
        'total_results': events.count(),
    }
    return render(request, 'event_list.html', context)


def event_detail(request, slug):
    """
    Detail page for a single event listing.
    """
    event = get_object_or_404(_detail_qs(request, Event), slug=slug)
    _bump_views(request, Event, event.pk)
    related_events = _public_qs(Event, request).exclude(pk=event.pk)[:3]

    schema = {
        '@context': 'https://schema.org',
        '@type': 'Event',
        'name': event.title,
        'startDate': event.event_date.isoformat(),
        'eventAttendanceMode': 'https://schema.org/OfflineEventAttendanceMode',
        'eventStatus': 'https://schema.org/EventScheduled',
        'location': {
            '@type': 'Place',
            'name': event.location,
            'address': {
                '@type': 'PostalAddress',
                'addressLocality': 'Kuppam',
                'addressRegion': 'Andhra Pradesh',
                'addressCountry': 'IN',
            },
        },
        'image': event.display_image,
        'description': event.description or event.title,
    }

    context = {
        'page_title': f'{event.title} - OneTownCity',
        'event': event,
        'related_events': related_events,
        'schema_json': _ld_json(schema),
        **_community_context(request, event),
    }
    return render(request, 'event_detail.html', context)


def news_list(request):
    """
    News listing page with search (by title or content) and pagination.
    Most recently published articles are shown first.
    """
    articles = _public_qs(News, request)

    query = request.GET.get('q', '').strip()

    if query:
        articles = articles.filter(
            Q(title__icontains=query) | Q(content__icontains=query)
        )

    paginator = Paginator(articles, 9)
    page_number = request.GET.get('page')
    page_obj = paginator.get_page(page_number)

    context = {
        'page_title': 'OneTownCity News',
        'page_obj': page_obj,
        'query': query,
        'total_results': articles.count(),
    }
    return render(request, 'news_list.html', context)


def news_detail(request, slug):
    """
    Detail page for a single news article.
    """
    article = get_object_or_404(_detail_qs(request, News), slug=slug)
    _bump_views(request, News, article.pk)
    related_articles = _public_qs(News, request).exclude(pk=article.pk)[:3]

    schema = {
        '@context': 'https://schema.org',
        '@type': 'NewsArticle',
        'headline': article.title[:110],
        'image': [article.display_image],
        'datePublished': article.published_date.isoformat(),
        'dateModified': article.updated_at.date().isoformat(),
        'author': {
            '@type': 'Organization',
            'name': article.source or 'OneTownCity',
        },
        'publisher': {
            '@type': 'Organization',
            'name': 'OneTownCity',
        },
        'description': (article.content or article.title)[:200],
        'mainEntityOfPage': request.build_absolute_uri(article.get_absolute_url()),
    }

    context = {
        'page_title': f'{article.title} - OneTownCity',
        'article': article,
        'related_articles': related_articles,
        'schema_json': _ld_json(schema),
        **_community_context(request, article),
    }
    return render(request, 'news_detail.html', context)


def project_list(request):
    """
    Upcoming Projects listing page with search (by title or location) and
    pagination. Featured/newest projects are shown first (model default
    ordering).
    """
    projects = _public_qs(Project, request)

    query = request.GET.get('q', '').strip()

    if query:
        projects = projects.filter(
            Q(title__icontains=query) | Q(location__icontains=query)
        )

    paginator = Paginator(projects, 9)
    page_number = request.GET.get('page')
    page_obj = paginator.get_page(page_number)

    context = {
        'page_title': 'Upcoming Projects - OneTownCity',
        'page_obj': page_obj,
        'query': query,
        'total_results': projects.count(),
    }
    return render(request, 'project_list.html', context)


def project_detail(request, slug):
    """
    Detail page for a single upcoming project.
    """
    project = get_object_or_404(_detail_qs(request, Project), slug=slug)
    _bump_views(request, Project, project.pk)
    related_projects = _public_qs(Project, request).exclude(pk=project.pk)[:3]

    context = {
        'page_title': f'{project.title} - OneTownCity',
        'project': project,
        'related_projects': related_projects,
        **_community_context(request, project),
    }
    return render(request, 'project_detail.html', context)


LISTING_MODEL_KEYS_BY_CLASS = {model_cls: key for key, model_cls in LISTING_MODELS.items()}


def _config_allowed(request, config):
    """
    Whether the current requester may use this Excel upload config. Django
    staff and Super Admins can use every config; a regular Admin is scoped to
    the listing models they hold an AdminCategoryPermission for — otherwise
    an Admin permitted only for Jobs could bulk-import Businesses, bypassing
    the category-permission system used everywhere else.
    """
    if request.user.is_staff:
        return True
    profile = request.profile
    if profile.is_super_admin:
        return True
    listing_model_key = LISTING_MODEL_KEYS_BY_CLASS.get(config['model'])
    return listing_model_key in profile.managed_listing_models()


@excel_upload_allowed
def upload_hub(request):
    """
    Excel Upload Center — linking to the bulk-upload page for each module
    (Businesses, Properties, Jobs, Events, News...), scoped to what the
    requester is allowed to touch (see _config_allowed).
    """
    context = {
        'page_title': 'Excel Upload Center - OneTownCity',
        'configs': [c for c in UPLOAD_CONFIGS.values() if _config_allowed(request, c)],
    }
    return render(request, 'uploads/upload_hub.html', context)


@excel_upload_allowed
def upload_view(request, model_key):
    """
    Handles Excel upload + validation + insert-or-update for a single
    module. The module is selected via model_key (see UPLOAD_CONFIGS).
    """
    config = UPLOAD_CONFIGS.get(model_key)
    if config is None:
        raise Http404('Unknown upload type')
    if not _config_allowed(request, config):
        messages.error(request, 'You do not have permission to bulk-upload this listing type.')
        return redirect('core:upload_hub')

    result = None

    if request.method == 'POST':
        form = ExcelUploadForm(request.POST, request.FILES)
        if form.is_valid():
            try:
                result = process_excel_upload(form.cleaned_data['file'], config)
            except ExcelValidationError as exc:
                messages.error(request, str(exc))
            else:
                if result.inserted:
                    messages.success(request, f'{result.inserted} new record(s) added.')
                if result.updated:
                    messages.success(request, f'{result.updated} existing record(s) updated.')
                if result.errors:
                    messages.warning(
                        request,
                        f'{len(result.errors)} row(s) were skipped due to validation errors. See details below.'
                    )
                if not result.inserted and not result.updated and not result.errors:
                    messages.info(request, 'No data rows were found in the uploaded file.')
            form = ExcelUploadForm()
        else:
            messages.error(request, 'Please choose a valid Excel file (.xlsx or .xls, max 5 MB).')
    else:
        form = ExcelUploadForm()

    context = {
        'page_title': f'Upload {config["label"]} - OneTownCity',
        'form': form,
        'config': config,
        'model_key': model_key,
        'result': result,
    }
    return render(request, 'uploads/upload_form.html', context)


@excel_upload_allowed
def download_sample(request, model_key):
    """
    Streams a ready-to-fill .xlsx sample template for the given module.
    """
    config = UPLOAD_CONFIGS.get(model_key)
    if config is None:
        raise Http404('Unknown upload type')
    if not _config_allowed(request, config):
        messages.error(request, 'You do not have permission to bulk-upload this listing type.')
        return redirect('core:upload_hub')

    workbook = build_sample_workbook(config)

    response = HttpResponse(
        content_type='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    )
    response['Content-Disposition'] = f'attachment; filename="{model_key}_upload_template.xlsx"'
    workbook.save(response)
    return response


def admin_login(request):
    """
    Staff-only login. Regular visitors never need an account —
    this is exclusively for the admin dashboard (built in a later phase).
    """
    if request.user.is_authenticated:
        return redirect('core:home')

    if request.method == 'POST':
        form = AdminLoginForm(request, data=request.POST)
        if form.is_valid():
            user = form.get_user()
            login(request, user)
            messages.success(request, f'Welcome back, {user.username}!')
            LoginHistory.objects.create(
                user=user, event_type='login',
                ip_address=_client_ip(request),
                user_agent=request.META.get('HTTP_USER_AGENT', '')[:300],
            )

            next_url = request.POST.get('next') or request.GET.get('next')
            if next_url:
                return redirect(next_url)

            profile = Profile.objects.filter(user=user).first()
            if profile and profile.role in (UserRole.SUPER_ADMIN, UserRole.ADMIN):
                return redirect('core:dashboard')
            return redirect('/admin/')
        else:
            messages.error(request, 'Invalid username or password, or this account does not have admin access.')
    else:
        form = AdminLoginForm(request)

    context = {
        'page_title': 'Admin Login - OneTownCity',
        'form': form,
    }
    return render(request, 'login.html', context)


@login_required(login_url='core:admin_login')
def admin_logout(request):
    LoginHistory.objects.create(
        user=request.user, event_type='logout',
        ip_address=_client_ip(request),
        user_agent=request.META.get('HTTP_USER_AGENT', '')[:300],
    )
    logout(request)
    messages.info(request, 'You have been logged out successfully.')
    return redirect('core:home')


def contact(request):
    """
    Contact form. Saves every submission to the database (visible in the
    Django admin / a future dashboard inbox), then sends a notification
    email via Django's email backend (console backend in development —
    prints to terminal instead of a real inbox).
    """
    if request.method == 'POST':
        form = ContactForm(request.POST)
        if form.is_valid():
            name = form.cleaned_data['name']
            email = form.cleaned_data['email']
            subject = form.cleaned_data['subject']
            message = form.cleaned_data['message']

            ContactMessage.objects.create(name=name, email=email, subject=subject, message=message)

            full_message = f"From: {name} <{email}>\n\n{message}"

            send_mail(
                subject=f'[OneTownCity Contact] {subject}',
                message=full_message,
                from_email=settings.DEFAULT_FROM_EMAIL,
                recipient_list=[settings.CONTACT_RECEIVER_EMAIL],
                fail_silently=True,
            )
            messages.success(request, 'Thank you for reaching out! We will get back to you soon.')
            return redirect('core:contact')
    else:
        form = ContactForm()

    context = {
        'page_title': 'Contact Us - OneTownCity',
        'form': form,
    }
    return render(request, 'contact.html', context)


@require_POST
def newsletter_subscribe(request):
    """
    Footer 'Stay Updated' email signup, available on every page. Silently
    treats a re-submitted email as already-subscribed instead of erroring.
    """
    email = (request.POST.get('email') or '').strip()

    referer = request.META.get('HTTP_REFERER', '')
    if referer and url_has_allowed_host_and_scheme(referer, allowed_hosts={request.get_host()}, require_https=request.is_secure()):
        next_url = referer
    else:
        next_url = reverse('core:home')

    try:
        validate_email(email)
    except ValidationError:
        messages.error(request, 'Please enter a valid email address.')
        return redirect(next_url)

    _, created = NewsletterSubscriber.objects.get_or_create(email=email)
    if created:
        messages.success(request, "You're subscribed! We'll keep you updated on new listings and news.")
    else:
        messages.info(request, "That email is already subscribed.")
    return redirect(next_url)


def about(request):
    """
    About Us: OneTownCity platform profile (who we are, mission, vision,
    what we offer, how it works, team, and commitment).
    """
    context = {
        'page_title': 'About Us - OneTownCity',
    }
    return render(request, 'about.html', context)


def privacy_policy(request):
    """Static privacy policy page, linked from the footer."""
    return render(request, 'privacy_policy.html', {'page_title': 'Privacy Policy - OneTownCity'})


def terms_of_service(request):
    """Static terms of service page, linked from the footer."""
    return render(request, 'terms_of_service.html', {'page_title': 'Terms of Service - OneTownCity'})


# ===========================================================================
# Google Sign-In (Supabase) — bridges a verified Supabase identity into a
# normal Django session so the rest of the app keeps working unchanged.
# ===========================================================================

def _client_ip(request):
    xff = request.META.get('HTTP_X_FORWARDED_FOR')
    if xff:
        return xff.split(',')[0].strip()
    return request.META.get('REMOTE_ADDR')


def _unique_username(base_text):
    base = re.sub(r'[^\w.@+-]', '', (base_text or 'user').split('@')[0])[:30] or 'user'
    username = base
    n = 1
    while User.objects.filter(username=username).exists():
        n += 1
        username = f'{base}{n}'[:150]
    return username


def log_audit(request, action, description):
    """Records one row in the Super Admin's Audit Logs screen for a sensitive/
    destructive action. See dashboard_audit_logs."""
    AuditLog.objects.create(
        actor=request.user if request.user.is_authenticated else None,
        action=action,
        description=description[:300],
        ip_address=_client_ip(request),
    )


def _notify_super_admins(message, url='', type='admin_request_submitted'):
    admins = User.objects.filter(profile__role=UserRole.SUPER_ADMIN)
    notify_bulk(admins, type, message, url=url)


def _notify_city_admins(city, message, url='', type='listing_submitted'):
    """Like _notify_super_admins, but only the City Admins scoped to `city`
    (via AdminCityPermission) — so 'View submitted content' has something to
    surface without City Admins being cc'd on every city's activity."""
    if city is None:
        return
    admins = User.objects.filter(profile__role=UserRole.CITY_ADMIN, city_permissions__city=city).distinct()
    notify_bulk(admins, type, message, url=url)


def _post_login_redirect(profile):
    """Where to send someone right after a successful sign-in, based on onboarding state + role."""
    if not profile.profile_completed:
        return reverse('core:complete_profile')
    if profile.role == UserRole.USER and not profile.intent:
        return reverse('core:choose_intent')
    if profile.role in (UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.CITY_ADMIN):
        return reverse('core:dashboard')
    return reverse('core:home')


def google_login(request):
    """
    Main sign-in page: a 'Sign In' tab (username/password + Continue with
    Google) and a 'Register' tab (username/password self-signup that
    collects the full profile upfront). This is the single front door for
    every role — Explorer, Content Provider, and Super Admin alike.
    """
    if request.user.is_authenticated:
        return redirect('core:home')
    context = {
        'page_title': 'Sign In - OneTownCity',
        'login_form': PasswordLoginForm(request),
        'register_form': RegisterForm(),
        'active_tab': request.GET.get('tab', 'signin'),
    }
    return render(request, 'signin.html', context)


@require_POST
def password_login(request):
    """Handles the username/password form on the 'Sign In' tab."""
    form = PasswordLoginForm(request, data=request.POST)
    if form.is_valid():
        user = form.get_user()
        login(request, user)
        LoginHistory.objects.create(
            user=user, event_type='login',
            ip_address=_client_ip(request),
            user_agent=request.META.get('HTTP_USER_AGENT', '')[:300],
        )
        profile, _ = Profile.objects.get_or_create(user=user)
        messages.success(request, f'Welcome back, {profile.full_name or user.get_username()}!')
        return redirect(_post_login_redirect(profile))

    messages.error(request, 'Invalid username or password.')
    context = {
        'page_title': 'Sign In - OneTownCity',
        'login_form': form,
        'register_form': RegisterForm(),
        'active_tab': 'signin',
    }
    return render(request, 'signin.html', context)


@require_POST
def register(request):
    """Handles the 'Register' tab: username/password signup with the full profile collected upfront."""
    form = RegisterForm(request.POST, request.FILES)
    if form.is_valid():
        user = User.objects.create(
            username=_unique_username(form.cleaned_data['email']),
            email=form.cleaned_data['email'],
        )
        user.set_password(form.cleaned_data['password'])
        user.save()

        profile = Profile.objects.create(
            user=user,
            full_name=form.cleaned_data['full_name'],
            phone_number=form.cleaned_data['phone_number'],
            profile_photo=form.cleaned_data.get('profile_photo'),
            address=form.cleaned_data['address'],
            city=form.cleaned_data['city'],
            state=form.cleaned_data['state'],
            pincode=form.cleaned_data['pincode'],
            profile_completed=True,
        )

        user.backend = 'django.contrib.auth.backends.ModelBackend'
        login(request, user)
        LoginHistory.objects.create(
            user=user, event_type='login',
            ip_address=_client_ip(request),
            user_agent=request.META.get('HTTP_USER_AGENT', '')[:300],
        )
        messages.success(request, 'Account created! One more step.')
        return redirect(_post_login_redirect(profile))

    context = {
        'page_title': 'Sign In - OneTownCity',
        'login_form': PasswordLoginForm(request),
        'register_form': form,
        'active_tab': 'register',
    }
    return render(request, 'signin.html', context)


@ensure_csrf_cookie
def auth_callback_page(request):
    """
    Where Supabase redirects back to after Google auth. The page's JS grabs
    the resulting session and POSTs the access token to auth_callback_api.
    """
    return render(request, 'auth_callback.html', {'page_title': 'Signing you in... - OneTownCity'})


@require_POST
def auth_callback_api(request):
    """
    JSON endpoint consumed by auth_callback.html: verifies the Supabase
    access token, creates/updates the local User + Profile on first sign-in,
    and logs the user into a normal Django session.
    """
    try:
        payload = json.loads(request.body or '{}')
    except ValueError:
        return JsonResponse({'error': 'Invalid request.'}, status=400)

    try:
        supabase_user = fetch_supabase_user(payload.get('access_token'))
    except SupabaseAuthError as exc:
        return JsonResponse({'error': str(exc)}, status=401)

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
            user = User.objects.create(username=_unique_username(email or supabase_user.id), email=email)
        profile, _ = Profile.objects.get_or_create(user=user)
        profile.supabase_uid = supabase_user.id

    if profile.is_blocked:
        return JsonResponse({'error': 'This account has been blocked. Contact support if you think this is a mistake.'}, status=403)

    if full_name and not profile.full_name:
        profile.full_name = full_name
    if avatar_url and not profile.profile_photo:
        profile.profile_photo_url = avatar_url
    profile.last_active_at = timezone.now()
    profile.save()

    user.backend = 'django.contrib.auth.backends.ModelBackend'
    login(request, user)

    LoginHistory.objects.create(
        user=user, event_type='login',
        ip_address=_client_ip(request),
        user_agent=request.META.get('HTTP_USER_AGENT', '')[:300],
    )

    if not profile.profile_completed:
        next_url = reverse('core:complete_profile')
    elif profile.role == UserRole.USER and not profile.intent:
        next_url = reverse('core:choose_intent')
    elif profile.role in (UserRole.SUPER_ADMIN, UserRole.ADMIN):
        next_url = reverse('core:dashboard')
    else:
        next_url = reverse('core:home')

    return JsonResponse({'redirect': next_url})


# ===========================================================================
# Onboarding: profile completion -> "what brings you here?" -> admin request
# ===========================================================================

@login_required(login_url=settings.GOOGLE_LOGIN_URL)
def complete_profile(request):
    profile, _ = Profile.objects.get_or_create(user=request.user)
    if profile.profile_completed:
        if profile.role == UserRole.USER and not profile.intent:
            return redirect('core:choose_intent')
        return redirect('core:home')

    if request.method == 'POST':
        form = ProfileCompletionForm(request.POST, request.FILES, instance=profile)
        if form.is_valid():
            profile = form.save(commit=False)
            profile.profile_completed = True
            profile.save()
            messages.success(request, 'Profile completed!')
            if profile.role == UserRole.USER:
                return redirect('core:choose_intent')
            return redirect('core:dashboard')
    else:
        form = ProfileCompletionForm(instance=profile, initial={'full_name': profile.full_name or request.user.get_full_name()})

    context = {
        'page_title': 'Complete Your Profile - OneTownCity',
        'form': form,
        'email': request.user.email,
    }
    return render(request, 'complete_profile.html', context)


@login_required(login_url=settings.GOOGLE_LOGIN_URL)
def choose_intent(request):
    profile, _ = Profile.objects.get_or_create(user=request.user)
    if not profile.profile_completed:
        return redirect('core:complete_profile')

    if request.method == 'POST':
        choice = request.POST.get('intent')
        if choice not in (Intent.UPLOAD, Intent.EXPLORE):
            messages.error(request, 'Please choose an option.')
        else:
            profile.intent = choice
            profile.save(update_fields=['intent'])
            if choice == Intent.UPLOAD:
                return redirect('core:admin_request_new')
            messages.success(request, 'Welcome to OneTownCity!')
            return redirect('core:home')

    return render(request, 'choose_intent.html', {'page_title': 'Welcome - OneTownCity'})


@onboarding_required
def admin_request_new(request):
    profile = request.profile
    if profile.role in (UserRole.ADMIN, UserRole.SUPER_ADMIN):
        return redirect('core:my_listings')

    existing = (
        AdminRequest.objects.filter(user=request.user)
        .exclude(status=AdminRequestStatus.REJECTED)
        .order_by('-created_at').first()
    )
    if existing and existing.status in (AdminRequestStatus.PENDING, AdminRequestStatus.APPROVED):
        return redirect('core:admin_request_pending')

    resubmitting = existing if existing and existing.status == AdminRequestStatus.CHANGES_REQUESTED else None

    if request.method == 'POST':
        form = AdminRequestForm(request.POST)
        if form.is_valid():
            if resubmitting:
                admin_request = resubmitting
                admin_request.status = AdminRequestStatus.PENDING
                admin_request.review_note = ''
                admin_request.save()
            else:
                admin_request = AdminRequest.objects.create(user=request.user)
            admin_request.categories.set(form.cleaned_data['categories'])
            _notify_super_admins(
                f'{profile.full_name or request.user.email} requested Content Provider access.',
                url=reverse('core:dashboard_admin_request_detail', args=[admin_request.pk]),
            )
            messages.success(request, 'Your request has been submitted for review.', extra_tags='celebrate-confetti')
            return redirect('core:admin_request_pending')
    else:
        initial = {'categories': resubmitting.categories.all()} if resubmitting else {}
        form = AdminRequestForm(initial=initial)

    selected_ids = {c.pk for c in form.initial.get('categories', [])} if not request.method == 'POST' \
        else {int(v) for v in request.POST.getlist('categories') if v.isdigit()}

    context = {
        'page_title': 'What Do You Want to Manage? - OneTownCity',
        'form': form,
        'resubmitting': bool(resubmitting),
        'top_categories': Category.objects.filter(parent=None, is_active=True).prefetch_related(
            Prefetch('children', queryset=Category.objects.filter(is_active=True).order_by('order', 'label'))
        ).order_by('order', 'label'),
        'selected_ids': selected_ids,
    }
    return render(request, 'admin_request_form.html', context)


@onboarding_required
def admin_request_pending(request):
    if request.profile.role in (UserRole.ADMIN, UserRole.SUPER_ADMIN):
        return redirect('core:my_listings')
    admin_request = AdminRequest.objects.filter(user=request.user).order_by('-created_at').first()
    context = {'page_title': 'Application Status - OneTownCity', 'admin_request': admin_request}
    return render(request, 'admin_request_pending.html', context)


# ===========================================================================
# Community features (generic across Business/Property/Job/Event/News)
# ===========================================================================

@onboarding_required
@require_POST
def toggle_like(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    ct = ContentType.objects.get_for_model(model_cls)
    like, created = Like.objects.get_or_create(content_type=ct, object_id=obj.pk, user=request.user)
    if not created:
        like.delete()
    return redirect(obj.get_absolute_url())


@onboarding_required
@require_POST
def toggle_favorite(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    ct = ContentType.objects.get_for_model(model_cls)
    favorite, created = Favorite.objects.get_or_create(content_type=ct, object_id=obj.pk, user=request.user)
    if not created:
        favorite.delete()
    return redirect(obj.get_absolute_url())


@onboarding_required
@require_POST
def add_comment(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    form = CommentForm(request.POST)
    if form.is_valid():
        ct = ContentType.objects.get_for_model(model_cls)
        parent = None
        parent_id = form.cleaned_data.get('parent_id')
        if parent_id:
            parent = Comment.objects.filter(pk=parent_id, content_type=ct, object_id=obj.pk).first()
        Comment.objects.create(
            content_type=ct, object_id=obj.pk, user=request.user,
            parent=parent, body=form.cleaned_data['body'],
        )
        messages.success(request, 'Comment posted.')
    else:
        messages.error(request, 'Please write a comment before posting.')
    return redirect(obj.get_absolute_url())


@onboarding_required
@require_POST
def add_review(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    form = ReviewForm(request.POST)
    if form.is_valid():
        ct = ContentType.objects.get_for_model(model_cls)
        Review.objects.update_or_create(
            content_type=ct, object_id=obj.pk, user=request.user,
            defaults={'rating': form.cleaned_data['rating'], 'body': form.cleaned_data['body']},
        )
        messages.success(request, 'Thanks for your review!', extra_tags='celebrate-burst')
    else:
        messages.error(request, 'Please choose a rating.')
    return redirect(obj.get_absolute_url())


@onboarding_required
@require_POST
def report_listing(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    form = ReportForm(request.POST)
    if form.is_valid():
        ct = ContentType.objects.get_for_model(model_cls)
        Report.objects.create(
            content_type=ct, object_id=obj.pk, user=request.user,
            reason=form.cleaned_data['reason'], details=form.cleaned_data['details'],
        )
        messages.success(request, 'Thanks — our team will review this listing.')
    else:
        messages.error(request, 'Please provide a reason for the report.')
    return redirect(obj.get_absolute_url())


@require_POST
def record_share(request, model_key, pk):
    """Logs a share click. Open to anonymous visitors too — sharing doesn't require an account."""
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    platform = request.POST.get('platform', 'copy_link')
    if platform not in dict(Share.PLATFORM_CHOICES):
        platform = 'copy_link'
    ct = ContentType.objects.get_for_model(model_cls)
    Share.objects.create(
        content_type=ct, object_id=obj.pk,
        user=request.user if request.user.is_authenticated else None,
        platform=platform,
    )
    return JsonResponse({'ok': True})


@onboarding_required
def my_favorites(request):
    favorites = (
        Favorite.objects.filter(user=request.user)
        .select_related('content_type').order_by('-created_at')
    )
    items = [
        {'obj': f.content_object, 'label': f.content_object._meta.verbose_name}
        for f in favorites if f.content_object is not None
    ]
    return render(request, 'favorites.html', {'page_title': 'My Favorites - OneTownCity', 'items': items, 'active_nav': 'favorites'})


# ===========================================================================
# Notifications
# ===========================================================================

@onboarding_required
def notifications_list(request):
    notification_qs = request.user.notifications.all()
    return render(request, 'notifications.html', {
        'page_title': 'Notifications - OneTownCity',
        'notification_qs': notification_qs,
    })


@onboarding_required
@require_POST
def notification_mark_read(request, pk):
    Notification.objects.filter(pk=pk, recipient=request.user).update(is_read=True)
    return redirect(request.POST.get('next') or reverse('core:notifications_list'))


@onboarding_required
@require_POST
def notifications_mark_all_read(request):
    request.user.notifications.filter(is_read=False).update(is_read=True)
    return redirect(request.POST.get('next') or reverse('core:notifications_list'))


@onboarding_required
def notification_open(request, pk):
    """
    WhatsApp-style click-through: opening a notification from the bell tray
    marks it read and deep-links straight to its target (the exact post,
    dashboard workspace, etc.) in one step, instead of the notifications
    page's separate "mark read" button + link.
    """
    notification = get_object_or_404(Notification, pk=pk, recipient=request.user)
    if not notification.is_read:
        notification.is_read = True
        notification.save(update_fields=['is_read'])
    return redirect(notification.url or reverse('core:notifications_list'))


@onboarding_required
@require_POST
def push_subscribe(request):
    """
    Saves (or refreshes) the browser's Push API subscription for the signed-
    in user — called by static/js/push-notifications.js right after
    `pushManager.subscribe()` succeeds. One row per browser/device; the
    endpoint URL is unique per subscription so re-subscribing the same
    browser just updates its keys instead of duplicating the row.
    """
    try:
        data = json.loads(request.body)
        keys = data['keys']
        endpoint = data['endpoint']
    except (KeyError, ValueError):
        return JsonResponse({'ok': False, 'error': 'Malformed subscription payload.'}, status=400)

    PushSubscription.objects.update_or_create(
        endpoint=endpoint,
        defaults={
            'user': request.user,
            'p256dh': keys.get('p256dh', ''),
            'auth': keys.get('auth', ''),
            'user_agent': request.META.get('HTTP_USER_AGENT', '')[:255],
        },
    )
    return JsonResponse({'ok': True})


@require_POST
def push_unsubscribe(request):
    """Drops a subscription (called when the browser reports it's no longer valid, or on explicit opt-out)."""
    try:
        endpoint = json.loads(request.body)['endpoint']
    except (KeyError, ValueError):
        return JsonResponse({'ok': False, 'error': 'Malformed payload.'}, status=400)
    PushSubscription.objects.filter(endpoint=endpoint).delete()
    return JsonResponse({'ok': True})


@onboarding_required
def notifications_unread_count(request):
    """Polled by notification-tray.js to keep the bell badge live without a full reload."""
    return JsonResponse({'count': request.user.notifications.filter(is_read=False).count()})


# ===========================================================================
# Content Provider (Admin): "My Listings" + submission wizard
# ===========================================================================

def _group_categories_by_top(categories):
    """
    Buckets a flat iterable of (possibly-child) Category rows under their
    top-level ancestor, preserving order. Used to turn a permission-filtered
    category list into the same collapsible parent/subcategory groups as
    Manage Categories and the onboarding checklist, instead of one long flat
    row of pills mixing parents and children together.
    """
    groups, order = {}, []
    for cat in categories:
        top = cat.parent if cat.parent_id else cat
        if top.pk not in groups:
            groups[top.pk] = {'top': top, 'top_permitted': False, 'children': []}
            order.append(top.pk)
        if cat.pk == top.pk:
            groups[top.pk]['top_permitted'] = True
        else:
            groups[top.pk]['children'].append(cat)
    return [groups[pk] for pk in order]


@onboarding_required
def my_listings(request):
    profile = request.profile
    if profile.role not in (UserRole.ADMIN, UserRole.SUPER_ADMIN, UserRole.CITY_ADMIN, UserRole.SUB_ADMIN):
        messages.info(request, 'Apply to become a Content Provider to add listings.')
        return redirect('core:admin_request_new')

    items = []
    for key, model_cls in LISTING_MODELS.items():
        qs = model_cls.objects.all() if profile.is_super_admin else model_cls.objects.filter(owner=request.user)
        for obj in qs.select_related('listing_category'):
            items.append({'model_key': key, 'obj': obj})
    items.sort(key=lambda item: item['obj'].created_at, reverse=True)

    # managed_category_ids() already covers every role uniformly: every
    # active category for Super Admin/City Admin, the content-permission-
    # filtered subset for a Sub Admin, and AdminCategoryPermission grants for
    # a plain Content Provider (see Profile.managed_category_ids).
    permitted_categories = Category.objects.filter(
        is_active=True, id__in=profile.managed_category_ids(),
    ).select_related('parent')

    context = {
        'page_title': 'My Listings - OneTownCity',
        'items': items,
        'permitted_category_groups': _group_categories_by_top(permitted_categories.order_by('order', 'label')),
        'active_nav': 'my_listings',
    }
    return render(request, 'dashboard/my_listings.html', context)


def _restrict_city_field(form, profile):
    """
    Confines the listing form's `city` field to cities the acting City Admin
    or Sub Admin actually manages, so neither can publish/attribute a listing
    to a city outside their scope (Super Admin and plain Content Providers
    keep the field's full city list — they aren't city-scoped).
    """
    if profile.is_city_admin or profile.is_sub_admin:
        form.fields['city'].queryset = Location.objects.filter(id__in=profile.managed_city_ids())


def _resolve_submission_status(profile, listing_model, save_mode):
    """
    Where an add/edit submission lands. 'draft' (the Content Provider's
    "Save as Draft" button — see listing_submit.html) always keeps it out of
    every review queue until they come back and submit it; otherwise Super
    Admin/City Admin publish immediately (as does News, and anything while
    PlatformSettings.auto_approve_listings is on), and everyone else enters
    the pending review queue for a City Admin/permitted Sub Admin to accept
    or reject.
    """
    if save_mode == 'draft':
        return ListingStatus.DRAFT
    if profile.is_super_admin or profile.is_city_admin or listing_model == 'news' or PlatformSettings.load().auto_approve_listings:
        return ListingStatus.APPROVED
    return ListingStatus.PENDING


@onboarding_required
def listing_submit(request, category_key):
    profile = request.profile
    category = get_object_or_404(Category, key=category_key, is_active=True)
    if not profile.can_manage_category(category):
        messages.error(request, "You don't have permission to add listings in this category.")
        return redirect('core:my_listings')

    form_cls = LISTING_SUBMIT_FORMS[category.listing_model]

    if request.method == 'POST':
        form = form_cls(request.POST, request.FILES)
        _restrict_city_field(form, profile)
        if form.is_valid():
            obj = form.save(commit=False)
            obj.owner = request.user
            obj.listing_category = category
            if not CityModule.is_enabled_for_city(category.listing_model, obj.city_id):
                messages.error(request, f'{category.label} listings are currently unavailable in this city.')
            else:
                save_mode = request.POST.get('save_mode', 'submit')
                obj.status = _resolve_submission_status(profile, category.listing_model, save_mode)
                if obj.status == ListingStatus.APPROVED:
                    obj.reviewed_by = request.user
                    obj.reviewed_at = timezone.now()
                    obj.save()
                    messages.success(request, 'Your listing was published.', extra_tags='celebrate-confetti')
                elif obj.status == ListingStatus.DRAFT:
                    obj.save()
                    messages.success(request, 'Saved as a draft — submit it for approval whenever you\'re ready.')
                else:
                    obj.save()
                    _notify_super_admins(
                        f'New {category.label} listing submitted: "{obj}"',
                        url=reverse('core:dashboard_post_detail', args=[category.listing_model, obj.pk]),
                        type='listing_submitted',
                    )
                    _notify_city_admins(
                        obj.city,
                        f'New {category.label} listing submitted: "{obj}"',
                        url=reverse('core:dashboard_post_detail', args=[category.listing_model, obj.pk]),
                        type='listing_submitted',
                    )
                    log_audit(request, 'content.submit', f'{profile.get_role_display()} submitted "{obj}" for approval')
                    messages.success(request, 'Your listing was submitted and is pending approval.', extra_tags='celebrate-confetti')
                return redirect('core:my_listings')
    else:
        initial = {}
        current = active_location(request)
        if current:
            initial['city'] = current.pk
        if category.listing_model == 'business' and category.business_subcategory:
            initial['category'] = category.business_subcategory
        form = form_cls(initial=initial)
        _restrict_city_field(form, profile)

    context = {
        'page_title': f'Add {category.label} - OneTownCity', 'form': form, 'category': category,
        'allow_draft': profile.is_admin, 'active_nav': 'my_listings',
    }
    return render(request, 'dashboard/listing_submit.html', context)


@onboarding_required
def listing_edit(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    profile = request.profile
    obj = get_object_or_404(model_cls, pk=pk)
    if not profile.is_super_admin and obj.owner_id != request.user.id:
        messages.error(request, 'You can only edit your own listings.')
        return redirect('core:my_listings')

    form_cls = LISTING_SUBMIT_FORMS[model_key]
    was_draft = obj.status == ListingStatus.DRAFT

    if request.method == 'POST':
        form = form_cls(request.POST, request.FILES, instance=obj)
        _restrict_city_field(form, profile)
        if form.is_valid():
            obj = form.save(commit=False)
            was_rejected = obj.status in (ListingStatus.REJECTED, ListingStatus.CHANGES_REQUESTED)
            # A listing that's already been submitted once can't be sent
            # back to draft through an edit — 'draft' is only honored while
            # it's still a draft; every other edit is itself a (re)submission.
            save_mode = request.POST.get('save_mode', 'submit') if was_draft else 'submit'
            obj.status = _resolve_submission_status(profile, model_key, save_mode)
            if obj.status in (ListingStatus.APPROVED, ListingStatus.PENDING):
                obj.rejection_reason = ''
            if obj.status == ListingStatus.APPROVED:
                obj.reviewed_by = request.user
                obj.reviewed_at = timezone.now()
            obj.save()

            if obj.status == ListingStatus.DRAFT:
                messages.success(request, 'Draft updated.')
            elif obj.status == ListingStatus.APPROVED:
                messages.success(request, 'Listing updated.')
            else:
                note = ' It has been submitted and is pending approval.' if was_draft else ' It will be reviewed again before going live.'
                messages.success(request, f'Listing updated.{note}')
            if obj.status == ListingStatus.PENDING:
                verb = 'resubmitted' if was_rejected else 'submitted'
                log_audit(request, 'content.submit', f'{profile.get_role_display()} {verb} "{obj}" for approval')
            return redirect('core:my_listings')
    else:
        form = form_cls(instance=obj)
        _restrict_city_field(form, profile)

    ct = ContentType.objects.get_for_model(obj)
    context = {
        'page_title': f'Edit {obj} - OneTownCity',
        'form': form,
        'object': obj,
        'model_key': model_key,
        'allow_draft': was_draft,
        'gallery_images': PostImage.objects.filter(content_type=ct, object_id=obj.pk),
        'gallery_videos': PostVideo.objects.filter(content_type=ct, object_id=obj.pk),
        'active_nav': 'my_listings',
    }
    return render(request, 'dashboard/listing_submit.html', context)


@onboarding_required
@require_POST
def listing_delete(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    profile = request.profile
    obj = get_object_or_404(model_cls, pk=pk)
    if not _can_manage_post(profile, obj):
        messages.error(request, 'You can only delete your own listings.')
        return redirect('core:my_listings')

    owner, title = obj.owner, str(obj)
    obj.delete()
    if owner and owner.id != request.user.id:
        notify(
            owner, 'listing_deleted',
            f'Your listing "{title}" was deleted by a Super Admin.' if profile.is_super_admin
            else f'Your listing "{title}" was deleted by an admin.',
            url=reverse('core:my_listings'),
        )
    messages.success(request, 'Listing deleted.')
    return redirect(_safe_next(request, reverse('core:my_listings')))


@onboarding_required
@require_POST
def my_listing_media_add(request, model_key, pk):
    """Owner (or moderating Admin/Super Admin) adds photos/videos to their own gallery."""
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    if not _can_manage_post(request.profile, obj):
        messages.error(request, 'You can only manage the gallery of your own listings.')
        return redirect('core:my_listings')

    ct = ContentType.objects.get_for_model(obj)
    images, image_errors = _validate_gallery_files(
        request.FILES.getlist('images'), GALLERY_IMAGE_TYPES, GALLERY_IMAGE_MAX_BYTES, 'image'
    )
    videos, video_errors = _validate_gallery_files(
        request.FILES.getlist('videos'), GALLERY_VIDEO_TYPES, GALLERY_VIDEO_MAX_BYTES, 'video'
    )
    for err in image_errors + video_errors:
        messages.error(request, err)

    added = 0
    if images:
        start_order = PostImage.objects.filter(content_type=ct, object_id=obj.pk).count()
        for i, uploaded_file in enumerate(images):
            PostImage.objects.create(
                content_type=ct, object_id=obj.pk, image=uploaded_file,
                order=start_order + i, uploaded_by=request.user,
            )
        added += len(images)
    if videos:
        start_order = PostVideo.objects.filter(content_type=ct, object_id=obj.pk).count()
        for i, uploaded_file in enumerate(videos):
            PostVideo.objects.create(
                content_type=ct, object_id=obj.pk, video=uploaded_file,
                order=start_order + i, uploaded_by=request.user,
            )
        added += len(videos)

    if added:
        messages.success(request, f'{added} gallery item(s) added.')
    elif not (image_errors or video_errors):
        messages.error(request, 'Choose at least one photo or video to upload.')
    return redirect(_safe_next(request, reverse('core:listing_edit', args=[model_key, pk])))


def _get_owned_media(media_cls, pk, profile):
    """Looks up a PostImage/PostVideo and checks the requester may manage its parent listing."""
    media = get_object_or_404(media_cls, pk=pk)
    obj = media.content_object
    if obj is None or not _can_manage_post(profile, obj):
        return None, None
    return media, obj


@onboarding_required
@require_POST
def my_listing_media_delete(request, media_type, pk):
    media_cls = {'image': PostImage, 'video': PostVideo}.get(media_type)
    if media_cls is None:
        raise Http404('Unknown media type')
    media, obj = _get_owned_media(media_cls, pk, request.profile)
    if media is None:
        messages.error(request, 'You can only manage the gallery of your own listings.')
        return redirect('core:my_listings')
    model_key = media.content_type.model
    media.delete()
    messages.success(request, f'{media_type.title()} deleted.')
    return redirect(_safe_next(request, reverse('core:listing_edit', args=[model_key, obj.pk])))


@onboarding_required
@require_POST
def my_listing_media_set_cover(request, pk):
    media, obj = _get_owned_media(PostImage, pk, request.profile)
    if media is None:
        messages.error(request, 'You can only manage the gallery of your own listings.')
        return redirect('core:my_listings')
    model_key = media.content_type.model

    if obj.image:
        obj.image.delete(save=False)
    obj.image = None
    obj.image_url = media.image.url
    obj.save(update_fields=['image', 'image_url'])

    messages.success(request, 'Cover photo updated.')
    return redirect(_safe_next(request, reverse('core:listing_edit', args=[model_key, obj.pk])))


# ===========================================================================
# Dashboard (role-dispatched: Super Admin gets the full control panel,
# Admins land on their own "My Listings")
# ===========================================================================

@onboarding_required
def dashboard(request):
    profile = request.profile
    if profile.role == UserRole.SUPER_ADMIN:
        return _super_admin_dashboard(request)
    if profile.role == UserRole.CITY_ADMIN:
        return _city_admin_dashboard(request)
    if profile.role == UserRole.SUB_ADMIN:
        return _sub_admin_dashboard(request)
    if profile.role == UserRole.ADMIN:
        return _admin_dashboard(request)
    return _user_dashboard(request)


@onboarding_required
def dashboard_profile(request):
    """Standalone 'My Profile' page — the same edit-your-own-details form
    every role's Overview used to embed inline, now split out so Overview
    stays dedicated to at-a-glance metrics."""
    profile = request.profile
    if request.method == 'POST':
        form = ProfileCompletionForm(request.POST, request.FILES, instance=profile)
        if form.is_valid():
            form.save()
            messages.success(request, 'Profile updated!')
            return redirect('core:dashboard_profile')
    else:
        form = ProfileCompletionForm(instance=profile)

    context = {
        'page_title': 'My Profile - OneTownCity',
        'form': form,
        'active_nav': 'profile',
    }
    return render(request, 'dashboard/profile.html', context)


def _owner_engagement_analytics(own_items):
    """
    Real engagement analytics for a Content Provider's own listings, built
    from the PostView/Like/Comment/Review/Share event tables — every row of
    those carries its own created_at, unlike the single running
    view_count/like_count/etc counters on the listing itself, so this is the
    only honest source for "how many this week" or "views per day over the
    last two weeks" (as opposed to fabricating numbers). Powers the Overview
    page's YouTube-style metric cards and timeline/breakdown charts.
    """
    now = timezone.now()
    week_ago = now - timedelta(days=7)
    two_weeks_ago = now - timedelta(days=14)
    window_start = (now - timedelta(days=13)).date()

    ct_ids = {}
    for item in own_items:
        ct = ContentType.objects.get_for_model(type(item['obj']))
        ct_ids.setdefault(ct, []).append(item['obj'].pk)

    def event_qs(model_cls):
        if not ct_ids:
            return model_cls.objects.none()
        q = Q()
        for ct, ids in ct_ids.items():
            q |= Q(content_type=ct, object_id__in=ids)
        return model_cls.objects.filter(q)

    views_qs = event_qs(PostView)
    likes_qs = event_qs(Like)
    shares_qs = event_qs(Share)
    reviews_qs = event_qs(Review)
    comments_qs = event_qs(Comment)

    def window_count(qs_list, start, end=None):
        total = 0
        for qs in qs_list:
            filtered = qs.filter(created_at__gte=start)
            if end is not None:
                filtered = filtered.filter(created_at__lt=end)
            total += filtered.count()
        return total

    def trend_pct(qs_list):
        recent = window_count(qs_list, week_ago)
        prior = window_count(qs_list, two_weeks_ago, week_ago)
        if prior == 0:
            return None if recent == 0 else 100.0
        return round((recent - prior) / prior * 100, 1)

    def daily_series(qs):
        rows = (
            qs.filter(created_at__date__gte=window_start)
            .annotate(day=TruncDate('created_at'))
            .values('day')
            .annotate(n=Count('id'))
        )
        by_day = {row['day']: row['n'] for row in rows}
        return [by_day.get(window_start + timedelta(days=i), 0) for i in range(14)]

    daily_engagement = [
        a + b + c + d for a, b, c, d in zip(
            daily_series(likes_qs), daily_series(shares_qs),
            daily_series(reviews_qs), daily_series(comments_qs),
        )
    ]

    top_posts = sorted(
        own_items,
        key=lambda i: i['obj'].like_count + i['obj'].share_count + i['obj'].review_count,
        reverse=True,
    )[:6]

    return {
        'labels': [(window_start + timedelta(days=i)).strftime('%b %d') for i in range(14)],
        'daily_views': daily_series(views_qs),
        'daily_engagement': daily_engagement,
        'trend_views': trend_pct([views_qs]),
        'trend_likes': trend_pct([likes_qs]),
        'trend_shares': trend_pct([shares_qs]),
        'trend_reviews': trend_pct([reviews_qs, comments_qs]),
        'top_posts_chart': {
            'labels': [str(i['obj'])[:18] for i in top_posts],
            'likes': [i['obj'].like_count for i in top_posts],
            'shares': [i['obj'].share_count for i in top_posts],
            'reviews': [i['obj'].review_count for i in top_posts],
        },
    }


def _admin_dashboard(request):
    """Content Provider ('Manager') workspace: their own listings' health at
    a glance, plus which categories they're permitted to publish into. Reuses
    exactly the data my_listings already computes per-owner — this is a
    summary landing page in front of that full table, not a parallel source
    of truth."""
    own_items = []
    for key, model_cls in LISTING_MODELS.items():
        qs = model_cls.objects.filter(owner=request.user).select_related('listing_category')
        for obj in qs:
            own_items.append({'model_key': key, 'obj': obj})
    own_items.sort(key=lambda item: item['obj'].created_at, reverse=True)

    stats = {
        'total': len(own_items),
        'pending': sum(1 for i in own_items if i['obj'].status == ListingStatus.PENDING),
        'approved': sum(1 for i in own_items if i['obj'].status == ListingStatus.APPROVED),
        'rejected': sum(1 for i in own_items if i['obj'].status == ListingStatus.REJECTED),
        'total_views': sum(i['obj'].view_count for i in own_items),
        'total_likes': sum(i['obj'].like_count for i in own_items),
        'total_shares': sum(i['obj'].share_count for i in own_items),
        'total_reviews': sum(i['obj'].review_count + i['obj'].comment_count for i in own_items),
    }

    context = {
        'page_title': 'Manager Workspace - OneTownCity',
        'stats': stats,
        'analytics': _owner_engagement_analytics(own_items),
        'recent_items': own_items[:8],
        'permitted_category_groups': _group_categories_by_top(
            Category.objects.filter(is_active=True, admin_permissions__admin=request.user)
            .select_related('parent').order_by('order', 'label')
        ),
        'active_nav': 'overview',
    }
    return render(request, 'dashboard/admin_dashboard.html', context)


def _user_dashboard(request):
    """Explorer's personal workspace: active status at a glance (favorites,
    notifications)."""
    context = {
        'page_title': 'My Workspace - OneTownCity',
        'favorites_count': Favorite.objects.filter(user=request.user).count(),
        'active_nav': 'overview',
    }
    return render(request, 'dashboard/user_dashboard.html', context)


def _super_admin_dashboard(request):
    # Each of these used to be 1-3 separate .count() calls per model (worth
    # ~40 queries total measured on this page alone) — collapsed into one
    # conditional-aggregate query per table, since same-table counts can
    # share a single query but cross-table ones can't.
    listing_status_totals = [
        m.objects.aggregate(
            pending=Count('id', filter=Q(status=ListingStatus.PENDING)),
            approved=Count('id', filter=Q(status=ListingStatus.APPROVED)),
            rejected=Count('id', filter=Q(status=ListingStatus.REJECTED)),
        )
        for m in LISTING_MODELS.values()
    ]
    pending_listings = sum(t['pending'] for t in listing_status_totals)
    approved_listings = sum(t['approved'] for t in listing_status_totals)
    rejected_listings = sum(t['rejected'] for t in listing_status_totals)

    profile_counts = Profile.objects.aggregate(
        total_users=Count('id', filter=Q(role=UserRole.USER)),
        total_admins=Count('id', filter=Q(role=UserRole.ADMIN)),
    )
    request_counts = AdminRequest.objects.aggregate(
        pending_requests=Count('id', filter=Q(status=AdminRequestStatus.PENDING)),
        approved_requests=Count('id', filter=Q(status=AdminRequestStatus.APPROVED)),
        rejected_requests=Count('id', filter=Q(status=AdminRequestStatus.REJECTED)),
    )
    business_counts = Business.objects.aggregate(
        total=Count('id'),
        restaurants=Count('id', filter=Q(category='restaurant')),
        schools=Count('id', filter=Q(category='school')),
        colleges=Count('id', filter=Q(category='college')),
        hospitals=Count('id', filter=Q(category='hospital')),
    )
    message_counts = ContactMessage.objects.aggregate(
        total=Count('id'),
        unread=Count('id', filter=Q(is_read=False)),
    )

    stats = {
        'total_users': profile_counts['total_users'],
        'total_admins': profile_counts['total_admins'],
        'pending_requests': request_counts['pending_requests'],
        'approved_requests': request_counts['approved_requests'],
        'rejected_requests': request_counts['rejected_requests'],
        'total_businesses': business_counts['total'],
        'total_restaurants': business_counts['restaurants'],
        'total_schools': business_counts['schools'],
        'total_colleges': business_counts['colleges'],
        'total_hospitals': business_counts['hospitals'],
        'total_properties': Property.objects.count(),
        'total_jobs': Job.objects.count(),
        'total_events': Event.objects.count(),
        'total_news': News.objects.count(),
        'total_projects': Project.objects.count(),
        'pending_listings': pending_listings,
        'approved_listings': approved_listings,
        'rejected_listings': rejected_listings,
        'total_comments': Comment.objects.count(),
        'total_reviews': Review.objects.count(),
        'total_likes': Like.objects.count(),
        'total_favorites': Favorite.objects.count(),
        'total_shares': Share.objects.count(),
        'total_messages': message_counts['total'],
        'unread_messages': message_counts['unread'],
    }

    context = {
        'page_title': 'Super Admin Dashboard - OneTownCity',
        'stats': stats,
        # 'user__profile' added: the template reads req.user.profile.full_name
        # / entry.user.profile.full_name per row, which without this was a
        # separate query per row (15 extra queries for a 5+10 row page).
        'recent_requests': AdminRequest.objects.select_related('user', 'user__profile').prefetch_related('categories').order_by('-created_at')[:5],
        'recent_logins': LoginHistory.objects.select_related('user', 'user__profile').filter(event_type='login').order_by('-created_at')[:10],
        'recent_messages': ContactMessage.objects.order_by('-created_at')[:5],
        'active_nav': 'overview',
    }
    return render(request, 'dashboard/super_admin_dashboard.html', context)


def _city_admin_dashboard(request):
    """
    City Admin's Overview: the same shape as _super_admin_dashboard, scoped
    to city_id__in=profile.managed_city_ids(). This also serves as 'View
    city-specific analytics' — no separate analytics page, matching how
    Super Admin's own analytics live on its Overview rather than elsewhere.
    """
    profile = request.profile
    city_ids = profile.managed_city_ids()

    listing_status_totals = [
        m.objects.filter(city_id__in=city_ids).aggregate(
            pending=Count('id', filter=Q(status=ListingStatus.PENDING)),
            approved=Count('id', filter=Q(status=ListingStatus.APPROVED)),
            rejected=Count('id', filter=Q(status=ListingStatus.REJECTED)),
            total=Count('id'),
        )
        for m in LISTING_MODELS.values()
    ]

    stats = {
        'total_listings': sum(t['total'] for t in listing_status_totals),
        'pending_listings': sum(t['pending'] for t in listing_status_totals),
        'approved_listings': sum(t['approved'] for t in listing_status_totals),
        'rejected_listings': sum(t['rejected'] for t in listing_status_totals),
        'total_businesses': Business.objects.filter(city_id__in=city_ids).count(),
        'total_properties': Property.objects.filter(city_id__in=city_ids).count(),
        'total_jobs': Job.objects.filter(city_id__in=city_ids).count(),
        'total_events': Event.objects.filter(city_id__in=city_ids).count(),
        'total_news': News.objects.filter(city_id__in=city_ids).count(),
        'total_projects': Project.objects.filter(city_id__in=city_ids).count(),
        'total_sub_admins': Profile.objects.filter(
            role=UserRole.SUB_ADMIN, user__city_permissions__city_id__in=city_ids
        ).distinct().count(),
        'total_content_providers': Profile.objects.filter(
            role=UserRole.ADMIN, user__city_permissions__city_id__in=city_ids
        ).distinct().count(),
    }

    recent_items = []
    for key, model_cls in LISTING_MODELS.items():
        qs = model_cls.objects.filter(city_id__in=city_ids).select_related('owner', 'listing_category').order_by('-created_at')[:5]
        for obj in qs:
            recent_items.append({'model_key': key, 'obj': obj})
    recent_items.sort(key=lambda item: item['obj'].created_at, reverse=True)

    context = {
        'page_title': 'City Admin Dashboard - OneTownCity',
        'stats': stats,
        'managed_cities': Location.objects.filter(id__in=city_ids).order_by('name'),
        'recent_items': recent_items[:8],
        'active_nav': 'overview',
    }
    return render(request, 'dashboard/city_admin_dashboard.html', context)


def _sub_admin_dashboard(request):
    """
    Sub Admin's Overview — same city-scoped shape as _city_admin_dashboard,
    but every stat/module card only appears once this specific Sub Admin has
    actually been granted the matching permission (has_content_permission
    for each listing type, view_content_providers for that section). This
    is a display concern only, not the enforcement itself: every URL a card
    here links to is independently permission-checked at the view
    (content_review_required, posts_dashboard_required,
    content_providers_required, has_content_permission()/has_permission()
    inside them), so a Sub Admin can't reach anything they weren't granted
    just by guessing a URL — hiding the card is a courtesy, not the guard.
    """
    profile = request.profile
    city_ids = profile.managed_city_ids()
    visible_models = {key: model_cls for key, model_cls in LISTING_MODELS.items() if _sub_admin_can_view_content(profile, key)}

    listing_status_totals = [
        m.objects.filter(city_id__in=city_ids).aggregate(
            pending=Count('id', filter=Q(status=ListingStatus.PENDING)),
            approved=Count('id', filter=Q(status=ListingStatus.APPROVED)),
            rejected=Count('id', filter=Q(status=ListingStatus.REJECTED)),
            total=Count('id'),
        )
        for m in visible_models.values()
    ]
    stats = {
        'total_listings': sum(t['total'] for t in listing_status_totals),
        'pending_listings': sum(t['pending'] for t in listing_status_totals),
        'approved_listings': sum(t['approved'] for t in listing_status_totals),
        'rejected_listings': sum(t['rejected'] for t in listing_status_totals),
    }
    if profile.has_permission('view_content_providers'):
        stats['total_content_providers'] = Profile.objects.filter(
            role=UserRole.ADMIN, user__city_permissions__city_id__in=city_ids
        ).distinct().count()

    per_model_counts = [
        {
            'model_key': key, 'label': str(model_cls._meta.verbose_name_plural).title(),
            'count': model_cls.objects.filter(city_id__in=city_ids).count(),
        }
        for key, model_cls in visible_models.items()
    ]

    recent_items = []
    for key, model_cls in visible_models.items():
        qs = model_cls.objects.filter(city_id__in=city_ids).select_related('owner', 'listing_category').order_by('-created_at')[:5]
        for obj in qs:
            recent_items.append({'model_key': key, 'obj': obj})
    recent_items.sort(key=lambda item: item['obj'].created_at, reverse=True)

    context = {
        'page_title': 'Sub Admin Dashboard - OneTownCity',
        'stats': stats,
        'per_model_counts': per_model_counts,
        'managed_cities': Location.objects.filter(id__in=city_ids).order_by('name'),
        'recent_items': recent_items[:8],
        'can_access_content': profile.has_any_content_access(),
        'can_manage_content_providers': profile.has_permission('view_content_providers'),
        'active_nav': 'overview',
    }
    return render(request, 'dashboard/sub_admin_dashboard.html', context)


def _filtered_profiles(request):
    """Shared query-param filtering for the Users dashboard and its Excel/PDF exports."""
    query = request.GET.get('q', '').strip()
    profiles = Profile.objects.select_related('user').order_by('-created_at')
    if query:
        profiles = profiles.filter(
            Q(full_name__icontains=query) | Q(user__email__icontains=query) | Q(user__username__icontains=query)
        )
    role_filter = request.GET.get('role', '').strip()
    if role_filter:
        profiles = profiles.filter(role=role_filter)
    return profiles, query, role_filter


#: Drives the page heading/sidebar-highlight for the Sub Admins and Content
#: Providers sidebar links, which both just deep-link into this same
#: role-filtered Manage Users view rather than being separate pages.
_ROLE_FILTER_VIEW_META = {
    UserRole.SUB_ADMIN: ('sub_admins', 'Sub Admins', 'View, filter, and moderate every Sub Admin account.'),
    UserRole.ADMIN: ('content_providers', 'Content Providers', 'View, filter, and moderate every Content Provider account.'),
}


@super_admin_required
def dashboard_users(request):
    profiles, query, role_filter = _filtered_profiles(request)
    active_nav, heading, subheading = _ROLE_FILTER_VIEW_META.get(
        role_filter, ('users', 'Manage Users', 'View, filter, and moderate every registered account.')
    )

    context = {
        'page_title': f'{heading} - OneTownCity',
        'heading': heading,
        'subheading': subheading,
        'profiles': profiles,
        'query': query,
        'role_filter': role_filter,
        'role_choices': UserRole.choices,
        'active_nav': active_nav,
    }
    return render(request, 'dashboard/users.html', context)


@super_admin_required
def dashboard_user_detail(request, user_id):
    """
    Full read-only profile view for one account — Super Admin clicks a row
    in Manage Users to see everything the flat table can't show inline
    (contact/address details, granted category permissions, every listing
    they own, recent login history). Reuses the same per-owner listing scan
    _admin_dashboard already does for a Content Provider's own workspace,
    just pointed at the viewed user instead of request.user.
    """
    profile = get_object_or_404(Profile.objects.select_related('user'), user_id=user_id)
    target_user = profile.user

    own_items = []
    for key, model_cls in LISTING_MODELS.items():
        qs = model_cls.objects.filter(owner=target_user).select_related('listing_category')
        for obj in qs:
            own_items.append({'model_key': key, 'obj': obj})
    own_items.sort(key=lambda item: item['obj'].created_at, reverse=True)

    stats = {
        'total': len(own_items),
        'pending': sum(1 for i in own_items if i['obj'].status == ListingStatus.PENDING),
        'approved': sum(1 for i in own_items if i['obj'].status == ListingStatus.APPROVED),
        'rejected': sum(1 for i in own_items if i['obj'].status == ListingStatus.REJECTED),
        'total_views': sum(i['obj'].view_count for i in own_items),
        'total_likes': sum(i['obj'].like_count for i in own_items),
    }

    granted_permissions = None
    if profile.role == UserRole.SUB_ADMIN:
        role_defaults = {
            rp.permission_id: rp.is_granted
            for rp in RolePermission.objects.filter(role=UserRole.SUB_ADMIN, permission__key__in=SUB_ADMIN_DELEGABLE_PERMISSIONS)
        }
        overrides = {
            up.permission_id: up.is_granted
            for up in UserPermission.objects.filter(user=target_user, permission__key__in=SUB_ADMIN_DELEGABLE_PERMISSIONS)
        }
        granted_permissions = [
            p for p in Permission.objects.filter(key__in=SUB_ADMIN_DELEGABLE_PERMISSIONS)
            if overrides.get(p.id, role_defaults.get(p.id, False))
        ]

    context = {
        'page_title': f'{profile.full_name or target_user.username} - OneTownCity',
        'viewed_profile': profile,
        'stats': stats,
        'items': own_items,
        'permitted_categories': Category.objects.filter(is_active=True, admin_permissions__admin=target_user) if profile.role == UserRole.ADMIN else None,
        'granted_permissions': granted_permissions,
        'login_history': LoginHistory.objects.filter(user=target_user)[:10],
        'active_nav': 'users',
    }
    return render(request, 'dashboard/user_detail.html', context)


@super_admin_required
@require_POST
def dashboard_user_toggle_block(request, user_id):
    profile = get_object_or_404(Profile, user_id=user_id)
    profile.is_blocked = not profile.is_blocked
    profile.save(update_fields=['is_blocked'])
    log_audit(request, 'user.toggle_block', f'{profile.user.email} is now {"blocked" if profile.is_blocked else "unblocked"}')
    messages.success(request, f'{profile.user.email} is now {"blocked" if profile.is_blocked else "unblocked"}.')
    return redirect('core:dashboard_users')


@super_admin_required
@require_POST
def dashboard_user_toggle_suspend(request, user_id):
    profile = get_object_or_404(Profile, user_id=user_id)
    profile.is_suspended = not profile.is_suspended
    profile.save(update_fields=['is_suspended'])
    log_audit(request, 'user.toggle_suspend', f'{profile.user.email} is now {"suspended" if profile.is_suspended else "unsuspended"}')
    messages.success(request, f'{profile.user.email} is now {"suspended" if profile.is_suspended else "unsuspended"}.')
    return redirect('core:dashboard_users')


@super_admin_required
@require_POST
def dashboard_user_promote_super_admin(request, user_id):
    """
    Lets an existing Super Admin promote another already-registered user to
    Super Admin, mirroring the create_super_admin management command (same
    role/is_staff/is_superuser fields) but reachable from Manage Users
    instead of the terminal. The target must have signed in with Google at
    least once already, since that's what creates their Profile row.
    """
    profile = get_object_or_404(Profile, user_id=user_id)
    if profile.role == UserRole.SUPER_ADMIN:
        messages.info(request, f'{profile.user.email} is already a Super Admin.')
        return redirect('core:dashboard_users')

    profile.role = UserRole.SUPER_ADMIN
    profile.is_blocked = False
    profile.is_suspended = False
    profile.save(update_fields=['role', 'is_blocked', 'is_suspended'])

    profile.user.is_staff = True
    profile.user.is_superuser = True
    profile.user.save(update_fields=['is_staff', 'is_superuser'])

    log_audit(request, 'user.promote_super_admin', f'{profile.user.email} promoted to Super Admin')
    messages.success(request, f'{profile.user.email} is now a Super Admin.')
    return redirect(_safe_next(request, reverse('core:dashboard_users')))


@super_admin_required
@require_POST
def dashboard_users_bulk_delete(request):
    """
    Bulk-deletes selected user accounts. Super Admin accounts (including the
    acting user's own) are always excluded from the selection as a safety
    guard — deleting the account you're using, or another Super Admin,
    should never be a one-click bulk action. Their listings survive with
    owner=None (ListingMixin.owner is on_delete=SET_NULL), matching the
    existing single-user-removal behavior elsewhere.
    """
    raw_ids = request.POST.getlist('items')
    requested_ids = {int(pk) for pk in raw_ids if pk.isdigit()}

    deletable_ids = set(
        Profile.objects.filter(user_id__in=requested_ids)
        .exclude(role=UserRole.SUPER_ADMIN)
        .exclude(user_id=request.user.id)
        .values_list('user_id', flat=True)
    )
    skipped = len(requested_ids) - len(deletable_ids)

    if deletable_ids:
        emails = list(User.objects.filter(id__in=deletable_ids).values_list('email', flat=True))
        User.objects.filter(id__in=deletable_ids).delete()
        log_audit(request, 'user.bulk_delete', f'Deleted {len(deletable_ids)} user(s): {", ".join(emails)}')
        messages.success(request, f'{len(deletable_ids)} user(s) deleted.')
    if skipped:
        messages.warning(request, f'{skipped} user(s) were skipped (Super Admin accounts and your own account can\'t be bulk-deleted).')
    if not deletable_ids and not skipped:
        messages.error(request, 'No users selected.')
    return redirect(_safe_next(request, reverse('core:dashboard_users')))


@super_admin_required
def dashboard_users_export_excel(request):
    profiles, _, _ = _filtered_profiles(request)
    workbook = build_users_workbook(profiles)
    response = HttpResponse(content_type='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
    response['Content-Disposition'] = 'attachment; filename="onetowncity_users.xlsx"'
    response.write(workbook.getvalue())
    return response


@super_admin_required
def dashboard_users_export_pdf(request):
    profiles, _, _ = _filtered_profiles(request)
    pdf = build_users_pdf(profiles)
    response = HttpResponse(pdf.getvalue(), content_type='application/pdf')
    response['Content-Disposition'] = 'attachment; filename="onetowncity_users.pdf"'
    return response


@super_admin_required
def dashboard_messages(request):
    """
    Every submission from the public Contact Us form, newest first —
    Super Admin only. Read/unread mirrors an inbox; nothing here is
    ever shown to the public.
    """
    query = request.GET.get('q', '').strip()
    contact_messages = ContactMessage.objects.all()
    if query:
        contact_messages = contact_messages.filter(
            Q(name__icontains=query) | Q(email__icontains=query) | Q(subject__icontains=query) | Q(message__icontains=query)
        )

    status_filter = request.GET.get('status', '').strip()
    if status_filter == 'unread':
        contact_messages = contact_messages.filter(is_read=False)
    elif status_filter == 'read':
        contact_messages = contact_messages.filter(is_read=True)

    paginator = Paginator(contact_messages, 20)
    page_obj = paginator.get_page(request.GET.get('page'))

    context = {
        'page_title': 'Contact Messages - OneTownCity',
        'page_obj': page_obj,
        'query': query,
        'status_filter': status_filter,
        'unread_count': ContactMessage.objects.filter(is_read=False).count(),
        'active_nav': 'messages',
    }
    return render(request, 'dashboard/messages.html', context)


@super_admin_required
@require_POST
def dashboard_message_toggle_read(request, pk):
    message_obj = get_object_or_404(ContactMessage, pk=pk)
    message_obj.is_read = not message_obj.is_read
    message_obj.save(update_fields=['is_read'])
    return redirect(_safe_next(request, reverse('core:dashboard_messages')))


@super_admin_required
def dashboard_admin_requests(request):
    status_filter = request.GET.get('status', '').strip()
    requests_qs = AdminRequest.objects.select_related('user__profile').prefetch_related('categories').order_by('-created_at')
    if status_filter:
        requests_qs = requests_qs.filter(status=status_filter)

    context = {
        'page_title': 'Admin Requests - OneTownCity',
        'requests': requests_qs,
        'status_filter': status_filter,
        'status_choices': AdminRequestStatus.choices,
        'active_nav': 'requests',
    }
    return render(request, 'dashboard/admin_requests.html', context)


@super_admin_required
def dashboard_admin_request_detail(request, pk):
    admin_request = get_object_or_404(
        AdminRequest.objects.select_related('user__profile').prefetch_related('categories'), pk=pk
    )
    if request.method == 'POST':
        form = AdminRequestReviewForm(request.POST)
        if form.is_valid():
            action = form.cleaned_data['action']
            note = form.cleaned_data['note']
            admin_request.review_note = note
            admin_request.reviewed_by = request.user
            admin_request.reviewed_at = timezone.now()
            applicant_profile = admin_request.user.profile

            if action == 'approve':
                admin_request.status = AdminRequestStatus.APPROVED
                for category in admin_request.categories.all():
                    AdminCategoryPermission.objects.get_or_create(
                        admin=admin_request.user, category=category, defaults={'granted_by': request.user}
                    )
                applicant_profile.role = UserRole.ADMIN
                applicant_profile.save(update_fields=['role'])
                notify(
                    admin_request.user, 'admin_request_approved',
                    'Your request to become a Content Provider has been approved!',
                    url=reverse('core:my_listings'),
                )
                log_audit(request, 'admin_request.approve', f'Approved Content Provider request from {admin_request.user.email}')
            elif action == 'reject':
                admin_request.status = AdminRequestStatus.REJECTED
                notify(
                    admin_request.user, 'admin_request_rejected',
                    f'Your request was rejected: {note}' if note else 'Your request was rejected.',
                    url=reverse('core:admin_request_pending'),
                )
                log_audit(request, 'admin_request.reject', f'Rejected Content Provider request from {admin_request.user.email}')
            else:
                admin_request.status = AdminRequestStatus.CHANGES_REQUESTED
                notify(
                    admin_request.user, 'admin_request_changes_requested',
                    f'Changes requested on your request: {note}' if note else 'Changes were requested on your request.',
                    url=reverse('core:admin_request_pending'),
                )
                log_audit(request, 'admin_request.changes_requested', f'Requested changes on request from {admin_request.user.email}')
            admin_request.save()
            messages.success(request, 'Request updated.')
            return redirect('core:dashboard_admin_requests')
    else:
        form = AdminRequestReviewForm()

    context = {
        'page_title': 'Review Admin Request - OneTownCity',
        'admin_request': admin_request,
        'form': form,
        'active_nav': 'requests',
    }
    return render(request, 'dashboard/admin_request_detail.html', context)


@super_admin_required
def dashboard_categories(request):
    if request.method == 'POST':
        action = request.POST.get('action', 'toggle')

        if action == 'toggle':
            category = get_object_or_404(Category, pk=request.POST.get('category_id'))
            category.is_active = not category.is_active
            category.save(update_fields=['is_active'])
            log_audit(request, 'category.toggle', f'"{category.label}" is now {"active" if category.is_active else "inactive"}')
            messages.success(request, f'{category.label} is now {"active" if category.is_active else "inactive"}.')

        elif action == 'create':
            parent_id = request.POST.get('parent') or None
            parent = get_object_or_404(Category, pk=parent_id) if parent_id else None
            form = CategoryForm(request.POST, parent=parent)
            if form.is_valid():
                category = form.save(commit=False)
                if not category.key:
                    category.key = unique_slug_for(Category, category.label, field_name='key', max_length=50)
                category.save()
                log_audit(request, 'category.create', f'"{category.label}" was added')
                messages.success(request, f'"{category.label}" was added.')
            else:
                messages.error(request, 'Could not add category: ' + ' '.join(
                    f'{f}: {", ".join(e)}' for f, e in form.errors.items()
                ))

        elif action == 'edit':
            category = get_object_or_404(Category, pk=request.POST.get('category_id'))
            form = CategoryForm(request.POST, instance=category, parent=category.parent)
            if form.is_valid():
                form.save()
                log_audit(request, 'category.edit', f'"{category.label}" was updated')
                messages.success(request, f'"{category.label}" was updated.')
            else:
                messages.error(request, 'Could not update category: ' + ' '.join(
                    f'{f}: {", ".join(e)}' for f, e in form.errors.items()
                ))

        elif action == 'delete':
            category = get_object_or_404(Category, pk=request.POST.get('category_id'))
            label = category.label
            try:
                category.delete()
                log_audit(request, 'category.delete', f'"{label}" was deleted')
                messages.success(request, f'"{label}" was deleted.')
            except ProtectedError:
                messages.error(
                    request,
                    f'"{label}" is used by existing listings and can\'t be deleted — deactivate it instead.'
                )

        return redirect('core:dashboard_categories')

    top_categories = Category.objects.filter(parent=None).prefetch_related('children').order_by('order', 'label')
    return render(request, 'dashboard/categories.html', {
        'page_title': 'Manage Categories - OneTownCity',
        'top_categories': top_categories,
        'listing_model_choices': Category.LISTING_MODEL_CHOICES,
        'active_nav': 'categories',
    })


# ===========================================================================
# Super Admin: platform administration (City Admins, roles & permissions,
# platform modules, platform settings, audit log)
# ===========================================================================

@super_admin_required
def dashboard_city_admins(request):
    """
    Manage City Admin accounts. 'Create' pre-provisions a User+Profile by
    email before that person has ever signed in — auth_callback_api (see
    the Google sign-in flow above) already falls back to matching an
    existing User by email on first login, so their next Google sign-in
    attaches to this account automatically. 'Edit' updates name/city scope
    for an existing City Admin, and 'toggle_active' reuses is_blocked, the
    same deactivation flag every other account uses.
    """
    if request.method == 'POST':
        action = request.POST.get('action', 'create')

        if action in ('create', 'edit'):
            form = CityAdminForm(request.POST)
            if form.is_valid():
                email = form.cleaned_data['email'].strip().lower()
                full_name = form.cleaned_data['full_name'].strip()
                cities = form.cleaned_data['cities']

                if action == 'edit':
                    profile = get_object_or_404(Profile, user_id=request.POST.get('user_id'), role=UserRole.CITY_ADMIN)
                    user = profile.user
                else:
                    user = User.objects.filter(email=email).first()
                    if user is None:
                        user = User.objects.create(username=_unique_username(email), email=email)
                    profile, _ = Profile.objects.get_or_create(user=user)
                    if profile.role == UserRole.SUPER_ADMIN:
                        messages.error(request, f'{email} is already a Super Admin and can\'t be reassigned here.')
                        return redirect('core:dashboard_city_admins')
                    profile.role = UserRole.CITY_ADMIN

                profile.full_name = full_name or profile.full_name
                profile.save()

                AdminCityPermission.objects.filter(admin=user).exclude(city__in=cities).delete()
                for city in cities:
                    AdminCityPermission.objects.get_or_create(admin=user, city=city, defaults={'granted_by': request.user})

                city_names = ', '.join(c.name for c in cities) or 'no cities assigned'
                log_audit(
                    request, 'city_admin.create' if action == 'create' else 'city_admin.edit',
                    f'{"Created" if action == "create" else "Updated"} City Admin {user.email} ({city_names})',
                )
                messages.success(request, f'{user.email} is now a City Admin.' if action == 'create' else f'{user.email} was updated.')
            else:
                messages.error(request, 'Could not save City Admin: ' + ' '.join(
                    f'{f}: {", ".join(e)}' for f, e in form.errors.items()
                ))

        elif action == 'toggle_active':
            profile = get_object_or_404(Profile, user_id=request.POST.get('user_id'), role=UserRole.CITY_ADMIN)
            profile.is_blocked = not profile.is_blocked
            profile.save(update_fields=['is_blocked'])
            log_audit(request, 'city_admin.toggle_active', f'{profile.user.email} is now {"deactivated" if profile.is_blocked else "active"}')
            messages.success(request, f'{profile.user.email} is now {"deactivated" if profile.is_blocked else "active"}.')

        return redirect('core:dashboard_city_admins')

    city_admins = (
        Profile.objects.filter(role=UserRole.CITY_ADMIN)
        .select_related('user')
        .prefetch_related('user__city_permissions__city')
        .order_by('-created_at')
    )
    return render(request, 'dashboard/city_admins.html', {
        'page_title': 'Manage City Admins - OneTownCity',
        'city_admins': city_admins,
        'cities': Location.objects.filter(kind=Location.Kind.CITY, is_active=True).order_by('name'),
        'active_nav': 'city_admins',
    })


@super_admin_required
def dashboard_roles_permissions(request):
    """
    Role x Permission matrix. Super Admin isn't shown as an editable row —
    its access is unconditional in Profile.has_permission(), never driven by
    this data. Toggling a cell here doesn't change what any *existing* view
    enforces yet (today only has_permission() reads it); it becomes
    load-bearing once City Admin/Sub Admin/Content Provider get their own
    permission-gated views in later RBAC stages.
    """
    if request.method == 'POST':
        role = request.POST.get('role')
        valid_roles = {choice[0] for choice in RolePermission.ROLE_CHOICES}
        if role in valid_roles:
            permission = get_object_or_404(Permission, pk=request.POST.get('permission_id'))
            row, _ = RolePermission.objects.get_or_create(role=role, permission=permission)
            row.is_granted = not row.is_granted
            row.updated_by = request.user
            row.save(update_fields=['is_granted', 'updated_by', 'updated_at'])
            role_label = dict(RolePermission.ROLE_CHOICES).get(role, role)
            log_audit(
                request, 'permission.update',
                f'{role_label}: {permission.label} {"granted" if row.is_granted else "revoked"}',
            )
        return redirect('core:dashboard_roles_permissions')

    roles = RolePermission.ROLE_CHOICES
    grants = {(row.role, row.permission_id): row.is_granted for row in RolePermission.objects.all()}
    matrix = [
        {
            'permission': permission,
            'cells': [
                {'role': role, 'granted': grants.get((role, permission.id), False)}
                for role, _label in roles
            ],
        }
        for permission in Permission.objects.all()
    ]
    return render(request, 'dashboard/roles_permissions.html', {
        'page_title': 'Roles & Permissions - OneTownCity',
        'roles': roles,
        'matrix': matrix,
        'active_nav': 'roles_permissions',
    })


@super_admin_required
def dashboard_platform_modules(request):
    if request.method == 'POST':
        module = get_object_or_404(PlatformModule, pk=request.POST.get('module_id'))
        module.is_enabled = not module.is_enabled
        module.updated_by = request.user
        module.save(update_fields=['is_enabled', 'updated_by', 'updated_at'])
        log_audit(request, 'module.toggle', f'{module.label} is now {"enabled" if module.is_enabled else "disabled"}')
        messages.success(request, f'{module.label} is now {"enabled" if module.is_enabled else "disabled"}.')
        return redirect('core:dashboard_platform_modules')

    return render(request, 'dashboard/platform_modules.html', {
        'page_title': 'Platform Modules - OneTownCity',
        'modules': PlatformModule.objects.all(),
        'active_nav': 'platform_modules',
    })


@super_admin_required
def dashboard_platform_settings(request):
    settings_obj = PlatformSettings.load()
    if request.method == 'POST':
        form = PlatformSettingsForm(request.POST, instance=settings_obj)
        if form.is_valid():
            settings_obj = form.save(commit=False)
            settings_obj.updated_by = request.user
            settings_obj.save()
            log_audit(request, 'settings.update', 'Platform settings updated')
            messages.success(request, 'Platform settings updated.')
            return redirect('core:dashboard_platform_settings')
    else:
        form = PlatformSettingsForm(instance=settings_obj)

    return render(request, 'dashboard/platform_settings.html', {
        'page_title': 'Platform Settings - OneTownCity',
        'form': form,
        'active_nav': 'platform_settings',
    })


@super_admin_required
def dashboard_audit_logs(request):
    logs = AuditLog.objects.select_related('actor')
    query = request.GET.get('q', '').strip()
    if query:
        logs = logs.filter(
            Q(action__icontains=query) | Q(description__icontains=query) | Q(actor__email__icontains=query)
        )

    paginator = Paginator(logs, 50)
    page_obj = paginator.get_page(request.GET.get('page'))

    return render(request, 'dashboard/audit_logs.html', {
        'page_title': 'Audit Logs - OneTownCity',
        'page_obj': page_obj,
        'query': query,
        'active_nav': 'audit_logs',
    })


# ===========================================================================
# City Admin: Sub Admins, Content Providers, City Modules
# ===========================================================================

def _get_city_scoped_profile(role, user_id, managed_city_ids):
    """
    404s unless the target Profile has `role` AND is assigned (via
    AdminCityPermission) to at least one city in managed_city_ids — used by
    every City-Admin user-management view so a City Admin can never address
    another city's Sub Admin/Content Provider by guessing a user_id, and
    can't touch a Super Admin/City Admin account through these views at all.
    """
    profile = get_object_or_404(Profile, user_id=user_id, role=role)
    if not AdminCityPermission.objects.filter(admin_id=user_id, city_id__in=managed_city_ids).exists():
        raise Http404('Not found.')
    return profile


@city_admin_or_super_required
def dashboard_sub_admins(request):
    """
    Manage Sub Admin accounts within the acting City Admin's own city/cities
    (Super Admin can reach this too — managed_city_ids() returns every city
    for Super Admin, so the scoping below is a no-op for them). Same
    pre-provision-by-email pattern as dashboard_city_admins (Stage 1).
    """
    managed_cities = Location.objects.filter(id__in=request.profile.managed_city_ids())
    managed_city_ids = list(managed_cities.values_list('id', flat=True))

    if request.method == 'POST':
        action = request.POST.get('action', 'create')

        if action in ('create', 'edit'):
            form = SubAdminForm(request.POST, cities_qs=managed_cities)
            if form.is_valid():
                email = form.cleaned_data['email'].strip().lower()
                full_name = form.cleaned_data['full_name'].strip()
                city = form.cleaned_data['city']

                if action == 'edit':
                    profile = _get_city_scoped_profile(UserRole.SUB_ADMIN, request.POST.get('user_id'), managed_city_ids)
                    user = profile.user
                else:
                    user = User.objects.filter(email=email).first()
                    if user is None:
                        user = User.objects.create(username=_unique_username(email), email=email)
                    profile, _ = Profile.objects.get_or_create(user=user)
                    if profile.role in (UserRole.SUPER_ADMIN, UserRole.CITY_ADMIN):
                        messages.error(request, f'{email} is already a {profile.get_role_display()} and can\'t be reassigned here.')
                        return redirect('core:dashboard_sub_admins')
                    profile.role = UserRole.SUB_ADMIN

                profile.full_name = full_name or profile.full_name
                profile.save()

                AdminCityPermission.objects.filter(admin=user).exclude(city=city).delete()
                AdminCityPermission.objects.get_or_create(admin=user, city=city, defaults={'granted_by': request.user})

                log_audit(
                    request, 'sub_admin.create' if action == 'create' else 'sub_admin.edit',
                    f'{"Created" if action == "create" else "Updated"} Sub Admin {user.email} ({city.name})',
                )
                messages.success(request, f'{user.email} is now a Sub Admin.' if action == 'create' else f'{user.email} was updated.')
            else:
                messages.error(request, 'Could not save Sub Admin: ' + ' '.join(
                    f'{f}: {", ".join(e)}' for f, e in form.errors.items()
                ))

        elif action == 'toggle_active':
            profile = _get_city_scoped_profile(UserRole.SUB_ADMIN, request.POST.get('user_id'), managed_city_ids)
            profile.is_blocked = not profile.is_blocked
            profile.save(update_fields=['is_blocked'])
            log_audit(request, 'sub_admin.toggle_active', f'{profile.user.email} is now {"deactivated" if profile.is_blocked else "active"}')
            messages.success(request, f'{profile.user.email} is now {"deactivated" if profile.is_blocked else "active"}.')

        return redirect('core:dashboard_sub_admins')

    sub_admins = (
        Profile.objects.filter(role=UserRole.SUB_ADMIN, user__city_permissions__city_id__in=managed_city_ids)
        .select_related('user')
        .prefetch_related('user__city_permissions__city')
        .distinct()
        .order_by('-created_at')
    )
    return render(request, 'dashboard/sub_admins.html', {
        'page_title': 'Manage Sub Admins - OneTownCity',
        'sub_admins': sub_admins,
        'cities': managed_cities,
        'active_nav': 'sub_admins',
    })


#: The permission subset a City Admin may delegate to an individual Sub
#: Admin. manage_sub_admins/manage_city_modules stay City-Admin-exclusive —
#: delegating "manage other sub admins" to a Sub Admin wasn't asked for and
#: would be recursive. Grouped (see Permission.group) to match the Sub Admin
#: Permissions screen's sections; GROUP_DISPLAY_ORDER below controls the
#: order those sections render in.
SUB_ADMIN_DELEGABLE_PERMISSIONS = [
    'view_dashboard', 'view_city_analytics',
    'view_content', 'add_content', 'edit_content', 'delete_content',
    'review_content', 'approve_content', 'reject_content', 'request_content_changes', 'manage_city_content',
    'view_content_providers', 'add_content_provider', 'edit_content_provider', 'toggle_content_provider',
    'view_businesses', 'add_businesses', 'edit_businesses', 'delete_businesses', 'approve_businesses',
    'view_events', 'add_events', 'edit_events', 'delete_events', 'approve_events',
    'view_announcements', 'add_announcements', 'edit_announcements', 'delete_announcements',
    'view_categories', 'add_categories', 'edit_categories', 'delete_categories',
    'view_users',
    'view_reports', 'export_reports',
]

GROUP_DISPLAY_ORDER = [
    'Dashboard', 'Content', 'Content Providers', 'Businesses', 'Events',
    'Announcements', 'Categories', 'Users', 'Reports & Analytics',
]


@city_admin_or_super_required
def dashboard_sub_admin_permissions(request, user_id):
    """
    Assign/remove specific permissions for one Sub Admin — a per-user
    override (UserPermission) layered on top of the Sub Admin role-level
    default (which stays False for everyone; only City Admin gets defaults
    turned on). A City Admin can never reach this for their own account —
    role_required already keeps a City Admin's own role out of
    UserRole.SUB_ADMIN, but the explicit check below is defense in depth.
    """
    if str(user_id) == str(request.user.id):
        messages.error(request, "You can't manage your own permissions.")
        return redirect('core:dashboard_sub_admins')

    managed_city_ids = request.profile.managed_city_ids()
    sub_admin = _get_city_scoped_profile(UserRole.SUB_ADMIN, user_id, managed_city_ids)

    if request.method == 'POST':
        permission = get_object_or_404(Permission, pk=request.POST.get('permission_id'), key__in=SUB_ADMIN_DELEGABLE_PERMISSIONS)
        row, _ = UserPermission.objects.get_or_create(
            user=sub_admin.user, permission=permission, defaults={'is_granted': False, 'granted_by': request.user},
        )
        row.is_granted = not row.is_granted
        row.granted_by = request.user
        row.save(update_fields=['is_granted', 'granted_by'])
        log_audit(
            request, 'sub_admin.permission_update',
            f'{sub_admin.user.email}: {permission.label} {"granted" if row.is_granted else "revoked"}',
        )
        return redirect('core:dashboard_sub_admin_permissions', user_id=user_id)

    role_defaults = {
        rp.permission_id: rp.is_granted
        for rp in RolePermission.objects.filter(role=UserRole.SUB_ADMIN, permission__key__in=SUB_ADMIN_DELEGABLE_PERMISSIONS)
    }
    overrides = {
        up.permission_id: up.is_granted
        for up in UserPermission.objects.filter(user=sub_admin.user, permission__key__in=SUB_ADMIN_DELEGABLE_PERMISSIONS)
    }
    rows_by_group = {}
    for permission in Permission.objects.filter(key__in=SUB_ADMIN_DELEGABLE_PERMISSIONS):
        rows_by_group.setdefault(permission.group, []).append({
            'permission': permission,
            'granted': overrides.get(permission.id, role_defaults.get(permission.id, False)),
            'is_override': permission.id in overrides,
        })
    groups = [
        {'name': group, 'rows': rows_by_group[group]}
        for group in GROUP_DISPLAY_ORDER if group in rows_by_group
    ]

    return render(request, 'dashboard/sub_admin_permissions.html', {
        'page_title': f'{sub_admin.full_name or sub_admin.user.email} Permissions - OneTownCity',
        'sub_admin': sub_admin,
        'groups': groups,
        'active_nav': 'sub_admins',
    })


#: Which delegated Content-Providers permission a Sub Admin needs for each
#: POST action here (Super Admin/City Admin bypass this entirely).
CONTENT_PROVIDER_ACTION_PERMISSIONS = {
    'create': 'add_content_provider', 'edit': 'edit_content_provider', 'toggle_active': 'toggle_content_provider',
}


@content_providers_required
def dashboard_content_providers(request):
    """
    Manage Content Provider accounts within the acting City Admin's own
    city/cities. Same pre-provision-by-email pattern as dashboard_sub_admins,
    plus a category grant — writes the same AdminCategoryPermission an Admin
    Request approval creates (dashboard_admin_request_detail), just
    City-Admin-initiated instead of Super-Admin-approved. A Sub Admin needs
    view_content_providers just to reach this page (content_providers_
    required), plus the specific add/edit/toggle permission below for each
    action they attempt.
    """
    acting_profile = request.profile
    managed_cities = Location.objects.filter(id__in=acting_profile.managed_city_ids())
    managed_city_ids = list(managed_cities.values_list('id', flat=True))

    if request.method == 'POST':
        action = request.POST.get('action', 'create')

        if (
            acting_profile.is_sub_admin
            and not acting_profile.has_permission(CONTENT_PROVIDER_ACTION_PERMISSIONS.get(action, ''))
        ):
            messages.error(request, 'You do not have permission to do that.')
            return redirect('core:dashboard_content_providers')

        if action in ('create', 'edit'):
            form = ContentProviderForm(request.POST, cities_qs=managed_cities)
            if form.is_valid():
                email = form.cleaned_data['email'].strip().lower()
                full_name = form.cleaned_data['full_name'].strip()
                city = form.cleaned_data['city']
                categories = form.cleaned_data['categories']

                if action == 'edit':
                    profile = _get_city_scoped_profile(UserRole.ADMIN, request.POST.get('user_id'), managed_city_ids)
                    user = profile.user
                else:
                    user = User.objects.filter(email=email).first()
                    if user is None:
                        user = User.objects.create(username=_unique_username(email), email=email)
                    profile, _ = Profile.objects.get_or_create(user=user)
                    if profile.role in (UserRole.SUPER_ADMIN, UserRole.CITY_ADMIN):
                        messages.error(request, f'{email} is already a {profile.get_role_display()} and can\'t be reassigned here.')
                        return redirect('core:dashboard_content_providers')
                    profile.role = UserRole.ADMIN

                profile.full_name = full_name or profile.full_name
                profile.save()

                AdminCityPermission.objects.filter(admin=user).exclude(city=city).delete()
                AdminCityPermission.objects.get_or_create(admin=user, city=city, defaults={'granted_by': request.user})

                AdminCategoryPermission.objects.filter(admin=user).exclude(category__in=categories).delete()
                for category in categories:
                    AdminCategoryPermission.objects.get_or_create(admin=user, category=category, defaults={'granted_by': request.user})

                log_audit(
                    request, 'content_provider.create' if action == 'create' else 'content_provider.edit',
                    f'{"Created" if action == "create" else "Updated"} Content Provider {user.email} ({city.name})',
                )
                messages.success(request, f'{user.email} is now a Content Provider.' if action == 'create' else f'{user.email} was updated.')
            else:
                messages.error(request, 'Could not save Content Provider: ' + ' '.join(
                    f'{f}: {", ".join(e)}' for f, e in form.errors.items()
                ))

        elif action == 'toggle_active':
            profile = _get_city_scoped_profile(UserRole.ADMIN, request.POST.get('user_id'), managed_city_ids)
            profile.is_blocked = not profile.is_blocked
            profile.save(update_fields=['is_blocked'])
            log_audit(request, 'content_provider.toggle_active', f'{profile.user.email} is now {"deactivated" if profile.is_blocked else "active"}')
            messages.success(request, f'{profile.user.email} is now {"deactivated" if profile.is_blocked else "active"}.')

        return redirect('core:dashboard_content_providers')

    content_providers = (
        Profile.objects.filter(role=UserRole.ADMIN, user__city_permissions__city_id__in=managed_city_ids)
        .select_related('user')
        .prefetch_related('user__category_permissions__category', 'user__city_permissions__city')
        .distinct()
        .order_by('-created_at')
    )
    return render(request, 'dashboard/content_providers.html', {
        'page_title': 'Manage Content Providers - OneTownCity',
        'content_providers': content_providers,
        'cities': managed_cities,
        'top_categories': Category.objects.filter(parent=None, is_active=True).prefetch_related('children'),
        'active_nav': 'content_providers',
    })


@city_admin_or_super_required
def dashboard_city_modules(request):
    """
    A City Admin's per-city module restrictions — can only further restrict
    a module Super Admin has already enabled platform-wide (see
    CityModule.is_enabled_for_city). ?city= picks which of the acting
    profile's cities is being edited (defaults to the first one).
    """
    managed_cities = list(Location.objects.filter(id__in=request.profile.managed_city_ids()).order_by('name'))
    if not managed_cities:
        messages.error(request, 'You are not assigned to any city yet.')
        return redirect('core:dashboard')

    requested_city_id = request.GET.get('city') or request.POST.get('city')
    city = next((c for c in managed_cities if str(c.pk) == str(requested_city_id)), managed_cities[0])

    if request.method == 'POST':
        module = get_object_or_404(PlatformModule, pk=request.POST.get('module_id'), is_enabled=True)
        row, _ = CityModule.objects.get_or_create(city=city, module=module, defaults={'is_enabled': True})
        row.is_enabled = not row.is_enabled
        row.updated_by = request.user
        row.save(update_fields=['is_enabled', 'updated_by', 'updated_at'])
        log_audit(request, 'city_module.toggle', f'{city.name}: {module.label} is now {"enabled" if row.is_enabled else "disabled"}')
        messages.success(request, f'{module.label} is now {"enabled" if row.is_enabled else "disabled"} for {city.name}.')
        return redirect(f"{reverse('core:dashboard_city_modules')}?city={city.pk}")

    city_overrides = {cm.module_id: cm.is_enabled for cm in CityModule.objects.filter(city=city)}
    rows = [
        {'module': module, 'enabled': city_overrides.get(module.id, True)}
        for module in PlatformModule.objects.filter(is_enabled=True)
    ]

    return render(request, 'dashboard/city_modules.html', {
        'page_title': 'City Modules - OneTownCity',
        'city': city,
        'managed_cities': managed_cities,
        'rows': rows,
        'active_nav': 'city_modules',
    })


@content_review_required
def dashboard_pending_listings(request):
    status_filter = request.GET.get('status', '')
    items = []
    for key, model_cls in LISTING_MODELS.items():
        qs = _scope_listing_qs(request, model_cls.objects.select_related('owner', 'listing_category'), key)
        if status_filter:
            qs = qs.filter(status=status_filter)
        for obj in qs:
            items.append({'model_key': key, 'obj': obj})
    items.sort(key=lambda item: item['obj'].created_at, reverse=True)

    context = {
        'page_title': 'Listing Approvals - OneTownCity',
        'items': items,
        'status_filter': status_filter,
        'status_choices': MODERATOR_STATUS_CHOICES,
        'active_nav': 'listings',
    }
    return render(request, 'dashboard/pending_listings.html', context)


#: Audit-log action name + human label for each review action, used by
#: _apply_listing_review so every approve/reject/changes-requested — by a
#: Super Admin, City Admin, or Sub Admin alike — lands a row in Audit Logs.
_REVIEW_AUDIT_ACTIONS = {
    'approve': ('content.approve', 'approved'),
    'reject': ('content.reject', 'rejected'),
    'changes_requested': ('content.changes_requested', 'requested changes on'),
}


def _apply_listing_review(request, obj, action, note):
    """
    Shared approve/reject/request-changes logic used by both the single-item
    review action and the Posts dashboard's bulk-action endpoint. Mutates and
    saves `obj`, notifies its owner (if any), and records the action in
    Audit Logs — "Sub Admin approved content" / "... rejected content" etc.
    """
    obj.reviewed_by = request.user
    obj.reviewed_at = timezone.now()

    if action == 'approve':
        obj.status = ListingStatus.APPROVED
        obj.rejection_reason = ''
        notif_type = 'listing_approved'
        notif_message = f'Your listing "{obj}" has been approved and is now live.'
    elif action == 'reject':
        obj.status = ListingStatus.REJECTED
        obj.rejection_reason = note
        notif_type = 'listing_rejected'
        notif_message = f'Your listing "{obj}" was rejected: {note}' if note else f'Your listing "{obj}" was rejected.'
    else:
        obj.status = ListingStatus.CHANGES_REQUESTED
        obj.rejection_reason = note
        notif_type = 'listing_changes_requested'
        notif_message = f'Changes requested on "{obj}": {note}' if note else f'Changes requested on "{obj}".'

    obj.save()

    if obj.owner_id:
        notify(
            obj.owner, notif_type, notif_message,
            url=obj.get_absolute_url() if obj.status == ListingStatus.APPROVED else reverse('core:my_listings'),
        )

    audit_action, audit_verb = _REVIEW_AUDIT_ACTIONS.get(action, ('content.review', 'reviewed'))
    log_audit(request, audit_action, f'{request.profile.get_role_display()} {audit_verb} "{obj}"' + (f': {note}' if note else ''))


@content_review_required
@require_POST
def dashboard_listing_review(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    profile = request.profile
    action = request.POST.get('action')
    if profile.is_sub_admin and not profile.has_content_permission(action, model_key):
        messages.error(request, 'You do not have permission to do that.')
        return redirect(_safe_next(request, reverse('core:dashboard_pending_listings')))
    obj = get_object_or_404(_scope_listing_qs(request, model_cls.objects.all(), model_key), pk=pk)
    if obj.owner_id == request.user.id:
        messages.error(request, 'You cannot approve or reject your own content.')
        return redirect(_safe_next(request, reverse('core:dashboard_pending_listings')))
    _apply_listing_review(request, obj, action, request.POST.get('note', '').strip())

    messages.success(request, 'Listing status updated.')
    return redirect(_safe_next(request, reverse('core:dashboard_pending_listings')))


# ===========================================================================
# Super Admin — Posts (unified cross-model listing management)
# ===========================================================================

POST_SEARCH_FIELDS = {
    'business': ['name', 'address'],
    'property': ['title', 'location'],
    'job': ['job_title', 'company', 'location'],
    'event': ['title', 'location'],
    'news': ['title', 'content'],
}

POST_SORT_KEYS = {
    'newest': (lambda item: item['obj'].created_at, True),
    'oldest': (lambda item: item['obj'].created_at, False),
    'most_viewed': (lambda item: item['obj'].view_count, True),
    'most_liked': (lambda item: item['obj'].like_count, True),
}


@super_admin_required
def dashboard_post_create_picker(request):
    """Category picker for the Super Admin's 'draft a single post' shortcut.
    Categories whose listing type module is disabled (Manage Platform
    Modules) are excluded."""
    disabled_modules = set(PlatformModule.objects.filter(is_enabled=False).values_list('key', flat=True))
    categories = Category.objects.filter(is_active=True).exclude(listing_model__in=disabled_modules)
    return render(request, 'dashboard/post_create_picker.html', {
        'page_title': 'New Post - OneTownCity',
        'categories': categories,
        'active_nav': 'posts',
    })


@super_admin_required
def dashboard_post_create(request, category_key):
    """
    Lets the Super Admin draft and publish a single standalone post directly
    (no approval queue — they authored it themselves), reusing the same
    per-model forms as the owner-facing listing_submit wizard.
    """
    category = get_object_or_404(Category, key=category_key, is_active=True)
    form_cls = LISTING_SUBMIT_FORMS[category.listing_model]

    if request.method == 'POST':
        form = form_cls(request.POST, request.FILES)
        if form.is_valid():
            obj = form.save(commit=False)
            obj.owner = request.user
            obj.listing_category = category
            obj.status = ListingStatus.APPROVED
            obj.reviewed_by = request.user
            obj.reviewed_at = timezone.now()
            obj.save()
            messages.success(request, f'"{obj}" was published.')
            return redirect('core:dashboard_posts')
    else:
        initial = {}
        if category.listing_model == 'business' and category.business_subcategory:
            initial['category'] = category.business_subcategory
        form = form_cls(initial=initial)

    return render(request, 'dashboard/post_create.html', {
        'page_title': f'New {category.label} - OneTownCity',
        'form': form,
        'category': category,
        'active_nav': 'posts',
    })


def _filtered_post_items(request):
    """
    Shared query-param filtering/sorting for the Posts dashboard, reused by
    dashboard_posts (paginated HTML table) and the Excel/PDF export views so
    an export always matches whatever the viewer is currently allowed to see.

    Visibility scoping (see _scope_listing_qs): Super Admin sees every
    listing; City Admin/permitted Sub Admin see their city/cities; Content
    Providers see only what they own.
    """
    q = request.GET.get('q', '').strip()
    category_id = request.GET.get('category', '').strip()
    model_filter = request.GET.get('model', '').strip()
    owner_id = request.GET.get('owner', '').strip()
    status_filter = request.GET.get('status', '').strip()
    sort = request.GET.get('sort', 'newest')

    items = []
    for key, model_cls in LISTING_MODELS.items():
        if model_filter and model_filter != key:
            continue
        qs = _scope_listing_qs(request, model_cls.objects.select_related('owner', 'listing_category'), key)
        if status_filter:
            qs = qs.filter(status=status_filter)
        if category_id:
            qs = qs.filter(listing_category_id=category_id)
        if owner_id:
            qs = qs.filter(owner_id=owner_id)
        if q:
            search_q = Q()
            for field in POST_SEARCH_FIELDS[key]:
                search_q |= Q(**{f'{field}__icontains': q})
            qs = qs.filter(search_q)
        for obj in qs:
            items.append({'model_key': key, 'obj': obj})

    sort_key, sort_reverse = POST_SORT_KEYS.get(sort, POST_SORT_KEYS['newest'])
    items.sort(key=sort_key, reverse=sort_reverse)
    return items, q, category_id, model_filter, owner_id, status_filter, sort


@posts_dashboard_required
def dashboard_posts(request):
    items, q, category_id, model_filter, owner_id, status_filter, sort = _filtered_post_items(request)

    owners = sorted(
        {item['obj'].owner for item in items if item['obj'].owner_id},
        key=lambda u: (u.get_full_name() or u.get_username()).lower(),
    )

    paginator = Paginator(items, 20)
    page_obj = paginator.get_page(request.GET.get('page'))

    counts = {'total': 0, 'pending': 0, 'approved': 0, 'rejected': 0}
    for model_key, model_cls in LISTING_MODELS.items():
        base_qs = _scope_listing_qs(request, model_cls.objects.all(), model_key)
        counts['total'] += base_qs.count()
        counts['pending'] += base_qs.filter(status=ListingStatus.PENDING).count()
        counts['approved'] += base_qs.filter(status=ListingStatus.APPROVED).count()
        counts['rejected'] += base_qs.filter(status=ListingStatus.REJECTED).count()

    context = {
        'page_title': 'Posts - OneTownCity',
        'page_obj': page_obj,
        'query': q,
        'selected_category': category_id,
        'selected_model': model_filter,
        'selected_owner': owner_id,
        'status_filter': status_filter,
        'sort': sort,
        'categories': Category.objects.filter(is_active=True),
        'model_choices': LISTING_MODELS.keys(),
        'owners': owners,
        'status_choices': MODERATOR_STATUS_CHOICES,
        'counts': counts,
        'active_nav': 'posts',
    }
    return render(request, 'dashboard/posts.html', context)


def _ensure_post_owner_access(request, obj):
    """
    IDOR guard: only Super Admin, City Admin (their own city), a permitted
    Sub Admin (same city scope), or the listing's own Content-Provider owner
    may view/act on it, even via a direct URL. Reuses _can_manage_post so
    this stays in lockstep with the same rule the Posts dashboard's buttons
    are already shown/hidden by.
    """
    if not _can_manage_post(request.profile, obj):
        raise Http404('Unknown listing type')


@posts_dashboard_required
def dashboard_post_detail(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls.objects.select_related('owner', 'listing_category', 'reviewed_by'), pk=pk)
    _ensure_post_owner_access(request, obj)
    ct = ContentType.objects.get_for_model(obj)
    gallery_images = PostImage.objects.filter(content_type=ct, object_id=obj.pk).select_related('uploaded_by')
    gallery_videos = PostVideo.objects.filter(content_type=ct, object_id=obj.pk).select_related('uploaded_by')

    context = {
        'page_title': f'{obj} - OneTownCity',
        'obj': obj,
        'model_key': model_key,
        'gallery_images': gallery_images,
        'gallery_videos': gallery_videos,
        'active_nav': 'posts',
        **_community_context(request, obj),
    }
    return render(request, 'dashboard/post_detail.html', context)


@posts_dashboard_required
@require_POST
def dashboard_post_toggle_active(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    _ensure_post_owner_access(request, obj)
    obj.is_active = not obj.is_active
    obj.save(update_fields=['is_active'])
    messages.success(request, f'{obj} is now {"active" if obj.is_active else "inactive"}.')
    return redirect(_safe_next(request, reverse('core:dashboard_posts')))


@posts_dashboard_required
@require_POST
def dashboard_post_toggle_featured(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    _ensure_post_owner_access(request, obj)
    obj.is_featured = not obj.is_featured
    obj.save(update_fields=['is_featured'])
    messages.success(request, f'{obj} is now {"featured" if obj.is_featured else "not featured"}.')
    return redirect(_safe_next(request, reverse('core:dashboard_posts')))


@posts_dashboard_required
@require_POST
def dashboard_post_add_images(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    _ensure_post_owner_access(request, obj)
    ct = ContentType.objects.get_for_model(obj)

    images, image_errors = _validate_gallery_files(
        request.FILES.getlist('images'), GALLERY_IMAGE_TYPES, GALLERY_IMAGE_MAX_BYTES, 'image'
    )
    videos, video_errors = _validate_gallery_files(
        request.FILES.getlist('videos'), GALLERY_VIDEO_TYPES, GALLERY_VIDEO_MAX_BYTES, 'video'
    )
    for err in image_errors + video_errors:
        messages.error(request, err)

    added = 0
    if images:
        start_order = PostImage.objects.filter(content_type=ct, object_id=obj.pk).count()
        for i, uploaded_file in enumerate(images):
            PostImage.objects.create(
                content_type=ct, object_id=obj.pk, image=uploaded_file,
                order=start_order + i, uploaded_by=request.user,
            )
        added += len(images)
    if videos:
        start_order = PostVideo.objects.filter(content_type=ct, object_id=obj.pk).count()
        for i, uploaded_file in enumerate(videos):
            PostVideo.objects.create(
                content_type=ct, object_id=obj.pk, video=uploaded_file,
                order=start_order + i, uploaded_by=request.user,
            )
        added += len(videos)

    if added:
        messages.success(request, f'{added} gallery item(s) added.')
    elif not (image_errors or video_errors):
        messages.error(request, 'Choose at least one photo or video to upload.')
    return redirect('core:dashboard_post_detail', model_key=model_key, pk=pk)


@posts_dashboard_required
@require_POST
def dashboard_post_delete_image(request, image_pk):
    image = get_object_or_404(PostImage, pk=image_pk)
    obj = image.content_object
    if obj is not None:
        _ensure_post_owner_access(request, obj)
    model_key = image.content_type.model
    image.delete()
    messages.success(request, 'Image deleted.')
    if obj is None:
        return redirect('core:dashboard_posts')
    return redirect('core:dashboard_post_detail', model_key=model_key, pk=obj.pk)


@posts_dashboard_required
@require_POST
def dashboard_post_delete_video(request, video_pk):
    video = get_object_or_404(PostVideo, pk=video_pk)
    obj = video.content_object
    if obj is not None:
        _ensure_post_owner_access(request, obj)
    model_key = video.content_type.model
    video.delete()
    messages.success(request, 'Video deleted.')
    if obj is None:
        return redirect('core:dashboard_posts')
    return redirect('core:dashboard_post_detail', model_key=model_key, pk=obj.pk)


@posts_dashboard_required
@require_POST
def dashboard_post_set_cover_image(request, image_pk):
    image = get_object_or_404(PostImage, pk=image_pk)
    obj = image.content_object
    if obj is None:
        raise Http404('Post no longer exists')
    _ensure_post_owner_access(request, obj)
    model_key = image.content_type.model

    if obj.image:
        obj.image.delete(save=False)
    obj.image = None
    obj.image_url = image.image.url
    obj.save(update_fields=['image', 'image_url'])

    messages.success(request, 'Cover image updated.')
    return redirect('core:dashboard_post_detail', model_key=model_key, pk=obj.pk)


BULK_ACTION_LABELS = {
    'approve': 'approved', 'reject': 'rejected', 'enable': 'enabled', 'disable': 'disabled',
    'feature': 'featured', 'unfeature': 'unfeatured', 'delete': 'deleted',
}


@posts_dashboard_required
@require_POST
def dashboard_posts_bulk_action(request):
    action = request.POST.get('bulk_action')
    note = request.POST.get('note', '').strip()
    raw_items = request.POST.getlist('items')

    if action not in BULK_ACTION_LABELS:
        messages.error(request, 'Unknown bulk action.')
        return redirect(_safe_next(request, reverse('core:dashboard_posts')))

    # Approval is tied to the same reviewer roles as dashboard_pending_listings
    # / dashboard_listing_review (Super Admin, City Admin, or a Sub Admin
    # granted approve_content/reject_content — generically or just for one
    # listing type, e.g. approve_businesses) — plain Content Providers get
    # moderation powers here (delete/enable/feature) but not approval
    # authority, even over their own listings.
    profile = request.profile
    if action in ('approve', 'reject') and profile.is_sub_admin and not (
        profile.has_permission(f'{action}_content')
        or any(profile.has_content_permission(action, model) for model in Profile.CONTENT_TYPE_PERMISSIONS)
    ):
        messages.error(request, 'You do not have permission to approve or reject listings.')
        return redirect(_safe_next(request, reverse('core:dashboard_posts')))

    count = 0
    for raw in raw_items:
        model_key, _, pk = raw.partition(':')
        model_cls = LISTING_MODELS.get(model_key)
        if model_cls is None or not pk.isdigit():
            continue
        obj = model_cls.objects.filter(pk=pk).first()
        if obj is None:
            continue
        if not _can_manage_post(profile, obj):
            continue
        if action in ('approve', 'reject') and profile.is_sub_admin and not profile.has_content_permission(action, model_key):
            continue
        if action in ('approve', 'reject') and obj.owner_id == request.user.id:
            continue

        if action in ('approve', 'reject'):
            _apply_listing_review(request, obj, action, note)
        elif action == 'enable':
            obj.is_active = True
            obj.save(update_fields=['is_active'])
        elif action == 'disable':
            obj.is_active = False
            obj.save(update_fields=['is_active'])
        elif action == 'feature':
            obj.is_featured = True
            obj.save(update_fields=['is_featured'])
        elif action == 'unfeature':
            obj.is_featured = False
            obj.save(update_fields=['is_featured'])
        elif action == 'delete':
            owner, title = obj.owner, str(obj)
            obj.delete()
            if owner and owner.id != request.user.id:
                notify(
                    owner, 'listing_deleted',
                    f'Your listing "{title}" was deleted by a Super Admin.' if request.profile.is_super_admin
                    else f'Your listing "{title}" was deleted by an admin.',
                    url=reverse('core:my_listings'),
                )
        count += 1

    messages.success(request, f'{count} post(s) {BULK_ACTION_LABELS[action]}.')
    return redirect(_safe_next(request, reverse('core:dashboard_posts')))


def _export_reports_allowed(request):
    """Export Reports (Reports & Analytics group) is its own delegable
    permission — a Sub Admin with content view/review access doesn't
    automatically get bulk-export rights over it."""
    profile = request.profile
    if profile.is_sub_admin and not profile.has_permission('export_reports'):
        messages.error(request, 'You do not have permission to export reports.')
        return False
    return True


@posts_dashboard_required
def dashboard_posts_export_excel(request):
    if not _export_reports_allowed(request):
        return redirect('core:dashboard_posts')
    items, *_ = _filtered_post_items(request)
    workbook = build_posts_workbook(items)
    response = HttpResponse(content_type='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
    response['Content-Disposition'] = 'attachment; filename="onetowncity_posts.xlsx"'
    response.write(workbook.getvalue())
    return response


@posts_dashboard_required
def dashboard_posts_export_pdf(request):
    if not _export_reports_allowed(request):
        return redirect('core:dashboard_posts')
    items, *_ = _filtered_post_items(request)
    pdf = build_posts_pdf(items)
    response = HttpResponse(pdf.getvalue(), content_type='application/pdf')
    response['Content-Disposition'] = 'attachment; filename="onetowncity_posts.pdf"'
    return response