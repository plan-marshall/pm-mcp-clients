<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import MascotFigure from '../components/MascotFigure.svelte';
  import { heldFigure } from '../stores/heldFigure';
  import { initRuntime } from '../stores/runtime';
  import { RECONNECT_INTERVAL_MS } from './ListenerOffView.svelte';

  /**
   * The page of a runtime that is not reachable on this machine (entry.adoc, After Pairing).
   *
   * It says what is wrong and the one command that starts the runtime, and connects again by
   * itself on a quiet timer. The `searching` figure stands beside the text, drawn from the object
   * URL the app held since its start; without it the text stands alone and says everything.
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

<div class="unreachable" data-testid="unreachable-view" role="alert">
  <MascotFigure pose="searching" height={FIGURE_HEIGHT} src={$heldFigure} />
  <div class="unreachable__text">
    <h1 class="unreachable__title">{$t('web.entry.unreachable.title')}</h1>
    <p class="unreachable__fix">{$t('web.entry.unreachable.fix', { command: 'pm-operator' })}</p>
  </div>
</div>

<style>
  .unreachable {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-6);
    min-height: 60vh;
    padding: var(--space-4);
  }

  .unreachable__text {
    max-width: 36rem;
  }

  .unreachable__title {
    margin: 0 0 var(--space-3);
    color: var(--color-text-primary);
    font-size: var(--text-xl);
    font-weight: var(--weight-semibold);
    line-height: var(--line-height-xl);
  }

  .unreachable__fix {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }
</style>
