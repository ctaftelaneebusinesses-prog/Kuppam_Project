# Adds "Tuition & Coaching Centers", "Student Services", and "Buy / Sell /
# Exchange" as real, top-level Category rows — same treatment as Repair
# Services / Places to Visit in 0034_seed_repair_tourism_categories +
# 0035_repair_tourism_top_level_categories. Business.CATEGORY_CHOICES
# already has 'tuition_center'/'student_services'/'marketplace' values (see
# 0041_alter_business_category), and the general Businesses page/submission
# form already accept them — but without a Category row here, there is no
# dedicated directory page, no nav/homepage entry, nothing for a City Admin
# to grant a Content Provider access to, and no "+ Add" quick-tile on the
# dashboard for any of the three.
#
# See core/views.py's DIRECTORY_CATEGORIES/CATEGORIES/SEARCH_CATEGORY_
# REDIRECT and core/models.py's Category._BUSINESS_DIRECTORY_KEYS/
# _BUSINESS_DIRECTORY_URL_NAMES for the matching non-migration half of this
# change — a Category row alone does not create the dedicated page; those
# dicts do.
from django.db import migrations

CATEGORIES = [
    dict(
        key='tuition_center', label='Tuition & Coaching Centers',
        business_subcategory='tuition_center', icon='bi-book-half',
        image='images/services/education.jpg',
        description='Find tuition centers, coaching institutes, and academic support near you.',
        order=14,
    ),
    dict(
        key='student_services', label='Student Services',
        business_subcategory='student_services', icon='bi-life-preserver',
        image='images/services/business.jpg',
        description='Local services useful to students — hostels, printing, courier, and more.',
        order=15,
    ),
    dict(
        key='marketplace', label='Buy / Sell / Exchange',
        business_subcategory='marketplace', icon='bi-arrow-left-right',
        image='images/services/shops.jpg',
        description='Shops and outlets for buying, selling, or exchanging used goods.',
        order=16,
    ),
]


def seed(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    for entry in CATEGORIES:
        Category.objects.get_or_create(
            key=entry['key'],
            defaults=dict(
                label=entry['label'], parent=None, listing_model='business',
                business_subcategory=entry['business_subcategory'], icon=entry['icon'],
                image=entry['image'], description=entry['description'],
                order=entry['order'], is_active=True,
            ),
        )


def unseed(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.filter(key__in=[entry['key'] for entry in CATEGORIES]).delete()


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0043_merge_20260908_1246'),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]
