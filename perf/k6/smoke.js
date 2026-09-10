// Light smoke check across the main page/API surfaces, at trivial load
// (1 VU, a handful of iterations) — proves the k6 tooling + these exact
// endpoints work, NOT a load test. See load-business-listing.js for an
// actual ramping-VU example, and auth-protected.js for the endpoint that
// needs a bearer token.
//
// Run:  k6 run perf/k6/smoke.js
// Staging: k6 run -e BASE_URL=https://staging.onetowncity.com perf/k6/smoke.js
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { BASE_URL } from './lib/config.js';

export const options = {
  vus: 2,
  iterations: 10,
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  },
};

export default function () {
  group('homepage', () => {
    const res = http.get(`${BASE_URL}/`);
    check(res, { 'homepage 200': (r) => r.status === 200 });
  });

  group('search', () => {
    // core/views.py's search() reads ?q= — mirrors e2e/tests/smoke.spec.ts's
    // browser search test (same minlength=2 constraint on the real form).
    const res = http.get(`${BASE_URL}/search/?q=shop`);
    check(res, { 'search 200': (r) => r.status === 200 });
  });

  group('city selection', () => {
    // The actual endpoint static/js/location-selector.js calls for the
    // header "Choose city" modal's live search (core/urls.py: location_search).
    const res = http.get(`${BASE_URL}/api/locations/search/?q=Kup`);
    check(res, { 'city search 200': (r) => r.status === 200 });
  });

  group('business listing (web page)', () => {
    const res = http.get(`${BASE_URL}/businesses/`);
    check(res, { 'business_list 200': (r) => r.status === 200 });
  });

  group('event listing (web page)', () => {
    const res = http.get(`${BASE_URL}/events/`);
    check(res, { 'event_list 200': (r) => r.status === 200 });
  });

  group('listing detail (web page)', () => {
    // No hardcoded slug (data varies by environment/seed state) — fetch a
    // real one from the DRF API list first, same approach as
    // e2e/tests/smoke.spec.ts's browser test (click-through rather than
    // guessing a slug).
    const listRes = http.get(`${BASE_URL}/api/v1/listings/business/?page=1&page_size=1`);
    const results = (() => {
      try {
        return JSON.parse(listRes.body).results || [];
      } catch {
        return [];
      }
    })();
    if (results.length > 0 && results[0].slug) {
      const detailRes = http.get(`${BASE_URL}/businesses/${results[0].slug}/`);
      check(detailRes, { 'business_detail 200': (r) => r.status === 200 });
    }
  });

  group('API listing endpoint (DRF)', () => {
    // core/api/urls.py: listing_collection — the same generic paginated
    // endpoint the Android app's Business/Property/Project browse screens use.
    const res = http.get(`${BASE_URL}/api/v1/listings/business/?page=1&page_size=10`);
    check(res, {
      'api listings 200': (r) => r.status === 200,
      'api listings has pagination envelope': (r) => {
        try {
          const body = JSON.parse(r.body);
          return 'results' in body && 'count' in body;
        } catch {
          return false;
        }
      },
    });
  });

  sleep(1);
}
