<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import LanListenerModal from '../components/LanListenerModal.svelte';
  import CredentialEntry from '../components/CredentialEntry.svelte';
  import { apiClient, type PairedDevice } from '../api/client';
  import { currentLocale } from '../i18n';
  import { runtimeStore, navigateTo } from '../stores/runtime';

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

  onMount(() => {
    isMounted = true;
    unsubscribeEvents = apiClient.subscribeEvents((event, data) => {
      handleEvent(event, data);
    });
    void loadData();
  });

  onDestroy(() => {
    isMounted = false;
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
   * @returns the day and month, in the interface language
   */
  function formatDate(rawDate?: string): string {
    if (!rawDate) return '';
    const date = new Date(rawDate);
    if (Number.isNaN(date.getTime())) return rawDate;
    return date.toLocaleDateString($currentLocale, { day: 'numeric', month: 'short' });
  }

  /**
   * @param rawDate an ISO timestamp
   * @returns the day, month and time, in the interface language
   */
  function formatDateTime(rawDate?: string): string {
    if (!rawDate) return '';
    const date = new Date(rawDate);
    if (Number.isNaN(date.getTime())) return rawDate;
    return date.toLocaleString($currentLocale, {
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  $: otherDevicesCount = devices.filter((d) => !isThisBrowser(d)).length;
  $: thisLabel = devices.find((d) => isThisBrowser(d))?.label ?? $runtimeStore.deviceLabel;
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
      <Button variant="secondary" on:click={() => navigateTo('projects')}>
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
          variant="danger"
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
                <td class="text-secondary">
                  {$t('devices.list.last_used', { time: formatDateTime(device.last_used_at) })}
                </td>
                <td>
                  <div class="device-actions">
                    <Button
                      variant="ghost"
                      on:click={() => startRename(device)}
                      data-testid={`rename-btn-${device.device_id}`}
                    >
                      {$t('devices.act.rename')}
                    </Button>
                    <Button
                      variant="danger"
                      on:click={() => promptRevoke(device)}
                      data-testid={`revoke-btn-${device.device_id}`}
                    >
                      {$t('devices.act.revoke')}
                    </Button>
                  </div>
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

  {#if pendingRevokeTarget}
    <div class="modal-backdrop" role="dialog" aria-modal="true" data-testid="revoke-modal">
      <div class="modal-card">
        {#if pendingRevokeTarget === 'others'}
          <h2 class="modal-title">
            {$t('devices.revoke.others_title', {
              count: otherDevicesCount,
              label: thisLabel ?? '',
            })}
          </h2>
          <p class="modal-explain">
            {$t('devices.revoke.explain', { label: thisLabel ?? '' })}
          </p>
          <div class="modal-actions">
            <Button variant="ghost" on:click={cancelRevoke} data-testid="revoke-cancel-button">
              {$t('devices.revoke.keep')}
            </Button>
            <Button variant="danger" on:click={confirmRevoke} data-testid="revoke-confirm-button">
              {$t('devices.revoke.action')}
            </Button>
          </div>
        {:else}
          {@const targetDev = pendingRevokeTarget}
          {@const isCurrent = isThisBrowser(targetDev)}
          <h2 class="modal-title">
            {$t('devices.revoke.title', { label: targetDev.label })}
          </h2>
          <p class="modal-explain">
            {#if isCurrent}
              {$t('devices.revoke.this_one')}
            {:else}
              {$t('devices.revoke.explain', { label: targetDev.label })}
            {/if}
          </p>
          <div class="modal-actions">
            <Button variant="ghost" on:click={cancelRevoke} data-testid="revoke-cancel-button">
              {$t('devices.revoke.keep')}
            </Button>
            <Button variant="danger" on:click={confirmRevoke} data-testid="revoke-confirm-button">
              {$t('devices.revoke.action')}
            </Button>
          </div>
        {/if}
      </div>
    </div>
  {/if}
</div>

<style>
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

  .device-actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
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
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-6);
    max-width: 32rem;
    width: 100%;
    box-shadow: var(--shadow-md);
  }

  .modal-title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0 0 var(--space-2) 0;
  }

  .modal-explain {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    line-height: 1.5;
    margin: 0 0 var(--space-6) 0;
  }

  .modal-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: var(--space-3);
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

    .modal-actions {
      flex-direction: column-reverse;
      align-items: stretch;
    }
  }
</style>
