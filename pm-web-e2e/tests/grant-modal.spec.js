import { test, expect } from '@playwright/test';
import {
  addSource,
  openProject,
  openStartSources,
  pair,
  switchScenario,
} from './helpers/pairing.js';

test.describe('GrantModal (UC-STA-3)', () => {
  test.beforeEach(async ({ page, request }) => {
    await switchScenario(request, 'start');
    await pair(page, request);
    await openProject(page, 'pm-mcp-clients');
  });

  test('waits on the address and the DOM, opens the grant question and handles answers', async ({
    page,
  }) => {
    await openStartSources(page);
    await expect(page).toHaveURL(/\/projects\/pm-mcp-clients\/start|\/entries\//);
    await expect(page.getByTestId('start-view')).toBeVisible();

    await addSource(page, 'issue');
    await expect(page.getByTestId('issue-panel')).toBeVisible();

    await page.getByTestId('issue-input').fill('https://github.com/cuioss/cui-http/issues/42');
    await page.getByTestId('issue-submit').click();

    const modal = page.getByTestId('grant-modal');
    await expect(modal).toBeVisible();
    await expect(modal).toHaveAttribute('role', 'dialog');
    await expect(modal).toContainText('cuioss/cui-http');

    // Refusal closes the modal and reports the refusal in the DOM
    await modal.getByTestId('grant-no').click();
    await expect(modal).not.toBeVisible();
    await expect(page.getByTestId('issue-error')).toBeVisible();

    // Reopen and close via Escape
    await page.getByTestId('issue-submit').click();
    await expect(modal).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(modal).not.toBeVisible();

    // Reopen and grant once
    await page.getByTestId('issue-submit').click();
    await expect(modal).toBeVisible();
    await modal.getByTestId('grant-once').click();
    await expect(modal).not.toBeVisible();
  });
});
