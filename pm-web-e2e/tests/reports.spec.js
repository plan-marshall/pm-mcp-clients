import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

/**
 * The number the heading names as unread. The scenario also announces a read of a report outside
 * the loaded page, so only a comparison before and after is stable.
 *
 * @param {import('@playwright/test').Page} page the reports page
 * @returns {Promise<number>} the unread count, or `NaN` when the heading names none
 */
async function unreadCount(page) {
  const text = (await page.getByTestId('unread-count').textContent()) ?? '';
  return Number(text.match(/\d+/)?.[0] ?? Number.NaN);
}

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

  test('marks a single report read from its row menu and removes it from view', async ({
    page,
  }) => {
    // Navigate to reports view
    await page
      .locator('.nav-rail')
      .getByRole('button', { name: /Reports/i })
      .click();
    await expect(page.locator('.reports-view')).toBeVisible();

    // Look for Ended: no change (rep-0139)
    const row = page.locator('.report-card', { hasText: 'Ended: no change' });
    await expect(row).toBeVisible();

    // Sibling pattern: wait for the initial unread count before reading rows
    await expect(page.getByTestId('unread-count')).toContainText('7 unread reports');

    // Open this row's menu and mark the one report read
    const rows = page.locator('.report-card');
    const initialCount = await rows.count();
    expect(initialCount).toBeGreaterThan(0);

    await row.getByRole('button', { name: /Report actions/i }).click();
    await row.getByRole('menuitem', { name: /Mark read/i }).click();

    // The read report leaves the list; waiting on the count waits on the view's own condition.
    await expect(rows).toHaveCount(initialCount - 1);
  });

  test('renders short body fitting on one line and long body with clamp control (D2a)', async ({
    page,
  }) => {
    await page
      .locator('.nav-rail')
      .getByRole('button', { name: /Reports/i })
      .click();
    await expect(page.locator('.reports-view')).toBeVisible();

    // rep-0142 has a short body that fits on one line without overflow
    const shortCard = page.locator('.report-card', { hasText: 'Ended: merged (#214)' });
    await expect(shortCard).toBeVisible();
    await expect(shortCard.locator('.report-card__body')).toHaveText('Brief note.');
    // Short body does not overflow, so no clamp control is rendered
    await expect(shortCard.getByRole('button', { name: /Show the text/i })).toHaveCount(0);

    // rep-0143 has a long body that wraps across lines
    const longCard = page.locator('.report-card', { hasText: 'relay-token-store started' });
    await expect(longCard).toBeVisible();
    const showBtn = longCard.getByRole('button', { name: /Show the text/i });
    await expect(showBtn).toBeVisible();

    // Check heights: when clamped, the body is 1 line. When expanded, height increases.
    const initialBox = await longCard.locator('.report-card__body').boundingBox();
    expect(initialBox).not.toBeNull();

    await showBtn.click();
    const hideBtn = longCard.getByRole('button', { name: /Hide the text/i });
    await expect(hideBtn).toBeVisible();

    const expandedBox = await longCard.locator('.report-card__body').boundingBox();
    expect(expandedBox).not.toBeNull();
    expect(expandedBox.height).toBeGreaterThan(initialBox.height);
  });

  test('sets unread rows apart with a dot and a bold subject, and opening reads them', async ({
    page,
  }) => {
    await page
      .locator('.nav-rail')
      .getByRole('button', { name: /Reports/i })
      .click();
    await expect(page.locator('.reports-view')).toBeVisible();

    const row = page.locator('.report-card', { hasText: 'Ended: merged (#214)' });

    await expect(row).toHaveAttribute('data-unread', 'true');
    await expect(row.locator('.report-card__unread-dot')).toBeVisible();
    const subject = row.locator('.report-card__subject');
    await expect(subject).toHaveClass(/report-card__subject--unread/);
    expect(await subject.evaluate((element) => getComputedStyle(element).fontWeight)).toBe('600');

    const before = await unreadCount(page);

    // Opening the report reads it, in the same act.
    await row.getByRole('button', { name: /Show the details/i }).click();
    await expect(page.locator('.completion-report')).toBeVisible();
    await expect.poll(() => unreadCount(page)).toBeLessThan(before);

    // While the detail is open the row stays beside it, read: no dot, the normal weight.
    const active = page.locator('.report-card--active');
    await expect(active).toHaveCount(1);
    await expect(active).toHaveAttribute('data-unread', 'false');
    await expect(active.locator('.report-card__unread-dot')).toHaveCount(0);
    await expect(active.getByTestId('report-read-state')).toContainText('read');

    // Back on the list the report the view read is gone from the unread reports.
    await page.getByRole('button', { name: /^Back$/ }).click();
    await expect(row).toHaveCount(0);
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

  test('marks all shown reports read and reloads the remaining unread reports', async ({
    page,
  }) => {
    await page
      .locator('.nav-rail')
      .getByRole('button', { name: /Reports/i })
      .click();
    await expect(page.locator('.reports-view')).toBeVisible();

    // The scenario announces one report and its read a moment after the list was drawn. Wait for
    // that pair to settle, so the mark-all acts on the stable 7 unread reports of the first page.
    await expect(page.getByTestId('report-arrival-notice')).toBeAttached();
    await expect(page.getByTestId('unread-count')).toContainText('7 unread reports');

    // Only the first page is loaded: the 2 older unread reports sit behind the older control.
    await expect(page.getByRole('button', { name: /Show 2 older/i })).toBeVisible();

    const markAllBtn = page.getByRole('button', { name: /Mark all shown read/i });
    await expect(markAllBtn).toBeVisible();
    await markAllBtn.click();

    // The first page is reloaded from the server: the 5 shown reports are read, and the 2 older
    // reports are now the first page, with the server's count. The empty state is never shown.
    await expect(page.getByTestId('unread-count')).toContainText('2 unread reports');
    await expect(page.locator('.report-card')).toHaveCount(2);
    await expect(
      page.locator('.report-card', { hasText: 'relay-auth-v2 ended: merged (#210)' }),
    ).toBeVisible();
    await expect(
      page.locator('.report-card', { hasText: 'A source of codeberg-issues was flagged' }),
    ).toBeVisible();
    await expect(page.locator('.empty-state')).toHaveCount(0);

    // The 2 remaining reports fit on one page, so the older control is not offered.
    await expect(page.getByRole('button', { name: /Show .* older/i })).toHaveCount(0);
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

  test('keeps the list beside the detail while the detail is open', async ({ page }) => {
    const before = await unreadCount(page);
    expect(before).toBeGreaterThan(0);

    const card = page.locator('.report-card', { hasText: 'Ended: merged (#214)' });
    await card.getByRole('button', { name: /Show the details/i }).click();

    await expect(page.locator('.completion-report')).toBeVisible();
    await expect(page.locator('.reports-tree')).toBeVisible();
    await expect(page.locator('.report-card--active')).toHaveCount(1);
    // One read moves the count; the scenario's own announce of another read may move it once more.
    await expect.poll(() => unreadCount(page)).toBeLessThan(before);

    await page.reload();
    await expect(page.locator('.reports-tree')).toBeVisible();
    await expect(page.getByTestId('layout-split')).toHaveCount(0);
  });
});
