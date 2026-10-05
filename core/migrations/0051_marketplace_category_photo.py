# Buy / Sell / Exchange borrowed the Nearby Shops storefront photo (a branded
# clothing store) since 0044_seed_student_categories.py. Point it at its own
# weekly-market photo — only when it still has the borrowed one, so an image
# an admin picked in the meantime is left alone.
from django.db import migrations


def forwards(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.filter(key='marketplace', image='images/services/shops.jpg').update(
        image='images/services/marketplace.jpg',
    )


def backwards(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.filter(key='marketplace', image='images/services/marketplace.jpg').update(
        image='images/services/shops.jpg',
    )


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0050_profile_consent'),
    ]

    operations = [
        migrations.RunPython(forwards, backwards),
    ]
