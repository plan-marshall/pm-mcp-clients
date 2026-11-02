/**
 * The acts a state offers (UX-P-04, UC-P-02).
 *
 * A view never keeps its own list of buttons: it asks this module for the acts of the resource it
 * shows and hands them to `ActionBar`. The API's offered acts win where it sends them — the table
 * then supplies the order and the role, which is what keeps the design's one-primary-act rule true
 * even when the runtime lists the acts in another order.
 *
 * The rules the tables keep, and the test proves for every state:
 *
 * * a state has at most one primary act, and it is the first act;
 * * a destructive act is never primary and never one of the outlined secondary acts — it lives in
 *   the overflow, behind its confirmation;
 * * every act names its catalogue label key, and an advisory key ends in `.title`.
 */

/** The role an act plays in the bar. */
export type ActRole = 'primary' | 'secondary' | 'overflow';

/** One act of a state, as the table and the bar carry it. */
export interface ActSpec {
  /** The act's id, reported back when a person chooses it. */
  id: string;
  /** The catalogue key of the visible label. */
  labelKey: string;
  /** The catalogue key of the hover advisory; it ends in `.title`. */
  advisoryKey?: string;
  /** Where the act sits in the bar. */
  role: ActRole;
  /** Whether the act cannot be taken back; a destructive act lives in the overflow. */
  destructive?: boolean;
  /** Whether the API marked the act unavailable, and why. */
  disabled?: boolean;
  disabledReason?: string;
}

/** One act as the API offers it for a resource. */
export interface OfferedAction {
  action: string;
  available: boolean;
  reason?: string | null;
}

/** The resource kinds the use cases carry, each with its own table. */
export type ResourceKind = 'plan' | 'epic' | 'project' | 'device' | 'runtime';

/** The resource kinds, in the order the design names them. */
export const RESOURCE_KINDS: ResourceKind[] = ['plan', 'epic', 'project', 'device', 'runtime'];

const OVERFLOW = { role: 'overflow' } as const;
const SECONDARY = { role: 'secondary' } as const;
const PRIMARY = { role: 'primary' } as const;

/** A destructive act, which always lives in the overflow. */
const DESTRUCTIVE = { role: 'overflow', destructive: true } as const;

/**
 * A plan's acts by state (UX-P-04).
 *
 * `waiting_for_you` carries the decision as its primary act; the plan page opens it (PLAN-55). The
 * panel keeps the acts it can run itself, so a primary act another view owns is offered there.
 */
export const PLAN_ACTS: Record<string, ActSpec[]> = {
  running: [
    { id: 'pause', labelKey: 'control.plan.pause', advisoryKey: 'control.plan.pause.title', ...SECONDARY },
    { id: 'chat', labelKey: 'control.plan.chat', advisoryKey: 'control.plan.chat.title', ...SECONDARY },
    { id: 'settings', labelKey: 'control.plan.settings', advisoryKey: 'control.plan.settings.title', ...OVERFLOW },
    { id: 'cancel', labelKey: 'control.plan.cancel', advisoryKey: 'control.plan.cancel.title', ...DESTRUCTIVE },
  ],
  pausing: [
    { id: 'start', labelKey: 'control.plan.start', advisoryKey: 'control.plan.start.title', ...SECONDARY },
    { id: 'chat', labelKey: 'control.plan.chat', advisoryKey: 'control.plan.chat.title', ...SECONDARY },
    { id: 'settings', labelKey: 'control.plan.settings', advisoryKey: 'control.plan.settings.title', ...OVERFLOW },
    { id: 'cancel', labelKey: 'control.plan.cancel', advisoryKey: 'control.plan.cancel.title', ...DESTRUCTIVE },
  ],
  paused: [
    { id: 'start', labelKey: 'control.plan.start', advisoryKey: 'control.plan.start.title', ...PRIMARY },
    { id: 'chat', labelKey: 'control.plan.chat', advisoryKey: 'control.plan.chat.title', ...SECONDARY },
    { id: 'settings', labelKey: 'control.plan.settings', advisoryKey: 'control.plan.settings.title', ...SECONDARY },
    { id: 'abandon', labelKey: 'control.plan.abandon', advisoryKey: 'control.plan.abandon.title', ...DESTRUCTIVE },
    { id: 'cancel', labelKey: 'control.plan.cancel', advisoryKey: 'control.plan.cancel.title', ...DESTRUCTIVE },
  ],
  waiting_for_you: [
    { id: 'decision', labelKey: 'control.plan.decision', advisoryKey: 'control.plan.decision.title', ...PRIMARY },
    { id: 'chat', labelKey: 'control.plan.chat', advisoryKey: 'control.plan.chat.title', ...SECONDARY },
    { id: 'pause', labelKey: 'control.plan.pause', advisoryKey: 'control.plan.pause.title', ...SECONDARY },
    { id: 'settings', labelKey: 'control.plan.settings', advisoryKey: 'control.plan.settings.title', ...OVERFLOW },
    { id: 'cancel', labelKey: 'control.plan.cancel', advisoryKey: 'control.plan.cancel.title', ...DESTRUCTIVE },
  ],
  held: [
    { id: 'resolve_hold', labelKey: 'control.plan.resolve_hold', advisoryKey: 'control.plan.resolve_hold.title', ...PRIMARY },
    { id: 'chat', labelKey: 'control.plan.chat', advisoryKey: 'control.plan.chat.title', ...SECONDARY },
    { id: 'settings', labelKey: 'control.plan.settings', advisoryKey: 'control.plan.settings.title', ...OVERFLOW },
    { id: 'cancel', labelKey: 'control.plan.cancel', advisoryKey: 'control.plan.cancel.title', ...DESTRUCTIVE },
  ],
  staged: [
    { id: 'start', labelKey: 'control.plan.start', advisoryKey: 'control.plan.start.title', ...PRIMARY },
    { id: 'settings', labelKey: 'control.plan.settings', advisoryKey: 'control.plan.settings.title', ...OVERFLOW },
    { id: 'cancel', labelKey: 'control.plan.cancel', advisoryKey: 'control.plan.cancel.title', ...DESTRUCTIVE },
  ],
  ended: [
    { id: 'report', labelKey: 'control.plan.report', advisoryKey: 'control.plan.report.title', ...PRIMARY },
    { id: 'chat', labelKey: 'control.plan.chat', advisoryKey: 'control.plan.chat_ended.title', ...SECONDARY },
    { id: 'follow_up', labelKey: 'control.plan.follow_up', advisoryKey: 'control.plan.follow_up.title', ...OVERFLOW },
  ],
};

/** An epic's acts by state (UC-P-02, UX-P-04). */
export const EPIC_ACTS: Record<string, ActSpec[]> = {
  running: [
    { id: 'next', labelKey: 'control.epic.next', advisoryKey: 'control.epic.next.title', ...PRIMARY },
    { id: 'findings', labelKey: 'control.epic.findings_review', advisoryKey: 'control.epic.findings_review.title', ...SECONDARY },
    { id: 'chat', labelKey: 'control.epic.chat', advisoryKey: 'control.epic.chat.title', ...SECONDARY },
    { id: 'auto', labelKey: 'control.epic.auto', advisoryKey: 'control.epic.auto.title', ...OVERFLOW },
    { id: 'common_files', labelKey: 'control.epic.overlap', advisoryKey: 'control.epic.overlap.title', ...OVERFLOW },
    { id: 'decompose', labelKey: 'control.epic.decompose_again', advisoryKey: 'control.epic.decompose_again.title', ...OVERFLOW },
    { id: 'watches', labelKey: 'control.epic.watches_review', advisoryKey: 'control.epic.watches_review.title', ...OVERFLOW },
    { id: 'archive', labelKey: 'control.epic.archive', advisoryKey: 'control.epic.archive.title', ...OVERFLOW },
    { id: 'cancel', labelKey: 'control.epic.cancel', advisoryKey: 'control.epic.cancel.title', ...DESTRUCTIVE },
  ],
  waiting_for_you: [
    { id: 'next', labelKey: 'control.epic.next', advisoryKey: 'control.epic.next.title', ...PRIMARY },
    { id: 'findings', labelKey: 'control.epic.findings_review', advisoryKey: 'control.epic.findings_review.title', ...SECONDARY },
    { id: 'chat', labelKey: 'control.epic.chat', advisoryKey: 'control.epic.chat.title', ...SECONDARY },
    { id: 'auto', labelKey: 'control.epic.auto', advisoryKey: 'control.epic.auto.title', ...OVERFLOW },
    { id: 'common_files', labelKey: 'control.epic.overlap', advisoryKey: 'control.epic.overlap.title', ...OVERFLOW },
    { id: 'decompose', labelKey: 'control.epic.decompose_again', advisoryKey: 'control.epic.decompose_again.title', ...OVERFLOW },
    { id: 'watches', labelKey: 'control.epic.watches_review', advisoryKey: 'control.epic.watches_review.title', ...OVERFLOW },
    { id: 'archive', labelKey: 'control.epic.archive', advisoryKey: 'control.epic.archive.title', ...OVERFLOW },
    { id: 'cancel', labelKey: 'control.epic.cancel', advisoryKey: 'control.epic.cancel.title', ...DESTRUCTIVE },
  ],
  held: [
    { id: 'findings', labelKey: 'control.epic.findings_review', advisoryKey: 'control.epic.findings_review.title', ...SECONDARY },
    { id: 'chat', labelKey: 'control.epic.chat', advisoryKey: 'control.epic.chat.title', ...SECONDARY },
    { id: 'auto', labelKey: 'control.epic.auto', advisoryKey: 'control.epic.auto.title', ...OVERFLOW },
    { id: 'cancel', labelKey: 'control.epic.cancel', advisoryKey: 'control.epic.cancel.title', ...DESTRUCTIVE },
  ],
  landing: [
    { id: 'chat', labelKey: 'control.epic.chat', advisoryKey: 'control.epic.chat.title', ...SECONDARY },
    { id: 'archive', labelKey: 'control.epic.archive', advisoryKey: 'control.epic.archive.title', ...OVERFLOW },
    { id: 'cancel', labelKey: 'control.epic.cancel', advisoryKey: 'control.epic.cancel.title', ...DESTRUCTIVE },
  ],
  ended: [
    { id: 'chat', labelKey: 'control.epic.chat', advisoryKey: 'control.epic.chat.title', ...SECONDARY },
    { id: 'archive', labelKey: 'control.epic.archive', advisoryKey: 'control.epic.archive.title', ...OVERFLOW },
  ],
};

/** A project's acts by state (UX-P-04; the grid that draws them is PLAN-59). */
export const PROJECT_ACTS: Record<string, ActSpec[]> = {
  ready: [
    { id: 'open_running_work', labelKey: 'project.act.open_running_work', advisoryKey: 'project.act.open_running_work.title', ...PRIMARY },
    { id: 'start_plan', labelKey: 'project.act.start_plan', advisoryKey: 'project.act.start_plan.title', ...SECONDARY },
    { id: 'start_epic', labelKey: 'project.act.start_epic', advisoryKey: 'project.act.start_epic.title', ...SECONDARY },
    { id: 'setup', labelKey: 'project.act.setup', advisoryKey: 'project.act.setup.title', ...OVERFLOW },
    { id: 'release', labelKey: 'project.act.release', advisoryKey: 'project.act.release.title', ...DESTRUCTIVE },
    { id: 'unenrol', labelKey: 'project.act.unenrol', advisoryKey: 'project.act.unenrol.title', ...DESTRUCTIVE },
  ],
};

/** A paired device's acts (UC-ENT-2, UX-P-04): rename beside, revoke behind the overflow. */
export const DEVICE_ACTS: Record<string, ActSpec[]> = {
  this_device: [
    { id: 'rename', labelKey: 'devices.act.rename', advisoryKey: 'devices.act.rename.title', ...SECONDARY },
    { id: 'revoke', labelKey: 'devices.act.revoke', advisoryKey: 'devices.act.revoke.title', ...DESTRUCTIVE },
    { id: 'revoke_others', labelKey: 'devices.act.revoke_others', advisoryKey: 'devices.act.revoke_others.title', ...DESTRUCTIVE },
  ],
  other_device: [
    { id: 'rename', labelKey: 'devices.act.rename', advisoryKey: 'devices.act.rename.title', ...SECONDARY },
    { id: 'revoke', labelKey: 'devices.act.revoke', advisoryKey: 'devices.act.revoke.title', ...DESTRUCTIVE },
  ],
};

/** The runtime's acts by lifecycle state (UC-P-02). */
export const RUNTIME_ACTS: Record<string, ActSpec[]> = {
  running: [
    { id: 'check', labelKey: 'system.runtime.check_now', advisoryKey: 'system.runtime.check_now.title', ...PRIMARY },
    { id: 'stop', labelKey: 'system.action.stop', advisoryKey: 'system.action.stop.title', ...SECONDARY },
    { id: 'restart', labelKey: 'system.action.restart', advisoryKey: 'system.action.restart.title', ...SECONDARY },
  ],
  update_available: [
    { id: 'upgrade', labelKey: 'system.action.upgrade', advisoryKey: 'system.action.upgrade.title', ...PRIMARY },
    { id: 'stop', labelKey: 'system.action.stop', advisoryKey: 'system.action.stop.title', ...SECONDARY },
    { id: 'restart', labelKey: 'system.action.restart', advisoryKey: 'system.action.restart.title', ...SECONDARY },
    { id: 'check', labelKey: 'system.runtime.check_now', advisoryKey: 'system.runtime.check_now.title', ...OVERFLOW },
  ],
  draining: [
    { id: 'withdraw_stop', labelKey: 'system.stopping.withdraw', advisoryKey: 'system.stopping.withdraw.title', ...PRIMARY },
    { id: 'stop_now', labelKey: 'system.stopping.stop_now', advisoryKey: 'system.stopping.stop_now.title', ...SECONDARY },
  ],
  upgrading: [
    { id: 'withdraw_upgrade', labelKey: 'system.upgrade.withdraw', advisoryKey: 'system.upgrade.withdraw.title', ...PRIMARY },
    { id: 'stop_now', labelKey: 'system.stopping.stop_now', advisoryKey: 'system.stopping.stop_now.title', ...SECONDARY },
  ],
  stopped: [
    { id: 'start', labelKey: 'system.action.start', advisoryKey: 'system.action.start.title', ...PRIMARY },
  ],
};

/** Every table, by resource kind. */
export const ACT_TABLES: Record<ResourceKind, Record<string, ActSpec[]>> = {
  plan: PLAN_ACTS,
  epic: EPIC_ACTS,
  project: PROJECT_ACTS,
  device: DEVICE_ACTS,
  runtime: RUNTIME_ACTS,
};

/**
 * @param kind the resource kind
 * @param action an act id the table does not know
 * @returns a label key following the catalogue's `control.{kind}.{action}` convention
 */
function fallbackLabelKey(kind: ResourceKind, action: string): string {
  return `control.${kind}.${action}`;
}

/**
 * @param spec the table's entry
 * @param offered the API's entry for the same act
 * @returns the spec with the API's availability applied
 */
function withAvailability(spec: ActSpec, offered: OfferedAction | undefined): ActSpec {
  if (!offered) {
    return spec;
  }
  if (offered.available) {
    return { ...spec, disabled: false, disabledReason: undefined };
  }
  return {
    ...spec,
    disabled: true,
    disabledReason: offered.reason ?? undefined,
  };
}

/**
 * @param kind the resource kind
 * @param action an act the API offers that no table names
 * @returns the act, in the overflow, so it can never become the state's primary act
 */
function unknownAct(kind: ResourceKind, action: string): ActSpec {
  return { id: action, labelKey: fallbackLabelKey(kind, action), ...OVERFLOW };
}

/**
 * The acts a state offers.
 *
 * Without offered acts the table is the answer. With them, the API decides which acts exist and
 * whether they can run; the table decides their order and their role. An act the table does not
 * know is appended to the overflow, never promoted to the primary act.
 *
 * @param kind the resource kind
 * @param state the resource's state, as the runtime or the view names it
 * @param offered the acts the API offered, when it sends them
 * @returns the state's acts, most important first
 */
export function actsFor(
  kind: ResourceKind,
  state: string,
  offered?: OfferedAction[] | null,
): ActSpec[] {
  const table = ACT_TABLES[kind][state] ?? [];
  if (offered === null || offered === undefined) {
    return table;
  }
  const byAction = new Map(offered.map((entry) => [entry.action, entry]));
  const known = table
    .filter((spec) => byAction.has(spec.id))
    .map((spec) => withAvailability(spec, byAction.get(spec.id)));
  const tableIds = new Set(table.map((spec) => spec.id));
  const extra = offered
    .filter((entry) => !tableIds.has(entry.action))
    .map((entry) =>
      withAvailability(unknownAct(kind, entry.action), entry),
    );
  return [...known, ...extra];
}
