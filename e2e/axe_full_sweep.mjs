import { chromium } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const BASE_URL = process.env.ONETOWNCITY_BASE_URL || 'http://127.0.0.1:8000';
const pages = [
  '/', '/search/?q=shop', '/businesses/', '/properties/', '/projects/', '/events/',
  '/tuition-centers/', '/student-services/', '/marketplace/', '/places-to-visit/',
  '/scholarships/', '/lost-found/', '/privacy-policy/', '/terms-of-service/',
];

const browser = await chromium.launch();
let totalViolations = 0;
for (const path of pages) {
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await context.newPage();
  await page.goto(BASE_URL + path, { waitUntil: 'networkidle' });
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
  console.log(`=== ${path}: ${results.violations.length} violation(s) ===`);
  for (const v of results.violations) {
    totalViolations++;
    console.log(`  [${v.id}] (${v.impact}) ${v.description} — ${v.nodes.length} node(s)`);
  }
  await context.close();
}
console.log('TOTAL VIOLATIONS:', totalViolations);
await browser.close();
