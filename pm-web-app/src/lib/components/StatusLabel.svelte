<script lang="ts">
  /**
   * The state of a row, as the design system draws it (VD-P-05): an 8 px dot in the state colour and
   * the state word in `--color-text-secondary`.
   *
   * The word is the view's copy — the shared vocabulary of `state.*` keys — because a state never
   * shows as a bare enum. The dot carries only the colour; the word carries the meaning.
   */
  export let tone: 'neutral' | 'running' | 'attention' | 'danger' | 'success' = 'neutral';
  /** The state word, from the view's catalogue key. */
  export let label: string;
</script>

<span class="status-label status-label--{tone}">
  <span class="status-label__dot" aria-hidden="true"></span>
  <span class="status-label__word">{label}</span>
</span>

<style>
  .status-label {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }

  .status-label__dot {
    inline-size: var(--space-2);
    block-size: var(--space-2);
    border-radius: var(--radius-full);
    background-color: var(--color-neutral);
    flex: 0 0 auto;
  }

  .status-label__word {
    color: var(--color-text-secondary);
  }

  .status-label--neutral .status-label__dot {
    background-color: var(--color-neutral);
  }

  .status-label--running .status-label__dot {
    background-color: var(--color-running);
  }

  .status-label--attention .status-label__dot {
    background-color: var(--color-attention);
  }

  .status-label--danger .status-label__dot {
    background-color: var(--color-danger);
  }

  .status-label--success .status-label__dot {
    background-color: var(--color-success);
  }
</style>
