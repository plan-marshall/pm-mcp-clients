<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import {
    apiClient,
    type CleanupJob,
    type CleanupItemEntry,
    type CleanupKeptItem,
  } from '../api/client';
  import { navigateTo, runtimeStore } from '../stores/runtime';
  import { formatBytes } from '../utils/tokenFormat';
  import { displayState } from '../display';

  let job = $state<CleanupJob | null>(null);
  let keptItems = $state<CleanupKeptItem[]>([]);
  let isLoading = $state(true);
  let loadError = $state('');
  let actionError = $state('');
  let actionSuccess = $state('');

  // Unchecked items (checked items are the ones to be deleted; unchecking excludes them and keeps them for good)
  let uncheckedIds = $state<Record<string, boolean>>({});

  // The item whose read-only preview is open, kept on the route so going back keeps the checks.
  let previewItemId = $state<string | null>(null);

  // Active view tab: 'job' or 'kept'
  let activeTab = $state<'job' | 'kept'>('job');

  // Confirmation modal state
  let isDeleteModalOpen = $state(false);
  let isDeleting = $state(false);

  // Selected kept items for release
  let selectedKeptIds = $state<Record<string, boolean>>({});

  onMount(() => {
    void loadData();
  });

  /**
   * Keeps the selection on the runtime store, so a preview or another view returns to it.
   */
  function persistSelection(): void {
    runtimeStore.update((s) => ({
      ...s,
      cleanupSelectionJobId: job?.job_id ?? null,
      cleanupUncheckedIds: { ...uncheckedIds },
    }));
  }

  /** Clears the checks and the preview a job held, so a later job starts clean. */
  function clearRememberedSelection(): void {
    uncheckedIds = {};
    previewItemId = null;
    runtimeStore.update((s) => ({
      ...s,
      cleanupSelectionJobId: null,
      cleanupUncheckedIds: {},
      cleanupPreviewItemId: null,
    }));
  }

  /**
   * Opens an item's read-only preview without deciding anything about it.
   *
   * @param item the item to inspect
   */
  function openPreview(item: CleanupItemEntry): void {
    previewItemId = item.id;
    runtimeStore.update((s) => ({ ...s, cleanupPreviewItemId: item.id }));
  }

  /** Closes the preview and returns to the job with its checks as they were. */
  function closePreview(): void {
    previewItemId = null;
    runtimeStore.update((s) => ({ ...s, cleanupPreviewItemId: null }));
  }

  async function loadData() {
    isLoading = true;
    loadError = '';
    try {
      const [cleanupJob, keptRes] = await Promise.all([
        apiClient.getCleanupJob().catch(() => null),
        apiClient.getKeptCleanup().catch(() => ({ items: [] })),
      ]);
      job = cleanupJob;
      keptItems = keptRes?.items || [];
      const remembered = get(runtimeStore);
      if (job && remembered.cleanupSelectionJobId === job.job_id) {
        uncheckedIds = { ...(remembered.cleanupUncheckedIds ?? {}) };
        previewItemId = remembered.cleanupPreviewItemId ?? null;
      } else {
        clearRememberedSelection();
        if (job) {
          runtimeStore.update((s) => ({ ...s, cleanupSelectionJobId: cleanupJob?.job_id ?? null }));
        }
      }
    } catch {
      loadError = $t('system.cleanup.load_failed');
    } finally {
      isLoading = false;
    }
  }

  // Count checked and unchecked
  let allItems = $derived.by(() => {
    const list: CleanupItemEntry[] = [];
    if (job?.projects) {
      for (const p of job.projects) {
        list.push(...p.items);
      }
    }
    return list;
  });

  let checkedItems = $derived.by(() => {
    return allItems.filter((item) => !uncheckedIds[item.id]);
  });

  let previewItem = $derived.by<CleanupItemEntry | null>(() => {
    if (!previewItemId) return null;
    return allItems.find((item) => item.id === previewItemId) ?? null;
  });

  /** The item a report's detail asked this visit to highlight, where it named one. */
  let highlightedItemId = $derived($runtimeStore.cleanupHighlightItemId ?? null);

  $effect(() => {
    const id = highlightedItemId;
    if (!id || !job || typeof document === 'undefined') return;
    const target =
      document.getElementById(`cleanup-item-${id}`) ?? document.getElementById('cleanup-summary');
    target?.scrollIntoView?.({ block: 'center' });
  });

  let uncheckedCount = $derived.by(() => {
    return allItems.length - checkedItems.length;
  });

  let selectedFreedBytes = $derived.by(() => {
    return checkedItems.reduce((acc, item) => acc + (item.freed_bytes || 0), 0);
  });

  function toggleItem(id: string) {
    if (uncheckedIds[id]) {
      const copy = { ...uncheckedIds };
      delete copy[id];
      uncheckedIds = copy;
    } else {
      uncheckedIds = { ...uncheckedIds, [id]: true };
    }
    persistSelection();
  }

  function checkAllForProject(items: CleanupItemEntry[]) {
    const copy = { ...uncheckedIds };
    for (const item of items) {
      delete copy[item.id];
    }
    uncheckedIds = copy;
    persistSelection();
  }

  function uncheckAllForProject(items: CleanupItemEntry[]) {
    const copy = { ...uncheckedIds };
    for (const item of items) {
      copy[item.id] = true;
    }
    uncheckedIds = copy;
    persistSelection();
  }

  function openDeleteModal() {
    actionError = '';
    isDeleteModalOpen = true;
  }

  function closeDeleteModal() {
    isDeleteModalOpen = false;
  }

  async function confirmDelete() {
    if (!job) return;
    isDeleting = true;
    actionError = '';
    try {
      const excluded = Object.keys(uncheckedIds);
      const res = await apiClient.executeCleanup(job.job_id, excluded);
      actionSuccess = $t('system.cleanup.deleted', {
        what: `${checkedItems.length} items`,
        size: formatBytes(res.freed_bytes),
      });
      closeDeleteModal();
      uncheckedIds = {};
      previewItemId = null;
      runtimeStore.update((s) => ({
        ...s,
        cleanupUncheckedIds: {},
        cleanupPreviewItemId: null,
        cleanupHighlightItemId: null,
      }));
      void loadData();
    } catch (err: unknown) {
      actionError = (err as Error)?.message || $t('system.cleanup.failed');
    } finally {
      isDeleting = false;
    }
  }

  async function handleRemind() {
    if (!job) return;
    try {
      await apiClient.remindCleanup(job.job_id);
      clearRememberedSelection();
      actionSuccess = $t('system.cleanup.reminded', { date: 'in 3 days' });
      setTimeout(() => navigateTo('system'), 1500);
    } catch (err: unknown) {
      actionError = (err as Error)?.message || $t('system.cleanup.postpone_failed');
    }
  }

  async function handleIgnore() {
    if (!job) return;
    try {
      await apiClient.ignoreCleanup(job.job_id);
      clearRememberedSelection();
      actionSuccess = $t('system.cleanup.ignored', { count: allItems.length });
      setTimeout(() => navigateTo('system'), 1500);
    } catch (err: unknown) {
      actionError = (err as Error)?.message || $t('system.cleanup.ignore_failed');
    }
  }

  async function handleReleaseKept(all = false) {
    try {
      actionError = '';
      const ids = all ? [] : Object.keys(selectedKeptIds);
      const res = await apiClient.releaseKeptCleanup(ids, all);
      actionSuccess = $t('system.cleanup.kept.released', {
        count: res.released_count || (all ? keptItems.length : ids.length),
      });
      selectedKeptIds = {};
      void loadData();
    } catch (err: unknown) {
      actionError = (err as Error)?.message || $t('system.cleanup.release_failed');
    }
  }

  function toggleKeptItem(id: string) {
    if (selectedKeptIds[id]) {
      const copy = { ...selectedKeptIds };
      delete copy[id];
      selectedKeptIds = copy;
    } else {
      selectedKeptIds = { ...selectedKeptIds, [id]: true };
    }
  }
</script>

<div class="cleanup-container" data-testid="cleanup-view">
  <div class="cleanup-header">
    <div class="cleanup-header__titles">
      <button type="button" class="back-link" onclick={() => navigateTo('system')}>
        ← {$t('system.view.title')}
      </button>
      <h1 class="cleanup-title">{$t('system.cleanup.title')}</h1>
      {#if job?.due_since}
        <span class="text-muted text-sm">
          {$t('system.cleanup.due_since', { time: job.due_since })}
        </span>
      {/if}
    </div>

    <div class="cleanup-tabs" role="tablist">
      <button
        type="button"
        class="tab-btn"
        class:tab-btn--active={activeTab === 'job'}
        onclick={() => {
          activeTab = 'job';
        }}
        role="tab"
        aria-selected={activeTab === 'job'}
      >
        {$t('system.cleanup.title')}
      </button>
      <button
        type="button"
        class="tab-btn"
        class:tab-btn--active={activeTab === 'kept'}
        onclick={() => {
          activeTab = 'kept';
        }}
        role="tab"
        aria-selected={activeTab === 'kept'}
      >
        {$t('system.cleanup.kept.title')} ({keptItems.length})
      </button>
    </div>
  </div>

  {#if actionSuccess}
    <div class="notice-alert notice-alert--success" role="status">
      {actionSuccess}
    </div>
  {/if}

  {#if actionError}
    <div class="notice-alert notice-alert--danger" role="alert">
      {actionError}
    </div>
  {/if}

  {#if isLoading}
    <div class="cleanup-loading">
      <Spinner size="2.5rem" label={$t('system.cleanup.loading')} />
    </div>
  {:else if loadError}
    <div class="notice-alert notice-alert--danger" role="alert">
      <p>{loadError}</p>
      <Button variant="secondary" onclick={() => loadData()}>{$t('web.common.retry')}</Button>
    </div>
  {:else if activeTab === 'job'}
    {#if !job || allItems.length === 0}
      <div class="empty-state card">
        <h2>{$t('system.cleanup.none', { time: 'tomorrow 03:00' })}</h2>
        <Button variant="secondary" onclick={() => navigateTo('system')}>
          {$t('system.cleanup.back_to_system')}
        </Button>
      </div>
    {:else}
      <!-- Summary Card -->
      <section
        id="cleanup-summary"
        class="card summary-card"
        class:summary-card--highlight={highlightedItemId}
        aria-labelledby="heading-summary"
      >
        <h2 id="heading-summary" class="card-title">{$t('system.cleanup.summary.title')}</h2>
        <div class="summary-content">
          <div class="summary-counts">
            <span>
              {$t('system.cleanup.summary.plans', { count: job.summary.plans_count })} ·
              {$t('system.cleanup.summary.epics', { count: job.summary.epics_count })} ·
              {$t('system.cleanup.summary.logs', { count: job.summary.logs_count })}
              {#if job.summary.read_reports_count}
                ·
                {$t('system.cleanup.summary.read_reports', {
                  count: job.summary.read_reports_count,
                })}
              {/if}
              {#if job.summary.unenrolled_projects_count}
                ·
                {$t('system.cleanup.summary.unenrolled', {
                  count: job.summary.unenrolled_projects_count,
                })}
              {/if}
            </span>
          </div>
          <div class="summary-freed">
            <span class="freed-badge">
              {$t('system.cleanup.summary.frees', { size: formatBytes(selectedFreedBytes) })}
            </span>
          </div>
        </div>
      </section>

      <!-- Details per project -->
      {#if previewItem}
        <section
          class="card preview-card"
          aria-labelledby="heading-preview"
          data-testid="cleanup-preview"
        >
          <div class="preview-head">
            <button type="button" class="btn-link" onclick={closePreview}>
              ← {$t('system.cleanup.preview.back')}
            </button>
            <h2 id="heading-preview" class="section-title">{previewItem.title}</h2>
            <span class="badge badge--kind">{displayState('cleanup', previewItem.kind)}</span>
          </div>
          <p class="text-muted text-sm">{$t('system.cleanup.preview.readonly')}</p>
          <dl class="preview-facts">
            {#if previewItem.ended_at}
              <dt>{$t('system.cleanup.details.ended', { time: previewItem.ended_at })}</dt>
              <dd></dd>
            {/if}
            {#if previewItem.detail}
              <dt>{$t('system.cleanup.preview.detail')}</dt>
              <dd>{previewItem.detail}</dd>
            {/if}
            <dt>{$t('system.cleanup.preview.size')}</dt>
            <dd class="mono">{formatBytes(previewItem.freed_bytes)}</dd>
          </dl>
          {#if !previewItem.detail}
            <p class="text-muted text-sm">{$t('system.cleanup.preview.empty')}</p>
          {/if}
        </section>
      {:else}
        <section class="projects-section" aria-labelledby="heading-details">
          <h2 id="heading-details" class="section-title">{$t('system.cleanup.details.title')}</h2>

          {#each job.projects as project}
            <div class="card project-card">
              <div class="project-header">
                <div class="project-name">
                  <h3>{project.display_name}</h3>
                  <span class="mono text-muted text-xs">{project.project_id}</span>
                </div>
                <div class="project-select-actions">
                  <button
                    type="button"
                    class="btn-link"
                    onclick={() => checkAllForProject(project.items)}
                  >
                    {$t('system.cleanup.details.check_all')}
                  </button>
                  <button
                    type="button"
                    class="btn-link"
                    onclick={() => uncheckAllForProject(project.items)}
                  >
                    {$t('system.cleanup.details.check_none')}
                  </button>
                </div>
              </div>

              <ul class="items-list">
                {#each project.items as item}
                  {@const isChecked = !uncheckedIds[item.id]}
                  <li
                    id={`cleanup-item-${item.id}`}
                    class="item-row"
                    class:item-row--excluded={!isChecked}
                    class:item-row--highlight={item.id === highlightedItemId}
                  >
                    <label class="item-checkbox-label">
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onchange={() => toggleItem(item.id)}
                      />
                      <div class="item-info">
                        <span class="item-title">{item.title}</span>
                        <div class="item-meta">
                          <span class="badge badge--kind">{displayState('cleanup', item.kind)}</span>
                          {#if item.ended_at}
                            <span class="text-muted text-xs">
                              {$t('system.cleanup.details.ended', { time: item.ended_at })}
                            </span>
                          {/if}
                          {#if item.detail}
                            <span class="text-muted text-xs">· {item.detail}</span>
                          {/if}
                        </div>
                      </div>
                    </label>
                    <span class="item-size mono">{formatBytes(item.freed_bytes)}</span>
                    <button
                      type="button"
                      class="btn-link"
                      aria-label={$t('system.cleanup.preview.open', { item: item.title })}
                      onclick={() => openPreview(item)}
                    >
                      {$t('system.cleanup.details.open')}
                    </button>
                  </li>
                {/each}
              </ul>
            </div>
          {/each}

          <p class="unchecked-note text-muted text-sm">
            {$t('system.cleanup.details.unchecked_note')}
          </p>
        </section>
      {/if}

      <!-- Action Bar -->
      <div class="cleanup-actions-bar">
        <div class="actions-left">
          <Button variant="secondary" onclick={handleRemind}>
            {$t('system.cleanup.action.remind', { days: 3 })}
          </Button>
          <Button variant="secondary" onclick={handleIgnore}>
            {$t('system.cleanup.action.ignore')}
          </Button>
        </div>

        <div class="actions-right">
          <Button variant="danger" disabled={checkedItems.length === 0} onclick={openDeleteModal}>
            {$t('system.cleanup.action.delete', { what: `${checkedItems.length} items` })}
          </Button>
        </div>
      </div>
    {/if}
  {:else if activeTab === 'kept'}
    <!-- Kept for good tab -->
    <section class="card" aria-labelledby="heading-kept">
      <div class="card-header">
        <h2 id="heading-kept" class="card-title">{$t('system.cleanup.kept.title')}</h2>
        {#if keptItems.length > 0}
          <div class="card-actions">
            <Button
              variant="secondary"
              disabled={Object.keys(selectedKeptIds).length === 0}
              onclick={() => handleReleaseKept(false)}
            >
              {$t('system.cleanup.kept.release', { count: Object.keys(selectedKeptIds).length })}
            </Button>
            <Button variant="secondary" onclick={() => handleReleaseKept(true)}>
              {$t('system.cleanup.kept.release_all')}
            </Button>
          </div>
        {/if}
      </div>

      {#if keptItems.length === 0}
        <p class="text-muted text-center cleanup-kept-empty">
          {$t('system.cleanup.kept.none')}
        </p>
      {:else}
        <ul class="items-list">
          {#each keptItems as k}
            <li class="item-row">
              <label class="item-checkbox-label">
                <input
                  type="checkbox"
                  checked={Boolean(selectedKeptIds[k.item_id])}
                  onchange={() => toggleKeptItem(k.item_id)}
                />
                <div class="item-info">
                  <span class="item-title">{k.title}</span>
                  <span class="text-muted text-xs">
                    {$t('system.cleanup.kept.why', { how: k.why, time: k.kept_at, who: k.who })}
                  </span>
                </div>
              </label>
              <span class="badge">{displayState('cleanup', k.kind)}</span>
            </li>
          {/each}
        </ul>
      {/if}
    </section>
  {/if}
</div>

<!-- Delete Confirmation Modal -->
{#if isDeleteModalOpen}
  <div class="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="modal-del-title">
    <div class="modal-card">
      <h2 id="modal-del-title" class="modal-title">
        {$t('system.cleanup.confirm.title', { what: `${checkedItems.length} items` })}
      </h2>

      <div class="modal-body">
        <p class="text-danger font-bold">
          {$t('system.cleanup.confirm.frees', { size: formatBytes(selectedFreedBytes) })}
        </p>
        <p class="text-muted text-sm">
          {$t('system.cleanup.confirm.stays')}
        </p>

        {#if uncheckedCount > 0}
          <p class="unchecked-warning text-sm">
            {$t('system.cleanup.confirm.kept', { count: uncheckedCount })}
          </p>
        {/if}
      </div>

      <div class="modal-footer">
        <Button variant="secondary" onclick={closeDeleteModal}>
          {$t('system.cleanup.confirm.back')}
        </Button>
        <Button variant="danger" loading={isDeleting} onclick={confirmDelete}>
          {$t('system.cleanup.confirm.do')}
        </Button>
      </div>
    </div>
  </div>
{/if}

<style>
  .cleanup-container {
    padding: var(--space-6);
    max-width: 1000px;
    margin: 0 auto;
    width: 100%;
  }

  .cleanup-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    flex-wrap: wrap;
    gap: var(--space-4);
    margin-bottom: var(--space-6);
  }

  .back-link {
    background: none;
    border: none;
    color: var(--color-primary);
    font-size: var(--text-sm);
    font-weight: 500;
    cursor: pointer;
    padding: 0;
    margin-bottom: var(--space-2);
  }

  .cleanup-title {
    font-size: var(--text-2xl);
    font-weight: 700;
    margin: 0;
    color: var(--color-text-primary);
  }

  .cleanup-tabs {
    display: flex;
    border-bottom: 2px solid var(--color-border-subtle);
  }

  .tab-btn {
    padding: var(--space-2) var(--space-4);
    background: transparent;
    border: none;
    border-bottom: 2px solid transparent;
    margin-bottom: -2px;
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-secondary);
    cursor: pointer;
    transition: all 0.15s;
  }

  .tab-btn--active {
    color: var(--color-primary);
    border-bottom-color: var(--color-primary);
  }

  .card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-5);
    margin-bottom: var(--space-6);
  }

  .summary-card {
    background-color: var(--color-bg-subtle);
  }

  .summary-card--highlight,
  .item-row--highlight {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }

  .preview-card {
    margin-bottom: var(--space-6);
  }

  .preview-head {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    flex-wrap: wrap;
    margin-bottom: var(--space-2);
  }

  .preview-facts {
    display: grid;
    grid-template-columns: auto 1fr;
    gap: var(--space-1) var(--space-4);
    margin: var(--space-3) 0 0 0;
    font-size: var(--text-sm);
  }

  .preview-facts dt {
    color: var(--color-text-muted);
  }

  .preview-facts dd {
    margin: 0;
  }

  .card-title {
    font-size: var(--text-lg);
    font-weight: 600;
    margin: 0 0 var(--space-3) 0;
  }

  .summary-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-4);
  }

  .freed-badge {
    display: inline-block;
    padding: var(--space-1) var(--space-3);
    border-radius: var(--radius-full);
    background-color: var(--color-success-subtle);
    color: var(--color-success-text);
    font-weight: 700;
    font-size: var(--text-base);
  }

  .section-title {
    font-size: var(--text-lg);
    font-weight: 600;
    margin-bottom: var(--space-4);
  }

  .project-card {
    padding: 0;
    overflow: hidden;
  }

  .project-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-4) var(--space-5);
    background-color: var(--color-bg-subtle);
    border-bottom: 1px solid var(--color-border);
  }

  .project-name h3 {
    margin: 0;
    font-size: var(--text-base);
    font-weight: 600;
  }

  .btn-link {
    background: none;
    border: none;
    color: var(--color-primary);
    font-size: var(--text-xs);
    font-weight: 600;
    cursor: pointer;
    margin-left: var(--space-3);
  }

  .items-list {
    list-style: none;
    padding: 0;
    margin: 0;
  }

  .item-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-3) var(--space-5);
    border-bottom: 1px solid var(--color-border-subtle);
    transition: background-color 0.15s;
  }

  .item-row:last-child {
    border-bottom: none;
  }

  .item-row--excluded {
    opacity: 0.6;
    background-color: var(--color-bg-subtle);
  }

  .item-checkbox-label {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    cursor: pointer;
    flex: 1;
  }

  .item-info {
    display: flex;
    flex-direction: column;
    gap: 0.1rem;
  }

  .item-title {
    font-weight: 500;
    font-size: var(--text-sm);
  }

  .item-meta {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .badge {
    padding: 0.15rem 0.4rem;
    border-radius: var(--radius-sm);
    font-size: 0.7rem;
    font-weight: 600;
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
  }

  .badge--kind {
    text-transform: uppercase;
  }

  .cleanup-actions-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-4);
    margin-top: var(--space-6);
    padding-top: var(--space-4);
    border-top: 1px solid var(--color-border);
  }

  .actions-left,
  .actions-right {
    display: flex;
    gap: var(--space-3);
  }

  .unchecked-note {
    margin-top: var(--space-2);
    font-style: italic;
  }

  .unchecked-warning {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning);
    padding: var(--space-2);
    border-radius: var(--radius-sm);
    font-weight: 500;
  }

  .notice-alert {
    padding: var(--space-3) var(--space-4);
    border-radius: var(--radius-md);
    margin-bottom: var(--space-4);
    font-size: var(--text-sm);
    font-weight: 500;
  }

  .notice-alert--success {
    background-color: var(--color-success-subtle);
    color: var(--color-success-text);
  }

  .notice-alert--danger {
    background-color: var(--color-danger-subtle);
    color: var(--color-danger);
  }

  /* Modal */
  .modal-backdrop {
    position: fixed;
    inset: 0;
    background-color: var(--color-overlay);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: var(--space-4);
    z-index: 100;
  }

  .modal-card {
    background-color: var(--color-bg-surface);
    border-radius: var(--radius-lg);
    max-width: 500px;
    width: 100%;
    padding: var(--space-6);
    box-shadow: var(--shadow-md);
  }

  .modal-title {
    font-size: var(--text-xl);
    font-weight: 700;
    margin-top: 0;
    margin-bottom: var(--space-4);
  }

  .modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-3);
    margin-top: var(--space-6);
  }

  .cleanup-kept-empty {
    padding: var(--space-4);
  }

  @media (max-width: 640px) {
    .cleanup-container {
      padding: var(--space-3);
    }
    .cleanup-actions-bar {
      flex-direction: column;
      align-items: stretch;
    }
    .actions-left,
    .actions-right {
      flex-direction: column;
    }
  }
</style>
