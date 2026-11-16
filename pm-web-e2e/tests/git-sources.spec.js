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

test.describe('Web Client Git Plan Sources (UC-STA-7, UC-STA-9)', () => {
  test.beforeEach(async ({ page, request }) => {
    await switchScenario(request, 'git_sources');
    await pair(page, request);
    await openProject(page, 'pm-mcp-clients');
  });

  test('adds a pull request with its changed files and starts the plan', async ({ page }) => {
    await openStartSources(page);
    await addSource(page, 'pull_request');

    await expect(page.getByTestId('pr-panel')).toBeVisible();
    await page
      .getByTestId('pr-input')
      .fill('https://github.com/plan-marshall/pm-mcp-clients/pull/214');
    await page.getByTestId('pr-submit').click();

    await expect(page.getByTestId('pr-panel')).toHaveCount(0);
    const source = page.getByTestId('source-list');
    await expect(source).toContainText('Add the Codeberg provider');
    await expect(page.locator('[data-testid^="pr-files-"]')).toBeVisible();
    await expect(page.locator('[data-testid^="pr-files-"]')).toContainText('untrusted');

    // The sources step keeps its one primary act, and Continue is it.
    await expect(page.locator('.btn--primary')).toHaveCount(1);

    await continueAndStart(page);

    await expect(page.getByTestId('started-title')).toBeVisible();
  });

  test('previews and adds the local changes', async ({ page }) => {
    await openStartSources(page);
    await addSource(page, 'local_changes');

    const panel = page.getByTestId('local-changes-panel');
    await expect(panel).toBeVisible();
    await expect(page.getByTestId('local-changes-summary')).toBeVisible();
    await expect(page.getByTestId('local-changes-ineligible')).toHaveCount(0);

    await page.getByTestId('local-changes-add').click();

    await expect(page.getByTestId('local-changes-panel')).toHaveCount(0);
    await expect(page.getByTestId('source-list')).toContainText('2 files changed');

    // Back from the ready step returns to the sources, whose one primary is Continue again.
    await page.getByTestId('source-continue').click();
    await expect(page.getByTestId('recipe-ready')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('.btn--primary')).toHaveCount(1);
    await page.getByTestId('start-back').click();
    await expect(page.getByTestId('source-list')).toBeVisible();
    await expect(page.locator('.btn--primary')).toHaveCount(1);

    await page.getByTestId('source-continue').click();
    await expect(page.getByTestId('recipe-ready')).toBeVisible({ timeout: 15000 });
    await page.getByTestId('ready-start').click();
    await expect(page.getByTestId('started-title')).toBeVisible();
  });
});
