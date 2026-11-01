<script lang="ts">
  /**
   * The wizard page (UX-P-03, template C): the compact step bar, the one card and the footer with
   * Back, the secondary act and the primary act.
   *
   * The page is 720 px wide and centred, the measure a form wants. The template owns the bar, the
   * card surface and the footer; the view fills the steps, the card's content and the acts.
   */
</script>

<div class="wizard-page" data-testid="wizard-page">
  {#if $$slots.steps}
    <nav class="wizard-page__steps" data-testid="wizard-page-steps">
      <slot name="steps" />
    </nav>
  {/if}

  <div class="wizard-page__card" data-testid="wizard-page-card">
    <slot />
  </div>

  <footer class="wizard-page__footer" data-testid="wizard-page-footer">
    <div class="wizard-page__back">
      <slot name="back" />
    </div>
    <div class="wizard-page__acts">
      <slot name="secondary" />
      <slot name="primary" />
    </div>
  </footer>
</div>

<style>
  .wizard-page {
    display: flex;
    flex-direction: column;
    gap: var(--space-5);
    width: 100%;
    max-width: 720px;
    margin-inline: auto;
    min-inline-size: 0;
    padding: var(--space-6);
  }

  .wizard-page__steps {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .wizard-page__card {
    padding: var(--space-5);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-surface);
    box-shadow: var(--shadow-1);
  }

  .wizard-page__footer {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
  }

  .wizard-page__acts {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }
</style>
