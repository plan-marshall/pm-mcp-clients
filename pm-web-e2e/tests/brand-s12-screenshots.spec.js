import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { pair, switchScenario } from './helpers/pairing.js';
import { makeRuntimeUnreachable } from './helpers/unreachable.js';

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
 * against the badge at 16 px in monochrome (`favourite-vs-badge-16.png`). The tab icon is drawn and
 * measured at 16 px, the size a browser tab shows it at.
 *
 * The same run writes the browser evidence of the mascot figure on every surface it stands on
 * (`mascot-{surface}-{lang}-{theme}-{width}.png`): the pairing hero, the revoked device, the
 * network listener turned off, the runtime that is not reachable, the reports page without unread
 * reports, and the rail-foot bear expanded and collapsed, above and below its free-height threshold.
 *
 * The set is evidence for a review, not a regression check, so it stays out of the default suite:
 * it runs only where `PM_E2E_BRAND_SHOTS` is set, from Maven with `-Dpm.e2e.brandShots=1`.
 */

test.skip(
  !process.env.PM_E2E_BRAND_SHOTS,
  'The S12 brand screenshots run only on request: -Dpm.e2e.brandShots=1 (PM_E2E_BRAND_SHOTS).',
);

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

/** The size a browser tab draws its icon at, and the size the icon is captured at. */
const TAB_ICON_SIZE = 16;

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
 * The store is read by an awaited evaluation: `waitForFunction` does not await a predicate that
 * returns a promise and would take the pending promise as a truthy answer.
 *
 * @param {import('@playwright/test').Page} page the page to wait for
 */
async function waitForApp(page) {
  await expect
    .poll(
      () =>
        page.evaluate(async () => {
          const { runtimeStore } = await import('/src/lib/stores/runtime.ts');
          let initialized = false;
          const unsubscribe = runtimeStore.subscribe((state) => {
            initialized = state.isInitialized;
          });
          unsubscribe();
          return initialized;
        }),
      { timeout: 20000 },
    )
    .toBe(true);
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
    await iconPage.setViewportSize({ width: TAB_ICON_SIZE, height: TAB_ICON_SIZE });
    await iconPage.emulateMedia({ colorScheme: theme });
    const response = await iconPage.goto(expected);
    expect(response?.ok(), `${expected} was not served`).toBe(true);
    const icon = iconPage.locator('svg').first();
    await expect(icon).toBeVisible();
    // The served icon fills the tab's 16 px square.
    const box = await icon.boundingBox();
    expect(box?.width).toBe(TAB_ICON_SIZE);
    expect(box?.height).toBe(TAB_ICON_SIZE);
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
          // Beside the hero the icon stands at 28 px; on the phone, without it, at 64 px.
          const shown = width === DESKTOP.width ? 'pairing-mark-28' : 'pairing-mark-64';
          await expect(page.getByTestId(shown).getByTestId('brand-mark')).toBeVisible();

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

/** A desktop below the pairing hero's free-height threshold (520 px). */
const SHORT_DESKTOP = { width: 1280, height: 480 };

/** A desktop tall enough to leave the rail-foot bear its 260 px of free height. */
const TALL_DESKTOP = { width: 1280, height: 1400 };

/** The free height below the last destination the rail-foot bear needs (brand-mark-mascot.adoc). */
const BEAR_FREE_HEIGHT = 260;

/**
 * Waits until the app holds the figure of the disconnected states as an object URL, which it
 * fetches once after its start. The store is read by an awaited evaluation, as `waitForApp` reads
 * the runtime.
 *
 * @param {import('@playwright/test').Page} page the paired page
 */
async function waitForHeldFigure(page) {
  await expect
    .poll(
      () =>
        page.evaluate(async () => {
          const { heldFigure } = await import('/src/lib/stores/heldFigure.ts');
          let url = '';
          const unsubscribe = heldFigure.subscribe((value) => {
            url = value;
          });
          unsubscribe();
          return url.startsWith('blob:');
        }),
      { timeout: 20000 },
    )
    .toBe(true);
}

/**
 * Asserts the one mascot figure of a surface: its pose, and that it is drawn.
 *
 * @param {import('@playwright/test').Locator} surface the surface that carries the figure
 * @param {string} pose the pose the surface names
 * @returns {Promise<import('@playwright/test').Locator>} the figure
 */
async function expectFigure(surface, pose) {
  const figure = surface.getByTestId('mascot-figure');
  await expect(figure).toHaveCount(1);
  await expect(figure).toBeVisible();
  await expect(figure).toHaveAttribute('data-pose', pose);
  // The image arrived and was decoded: a figure that failed to load has no natural size.
  await expect
    .poll(() => figure.evaluate((image) => image.complete && image.naturalWidth > 0))
    .toBe(true);
  return figure;
}

/**
 * Writes one evidence screenshot of a mascot surface and asserts it was written.
 *
 * @param {import('@playwright/test').Page} page the page that shows the surface
 * @param {string} surface the surface the screenshot shows
 * @param {{ locale: string, theme: string, width: number }} where the locale, theme and width
 */
async function captureEvidence(page, surface, { locale, theme, width }) {
  const file = shotPath(`mascot-${surface}`, locale, theme, width);
  await page.screenshot({ path: file, animations: 'disabled' });
  expectWritten(file);
}

/**
 * @param {import('@playwright/test').Page} page the page that shows the rail
 * @returns {Promise<{ freeHeight: number, background: string }>} the free height below the last
 *   destination and the background the bear draws there
 */
async function railFoot(page) {
  return page.getByTestId('rail-spacer').evaluate((spacer) => ({
    freeHeight: spacer.getBoundingClientRect().height,
    background: getComputedStyle(spacer.querySelector('.nav-rail__bear')).backgroundImage,
  }));
}

/**
 * Replaces every list in a JSON answer by an empty one, so a page reads an empty collection.
 *
 * @param {unknown} value the parsed answer
 * @returns {unknown} the answer without entries
 */
function withoutEntries(value) {
  if (Array.isArray(value)) {
    return [];
  }
  if (value !== null && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value).map(([key, inner]) => [key, withoutEntries(inner)]),
    );
  }
  return value;
}

test.describe('S12 mascot evidence', () => {
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
        const shot = { locale, theme, width };
        const desktop = width === DESKTOP.width;

        test(`pairing hero (${where})`, async ({ page, request }) => {
          await page.setViewportSize({ width, height });
          await switchScenario(request, 'web-pairing');
          await page.goto('/');
          await page.evaluate(() => localStorage.clear());
          await storeChoices(page, locale, theme);
          await page.goto('/pair');
          await waitForApp(page);
          await expect(page.getByTestId('pairing-view')).toBeVisible();

          if (desktop) {
            await expectFigure(page.getByTestId('pairing-hero'), 'welcome');
          } else {
            await expect(page.getByTestId('pairing-hero')).toBeHidden();
          }
          await captureEvidence(page, 'pairing-hero', shot);

          if (desktop) {
            // Below the free-height threshold the hero gives way and the icon grows to 64 px.
            await page.setViewportSize(SHORT_DESKTOP);
            await expect(page.getByTestId('pairing-hero')).toBeHidden();
            await expect(
              page.getByTestId('pairing-mark-64').getByTestId('brand-mark'),
            ).toBeVisible();
            await captureEvidence(page, 'pairing-hero-short', shot);
          }
        });

        test(`revoked device (${where})`, async ({ page, request }) => {
          await openShell(page, request, { scenario: 'paired-devices', locale, theme });
          await page.goto('/settings');
          await expect(page.locator('.settings-view')).toBeVisible();
          const row = page.getByTestId('device-row-dev-1');
          await row.getByTestId('action-bar-overflow').click();
          await row.getByTestId('action-revoke').click();
          await page.getByTestId('confirm-dialog-confirm').click();

          await page.setViewportSize({ width, height });
          await expectFigure(page.getByTestId('revoked-view'), 'access-ended');
          await captureEvidence(page, 'revoked', shot);
        });

        test(`network listener turned off (${where})`, async ({ page, request }) => {
          await openShell(page, request, { scenario: 'paired-devices', locale, theme });
          await waitForHeldFigure(page);
          await page.evaluate(async () => {
            const { runtimeStore } = await import('/src/lib/stores/runtime.ts');
            runtimeStore.update((state) => ({
              ...state,
              isListenerClosed: true,
              currentRoute: 'listener-off',
            }));
          });

          await page.setViewportSize({ width, height });
          const figure = await expectFigure(page.getByTestId('listener-off-view'), 'searching');
          await expect(figure).toHaveAttribute('src', /^blob:/);
          await captureEvidence(page, 'listener-off', shot);
        });

        test(`runtime not reachable (${where})`, async ({ page, request }) => {
          await openShell(page, request, { scenario: 'fully-onboarded', locale, theme });
          await waitForHeldFigure(page);
          await makeRuntimeUnreachable(page);

          await page.setViewportSize({ width, height });
          const figure = await expectFigure(page.getByTestId('unreachable-view'), 'searching');
          await expect(figure).toHaveAttribute('src', /^blob:/);
          await captureEvidence(page, 'unreachable', shot);
        });

        test(`reports without unread reports (${where})`, async ({ page, request }) => {
          await openShell(page, request, { scenario: 'reports', locale, theme });
          // The list is read empty, so the page shows its calm state.
          await page.route(
            (url) => url.pathname.endsWith('/reports'),
            async (route) => {
              if (route.request().method() !== 'GET') {
                await route.continue();
                return;
              }
              const response = await route.fetch();
              await route.fulfill({ response, json: withoutEntries(await response.json()) });
            },
          );
          await page.goto('/reports');
          await waitForApp(page);

          await page.setViewportSize({ width, height });
          const reports = page.locator('.reports-view');
          await expect(reports).toBeVisible();
          await expectFigure(reports, 'all-clear');
          await captureEvidence(page, 'reports-no-reports', shot);
        });

        test(`rail-foot bear (${where})`, async ({ page, request }) => {
          await openShell(page, request, { scenario: 'fully-onboarded', locale, theme });

          if (!desktop) {
            // The phone has no rail, so no bear: the shot shows the page without it.
            await page.setViewportSize({ width, height });
            await expect(page.getByTestId('nav-rail')).toBeHidden();
            await captureEvidence(page, 'rail-foot', shot);
            return;
          }

          const rail = page.getByTestId('nav-rail');
          await page.setViewportSize(TALL_DESKTOP);
          await expect(rail).toHaveClass(/nav-rail--bear/);
          const above = await railFoot(page);
          expect(above.freeHeight).toBeGreaterThanOrEqual(BEAR_FREE_HEIGHT);
          expect(above.background).toContain('url(');
          await captureEvidence(page, 'rail-foot-expanded-above', shot);

          await page.setViewportSize(SHORT_DESKTOP);
          const below = await railFoot(page);
          expect(below.freeHeight).toBeLessThan(BEAR_FREE_HEIGHT);
          expect(below.background).toBe('none');
          await captureEvidence(page, 'rail-foot-expanded-below', shot);

          await page.setViewportSize(TALL_DESKTOP);
          await page.getByTestId('rail-toggle').click();
          await expect(rail).toHaveClass(/nav-rail--collapsed/);
          await expect(rail).not.toHaveClass(/nav-rail--bear/);
          expect((await railFoot(page)).background).toBe('none');
          await captureEvidence(page, 'rail-foot-collapsed', shot);
        });
      }
    }
  }
});
