import { test, expect } from '@playwright/test';
import { pair, switchScenario } from './helpers/pairing.js';

/**
 * The Projects lens of the web app (PLAN-59 D1, D4): the list of project rows and the one project
 * page beside it, the visible _Add a plan_ on a ready row, and the two destructive acts that stay
 * in the row menu behind their confirmation.
 *
 * A project is listed in more than one group (a favourite is also in _All projects_), so every
 * locator is scoped to the section it reads.
 */

test.describe('Projects lens (UC-PRJ-1..3)', () => {
  test.beforeEach(async ({ page, request }) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await switchScenario(request, 'projects-and-start');
    await pair(page, request);

    await page.goto('/projects');
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();
    await expect(page.getByTestId('section-favourites')).toBeVisible();
  });

  test('lists the projects as rows at 1440 px with their state words and what they need', async ({
    page,
  }) => {
    await expect(page.getByTestId('section-recent')).toBeVisible();
    await expect(page.getByTestId('section-all-projects')).toBeVisible();
    await expect(page.getByTestId('being-added-row')).toContainText('~/git/cui-parent');

    const favourites = page.getByTestId('section-favourites');
    const favourite = favourites.getByTestId('project-row-pm-mcp-clients');
    await expect(favourite).toBeVisible();
    await expect(favourite).toContainText('plan-marshall/pm-mcp-clients');
    await expect(favourite.getByText('ready')).toBeVisible();
    // The work counts and the age of the last activity are the row's own line.
    await expect(favourite).toContainText('3 plans');
    await expect(favourite).toContainText('1 epic');
    await expect(favourites.getByTestId('star-btn-pm-mcp-clients')).toBeVisible();

    // A project whose setup is open reads its state word, never the wire's own value.
    const recent = page.getByTestId('section-recent');
    const notReady = recent.getByTestId('project-row-plan-marshall');
    await expect(notReady.getByText('setup open')).toBeVisible();
    await expect(notReady.getByText('! setup')).toBeVisible();

    // The list is a column of rows, not a card grid: the rows stack one under the other.
    const first = await favourite.boundingBox();
    const second = await favourites.getByTestId('project-row-pm-mcp-core').boundingBox();
    expect(first).not.toBeNull();
    expect(second).not.toBeNull();
    expect(second.y).toBeGreaterThan(first.y);
  });

  test('opens the one project page from a row', async ({ page }) => {
    await page
      .getByTestId('section-favourites')
      .getByTestId('project-row-pm-mcp-clients')
      .getByRole('link')
      .first()
      .click();

    await expect(page).toHaveURL(/\/projects\/pm-mcp-clients$/);
    const projectPage = page.getByTestId('project-page');
    await expect(projectPage).toBeVisible();
    await expect(projectPage.getByRole('heading', { level: 1 })).toContainText(
      'plan-marshall/pm-mcp-clients',
    );
    await expect(page.getByTestId('project-page-work')).toBeVisible();
  });

  test('keeps release and unenrol in the row menu, never on the row', async ({ page }) => {
    const row = page.getByTestId('section-favourites').getByTestId('project-row-pm-mcp-clients');
    await expect(row.getByRole('button', { name: 'Release…' })).toHaveCount(0);
    await expect(row.getByRole('button', { name: 'Unenrol…' })).toHaveCount(0);

    await row.getByRole('button', { name: 'Actions for plan-marshall/pm-mcp-clients' }).click();

    await expect(page.getByTestId('action-release')).toBeVisible();
    await expect(page.getByTestId('action-unenrol')).toBeVisible();
    await expect(page.getByTestId('action-add_epic')).toBeVisible();
  });

  test('releases a project through the menu and its confirmation', async ({ page }) => {
    const row = page.getByTestId('section-favourites').getByTestId('project-row-pm-mcp-clients');
    await row.getByRole('button', { name: 'Actions for plan-marshall/pm-mcp-clients' }).click();
    await page.getByTestId('action-release').click();

    const dialog = page.getByTestId('release-modal');
    await expect(dialog).toBeVisible();
    await page.getByTestId('release-reason-input').fill('The project moved to another machine');
    await dialog.getByTestId('confirm-dialog-confirm').click();

    await expect(dialog).toHaveCount(0);
    // A released project stays listed, reading its state word, and can be enrolled again.
    const released = page
      .getByTestId('section-favourites')
      .getByTestId('project-row-pm-mcp-clients');
    await expect(released).toContainText('released');
  });

  test('starts a plan from a ready row without selecting the project first', async ({ page }) => {
    await page
      .getByTestId('section-favourites')
      .getByTestId('add-plan-pm-mcp-clients')
      .click();

    await expect(page.getByTestId('start-view')).toBeVisible();
    await expect(page).not.toHaveURL(/\/projects\/?$/);
  });

  test('shows the three identity fields of a project being added', async ({ page }) => {
    const being = page.getByTestId('being-added-row');
    await being.getByTestId('continue-onboarding-btn').click();

    await expect(page).toHaveURL(/\/projects\/onboarding\/ent-cui$/);
    const identity = page.getByTestId('project-identity-step');
    await expect(identity).toBeVisible();
    await expect(page.getByTestId('project-identity-id')).toHaveValue('cui-parent');
    await expect(page.getByTestId('project-identity-name')).toHaveValue('cui-parent');
    await expect(page.getByTestId('project-identity-pilot')).toBeVisible();

    await page.reload();
    await expect(page).toHaveURL(/\/projects\/onboarding\/ent-cui$/);
    await expect(identity).toBeVisible();
  });

  test('shows Add a plan disabled with its reason on a project that is not ready', async ({
    page,
  }) => {
    const addPlan = page.getByTestId('section-recent').getByTestId('add-plan-plan-marshall');
    await expect(addPlan).toHaveAttribute('aria-disabled', 'true');
    await expect(addPlan).toHaveAttribute('title', /plan-marshall is not ready: setup/);
  });
});
