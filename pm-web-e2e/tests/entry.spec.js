import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

test.describe('Web Client Entry & Connection (UC-ENT-1)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to the entry scenario (machine not onboarded yet, network listener open)
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'entry' },
    });
    expect(res.ok()).toBeTruthy();

    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test('shows the connection status in the header badge after pairing', async ({ page }) => {
    await expect(page.getByTestId('connection-status-banner')).toHaveCount(0);

    const badge = page.getByTestId('header-machine');
    await expect(badge).toBeVisible();
    await expect(badge).toHaveAttribute('data-connection-mode', 'loopback');
    await expect(badge).toHaveAttribute(
      'title',
      /This browser is on the machine or reaches it through an SSH forward/,
    );
    await expect(badge).toHaveAttribute('title', /paired device/);
  });

  test('opens the network access modal from settings and shows the fingerprint', async ({
    page,
    request,
  }) => {
    // The machine settings are reached once the machine is onboarded.
    await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'entry-device-lifecycle' },
    });
    await page.goto('/settings');
    await expect(page.locator('.settings-view')).toBeVisible();

    await page.getByTestId('lan-details-button').click();

    const modal = page.getByTestId('lan-listener-modal');
    await expect(modal).toBeVisible();
    await expect(page.getByTestId('lan-listener-state')).toContainText('open');
    await expect(page.getByTestId('lan-fingerprint')).toContainText('4F:1A:9C');
    await expect(page.getByTestId('lan-turn-off-button')).toBeVisible();
  });

  test('offers the credential entry on the loopback connection', async ({ page, request }) => {
    await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'entry-device-lifecycle' },
    });
    await page.goto('/settings');
    await expect(page.getByTestId('credentials-section')).toBeVisible();
    await expect(page.getByTestId('credential-entry')).toBeVisible();
    await expect(page.getByTestId('credential-form')).toBeVisible();
  });

  test('shows the pairing page for an unpaired browser', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();

    await expect(page.locator('h1')).toHaveText('Pair this browser');
    await expect(page.locator('#pairing-code')).toBeVisible();
  });
});
