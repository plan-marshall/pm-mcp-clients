/**
 * The runtime-wide events that move an intent record (specification, Event Catalogue), and the rules
 * the panels share for saying a record changed.
 *
 * The engine writes a new intent or outline revision as a phase produces it, so a phase change and a
 * queued plan revision are what a panel of those records reloads on. `EventName` names no
 * intent- or outline-specific event: `plan.changed` is not a member of the catalogue, and no event
 * names a revision of either record. An epic's intent is decomposed rather than revised, so it
 * reloads on its own set, and its change is read from the record, which carries no revision.
 *
 * Every name must be a member of `pm-api`'s closed `EventName` catalogue; `intentEvents.test.ts` reads
 * that catalogue from the repository and refuses a name the runtime cannot send.
 */
export const INTENT_PLAN_EVENTS: readonly string[] = ['phase.changed', 'plan.revision_queued'];

/**
 * The runtime-wide events that move an epic's intent record.
 *
 * The epic's intent is written by a decomposition, so the draft's change (`epic.decomposition_changed`)
 * and the register that adds or drops a staged plan (`epic.register_changed`) are what a panel of it
 * reloads on; a plan's own criterion, which the epic record carries, arrives with
 * `plan.revision_queued`. A phase change is not among them: every plan's phase would refetch every
 * mounted epic panel without the record having moved.
 *
 * Every name must be a member of `pm-api`'s closed `EventName` catalogue; `intentEvents.test.ts` reads
 * that catalogue from the repository and refuses a name the runtime cannot send.
 */
export const INTENT_EPIC_EVENTS: readonly string[] = [
  'epic.decomposition_changed',
  'epic.register_changed',
  'plan.revision_queued',
];

/**
 * @param event the event name the stream delivered
 * @returns `true` when a panel of an epic's intent must read its record again
 */
export function refreshesEpicIntentView(event: string): boolean {
  return INTENT_EPIC_EVENTS.includes(event);
}

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

/**
 * @param record a record the runtime serves, or `null`
 * @returns the record as one comparable string, the reader's own member order, or `null`
 */
export function recordFingerprint(record: unknown | null): string | null {
  return record === null ? null : JSON.stringify(record);
}

/**
 * Decides which record a panel announces as changed, for a record that carries no revision.
 *
 * The epic's intent record has no revision, so the change is read from the record itself: a first read
 * announces nothing, a reload that answers the record on screen announces nothing, and only a reload
 * on a runtime event that answers another record is announced, once, until the next event.
 *
 * @param previous the fingerprint of the record on screen, or `null`
 * @param next the fingerprint the read answered, or `null`
 * @param initial whether the read was the panel's first
 * @returns the fingerprint to announce, or `null` when there is nothing to say
 */
export function changedFingerprint(
  previous: string | null,
  next: string | null,
  initial: boolean,
): string | null {
  return !initial && next !== null && previous !== null && next !== previous ? next : null;
}
