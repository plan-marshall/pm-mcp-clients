<script module lang="ts">
  /** One figure of the list: its name and its already-formatted value. */
  export interface KeyValueItem {
    /** The name of the figure, from the view's catalogue key. */
    label: string;
    /** The figure itself, already formatted by the view. */
    value: string;
  }
</script>

<script lang="ts">
  /**
   * Runtime figures and report results (VD-P-05): two columns, the label muted and the value in
   * `--color-text-primary`.
   *
   * It is a definition list, not a table: every label is the term of its value, so a screen reader
   * pairs them without the view having to repeat the label in the value.
   */
  export let items: KeyValueItem[] = [];
</script>

<dl class="key-value-list" data-testid="key-value-list">
  {#each items as item}
    <dt class="key-value-list__label">{item.label}</dt>
    <dd class="key-value-list__value">{item.value}</dd>
  {/each}
</dl>

<style>
  .key-value-list {
    display: grid;
    grid-template-columns: minmax(0, max-content) minmax(0, 1fr);
    gap: var(--space-2) var(--space-4);
    margin: 0;
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }

  .key-value-list__label {
    color: var(--color-text-muted);
    overflow-wrap: anywhere;
  }

  .key-value-list__value {
    margin: 0;
    color: var(--color-text-primary);
    overflow-wrap: anywhere;
  }
</style>
