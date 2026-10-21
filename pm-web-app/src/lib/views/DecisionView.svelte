<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import ChatPanel from '../components/ChatPanel.svelte';
  import {
    apiClient,
    type ChatReadBack,
    type DecisionEnvelope,
    type DecisionPart,
    type DecisionInterpretation,
  } from '../api/client';
  import { runtimeStore, refreshDecisionsCount } from '../stores/runtime';

  const NOTIFY_KEY = 'pm_decision_notify';

  let decisions = $state<DecisionEnvelope[]>([]);
  let isLoading = $state(true);
  let errorMessage = $state('');

  let showEvidence = $state<Record<string, boolean>>({});
  /** The chosen option of each part, keyed by decision and part id. */
  let partAnswers = $state<Record<string, Record<string, string>>>({});
  let partNotes = $state<Record<string, string>>({});
  let ownWordsTexts = $state<Record<string, string>>({});
  let ownWordsMode = $state<Record<string, boolean>>({});
  let interpretations = $state<Record<string, DecisionInterpretation | null>>({});
  let isInterpreting = $state<Record<string, boolean>>({});
  let isSubmitting = $state<Record<string, boolean>>({});
  let waiverReasons = $state<Record<string, string>>({});
  let revealed = $state<Record<string, boolean>>({});
  let reopened = $state<Record<string, { n: number; m: number }>>({});
  let revisedNotices = $state<Record<string, string>>({});
  let notices = $state<Array<{ id: string; text: string }>>([]);

  let notifyState = $state<'default' | 'granted' | 'denied'>('default');
  let notifyAsked = $state(false);
  /** Whether a decision arrived in this session; the permission is asked only then, never at start. */
  let sawDecisionEvent = $state(false);
  let notifyMessage = $state('');

  let chatDecisionId = $state<string | null>(null);
  let chat = $state<ChatReadBack | null>(null);
  let chatOpen = $state(false);
  let chatBusy = $state(false);
  let chatNotice = $state('');

  let unsubscribeEvents: (() => void) | null = null;
  let isMounted = true;

  onMount(async () => {
    isMounted = true;
    readNotifyPreference();
    unsubscribeEvents = apiClient.subscribeEvents((event, data) => {
      handleEvent(event, data);
    });
    await loadDecisions();
  });

  onDestroy(() => {
    isMounted = false;
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  /** Reads the permission the operator gave once, if any. */
  function readNotifyPreference(): void {
    if (typeof window === 'undefined') return;
    try {
      const stored = window.localStorage.getItem(NOTIFY_KEY);
      if (stored === 'granted' || stored === 'denied') {
        notifyState = stored;
        notifyAsked = true;
        return;
      }
      if (typeof Notification !== 'undefined') {
        const permission = Notification.permission;
        notifyState =
          permission === 'granted' ? 'granted' : permission === 'denied' ? 'denied' : 'default';
        notifyAsked = permission !== 'default';
      }
    } catch {
      notifyState = 'denied';
      notifyAsked = true;
    }
  }

  async function allowNotifications(): Promise<void> {
    notifyAsked = true;
    try {
      if (typeof Notification === 'undefined') {
        notifyState = 'denied';
        notifyMessage = $t('decision.notify.failed');
        return;
      }
      const permission = await Notification.requestPermission();
      notifyState = permission === 'granted' ? 'granted' : 'denied';
      notifyMessage =
        notifyState === 'granted' ? $t('decision.notify.granted') : $t('decision.notify.denied');
    } catch {
      notifyState = 'denied';
      notifyMessage = $t('decision.notify.failed');
    }
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(NOTIFY_KEY, notifyState);
    }
  }

  function denyNotifications(): void {
    notifyAsked = true;
    notifyState = 'denied';
    notifyMessage = $t('decision.notify.denied');
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(NOTIFY_KEY, 'denied');
    }
  }

  /** The parameters the notification's title names its subject by. */
  function notificationParams(dec: DecisionEnvelope): Record<string, string> {
    const subject = dec.subject ?? '';
    const kind = dec.is_waiver ? 'waiver' : 'decision';
    if (subject.startsWith('epic ')) {
      return { subject: 'epic', name: subject.slice('epic '.length), kind };
    }
    if (subject.startsWith('setup of ')) {
      return { subject: 'setup', project: subject.slice('setup of '.length), kind };
    }
    if (dec.scope === 'entry' || subject.startsWith('start ')) {
      return { subject: 'entry', project: subject.replace(/^start (in )?/, ''), kind };
    }
    return { subject: 'other', name: subject.replace(/^plan /, ''), kind };
  }

  /** Fires a desktop notification while the tab is in the background. */
  function notifyDecision(dec: DecisionEnvelope): void {
    if (notifyState !== 'granted') return;
    if (typeof Notification === 'undefined') return;
    if (typeof document === 'undefined' || document.visibilityState === 'visible') return;
    try {
      new Notification($t('decision.notify.title', notificationParams(dec)));
    } catch {
      // A browser that refuses the notification leaves the tab title and the list to say so.
    }
  }

  function handleEvent(event: string, data: unknown) {
    if (!isMounted) return;
    if (event === 'decision.opened' || event === 'decision.created') {
      sawDecisionEvent = true;
      const dec = data as DecisionEnvelope;
      if (dec && dec.id) {
        const existing = decisions.find((entry) => entry.id === dec.id);
        if (existing) {
          if ((dec.revision ?? 1) > (existing.revision ?? 1)) {
            revisedNotices[dec.id] = $t('decision.outcome.revised', {
              change: dec.change || $t('decision.changed'),
            });
            decisions = decisions.map((entry) => (entry.id === dec.id ? dec : entry));
          }
        } else {
          decisions = [dec, ...decisions];
        }
        notifyDecision(dec);
      } else {
        void loadDecisions();
      }
    } else if (event === 'decision.revised') {
      const revised = data as DecisionEnvelope;
      if (revised?.id) {
        revisedNotices[revised.id] = $t('decision.outcome.revised', {
          change: revised.change || $t('decision.changed'),
        });
        decisions = decisions.map((entry) => (entry.id === revised.id ? revised : entry));
      }
    } else if (event === 'decision.answered') {
      const answer = data as {
        id?: string;
        decision_id?: string;
        channel?: string;
        device?: string;
        answer?: string;
      };
      const id = answer?.id || answer?.decision_id;
      if (id) {
        const channel =
          answer.channel === 'cli' ? $t('decision.channel.cli') : $t('decision.channel.web');
        const text = $t('decision.outcome.answered_elsewhere', {
          channel,
          device: answer.device || '—',
          answer: answer.answer || '—',
        });
        notices = [{ id: `${id}-answered`, text }, ...notices];
        if (chatDecisionId === id) {
          chatOpen = false;
          chatNotice = $t('decision.chat.closed_elsewhere', {
            id,
            channel,
            device: answer.device || '—',
          });
        }
        decisions = decisions.filter((d) => d.id !== id);
      } else {
        void loadDecisions();
      }
      void refreshDecisionsCount();
    } else if (event === 'decision.closed' || event === 'decision.withdrawn') {
      const closeData = data as { id?: string; decision_id?: string; reason?: string };
      const id = closeData?.id || closeData?.decision_id;
      if (id) {
        decisions = decisions.filter((d) => d.id !== id);
        if (chatDecisionId === id) {
          chatOpen = false;
          chatNotice = $t('decision.chat.withdrawn', {
            id,
            reason: closeData.reason || $t('decision.outcome.withdrawn'),
          });
        }
      } else {
        void loadDecisions();
      }
      void refreshDecisionsCount();
    }
  }

  async function loadDecisions() {
    isLoading = true;
    errorMessage = '';
    try {
      decisions = await apiClient.getDecisions();
      void refreshDecisionsCount();
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('decisions.list.load_failed');
    } finally {
      isLoading = false;
    }
  }

  function toggleEvidence(id: string) {
    showEvidence[id] = !showEvidence[id];
  }

  /** The key of one part of a decision, in the state maps. */
  function keyOf(dec: DecisionEnvelope, part: DecisionPart, index: number): string {
    return `${dec.id}:${part.part_id || index}`;
  }

  function answerOf(dec: DecisionEnvelope, part: DecisionPart, index: number): string {
    return partAnswers[dec.id]?.[part.part_id || String(index)] ?? '';
  }

  function setAnswer(
    dec: DecisionEnvelope,
    part: DecisionPart,
    index: number,
    value: string,
  ): void {
    const partId = part.part_id || String(index);
    partAnswers[dec.id] = { ...(partAnswers[dec.id] ?? {}), [partId]: value };
  }

  /** The 1-based part a dependency string names, if it names one. */
  function dependencyIndex(dec: DecisionEnvelope, part: DecisionPart): number | null {
    if (!part.dependency) return null;
    const match = /(\d+)/.exec(part.dependency);
    if (match) {
      const number = Number.parseInt(match[1], 10);
      if (number >= 1 && number <= dec.parts.length) return number;
    }
    const byId = dec.parts.findIndex((entry) => entry.part_id === part.dependency);
    return byId >= 0 ? byId + 1 : null;
  }

  function partAvailable(dec: DecisionEnvelope, part: DecisionPart, index: number): boolean {
    if (!part.dependency) return true;
    const dependency = dependencyIndex(dec, part);
    if (dependency === null) return true;
    const earlier = dec.parts[dependency - 1];
    if (!earlier) return true;
    return Boolean(answerOf(dec, earlier, dependency - 1));
  }

  /** The answer of the part a dependency names, for the line that names it. */
  function dependencyAnswer(dec: DecisionEnvelope, part: DecisionPart): string {
    const dependency = dependencyIndex(dec, part);
    if (dependency === null) return '';
    const earlier = dec.parts[dependency - 1];
    if (!earlier) return '';
    const answer = answerOf(dec, earlier, dependency - 1);
    const option = earlier.options.find((entry) => entry.key === answer);
    return option?.label ?? answer;
  }

  function selectOption(
    dec: DecisionEnvelope,
    part: DecisionPart,
    index: number,
    key: string,
  ): void {
    setAnswer(dec, part, index, key);
    ownWordsMode[keyOf(dec, part, index)] = false;
    interpretations[keyOf(dec, part, index)] = null;
    reopenDependants(dec, index + 1);
  }

  /** Opens the parts that depend on the one just changed, and drops their answers. */
  function reopenDependants(dec: DecisionEnvelope, changedPart: number): void {
    dec.parts.forEach((entry, index) => {
      if (dependencyIndex(dec, entry) !== changedPart) return;
      const partId = entry.part_id || String(index);
      if (partAnswers[dec.id]?.[partId]) {
        const next = { ...(partAnswers[dec.id] ?? {}) };
        delete next[partId];
        partAnswers[dec.id] = next;
        reopened[dec.id] = { n: index + 1, m: changedPart };
      }
    });
  }

  function allPartsAnswered(dec: DecisionEnvelope): boolean {
    return dec.parts.every((part, index) => {
      if (!partAvailable(dec, part, index)) return false;
      return Boolean(answerOf(dec, part, index));
    });
  }

  function isSubmittingDecision(dec: DecisionEnvelope): boolean {
    return dec.parts.some((part, index) => Boolean(isSubmitting[keyOf(dec, part, index)]));
  }

  function isChatting(dec: DecisionEnvelope): boolean {
    return chatOpen && chatDecisionId === dec.id;
  }

  async function handleWaiver(id: string, action: 'waive' | 'decline') {
    const reason = waiverReasons[id]?.trim() || '';
    if (!reason) return;
    isSubmitting[id] = true;
    try {
      await apiClient.answerDecision(id, { action, reason });
      decisions = decisions.filter((d) => d.id !== id);
      void refreshDecisionsCount();
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('decision.waiver.failed');
    } finally {
      isSubmitting[id] = false;
    }
  }

  async function interpretOwnWords(
    dec: DecisionEnvelope,
    part: DecisionPart,
    index: number,
  ): Promise<void> {
    const key = keyOf(dec, part, index);
    const words = ownWordsTexts[key]?.trim() || '';
    if (!words) return;
    isInterpreting[key] = true;
    try {
      interpretations[key] = await apiClient.interpretDecision(dec.id, words);
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('decision.interpret.failed');
    } finally {
      isInterpreting[key] = false;
    }
  }

  async function submitOwnWordsAnswer(
    dec: DecisionEnvelope,
    part: DecisionPart,
    index: number,
  ): Promise<void> {
    const key = keyOf(dec, part, index);
    const interp = interpretations[key];
    const option = interp?.mapped_option_key || 'own_words';
    isSubmitting[key] = true;
    try {
      if (dec.parts.length > 1) {
        const partId = part.part_id || String(index);
        await apiClient.answerDecision(dec.id, {
          words: ownWordsTexts[key],
          part_answers: { [partId]: option },
          option,
          note: partNotes[key],
        });
      } else {
        await apiClient.answerDecision(dec.id, {
          words: ownWordsTexts[key],
          option,
          note: partNotes[key],
        });
      }
      setAnswer(dec, part, index, option);
      closeDecision(dec.id);
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('decision.answer.failed');
    } finally {
      isSubmitting[key] = false;
    }
  }

  async function submitDecision(dec: DecisionEnvelope): Promise<void> {
    if (!allPartsAnswered(dec)) return;
    const submittingKey = dec.parts[0] ? keyOf(dec, dec.parts[0], 0) : dec.id;
    isSubmitting[submittingKey] = true;
    try {
      if (dec.parts.length === 1) {
        const part = dec.parts[0];
        await apiClient.answerDecision(dec.id, {
          option: answerOf(dec, part, 0),
          note: partNotes[keyOf(dec, part, 0)],
        });
      } else {
        const answers: Record<string, string> = {};
        for (const [index, part] of dec.parts.entries()) {
          const partId = part.part_id || String(index);
          answers[partId] = answerOf(dec, part, index);
        }
        await apiClient.answerDecision(dec.id, { part_answers: answers });
      }
      closeDecision(dec.id);
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('decision.answer.failed');
    } finally {
      isSubmitting[submittingKey] = false;
    }
  }

  async function submitWithdraw(dec: DecisionEnvelope) {
    isSubmitting[dec.id] = true;
    try {
      await apiClient.withdrawDecision(dec.id);
      closeDecision(dec.id);
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('decision.withdraw.failed');
    } finally {
      isSubmitting[dec.id] = false;
    }
  }

  function closeDecision(id: string) {
    decisions = decisions.filter((d) => d.id !== id);
    if (chatDecisionId === id) {
      chatOpen = false;
      chat = null;
    }
    void refreshDecisionsCount();
  }

  function handleLater(id: string) {
    // Decision stays open; operator moves away
    decisions = decisions.filter((d) => d.id !== id);
  }

  function reasonOf(err: unknown): string {
    return err instanceof Error ? err.message : String(err);
  }

  async function openChat(dec: DecisionEnvelope): Promise<void> {
    chatDecisionId = dec.id;
    chatOpen = true;
    chat = null;
    chatNotice = '';
    try {
      chat = await apiClient.getDecisionChat(dec.id);
    } catch (err: unknown) {
      chatNotice = $t('decision.chat.unavailable', { reason: reasonOf(err) });
    }
  }

  async function sendChat(text: string): Promise<void> {
    if (!chatDecisionId) return;
    chatBusy = true;
    try {
      chat = await apiClient.decisionChatTurn(chatDecisionId, text);
    } catch (err: unknown) {
      chatNotice = $t('decision.chat.unavailable', { reason: reasonOf(err) });
    } finally {
      chatBusy = false;
    }
  }

  async function answerChatProposal(accept: boolean): Promise<void> {
    if (!chatDecisionId) return;
    const proposal = chat?.proposal;
    chatBusy = true;
    try {
      chat = await apiClient.answerDecisionChatProposal(chatDecisionId, accept);
      if (accept && proposal?.after) {
        takeProposedAnswer(proposal.after, proposal.applies ?? '');
      }
    } catch (err: unknown) {
      chatNotice = $t('decision.chat.unavailable', { reason: reasonOf(err) });
    } finally {
      chatBusy = false;
    }
  }

  /** Takes a proposed answer as the operator's own words and reads it back. */
  function takeProposedAnswer(words: string, applies: string): void {
    const dec = decisions.find((entry) => entry.id === chatDecisionId);
    if (!dec || dec.parts.length === 0) return;
    const match = /part\s+(\d+)/i.exec(applies);
    const index = match ? Math.min(dec.parts.length - 1, Number.parseInt(match[1], 10) - 1) : 0;
    const part = dec.parts[index];
    const key = keyOf(dec, part, index);
    ownWordsTexts[key] = words;
    ownWordsMode[key] = true;
    interpretations[key] = null;
    chatOpen = false;
    void interpretOwnWords(dec, part, index);
  }

  async function endTheChat(): Promise<void> {
    if (!chatDecisionId) return;
    chatBusy = true;
    try {
      await apiClient.endDecisionChat(chatDecisionId);
    } catch {
      // Closing the panel is the operator's act; a chat the runtime already closed stays closed.
    } finally {
      chatOpen = false;
      chat = null;
      chatNotice = '';
      chatDecisionId = null;
      chatBusy = false;
    }
  }

  /** Whether a costly or irreversible decision keeps its recommendation one step away. */
  function recommendationOnRequest(dec: DecisionEnvelope): boolean {
    return dec.reversibility === 'costly' || dec.reversibility === 'irreversible';
  }

  function recommendationVisible(dec: DecisionEnvelope): boolean {
    return !recommendationOnRequest(dec) || Boolean(revealed[dec.id]);
  }
</script>

<div class="decisions-view">
  <div class="decisions-container">
    <header class="decisions-header">
      <h1 class="decisions-heading">
        {$t('decisions.list.title')}
        {#if decisions.length > 0}
          <span class="unread-pill">{$t('decisions.list.open', { count: decisions.length })}</span>
        {/if}
      </h1>
    </header>

    {#if notifyState === 'default' && !notifyAsked && sawDecisionEvent && decisions.length > 0}
      <div class="notify-banner" data-testid="notify-banner" role="region">
        <p class="notify-banner__text">{$t('decision.notify.ask')}</p>
        <div class="notify-banner__actions">
          <Button variant="secondary" onclick={denyNotifications}>
            {$t('decision.notify.deny')}
          </Button>
          <Button variant="primary" onclick={allowNotifications}>
            {$t('decision.notify.allow')}
          </Button>
        </div>
      </div>
    {/if}
    {#if notifyMessage}
      <p class="notify-message" data-testid="notify-message" role="status">{notifyMessage}</p>
    {/if}

    {#if notices.length > 0}
      <div class="notice-list" data-testid="decision-notices">
        {#each notices as notice (notice.id)}
          <div class="notice-line" role="status">
            <span>{notice.text}</span>
            <button
              type="button"
              class="notice-dismiss"
              aria-label={$t('common.close')}
              onclick={() => (notices = notices.filter((entry) => entry.id !== notice.id))}
            >
              ×
            </button>
          </div>
        {/each}
      </div>
    {/if}

    {#if errorMessage}
      <div class="error-banner" role="alert">
        <p>{errorMessage}</p>
        <Button variant="secondary" size="small" onclick={loadDecisions}
          >{$t('web.common.retry')}</Button
        >
      </div>
    {/if}

    {#if isLoading}
      <div class="loading-state">
        <Spinner size="2.5rem" label={$t('decisions.list.loading')} />
      </div>
    {:else if decisions.length === 0}
      <div class="empty-state">
        <p class="empty-title">
          {$t('decisions.list.empty')}
        </p>
      </div>
    {:else}
      <div class="decisions-list">
        {#each decisions as dec (dec.id)}
          <article class="decision-card" data-decision-id={dec.id}>
            {#if dec.is_waiver || dec.id.startsWith('WR-')}
              <!-- Waiver Request View -->
              <div class="waiver-card">
                <header class="card-header">
                  <h2 class="decision-title">
                    {$t('decision.waiver.title', {
                      plan: dec.subject || $t('decision.waiver.plan_fallback'),
                    })}
                  </h2>
                  <span class="waiting-time">
                    {$t('decision.waiting', {
                      subject: dec.scope || 'plan',
                      time: dec.delivered_at || 'now',
                    })}
                  </span>
                </header>

                <div class="waiver-details">
                  <div class="detail-row">
                    <span class="detail-label">{$t('decision.waiver.gate')}:</span>
                    <span class="detail-value mono"
                      >{dec.waiver_gate ||
                        (dec.evidence?.gate as string) ||
                        $t('decision.waiver.gate_fallback')}</span
                    >
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">{$t('decision.waiver.result')}:</span>
                    <span class="detail-value"
                      >{dec.waiver_result ||
                        (dec.evidence?.result as string) ||
                        $t('decision.waiver.result_failed')}</span
                    >
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">{$t('decision.waiver.asked_by')}:</span>
                    <span class="detail-value"
                      >{dec.asking_phase || 'execute'} · {dec.asking_role || 'implement'}</span
                    >
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">{$t('decision.waiver.its_reason')}:</span>
                    <span class="detail-value">{dec.waiver_reason || dec.description || ''}</span>
                  </div>
                </div>

                <div class="waiver-form">
                  <label for="waiver-reason-{dec.id}" class="form-label">
                    {$t('decision.waiver.reason')}
                  </label>
                  <textarea
                    id="waiver-reason-{dec.id}"
                    class="form-textarea"
                    rows="2"
                    placeholder={$t('decision.waiver.reason_required')}
                    bind:value={waiverReasons[dec.id]}
                  ></textarea>
                  {#if !waiverReasons[dec.id]?.trim()}
                    <span class="form-hint">{$t('decision.waiver.reason_required')}</span>
                  {/if}
                </div>

                <footer class="card-actions">
                  <Button variant="secondary" onclick={() => handleLater(dec.id)}>
                    {$t('decision.waiver.later')}
                  </Button>
                  <Button
                    variant="danger"
                    disabled={!waiverReasons[dec.id]?.trim() || isSubmitting[dec.id]}
                    onclick={() => handleWaiver(dec.id, 'decline')}
                  >
                    {$t('decision.waiver.decline')}
                  </Button>
                  <Button
                    variant="primary"
                    disabled={!waiverReasons[dec.id]?.trim() || isSubmitting[dec.id]}
                    onclick={() => handleWaiver(dec.id, 'waive')}
                  >
                    {$t('decision.waiver.waive')}
                  </Button>
                </footer>
              </div>
            {:else}
              <!-- Standard Decision View -->
              <div class="standard-decision-card">
                <header class="card-header">
                  <h2 class="decision-title">{dec.title}</h2>
                  <div class="badges-row">
                    <span class="badge badge-subject">{dec.subject}</span>
                    <span class="badge badge-phase">{dec.asking_phase} · {dec.asking_role}</span>
                    <span class="badge badge-blocks">{$t(`decision.blocks.${dec.blocks}`)}</span>
                    <span class="badge badge-reversibility badge-{dec.reversibility}">
                      {$t(`decision.reversibility.${dec.reversibility}`)}
                    </span>
                  </div>
                </header>

                {#if dec.description}
                  <p class="decision-description">{dec.description}</p>
                {/if}

                {#if dec.evidence}
                  <div class="evidence-section">
                    <button
                      type="button"
                      class="evidence-toggle"
                      onclick={() => toggleEvidence(dec.id)}
                    >
                      {$t(
                        showEvidence[dec.id] ? 'decision.evidence.hide' : 'decision.evidence.show',
                      )}
                    </button>
                    {#if showEvidence[dec.id]}
                      <div class="evidence-box">
                        <pre class="evidence-json">{JSON.stringify(dec.evidence, null, 2)}</pre>
                      </div>
                    {/if}
                  </div>
                {/if}

                <!-- Parts -->
                {#each dec.parts as part, pIdx (part.part_id || pIdx)}
                  <div class="decision-part">
                    {#if dec.parts.length > 1}
                      <div class="part-position">
                        {$t('decision.part.position', { n: pIdx + 1, total: dec.parts.length })}
                      </div>
                    {/if}

                    <p class="part-question">{part.question}</p>

                    {#if revisedNotices[dec.id]}
                      <p
                        class="decision-revised"
                        data-testid="decision-revised-{dec.id}"
                        role="status"
                      >
                        {revisedNotices[dec.id]}
                      </p>
                    {/if}

                    {#if part.dependency}
                      {#if partAvailable(dec, part, pIdx)}
                        <p class="part-dependency" data-testid="part-dependency-{dec.id}">
                          {$t('decision.part.depends_on', {
                            n: dependencyIndex(dec, part) ?? 0,
                            answer: dependencyAnswer(dec, part),
                          })}
                        </p>
                      {:else}
                        <p class="part-dependency part-dependency--waiting">
                          {$t('decision.part.waiting', {
                            n: dependencyIndex(dec, part) ?? 0,
                          })}
                        </p>
                      {/if}
                    {/if}

                    {#if reopened[dec.id]?.n === pIdx + 1}
                      <p class="part-reopened" data-testid="part-reopened-{dec.id}" role="status">
                        {$t('decision.part.reopened', {
                          n: reopened[dec.id].n,
                          m: reopened[dec.id].m,
                        })}
                      </p>
                    {/if}

                    {#if part.recommendation}
                      {#if recommendationVisible(dec)}
                        <div class="recommendation-banner" data-testid="recommendation-{dec.id}">
                          {$t('decision.recommendation.by', {
                            role: part.recommending_role || dec.asking_role || 'reviewer',
                            option: part.recommendation,
                          })}
                        </div>
                        {#if part.recommendation_rationale}
                          <p class="recommendation-rationale">
                            {$t('decision.recommendation.rationale', {
                              rationale: part.recommendation_rationale,
                            })}
                          </p>
                        {/if}
                      {:else}
                        <div
                          class="recommendation-on-request"
                          data-testid="recommendation-hidden-{dec.id}"
                        >
                          <p class="recommendation-request-text">
                            {$t('decision.recommendation.on_request')}
                          </p>
                          <Button
                            size="small"
                            variant="secondary"
                            onclick={() => (revealed[dec.id] = true)}
                          >
                            {$t('decision.recommendation.show')}
                          </Button>
                        </div>
                      {/if}
                    {/if}

                    <!-- Options list -->
                    {#if partAvailable(dec, part, pIdx)}
                      <div class="options-list" role="radiogroup" aria-label={part.question}>
                        {#each part.options as opt}
                          {@const chosen =
                            answerOf(dec, part, pIdx) === opt.key &&
                            !ownWordsMode[keyOf(dec, part, pIdx)]}
                          <label
                            class="option-card"
                            class:selected={chosen}
                            class:recommended={recommendationVisible(dec) &&
                              part.recommendation === opt.key}
                          >
                            <input
                              type="radio"
                              name="opt-{dec.id}-{pIdx}"
                              value={opt.key}
                              checked={chosen}
                              disabled={isChatting(dec)}
                              onchange={() => selectOption(dec, part, pIdx, opt.key)}
                            />
                            <div class="option-content">
                              <div class="option-label-row">
                                <span class="option-label">{opt.label}</span>
                                {#if recommendationVisible(dec) && part.recommendation === opt.key}
                                  <span class="recommended-tag"
                                    >← {$t('decision.recommendation.marker')}</span
                                  >
                                {/if}
                                {#if opt.quorum_support}
                                  <span class="support-badge">
                                    {$t('decision.support.votes', {
                                      votes: opt.quorum_support.votes_count,
                                      total: opt.quorum_support.total_votes,
                                      models: opt.quorum_support.models_count,
                                    })}
                                    · {$t(`decision.support.band.${opt.quorum_support.band}`)}
                                  </span>
                                {/if}
                              </div>
                              {#if opt.tradeoff_description}
                                <p class="option-tradeoff">{opt.tradeoff_description}</p>
                              {/if}
                              {#if opt.consequences}
                                <p class="option-consequences">{opt.consequences}</p>
                              {/if}
                              {#if opt.quorum_support?.dissent_summary}
                                <blockquote class="option-dissent">
                                  {$t('decision.dissent', {
                                    role: part.recommending_role || 'reviewer',
                                    reason: opt.quorum_support.dissent_summary,
                                  })}
                                </blockquote>
                              {/if}
                            </div>
                          </label>
                        {/each}
                      </div>

                      <!-- Own words section -->
                      {#if part.allow_own_words !== false}
                        <div class="own-words-section">
                          <button
                            type="button"
                            class="btn-toggle-own-words"
                            disabled={isChatting(dec)}
                            onclick={() => {
                              const key = keyOf(dec, part, pIdx);
                              ownWordsMode[key] = !ownWordsMode[key];
                              if (ownWordsMode[key]) {
                                setAnswer(dec, part, pIdx, '');
                              }
                            }}
                          >
                            ✎ {$t('decision.answer.own_words')}
                          </button>

                          {#if ownWordsMode[keyOf(dec, part, pIdx)]}
                            <div class="own-words-box">
                              {#if !interpretations[keyOf(dec, part, pIdx)]}
                                <textarea
                                  class="form-textarea"
                                  rows="3"
                                  placeholder={$t('decision.answer.own_words_placeholder')}
                                  bind:value={ownWordsTexts[keyOf(dec, part, pIdx)]}
                                ></textarea>
                                <div class="own-words-actions">
                                  <Button
                                    size="small"
                                    variant="secondary"
                                    disabled={!ownWordsTexts[keyOf(dec, part, pIdx)]?.trim() ||
                                      isInterpreting[keyOf(dec, part, pIdx)]}
                                    onclick={() => interpretOwnWords(dec, part, pIdx)}
                                  >
                                    {isInterpreting[keyOf(dec, part, pIdx)]
                                      ? $t('decision.interpret.working', {
                                          role: part.recommending_role || 'interpret',
                                        })
                                      : $t('decision.readback.action')}
                                  </Button>
                                </div>
                              {:else}
                                <div class="readback-box">
                                  <p class="readback-text">
                                    {$t('decision.readback.understood', {
                                      reading:
                                        interpretations[keyOf(dec, part, pIdx)]?.read_back_text ||
                                        '',
                                    })}
                                  </p>
                                  <div class="readback-actions">
                                    <Button
                                      size="small"
                                      variant="primary"
                                      onclick={() => submitOwnWordsAnswer(dec, part, pIdx)}
                                    >
                                      {$t('decision.readback.confirm')}
                                    </Button>
                                    <Button
                                      size="small"
                                      variant="secondary"
                                      onclick={() => {
                                        interpretations[keyOf(dec, part, pIdx)] = null;
                                      }}
                                    >
                                      {$t('decision.readback.edit')}
                                    </Button>
                                    <Button
                                      size="small"
                                      variant="secondary"
                                      onclick={() => {
                                        ownWordsMode[keyOf(dec, part, pIdx)] = false;
                                        interpretations[keyOf(dec, part, pIdx)] = null;
                                      }}
                                    >
                                      {$t('decision.readback.choose')}
                                    </Button>
                                  </div>
                                </div>
                              {/if}
                            </div>
                          {/if}
                        </div>
                      {/if}

                      <!-- Optional note -->
                      {#if part.allow_note}
                        <div class="note-section">
                          <label for="note-{dec.id}-{pIdx}" class="form-label">
                            {$t('decision.answer.note')}
                          </label>
                          <input
                            id="note-{dec.id}-{pIdx}"
                            type="text"
                            class="form-input"
                            placeholder={$t('decision.answer.note_placeholder')}
                            bind:value={partNotes[keyOf(dec, part, pIdx)]}
                          />
                        </div>
                      {/if}
                    {/if}
                  </div>
                {/each}

                <footer class="card-actions">
                  <Button
                    variant="primary"
                    disabled={!allPartsAnswered(dec) ||
                      isSubmittingDecision(dec) ||
                      isChatting(dec)}
                    onclick={() => submitDecision(dec)}
                  >
                    {$t('decision.confirm.action')}
                  </Button>
                  {#if !dec.is_waiver}
                    <Button
                      variant="secondary"
                      disabled={isChatting(dec) || isSubmittingDecision(dec)}
                      onclick={() => openChat(dec)}
                    >
                      {$t('decision.answer.chat')}
                    </Button>
                  {/if}
                  <Button
                    variant="secondary"
                    disabled={isSubmittingDecision(dec) || isChatting(dec)}
                    onclick={() => submitWithdraw(dec)}
                  >
                    {$t('decision.answer.none_fits', { subject: dec.scope || 'plan' })}
                  </Button>
                  <Button variant="secondary" onclick={() => handleLater(dec.id)}>
                    {$t('decision.answer.later')}
                  </Button>
                </footer>

                {#if isChatting(dec)}
                  <div class="decision-chat" data-testid="decision-chat-{dec.id}">
                    <ChatPanel
                      open={true}
                      role={chat?.role ?? ''}
                      subject={chat?.subject_id ?? dec.id}
                      turns={chat?.turns ?? []}
                      proposal={chat?.proposal ?? null}
                      available={chat !== null}
                      busy={chatBusy}
                      onSend={sendChat}
                      onEnd={endTheChat}
                      onAccept={() => answerChatProposal(true)}
                      onReject={() => answerChatProposal(false)}
                      onToggle={() => openChat(dec)}
                      acceptLabel={$t('decision.chat.answer_proposed')}
                    />
                    {#if chatNotice}
                      <p
                        class="decision-chat__notice"
                        role="alert"
                        data-testid="decision-chat-notice"
                      >
                        {chatNotice}
                      </p>
                    {:else}
                      <p class="decision-chat__open" role="status">
                        {$t('decision.chat.stays_open', { id: dec.id })}
                      </p>
                    {/if}
                  </div>
                {/if}
              </div>
            {/if}
          </article>
        {/each}
      </div>
    {/if}
  </div>
</div>

<style>
  .decisions-view {
    padding: var(--space-6) var(--space-4);
    display: flex;
    justify-content: center;
  }

  .decisions-container {
    width: 100%;
    max-width: 800px;
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .decisions-header {
    border-bottom: 1px solid var(--color-border-subtle);
    padding-bottom: var(--space-4);
  }

  .decisions-heading {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0;
  }

  .unread-pill {
    font-size: var(--text-xs);
    font-weight: 600;
    padding: 0.15rem 0.5rem;
    border-radius: var(--radius-full);
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
  }

  .error-banner {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-danger-subtle);
    border: 1px solid var(--color-danger-border);
    border-radius: var(--radius-md);
    color: var(--color-danger);
  }

  .loading-state,
  .empty-state {
    padding: var(--space-10) var(--space-4);
    text-align: center;
    color: var(--color-text-muted);
  }

  .decisions-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-6);
  }

  .decision-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    padding: var(--space-5);
  }

  .card-header {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin-bottom: var(--space-4);
  }

  .decision-title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0;
  }

  .waiting-time {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .badges-row {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    align-items: center;
  }

  .badge {
    font-size: var(--text-xs);
    font-weight: 500;
    padding: 0.15rem 0.45rem;
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
  }

  .badge-subject {
    font-family: var(--font-mono);
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
  }

  .badge-reversibility.badge-reversible {
    background-color: var(--color-success-subtle);
    color: var(--color-success);
  }

  .badge-reversibility.badge-costly {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning);
  }

  .badge-reversibility.badge-irreversible {
    background-color: var(--color-danger-subtle);
    color: var(--color-danger);
  }

  .decision-description {
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    margin: 0 0 var(--space-4) 0;
  }

  .evidence-section {
    margin-bottom: var(--space-4);
  }

  .evidence-toggle {
    background: none;
    border: none;
    color: var(--color-primary);
    font-size: var(--text-xs);
    font-weight: 600;
    cursor: pointer;
    padding: 0;
  }

  .evidence-box {
    margin-top: var(--space-2);
    padding: var(--space-3);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    overflow-x: auto;
  }

  .evidence-json {
    margin: 0;
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .decision-part {
    margin-top: var(--space-4);
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-4);
  }

  .part-position {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    margin-bottom: var(--space-1);
  }

  .part-question {
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0 0 var(--space-3) 0;
  }

  .recommendation-banner {
    padding: var(--space-2) var(--space-3);
    background-color: var(--color-primary-subtle);
    border-left: 3px solid var(--color-primary);
    color: var(--color-primary);
    font-size: var(--text-xs);
    font-weight: 600;
    margin-bottom: var(--space-3);
    border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  }

  .options-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    margin-bottom: var(--space-4);
  }

  .option-card {
    display: flex;
    align-items: flex-start;
    gap: var(--space-3);
    padding: var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    cursor: pointer;
    transition:
      border-color 0.15s ease,
      background-color 0.15s ease;
  }

  .option-card:hover {
    background-color: var(--color-bg-subtle);
  }

  .option-card.selected {
    border-color: var(--color-primary);
    background-color: var(--color-primary-subtle);
  }

  .option-card.recommended {
    border-left: 3px solid var(--color-primary);
  }

  .option-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .option-label-row {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .option-label {
    font-weight: 600;
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .recommended-tag {
    font-size: var(--text-xs);
    color: var(--color-primary);
    font-weight: 600;
  }

  .support-badge {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .option-tradeoff,
  .option-consequences {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    margin: 0;
  }

  .option-dissent {
    margin: var(--space-2) 0 0 0;
    padding: var(--space-2);
    background-color: var(--color-warning-subtle);
    border-left: 2px solid var(--color-warning);
    font-size: var(--text-xs);
    color: var(--color-warning);
    font-style: italic;
  }

  .own-words-section {
    margin-top: var(--space-3);
  }

  .btn-toggle-own-words {
    background: none;
    border: none;
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
    font-weight: 600;
    cursor: pointer;
    padding: 0;
  }

  .btn-toggle-own-words:hover {
    color: var(--color-primary);
  }

  .own-words-box {
    margin-top: var(--space-2);
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .readback-box {
    padding: var(--space-3);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    border-left: 3px solid var(--color-primary);
  }

  .readback-text {
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    margin: 0 0 var(--space-2) 0;
  }

  .readback-actions,
  .own-words-actions {
    display: flex;
    gap: var(--space-2);
  }

  .note-section {
    margin-top: var(--space-3);
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .form-label {
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .form-textarea,
  .form-input {
    width: 100%;
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    font-family: inherit;
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    background-color: var(--color-bg-surface);
    box-sizing: border-box;
  }

  .form-textarea:focus,
  .form-input:focus {
    outline: none;
    border-color: var(--color-primary);
  }

  .form-hint {
    font-size: var(--text-xs);
    color: var(--color-warning);
  }

  .card-actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    justify-content: flex-end;
    margin-top: var(--space-5);
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-4);
  }

  /* Waiver view styles */
  .waiver-details {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin-bottom: var(--space-4);
    background-color: var(--color-bg-subtle);
    padding: var(--space-3);
    border-radius: var(--radius-md);
  }

  .detail-row {
    display: flex;
    gap: var(--space-2);
    font-size: var(--text-sm);
  }

  .detail-label {
    font-weight: 600;
    color: var(--color-text-secondary);
    min-width: 5.5rem;
  }

  .detail-value {
    color: var(--color-text-primary);
  }

  .waiver-form {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .notify-banner {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
    padding: var(--space-3);
    border: 1px solid var(--color-primary);
    border-radius: var(--radius-md);
    background-color: var(--color-primary-subtle);
  }

  .notify-banner__text {
    margin: 0;
    font-size: var(--text-sm);
    flex: 1 1 18rem;
  }

  .notify-banner__actions {
    display: flex;
    gap: var(--space-2);
  }

  .notify-message {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .notice-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .notice-line {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-3);
    border-left: 3px solid var(--color-primary);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-sm);
    font-size: var(--text-sm);
  }

  .notice-dismiss {
    background: none;
    border: none;
    color: var(--color-text-secondary);
    cursor: pointer;
    font-size: var(--text-base);
  }

  .decision-revised,
  .part-reopened,
  .part-dependency {
    margin: var(--space-2) 0;
    padding: var(--space-2);
    border-left: 3px solid var(--color-warning);
    background-color: var(--color-warning-subtle);
    color: var(--color-warning);
    font-size: var(--text-xs);
  }

  .part-dependency {
    border-left-color: var(--color-primary);
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
  }

  .part-dependency--waiting {
    border-left-color: var(--color-text-muted);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
  }

  .recommendation-rationale {
    margin: 0 0 var(--space-3) 0;
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .recommendation-on-request {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    margin-bottom: var(--space-3);
  }

  .recommendation-request-text {
    margin: 0;
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    flex: 1 1 14rem;
  }

  .decision-chat {
    margin-top: var(--space-4);
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-3);
  }

  .decision-chat__notice {
    margin: var(--space-2) 0 0 0;
    font-size: var(--text-xs);
    color: var(--color-danger);
  }

  .decision-chat__open {
    margin: var(--space-2) 0 0 0;
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  @media (max-width: 640px) {
    .decisions-view {
      padding: var(--space-3) var(--space-2);
    }

    .card-actions {
      flex-direction: column;
    }
  }
</style>
