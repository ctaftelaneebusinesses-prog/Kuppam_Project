// Example ramping-VU load test (not just a smoke check) against the
// Business listing API — the pattern to copy for Property/Project/Event
// once real load numbers are agreed for Phase 5. Capped deliberately low
// (max 10 VUs, ~1 minute) so it's safe to run against a local dev server;
// scale stages up only for an actual staging run, never production.
//
// Run:  k6 run perf/k6/load-business-listing.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from './lib/config.js';

export const options = {
  stages: [
    { duration: '10s', target: 5 },  // ramp up
    { duration: '30s', target: 10 }, // hold
    { duration: '10s', target: 0 },  // ramp down
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
  },
};

export default function () {
  const page = Math.floor(Math.random() * 3) + 1; // pages 1-3, avoids every VU hammering identical cache-friendly page 1 only
  const res = http.get(`${BASE_URL}/api/v1/listings/business/?page=${page}&page_size=20`);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'has pagination envelope': (r) => {
      try {
        const body = JSON.parse(r.body);
        return 'results' in body && 'count' in body;
      } catch {
        return false;
      }
    },
  });

  sleep(Math.random() * 1 + 0.5); // 0.5-1.5s think time between requests
}
