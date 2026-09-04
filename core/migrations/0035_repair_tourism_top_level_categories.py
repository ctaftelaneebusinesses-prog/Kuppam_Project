# Promotes "Repair Services" and "Places to Visit" (seeded as children of
# "Businesses" in 0034) to top-level categories — same treatment as
# Restaurants/Hospitals/Education/Transport in 0017_restructure_category_
# hierarchy: their own homepage tile (image/description) and their own
# dedicated directory page (see DIRECTORY_CATEGORIES/repair_list/
# places_to_visit_list in views.py), fully excluded from the general
# "Businesses" page/permission bucket instead of being one of its filter
# chips.
from django.db import migrations


def promote(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    repair = Category.objects.filter(key='repair').first()
    if repair is not None:
        repair.parent = None
        repair.image = 'images/services/business.jpg'
        repair.description = 'Find electronics, appliance, mobile, and automobile repair shops near you.'
        repair.order = 11
        repair.save()
    tourism = Category.objects.filter(key='tourism').first()
    if tourism is not None:
        tourism.parent = None
        tourism.image = 'images/services/business.jpg'
        tourism.description = 'Discover parks, temples, monuments, and local attractions worth visiting.'
        tourism.order = 12
        tourism.save()


def demote(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    businesses = Category.objects.filter(key='business').first()
    Category.objects.filter(key__in=['repair', 'tourism']).update(
        parent=businesses, image='', description='',
    )


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0034_seed_repair_tourism_categories'),
    ]

    operations = [
        migrations.RunPython(promote, demote),
    ]
