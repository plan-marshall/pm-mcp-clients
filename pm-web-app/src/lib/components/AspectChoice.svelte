<script lang="ts">
  import { t } from '../i18n';
  import Button from './Button.svelte';

  /**
   * ④ Roles, ⑤ Credentials, ⑥ Providers, ⑦ Plans: the review screens.
   *
   * Each shows what the runtime reports and records it with one button; the aspect's later run
   * from the machine settings uses the same screens with the current values as the proposal.
   */
  interface RoleRow {
    role: string;
    harness_id: string;
    model_id: string;
    effort?: string;
    origin?: string;
  }

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

  interface PlanDefaultsRow {
    landing: string;
    method: string;
    asking: string;
    posture?: string;
  }

  export let kind: 'roles' | 'store' | 'providers' | 'defaults' = 'roles';
  export let roles: RoleRow[] = [];
  export let stores: StoreRow[] = [];
  export let providers: ProviderRow[] = [];
  export let planDefaults: PlanDefaultsRow | null = null;
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
  {#if kind === 'roles'}
    <ul class="choice-screen__list">
      {#each roles as role}
        <li class="choice-screen__row" data-role={role.role}>
          <div class="choice-screen__row-main">
            <span class="choice-screen__row-title">{role.role}</span>
            <span class="choice-screen__row-meta">
              {$t('onboard.roles.review.assignment', {
                harness: role.harness_id,
                model: role.model_id,
              })}
              {#if role.effort}· {$t('onboard.roles.review.effort', { effort: role.effort })}{/if}
            </span>
          </div>
        </li>
      {/each}
    </ul>
    <p class="choice-screen__hint">
      {$t('onboard.roles.review.defaults', { count: roles.length })}
    </p>
  {:else if kind === 'store'}
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
  {:else if kind === 'providers'}
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
  {:else if kind === 'defaults' && planDefaults}
    <dl class="defaults-list">
      <div class="defaults-list__row">
        <dt>{$t('onboard.defaults.review.landing')}</dt>
        <dd class="mono">{planDefaults.landing}</dd>
      </div>
      <div class="defaults-list__row">
        <dt>{$t('onboard.defaults.review.method')}</dt>
        <dd class="mono">{planDefaults.method}</dd>
      </div>
      <div class="defaults-list__row">
        <dt>{$t('onboard.defaults.review.asking')}</dt>
        <dd class="mono">{planDefaults.asking}</dd>
      </div>
      {#if planDefaults.posture}
        <div class="defaults-list__row">
          <dt>{$t('onboard.defaults.review.posture')}</dt>
          <dd class="mono">{planDefaults.posture}</dd>
        </div>
      {/if}
    </dl>
  {/if}

  <div class="choice-screen__actions">
    <Button
      variant="primary"
      loading={busy}
      on:click={() =>
        onRecord(
          kind === 'store'
            ? selectedStore
            : kind === 'providers'
              ? selectedProviders
              : planDefaults,
        )}
      data-testid={`${kind}-record`}
    >
      {#if kind === 'roles'}
        {$t('onboard.roles.review.action')}
      {:else if kind === 'store'}
        {$t('onboard.store.choose.action', {
          store: stores.find((s) => s.kind === selectedStore)?.name ?? selectedStore,
        })}
      {:else if kind === 'providers'}
        {$t('onboard.providers.select.action', { count: selectedProviders.length })}
      {:else}
        {$t('onboard.defaults.review.action')}
      {/if}
    </Button>
  </div>
</section>

<style>
  .defaults-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .defaults-list__row {
    display: grid;
    grid-template-columns: 10rem 1fr;
    gap: var(--space-4);
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    font-size: var(--text-sm);
  }

  .defaults-list__row dt {
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .defaults-list__row dd {
    color: var(--color-text-primary);
    overflow-wrap: anywhere;
  }

  @media (max-width: 768px) {
    .defaults-list__row {
      grid-template-columns: 1fr;
      gap: var(--space-1);
    }
  }

  .defaults-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .defaults-list__row {
    display: grid;
    grid-template-columns: 10rem 1fr;
    gap: var(--space-4);
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    font-size: var(--text-sm);
  }

  .defaults-list__row dt {
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .defaults-list__row dd {
    color: var(--color-text-primary);
    overflow-wrap: anywhere;
  }

  @media (max-width: 768px) {
    .defaults-list__row {
      grid-template-columns: 1fr;
      gap: var(--space-1);
    }
  }
</style>
