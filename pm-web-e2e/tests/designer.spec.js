import { test, expect } from '@playwright/test';

test.describe('Process Designer Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Pair browser first
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    // Open projects / workspace view and click designer
    const explorerBtn = page.getByRole('button', { name: 'Projects' });
    if (await explorerBtn.isVisible()) {
      await explorerBtn.click();
      const designerBtn = page.getByRole('button', { name: 'Process Designer' });
      await designerBtn.click();
    }
  });

  test('renders Process Designer with editor and validates workflow', async ({ page }) => {
    await expect(page.locator('.designer__title')).toHaveText('Process Designer');
    await expect(page.locator('.editor-textarea')).toBeVisible();

    const validateBtn = page.getByRole('button', { name: 'Validate' });
    await validateBtn.click();

    await expect(page.locator('.validation-bar')).toContainText('The process is valid.');
  });

  test('inserts a building block into the editor', async ({ page }) => {
    const qualityGateBtn = page.getByRole('button', { name: 'Quality Gate' });
    await qualityGateBtn.click();

    const editor = page.locator('.editor-textarea');
    await expect(editor).toHaveValue(/gate quality-check:/);
  });
});
