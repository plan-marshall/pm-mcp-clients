<script lang="ts">
  import Marker, { type MarkerKind } from './Marker.svelte';
  import { uniqueId } from '../utils/uniqueId';

  export interface RadioOption {
    value: string;
    label: string;
    description?: string;
    disabled?: boolean;
    disabledReason?: string;
    marker?: MarkerKind;
  }

  /**
   * Radio group owning fieldset and legend (components-fields.adoc, Choice Controls).
   *
   * Offers row and card forms. The whole row or card is the target. Keyboard-complete
   * with wrapping arrow keys, and 44 px target on a phone.
   */
  interface Props {
    /** Group legend / question text. */
    legend: string;
    /** Group input name attribute. */
    name: string;
    /** Selected option value (bindable). */
    value?: string;
    /** The list of radio options. */
    options: RadioOption[];
    /** Form variant: 'row' (default) or 'card'. */
    variant?: 'row' | 'card';
    /** Whether the whole group is disabled. */
    disabled?: boolean;
    /** Test id. */
    testid?: string;
    /** Event handler for value change. */
    onchange?: (value: string) => void;
  }

  let {
    legend,
    name,
    value = $bindable(''),
    options = [],
    variant = 'row',
    disabled = false,
    testid,
    onchange,
  }: Props = $props();

  /** The group's own id; each option's input, description and reason ids are built from it. */
  const groupId = uniqueId('radio');
  /** The group's root, where the arrow keys find the radio to focus (no selector built from a value). */
  let root: HTMLFieldSetElement;

  /**
   * @param index the option's position
   * @returns the id of the option's input
   */
  function optionId(index: number): string {
    return `${groupId}-${index}`;
  }

  /**
   * @param opt the option
   * @param index the option's position
   * @param isDisabled whether the option is disabled
   * @returns the ids of the option's description and disabled reason, or undefined when it has none
   */
  function describedByOf(
    opt: RadioOption,
    index: number,
    isDisabled: boolean | undefined,
  ): string | undefined {
    const ids: string[] = [];
    if (opt.description) ids.push(`${optionId(index)}-description`);
    if (isDisabled && opt.disabledReason) ids.push(`${optionId(index)}-reason`);
    return ids.length > 0 ? ids.join(' ') : undefined;
  }

  /** Selects an option; a disabled input sends no change, so only an enabled option arrives here. */
  function selectOption(opt: RadioOption): void {
    value = opt.value;
    onchange?.(opt.value);
  }

  function handleKeyDown(e: KeyboardEvent): void {
    const enabled = options.filter((o) => !disabled && !o.disabled);
    if (enabled.length === 0) return;

    let target: RadioOption | null = null;
    const currIdx = enabled.findIndex((o) => o.value === value);

    if (e.key === 'ArrowDown' || e.key === 'ArrowRight') {
      e.preventDefault();
      const nextIdx = currIdx === -1 ? 0 : (currIdx + 1) % enabled.length;
      target = enabled[nextIdx];
    } else if (e.key === 'ArrowUp' || e.key === 'ArrowLeft') {
      e.preventDefault();
      const prevIdx = currIdx === -1 ? enabled.length - 1 : (currIdx - 1 + enabled.length) % enabled.length;
      target = enabled[prevIdx];
    }

    if (target !== null) {
      selectOption(target);
      const radios = root.querySelectorAll<HTMLInputElement>('input[type="radio"]');
      radios[options.indexOf(target)].focus();
    }
  }
</script>

<fieldset class="radio-group radio-group--{variant}" data-testid={testid} bind:this={root}>
  <legend class="radio-group__legend">{legend}</legend>

  <div class="radio-group__options">
    {#each options as opt, i (opt.value)}
      {@const isSelected = value === opt.value}
      {@const isDisabled = disabled || opt.disabled}
      <label
        class="radio-option radio-{variant}"
        class:radio-option--selected={isSelected}
        class:radio-option--disabled={isDisabled}
      >
        <div class="radio-option__header">
          <input
            id={optionId(i)}
            type="radio"
            {name}
            value={opt.value}
            checked={isSelected}
            disabled={isDisabled}
            class="radio-option__input"
            aria-describedby={describedByOf(opt, i, isDisabled)}
            onchange={() => selectOption(opt)}
            onkeydown={handleKeyDown}
          />
          <span class="radio-option__label">{opt.label}</span>
          {#if opt.marker}
            <Marker kind={opt.marker} />
          {/if}
        </div>

        {#if opt.description}
          <span id="{optionId(i)}-description" class="radio-option__description">{opt.description}</span>
        {/if}

        {#if isDisabled && opt.disabledReason}
          <span id="{optionId(i)}-reason" class="radio-option__disabled-reason">{opt.disabledReason}</span>
        {/if}
      </label>
    {/each}
  </div>
</fieldset>

<style>
  .radio-group {
    border: none;
    margin: 0;
    padding: 0;
    width: 100%;
  }

  .radio-group__legend {
    font-size: var(--text-sm);
    font-weight: var(--weight-medium);
    color: var(--color-text-primary);
    line-height: var(--line-height-sm);
    margin-bottom: var(--space-2);
    padding: 0;
  }

  .radio-group--row .radio-group__options {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .radio-group--card .radio-group__options {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: var(--space-3);
  }

  .radio-option {
    box-sizing: border-box;
    cursor: pointer;
    user-select: none;
    transition: all var(--duration-fast) var(--ease-standard);
  }

  .radio-row {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding: var(--space-2);
    min-height: 24px;
    border-radius: var(--control-radius);
  }

  .radio-row:hover:not(.radio-option--disabled) {
    background-color: var(--color-bg-subtle);
  }

  .radio-card {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-4);
    min-height: 44px;
    background-color: var(--control-bg);
    border: 1px solid var(--control-border);
    border-radius: var(--radius-lg);
  }

  .radio-card:hover:not(.radio-option--disabled) {
    border-color: var(--color-brand);
  }

  .radio-card.radio-option--selected {
    border-color: var(--color-brand);
    background-color: var(--color-brand-subtle);
  }

  .radio-option--disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .radio-option__header {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .radio-option__input {
    margin: 0;
    accent-color: var(--color-brand);
    cursor: pointer;
  }

  .radio-option--disabled .radio-option__input {
    cursor: not-allowed;
  }

  .radio-option__label {
    font-size: var(--text-sm);
    font-weight: var(--weight-medium);
    color: var(--color-text-primary);
  }

  .radio-option__description {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    line-height: var(--line-height-xs);
    margin-inline-start: calc(var(--space-2) + 16px);
  }

  .radio-card .radio-option__description {
    margin-inline-start: 0;
  }

  .radio-option__disabled-reason {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    line-height: var(--line-height-xs);
    margin-inline-start: calc(var(--space-2) + 16px);
  }

  .radio-card .radio-option__disabled-reason {
    margin-inline-start: 0;
  }

  @media (max-width: 767px) {
    .radio-option {
      min-height: 44px;
    }
  }
</style>
