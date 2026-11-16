<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** The automatic run's result, per aspect, with the reason the runtime names. */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.auto.result.title')}
    </h1>
  </div>
  <table class="result-table" data-testid="auto-result">
    <thead>
      <tr>
        <th>{$t('onboard.auto.result.aspect')}</th>
        <th>{$t('onboard.auto.result.value')}</th>
        <th>{$t('onboard.auto.result.reason')}</th>
      </tr>
    </thead>
    <tbody>
      {#each view.arrayField<Record<string, string>>('results') as result}
        <tr>
          <td>{$t(`onboard.step.${result.aspect ?? ''}`)}</td>
          <td>{result.value}</td>
          <td>{$t(`onboard.auto.reason.${result.reason ?? 'default'}`)}</td>
        </tr>
      {/each}
    </tbody>
  </table>
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
