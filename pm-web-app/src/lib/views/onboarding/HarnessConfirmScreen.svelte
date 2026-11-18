<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * The confirmation of the enrolled harnesses (`harness.confirm`, `harness.add-confirm`): what is
   * enrolled, which one is the default, and the links that enrol or go back.
   */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.harness.confirm.title', { count: 0 })}
    </h1>
  </div>
  {#if view.field('default')}
    <p class="state-card__summary">
      {$t('onboard.harness.choose.default_label')}:
      <span class="mono">{view.field('default')}</span>
    </p>
  {/if}
  <div class="state-card__actions">
    {#each view.links as { id, isPrimary }}
      <Button
        variant={isPrimary ? 'primary' : 'secondary'}
        loading={view.busy}
        on:click={() => view.onLink(id)}
      >
        {view.linkLabel(id)}
      </Button>
    {/each}
  </div>
</div>
