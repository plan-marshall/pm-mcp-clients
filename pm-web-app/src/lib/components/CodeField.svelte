<script lang="ts">
  import Field from './Field.svelte';
  import { uniqueId } from '../utils/uniqueId';

  /**
   * Pairing code input owning Field and `<input>` (components-fields.adoc, Special Fields).
   *
   * Monospace and large, autocomplete="one-time-code", accepted in any case with or without
   * dash (e.g. KXQB-MRTN), reports "complete" at 8 characters.
   */
  interface Props {
    /** The code input value (bindable). */
    value?: string;
    /** The element id. Auto-generated when omitted. */
    id?: string;
    /** The label text above the control. */
    label?: string;
    /** Whether the field is optional. */
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
    /** Whether the entered code is complete (8 characters). */
    complete?: boolean;
    /** Handler called when code reaches 8 characters with normalized code. */
    oncomplete?: (code: string) => void;
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
    placeholder = 'KXQB-MRTN',
    name,
    testid,
    complete = $bindable(false),
    oncomplete,
    oninput,
    onchange,
    onblur,
    onfocus,
    onkeydown,
  }: Props = $props();

  const generatedId = uniqueId('cf');
  const effectiveId = $derived(id ?? generatedId);
  const hintId = $derived(`${effectiveId}-hint`);
  const errorId = $derived(`${effectiveId}-error`);
  const reasonId = $derived(`${effectiveId}-reason`);

  const describedBy = $derived.by(() => {
    const ids: string[] = [];
    if (error) {
      ids.push(errorId);
    } else if (hint) {
      ids.push(hintId);
    }
    if (disabled && disabledReason) {
      ids.push(reasonId);
    }
    return ids.length > 0 ? ids.join(' ') : undefined;
  });

  /** The last complete code reported to `oncomplete`; null while the code is incomplete. */
  let lastReported: string | null = null;

  /**
   * Strips everything but letters and digits and upper-cases the rest.
   *
   * @param raw the text of the field
   * @returns the code without dash or spaces, in upper case
   */
  function clean(raw: string): string {
    return raw.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
  }

  // An incomplete value ends the reported code whoever set it, so a parent that clears the field
  // gets the same code reported again when it is entered anew.
  $effect(() => {
    if (clean(value).length !== 8) {
      lastReported = null;
    }
  });

  /**
   * Reports a code once when it becomes complete, and again only for a different complete code, so
   * typing the dash into a complete code (KXQBMRTN, then KXQB-MRTN) does not report it twice.
   */
  function checkCode(raw: string): void {
    const cleaned = clean(raw);
    if (cleaned.length !== 8) {
      complete = false;
      lastReported = null;
      return;
    }
    complete = true;
    const formatted = `${cleaned.slice(0, 4)}-${cleaned.slice(4)}`;
    if (formatted === lastReported) return;
    lastReported = formatted;
    oncomplete?.(formatted);
  }

  function handleInput(e: Event & { currentTarget: HTMLInputElement }): void {
    value = e.currentTarget.value;
    checkCode(value);
    oninput?.(e);
  }
</script>

<Field
  id={effectiveId}
  {label}
  {optional}
  {hint}
  {error}
  {disabled}
  {disabledReason}
  {hintId}
  {errorId}
  {reasonId}
>
  <input
    id={effectiveId}
    class="code-field__input"
    class:code-field__input--invalid={Boolean(error)}
    type="text"
    {name}
    bind:value
    autocomplete="one-time-code"
    spellcheck="false"
    autocapitalize="characters"
    maxlength={12}
    {disabled}
    {placeholder}
    data-testid={testid}
    aria-invalid={error ? 'true' : undefined}
    aria-describedby={describedBy}
    oninput={handleInput}
    {onchange}
    {onblur}
    {onfocus}
    {onkeydown}
  />
</Field>

<style>
  .code-field__input {
    box-sizing: border-box;
    width: 100%;
    max-width: 18rem;
    min-height: var(--control-height);
    height: 3rem;
    padding: var(--space-2) var(--space-4);
    font-family: var(--font-mono);
    font-size: var(--text-2xl);
    font-weight: var(--weight-semibold);
    letter-spacing: 0.15em;
    text-align: center;
    text-transform: uppercase;
    color: var(--color-text-primary);
    background-color: var(--control-bg);
    border: 1px solid var(--control-border);
    border-radius: var(--control-radius);
    transition:
      border-color var(--duration-fast) var(--ease-standard),
      box-shadow var(--duration-fast) var(--ease-standard);
  }

  .code-field__input:focus {
    outline: 2px solid transparent;
    box-shadow: var(--focus-ring);
    border-color: var(--color-brand);
  }

  .code-field__input--invalid,
  .code-field__input[aria-invalid='true'] {
    border-color: var(--color-danger);
  }

  .code-field__input:disabled {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-muted);
    border-color: var(--color-border);
    cursor: not-allowed;
  }

  .code-field__input::placeholder {
    color: var(--color-text-muted);
    opacity: 0.6;
    letter-spacing: 0.15em;
  }
</style>
