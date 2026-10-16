import { test, expect } from '@playwright/test';

test.describe('Paired Devices Flow (UC-ENT-2)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to paired-devices scenario
    const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
      data: { scenario: 'paired-devices' },
    });
    expect(res.ok()).toBeTruthy();

    // Clear localStorage and pair with code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    // Open Settings view
    const settingsBtn = page.getByRole('button', { name: /Settings/i });
    await expect(settingsBtn).toBeVisible();
    await settingsBtn.click();
    await expect(page.locator('.settings-view')).toBeVisible();
  });

  test('displays paired devices table with columns and network listener status', async ({ page }) => {
    await expect(page.getByTestId('paired-devices-section')).toBeVisible();
    await expect(page.locator('.devices-table')).toBeVisible();

    // Verify seeded devices
    await expect(page.getByText('Firefox on mbp-oliver')).toBeVisible();
    await expect(page.getByText('Safari on iPad')).toBeVisible();
    await expect(page.getByText('Chrome on build-box')).toBeVisible();

    // Verify network listener status
    await expect(page.getByTestId('listener-status')).toContainText('Network listener: on.');
  });

  test('renames a device and updates the table', async ({ page }) => {
    const renameBtn = page.getByTestId('rename-btn-dev-2');
    await expect(renameBtn).toBeVisible();
    await renameBtn.click();

    const input = page.getByTestId('rename-input-dev-2');
    await expect(input).toBeVisible();
    await input.fill('iPad Oliver');

    const saveBtn = page.getByTestId('save-rename-dev-2');
    await saveBtn.click();

    await expect(page.getByText('iPad Oliver')).toBeVisible();
  });

  test('turns network listener off and shows explanatory notice', async ({ page }) => {
    const listenerOffBtn = page.getByTestId('listener-off-button');
    await expect(listenerOffBtn).toBeVisible();
    await listenerOffBtn.click();

    await expect(page.getByTestId('listener-status')).toContainText('Network listener: off.');
    await expect(page.getByTestId('listener-notice')).toBeVisible();
    await expect(page.getByTestId('listener-off-button')).not.toBeVisible();
  });

  test('revokes another device via confirmation modal', async ({ page }) => {
    const revokeBtn = page.getByTestId('revoke-btn-dev-3');
    await expect(revokeBtn).toBeVisible();
    await revokeBtn.click();

    // Modal opens
    const modal = page.getByTestId('revoke-modal');
    await expect(modal).toBeVisible();
    await expect(modal).toContainText('Revoke Chrome on build-box?');

    // Confirm
    const confirmBtn = page.getByTestId('revoke-confirm-button');
    await confirmBtn.click();

    // Verify device removed
    await expect(page.getByText('Chrome on build-box')).not.toBeVisible();
  });

  test('revoking this browser redirects to revoked page or pairing', async ({ page }) => {
    const revokeBtn = page.getByTestId('revoke-btn-dev-1');
    await expect(revokeBtn).toBeVisible();
    await revokeBtn.click();

    const modal = page.getByTestId('revoke-modal');
    await expect(modal).toBeVisible();

    const confirmBtn = page.getByTestId('revoke-confirm-button');
    await confirmBtn.click();

    // Should either land on revoked page or pairing page
    await expect(page.getByRole('button', { name: 'Pair this browser' })).toBeVisible();
  });
});
