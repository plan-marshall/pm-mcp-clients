<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { get } from 'svelte/store';
  import { t, currentLocale } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import FilterInput from '../components/FilterInput.svelte';
  import WorkControlPanel from '../components/WorkControlPanel.svelte';
  import WorkflowIcon from '../components/WorkflowIcon.svelte';
  import { phaseIconId } from '../icons/workflowIcons';
  import { buildHaystack, matchesHaystack, tokenize } from '../utils/filterText';
  import { displayState, displayUnit, hasUnitName } from '../display';
  import { formatDuration, formatTime } from '../display/format';
  import {
    apiClient,
    type ProjectSummary,
    type ProjectDetailResponse,
    type ActivePlanSummary,
    type ActiveEpicSummary,
    type AlongsideLanding,
    type EndedWorkPage,
  } from '../api/client';
  import {
    runtimeStore,
    openPlanView,
    openEpicView,
    openEndedPlanView,
    openCompletionReport,
    rememberedStartRoute,
    setRememberedStartRoute,
  } from '../stores/runtime';
  import { openStartWork, openStartEntry } from '../stores/start';
  import { openGitSource } from '../stores/gitSource';

  let projects: ProjectSummary[] = [];
  let selectedProjectId: string | null = null;
  let detail: ProjectDetailResponse | null = null;
  let viewMode: 'plan' | 'epic' = 'plan';

  let showEnded = false;
  let endedPage: EndedWorkPage | null = null;
  let currentEndedPage = 1;
  let startPageIsOverview = false;
  let detailRequestGeneration = 0;
  let endedRequestGeneration = 0;

  let isLoadingProjects = false;
  let isLoadingDetail = false;
  let isLoadingEnded = false;
  let errorMessage: string | null = null;

  let unsubscribeEvents: (() => void) | null = null;
  /** The plan or epic whose actions the operator opened from its row. */
  let controlSubject: { kind: 'plan' | 'epic'; id: string } | null = null;

  /** The filter of the running-work lists; the project tiles are navigation, not list items. */
  let runningQuery = '';
  /** The filter of the ended-work page. */
  let endedQuery = '';

  /**
   * @param plan a running plan
   * @returns the identifier the list and the filter use
   */
  function planKeyOf(plan: ActivePlanSummary): string {
    return plan.plan_id || (plan as ActivePlanSummary & { planId?: string }).planId || '';
  }

  /**
   * @param epic a running epic
   * @returns the identifier the list and the filter use
   */
  function epicKeyOf(epic: ActiveEpicSummary): string {
    return epic.epic_id || (epic as ActiveEpicSummary & { epicId?: string }).epicId || '';
  }

  /**
   * @param item an ended-work row
   * @returns the identifier the table and the filter use
   */
  function endedKeyOf(item: EndedWorkPage['items'][number]): string {
    return item.id ?? item.item_id ?? item.title;
  }

  /**
   * The fields a running plan shows, for the shared filter.
   *
   * @param plan the running plan
   * @returns every visible value
   */
  function planFilterFields(plan: ActivePlanSummary): Array<string | null | undefined> {
    return [
      plan.plan_id,
      plan.title,
      selectedProject?.display_name,
      selectedProject?.project_id,
      plan.epic_id,
      plan.epic_title,
      getPlanPhase(plan),
      getPlanState(plan),
      plan.off_track_signal,
    ];
  }

  /**
   * The fields a running epic shows, for the shared filter.
   *
   * @param epic the running epic
   * @returns every visible value
   */
  function epicFilterFields(epic: ActiveEpicSummary): Array<string | null | undefined> {
    return [
      epic.epic_id,
      epic.title,
      selectedProject?.display_name,
      selectedProject?.project_id,
      epic.needs_operator_kind,
      ...(epic.plans ?? []).flatMap((plan) => [plan.title, plan.plan_id]),
    ];
  }

  /**
   * The fields an ended-work row shows, for the shared filter.
   *
   * @param item the ended-work row
   * @returns every visible value
   */
  function endedFilterFields(item: EndedWorkPage['items'][number]): Array<string | null | undefined> {
    return [
      item.id,
      item.item_id,
      item.kind,
      item.title,
      item.outcome,
      item.terminal_reason,
      item.reason,
      item.ended_at,
      formatRelative(item.ended_at),
    ];
  }

  /**
   * Builds one normalized haystack per item, so filtering does not re-normalize on each keystroke.
   *
   * @param items the list
   * @param keyOf the identifier of an item
   * @param fieldsOf the values the item shows
   * @returns the haystacks by identifier
   */
  function indexItems<T>(
    items: T[],
    keyOf: (item: T) => string,
    fieldsOf: (item: T) => Array<string | null | undefined>,
  ): Map<string, string> {
    const index = new Map<string, string>();
    for (const item of items) index.set(keyOf(item), buildHaystack(fieldsOf(item)));
    return index;
  }

  /**
   * @param items the list
   * @param query the operator's query
   * @param keyOf the identifier of an item
   * @param index the item haystacks
   * @returns the items that match, in the served order
   */
  function applyFilter<T>(
    items: T[],
    query: string,
    keyOf: (item: T) => string,
    index: Map<string, string>,
  ): T[] {
    const tokens = tokenize(query);
    if (tokens.length === 0) return items;
    return items.filter((item) => matchesHaystack(tokens, index.get(keyOf(item)) ?? ''));
  }

  $: planHaystacks = indexItems(detail?.plans ?? [], planKeyOf, planFilterFields);
  $: epicHaystacks = indexItems(detail?.epics ?? [], epicKeyOf, epicFilterFields);
  $: endedHaystacks = indexItems(endedPage?.items ?? [], endedKeyOf, endedFilterFields);
  $: filteredPlans = applyFilter(detail?.plans ?? [], runningQuery, planKeyOf, planHaystacks);
  $: filteredEpics = applyFilter(detail?.epics ?? [], runningQuery, epicKeyOf, epicHaystacks);
  $: filteredEndedItems = applyFilter(
    endedPage?.items ?? [],
    endedQuery,
    endedKeyOf,
    endedHaystacks,
  );

  $: selectedProject = projects.find((p) => p.project_id === selectedProjectId) || null;

  onMount(async () => {
    if (typeof window !== 'undefined') {
      startPageIsOverview = rememberedStartRoute() === 'overview';
      const savedMode = localStorage.getItem('pm_overview_view_mode');
      if (savedMode === 'plan' || savedMode === 'epic') {
        viewMode = savedMode;
      }
      // A figure of the system view asked for the plans or the epics; consume it once.
      const focus = get(runtimeStore).overviewFocus;
      if (focus === 'plans') {
        viewMode = 'plan';
        runtimeStore.update((state) => ({ ...state, overviewFocus: null }));
      } else if (focus === 'epics') {
        viewMode = 'epic';
        runtimeStore.update((state) => ({ ...state, overviewFocus: null }));
      }
      const savedProj = localStorage.getItem('pm_overview_selected_project');
      if (savedProj) {
        selectedProjectId = savedProj;
      }
      // A view that opened the plan or the project came back to Running Work returns to the
      // section it left: Ended Work, on the page it showed.
      showEnded = localStorage.getItem('pm_overview_show_ended') === '1';
      const savedEndedPage = Number.parseInt(
        localStorage.getItem('pm_overview_ended_page') ?? '',
        10,
      );
      if (Number.isInteger(savedEndedPage) && savedEndedPage > 0) {
        currentEndedPage = savedEndedPage;
      }
      // The store names the project another view opened one step ago; it wins over the one this
      // browser last selected.
      const fromStore = get(runtimeStore).selectedProjectId;
      if (fromStore) {
        selectedProjectId = fromStore;
        // The store names a project another view opened one step ago. Consume it once, so a
        // later visit falls back to the browser's own selection rather than snapping back.
        runtimeStore.update((s) => ({ ...s, selectedProjectId: null }));
      }
    }

    unsubscribeEvents = apiClient.subscribeEvents((event) => {
      if (
        event === 'project.work_started' ||
        event === 'project.work_ended' ||
        event === 'project.changed' ||
        event === 'phase.changed' ||
        event === 'activity.changed' ||
        event === 'needs_operator.changed' ||
        event === 'task.off_track'
      ) {
        void refreshAll();
      }
    });

    await loadProjects();
    if (showEnded && selectedProjectId) {
      await loadEndedWork(selectedProjectId, currentEndedPage);
    }
  });

  onDestroy(() => {
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  async function loadProjects() {
    isLoadingProjects = true;
    errorMessage = null;
    try {
      const res = await apiClient.getProjects();
      projects = res.projects || [];
      if (projects.length > 0) {
        if (!selectedProjectId || !projects.some((p) => p.project_id === selectedProjectId)) {
          selectedProjectId = projects[0].project_id;
        }
        await loadProjectDetail(selectedProjectId);
      } else {
        selectedProjectId = null;
        detail = null;
      }
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('overview.projects.load_failed');
    } finally {
      isLoadingProjects = false;
    }
  }

  async function loadProjectDetail(projectId: string) {
    const requestGeneration = ++detailRequestGeneration;
    isLoadingDetail = true;
    try {
      const res = await apiClient.getProjectDetail(projectId);
      if (projectId === selectedProjectId && requestGeneration === detailRequestGeneration) {
        detail = res;
      }
    } catch (err: unknown) {
      if (projectId === selectedProjectId && requestGeneration === detailRequestGeneration) {
        detail = null;
      }
    } finally {
      if (requestGeneration === detailRequestGeneration) {
        isLoadingDetail = false;
      }
    }
  }

  async function refreshAll() {
    try {
      const res = await apiClient.getProjects();
      projects = res.projects || [];
      if (selectedProjectId) {
        if (projects.some((p) => p.project_id === selectedProjectId)) {
          await loadProjectDetail(selectedProjectId);
          if (showEnded) {
            await loadEndedWork(selectedProjectId, currentEndedPage);
          }
        } else if (projects.length > 0) {
          selectProject(projects[0].project_id);
        } else {
          selectedProjectId = null;
          detail = null;
        }
      }
    } catch {
      // background refresh fail soft
    }
  }

  /**
   * Opens the actions of one plan or epic beside the project, without leaving the overview.
   *
   * @param kind the subject's kind
   * @param id the plan or epic
   */
  function openControls(kind: 'plan' | 'epic', id: string) {
    controlSubject = { kind, id };
  }

  function closeControls() {
    controlSubject = null;
  }

  function selectProject(projectId: string) {
    detailRequestGeneration += 1;
    endedRequestGeneration += 1;
    selectedProjectId = projectId;
    showEnded = false;
    endedPage = null;
    currentEndedPage = 1;
    if (typeof window !== 'undefined') {
      localStorage.setItem('pm_overview_selected_project', projectId);
      localStorage.setItem('pm_overview_show_ended', '0');
      localStorage.setItem('pm_overview_ended_page', '1');
    }
    void loadProjectDetail(projectId);
  }

  /**
   * Remembers the Ended Work section while the operator opens one of its plans or reports.
   */
  function rememberEndedState() {
    if (typeof window !== 'undefined') {
      localStorage.setItem('pm_overview_show_ended', showEnded ? '1' : '0');
      localStorage.setItem('pm_overview_ended_page', String(currentEndedPage));
    }
  }

  /**
   * Remembers or forgets Running Work as the browser's start page.
   */
  function toggleStartPage() {
    startPageIsOverview = !startPageIsOverview;
    setRememberedStartRoute(startPageIsOverview ? 'overview' : null);
  }

  /**
   * Opens an ended item as it ended: a plan in its plan view, an epic in its epic view.
   *
   * @param item the ended work row
   */
  function openEndedItem(item: EndedWorkPage['items'][number]) {
    const id = item.id ?? item.item_id;
    if (!id) return;
    rememberEndedState();
    if (item.kind === 'epic') {
      openEpicView(id);
      return;
    }
    openEndedPlanView({
      plan_id: id,
      outcome: item.outcome ?? null,
      reason: item.reason ?? null,
      ended_at: item.ended_at ?? null,
    });
  }

  /**
   * Opens an ended plan's completion report in one step.
   *
   * @param item the ended work row of a plan
   */
  function openEndedReport(item: EndedWorkPage['items'][number]) {
    const id = item.id ?? item.item_id;
    if (!id) return;
    rememberEndedState();
    openCompletionReport(id);
  }

  function setViewMode(mode: 'plan' | 'epic') {
    viewMode = mode;
    if (typeof window !== 'undefined') {
      localStorage.setItem('pm_overview_view_mode', mode);
    }
  }

  async function openEndedWork() {
    showEnded = true;
    currentEndedPage = 1;
    rememberEndedState();
    if (selectedProjectId) {
      await loadEndedWork(selectedProjectId, 1);
    }
  }

  function closeEndedWork() {
    showEnded = false;
    currentEndedPage = 1;
    endedRequestGeneration += 1;
    endedPage = null;
    rememberEndedState();
  }

  async function loadEndedWork(projectId: string, page: number) {
    const requestGeneration = ++endedRequestGeneration;
    isLoadingEnded = true;
    try {
      const res = await apiClient.getEndedWork(projectId, page);
      if (projectId === selectedProjectId && requestGeneration === endedRequestGeneration) {
        endedPage = res;
        currentEndedPage = page;
        rememberEndedState();
      }
    } catch (err: unknown) {
      if (projectId === selectedProjectId && requestGeneration === endedRequestGeneration) {
        endedPage = null;
      }
    } finally {
      if (requestGeneration === endedRequestGeneration) {
        isLoadingEnded = false;
      }
    }
  }

  /**
   * @param isoStr the instant the runtime sends
   * @returns its age in the unit that age has, `9 d ago`
   */
  function formatRelative(isoStr?: string | null): string {
    return formatTime(isoStr, 'relative', $currentLocale);
  }

  /**
   * @param isoStr the instant the runtime sends
   * @returns the moment itself, for a list row's hover advisory
   */
  function formatMoment(isoStr?: string | null): string {
    return formatTime(isoStr, 'short', $currentLocale);
  }

  /**
   * @param isoStr the instant a phase started
   * @returns how long the phase has run, `2 h 10 min`
   */
  function phaseDuration(isoStr?: string | null): string {
    if (!isoStr) return '';
    const started = new Date(isoStr);
    if (Number.isNaN(started.getTime())) return '';
    return formatDuration(Math.max(0, Date.now() - started.getTime()), $currentLocale);
  }

  function getPlanPhase(p: ActivePlanSummary): string {
    return p.current_phase || '';
  }

  function getPlanState(p: ActivePlanSummary): string {
    // The wire value stays as the contract sends it: the display layer names it, and the badge
    // class matches both spellings (`badge--state-waiting_for_you`).
    return p.phase_state || 'running';
  }

  // The overview endpoints answer with the snake_case spelling of the API contract or with the
  // camelCase one. The contract types declare the snake_case keys, so the accessors read the
  // alternate spelling through one typed alias instead of a per-site cast.
  type CamelCaseAliases = {
    needsOperatorCount?: number | null;
    needsOperatorKind?: string | null;
    tasksTotal?: number;
    tasksDone?: number;
    epicTitle?: string | null;
    epicId?: string | null;
    offTrackSignal?: string | null;
    pullRequestNumber?: number | null;
    pullRequestUrl?: string | null;
    phaseEnteredAt?: string | null;
    runningPlansCount?: number;
    stagedPlansCount?: number;
    planId?: string;
  };

  function withAliases<T extends object>(value: T): T & CamelCaseAliases {
    return value as T & CamelCaseAliases;
  }

  function planIdOf(plan: ActivePlanSummary): string {
    return plan.plan_id || withAliases(plan).planId || '';
  }

  function epicIdOf(epic: ActiveEpicSummary): string {
    return epic.epic_id || withAliases(epic).epicId || '';
  }

  function getNeedsOperatorCount(item: ActivePlanSummary | ActiveEpicSummary): number {
    return item.needs_operator_count ?? withAliases(item).needsOperatorCount ?? 0;
  }

  function getNeedsOperatorKind(item: ActivePlanSummary | ActiveEpicSummary): string {
    return item.needs_operator_kind ?? withAliases(item).needsOperatorKind ?? '';
  }

  function getPlanTasksTotal(p: ActivePlanSummary): number {
    return p.tasks_total ?? withAliases(p).tasksTotal ?? 0;
  }

  function getPlanTasksDone(p: ActivePlanSummary): number {
    return p.tasks_done ?? withAliases(p).tasksDone ?? 0;
  }

  function getPlanEpicTitle(p: ActivePlanSummary): string {
    return p.epic_title || withAliases(p).epicTitle || p.epic_id || withAliases(p).epicId || '';
  }

  function getPlanOffTrack(p: ActivePlanSummary): string {
    return p.off_track_signal || withAliases(p).offTrackSignal || '';
  }

  function getPlanPrNumber(p: ActivePlanSummary): number | null {
    return p.pull_request_number ?? withAliases(p).pullRequestNumber ?? null;
  }

  function getPlanPrUrl(p: ActivePlanSummary): string {
    return p.pull_request_url || withAliases(p).pullRequestUrl || '#';
  }

  function getPlanPhaseEnteredAt(p: ActivePlanSummary): string | null {
    return p.phase_entered_at || withAliases(p).phaseEnteredAt || null;
  }

  function getEpicRunningCount(e: ActiveEpicSummary): number {
    return e.running_plans_count ?? withAliases(e).runningPlansCount ?? 0;
  }

  function getEpicStagedCount(e: ActiveEpicSummary): number {
    return e.staged_plans_count ?? withAliases(e).stagedPlansCount ?? 0;
  }

  function formatProjectCounts(p: ProjectSummary): string {
    if (p.setup_needs_operator && p.active_plans_count === 0) {
      return $t('overview.project.setup');
    }
    if (p.active_plans_count > 0 && p.active_epics_count > 0) {
      return $t('overview.project.counts', {
        plans: p.active_plans_count,
        epics: p.active_epics_count,
      });
    }
    if (p.active_plans_count > 0) {
      return $t('overview.project.plans_only', { plans: p.active_plans_count });
    }
    if (p.active_epics_count > 0) {
      return $t('overview.project.epics_only', { epics: p.active_epics_count });
    }
    return '';
  }
</script>

<div class="overview overview-container" data-testid="overview-view">
  <!-- Left Column / Projects Sidebar -->
  <aside class="overview-sidebar" aria-label={$t('overview.running.title')}>
    <div class="sidebar-header">
      <h2 class="sidebar-title">{$t('overview.running.title')}</h2>
      <div class="sidebar-header__actions">
        {#if isLoadingProjects}
          <Spinner size="1rem" label={$t('overview.projects.loading')} />
        {/if}
        <button
          class="start-page-toggle"
          class:start-page-toggle--active={startPageIsOverview}
          aria-pressed={startPageIsOverview}
          on:click={toggleStartPage}
          data-testid="start-page-toggle"
        >
          {startPageIsOverview
            ? $t('overview.start_page.is_default')
            : $t('overview.start_page.set')}
        </button>
      </div>
    </div>

    {#if projects.length === 0 && !isLoadingProjects}
      <div class="sidebar-empty">
        <p>{$t('overview.projects.empty')}</p>
      </div>
    {:else}
      <div class="project-list" role="tablist" aria-label={$t('project.explorer.title')}>
        {#each projects as proj}
          <button
            class="project-item"
            class:project-item--selected={proj.project_id === selectedProjectId}
            role="tab"
            aria-selected={proj.project_id === selectedProjectId}
            on:click={() => selectProject(proj.project_id)}
          >
            <div class="project-item__header">
              {#if proj.needs_operator_count > 0}
                <span class="badge badge--attention" title={$t('overview.needs.attention_hint')}>
                  ! {proj.needs_operator_count}
                </span>
              {:else if proj.setup_needs_operator}
                <span class="badge badge--attention" title={$t('overview.needs.setup_hint')}
                  >! 1</span
                >
              {/if}
              <span class="project-item__name">{proj.display_name}</span>
            </div>
            <div class="project-item__meta">
              <span class="project-item__counts">{formatProjectCounts(proj)}</span>
              {#if proj.last_activity_at}
                <span
                  class="project-item__time"
                  title={formatMoment(proj.last_activity_at)}
                  >{formatRelative(proj.last_activity_at)}</span
                >
              {/if}
            </div>
          </button>
        {/each}
      </div>
    {/if}
  </aside>

  <!-- Main Panel -->
  <main class="overview-main">
    {#if !selectedProject}
      <div class="main-empty">
        <p>{$t('overview.projects.empty')}</p>
      </div>
    {:else if showEnded}
      <!-- Ended Work View -->
      <section class="ended-view" aria-label={$t('overview.ended.title')}>
        <div class="ended-header">
          <div>
            <h1 class="project-title">
              {selectedProject.display_name} · {$t('overview.ended.title')}
            </h1>
            {#if endedPage}
              <span class="ended-page-info">
                {$t('overview.ended.page', { page: endedPage.page, pages: endedPage.total_pages })}
              </span>
            {/if}
          </div>
          <Button
            variant="secondary"
            on:click={closeEndedWork}
            data-advisory={$t('common.actions.back.title')}
          >
            {$t('common.actions.back')}
          </Button>
        </div>

        {#if isLoadingEnded}
          <div class="ended-loading">
            <Spinner size="2rem" label={$t('overview.ended.loading')} />
          </div>
        {:else if !endedPage || endedPage.items.length === 0}
          <p class="empty-text">
            {$t('overview.ended.empty', { project: selectedProject.display_name })}
          </p>
        {:else}
          <FilterInput
            value={endedQuery}
            label={$t('common.filter.label')}
            placeholder={$t('common.filter.placeholder')}
            resultCount={filteredEndedItems.length}
            onchange={(value) => (endedQuery = value)}
          />
          {#if filteredEndedItems.length === 0}
            <p class="empty-text" data-testid="ended-filter-empty">{$t('common.filter.empty')}</p>
          {:else}
            <div class="ended-table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>{$t('overview.ended.col.kind')}</th>
                  <th>{$t('overview.ended.col.title')}</th>
                  <th>{$t('overview.ended.col.outcome')}</th>
                  <th>{$t('overview.ended.col.pull_request')}</th>
                  <th>{$t('overview.ended.col.ended')}</th>
                  <th>{$t('overview.ended.col.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {#each filteredEndedItems as item}
                  <tr class="ended-row" data-testid="ended-item">
                    <td>
                      <span class="badge badge--kind badge--{item.kind}">
                        {item.kind === 'epic'
                          ? '◇ ' + $t('overview.kind.epic')
                          : '▸ ' + $t('overview.kind.plan')}
                      </span>
                    </td>
                    <td>
                      <button
                        class="ended-open"
                        on:click={() => openEndedItem(item)}
                        data-testid="ended-open-{item.id ?? item.item_id}"
                      >
                        {item.title}
                      </button>
                    </td>
                    <td>
                      {#if item.outcome}
                        <span class="badge badge--outcome badge--{item.outcome}">
                          {$t('overview.ended.outcome', { outcome: item.outcome })}
                        </span>
                      {:else if item.terminal_reason}
                        <span class="badge badge--outcome badge--{item.terminal_reason}">
                          {$t('overview.ended.epic_end', { end: item.terminal_reason })}
                        </span>
                      {/if}
                      {#if item.reason}
                        <span class="ended-reason"
                          >{$t('overview.ended.reason', { reason: item.reason })}</span
                        >
                      {/if}
                    </td>
                    <td>
                      {#if item.pull_request_number}
                        <a
                          href={item.pull_request_url || '#'}
                          target="_blank"
                          rel="noreferrer"
                          class="pr-link"
                        >
                          #{item.pull_request_number}
                        </a>
                      {:else}
                        —
                      {/if}
                    </td>
                    <td class="text-muted" title={formatMoment(item.ended_at)}>{formatRelative(item.ended_at)}</td>
                    <td>
                      {#if item.kind === 'plan'}
                        <button
                          class="ended-report"
                          on:click={() => openEndedReport(item)}
                          data-testid="ended-report-{item.id ?? item.item_id}"
                          data-advisory={$t('report.completion.open.title')}
                        >
                          {$t('report.completion.open')}
                        </button>
                      {/if}
                    </td>
                  </tr>
                {/each}
              </tbody>
            </table>
          </div>

          <div class="ended-pagination">
            <Button
              variant="secondary"
              disabled={!endedPage.has_newer || currentEndedPage <= 1}
              on:click={() => loadEndedWork(selectedProjectId!, currentEndedPage - 1)}
              data-advisory={$t('overview.ended.newer.title')}
            >
              {$t('overview.ended.newer')}
            </Button>
            <Button
              variant="secondary"
              disabled={!endedPage.has_older}
              on:click={() => loadEndedWork(selectedProjectId!, currentEndedPage + 1)}
              data-advisory={$t('overview.ended.older.title')}
            >
              {$t('overview.ended.older')}
            </Button>
          </div>
          {/if}
        {/if}
      </section>
    {:else}
      <!-- Running Work View -->
      <section class="project-detail" aria-label={selectedProject.display_name}>
        <!-- Project Title Bar & Controls -->
        <div class="project-header">
          <div class="project-header__info">
            <h1 class="project-title">{selectedProject.display_name}</h1>
            <span class="project-header__counts">{formatProjectCounts(selectedProject)}</span>
          </div>

          <div class="project-header__actions">
            <div class="view-toggle" role="radiogroup" aria-label={$t('overview.view.label')}>
              <button
                class="toggle-btn"
                class:toggle-btn--active={viewMode === 'plan'}
                role="radio"
                aria-checked={viewMode === 'plan'}
                data-advisory={$t('overview.view.by_plan.title')}
                on:click={() => setViewMode('plan')}
              >
                {$t('overview.view.by_plan')}
              </button>
              <button
                class="toggle-btn"
                class:toggle-btn--active={viewMode === 'epic'}
                role="radio"
                aria-checked={viewMode === 'epic'}
                data-advisory={$t('overview.view.by_epic.title')}
                on:click={() => setViewMode('epic')}
              >
                {$t('overview.view.by_epic')}
              </button>
            </div>

            <Button
              variant="secondary"
              on:click={openEndedWork}
              data-advisory={$t('overview.ended.open.title')}
            >
              {$t('overview.ended.open')}
            </Button>

            {#each detail?.open_entries ?? [] as entry (entry.entry_id)}
              <Button
                variant="secondary"
                on:click={() => openStartEntry(entry.entry_id, selectedProjectId!)}
              >
                {$t('start.entry.continue', { count: entry.source_count })}
              </Button>
            {/each}

            <Button variant="primary" on:click={() => openStartWork(selectedProjectId!, 'plan')}>
              {$t('start.ready.start_plan')}
            </Button>
            <Button variant="secondary" on:click={() => openStartWork(selectedProjectId!, 'epic')}>
              {$t('start.ready.start_epic')}
            </Button>
            <Button variant="secondary" on:click={() => openGitSource(selectedProjectId!)}>
              {$t('git_sources.mode.adopt_pr')}
            </Button>
          </div>
        </div>

        {#if controlSubject}
          <section class="overview-control" data-testid="overview-control">
            <div class="overview-control__head">
              <button type="button" class="overview-control__close" on:click={closeControls}>
                {$t('common.close')}
              </button>
            </div>
            <WorkControlPanel
              subjectId={controlSubject.id}
              subjectKind={controlSubject.kind}
              projectId={selectedProjectId ?? ''}
              onReview={() => {
                if (controlSubject?.kind === 'epic') {
                  openEpicView(controlSubject.id);
                }
              }}
              onDecompose={() => {
                if (controlSubject?.kind === 'epic') {
                  openEpicView(controlSubject.id);
                }
              }}
              onChanged={refreshAll}
            />
          </section>
        {/if}

        {#if isLoadingDetail}
          <div class="detail-loading">
            <Spinner size="2rem" label={$t('overview.details.loading')} />
          </div>
        {:else if !detail}
          <p class="empty-text">{$t('overview.projects.none_active')}</p>
        {:else}
          <!-- Cards Container -->
          <div class="cards-container">
            {#if (viewMode === 'plan' ? detail.plans.length : detail.epics.length) > 0}
              <FilterInput
                value={runningQuery}
                label={$t('common.filter.label')}
                placeholder={$t('common.filter.placeholder')}
                resultCount={viewMode === 'plan' ? filteredPlans.length : filteredEpics.length}
                onchange={(value) => (runningQuery = value)}
              />
            {/if}
            {#if viewMode === 'plan'}
              <!-- Plans List -->
              {#if filteredPlans.length === 0}
                <p class="empty-text" data-testid="running-filter-empty">
                  {$t('common.filter.empty')}
                </p>
              {:else}
                <div class="plan-cards">
                {#each filteredPlans as plan}
                  <div
                    class="work-card work-card--plan plan-card"
                    data-testid="plan-card"
                    role="button"
                    tabindex="0"
                    on:click={() => openPlanView(planIdOf(plan))}
                    on:keydown={(e) => e.key === 'Enter' && openPlanView(planIdOf(plan))}
                  >
                    <div class="work-card__header">
                      <div class="work-card__title-group">
                        <span class="work-card__glyph">▸</span>
                        <span class="badge badge--kind">{$t('overview.kind.plan')}</span>
                        <h3 class="work-card__title">{plan.title}</h3>
                      </div>
                      <button
                        type="button"
                        class="card-action"
                        data-testid="overview-actions-{plan.plan_id}"
                        data-advisory={$t('control.actions.open.title')}
                        on:click|stopPropagation={() => openControls('plan', plan.plan_id)}
                      >
                        {$t('control.actions.open')}
                      </button>

                      <div class="work-card__badges">
                        {#if getNeedsOperatorCount(plan) > 0}
                          <span class="badge badge--attention">
                            ! {getNeedsOperatorKind(plan)
                              ? $t('overview.needs.' + getNeedsOperatorKind(plan))
                              : 'attention'}
                          </span>
                        {/if}
                        {#if getPlanPhase(plan)}
                          <span class="badge badge--phase">
                            <WorkflowIcon id={phaseIconId(getPlanPhase(plan))} size={16} />
                            {getPlanPhase(plan)}
                          </span>
                        {/if}
                        <span class="badge badge--state badge--state-{getPlanState(plan)}">
                          {displayState('plan', getPlanState(plan), $t)}
                        </span>
                      </div>
                    </div>

                    <div class="work-card__details">
                      {#if getPlanPhaseEnteredAt(plan)}
                        <span class="detail-item">
                          {$t('overview.time.in_phase', {
                            phase: getPlanPhase(plan),
                            duration: phaseDuration(getPlanPhaseEnteredAt(plan)),
                          })}
                        </span>
                      {/if}
                      {#if getPlanTasksTotal(plan) > 0}
                        <span class="detail-item">
                          {$t('overview.progress.tasks', {
                            done: getPlanTasksDone(plan),
                            total: getPlanTasksTotal(plan),
                          })}
                        </span>
                      {/if}
                      {#if getPlanEpicTitle(plan)}
                        <span class="detail-item">
                          {$t('overview.plan.belongs_to', { epic: '◇ ' + getPlanEpicTitle(plan) })}
                        </span>
                      {/if}
                      {#if getPlanOffTrack(plan)}
                        <span
                          class="badge badge--warning badge--off-track"
                          data-testid="off-track-badge"
                        >
                          {$t('overview.plan.off_track', {
                            signal: displayState('signal', getPlanOffTrack(plan), $t),
                          })}
                        </span>
                      {/if}
                      {#if getPlanPrNumber(plan)}
                        <a
                          href={getPlanPrUrl(plan)}
                          target="_blank"
                          rel="noreferrer"
                          class="pr-link"
                          on:click|stopPropagation
                        >
                          #{getPlanPrNumber(plan)}
                        </a>
                      {/if}
                    </div>
                  </div>
                {/each}
              </div>
              {/if}
            {:else}
              <!-- By Epic List -->
              {#if filteredEpics.length === 0}
                <p class="empty-text" data-testid="running-filter-empty">
                  {$t('common.filter.empty')}
                </p>
              {:else}
                <div class="epic-cards">
                {#each filteredEpics as epic}
                  <div
                    class="work-card work-card--epic epic-card"
                    data-testid="epic-card"
                    role="button"
                    tabindex="0"
                    on:click={() => openEpicView(epicIdOf(epic))}
                    on:keydown={(e) => e.key === 'Enter' && openEpicView(epicIdOf(epic))}
                  >
                    <div class="work-card__header">
                      <div class="work-card__title-group">
                        <span class="work-card__glyph">◇</span>
                        <span class="badge badge--kind badge--epic">{$t('overview.kind.epic')}</span
                        >
                        <h3 class="work-card__title">{epic.title}</h3>
                      </div>
                      <button
                        type="button"
                        class="card-action"
                        data-testid="overview-actions-epic-{epicIdOf(epic)}"
                        data-advisory={$t('control.actions.open.title')}
                        on:click|stopPropagation={() => openControls('epic', epicIdOf(epic))}
                      >
                        {$t('control.actions.open')}
                      </button>

                      <div class="work-card__badges">
                        {#if getNeedsOperatorCount(epic) > 0}
                          <span class="badge badge--attention">
                            ! {getNeedsOperatorKind(epic) || 'attention'}
                          </span>
                        {/if}
                        <span class="badge badge--counts">
                          {$t('overview.epic.plans', {
                            running: getEpicRunningCount(epic),
                            staged: getEpicStagedCount(epic),
                          })}
                        </span>
                      </div>
                    </div>

                    <!-- Nested Plans -->
                    {#if epic.plans && epic.plans.length > 0}
                      <div class="nested-plans">
                        {#each epic.plans as childPlan, idx}
                          <div
                            class="nested-plan-row"
                            role="button"
                            tabindex="0"
                            on:click|stopPropagation={() => openPlanView(planIdOf(childPlan))}
                            on:keydown|stopPropagation={(e) =>
                              e.key === 'Enter' && openPlanView(planIdOf(childPlan))}
                          >
                            <span class="nested-tree-char"
                              >{idx === epic.plans.length - 1 ? '└─' : '├─'}</span
                            >
                            <span class="nested-plan-title">{childPlan.title}</span>
                            {#if getPlanPhase(childPlan)}
                              <span class="badge badge--phase">
                                <WorkflowIcon id={phaseIconId(getPlanPhase(childPlan))} size={16} />
                                {getPlanPhase(childPlan)}
                              </span>
                            {/if}
                            <span class="badge badge--state badge--state-{getPlanState(childPlan)}">
                              {displayState('plan', getPlanState(childPlan), $t)}
                            </span>
                            <button
                              type="button"
                              class="card-action"
                              data-testid="overview-actions-{planIdOf(childPlan)}"
                              data-advisory={$t('control.actions.open.title')}
                              on:click|stopPropagation={() =>
                                openControls('plan', planIdOf(childPlan))}
                            >
                              {$t('control.actions.open')}
                            </button>
                          </div>
                        {/each}
                      </div>
                    {/if}
                  </div>
                {/each}

                <!-- Without Epic Section -->
                {#if filteredPlans.some((p) => !p.epic_id && !withAliases(p).epicId)}
                  <div class="without-epic-section">
                    <h4 class="section-subtitle">{$t('overview.group.without_epic')}</h4>
                    {#each filteredPlans.filter((p) => !p.epic_id && !withAliases(p).epicId) as plan}
                      <div
                        class="work-card work-card--plan"
                        role="button"
                        tabindex="0"
                        on:click={() => openPlanView(planIdOf(plan))}
                        on:keydown={(e) => e.key === 'Enter' && openPlanView(planIdOf(plan))}
                      >
                        <div class="work-card__header">
                          <div class="work-card__title-group">
                            <span class="work-card__glyph">▸</span>
                            <span class="badge badge--kind">{$t('overview.kind.plan')}</span>
                            <h3 class="work-card__title">{plan.title}</h3>
                          </div>
                          <div class="work-card__badges">
                            {#if getNeedsOperatorCount(plan) > 0}
                              <span class="badge badge--attention">
                                ! {getNeedsOperatorKind(plan)
                                  ? $t('overview.needs.' + getNeedsOperatorKind(plan))
                                  : 'attention'}
                              </span>
                            {/if}
                            {#if getPlanPhase(plan)}
                              <span class="badge badge--phase">
                                <WorkflowIcon id={phaseIconId(getPlanPhase(plan))} size={16} />
                                {getPlanPhase(plan)}
                              </span>
                            {/if}
                            <span class="badge badge--state badge--state-{getPlanState(plan)}">
                              {displayState('plan', getPlanState(plan), $t)}
                            </span>
                          </div>
                        </div>
                      </div>
                    {/each}
                  </div>
                {/if}
              </div>
              {/if}
            {/if}

            <!-- Alongside Section -->
            {#if detail.alongside_landings && detail.alongside_landings.length > 0}
              <div class="alongside-section" data-testid="alongside-section">
                <h4 class="section-subtitle">{$t('overview.group.alongside')}</h4>
                {#each detail.alongside_landings as landing}
                  <div class="alongside-row">
                    {#if landing.state === 'failed'}
                      <span class="badge badge--attention"
                        >! {$t('overview.needs.landing_failed')}</span
                      >
                    {/if}
                    <span class="alongside-desc">
                      {#if landing.unit}
                        {$t('overview.landing.unit', {
                          unit: displayUnit(landing.unit, $t),
                          state: displayState('landing', landing.state, $t),
                        })}
                        {#if !hasUnitName(landing.unit)}
                          <span class="chip chip--unit">{landing.unit}</span>
                        {/if}
                      {:else if landing.count}
                        {$t('overview.landing.removal', {
                          count: landing.count,
                          state: displayState('landing', landing.state, $t),
                        })}
                      {:else}
                        {displayState('landing', landing.state, $t)}
                      {/if}
                    </span>
                    <span class="badge badge--state badge--state-{landing.state}"
                      >{displayState('landing', landing.state, $t)}</span
                    >
                  </div>
                {/each}
              </div>
            {/if}
          </div>
        {/if}
      </section>
    {/if}
  </main>
</div>

<style>
  .overview-container {
    display: flex;
    flex-direction: row;
    height: 100%;
    min-height: calc(100vh - 4rem);
    background-color: var(--color-bg-app);
  }

  /* Sidebar */
  .overview-sidebar {
    width: 280px;
    flex-shrink: 0;
    border-right: 1px solid var(--color-border);
    background-color: var(--color-bg-surface);
    display: flex;
    flex-direction: column;
  }

  .sidebar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--space-4);
    border-bottom: 1px solid var(--color-border-subtle);
  }

  .sidebar-title {
    font-size: var(--text-base);
    font-weight: 600;
    margin: 0;
    color: var(--color-text-primary);
  }

  .sidebar-header__actions {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .start-page-toggle {
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-xs);
    font-weight: 500;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background: var(--color-bg-surface);
    color: var(--color-text-secondary);
    cursor: pointer;
  }

  .start-page-toggle:hover {
    background: var(--color-bg-subtle);
    color: var(--color-text-primary);
  }

  .start-page-toggle--active {
    background: var(--color-primary-subtle);
    border-color: var(--color-primary);
    color: var(--color-primary);
  }

  .sidebar-empty {
    padding: var(--space-4);
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .project-list {
    display: flex;
    flex-direction: column;
    overflow-y: auto;
  }

  .project-item {
    display: flex;
    flex-direction: column;
    padding: var(--space-3) var(--space-4);
    text-align: left;
    background: none;
    border: none;
    border-left: 3px solid transparent;
    border-bottom: 1px solid var(--color-border-subtle);
    cursor: pointer;
    transition: background-color 0.15s ease;
  }

  .project-item:hover {
    background-color: var(--color-bg-subtle);
  }

  .project-item--selected {
    background-color: var(--color-primary-subtle);
    border-left-color: var(--color-primary);
  }

  .project-item__header {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin-bottom: var(--space-1);
  }

  .project-item__name {
    font-weight: 600;
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .project-item__meta {
    display: flex;
    justify-content: space-between;
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  /* Main Panel */
  .overview-main {
    flex: 1;
    overflow-y: auto;
    padding: var(--space-6);
  }

  .main-empty {
    padding: var(--space-8);
    text-align: center;
    color: var(--color-text-muted);
  }

  .project-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: var(--space-4);
    margin-bottom: var(--space-6);
    padding-bottom: var(--space-4);
    border-bottom: 1px solid var(--color-border);
  }

  .project-title {
    font-size: var(--text-xl);
    font-weight: 700;
    margin: 0 0 var(--space-1) 0;
    color: var(--color-text-primary);
  }

  .project-header__counts {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .project-header__actions {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  .view-toggle {
    display: flex;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    overflow: hidden;
  }

  .toggle-btn {
    padding: var(--space-1) var(--space-3);
    font-size: var(--text-xs);
    font-weight: 500;
    background: var(--color-bg-surface);
    color: var(--color-text-secondary);
    border: none;
    cursor: pointer;
  }

  .toggle-btn--active {
    background: var(--color-primary);
    color: var(--color-primary-text);
  }

  /* Cards */
  .cards-container {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .work-card {
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-4);
    box-shadow: var(--shadow-sm);
  }

  .work-card__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: var(--space-2);
    margin-bottom: var(--space-2);
  }

  .work-card__title-group {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .work-card__glyph {
    color: var(--color-primary);
    font-weight: bold;
  }

  .work-card--epic .work-card__glyph {
    color: var(--color-epic);
  }

  .work-card__title {
    margin: 0;
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .work-card__badges {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .work-card__details {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-3);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .detail-item::after {
    content: '·';
    margin-left: var(--space-3);
  }

  .detail-item:last-child::after {
    content: '';
    margin-left: 0;
  }

  /* Nested Plans */
  .nested-plans {
    margin-top: var(--space-3);
    padding-top: var(--space-2);
    border-top: 1px solid var(--color-border-subtle);
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .nested-plan-row {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
    padding-left: var(--space-4);
  }

  .nested-tree-char {
    font-family: var(--font-mono);
    color: var(--color-text-muted);
  }

  .nested-plan-title {
    flex: 1;
    color: var(--color-text-primary);
  }

  /* Alongside Section */
  .section-subtitle {
    font-size: var(--text-sm);
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--color-text-muted);
    margin: var(--space-4) 0 var(--space-2) 0;
  }

  .alongside-row {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-2) var(--space-3);
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    font-size: var(--text-sm);
  }

  .alongside-desc {
    flex: 1;
  }

  /* The id of a unit the catalogue does not name: a chip beside the unknown word, so two
     landings of unnamed units stay distinguishable (CC-P-01). */
  .chip--unit {
    display: inline-block;
    margin-inline-start: 0.35rem;
    padding: 0 0.35rem;
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-sm);
    font-family: var(--font-mono);
    font-size: var(--text-xs);
  }

  /* Ended Work */
  .ended-view {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .ended-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid var(--color-border);
    padding-bottom: var(--space-4);
  }

  .ended-page-info {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .ended-table-wrap {
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    overflow-x: auto;
  }

  .ended-reason {
    display: block;
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .ended-open {
    padding: 0;
    border: none;
    background: none;
    color: var(--color-text-primary);
    font-weight: 600;
    font-size: inherit;
    font-family: inherit;
    text-align: left;
    cursor: pointer;
    text-decoration: underline;
    text-decoration-color: var(--color-border);
  }

  .ended-open:hover {
    color: var(--color-primary);
  }

  .ended-report {
    padding: 0;
    border: none;
    background: none;
    color: var(--color-primary);
    font-size: var(--text-xs);
    font-family: inherit;
    cursor: pointer;
    text-decoration: underline;
  }

  .ended-pagination {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-2);
  }

  /* Badges */
  .badge {
    display: inline-flex;
    align-items: center;
    gap: 0.35em;
    padding: 0.125rem 0.5rem;
    font-size: var(--text-xs);
    font-weight: 500;
    border-radius: var(--radius-full);
  }

  .badge--attention {
    background-color: var(--color-danger-subtle);
    color: var(--color-danger-text);
    font-weight: 600;
  }

  .badge--kind {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
  }

  .badge--epic {
    background-color: var(--color-epic-subtle);
    color: var(--color-epic-text);
  }

  .badge--phase {
    background-color: var(--color-bg-inset);
    color: var(--color-text-primary);
    font-family: var(--font-mono);
  }

  .badge--state {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
  }

  .badge--state-running {
    background-color: var(--color-success-subtle);
    color: var(--color-success-text);
  }

  .badge--state-waiting,
  .badge--state-waiting_for_you {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning-text);
  }

  .badge--state-paused,
  .badge--state-pausing {
    background-color: var(--color-bg-inset);
    color: var(--color-text-secondary);
  }

  .badge--warning {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning-text);
    font-weight: 600;
  }

  .badge--outcome {
    font-weight: 600;
  }

  .badge--merged,
  .badge--completed {
    background-color: var(--color-success-subtle);
    color: var(--color-success-text);
  }

  .badge--cancelled,
  .badge--aborted {
    background-color: var(--color-danger-subtle);
    color: var(--color-danger-text);
  }

  .pr-link {
    color: var(--color-primary);
    text-decoration: none;
    font-family: var(--font-mono);
  }

  .pr-link:hover {
    text-decoration: underline;
  }

  /* Responsive Design down to 320px */
  @media (max-width: 768px) {
    .overview-container {
      flex-direction: column;
    }

    .overview-sidebar {
      width: 100%;
      border-right: none;
      border-bottom: 1px solid var(--color-border);
      max-height: 200px;
    }

    .overview-main {
      padding: var(--space-3);
    }

    .project-header {
      flex-direction: column;
      align-items: flex-start;
    }

    .project-header__actions {
      width: 100%;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: var(--space-2);
    }

    .work-card__header {
      flex-direction: column;
      align-items: flex-start;
    }
  }
</style>
