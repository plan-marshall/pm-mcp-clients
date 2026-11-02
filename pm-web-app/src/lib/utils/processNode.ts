import type { ProcessNode } from '../api/client';
import type { UiIconId } from '../icons/uiIcons';

/** A catalogue look-up, as the `t` store hands one to a component or a helper. */
export type Translate = (key: string, params?: Record<string, string | number>) => string;

/**
 * The icon of a process state.
 *
 * The tree and the node narrative read the same mapping, so a state never shows one icon in
 * the tree and another in the narrative.
 *
 * @param state the process state, from the runtime's closed list
 * @returns the icon name of that state
 */
export function getNodeStateIcon(state: string): UiIconId {
  switch (state?.toLowerCase()) {
    case 'done':
      return 'state.check';
    case 'running':
      return 'act.start';
    case 'waiting':
    case 'waiting_for_you':
    case 'waiting_for_operator':
      return 'state.alert';
    case 'failed':
      return 'state.error';
    default:
      return 'state.dot';
  }
}

/**
 * The words of a process state, in the operator's locale.
 *
 * A node the plan ran more than once names the count and, while it runs, the run it is in. The
 * tree and the node narrative both call this function.
 *
 * The state is the runtime's `NodeState` (`waiting_for_operator` is the value "waiting for you"
 * reads as). A value no word names reads as the catalogue's unknown word, never as the raw wire
 * value: a raw value is a transport value and has no place on screen.
 *
 * @param node the process node
 * @param t the catalogue look-up
 * @returns the localized state text
 */
export function getNodeStateText(node: ProcessNode, t: Translate): string {
  const state = node.state?.toLowerCase();
  if (state === 'done' && node.run_count > 1) {
    return t('plan.node.state.done_times', { count: node.run_count });
  }
  if (state === 'running' && node.current_run > 1) {
    return t('plan.node.state.running_run', { run: node.current_run });
  }
  if (state === 'waiting' || state === 'waiting_for_you' || state === 'waiting_for_operator') {
    return t('plan.node.state.waiting');
  }
  if (state === 'done') return t('plan.node.state.done');
  if (state === 'running') return t('plan.node.state.running');
  if (state === 'failed') return t('plan.node.state.failed');
  if (state === 'planned') return t('plan.node.state.planned');
  if (state === 'not_taken') return t('plan.node.state.not_taken');
  if (state === 'not_in_plan') return t('plan.node.state.not_in_plan');
  return t('state.unknown');
}
