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
 * Reads what the engine rendered, with the container it measured. The columns' widths are the
 * rendered ones (`getBoundingClientRect`), not the widths they asked for.
 *
 * @param {import('@playwright/test').Page} page the browser page
 * @returns {Promise<{container: number, columns: number[]}>}
 */
async function readWindow(page) {
  return page.evaluate(() => {
    const frame = document.querySelector('[data-testid="triage-page"]');
    const columns = [...document.querySelectorAll('[data-column-index]')].map((element) =>
      Math.round(element.getBoundingClientRect().width),
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

  test('opens the names popover under its own control, not at the bottom of the column', async ({
    page,
    request,
  }) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await openReportsWithDetail(page, request);
    await page.goto('/dev/components');

    const engine = page.locator('[data-testid="column-engine-fixture"]').first();
    await expect(engine).toBeVisible();

    const names = engine.getByRole('button', { name: 'Names' });
    await names.click();
    const popover = engine.getByRole('group', { name: 'Names' });
    await expect(popover).toBeVisible();

    const buttonBox = await names.boundingBox();
    const popoverBox = await popover.boundingBox();
    expect(buttonBox).not.toBeNull();
    expect(popoverBox).not.toBeNull();
    // The popover opens at the control's own bottom, not at the bottom of the tallest column.
    expect(popoverBox.y).toBeGreaterThanOrEqual(buttonBox.y + buttonBox.height - 1);
    expect(popoverBox.y - (buttonBox.y + buttonBox.height)).toBeLessThan(40);
  });

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

/**
 * The window the design's rule gives the showroom's five-level chain, for a container width.
 *
 * @param {number} container the template's container width in px
 * @returns {{content: number, columns: number, strip: number, list: number, object: number}}
 */
function expectedShowroomWindow(container) {
  const inset = container < 480 ? 12 : 24;
  const content = container - 2 * inset;
  const depth = 5;
  for (let columns = Math.min(4, depth); columns >= 2; columns--) {
    const strip = depth > columns ? 136 : 0;
    if ((columns - 1) * 220 + 480 + strip <= content) {
      const list = Math.min(320, Math.floor((content - strip - 480) / (columns - 1)));
      return { content, columns, strip, list, object: content - strip - list * (columns - 1) };
    }
  }
  if (220 + 480 + 44 <= content) {
    return { content, columns: 2, strip: 44, list: 220, object: 480 };
  }
  return { content, columns: 1, strip: 0, list: 0, object: content };
}

/**
 * Reads the showroom's first column-engine fixture: the container it measured, the window it
 * rendered there, the widths of the columns (`getBoundingClientRect`) and its two strips.
 *
 * @param {import('@playwright/test').Page} page the browser page
 * @returns {Promise<{container: number, start: number, columns: number[], strip: number,
 *   returnStrip: boolean}>}
 */
async function readShowroomWindow(page) {
  return page.evaluate(() => {
    const fixture = document.querySelector('[data-testid="column-engine-fixture"]');
    const frame = fixture.querySelector('[data-testid="triage-page"]');
    const windowEl = fixture.querySelector('.triage-page__window');
    const strip = fixture.querySelector('.gap-strip--start');
    const returnStrip = fixture.querySelector('.gap-strip--end');
    return {
      container: frame.clientWidth,
      start: Number(windowEl.getAttribute('data-start')),
      columns: [...fixture.querySelectorAll('[data-column-index]')].map((element) =>
        Math.round(element.getBoundingClientRect().width),
      ),
      strip: strip === null ? 0 : Math.round(strip.getBoundingClientRect().width),
      returnStripWidth:
        returnStrip === null ? 0 : Math.round(returnStrip.getBoundingClientRect().width),
      returnStrip: returnStrip !== null,
    };
  });
}

/**
 * Pairs the browser and opens the showroom's column-engine fixture.
 *
 * @param {import('@playwright/test').Page} page the browser page
 * @param {import('@playwright/test').APIRequestContext} request the suite's request context
 * @returns {Promise<import('@playwright/test').Locator>} the first (light) fixture
 */
async function openShowroomChain(page, request) {
  const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
    data: { scenario: 'reports' },
  });
  expect(res.ok()).toBeTruthy();

  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/#code=KXQB-MRTN');
  await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  await page.goto('/dev/components');

  const engine = page.locator('[data-testid="column-engine-fixture"]').first();
  await expect(engine).toBeVisible();
  return engine;
}

test.describe('The deep chain on the showroom fixture (navigation.adoc#window)', () => {
  for (const viewport of [1280, 1440]) {
    test(`shows the ${viewport} px window, peeks by the strip and by [, and returns`, async ({
      page,
      request,
    }) => {
      await page.setViewportSize({ width: viewport, height: 900 });
      const engine = await openShowroomChain(page, request);
      // The fixture opens the chat; the chain's window gets the whole container once it is hidden.
      await engine.getByRole('button', { name: 'Hide the chat' }).click();
      await expect(engine.getByTestId('side-column')).toBeHidden();

      // The window is computed from the container the shell leaves, never from the viewport.
      await expect
        .poll(async () => (await readShowroomWindow(page)).columns.length)
        .toBeGreaterThan(1);
      const normal = await readShowroomWindow(page);
      const expected = expectedShowroomWindow(normal.container);
      expect(normal.columns).toHaveLength(expected.columns);
      expect(normal.columns.slice(0, -1)).toEqual(Array(expected.columns - 1).fill(expected.list));
      expect(normal.columns.at(-1)).toBe(expected.object);
      expect(normal.strip).toBe(expected.strip);
      expect(
        normal.columns.reduce((sum, width) => sum + width, 0) +
          normal.strip +
          normal.returnStripWidth,
      ).toBe(expected.content);

      // A peek by the strip: the window starts at the lens, a return strip stands at the right.
      await engine.locator('.gap-strip__mark').first().click();
      await expect.poll(async () => (await readShowroomWindow(page)).start).toBe(0);
      const peeked = await readShowroomWindow(page);
      expect(peeked.columns).toHaveLength(expected.columns);
      expect(peeked.returnStrip).toBe(true);
      expect(
        peeked.columns.reduce((sum, width) => sum + width, 0) +
          peeked.strip +
          peeked.returnStripWidth,
      ).toBe(expected.content);

      // The return, and the object's own window again.
      await engine.locator('.gap-strip__return').click();
      await expect.poll(async () => (await readShowroomWindow(page)).start).toBe(normal.start);
      expect((await readShowroomWindow(page)).returnStrip).toBe(false);

      // Peek at the lens again, then walk right to the object with `]`: a peek that reaches the
      // object is the object's own window — the strip of the levels hidden to its left and the
      // object's own width, never a stretched object without a strip.
      await engine.locator('.gap-strip__mark').first().click();
      await expect.poll(async () => (await readShowroomWindow(page)).start).toBe(0);
      for (let step = 0; step < 4; step++) {
        await page.keyboard.press('BracketRight');
      }
      await expect.poll(async () => (await readShowroomWindow(page)).start).toBe(normal.start);
      const walked = await readShowroomWindow(page);
      expect(walked.strip).toBe(expected.strip);
      expect(walked.columns.at(-1)).toBe(expected.object);
      expect(walked.returnStrip).toBe(false);

      // A peek by `[`: the focus walks left from the object it now stands on, and the press that
      // leaves the window shifts it. The wait is on the key's own effect, never on a timer.
      for (let step = 0; step <= 4 - normal.start; step++) {
        await page.keyboard.press('BracketLeft');
      }
      await expect.poll(async () => (await readShowroomWindow(page)).start).toBe(normal.start - 1);
      expect((await readShowroomWindow(page)).returnStrip).toBe(true);

      // And back to the object.
      await engine.locator('.gap-strip__return').click();
      await expect.poll(async () => (await readShowroomWindow(page)).start).toBe(normal.start);
    });
  }
});
