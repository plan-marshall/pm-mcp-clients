import { test, expect } from '@playwright/test';

// Controlling work (UC-CTL-1..5): the mock runtime's contract for a plan's and an epic's actions,
// its holds, and a plan's settings. The browser drives the same endpoints through the web client.
const BASE = 'http://127.0.0.1:7420';

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
    const list = await request.get(`${BASE}/api/v1/plans/relay-cursor-order/holds`, { headers: auth() });
    expect(list.ok()).toBeTruthy();
    const holds = await list.json();
    expect(holds).toHaveLength(1);
    expect(holds[0].kind).toBe('integrity_pause');
    expect(holds[0].file_path).toBe('references.json');

    const retry = await request.post(`${BASE}/api/v1/plans/relay-cursor-order/holds/H-0001/resolve`, {
      headers: auth(),
      data: { act: 'retry' },
    });
    expect((await retry.json()).released).toBe(false);

    const accept = await request.post(`${BASE}/api/v1/plans/relay-cursor-order/holds/H-0001/resolve`, {
      headers: auth(),
      data: { act: 'accept' },
    });
    expect((await accept.json()).released).toBe(true);
  });

  test('serves an epic archive disabled while the ledger lands and toggles automatic', async ({ request }) => {
    const landing = await request.get(`${BASE}/api/v1/epics/relay-docs-epic/actions`, { headers: auth() });
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
    const currentRes = await request.get(`${BASE}/api/v1/plans/fix-relay/settings`, { headers: auth() });
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
});
