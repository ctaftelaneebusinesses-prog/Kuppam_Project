# Adds "Scholarships & Government Schemes" and "Lost & Found" as
# PlatformModule rows — every other listing_model (business/property/job/
# event/news/project) already has one (see 0028_seed_permissions_and_modules)
# so a Super Admin can switch it off platform-wide from Manage Platform
# Modules (core.models.PlatformModule / CityModule.is_enabled_for_city, read
# by apply_new_listing_submission before any new listing of that type is
# accepted). Without a row here these two would work by default (no row =
# allowed — see is_enabled_for_city's DoesNotExist branch), but a Super
# Admin would have no way to ever turn them off, unlike every other type.
from django.db import migrations

MODULES = [
    ('scholarship', 'Scholarships & Government Schemes', 'Scholarship and government scheme listings for students.'),
    ('lostfound', 'Lost & Found', 'User-reported lost and found item listings.'),
]


def seed(apps, schema_editor):
    PlatformModule = apps.get_model('core', 'PlatformModule')
    for key, label, description in MODULES:
        PlatformModule.objects.get_or_create(key=key, defaults={'label': label, 'description': description})


def unseed(apps, schema_editor):
    PlatformModule = apps.get_model('core', 'PlatformModule')
    PlatformModule.objects.filter(key__in=[key for key, _, _ in MODULES]).delete()


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0046_seed_scholarship_lostfound_categories'),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]
