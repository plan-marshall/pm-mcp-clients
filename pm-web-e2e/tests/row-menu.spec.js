import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

/**
 * The row menu at phone width (`doc/design/web-app/components.adoc#row-menu`): a menu opened by a
 * row at the bottom of the viewport opens where the sticky bottom bar does not cover it.
 *
 * The check is measured, not read from the style sheet: it places the last report's trigger
 * directly above the bottom bar, opens its menu, and asks the page which element is drawn on top
 * at the corners and the centre of every entry. A menu laid out under the bar fails, and so does
 * one that leaves the viewport.
 */

const PHONE = { width: 390, height: 844 };

test.describe('RowMenu at phone width', () => {
  test.beforeEach(async ({ page, request }) => {
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'reports' },
    });
    expect(res.ok()).toBeTruthy();

    await page.setViewportSize(PHONE);
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test('opens the menu of a row at the bottom of the viewport clear of the bottom bar at 390 px', async ({
    page,
  }) => {
    await page.goto('/reports');
    await expect(page.locator('.reports-view')).toBeVisible();
    await expect(page.getByTestId('unread-count')).toBeVisible();
    // The list, not only its count, must be drawn before the row's place is measured.
    await expect(page.locator('.report-card').last()).toBeVisible();

    const bar = page.getByTestId('bottom-bar');
    await expect(bar).toBeVisible();
    const trigger = page
      .locator('.report-card')
      .last()
      .getByRole('button', { name: /Report actions/i });
    await expect(trigger).toBeVisible();

    // Make the viewport exactly as tall as it takes to put the trigger right above the sticky bar
    // at the top of the page: the row is then the bottom row of the visible page, whatever the
    // scenario's list length and whatever the page draws below the list.
    await trigger.scrollIntoViewIfNeeded();
    const scrollY = await page.evaluate(() => window.scrollY);
    const triggerBox = await trigger.boundingBox();
    const barBox = await bar.boundingBox();
    expect(triggerBox).not.toBeNull();
    expect(barBox).not.toBeNull();
    const height = Math.ceil(scrollY + triggerBox.y + triggerBox.height + barBox.height + 4);
    await page.setViewportSize({ width: PHONE.width, height });
    await page.evaluate(() => window.scrollTo(0, 0));

    const placedTrigger = await trigger.boundingBox();
    const placedBar = await bar.boundingBox();
    expect(placedTrigger).not.toBeNull();
    expect(placedBar).not.toBeNull();
    // The precondition: the trigger is the last thing above the bar, so a menu that opens
    // downwards can only open into the bar.
    expect(placedTrigger.y + placedTrigger.height).toBeLessThanOrEqual(placedBar.y);
    expect(placedBar.y - (placedTrigger.y + placedTrigger.height)).toBeLessThan(48);

    await trigger.click();
    const menu = page.getByRole('menu');
    await expect(menu).toBeVisible();

    // The whole menu lies inside the viewport.
    const menuBox = await menu.boundingBox();
    expect(menuBox).not.toBeNull();
    expect(menuBox.y).toBeGreaterThanOrEqual(0);
    expect(menuBox.y + menuBox.height).toBeLessThanOrEqual(height);
    expect(menuBox.x).toBeGreaterThanOrEqual(0);
    expect(menuBox.x + menuBox.width).toBeLessThanOrEqual(PHONE.width);

    // The bar covers no entry: at every corner and the centre of each entry, the topmost element
    // is that entry. Where an entry lies over the bar's box, the entry is the one drawn on top.
    const entries = menu.getByRole('menuitem');
    const count = await entries.count();
    expect(count).toBeGreaterThan(0);
    for (let i = 0; i < count; i += 1) {
      const covered = await entries.nth(i).evaluate((entry) => {
        const box = entry.getBoundingClientRect();
        const points = [
          [box.left + 2, box.top + 2],
          [box.right - 2, box.top + 2],
          [box.left + 2, box.bottom - 2],
          [box.right - 2, box.bottom - 2],
          [box.left + box.width / 2, box.top + box.height / 2],
        ];
        return points.filter(([x, y]) => {
          const hit = document.elementFromPoint(x, y);
          return hit === null || !entry.contains(hit);
        }).length;
      });
      expect(covered).toBe(0);
    }
  });
});
