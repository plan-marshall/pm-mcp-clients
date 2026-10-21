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

  test('shows the difference and places the open unit in a project', async ({ page }) => {
    // Choose the project explicitly so the placement lands there
    await page.getByTestId('project-select').selectOption('pm-mcp-clients');

    // A changed unit is what the comparison and the placing are for; the same text is refused.
    await page
      .getByTestId('workflow-text')
      .fill(
        'workflow landing.pull-request version 2 format 1\n' +
          '---\nname: Landing\ntype: subworkflow\nentry start\nexit done\n---\n' +
          'state start\n  done -> done\n',
      );
    await expect(page.getByTestId('workflow-text')).toHaveValue(/state start/);

    await page.getByTestId('validate-button').click();
    await expect(page.getByTestId('validation-valid')).toBeVisible();

    await page.getByTestId('place-button').click();
    await expect(page.getByTestId('place-dialog')).toBeVisible();
    await page.getByTestId('place-compare').click();

    await expect(page.getByTestId('designer-diff')).toBeVisible();
    await expect(page.getByTestId('place-confirm')).toBeVisible();
    await page.getByTestId('place-confirm').click();

    await expect(page.getByTestId('designer-place-done')).toContainText('Placed:', {
      timeout: 15000,
    });
  });
});

test.describe('Process Designer: intake, draft, frontmatter, and phone width (UC-PLN-3)', () => {
  test.beforeEach(async ({ page, request }) => {
    const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
      data: { scenario: 'designer' },
    });
    expect(res.ok()).toBeTruthy();
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
    await page.getByRole('button', { name: 'Machine settings' }).click();
    await page.getByTestId('machine-settings-button').click();
    await page.getByTestId('open-designer-btn').click();
    await expect(page.getByTestId('process-designer')).toBeVisible();
  });

  test('reads an uploaded unit in the browser', async ({ page }) => {
    await page.getByRole('button', { name: 'Upload a file' }).click();
    await page.getByTestId('designer-file-input').setInputFiles({
      name: 'uploaded.pmwf',
      mimeType: 'text/plain',
      buffer: Buffer.from(
        'workflow uploaded-unit version 1 format 1\n' +
          '---\nname: Uploaded\ntype: subworkflow\nentry start\nexit done\n---\n' +
          'state start\n  done -> done\n',
      ),
    });

    await expect(page.getByTestId('workflow-text')).toHaveValue(/workflow uploaded-unit/);
    await expect(page.getByTestId('designer-unit')).toHaveText('uploaded-unit');
  });

  test('keeps the draft across a reload and says when it is from', async ({ page }) => {
    await page
      .getByTestId('workflow-text')
      .fill(
        'workflow landing.pull-request version 9 format 1\n---\nname: Drafted\n---\nstate start\n',
      );
    page.on('dialog', (dialog) => dialog.accept());
    await page.reload();

    await expect(page.getByTestId('process-designer')).toBeVisible();
    await expect(page.getByTestId('workflow-text')).toHaveValue(/name: Drafted/);
    await expect(page.getByTestId('designer-notice')).toContainText('Your draft from');
  });

  test('writes a frontmatter change from the panel into the text', async ({ page }) => {
    await page.getByTestId('frontmatter-name').fill('Renamed');
    await expect(page.getByTestId('workflow-text')).toHaveValue(/name: Renamed/);
    await expect(page.getByTestId('workflow-text')).toHaveValue(/modified: \d{4}-\d{2}-\d{2}/);
  });

  test('jumps to the line of a validation finding', async ({ page }) => {
    await page.getByTestId('workflow-text').fill('this is not a workflow');
    await page.getByTestId('validate-button').click();
    await expect(page.getByTestId('validation-summary')).toContainText('1 finding');

    await page.getByTestId('finding-0').getByRole('button').click();

    const line = await page.getByTestId('workflow-text').evaluate((element) => {
      const field = element;
      return field.value.slice(0, field.selectionStart).split('\n').length;
    });
    expect(line).toBeGreaterThan(0);
  });

  test('asks before leaving a changed draft', async ({ page }) => {
    await page.getByTestId('workflow-text').fill('workflow changed version 1 format 1\n');
    await page.getByTestId('designer-back').click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Leave the designer?');
    await dialog.getByRole('button', { name: 'Stay' }).click();
    await expect(page.getByTestId('process-designer')).toBeVisible();
  });

  test('is read only at phone width with Validate kept', async ({ page }) => {
    await page.setViewportSize({ width: 600, height: 900 });

    await expect(page.getByTestId('designer-phone-note')).toBeVisible();
    await expect(page.getByTestId('workflow-text')).toHaveAttribute('readonly', '');
    await expect(page.getByTestId('validate-button')).toBeVisible();
    await expect(page.getByTestId('place-button')).toHaveCount(0);
  });
});
