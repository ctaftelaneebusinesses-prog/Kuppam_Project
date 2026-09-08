# Adds "Scholarships & Government Schemes" and "Lost & Found" as real,
# top-level Category rows — same treatment as every other top-level student
# category (see 0034_seed_repair_tourism_categories and
# 0044_seed_student_categories). Unlike those, these two aren't Business
# subcategories: Scholarship and LostFound (0045) are standalone listing
# types of their own, so listing_model points straight at them with no
# business_subcategory value to pin.
from django.db import migrations

CATEGORIES = [
    dict(
        key='scholarships', label='Scholarships & Government Schemes',
        listing_model='scholarship', icon='bi-mortarboard-fill',
        image='images/services/education.jpg',
        description='Find scholarships and government schemes for students, verified and kept up to date.',
        order=17,
    ),
    dict(
        key='lost-found', label='Lost & Found',
        listing_model='lostfound', icon='bi-search-heart',
        image='images/services/business.jpg',
        description='Report or search for lost and found items in your city.',
        order=18,
    ),
]


def seed(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    for entry in CATEGORIES:
        Category.objects.get_or_create(
            key=entry['key'],
            defaults=dict(
                label=entry['label'], parent=None, listing_model=entry['listing_model'],
                icon=entry['icon'], image=entry['image'], description=entry['description'],
                order=entry['order'], is_active=True,
            ),
        )


def unseed(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.filter(key__in=[entry['key'] for entry in CATEGORIES]).delete()


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0045_add_scholarship_lostfound_models'),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]
