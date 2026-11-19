import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import {
  PROJECT_REFRESH_EVENTS,
  ENTRY_REFRESH_EVENTS,
  PLAN_PROCESS_REFRESH_EVENTS,
  refreshesProjectView,
  refreshesEntry,
  refreshesPlanProcess,
} from './refreshEvents';

/**
 * `EventName` is the closed catalogue of every event the runtime streams. The refresh sets only
 * choose which of them make a client re-read a resource; a name the catalogue does not hold is a
 * stale member, and a rename in `pm-api` would silently stop the refresh.
 */
const EVENT_NAME_SOURCE = path.resolve(
  __dirname,
  '../../../../pm-api/src/main/java/de/planmarshall/api/event/EventName.java',
);

/** @returns the wire names of the closed `EventName` catalogue */
function catalogueNames(): Set<string> {
  const source = fs.readFileSync(EVENT_NAME_SOURCE, 'utf-8');
  const names = new Set<string>();
  // A member is `NAME("wire", STREAM, ...)`: the wire name comes first, its streams follow.
  for (const match of source.matchAll(/^\s*[A-Z][A-Z0-9_]*\("([^"]+)"[,)]/gm)) {
    names.add(match[1]);
  }
  return names;
}

const SETS: ReadonlyArray<readonly [string, readonly string[]]> = [
  ['project', PROJECT_REFRESH_EVENTS],
  ['entry', ENTRY_REFRESH_EVENTS],
  ['plan process', PLAN_PROCESS_REFRESH_EVENTS],
];

describe('the refresh events', () => {
  it('names only members of the closed EventName catalogue', () => {
    const catalogue = catalogueNames();

    expect(catalogue.size).toBeGreaterThan(20);
    for (const [stream, events] of SETS) {
      expect(events.length, `${stream} has no event`).toBeGreaterThan(0);
      for (const event of events) {
        expect(catalogue, `the ${stream} set names ${event}, which EventName does not hold`).toContain(
          event,
        );
      }
    }
  });

  it('keeps one set per stream, and a name is in one set only', () => {
    const seen = new Map<string, string>();

    for (const [stream, events] of SETS) {
      for (const event of events) {
        expect(seen.get(event), `${event} is in ${seen.get(event)} and in ${stream}`).toBeUndefined();
        seen.set(event, stream);
      }
    }
  });

  it('answers each predicate from its own set only', () => {
    for (const event of PROJECT_REFRESH_EVENTS) {
      expect(refreshesProjectView(event)).toBe(true);
      expect(refreshesEntry(event)).toBe(false);
      expect(refreshesPlanProcess(event)).toBe(false);
    }
    for (const event of ENTRY_REFRESH_EVENTS) {
      expect(refreshesEntry(event)).toBe(true);
      expect(refreshesProjectView(event)).toBe(false);
    }
    for (const event of PLAN_PROCESS_REFRESH_EVENTS) {
      expect(refreshesPlanProcess(event)).toBe(true);
      expect(refreshesProjectView(event)).toBe(false);
    }
    expect(refreshesProjectView('stream.closed')).toBe(false);
    expect(refreshesEntry('not.a.catalogued.event')).toBe(false);
    expect(refreshesPlanProcess('chat.turn.delta')).toBe(false);
  });

  it('refreshes the project explorer on the one project event the catalogue holds', () => {
    expect(PROJECT_REFRESH_EVENTS).toEqual(['project.changed']);
  });

  it('refreshes the entry on every event that changes what it holds, and never on a chat turn', () => {
    expect(ENTRY_REFRESH_EVENTS).toEqual([
      'reset',
      'source.added',
      'source.removed',
      'source.flagged',
      'progress.changed',
      'check.result',
      'entry.moved',
      'state.entered',
    ]);
    expect(refreshesEntry('chat.turn.started')).toBe(false);
    expect(refreshesEntry('chat.turn.completed')).toBe(false);
    expect(refreshesEntry('chat.proposal')).toBe(false);
  });

  it('refreshes the plan process on the four events it always did', () => {
    expect(PLAN_PROCESS_REFRESH_EVENTS).toEqual([
      'activity.changed',
      'phase.changed',
      'task.off_track',
      'project.work_ended',
    ]);
  });
});
