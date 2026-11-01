<script lang="ts">
  import Button from './Button.svelte';
  import Icon from './Icon.svelte';

  /**
   * The block a view draws when what it wanted to show could not be read (VD-P-05, VD-S-04): an
   * icon, a title, one sentence and one act, with the technical code folded under `Details`.
   *
   * The sentence says what the person can do next; the code is there for a report and never leads,
   * so it stays folded away until someone opens it.
   */
  export let title: string;
  /** The one sentence under the title, from the view's catalogue key. */
  export let message: string;
  /** The technical code, shown folded under `Details`; a view with none draws no `Details`. */
  export let code: string | null | undefined = undefined;
  /** The word that opens the folded code, from the view's catalogue key. */
  export let detailsLabel: string | undefined = undefined;
  /** The one act, from the view's catalogue key; omitted when there is none. */
  export let actionLabel: string | undefined = undefined;
  /** Reports the act the person took. */
  export let onAct: () => void = () => {};
  /** The heading level of the title, so a view keeps its outline in order. */
  export let titleLevel: 2 | 3 | 4 | 5 | 6 = 3;
</script>

<div class="error-state" data-testid="error-state">
  <span class="error-state__mark"><Icon name="state.error" size={24} /></span>
  <svelte:element this={`h${titleLevel}`} class="error-state__title">{title}</svelte:element>
  <p class="error-state__message">{message}</p>
  {#if actionLabel}
    <Button variant="secondary" on:click={onAct}>{actionLabel}</Button>
  {/if}
  {#if code && detailsLabel}
    <details class="error-state__details">
      <summary class="error-state__summary">{detailsLabel}</summary>
      <code class="error-state__code">{code}</code>
    </details>
  {/if}
</div>

<style>
  .error-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-10) var(--space-6);
    text-align: center;
  }

  /* The failed state takes the danger role the design system gives it, and its word says so too. */
  .error-state__mark {
    display: inline-flex;
    color: var(--color-danger);
  }

  .error-state__title {
    margin: 0;
    color: var(--color-text-primary);
    font-size: var(--text-md);
    font-weight: var(--weight-semibold);
    line-height: var(--line-height-md);
  }

  .error-state__message {
    max-inline-size: 52ch;
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }

  .error-state__details {
    max-inline-size: 100%;
    margin-block-start: var(--space-2);
    color: var(--color-text-muted);
    font-size: var(--text-xs);
    line-height: var(--line-height-xs);
  }

  .error-state__summary {
    cursor: pointer;
  }

  .error-state__code {
    display: block;
    margin-block-start: var(--space-2);
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    font-family: var(--font-mono);
    overflow-wrap: anywhere;
  }
</style>
