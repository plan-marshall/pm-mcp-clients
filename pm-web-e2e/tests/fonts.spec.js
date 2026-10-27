import { test, expect } from '@playwright/test';
import { switchScenario, pair } from './helpers/pairing.js';

/**
 * The self-hosted Inter face of VD-P-03.
 *
 * The app carries the font in its own bundle: the browser must fetch it from the app's origin and
 * must never ask a font service for one. The SIL Open Font License ships beside the file.
 */
test.describe('Self-hosted Inter (VD-P-03)', () => {
  let fontRequests;

  test.beforeEach(async ({ page, request }) => {
    fontRequests = [];
    page.on('request', (browserRequest) => {
      if (
        browserRequest.resourceType() === 'font' ||
        /\.(woff2?|ttf|otf)(\?.*)?$/.test(new URL(browserRequest.url()).pathname)
      ) {
        fontRequests.push(browserRequest.url());
      }
    });
    await switchScenario(request, 'fully-onboarded');
    await pair(page, request);
  });

  test.afterAll(async ({ request }) => {
    try {
      await switchScenario(request, 'web-pairing');
    } catch {
      // Best-effort reset for the suites that follow
    }
  });

  test('serves the face from the app origin and asks no font service', async ({ page }) => {
    await page.goto('/overview');
    await page.evaluate(() => document.fonts.ready);

    expect(fontRequests.length).toBeGreaterThan(0);
    const origin = new URL(page.url()).origin;
    const foreign = fontRequests.filter((url) => !url.startsWith(`${origin}/`));
    expect(foreign).toEqual([]);

    const paths = fontRequests.map((url) => new URL(url).pathname);
    expect(paths).toContain('/fonts/InterVariable.woff2');

    await expect
      .poll(() =>
        page.evaluate(() => getComputedStyle(document.body).fontFamily.startsWith('Inter')),
      )
      .toBe(true);

    await expect.poll(() => page.evaluate(() => document.fonts.check('14px Inter'))).toBe(true);
  });

  test('ships the SIL Open Font License beside the file', async ({ request }) => {
    const licence = await request.get('/fonts/OFL.txt');

    expect(licence.ok()).toBe(true);
    expect(await licence.text()).toContain('SIL OPEN FONT LICENSE');
  });
});
