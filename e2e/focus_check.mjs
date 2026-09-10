import { chromium } from '@playwright/test';

const BASE_URL = process.env.ONETOWNCITY_BASE_URL || 'http://127.0.0.1:8000';
const browser = await chromium.launch();
const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
const page = await context.newPage();
await page.goto(BASE_URL + '/', { waitUntil: 'networkidle' });

for (let i = 0; i < 6; i++) {
  await page.keyboard.press('Tab');
  const info = await page.evaluate(() => {
    const el = document.activeElement;
    if (!el) return null;
    const s = getComputedStyle(el);
    return {
      tag: el.tagName, name: el.textContent?.trim().slice(0, 30),
      outline: s.outlineStyle + ' ' + s.outlineWidth + ' ' + s.outlineColor,
      boxShadow: s.boxShadow,
      background: s.backgroundColor,
      border: s.borderStyle + ' ' + s.borderWidth + ' ' + s.borderColor,
    };
  });
  console.log(JSON.stringify(info));
}
await browser.close();
