import { test, expect } from '@playwright/test';

test.describe('Web Client Pairing Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Clear localStorage to ensure clean state
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
  });

  test('displays pairing form when not paired', async ({ page }) => {
    await page.goto('/');

    await expect(page.locator('h1')).toHaveText('Pair this browser');
    await expect(page.locator('#pairing-code')).toBeVisible();

    const pairBtn = page.getByRole('button', { name: 'Pair this browser' });
    await expect(pairBtn).toBeDisabled();
  });

  test('enables pair button and pairs successfully with code', async ({ page }) => {
    await page.goto('/');

    const input = page.locator('#pairing-code');
    await input.fill('KXQB-MRTN');

    const pairBtn = page.getByRole('button', { name: 'Pair this browser' });
    await expect(pairBtn).not.toBeDisabled();

    await pairBtn.click();

    // After pairing against web-pairing scenario, should transition to onboard.mode or workspace
    await expect(page.locator('.app-header__title')).toHaveText('plan-marshall');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test('automatically pairs when URL contains fragment #code=...', async ({ page }) => {
    await page.goto('/#code=KXQB-MRTN');

    // Should automatically pair and strip fragment
    await expect(page).toHaveURL('/');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });
});
