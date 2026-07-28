from urllib.parse import quote

from django import template

register = template.Library()


@register.filter
def maps_search_url(location_text):
    """Google Maps search link built from a free-text address/location string."""
    if not location_text:
        return ''
    return 'https://www.google.com/maps/search/?api=1&query=' + quote(str(location_text))
