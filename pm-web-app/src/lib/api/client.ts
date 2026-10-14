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
  private baseUrl = '';
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

  public async getStatus(): Promise<StatusResponse> {
    return this.request<StatusResponse>('GET', '/api/v1/status');
  }

  public async request<T>(method: string, path: string, body?: unknown): Promise<T> {
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

    const connect = async () => {
      const secret = this.getSecret();
      if (!secret || !active) return;

      try {
        const res = await fetch(`${this.baseUrl}/api/v1/events`, {
          headers: {
            Authorization: `Bearer ${secret}`,
          },
          signal,
        });

        if (res.status === 401) {
          this.clearAuth();
          this.onRevokedCallback?.('unauthorized');
          return;
        }

        if (!res.ok || !res.body) {
          throw new Error(`SSE failed: ${res.status}`);
        }

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (active) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const parts = buffer.split('\n\n');
          buffer = parts.pop() || '';

          for (const chunk of parts) {
            const lines = chunk.split('\n');
            let eventType = 'message';
            let eventData = '';

            for (const line of lines) {
              if (line.startsWith('event:')) {
                eventType = line.slice(6).trim();
              } else if (line.startsWith('data:')) {
                eventData += line.slice(5).trim();
              }
            }

            if (eventData) {
              try {
                const parsed = JSON.parse(eventData);
                onEvent(eventType, parsed);
                if (eventType === 'web_device_revoked') {
                  this.clearAuth();
                  this.onRevokedCallback?.(parsed.reason);
                  active = false;
                  return;
                }
              } catch {
                onEvent(eventType, eventData);
              }
            }
          }
        }
      } catch (err: unknown) {
        if ((err as Error)?.name === 'AbortError') return;
        if (active) {
          setTimeout(connect, 2000);
        }
      }
    };

    connect();

    return () => {
      active = false;
    };
  }
}

export const apiClient = new ApiClient();
