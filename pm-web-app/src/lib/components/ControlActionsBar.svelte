<script lang="ts">
  import { tick } from 'svelte';
  import { t } from '../i18n';
  import type { ArchiveCheck, WorkActions } from '../api/client';
  import Icon from './Icon.svelte';
  import { uniqueId } from '../utils/uniqueId';

  export let subjectId: string;
  export let subjectKind: 'plan' | 'epic' = 'plan';
  export let actions: WorkActions | null = null;
  export let archiveCheck: ArchiveCheck | null = null;
  export let busy = false;
  /** The epic's scope and how many of it run, for the dialogue that raises it. */
  export let scope = 0;
  export let running = 0;
  /** The plan the epic would start next, named by *Next plan*. */
  export let nextPlan = '';
  /** The plan's own pull request, where the runtime reports one. */
  export let pullRequestNumber: number | null = null;
  /** The epic's automatic mode, where its view reports it apart from the actions. */
  export let autoEnabled: boolean | null = null;
  export let onAction: (action: string, payload?: Record<string, unknown>) => void = () => {};

  type Dialog = 'pause' | 'end' | 'raise' | 'overlap' | 'archive' | null;

  let dialog: Dialog = null;
  let pausePoint = 'task';
  let endAction: 'cancel' | 'abandon' | 'abort' = 'cancel';
  let endReason = '';
  let closePullRequest = false;
  let overlapCount = 0;

  /** The control that opened the dialog, so the focus goes back to it when the dialog closes. */
  let opener: HTMLElement | null = null;

  /** The open dialog's box, which takes the focus when it opens. */
  let dialogElement: HTMLElement | null = null;

  /** The id of the open dialog's title, for `aria-labelledby`. */
  const titleId = uniqueId('control-dialog-title');

  $: automatic = autoEnabled ?? actions?.auto_enabled !== false;
  $: commonFiles = actions?.overlap_max_files ?? 0;

  /** The label of one action, by the subject's kind. */
  function label(action: string): string {
    if (subjectKind === 'epic') {
      switch (action) {
        case 'next':
          return nextPlan
            ? $t('control.epic.next', { plan: nextPlan })
            : $t('control.epic.next_any');
        case 'auto':
          return $t('control.epic.auto', {
            mode: automatic ? 'on' : 'off',
          });
        case 'common_files':
          return $t('control.epic.overlap', { count: commonFiles });
        case 'findings':
          return $t('control.epic.findings_review');
        case 'watches':
          return $t('control.epic.watches_review');
        case 'decompose':
          return $t('control.epic.decompose_again');
        case 'archive':
          return $t('control.epic.archive');
        case 'cancel':
          return $t('control.epic.cancel');
        default:
          return $t(`control.epic.${action}`);
      }
    }
    if (action === 'cancel' && actions?.hold_kind === 'integrity_pause') {
      return $t('control.end.action', { action: 'abort' });
    }
    return $t(`control.plan.${action}`);
  }

  /** The act a plan's end is named by: cancel, abandon, or the abort of an integrity pause. */
  function endAct(): 'cancel' | 'abandon' | 'abort' {
    if (actions?.hold_kind === 'integrity_pause') {
      return 'abort';
    }
    return endAction;
  }

  /**
   * Opens a dialog, remembering the control the focus goes back to when it closes.
   *
   * @param next the dialog to show
   */
  function openDialog(next: Exclude<Dialog, null>): void {
    const active = document.activeElement;
    opener = active instanceof HTMLElement ? active : null;
    dialog = next;
    void tick().then(() => dialogElement?.focus());
  }

  /**
   * Closes the dialog and gives the focus back to the control that opened it.
   *
   * A confirm disables the opener while the action runs, and a disabled control cannot take the
   * focus; the keyboard then stays in the panel, on the first act that can still run.
   */
  async function closeDialog(): Promise<void> {
    const previous = opener;
    dialog = null;
    opener = null;
    dialogElement = null;
    await tick();
    if (previous?.isConnected && !(previous as HTMLButtonElement).disabled) {
      previous.focus();
      return;
    }
    const fallback = document.querySelector<HTMLButtonElement>(
      '[data-testid="control-actions"] button:not([disabled])',
    );
    fallback?.focus();
  }

  /** @param event the key press anywhere while a dialog is open */
  function closeOnEscape(event: KeyboardEvent): void {
    if (event.key === 'Escape' && dialog) {
      event.stopPropagation();
      void closeDialog();
    }
  }

  function choose(action: string): void {
    if (subjectKind === 'epic') {
      chooseEpic(action);
      return;
    }
    if (action === 'pause') {
      pausePoint = actions?.pausing_point ?? 'task';
      openDialog('pause');
      return;
    }
    if (action === 'cancel' || action === 'abandon') {
      endAction = action;
      endReason = '';
      closePullRequest = false;
      openDialog('end');
      return;
    }
    onAction(action);
  }

  function chooseEpic(action: string): void {
    if (action === 'next') {
      if (scope > 0 && running >= scope) {
        openDialog('raise');
        return;
      }
      onAction('next', { raise_scope: false });
      return;
    }
    if (action === 'auto') {
      onAction('auto', { enabled: !automatic });
      return;
    }
    if (action === 'common_files') {
      overlapCount = commonFiles;
      openDialog('overlap');
      return;
    }
    if (action === 'archive') {
      openDialog('archive');
      return;
    }
    onAction(action);
  }

  function confirmPause(): void {
    void closeDialog();
    onAction('pause', { point: pausePoint });
  }

  function confirmEnd(): void {
    if (!endReason.trim()) return;
    const act = endAct();
    void closeDialog();
    onAction(act, { reason: endReason.trim(), close_pr: closePullRequest });
  }

  function confirmRaise(): void {
    void closeDialog();
    onAction('next', { raise_scope: true });
  }

  function confirmOverlap(): void {
    void closeDialog();
    onAction('common_files', { count: overlapCount });
  }
</script>

<svelte:window on:keydown={closeOnEscape} />

<section
  class="control-actions"
  data-testid="control-actions"
  aria-label={$t('control.actions.title', { subject: subjectId })}
>
  <h3 class="control-actions__title">{$t('control.actions.title', { subject: subjectId })}</h3>
  {#if actions}
    <ul class="control-actions__list">
      {#each actions.actions as availability (availability.action)}
        <li class="control-actions__item">
          <button
            type="button"
            class="control-actions__button"
            data-testid={`action-${availability.action}`}
            disabled={!availability.available || busy}
            title={!availability.available ? (availability.reason ?? '') : ''}
            aria-describedby={!availability.available && availability.reason
              ? `reason-${availability.action}-${subjectId}`
              : undefined}
            data-advisory={availability.action === 'chat'
              ? $t('control.plan.chat.title')
              : undefined}
            on:click={() => choose(availability.action)}
          >
            {label(availability.action)}
          </button>
          {#if !availability.available && availability.reason}
            <span
              class="control-actions__reason"
              data-testid={`reason-${availability.action}`}
              id={`reason-${availability.action}-${subjectId}`}
            >
              {$t('control.action.disabled', {
                action: label(availability.action),
                reason: availability.reason,
              })}
            </span>
          {/if}
        </li>
      {/each}
    </ul>
  {:else}
    <p class="control-actions__empty" data-testid="control-actions-empty">…</p>
  {/if}

  {#if dialog}
    <div
      class="control-actions__dialog"
      data-testid={`control-dialog-${dialog}`}
      role="dialog"
      aria-labelledby={titleId}
      tabindex="-1"
      bind:this={dialogElement}
    >
      {#if dialog === 'pause'}
        <h4 class="control-actions__dialog-title" id={titleId} data-testid="pause-title">
          {$t('control.pause.title', { plan: subjectId })}
        </h4>
        <fieldset class="control-actions__choices">
          <legend class="control-actions__legend">{$t('control.pause.where')}</legend>
          {#each ['now', 'task', 'phase'] as point (point)}
            <label class="control-actions__choice">
              <input
                type="radio"
                name="pause-point-{subjectId}"
                value={point}
                checked={pausePoint === point}
                on:change={() => (pausePoint = point)}
              />
              {point === 'now'
                ? $t('control.pause.now')
                : point === 'task'
                  ? $t('control.pause.after_task')
                  : $t('control.pause.after_phase')}
            </label>
          {/each}
        </fieldset>
        <div class="control-actions__dialog-actions">
          <button type="button" data-testid="pause-keep" on:click={closeDialog}>
            {$t('control.pause.keep_running')}
          </button>
          <button type="button" data-testid="pause-confirm" disabled={busy} on:click={confirmPause}>
            {$t('control.pause.action')}
          </button>
        </div>
      {:else if dialog === 'end'}
        <h4 class="control-actions__dialog-title" id={titleId} data-testid="end-title">
          {$t('control.end.title', { action: endAct(), plan: subjectId })}
        </h4>
        {#if pullRequestNumber !== null}
          <p class="control-actions__line">
            {$t('control.end.pr_open', { number: pullRequestNumber })}
          </p>
          <label class="control-actions__choice">
            <input type="checkbox" bind:checked={closePullRequest} />
            {$t('control.end.close_pr_choice')}
          </label>
        {/if}
        <label class="control-actions__field">
          {$t('control.end.label.reason')}
          <input
            type="text"
            data-testid="end-reason"
            bind:value={endReason}
            placeholder={$t('control.end.reason_required')}
          />
        </label>
        {#if !endReason.trim()}
          <p class="control-actions__hint">{$t('control.end.reason_required')}</p>
        {/if}
        <p class="control-actions__line">
          {$t('control.end.afterwards', { outcome: endAct() })}
        </p>
        <div class="control-actions__dialog-actions">
          <button type="button" data-testid="end-keep" on:click={closeDialog}>
            {$t('control.end.keep', { action: endAct() })}
          </button>
          <button
            type="button"
            data-testid="end-confirm"
            disabled={busy || !endReason.trim()}
            on:click={confirmEnd}
          >
            {$t('control.end.action', { action: endAct() })}
          </button>
        </div>
      {:else if dialog === 'raise'}
        <h4 class="control-actions__dialog-title" id={titleId}>{$t('control.epic.next_any')}</h4>
        <p class="control-actions__line">
          {$t('control.epic.scope_full', {
            epic: subjectId,
            running,
            scope,
            plan: nextPlan,
          })}
        </p>
        <div class="control-actions__dialog-actions">
          <button type="button" data-testid="raise-leave" on:click={closeDialog}>
            {$t('control.epic.next_leave')}
          </button>
          <button type="button" data-testid="raise-confirm" disabled={busy} on:click={confirmRaise}>
            {$t('control.epic.next_raise', { count: scope + 1 })}
          </button>
        </div>
      {:else if dialog === 'overlap'}
        <h4 class="control-actions__dialog-title" id={titleId}>
          {$t('control.epic.overlap_change')}
        </h4>
        <p class="control-actions__line">
          {$t('control.epic.overlap_means', { count: overlapCount })}
        </p>
        <label class="control-actions__field">
          {$t('control.epic.overlap_change')}
          <input type="number" min="0" data-testid="overlap-count" bind:value={overlapCount} />
        </label>
        <div class="control-actions__dialog-actions">
          <button type="button" data-testid="overlap-leave" on:click={closeDialog}>
            {$t('control.epic.leave_it')}
          </button>
          <button
            type="button"
            data-testid="overlap-confirm"
            disabled={busy}
            on:click={confirmOverlap}
          >
            {$t('control.epic.overlap_save')}
          </button>
        </div>
      {:else if dialog === 'archive'}
        <h4 class="control-actions__dialog-title" id={titleId} data-testid="archive-title">
          {$t('control.archive.title', { epic: subjectId })}
        </h4>
        {#if archiveCheck}
          {#if archiveCheck.completion_review_first}
            <p class="control-actions__line" data-testid="archive-review-first">
              {$t('control.archive.review_first')}
            </p>
          {:else}
            <ul class="control-actions__checks">
              {#each archiveCheck.lines as line (line.label)}
                <li class="control-actions__check" data-testid={`archive-line-${line.label}`}>
                  <span class="control-actions__check-glyph"
                    ><Icon
                      name={line.passed ? 'state.check' : 'state.error'}
                      size={16}
                      label={$t(
                        line.passed ? 'control.archive.check.passed' : 'control.archive.check.failed',
                      )}
                    /></span
                  >
                  <span>{$t(line.label)}</span>
                  {#if line.note}
                    <span class="control-actions__check-note">{line.note}</span>
                  {/if}
                  {#if line.action === 'review-findings'}
                    <button
                      type="button"
                      class="control-actions__link"
                      data-testid="archive-review-findings"
                      on:click={() => onAction('findings')}
                    >
                      {$t('control.archive.review_findings')}
                    </button>
                  {/if}
                </li>
              {/each}
            </ul>
            <p class="control-actions__line" data-testid="archive-carried">
              {$t('control.archive.carried', {
                findings: archiveCheck.carried_findings,
                watches: archiveCheck.carried_watches,
              })}
            </p>
          {/if}
        {:else}
          <p class="control-actions__line">…</p>
        {/if}
        <div class="control-actions__dialog-actions">
          <button type="button" data-testid="archive-not-yet" on:click={closeDialog}>
            {$t('control.archive.not_yet')}
          </button>
          <button
            type="button"
            data-testid="archive-confirm"
            disabled={busy || !archiveCheck?.passed}
            on:click={() => {
              void closeDialog();
              onAction('archive');
            }}
          >
            {$t('control.archive.action')}
          </button>
        </div>
      {/if}
    </div>
  {/if}
</section>

<style>
  .control-actions {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
  }

  .control-actions__title {
    margin: 0;
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .control-actions__list {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    list-style: none;
    margin: 0;
    padding: 0;
  }

  .control-actions__item {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .control-actions__button {
    min-height: 44px;
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    cursor: pointer;
  }

  .control-actions__button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .control-actions__reason {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .control-actions__dialog {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-3);
    border: 1px solid var(--color-primary);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
  }

  .control-actions__dialog-title {
    margin: 0;
    font-size: var(--text-base);
  }

  .control-actions__choices {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    border: none;
    margin: 0;
    padding: 0;
  }

  .control-actions__legend {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .control-actions__choice {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    min-height: 44px;
    font-size: var(--text-sm);
  }

  .control-actions__field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .control-actions__field input {
    min-height: 44px;
    padding: var(--space-1) var(--space-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
  }

  .control-actions__line {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .control-actions__hint {
    margin: 0;
    font-size: var(--text-xs);
    color: var(--color-warning-text);
  }

  .control-actions__checks {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .control-actions__check {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
  }

  .control-actions__check-glyph {
    font-weight: 700;
  }

  .control-actions__check-note {
    color: var(--color-text-muted);
    font-size: var(--text-xs);
  }

  .control-actions__link {
    background: none;
    border: none;
    padding: 0;
    color: var(--color-primary);
    font-size: var(--text-xs);
    text-decoration: underline;
    cursor: pointer;
  }

  .control-actions__dialog-actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    justify-content: flex-end;
  }

  .control-actions__dialog-actions button {
    min-height: 44px;
    padding: var(--space-2) var(--space-3);
    cursor: pointer;
  }

  @media (max-width: 480px) {
    .control-actions__button,
    .control-actions__dialog-actions button {
      width: 100%;
    }

    /* A phone gets the 44 px target on the inline link of the archive check too. */
    .control-actions__link {
      display: inline-flex;
      align-items: center;
      min-height: 44px;
    }
  }
</style>
