#!/bin/bash
# Stops the dev server started by scripts/deploy_test.sh.
set -euo pipefail

cd "$(dirname "$0")"
source lib.sh
cd "$(repo_root)"

STATE_DIR="scripts/.state"
PID_FILE="$STATE_DIR/deploy_test.pid"
PORT_FILE="$STATE_DIR/deploy_test.port"

if [ ! -f "$PID_FILE" ]; then
    echo "No deploy_test.pid found — nothing to stop."
    exit 0
fi

PID="$(cat "$PID_FILE")"
if kill -0 "$PID" 2>/dev/null; then
    kill "$PID"
    echo "Stopped dev server (PID $PID, port $(cat "$PORT_FILE" 2>/dev/null || echo '?'))."
else
    echo "No running process for PID $PID (already stopped)."
fi

rm -f "$PID_FILE" "$PORT_FILE"
