import { writable, type Writable } from 'svelte/store';

/** The density of a list view on a wide screen. */
export type ViewLayout = 'comfortable' | 'compact' | 'split';

/** The views whose layout choice the browser remembers. */
export type ViewLayoutKey = 'reports' | 'decisions' | 'overview' | 'epic';

/** The layout a view shows when the browser holds no valid choice. */
export const DEFAULT_VIEW_LAYOUT: ViewLayout = 'comfortable';

/** Every layout a view may offer, in the order the toggle shows them. */
export const VIEW_LAYOUTS: readonly ViewLayout[] = ['comfortable', 'compact', 'split'];

/**
 * @param value a stored or chosen value @returns whether it names a layout
 */
export function isViewLayout(value: unknown): value is ViewLayout {
  return value === 'comfortable' || value === 'compact' || value === 'split';
}

/**
 * @param view the view whose choice is stored
 * @returns the browser storage key of that view's layout
 */
export function layoutStorageKey(view: ViewLayoutKey): string {
  return `pm_view_layout_${view}`;
}

/**
 * @returns the browser storage, or null when it is unavailable (server rendering, disabled
 * storage, private mode)
 */
function layoutStorage(): Storage | null {
  try {
    return typeof window === 'undefined' ? null : window.localStorage;
  } catch {
    return null;
  }
}

/**
 * Reads the layout the browser remembered for a view.
 *
 * An unreadable, missing, or unknown value falls back rather than throwing, so a stale key from
 * an older release cannot break the screen.
 *
 * @param view the view whose choice to read
 * @param fallback the layout to use when the browser holds none
 * @returns the stored layout, or the fallback
 */
export function readLayout(
  view: ViewLayoutKey,
  fallback: ViewLayout = DEFAULT_VIEW_LAYOUT,
): ViewLayout {
  const storage = layoutStorage();
  if (!storage) return fallback;
  try {
    const stored = storage.getItem(layoutStorageKey(view));
    return isViewLayout(stored) ? stored : fallback;
  } catch {
    return fallback;
  }
}

/**
 * Remembers a view's layout.
 *
 * Web storage can be full, disabled, or refuse writes in private mode; none of that may break
 * the screen, so a failed write keeps the choice for this visit only.
 *
 * @param view the view whose choice to store
 * @param layout the chosen layout
 */
export function writeLayout(view: ViewLayoutKey, layout: ViewLayout): void {
  const storage = layoutStorage();
  if (!storage) return;
  try {
    storage.setItem(layoutStorageKey(view), layout);
  } catch {
    // Full, disabled, or private-mode storage: the choice still applies to this visit.
  }
}

/**
 * Creates the writable layout store of a view, backed by the browser's storage.
 *
 * @param view the view whose choice this store holds
 * @param fallback the layout to use when the browser holds none
 * @returns a store that persists every change
 */
export function createViewLayout(
  view: ViewLayoutKey,
  fallback: ViewLayout = DEFAULT_VIEW_LAYOUT,
): Writable<ViewLayout> {
  const inner = writable<ViewLayout>(readLayout(view, fallback));
  return {
    subscribe: inner.subscribe,
    set(layout: ViewLayout): void {
      writeLayout(view, layout);
      inner.set(layout);
    },
    update(updater: (layout: ViewLayout) => ViewLayout): void {
      let next: ViewLayout = fallback;
      inner.update((current) => {
        next = updater(current);
        return next;
      });
      writeLayout(view, next);
    },
  };
}
