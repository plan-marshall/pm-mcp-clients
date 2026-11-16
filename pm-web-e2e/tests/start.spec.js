import { test, expect } from '@playwright/test';
import {
  addDescription,
  addSource,
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
      '\u201cAdd a Codeberg provider for issues\u201d has started.',
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

    await expect(page.getByTestId('started-title')).toContainText(
      '\u201cRelay hardening\u201d has started.',
    );
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

    // The gate answers the catalogue's act_not_available (its reason project_not_ready stays
    // in the body); the view draws that code's title and keeps the code under Details.
    await expect(page.getByTestId('start-error')).toBeVisible({ timeout: 15000 });
    await expect(page.getByTestId('start-error')).toContainText('This act is not possible now');
    await expect(page.getByTestId('start-error')).toContainText('act_not_available');
  });

  test('keeps one primary act per step and goes back from the ready step to the sources', async ({
    page,
  }) => {
    await page.getByRole('button', { name: 'Start the plan' }).click();

    // ① asks what the plan starts from: one primary act, the choice of sources.
    await expect(page.getByTestId('start-choose')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('.btn--primary')).toHaveCount(1);

    await page.getByTestId('choose-sources').click();
    await expect(page.getByTestId('source-list')).toBeVisible();
    await expect(page.locator('.btn--primary')).toHaveCount(1);

    await addDescription(page, 'Add a Codeberg provider for issues');
    await page.getByTestId('source-continue').click();

    // ② Ready: one primary act, the start of the plan.
    await expect(page.getByTestId('recipe-ready')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('.btn--primary')).toHaveCount(1);
    await expect(page.getByTestId('ready-start')).toBeVisible();

    // Back returns to the sources, whose own primary is the only one again.
    await page.getByTestId('start-back').click();
    await expect(page.getByTestId('source-list')).toBeVisible();
    await expect(page.getByTestId('start-back')).toHaveCount(0);
    await expect(page.locator('.btn--primary')).toHaveCount(1);
  });

  test('shows the setup gate with its cause and its retry after 20 s without an answer', async ({
    page,
  }) => {
    test.setTimeout(60000);
    // The gate is the opening call: an entry call that never answers is the slow case.
    await page.route('**/api/v1/projects/*/entries', () => {});

    await page.getByRole('button', { name: 'Start the plan' }).click();
    await expect(page.getByTestId('start-gate-checking')).toBeVisible();

    await expect(page.getByTestId('start-gate-cause')).toBeVisible({ timeout: 30000 });
    await expect(page.getByTestId('start-error')).toContainText('The request took too long');
    await expect(page.getByTestId('start-gate-retry')).toHaveText('Try again');
  });

  test('shows the error pattern with its act for a start without a project', async ({ page }) => {
    await page.goto('/start');

    const pattern = page.getByTestId('start-no-project');
    await expect(pattern).toBeVisible();
    await expect(pattern).toContainText('There is no project to start work in');
    await expect(pattern).toContainText('A start needs a project');

    await page.getByTestId('start-choose-project').click();
    await expect(page.getByTestId('project-explorer-view')).toBeVisible({ timeout: 15000 });
  });
});
