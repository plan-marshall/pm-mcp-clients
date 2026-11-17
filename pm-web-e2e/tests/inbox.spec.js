import { test, expect } from '@playwright/test';
import { pair, switchScenario } from './helpers/pairing.js';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

/**
 * The start page: the list of what waits (UC-PLN-7; needs-and-find.adoc, What Waits).
 *
 * It is `/`, the list column of the lens What waits: the rows in the order the runtime serves them
 * (blocking first), each with its reason, the second line and the act that opens its place; an item
 * resolved in another client leaves with the quiet notice; and nothing waits shows the empty state
 * with the running work.
 */
test.describe('The start page: what waits', () => {
  test.beforeEach(async ({ page, request }) => {
    await switchScenario(request, 'running-work');
    await pair(page, request);
  });

  test.afterAll(async ({ request }) => {
    try {
      await switchScenario(request, 'web-pairing');
    } catch {
      // Best-effort cleanup.
    }
  });

  test('lists what waits in the served order, blocking first', async ({ page, request }) => {
    await page.goto('/');
    await expect(page.getByTestId('inbox-view')).toBeVisible();

    const rows = page.getByTestId('inbox-row');
    await expect(rows.first()).toBeVisible();

    // The runtime's own order, read with the browser's pairing: the client keeps it and sorts nothing.
    const secret = await page.evaluate(() => localStorage.getItem('pm_device_secret'));
    const answer = await request.get(`${MOCK_RUNTIME_URL}/api/v1/machine/needs-operator?page=1`, {
      headers: { Authorization: `Bearer ${secret}` },
    });
    expect(answer.ok()).toBeTruthy();
    const served = (await answer.json()).items;

    const shown = await rows.evaluateAll((elements) =>
      elements.map((element) => element.getAttribute('data-kind')),
    );
    expect(shown).toEqual(served.map((item) => item.kind));
    expect(shown.length).toBeGreaterThan(5);

    // Blocking first: no item without a `blocks` value stands above one that has it, and
    // everything below the first free item blocks nothing.
    const firstFree = served.findIndex((item) => !item.blocks);
    expect(firstFree).toBeGreaterThan(0);
    expect(served.slice(0, firstFree).every((item) => Boolean(item.blocks))).toBe(true);
    expect(served.slice(firstFree).every((item) => !item.blocks)).toBe(true);

    // A decision carries no reason badge; every other kind names its reason.
    const decision = page.locator('[data-testid="inbox-row"][data-kind="decision"]').first();
    await expect(decision.locator('.reason-badge')).toHaveCount(0);
    const held = page.locator('[data-testid="inbox-row"][data-kind="held"]').first();
    await expect(held.locator('.reason-badge')).toContainText('held');
    await expect(held.getByTestId('inbox-detail')).toContainText('waits since');
    await expect(held.getByTestId('inbox-detail')).toContainText('blocks: plan');
  });

  test('opens the place of each act', async ({ page }) => {
    await page.goto('/');

    // A due cleanup opens the cleanup job.
    const cleanup = page.locator('[data-testid="inbox-row"][data-kind="cleanup_due"]').first();
    await cleanup.getByRole('button', { name: 'Cleanup' }).click();
    await expect(page).toHaveURL(/\/system\/cleanup$/);

    // A credential opens the settings' providers.
    await page.goto('/');
    const credential = page.locator('[data-testid="inbox-row"][data-kind="credential"]').first();
    await credential.getByRole('button', { name: 'Credentials' }).click();
    await expect(page).toHaveURL(/\/settings\?tab=credentials$/);

    // An item that belongs to a plan opens the plan.
    await page.goto('/');
    const held = page.locator('[data-testid="inbox-row"][data-kind="held"]').first();
    await held.getByRole('button').click();
    await expect(page).toHaveURL(/\/plans\/fix-relay$/);
    await expect(page.getByTestId('plan-view')).toBeVisible();
  });

  test('lets an item resolved in another client leave, with the quiet notice', async ({
    page,
    request,
  }) => {
    await page.goto('/');
    await expect(page.locator('[data-testid="inbox-row"][data-kind="decision"]').first()).toBeVisible();

    // Another client resolves what waits: the runtime serves a list without those items and
    // announces the change on the stream.
    const moved = await request.post(`${MOCK_RUNTIME_URL}/mock/goto/overview.needs-changed`);
    expect(moved.ok()).toBeTruthy();

    await expect(page.locator('[data-testid="inbox-row"][data-kind="decision"]')).toHaveCount(0, {
      timeout: 15000,
    });
    // The list still holds what waits, and the frame's polite region names what left.
    await expect(page.getByTestId('inbox-row').first()).toBeVisible();
    await expect
      .poll(
        () =>
          page.evaluate(() =>
            [...document.querySelectorAll('[aria-live="polite"]')]
              .map((region) => (region.textContent ?? '').trim())
              .join(' '),
          ),
        { timeout: 15000 },
      )
      .toContain('was resolved in another client');
  });

  test('shows the same total in the rail, the tab title and the list', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByTestId('inbox-view')).toBeVisible();

    const entries = page.getByTestId('nav-rail').locator('.nav-rail__item');
    await expect(entries.first()).toContainText('What waits');
    await expect(page.getByTestId('nav-inbox-btn')).toHaveAttribute('aria-current', 'page');

    const count = ((await page.getByTestId('inbox-count').textContent()) ?? '').replace(/\D/g, '');
    expect(count).not.toBe('');
    await expect(page.getByTestId('nav-inbox-btn-count')).toHaveText(count);
    await expect(page).toHaveTitle(new RegExp(`${count} items? waits?`));
  });

  test('shows the empty state when nothing waits', async ({ page, request }) => {
    const moved = await request.post(`${MOCK_RUNTIME_URL}/mock/goto/overview.empty`);
    expect(moved.ok()).toBeTruthy();

    await page.goto('/');

    const empty = page.getByTestId('inbox-empty');
    await expect(empty).toBeVisible();
    await expect(empty).toContainText('Nothing waits for you.');
    await expect(page.getByTestId('inbox-row')).toHaveCount(0);
  });
});
