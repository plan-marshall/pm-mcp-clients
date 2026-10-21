<script lang="ts">
  import { t } from '../i18n';
  import type { PlanSettingsRecord } from '../api/client';

  /** A value another client changed while this dialogue was open. */
  interface SettingsConflict {
    aspect: string;
    currentValue: string;
    wantedValue: string;
    revision: number;
  }

  export let planId: string;
  export let settings: PlanSettingsRecord | null = null;
  export let open = false;
  export let busy = false;
  /** A change another client made first, shown until the operator decides. */
  export let conflict: SettingsConflict | null = null;
  /** A refusal or a too-late answer the runtime gave for this dialogue. */
  export let notice = '';
  export let onApply: (payload: {
    aspect: string;
    value: string | null;
    revision: number;
  }) => void = () => {};
  export let onApplyAgain: (payload: {
    aspect: string;
    value: string | null;
    revision: number;
  }) => void = () => {};
  export let onKeepCurrent: () => void = () => {};
  export let onClose: () => void = () => {};

  const GROUPS = ['plan_defaults', 'roles', 'integrations', 'providers'] as const;
  let chosen: Record<string, string> = {};
  let seededRevision: number | null = null;

  // A new revision is the runtime's value: reseed the selects, so a kept or applied change does
  // not leave the operator's rejected value selected.
  $: if (settings && settings.revision !== seededRevision) {
    seededRevision = settings.revision;
    chosen = Object.fromEntries(settings.values.map((entry) => [entry.aspect, entry.value ?? '']));
  }

  function groupFor(aspect: string): string {
    if (aspect.startsWith('role.')) return 'roles';
    if (aspect.startsWith('integration.')) return 'integrations';
    if (aspect.startsWith('provider.')) return 'providers';
    return 'plan_defaults';
  }

  function groups(): string[] {
    if (!settings) return [];
    const present = new Set(settings.values.map((entry) => groupFor(entry.aspect)));
    return GROUPS.filter((group) => present.has(group));
  }

  function apply(aspect: string): void {
    if (!settings) return;
    const value = chosen[aspect] === '' ? null : chosen[aspect];
    onApply({ aspect, value, revision: settings.revision });
  }
</script>

{#if open}
  <div class="settings-modal" data-testid="settings-modal" role="dialog" aria-modal="true">
    <div class="settings-modal__panel">
      <h2 class="settings-modal__title" data-testid="settings-title">
        {$t('control.settings.title', { plan: planId })}
      </h2>

      {#if notice}
        <p class="settings-modal__notice" data-testid="settings-notice" role="status">{notice}</p>
      {/if}

      {#if conflict}
        <div class="settings-modal__conflict" data-testid="settings-conflict" role="alert">
          <h3 class="settings-modal__conflict-title">
            {$t('control.settings.conflict_title')}
          </h3>
          <p>
            {$t('control.settings.conflict', { value: conflict.currentValue })}
          </p>
          <dl class="settings-modal__conflict-facts">
            <dt>{$t('control.settings.yours')}</dt>
            <dd data-testid="settings-conflict-yours">{conflict.wantedValue}</dd>
            <dt>{$t('control.settings.origin', { origin: 'plan' })}</dt>
            <dd data-testid="settings-conflict-current">{conflict.currentValue}</dd>
          </dl>
          <div class="settings-modal__conflict-actions">
            <button
              type="button"
              data-testid="settings-conflict-keep"
              disabled={busy}
              on:click={() => onKeepCurrent()}
            >
              {$t('control.settings.keep_current')}
            </button>
            <button
              type="button"
              data-testid="settings-conflict-apply-again"
              disabled={busy}
              on:click={() =>
                onApplyAgain({
                  aspect: conflict.aspect,
                  value: conflict.wantedValue === '' ? null : conflict.wantedValue,
                  revision: conflict.revision,
                })}
            >
              {$t('control.settings.apply_again')}
            </button>
          </div>
        </div>
      {/if}

      {#if settings}
        {#each groups() as group (group)}
          <section class="settings-modal__group" data-testid={`settings-group-${group}`}>
            <h3>{$t('control.settings.group', { group })}</h3>
            {#each settings.values.filter((entry) => groupFor(entry.aspect) === group) as entry (entry.aspect)}
              <div class="settings-modal__row" data-testid={`settings-aspect-${entry.aspect}`}>
                <span class="settings-modal__aspect">{entry.aspect}</span>
                <select
                  class="settings-modal__value"
                  data-testid={`settings-value-${entry.aspect}`}
                  bind:value={chosen[entry.aspect]}
                  disabled={busy}
                >
                  <option value=""
                    >{$t('control.settings.use_inherited', { level: entry.origin })}</option
                  >
                  {#each entry.choices ?? [] as choice (choice)}
                    <option value={choice}>{choice}</option>
                  {/each}
                </select>
                <span
                  class="settings-modal__origin"
                  data-testid={`settings-origin-${entry.aspect}`}
                >
                  {$t('control.settings.origin', { origin: entry.origin })}
                </span>
                <button
                  type="button"
                  class="settings-modal__apply"
                  data-testid={`settings-apply-${entry.aspect}`}
                  on:click={() => apply(entry.aspect)}
                  disabled={busy}
                >
                  {$t('common.save')}
                </button>
              </div>
            {/each}
          </section>
        {/each}
      {:else}
        <p class="settings-modal__empty">…</p>
      {/if}

      <div class="settings-modal__actions">
        <button type="button" data-testid="settings-close-button" on:click={() => onClose()}>
          {$t('control.settings.close')}
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  .settings-modal {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgb(0 0 0 / 0.4);
    z-index: 50;
  }

  .settings-modal__panel {
    max-width: 40rem;
    width: 100%;
    margin: var(--space-3);
    padding: var(--space-4);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-surface);
    overflow-y: auto;
    max-height: 90vh;
  }

  .settings-modal__row {
    display: grid;
    grid-template-columns: 10rem 1fr auto auto;
    gap: var(--space-2);
    align-items: center;
    padding: var(--space-1) 0;
  }

  .settings-modal__actions {
    display: flex;
    justify-content: flex-end;
    margin-top: var(--space-3);
  }

  .settings-modal__notice {
    margin: 0 0 var(--space-2) 0;
    padding: var(--space-2);
    border-left: 3px solid var(--color-warning);
    background-color: var(--color-warning-subtle);
    font-size: var(--text-sm);
  }

  .settings-modal__conflict {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin: 0 0 var(--space-3) 0;
    padding: var(--space-3);
    border: 1px solid var(--color-warning);
    border-radius: var(--radius-md);
    background-color: var(--color-warning-subtle);
  }

  .settings-modal__conflict-title {
    margin: 0;
    font-size: var(--text-base);
  }

  .settings-modal__conflict-facts {
    display: grid;
    grid-template-columns: max-content 1fr;
    gap: var(--space-1) var(--space-3);
    margin: 0;
    font-size: var(--text-sm);
  }

  .settings-modal__conflict-actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .settings-modal__conflict-actions button {
    min-height: 44px;
    padding: var(--space-2) var(--space-3);
    cursor: pointer;
  }

  .settings-modal__actions button,
  .settings-modal__apply {
    min-height: 44px;
    padding: var(--space-2) var(--space-3);
    cursor: pointer;
  }

  @media (max-width: 480px) {
    .settings-modal__row {
      grid-template-columns: 1fr;
    }

    .settings-modal__panel {
      margin: 0;
      border-radius: 0;
      min-height: 100%;
    }
  }
</style>
