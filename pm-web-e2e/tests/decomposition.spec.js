import { test, expect } from '@playwright/test';
import { pair, switchScenario } from './helpers/pairing.js';

async function pairOnDecompositionScenario(page, request) {
  await switchScenario(request, 'decomposition');
  await pair(page, request);

  await page.goto('/epics/relay-hardening/decomposition?project=pm-mcp-clients');
  await expect(page.getByTestId('decomposition-title')).toContainText(
    'relay-hardening · decomposition',
    { timeout: 15000 },
  );
}

test.describe('Epic Decomposition (UC-STA-6)', () => {
  test.beforeEach(async ({ page, request }) => {
    await pairOnDecompositionScenario(page, request);
  });

  test('shows the workstreams, the staged plans, and the failing check', async ({ page }) => {
    await expect(page.getByTestId('workstream-WS-1')).toContainText('Reconnect');
    await expect(page.getByTestId('workstream-WS-2')).toContainText('Authentication');
    await expect(page.getByTestId('plan-relay-auth-v2')).toContainText('relay-auth-v2');
    await expect(page.getByTestId('plan-relay-auth-v2')).toContainText('check failed');
    await expect(page.getByTestId('plan-relay-reconnect-backoff')).toContainText('checked');
    await expect(page.getByTestId('plan-surface-relay-auth-v2')).toContainText('pm-api/src/cursor');
    await expect(page.getByTestId('conflict-banner')).toBeVisible();
    await expect(page.getByTestId('agree-commit')).toHaveAttribute('aria-disabled', 'true');
    await expect(page.getByTestId('agree-commit')).toHaveAttribute('aria-describedby');
  });

  test('edits a plan, reads the change back, and agrees the cut', async ({ page }) => {
    await page.getByTestId('plan-edit-relay-auth-v2').click();

    const modal = page.getByTestId('instruct-modal');
    await expect(modal).toBeVisible();

    // Escape closes Dialog
    await page.keyboard.press('Escape');
    await expect(modal).not.toBeVisible();

    // Reopen and submit via Compose
    await page.getByTestId('plan-edit-relay-auth-v2').click();
    await expect(modal).toBeVisible();
    const input = modal.getByRole('textbox');
    await input.fill('the exchange belongs into pm-relay');
    await input.press('Enter');

    const readBack = page.getByTestId('read-back');
    await expect(readBack).toBeVisible();
    await expect(readBack).toContainText('Surface');
    await expect(readBack).toContainText('pm-relay/src/relay-auth-v2');

    await page.getByTestId('read-back-accept').click();
    await expect(page.getByTestId('plan-surface-relay-auth-v2')).toContainText(
      'pm-relay/src/relay-auth-v2',
    );

    await expect(page.getByTestId('agree-commit')).toBeEnabled();
    await page.getByTestId('agree-commit').click();
    await expect(page.getByTestId('decomposition-committed')).toContainText(
      'relay-hardening: 2 workstreams, 4 plans committed',
    );
  });

  test('adds a plan through the instruction modal', async ({ page }) => {
    await page.getByTestId('add-plan').click();
    const modal = page.getByTestId('instruct-modal');
    await expect(modal).toBeVisible();
    const input = modal.getByRole('textbox');
    await input.fill('add a docs plan for pm-relay');
    await input.press('Enter');
    await expect(page.getByTestId('read-back')).toBeVisible();
  });
});
