<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import CheckField from '../components/CheckField.svelte';
  import ConfirmDialog from '../components/ConfirmDialog.svelte';
  import KindMark, { type Kind } from '../components/KindMark.svelte';
  import Link from '../components/Link.svelte';
  import Tabs from '../components/Tabs.svelte';
  import { actFailureText, errorCodeOf } from '../api/errors';
  import {
    apiClient,
    type CleanupJob,
    type CleanupItemEntry,
    type CleanupKeptItem,
  } from '../api/client';
  import { navigateTo, runtimeStore } from '../stores/runtime';
  import { formatBytes } from '../display/format';
  import { displayState } from '../display';

  let job = $state<CleanupJob | null>(null);
  let keptItems = $state<CleanupKeptItem[]>([]);
  let isLoading = $state(true);
  /** The code of a failed read, `null` while none failed and empty when no code could be read. */
  let loadError = $state<string | null>(null);
  /** Whether a cleanup read has been served at least once, so a later failure is an arrival. */
  let hasShownCleanup = $state(false);
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
    loadError = null;
    try {
      const [cleanupJob, keptRes] = await Promise.all([
        // A job the runtime has none of is an answer, not a failure; every other refusal is the
        // load's own and reaches the error pattern below.
        apiClient.getCleanupJob().catch((err: unknown) => {
          if ((err as Error & { status?: number })?.status === 404) return null;
          throw err;
        }),
        // The kept list is a soft read on purpose: a failure here must not hide the delete list
        // that the person came for, so the kept tab shows none rather than an error.
        apiClient.getKeptCleanup().catch(() => ({ items: [] })),
      ]);
      job = cleanupJob;
      keptItems = keptRes?.items || [];
      hasShownCleanup = true;
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
    } catch (err: unknown) {
      loadError = errorCodeOf(err);
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
      actionError = actFailureText(err, $t('system.cleanup.failed'));
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
      actionError = actFailureText(err, $t('system.cleanup.postpone_failed'));
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
      actionError = actFailureText(err, $t('system.cleanup.ignore_failed'));
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
      actionError = actFailureText(err, $t('system.cleanup.release_failed'));
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

  /** Checks every item of the job: the one act the details head offers over all projects. */
  function checkAllItems(): void {
    uncheckedIds = {};
    persistSelection();
  }

  /** Unchecks every item of the job, so _Delete_ refuses until one is checked again. */
  function uncheckAllItems(): void {
    const copy: Record<string, boolean> = {};
    for (const item of allItems) {
      copy[item.id] = true;
    }
    uncheckedIds = copy;
    persistSelection();
  }

  /**
   * @param item one item of the job
   * @returns whether _Delete_ would send it
   */
  function isItemChecked(item: CleanupItemEntry): boolean {
    return !uncheckedIds[item.id];
  }

  /**
   * @param item the item whose check changed
   * @param checked whether the item is checked now
   */
  function setItemChecked(item: CleanupItemEntry, checked: boolean): void {
    if (checked === isItemChecked(item)) return;
    const copy = { ...uncheckedIds };
    if (checked) {
      delete copy[item.id];
    } else {
      copy[item.id] = true;
    }
    uncheckedIds = copy;
    persistSelection();
  }

  /**
   * @param kind the kind of an item or a kept entry
   * @returns the kind mark the row carries, where the closed set names the kind
   */
  function kindOf(kind: string): Kind | null {
    switch (kind) {
      case 'plan':
        return 'plan';
      case 'epic':
        return 'epic';
      case 'unenrolled':
        return 'project';
      case 'report':
        return 'report';
      default:
        return null;
    }
  }

  /**
   * @param item one item of the job
   * @returns the detail line: what it is (unless its row carries a `KindMark`), when it ended and its
   *   own detail; the size stands in its own column
   */
  function itemDescription(item: CleanupItemEntry): string {
    const parts: string[] = [];
    if (!kindOf(item.kind)) {
      const kind = displayState('cleanup', item.kind, $t);
      if (kind) parts.push(kind);
    }
    if (item.ended_at) parts.push($t('system.cleanup.details.ended', { time: item.ended_at }));
    if (item.detail) parts.push(item.detail);
    return parts.join(' · ');
  }

  /** The tabs of the view, the kept list carrying its count. */
  let tabs = $derived([
    { id: 'job', label: $t('system.cleanup.title') },
    { id: 'kept', label: $t('system.cleanup.kept.title'), count: keptItems.length },
  ]);

  /** The checked items per kind, so the delete act names what it deletes from the catalogue. */
  let checkedKinds = $derived.by(() => {
    const counts = { plans: 0, epics: 0, logs: 0, reports: 0, projects: 0 };
    for (const item of checkedItems) {
      switch (item.kind) {
        case 'plan':
          counts.plans += 1;
          break;
        case 'epic':
          counts.epics += 1;
          break;
        case 'logs':
          counts.logs += 1;
          break;
        case 'report':
          counts.reports += 1;
          break;
        case 'unenrolled':
          counts.projects += 1;
          break;
      }
    }
    return counts;
  });

  /** What _Delete_ removes, in the catalogue's own words; empty while nothing is checked. */
  let deleteWhat = $derived.by(() => {
    const parts: string[] = [];
    if (checkedKinds.plans > 0) {
      parts.push($t('system.cleanup.summary.plans', { count: checkedKinds.plans }));
    }
    if (checkedKinds.epics > 0) {
      parts.push($t('system.cleanup.summary.epics', { count: checkedKinds.epics }));
    }
    if (checkedKinds.logs > 0) {
      parts.push($t('system.cleanup.summary.logs', { count: checkedKinds.logs }));
    }
    if (checkedKinds.reports > 0) {
      parts.push($t('system.cleanup.summary.read_reports', { count: checkedKinds.reports }));
    }
    if (checkedKinds.projects > 0) {
      parts.push($t('system.cleanup.summary.unenrolled', { count: checkedKinds.projects }));
    }
    return parts.join(' · ');
  });
</script>

<div class="cleanup-container" data-testid="cleanup-view">
  <div class="cleanup-header">
    <div class="cleanup-header__titles">
      <Link href="/system" label={$t('system.view.title')} />
      <h1 class="cleanup-title">{$t('system.cleanup.title')}</h1>
      {#if job?.due_since}
        <span class="text-muted text-sm">
          {$t('system.cleanup.due_since', { time: job.due_since })}
        </span>
      {/if}
    </div>

    <Tabs
      label={$t('system.cleanup.title')}
      active={activeTab}
      {tabs}
      onSelect={(id) => {
        activeTab = id as 'job' | 'kept';
      }}
    />
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
    <div class="cleanup-loading" data-testid="cleanup-loading">
      <Skeleton shape="card" rows={3} label={$t('system.cleanup.loading')} />
    </div>
  {:else if loadError !== null}
    <div class="notice-alert notice-alert--danger" data-testid="cleanup-error">
      <ErrorState
        code={loadError}
        actionLabel={$t('common.action.retry')}
        onAct={() => loadData()}
        titleLevel={2}
        announce={hasShownCleanup}
      />
    </div>
  {:else if activeTab === 'job'}
    {#if !job || allItems.length === 0}
      <div class="empty-state card">
        <h2>{$t('system.cleanup.none', { time: 'tomorrow 03:00' })}</h2>
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
            <span class="summary-freed__text">
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
            <Button variant="ghost" onclick={closePreview}>
              {$t('system.cleanup.preview.back')}
            </Button>
            <h2 id="heading-preview" class="section-title">{previewItem.title}</h2>
            {#if kindOf(previewItem.kind)}
              <KindMark kind={kindOf(previewItem.kind) as Kind} />
            {:else}
              <span class="text-muted text-sm"
                >{displayState('cleanup', previewItem.kind, $t)}</span
              >
            {/if}
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
          <div class="details-head">
            <h2 id="heading-details" class="section-title">{$t('system.cleanup.details.title')}</h2>
            <div class="details-head__acts">
              <Button variant="secondary" onclick={checkAllItems}>
                {$t('system.cleanup.details.check_all')}
              </Button>
              <Button variant="secondary" onclick={uncheckAllItems}>
                {$t('system.cleanup.details.check_none')}
              </Button>
            </div>
          </div>

          {#each job.projects as project}
            <div class="card project-card">
              <div class="project-header">
                <div class="project-name">
                  <h3>{project.display_name}</h3>
                  <span class="mono text-muted text-xs">{project.project_id}</span>
                </div>
              </div>

              <ul class="items-list">
                {#each project.items as item}
                  {@const isChecked = isItemChecked(item)}
                  <li
                    id={`cleanup-item-${item.id}`}
                    class="item-row"
                    class:item-row--excluded={!isChecked}
                    class:item-row--highlight={item.id === highlightedItemId}
                  >
                    <div class="item-check">
                      <CheckField
                        checked={isChecked}
                        label={item.title}
                        description={itemDescription(item)}
                        onchange={(checked) => setItemChecked(item, checked)}
                      />
                    </div>
                    <div class="item-kind">
                      {#if kindOf(item.kind)}
                        <KindMark kind={kindOf(item.kind) as Kind} wordHidden />
                      {/if}
                    </div>
                    <span class="item-size mono">{formatBytes(item.freed_bytes)}</span>
                    <Button
                      variant="ghost"
                      size="sm"
                      aria-label={$t('system.cleanup.preview.open', { item: item.title })}
                      onclick={() => openPreview(item)}
                    >
                      {$t('system.cleanup.details.open')}
                    </Button>
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
          <Button
            variant="primary"
            disabled={checkedItems.length === 0}
            disabledReason={checkedItems.length === 0
              ? $t('system.cleanup.action.nothing_checked')
              : undefined}
            onclick={openDeleteModal}
          >
            {$t('system.cleanup.action.delete', { what: deleteWhat })}
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
              <div class="item-check">
                <CheckField
                  checked={Boolean(selectedKeptIds[k.item_id])}
                  label={k.title}
                  description={$t('system.cleanup.kept.why', {
                    how: k.why,
                    time: k.kept_at,
                    who: k.who,
                  })}
                  onchange={() => toggleKeptItem(k.item_id)}
                />
              </div>
              <div class="item-kind">
                {#if kindOf(k.kind)}
                  <KindMark kind={kindOf(k.kind) as Kind} wordHidden />
                {:else}
                  <span class="text-muted text-xs">{displayState('cleanup', k.kind, $t)}</span>
                {/if}
              </div>
            </li>
          {/each}
        </ul>
      {/if}
    </section>
  {/if}
</div>

<!-- Delete Confirmation Dialogue -->
<ConfirmDialog
  open={isDeleteModalOpen}
  title={$t('system.cleanup.confirm.title', { what: deleteWhat })}
  consequences={[
    $t('system.cleanup.confirm.frees', { size: formatBytes(selectedFreedBytes) }),
    $t('system.cleanup.confirm.stays'),
    ...(uncheckedCount > 0 ? [$t('system.cleanup.confirm.kept', { count: uncheckedCount })] : []),
  ]}
  actionLabel={$t('system.cleanup.confirm.do')}
  cancelLabel={$t('system.cleanup.confirm.back')}
  busy={isDeleting}
  testid="cleanup-confirm"
  onConfirm={confirmDelete}
  onClose={closeDeleteModal}
/>

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

  .cleanup-title {
    font-size: var(--text-2xl);
    font-weight: 700;
    margin: 0;
    color: var(--color-text-primary);
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

  .summary-freed__text {
    font-weight: 700;
    color: var(--color-success-text);
  }

  .details-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-3);
    margin-bottom: var(--space-4);
  }

  .details-head__acts {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
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

  .items-list {
    list-style: none;
    padding: 0;
    margin: 0;
  }

  .item-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-2) var(--space-5);
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

  .item-check {
    flex: 1;
    min-inline-size: 0;
  }

  .item-kind {
    display: inline-flex;
    align-items: center;
    min-inline-size: 1.5rem;
    justify-content: flex-end;
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
