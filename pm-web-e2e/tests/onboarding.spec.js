import { test, expect } from '@playwright/test';

test.describe('Onboarding Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Clear localStorage and pair with code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    // Navigate to Settings then click Configure on Harnesses aspect
    const settingsBtn = page.getByRole('button', { name: /Settings/i });
    await settingsBtn.click();
    await page.locator('[data-aspect="harnesses"]').getByRole('button', { name: 'Configure' }).click();
  });

  test('displays onboarding step bar and steps overview', async ({ page }) => {
    await expect(page.locator('.onboarding')).toBeVisible();
    await expect(page.locator('.step-bar')).toBeVisible();

    // Verify entry choice buttons are visible
    const guidedBtn = page.getByRole('button', { name: /Begin with step 1/i });
    const autoBtn = page.getByRole('button', { name: /Decide everything for me/i });

    await expect(guidedBtn).toBeVisible();
    await expect(autoBtn).toBeVisible();
  });

  test('can progress through guided onboarding step', async ({ page }) => {
    const guidedBtn = page.getByRole('button', { name: /Begin with step 1/i });
    await guidedBtn.click();

    // Should display step bar and state card
    await expect(page.locator('.step-bar')).toBeVisible();
    await expect(page.locator('.state-card')).toBeVisible();
  });
});
