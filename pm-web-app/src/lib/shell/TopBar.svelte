<script lang="ts">
  import { runtimeStore } from '../stores/runtime';
  import { openCommandPalette } from '../stores/searchIndex';
  import { apiClient, type ConnectionStatus } from '../api/client';
  import { t } from '../i18n';
  import { pathOf } from './nav';
  import Breadcrumb, { type Crumb } from '../components/Breadcrumb.svelte';
  import Button from '../components/Button.svelte';
  import LiveIndicator from './LiveIndicator.svelte';

  /**
   * The top bar of the shell (UX-P-02, UX-P-12): where the person stands, the way into the search,
   * the live indicator, and the machine in a neutral pill.
   *
   * The path is the one trail the whole app draws; the pill stays neutral in both connection modes,
   * because a machine that is simply named never reads as a warning.
   */
  const loopbackHosts = ['127.0.0.1', 'localhost', '::1', '[::1]'];
  const CONNECTION_HEARTBEAT_MS = 30_000;

  /** How this browser reaches the machine: the loopback listener, or the network one. */
  const isLoopback =
    typeof window !== 'undefined' && loopbackHosts.includes(window.location.hostname);
  const connectionMode = isLoopback ? 'loopback' : 'lan';

  /** The connection of this browser to the machine: devices that can act, and the mode. */
  let connectionStatus = $state<ConnectionStatus | null>(null);

  const connectionActive = $derived($runtimeStore.isInitialized && $runtimeStore.isPaired);

  const connectionTitle = $derived.by(() => {
    const detail = $t(
      isLoopback ? 'entry.connection.loopback.detail' : 'entry.connection.lan.detail',
    );
    if (!connectionStatus) {
      return detail;
    }
    const devices = $t('entry.connection.devices', {
      count: connectionStatus.paired_devices_count,
    });
    return `${detail} · ${devices}`;
  });

  const crumbs = $derived(
    pathOf($runtimeStore.currentRoute, $runtimeStore).map(
      (crumb): Crumb => ({
        label: $t(crumb.key, crumb.args),
        id: crumb.id,
      }),
    ),
  );

  // The connection status follows the session: read on mount, on a listener/device event, and on a
  // heartbeat, so a machine that went away or came back is seen without a reload. Only the latest
  // request may write, so an older answer that resolves last cannot replace a newer count.
  $effect(() => {
    if (!connectionActive) {
      return;
    }
    let mounted = true;
    let latestRequest = 0;
    const load = async (): Promise<void> => {
      const request = ++latestRequest;
      try {
        const next = await apiClient.getConnectionStatus();
        if (mounted && request === latestRequest) connectionStatus = next;
      } catch {
        if (mounted && request === latestRequest) connectionStatus = null;
      }
    };
    void load();
    const unsubscribe = apiClient.subscribeEvents((event) => {
      if (event === 'listener.changed' || event === 'device.paired' || event === 'device.revoked') {
        void load();
      }
    });
    const heartbeat = setInterval(() => void load(), CONNECTION_HEARTBEAT_MS);
    return () => {
      mounted = false;
      unsubscribe();
      clearInterval(heartbeat);
    };
  });
</script>

<header class="app-header" data-testid="top-bar">
  <div class="app-header__path">
    <Breadcrumb {crumbs} label={$t('web.frame.path.label')} />
  </div>

  <div class="app-header__actions">
    <Button
      variant="ghost"
      size="sm"
      icon="act.search"
      title={$t('web.frame.search.title')}
      onclick={openCommandPalette}
      data-testid="search-trigger"
    >
      {$t('web.frame.search')}
    </Button>

    <LiveIndicator />

    {#if $runtimeStore.isPaired}
      <span
        class="app-header__machine mono"
        title={connectionTitle}
        data-connection-mode={connectionMode}
        data-testid="header-machine"
      >
        {$t('web.frame.machine', { name: $runtimeStore.machineName })}
      </span>
    {/if}
  </div>
</header>

<style>
  .app-header {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    justify-content: space-between;
    align-items: center;
    padding: var(--space-2) var(--space-6);
    background-color: var(--color-bg-surface);
    border-block-end: 1px solid var(--color-border);
  }

  .app-header__path {
    min-inline-size: 0;
    flex: 1 1 auto;
  }

  .app-header__actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .app-header__machine {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    padding: 2px 8px;
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-sm);
    border-inline-start: 3px solid var(--color-border);
  }

  @media (max-width: 767px) {
    .app-header {
      padding-inline: var(--space-3);
    }
  }
</style>
