// Phase 5 structured audit: viewport/zoom/theme sweep + axe-core
// accessibility scans + functional flow walkthrough. Writes structured JSON
// + screenshots to e2e/audit-results/ instead of relying on console output,
// so results can be reviewed methodically rather than by scrollback.
//
// Run: node audit.mjs
import { chromium } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import fs from 'fs';
import path from 'path';

const BASE_URL = process.env.BASE_URL || 'http://127.0.0.1:8000';
const OUT_DIR = path.join(process.cwd(), 'audit-results');
const SHOT_DIR = path.join(OUT_DIR, 'screenshots');
fs.mkdirSync(SHOT_DIR, { recursive: true });

const VIEWPORT_WIDTHS = [320, 360, 390, 412, 480, 768, 1024, 1280, 1440, 1920];
const BASE_HEIGHT = 900;

// Representative pages for the full viewport x theme sweep — one of each
// template shape actually in the codebase (see core/urls.py / core/views.py):
// home, populated list, detail, search, thin/empty list, auth form.
const SWEEP_PAGES = [
  { name: 'home', path: '/' },
  { name: 'business-list', path: '/businesses/' },
  { name: 'business-detail', path: '/businesses/adbutha-aharam/' },
  { name: 'search-results', path: '/search/?q=shop' },
  { name: 'job-list', path: '/jobs/' },
  { name: 'scholarships-empty', path: '/scholarships/' },
  { name: 'signin', path: '/signin/' },
];

const ZOOM_PAGES = [
  { name: 'home', path: '/' },
  { name: 'business-detail', path: '/businesses/adbutha-aharam/' },
  { name: 'search-results', path: '/search/?q=shop' },
];
const ZOOM_LEVELS = [100, 125, 150, 175, 200];

// Part 2 — the 21 requested feature flows, mapped to real URLs (core/urls.py).
// Some have zero real data right now (Projects/Scholarships/Lost & Found —
// confirmed via direct DB query, not guessed) — those are noted, not faked.
const FLOWS = [
  { name: 'HOME', path: '/' },
  { name: 'SEARCH', path: '/search/?q=shop' },
  { name: 'CITY_DETECTION', path: '/', note: 'Reverse-geocode requires real geolocation permission grant; browser automation cannot supply a real GPS fix — see manual note in report.' },
  { name: 'MANUAL_CITY_SELECTION', path: '/', interact: 'city-selector' },
  { name: 'BUSINESSES', path: '/businesses/' },
  { name: 'PROPERTIES', path: '/properties/' },
  { name: 'PROJECTS', path: '/projects/', note: 'Zero approved Project rows exist — empty state only.' },
  { name: 'EVENTS', path: '/events/' },
  { name: 'TUITION_CENTERS', path: '/tuition-centers/' },
  { name: 'STUDENT_SERVICES', path: '/student-services/' },
  { name: 'MARKETPLACE', path: '/marketplace/' },
  { name: 'PLACES_TO_VISIT', path: '/places-to-visit/' },
  { name: 'SCHOLARSHIPS', path: '/scholarships/', note: 'Zero approved Scholarship rows exist — empty state only.' },
  { name: 'LOST_AND_FOUND', path: '/lost-found/', note: 'Zero approved LostFound rows exist — empty state only.' },
  { name: 'FAVORITES', path: '/favorites/', note: 'Requires sign-in.' },
  { name: 'REVIEWS', path: '/businesses/adbutha-aharam/', note: 'Reviewed on a real business detail page; posting requires sign-in.' },
  { name: 'COMMENTS', path: '/businesses/adbutha-aharam/', note: 'Same page as reviews; posting requires sign-in.' },
  { name: 'NOTIFICATIONS', path: '/notifications/', note: 'Requires sign-in.' },
  { name: 'ACCOUNT', path: '/signin/' },
  { name: 'LISTING_SUBMISSION', path: '/businesses/', note: 'Submission requires an authenticated, approved Content Provider account — cannot be exercised via anonymous browser automation without real credentials.' },
  { name: 'LISTING_EDITING', path: '/businesses/', note: 'Same auth requirement as submission.' },
];

const report = {
  meta: { baseUrl: BASE_URL, startedAt: new Date().toISOString() },
  viewportSweep: [],
  zoomSweep: [],
  flows: [],
};

function checkOverflow(pageMetrics) {
  return pageMetrics.scrollWidth > pageMetrics.clientWidth + 1;
}

async function collectPageSignals(page) {
  const consoleErrors = [];
  const networkFailures = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error' && !msg.text().startsWith('Failed to load resource:')) {
      consoleErrors.push(msg.text());
    }
  });
  page.on('requestfailed', (req) => {
    if (new URL(req.url()).hostname === '127.0.0.1') {
      networkFailures.push(`${req.method()} ${req.url()} — ${req.failure()?.errorText}`);
    }
  });
  page.on('response', (res) => {
    if (res.status() >= 500 && new URL(res.url()).hostname === '127.0.0.1') {
      networkFailures.push(`${res.request().method()} ${res.url()} — HTTP ${res.status()}`);
    }
  });
  return { consoleErrors, networkFailures };
}

async function runViewportSweep(browser) {
  for (const theme of ['light', 'dark']) {
    for (const width of VIEWPORT_WIDTHS) {
      for (const pageSpec of SWEEP_PAGES) {
        const context = await browser.newContext({ viewport: { width, height: BASE_HEIGHT }, colorScheme: theme });
        const page = await context.newPage();
        const { consoleErrors, networkFailures } = await collectPageSignals(page);
        const entry = { theme, width, page: pageSpec.name, path: pageSpec.path };
        try {
          await page.goto(BASE_URL + pageSpec.path, { waitUntil: 'networkidle', timeout: 20000 });
          if (theme === 'dark') {
            await page.evaluate(() => document.documentElement.setAttribute('data-theme', 'dark'));
            await page.waitForTimeout(150);
          }
          const metrics = await page.evaluate(() => ({
            scrollWidth: document.documentElement.scrollWidth,
            clientWidth: document.documentElement.clientWidth,
          }));
          entry.horizontalOverflow = checkOverflow(metrics);
          entry.scrollWidth = metrics.scrollWidth;
          entry.clientWidth = metrics.clientWidth;

          const shotName = `${pageSpec.name}_${theme}_${width}.png`;
          await page.screenshot({ path: path.join(SHOT_DIR, shotName), fullPage: true });
          entry.screenshot = `screenshots/${shotName}`;

          // Only run the (relatively slow) axe scan once per page/theme at a
          // representative width, not all 10 — a11y violations are almost
          // always width-independent (labels/contrast/alt-text/headings),
          // so this is deliberate sampling, not a gap.
          if (width === 1280) {
            const axeResults = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
            entry.axeViolations = axeResults.violations.map((v) => ({
              id: v.id,
              impact: v.impact,
              description: v.description,
              nodes: v.nodes.length,
              help: v.helpUrl,
            }));
          }
        } catch (err) {
          entry.error = String(err);
        }
        entry.consoleErrors = consoleErrors;
        entry.networkFailures = networkFailures;
        report.viewportSweep.push(entry);
        await context.close();
      }
    }
    console.log(`[audit] viewport sweep: ${theme} theme done`);
  }
}

async function runZoomSweep(browser) {
  for (const pageSpec of ZOOM_PAGES) {
    for (const zoom of ZOOM_LEVELS) {
      const factor = zoom / 100;
      const width = Math.round(1280 / factor);
      const height = Math.round(BASE_HEIGHT / factor);
      const context = await browser.newContext({ viewport: { width, height } });
      const page = await context.newPage();
      const entry = { page: pageSpec.name, zoom, effectiveViewport: `${width}x${height}` };
      try {
        await page.goto(BASE_URL + pageSpec.path, { waitUntil: 'networkidle', timeout: 20000 });
        const metrics = await page.evaluate(() => ({
          scrollWidth: document.documentElement.scrollWidth,
          clientWidth: document.documentElement.clientWidth,
        }));
        entry.horizontalOverflow = checkOverflow(metrics);
        const shotName = `zoom_${pageSpec.name}_${zoom}.png`;
        await page.screenshot({ path: path.join(SHOT_DIR, shotName), fullPage: true });
        entry.screenshot = `screenshots/${shotName}`;
      } catch (err) {
        entry.error = String(err);
      }
      report.zoomSweep.push(entry);
      await context.close();
    }
  }
  console.log('[audit] zoom sweep done');
}

async function runFlows(browser) {
  for (const flow of FLOWS) {
    for (const viewport of [{ name: 'mobile', width: 390, height: 844 }, { name: 'desktop', width: 1280, height: 900 }]) {
      const context = await browser.newContext({ viewport: { width: viewport.width, height: viewport.height } });
      const page = await context.newPage();
      const { consoleErrors, networkFailures } = await collectPageSignals(page);
      const entry = { flow: flow.name, viewport: viewport.name, path: flow.path, note: flow.note || null };
      try {
        const response = await page.goto(BASE_URL + flow.path, { waitUntil: 'networkidle', timeout: 20000 });
        entry.status = response?.status();

        if (flow.interact === 'city-selector') {
          await page.locator('#hkLocationSelector').click();
          await page.waitForTimeout(300);
          entry.modalVisible = await page.locator('#hkLocationModal').isVisible();
          await page.locator('#hkLocationSearch').fill('Kup');
          await page.waitForTimeout(500);
        }

        const metrics = await page.evaluate(() => ({
          scrollWidth: document.documentElement.scrollWidth,
          clientWidth: document.documentElement.clientWidth,
          bodyText: document.body.innerText.slice(0, 500),
        }));
        entry.horizontalOverflow = checkOverflow(metrics);

        const shotName = `flow_${flow.name}_${viewport.name}.png`;
        await page.screenshot({ path: path.join(SHOT_DIR, shotName), fullPage: true });
        entry.screenshot = `screenshots/${shotName}`;
      } catch (err) {
        entry.error = String(err);
      }
      entry.consoleErrors = consoleErrors;
      entry.networkFailures = networkFailures;
      report.flows.push(entry);
      await context.close();
    }
  }
  console.log('[audit] flow walkthrough done');
}

const browser = await chromium.launch();
await runViewportSweep(browser);
await runZoomSweep(browser);
await runFlows(browser);
await browser.close();

report.meta.finishedAt = new Date().toISOString();
fs.writeFileSync(path.join(OUT_DIR, 'report.json'), JSON.stringify(report, null, 2));

// A compact summary is what actually gets read after this — the full
// report.json backs it up with per-page detail.
const overflowIssues = [...report.viewportSweep, ...report.zoomSweep, ...report.flows].filter((e) => e.horizontalOverflow);
const consoleIssues = [...report.viewportSweep, ...report.flows].filter((e) => (e.consoleErrors || []).length);
const networkIssues = [...report.viewportSweep, ...report.flows].filter((e) => (e.networkFailures || []).length);
const a11yEntries = report.viewportSweep.filter((e) => e.axeViolations && e.axeViolations.length);
const errored = [...report.viewportSweep, ...report.zoomSweep, ...report.flows].filter((e) => e.error);

const summary = {
  totals: {
    viewportSweepRuns: report.viewportSweep.length,
    zoomSweepRuns: report.zoomSweep.length,
    flowRuns: report.flows.length,
  },
  horizontalOverflowCount: overflowIssues.length,
  horizontalOverflowSamples: overflowIssues.slice(0, 30).map((e) => ({ page: e.page || e.flow, theme: e.theme, width: e.width, zoom: e.zoom, viewport: e.viewport, scrollWidth: e.scrollWidth, clientWidth: e.clientWidth })),
  consoleErrorCount: consoleIssues.length,
  consoleErrorSamples: consoleIssues.slice(0, 20).map((e) => ({ page: e.page || e.flow, theme: e.theme, width: e.width, viewport: e.viewport, errors: e.consoleErrors })),
  networkFailureCount: networkIssues.length,
  networkFailureSamples: networkIssues.slice(0, 20).map((e) => ({ page: e.page || e.flow, theme: e.theme, width: e.width, viewport: e.viewport, failures: e.networkFailures })),
  a11yViolationPages: a11yEntries.map((e) => ({ page: e.page, theme: e.theme, violations: e.axeViolations })),
  hardErrors: errored.map((e) => ({ page: e.page || e.flow, theme: e.theme, width: e.width || e.zoom, viewport: e.viewport, error: e.error })),
};
fs.writeFileSync(path.join(OUT_DIR, 'summary.json'), JSON.stringify(summary, null, 2));
console.log('[audit] done. See audit-results/summary.json and audit-results/report.json');
