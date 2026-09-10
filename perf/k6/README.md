# k6 performance tests (Phase 5 tooling)

Development/staging-only. Every script imports `lib/config.js`, which
refuses to run against any host outside `127.0.0.1` / `localhost` / a
`staging.*` subdomain — pointing `BASE_URL` at production raises an error
before any request is made.

## Setup

k6 binary: install to `~/.local/bin` (no root needed), e.g.:

```bash
curl -sL -o /tmp/k6.tar.gz "https://github.com/grafana/k6/releases/download/v2.2.0/k6-v2.2.0-linux-amd64.tar.gz"
tar -xzf /tmp/k6.tar.gz -C /tmp
install -m 0755 /tmp/k6-v2.2.0-linux-amd64/k6 ~/.local/bin/k6
```

Start the Django dev server first: `python manage.py runserver` (from the
repo root).

## Scripts

| Script | What it checks | Load |
|---|---|---|
| `smoke.js` | homepage, search, city selection, business listing (page + API), event listing, listing detail, DRF listing API | trivial (2 VUs, 10 iterations) — proves the tooling + endpoints work |
| `load-business-listing.js` | Business listing API under ramping load — the pattern to copy for other endpoints | capped at 10 VUs, ~50s |
| `auth-protected.js` | `GET /api/v1/auth/me/` — asserts 401 when unauthenticated; pass `-e AUTH_TOKEN=<token>` for an authenticated run | trivial (1 VU, 5 iterations) |

## Run

```bash
cd perf/k6
k6 run smoke.js
k6 run load-business-listing.js
k6 run auth-protected.js                        # unauthenticated (expects 401)
k6 run -e AUTH_TOKEN=<supabase access_token> auth-protected.js
k6 run -e BASE_URL=https://staging.onetowncity.com smoke.js
```

## Every path here is real, not guessed

Verified against `core/urls.py`, `core/api/urls.py`, and
`static/js/location-selector.js` before writing these scripts:
- `/`, `/search/?q=`, `/businesses/`, `/businesses/<slug>/`, `/events/`
- `/api/locations/search/?q=` — the actual endpoint the header "Choose city"
  modal calls (not the DRF `/api/v1/locations/cities/`, which the web UI
  doesn't use)
- `/api/v1/listings/business/` — the generic DRF listing endpoint
  (`core/api/views.py`'s `listing_collection`), the same one the Android
  Business/Property/Project browse screens use
- `/api/v1/auth/me/` — requires `Authorization: Bearer <supabase token>`
  (`SupabaseTokenAuthentication`)
