# Backfills city=NULL on Business/Property rows to Kuppam. These predate the
# multi-city Location system and, since _public_qs() (see views.py) scopes
# every public page to the visitor's selected city, a NULL city means the
# listing is invisible on every city-specific page (including Kuppam's own
# homepage/hero stats) — only ever showing up in the unscoped "all cities"
# view. Kuppam is the platform's original city, so a NULL city here almost
# certainly means "this predates city selection, and belongs to Kuppam."
from django.db import migrations


def backfill(apps, schema_editor):
    Location = apps.get_model('core', 'Location')
    kuppam = Location.objects.filter(slug='city-kuppam').first()
    if kuppam is None:
        return
    Business = apps.get_model('core', 'Business')
    Property = apps.get_model('core', 'Property')
    Business.objects.filter(city=None).update(city=kuppam)
    Property.objects.filter(city=None).update(city=kuppam)


def unbackfill(apps, schema_editor):
    # Not reversible: once merged into "city=Kuppam", the original NULL rows
    # can't be distinguished from listings genuinely created for Kuppam.
    pass


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0037_job_job_type_job_shift_date_job_shift_end_time_and_more'),
    ]

    operations = [
        migrations.RunPython(backfill, unbackfill),
    ]
