import { test, expect } from '@playwright/test';
import { pair, switchScenario } from './helpers/pairing.js';
import { ensureMockRuntime, stopMockRuntime } from './helpers/mockRuntime.js';

/**
 * The one error and loading pattern (UX-S-06, UX-P-08): a failed load draws the shared
 * `ErrorState` with the texts of its code and the one act, in both languages; a request that never
 * answers ends in the pattern within the client's 15-second limit, never in an endless spinner.
 */
test.describe('The one error and loading pattern (UX-S-06)', () => {
  test.afterEach(async ({ request }) => {
    // A check that failed between the stop and the start must not take the mock, and with it every
    // following serial test, down.
    await ensureMockRuntime('fully-onboarded');
    try {
      await switchScenario(request, 'web-pairing');
    } catch {
      // Best-effort cleanup.
    }
  });

  test('shows the pattern for an unknown plan instead of a raw code', async ({ page, request }) => {
    await switchScenario(request, 'plan-view');
    await pair(page, request);

    await page.goto('/plans/does-not-exist');

    const error = page.getByTestId('error-state');
    await expect(error).toBeVisible({ timeout: 15000 });
    await expect(error.getByRole('heading')).toHaveText('This entry does not exist');
    await expect(error).toContainText('Check the id or pick the entry from the list again.');
    await expect(error.getByRole('button', { name: 'Try again' })).toBeVisible();

    const details = error.locator('details');
    await expect(details.locator('summary')).toHaveText('Details');
    await details.locator('summary').click();
    await expect(details).toContainText(
      'No plan, epic, decision, report or entry carries this id.',
    );
    await expect(details.locator('code')).toHaveText('scope_not_found');
    await expect(error).not.toContainText('plan_not_found');
  });

  test('shows the pattern within 15 s when the mock stops while a view loads', async ({
    page,
    request,
  }) => {
    // The client's 15-second limit is the assertion's own subject: the pattern must be there once
    // that limit passes, with the 5-second margin the clause names, so the wait is bounded at 20 s
    // and the test's own bound stays above it.
    test.setTimeout(45000);
    await switchScenario(request, 'plan-view');
    await pair(page, request);

    // Hold the plan read open, so the view really waits on it rather than failing at once; the
    // machine goes away underneath, and the client's own 15-second limit must end the wait.
    await page.route('**/api/v1/plans/does-not-exist/**', () => {});
    await stopMockRuntime();
    await page.goto('/plans/does-not-exist');

    const error = page.getByTestId('error-state');
    await expect(error).toBeVisible({ timeout: 20000 });
    await expect(error.getByRole('heading')).toHaveText('The request took too long');
    await expect(error.getByRole('button', { name: 'Try again' })).toBeVisible();
  });

  test('shows the offline pattern when the mock is already gone', async ({ page, request }) => {
    await switchScenario(request, 'plan-view');
    await pair(page, request);

    await stopMockRuntime();
    await page.goto('/plans/does-not-exist');

    const error = page.getByTestId('error-state');
    await expect(error).toBeVisible({ timeout: 15000 });
    // The dev server proxies `/api` to the mock, so a dead mock is an unreadable HTTP failure, not a
    // refused connection: the client reads no code and the block draws the shared unknown fallback.
    await expect(error.getByRole('heading')).toHaveText('The runtime refused the request');
    await expect(error.getByRole('button', { name: 'Try again' })).toBeVisible();
  });
});
