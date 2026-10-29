<script lang="ts">
  import Button from './Button.svelte';

  /**
   * The one thing that waits for the operator (VD-P-05, VD-S-03, UX-P-05): a block in
   * `--color-attention-subtle` with a dot, one sentence and one act.
   *
   * It is not a live region: the strip is part of the page a person opened, so it announces
   * nothing of its own. The sentence says what waits; the act is the one step that addresses it.
   */
  export let message: string;
  /** The act beside the sentence, from the view's catalogue key; omitted when there is none. */
  export let actionLabel: string | undefined = undefined;
  /** Reports the act the person took. */
  export let onAct: () => void = () => {};
</script>

<div class="attention-strip" data-testid="attention-strip">
  <span class="attention-strip__dot" aria-hidden="true"></span>
  <p class="attention-strip__message">{message}</p>
  {#if actionLabel}
    <Button variant="ghost" size="sm" on:click={onAct}>{actionLabel}</Button>
  {/if}
</div>

<style>
  .attention-strip {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-3) var(--space-4);
    border-radius: var(--radius-md);
    background-color: var(--color-attention-subtle);
    color: var(--color-attention-text);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }

  .attention-strip__dot {
    flex: 0 0 auto;
    inline-size: var(--space-2);
    block-size: var(--space-2);
    border-radius: var(--radius-full);
    background-color: var(--color-attention);
  }

  .attention-strip__message {
    flex: 1 1 auto;
    margin: 0;
    min-inline-size: 0;
    overflow-wrap: anywhere;
  }
</style>
