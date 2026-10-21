import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { EPIC_VIEW_EVENTS, refreshesEpicView } from './epicEvents';

/**
 * `EventName` is the closed catalogue of every event the runtime streams. The epic view only
 * chooses which of them reload it; it must never name one the catalogue does not hold, or a rename
 * in `pm-api` would silently stop the reload.
 */
const EVENT_NAME_SOURCE = path.resolve(
  __dirname,
  '../../../../pm-api/src/main/java/de/planmarshall/api/event/EventName.java',
);

function catalogueNames(): Set<string> {
  const source = fs.readFileSync(EVENT_NAME_SOURCE, 'utf-8');
  const names = new Set<string>();
  for (const match of source.matchAll(/^\s*[A-Z][A-Z0-9_]*\("([^"]+)"\)/gm)) {
    names.add(match[1]);
  }
  return names;
}

describe('epic view events', () => {
  it('subscribes only to names of the closed EventName catalogue', () => {
    const catalogue = catalogueNames();

    expect(catalogue.size).toBeGreaterThan(20);
    for (const event of EPIC_VIEW_EVENTS) {
      expect(catalogue, `${event} must be a member of EventName`).toContain(event);
    }
  });

  it('refreshes on a catalogued row change and stays quiet otherwise', () => {
    expect(refreshesEpicView('epic.row_changed')).toBe(true);
    expect(refreshesEpicView('epic.auto_changed')).toBe(true);
    expect(refreshesEpicView('hold.cleared')).toBe(true);
    expect(refreshesEpicView('stream.closed')).toBe(false);
    expect(refreshesEpicView('not.a.catalogued.event')).toBe(false);
  });
});
