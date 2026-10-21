<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import { apiClient, normaliseReportItem, type ReportItem, type ReportsPage } from '../api/client';
  import { runtimeStore, refreshReportsCount, syncUrl, navigateTo } from '../stores/runtime';
  import CompletionReportView from './CompletionReportView.svelte';

  let items = $state<ReportItem[]>([]);
  let unreadCount = $state(0);
  let hasOlder = $state(false);
  let olderCount = $state(0);
  let isLoading = $state(true);
  let isLoadingOlder = $state(false);
  let errorMessage = $state('');

  /** The coalesced quiet notice of the reports that arrived while this view was open. */
  let noticeCount = $state(0);
  let noticeFrom = $state('');
  let noticeSubject = $state('');
  let noticeTimer: ReturnType<typeof setTimeout> | null = null;

  let activeDetailPlanId = $state<string | null>(null);
  let syncedRouteReportPlan: string | null | undefined;

  let unsubscribeEvents: (() => void) | null = null;
  let isMounted = true;

  interface ProjectGroup {
    project: string;
    projectId?: string;
    epicGroups: Record<string, ReportItem[]>;
    planItems: ReportItem[];
    projectItems: ReportItem[];
  }

  let groupedReports = $derived.by<ProjectGroup[]>(() => {
    const projectMap: Record<string, ProjectGroup> = {};

    for (const item of items) {
      const proj = item.from?.project || $t('report.list.machine');
      if (!projectMap[proj]) {
        projectMap[proj] = {
          project: proj,
          projectId: item.from?.project,
          epicGroups: {},
          planItems: [],
          projectItems: [],
        };
      }
      const group = projectMap[proj];
      if (!group.projectId && item.from?.project) group.projectId = item.from.project;
      if (item.from?.epic) {
        if (!group.epicGroups[item.from.epic]) {
          group.epicGroups[item.from.epic] = [];
        }
        group.epicGroups[item.from.epic].push(item);
      } else if (item.from?.plan) {
        group.planItems.push(item);
      } else {
        group.projectItems.push(item);
      }
    }

    return Object.values(projectMap);
  });

  /**
   * The plan whose completion report the route opened, from `/reports?plan={id}` or the store a
   * plan view set when it led here.
   *
   * @returns the plan id, or `null` for the plain list
   */
  function requestedReportPlan(): string | null {
    if (typeof window !== 'undefined') {
      const fromQuery = new URLSearchParams(window.location.search).get('plan');
      if (fromQuery) return fromQuery;
    }
    return $runtimeStore.selectedReportDetailPlanId ?? null;
  }

  /** Leaves the detail and returns to the list, clearing the plan from the address bar. */
  function closeDetail(): void {
    activeDetailPlanId = null;
    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: null }));
    syncUrl('/reports', true);
  }

  // The reports route stays the same while its detail changes (a header visit to Reports, the
  // browser's back between two reports). Follow the store so the view does not keep a plan the
  // address no longer names.
  $effect(() => {
    if ($runtimeStore.currentRoute !== 'reports') return;
    const routePlan = $runtimeStore.selectedReportDetailPlanId ?? null;
    if (routePlan === syncedRouteReportPlan) return;
    syncedRouteReportPlan = routePlan;
    activeDetailPlanId = routePlan;
  });

  onMount(async () => {
    isMounted = true;
    const initialPlan = requestedReportPlan();
    activeDetailPlanId = initialPlan;
    syncedRouteReportPlan = initialPlan;
    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: initialPlan }));
    unsubscribeEvents = apiClient.subscribeEvents((event, data) => {
      handleEvent(event, data);
    });
    await loadReports();
  });

  onDestroy(() => {
    isMounted = false;
    if (noticeTimer) {
      clearTimeout(noticeTimer);
      noticeTimer = null;
    }
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  function handleEvent(event: string, data: unknown) {
    if (!isMounted) return;
    if (event === 'report.sent') {
      // The runtime announces a report wrapped as `{ report: … }`; a fixture may send it bare.
      const payload = (data ?? {}) as { report?: ReportItem };
      const received = payload.report ?? (data as ReportItem);
      const newReport = received ? normaliseReportItem(received) : received;
      if (newReport && newReport.id) {
        if (!items.some((i) => i.id === newReport.id)) {
          items = [newReport, ...items];
          unreadCount++;
          showArrivalNotice(newReport);
        }
      } else {
        void loadReports();
      }
    } else if (event === 'report.read') {
      const readData = data as { report_id?: string; report_ids?: string[] };
      const ids = readData?.report_ids || (readData?.report_id ? [readData.report_id] : []);
      if (ids.length > 0) {
        items = items.filter((i) => !ids.includes(i.id));
        unreadCount = Math.max(0, unreadCount - ids.length);
      } else {
        void loadReports();
      }
    }
  }

  /**
   * Shows the quiet notice of an arrival, coalescing the reports that arrive together.
   *
   * The notice is a polite live region: it never takes the focus, never ends a dialogue, and
   * leaves the tab title alone.
   *
   * @param report the report that arrived
   */
  function showArrivalNotice(report: ReportItem): void {
    noticeCount++;
    noticeFrom = fromLabel(report);
    noticeSubject = report.subject ?? '';
    if (noticeTimer) clearTimeout(noticeTimer);
    noticeTimer = setTimeout(() => {
      noticeCount = 0;
      noticeTimer = null;
    }, 6000);
  }

  /**
   * @param report a report
   * @returns the deepest part of its origin, as the notice names it
   */
  function fromLabel(report: ReportItem): string {
    return (
      report.from?.plan ?? report.from?.epic ?? report.from?.project ?? $t('report.list.machine')
    );
  }

  async function loadReports() {
    isLoading = true;
    errorMessage = '';
    try {
      const page: ReportsPage = await apiClient.getReports();
      items = page.items || [];
      unreadCount = page.unread_count ?? items.length;
      hasOlder = page.has_older;
      olderCount = page.older_count;
      void refreshReportsCount();
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('report.list.load_failed');
    } finally {
      isLoading = false;
    }
  }

  async function loadOlder() {
    isLoadingOlder = true;
    try {
      const page: ReportsPage = await apiClient.getReports(true);
      const existingIds = new Set(items.map((i) => i.id));
      const newItems = (page.items || []).filter((i) => !existingIds.has(i.id));
      items = [...items, ...newItems];
      hasOlder = page.has_older;
      olderCount = page.older_count;
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('report.list.older_failed');
    } finally {
      isLoadingOlder = false;
    }
  }

  async function markRead(item: ReportItem) {
    try {
      await apiClient.markReportRead(item.id);
      items = items.filter((i) => i.id !== item.id);
      unreadCount = Math.max(0, unreadCount - 1);
      void refreshReportsCount();
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('report.list.mark_read_failed');
    }
  }

  async function markAllShownRead() {
    if (items.length === 0) return;
    const ids = items.map((i) => i.id);
    try {
      await apiClient.markAllReportsRead(ids);
      items = [];
      unreadCount = 0;
      void refreshReportsCount();
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('report.list.mark_all_failed');
    }
  }

  /**
   * Opens a report's detail and records the plan on the route, so a plain visit to Reports
   * closes it the same way it closes a detail reached by its query.
   *
   * @param planId the plan whose completion report to show
   */
  function showDetail(planId: string): void {
    activeDetailPlanId = planId;
    syncedRouteReportPlan = planId;
    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: planId }));
  }

  /**
   * @param report a report
   * @returns the kind of the report's subject, as the detail chooses its view
   */
  function subjectKindOf(report: ReportItem): string {
    if (report.subject_kind) return report.subject_kind;
    if (report.kind?.startsWith('system.cleanup')) return 'cleanup';
    if (report.kind?.startsWith('source.')) return 'start';
    return (report.kind ?? '').split('.')[0] ?? 'plan';
  }

  /**
   * @param report a report
   * @returns whether its detail is offered at all
   */
  function hasDetail(report: ReportItem): boolean {
    if (report.subject_deleted) return true;
    const kind = subjectKindOf(report);
    if (
      ['plan', 'epic', 'project', 'start', 'cleanup', 'decision'].includes(kind) ||
      ['plan_completed', 'subject'].includes(kind)
    ) {
      return true;
    }
    return Boolean(report.detail_link || report.detail);
  }

  /**
   * Opens a part of the report's origin without reading the report.
   *
   * The path is the way to the subject's view; only the details action reads.
   *
   * @param part the part of the path the operator chose
   * @param report the report whose path it is
   */
  function openFromPart(part: 'project' | 'epic' | 'plan', report: ReportItem): void {
    if (part === 'project' && report.from?.project) {
      navigateTo('projects', { explorerProjectId: report.from.project });
    } else if (part === 'epic' && report.from?.epic) {
      navigateTo('epic', { epicId: report.from.epic });
    } else if (part === 'plan' && report.from?.plan) {
      navigateTo('plan', { planId: report.from.plan });
    }
  }

  /**
   * @param report a report
   * @returns the plan id its kind names, where it names one
   */
  function planIdOf(report: ReportItem): string | null {
    if (report.from?.plan) return report.from.plan;
    const params = report.subject_params ?? {};
    const fromParams = params['plan'];
    if (typeof fromParams === 'string' && fromParams.length > 0) return fromParams;
    const link = report.detail_link || report.detail || '';
    const completion = /plans\/([^/]+)\/completion/.exec(link);
    if (completion?.[1]) return completion[1];
    const plan = /plans\/([^/]+)$/.exec(link);
    return plan?.[1] ?? null;
  }

  /**
   * @param report a report
   * @returns the epic id its kind names, where it names one
   */
  function epicIdOf(report: ReportItem): string | null {
    if (report.from?.epic) return report.from.epic;
    const link = report.detail_link || report.detail || '';
    return /epics\/([^/]+)/.exec(link)?.[1] ?? null;
  }

  /**
   * @param report a report
   * @returns the cleanup item its subject names, where it names one
   */
  function cleanupItemOf(report: ReportItem): string | null {
    const params = report.subject_params ?? {};
    for (const key of ['item', 'candidate', 'plan', 'epic']) {
      const value = params[key];
      if (typeof value === 'string' && value.length > 0) return value;
    }
    return null;
  }

  /**
   * Opens a report's detail and reads the report in the same act.
   *
   * @param report the report to open
   */
  async function openDetail(item: ReportItem) {
    if (item.subject_deleted) return;

    // Opening the details reads the report, in the same act.
    void markRead(item);

    const kind = subjectKindOf(item);
    if (kind === 'plan' || kind === 'plan_completed') {
      const planId = planIdOf(item);
      if (!planId) return;
      const link = item.detail_link || item.detail || '';
      if (item.kind === 'plan.ended' || link.includes('/completion')) {
        showDetail(planId);
      } else {
        navigateTo('plan', { planId });
      }
      return;
    }
    if (kind === 'epic') {
      const epicId = epicIdOf(item);
      if (epicId) navigateTo('epic', { epicId });
      return;
    }
    if (kind === 'project') {
      if (item.from?.project) {
        navigateTo('projects', { explorerProjectId: item.from.project });
      }
      return;
    }
    if (kind === 'start') {
      navigateTo('start', { projectId: item.from?.project });
      return;
    }
    if (kind === 'cleanup') {
      runtimeStore.update((s) => ({
        ...s,
        cleanupHighlightItemId: cleanupItemOf(item),
      }));
      navigateTo('cleanup');
      return;
    }
    if (kind === 'decision') {
      navigateTo('decisions');
      return;
    }

    // A report whose kind names no view of its own follows its detail link.
    const detailLink = item.detail_link || item.detail;
    if (!detailLink) return;
    const match = /plans\/([^/]+)\/completion/.exec(detailLink);
    if (match?.[1]) {
      showDetail(match[1]);
      return;
    }
    if (item.from?.plan) {
      showDetail(item.from.plan);
    }
  }

  /**
   * @param report a report
   * @returns the name the deleted-subject explanation uses
   */
  function subjectLabelOf(report: ReportItem): string {
    return report.from?.plan ?? report.from?.epic ?? report.subject;
  }

  function formatTime(item: ReportItem): string {
    const createdStr = item.created || item.created_at;
    const isSentLater =
      (item.sent && item.created && item.sent !== item.created) ||
      (item.sent_at && item.created_at && item.sent_at !== item.created_at);

    if (isSentLater) {
      const sentStr = item.sent || item.sent_at || '';
      return `${createdStr} · ${$t('report.list.sent_at', { time: sentStr })}`;
    }
    return createdStr;
  }
</script>

<div class="reports-view">
  {#snippet reportCard(item: ReportItem)}
    <div class="report-card" class:report-card--deleted={item.subject_deleted}>
      <div class="report-card__header">
        <span class="item-glyph" aria-hidden="true">▸</span>
        <span class="report-from mono">
          {#if item.from?.project}
            <button
              type="button"
              class="path-link"
              aria-label={$t('report.from.open_project', { project: item.from.project })}
              onclick={() => openFromPart('project', item)}
            >
              {item.from.project}
            </button>
          {:else}
            <span class="text-muted">{$t('report.list.machine')}</span>
          {/if}
          {#if item.from?.epic}
            <span class="path-sep" aria-hidden="true">›</span>
            <button
              type="button"
              class="path-link"
              aria-label={$t('report.from.open_epic', { epic: item.from.epic })}
              onclick={() => openFromPart('epic', item)}
            >
              {item.from.epic}
            </button>
          {/if}
          {#if item.from?.plan}
            <span class="path-sep" aria-hidden="true">›</span>
            <button
              type="button"
              class="path-link"
              aria-label={$t('report.from.open_plan', { plan: item.from.plan })}
              onclick={() => openFromPart('plan', item)}
            >
              {item.from.plan}
            </button>
          {/if}
        </span>
        <span class="item-subject font-semibold">{item.subject}</span>
        <span class="item-time text-muted">{formatTime(item)}</span>
      </div>

      {#if item.body || item.summary}
        <div class="report-card__body text-muted">
          {item.body || item.summary}
        </div>
      {/if}

      <div class="report-card__actions">
        {#if hasDetail(item)}
          {#if item.subject_deleted}
            <button
              class="action-btn action-btn--detail"
              disabled
              title={$t('report.detail.deleted_by_cleanup', {
                item: subjectLabelOf(item),
                date: item.created || item.created_at,
              })}
            >
              {$t('report.action.details')}
            </button>
            <span class="deleted-note text-muted">
              {$t('report.detail.deleted_by_cleanup', {
                item: subjectLabelOf(item),
                date: item.created || item.created_at,
              })}
            </span>
          {:else}
            <button class="action-btn action-btn--detail" onclick={() => openDetail(item)}>
              {$t('report.action.details')}
            </button>
          {/if}
        {/if}
        <button class="action-btn action-btn--read" onclick={() => markRead(item)}>
          {$t('report.action.mark_read')}
        </button>
      </div>
    </div>
  {/snippet}

  {#if noticeCount > 0}
    <div
      class="arrival-notice"
      role="status"
      aria-live="polite"
      data-testid="report-arrival-notice"
    >
      {#if noticeCount === 1}
        {$t('report.notice.one', { from: noticeFrom, subject: noticeSubject })}
      {:else}
        {$t('report.notice.many', { count: noticeCount, from: noticeFrom })}
      {/if}
    </div>
  {/if}

  {#if activeDetailPlanId}
    {#key activeDetailPlanId}
      <CompletionReportView planId={activeDetailPlanId} onClose={closeDetail} />
    {/key}
  {:else}
    <div class="reports-container">
      <!-- Header -->
      <header class="reports-header">
        <div class="reports-header__title-row">
          <h1 class="reports-heading">
            {$t('report.list.title')}
            {#if unreadCount > 0}
              <span class="unread-pill">
                {$t('report.list.unread', { count: unreadCount })}
              </span>
            {/if}
          </h1>
          {#if items.length > 0}
            <Button variant="secondary" size="small" onclick={markAllShownRead}>
              {$t('report.action.mark_all_read')}
            </Button>
          {/if}
        </div>
      </header>

      {#if errorMessage}
        <div class="error-banner" role="alert">
          <p>{errorMessage}</p>
          <Button variant="secondary" size="small" onclick={loadReports}>
            {$t('web.common.retry')}
          </Button>
        </div>
      {/if}

      {#if isLoading}
        <div class="loading-state">
          <Spinner size="2.5rem" label={$t('report.list.loading')} />
        </div>
      {:else if items.length === 0}
        <div class="empty-state">
          <p class="empty-title">
            {$t('report.list.empty')}
          </p>
        </div>
      {:else}
        <!-- Grouped List -->
        <div class="reports-tree">
          {#each groupedReports as pGroup}
            <section class="project-section">
              <h2 class="project-header">
                <span class="project-glyph" aria-hidden="true">▪</span>
                {#if pGroup.projectId}
                  <button
                    type="button"
                    class="group-link"
                    aria-label={$t('report.from.open_project', { project: pGroup.project })}
                    onclick={() => navigateTo('projects', { explorerProjectId: pGroup.projectId })}
                  >
                    {pGroup.project}
                  </button>
                {:else}
                  <span class="project-title">{$t('report.list.machine')}</span>
                {/if}
              </h2>

              <div class="project-content">
                <!-- Epic groups -->
                {#each Object.entries(pGroup.epicGroups) as [epicName, epicItems]}
                  <div class="epic-section">
                    <h3 class="epic-header">
                      <span class="epic-glyph" aria-hidden="true">◇</span>
                      <button
                        type="button"
                        class="group-link"
                        aria-label={$t('report.from.open_epic', { epic: epicName })}
                        onclick={() => openFromPart('epic', epicItems[0])}
                      >
                        {epicName}
                      </button>
                    </h3>
                    <div class="items-list">
                      {#each epicItems as item}
                        {@render reportCard(item)}
                      {/each}
                    </div>
                  </div>
                {/each}

                <!-- Plan items under project without epic -->
                {#if pGroup.planItems.length > 0}
                  <div class="items-list">
                    {#each pGroup.planItems as item}
                      {@render reportCard(item)}
                    {/each}
                  </div>
                {/if}

                <!-- Project-level items -->
                {#if pGroup.projectItems.length > 0}
                  <div class="items-list">
                    {#each pGroup.projectItems as item}
                      {@render reportCard(item)}
                    {/each}
                  </div>
                {/if}
              </div>
            </section>
          {/each}
        </div>

        <!-- Pagination -->
        {#if hasOlder}
          <div class="pagination-bar">
            <Button variant="secondary" disabled={isLoadingOlder} onclick={loadOlder}>
              {#if isLoadingOlder}
                {$t('report.list.loading_older')}
              {:else if olderCount > 0}
                {$t('report.list.show_older', { count: olderCount })}
              {:else}
                {$t('report.list.show_older_plain')}
              {/if}
            </Button>
          </div>
        {/if}
      {/if}
    </div>
  {/if}
</div>

<style>
  .reports-view {
    max-width: 900px;
    margin: 0 auto;
    padding: var(--space-6) var(--space-4);
  }

  .reports-header {
    margin-bottom: var(--space-6);
    border-bottom: 1px solid var(--color-border);
    padding-bottom: var(--space-4);
  }

  .arrival-notice {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }

  .reports-header__title-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-3);
  }

  .reports-heading {
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0;
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  .unread-pill {
    font-size: var(--text-xs);
    font-weight: 600;
    padding: var(--space-1) var(--space-3);
    background: var(--color-primary-subtle);
    color: var(--color-primary);
    border-radius: var(--radius-full);
  }

  .error-banner {
    background: var(--color-danger-subtle);
    border: 1px solid var(--color-danger-border);
    color: var(--color-danger);
    padding: var(--space-4);
    border-radius: var(--radius-md);
    margin-bottom: var(--space-4);
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .loading-state {
    display: flex;
    justify-content: center;
    padding: var(--space-10) 0;
  }

  .empty-state {
    text-align: center;
    padding: var(--space-10) var(--space-4);
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
  }

  .empty-title {
    color: var(--color-text-muted);
    font-size: var(--text-base);
    margin: 0;
  }

  .reports-tree {
    display: flex;
    flex-direction: column;
    gap: var(--space-6);
  }

  .project-section {
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-4);
  }

  .project-header {
    font-size: var(--text-lg);
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0 0 var(--space-3) 0;
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .project-glyph {
    color: var(--color-primary);
  }

  .project-content {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    padding-left: var(--space-3);
  }

  .epic-section {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .epic-header {
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-text-secondary);
    margin: 0;
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .epic-glyph {
    color: var(--color-text-muted);
  }

  .items-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding-left: var(--space-3);
  }

  .report-card {
    background: var(--color-bg-subtle);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    padding: var(--space-3) var(--space-4);
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .report-card__header {
    display: flex;
    align-items: baseline;
    gap: var(--space-2);
    flex-wrap: wrap;
    font-size: var(--text-sm);
  }

  .item-glyph {
    color: var(--color-primary);
  }

  .report-from {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
    flex-wrap: wrap;
  }

  .path-link,
  .group-link {
    background: none;
    border: none;
    padding: 0;
    font: inherit;
    color: var(--color-primary);
    cursor: pointer;
    text-align: left;
    text-decoration: none;
  }

  .path-link:hover,
  .group-link:hover {
    text-decoration: underline;
  }

  .group-link {
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .path-sep {
    color: var(--color-border);
  }

  .deleted-note {
    font-size: var(--text-xs);
    font-style: italic;
  }

  .report-card--deleted {
    opacity: 0.85;
  }

  .action-btn:disabled {
    color: var(--color-text-muted);
    cursor: not-allowed;
    text-decoration: line-through;
  }

  .item-subject {
    color: var(--color-text-primary);
    flex: 1;
    min-width: 180px;
  }

  .item-time {
    font-size: var(--text-xs);
    margin-left: auto;
  }

  .report-card__body {
    font-size: var(--text-xs);
    line-height: 1.4;
    padding-left: var(--space-4);
  }

  .report-card__actions {
    display: flex;
    gap: var(--space-3);
    margin-top: var(--space-1);
    padding-left: var(--space-4);
  }

  .action-btn {
    background: none;
    border: none;
    padding: 0;
    font-size: var(--text-xs);
    font-weight: 500;
    cursor: pointer;
    text-decoration: underline;
  }

  .action-btn--detail {
    color: var(--color-primary);
  }

  .action-btn--read {
    color: var(--color-text-muted);
  }

  .action-btn--read:hover {
    color: var(--color-text-primary);
  }

  .pagination-bar {
    display: flex;
    justify-content: center;
    margin-top: var(--space-6);
  }

  @media (max-width: 640px) {
    .project-content {
      padding-left: 0;
    }

    .items-list {
      padding-left: 0;
    }

    .item-time {
      margin-left: 0;
      width: 100%;
    }
  }
</style>
