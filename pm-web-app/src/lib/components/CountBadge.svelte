<script lang="ts">
  /**
   * A number in a pill (VD-P-05): the attention fill for "needs you", the neutral fill otherwise.
   *
   * It belongs in the rail, in tabs and in group heads — never in a row, where `StatusLabel` names
   * the state. The view decides what the number counts and should name it for a screen reader: the
   * number then reads as the name, not as a bare figure beside it.
   */
  export let count: number;
  /** Whether the number names something that waits for the operator. */
  export let attention = false;
  /** The accessible name, where the bare number would not say what it counts. */
  export let label: string | undefined = undefined;
</script>

<span
  class="count-badge count-badge--{attention ? 'attention' : 'neutral'}"
  data-testid="count-badge"
>
  {#if label}
    <span class="sr-only">{label}</span>
    <span aria-hidden="true">{count}</span>
  {:else}
    {count}
  {/if}
</span>

<style>
  .count-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-inline-size: 20px;
    block-size: 20px;
    padding: 0 var(--space-1);
    border-radius: var(--radius-full);
    font-size: var(--text-xs);
    font-weight: var(--weight-semibold);
    line-height: 1;
    font-variant-numeric: tabular-nums;
  }

  .count-badge--neutral {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
  }

  /* The attention pair the token set enforces: `--color-attention-text` on its subtle fill carries
     the contrast a 12 px number owes, where white on the strong attention colour does not. */
  .count-badge--attention {
    background-color: var(--color-attention-subtle);
    color: var(--color-attention-text);
  }
</style>
