<script lang="ts">
  import { t } from '../i18n';

  export let activeStep = 1; // 1 to 7, or 8 for done
  export let completedSteps: number[] = [];

  const steps = [
    { num: 1, key: 'onboard.step.language' },
    { num: 2, key: 'onboard.step.harnesses' },
    { num: 3, key: 'onboard.step.models' },
    { num: 4, key: 'onboard.step.roles' },
    { num: 5, key: 'onboard.step.credentials' },
    { num: 6, key: 'onboard.step.providers' },
    { num: 7, key: 'onboard.step.plans' },
  ];
</script>

<nav class="step-bar" aria-label="Onboarding Steps">
  <ol class="step-bar__list">
    {#each steps as step, i}
      {@const isDone = completedSteps.includes(step.num) || activeStep > step.num}
      {@const isActive = activeStep === step.num}
      <li
        class="step-bar__item"
        class:step-bar__item--active={isActive}
        class:step-bar__item--done={isDone}
      >
        <span class="step-bar__indicator" aria-hidden="true">
          {#if isDone}
            ✓
          {:else}
            {step.num}
          {/if}
        </span>
        <span class="step-bar__label">{$t(step.key)}</span>
        {#if i < steps.length - 1}
          <span class="step-bar__separator" aria-hidden="true">─</span>
        {/if}
      </li>
    {/each}
    <li class="step-bar__item" class:step-bar__item--done={activeStep > 7}>
      <span class="step-bar__separator" aria-hidden="true">─</span>
      <span class="step-bar__indicator" aria-hidden="true">
        {#if activeStep > 7}✓{:else}○{/if}
      </span>
      <span class="step-bar__label">{$t('onboard.step.done')}</span>
    </li>
  </ol>
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

  .step-bar__item--active {
    color: var(--color-primary);
    font-weight: 600;
  }

  .step-bar__item--done {
    color: var(--color-success);
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

  .step-bar__separator {
    color: var(--color-border);
    user-select: none;
  }
</style>
