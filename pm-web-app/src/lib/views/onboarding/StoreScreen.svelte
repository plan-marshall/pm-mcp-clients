<script lang="ts">
  import AspectChoice, { type StoreRow } from '../../components/AspectChoice.svelte';
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** Where the machine keeps its tokens (`store.choose`, `store.reconfigure`). */
  let { view }: ScreenProps = $props();

  let stores = $derived(view.arrayField<StoreRow>('stores'));
  let platform = $derived(view.field('platform') ?? view.field('os') ?? view.field('backend'));
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.store.choose.title')}
    </h1>
  </div>
  {#if stores.length > 0}
    <AspectChoice
      kind="store"
      {stores}
      {platform}
      busy={view.busy}
      onRecord={(choice) => {
        const target = view.linkBy(String(choice), 'os', 'file') ?? view.links[0];
        if (target) view.onLink(target.id);
      }}
    />
  {:else if view.links.length === 0}
    <p class="state-card__next">{$t('onboard.state.no_links')}</p>
  {:else}
    <div class="state-card__actions">
      {#each view.links as { id, isPrimary } (id)}
        <Button
          variant={isPrimary ? 'primary' : 'secondary'}
          loading={view.busy}
          data-testid={isPrimary ? 'store-record' : `store-action-${id}`}
          on:click={() => view.onLink(id)}
        >
          {view.linkLabel(id)}
        </Button>
      {/each}
    </div>
  {/if}
</div>
