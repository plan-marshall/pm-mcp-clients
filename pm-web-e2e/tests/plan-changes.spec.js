import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';
import { openPlan } from './helpers/plans.js';

test.describe('A plan’s changes (PLAN-164)', () => {
  test.beforeEach(async ({ page, request }) => {
    // The plan-view scenario is the running-work start state: its capture is fix-relay's.
    const switchRes = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'plan-view' },
    });
    expect(switchRes.ok()).toBeTruthy();

    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    const overviewNav = page.locator('.nav-rail').getByRole('button', { name: /Running work/i });
    await expect(overviewNav).toBeVisible();
    await overviewNav.click();
    await expect(page.locator('.overview')).toBeVisible();
  });

  test.afterAll(async ({ request }) => {
    try {
      await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
        data: { scenario: 'web-pairing' },
      });
    } catch {
      // Best-effort cleanup, as the other plan-view suites do.
    }
  });

  test('opens the tab from the size line, a file from its row, and keeps both through a reload', async ({
    page,
  }, testInfo) => {
    await openPlan(page, 'Fix Relay Transport');

    // The size line opens the tab and says the figures of the same capture.
    const size = page.locator('[data-testid="plan-change-size"]');
    await expect(size).toContainText('12 files');
    await expect(size).toContainText('+340');
    await size.click();

    await expect(page).toHaveURL(/tab=changes/);
    const changes = page.locator('[data-testid="plan-changes"]');
    await expect(changes).toBeVisible();
    await expect(changes.locator('[data-testid="changes-title"]')).toContainText(
      'changes · at 9f3c2ab',
    );
    await expect(page.locator('[data-testid="change-row"]')).toHaveCount(12);

    // The special rows the fixture carries: a renamed file, a binary one and an unplanned one.
    await expect(changes).toContainText(
      '← pm-api/src/test/java/de/planmarshall/api/overview/PlanChangesContractTest.java',
    );
    await expect(changes).toContainText('binary');
    await expect(changes).toContainText('unplanned');

    // A row opens its hunks, with the old and the new line numbers.
    const row = page
      .locator('[data-testid="change-row"]')
      .filter({ hasText: 'changes/PlanChanges.java' });
    await row.click();
    await expect(page).toHaveURL(
      /\/plans\/fix-relay\?tab=changes&file=pm-api\/src\/main\/java\/de\/planmarshall\/api\/changes\/PlanChanges\.java/,
    );
    const hunks = page.locator('[data-testid="change-hunks"]');
    await expect(hunks).toBeVisible();
    await expect(hunks.locator('[data-testid="diff-line"]').first()).toBeVisible();
    await expect(hunks.locator('[data-testid="line-old"]').first()).toHaveText('1');

    await testInfo.attach('plan-changes-tab', {
      body: await page.screenshot({ fullPage: false }),
      contentType: 'image/png',
    });

    // The address survives a reload: the same tab, the same file.
    await page.reload();
    await expect(page.locator('[data-testid="plan-changes"]')).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Changes' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    await expect(page.locator('[data-testid="change-hunks"]')).toContainText(
      'PlanChanges',
    );
    await expect(
      page.locator('[data-testid="change-row"]').filter({ hasText: 'changes/PlanChanges.java' }),
    ).toHaveAttribute('aria-current', 'true');
  });
});
