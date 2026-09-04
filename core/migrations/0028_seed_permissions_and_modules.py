from django.db import migrations

PERMISSIONS = [
    # (key, label, group)
    ('manage_city_admins', 'Manage City Admins', 'Users'),
    ('manage_users', 'Manage Users', 'Users'),
    ('manage_admins', 'Manage Admins', 'Users'),
    ('view_sub_admins', 'View Sub Admins', 'Users'),
    ('view_content_providers', 'View Content Providers', 'Users'),
    ('view_all_content', 'View All Content', 'Content'),
    ('manage_categories', 'Manage Categories', 'Content'),
    ('manage_content_approval', 'Manage Content Approval Settings', 'Content'),
    ('manage_modules', 'Manage Platform Modules', 'Platform'),
    ('manage_roles_permissions', 'Manage Roles & Permissions', 'Platform'),
    ('manage_settings', 'Manage Platform Settings', 'Platform'),
    ('view_analytics', 'View Dashboards & Analytics', 'Platform'),
    ('view_audit_logs', 'View Audit Logs', 'Platform'),
]

MODULES = [
    # (key, label, description)
    ('business', 'Businesses', 'Business directory listings (shops, restaurants, hospitals, education, transport).'),
    ('property', 'Properties', 'Real estate / property listings.'),
    ('job', 'Jobs', 'Job board listings.'),
    ('event', 'Events', 'Local events listings.'),
    ('news', 'News', 'News posts.'),
    ('project', 'Projects', 'Government/community project listings.'),
    ('reviews', 'Reviews & Ratings', 'Star ratings, written reviews, and comments on listings.'),
    ('push_notifications', 'Push Notifications', 'Browser push notifications for admins and users.'),
]


def seed(apps, schema_editor):
    Permission = apps.get_model('core', 'Permission')
    PlatformModule = apps.get_model('core', 'PlatformModule')

    for key, label, group in PERMISSIONS:
        Permission.objects.get_or_create(key=key, defaults={'label': label, 'group': group})

    for key, label, description in MODULES:
        PlatformModule.objects.get_or_create(key=key, defaults={'label': label, 'description': description})


def unseed(apps, schema_editor):
    Permission = apps.get_model('core', 'Permission')
    PlatformModule = apps.get_model('core', 'PlatformModule')
    Permission.objects.filter(key__in=[p[0] for p in PERMISSIONS]).delete()
    PlatformModule.objects.filter(key__in=[m[0] for m in MODULES]).delete()


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0027_permission_alter_profile_role_auditlog_and_more'),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]
