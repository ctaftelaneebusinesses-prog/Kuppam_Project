from django.db import migrations


LOCATIONS = {
    'Andhra Pradesh': {
        'districts': {
            'Chittoor': ['Kuppam'],
            'Visakhapatnam': ['Visakhapatnam'],
            'NTR': ['Vijayawada'],
            'SPSR Nellore': ['Nellore'],
            'Tirupati': ['Tirupati'],
        },
    },
    'Telangana': {'districts': {'Hyderabad': ['Hyderabad']}},
    'Karnataka': {'districts': {'Bengaluru Urban': ['Bengaluru']}},
    'Tamil Nadu': {'districts': {'Chennai': ['Chennai']}},
    'Maharashtra': {'districts': {'Mumbai City': ['Mumbai'], 'Pune': ['Pune']}},
    'Delhi': {'districts': {'New Delhi': ['Delhi']}},
    'Kerala': {'districts': {'Ernakulam': ['Kochi']}},
    'West Bengal': {'districts': {'Kolkata': ['Kolkata']}},
    'Rajasthan': {'districts': {'Jaipur': ['Jaipur']}},
    'Gujarat': {'districts': {'Ahmedabad': ['Ahmedabad']}},
}


ALIASES = {
    'Bengaluru': ['Bangalore'],
    'Visakhapatnam': ['Vizag'],
}


def seed_locations(apps, schema_editor):
    Location = apps.get_model('core', 'Location')
    Business = apps.get_model('core', 'Business')
    Property = apps.get_model('core', 'Property')
    Job = apps.get_model('core', 'Job')
    Event = apps.get_model('core', 'Event')
    News = apps.get_model('core', 'News')
    Project = apps.get_model('core', 'Project')

    country, _ = Location.objects.get_or_create(
        kind='country', name='India', slug='india',
        defaults={'country_code': 'IN'},
    )
    city_rows = {}
    for state_name, state_data in LOCATIONS.items():
        state, _ = Location.objects.get_or_create(
            kind='state', name=state_name, slug=state_name.lower().replace(' ', '-'),
            defaults={'parent': country, 'country_code': 'IN'},
        )
        for district_name, city_names in state_data['districts'].items():
            district, _ = Location.objects.get_or_create(
                kind='district', name=district_name,
                slug=f'{state.slug}-{district_name.lower().replace(" ", "-")}',
                defaults={'parent': state, 'country_code': 'IN'},
            )
            for city_name in city_names:
                city, _ = Location.objects.get_or_create(
                    kind='city', name=city_name,
                    slug=f'city-{city_name.lower().replace(" ", "-")}',
                    defaults={
                        'parent': district,
                        'country_code': 'IN',
                        'aliases': ALIASES.get(city_name, []),
                    },
                )
                city_rows[city_name] = city

    models_and_fields = [
        (Business, ('name', 'address')),
        (Property, ('title', 'location')),
        (Job, ('job_title', 'company', 'location')),
        (Event, ('title', 'location')),
        (News, ('title', 'content')),
        (Project, ('title', 'location')),
    ]
    for model, fields in models_and_fields:
        for city_name, city in city_rows.items():
            from django.db.models import Q
            match = Q()
            for field in fields:
                match |= Q(**{f'{field}__icontains': city_name})
            model.objects.filter(city__isnull=True).filter(match).update(city=city)


def remove_locations(apps, schema_editor):
    Location = apps.get_model('core', 'Location')
    Location.objects.all().delete()


class Migration(migrations.Migration):
    dependencies = [('core', '0021_location_business_city_event_city_job_city_news_city_and_more')]
    operations = [migrations.RunPython(seed_locations, remove_locations)]
