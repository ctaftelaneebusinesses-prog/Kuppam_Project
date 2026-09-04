from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0020_alter_notification_type'),
    ]

    operations = [
        migrations.DeleteModel(
            name='SiteSettings',
        ),
    ]
