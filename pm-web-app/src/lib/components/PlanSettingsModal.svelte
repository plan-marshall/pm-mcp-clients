<script lang="ts">
  import { t } from '../i18n';
  import type { PlanSettingsRecord } from '../api/client';

  export let planId: string;
  export let settings: PlanSettingsRecord | null = null;
  export let open = false;
  export let busy = false;
  export let onApply: (payload: { aspect: string; value: string | null; revision: number }) => void =
    () => {};
  export let onClose: () => void = () => {};

  const GROUPS = ['plan_defaults', 'roles', 'integrations', 'providers'] as const;
  let chosen: Record<string, string> = {};

  $: if (settings) {
    const next: Record<string, string> = {};
    for (const entry of settings.values) {
      if (!(entry.aspect in chosen)) {
        next[entry.aspect] = entry.value ?? '';
      }
    }
    if (Object.keys(next).length > 0) {
      chosen = { ...chosen, ...next };
    }
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
                  <option value="">{$t('control.settings.use_inherited', { level: entry.origin })}</option>
                  {#each entry.choices ?? [] as choice (choice)}
                    <option value={choice}>{choice}</option>
                  {/each}
                </select>
                <span class="settings-modal__origin" data-testid={`settings-origin-${entry.aspect}`}>
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
