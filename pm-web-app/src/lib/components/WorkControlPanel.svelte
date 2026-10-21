<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';
  import ControlActionsBar from './ControlActionsBar.svelte';
  import HoldResolutionModal from './HoldResolutionModal.svelte';
  import PlanSettingsModal from './PlanSettingsModal.svelte';
  import ChatPanel from './ChatPanel.svelte';
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
  import { navigateTo } from '../stores/runtime';

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
    running?: number;
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
    running = 0,
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

  onMount(() => {
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

  async function handleAction(
    action: string,
    payload: Record<string, unknown> = {},
  ): Promise<void> {
    notice = '';
    failure = '';
    if (action === 'settings') {
      await openSettings();
      return;
    }
    if (action === 'chat') {
      await openChat();
      return;
    }
    if (subjectKind === 'epic' && (action === 'findings' || action === 'watches')) {
      onReview(action);
      return;
    }
    if (subjectKind === 'epic' && action === 'decompose') {
      onDecompose();
      return;
    }
    busy = true;
    const previousAuto = actions?.auto_enabled;
    try {
      const response =
        subjectKind === 'epic'
          ? await apiClient.applyEpicAction(subjectId, action, payload)
          : await apiClient.applyPlanAction(subjectId, action, payload);
      actions = response;
      if (action === 'auto' && typeof response.auto_enabled === 'boolean') {
        autoValue = response.auto_enabled;
      }
      notice = noticeFor(action, payload, previousAuto, response);
      await load();
      onChanged();
    } catch (err: unknown) {
      failure = $t('control.action.failed', { reason: reasonOf(err) });
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

  function openHold(): void {
    holdOutcome = null;
    holdOpen = true;
  }
</script>

<section class="work-control" data-testid="work-control">
  {#if !ended}
    <ControlActionsBar
      {subjectId}
      {subjectKind}
      {actions}
      {archiveCheck}
      {busy}
      {scope}
      {running}
      {nextPlan}
      {pullRequestNumber}
      autoEnabled={autoValue ?? autoStart}
      onAction={handleAction}
    />
  {:else}
    <button type="button" class="work-control__chat" data-testid="action-chat" onclick={openChat}>
      {$t('control.plan.chat')}
    </button>
  {/if}

  {#if notice}
    <p class="work-control__notice" data-testid="control-notice" role="status">{notice}</p>
  {/if}
  {#if failure}
    <p class="work-control__failure" data-testid="control-failure" role="alert">{failure}</p>
  {/if}
  {#if subjectKind === 'plan' && holds.length > 0}
    <button type="button" class="work-control__hold" data-testid="open-hold" onclick={openHold}>
      {$t('control.hold.resolve_title')}
    </button>
  {/if}
</section>

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

  .work-control__hold,
  .work-control__chat {
    min-height: 44px;
    padding: var(--space-2) var(--space-3);
    cursor: pointer;
    align-self: flex-start;
  }
</style>
