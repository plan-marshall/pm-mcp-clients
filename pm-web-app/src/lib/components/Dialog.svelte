<script module lang="ts">
  /**
   * The dialogs that are open, outermost first. Only the last one — the one on top — answers the
   * keyboard: a dialog opened from another lies outside the outer panel (every dialog renders under
   * its theme scope, not inside the caller), so an Esc or a Tab meant for the inner one would
   * otherwise be taken by the outer one and move the focus out of the dialog on top.
   */
  const openPanels: HTMLElement[] = [];
  let rootOverflowBeforeFirstDialog: string | null = null;
</script>

<script lang="ts">
  import { afterUpdate, onMount } from 'svelte';
  import { uniqueId } from '../utils/uniqueId';
  import { portal } from '../utils/portal';

  /**
   * The dialog of the design system (VD-P-05, VD-S-03): radius 12 px, `--shadow-overlay`, the
   * title in `--text-xl`, and the acts of the footer on the right.
   *
   * It is modal in the sense the role promises: it opens on the focus, Tab and Shift-Tab stay
   * inside it, Escape closes it, and the focus goes back to the control that opened it. A danger
   * dialog names the consequences in a list before its act, so the act never reads as a plain
   * click.
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
  /** The test id of the dialog's own box; a composing dialog names itself. */
  export let testid = 'dialog';
  /** Optional element focus returns to when the dialog is removed. */
  export let returnFocusElement: HTMLElement | null = null;
  /**
   * The dialog's width: `default` is the reading width of a confirmation, `wide` the width a
   * side-by-side difference needs. A caller names the size; it never overrides the width from
   * outside the component, where the cascade would decide instead of the intent.
   */
  export let size: 'default' | 'wide' = 'default';
  /** Whether the footer area is drawn; a stage that offers no act keeps no empty row. */
  export let footer = true;

  /** The elements the Tab key walks, in document order. */
  const FOCUSABLE =
    'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), ' +
    'textarea:not([disabled]), [tabindex]:not([tabindex="-1"]), ' +
    '[contenteditable]:not([contenteditable="false"])';

  /** The id the title carries, for `aria-labelledby`. */
  const titleId = uniqueId('dialog-title');

  /** The control the focus goes back to when the dialog leaves the document. */
  let returnFocus: HTMLElement | null = null;

  /** The dialog's own element, the box the focus stays in. */
  let panel: HTMLElement | undefined = undefined;

  /**
   * Takes the focus into the dialog when it opens, and gives it back to where it came from when the
   * dialog is removed.
   *
   * @param node the dialog's own element
   * @returns the action's teardown, which restores the focus
   */
  function takeFocus(node: HTMLElement): { destroy: () => void } {
    panel = node;
    const root = document.documentElement;
    if (openPanels.length === 0) {
      rootOverflowBeforeFirstDialog = root.style.overflow;
    }
    openPanels.push(node);
    returnFocus = returnFocusElement ?? (document.activeElement as HTMLElement | null);
    // The page behind the backdrop must not scroll under the dialog; its own position survives.
    root.style.overflow = 'hidden';
    node.focus();
    queueMicrotask(() => {
      // A child that took the focus while mounting — an autofocused field — keeps it: the dialog
      // box only takes it back when nothing inside the dialog holds it.
      if (panel === node && !node.contains(document.activeElement)) {
        node.focus();
      }
    });
    return {
      destroy(): void {
        const index = openPanels.indexOf(node);
        if (index >= 0) {
          openPanels.splice(index, 1);
        }
        if (openPanels.length === 0) {
          root.style.overflow = rootOverflowBeforeFirstDialog ?? '';
          rootOverflowBeforeFirstDialog = null;
        }
        returnFocus?.focus();
        returnFocus = null;
        panel = undefined;
      },
    };
  }

  /** @returns the elements inside the dialog that can take the focus, in native tab order */
  function focusable(): HTMLElement[] {
    if (!panel) {
      return [];
    }
    // `:disabled` also matches a control an ancestor `fieldset` disables, which a `focus()` call
    // cannot reach. A positive `tabindex` comes first in the native order, ascending.
    const rank = (element: HTMLElement): number =>
      element.tabIndex > 0 ? element.tabIndex : Number.MAX_SAFE_INTEGER;
    return Array.from(panel.querySelectorAll<HTMLElement>(FOCUSABLE))
      .filter((element) => !element.matches(':disabled'))
      .sort((first, second) => rank(first) - rank(second));
  }

  /** @param event the key press anywhere while the dialog is open */
  function onKeydown(event: KeyboardEvent): void {
    if (!open || panel === undefined || openPanels[openPanels.length - 1] !== panel) {
      return;
    }
    if (event.key === 'Escape') {
      if (event.defaultPrevented || event.isComposing) {
        return;
      }
      // The Escape yields to the menu that holds the keyboard: that menu closes itself on this
      // press. A menu that merely lies somewhere in the dialog is no reason to swallow the Escape.
      const focused = document.activeElement instanceof Element ? document.activeElement : null;
      if (focused?.closest('[role="menu"]')) {
        return;
      }
      // Captured before the page's own Escape handlers, so the dialog closes on its own.
      event.preventDefault();
      event.stopPropagation();
      onClose();
      return;
    }
    if (event.key !== 'Tab') {
      return;
    }
    const items = focusable();
    if (items.length === 0) {
      event.preventDefault();
      panel?.focus();
      return;
    }
    const first = items[0];
    const last = items[items.length - 1];
    const current = document.activeElement;
    // A focus on the dialog box itself, or one that has left it — a click on the backdrop drops it
    // to the body — counts as leaving too, so the next Tab returns it into the dialog instead of
    // the page behind it.
    const leaving =
      current === null || current === panel || !panel?.contains(current)
        ? true
        : event.shiftKey
          ? current === first
          : current === last;
    if (!leaving) {
      return;
    }
    event.preventDefault();
    (event.shiftKey ? last : first).focus();
  }

  // A capture listener on the window is the first stop of every key press: the dialog sees Escape
  // before a view's own handler, and stops its propagation there.
  onMount(() => {
    window.addEventListener('keydown', onKeydown, true);
    return () => window.removeEventListener('keydown', onKeydown, true);
  });

  // A footer that goes away takes the act that held the focus with it. The focus then returns to the
  // dialog box, which stays mounted, instead of falling to the page behind the dialog. It runs after
  // the update, because the earlier pass still holds the control that is about to be removed, and
  // only on the dialog on top: a background dialog never takes the focus from the one opened from it.
  afterUpdate(() => {
    if (!open || footer || panel === undefined || openPanels[openPanels.length - 1] !== panel) {
      return;
    }
    const active = typeof document === 'undefined' ? null : document.activeElement;
    if (active === null || !panel.contains(active)) {
      panel.focus();
    }
  });
</script>

{#if open}
  <div class="dialog-backdrop" use:portal>
    <div
      class="dialog"
      class:dialog--danger={danger}
      class:dialog--wide={size === 'wide'}
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      tabindex="-1"
      use:takeFocus
      data-testid={testid}
    >
      <h2 class="dialog__title" id={titleId}>{title}</h2>

      {#if danger && consequences.length > 0}
        <ul class="dialog__consequences">
          {#each consequences as consequence}
            <li class="dialog__consequence">{consequence}</li>
          {/each}
        </ul>
      {/if}

      <div class="dialog__body">
        <slot />
      </div>

      {#if footer && $$slots.footer}
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
    z-index: var(--layer-dialog);
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

  /* The width a side-by-side difference needs; the dialog owns the rule so no caller has to win
     a cascade against it. */
  .dialog--wide {
    inline-size: min(1000px, 100%);
  }

  .dialog__title {
    margin: 0;
    font-size: var(--text-xl);
    line-height: var(--line-height-xl);
  }

  .dialog--danger .dialog__title,
  .dialog--danger .dialog__consequences {
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
