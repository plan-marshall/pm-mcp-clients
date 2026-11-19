<script lang="ts">
  import { advisory } from '../utils/advisoryTitle';
  import Icon from './Icon.svelte';
  import { uniqueId } from '../utils/uniqueId';

  /**
   * Toggle (VD-P-05): one thing on or off (favourite, follow mode, reveal secret).
   *
   * A `<button>` with `aria-pressed`, its label naming the thing, not the state.
   * The pressed state is drawn by the icon and the pressed ground (--color-brand-subtle),
   * never by colour alone.
   *
   * A toggle that cannot act because of a reason (`disabledReason`) stays in the tab order
   * (`aria-disabled="true"` instead of native `disabled`; title and `aria-describedby` carry the
   * reason; click does nothing). A toggle with no reason keeps native `disabled`.
   */
  export let pressed = false;
  export let label: string;
  export let icon: string;
  export let pressedIcon: string | undefined = undefined;
  export let disabled = false;
  /** Why the control cannot act; the reason becomes its advisory and its description. */
  export let disabledReason: string | null | undefined = undefined;
  export let onToggle: (pressed: boolean) => void = () => {};

  /** The id of the element that carries the reason, for `aria-describedby`. */
  const reasonId = uniqueId('toggle-reason');

  $: hasReason = disabled && !!disabledReason;
  $: isAriaDisabled = hasReason;
  $: isNativeDisabled = disabled && !disabledReason;
  $: reason = hasReason ? (disabledReason as string) : '';
  $: describedBy =
    [reason ? reasonId : null, $$restProps['aria-describedby']].filter(Boolean).join(' ') ||
    undefined;
  $: effectiveIcon = pressed && pressedIcon ? pressedIcon : icon;

  function handleClick(event: MouseEvent): void {
    if (disabled) {
      event.preventDefault();
      event.stopPropagation();
      event.stopImmediatePropagation();
      return;
    }
    const next = !pressed;
    pressed = next;
    onToggle(next);
  }
</script>

<button
  type="button"
  class="toggle"
  class:toggle--no-pressed-icon={!pressedIcon}
  class:toggle--disabled={disabled}
  aria-pressed={pressed}
  aria-label={label}
  title={reason || label || $$restProps['title']}
  disabled={isNativeDisabled ? true : undefined}
  aria-disabled={isAriaDisabled ? 'true' : undefined}
  aria-describedby={describedBy}
  use:advisory
  on:click={handleClick}
  on:click
  {...$$restProps}
>
  <span class="toggle__icon" data-testid="toggle-icon" data-icon={effectiveIcon}>
    <Icon name={effectiveIcon} size={16} />
    {#if pressed && !pressedIcon}
      <span class="toggle__indicator" aria-hidden="true"></span>
    {/if}
  </span>
</button>

{#if reason}
  <span class="sr-only" id={reasonId}>{reason}</span>
{/if}

<style>
  .toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-block-size: 24px;
    min-inline-size: 24px;
    padding: var(--space-1);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    cursor: pointer;
    transition:
      background-color 150ms ease,
      border-color 150ms ease;
  }

  .toggle:hover:not(:disabled):not([aria-disabled='true']) {
    background-color: var(--color-bg-subtle);
  }

  .toggle[aria-pressed='true'] {
    background-color: var(--color-brand-subtle);
    border-color: var(--color-border);
    color: var(--color-text-primary);
  }

  .toggle:focus {
    outline: none;
    box-shadow: var(--focus-ring);
  }

  .toggle:disabled,
  .toggle[aria-disabled='true'] {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .toggle__icon {
    position: relative;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .toggle__indicator {
    position: absolute;
    inset-block-end: -2px;
    inset-inline: 0;
    block-size: 2px;
    border-radius: 1px;
    background-color: var(--color-brand);
  }

  @media (max-width: 767px) {
    .toggle {
      min-block-size: 44px;
      min-inline-size: 44px;
      padding: var(--space-2);
    }
  }
</style>
