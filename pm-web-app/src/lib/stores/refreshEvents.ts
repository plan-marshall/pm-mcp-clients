/**
 * The runtime events that make a client re-read a resource, one set per stream (specification,
 * Event Catalogue). A view's predicate and its test both read the set from here, so a new event name
 * is one edit; every name must be a member of `pm-api`'s closed `EventName` catalogue, which
 * `refreshEvents.test.ts` reads from the repository and enforces.
 */

/**
 * The events of the project stream that change what the explorer and the runtime store show. The
 * closed `EventName` catalogue names one of them; the release and the enrolment are readings of the
 * project resource (`ProjectReleasedEvent`, `ProjectEnrolledEvent`), not events the runtime streams,
 * so no name is listed for them here.
 */
export const PROJECT_REFRESH_EVENTS: readonly string[] = ['project.changed'];

/**
 * The events of the entry stream that change what the entry resource holds: its sources, their
 * progress and checks, and its state; `reset` asks for a fresh read. The chat's turns and proposals
 * are the chat's own read, and a streamed piece of a turn changes nothing the entry holds.
 */
export const ENTRY_REFRESH_EVENTS: readonly string[] = [
  'reset',
  'source.added',
  'source.removed',
  'source.flagged',
  'progress.changed',
  'check.result',
  'entry.moved',
  'state.entered',
];

/**
 * The runtime-wide events that change a plan's process: a new activity, a phase change, a task that
 * went off track, and work that ended.
 */
export const PLAN_PROCESS_REFRESH_EVENTS: readonly string[] = [
  'activity.changed',
  'phase.changed',
  'task.off_track',
  'project.work_ended',
];

/**
 * @param event the event name the stream delivered
 * @returns `true` when the project views must re-read what they hold
 */
export function refreshesProjectView(event: string): boolean {
  return PROJECT_REFRESH_EVENTS.includes(event);
}

/**
 * @param event the event name the entry's stream delivered
 * @returns `true` when the entry view must re-read the entry
 */
export function refreshesEntry(event: string): boolean {
  return ENTRY_REFRESH_EVENTS.includes(event);
}

/**
 * @param event the event name the runtime stream delivered
 * @returns `true` when the plan view must re-read the plan's process
 */
export function refreshesPlanProcess(event: string): boolean {
  return PLAN_PROCESS_REFRESH_EVENTS.includes(event);
}
