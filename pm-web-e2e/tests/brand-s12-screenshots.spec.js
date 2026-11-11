import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { pair, switchScenario } from './helpers/pairing.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/**
 * The S12 screenshot set of the brand: the rail head expanded and collapsed, the pairing page and
 * the tab icon on `fully-onboarded` and on `running-work`, each in German and English, light and
 * dark, at the desktop width and on the phone.
 *
 * Every screenshot is written to `test-results/s12-brand/{surface}-{lang}-{theme}-{width}.png`
 * (`test-results` is not committed), and the spec asserts each file was written and is not empty,
 * so a capture that silently failed turns the run red.
 */

/** The directory the S12 screenshots are written to. */
const SHOT_DIR = path.resolve(__dirname, '../test-results/s12-brand');

/** The two interface locales. */
const LOCALES = ['de', 'en'];

/** The two themes. */
const THEMES = ['light', 'dark'];

/** The desktop width and the phone width. */
const WIDTHS = [
  { width: 1280, height: 800 },
  { width: 390, height: 844 },
];

/** The desktop viewport, where the rail and its language toggle are drawn. */
const DESKTOP = WIDTHS[0];

/** The size the tab icon is rendered at for its screenshot. */
const ICON_VIEWPORT = { width: 64, height: 64 };

/**
 * @param {string} surface the surface the screenshot shows
 * @param {string} locale the interface locale
 * @param {string} theme the theme
 * @param {number} width the width of the page the surface was read from
 * @returns the path of the screenshot file
 */
function shotPath(surface, locale, theme, width) {
  return path.join(SHOT_DIR, `${surface}-${locale}-${theme}-${width}.png`);
}

/**
 * Asserts that a screenshot file was written and holds data.
 *
 * @param {string} file the screenshot file
 */
function expectWritten(file) {
  expect(fs.existsSync(file), `${file} was not written`).toBe(true);
  expect(fs.statSync(file).size, `${file} is empty`).toBeGreaterThan(0);
}

/**
 * Waits until the app has initialized its runtime, which follows the catalogue load, so the page
 * is rendered in the locale the browser stored before the navigation.
 *
 * @param {import('@playwright/test').Page} page the page to wait for
 */
async function waitForApp(page) {
  await page.waitForFunction(
    async () => {
      const { runtimeStore } = await import('/src/lib/stores/runtime.ts');
      let initialized = false;
      const unsubscribe = runtimeStore.subscribe((state) => {
        initialized = state.isInitialized;
      });
      unsubscribe();
      return initialized;
    },
    undefined,
    { timeout: 20000 },
  );
}

/**
 * Stores the interface language and the theme before the next document load, so the app boots in
 * them: the theme is read before the first paint, the language with the catalogue.
 *
 * @param {import('@playwright/test').Page} page the page of the running app
 * @param {string} locale the interface locale
 * @param {string} theme the theme
 */
async function storeChoices(page, locale, theme) {
  await page.evaluate(
    ({ activeLocale, activeTheme }) => {
      localStorage.setItem('pm_locale', activeLocale);
      localStorage.setItem('pm_theme', activeTheme);
    },
    { activeLocale: locale, activeTheme: theme },
  );
}

/**
 * Switches the mock scenario, pairs at the desktop width and reloads the shell in the requested
 * language and theme.
 *
 * @param {import('@playwright/test').Page} page the page of the running app
 * @param {import('@playwright/test').APIRequestContext} request the request fixture
 * @param {{ scenario: string, locale: string, theme: string }} choice what the shell opens in
 */
async function openShell(page, request, { scenario, locale, theme }) {
  await page.setViewportSize(DESKTOP);
  await switchScenario(request, scenario);
  await pair(page, request);
  await storeChoices(page, locale, theme);
  await page.reload();
  await waitForApp(page);

  await expect(page.locator('html')).toHaveAttribute('data-theme', theme);
  // The toggle offers the other language, so it names the one that is not active.
  await expect(page.getByTestId('language-toggle')).toHaveText(locale === 'en' ? 'DE' : 'EN');
  await expect(page.getByTestId('nav-rail').getByTestId('brand-mark')).toBeVisible();
}

/**
 * Writes the tab icon the page's icon link names: the served icon file, rendered on its own.
 *
 * The browser cannot screenshot its own tab strip, so the icon is opened beside the page and
 * captured as the document draws it. The link first has to agree with the count it follows, so
 * the icon is read once the app has applied it.
 *
 * @param {import('@playwright/test').Page} page the paired page
 * @param {string} theme the theme the page shows
 * @param {string} file the screenshot file
 */
async function captureTabIcon(page, theme, file) {
  const openDecisions = await page.evaluate(async () => {
    const { runtimeStore } = await import('/src/lib/stores/runtime.ts');
    let count = 0;
    const unsubscribe = runtimeStore.subscribe((state) => {
      count = state.openDecisionsCount;
    });
    unsubscribe();
    return count;
  });
  const expected = openDecisions > 0 ? '/brand-mark-attention.svg' : '/brand-mark.svg';
  const link = page.locator('link[rel="icon"]');
  await expect(link).toHaveAttribute('href', expected);

  const iconPage = await page.context().newPage();
  try {
    await iconPage.setViewportSize(ICON_VIEWPORT);
    await iconPage.emulateMedia({ colorScheme: theme });
    const response = await iconPage.goto(expected);
    expect(response?.ok(), `${expected} was not served`).toBe(true);
    await expect(iconPage.locator('svg').first()).toBeVisible();
    await iconPage.screenshot({ path: file, animations: 'disabled' });
  } finally {
    await iconPage.close();
  }
}

test.describe('S12 brand screenshots', () => {
  test.afterAll(async ({ request }) => {
    try {
      await switchScenario(request, 'web-pairing');
    } catch {
      // Best-effort cleanup.
    }
  });

  for (const locale of LOCALES) {
    for (const theme of THEMES) {
      for (const { width, height } of WIDTHS) {
        const where = `${locale}, ${theme}, ${width}px`;

        test(`rail head and tab icon on fully-onboarded (${where})`, async ({ page, request }) => {
          await openShell(page, request, { scenario: 'fully-onboarded', locale, theme });
          const rail = page.getByTestId('nav-rail');

          // The phone has no rail: its screenshot shows the head the phone draws instead.
          await page.setViewportSize({ width, height });
          await expect(page.getByTestId('top-bar')).toBeVisible();
          const expanded = shotPath('rail-head-expanded', locale, theme, width);
          await page.screenshot({ path: expanded, animations: 'disabled' });

          // The rail is collapsed where it is drawn, then the page is shown at the width again.
          await page.setViewportSize(DESKTOP);
          await page.getByTestId('rail-toggle').click();
          await expect(rail).toHaveClass(/nav-rail--collapsed/);
          await page.setViewportSize({ width, height });
          await expect(page.getByTestId('top-bar')).toBeVisible();
          const collapsed = shotPath('rail-head-collapsed', locale, theme, width);
          await page.screenshot({ path: collapsed, animations: 'disabled' });

          const icon = shotPath('tab-icon-fully-onboarded', locale, theme, width);
          await captureTabIcon(page, theme, icon);

          expectWritten(expanded);
          expectWritten(collapsed);
          expectWritten(icon);
        });

        test(`tab icon on running-work (${where})`, async ({ page, request }) => {
          await openShell(page, request, { scenario: 'running-work', locale, theme });
          await page.setViewportSize({ width, height });
          await expect(page.getByTestId('top-bar')).toBeVisible();

          const icon = shotPath('tab-icon-running-work', locale, theme, width);
          await captureTabIcon(page, theme, icon);

          expectWritten(icon);
        });

        test(`pairing page (${where})`, async ({ page, request }) => {
          await page.setViewportSize({ width, height });
          await switchScenario(request, 'web-pairing');
          await page.goto('/');
          await page.evaluate(() => localStorage.clear());
          await storeChoices(page, locale, theme);
          await page.goto('/pair');
          await waitForApp(page);

          await expect(page.locator('html')).toHaveAttribute('data-theme', theme);
          const pairView = page.getByTestId('pairing-view');
          await expect(pairView).toBeVisible();
          await expect(pairView.getByTestId('brand-mark')).toBeVisible();

          const file = shotPath('pairing', locale, theme, width);
          await page.screenshot({ path: file, animations: 'disabled' });

          expectWritten(file);
        });
      }
    }
  }
});
