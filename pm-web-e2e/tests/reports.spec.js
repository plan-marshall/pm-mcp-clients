import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

test.describe('Reports & Completion Reports Flow (UC-PLN-6)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to reports scenario
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'reports' },
    });
    expect(res.ok()).toBeTruthy();

    // Clear localStorage and pair with code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test('opens the reports view from the header navigation', async ({ page }) => {
    const reportsBtn = page.locator('.nav-rail').getByRole('button', { name: /Reports/i });
    await expect(reportsBtn).toBeVisible();
    await reportsBtn.click();

    await expect(page.locator('.reports-view')).toBeVisible();
    await expect(page.locator('.reports-heading')).toContainText('Reports');

    // Verify grouped hierarchy
    await expect(page.getByText('pm-mcp-clients').first()).toBeVisible();
    await expect(page.getByText('relay-hardening').first()).toBeVisible();
    await expect(page.getByText('fix-relay').first()).toBeVisible();
    await expect(page.getByText('Ended: merged (#214)')).toBeVisible();
  });

  test('marks a single report read and removes it from view', async ({ page }) => {
    // Navigate to reports view
    await page
      .locator('.nav-rail')
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
      .locator('.nav-rail')
      .getByRole('button', { name: /Reports/i })
      .click();
    await expect(page.locator('.reports-view')).toBeVisible();

    // Click Show the details on fix-relay report
    const card = page.locator('.report-card', { hasText: 'Ended: merged (#214)' });
    const detailBtn = card.getByRole('button', { name: /Show the details/i });
    await expect(detailBtn).toBeVisible();
    await detailBtn.click();
    await expect(page).toHaveURL(/plan=fix-relay/);

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
      .locator('.nav-rail')
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

test.describe('Reports: paths, details, and the quiet notice (UC-PLN-6)', () => {
  test.beforeEach(async ({ page, request }) => {
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'reports' },
    });
    expect(res.ok()).toBeTruthy();

    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
    await page
      .locator('.nav-rail')
      .getByRole('button', { name: /Reports/i })
      .click();
    await expect(page.locator('.reports-view')).toBeVisible();
  });

  test('each part of the from path opens its view without reading the report', async ({ page }) => {
    await page
      .getByRole('button', { name: /Open the plan fix-relay/i })
      .first()
      .click();
    await expect(page).toHaveURL(/\/plans\/fix-relay$/);

    await page
      .locator('.nav-rail')
      .getByRole('button', { name: /Reports/i })
      .click();
    await expect(page.getByText('Ended: merged (#214)')).toBeVisible();
  });

  test('opens an epic report at its epic', async ({ page }) => {
    const card = page.locator('.report-card', { hasText: 'relay-docs waits' });
    await card.getByRole('button', { name: /Show the details/i }).click();
    await expect(page).toHaveURL(/\/epics\/relay-hardening$/);
  });

  test('opens a project report at the project list', async ({ page }) => {
    const card = page.locator('.report-card', { hasText: 'pm-mcp-core is onboarded' });
    await card.getByRole('button', { name: /Show the details/i }).click();
    await expect(page).toHaveURL(/\/projects(\?.*)?$/);
  });

  test('announces an arriving report politely and coalesces it', async ({ page, request }) => {
    const title = await page.title();

    const report = {
      report: {
        id: 'rep-9001',
        from: {
          project_id: 'pm-mcp-clients',
          epic_id: 'relay-hardening',
          plan_id: 'new-plan',
        },
        kind: 'plan.started',
        subject: 'new-plan started by relay-hardening',
        created_at: '2026-10-21T10:00:00Z',
      },
    };
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/broadcast`, {
      data: { event: 'report.sent', data: report },
    });
    expect(res.ok()).toBeTruthy();

    const notice = page.getByTestId('report-arrival-notice');
    await expect(notice).toBeAttached();
    await expect(notice).toHaveAttribute('aria-live', 'polite');
    await expect(notice).toContainText('new-plan started by relay-hardening');
    expect(await page.title()).toBe(title);
  });

  test('filters the loaded reports as you type and shows a no-match state', async ({ page }) => {
    const filter = page.getByTestId('filter-input');
    await expect(filter).toBeVisible();

    await filter.fill('overlap with relay-auth-v2');

    await expect(page.locator('.report-card', { hasText: 'relay-docs waits' })).toBeVisible();
    await expect(page.locator('.report-card')).toHaveCount(1);
    await expect(page.getByTestId('filter-results')).toContainText('1 match');
    await expect(page.getByTestId('loaded-only-hint')).toBeVisible();

    await filter.fill('nothing-matches-this');

    await expect(page.getByTestId('report-filter-empty')).toBeVisible();
    await expect(page.getByTestId('filter-results')).toContainText('No matches');
    await expect(page.getByTestId('loaded-only-hint')).toBeVisible();
  });

  test('keeps the list beside the detail in split mode and remembers the layout', async ({
    page,
  }) => {
    await page.getByTestId('layout-split').click();
    await expect(page.locator('.reports-view')).toHaveAttribute('data-layout', 'split');
    await expect(page.locator('.unread-pill')).toContainText('7 unread reports');

    const card = page.locator('.report-card', { hasText: 'Ended: merged (#214)' });
    await card.getByRole('button', { name: /Show the details/i }).click();

    await expect(page.locator('.completion-report')).toBeVisible();
    await expect(page.locator('.reports-tree')).toBeVisible();
    await expect(page.locator('.report-card--active')).toBeVisible();
    // One read decrements the pill once, whichever of the event and the response arrives first.
    await expect(page.locator('.unread-pill')).toContainText('6 unread reports');

    await page.reload();
    await expect(page.locator('.reports-view')).toHaveAttribute('data-layout', 'split');
  });
});
