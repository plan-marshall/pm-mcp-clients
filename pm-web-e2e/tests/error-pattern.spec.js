import { test, expect } from '@playwright/test';
import { pair, switchScenario } from './helpers/pairing.js';
import { ensureMockRuntime, stopMockRuntime } from './helpers/mockRuntime.js';

/**
 * The one error and loading pattern (UX-S-06, UX-P-08): a failed load draws the shared
 * `ErrorState` with the texts of its code and the one act, in both languages; a request that never
 * answers ends in the pattern within the client's 15-second limit, never in an endless spinner.
 *
 * A runtime that is not reachable at all is not a failed load: the app says so on a page of its own
 * (entry.adoc, Where the App Opens), which covers the open view without replacing it, so the
 * view's own error block stays hidden behind it.
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

  test('shows the pattern within 15 s when a view read never answers', async ({
    page,
    request,
  }) => {
    // The client's 15-second limit is the assertion's own subject: the pattern must be there once
    // that limit passes, with the 5-second margin the clause names, so the wait is bounded at 20 s
    // and the test's own bound stays above it.
    test.setTimeout(45000);
    await switchScenario(request, 'plan-view');
    await pair(page, request);

    // Hold the plan read open, so the view really waits on it rather than failing at once. The
    // runtime itself stays reachable, so the view is the page shown, and the client's own
    // 15-second limit must end the wait.
    await page.route('**/api/v1/plans/does-not-exist**', () => {});
    await page.goto('/plans/does-not-exist');

    const error = page.getByTestId('error-state');
    await expect(error).toBeVisible({ timeout: 20000 });
    await expect(error.getByRole('heading')).toHaveText('The request took too long');
    await expect(error.getByRole('button', { name: 'Try again' })).toBeVisible();
  });

  test('covers the open view with the unreachable page when the mock is gone', async ({
    page,
    request,
  }) => {
    await switchScenario(request, 'plan-view');
    await pair(page, request);

    await stopMockRuntime();
    await page.goto('/plans/does-not-exist');

    // The runtime is not reachable: its own page says so instead of the view's error block.
    const unreachable = page.getByTestId('unreachable-view');
    await expect(unreachable).toBeVisible({ timeout: 15000 });
    await expect(unreachable.getByRole('heading')).toHaveText(
      'plan-marshall is not reachable on this machine',
    );
    // The open view stays mounted behind it, hidden, so nothing the operator had open is lost;
    // its error block is not what the operator sees.
    await expect(page.getByTestId('open-view')).toBeHidden();
    await expect(page.getByTestId('error-state')).toBeHidden();
  });
});
