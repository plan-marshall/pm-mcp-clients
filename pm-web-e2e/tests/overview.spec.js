import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

test.describe('Overview View (UC-PLN-4)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to running-work scenario
    const switchRes = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'running-work' },
    });
    expect(switchRes.ok()).toBeTruthy();

    // Clear localStorage and pair with valid code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    // Navigate to Running Work overview
    const overviewNav = page.locator('.app-header').getByRole('button', { name: /Running work/i });
    await expect(overviewNav).toBeVisible();
    await overviewNav.click();

    // Ensure overview container is mounted
    await expect(page.locator('.overview')).toBeVisible();
  });

  test.afterAll(async ({ request }) => {
    // Reset back to web-pairing scenario for other test suites
    try {
      await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
        data: { scenario: 'web-pairing' },
      });
    } catch {
      // Best-effort cleanup
    }
  });

  test('views projects list and selects a project', async ({ page }) => {
    // Verify projects are rendered in sidebar
    const projectList = page.locator('.project-list');
    await expect(projectList).toBeVisible();

    const clientsItem = projectList.getByRole('tab', { name: /pm-mcp-clients/i });
    const coreItem = projectList.getByRole('tab', { name: /pm-mcp-core/i });
    const pmItem = projectList.getByRole('tab', { name: /plan-marshall/i });

    await expect(clientsItem).toBeVisible();
    await expect(coreItem).toBeVisible();
    await expect(pmItem).toBeVisible();

    // pm-mcp-clients should be selected first by default (has needs-operator items)
    await expect(clientsItem).toHaveAttribute('aria-selected', 'true');
    await expect(page.locator('.project-title')).toContainText('pm-mcp-clients');

    // Switch to pm-mcp-core
    await coreItem.click();
    await expect(coreItem).toHaveAttribute('aria-selected', 'true');
    await expect(page.locator('.project-title')).toContainText('pm-mcp-core');

    // Verify pm-mcp-core details
    await expect(page.getByText('Session log and generations')).toBeVisible();
  });

  test('toggles between by-plan and by-epic views', async ({ page }) => {
    // In by-plan view (default)
    const byPlanBtn = page.getByRole('radio', { name: /by plan/i });
    const byEpicBtn = page.getByRole('radio', { name: /by epic/i });

    await expect(byPlanBtn).toBeVisible();
    await expect(byEpicBtn).toBeVisible();

    // Verify plans are shown directly
    await expect(page.getByText('Fix Relay Transport')).toBeVisible();
    await expect(page.getByText('Drop the legacy endpoints')).toBeVisible();
    await expect(page.getByText('Relay metrics')).toBeVisible();

    // Switch to by-epic view
    await byEpicBtn.click();
    await expect(byEpicBtn).toHaveAttribute('aria-checked', 'true');

    // In by-epic view, verify epic card
    const epicTitle = page.getByTestId('epic-card').getByText('Relay Hardening');
    await expect(epicTitle).toBeVisible();
    await expect(page.getByText('1 running · 3 staged', { exact: true })).toBeVisible();

    // Switch back to by-plan
    await byPlanBtn.click();
    await expect(byPlanBtn).toHaveAttribute('aria-checked', 'true');
    await expect(page.getByText('Fix Relay Transport')).toBeVisible();
  });

  test('views attention markers and off-track indicators', async ({ page }) => {
    // pm-mcp-clients has 2 needs-operator plans
    const clientsBadge = page.locator('.project-item--selected .badge--attention');
    await expect(clientsBadge).toBeVisible();
    await expect(clientsBadge).toContainText('! 2');

    // Plan card attention marker
    const decisionBadge = page.locator('.badge--attention').first();
    await expect(decisionBadge).toBeVisible();

    // Verify off-track badge on plan with repeating signal
    const offTrackBadge = page.locator('.badge--off-track').first();
    await expect(offTrackBadge).toBeVisible();
    await expect(offTrackBadge).toContainText('repeating');
  });

  test('navigates to ended work, verifies outcome badges and pagination', async ({ page }) => {
    // Click Ended work link
    const endedLink = page.getByRole('button', { name: /^Ended work/i });
    await expect(endedLink).toBeVisible();
    await endedLink.click();

    // Verify Ended work table is displayed
    const endedView = page.locator('.ended-view');
    await expect(endedView).toBeVisible();
    await expect(endedView.locator('table.data-table')).toBeVisible();

    // Verify outcomes (merged, cancelled)
    await expect(page.getByText('fix-relay')).toBeVisible();
    await expect(page.locator('.badge--outcome').first()).toBeVisible();

    // Verify pagination controls
    const olderBtn = page.getByRole('button', { name: /Older/i });
    const newerBtn = page.getByRole('button', { name: /Newer/i });

    await expect(olderBtn).toBeVisible();
    await expect(olderBtn).toBeEnabled();
    await expect(newerBtn).toBeDisabled();

    // Click Older to load page 2
    await olderBtn.click();
    await expect(page.getByText(/page 2 of 3/i)).toBeVisible();
    await expect(newerBtn).toBeEnabled();
    await expect(olderBtn).toBeEnabled();

    // Click Newer to return to page 1
    await newerBtn.click();
    await expect(page.getByText(/page 1 of 3/i)).toBeVisible();

    // Click Back to return to running work view
    const backBtn = page.getByRole('button', { name: /Back/i });
    await expect(backBtn).toBeVisible();
    await backBtn.click();

    // Verify back on main running work view
    await expect(page.locator('.plan-card').first()).toBeVisible();
  });

  test('opens an ended plan as it ended and its completion report in one step', async ({ page }) => {
    await page.getByRole('button', { name: /^Ended work/i }).click();
    await expect(page.locator('[data-testid="ended-item"]').first()).toBeVisible();

    // The title opens the plan's own view, remembering the section it was opened from.
    await page.locator('[data-testid="ended-open-fix-relay-v1"]').click();
    await expect(page).toHaveURL(/\/plans\/fix-relay-v1$/);
    await expect(page.locator('[data-testid="plan-view"]')).toBeVisible();

    // The browser's back returns to Ended Work, on the page the operator left.
    await page.goBack();
    await expect(page.locator('.ended-view')).toBeVisible();
    await expect(page.getByText(/page 1 of 3/i)).toBeVisible();

    // A plan's completion report is one step from its row.
    await page.locator('[data-testid="ended-report-fix-relay-v1"]').click();
    await expect(page).toHaveURL(/\/reports\?plan=fix-relay-v1$/);
    await expect(page.locator('.completion-report')).toBeVisible();
  });

  test('remembers Running Work as the browser start page across a reload', async ({ page }) => {
    const toggle = page.locator('[data-testid="start-page-toggle"]');
    await expect(toggle).toHaveText('Set as browser start page');
    await toggle.click();

    await expect(toggle).toHaveText('Default start page');
    expect(await page.evaluate(() => localStorage.getItem('pm_start_route'))).toBe('overview');

    await page.reload();
    await expect(page.locator('.overview')).toBeVisible();
    await expect(page.locator('[data-testid="start-page-toggle"]')).toHaveText(
      'Default start page',
    );
  });
});
