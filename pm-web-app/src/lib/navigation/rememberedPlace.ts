/**
 * The place the operator left: the project, the grouping and the selected plan or epic.
 *
 * `plan-overview.adoc#persistence` states what it holds and what it never holds: never the mode
 * (running or ended) and never the follow mode, so the rail always opens running work. The web app
 * keeps it per browser, in its local storage beside its device secret, under the keys the overview
 * has always used: `pm_overview_selected_project` for the project, `pm_overview_view_mode` for the
 * grouping, and `pm_overview_selected_object` for the selected plan or epic.
 *
 * The address wins over the remembered place. It fills an address at two entry points only
 * (`navigation.adoc#url`): a load the app did not create, at `/` or at the bare `/overview`, by a
 * replace; and a click on Running work, by a push of the filled address. A full address, the lens's
 * own list (`/?lens=waits`) and the list Up or the breadcrumb's lens segment push are never
 * filled.
 */

import type { Address } from '../routes';

/** The grouping of the overview's list: by plan or by epic. */
export type PlaceGrouping = 'plan' | 'epic';

/** What the overview remembers about where the operator was. */
export interface RememberedPlace {
  /** The selected project of Running work. */
  project?: string | null;
  /** The grouping of its list. */
  group?: PlaceGrouping;
  /** The selected plan of the project. */
  planId?: string | null;
  /** The selected epic of the project. */
  epicId?: string | null;
}

/** The local-storage keys of the remembered place. */
export const PLACE_KEYS = {
  project: 'pm_overview_selected_project',
  group: 'pm_overview_view_mode',
  object: 'pm_overview_selected_object',
} as const;

/** The facts a fall-back needs: what the runtime knows about the remembered objects. */
export interface PlaceFacts {
  /** Whether the remembered plan or epic has ended; only a confirmed running object opens. */
  planEnded?: boolean;
  /** Whether the remembered project still has running work. */
  projectRuns?: boolean;
  /** Whether any work runs or waits, for the start rule's own fallback. */
  workRunning: boolean;
}

/** The place a stored selection names, or nothing when the storage is empty or unreadable. */
export interface StoredObject {
  planId?: string | null;
  epicId?: string | null;
}

/**
 * Reads the remembered place.
 *
 * A storage that cannot be read (no local storage, a denied origin) answers with an empty place:
 * the app then opens its start rule, never a guessed one.
 *
 * @param storage the browser's local storage
 * @returns the place, with only the keys that hold something
 */
export function readRememberedPlace(storage: Storage | undefined = globalThis.localStorage): RememberedPlace {
  const place: RememberedPlace = {};
  try {
    const project = storage?.getItem(PLACE_KEYS.project);
    if (project) place.project = project;
    const group = storage?.getItem(PLACE_KEYS.group);
    place.group = group === 'epic' ? 'epic' : 'plan';
  } catch {
    // A storage that cannot be read is no place at all: the start rule decides instead.
    return {};
  }
  try {
    const object = storage?.getItem(PLACE_KEYS.object);
    if (object) {
      const stored = JSON.parse(object) as StoredObject;
      if (stored.planId) place.planId = stored.planId;
      if (stored.epicId) place.epicId = stored.epicId;
    }
  } catch {
    // A malformed selected object is dropped; the project and the grouping stay readable.
  }
  return place;
}

/**
 * Writes the remembered place.
 *
 * The mode and the follow mode are not part of it: only the project, the grouping and the selected
 * object are kept, so the rail opens running work.
 *
 * @param place the place to keep
 * @param storage the browser's local storage
 */
export function writeRememberedPlace(
  place: RememberedPlace,
  storage: Storage | undefined = globalThis.localStorage,
): void {
  try {
    if (place.project) {
      storage?.setItem(PLACE_KEYS.project, place.project);
    } else {
      storage?.removeItem(PLACE_KEYS.project);
    }
    storage?.setItem(PLACE_KEYS.group, place.group === 'epic' ? 'epic' : 'plan');
    if (place.planId || place.epicId) {
      const object: StoredObject = {};
      if (place.planId) object.planId = place.planId;
      if (place.epicId) object.epicId = place.epicId;
      storage?.setItem(PLACE_KEYS.object, JSON.stringify(object));
    } else {
      storage?.removeItem(PLACE_KEYS.object);
    }
  } catch {
    // A storage that refuses the write loses the memory, never the view it is looking at.
  }
}

/**
 * The address the remembered place names, with its fall-backs.
 *
 * A remembered plan or epic that has ended falls back to its project; a project that no longer has
 * running work falls back to the list of projects.
 *
 * @param place the remembered place
 * @param facts what the runtime knows about those objects
 * @returns the address to open, or `null` when the place is empty
 */
export function addressOfPlace(place: RememberedPlace, facts: PlaceFacts): Address | null {
  // A plan cannot run in a project that runs nothing, so a project without running work sends the
  // place to the list of projects even when the plan was read as running.
  const running = facts.planEnded === false && facts.projectRuns !== false;
  if (running && place.planId) {
    return { route: 'plan', planId: place.planId };
  }
  if (running && place.epicId) {
    return { route: 'epic', epicId: place.epicId };
  }
  if (place.project) {
    // Running work is the default mode, so a project that still runs opens the lens scoped to it,
    // in the grouping the operator left.
    if (facts.projectRuns === false) return { route: 'projects' };
    const address: Address = { route: 'overview', project: place.project };
    if (place.group === 'epic') address.group = 'epic';
    return address;
  }
  return null;
}

/** The bare addresses the remembered place may fill: `/` and the unfilled `/overview`. */
export function isFillableStart(address: Address | null): boolean {
  if (!address) return false;
  if (address.route === null) return address.lens === undefined;
  return (
    address.route === 'overview' &&
    !address.project &&
    !address.mode &&
    !address.group &&
    !address.follow &&
    !address.open &&
    !address.chat
  );
}

/** What the start rule reads. */
export interface StartInput {
  /** Whether the load carries the app's mark; `false` for a typed address, a bookmark, a new tab. */
  fromOutside: boolean;
  /** The address the bar names, or `null` for none of the app's routes. */
  requested: Address | null;
  /** Whether the machine is onboarded. */
  onboarded: boolean;
  /** The remembered place. */
  place: RememberedPlace;
  /** What the runtime knows about the remembered objects. */
  facts: PlaceFacts;
}

/**
 * The one start rule of the app.
 *
 * The remembered place fills the start at a mark-less load, at `/` or at the bare `/overview`. A
 * full address opens what it names, and `/?lens=waits` is such an address. Without a place the app
 * opens what waits, and until PLAN-49 registers `/` the present rule stands: the overview while
 * work runs or waits, the projects otherwise.
 *
 * @param input what the start reads
 * @returns the address to open
 */
export function startAddress(input: StartInput): Address {
  if (!input.onboarded) return { route: 'onboarding' };
  const requested = input.requested;
  if (requested?.route === 'onboarding') return requested;
  const present = (): Address => ({ route: input.facts.workRunning ? 'overview' : 'projects' });
  if (requested && requested.route !== 'pair' && requested.route !== 'entry') {
    // `/?lens=waits` names its lens, so it is a full address and is never filled.
    if (isFillableStart(requested)) {
      if (input.fromOutside) {
        const filled = addressOfPlace(input.place, input.facts);
        if (filled) return filled;
      }
      // Without a place the start is what waits; until PLAN-49 registers `/`, the present rule.
      return present();
    }
    return requested;
  }
  return present();
}

/**
 * The address a click on Running work opens: the lens, filled from the remembered place.
 *
 * The rail and the phone's bottom bar both call it. It opens the lens in its running mode, scoped
 * to the remembered project, by a push — never the mode the operator left, so a click after ended
 * work opens running work.
 *
 * @param place the remembered place
 * @param facts what the runtime knows about it
 * @returns the address to push
 */
export function runningWorkAddress(place: RememberedPlace, facts: PlaceFacts): Address {
  const project = place.project;
  if (!project) return unfilledLensAddress();
  // A project without running work falls back to the list of projects, exactly as the fill does.
  if (facts.projectRuns === false) return { route: 'projects' };
  return { route: 'overview', project };
}

/**
 * The lens's own list, with nothing selected: the address Up and the breadcrumb's lens segment
 * push, and a reload of it stays unfilled because the pushed entry carries the app's mark.
 *
 * @returns the unfilled list of Running work
 */
export function unfilledLensAddress(): Address {
  return { route: 'overview' };
}
