import { test, expect } from '@playwright/test';

/**
 * The onboarding route (UC-ONB-1..10) at desktop and phone width, in light and dark.
 *
 * One route renders the state the runtime reports; the done steps are read-only summaries.
 */

async function openOnboarding(page, request, scenario = 'onboarding') {
  const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
    data: { scenario },
  });
  expect(res.ok()).toBeTruthy();

  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/pair#code=KXQB-MRTN');
  await expect(page.locator('.app-header__machine')).toContainText('Machine:');
}

test.describe('Onboarding route (UC-ONB-1..10)', () => {
  test.beforeEach(async ({ page, request }) => {
    await openOnboarding(page, request);
    // A machine that is not onboarded opens the onboarding on its own route.
    await expect(page).toHaveURL(/\/onboarding$/);
  });

  test('opens the onboarding at the mode choice on its own route', async ({ page }) => {
    await expect(page.getByTestId('onboarding-view')).toBeVisible();
    await expect(page.locator('.step-bar')).toBeVisible();
    await expect(page.getByRole('button', { name: /Begin with step 1/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Decide everything for me/i })).toBeVisible();
  });

  test('walks from the mode choice into the language step', async ({ page }) => {
    await page.getByRole('button', { name: /Begin with step 1/i }).click();

    await expect(page.getByRole('heading', { name: 'Choose your languages' })).toBeVisible();
    await expect(page.getByTestId('language-screen')).toBeVisible();
  });

  test('asks the harness-scan consent and holds on not now', async ({ page }) => {
    await page.getByRole('button', { name: /Begin with step 1/i }).click();
    await page.getByTestId('language-record').click();

    await expect(
      page.getByRole('heading', { name: 'Look for harnesses on this machine?' }),
    ).toBeVisible();
    await expect(page.getByText('claude, opencode, codex, agy')).toBeVisible();

    // A declined required consent stays in the same state.
    await page.getByRole('button', { name: /^Not now$/i }).click();
    await expect(
      page.getByRole('heading', { name: 'Look for harnesses on this machine?' }),
    ).toBeVisible();

    // Accepting it moves on to the harness choice.
    await page.getByRole('button', { name: /^Look for harnesses$/i }).click();
    await expect(
      page.getByRole('heading', { name: 'Choose the harnesses to enrol' }),
    ).toBeVisible();
  });

  test('lists the found harnesses with their tokens and the default', async ({ page }) => {
    await page.getByRole('button', { name: /Begin with step 1/i }).click();
    await page.getByTestId('language-record').click();
    await page.getByRole('button', { name: /^Look for harnesses$/i }).click();

    await expect(page.getByTestId('harness-toggle-claude')).toBeChecked();
    await expect(page.getByTestId('harness-toggle-codex')).toBeDisabled();
    await expect(page.getByText('cannot run workers')).toBeVisible();
    await expect(page.getByTestId('harness-check')).toContainText(
      'Check 2 harnesses, Claude Code the default',
    );
  });

  test('opens the automatic run at its one consent', async ({ page }) => {
    await page.getByRole('button', { name: /Decide everything for me/i }).click();

    await expect(page.getByRole('heading', { name: 'Decide everything for me?' })).toBeVisible();
    await expect(page.getByText('This consent is for this run only.')).toBeVisible();
  });

  test('renders at phone width without horizontal overflow', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });

    await expect(page.getByTestId('onboarding-view')).toBeVisible();
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(1);
  });

  test('renders in the dark theme from the custom properties', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.reload();
    await expect(page.getByTestId('onboarding-view')).toBeVisible();

    const background = await page.evaluate(() =>
      getComputedStyle(document.documentElement).getPropertyValue('--color-bg-app').trim(),
    );
    expect(background).toBe('#0b0f19');
  });
});

test.describe('Machine settings route', () => {
  test('opens /settings/machine and shows every aspect', async ({ page, request }) => {
    // The settings are reachable once the machine is onboarded.
    await openOnboarding(page, request, 'entry-device-lifecycle');

    await page.goto('/settings/machine');

    await expect(page).toHaveURL(/\/settings\/machine$/);
    await expect(page.getByTestId('machine-settings-view')).toBeVisible();
    await expect(page.getByTestId('aspect-list')).toBeVisible();
    await expect(page.locator('[data-aspect="language"]')).toBeVisible();
    await expect(page.locator('[data-aspect="defaults"]')).toBeVisible();

    await page.getByTestId('withdraw-scan_harnesses').click();
    await expect(page.getByText('Withdrawn: you will be asked again next time.')).toBeVisible();
  });

  test('opens the onboarding at the aspect whose Change was clicked', async ({ page, request }) => {
    await openOnboarding(page, request, 'entry-device-lifecycle');
    await page.goto('/settings/machine');
    await expect(page.getByTestId('machine-settings-view')).toBeVisible();

    await page.getByTestId('change-aspect-providers').click();

    await expect(page).toHaveURL(/\/onboarding\?step=6$/);
    await expect(page.getByTestId('onboarding-view')).toBeVisible();
  });

  test('returns from the settings to the route it was opened from', async ({ page, request }) => {
    await openOnboarding(page, request, 'entry-device-lifecycle');
    await page.goto('/projects');
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();

    await page.getByRole('button', { name: /Settings/i }).click();
    await expect(page.locator('.settings-view')).toBeVisible();

    await page.getByTestId('settings-back-button').click();
    await expect(page).toHaveURL(/\/projects$/);
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();
  });
});
