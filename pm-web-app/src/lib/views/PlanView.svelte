<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import Button from '../components/Button.svelte';
  import NodeNarrative from '../components/NodeNarrative.svelte';
  import Spinner from '../components/Spinner.svelte';
  import WorkControlPanel from '../components/WorkControlPanel.svelte';
  import WorkflowIcon from '../components/WorkflowIcon.svelte';
  import { phaseIconId } from '../icons/workflowIcons';
  import {
    apiClient,
    type ProcessTree,
    type ProcessNode,
    type LeafTask,
    type TaskActivity,
    type ProcessTokens,
    type ProcessEndedInfo,
    type TokenBreakdownItem,
    type ActivityEntry,
  } from '../api/client';
  import {
    runtimeStore,
    openEpicView,
    openProjectInExplorer,
    openCompletionReport,
    goBack,
    navigateTo,
  } from '../stores/runtime';
  import { formatSplit, formatTokens, formatDurationSeconds } from '../utils/tokenFormat';
  import { stripControlSequences } from '../utils/outputSafety';
  import { getNodeStateIcon, getNodeStateText } from '../utils/processNode';
  import Icon from '../components/Icon.svelte';

  /** What the inspector shows: a leaf task's live feed, or a node's own narrative. */
  type TreeSelection = { kind: 'task'; taskId: string } | { kind: 'node'; nodeId: string };

  let planId = $derived($runtimeStore.selectedPlanId || '');
  let plan = $state<ProcessTree | null>(null);
  let isLoading = $state(true);
  let errorMessage = $state('');
  let showTokenBreakdown = $state(false);
  let isLoadingBreakdown = $state(false);
  let reportPhaseBreakdown = $state<TokenBreakdownItem[] | null>(null);
  let reportRoleBreakdown = $state<TokenBreakdownItem[] | null>(null);

  /**
   * How the plan ended: the process resource's own record, or what Ended Work handed the view.
   */
  let ended: ProcessEndedInfo | null = $derived(
    plan?.ended ??
      ($runtimeStore.selectedPlanEnded?.plan_id === plan?.plan_id
        ? $runtimeStore.selectedPlanEnded
        : null) ??
      null,
  );

  let tokenBreakdown: {
    byPhase: TokenBreakdownItem[];
    byRole: TokenBreakdownItem[];
  } = $derived({
    byPhase:
      (plan?.tokens_so_far?.by_phase?.length ? plan.tokens_so_far.by_phase : null) ??
      reportPhaseBreakdown ??
      [],
    byRole:
      (plan?.tokens_so_far?.by_role?.length ? plan.tokens_so_far.by_role : null) ??
      reportRoleBreakdown ??
      [],
  });

  let selection = $state<TreeSelection | null>(
    $runtimeStore.selectedTaskId ? { kind: 'task', taskId: $runtimeStore.selectedTaskId } : null,
  );
  let selectedTaskId = $derived(selection?.kind === 'task' ? selection.taskId : null);
  let selectedNode = $derived(
    findNode(plan?.nodes ?? [], selection?.kind === 'node' ? selection.nodeId : null),
  );
  let taskActivity = $state<TaskActivity | null>(null);
  let isLoadingActivity = $state(false);

  let expandedNodes = $state<Record<string, boolean>>({});

  /** Every node the process omitted from the tree, at any depth. */
  let prunedNodes = $derived(collectNotInPlan(plan?.nodes ?? []));

  /** The distinct reasons of the omitted nodes, shown once in the pruned group. */
  let prunedReason = $derived(
    [
      ...new Set(
        prunedNodes
          .map((node) => node.prune_reason)
          .filter((reason): reason is string => Boolean(reason)),
      ),
    ].join(' · '),
  );

  let unsubscribeEvents: (() => void) | null = null;
  let unsubscribeActivity: (() => void) | null = null;
  /**
   * The activity load that owns the panel.
   *
   * A task request can resolve after the operator selected something else; the generation is
   * bumped on every load and on every switch away from a task, so a stale answer never replaces
   * the current feed and never opens a subscription the operator cannot see.
   */
  let activityLoadGeneration = 0;

  onMount(async () => {
    unsubscribeEvents = apiClient.subscribeEvents((event) => {
      if (
        event === 'plan.changed' ||
        event === 'activity.changed' ||
        event === 'phase.changed' ||
        event === 'task.off_track' ||
        event === 'project.work_ended'
      ) {
        void loadPlanProcess(false);
      }
    });

    await loadPlanProcess(true);
  });

  onDestroy(() => {
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
    if (unsubscribeActivity) {
      unsubscribeActivity();
      unsubscribeActivity = null;
    }
  });

  $effect(() => {
    if (selectedTaskId && planId) {
      void loadTaskActivity(planId, selectedTaskId);
    } else {
      activityLoadGeneration += 1;
      taskActivity = null;
      if (unsubscribeActivity) {
        unsubscribeActivity();
        unsubscribeActivity = null;
      }
    }
  });

  async function loadPlanProcess(initial = false) {
    if (!planId) return;
    if (initial) isLoading = true;
    errorMessage = '';
    try {
      const res = await apiClient.getPlanProcess(planId);
      plan = res;

      // Expand all nodes by default or ensure active path is expanded
      if (initial && res.nodes) {
        const initExpanded: Record<string, boolean> = {};
        function expand(nodes: ProcessNode[]) {
          for (const n of nodes) {
            initExpanded[n.node_id] = true;
            if (n.sub_nodes && n.sub_nodes.length > 0) {
              expand(n.sub_nodes);
            }
          }
        }
        expand(res.nodes);
        expandedNodes = initExpanded;
      }

      // Keep the inspected subject while it still exists; otherwise fall back deliberately, so a
      // refresh never leaves the inspector on an item the tree no longer shows.
      if (res.nodes && !isSelectionValid(res.nodes, selection)) {
        selection = defaultSelection(res.nodes);
      }
    } catch (err: unknown) {
      const message = (err as Error)?.message;
      errorMessage =
        message === 'plan_not_found'
          ? $t('plan.view.not_found')
          : message || $t('plan.view.load_failed');
    } finally {
      if (initial) isLoading = false;
    }
  }

  function findRunningTask(nodes: ProcessNode[]): LeafTask | null {
    for (const node of nodes) {
      if (node.state === 'not_in_plan') continue;
      if (node.leaf_tasks && node.leaf_tasks.length > 0) {
        const running = node.leaf_tasks.find((t) => t.status === 'running');
        if (running) return running;
      }
      if (node.sub_nodes && node.sub_nodes.length > 0) {
        const found = findRunningTask(node.sub_nodes);
        if (found) return found;
      }
    }
    return null;
  }

  /**
   * @param nodes the process tree
   * @param nodeId the node to find, or `null`
   * @returns the node, or `null` when the tree does not carry it
   */
  function findNode(nodes: ProcessNode[], nodeId: string | null): ProcessNode | null {
    if (!nodeId) return null;
    for (const node of nodes) {
      if (node.state === 'not_in_plan') continue;
      if (node.node_id === nodeId) return node;
      const found = findNode(node.sub_nodes ?? [], nodeId);
      if (found) return found;
    }
    return null;
  }

  /**
   * @param nodes the process tree
   * @param taskId the leaf task to find
   * @returns the task, or `null` when no visible node carries it
   */
  function findTask(nodes: ProcessNode[], taskId: string): LeafTask | null {
    for (const node of nodes) {
      if (node.state === 'not_in_plan') continue;
      const task = node.leaf_tasks?.find((candidate) => candidate.task_id === taskId);
      if (task) return task;
      const found = findTask(node.sub_nodes ?? [], taskId);
      if (found) return found;
    }
    return null;
  }

  /**
   * @param nodes the process tree
   * @returns the first running node the tree shows, or `null`
   */
  function findRunningNode(nodes: ProcessNode[]): ProcessNode | null {
    for (const node of nodes) {
      if (node.state === 'not_in_plan') continue;
      if (node.state === 'running') return node;
      const found = findRunningNode(node.sub_nodes ?? []);
      if (found) return found;
    }
    return null;
  }

  /**
   * @param nodes the process tree
   * @returns the first node the tree shows, or `null`
   */
  function findFirstVisibleNode(nodes: ProcessNode[]): ProcessNode | null {
    return nodes.find((node) => node.state !== 'not_in_plan') ?? null;
  }

  /**
   * The deliberate fallback order: a running task, else a running node, else the first visible
   * node. The inspector therefore always names something the tree shows.
   *
   * @param nodes the process tree
   * @returns the selection to inspect
   */
  function defaultSelection(nodes: ProcessNode[]): TreeSelection | null {
    const runningTask = findRunningTask(nodes);
    if (runningTask) {
      return { kind: 'task', taskId: runningTask.task_id };
    }
    const runningNode = findRunningNode(nodes);
    if (runningNode) {
      return { kind: 'node', nodeId: runningNode.node_id };
    }
    const firstNode = findFirstVisibleNode(nodes);
    if (firstNode) {
      return { kind: 'node', nodeId: firstNode.node_id };
    }
    return null;
  }

  /**
   * @param nodes the process tree
   * @param current the selection to check
   * @returns whether the tree still shows the selected subject
   */
  function isSelectionValid(nodes: ProcessNode[], current: TreeSelection | null): boolean {
    if (!current) return false;
    if (current.kind === 'task') {
      return findTask(nodes, current.taskId) !== null;
    }
    const node = findNode(nodes, current.nodeId);
    return node !== null && node.state !== 'not_in_plan';
  }

  /** A field that is a string, or `null`; JSON says nothing about the type it carries. */
  function textOf(value: unknown): string | null {
    return typeof value === 'string' ? value : null;
  }

  /** The string items of a JSON array, and nothing else. */
  function stringsOf(value: unknown): string[] {
    return Array.isArray(value)
      ? value.filter((item): item is string => typeof item === 'string')
      : [];
  }

  /**
   * Reads one live-activity event into an entry.
   *
   * The stream reports the entry either directly (`task.activity`) or inside an `entry` field
   * (`activity`); a text entry names its body `text` in one and `content` in the other. Every
   * field is checked, because the renderer hands them to text helpers that require strings.
   */
  function toActivityEntry(event: string, data: unknown): ActivityEntry | null {
    if (event !== 'task.activity' && event !== 'activity') return null;
    if (!data || typeof data !== 'object') return null;
    const payload = data as Record<string, unknown>;
    const source =
      payload.entry && typeof payload.entry === 'object'
        ? (payload.entry as Record<string, unknown>)
        : payload;
    if (typeof source.kind !== 'string') return null;
    return {
      timestamp: textOf(source.timestamp) ?? '',
      kind: source.kind,
      tool_name: textOf(source.tool_name),
      tool_target: textOf(source.tool_target),
      tool_failed: source.tool_failed === true,
      content: textOf(source.content) ?? textOf(source.text),
      size: textOf(source.size),
      files: stringsOf(source.files),
      questions: stringsOf(source.questions),
    };
  }

  async function loadTaskActivity(pId: string, tId: string) {
    const generation = ++activityLoadGeneration;
    if (unsubscribeActivity) {
      unsubscribeActivity();
      unsubscribeActivity = null;
    }

    isLoadingActivity = true;
    try {
      const loaded = await apiClient.getTaskActivity(pId, tId);
      if (generation !== activityLoadGeneration) return;
      taskActivity = loaded;
      unsubscribeActivity = apiClient.subscribeTaskActivity(pId, tId, (event, data) => {
        const entry = toActivityEntry(event, data);
        if (taskActivity && entry) {
          taskActivity = {
            ...taskActivity,
            entries: [...taskActivity.entries, entry],
          };
        }
      });
    } catch {
      if (generation === activityLoadGeneration) {
        taskActivity = null;
      }
    } finally {
      if (generation === activityLoadGeneration) {
        isLoadingActivity = false;
      }
    }
  }

  function toggleNode(nodeId: string) {
    expandedNodes = {
      ...expandedNodes,
      [nodeId]: !expandedNodes[nodeId],
    };
  }

  function jumpToCurrentPosition() {
    if (!plan) return;
    const path = plan.current_position_path || [];
    const nextExp = { ...expandedNodes };
    for (const p of path) {
      nextExp[p] = true;
    }
    expandedNodes = nextExp;

    const running = findRunningTask(plan.nodes);
    if (running) {
      selection = { kind: 'task', taskId: running.task_id };
    } else {
      const runningNode = findRunningNode(plan.nodes);
      if (runningNode) {
        selection = { kind: 'node', nodeId: runningNode.node_id };
      }
    }
    if (typeof document !== 'undefined') {
      const el = document.querySelector('.node-item--running, .node-item--waiting');
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }
  }

  function formatCoverage(tokens: ProcessTokens): string {
    return $t('common.tokens.coverage', {
      unmeasured: Math.max(0, tokens.tasks_total - tokens.tasks_measured),
      total: tokens.tasks_total,
      reason: 'none',
    });
  }

  function formatTokensLine(tokens?: ProcessTokens | null, loc = 'en'): string {
    if (!tokens || tokens.none_yet) {
      return $t('plan.view.tokens_none');
    }
    if (tokens.not_measured) {
      return `${$t('common.tokens.not_measured')} · ${formatCoverage(tokens)}`;
    }
    const split = formatSplit(tokens.total, tokens.input, tokens.output, tokens.floor, loc);
    if (tokens.floor && tokens.tasks_total > tokens.tasks_measured) {
      return `${split} · ${formatCoverage(tokens)}`;
    }
    return split;
  }

  /**
   * Goes back to the view the plan was opened from.
   *
   * The caller's navigation stack names that view; a plan opened by a deep link or a reload has
   * none, and falls back to Running Work.
   */
  function goBackToCaller() {
    if (!goBack()) {
      navigateTo('overview');
    }
  }

  function openEpic(epicId: string) {
    openEpicView(epicId);
  }

  function openProject(projectId: string) {
    openProjectInExplorer(projectId);
  }

  function openReport(pId: string) {
    openCompletionReport(pId);
  }

  /**
   * Opens or closes the one-step token breakdown.
   *
   * The process resource may carry the breakdown itself. When it does not, the completion
   * resource is the second source (its token usage record holds the same figure by phase and by
   * role); a plan that has not ended yet reports none, and the disclosure says so.
   */
  async function toggleTokenBreakdown() {
    showTokenBreakdown = !showTokenBreakdown;
    if (
      !showTokenBreakdown ||
      tokenBreakdown.byPhase.length > 0 ||
      tokenBreakdown.byRole.length > 0
    ) {
      return;
    }
    // A running plan has no completion report to read the breakdown from; the disclosure says
    // the figure is not reported yet rather than asking for a report that does not exist.
    if (!ended) {
      return;
    }
    isLoadingBreakdown = true;
    try {
      const report = await apiClient.getPlanCompletion(planId);
      reportPhaseBreakdown = report.token_usage?.by_phase ?? [];
      reportRoleBreakdown = report.token_usage?.by_role ?? [];
    } catch {
      reportPhaseBreakdown = [];
      reportRoleBreakdown = [];
    } finally {
      isLoadingBreakdown = false;
    }
  }

  function formatBreakdownValue(value?: number | null): string {
    return formatTokens(value ?? 0, false, $currentLocale);
  }

  /**
   * Formats the end time the ended plan carries.
   *
   * @param iso the runtime's ISO timestamp
   * @returns the timestamp in the operator's locale, or the raw value when it is not a date
   */
  function formatEndedAt(iso: string): string {
    const date = new Date(iso);
    return Number.isNaN(date.getTime()) ? iso : date.toLocaleString($currentLocale);
  }

  /**
   * The cached tokens of a breakdown row: cache reads and cache writes, as the completion
   * report counts them.
   *
   * @param item one row of the runtime's breakdown
   * @returns the row's cached tokens, or `null` when the row reports no components
   */
  function cachedOf(item: TokenBreakdownItem): number | null {
    if (!item.components) {
      return null;
    }
    return (item.components.cache_read ?? 0) + (item.components.cache_write ?? 0);
  }

  /**
   * Formats one breakdown cell, naming an unreported component rather than showing a zero.
   *
   * @param value the token count, or `null` when the runtime did not report it
   * @returns the catalogue-formatted count, or a dash
   */
  function formatComponent(value: number | null): string {
    return value === null ? '—' : formatBreakdownValue(value);
  }

  function selectTask(taskId: string) {
    selection = { kind: 'task', taskId };
  }

  function selectNode(nodeId: string) {
    selection = { kind: 'node', nodeId };
  }

  function isNodeSelected(nodeId: string): boolean {
    return selection?.kind === 'node' && selection.nodeId === nodeId;
  }

  /**
   * @param nodes the process tree
   * @returns every omitted node at any depth, without descending into an omitted node
   */
  function collectNotInPlan(nodes: ProcessNode[]): ProcessNode[] {
    const pruned: ProcessNode[] = [];
    for (const node of nodes) {
      if (node.state === 'not_in_plan') {
        pruned.push(node);
        continue;
      }
      pruned.push(...collectNotInPlan(node.sub_nodes ?? []));
    }
    return pruned;
  }

  /**
   * @param node a process node
   * @returns its sub-nodes that the tree shows
   */
  function visibleSubNodes(node: ProcessNode): ProcessNode[] {
    return (node.sub_nodes ?? []).filter((child) => child.state !== 'not_in_plan');
  }

  function hasChildren(node: ProcessNode): boolean {
    return visibleSubNodes(node).length > 0 || (node.leaf_tasks?.length ?? 0) > 0;
  }
</script>

<div class="plan-view" data-testid="plan-view">
  {#if isLoading}
    <div class="plan-view__loading" data-testid="plan-loading">
      <Spinner size="2.5rem" label={$t('plan.view.loading')} />
    </div>
  {:else if errorMessage}
    <div class="plan-view__error" role="alert" data-testid="plan-error">
      <p>{errorMessage}</p>
      <Button variant="secondary" onclick={() => loadPlanProcess(true)}
        >{$t('web.common.retry')}</Button
      >
    </div>
  {:else if plan}
    <!-- Path Navigation Header -->
    <header class="plan-header" data-testid="plan-header">
      <div class="plan-header__top">
        <nav
          class="plan-breadcrumb mono"
          aria-label={$t('common.breadcrumb')}
          data-testid="plan-breadcrumb"
        >
          <button
            class="breadcrumb-link"
            onclick={() => openProject(plan!.project.project_id)}
            data-testid="breadcrumb-project"
          >
            {plan.project?.display_name || plan.project?.project_id || 'project'}
          </button>
          {#if plan.epic}
            <span class="breadcrumb-sep">›</span>
            <button
              class="breadcrumb-link"
              onclick={() => openEpic(plan!.epic!.epic_id)}
              data-testid="breadcrumb-epic"
            >
              {plan.epic.title || plan.epic.epic_id}
            </button>
          {/if}
          <span class="breadcrumb-sep">›</span>
          <span class="breadcrumb-current font-semibold" data-testid="breadcrumb-plan">
            {plan.plan_id}
          </span>
        </nav>

        <div class="plan-header__actions">
          <Button
            variant="secondary"
            onclick={() => navigateTo('designer', { projectId: plan!.project.project_id })}
            data-testid="design-process-btn"
            data-advisory={$t('designer.plan.open.title')}
          >
            {$t('designer.plan.open')}
          </Button>
          <Button
            variant="secondary"
            onclick={jumpToCurrentPosition}
            data-testid="jump-btn"
            data-advisory={$t('plan.path.jump.title')}
          >
            {$t('plan.path.jump')}
          </Button>
          {#if plan.pull_request_number}
            <a
              href={plan.pull_request_url || '#'}
              target="_blank"
              rel="noreferrer"
              class="pr-badge-link"
              data-testid="pr-link"
            >
              #{plan.pull_request_number}
            </a>
          {/if}
          <button
            class="report-link-btn"
            onclick={() => openReport(plan!.plan_id)}
            data-testid="completion-report-link"
            data-advisory={$t('report.action.details.title')}
          >
            {$t('report.action.details')}
          </button>
          <Button
            variant="ghost"
            onclick={goBackToCaller}
            data-testid="back-btn"
            data-advisory={$t('common.actions.back.title')}
          >
            {$t('common.actions.back')}
          </Button>
        </div>
      </div>

      <!-- Plan Title and Status Line -->
      <div class="plan-headline">
        <h1 class="plan-title" data-testid="plan-title">{plan.title}</h1>
        <div class="plan-status-tags">
          <span class="badge badge--phase" data-testid="plan-phase">
            <WorkflowIcon id={phaseIconId(plan.current_phase)} size={16} />
            {plan.current_phase}
          </span>
          <span class="badge badge--state badge--state-{plan.phase_state}" data-testid="plan-state">
            {plan.phase_state}
          </span>
          {#if plan.duration_seconds > 0}
            <span class="duration-tag">{formatDurationSeconds(plan.duration_seconds)}</span>
          {/if}
        </div>
      </div>

      <!-- An Ended Plan -->
      {#if ended}
        <div class="ended-banner" data-testid="plan-ended">
          <div class="ended-banner__text">
            <span class="ended-outcome">
              {#if ended.outcome}
                {$t('plan.view.ended', {
                  outcome: $t('overview.ended.outcome', { outcome: ended.outcome }),
                })}
              {:else}
                {$t('plan.view.ended_none')}
              {/if}
            </span>
            {#if ended.ended_at}
              <span class="ended-time text-muted">
                {$t('overview.ended.time', { time: formatEndedAt(ended.ended_at) })}
              </span>
            {/if}
            {#if ended.reason}
              <span class="ended-reason text-muted">
                {$t('overview.ended.reason', { reason: ended.reason })}
              </span>
            {/if}
          </div>
          <button
            class="report-link-btn"
            onclick={() => openReport(plan!.plan_id)}
            data-testid="ended-completion-report"
            data-advisory={$t('report.completion.open.title')}
          >
            {$t('report.completion.open')}
          </button>
        </div>
      {/if}

      <!-- The plan's own actions, settings, holds, and chats -->
      <WorkControlPanel
        subjectId={plan.plan_id}
        subjectKind="plan"
        projectId={plan.project.project_id}
        ended={Boolean(ended)}
        pullRequestNumber={plan.pull_request_number ?? null}
        onChanged={() => void loadPlanProcess(false)}
      />

      <!-- Tokens So Far -->
      <div class="plan-tokens mono" data-testid="plan-tokens">
        <span class="tokens-label">{$t('plan.view.tokens')}</span>
        <span class="tokens-value">{formatTokensLine(plan.tokens_so_far, $currentLocale)}</span>
        {#if plan.tokens_so_far && !plan.tokens_so_far.none_yet}
          <button
            class="tokens-toggle"
            aria-expanded={showTokenBreakdown}
            aria-controls="plan-token-breakdown"
            onclick={toggleTokenBreakdown}
            data-testid="tokens-toggle"
          >
            {showTokenBreakdown
              ? $t('plan.view.tokens_detail_hide')
              : $t('plan.view.tokens_detail')}
          </button>
        {/if}
      </div>

      {#if showTokenBreakdown && plan.tokens_so_far}
        <div class="token-breakdown" id="plan-token-breakdown" data-testid="token-breakdown">
          {#if isLoadingBreakdown}
            <div class="token-breakdown__empty">
              <Spinner size="1rem" label={$t('plan.view.tokens_detail')} />
            </div>
          {:else if tokenBreakdown.byPhase.length === 0 && tokenBreakdown.byRole.length === 0}
            <p class="token-breakdown__empty text-muted">
              {$t('plan.view.tokens_detail_waiting')}
            </p>
          {:else}
            {#if tokenBreakdown.byPhase.length > 0}
              <section class="token-breakdown__group" data-testid="token-breakdown-phase">
                <h3 class="token-breakdown__title">{$t('report.completion.by_phase')}</h3>
                <table class="token-table">
                  <thead>
                    <tr>
                      <th scope="col">{$t('report.completion.by_phase')}</th>
                      <th scope="col">{$t('plan.tokens.col.input')}</th>
                      <th scope="col">{$t('plan.tokens.col.cached')}</th>
                      <th scope="col">{$t('plan.tokens.col.output')}</th>
                      <th scope="col">{$t('plan.tokens.col.total')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {#each tokenBreakdown.byPhase as item (item.name)}
                      <tr data-testid="token-phase-{item.name}">
                        <th scope="row" class="mono">{item.name}</th>
                        <td class="mono">{formatBreakdownValue(item.input)}</td>
                        <td class="mono">{formatComponent(cachedOf(item))}</td>
                        <td class="mono">{formatBreakdownValue(item.output)}</td>
                        <td class="mono">{formatBreakdownValue(item.total)}</td>
                      </tr>
                    {/each}
                  </tbody>
                </table>
              </section>
            {/if}

            {#if tokenBreakdown.byRole.length > 0}
              <section class="token-breakdown__group" data-testid="token-breakdown-role">
                <h3 class="token-breakdown__title">{$t('report.completion.by_role')}</h3>
                <table class="token-table">
                  <thead>
                    <tr>
                      <th scope="col">{$t('report.completion.by_role')}</th>
                      <th scope="col">{$t('plan.tokens.col.input')}</th>
                      <th scope="col">{$t('plan.tokens.col.cached')}</th>
                      <th scope="col">{$t('plan.tokens.col.output')}</th>
                      <th scope="col">{$t('plan.tokens.col.total')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {#each tokenBreakdown.byRole as item (item.name)}
                      <tr data-testid="token-role-{item.name}">
                        <th scope="row" class="mono">{item.name}</th>
                        <td class="mono">{formatBreakdownValue(item.input)}</td>
                        <td class="mono">{formatComponent(cachedOf(item))}</td>
                        <td class="mono">{formatBreakdownValue(item.output)}</td>
                        <td class="mono">{formatBreakdownValue(item.total)}</td>
                      </tr>
                    {/each}
                  </tbody>
                </table>
              </section>
            {/if}
          {/if}
        </div>
      {/if}
    </header>

    <!-- Two-Panel Main Layout -->
    <div class="plan-content">
      <!-- Left: Process Tree -->
      <section
        class="plan-tree-panel"
        aria-label={$t('plan.view.tree_label')}
        data-testid="process-tree"
      >
        {#snippet processNode(node: ProcessNode, level: 'root' | 'sub')}
          <div
            class="node-item {level === 'root'
              ? 'node-item--level-1'
              : 'node-item--sub'} node-item--{node.state}"
            data-testid="node-{node.node_id}"
          >
            <div class="node-row {isNodeSelected(node.node_id) ? 'node-row--selected' : ''}">
              <button
                type="button"
                class="toggle-btn"
                onclick={() => toggleNode(node.node_id)}
                aria-label={level === 'root'
                  ? $t('plan.view.toggle_node')
                  : $t('plan.view.toggle_sub_node')}
                data-advisory={level === 'root'
                  ? $t('plan.view.toggle_node.title')
                  : $t('plan.view.toggle_sub_node.title')}
                aria-expanded={hasChildren(node) ? Boolean(expandedNodes[node.node_id]) : undefined}
                data-testid="toggle-node-{node.node_id}"
              >
                <span class="node-glyph"><Icon name={getNodeStateIcon(node.state)} size={16} /></span>
                <span class="toggle-icon">
                  {#if hasChildren(node)}
                    {expandedNodes[node.node_id] ? '▾' : '▸'}
                  {:else}
                    &nbsp;
                  {/if}
                </span>
              </button>

              <button
                type="button"
                class="node-info node-select"
                onclick={() => selectNode(node.node_id)}
                aria-pressed={isNodeSelected(node.node_id)}
                aria-current={isNodeSelected(node.node_id) ? 'true' : undefined}
                aria-label={`${stripControlSequences(node.title)} — ${$t('plan.node.select')}`}
                data-advisory={$t('plan.node.select.title')}
                data-testid="select-node-{node.node_id}"
              >
                {#if level === 'root' && node.kind === 'phase'}
                  <WorkflowIcon id={phaseIconId(node.node_id)} size={16} />
                {/if}
                <span class="node-kind-badge">{level === 'sub' ? `↳ ${node.kind}` : node.kind}</span
                >
                <span class="node-title font-semibold">{stripControlSequences(node.title)}</span>
                <span class="node-state-text">{getNodeStateText(node, $t)}</span>
                {#if node.duration_seconds > 0}
                  <span class="node-duration">{formatDurationSeconds(node.duration_seconds)}</span>
                {/if}
              </button>
            </div>

            <!-- Children (sub-nodes at every depth, then this node's leaf tasks) -->
            {#if expandedNodes[node.node_id]}
              <div class="node-children">
                {#if node.sub_nodes && node.sub_nodes.length > 0}
                  {#each visibleSubNodes(node) as child (child.node_id)}
                    {@render processNode(child, 'sub')}
                  {/each}
                {/if}
                {#if node.leaf_tasks && node.leaf_tasks.length > 0}
                  <div class="tasks-list">
                    {#each node.leaf_tasks as task (task.task_id)}
                      <button
                        class="task-row {selectedTaskId === task.task_id
                          ? 'task-row--selected'
                          : ''}"
                        onclick={() => selectTask(task.task_id)}
                        aria-current={selectedTaskId === task.task_id ? 'true' : undefined}
                        data-testid="task-{task.task_id}"
                        data-advisory={$t('plan.view.select_task.title')}
                      >
                        <span class="task-glyph"
                          ><Icon name={getNodeStateIcon(task.status)} size={16} /></span
                        >
                        <span class="task-id font-semibold">{task.task_id}</span>
                        <span class="task-role">{task.role}</span>
                        <span class="task-model">{task.harness} {task.model}</span>
                        <span class="task-attempt">attempt {task.attempt}</span>
                        <span class="task-status badge badge--state badge--state-{task.status}"
                          >{task.status}</span
                        >
                        {#if task.off_track}
                          <span class="badge badge--warning" data-testid="task-off-track-badge">
                            off track: {task.off_track.signal}
                          </span>
                        {/if}
                      </button>
                    {/each}
                  </div>
                {/if}
              </div>
            {/if}
          </div>
        {/snippet}

        <div class="tree-container">
          {#each plan.nodes.filter((n) => n.state !== 'not_in_plan') as node (node.node_id)}
            {@render processNode(node, 'root')}
          {/each}

          <!-- Pruned / Not in plan section -->
          {#if prunedNodes.length > 0}
            <div class="not-in-plan-box" data-testid="not-in-plan-box">
              <span class="not-in-plan-glyph">·</span>
              <span class="not-in-plan-text">
                {$t('plan.not_in_plan.group')}: {prunedNodes
                  .map((node) => stripControlSequences(node.title))
                  .join(', ')}
                {#if prunedReason}
                  ({stripControlSequences(prunedReason)})
                {/if}
              </span>
            </div>
          {/if}
        </div>
      </section>

      <!-- Right: Worker Task Activity Inspector -->
      <section
        class="plan-detail-panel"
        aria-label={$t('plan.view.inspector_label')}
        data-testid="task-inspector"
        data-selected-kind={selection?.kind ?? 'none'}
      >
        {#if selection?.kind === 'task'}
          {#if isLoadingActivity}
            <div class="detail-loading">
              <Spinner size="2rem" label={$t('plan.activity.loading')} />
            </div>
          {:else if taskActivity}
            <div class="task-inspector" data-testid="activity-feed">
              <div class="task-inspector__head">
                <div class="task-headline">
                  <h3 class="task-title font-semibold">{taskActivity.task_id}</h3>
                  <span class="task-role-tag">{taskActivity.role}</span>
                  <span class="task-meta">{taskActivity.harness} {taskActivity.model}</span>
                  <span class="task-meta">attempt {taskActivity.attempt}</span>
                </div>
                <div class="task-metrics mono">
                  <span>{formatDurationSeconds(taskActivity.duration_seconds)}</span>
                  {#if taskActivity.tokens_so_far > 0}
                    <span>· {taskActivity.tokens_so_far} tokens</span>
                  {/if}
                </div>
              </div>

              <!-- Off-track alert banner -->
              {#if taskActivity.off_track}
                <div class="off-track-banner" data-testid="off-track-alert" role="alert">
                  <span class="warning-icon"><Icon name="state.alert" size={16} /></span>
                  <span class="off-track-msg">
                    {$t('plan.activity.off_track', {
                      signal: taskActivity.off_track.signal,
                      value: taskActivity.off_track.value ?? '?',
                      threshold: taskActivity.off_track.threshold ?? '?',
                      action: taskActivity.off_track.action,
                    })}
                  </span>
                </div>
              {/if}

              <!-- Activity Entries -->
              <div class="activity-entries">
                {#if taskActivity.entries.length === 0}
                  <div class="activity-empty text-muted">{$t('plan.activity.empty')}</div>
                {:else}
                  {#each taskActivity.entries as entry}
                    <div
                      class="activity-entry activity-entry--{entry.kind}"
                      data-testid="activity-entry"
                    >
                      <span class="entry-time mono">{entry.timestamp}</span>
                      <span class="entry-kind-tag entry-kind-tag--{entry.kind}">{entry.kind}</span>
                      <div class="entry-body">
                        {#if entry.kind === 'tool' || entry.kind === 'tool_call'}
                          <div class="tool-call mono">
                            <span class="tool-name">{entry.tool_name}</span>
                            <span class="tool-target"
                              >{stripControlSequences(entry.tool_target)}</span
                            >
                            {#if entry.tool_failed}
                              <span class="badge badge--danger">failed · {entry.size || ''}</span>
                            {/if}
                          </div>
                        {:else if entry.kind === 'changed' || entry.kind === 'file_changed'}
                          <div class="file-changed mono" data-model-text>
                            {stripControlSequences(
                              entry.content || (entry.files && entry.files[0]) || '',
                            )}
                          </div>
                        {:else if entry.kind === 'gap'}
                          <div class="activity-gap text-muted italic" data-model-text>
                            {stripControlSequences(entry.content || '')}
                          </div>
                        {:else}
                          <div class="entry-content" data-model-text>
                            {stripControlSequences(entry.content)}
                          </div>
                        {/if}
                      </div>
                    </div>
                  {/each}
                {/if}
              </div>
            </div>
          {:else}
            <div class="no-task-selected text-muted" data-testid="task-no-activity">
              <p>{$t('plan.activity.empty')}</p>
            </div>
          {/if}
        {:else if selectedNode}
          <NodeNarrative
            node={selectedNode}
            {plan}
            onSelectTask={selectTask}
            onSelectNode={selectNode}
          />
        {:else}
          <div class="no-task-selected text-muted" data-testid="nothing-selected">
            <p>{$t('plan.activity.select_item')}</p>
          </div>
        {/if}
      </section>
    </div>
  {/if}
</div>

<style>
  .plan-view {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: calc(100vh - 4rem);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .plan-view__loading,
  .plan-view__error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: var(--space-8);
    gap: var(--space-4);
  }

  .plan-header {
    background-color: var(--color-bg-surface);
    border-bottom: 1px solid var(--color-border);
    padding: var(--space-4) var(--space-6);
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .plan-header__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-4);
    flex-wrap: wrap;
  }

  .plan-breadcrumb {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
  }

  .breadcrumb-link {
    background: none;
    border: none;
    padding: 0;
    color: var(--color-accent);
    cursor: pointer;
    text-decoration: underline;
    font-size: inherit;
    font-family: inherit;
  }

  .breadcrumb-link:hover {
    color: var(--color-accent-hover);
  }

  .breadcrumb-sep {
    color: var(--color-text-muted);
  }

  .breadcrumb-current {
    color: var(--color-text-primary);
  }

  .plan-header__actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    min-inline-size: 0;
    max-inline-size: 100%;
  }

  .pr-badge-link {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
    border: 1px solid var(--color-border);
    color: var(--color-accent);
    font-size: var(--text-xs);
    font-family: monospace;
    text-decoration: none;
  }

  .report-link-btn {
    background: none;
    border: none;
    padding: var(--space-1) var(--space-2);
    color: var(--color-text-secondary);
    cursor: pointer;
    font-size: var(--text-sm);
    text-decoration: underline;
  }

  .plan-headline {
    display: flex;
    align-items: center;
    gap: var(--space-4);
    flex-wrap: wrap;
  }

  .plan-title {
    font-size: var(--text-xl, 1.25rem);
    font-weight: 700;
    margin: 0;
  }

  .plan-status-tags {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .badge--phase {
    display: inline-flex;
    align-items: center;
    gap: 0.35em;
  }

  .plan-tokens {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    display: flex;
    gap: var(--space-2);
  }

  .tokens-label {
    color: var(--color-text-muted);
  }

  .tokens-value {
    font-weight: 500;
  }

  .tokens-toggle {
    background: none;
    border: none;
    padding: 0;
    color: var(--color-primary);
    cursor: pointer;
    font-size: var(--text-xs);
    font-family: inherit;
    text-decoration: underline;
  }

  .tokens-toggle:hover {
    color: var(--color-primary-hover);
  }

  .ended-banner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
    flex-wrap: wrap;
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border-subtle);
    border-left: 3px solid var(--color-text-muted);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
    font-size: var(--text-sm);
  }

  .ended-banner__text {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    flex-wrap: wrap;
  }

  .ended-outcome {
    font-weight: 600;
  }

  .token-breakdown {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-3);
  }

  .token-breakdown__group {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    overflow-x: auto;
  }

  .token-breakdown__title {
    margin: 0;
    font-size: var(--text-sm);
    font-weight: 600;
  }

  .token-breakdown__empty {
    margin: 0;
    font-size: var(--text-xs);
  }

  .token-table {
    width: 100%;
    border-collapse: collapse;
    font-size: var(--text-xs);
  }

  .token-table th,
  .token-table td {
    padding: var(--space-1) var(--space-2);
    text-align: right;
    border-bottom: 1px solid var(--color-border-subtle);
  }

  .token-table th:first-child,
  .token-table td:first-child {
    text-align: left;
  }

  .token-table thead th {
    color: var(--color-text-muted);
    font-weight: 500;
  }

  /* Two-panel layout */
  .plan-content {
    display: flex;
    flex: 1;
    overflow: hidden;
  }

  .plan-tree-panel {
    flex: 1;
    min-width: 320px;
    border-right: 1px solid var(--color-border);
    overflow-y: auto;
    padding: var(--space-4);
    background-color: var(--color-bg-surface);
  }

  .plan-detail-panel {
    flex: 1.2;
    min-width: 360px;
    overflow-y: auto;
    padding: var(--space-4);
    background-color: var(--color-bg-app);
  }

  .tree-container {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .node-item {
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    overflow: hidden;
  }

  .node-item--running {
    border-left: 3px solid var(--color-accent);
  }

  .node-item--waiting {
    border-left: 3px solid var(--color-warning);
  }

  .node-row {
    display: flex;
    align-items: center;
    padding: var(--space-2) var(--space-3);
    gap: var(--space-2);
    border-radius: var(--radius-sm);
  }

  .node-row--selected {
    background-color: var(--color-primary-subtle);
  }

  .node-select {
    padding: var(--space-1);
    margin: calc(var(--space-1) * -1) 0;
    border: none;
    border-radius: var(--radius-sm);
    background: none;
    color: inherit;
    font-family: inherit;
    text-align: left;
    cursor: pointer;
  }

  .node-select:hover {
    background-color: var(--color-bg-subtle-hover);
  }

  .toggle-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    background: none;
    border: none;
    cursor: pointer;
    padding: 0;
    color: inherit;
    font-size: inherit;
  }

  .node-glyph {
    font-family: monospace;
    font-weight: bold;
    width: 1.2rem;
    text-align: center;
  }

  .toggle-icon {
    font-size: 0.8rem;
    color: var(--color-text-muted);
  }

  .node-info {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex: 1;
    font-size: var(--text-sm);
  }

  .node-kind-badge {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    text-transform: uppercase;
    font-weight: 500;
  }

  .node-state-text {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .node-duration {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    margin-left: auto;
  }

  .node-children {
    padding-left: var(--space-6);
    padding-bottom: var(--space-2);
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    border-top: 1px solid var(--color-border-subtle);
  }

  .node-item--sub {
    border: none;
    background: transparent;
  }

  .tasks-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 4px 0 4px var(--space-4);
  }

  .task-row {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-2);
    border-radius: var(--radius-sm);
    border: 1px solid transparent;
    background-color: var(--color-bg-subtle);
    cursor: pointer;
    text-align: left;
    font-size: var(--text-xs);
    transition: background-color 0.15s ease;
  }

  .task-row:hover {
    background-color: var(--color-bg-subtle-hover);
  }

  .task-row--selected {
    background-color: var(--color-primary-subtle);
    border-color: var(--color-accent);
  }

  .task-glyph {
    font-family: monospace;
    font-weight: bold;
  }

  .task-id {
    font-family: monospace;
  }

  .task-role {
    color: var(--color-text-secondary);
  }

  .task-model {
    color: var(--color-text-muted);
  }

  .task-attempt {
    color: var(--color-text-muted);
  }

  .not-in-plan-box {
    margin-top: var(--space-4);
    padding: var(--space-3);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .not-in-plan-glyph {
    font-weight: bold;
    font-size: 1.1rem;
  }

  /* Right panel inspector */
  .task-inspector {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .task-inspector__head {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md, 6px);
    padding: var(--space-3) var(--space-4);
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .task-headline {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .task-title {
    margin: 0;
    font-size: var(--text-base);
  }

  .task-role-tag {
    font-size: var(--text-xs);
    padding: 2px 6px;
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-sm);
    font-weight: 500;
  }

  .task-meta {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .task-metrics {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .off-track-banner {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-3);
    background-color: var(--color-warning-subtle);
    border: 1px solid var(--color-warning-border);
    border-radius: var(--radius-md);
    color: var(--color-warning-text);
    font-size: var(--text-sm);
    font-weight: 500;
  }

  .warning-icon {
    font-size: 1.2rem;
  }

  .activity-entries {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .activity-entry {
    display: flex;
    align-items: flex-start;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-3);
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-sm);
    font-size: var(--text-xs);
  }

  .entry-time {
    color: var(--color-text-muted);
    flex-shrink: 0;
  }

  .entry-kind-tag {
    padding: 1px 6px;
    border-radius: 3px;
    font-weight: 600;
    font-size: 10px;
    text-transform: uppercase;
    flex-shrink: 0;
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
  }

  .entry-kind-tag--tool,
  .entry-kind-tag--tool_call {
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
  }

  .entry-kind-tag--changed,
  .entry-kind-tag--file_changed {
    background-color: var(--color-success-subtle);
    color: var(--color-success-text);
  }

  .entry-kind-tag--text {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
  }

  .entry-body {
    flex: 1;
    word-break: break-word;
  }

  .tool-call {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .tool-name {
    font-weight: 600;
    color: var(--color-accent);
  }

  .tool-target {
    color: var(--color-text-primary);
  }

  .no-task-selected {
    padding: var(--space-8);
    text-align: center;
  }

  /* Responsive */
  @media (max-width: 768px) {
    .plan-content {
      flex-direction: column;
    }
    .plan-tree-panel {
      border-right: none;
      border-bottom: 1px solid var(--color-border);
    }
  }
</style>
