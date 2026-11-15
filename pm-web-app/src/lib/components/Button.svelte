<script context="module" lang="ts">
  export const BUTTON_VARIANTS = ['primary', 'secondary', 'danger', 'ghost'] as const;
  export type ButtonVariant = (typeof BUTTON_VARIANTS)[number];

  export const BUTTON_ICON_SIZES = { sm: 16, md: 20, lg: 20 } as const;
  export type ButtonSize = keyof typeof BUTTON_ICON_SIZES;
  export const BUTTON_SIZES = ['sm', 'md', 'lg'] as const satisfies readonly ButtonSize[];
</script>

<script lang="ts">
  import { advisory } from '../utils/advisoryTitle';
  import Icon from './Icon.svelte';
  import { uniqueId } from '../utils/uniqueId';

  /**
   * A control of the design system (VD-P-05): its four variants, its three sizes, an optional icon,
   * and a disabled state that says why.
   *
   * An act that cannot run because of a reason (`disabledReason`) stays in the tab order
   * (`aria-disabled="true"` instead of native `disabled`; title and `aria-describedby` carry the
   * reason; click does nothing). An act with no reason keeps native `disabled`.
   * A loading button keeps its label text alongside its spinner and ignores clicks.
   * Inside `<form>`, disabled and loading buttons prevent submission.
   */
  export let variant: ButtonVariant = 'secondary';
  export let type: 'button' | 'submit' | 'reset' = 'button';
  /** The size: sm, md, or lg. */
  export let size: ButtonSize = 'md';
  /** The icon left of the label, from the delivered UI icon set. */
  export let icon: string | null | undefined = undefined;
  export let disabled = false;
  /** Why the control cannot act; the reason becomes its advisory and its description. */
  export let disabledReason: string | null | undefined = undefined;
  export let loading = false;
  export let buttonElement: HTMLButtonElement | null = null;

  /** The id of the element that carries the reason, for `aria-describedby`. */
  const reasonId = uniqueId('btn-reason');

  $: hasReason = disabled && !!disabledReason;
  $: isAriaDisabled = hasReason;
  $: isNativeDisabled = disabled && !disabledReason;
  $: reason = hasReason ? (disabledReason as string) : '';
  $: describedBy =
    [reason ? reasonId : null, $$restProps['aria-describedby']].filter(Boolean).join(' ') ||
    undefined;
  $: iconSize = BUTTON_ICON_SIZES[size];

  function handleClick(event: MouseEvent): void {
    if (disabled || loading) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }
  }
</script>

<button
  bind:this={buttonElement}
  {type}
  class="btn btn--{variant} btn--{size}"
  class:btn--loading={loading}
  class:btn--disabled={disabled}
  disabled={isNativeDisabled || loading ? true : undefined}
  aria-disabled={isAriaDisabled ? 'true' : undefined}
  aria-busy={loading ? 'true' : undefined}
  {...$$restProps}
  title={reason || $$restProps['title']}
  aria-describedby={describedBy}
  use:advisory
  on:click={handleClick}
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
    background-color: var(--color-bg-subtle);
    color: var(--color-text-muted);
    border-color: var(--color-border);
    cursor: not-allowed;
  }

  .btn[aria-disabled="true"] {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-muted);
    border-color: var(--color-border);
    cursor: not-allowed;
  }

  .btn:focus {
    outline: none;
    box-shadow: var(--focus-ring);
  }

  .btn--primary {
    background-color: var(--color-brand);
    color: var(--color-on-brand);
  }

  .btn--primary:hover:not(:disabled):not([aria-disabled="true"]) {
    background-color: var(--color-brand-hover);
  }

  .btn--secondary {
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    border-color: var(--color-border-strong);
  }

  .btn--secondary:hover:not(:disabled):not([aria-disabled="true"]) {
    background-color: var(--color-bg-subtle);
  }

  .btn--danger {
    background-color: var(--color-danger);
    color: var(--color-on-brand);
  }

  .btn--danger:hover:not(:disabled):not([aria-disabled="true"]) {
    filter: brightness(0.9);
  }

  .btn--ghost {
    background-color: transparent;
    color: var(--color-text-secondary);
  }

  .btn--ghost:hover:not(:disabled):not([aria-disabled="true"]) {
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
