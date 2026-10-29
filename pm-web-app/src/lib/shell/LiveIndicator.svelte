<script lang="ts">
  import { runtimeStore, retryEventStream } from '../stores/runtime';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';

  /**
   * The live indicator of the top bar (UX-P-12): the state of the event stream in one line.
   *
   * `● Live` while the stream is connected, `Reconnecting…` while it retries, and `Offline` with
   * `Try again` once it gives up. It replaces the offline banner of the old frame, so the state of
   * the connection lives in the one always-visible row rather than above the view.
   */

  /**
   * The state the indicator shows: the stream's own state (UX-P-12).
   *
   * A single failed request does not make the connection offline — the stream says whether the
   * machine is reachable, and it is the stream that gives up.
   */
  const shown = $derived($runtimeStore.streamState);

  const label = $derived(
    $t(
      shown === 'connected'
        ? 'web.frame.live.connected'
        : shown === 'connecting'
          ? 'web.frame.live.connecting'
          : shown === 'reconnecting'
            ? 'web.frame.live.reconnecting'
            : 'web.frame.live.offline',
    ),
  );
</script>

<div
  class="live-indicator"
  data-state={shown}
  data-testid="live-indicator"
  role="status"
  aria-live="polite"
>
  <span class="live-indicator__dot" aria-hidden="true"></span>
  <span class="live-indicator__label">{label}</span>
  {#if shown === 'offline'}
    <Button
      variant="ghost"
      size="sm"
      data-testid="live-retry"
      on:click={() => void retryEventStream()}
    >
      {$t('web.frame.live.retry')}
    </Button>
  {/if}
</div>

<style>
  .live-indicator {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-full);
    border: 1px solid var(--color-border);
    font-size: var(--text-xs);
    line-height: var(--line-height-xs);
    color: var(--color-text-secondary);
    white-space: nowrap;
  }

  /* A colour never carries the meaning alone: the dot stands beside the word it repeats. */
  .live-indicator__dot {
    inline-size: 8px;
    block-size: 8px;
    border-radius: var(--radius-full);
    background-color: var(--color-text-muted);
  }

  .live-indicator[data-state='connected'] .live-indicator__dot {
    background-color: var(--color-success);
  }

  .live-indicator[data-state='connected'] {
    color: var(--color-text-primary);
  }

  .live-indicator[data-state='connecting'] .live-indicator__dot {
    background-color: var(--color-neutral);
  }

  .live-indicator[data-state='reconnecting'] {
    color: var(--color-attention-text);
    border-color: var(--color-attention);
    background-color: var(--color-attention-subtle);
  }

  .live-indicator[data-state='reconnecting'] .live-indicator__dot {
    background-color: var(--color-attention);
  }

  .live-indicator[data-state='offline'] {
    color: var(--color-danger);
    border-color: var(--color-danger);
  }

  .live-indicator[data-state='offline'] .live-indicator__dot {
    background-color: var(--color-danger);
  }
</style>
