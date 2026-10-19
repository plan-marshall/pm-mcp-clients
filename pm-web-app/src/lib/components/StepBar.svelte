<script lang="ts">
  import { t } from '../i18n';

  /**
   * The step bar of the onboarding.
   *
   * It shows where the run is and lets a done step be opened read-only: the bar never navigates
   * ahead, and a done step is shown beside the current one, never re-run from here.
   */
  export let activeStep = 1; // 1 to 7, or 8 for done
  export let completedSteps: number[] = [];
  export let selectedStep: number | null = null;
  export let summaries: Record<string, string> = {};

  export let onSelectStep: (step: number) => void = () => {};

  const steps = [
    { num: 1, key: 'onboard.step.language' },
    { num: 2, key: 'onboard.step.harnesses' },
    { num: 3, key: 'onboard.step.models' },
    { num: 4, key: 'onboard.step.roles' },
    { num: 5, key: 'onboard.step.credentials' },
    { num: 6, key: 'onboard.step.providers' },
    { num: 7, key: 'onboard.step.plans' },
  ];

  function isDone(step: number): boolean {
    return completedSteps.includes(step) || activeStep > step;
  }

  function select(step: number): void {
    if (!isDone(step)) return;
    onSelectStep(selectedStep === step ? 0 : step);
  }
</script>

<nav class="step-bar" aria-label={$t('onboard.frame.title')}>
  <ol class="step-bar__list">
    {#each steps as step}
      {@const done = isDone(step.num)}
      <li
        class="step-bar__item"
        class:step-bar__item--active={activeStep === step.num}
        class:step-bar__item--done={done}
        class:step-bar__item--selected={selectedStep === step.num}
      >
        {#if done}
          <button
            type="button"
            class="step-bar__button"
            aria-pressed={selectedStep === step.num}
            aria-label={$t('onboard.completed.show_step', { step: step.num })}
            onclick={() => select(step.num)}
            data-testid={`step-button-${step.num}`}
          >
            <span class="step-bar__indicator" aria-hidden="true">✓</span>
            <span class="step-bar__label">{$t(step.key)}</span>
          </button>
        {:else}
          <span class="step-bar__static">
            <span class="step-bar__indicator" aria-hidden="true">{step.num}</span>
            <span class="step-bar__label">{$t(step.key)}</span>
          </span>
        {/if}
      </li>
    {/each}
    <li class="step-bar__item" class:step-bar__item--done={activeStep > 7}>
      <span class="step-bar__static">
        <span class="step-bar__indicator" aria-hidden="true">
          {#if activeStep > 7}✓{:else}○{/if}
        </span>
        <span class="step-bar__label">{$t('onboard.step.done')}</span>
      </span>
    </li>
  </ol>

  {#if selectedStep && summaries[String(selectedStep)]}
    <div class="step-summary" data-testid="step-summary">
      <div class="step-summary__head">
        <h2 class="step-summary__title">
          {$t(steps[selectedStep - 1]?.key ?? 'onboard.step.done')}
        </h2>
        <button
          type="button"
          class="step-summary__close"
          aria-label={$t('onboard.completed.hide_step')}
          onclick={() => onSelectStep(0)}
        >
          ✕
        </button>
      </div>
      <p class="step-summary__value">{summaries[String(selectedStep)]}</p>
      <p class="step-summary__note">{$t('onboard.completed.readonly')}</p>
    </div>
  {/if}
</nav>

<style>
  .step-bar {
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-surface);
    border-bottom: 1px solid var(--color-border);
    overflow-x: auto;
  }

  .step-bar__list {
    display: flex;
    align-items: center;
    list-style: none;
    gap: var(--space-2);
    min-width: max-content;
  }

  .step-bar__item {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }

  .step-bar__button,
  .step-bar__static {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    color: inherit;
    font-size: inherit;
    background: none;
    border: none;
    padding: 0;
  }

  .step-bar__button {
    cursor: pointer;
    border-radius: var(--radius-sm);
  }

  .step-bar__button:hover {
    color: var(--color-text-primary);
  }

  .step-bar__item--active {
    color: var(--color-primary);
    font-weight: 600;
  }

  .step-bar__item--done {
    color: var(--color-success);
  }

  .step-bar__item--selected {
    text-decoration: underline;
    text-underline-offset: 3px;
  }

  .step-bar__indicator {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 1.5rem;
    height: 1.5rem;
    border-radius: 50%;
    border: 1px solid currentColor;
    font-size: var(--text-xs);
  }

  .step-bar__item--active .step-bar__indicator {
    background-color: var(--color-primary);
    color: var(--color-primary-text);
    border-color: var(--color-primary);
  }

  .step-summary {
    margin-top: var(--space-3);
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-subtle);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    max-width: 100%;
  }

  .step-summary__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
  }

  .step-summary__title {
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .step-summary__close {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
    padding: 0 var(--space-1);
  }

  .step-summary__value {
    margin-top: var(--space-1);
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .step-summary__note {
    margin-top: var(--space-1);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  @media (max-width: 768px) {
    .step-bar {
      padding: var(--space-2) var(--space-3);
    }

    .step-bar__list {
      gap: var(--space-1);
    }

    .step-bar__label {
      font-size: var(--text-xs);
    }
  }
</style>
