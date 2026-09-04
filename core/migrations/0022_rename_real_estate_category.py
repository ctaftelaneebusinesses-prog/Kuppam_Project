from django.db import migrations


def rename_forward(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.filter(key='property').update(label='Property Listing')


def rename_backward(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.filter(key='property').update(label='Real Estate')


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0021_remove_sitesettings'),
    ]

    operations = [
        migrations.RunPython(rename_forward, rename_backward),
    ]
