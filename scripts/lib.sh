#!/bin/bash
# Shared helpers for scripts/*.sh. Not meant to be run directly.

# find_free_port [start_port] — prints the first free TCP port on 127.0.0.1
# starting at start_port (default 8000), checked by actually binding it
# (not just grepping netstat), so it can't race a port that's free-but-about
# -to-be-taken. Searches up to 100 ports past the start before giving up.
find_free_port() {
    local start_port="${1:-8000}"
    python3 - "$start_port" <<'PYEOF'
import socket
import sys

start = int(sys.argv[1])
for candidate in range(start, start + 100):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            s.bind(("127.0.0.1", candidate))
            print(candidate)
            sys.exit(0)
        except OSError:
            continue
print(f"ERROR: no free port found in {start}-{start + 99}", file=sys.stderr)
sys.exit(1)
PYEOF
}

# repo_root — prints the absolute path to the project root, regardless of
# which directory the calling script was invoked from.
repo_root() {
    cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd
}
