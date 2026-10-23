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
    const overviewNav = page.locator('.app-header').getByRole('button', { name: /Running work/i });
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
    await expect(page.locator('[data-testid="breadcrumb-epic"]')).toHaveText('Relay Hardening');
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
    await expect(entries.first()).toContainText('Reading the relay');
    await expect(entries.nth(1)).toContainText('read');
    await expect(entries.nth(2)).toContainText('bash');

    // Verify Jump button
    const jumpBtn = page.locator('[data-testid="jump-btn"]');
    await expect(jumpBtn).toBeVisible();
    await jumpBtn.click();
  });

  test('narrates every part of session-log, including done phases and done tasks', async ({
    page,
  }) => {
    // Open session-log from the overview: its project tab, then its own plan card.
    await page.getByRole('tab', { name: /pm-mcp-core/ }).click();
    const sessionLogCard = page
      .locator('[data-testid="plan-card"]')
      .filter({ hasText: 'Session log and generations' });
    await expect(sessionLogCard).toBeVisible();
    await sessionLogCard.click();

    await expect(page.locator('[data-testid="plan-view"]')).toBeVisible();

    // The positive structure: the tree on the left, the narrative on the right.
    await expect(page.locator('[data-testid="process-tree"]')).toBeVisible();
    await expect(page.locator('[data-testid="task-inspector"]')).toBeVisible();

    // A done phase narrates its own state and duration instead of leaving the panel at "select".
    await page.locator('[data-testid="select-node-init"]').click();
    const narrative = page.locator('[data-testid="node-narrative"]');
    await expect(narrative).toBeVisible();
    await expect(narrative.locator('[data-testid="node-narrative-title"]')).toContainText('init');
    await expect(narrative.locator('[data-testid="node-narrative-state"]')).toContainText('done');
    await expect(narrative.locator('[data-testid="node-narrative-duration"]')).toBeVisible();
    await expect(page.locator('[data-testid="task-inspector"]')).toHaveAttribute(
      'data-selected-kind',
      'node',
    );

    // A done leaf task still shows its live-activity feed.
    await page.locator('[data-testid="task-TASK-001"]').click();
    await expect(page.locator('[data-testid="activity-feed"]')).toContainText('TASK-001');
    await expect(page.locator('[data-testid="activity-entry"]').first()).toContainText(
      'The session log now keeps its narrative per generation.',
    );

    // Expanding and collapsing is independent of selection: the inspected task stays on the right.
    // The live feed may append an entry, so the stable head is what must not change.
    const inspector = page.locator('[data-testid="task-inspector"]');
    const headBefore = await inspector.locator('.task-inspector__head').innerText();
    const executeToggle = page.locator('[data-testid="toggle-node-execute"]');
    await expect(executeToggle).toHaveAttribute('aria-expanded', 'true');
    await executeToggle.click();
    await expect(executeToggle).toHaveAttribute('aria-expanded', 'false');
    await expect(inspector).toHaveAttribute('data-selected-kind', 'task');
    await expect(inspector.locator('.task-inspector__head')).toHaveText(headBefore);
    await expect(inspector).toContainText('TASK-001');
    await expect(page.locator('[data-testid="node-narrative"]')).toHaveCount(0);

    // Re-open the tasks, then prove the explicit empty route shows an empty feed, not a stale one.
    await executeToggle.click();
    await expect(executeToggle).toHaveAttribute('aria-expanded', 'true');
    await page.locator('[data-testid="task-TASK-002"]').click();
    const quietFeed = page.locator('[data-testid="activity-feed"]');
    await expect(quietFeed).toContainText('TASK-002');
    await expect(quietFeed).toContainText('No activity entries yet.');
    await expect(page.locator('[data-testid="activity-entry"]')).toHaveCount(0);
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

    // Verify the workstreams and their member plans
    await expect(page.locator('[data-testid="epic-workstreams"]')).toBeVisible();
    const reconnect = page.locator('[data-testid="workstream-WS-1"]');
    await expect(reconnect).toBeVisible();
    await expect(reconnect).toContainText('Reconnect');
    await expect(page.locator('[data-testid="epic-row-relay-cursor-order"]')).toContainText(
      'relay-cursor-order',
    );

    const auth = page.locator('[data-testid="workstream-WS-2"]');
    await expect(auth).toBeVisible();
    await expect(auth).toContainText('Authentication');
    await expect(page.locator('[data-testid="epic-row-relay-auth-v2"]')).toContainText(
      'relay-auth-v2',
    );

    // Verify sources, landings, registers, tokens, and events
    await expect(page.locator('[data-testid="epic-sources"]')).toBeVisible();
    await expect(page.locator('[data-testid="epic-landing-count"]')).toContainText('2 landings');
    await expect(page.locator('[data-testid="epic-findings"]')).toBeVisible();
    await expect(page.locator('[data-testid="epic-watches"]')).toBeVisible();
    await expect(page.locator('[data-testid="epic-token-breakdown"]')).toBeVisible();
    await expect(page.locator('[data-testid="epic-events"]')).toBeVisible();

    // Back returns to the plan the epic was opened from, not to the overview
    const backBtn = page.locator('[data-testid="back-btn"]');
    await expect(backBtn).toBeVisible();
    await backBtn.click();
    await expect(page.locator('[data-testid="plan-view"]')).toBeVisible();

    // The plan's own back then returns to Running Work, where it was opened
    await page.locator('[data-testid="back-btn"]').click();
    await expect(page.locator('.overview')).toBeVisible();
  });

  test('opens the project in the explorer in one step', async ({ page }) => {
    const planCard = page.locator('[data-testid="plan-card"]').first();
    await planCard.click();
    await expect(page.locator('[data-testid="plan-view"]')).toBeVisible();

    await page.locator('[data-testid="breadcrumb-project"]').click();

    await expect(page).toHaveURL(/\/projects\?project=pm-mcp-clients$/);
    await expect(page.locator('[data-testid="project-explorer-view"]')).toBeVisible();
  });

  test('discloses the token breakdown by phase and role', async ({ page }) => {
    const planCard = page.locator('[data-testid="plan-card"]').first();
    await planCard.click();
    await expect(page.locator('[data-testid="plan-tokens"]')).toBeVisible();

    const toggle = page.locator('[data-testid="tokens-toggle"]');
    await expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await toggle.click();

    await expect(toggle).toHaveAttribute('aria-expanded', 'true');
    const breakdown = page.locator('[data-testid="token-breakdown"]');
    await expect(breakdown).toBeVisible();
    await expect(breakdown).toContainText('The breakdown by phase and role is not reported yet.');

    await toggle.click();
    await expect(breakdown).toHaveCount(0);
  });
});
