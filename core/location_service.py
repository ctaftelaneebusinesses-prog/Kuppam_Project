"""Location resolution and request-scoped active-city helpers."""

import json
from urllib.parse import urlencode
from urllib.request import Request, urlopen

from django.core.exceptions import ValidationError
from django.core.cache import cache

from .models import Location

SESSION_LOCATION_KEY = 'onetowncity_location'


def serialize_location(location, source='saved_preference'):
    return {
        'cityId': location.pk,
        'slug': location.slug,
        'city': location.name,
        'district': location.district,
        'state': location.state,
        'country': 'India',
        'countryCode': 'IN',
        'latitude': float(location.latitude) if location.latitude is not None else None,
        'longitude': float(location.longitude) if location.longitude is not None else None,
        'source': source,
    }


def active_location(request):
    """Return the selected canonical city, or None for an unscoped visitor."""
    if hasattr(request, '_onetowncity_active_location'):
        return request._onetowncity_active_location
    saved = request.session.get(SESSION_LOCATION_KEY)
    if not isinstance(saved, dict) or not saved.get('cityId'):
        request._onetowncity_active_location = None
        return None
    request._onetowncity_active_location = Location.objects.filter(
        pk=saved['cityId'], kind=Location.Kind.CITY, is_active=True,
    ).select_related('parent', 'parent__parent').first()
    return request._onetowncity_active_location


def save_location(request, location, source='manual_selection'):
    data = serialize_location(location, source=source)
    request.session[SESSION_LOCATION_KEY] = data
    request.session.modified = True
    return data


def search_cities(query, limit=20):
    query = (query or '').strip()[:100]
    cache_key = f'onetowncity:locations:{query.casefold()}:{limit}'
    cached = cache.get(cache_key)
    if cached is not None:
        return cached
    cities = Location.objects.filter(kind=Location.Kind.CITY, is_active=True).select_related('parent', 'parent__parent')
    if query:
        cities = cities.filter(name__icontains=query) | cities.filter(aliases__icontains=query)
    result = list(cities.order_by('name')[:limit])
    cache.set(cache_key, result, 300)
    return result


def reverse_geocode(latitude, longitude):
    """Resolve coordinates through Nominatim and return an India-only payload."""
    params = urlencode({'lat': latitude, 'lon': longitude, 'format': 'jsonv2', 'addressdetails': 1})
    request = Request(
        f'https://nominatim.openstreetmap.org/reverse?{params}',
        headers={'User-Agent': 'OneTownCity/1.0 location resolution'},
    )
    with urlopen(request, timeout=8) as response:
        payload = json.load(response)
    address = payload.get('address') or {}
    country_code = (address.get('country_code') or '').upper()
    if country_code != 'IN':
        raise ValidationError('OneTownCity is currently available across India.')
    city_name = address.get('city') or address.get('town') or address.get('municipality') or address.get('village')
    if not city_name:
        raise ValidationError('We could not determine a city from that location.')
    city = next(iter(search_cities(city_name, limit=1)), None)
    if city is None:
        raise ValidationError('This location is not supported yet. Please choose a city.')
    result = serialize_location(city, source='geolocation')
    result.update({
        'latitude': float(latitude),
        'longitude': float(longitude),
        'district': address.get('state_district') or city.district,
        'state': address.get('state') or city.state,
    })
    return result
