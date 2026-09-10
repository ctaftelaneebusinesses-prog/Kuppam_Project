"""
Django settings for hello_kuppam project.
"""

import logging
import os
import sys
from pathlib import Path
from django.core.exceptions import ImproperlyConfigured
from dotenv import load_dotenv

load_dotenv()

BASE_DIR = Path(__file__).resolve().parent.parent

# ------------------------------------------------------------------
# SECURITY
# ------------------------------------------------------------------
# Phase 6 hardening: DEBUG used to default to 'True' and SECRET_KEY to a
# well-known insecure literal — meaning a production deploy that simply
# forgot to set either env var would silently run in DEBUG mode with a
# guessable key, rather than failing to start. DEBUG now defaults to the
# safe value (False); a *local* dev environment still works exactly as
# before since .env already sets DEBUG=True and a real SECRET_KEY
# explicitly (see .env.example) — this only changes behavior when both are
# left unset, which today means "quietly insecure" and now means "refuses
# to start with a clear error" instead.
DEBUG = os.getenv('DEBUG', 'False') == 'True'

SECRET_KEY = os.getenv('SECRET_KEY', '')
if not SECRET_KEY:
    if DEBUG:
        # Local-only fallback so a fresh clone with DEBUG=True but no
        # SECRET_KEY yet can still run manage.py runserver — unmistakably
        # not a value that could pass for a real production key.
        SECRET_KEY = 'django-insecure-local-dev-only-DO-NOT-USE-IN-PRODUCTION'
    else:
        raise ImproperlyConfigured(
            'SECRET_KEY environment variable is required when DEBUG is not "True". '
            'Set it in the environment before starting the server — see .env.example.'
        )

ALLOWED_HOSTS = os.getenv('ALLOWED_HOSTS', '127.0.0.1,localhost').split(',')

# Comma-separated list of scheme-qualified origins allowed to submit
# cross-site POSTs, e.g. "https://hellokuppam.com,https://www.hellokuppam.com".
# Required in production once the app has a real domain.
CSRF_TRUSTED_ORIGINS = [o for o in os.getenv('CSRF_TRUSTED_ORIGINS', '').split(',') if o]

if not DEBUG:
    # Render/Railway/most PaaS terminate HTTPS at their edge and forward
    # plain HTTP internally — this tells Django to trust that header instead
    # of seeing every request as insecure.
    SECURE_PROXY_SSL_HEADER = ('HTTP_X_FORWARDED_PROTO', 'https')
    SESSION_COOKIE_SECURE = True
    CSRF_COOKIE_SECURE = True
    SECURE_SSL_REDIRECT = True
    SECURE_HSTS_SECONDS = 31536000
    SECURE_HSTS_INCLUDE_SUBDOMAINS = True
    SECURE_HSTS_PRELOAD = True

# ------------------------------------------------------------------
# APPLICATION DEFINITION
# ------------------------------------------------------------------
INSTALLED_APPS = [
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'django.contrib.humanize',
    'django.contrib.sitemaps',

    # Third-party
    'rest_framework',

    # Local apps
    'core',
]

MIDDLEWARE = [
    'django.middleware.security.SecurityMiddleware',
    'whitenoise.middleware.WhiteNoiseMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.locale.LocaleMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
    'core.middleware.MaintenanceModeMiddleware',
]

ROOT_URLCONF = 'hello_kuppam.urls'

TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS': [BASE_DIR / 'templates'],
        'APP_DIRS': True,
        'OPTIONS': {
            'context_processors': [
                'django.template.context_processors.debug',
                'django.template.context_processors.request',
                'django.template.context_processors.i18n',
                'django.contrib.auth.context_processors.auth',
                'django.contrib.messages.context_processors.messages',
                'core.context_processors.supabase_config',
                'core.context_processors.location',
                'core.context_processors.push_config',
                'core.context_processors.notifications',
                'core.context_processors.unread_messages',
                'core.context_processors.pending_admin_requests',
                'core.context_processors.category_tree',
            ],
        },
    },
]

WSGI_APPLICATION = 'hello_kuppam.wsgi.application'
ASGI_APPLICATION = 'hello_kuppam.asgi.application'

# ------------------------------------------------------------------
# DATABASE â€” Supabase PostgreSQL
# ------------------------------------------------------------------
import dj_database_url

DATABASES = {
    'default': dj_database_url.parse(
        os.getenv('DATABASE_URL'),
        # Phase C infra confirmation (2026-09-10): the configured DATABASE_URL
        # port was verified as 6543 — Supabase's TRANSACTION-mode pgbouncer
        # pooler, not session mode. Owner-confirmed limits: 15 backend
        # (pooler-to-Postgres) connections on this Supabase Nano project,
        # 200 max client (app-to-pooler) connections through Supavisor.
        # Single Railway instance running 3 gunicorn workers x 6 threads
        # (18 concurrent request slots) is far below both ceilings.
        # conn_max_age=0 is kept for a different, still-solid reason under
        # transaction mode specifically: pgbouncer only assigns a real
        # Postgres server connection to a client for the duration of one
        # transaction, then returns it to the pool — a Django-side
        # "persistent" connection held across requests doesn't map onto that
        # model at all, and would just occupy a transaction-pooler client
        # slot indefinitely without actually reusing a server-side
        # connection the way conn_max_age>0 assumes. Closing the connection
        # after each request is the architecturally correct choice for a
        # transaction pooler, not merely a conservative one.
        conn_max_age=0,
    )
}
# `manage.py test` needs CREATE DATABASE rights on whatever DATABASE_URL
# points at to spin up its throwaway test DB — the Supabase pooler
# connection this project otherwise uses doesn't grant that. Running
# the test suite against a local in-memory SQLite DB instead sidesteps
# that entirely; it's only ever used under the test runner, never for
# a real request, so it doesn't affect production behavior.
if 'test' in sys.argv:
    DATABASES['default'] = {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': ':memory:',
    }
else:
    # Bound the PostgreSQL connection handshake so a stalled network
    # connection fails promptly instead of hanging the request.
    DATABASES['default'].setdefault('OPTIONS', {})['connect_timeout'] = 10
# ------------------------------------------------------------------
# CACHING
# ------------------------------------------------------------------
# Used for the category nav tree and site-theme flags (core/context_processors.py),
# which are otherwise re-queried on every single page view. REDIS_URL is optional —
# add a Redis instance on Railway/Render and set it there to share the cache across
# gunicorn workers; without it, every worker just keeps its own in-memory cache,
# which is still faster than re-hitting Postgres on every request.
REDIS_URL = os.getenv('REDIS_URL', '')

if REDIS_URL:
    CACHES = {
        'default': {
            'BACKEND': 'django_redis.cache.RedisCache',
            'LOCATION': REDIS_URL,
            'OPTIONS': {'CLIENT_CLASS': 'django_redis.client.DefaultClient'},
        }
    }
else:
    CACHES = {
        'default': {
            'BACKEND': 'django.core.cache.backends.locmem.LocMemCache',
        }
    }

# PASSWORD VALIDATION
# ------------------------------------------------------------------
AUTH_PASSWORD_VALIDATORS = [
    {'NAME': 'django.contrib.auth.password_validation.UserAttributeSimilarityValidator'},
    {'NAME': 'django.contrib.auth.password_validation.MinimumLengthValidator'},
    {'NAME': 'django.contrib.auth.password_validation.CommonPasswordValidator'},
    {'NAME': 'django.contrib.auth.password_validation.NumericPasswordValidator'},
]

# ------------------------------------------------------------------
# INTERNATIONALIZATION
# ------------------------------------------------------------------
LANGUAGE_CODE = 'en'
TIME_ZONE = 'Asia/Kolkata'
USE_I18N = True
USE_TZ = True

LANGUAGES = [
    ('en', 'English'),
    ('te', 'Telugu'),
    ('hi', 'Hindi'),
    ('ta', 'Tamil'),
    ('kn', 'Kannada'),
]
LOCALE_PATHS = [BASE_DIR / 'locale']

# ------------------------------------------------------------------
# STATIC & MEDIA FILES
# ------------------------------------------------------------------
STATIC_URL = 'static/'
STATICFILES_DIRS = [BASE_DIR / 'static']
STATIC_ROOT = BASE_DIR / 'staticfiles'

# Whitenoise serves static files directly from the app server in production
# (no separate nginx/CDN needed) with cache-busting hashed filenames + gzip.
STORAGES = {
    'default': {'BACKEND': 'core.storage.SupabaseMediaStorage'},
    'staticfiles': {'BACKEND': 'whitenoise.storage.CompressedManifestStaticFilesStorage'},
}

MEDIA_URL = 'media/'
MEDIA_ROOT = BASE_DIR / 'media'

# Same reasoning as the sqlite DATABASES override above: SupabaseMediaStorage
# makes a real network call to Supabase Storage on every save, which the
# test suite has no credentials for and shouldn't depend on anyway. Local
# disk (Django's default FileSystemStorage) under a throwaway temp dir is
# only ever used under the test runner.
if 'test' in sys.argv:
    import tempfile
    STORAGES['default'] = {'BACKEND': 'django.core.files.storage.FileSystemStorage'}
    MEDIA_ROOT = Path(tempfile.mkdtemp(prefix='onetowncity-test-media-'))

DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'

# ------------------------------------------------------------------
# MESSAGES (map Django's default 'error' tag to Bootstrap's 'danger'
# alert class, used by every messages.error(...) call site-wide)
# ------------------------------------------------------------------
from django.contrib.messages import constants as message_constants
MESSAGE_TAGS = {
    message_constants.ERROR: 'danger',
}

# ------------------------------------------------------------------
# REST API (core.api — the shared contract for the web's own AJAX calls
# and, eventually, the native Android app; see core/api/)
# ------------------------------------------------------------------
REST_FRAMEWORK = {
    'DEFAULT_AUTHENTICATION_CLASSES': [
        # For a client with no shared browser cookie jar (a future native
        # Android screen): Authorization: Bearer <supabase access token>.
        # Listed first deliberately: when every authenticator declines a
        # request, DRF's 401-vs-403 decision asks only the *first*
        # configured authenticator for a WWW-Authenticate header (see
        # rest_framework.views.APIView.get_authenticate_header) — Session
        # Authentication doesn't have one, so an unauthenticated request
        # would otherwise get downgraded to a bare 403. Bearer-token auth
        # does supply one, so anonymous requests correctly get 401.
        'core.api.authentication.SupabaseTokenAuthentication',
        # Lets an already browser-session-authenticated visitor (the
        # existing Supabase-Google-login-mirrored-into-a-Django-session
        # flow, or the legacy staff login) call the API with no extra
        # login step. Enforces CSRF on unsafe methods same as the rest of
        # the site (see core.api.authentication.SessionAuthentication for
        # why this isn't just rest_framework's own SessionAuthentication).
        'core.api.authentication.SessionAuthentication',
    ],
    'DEFAULT_PERMISSION_CLASSES': ['rest_framework.permissions.AllowAny'],
    'DEFAULT_PAGINATION_CLASS': 'core.api.pagination.StandardResultsSetPagination',
    'PAGE_SIZE': 20,
    'DEFAULT_RENDERER_CLASSES': ['rest_framework.renderers.JSONRenderer'],
    'DEFAULT_PARSER_CLASSES': [
        'rest_framework.parsers.JSONParser',
        'rest_framework.parsers.MultiPartParser',
        'rest_framework.parsers.FormParser',
    ],
    'EXCEPTION_HANDLER': 'core.api.exceptions.exception_handler',
    'DATETIME_FORMAT': 'iso-8601',
    'DATE_FORMAT': 'iso-8601',
}
if DEBUG:
    REST_FRAMEWORK['DEFAULT_RENDERER_CLASSES'].append('rest_framework.renderers.BrowsableAPIRenderer')

# ------------------------------------------------------------------
# AUTH REDIRECTS (admin dashboard login)
# ------------------------------------------------------------------
LOGIN_URL = 'core:admin_login'
LOGIN_REDIRECT_URL = '/admin/'
LOGOUT_REDIRECT_URL = 'core:home'

# ------------------------------------------------------------------
# EMAIL (Contact form)
# ------------------------------------------------------------------
# No SMTP/email provider has been chosen yet (checked .env.example — every
# value is blank) — a specific provider/host/credential is deliberately NOT
# invented here. What this does provide is provider-agnostic: every setting
# Django's SMTP backend needs, sourced only from env vars, plus a backend
# default that reacts to whether EMAIL_HOST is actually set instead of
# requiring the owner to separately remember to also flip EMAIL_BACKEND —
# that second, easy-to-forget step is exactly how a "configured" provider
# can still silently keep using the console backend in production.
EMAIL_HOST = os.getenv('EMAIL_HOST', '')
EMAIL_PORT = int(os.getenv('EMAIL_PORT', '587'))
EMAIL_HOST_USER = os.getenv('EMAIL_HOST_USER', '')
EMAIL_HOST_PASSWORD = os.getenv('EMAIL_HOST_PASSWORD', '')
EMAIL_USE_TLS = os.getenv('EMAIL_USE_TLS', 'True') == 'True'
EMAIL_USE_SSL = os.getenv('EMAIL_USE_SSL', 'False') == 'True'
EMAIL_TIMEOUT = int(os.getenv('EMAIL_TIMEOUT', '10'))

# EMAIL_BACKEND is still a full override (e.g. for an API-based, non-SMTP
# provider package) — but its *default* is derived from EMAIL_HOST rather
# than hardcoded to console, so setting EMAIL_HOST/USER/PASSWORD alone is
# enough to switch to real delivery.
EMAIL_BACKEND = os.getenv(
    'EMAIL_BACKEND',
    'django.core.mail.backends.smtp.EmailBackend' if EMAIL_HOST else 'django.core.mail.backends.console.EmailBackend',
)
DEFAULT_FROM_EMAIL = os.getenv('DEFAULT_FROM_EMAIL', 'ctaftelaneebusinesses@gmail.com')
# Used only if Django's own error-to-admins email (ADMINS/LOGGING's
# AdminEmailHandler) is ever wired up — neither is configured in this
# project today, so this has no effect on its own; kept explicit so it
# doesn't fall back to Django's built-in 'root@localhost' default.
SERVER_EMAIL = os.getenv('SERVER_EMAIL', DEFAULT_FROM_EMAIL)
# Owner-approved OneTownCity support address (Phase B, 2026-09-10) — was
# previously defaulting to the old hellokuppam.com domain.
CONTACT_RECEIVER_EMAIL = os.getenv('CONTACT_RECEIVER_EMAIL', 'ctaftelaneebusinesses@gmail.com')

if not DEBUG and EMAIL_BACKEND == 'django.core.mail.backends.console.EmailBackend':
    # Deliberately a warning, not a hard startup failure — email is a
    # feature dependency (contact-form notifications), not a security-
    # critical one like SECRET_KEY above, so it shouldn't be able to take
    # the whole site down. But "must not silently use console" means this
    # has to be visible somewhere, not just an unlogged no-op.
    logging.getLogger(__name__).warning(
        'EMAIL_HOST is not set — running with DEBUG=False but still on the console '
        'email backend. Contact-form notifications (and any other outgoing mail) '
        'will only print to the server log, not actually deliver anywhere.'
    )

# ------------------------------------------------------------------
# SUPABASE (Auth + Storage)
# ------------------------------------------------------------------
# Used both for Supabase Storage and for the Google Sign-In bridge:
# the frontend uses SUPABASE_URL/SUPABASE_ANON_KEY with supabase-js to run
# the Google OAuth flow, then Django verifies the resulting access token
# server-side (via the Supabase Auth API) and mirrors the identity into a
# normal Django session — see core.supabase_auth.
SUPABASE_URL = os.getenv('SUPABASE_URL', '')
SUPABASE_ANON_KEY = os.getenv('SUPABASE_ANON_KEY', '')
SUPABASE_SERVICE_ROLE_KEY = os.getenv('SUPABASE_SERVICE_ROLE_KEY', '')

# Bucket used by core.storage.SupabaseMediaStorage (STORAGES['default'] above)
# for every ImageField in the project — listing photos, profile photos, and
# the post gallery. Created by `python manage.py ensure_supabase_bucket`.
SUPABASE_STORAGE_BUCKET = os.getenv('SUPABASE_STORAGE_BUCKET', 'media')

# ------------------------------------------------------------------
# WEB PUSH (desktop + mobile browser notifications)
# ------------------------------------------------------------------
# VAPID keypair identifying this server to browser push services (Chrome/
# FCM, Firefox/autopush, etc). Generate once via py-vapid and keep it
# constant across deploys — see .env.example — since rotating it silently
# invalidates every visitor's existing subscription. Same pair is safe to
# reuse between dev and prod.
VAPID_PUBLIC_KEY = os.getenv('VAPID_PUBLIC_KEY', '')
VAPID_PRIVATE_KEY = os.getenv('VAPID_PRIVATE_KEY', '')
VAPID_ADMIN_EMAIL = os.getenv('VAPID_ADMIN_EMAIL', '')

# Public-facing login for Google/Supabase-authenticated users (Content
# Providers, Normal Users, and the Super Admin's app-facing session).
# Kept separate from LOGIN_URL/'core:admin_login', which continues to
# gate the existing username/password staff login for Django's own
# /admin/ backend and the Excel upload tools — that flow is untouched.
GOOGLE_LOGIN_URL = 'core:google_login'

# SHA-256 fingerprint of the Android release signing certificate, for
# /.well-known/assetlinks.json (see core.views.assetlinks_json). Empty by
# default — only a real production keystore can produce this; see Phase 6's
# signing review. An empty value serves an empty fingerprint list, which
# fails App Links verification cleanly rather than shipping a wrong/fake one.
ANDROID_RELEASE_CERT_SHA256 = os.getenv('ANDROID_RELEASE_CERT_SHA256', '')

