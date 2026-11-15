import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { INTENT_PLAN_EVENTS, refreshesIntentView } from './intentEvents';

/**
 * `EventName` is the closed catalogue of every event the runtime streams. The intent and outline
 * panels only reload on a name the runtime can actually send, so the set is held against the
 * catalogue the same way the epic view's set is.
 */

const EVENT_NAME_SOURCE = path.resolve(
  __dirname,
  '../../../../pm-api/src/main/java/de/planmarshall/api/event/EventName.java',
);

/** @returns the wire names of the closed `EventName` catalogue */
function catalogue(): string[] {
  const source = fs.readFileSync(EVENT_NAME_SOURCE, 'utf-8');
  return Array.from(source.matchAll(/^ {4}[A-Z_]+\((?:"([a-z_.]+)")/gm)).map((match) => match[1]);
}

describe('the intent panels refresh on real events', () => {
  it('subscribes only to names of the closed EventName catalogue', () => {
    const names = catalogue();

    expect(names).toContain('phase.changed');
    for (const event of INTENT_PLAN_EVENTS) {
      expect(names, `${event} must be a member of EventName`).toContain(event);
    }
  });

  it('names no event the catalogue lacks, plan.changed included', () => {
    const names = catalogue();

    expect(names).not.toContain('plan.changed');
    expect(INTENT_PLAN_EVENTS).not.toContain('plan.changed');
  });

  it('refreshes on a phase change and a queued plan revision, and on nothing else', () => {
    expect(refreshesIntentView('phase.changed')).toBe(true);
    expect(refreshesIntentView('plan.revision_queued')).toBe(true);
    expect(refreshesIntentView('hold.opened')).toBe(false);
    expect(refreshesIntentView('tokens.changed')).toBe(false);
  });
});
