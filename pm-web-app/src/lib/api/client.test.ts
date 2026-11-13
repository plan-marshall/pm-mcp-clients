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

  it('posts the bulk read as report_ids and never a bare or all body', async () => {
    let readUrl = '';
    let readMethod = '';
    let readBody: unknown = null;
    globalThis.fetch = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      readUrl = url;
      readMethod = init?.method || '';
      if (init?.body) {
        readBody = JSON.parse(init.body as string);
      }
      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => ({ marked_count: 2 }),
      });
    });

    const result = await client.markAllReportsRead(['rep-0142', 'rep-0139']);

    expect(readMethod).toBe('POST');
    expect(readUrl).toContain('/api/v1/reports/read');
    expect(readBody).toEqual({ report_ids: ['rep-0142', 'rep-0139'] });
    expect(readBody).not.toHaveProperty('all');
    expect(result.marked_count).toBe(2);
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

  it('gives the withdraw of a stop a deadline of its own when the caller brings none', async () => {
    // Arrange: a runtime that never answers the DELETE.
    const timeout = vi.spyOn(AbortSignal, 'timeout');
    let capturedSignal: AbortSignal | undefined;
    globalThis.fetch = vi.fn().mockImplementation((_url, init) => {
      capturedSignal = (init as RequestInit)?.signal as AbortSignal | undefined;
      return new Promise(() => {});
    });

    // Act: the caller passes no signal, so the client's own bound is the one that applies.
    void client.withdrawStop();

    // Assert: the request is bounded by the client's 15-second deadline.
    expect(timeout).toHaveBeenCalledWith(15_000);
    expect(capturedSignal).toBeInstanceOf(AbortSignal);
    expect(capturedSignal?.aborted).toBe(false);
  });

  it('ends a request that never answers in the timeout code after the client limit', async () => {
    // Arrange: the client's own deadline has passed and the fetch reports the abort.
    const timeout = vi.spyOn(AbortSignal, 'timeout').mockReturnValue(AbortSignal.abort());
    globalThis.fetch = vi
      .fn()
      .mockRejectedValue(Object.assign(new Error('aborted'), { name: 'AbortError' }));

    // Act & Assert: the failure is the typed timeout, not a bare AbortError.
    await expect(client.getReports()).rejects.toMatchObject({ code: 'timeout' });
    expect(timeout).toHaveBeenCalledWith(15_000);
  });

  it('reports a caller deadline that fires mid-body as the timeout code', async () => {
    // Arrange: the status line arrives, and the caller's own deadline fires while the body is read.
    const controller = new AbortController();
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => {
        controller.abort(new DOMException('The operation timed out.', 'TimeoutError'));
        throw new TypeError('The operation was aborted');
      },
    });

    // Act & Assert: the same bound says the same thing whether it fires before or inside the body.
    await expect(client.getStatus(controller.signal)).rejects.toMatchObject({ code: 'timeout' });
  });

  it('ends a request that never answers in the timeout code when the limit passes, and not before', async () => {
    // Arrange: a runtime that never answers, and the client's own bound driven by the fake clock.
    vi.useFakeTimers();
    const deadline = new AbortController();
    vi.spyOn(AbortSignal, 'timeout').mockImplementation((milliseconds: number) => {
      setTimeout(
        () => deadline.abort(new DOMException('The operation timed out.', 'TimeoutError')),
        milliseconds,
      );
      return deadline.signal;
    });
    globalThis.fetch = vi.fn().mockImplementation((_url, init) => {
      const signal = (init as RequestInit).signal as AbortSignal;
      return new Promise((_resolve, reject) => {
        signal.addEventListener(
          'abort',
          () => reject(Object.assign(new Error('aborted'), { name: 'AbortError' })),
          { once: true },
        );
      });
    });
    let failure: unknown = null;
    const request = client.getReports().catch((err: unknown) => {
      failure = err;
    });

    // Act & Assert: nothing is reported a second before the 15-second limit passes.
    await vi.advanceTimersByTimeAsync(14_000);
    expect(failure).toBeNull();

    // The limit passes and the request ends in the typed code.
    await vi.advanceTimersByTimeAsync(1_500);
    await request;
    expect(failure).toMatchObject({ code: 'timeout' });
  });

  it('gives a check the longer client limit', async () => {
    // Arrange
    const timeout = vi.spyOn(AbortSignal, 'timeout').mockReturnValue(AbortSignal.abort());
    globalThis.fetch = vi
      .fn()
      .mockRejectedValue(Object.assign(new Error('aborted'), { name: 'AbortError' }));

    // Act & Assert
    await expect(client.checkUpgrade()).rejects.toMatchObject({ code: 'timeout' });
    expect(timeout).toHaveBeenCalledWith(30_000);
  });

  it('keeps a caller abort distinct from the client deadline', async () => {
    // Arrange: the caller's own controller is the one that aborted.
    const controller = new AbortController();
    controller.abort();
    globalThis.fetch = vi
      .fn()
      .mockRejectedValue(Object.assign(new Error('aborted'), { name: 'AbortError' }));

    // Act & Assert: a superseded read is not reported as a timeout.
    await expect(client.getStatus(controller.signal)).rejects.not.toMatchObject({
      code: 'timeout',
    });
  });

  it('reports a caller deadline that fired as the timeout code', async () => {
    // Arrange: the caller's bound is a deadline, not a cancellation.
    const controller = new AbortController();
    controller.abort(new DOMException('The operation timed out.', 'TimeoutError'));
    globalThis.fetch = vi
      .fn()
      .mockRejectedValue(Object.assign(new Error('aborted'), { name: 'AbortError' }));

    // Act & Assert
    await expect(client.getStatus(controller.signal)).rejects.toMatchObject({ code: 'timeout' });
  });

  it('turns a body lost mid-transfer into the unknown outcome', async () => {
    // Arrange: the status line arrives, the body is cut by the connection.
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => {
        throw new TypeError('Failed to fetch');
      },
    });

    // Act & Assert
    await expect(client.getReports()).rejects.toMatchObject({ code: 'network_offline' });
  });

  it('turns a truncated response body into the unknown outcome', async () => {
    // Arrange: the status line and headers arrive, the body never completes.
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => {
        throw new SyntaxError('Unexpected end of JSON input');
      },
    });

    // Act & Assert: the client cannot tell whether the runtime acted, so the outcome is unknown.
    await expect(client.getReports()).rejects.toMatchObject({ code: 'network_offline' });
  });

  it('turns a truncated pairing body into the unknown outcome', async () => {
    // Arrange
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => {
        throw new SyntaxError('Unexpected end of JSON input');
      },
    });

    // Act & Assert
    await expect(client.pair('KXQB-MRTN')).rejects.toMatchObject({ code: 'network_offline' });
    expect(client.isPaired()).toBe(false);
  });

  it('turns a truncated conflict-capable body into the unknown outcome', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_stored_secret');
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => {
        throw new SyntaxError('Unexpected end of JSON input');
      },
    });

    // Act & Assert
    await expect(
      client.saveProjectSettings('pm-mcp-clients', 'landing', 'landing.files', 7),
    ).rejects.toMatchObject({ code: 'network_offline' });
  });

  it('refuses a path that is not one this client may call', async () => {
    await expect(client.request('GET', '//evil')).rejects.toThrow('Invalid request path');
  });

  it('ends a pairing that never answers in the timeout code', async () => {
    vi.spyOn(AbortSignal, 'timeout').mockReturnValue(AbortSignal.abort());
    globalThis.fetch = vi
      .fn()
      .mockRejectedValue(Object.assign(new Error('aborted'), { name: 'AbortError' }));

    await expect(client.pair('KXQB-MRTN')).rejects.toMatchObject({ code: 'timeout' });
  });

  it('ends a conflict-capable call that never answers in the timeout code', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_stored_secret');
    vi.spyOn(AbortSignal, 'timeout').mockReturnValue(AbortSignal.abort());
    globalThis.fetch = vi
      .fn()
      .mockRejectedValue(Object.assign(new Error('aborted'), { name: 'AbortError' }));

    await expect(
      client.saveProjectSettings('pm-mcp-clients', 'landing', 'landing.files', 7),
    ).rejects.toMatchObject({ code: 'timeout' });
  });

  it('reports a body the client deadline cut as the timeout', async () => {
    vi.spyOn(AbortSignal, 'timeout').mockReturnValue(AbortSignal.abort());
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => {
        throw new TypeError('terminated');
      },
    });

    await expect(client.getReports()).rejects.toMatchObject({ code: 'timeout' });
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

  it('reads the matches of finding work by name from the runtime', async () => {
    // Arrange
    let requested = '';
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      requested = url;
      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => ({ page: 1, total_pages: 1, query: 'a b/c', matches: [] }),
      });
    }) as unknown as typeof fetch;

    // Act
    const page = await client.findWork('a b/c');

    // Assert
    expect(requested).toBe('http://localhost:7420/api/v1/find?q=a%20b%2Fc');
    expect(page.query).toBe('a b/c');
    expect(page.matches).toEqual([]);
  });

  it('does not report the machine offline when a read is aborted', async () => {
    // Arrange
    const onOffline = vi.fn();
    client.setOnOffline(onOffline);
    const inits: Array<RequestInit | undefined> = [];
    globalThis.fetch = vi.fn().mockImplementation((_url: string, init?: RequestInit) => {
      inits.push(init);
      const aborted = new Error('The operation was aborted');
      aborted.name = 'AbortError';
      return Promise.reject(aborted);
    }) as unknown as typeof fetch;
    const controller = new AbortController();
    controller.abort();

    // Act & Assert
    await expect(client.findWork('relay', controller.signal)).rejects.toThrow('aborted');
    // The caller's abort reaches the request (beside the client's own deadline), so aborting the
    // search stops the read itself.
    const bounded = inits[0]?.signal as AbortSignal | undefined;
    expect(bounded).toBeInstanceOf(AbortSignal);
    expect(bounded?.aborted).toBe(true);
    expect(onOffline).not.toHaveBeenCalled();
  });

  it.each([
    ['plan', 'codeberg-issues', '/api/v1/plans/codeberg-issues/chat'],
    ['epic', 'relay-hardening', '/api/v1/epics/relay-hardening/chat'],
  ] as const)(
    'addresses the chat of an ended %s by its own chat route, never under its project',
    async (kind, subjectId, path) => {
      // Arrange
      const requested: Array<{ method: string; url: string; body: unknown }> = [];
      globalThis.fetch = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
        requested.push({
          method: init?.method ?? 'GET',
          url,
          body: typeof init?.body === 'string' ? JSON.parse(init.body) : undefined,
        });
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () =>
            chatResource({ subject: { kind, id: subjectId }, scope: kind }),
        });
      }) as unknown as typeof fetch;

      // Act
      await client.getEndedChat(kind, subjectId);
      await client.acceptFollowUp(kind, subjectId, 'T-0207');

      // Assert: the accept names the proposal's turn, as the route requires
      expect(requested).toEqual([
        { method: 'GET', url: `http://localhost:7420${path}`, body: undefined },
        {
          method: 'POST',
          url: `http://localhost:7420${path}/proposal/accept`,
          body: { turn_id: 'T-0207' },
        },
      ]);
    },
  );
});

/**
 * @param overrides the members this test changes
 * @returns a `ChatResource` as the runtime writes it
 */
function chatResource(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    id: 'chat-1',
    subject: { kind: 'entry', id: 'ent-showroom' },
    role: { name: 'refine', model: 'sonnet' },
    scope: 'entry',
    state: 'chat.read-back',
    context: { knows: [{ key: 'chat.knows.description' }], as_of: '2026-10-14T10:00:00Z' },
    proposal: {
      turn_id: 'T-0003',
      reply_turn_id: 'T-0002',
      kind: 'description',
      summary: 'a description with fsynced',
      applies: { key: 'chat.applies.after_task', args: { task_id: 'TASK-3' } },
      before: 'the description before',
      after: 'the description with fsynced',
      state: 'open',
    },
    turns: [
      {
        turn_id: 'T-0001',
        kind: 'message',
        author: 'operator',
        status: 'complete',
        format: 'plain',
        untrusted: false,
        text: 'add fsynced',
        steps: [],
        refs: [],
        at: '2026-10-14T10:00:00Z',
      },
      {
        turn_id: 'T-0002',
        kind: 'message',
        author: 'refine',
        status: 'complete',
        format: 'markdown',
        untrusted: false,
        model: 'sonnet',
        text: 'The description with fsynced',
        steps: [],
        refs: [],
        at: '2026-10-14T10:00:05Z',
      },
    ],
    ...overrides,
  };
}

describe('ApiClient chat on the B1 contract', () => {
  let client: ApiClient;
  let requested: Array<{ method: string; url: string; body: unknown }>;

  beforeEach(() => {
    localStorage.clear();
    client = new ApiClient('http://localhost:7420');
    requested = [];
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  /**
   * @param answers the body to answer the nth request with
   * @param status the HTTP status of every answer
   */
  function respondWith(answers: unknown[], status = 200): void {
    let next = 0;
    globalThis.fetch = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      requested.push({
        method: init?.method ?? 'GET',
        url,
        body: typeof init?.body === 'string' ? JSON.parse(init.body) : undefined,
      });
      const body = answers[Math.min(next, answers.length - 1)];
      next += 1;
      return Promise.resolve({ ok: true, status, json: async () => body });
    }) as unknown as typeof fetch;
  }

  it('opens a start entry chat on its ChatRoutes address and reads a ChatResource', async () => {
    // Arrange
    respondWith([chatResource()], 201);

    // Act
    const chat = await client.openEntryChat('ent-showroom');

    // Assert: the B1 body of the answer becomes the chat the views read
    expect(requested).toEqual([
      {
        method: 'POST',
        url: 'http://localhost:7420/api/v1/entries/ent-showroom/chat',
        body: undefined,
      },
    ]);
    expect(chat.chat_id).toBe('chat-1');
    expect(chat.subject_id).toBe('ent-showroom');
    expect(chat.subject_kind).toBe('entry');
    expect(chat.role).toBe('refine');
    expect(chat.turns.map((turn) => turn.role)).toEqual(['operator', 'refine']);
    expect(chat.proposal?.turn_id).toBe('T-0003');
    expect(chat.proposal?.after).toBe('the description with fsynced');
  });

  it('sends a turn with a client_turn_id and reads on until the reply turn is complete', async () => {
    // Arrange: the first read still shows the reply streaming, the second shows it complete.
    const operatorTurn = {
      turn_id: 'T-0011',
      kind: 'message',
      author: 'operator',
      status: 'complete',
      format: 'plain',
      untrusted: false,
      text: 'add fsynced',
      steps: [],
      refs: [],
      at: '2026-10-14T10:00:00Z',
    };
    const streaming = chatResource({
      turns: [
        operatorTurn,
        {
          turn_id: 'T-0012',
          kind: 'message',
          author: 'refine',
          status: 'streaming',
          format: 'markdown',
          untrusted: false,
          model: 'sonnet',
          text: 'The desc',
          steps: [],
          refs: [],
          at: '2026-10-14T10:00:01Z',
        },
      ],
      proposal: null,
    });
    const complete = chatResource({
      turns: [
        operatorTurn,
        {
          turn_id: 'T-0012',
          kind: 'message',
          author: 'refine',
          status: 'complete',
          format: 'markdown',
          untrusted: false,
          model: 'sonnet',
          text: 'The description with fsynced',
          steps: [],
          refs: [],
          at: '2026-10-14T10:00:02Z',
        },
      ],
      proposal: null,
    });
    respondWith([{ turn_id: 'T-0011' }, streaming, complete], 202);
    vi.useFakeTimers();

    // Act
    const pending = client.chatTurn('ent-showroom', 'add fsynced');
    await vi.advanceTimersByTimeAsync(5000);
    const chat = await pending;

    // Assert: the message carries the client's own turn id, and the read follows the turn
    expect(requested[0]).toMatchObject({
      method: 'POST',
      url: 'http://localhost:7420/api/v1/entries/ent-showroom/chat/turns',
      body: { text: 'add fsynced' },
    });
    const sentBody = requested[0]?.body as Record<string, unknown>;
    expect(sentBody['client_turn_id']).toEqual(expect.any(String));
    expect(String(sentBody['client_turn_id']).length).toBeGreaterThan(0);
    expect(requested.slice(1).map((call) => call.url)).toEqual([
      'http://localhost:7420/api/v1/entries/ent-showroom/chat',
      'http://localhost:7420/api/v1/entries/ent-showroom/chat',
    ]);
    expect(chat.turns.map((turn) => turn.text)).toContain('The description with fsynced');
  });

  it('sends the client_turn_id again when the same message is retried', async () => {
    // Arrange: the first answer is lost, so the second send repeats the same message
    const bodies: unknown[] = [];
    let call = 0;
    globalThis.fetch = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      void url;
      call += 1;
      if (init?.method === 'POST') {
        bodies.push(init?.body);
      }
      if (call === 1) {
        return Promise.reject(new Error('Failed to fetch'));
      }
      if (call === 2) {
        return Promise.resolve({
          ok: true,
          status: 202,
          json: async () => ({ turn_id: 'T-0001' }),
        }) as unknown as Promise<Response>;
      }
      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => chatResource(),
      }) as unknown as Promise<Response>;
    }) as unknown as typeof fetch;

    // Act
    const chat = await client.chatTurn('ent-showroom', 'add fsynced');

    // Assert: one client turn id for the message, so the runtime records it once
    const first = JSON.parse(String(bodies[0])) as { client_turn_id: string };
    const second = JSON.parse(String(bodies[1])) as { client_turn_id: string };
    expect(second.client_turn_id).toBe(first.client_turn_id);
    expect(chat.chat_id).toBe('chat-1');
  });

  it('answers an entry chat proposal with the proposal turn_id', async () => {
    // Arrange
    respondWith([chatResource()]);

    // Act
    await client.answerChatProposal('ent-showroom', 'T-0003', true);

    // Assert
    expect(requested).toEqual([
      {
        method: 'POST',
        url: 'http://localhost:7420/api/v1/entries/ent-showroom/chat/proposal/accept',
        body: { turn_id: 'T-0003' },
      },
    ]);
  });

  it('ends a start entry chat on the B1 end act', async () => {
    // Arrange
    respondWith([chatResource({ state: 'chat.end' })]);

    // Act
    await client.endEntryChat('ent-showroom');

    // Assert
    expect(requested).toEqual([
      {
        method: 'POST',
        url: 'http://localhost:7420/api/v1/entries/ent-showroom/chat/end',
        body: undefined,
      },
    ]);
  });

  it.each([
    ['plan', 'fix-relay', '/api/v1/plans/fix-relay/chat'],
    ['epic', 'epic:relay-hardening', '/api/v1/epics/relay-hardening/chat'],
  ] as const)(
    'reads and writes the chat of a %s on its ChatRoutes address',
    async (kind, subjectId, path) => {
      // Arrange
      respondWith([chatResource({ subject: { kind, id: subjectId }, scope: kind })]);

      // Act
      await client.getChat(subjectId);
      await client.answerSubjectChatProposal(subjectId, 'T-0031', false);
      await client.endChat(subjectId);

      // Assert: the proposal's answer names the turn, the end carries no body
      expect(requested).toEqual([
        { method: 'GET', url: `http://localhost:7420${path}`, body: undefined },
        {
          method: 'POST',
          url: `http://localhost:7420${path}/proposal/reject`,
          body: { turn_id: 'T-0031' },
        },
        { method: 'POST', url: `http://localhost:7420${path}/end`, body: undefined },
      ]);
    },
  );

  it('sends a plan chat turn with a client_turn_id', async () => {
    // Arrange
    respondWith([
      { turn_id: 'T-0041' },
      chatResource({
        subject: { kind: 'plan', id: 'fix-relay' },
        turns: [
          {
            turn_id: 'T-0041',
            kind: 'message',
            author: 'operator',
            status: 'complete',
            format: 'plain',
            untrusted: false,
            text: 'why is it held?',
            steps: [],
            refs: [],
            at: '2026-10-14T10:00:00Z',
          },
          {
            turn_id: 'T-0042',
            kind: 'message',
            author: 'replan',
            status: 'complete',
            format: 'markdown',
            untrusted: false,
            model: 'sonnet',
            text: 'It waits for the scope.',
            steps: [],
            refs: [],
            at: '2026-10-14T10:00:02Z',
          },
        ],
        proposal: null,
      }),
    ]);

    // Act
    await client.chatWithSubject('fix-relay', 'why is it held?');

    // Assert
    expect(requested[0]).toMatchObject({
      method: 'POST',
      url: 'http://localhost:7420/api/v1/plans/fix-relay/chat/turns',
      body: { text: 'why is it held?' },
    });
    expect((requested[0]?.body as Record<string, unknown>)['client_turn_id']).toEqual(
      expect.any(String),
    );
  });

  it('stops reading at the bound while the reply keeps streaming', async () => {
    // Arrange: every read still shows the reply being written.
    const streaming = chatResource({
      turns: [
        {
          turn_id: 'T-0071',
          kind: 'message',
          author: 'operator',
          status: 'complete',
          format: 'plain',
          untrusted: false,
          text: 'add fsynced',
          steps: [],
          refs: [],
          at: '2026-10-14T10:00:00Z',
        },
        {
          turn_id: 'T-0072',
          kind: 'message',
          author: 'refine',
          status: 'streaming',
          format: 'markdown',
          untrusted: false,
          model: 'sonnet',
          text: 'The desc',
          steps: [],
          refs: [],
          at: '2026-10-14T10:00:01Z',
        },
      ],
      proposal: null,
    });
    respondWith([{ turn_id: 'T-0071' }, streaming]);
    vi.useFakeTimers();

    // Act
    const pending = client.chatTurn('ent-showroom', 'add fsynced');
    await vi.advanceTimersByTimeAsync(25_000);
    const chat = await pending;

    // Assert: the bound ends the wait and the chat is returned as it stands.
    expect(chat.turns.map((turn) => turn.text)).toContain('The desc');
  });

  it('reports no ended-work follow-up when the chat cannot be read', async () => {
    // Arrange: the runtime refuses the read.
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
      json: async () => ({ error: 'target_not_found' }),
    }) as unknown as typeof fetch;

    // Act & Assert
    expect(await client.getEndedChat('plan', 'codeberg-issues')).toBeNull();
  });
});
