<script lang="ts">
  import { advisory as applyAdvisory } from '../utils/advisoryTitle';
  import Icon from './Icon.svelte';
  import { uniqueId } from '../utils/uniqueId';

  /**
   * The square, ghost control of the design system (VD-P-05): one icon, no visible label.
   *
   * It stands for row menus (`act.more`), "jump to now", and copy. Because the glyph alone does not
   * name the act, the control is always named by `label` and carries the advisory its view passes
   * from the catalogue's `X.title` key (Interactions Affordances).
   */
  export let icon: string;
  /** The accessible name; an icon-only control has no visible text to name it. */
  export let label: string;
  /** The hover advisory, from the view's `X.title` catalogue key. */
  export let advisory: string | undefined = undefined;
  export let size: 'sm' | 'md' | 'lg' = 'md';
  export let type: 'button' | 'submit' | 'reset' = 'button';
  export let disabled = false;
  /** Why the control cannot act; the reason becomes its advisory and its description. */
  export let disabledReason: string | null | undefined = undefined;

  /** The id of the element that carries the reason, for `aria-describedby`. */
  const reasonId = uniqueId('icon-btn-reason');

  /** The delivered icon size of each control size. */
  const ICON_SIZES = { sm: 16, md: 20, lg: 20 } as const;

  $: reason = disabled && disabledReason ? disabledReason : '';
  $: tooltip = reason || advisory || $$restProps['title'];
  $: describedBy =
    [reason ? reasonId : null, $$restProps['aria-describedby']].filter(Boolean).join(' ') ||
    undefined;
  $: iconSize = ICON_SIZES[size];
</script>

<button
  {type}
  class="icon-btn icon-btn--ghost icon-btn--{size}"
  aria-label={label}
  disabled={disabled}
  {...$$restProps}
  title={tooltip}
  aria-describedby={describedBy}
  use:applyAdvisory
  on:click
>
  <Icon name={icon} size={iconSize} />
</button>

{#if reason}
  <span class="sr-only" id={reasonId}>{reason}</span>
{/if}

<style>
  .icon-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    border: 1px solid transparent;
    border-radius: var(--radius-md);
    background-color: transparent;
    color: var(--color-text-secondary);
    transition: background-color var(--duration-fast) var(--ease-standard);
    cursor: pointer;
  }

  .icon-btn--sm {
    inline-size: 28px;
    block-size: 28px;
  }

  .icon-btn--md {
    inline-size: 34px;
    block-size: 34px;
  }

  .icon-btn--lg {
    inline-size: 40px;
    block-size: 40px;
  }

  .icon-btn:hover:not(:disabled) {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
  }

  .icon-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  /* A phone gets the 44 px target, whatever size the view asked for. */
  @media (max-width: 767px) {
    .icon-btn {
      inline-size: 44px;
      block-size: 44px;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .icon-btn {
      transition: none;
    }
  }
</style>
