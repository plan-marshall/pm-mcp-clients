import { test, expect } from '@playwright/test';
import {
  addDescription,
  continueAndStart,
  openProject,
  openStartSources,
  pair,
  switchScenario,
} from './helpers/pairing.js';

test.describe('Web Client Starting Work (UC-STA-1..5)', () => {
  test.beforeEach(async ({ page, request }) => {
    await switchScenario(request, 'start');
    await pair(page, request);
    await openProject(page, 'pm-mcp-clients');
  });

  test('starts a plan from a description', async ({ page }) => {
    await openStartSources(page);
    await addDescription(page, 'Add a Codeberg provider for issues');

    await continueAndStart(page);

    await expect(page.getByTestId('started-title')).toContainText(
      'Add a Codeberg provider for issues has started',
    );
  });

  test('runs a recipe and derives its plan id', async ({ page }) => {
    await page.getByRole('button', { name: 'Start the plan' }).click();
    await expect(page.getByTestId('start-choose')).toBeVisible();
    await page.getByTestId('choose-recipe').click();

    await expect(page.getByTestId('start-recipe')).toBeVisible();
    await page.getByTestId('recipe-choose-code-review').click();

    // A recipe with parameters asks for them first.
    await expect(page.getByTestId('recipe-parameters')).toBeVisible();
    await page.getByTestId('recipe-continue').click();

    await expect(page.getByTestId('recipe-ready')).toBeVisible();
    await expect(page.getByTestId('ready-id')).toHaveValue(/^recipe-code-review-/);

    await page.getByTestId('ready-start').click();
    await expect(page.getByTestId('started-title')).toContainText(/recipe-code-review-/);
  });

  test('starts an epic and reports it', async ({ page }) => {
    await openStartSources(page, 'epic');
    await addDescription(page, 'Relay hardening');

    await continueAndStart(page);

    await expect(page.getByTestId('started-title')).toContainText('Relay hardening has started');
    await expect(page.getByTestId('started-decompose')).toBeVisible();
  });

  test('refuses a start in a project that is not ready', async ({ page }) => {
    // The scenario serves three projects; the bare plan-marshall one still asks for setup.
    await page
      .locator('.project-list')
      .getByRole('tab', { name: /^! 1 plan-marshall$/ })
      .click();
    await expect(page.locator('.project-title')).toContainText('plan-marshall');

    await page.getByRole('button', { name: 'Start the plan' }).click();

    await expect(page.getByTestId('start-error')).toBeVisible({ timeout: 15000 });
    await expect(page.getByTestId('start-error')).toContainText('start_refused');
  });
});
