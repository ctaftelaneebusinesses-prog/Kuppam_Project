from functools import lru_cache
from pathlib import PurePosixPath
from urllib.parse import quote

from django import template
from django.contrib.staticfiles import finders
from django.utils.translation import get_language

from core.i18n_utils import translate_field as _translate_field
from core.i18n_utils import translate_text

register = template.Library()


@lru_cache(maxsize=256)
def _webp_sibling(static_path):
    candidate = str(PurePosixPath(static_path).with_suffix('.webp'))
    return candidate if candidate != static_path and finders.find(candidate) else ''


@register.filter
def webp_sibling(static_path):
    """
    The static path of a pre-generated .webp twin of a .jpg/.png static
    image (e.g. images/services/news.jpg -> images/services/news.webp), or ''
    when none exists. Lets templates offer the ~55% smaller WebP to browsers
    that support it while keeping the original as the fallback — without
    changing the image paths stored on Category rows or in CATEGORIES.
    """
    if not static_path:
        return ''
    return _webp_sibling(str(static_path))


@register.filter
def maps_search_url(location_text):
    """Google Maps search link built from a free-text address/location string."""
    if not location_text:
        return ''
    return 'https://www.google.com/maps/search/?api=1&query=' + quote(str(location_text))


@register.filter
def dyntrans(text):
    """
    Machine-translates an arbitrary short string (category labels, section
    headings, etc.) into the current request's active language. See
    core/i18n_utils.py — falls back to the original text on any failure.
    """
    return translate_text(text, get_language())


@register.filter
def translate_field(obj, field_name):
    """
    Machine-translates one field of a listing (Business/Property/Job/Event/
    News/Project instance) into the current active language, caching the
    result in TranslationCache so it's translated at most once. Use on
    listing detail/card templates: {{ business|translate_field:"description" }}.
    """
    return _translate_field(obj, field_name, get_language())


@register.filter
def has_permission(profile, key):
    """
    Django templates can't call a method with an argument directly, so this
    filter is how nav/section visibility checks reach Profile.has_permission()
    for a specific permission key, e.g.
    {% if request.profile|has_permission:"view_content_providers" %}.
    """
    return bool(profile) and profile.has_permission(key)
