<script lang="ts">
  import Marker, { type MarkerKind } from './Marker.svelte';

  export interface CheckOption {
    value: string;
    label: string;
    description?: string;
    disabled?: boolean;
    disabledReason?: string;
    marker?: MarkerKind;
  }

  /**
   * Checkbox group owning fieldset and legend (components-fields.adoc, Choice Controls).
   *
   * Row and card forms for a decision part that accepts several answers. The whole
   * row or card is the target. Supports none, one, or all checked, and 44 px target on a phone.
   */
  interface Props {
    /** Group legend / question text. */
    legend: string;
    /** Selected option values (bindable). */
    values?: string[];
    /** The list of check options. */
    options: CheckOption[];
    /** Form variant: 'row' (default) or 'card'. */
    variant?: 'row' | 'card';
    /** Whether the whole group is disabled. */
    disabled?: boolean;
    /** Test id. */
    testid?: string;
    /** Event handler for selection changes. */
    onchange?: (values: string[]) => void;
  }

  let {
    legend,
    values = $bindable([]),
    options = [],
    variant = 'row',
    disabled = false,
    testid,
    onchange,
  }: Props = $props();

  function toggleOption(opt: CheckOption, e?: MouseEvent): void {
    if (disabled || opt.disabled) return;
    if (e && e.target instanceof HTMLInputElement) {
      // Input already toggled in native DOM
    } else if (e) {
      e.preventDefault();
    }

    const current = [...values];
    const idx = current.indexOf(opt.value);
    if (idx >= 0) {
      current.splice(idx, 1);
    } else {
      current.push(opt.value);
    }
    values = current;
    onchange?.(current);
  }
</script>

<fieldset class="check-group check-group--{variant}" data-testid={testid}>
  <legend class="check-group__legend">{legend}</legend>

  <div class="check-group__options">
    {#each options as opt (opt.value)}
      {@const isChecked = values.includes(opt.value)}
      {@const isDisabled = disabled || opt.disabled}
      <!-- svelte-ignore a11y_click_events_have_key_events -->
      <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
      <label
        class="check-option check-{variant}"
        class:check-option--checked={isChecked}
        class:check-option--disabled={isDisabled}
        onclick={(e) => toggleOption(opt, e)}
      >
        <div class="check-option__header">
          <input
            type="checkbox"
            value={opt.value}
            checked={isChecked}
            disabled={isDisabled}
            class="check-option__input"
          />
          <span class="check-option__label">{opt.label}</span>
          {#if opt.marker}
            <Marker kind={opt.marker} />
          {/if}
        </div>

        {#if opt.description}
          <span class="check-option__description">{opt.description}</span>
        {/if}

        {#if isDisabled && opt.disabledReason}
          <span class="check-option__disabled-reason">{opt.disabledReason}</span>
        {/if}
      </label>
    {/each}
  </div>
</fieldset>

<style>
  .check-group {
    border: none;
    margin: 0;
    padding: 0;
    width: 100%;
  }

  .check-group__legend {
    font-size: var(--text-sm);
    font-weight: var(--weight-medium);
    color: var(--color-text-primary);
    line-height: var(--line-height-sm);
    margin-bottom: var(--space-2);
    padding: 0;
  }

  .check-group--row .check-group__options {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .check-group--card .check-group__options {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: var(--space-3);
  }

  .check-option {
    box-sizing: border-box;
    cursor: pointer;
    user-select: none;
    transition: all var(--duration-fast) var(--ease-standard);
  }

  .check-row {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding: var(--space-2);
    min-height: 24px;
    border-radius: var(--control-radius);
  }

  .check-row:hover:not(.check-option--disabled) {
    background-color: var(--color-bg-subtle);
  }

  .check-card {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-4);
    min-height: 44px;
    background-color: var(--control-bg);
    border: 1px solid var(--control-border);
    border-radius: var(--radius-lg);
  }

  .check-card:hover:not(.check-option--disabled) {
    border-color: var(--color-brand);
  }

  .check-card.check-option--checked {
    border-color: var(--color-brand);
    background-color: var(--color-brand-subtle);
  }

  .check-option--disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .check-option__header {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .check-option__input {
    margin: 0;
    accent-color: var(--color-brand);
    cursor: pointer;
  }

  .check-option--disabled .check-option__input {
    cursor: not-allowed;
  }

  .check-option__label {
    font-size: var(--text-sm);
    font-weight: var(--weight-medium);
    color: var(--color-text-primary);
  }

  .check-option__description {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    line-height: var(--line-height-xs);
    margin-inline-start: calc(var(--space-2) + 16px);
  }

  .check-card .check-option__description {
    margin-inline-start: 0;
  }

  .check-option__disabled-reason {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    line-height: var(--line-height-xs);
    margin-inline-start: calc(var(--space-2) + 16px);
  }

  .check-card .check-option__disabled-reason {
    margin-inline-start: 0;
  }

  @media (max-width: 767px) {
    .check-option {
      min-height: 44px;
    }
  }
</style>
