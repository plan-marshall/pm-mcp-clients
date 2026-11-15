import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

/**
 * The column window (`doc/design/web-app/navigation.adoc#window`) in the running app: the window
 * the reports engine gets in the container the shell leaves — the report's detail beside the list —
 * at `1024`, `1280`, `1440` and `1920 px` with the rail expanded and collapsed, and on the phone at
 * `390 px`.
 *
 * Every expectation is computed from the container width the DOM reports — the template measures
 * its own container, never the viewport — so the suite proves the rule rather than a screenshot.
 * The waits stand on the layout's own condition: the rail's class, the rendered columns' count.
 */

/**
 * The window the design's rule gives a two-level chain (the reports list and the report beside it).
 *
 * @param {number} container the template's container width in px
 * @returns {{content: number, columns: number, list: number, object: number}}
 */
function expectedWindow(container) {
  const inset = container < 480 ? 12 : 24;
  const content = container - 2 * inset;
  if (content < 700) {
    return { content, columns: 1, list: 0, object: content };
  }
  const list = Math.min(320, content - 480);
  return { content, columns: 2, list, object: content - list };
}

/**
 * Pairs the browser and opens a report's detail beside the reports list.
 *
 * @param {import('@playwright/test').Page} page the browser page
 * @param {import('@playwright/test').APIRequestContext} request the suite's request context
 * @param {boolean} narrow whether the phone's bottom bar carries the destinations
 */
async function openReportsWithDetail(page, request, narrow = false) {
  const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
    data: { scenario: 'reports' },
  });
  expect(res.ok()).toBeTruthy();

  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/#code=KXQB-MRTN');
  await expect(page.locator('.app-header__machine')).toContainText('Machine:');

  const navigation = narrow ? page.getByTestId('bottom-bar') : page.locator('.nav-rail');
  await navigation.getByRole('button', { name: /Reports/i }).click();
  await expect(page.locator('.reports-view')).toBeVisible();

  await page
    .locator('.report-card', { hasText: 'Ended: merged (#214)' })
    .getByRole('button', { name: /Show the details/i })
    .click();
  // The object column is the report's detail: the window has its second level once it is open.
  await expect(page.locator('.completion-report')).toBeVisible();
}

/**
 * Puts the rail in the state a case names, waiting on the rail's own class.
 *
 * @param {import('@playwright/test').Page} page the browser page
 * @param {'expanded' | 'collapsed'} state the state the case needs
 */
async function setRail(page, state) {
  const rail = page.locator('.nav-rail');
  await expect(rail).toBeVisible();
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
 * @returns {Promise<{container: number, columns: number[]}>}
 */
async function readWindow(page) {
  return page.evaluate(() => {
    const frame = document.querySelector('[data-testid="triage-page"]');
    const columns = [...document.querySelectorAll('[data-column-index]')].map((element) =>
      Number.parseFloat(element.style.inlineSize),
    );
    return { container: frame === null ? 0 : frame.clientWidth, columns };
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
        await openReportsWithDetail(page, request);
        await setRail(page, rail);

        // The window is computed from the container the shell leaves, not from the viewport; the
        // observer reports the new container a frame after the rail moved, so the wait stands on
        // the window's own answer for that container.
        await expect
          .poll(async () => {
            const seen = await readWindow(page);
            const expected = expectedWindow(seen.container);
            return (
              seen.columns.length === expected.columns &&
              seen.columns[seen.columns.length - 1] === expected.object
            );
          })
          .toBe(true);

        const seen = await readWindow(page);
        const expected = expectedWindow(seen.container);

        expect(seen.columns).toHaveLength(expected.columns);
        expect(seen.columns[seen.columns.length - 1]).toBe(expected.object);
        if (expected.columns === 2) {
          expect(seen.columns[0]).toBe(expected.list);
          expect(expected.object).toBeGreaterThanOrEqual(480);
        }
        expect(seen.columns.reduce((sum, width) => sum + width, 0)).toBe(expected.content);
      });
    }
  }

  test('shows one column on a 390 px phone', async ({ page, request }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await openReportsWithDetail(page, request, true);

    await expect(page.getByTestId('nav-rail')).toBeHidden();

    const seen = await readWindow(page);
    const expected = expectedWindow(seen.container);

    expect(expected.columns).toBe(1);
    expect(seen.columns).toHaveLength(1);
    expect(seen.columns[0]).toBe(expected.object);

    // The page itself never scrolls sideways.
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(1);
  });
});
