from django.db import migrations

CITY_OPERATIONS_PERMISSIONS = [
    # (key, label, group)
    ('manage_sub_admins', 'Manage Sub Admins', 'City Operations'),
    ('manage_content_providers', 'Manage Content Providers', 'City Operations'),
    ('review_content', 'Review Submitted Content', 'City Operations'),
    ('approve_content', 'Approve Content', 'City Operations'),
    ('reject_content', 'Reject Content', 'City Operations'),
    ('request_content_changes', 'Request Content Changes', 'City Operations'),
    ('manage_city_content', 'Manage City-Specific Content', 'City Operations'),
    ('view_city_analytics', 'View City-Specific Analytics', 'City Operations'),
    ('manage_city_modules', 'Manage Allowed City Modules', 'City Operations'),
]

# All of these are on by default for City Admin — this is what "City Admin
# can: ..." means concretely now that the RolePermission matrix is real.
CITY_ADMIN_DEFAULT_KEYS = [key for key, _, _ in CITY_OPERATIONS_PERMISSIONS]


def seed(apps, schema_editor):
    Permission = apps.get_model('core', 'Permission')
    RolePermission = apps.get_model('core', 'RolePermission')

    for key, label, group in CITY_OPERATIONS_PERMISSIONS:
        Permission.objects.get_or_create(key=key, defaults={'label': label, 'group': group})

    for key in CITY_ADMIN_DEFAULT_KEYS:
        permission = Permission.objects.get(key=key)
        RolePermission.objects.update_or_create(
            role='city_admin', permission=permission, defaults={'is_granted': True},
        )


def unseed(apps, schema_editor):
    Permission = apps.get_model('core', 'Permission')
    RolePermission = apps.get_model('core', 'RolePermission')
    keys = [key for key, _, _ in CITY_OPERATIONS_PERMISSIONS]
    RolePermission.objects.filter(role='city_admin', permission__key__in=keys).delete()
    Permission.objects.filter(key__in=keys).delete()


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0029_citymodule_userpermission'),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]
