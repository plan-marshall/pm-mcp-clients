<script lang="ts">
  import Dialog from './Dialog.svelte';
  import Button from './Button.svelte';
  import { t } from '../i18n';

  /**
   * The confirmation a destructive act asks for (UX-P-04, VD-S-03).
   *
   * The dialog names the consequences in the danger colour before its act can run. Where the act
   * is as wide as a pairing or an enrolment, the operator must type the resource's name first: a
   * click alone is too cheap for what cannot be taken back.
   *
   * While the act runs the dialog is a guard: a second confirm is ignored, the cancel is disabled,
   * and Escape does not close it. The view that owns the request reports it through `busy`.
   */
  export let open = false;
  /** The title, from the view's catalogue key. */
  export let title: string;
  /** What the act costs, one sentence per consequence, from the catalogue. */
  export let consequences: string[] = [];
  /** The act's own label, from the catalogue. */
  export let actionLabel: string;
  /** The label that keeps things as they are, from the catalogue. */
  export let cancelLabel: string;
  /** The name the operator must type; absent where a click is enough. */
  export let typedName: string | undefined = undefined;
  /** Whether the act is already running, which holds the dialog open. */
  export let busy = false;
  /** Whether the act is refused for a reason the dialog's body named, e.g. a missing reason. */
  export let confirmDisabled = false;
  /** The test id of the dialog's own box; a composing dialog names itself. */
  export let testid = 'confirm-dialog';
  /** Reports the confirmed act. */
  export let onConfirm: () => void = () => {};
  /** Reports the close the dialog asks for; the view decides when it is gone. */
  export let onClose: () => void = () => {};

  let typed = '';

  // A reopened dialog starts from nothing: the name is a guard for this opening, not credit.
  $: if (!open) typed = '';

  /** A typed name is only a guard where the act asks for one. */
  $: typedMatches = typedName === undefined || (typedName !== '' && typed === typedName);

  /** Confirms once: a second click while the act runs is ignored. */
  function confirm(): void {
    if (busy || !typedMatches || confirmDisabled) {
      return;
    }
    onConfirm();
  }

  /** Closes unless the act is running: Escape and cancel may not abandon it half-sent. */
  function close(): void {
    if (!busy) {
      onClose();
    }
  }
</script>

<Dialog {open} {title} danger {consequences} onClose={close} {testid}>
  <slot />

  {#if typedName !== undefined}
    <label class="confirm-dialog__field">
      {$t('web.confirm.typed_name', { name: typedName })}
      <input
        type="text"
        class="confirm-dialog__input"
        data-testid="confirm-dialog-input"
        bind:value={typed}
        autocomplete="off"
        spellcheck="false"
      />
    </label>
  {/if}

  <svelte:fragment slot="footer">
    <Button
      variant="secondary"
      data-testid="confirm-dialog-cancel"
      disabled={busy}
      on:click={close}
    >
      {cancelLabel}
    </Button>
    <Button
      variant="danger"
      data-testid="confirm-dialog-confirm"
      disabled={!typedMatches || confirmDisabled}
      loading={busy}
      on:click={confirm}
    >
      {actionLabel}
    </Button>
  </svelte:fragment>
</Dialog>

<style>
  .confirm-dialog__field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .confirm-dialog__input {
    min-block-size: 44px;
    padding: var(--space-1) var(--space-2);
    border: 1px solid var(--color-border-strong);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
  }
</style>
