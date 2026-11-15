<script lang="ts">
  import Field from './Field.svelte';
  import { t } from '../i18n';

  /**
   * Numeric input owning Field and `<input type="number">` (components-fields.adoc, Special Fields).
   *
   * Draws unit beside control, enforces min/max/step bounds, and offers an explicit
   * never choice where a design offers one (value becomes null, never -1).
   */
  interface Props {
    /** The numeric value or null when never/empty (bindable). */
    value?: number | null;
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
    /** The unit shown beside the field (e.g. days, seconds, MB). */
    unit?: string;
    /** Minimum allowed value. */
    min?: number;
    /** Maximum allowed value. */
    max?: number;
    /** Step increment. */
    step?: number;
    /** Whether to offer an explicit never choice. */
    allowNever?: boolean;
    /** Placeholder example value. */
    placeholder?: string;
    /** Name attribute. */
    name?: string;
    /** Test id. */
    testid?: string;
    /** Event handler for value change. */
    onchange?: (value: number | null) => void;
    /** Event handler for input. */
    oninput?: (e: Event & { currentTarget: HTMLInputElement }) => void;
    /** Event handler for blur. */
    onblur?: (e: FocusEvent & { currentTarget: HTMLInputElement }) => void;
    /** Event handler for focus. */
    onfocus?: (e: FocusEvent & { currentTarget: HTMLInputElement }) => void;
    /** Event handler for keydown. */
    onkeydown?: (e: KeyboardEvent & { currentTarget: HTMLInputElement }) => void;
  }

  let {
    value = $bindable(null),
    id,
    label,
    optional = false,
    hint,
    error,
    disabled = false,
    disabledReason,
    unit,
    min,
    max,
    step,
    allowNever = false,
    placeholder,
    name,
    testid,
    onchange,
    oninput,
    onblur,
    onfocus,
    onkeydown,
  }: Props = $props();

  const generatedId = `nf-${Math.random().toString(36).slice(2, 9)}`;
  const effectiveId = $derived(id ?? generatedId);
  const hintId = $derived(`${effectiveId}-hint`);
  const errorId = $derived(`${effectiveId}-error`);
  const reasonId = $derived(`${effectiveId}-reason`);

  let isNever = $state(false);
  let lastNumericValue = $state<number | null>(null);

  $effect(() => {
    if (allowNever && (value === null || value === undefined)) {
      isNever = true;
    } else if (value !== null && value !== undefined) {
      isNever = false;
      lastNumericValue = value;
    }
  });

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

  function handleInput(e: Event & { currentTarget: HTMLInputElement }): void {
    const raw = e.currentTarget.value.trim();
    if (raw === '') {
      value = null;
    } else {
      let num = Number(raw);
      if (!Number.isNaN(num)) {
        if (min !== undefined && num < min) num = min;
        if (max !== undefined && num > max) num = max;
        value = num;
        lastNumericValue = num;
        e.currentTarget.value = String(num);
      }
    }
    oninput?.(e);
  }

  function handleChange(e: Event & { currentTarget: HTMLInputElement }): void {
    const raw = e.currentTarget.value.trim();
    if (raw === '') {
      value = null;
      onchange?.(null);
    } else {
      let num = Number(raw);
      if (!Number.isNaN(num)) {
        if (min !== undefined && num < min) num = min;
        if (max !== undefined && num > max) num = max;
        value = num;
        lastNumericValue = num;
        e.currentTarget.value = String(num);
        onchange?.(num);
      }
    }
  }

  function handleNeverToggle(e: Event & { currentTarget: HTMLInputElement }): void {
    isNever = e.currentTarget.checked;
    if (isNever) {
      value = null;
      onchange?.(null);
    } else {
      const restored = lastNumericValue ?? min ?? 0;
      value = restored;
      onchange?.(restored);
    }
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
  <div class="number-field__row">
    <input
      id={effectiveId}
      class="number-field__input"
      class:number-field__input--invalid={Boolean(error)}
      type="number"
      inputmode="numeric"
      {min}
      {max}
      {step}
      {name}
      value={isNever ? '' : (value ?? '')}
      disabled={disabled || isNever}
      {placeholder}
      data-testid={testid}
      aria-invalid={error ? 'true' : undefined}
      aria-describedby={describedBy}
      oninput={handleInput}
      onchange={handleChange}
      {onblur}
      {onfocus}
      {onkeydown}
    />

    {#if unit}
      <span class="number-field__unit">{unit}</span>
    {/if}

    {#if allowNever}
      <label class="number-field__never" class:number-field__never--disabled={disabled}>
        <input
          type="checkbox"
          checked={isNever}
          {disabled}
          onchange={handleNeverToggle}
        />
        <span>{$t('common.field.never')}</span>
      </label>
    {/if}
  </div>
</Field>

<style>
  .number-field__row {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .number-field__input {
    box-sizing: border-box;
    width: 8rem;
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

  .number-field__input:focus {
    outline: none;
    box-shadow: var(--focus-ring);
    border-color: var(--color-brand);
  }

  .number-field__input--invalid,
  .number-field__input[aria-invalid='true'] {
    border-color: var(--color-danger);
  }

  .number-field__input:disabled {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-muted);
    border-color: var(--color-border);
    cursor: not-allowed;
  }

  .number-field__unit {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    white-space: nowrap;
  }

  .number-field__never {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    margin-inline-start: var(--space-2);
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    cursor: pointer;
    user-select: none;
  }

  .number-field__never--disabled {
    color: var(--color-text-muted);
    cursor: not-allowed;
  }
</style>
