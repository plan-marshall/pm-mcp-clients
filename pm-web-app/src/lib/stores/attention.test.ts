import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { get } from 'svelte/store';
import {
  ATTENTION_KINDS,
  attentionFallbackKeyOf,
  attentionStore,
  attentionTargetOf,
  attentionTitleOf,
  handleNeedsOperatorChanged,
  loadAttention,
  loadMoreAttention,
  resetAttention,
} from './attention';
import type { NeedsOperatorItem, NeedsOperatorPage } from '../api/client';

/**
 * The attention store (shared/needs-and-find.adoc, The List of What Waits): the runtime's list as
 * it is served, in the order served, with the act of every kind, and an item resolved in another
 * client leaving with the quiet notice.
 */

/** A page of the list, as `GET /api/v1/machine/needs-operator` answers it. */
function pageOf(
  items: NeedsOperatorItem[],
  extra: Partial<NeedsOperatorPage> = {},
): NeedsOperatorPage {
  return {
    page: 1,
    total_pages: 1,
    has_newer: false,
    has_older: false,
    total: items.length,
    items,
    ...extra,
  };
}

/** One item of a kind, with the object it belongs to where the kind needs one. */
function itemOf(kind: string, overrides: Partial<NeedsOperatorItem> = {}): NeedsOperatorItem {
  return { kind, ...overrides };
}

/** Answers every read with the given page. */
function servePages(...pages: NeedsOperatorPage[]): void {
  let read = 0;
  globalThis.fetch = vi.fn().mockImplementation(() => {
    const page = pages[Math.min(read, pages.length - 1)];
    read += 1;
    return Promise.resolve({
      ok: true,
      status: 200,
      json: async () => page,
    });
  });
}

describe('the attention store', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'pmw_secret_attention');
    resetAttention();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('turns every kind the runtime serves into an item with the act of its place', () => {
    const items: NeedsOperatorItem[] = [
      itemOf('cleanup_due', { link: '/api/v1/system/cleanup' }),
      itemOf('credential', { link: '/api/v1/credentials/github' }),
      itemOf('decision', {
        link: '/api/v1/plans/drop-legacy-api',
        project: { id: 'pm-mcp-clients', title: 'pm-mcp-clients' },
        plan: { id: 'drop-legacy-api', title: 'Drop the legacy endpoints' },
        blocks: 'plan',
      }),
      itemOf('findings', {
        link: '/api/v1/epics/relay-hardening',
        epic: { id: 'relay-hardening', title: 'Relay Hardening' },
        blocks: 'epic',
      }),
      itemOf('setup', {
        link: '/api/v1/projects/plan-marshall',
        project: { id: 'plan-marshall', title: 'plan-marshall' },
      }),
      itemOf('held', {
        link: '/api/v1/plans/fix-relay',
        plan: { id: 'fix-relay', title: 'Fix Relay Transport' },
        blocks: 'plan',
      }),
    ];

    expect(ATTENTION_KINDS).toContain('cleanup_due');
    expect(new Set(items.map((item) => item.kind))).toEqual(
      new Set(['cleanup_due', 'credential', 'decision', 'findings', 'setup', 'held']),
    );
    expect(attentionTargetOf(items[0])).toBe('/system/cleanup');
    expect(attentionTargetOf(items[1])).toBe('/settings?tab=credentials');
    expect(attentionTargetOf(items[2])).toBe('/plans/drop-legacy-api');
    expect(attentionTargetOf(items[3])).toBe('/epics/relay-hardening');
    expect(attentionTargetOf(items[4])).toBe('/projects/plan-marshall');
    expect(attentionTargetOf(items[5])).toBe('/plans/fix-relay');
  });

  it('opens a decision its link names when the item belongs to no work', () => {
    const item = itemOf('decision', { link: '/api/v1/decisions/Q-016' });

    expect(attentionTargetOf(item)).toBe('/decisions/Q-016');
    expect(attentionTitleOf(item)).toBe('');
    expect(attentionFallbackKeyOf(item)).toBe('overview.needs.decision');
  });

  it('keeps the order the runtime served, blocking first', async () => {
    servePages(
      pageOf([
        itemOf('decision', { plan: { id: 'a', title: 'A' }, blocks: 'plan' }),
        itemOf('cleanup_due'),
        itemOf('decision', { plan: { id: 'b', title: 'B' }, blocks: 'plan' }),
      ]),
    );

    await loadAttention();

    expect(get(attentionStore).items.map((item) => item.kind)).toEqual([
      'decision',
      'cleanup_due',
      'decision',
    ]);
    expect(get(attentionStore).total).toBe(3);
  });

  it('drops a report, because a report is never an item', async () => {
    servePages(
      pageOf([
        itemOf('decision', { plan: { id: 'a', title: 'A' } }),
        itemOf('report', { link: '/api/v1/reports/7' }),
      ]),
    );

    await loadAttention();

    expect(get(attentionStore).items.map((item) => item.kind)).toEqual(['decision']);
  });

  it('lets an answered decision leave when another client resolves it, with the quiet notice', async () => {
    servePages(
      pageOf([
        itemOf('decision', { plan: { id: 'a', title: 'A' }, blocks: 'plan' }),
        itemOf('cleanup_due'),
      ]),
      pageOf([itemOf('cleanup_due')]),
    );

    await loadAttention();
    expect(get(attentionStore).items).toHaveLength(2);

    await handleNeedsOperatorChanged();

    expect(get(attentionStore).items.map((item) => item.kind)).toEqual(['cleanup_due']);
    expect(get(attentionStore).total).toBe(1);
    expect(get(attentionStore).leftNotice).toBe('decision');
  });

  it('reads the next page at the end, so the count and the rows agree', async () => {
    servePages(
      pageOf([itemOf('decision', { plan: { id: 'a', title: 'A' } })], {
        has_older: true,
        total: 2,
      }),
      pageOf([itemOf('cleanup_due')], { page: 2, has_older: false, total: 2 }),
    );

    await loadAttention();
    expect(get(attentionStore).hasOlder).toBe(true);
    expect(get(attentionStore).total).toBe(2);

    await loadMoreAttention();

    expect(get(attentionStore).items.map((item) => item.kind)).toEqual(['decision', 'cleanup_due']);
    expect(get(attentionStore).hasOlder).toBe(false);
    expect(get(attentionStore).total).toBe(2);
  });

  it('keeps the list it had when a read fails, and says the read failed', async () => {
    servePages(pageOf([itemOf('cleanup_due')]));
    await loadAttention();

    globalThis.fetch = vi.fn().mockRejectedValue(new TypeError('connection lost'));
    await loadAttention();

    expect(get(attentionStore).items.map((item) => item.kind)).toEqual(['cleanup_due']);
    expect(get(attentionStore).loadError).not.toBeNull();
  });
});
