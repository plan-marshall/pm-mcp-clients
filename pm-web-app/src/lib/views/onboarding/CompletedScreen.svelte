<script lang="ts">
  import Button from '../../components/Button.svelte';
  import Icon from '../../components/Icon.svelte';
  import { t } from '../../i18n';
  import { navigateTo } from '../../stores/runtime';
  import type { ScreenProps } from './screens';

  /** The completed card (`onboard.completed`): every done step with its recorded summary. */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <span class="badge-success" aria-hidden="true"><Icon name="state.check" size={16} /></span>
    <h1 class="state-card__title">{$t('onboard.completed.title')}</h1>
    <p class="state-card__summary" data-testid="completed-summary">
      {view.completedSummary()}
    </p>
  </div>

  {#if view.doneSteps.length > 0}
    <ul class="completed-list" data-testid="completed-steps">
      {#each view.doneSteps as step}
        <li class="completed-row">
          <span class="completed-row__check" aria-hidden="true"
            ><Icon name="state.check" size={16} /></span
          >
          <span class="completed-row__name">{$t(`onboard.step.${view.stepKey(step)}`)}</span>
          <span class="completed-row__value">
            {view.summaries[String(step)] ?? $t('onboard.completed.no_summary')}
          </span>
        </li>
      {/each}
    </ul>
  {/if}

  <p class="state-card__next">{$t('onboard.completed.next')}</p>

  <div class="state-card__actions state-card__actions--center">
    <Button variant="secondary" on:click={() => navigateTo('settings')}>
      {$t('onboard.completed.machine_settings')}
    </Button>
    <Button variant="primary" on:click={() => navigateTo('projects')}>
      {$t('onboard.completed.to_projects')}
    </Button>
  </div>
</div>
