<script lang="ts">
  import Field from './Field.svelte';

  export interface SelectOption {
    value: string;
    label: string;
    disabled?: boolean;
    disabledReason?: string;
  }

  /**
   * Dropdown select input owning Field and native `<select>` (components-fields.adoc, Choice Controls).
   *
   * For more than five options or a list the runtime sends. Draws `--control-height`
   * (34 px desktop, 44 px phone), `--control-radius`, `--control-bg` and `--control-border`.
   */
  interface Props {
    /** Selected value (bindable). */
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
    /** The options to choose from. */
    options?: SelectOption[];
    /** Placeholder option shown when no value selected. */
    placeholder?: string;
    /** Name attribute. */
    name?: string;
    /** Test id. */
    testid?: string;
    /** Event handler for value change. */
    onchange?: (value: string) => void;
    /** Event handler for input. */
    oninput?: (e: Event & { currentTarget: HTMLSelectElement }) => void;
    /** Event handler for blur. */
    onblur?: (e: FocusEvent & { currentTarget: HTMLSelectElement }) => void;
    /** Event handler for focus. */
    onfocus?: (e: FocusEvent & { currentTarget: HTMLSelectElement }) => void;
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
    options = [],
    placeholder,
    name,
    testid,
    onchange,
    oninput,
    onblur,
    onfocus,
  }: Props = $props();

  const generatedId = `sel-${Math.random().toString(36).slice(2, 9)}`;
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

  function handleChange(e: Event & { currentTarget: HTMLSelectElement }): void {
    value = e.currentTarget.value;
    onchange?.(value);
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
  <select
    id={effectiveId}
    class="select-field__select"
    class:select-field__select--invalid={Boolean(error)}
    {name}
    bind:value
    {disabled}
    data-testid={testid}
    aria-invalid={error ? 'true' : undefined}
    aria-describedby={describedBy}
    onchange={handleChange}
    {oninput}
    {onblur}
    {onfocus}
  >
    {#if placeholder}
      <option value="" disabled selected={!value}>{placeholder}</option>
    {/if}
    {#each options as opt (opt.value)}
      <option value={opt.value} disabled={opt.disabled} title={opt.disabledReason}>
        {opt.label}{opt.disabled && opt.disabledReason ? ` (${opt.disabledReason})` : ''}
      </option>
    {/each}
  </select>
</Field>

<style>
  .select-field__select {
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
    cursor: pointer;
    transition:
      border-color var(--duration-fast) var(--ease-standard),
      box-shadow var(--duration-fast) var(--ease-standard);
  }

  .select-field__select:focus {
    outline: none;
    box-shadow: var(--focus-ring);
    border-color: var(--color-brand);
  }

  .select-field__select--invalid,
  .select-field__select[aria-invalid='true'] {
    border-color: var(--color-danger);
  }

  .select-field__select:disabled {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-muted);
    border-color: var(--color-border);
    cursor: not-allowed;
  }
</style>
