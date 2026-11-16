<script lang="ts">
  /**
   * Checkbox control for an independent yes or no (components-fields.adoc, Choice Controls).
   *
   * The whole label row is the target. Keyboard-complete with Space, and 44 px target on a phone.
   */
  interface Props {
    /** Whether the box is checked (bindable). */
    checked?: boolean;
    /** The element id. Auto-generated when omitted. */
    id?: string;
    /** The visible label text. */
    label: string;
    /** Optional secondary description. */
    description?: string;
    /** Whether the control is disabled. */
    disabled?: boolean;
    /** Reason the control is disabled. */
    disabledReason?: string;
    /** Input name attribute. */
    name?: string;
    /** Test id. */
    testid?: string;
    /** Event handler for check change. */
    onchange?: (checked: boolean) => void;
  }

  let {
    checked = $bindable(false),
    id,
    label,
    description,
    disabled = false,
    disabledReason,
    name,
    testid,
    onchange,
  }: Props = $props();

  const generatedId = `chk-${Math.random().toString(36).slice(2, 9)}`;
  const effectiveId = $derived(id ?? generatedId);

  function handleClick(e: MouseEvent): void {
    if (disabled) return;
    if (e.target instanceof HTMLInputElement) {
      checked = e.target.checked;
      onchange?.(checked);
      return;
    }
    e.preventDefault();
    checked = !checked;
    onchange?.(checked);
  }

  function handleKeyDown(e: KeyboardEvent): void {
    if (disabled) return;
    if (e.key === ' ') {
      e.preventDefault();
      checked = !checked;
      onchange?.(checked);
    }
  }
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<label
  class="check-field"
  class:check-field--checked={checked}
  class:check-field--disabled={disabled}
  onclick={handleClick}
>
  <div class="check-field__row">
    <input
      id={effectiveId}
      type="checkbox"
      {name}
      bind:checked
      {disabled}
      data-testid={testid}
      class="check-field__input"
      onkeydown={handleKeyDown}
    />
    <span class="check-field__label">{label}</span>
  </div>

  {#if description}
    <span class="check-field__description">{description}</span>
  {/if}

  {#if disabled && disabledReason}
    <span class="check-field__disabled-reason">{disabledReason}</span>
  {/if}
</label>

<style>
  .check-field {
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding: var(--space-2);
    min-height: 24px;
    border-radius: var(--control-radius);
    cursor: pointer;
    user-select: none;
    transition: background-color var(--duration-fast) var(--ease-standard);
  }

  .check-field:hover:not(.check-field--disabled) {
    background-color: var(--color-bg-subtle);
  }

  .check-field--disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .check-field__row {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .check-field__input {
    margin: 0;
    accent-color: var(--color-brand);
    cursor: pointer;
  }

  .check-field--disabled .check-field__input {
    cursor: not-allowed;
  }

  .check-field__label {
    font-size: var(--text-sm);
    font-weight: var(--weight-medium);
    color: var(--color-text-primary);
    line-height: var(--line-height-sm);
  }

  .check-field__description {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    line-height: var(--line-height-xs);
    margin-inline-start: calc(var(--space-2) + 16px);
  }

  .check-field__disabled-reason {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    line-height: var(--line-height-xs);
    margin-inline-start: calc(var(--space-2) + 16px);
  }

  @media (max-width: 767px) {
    .check-field {
      min-height: 44px;
    }
  }
</style>
