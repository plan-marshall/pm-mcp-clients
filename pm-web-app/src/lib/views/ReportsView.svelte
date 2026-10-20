<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import { apiClient, type ReportItem, type ReportsPage } from '../api/client';
  import { runtimeStore, refreshReportsCount, syncUrl } from '../stores/runtime';
  import CompletionReportView from './CompletionReportView.svelte';

  let items = $state<ReportItem[]>([]);
  let unreadCount = $state(0);
  let hasOlder = $state(false);
  let olderCount = $state(0);
  let isLoading = $state(true);
  let isLoadingOlder = $state(false);
  let errorMessage = $state('');

  let activeDetailPlanId = $state<string | null>(null);

  let unsubscribeEvents: (() => void) | null = null;
  let isMounted = true;

  interface ProjectGroup {
    project: string;
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
          epicGroups: {},
          planItems: [],
          projectItems: [],
        };
      }
      const group = projectMap[proj];
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

  onMount(async () => {
    isMounted = true;
    activeDetailPlanId = requestedReportPlan();
    unsubscribeEvents = apiClient.subscribeEvents((event, data) => {
      handleEvent(event, data);
    });
    await loadReports();
  });

  onDestroy(() => {
    isMounted = false;
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  function handleEvent(event: string, data: unknown) {
    if (!isMounted) return;
    if (event === 'report.sent') {
      const newReport = data as ReportItem;
      if (newReport && newReport.id) {
        if (!items.some((i) => i.id === newReport.id)) {
          items = [newReport, ...items];
          unreadCount++;
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

  async function openDetail(item: ReportItem) {
    // Mark report read first as per specification
    void markRead(item);

    const detailLink = item.detail_link || item.detail;
    if (!detailLink) return;

    // Check if detail links to a plan completion report
    const match = /plans\/([^/]+)\/completion/.exec(detailLink);
    if (match?.[1]) {
      activeDetailPlanId = match[1];
      return;
    }

    const planReportMatch = /plan report\s+([^\s]+)/.exec(detailLink);
    if (planReportMatch?.[1]) {
      activeDetailPlanId = planReportMatch[1];
      return;
    }

    // If it is another plan reference
    if (item.from?.plan) {
      activeDetailPlanId = item.from.plan;
      return;
    }
  }

  function formatTime(item: ReportItem): string {
    const createdStr = item.created || item.created_at;
    const isSentLater =
      (item.sent && item.created && item.sent !== item.created) ||
      (item.sent_at && item.created_at && item.sent_at !== item.created_at);

    if (isSentLater) {
      const sentStr = item.sent || item.sent_at;
      return `${createdStr} · ${$t('report.list.sent_at', { time: sentStr })}`;
    }
    return createdStr;
  }
</script>

<div class="reports-view">
  {#if activeDetailPlanId}
    <CompletionReportView planId={activeDetailPlanId} onClose={closeDetail} />
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
                <span class="project-glyph">▪</span>
                <span class="project-title">{pGroup.project}</span>
              </h2>

              <div class="project-content">
                <!-- Epic groups -->
                {#each Object.entries(pGroup.epicGroups) as [epicName, epicItems]}
                  <div class="epic-section">
                    <h3 class="epic-header">
                      <span class="epic-glyph">◇</span>
                      <span class="epic-title">{epicName}</span>
                    </h3>
                    <div class="items-list">
                      {#each epicItems as item}
                        <div class="report-card">
                          <div class="report-card__header">
                            <span class="item-glyph">▸</span>
                            {#if item.from?.plan}
                              <span class="item-target mono font-medium">{item.from.plan}</span>
                            {/if}
                            <span class="item-subject font-semibold">{item.subject}</span>
                            <span class="item-time text-muted">{formatTime(item)}</span>
                          </div>

                          {#if item.body || item.summary}
                            <div class="report-card__body text-muted">
                              {item.body || item.summary}
                            </div>
                          {/if}

                          <div class="report-card__actions">
                            {#if item.detail_link || item.detail}
                              <button
                                class="action-btn action-btn--detail"
                                onclick={() => openDetail(item)}
                              >
                                {$t('report.action.details')}
                              </button>
                            {/if}
                            <button
                              class="action-btn action-btn--read"
                              onclick={() => markRead(item)}
                            >
                              {$t('report.action.mark_read')}
                            </button>
                          </div>
                        </div>
                      {/each}
                    </div>
                  </div>
                {/each}

                <!-- Plan items under project without epic -->
                {#if pGroup.planItems.length > 0}
                  <div class="items-list">
                    {#each pGroup.planItems as item}
                      <div class="report-card">
                        <div class="report-card__header">
                          <span class="item-glyph">▸</span>
                          <span class="item-target mono font-medium">{item.from?.plan}</span>
                          <span class="item-subject font-semibold">{item.subject}</span>
                          <span class="item-time text-muted">{formatTime(item)}</span>
                        </div>

                        {#if item.body || item.summary}
                          <div class="report-card__body text-muted">
                            {item.body || item.summary}
                          </div>
                        {/if}

                        <div class="report-card__actions">
                          {#if item.detail_link || item.detail}
                            <button
                              class="action-btn action-btn--detail"
                              onclick={() => openDetail(item)}
                            >
                              {$t('report.action.details')}
                            </button>
                          {/if}
                          <button
                            class="action-btn action-btn--read"
                            onclick={() => markRead(item)}
                          >
                            {$t('report.action.mark_read')}
                          </button>
                        </div>
                      </div>
                    {/each}
                  </div>
                {/if}

                <!-- Project-level items -->
                {#if pGroup.projectItems.length > 0}
                  <div class="items-list">
                    {#each pGroup.projectItems as item}
                      <div class="report-card">
                        <div class="report-card__header">
                          <span class="item-glyph">▪</span>
                          <span class="item-target mono font-medium">
                            {$t('report.list.project')}
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
                          {#if item.detail_link || item.detail}
                            <button
                              class="action-btn action-btn--detail"
                              onclick={() => openDetail(item)}
                            >
                              {$t('report.action.details')}
                            </button>
                          {/if}
                          <button
                            class="action-btn action-btn--read"
                            onclick={() => markRead(item)}
                          >
                            {$t('report.action.mark_read')}
                          </button>
                        </div>
                      </div>
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

  .item-target {
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
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
