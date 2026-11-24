<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';
  import Button from './Button.svelte';
  import Dialog from './Dialog.svelte';
  import ConfirmDialog from './ConfirmDialog.svelte';
  import { isLoopbackSession } from '../utils/loopback';
  import { apiClient, type ConnectionStatus } from '../api/client';

  /**
   * The network listener of this machine (`settings` § the network access): its state, the
   * certificate fingerprint to compare with the terminal's, and the two answers. It is the design
   * `Dialog`: _Keep it open_ is the ghost act that leaves without acting, _Turn the network access
   * off_ the secondary one. Turning it off loses nothing — the listener is turned on again from the
   * machine's settings — so the act is no `ConfirmDialog` and carries no danger colour.
   */
  export let onClose: () => void = () => {};
  export let onChanged: () => void = () => {};

  let status: ConnectionStatus | null = null;
  let notice = '';
  let error = '';
  let isMounted = true;
  let confirmingTurnOff = false;

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

  function handleTurnOff(): void {
    if (isLoopbackSession()) {
      void turnOff();
    } else {
      confirmingTurnOff = true;
    }
  }
</script>

<Dialog open={true} title={$t('entry.lan.title')} {onClose} testid="lan-listener-modal">
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

  <svelte:fragment slot="footer">
    <Button variant="ghost" data-testid="lan-keep-button" on:click={onClose}>
      {$t('entry.lan.keep')}
    </Button>
    {#if status?.lan_enabled}
      <Button variant="danger" data-testid="lan-turn-off-button" on:click={handleTurnOff}>
        {$t('entry.lan.turn_off')}
      </Button>
    {/if}
  </svelte:fragment>
</Dialog>

<ConfirmDialog
  open={confirmingTurnOff}
  title={$t('devices.listener_off.confirm_title')}
  consequences={[$t('devices.listener_off.confirm_explain')]}
  actionLabel={$t('devices.act.listener_off')}
  cancelLabel={$t('devices.revoke.keep')}
  onConfirm={() => {
    confirmingTurnOff = false;
    void turnOff();
  }}
  onClose={() => {
    confirmingTurnOff = false;
  }}
/>

<style>
  .modal-state {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    margin: 0;
  }

  .fingerprint {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
  }

  .fingerprint__label {
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .fingerprint__value {
    font-family: var(--font-mono);
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
    color: var(--color-brand);
    margin: 0;
  }

  .error-text {
    font-size: var(--text-sm);
    color: var(--color-danger);
    margin: 0;
  }
</style>
