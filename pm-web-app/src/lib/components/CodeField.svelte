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
    /**
     * Whether the field holds a complete code (8 characters), derived from the value whoever wrote
     * it (bindable, read-only: a value passed in is overwritten).
     */
    complete?: boolean;
    /** Called with the normalized code when the operator's input completes a new code. */
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

  /** The complete code the field holds, as `KXQB-MRTN`; null while the code is incomplete. */
  let held: string | null = null;

  /**
   * @param raw the text of the field, in any case, with or without dash or spaces
   * @returns the code as `KXQB-MRTN` when the text holds exactly 8 letters and digits, else null
   */
  function codeOf(raw: string): string | null {
    const cleaned = raw.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
    return cleaned.length === 8 ? `${cleaned.slice(0, 4)}-${cleaned.slice(4)}` : null;
  }

  /** @param code the complete code the field holds now, or null; `complete` follows it */
  function hold(code: string | null): void {
    held = code;
    complete = code !== null;
  }

  // `complete` is derived from the value in both directions, whoever wrote it: a parent that
  // assigns a complete code makes the field complete, one that clears or shortens it does not.
  $effect(() => hold(codeOf(value)));

  /**
   * Reports to `oncomplete` a code the operator completes: once when it becomes complete, and again
   * only for a different code, so typing the dash into a complete code (KXQBMRTN, then KXQB-MRTN)
   * reports nothing new. A code the parent writes is known to the parent and is not reported.
   */
  function handleInput(e: Event & { currentTarget: HTMLInputElement }): void {
    value = e.currentTarget.value;
    const code = codeOf(value);
    const completed = code !== null && code !== held;
    hold(code);
    if (completed) oncomplete?.(code);
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
