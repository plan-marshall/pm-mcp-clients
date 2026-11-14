/**
 * The runtime-wide events that move a plan's intent or outline record (specification, Event
 * Catalogue).
 *
 * The engine writes a new intent or outline revision as a phase produces it, so a phase change and a
 * queued plan revision are what a panel of these records reloads on. `EventName` names no
 * intent- or outline-specific event: `plan.changed` is not a member of the catalogue, and no event
 * names a revision of either record.
 *
 * Every name must be a member of `pm-api`'s closed `EventName` catalogue; the unit test reads that
 * catalogue from the repository and refuses a name the runtime cannot send.
 */
export const INTENT_PLAN_EVENTS: readonly string[] = ['phase.changed', 'plan.revision_queued'];

/**
 * @param event the event name the stream delivered
 * @returns `true` when a panel of the plan's intent or outline must read its record again
 */
export function refreshesIntentView(event: string): boolean {
  return INTENT_PLAN_EVENTS.includes(event);
}
