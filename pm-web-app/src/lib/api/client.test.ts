import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ApiClient } from './client';

describe('ApiClient', () => {
  let client: ApiClient;

  beforeEach(() => {
    localStorage.clear();
    client = new ApiClient('http://localhost:7420');
  });

  afterEach(() => {
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
    await expect(client.request('GET', 'http://attacker.com')).rejects.toThrow('Invalid request path');
    await expect(client.request('GET', '/\t/attacker.example/path')).rejects.toThrow('Invalid request path');
    await expect(client.request('GET', '/\n/attacker.example/path')).rejects.toThrow('Invalid request path');
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
});
