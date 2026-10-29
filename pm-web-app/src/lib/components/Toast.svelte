<script lang="ts">
  import { onDestroy } from 'svelte';
  import Button from './Button.svelte';

  /**
   * A short confirmation (VD-P-05, VD-S-03): bottom right, bottom on a phone, four seconds on
   * screen, with an optional act.
   *
   * It is a live region, so a screen reader announces it without the focus moving: the person who
   * triggered the act learns the result where they are. The view removes it when the four seconds
   * are over, or the person takes its act.
   */
  export let open = false;
  /** The sentence the toast carries, from the view's catalogue key. */
  export let message: string;
  /** The act beside the sentence, from the view's catalogue key; omitted when there is none. */
  export let actionLabel: string | undefined = undefined;
  /** Reports the act the person took. */
  export let onAction: () => void = () => {};
  /** Reports the end of the four seconds, so the view can take the toast off. */
  export let onDismiss: () => void = () => {};
  /** How long the toast stays; four seconds is the value of the design system. */
  export let durationMs = 4000;

  let timer: ReturnType<typeof setTimeout> | undefined = undefined;

  /** Stops the countdown of a toast that left the screen early or was replaced. */
  function stopTimer(): void {
    if (timer !== undefined) {
      clearTimeout(timer);
      timer = undefined;
    }
  }

  $: if (open) {
    stopTimer();
    timer = setTimeout(() => {
      timer = undefined;
      onDismiss();
    }, durationMs);
  } else {
    stopTimer();
  }

  onDestroy(stopTimer);
</script>

{#if open}
  <div class="toast" role="status" aria-live="polite">
    <span class="toast__message">{message}</span>
    {#if actionLabel}
      <Button variant="ghost" size="sm" on:click={onAction}>{actionLabel}</Button>
    {/if}
  </div>
{/if}

<style>
  .toast {
    position: fixed;
    inset-block-end: var(--space-6);
    inset-inline-end: var(--space-6);
    z-index: 90;
    display: inline-flex;
    align-items: center;
    gap: var(--space-3);
    max-inline-size: min(420px, calc(100vw - var(--space-8)));
    padding: var(--space-3) var(--space-4);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-raised);
    box-shadow: var(--shadow-2);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }

  /* A phone puts the toast across the bottom, where the thumb already is. */
  @media (max-width: 767px) {
    .toast {
      inset-inline: var(--space-4);
      inset-block-end: var(--space-4);
      max-inline-size: none;
    }
  }
</style>
