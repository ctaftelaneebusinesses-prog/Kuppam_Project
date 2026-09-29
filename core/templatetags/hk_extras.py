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


@lru_cache(maxsize=256)
def _webp_width_variant(static_path, width):
    path = PurePosixPath(static_path)
    candidate = str(path.with_name(f'{path.stem}-{width}.webp'))
    return candidate if finders.find(candidate) else ''


@register.filter
def webp_width(static_path, width):
    """
    The static path of a pre-resized WebP of a stock photo at `width` pixels
    (images/services/news.jpg|webp_width:640 -> images/services/news-640.webp),
    or '' when there isn't one. The originals are 1536px wide but shown as
    ~300px cards and ~1300px banners; these variants are what srcset serves.
    """
    if not static_path:
        return ''
    return _webp_width_variant(str(static_path), int(width))


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


@register.filter
def inr_short(amount):
    """
    A rupee amount in the compact Indian style buyers actually read listings
    in: 12500000 -> '1.25 Cr', 1500000 -> '15 Lakh', 18000 -> '18,000'.
    Trailing zeros are dropped (1.50 Cr -> 1.5 Cr, 15.00 Lakh -> 15 Lakh).
    """
    try:
        value = float(amount)
    except (TypeError, ValueError):
        return ''
    for unit_value, unit in ((10_000_000, 'Cr'), (100_000, 'Lakh')):
        if value >= unit_value:
            number = f'{value / unit_value:.2f}'.rstrip('0').rstrip('.')
            return f'{number} {unit}'
    return f'{value:,.0f}'


#: Size of the .hk-tint-N palette in main.css.
TINT_COUNT = 8


def tint_index(model, field_name, value):
    """
    Palette slot (0..TINT_COUNT-1) for one value of a model's choice field:
    its position in the field's choices, so a type keeps the same color on
    its filter tile and on every card, on every page. Unknown values get 0.
    """
    keys = [key for key, _ in model._meta.get_field(field_name).flatchoices]
    return (keys.index(value) if value in keys else 0) % TINT_COUNT


@register.filter
def choice_tint(obj, field_name):
    """{{ property|choice_tint:'property_type' }} -> 3 (see tint_index)."""
    return tint_index(type(obj), field_name, getattr(obj, field_name, None))
