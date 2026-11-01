<script lang="ts">
  import { t, currentLocale } from '../i18n';
  import { formatTime } from '../display/format';
  import type { HoldRecord, HoldResolutionResult } from '../api/client';

  export let hold: HoldRecord | null = null;
  export let open = false;
  export let busy = false;
  /** The project whose access a grant names. */
  export let project = '';
  /** What the last act did: released the subject, or showed what still holds. */
  export let outcome: HoldResolutionResult | null = null;
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

  /** The pull request number of a `show_pr` act, read from its URL. */
  function pullRequestNumber(): number {
    const match = /\/(\d+)(?:$|\/)/.exec(hold?.pull_request_url ?? '');
    return match ? Number.parseInt(match[1], 10) : 0;
  }

  function actLabel(act: string): string {
    const key = ACT_KEYS[act] ?? act;
    if (act === 'grant') {
      return $t(key, { project: project || hold?.subject_id || '' });
    }
    if (act === 'show_pr') {
      return $t(key, { number: pullRequestNumber() });
    }
    return $t(key);
  }

  /** What an act that cannot be undone does, before the operator confirms it. */
  function explanation(act: string): string {
    if (act === 'accept') {
      return $t('control.hold.integrity.no_restore');
    }
    if (act === 'give_up_removal') {
      return $t('control.hold.give_up_removal.explain');
    }
    if (act === 'cancel_epic') {
      return $t('control.cancel_epic.afterwards');
    }
    return $t('control.end.afterwards', {
      outcome: act === 'abort' ? 'aborted' : 'cancelled',
    });
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
      const text = reason.trim();
      reason = '';
      onResolve(text ? { act, reason: text } : { act });
    }
  }

  function cancelConfirm(): void {
    confirming = null;
    reason = '';
  }

  function close(): void {
    confirming = null;
    reason = '';
    onClose();
  }
</script>

{#if open && hold}
  <div class="hold-modal" data-testid="hold-modal" role="dialog" aria-modal="true">
    <div class="hold-modal__panel">
      <h2 class="hold-modal__title" data-testid="hold-title">
        {$t('control.hold.title', { kind: hold.kind, name: hold.subject_id })}
      </h2>

      {#if outcome}
        <p
          class="hold-modal__outcome"
          data-testid={outcome.released ? 'hold-released' : 'hold-still-held'}
          role="status"
        >
          {#if outcome.released}
            {$t('control.hold.released', { subject: outcome.subject || hold.subject_id })}
          {:else}
            {$t('control.hold.still_held', { cause: outcome.cause || hold.cause })}
          {/if}
        </p>
      {/if}

      <dl class="hold-modal__facts">
        <dt>{$t('control.hold.cause')}</dt>
        <dd data-testid="hold-cause">{hold.cause}</dd>

        {#if hold.last_tried_at}
          <dt></dt>
          <dd data-testid="hold-tried">
            {$t('control.hold.tried', {
              count: hold.attempts,
              time: formatTime(hold.last_tried_at, 'short', $currentLocale),
            })}
          </dd>
        {/if}

        {#if hold.file_path}
          <dt>{$t('control.hold.integrity.file')}</dt>
          <dd data-testid="hold-file">{hold.file_path}</dd>
          <dt></dt>
          <dd data-testid="hold-recorded">
            {$t('control.hold.integrity.recorded', {
              size: hold.recorded_size ?? 0,
              digest: hold.recorded_digest ?? '',
            })}
          </dd>
          <dt></dt>
          <dd data-testid="hold-current">
            {$t('control.hold.integrity.current', {
              size: hold.current_size ?? 0,
              digest: hold.current_digest ?? '',
            })}
          </dd>
        {/if}
      </dl>

      {#if confirming}
        <div class="hold-modal__confirm" data-testid="hold-confirm">
          <p class="hold-modal__explain">{explanation(confirming)}</p>
          <label class="hold-modal__field">
            {$t('control.hold.reason')}
            <input type="text" data-testid="hold-reason" bind:value={reason} />
          </label>
          <div class="hold-modal__actions">
            <button
              type="button"
              data-testid="hold-confirm-button"
              on:click={confirmAct}
              disabled={busy}
            >
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
          <button type="button" data-testid="hold-close-button" on:click={close}>
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
    background-color: var(--color-overlay);
    z-index: 50;
  }

  .hold-modal__panel {
    max-width: 32rem;
    width: 100%;
    margin: var(--space-3);
    padding: var(--space-4);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-surface);
    max-height: 90vh;
    overflow-y: auto;
  }

  .hold-modal__outcome {
    margin: var(--space-2) 0;
    padding: var(--space-2);
    border-left: 3px solid var(--color-primary);
    background-color: var(--color-bg-subtle);
    font-size: var(--text-sm);
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

  .hold-modal__explain {
    margin: 0 0 var(--space-2) 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .hold-modal__field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    margin-bottom: var(--space-2);
  }

  .hold-modal__field input {
    min-height: 44px;
    padding: var(--space-1) var(--space-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
  }

  @media (max-width: 480px) {
    .hold-modal__panel {
      margin: 0;
      border-radius: 0;
      min-height: 100%;
    }
  }
</style>
