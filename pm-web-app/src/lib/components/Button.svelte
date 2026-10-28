<script lang="ts">
  import { advisory } from '../utils/advisoryTitle';
  import Icon from './Icon.svelte';
  import { uniqueId } from '../utils/uniqueId';

  /**
   * A control of the design system (VD-P-05): its four variants, its three sizes, an optional icon,
   * and a disabled state that says why.
   *
   * `variant`, `type`, `loading` and the click forwarding are the surface the views already use;
   * the sizes, the icon and the disabled reason are additive, so no call site changes its markup.
   * The `small` spelling the views already pass draws the `sm` size — before this change it was an
   * inert attribute, so those ten buttons shrink to the small size they asked for.
   * `danger` belongs inside a danger dialog, where the consequence is named beside the act.
   */
  export let variant: 'primary' | 'secondary' | 'danger' | 'ghost' = 'secondary';
  export let type: 'button' | 'submit' | 'reset' = 'button';
  /**
   * The size; `small` is the spelling the views already pass and draws the `sm` size.
   */
  export let size: 'sm' | 'md' | 'lg' | 'small' = 'md';
  /** The icon left of the label, from the delivered UI icon set. */
  export let icon: string | null | undefined = undefined;
  export let disabled = false;
  /** Why the control cannot act; the reason becomes its advisory and its description. */
  export let disabledReason: string | null | undefined = undefined;
  export let loading = false;

  /** The id of the element that carries the reason, for `aria-describedby`. */
  const reasonId = uniqueId('btn-reason');

  /** The delivered icon size of each button size. */
  const ICON_SIZES = { sm: 16, small: 16, md: 20, lg: 20 } as const;

  $: reason = disabled && disabledReason ? disabledReason : '';
  $: shape = size === 'small' ? 'sm' : size;
  $: describedBy =
    [reason ? reasonId : null, $$restProps['aria-describedby']].filter(Boolean).join(' ') ||
    undefined;
  $: iconSize = ICON_SIZES[size];
</script>

<button
  {type}
  class="btn btn--{variant} btn--{shape}"
  class:btn--loading={loading}
  disabled={disabled || loading}
  {...$$restProps}
  title={reason || $$restProps['title']}
  aria-describedby={describedBy}
  use:advisory
  on:click
>
  {#if loading}
    <span class="btn__spinner" aria-hidden="true"></span>
  {/if}
  {#if icon}
    <Icon name={icon} size={iconSize} />
  {/if}
  <span class="btn__content"><slot /></span>
</button>

{#if reason}
  <span class="sr-only" id={reasonId}>{reason}</span>
{/if}

<style>
  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-4);
    font-size: var(--text-sm);
    font-weight: 500;
    line-height: 1.25;
    border-radius: var(--radius-md);
    transition: all var(--duration-fast) var(--ease-standard);
    white-space: nowrap;
    border: 1px solid transparent;
  }

  .btn--sm {
    min-height: 28px;
    padding: var(--space-1) var(--space-3);
    font-size: var(--text-xs);
  }

  .btn--md {
    min-height: 34px;
  }

  .btn--lg {
    min-height: 40px;
    padding: var(--space-2) var(--space-5);
    font-size: var(--text-base);
  }

  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .btn--primary {
    background-color: var(--color-brand);
    color: var(--color-on-brand);
  }

  .btn--primary:hover:not(:disabled) {
    background-color: var(--color-brand-hover);
  }

  .btn--secondary {
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    border-color: var(--color-border-strong);
  }

  .btn--secondary:hover:not(:disabled) {
    background-color: var(--color-bg-subtle);
  }

  .btn--danger {
    background-color: var(--color-danger);
    color: var(--color-on-brand);
  }

  .btn--danger:hover:not(:disabled) {
    filter: brightness(0.9);
  }

  .btn--ghost {
    background-color: transparent;
    color: var(--color-text-secondary);
  }

  .btn--ghost:hover:not(:disabled) {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
  }

  .btn__spinner {
    width: 1em;
    height: 1em;
    border: 2px solid currentColor;
    border-right-color: transparent;
    border-radius: 50%;
    animation: spin 0.75s linear infinite;
  }

  /* A phone gets the 44 px target, whatever size the view asked for. */
  @media (max-width: 767px) {
    .btn {
      min-height: 44px;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .btn {
      transition: none;
    }

    .btn__spinner {
      animation: none;
    }
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
</style>
