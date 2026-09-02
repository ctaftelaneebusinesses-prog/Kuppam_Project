# Seeds "Regular / Full-Time" and "Hourly Basis" as real Category children
# under "Jobs" (key='job') — same shape as the Real Estate / Upcoming
# Projects subcategories in 0017_restructure_category_hierarchy (e.g. "For
# Sale"/"Planned"), now that Job.job_type exists (see the job_type/shift_*
# fields added to Job) and Category._LISTING_COUNT_MAP / views.py's
# SUBCATEGORY_INITIAL_FIELDS know how to count/pre-fill against it. Without
# this, Job.job_type existed as a model field with no matching Category rows
# — Manage Categories kept showing "No subcategories yet" under Jobs even
# though Regular/Hourly Basis is a real, filterable distinction on the
# public Jobs page.
from django.db import migrations


def seed(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    jobs = Category.objects.filter(key='job').first()
    if jobs is None:
        return
    Category.objects.get_or_create(
        key='job-regular',
        defaults=dict(
            label='Regular / Full-Time', parent=jobs, listing_model='job',
            business_subcategory='regular', icon='bi-briefcase', order=1, is_active=True,
        ),
    )
    Category.objects.get_or_create(
        key='job-hourly',
        defaults=dict(
            label='Hourly Basis', parent=jobs, listing_model='job',
            business_subcategory='hourly', icon='bi-clock-history', order=2, is_active=True,
        ),
    )


def unseed(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.filter(key__in=['job-regular', 'job-hourly']).delete()


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0039_business_working_hours_alter_business_website'),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]
