import { test, expect } from '@playwright/test';

test.describe('Process Designer Flow (UC-PLN-3)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch the mock runtime to the designer scenario
    const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
      data: { scenario: 'designer' },
    });
    expect(res.ok()).toBeTruthy();

    // Clear localStorage and pair with the scenario's code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    // Open the designer from the machine settings
    await page.getByRole('button', { name: 'Machine settings' }).click();
    await page.getByTestId('machine-settings-button').click();
    await expect(page.getByTestId('machine-settings-view')).toBeVisible();
    await page.getByTestId('open-designer-btn').click();
    await expect(page.getByTestId('process-designer')).toBeVisible();
    await expect(page).toHaveURL(/\/designer$/);
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

  test('loads the installed units and the building-block palette', async ({ page }) => {
    await expect(page.getByTestId('designer-title')).toHaveText('Process designer');
    await expect(page.getByTestId('designer-checked')).toContainText(
      "checked against this machine's projects",
    );

    // A machine unit is opened and its text is in the editor
    await expect(page.getByTestId('workflow-text')).toHaveValue(/workflow landing\.pull-request/);

    // The palette groups the blocks of the machine catalogue
    await expect(page.getByTestId('block-palette')).toBeVisible();
    await expect(page.getByTestId('block-group-primitive')).toBeVisible();
    await expect(page.getByTestId('block-select-git.commit_and_push')).toBeVisible();

    // The compiled-only unit is marked and offers no removal
    await expect(page.getByTestId('unit-compiled-epic')).toBeVisible();
  });

  test('validates the unit and shows the composed process', async ({ page }) => {
    await page.getByTestId('validate-button').click();

    await expect(page.getByTestId('validation-stage-parse')).toContainText('parses');
    await expect(page.getByTestId('validation-valid')).toContainText('the runtime would load it');

    await page.getByTestId('preview-button').click();
    const preview = page.getByTestId('composed-preview');
    await expect(preview).toBeVisible();
    await expect(preview).toContainText('If a plan started now');
    await expect(page.getByTestId('preview-node-phase-6-finalize')).toBeVisible();
  });

  test('shows a parse finding for invalid text', async ({ page }) => {
    const editor = page.getByTestId('workflow-text');
    await editor.fill('this is not a workflow');

    await page.getByTestId('validate-button').click();

    await expect(page.getByTestId('validation-stage-parse')).toContainText('parses');
    await expect(page.getByTestId('validation-summary')).toContainText('1 finding');
    await expect(page.getByTestId('validation-findings')).toContainText('workflow <name>');
    await expect(page.getByTestId('place-button')).toBeDisabled();
  });

  test('inserts a building block into the text', async ({ page }) => {
    await page.getByTestId('block-insert-git.commit_and_push').click();

    await expect(page.getByTestId('workflow-text')).toHaveValue(
      /call git\.commit_and_push entry commit/,
    );
  });

  test('places the open unit in a project', async ({ page }) => {
    // Choose the project explicitly so the placement lands there
    await page.getByTestId('project-select').selectOption('pm-mcp-clients');

    await page.getByTestId('validate-button').click();
    await expect(page.getByTestId('validation-valid')).toBeVisible();

    await page.getByTestId('place-button').click();
    await expect(page.getByTestId('designer-placed')).toContainText('Placed:');
  });
});
