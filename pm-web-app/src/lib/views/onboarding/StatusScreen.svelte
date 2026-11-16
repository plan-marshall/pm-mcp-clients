<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * A read-only state of the onboarding: what the runtime reported as the title, the cause and the
   * fix where it names them, and the links the state offers. It never prints the state's id.
   */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.state.details')}
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
    {#if view.links.length === 0}
      <p class="state-card__next">{$t('onboard.state.no_links')}</p>
    {:else}
      {#each view.links as { id, isPrimary }}
        <Button
          variant={isPrimary ? 'primary' : 'secondary'}
          loading={view.busy}
          on:click={() => view.onLink(id)}
        >
          {view.linkLabel(id)}
        </Button>
      {/each}
    {/if}
  </div>
</div>
