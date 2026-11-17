<script lang="ts">
  import Button from '../../components/Button.svelte';
  import CredentialEntry from '../../components/CredentialEntry.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * A provider whose check failed (`providers.failed`): the provider, the cause and the fix, and
   * the acts the state offers (UC-F-09, onboarding-steps-4-7.adoc, Check).
   *
   * "Enter the token again" opens the token entry for that provider in place; "Check again" and
   * "Continue without" are the state's own links, in its words.
   */
  let { view }: ScreenProps = $props();

  /** Whether the operator asked for the token entry. */
  let entering = $state(false);

  /** The provider's name, as the state reports it. */
  const provider = $derived(view.field('provider') ?? '');

  /** The key of the provider's token: the provider's id, or its name when it reports no id. */
  const credentialKey = $derived(view.field('provider_id') ?? provider);
</script>

<div class="state-card" data-testid="providers-failed">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.providers.failed.title')}
    </h1>
  </div>
  <dl class="detail-list">
    {#if provider}
      <div class="detail-row">
        <dt>{$t('onboard.providers.field')}</dt>
        <dd>{provider}</dd>
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

  <div class="providers-failed__token">
    <Button
      variant="secondary"
      loading={view.busy}
      aria-expanded={entering}
      onclick={() => (entering = !entering)}
      data-testid="providers-failed-enter-token"
    >
      {$t('onboard.providers.failed.enter_token')}
    </Button>
    {#if entering}
      <CredentialEntry credentialKey={credentialKey} label={provider} />
    {/if}
  </div>

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

<style>
  .providers-failed__token {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    margin-block-start: var(--space-4);
    align-items: flex-start;
  }
</style>
