import { test, expect } from '@playwright/test';

async function pairOnStartScenario(page, request) {
  const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
    data: { scenario: 'start' },
  });
  expect(res.ok()).toBeTruthy();

  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/#code=KXQB-MRTN');
  await expect(page.locator('.app-header__machine')).toContainText('Machine:');

  const overviewNav = page.locator('.app-header').getByRole('button', { name: /Running work/i });
  await expect(overviewNav).toBeVisible();
  await overviewNav.click();
  await expect(page.locator('.overview')).toBeVisible();

  // The scenario's plan-marshall asks for setup; select the ready project explicitly.
  await page
    .locator('.project-list')
    .getByRole('tab', { name: /pm-mcp-clients/i })
    .click();
  await expect(page.locator('.project-title')).toContainText('pm-mcp-clients');
}

test.describe('Web Client Starting Work (UC-STA-1..5)', () => {
  test.beforeEach(async ({ page, request }) => {
    await pairOnStartScenario(page, request);
  });

  test('starts a plan from a description and a document', async ({ page }) => {
    await page.getByRole('button', { name: 'Start the plan' }).click();

    const modal = page.getByTestId('start-modal');
    await expect(modal).toBeVisible();
    await expect(page.getByTestId('start-title')).toContainText('Start a plan in pm-mcp-clients');

    await page.getByTestId('source-description').fill('Add a Codeberg provider for issues');
    await page.getByTestId('source-documents').fill('doc/concepts/forgejo.adoc');
    await page.getByTestId('source-issue').fill('4711');
    await page.getByTestId('start-submit').click();

    await expect(page.getByTestId('start-done')).toContainText(
      'add-a-codeberg-provider-for-issues has started',
    );
  });

  test('runs a recipe and derives its plan id', async ({ page }) => {
    await page.getByRole('button', { name: 'Start the plan' }).click();
    await page.getByTestId('start-mode-recipe').click();

    await expect(page.getByTestId('recipe-select')).toBeVisible();
    await page.getByTestId('recipe-select').selectOption('coverage');
    await expect(page.getByTestId('recipe-parameters-title')).toContainText('coverage');

    await page.getByTestId('start-submit').click();
    await expect(page.getByTestId('start-done')).toContainText('has started');
    await expect(page.getByTestId('start-done')).toContainText('recipe-coverage-');
  });

  test('starts an epic and reports it', async ({ page }) => {
    await page.getByRole('button', { name: 'Start the epic' }).click();

    await expect(page.getByTestId('start-title')).toContainText('Start an epic in pm-mcp-clients');
    await page.getByTestId('source-description').fill('Relay hardening');
    await page.getByTestId('start-submit').click();

    await expect(page.getByTestId('start-done')).toContainText('relay-hardening has started');
  });

  test('refuses a start in a project that is not ready', async ({ page }) => {
    // The start scenario serves three projects; select the one that is not ready.
    await page
      .locator('.project-list')
      .getByRole('tab', { name: /plan-marshall/i })
      .click();
    await page.getByRole('button', { name: 'Start the plan' }).click();
    await page.getByTestId('source-description').fill('something');
    await page.getByTestId('start-submit').click();

    await expect(page.getByTestId('start-error')).toBeVisible();
  });
});
