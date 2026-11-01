import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

test.describe('System View, Runtime Lifecycle & Cleanup (UC-SYS-1..3)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to system scenario
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'system' },
    });
    expect(res.ok()).toBeTruthy();

    // Clear localStorage and pair with code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    // Navigate to System view
    const systemNavBtn = page.getByRole('button', { name: /System/i });
    await expect(systemNavBtn).toBeVisible();
    await systemNavBtn.click();
    await expect(page.getByTestId('system-view')).toBeVisible();
  });

  test.afterAll(async ({ request }) => {
    try {
      await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
        data: { scenario: 'web-pairing' },
      });
    } catch {
      // Best-effort cleanup
    }
  });

  test('renders 6-part system dashboard with metrics and cards', async ({ page }) => {
    // The rail's "Work" group and the System view's "Work" card share a word: the dashboard
    // assertions read the view, never the frame.
    const view = page.getByTestId('system-view');

    // 1. Runtime Card
    await expect(view.getByRole('heading', { name: 'Runtime' })).toBeVisible();
    await expect(view.getByText(/pm-mcpd 0\.9\.1/i).first()).toBeVisible();
    await expect(view.getByText(/CPU/i)).toBeVisible();

    // 2. Work Card
    await expect(view.getByRole('heading', { name: 'Work', exact: true })).toBeVisible();
    await expect(view.getByText('Plans', { exact: true })).toBeVisible();

    // 3. Workers Card
    await expect(view.getByRole('heading', { name: 'Workers' })).toBeVisible();
    await expect(view.getByText(/busy/i).first()).toBeVisible();
    await expect(view.getByText(/idle/i).first()).toBeVisible();

    // 4. Jobs Card
    await expect(view.getByRole('heading', { name: 'Jobs' })).toBeVisible();
    await expect(view.getByText(/build/i).first()).toBeVisible();

    // 5. Models Card
    await expect(view.getByRole('heading', { name: 'Models' })).toBeVisible();

    // 6. Cleanup banner
    await expect(page.getByRole('region', { name: /Cleanup banner/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Review…/i })).toBeVisible();
  });

  test('toggles model time windows (Today, 7 days)', async ({ page }) => {
    const todayBtn = page.getByRole('button', { name: 'Today' });
    const weekBtn = page.getByRole('button', { name: '7 days' });

    await expect(todayBtn).toBeVisible();
    await expect(weekBtn).toBeVisible();

    // Switch to Today
    await todayBtn.click();
    await expect(todayBtn).toHaveClass(/active/);

    // Switch to 7 days
    await weekBtn.click();
    await expect(weekBtn).toHaveClass(/active/);
  });

  test('opens lifecycle confirmation modal and closes it', async ({ page }) => {
    const stopBtn = page.getByRole('button', { name: /Stop…/i });
    await expect(stopBtn).toBeVisible();
    await stopBtn.click();

    // Confirmation modal should be visible
    const modal = page.getByRole('dialog');
    await expect(modal).toBeVisible();
    await expect(modal.getByText(/Stop the runtime\?/i)).toBeVisible();
    await expect(modal.getByText(/When what runs is done/i)).toBeVisible();
    await expect(modal.getByText('Now', { exact: true })).toBeVisible();

    // Click "Keep it running" to close without action
    const keepBtn = modal.getByRole('button', { name: /Keep it running/i });
    await expect(keepBtn).toBeVisible();
    await keepBtn.click();

    await expect(modal).not.toBeVisible();
  });

  test('navigates to cleanup review, unchecks item, and opens deletion modal', async ({ page }) => {
    const reviewBtn = page.getByRole('button', { name: /Review…/i });
    await expect(reviewBtn).toBeVisible();
    await reviewBtn.click();

    // Cleanup view mounted
    await expect(page.getByTestId('cleanup-view')).toBeVisible();
    await expect(page.getByText('Summary')).toBeVisible();
    await expect(page.getByText('fix-relay')).toBeVisible();

    // Verify checkboxes
    const checkboxes = page.locator('input[type="checkbox"]');
    const count = await checkboxes.count();
    expect(count).toBeGreaterThan(0);

    // Uncheck first item
    await checkboxes.first().click();

    // Delete button should show updated count
    const deleteBtn = page.getByRole('button', { name: /Delete.*items/i });
    await expect(deleteBtn).toBeVisible();
    await deleteBtn.click();

    // Confirmation dialog
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText(/cannot be undone/i)).toBeVisible();

    // Cancel deletion
    const backBtn = dialog.getByRole('button', { name: 'Back' });
    await expect(backBtn).toBeVisible();
    await backBtn.click();

    await expect(dialog).not.toBeVisible();
  });
});

test.describe('System: figures, filters, upgrade, and cleanup preview (UC-SYS-1..3)', () => {
  test.beforeEach(async ({ page, request }) => {
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'system' },
    });
    expect(res.ok()).toBeTruthy();
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
    await page.getByRole('button', { name: /System/i }).click();
    await expect(page.getByTestId('system-view')).toBeVisible();
  });

  test('remembers the window, the grouping, and the project across a reload', async ({ page }) => {
    await page.getByRole('button', { name: 'Today', exact: true }).click();
    await page.getByRole('button', { name: 'Per role' }).click();
    await page.getByTestId('system-project-filter').selectOption('pm-mcp-clients');

    await page.reload();
    await expect(page.getByTestId('system-view')).toBeVisible();

    await expect(page.getByRole('button', { name: 'Today', exact: true })).toHaveClass(/active/);
    await expect(page.getByRole('button', { name: 'Per role' })).toHaveClass(/active/);
    await expect(page.getByTestId('system-project-filter')).toHaveValue('pm-mcp-clients');
  });

  test('links a work figure to what it counts', async ({ page }) => {
    await page.getByTitle(/running plans in the overview/i).first().click();
    await expect(page).toHaveURL(/\/overview$/);
  });

  test('shows the pre-upgrade checks, consents, and withdraws the upgrade', async ({ page }) => {
    await page
      .getByRole('button', { name: /Upgrade/i })
      .first()
      .click();

    const consent = page.getByTestId('upgrade-consent');
    await expect(consent).toBeVisible();
    await expect(consent).toContainText('Upgrade 0.9.1 to 0.10.0?');
    await expect(page.getByTestId('upgrade-check-list')).toContainText('workers are busy');

    await page.getByRole('button', { name: /Upgrade when done/i }).click();

    await expect(page.getByTestId('upgrade-steps')).toBeVisible();
    await page.getByRole('button', { name: /Withdraw the upgrade/i }).click();
    await expect(page.getByTestId('upgrade-withdrawn')).toContainText('0.9.1 keeps running');
  });

  test('opens a cleanup item read-only and keeps the unchecked items', async ({ page }) => {
    await page.getByRole('button', { name: /Review…/i }).click();
    await expect(page.getByTestId('cleanup-view')).toBeVisible();

    const first = page.locator('input[type="checkbox"]').first();
    await first.click();
    await expect(first).not.toBeChecked();

    await page.getByRole('button', { name: /Open fix-relay read-only/i }).click();
    await expect(page.getByTestId('cleanup-preview')).toBeVisible();
    await expect(page.getByText(/opening an item decides nothing/i)).toBeVisible();

    await page.getByRole('button', { name: /Back to the job/i }).click();
    await expect(page.locator('input[type="checkbox"]').first()).not.toBeChecked();
  });
});
