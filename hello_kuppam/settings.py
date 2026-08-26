"""
Django settings for hello_kuppam project.
"""

import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

BASE_DIR = Path(__file__).resolve().parent.parent

# ------------------------------------------------------------------
# SECURITY
# ------------------------------------------------------------------
SECRET_KEY = os.getenv('SECRET_KEY', 'django-insecure-change-this-in-production')

DEBUG = os.getenv('DEBUG', 'True') == 'True'

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
        # Persistent connections pin one Supabase pooler slot (15 total in
        # session mode, the only mode this project's Supabase plan exposes)
        # per thread for their whole lifetime. With 2 gunicorn workers x 4
        # threads that's up to 8 slots held continuously, and a killed/
        # restarted worker (OOM, timeout) doesn't release its slot cleanly —
        # a couple of crash cycles is enough to exhaust the pool and start
        # 500ing every request. Keeping this at 0 closes each connection
        # right after its request instead of holding it for minutes.
        conn_max_age=0,
    )
}
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
# AUTH REDIRECTS (admin dashboard login)
# ------------------------------------------------------------------
LOGIN_URL = 'core:admin_login'
LOGIN_REDIRECT_URL = '/admin/'
LOGOUT_REDIRECT_URL = 'core:home'

# ------------------------------------------------------------------
# EMAIL (Contact form â€” console backend for development)
# ------------------------------------------------------------------
EMAIL_BACKEND = 'django.core.mail.backends.console.EmailBackend'
DEFAULT_FROM_EMAIL = 'noreply@hellokuppam.com'
CONTACT_RECEIVER_EMAIL = 'admin@hellokuppam.com'

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

