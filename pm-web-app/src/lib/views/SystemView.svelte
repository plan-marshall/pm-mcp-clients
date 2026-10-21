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
    type EnrolledProject,
  } from '../api/client';
  import { runtimeStore, navigateTo } from '../stores/runtime';
  import { hasUnsentInput, startUnsentInputTracking } from '../stores/unsentInput';
  import { formatSplit, formatBytes } from '../utils/tokenFormat';

  /** The browser's remembered filters: the window, the grouping, and the project. */
  const SYSTEM_FILTERS_KEY = 'pm_system_filters';

  let snapshot = $state<SystemSnapshot | null>(null);
  let projects = $state<EnrolledProject[]>([]);
  let isLoading = $state(true);
  let loadError = $state('');
  let actionError = $state('');
  let actionSuccess = $state('');

  // Selected filters
  let selectedWindow = $state<'hour' | 'today' | '7d'>('hour');
  let selectedProject = $state<string>('');
  let selectedGroupBy = $state<'model' | 'role'>('model');

  /** Where the upgrade is: offered, consented to, running, failed, or waiting to reload. */
  type UpgradePhase = 'idle' | 'consent' | 'progress' | 'withdrawn' | 'failed' | 'reload';
  let upgradePhase = $state<UpgradePhase>('idle');
  let upgradeInfo = $state<{
    version?: string;
    step?: string;
    installed?: string;
    withdrawable?: boolean;
    reason?: string;
    restored?: string;
  }>({});
  let reloadWaitsForInput = $state(false);

  // Confirmation modal state for lifecycle actions
  let activeModal = $state<'stop' | 'restart' | null>(null);
  let modalWhen = $state<'drain' | 'now'>('drain');
  let isExecutingAction = $state(false);
  let isWithdrawingUpgrade = $state(false);

  let unsubscribeEvents: (() => void) | null = null;
  let isMounted = true;

  /**
   * Reads the filters the browser remembered, keeping only values this view understands.
   */
  function restoreFilters(): void {
    if (typeof window === 'undefined') return;
    try {
      const raw = window.localStorage.getItem(SYSTEM_FILTERS_KEY);
      if (!raw) return;
      const saved = JSON.parse(raw) as {
        window?: string;
        groupBy?: string;
        project?: string;
      };
      if (saved.window === 'hour' || saved.window === 'today' || saved.window === '7d') {
        selectedWindow = saved.window;
      }
      if (saved.groupBy === 'model' || saved.groupBy === 'role') {
        selectedGroupBy = saved.groupBy;
      }
      if (typeof saved.project === 'string') {
        selectedProject = saved.project;
      }
    } catch {
      // A disabled or unreadable storage keeps the defaults; the view still works.
    }
  }

  /** Writes the filters to the browser, so the next visit opens where this one was. */
  function persistFilters(): void {
    if (typeof window === 'undefined') return;
    try {
      window.localStorage.setItem(
        SYSTEM_FILTERS_KEY,
        JSON.stringify({
          window: selectedWindow,
          groupBy: selectedGroupBy,
          project: selectedProject,
        }),
      );
    } catch {
      // Full, disabled, or private-mode storage: the filters apply to this visit only.
    }
  }

  onMount(() => {
    isMounted = true;
    restoreFilters();
    startUnsentInputTracking();
    unsubscribeEvents = apiClient.subscribeEvents((event, data) => {
      handleSystemEvent(event, data);
    });
    void apiClient
      .getProjectExplorer()
      .then((response) => {
        if (isMounted) projects = response.all_projects ?? [];
      })
      .catch(() => {
        // The project filter is optional; without the list the view still shows the machine.
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
        loadError = $t('system.view.load_failed');
      }
    } finally {
      if (isMounted) isLoading = false;
    }
  }

  function handleWindowChange(win: 'hour' | 'today' | '7d') {
    selectedWindow = win;
    persistFilters();
    void loadSnapshot(true);
  }

  function handleProjectChange(prj: string) {
    selectedProject = prj;
    persistFilters();
    void loadSnapshot(true);
  }

  function handleGroupChange(groupBy: 'model' | 'role') {
    selectedGroupBy = groupBy;
    persistFilters();
  }

  /**
   * Opens the overview on the work kind a figure counts.
   *
   * @param focus whether the plans or the epics are shown
   */
  function openOverview(focus: 'plans' | 'epics'): void {
    runtimeStore.update((s) => ({ ...s, overviewFocus: focus }));
    navigateTo('overview');
  }

  /**
   * Handles the runtime's events: the metrics refresh, and the upgrade's steps.
   *
   * @param event the event's name
   * @param data its payload
   */
  function handleSystemEvent(event: string, data: unknown): void {
    if (!isMounted) return;
    if (
      event === 'system.metrics' ||
      event === 'runtime.draining' ||
      event === 'runtime.drain_withdrawn'
    ) {
      void loadSnapshot(false);
      return;
    }
    if (event === 'runtime.upgrading') {
      upgradePhase = 'progress';
      upgradeInfo = { ...upgradeInfo, ...((data ?? {}) as typeof upgradeInfo) };
      return;
    }
    if (event === 'runtime.upgraded') {
      const info = (data ?? {}) as { version?: string };
      upgradeInfo = { ...upgradeInfo, version: info.version ?? upgradeInfo.version };
      upgradePhase = 'reload';
      reloadWaitsForInput = false;
      void loadSnapshot(false);
      maybeReloadAfterUpgrade();
      return;
    }
    if (event === 'runtime.upgrade_failed') {
      upgradePhase = 'failed';
      upgradeInfo = { ...upgradeInfo, ...((data ?? {}) as typeof upgradeInfo) };
      return;
    }
  }

  /**
   * Loads the app again after an upgrade, unless a field holds text the operator has not sent.
   */
  function maybeReloadAfterUpgrade(): void {
    if (hasUnsentInput()) {
      reloadWaitsForInput = true;
      return;
    }
    reloadWaitsForInput = false;
    try {
      window.location.reload();
    } catch {
      // A browser that refuses the reload keeps the notice with its own Load again button.
      reloadWaitsForInput = true;
    }
  }

  function openModal(action: 'stop' | 'restart') {
    activeModal = action;
    modalWhen = 'drain';
    actionError = '';
    actionSuccess = '';
  }

  /** Opens the upgrade's consent screen: the checks first, then the choice of when. */
  function openUpgradeConsent() {
    upgradePhase = 'consent';
    upgradeInfo = {
      version: snapshot?.runtime.upgrade_available_version ?? undefined,
      installed: snapshot?.runtime.version,
      withdrawable: true,
    };
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
      } else {
        await apiClient.stopRuntime(modalWhen, 'restart');
      }
      closeModal();
      void loadSnapshot(false);
    } catch (err: unknown) {
      actionError = (err as Error)?.message || $t('system.action.failed');
    } finally {
      isExecutingAction = false;
    }
  }

  /** Consents to the upgrade and starts it with the chosen `when`. */
  async function confirmUpgrade() {
    const version = upgradeInfo.version ?? snapshot?.runtime.upgrade_available_version ?? undefined;
    isExecutingAction = true;
    actionError = '';
    upgradeInfo = {
      ...upgradeInfo,
      version,
      step: 'fetch',
      installed: snapshot?.runtime.version,
      withdrawable: true,
    };
    upgradePhase = 'progress';
    try {
      await apiClient.upgradeRuntime(version, modalWhen);
      void loadSnapshot(false);
    } catch (err: unknown) {
      upgradePhase = 'failed';
      upgradeInfo = {
        ...upgradeInfo,
        step: 'fetch',
        reason: (err as Error)?.message ?? $t('system.action.failed'),
      };
    } finally {
      isExecutingAction = false;
    }
  }

  /** Withdraws the upgrade while it has not switched the links yet. */
  async function withdrawUpgrade() {
    isWithdrawingUpgrade = true;
    actionError = '';
    try {
      await apiClient.withdrawUpgrade();
      upgradePhase = 'withdrawn';
    } catch (err: unknown) {
      const message = (err as Error)?.message ?? '';
      if (message.includes('not_withdrawable') || message.includes('409')) {
        actionError = $t('system.upgrade.too_late');
      } else {
        actionError = $t('system.upgrade.withdraw_failed', { reason: message });
      }
    } finally {
      isWithdrawingUpgrade = false;
    }
  }

  /** Hurries the waiting upgrade into the stop now. */
  async function upgradeNow() {
    const version = upgradeInfo.version ?? snapshot?.runtime.upgrade_available_version ?? undefined;
    isExecutingAction = true;
    try {
      await apiClient.upgradeRuntime(version, 'now');
      upgradeInfo = { ...upgradeInfo, step: 'stop' };
      upgradePhase = 'progress';
    } catch (err: unknown) {
      actionError = (err as Error)?.message ?? $t('system.action.failed');
    } finally {
      isExecutingAction = false;
    }
  }

  /** Dismisses a failed upgrade's notice, keeping the system view as it is. */
  function dismissUpgrade(): void {
    upgradePhase = 'idle';
    upgradeInfo = {};
    actionError = '';
  }

  /**
   * @returns the step index the upgrade reached, for the check list
   */
  let upgradeStepIndex = $derived.by(() => {
    const order = ['fetch', 'verify', 'install', 'wait', 'stop', 'start'];
    const index = order.indexOf(upgradeInfo.step ?? '');
    return index < 0 ? 0 : index;
  });

  /** @returns the catalogue key of the failure the upgrade reported */
  function upgradeFailureKey(): string {
    if (upgradeInfo.step === 'start') {
      return upgradeInfo.restored ? 'system.upgrade.failed_start' : 'system.upgrade.failed_both';
    }
    return 'system.upgrade.failed_before';
  }

  /**
   * @param step the upgrade step
   * @returns its place in the check list
   */
  function stepState(step: string): 'done' | 'running' | 'pending' {
    const order = ['fetch', 'verify', 'install', 'wait', 'stop', 'start'];
    const index = order.indexOf(step);
    if (index < upgradeStepIndex) return 'done';
    if (index === upgradeStepIndex) return 'running';
    return 'pending';
  }

  async function handleCheckUpgrade() {
    try {
      actionError = '';
      actionSuccess = '';
      await apiClient.checkUpgrade();
      void loadSnapshot(false);
      actionSuccess = $t('system.upgrade.check_done');
    } catch (err: unknown) {
      actionError = (err as Error)?.message || $t('system.upgrade.check_failed');
    }
  }

  function copyCommand(text: string) {
    if (navigator?.clipboard) {
      void navigator.clipboard.writeText(text);
      actionSuccess = $t('system.action.copied');
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
      {#if projects.length > 0}
        <select
          class="project-filter"
          aria-label={$t('system.work.per_project')}
          value={selectedProject}
          onchange={(event) => handleProjectChange(event.currentTarget.value)}
          data-testid="system-project-filter"
        >
          <option value="">{$t('system.work.all')}</option>
          {#each projects as project (project.project_id)}
            <option value={project.project_id}>{project.display_name}</option>
          {/each}
        </select>
      {/if}

      <div class="window-selector" role="group" aria-label={$t('system.view.window_label')}>
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
        aria-label={$t('system.view.refresh')}
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

  {#if snapshot?.runtime.upgrade_available_version && upgradePhase === 'idle'}
    <section class="upgrade-panel upgrade-panel--available" aria-labelledby="upgrade-available">
      <div class="upgrade-panel__head">
        <h2 id="upgrade-available" class="upgrade-panel__title">
          {$t('system.upgrade.available_title', {
            version: snapshot.runtime.upgrade_available_version,
          })}
        </h2>
        <span class="text-muted text-sm">
          {$t('system.upgrade.available_summary', {
            installed: snapshot.runtime.version,
            version: snapshot.runtime.upgrade_available_version,
            date: snapshot.runtime.upgrade_available_date ?? $t('system.figure.unavailable'),
          })}
        </span>
      </div>
      {#if snapshot.runtime.upgrade_release_notes_summary}
        <p class="upgrade-panel__notes">{snapshot.runtime.upgrade_release_notes_summary}</p>
      {/if}
      <div class="upgrade-panel__actions">
        {#if snapshot.runtime.upgrade_release_notes_url}
          <a
            href={snapshot.runtime.upgrade_release_notes_url}
            target="_blank"
            rel="noreferrer"
            class="link-muted"
          >
            {$t('system.runtime.release_notes')}
          </a>
        {/if}
        <Button
          variant="primary"
          disabled={snapshot.runtime.upgrade_possible === false}
          onclick={openUpgradeConsent}
        >
          {$t('system.action.upgrade')}
        </Button>
      </div>
      {#if snapshot.runtime.upgrade_possible === false}
        <p class="text-muted text-sm">
          {$t('system.upgrade.disabled', {
            reason: snapshot.runtime.upgrade_disabled_reason ?? $t('system.figure.unavailable'),
          })}
        </p>
      {/if}
    </section>
  {/if}

  {#if upgradePhase === 'progress'}
    <section class="upgrade-panel" aria-labelledby="upgrade-progress" role="status">
      <h2 id="upgrade-progress" class="upgrade-panel__title">
        {$t('system.upgrade.title', {
          version: upgradeInfo.version ?? snapshot?.runtime.upgrade_available_version ?? '',
        })}
      </h2>
      <ul class="upgrade-steps" data-testid="upgrade-steps">
        {#each ['fetch', 'verify', 'install', 'wait', 'stop', 'start'] as step}
          {@const state = stepState(step)}
          <li class="upgrade-step upgrade-step--{state}" data-testid={`upgrade-step-${step}`}>
            <span class="upgrade-step__glyph" aria-hidden="true">
              {state === 'done' ? '✓' : state === 'running' ? '◐' : '·'}
            </span>
            {$t('system.upgrade.step', {
              step,
              installed: upgradeInfo.installed ?? snapshot?.runtime.version ?? '',
              version: upgradeInfo.version ?? '',
            })}
          </li>
        {/each}
      </ul>
      <div class="upgrade-panel__actions">
        {#if upgradeInfo.withdrawable !== false && upgradeStepIndex < 2}
          <Button variant="secondary" loading={isWithdrawingUpgrade} onclick={withdrawUpgrade}>
            {$t('system.upgrade.withdraw')}
          </Button>
        {/if}
        {#if upgradeInfo.step === 'wait'}
          <Button variant="primary" loading={isExecutingAction} onclick={upgradeNow}>
            {$t('system.upgrade.now')}
          </Button>
        {/if}
      </div>
    </section>
  {:else if upgradePhase === 'withdrawn'}
    <div class="notice-alert notice-alert--success" role="status" data-testid="upgrade-withdrawn">
      {$t('system.upgrade.withdrawn', {
        installed: upgradeInfo.installed ?? snapshot?.runtime.version ?? '',
      })}
    </div>
  {:else if upgradePhase === 'failed'}
    <section class="upgrade-panel upgrade-panel--failed" role="alert" data-testid="upgrade-failed">
      <p>
        {$t(upgradeFailureKey(), {
          version: upgradeInfo.version ?? '',
          step: upgradeInfo.step ?? '',
          reason: upgradeInfo.reason ?? $t('system.figure.unavailable'),
          installed:
            upgradeInfo.restored ?? upgradeInfo.installed ?? snapshot?.runtime.version ?? '',
        })}
      </p>
      <div class="upgrade-panel__actions">
        <Button variant="secondary" onclick={dismissUpgrade}>
          {$t('system.upgrade.dismiss')}
        </Button>
        <Button variant="primary" onclick={openUpgradeConsent}>
          {$t('system.upgrade.try_again')}
        </Button>
      </div>
    </section>
  {:else if upgradePhase === 'reload'}
    <section class="upgrade-panel" role="status" data-testid="upgrade-reload">
      <p>
        {$t('system.mismatch.web', {
          version: upgradeInfo.version ?? snapshot?.runtime.upgrade_available_version ?? '',
        })}
      </p>
      {#if reloadWaitsForInput}
        <p class="text-muted text-sm">{$t('system.upgrade.reload.unsent')}</p>
      {/if}
      <div class="upgrade-panel__actions">
        <Button variant="primary" onclick={maybeReloadAfterUpgrade}>
          {$t('system.mismatch.load_again')}
        </Button>
      </div>
    </section>
  {/if}

  {#if isLoading && !snapshot}
    <div class="system-loading">
      <Spinner size="2.5rem" label={$t('system.view.loading')} />
    </div>
  {:else if loadError && !snapshot}
    <div class="notice-alert notice-alert--danger" role="alert">
      <p>{loadError}</p>
      <Button variant="secondary" onclick={() => loadSnapshot(true)}
        >{$t('web.common.retry')}</Button
      >
    </div>
  {:else if $runtimeStore.isStopped}
    <div class="stopped-panel" role="region" aria-label={$t('system.stopped.region')}>
      <h2 class="stopped-title">{$t('system.stopped.title', { who: 'operator', time: 'now' })}</h2>
      <p class="stopped-desc">
        {$t('system.stopped.how', { command: 'pm-operator runtime start' })}
      </p>
      <div class="copy-box">
        <code class="mono">{$t('system.stopped.command')}</code>
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
      <div class="cleanup-banner" role="region" aria-label={$t('system.cleanup.region')}>
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
              <Button variant="primary" onclick={openUpgradeConsent}>
                {$t('system.action.upgrade')}
              </Button>
            {/if}
          </div>
        </div>

        <div class="metrics-list">
          <div class="metric-row">
            <span class="metric-label">{$t('system.runtime.field.version')}</span>
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
              <span class="metric-label">{$t('system.runtime.field.update')}</span>
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
                    {$t('system.runtime.release_notes')}
                  </a>
                {/if}
              </span>
            </div>
          {:else}
            <div class="metric-row">
              <span class="metric-label">{$t('system.runtime.field.update')}</span>
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
              <span class="metric-label">{$t('system.runtime.field.uptime')}</span>
              <span class="metric-value">{snapshot.runtime.uptime_formatted}</span>
            </div>
          {/if}

          <div class="metric-row">
            <span class="metric-label">{$t('system.runtime.field.memory')}</span>
            <span class="metric-value">
              {$t('system.runtime.memory', {
                used: formatBytes(snapshot.runtime.memory_used_bytes || 0),
                max: formatBytes(snapshot.runtime.memory_max_bytes || 0),
                resident: snapshot.runtime.memory_resident_bytes
                  ? formatBytes(snapshot.runtime.memory_resident_bytes)
                  : $t('system.figure.unavailable'),
              })}
            </span>
          </div>

          <div class="metric-row">
            <span class="metric-label">{$t('system.runtime.field.cpu')}</span>
            <span class="metric-value">
              {#if snapshot.runtime.cpu_measured && snapshot.runtime.cpu_percent !== null}
                {snapshot.runtime.cpu_percent} %
              {:else}
                <span class="text-muted">{$t('system.figure.unavailable_reason')}</span>
              {/if}
            </span>
          </div>

          <div class="metric-row">
            <span class="metric-label">{$t('system.runtime.field.store')}</span>
            <span class="metric-value">
              {$t('system.runtime.store', {
                size: formatBytes(snapshot.runtime.machine_store_bytes || 0),
                free: formatBytes(snapshot.runtime.disk_free_bytes || 0),
                time: snapshot.runtime.disk_measured_at || $t('system.figure.unavailable'),
              })}
            </span>
          </div>

          <div class="metric-row">
            <span class="metric-label">{$t('system.runtime.field.listeners')}</span>
            <span class="metric-value">
              {$t('system.runtime.listeners', {
                loopback: snapshot.runtime.loopback_address ? 'open' : 'off',
                loopback_address: snapshot.runtime.loopback_address || '',
                lan: snapshot.runtime.lan_address ? 'open' : 'off',
                lan_address: snapshot.runtime.lan_address || '',
                devices: snapshot.runtime.lan_devices_count || 0,
              })}
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
              {$t('system.work.recovering')}: {snapshot.work.recovering_projects.join(', ')}
            </span>
          {/if}
        </div>

        <div class="metrics-list">
          <div class="metric-row">
            <span class="metric-label">{$t('system.work.field.plans')}</span>
            <div class="metric-value tags-row">
              {#if snapshot.work.plans_by_state && Object.keys(snapshot.work.plans_by_state).length > 0}
                {#each Object.entries(snapshot.work.plans_by_state) as [st, count]}
                  {#if count > 0}
                    <button
                      type="button"
                      class="status-tag status-tag--{st} status-tag--link"
                      aria-label={$t('system.work.link.plans', { state: st, count })}
                      onclick={() => openOverview('plans')}
                    >
                      {count}
                      {st.replace('_', ' ')}
                    </button>
                  {/if}
                {/each}
              {:else}
                <span class="text-muted">{$t('system.work.none_running')}</span>
              {/if}
            </div>
          </div>

          <div class="metric-row">
            <span class="metric-label">{$t('system.work.field.epics')}</span>
            <div class="metric-value tags-row">
              {#if snapshot.work.epics_by_state && Object.keys(snapshot.work.epics_by_state).length > 0}
                {#each Object.entries(snapshot.work.epics_by_state) as [st, count]}
                  {#if count > 0}
                    <button
                      type="button"
                      class="status-tag status-tag--{st} status-tag--link"
                      aria-label={$t('system.work.link.epics', { state: st, count })}
                      onclick={() => openOverview('epics')}
                    >
                      {count}
                      {st.replace('_', ' ')}
                    </button>
                  {/if}
                {/each}
              {:else}
                <span class="text-muted">{$t('system.work.none_running')}</span>
              {/if}
            </div>
          </div>

          <div class="metric-row">
            <span class="metric-label">{$t('system.work.field.tasks')}</span>
            <span class="metric-value">
              {$t('system.work.tasks', {
                running: snapshot.work.tasks_running || 0,
                queued: snapshot.work.tasks_queued || 0,
              })}
            </span>
          </div>

          <div class="metric-row">
            <span class="metric-label">{$t('system.work.field.projects')}</span>
            <span class="metric-value">
              <button
                type="button"
                class="metric-link"
                aria-label={$t('system.work.link.projects', {
                  count: snapshot.work.active_projects_count || 0,
                })}
                onclick={() => navigateTo('projects')}
              >
                {$t('system.work.projects', { count: snapshot.work.active_projects_count || 0 })}
              </button>
            </span>
          </div>
        </div>
      </section>

      <!-- 3. Workers Card -->
      <section class="card" aria-labelledby="heading-workers">
        <div class="card-header">
          <h2 id="heading-workers" class="card-title">{$t('system.workers.title')}</h2>
          <span class="badge">
            {$t('system.workers.count', {
              count: snapshot.workers.length,
              busy: snapshot.workers.filter((w) => w.state.toLowerCase() === 'busy').length,
              idle: snapshot.workers.filter((w) => w.state.toLowerCase() === 'idle').length,
            })}
          </span>
        </div>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>{$t('system.workers.col.role')}</th>
                <th>{$t('system.workers.col.harness')}</th>
                <th>{$t('system.workers.col.model')}</th>
                <th>{$t('system.workers.col.state')}</th>
                <th>{$t('system.workers.col.current_item')}</th>
              </tr>
            </thead>
            <tbody>
              {#if snapshot.workers.length === 0}
                <tr>
                  <td colspan="5" class="text-muted text-center">{$t('system.workers.none')}</td>
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
            <span class="metric-label">{$t('system.jobs.field.build_slots')}</span>
            <span class="metric-value">
              {$t('system.jobs.slots', {
                kind: $t('system.jobs.field.build_slots'),
                used: snapshot.jobs.build?.slots_used || 0,
                total: snapshot.jobs.build?.slots_total ?? 0,
                queued: snapshot.jobs.build?.queued || 0,
              })}
            </span>
          </div>

          <div class="metric-row">
            <span class="metric-label">{$t('system.jobs.field.test_slots')}</span>
            <span class="metric-value">
              {$t('system.jobs.slots', {
                kind: $t('system.jobs.field.test_slots'),
                used: snapshot.jobs.test?.slots_used || 0,
                total: snapshot.jobs.test?.slots_total ?? 0,
                queued: snapshot.jobs.test?.queued || 0,
              })}
            </span>
          </div>

          <div class="metric-row">
            <span class="metric-label">{$t('system.jobs.field.locks')}</span>
            <span class="metric-value">
              {$t('system.jobs.locks_held', { count: snapshot.jobs.locks_held || 0 })}
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

          <div class="group-selector" role="group" aria-label={$t('system.models.grouping')}>
            <button
              type="button"
              class="btn-toggle"
              class:btn-toggle--active={selectedGroupBy === 'model'}
              onclick={() => handleGroupChange('model')}
            >
              {$t('system.models.per_model')}
            </button>
            <button
              type="button"
              class="btn-toggle"
              class:btn-toggle--active={selectedGroupBy === 'role'}
              onclick={() => handleGroupChange('role')}
            >
              {$t('system.models.per_role')}
            </button>
          </div>
        </div>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>
                  {$t(
                    selectedGroupBy === 'model'
                      ? 'system.models.col.model'
                      : 'system.models.col.role',
                  )}
                </th>
                <th>{$t('system.models.col.calls')}</th>
                <th>{$t('system.models.col.tokens')}</th>
                <th>{$t('system.models.col.status')}</th>
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
                          {$t('system.models.not_measured', {
                            model: m.id.split(':')[0] || m.id,
                          })}
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

<!-- Upgrade Consent Dialogue -->
{#if upgradePhase === 'consent' && snapshot}
  <div
    class="modal-backdrop"
    role="dialog"
    aria-modal="true"
    aria-labelledby="upgrade-consent-title"
    data-testid="upgrade-consent"
  >
    <div class="modal-card">
      <h2 id="upgrade-consent-title" class="modal-title">
        {$t('system.upgrade.consent', {
          installed: snapshot.runtime.version,
          version: snapshot.runtime.upgrade_available_version ?? '',
        })}
      </h2>

      <div class="modal-body">
        <p class="text-muted text-sm">
          {$t('system.upgrade.explain', {
            installed: snapshot.runtime.version,
            version: snapshot.runtime.upgrade_available_version ?? '',
          })}
        </p>

        <h3 class="modal-section-title">{$t('system.upgrade.check.title')}</h3>
        <ul class="upgrade-check-list" data-testid="upgrade-check-list">
          <li class="upgrade-check">
            <span class="upgrade-step__glyph" aria-hidden="true">✓</span>
            {$t('system.upgrade.check.running_workers', {
              count: snapshot.workers.filter((w) => w.state.toLowerCase() === 'busy').length,
            })}
          </li>
          <li class="upgrade-check">
            <span class="upgrade-step__glyph" aria-hidden="true">✓</span>
            {$t('system.upgrade.check.active_builds', {
              count: snapshot.jobs.build?.slots_used ?? 0,
            })}
          </li>
          <li class="upgrade-check">
            {#if snapshot.runtime.dirty_worktrees !== null && snapshot.runtime.dirty_worktrees !== undefined}
              <span class="upgrade-step__glyph" aria-hidden="true">
                {snapshot.runtime.dirty_worktrees === 0 ? '✓' : '✗'}
              </span>
              {$t('system.upgrade.check.dirty_worktrees', {
                count: snapshot.runtime.dirty_worktrees,
              })}
            {:else}
              <span class="upgrade-step__glyph" aria-hidden="true">?</span>
              {$t('system.upgrade.check.dirty_reported')}
            {/if}
          </li>
        </ul>

        <fieldset class="modal-fieldset">
          <legend class="modal-legend">{$t('system.confirm.when')}</legend>
          <label class="radio-label">
            <input
              type="radio"
              name="upgradeWhen"
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
              name="upgradeWhen"
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
      </div>

      <div class="modal-footer">
        <Button
          variant="secondary"
          onclick={() => {
            upgradePhase = 'idle';
          }}
        >
          {$t('system.confirm.not_now')}
        </Button>
        <Button
          variant={modalWhen === 'now' ? 'danger' : 'primary'}
          loading={isExecutingAction}
          onclick={confirmUpgrade}
        >
          {$t('system.confirm.do', { act: 'upgrade', when: modalWhen })}
        </Button>
      </div>
    </div>
  </div>
{/if}

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
          <strong>{$t('system.confirm.running_now')}</strong>
          <span>
            {$t('system.confirm.running_what', {
              tasks: snapshot?.work.tasks_running || 0,
              projects: snapshot?.work.active_projects_count || 0,
            })}
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

  .project-filter {
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-sm);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
  }

  .status-tag--link {
    border: none;
    cursor: pointer;
    font-family: inherit;
  }

  .status-tag--link:hover {
    text-decoration: underline;
  }

  .metric-link {
    background: none;
    border: none;
    padding: 0;
    font: inherit;
    color: var(--color-primary);
    cursor: pointer;
    text-align: right;
  }

  .metric-link:hover {
    text-decoration: underline;
  }

  .upgrade-panel {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-4) var(--space-5);
    margin-bottom: var(--space-5);
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .upgrade-panel--available {
    border-color: var(--color-primary);
    background-color: var(--color-primary-subtle);
  }

  .upgrade-panel--failed {
    border-color: var(--color-danger-border);
    background-color: var(--color-danger-subtle);
    color: var(--color-danger);
  }

  .upgrade-panel__head {
    display: flex;
    align-items: baseline;
    gap: var(--space-3);
    flex-wrap: wrap;
  }

  .upgrade-panel__title {
    font-size: var(--text-lg);
    font-weight: 700;
    margin: 0;
  }

  .upgrade-panel__notes {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .upgrade-panel__actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: var(--space-3);
    flex-wrap: wrap;
  }

  .upgrade-steps,
  .upgrade-check-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .upgrade-step,
  .upgrade-check {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
  }

  .upgrade-step--pending {
    color: var(--color-text-muted);
  }

  .upgrade-step--running {
    font-weight: 600;
  }

  .upgrade-step__glyph {
    width: 1rem;
    text-align: center;
    font-weight: 700;
  }

  .modal-section-title {
    font-size: var(--text-sm);
    font-weight: 700;
    margin: var(--space-4) 0 var(--space-2) 0;
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
