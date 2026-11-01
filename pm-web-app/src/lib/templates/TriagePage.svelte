<script lang="ts">
  /**
   * The triage page (UX-P-03, template A): the list on the left, the detail on the right, the
   * filter chips above both.
   *
   * The page takes the full width and splits it 40 / 60 between list and detail. Below 1100 px the
   * detail replaces the list — the view marks `selected` when it shows one, so the narrow layout
   * never draws both panes at once. The template owns the panes, never the items: the view that
   * fills the list slot keeps its rows, its selection and its `j`/`k`/`Enter` handling.
   */
  export let selected = false;
</script>

<div class="triage-page" class:triage-page--detail={selected} data-testid="triage-page">
  {#if $$slots.filters}
    <div class="triage-page__filters" data-testid="triage-filters">
      <slot name="filters" />
    </div>
  {/if}

  <div class="triage-page__list" data-testid="triage-list">
    <slot name="list" />
  </div>

  <div class="triage-page__detail" data-testid="triage-detail">
    <slot name="detail" />
  </div>
</div>

<style>
  .triage-page {
    display: grid;
    /* 40 / 60: the list keeps a readable row measure, the detail takes the rest. */
    grid-template-columns: minmax(0, 2fr) minmax(0, 3fr);
    gap: var(--space-5);
    width: 100%;
    min-inline-size: 0;
    padding: var(--space-6);
  }

  .triage-page__filters {
    display: flex;
    flex-wrap: wrap;
    grid-column: 1 / -1;
    gap: var(--space-2);
    align-items: center;
  }

  .triage-page__list,
  .triage-page__detail {
    min-inline-size: 0;
  }

  /* Below 1100 px the detail replaces the list: one pane, never two. */
  @media (max-width: 1099px) {
    .triage-page {
      grid-template-columns: minmax(0, 1fr);
    }

    .triage-page:not(.triage-page--detail) .triage-page__detail {
      display: none;
    }

    .triage-page--detail .triage-page__list,
    .triage-page--detail .triage-page__filters {
      display: none;
    }
  }
</style>
