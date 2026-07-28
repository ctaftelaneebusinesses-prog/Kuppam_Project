from django.db import migrations
from django.utils.text import slugify

CATEGORY_SEED = [
    # key,        label,                 listing_model, business_subcategory, icon,               order
    ('business',  'Business',            'business',    '',            'bi-building',       1),
    ('shop',      'Shop',                'business',    'retail',      'bi-shop',           2),
    ('restaurant','Restaurant',          'business',    'restaurant',  'bi-cup-hot',        3),
    ('school',    'School',              'business',    'school',      'bi-mortarboard',    4),
    ('college',   'College',             'business',    'college',     'bi-mortarboard',    5),
    ('hospital',  'Hospital',            'business',    'hospital',    'bi-hospital',       6),
    ('property',  'Property',            'property',    '',            'bi-house-door',     7),
    ('job',       'Job',                 'job',         '',            'bi-briefcase',      8),
    ('event',     'Event',               'event',       '',            'bi-calendar-event', 9),
    ('news',      'Local News',          'news',        '',            'bi-newspaper',      10),
    ('transport', 'Transport Service',   'business',    'transport',   'bi-bus-front',      11),
    ('other',     'Other',               'business',    'other',       'bi-three-dots',     12),
]

# (model_name, [source fields to join for the slug])
SLUG_SOURCES = {
    'Business': ['name'],
    'Property': ['title'],
    'Job': ['job_title', 'company'],
    'Event': ['title'],
    'News': ['title'],
}


def seed_categories(apps, schema_editor):
    Category = apps.get_model('core', 'Category')
    for key, label, listing_model, business_subcategory, icon, order in CATEGORY_SEED:
        Category.objects.get_or_create(
            key=key,
            defaults=dict(
                label=label,
                listing_model=listing_model,
                business_subcategory=business_subcategory,
                icon=icon,
                order=order,
                is_active=True,
            ),
        )


def backfill_slugs(apps, schema_editor):
    for model_name, source_fields in SLUG_SOURCES.items():
        Model = apps.get_model('core', model_name)
        seen = set(Model.objects.exclude(slug=None).exclude(slug='').values_list('slug', flat=True))
        for obj in Model.objects.filter(slug__isnull=True).order_by('pk'):
            text = ' '.join(str(getattr(obj, f, '') or '') for f in source_fields).strip()
            base = slugify(text)[:200] or f'{model_name.lower()}-{obj.pk}'
            slug = base
            n = 1
            while slug in seen:
                n += 1
                slug = f'{base}-{n}'
            seen.add(slug)
            obj.slug = slug
            obj.save(update_fields=['slug'])


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0006_category_business_avg_rating_business_comment_count_and_more'),
    ]

    operations = [
        migrations.RunPython(seed_categories, migrations.RunPython.noop),
        migrations.RunPython(backfill_slugs, migrations.RunPython.noop),
    ]
