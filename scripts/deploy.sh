#!/usr/bin/env bash
set -Eeuo pipefail

# OneTownCity production deployment entrypoint.
# Install dependencies, validate production settings, prepare Django, and run Gunicorn.

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PYTHON_BIN="${PYTHON_BIN:-python3}"
PIP_BIN="${PIP_BIN:-$PYTHON_BIN -m pip}"

if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
    printf 'Error: Python interpreter not found: %s\n' "$PYTHON_BIN" >&2
    exit 1
fi

: "${DATABASE_URL:?DATABASE_URL must be set}"
: "${SECRET_KEY:?SECRET_KEY must be set}"
: "${ALLOWED_HOSTS:?ALLOWED_HOSTS must be set}"

if [[ "${DEBUG:-False}" == "True" ]]; then
    printf 'Error: DEBUG must be False for deployment.\n' >&2
    exit 1
fi

if [[ "$SECRET_KEY" == *change-this* || "$SECRET_KEY" == django-insecure-* ]]; then
    printf 'Error: SECRET_KEY is still using a development value.\n' >&2
    exit 1
fi

printf '%s\n' 'Installing Python dependencies...'
# shellcheck disable=SC2086
$PIP_BIN install --no-cache-dir --timeout "${PIP_TIMEOUT:-300}" -r requirements.txt

printf '%s\n' 'Checking production configuration...'
"$PYTHON_BIN" manage.py check --deploy

printf '%s\n' 'Applying database migrations...'
"$PYTHON_BIN" manage.py migrate --noinput

printf '%s\n' 'Collecting static assets...'
"$PYTHON_BIN" manage.py collectstatic --noinput

PORT="${PORT:-8000}"
WEB_CONCURRENCY="${WEB_CONCURRENCY:-2}"
WEB_THREADS="${WEB_THREADS:-4}"

printf 'Starting Gunicorn on port %s...\n' "$PORT"
exec "$PYTHON_BIN" -m gunicorn hello_kuppam.wsgi:application \
    --bind "0.0.0.0:$PORT" \
    --workers "$WEB_CONCURRENCY" \
    --threads "$WEB_THREADS" \
    --worker-class gthread \
    --timeout "${WEB_TIMEOUT:-30}" \
    --graceful-timeout "${WEB_GRACEFUL_TIMEOUT:-30}" \
    --max-requests "${WEB_MAX_REQUESTS:-500}" \
    --max-requests-jitter "${WEB_MAX_REQUESTS_JITTER:-50}" \
    --preload \
    --access-logfile - \
    --error-logfile -
