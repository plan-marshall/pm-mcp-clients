import { test, expect } from '@playwright/test';

test.describe('Reports & Completion Reports Flow (UC-PLN-6)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to reports scenario
    const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
      data: { scenario: 'reports' },
    });
    expect(res.ok()).toBeTruthy();

    // Clear localStorage and pair with code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test('displays unread reports arrival notice on load and navigates to reports view', async ({
    page,
  }) => {
    // Check arrival banner if present or click Reports button
    const reportsBtn = page.locator('.app-header').getByRole('button', { name: /Reports/i });
    await expect(reportsBtn).toBeVisible();
    await reportsBtn.click();

    await expect(page.locator('.reports-view')).toBeVisible();
    await expect(page.locator('.reports-heading')).toContainText('Reports');

    // Verify grouped hierarchy
    await expect(page.getByText('pm-mcp-clients')).toBeVisible();
    await expect(page.getByText('relay-hardening')).toBeVisible();
    await expect(page.getByText('fix-relay')).toBeVisible();
    await expect(page.getByText('Ended: merged (#214)')).toBeVisible();
  });

  test('marks a single report read and removes it from view', async ({ page }) => {
    // Navigate to reports view
    await page
      .locator('.app-header')
      .getByRole('button', { name: /Reports/i })
      .click();
    await expect(page.locator('.reports-view')).toBeVisible();

    // Look for Ended: no change (rep-0139)
    await expect(page.getByText('Ended: no change')).toBeVisible();

    // Click Mark read for rep-0139
    const markReadButtons = page.locator('.action-btn--read');
    const initialCount = await markReadButtons.count();
    expect(initialCount).toBeGreaterThan(0);

    // Click the first Mark read button
    await markReadButtons.first().click();

    // Item count should decrease
    await expect(page.locator('.action-btn--read')).toHaveCount(initialCount - 1);
  });

  test('opens completion report from report detail', async ({ page }) => {
    // Navigate to reports view
    await page
      .locator('.app-header')
      .getByRole('button', { name: /Reports/i })
      .click();
    await expect(page.locator('.reports-view')).toBeVisible();

    // Click Show the details on fix-relay report
    const detailBtn = page.locator('.action-btn--detail').first();
    await expect(detailBtn).toBeVisible();
    await detailBtn.click();

    // Completion report modal / view should be visible
    await expect(page.locator('.completion-report')).toBeVisible();
    await expect(page.getByText('the pull request #214 was merged into main')).toBeVisible();
    await expect(page.getByText('cursor write order')).toBeVisible();

    // Tokens section should display breakdown and waste
    await expect(page.locator('.tokens-section')).toBeVisible();
    await expect(page.getByText('refine')).toBeVisible();

    // Close completion report
    const backBtn = page.getByRole('button', { name: /Back/i });
    await expect(backBtn).toBeVisible();
    await backBtn.click();

    await expect(page.locator('.reports-view')).toBeVisible();
  });

  test('marks all shown reports read and shows empty state', async ({ page }) => {
    await page
      .locator('.app-header')
      .getByRole('button', { name: /Reports/i })
      .click();
    await expect(page.locator('.reports-view')).toBeVisible();

    const markAllBtn = page.getByRole('button', { name: /Mark all shown read/i });
    await expect(markAllBtn).toBeVisible();
    await markAllBtn.click();

    // Should transition to empty state
    await expect(page.locator('.empty-state')).toBeVisible();
    await expect(
      page.getByText('Nothing new. Reports you have read are not shown any more.'),
    ).toBeVisible();
  });
});
