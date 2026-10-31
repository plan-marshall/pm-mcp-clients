import { writable } from 'svelte/store';
import { apiClient, type FindKind, type FindMatch, type FindPage } from '../api/client';

export type { FindKind, FindMatch, FindPage };

/**
 * The search index of "go to work by name" (UC-PLN-8, UX-P-02).
 *
 * It is the client of the runtime's `GET /api/v1/find?q=` and nothing else: it sends the text the
 * operator typed after a pause, cancels the read a newer text supersedes, and keeps the matches in
 * the order served. It builds no index from the stores and loads no archive — the runtime answers
 * over running and ended work, and the client shows what comes back.
 */

/** How long the text must pause before it is sent, in milliseconds. */
export const SEARCH_DEBOUNCE_MS = 200;

/** What the index is doing with the text it holds. */
export type SearchStatus =
  /** No text: the field carries its hint, and no read was sent. */
  | 'idle'
  /** The text is waiting for its pause, or its read is on the way. */
  | 'searching'
  /** The read answered; `matches` is what the runtime served. */
  | 'done'
  /** The read failed; an empty `matches` is not an empty answer. */
  | 'failed';

/** The state of the one search the palette shows. */
export interface SearchIndexState {
  /** The text the matches belong to; empty while the field is empty. */
  text: string;
  /** The matches, in the runtime's order. */
  matches: FindMatch[];
  /** What the index is doing with the text. */
  status: SearchStatus;
}

/** The text of an empty field: the hint, never a request. */
const EMPTY: SearchIndexState = { text: '', matches: [], status: 'idle' };

/** The one search every view of the palette reads. */
export const searchIndexStore = writable<SearchIndexState>({ ...EMPTY });

/** The timer of the pause in the typing. */
let pauseTimer: ReturnType<typeof setTimeout> | null = null;

/** The read on its way, aborted when a newer text supersedes it. */
let inFlight: AbortController | null = null;

/** The read this index currently wants; an older answer never replaces a newer one. */
let sequence = 0;

/**
 * Stops the pause and the read on their way.
 *
 * A read that is already gone is not an error: its answer is ignored by the sequence check.
 */
function cancelPending(): void {
  if (pauseTimer !== null) {
    clearTimeout(pauseTimer);
    pauseTimer = null;
  }
  if (inFlight !== null) {
    inFlight.abort();
    inFlight = null;
  }
}

/**
 * @param matches the matches, in the runtime's order
 * @returns one group per run of consecutive matches of the same kind and endedness
 */
export function groupFindMatches(
  matches: FindMatch[],
): Array<{ kind: FindKind; ended: boolean; matches: FindMatch[] }> {
  const groups: Array<{ kind: FindKind; ended: boolean; matches: FindMatch[] }> = [];
  for (const match of matches) {
    const last = groups[groups.length - 1];
    if (last && last.kind === match.kind && last.ended === match.ended) {
      last.matches.push(match);
    } else {
      groups.push({ kind: match.kind, ended: match.ended, matches: [match] });
    }
  }
  return groups;
}

/**
 * Sends one text to the runtime and keeps its answer, if no newer text was typed since.
 *
 * @param text the text to look for
 * @param request the sequence this read belongs to
 */
async function read(text: string, request: number): Promise<void> {
  const controller = new AbortController();
  inFlight = controller;
  try {
    const page: FindPage = await apiClient.findWork(text, controller.signal);
    if (request !== sequence) {
      return;
    }
    searchIndexStore.set({ text, matches: page.matches ?? [], status: 'done' });
  } catch {
    if (request !== sequence || controller.signal.aborted) {
      return;
    }
    searchIndexStore.set({ text, matches: [], status: 'failed' });
  } finally {
    if (inFlight === controller) {
      inFlight = null;
    }
  }
}

/**
 * Takes the text the operator typed and schedules its read after the pause in the typing.
 *
 * An empty text sends nothing: the field returns to its hint and the matches go with the text they
 * belonged to.
 *
 * @param text the whole text of the one field
 */
export function setSearchText(text: string): void {
  const query = text.trim();
  cancelPending();
  sequence += 1;
  if (query.length === 0) {
    searchIndexStore.set({ ...EMPTY });
    return;
  }
  searchIndexStore.set({ text: query, matches: [], status: 'searching' });
  const request = sequence;
  pauseTimer = setTimeout(() => {
    pauseTimer = null;
    void read(query, request);
  }, SEARCH_DEBOUNCE_MS);
}

/**
 * Forgets the search when the panel closes, so the next open starts at the hint.
 */
export function resetSearchIndex(): void {
  cancelPending();
  sequence += 1;
  searchIndexStore.set({ ...EMPTY });
}

/** Whether the command palette is open over the view. */
export const commandPaletteOpen = writable(false);

/**
 * Opens the command palette over the view the operator is on; it is never a route.
 */
export function openCommandPalette(): void {
  commandPaletteOpen.set(true);
}

/**
 * Closes the command palette and forgets the search it held.
 */
export function closeCommandPalette(): void {
  resetSearchIndex();
  commandPaletteOpen.set(false);
}
