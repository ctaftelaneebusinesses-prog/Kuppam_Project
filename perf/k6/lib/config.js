// Shared config + safety guard for every k6 script in this directory.
//
// BASE_URL defaults to the local Django dev server. Override with
// `k6 run -e BASE_URL=https://staging.onetowncity.com ...` for a staging
// run — never point this at the production host. ALLOWED_HOSTS below is a
// deliberately short allowlist (localhost/staging only); every script in
// this directory calls assertSafeTarget() before making a single request.
export const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8000';

const ALLOWED_HOST_PATTERNS = [
  /^127\.0\.0\.1(:\d+)?$/,
  /^localhost(:\d+)?$/,
  /^0\.0\.0\.0(:\d+)?$/,
  /staging\./,
];

export function assertSafeTarget(baseUrl) {
  const host = baseUrl.replace(/^https?:\/\//, '').replace(/\/.*$/, '');
  const isSafe = ALLOWED_HOST_PATTERNS.some((pattern) => pattern.test(host));
  if (!isSafe) {
    throw new Error(
      `Refusing to run: BASE_URL host "${host}" is not in the local/staging allowlist ` +
      `(perf/k6/lib/config.js). This test structure is dev/staging-only — production load ` +
      `testing needs explicit, separate authorization.`,
    );
  }
}

assertSafeTarget(BASE_URL);
