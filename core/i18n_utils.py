"""
Machine translation for dynamic, user-submitted content — business
descriptions, job posts, category labels, etc. — so switching the site
language translates *everything visible*, not just the static UI chrome
(which is handled separately by Django's own {% trans %}/.po pipeline).

Uses deep-translator's free, unofficial Google Translate endpoint (no API
key). Every call is wrapped in a broad try/except: a translation failure
must never break a page, it should just fall back to the original text.
"""
import hashlib
import logging
import socket

from django.core.cache import cache

logger = logging.getLogger(__name__)

#: deep-translator's GoogleTranslator doesn't expose a request timeout, so a
#: slow/unresponsive translate.google.com could otherwise hang a page load
#: indefinitely (observed in testing: a bare socket read with no timeout).
#: Applied only around the translation call itself (see _translate_via_google)
#: so it doesn't affect unrelated network calls elsewhere in the process
#: (Supabase auth, email, etc.).
TRANSLATE_SOCKET_TIMEOUT_SECONDS = 5

#: Matches settings.LANGUAGES minus English (nothing to translate into itself).
TRANSLATABLE_LANGUAGES = {'te', 'hi', 'ta'}

CACHE_TTL_SECONDS = 60 * 60 * 24 * 30  # 30 days — static-ish UI/category strings


def translate_text(text, language):
    """
    Translates `text` into `language` (e.g. 'te'), using a generic
    process-wide cache keyed by content+language so any given string is
    only ever sent to the translator once. Returns `text` unchanged if the
    language isn't one we translate into, the text is empty, or translation
    fails for any reason.
    """
    if not text or language not in TRANSLATABLE_LANGUAGES:
        return text

    cache_key = 'dyntrans:' + language + ':' + hashlib.sha1(text.encode('utf-8')).hexdigest()
    cached = cache.get(cache_key)
    if cached is not None:
        return cached

    translated = _translate_via_google(text, language)
    cache.set(cache_key, translated, CACHE_TTL_SECONDS)
    return translated


def _translate_via_google(text, language):
    previous_timeout = socket.getdefaulttimeout()
    try:
        from deep_translator import GoogleTranslator
        socket.setdefaulttimeout(TRANSLATE_SOCKET_TIMEOUT_SECONDS)
        result = GoogleTranslator(source='auto', target=language).translate(text)
        return result or text
    except Exception:
        logger.warning('Dynamic translation failed for language=%s', language, exc_info=True)
        return text
    finally:
        socket.setdefaulttimeout(previous_timeout)


def translate_field(obj, field_name, language):
    """
    Translates one field of one listing (Business/Property/Job/Event/News/
    Project instance), persisting the result in TranslationCache so it's
    translated at most once per object+field+language — used by the
    `translate_field` template filter on listing detail/card templates.
    """
    text = getattr(obj, field_name, '') or ''
    if not text or language not in TRANSLATABLE_LANGUAGES:
        return text

    from django.contrib.contenttypes.models import ContentType
    from .models import TranslationCache

    content_type = ContentType.objects.get_for_model(obj)
    existing = TranslationCache.objects.filter(
        content_type=content_type, object_id=obj.pk, field_name=field_name, language=language,
    ).first()
    if existing is not None and existing.source_text == text:
        return existing.translated_text

    translated = _translate_via_google(text, language)
    TranslationCache.objects.update_or_create(
        content_type=content_type, object_id=obj.pk, field_name=field_name, language=language,
        defaults={'source_text': text, 'translated_text': translated},
    )
    return translated
