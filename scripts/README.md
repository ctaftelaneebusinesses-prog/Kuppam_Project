# Local deploy & stress test scripts

## `deploy_test.sh`

Simulates a deploy locally: `manage.py check` -> migration check -> starts
the dev server -> hits real endpoints (`/`, `/robots.txt`, `/sitemap.xml`,
`/privacy-policy/`, `/terms-of-service/`, `/api/v1/listings/business/`,
`/api/v1/auth/me/`) and checks their status codes.

Picks a free port automatically starting at 8000 (checked by actually
binding it, not by guessing) so it never collides with anything else
already running. Override with `PORT=9000 scripts/deploy_test.sh`.

Leaves the server running in the background either way — stop it with
`scripts/stop_deploy_test.sh`.

## `stop_deploy_test.sh`

Stops the server `deploy_test.sh` started.

## `stress_test.sh`

Runs the existing k6 scripts in `perf/k6/` against the local server.
Reuses a server already started by `deploy_test.sh` if one is running;
otherwise starts one itself (same auto-port-finding) and stops it when
done. Requires the `k6` binary — see `perf/k6/README.md` if it's not
installed.

`perf/k6/lib/config.js` refuses to run against anything except
`localhost`/`127.0.0.1`/a `staging.*` host, so this can never accidentally
hit production.

## State

Both scripts keep their PID/port in `scripts/.state/` (gitignored,
recreated each run) — not meant to be edited by hand.
