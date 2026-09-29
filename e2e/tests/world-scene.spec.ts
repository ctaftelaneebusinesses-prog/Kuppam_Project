import { test, expect } from '@playwright/test';

/**
 * Category pages decorate their outer margins with illustration pieces
 * (static/js/world-scene.js). On pages with a hero banner those pieces must
 * sit entirely below the banner, never beside or around it — the banner
 * shows only its own photo. Every listing page shares one hero
 * (listing_page_base.html, .hk-lp-hero).
 */
for (const path of ['/businesses/', '/jobs/', '/events/', '/properties/']) {
  test(`illustrations stay below the hero on ${path}`, async ({ page }) => {
    await page.goto(path, { waitUntil: 'load' });
    await expect(page.locator('.world-bg.is-active.is-below-hero')).toHaveCount(1);

    const result = await page.evaluate(() => {
      const hero = document.querySelector('.hk-lp-hero')!;
      const heroBottom = hero.getBoundingClientRect().bottom;
      const tops = [...document.querySelectorAll('.world-actor')].map(
        (el) => el.getBoundingClientRect().top,
      );
      return { heroBottom, tops };
    });

    expect(result.tops.length).toBeGreaterThan(0);
    for (const top of result.tops) {
      expect(top).toBeGreaterThanOrEqual(result.heroBottom);
    }
  });
}

test('illustration layer never lengthens the page', async ({ page }) => {
  await page.goto('/businesses/', { waitUntil: 'load' });
  const { layerHeight, docHeight } = await page.evaluate(() => ({
    layerHeight: (document.querySelector('.world-bg') as HTMLElement).offsetHeight,
    docHeight: document.documentElement.scrollHeight,
  }));
  expect(layerHeight).toBeLessThanOrEqual(docHeight);
});
