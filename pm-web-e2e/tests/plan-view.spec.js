import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';
import { openPlan } from './helpers/plans.js';

test.describe('Plan and Epic Views (UC-PLN-1, UC-PLN-5)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to plan-view scenario
    const switchRes = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'plan-view' },
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

  test('views the plan head on the object page, the process tree, task activity and off-track indicator', async ({
    page,
  }) => {
    // Open the fix-relay plan from the overview.
    await openPlan(page, 'Fix Relay Transport');

    // The head draws the object template's slots: kind, title, id chip, PR chip, phase, state.
    await expect(
      page.getByTestId('plan-kind').locator('[data-kind="plan"]'),
    ).toBeVisible();
    await expect(page.getByTestId('plan-title')).toHaveText('Fix Relay Transport');
    await expect(page.getByTestId('plan-id-chip')).toHaveText('fix-relay');
    await expect(page.getByTestId('plan-pr-chip')).toContainText('PR #214');
    await expect(page.getByTestId('plan-phase')).toHaveText('execute');
    await expect(page.getByTestId('plan-state')).toHaveText('running');
    await expect(page.getByTestId('plan-state-sentence')).toContainText('for 2 h 10 min');

    // The top bar's breadcrumb is the only path: the head draws none (D1).
    await expect(page.getByTestId('plan-breadcrumb')).toHaveCount(0);
    await expect(page.getByTestId('breadcrumb')).toContainText('Fix Relay Transport');

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

    // Jump to now is an icon button named by the catalogue; it brings the running node into view.
    const jumpBtn = page.getByRole('button', { name: 'to where it is' });
    await expect(jumpBtn).toHaveAttribute('data-testid', 'jump-btn');
    await jumpBtn.click();
    await expect(page.getByTestId('node-execute')).toBeInViewport();
  });

  test('shows taken and not-taken steps, leaf rows without a toggle and the prune reason', async ({
    page,
    request,
  }) => {
    // The `process.phase.execute` state of the running-work scenario serves the plan in execute.
    const moved = await request.post(`${MOCK_RUNTIME_URL}/mock/goto/process.phase.execute`);
    expect(moved.ok()).toBeTruthy();

    await page.goto('/plans/fix-relay');
    await expect(page.getByTestId('process-tree')).toBeVisible();

    // The taken step runs; the step beside it and the sub-step below the planned sub-workflow are
    // muted, their state word kept.
    await expect(page.getByTestId('node-execute.envelope')).toHaveClass(/node-item--running/);
    const notTakenStep = page.getByTestId('node-execute.remediation');
    await expect(notTakenStep).toHaveClass(/node-item--not_taken/);
    await expect(notTakenStep).toContainText('not taken');
    await expect(page.getByTestId('node-execute.verify')).not.toHaveClass(/node-item--not_taken/);
    await expect(page.getByTestId('node-execute.verify.integration-tests')).toHaveClass(
      /node-item--not_taken/,
    );

    // A leaf has no toggle; a node with something to open keeps it.
    await expect(page.getByTestId('toggle-node-execute.remediation')).toHaveCount(0);
    await expect(page.getByTestId('toggle-node-execute.envelope')).toHaveAttribute(
      'aria-expanded',
      'true',
    );

    // The land-lane plan names why its phases were left out in the catalogue's words.
    const pruned = await request.post(`${MOCK_RUNTIME_URL}/mock/goto/process.pruned-lane`);
    expect(pruned.ok()).toBeTruthy();
    await page.goto('/plans/docs-typo');
    const box = page.getByTestId('not-in-plan-box');
    await expect(box).toContainText('the land lane');
    await expect(box).not.toContainText('lane_choice');
  });

  test('keeps the selected node in the address across the tabs and a fresh load', async ({
    page,
  }) => {
    await openPlan(page, 'Fix Relay Transport');

    await page.getByTestId('select-node-init').click();
    await expect(page).toHaveURL(/[?&]node=init(&|$)/);

    await page.getByRole('tab', { name: 'Intent' }).click();
    await expect(page).toHaveURL(/tab=intent/);
    await expect(page).not.toHaveURL(/node=/);

    await page.getByRole('tab', { name: 'Process' }).click();
    await expect(page).toHaveURL(/[?&]node=init(&|$)/);

    const freshPage = await page.context().newPage();
    await freshPage.goto('/plans/fix-relay?node=init');
    await expect(freshPage.getByTestId('select-node-init')).toHaveAttribute('aria-current', 'true');
    await expect(freshPage.getByTestId('node-narrative-title')).toContainText('init');
    await freshPage.close();
  });

  test('links the narrative of refine to the Intent tab', async ({ page }) => {
    await openPlan(page, 'Fix Relay Transport');

    await page.getByTestId('select-node-refine').click();
    const link = page.getByTestId('node-intent-link');
    await expect(link).toHaveText('Intent');
    await link.click();

    await expect(page).toHaveURL(/tab=intent/);
    await expect(page).not.toHaveURL(/node=/);
    await expect(page.getByTestId('plan-intent-panel-tab')).toBeVisible();
  });

  test('draws the attention strip and the decision link of a waiting plan', async ({
    page,
    request,
  }) => {
    // The `decisions.open` state serves the plan that waits for its decision.
    const moved = await request.post(`${MOCK_RUNTIME_URL}/mock/goto/decisions.open`);
    expect(moved.ok()).toBeTruthy();

    await page.goto('/plans/drop-legacy-api');
    await expect(page.locator('[data-testid="plan-view"]')).toBeVisible();
    await expect(page.getByTestId('plan-title')).toHaveText('Drop the legacy endpoints');

    // The waiting plan says what it waits for, and the decision is a head link that opens it in
    // place beside the plan.
    await expect(page.getByTestId('plan-attention-strip')).toContainText(
      'Waits for your decision',
    );
    const link = page.getByTestId('plan-decision-link');
    await expect(link).toHaveText('Open the decision');
    await expect(link).toHaveAttribute('href', '/plans/drop-legacy-api?open=decision:Q-009');

    // The link opens the decision that waits in place, and the history's back returns to the plan.
    await page.setViewportSize({ width: 1440, height: 900 });
    await link.click();
    await expect(page).toHaveURL(/\/plans\/drop-legacy-api\?open=decision:Q-009$/);
    await expect(page.getByTestId('plan-view')).toBeVisible();
    await expect(page.getByTestId('decision-detail')).toContainText(
      'Delete the published v1 endpoints from the release branch?',
    );

    await page.goBack();
    await expect(page).toHaveURL(/\/plans\/drop-legacy-api$/);
    await expect(page.getByTestId('decision-detail')).toHaveCount(0);

    // A reload of the `open=` address closes back to the plan, the entry having nothing to return
    // to: a fresh page, so the plan's own entry is not behind it.
    const freshPage = await page.context().newPage();
    await freshPage.goto('/plans/drop-legacy-api?open=decision:Q-009');
    await expect(freshPage.getByTestId('decision-detail')).toBeVisible();
    await freshPage.getByTestId('plan-in-place-close').click();
    await expect(freshPage).toHaveURL(/\/plans\/drop-legacy-api$/);
    await expect(freshPage.getByTestId('decision-detail')).toHaveCount(0);
    await freshPage.close();
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

  test('discloses the token breakdown by phase and role', async ({ page }) => {
    await openPlan(page, 'Fix Relay Transport');
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
