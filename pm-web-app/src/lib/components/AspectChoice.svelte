<script lang="ts">
  import { t } from '../i18n';
  import Button from './Button.svelte';

  /**
   * ⑤ Credentials and ⑥ Providers: the two review screens that pick from a list.
   *
   * Each shows what the runtime reports and records it with one button; the aspect's later run
   * from the machine settings uses the same screens with the current values as the proposal. The
   * plans' own choice screens (`defaults.review`) and the role assignment (`roles.review`) are
   * screens of their own beside this one.
   */
  interface StoreRow {
    kind: string;
    name: string;
    available: boolean;
    recommended?: boolean;
  }

  interface ProviderRow {
    provider: string;
    name: string;
    available: boolean;
    variant?: string;
    token_stored?: boolean;
    cause?: string;
  }

  export let kind: 'store' | 'providers' = 'store';
  export let stores: StoreRow[] = [];
  export let providers: ProviderRow[] = [];

  export let busy = false;

  export let onRecord: (choice: unknown) => void = () => {};

  let selectedStore = stores.find((s) => s.recommended)?.kind ?? stores[0]?.kind ?? '';
  let selectedProviders = providers.filter((p) => p.available).map((p) => p.provider);

  function toggleProvider(id: string): void {
    selectedProviders = selectedProviders.includes(id)
      ? selectedProviders.filter((v) => v !== id)
      : [...selectedProviders, id];
  }
</script>

<section class="choice-screen" data-testid={`${kind}-screen`}>
  {#if kind === 'store'}
    <ul class="choice-screen__list">
      {#each stores as store}
        <li class="choice-screen__row" class:choice-screen__row--disabled={!store.available}>
          <input
            type="radio"
            name="credential-store"
            value={store.kind}
            checked={selectedStore === store.kind}
            disabled={!store.available}
            aria-label={store.name}
            onchange={() => (selectedStore = store.kind)}
            data-testid={`store-${store.kind}`}
          />
          <div class="choice-screen__row-main">
            <span class="choice-screen__row-title">{store.name}</span>
            <span class="choice-screen__row-meta">
              {store.available
                ? $t('onboard.providers.select.available')
                : $t('onboard.providers.select.not_available')}
            </span>
          </div>
          {#if store.recommended}
            <span class="choice-screen__badge choice-screen__badge--default">
              {$t('onboard.catalogue.select.recommendation')}
            </span>
          {/if}
        </li>
      {/each}
    </ul>
  {:else}
    <ul class="choice-screen__list">
      {#each providers as provider}
        <li class="choice-screen__row" data-provider={provider.provider}>
          <input
            type="checkbox"
            checked={selectedProviders.includes(provider.provider)}
            disabled={!provider.available}
            aria-label={provider.name}
            onchange={() => toggleProvider(provider.provider)}
            data-testid={`provider-toggle-${provider.provider}`}
          />
          <div class="choice-screen__row-main">
            <span class="choice-screen__row-title">{provider.name}</span>
            <span class="choice-screen__row-meta">
              {#if provider.variant}{provider.variant} ·
              {/if}
              {provider.available
                ? $t('onboard.providers.select.available')
                : $t('onboard.providers.select.not_available')}
              {#if provider.token_stored}
                · {$t('onboard.providers.select.token_stored')}
              {/if}
              {#if provider.cause}· {provider.cause}{/if}
            </span>
          </div>
        </li>
      {/each}
    </ul>
  {/if}

  <div class="choice-screen__actions">
    <Button
      variant="primary"
      loading={busy}
      on:click={() =>
        onRecord(kind === 'store' ? selectedStore : selectedProviders)}
      data-testid={`${kind}-record`}
    >
      {#if kind === 'store'}
        {$t('onboard.store.choose.action', {
          store: stores.find((s) => s.kind === selectedStore)?.name ?? selectedStore,
        })}
      {:else}
        {$t('onboard.providers.select.action', { count: selectedProviders.length })}
      {/if}
    </Button>
  </div>
</section>
