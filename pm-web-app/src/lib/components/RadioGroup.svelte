<script lang="ts">
  import Marker, { type MarkerKind } from './Marker.svelte';

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

  function selectOption(opt: RadioOption): void {
    if (disabled || opt.disabled) return;
    value = opt.value;
    onchange?.(opt.value);
  }

  function handleKeyDown(e: KeyboardEvent, _currentIndex: number): void {
    const enabled = options.filter((o) => !disabled && !o.disabled);
    if (enabled.length === 0) return;

    let targetValue: string | null = null;
    const currIdx = enabled.findIndex((o) => o.value === value);

    if (e.key === 'ArrowDown' || e.key === 'ArrowRight') {
      e.preventDefault();
      const nextIdx = currIdx === -1 ? 0 : (currIdx + 1) % enabled.length;
      targetValue = enabled[nextIdx].value;
    } else if (e.key === 'ArrowUp' || e.key === 'ArrowLeft') {
      e.preventDefault();
      const prevIdx = currIdx === -1 ? enabled.length - 1 : (currIdx - 1 + enabled.length) % enabled.length;
      targetValue = enabled[prevIdx].value;
    }

    if (targetValue !== null) {
      value = targetValue;
      onchange?.(targetValue);
      const radioEl = document.querySelector<HTMLInputElement>(
        `input[type="radio"][name="${name}"][value="${targetValue}"]`,
      );
      radioEl?.focus();
    }
  }
</script>

<fieldset class="radio-group radio-group--{variant}" data-testid={testid}>
  <legend class="radio-group__legend">{legend}</legend>

  <div class="radio-group__options">
    {#each options as opt, i (opt.value)}
      {@const isSelected = value === opt.value}
      {@const isDisabled = disabled || opt.disabled}
      <!-- svelte-ignore a11y_click_events_have_key_events -->
      <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
      <label
        class="radio-option radio-{variant}"
        class:radio-option--selected={isSelected}
        class:radio-option--disabled={isDisabled}
        onclick={() => selectOption(opt)}
      >
        <div class="radio-option__header">
          <input
            type="radio"
            {name}
            value={opt.value}
            checked={isSelected}
            disabled={isDisabled}
            class="radio-option__input"
            onkeydown={(e) => handleKeyDown(e, i)}
          />
          <span class="radio-option__label">{opt.label}</span>
          {#if opt.marker}
            <Marker kind={opt.marker} />
          {/if}
        </div>

        {#if opt.description}
          <span class="radio-option__description">{opt.description}</span>
        {/if}

        {#if isDisabled && opt.disabledReason}
          <span class="radio-option__disabled-reason">{opt.disabledReason}</span>
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
