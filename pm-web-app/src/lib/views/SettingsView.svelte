<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import ActionBar from '../components/ActionBar.svelte';
  import ConfirmDialog from '../components/ConfirmDialog.svelte';
  import LanListenerModal from '../components/LanListenerModal.svelte';
  import CredentialEntry from '../components/CredentialEntry.svelte';
  import { DEVICE_ACTS } from '../actions/byState';
  import { apiClient, type PairedDevice } from '../api/client';
  import { currentLocale } from '../i18n';
  import { formatDay, formatTime } from '../display/format';
  import { runtimeStore, navigateTo, goBack } from '../stores/runtime';

  /**
   * The paired devices of the machine (UC-ENT-2): the list, renaming one inline, revoking one,
   * revoking all others, and the network listener with its warning view.
   */
  let devices: PairedDevice[] = [];
  let isLoading = false;
  let loadError = false;
  let actionError = '';
  let listenerNotice = '';
  let renamingId: string | null = null;
  let renameValue = '';
  let lanListenerOn = true;
  let lanModalOpen = false;
  let pendingRevokeTarget: PairedDevice | 'others' | null = null;

  let unsubscribeEvents: (() => void) | null = null;
  let isMounted = true;
  /** The moment the ages are measured at; a quiet timer moves it so an idle list stays true. */
  let clock = Date.now();
  let clockTimer: ReturnType<typeof setInterval> | null = null;

  onMount(() => {
    isMounted = true;
    unsubscribeEvents = apiClient.subscribeEvents((event, data) => {
      handleEvent(event, data);
    });
    clockTimer = setInterval(() => {
      clock = Date.now();
    }, 30_000);
    void loadData();
  });

  onDestroy(() => {
    isMounted = false;
    if (clockTimer !== null) {
      clearInterval(clockTimer);
      clockTimer = null;
    }
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  function handleEvent(event: string, data: unknown) {
    if (event === 'device.renamed') {
      const payload = data as { device_id: string; label: string };
      if (payload?.device_id && payload?.label) {
        devices = devices.map((d) =>
          d.device_id === payload.device_id ? { ...d, label: payload.label } : d,
        );
      }
    } else if (event === 'device.revoked') {
      const payload = data as { device_id: string };
      if (payload?.device_id) {
        devices = devices.filter((d) => d.device_id !== payload.device_id);
      }
    }
  }

  async function loadData() {
    isLoading = true;
    loadError = false;
    try {
      const [devList, status] = await Promise.all([
        apiClient.getDevices(),
        apiClient.getStatus().catch(() => null),
      ]);
      if (!isMounted) return;
      devices = Array.isArray(devList) ? devList : [];
      if (status?.web && typeof status.web.lan === 'boolean') {
        lanListenerOn = status.web.lan;
      }
    } catch {
      if (!isMounted) return;
      loadError = true;
      devices = [];
    } finally {
      if (isMounted) {
        isLoading = false;
      }
    }
  }

  function isThisBrowser(device: PairedDevice): boolean {
    const currentId = apiClient.getDeviceId();
    if (currentId) return device.device_id === currentId;
    const currentLabel = $runtimeStore.deviceLabel || apiClient.getLabel();
    return Boolean(currentLabel && device.label === currentLabel);
  }

  function startRename(device: PairedDevice) {
    renamingId = device.device_id;
    renameValue = device.label;
    actionError = '';
  }

  async function saveRename(deviceId: string) {
    if (!renameValue.trim()) return;
    actionError = '';
    try {
      await apiClient.renameDevice(deviceId, renameValue.trim());
      devices = devices.map((d) =>
        d.device_id === deviceId ? { ...d, label: renameValue.trim() } : d,
      );
      renamingId = null;
    } catch {
      actionError = $t('web.settings.devices.rename_failed');
    }
  }

  function promptRevoke(target: PairedDevice | 'others') {
    actionError = '';
    pendingRevokeTarget = target;
  }

  function cancelRevoke() {
    pendingRevokeTarget = null;
  }

  async function confirmRevoke() {
    if (!pendingRevokeTarget) return;
    actionError = '';

    if (pendingRevokeTarget === 'others') {
      try {
        await apiClient.revokeOtherDevices();
        devices = devices.filter((d) => isThisBrowser(d));
        pendingRevokeTarget = null;
      } catch {
        actionError = $t('web.settings.devices.revoke_failed');
      }
      return;
    }

    const device = pendingRevokeTarget;
    const isCurrent = isThisBrowser(device);
    try {
      await apiClient.revokeDevice(device.device_id);
      devices = devices.filter((d) => d.device_id !== device.device_id);
      pendingRevokeTarget = null;
      if (isCurrent) {
        apiClient.clearAuth();
        runtimeStore.update((s) => ({
          ...s,
          isPaired: false,
          isRevoked: true,
          revokedReason: 'device_revoked',
          revokedInfo: {
            reason: 'device_revoked',
            revoked_by: $runtimeStore.deviceLabel,
            details: $t('web.settings.devices.revoked_this'),
          },
          currentRoute: 'revoked',
        }));
      }
    } catch {
      actionError = $t('web.settings.devices.revoke_failed');
    }
  }

  async function turnListenerOff() {
    actionError = '';
    try {
      await apiClient.turnOffLanListener();
      lanListenerOn = false;
      listenerNotice = $t('devices.listener_off.done');
    } catch {
      actionError = $t('web.settings.devices.listener_off_failed');
    }
  }

  /**
   * @param rawDate an ISO timestamp
   * @returns the day, in the interface language
   */
  function formatDate(rawDate?: string): string {
    return formatDay(rawDate, $currentLocale);
  }

  /**
   * @param rawDate an ISO timestamp
   * @returns its age in the unit that age has, measured at the list's own clock
   */
  function formatRelative(rawDate?: string): string {
    return formatTime(rawDate, 'relative', $currentLocale, new Date(clock));
  }

  /**
   * @param rawDate an ISO timestamp
   * @returns the moment to the minute, in the interface language
   */
  function formatDateTime(rawDate?: string): string {
    return formatTime(rawDate, 'short', $currentLocale);
  }

  $: otherDevicesCount = devices.filter((d) => !isThisBrowser(d)).length;
  $: thisLabel = devices.find((d) => isThisBrowser(d))?.label ?? $runtimeStore.deviceLabel;

  /**
   * @param device a paired device
   * @returns the acts the device's row offers (UX-P-04)
   */
  function deviceActs(device: PairedDevice) {
    const state = isThisBrowser(device) ? 'this_device' : 'other_device';
    return DEVICE_ACTS[state]
      .filter((act) => act.id !== 'revoke_others')
      .map((act) => ({
        id: act.id,
        label: $t(act.labelKey),
        advisory: act.advisoryKey ? $t(act.advisoryKey) : undefined,
        role: act.role,
        destructive: act.destructive,
      }));
  }

  /**
   * @param device a paired device
   * @param id the act its row reported
   */
  function chooseDeviceAct(device: PairedDevice, id: string): void {
    if (id === 'rename') {
      startRename(device);
      return;
    }
    if (id === 'revoke') {
      promptRevoke(device);
    }
  }

  /** The confirmation the revoke asks for, with the name to type where the act needs one. */
  function revokeTitle(): string {
    if (pendingRevokeTarget === 'others') {
      return $t('devices.revoke.others_title', {
        count: otherDevicesCount,
        label: thisLabel ?? '',
      });
    }
    return $t('devices.revoke.title', { label: pendingRevokeTarget?.label ?? '' });
  }

  /** @returns what the revoke costs, in the words of the device it ends */
  function revokeConsequences(): string[] {
    if (pendingRevokeTarget === 'others') {
      return [$t('devices.revoke.explain', { label: thisLabel ?? '' })];
    }
    if (pendingRevokeTarget && isThisBrowser(pendingRevokeTarget)) {
      return [$t('devices.revoke.this_one')];
    }
    return [$t('devices.revoke.explain', { label: pendingRevokeTarget?.label ?? '' })];
  }

  // The title and the consequences name the target, so they follow `pendingRevokeTarget`.
  $: revokeTitleText = pendingRevokeTarget === null ? '' : revokeTitle();
  $: revokeConsequenceList = pendingRevokeTarget === null ? [] : revokeConsequences();
</script>

<div class="settings-view" data-testid="settings-view">
  <div class="settings-view__header">
    <h1 class="settings-view__title">{$t('web.settings.title')}</h1>
    <div class="settings-view__actions">
      <Button
        variant="secondary"
        on:click={() => navigateTo('machine-settings')}
        data-testid="machine-settings-button"
      >
        {$t('web.settings.page.title')}
      </Button>
      <Button
        variant="secondary"
        on:click={() => {
          if (!goBack()) {
            navigateTo('projects');
          }
        }}
        data-testid="settings-back-button"
      >
        {$t('web.common.back')}
      </Button>
    </div>
  </div>

  <section class="settings-section" data-testid="paired-devices-section">
    <div class="section-header">
      <div>
        <h2 class="settings-section__title">{$t('devices.list.title')}</h2>
        <p class="settings-section__subtitle">{$t('devices.list.subtitle')}</p>
      </div>
      {#if otherDevicesCount > 0}
        <Button
          variant="secondary"
          on:click={() => promptRevoke('others')}
          data-testid="revoke-others-button"
        >
          {$t('devices.act.revoke_others')}
        </Button>
      {/if}
    </div>

    <div class="devices-card">
      {#if actionError}
        <p class="error-text" role="alert">{actionError}</p>
      {/if}
      {#if isLoading}
        <p class="loading-text">{$t('web.settings.devices.loading')}</p>
      {:else if loadError}
        <p class="empty-text error-text">{$t('web.settings.devices.load_failed')}</p>
      {:else if devices.length === 0}
        <p class="empty-text" data-testid="empty-devices">{$t('devices.list.empty')}</p>
      {:else}
        <table class="devices-table" data-testid="devices-table">
          <thead>
            <tr>
              <th>{$t('web.settings.devices.column_device')}</th>
              <th>{$t('web.settings.devices.column_paired')}</th>
              <th>{$t('web.settings.devices.column_last_used')}</th>
              <th>{$t('web.settings.devices.column_action')}</th>
            </tr>
          </thead>
          <tbody>
            {#each devices as device}
              <tr data-testid={`device-row-${device.device_id}`}>
                <td>
                  {#if renamingId === device.device_id}
                    <div class="rename-inline">
                      <input
                        type="text"
                        bind:value={renameValue}
                        class="rename-input"
                        data-testid={`rename-input-${device.device_id}`}
                        aria-label={$t('web.settings.devices.new_label')}
                      />
                      <Button
                        variant="primary"
                        on:click={() => saveRename(device.device_id)}
                        data-testid={`save-rename-${device.device_id}`}
                      >
                        {$t('web.common.save')}
                      </Button>
                      <Button variant="ghost" on:click={() => (renamingId = null)}>
                        {$t('web.settings.devices.cancel')}
                      </Button>
                    </div>
                  {:else}
                    <strong class="device-label">{device.label}</strong>
                    {#if isThisBrowser(device)}
                      <span class="badge-current" data-testid="this-browser-badge">
                        {$t('devices.list.this_one')}
                      </span>
                    {/if}
                  {/if}
                </td>
                <td class="text-secondary">
                  {$t('devices.list.paired', { date: formatDate(device.created_at) })}
                  {#if device.paired_from}
                    · {$t('devices.list.from', { from: device.paired_from })}
                  {/if}
                </td>
                <td class="text-secondary" title={formatDateTime(device.last_used_at)}>
                  <span class="visually-hidden">{formatDateTime(device.last_used_at)}</span>
                  {$t('devices.list.last_used', { time: formatRelative(device.last_used_at) })}
                </td>
                <td>
                  <ActionBar
                    acts={deviceActs(device)}
                    onAct={(id) => chooseDeviceAct(device, id)}
                  />
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      {/if}
    </div>

    <p class="terminal-note">{$t('devices.list.terminal_note')}</p>

    <div class="listener-bar" data-testid="listener-bar">
      <span class="listener-status" data-testid="listener-status">
        {$t('devices.list.listener', { state: lanListenerOn ? 'on' : 'off' })}.
      </span>
      <div class="listener-actions">
        <Button
          variant="ghost"
          on:click={() => (lanModalOpen = true)}
          data-testid="lan-details-button"
        >
          {$t('entry.lan.title')}
        </Button>
        {#if lanListenerOn}
          <Button variant="secondary" on:click={turnListenerOff} data-testid="listener-off-button">
            {$t('devices.act.listener_off')}
          </Button>
        {/if}
      </div>
    </div>
    {#if listenerNotice}
      <p class="notice-text" data-testid="listener-notice">{listenerNotice}</p>
    {/if}
  </section>

  <section class="settings-section" data-testid="credentials-section">
    <h2 class="settings-section__title">{$t('onboard.step.credentials')}</h2>
    <CredentialEntry
      credentialKey="github"
      label={$t('web.settings.credentials.provider.github')}
    />
  </section>

  {#if lanModalOpen}
    <LanListenerModal onClose={() => (lanModalOpen = false)} onChanged={loadData} />
  {/if}

  <ConfirmDialog
    open={pendingRevokeTarget !== null}
    title={revokeTitleText}
    consequences={revokeConsequenceList}
    actionLabel={$t('devices.revoke.action')}
    cancelLabel={$t('devices.revoke.keep')}
    typedName={pendingRevokeTarget === 'others' ? (thisLabel ?? '') : undefined}
    onConfirm={confirmRevoke}
    onClose={cancelRevoke}
  />
</div>

<style>
  .visually-hidden {
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

  .settings-view {
    max-width: 64rem;
    margin: 0 auto;
    padding: var(--space-8) var(--space-4);
  }

  .settings-view__header {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-8);
  }

  .settings-view__title {
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .settings-view__actions {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .settings-section {
    margin-bottom: var(--space-8);
  }

  .section-header {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-4);
  }

  .settings-section__title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .settings-section__subtitle {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .devices-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    overflow-x: auto;
  }

  .devices-table {
    width: 100%;
    border-collapse: collapse;
    font-size: var(--text-sm);
  }

  .devices-table th,
  .devices-table td {
    padding: var(--space-3) var(--space-4);
    text-align: left;
    border-bottom: 1px solid var(--color-border-subtle);
  }

  .devices-table th {
    background-color: var(--color-bg-subtle);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .device-label {
    color: var(--color-text-primary);
  }

  .text-secondary {
    color: var(--color-text-secondary);
  }

  .badge-current {
    display: inline-block;
    padding: 2px 6px;
    font-size: var(--text-xs);
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
    border-radius: var(--radius-sm);
    margin-left: var(--space-2);
    font-weight: 500;
  }

  .rename-inline {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .rename-input {
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-sm);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
  }

  .terminal-note {
    margin-top: var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .listener-bar {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    align-items: center;
    justify-content: space-between;
    margin-top: var(--space-4);
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    border: 1px solid var(--color-border-subtle);
  }

  .listener-status {
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .listener-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .notice-text {
    font-size: var(--text-sm);
    color: var(--color-primary);
    margin-top: var(--space-2);
  }

  .error-text {
    color: var(--color-danger);
    font-size: var(--text-sm);
    padding: var(--space-3) var(--space-4);
    margin: 0;
  }

  .empty-text {
    padding: var(--space-6) var(--space-4);
    text-align: center;
    color: var(--color-text-secondary);
  }

  .loading-text {
    padding: var(--space-6) var(--space-4);
    text-align: center;
    color: var(--color-text-secondary);
  }

  @media (max-width: 768px) {
    .settings-view {
      padding: var(--space-4) var(--space-3);
    }

    .devices-table th:nth-child(2),
    .devices-table td:nth-child(2),
    .devices-table th:nth-child(3),
    .devices-table td:nth-child(3) {
      display: none;
    }
  }
</style>
