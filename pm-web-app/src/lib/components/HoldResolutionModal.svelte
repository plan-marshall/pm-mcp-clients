<script lang="ts">
  import { t } from '../i18n';
  import type { HoldRecord } from '../api/client';

  export let hold: HoldRecord;
  export let open = false;
  export let busy = false;
  export let onResolve: (payload: { act: string; reason?: string }) => void = () => {};
  export let onClose: () => void = () => {};

  const ACT_KEYS: Record<string, string> = {
    retry: 'control.hold.act.retry',
    recycle: 'control.hold.act.recycle',
    grant: 'control.hold.act.grant',
    add_credential: 'control.hold.act.add_credential',
    raise_budget: 'control.hold.act.raise_budget',
    accept: 'control.hold.act.accept_change',
    show_pr: 'control.hold.act.show_pr',
    give_up_removal: 'control.hold.act.give_up_removal',
    end_plan: 'control.hold.act.end_plan',
    abort: 'control.hold.act.abort_plan',
    cancel_epic: 'control.hold.act.cancel_epic',
  };

  const IRREVERSIBLE = new Set(['accept', 'end_plan', 'abort', 'cancel_epic', 'give_up_removal']);

  let confirming: string | null = null;
  let reason = '';

  function actLabel(act: string): string {
    return $t(ACT_KEYS[act] ?? act);
  }

  function startAct(act: string): void {
    if (IRREVERSIBLE.has(act)) {
      confirming = act;
      return;
    }
    onResolve({ act });
  }

  function confirmAct(): void {
    if (confirming) {
      const act = confirming;
      confirming = null;
      onResolve(reason ? { act, reason } : { act });
    }
  }

  function cancelConfirm(): void {
    confirming = null;
    reason = '';
  }
</script>

{#if open}
  <div class="hold-modal" data-testid="hold-modal" role="dialog" aria-modal="true">
    <div class="hold-modal__panel">
      <h2 class="hold-modal__title" data-testid="hold-title">
        {$t('control.hold.title', { kind: hold.kind, name: hold.subject_id })}
      </h2>

      <dl class="hold-modal__facts">
        <dt>{$t('control.hold.cause')}</dt>
        <dd data-testid="hold-cause">{hold.cause}</dd>

        {#if hold.last_tried_at}
          <dt></dt>
          <dd data-testid="hold-tried">
            {$t('control.hold.tried', { count: hold.attempts, time: hold.last_tried_at })}
          </dd>
        {/if}

        {#if hold.file_path}
          <dt>{$t('control.hold.integrity.file')}</dt>
          <dd data-testid="hold-file">{hold.file_path}</dd>
          <dt></dt>
          <dd data-testid="hold-recorded">
            {$t('control.hold.integrity.recorded', {
              size: hold.recorded_size,
              digest: hold.recorded_digest,
            })}
          </dd>
          <dt></dt>
          <dd data-testid="hold-current">
            {$t('control.hold.integrity.current', {
              size: hold.current_size,
              digest: hold.current_digest,
            })}
          </dd>
        {/if}
      </dl>

      {#if confirming}
        <div class="hold-modal__confirm" data-testid="hold-confirm">
          <label>
            {$t('control.hold.cause')}
            <input
              type="text"
              data-testid="hold-reason"
              bind:value={reason}
              placeholder=""
            />
          </label>
          <div class="hold-modal__actions">
            <button type="button" data-testid="hold-confirm-button" on:click={confirmAct} disabled={busy}>
              {actLabel(confirming)}
            </button>
            <button type="button" data-testid="hold-cancel-button" on:click={cancelConfirm}>
              {$t('common.back')}
            </button>
          </div>
        </div>
      {:else}
        <div class="hold-modal__actions">
          {#each hold.acts as act (act)}
            <button
              type="button"
              class="hold-modal__act"
              data-testid={`hold-act-${act}`}
              on:click={() => startAct(act)}
              disabled={busy}
            >
              {actLabel(act)}
            </button>
          {/each}
          <button type="button" data-testid="hold-close-button" on:click={() => onClose()}>
            {$t('common.close')}
          </button>
        </div>
      {/if}
    </div>
  </div>
{/if}

<style>
  .hold-modal {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgb(0 0 0 / 0.4);
    z-index: 50;
  }

  .hold-modal__panel {
    max-width: 32rem;
    width: 100%;
    margin: var(--space-3);
    padding: var(--space-4);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-surface);
  }

  .hold-modal__facts {
    display: grid;
    grid-template-columns: max-content 1fr;
    gap: var(--space-1) var(--space-3);
    margin: var(--space-3) 0;
  }

  .hold-modal__actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .hold-modal__act,
  .hold-modal__actions button {
    min-height: 44px;
    padding: var(--space-2) var(--space-3);
    cursor: pointer;
  }

  @media (max-width: 480px) {
    .hold-modal__panel {
      margin: 0;
      border-radius: 0;
      min-height: 100%;
    }
  }
</style>
