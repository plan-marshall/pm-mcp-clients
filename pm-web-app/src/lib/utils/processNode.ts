import type { ProcessNode } from '../api/client';
import type { UiIconId } from '../icons/uiIcons';
import { phaseIconId, unitIconId, type WorkflowIconId } from '../icons/workflowIcons';
import { displayState } from '../display';
import { stripControlSequences } from './outputSafety';

/** A catalogue look-up, as the `t` store hands one to a component or a helper. */
export type Translate = (key: string, params?: Record<string, string | number>) => string;

/**
 * The icon of a node's kind.
 *
 * A phase draws its phase icon, a sub-workflow the icon of its unit, and every other kind (a
 * state, a root node such as `sanity-check`) the neutral workflow icon. Every id resolves through
 * the workflow icon registry, so no id is invented here.
 *
 * @param node the process node
 * @returns the workflow icon id of the node's kind
 */
export function getNodeKindIcon(node: ProcessNode): WorkflowIconId {
  const kind = node.kind?.toLowerCase();
  if (kind === 'phase') {
    return phaseIconId(node.node_id);
  }
  if (kind === 'sub_workflow') {
    return unitIconId(node.node_id || node.title);
  }
  return 'workflow.default';
}

/**
 * The name a row shows for a node: its title without a leading kind word.
 *
 * The kind is drawn as an icon beside the name, so "phase execute" reads "execute". The word is
 * stripped whether the title carries the raw kind token or its localized word, in any case, as
 * long as more text follows it; a title that is the kind word alone keeps it, so a name is never
 * empty.
 *
 * @param node the process node
 * @param t the catalogue look-up
 * @returns the control-stripped title without its leading kind word
 */
export function getNodeDisplayName(node: ProcessNode, t: Translate): string {
  const title = stripControlSequences(node.title);
  const kindWords = [node.kind ?? '', displayState('node_kind', node.kind, t)]
    .map((candidate) => candidate.trim().toLowerCase())
    .filter((candidate) => candidate.length > 0);
  for (const kindWord of kindWords) {
    // `sub_workflow`, `Sub workflow` and `sub-workflow` name the same kind: the parts of the word
    // may be joined by a space, an underscore or a hyphen.
    const pattern = kindWord
      .split(/[\s_-]+/)
      .map((part) => part.replace(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`))
      .join(String.raw`[\s_-]+`);
    const match = new RegExp(String.raw`^${pattern}\s+`, 'i').exec(title);
    if (match === null) {
      continue;
    }
    const rest = title.slice(match[0].length);
    if (rest.length > 0) {
      return rest;
    }
  }
  return title;
}

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
