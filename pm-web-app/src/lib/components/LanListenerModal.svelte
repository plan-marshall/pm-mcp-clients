<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';
  import Button from './Button.svelte';
  import { apiClient, type ConnectionStatus } from '../api/client';

  export let onClose: () => void = () => {};
  export let onChanged: () => void = () => {};

  let status: ConnectionStatus | null = null;
  let notice = '';
  let error = '';
  let isMounted = true;

  onMount(() => {
    isMounted = true;
    void load();
    return () => {
      isMounted = false;
    };
  });

  async function load(): Promise<void> {
    try {
      const next = await apiClient.getConnectionStatus();
      if (isMounted) status = next;
    } catch {
      if (isMounted) status = null;
    }
  }

  async function turnOff(): Promise<void> {
    error = '';
    try {
      await apiClient.turnOffLanListener();
      notice = $t('entry.lan.off_done');
      await load();
      onChanged();
    } catch {
      error = $t('entry.lan.turn_off');
    }
  }
</script>

<div
  class="modal-backdrop"
  role="dialog"
  aria-modal="true"
  aria-label={$t('entry.lan.title')}
  data-testid="lan-listener-modal"
>
  <div class="modal-card">
    <h3 class="modal-title">{$t('entry.lan.title')}</h3>
    <p class="modal-state" data-testid="lan-listener-state">
      {$t('entry.lan.state', {
        state: status?.lan_enabled ? $t('entry.lan.state_on') : $t('entry.lan.state_off'),
      })}
    </p>

    {#if status?.lan_enabled && status.lan_fingerprint}
      <div class="fingerprint" data-testid="lan-fingerprint">
        <span class="fingerprint__label">{$t('entry.lan.fingerprint')}</span>
        <code class="fingerprint__value">{status.lan_fingerprint}</code>
        <span class="fingerprint__hint">{$t('entry.lan.fingerprint_hint')}</span>
      </div>
    {/if}

    {#if notice}
      <p class="notice-text" data-testid="lan-listener-notice">{notice}</p>
    {/if}
    {#if error}
      <p class="error-text" role="alert">{error}</p>
    {/if}

    <div class="modal-actions">
      <Button variant="ghost" on:click={onClose}>{$t('entry.lan.keep')}</Button>
      {#if status?.lan_enabled}
        <Button variant="danger" on:click={turnOff} data-testid="lan-turn-off-button">
          {$t('entry.lan.turn_off')}
        </Button>
      {/if}
    </div>
  </div>
</div>

<style>
  .modal-backdrop {
    position: fixed;
    inset: 0;
    background-color: var(--color-overlay);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 100;
  }

  .modal-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-6);
    max-width: 520px;
    width: 90%;
    box-shadow: var(--shadow-md);
  }

  .modal-title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0 0 var(--space-2) 0;
  }

  .modal-state {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    margin: 0 0 var(--space-4) 0;
  }

  .fingerprint {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding: var(--space-3) var(--space-4);
    margin-bottom: var(--space-4);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
  }

  .fingerprint__label {
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .fingerprint__value {
    font-family: var(--font-mono, monospace);
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    overflow-wrap: anywhere;
  }

  .fingerprint__hint {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .notice-text {
    font-size: var(--text-sm);
    color: var(--color-primary);
    margin: 0 0 var(--space-3) 0;
  }

  .error-text {
    font-size: var(--text-sm);
    color: var(--color-danger);
    margin: 0 0 var(--space-3) 0;
  }

  .modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-3);
  }
</style>
