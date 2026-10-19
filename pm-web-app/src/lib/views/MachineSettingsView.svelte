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
    storeCredential,
  } from '../stores/runtime';
  import type {
    ConsentRecord,
    MachineCredentialStatus,
    MachineSettingsRecord,
  } from '../api/client';

  /** A setting changed here that another client changed meanwhile. */
  interface ConflictState {
    key: string;
    currentValue: string;
    wantedValue: string;
  }

  let isLoading = false;
  let notice = '';
  let error = '';
  let documentsMode = 'ask';
  let cleanupDays = 30;
  let cleanupRemindDays = 3;
  let conflict: ConflictState | null = null;
  let rotating: MachineCredentialStatus | null = null;
  let rotatingMode: 'rotate' | 'replace' = 'rotate';
  let rotatingSecret = '';
  let rotatingKind = '';
  let isSavingToken = false;
  let tokenNotice = '';
  let tokenError = '';

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

  /**
   * Applies one setting, and opens the conflict dialogue instead of overwriting a value that
   * changed in another client.
   */
  async function applySetting(key: string, value: string | null): Promise<void> {
    error = '';
    notice = '';
    conflict = null;
    const result = await updateMachineSetting(key, value);
    if (result?.conflict) {
      conflict = {
        key,
        currentValue: result.current_value ?? '',
        wantedValue: value ?? '',
      };
      return;
    }
    if (!result) {
      error = $t('web.settings.update_failed');
      return;
    }
    notice = $t('web.settings.saved');
  }

  async function saveDocumentsMode(): Promise<void> {
    await applySetting('documents_mode', documentsMode);
  }

  async function saveCleanup(): Promise<void> {
    await applySetting('cleanup_durations', String(cleanupDays));
  }

  /** Applies the value this page wanted, on the revision the other client left behind. */
  async function applyConflictAgain(): Promise<void> {
    if (!conflict) return;
    const wanted = conflict.wantedValue;
    const key = conflict.key;
    conflict = null;
    await applySetting(key, wanted);
  }

  /** Keeps the other client's value and closes the dialogue, without writing anything. */
  async function keepCurrentValue(): Promise<void> {
    conflict = null;
    applyLocal(await loadMachineSettings());
  }

  async function withdraw(consent: ConsentRecord): Promise<void> {
    error = '';
    notice = '';
    const result = await withdrawConsent(consent.kind);
    if (result?.conflict) {
      conflict = {
        key: `consent:${consent.kind}`,
        currentValue: result.current_value ?? '',
        wantedValue: '',
      };
      return;
    }
    notice = $t('web.settings.consents.withdrawn');
  }

  function startRotate(credential: MachineCredentialStatus, mode: 'rotate' | 'replace'): void {
    rotating = credential;
    rotatingMode = mode;
    rotatingSecret = '';
    rotatingKind = credential.kind;
    tokenNotice = '';
    tokenError = '';
  }

  function closeRotate(): void {
    rotating = null;
    rotatingSecret = '';
  }

  async function saveToken(): Promise<void> {
    if (!rotating || !rotatingSecret || isSavingToken) return;
    isSavingToken = true;
    tokenError = '';
    // A rotation keeps the recorded kind; a replacement records the chosen one.
    const kind = rotatingMode === 'replace' ? rotatingKind : null;
    const result = await storeCredential(rotating.key, rotatingSecret, kind);
    isSavingToken = false;
    if (result.ok) {
      tokenNotice = $t('web.settings.credentials.rotated', { key: rotating.key });
      rotatingSecret = '';
      rotating = null;
      if (result.kindFailed) {
        tokenError = $t('web.settings.update_failed');
      }
    } else {
      tokenError = $t('web.settings.credentials.failed', {
        cause: result.cause || $t('web.settings.update_failed'),
      });
    }
  }

  function stepName(aspect: string): string {
    return $t(`onboard.step.${aspectKey(aspect)}`);
  }

  function aspectKey(aspect: string): string {
    const map: Record<string, string> = {
      language: 'language',
      harness: 'harnesses',
      harnesses: 'harnesses',
      catalogue: 'models',
      models: 'models',
      roles: 'roles',
      store: 'credentials',
      credentials: 'credentials',
      providers: 'providers',
      defaults: 'defaults',
      plans: 'plans',
    };
    return map[aspect] ?? aspect;
  }

  function expiryLabel(credential: MachineCredentialStatus): string {
    if (credential.expired) {
      const held = credential.held_plan ? 1 : 0;
      return $t('web.settings.providers.expired', { count: held });
    }
    if (credential.expires_in_days !== undefined) {
      return $t('web.settings.providers.expires_soon', { days: credential.expires_in_days });
    }
    return $t('web.settings.providers.no_expiry');
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
    <p class="loading-text">{$t('web.settings.devices.loading')}</p>
  {:else if !settings}
    <p class="empty-text">{$t('web.settings.aspects.empty')}</p>
  {:else}
    {#if error}
      <p class="error-text" role="alert">{error}</p>
    {/if}
    {#if notice}
      <p class="notice-text">{notice}</p>
    {/if}

    {#if conflict}
      <div class="conflict" role="alert" data-testid="conflict-dialog">
        <h2 class="conflict__title">{$t('web.settings.conflict.title')}</h2>
        <p class="conflict__line">
          {$t('web.settings.conflict.current', { value: conflict.currentValue })}
        </p>
        <p class="conflict__line">
          {$t('web.settings.conflict.yours', { value: conflict.wantedValue })}
        </p>
        <div class="conflict__actions">
          <Button variant="ghost" on:click={keepCurrentValue} data-testid="conflict-keep">
            {$t('web.settings.conflict.keep')}
          </Button>
          <Button variant="primary" on:click={applyConflictAgain} data-testid="conflict-apply">
            {$t('web.settings.conflict.apply_again')}
          </Button>
        </div>
      </div>
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
                {consent.granted_at ?? ''} · {consent.channel ??
                  $t('web.settings.consents.where_terminal')}
              </span>
              {#if consent.project}
                <span class="consent-row__where">
                  {consent.project}
                  {#if consent.script_count !== undefined}
                    · {$t('web.settings.consents.scripts', { count: consent.script_count })}
                  {/if}
                </span>
              {/if}
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
      <p class="hint-text">{$t('web.settings.documents.loopback_only')}</p>
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
            aria-label={$t('web.settings.cleanup.title')}
          />
          {$t('web.settings.cleanup.days')}
        </label>
        <label class="choice">
          <input type="number" min="0" bind:value={cleanupRemindDays} />
          {$t('web.settings.cleanup.remind')}
          {$t('web.settings.cleanup.days')}
        </label>
        <Button variant="secondary" on:click={saveCleanup} data-testid="save-cleanup">
          {$t('web.settings.cleanup.save')}
        </Button>
      </div>
    </section>

    <section class="settings-section" data-testid="credentials-section">
      <h2 class="settings-section__title">{$t('web.settings.credentials.title')}</h2>
      {#if settings.credentials.length === 0}
        <p class="empty-text">{$t('web.settings.credentials.none')}</p>
      {:else}
        <ul class="credential-list">
          {#each settings.credentials as credential}
            <li class="credential-row" data-credential={credential.key}>
              <span class="credential-row__main">
                {credential.key} · {credential.provider} · {credential.kind}
              </span>
              <span class="credential-row__expiry" class:warn={credential.expired}>
                {expiryLabel(credential)}
              </span>
              <Button
                variant="ghost"
                on:click={() => startRotate(credential, 'rotate')}
                data-testid={`rotate-${credential.key}`}
              >
                {$t('web.settings.credentials.rotate')}
              </Button>
              <Button
                variant="ghost"
                on:click={() => startRotate(credential, 'replace')}
                data-testid={`replace-${credential.key}`}
              >
                {$t('web.settings.credentials.replace')}
              </Button>
            </li>
          {/each}
        </ul>
      {/if}

      {#if tokenNotice}
        <p class="notice-text" data-testid="token-notice">{tokenNotice}</p>
      {/if}
      {#if tokenError}
        <p class="error-text" role="alert">{tokenError}</p>
      {/if}
    </section>

    <section class="settings-section" data-testid="devices-summary">
      <h2 class="settings-section__title">{$t('web.settings.devices.title')}</h2>
      <Button variant="secondary" on:click={() => navigateTo('settings')}>
        {$t('web.settings.devices.manage')}
      </Button>
    </section>
  {/if}

  {#if rotating}
    <div class="modal-backdrop" role="dialog" aria-modal="true" data-testid="rotate-modal">
      <div class="modal-card">
        <h2 class="modal-title">
          {$t(
            rotatingMode === 'replace'
              ? 'web.settings.credentials.replace_title'
              : 'web.settings.credentials.rotate_title',
            { key: rotating.key },
          )}
        </h2>
        <dl class="rotate-details">
          <div class="rotate-details__row">
            <dt>
              {$t('web.settings.credentials.entry', {
                key: rotating.key,
                provider: rotating.provider,
              })}
            </dt>
            <dd>
              {$t(
                rotatingMode === 'replace'
                  ? 'web.settings.credentials.replace_explain'
                  : 'web.settings.credentials.rotate_explain',
              )}
            </dd>
          </div>
          {#if rotatingMode === 'replace'}
            <div class="rotate-details__row">
              <dt>
                <label class="rotate-label" for="replace-kind">
                  {$t('web.settings.credentials.kind')}
                </label>
              </dt>
              <dd>
                <input
                  id="replace-kind"
                  type="text"
                  class="rotate-input"
                  bind:value={rotatingKind}
                  data-testid="replace-kind"
                />
              </dd>
            </div>
          {:else}
            <div class="rotate-details__row">
              <dt>{$t('web.settings.credentials.kind')}</dt>
              <dd>{rotating.kind}</dd>
            </div>
          {/if}
          <div class="rotate-details__row">
            <dt>
              {$t('web.settings.credentials.now', {
                kind: rotating.kind,
                expiry: expiryLabel(rotating),
              })}
            </dt>
            <dd></dd>
          </div>
        </dl>

        <label class="rotate-label" for="rotate-secret">
          {$t('web.settings.credentials.secret')}
        </label>
        <input
          id="rotate-secret"
          type="password"
          class="rotate-input"
          autocomplete="off"
          bind:value={rotatingSecret}
          data-testid="rotate-secret"
        />
        <p class="hint-text">{$t('web.settings.credentials.secret_hint')}</p>
        <p class="hint-text">
          {$t('web.settings.credentials.lan', {
            command: `pm-operator credentials rotate ${rotating.key}`,
          })}
        </p>

        <div class="modal-actions">
          <Button variant="ghost" on:click={closeRotate} data-testid="rotate-cancel">
            {$t('web.settings.back')}
          </Button>
          <Button
            variant="primary"
            disabled={!rotatingSecret}
            loading={isSavingToken}
            on:click={saveToken}
            data-testid="rotate-save"
          >
            {$t('web.settings.credentials.store')}
          </Button>
        </div>
      </div>
    </div>
  {/if}
</div>

<style>
  .machine-settings {
    max-width: 60rem;
    margin: 0 auto;
    padding: var(--space-8) var(--space-4);
  }

  .machine-settings__header {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
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
    flex-wrap: wrap;
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
  .consent-row__where,
  .credential-row__main {
    flex: 1 1 14rem;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .credential-row__expiry {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
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

  .choice input[type='number'] {
    width: 5rem;
    padding: var(--space-1) var(--space-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
  }

  .hint-text {
    margin-top: var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .warn {
    color: var(--color-warning);
  }

  .notice-text {
    color: var(--color-primary);
    font-size: var(--text-sm);
    margin-top: var(--space-2);
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

  .conflict {
    padding: var(--space-4);
    margin-bottom: var(--space-6);
    background-color: var(--color-warning-subtle);
    border: 1px solid var(--color-warning-border);
    border-radius: var(--radius-md);
  }

  .conflict__title {
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-warning);
    margin-bottom: var(--space-2);
  }

  .conflict__line {
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .conflict__actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: var(--space-3);
    margin-top: var(--space-3);
  }

  .modal-backdrop {
    position: fixed;
    inset: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: var(--space-4);
    z-index: 100;
  }

  .modal-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-6);
    max-width: 34rem;
    width: 100%;
    max-height: 90vh;
    overflow-y: auto;
    box-shadow: var(--shadow-md);
  }

  .modal-title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: var(--space-4);
  }

  .rotate-details {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin-bottom: var(--space-4);
  }

  .rotate-details__row {
    display: grid;
    grid-template-columns: 1fr;
    gap: var(--space-1);
    font-size: var(--text-sm);
  }

  .rotate-details__row dt {
    color: var(--color-text-secondary);
    font-weight: 600;
  }

  .rotate-details__row dd {
    color: var(--color-text-primary);
  }

  .rotate-label {
    display: block;
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-secondary);
    margin-bottom: var(--space-1);
  }

  .rotate-input {
    width: 100%;
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .modal-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: var(--space-3);
    margin-top: var(--space-4);
  }

  @media (max-width: 768px) {
    .machine-settings {
      padding: var(--space-4) var(--space-3);
    }

    .aspect-row__name {
      min-width: 0;
    }

    .conflict__actions,
    .modal-actions {
      flex-direction: column-reverse;
      align-items: stretch;
    }
  }
</style>
