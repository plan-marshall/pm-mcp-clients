<script lang="ts">
  import CodeField from '../../lib/components/CodeField.svelte';

  /**
   * A parent that binds CodeField's `value` and `complete`, shows the bound `complete` and clears
   * or replaces the value the way a parent does after a refused pairing call or from a deep link.
   * Every text comes in through the props, so the fixture writes no literal of its own.
   */
  interface Props {
    /** The label of the code field. */
    label: string;
    /** The text of the parent's replace button. */
    replaceLabel: string;
    /** The value the parent writes when its replace button is pressed. */
    replacement: string;
    /** Handed to the field's `oncomplete`. */
    oncomplete?: (code: string) => void;
  }

  let { label, replaceLabel, replacement, oncomplete }: Props = $props();

  let value = $state('');
  let complete = $state(false);
</script>

<CodeField {label} {oncomplete} bind:value bind:complete />
<output data-testid="bound-complete">{String(complete)}</output>
<button type="button" data-testid="parent-replace" onclick={() => (value = replacement)}>
  {replaceLabel}
</button>
