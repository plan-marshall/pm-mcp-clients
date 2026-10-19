import { test, expect } from '@playwright/test';

test.describe('Web Client Entry & Connection (UC-ENT-1)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to the entry scenario (machine not onboarded yet, network listener open)
    const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
      data: { scenario: 'entry' },
    });
    expect(res.ok()).toBeTruthy();

    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test('shows the connection status banner in loopback mode after pairing', async ({ page }) => {
    const banner = page.getByTestId('connection-status-banner');
    await expect(banner).toBeVisible();
    await expect(banner).toHaveAttribute('data-mode', 'loopback');
    await expect(banner).toContainText('Connected over loopback');
    await expect(banner).toContainText('paired device');
  });

  test('opens the network access modal from settings and shows the fingerprint', async ({
    page,
    request,
  }) => {
    // The machine settings are reached once the machine is onboarded.
    await request.post('http://127.0.0.1:7420/mock/scenario', {
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
    await request.post('http://127.0.0.1:7420/mock/scenario', {
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
