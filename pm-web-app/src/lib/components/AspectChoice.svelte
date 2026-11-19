<script lang="ts" module>
  /**
   * One credential store as `store.choose` publishes it (state catalogue
   * `19-onboarding-aspects-models-providers.adoc:180-186`): each offered store with what protects a
   * secret in it and what does not, the recommendation preselected. `protects_against` and
   * `does_not_protect_against` are the store's own protection text where the engine states one; a
   * store that carries none is shown with the generic line and no claim of a platform, because no
   * row and no `pm-api` type publishes the store's backend, platform or operating system.
   */
  export interface StoreRow {
    kind: string;
    name: string;
    available: boolean;
    recommended?: boolean;
    protects_against?: string;
    does_not_protect_against?: string;
  }

  /** One provider as the `providers.select` row publishes it (state catalogue `19-…:262-268`). */
  export interface ProviderRow {
    provider: string;
    name: string;
    available: boolean;
    variant?: string;
    token_stored?: boolean;
    cause?: string;
  }
</script>

<script lang="ts">
  import { t } from '../i18n';
  import { proposalDelta } from './choiceProposal';
  import Button from './Button.svelte';
  import Marker from './Marker.svelte';

  /**
   * ⑤ Credentials and ⑥ Providers: the two review screens that pick from a list.
   *
   * Each shows what the runtime reports and records it with one button; the aspect's later run
   * from the machine settings uses the same screens with the current values as the proposal. The
   * plans' own choice screens (`defaults.review`) and the role assignment (`roles.review`) are
   * screens of their own beside this one.
   */
  export let kind: 'store' | 'providers' = 'store';
  export let stores: readonly StoreRow[] = [];
  export let providers: readonly ProviderRow[] = [];
  export let busy = false;

  export let onRecord: (choice: unknown) => void = () => {};

  /**
   * The store the screen proposes, from the published recommendation alone: the recommended store
   * where one is available, else the first available one. An unavailable store is never proposed,
   * but it stays selectable — choosing it is the way into `store.unavailable` (`19-…:185-191`).
   */
  function pickDefaultStore(list: readonly StoreRow[]): string {
    return (
      list.find((s) => s.available && s.recommended)?.kind ??
      list.find((s) => s.available)?.kind ??
      ''
    );
  }

  let prevStores: readonly StoreRow[] = stores;
  let prevProviders: readonly ProviderRow[] = providers;
  let selectedStore = pickDefaultStore(stores);
  let selectedProviders = providers.filter((p) => p.available).map((p) => p.provider);
  // The edits a refreshed proposal dropped because the row they belonged to is gone; the screen
  // says so rather than losing them silently.
  let droppedEdits: string[] = [];

  // A refresh keeps the operator's choice: only a selection that is no longer selectable is
  // resynced (a store that vanished, or one that cannot be recorded any more), and a vanished row
  // says so.
  $: {
    const delta = proposalDelta(
      prevStores.map((s) => s.kind),
      stores.map((s) => s.kind),
    );
    const availabilityChanged =
      delta.added.length === 0 &&
      delta.removed.length === 0 &&
      stores.some((s) => {
        const before = prevStores.find((p) => p.kind === s.kind);
        return before !== undefined && before.available !== s.available;
      });
    if (delta.added.length > 0 || delta.removed.length > 0 || availabilityChanged) {
      const previous = prevStores;
      prevStores = stores;
      const selected = stores.find((s) => s.kind === selectedStore);
      // An unavailable OS store stays a legal choice: it is the way into `store.unavailable`.
      const selectable = selected !== undefined && (selected.available || selected.kind === 'os');
      if (selectedStore !== '' && !selectable) {
        const gone = previous
          .filter((s) => delta.removed.includes(s.kind))
          .map((s) => s.name);
        if (gone.length > 0) droppedEdits = gone;
        selectedStore = pickDefaultStore(stores);
      }
    }
  }

  // A refresh keeps the operator's ticks: a provider that stays keeps its state, a new available
  // one is proposed, a vanished or no-longer-available one takes its tick with it.
  $: {
    const delta = proposalDelta(
      prevProviders.map((p) => p.provider),
      providers.map((p) => p.provider),
    );
    const availabilityChanged =
      delta.added.length === 0 &&
      delta.removed.length === 0 &&
      providers.some((p) => {
        const before = prevProviders.find((q) => q.provider === p.provider);
        return before !== undefined && before.available !== p.available;
      });
    if (delta.added.length > 0 || delta.removed.length > 0 || availabilityChanged) {
      const previous = prevProviders;
      prevProviders = providers;
      const untickedGone = previous.filter(
        (p) => delta.removed.includes(p.provider) && p.available && !selectedProviders.includes(p.provider),
      );
      if (untickedGone.length > 0) {
        droppedEdits = [...droppedEdits, ...untickedGone.map((p) => p.name)];
      }
      const present = new Set(providers.filter((p) => p.available).map((p) => p.provider));
      const kept = selectedProviders.filter((id) => present.has(id));
      const added = providers
        .filter((p) => delta.added.includes(p.provider) && p.available)
        .map((p) => p.provider)
        .filter((id) => !kept.includes(id));
      selectedProviders = [...kept, ...added];
    }
  }

  $: activeStore = stores.find((s) => s.kind === selectedStore);

  function toggleProvider(id: string): void {
    selectedProviders = selectedProviders.includes(id)
      ? selectedProviders.filter((v) => v !== id)
      : [...selectedProviders, id];
  }

  /**
   * The store's own protection text where the engine states one, else the generic line the
   * catalogue row names — never a claim derived from a platform the state does not publish.
   *
   * @param store the store row
   * @param field which of the two lines to read
   * @returns the text of the line
   */
  function protectionDetail(store: StoreRow, field: 'protects' | 'does_not_protect'): string {
    const custom = field === 'protects' ? store.protects_against : store.does_not_protect_against;
    if (custom) return custom;
    return $t(
      field === 'protects'
        ? 'onboard.store.choose.unknown.protects'
        : 'onboard.store.choose.unknown.does_not_protect',
    );
  }
</script>

<section class="choice-screen" data-testid={`${kind}-screen`}>
  {#if kind === 'store'}
    <ul class="choice-screen__list">
      {#each stores as store (store.kind)}
        {@const protectsId = `store-${store.kind}-protects`}
        {@const doesNotProtectId = `store-${store.kind}-does-not-protect`}
        <li class="choice-screen__row" class:choice-screen__row--disabled={!store.available}>
          <input
            type="radio"
            name="credential-store"
            value={store.kind}
            checked={selectedStore === store.kind}
            aria-label={store.name}
            aria-describedby={`${protectsId} ${doesNotProtectId}`}
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
            <span class="choice-screen__row-meta" id={protectsId}>
              {$t('onboard.store.choose.protects_against', {
                value: protectionDetail(store, 'protects'),
              })}
            </span>
            <span class="choice-screen__row-meta" id={doesNotProtectId}>
              {$t('onboard.store.choose.does_not_protect_against', {
                value: protectionDetail(store, 'does_not_protect'),
              })}
            </span>
          </div>
          {#if store.recommended}
            <div class="choice-screen__row-side">
              <Marker kind="recommended" label={store.name} />
            </div>
          {/if}
        </li>
      {/each}
    </ul>
  {:else}
    <ul class="choice-screen__list">
      {#each providers as provider (provider.provider)}
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

  {#if droppedEdits.length > 0}
    <p class="choice-screen__note" role="status" data-testid={`${kind}-dropped`}>
      {$t('onboard.choice.edit_dropped', { rows: droppedEdits.join(', ') })}
    </p>
  {/if}

  <div class="choice-screen__actions">
    <Button
      variant="primary"
      disabled={kind === 'store' ? selectedStore === '' : false}
      loading={busy}
      on:click={() =>
        onRecord(kind === 'store' ? selectedStore : selectedProviders)}
      data-testid={`${kind}-record`}
    >
      {#if kind === 'store'}
        {activeStore
          ? $t('onboard.store.choose.action', {
              store: activeStore.name ?? activeStore.kind,
            })
          : $t('onboard.store.choose.none')}
      {:else}
        {$t('onboard.providers.select.action', { count: selectedProviders.length })}
      {/if}
    </Button>
  </div>
</section>
