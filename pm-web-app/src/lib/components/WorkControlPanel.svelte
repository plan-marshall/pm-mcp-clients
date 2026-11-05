<script lang="ts">
  import { tick } from 'svelte';
  import { t } from '../i18n';
  import ActionBar from './ActionBar.svelte';
  import Button from './Button.svelte';
  import ConfirmDialog from './ConfirmDialog.svelte';
  import HoldResolutionModal from './HoldResolutionModal.svelte';
  import PlanSettingsModal from './PlanSettingsModal.svelte';
  import ChatPanel from './ChatPanel.svelte';
  import Icon from './Icon.svelte';
  import { uniqueId } from '../utils/uniqueId';
  import { actsFor, type ActSpec } from '../actions/byState';
  import {
    apiClient,
    type ArchiveCheck,
    type ChatProposal,
    type ChatReadBack,
    type HoldRecord,
    type HoldResolutionResult,
    type PlanSettingsRecord,
    type WorkActions,
  } from '../api/client';
  import { navigateTo, openCompletionReport } from '../stores/runtime';

  interface SettingsConflict {
    aspect: string;
    currentValue: string;
    wantedValue: string;
    revision: number;
  }

  interface Props {
    subjectId: string;
    subjectKind?: 'plan' | 'epic';
    projectId?: string;
    ended?: boolean;
    scope?: number;
    /** How many of the epic's plans run; `null` when the view does not know, and then not shown. */
    running?: number | null;
    nextPlan?: string;
    pullRequestNumber?: number | null;
    /** Whether an epic starts its plans by itself, as the epic resource reports it. */
    autoStart?: boolean | null;
    onChanged?: () => void;
    /** An epic's review is opened by its view, which owns the review screen. */
    onReview?: (register: 'findings' | 'watches') => void;
    onDecompose?: () => void;
  }

  let {
    subjectId,
    subjectKind = 'plan',
    projectId = '',
    ended = false,
    scope = 0,
    running = null,
    nextPlan = '',
    pullRequestNumber = null,
    autoStart = null,
    onChanged = () => {},
    onReview = () => {},
    onDecompose = () => {},
  }: Props = $props();

  let actions = $state<WorkActions | null>(null);
  /** The automatic mode the last act set, so a resource that has not caught up cannot undo it. */
  let autoValue = $state<boolean | null>(null);
  let archiveCheck = $state<ArchiveCheck | null>(null);
  let busy = $state(false);
  let notice = $state('');
  let failure = $state('');

  let settings = $state<PlanSettingsRecord | null>(null);
  let settingsOpen = $state(false);
  let settingsConflict = $state<SettingsConflict | null>(null);
  let settingsNotice = $state('');

  let holds = $state<HoldRecord[]>([]);
  let holdOutcome = $state<HoldResolutionResult | null>(null);
  let holdOpen = $state(false);
  let holdLoaded = $state(false);

  let chat = $state<ChatReadBack | null>(null);
  let chatOpen = $state(false);
  let chatBusy = $state(false);
  let chatNotice = $state('');
  let endedProposal = $state<ChatProposal | null>(null);
  let endedRole = $state('');

  let chatSubject = $derived(subjectKind === 'epic' ? `epic:${subjectId}` : subjectId);
  let loadedSubject = $state('');

  type Dialog = 'pause' | 'end' | 'raise' | 'overlap' | 'archive' | null;

  let dialog = $state<Dialog>(null);
  let pausePoint = $state('task');
  let endAction = $state<'cancel' | 'abandon' | 'abort'>('cancel');
  let endReason = $state('');
  let closePullRequest = $state(false);
  let overlapCount = $state(0);

  /** The epic's own cancellation, which asks for the plans and a reason (<<cancel-epic>>). */
  let epicCancelOpen = $state(false);
  let cancelWithPlans = $state(true);
  let cancelReason = $state('');

  /**
   * The control the focus goes back to once `busy` clears, when the opener was disabled while the
   * act ran. Without it the focus would drop to the body: a disabled control cannot take it.
   */
  let focusAfterBusy = $state<HTMLElement | 'bar' | null>(null);

  /** The control that opened the dialog, so the focus goes back to it when the dialog closes. */
  let opener: HTMLElement | null = null;

  /** The open dialog's box, which takes the focus when it opens. */
  let dialogElement = $state<HTMLElement | null>(null);

  /** The id of the open dialog's title, for `aria-labelledby`. */
  const titleId = uniqueId('control-dialog-title');

  let automatic = $derived(autoValue ?? autoStart ?? actions?.auto_enabled !== false);
  let commonFiles = $derived(actions?.overlap_max_files ?? 0);

  /**
   * The plan acts the panel handles: the runtime's own acts, and the three it routes itself
   * (`resolve_hold`, `report`, `follow_up`). `decision` is the plan page's, so it is not here.
   */
  const PLAN_ACTS_HANDLED = new Set([
    'pause',
    'start',
    'cancel',
    'abandon',
    'chat',
    'settings',
    'resolve_hold',
    'report',
    'follow_up',
  ]);
  /** The epic acts the panel handles. */
  const EPIC_ACTS_HANDLED = new Set([
    'next',
    'findings',
    'watches',
    'decompose',
    'chat',
    'auto',
    'common_files',
    'archive',
    'cancel',
  ]);

  /** The acts of the subject's kind the panel runs; anything else a runtime offers is dropped. */
  let handledActs = $derived(subjectKind === 'epic' ? EPIC_ACTS_HANDLED : PLAN_ACTS_HANDLED);

  /** The state the acts of this panel are derived from. */
  let actState = $derived(planState());
  let actSpecs = $derived(
    ended
      ? [{ id: 'chat', labelKey: 'control.plan.chat', advisoryKey: 'control.plan.chat_ended.title', role: 'secondary' as const }]
      : // Only the acts this panel can run reach the bar: an id the runtime offers beyond this set
        // (the plan page's `decision` among them) would otherwise be posted to a route that does
        // not take it, or drawn with a label no catalogue carries.
        actsFor(subjectKind, actState, actions?.actions).filter((spec) =>
          handledActs.has(spec.id),
        ),
  );
  let barActs = $derived(actSpecs.map(toBarAct));

  /**
   * @returns the state word the acts-by-state table knows
   */
  function planState(): string {
    const state = actions?.state ?? 'running';
    if (state === 'waiting') return 'waiting_for_you';
    if (ended) return 'ended';
    return state;
  }

  /**
   * @param spec the act's table entry
   * @returns the act as the bar draws it
   */
  function toBarAct(spec: ActSpec) {
    return {
      id: spec.id,
      label: label(spec.id),
      advisory: spec.advisoryKey ? $t(spec.advisoryKey) : undefined,
      role: spec.role,
      destructive: spec.destructive,
      disabled: spec.disabled,
      disabledReason: spec.disabledReason,
    };
  }

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
        case 'chat':
          return $t('control.epic.chat');
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

  /** @returns how the plan is archived, the word the end dialogue's consequence names */
  function endOutcome(): 'cancelled' | 'abandoned' | 'aborted' {
    const act = endAct();
    if (act === 'cancel') {
      return 'cancelled';
    }
    return act === 'abandon' ? 'abandoned' : 'aborted';
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
    if (fallback) {
      fallback.focus();
      return;
    }
    // The act that closed the dialog disables the whole bar while it runs. Keep the control to
    // return to, or the bar when it is gone, and give it the focus as soon as it is enabled again.
    focusAfterBusy = previous?.isConnected ? previous : 'bar';
  }

  // The confirmed act disables its opener until the request settles; the focus then returns to it,
  // or to the first control that can run when the act removed the opener (a pause leaves no pause).
  $effect(() => {
    if (busy || !focusAfterBusy) {
      return;
    }
    const target = focusAfterBusy;
    focusAfterBusy = null;
    if (target !== 'bar' && target.isConnected && !(target as HTMLButtonElement).disabled) {
      target.focus();
      return;
    }
    document
      .querySelector<HTMLButtonElement>('[data-testid="control-actions"] button:not([disabled])')
      ?.focus();
  });

  /** @param event the key press inside an open dialog, which Tab must not leave */
  function trapFocus(event: KeyboardEvent): void {
    if (event.key !== 'Tab' || !dialogElement) {
      return;
    }
    const items = Array.from(
      dialogElement.querySelectorAll<HTMLElement>(
        'button:not([disabled]), input:not([disabled]), select:not([disabled]), ' +
          'textarea:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])',
      ),
    );
    if (items.length === 0) {
      event.preventDefault();
      dialogElement.focus();
      return;
    }
    const first = items[0];
    const last = items[items.length - 1];
    const active = document.activeElement;
    if (!dialogElement.contains(active)) {
      event.preventDefault();
      first.focus();
      return;
    }
    if (event.shiftKey && (active === first || active === dialogElement)) {
      event.preventDefault();
      last.focus();
      return;
    }
    if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }

  /** @param event the key press anywhere while a dialog is open */
  function closeOnEscape(event: KeyboardEvent): void {
    if (event.key === 'Escape' && dialog) {
      event.stopPropagation();
      void closeDialog();
    }
  }

  /**
   * Routes an act the bar reports: the dialogs stay here, and the panel applies the rest.
   *
   * @param id the chosen act
   */
  function chooseAct(id: string): void {
    if (ended) {
      // The ended panel draws the chat alone; no act of the table is posted from here.
      if (id === 'chat' || id === 'follow_up') {
        void openChat();
      }
      return;
    }
    if (subjectKind === 'epic') {
      chooseEpic(id);
      return;
    }
    if (id === 'pause') {
      pausePoint = actions?.pausing_point ?? 'task';
      openDialog('pause');
      return;
    }
    if (id === 'cancel' || id === 'abandon') {
      endAction = id;
      endReason = '';
      closePullRequest = false;
      openDialog('end');
      return;
    }
    if (id === 'resolve_hold') {
      void openHold();
      return;
    }
    if (id === 'report') {
      openCompletionReport(subjectId);
      return;
    }
    if (id === 'follow_up') {
      void openChat();
      return;
    }
    void handleAction(id);
  }

  /** @param id the chosen act of an epic */
  function chooseEpic(id: string): void {
    if (id === 'next') {
      // A count the view does not know cannot be read as "a place is free": where the epic's
      // scope is known, ask and say that the count is unknown instead of showing it as none.
      // Without a scope (a row of Running Work) there is nothing to weigh, so the act starts.
      if (scope > 0 && (running === null || running >= scope)) {
        openDialog('raise');
        return;
      }
      void handleAction('next', { raise_scope: false });
      return;
    }
    if (id === 'common_files') {
      overlapCount = commonFiles;
      openDialog('overlap');
      return;
    }
    if (id === 'archive') {
      openDialog('archive');
      return;
    }
    if (id === 'cancel') {
      // The dialogue is new: a refusal of the attempt before it must not stand in it.
      failure = '';
      cancelWithPlans = true;
      cancelReason = '';
      epicCancelOpen = true;
      return;
    }
    void handleAction(id);
  }

  /**
   * Sends the epic's cancel the dialogue confirmed, with the choice it made for the epic's plans.
   *
   * The bar is disabled while the act runs, so the focus returns to it when `busy` clears.
   */
  async function confirmEpicCancel(): Promise<void> {
    const reason = cancelReason.trim();
    if (!reason || busy) {
      return;
    }
    const from = document.activeElement;
    // The dialog stays open while the act runs: its busy guard ignores a second confirm, and a
    // refusal leaves the dialogue in place so the reason can be corrected and sent again.
    const accepted = await handleAction('cancel', { with_plans: cancelWithPlans, reason });
    if (!accepted) {
      return;
    }
    epicCancelOpen = false;
    focusAfterBusy =
      document.querySelector<HTMLElement>('[data-testid="action-bar-overflow"]') ??
      (from instanceof HTMLElement ? from : null);
  }

  function confirmPause(): void {
    void closeDialog();
    void handleAction('pause', { point: pausePoint });
  }

  function confirmEnd(): void {
    if (!endReason.trim()) return;
    const act = endAct();
    void closeDialog();
    void handleAction(act, { reason: endReason.trim(), close_pr: closePullRequest });
  }

  function confirmRaise(): void {
    void closeDialog();
    void handleAction('next', { raise_scope: true });
  }

  function confirmOverlap(): void {
    void closeDialog();
    void handleAction('common_files', { count: overlapCount });
  }

  // A new subject starts from nothing; the panel is reused when Overview switches rows.
  $effect(() => {
    const key = `${subjectKind}:${subjectId}:${ended}`;
    if (key === loadedSubject) return;
    loadedSubject = key;
    actions = null;
    archiveCheck = null;
    holds = [];
    holdLoaded = false;
    holdOpen = false;
    holdOutcome = null;
    autoValue = null;
    notice = '';
    failure = '';
    settingsOpen = false;
    settingsConflict = null;
    settingsNotice = '';
    chatOpen = false;
    chat = null;
    endedProposal = null;
    dialog = null;
    epicCancelOpen = false;
    cancelWithPlans = true;
    cancelReason = '';
    void load();
  });

  async function load(): Promise<void> {
    failure = '';
    try {
      const next = ended
        ? null
        : subjectKind === 'epic'
          ? await apiClient.getEpicActions(subjectId)
          : await apiClient.getPlanActions(subjectId);
      actions = next;
      if (subjectKind === 'epic') {
        archiveCheck = await apiClient.getEpicArchiveCheck(subjectId).catch(() => null);
      } else {
        const nextHolds = await apiClient.getPlanHolds(subjectId).catch(() => []);
        holds = nextHolds;
        if (!holdLoaded && nextHolds.length > 0) {
          holdOpen = true;
        }
        holdLoaded = true;
      }
    } catch (err: unknown) {
      failure = $t('control.action.failed', { reason: reasonOf(err) });
    }
  }

  function reasonOf(err: unknown): string {
    return err instanceof Error ? err.message : String(err);
  }

  /**
   * Applies an act and says whether the runtime accepted it, so a dialog can stay open on a
   * refusal instead of closing over the reason the operator has to correct.
   *
   * @param action the act's id
   * @param payload the act's named parameters
   * @returns `true` when the act went through
   */
  async function handleAction(
    action: string,
    payload: Record<string, unknown> = {},
  ): Promise<boolean> {
    notice = '';
    failure = '';
    if (action === 'settings') {
      await openSettings();
      return true;
    }
    if (action === 'chat') {
      await openChat();
      return true;
    }
    if (subjectKind === 'epic' && (action === 'findings' || action === 'watches')) {
      onReview(action === 'findings' ? 'findings' : 'watches');
      return true;
    }
    if (subjectKind === 'epic' && action === 'decompose') {
      onDecompose();
      return true;
    }
    busy = true;
    const previousAuto = actions?.auto_enabled;
    try {
      const wanted = action === 'auto' ? { enabled: payload.enabled ?? !automatic } : payload;
      const response =
        subjectKind === 'epic'
          ? await apiClient.applyEpicAction(subjectId, action, wanted)
          : await apiClient.applyPlanAction(subjectId, action, wanted);
      actions = response;
      if (action === 'auto' && typeof response.auto_enabled === 'boolean') {
        autoValue = response.auto_enabled;
      }
      notice = noticeFor(action, wanted, previousAuto, response);
      await load();
      onChanged();
      return true;
    } catch (err: unknown) {
      failure = $t('control.action.failed', { reason: reasonOf(err) });
      return false;
    } finally {
      busy = false;
    }
  }

  /** What an act says of itself, where the runtime reports the value it set. */
  function noticeFor(
    action: string,
    payload: Record<string, unknown>,
    previousAuto: boolean | null | undefined,
    response: WorkActions,
  ): string {
    if (action === 'auto') {
      const wanted = payload.enabled === true;
      if (previousAuto === wanted) {
        return $t('control.epic.auto_already', {
          epic: subjectId,
          mode: wanted ? 'on' : 'off',
        });
      }
      return wanted
        ? $t('control.epic.auto_on_done', { epic: subjectId })
        : $t('control.epic.auto_off_done', { epic: subjectId });
    }
    if (action === 'common_files') {
      return $t('control.epic.overlap_done', {
        epic: subjectId,
        count: Number(payload.count ?? response.overlap_max_files ?? 0),
        started: 'none',
      });
    }
    return '';
  }

  async function openSettings(): Promise<void> {
    settingsConflict = null;
    settingsNotice = '';
    try {
      settings = await apiClient.getPlanSettings(subjectId);
      settingsOpen = true;
    } catch (err: unknown) {
      failure = $t('control.action.failed', { reason: reasonOf(err) });
    }
  }

  async function applySetting(payload: {
    aspect: string;
    value: string | null;
    revision: number;
  }): Promise<void> {
    busy = true;
    settingsNotice = '';
    try {
      const result = await apiClient.updatePlanSettings(
        subjectId,
        payload.aspect,
        payload.value,
        payload.revision,
      );
      if (result.applied) {
        settingsConflict = null;
        settings = await apiClient.getPlanSettings(subjectId);
        settingsNotice = $t('control.settings.applies', {
          plan: subjectId,
          version: result.revision,
        });
        onChanged();
      } else if (result.conflict) {
        settingsConflict = {
          aspect: payload.aspect,
          currentValue: result.current_value ?? '',
          wantedValue: payload.value ?? '',
          revision: result.revision,
        };
      } else if (result.state) {
        settingsNotice = $t('control.settings.too_late', {
          plan: subjectId,
          position: result.state,
        });
      }
    } catch (err: unknown) {
      settingsNotice = $t('control.settings.refused', {
        value: payload.value ?? '',
        reason: reasonOf(err),
      });
    } finally {
      busy = false;
    }
  }

  async function keepCurrent(): Promise<void> {
    settingsConflict = null;
    try {
      settings = await apiClient.getPlanSettings(subjectId);
    } catch {
      // Keeping the current value needs no further read; the next open reads it again.
    }
  }

  async function openChat(): Promise<void> {
    chatNotice = '';
    chatOpen = true;
    if (ended) {
      const proposal = await apiClient.getEndedChat(projectId, subjectId).catch(() => null);
      endedProposal = proposal?.proposal ?? null;
      endedRole = proposal?.role ?? (subjectKind === 'epic' ? 'decompose' : 'replan');
      if (!proposal) {
        chatNotice = $t('decision.chat.closed');
      }
      return;
    }
    try {
      chat = await apiClient.getChat(chatSubject);
    } catch {
      chat = null;
      chatNotice = $t('decision.chat.closed');
    }
  }

  async function sendChat(text: string): Promise<void> {
    chatBusy = true;
    try {
      chat = await apiClient.chatWithSubject(chatSubject, text);
    } catch (err: unknown) {
      chatNotice = $t('control.action.failed', { reason: reasonOf(err) });
    } finally {
      chatBusy = false;
    }
  }

  async function answerProposal(accept: boolean): Promise<void> {
    chatBusy = true;
    try {
      chat = await apiClient.answerSubjectChatProposal(chatSubject, accept);
      onChanged();
    } catch (err: unknown) {
      chatNotice = $t('control.action.failed', { reason: reasonOf(err) });
    } finally {
      chatBusy = false;
    }
  }

  /** Closes the chat panel without ending the chat the runtime holds. */
  function closeChat(): void {
    chatOpen = false;
    chat = null;
    endedProposal = null;
  }

  async function endTheChat(): Promise<void> {
    chatBusy = true;
    try {
      if (!ended) {
        await apiClient.endChat(chatSubject);
      }
      chatOpen = false;
      chat = null;
      endedProposal = null;
    } catch (err: unknown) {
      chatNotice = $t('control.action.failed', { reason: reasonOf(err) });
    } finally {
      chatBusy = false;
    }
  }

  /** Opens the start on the follow-up plan the ended work's chat proposed. */
  async function startFollowUp(): Promise<void> {
    chatBusy = true;
    try {
      const result = await apiClient.acceptFollowUp(projectId, subjectId);
      const entryId = result.start?.entry?.entry_id;
      chatOpen = false;
      if (entryId) {
        navigateTo('start', { projectId, entryId });
      }
      onChanged();
    } catch (err: unknown) {
      chatNotice = $t('control.action.failed', { reason: reasonOf(err) });
    } finally {
      chatBusy = false;
    }
  }

  async function resolveHold(payload: { act: string; reason?: string }): Promise<void> {
    const hold = holds[0];
    if (!hold) return;
    busy = true;
    try {
      const result = await apiClient.resolveHold(
        subjectId,
        hold.hold_id,
        payload.act,
        payload.reason,
      );
      holdOutcome = result;
      holds = await apiClient.getPlanHolds(subjectId).catch(() => []);
      if (result.released) {
        holdOpen = false;
        notice = $t('control.hold.released', {
          subject: result.subject || subjectId,
        });
      }
      onChanged();
    } catch (err: unknown) {
      failure = $t('control.hold.act_failed', { reason: reasonOf(err) });
    } finally {
      busy = false;
    }
  }

  function closeHold(): void {
    holdOpen = false;
    holdOutcome = null;
  }

  /**
   * Opens the hold on a fresh read: an earlier read may have failed, and the dialogue needs the
   * hold the runtime reports now. A failed read is reported instead of opening a dialogue whose
   * hold nobody confirmed any more.
   */
  async function openHold(): Promise<void> {
    holdOutcome = null;
    if (subjectKind === 'plan') {
      try {
        holds = await apiClient.getPlanHolds(subjectId);
      } catch (err: unknown) {
        failure = $t('control.hold.act_failed', { reason: reasonOf(err) });
        return;
      }
    }
    holdOpen = true;
  }
</script>

<svelte:window on:keydown={closeOnEscape} />

<section
  class="work-control"
  data-testid="work-control"
  aria-label={$t('control.actions.title', { subject: subjectId })}
>
  {#if ended}
    <div data-testid="control-actions">
      <ActionBar acts={barActs} {busy} onAct={chooseAct} />
    </div>
  {:else if actions}
    <div data-testid="control-actions">
      <ActionBar acts={barActs} {busy} onAct={chooseAct} />
    </div>
  {:else}
    <p class="control-actions__empty" data-testid="control-actions-empty">…</p>
  {/if}

  {#if notice}
    <p class="work-control__notice" data-testid="control-notice" role="status">{notice}</p>
  {/if}
  {#if failure}
    <p class="work-control__failure" data-testid="control-failure" role="alert">{failure}</p>
  {/if}
  {#if subjectKind === 'plan' && holds.length > 0}
    <Button
      variant="secondary"
      data-testid="open-hold"
      on:click={openHold}
    >
      {$t('control.hold.resolve_title')}
    </Button>
  {/if}

  {#if dialog}
    <div
      class="control-dialog"
      data-testid={`control-dialog-${dialog}`}
      role="dialog"
      aria-modal={dialog === 'end' ? 'true' : undefined}
      aria-labelledby={titleId}
      tabindex="-1"
      bind:this={dialogElement}
      onkeydown={dialog === 'end' ? trapFocus : undefined}
    >
      {#if dialog === 'pause'}
        <h4 class="control-dialog__title" id={titleId} data-testid="pause-title">
          {$t('control.pause.title', { plan: subjectId })}
        </h4>
        <fieldset class="control-dialog__choices">
          <legend class="control-dialog__legend">{$t('control.pause.where')}</legend>
          {#each ['now', 'task', 'phase'] as point (point)}
            <label class="control-dialog__choice">
              <input
                type="radio"
                name="pause-point-{subjectId}"
                value={point}
                checked={pausePoint === point}
                onchange={() => (pausePoint = point)}
              />
              {point === 'now'
                ? $t('control.pause.now')
                : point === 'task'
                  ? $t('control.pause.after_task')
                  : $t('control.pause.after_phase')}
            </label>
          {/each}
        </fieldset>
        <div class="control-dialog__actions">
          <Button variant="secondary" data-testid="pause-keep" on:click={closeDialog}>
            {$t('control.pause.keep_running')}
          </Button>
          <Button variant="primary" data-testid="pause-confirm" disabled={busy} on:click={confirmPause}>
            {$t('control.pause.action')}
          </Button>
        </div>
      {:else if dialog === 'end'}
        <h4 class="control-dialog__title" id={titleId} data-testid="end-title">
          {$t('control.end.title', { action: endAct(), plan: subjectId })}
        </h4>
        {#if pullRequestNumber !== null}
          <p class="control-dialog__line">
            {$t('control.end.pr_open', { number: pullRequestNumber })}
          </p>
          <label class="control-dialog__choice">
            <input type="checkbox" bind:checked={closePullRequest} />
            {$t('control.end.close_pr_choice')}
          </label>
        {/if}
        <label class="control-dialog__field">
          {$t('control.end.label.reason')}
          <input
            type="text"
            data-testid="end-reason"
            bind:value={endReason}
            placeholder={$t('control.end.reason_required')}
          />
        </label>
        {#if !endReason.trim()}
          <p class="control-dialog__hint">{$t('control.end.reason_required')}</p>
        {/if}
        <ul class="control-dialog__consequences" data-testid="end-consequences">
          <li>{$t('control.end.afterwards', { outcome: endOutcome() })}</li>
        </ul>
        <div class="control-dialog__actions">
          <Button variant="secondary" data-testid="end-keep" on:click={closeDialog}>
            {$t('control.end.keep', { action: endAct() })}
          </Button>
          <Button
            variant="danger"
            data-testid="end-confirm"
            disabled={busy || !endReason.trim()}
            on:click={confirmEnd}
          >
            {$t('control.end.action', { action: endAct() })}
          </Button>
        </div>
      {:else if dialog === 'raise'}
        <h4 class="control-dialog__title" id={titleId}>{$t('control.epic.next_any')}</h4>
        <p class="control-dialog__line" data-testid="raise-scope">
          {#if running === null}
            {$t('control.epic.scope_unknown', {
              epic: subjectId,
              scope,
              plan: nextPlan,
            })}
          {:else}
            {$t('control.epic.scope_full', {
              epic: subjectId,
              running,
              scope,
              plan: nextPlan,
            })}
          {/if}
        </p>
        <div class="control-dialog__actions">
          <Button variant="secondary" data-testid="raise-leave" on:click={closeDialog}>
            {$t('control.epic.next_leave')}
          </Button>
          <Button variant="primary" data-testid="raise-confirm" disabled={busy} on:click={confirmRaise}>
            {$t('control.epic.next_raise', { count: scope + 1 })}
          </Button>
        </div>
      {:else if dialog === 'overlap'}
        <h4 class="control-dialog__title" id={titleId}>
          {$t('control.epic.overlap_change')}
        </h4>
        <p class="control-dialog__line">
          {$t('control.epic.overlap_means', { count: overlapCount })}
        </p>
        <label class="control-dialog__field">
          {$t('control.epic.overlap_change')}
          <input type="number" min="0" data-testid="overlap-count" bind:value={overlapCount} />
        </label>
        <div class="control-dialog__actions">
          <Button variant="secondary" data-testid="overlap-leave" on:click={closeDialog}>
            {$t('control.epic.leave_it')}
          </Button>
          <Button
            variant="primary"
            data-testid="overlap-confirm"
            disabled={busy}
            on:click={confirmOverlap}
          >
            {$t('control.epic.overlap_save')}
          </Button>
        </div>
      {:else if dialog === 'archive'}
        <h4 class="control-dialog__title" id={titleId} data-testid="archive-title">
          {$t('control.archive.title', { epic: subjectId })}
        </h4>
        {#if archiveCheck}
          {#if archiveCheck.completion_review_first}
            <p class="control-dialog__line" data-testid="archive-review-first">
              {$t('control.archive.review_first')}
            </p>
          {:else}
            <ul class="control-dialog__checks">
              {#each archiveCheck.lines as line (line.label)}
                <li class="control-dialog__check" data-testid={`archive-line-${line.label}`}>
                  <span class="control-dialog__check-glyph"
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
                    <span class="control-dialog__check-note">{line.note}</span>
                  {/if}
                  {#if line.action === 'review-findings'}
                    <Button
                      variant="ghost"
                      size="sm"
                      class="control-dialog__link"
                      data-testid="archive-review-findings"
                      on:click={() => onReview('findings')}
                    >
                      {$t('control.archive.review_findings')}
                    </Button>
                  {/if}
                </li>
              {/each}
            </ul>
            <p class="control-dialog__line" data-testid="archive-carried">
              {$t('control.archive.carried', {
                findings: archiveCheck.carried_findings,
                watches: archiveCheck.carried_watches,
              })}
            </p>
          {/if}
        {:else}
          <p class="control-dialog__line">…</p>
        {/if}
        <div class="control-dialog__actions">
          <Button variant="secondary" data-testid="archive-not-yet" on:click={closeDialog}>
            {$t('control.archive.not_yet')}
          </Button>
          <Button
            variant="primary"
            data-testid="archive-confirm"
            disabled={busy || !archiveCheck?.passed}
            on:click={() => {
              void closeDialog();
              void handleAction('archive');
            }}
          >
            {$t('control.archive.action')}
          </Button>
        </div>
      {/if}
    </div>
  {/if}
</section>

<ConfirmDialog
  open={epicCancelOpen}
  title={$t('control.cancel_epic.title', { epic: subjectId })}
  consequences={[$t('control.cancel_epic.afterwards')]}
  actionLabel={$t('control.cancel_epic.action')}
  cancelLabel={$t('control.cancel_epic.keep')}
  busy={busy}
  confirmDisabled={!cancelReason.trim()}
  onConfirm={confirmEpicCancel}
  onClose={() => (epicCancelOpen = false)}
>
  {#if running !== null}
    <p class="control-dialog__line" data-testid="epic-cancel-plans">
      {$t('control.cancel_epic.plans_line', { count: running })}
    </p>
  {/if}
  <fieldset class="control-dialog__choices">
    <legend class="control-dialog__legend">{$t('control.cancel_epic.label.plans')}</legend>
    <label class="control-dialog__choice">
      <input type="radio" name="epic-cancel-plans" bind:group={cancelWithPlans} value={true} />
      {$t('control.cancel_epic.plans_too')}
    </label>
    <label class="control-dialog__choice">
      <input type="radio" name="epic-cancel-plans" bind:group={cancelWithPlans} value={false} />
      {$t('control.cancel_epic.plans_on')}
    </label>
  </fieldset>
  <label class="control-dialog__field">
    {$t('control.end.label.reason')}
    <input type="text" data-testid="epic-cancel-reason" bind:value={cancelReason} />
  </label>
  {#if !cancelReason.trim()}
    <p class="control-dialog__hint">{$t('control.end.reason_required')}</p>
  {/if}
  {#if failure}
    <p class="control-dialog__hint" role="alert" data-testid="epic-cancel-failure">{failure}</p>
  {/if}
</ConfirmDialog>

<PlanSettingsModal
  planId={subjectId}
  {settings}
  open={settingsOpen}
  {busy}
  conflict={settingsConflict}
  notice={settingsNotice}
  onApply={applySetting}
  onApplyAgain={applySetting}
  onKeepCurrent={keepCurrent}
  onClose={() => (settingsOpen = false)}
/>

<HoldResolutionModal
  hold={holds[0] ?? null}
  open={holdOpen}
  {busy}
  project={projectId}
  outcome={holdOutcome}
  onResolve={resolveHold}
  onClose={closeHold}
/>

{#if chatOpen}
  {#if ended}
    <ChatPanel
      open={true}
      role={endedRole}
      subject={subjectId}
      turns={[]}
      proposal={endedProposal}
      available={true}
      busy={chatBusy}
      onSend={() => {}}
      onEnd={endTheChat}
      onAccept={startFollowUp}
      onReject={endTheChat}
      onToggle={() => {}}
      onClose={closeChat}
      acceptLabel={$t('start.chat.start_follow_up')}
      rejectLabel={$t('start.decomposition.discard')}
    />
  {:else}
    <ChatPanel
      open={true}
      role={chat?.role ?? ''}
      subject={chat?.subject_id ?? subjectId}
      turns={chat?.turns ?? []}
      proposal={chat?.proposal ?? null}
      available={chat !== null}
      busy={chatBusy}
      onSend={sendChat}
      onEnd={endTheChat}
      onAccept={() => answerProposal(true)}
      onReject={() => answerProposal(false)}
      onToggle={openChat}
      onClose={closeChat}
    />
  {/if}
  {#if chatNotice}
    <p class="work-control__failure" data-testid="chat-notice" role="alert">{chatNotice}</p>
  {/if}
{/if}

<style>
  .work-control {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .work-control__notice,
  .work-control__failure {
    margin: 0;
    padding: var(--space-2);
    border-radius: var(--radius-sm);
    font-size: var(--text-sm);
  }

  .work-control__notice {
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
  }

  .work-control__failure {
    background-color: var(--color-danger-subtle);
    color: var(--color-danger);
  }

  .control-actions__empty {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }

  .control-dialog {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-3);
    border: 1px solid var(--color-primary);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
  }

  .control-dialog__title {
    margin: 0;
    font-size: var(--text-base);
  }

  .control-dialog__choices {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    border: none;
    margin: 0;
    padding: 0;
  }

  .control-dialog__legend {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .control-dialog__choice {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    min-height: 44px;
    font-size: var(--text-sm);
  }

  .control-dialog__field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .control-dialog__field input {
    min-height: 44px;
    padding: var(--space-1) var(--space-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
  }

  .control-dialog__line {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  /* A danger act names what it costs in the danger colour, before the act itself. */
  .control-dialog__consequences {
    margin: 0;
    padding-inline-start: var(--space-5);
    font-size: var(--text-sm);
    color: var(--color-danger);
  }

  .control-dialog__hint {
    margin: 0;
    font-size: var(--text-xs);
    color: var(--color-warning-text);
  }

  .control-dialog__checks {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .control-dialog__check {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
  }

  .control-dialog__check-glyph {
    font-weight: 700;
  }

  .control-dialog__check-note {
    color: var(--color-text-muted);
    font-size: var(--text-xs);
  }

  .control-dialog__actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    justify-content: flex-end;
  }

  @media (max-width: 480px) {
    .control-dialog__actions {
      flex-direction: column;
      align-items: stretch;
    }
  }
</style>
