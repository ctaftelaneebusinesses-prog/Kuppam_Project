// Part 6 performance baseline — per-endpoint p50/p95/p99, request count,
// and response size, measured before any further optimization (this run
// happens AFTER the confirmed N+1 fix in core/api/views.py, which is
// documented separately with its own before/after query-count numbers via
// Django's CaptureQueriesContext — this script measures wall-clock HTTP
// latency, which also includes network RTT to the Supabase pooler).
//
// Run: k6 run --vus 3 --iterations 60 baseline.js
import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL } from './lib/config.js';

const trends = {
  homepage: new Trend('homepage_duration', true),
  search: new Trend('search_duration', true),
  citySelection: new Trend('city_selection_duration', true),
  businessList: new Trend('business_list_duration', true),
  businessDetail: new Trend('business_detail_duration', true),
  apiListings: new Trend('api_listings_duration', true),
};

export const options = {
  vus: 3,
  iterations: 60,
};

export default function () {
  let res = http.get(`${BASE_URL}/`);
  trends.homepage.add(res.timings.duration, { size: res.body.length });
  check(res, { 'homepage 200': (r) => r.status === 200 });

  res = http.get(`${BASE_URL}/search/?q=shop`);
  trends.search.add(res.timings.duration);
  check(res, { 'search 200': (r) => r.status === 200 });

  res = http.get(`${BASE_URL}/api/locations/search/?q=Kup`);
  trends.citySelection.add(res.timings.duration);
  check(res, { 'city search 200': (r) => r.status === 200 });

  res = http.get(`${BASE_URL}/businesses/`);
  trends.businessList.add(res.timings.duration);
  check(res, { 'business_list 200': (r) => r.status === 200 });

  res = http.get(`${BASE_URL}/businesses/adbutha-aharam/`);
  trends.businessDetail.add(res.timings.duration);
  check(res, { 'business_detail 200': (r) => r.status === 200 });

  res = http.get(`${BASE_URL}/api/v1/listings/business/?page=1&page_size=20`);
  trends.apiListings.add(res.timings.duration);
  check(res, { 'api listings 200': (r) => r.status === 200 });
}

export function handleSummary(data) {
  const rows = {};
  for (const [key, metricName] of Object.entries({
    homepage: 'homepage_duration',
    search: 'search_duration',
    citySelection: 'city_selection_duration',
    businessList: 'business_list_duration',
    businessDetail: 'business_detail_duration',
    apiListings: 'api_listings_duration',
  })) {
    const m = data.metrics[metricName];
    if (!m) continue;
    rows[key] = {
      count: m.values.count,
      p50_ms: Math.round(m.values['p(50)'] || m.values.med),
      p95_ms: Math.round(m.values['p(95)']),
      p99_ms: Math.round(m.values['p(99)'] ?? m.values['p(95)']),
      avg_ms: Math.round(m.values.avg),
      min_ms: Math.round(m.values.min),
      max_ms: Math.round(m.values.max),
    };
  }
  return {
    stdout: JSON.stringify(rows, null, 2) + '\n',
    '../../perf-baseline-results.json': JSON.stringify(rows, null, 2),
  };
}
