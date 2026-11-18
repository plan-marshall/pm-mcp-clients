import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import {
  INTENT_EPIC_EVENTS,
  INTENT_PLAN_EVENTS,
  changedFingerprint,
  changedRevision,
  recordFingerprint,
  refreshesEpicIntentView,
  refreshesIntentView,
} from './intentEvents';

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
  return Array.from(source.matchAll(/^\s*[A-Z][A-Z0-9_]*\("([^"]+)"[,)]/gm)).map(
    (match) => match[1],
  );
}

describe('the intent panels refresh on real events', () => {
  it('subscribes only to names of the closed EventName catalogue', () => {
    const names = catalogue();

    expect(names.length).toBeGreaterThan(20);
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

describe('the epic intent panel refreshes on real events', () => {
  it('subscribes only to names of the closed EventName catalogue', () => {
    const names = catalogue();

    for (const event of INTENT_EPIC_EVENTS) {
      expect(names, `${event} must be a member of EventName`).toContain(event);
    }
  });

  it('refreshes on the decomposition and the register of the epic and on a plan revision', () => {
    expect(refreshesEpicIntentView('epic.decomposition_changed')).toBe(true);
    expect(refreshesEpicIntentView('epic.register_changed')).toBe(true);
    expect(refreshesEpicIntentView('plan.revision_queued')).toBe(true);
  });

  it('does not reload every mounted panel for a phase change', () => {
    expect(refreshesEpicIntentView('phase.changed')).toBe(false);
    expect(refreshesEpicIntentView('tokens.changed')).toBe(false);
    expect(INTENT_EPIC_EVENTS).not.toContain('phase.changed');
  });
});

describe('the change notice a panel says once', () => {
  it('says nothing on a first read, and nothing when the revision did not move', () => {
    expect(changedRevision(null, { revision: 3 }, true)).toBeNull();
    expect(changedRevision({ revision: 3 }, { revision: 3 }, false)).toBeNull();
    expect(changedRevision({ revision: 3 }, null, false)).toBeNull();
  });

  it('announces a new revision that arrived on an event', () => {
    expect(changedRevision({ revision: 3 }, { revision: 4 }, false)).toBe(4);
  });
});

describe('the change notice of a record without a revision', () => {
  const first = { epic_id: 'relay-hardening', destination: 'a' };
  const second = { epic_id: 'relay-hardening', destination: 'b' };

  it('fingerprints the record itself, and nothing for an absent one', () => {
    expect(recordFingerprint(first)).toBe(JSON.stringify(first));
    expect(recordFingerprint(null)).toBeNull();
    expect(recordFingerprint(first)).not.toBe(recordFingerprint(second));
  });

  it('says nothing on a first read, and nothing for an unchanged record', () => {
    const fingerprint = recordFingerprint(first);

    expect(changedFingerprint(null, fingerprint, true)).toBeNull();
    expect(changedFingerprint(fingerprint, fingerprint, false)).toBeNull();
    expect(changedFingerprint(fingerprint, null, false)).toBeNull();
  });

  it('announces the record that arrived on an event', () => {
    const next = recordFingerprint(second);

    expect(changedFingerprint(recordFingerprint(first), next, false)).toBe(next);
  });
});
