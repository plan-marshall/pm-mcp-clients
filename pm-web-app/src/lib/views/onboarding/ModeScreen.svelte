<script lang="ts">
  import { t } from '../../i18n';
  import Button from '../../components/Button.svelte';
  import type { ScreenProps } from './screens';

  /**
   * The mode screen (`onboard.mode`, `onboard.entry`, `onboard.summary`): the seven steps the
   * machine goes through, and the two ways to begin.
   */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">{$t('onboard.entry.title')}</h1>
    <p class="state-card__summary">
      {$t('onboard.entry.intro', { total: view.machineSteps.length })}
    </p>
  </div>

  <ol class="steps-overview" data-testid="steps-overview">
    {#each view.machineSteps as step}
      <li>
        <strong>{step}. {$t(`onboard.step.${view.stepKey(step)}`)}</strong>
        ─ {$t(`onboard.entry.step.${view.stepKey(step)}`)}
      </li>
    {/each}
  </ol>

  <div class="state-card__actions">
    {#if view.links.length > 0}
      {#each view.links as { id, isPrimary }}
        <Button
          variant={isPrimary ? 'primary' : 'secondary'}
          loading={view.busy}
          on:click={() => view.onLink(id)}
        >
          {#if id === 'auto'}
            {$t('onboard.mode.auto')}
          {:else if id === 'begin'}
            {$t('onboard.mode.guided')}
          {:else}
            {view.linkLabel(id)}
          {/if}
        </Button>
      {/each}
    {:else}
      <Button variant="secondary" loading={view.busy} on:click={view.onAuto}>
        {$t('onboard.mode.auto')}
      </Button>
      <Button variant="primary" loading={view.busy} on:click={() => view.onLink('begin')}>
        {$t('onboard.mode.guided')}
      </Button>
    {/if}
  </div>
</div>
