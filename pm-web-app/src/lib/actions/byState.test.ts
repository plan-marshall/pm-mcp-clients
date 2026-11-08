import { describe, it, expect } from 'vitest';
import {
  ACT_TABLES,
  EPIC_ACTS,
  PLAN_ACTS,
  RESOURCE_KINDS,
  actsFor,
  handledActIds,
  tableActIds,
  type ActSpec,
  type ResourceKind,
} from './byState';

/**
 * The acts-by-state tables (UX-P-04, UC-P-02): every state of every resource kind names its acts,
 * each state has at most one primary act, and a destructive act is never primary and never one of
 * the outlined secondary acts at rest — it lives in the overflow, behind a confirmation.
 */

/** The ids the design names as destructive, whatever the resource kind. */
const DESTRUCTIVE_IDS = new Set([
  'cancel',
  'abandon',
  'release',
  'unenrol',
  'revoke',
  'revoke_others',
]);

/**
 * @param table a state's acts
 * @returns the acts that render as a primary act
 */
function primaries(table: ActSpec[]): ActSpec[] {
  return table.filter((act) => act.role === 'primary');
}

/** Every `[kind, state, acts]` row of every table, so no state escapes the rules. */
const ROWS: Array<{ kind: ResourceKind; state: string; acts: ActSpec[] }> = RESOURCE_KINDS.flatMap(
  (kind) =>
    Object.entries(ACT_TABLES[kind]).map(([state, acts]) => ({
      kind,
      state,
      acts,
    })),
);

/** Every act of every table, with the state it belongs to. */
const EVERY_ACT: Array<{ kind: ResourceKind; state: string; act: ActSpec }> = ROWS.flatMap((row) =>
  row.acts.map((act) => ({ kind: row.kind, state: row.state, act })),
);

describe('acts by state', () => {
  it('names at least one state per resource kind', () => {
    for (const kind of RESOURCE_KINDS) {
      expect(Object.keys(ACT_TABLES[kind]).length).toBeGreaterThan(0);
    }
  });

  it('gives every state at least one act', () => {
    const empty = ROWS.filter((row) => row.acts.length === 0).map(
      (row) => `${row.kind}/${row.state}`,
    );

    expect(empty).toEqual([]);
  });

  it('has at most one primary act per state', () => {
    const offenders = ROWS.filter((row) => primaries(row.acts).length > 1).map(
      (row) => `${row.kind}/${row.state}`,
    );

    expect(offenders).toEqual([]);
  });

  it('never makes a destructive act primary', () => {
    const offenders = ROWS.flatMap((row) =>
      primaries(row.acts)
        .filter((act) => act.destructive === true || DESTRUCTIVE_IDS.has(act.id))
        .map((act) => `${row.kind}/${row.state}: ${act.id}`),
    );

    expect(offenders).toEqual([]);
  });

  it('keeps every destructive act in the overflow', () => {
    const offenders = EVERY_ACT.filter(
      (entry) => entry.act.destructive === true && entry.act.role !== 'overflow',
    ).map((entry) => `${entry.kind}/${entry.state}: ${entry.act.id}`);

    expect(offenders).toEqual([]);
  });

  it('marks the acts the design names as destructive', () => {
    const marked = new Set(EVERY_ACT.filter((entry) => entry.act.destructive).map((e) => e.act.id));

    for (const id of DESTRUCTIVE_IDS) {
      expect(marked.has(id), `${id} must be marked destructive`).toBe(true);
    }
  });

  it('gives every act an id, a label key and a unique place in its state', () => {
    for (const row of ROWS) {
      const ids = row.acts.map((act) => act.id);
      expect(new Set(ids).size, `${row.kind}/${row.state} repeats an id`).toBe(ids.length);
      for (const act of row.acts) {
        expect(act.id).not.toBe('');
        expect(act.labelKey.endsWith('.')).toBe(false);
      }
    }
  });

  it('starts a state with a primary act or none at all, never with an afterthought', () => {
    const offenders = ROWS.filter((row) => {
      const primary = row.acts.findIndex((act) => act.role === 'primary');
      return primary > 0;
    }).map((row) => `${row.kind}/${row.state}`);

    expect(offenders).toEqual([]);
  });
});

describe('actsFor', () => {
  it('derives the state’s acts when the API offers none', () => {
    expect(actsFor('runtime', 'draining')).toEqual(ACT_TABLES.runtime.draining);
  });

  it('takes the offered acts as the available ones, in the table’s order', () => {
    const acts = actsFor('plan', 'running', [
      { action: 'settings', available: true },
      { action: 'pause', available: true },
      { action: 'chat', available: false, reason: 'no configuration of replan qualifies yet' },
    ]);

    expect(acts.map((act) => act.id)).toEqual(['pause', 'chat', 'settings']);
    const chat = acts.find((act) => act.id === 'chat');
    expect(chat?.disabled).toBe(true);
    expect(chat?.disabledReason).toBe('no configuration of replan qualifies yet');
    const settings = acts.find((act) => act.id === 'settings');
    expect(settings?.role).toBe('overflow');
  });

  it('drops an act the table knows that the API did not offer', () => {
    const acts = actsFor('plan', 'running', [{ action: 'pause', available: true }]);

    expect(acts.map((act) => act.id)).toEqual(['pause']);
  });

  it('keeps an offered act the table does not know, in the overflow', () => {
    const acts = actsFor('plan', 'running', [{ action: 'archive', available: true }]);

    expect(acts.map((act) => act.id)).toEqual(['archive']);
    expect(acts[0].role).toBe('overflow');
  });

  it('never makes an unknown offered act the primary act', () => {
    const acts = actsFor('plan', 'paused', [
      { action: 'unknown_first', available: true },
      { action: 'start', available: true },
    ]);

    expect(primaries(acts).map((act) => act.id)).toEqual(['start']);
    expect(acts.find((act) => act.id === 'unknown_first')?.role).toBe('overflow');
  });

  it('never marks an unknown offered act destructive by mistake', () => {
    const acts = actsFor('plan', 'running', [{ action: 'archive', available: true }]);

    const unknown = acts.find((act) => act.id === 'archive');
    expect(unknown?.role).toBe('overflow');
    expect(unknown?.destructive).toBeUndefined();
  });

  it('keeps a destructive offered act in the overflow and out of the primary place', () => {
    const acts = actsFor('plan', 'paused', [
      { action: 'cancel', available: true },
      { action: 'start', available: true },
    ]);

    const cancel = acts.find((act) => act.id === 'cancel');
    expect(cancel?.destructive).toBe(true);
    expect(cancel?.role).toBe('overflow');
    expect(primaries(acts).map((act) => act.id)).toEqual(['start']);
  });

  it('falls back to the table of a state it does not know as an empty list', () => {
    expect(actsFor('project', 'unheard_of')).toEqual([]);
  });

  it('offers no act when the API explicitly offers none', () => {
    expect(actsFor('plan', 'running', [])).toEqual([]);
  });

  it('names the five resource kinds the use cases carry', () => {
    expect(RESOURCE_KINDS).toEqual(['plan', 'epic', 'project', 'device', 'runtime']);
  });
});

describe('tableActIds and handledActIds', () => {
  it('collects unique act ids from state tables and excludes specified ids', () => {
    const planIds = tableActIds(PLAN_ACTS);
    expect(planIds.has('decision')).toBe(true);
    expect(planIds.has('pause')).toBe(true);
    expect(planIds.has('start')).toBe(true);

    const planIdsMinusDecision = tableActIds(PLAN_ACTS, ['decision']);
    expect(planIdsMinusDecision.has('decision')).toBe(false);
    expect(planIdsMinusDecision.size).toBe(planIds.size - 1);

    expect(handledActIds('plan')).toEqual(planIdsMinusDecision);
    expect(handledActIds('epic')).toEqual(tableActIds(EPIC_ACTS, ['decision']));

    const decisionAct: ActSpec = {
      id: 'decision',
      labelKey: 'control.plan.decision',
      advisoryKey: 'control.plan.decision.title',
      role: 'primary',
    };
    EPIC_ACTS.running.push(decisionAct);
    try {
      const epicIdsWithDecision = tableActIds(EPIC_ACTS);
      expect(epicIdsWithDecision.has('decision')).toBe(true);
      const filtered = handledActIds('epic');
      expect(filtered.has('decision')).toBe(false);
      expect(filtered.size).toBe(epicIdsWithDecision.size - 1);
    } finally {
      EPIC_ACTS.running.pop();
    }
  });
});
