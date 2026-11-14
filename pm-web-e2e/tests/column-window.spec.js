import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

/**
 * The column window (`doc/design/web-app/navigation.adoc#window`) in the running app: the window a
 * five-level chain gets in the container the shell leaves, at `1024`, `1280`, `1440` and `1920 px`
 * with the rail expanded and collapsed, and on the phone at `390 px`.
 *
 * Every expectation is computed from the container width the DOM reports — the template measures
 * its own container, never the viewport — so the suite proves the table rather than a screenshot.
 * The waits stand on the layout's own condition: the rail's class, the rendered columns' count.
 */

/** The window the design's rule gives a container (`navigation.adoc#window`), as the DOM must agree
 * with it. The development page's fixture chains five levels, so every window of two or more
 * columns leaves levels to the gap strip.
 *
 * @param {number} container the template's container width in px
 * @returns {{content: number, columns: number, list: number, object: number, strip: number}}
 */
function expectedWindow(container) {
  const inset = container < 480 ? 12 : 24;
  const content = container - 2 * inset;
  const wantsNames = (n) => (n - 1) * 220 + 480 + 136 <= content;
  const wantsMarks = 220 + 480 + 44 <= content;

  for (let n = 4; n >= 2; n--) {
    if (wantsNames(n)) {
      return widths(content, n, 136);
    }
  }
  if (wantsMarks) {
    return widths(content, 2, 44);
  }
  return { content, columns: 1, list: 0, object: content, strip: 0 };
}

/**
 * @param {number} content the content width in px
 * @param {number} columns the number of columns
 * @param {number} strip the strip's width in px
 * @returns the widths the window takes
 */
function widths(content, columns, strip) {
  const list = Math.min(320, Math.floor((content - strip - 480) / (columns - 1)));
  return { content, columns, list, object: content - strip - list * (columns - 1), strip };
}

/**
 * Pairs the browser and opens the development page's column engine.
 *
 * @param {import('@playwright/test').Page} page the browser page
 * @param {import('@playwright/test').APIRequestContext} request the suite's request context
 * @returns {Promise<import('@playwright/test').Locator>} the engine's container
 */
async function openEngine(page, request) {
  const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
    data: { scenario: 'fully-onboarded' },
  });
  expect(res.ok()).toBeTruthy();

  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/#code=KXQB-MRTN');
  await expect(page.locator('.app-header__machine')).toContainText('Machine:');

  await page.goto('/dev/components');
  const engine = page.locator('[data-testid="triage-page"]').first();
  await expect(engine).toBeVisible();
  return engine;
}

/**
 * Puts the rail in the state a case names, waiting on the rail's own class.
 *
 * @param {import('@playwright/test').Page} page the browser page
 * @param {'expanded' | 'collapsed'} state the state the case needs
 */
async function setRail(page, state) {
  const rail = page.getByTestId('nav-rail');
  const collapsed = (await rail.getAttribute('class'))?.includes('nav-rail--collapsed') ?? false;
  if (collapsed === (state === 'collapsed')) {
    return;
  }
  await page.getByTestId('rail-toggle').click();
  if (state === 'collapsed') {
    await expect(rail).toHaveClass(/nav-rail--collapsed/);
  } else {
    await expect(rail).not.toHaveClass(/nav-rail--collapsed/);
  }
}

/**
 * Reads what the engine rendered, with the container it measured.
 *
 * @param {import('@playwright/test').Page} page the browser page
 * @returns {Promise<{container: number, columns: number[], strip: number}>}
 */
async function readWindow(page) {
  return page.evaluate(() => {
    const frame = document.querySelector('[data-testid="triage-page"]');
    const columns = [...document.querySelectorAll('[data-column-index]')].map((element) =>
      Number.parseFloat(element.style.inlineSize),
    );
    const strip = document.querySelector('[data-testid="triage-page"] .gap-strip');
    return {
      container: frame === null ? 0 : frame.clientWidth,
      columns,
      strip: strip === null ? 0 : Number.parseFloat(strip.style.inlineSize),
    };
  });
}

test.describe('The column window (navigation.adoc#window)', () => {
  for (const viewport of [1024, 1280, 1440, 1920]) {
    for (const rail of ['expanded', 'collapsed']) {
      test(`shows the window of a ${viewport} px viewport, rail ${rail}`, async ({
        page,
        request,
      }) => {
        await page.setViewportSize({ width: viewport, height: 900 });
        await openEngine(page, request);
        await setRail(page, rail);

        // The window is computed from the container the rail leaves, not from the viewport.
        const seen = await readWindow(page);
        const expected = expectedWindow(seen.container);

        expect(seen.columns).toHaveLength(expected.columns);
        if (expected.columns > 1) {
          expect(seen.columns[0]).toBe(expected.list);
          expect(seen.columns[seen.columns.length - 1]).toBe(expected.object);
          expect(seen.strip).toBe(expected.strip);
        }
        expect(seen.columns.reduce((sum, width) => sum + width, 0) + seen.strip).toBe(
          expected.content,
        );
      });
    }
  }

  test('shows one column and no strip on a 390 px phone', async ({ page, request }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    const engine = await openEngine(page, request);

    await expect(page.getByTestId('nav-rail')).toBeHidden();

    const seen = await readWindow(page);
    const expected = expectedWindow(seen.container);

    expect(expected.columns).toBe(1);
    expect(seen.columns).toHaveLength(1);
    expect(seen.strip).toBe(0);
    expect(seen.columns[0]).toBe(expected.object);

    // The page itself never scrolls sideways.
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(1);
    await expect(engine).toBeVisible();
  });
});
