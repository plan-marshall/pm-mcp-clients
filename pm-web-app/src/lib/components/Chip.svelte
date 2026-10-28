<script lang="ts">
  import { onDestroy } from 'svelte';
  import { advisory } from '../utils/advisoryTitle';
  import { t } from '../i18n';

  /**
   * An id, branch or version (VD-P-05) in `--text-xs` monospace on `--color-bg-subtle`, copied on
   * click.
   *
   * The value is the visible label, so a chip never says what it copies twice; its advisory names
   * the act. A refused clipboard changes nothing on screen rather than claiming a copy that did not
   * happen, and the confirmation clears itself.
   */
  export let value: string;

  /** How long the confirmation stays beside the value. */
  const COPIED_MS = 2000;

  /** Whether the last click copied, for the short confirmation beside the value. */
  let copied = false;
  let resetTimer: ReturnType<typeof setTimeout> | undefined;

  async function copy(): Promise<void> {
    try {
      await navigator.clipboard.writeText(value);
      copied = true;
      clearTimeout(resetTimer);
      resetTimer = setTimeout(() => {
        copied = false;
      }, COPIED_MS);
    } catch {
      copied = false;
    }
  }

  onDestroy(() => clearTimeout(resetTimer));
</script>

<button
  type="button"
  class="chip"
  data-advisory={$t('web.common.copy.title')}
  use:advisory
  on:click={copy}
>
  <span class="chip__value">{value}</span>
  <span class="chip__state" aria-live="polite">{copied ? $t('web.common.copied') : ''}</span>
</button>

<style>
  .chip {
    display: inline-flex;
    align-items: center;
    max-inline-size: 100%;
    padding: var(--space-1) var(--space-2);
    border: 1px solid transparent;
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    line-height: var(--line-height-xs);
    cursor: pointer;
  }

  .chip:hover {
    color: var(--color-text-primary);
    border-color: var(--color-border);
  }

  .chip__value {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .chip__state {
    font-family: var(--font-sans);
    color: var(--color-text-muted);
  }

  /* The live region stays in the document and out of the layout while empty, so a confirmation that
     arrives later is an update the screen reader announces, not a region it never saw. */
  .chip__state:not(:empty) {
    margin-inline-start: var(--space-1);
  }
</style>
