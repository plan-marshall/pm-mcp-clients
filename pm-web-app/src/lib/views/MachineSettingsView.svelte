<script lang="ts">
  import { onMount } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import { formatTime } from '../display/format';
  import Button from '../components/Button.svelte';
  import {
    runtimeStore,
    navigateTo,
    loadMachineSettings,
    updateMachineSetting,
    withdrawConsent,
    storeCredential,
    openOnboardingStep,
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
    /** The cleanup categories the operator changed, which applying again sends over the current record. */
    changedFields?: CleanupDurationsRecord;
  }

  /** The cleanup durations as one record of days, `-1` for a category that is never offered. */
  type CleanupDurationsRecord = Record<string, number>;

  /** The record's keys in the design's order; the record itself decides which are shown. */
  const CLEANUP_CATEGORY_ORDER: ReadonlyArray<string> = [
    'plans_after_days',
    'epics_after_days',
    'logs_after_days',
    'reports_after_days',
    'unenrolled_after_days',
    'remind_after_days',
  ];

  let isLoading = false;
  let notice = '';
  let error = '';
  /** The retention minimum of `doc/design/shared/cleanup.adoc`, refused before it is sent. */
  const MINIMUM_RETENTION_DAYS = 7;
  /** The reminder minimum of the same contract. */
  const MINIMUM_REMIND_DAYS = 1;
  /** The value a retention category carries when its items are never offered. */
  const NEVER = -1;
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
   * Reads the cleanup durations a setting value carries.
   *
   * The setting travels as one record of days; an older client sends a bare number of days, which
   * is not a record and is shown as the value it is.
   *
   * @param value the setting value
   * @returns the record, or null for a value that is not one
   */
  function parseCleanupRecord(value: string): CleanupDurationsRecord | null {
    try {
      const parsed: unknown = JSON.parse(value);
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return null;
      const record: CleanupDurationsRecord = {};
      for (const [key, days] of Object.entries(parsed)) {
        if (typeof days === 'number' && Number.isFinite(days)) record[key] = days;
      }
      return record;
    } catch {
      return null;
    }
  }

  /**
   * Renders a cleanup record in the catalogue's words, one category each.
   *
   * A conflict compares two records; printing them as JSON shows the operator braces and keys
   * instead of the categories and days they chose. The record itself names the categories, so a
   * field added to it is shown rather than skipped.
   *
   * @param value the setting value
   * @returns the categories in words, or the value itself when it is not a record
   */
  function cleanupWords(value: string): string {
    const record = parseCleanupRecord(value);
    if (!record) return value;
    const known = CLEANUP_CATEGORY_ORDER;
    const keys = [
      ...known.filter((key) => record[key] !== undefined),
      ...Object.keys(record).filter((key) => !known.includes(key)),
    ];
    const words: string[] = [];
    for (const key of keys) {
      const days = record[key];
      if (days === undefined) continue;
      const kind = key.replace(/_after_days$/, '');
      if (kind === 'remind') {
        // Its label ends in "after": the sentence carries the case the bare noun would not.
        words.push($t('system.cleanup.durations.remind_at', { days }));
        continue;
      }
      const label = $t('system.cleanup.durations.kind', { kind });
      const amount = $t('system.cleanup.durations.value', {
        never: days === NEVER ? 'true' : 'false',
        days,
      });
      words.push(`${label}: ${amount}`);
    }
    return words.length > 0 ? words.join(' · ') : value;
  }

  /** @param value a cleanup record @returns the categories whose value differs from another record */
  function changedCleanupFields(
    value: CleanupDurationsRecord,
    shown: CleanupDurationsRecord | undefined,
  ): CleanupDurationsRecord {
    const changed: CleanupDurationsRecord = {};
    for (const [key, days] of Object.entries(value)) {
      if (shown?.[key] !== days) changed[key] = days;
    }
    return changed;
  }

  /**
   * Applies one setting, and opens the conflict dialogue instead of overwriting a value that
   * changed in another client.
   *
   * @param key the setting
   * @param value its new value, or null for a withdrawal
   * @param changedFields the cleanup categories the operator changed, if the value is a record
   */
  async function applySetting(
    key: string,
    value: string | null,
    changedFields?: CleanupDurationsRecord,
  ): Promise<boolean> {
    error = '';
    notice = '';
    conflict = null;
    const result = await updateMachineSetting(key, value);
    if (result?.conflict) {
      conflict = {
        key,
        currentValue: result.current_value ?? '',
        wantedValue: value ?? '',
        changedFields,
      };
      return false;
    }
    if (!result) {
      error = $t('web.settings.update_failed');
      return false;
    }
    notice = $t('web.settings.saved');
    return true;
  }

  async function saveDocumentsMode(): Promise<void> {
    await applySetting('documents_mode', documentsMode);
  }

  /**
   * Sends the cleanup durations the page shows: how long work is kept, and the reminder interval.
   *
   * The setting travels as one record, so both figures go in it; the categories the page does not
   * show keep the values the runtime holds, so saving the visible ones never resets them. A value
   * outside the contract (`doc/design/shared/cleanup.adoc`: at least seven days, a reminder of at
   * least one) writes nothing rather than a default the runtime would fill in.
   */
  async function saveCleanup(): Promise<void> {
    const days = Number(cleanupDays);
    const remind = Number(cleanupRemindDays);
    if (!Number.isInteger(days) || (days !== NEVER && days < MINIMUM_RETENTION_DAYS)) {
      // The contract's own sentence, with its minimum: a generic failure hides what to change.
      error = $t('system.cleanup.durations.minimum', {
        min: MINIMUM_RETENTION_DAYS,
        kind: 'plans',
      });
      notice = '';
      return;
    }
    if (!Number.isInteger(remind) || remind < MINIMUM_REMIND_DAYS) {
      error = $t('system.cleanup.durations.minimum', {
        min: MINIMUM_REMIND_DAYS,
        kind: 'remind',
      });
      notice = '';
      return;
    }
    const recorded = settings?.cleanup_durations;
    const durations = {
      plans_after_days: days,
      epics_after_days: recorded?.epics_after_days ?? days,
      logs_after_days: recorded?.logs_after_days ?? days,
      reports_after_days: recorded?.reports_after_days ?? days,
      unenrolled_after_days: recorded?.unenrolled_after_days ?? days,
      remind_after_days: remind,
    };
    // Only what the operator changed travels again after a conflict; the other categories keep
    // the value the other client recorded.
    await applySetting(
      'cleanup_durations',
      JSON.stringify(durations),
      changedCleanupFields(durations, recorded),
    );
  }

  /** Applies the value this page wanted, on the revision the other client left behind. */
  async function applyConflictAgain(): Promise<void> {
    if (!conflict) return;
    const key = conflict.key;
    const changed = conflict.changedFields;
    let wanted = conflict.wantedValue;
    if (key === 'cleanup_durations' && changed) {
      const current = parseCleanupRecord(conflict.currentValue) ?? {};
      wanted = JSON.stringify({ ...current, ...changed });
    }
    conflict = null;
    // A second conflict keeps the changed set: the categories the operator did not touch must
    // follow the record the other client left again, not the one this resend was built from.
    const applied = await applySetting(key, wanted, changed);
    if (applied) {
      // The fields show the record the runtime now holds, so the next save does not resend a value
      // this resend deliberately kept from the other client.
      applyLocal($runtimeStore.machineSettings ?? null);
    }
  }

  /** Keeps the other client's value and closes the dialogue, without writing anything. */
  async function keepCurrentValue(): Promise<void> {
    conflict = null;
    const reloaded = await loadMachineSettings();
    if (!reloaded) {
      error = $t('web.settings.update_failed');
      return;
    }
    applyLocal(reloaded);
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
    if (!result) {
      error = $t('web.settings.update_failed');
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
      if (result.kindFailed || result.refreshFailed) {
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

  /**
   * The place a consent was given in.
   *
   * The runtime names the console `terminal`; a device carries the label the operator gave it, so
   * only that value is a name of its own and the console token reads as the catalogue's word.
   *
   * @param consent the remembered consent
   * @returns the word or the device label
   */
  function consentChannel(consent: { channel?: string | null }): string {
    if (!consent.channel || consent.channel === 'terminal') {
      return $t('web.settings.consents.where_terminal');
    }
    return consent.channel;
  }
</script>

<div class="machine-settings" data-testid="machine-settings-view">
  <div class="machine-settings__header">
    <h1 class="machine-settings__title">{$t('web.settings.page.title')}</h1>
    <div class="machine-settings__header-actions">
      <Button
        variant="secondary"
        on:click={() => navigateTo('designer')}
        data-testid="open-designer-btn"
      >
        {$t('designer.view.title')}
      </Button>
      <Button variant="secondary" on:click={() => navigateTo('settings')}>
        {$t('web.settings.back')}
      </Button>
    </div>
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
          {$t('web.settings.conflict.current', { value: cleanupWords(conflict.currentValue) })}
        </p>
        <p class="conflict__line">
          {$t('web.settings.conflict.yours', { value: cleanupWords(conflict.wantedValue) })}
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
              on:click={() => openOnboardingStep(aspect.step)}
              aria-label={`${$t('web.settings.aspects.change')} ${stepName(aspect.aspect)}`}
              data-testid={`change-aspect-${aspect.aspect}`}
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
                {formatTime(consent.granted_at, 'short', $currentLocale)} · {consentChannel(consent)}
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
          {$t('web.settings.cleanup.keep')}
          <input
            type="number"
            bind:value={cleanupDays}
            data-testid="cleanup-days"
            aria-label={$t('web.settings.cleanup.title')}
          />
          {$t('web.settings.cleanup.days')}
        </label>
        <label class="choice">
          {$t('web.settings.cleanup.remind')}
          <input type="number" min="1" bind:value={cleanupRemindDays} data-testid="cleanup-remind-days" />
          {$t('web.settings.cleanup.days_after')}
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

  .machine-settings__header-actions {
    display: flex;
    align-items: center;
    gap: var(--space-2);
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
    background-color: var(--color-overlay);
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
