<script lang="ts">
  import { currentLocale, t } from '../i18n';
  import type { LeafTask, ProcessNode, ProcessTree } from '../api/client';
  import { formatDurationSeconds, formatTokens } from '../utils/tokenFormat';
  import { stripControlSequences } from '../utils/outputSafety';
  import { getNodeStateIcon, getNodeStateText } from '../utils/processNode';
  import { displayState } from '../display';
  import Icon from './Icon.svelte';

  interface Props {
    node: ProcessNode;
    plan: ProcessTree;
    onSelectTask: (taskId: string) => void;
    onSelectNode: (nodeId: string) => void;
  }

  let { node, plan, onSelectTask, onSelectNode }: Props = $props();

  /**
   * Every task the node already carries, including the tasks of its sub-nodes.
   *
   * A phase whose work sits in a sub-workflow still narrates that work; the tree keeps the
   * level-by-level structure, and the narrative says what ran inside the selected part. A part
   * the process omitted from the tree is skipped, like the tree and the selection helpers do.
   *
   * @param current the node whose tasks are collected
   * @returns the node's own tasks, then its visible sub-nodes' tasks in tree order
   */
  function collectLeafTasks(current: ProcessNode): LeafTask[] {
    const tasks = [...(current.leaf_tasks ?? [])];
    for (const subNode of current.sub_nodes ?? []) {
      if (subNode.state === 'not_in_plan') continue;
      tasks.push(...collectLeafTasks(subNode));
    }
    return tasks;
  }

  let childTasks = $derived(collectLeafTasks(node));
  let subNodes = $derived((node.sub_nodes ?? []).filter((child) => child.state !== 'not_in_plan'));
</script>

<div class="node-narrative" data-testid="node-narrative" data-plan-id={plan.plan_id}>
  <header class="node-narrative__head">
    <p class="node-narrative__eyebrow text-muted">{$t('plan.node.narrative')}</p>
    <div class="node-narrative__title-line">
      <h3 class="node-narrative__title font-semibold" data-testid="node-narrative-title">
        {stripControlSequences(node.title)}
      </h3>
      <span class="node-narrative__kind">{displayState('node_kind', node.kind, $t)}</span>
      <span class="node-narrative__state" data-testid="node-narrative-state">
        {getNodeStateText(node, $t)}
      </span>
      {#if node.run_count > 0}
        <span class="node-narrative__runs" data-testid="node-narrative-runs">
          {$t('plan.node.run', { count: node.run_count })}
        </span>
      {/if}
      {#if node.duration_seconds > 0}
        <span class="node-narrative__duration mono" data-testid="node-narrative-duration">
          {formatDurationSeconds(node.duration_seconds)}
        </span>
      {/if}
    </div>

    {#if node.waits_for}
      <p class="node-narrative__detail" data-testid="node-narrative-waits">
        <span class="node-narrative__label">{$t('plan.node.waits_for')}</span>
        <span>{stripControlSequences(node.waits_for)}</span>
      </p>
    {/if}
    {#if node.prune_reason}
      <p class="node-narrative__detail" data-testid="node-narrative-prune">
        <span class="node-narrative__label">{$t('plan.node.prune_reason')}</span>
        <span>{stripControlSequences(node.prune_reason)}</span>
      </p>
    {/if}
  </header>

  {#if childTasks.length === 0}
    <div class="node-narrative__empty text-muted" data-testid="node-no-activity">
      <p>{$t('plan.node.no_activity')}</p>
    </div>
  {:else}
    <section class="node-narrative__section" data-testid="node-child-tasks">
      <h4 class="node-narrative__section-title">{$t('plan.node.child_tasks')}</h4>
      <div class="node-narrative__tasks">
        {#each childTasks as task (task.task_id)}
          <button
            type="button"
            class="child-task"
            onclick={() => onSelectTask(task.task_id)}
            data-testid="node-child-task-{task.task_id}"
          >
            <span class="child-task__glyph" aria-hidden="true"
              ><Icon name={getNodeStateIcon(task.status)} size={16} /></span
            >
            <span class="child-task__id mono font-semibold"
              >{stripControlSequences(task.task_id)}</span
            >
            <span class="child-task__role">{stripControlSequences(task.role)}</span>
            <span class="child-task__model">
              {stripControlSequences(task.harness)}
              {stripControlSequences(task.model)}
            </span>
            <span class="child-task__attempt">
              {$t('plan.node.attempt', { attempt: task.attempt })}
            </span>
            <span class="child-task__status badge badge--state badge--state-{task.status}"
              >{task.status}</span
            >
            {#if task.duration_seconds > 0}
              <span class="child-task__duration mono">
                {formatDurationSeconds(task.duration_seconds)}
              </span>
            {/if}
            {#if task.tokens_so_far > 0}
              <span class="child-task__tokens mono" title={$t('plan.view.tokens')}>
                {formatTokens(task.tokens_so_far, false, $currentLocale)}
              </span>
            {/if}
            <span
              class="child-task__activity"
              data-testid="node-child-task-activity-{task.task_id}"
            >
              {task.activity_link ? $t('plan.node.has_activity') : $t('plan.node.no_activity_link')}
            </span>
          </button>
        {/each}
      </div>
    </section>
  {/if}

  {#if subNodes.length > 0}
    <section class="node-narrative__section" data-testid="node-sub-nodes">
      <h4 class="node-narrative__section-title">{$t('plan.node.child_nodes')}</h4>
      <ul class="node-narrative__outline">
        {#each subNodes as subNode (subNode.node_id)}
          <li>
            <button
              type="button"
              class="child-node"
              onclick={() => onSelectNode(subNode.node_id)}
              aria-label={`${stripControlSequences(subNode.title)} — ${$t('plan.node.select')}`}
              data-testid="node-child-node-{subNode.node_id}"
            >
              <span class="child-node__glyph" aria-hidden="true"
                ><Icon name={getNodeStateIcon(subNode.state)} size={16} /></span
              >
              <span class="child-node__title">{stripControlSequences(subNode.title)}</span>
              <span class="child-node__kind">{displayState('node_kind', subNode.kind, $t)}</span>
              <span class="child-node__state">{getNodeStateText(subNode, $t)}</span>
            </button>
          </li>
        {/each}
      </ul>
    </section>
  {/if}
</div>

<style>
  .node-narrative {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .node-narrative__head {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md, 6px);
    padding: var(--space-3) var(--space-4);
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .node-narrative__eyebrow {
    margin: 0;
    font-size: var(--text-xs);
    text-transform: uppercase;
  }

  .node-narrative__title-line {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .node-narrative__title {
    margin: 0;
    font-size: var(--text-base);
  }

  .node-narrative__kind {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    text-transform: uppercase;
    font-weight: 500;
  }

  .node-narrative__state,
  .node-narrative__runs,
  .node-narrative__duration {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .node-narrative__duration {
    margin-left: auto;
    color: var(--color-text-muted);
  }

  .node-narrative__detail {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    display: flex;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .node-narrative__label {
    color: var(--color-text-muted);
    font-size: var(--text-xs);
    text-transform: uppercase;
  }

  .node-narrative__empty {
    padding: var(--space-4);
    border: 1px dashed var(--color-border);
    border-radius: var(--radius-md, 6px);
    font-size: var(--text-sm);
  }

  .node-narrative__empty p {
    margin: 0;
  }

  .node-narrative__section {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .node-narrative__section-title {
    margin: 0;
    font-size: var(--text-sm);
    font-weight: 600;
  }

  .node-narrative__tasks,
  .node-narrative__outline {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .child-task,
  .child-node {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    width: 100%;
    padding: var(--space-2);
    border: 1px solid transparent;
    border-radius: var(--radius-sm, 4px);
    background-color: var(--color-bg-surface);
    color: inherit;
    font-size: var(--text-xs);
    font-family: inherit;
    text-align: left;
    cursor: pointer;
    transition: background-color 0.15s ease;
  }

  .child-task:hover,
  .child-node:hover {
    background-color: var(--color-bg-subtle);
  }

  .child-task__glyph,
  .child-node__glyph {
    font-family: monospace;
    font-weight: bold;
    width: 1.2rem;
    text-align: center;
  }

  .child-task__role,
  .child-node__state {
    color: var(--color-text-secondary);
  }

  .child-task__model,
  .child-task__attempt,
  .child-task__duration,
  .child-task__tokens,
  .child-node__kind {
    color: var(--color-text-muted);
  }

  .child-task__activity {
    margin-left: auto;
    color: var(--color-text-muted);
    white-space: nowrap;
  }

  .child-node__title {
    font-weight: 500;
  }

  .child-node__kind {
    font-size: var(--text-xs);
    text-transform: uppercase;
  }
</style>
