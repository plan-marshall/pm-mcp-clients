<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import { apiClient, type PairedDevice } from '../api/client';
  import { runtimeStore, navigateTo } from '../stores/runtime';

  let devices: PairedDevice[] = [];
  let isLoading = false;
  let loadError = '';
  let actionError = '';
  let listenerNotice = '';
  let renamingId: string | null = null;
  let renameValue = '';
  let lanListenerOn = true;

  // Revocation modal state
  let pendingRevokeTarget: PairedDevice | 'others' | null = null;

  let unsubscribeEvents: (() => void) | null = null;

  const aspects = [
    { id: 'harnesses', title: 'Harnesses', desc: 'Coding agents and CLI harnesses' },
    { id: 'models', title: 'Models', desc: 'Allowed model tiers and defaults' },
    { id: 'roles', title: 'Roles', desc: 'Role assignments and qualification' },
    { id: 'credentials', title: 'Credentials', desc: 'Credential store backend' },
    { id: 'providers', title: 'Providers', desc: 'Git, CI and analysis services' },
    { id: 'plans', title: 'Plans', desc: 'Plan defaults and landing method' },
  ];

  onMount(async () => {
    await loadData();
    unsubscribeEvents = apiClient.subscribeEvents((event, data) => {
      handleEvent(event, data);
    });
  });

  onDestroy(() => {
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
    loadError = '';
    try {
      const [devList, status] = await Promise.all([
        apiClient.getDevices(),
        apiClient.getStatus().catch(() => null),
      ]);
      devices = Array.isArray(devList) ? devList : [];
      if (status?.web && typeof status.web.lan === 'boolean') {
        lanListenerOn = status.web.lan;
      }
    } catch {
      loadError = 'Failed to load paired devices.';
      devices = [];
    } finally {
      isLoading = false;
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
      actionError = 'Failed to rename device. Please try again.';
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
        actionError = 'Failed to revoke other devices. Please try again.';
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
          revokedInfo: { reason: 'device_revoked', revoked_by: 'this browser', details: 'Revoked' },
          currentRoute: 'revoked',
        }));
      }
    } catch {
      actionError = 'Failed to revoke device. Please try again.';
    }
  }

  async function turnListenerOff() {
    actionError = '';
    try {
      await apiClient.turnOffLanListener();
      lanListenerOn = false;
      listenerNotice =
        $t('devices.listener_off.done') ||
        'The network listener is off; the devices on the network stay paired and work again once it is on.';
    } catch {
      actionError = 'Failed to turn off network listener.';
    }
  }

  function formatPaired(rawDate?: string, from?: string): string {
    const fromStr = from === 'network' ? 'from network' : 'from loopback';
    if (!rawDate) return `paired recently ${fromStr}`;
    try {
      const d = new Date(rawDate);
      const day = d.getUTCDate();
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const month = months[d.getUTCMonth()];
      return `paired ${day} ${month} ${fromStr}`;
    } catch {
      return `paired ${rawDate} ${fromStr}`;
    }
  }

  function formatLastUsed(rawDate?: string): string {
    if (!rawDate) return 'just now';
    try {
      const d = new Date(rawDate);
      if (isNaN(d.getTime())) return rawDate;
      const now = Date.now();
      const diffMs = now - d.getTime();
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
      if (diffDays === 0 && diffMs >= 0) {
        const hours = String(d.getUTCHours()).padStart(2, '0');
        const mins = String(d.getUTCMinutes()).padStart(2, '0');
        return `today ${hours}:${mins}`;
      }
      if (diffDays >= 1 && diffDays < 7) {
        return `${diffDays} d ago`;
      }
      const weeks = Math.floor(diffDays / 7);
      if (weeks >= 1 && weeks < 8) {
        return `${weeks} ${weeks === 1 ? 'week' : 'weeks'} ago`;
      }
      const day = d.getUTCDate();
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const month = months[d.getUTCMonth()];
      const hours = String(d.getUTCHours()).padStart(2, '0');
      const mins = String(d.getUTCMinutes()).padStart(2, '0');
      return `${day} ${month} ${hours}:${mins}`;
    } catch {
      return rawDate;
    }
  }

  $: otherDevicesCount = devices.filter((d) => !isThisBrowser(d)).length;
</script>

<div class="settings-view" data-testid="settings-view">
  <div class="settings-view__header">
    <h1 class="settings-view__title">{$t('web.settings.title')}</h1>
    <Button variant="secondary" on:click={() => navigateTo('workspace')}>
      {$t('web.common.back')}
    </Button>
  </div>

  <section class="settings-section">
    <h2 class="settings-section__title">{$t('web.settings.aspects')}</h2>
    <div class="aspects-grid">
      {#each aspects as aspect}
        <div class="aspect-card" data-aspect={aspect.id}>
          <div class="aspect-card__info">
            <h3 class="aspect-card__title">{aspect.title}</h3>
            <p class="aspect-card__desc">{aspect.desc}</p>
          </div>
          <Button
            variant="secondary"
            aria-label={`Configure ${aspect.title}`}
            on:click={() => navigateTo('onboarding')}
          >
            Configure
          </Button>
        </div>
      {/each}
    </div>
  </section>

  <section class="settings-section" data-testid="paired-devices-section">
    <div class="section-header">
      <div>
        <h2 class="settings-section__title">
          {$t('devices.list.title')} · {$t('devices.list.subtitle')}
        </h2>
      </div>
      {#if otherDevicesCount > 0}
        <Button variant="danger" on:click={() => promptRevoke('others')} data-testid="revoke-others-button">
          {$t('devices.act.revoke_others')}
        </Button>
      {/if}
    </div>

    <div class="devices-card">
      {#if actionError}
        <p class="error-text" role="alert">{actionError}</p>
      {/if}
      {#if isLoading}
        <p class="loading-text">Loading devices…</p>
      {:else if loadError}
        <p class="empty-text error-text">{loadError}</p>
      {:else if devices.length === 0}
        <p class="empty-text" data-testid="empty-devices">{$t('devices.list.empty')}</p>
      {:else}
        <table class="devices-table" data-testid="devices-table">
          <thead>
            <tr>
              <th>Device</th>
              <th>Paired</th>
              <th>Last Used</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {#each devices as device, idx}
              <tr data-testid={`device-row-${device.device_id}`}>
                <td>
                  {#if renamingId === device.device_id}
                    <div class="rename-inline">
                      <input
                        type="text"
                        bind:value={renameValue}
                        class="rename-input"
                        data-testid={`rename-input-${device.device_id}`}
                        aria-label="New device label"
                      />
                      <Button
                        variant="primary"
                        on:click={() => saveRename(device.device_id)}
                        data-testid={`save-rename-${device.device_id}`}
                      >
                        Save
                      </Button>
                      <Button variant="ghost" on:click={() => (renamingId = null)}>
                        Cancel
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
                  {formatPaired(device.created_at, device.paired_from)}
                </td>
                <td class="text-secondary">{formatLastUsed(device.last_used_at)}</td>
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

    <!-- Network listener info & control -->
    <div class="listener-bar" data-testid="listener-bar">
      <span class="listener-status" data-testid="listener-status">
        Network listener: <strong>{lanListenerOn ? 'on' : 'off'}</strong>.
      </span>
      {#if lanListenerOn}
        <Button variant="secondary" on:click={turnListenerOff} data-testid="listener-off-button">
          {$t('devices.act.listener_off')}
        </Button>
      {/if}
    </div>
    {#if listenerNotice}
      <p class="notice-text" data-testid="listener-notice">{listenerNotice}</p>
    {/if}
  </section>

  <!-- Revocation Confirmation Modal -->
  {#if pendingRevokeTarget}
    <div class="modal-backdrop" role="dialog" aria-modal="true" data-testid="revoke-modal">
      <div class="modal-card">
        {#if pendingRevokeTarget === 'others'}
          <h3 class="modal-title">
            Revoke all {otherDevicesCount} other paired {otherDevicesCount === 1 ? 'device' : 'devices'}?
          </h3>
          <p class="modal-explain">
            {($runtimeStore.deviceLabel || 'This browser')} stays paired; every other browser is signed out.
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
          <h3 class="modal-title">
            {$t('devices.revoke.title', { label: targetDev.label })}
          </h3>
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
    max-width: 1000px;
    margin: 0 auto;
    padding: var(--space-8) var(--space-4);
  }

  .settings-view__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-8);
  }

  .settings-view__title {
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .settings-section {
    margin-bottom: var(--space-8);
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-4);
  }

  .settings-section__title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .aspects-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: var(--space-4);
  }

  .aspect-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-4);
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
  }

  .aspect-card__title {
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .aspect-card__desc {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .devices-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    overflow: hidden;
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

  .listener-bar {
    display: flex;
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

  /* Modal */
  .modal-backdrop {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 100;
  }

  .modal-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-6);
    max-width: 500px;
    width: 90%;
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
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
    justify-content: flex-end;
    gap: var(--space-3);
  }
</style>
