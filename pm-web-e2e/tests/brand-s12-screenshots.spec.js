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
 *
 * Beside them it writes the comparison sheets of the marks, read from their sources on disk because
 * they are not served and drawn in a plain page on a white and a dark ground: the kind marks beside
 * the waiting mark and the badge at 16 and 24 px (`kind-marks-{size}.png`), and the favourite marks
 * against the badge at 16 px in monochrome (`favourite-vs-badge-16.png`).
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

/** The web app, whose icon and mark sources the comparison sheets read from disk. */
const APP_DIR = path.resolve(__dirname, '../../pm-web-app');

/** The interface icons of the web app. */
const UI_ICON_DIR = path.join(APP_DIR, 'src/lib/icons/ui');

/** The component that draws the waiting mark; the mark has no SVG file of its own. */
const WAITING_MARK_FILE = path.join(APP_DIR, 'src/lib/components/WaitingMark.svelte');

/** The badge, the browser-tab copy of the brand mark. */
const BADGE_FILE = path.join(APP_DIR, 'static/brand-mark.svg');

/** The sizes the kind marks are compared at. */
const KIND_MARK_SIZES = [16, 24];

/** The size the favourite marks are compared with the badge at. */
const FAVOURITE_SIZE = 16;

/** The grounds every comparison sheet is drawn on: the marks take the ground's text colour. */
const GROUNDS = [
  { name: 'white', background: '#ffffff', color: '#1f1f1f' },
  { name: 'dark', background: '#1f1f1f', color: '#f2f2f2' },
];

/**
 * @param {string} file an SVG file
 * @returns the SVG source of the file
 */
function readSvg(file) {
  return fs.readFileSync(file, 'utf8').trim();
}

/**
 * @param {string} name the name of an interface icon, without its extension
 * @returns the SVG source of the icon
 */
function uiIcon(name) {
  return readSvg(path.join(UI_ICON_DIR, `${name}.svg`));
}

/**
 * The clipboard drawing of the waiting mark, cut out of its component, which carries no SVG file of
 * its own.
 *
 * @returns the SVG source of the waiting mark
 */
function waitingMark() {
  const source = fs.readFileSync(WAITING_MARK_FILE, 'utf8');
  const start = source.indexOf('<svg');
  const end = source.indexOf('</svg>', start);
  expect(start, 'WaitingMark.svelte holds no <svg>').toBeGreaterThanOrEqual(0);
  expect(end, 'WaitingMark.svelte holds no </svg>').toBeGreaterThan(start);
  return source.slice(start, end + '</svg>'.length);
}

/** @returns the SVG source of the badge */
function badge() {
  return readSvg(BADGE_FILE);
}

/**
 * @param {{ label: string, svg: string }} mark a mark and its name
 * @returns the cell that draws the mark above its name
 */
function sheetCell({ label, svg }) {
  return `<div class="cell"><div class="mark">${svg}</div><span>${label}</span></div>`;
}

/**
 * @param {{ name: string, background: string, color: string }} ground the ground
 * @param {string} cells the cells drawn on it
 * @returns the row that draws the cells on the ground
 */
function sheetGround({ name, background, color }, cells) {
  const style = `background:${background};color:${color}`;
  return `<div class="ground" data-ground="${name}" style="${style}">${cells}</div>`;
}

/**
 * A plain page that draws the marks side by side at one size, once on each ground, each mark named
 * beneath it.
 *
 * The waiting mark is drawn in its rest state, the state reduced motion holds: the open dots filled
 * and the ticks undrawn. A monochrome sheet takes the colour out of every mark, the badge included.
 *
 * @param {{ label: string, svg: string }[]} marks the marks, in the order they are drawn
 * @param {number} size the size every mark is drawn at, in CSS pixels
 * @param {boolean} monochrome whether the sheet is drawn without colour
 * @returns the HTML of the page
 */
function comparisonSheet(marks, size, monochrome) {
  const cells = marks.map(sheetCell).join('');
  const grounds = GROUNDS.map((ground) => sheetGround(ground, cells)).join('');
  return `<!doctype html>
<html>
<head>
<style>
  body { margin: 0; background: #ffffff; font: 11px sans-serif; }
  .sheet { display: inline-block; ${monochrome ? 'filter: grayscale(1);' : ''} }
  .ground { display: flex; align-items: flex-end; gap: 16px; padding: 12px; }
  .cell { display: flex; flex-direction: column; align-items: center; gap: 6px; }
  .mark svg { display: block; width: ${size}px; height: ${size}px; }
  .waiting-mark__dot { fill: currentColor; stroke: none; }
  .waiting-mark__tick { display: none; }
</style>
</head>
<body><div class="sheet" data-testid="comparison-sheet">${grounds}</div></body>
</html>`;
}

/**
 * Draws a comparison sheet in a plain page and writes it.
 *
 * @param {import('@playwright/test').Page} page the page to draw in
 * @param {{ label: string, svg: string }[]} marks the marks, in the order they are drawn
 * @param {{ size: number, monochrome: boolean }} drawing the size and whether colour is taken out
 * @param {string} file the screenshot file
 */
async function captureSheet(page, marks, { size, monochrome }, file) {
  await page.setContent(comparisonSheet(marks, size, monochrome));
  const sheet = page.getByTestId('comparison-sheet');
  await expect(sheet).toBeVisible();
  // Every mark is drawn once on every ground, at the size asked for.
  const drawn = sheet.locator('.mark > svg');
  await expect(drawn).toHaveCount(marks.length * GROUNDS.length);
  for (const svg of await drawn.all()) {
    const box = await svg.boundingBox();
    expect(box?.width).toBe(size);
    expect(box?.height).toBe(size);
  }
  await sheet.screenshot({ path: file, animations: 'disabled' });
}

/** @returns the three kind marks beside the waiting mark and the badge */
function kindMarks() {
  return [
    { label: 'kind-plan', svg: uiIcon('kind-plan') },
    { label: 'kind-task', svg: uiIcon('kind-task') },
    { label: 'kind-report', svg: uiIcon('kind-report') },
    { label: 'waiting mark', svg: waitingMark() },
    { label: 'badge', svg: badge() },
  ];
}

/** @returns the two favourite marks beside the badge */
function favouriteMarks() {
  return [
    { label: 'act-favourite', svg: uiIcon('act-favourite') },
    { label: 'act-favourite-on', svg: uiIcon('act-favourite-on') },
    { label: 'badge', svg: badge() },
  ];
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

test.describe('S12 mark comparison sheets', () => {
  for (const size of KIND_MARK_SIZES) {
    test(`kind marks beside the waiting mark and the badge at ${size}px`, async ({ page }) => {
      const file = path.join(SHOT_DIR, `kind-marks-${size}.png`);
      await captureSheet(page, kindMarks(), { size, monochrome: false }, file);

      expectWritten(file);
    });
  }

  test(`favourite marks against the badge at ${FAVOURITE_SIZE}px in monochrome`, async ({
    page,
  }) => {
    const file = path.join(SHOT_DIR, `favourite-vs-badge-${FAVOURITE_SIZE}.png`);
    await captureSheet(page, favouriteMarks(), { size: FAVOURITE_SIZE, monochrome: true }, file);

    expectWritten(file);
  });
});
