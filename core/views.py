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
#         'page_title': 'Hello Kuppam - Your Local Information Portal',
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
#         'page_title': f'Search Results for "{query}"' if query else 'Search - Hello Kuppam',
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
#         'page_title': 'Admin Login - Hello Kuppam',
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
#                 subject=f'[Hello Kuppam Contact] {subject}',
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
#         'page_title': 'Contact Us - Hello Kuppam',
#         'form': form,
#     }
#     return render(request, 'contact.html', context)

import json
import re
from urllib.parse import urlencode

from django.contrib import messages
from django.contrib.auth import get_user_model, login, logout
from django.contrib.auth.decorators import login_required
from django.contrib.contenttypes.models import ContentType
from django.conf import settings
from django.core.exceptions import ValidationError
from django.core.mail import send_mail
from django.core.paginator import Paginator
from django.core.validators import validate_email
from django.db.models import F, Q
from django.http import Http404, HttpResponse, JsonResponse
from django.shortcuts import get_object_or_404, redirect, render
from django.urls import reverse
from django.utils import timezone
from django.utils.http import url_has_allowed_host_and_scheme
from django.views.decorators.csrf import ensure_csrf_cookie
from django.views.decorators.http import require_POST

from .decorators import admin_or_super_required, onboarding_required, staff_required, super_admin_required
from .excel_utils import UPLOAD_CONFIGS, ExcelValidationError, build_sample_workbook, process_excel_upload
from .forms import (
    AdminLoginForm, AdminRequestForm, AdminRequestReviewForm, CommentForm, ContactForm,
    ExcelUploadForm, LISTING_SUBMIT_FORMS, PasswordLoginForm, ProfileCompletionForm,
    RegisterForm, ReportForm, ReviewForm,
)
from .models import (
    AdminCategoryPermission, AdminRequest, AdminRequestStatus, Business, Category, Comment,
    ContactMessage, Event, Favorite, Intent, Job, Like, ListingStatus, LoginHistory, News,
    NewsletterSubscriber, Notification, PostImage, Profile, Project, Property, Report, Review,
    Share, UserRole,
)
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


def _safe_next(request, fallback):
    """Validates a POSTed `next` redirect target so it can't be used for an open redirect."""
    next_url = request.POST.get('next')
    if next_url and url_has_allowed_host_and_scheme(next_url, allowed_hosts={request.get_host()}, require_https=request.is_secure()):
        return next_url
    return fallback


def _public_qs(model_cls):
    """Base queryset for anything shown to the public: active AND approved."""
    return model_cls.objects.filter(is_active=True, status=ListingStatus.APPROVED)


def _detail_qs(request, model_cls):
    """
    Queryset used to look up a single listing for its detail page. Super
    admin can open any listing regardless of status/active state (e.g. to
    preview a pending submission); everyone else only sees public rows.
    """
    profile = getattr(request.user, 'profile', None) if request.user.is_authenticated else None
    if profile and profile.is_super_admin:
        return model_cls.objects.all()
    return _public_qs(model_cls)


def _bump_views(model_cls, pk):
    model_cls.objects.filter(pk=pk).update(view_count=F('view_count') + 1)


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


# Category definitions used across the homepage, search, and (later) listing pages.
# 'image' is used by the homepage Services cards — always a real, professional
# photo (never an icon/emoji-style illustration). 'count_fn' computes each
# card's live listing count from the same querysets the list pages use.
CATEGORIES = [
    {
        'name': 'Real Estate', 'icon': 'bi-house-door', 'slug': 'real-estate',
        'image': 'images/services/real-estate.jpg',
        'description': 'Find houses, apartments, plots, villas, rental properties, and commercial spaces available across Kuppam.',
        'count_fn': lambda: _public_qs(Property).count(),
    },
    {
        'name': 'Businesses', 'icon': 'bi-shop', 'slug': 'shops',
        'image': 'images/services/business.jpg',
        'description': 'Explore car garages, clothing and textile shops, stationery shops, supermarkets, salons, and other local businesses around Kuppam.',
        'count_fn': lambda: _public_qs(Business).exclude(category__in=_DIRECTORY_BUSINESS_CATEGORIES).count(),
    },
    {
        'name': 'Jobs', 'icon': 'bi-briefcase', 'slug': 'jobs',
        'image': 'images/services/jobs.jpg',
        'description': 'Browse job openings from local shops, offices, and companies hiring across Kuppam.',
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
        'description': 'Discover the best restaurants, cafés, bakeries, and food outlets in Kuppam.',
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
        'description': 'Explore schools, colleges, universities, and other educational institutions in Kuppam.',
        'count_fn': lambda: _public_qs(Business).filter(category__in=DIRECTORY_CATEGORIES['education']['categories']).count(),
    },
    {
        'name': 'Transport', 'icon': 'bi-bus-front', 'slug': 'transport',
        'image': 'images/services/transport.jpg',
        'description': 'Find buses, taxis, auto services, logistics, and transportation facilities.',
        'count_fn': lambda: _public_qs(Business).filter(category='transport').count(),
    },
    {
        'name': 'News', 'icon': 'bi-newspaper', 'slug': 'news',
        'image': 'images/services/news.jpg',
        'description': 'Catch up on local announcements, civic updates, and news from around Kuppam.',
        'count_fn': lambda: _public_qs(News).count(),
    },
    {
        'name': 'Upcoming Projects', 'icon': 'bi-cone-striped', 'slug': 'projects',
        'image': 'images/services/upcoming-projects.jpg',
        'description': 'Track planned and ongoing civic and infrastructure projects shaping Kuppam.',
        'count_fn': lambda: _public_qs(Project).count(),
    },
]


def _categories_with_counts():
    """CATEGORIES enriched with a live listing count per card, computed fresh per request."""
    result = []
    for cat in CATEGORIES:
        enriched = {k: v for k, v in cat.items() if k != 'count_fn'}
        enriched['count'] = cat['count_fn']()
        result.append(enriched)
    return result


def home(request):
    """
    Homepage: hero section, search box, category grid, featured businesses.
    """
    # Fall back to the latest listings whenever nothing has been marked
    # "Featured" yet, so these sections never render as blank gaps on the
    # homepage while admins are still curating featured picks.
    featured_businesses = _public_qs(Business).filter(is_featured=True)[:6] \
        or _public_qs(Business).order_by('-created_at')[:6]
    featured_properties = _public_qs(Property).filter(is_featured=True)[:6] \
        or _public_qs(Property).order_by('-created_at')[:6]
    featured_jobs = _public_qs(Job).filter(is_featured=True)[:6] \
        or _public_qs(Job).order_by('-created_at')[:6]
    featured_events = _public_qs(Event).filter(is_featured=True, event_date__gte=timezone.localdate())[:6] \
        or _public_qs(Event).filter(event_date__gte=timezone.localdate()).order_by('event_date')[:6]
    featured_news = _public_qs(News).filter(is_featured=True)[:6] \
        or _public_qs(News).order_by('-created_at')[:6]
    featured_projects = _public_qs(Project).filter(is_featured=True)[:6] \
        or _public_qs(Project).order_by('-created_at')[:6]

    stats = {
        'businesses': _public_qs(Business).count(),
        'properties': _public_qs(Property).count(),
        'jobs': _public_qs(Job).count(),
        'users': get_user_model().objects.count(),
    }

    context = {
        'page_title': 'Hello Kuppam - Your Local Information Portal',
        'categories': _categories_with_counts(),
        'featured_businesses': featured_businesses,
        'featured_properties': featured_properties,
        'featured_jobs': featured_jobs,
        'featured_events': featured_events,
        'featured_news': featured_news,
        'featured_projects': featured_projects,
        'stats': stats,
    }
    return render(request, 'home.html', context)


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
                'business', 'Businesses', 'bi-shop',
                _public_qs(Business).exclude(category__in=_DIRECTORY_BUSINESS_CATEGORIES),
                'core:business_list', 'partials/business_card.html', 'business',
            ),
        ]
        for directory_key, config in DIRECTORY_CATEGORIES.items():
            sections.append(_section(
                'business', config['label'], config['icon'],
                _public_qs(Business).filter(category__in=config['categories']),
                SEARCH_CATEGORY_REDIRECT[directory_key], 'partials/business_card.html', 'business',
            ))
        sections += [
            _section('property', 'Properties', 'bi-house-door', _public_qs(Property), 'core:property_list', 'partials/property_card.html', 'property'),
            _section('job', 'Jobs', 'bi-briefcase', _public_qs(Job), 'core:job_list', 'partials/job_card.html', 'job'),
            _section('event', 'Events', 'bi-calendar-event', _public_qs(Event), 'core:event_list', 'partials/event_card.html', 'event'),
            _section('news', 'News', 'bi-newspaper', _public_qs(News), 'core:news_list', 'partials/news_card.html', 'article'),
            _section('project', 'Upcoming Projects', 'bi-cone-striped', _public_qs(Project), 'core:project_list', 'partials/project_card.html', 'project'),
        ]
        results = [s for s in sections if s['count']]
        total_results = sum(s['count'] for s in sections)

    context = {
        'page_title': f'Search Results for "{query}"' if query else 'Search - Hello Kuppam',
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
    businesses = _public_qs(Business).exclude(category__in=_DIRECTORY_BUSINESS_CATEGORIES)

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
        'page_title': 'Businesses - Hello Kuppam',
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
    _bump_views(Business, business.pk)
    related_businesses = _public_qs(Business).filter(category=business.category).exclude(pk=business.pk)[:3]

    context = {
        'page_title': f'{business.name} - Hello Kuppam',
        'business': business,
        'related_businesses': related_businesses,
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

    businesses = _public_qs(Business).filter(category__in=config['categories'])

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
        'page_title': f"{config['label']} - Hello Kuppam",
        'page_obj': page_obj,
        'query': query,
        'total_results': businesses.count(),
        'directory_label': config['label'],
        'directory_icon': config['icon'],
        'subcategory_choices': subcategory_choices,
        'selected_subcategory': subcategory,
    }
    return render(request, 'directory_list.html', context)


def property_list(request):
    """
    Properties listing page with search (by title or location), filter
    by type, and pagination.
    """
    properties = _public_qs(Property)

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
        'page_title': 'Properties - Hello Kuppam',
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
    _bump_views(Property, property_obj.pk)
    related_properties = _public_qs(Property).filter(property_type=property_obj.property_type).exclude(pk=property_obj.pk)[:3]

    context = {
        'page_title': f'{property_obj.title} - Hello Kuppam',
        'property': property_obj,
        'related_properties': related_properties,
        **_community_context(request, property_obj),
    }
    return render(request, 'property_detail.html', context)


def job_list(request):
    """
    Jobs listing page with search (by job title, company or location)
    and pagination.
    """
    jobs = _public_qs(Job)

    query = request.GET.get('q', '').strip()

    if query:
        jobs = jobs.filter(
            Q(job_title__icontains=query) | Q(company__icontains=query) | Q(location__icontains=query)
        )

    paginator = Paginator(jobs, 9)
    page_number = request.GET.get('page')
    page_obj = paginator.get_page(page_number)

    context = {
        'page_title': 'Jobs - Hello Kuppam',
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
    _bump_views(Job, job.pk)
    related_jobs = _public_qs(Job).filter(company=job.company).exclude(pk=job.pk)[:3]

    context = {
        'page_title': f'{job.job_title} at {job.company} - Hello Kuppam',
        'job': job,
        'related_jobs': related_jobs,
        **_community_context(request, job),
    }
    return render(request, 'job_detail.html', context)


def event_list(request):
    """
    Events listing page with search (by title or location) and pagination.
    Upcoming events are shown first (model default ordering).
    """
    events = _public_qs(Event)

    query = request.GET.get('q', '').strip()

    if query:
        events = events.filter(
            Q(title__icontains=query) | Q(location__icontains=query)
        )

    paginator = Paginator(events, 9)
    page_number = request.GET.get('page')
    page_obj = paginator.get_page(page_number)

    context = {
        'page_title': 'Events - Hello Kuppam',
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
    _bump_views(Event, event.pk)
    related_events = _public_qs(Event).exclude(pk=event.pk)[:3]

    context = {
        'page_title': f'{event.title} - Hello Kuppam',
        'event': event,
        'related_events': related_events,
        **_community_context(request, event),
    }
    return render(request, 'event_detail.html', context)


def news_list(request):
    """
    News listing page with search (by title or content) and pagination.
    Most recently published articles are shown first.
    """
    articles = _public_qs(News)

    query = request.GET.get('q', '').strip()

    if query:
        articles = articles.filter(
            Q(title__icontains=query) | Q(content__icontains=query)
        )

    paginator = Paginator(articles, 9)
    page_number = request.GET.get('page')
    page_obj = paginator.get_page(page_number)

    context = {
        'page_title': 'News - Hello Kuppam',
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
    _bump_views(News, article.pk)
    related_articles = _public_qs(News).exclude(pk=article.pk)[:3]

    context = {
        'page_title': f'{article.title} - Hello Kuppam',
        'article': article,
        'related_articles': related_articles,
        **_community_context(request, article),
    }
    return render(request, 'news_detail.html', context)


def project_list(request):
    """
    Upcoming Projects listing page with search (by title or location) and
    pagination. Featured/newest projects are shown first (model default
    ordering).
    """
    projects = _public_qs(Project)

    query = request.GET.get('q', '').strip()

    if query:
        projects = projects.filter(
            Q(title__icontains=query) | Q(location__icontains=query)
        )

    paginator = Paginator(projects, 9)
    page_number = request.GET.get('page')
    page_obj = paginator.get_page(page_number)

    context = {
        'page_title': 'Upcoming Projects - Hello Kuppam',
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
    _bump_views(Project, project.pk)
    related_projects = _public_qs(Project).exclude(pk=project.pk)[:3]

    context = {
        'page_title': f'{project.title} - Hello Kuppam',
        'project': project,
        'related_projects': related_projects,
        **_community_context(request, project),
    }
    return render(request, 'project_detail.html', context)


@staff_required
def upload_hub(request):
    """
    Excel Upload Center — staff-only hub linking to the bulk-upload page
    for each module (Businesses, Properties, Jobs, Events, News).
    """
    context = {
        'page_title': 'Excel Upload Center - Hello Kuppam',
        'configs': UPLOAD_CONFIGS.values(),
    }
    return render(request, 'uploads/upload_hub.html', context)


@staff_required
def upload_view(request, model_key):
    """
    Handles Excel upload + validation + insert-or-update for a single
    module. The module is selected via model_key (see UPLOAD_CONFIGS).
    """
    config = UPLOAD_CONFIGS.get(model_key)
    if config is None:
        raise Http404('Unknown upload type')

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
        'page_title': f'Upload {config["label"]} - Hello Kuppam',
        'form': form,
        'config': config,
        'model_key': model_key,
        'result': result,
    }
    return render(request, 'uploads/upload_form.html', context)


@staff_required
def download_sample(request, model_key):
    """
    Streams a ready-to-fill .xlsx sample template for the given module.
    """
    config = UPLOAD_CONFIGS.get(model_key)
    if config is None:
        raise Http404('Unknown upload type')

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
        'page_title': 'Admin Login - Hello Kuppam',
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
                subject=f'[Hello Kuppam Contact] {subject}',
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
        'page_title': 'Contact Us - Hello Kuppam',
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
    About Us: Hello Kuppam platform profile (who we are, mission, vision,
    what we offer, how it works, team, and commitment).
    """
    context = {
        'page_title': 'About Us - Hello Kuppam',
    }
    return render(request, 'about.html', context)


def history(request):
    """History of Kuppam: a static, chronologically-ordered editorial page."""
    gallery_images = [
        {'file': 'images/history/gallery-engineering-college.png', 'caption': 'Kuppam Engineering College'},
        {'file': 'images/history/gallery-university-entrance.png', 'caption': 'Dravidian University entrance, Kuppam'},
        {'file': 'images/history/gallery-railway-alt.png', 'caption': 'Kuppam Railway Station'},
        {'file': 'images/history/ancient-temple.jpg', 'caption': 'Sri KuppamSubrahmanyaswamy Temple'},
        {'file': 'images/history/gallery-tomato.jpg', 'caption': 'Tomato cultivation, South India'},
        {'file': 'images/history/gallery-kanipakam-temple.jpg', 'caption': 'Kanipakam Temple gopuram, Chittoor district'},
        {'file': 'images/history/gallery-horsley-blue-mountains.jpg', 'caption': 'Blue mountains, Horsley Hills'},
        {'file': 'images/history/gallery-horsley-rocky.jpg', 'caption': 'Rocky terrain, Horsley Hills'},
        {'file': 'images/history/gallery-horsley-view2.jpg', 'caption': 'View from Horsley Hills'},
        {'file': 'images/history/gallery-gangamma-jatara.png', 'caption': 'Gangamma Jatara festival, Kuppam'},
        {'file': 'images/history/gallery-lush-fields.jpg', 'caption': 'Lush green fields near the hills', 'wide': True},
        {'file': 'images/history/gallery-horsley-hdr.jpg', 'caption': 'Horsley Hills, wide panorama', 'wide': True},
        {'file': 'images/history/tourism-horsley.jpg', 'caption': 'Horsley Hills, ultra-wide panorama', 'wide': True},
    ]
    context = {
        'page_title': 'History of Kuppam - Hello Kuppam',
        'gallery_images': gallery_images,
    }
    return render(request, 'history.html', context)


def privacy_policy(request):
    """Static privacy policy page, linked from the footer."""
    return render(request, 'privacy_policy.html', {'page_title': 'Privacy Policy - Hello Kuppam'})


def terms_of_service(request):
    """Static terms of service page, linked from the footer."""
    return render(request, 'terms_of_service.html', {'page_title': 'Terms of Service - Hello Kuppam'})


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


def _notify_super_admins(message, url=''):
    admins = User.objects.filter(profile__role=UserRole.SUPER_ADMIN)
    Notification.objects.bulk_create([
        Notification(recipient=admin_user, type='admin_request_submitted', message=message, url=url)
        for admin_user in admins
    ])


def _post_login_redirect(profile):
    """Where to send someone right after a successful sign-in, based on onboarding state + role."""
    if not profile.profile_completed:
        return reverse('core:complete_profile')
    if profile.role == UserRole.USER and not profile.intent:
        return reverse('core:choose_intent')
    if profile.role in (UserRole.SUPER_ADMIN, UserRole.ADMIN):
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
        'page_title': 'Sign In - Hello Kuppam',
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
        'page_title': 'Sign In - Hello Kuppam',
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
        'page_title': 'Sign In - Hello Kuppam',
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
    return render(request, 'auth_callback.html', {'page_title': 'Signing you in... - Hello Kuppam'})


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
        'page_title': 'Complete Your Profile - Hello Kuppam',
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
            messages.success(request, 'Welcome to Hello Kuppam!')
            return redirect('core:home')

    return render(request, 'choose_intent.html', {'page_title': 'Welcome - Hello Kuppam'})


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
            messages.success(request, 'Your request has been submitted for review.')
            return redirect('core:admin_request_pending')
    else:
        initial = {'categories': resubmitting.categories.all()} if resubmitting else {}
        form = AdminRequestForm(initial=initial)

    context = {
        'page_title': 'What Do You Want to Manage? - Hello Kuppam',
        'form': form,
        'resubmitting': bool(resubmitting),
    }
    return render(request, 'admin_request_form.html', context)


@onboarding_required
def admin_request_pending(request):
    if request.profile.role in (UserRole.ADMIN, UserRole.SUPER_ADMIN):
        return redirect('core:my_listings')
    admin_request = AdminRequest.objects.filter(user=request.user).order_by('-created_at').first()
    context = {'page_title': 'Application Status - Hello Kuppam', 'admin_request': admin_request}
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
        messages.success(request, 'Thanks for your review!')
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
    return render(request, 'favorites.html', {'page_title': 'My Favorites - Hello Kuppam', 'items': items, 'active_nav': 'favorites'})


# ===========================================================================
# Notifications
# ===========================================================================

@onboarding_required
def notifications_list(request):
    notification_qs = request.user.notifications.all()
    return render(request, 'notifications.html', {
        'page_title': 'Notifications - Hello Kuppam',
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


# ===========================================================================
# Content Provider (Admin): "My Listings" + submission wizard
# ===========================================================================

@onboarding_required
def my_listings(request):
    profile = request.profile
    if profile.role not in (UserRole.ADMIN, UserRole.SUPER_ADMIN):
        messages.info(request, 'Apply to become a Content Provider to add listings.')
        return redirect('core:admin_request_new')

    items = []
    for key, model_cls in LISTING_MODELS.items():
        qs = model_cls.objects.all() if profile.is_super_admin else model_cls.objects.filter(owner=request.user)
        for obj in qs.select_related('listing_category'):
            items.append({'model_key': key, 'obj': obj})
    items.sort(key=lambda item: item['obj'].created_at, reverse=True)

    permitted_categories = Category.objects.filter(is_active=True)
    if not profile.is_super_admin:
        permitted_categories = permitted_categories.filter(admin_permissions__admin=request.user)

    context = {
        'page_title': 'My Listings - Hello Kuppam',
        'items': items,
        'permitted_categories': permitted_categories,
        'active_nav': 'my_listings',
    }
    return render(request, 'dashboard/my_listings.html', context)


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
        if form.is_valid():
            obj = form.save(commit=False)
            obj.owner = request.user
            obj.listing_category = category
            obj.status = ListingStatus.PENDING
            obj.save()
            _notify_super_admins(
                f'New {category.label} listing submitted: "{obj}"',
                url=reverse('core:dashboard_pending_listings'),
            )
            messages.success(request, 'Your listing was submitted and is pending approval.')
            return redirect('core:my_listings')
    else:
        initial = {}
        if category.listing_model == 'business' and category.business_subcategory:
            initial['category'] = category.business_subcategory
        form = form_cls(initial=initial)

    context = {'page_title': f'Add {category.label} - Hello Kuppam', 'form': form, 'category': category, 'active_nav': 'my_listings'}
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

    if request.method == 'POST':
        form = form_cls(request.POST, request.FILES, instance=obj)
        if form.is_valid():
            obj = form.save(commit=False)
            if not profile.is_super_admin:
                obj.status = ListingStatus.PENDING
                obj.rejection_reason = ''
            obj.save()
            note = '' if profile.is_super_admin else ' It will be reviewed again before going live.'
            messages.success(request, f'Listing updated.{note}')
            return redirect('core:my_listings')
    else:
        form = form_cls(instance=obj)

    context = {
        'page_title': f'Edit {obj} - Hello Kuppam',
        'form': form,
        'object': obj,
        'model_key': model_key,
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
    if not profile.is_super_admin and obj.owner_id != request.user.id:
        messages.error(request, 'You can only delete your own listings.')
        return redirect('core:my_listings')
    obj.delete()
    messages.success(request, 'Listing deleted.')
    return redirect(_safe_next(request, reverse('core:my_listings')))


# ===========================================================================
# Dashboard (role-dispatched: Super Admin gets the full control panel,
# Admins land on their own "My Listings")
# ===========================================================================

@onboarding_required
def dashboard(request):
    profile = request.profile
    if profile.role == UserRole.SUPER_ADMIN:
        return _super_admin_dashboard(request)
    if profile.role == UserRole.ADMIN:
        return redirect('core:my_listings')
    messages.info(request, 'The dashboard is only available to Admins and the Super Admin.')
    return redirect('core:home')


def _super_admin_dashboard(request):
    pending_listings = sum(m.objects.filter(status=ListingStatus.PENDING).count() for m in LISTING_MODELS.values())
    approved_listings = sum(m.objects.filter(status=ListingStatus.APPROVED).count() for m in LISTING_MODELS.values())
    rejected_listings = sum(m.objects.filter(status=ListingStatus.REJECTED).count() for m in LISTING_MODELS.values())

    stats = {
        'total_users': Profile.objects.filter(role=UserRole.USER).count(),
        'total_admins': Profile.objects.filter(role=UserRole.ADMIN).count(),
        'pending_requests': AdminRequest.objects.filter(status=AdminRequestStatus.PENDING).count(),
        'approved_requests': AdminRequest.objects.filter(status=AdminRequestStatus.APPROVED).count(),
        'rejected_requests': AdminRequest.objects.filter(status=AdminRequestStatus.REJECTED).count(),
        'total_businesses': Business.objects.count(),
        'total_restaurants': Business.objects.filter(category='restaurant').count(),
        'total_schools': Business.objects.filter(category='school').count(),
        'total_colleges': Business.objects.filter(category='college').count(),
        'total_hospitals': Business.objects.filter(category='hospital').count(),
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
    }

    context = {
        'page_title': 'Super Admin Dashboard - Hello Kuppam',
        'stats': stats,
        'recent_requests': AdminRequest.objects.select_related('user').prefetch_related('categories').order_by('-created_at')[:5],
        'recent_logins': LoginHistory.objects.select_related('user').filter(event_type='login').order_by('-created_at')[:10],
        'active_nav': 'overview',
    }
    return render(request, 'dashboard/super_admin_dashboard.html', context)


@super_admin_required
def dashboard_users(request):
    query = request.GET.get('q', '').strip()
    profiles = Profile.objects.select_related('user').order_by('-created_at')
    if query:
        profiles = profiles.filter(
            Q(full_name__icontains=query) | Q(user__email__icontains=query) | Q(user__username__icontains=query)
        )
    role_filter = request.GET.get('role', '').strip()
    if role_filter:
        profiles = profiles.filter(role=role_filter)

    context = {
        'page_title': 'Manage Users - Hello Kuppam',
        'profiles': profiles,
        'query': query,
        'role_filter': role_filter,
        'role_choices': UserRole.choices,
        'active_nav': 'users',
    }
    return render(request, 'dashboard/users.html', context)


@super_admin_required
@require_POST
def dashboard_user_toggle_block(request, user_id):
    profile = get_object_or_404(Profile, user_id=user_id)
    profile.is_blocked = not profile.is_blocked
    profile.save(update_fields=['is_blocked'])
    messages.success(request, f'{profile.user.email} is now {"blocked" if profile.is_blocked else "unblocked"}.')
    return redirect('core:dashboard_users')


@super_admin_required
@require_POST
def dashboard_user_toggle_suspend(request, user_id):
    profile = get_object_or_404(Profile, user_id=user_id)
    profile.is_suspended = not profile.is_suspended
    profile.save(update_fields=['is_suspended'])
    messages.success(request, f'{profile.user.email} is now {"suspended" if profile.is_suspended else "unsuspended"}.')
    return redirect('core:dashboard_users')


@super_admin_required
def dashboard_admin_requests(request):
    status_filter = request.GET.get('status', '').strip()
    requests_qs = AdminRequest.objects.select_related('user__profile').prefetch_related('categories').order_by('-created_at')
    if status_filter:
        requests_qs = requests_qs.filter(status=status_filter)

    context = {
        'page_title': 'Admin Requests - Hello Kuppam',
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
                Notification.objects.create(
                    recipient=admin_request.user, type='admin_request_approved',
                    message='Your request to become a Content Provider has been approved!',
                    url=reverse('core:my_listings'),
                )
            elif action == 'reject':
                admin_request.status = AdminRequestStatus.REJECTED
                Notification.objects.create(
                    recipient=admin_request.user, type='admin_request_rejected',
                    message=f'Your request was rejected: {note}' if note else 'Your request was rejected.',
                    url=reverse('core:admin_request_pending'),
                )
            else:
                admin_request.status = AdminRequestStatus.CHANGES_REQUESTED
                Notification.objects.create(
                    recipient=admin_request.user, type='admin_request_changes_requested',
                    message=f'Changes requested on your request: {note}' if note else 'Changes were requested on your request.',
                    url=reverse('core:admin_request_pending'),
                )
            admin_request.save()
            messages.success(request, 'Request updated.')
            return redirect('core:dashboard_admin_requests')
    else:
        form = AdminRequestReviewForm()

    context = {
        'page_title': 'Review Admin Request - Hello Kuppam',
        'admin_request': admin_request,
        'form': form,
        'active_nav': 'requests',
    }
    return render(request, 'dashboard/admin_request_detail.html', context)


@super_admin_required
def dashboard_categories(request):
    if request.method == 'POST':
        category = get_object_or_404(Category, pk=request.POST.get('category_id'))
        category.is_active = not category.is_active
        category.save(update_fields=['is_active'])
        messages.success(request, f'{category.label} is now {"active" if category.is_active else "inactive"}.')
        return redirect('core:dashboard_categories')

    return render(request, 'dashboard/categories.html', {
        'page_title': 'Manage Categories - Hello Kuppam',
        'categories': Category.objects.all(),
        'active_nav': 'categories',
    })


@super_admin_required
def dashboard_pending_listings(request):
    status_filter = request.GET.get('status', ListingStatus.PENDING)
    items = []
    for key, model_cls in LISTING_MODELS.items():
        qs = model_cls.objects.select_related('owner', 'listing_category')
        if status_filter:
            qs = qs.filter(status=status_filter)
        for obj in qs:
            items.append({'model_key': key, 'obj': obj})
    items.sort(key=lambda item: item['obj'].created_at, reverse=True)

    context = {
        'page_title': 'Listing Approvals - Hello Kuppam',
        'items': items,
        'status_filter': status_filter,
        'status_choices': ListingStatus.choices,
        'active_nav': 'listings',
    }
    return render(request, 'dashboard/pending_listings.html', context)


def _apply_listing_review(obj, action, note, actor):
    """
    Shared approve/reject/request-changes logic used by both the single-item
    review action and the Posts dashboard's bulk-action endpoint. Mutates and
    saves `obj`, and notifies its owner (if any) exactly like the original
    single-item flow did.
    """
    obj.reviewed_by = actor
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
        Notification.objects.create(
            recipient_id=obj.owner_id, type=notif_type, message=notif_message,
            url=obj.get_absolute_url() if obj.status == ListingStatus.APPROVED else reverse('core:my_listings'),
        )


@super_admin_required
@require_POST
def dashboard_listing_review(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    _apply_listing_review(obj, request.POST.get('action'), request.POST.get('note', '').strip(), request.user)

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
def dashboard_posts(request):
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
        qs = model_cls.objects.select_related('owner', 'listing_category')
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

    owners = sorted(
        {item['obj'].owner for item in items if item['obj'].owner_id},
        key=lambda u: (u.get_full_name() or u.get_username()).lower(),
    )

    paginator = Paginator(items, 20)
    page_obj = paginator.get_page(request.GET.get('page'))

    counts = {'total': 0, 'pending': 0, 'approved': 0, 'rejected': 0}
    for model_cls in LISTING_MODELS.values():
        counts['total'] += model_cls.objects.count()
        counts['pending'] += model_cls.objects.filter(status=ListingStatus.PENDING).count()
        counts['approved'] += model_cls.objects.filter(status=ListingStatus.APPROVED).count()
        counts['rejected'] += model_cls.objects.filter(status=ListingStatus.REJECTED).count()

    context = {
        'page_title': 'Posts - Hello Kuppam',
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
        'status_choices': ListingStatus.choices,
        'counts': counts,
        'active_nav': 'posts',
    }
    return render(request, 'dashboard/posts.html', context)


@super_admin_required
def dashboard_post_detail(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls.objects.select_related('owner', 'listing_category', 'reviewed_by'), pk=pk)
    ct = ContentType.objects.get_for_model(obj)
    gallery_images = PostImage.objects.filter(content_type=ct, object_id=obj.pk).select_related('uploaded_by')

    context = {
        'page_title': f'{obj} - Hello Kuppam',
        'obj': obj,
        'model_key': model_key,
        'gallery_images': gallery_images,
        'active_nav': 'posts',
        **_community_context(request, obj),
    }
    return render(request, 'dashboard/post_detail.html', context)


@super_admin_required
@require_POST
def dashboard_post_toggle_active(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    obj.is_active = not obj.is_active
    obj.save(update_fields=['is_active'])
    messages.success(request, f'{obj} is now {"active" if obj.is_active else "inactive"}.')
    return redirect(_safe_next(request, reverse('core:dashboard_posts')))


@super_admin_required
@require_POST
def dashboard_post_toggle_featured(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    obj.is_featured = not obj.is_featured
    obj.save(update_fields=['is_featured'])
    messages.success(request, f'{obj} is now {"featured" if obj.is_featured else "not featured"}.')
    return redirect(_safe_next(request, reverse('core:dashboard_posts')))


@super_admin_required
@require_POST
def dashboard_post_add_images(request, model_key, pk):
    model_cls = LISTING_MODELS.get(model_key)
    if model_cls is None:
        raise Http404('Unknown listing type')
    obj = get_object_or_404(model_cls, pk=pk)
    ct = ContentType.objects.get_for_model(obj)
    files = request.FILES.getlist('images')
    if not files:
        messages.error(request, 'Choose at least one image to upload.')
    else:
        start_order = PostImage.objects.filter(content_type=ct, object_id=obj.pk).count()
        for i, uploaded_file in enumerate(files):
            PostImage.objects.create(
                content_type=ct, object_id=obj.pk, image=uploaded_file,
                order=start_order + i, uploaded_by=request.user,
            )
        messages.success(request, f'{len(files)} image(s) added.')
    return redirect('core:dashboard_post_detail', model_key=model_key, pk=pk)


@super_admin_required
@require_POST
def dashboard_post_delete_image(request, image_pk):
    image = get_object_or_404(PostImage, pk=image_pk)
    obj = image.content_object
    model_key = image.content_type.model
    image.delete()
    messages.success(request, 'Image deleted.')
    if obj is None:
        return redirect('core:dashboard_posts')
    return redirect('core:dashboard_post_detail', model_key=model_key, pk=obj.pk)


@super_admin_required
@require_POST
def dashboard_post_set_cover_image(request, image_pk):
    image = get_object_or_404(PostImage, pk=image_pk)
    obj = image.content_object
    if obj is None:
        raise Http404('Post no longer exists')
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


@super_admin_required
@require_POST
def dashboard_posts_bulk_action(request):
    action = request.POST.get('bulk_action')
    note = request.POST.get('note', '').strip()
    raw_items = request.POST.getlist('items')

    if action not in BULK_ACTION_LABELS:
        messages.error(request, 'Unknown bulk action.')
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

        if action in ('approve', 'reject'):
            _apply_listing_review(obj, action, note, request.user)
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
            obj.delete()
        count += 1

    messages.success(request, f'{count} post(s) {BULK_ACTION_LABELS[action]}.')
    return redirect(_safe_next(request, reverse('core:dashboard_posts')))