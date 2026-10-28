import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

// Controlling work (UC-CTL-1..5): the mock runtime's contract for a plan's and an epic's actions,
// its holds, and a plan's settings, and the web app's own views of them.
const BASE = MOCK_RUNTIME_URL;

test.describe('Controlling work (UC-CTL-1..5)', () => {
  let secret;

  test.beforeAll(async ({ request }) => {
    const scenario = await request.post(`${BASE}/mock/scenario`, {
      data: { scenario: 'control' },
    });
    expect(scenario.ok()).toBeTruthy();

    const pair = await request.post(`${BASE}/api/v1/pair`, { data: { code: 'KXQB-MRTN' } });
    expect(pair.ok()).toBeTruthy();
    secret = (await pair.json()).device_secret;
  });

  const auth = () => ({ Authorization: `Bearer ${secret}` });

  async function pairBrowser(page, request) {
    const scenario = await request.post(`${BASE}/mock/scenario`, {
      data: { scenario: 'control' },
    });
    expect(scenario.ok()).toBeTruthy();
    const reset = await request.post(`${BASE}/mock/reset`, { data: {} });
    expect(reset.ok()).toBeTruthy();
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:', {
      timeout: 30000,
    });
  }

  test('serves the actions of a running plan with their availability', async ({ request }) => {
    const res = await request.get(`${BASE}/api/v1/plans/fix-relay/actions`, { headers: auth() });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.subject_id).toBe('fix-relay');
    expect(body.state).toBe('running');
    expect(body.actions.map((entry) => entry.action)).toEqual(
      expect.arrayContaining(['pause', 'cancel', 'chat', 'settings']),
    );
  });

  test('pauses a plan at its point and starts it again', async ({ request }) => {
    const pause = await request.post(`${BASE}/api/v1/plans/relay-auth-v2/actions/pause`, {
      headers: auth(),
      data: { point: 'phase' },
    });
    expect(pause.ok()).toBeTruthy();
    const paused = await pause.json();
    expect(paused.state).toBe('pausing');
    expect(paused.pausing_point).toBe('phase');

    const start = await request.post(`${BASE}/api/v1/plans/relay-auth-v2/actions/start`, {
      headers: auth(),
      data: {},
    });
    expect((await start.json()).state).toBe('running');
  });

  test('shows an integrity pause and releases it only on an accept', async ({ request }) => {
    const list = await request.get(`${BASE}/api/v1/plans/relay-cursor-order/holds`, {
      headers: auth(),
    });
    expect(list.ok()).toBeTruthy();
    const holds = await list.json();
    expect(holds).toHaveLength(1);
    expect(holds[0].kind).toBe('integrity_pause');
    expect(holds[0].file_path).toBe('references.json');

    const retry = await request.post(
      `${BASE}/api/v1/plans/relay-cursor-order/holds/H-0001/resolve`,
      { headers: auth(), data: { act: 'retry' } },
    );
    expect((await retry.json()).released).toBe(false);

    const accept = await request.post(
      `${BASE}/api/v1/plans/relay-cursor-order/holds/H-0001/resolve`,
      { headers: auth(), data: { act: 'accept' } },
    );
    expect((await accept.json()).released).toBe(true);
  });

  test('serves an epic archive disabled while the ledger lands and toggles automatic', async ({
    request,
  }) => {
    const landing = await request.get(`${BASE}/api/v1/epics/relay-docs-epic/actions`, {
      headers: auth(),
    });
    const landingBody = await landing.json();
    const archive = landingBody.actions.find((entry) => entry.action === 'archive');
    expect(archive.available).toBe(false);
    expect(archive.reason).toBe('the ledger is landing');

    const auto = await request.post(`${BASE}/api/v1/epics/relay-hardening/actions/auto`, {
      headers: auth(),
      data: { enabled: false },
    });
    expect((await auto.json()).auto_enabled).toBe(false);
  });

  test('guards a plan settings change against a stale revision', async ({ request }) => {
    const currentRes = await request.get(`${BASE}/api/v1/plans/fix-relay/settings`, {
      headers: auth(),
    });
    const settings = await currentRes.json();
    expect(settings.revision).toBe(7);

    const stale = await request.put(`${BASE}/api/v1/plans/fix-relay/settings`, {
      headers: auth(),
      data: { aspect: 'method', value: 'method.tests-after', revision: 6 },
    });
    expect(stale.status()).toBe(409);
    expect((await stale.json()).conflict).toBe(true);

    const applied = await request.put(`${BASE}/api/v1/plans/fix-relay/settings`, {
      headers: auth(),
      data: { aspect: 'method', value: 'method.tests-after', revision: 7 },
    });
    expect(applied.ok()).toBeTruthy();
    expect((await applied.json()).applied).toBe(true);
  });

  test('shows a running plan’s actions, its pause dialogue, and the point it holds at', async ({
    page,
    request,
  }) => {
    await pairBrowser(page, request);
    await page.goto('/plans/fix-relay');

    await expect(page.getByTestId('plan-title')).toHaveText('Fix Relay Transport');
    await expect(page.getByTestId('action-pause')).toBeVisible();
    await expect(page.getByTestId('action-cancel')).toBeVisible();
    await expect(page.getByTestId('action-settings')).toBeVisible();

    await page.getByTestId('action-pause').click();
    await expect(page.getByTestId('pause-title')).toContainText('Pause fix-relay?');
    await page.getByLabel('After the current phase').check();
    await page.getByTestId('pause-confirm').click();

    await expect(page.getByTestId('action-start')).toBeVisible({ timeout: 5000 });
  });

  test('shows what ends before a cancel and asks for the reason', async ({ page, request }) => {
    await pairBrowser(page, request);
    await page.goto('/plans/fix-relay');

    await page.getByTestId('action-cancel').click();
    await expect(page.getByTestId('end-title')).toContainText('Cancel fix-relay?');
    await expect(page.getByTestId('end-confirm')).toBeDisabled();

    await page.getByTestId('end-reason').fill('replaced by plan relay-v2');
    await expect(page.getByTestId('end-confirm')).toBeEnabled();
    await page.getByTestId('end-confirm').click();
    await expect(page.getByTestId('action-pause')).toHaveCount(0, { timeout: 5000 });
  });

  test('resolves the integrity pause of a held plan, and shows what still holds', async ({
    page,
    request,
  }) => {
    await pairBrowser(page, request);
    await page.goto('/plans/relay-cursor-order');

    await expect(page.getByTestId('hold-modal')).toBeVisible();
    await expect(page.getByTestId('hold-title')).toContainText('integrity pause');
    await expect(page.getByTestId('hold-file')).toContainText('references.json');

    await page.getByTestId('hold-act-retry').click();
    await expect(page.getByTestId('hold-still-held')).toContainText('Still held', {
      timeout: 5000,
    });
  });

  test('offers a plan’s settings and reports a refused value', async ({ page, request }) => {
    await pairBrowser(page, request);
    await page.goto('/plans/fix-relay');

    await page.getByTestId('action-settings').click();
    await expect(page.getByTestId('settings-modal')).toBeVisible();
    await expect(page.getByTestId('settings-origin-landing')).toContainText('from the project');

    await page.getByTestId('settings-value-landing').selectOption('landing.main');
    await page.getByTestId('settings-apply-landing').click();

    await expect(page.getByTestId('settings-notice')).toContainText('is refused', {
      timeout: 5000,
    });
  });

  test('shows an epic’s workstreams, registers, tokens, events, and actions', async ({
    page,
    request,
  }) => {
    await pairBrowser(page, request);
    await page.goto('/epics/relay-hardening');

    await expect(page.getByTestId('epic-workstreams')).toBeVisible();
    await expect(page.getByTestId('workstream-WS-1')).toContainText('Reconnect');
    await expect(page.getByTestId('epic-row-relay-cursor-order')).toBeVisible();
    await expect(page.getByTestId('epic-sources')).toContainText('2 documents');
    await expect(page.getByTestId('epic-landing-count')).toHaveText('1 landing');
    await expect(page.getByTestId('epic-findings')).toHaveText(/0 new · \d+ open · \d+ kept/);
    await expect(page.getByTestId('epic-watches')).toContainText('4 open');
    await expect(page.getByTestId('epic-own-tokens')).toBeVisible();
    await expect(page.getByTestId('epic-events')).toContainText('relay-auth-v2 asks a decision');
    await expect(page.getByTestId('epic-mode')).toContainText('Automatic: on');

    await expect(page.getByTestId('action-next')).toBeVisible();
    await expect(page.getByTestId('action-auto')).toContainText('Automatic: on');
    await expect(page.getByTestId('action-archive')).toBeVisible();
  });

  test('toggles an epic’s automatic mode with the value it sets', async ({ page, request }) => {
    await pairBrowser(page, request);
    await page.goto('/epics/relay-hardening');

    await expect(page.getByTestId('action-auto')).toContainText('Automatic: on');
    await page.getByTestId('action-auto').click();

    await expect(page.getByTestId('control-notice')).toContainText('starts no plan by itself', {
      timeout: 5000,
    });
    await expect(page.getByTestId('action-auto')).toContainText('Automatic: off');
  });

  test('shows the archive check and the open finding that holds it', async ({ page, request }) => {
    await pairBrowser(page, request);
    await page.goto('/epics/relay-hardening');

    await page.getByTestId('action-archive').click();

    await expect(page.getByTestId('archive-title')).toContainText('Archive relay-hardening?');
    await expect(page.getByTestId('archive-line-control.archive.open_findings')).toBeVisible();
    await expect(page.getByTestId('archive-carried')).toContainText('3 kept findings');
    await expect(page.getByTestId('archive-confirm')).toBeDisabled();
  });

  test('opens the findings review with every disposition', async ({ page, request }) => {
    await pairBrowser(page, request);
    await page.goto('/epics/relay-hardening');

    await page.getByTestId('open-findings-review').click();

    await expect(page.getByTestId('review-modal')).toBeVisible();
    await expect(page.getByTestId('review-title')).toContainText('findings review');
    await expect(page.getByTestId('review-item-F-0004')).toContainText(
      'Reconnect loses the last event',
    );
    await page.getByTestId('review-item-F-0005').click();
    await expect(page.getByTestId('review-untrusted')).toBeVisible();
  });

  test('opens a plan’s actions from its row in Running Work', async ({ page, request }) => {
    await pairBrowser(page, request);
    await page.getByRole('button', { name: /Running work/i }).click();

    await page.getByTestId('overview-actions-fix-relay').click();

    await expect(page.getByTestId('overview-control')).toBeVisible();
    await expect(page.getByTestId('action-pause')).toBeVisible();
  });
});
