<script lang="ts">
  import AspectChoice, { type StoreRow } from '../../components/AspectChoice.svelte';
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** Where the machine keeps its tokens (`store.choose`, `store.reconfigure`). */
  let { view }: ScreenProps = $props();

  let stores = $derived(view.arrayField<StoreRow>('stores'));

  /**
   * Records the link of the kind the operator chose: `os` for an available OS store, `file` for the
   * file store, and `os_unavailable` for an OS store that is not available now — the catalogue's
   * way into `store.unavailable` (`19-onboarding-aspects-models-providers.adoc:185-191`). A kind
   * the state offers no link for records nothing; the screen never falls back to another link.
   *
   * @param choice the kind the operator chose
   */
  function record(choice: unknown): void {
    const kind = String(choice);
    const store = stores.find((s) => s.kind === kind);
    const linkId = kind === 'os' && store && !store.available ? 'os_unavailable' : kind;
    const target = view.linkBy(linkId);
    if (target) view.onLink(target.id);
  }
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
      busy={view.busy}
      onRecord={record}
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
