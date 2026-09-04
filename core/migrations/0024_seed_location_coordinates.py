from django.db import migrations


CITY_COORDINATES = {
    'Kuppam': (12.750000, 78.230000),
    'Visakhapatnam': (17.686800, 83.218500),
    'Vijayawada': (16.506200, 80.648000),
    'Nellore': (14.442600, 79.986500),
    'Tirupati': (13.628800, 79.419200),
    'Hyderabad': (17.385000, 78.486700),
    'Bengaluru': (12.971600, 77.594600),
    'Chennai': (13.082700, 80.270700),
    'Mumbai': (19.076000, 72.877700),
    'Pune': (18.520400, 73.856700),
    'Delhi': (28.613900, 77.209000),
    'Kochi': (9.931200, 76.267300),
    'Kolkata': (22.572600, 88.363900),
    'Jaipur': (26.912400, 75.787300),
    'Ahmedabad': (23.022500, 72.571400),
}


def seed_coordinates(apps, schema_editor):
    Location = apps.get_model('core', 'Location')
    for city_name, coordinates in CITY_COORDINATES.items():
        Location.objects.filter(kind='city', name=city_name).update(
            latitude=coordinates[0], longitude=coordinates[1],
        )


class Migration(migrations.Migration):
    dependencies = [('core', '0023_location_latitude_location_longitude')]
    operations = [migrations.RunPython(seed_coordinates, migrations.RunPython.noop)]
