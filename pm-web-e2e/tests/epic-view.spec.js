import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

/**
 * The epic page, UC-PLN-5 and UX-P-10: the epic as an object page — its head, the tokens line with
 * its plan-by-plan disclosure, and the five tabs of `epic-view.adoc#tabs` — with its plans grouped
 * by workstream, each row's title first, its id as a chip and what it waits for with since when.
 * Both canonical scenarios carry the epic: `running-work` as the overview's working case,
 * `fully-onboarded` as the showroom's.
 */

const MOCK = MOCK_RUNTIME_URL;
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
 * Opens Running Work and, from there, the epic's page.
 *
 * @param page the page
 * @param title the epic's title as the overview writes it
 */
async function openEpicFromOverview(page, title = 'Relay Hardening') {
  await page
    .locator('.nav-rail')
    .getByRole('button', { name: /Running work/i })
    .click();
  await expect(page.locator('.overview')).toBeVisible();

  await page.getByRole('radio', { name: /by epic/i }).click();
  const card = page.getByTestId('epic-card').filter({ hasText: title });
  await expect(card).toBeVisible();
  await card.click();

  await expect(page.getByTestId('epic-view')).toBeVisible();
}

test.describe('Epic page (UC-PLN-5, UX-P-10)', () => {
  test.afterAll(async ({ request }) => {
    try {
      await request.post(`${MOCK}/mock/scenario`, { data: { scenario: 'onboarding' } });
    } catch {
      // Best-effort cleanup, as the neighbouring suites do.
    }
  });

  test('opens from Running Work with the object page head and no path of its own', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await expect(page.getByTestId('object-page')).toBeVisible();
    await expect(page.getByTestId('object-page-kind')).toContainText('epic');
    await expect(page.getByTestId('object-page-title')).toHaveText('Relay Hardening');
    await expect(page.getByTestId('object-page-id')).toHaveText('relay-hardening');
    await expect(page.getByTestId('epic-state')).toContainText('running');
    await expect(page.getByTestId('epic-mode')).toContainText('Automatic: on');
    await expect(page.getByTestId('epic-scope')).toContainText('of 3 at once');
    await expect(page.getByTestId('epic-next-plan')).toContainText(
      'Next plan: relay-reconnect-metrics',
    );

    // The path is the top bar's, never the page's (`navigation.adoc#up`).
    await expect(page.getByTestId('epic-breadcrumb')).toHaveCount(0);
    await expect(page.getByTestId('back-btn')).toHaveCount(0);
  });

  test('draws the plans with the title first, the id as a chip and the wait with its since', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    const reconnect = page.getByTestId('workstream-WS-1');
    await expect(reconnect).toContainText('Reconnect');
    await expect(page.getByTestId('workstream-counts-WS-1')).toContainText(
      '1 shipped · 1 running · 1 paused · 1 staged',
    );

    // The title comes first, before the id chip and the state.
    const row = page.getByTestId('epic-row-relay-reconnect-backoff');
    await expect(row).toHaveText(/^\s*relay-reconnect-backoff/);
    await expect(page.getByTestId('epic-plan-chip-relay-reconnect-backoff')).toContainText(
      'relay-reconnect-backoff',
    );
    await expect(page.getByTestId('epic-plan-status-relay-reconnect-backoff')).toContainText(
      'shipped',
    );

    await expect(page.getByTestId('wait-relay-token-rotation')).toContainText(
      'waits for relay-auth-v2',
    );
    await expect(page.getByTestId('wait-relay-docs')).toContainText(
      'waits: overlap with relay-auth-v2',
    );
    // Each wait says since when, as the served instant and a relative figure.
    const since = page.getByTestId('wait-since-relay-docs');
    await expect(since).toHaveAttribute('datetime', '2026-10-16T09:40:00Z');
    await expect(since).not.toBeEmpty();

    await page.getByTestId('workstream-toggle-WS-2').click();
    await expect(page.getByTestId('epic-row-relay-auth-v2')).toHaveCount(0);
    await expect(page.getByTestId('workstream-counts-WS-2')).toContainText('2 staged');
    await page.getByTestId('workstream-toggle-WS-2').click();
    await expect(page.getByTestId('epic-row-relay-auth-v2')).toBeVisible();
  });

  test('shows the five tabs, Plans the default and each tab its own panel', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    const tabs = page.getByRole('tab');
    await expect(tabs).toHaveCount(5);
    await expect(tabs.nth(0)).toHaveText('Plans');
    await expect(tabs.nth(1)).toHaveText('Intent');
    await expect(tabs.nth(2)).toHaveText('Landings');
    await expect(tabs.nth(3)).toHaveText('Reviews');
    await expect(tabs.nth(4)).toHaveText('Events');
    await expect(page.getByTestId('epic-panel-plans')).toBeVisible();

    await page.getByRole('tab', { name: 'Intent' }).click();
    await expect(page.getByTestId('epic-panel-intent')).toBeVisible();
    await expect(page.getByTestId('epic-intent-sources')).toContainText('2 documents');

    await page.getByRole('tab', { name: 'Landings' }).click();
    await expect(page.getByTestId('epic-panel-landings')).toBeVisible();

    await page.getByRole('tab', { name: 'Reviews' }).click();
    await expect(page.getByTestId('epic-panel-reviews')).toBeVisible();

    await page.getByRole('tab', { name: 'Events' }).click();
    await expect(page.getByTestId('epic-panel-events')).toBeVisible();
    await expect(page.getByTestId('epic-events')).toContainText('relay-auth-v2 asks a decision');
  });

  test('writes the chosen tab into the address by a replace, and a reload keeps it', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await page.getByRole('tab', { name: 'Intent' }).click();

    await expect(page).toHaveURL(/\/epics\/relay-hardening\?.*tab=intent$/);
    await page.reload();
    await expect(page.getByTestId('epic-panel-intent')).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Intent' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
  });

  test('shows the landings with their pull requests and the analysis of the epic', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await page.getByRole('tab', { name: 'Landings' }).click();

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

  test('opens the findings review on its first open item, with the counts of the card', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await page.getByRole('tab', { name: 'Reviews' }).click();

    await expect(page.getByTestId('epic-findings')).toContainText('0 new · 1 open · 1 kept');
    await expect(page.getByTestId('epic-watches')).toContainText('4 open · 0 kept');

    await page.getByTestId('open-findings-review').click();
    await expect(page.getByTestId('review-modal')).toBeVisible();
    await expect(page.getByTestId('review-title')).toContainText('findings review');
    // F-0004 is the first item that is still open, and the panel opens on it.
    await expect(page.getByTestId('review-statement')).toContainText(
      'Reconnect loses the last event',
    );
    await expect(page.getByTestId('review-open-count')).toHaveText('1 open');
    await page.getByTestId('review-close').click();
    await expect(page.getByTestId('review-modal')).toHaveCount(0);

    await page.getByTestId('open-watches-review').click();
    await expect(page.getByTestId('review-title')).toContainText('watches review');
    await expect(page.getByTestId('review-item-W-0001')).toContainText(
      'The reconnect backoff grows without bound',
    );
  });

  test('writes the tokens as a floor with its coverage, and the split per plan in one step', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await expect(page.getByTestId('epic-tokens')).toContainText('≥ 3.86 M');
    await expect(page.getByTestId('epic-tokens')).toContainText('3.52 M in');
    await expect(page.getByTestId('epic-tokens')).toContainText('0.34 M out');
    await expect(page.getByTestId('epic-tokens-unmeasured')).toContainText('1 task not measured');

    await page.getByTestId('epic-tokens-disclosure').click();

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
    await page.getByRole('tab', { name: 'Events' }).click();

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

  test('shows the epic as a list column with only its plans while one of them is open beside it', async ({
    page,
    request,
  }) => {
    await pair(page, request, 'running-work');
    await page.goto('/epics/relay-hardening?open=plan:relay-cursor-order');

    await expect(page.getByTestId('epic-list-column')).toBeVisible();
    await expect(page.getByTestId('workstream-WS-1')).toContainText('Reconnect');
    await expect(page.getByTestId('epic-row-relay-cursor-order')).toBeVisible();
    // Nothing of the other tabs stands in the list column.
    await expect(page.getByRole('tab')).toHaveCount(0);
    await expect(page.getByTestId('epic-tokens')).toHaveCount(0);
  });

  test('leads to a plan in one step and back to the epic', async ({ page, request }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await page.getByTestId('epic-open-relay-reconnect-backoff').click();
    await expect(page.getByTestId('plan-view')).toBeVisible();

    // The epic's head draws no Back (D6): the plan's own path leads back to its epic.
    await page.locator('[data-testid="breadcrumb-epic"]').click();
    await expect(page.getByTestId('epic-view')).toBeVisible();
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

    await page.getByRole('tab', { name: 'Events' }).click();
    await expect(page.getByTestId('epic-events')).toContainText('landed');
  });

  test('stays readable at 320px without a horizontal scrollbar', async ({ page, request }) => {
    await pair(page, request, 'running-work');
    await openEpicFromOverview(page);

    await page.setViewportSize({ width: 320, height: 800 });
    await expect(page.getByTestId('epic-workstreams')).toBeVisible();

    await page.getByRole('tab', { name: 'Reviews' }).click();
    await expect(page.getByTestId('epic-registers')).toBeVisible();

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(1);
  });
});
