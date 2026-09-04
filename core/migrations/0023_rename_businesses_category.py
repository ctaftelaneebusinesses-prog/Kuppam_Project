from django.db import migrations


def rename_forward(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.filter(key='business').update(label='Nearby Shops')


def rename_backward(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.filter(key='business').update(label='Businesses')


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0022_rename_real_estate_category'),
    ]

    operations = [
        migrations.RunPython(rename_forward, rename_backward),
    ]
