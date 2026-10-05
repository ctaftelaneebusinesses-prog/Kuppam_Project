# Folds "Tuition & Coaching Centers" and "Scholarships & Government Schemes"
# into "Student Services" as its sub-categories, so the three show up as one
# category in the header/footer menus and on the homepage (which only list
# top-level categories). Both stay active, so Content Providers still pick
# them as subcategories of Student Services when submitting.
from django.db import migrations

CHILD_KEYS = ('tuition_center', 'scholarships')

DESCRIPTION = (
    'Tuition and coaching centers, scholarships and government schemes, '
    'hostels, printing and other services for students.'
)
OLD_DESCRIPTION = 'Local services useful to students — hostels, printing, courier, and more.'


def forwards(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    parent = Category.objects.filter(key='student_services').first()
    if parent is None:
        return
    Category.objects.filter(key__in=CHILD_KEYS, parent=None).update(parent=parent)
    parent.description = DESCRIPTION
    parent.image = 'images/services/student-services.jpg'
    parent.save(update_fields=['description', 'image'])


def backwards(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.filter(key__in=CHILD_KEYS, parent__key='student_services').update(parent=None)
    Category.objects.filter(key='student_services', description=DESCRIPTION).update(description=OLD_DESCRIPTION)


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0051_marketplace_category_photo'),
    ]

    operations = [
        migrations.RunPython(forwards, backwards),
    ]
