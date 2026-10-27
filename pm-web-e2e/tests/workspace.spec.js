import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

test.describe('Routing, History & Frame', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch the mock runtime to the projects scenario
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'projects' },
    });
    expect(res.ok()).toBeTruthy();

    // Clear localStorage and pair with the scenario's code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    // The explorer is the landing view when nothing needs the operator
    await page.getByTestId('nav-projects-btn').click();
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();
  });

  test('lands on the project explorer with its own path, without the retired workspace', async ({
    page,
  }) => {
    await expect(page).toHaveURL(/\/projects$/);
    // The temporary workspace stub is gone: no tab bar of projects, plans, and system
    await expect(page.locator('.workspace__tabs')).toHaveCount(0);
    await expect(page.locator('.data-table')).toHaveCount(0);
    await expect(page.getByTestId('add-project-btn')).toBeVisible();
  });

  test('writes every view to the address bar and moves with browser history', async ({ page }) => {
    const header = page.locator('.app-header');
    await header.getByRole('button', { name: 'Running work' }).click();
    await expect(page).toHaveURL(/\/overview$/);

    await header.getByRole('button', { name: 'Reports' }).click();
    await expect(page).toHaveURL(/\/reports$/);

    // The browser's back returns to the view before, not to `/`
    await page.goBack();
    await expect(page).toHaveURL(/\/overview$/);

    await page.goForward();
    await expect(page).toHaveURL(/\/reports$/);
  });

  test('opens a deep link into the view it names', async ({ page }) => {
    await page.goto('/system');
    await expect(page).toHaveURL(/\/system$/);
    await expect(page.getByTestId('system-view')).toBeVisible();

    await page.goto('/settings/machine');
    await expect(page).toHaveURL(/\/settings\/machine$/);
    await expect(page.getByTestId('machine-settings-view')).toBeVisible();
  });

  test('navigates to the settings and the machine settings by their own paths', async ({
    page,
  }) => {
    await page.getByRole('button', { name: 'Machine settings' }).click();
    await expect(page).toHaveURL(/\/settings$/);
    await expect(page.locator('.settings-view')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Paired devices' })).toBeVisible();

    await page.getByTestId('machine-settings-button').click();
    await expect(page).toHaveURL(/\/settings\/machine$/);
    await expect(page.getByTestId('machine-settings-view')).toBeVisible();
  });

  test('switches the theme between the three choices', async ({ page }) => {
    await page.getByTestId('theme-dark').click();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

    await page.getByTestId('theme-light').click();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
  });
});
