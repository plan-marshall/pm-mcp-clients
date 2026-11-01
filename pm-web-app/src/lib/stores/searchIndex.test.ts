import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { get } from 'svelte/store';
import {
  SEARCH_DEBOUNCE_MS,
  groupFindMatches,
  resetSearchIndex,
  searchIndexStore,
  setSearchText,
  type FindMatch,
  type FindPage,
} from './searchIndex';

/**
 * The search index is the client of the runtime's `GET /api/v1/find?q=`: it sends the text, keeps
 * the matches in the order served, and never builds an index of its own.
 */

/**
 * @param matches the matches the runtime serves
 * @param query the text the page repeats
 * @returns the page representation the runtime answers with
 */
function pageOf(matches: FindMatch[], query: string): FindPage {
  return {
    page: 1,
    total_pages: 1,
    has_newer: false,
    has_older: false,
    query,
    matches,
  };
}

/**
 * @param body the object the runtime answers with
 * @returns a fetch result carrying it
 */
function jsonResponse(body: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: async () => body,
  } as Response;
}

/** The matches the mock runtime serves for "relay" on the fully-onboarded state. */
const RELAY_MATCHES: FindMatch[] = [
  {
    kind: 'epic',
    id: 'relay-hardening',
    title: 'Relay Hardening',
    project: { id: 'pm-mcp-clients', title: 'PM MCP Clients' },
    epic: { id: 'relay-hardening', title: 'Relay Hardening' },
    ended: false,
  },
  {
    kind: 'plan',
    id: 'fix-relay',
    title: 'Fix Relay Transport',
    project: { id: 'pm-mcp-clients', title: 'PM MCP Clients' },
    epic: { id: 'relay-hardening', title: 'Relay Hardening' },
    plan: { id: 'fix-relay', title: 'Fix Relay Transport' },
    ended: false,
  },
  {
    kind: 'plan',
    id: 'relay-metrics',
    title: 'Relay metrics',
    project: { id: 'pm-mcp-clients', title: 'PM MCP Clients' },
    epic: { id: 'relay-hardening', title: 'Relay Hardening' },
    plan: { id: 'relay-metrics', title: 'Relay metrics' },
    ended: false,
  },
  {
    kind: 'decision',
    id: 'Q-007',
    title: 'Drop the legacy relay endpoint',
    plan: { id: 'fix-relay', title: 'Fix Relay Transport' },
    ended: false,
  },
];

describe('searchIndex', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    resetSearchIndex();
    localStorage.clear();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('finds the epic, its plans and its decisions for "relay", in the order served', async () => {
    // Arrange
    const requested: string[] = [];
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      requested.push(url);
      return Promise.resolve(jsonResponse(pageOf(RELAY_MATCHES, 'relay')));
    }) as unknown as typeof fetch;

    // Act
    setSearchText('relay');
    await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);

    // Assert
    expect(requested).toEqual(['/api/v1/find?q=relay']);
    const state = get(searchIndexStore);
    expect(state.status).toBe('done');
    expect(state.matches).toEqual(RELAY_MATCHES);
    expect(state.matches.map((match) => `${match.kind}:${match.id}`)).toEqual([
      'epic:relay-hardening',
      'plan:fix-relay',
      'plan:relay-metrics',
      'decision:Q-007',
    ]);
  });

  it('finds the plan of pull request 214 for "214"', async () => {
    // Arrange
    const prMatch: FindMatch = {
      kind: 'plan',
      id: 'fix-relay',
      title: 'Fix Relay Transport',
      project: { id: 'pm-mcp-clients', title: 'PM MCP Clients' },
      epic: { id: 'relay-hardening', title: 'Relay Hardening' },
      plan: { id: 'fix-relay', title: 'Fix Relay Transport' },
      ended: false,
    };
    const requested: string[] = [];
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      requested.push(url);
      return Promise.resolve(jsonResponse(pageOf([prMatch], '214')));
    }) as unknown as typeof fetch;

    // Act
    setSearchText('214');
    await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);

    // Assert
    expect(requested).toEqual(['/api/v1/find?q=214']);
    expect(get(searchIndexStore).matches).toEqual([prMatch]);
    expect(get(searchIndexStore).matches[0]?.id).toBe('fix-relay');
  });

  it('sends nothing for an empty text', async () => {
    // Arrange
    const fetchSpy = vi.fn();
    globalThis.fetch = fetchSpy as unknown as typeof fetch;
    setSearchText('relay');
    await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
    fetchSpy.mockClear();

    // Act
    setSearchText('   ');
    await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);

    // Assert
    expect(fetchSpy).not.toHaveBeenCalled();
    const state = get(searchIndexStore);
    expect(state.text).toBe('');
    expect(state.matches).toEqual([]);
    expect(state.status).toBe('idle');
  });

  it('sends one request per pause in the typing, never one per keystroke', async () => {
    // Arrange
    const requested: string[] = [];
    globalThis.fetch = vi.fn().mockImplementation((url: string) => {
      requested.push(url);
      return Promise.resolve(jsonResponse(pageOf(RELAY_MATCHES, 'relay')));
    }) as unknown as typeof fetch;

    // Act
    setSearchText('r');
    setSearchText('re');
    setSearchText('rel');
    setSearchText('relay');
    await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);

    // Assert
    expect(requested).toEqual(['/api/v1/find?q=relay']);
  });

  it('cancels the older request when a newer text arrives, and keeps the last answer', async () => {
    // Arrange
    const pending: { resolve: ((value: Response) => void) | null } = { resolve: null };
    const signals: AbortSignal[] = [];
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      if (init?.signal) signals.push(init.signal);
      if (url.endsWith('q=re')) {
        return new Promise<Response>((resolve) => {
          pending.resolve = resolve;
        });
      }
      return Promise.resolve(jsonResponse(pageOf(RELAY_MATCHES, 'relay')));
    });
    globalThis.fetch = fetchMock as unknown as typeof fetch;

    // Act
    setSearchText('re');
    await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
    setSearchText('relay');
    await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
    pending.resolve?.(jsonResponse(pageOf([RELAY_MATCHES[0]!], 're')));
    await vi.advanceTimersByTimeAsync(0);

    // Assert
    expect(signals).toHaveLength(2);
    expect(signals[0]?.aborted).toBe(true);
    expect(signals[1]?.aborted).toBe(false);
    expect(get(searchIndexStore).text).toBe('relay');
    expect(get(searchIndexStore).matches).toEqual(RELAY_MATCHES);
  });

  it('keeps a failed read apart from an empty answer', async () => {
    // Arrange
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({ error: 'failure' }),
    }) as unknown as typeof fetch;

    // Act
    setSearchText('relay');
    await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);

    // Assert
    const state = get(searchIndexStore);
    expect(state.status).toBe('failed');
    expect(state.matches).toEqual([]);
  });
});

describe('groupFindMatches', () => {
  it('groups consecutive matches of one kind and keeps the served order', () => {
    // Arrange
    const matches: FindMatch[] = [
      RELAY_MATCHES[0]!,
      RELAY_MATCHES[1]!,
      RELAY_MATCHES[2]!,
      RELAY_MATCHES[3]!,
      {
        kind: 'plan',
        id: 'relay-cursor-order',
        title: 'relay-cursor-order',
        project: { id: 'pm-mcp-clients', title: 'PM MCP Clients' },
        ended: true,
        outcome: 'merged',
      },
    ];

    // Act
    const groups = groupFindMatches(matches);

    // Assert
    expect(groups.map((group) => `${group.kind}:${group.ended}:${group.matches.length}`)).toEqual([
      'epic:false:1',
      'plan:false:2',
      'decision:false:1',
      'plan:true:1',
    ]);
  });

  it('answers no group for no match', () => {
    expect(groupFindMatches([])).toEqual([]);
  });
});
