import { test, expect } from '@playwright/test';

test.describe('Machine Onboarding & Settings Flow (UC-ONB-1..10)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch the mock runtime to the onboarding scenario
    const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
      data: { scenario: 'onboarding' },
    });
    expect(res.ok()).toBeTruthy();

    // Clear localStorage and pair with the scenario's code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test('opens the onboarding at the mode choice', async ({ page }) => {
    await expect(page.locator('.onboarding')).toBeVisible();
    await expect(page.locator('.step-bar')).toBeVisible();

    await expect(page.getByRole('button', { name: /Begin with step 1/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Decide everything for me/i })).toBeVisible();
  });

  test('walks from the mode choice into the language step', async ({ page }) => {
    await page.getByRole('button', { name: /Begin with step 1/i }).click();

    await expect(page.getByRole('heading', { name: 'Choose your languages' })).toBeVisible();
  });

  test('asks the harness-scan consent and holds on not now', async ({ page }) => {
    await page.getByRole('button', { name: /Begin with step 1/i }).click();
    await page.getByRole('button', { name: 'set' }).click();

    await expect(
      page.getByRole('heading', { name: 'Look for harnesses on this machine?' }),
    ).toBeVisible();
    await expect(page.getByText('claude, opencode, codex, agy')).toBeVisible();

    // A declined required consent stays in the same state
    await page.getByRole('button', { name: /^Not now$/i }).click();
    await expect(
      page.getByRole('heading', { name: 'Look for harnesses on this machine?' }),
    ).toBeVisible();

    // Accepting it moves on to the harness choice
    await page.getByRole('button', { name: /^Yes$/i }).click();
    await expect(
      page.getByRole('heading', { name: 'Choose the harnesses to enrol' }),
    ).toBeVisible();
  });

  test('opens the automatic run at its one consent', async ({ page }) => {
    await page.getByRole('button', { name: /Decide everything for me/i }).click();

    await expect(page.getByRole('heading', { name: 'Decide everything for me?' })).toBeVisible();
    await expect(page.getByText('This consent is for this run only.')).toBeVisible();
  });

  test('shows the machine settings and withdraws a remembered consent', async ({ page }) => {
    // Open the settings from the header, then the machine settings page
    await page.getByRole('button', { name: /Machine settings/ }).first().click();
    await expect(page.locator('.settings-view')).toBeVisible();
    await page.getByTestId('machine-settings-button').click();

    await expect(page.getByTestId('machine-settings-view')).toBeVisible();
    await expect(page.getByTestId('aspect-list')).toBeVisible();

    // The seven aspects the onboarding recorded
    await expect(page.locator('[data-aspect="language"]')).toBeVisible();
    await expect(page.locator('[data-aspect="defaults"]')).toBeVisible();

    // Withdraw a remembered consent
    await page.getByTestId('withdraw-scan_harnesses').click();
    await expect(page.getByText('Withdrawn: you will be asked again next time.')).toBeVisible();
  });
});
