export interface PairingResponse {
  device_secret: string;
  device_id: string;
  label: string;
}

export interface StatusResponse {
  status: string;
  version?: string;
  listener?: string;
  onboarded?: boolean;
  machine?: {
    name?: string;
    onboarded?: boolean;
  };
  projects?: unknown[];
}

export interface HypermediaLink {
  href: string;
  method?: string;
  next?: string;
}

export interface StateRepresentation {
  scope?: string;
  state?: string;
  summary?: string;
  links?: Record<string, HypermediaLink | string>;
  _links?: Record<string, HypermediaLink | string>;
  [key: string]: unknown;
}

const STORAGE_KEY_SECRET = 'pm_device_secret';
const STORAGE_KEY_DEVICE_ID = 'pm_device_id';
const STORAGE_KEY_LABEL = 'pm_device_label';

export class ApiClient {
  private readonly baseUrl: string;
  private onRevokedCallback?: (reason?: string) => void;
  private onOfflineCallback?: () => void;

  constructor(baseUrl = '') {
    this.baseUrl = baseUrl;
  }

  public setOnRevoked(callback: (reason?: string) => void): void {
    this.onRevokedCallback = callback;
  }

  public setOnOffline(callback: () => void): void {
    this.onOfflineCallback = callback;
  }

  public getSecret(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(STORAGE_KEY_SECRET);
  }

  public getLabel(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(STORAGE_KEY_LABEL);
  }

  public isPaired(): boolean {
    return Boolean(this.getSecret());
  }

  public clearAuth(): void {
    if (typeof window === 'undefined') return;
    localStorage.removeItem(STORAGE_KEY_SECRET);
    localStorage.removeItem(STORAGE_KEY_DEVICE_ID);
    localStorage.removeItem(STORAGE_KEY_LABEL);
  }

  public async pair(code: string): Promise<PairingResponse> {
    const cleanCode = code.trim();
    const url = `${this.baseUrl}/api/v1/pair`;

    let res: Response;
    try {
      res = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ code: cleanCode }),
      });
    } catch {
      this.onOfflineCallback?.();
      throw new Error('network_offline');
    }

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || 'invalid_code');
    }

    const data: PairingResponse = await res.json();
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY_SECRET, data.device_secret);
      localStorage.setItem(STORAGE_KEY_DEVICE_ID, data.device_id);
      localStorage.setItem(STORAGE_KEY_LABEL, data.label);
    }

    return data;
  }

  public async getAvailableMessages(): Promise<{ locales: string[]; areas: string[] }> {
    return this.request<{ locales: string[]; areas: string[] }>('GET', '/api/v1/messages');
  }

  public async getMessages(locale: string, area = 'all'): Promise<Record<string, string>> {
    return this.request<Record<string, string>>('GET', `/api/v1/messages/${locale}/${area}`);
  }

  public async getStatus(): Promise<StatusResponse> {
    return this.request<StatusResponse>('GET', '/api/v1/status');
  }

  public async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    if (
      !path.startsWith('/') ||
      path.startsWith('//') ||
      path.startsWith('/\\') ||
      /[\t\n\r]/.test(path)
    ) {
      throw new Error(`Invalid request path: ${path}`);
    }

    const secret = this.getSecret();
    const headers: Record<string, string> = {};

    if (secret) {
      headers['Authorization'] = `Bearer ${secret}`;
    }

    if (body !== undefined) {
      headers['Content-Type'] = 'application/json';
    }

    let res: Response;
    try {
      res = await fetch(`${this.baseUrl}${path}`, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
      });
    } catch {
      this.onOfflineCallback?.();
      throw new Error('network_offline');
    }

    if (res.status === 401) {
      this.clearAuth();
      this.onRevokedCallback?.('unauthorized');
      throw new Error('unauthorized');
    }

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `HTTP ${res.status}`);
    }

    return res.json() as Promise<T>;
  }

  public async followLink(
    link: HypermediaLink | string,
    body?: unknown,
  ): Promise<StateRepresentation> {
    const href = typeof link === 'string' ? link : link.href;
    const method = typeof link === 'string' ? 'POST' : link.method || 'POST';
    return this.request<StateRepresentation>(method, href, body);
  }

  public subscribeEvents(
    onEvent: (event: string, data: unknown) => void,
    signal?: AbortSignal,
  ): () => void {
    let active = true;
    let currentReader: ReadableStreamDefaultReader<Uint8Array> | undefined;
    const internalController = new AbortController();

    if (signal) {
      if (signal.aborted) {
        internalController.abort();
      } else {
        signal.addEventListener('abort', () => internalController.abort(), { once: true });
      }
    }

    const abortAll = () => {
      active = false;
      internalController.abort();
      if (currentReader) {
        void currentReader.cancel().catch(() => {});
        currentReader = undefined;
      }
    };

    const connect = async () => {
      const secret = this.getSecret();
      if (!secret || !active || internalController.signal.aborted) return;

      try {
        const res = await fetch(`${this.baseUrl}/api/v1/events`, {
          headers: {
            Authorization: `Bearer ${secret}`,
          },
          signal: internalController.signal,
        });

        if (res.status === 401) {
          this.clearAuth();
          this.onRevokedCallback?.('unauthorized');
          abortAll();
          return;
        }

        if (!res.ok || !res.body) {
          throw new Error(`SSE failed: ${res.status}`);
        }

        const reader = res.body.getReader();
        currentReader = reader;
        const decoder = new TextDecoder();
        let buffer = '';

        while (active && !internalController.signal.aborted) {
          const { done, value } = await reader.read();
          if (done) {
            currentReader = undefined;
            if (active && !internalController.signal.aborted) {
              setTimeout(() => {
                void connect();
              }, 2000);
            }
            return;
          }

          buffer += decoder.decode(value, { stream: true });
          const parts = buffer.split(/(?:\r\n|\r|\n){2}/);
          buffer = parts.pop() || '';

          for (const chunk of parts) {
            const parsed = parseSseChunk(chunk);
            if (!parsed) continue;

            const revoked = handleSseMessage(parsed.eventType, parsed.eventData, onEvent, (reason) => {
              this.clearAuth();
              this.onRevokedCallback?.(reason);
            });

            if (revoked) {
              abortAll();
              return;
            }
          }
        }
      } catch (err: unknown) {
        currentReader = undefined;
        if ((err as Error)?.name === 'AbortError') return;
        if (active && !internalController.signal.aborted) {
          setTimeout(() => {
            void connect();
          }, 2000);
        }
      }
    };

    void connect();

    return () => {
      abortAll();
    };
  }
}

function parseSseChunk(chunk: string): { eventType: string; eventData: string } | null {
  const lines = chunk.split(/\r\n|\r|\n/);
  let eventType = 'message';
  let eventData = '';

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventType = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      eventData += line.slice(5).trim();
    }
  }

  return eventData ? { eventType, eventData } : null;
}

function handleSseMessage(
  eventType: string,
  eventData: string,
  onEvent: (event: string, data: unknown) => void,
  onRevoke: (reason?: string) => void,
): boolean {
  try {
    const parsed = JSON.parse(eventData);
    onEvent(eventType, parsed);
    if (eventType === 'web_device_revoked') {
      onRevoke(parsed.reason);
      return true;
    }
  } catch {
    onEvent(eventType, eventData);
  }
  return false;
}

export const apiClient = new ApiClient();
