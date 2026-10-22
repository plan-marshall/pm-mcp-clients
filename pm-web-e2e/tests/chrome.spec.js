import { test, expect } from '@playwright/test';
import { pair, switchScenario } from './helpers/pairing.js';

/**
 * The web chrome, navigation and action advisories: the header is the only always-on status row,
 * the brand opens the start page, every action explains itself on hover, and the designer is
 * reachable from the header and from a plan.
 */
test.describe('Web chrome, navigation and action advisories', () => {
  test.beforeEach(async ({ page, request }) => {
    await switchScenario(request, 'plan-view');
    await pair(page, request);
  });

  test.afterAll(async ({ request }) => {
    try {
      await request.post('http://127.0.0.1:7420/mock/scenario', {
        data: { scenario: 'web-pairing' },
      });
    } catch {
      // Best-effort cleanup.
    }
  });

  test('keeps the connection status in the header and drops the full-width banner', async ({
    page,
  }) => {
    await expect(page.getByTestId('connection-status-banner')).toHaveCount(0);

    const badge = page.getByTestId('header-machine');
    await expect(badge).toBeVisible();
    await expect(badge).toHaveAttribute('data-connection-mode', /loopback|lan/);
    await expect(badge).toHaveAttribute('title', /paired device/);
  });

  test('gives header and list actions a non-empty hover advisory', async ({ page }) => {
    const headerTitles = await page
      .locator('.app-header__actions button')
      .evaluateAll((buttons) => buttons.map((button) => button.getAttribute('title') ?? ''));
    expect(headerTitles.length).toBeGreaterThan(0);
    expect(headerTitles.every((title) => title.trim().length > 0)).toBe(true);

    await page.getByTestId('nav-projects-btn').click();
    await expect(page.getByTestId('project-filter-input')).toBeVisible();
    await expect(page.locator('.project-card').first()).toBeVisible();

    const listTitles = await page
      .locator('.project-card button')
      .evaluateAll((buttons) => buttons.map((button) => button.getAttribute('title') ?? ''));
    expect(listTitles.length).toBeGreaterThan(0);
    expect(listTitles.every((title) => title.trim().length > 0)).toBe(true);
  });

  test('opens the start page from the brand', async ({ page }) => {
    await page.goto('/reports');
    await expect(page.getByTestId('brand-home')).toBeVisible();

    await page.getByTestId('brand-home').click();

    await expect(page).toHaveURL(/\/(overview|projects)$/);
  });

  test('opens the designer from the header and from a plan, keeping the project across a reload', async ({
    page,
  }) => {
    await page.getByTestId('nav-designer-btn').click();
    await expect(page.getByTestId('process-designer')).toBeVisible();
    await expect(page).toHaveURL(/\/designer$/);

    await page.goto('/overview');
    await page.locator('[data-testid="plan-card"]').first().click();
    await expect(page.getByTestId('plan-view')).toBeVisible();

    await page.getByTestId('design-process-btn').click();
    await expect(page).toHaveURL(/\/designer\?project=pm-mcp-clients$/);

    await page.reload();
    await expect(page.getByTestId('process-designer')).toBeVisible();
    await expect(page.getByTestId('designer-checked')).toContainText('pm-mcp-clients');
  });
});
