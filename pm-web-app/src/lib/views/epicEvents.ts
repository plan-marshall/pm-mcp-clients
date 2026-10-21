/**
 * The runtime-wide events that move something the epic view shows (specification, Event Catalogue).
 *
 * The view reloads the epic resource on each of them: a row's status or wait (`epic.row_changed`),
 * a scope started or ended, a phase or activity change, what needs the operator, the automatic
 * mode, a register entry, a decomposition draft, the tokens, or a hold.
 *
 * Every name must be a member of `pm-api`'s closed `EventName` catalogue; the unit test reads that
 * catalogue from the repository and refuses a name the runtime cannot send.
 */
export const EPIC_VIEW_EVENTS: readonly string[] = [
  'project.work_started',
  'project.work_ended',
  'project.changed',
  'scope.started',
  'scope.ended',
  'phase.changed',
  'activity.changed',
  'needs_operator.changed',
  'epic.row_changed',
  'epic.auto_changed',
  'epic.register_changed',
  'epic.decomposition_changed',
  'tokens.changed',
  'hold.opened',
  'hold.changed',
  'hold.cleared',
];

/**
 * @param event the event name the stream delivered
 * @returns `true` when the epic view must reload for it
 */
export function refreshesEpicView(event: string): boolean {
  return EPIC_VIEW_EVENTS.includes(event);
}
