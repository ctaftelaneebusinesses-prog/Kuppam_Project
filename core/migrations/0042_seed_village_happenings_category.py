# Adds "What's Happening in Your Village" as a top-level Category (same
# listing_model='news' shape as the existing "OneTownCity News" category —
# it reuses the News model exactly, just tagged separately) so Content
# Providers can select it and submit local happenings/updates under it, and
# so the homepage's existing "Today in Your Town" section (see home()'s
# news_today/recent_news_fallback queries) has a dedicated category to
# scope itself to instead of blending in every News article site-wide.
from django.db import migrations


def seed(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.get_or_create(
        key='village-happenings',
        defaults=dict(
            label="What's Happening in Your Village", parent=None, listing_model='news',
            icon='bi-broadcast', image='images/services/news.jpg',
            description='Local happenings, announcements, and everyday updates from your village or neighborhood.',
            order=13, is_active=True,
        ),
    )


def unseed(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    Category.objects.filter(key='village-happenings').delete()


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0041_add_location_and_working_hours_fields'),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]
