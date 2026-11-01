<script lang="ts">
  /**
   * The object page (UX-P-03, template B): the head of an object — kind, path, title, id chip,
   * state sentence and the primary, secondary and overflow acts — an attention strip when the
   * state asks for something, the tabs and the body.
   *
   * The page is capped at 1440 px and centred. The template owns the regions; the view fills them
   * with its own content, so no object view sets its own page width. The attention slot takes the
   * `AttentionStrip` component and the tabs slot the `Tabs` component: the template puts no second
   * surface, border or rule around them.
   */
</script>

<div class="object-page" data-testid="object-page">
  <header class="object-page__head">
    <div class="object-page__identity">
      <div class="object-page__eyebrow">
        <span class="object-page__kind" data-testid="object-page-kind"><slot name="kind" /></span>
        <span class="object-page__path" data-testid="object-page-path"><slot name="path" /></span>
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

  .object-page__head {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-4);
    min-inline-size: 0;
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
