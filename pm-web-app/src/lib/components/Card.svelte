<script lang="ts">
  import { onMount } from 'svelte';

  /**
   * The surface of a block (VD-P-05, VD-S-03): `--color-bg-surface`, radius 12 px, padding 20 px
   * and an optional head that carries the title and the acts of that head.
   *
   * A card inside a card is refused: the design system puts a divider between nested parts rather
   * than a second surface. Slot content belongs to the scope of the view that writes it, so the
   * card cannot know beforehand whether it will be drawn inside another one; it looks at the
   * element around it once it is in the document, draws a divider instead of a surface, and the
   * development build names the nesting in the console of the page that caused it.
   */
  export let title: string | undefined = undefined;
  /**
   * The heading level of the title, so a view keeps its outline in order: a card under a section
   * heading takes the level below it.
   */
  export let titleLevel: 2 | 3 | 4 | 5 | 6 = 2;

  /** The level a nested part's title takes, one below the card that holds it. */
  $: nestedLevel = Math.min(titleLevel + 1, 6) as 2 | 3 | 4 | 5 | 6;

  /** Whether this card sits inside the body of another card. */
  let nested = false;

  /** The card's own element, for the nesting check. */
  let host: HTMLElement | undefined = undefined;

  onMount(() => {
    // The surface carries its own hook, so a `.card` of a view's stylesheet is never mistaken for
    // the component around this one.
    const outer = host?.parentElement?.closest('[data-card]') ?? null;
    nested = outer !== null;
    if (nested && import.meta.env.DEV) {
      console.warn(
        '[Card] a card inside a card is refused; nested parts use a divider instead of a surface',
      );
    }
  });
</script>

{#if nested}
  <div class="card__nested" data-testid="card-nested">
    <hr class="card__divider" />
    {#if title || $$slots.actions}
      <div class="card__nested-head">
        {#if title}
          <svelte:element this={`h${nestedLevel}`} class="card__title card__title--nested">
            {title}
          </svelte:element>
        {/if}
        {#if $$slots.actions}
          <div class="card__actions">
            <slot name="actions" />
          </div>
        {/if}
      </div>
    {/if}
    <slot />
  </div>
{:else}
  <section class="card" data-card data-testid="card" bind:this={host}>
    {#if title || $$slots.actions}
      <header class="card__head">
        {#if title}
          <svelte:element this={`h${titleLevel}`} class="card__title">{title}</svelte:element>
        {/if}
        {#if $$slots.actions}
          <div class="card__actions">
            <slot name="actions" />
          </div>
        {/if}
      </header>
    {/if}
    <div class="card__body">
      <slot />
    </div>
  </section>
{/if}

<style>
  .card {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    padding: var(--space-5);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-surface);
    box-shadow: var(--shadow-1);
  }

  .card__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-3);
  }

  .card__title {
    margin: 0;
    font-size: var(--text-md);
    font-weight: var(--weight-semibold);
    line-height: var(--line-height-md);
  }

  .card__title--nested {
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
    color: var(--color-text-secondary);
  }

  .card__nested-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
  }

  .card__actions {
    display: flex;
    flex: 0 0 auto;
    align-items: center;
    gap: var(--space-2);
  }

  .card__body {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  /* A nested card is a part of the card around it: the divider carries the separation, so the part
     keeps no surface, no radius and no padding of its own. */
  .card__nested {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .card__divider {
    margin: 0;
    border: 0;
    border-block-start: 1px solid var(--color-border);
  }
</style>
