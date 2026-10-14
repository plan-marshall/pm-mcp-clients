import { test, expect } from '@playwright/test';

test.describe('Workspace & System Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Clear localStorage and pair with code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test('navigates to workspace projects and displays project table', async ({ page }) => {
    const explorerBtn = page.getByRole('button', { name: 'Projects' });
    await expect(explorerBtn).toBeVisible();
    await explorerBtn.click();

    // Verify workspace view is active and projects table is shown
    await expect(page.locator('.workspace')).toBeVisible();
    await expect(page.locator('.data-table')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Process Designer' })).toBeVisible();
  });

  test('switches between projects, plans, and system tabs in workspace', async ({ page }) => {
    const explorerBtn = page.getByRole('button', { name: 'Projects' });
    await explorerBtn.click();

    // Switch to Plans tab
    const plansTab = page.getByRole('button', { name: 'Plans' });
    await plansTab.click();
    await expect(page.locator('.data-table')).toContainText('PLN-001');

    // Switch to System tab
    const systemTab = page.getByRole('button', { name: 'System' });
    await systemTab.click();
    await expect(page.locator('.system-stats')).toBeVisible();
    await expect(page.locator('.system-stats')).toContainText('mock-machine');
  });

  test('navigates to settings and displays paired devices and aspect re-runs', async ({ page }) => {
    const settingsBtn = page.getByRole('button', { name: /Settings/i });
    await expect(settingsBtn).toBeVisible();
    await settingsBtn.click();

    await expect(page.locator('.settings-view')).toBeVisible();
    await expect(page.getByText('Paired Devices')).toBeVisible();
    await expect(page.getByText('Configuration Aspects')).toBeVisible();
  });

  test('toggles light/dark theme', async ({ page }) => {
    const themeBtn = page.locator('button[aria-label="Toggle light/dark theme"]');
    await expect(themeBtn).toBeVisible();

    const initialTheme = await page.locator('html').getAttribute('data-theme');
    await themeBtn.click();

    const newTheme = await page.locator('html').getAttribute('data-theme');
    expect(newTheme).not.toBe(initialTheme);
  });
});
