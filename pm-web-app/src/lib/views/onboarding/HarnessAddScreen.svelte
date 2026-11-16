<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * Adding a harness the machine does not know (`harness.add`, `harness.unknown`): the executable
   * the runtime found, the name it proposes, and the links that add it or use its shipped settings.
   */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.harness.add.title')}
    </h1>
  </div>
  <dl class="detail-list">
    {#if view.field('name')}
      <div class="detail-row">
        <dt>{$t('onboard.harness.add.name')}</dt>
        <dd>{view.field('name')}</dd>
      </div>
    {/if}
    {#if view.field('executable')}
      <div class="detail-row">
        <dt>{$t('onboard.harness.add.executable')}</dt>
        <dd class="mono">{view.field('executable')}</dd>
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
