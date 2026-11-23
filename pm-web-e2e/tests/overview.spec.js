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
    const overviewNav = page.locator('.nav-rail').getByRole('button', { name: /Running work/i });
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

    // In by-epic view, verify epic card and that both groupings hold the same plans and epic head count equals its rows
    const epicCard = page.getByTestId('epic-card');
    const epicTitle = epicCard.getByText('Relay Hardening');
    await expect(epicTitle).toBeVisible();
    await expect(epicCard.locator('[data-testid="plan-card"]')).toHaveCount(2);
    await expect(page.getByText('2 running · 3 staged', { exact: true })).toBeVisible();
    await expect(page.getByText('Fix Relay Transport')).toBeVisible();
    await expect(page.getByText('Drop the legacy endpoints')).toBeVisible();
    await expect(page.getByText('Relay metrics')).toBeVisible();

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

    // Verify Ended work table is displayed and columns are aligned
    const endedView = page.locator('.ended-view');
    await expect(endedView).toBeVisible();
    const table = endedView.locator('table.data-table');
    await expect(table).toBeVisible();

    const tableMetrics = await table.evaluate((el) => {
      const style = window.getComputedStyle(el);
      const ths = Array.from(el.querySelectorAll('thead th'));
      const firstRowTds = Array.from(el.querySelectorAll('tbody tr:first-child td'));
      const thRects = ths.map((th) => {
        const r = th.getBoundingClientRect();
        const cs = window.getComputedStyle(th);
        return { x: Math.round(r.x), width: Math.round(r.width), paddingLeft: parseFloat(cs.paddingLeft) };
      });
      const tdRects = firstRowTds.map((td) => {
        const r = td.getBoundingClientRect();
        const cs = window.getComputedStyle(td);
        return { x: Math.round(r.x), width: Math.round(r.width), paddingLeft: parseFloat(cs.paddingLeft) };
      });
      return {
        tableLayout: style.tableLayout,
        borderCollapse: style.borderCollapse,
        thRects,
        tdRects,
        hasKindMark: el.querySelectorAll('tbody tr .kind-mark').length,
        rowCount: el.querySelectorAll('tbody tr').length,
      };
    });
    expect(tableMetrics.tableLayout).toBe('fixed');
    expect(tableMetrics.borderCollapse).toBe('collapse');
    expect(tableMetrics.hasKindMark).toBe(tableMetrics.rowCount);
    expect(tableMetrics.thRects.length).toBe(6);
    expect(tableMetrics.tdRects.length).toBe(6);
    for (let i = 0; i < 6; i += 1) {
      expect(Math.abs(tableMetrics.thRects[i].x - tableMetrics.tdRects[i].x)).toBeLessThanOrEqual(1);
      expect(Math.abs(tableMetrics.thRects[i].width - tableMetrics.tdRects[i].width)).toBeLessThanOrEqual(1);
      expect(tableMetrics.thRects[i].paddingLeft).toBeGreaterThanOrEqual(12);
      expect(tableMetrics.tdRects[i].paddingLeft).toBeGreaterThanOrEqual(12);
    }

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

    // A page carries no Back act of its own: the rail's overview item returns to the running work,
    // and the running view lists its plan cards again.
    await page.getByTestId('nav-overview-btn').click();

    // Verify back on main running work view: some plan card is there, whichever the runtime serves.
    await expect(page.locator('.plan-card').first()).toBeVisible();
  });

  test('opens an ended plan as it ended and its completion report in one step', async ({
    page,
  }) => {
    await page.getByRole('button', { name: /^Ended work/i }).click();
    await expect(page.locator('[data-testid="ended-item"]').first()).toBeVisible();

    // The title opens the plan's own view, and the lens's mode travels with the address.
    await page.locator('[data-testid="ended-open-fix-relay-v1"]').click();
    await expect(page).toHaveURL(/\/plans\/fix-relay-v1\?mode=ended$/);
    await expect(page.locator('[data-testid="plan-view"]')).toBeVisible();

    // The browser's back returns to Ended Work, on the page the operator left.
    await page.goBack();
    await expect(page.locator('.ended-view')).toBeVisible();
    await expect(page.getByText(/page 1 of 3/i)).toBeVisible();

    // A plan's completion report is one step from its row.
    await page.locator('[data-testid="ended-report-fix-relay-v1"]').click();
    await expect(page).toHaveURL(/\/reports(\/[^/?]+)?$/);
    await expect(page.locator('.completion-report')).toBeVisible();
  });

  test('aligns plan row columns at 1440 px and renders the row menu as an icon button', async ({
    page,
    request,
  }) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    const gotoRes = await request.post(`${MOCK_RUNTIME_URL}/mock/goto/overview.running`);
    expect(gotoRes.ok()).toBeTruthy();
    await page.reload();
    await expect(page.locator('.overview')).toBeVisible();

    const rows = page.locator('[data-testid="plan-card"]');
    await expect(rows).toHaveCount(3);

    for (const colTestId of [
      'col-phase',
      'col-state',
      'col-progress',
      'col-since',
      'col-actions',
    ]) {
      const boxes = await page.locator(`[data-testid="plan-card"] [data-testid="${colTestId}"]`).evaluateAll((els) =>
        els.map((el) => el.getBoundingClientRect().x),
      );
      expect(boxes.length).toBeGreaterThanOrEqual(2);
      for (let i = 1; i < boxes.length; i += 1) {
        expect(Math.abs(boxes[i] - boxes[0]), `column ${colTestId} X mismatch`).toBeLessThanOrEqual(
          1,
        );
      }
    }

    const rowActionBtns = page.locator('[data-testid="plan-card"] [data-testid^="overview-actions-"]');
    const count = await rowActionBtns.count();
    expect(count).toBeGreaterThanOrEqual(2);
    for (let i = 0; i < count; i += 1) {
      const btn = rowActionBtns.nth(i);
      await expect(btn).toHaveClass(/\bicon-btn\b/);
      await expect(btn.locator('.ui-icon')).toBeVisible();
      expect((await btn.getAttribute('aria-label'))?.trim()).toBeTruthy();
      expect((await btn.innerText()).trim()).toBe('');
    }
  });

  test('the rail item opens running work after ended work was open, and a reload of the unfilled /overview stays unfilled', async ({
    page,
  }) => {
    await page.getByRole('button', { name: /^Ended work/i }).click();
    await expect(page.locator('.ended-view')).toBeVisible();
    await expect(page).toHaveURL(/mode=ended/);

    await page.locator('.nav-rail').getByRole('button', { name: /Running work/i }).click();
    await expect(page.locator('.ended-view')).toHaveCount(0);
    await expect(page.locator('[data-testid="plan-card"]').first()).toBeVisible();
    await expect(page).not.toHaveURL(/mode=ended/);

    // Record another project and grouping in localStorage, then reload a marked unfilled /overview.
    await page.locator('.project-list').getByRole('tab', { name: /pm-mcp-core/i }).click();
    await expect(page.locator('.project-title')).toContainText('pm-mcp-core');
    await page.getByRole('radio', { name: /by epic/i }).click();
    await expect(page.getByRole('radio', { name: /by epic/i })).toHaveAttribute(
      'aria-checked',
      'true',
    );

    await page.evaluate(() => {
      window.history.pushState({ path: '/overview', pmApp: true }, '', '/overview');
    });
    await page.reload();
    await expect(page.locator('.overview')).toBeVisible();
    await expect(page).toHaveURL(/\/overview$/);
    await expect(page.getByRole('radio', { name: /by plan/i })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    await expect(page.locator('.project-item--selected')).toContainText('pm-mcp-clients');
  });
});
