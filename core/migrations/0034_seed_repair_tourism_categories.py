# Adds "Repair Services" and "Places to Visit" as real Category rows (children
# of the "Businesses" top-level category, same shape as automobile/grocery/
# etc. from 0017_restructure_category_hierarchy). Business.CATEGORY_CHOICES
# already has 'repair'/'tourism' values and the general Businesses page/form
# already accept them — but without a Category row here, there was nothing
# for a City Admin to grant a Content Provider access to, and no dashboard
# "+ Add" quick-tile for either.
from django.db import migrations


def seed(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    businesses = Category.objects.filter(key='business').first()
    if businesses is None:
        return
    Category.objects.get_or_create(
        key='repair',
        defaults=dict(
            label='Repair Services', parent=businesses, listing_model='business',
            business_subcategory='repair', icon='bi-wrench-adjustable', order=11, is_active=True,
        ),
    )
    Category.objects.get_or_create(
        key='tourism',
        defaults=dict(
            label='Places to Visit', parent=businesses, listing_model='business',
            business_subcategory='tourism', icon='bi-binoculars', order=12, is_active=True,
        ),
    )


def unseed(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.filter(key__in=['repair', 'tourism']).delete()


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0033_business_latitude_business_longitude_and_more'),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]
