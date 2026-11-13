<script lang="ts">
  import { onDestroy } from 'svelte';
  import { advisory } from '../utils/advisoryTitle';
  import { t } from '../i18n';

  /**
   * An id, branch or version (VD-P-05, components-tags.adoc: chip) in `--text-xs` monospace on
   * `--color-bg-subtle`, copied on click.
   *
   * The value is the visible label, so a chip never says what it copies twice; its accessible name
   * names the act with the value (`common.chip.copy`), so a keyboard, touch or screen-reader user
   * learns that it copies without the hover advisory. A refused clipboard changes nothing on screen
   * rather than claiming a copy that did not happen, and the confirmation clears itself.
   */
  export let value: string;

  /** How long the confirmation stays beside the value. */
  const COPIED_MS = 2000;

  /** Whether the last click copied, for the short confirmation beside the value. */
  let copied = false;
  let resetTimer: ReturnType<typeof setTimeout> | undefined;

  /**
   * Copies a value, with the fallback the browser offers outside a secure context.
   *
   * `navigator.clipboard` exists only in a secure context, and the app is also opened over plain
   * HTTP on the machine's LAN address. The selection-based copy is the one that still works there,
   * so a phone gets a working chip instead of nothing.
   *
   * @param text the value to copy
   * @returns whether either path copied
   */
  async function writeToClipboard(text: string): Promise<boolean> {
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(text);
        return true;
      }
    } catch {
      // A refused or missing permission falls through to the selection-based copy.
    }
    return copyBySelection(text);
  }

  /**
   * @param text the value to copy
   * @returns whether the browser copied the selected text
   */
  function copyBySelection(text: string): boolean {
    const field = document.createElement('textarea');
    field.value = text;
    field.setAttribute('readonly', '');
    field.style.position = 'fixed';
    field.style.opacity = '0';
    document.body.appendChild(field);
    field.select();
    try {
      return document.execCommand('copy');
    } catch {
      return false;
    } finally {
      field.remove();
    }
  }

  async function copy(): Promise<void> {
    const done = await writeToClipboard(value);
    copied = done;
    clearTimeout(resetTimer);
    if (done) {
      resetTimer = setTimeout(() => {
        copied = false;
      }, COPIED_MS);
    }
  }

  onDestroy(() => clearTimeout(resetTimer));
</script>

{#if value}
  <button
    type="button"
    class="chip"
    aria-label={$t('common.chip.copy', { value })}
    data-advisory={$t('web.common.copy.title')}
    use:advisory
    on:click={copy}
  >
    <span class="chip__value">{value}</span>
    <span class="chip__state" aria-live="polite">{copied ? $t('web.common.copied') : ''}</span>
  </button>
{/if}

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

  /* A phone gets the 44 px target, as every control of the design system does. */
  @media (max-width: 767px) {
    .chip {
      min-block-size: 44px;
    }
  }
</style>
