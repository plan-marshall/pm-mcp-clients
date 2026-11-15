<script context="module" lang="ts">
  export interface SegmentedControlOption {
    id: string;
    label: string;
  }
</script>

<script lang="ts">
  import { tick } from 'svelte';

  /**
   * SegmentedControl (VD-P-05, VD-S-03): two to five options that switch how the same data is shown.
   *
   * It is a `role="radiogroup"` whose options carry `aria-checked`; the chosen option sits on
   * `--color-brand-subtle` with the text in `--color-text-primary`, never a solid brand fill.
   * Arrow keys move and wrap around the options with a single tab stop on the active option.
   */
  export let options: SegmentedControlOption[] = [];
  export let value: string;
  export let label: string;
  export let onChange: (id: string) => void = () => {};

  if (options.length < 2 || options.length > 5) {
    throw new Error(`SegmentedControl requires between 2 and 5 options, received ${options.length}`);
  }

  let buttonElements: HTMLButtonElement[] = [];

  function selectOption(id: string): void {
    if (id === value) return;
    value = id;
    onChange(id);
  }

  async function focusAndSelect(index: number): Promise<void> {
    const nextOption = options[index];
    if (!nextOption) return;
    selectOption(nextOption.id);
    await tick();
    buttonElements[index]?.focus();
  }

  function onKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      event.preventDefault();
      const nextIndex = (index + 1) % options.length;
      void focusAndSelect(nextIndex);
      return;
    }
    if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      event.preventDefault();
      const prevIndex = (index - 1 + options.length) % options.length;
      void focusAndSelect(prevIndex);
      return;
    }
    if (event.key === 'Home') {
      event.preventDefault();
      void focusAndSelect(0);
      return;
    }
    if (event.key === 'End') {
      event.preventDefault();
      void focusAndSelect(options.length - 1);
      return;
    }
  }
</script>

<div class="segmented-control" role="radiogroup" aria-label={label} {...$$restProps}>
  {#each options as option, i (option.id)}
    {@const isSelected = option.id === value}
    <button
      type="button"
      role="radio"
      aria-checked={isSelected}
      tabindex={isSelected ? 0 : -1}
      class="segmented-control__option"
      bind:this={buttonElements[i]}
      on:click={() => selectOption(option.id)}
      on:keydown={(e) => onKeydown(e, i)}
    >
      <span class="segmented-control__label">{option.label}</span>
    </button>
  {/each}
</div>

<style>
  .segmented-control {
    display: inline-flex;
    align-items: center;
    padding: 2px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
    gap: 2px;
  }

  .segmented-control__option {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-block-size: 24px;
    padding: var(--space-1) var(--space-3);
    border: none;
    border-radius: var(--radius-sm);
    background: transparent;
    color: var(--color-text-muted);
    font-size: var(--text-xs);
    font-weight: 500;
    line-height: var(--line-height-sm);
    cursor: pointer;
    white-space: nowrap;
    transition: background-color 150ms ease, color 150ms ease;
  }

  .segmented-control__option:hover:not([aria-checked="true"]) {
    color: var(--color-text-primary);
    background-color: var(--color-bg-raised);
  }

  .segmented-control__option[aria-checked="true"] {
    background-color: var(--color-brand-subtle);
    color: var(--color-text-primary);
    font-weight: 600;
    box-shadow: var(--shadow-sm);
  }

  .segmented-control__option:focus {
    outline: none;
    box-shadow: var(--focus-ring);
  }

  .segmented-control__label {
    display: inline-block;
  }

  @media (max-width: 767px) {
    .segmented-control__option {
      min-block-size: 44px;
      padding: var(--space-2) var(--space-4);
      font-size: var(--text-sm);
    }
  }
</style>
