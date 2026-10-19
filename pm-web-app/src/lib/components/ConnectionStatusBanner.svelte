<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import { apiClient, type ConnectionStatus } from '../api/client';

  /**
   * The connection of this browser to the machine: the listener it reached, and how many devices
   * can act on the machine.
   *
   * The status is read on mount, on a listener change, and on a heartbeat, so a machine that went
   * away or came back is seen without a reload.
   */
  const loopbackHosts = ['127.0.0.1', 'localhost', '::1', '[::1]'];
  const HEARTBEAT_MS = 30_000;

  let status: ConnectionStatus | null = null;
  let unsubscribe: (() => void) | null = null;
  let heartbeat: ReturnType<typeof setInterval> | null = null;
  let isMounted = true;

  onMount(() => {
    isMounted = true;
    void load();
    unsubscribe = apiClient.subscribeEvents((event) => {
      if (event === 'listener.changed' || event === 'device.paired' || event === 'device.revoked') {
        void load();
      }
    });
    heartbeat = setInterval(() => void load(), HEARTBEAT_MS);
  });

  onDestroy(() => {
    isMounted = false;
    if (unsubscribe) {
      unsubscribe();
      unsubscribe = null;
    }
    if (heartbeat) {
      clearInterval(heartbeat);
      heartbeat = null;
    }
  });

  async function load(): Promise<void> {
    try {
      const next = await apiClient.getConnectionStatus();
      if (isMounted) status = next;
    } catch {
      if (isMounted) status = null;
    }
  }

  $: isLoopback = typeof window !== 'undefined' && loopbackHosts.includes(window.location.hostname);
</script>

{#if status}
  <div
    class="connection-status-banner"
    data-testid="connection-status-banner"
    data-mode={isLoopback ? 'loopback' : 'lan'}
    role="status"
  >
    <span class="connection-status-banner__label">
      {isLoopback ? $t('entry.connection.loopback') : $t('entry.connection.lan')}
    </span>
    <span class="connection-status-banner__detail">
      {isLoopback ? $t('entry.connection.loopback.detail') : $t('entry.connection.lan.detail')}
    </span>
    <span class="connection-status-banner__devices">
      {$t('entry.connection.devices', { count: status.paired_devices_count })}
    </span>
  </div>
{/if}

<style>
  .connection-status-banner {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-2) var(--space-6);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    background-color: var(--color-bg-subtle);
    border-bottom: 1px solid var(--color-border-subtle);
  }

  .connection-status-banner[data-mode='lan'] {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning);
  }

  .connection-status-banner__label {
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .connection-status-banner[data-mode='lan'] .connection-status-banner__label {
    color: inherit;
  }

  @media (max-width: 480px) {
    .connection-status-banner__detail {
      display: none;
    }
  }
</style>
