<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * A provider whose check failed (`providers.failed`): the provider, the cause and the fix, and
   * the acts the state offers (UC-F-09).
   */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.providers.failed.title')}
    </h1>
  </div>
  <dl class="detail-list">
    {#if view.field('provider')}
      <div class="detail-row">
        <dt>{$t('onboard.providers.field')}</dt>
        <dd>{view.field('provider')}</dd>
      </div>
    {/if}
    {#if view.field('cause')}
      <div class="detail-row">
        <dt>{$t('onboard.state.cause')}</dt>
        <dd>{view.field('cause')}</dd>
      </div>
    {/if}
    {#if view.field('fix')}
      <div class="detail-row">
        <dt>{$t('onboard.state.fix')}</dt>
        <dd class="mono">{view.field('fix')}</dd>
      </div>
    {/if}
  </dl>
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
