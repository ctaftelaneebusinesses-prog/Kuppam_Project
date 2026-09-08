"""Minimal, no-magic object builders shared by core.api's test modules."""
from django.contrib.auth import get_user_model

from core.models import (
    AdminCategoryPermission, Business, Category, Event, Job, ListingStatus, Location, LostFound, News, Profile,
    Project, Property, Scholarship, UserRole,
)

User = get_user_model()

_counter = {'n': 0}


def _next_username(prefix):
    _counter['n'] += 1
    return f'{prefix}{_counter["n"]}'


def make_user(role=UserRole.USER, blocked=False, suspended=False, super_admin=False, prefix='user'):
    username = _next_username(prefix)
    user = User.objects.create_user(username=username, email=f'{username}@example.com', password='pass-12345!')
    profile = Profile.objects.create(
        user=user,
        role=UserRole.SUPER_ADMIN if super_admin else role,
        full_name=username.title(),
        profile_completed=True,
        is_blocked=blocked,
        is_suspended=suspended,
    )
    return user, profile


def make_city(name='Kuppam', slug=None):
    slug = slug or name.lower().replace(' ', '-') + f'-{_next_username("city")}'
    return Location.objects.create(kind=Location.Kind.CITY, name=name, slug=slug, country_code='IN')


def make_category(listing_model='business', label=None, key=None):
    key = key or _next_username(f'{listing_model}-cat')
    return Category.objects.create(key=key, label=label or key.title(), listing_model=listing_model, is_active=True)


def grant_category(admin_user, category):
    AdminCategoryPermission.objects.get_or_create(admin=admin_user, category=category)


def make_business(owner=None, city=None, listing_category=None, status=ListingStatus.APPROVED, is_active=True, category='retail', name=None):
    return Business.objects.create(
        name=name or _next_username('Business'), category=category, address='123 Main St',
        phone_number='9876543210', owner=owner, city=city, listing_category=listing_category,
        status=status, is_active=is_active,
    )


def make_property(owner=None, city=None, listing_category=None, status=ListingStatus.APPROVED, is_active=True):
    return Property.objects.create(
        title=_next_username('Property'), property_type='sale', price=1000, location='Main Rd',
        contact_number='9876543210', owner=owner, city=city, listing_category=listing_category,
        status=status, is_active=is_active,
    )


def make_job(owner=None, city=None, listing_category=None, status=ListingStatus.APPROVED, is_active=True):
    return Job.objects.create(
        job_title=_next_username('Job'), company='Acme', location='Main Rd', contact_number='9876543210',
        owner=owner, city=city, listing_category=listing_category, status=status, is_active=is_active,
    )


def make_event(owner=None, city=None, listing_category=None, status=ListingStatus.APPROVED, is_active=True, event_date=None):
    from django.utils import timezone
    return Event.objects.create(
        title=_next_username('Event'), event_date=event_date or timezone.localdate(), location='Town Hall',
        owner=owner, city=city, listing_category=listing_category, status=status, is_active=is_active,
    )


def make_news(owner=None, city=None, listing_category=None, status=ListingStatus.APPROVED, is_active=True):
    return News.objects.create(
        title=_next_username('News'), content='Something happened.', owner=owner, city=city,
        listing_category=listing_category, status=status, is_active=is_active,
    )


def make_project(owner=None, city=None, listing_category=None, status=ListingStatus.APPROVED, is_active=True):
    return Project.objects.create(
        title=_next_username('Project'), project_status='planned', location='Main Rd', owner=owner, city=city,
        listing_category=listing_category, status=status, is_active=is_active,
    )


def make_scholarship(owner=None, city=None, listing_category=None, status=ListingStatus.APPROVED, is_active=True):
    return Scholarship.objects.create(
        title=_next_username('Scholarship'), scholarship_type='merit', provider='Test Foundation',
        owner=owner, city=city, listing_category=listing_category, status=status, is_active=is_active,
    )


def make_lostfound(owner=None, city=None, listing_category=None, status=ListingStatus.APPROVED, is_active=True, report_type='lost'):
    from django.utils import timezone
    return LostFound.objects.create(
        report_type=report_type, title=_next_username('LostItem'), item_category='other',
        event_date=timezone.localdate(), location='Main Rd',
        owner=owner, city=city, listing_category=listing_category, status=status, is_active=is_active,
    )


MAKERS = {
    'business': make_business,
    'property': make_property,
    'job': make_job,
    'event': make_event,
    'news': make_news,
    'project': make_project,
    'scholarship': make_scholarship,
    'lostfound': make_lostfound,
}
