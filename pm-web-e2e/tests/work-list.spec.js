import { test, expect } from '@playwright/test';
import { pair, switchScenario } from './helpers/pairing.js';

/**
 * Work list column, Up navigation and top bar breadcrumb (PLAN-144 D5).
 *
 * Verifies on scenario `running-work`:
 * - Work list column beside a plan/epic with sibling replacement
 * - Up navigation from plan to epic and epic to project
 * - Top bar breadcrumb segments with KindMark and unfilled lens list behavior
 * - Phone layout hiding work list and showing the bottom bar
 */
test.describe('Work list column and navigation (PLAN-144 D5)', () => {
  test.beforeEach(async ({ page, request }) => {
    await switchScenario(request, 'running-work');
    await pair(page, request);
  });

  test.afterAll(async ({ request }) => {
    try {
      await switchScenario(request, 'web-pairing');
    } catch {
      // Best-effort cleanup
    }
  });

  test('lists the siblings of the open object and navigates back by Up', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 });

    // Open fix-relay plan
    await page.goto('/plans/fix-relay');
    await expect(page.getByTestId('plan-view')).toBeVisible();

    // The column lists the plans of the plan's epic — its siblings — not the project's epics
    const workList = page.getByTestId('work-list-column');
    await expect(workList).toBeVisible();

    const fixRelayRow = page.getByTestId('work-item-fix-relay');
    await expect(fixRelayRow).toBeVisible();
    await expect(fixRelayRow.getByRole('link')).toHaveAttribute('aria-current', 'page');
    await expect(page.getByTestId('work-item-relay-hardening')).toHaveCount(0);

    // Up in the column head points to the parent epic (Relay Hardening)
    const planUpLink = workList.getByTestId('up-link');
    await expect(planUpLink).toBeVisible();
    await expect(planUpLink).toContainText('Relay Hardening');
    await expect(planUpLink).toHaveAttribute('href', '/epics/relay-hardening');

    await planUpLink.click();
    await expect(page).toHaveURL(/\/epics\/relay-hardening/);
    await expect(page.getByTestId('epic-view')).toBeVisible();

    // From the epic, the column lists the project's epics and the plans that belong to none
    const epicLinkInList = page.getByTestId('work-item-relay-hardening').getByRole('link');
    await expect(epicLinkInList).toHaveAttribute('aria-current', 'page');
    await expect(page.getByTestId('work-item-drop-legacy-api')).toBeVisible();

    // From epic, Up leads to the project's level of Running work
    const epicUpLink = page.getByTestId('work-list-column').getByTestId('up-link');
    await expect(epicUpLink).toContainText('pm-mcp-clients');
    await expect(epicUpLink).toHaveAttribute('href', '/overview?project=pm-mcp-clients');

    // One step to the plan that belongs to no epic: it is a row of the epic's column
    await page.getByTestId('work-item-drop-legacy-api').getByRole('link').click();
    await expect(page).toHaveURL(/\/plans\/drop-legacy-api/);
    await expect(page.getByTestId('plan-view')).toBeVisible();

    // Its own Up is the project, since no epic stands between them
    const looseUpLink = page.getByTestId('work-list-column').getByTestId('up-link');
    await expect(looseUpLink).toHaveAttribute('href', '/overview?project=pm-mcp-clients');

    await looseUpLink.click();
    await expect(page).toHaveURL(/\/overview\?project=pm-mcp-clients/);
    await expect(page.locator('.overview')).toBeVisible();
  });

  test('renders top bar breadcrumb segments with KindMark and navigates to levels', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 1440, height: 900 });

    await page.goto('/plans/fix-relay');
    await expect(page.getByTestId('plan-view')).toBeVisible();

    const breadcrumb = page.getByTestId('breadcrumb');
    await expect(breadcrumb).toBeVisible();

    // Segments: Running work › pm-mcp-clients › Relay Hardening › Fix Relay Transport
    const links = breadcrumb.locator('.breadcrumb__link');
    await expect(links).toHaveCount(3);

    // Lens segment
    await expect(links.nth(0)).toContainText('Running work');
    await expect(links.nth(0).locator('.kind-mark[data-kind="plan"]')).toBeVisible();

    // Project segment
    await expect(links.nth(1)).toContainText('pm-mcp-clients');
    await expect(links.nth(1).locator('.kind-mark[data-kind="project"]')).toBeVisible();

    // Epic segment
    await expect(links.nth(2)).toContainText('Relay Hardening');
    await expect(links.nth(2).locator('.kind-mark[data-kind="epic"]')).toBeVisible();

    // Current plan segment
    const currentCrumb = breadcrumb.locator('.breadcrumb__current');
    await expect(currentCrumb).toContainText('Fix Relay Transport');
    await expect(currentCrumb.locator('.kind-mark[data-kind="plan"]')).toBeVisible();
    await expect(currentCrumb).toHaveAttribute('aria-current', 'page');

    // Click epic segment navigates to epic
    await links.nth(2).click();
    await expect(page).toHaveURL(/\/epics\/relay-hardening/);
    await expect(page.getByTestId('epic-view')).toBeVisible();
  });

  test('lens segment in breadcrumb opens unfilled list and reload keeps it unfilled', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 1440, height: 900 });

    await page.goto('/plans/fix-relay');
    await expect(page.getByTestId('plan-view')).toBeVisible();

    // Click lens crumb (Running work)
    const breadcrumb = page.getByTestId('breadcrumb');
    const lensCrumb = breadcrumb.locator('.breadcrumb__link').nth(0);
    await expect(lensCrumb).toContainText('Running work');
    await lensCrumb.click();

    // Pushes bare unfilled /overview
    await expect(page).toHaveURL(/\/overview$/);
    await expect(page.locator('.overview')).toBeVisible();

    // Reload page: state carries app mark, so it stays unfilled on /overview
    await page.reload();
    await expect(page.locator('.app-header__machine')).toBeVisible();
    await expect(page).toHaveURL(/\/overview$/);
    await expect(page.locator('.overview')).toBeVisible();
  });

  test('hides work list column on phone, shows Up there, and displays bottom bar', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 390, height: 844 });

    await page.goto('/plans/fix-relay');
    await expect(page.getByTestId('plan-view')).toBeVisible();

    // Work list column is hidden on phone
    await expect(page.getByTestId('work-list-column')).toBeHidden();

    // Up is in the object column's head, leading to the plan's epic at every width
    await expect(page.getByTestId('up-link')).toBeVisible();
    await expect(page.getByTestId('up-link')).toHaveAttribute('href', '/epics/relay-hardening');

    // Bottom bar is visible on phone
    const bottomBar = page.getByTestId('bottom-bar');
    await expect(bottomBar).toBeVisible();

    // Bottom items: What waits (needs), Running work (running), Decisions, More
    await expect(bottomBar.getByTestId('bottom-needs')).toBeVisible();
    await expect(bottomBar.getByTestId('bottom-running')).toBeVisible();
    await expect(bottomBar.getByTestId('bottom-decisions')).toBeVisible();
    await expect(bottomBar.getByTestId('bottom-more')).toBeVisible();
  });
});
