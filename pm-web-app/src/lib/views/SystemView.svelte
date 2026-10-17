<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import {
    apiClient,
    type SystemSnapshot,
    type WorkerMetric,
    type ModelMetric,
  } from '../api/client';
  import { runtimeStore, navigateTo } from '../stores/runtime';
  import { formatTokens, formatSplit, formatBytes } from '../utils/tokenFormat';

  let snapshot = $state<SystemSnapshot | null>(null);
  let isLoading = $state(true);
  let loadError = $state('');
  let actionError = $state('');
  let actionSuccess = $state('');

  // Selected filters
  let selectedWindow = $state<'hour' | 'today' | '7d'>('hour');
  let selectedProject = $state<string>('');
  let selectedGroupBy = $state<'model' | 'role'>('model');

  // Confirmation modal state for lifecycle actions
  let activeModal = $state<'stop' | 'restart' | 'upgrade' | null>(null);
  let modalWhen = $state<'drain' | 'now'>('drain');
  let isExecutingAction = $state(false);

  let unsubscribeEvents: (() => void) | null = null;
  let isMounted = true;

  onMount(() => {
    isMounted = true;
    unsubscribeEvents = apiClient.subscribeEvents((event) => {
      if (
        event === 'system.metrics' ||
        event === 'runtime.draining' ||
        event === 'drain.withdrawn'
      ) {
        void loadSnapshot(false);
      }
    });
    void loadSnapshot(true);
  });

  onDestroy(() => {
    isMounted = false;
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  async function loadSnapshot(showSpinner = false) {
    if (showSpinner) isLoading = true;
    loadError = '';
    try {
      const snap = await apiClient.getSystemSnapshot(selectedWindow, selectedProject || undefined);
      if (!isMounted) return;
      snapshot = snap;
    } catch (err: unknown) {
      if (!isMounted) return;
      const msg = (err as Error)?.message;
      if (msg === 'network_offline') {
        runtimeStore.update((s) => ({ ...s, isOffline: true }));
      } else {
        loadError = 'Failed to load system snapshot.';
      }
    } finally {
      if (isMounted) isLoading = false;
    }
  }

  function handleWindowChange(win: 'hour' | 'today' | '7d') {
    selectedWindow = win;
    void loadSnapshot(true);
  }

  function handleProjectChange(prj: string) {
    selectedProject = prj;
    void loadSnapshot(true);
  }

  function openModal(action: 'stop' | 'restart' | 'upgrade') {
    activeModal = action;
    modalWhen = 'drain';
    actionError = '';
    actionSuccess = '';
  }

  function closeModal() {
    activeModal = null;
    actionError = '';
  }

  async function executeLifecycleAction() {
    if (!activeModal) return;
    isExecutingAction = true;
    actionError = '';
    try {
      if (activeModal === 'stop') {
        await apiClient.stopRuntime(modalWhen);
        if (modalWhen === 'drain') {
          runtimeStore.update((s) => ({ ...s, isDraining: true }));
        } else {
          runtimeStore.update((s) => ({ ...s, isStopped: true }));
        }
      } else if (activeModal === 'restart') {
        await apiClient.stopRuntime(modalWhen, 'restart');
      } else if (activeModal === 'upgrade') {
        await apiClient.upgradeRuntime(
          snapshot?.runtime.upgrade_available_version || undefined,
          modalWhen,
        );
      }
      closeModal();
      void loadSnapshot(false);
    } catch (err: unknown) {
      actionError = (err as Error)?.message || 'Action failed';
    } finally {
      isExecutingAction = false;
    }
  }

  async function handleCheckUpgrade() {
    try {
      actionError = '';
      actionSuccess = '';
      await apiClient.checkUpgrade();
      void loadSnapshot(false);
      actionSuccess = 'Update check completed.';
    } catch (err: unknown) {
      actionError = (err as Error)?.message || 'Update check failed';
    }
  }

  function copyCommand(text: string) {
    if (navigator?.clipboard) {
      void navigator.clipboard.writeText(text);
      actionSuccess = 'Copied to clipboard.';
      setTimeout(() => {
        actionSuccess = '';
      }, 3000);
    }
  }

  // Derived filtered models
  let displayModels = $derived.by(() => {
    if (!snapshot?.models) return [];
    if (selectedGroupBy === 'role') {
      const byRole: Record<string, ModelMetric> = {};
      for (const m of snapshot.models) {
        const key = m.role || 'unknown';
        if (!byRole[key]) {
          byRole[key] = {
            id: key,
            role: key,
            calls_count: 0,
            tokens_total: 0,
            tokens_in: 0,
            tokens_out: 0,
            errors_count: 0,
            retries_count: 0,
            unmeasured_calls: 0,
            tokens_unmeasured: false,
          };
        }
        byRole[key].calls_count += m.calls_count;
        byRole[key].tokens_total += m.tokens_total;
        byRole[key].tokens_in += m.tokens_in;
        byRole[key].tokens_out += m.tokens_out;
        byRole[key].errors_count += m.errors_count;
        byRole[key].retries_count += m.retries_count;
        if (m.tokens_unmeasured) {
          byRole[key].tokens_unmeasured = true;
          byRole[key].unmeasured_calls =
            (byRole[key].unmeasured_calls || 0) + (m.unmeasured_calls || m.calls_count);
        }
      }
      return Object.values(byRole);
    }
    return snapshot.models;
  });
</script>

<div class="system-container" data-testid="system-view">
  <div class="system-header">
    <div class="system-header__titles">
      <h1 class="system-title">{$t('system.view.title')}</h1>
      {#if snapshot?.runtime}
        <span class="system-subtitle mono">
          pm-mcpd {snapshot.runtime.version}
        </span>
      {/if}
    </div>

    <div class="system-header__controls">
      <div class="window-selector" role="group" aria-label="Window selection">
        <button
          type="button"
          class="btn-toggle"
          class:btn-toggle--active={selectedWindow === 'hour'}
          onclick={() => handleWindowChange('hour')}
        >
          {$t('system.models.window', { window: 'hour' })}
        </button>
        <button
          type="button"
          class="btn-toggle"
          class:btn-toggle--active={selectedWindow === 'today'}
          onclick={() => handleWindowChange('today')}
        >
          {$t('system.models.window', { window: 'today' })}
        </button>
        <button
          type="button"
          class="btn-toggle"
          class:btn-toggle--active={selectedWindow === '7d'}
          onclick={() => handleWindowChange('7d')}
        >
          {$t('system.models.window', { window: 'week' })}
        </button>
      </div>

      <button
        type="button"
        class="refresh-btn"
        aria-label="Refresh system view"
        onclick={() => loadSnapshot(true)}
      >
        ↻
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

  {#if isLoading && !snapshot}
    <div class="system-loading">
      <Spinner size="2.5rem" label="Loading system dashboard…" />
    </div>
  {:else if loadError && !snapshot}
    <div class="notice-alert notice-alert--danger" role="alert">
      <p>{loadError}</p>
      <Button variant="secondary" onclick={() => loadSnapshot(true)}>Retry</Button>
    </div>
  {:else if $runtimeStore.isStopped}
    <div class="stopped-panel" role="region" aria-label="Stopped runtime">
      <h2 class="stopped-title">{$t('system.stopped.title', { who: 'operator', time: 'now' })}</h2>
      <p class="stopped-desc">
        {$t('system.stopped.how', { command: 'pm-operator runtime start' })}
      </p>
      <div class="copy-box">
        <code class="mono">pm-operator runtime start</code>
        <Button variant="secondary" onclick={() => copyCommand('pm-operator runtime start')}>
          {$t('system.stopped.copy')}
        </Button>
      </div>
      <div class="stopped-reconnect">
        <Button variant="primary" onclick={() => loadSnapshot(true)}>
          {$t('web.offline.reconnecting')}
        </Button>
      </div>
    </div>
  {:else if snapshot}
    <!-- Cleanup Banner if due items exist -->
    {#if snapshot.cleanup && (snapshot.cleanup.plans_count > 0 || snapshot.cleanup.epics_count > 0 || snapshot.cleanup.logs_count > 0)}
      <div class="cleanup-banner" role="region" aria-label="Cleanup banner">
        <div class="cleanup-banner__info">
          <span class="cleanup-banner__badge">🧹 {$t('system.cleanup.title')}</span>
          <span class="cleanup-banner__text">
            {$t('system.cleanup.line_due', {
              what: `${snapshot.cleanup.plans_count} plans · ${snapshot.cleanup.epics_count} epics · ${snapshot.cleanup.logs_count} logs`,
              size: formatBytes(snapshot.cleanup.freed_bytes),
            })}
          </span>
        </div>
        <Button variant="primary" onclick={() => navigateTo('cleanup')}>
          {$t('system.cleanup.review')}
        </Button>
      </div>
    {/if}

    <div class="system-grid">
      <!-- 1. Runtime Card -->
      <section class="card" aria-labelledby="heading-runtime">
        <div class="card-header">
          <h2 id="heading-runtime" class="card-title">{$t('system.runtime.title')}</h2>
          <div class="card-actions">
            <Button variant="secondary" onclick={() => openModal('stop')}>
              {$t('system.action.stop')}
            </Button>
            <Button variant="secondary" onclick={() => openModal('restart')}>
              {$t('system.action.restart')}
            </Button>
            {#if snapshot.runtime.upgrade_possible}
              <Button variant="primary" onclick={() => openModal('upgrade')}>
                {$t('system.action.upgrade')}
              </Button>
            {/if}
          </div>
        </div>

        <div class="metrics-list">
          <div class="metric-row">
            <span class="metric-label">Version</span>
            <span class="metric-value mono">
              pm-mcpd {snapshot.runtime.version}
              {#if snapshot.runtime.build_date}
                · build {snapshot.runtime.build_date}
              {/if}
              {#if snapshot.runtime.git_commit}
                ({snapshot.runtime.git_commit})
              {/if}
            </span>
          </div>

          {#if snapshot.runtime.upgrade_available_version}
            <div class="metric-row metric-row--highlight">
              <span class="metric-label">Update</span>
              <span class="metric-value">
                <strong
                  >{$t('system.runtime.available', {
                    version: snapshot.runtime.upgrade_available_version,
                  })}</strong
                >
                {#if snapshot.runtime.upgrade_release_notes_url}
                  <a
                    href={snapshot.runtime.upgrade_release_notes_url}
                    target="_blank"
                    rel="noreferrer"
                    class="link-muted"
                  >
                    Release notes
                  </a>
                {/if}
              </span>
            </div>
          {:else}
            <div class="metric-row">
              <span class="metric-label">Update</span>
              <span class="metric-value text-muted">
                {$t('system.runtime.not_checked')}
                <button type="button" class="btn-inline" onclick={handleCheckUpgrade}>
                  {$t('system.runtime.check_now')}
                </button>
              </span>
            </div>
          {/if}

          {#if snapshot.runtime.uptime_formatted}
            <div class="metric-row">
              <span class="metric-label">Uptime</span>
              <span class="metric-value">{snapshot.runtime.uptime_formatted}</span>
            </div>
          {/if}

          <div class="metric-row">
            <span class="metric-label">Memory</span>
            <span class="metric-value">
              {formatBytes(snapshot.runtime.memory_used_bytes || 0)} in use of {formatBytes(
                snapshot.runtime.memory_max_bytes || 0,
              )}
              {#if snapshot.runtime.memory_resident_bytes}
                · {formatBytes(snapshot.runtime.memory_resident_bytes)} resident
              {/if}
            </span>
          </div>

          <div class="metric-row">
            <span class="metric-label">CPU</span>
            <span class="metric-value">
              {#if snapshot.runtime.cpu_measured && snapshot.runtime.cpu_percent !== null}
                {snapshot.runtime.cpu_percent} %
              {:else}
                <span class="text-muted">{$t('system.figure.unavailable_reason')}</span>
              {/if}
            </span>
          </div>

          <div class="metric-row">
            <span class="metric-label">Store</span>
            <span class="metric-value">
              {formatBytes(snapshot.runtime.machine_store_bytes || 0)} · {formatBytes(
                snapshot.runtime.disk_free_bytes || 0,
              )} free on disk
              {#if snapshot.runtime.disk_measured_at}
                (measured {snapshot.runtime.disk_measured_at})
              {/if}
            </span>
          </div>

          <div class="metric-row">
            <span class="metric-label">Listeners</span>
            <span class="metric-value">
              socket · loopback {snapshot.runtime.loopback_address || 'off'}
              {#if snapshot.runtime.lan_address}
                · network {snapshot.runtime.lan_address} ({snapshot.runtime.lan_devices_count || 0} devices)
              {/if}
            </span>
          </div>
        </div>
      </section>

      <!-- 2. Work Card -->
      <section class="card" aria-labelledby="heading-work">
        <div class="card-header">
          <h2 id="heading-work" class="card-title">{$t('system.work.title')}</h2>
          {#if snapshot.work.recovering_projects && snapshot.work.recovering_projects.length > 0}
            <span class="badge badge--warning">
              recovering: {snapshot.work.recovering_projects.join(', ')}
            </span>
          {/if}
        </div>

        <div class="metrics-list">
          <div class="metric-row">
            <span class="metric-label">Plans</span>
            <div class="metric-value tags-row">
              {#if snapshot.work.plans_by_state && Object.keys(snapshot.work.plans_by_state).length > 0}
                {#each Object.entries(snapshot.work.plans_by_state) as [st, count]}
                  {#if count > 0}
                    <span class="status-tag status-tag--{st}">{count} {st.replace('_', ' ')}</span>
                  {/if}
                {/each}
              {:else}
                <span class="text-muted">{$t('system.work.none_running')}</span>
              {/if}
            </div>
          </div>

          <div class="metric-row">
            <span class="metric-label">Epics</span>
            <div class="metric-value tags-row">
              {#if snapshot.work.epics_by_state && Object.keys(snapshot.work.epics_by_state).length > 0}
                {#each Object.entries(snapshot.work.epics_by_state) as [st, count]}
                  {#if count > 0}
                    <span class="status-tag status-tag--{st}">{count} {st.replace('_', ' ')}</span>
                  {/if}
                {/each}
              {:else}
                <span class="text-muted">{$t('system.work.none_running')}</span>
              {/if}
            </div>
          </div>

          <div class="metric-row">
            <span class="metric-label">Tasks</span>
            <span class="metric-value">
              <strong>{snapshot.work.tasks_running || 0}</strong> running · {snapshot.work
                .tasks_queued || 0} queued
            </span>
          </div>

          <div class="metric-row">
            <span class="metric-label">Projects</span>
            <span class="metric-value">
              {$t('system.work.projects', { count: snapshot.work.active_projects_count || 0 })}
            </span>
          </div>
        </div>
      </section>

      <!-- 3. Workers Card -->
      <section class="card" aria-labelledby="heading-workers">
        <div class="card-header">
          <h2 id="heading-workers" class="card-title">{$t('system.workers.title')}</h2>
          <span class="badge">
            {snapshot.workers.length} · {snapshot.workers.filter(
              (w) => w.state.toLowerCase() === 'busy',
            ).length} busy · {snapshot.workers.filter((w) => w.state.toLowerCase() === 'idle')
              .length} idle
          </span>
        </div>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Role</th>
                <th>Harness</th>
                <th>Model</th>
                <th>State</th>
                <th>Current Item</th>
              </tr>
            </thead>
            <tbody>
              {#if snapshot.workers.length === 0}
                <tr>
                  <td colspan="5" class="text-muted text-center">No active workers.</td>
                </tr>
              {:else}
                {#each snapshot.workers as w}
                  <tr>
                    <td><strong>{w.role}</strong></td>
                    <td>{w.harness}</td>
                    <td class="mono">{w.model}</td>
                    <td>
                      <span class="worker-state worker-state--{w.state.toLowerCase()}">
                        {w.state.toLowerCase()}
                      </span>
                    </td>
                    <td class="text-truncate">{w.current_plan_title || '—'}</td>
                  </tr>
                {/each}
              {/if}
            </tbody>
          </table>
        </div>
      </section>

      <!-- 4. Jobs Card -->
      <section class="card" aria-labelledby="heading-jobs">
        <div class="card-header">
          <h2 id="heading-jobs" class="card-title">{$t('system.jobs.title')}</h2>
          <span class="badge">
            {$t('system.jobs.locks', { count: snapshot.jobs.locks_held || 0 })}
          </span>
        </div>

        <div class="metrics-list">
          <div class="metric-row">
            <span class="metric-label">Build slots</span>
            <span class="metric-value">
              {snapshot.jobs.build?.slots_used || 0} of {snapshot.jobs.build?.slots_total || 0} slots
              {#if snapshot.jobs.build?.queued && snapshot.jobs.build.queued > 0}
                · {snapshot.jobs.build.queued} queued
              {/if}
            </span>
          </div>

          <div class="metric-row">
            <span class="metric-label">Test slots</span>
            <span class="metric-value">
              {snapshot.jobs.test?.slots_used || 0} of {snapshot.jobs.test?.slots_total || 0} slots
              {#if snapshot.jobs.test?.queued && snapshot.jobs.test.queued > 0}
                · {snapshot.jobs.test.queued} queued
              {/if}
            </span>
          </div>

          <div class="metric-row">
            <span class="metric-label">Locks</span>
            <span class="metric-value">
              {snapshot.jobs.locks_held || 0} held
            </span>
          </div>
        </div>
      </section>

      <!-- 5. Models Usage Card -->
      <section class="card card--full" aria-labelledby="heading-models">
        <div class="card-header card-header--flex">
          <div class="models-title-row">
            <h2 id="heading-models" class="card-title">{$t('system.models.title')}</h2>
            <span class="text-muted text-sm">
              {$t('system.models.window', { window: selectedWindow })}
            </span>
          </div>

          <div class="group-selector" role="group" aria-label="Grouping selector">
            <button
              type="button"
              class="btn-toggle"
              class:btn-toggle--active={selectedGroupBy === 'model'}
              onclick={() => {
                selectedGroupBy = 'model';
              }}
            >
              {$t('system.models.per_model')}
            </button>
            <button
              type="button"
              class="btn-toggle"
              class:btn-toggle--active={selectedGroupBy === 'role'}
              onclick={() => {
                selectedGroupBy = 'role';
              }}
            >
              {$t('system.models.per_role')}
            </button>
          </div>
        </div>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>{selectedGroupBy === 'model' ? 'Model' : 'Role'}</th>
                <th>Calls</th>
                <th>Tokens (in · out)</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {#if displayModels.length === 0}
                <tr>
                  <td colspan="4" class="text-muted text-center">{$t('system.models.none')}</td>
                </tr>
              {:else}
                {#each displayModels as m}
                  <tr>
                    <td>
                      <strong class="mono">{m.id}</strong>
                    </td>
                    <td>{m.calls_count}</td>
                    <td>
                      {#if m.tokens_unmeasured && m.tokens_total === 0}
                        <span class="text-muted">
                          tokens not measured: not reported by {m.id.split(':')[0] || m.id}
                        </span>
                      {:else}
                        <span class="mono">
                          {formatSplit(
                            m.tokens_total,
                            m.tokens_in,
                            m.tokens_out,
                            false,
                            $currentLocale,
                          )}
                        </span>
                      {/if}
                    </td>
                    <td>
                      {#if m.errors_count > 0 || m.retries_count > 0}
                        <div class="tags-row">
                          {#if m.retries_count > 0}
                            <span class="badge badge--warning">
                              {m.retries_count}
                              {m.retries_count === 1 ? 'retry' : 'retries'}
                            </span>
                          {/if}
                          {#if m.errors_count > 0}
                            <span class="badge badge--danger">
                              {m.errors_count}
                              {m.errors_count === 1 ? 'error' : 'errors'}
                            </span>
                          {/if}
                        </div>
                      {:else}
                        <span class="text-muted">ok</span>
                      {/if}
                    </td>
                  </tr>
                {/each}
              {/if}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  {/if}
</div>

<!-- Modal Confirmation Dialogue -->
{#if activeModal}
  <div class="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="modal-title">
    <div class="modal-card">
      <h2 id="modal-title" class="modal-title">
        {$t('system.confirm.title', {
          act: activeModal,
          version: snapshot?.runtime.upgrade_available_version || '',
        })}
      </h2>

      <div class="modal-body">
        <div class="modal-running-notice">
          <strong>Running now:</strong>
          <span>
            {snapshot?.work.tasks_running || 0} tasks · {snapshot?.work.active_projects_count || 0} projects
          </span>
        </div>

        <fieldset class="modal-fieldset">
          <legend class="modal-legend">{$t('system.confirm.when')}</legend>
          <label class="radio-label">
            <input
              type="radio"
              name="modalWhen"
              value="drain"
              checked={modalWhen === 'drain'}
              onchange={() => {
                modalWhen = 'drain';
              }}
            />
            <div class="radio-content">
              <strong>{$t('system.confirm.drain')}</strong>
              <span class="text-muted text-sm">{$t('system.confirm.drain_explain')}</span>
            </div>
          </label>

          <label class="radio-label">
            <input
              type="radio"
              name="modalWhen"
              value="now"
              checked={modalWhen === 'now'}
              onchange={() => {
                modalWhen = 'now';
              }}
            />
            <div class="radio-content">
              <strong>{$t('system.confirm.now')}</strong>
              <span class="text-muted text-sm">{$t('system.confirm.now_explain')}</span>
            </div>
          </label>
        </fieldset>

        {#if activeModal === 'stop'}
          <p class="modal-note text-muted text-sm">{$t('system.confirm.stop_note')}</p>
        {:else if activeModal === 'restart'}
          <p class="modal-note text-muted text-sm">{$t('system.confirm.restart_note')}</p>
        {/if}
      </div>

      <div class="modal-footer">
        <Button variant="secondary" onclick={closeModal}>
          {$t('system.confirm.keep')}
        </Button>
        <Button
          variant={modalWhen === 'now' ? 'danger' : 'primary'}
          loading={isExecutingAction}
          onclick={executeLifecycleAction}
        >
          {$t('system.confirm.do', { act: activeModal, when: modalWhen })}
        </Button>
      </div>
    </div>
  </div>
{/if}

<style>
  .system-container {
    padding: var(--space-6);
    max-width: 1200px;
    margin: 0 auto;
    width: 100%;
  }

  .system-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-4);
    margin-bottom: var(--space-6);
  }

  .system-title {
    font-size: var(--text-2xl);
    font-weight: 700;
    margin: 0;
    color: var(--color-text-primary);
  }

  .system-subtitle {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }

  .system-header__controls {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  .window-selector,
  .group-selector {
    display: inline-flex;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    overflow: hidden;
    background-color: var(--color-bg-surface);
  }

  .btn-toggle {
    padding: var(--space-1) var(--space-3);
    font-size: var(--text-sm);
    background: transparent;
    border: none;
    cursor: pointer;
    color: var(--color-text-secondary);
    transition: all 0.15s;
  }

  .btn-toggle:not(:last-child) {
    border-right: 1px solid var(--color-border);
  }

  .btn-toggle--active {
    background-color: var(--color-primary);
    color: #ffffff;
    font-weight: 600;
  }

  .refresh-btn {
    background: transparent;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    padding: var(--space-1) var(--space-3);
    font-size: var(--text-lg);
    cursor: pointer;
    color: var(--color-text-primary);
  }

  .cleanup-banner {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-4);
    padding: var(--space-4) var(--space-6);
    background-color: var(--color-primary-subtle, #eff6ff);
    border: 1px solid var(--color-primary, #2563eb);
    border-radius: var(--radius-lg);
    margin-bottom: var(--space-6);
  }

  .cleanup-banner__info {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  .cleanup-banner__badge {
    font-weight: 700;
    color: var(--color-primary);
  }

  .cleanup-banner__text {
    font-size: var(--text-sm);
    font-weight: 500;
  }

  .system-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-6);
  }

  .card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-5);
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .card--full {
    grid-column: 1 / -1;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid var(--color-border-subtle);
    padding-bottom: var(--space-3);
  }

  .card-header--flex {
    flex-wrap: wrap;
    gap: var(--space-3);
  }

  .card-title {
    font-size: var(--text-lg);
    font-weight: 600;
    margin: 0;
    color: var(--color-text-primary);
  }

  .card-actions {
    display: flex;
    gap: var(--space-2);
  }

  .metrics-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .metric-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    font-size: var(--text-sm);
    gap: var(--space-4);
  }

  .metric-label {
    color: var(--color-text-secondary);
    font-weight: 500;
    min-width: 80px;
  }

  .metric-value {
    color: var(--color-text-primary);
    text-align: right;
  }

  .metric-row--highlight {
    background-color: var(--color-primary-subtle);
    padding: var(--space-2);
    border-radius: var(--radius-sm);
  }

  .table-container {
    overflow-x: auto;
  }

  .data-table {
    width: 100%;
    border-collapse: collapse;
    font-size: var(--text-sm);
    text-align: left;
  }

  .data-table th,
  .data-table td {
    padding: var(--space-2) var(--space-3);
    border-bottom: 1px solid var(--color-border-subtle);
  }

  .data-table th {
    font-weight: 600;
    color: var(--color-text-secondary);
    background-color: var(--color-bg-subtle);
  }

  .tags-row {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    justify-content: flex-end;
  }

  .status-tag {
    display: inline-block;
    padding: 0.15rem 0.5rem;
    font-size: var(--text-xs);
    font-weight: 600;
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
  }

  .status-tag--running {
    background-color: var(--color-success-subtle);
    color: var(--color-success);
  }

  .status-tag--waiting_for_you {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning);
  }

  .worker-state {
    display: inline-block;
    padding: 0.1rem 0.4rem;
    font-size: var(--text-xs);
    font-weight: 600;
    border-radius: var(--radius-sm);
  }

  .worker-state--busy {
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
  }

  .worker-state--idle {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-muted);
  }

  .badge {
    padding: 0.2rem 0.5rem;
    border-radius: var(--radius-full);
    font-size: var(--text-xs);
    font-weight: 600;
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
  }

  .badge--warning {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning);
  }

  .badge--danger {
    background-color: var(--color-danger-subtle);
    color: var(--color-danger);
  }

  .btn-inline {
    background: none;
    border: none;
    color: var(--color-primary);
    text-decoration: underline;
    font-size: var(--text-sm);
    cursor: pointer;
    margin-left: var(--space-2);
  }

  .link-muted {
    color: var(--color-primary);
    font-size: var(--text-xs);
    margin-left: var(--space-2);
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
    color: var(--color-success);
  }

  .notice-alert--danger {
    background-color: var(--color-danger-subtle);
    color: var(--color-danger);
  }

  .stopped-panel {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-8);
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-4);
  }

  .copy-box {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    background-color: var(--color-bg-subtle);
    padding: var(--space-2) var(--space-4);
    border-radius: var(--radius-md);
    border: 1px solid var(--color-border);
  }

  /* Modal */
  .modal-backdrop {
    position: fixed;
    inset: 0;
    background-color: rgba(0, 0, 0, 0.5);
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
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  }

  .modal-title {
    font-size: var(--text-xl);
    font-weight: 700;
    margin-top: 0;
    margin-bottom: var(--space-4);
  }

  .modal-fieldset {
    border: none;
    padding: 0;
    margin: var(--space-4) 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .modal-legend {
    font-weight: 600;
    margin-bottom: var(--space-2);
  }

  .radio-label {
    display: flex;
    align-items: flex-start;
    gap: var(--space-3);
    cursor: pointer;
    padding: var(--space-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
  }

  .radio-content {
    display: flex;
    flex-direction: column;
  }

  .modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-3);
    margin-top: var(--space-6);
  }

  @media (max-width: 768px) {
    .system-grid {
      grid-template-columns: 1fr;
    }
  }

  @media (max-width: 480px) {
    .system-container {
      padding: var(--space-3);
    }
    .metric-row {
      flex-direction: column;
      align-items: flex-start;
    }
    .metric-value {
      text-align: left;
    }
  }
</style>
