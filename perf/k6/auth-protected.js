// Auth-protected endpoint check — GET /api/v1/auth/me/ (core/api/views.py's
// `me`, requires a valid Supabase bearer token; see
// core/api/authentication.py's SupabaseTokenAuthentication, the same
// mechanism android/app/.../auth/SessionManager.kt uses).
//
// No token is fabricated here — there's no way to mint a real Supabase
// session token without going through actual sign-in. To run this locally:
//   1. Sign in at http://127.0.0.1:8000/signin/ in a browser.
//   2. Open DevTools -> Application -> Local Storage -> find the
//      Supabase session entry (key starts with "sb-") and copy its
//      `access_token` value.
//   3. k6 run -e AUTH_TOKEN=<that token> perf/k6/auth-protected.js
//
// Without AUTH_TOKEN, this script checks that the endpoint correctly
// requires authentication (expects 401) rather than skipping outright —
// that's still a meaningful assertion about the auth-protected endpoint.
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from './lib/config.js';

export const options = {
  vus: 1,
  iterations: 5,
  thresholds: {
    http_req_failed: ['rate<0.05'],
  },
};

export default function () {
  const token = __ENV.AUTH_TOKEN || '';
  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  const res = http.get(`${BASE_URL}/api/v1/auth/me/`, { headers });

  if (token) {
    check(res, {
      'authenticated /auth/me/ returns 200': (r) => r.status === 200,
      'response has expected profile fields': (r) => {
        try {
          const body = JSON.parse(r.body);
          return 'username' in body && 'email' in body;
        } catch {
          return false;
        }
      },
    });
  } else {
    check(res, {
      'unauthenticated /auth/me/ correctly rejected with 401': (r) => r.status === 401,
    });
  }
}
