import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

/**
 * The critical journeys at phone width (WEB-1): the onboarding wizard and the running-work
 * overview must render without a horizontal scrollbar and keep their controls reachable.
 *
 * The two viewports are the narrowest the design promises (320x568) and a common phone
 * (375x667); the layout collapses the sidebar and the card headers below 768px.
 */

const PHONES = [
  { label: '320x568', width: 320, height: 568 },
  { label: '375x667', width: 375, height: 667 },
];

async function useScenario(request, scenario) {
  const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
    data: { scenario },
  });
  expect(res.ok()).toBeTruthy();
}

async function pair(page, path = '/pair#code=KXQB-MRTN') {
  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto(path);
  await expect(page.locator('.app-header__machine')).toContainText('Machine:');
}

async function expectNoHorizontalOverflow(page) {
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
  );
  expect(overflow).toBeLessThanOrEqual(1);
}

async function expectWithinViewport(page, locator, width) {
  await expect(locator).toBeVisible();
  const box = await locator.boundingBox();
  expect(box).not.toBeNull();
  expect(box.x).toBeGreaterThanOrEqual(0);
  expect(box.x + box.width).toBeLessThanOrEqual(width + 1);
}

test.describe('Mobile viewports (WEB-1)', () => {
  for (const phone of PHONES) {
    test(`onboarding renders without horizontal overflow at ${phone.label}`, async ({
      page,
      request,
    }) => {
      await page.setViewportSize({ width: phone.width, height: phone.height });
      await useScenario(request, 'onboarding');
      await pair(page);
      await expect(page).toHaveURL(/\/onboarding$/);

      await expect(page.getByTestId('onboarding-view')).toBeVisible();
      await expectNoHorizontalOverflow(page);

      const begin = page.getByRole('button', { name: /Begin with step 1/i });
      await expectWithinViewport(page, begin, phone.width);

      await begin.click();
      await expect(page.getByTestId('language-screen')).toBeVisible();
      await expectNoHorizontalOverflow(page);
    });

    test(`running work keeps its attention band and cards reachable at ${phone.label}`, async ({
      page,
      request,
    }) => {
      await page.setViewportSize({ width: phone.width, height: phone.height });
      await useScenario(request, 'running-work');
      await pair(page);

      const overviewNav = page
        .locator('.bottom-bar')
        .getByRole('button', { name: /Running work/i });
      await expectWithinViewport(page, overviewNav, phone.width);
      await overviewNav.click();

      await expect(page.locator('.overview')).toBeVisible();
      await expect(page.locator('.plan-card').first()).toBeVisible();
      await expect(page.locator('.badge--attention').first()).toBeVisible();
      await expectNoHorizontalOverflow(page);

      const cardAction = page.locator('.plan-card .card-action').first();
      await expectWithinViewport(page, cardAction, phone.width);
      await expectNoHorizontalOverflow(page);
    });

    test(`reports filter and split layout stay usable at ${phone.label}`, async ({
      page,
      request,
    }) => {
      await page.setViewportSize({ width: phone.width, height: phone.height });
      await useScenario(request, 'reports');
      await pair(page);

      await page
        .locator('.bottom-bar')
        .getByRole('button', { name: /Reports/i })
        .click();
      await expect(page.locator('.reports-view')).toBeVisible();

      const filter = page.getByTestId('filter-input');
      await expectWithinViewport(page, filter, phone.width);
      await filter.fill('overlap with relay-auth-v2');
      await expect(page.locator('.report-card')).toHaveCount(1);
      await expectNoHorizontalOverflow(page);

      await page.getByTestId('layout-split').click();
      await expect(page.locator('.reports-view')).toHaveAttribute('data-layout', 'split');
      // Below the split breakpoint the view is one column: the empty detail pane is hidden.
      await expect(page.locator('.reports-split__detail')).toBeHidden();
      await expect(page.locator('.reports-tree')).toBeVisible();
      await expectNoHorizontalOverflow(page);
    });
  }

  test('carries four labelled destinations in the bottom bar', async ({ page, request }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await useScenario(request, 'fully-onboarded');
    await pair(page);

    const bar = page.getByTestId('bottom-bar');
    await expect(bar).toBeVisible();
    await expect(bar.getByRole('button')).toHaveCount(4);
    for (const name of [/Running work/i, /Decisions/i, /Reports/i, /More/i]) {
      const item = bar.getByRole('button', { name });
      await expect(item).toBeVisible();
      await expect(item).toHaveText(/\S/);
    }

    await expect(page.getByTestId('nav-rail')).toBeHidden();

    await bar.getByRole('button', { name: /More/i }).click();
    const sheet = page.getByTestId('dialog');
    await expect(sheet).toBeVisible();
    await expect(page.getByTestId('sheet-group-work')).toBeVisible();
    await expect(page.getByTestId('sheet-group-records')).toBeVisible();
    await expect(page.getByTestId('sheet-group-machine')).toBeVisible();

    // The language switch and the theme choice of the rail's foot stay reachable on a phone.
    await expect(page.getByTestId('sheet-language-toggle')).toBeVisible();
    await expect(page.getByTestId('sheet-theme-light')).toBeVisible();
    await expect(page.getByTestId('sheet-theme-dark')).toBeVisible();
    await expect(page.getByTestId('sheet-theme-system')).toBeVisible();

    await page.getByTestId('sheet-system').click();
    await expect(page).toHaveURL(/\/system$/);
    await expect(page.getByTestId('dialog')).toHaveCount(0);
  });

  test('keeps every route without horizontal scroll at 390 px', async ({ page, request }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await useScenario(request, 'fully-onboarded');
    await pair(page);

    const routes = [
      '/overview',
      '/plans/fix-relay',
      '/epics/relay-hardening',
      '/projects',
      '/decisions',
      '/reports',
      '/designer',
      '/system',
      '/system/cleanup',
      '/settings',
    ];

    for (const path of routes) {
      await page.goto(path);
      await expect.soft(page.getByTestId('bottom-bar')).toBeVisible();
      await expectNoHorizontalOverflow(page);
    }
  });

  test.afterAll(async ({ request }) => {
    try {
      await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
        data: { scenario: 'web-pairing' },
      });
    } catch {
      // Best-effort cleanup for the suites that run after this one.
    }
  });
});
