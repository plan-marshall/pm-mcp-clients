<script lang="ts">
  import Field from './Field.svelte';
  import { uniqueId } from '../utils/uniqueId';

  /**
   * Multi-line text input owning Field and `<textarea>` (components-fields.adoc, One Field System).
   *
   * Draws `--control-radius`, `--control-bg` and `--control-border`. Supports a monospace variant.
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
    /** Whether the textarea is disabled. */
    disabled?: boolean;
    /** Reason the field is disabled. */
    disabledReason?: string;
    /** Monospace variant for code, logs and references. */
    mono?: boolean;
    /** Number of visible text lines. */
    rows?: number;
    /** Placeholder example value. Never the label or hint. */
    placeholder?: string;
    /** Textarea name attribute. */
    name?: string;
    /** Spellcheck attribute. */
    spellcheck?: boolean | 'true' | 'false';
    /** Autofocus attribute. */
    autofocus?: boolean;
    /** Readonly attribute. */
    readonly?: boolean;
    /** Maximum length. */
    maxlength?: number;
    /** Test id. */
    testid?: string;
    /** Event handlers. */
    oninput?: (e: Event & { currentTarget: HTMLTextAreaElement }) => void;
    /** Event handler for change. */
    onchange?: (e: Event & { currentTarget: HTMLTextAreaElement }) => void;
    /** Event handler for blur. */
    onblur?: (e: FocusEvent & { currentTarget: HTMLTextAreaElement }) => void;
    /** Event handler for focus. */
    onfocus?: (e: FocusEvent & { currentTarget: HTMLTextAreaElement }) => void;
    /** Event handler for keydown. */
    onkeydown?: (e: KeyboardEvent & { currentTarget: HTMLTextAreaElement }) => void;
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
    rows = 4,
    placeholder,
    name,
    spellcheck,
    autofocus,
    readonly,
    maxlength,
    testid,
    oninput,
    onchange,
    onblur,
    onfocus,
    onkeydown,
  }: Props = $props();

  const generatedId = uniqueId('ta');
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
  <textarea
    id={effectiveId}
    class="text-area__input"
    class:text-area__input--mono={mono}
    class:text-area__input--invalid={Boolean(error)}
    {name}
    bind:value
    {rows}
    {placeholder}
    {disabled}
    {readonly}
    {spellcheck}
    {autofocus}
    {maxlength}
    data-testid={testid}
    aria-invalid={error ? 'true' : undefined}
    aria-describedby={describedBy}
    {oninput}
    {onchange}
    {onblur}
    {onfocus}
    {onkeydown}
  ></textarea>
</Field>

<style>
  .text-area__input {
    box-sizing: border-box;
    width: 100%;
    min-height: calc(var(--control-height) * 2);
    padding: var(--space-2) var(--space-3);
    font-family: var(--font-sans);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
    color: var(--color-text-primary);
    background-color: var(--control-bg);
    border: 1px solid var(--control-border);
    border-radius: var(--control-radius);
    resize: vertical;
    transition:
      border-color var(--duration-fast) var(--ease-standard),
      box-shadow var(--duration-fast) var(--ease-standard);
  }

  .text-area__input:focus {
    outline: 2px solid transparent;
    box-shadow: var(--focus-ring);
    border-color: var(--color-brand);
  }

  .text-area__input--invalid,
  .text-area__input[aria-invalid='true'] {
    border-color: var(--color-danger);
  }

  .text-area__input--mono {
    font-family: var(--font-mono);
    font-feature-settings: var(--font-feature-settings);
  }

  .text-area__input:disabled {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-muted);
    border-color: var(--color-border);
    cursor: not-allowed;
  }

  .text-area__input::placeholder {
    color: var(--color-text-muted);
    opacity: 1;
  }
</style>
