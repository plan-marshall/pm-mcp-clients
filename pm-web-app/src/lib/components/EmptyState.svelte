<script lang="ts">
  import BrandMark from './BrandMark.svelte';
  import Button from './Button.svelte';
  import Icon from './Icon.svelte';

  /**
   * The block a list draws instead of an empty list (VD-P-05, VD-P-08, VD-S-04): an icon or the
   * brand mark in `--color-text-muted`, a title, one sentence and one act.
   *
   * The sentence says what is missing and what the person can do about it; the act is that step.
   * An empty state is not an error, so it carries neither a danger colour nor a retry of its own.
   */
  export let title: string;
  /** The one sentence under the title, from the view's catalogue key. */
  export let message: string;
  /** The icon above the title, from the delivered UI icon set. */
  export let icon: string | null | undefined = undefined;
  /** Draws the brand mark instead of an icon; an empty state of the design system uses the mark. */
  export let brand = false;
  /** The one act, from the view's catalogue key; omitted when there is none. */
  export let actionLabel: string | undefined = undefined;
  /** Reports the act the person took. */
  export let onAct: () => void = () => {};
  /** The heading level of the title, so a view keeps its outline in order. */
  export let titleLevel: 2 | 3 | 4 | 5 | 6 = 3;
</script>

<div class="empty-state" data-testid="empty-state">
  {#if brand}
    <span class="empty-state__mark"><BrandMark size={64} /></span>
  {:else if icon}
    <span class="empty-state__mark"><Icon name={icon} size={24} /></span>
  {/if}
  <svelte:element this={`h${titleLevel}`} class="empty-state__title">{title}</svelte:element>
  <p class="empty-state__message">{message}</p>
  {#if actionLabel}
    <Button variant="primary" on:click={onAct}>{actionLabel}</Button>
  {/if}
</div>

<style>
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-10) var(--space-6);
    text-align: center;
  }

  /* The mark is a watermark, not a picture: the muted role keeps it behind the words. */
  .empty-state__mark {
    display: inline-flex;
    color: var(--color-text-muted);
  }

  .empty-state__title {
    margin: 0;
    color: var(--color-text-primary);
    font-size: var(--text-md);
    font-weight: var(--weight-semibold);
    line-height: var(--line-height-md);
  }

  .empty-state__message {
    max-inline-size: 52ch;
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }
</style>
