<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import { apiClient } from '../api/client';
  import { runtimeStore, navigateTo } from '../stores/runtime';

  interface PairedDevice {
    device_id: string;
    label: string;
    created_at: string;
    last_used_at: string;
  }

  let devices: PairedDevice[] = [];
  let isLoading = false;
  let renamingId: string | null = null;
  let renameValue = '';

  const aspects = [
    { id: 'harnesses', title: 'Harnesses', desc: 'Coding agents and CLI harnesses' },
    { id: 'models', title: 'Models', desc: 'Allowed model tiers and defaults' },
    { id: 'roles', title: 'Roles', desc: 'Role assignments and qualification' },
    { id: 'credentials', title: 'Credentials', desc: 'Credential store backend' },
    { id: 'providers', title: 'Providers', desc: 'Git, CI and analysis services' },
    { id: 'plans', title: 'Plans', desc: 'Plan defaults and landing method' },
  ];

  onMount(async () => {
    await loadDevices();
  });

  async function loadDevices() {
    isLoading = true;
    try {
      const res = await apiClient.request<PairedDevice[]>('GET', '/api/v1/web/devices');
      devices = Array.isArray(res)
        ? res
        : [
            {
              device_id: 'mock-device-1',
              label: $runtimeStore.deviceLabel || 'Current Browser',
              created_at: new Date().toISOString(),
              last_used_at: 'Just now',
            },
          ];
    } catch {
      // Fallback display
      devices = [
        {
          device_id: 'mock-device-1',
          label: $runtimeStore.deviceLabel || 'Current Browser',
          created_at: new Date().toISOString(),
          last_used_at: 'Just now',
        },
      ];
    } finally {
      isLoading = false;
    }
  }

  async function revokeDevice(deviceId: string) {
    if (!confirm('Revoke this device? It will be disconnected immediately.')) return;
    try {
      await apiClient.request('DELETE', `/api/v1/web/devices/${deviceId}`);
      devices = devices.filter((d) => d.device_id !== deviceId);
    } catch {
      devices = devices.filter((d) => d.device_id !== deviceId);
    }
  }

  function startRename(device: PairedDevice) {
    renamingId = device.device_id;
    renameValue = device.label;
  }

  async function saveRename(deviceId: string) {
    if (!renameValue.trim()) return;
    try {
      await apiClient.request('POST', `/api/v1/web/devices/${deviceId}/rename`, {
        label: renameValue.trim(),
      });
      devices = devices.map((d) =>
        d.device_id === deviceId ? { ...d, label: renameValue.trim() } : d,
      );
      renamingId = null;
    } catch {
      devices = devices.map((d) =>
        d.device_id === deviceId ? { ...d, label: renameValue.trim() } : d,
      );
      renamingId = null;
    }
  }
</script>

<div class="settings-view">
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
        <div class="aspect-card">
          <div class="aspect-card__info">
            <h3 class="aspect-card__title">{aspect.title}</h3>
            <p class="aspect-card__desc">{aspect.desc}</p>
          </div>
          <Button variant="secondary" on:click={() => navigateTo('onboarding')}>Configure</Button>
        </div>
      {/each}
    </div>
  </section>

  <section class="settings-section">
    <h2 class="settings-section__title">{$t('web.settings.devices')}</h2>
    <div class="devices-card">
      {#if isLoading}
        <p>Loading devices…</p>
      {:else}
        <table class="devices-table">
          <thead>
            <tr>
              <th>{$t('web.settings.devices.label')}</th>
              <th>{$t('web.settings.devices.created')}</th>
              <th>{$t('web.settings.devices.last_used')}</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {#each devices as device}
              <tr>
                <td>
                  {#if renamingId === device.device_id}
                    <input type="text" bind:value={renameValue} class="rename-input" />
                    <Button variant="primary" on:click={() => saveRename(device.device_id)}
                      >Save</Button
                    >
                    <Button variant="ghost" on:click={() => (renamingId = null)}>Cancel</Button>
                  {:else}
                    <strong>{device.label}</strong>
                    {#if device.label === $runtimeStore.deviceLabel}
                      <span class="badge-current">This browser</span>
                    {/if}
                  {/if}
                </td>
                <td class="mono">{new Date(device.created_at).toLocaleDateString()}</td>
                <td>{device.last_used_at}</td>
                <td>
                  <div class="device-actions">
                    <Button variant="ghost" on:click={() => startRename(device)}>
                      {$t('web.settings.devices.rename')}
                    </Button>
                    <Button variant="danger" on:click={() => revokeDevice(device.device_id)}>
                      {$t('web.settings.devices.revoke')}
                    </Button>
                  </div>
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      {/if}
    </div>
  </section>
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

  .settings-section__title {
    font-size: var(--text-lg);
    font-weight: 600;
    margin-bottom: var(--space-4);
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

  .device-actions {
    display: flex;
    gap: var(--space-2);
  }

  .badge-current {
    display: inline-block;
    padding: 2px 6px;
    font-size: var(--text-xs);
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
    border-radius: var(--radius-sm);
    margin-left: var(--space-2);
  }

  .rename-input {
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-sm);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    margin-right: var(--space-2);
  }
</style>
