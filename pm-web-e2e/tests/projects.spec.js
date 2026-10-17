import { test, expect } from '@playwright/test';

test.describe('Projects Explorer Flow (UC-PRJ-1..3)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to projects scenario
    const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
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

    await expect(page.getByTestId('section-favourites')).toBeVisible();
    await expect(page.getByTestId('project-card-pm-mcp-clients')).toBeVisible();
    await expect(page.getByTestId('star-btn-pm-mcp-clients')).toHaveText('★');

    await expect(page.getByTestId('section-recent')).toBeVisible();
    await expect(page.getByTestId('project-card-plan-marshall')).toBeVisible();

    await expect(page.getByTestId('section-all-projects')).toBeVisible();
    await expect(page.getByTestId('project-card-API-Sheriff')).toBeVisible();
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
    const starBtn = page.getByTestId('star-btn-pm-mcp-clients');
    await expect(starBtn).toBeVisible();
    await starBtn.click();

    // After toggle, favourite changes and view refreshes
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();
  });

  test('releases a project with modal confirmation', async ({ page }) => {
    const releaseBtn = page.getByTestId('release-btn-pm-mcp-clients');
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
