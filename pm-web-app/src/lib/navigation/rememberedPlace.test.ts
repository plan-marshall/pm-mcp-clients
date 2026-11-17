import { beforeEach, describe, expect, it } from 'vitest';
import type { Address } from '../routes';
import {
  PLACE_KEYS,
  addressOfPlace,
  isFillableStart,
  readRememberedPlace,
  runningWorkAddress,
  startAddress,
  unfilledLensAddress,
  writeRememberedPlace,
  type PlaceFacts,
  type RememberedPlace,
} from './rememberedPlace';

/** Work that runs, and a remembered plan that still runs in a project that still runs. */
const RUNNING: PlaceFacts = { planEnded: false, projectRuns: true, workRunning: true };

/** A machine with nothing running and nothing waiting. */
const IDLE: PlaceFacts = { workRunning: false };

/** A place that remembers a plan of a project. */
const PLACE: RememberedPlace = {
  project: 'pm-mcp-clients',
  group: 'plan',
  planId: 'fix-relay',
};

describe('the remembered place', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('keeps the project, the grouping and the selected object under the existing keys', () => {
    writeRememberedPlace(PLACE);

    expect(window.localStorage.getItem(PLACE_KEYS.project)).toBe('pm-mcp-clients');
    expect(window.localStorage.getItem(PLACE_KEYS.group)).toBe('plan');
    expect(JSON.parse(window.localStorage.getItem(PLACE_KEYS.object) ?? '{}')).toEqual({
      planId: 'fix-relay',
    });
    expect(readRememberedPlace()).toEqual(PLACE);
  });

  it('never remembers the mode or the follow mode', () => {
    // The place has no field for either: a mode that was open is not kept, so the rail opens
    // running work.
    writeRememberedPlace({ ...PLACE, project: 'pm-mcp-clients' });

    const stored = Object.entries(window.localStorage).map(([key, value]) => `${key}=${value}`);
    expect(stored.join('&')).not.toContain('ended');
    expect(stored.join('&')).not.toContain('follow');
    expect(readRememberedPlace()).not.toHaveProperty('mode');
  });

  it('answers with an empty place for storage that holds nothing or cannot be read', () => {
    expect(readRememberedPlace()).toEqual({ group: 'plan' });

    window.localStorage.setItem(PLACE_KEYS.object, 'not json');
    expect(readRememberedPlace()).toEqual({ group: 'plan' });
  });

  it('falls back from an ended plan to its project', () => {
    expect(addressOfPlace(PLACE, { ...RUNNING, planEnded: true })).toEqual({
      route: 'overview',
      project: 'pm-mcp-clients',
    });
    // An unread plan is never opened: only a confirmed running object opens.
    expect(addressOfPlace(PLACE, { ...RUNNING, planEnded: undefined })).toEqual({
      route: 'overview',
      project: 'pm-mcp-clients',
    });
  });

  it('falls back from an epic the same way', () => {
    expect(addressOfPlace({ project: 'p', epicId: 'relay-hardening' }, RUNNING)).toEqual({
      route: 'epic',
      epicId: 'relay-hardening',
    });
    expect(
      addressOfPlace({ project: 'p', epicId: 'relay-hardening' }, { ...RUNNING, planEnded: true }),
    ).toEqual({ route: 'overview', project: 'p' });
  });

  it('falls back from a project without running work to the list of projects', () => {
    expect(addressOfPlace(PLACE, { ...RUNNING, projectRuns: false })).toEqual({
      route: 'projects',
    });
  });

  it('names no address for an empty place', () => {
    expect(addressOfPlace({ group: 'plan' }, RUNNING)).toBeNull();
  });
});

describe('the start rule', () => {
  it('fills a mark-less load at / and at the bare /overview from the place', () => {
    for (const requested of [{ route: 'inbox' } as Address, { route: 'overview' } as Address]) {
      expect(
        startAddress({
          fromOutside: true,
          requested,
          onboarded: true,
          place: PLACE,
          facts: RUNNING,
        }),
      ).toEqual({ route: 'plan', planId: 'fix-relay' });
    }
  });

  it('does not fill a load the app created, so a reload stays where it is', () => {
    expect(
      startAddress({
        fromOutside: false,
        requested: { route: 'overview' },
        onboarded: true,
        place: PLACE,
        facts: RUNNING,
      }),
    ).toEqual({ route: 'overview' });
  });

  it('never fills a full address, nor the lens of what waits', () => {
    const scoped: Address = { route: 'overview', project: 'other' };
    expect(
      startAddress({
        fromOutside: true,
        requested: scoped,
        onboarded: true,
        place: PLACE,
        facts: RUNNING,
      }),
    ).toEqual(scoped);

    const waits: Address = { route: 'inbox', lens: 'waits' };
    expect(
      startAddress({
        fromOutside: true,
        requested: waits,
        onboarded: true,
        place: PLACE,
        facts: RUNNING,
      }),
    ).toEqual(waits);
    expect(isFillableStart(waits)).toBe(false);
  });

  it('never fills an inbox address that carries anything else of the scheme', () => {
    const withOpen: Address = { route: 'inbox', open: { kind: 'plan', id: 'fix-relay' } };

    expect(isFillableStart(withOpen)).toBe(false);
    // The address opens what it names; the remembered place does not replace its object.
    expect(
      startAddress({
        fromOutside: true,
        requested: withOpen,
        onboarded: true,
        place: PLACE,
        facts: RUNNING,
      }),
    ).toEqual(withOpen);
  });

  it('never fills the lens’s own list pushed by Up or the lens segment on a reload', () => {
    // The push carried the app's mark, so a reload of that entry is not a mark-less load.
    expect(
      startAddress({
        fromOutside: false,
        requested: unfilledLensAddress(),
        onboarded: true,
        place: PLACE,
        facts: RUNNING,
      }),
    ).toEqual({ route: 'overview' });
  });

  it('opens what waits without a place, and keeps a bare /overview', () => {
    expect(
      startAddress({
        fromOutside: true,
        requested: { route: 'inbox' },
        onboarded: true,
        place: {},
        facts: RUNNING,
      }),
    ).toEqual({ route: 'inbox' });
    expect(
      startAddress({
        fromOutside: true,
        requested: { route: 'inbox' },
        onboarded: true,
        place: {},
        facts: IDLE,
      }),
    ).toEqual({ route: 'inbox' });
    // A bare `/overview` names the lens, so an operator who opened it stays there.
    expect(
      startAddress({
        fromOutside: true,
        requested: { route: 'overview' },
        onboarded: true,
        place: {},
        facts: RUNNING,
      }),
    ).toEqual({ route: 'overview' });
  });

  it('sends an unonboarded machine to the onboarding and leaves the entry alone', () => {
    expect(
      startAddress({
        fromOutside: true,
        requested: { route: 'inbox' },
        onboarded: false,
        place: PLACE,
        facts: RUNNING,
      }),
    ).toEqual({ route: 'onboarding' });
    expect(
      startAddress({
        fromOutside: true,
        requested: { route: 'pair' },
        onboarded: true,
        place: {},
        facts: RUNNING,
      }),
    ).toEqual({ route: 'inbox' });
  });

  it('opens the filled lens from a click on Running work, and the plain lens without a place', () => {
    // The click opens the lens in running work, scoped to the remembered project: the place never
    // holds the mode, so ended work is not reopened.
    expect(runningWorkAddress(PLACE, RUNNING)).toEqual({
      route: 'overview',
      project: 'pm-mcp-clients',
    });
    expect(runningWorkAddress({}, RUNNING)).toEqual({ route: 'overview' });
    expect(runningWorkAddress(PLACE, { ...RUNNING, projectRuns: false })).toEqual({
      route: 'projects',
    });
    expect(unfilledLensAddress()).toEqual({ route: 'overview' });
    expect(isFillableStart(unfilledLensAddress())).toBe(true);
  });
});
