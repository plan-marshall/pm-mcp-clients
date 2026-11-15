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
    for (const requested of [{ route: null } as Address, { route: 'overview' } as Address]) {
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

    const waits: Address = { route: null, lens: 'waits' };
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

  it('keeps the present rule without a place: the overview while work runs, else the projects', () => {
    expect(
      startAddress({
        fromOutside: true,
        requested: { route: null },
        onboarded: true,
        place: {},
        facts: RUNNING,
      }),
    ).toEqual({ route: 'overview' });
    expect(
      startAddress({
        fromOutside: true,
        requested: { route: null },
        onboarded: true,
        place: {},
        facts: IDLE,
      }),
    ).toEqual({ route: 'projects' });
  });

  it('sends an onboarded machine to the onboarding and leaves the entry alone', () => {
    expect(
      startAddress({
        fromOutside: true,
        requested: { route: null },
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
    ).toEqual({ route: 'overview' });
  });

  it('opens the filled place from a click on Running work, and the plain lens without one', () => {
    expect(runningWorkAddress(PLACE, RUNNING)).toEqual({ route: 'plan', planId: 'fix-relay' });
    expect(runningWorkAddress({}, RUNNING)).toEqual({ route: 'overview' });
    expect(unfilledLensAddress()).toEqual({ route: 'overview' });
    expect(isFillableStart(unfilledLensAddress())).toBe(true);
  });
});
