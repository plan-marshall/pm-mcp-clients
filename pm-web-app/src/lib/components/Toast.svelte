<script lang="ts">
  import { onDestroy } from 'svelte';
  import Button from './Button.svelte';

  /**
   * A short confirmation (VD-P-05, VD-S-03): bottom right, bottom on a phone, four seconds on
   * screen, with an optional act.
   *
   * It is a live region, so a screen reader announces it without the focus moving: the person who
   * triggered the act learns the result where they are. The region stays in the document while the
   * toast is empty — a region that arrives with its text is a node several screen readers never
   * announce — so the message is an update to a region the reader already knows. The act sits
   * beside the region, not inside it, because the act is a control and a live region is not.
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
  /** Whether the countdown is already running, so a re-render does not restart it. */
  let counting = false;

  /** Stops the countdown of a toast that left the screen early or was replaced. */
  function stopTimer(): void {
    if (timer !== undefined) {
      clearTimeout(timer);
      timer = undefined;
    }
    counting = false;
  }

  // The countdown starts on the opening alone: a view that re-renders while the toast is on screen
  // does not get a fifth second, and a toast that never opened never calls back.
  $: if (open && !counting) {
    counting = true;
    timer = setTimeout(() => {
      timer = undefined;
      counting = false;
      onDismiss();
    }, durationMs);
  } else if (!open && counting) {
    stopTimer();
  }

  onDestroy(stopTimer);
</script>

<div class="toast" class:toast--hidden={!open}>
  <span class="toast__status" role="status" aria-live="polite">{open ? message : ''}</span>
  {#if open && actionLabel}
    <Button variant="ghost" size="sm" on:click={onAction}>{actionLabel}</Button>
  {/if}
</div>

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

  /* An empty toast keeps its region in the accessibility tree without drawing anything: clipped to
     a pixel, it is still a live region a later message can update. */
  .toast--hidden {
    inline-size: 1px;
    block-size: 1px;
    padding: 0;
    overflow: hidden;
    clip-path: inset(50%);
    white-space: nowrap;
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
