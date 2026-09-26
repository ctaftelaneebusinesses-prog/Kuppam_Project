#!/bin/bash
# Stress/load-tests a LOCAL, production-like server using the existing k6
# scripts in perf/k6/.
#
# Deliberately does NOT use `manage.py runserver` (that's what
# deploy_test.sh is for) — it's single-threaded-ish and genuinely can't
# handle k6's concurrent load, so testing against it produces failures
# that are artifacts of the dev server, not the app. Instead this starts
# gunicorn with the SAME worker/thread config as the real Procfile
# (WEB_CONCURRENCY/WEB_THREADS, default 3/6, --worker-class gthread), so
# the numbers this produces are actually meaningful.
#
# Usage:
#   scripts/stress_test.sh              # start local gunicorn, run all k6 scripts, stop it
#   PORT=9000 scripts/stress_test.sh    # target an already-running server (e.g. staging) instead
set -euo pipefail

cd "$(dirname "$0")"
source lib.sh
cd "$(repo_root)"

if ! command -v k6 >/dev/null 2>&1; then
    echo "k6 is not installed or not on PATH — see perf/k6/README.md for install steps."
    exit 1
fi

STATE_DIR="scripts/.state"
mkdir -p "$STATE_DIR"
LOG_FILE="$STATE_DIR/stress_gunicorn.log"
PID_FILE="$STATE_DIR/stress_gunicorn.pid"
STARTED_OWN_SERVER=0
GUNICORN_PID=""

cleanup() {
    if [ -n "$GUNICORN_PID" ] && kill -0 "$GUNICORN_PID" 2>/dev/null; then
        echo "== stopping the gunicorn instance this script started (PID $GUNICORN_PID) =="
        kill "$GUNICORN_PID" 2>/dev/null || true
        wait "$GUNICORN_PID" 2>/dev/null || true
    fi
    rm -f "$PID_FILE"
}
trap cleanup EXIT

if [ -n "${PORT:-}" ]; then
    TARGET_PORT="$PORT"
    echo "Using explicit PORT=$TARGET_PORT (assuming something is already listening there)"
else
    source venv/bin/activate
    TARGET_PORT="$(find_free_port 8000)"
    echo "== starting gunicorn on 127.0.0.1:$TARGET_PORT (same config as the Procfile) =="
    WEB_CONCURRENCY="${WEB_CONCURRENCY:-3}"
    WEB_THREADS="${WEB_THREADS:-6}"
    nohup gunicorn hello_kuppam.wsgi:application \
        --bind "127.0.0.1:$TARGET_PORT" \
        --workers "$WEB_CONCURRENCY" \
        --threads "$WEB_THREADS" \
        --worker-class gthread \
        --timeout 30 \
        --log-file - \
        >"$LOG_FILE" 2>&1 &
    GUNICORN_PID=$!
    echo "$GUNICORN_PID" >"$PID_FILE"
    STARTED_OWN_SERVER=1

    echo "Waiting for gunicorn to come up..."
    UP=0
    for _ in $(seq 1 30); do
        if curl -sS -o /dev/null "http://127.0.0.1:$TARGET_PORT/" 2>/dev/null; then
            UP=1
            break
        fi
        sleep 0.5
    done
    if [ "$UP" -ne 1 ]; then
        echo "FAIL: gunicorn never came up. Log:"
        cat "$LOG_FILE"
        exit 1
    fi
fi

BASE_URL="http://127.0.0.1:$TARGET_PORT"
echo
echo "== stress-testing $BASE_URL =="
echo "(perf/k6/lib/config.js refuses any non-local/staging target, so this can never hit production)"
echo

# Run every script regardless of an earlier one's threshold result (a k6
# threshold miss, e.g. p95 latency, is not a crash and shouldn't stop the
# rest of the suite from running) — collect pass/fail per script instead.
cd perf/k6
RESULTS=()
for script in smoke.js load-business-listing.js auth-protected.js; do
    echo "--- $script ---"
    if k6 run -e BASE_URL="$BASE_URL" "$script"; then
        RESULTS+=("PASS  $script")
    else
        RESULTS+=("FAIL  $script")
    fi
    echo
done
cd "$(repo_root)"

echo "== summary =="
FAILED=0
for r in "${RESULTS[@]}"; do
    echo "  $r"
    [[ "$r" == FAIL* ]] && FAILED=1
done
exit "$FAILED"
