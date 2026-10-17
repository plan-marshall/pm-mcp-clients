import { test, expect } from '@playwright/test';

test.describe('Plan and Epic Views (UC-PLN-1, UC-PLN-5)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to plan-view scenario
    const switchRes = await request.post('http://127.0.0.1:7420/mock/scenario', {
      data: { scenario: 'plan-view' },
    });
    expect(switchRes.ok()).toBeTruthy();

    // Clear localStorage and pair with valid code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    // Navigate to Running Work overview
    const overviewNav = page.getByRole('button', { name: /Running work/i });
    await expect(overviewNav).toBeVisible();
    await overviewNav.click();

    // Ensure overview container is mounted
    await expect(page.locator('.overview')).toBeVisible();
  });

  test.afterAll(async ({ request }) => {
    // Reset back to web-pairing scenario for other test suites
    try {
      await request.post('http://127.0.0.1:7420/mock/scenario', {
        data: { scenario: 'web-pairing' },
      });
    } catch {
      // Best-effort cleanup
    }
  });

  test('views plan process tree, task activity, and off-track indicator', async ({ page }) => {
    // Click on fix-relay plan card in overview
    const planCard = page.locator('[data-testid="plan-card"]').first();
    await expect(planCard).toBeVisible();
    await planCard.click();

    // Ensure PlanView is mounted
    const planView = page.locator('[data-testid="plan-view"]');
    await expect(planView).toBeVisible();

    // Verify breadcrumb path project › epic › plan
    await expect(page.locator('[data-testid="breadcrumb-project"]')).toHaveText('pm-mcp-clients');
    await expect(page.locator('[data-testid="breadcrumb-epic"]')).toHaveText('relay-hardening');
    await expect(page.locator('[data-testid="breadcrumb-plan"]')).toHaveText('fix-relay');

    // Verify title and badges
    await expect(page.locator('[data-testid="plan-title"]')).toHaveText('Fix Relay Transport');
    await expect(page.locator('[data-testid="plan-phase"]')).toHaveText('execute');
    await expect(page.locator('[data-testid="plan-state"]')).toHaveText('running');
    await expect(page.locator('[data-testid="pr-link"]')).toHaveText('#214');

    // Verify process tree nodes
    const tree = page.locator('[data-testid="process-tree"]');
    await expect(tree).toBeVisible();
    await expect(page.locator('[data-testid="node-init"]')).toBeVisible();
    await expect(page.locator('[data-testid="node-execute"]')).toBeVisible();

    // Verify leaf task in tree
    const taskRow = page.locator('[data-testid="task-TASK-003"]');
    await expect(taskRow).toBeVisible();
    await expect(taskRow).toContainText('TASK-003');
    await expect(taskRow).toContainText('implement');

    // Verify task activity inspector on the right
    const inspector = page.locator('[data-testid="task-inspector"]');
    await expect(inspector).toBeVisible();
    await expect(inspector).toContainText('TASK-003');
    await expect(inspector).toContainText('implement');

    // Verify off-track warning banner
    const offTrackAlert = page.locator('[data-testid="off-track-alert"]');
    await expect(offTrackAlert).toBeVisible();
    await expect(offTrackAlert).toContainText('off track: repeating');

    // Verify activity feed entries
    const entries = page.locator('[data-testid="activity-entry"]');
    await expect(entries.first()).toBeVisible();
    await expect(entries).toContainText(['Reading the relay transport', 'read', 'bash']);

    // Verify Jump button
    const jumpBtn = page.locator('[data-testid="jump-btn"]');
    await expect(jumpBtn).toBeVisible();
    await jumpBtn.click();
  });

  test('navigates to epic view, verifies running and staged plans, and returns', async ({
    page,
  }) => {
    // Open fix-relay plan view
    const planCard = page.locator('[data-testid="plan-card"]').first();
    await expect(planCard).toBeVisible();
    await planCard.click();

    await expect(page.locator('[data-testid="plan-view"]')).toBeVisible();

    // Click epic link in breadcrumb to navigate to EpicView
    const epicBreadcrumb = page.locator('[data-testid="breadcrumb-epic"]');
    await expect(epicBreadcrumb).toBeVisible();
    await epicBreadcrumb.click();

    // Ensure EpicView is mounted
    const epicView = page.locator('[data-testid="epic-view"]');
    await expect(epicView).toBeVisible();

    // Verify epic breadcrumbs and title
    await expect(page.locator('[data-testid="breadcrumb-project"]')).toHaveText('pm-mcp-clients');
    await expect(page.locator('[data-testid="breadcrumb-epic"]')).toHaveText('relay-hardening');
    await expect(page.locator('[data-testid="epic-title"]')).toHaveText('Relay Hardening');
    await expect(page.locator('[data-testid="epic-state"]')).toHaveText('running');

    // Verify running plans
    const runningPlan = page.locator('[data-testid="running-plan-fix-relay"]');
    await expect(runningPlan).toBeVisible();
    await expect(runningPlan).toContainText('Fix Relay Transport');
    await expect(runningPlan).toContainText('execute');

    // Verify staged plans
    const stagedPlan = page.locator('[data-testid="staged-plan-relay-auth-v2"]');
    await expect(stagedPlan).toBeVisible();
    await expect(stagedPlan).toContainText('Relay Auth v2');

    // Verify alongside landings
    const alongsideSection = page.locator('[data-testid="epic-alongside-section"]');
    await expect(alongsideSection).toBeVisible();
    await expect(alongsideSection).toContainText('remediation.ci-failure · done');

    // Click back button to return to overview
    const backBtn = page.locator('[data-testid="back-btn"]');
    await expect(backBtn).toBeVisible();
    await backBtn.click();

    await expect(page.locator('.overview')).toBeVisible();
  });
});
