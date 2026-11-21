<script lang="ts">
  import type { HTMLInputAttributes } from 'svelte/elements';
  import Field from './Field.svelte';
  import { uniqueId } from '../utils/uniqueId';

  /**
   * Free text input owning Field and `<input>` (components-fields.adoc, One Field System).
   *
   * Draws `--control-height` (34 px desktop, 44 px phone), `--control-radius`, `--control-bg`
   * and `--control-border`. Supports a monospace variant for paths, ids and references.
   */
  interface Props {
    /** The control value (bindable). */
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
    /** Whether the input is disabled. */
    disabled?: boolean;
    /** Reason the field is disabled. */
    disabledReason?: string;
    /** Monospace variant for paths, ids and references. */
    mono?: boolean;
    /** Placeholder example value. Never the label or hint. */
    placeholder?: string;
    /** Input type (e.g. text, password, url). */
    type?: string;
    /** Input name attribute. */
    name?: string;
    /** Autocomplete attribute. */
    autocomplete?: HTMLInputAttributes['autocomplete'];
    /** Spellcheck attribute. */
    spellcheck?: boolean | 'true' | 'false';
    /** Autocapitalize attribute. */
    autocapitalize?: HTMLInputAttributes['autocapitalize'];
    /** Autofocus attribute. */
    autofocus?: boolean;
    /** Readonly attribute. */
    readonly?: boolean;
    /** Input mode attribute. */
    inputmode?: 'none' | 'text' | 'decimal' | 'numeric' | 'tel' | 'search' | 'email' | 'url';
    /** Maximum length. */
    maxlength?: number;
    /** Minimum length. */
    minlength?: number;
    /** Pattern regex. */
    pattern?: string;
    /** Test id. */
    testid?: string;
    /** Event handlers. */
    oninput?: (e: Event & { currentTarget: HTMLInputElement }) => void;
    /** Event handler for input commit. */
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
    mono = false,
    placeholder,
    type = 'text',
    name,
    autocomplete,
    spellcheck,
    autocapitalize,
    autofocus,
    readonly,
    inputmode,
    maxlength,
    minlength,
    pattern,
    testid,
    oninput,
    onchange,
    onblur,
    onfocus,
    onkeydown,
  }: Props = $props();

  const generatedId = uniqueId('tf');
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
  <!-- svelte-ignore a11y_autofocus -->
  <input
    id={effectiveId}
    class="text-field__input"
    class:text-field__input--mono={mono}
    class:text-field__input--invalid={Boolean(error)}
    {type}
    {name}
    bind:value
    {placeholder}
    {disabled}
    {readonly}
    {autocomplete}
    {spellcheck}
    {autocapitalize}
    {autofocus}
    {inputmode}
    {maxlength}
    {minlength}
    {pattern}
    data-testid={testid}
    aria-invalid={error ? 'true' : undefined}
    aria-describedby={describedBy}
    {oninput}
    {onchange}
    {onblur}
    {onfocus}
    {onkeydown}
  />
</Field>

<style>
  .text-field__input {
    box-sizing: border-box;
    width: 100%;
    min-height: var(--control-height);
    padding: var(--space-1) var(--space-3);
    font-family: var(--font-sans);
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

  .text-field__input:focus {
    outline: 2px solid transparent;
    box-shadow: var(--focus-ring);
    border-color: var(--color-brand);
  }

  .text-field__input--invalid,
  .text-field__input[aria-invalid='true'] {
    border-color: var(--color-danger);
  }

  .text-field__input--mono {
    font-family: var(--font-mono);
    font-feature-settings: var(--font-feature-settings);
  }

  .text-field__input:disabled {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-muted);
    border-color: var(--color-border);
    cursor: not-allowed;
  }

  .text-field__input::placeholder {
    color: var(--color-text-muted);
    opacity: 1;
  }
</style>
