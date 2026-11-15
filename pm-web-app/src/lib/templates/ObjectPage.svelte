<script lang="ts">
  /**
   * The object page (UX-P-03, template B): the head of an object — kind, title, id chip, state
   * sentence and the primary, secondary and overflow acts — an attention strip when the state asks
   * for something, the tabs and the body.
   *
   * On a page of its own the page is capped at 1440 px and centred. Inside the column engine it is
   * the object column, which carries no cap; `compact` draws the head of a list column instead:
   * `KindMark`, title, state word and the main act, and nothing below it. The object column's head
   * carries no path — the top bar's breadcrumb is the only one (`navigation.adoc#up`) — and the
   * template has no side column of its own: inside the engine the side column is the engine's (D6).
   *
   * The template owns the regions; the view fills them with its own content, so no object view sets
   * its own page width. The attention slot takes the `AttentionStrip` component and the tabs slot
   * the `Tabs` component: the template puts no second surface, border or rule around them.
   */
  /** Whether the head is the head of a list column, with nothing below it. */
  export let compact = false;
  /** Whether the page stands inside the engine's object column: no cap and no page inset. */
  export let inColumn = false;
</script>

<div
  class="object-page"
  class:object-page--compact={compact}
  class:object-page--in-column={inColumn}
  data-testid="object-page"
>
  {#if compact}
    <header class="object-page__head object-page__head--compact">
      <div class="object-page__identity">
        <div class="object-page__eyebrow">
          <span class="object-page__kind" data-testid="object-page-kind"><slot name="kind" /></span>
        </div>
        <h2 class="object-page__title" data-testid="object-page-title"><slot name="title" /></h2>
        <span class="object-page__state-word" data-testid="object-page-state"
          ><slot name="state" /></span
        >
      </div>

      <div class="object-page__acts">
        <slot name="primary" />
      </div>
    </header>
  {:else}
    <header class="object-page__head">
      <div class="object-page__identity">
        <div class="object-page__eyebrow">
          <span class="object-page__kind" data-testid="object-page-kind"><slot name="kind" /></span>
        </div>
        <h1 class="object-page__title" data-testid="object-page-title"><slot name="title" /></h1>
        <div class="object-page__state">
          <span class="object-page__id" data-testid="object-page-id"><slot name="id" /></span>
          <span class="object-page__state-sentence" data-testid="object-page-state"
            ><slot name="state" /></span
          >
        </div>
      </div>

      <div class="object-page__acts">
        <slot name="primary" />
        <slot name="secondary" />
        <slot name="overflow" />
      </div>
    </header>

    {#if $$slots.attention}
      <div class="object-page__attention" data-testid="object-page-attention">
        <slot name="attention" />
      </div>
    {/if}

    {#if $$slots.tabs}
      <div class="object-page__tabs" data-testid="object-page-tabs">
        <slot name="tabs" />
      </div>
    {/if}

    <div class="object-page__body" data-testid="object-page-body">
      <slot />
    </div>
  {/if}
</div>

<style>
  .object-page {
    display: flex;
    flex-direction: column;
    gap: var(--space-5);
    width: 100%;
    max-width: 1440px;
    margin-inline: auto;
    min-inline-size: 0;
    padding: var(--space-6);
  }

  /* Inside a column the object takes the column's width: no cap, no page inset of its own. */
  .object-page--compact {
    max-width: none;
    margin-inline: 0;
    padding: 0;
    gap: var(--space-3);
  }

  /* The full head in the object column: the same, with the page's own spacing inside the column. */
  .object-page--in-column {
    max-width: none;
    margin-inline: 0;
    padding: 0;
  }

  .object-page__head {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-4);
    min-inline-size: 0;
  }

  .object-page__head--compact {
    align-items: center;
    gap: var(--space-3);
  }

  .object-page__identity {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    min-inline-size: 0;
  }

  .object-page__eyebrow {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: var(--space-3);
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .object-page__title {
    margin: 0;
    overflow-wrap: anywhere;
    color: var(--color-text-primary);
    font-size: var(--text-2xl);
    font-weight: var(--weight-semibold);
    line-height: var(--line-height-2xl);
  }

  .object-page--compact .object-page__title {
    font-size: var(--text-lg);
    line-height: var(--line-height-lg);
  }

  .object-page__state {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
  }

  .object-page__id {
    padding: var(--space-1) var(--space-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-full);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    font-family: var(--font-mono);
    font-size: var(--text-xs);
  }

  .object-page__state-sentence {
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .object-page__state-word {
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .object-page__acts {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .object-page__body {
    min-inline-size: 0;
  }

  /* The phone takes the app's narrow inset, as every view did before the template owned it. */
  @media (max-width: 480px) {
    .object-page {
      padding: var(--space-3);
    }
  }
</style>
