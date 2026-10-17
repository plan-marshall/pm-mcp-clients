<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import {
    runtimeStore,
    navigateTo,
    loadMachineSettings,
    updateMachineSetting,
    withdrawConsent,
  } from '../stores/runtime';
  import type { ConsentRecord, MachineSettingsRecord } from '../api/client';

  let isLoading = false;
  let notice = '';
  let error = '';
  let documentsMode = 'ask';
  let cleanupDays = 30;
  let cleanupRemindDays = 3;

  onMount(async () => {
    isLoading = true;
    const settings = await loadMachineSettings();
    applyLocal(settings);
    isLoading = false;
  });

  $: settings = $runtimeStore.machineSettings;

  function applyLocal(value: MachineSettingsRecord | null): void {
    if (!value) return;
    documentsMode = value.documents_mode || 'ask';
    cleanupDays = value.cleanup_durations?.plans_after_days ?? 30;
    cleanupRemindDays = value.cleanup_durations?.remind_after_days ?? 3;
  }

  async function saveDocumentsMode(): Promise<void> {
    error = '';
    notice = '';
    const result = await updateMachineSetting('documents_mode', documentsMode);
    if (result?.conflict) {
      error = $t('web.settings.conflict.refused', {
        value: result.current_value ?? '',
      });
      return;
    }
    notice = $t('web.settings.saved');
  }

  async function saveCleanup(): Promise<void> {
    error = '';
    notice = '';
    const result = await updateMachineSetting('cleanup_durations', String(cleanupDays));
    if (result?.conflict) {
      error = $t('web.settings.conflict.refused', {
        value: result.current_value ?? '',
      });
      return;
    }
    notice = $t('web.settings.saved');
  }

  async function withdraw(consent: ConsentRecord): Promise<void> {
    error = '';
    notice = '';
    await withdrawConsent(consent.kind);
    notice = $t('web.settings.consents.withdrawn');
  }

  function stepName(aspect: string): string {
    return $t(`onboard.step.${aspectKey(aspect)}`);
  }

  function aspectKey(aspect: string): string {
    const map: Record<string, string> = {
      language: 'language',
      harness: 'harnesses',
      catalogue: 'models',
      roles: 'roles',
      store: 'credentials',
      providers: 'providers',
      defaults: 'plans',
    };
    return map[aspect] ?? aspect;
  }
</script>

<div class="machine-settings" data-testid="machine-settings-view">
  <div class="machine-settings__header">
    <h1 class="machine-settings__title">{$t('web.settings.page.title')}</h1>
    <Button variant="secondary" on:click={() => navigateTo('settings')}>
      {$t('web.settings.back')}
    </Button>
  </div>

  {#if isLoading}
    <p class="loading-text">Loading…</p>
  {:else if !settings}
    <p class="empty-text">{$t('web.settings.page.title')}</p>
  {:else}
    {#if error}
      <p class="error-text" role="alert">{error}</p>
    {/if}
    {#if notice}
      <p class="notice-text">{notice}</p>
    {/if}

    <section class="settings-section">
      <h2 class="settings-section__title">{$t('web.settings.aspects.title')}</h2>
      <ul class="aspect-list" data-testid="aspect-list">
        {#each settings.aspects as aspect}
          <li class="aspect-row" data-aspect={aspect.aspect}>
            <span class="aspect-row__name">
              {aspect.step}. {stepName(aspect.aspect)}
            </span>
            <span class="aspect-row__value">{aspect.summary}</span>
            <Button
              variant="ghost"
              on:click={() => navigateTo('onboarding')}
              aria-label={`${$t('web.settings.aspects.change')} ${stepName(aspect.aspect)}`}
            >
              {$t('web.settings.aspects.change')}
            </Button>
          </li>
        {/each}
      </ul>
      <p class="hint-text">{$t('web.settings.aspects.project_note')}</p>
    </section>

    <section class="settings-section" data-testid="consents-section">
      <h2 class="settings-section__title">{$t('web.settings.consents.title')}</h2>
      {#if settings.consents.length === 0}
        <p class="empty-text" data-testid="no-consents">{$t('web.settings.consents.none')}</p>
      {:else}
        <ul class="consent-list">
          {#each settings.consents as consent}
            <li class="consent-row" data-consent={consent.kind}>
              <span>{$t(`common.consent.kind.${consent.kind}`)}</span>
              <span class="consent-row__where">
                {consent.granted_at ?? ''} · {consent.channel ?? $t('web.settings.consents.where_terminal')}
              </span>
              <Button
                variant="ghost"
                on:click={() => withdraw(consent)}
                data-testid={`withdraw-${consent.kind}`}
              >
                {$t('web.settings.consents.withdraw')}
              </Button>
            </li>
          {/each}
        </ul>
      {/if}
    </section>

    <section class="settings-section" data-testid="documents-section">
      <h2 class="settings-section__title">{$t('web.settings.documents.title')}</h2>
      <div class="choice-row">
        {#each ['copy', 'move', 'ask'] as mode}
          <label class="choice">
            <input type="radio" name="documents_mode" value={mode} bind:group={documentsMode} />
            {$t(`web.settings.documents.${mode}`)}
          </label>
        {/each}
        <Button variant="secondary" on:click={saveDocumentsMode} data-testid="save-documents">
          {$t('web.common.save')}
        </Button>
      </div>
    </section>

    <section class="settings-section" data-testid="cleanup-section">
      <h2 class="settings-section__title">{$t('web.settings.cleanup.title')}</h2>
      <div class="choice-row">
        <label class="choice">
          <input
            type="number"
            min="0"
            bind:value={cleanupDays}
            data-testid="cleanup-days"
            aria-label={$t('web.settings.cleanup.days')}
          />
          {$t('web.settings.cleanup.days')}
        </label>
        <label class="choice">
          <input type="number" min="0" bind:value={cleanupRemindDays} />
          {$t('web.settings.cleanup.days')}
        </label>
        <Button variant="secondary" on:click={saveCleanup} data-testid="save-cleanup">
          {$t('web.settings.cleanup.save')}
        </Button>
      </div>
    </section>

    <section class="settings-section" data-testid="credentials-section">
      <h2 class="settings-section__title">{$t('web.settings.credentials.title')}</h2>
      <ul class="credential-list">
        {#each settings.credentials as credential}
          <li class="credential-row" data-credential={credential.key}>
            <span>{credential.provider} · {credential.kind}</span>
            {#if credential.expired}
              <span class="warn">⚠ {$t('web.settings.providers.not_available')}</span>
            {:else if credential.expires_in_days !== undefined}
              <span class="warn">
                ⚠ {$t('web.settings.providers.expires_soon', { days: credential.expires_in_days })}
              </span>
            {/if}
            <Button variant="ghost">{$t('web.settings.credentials.rotate')}</Button>
            <Button variant="ghost">{$t('web.settings.credentials.replace')}</Button>
          </li>
        {/each}
      </ul>
    </section>

    <section class="settings-section" data-testid="devices-summary">
      <h2 class="settings-section__title">{$t('web.settings.devices.title')}</h2>
      <Button variant="secondary" on:click={() => navigateTo('settings')}>
        {$t('web.settings.devices.manage')}
      </Button>
    </section>
  {/if}
</div>

<style>
  .machine-settings {
    max-width: 900px;
    margin: 0 auto;
    padding: var(--space-8) var(--space-4);
  }

  .machine-settings__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-8);
  }

  .machine-settings__title {
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .settings-section {
    margin-bottom: var(--space-8);
  }

  .settings-section__title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: var(--space-3);
  }

  .aspect-list,
  .consent-list,
  .credential-list {
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .aspect-row,
  .consent-row,
  .credential-row {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
  }

  .aspect-row__name {
    font-weight: 600;
    min-width: 9rem;
  }

  .aspect-row__value,
  .consent-row__where {
    flex: 1;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .choice-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-4);
  }

  .choice {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    font-size: var(--text-sm);
  }

  .hint-text {
    margin-top: var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .warn {
    color: var(--color-warning, #b45309);
    font-size: var(--text-sm);
  }

  .notice-text {
    color: var(--color-primary);
    font-size: var(--text-sm);
  }

  .error-text {
    color: var(--color-danger);
    font-size: var(--text-sm);
  }

  .empty-text,
  .loading-text {
    padding: var(--space-6) var(--space-4);
    color: var(--color-text-secondary);
  }
</style>
