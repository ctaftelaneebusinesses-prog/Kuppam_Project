import { defineConfig, devices } from '@playwright/test';

/**
 * Phase 5 browser QA config. Uses Playwright's own bundled Chromium/WebKit
 * builds (devices presets below), never the OS "chrome"/"msedge" channel —
 * those require root to install via apt and aren't available in this WSL
 * environment non-interactively; the bundled builds need no such install.
 *
 * Targets the local Django dev server only (see baseURL) — never point this
 * at production.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['html', { open: 'never' }], ['list']],
  timeout: 30_000,

  use: {
    baseURL: process.env.ONETOWNCITY_BASE_URL || 'http://127.0.0.1:8000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  projects: [
    {
      name: 'Desktop Chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'Mobile Chromium',
      use: { ...devices['Pixel 7'] },
    },
  ],

  // Reuses an already-running `python manage.py runserver` if one is up on
  // baseURL (the common case); otherwise starts one itself so `npm test`
  // works standalone too.
  webServer: {
    command: 'cd .. && ./venv/bin/python manage.py runserver 127.0.0.1:8000',
    url: process.env.ONETOWNCITY_BASE_URL || 'http://127.0.0.1:8000',
    reuseExistingServer: true,
    timeout: 60_000,
  },
});
