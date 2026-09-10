import { chromium } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const BASE_URL = process.env.BASE_URL || 'http://127.0.0.1:8000';
const pages = ['/', '/businesses/', '/businesses/adbutha-aharam/', '/search/?q=shop', '/jobs/', '/scholarships/', '/signin/'];

const browser = await chromium.launch();
for (const path of pages) {
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await context.newPage();
  await page.goto(BASE_URL + path, { waitUntil: 'networkidle' });
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
  console.log(`\n=== ${path} ===`);
  for (const v of results.violations) {
    console.log(`\n[${v.id}] ${v.description}`);
    for (const node of v.nodes) {
      console.log('  target:', JSON.stringify(node.target));
      console.log('  html:', node.html.slice(0, 200));
      console.log('  failureSummary:', node.failureSummary);
    }
  }
  await context.close();
}
await browser.close();
