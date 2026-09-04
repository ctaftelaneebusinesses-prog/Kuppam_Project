from django.db import migrations

# (key, label, group) — the full delegable Sub Admin permission catalog.
# New permissions can always be added later the same way: a row here (or via
# Django admin) plus, if it should gate something, a has_permission()/
# has_content_permission() check at the relevant view.
NEW_PERMISSIONS = [
    ('view_dashboard', 'View Dashboard', 'Dashboard'),

    ('view_content', 'View Content', 'Content'),
    ('add_content', 'Add Content', 'Content'),
    ('edit_content', 'Edit Content', 'Content'),
    ('delete_content', 'Delete Content', 'Content'),

    ('add_content_provider', 'Add Content Provider', 'Content Providers'),
    ('edit_content_provider', 'Edit Content Provider', 'Content Providers'),
    ('toggle_content_provider', 'Activate/Deactivate Content Provider', 'Content Providers'),

    ('view_businesses', 'View Businesses', 'Businesses'),
    ('add_businesses', 'Add Businesses', 'Businesses'),
    ('edit_businesses', 'Edit Businesses', 'Businesses'),
    ('delete_businesses', 'Delete Businesses', 'Businesses'),
    ('approve_businesses', 'Approve Businesses', 'Businesses'),

    ('view_events', 'View Events', 'Events'),
    ('add_events', 'Add Events', 'Events'),
    ('edit_events', 'Edit Events', 'Events'),
    ('delete_events', 'Delete Events', 'Events'),
    ('approve_events', 'Approve Events', 'Events'),

    ('view_announcements', 'View Announcements', 'Announcements'),
    ('add_announcements', 'Add Announcements', 'Announcements'),
    ('edit_announcements', 'Edit Announcements', 'Announcements'),
    ('delete_announcements', 'Delete Announcements', 'Announcements'),

    ('view_categories', 'View Categories', 'Categories'),
    ('add_categories', 'Add Categories', 'Categories'),
    ('edit_categories', 'Edit Categories', 'Categories'),
    ('delete_categories', 'Delete Categories', 'Categories'),

    ('view_users', 'View Users', 'Users'),

    ('view_reports', 'View Reports', 'Reports & Analytics'),
    ('export_reports', 'Export Reports', 'Reports & Analytics'),
]

# Existing permissions (from earlier migrations) that this screen's grouping
# reorganizes so they sit next to their new siblings above.
REGROUPED_PERMISSIONS = [
    ('view_city_analytics', 'Dashboard'),
    ('view_content_providers', 'Content Providers'),
    ('manage_city_content', 'Content'),
]


def seed(apps, schema_editor):
    Permission = apps.get_model('core', 'Permission')
    for key, label, group in NEW_PERMISSIONS:
        Permission.objects.get_or_create(key=key, defaults={'label': label, 'group': group})
    for key, group in REGROUPED_PERMISSIONS:
        Permission.objects.filter(key=key).update(group=group)


def unseed(apps, schema_editor):
    Permission = apps.get_model('core', 'Permission')
    Permission.objects.filter(key__in=[key for key, _, _ in NEW_PERMISSIONS]).delete()


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0030_seed_city_operations_permissions'),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]
