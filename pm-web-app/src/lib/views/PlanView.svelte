<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import Button from '../components/Button.svelte';
  import IconButton from '../components/IconButton.svelte';
  import NodeNarrative from '../components/NodeNarrative.svelte';
  import Spinner from '../components/Spinner.svelte';
  import Tabs, { type TabItem } from '../components/Tabs.svelte';
  import PlanChanges from '../components/PlanChanges.svelte';
  import PlanIntentPanel from '../components/PlanIntentPanel.svelte';
  import BuilderCheckList from '../components/BuilderCheckList.svelte';
  import OutlinePanel from '../components/OutlinePanel.svelte';
  import WorkControlPanel from '../components/WorkControlPanel.svelte';
  import WorkListColumn from '../components/WorkListColumn.svelte';
  import UpLink from '../components/UpLink.svelte';
  import WorkflowIcon from '../components/WorkflowIcon.svelte';
  import ObjectPage from '../templates/ObjectPage.svelte';
  import KindMark from '../components/KindMark.svelte';
  import Chip from '../components/Chip.svelte';
  import PrChip from '../components/PrChip.svelte';
  import PhaseLabel from '../components/PhaseLabel.svelte';
  import StatusLabel from '../components/StatusLabel.svelte';
  import AttentionStrip from '../components/AttentionStrip.svelte';
  import Link from '../components/Link.svelte';
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
    type ChangeSizeRecord,
    type OffTrackInfo,
    type BuilderCheckRecord,
    type OutlineDeliverableRecord,
  } from '../api/client';
  import {
    runtimeStore,
    addressOfState,
    selectObjectTab,
    selectPlanChangeFile,
  } from '../stores/runtime';
  import { refreshesPlanProcess } from '../stores/refreshEvents';
  import { refreshesIntentView } from '../components/intentEvents';
  import {
    dateOnRead,
    formatSplit,
    formatTokens,
    formatDuration,
    formatDay,
    formatTime,
    dayOf,
    type Locale,
  } from '../display/format';
  import { stripControlSequences } from '../utils/outputSafety';
  import {
    getNodeDisplayName,
    getNodeKindIcon,
    getNodeStateIcon,
    getNodeStateText,
  } from '../utils/processNode';
  import { displayPruneReason, displayState, formatChangeSize } from '../display';
  import { buildChain, upOf } from '../navigation/chain';
  import { formatAddress, PLAN_TABS, type Address, type PlanTab } from '../routes';
  import { writeAddress } from '../navigation/history';
  import CompletionReportView from './CompletionReportView.svelte';
  import DecisionView from './DecisionView.svelte';
  import { narrowLayout } from '../utils/narrowLayout';
  import Icon from '../components/Icon.svelte';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import { errorCodeOf } from '../api/errors';

  /** What the inspector shows: a leaf task's live feed, or a node's own narrative. */
  type TreeSelection = { kind: 'task'; taskId: string } | { kind: 'node'; nodeId: string };

  let planId = $derived($runtimeStore.selectedPlanId || '');
  let lastLoadedPlanId: string | null = null;

  $effect(() => {
    const current = planId;
    if (current && lastLoadedPlanId !== null && current !== lastLoadedPlanId) {
      lastLoadedPlanId = current;
      reportPhaseBreakdown = null;
      reportRoleBreakdown = null;
      isLoadingBreakdown = false;
      showTokenBreakdown = false;
      queuedRevision = null;
      openDecision = null;
      decisionRequest += 1;
      intentLoading = false;
      intentSettled = false;
      intentError = null;
      intentRequest += 1;
      intentHasRecord = false;
      intentCheck = null;
      intentDeliverables = [];
      void loadPlanProcess(true);
    } else if (current && lastLoadedPlanId === null) {
      lastLoadedPlanId = current;
    }
  });

  let plan = $state<ProcessTree | null>(null);
  const planProjectId = $derived(plan?.project?.project_id ?? $runtimeStore.selectedProjectId ?? '');
  let isLoading = $state(true);
  let errorCode = $state<string | null>(null);

  /**
   * The parent Up leads to, from what this view already holds: the epic of the plan, or the
   * project when the plan belongs to no epic (`navigation.adoc#up`).
   */
  const upParent = $derived(
    upOf(
      buildChain(
        $runtimeStore.currentRoute,
        {
          plan: plan ?? $runtimeStore.currentProcessTree,
          planId,
          projectId: plan?.project?.project_id ?? $runtimeStore.selectedProjectId,
        },
        $t,
      ),
      $runtimeStore.currentRoute,
    ),
  );
  let showTokenBreakdown = $state(false);
  let isLoadingBreakdown = $state(false);
  let reportPhaseBreakdown = $state<TokenBreakdownItem[] | null>(null);
  let reportRoleBreakdown = $state<TokenBreakdownItem[] | null>(null);
  /** The moment the day headers are measured at; a quiet timer moves it across midnight. */
  let clock = $state(Date.now());

  /**
   * The size the last `plan.change_captured` announced, or `null` before the plan's first capture
   * of this load.
   *
   * The line's figures come from the process resource or from the event, never summed here: the
   * list and the size are one measurement of git (`plan-view.adoc`, Change So Far).
   */
  let changeFromEvent = $state<ChangeSizeRecord | null>(null);

  /** A count the view raises on a capture, so the tab reads its list again. */
  let changesReloadToken = $state(0);

  /** The size of the plan's change: the event's figures, else the process resource's. */
  const changeSize = $derived<ChangeSizeRecord | null>(changeFromEvent ?? plan?.change ?? null);

  /** The tab a plan opens on when the address names none. */
  const DEFAULT_TAB: PlanTab = 'process';

  /**
   * The plan's tab the address names, or the default one the address omits.
   *
   * The plan carries the tab set of D6: Process (the default), Intent and Changes
   * (`plan-view-interaction.adoc#addresses`).
   */
  const activeTab = $derived<PlanTab>(
    PLAN_TABS.find((tab) => tab === $runtimeStore.objectTab) ?? DEFAULT_TAB,
  );

  /**
   * @param tab a tab of this view
   * @returns the id of its panel while that tab is the shown one, and none for the others
   */
  function panelIdOf(tab: PlanTab): string | undefined {
    // Only the shown tab has a panel in the document, so only it may name one.
    return tab === activeTab ? `plan-panel-${tab}` : undefined;
  }

  /** The catalogue key of each plan tab; the set itself is the router's `PLAN_TABS`. */
  const TAB_LABEL_KEYS: Record<PlanTab, string> = {
    process: 'plan.tab.process',
    intent: 'plan.tab.intent',
    changes: 'plan.tab.changes',
  };

  /**
   * The tabs of the plan's column, in the router's order: every plan tab, except Changes before
   * the first capture — the same condition as the size line's
   * (`plan-view-interaction.adoc#layout`).
   */
  const tabItems = $derived<TabItem[]>(
    PLAN_TABS.filter((tab) => tab !== 'changes' || changeSize !== null).map((tab) => ({
      id: tab,
      label: $t(TAB_LABEL_KEYS[tab]),
      panelId: panelIdOf(tab),
    })),
  );

  /**
   * @param tab the tab a person chose
   */
  function selectTab(tab: string): void {
    // The Tabs component emits the ids this view gave it, so the tab is one of the router's set.
    const known = PLAN_TABS.find((entry) => entry === tab);
    if (known) {
      selectObjectTab(known);
    }
  }

  /**
   * @param size the size the runtime served
   * @returns the catalogue's one-line form of it
   */
  function changeText(size: ChangeSizeRecord): string {
    return formatChangeSize(size, $t);
  }
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

  /** The plan waits for the operator: the strip and the decision head link belong to that state. */
  const waiting = $derived(
    plan !== null && (plan.phase_state === 'waiting_for_you' || plan.needs_operator),
  );

  /**
   * The open decision of the plan, read once the plan waits for one.
   *
   * The head link and the strip's wording both hang on it: a plan that waits for a hold or a
   * credential gets no decision sentence, and a plan whose decisions cannot be read keeps no link
   * to nowhere (`plan-view-interaction.adoc#layout`).
   */
  let openDecision = $state<{ planId: string; id: string } | null>(null);
  /** The decision read that owns the link; a superseded answer never writes it. */
  let decisionRequest = 0;
  /** Whether the last decision read failed, so a process refresh retries it. */
  let decisionReadFailed = $state(false);

  /**
   * The decision's id, when the link belongs to the plan on screen.
   *
   * The router reuses this view across plans, and a decision read that started for the previous
   * plan can land before the new plan's own read: the plan id kept beside the id keeps that
   * answer off the new plan's head.
   */
  const openDecisionId = $derived(
    openDecision !== null && openDecision.planId === planId ? openDecision.id : null,
  );

  async function loadOpenDecision(): Promise<void> {
    const request = ++decisionRequest;
    if (!planId) {
      openDecision = null;
      return;
    }
    const currentId = planId;
    try {
      const decisions = await apiClient.getDecisions({ plan: currentId });
      if (request !== decisionRequest || currentId !== planId) {
        return;
      }
      // Only a decision that blocks the plan is the plan's wait; one blocking an epic or a
      // project is not this page's.
      const decision = decisions.find((entry) => entry.blocks === 'plan') ?? null;
      openDecision = decision ? { planId: currentId, id: decision.id } : null;
      decisionReadFailed = false;
    } catch {
      if (request !== decisionRequest) {
        return;
      }
      openDecision = null;
      decisionReadFailed = true;
    }
  }

  $effect(() => {
    // Only the wait itself (and the plan) re-reads the decision: a process reload that leaves the
    // plan waiting keeps the link, so a refresh never adds a read of its own.
    const currentId = planId;
    if (currentId !== '' && waiting) {
      void loadOpenDecision();
    } else {
      decisionRequest += 1;
      openDecision = null;
    }
  });

  /** The queued revision the runtime reported for this plan, absent once it left the queue. */
  let queuedRevision = $state<{ applies: string; taskId: string | null } | null>(null);
  /** Counts the queued-revision events, so a process response that predates one yields to it. */
  let queuedRevisionEvent = 0;

  /**
   * Reads a queued revision as the wire carries it: the point it waits for and, where the point is
   * a task boundary, that task.
   *
   * @param value the revision (`queued_revision` of the resource or of its event)
   * @returns the point and the task, or `null` when nothing waits
   */
  function queuedRevisionOf(value: unknown): { applies: string; taskId: string | null } | null {
    if (!value || typeof value !== 'object') {
      return null;
    }
    const applies = (value as Record<string, unknown>).applies;
    if (!applies || typeof applies !== 'object') {
      return null;
    }
    const key = textOf((applies as Record<string, unknown>).key) ?? '';
    const args = (applies as Record<string, unknown>).args;
    const taskId =
      args && typeof args === 'object' ? textOf((args as Record<string, unknown>).task_id) : null;
    // Only a point the wire names is a point: an unknown key names no task, never a task boundary
    // with an empty id that the line would render as "applies after ".
    if (key === 'chat.applies.on_start') {
      return { applies: 'on_start', taskId: null };
    }
    if (key === 'chat.applies.after_task') {
      return { applies: 'after_task', taskId };
    }
    return { applies: 'unknown', taskId: null };
  }

  /**
   * Applies a `plan.revision_queued` of this plan: the chat revision it queued, and the point it
   * waits for. A payload without a `queued_revision` says the revision left the queue.
   *
   * @param data the event's payload
   */
  function applyQueuedRevision(data: unknown): void {
    if (!data || typeof data !== 'object') {
      return;
    }
    const payload = data as Record<string, unknown>;
    if (payload.plan_id !== planId) {
      return;
    }
    queuedRevisionEvent += 1;
    queuedRevision = queuedRevisionOf(payload.queued_revision);
  }

  /**
   * The plan-level off-track signal: the first off-track task's, found in the tree the process
   * resource already carries (the task-level banner stays in the inspector).
   */
  const planOffTrack = $derived<OffTrackInfo | null>(firstOffTrack(plan?.nodes ?? []));

  /**
   * @param nodes the process tree
   * @returns the first off-track task signal at any depth, or `null`
   */
  function firstOffTrack(nodes: ProcessNode[]): OffTrackInfo | null {
    for (const node of nodes) {
      const task = (node.leaf_tasks ?? []).find((entry) => entry.off_track);
      if (task?.off_track) {
        return task.off_track;
      }
      const found = firstOffTrack(node.sub_nodes ?? []);
      if (found) {
        return found;
      }
    }
    return null;
  }

  /** The width of the object column itself, as its own `ResizeObserver` reports it. */
  let columnWidth = $state(0);
  /** The object column this view draws into; its width decides the side-by-side layout. */
  let columnElement = $state<HTMLElement | null>(null);
  /** Whether the column is at least `720 px` wide, so Process and Changes stand side by side. */
  const wideColumn = $derived(columnWidth >= 720);

  $effect(() => {
    const host = columnElement;
    if (host === null) {
      return;
    }
    const observer = new ResizeObserver((entries) => {
      columnWidth = entries[entries.length - 1].contentRect.width;
    });
    observer.observe(host);
    return () => observer.disconnect();
  });

  /** The intent record the Intent tab needs beyond what its own panel reads, and whether one exists. */
  let intentLoading = $state(false);
  /**
   * Whether the Intent read answered at least once for this plan.
   *
   * The tab's first paint happens before its effect starts the read, so it must not say "not
   * written yet" in that moment; it waits in its loading state until the first answer arrives.
   */
  let intentSettled = $state(false);
  let intentError = $state<string | null>(null);
  let intentHasRecord = $state(false);
  let intentCheck = $state<BuilderCheckRecord | null>(null);
  let intentDeliverables = $state<OutlineDeliverableRecord[]>([]);
  /** The intent read that owns the tab; a superseded answer never writes it. */
  let intentRequest = 0;

  /**
   * Reads the intent record for its builder check and its deliverables, and the outline for the
   * deliverable labels; the panels read their own records again, as the development page's fixture
   * does.
   *
   * The tab reads again whenever it is opened and whenever the runtime reports a change of the
   * records, so a record refine writes while the tab is closed reaches it.
   */
  async function loadIntent(): Promise<void> {
    if (!planId) {
      return;
    }
    const request = ++intentRequest;
    const currentId = planId;
    intentLoading = true;
    intentError = null;
    try {
      const record = await apiClient.getPlanIntent(currentId);
      if (request !== intentRequest || currentId !== planId) {
        return;
      }
      intentHasRecord = record !== null;
      intentCheck = record?.builder_check ?? null;
      if (record === null) {
        // Nothing exists, so no outline read can change the tab; the empty state stands at once.
        intentLoading = false;
        intentSettled = true;
        return;
      }
    } catch (err: unknown) {
      if (request !== intentRequest || currentId !== planId) {
        return;
      }
      // A failed read is not "refine wrote nothing": it says so and offers the read again.
      intentError = errorCodeOf(err);
      intentLoading = false;
      intentSettled = true;
      return;
    }
    try {
      const outline = await apiClient.getPlanOutline(currentId);
      if (request !== intentRequest || currentId !== planId) {
        return;
      }
      intentDeliverables = outline?.deliverables ?? [];
    } catch {
      if (request !== intentRequest || currentId !== planId) {
        return;
      }
      intentDeliverables = [];
    }
    if (request === intentRequest && currentId === planId) {
      intentLoading = false;
      intentSettled = true;
    }
  }

  $effect(() => {
    if (activeTab === 'intent') {
      void loadIntent();
    }
  });

  /**
   * @param state the plan's own state
   * @returns the dot colour that state takes
   */
  function stateTone(state: string): 'neutral' | 'running' | 'attention' | 'danger' | 'success' {
    switch (state) {
      case 'running':
        return 'running';
      case 'waiting_for_you':
        return 'attention';
      case 'failed':
        return 'danger';
      case 'done':
        return 'success';
      default:
        return 'neutral';
    }
  }

  /**
   * @param nodes the process tree
   * @returns the leaf task that runs, and its place among every leaf task, or `null`
   */
  function runningTaskPosition(nodes: ProcessNode[]): { n: number; m: number } | null {
    const tasks: LeafTask[] = [];
    function collect(list: ProcessNode[]): void {
      for (const node of list) {
        tasks.push(...(node.leaf_tasks ?? []));
        collect(node.sub_nodes ?? []);
      }
    }
    collect(nodes);
    const index = tasks.findIndex((task) => task.status === 'running');
    if (index < 0 || tasks.length === 0) {
      return null;
    }
    return { n: index + 1, m: tasks.length };
  }

  /** The duration phrase of the head's state sentence, and the task's place where the tree has one. */
  const stateSentence = $derived(
    [
      ...((plan?.duration_seconds ?? 0) > 0
        ? [
            $t('plan.view.state_for', {
              duration: formatDuration((plan?.duration_seconds ?? 0) * 1000, $currentLocale),
            }),
          ]
        : []),
      ...(() => {
        const position = plan ? runningTaskPosition(plan.nodes) : null;
        return position
          ? [$t('plan.view.task_position', { n: position.n, m: position.m })]
          : [];
      })(),
    ].join(' · '),
  );

  /** The address of the plan's process designer, a head link (`navigation.adoc#levels`). */
  const designerHref = $derived(
    plan ? `/designer?project=${encodeURIComponent(plan.project.project_id)}` : '',
  );

  /**
   * The caller's address: the store's own address for this plan, without the object opened in
   * place — its mode, tab, node, file, chat and lens context included, so a close returns to
   * exactly where the plan was opened from.
   *
   * @returns the caller's address
   */
  function callerAddress(): Address {
    const address: Address = { ...addressOfState($runtimeStore) };
    delete address.open;
    if (ended) address.mode = 'ended';
    return address;
  }

  /** The head link that opens the waiting decision in place beside the plan. */
  const decisionHref = $derived(
    openDecisionId === null
      ? ''
      : (formatAddress({ ...callerAddress(), open: { kind: 'decision', id: openDecisionId } }) ??
        ''),
  );

  /** The head link that opens the ended plan's completion report in place beside it. */
  const completionHref = $derived(
    ended
      ? (formatAddress({
          ...callerAddress(),
          open: { kind: 'completion-report', id: planId },
        }) ?? '')
      : '',
  );

  /** The decision the address opens in place beside the plan, when it names one. */
  const inPlaceDecisionId = $derived(
    $runtimeStore.openObject?.kind === 'decision' ? $runtimeStore.openObject.id : null,
  );

  /**
   * The completion report the address opens in place beside this plan, when it names one: another
   * plan's report is not this page's object.
   */
  const inPlaceReportId = $derived(
    $runtimeStore.openObject?.kind === 'completion-report' &&
      $runtimeStore.openObject.id === planId
      ? $runtimeStore.openObject.id
      : null,
  );

  /** Whether this document opened the in-place object by a push of its own, so Back closes it. */
  let inPlacePushed = $state(false);
  /** Set while a close runs, so two close paths of one update never move the history twice. */
  let closingInPlace = false;

  // A new in-place object opens the next close again.
  $effect(() => {
    if ($runtimeStore.openObject !== null) {
      closingInPlace = false;
    } else {
      // No object is open: a flag from an earlier push must not make a later, loaded object close
      // by Back.
      inPlacePushed = false;
    }
  });

  /**
   * Opens one object in place beside the plan by a push of this document, so the history's back
   * closes it (`navigation.adoc#url`).
   *
   * @param kind the object's kind
   * @param id the object's id
   */
  function openInPlace(kind: 'decision' | 'completion-report', id: string | null): void {
    if (id === null) return;
    runtimeStore.update((s) => ({ ...s, openObject: { kind, id } }));
    // Only an address the writer actually pushed is closed by Back. A second activation of the same
    // link pushes nothing and keeps the flag of the push that opened the entry; one the bar already
    // showed when it was loaded leaves no entry, so its close replaces the address.
    if (writeAddress({ ...callerAddress(), open: { kind, id } }, 'push')) {
      inPlacePushed = true;
    }
  }

  /**
   * Closes the object opened in place: the history's back when this document pushed it, a replacing
   * write to the caller's address otherwise — an address that was loaded with `open=` has nothing
   * to go back to (`navigation.adoc#url`).
   */
  function closeInPlace(): void {
    if (closingInPlace) return;
    closingInPlace = true;
    if (inPlacePushed) {
      inPlacePushed = false;
      window.history.back();
      return;
    }
    runtimeStore.update((s) => ({ ...s, openObject: null }));
    writeAddress(callerAddress(), 'replace');
  }

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

  /**
   * The distinct reasons of the omitted nodes, shown once in the pruned group through the
   * catalogue's words, never as the raw reason.
   */
  let prunedReasons = $derived([
    ...new Set(
      prunedNodes
        .map((node) => node.prune_reason)
        .filter((reason): reason is string => Boolean(reason)),
    ),
  ]);

  let unsubscribeEvents: (() => void) | null = null;
  let unsubscribeActivity: (() => void) | null = null;
  let clockTimer: ReturnType<typeof setInterval> | null = null;
  /**
   * The activity load that owns the panel.
   *
   * A task request can resolve after the operator selected something else; the generation is
   * bumped on every load and on every switch away from a task, so a stale answer never replaces
   * the current feed and never opens a subscription the operator cannot see.
   */
  let activityLoadGeneration = 0;

  onMount(async () => {
    unsubscribeEvents = apiClient.subscribeEvents((event, data) => {
      // The runtime stream's events that change a plan's process (`EventName`).
      // A decision of this plan opening, changing or leaving re-reads the head's link. A decision
      // that opens for this plan is read even before the process reports the wait; one that leaves
      // matters while the plan waits or while its own decision stands beside it.
      if (
        event === 'decision.opened' ||
        event === 'decision.changed' ||
        event === 'decision.answered' ||
        event === 'decision.closed'
      ) {
        const payload = data as { plan?: unknown } | null;
        const namesThisPlan = typeof payload?.plan === 'string' && payload.plan === planId;
        if (namesThisPlan || waiting || openDecision !== null) {
          void loadOpenDecision();
        }
      }
      if (refreshesPlanProcess(event)) {
        void loadPlanProcess(false);
        // A read that failed is retried by the next process refresh, so a transient failure never
        // leaves a waiting plan without its link.
        if (decisionReadFailed && waiting) {
          void loadOpenDecision();
        }
      }
      if (event === 'plan.revision_queued') {
        applyQueuedRevision(data);
      }
      if (event === 'plan.change_captured') {
        applyCapturedChange(data);
      }
      if (refreshesIntentView(event) && activeTab === 'intent') {
        void loadIntent();
      }
    });
    clockTimer = setInterval(() => {
      clock = Date.now();
    }, 30_000);

    await loadPlanProcess(true);
  });

  onDestroy(() => {
    runtimeStore.update((s) => ({ ...s, currentProcessTree: null }));
    if (clockTimer !== null) {
      clearInterval(clockTimer);
      clockTimer = null;
    }
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
    // The feed lives in the Process tab: another tab hides the inspector and ends its stream,
    // while the selection stays so returning to Process reads the same task again.
    if (selectedTaskId && planId && activeTab === 'process') {
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

  let processLoadGeneration = 0;
  async function loadPlanProcess(initial = false) {
    if (!planId) return;
    const currentId = planId;
    const gen = ++processLoadGeneration;
    // A revision event that arrives while this read is in flight is newer than its answer.
    const queuedEventsAtStart = queuedRevisionEvent;
    if (initial) isLoading = true;
    errorCode = null;
    try {
      const res = await apiClient.getPlanProcess(currentId);
      if (gen !== processLoadGeneration) return;
      const previousPlanId = plan?.plan_id ?? null;
      plan = res;
      // The resource carries the queued revision too, so a reload while one waits shows its line.
      if (queuedRevisionEvent === queuedEventsAtStart) {
        queuedRevision = queuedRevisionOf(res.queued_revision);
      }
      runtimeStore.update((s) => ({ ...s, currentProcessTree: res }));
      if (previousPlanId !== null && previousPlanId !== res.plan_id) {
        // Another plan is open: the size the last capture announced belonged to the old one.
        changeFromEvent = null;
        changesReloadToken += 1;
      }

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
      if (gen !== processLoadGeneration) return;
      errorCode = errorCodeOf(err);
    } finally {
      if (gen === processLoadGeneration) {
        isLoading = false;
      }
    }
  }

  /**
   * Applies a `plan.change_captured` of this plan: the size line moves to the announced figures and
   * the tab reads its list again.
   *
   * The payload names the figures, so the line needs no read and the view sums nothing up. A
   * payload without them still raises the token: the list is the answer that matters.
   *
   * @param data the event's payload
   */
  function applyCapturedChange(data: unknown): void {
    if (!data || typeof data !== 'object') {
      return;
    }
    const payload = data as Record<string, unknown>;
    if (payload.plan_id !== planId) {
      return;
    }
    const figure = (value: unknown): number | null =>
      typeof value === 'number' && Number.isInteger(value) && value >= 0 ? value : null;
    const files = figure(payload.files_total);
    const insertions = figure(payload.insertions_total);
    const deletions = figure(payload.deletions_total);
    const unplanned = figure(payload.unplanned_total);
    if (files !== null && insertions !== null && deletions !== null && unplanned !== null) {
      changeFromEvent = { files, insertions, deletions, unplanned };
    }
    changesReloadToken += 1;
  }

  // The address may name a tab this view does not draw — Changes before the plan's first capture,
  // or a tab of another object. The view then opens Process and replaces the address, so the
  // address never names a panel that is not there (`plan-view-interaction.adoc#changes`).
  $effect(() => {
    if (plan === null) {
      return;
    }
    if (activeTab === 'changes' && changeSize === null) {
      selectObjectTab('process');
      return;
    }
    const named = $runtimeStore.objectTab;
    // The plan tabs are the router's own set, so a tab added there is never rewritten here.
    if (typeof named === 'string' && !(PLAN_TABS as ReadonlyArray<string>).includes(named)) {
      selectObjectTab('process');
    }
  });

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
      timestamp: dateOnRead(textOf(source.timestamp) ?? textOf(source.time) ?? '', new Date()),
      kind: source.kind,
      tool_name: textOf(source.tool_name),
      tool_target: textOf(source.tool_target),
      tool_failed: source.tool_failed === true,
      content: textOf(source.content) ?? textOf(source.text),
      size: textOf(source.size),
      files: stringsOf(source.files),
      questions: stringsOf(source.questions),
      launch_model: textOf(source.launch_model),
      served_model: textOf(source.served_model),
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
      const readAt = new Date();
      taskActivity = {
        ...loaded,
        entries: loaded.entries.map((entry) => ({
          ...entry,
          timestamp: dateOnRead(entry.timestamp, readAt),
        })),
      };
      unsubscribeActivity = apiClient.subscribeTaskActivity(pId, tId, (event, data) => {
        const entry = toActivityEntry(event, data);
        if (taskActivity && entry) {
          // The row is dated by its arrival, so the clock the headers are measured at moves with
          // it: a row that arrives just after midnight opens the new day as "today".
          clock = Date.now();
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

  /** Opens the path to the current position and selects what runs there; offered with a plan. */
  function jumpToCurrentPosition(current: ProcessTree) {
    const path = current.current_position_path;
    const nextExp = { ...expandedNodes };
    for (const p of path) {
      nextExp[p] = true;
    }
    expandedNodes = nextExp;

    const running = findRunningTask(current.nodes);
    if (running) {
      selection = { kind: 'task', taskId: running.task_id };
    } else {
      const runningNode = findRunningNode(current.nodes);
      if (runningNode) {
        selection = { kind: 'node', nodeId: runningNode.node_id };
      }
    }
    // The app renders in the browser only (`ssr = false` in the root layout).
    const el = document.querySelector(
      '.node-item--running, .node-item--waiting_for_operator, .node-item--waiting_for_you, .node-item--waiting',
    );
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }

  function formatCoverage(tokens: ProcessTokens): string {
    return $t('common.tokens.coverage', {
      unmeasured: Math.max(0, tokens.tasks_total - tokens.tasks_measured),
      total: tokens.tasks_total,
      reason: 'none',
    });
  }

  /**
   * @param tokens the plan's tokens, which the process resource always carries
   * @param loc the interface language
   * @returns the tokens line
   */
  function formatTokensLine(tokens: ProcessTokens, loc: Locale): string {
    if (tokens.none_yet) {
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
   * @returns the moment to the minute, in the interface language
   */
  function formatEndedAt(iso: string): string {
    return formatTime(iso, 'short', $currentLocale);
  }

  /**
   * The wall clock of an activity entry.
   *
   * The contract allows either an instant or the clock the runtime has already formatted
   * (`pm-api` `ActivityEntry`): an instant is rendered by the one format, a clock is shown as it
   * arrives.
   *
   * @param timestamp the entry's time as the runtime sends it
   * @returns the clock, `14:02:11`
   */
  function entryClock(timestamp: string): string {
    return formatTime(timestamp, 'clock', $currentLocale) || timestamp;
  }

  /**
   * Whether an activity entry draws a row.
   *
   * A `link` entry without a target carries nothing to show: it names a link the worker did not
   * give (`plan-view-interaction.adoc#node-narrative`). Every other kind keeps its row.
   *
   * @param entry the activity entry
   * @returns whether the feed draws it
   */
  function showsActivityRow(entry: ActivityEntry): boolean {
    if (entry.kind === 'link') {
      // The target the row renders is the cleaned text; a target that cleans away leaves no row.
      return stripControlSequences(entry.tool_target).trim() !== '';
    }
    return true;
  }

  /** The entries the feed draws: the link rows without a target are left out. */
  const activityEntries = $derived((taskActivity?.entries ?? []).filter(showsActivityRow));

  /**
   * @param kind the activity kind
   * @returns the delivered icon of that kind
   */
  function activityKindIcon(kind: string): string {
    switch (kind) {
      case 'tool':
      case 'tool_call':
        return 'kind.harness';
      case 'changed':
      case 'file_changed':
        return 'act.edit';
      case 'link':
        return 'act.external';
      case 'question':
        return 'kind.decision';
      case 'answer_ack':
        return 'state.check';
      case 'gap':
        return 'state.clock';
      case 'model_differs':
        return 'state.alert';
      case 'text':
      default:
        return 'kind.document';
    }
  }

  /**
   * The day a row opens, where the row carries a date or a clock.
   *
   * A row the runtime sends with a clock only was dated when it was read (`dateOnRead`), so its day
   * is fixed and the header follows the live clock: a page left open over midnight names the same
   * day `yesterday`, and the next dated row of one day moves under a single header.
   *
   * @param entries the entries of the task's activity
   * @param index the entry's position
   * @returns the day to print above the row, or an empty string
   */
  function dayLabel(entries: ActivityEntry[], index: number): string {
    const timestamp = entries[index]?.timestamp;
    const now = new Date(clock);
    const day = formatDay(timestamp, $currentLocale, now);
    if (!day) return '';
    const own = dayOf(timestamp, now);
    if (own === null) return day;
    for (let cursor = index - 1; cursor >= 0; cursor -= 1) {
      const previous = dayOf(entries[cursor]?.timestamp, now);
      if (previous === null) continue;
      return previous.getTime() === own.getTime() ? '' : day;
    }
    return day;
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

<div
  class="plan-view-layout"
  class:plan-view-layout--with-object={inPlaceDecisionId !== null || inPlaceReportId !== null}
  data-testid="plan-view-layout"
>
  {#if planProjectId}
    <WorkListColumn
      projectId={planProjectId}
      currentPlanId={planId}
      parentEpicId={plan?.plan_id === planId ? (plan?.epic?.epic_id ?? null) : null}
    />
  {/if}
  <div class="plan-view" data-testid="plan-view" bind:this={columnElement}>
  {#if $narrowLayout && !isLoading && upParent?.kind && upParent.address}
    <div class="plan-view__up" data-testid="plan-view-up">
      <UpLink kind={upParent.kind} title={upParent.title} address={upParent.address} />
    </div>
  {/if}
  {#if isLoading}
    <div class="plan-view__loading" data-testid="plan-loading">
      <Skeleton shape="card" rows={3} label={$t('plan.view.loading')} />
    </div>
  {:else if errorCode !== null && !plan}
    <div class="plan-view__error" data-testid="plan-error">
      <ErrorState
        code={errorCode}
        actionLabel={$t('common.action.retry')}
        onAct={() => loadPlanProcess(true)}
        titleLevel={2}
        announce={plan !== null}
      />
    </div>
  {:else if plan}
    {#if errorCode !== null}
      <div class="plan-view__error" data-testid="plan-error">
        <ErrorState
          code={errorCode}
          actionLabel={$t('common.action.retry')}
          onAct={() => loadPlanProcess(false)}
          titleLevel={2}
        />
      </div>
    {/if}
    <ObjectPage>
      <span slot="kind" data-testid="plan-kind"><KindMark kind="plan" /></span>
      <span slot="title" data-testid="plan-title">{stripControlSequences(plan.title)}</span>
      <span slot="id" data-testid="plan-id-chip"><Chip value={plan.plan_id} /></span>

      <span slot="state" class="plan-state-line" data-testid="plan-state-line">
        {#if plan.pull_request_number}
          <span data-testid="plan-pr-chip">
            <PrChip number={plan.pull_request_number} href={plan.pull_request_url ?? undefined} />
          </span>
        {/if}
        <span data-testid="plan-phase"><PhaseLabel phase={plan.current_phase} /></span>
        <span data-testid="plan-state">
          <StatusLabel
            tone={stateTone(plan.phase_state)}
            label={displayState('plan', plan.phase_state, $t)}
          />
        </span>
        {#if stateSentence !== ''}
          <span class="plan-state__sep" aria-hidden="true">·</span>
          <span class="plan-state__sentence" data-testid="plan-state-sentence"
            >{stateSentence}</span
          >
        {/if}
      </span>

      <div slot="primary" class="plan-acts">
        <!-- The plan's own actions, settings, holds, and chats -->
        <WorkControlPanel
          subjectId={plan.plan_id}
          subjectKind="plan"
          projectId={plan.project.project_id}
          ended={Boolean(ended)}
          pullRequestNumber={plan.pull_request_number ?? null}
          onChanged={() => void loadPlanProcess(false)}
        />
      </div>

      <div slot="secondary" class="plan-head-links">
        <Link
          href={designerHref}
          label={$t('designer.plan.open')}
          advisory={$t('designer.plan.open.title')}
          testid="design-process-link"
        />
        {#if ended}
          <Link
            href={completionHref}
            label={$t('report.action.details')}
            advisory={$t('report.action.details.title')}
            testid="completion-report-link"
            onactivate={() => openInPlace('completion-report', planId)}
          />
        {/if}
        {#if waiting && openDecisionId}
          <Link
            href={decisionHref}
            label={$t('control.plan.decision')}
            advisory={$t('control.plan.decision.title')}
            testid="plan-decision-link"
            onactivate={() => openInPlace('decision', openDecisionId)}
          />
        {/if}
      </div>

      <div slot="attention" class="plan-attention">
        {#if waiting && openDecisionId}
          <div data-testid="plan-attention-strip">
            <AttentionStrip message={$t('plan.view.waits_for_decision')} />
          </div>
        {/if}

        {#if planOffTrack}
          <div class="off-track-banner" data-testid="plan-off-track" role="alert">
            <span class="warning-icon"><Icon name="state.alert" size={16} /></span>
            <span class="off-track-msg">
              {$t('plan.view.off_track', {
                signal: displayState('signal', planOffTrack.signal, $t),
              })}
            </span>
          </div>
        {/if}

        {#if queuedRevision}
          {#if queuedRevision.applies === 'unknown'}
            <p class="plan-revision mono" data-testid="plan-revision-queued">
              {$t('control.chat.revision_queued_unscheduled')}
            </p>
          {:else}
            <p class="plan-revision mono" data-testid="plan-revision-queued">
              {$t('control.chat.revision_queued', {
                applies: queuedRevision.applies,
                plan: plan.plan_id,
                task_id: queuedRevision.taskId ?? '',
              })}
            </p>
          {/if}
        {/if}

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
          </div>
        {/if}
      </div>

      <div slot="tabs" class="plan-tabstrip">
        <!-- Tokens So Far -->
        <div class="plan-tokens mono" data-testid="plan-tokens">
          <span class="tokens-label">{$t('plan.view.tokens')}</span>
          <span class="tokens-value">{formatTokensLine(plan.tokens_so_far, $currentLocale)}</span>
          {#if !plan.tokens_so_far.none_yet}
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

        {#if showTokenBreakdown}
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
        <!-- The size line of the change: it opens the Changes tab, and is absent before the first capture -->
        {#if changeSize}
          <div class="plan-change-size" data-testid="plan-change-size-row">
            <span class="tokens-label">{$t('plan.change.so_far')}</span>
            <button
              type="button"
              class="change-size-link mono"
              data-testid="plan-change-size"
              onclick={() => {
                if (activeTab !== 'changes') {
                  selectObjectTab('changes');
                }
              }}
            >
              {changeText(changeSize)}
            </button>
          </div>
        {/if}

        <Tabs tabs={tabItems} active={activeTab} label={$t('plan.tabs')} onSelect={selectTab} />
      </div>

    <div
      class="plan-panel-host"
      data-testid="plan-panel-host"
      data-layout={wideColumn ? 'side' : 'stacked'}
    >

    {#if activeTab === 'changes' && changeSize}
      <div
        class="plan-changes-panel"
        id="plan-panel-changes"
        role="tabpanel"
        aria-label={$t('plan.tab.changes')}
        data-testid="plan-changes-panel"
      >
        <PlanChanges
          planId={plan.plan_id}
          change={changeSize}
          selectedFile={$runtimeStore.changeFile ?? null}
          onSelectFile={selectPlanChangeFile}
          reloadToken={changesReloadToken}
          wide={wideColumn}
        />
      </div>
    {:else if activeTab === 'intent'}
      <div
        class="plan-intent-panel"
        id="plan-panel-intent"
        role="tabpanel"
        aria-label={$t('intent.tab.title')}
        data-testid="plan-intent-panel-tab"
      >
        {#if intentError !== null}
          <ErrorState
            code={intentError}
            actionLabel={$t('common.action.retry')}
            onAct={() => void loadIntent()}
            titleLevel={2}
          />
        {:else if !intentSettled || (intentLoading && !intentHasRecord)}
          <div class="plan-intent-panel__loading" data-testid="plan-intent-loading">
            <Spinner size="1.5rem" label={$t('intent.tab.title')} />
          </div>
        {:else if !intentHasRecord}
          <p class="text-muted" data-testid="plan-intent-none">{$t('intent.plan.none')}</p>
        {:else}
          <PlanIntentPanel
            planId={plan.plan_id}
            deliverables={intentDeliverables}
            deliverableHref={(number) => `#deliverable-${number}`}
          />
          <BuilderCheckList check={intentCheck} />
          <OutlinePanel planId={plan.plan_id} />
        {/if}
      </div>
    {:else if activeTab === 'process'}
      <!-- Two-Panel Main Layout -->
      <div
        class="plan-content"
        id="plan-panel-process"
        role="tabpanel"
        aria-label={$t('plan.tab.process')}
      >
      <!-- Left: Process Tree -->
      <section
        class="plan-tree-panel"
        aria-label={$t('plan.view.tree_label')}
        data-testid="process-tree"
      >
        <div class="plan-tree-toolbar">
          <IconButton
            icon="nav.running"
            size="sm"
            label={$t('plan.path.jump')}
            advisory={$t('plan.path.jump.title')}
            onclick={() => jumpToCurrentPosition(plan!)}
            data-testid="jump-btn"
          />
        </div>
        {#snippet processNode(node: ProcessNode, level: 'root' | 'sub')}
          <div
            class="node-item {level === 'root'
              ? 'node-item--level-1'
              : 'node-item--sub'} node-item--{node.state}"
            data-testid="node-{node.node_id}"
          >
            <div class="node-row {isNodeSelected(node.node_id) ? 'node-row--selected' : ''}">
              <!-- Only a node with something to open has a toggle; a leaf keeps the place free. -->
              {#if hasChildren(node)}
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
                  aria-expanded={Boolean(expandedNodes[node.node_id])}
                  data-testid="toggle-node-{node.node_id}"
                >
                  <span class="toggle-icon" aria-hidden="true"
                    >{expandedNodes[node.node_id] ? '▾' : '▸'}</span
                  >
                </button>
              {:else}
                <span class="toggle-spacer" aria-hidden="true"></span>
              {/if}
              <span
                class="node-glyph"
                aria-hidden="true"
                data-testid="node-state-icon-{node.node_id}"
                ><Icon name={getNodeStateIcon(node.state)} size={16} /></span
              >

              <button
                type="button"
                class="node-info node-select"
                onclick={() => selectNode(node.node_id)}
                aria-pressed={isNodeSelected(node.node_id)}
                aria-current={isNodeSelected(node.node_id) ? 'true' : undefined}
                data-advisory={$t('plan.node.select.title')}
                data-testid="select-node-{node.node_id}"
              >
                <span class="node-kind-icon" data-testid="node-kind-{node.node_id}"
                  ><WorkflowIcon
                    id={getNodeKindIcon(node)}
                    size={16}
                    label={displayState('node_kind', node.kind, $t)}
                  /></span
                >
                <span class="node-title font-semibold">{getNodeDisplayName(node, $t)}</span>
                <span class="node-state-text">{getNodeStateText(node, $t)}</span>
                {#if node.duration_seconds > 0}
                  <span class="node-duration"
                    >{formatDuration(node.duration_seconds * 1000, $currentLocale)}</span
                  >
                {/if}
                <span class="sr-only">{$t('plan.node.select')}</span>
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
                        <span class="task-attempt"
                          >{$t('plan.node.attempt', { attempt: task.attempt })}</span
                        >
                        <span class="task-status badge badge--state badge--state-{task.status}"
                          >{displayState('task', task.status, $t)}</span
                        >
                        {#if task.off_track}
                          <span class="badge badge--warning" data-testid="task-off-track-badge">
                            {$t('plan.view.off_track', {
                              signal: displayState('signal', task.off_track.signal, $t),
                            })}
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
                {#if prunedReasons.length > 0}
                  ({prunedReasons.map((reason) => displayPruneReason(reason, $t)).join(' · ')})
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
                  <span class="task-meta" data-testid="activity-model"
                    >{taskActivity.harness} {taskActivity.model}</span
                  >
                  <span class="task-meta" data-testid="activity-attempt"
                    >{$t('plan.node.attempt', { attempt: taskActivity.attempt })}</span
                  >
                </div>
                <div class="task-metrics mono">
                  <span>{formatDuration(taskActivity.duration_seconds * 1000, $currentLocale)}</span
                  >
                  {#if taskActivity.tokens_so_far > 0}
                    <span
                      >· {formatTokens(taskActivity.tokens_so_far, false, $currentLocale)}
                      {$t('plan.view.tokens')}</span
                    >
                  {/if}
                </div>
              </div>

              <!-- Off-track alert banner -->
              {#if taskActivity.off_track}
                <div class="off-track-banner" data-testid="off-track-alert" role="alert">
                  <span class="warning-icon"><Icon name="state.alert" size={16} /></span>
                  <span class="off-track-msg">
                    {$t('plan.activity.off_track', {
                      signal: displayState('signal', taskActivity.off_track.signal, $t),
                      value: taskActivity.off_track.value ?? '?',
                      threshold: taskActivity.off_track.threshold ?? '?',
                      action: displayState('action', taskActivity.off_track.action, $t),
                    })}
                  </span>
                </div>
              {/if}

              <!-- Activity Entries -->
              <div class="activity-entries">
                {#if activityEntries.length === 0}
                  <div class="activity-empty text-muted">{$t('plan.activity.empty')}</div>
                {:else}
                  {#each activityEntries as entry, index}
                    {@const label = dayLabel(activityEntries, index)}
                    {#if label}
                      <div class="activity-day mono" data-testid="activity-day">{label}</div>
                    {/if}
                    <div
                      class="activity-entry activity-entry--{entry.kind}"
                      data-testid="activity-entry"
                    >
                      <span class="entry-time mono">{entryClock(entry.timestamp)}</span>
                      <span
                        class="entry-kind"
                        data-testid="activity-kind"
                        role="img"
                        aria-label={displayState('activity', entry.kind, $t)}
                      >
                        <Icon name={activityKindIcon(entry.kind)} size={16} />
                      </span>
                      <div class="entry-body">
                        {#if entry.kind === 'tool' || entry.kind === 'tool_call'}
                          <div class="tool-call mono">
                            <span class="tool-name">{entry.tool_name}</span>
                            <span class="tool-target"
                              >{stripControlSequences(entry.tool_target)}</span
                            >
                            {#if entry.tool_failed}
                              <span class="badge badge--danger" data-testid="tool-failed"
                                >{entry.size
                                  ? $t('plan.activity.kind.tool_failed', { size: entry.size })
                                  : displayState('task', 'failed', $t)}</span
                              >
                            {/if}
                          </div>
                        {:else if entry.kind === 'changed' || entry.kind === 'file_changed'}
                          <div class="file-changed mono" data-model-text>
                            {stripControlSequences(
                              entry.content || (entry.files && entry.files[0]) || '',
                            )}
                          </div>
                        {:else if entry.kind === 'link'}
                          <div class="entry-content mono" data-model-text>
                            {stripControlSequences(entry.tool_target)}
                          </div>
                        {:else if entry.kind === 'gap'}
                          <div class="activity-gap text-muted italic" data-model-text>
                            {stripControlSequences(entry.content || '')}
                          </div>
                        {:else if entry.kind === 'model_differs'}
                          <div
                            class="entry-content model-differs"
                            data-testid="model-differs"
                            data-model-text
                          >
                            {$t('plan.activity.model_differs', {
                              served: stripControlSequences(entry.served_model ?? '?'),
                              launched: stripControlSequences(entry.launch_model ?? '?'),
                            })}
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
    </ObjectPage>
  {/if}
</div>
  {#if inPlaceDecisionId !== null || inPlaceReportId !== null}
    <aside class="plan-view-object" data-testid="plan-in-place">
      <header class="plan-view-object__head">
        <Button variant="ghost" size="sm" onclick={closeInPlace} data-testid="plan-in-place-close">
          {$t('common.action.close')}
        </Button>
      </header>
      <div class="plan-view-object__body">
        {#if inPlaceDecisionId !== null}
          {#key inPlaceDecisionId}
            <DecisionView
              inPlace={{ id: inPlaceDecisionId, planId, onClose: closeInPlace }}
            />
          {/key}
        {:else if inPlaceReportId !== null}
          {#key inPlaceReportId}
            <CompletionReportView planId={inPlaceReportId} titleLevel={2} />
          {/key}
        {/if}
      </div>
    </aside>
  {/if}
</div>

<style>
  .plan-view-layout {
    display: flex;
    flex-direction: row;
    align-items: stretch;
    inline-size: 100%;
    min-inline-size: 0;
    min-height: 100%;
  }

  .plan-view {
    flex: 1 1 auto;
    min-inline-size: 0;
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

  /* The first visible column's head carries Up where the work list column is hidden
     (`navigation.adoc#up`). */
  .plan-view__up {
    padding: var(--space-3) var(--space-6) 0;
  }

  .plan-tokens {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    display: flex;
    gap: var(--space-2);
  }

  .plan-change-size {
    display: flex;
    align-items: baseline;
    gap: var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .change-size-link {
    background: none;
    border: none;
    padding: 0;
    color: var(--color-primary);
    cursor: pointer;
    font-size: var(--text-xs);
  }

  .change-size-link:hover {
    text-decoration: underline;
  }

  .change-size-link:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }

  .plan-changes-panel {
    flex: 1 1 auto;
    min-block-size: 0;
    overflow: auto;
    padding: var(--space-4) var(--space-6);
  }

  .model-differs {
    color: var(--color-text-secondary);
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
    display: inline-flex;
    justify-content: center;
    width: 1.2rem;
  }

  .toggle-icon {
    font-size: 0.8rem;
    color: var(--color-text-muted);
  }

  /* A leaf has no toggle; the spacer keeps its state icon in line with its siblings'. */
  .toggle-spacer {
    display: inline-block;
    width: 0.8rem;
  }

  /* A step the process did not take stays readable, muted, with its state word kept. */
  .node-item--not_taken > .node-row {
    color: var(--color-text-muted);
  }

  .node-item--not_taken > .node-row .node-state-text {
    color: var(--color-text-muted);
  }

  .node-info {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex: 1;
    font-size: var(--text-sm);
  }

  .node-kind-icon {
    display: inline-flex;
    align-items: center;
    color: var(--color-text-muted);
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

  .activity-day {
    padding: var(--space-1) 0;
    color: var(--color-text-muted);
    font-size: var(--text-xs);
    font-weight: 600;
  }

  .entry-time {
    color: var(--color-text-muted);
    flex-shrink: 0;
  }

  .entry-kind {
    display: inline-flex;
    align-items: center;
    flex-shrink: 0;
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
  .plan-state-line {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
  }

  .plan-state__sep {
    color: var(--color-text-muted);
  }

  .plan-state__sentence {
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .plan-acts {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .plan-head-links {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
  }

  .plan-tabstrip {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    min-inline-size: 0;
  }

  .plan-revision {
    margin: 0;
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .plan-panel-host {
    display: flex;
    flex-direction: column;
    flex: 1 1 auto;
    min-inline-size: 0;
    min-block-size: 0;
  }

  /* The template's body carries the panels, so the column's height must reach them: each pane
     keeps its own scroll while they stand side by side (`plan-view-interaction.adoc#layout`). */
  .plan-view :global(.object-page) {
    flex: 1 1 auto;
    min-block-size: 0;
  }

  .plan-view :global(.object-page__body) {
    display: flex;
    flex-direction: column;
    flex: 1 1 auto;
    min-block-size: 0;
  }

  .plan-panel-host[data-layout='side'] .plan-content {
    flex: 1 1 auto;
    min-block-size: 0;
  }

  .plan-intent-panel {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    min-inline-size: 0;
  }

  .plan-intent-panel__loading {
    display: flex;
    justify-content: center;
    padding: var(--space-6);
  }

  .plan-tree-toolbar {
    display: flex;
    justify-content: flex-end;
    padding-block-end: var(--space-2);
  }

  /* Process: side by side while the object column is at least 720 px wide, stacked below it
     (`plan-view-interaction.adoc#layout`). The width is the column's own, never the viewport's. */
  .plan-panel-host[data-layout='side'] .plan-content {
    flex-direction: row;
  }

  .plan-panel-host[data-layout='stacked'] .plan-content {
    flex-direction: column;
  }

  .plan-panel-host[data-layout='side'] .plan-tree-panel {
    flex: 0 0 320px;
    min-width: 280px;
    max-width: 360px;
  }

  .plan-panel-host[data-layout='side'] .plan-detail-panel {
    flex: 1 1 auto;
    min-width: 0;
  }

  .plan-panel-host[data-layout='stacked'] .plan-tree-panel,
  .plan-panel-host[data-layout='stacked'] .plan-detail-panel {
    min-width: 0;
    border-right: none;
  }

  .plan-panel-host[data-layout='stacked'] .plan-tree-panel {
    border-bottom: 1px solid var(--color-border);
  }

  .plan-attention .off-track-banner {
    margin: 0;
  }

  .plan-attention {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    min-inline-size: 0;
  }

  /* The object opened in place beside the plan: its own column, its own scroll. */
  .plan-view-object {
    flex: 0 0 auto;
    inline-size: min(560px, 45vw);
    min-inline-size: 0;
    max-block-size: 100%;
    overflow-y: auto;
    overflow-x: hidden;
    border-inline-start: 1px solid var(--color-border);
    background-color: var(--color-bg-app);
  }

  .plan-view-object__head {
    display: flex;
    justify-content: flex-end;
    padding: var(--space-2) var(--space-3) 0;
  }

  .plan-view-object__body {
    min-inline-size: 0;
    padding: var(--space-2) var(--space-3) var(--space-4);
  }

  /* A phone has room for one column: the object takes the space the plan leaves (`navigation.adoc#window`). */
  @media (max-width: 767px) {
    .plan-view-object {
      inline-size: 100%;
    }

    .plan-view-layout--with-object .plan-view {
      display: none;
    }
  }
</style>
