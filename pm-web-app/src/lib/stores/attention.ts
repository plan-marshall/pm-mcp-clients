/**
 * The attention inbox's store (UC-PLN-7; `shared/needs-and-find.adoc`, The List of What Waits).
 *
 * It holds the runtime's list of what waits, as `GET /api/v1/machine/needs-operator` serves it —
 * in the order served, the items that block running work first and then the oldest, sorted by
 * nothing on the client — and keeps it in step with the stream: `needs_operator.changed` re-reads
 * it, and an item that another client resolved leaves the list with the quiet notice its kind
 * names. A report is never an item: the runtime serves reports as reports, and a payload that
 * carries one drops it.
 *
 * The store names the place of every kind (`attentionTargetOf`) and the title of the object it
 * belongs to (`attentionTitleOf`), so the list and its act open the same place and cannot drift
 * apart. It carries no catalogue text: the view translates the keys it hands out.
 */

import { get, writable } from 'svelte/store';
import {
  apiClient,
  type NeedsOperatorItem,
  type NeedsOperatorLocation,
  type NeedsOperatorPage,
} from '../api/client';

/**
 * The kinds the runtime serves, in its order (`NeedsOperatorKind`): the decision, the waiver, any
 * other operator wait, the holds, new findings, the setup, then the machine's own items. A kind a
 * client predates reads as `other`, so one new kind never fails the whole list.
 */
export const ATTENTION_KINDS = Object.freeze([
  'decision',
  'waiver',
  'waits_for_you',
  'stalled',
  'condition',
  'integrity_pause',
  'held',
  'landing_failed',
  'findings',
  'setup',
  'credential',
  'cleanup_due',
  'other',
] as const);

/** One kind of what waits. */
export type AttentionKind = (typeof ATTENTION_KINDS)[number];

/** The wire kind that is never an item of this list. */
const NEVER_AN_ITEM = 'report';

/** One item of the list, as the view reads it. */
export interface AttentionItem {
  /** Why it waits; an unknown wire kind reads as `other`. */
  kind: AttentionKind;
  /** The path of the item itself, or `null` when the runtime names none. */
  link: string | null;
  /** The project it belongs to, or `null` for the machine's own items. */
  project: NeedsOperatorLocation | null;
  /** The epic it belongs to, or `null`. */
  epic: NeedsOperatorLocation | null;
  /** The plan it belongs to, or `null`. */
  plan: NeedsOperatorLocation | null;
  /** When it started to wait, or `null` when the runtime measures none. */
  since: string | null;
  /** What it blocks, or `null` when it blocks no work. */
  blocks: string | null;
}

/** The state of the one list of what waits. */
export interface AttentionState {
  /** The loaded items, in the order served. */
  items: AttentionItem[];
  /** The number of items of every page, as the runtime counts them. */
  total: number;
  /** The page the last read ended at. */
  page: number;
  /** Whether an older page exists: what `Show more` reads. */
  hasOlder: boolean;
  /** Whether the list was served at least once. */
  loaded: boolean;
  /** Whether a read is in flight. */
  isLoading: boolean;
  /** The code of a failed read; the list keeps what it had. */
  loadError: string | null;
  /** The kind of the item that last left because another client resolved it. */
  leftNotice: AttentionKind | null;
}

const initialState: AttentionState = {
  items: [],
  total: 0,
  page: 1,
  hasOlder: false,
  loaded: false,
  isLoading: false,
  loadError: null,
  leftNotice: null,
};

/** The one store of the list of what waits. */
export const attentionStore = writable<AttentionState>({ ...initialState });

/** @returns the store's state to its start, so a later list starts clean */
export function resetAttention(): void {
  attentionStore.set({ ...initialState });
}

/**
 * @param wire the kind the runtime serves
 * @returns the kind as the client reads it, or `null` for a kind that is never an item
 */
function kindOf(wire: string | null | undefined): AttentionKind | null {
  const value = (wire ?? '').trim();
  if (!value) return 'other';
  if (value === NEVER_AN_ITEM) return null;
  if (value === 'waiver_request') return 'waiver';
  if (value === 'operator_wait') return 'waits_for_you';
  return (ATTENTION_KINDS as readonly string[]).includes(value) ? (value as AttentionKind) : 'other';
}

/**
 * @param item one item as the runtime serves it
 * @returns the item the store keeps, or `null` for a kind that is never an item
 */
function itemOf(item: NeedsOperatorItem): AttentionItem | null {
  const kind = kindOf(item.kind);
  if (kind === null) {
    return null;
  }
  return {
    kind,
    link: item.link ?? null,
    project: item.project ?? null,
    epic: item.epic ?? null,
    plan: item.plan ?? null,
    since: item.since ?? null,
    blocks: item.blocks ?? null,
  };
}

/**
 * @param items the items of a page
 * @returns the store's items, without the kinds that are never items
 */
function itemsOf(items: NeedsOperatorItem[] | null | undefined): AttentionItem[] {
  const kept: AttentionItem[] = [];
  for (const item of items ?? []) {
    const normalized = itemOf(item);
    if (normalized !== null) {
      kept.push(normalized);
    }
  }
  return kept;
}

/**
 * The id a link names under a path, decoded.
 *
 * @param link the link the runtime serves, e.g. `/api/v1/decisions/Q-009`
 * @param prefix the API path the id follows, e.g. `/decisions/`
 * @returns the id, or `null` when the link names another resource
 */
function idIn(link: string | null, prefix: string): string | null {
  if (!link) return null;
  const index = link.indexOf(prefix);
  if (index < 0) return null;
  const raw = link.slice(index + prefix.length).split(/[/?#]/, 1)[0] ?? '';
  if (!raw) return null;
  try {
    return decodeURIComponent(raw);
  } catch {
    return null;
  }
}

/**
 * @param item one item of the list
 * @returns the decision its link names, or `null` when the link names none
 */
function decisionOf(item: AttentionItem): string | null {
  return idIn(item.link, '/decisions/');
}

/**
 * The place an item's act opens: the address of the object the item belongs to.
 *
 * The runtime's own `link` names the place first, so a decision whose link names its decision
 * opens the decision and one that names its plan opens the plan. The machine's own items open
 * their own places: a due cleanup opens the cleanup job and a credential opens the settings'
 * providers. Only an item that names nothing opens the list itself.
 *
 * @param item one item of the list
 * @returns the path of the place the act opens
 */
export function attentionTargetOf(item: AttentionItem): string {
  const byLink = targetOfLink(item.link);
  if (byLink !== null) {
    return byLink;
  }
  if (item.plan?.id) return `/plans/${encodeURIComponent(item.plan.id)}`;
  if (item.epic?.id) return `/epics/${encodeURIComponent(item.epic.id)}`;
  if (item.project?.id) return `/projects/${encodeURIComponent(item.project.id)}`;
  return '/';
}

/**
 * @param link the item's own link
 * @returns the app's path for it, or `null` when the link names no place of the app
 */
function targetOfLink(link: string | null): string | null {
  if (!link) return null;
  if (link.includes('/system/cleanup')) return '/system/cleanup';
  if (link.includes('/credentials/')) return '/settings?tab=credentials';
  const decision = idIn(link, '/decisions/');
  if (decision) return `/decisions/${encodeURIComponent(decision)}`;
  const plan = idIn(link, '/plans/');
  if (plan) return `/plans/${encodeURIComponent(plan)}`;
  const epic = idIn(link, '/epics/');
  if (epic) return `/epics/${encodeURIComponent(epic)}`;
  const project = idIn(link, '/projects/');
  if (project) return `/projects/${encodeURIComponent(project)}`;
  return null;
}

/**
 * @param item one item of the list
 * @returns the title of the object it belongs to, or an empty string when it names none
 */
export function attentionTitleOf(item: AttentionItem): string {
  return (
    (item.plan?.title ?? '').trim() ||
    (item.epic?.title ?? '').trim() ||
    (item.project?.title ?? '').trim()
  );
}

/**
 * The kind mark of an item: the kind of the object it belongs to, not the reason it waits.
 *
 * @param item one item of the list
 * @returns the `KindMark` kind of its object
 */
export function attentionKindMarkOf(
  item: AttentionItem,
): 'project' | 'epic' | 'plan' | 'decision' | 'machine' {
  if (item.plan) return 'plan';
  if (item.epic) return 'epic';
  if (item.project) return 'project';
  if (decisionOf(item) !== null || item.kind === 'decision') return 'decision';
  return 'machine';
}

/**
 * The catalogue key an item without a title reads as.
 *
 * @param item one item of the list
 * @returns the key of the place or object the item names
 */
export function attentionFallbackKeyOf(item: AttentionItem): string {
  if (item.kind === 'cleanup_due') return 'overview.needs.subject.cleanup';
  if (item.kind === 'credential') return 'overview.needs.credential';
  return `overview.needs.${item.kind}`;
}

/** A catalogue key with the names its text carries. */
export interface AttentionActLabel {
  /** The catalogue key of the act's words: the words of the place it opens. */
  key: string;
  /** The names the text carries. */
  args?: Record<string, string>;
}

/**
 * The words of an item's act: those of the place the act opens, never a word of the list.
 *
 * Where the place is a decision, the place has no name of its own in the catalogue, so the act
 * carries the decision's id beside the kind's word — a name no other control of the shell shows,
 * so the row's act and the rail's Decisions entry never share an accessible name.
 *
 * @param item one item of the list
 * @returns the key and arguments of the act's label
 */
export function attentionActLabelOf(item: AttentionItem): AttentionActLabel {
  const target = attentionTargetOf(item);
  if (target === '/system/cleanup') return { key: 'system.cleanup.title' };
  if (target.startsWith('/settings')) return { key: 'web.settings.tab.credentials' };
  if (target.startsWith('/decisions/')) {
    return { key: 'overview.needs.decision', args: { id: target.slice('/decisions/'.length) } };
  }
  if (target.startsWith('/plans/') && item.plan?.id) {
    return { key: 'plan.view.title', args: { plan: attentionTitleOf(item) || item.plan.id } };
  }
  if (target.startsWith('/epics/') && item.epic?.id) {
    return { key: 'epic.view.title', args: { epic: attentionTitleOf(item) || item.epic.id } };
  }
  if (target.startsWith('/projects/')) return { key: 'project.explorer.title' };
  return { key: 'overview.needs.title' };
}

/**
 * @param value the figure the runtime served
 * @param fallback the figure to use when the runtime served none
 * @returns the served figure, or the fallback for an absent or unreadable one
 */
function countOf(value: number | null | undefined, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : fallback;
}

/**
 * Reads one page of the list.
 *
 * Page 1 replaces the list; a later page is read by {@link loadMoreAttention}. A failed read
 * keeps the list it had and names the failure, so the rows never vanish under a lost connection.
 *
 * @param page the page to read, 1-based
 */
export async function loadAttention(page = 1): Promise<void> {
  if (!apiClient.isPaired()) {
    return;
  }
  attentionStore.update((state) => ({ ...state, isLoading: true, loadError: null }));
  try {
    const answer: NeedsOperatorPage = await apiClient.getNeedsOperator(page);
    const items = itemsOf(answer.items);
    const total = countOf(answer.total, items.length);
    attentionStore.update((state) =>
      page <= 1
        ? {
            ...state,
            items,
            total,
            page: 1,
            hasOlder: answer.has_older === true,
            loaded: true,
            isLoading: false,
            loadError: null,
            leftNotice: leftOf(state, items),
          }
        : {
            ...state,
            items: [...state.items, ...items],
            total,
            page,
            hasOlder: answer.has_older === true,
            loaded: true,
            isLoading: false,
            loadError: null,
          },
    );
  } catch (error: unknown) {
    attentionStore.update((state) => ({
      ...state,
      isLoading: false,
      loadError: (error as Error)?.message || 'network_offline',
    }));
  }
}

/**
 * Reads the next page at the end of the list, where the runtime reports one.
 *
 * The page is never read on scroll alone: the person asks for it with `Show more`
 * (`overview.needs.more`), so the count beside the rail entry and the rows on screen never
 * disagree about being complete.
 */
export async function loadMoreAttention(): Promise<void> {
  const state = get(attentionStore);
  if (!state.hasOlder || state.isLoading) {
    return;
  }
  await loadAttention(state.page + 1);
}

/**
 * @param item one item of the list
 * @returns the identity its row keeps: its kind, its link and its since, so a neighbour does not
 *   take the key of the item that left
 */
function identityOf(item: AttentionItem): string {
  return `${item.kind}\u0000${item.link ?? ''}\u0000${item.since ?? ''}`;
}

/**
 * @param state the state before the read
 * @param items the items the read served
 * @returns the kind of the first item that left the list, or `null` when none did
 */
function leftOf(state: AttentionState, items: AttentionItem[]): AttentionKind | null {
  if (!state.loaded) {
    return null;
  }
  const kept = new Set(items.map(identityOf));
  const gone = state.items.find((item) => !kept.has(identityOf(item)));
  return gone?.kind ?? null;
}

/**
 * Keeps the list in step with the stream, after `needs_operator.changed`.
 *
 * The event's summary counts the machine's items, not the list's rows, so the list is re-read
 * rather than patched: only the read carries the rows' path, since and blocks, and only the read
 * can tell that an item was resolved elsewhere — which is the one case the quiet notice names.
 * Every page the person already opened is read again, so a row resolved above the fold does not
 * shift the ones below it.
 */
export async function handleNeedsOperatorChanged(): Promise<void> {
  const state = get(attentionStore);
  const pages = Math.max(1, state.page);
  if (!apiClient.isPaired()) {
    return;
  }
  attentionStore.update((current) => ({ ...current, isLoading: true, loadError: null }));
  try {
    const items: AttentionItem[] = [];
    let answer: NeedsOperatorPage = await apiClient.getNeedsOperator(1);
    items.push(...itemsOf(answer.items));
    for (let page = 2; page <= pages; page += 1) {
      answer = await apiClient.getNeedsOperator(page);
      items.push(...itemsOf(answer.items));
    }
    attentionStore.update((current) => ({
      ...current,
      items,
      total: countOf(answer.total, items.length),
      page: Math.max(1, countOf(answer.page, 1)),
      hasOlder: answer.has_older === true,
      loaded: true,
      isLoading: false,
      loadError: null,
      leftNotice: leftOf(state, items) ?? current.leftNotice,
    }));
  } catch (error: unknown) {
    attentionStore.update((current) => ({
      ...current,
      isLoading: false,
      loadError: (error as Error)?.message || 'network_offline',
    }));
  }
}

