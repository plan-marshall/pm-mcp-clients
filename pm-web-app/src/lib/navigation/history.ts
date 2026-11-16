/**
 * The one writer of the address bar.
 *
 * Every entry the app creates carries the app's mark in `history.state`, so a load without the
 * mark is the app opened from outside it (a typed address, a bookmark, a new tab) and may be
 * filled from the remembered place. `navigation.adoc#url` states which changes push an entry and
 * which replace the current one, and this module is the only place that decides it:
 *
 * - a change of depth (a child opened, Up, a breadcrumb segment) and a change of lens push;
 * - opening an object in place pushes, so the history's back closes it;
 * - a sibling in the same column, a `j`/`k` move, a tab, a node, a mode or a scope, and the
 *   opening or hiding of a side column replace;
 * - every selection the app makes on its own (the fallback after a deletion, filling a bare lens,
 *   the next decision) replaces, even when it changes the depth.
 *
 * One write does not change the address: `openedFromOutside` re-applies the mark to the current
 * entry when the framework dropped it on hydration, so a reload of the app's own entry stays the
 * app's own. A pairing code lives in the fragment and is redeemed before the first write; no write
 * of this module ever puts a fragment in the address, so the code never reaches the history.
 */

import { type Address, type ObjectTab, formatAddress, parseAddress } from '../routes';

/** The key of the app's mark inside `history.state`. */
export const APP_MARK = 'pmApp';

/** How a write changes the history: one new entry, or the current one replaced. */
export type WriteIntent = 'push' | 'replace';

/** The state every entry the app writes carries. */
interface MarkedState {
  path: string;
  [APP_MARK]: true;
}

/**
 * Whether a history state carries the app's mark.
 *
 * Read once at start: a load whose state has none is the app opened from outside, and filling it
 * from the remembered place is allowed. A write during the same load never changes that answer.
 *
 * @param state the state read from `history.state`, or another state to test
 * @returns `true` when the app created the entry
 */
export function carriesAppMark(state: unknown = history.state): boolean {
  return typeof state === 'object' && state !== null && (state as Record<string, unknown>)[APP_MARK] === true;
}

/**
 * Whether this load was opened from outside the app: read once, at start, and remembered.
 *
 * A fresh document load answers from `history.state` before the app writes anything, so a write of
 * this same load (the pairing fragment dropped, the first address written) never changes it. A
 * reload or a history traversal of an entry the app wrote answers `false` from the mark the entry
 * carried at start (`initialMarked`), because SvelteKit replaces `history.state` on client
 * hydration: the answer is fixed for the load, and the mark is re-applied to the current entry so
 * the next reload of it stays the app's own (a reload of an unfilled lens stays unfilled).
 *
 * @returns `true` when the load carried no mark of the app
 */
export function openedFromOutside(): boolean {
  startMark ??= !carriesAppMark();
  if (
    startMark === false &&
    typeof window !== 'undefined' &&
    typeof history !== 'undefined' &&
    !carriesAppMark(history.state)
  ) {
    window.history.replaceState(
      {
        ...(typeof history.state === 'object' && history.state !== null ? history.state : {}),
        path: `${window.location.pathname}${window.location.search}`,
        [APP_MARK]: true,
      },
      '',
    );
  }
  return startMark ?? false;
}

/**
 * Whether the current document load came from a browser reload or history traversal rather than a
 * fresh document navigation (`navigate`).
 *
 * @returns `true` when the navigation timing entry is `'reload'` or `'back_forward'`
 */
function isReloadOrHistoryTraversal(): boolean {
  if (typeof performance === 'undefined' || typeof performance.getEntriesByType !== 'function') {
    return false;
  }
  const nav = performance.getEntriesByType('navigation')[0] as
    | PerformanceNavigationTiming
    | undefined;
  return nav?.type === 'reload' || nav?.type === 'back_forward';
}

/** The answer of the first `openedFromOutside` of this load. */
const initialMarked =
  typeof history !== 'undefined' &&
  isReloadOrHistoryTraversal() &&
  carriesAppMark(history.state);
let startMark: boolean | null = initialMarked ? false : null;

/**
 * Forgets the answer of `openedFromOutside`, for a test that simulates another load of the app.
 */
export function resetOpenedFromOutside(): void {
  startMark = null;
}

/**
 * The address of a location, or `null` when its path names none of the app's routes.
 *
 * @param pathname the path of the location
 * @param search the query of the location
 * @returns the address, or `null` for a path the app cannot open
 */
export function addressOf(pathname: string, search = ''): Address | null {
  return parseAddress(`${pathname}${search}`);
}

/**
 * The address the address bar shows.
 *
 * @returns the address, or `null` when the bar names none of the app's routes
 */
export function currentAddress(): Address | null {
  return addressOf(window.location.pathname, window.location.search);
}

/**
 * The level an address names, as the chain of columns orders it.
 *
 * A deeper level pushes when it opens and when it is left through Up; two addresses of one level
 * are siblings, and selecting between them replaces.
 *
 * @param address the address
 * @returns the depth of its object
 */
export function levelOf(address: Address): number {
  switch (address.route) {
    case null:
      return 0;
    case 'task':
      return 3;
    case 'plan':
    case 'epic':
    case 'project':
    case 'decision':
    case 'report':
    case 'cleanup':
      return 2;
    default:
      return 1;
  }
}

/**
 * The lens an address belongs to: the rail entry it keeps.
 *
 * @param address the address
 * @returns the name of its lens
 */
export function lensOf(address: Address): string {
  switch (address.route) {
    case null:
      return 'waits';
    case 'overview':
    case 'plan':
    case 'epic':
    case 'task':
      return 'running';
    case 'projects':
    case 'project':
      return 'projects';
    case 'decisions':
    case 'decision':
      return 'decisions';
    case 'reports':
    case 'report':
      return 'reports';
    case 'settings':
    case 'machine-settings':
      return 'settings';
    case 'system':
    case 'cleanup':
      return 'system';
    case 'designer':
      return 'designer';
    case 'start':
      return 'start';
    default:
      return address.route;
  }
}

/**
 * Whether a change between two addresses pushes a history entry or replaces the current one.
 *
 * @param previous the address the app showed, or `null` for its first write
 * @param next the address it is about to show
 * @returns the intent the change carries
 */
export function intentFor(previous: Address | null, next: Address): WriteIntent {
  if (!previous) return 'replace';
  if (lensOf(previous) !== lensOf(next)) return 'push';
  if (levelOf(previous) !== levelOf(next)) return 'push';
  if (next.open && !previous.open) return 'push';
  return 'replace';
}

/**
 * Whether the address bar already shows this address.
 *
 * @param path the address to test
 * @returns `true` when path and query both match
 */
function shows(path: string): boolean {
  return `${window.location.pathname}${window.location.search}${window.location.hash}` === path;
}

/**
 * Writes an address to the address bar, marked as the app's own.
 *
 * @param address the address to show
 * @param intent whether to push a new entry or replace the current one
 * @returns `true` when the bar changed; `false` for an address that has no path
 */
export function writeAddress(address: Address, intent: WriteIntent): boolean {
  const path = formatAddress(address);
  if (!path) return false;
  if (shows(path)) return false;
  const state: MarkedState = { path, [APP_MARK]: true };
  if (intent === 'push') {
    window.history.pushState(state, '', path);
  } else {
    window.history.replaceState(state, '', path);
  }
  return true;
}

/**
 * The address a selection falls back to when its object was deleted while it was shown.
 *
 * The fallback is the parent in the chain — a task to its plan, a plan or an epic to the lens,
 * one decision or report to its list, the project page to the projects — and it replaces the
 * address rather than pushing, so Back never returns to the deleted object.
 *
 * @param address the address whose object went away
 * @returns the parent's address, or `null` when it already names the top of its chain
 */
export function parentOf(address: Address): Address | null {
  switch (address.route) {
    case 'task': {
      if (!address.planId) return null;
      const parent: Address = { route: 'plan', planId: address.planId };
      if (address.mode) parent.mode = address.mode;
      return parent;
    }
    case 'plan':
    case 'epic': {
      const parent: Address = { route: 'overview' };
      if (address.project) parent.project = address.project;
      if (address.mode) parent.mode = address.mode;
      return parent;
    }
    case 'decision':
      return { route: 'decisions' };
    case 'report':
      return { route: 'reports' };
    case 'project':
      return { route: 'projects' };
    default:
      return null;
  }
}

/**
 * The address a tab switch writes: the chosen tab, and the other tab's key dropped.
 *
 * The view keeps the other tab's selection in memory while the object stays open; the address
 * carries only the shown tab, so a reload never opens a key of a tab that is not there.
 *
 * @param address the address of the object
 * @param tab the tab being opened
 * @returns the address of that tab
 */
export function switchTab(address: Address, tab: ObjectTab): Address {
  const next: Address = { ...address, tab };
  delete next.node;
  delete next.file;
  return next;
}
