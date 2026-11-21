<script lang="ts">
  import { untrack } from 'svelte';
  import Field from './Field.svelte';
  import { uniqueId } from '../utils/uniqueId';
  import { t } from '../i18n';

  /**
   * Numeric input owning Field and `<input type="number">` (components-fields.adoc, Special Fields).
   *
   * Draws unit beside control, takes what is typed as it is and clamps to the min/max bounds on
   * commit (blur, Enter, step), and offers an explicit never choice where a design offers one
   * (value becomes null, never -1). Emptying the field leaves it editable; only the never checkbox
   * and a null the parent assigns (initially or later) turn never on, and a number the parent
   * assigns turns it off again.
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

  const generatedId = uniqueId('nf');
  const effectiveId = $derived(id ?? generatedId);
  const hintId = $derived(`${effectiveId}-hint`);
  const errorId = $derived(`${effectiveId}-error`);
  const reasonId = $derived(`${effectiveId}-reason`);

  // Typing never changes the never choice; its checkbox does, and so does a value the parent assigns:
  // null from outside (and an initial null) turns a never field to never, a number turns it off.
  let isNever = $state(untrack(() => allowNever && (value === null || value === undefined)));
  let lastNumericValue = $state<number | null>(null);
  // The value this field wrote last, so a value differing from it is known to come from outside.
  let ownValue: number | null | undefined = untrack(() => value);
  // Set by a commit on Enter, so the change event the browser sends after it does not commit twice.
  let committedOnEnter = false;

  $effect(() => {
    const current = value;
    const empty = current === null || current === undefined;
    if (!empty) {
      lastNumericValue = current;
    }
    if (current !== ownValue) {
      ownValue = current;
      isNever = allowNever && empty;
    }
  });

  /**
   * Writes a value this field produced itself, so the effect does not take it for one from outside.
   *
   * @param next the value to hold
   */
  function setOwn(next: number | null): void {
    ownValue = next;
    value = next;
  }

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

  /**
   * A number input's value is either empty or a valid floating-point number (the browser sanitizes
   * anything else to empty), so the text parses to null or to a number.
   *
   * @param text the input's value
   * @returns null for an empty field, otherwise the number typed
   */
  function parse(text: string): number | null {
    const raw = text.trim();
    return raw === '' ? null : Number(raw);
  }

  /**
   * Takes what is typed as it is: no clamping and no rewriting of the text, so a value on its way
   * into the range (1 on the way to 15 with a minimum of 10) is not overwritten.
   */
  function handleInput(e: Event & { currentTarget: HTMLInputElement }): void {
    committedOnEnter = false;
    setOwn(parse(e.currentTarget.value));
    oninput?.(e);
  }

  /**
   * Commits the field: clamps to min and max, writes the clamped text back and reports it.
   *
   * @param input the number input
   */
  function commit(input: HTMLInputElement): void {
    let num = parse(input.value);
    if (num !== null) {
      if (min !== undefined && num < min) num = min;
      if (max !== undefined && num > max) num = max;
      lastNumericValue = num;
      input.value = String(num);
    }
    setOwn(num);
    onchange?.(num);
  }

  function handleChange(e: Event & { currentTarget: HTMLInputElement }): void {
    if (committedOnEnter) {
      committedOnEnter = false;
      return;
    }
    commit(e.currentTarget);
  }

  function handleKeyDown(e: KeyboardEvent & { currentTarget: HTMLInputElement }): void {
    if (e.key === 'Enter') {
      commit(e.currentTarget);
      committedOnEnter = true;
    }
    onkeydown?.(e);
  }

  function handleNeverToggle(e: Event & { currentTarget: HTMLInputElement }): void {
    isNever = e.currentTarget.checked;
    if (isNever) {
      setOwn(null);
      onchange?.(null);
    } else {
      const restored = lastNumericValue ?? min ?? 0;
      setOwn(restored);
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
      onkeydown={handleKeyDown}
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
    outline: 2px solid transparent;
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

  @media (max-width: 767px) {
    .number-field__never {
      min-height: var(--control-height);
    }
  }
</style>
