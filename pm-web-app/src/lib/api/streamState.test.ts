import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ApiClient, type EventStreamState } from './client';

/** A response body that never ends, so the stream stays connected while the test reads it. */
function hangingBody() {
  return {
    getReader: () => ({
      read: () => new Promise<never>(() => {}),
      cancel: () => Promise.resolve(),
    }),
  };
}

/**
 * The state of the shared event stream: what the live indicator shows while the machine goes
 * away, while the client retries, and after it gave up (UX-P-12).
 */
describe('the event stream state', () => {
  let client: ApiClient;
  let states: EventStreamState[];

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'pmw_secret_of_the_test_browser');
    client = new ApiClient('http://localhost:7420');
    states = [];
    client.setOnStreamState((state) => states.push(state));
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('reports connecting, then reconnecting while it retries, then offline once it gave up', async () => {
    globalThis.fetch = vi.fn().mockRejectedValue(new Error('the machine is away'));
    const unsubscribe = client.subscribeEvents(() => {});

    await vi.advanceTimersByTimeAsync(0);
    expect(states).toEqual(['connecting', 'reconnecting']);

    await vi.advanceTimersByTimeAsync(25_000);
    expect(states.at(-1)).toBe('offline');
    expect(states.filter((state) => state === 'offline')).toHaveLength(1);

    unsubscribe();
  });

  it('reports connected once the machine answers, and reconnecting when the stream drops', async () => {
    let reads = 0;
    const body = {
      getReader: () => ({
        read: () => {
          reads += 1;
          return reads === 1 ? Promise.resolve({ done: true }) : new Promise<never>(() => {});
        },
        cancel: () => Promise.resolve(),
      }),
    };
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: true, status: 200, body });

    const unsubscribe = client.subscribeEvents(() => {});
    await vi.advanceTimersByTimeAsync(0);

    // The first read ends the stream at once, so a live stream that drops reports the retry.
    expect(states).toContain('connected');
    expect(states.at(-1)).toBe('reconnecting');

    unsubscribe();
  });

  it('comes back live on the retry after it gave up', async () => {
    globalThis.fetch = vi.fn().mockRejectedValue(new Error('the machine is away'));
    const unsubscribe = client.subscribeEvents(() => {});
    await vi.advanceTimersByTimeAsync(25_000);
    expect(states.at(-1)).toBe('offline');

    globalThis.fetch = vi.fn().mockResolvedValue({ ok: true, status: 200, body: hangingBody() });
    client.retryEventStream();
    await vi.advanceTimersByTimeAsync(0);

    expect(states.at(-1)).toBe('connected');

    unsubscribe();
  });

  it('does nothing on a retry while the stream is live', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: true, status: 200, body: hangingBody() });
    const unsubscribe = client.subscribeEvents(() => {});
    await vi.advanceTimersByTimeAsync(0);
    expect(states.at(-1)).toBe('connected');

    client.retryEventStream();
    await vi.advanceTimersByTimeAsync(0);
    expect(states.at(-1)).toBe('connected');
    expect(globalThis.fetch).toHaveBeenCalledTimes(1);

    unsubscribe();
  });
});
