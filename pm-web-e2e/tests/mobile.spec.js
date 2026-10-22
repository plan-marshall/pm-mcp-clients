import { test, expect } from '@playwright/test';

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
  const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
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
        .locator('.app-header')
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
  }

  test.afterAll(async ({ request }) => {
    try {
      await request.post('http://127.0.0.1:7420/mock/scenario', {
        data: { scenario: 'web-pairing' },
      });
    } catch {
      // Best-effort cleanup for the suites that run after this one.
    }
  });
});
