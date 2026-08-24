# OneTownCity (formerly "Hello Kuppam") — Project Overview

A hyperlocal community platform for Kuppam: business directory, property listings,
job board, events, news, and community projects — with a role-based content
submission/approval workflow (Normal User → Content Provider → Category Admin →
Super Admin).

This document is a single, up-to-date reference for how the project is built,
how to run it locally, and how it is deployed. Update it whenever the stack,
hosting, or local setup steps change.

---

## 1. Tech Stack

### Backend
- **Framework:** Django 5.1.3 (Python), classic server-rendered Django templates
  (no DRF/REST API layer — views return HTML directly).
- **Database:** PostgreSQL, hosted on **Supabase** (connected via
  `DATABASE_URL` / Supabase Session Pooler connection string).
  - `dj-database-url` parses the connection string in
    [hello_kuppam/settings.py](hello_kuppam/settings.py).
  - Standard Django ORM + migrations (`core/migrations/`).
- **Auth:** Two parallel systems:
  1. Django's built-in username/password auth — gates `/admin/` (Django admin)
     and the Excel bulk-upload tools (`core.admin_login`).
  2. **Supabase Auth** (Google Sign-In) — the public-facing login for Normal
     Users, Content Providers, and the Super Admin's app-facing dashboard.
     Flow: frontend uses `supabase-js` with `SUPABASE_URL`/`SUPABASE_ANON_KEY`
     to run Google OAuth, then Django verifies the resulting access token
     server-side against the Supabase Auth API and mirrors the identity into
     a normal Django session. See [core/supabase_auth.py](core/supabase_auth.py).
- **File/media storage:** Supabase Storage (a bucket, default name `media`),
  not local disk in production. Implemented as a custom Django storage backend
  in [core/storage.py](core/storage.py) (`core.storage.SupabaseMediaStorage`),
  wired in as `STORAGES['default']`. Created/managed via the
  `python manage.py ensure_supabase_bucket` management command.
- **Static files:** Served by **WhiteNoise** directly from the app server
  (`whitenoise.middleware.WhiteNoiseMiddleware`), with cache-busting hashed
  filenames + gzip compression (`CompressedManifestStaticFilesStorage`). No
  separate CDN/nginx needed.
- **Excel import:** `pandas` + `openpyxl` power bulk data upload/import tools
  (`core/excel_utils.py`) used by admins to seed listings from spreadsheets.
- **Other key libraries:** `Pillow` (image processing), `python-dotenv`
  (loads `.env` locally), `python-dateutil`, `gunicorn` (production WSGI
  server), `psycopg2-binary` (Postgres driver).

Full pinned list: [requirements.txt](requirements.txt).

### Frontend
- **No SPA framework** — plain Django templates (`templates/`) rendered
  server-side, styled with **Bootstrap 5.3.3** (via CDN, not an npm build).
- **Bootstrap Icons 1.11.3** (CDN) for iconography.
- **AOS (Animate On Scroll) 2.3.4** (CDN) for scroll animations.
- **Custom CSS design system:** [static/css/tokens.css](static/css/tokens.css)
  defines `hk-*` custom properties/tokens (colors, spacing, etc.) layered on
  top of Bootstrap; [static/css/main.css](static/css/main.css) holds the
  bulk of custom styling, including the site-wide dark theme (slate-950
  ambient-glow canvas, toggle-driven).
- **Vanilla JavaScript** only — [static/js/main.js](static/js/main.js) (no
  React/Vue/build step, no bundler, no `package.json`).
- **Fonts:** Google Fonts — Inter (400–800 weights).

### Data model (`core/models.py`)
Six listing types share a common `ListingMixin` (status: draft/pending/
approved/rejected, category FK, timestamps, etc.): **Business, Property, Job,
Event, News, Project**. Supporting models: `Category`, `Profile`,
`AdminRequest` / `AdminCategoryPermission` (role & category-scoped admin
workflow), `Like`, `Comment`, `Review`, `Favorite`, `Share`, `Report`,
`PostImage`, `Notification`, `LoginHistory`, `ContactMessage`,
`NewsletterSubscriber`.

---

## 2. Project Structure

```
HelloKuppam/                   ← repo root
└── hello_kuppam/               ← Django project root (this is where you `cd` to run everything)
    ├── manage.py
    ├── requirements.txt
    ├── Procfile                ← production process definition (gunicorn + migrate)
    ├── hello_kuppam/            ← Django project package (settings, urls, wsgi/asgi)
    ├── core/                   ← the one Django app — models, views, urls, admin, etc.
    │   ├── models.py
    │   ├── views.py
    │   ├── urls.py
    │   ├── forms.py
    │   ├── admin.py
    │   ├── classification.py   ← auto-classification helpers for listings
    │   ├── excel_utils.py      ← Excel bulk import logic (source of truth for category config)
    │   ├── storage.py          ← Supabase Storage Django storage backend
    │   ├── supabase_auth.py    ← Google/Supabase auth bridge
    │   ├── context_processors.py
    │   ├── management/commands/ ← create_super_admin, ensure_supabase_bucket, migrate_local_media_to_supabase, reclassify_businesses
    │   └── migrations/
    ├── templates/              ← all HTML (Django template language), incl. templates/dashboard/
    ├── static/                 ← source CSS/JS/images (css/tokens.css, css/main.css, js/main.js)
    ├── staticfiles/            ← collectstatic output (generated, not hand-edited)
    └── media/                  ← local dev media fallback (production media lives in Supabase Storage)
```

---

## 3. Environment Variables (`.env`, not committed)

| Variable | Purpose |
|---|---|
| `DEBUG` | `True`/`False`. Controls Django debug mode and prod security headers. |
| `SECRET_KEY` | Django secret key. |
| `ALLOWED_HOSTS` | Comma-separated allowed hostnames. |
| `CSRF_TRUSTED_ORIGINS` | Comma-separated scheme-qualified origins for CSRF (needed once a real domain is live). |
| `DATABASE_URL` | Supabase Postgres connection string (Session Pooler). |
| `SUPABASE_URL` | Supabase project URL (used by both storage and auth). |
| `SUPABASE_ANON_KEY` | Public anon key, used client-side for Google Sign-In via `supabase-js`. |
| `SUPABASE_SERVICE_ROLE_KEY` | Server-side privileged key, used for Storage/Auth verification calls. |
| `SUPABASE_STORAGE_BUCKET` | Bucket name for uploaded media (default `media`). |

A `.env.example` should exist alongside `.env` as a template — copy it and
fill in real values (see setup steps below).

---

## 4. Running the Project Locally

```powershell
# 1. From the repo, go to the Django project root
cd F:\Projects\HelloKuppam\hello_kuppam

# 2. Create a virtual environment
python -m venv venv

# 3. Activate it
venv\Scripts\Activate.ps1

# 4. Install dependencies
pip install --no-cache-dir --timeout 300 -r requirements.txt

# 5. Create your local .env from the example and fill in real Supabase values
Copy-Item .env.example .env
notepad .env
# → fill in DATABASE_URL with your Supabase Session Pooler string + password,
#   plus SUPABASE_URL / SUPABASE_ANON_KEY / SUPABASE_SERVICE_ROLE_KEY

# 6. Apply migrations (verifies the DB connection too)
python manage.py migrate

# 7. (Optional) create a Django admin superuser
python manage.py createsuperuser

# 8. Run the dev server
python manage.py runserver
```

Site will be at `http://127.0.0.1:8000/`. Django admin at `/admin/`.

### Useful management commands
- `python manage.py ensure_supabase_bucket` — creates/verifies the Supabase
  Storage bucket used for all uploaded images.
- `python manage.py create_super_admin` — bootstraps the first Super Admin
  account.
- `python manage.py migrate_local_media_to_supabase` — migrates any
  locally-stored media into Supabase Storage.
- `python manage.py reclassify_businesses` — re-runs auto-classification
  (`core/classification.py`) against existing Business rows.

---

## 5. Deployment / Hosting

For a complete deploy from a Linux/macOS shell, run:

```bash
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

The script requires `DATABASE_URL`, `SECRET_KEY`, and `ALLOWED_HOSTS`, and
refuses to run with `DEBUG=True` or a development secret. It installs the
requirements, runs the production checks, applies migrations, collects static
assets, and starts Gunicorn. `PORT`, `WEB_CONCURRENCY`, and `WEB_THREADS` can
be overridden by the hosting platform.

- **Process definition:** [Procfile](Procfile):
  ```
  web: gunicorn hello_kuppam.wsgi:application --log-file -
  release: python manage.py migrate --noinput
  ```
  This is the standard Heroku-style Procfile format, consumed by PaaS
  platforms that build from a Procfile (Render, Railway, Heroku, etc.). The
  `release` phase runs migrations automatically on every deploy before the
  `web` process starts.
- **Runtime:** WSGI app served by **gunicorn** (`hello_kuppam.wsgi.application`).
- **Static files:** handled entirely by WhiteNoise inside the same process —
  no separate static hosting/CDN is required. Run
  `python manage.py collectstatic` as part of the build step so
  `staticfiles/` is populated with hashed/gzipped assets.
- **Database + file storage:** both externalized to **Supabase** (Postgres +
  Storage), so the app server itself is stateless and can be redeployed/
  restarted freely without data loss.
- **HTTPS/proxy:** `settings.py` sets `SECURE_PROXY_SSL_HEADER`,
  `SESSION_COOKIE_SECURE`, and `CSRF_COOKIE_SECURE` when `DEBUG=False`,
  since the PaaS terminates TLS at its edge and forwards plain HTTP
  internally.
- **Required production env vars:** all of the variables in section 3 above,
  plus a real `ALLOWED_HOSTS` and `CSRF_TRUSTED_ORIGINS` for the live domain.
- **Source control:** GitHub — `origin` remote is
  `https://github.com/ctaftelaneebusinesses-prog/Kuppam_Project.git`, `main`
  branch.

---

## 6. Naming Note

The product is branded **OneTownCity** (rebranded from "Hello Kuppam" on
2026-07-31 — new logo, amber/orange gradient, updated SEO meta), but the
Django project package, repo folder, and Python module are still named
`hello_kuppam` / `Kuppam_Project` internally. That's expected — only
user-facing strings/branding changed, not the codebase's internal naming.
