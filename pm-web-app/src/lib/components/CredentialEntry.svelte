<script lang="ts">
  import { t } from '../i18n';
  import Button from './Button.svelte';
  import { apiClient } from '../api/client';

  export let credentialKey: string;
  export let label: string;

  const loopbackHosts = ['127.0.0.1', 'localhost', '::1', '[::1]'];

  let secret = '';
  let isSaving = false;
  let notice = '';
  let error = '';

  $: isLoopback = typeof window !== 'undefined' && loopbackHosts.includes(window.location.hostname);
  $: command = `pm-operator credentials set ${credentialKey}`;

  async function save(): Promise<void> {
    if (!secret || isSaving) return;
    isSaving = true;
    notice = '';
    error = '';
    try {
      await apiClient.setCredential(credentialKey, secret);
      secret = '';
      notice = $t('entry.credentials.stored');
    } catch (err) {
      const message = (err as Error)?.message;
      if (message === 'loopback_only') {
        error = $t('entry.credentials.loopback_only', { command });
      } else {
        error = message || $t('entry.credentials.save');
      }
    } finally {
      isSaving = false;
    }
  }
</script>

<section class="credential-entry" data-testid="credential-entry">
  <div class="credential-entry__header">
    <h3 class="credential-entry__title">{label}</h3>
    <span class="credential-entry__key mono">{credentialKey}</span>
  </div>

  {#if !isLoopback}
    <div
      class="credential-entry__loopback-warning"
      role="alert"
      data-testid="loopback-only-warning"
    >
      <strong>{$t('entry.credentials.loopback_only_title')}</strong>
      <p>
        {$t('entry.credentials.loopback_only', { command })}
      </p>
    </div>
  {:else}
    <form
      class="credential-entry__form"
      on:submit|preventDefault={save}
      data-testid="credential-form"
    >
      <label class="credential-entry__label" for={`credential-${credentialKey}`}>
        {$t('entry.credentials.field')}
      </label>
      <input
        id={`credential-${credentialKey}`}
        type="password"
        class="credential-entry__input"
        autocomplete="off"
        bind:value={secret}
        data-testid="credential-secret-input"
      />
      <Button type="submit" variant="primary" loading={isSaving} disabled={!secret}>
        {$t('entry.credentials.save')}
      </Button>
    </form>
  {/if}

  {#if notice}
    <p class="credential-entry__notice" data-testid="credential-notice">{notice}</p>
  {/if}
  {#if error}
    <p class="credential-entry__error" role="alert" data-testid="credential-error">{error}</p>
  {/if}
</section>

<style>
  .credential-entry {
    padding: var(--space-4);
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
  }

  .credential-entry__header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--space-2);
    margin-bottom: var(--space-3);
  }

  .credential-entry__title {
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0;
  }

  .credential-entry__key {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .credential-entry__form {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    gap: var(--space-3);
  }

  .credential-entry__label {
    flex-basis: 100%;
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .credential-entry__input {
    flex: 1 1 220px;
    padding: var(--space-2) var(--space-3);
    font-size: var(--text-sm);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .credential-entry__loopback-warning {
    font-size: var(--text-sm);
    color: var(--color-warning);
  }

  .credential-entry__notice {
    font-size: var(--text-sm);
    color: var(--color-primary);
    margin: var(--space-3) 0 0 0;
  }

  .credential-entry__error {
    font-size: var(--text-sm);
    color: var(--color-danger);
    margin: var(--space-3) 0 0 0;
  }
</style>
