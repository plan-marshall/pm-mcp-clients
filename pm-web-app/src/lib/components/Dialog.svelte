<script lang="ts">
  import { uniqueId } from '../utils/uniqueId';

  /**
   * The dialog of the design system (VD-P-05, VD-S-03): radius 12 px, `--shadow-overlay`, the
   * title in `--text-xl`, and the acts of the footer on the right.
   *
   * It is modal in the sense the role promises: it opens on the focus, Escape closes it, and the
   * focus goes back to the control that opened it. A danger dialog names the consequences in a
   * list before its act, so the act never reads as a plain click.
   */
  export let open = false;
  /** The title, from the view's catalogue key. */
  export let title: string;
  /** Whether the dialog carries an act that cannot be undone. */
  export let danger = false;
  /** What a danger act costs, one sentence per consequence. */
  export let consequences: string[] = [];
  /** Reports the close the dialog asks for; the view decides when it is gone. */
  export let onClose: () => void = () => {};

  /** The id the title carries, for `aria-labelledby`. */
  const titleId = uniqueId('dialog-title');

  /** The control the focus goes back to when the dialog leaves the document. */
  let returnFocus: HTMLElement | null = null;

  /**
   * Takes the focus into the dialog when it opens, and gives it back to where it came from when the
   * dialog is removed.
   *
   * @param node the dialog's own element
   * @returns the action's teardown, which restores the focus
   */
  function takeFocus(node: HTMLElement): { destroy: () => void } {
    returnFocus = document.activeElement as HTMLElement | null;
    node.focus();
    return {
      destroy(): void {
        returnFocus?.focus();
        returnFocus = null;
      },
    };
  }

  /** @param event the key press anywhere while the dialog is open */
  function onKeydown(event: KeyboardEvent): void {
    if (open && event.key === 'Escape') {
      event.stopPropagation();
      onClose();
    }
  }
</script>

<svelte:window on:keydown={onKeydown} />

{#if open}
  <div class="dialog-backdrop">
    <div
      class="dialog"
      class:dialog--danger={danger}
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      tabindex="-1"
      use:takeFocus
      data-testid="dialog"
    >
      <h2 class="dialog__title" id={titleId}>{title}</h2>

      {#if danger && consequences.length > 0}
        <ul class="dialog__consequences">
          {#each consequences as consequence (consequence)}
            <li class="dialog__consequence">{consequence}</li>
          {/each}
        </ul>
      {/if}

      <div class="dialog__body">
        <slot />
      </div>

      {#if $$slots.footer}
        <div class="dialog__footer">
          <slot name="footer" />
        </div>
      {/if}
    </div>
  </div>
{/if}

<style>
  .dialog-backdrop {
    position: fixed;
    inset: 0;
    z-index: 100;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: var(--space-4);
    background-color: var(--color-overlay);
  }

  .dialog {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    inline-size: min(560px, 100%);
    max-block-size: 100%;
    overflow: auto;
    padding: var(--space-6);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-surface);
    box-shadow: var(--shadow-overlay);
    color: var(--color-text-primary);
  }

  .dialog:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring), var(--shadow-overlay);
  }

  .dialog__title {
    margin: 0;
    font-size: var(--text-xl);
    line-height: var(--line-height-xl);
  }

  .dialog--danger .dialog__title {
    color: var(--color-danger);
  }

  .dialog__consequences {
    margin: 0;
    padding-inline-start: var(--space-5);
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }

  .dialog__body {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  /* The acts of a dialog sit on the right, with the primary act last. */
  .dialog__footer {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: var(--space-2);
  }

  @media (max-width: 767px) {
    .dialog {
      padding: var(--space-4);
    }
  }
</style>
