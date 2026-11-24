<script lang="ts">
  import Field from './Field.svelte';
  import Toggle from './Toggle.svelte';
  import Button from './Button.svelte';
  import { uniqueId } from '../utils/uniqueId';
  import { t } from '../i18n';

  /**
   * Token and secret input owning Field and `<input>` (components-fields.adoc, Special Fields).
   *
   * Masked by default, reveal Toggle, autocomplete="off", spellcheck="false",
   * autocapitalize="off", pasted whitespace trimmed, never prefilled, and forgotten once stored.
   */
  interface Props {
    /** The secret value (bindable). */
    value?: string;
    /** The element id. Auto-generated when omitted. */
    id?: string;
    /** The label text above the control. */
    label?: string;
    /** Whether the field is optional; marks label with `(optional)`. */
    optional?: boolean;
    /** One sentence under the field explaining format or consequence. */
    hint?: string;
    /** One error message under the field; puts field in invalid state. */
    error?: string | null;
    /** Whether the field is disabled. */
    disabled?: boolean;
    /** Reason the field is disabled. */
    disabledReason?: string;
    /** Placeholder example value. */
    placeholder?: string;
    /** Name attribute. */
    name?: string;
    /** Test id. */
    testid?: string;
    /** Optional store button label. */
    storeLabel?: string;
    /**
     * Handler called when secret is stored, resetting value and removing from DOM. Only a
     * rejection puts the secret back; the field then swallows the rejection.
     */
    onstore?: (secret: string) => void | Promise<void>;
    /** Event handlers. */
    oninput?: (e: Event & { currentTarget: HTMLInputElement }) => void;
    /** Event handler for change. */
    onchange?: (e: Event & { currentTarget: HTMLInputElement }) => void;
    /** Event handler for blur. */
    onblur?: (e: FocusEvent & { currentTarget: HTMLInputElement }) => void;
    /** Event handler for focus. */
    onfocus?: (e: FocusEvent & { currentTarget: HTMLInputElement }) => void;
    /** Event handler for keydown. */
    onkeydown?: (e: KeyboardEvent & { currentTarget: HTMLInputElement }) => void;
  }

  let {
    value = $bindable(''),
    id,
    label,
    optional = false,
    hint,
    error,
    disabled = false,
    disabledReason,
    placeholder,
    name,
    testid,
    storeLabel,
    onstore,
    oninput,
    onchange,
    onblur,
    onfocus,
    onkeydown,
  }: Props = $props();

  const generatedId = uniqueId('sf');
  const effectiveId = $derived(id ?? generatedId);
  const hintId = $derived(`${effectiveId}-hint`);
  const errorId = $derived(`${effectiveId}-error`);
  const reasonId = $derived(`${effectiveId}-reason`);

  let revealed = $state(false);
  // Counts the stores started, so a rejected store knows whether a newer one owns the field.
  let storeGeneration = 0;
  /**
   * Whether the last store was rejected, until a newer store starts or the caller names its own
   * `error`. A field that silently puts the secret back leaves the person guessing. The flag is
   * held, not the sentence: a language switch re-words the message like every other one.
   */
  let storeFailed = $state(false);

  /** The caller's own error wins; a rejected store reports its own until a newer store starts. */
  const shownError = $derived(error ?? (storeFailed ? $t('common.field.store_failed') : null));

  const describedBy = $derived.by(() => {
    const ids: string[] = [];
    if (shownError) {
      ids.push(errorId);
    } else if (hint) {
      ids.push(hintId);
    }
    if (disabled && disabledReason) {
      ids.push(reasonId);
    }
    return ids.length > 0 ? ids.join(' ') : undefined;
  });

  /**
   * Inserts the trimmed paste at the selection, places the caret after it and sends the input
   * event a typed character would, so `value` and the caller's `oninput` follow once. A paste
   * without text (an image, no clipboard data) is left to the browser, which inserts no text
   * into the field. A paste of whitespace only is cancelled and changes nothing, so the browser
   * never inserts it untrimmed and the selected part of the secret is never replaced with nothing.
   */
  function handlePaste(e: ClipboardEvent & { currentTarget: HTMLInputElement }): void {
    const raw = e.clipboardData?.getData('text') ?? '';
    if (!raw) return;
    e.preventDefault();
    const pasted = raw.trim();
    if (!pasted) return;
    const input = e.currentTarget;
    // A text or password input always has a selection, so both ends are numbers here.
    const start = input.selectionStart as number;
    const end = input.selectionEnd as number;
    input.setRangeText(pasted, start, end, 'end');
    input.dispatchEvent(new Event('input', { bubbles: true }));
  }

  function handleInput(e: Event & { currentTarget: HTMLInputElement }): void {
    value = e.currentTarget.value;
    oninput?.(e);
  }

  /**
   * Hands the secret to `onstore` and forgets it: the field is cleared and masked at once. A
   * rejected store puts the secret back, still masked, so the operator's input is not lost — but
   * only into a field still empty and only while no newer store has started: what was typed or
   * stored while the store was pending is newer and stays. A reveal pressed while the store was
   * pending is undone, so the restored secret is masked. The field then names the failure itself
   * (`common.field.store_failed`), and a caller's own `error` takes precedence over that message.
   */
  async function handleStore(): Promise<void> {
    if (!value || disabled) return;
    const secretToStore = value;
    storeGeneration += 1;
    const generation = storeGeneration;
    storeFailed = false;
    value = '';
    revealed = false;
    try {
      await onstore?.(secretToStore);
    } catch {
      if (generation === storeGeneration) {
        // The store that was refused is the current one: report it even when newer typing means
        // there is nothing to put back, or the rejection stays silent.
        storeFailed = true;
        if (!value) {
          value = secretToStore;
          revealed = false;
        }
      }
    }
  }
</script>

<Field
  id={effectiveId}
  {label}
  {optional}
  {hint}
  error={shownError}
  {disabled}
  {disabledReason}
  {hintId}
  {errorId}
  {reasonId}
>
  <div class="secret-field__row">
    <input
      id={effectiveId}
      class="secret-field__input"
      class:secret-field__input--invalid={Boolean(shownError)}
      type={revealed ? 'text' : 'password'}
      {name}
      bind:value
      autocomplete="off"
      spellcheck="false"
      autocapitalize="off"
      {disabled}
      {placeholder}
      data-testid={testid}
      aria-invalid={shownError ? 'true' : undefined}
      aria-describedby={describedBy}
      onpaste={handlePaste}
      oninput={handleInput}
      {onchange}
      {onblur}
      {onfocus}
      {onkeydown}
    />

    <Toggle
      pressed={revealed}
      label={$t('common.field.reveal')}
      icon="state.lock"
      {disabled}
      onToggle={(state) => (revealed = state)}
    />

    {#if storeLabel || onstore}
      <Button
        type="button"
        variant="primary"
        disabled={disabled || !value}
        onclick={handleStore}
      >
        {storeLabel ? storeLabel : $t('common.action.save')}
      </Button>
    {/if}
  </div>
</Field>

<style>
  .secret-field__row {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    width: 100%;
  }

  .secret-field__input {
    box-sizing: border-box;
    flex: 1;
    min-height: var(--control-height);
    padding: var(--space-1) var(--space-3);
    font-family: var(--font-mono);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
    color: var(--color-text-primary);
    background-color: var(--control-bg);
    border: 1px solid var(--control-border);
    border-radius: var(--control-radius);
    transition:
      border-color var(--duration-fast) var(--ease-standard),
      box-shadow var(--duration-fast) var(--ease-standard);
  }

  .secret-field__input:focus {
    outline: 2px solid transparent;
    box-shadow: var(--focus-ring);
    border-color: var(--color-brand);
  }

  .secret-field__input--invalid,
  .secret-field__input[aria-invalid='true'] {
    border-color: var(--color-danger);
  }

  .secret-field__input:disabled {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-muted);
    border-color: var(--color-border);
    cursor: not-allowed;
  }
</style>
