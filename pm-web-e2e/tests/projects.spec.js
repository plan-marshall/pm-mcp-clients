import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

test.describe('Projects Explorer Flow (UC-PRJ-1..3)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to projects scenario
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'projects' },
    });
    expect(res.ok()).toBeTruthy();

    // Clear localStorage and pair with code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    // Navigate to Projects view
    const projectsNavBtn = page.getByTestId('nav-projects-btn');
    await expect(projectsNavBtn).toBeVisible();
    await projectsNavBtn.click();
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();
  });

  test('displays full 4-part explorer with being added, favourites, recent, and all projects', async ({
    page,
  }) => {
    await expect(page.getByTestId('section-being-added')).toBeVisible();
    await expect(page.getByText('~/git/cui-parent')).toBeVisible();

    const favourites = page.getByTestId('section-favourites');
    await expect(favourites).toBeVisible();
    await expect(favourites.getByTestId('project-card-pm-mcp-clients')).toBeVisible();
    await expect(favourites.getByTestId('star-btn-pm-mcp-clients')).toHaveText('★');

    const recent = page.getByTestId('section-recent');
    await expect(recent).toBeVisible();
    await expect(recent.getByTestId('project-card-plan-marshall')).toBeVisible();

    const allProjects = page.getByTestId('section-all-projects');
    await expect(allProjects).toBeVisible();
    await expect(allProjects.getByTestId('project-card-API-Sheriff')).toBeVisible();
  });

  test('filters projects by query input', async ({ page }) => {
    const filterInput = page.getByTestId('project-filter-input');
    await expect(filterInput).toBeVisible();
    await filterInput.fill('http');

    await expect(page.getByTestId('section-filtered-matches')).toBeVisible();
    await expect(page.getByTestId('project-card-cui-http')).toBeVisible();
    await expect(page.getByTestId('project-card-API-Sheriff')).not.toBeVisible();
  });

  test('toggles favourite status when star button is clicked', async ({ page }) => {
    const starBtn = page.getByTestId('section-all-projects').getByTestId('star-btn-pm-mcp-clients');
    await expect(starBtn).toBeVisible();
    await starBtn.click();

    // After toggle, favourite changes and view refreshes
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();
  });

  test('reorders favourites with the move controls and keeps the operator order', async ({
    page,
  }) => {
    const favourites = page.getByTestId('section-favourites');
    const cards = favourites.locator('[data-testid^="project-card-"]');
    await expect(cards.first()).toHaveAttribute('data-testid', 'project-card-pm-mcp-clients');

    await page.getByTestId('favourite-down-pm-mcp-clients').click();

    await expect(cards.first()).toHaveAttribute('data-testid', 'project-card-pm-mcp-core');
    await expect(page.getByTestId('reorder-error')).not.toBeVisible();
  });

  test('opens the project onboarding route from Add a project and from Continue', async ({
    page,
  }) => {
    await page.getByTestId('add-project-btn').click();
    await expect(page).toHaveURL(/\/projects\/onboard$/);
    await expect(page.getByTestId('project-onboarding-view')).toBeVisible();
    await expect(page.getByTestId('project-checkout-step')).toBeVisible();

    await page.goto('/projects');
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();
    await page.getByTestId('continue-onboarding-btn').click();

    await expect(page).toHaveURL(/\/projects\/onboard$/);
    await expect(page.getByTestId('project-onboarding-step')).toBeVisible();
    await expect(page.getByTestId('project-step-2')).toHaveAttribute('aria-current', 'step');
    await expect(page.getByTestId('project-onboarding-step')).toContainText('~/git/cui-parent');
  });

  test('releases a project with modal confirmation', async ({ page }) => {
    const releaseBtn = page
      .getByTestId('section-all-projects')
      .getByTestId('release-btn-pm-mcp-clients');
    await expect(releaseBtn).toBeVisible();
    await releaseBtn.click();

    const modal = page.getByTestId('release-modal');
    await expect(modal).toBeVisible();

    const reasonInput = page.getByTestId('release-reason-input');
    await expect(reasonInput).toBeVisible();
    await reasonInput.fill('Project completed and archived');

    const confirmBtn = page.getByTestId('confirm-release-btn');
    await confirmBtn.click();

    await expect(modal).not.toBeVisible();
  });
});
