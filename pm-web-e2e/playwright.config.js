import { defineConfig, devices } from '@playwright/test';
import { BASE_URL, WEB_PORT } from './tests/helpers/ports.js';

const customPortsPassed = Boolean(process.env.PM_WEB_PORT || process.env.PM_MOCK_WEB_PORT);

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  timeout: 30000,
  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
  },
  globalSetup: './global-setup.js',
  webServer: {
    command: `npm run dev --prefix ../pm-web-app -- --port ${WEB_PORT}`,
    port: WEB_PORT,
    reuseExistingServer: !process.env.CI && !customPortsPassed,
    timeout: 30000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
