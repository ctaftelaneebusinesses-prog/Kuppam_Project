# OneTownCity

**A hyperlocal community platform for Kuppam** — a business directory, property listings, job board, events, news, and community projects, built around a role-based content submission and approval workflow.

[![Django](https://img.shields.io/badge/Django-5.2-092E20?logo=django&logoColor=white)](https://www.djangoproject.com/)
[![DRF](https://img.shields.io/badge/REST%20Framework-3.16-A30000?logo=django&logoColor=white)](https://www.django-rest-framework.org/)
[![Python](https://img.shields.io/badge/Python-3.11%2B-3776AB?logo=python&logoColor=white)](https://www.python.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-3ECF8E?logo=supabase&logoColor=white)](https://supabase.com/)
[![Kotlin](https://img.shields.io/badge/Android-Kotlin%20%2B%20Compose-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Proprietary-lightgrey.svg)](#license)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [1. Clone the repository](#1-clone-the-repository)
  - [2. Create a virtual environment](#2-create-a-virtual-environment)
  - [3. Install dependencies](#3-install-dependencies)
  - [4. Configure environment variables](#4-configure-environment-variables)
  - [5. Apply database migrations](#5-apply-database-migrations)
  - [6. Create an admin account](#6-create-an-admin-account)
  - [7. Run the development server](#7-run-the-development-server)
- [Environment Variables](#environment-variables)
- [REST API](#rest-api)
- [Android App](#android-app)
- [Running Tests](#running-tests)
- [Useful Management Commands](#useful-management-commands)
- [Deployment](#deployment)
- [Internationalization](#internationalization)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

OneTownCity connects a town's residents, businesses, and administrators on one platform. Content flows through a moderated pipeline — **Normal User → Content Provider → Category Admin → Super Admin** — so every business, property, job, event, or news post is reviewed before it goes live.

The project ships as three coordinated pieces:

| Surface | Description |
|---|---|
| **Website** | Server-rendered Django templates — the primary public-facing experience |
| **REST API** (`/api/v1/`) | Django REST Framework endpoints backing the native Android client |
| **Android App** | A Kotlin + Jetpack Compose client consuming the REST API |

## Features

- 🏢 **Business Directory** — restaurants, hospitals, education, transport, repair services, tuition centers, marketplace listings, student services, and more
- 🏠 **Property Listings**, 💼 **Job Board**, 📅 **Events**, 📰 **News**, 🤝 **Community Projects**
- 👥 **Role-based access control** — Normal User, Content Provider, City Admin, Sub Admin, Super Admin, each with scoped category/city permissions
- ✅ **Moderation workflow** — draft → pending → approved/rejected/changes-requested, with full audit logging
- ❤️ **Social layer** — likes, threaded comments, ratings/reviews, favorites, sharing, and reporting
- 🔔 **Notifications** — in-app notification center plus Web Push (VAPID) for desktop and mobile browsers
- 🔐 **Dual authentication** — Google Sign-In via Supabase Auth for the public app, classic Django auth for staff/admin tooling
- 🌐 **Multi-language support** — Telugu, Hindi, Tamil, and Kannada translations
- 📊 **Bulk data tools** — Excel import/export for administrators
- 📱 **Native Android client** — browses listings, cities, and categories via the REST API
- 🎨 **Light & dark themes**, responsive design, PWA-ready with an offline fallback

## Tech Stack

**Backend**
- [Django 5.2](https://www.djangoproject.com/) with [Django REST Framework](https://www.django-rest-framework.org/)
- **PostgreSQL** hosted on [Supabase](https://supabase.com/) (database, file storage, and auth bridge)
- **WhiteNoise** for compressed, hashed static file serving
- **gunicorn** as the production WSGI server
- **Redis** (optional, via `django-redis`) for shared caching across workers

**Frontend**
- Server-rendered Django templates, no build step
- **Bootstrap 5.3** (self-hosted) + a custom CSS design-token system
- Vanilla JavaScript — no framework, no bundler

**Android**
- **Kotlin** + **Jetpack Compose** (Material 3)
- **Navigation Compose** for in-app and deep-link routing
- **Coil** for image loading

**Infrastructure**
- Deployed via `Procfile` (gunicorn + automatic migrations on release) to any buildpack-based PaaS
- File storage and authentication delegated to **Supabase**

## Architecture

```
┌───────────────────┐        ┌───────────────────────┐
│     Website         │        │     Android App        │
│ (Django templates)  │        │ (Kotlin + Compose)     │
└──────────┬──────────┘        └───────────┬────────────┘
           │ Django sessions               │ Bearer token
           │ (Google/Supabase OAuth)       │ (Supabase Auth)
           ▼                               ▼
┌─────────────────────────────────────────────────────────┐
│                   Django Application                      │
│   core/views.py           core/api/  (DRF, /api/v1/)      │
│   Shared business logic: apply_new_listing_submission,    │
│   apply_listing_edit_state, permissions, notifications    │
└───────────────────────────┬────────────────────────────┘
                             │
              ┌──────────────┴───────────────┐
              ▼                               ▼
     ┌──────────────────┐          ┌──────────────────────┐
     │ Supabase           │          │ Supabase               │
     │ PostgreSQL          │          │ Storage / Auth          │
     └──────────────────┘          └──────────────────────┘
```

The website and the Android app both terminate on the same Django backend and share the same submission/approval state machine, so listing rules can never drift between the two clients.

## Project Structure

```
Kuppam_Project/
├── hello_kuppam/           # Django project package (settings, urls, wsgi/asgi)
├── core/                   # Main Django app
│   ├── models.py           # Business, Property, Job, Event, News, Project, Profile, etc.
│   ├── views.py            # Website views + dashboard
│   ├── forms.py            # Listing submission forms & validation
│   ├── admin.py            # Django admin configuration
│   ├── decorators.py       # Auth, role, and rate-limiting decorators
│   ├── storage.py          # Supabase Storage backend
│   ├── supabase_auth.py    # Google/Supabase auth bridge
│   ├── excel_utils.py      # Bulk import/export
│   ├── i18n_utils.py       # Translation pipeline
│   ├── push.py             # Web Push (VAPID) notifications
│   ├── migrations/         # Database migrations
│   └── api/                # REST API (Django REST Framework)
│       ├── views.py
│       ├── serializers.py
│       ├── permissions.py
│       ├── authentication.py
│       ├── urls.py
│       └── tests/
├── templates/              # HTML templates (public site + admin dashboard)
├── static/                 # CSS, JavaScript, images
├── locale/                 # Translation files (te, hi, ta, kn)
├── android/                # Native Android app (Kotlin + Jetpack Compose)
│   └── app/src/main/java/com/onetowncity/app/
├── manage.py
├── requirements.txt
├── Procfile
└── .env.example
```

## Getting Started

### Prerequisites

- **Python 3.11+**
- **PostgreSQL** database (a free [Supabase](https://supabase.com/) project works out of the box)
- **Git**
- *(Optional, for Android)* **Android Studio** with a compatible JDK, or the Gradle wrapper + Android SDK command-line tools

### 1. Clone the repository

```bash
git clone git@github.com:ctaftelaneebusinesses-prog/Kuppam_Project.git
cd Kuppam_Project
```

### 2. Create a virtual environment

```bash
python3 -m venv venv

# macOS / Linux
source venv/bin/activate

# Windows (PowerShell)
venv\Scripts\Activate.ps1
```

### 3. Install dependencies

```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### 4. Configure environment variables

```bash
cp .env.example .env
```

Then open `.env` and fill in your own values (see [Environment Variables](#environment-variables) below). At minimum you'll need a `DATABASE_URL` and a Supabase project's URL/keys.

### 5. Apply database migrations

```bash
python manage.py migrate
```

### 6. Create an admin account

```bash
python manage.py create_super_admin
```

### 7. Run the development server

```bash
python manage.py runserver
```

The site will be available at **http://127.0.0.1:8000/**, and the Django admin at **http://127.0.0.1:8000/admin/**.

## Environment Variables

All variables live in `.env` (never committed — see `.env.example` for a template).

| Variable | Required | Description |
|---|---|---|
| `DEBUG` | ✅ | `True` for local development, `False` in production |
| `SECRET_KEY` | ✅ | Django's cryptographic signing key — always set a unique value in production |
| `ALLOWED_HOSTS` | ✅ | Comma-separated list of hostnames the app will serve |
| `CSRF_TRUSTED_ORIGINS` | Production | Comma-separated, scheme-qualified origins for CSRF protection |
| `DATABASE_URL` | ✅ | PostgreSQL connection string (Supabase transaction pooler, port `6543`, recommended) |
| `SUPABASE_URL` | ✅ | Your Supabase project URL |
| `SUPABASE_ANON_KEY` | ✅ | Public anon key, used client-side for Google Sign-In |
| `SUPABASE_SERVICE_ROLE_KEY` | ✅ | Server-side privileged key for Storage/Auth verification |
| `SUPABASE_STORAGE_BUCKET` | ✅ | Storage bucket name for uploaded media (default: `media`) |
| `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` | Optional | Web Push notification keys (see generation command in `.env.example`) |
| `VAPID_ADMIN_EMAIL` | Optional | Contact email required by the Web Push protocol |

## REST API

The API lives under `core/api/` and is mounted at **`/api/v1/`**, built with Django REST Framework and authenticated via Supabase Bearer tokens (with session-cookie fallback for browser clients).

| Group | Example endpoints |
|---|---|
| Auth | `auth/me/`, `auth/logout/` |
| Locations | `locations/cities/`, `locations/reverse-geocode/` |
| Discovery | `categories/`, `search/` |
| Listings | `listings/<model>/`, `listings/<model>/<id>/` |
| Social | `.../favorite/`, `.../like/`, `.../comments/`, `.../reviews/`, `.../report/` |
| Media | `.../media/`, `media/images/<id>/`, `media/videos/<id>/` |
| Account | `my/listings/`, `my/favorites/` |
| Notifications | `notifications/`, `notifications/<id>/read/`, `notifications/unread-count/` |

A full test suite lives in `core/api/tests/`. Run it with:

```bash
python manage.py test core
```

## Android App

The native client lives in `android/`, built with Kotlin and Jetpack Compose.

```bash
cd android
./gradlew assembleDebug     # build a debug APK
./gradlew testDebugUnitTest # run unit tests
```

Open the `android/` folder directly in **Android Studio** for the full IDE experience (emulator, layout preview, etc.). The app talks to the same backend as the website via the REST API described above.

> **Note:** a release build requires signing credentials supplied via environment variables (`ONETOWNCITY_STORE_FILE`, `ONETOWNCITY_STORE_PASSWORD`, `ONETOWNCITY_KEY_ALIAS`, `ONETOWNCITY_KEY_PASSWORD`) — these are never committed to the repository.

## Running Tests

```bash
# Full backend + API test suite
python manage.py test core

# A specific test module
python manage.py test core.api.tests.test_listings
```

The test suite uses an in-memory SQLite database and local file storage, so it never touches your real Supabase project.

## Useful Management Commands

| Command | Purpose |
|---|---|
| `python manage.py create_super_admin` | Bootstrap the first Super Admin account |
| `python manage.py ensure_supabase_bucket` | Create/verify the Supabase Storage bucket |
| `python manage.py migrate_local_media_to_supabase` | Migrate local media into Supabase Storage |
| `python manage.py reclassify_businesses` | Re-run auto-classification against existing listings |

## Deployment

The app is designed for any buildpack-based PaaS (Render, Railway, Heroku, etc.) via the included `Procfile`:

```
web: gunicorn hello_kuppam.wsgi:application --workers ${WEB_CONCURRENCY:-3} --threads ${WEB_THREADS:-6} --worker-class gthread --timeout 30 --graceful-timeout 30 --max-requests 500 --max-requests-jitter 50 --preload --log-file -
release: python manage.py migrate --noinput
```

- Static files are served entirely by **WhiteNoise** — no separate CDN required, just run `python manage.py collectstatic` as part of your build step.
- Database and file storage are fully externalized to **Supabase**, so the app server itself is stateless.
- Set `DEBUG=False` and provide a real `ALLOWED_HOSTS` / `CSRF_TRUSTED_ORIGINS` before deploying — production security headers (`SECURE_SSL_REDIRECT`, HSTS, secure cookies) activate automatically once `DEBUG` is `False`.

## Internationalization

OneTownCity supports **Telugu, Hindi, Tamil, and Kannada** alongside English, with translation files under `locale/`. Dynamic listing content is machine-translated and cached via `core/i18n_utils.py`.

## Contributing

1. Create a feature branch from `main`
2. Make your changes, following the existing code style
3. Add or update tests for any behavior change
4. Run `python manage.py test core` and confirm everything passes
5. Open a pull request describing what changed and why

## License

This project is proprietary. All rights reserved — please contact the maintainer for usage terms.
