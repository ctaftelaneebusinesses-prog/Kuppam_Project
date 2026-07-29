from django.db import migrations


def seed_project_category(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.get_or_create(
        key='project',
        defaults=dict(
            label='Upcoming Projects',
            listing_model='project',
            business_subcategory='',
            icon='bi-cone-striped',
            order=13,
            is_active=True,
        ),
    )


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0011_alter_category_listing_model_and_more'),
    ]

    operations = [
        migrations.RunPython(seed_project_category, migrations.RunPython.noop),
    ]
