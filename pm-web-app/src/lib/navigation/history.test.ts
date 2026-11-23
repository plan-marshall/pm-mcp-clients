import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Address } from '../routes';
import { formatAddress, parseAddress } from '../routes';
import {
  APP_MARK,
  addressOf,
  carriesAppMark,
  currentAddress,
  intentFor,
  levelOf,
  lensOf,
  parentOf,
  switchTab,
  writeAddress,
} from './history';

/** The plan of a project, the object every case of the plan's own column starts from. */
const PLAN: Address = { route: 'plan', planId: 'fix-relay' };

/** The overview of the Running work lens. */
const OVERVIEW: Address = { route: 'overview', project: 'pm-mcp-clients' };

describe('the writer of the address', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/');
  });

  it('marks every entry it writes', () => {
    expect(writeAddress(PLAN, 'push')).toBe(true);
    expect(carriesAppMark()).toBe(true);
    expect(window.history.state).toMatchObject({ path: '/plans/fix-relay', [APP_MARK]: true });

    expect(writeAddress({ ...PLAN, tab: 'intent' }, 'replace')).toBe(true);
    expect(carriesAppMark()).toBe(true);
    expect(window.history.state).toMatchObject({ [APP_MARK]: true });
  });

  it('reads the mark of a load the app did not create as absent', () => {
    // A typed address, a bookmark or a new tab: the app has written nothing yet.
    expect(carriesAppMark()).toBe(false);
    expect(carriesAppMark({ path: '/plans/fix-relay' })).toBe(false);
    expect(carriesAppMark({ [APP_MARK]: true })).toBe(true);
  });

  it('leaves the bar alone when it already shows the address', () => {
    expect(writeAddress(PLAN, 'push')).toBe(true);
    expect(writeAddress(PLAN, 'push')).toBe(false);
    expect(writeAddress(PLAN, 'replace')).toBe(false);
  });

  it('never writes the pairing code in the fragment', () => {
    window.location.hash = '#code=abc-123';
    writeAddress(PLAN, 'push');

    // The address the writer stores is path and query only: the code lives in the fragment, is
    // redeemed before the first write, and never reaches the history.
    expect(window.history.state).toMatchObject({ path: '/plans/fix-relay' });
    expect(JSON.stringify(window.history.state)).not.toContain('code=');
  });

  it('keeps every pushed selection readable, so back and forward restore it', () => {
    // Each entry carries the path of the selection it holds: the popstate handler reads it back
    // into the store, so the browser's back and forward move through the selections.
    writeAddress(OVERVIEW, 'replace');
    writeAddress(PLAN, 'push');
    const task: Address = { route: 'task', planId: 'fix-relay', taskId: 'TASK-4' };
    writeAddress(task, 'push');

    expect(addressOf('/plans/fix-relay/tasks/TASK-4')).toEqual(task);
    expect(addressOf('/plans/fix-relay')).toEqual({
      route: 'plan',
      planId: 'fix-relay',
    });
    expect(addressOf('/overview?project=pm-mcp-clients')).toEqual({
      route: 'overview',
      project: 'pm-mcp-clients',
    });
  });

  it('reports the address the bar shows', () => {
    window.history.replaceState(null, '', '/plans/fix-relay?tab=intent');
    expect(currentAddress()).toEqual({ route: 'plan', planId: 'fix-relay', tab: 'intent' });
    // The root is the list of what waits, an address of the scheme like any other.
    window.history.replaceState(null, '', '/');
    expect(currentAddress()).toEqual({ route: 'inbox' });
    window.history.replaceState(null, '', '/nowhere');
    expect(currentAddress()).toBeNull();
  });
});

describe('the push and replace rule', () => {
  it('pushes every change of depth', () => {
    // A child opened and Up are both depth changes, so both push.
    expect(intentFor(OVERVIEW, PLAN)).toBe('push');
    expect(intentFor(PLAN, OVERVIEW)).toBe('push');
    expect(intentFor(PLAN, { route: 'task', planId: 'fix-relay', taskId: 'TASK-4' })).toBe('push');
    expect(intentFor({ route: 'task', planId: 'p', taskId: 't' }, { route: 'plan', planId: 'p' })).toBe(
      'push',
    );
  });

  it('pushes every change of lens', () => {
    expect(intentFor(PLAN, { route: 'decisions' })).toBe('push');
    expect(intentFor({ route: 'decisions' }, { route: 'reports' })).toBe('push');
    expect(intentFor({ route: 'projects' }, OVERVIEW)).toBe('push');
  });

  it('pushes an object opened in place', () => {
    const opened: Address = { ...PLAN, open: { kind: 'decision', id: 'Q-007' } };
    expect(intentFor(PLAN, opened)).toBe('push');
  });

  it('replaces a sibling in the same column, and a j or k move', () => {
    expect(intentFor(PLAN, { ...PLAN, planId: 'fix-landing' })).toBe('replace');
    expect(intentFor(OVERVIEW, { ...OVERVIEW, project: 'other' })).toBe('replace');
    expect(intentFor({ route: 'decisions' }, { route: 'decisions' })).toBe('replace');
  });

  it('replaces a tab, a node, a mode, a scope and the chat', () => {
    expect(intentFor(PLAN, { ...PLAN, tab: 'changes' })).toBe('replace');
    expect(intentFor(PLAN, { ...PLAN, node: 'execute/land' })).toBe('replace');
    expect(intentFor(PLAN, { ...PLAN, mode: 'ended' })).toBe('replace');
    expect(intentFor(OVERVIEW, { ...OVERVIEW, group: 'epic' })).toBe('replace');
    expect(intentFor(PLAN, { ...PLAN, chat: true })).toBe('replace');
  });

  it('replaces the first write of a load', () => {
    expect(intentFor(null, PLAN)).toBe('replace');
  });

  it('orders the objects of a lens by level', () => {
    expect(levelOf({ route: null, lens: 'waits' })).toBe(0);
    expect(levelOf(OVERVIEW)).toBe(1);
    expect(levelOf(PLAN)).toBe(2);
    expect(levelOf({ route: 'task', planId: 'p', taskId: 't' })).toBe(3);
    expect(lensOf(PLAN)).toBe('running');
    expect(lensOf({ route: 'project', projectId: 'p' })).toBe('projects');
    expect(lensOf({ route: 'decision', decisionId: 'Q-1' })).toBe('decisions');
    expect(lensOf({ route: 'report', reportId: 'r-1' })).toBe('reports');
  });
});

describe('an object deleted while it is shown', () => {
  it('falls back to its parent in the chain', () => {
    expect(parentOf({ route: 'task', planId: 'fix-relay', taskId: 'TASK-4' })).toEqual({
      route: 'plan',
      planId: 'fix-relay',
    });
    expect(parentOf(PLAN)).toEqual({ route: 'overview' });
    expect(parentOf({ ...PLAN, project: 'pm-mcp-clients' })).toEqual({
      route: 'overview',
      project: 'pm-mcp-clients',
    });
    expect(parentOf({ route: 'decision', decisionId: 'Q-007' })).toEqual({ route: 'decisions' });
    expect(parentOf({ route: 'report', reportId: 'r-42' })).toEqual({ route: 'reports' });
    expect(parentOf({ route: 'project', projectId: 'p' })).toEqual({ route: 'projects' });
    expect(parentOf({ route: 'overview' })).toBeNull();
  });

  it('replaces the address instead of pushing, so Back never returns to it', () => {
    const parent = parentOf({ route: 'task', planId: 'fix-relay', taskId: 'TASK-4' })!;
    window.history.replaceState({ path: '/plans/fix-relay/tasks/TASK-4', [APP_MARK]: true }, '', '/plans/fix-relay/tasks/TASK-4');
    writeAddress(parent, 'replace');

    expect(window.location.pathname).toBe('/plans/fix-relay');
    expect(parseAddress(window.location.pathname)).toEqual({
      route: 'plan',
      planId: 'fix-relay',
    });
  });
});

describe('a tab switch', () => {
  it('writes the chosen tab and drops the other tab’s key', () => {
    const changes: Address = { ...PLAN, tab: 'changes', file: 'pm-relay/pom.xml' };
    const process = switchTab(changes, 'process');

    expect(process.tab).toBe('process');
    expect(process.file).toBeUndefined();
    expect(process.node).toBeUndefined();
    expect(formatAddress(process)).toBe('/plans/fix-relay');

    const back = switchTab(process, 'changes');
    expect(back.node).toBeUndefined();
  });

  it('replaces the address rather than pushing', () => {
    expect(intentFor(PLAN, switchTab(PLAN, 'intent'))).toBe('replace');
  });
});

describe('the mark of a load', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState(null, '', '/');
    vi.resetModules();
  });

  it('treats a load without the mark as opened from outside and writes none', async () => {
    const { openedFromOutside } = await import('./history');

    expect(openedFromOutside()).toBe(true);
    expect(window.history.state).toBeNull();
  });

  it('answers the app’s own for an entry that still carries the mark', async () => {
    window.history.replaceState({ path: '/overview', [APP_MARK]: true }, '', '/overview');

    const { openedFromOutside } = await import('./history');

    expect(openedFromOutside()).toBe(false);
  });

  it.each(['reload', 'back_forward'] as const)(
    're-applies the mark the framework dropped on hydration of a %s, by a replace',
    async (type) => {
      window.history.replaceState({ path: '/overview', [APP_MARK]: true }, '', '/overview');
      vi.spyOn(performance, 'getEntriesByType').mockReturnValue([
        { type } as unknown as PerformanceNavigationTiming,
      ]);
      const { openedFromOutside } = await import('./history');

      const replaceSpy = vi.spyOn(window.history, 'replaceState');
      const pushSpy = vi.spyOn(window.history, 'pushState');
      // SvelteKit replaces `history.state` on client hydration, so the mark is gone from the entry.
      window.history.replaceState({ from: 'sveltekit' }, '', '/overview');
      replaceSpy.mockClear();

      expect(openedFromOutside()).toBe(false);
      expect(window.history.state).toMatchObject({
        from: 'sveltekit',
        path: '/overview',
        [APP_MARK]: true,
      });
      // The address does not change: the entry is the same one, marked again.
      expect(replaceSpy).toHaveBeenCalledTimes(1);
      expect(pushSpy).not.toHaveBeenCalled();
    },
  );

  it('keeps a fresh navigation’s answer even after the app writes the mark', async () => {
    const { openedFromOutside, writeAddress: write } = await import('./history');

    expect(openedFromOutside()).toBe(true);
    write(OVERVIEW, 'replace');

    expect(carriesAppMark()).toBe(true);
    expect(openedFromOutside()).toBe(true);
  });

  it('returns the answer to the load’s own state after a reset', async () => {
    window.history.replaceState({ path: '/overview', [APP_MARK]: true }, '', '/overview');
    vi.spyOn(performance, 'getEntriesByType').mockReturnValue([
      { type: 'reload' } as unknown as PerformanceNavigationTiming,
    ]);
    const { openedFromOutside, resetOpenedFromOutside } = await import('./history');
    expect(openedFromOutside()).toBe(false);

    // The framework dropped the mark, and a test asks for another load of the app: the load began
    // as a reload of the app's own entry, so it still answers so.
    window.history.replaceState({ from: 'sveltekit' }, '', '/overview');
    resetOpenedFromOutside();

    expect(openedFromOutside()).toBe(false);
  });

  it('closes and replaces open object on Address', async () => {
    const { closeOpenObject, replaceOpenObject } = await import('./history');
    const base = { route: 'plan', planId: 'PLAN-1', open: { kind: 'task', id: 'TSK-1' } } as const;
    const closed = closeOpenObject(base as any);
    expect(closed.open).toBeUndefined();
    expect(closed.planId).toBe('PLAN-1');

    const replaced = replaceOpenObject(base as any, { kind: 'decision', id: 'DEC-2' });
    expect(replaced.open).toEqual({ kind: 'decision', id: 'DEC-2' });
    expect(replaced.planId).toBe('PLAN-1');
  });
});
