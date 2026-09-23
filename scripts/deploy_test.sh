#!/bin/bash
# Local deployment smoke test: boots the app the way a real deploy would
# (system check -> migration check -> start server -> hit real endpoints)
# against an automatically-chosen free port, so it never collides with
# anything else already listening (e.g. a server you started by hand).
#
# Usage:
#   scripts/deploy_test.sh              # auto-pick a free port from 8000
#   PORT=9000 scripts/deploy_test.sh    # force a specific starting port
#
# Leaves the dev server running in the background on success or failure so
# you can keep poking at it. Stop it with scripts/stop_deploy_test.sh.
set -euo pipefail

cd "$(dirname "$0")"
source lib.sh
cd "$(repo_root)"

STATE_DIR="scripts/.state"
LOG_FILE="$STATE_DIR/deploy_test.log"
PID_FILE="$STATE_DIR/deploy_test.pid"
PORT_FILE="$STATE_DIR/deploy_test.port"
mkdir -p "$STATE_DIR"

if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "A deploy_test server is already running (PID $(cat "$PID_FILE"), port $(cat "$PORT_FILE"))."
    echo "Stop it first with scripts/stop_deploy_test.sh, or just reuse it."
    exit 1
fi

source venv/bin/activate

echo "== manage.py check =="
python manage.py check

echo "== migrations check (no missing/accidental migrations) =="
python manage.py makemigrations --check --dry-run

PORT="${PORT:-$(find_free_port 8000)}"
echo
echo "== starting dev server on 127.0.0.1:$PORT =="
nohup python manage.py runserver "127.0.0.1:$PORT" >"$LOG_FILE" 2>&1 &
SERVER_PID=$!
echo "$SERVER_PID" >"$PID_FILE"
echo "$PORT" >"$PORT_FILE"

echo "Waiting for the server to come up..."
UP=0
for _ in $(seq 1 20); do
    if curl -sS -o /dev/null "http://127.0.0.1:$PORT/" 2>/dev/null; then
        UP=1
        break
    fi
    sleep 0.5
done
if [ "$UP" -ne 1 ]; then
    echo "FAIL: server never came up. Log:"
    cat "$LOG_FILE"
    exit 1
fi

check_url() {
    local path="$1" expect="$2" code
    code=$(curl -sS -o /dev/null -w '%{http_code}' "http://127.0.0.1:$PORT$path")
    if [ "$code" = "$expect" ]; then
        echo "  OK   $path -> $code"
    else
        echo "  FAIL $path -> $code (expected $expect)"
        FAILED=1
    fi
}

echo
echo "== smoke-checking real endpoints =="
FAILED=0
check_url "/" 200
check_url "/robots.txt" 200
check_url "/sitemap.xml" 200
check_url "/privacy-policy/" 200
check_url "/terms-of-service/" 200
check_url "/api/v1/listings/business/" 200
check_url "/api/v1/auth/me/" 401   # unauthenticated -> must be rejected, not 200

echo
if [ "${FAILED:-0}" -eq 0 ]; then
    echo "DEPLOY TEST: PASS"
else
    echo "DEPLOY TEST: FAIL"
fi

echo
echo "Server is running in the background: PID $SERVER_PID, port $PORT"
echo "Log:  $LOG_FILE"
echo "Stop: scripts/stop_deploy_test.sh"

exit "${FAILED:-0}"
