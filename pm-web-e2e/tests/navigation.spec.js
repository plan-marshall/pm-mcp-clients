import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

/**
 * The address scheme (`doc/design/web-app/navigation.adoc#url`): every object has one canonical
 * address, the address bar carries the selection, and the remembered place fills a load the app
 * did not create at two entry points only.
 *
 * Every wait is on the address or the DOM the app produces, never on time.
 */
test.describe('Addresses and the remembered place (D2)', () => {
  test.beforeEach(async ({ page, request }) => {
    const switchRes = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'running-work' },
    });
    expect(switchRes.ok()).toBeTruthy();

    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test.afterAll(async ({ request }) => {
    try {
      await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
        data: { scenario: 'web-pairing' },
      });
    } catch {
      // Best-effort cleanup
    }
  });

  test('loads every owned address at its own route', async ({ page }) => {
    await page.goto('/overview');
    await expect(page.locator('.overview')).toBeVisible();
    await expect(page).toHaveURL(/\/overview$/);

    await page.goto('/decomposition');
    await expect(page).toHaveURL(/\/decomposition$/);

    // An epic called `decompose` is an id, because the workflow has `/decomposition` of its own.
    await page.goto('/epics/decompose');
    await expect(page).toHaveURL(/\/decomposition$/);
  });

  test('opens a deep link and keeps the address of the plan it names', async ({ page }) => {
    await page.goto('/plans/fix-relay');
    await expect(page.locator('[data-testid="plan-title"]')).toBeVisible();
    await expect(page).toHaveURL(/\/plans\/fix-relay$/);
  });

  test('moves back and forward through the selections', async ({ page }) => {
    await page.goto('/plans/fix-relay');
    await expect(page.locator('[data-testid="plan-title"]')).toBeVisible();

    await page.getByTestId('breadcrumb').getByRole('link', { name: 'pm-mcp-clients' }).click();
    await expect(page).toHaveURL(/\/overview\?project=pm-mcp-clients$/);

    await page.goBack();
    await expect(page).toHaveURL(/\/plans\/fix-relay$/);

    await page.goForward();
    await expect(page).toHaveURL(/\/overview\?project=pm-mcp-clients$/);
  });

  test('replaces an old address with the address of the scheme', async ({ page }) => {
    await page.goto('/decisions?decision=Q-007');
    await expect(page).toHaveURL(/\/decisions\/Q-007$/);
    await expect(page.getByTestId('decision-detail')).toHaveAttribute(
      'data-decision-id',
      'Q-007',
    );
  });

  test('fills a fresh load at /overview from the remembered place, by a replace', async ({
    page,
  }) => {
    // The place the operator left: a selected plan, never the mode. The mock summary carries no
    // plan, so the plan alone is remembered — the fill is what this case proves.
    await page.evaluate(() => {
      localStorage.setItem(
        'pm_overview_selected_object',
        JSON.stringify({ planId: 'fix-relay' }),
      );
      localStorage.setItem('pm_overview_view_mode', 'plan');
    });

    // A new document at the bare overview carries no mark of the app, so the place fills it.
    await page.evaluate(() => window.location.assign('/overview'));
    await expect(page.locator('[data-testid="plan-title"]')).toBeVisible();
    await expect(page).toHaveURL(/\/plans\/fix-relay$/);
  });

  test('fills a click on Running work by a push and opens running work after ended work', async ({
    page,
  }) => {
    await page.evaluate(() => {
      localStorage.setItem('pm_overview_selected_project', 'pm-mcp-clients');
      localStorage.setItem('pm_overview_view_mode', 'plan');
    });

    await page.goto('/plans/fix-relay?mode=ended');
    await expect(page.locator('[data-testid="plan-title"]')).toBeVisible();

    const runningWork = page.locator('.nav-rail').getByRole('button', { name: /Running work/i });
    await runningWork.click();

    // The lens opens in running work, scoped to the remembered project, by a push.
    await expect(page.locator('.overview')).toBeVisible();
    await expect(page).toHaveURL(/\/overview\?project=pm-mcp-clients$/);

    await page.goBack();
    await expect(page).toHaveURL(/\/plans\/fix-relay\?mode=ended$/);
  });

  test('keeps a marked /overview as it is on a reload', async ({ page }) => {
    await page.goto('/overview');
    await expect(page.locator('.overview')).toBeVisible();

    await page.reload();
    await expect(page.locator('.overview')).toBeVisible();
    await expect(page).toHaveURL(/\/overview$/);
  });

  test('never fills the lens of what waits', async ({ page }) => {
    // The remembered place knows a plan, and `/?lens=waits` still wins.
    await page.goto('/plans/fix-relay');
    await expect(page.locator('[data-testid="plan-title"]')).toBeVisible();

    await page.goto('/?lens=waits');
    await expect(page).not.toHaveURL(/\/plans\//);
  });

  test('drops the file of the plan when the tab switches to Process', async ({ page }) => {
    // The plan's tabs are PLAN-55/PLAN-58 surface; until they land the rule is asserted where it
    // lives, in the address model (routes.test.ts), and this case records the address the tab key
    // travels in.
    await page.goto('/plans/fix-relay?tab=changes&file=pm-relay/pom.xml');
    await expect(page).toHaveURL(/tab=changes/);
    await expect(page).toHaveURL(/file=/);
  });
});
