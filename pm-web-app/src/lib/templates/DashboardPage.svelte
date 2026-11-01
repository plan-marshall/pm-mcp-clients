<script lang="ts">
  /**
   * The dashboard page (UX-P-03, template D): the title, the time window switch, the grid of cards
   * with figures and the "updated" line.
   *
   * The page is capped at 1440 px and centred. The template owns the head, the switch region, the
   * card grid and the updated line; the view fills them, so no dashboard view sets its own page
   * width.
   */
</script>

<div class="dashboard-page" data-testid="dashboard-page">
  {#if $$slots.title || $$slots.controls}
    <header class="dashboard-page__head">
      <div class="dashboard-page__title">
        <slot name="title" />
      </div>
      {#if $$slots.controls}
        <div class="dashboard-page__controls" data-testid="dashboard-page-controls">
          <slot name="controls" />
        </div>
      {/if}
    </header>
  {/if}

  <div class="dashboard-page__grid" data-testid="dashboard-page-grid">
    <slot />
  </div>

  {#if $$slots.updated}
    <div class="dashboard-page__updated" data-testid="dashboard-page-updated">
      <slot name="updated" />
    </div>
  {/if}
</div>

<style>
  .dashboard-page {
    display: flex;
    flex-direction: column;
    gap: var(--space-5);
    width: 100%;
    max-width: 1440px;
    margin-inline: auto;
    min-inline-size: 0;
    padding: var(--space-6);
  }

  .dashboard-page__head {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-4);
    min-inline-size: 0;
  }

  .dashboard-page__title {
    min-inline-size: 0;
  }

  .dashboard-page__controls {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    min-inline-size: 0;
  }

  .dashboard-page__grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(320px, 100%), 1fr));
    gap: var(--space-5);
    min-inline-size: 0;
  }

  .dashboard-page__updated {
    margin: 0;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  /* The phone takes the app's narrow inset, as every view did before the template owned it. */
  @media (max-width: 480px) {
    .dashboard-page {
      padding: var(--space-3);
    }
  }
</style>
