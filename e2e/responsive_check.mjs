// One-off responsive/overflow verification for the WEB PRODUCTION ASSET
// GATE task — checks the required width matrix + light/dark + zoom levels
// for horizontal overflow on the home page, using Playwright's own bundled
// Chromium (see playwright.config.ts's rationale — no system Chrome here).
import { chromium } from '@playwright/test';

const BASE_URL = process.env.ONETOWNCITY_BASE_URL || 'http://127.0.0.1:8010';
const WIDTHS = [320, 360, 390, 412, 480, 768, 1024, 1280, 1440, 1920];
const COLOR_SCHEMES = ['light', 'dark'];
const ZOOMS = [1.0, 1.25, 1.5, 1.75, 2.0];

const results = [];

async function checkOverflow(page) {
  return page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
    overflow: document.documentElement.scrollWidth > document.documentElement.clientWidth + 1,
  }));
}

async function main() {
  const browser = await chromium.launch();

  // Pass 1: every required width x both color schemes, zoom = 100%.
  for (const colorScheme of COLOR_SCHEMES) {
    for (const width of WIDTHS) {
      const context = await browser.newContext({ viewport: { width, height: 900 }, colorScheme });
      const page = await context.newPage();
      await page.goto(BASE_URL + '/', { waitUntil: 'networkidle', timeout: 20000 });
      const r = await checkOverflow(page);
      results.push({ width, colorScheme, zoom: 1.0, ...r });
      await context.close();
    }
  }

  // Pass 2: zoom sweep at a fixed representative width (1280, light mode).
  for (const zoom of ZOOMS) {
    const context = await browser.newContext({ viewport: { width: 1280, height: 900 }, colorScheme: 'light' });
    const page = await context.newPage();
    await page.goto(BASE_URL + '/', { waitUntil: 'networkidle', timeout: 20000 });
    await page.evaluate((z) => { document.documentElement.style.zoom = String(z); }, zoom);
    const r = await checkOverflow(page);
    results.push({ width: 1280, colorScheme: 'light', zoom, ...r });
    await context.close();
  }

  // Pass 3: a mobile-narrow zoom sweep too (390, the most overflow-prone width).
  for (const zoom of ZOOMS) {
    const context = await browser.newContext({ viewport: { width: 390, height: 844 }, colorScheme: 'light' });
    const page = await context.newPage();
    await page.goto(BASE_URL + '/', { waitUntil: 'networkidle', timeout: 20000 });
    await page.evaluate((z) => { document.documentElement.style.zoom = String(z); }, zoom);
    const r = await checkOverflow(page);
    results.push({ width: 390, colorScheme: 'light', zoom, note: 'mobile-narrow zoom sweep', ...r });
    await context.close();
  }

  await browser.close();

  console.log(JSON.stringify(results, null, 2));
  const failures = results.filter((r) => r.overflow);
  console.log(`\n${results.length} checks run, ${failures.length} overflow failures.`);
  if (failures.length) {
    console.log('FAILURES:', JSON.stringify(failures, null, 2));
    process.exit(1);
  }
}

main().catch((err) => { console.error(err); process.exit(1); });
