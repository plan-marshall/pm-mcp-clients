import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';
import { expectNothingRedAtRest, openOverflow } from './helpers/acts.js';

test.describe('Paired Devices Flow (UC-ENT-2)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to paired-devices scenario
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
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

  test('displays paired devices table with columns and network listener status', async ({
    page,
  }) => {
    await expect(page.getByTestId('paired-devices-section')).toBeVisible();
    await expect(page.locator('.devices-table')).toBeVisible();

    // Verify seeded devices
    await expect(page.getByText('Firefox on mbp-oliver')).toBeVisible();
    await expect(page.getByText('Safari on iPad')).toBeVisible();
    await expect(page.getByText('Chrome on build-box')).toBeVisible();

    // Verify network listener status
    await expect(page.getByTestId('listener-status')).toContainText('Network listener: on.');
  });

  test('draws rename beside a device and revoke in its overflow, nothing red at rest', async ({
    page,
  }) => {
    const row = page.getByTestId('device-row-dev-3');

    await expect(row.getByTestId('action-rename')).toBeVisible();
    await expect(row.getByTestId('action-revoke')).toHaveCount(0);
    await expectNothingRedAtRest(page);

    const menu = await openOverflow(row);
    await expect(menu.getByTestId('action-revoke')).toBeVisible();
  });

  test('renames a device and updates the table', async ({ page }) => {
    const row = page.getByTestId('device-row-dev-2');
    const renameBtn = row.getByTestId('action-rename');
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

  test('revokes another device via the confirmation dialog', async ({ page }) => {
    const row = page.getByTestId('device-row-dev-3');
    await openOverflow(row);
    await row.getByTestId('action-revoke').click();

    // The confirmation names the consequence in the danger colour
    const dialog = page.getByTestId('confirm-dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog).toContainText('Revoke Chrome on build-box?');
    await expect(dialog).toContainText('It is signed out at once');

    const confirmBtn = page.getByTestId('confirm-dialog-confirm');
    await confirmBtn.click();
    await expect(dialog).not.toBeVisible();

    // Verify device removed
    await expect(page.getByTestId('device-row-dev-3')).not.toBeVisible();
  });

  test('revoking this browser redirects to revoked page or pairing', async ({ page }) => {
    // The pairing names the device this browser is; its row is the one that ends the session.
    const deviceId = await page.evaluate(() => localStorage.getItem('pm_device_id'));
    const row = page.getByTestId(`device-row-${deviceId}`);
    await openOverflow(row);
    await row.getByTestId('action-revoke').click();

    const dialog = page.getByTestId('confirm-dialog');
    await expect(dialog).toBeVisible();

    const confirmBtn = page.getByTestId('confirm-dialog-confirm');
    await confirmBtn.click();

    // Should either land on revoked page or pairing page
    await expect(
      page.getByTestId('revoked-view').or(page.getByRole('button', { name: 'Pair this browser' })),
    ).toBeVisible();
  });

  test('asks for the typed name before revoking all others', async ({ page }) => {
    await page.getByTestId('revoke-others-button').click();

    const dialog = page.getByTestId('confirm-dialog');
    await expect(dialog).toBeVisible();
    const confirmBtn = page.getByTestId('confirm-dialog-confirm');
    await expect(confirmBtn).toBeDisabled();

    // The dialog names the exact label to type; typing it enables the act.
    const text = await dialog.innerText();
    const name = /Type (.+) to confirm/.exec(text)?.[1];
    expect(name).toBeTruthy();
    await page.getByTestId('confirm-dialog-input').fill(name);
    await expect(confirmBtn).toBeEnabled();
  });
});
