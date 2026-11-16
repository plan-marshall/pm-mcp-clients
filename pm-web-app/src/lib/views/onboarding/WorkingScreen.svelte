<script lang="ts">
  import Button from '../../components/Button.svelte';
  import Spinner from '../../components/Spinner.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * A state the runtime is working in: what it does, the tokens it expects where the runtime states
   * them, and the links that let the operator go on without it.
   */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.step.models')}
    </h1>
  </div>
  <div class="model-working">
    <Spinner size="2rem" />
    <div class="model-working__details">
      <p class="model-working__status">
        {$t('common.model.working', {
          role: view.field('role') ?? 'steward',
          harness: view.field('harness') ?? '',
          model: view.field('model') ?? '',
        })}
      </p>
      {#if view.field('tokens')}
        <p class="model-working__estimate">{view.field('tokens')}</p>
      {/if}
    </div>
  </div>
  <p class="model-working__note">{$t('common.model.leave_note')}</p>
  {#if view.links.length > 0}
    <div class="state-card__actions">
      {#each view.links as { id }}
        <Button variant="secondary" loading={view.busy} on:click={() => view.onLink(id)}>
          {$t('common.model.continue_without')}
        </Button>
      {/each}
    </div>
  {/if}
</div>
