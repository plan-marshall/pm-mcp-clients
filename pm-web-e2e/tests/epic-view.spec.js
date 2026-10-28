import { test, expect } from '@playwright/test';

/**
 * The epic view, UC-PLN-5: the whole epic at a glance — its head, workstreams and members, sources,
 * landings, findings and watches, tokens, and events — read from `GET /api/v1/epics/{id}` and
 * refreshed by the runtime's events. Both canonical scenarios carry the epic: `running-work` as the
 * overview's working case, `fully-onboarded` as the showroom's.
 */

const MOCK = 'http://127.0.0.1:7420';
const PAIRING_CODE = 'KXQB-MRTN';

/**
 * Switches the mock runtime to a scenario and pairs the browser with it.
 *
 * @param page the page
 * @param request the API request context
 * @param scenario the scenario's name
 */
async function pair(page, request, scenario) {
  const switchRes = await request.post(`${MOCK}/mock/scenario`, { data: { scenario } });
  expect(switchRes.ok()).toBeTruthy();

  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto(`/#code=${PAIRING_CODE}`);
  await expect(page.locator('.app-header__machine')).toContainText('Machine:');
}

/**
 * Opens Running Work and, from there, the epic's view.
 *
 * @param page the page
 * @param title the epic's title as the overview writes it
 */
async function openEpicFromOverview(page, title = 'Relay Hardening') {
  await page
    .locator('.app-header')
    .getByRole('button', { name: /Running work/i })
    .click();
  await expect(page.locator('.overview')).toBeVisible();

  await page.getByRole('radio', { name: /by epic/i }).click();
  const card = page.getByTestId('epic-card').filter({ hasText: title });
  await expect(card).toBeVisible();
  await card.click();

  await expect(page.getByTestId('epic-view')).toBeVisible();
}

test.describe('Epic View (UC-PLN-5)', () => {
  test.afterAll(async ({ request }) => {
    try {
      await request.post(`${MOCK}/mock/scenario`, { data: { scenario: 'onboarding' } });
    } catch {
      // Best-effort cleanup, as the neighbouring suites do.
    }
  });

  test('opens from Running Work with the path, title, and head of the epic', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await expect(page.getByTestId('breadcrumb-project')).toHaveText('pm-mcp-clients');
    await expect(page.getByTestId('breadcrumb-epic')).toHaveText('relay-hardening');
    await expect(page.getByTestId('epic-title')).toHaveText('Relay Hardening');
    await expect(page.getByTestId('epic-state')).toHaveText('running');
    await expect(page.getByTestId('epic-mode')).toContainText('Automatic: on');
    await expect(page.getByTestId('epic-scope')).toContainText('of 3 at once');
    await expect(page.getByTestId('epic-next-plan')).toContainText(
      'Next plan: relay-reconnect-metrics',
    );
  });

  test('leads to the project in one step and back to the overview', async ({ page, request }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await page.getByTestId('breadcrumb-project').click();
    await expect(page).toHaveURL(/\?project=pm-mcp-clients/);
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();

    await page
      .locator('.app-header')
      .getByRole('button', { name: /Running work/i })
      .click();
    await expect(page.locator('.overview')).toBeVisible();
  });

  test('folds and unfolds a workstream and names what a staged plan waits for', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    const reconnect = page.getByTestId('workstream-WS-1');
    const auth = page.getByTestId('workstream-WS-2');
    await expect(reconnect).toContainText('WS-1');
    await expect(reconnect).toContainText('Reconnect');
    await expect(page.getByTestId('workstream-counts-WS-1')).toContainText(
      '1 shipped · 1 running · 1 paused · 1 staged',
    );
    await expect(auth).toContainText('Authentication');

    await expect(page.getByTestId('epic-row-relay-reconnect-backoff')).toContainText('shipped');
    await expect(page.getByTestId('epic-row-relay-auth-v2')).toContainText('waiting for you');
    await expect(page.getByTestId('wait-relay-token-rotation')).toContainText(
      'waits for relay-auth-v2',
    );
    await expect(page.getByTestId('wait-relay-docs')).toContainText(
      'waits: overlap with relay-auth-v2',
    );

    await page.getByTestId('workstream-toggle-WS-2').click();
    await expect(page.getByTestId('epic-row-relay-auth-v2')).toHaveCount(0);
    await expect(page.getByTestId('workstream-counts-WS-2')).toContainText('2 staged');

    await page.getByTestId('workstream-toggle-WS-2').click();
    await expect(page.getByTestId('epic-row-relay-auth-v2')).toBeVisible();
  });

  test('opens a plan in its view in one step and returns to the epic', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await page.getByTestId('epic-row-relay-reconnect-backoff').getByRole('button').click();
    await expect(page.getByTestId('plan-view')).toBeVisible();
    await expect(page.getByTestId('breadcrumb-plan')).toHaveText('relay-reconnect-backoff');

    await page.getByTestId('back-btn').click();
    await expect(page.getByTestId('epic-view')).toBeVisible();
    await expect(page.getByTestId('breadcrumb-epic')).toHaveText('relay-hardening');
  });

  test('shows the landings with their pull requests as external links', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await expect(page.getByTestId('epic-landing-count')).toHaveText('1 landing');

    const landing = page.getByTestId('epic-landing-relay-reconnect-backoff');
    await expect(landing).toContainText('relay-reconnect-backoff');
    const link = landing.getByRole('link', { name: '#211' });
    await expect(link).toHaveAttribute(
      'href',
      'https://github.com/plan-marshall/pm-mcp-clients/pull/211',
    );
    await expect(link).toHaveAttribute('target', '_blank');
    await expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  test('opens the findings and the watches review from the registers', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await expect(page.getByTestId('epic-findings')).toHaveText('0 new · 1 open · 1 kept');
    await expect(page.getByTestId('epic-watches')).toHaveText('4 open · 0 kept');

    await page.getByTestId('open-findings-review').click();
    await expect(page.getByTestId('review-modal')).toBeVisible();
    await expect(page.getByTestId('review-title')).toContainText('findings review');
    await expect(page.getByTestId('review-item-F-0004')).toContainText(
      'Reconnect loses the last event',
    );
    await page.getByTestId('review-close').click();
    await expect(page.getByTestId('review-modal')).toHaveCount(0);

    await page.getByTestId('open-watches-review').click();
    await expect(page.getByTestId('review-modal')).toBeVisible();
    await expect(page.getByTestId('review-title')).toContainText('watches review');
    await expect(page.getByTestId('review-item-W-0001')).toContainText(
      'The reconnect backoff grows without bound',
    );
  });

  test('writes the tokens as a floor with its coverage and the split per plan', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await expect(page.getByTestId('epic-tokens')).toContainText('≥ 3.86 M');
    await expect(page.getByTestId('epic-tokens')).toContainText('3.52 M in');
    await expect(page.getByTestId('epic-tokens')).toContainText('340 k out');
    await expect(page.getByTestId('epic-tokens-unmeasured')).toContainText('1 task not measured');
    await expect(page.getByTestId('epic-own-tokens')).toContainText('120 k');
    await expect(page.getByTestId('epic-plan-tokens-relay-reconnect-backoff')).toContainText(
      '1.40 M',
    );
  });

  test('adds a live event to the event list when the runtime broadcasts one', async ({
    page,
    request,
  }) => {
    // The mock serves a fixed resource, so the test grows the served body on the reload the
    // client makes after the event: what it proves is the client's own reaction.
    let reloaded = false;
    await page.route('**/api/v1/epics/relay-hardening', async (route) => {
      const response = await route.fetch();
      const body = await response.json();
      if (reloaded) {
        body.events = [
          { at: '2026-10-16T12:45:00Z', text: 'relay-reconnect-metrics started (3 of 3 at once)' },
          ...body.events,
        ];
      }
      await route.fulfill({ response, json: body });
    });

    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await expect(page.getByTestId('epic-events')).toContainText('relay-auth-v2 asks a decision');
    await expect(page.getByTestId('epic-events')).not.toContainText(
      'relay-reconnect-metrics started',
    );

    reloaded = true;
    const broadcast = await request.post(`${MOCK}/mock/broadcast`, {
      data: { event: 'epic.row_changed', data: { epic_id: 'relay-hardening' } },
    });
    expect(broadcast.ok()).toBeTruthy();

    await expect(page.getByTestId('epic-events')).toContainText(
      'relay-reconnect-metrics started (3 of 3 at once)',
    );
  });

  test('shows the showroom epic with its third workstream and its events', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'fully-onboarded');
    await openEpicFromOverview(page);

    await expect(page.getByTestId('workstream-WS-3')).toContainText('Observability');
    await expect(page.getByTestId('epic-row-relay-metrics')).toContainText('paused');
    await expect(page.getByTestId('epic-row-relay-metrics-docs')).toContainText('staged');
    await expect(page.getByTestId('epic-events')).toContainText('landed');
  });

  test('stays readable at 320px without a horizontal scrollbar', async ({ page, request }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await page.setViewportSize({ width: 320, height: 800 });
    await expect(page.getByTestId('epic-workstreams')).toBeVisible();
    await expect(page.getByTestId('epic-registers')).toBeVisible();
    await expect(page.getByTestId('epic-token-breakdown')).toBeVisible();

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(1);
  });
});
