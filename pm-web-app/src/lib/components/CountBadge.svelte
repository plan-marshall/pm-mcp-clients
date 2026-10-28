<script lang="ts">
  /**
   * A number in a pill (VD-P-05): the attention fill for "needs you", the neutral fill otherwise.
   *
   * It belongs in the rail, in tabs and in group heads — never in a row, where `StatusLabel` names
   * the state. The view decides what the number counts and may name it for a screen reader.
   */
  export let count: number;
  /** Whether the number names something that waits for the operator. */
  export let attention = false;
  /** The accessible name, where the bare number would not say what it counts. */
  export let label: string | undefined = undefined;
</script>

<span
  class="count-badge count-badge--{attention ? 'attention' : 'neutral'}"
  aria-label={label}
  data-testid="count-badge"
>
  {count}
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

  .count-badge--attention {
    background-color: var(--color-attention);
    color: var(--color-on-brand);
  }
</style>
