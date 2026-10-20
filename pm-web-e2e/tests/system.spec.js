import { test, expect } from '@playwright/test';

test.describe('System View, Runtime Lifecycle & Cleanup (UC-SYS-1..3)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to system scenario
    const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
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
      await request.post('http://127.0.0.1:7420/mock/scenario', {
        data: { scenario: 'web-pairing' },
      });
    } catch {
      // Best-effort cleanup
    }
  });

  test('renders 6-part system dashboard with metrics and cards', async ({ page }) => {
    // 1. Runtime Card
    await expect(page.getByRole('heading', { name: 'Runtime' })).toBeVisible();
    await expect(page.getByText(/pm-mcpd 0\.9\.1/i).first()).toBeVisible();
    await expect(page.getByText(/CPU/i)).toBeVisible();

    // 2. Work Card
    await expect(page.getByRole('heading', { name: 'Work', exact: true })).toBeVisible();
    await expect(page.getByText(/plans/i)).toBeVisible();

    // 3. Workers Card
    await expect(page.getByRole('heading', { name: 'Workers' })).toBeVisible();
    await expect(page.getByText(/busy/i)).toBeVisible();
    await expect(page.getByText(/idle/i)).toBeVisible();

    // 4. Jobs Card
    await expect(page.getByRole('heading', { name: 'Jobs' })).toBeVisible();
    await expect(page.getByText(/build/i)).toBeVisible();

    // 5. Models Card
    await expect(page.getByRole('heading', { name: 'Models' })).toBeVisible();

    // 6. Cleanup Card
    await expect(page.getByRole('heading', { name: 'Cleanup' })).toBeVisible();
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
