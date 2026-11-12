<script lang="ts" module>
  /**
   * The quiet timer on which a page of a lost connection tries again; the runtime that is not
   * reachable waits on the same one.
   */
  export const RECONNECT_INTERVAL_MS = 15_000;
</script>

<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import Button from '../components/Button.svelte';
  import { formatTime } from '../display/format';
  import { runtimeStore, initRuntime } from '../stores/runtime';
  import MascotFigure from '../components/MascotFigure.svelte';
  import { heldFigure } from '../stores/heldFigure';

  /**
   * The page of a device whose network listener was turned off.
   *
   * The device stays paired, so its secret is not deleted: the page says why it cannot reach the
   * machine and connects again by itself once the listener is on (checked on a quiet timer, with
   * the button for an immediate try). The `searching` figure stands beside the text, drawn from the
   * object URL the app held since its start, never fetched from here; without it the text stands
   * alone. Where the page is too narrow to hold both, the figure is hidden, never stacked above the
   * text, and the card takes the width.
   */

  /** The figure's height beside the text, within the design's 160–200 px. */
  const FIGURE_HEIGHT = 180;

  let isReconnecting = false;
  let retry: ReturnType<typeof setInterval> | null = null;
  let isMounted = true;

  onMount(() => {
    isMounted = true;
    retry = setInterval(() => void reconnect(), RECONNECT_INTERVAL_MS);
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
  <MascotFigure pose="searching" height={FIGURE_HEIGHT} src={$heldFigure} />
  <div class="listener-off__card">
    <h1 class="listener-off__title">{$t('web.entry.lan_off.title')}</h1>

    {#if closedAt}
      <p class="listener-off__reason">
        {$t('web.entry.lan_off.reason', {
          time: formatTime(closedAt, 'short', $currentLocale),
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
  /*
   * The page is the query container: it stretches across the view area, so its width is the page's
   * and never the content's, and the narrow rule below follows the page, not the viewport the rail
   * also takes from.
   */
  .listener-off {
    container: listener-off / inline-size;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-6);
    min-height: 60vh;
    padding: var(--space-4);
  }

  /*
   * Where the page cannot hold the figure beside a card that keeps its text and its button, the
   * figure is hidden, never stacked above the text.
   */
  @container listener-off (max-width: 36rem) {
    .listener-off :global(.mascot-figure) {
      display: none;
    }
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
