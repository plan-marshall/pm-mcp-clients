import { test, expect } from '@playwright/test';

/**
 * The fully-onboarded showroom: one machine that is onboarded and whose every operator domain is
 * populated at once. Each view is opened from the same scenario, without a scenario switch in the
 * middle of the walkthrough.
 */
test.describe('Fully onboarded showroom', () => {
  test.beforeEach(async ({ page, request }) => {
    const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
      data: { scenario: 'fully-onboarded' },
    });
    expect(res.ok()).toBeTruthy();

    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine: mock-machine');
  });

  test.afterAll(async ({ request }) => {
    try {
      await request.post('http://127.0.0.1:7420/mock/scenario', {
        data: { scenario: 'web-pairing' },
      });
    } catch {
      // Best-effort cleanup
    }
  });

  test('shows the populated header badges and the running work overview', async ({ page }) => {
    await expect(page.locator('.header-action-btn--decisions .reports-badge')).toHaveText('3');
    await expect(page.locator('.header-action-btn--reports .reports-badge')).toHaveText('5');

    await expect(page.locator('.overview')).toBeVisible();
    await expect(page.locator('.project-list')).toBeVisible();
    await expect(
      page.locator('.project-list').getByRole('tab', { name: /pm-mcp-clients/i }),
    ).toBeVisible();
    await expect(page.locator('[data-testid="plan-card"]').first()).toBeVisible();
  });

  test('browses the three enrolled projects in the explorer', async ({ page }) => {
    await page.getByTestId('nav-projects-btn').click();
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();

    await expect(
      page.getByTestId('section-favourites').getByTestId('project-card-pm-mcp-clients'),
    ).toBeVisible();
    await expect(
      page.getByTestId('section-favourites').getByTestId('project-card-pm-mcp-core'),
    ).toBeVisible();
    await expect(
      page.getByTestId('section-recent').getByTestId('project-card-plan-marshall'),
    ).toBeVisible();
    await expect(page.getByTestId('section-all-projects')).toBeVisible();

    // No fourth project is enrolled in the showroom.
    await expect(page.getByTestId('project-card-API-Sheriff')).toHaveCount(0);
    await expect(page.getByTestId('project-card-legacy-portal')).toHaveCount(0);
  });

  test('opens the start entry with its sources, refinement chat, and file tree', async ({
    page,
  }) => {
    await page.goto('/entries/ent-showroom');
    await expect(page.getByTestId('start-view')).toBeVisible();
    await expect(page.getByTestId('source-list')).toBeVisible();
    await expect(page.getByTestId('source-src-description-1')).toBeVisible();
    await expect(page.getByTestId('source-src-pr-1')).toBeVisible();
    await expect(page.getByTestId('source-src-local-1')).toBeVisible();
    await expect(page.getByTestId('source-src-lesson-1')).toBeVisible();

    // The refinement chat carries the read-back proposal.
    await page.getByTestId('source-chat').click();
    await expect(page.getByTestId('chat-panel')).toBeVisible();
    await expect(page.getByTestId('chat-proposal')).toBeVisible();
    await expect(page.getByTestId('chat-proposal-after')).toContainText('fsynced');

    // The confined file picker lists the checkout's files.
    await page.getByTestId('add-documents').click();
    await expect(page.getByTestId('confined-file-picker')).toBeVisible();
    await expect(page.getByTestId('picker-row-doc')).toBeVisible();
    await expect(page.getByTestId('picker-row-pom.xml')).toBeVisible();
  });

  test('walks the six-phase process tree of the running plan', async ({ page }) => {
    await page.locator('[data-testid="plan-card"]').first().click();
    await expect(page.getByTestId('plan-view')).toBeVisible();
    await expect(page.getByTestId('plan-title')).toHaveText('Fix Relay Transport');

    for (const phase of ['init', 'refine', 'outline', 'plan', 'execute', 'finalize']) {
      await expect(page.getByTestId(`node-${phase}`)).toBeVisible();
    }

    await expect(page.getByTestId('task-TASK-003')).toBeVisible();
    await expect(page.getByTestId('task-inspector')).toContainText('TASK-003');
    await expect(page.getByTestId('activity-entry').first()).toBeVisible();
    await expect(page.getByTestId('off-track-alert')).toContainText('off track: repeating');
  });

  test('inspects the epic workstreams and its token split', async ({ page }) => {
    await page.goto('/epics/relay-hardening');
    await expect(page.getByTestId('epic-view')).toBeVisible();
    await expect(page.getByTestId('epic-title')).toHaveText('Relay Hardening');
    await expect(page.getByTestId('epic-state')).toHaveText('running');
    await expect(page.getByTestId('epic-tokens')).toContainText('Tokens');
    await expect(page.getByTestId('running-plan-fix-relay')).toBeVisible();
    await expect(page.getByTestId('staged-plan-relay-auth-v2')).toBeVisible();
    await expect(page.getByTestId('epic-alongside-section')).toBeVisible();
  });

  test('answers the three decisions, revealing a recommendation and switching parts', async ({
    page,
  }) => {
    await page.locator('.header-action-btn--decisions').click();
    await expect(page.locator('.decisions-view')).toBeVisible();

    const cards = page.locator('.decision-card');
    await expect(cards).toHaveCount(3);

    const conflict = page.locator('[data-decision-id="Q-007"]');
    await expect(conflict).toContainText('Resolve rebase conflict');
    await expect(conflict.locator('.recommendation-banner')).toBeVisible();
    await expect(conflict.locator('.recommended-tag')).toBeVisible();

    const multiPart = page.locator('[data-decision-id="Q-012"]');
    await expect(multiPart.locator('.decision-part')).toHaveCount(3);
    await expect(multiPart.locator('.part-position')).toHaveCount(3);
    await expect(multiPart).toContainText(/part 1 of 3/i);
  });

  test('reads the unread reports and opens the completion report', async ({ page }) => {
    await page.locator('.header-action-btn--reports').click();
    await expect(page.locator('.reports-view')).toBeVisible();
    await expect(page.locator('.report-card')).toHaveCount(5);

    const ended = page.locator('.report-card', { hasText: 'fix-relay ended: merged (#214)' });
    await expect(ended).toBeVisible();
    await ended.locator('.action-btn--detail').click();

    await expect(page.locator('.completion-report')).toBeVisible();
    await expect(page.getByText('the pull request #214 was merged into main')).toBeVisible();
    await expect(page.getByText('cursor write order')).toBeVisible();
    await expect(page.locator('.tokens-section')).toBeVisible();
  });

  test('verifies the system metrics, the upgrade, and the cleanup preview', async ({ page }) => {
    await page.getByRole('button', { name: /System/i }).click();
    await expect(page.getByTestId('system-view')).toBeVisible();
    await expect(page.getByText(/pm-mcpd 0\.9\.1/i).first()).toBeVisible();
    await expect(page.getByText('Update', { exact: true })).toBeVisible();

    await page.getByRole('button', { name: /Review…/i }).click();
    await expect(page.getByTestId('cleanup-view')).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Cleanup', selected: true })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Kept for good/ })).toBeVisible();
  });

  test('verifies the machine settings and the paired devices', async ({ page }) => {
    await page.getByRole('button', { name: 'Machine settings' }).click();
    await expect(page.getByTestId('settings-view')).toBeVisible();
    await expect(page.getByTestId('devices-table')).toBeVisible();
    await expect(page.getByText('Primary Desktop Browser')).toBeVisible();
    await expect(page.getByText('Terminal Companion')).toBeVisible();

    await page.getByTestId('machine-settings-button').click();
    await expect(page.getByTestId('machine-settings-view')).toBeVisible();
    await expect(page.getByTestId('aspect-list')).toContainText('31 roles');
    await expect(page.getByTestId('aspect-list')).toContainText('Claude Code');
  });

  test('opens the process designer with its units and structure', async ({ page }) => {
    await page.goto('/designer');
    await expect(page.getByTestId('process-designer')).toBeVisible();
    await expect(page.getByTestId('designer-title')).toBeVisible();
    await expect(page.getByTestId('unit-open-landing.pull-request')).toBeVisible();
    await expect(page.getByTestId('structure-pane')).toBeVisible();
  });
});
