import { test, expect } from '@playwright/test';

async function pairOnGitSourcesScenario(page, request) {
  const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
    data: { scenario: 'git_sources' },
  });
  expect(res.ok()).toBeTruthy();

  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/#code=GTSR-CMTR');
  await expect(page.locator('.app-header__machine')).toContainText('Machine:');

  const overviewNav = page.locator('.app-header').getByRole('button', { name: /Running work/i });
  await expect(overviewNav).toBeVisible();
  await overviewNav.click();
  await expect(page.locator('.overview')).toBeVisible();

  // The scenario serves a not-ready project too; select the ready one explicitly.
  await page
    .locator('.project-list')
    .getByRole('tab', { name: /pm-mcp-clients/i })
    .click();
  await expect(page.locator('.project-title')).toContainText('pm-mcp-clients');
}

async function openGitSourceModal(page) {
  await page.getByRole('button', { name: 'Adopt a pull request' }).click();
  await expect(page.getByTestId('git-source-modal')).toBeVisible();
}

test.describe('Web Client Git Plan Sources (UC-STA-7, UC-STA-9)', () => {
  test.beforeEach(async ({ page, request }) => {
    await pairOnGitSourcesScenario(page, request);
  });

  test('observes and adopts a pull request', async ({ page }) => {
    await openGitSourceModal(page);
    await expect(page.getByTestId('git-source-title')).toContainText(
      'Git sources in pm-mcp-clients',
    );

    await page.getByTestId('pr-reference-input').fill('4711');
    await page.getByTestId('pr-observe').click();

    await expect(page.getByTestId('pr-observation')).toBeVisible();
    await expect(page.getByTestId('pr-files')).toContainText('untrusted');

    await page.getByTestId('pr-adopt-submit').click();
    await expect(page.getByTestId('git-source-done')).toContainText(
      'adopt-pr-4711 adopted the pull request',
    );
  });

  test('requires an explicit acceptance for a flagged pull request', async ({ page }) => {
    await openGitSourceModal(page);

    await page.getByTestId('pr-reference-input').fill('6677');
    await page.getByTestId('pr-observe').click();

    await expect(page.getByTestId('pr-flagged-title')).toBeVisible();
    await expect(page.getByTestId('pr-adopt-submit')).toBeDisabled();

    await page.getByTestId('pr-accept-screening').check();
    await page.getByTestId('pr-adopt-submit').click();
    await expect(page.getByTestId('git-source-done')).toContainText('adopted the pull request');
  });

  test('previews and captures the local changes', async ({ page }) => {
    await openGitSourceModal(page);
    await page.getByTestId('git-source-tab-local').click();

    await expect(page.getByTestId('local-preview')).toBeVisible();
    await expect(page.getByTestId('local-eligible')).toBeVisible();
    await expect(page.getByTestId('local-capture')).toBeDisabled();

    await page.getByTestId('local-confirm').check();
    await page.getByTestId('local-capture').click();

    await expect(page.getByTestId('git-source-done')).toContainText('captured the local changes');
    await expect(page.getByTestId('git-source-snapshot')).toContainText('snapshot commit');
  });

  test('refuses local changes where the landing does not commit (PM-WF-25)', async ({ page }) => {
    // pm-mcp-core stands in for a project whose landing is landing.files.
    await page
      .locator('.project-list')
      .getByRole('tab', { name: /pm-mcp-core/i })
      .click();
    await expect(page.locator('.project-title')).toContainText('pm-mcp-core');

    await openGitSourceModal(page);
    await page.getByTestId('git-source-tab-local').click();

    await expect(page.getByTestId('local-ineligible')).toBeVisible();
    await expect(page.getByTestId('local-pm-wf-25')).toContainText('PM-WF-25');
    await expect(page.getByTestId('local-capture')).toBeDisabled();
  });
});
