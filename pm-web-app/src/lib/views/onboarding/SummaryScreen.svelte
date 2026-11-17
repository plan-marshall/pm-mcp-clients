<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** One recorded aspect of the setup, as the runtime reports it. */
  interface Aspect {
    aspect?: string;
    step?: number;
    summary?: string;
  }

  /**
   * The read-only review of the setup (`onboard.summary`): the runtime's summary and the summary it
   * recorded for each of the seven steps, with the links that go on.
   */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">{view.field('summary') || $t('onboard.settings.title')}</h1>
  </div>
  <ul class="completed-list" data-testid="setup-summary">
    {#each view.arrayField<Aspect>('aspects') as aspect}
      <li class="completed-row">
        <span class="completed-row__name">
          {$t(`onboard.step.${view.stepKey(aspect.step ?? 0)}`)}
        </span>
        <span class="completed-row__value">{aspect.summary ?? ''}</span>
      </li>
    {/each}
  </ul>
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
