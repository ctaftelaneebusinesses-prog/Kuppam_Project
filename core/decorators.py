from functools import wraps

from django.conf import settings
from django.contrib import messages
from django.contrib.auth import logout
from django.contrib.auth.decorators import login_required
from django.shortcuts import redirect

from .models import Profile, UserRole


def staff_required(view_func):
    """
    Restricts a view to logged-in staff users only (used for the Excel
    upload tools and other developer-only pages). Unrelated to the new
    Google/Supabase-authenticated Super Admin / Admin / User roles.
    """
    @login_required(login_url='core:admin_login')
    @wraps(view_func)
    def wrapper(request, *args, **kwargs):
        if not request.user.is_staff:
            messages.error(request, 'You do not have permission to access this page.')
            return redirect('core:home')
        return view_func(request, *args, **kwargs)
    return wrapper


def onboarding_required(view_func):
    """
    Restricts a view to signed-in Google/Supabase users who have completed
    their profile (and, for the 'user' role, picked an intent). Redirects
    into the onboarding flow instead of bouncing them out.
    """
    @login_required(login_url=settings.GOOGLE_LOGIN_URL)
    @wraps(view_func)
    def wrapper(request, *args, **kwargs):
        profile, _ = Profile.objects.get_or_create(user=request.user)
        if profile.is_blocked:
            logout(request)
            messages.error(request, 'This account has been blocked. Contact support if you think this is a mistake.')
            return redirect('core:google_login')
        if not profile.profile_completed:
            return redirect('core:complete_profile')
        if profile.role == UserRole.USER and not profile.intent:
            return redirect('core:choose_intent')
        request.profile = profile
        return view_func(request, *args, **kwargs)
    return wrapper


def role_required(*roles):
    """Restricts a view to one or more Profile roles (e.g. UserRole.SUPER_ADMIN)."""
    def decorator(view_func):
        @onboarding_required
        @wraps(view_func)
        def wrapper(request, *args, **kwargs):
            profile = request.profile
            if profile.role not in roles:
                messages.error(request, 'You do not have permission to access this page.')
                return redirect('core:home')
            return view_func(request, *args, **kwargs)
        return wrapper
    return decorator


super_admin_required = role_required(UserRole.SUPER_ADMIN)
admin_or_super_required = role_required(UserRole.SUPER_ADMIN, UserRole.ADMIN)
city_admin_or_super_required = role_required(UserRole.SUPER_ADMIN, UserRole.CITY_ADMIN)


def _sub_admin_has_content_access(profile):
    return profile.is_sub_admin and (
        profile.has_permission('review_content') or profile.has_permission('manage_city_content')
    )


def content_review_required(view_func):
    """
    Pending Listings / listing review: Super Admin and City Admin always
    qualify (city-scoping happens inside the view via managed_city_ids());
    a Sub Admin only qualifies if their City Admin has granted them
    review_content or manage_city_content (see UserPermission).
    """
    @onboarding_required
    @wraps(view_func)
    def wrapper(request, *args, **kwargs):
        profile = request.profile
        if not (profile.is_super_admin or profile.is_city_admin or _sub_admin_has_content_access(profile)):
            messages.error(request, 'You do not have permission to access this page.')
            return redirect('core:home')
        return view_func(request, *args, **kwargs)
    return wrapper


def posts_dashboard_required(view_func):
    """
    The Posts dashboard and its detail/toggle/gallery/bulk/export siblings:
    Super Admin (everything), Content Provider (their own listings only),
    City Admin (their city/cities), or a permitted Sub Admin (same city
    scope as City Admin, gated the same way as content_review_required).
    """
    @onboarding_required
    @wraps(view_func)
    def wrapper(request, *args, **kwargs):
        profile = request.profile
        allowed = (
            profile.is_super_admin or profile.is_admin or profile.is_city_admin
            or _sub_admin_has_content_access(profile)
        )
        if not allowed:
            messages.error(request, 'You do not have permission to access this page.')
            return redirect('core:home')
        return view_func(request, *args, **kwargs)
    return wrapper


def excel_upload_allowed(view_func):
    """
    Restricts a view to Django staff (legacy developer accounts) OR a
    Google/Supabase-authenticated Admin/Super Admin profile. Two separate
    auth flows exist side by side (see staff_required's docstring), so this
    checks both rather than picking one.
    """
    @wraps(view_func)
    def wrapper(request, *args, **kwargs):
        if not request.user.is_authenticated:
            return redirect(settings.GOOGLE_LOGIN_URL)
        if request.user.is_staff:
            return view_func(request, *args, **kwargs)
        profile, _ = Profile.objects.get_or_create(user=request.user)
        if profile.is_blocked:
            logout(request)
            messages.error(request, 'This account has been blocked. Contact support if you think this is a mistake.')
            return redirect('core:google_login')
        if profile.role not in (UserRole.ADMIN, UserRole.SUPER_ADMIN):
            messages.error(request, 'You do not have permission to access this page.')
            return redirect('core:home')
        request.profile = profile
        return view_func(request, *args, **kwargs)
    return wrapper
