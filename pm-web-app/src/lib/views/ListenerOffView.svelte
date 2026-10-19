<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import { runtimeStore, initRuntime } from '../stores/runtime';

  /**
   * The page of a device whose network listener was turned off.
   *
   * The device stays paired, so its secret is not deleted: the page says why it cannot reach the
   * machine and connects again by itself once the listener is on (checked on a quiet timer, with
   * the button for an immediate try).
   */
  const RETRY_MS = 15_000;

  let isReconnecting = false;
  let retry: ReturnType<typeof setInterval> | null = null;
  let isMounted = true;

  onMount(() => {
    isMounted = true;
    retry = setInterval(() => void reconnect(), RETRY_MS);
  });

  onDestroy(() => {
    isMounted = false;
    if (retry) {
      clearInterval(retry);
      retry = null;
    }
  });

  $: closedAt = $runtimeStore.listenerClosedAt ?? '';

  async function reconnect(): Promise<void> {
    if (isReconnecting || !isMounted) return;
    isReconnecting = true;
    try {
      await initRuntime();
    } finally {
      isReconnecting = false;
    }
  }
</script>

<div class="listener-off" data-testid="listener-off-view" role="alert">
  <div class="listener-off__card">
    <h1 class="listener-off__title">{$t('web.entry.lan_off.title')}</h1>

    {#if closedAt}
      <p class="listener-off__reason">
        {$t('web.entry.lan_off.reason', {
          time: new Date(closedAt).toLocaleTimeString(undefined, {
            hour: '2-digit',
            minute: '2-digit',
          }),
          command: 'pm-operator web lan',
        })}
      </p>
    {:else}
      <p class="listener-off__reason">{$t('web.entry.lan_off.unknown')}</p>
    {/if}

    <p class="listener-off__wait">{$t('web.entry.lan_off.wait')}</p>

    <div class="listener-off__actions">
      <Button variant="secondary" loading={isReconnecting} on:click={reconnect}>
        {$t('web.offline.reconnecting')}
      </Button>
    </div>
  </div>
</div>

<style>
  .listener-off {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 60vh;
    padding: var(--space-4);
  }

  .listener-off__card {
    max-width: 36rem;
    width: 100%;
    padding: var(--space-6);
    background-color: var(--color-warning-subtle);
    border: 1px solid var(--color-warning-border);
    border-radius: var(--radius-lg);
  }

  .listener-off__title {
    font-size: var(--text-xl);
    font-weight: 700;
    color: var(--color-warning);
    margin-bottom: var(--space-3);
  }

  .listener-off__reason,
  .listener-off__wait {
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    margin-bottom: var(--space-3);
    line-height: 1.5;
  }

  .listener-off__wait {
    color: var(--color-text-secondary);
  }

  .listener-off__actions {
    display: flex;
    justify-content: flex-end;
  }
</style>
