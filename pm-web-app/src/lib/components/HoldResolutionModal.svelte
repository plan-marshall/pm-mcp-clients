<script lang="ts">
  import { t, currentLocale } from '../i18n';
  import { formatTime } from '../display/format';
  import type { HoldRecord, HoldResolutionResult } from '../api/client';
  import Dialog from './Dialog.svelte';
  import ConfirmDialog from './ConfirmDialog.svelte';
  import Button from './Button.svelte';
  import TextField from './TextField.svelte';
  import KeyValueList, { type KeyValueItem } from './KeyValueList.svelte';

  /**
   * The dialogue of a hold (work-actions.adoc § #holds): its title, the cause and the facts the
   * runtime reports, and the acts it offers. It is the design `Dialog`; the acts are secondary by
   * the hierarchy and leaving without acting is its ghost act, and an act that cannot be undone
   * opens the act's own `ConfirmDialog` with the operator's reason as a `TextField`.
   */
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

  $: title = hold === null ? '' : $t('control.hold.title', { kind: hold.kind, name: hold.subject_id });

  /** The figures of the hold, as the one list of figures: the cause, the attempts, the tampered file. */
  $: facts = ((): KeyValueItem[] => {
    if (hold === null) {
      return [];
    }
    const items: KeyValueItem[] = [{ label: $t('control.hold.cause'), value: hold.cause }];
    if (hold.last_tried_at) {
      items.push({
        label: '',
        value: $t('control.hold.tried', {
          count: hold.attempts,
          time: formatTime(hold.last_tried_at, 'short', $currentLocale),
        }),
      });
    }
    if (hold.file_path) {
      items.push(
        { label: $t('control.hold.integrity.file'), value: hold.file_path },
        {
          label: '',
          value: $t('control.hold.integrity.recorded', {
            size: hold.recorded_size ?? 0,
            digest: hold.recorded_digest ?? '',
          }),
        },
        {
          label: '',
          value: $t('control.hold.integrity.current', {
            size: hold.current_size ?? 0,
            digest: hold.current_digest ?? '',
          }),
        },
      );
    }
    return items;
  })();
</script>

{#if hold !== null}
  <Dialog open={open} {title} onClose={close} testid="hold-modal">
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

    <KeyValueList items={facts} />

    <div class="hold-modal__acts">
      {#each hold.acts as act (act)}
        <Button
          variant="secondary"
          data-testid={`hold-act-${act}`}
          disabled={busy}
          on:click={() => startAct(act)}
        >
          {actLabel(act)}
        </Button>
      {/each}
      <Button variant="ghost" data-testid="hold-close-button" on:click={close}>
        {$t('common.action.close')}
      </Button>
    </div>
  </Dialog>
{/if}

{#if confirming}
  <ConfirmDialog
    open={true}
    title={actLabel(confirming)}
    consequences={[explanation(confirming)]}
    actionLabel={actLabel(confirming)}
    cancelLabel={$t('common.action.back')}
    {busy}
    testid="hold-confirm"
    onConfirm={confirmAct}
    onClose={cancelConfirm}
  >
    <TextField
      label={$t('control.hold.reason')}
      testid="hold-reason"
      bind:value={reason}
      autocomplete="off"
    />
  </ConfirmDialog>
{/if}

<style>
  .hold-modal__outcome {
    margin: 0;
    padding: var(--space-2);
    border-left: 3px solid var(--color-brand);
    background-color: var(--color-bg-subtle);
    font-size: var(--text-sm);
  }

  .hold-modal__acts {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: var(--space-2);
  }
</style>
