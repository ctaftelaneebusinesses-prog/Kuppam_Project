// ONETOWNCITY FINAL WEB UI RELEASE GATE — real-browser regression sweep.
// READ-ONLY: navigates and inspects only, never submits a form that would
// create data (this session already had one incident of writing test data
// to the shared database via manage.py shell — this script is scoped to
// avoid any repeat of that: GET navigation, click-through, and evaluate()
// calls that only read the DOM).
import { chromium } from '@playwright/test';
import fs from 'fs';

const BASE_URL = process.env.ONETOWNCITY_BASE_URL || 'http://127.0.0.1:8000';
const WIDTHS = [320, 360, 390, 412, 480, 768, 1024, 1280, 1440, 1920];
const THEMES = ['light', 'dark'];
const PUBLIC_PAGES = [
  ['Home', '/'],
  ['Search', '/search/?q=shop'],
  ['Businesses', '/businesses/'],
  ['Properties', '/properties/'],
  ['Projects', '/projects/'],
  ['Events', '/events/'],
  ['Tuition Centers', '/tuition-centers/'],
  ['Student Services', '/student-services/'],
  ['Marketplace', '/marketplace/'],
  ['Places to Visit', '/places-to-visit/'],
  ['Scholarships', '/scholarships/'],
  ['Lost & Found', '/lost-found/'],
  ['Privacy', '/privacy-policy/'],
  ['Terms', '/terms-of-service/'],
];
const AUTH_PAGES = [
  ['Favorites', '/favorites/'],
  ['Notifications', '/notifications/'],
  ['Account', '/dashboard/profile/'],
  ['Account deletion', '/dashboard/profile/delete/'],
  ['Listing submission', '/dashboard/posts/new/'],
];

const results = { overflow: [], consoleErrors: [], brokenImages: [], authRedirects: [], screenshots: [] };

async function setTheme(page, theme) {
  await page.evaluate((t) => {
    document.documentElement.setAttribute('data-theme', t);
    try { localStorage.setItem('hkTheme', t); } catch (e) {}
  }, theme);
}

async function checkPage(page, label, path) {
  const consoleErrs = [];
  const handler = (msg) => { if (msg.type() === 'error') consoleErrs.push(msg.text().slice(0, 200)); };
  page.on('console', handler);

  const resp = await page.goto(BASE_URL + path, { waitUntil: 'networkidle', timeout: 20000 });
  const status = resp ? resp.status() : null;

  for (const theme of THEMES) {
    await setTheme(page, theme);
    await page.waitForTimeout(50);
    const metrics = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
      brokenImages: Array.from(document.images).filter((img) => img.complete && img.naturalWidth === 0).map((img) => img.src).slice(0, 5),
    }));
    const overflow = metrics.scrollWidth > metrics.clientWidth + 1;
    if (overflow) results.overflow.push({ label, path, width: page.viewportSize().width, theme, scrollWidth: metrics.scrollWidth, clientWidth: metrics.clientWidth });
    if (metrics.brokenImages.length) results.brokenImages.push({ label, path, theme, images: metrics.brokenImages });
  }

  page.off('console', handler);
  if (consoleErrs.length) results.consoleErrors.push({ label, path, status, errors: [...new Set(consoleErrs)] });
  return status;
}

async function main() {
  fs.mkdirSync('screens', { recursive: true });
  const browser = await chromium.launch();

  // Pass 1: every public page x every required width, both themes checked per load.
  for (const [label, path] of PUBLIC_PAGES) {
    for (const width of WIDTHS) {
      const context = await browser.newContext({ viewport: { width, height: 900 } });
      const page = await context.newPage();
      const status = await checkPage(page, label, path);
      if (width === 390 || width === 1920) {
        const file = `screens/${label.replace(/[^a-z0-9]/gi, '_')}_${width}.png`;
        await page.screenshot({ path: file });
        results.screenshots.push(file);
      }
      await context.close();
      if (status !== 200) console.log(`NON-200: ${label} ${path} -> ${status}`);
    }
  }

  // Pass 2: unauthenticated auth-required pages must redirect cleanly, not crash.
  for (const [label, path] of AUTH_PAGES) {
    const context = await browser.newContext({ viewport: { width: 390, height: 844 } });
    const page = await context.newPage();
    const resp = await page.goto(BASE_URL + path, { waitUntil: 'networkidle', timeout: 20000 });
    results.authRedirects.push({ label, path, finalUrl: page.url(), status: resp ? resp.status() : null });
    await context.close();
  }

  // Pass 3: click-through into a real listing detail page (Business) for
  // review/comment widget + zoom sweep, mirroring the existing smoke test's
  // click-through pattern (never hardcodes a slug).
  {
    const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
    const page = await context.newPage();
    await page.goto(BASE_URL + '/businesses/', { waitUntil: 'networkidle' });
    let detailUrl = null;
    const links = await page.locator('a[href^="/businesses/"]').evaluateAll((as) => as.map((a) => a.getAttribute('href')).filter((h) => h && h !== '/businesses/'));
    if (links.length) {
      detailUrl = links[0];
      await page.goto(BASE_URL + detailUrl, { waitUntil: 'networkidle' });
      const hasReviewSection = await page.locator('text=/Review/i').count();
      const hasCommentSection = await page.locator('text=/Comment/i').count();
      results.listingDetail = { url: detailUrl, hasReviewSection: hasReviewSection > 0, hasCommentSection: hasCommentSection > 0 };
      await page.screenshot({ path: 'screens/business_detail_1280.png' });
      results.screenshots.push('screens/business_detail_1280.png');

      // zoom sweep on the detail page at mobile + desktop widths
      for (const width of [390, 1280]) {
        await page.setViewportSize({ width, height: 900 });
        for (const zoom of [1.0, 1.25, 1.5, 1.75, 2.0]) {
          await page.evaluate((z) => { document.documentElement.style.zoom = String(z); }, zoom);
          await page.waitForTimeout(100);
          const m = await page.evaluate(() => ({ scrollWidth: document.documentElement.scrollWidth, clientWidth: document.documentElement.clientWidth }));
          if (m.scrollWidth > m.clientWidth + 1) {
            results.overflow.push({ label: 'Business detail (zoom)', path: detailUrl, width, zoom, scrollWidth: m.scrollWidth, clientWidth: m.clientWidth });
          }
        }
        await page.evaluate(() => { document.documentElement.style.zoom = '1'; });
      }
    } else {
      results.listingDetail = { error: 'no business listing links found to click through' };
    }
    await context.close();
  }

  // Pass 4: keyboard navigation + focus visibility on the home page.
  {
    const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
    const page = await context.newPage();
    await page.goto(BASE_URL + '/', { waitUntil: 'networkidle' });
    const tabStops = [];
    for (let i = 0; i < 8; i++) {
      await page.keyboard.press('Tab');
      const info = await page.evaluate(() => {
        const el = document.activeElement;
        if (!el || el === document.body) return null;
        const style = getComputedStyle(el);
        return {
          tag: el.tagName,
          hasOutline: style.outlineStyle !== 'none' && style.outlineWidth !== '0px',
          accessibleName: el.getAttribute('aria-label') || el.textContent?.trim().slice(0, 40) || el.getAttribute('alt') || null,
        };
      });
      if (info) tabStops.push(info);
    }
    results.keyboardNav = tabStops;
    await context.close();
  }

  // Pass 5: city selector modal opens via keyboard-reachable button.
  {
    const context = await browser.newContext({ viewport: { width: 390, height: 844 } });
    const page = await context.newPage();
    await page.goto(BASE_URL + '/', { waitUntil: 'networkidle' });
    const selector = page.locator('#hkLocationSelector');
    const exists = await selector.count();
    let modalOpened = false;
    if (exists) {
      await selector.click();
      await page.waitForTimeout(300);
      modalOpened = await page.locator('#hkLocationModal').isVisible().catch(() => false);
      if (modalOpened) await page.screenshot({ path: 'screens/city_modal_390.png' });
    }
    results.cityModal = { selectorFound: exists > 0, modalOpened };
    await context.close();
  }

  await browser.close();
  fs.writeFileSync('final_ui_gate_results.json', JSON.stringify(results, null, 2));
  console.log(JSON.stringify({
    overflowCount: results.overflow.length,
    consoleErrorPages: results.consoleErrors.length,
    brokenImagePages: results.brokenImages.length,
    authRedirects: results.authRedirects,
    listingDetail: results.listingDetail,
    keyboardNavStops: results.keyboardNav?.length,
    cityModal: results.cityModal,
  }, null, 2));
}

main().catch((err) => { console.error(err); process.exit(1); });
