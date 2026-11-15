/**
 * The runtime-wide events that move a plan's intent or outline record (specification, Event
 * Catalogue), and the one rule both panels share for saying a record changed.
 *
 * The engine writes a new intent or outline revision as a phase produces it, so a phase change and a
 * queued plan revision are what a panel of these records reloads on. `EventName` names no
 * intent- or outline-specific event: `plan.changed` is not a member of the catalogue, and no event
 * names a revision of either record.
 *
 * Every name must be a member of `pm-api`'s closed `EventName` catalogue; `intentEvents.test.ts` reads
 * that catalogue from the repository and refuses a name the runtime cannot send.
 */
export const INTENT_PLAN_EVENTS: readonly string[] = ['phase.changed', 'plan.revision_queued'];

/**
 * @param event the event name the stream delivered
 * @returns `true` when a panel of the plan's intent or outline must read its record again
 */
export function refreshesIntentView(event: string): boolean {
  return INTENT_PLAN_EVENTS.includes(event);
}

/** A record that carries the revision a panel compares. */
export interface RevisionedRecord {
  revision: number;
}

/**
 * Decides which revision a panel announces as changed.
 *
 * A first read announces nothing, and so does a reload that answers the same revision; only a reload
 * on a runtime event that answers a new revision is announced, once.
 *
 * @param previous the record on screen before the read, or `null`
 * @param next the record the read answered, or `null`
 * @param initial whether the read was the panel's first
 * @returns the revision to announce, or `null` when there is nothing to say
 */
export function changedRevision<T extends RevisionedRecord>(
  previous: T | null,
  next: T | null,
  initial: boolean,
): number | null {
  return !initial && next !== null && previous !== null && next.revision !== previous.revision
    ? next.revision
    : null;
}
