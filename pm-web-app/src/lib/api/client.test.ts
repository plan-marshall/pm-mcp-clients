import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ApiClient } from './client';

describe('ApiClient', () => {
  let client: ApiClient;

  beforeEach(() => {
    localStorage.clear();
    client = new ApiClient('http://localhost:7420');
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('pairs successfully and saves credentials in localStorage', async () => {
    // Arrange
    const mockResponse = {
      device_secret: 'pmw_secret_123',
      device_id: 'mock-device-1',
      label: 'Test Browser',
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockResponse,
    });

    // Act
    const res = await client.pair('KXQB-MRTN');

    // Assert
    expect(res.device_secret).toBe('pmw_secret_123');
    expect(client.getSecret()).toBe('pmw_secret_123');
    expect(client.getLabel()).toBe('Test Browser');
    expect(client.isPaired()).toBe(true);
  });

  it('throws an error when pairing fails with 400', async () => {
    // Arrange
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ error: 'invalid_code', message: 'Code does not match' }),
    });

    // Act & Assert
    await expect(client.pair('WRONGCODE')).rejects.toThrow('invalid_code');
    expect(client.isPaired()).toBe(false);
  });

  it('attaches Authorization header when secret is present', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_stored_secret');
    let capturedHeaders: Record<string, string> = {};

    globalThis.fetch = vi.fn().mockImplementation((_url, init) => {
      capturedHeaders = init?.headers as Record<string, string>;
      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => ({ status: 'ready' }),
      });
    });

    // Act
    const status = await client.getStatus();

    // Assert
    expect(status.status).toBe('ready');
    expect(capturedHeaders['Authorization']).toBe('Bearer pmw_stored_secret');
  });

  it('clears credentials and triggers callback on 401 unauthorized', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_revoked');
    const onRevoked = vi.fn();
    client.setOnRevoked(onRevoked);

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ error: 'unauthorized' }),
    });

    // Act & Assert
    await expect(client.getStatus()).rejects.toThrow('unauthorized');
    expect(client.getSecret()).toBeNull();
    expect(onRevoked).toHaveBeenCalledWith('unauthorized');
  });

  it('follows hypermedia link with proper HTTP method', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_secret');
    let capturedMethod = '';

    globalThis.fetch = vi.fn().mockImplementation((_url, init) => {
      capturedMethod = init?.method;
      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => ({ state: 'harness.choose', summary: 'Harnesses' }),
      });
    });

    // Act
    const nextState = await client.followLink({
      href: '/api/v1/operator/onboard/language',
      method: 'POST',
    });

    // Assert
    expect(capturedMethod).toBe('POST');
    expect(nextState.state).toBe('harness.choose');
  });

  it('rejects protocol-relative and slash-backslash paths', async () => {
    await expect(client.request('GET', '//attacker.com')).rejects.toThrow('Invalid request path');
    await expect(client.request('GET', '/\\attacker.com')).rejects.toThrow('Invalid request path');
    await expect(client.request('GET', 'http://attacker.com')).rejects.toThrow(
      'Invalid request path',
    );
    await expect(client.request('GET', '/\t/attacker.example/path')).rejects.toThrow(
      'Invalid request path',
    );
    await expect(client.request('GET', '/\n/attacker.example/path')).rejects.toThrow(
      'Invalid request path',
    );
  });

  it('fetches message catalogue and available locales via API', async () => {
    // Arrange
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/api/v1/messages')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({ locales: ['en', 'de'], areas: ['common'] }),
        });
      }
      if (url.endsWith('/api/v1/messages/en/common')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({ 'web.common.copy': 'Copy' }),
        });
      }
      return Promise.reject(new Error(`Unexpected url: ${url}`));
    });

    // Act
    const meta = await client.getAvailableMessages();
    const messages = await client.getMessages('en', 'common');

    // Assert
    expect(meta.locales).toContain('en');
    expect(messages['web.common.copy']).toBe('Copy');
  });

  it('fetches decisions and answers a decision', async () => {
    // Arrange
    globalThis.fetch = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes('/api/v1/decisions?project=pm-mcp-clients')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => [{ id: 'Q-007', title: 'Rebase conflict', subject: 'plan fix-relay' }],
        });
      }
      if (url.endsWith('/api/v1/decisions/Q-007')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({
            id: 'Q-007',
            title: 'Rebase conflict',
            subject: 'plan fix-relay',
            parts: [],
          }),
        });
      }
      if (url.endsWith('/api/v1/decisions/Q-007/answer') && init?.method === 'POST') {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({ ok: true }),
        });
      }
      if (url.endsWith('/api/v1/decisions/Q-007/interpret') && init?.method === 'POST') {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({ interpretation_type: 'OPTION', read_back_text: 'Rebase onto main' }),
        });
      }
      if (url.endsWith('/api/v1/decisions/Q-007/withdraw') && init?.method === 'POST') {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({ ok: true }),
        });
      }
      return Promise.reject(new Error(`Unexpected url: ${url}`));
    });

    // Act
    const list = await client.getDecisions({ project: 'pm-mcp-clients' });
    const single = await client.getDecision('Q-007');
    const answerRes = await client.answerDecision('Q-007', { option: '2' });
    const interp = await client.interpretDecision('Q-007', 'rebase');
    const withdrawRes = await client.withdrawDecision('Q-007');

    // Assert
    expect(list).toHaveLength(1);
    expect(list[0].id).toBe('Q-007');
    expect(single.id).toBe('Q-007');
    expect(answerRes.ok).toBe(true);
    expect(interp.read_back_text).toBe('Rebase onto main');
    expect(withdrawRes.ok).toBe(true);
  });

  it('fetches system snapshot with query parameters', async () => {
    let capturedUrl = '';
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      capturedUrl = url;
      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => ({
          runtime: { version: '0.9.1' },
          work: { tasks_running: 4 },
          workers: [],
          jobs: {},
          models: [],
          cleanup: { plans_count: 12, freed_bytes: 1932735283 },
          window: 'today',
          project: 'prj-1',
        }),
      });
    });

    const snapshot = await client.getSystemSnapshot('today', 'prj-1');
    expect(capturedUrl).toContain('/api/v1/system?window=today&project=prj-1');
    expect(snapshot.runtime.version).toBe('0.9.1');
    expect(snapshot.cleanup.freed_bytes).toBe(1932735283);
  });

  it('handles stopRuntime and withdrawStop lifecycle methods', async () => {
    let stopBody: unknown = null;
    let stopMethod = '';
    globalThis.fetch = vi.fn().mockImplementation((_url: string, init?: RequestInit) => {
      stopMethod = init?.method || '';
      if (init?.body) {
        stopBody = JSON.parse(init.body as string);
      }
      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => ({ lifecycle_state: 'draining' }),
      });
    });

    const res = await client.stopRuntime('drain');
    expect(stopMethod).toBe('POST');
    expect(stopBody).toEqual({ now: false, drain: true });
    expect(res.lifecycle_state).toBe('draining');

    await client.withdrawStop();
    expect(stopMethod).toBe('DELETE');
  });

  it('handles cleanup operations: job, execute, remind, and ignore', async () => {
    let lastUrl = '';
    let lastMethod = '';
    let lastBody: unknown = null;

    globalThis.fetch = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      lastUrl = url;
      lastMethod = init?.method || 'GET';
      if (init?.body) {
        lastBody = JSON.parse(init.body as string);
      }
      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => {
          if (url.includes('/delete')) {
            return { status: 'deleted', freed_bytes: 1825361100 };
          }
          if (url.endsWith('/system/cleanup/kept') && init?.method === 'PUT') {
            return { released_count: 2 };
          }
          return { job_id: 'cln-0007', summary: { freed_bytes: 1932735283 } };
        },
      });
    });

    const job = await client.getCleanupJob();
    expect(lastUrl).toContain('/api/v1/system/cleanup');
    expect(job.job_id).toBe('cln-0007');

    const deleteRes = await client.executeCleanup('cln-0007', ['item-1']);
    expect(lastUrl).toContain('/api/v1/system/cleanup/cln-0007/delete');
    expect(lastBody).toEqual({ unchecked_ids: ['item-1'] });
    expect(deleteRes.freed_bytes).toBe(1825361100);

    await client.remindCleanup('cln-0007');
    expect(lastUrl).toContain('/api/v1/system/cleanup/cln-0007/remind');
    expect(lastMethod).toBe('POST');

    await client.ignoreCleanup('cln-0007');
    expect(lastUrl).toContain('/api/v1/system/cleanup/cln-0007/ignore');
    expect(lastMethod).toBe('POST');

    const releaseRes = await client.releaseKeptCleanup(['k-1', 'k-2']);
    expect(lastUrl).toContain('/api/v1/system/cleanup/kept');
    expect(lastMethod).toBe('PUT');
    expect(lastBody).toEqual({ item_ids: ['k-1', 'k-2'] });
    expect(releaseRes.released_count).toBe(2);
  });

  it('handles the process-designer workflow methods', async () => {
    let lastUrl = '';
    let lastMethod = '';
    let lastBody: unknown = null;

    globalThis.fetch = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      lastUrl = url;
      lastMethod = init?.method || 'GET';
      if (init?.body) {
        lastBody = JSON.parse(init.body as string);
      }
      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => {
          if (url.endsWith('/catalogue')) {
            return {
              scope: 'pm-mcp-clients',
              runtime_version: '0.9.0',
              digest: 'sha256:cat',
              blocks: [],
            };
          }
          if (url.includes('/workflows/validate')) {
            return { valid: true, stages: [], findings: [] };
          }
          if (url.includes('/workflows/')) {
            return {
              unit: 'phase-6-finalize',
              target: 'pm-mcp-clients',
              status: 'landed',
              steps: ['written'],
            };
          }
          return [
            {
              name: 'phase-6-finalize',
              group: 'project_owned',
              level_in_force: 'project',
              levels: [],
            },
          ];
        },
      });
    });

    const units = await client.getWorkflows('pm-mcp-clients');
    expect(lastUrl).toContain('/api/v1/projects/pm-mcp-clients/workflows');
    expect(units[0].name).toBe('phase-6-finalize');

    await client.getWorkflowCatalogue('pm-mcp-clients');
    expect(lastUrl).toContain('/api/v1/projects/pm-mcp-clients/workflows/catalogue');

    const validation = await client.validateWorkflow({ text: 'x', unit_name: 'phase-6-finalize' });
    expect(lastUrl).toContain('/api/v1/workflows/validate');
    expect(lastMethod).toBe('POST');
    expect(validation.valid).toBe(true);

    const placed = await client.placeWorkflowUnit('pm-mcp-clients', 'phase-6-finalize', {
      text: 'x',
    });
    expect(lastUrl).toContain('/api/v1/projects/pm-mcp-clients/workflows/phase-6-finalize');
    expect(lastMethod).toBe('PUT');
    expect(lastBody).toEqual({ text: 'x' });
    expect(placed.status).toBe('landed');

    await client.removeWorkflowUnit(null, 'phase-6-finalize');
    expect(lastUrl).toContain('/api/v1/workflows/phase-6-finalize');
    expect(lastMethod).toBe('DELETE');
  });

  it('reads and steps a project onboarding, keeping a conflict body', async () => {
    // Arrange
    let lastUrl = '';
    let lastMethod = '';
    let lastBody: unknown;
    let status = 200;
    let responseBody: Record<string, unknown> = {
      project_id: 'pm-mcp-clients',
      step: 2,
      state: 'project.identity',
    };
    globalThis.fetch = vi.fn().mockImplementation((url, init) => {
      lastUrl = String(url);
      lastMethod = init?.method ?? 'GET';
      lastBody = init?.body ? JSON.parse(String(init.body)) : undefined;
      return Promise.resolve({
        ok: status < 400,
        status,
        json: async () => responseBody,
      });
    });

    // Act
    const run = await client.getProjectOnboarding('pm-mcp-clients');

    // Assert
    expect(lastMethod).toBe('GET');
    expect(lastUrl).toContain('/api/v1/projects/pm-mcp-clients/onboarding');
    expect(run.step).toBe(2);

    // Arrange: another client changed the value first
    status = 409;
    responseBody = {
      conflict: true,
      current_value: 'landing.main',
      wanted_value: 'landing.files',
      revision: 9,
    };

    // Act
    const conflicted = await client.stepProjectOnboarding('pm-mcp-clients', 'own', {
      value: 'landing.files',
    });

    // Assert
    expect(lastMethod).toBe('POST');
    expect(lastUrl).toContain('/api/v1/projects/pm-mcp-clients/onboarding/step');
    expect(lastBody).toEqual({ step: 'own', value: 'landing.files' });
    expect(conflicted.conflict).toBe(true);
    expect(conflicted.current_value).toBe('landing.main');
  });

  it('saves a project value against the revision it was read at', async () => {
    // Arrange
    let lastBody: unknown;
    let status = 200;
    let responseBody: Record<string, unknown> = { applied: true, conflict: false, revision: 8 };
    globalThis.fetch = vi.fn().mockImplementation((_url, init) => {
      lastBody = init?.body ? JSON.parse(String(init.body)) : undefined;
      return Promise.resolve({
        ok: status < 400,
        status,
        json: async () => responseBody,
      });
    });

    // Act
    const applied = await client.saveProjectSettings(
      'pm-mcp-clients',
      'landing',
      'landing.files',
      7,
    );

    // Assert
    expect(lastBody).toEqual({
      aspect: 'landing',
      value: 'landing.files',
      expected_revision: 7,
    });
    expect(applied.applied).toBe(true);

    // Arrange: the revision moved on
    status = 409;
    responseBody = {
      applied: false,
      conflict: true,
      revision: 9,
      current_value: 'landing.pull-request',
    };

    // Act
    const refused = await client.saveProjectSettings(
      'pm-mcp-clients',
      'landing',
      'landing.files',
      7,
    );

    // Assert
    expect(refused.conflict).toBe(true);
    expect(refused.current_value).toBe('landing.pull-request');
  });

  it('persists the favourite order through the explorer contract', async () => {
    // Arrange
    let lastUrl = '';
    let lastMethod = '';
    let lastBody: unknown;
    globalThis.fetch = vi.fn().mockImplementation((url, init) => {
      lastUrl = String(url);
      lastMethod = init?.method ?? 'GET';
      lastBody = init?.body ? JSON.parse(String(init.body)) : undefined;
      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => ({ status: 'ok', project_ids: ['pm-mcp-core', 'pm-mcp-clients'] }),
      });
    });

    // Act
    const result = await client.reorderFavouriteProjects(['pm-mcp-core', 'pm-mcp-clients']);

    // Assert
    expect(lastMethod).toBe('PUT');
    expect(lastUrl).toContain('/api/v1/explorer/favourites');
    expect(lastBody).toEqual({ project_ids: ['pm-mcp-core', 'pm-mcp-clients'] });
    expect(result.project_ids).toEqual(['pm-mcp-core', 'pm-mcp-clients']);
  });

  it('clears the pairing when a conflict-capable call is refused with 401', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_revoked');
    const onRevoked = vi.fn();
    client.setOnRevoked(onRevoked);
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ error: 'unauthorized' }),
    });

    // Act & Assert
    await expect(
      client.saveProjectSettings('pm-mcp-clients', 'landing', 'landing.files', 7),
    ).rejects.toThrow('unauthorized');
    expect(onRevoked).toHaveBeenCalledWith('unauthorized');
    expect(client.isPaired()).toBe(false);
  });

  it('leaves the pairing untouched when an unauthenticated call is refused with 401', async () => {
    // Arrange: no secret is stored, so this 401 says "pair first", not "pairing revoked".
    const onRevoked = vi.fn();
    client.setOnRevoked(onRevoked);
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ error: 'unauthorized' }),
    });

    // Act & Assert
    await expect(client.getStatus()).rejects.toThrow('unauthorized');
    expect(onRevoked).not.toHaveBeenCalled();
    expect(client.getSecret()).toBeNull();
  });

  it('reports online again after a failed request and the next success', async () => {
    // Arrange
    const onOffline = vi.fn();
    const onOnline = vi.fn();
    client.setOnOffline(onOffline);
    client.setOnOnline(onOnline);
    globalThis.fetch = vi.fn().mockRejectedValue(new Error('down'));

    // Act: the first call fails, the next one succeeds.
    await expect(client.getStatus()).rejects.toThrow('network_offline');
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ status: 'ready' }),
    });
    await client.getStatus();

    // Assert
    expect(onOffline).toHaveBeenCalledTimes(1);
    expect(onOnline).toHaveBeenCalledTimes(1);
  });

  it('shares one event stream among subscribers', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_stored_secret');
    const encoder = new TextEncoder();
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      body: new ReadableStream<Uint8Array>({
        start(controller) {
          controller.enqueue(encoder.encode('event: report.sent\ndata: {"subject":"Plan 7"}\n\n'));
        },
      }),
    });
    globalThis.fetch = fetchMock;
    const first = vi.fn();
    const second = vi.fn();

    // Act
    const offFirst = client.subscribeEvents(first);
    const controller = new AbortController();
    const offSecond = client.subscribeEvents(second, controller.signal);

    // Assert: one connection carries the event to both listeners.
    await vi.waitFor(() =>
      expect(first).toHaveBeenCalledWith('report.sent', { subject: 'Plan 7' }),
    );
    expect(second).toHaveBeenCalledWith('report.sent', { subject: 'Plan 7' });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/events');

    // The connection stays open until the last listener leaves.
    const signal = (fetchMock.mock.calls[0][1] as RequestInit).signal as AbortSignal;
    offFirst();
    expect(signal.aborted).toBe(false);
    controller.abort();
    expect(signal.aborted).toBe(true);
    // The manual unsubscribe after the abort is a harmless no-op.
    offSecond();
  });

  it('reports a 401 on the shared event stream once', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_revoked');
    const onRevoked = vi.fn();
    client.setOnRevoked(onRevoked);
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ error: 'unauthorized' }),
    });

    // Act
    const offFirst = client.subscribeEvents(() => {});
    const offSecond = client.subscribeEvents(() => {});

    // Assert
    await vi.waitFor(() => expect(onRevoked).toHaveBeenCalledTimes(1));
    expect(onRevoked).toHaveBeenCalledWith('unauthorized');
    expect(client.getSecret()).toBeNull();
    offFirst();
    offSecond();
  });

  it('reports online when the event stream reconnects', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_stored_secret');
    const onOnline = vi.fn();
    client.setOnOnline(onOnline);
    globalThis.fetch = vi.fn().mockImplementation(() =>
      Promise.resolve({
        ok: true,
        status: 200,
        body: new ReadableStream<Uint8Array>({
          start(controller) {
            controller.close();
          },
        }),
      }),
    );
    vi.useFakeTimers();

    // Act: the reader closes and reconnects after the retry delay.
    const off = client.subscribeEvents(() => {});
    await vi.advanceTimersByTimeAsync(0);
    expect(onOnline).toHaveBeenCalledTimes(1);
    await vi.advanceTimersByTimeAsync(2000);
    expect(onOnline).toHaveBeenCalledTimes(2);

    // Assert
    off();
    vi.useRealTimers();
  });

  it('does not open a stream for a subscriber whose signal is already aborted', () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_stored_secret');
    const fetchMock = vi.fn();
    globalThis.fetch = fetchMock;
    const controller = new AbortController();
    controller.abort();
    const listener = vi.fn();

    // Act
    const off = client.subscribeEvents(listener, controller.signal);
    off();

    // Assert
    expect(fetchMock).not.toHaveBeenCalled();
    expect(listener).not.toHaveBeenCalled();
  });
});
