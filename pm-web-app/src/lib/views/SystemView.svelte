<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import Button from '../components/Button.svelte';
  import Icon from '../components/Icon.svelte';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import { actFailureReason, actFailureText, errorCodeOf } from '../api/errors';
  import {
    apiClient,
    type SystemSnapshot,
    type WorkerMetric,
    type ModelMetric,
    type EnrolledProject,
  } from '../api/client';
  import { runtimeStore, navigateTo, advanceLifecycleEpoch } from '../stores/runtime';
  import { setReloadDeferred } from '../stores/reloadDeferred';
  import { displayState } from '../display';
  import {
    hasUnsentInput,
    hasUnsentInputStore,
    startUnsentInputTracking,
  } from '../stores/unsentInput';
  import { formatSplit, formatBytes, formatTime } from '../display/format';
  import DashboardPage from '../templates/DashboardPage.svelte';
  import ActionBar from '../components/ActionBar.svelte';
  import SegmentedControl from '../components/SegmentedControl.svelte';
  import SelectField from '../components/SelectField.svelte';
  import IconButton from '../components/IconButton.svelte';
  import StatusLabel from '../components/StatusLabel.svelte';
  import CountBadge from '../components/CountBadge.svelte';
  import DataTable, {
    type DataTableColumn,
    type DataTableRow,
  } from '../components/DataTable.svelte';
  import Dialog from '../components/Dialog.svelte';
  import ConfirmDialog from '../components/ConfirmDialog.svelte';
  import RadioGroup from '../components/RadioGroup.svelte';
  import ExternalLink from '../components/ExternalLink.svelte';
  import { actsFor } from '../actions/byState';

  /** The browser's remembered filters: the window, the grouping, and the project. */
  const SYSTEM_FILTERS_KEY = 'pm_system_filters';

  let snapshot = $state<SystemSnapshot | null>(null);
  let projects = $state<EnrolledProject[]>([]);
  let isLoading = $state(true);
  let loadError = $state<string | null>(null);
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
  /**
   * The page heading, which takes the focus back when the consent closes: the control that opened
   * the dialog — the panel's Upgrade button — is unmounted while the runtime reads as upgrading, and
   * a removed element cannot take the focus back.
   */
  let pageHeading = $state<HTMLElement | null>(null);

  // Confirmation modal state for lifecycle actions
  let activeModal = $state<'stop' | 'restart' | null>(null);
  let modalWhen = $state<'drain' | 'now'>('drain');
  let isExecutingAction = $state(false);
  let isWithdrawingUpgrade = $state(false);

  let unsubscribeEvents: (() => void) | null = null;
  let isMounted = true;

  /**
   * Who stopped the runtime and when, where the runtime recorded both; `null` otherwise, and the
   * stopped page then says only that the runtime is stopped rather than inventing either.
   */
  const stoppedBy = $derived.by(() => {
    const info = $runtimeStore.stoppedInfo;
    const time = formatTime(info?.time, 'short', $currentLocale);
    return info?.who && time ? { who: info.who, time } : null;
  });

  /**
   * Reads the filters the browser remembered, keeping only values this view understands.
   */
  function restoreFilters(): void {
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
        if (!isMounted) return;
        projects = response.all_projects ?? [];
        if (selectedProject && !projects.some((p) => p.project_id === selectedProject)) {
          handleProjectChange('');
        }
      })
      .catch(() => {
        // Without the list the filter cannot be shown, so it must not apply either.
        if (isMounted && selectedProject) handleProjectChange('');
      });
    void loadSnapshot(true);
  });

  onDestroy(() => {
    isMounted = false;
    // The deferred reload is this view's obligation: when it goes, the deferral goes with it, so a
    // language change is not blocked for the rest of the session.
    setReloadDeferred(false);
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  async function loadSnapshot(showSpinner = false) {
    if (showSpinner) isLoading = true;
    loadError = null;
    try {
      const snap = await apiClient.getSystemSnapshot(selectedWindow, selectedProject || undefined);
      if (!isMounted) return;
      snapshot = snap;
    } catch (err: unknown) {
      if (!isMounted) return;
      // The failure is a state, not a text: the code is read where it renders, so the words follow
      // a language switch and never show a key.
      loadError = errorCodeOf(err);
      if (loadError === 'network_offline') {
        runtimeStore.update((s) => ({ ...s, isOffline: true }));
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
  function maybeReloadAfterUpgrade(force = false): void {
    if (!force && hasUnsentInput()) {
      reloadWaitsForInput = true;
      setReloadDeferred(true);
      return;
    }
    reloadWaitsForInput = false;
    try {
      window.location.reload();
    } catch {
      // A browser that refuses the reload keeps the notice with its own Load again button.
      reloadWaitsForInput = true;
      setReloadDeferred(true);
      return;
    }
    // Only a reload that returned without navigating leaves the page running the old code: the
    // held catalogue is released then, never before the call that unloads the page.
    setReloadDeferred(false);
  }

  // The reload waits while a field holds text; once the operator sends or clears it, the wait
  // ends and the app loads the new release by itself.
  $effect(() => {
    if (upgradePhase === 'reload' && reloadWaitsForInput && !$hasUnsentInputStore) {
      maybeReloadAfterUpgrade();
    }
  });

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
    // The operator can change the radio group while the request runs: the choice the request was
    // sent with is the one the state update must follow.
    const when = modalWhen;
    isExecutingAction = true;
    actionError = '';
    try {
      if (activeModal === 'stop') {
        await apiClient.stopRuntime(when);
        advanceLifecycleEpoch();
        if (when === 'drain') {
          runtimeStore.update((s) => ({ ...s, isDraining: true }));
        } else {
          // This stop has no recorded attribution yet: a previous stop's must not be shown for it.
          runtimeStore.update((s) => ({ ...s, isStopped: true, stoppedInfo: null }));
        }
      } else {
        await apiClient.stopRuntime(when, 'restart');
        advanceLifecycleEpoch();
      }
      closeModal();
      void loadSnapshot(false);
    } catch (err: unknown) {
      actionError = actFailureText(err, $t('system.action.failed'));
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
        reason: actFailureReason(err, $t('system.action.failed')),
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
      // The refusal's own code picks the sentence; it is never printed. Only `not_withdrawable`
      // means the links are switched: another refusal, or a bare HTTP status, is not "too late".
      if (errorCodeOf(err) === 'not_withdrawable') {
        actionError = $t('system.upgrade.too_late');
      } else {
        actionError = $t('system.upgrade.withdraw_failed', {
          reason: actFailureReason(err, $t('system.action.failed')),
        });
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
      actionError = actFailureText(err, $t('system.action.failed'));
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

  /** The runtime's lifecycle state, which selects its acts (UC-P-02). */
  let runtimeState = $derived(
    $runtimeStore.isDraining
      ? 'draining'
      : upgradePhase !== 'idle'
        ? 'upgrading'
        : snapshot?.runtime.upgrade_available_version && snapshot.runtime.upgrade_possible !== false
          ? 'update_available'
          : 'running',
  );

  /** While the runtime drains or upgrades, its own panel carries the acts. */
  let showRuntimeActs = $derived(runtimeState === 'running' || runtimeState === 'update_available');

  /** The runtime card's acts, from the state's table. */
  let runtimeActs = $derived(
    actsFor('runtime', runtimeState).map((spec) => ({
      id: spec.id,
      label: $t(spec.labelKey),
      advisory: spec.advisoryKey ? $t(spec.advisoryKey) : undefined,
      role: spec.role,
      destructive: spec.destructive,
    })),
  );

  /** @param id the act the runtime bar reports */
  function handleRuntimeAct(id: string): void {
    switch (id) {
      case 'check':
        void handleCheckUpgrade();
        return;
      case 'stop':
        openModal('stop');
        return;
      case 'restart':
        openModal('restart');
        return;
      case 'upgrade':
        openUpgradeConsent();
        return;
    }
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
      actionError = $t('system.upgrade.check_failed', {
        reason: actFailureReason(err, $t('system.action.failed')),
      });
    }
  }

  /**
   * Copies the start command; the confirmation is shown only once the browser took it, so a
   * refused clipboard claims no copy that did not happen.
   *
   * @param text the command to copy
   */
  async function copyCommand(text: string): Promise<void> {
    if (!navigator.clipboard) return;
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      return;
    }
    actionSuccess = $t('system.action.copied');
    setTimeout(() => {
      actionSuccess = '';
    }, 3000);
  }

  /**
   * A size the runtime measured, or `not available` where it sent none: an unmeasured figure is
   * never shown as `0 B` (system.adoc, The Requests: `unavailable` where it is not measured).
   *
   * @param bytes the measured size, absent when not measured
   * @returns the text of the figure
   */
  function bytesFigure(bytes: number | undefined): string {
    return bytes === undefined ? $t('system.figure.unavailable') : formatBytes(bytes);
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

  /**
   * @param url the release-notes address the runtime offers, a path of the app or an absolute one
   * @returns an absolute http(s) address, resolved against the app's own origin where it is a path
   */
  function releaseNotesHref(url: string): string {
    if (/^https?:\/\//i.test(url)) return url;
    if (typeof window === 'undefined') return url;
    try {
      return new URL(url, window.location.origin).href;
    } catch {
      return url;
    }
  }

  /** The tones of `StatusLabel`, chosen from the state word the runtime serves. */
  const DANGER_STATES = ['failed', 'landing_failed', 'error'];
  const ATTENTION_STATES = ['waiting_for_you', 'stalled', 'held'];
  const SUCCESS_STATES = ['done', 'merged', 'ready', 'completed', 'success', 'idle'];

  /**
   * @param state the state word of a plan or an epic
   * @returns the tone its dot takes
   */
  function stateTone(state: string): 'neutral' | 'running' | 'attention' | 'danger' | 'success' {
    const word = state.toLowerCase();
    if (word === 'running' || word === 'pausing') return 'running';
    if (ATTENTION_STATES.includes(word)) return 'attention';
    if (DANGER_STATES.includes(word)) return 'danger';
    if (SUCCESS_STATES.includes(word)) return 'success';
    return 'neutral';
  }

  /** The window: three options that switch what the same figures count. */
  let windowOptions = $derived([
    { id: 'hour', label: $t('system.models.window', { window: 'hour' }) },
    { id: 'today', label: $t('system.models.window', { window: 'today' }) },
    { id: '7d', label: $t('system.models.window', { window: 'week' }) },
  ]);

  /** The grouping of the models: per model or per role. */
  let groupOptions = $derived([
    { id: 'model', label: $t('system.models.per_model') },
    { id: 'role', label: $t('system.models.per_role') },
  ]);

  /** The enrolled projects of the filter, with the answer that clears it. */
  let projectOptions = $derived([
    { value: '', label: $t('system.work.all') },
    ...projects.map((project) => ({
      value: project.project_id,
      label: project.display_name,
    })),
  ]);

  /** The choice of when a lifecycle act takes effect, drain preselected. */
  let whenOptions = $derived([
    {
      value: 'drain',
      label: $t('system.confirm.drain'),
      description: $t('system.confirm.drain_explain'),
    },
    {
      value: 'now',
      label: $t('system.confirm.now'),
      description: $t('system.confirm.now_explain'),
    },
  ]);

  /** The workers as the table's rows; every cell is text the view already formatted. */
  let workerColumns = $derived<DataTableColumn[]>([
    { id: 'role', header: $t('system.workers.col.role'), width: '8rem' },
    { id: 'harness', header: $t('system.workers.col.harness'), width: '5.5rem' },
    { id: 'model', header: $t('system.workers.col.model'), width: '6.5rem' },
    { id: 'state', header: $t('system.workers.col.state'), width: '7rem' },
    { id: 'item', header: $t('system.workers.col.current_item'), width: '8rem' },
  ]);

  let workerRows = $derived<DataTableRow[]>(
    (snapshot?.workers ?? []).map((worker, index) => ({
      id: `${worker.role}-${worker.model}-${index}`,
      cells: {
        role: worker.role,
        harness: worker.harness,
        model: worker.model,
        state: displayState('worker', worker.state, $t),
        item: worker.current_plan_title || '—',
      },
    })),
  );

  /** The models as the table's rows, the same figures the role grouping sums. */
  let modelColumns = $derived<DataTableColumn[]>([
    {
      id: 'model',
      header: $t(selectedGroupBy === 'model' ? 'system.models.col.model' : 'system.models.col.role'),
      width: '16rem',
    },
    { id: 'calls', header: $t('system.models.col.calls'), width: '6rem', numeric: true },
    { id: 'tokens', header: $t('system.models.col.tokens'), width: '22rem', numeric: true },
    { id: 'status', header: $t('system.models.col.status'), width: '10rem' },
  ]);

  /**
   * @param model one model row
   * @returns the token figure, or the words of an unmeasured model
   */
  function tokenFigure(model: ModelMetric): string {
    if (model.tokens_unmeasured && model.tokens_total === 0) {
      return $t('system.models.not_measured', { model: model.id.split(':')[0] || model.id });
    }
    return formatSplit(model.tokens_total, model.tokens_in, model.tokens_out, false, $currentLocale);
  }

  /**
   * @param model one model row
   * @returns what its status cell says: the retries and errors, or the word for a healthy model
   */
  function modelStatus(model: ModelMetric): string {
    const parts: string[] = [];
    if (model.retries_count > 0) {
      parts.push($t('system.models.retry', { count: model.retries_count }));
    }
    if (model.errors_count > 0) {
      parts.push($t('system.models.error', { count: model.errors_count }));
    }
    return parts.length > 0 ? parts.join(' · ') : $t('system.models.ok');
  }

  let modelRows = $derived<DataTableRow[]>(
    displayModels.map((model) => ({
      id: model.id,
      cells: {
        model: model.id,
        calls: String(model.calls_count),
        tokens: tokenFigure(model),
        status: modelStatus(model),
      },
    })),
  );
</script>

<DashboardPage>
  <div slot="title" class="system-header__titles">
    <h1 class="system-title" tabindex="-1" bind:this={pageHeading}>{$t('system.view.title')}</h1>
    {#if snapshot?.runtime}
      <span class="system-subtitle mono">
        pm-mcpd {snapshot.runtime.version}
      </span>
    {/if}
  </div>

  <div slot="controls" class="system-header__controls">
    {#if projects.length > 0}
      <div class="project-filter-field">
        <SelectField
          label={$t('system.work.per_project')}
          testid="system-project-filter"
          value={selectedProject}
          options={projectOptions}
          onchange={(value) => handleProjectChange(value)}
        />
      </div>
    {/if}

    <SegmentedControl
      label={$t('system.view.window_label')}
      value={selectedWindow}
      options={windowOptions}
      onChange={(id) => handleWindowChange(id as 'hour' | 'today' | '7d')}
    />

    <IconButton
      icon="act.refresh"
      label={$t('system.view.refresh')}
      advisory={$t('system.view.refresh')}
      onclick={() => loadSnapshot(true)}
    />
  </div>

  <div class="system-container" data-testid="system-view">
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

    {#if snapshot?.runtime.upgrade_available_version && upgradePhase === 'idle' && !$runtimeStore.isDraining}
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
            <ExternalLink
              href={releaseNotesHref(snapshot.runtime.upgrade_release_notes_url)}
              label={$t('system.runtime.release_notes')}
            />
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
                {#if state === 'done'}<Icon
                    name="state.check"
                    size={16}
                  />{:else if state === 'running'}<Icon name="state.clock" size={16} />{:else}<Icon
                    name="state.dot"
                    size={16}
                  />{/if}
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
      <section
        class="upgrade-panel upgrade-panel--failed"
        role="alert"
        data-testid="upgrade-failed"
      >
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
          <Button variant="primary" onclick={() => maybeReloadAfterUpgrade(true)}>
            {$t('system.mismatch.load_again')}
          </Button>
        </div>
      </section>
    {/if}

    {#if isLoading && !snapshot}
      <div class="system-loading">
        <Skeleton shape="card" rows={3} label={$t('system.view.loading')} />
      </div>
    {:else if loadError !== null && !snapshot}
      <div class="notice-alert notice-alert--danger" data-testid="system-error">
        <ErrorState
          code={loadError}
          actionLabel={$t('common.action.retry')}
          onAct={() => loadSnapshot(true)}
          titleLevel={2}
          announce={snapshot !== null}
        />
      </div>
    {:else if $runtimeStore.isStopped}
      <div class="stopped-panel" role="region" aria-label={$t('system.stopped.region')}>
        <h2 class="stopped-title">
          {stoppedBy
            ? $t('system.stopped.title', { who: stoppedBy.who, time: stoppedBy.time })
            : $t('system.stopped.title_plain')}
        </h2>
        <p class="stopped-desc">
          {$t('system.stopped.how', { command: 'pm-operator runtime start' })}
        </p>
        <div class="copy-box">
          <code class="mono">{$t('system.stopped.command')}</code>
          <Button variant="secondary" onclick={() => void copyCommand('pm-operator runtime start')}>
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
            <span class="cleanup-banner__label"
              ><Icon name="act.clean" size={16} /> {$t('system.cleanup.title')}</span
            >
            <span class="cleanup-banner__text">
              {$t('system.cleanup.line_due', {
                what: $t('system.cleanup.summary.what', {
                  plans: snapshot.cleanup.plans_count,
                  epics: snapshot.cleanup.epics_count,
                  logs: snapshot.cleanup.logs_count,
                  reports: snapshot.cleanup.read_reports_count ?? 0,
                  projects: snapshot.cleanup.unenrolled_projects_count ?? 0,
                }),
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
            {#if showRuntimeActs}
              <div class="card-actions">
                <ActionBar acts={runtimeActs} busy={isExecutingAction} onAct={handleRuntimeAct} />
              </div>
            {/if}
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
                    <ExternalLink
                      href={releaseNotesHref(snapshot.runtime.upgrade_release_notes_url)}
                      label={$t('system.runtime.release_notes')}
                    />
                  {/if}
                </span>
              </div>
            {:else}
              <div class="metric-row">
                <span class="metric-label">{$t('system.runtime.field.update')}</span>
                <span class="metric-value text-muted">{$t('system.runtime.not_checked')}</span>
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
                  used: bytesFigure(snapshot.runtime.memory_used_bytes),
                  max: bytesFigure(snapshot.runtime.memory_max_bytes),
                  resident: bytesFigure(snapshot.runtime.memory_resident_bytes),
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
                  size: bytesFigure(snapshot.runtime.machine_store_bytes),
                  free: bytesFigure(snapshot.runtime.disk_free_bytes),
                  time:
                    formatTime(snapshot.runtime.disk_measured_at, 'short', $currentLocale) ||
                    $t('system.figure.unavailable'),
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
              <span class="text-muted text-sm">
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
                      <Button
                        variant="ghost"
                        size="sm"
                        aria-label={`${count} ${displayState('plan', st, $t)} — ${$t(
                          'system.work.link.plans',
                          {
                            state: displayState('plan', st, $t),
                            count,
                          },
                        )}`}
                        onclick={() => openOverview('plans')}
                      >
                        <CountBadge count={count} />
                        <StatusLabel tone={stateTone(st)} label={displayState('plan', st, $t)} />
                      </Button>
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
                      <Button
                        variant="ghost"
                        size="sm"
                        aria-label={`${count} ${displayState('epic', st, $t)} — ${$t(
                          'system.work.link.epics',
                          {
                            state: displayState('epic', st, $t),
                            count,
                          },
                        )}`}
                        onclick={() => openOverview('epics')}
                      >
                        <CountBadge count={count} />
                        <StatusLabel tone={stateTone(st)} label={displayState('epic', st, $t)} />
                      </Button>
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
                <Button
                  variant="ghost"
                  size="sm"
                  aria-label={`${$t('system.work.projects', {
                    count: snapshot.work.active_projects_count || 0,
                  })} — ${$t('system.work.link.projects', {
                    count: snapshot.work.active_projects_count || 0,
                  })}`}
                  onclick={() => navigateTo('projects')}
                >
                  {$t('system.work.projects', { count: snapshot.work.active_projects_count || 0 })}
                </Button>
              </span>
            </div>
          </div>
        </section>

        <!-- 3. Workers Card -->
        <section class="card" aria-labelledby="heading-workers">
          <div class="card-header">
            <h2 id="heading-workers" class="card-title">{$t('system.workers.title')}</h2>
            <CountBadge
              count={snapshot.workers.length}
              label={$t('system.workers.count', {
                count: snapshot.workers.length,
                busy: snapshot.workers.filter((w) => w.state.toLowerCase() === 'busy').length,
                idle: snapshot.workers.filter((w) => w.state.toLowerCase() === 'idle').length,
              })}
            />
          </div>

          <DataTable
            columns={workerColumns}
            rows={workerRows}
            label={$t('system.workers.title')}
            compact
          >
            <svelte:fragment slot="empty">
              <span class="text-muted text-center">{$t('system.workers.none')}</span>
            </svelte:fragment>
          </DataTable>
        </section>

        <!-- 4. Jobs Card -->
        <section class="card" aria-labelledby="heading-jobs">
          <div class="card-header">
            <h2 id="heading-jobs" class="card-title">{$t('system.jobs.title')}</h2>
            <CountBadge
              count={snapshot.jobs.locks_held || 0}
              label={$t('system.jobs.locks', { count: snapshot.jobs.locks_held || 0 })}
            />
          </div>

          <div class="metrics-list">
            <div class="metric-row">
              <span class="metric-label">{$t('system.jobs.field.build_slots')}</span>
              <span class="metric-value">
                {$t('system.jobs.slots', {
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
                {$t('system.models.window', {
                  window: selectedWindow === '7d' ? 'week' : selectedWindow,
                })}
              </span>
            </div>

            <SegmentedControl
              label={$t('system.models.grouping')}
              value={selectedGroupBy}
              options={groupOptions}
              onChange={(id) => handleGroupChange(id as 'model' | 'role')}
            />
          </div>

          <DataTable
            columns={modelColumns}
            rows={modelRows}
            label={$t('system.models.title')}
            compact
          >
            <svelte:fragment slot="empty">
              <span class="text-muted text-center">{$t('system.models.none')}</span>
            </svelte:fragment>
          </DataTable>
        </section>
      </div>
    {/if}
  </div>
</DashboardPage>

<!-- Upgrade Consent Dialogue -->
<Dialog
  open={upgradePhase === 'consent' && snapshot !== null}
  title={snapshot
    ? $t('system.upgrade.consent', {
        installed: snapshot.runtime.version,
        version: snapshot.runtime.upgrade_available_version ?? '',
      })
    : ''}
  danger
  testid="upgrade-consent"
  returnFocusElement={pageHeading}
  onClose={() => {
    upgradePhase = 'idle';
  }}
>
  {#if snapshot}
    <p class="text-muted text-sm">
      {$t('system.upgrade.explain', {
        installed: snapshot.runtime.version,
        version: snapshot.runtime.upgrade_available_version ?? '',
      })}
    </p>

    <h3 class="dialog-section-title">{$t('system.upgrade.check.title')}</h3>
    <ul class="upgrade-check-list" data-testid="upgrade-check-list">
      <li class="upgrade-check">
        <span class="upgrade-step__glyph" aria-hidden="true"
          ><Icon name="state.check" size={16} /></span
        >
        {$t('system.upgrade.check.running_workers', {
          count: snapshot.workers.filter((w) => w.state.toLowerCase() === 'busy').length,
        })}
      </li>
      <li class="upgrade-check">
        <span class="upgrade-step__glyph" aria-hidden="true"
          ><Icon name="state.check" size={16} /></span
        >
        {$t('system.upgrade.check.active_builds', {
          count: snapshot.jobs.build?.slots_used ?? 0,
        })}
      </li>
      <li class="upgrade-check">
        {#if snapshot.runtime.dirty_worktrees !== null && snapshot.runtime.dirty_worktrees !== undefined}
          <span class="upgrade-step__glyph" aria-hidden="true">
            <Icon
              name={snapshot.runtime.dirty_worktrees === 0 ? 'state.check' : 'state.error'}
              size={16}
            />
          </span>
          {$t('system.upgrade.check.dirty_worktrees', {
            count: snapshot.runtime.dirty_worktrees,
          })}
        {:else}
          <span class="upgrade-step__glyph" aria-hidden="true"
            ><Icon name="state.dot" size={16} /></span
          >
          {$t('system.upgrade.check.dirty_reported')}
        {/if}
      </li>
    </ul>

    <RadioGroup
      legend={$t('system.confirm.when')}
      name="upgradeWhen"
      value={modalWhen}
      options={whenOptions}
      onchange={(value) => {
        modalWhen = value as 'drain' | 'now';
      }}
    />
  {/if}

  <svelte:fragment slot="footer">
    <Button
      variant="ghost"
      onclick={() => {
        upgradePhase = 'idle';
      }}
    >
      {$t('system.confirm.not_now')}
    </Button>
    <Button variant="danger" loading={isExecutingAction} onclick={confirmUpgrade}>
      {$t('system.confirm.do', { act: 'upgrade', when: modalWhen })}
    </Button>
  </svelte:fragment>
</Dialog>

<!-- Modal Confirmation Dialogue -->
<ConfirmDialog
  open={activeModal !== null}
  title={$t('system.confirm.title', {
    act: activeModal ?? '',
    version: snapshot?.runtime.upgrade_available_version || '',
  })}
  consequences={[
    $t('system.confirm.running_what', {
      tasks: snapshot?.work.tasks_running || 0,
      projects: snapshot?.work.active_projects_count || 0,
    }),
    activeModal === 'stop' ? $t('system.confirm.stop_note') : $t('system.confirm.restart_note'),
  ]}
  actionLabel={$t('system.confirm.do', { act: activeModal ?? '', when: modalWhen })}
  cancelLabel={$t('system.confirm.keep')}
  busy={isExecutingAction}
  testid="lifecycle-confirm"
  onConfirm={executeLifecycleAction}
  onClose={closeModal}
>
  <RadioGroup
    legend={$t('system.confirm.when')}
    name="modalWhen"
    value={modalWhen}
    options={whenOptions}
    onchange={(value) => {
      modalWhen = value as 'drain' | 'now';
    }}
  />
</ConfirmDialog>

<style>
  .system-container {
    min-inline-size: 0;
    max-inline-size: 100%;
    width: 100%;
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
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    min-inline-size: 0;
    max-inline-size: 100%;
  }

  /* The project filter carries its own label through SelectField. */
  .project-filter-field {
    min-inline-size: 12rem;
  }

  .cleanup-banner {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-4);
    padding: var(--space-4) var(--space-6);
    background-color: var(--color-primary-subtle);
    border: 1px solid var(--color-primary);
    border-radius: var(--radius-lg);
    margin-bottom: var(--space-6);
  }

  .cleanup-banner__info {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  .cleanup-banner__label {
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
    min-inline-size: 0;
  }

  .card--full {
    grid-column: 1 / -1;
  }

  .card-header {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    align-items: center;
    gap: var(--space-3);
    border-bottom: 1px solid var(--color-border-subtle);
    padding-bottom: var(--space-3);
    min-inline-size: 0;
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
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .metrics-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .metric-row {
    display: flex;
    flex-wrap: wrap;
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

  .tags-row {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    justify-content: flex-end;
    align-items: center;
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

  .dialog-section-title {
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

  @media (max-width: 768px) {
    .system-grid {
      grid-template-columns: 1fr;
    }
  }

  @media (max-width: 480px) {
    .metric-row {
      flex-direction: column;
      align-items: flex-start;
    }
    .metric-value {
      text-align: left;
    }
  }
</style>
