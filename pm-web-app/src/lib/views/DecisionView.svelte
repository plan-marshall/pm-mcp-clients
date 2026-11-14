<script lang="ts">
  import { onMount, onDestroy, tick } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import { formatTime } from '../display/format';
  import Button from '../components/Button.svelte';
  import Icon from '../components/Icon.svelte';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import { actFailureText, errorCodeOf } from '../api/errors';
  import ChatPanel from '../components/ChatPanel.svelte';
  import FilterInput from '../components/FilterInput.svelte';
  import {
    apiClient,
    type ChatReadBack,
    type DecisionEnvelope,
    type DecisionPart,
    type DecisionInterpretation,
  } from '../api/client';
  import { runtimeStore, refreshDecisionsCount } from '../stores/runtime';
  import { buildHaystack, matchesHaystack, tokenize } from '../utils/filterText';

  const NOTIFY_KEY = 'pm_decision_notify';

  let decisions = $state<DecisionEnvelope[]>([]);
  let isLoading = $state(true);
  let errorMessage = $state('');
  /** The act that owns the banner, so another act's success cannot clear its failure. */
  let errorOwner: string | null = $state(null);
  /** The code of a failed list read, which `ErrorState` resolves to its words. */
  let loadError = $state<string | null>(null);
  /** Whether the list has been served at least once, so a later failure is an arrival. */
  let hasShownDecisions = $state(false);

  /** The filter query; it runs over the loaded decisions only. */
  let query = $state('');
  /** The decision a command-palette match named; its card is marked in the column. */
  let selectedDecisionId = $state<string | null>(null);

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
  /** The decision whose chat has a request in flight, so another decision's panel stays usable. */
  let chatBusyId = $state<string | null>(null);
  /** The operation that set `chatBusyId`; a late `finally` clears only its own. */
  let chatBusyToken = 0;
  let chatNotice = $state('');
  /**
   * Counts the panel's opens: reopening the same decision makes its id equal again, so the id alone
   * cannot tell a stale load from the one the panel now shows. Every continuation checks the
   * generation it started with, and closing the panel invalidates it.
   */
  let chatGeneration = $state(0);

  /** Invalidates every chat continuation still in flight, as closing the panel does. */
  function invalidateChat(): void {
    chatGeneration += 1;
  }

  let unsubscribeEvents: (() => void) | null = null;
  let isMounted = true;

  /** The normalized query tokens, computed once per keystroke rather than once per decision. */
  const queryTokens = $derived(tokenize(query));

  /** One normalized haystack per loaded decision, rebuilt only when the list changes. */
  const decisionHaystacks = $derived.by(() => {
    const haystacks = new Map<string, string>();
    for (const dec of decisions) {
      haystacks.set(dec.id, buildHaystack(decisionVisibleFields(dec)));
    }
    return haystacks;
  });

  /** The loaded decisions that match the filter, in the served order. */
  const matchingDecisions = $derived.by(() => {
    if (queryTokens.length === 0) return decisions;
    return decisions.filter((dec) =>
      matchesHaystack(queryTokens, decisionHaystacks.get(dec.id) ?? ''),
    );
  });

  // A marked decision the filter hides loses its mark, so it does not come back marked once the
  // filter changes again.
  $effect(() => {
    const selected = selectedDecisionId;
    if (selected !== null && !matchingDecisions.some((dec) => dec.id === selected)) {
      selectedDecisionId = null;
    }
  });

  // A match of the command palette opens the decision it names: the route carries the id, and the
  // request is applied once the list holds that decision, so a late read still lands on it.
  let requestedDecisionId = $state<string | null>(null);
  $effect(() => {
    const requested = $runtimeStore.selectedDecisionId;
    if (requested) {
      requestedDecisionId = requested;
    } else {
      // The route names no decision any more: a request that has not landed yet is dropped, so a
      // later read or `decision.opened` event cannot select a decision the URL no longer names.
      requestedDecisionId = null;
    }
  });
  $effect(() => {
    const requested = requestedDecisionId;
    // While a read is in flight the spinner stands in for the column, so no card could be scrolled
    // to: the request stays pending until the read completes and the column is mounted again.
    if (!requested || isLoading || !decisions.some((decision) => decision.id === requested)) {
      return;
    }
    // A filter that hides the requested decision would consume the request with no card to land
    // on, so the filter is cleared first.
    if (!matchingDecisions.some((decision) => decision.id === requested)) {
      query = '';
    }
    selectedDecisionId = requested;
    requestedDecisionId = null;
    // The request marks the decision's card and brings it into view. A card the cleared filter
    // hid mounts only with the next update, so the query waits for it.
    void tick().then(() => {
      const card = Array.from(document.querySelectorAll<HTMLElement>('[data-decision-id]')).find(
        (element) => element.dataset.decisionId === requested,
      );
      if (typeof card?.scrollIntoView === 'function') {
        card.scrollIntoView({ block: 'start' });
      }
    });
  });

  /**
   * The fields the decision views can show.
   *
   * The card, the waiver card, the option list, and the evidence block all contribute, so a
   * query that matches only an option, the gate, the evidence, or a note finds the decision.
   *
   * @param dec the decision
   * @returns every visible value, for the shared haystack
   */
  function decisionVisibleFields(dec: DecisionEnvelope): Array<string | null | undefined> {
    const values: Array<string | null | undefined> = [
      dec.id,
      dec.title,
      dec.header,
      dec.subject,
      dec.scope,
      dec.asking_phase,
      dec.asking_role,
      dec.blocks,
      $t(`decision.blocks.${dec.blocks}`),
      dec.reversibility,
      $t(`decision.reversibility.${dec.reversibility}`),
      dec.description,
      dec.change,
      dec.waiver_gate,
      dec.waiver_result,
      dec.waiver_reason,
      dec.evidence ? JSON.stringify(dec.evidence) : '',
      dec.delivered_at,
      dec.is_waiver ? $t('decision.waiver.title', { plan: dec.subject ?? '' }) : '',
    ];
    dec.parts.forEach((part: DecisionPart, index: number) => {
      values.push(
        part.question,
        part.recommendation,
        part.recommendation_rationale,
        part.recommending_role,
        part.answer_type,
        part.dependency,
        partNotes[keyOf(dec, part, index)],
      );
      for (const option of part.options) {
        values.push(
          option.key,
          option.label,
          option.tradeoff_description,
          option.consequences,
          option.quorum_support?.dissent_summary,
        );
        if (part.recommendation === option.key) values.push(option.label);
      }
    });
    return values;
  }

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
      // A browser without the API refuses like one that blocks the prompt; the outcome is stored
      // either way, so the banner does not ask again on the next visit.
      if (typeof Notification === 'undefined') throw new Error('no notifications');
      const permission = await Notification.requestPermission();
      notifyState = permission === 'granted' ? 'granted' : 'denied';
      notifyMessage =
        notifyState === 'granted' ? $t('decision.notify.granted') : $t('decision.notify.denied');
    } catch {
      notifyState = 'denied';
      notifyMessage = $t('decision.notify.failed');
    }
    window.localStorage.setItem(NOTIFY_KEY, notifyState);
  }

  function denyNotifications(): void {
    notifyAsked = true;
    notifyState = 'denied';
    notifyMessage = $t('decision.notify.denied');
    window.localStorage.setItem(NOTIFY_KEY, 'denied');
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
    if (subject.startsWith('start ')) {
      return { subject: 'entry', project: subject.replace(/^start (in )?/, ''), kind };
    }
    return { subject: 'other', name: subject.replace(/^plan /, ''), kind };
  }

  /** Fires a desktop notification while the tab is in the background. */
  function notifyDecision(dec: DecisionEnvelope): void {
    if (notifyState !== 'granted') return;
    if (typeof Notification === 'undefined') return;
    if (document.visibilityState === 'visible') return;
    try {
      new Notification($t('decision.notify.title', notificationParams(dec)));
    } catch {
      // A browser that refuses the notification leaves the tab title and the list to say so.
    }
  }

  /**
   * Replaces a decision with its newer revision and drops the answers to the old one.
   *
   * @param revised the newer revision
   * @param change what changed, as `decision.changed` reports it, if it does
   */
  function applyRevision(revised: DecisionEnvelope, change?: string | null): void {
    revisedNotices[revised.id] = $t('decision.outcome.revised', {
      change: change || $t('decision.changed'),
    });
    clearDecisionState(revised.id);
    // A proposal the chat made for the old question must not be taken for the revised one.
    if (chatDecisionId === revised.id) closeChatPanel();
    decisions = decisions.map((entry) => (entry.id === revised.id ? revised : entry));
  }

  /**
   * Counts the times a decision's state was dropped: an interpretation that was asked before the
   * drop and answers after it belongs to a question that no longer stands.
   */
  const stateEpochs: Record<string, number> = {};

  /** Drops every answer, note, interpretation, and submission the decision carried. */
  function clearDecisionState(id: string): void {
    stateEpochs[id] = (stateEpochs[id] ?? 0) + 1;
    const prefix = `${id}:`;
    delete partAnswers[id];
    delete reopened[id];
    for (const map of [ownWordsMode, ownWordsTexts, interpretations, isInterpreting, partNotes]) {
      for (const key of Object.keys(map)) {
        if (key.startsWith(prefix)) delete map[key];
      }
    }
    delete isSubmitting[id];
    for (const key of Object.keys(isSubmitting)) {
      if (key.startsWith(prefix)) delete isSubmitting[key];
    }
  }

  /**
   * The decision a `decision.opened` or `decision.changed` payload carries.
   *
   * The event's `data` holds the decision under `decision`, beside the envelope's `event`, `at` and
   * `scope`; the payload itself is never a decision.
   *
   * @param data the event's data
   * @returns the decision, or `null` when the payload names none
   */
  function decisionOf(data: unknown): DecisionEnvelope | null {
    const decision = (data as { decision?: DecisionEnvelope } | null)?.decision;
    return decision?.id ? decision : null;
  }

  /**
   * The line that ends a decision's chat when the decision was answered elsewhere.
   *
   * The answer names its channel only where it is known; an absent channel stays absent and is never
   * read as one of the two, so the line then says only that the decision is answered.
   *
   * @param id the decision
   * @param channel the channel the answer names, if any
   * @param device the device the answer names, if any
   * @returns the line
   */
  function answeredElsewhereNotice(id: string, channel?: string, device?: string): string {
    if (channel !== 'cli' && channel !== 'web') return $t('decision.chat.answered', { id });
    return $t('decision.chat.closed_elsewhere', {
      id,
      channel: $t(`decision.channel.${channel}`),
      device: device || '—',
    });
  }

  /**
   * Ends the chat of a decision that closed elsewhere and says so.
   *
   * The decision leaves the list with its card, so the line stands in the notices above the list,
   * where the operator finds it instead of the decision. The chat no longer belongs to any decision,
   * so the `decision.closed` that follows a `decision.answered` adds no second line.
   *
   * @param id the decision whose chat ended
   * @param line the line that says why
   */
  function endChatOf(id: string, line: string): void {
    chatOpen = false;
    chat = null;
    chatDecisionId = null;
    invalidateChat();
    notices = [{ id: `${id}-chat`, text: line }, ...notices];
  }

  function handleEvent(event: string, data: unknown) {
    if (!isMounted) return;
    if (event === 'decision.opened') {
      sawDecisionEvent = true;
      const dec = decisionOf(data);
      if (dec) {
        const existing = decisions.find((entry) => entry.id === dec.id);
        if (existing) {
          if (dec.revision > existing.revision) {
            applyRevision(dec);
          }
        } else {
          decisions = [dec, ...decisions];
        }
        notifyDecision(dec);
      } else {
        void loadDecisions();
      }
    } else if (event === 'decision.changed') {
      const revised = decisionOf(data);
      if (revised) {
        if (decisions.some((entry) => entry.id === revised.id)) {
          applyRevision(revised, (data as { change_summary?: string | null }).change_summary);
        } else {
          decisions = [revised, ...decisions];
        }
      } else {
        void loadDecisions();
      }
    } else if (event === 'decision.answered') {
      const answer = data as {
        id?: string;
        channel?: string;
        device?: string;
        answer?: string;
      };
      const id = answer?.id;
      if (id) {
        // The event carries the option key the answer names; the option's label is the word the
        // operator chose. The decision is taken from the list here, before it is dropped below.
        const answered = decisions.find((decision) => decision.id === id);
        const optionKey = answer.answer || '';
        const chosen = answered?.parts
          ?.flatMap((part) => part.options)
          .find((option) => option.key === optionKey);
        const text = $t('decision.outcome.answered', {
          answer: chosen?.label || optionKey || '—',
        });
        notices = [{ id: `${id}-answered`, text }, ...notices];
        if (chatDecisionId === id) {
          endChatOf(id, answeredElsewhereNotice(id, answer.channel, answer.device));
        }
        decisions = decisions.filter((d) => d.id !== id);
      } else {
        void loadDecisions();
      }
      void refreshDecisionsCount();
    } else if (event === 'decision.closed') {
      const closeData = data as { id?: string; outcome?: string; reason?: string };
      const id = closeData?.id;
      if (id) {
        decisions = decisions.filter((d) => d.id !== id);
        if (chatDecisionId === id) {
          // A revision that removed the need for the decision withdraws it; every other outcome
          // (answered, waived, declined) is an answer, and the chat ends with it as answered.
          endChatOf(
            id,
            closeData.outcome === 'withdrawn'
              ? $t('decision.chat.withdrawn', {
                  id,
                  reason: closeData.reason || $t('decision.outcome.withdrawn'),
                })
              : $t('decision.chat.answered', { id }),
          );
        }
      } else {
        void loadDecisions();
      }
      void refreshDecisionsCount();
    }
  }

  /**
   * Shows one act's failure, owning the banner.
   *
   * Two acts of two decisions can be in flight together; the banner belongs to the one that failed,
   * so a later success of the other one does not clear a failure the operator has not seen.
   *
   * @param owner the act that failed
   * @param message the sentence to show
   */
  function showActFailure(owner: string, message: string): void {
    errorOwner = owner;
    errorMessage = message;
  }

  /**
   * Clears the banner when this act owns it.
   *
   * @param owner the act that succeeded
   */
  function clearActFailure(owner: string): void {
    if (errorOwner !== owner) return;
    errorOwner = null;
    errorMessage = '';
  }

  async function loadDecisions() {
    isLoading = true;
    loadError = null;
    // A reload answers for the act the banner reported too: the act's failure is no longer news.
    errorMessage = '';
    errorOwner = null;
    try {
      decisions = await apiClient.getDecisions();
      hasShownDecisions = true;
      void refreshDecisionsCount();
    } catch (err: unknown) {
      loadError = errorCodeOf(err);
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
      clearActFailure(`waiver:${id}`);
      decisions = decisions.filter((d) => d.id !== id);
      void refreshDecisionsCount();
    } catch (err: unknown) {
      showActFailure(`waiver:${id}`, actFailureText(err, $t('decision.waiver.failed')));
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
    const epoch = stateEpochs[dec.id] ?? 0;
    isInterpreting[key] = true;
    try {
      const interpretation = await apiClient.interpretDecision(dec.id, words);
      // A revision dropped this decision's state while the request ran: its answer is stale.
      if ((stateEpochs[dec.id] ?? 0) !== epoch) return;
      interpretations[key] = interpretation;
      clearActFailure(`interpret:${key}`);
    } catch (err: unknown) {
      if ((stateEpochs[dec.id] ?? 0) !== epoch) return;
      showActFailure(`interpret:${key}`, actFailureText(err, $t('decision.interpret.failed')));
    } finally {
      if ((stateEpochs[dec.id] ?? 0) === epoch) isInterpreting[key] = false;
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
    if (dec.parts.length > 1) {
      // Record this part's reading; the Decide button submits all parts together.
      setAnswer(dec, part, index, option);
      reopenDependants(dec, index + 1);
      ownWordsMode[key] = false;
      interpretations[key] = null;
      return;
    }
    isSubmitting[key] = true;
    try {
      await apiClient.answerDecision(dec.id, {
        words: ownWordsTexts[key],
        option,
        note: partNotes[key],
      });
      clearActFailure(`answer:${key}`);
      setAnswer(dec, part, index, option);
      closeDecision(dec.id);
    } catch (err: unknown) {
      showActFailure(`answer:${key}`, actFailureText(err, $t('decision.answer.failed')));
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
        const notes: string[] = [];
        for (const [index, part] of dec.parts.entries()) {
          const partId = part.part_id || String(index);
          answers[partId] = answerOf(dec, part, index);
          const note = partNotes[keyOf(dec, part, index)]?.trim();
          if (note) notes.push(note);
        }
        await apiClient.answerDecision(dec.id, {
          part_answers: answers,
          ...(notes.length > 0 ? { note: notes.join('\n') } : {}),
        });
      }
      clearActFailure(`answer:${submittingKey}`);
      closeDecision(dec.id);
    } catch (err: unknown) {
      showActFailure(`answer:${submittingKey}`, actFailureText(err, $t('decision.answer.failed')));
    } finally {
      isSubmitting[submittingKey] = false;
    }
  }

  async function submitWithdraw(dec: DecisionEnvelope) {
    isSubmitting[dec.id] = true;
    try {
      await apiClient.withdrawDecision(dec.id);
      clearActFailure(`withdraw:${dec.id}`);
      closeDecision(dec.id);
    } catch (err: unknown) {
      showActFailure(`withdraw:${dec.id}`, actFailureText(err, $t('decision.withdraw.failed')));
    } finally {
      isSubmitting[dec.id] = false;
    }
  }

  /**
   * Drops an answered or withdrawn decision from the list.
   *
   * Its chat closes with it, whichever way the answer was reached: the own-words confirmation is
   * disabled while the chat is open (`isChatting`), and a chat that opened after the readback is
   * closed here, so no continuation of it outlives the card.
   *
   * @param id the decision
   */
  function closeDecision(id: string) {
    if (chatDecisionId === id) closeChatPanel();
    decisions = decisions.filter((d) => d.id !== id);
    void refreshDecisionsCount();
  }

  /** Closes the chat panel; a load or answer still in flight must not reopen it. */
  function closeChatPanel(): void {
    chatDecisionId = null;
    chatOpen = false;
    chat = null;
    invalidateChat();
  }

  /**
   * Leaves a decision unanswered: it answers nothing, so the decision stays open and in the list
   * until it is answered, and only the mark a command-palette match gave its card is dropped.
   *
   * @param id the decision the operator leaves
   */
  function handleLater(id: string) {
    if (selectedDecisionId === id) selectedDecisionId = null;
  }

  function reasonOf(err: unknown): string {
    return actFailureText(err, $t('error.unknown.fix'));
  }

  async function openChat(dec: DecisionEnvelope): Promise<void> {
    invalidateChat();
    const generation = chatGeneration;
    chatDecisionId = dec.id;
    chatOpen = true;
    chat = null;
    chatNotice = '';
    try {
      const loaded = await apiClient.getDecisionChat(dec.id);
      // The operator may have switched to another decision's chat, or reopened this one, while
      // this load ran: the answer belongs to the panel it was asked for, never to a newer one.
      if (chatGeneration === generation) chat = loaded;
    } catch (err: unknown) {
      if (chatGeneration === generation) {
        chatNotice = $t('decision.chat.unavailable', { reason: reasonOf(err) });
        chatOpen = false;
      }
    }
  }

  async function sendChat(text: string): Promise<void> {
    const decisionId = chatDecisionId;
    if (!decisionId) return;
    const generation = chatGeneration;
    const token = ++chatBusyToken;
    chatBusyId = decisionId;
    try {
      const answered = await apiClient.decisionChatTurn(decisionId, text);
      if (chatGeneration === generation) chat = answered;
    } catch (err: unknown) {
      if (chatGeneration === generation) {
        chatNotice = $t('decision.chat.unavailable', { reason: reasonOf(err) });
      }
    } finally {
      if (chatBusyToken === token) chatBusyId = null;
    }
  }

  async function answerChatProposal(accept: boolean): Promise<void> {
    if (!chatDecisionId) return;
    // The panel can be closed, or another decision's panel opened, while the answer is in flight,
    // so the decision, its revision and the generation it belongs to are carried past the await:
    // a late answer never replaces another panel's transcript or a revised decision's answer.
    const decisionId = chatDecisionId;
    const generation = chatGeneration;
    const revision = decisions.find((entry) => entry.id === decisionId)?.revision ?? 1;
    const proposal = chat?.proposal;
    const token = ++chatBusyToken;
    chatBusyId = decisionId;
    try {
      const answered = await apiClient.answerDecisionChatProposal(decisionId, accept);
      if (chatGeneration === generation) {
        chat = answered;
      }
      if (accept && proposal?.after) {
        takeProposedAnswer(
          decisionId,
          proposal.after,
          proposal.applies ?? '',
          generation,
          revision,
        );
      }
    } catch (err: unknown) {
      if (chatGeneration === generation) {
        chatNotice = $t('decision.chat.unavailable', { reason: reasonOf(err) });
      }
    } finally {
      if (chatBusyToken === token) chatBusyId = null;
    }
  }

  /** Takes a proposed answer as the operator's own words and reads it back. */
  function takeProposedAnswer(
    decisionId: string,
    words: string,
    applies: string,
    generation: number,
    revision: number,
  ): void {
    const dec = decisions.find((entry) => entry.id === decisionId);
    if (!dec || dec.parts.length === 0) return;
    // Only the chat this answer belongs to closes: a panel the operator opened meanwhile stays.
    if (chatGeneration === generation && chatDecisionId === decisionId) {
      chatOpen = false;
      invalidateChat();
    }
    // An answer to an earlier revision of the decision does not apply to its current revision.
    if ((dec.revision ?? 1) !== revision) return;
    const match = /part\s+(\d+)/i.exec(applies);
    const index = match
      ? Math.max(0, Math.min(dec.parts.length - 1, Number.parseInt(match[1], 10) - 1))
      : 0;
    const part = dec.parts[index];
    const key = keyOf(dec, part, index);
    ownWordsTexts[key] = words;
    ownWordsMode[key] = true;
    interpretations[key] = null;
    void interpretOwnWords(dec, part, index);
  }

  async function endTheChat(): Promise<void> {
    if (!chatDecisionId) return;
    // The panel can be left while the chat is ended, and another decision's chat opened meanwhile:
    // only the chat this act ended is closed when it answers, and only the open it belonged to.
    const decisionId = chatDecisionId;
    const generation = chatGeneration;
    const token = ++chatBusyToken;
    chatBusyId = decisionId;
    try {
      await apiClient.endDecisionChat(decisionId);
    } catch {
      // Closing the panel is the operator's act; a chat the runtime already closed stays closed.
    } finally {
      if (chatGeneration === generation && chatDecisionId === decisionId) {
        chatOpen = false;
        chat = null;
        chatNotice = '';
        chatDecisionId = null;
        invalidateChat();
      }
      if (chatBusyToken === token) {
        chatBusyId = null;
      }
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

{#snippet decisionCard(dec: DecisionEnvelope)}
  <article
    class="decision-card"
    class:decision-card--selected={selectedDecisionId === dec.id}
    aria-current={selectedDecisionId === dec.id ? 'true' : undefined}
    data-decision-id={dec.id}
  >
    {#if dec.is_waiver}
      <!-- Waiver Request View -->
      <div class="waiver-card">
        <header class="card-header">
          <h2 class="decision-title">
            {$t('decision.waiver.title', {
              plan: dec.subject || $t('decision.waiver.plan_fallback'),
            })}
          </h2>
          {#if dec.delivered_at}
            <span class="waiting-time">
              {$t('decision.waiting', {
                subject: dec.scope,
                time: formatTime(dec.delivered_at, 'short', $currentLocale),
              })}
            </span>
          {/if}
        </header>

        <div class="waiver-details">
          <div class="detail-row">
            <span class="detail-label">{$t('decision.waiver.gate')}:</span>
            <span class="detail-value mono"
              >{dec.waiver_gate || $t('decision.waiver.gate_fallback')}</span
            >
          </div>
          <div class="detail-row">
            <span class="detail-label">{$t('decision.waiver.result')}:</span>
            <span class="detail-value"
              >{dec.waiver_result || $t('decision.waiver.result_failed')}</span
            >
          </div>
          <div class="detail-row">
            <span class="detail-label">{$t('decision.waiver.asked_by')}:</span>
            <span class="detail-value"
              >{[dec.asking_phase, dec.asking_role].filter(Boolean).join(' · ')}</span
            >
          </div>
          <div class="detail-row">
            <span class="detail-label">{$t('decision.waiver.its_reason')}:</span>
            <span class="detail-value" data-model-text>{dec.waiver_reason || dec.description || ''}</span>
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
          <Button
            variant="secondary"
            onclick={() => handleLater(dec.id)}
            data-advisory={$t('decision.waiver.later.title')}
          >
            {$t('decision.waiver.later')}
          </Button>
          <Button
            variant="danger"
            disabled={!waiverReasons[dec.id]?.trim() || isSubmitting[dec.id]}
            onclick={() => handleWaiver(dec.id, 'decline')}
            data-advisory={$t('decision.waiver.decline.title')}
          >
            {$t('decision.waiver.decline')}
          </Button>
          <Button
            variant="primary"
            disabled={!waiverReasons[dec.id]?.trim() || isSubmitting[dec.id]}
            onclick={() => handleWaiver(dec.id, 'waive')}
            data-advisory={$t('decision.waiver.waive.title')}
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

        {#if dec.description && dec.description !== dec.title}
          <p class="decision-description" data-model-text>{dec.description}</p>
        {/if}

        {#if revisedNotices[dec.id]}
          <p class="decision-revised" data-testid="decision-revised-{dec.id}" role="status">
            {revisedNotices[dec.id]}
          </p>
        {/if}

        {#if dec.evidence}
          <div class="evidence-section">
            <button
              type="button"
              class="evidence-toggle"
              data-advisory={$t(
                showEvidence[dec.id]
                  ? 'decision.evidence.hide.title'
                  : 'decision.evidence.show.title',
              )}
              onclick={() => toggleEvidence(dec.id)}
            >
              {$t(showEvidence[dec.id] ? 'decision.evidence.hide' : 'decision.evidence.show')}
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
                <div class="recommendation-on-request" data-testid="recommendation-hidden-{dec.id}">
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
                    answerOf(dec, part, pIdx) === opt.key && !ownWordsMode[keyOf(dec, part, pIdx)]}
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
                    data-advisory={$t('decision.answer.own_words.title')}
                    onclick={() => {
                      const key = keyOf(dec, part, pIdx);
                      ownWordsMode[key] = !ownWordsMode[key];
                      if (ownWordsMode[key]) {
                        setAnswer(dec, part, pIdx, '');
                      }
                    }}
                  >
                    <Icon name="act.edit" size={16} /> {$t('decision.answer.own_words')}
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
                                interpretations[keyOf(dec, part, pIdx)]?.read_back_text || '',
                            })}
                          </p>
                          <div class="readback-actions">
                            <Button
                              size="small"
                              variant="primary"
                              data-advisory={$t('decision.readback.confirm.title')}
                              disabled={isChatting(dec)}
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
            disabled={!allPartsAnswered(dec) || isSubmittingDecision(dec) || isChatting(dec)}
            onclick={() => submitDecision(dec)}
            data-advisory={$t('decision.confirm.action.title')}
          >
            {$t('decision.confirm.action')}
          </Button>
          {#if !dec.is_waiver}
            <Button
              variant="secondary"
              disabled={isChatting(dec) || isSubmittingDecision(dec)}
              onclick={() => openChat(dec)}
              data-advisory={$t('decision.answer.chat.title')}
            >
              {$t('decision.answer.chat')}
            </Button>
          {/if}
          <Button
            variant="secondary"
            disabled={isSubmittingDecision(dec) || isChatting(dec)}
            onclick={() => submitWithdraw(dec)}
            data-advisory={$t('decision.answer.none_fits.title', { subject: dec.scope })}
          >
            {$t('decision.answer.none_fits', { subject: dec.scope })}
          </Button>
          <Button
            variant="secondary"
            onclick={() => handleLater(dec.id)}
            data-advisory={$t('decision.answer.later.title')}
          >
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
              busy={chatBusyId === dec.id}
              onSend={sendChat}
              onEnd={endTheChat}
              onAccept={() => answerChatProposal(true)}
              onReject={() => answerChatProposal(false)}
              onClose={closeChatPanel}
              acceptLabel={$t('decision.chat.answer_proposed')}
            />
            <p class="decision-chat__open" role="status">
              {$t('decision.chat.stays_open', { id: dec.id })}
            </p>
          </div>
        {/if}
        {#if chatNotice && chatDecisionId === dec.id}
          <p class="decision-chat__notice" role="alert" data-testid="decision-chat-notice">
            {chatNotice}
          </p>
        {/if}
      </div>
    {/if}
  </article>
{/snippet}

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
          <Button
            variant="secondary"
            onclick={denyNotifications}
            data-advisory={$t('decision.notify.deny.title')}
          >
            {$t('decision.notify.deny')}
          </Button>
          <Button
            variant="primary"
            onclick={allowNotifications}
            data-advisory={$t('decision.notify.allow.title')}
          >
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
              data-advisory={$t('decision.notice.dismiss.title')}
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
          >{$t('common.action.retry')}</Button
        >
      </div>
    {/if}

    {#if loadError !== null && decisions.length > 0}
      <div class="error-banner" data-testid="decisions-error">
        <ErrorState code={loadError} actionLabel={$t('common.action.retry')} onAct={loadDecisions} />
      </div>
    {/if}

    {#if isLoading}
      <div class="loading-state" data-testid="decisions-loading">
        <Skeleton shape="card" rows={3} label={$t('decisions.list.loading')} />
      </div>
    {:else if loadError !== null && decisions.length === 0}
      <div class="error-banner" data-testid="decisions-error">
        <ErrorState
          code={loadError}
          actionLabel={$t('common.action.retry')}
          onAct={loadDecisions}
          announce={hasShownDecisions}
        />
      </div>
    {:else if decisions.length === 0}
      <div class="empty-state">
        <p class="empty-title">
          {$t('decisions.list.empty')}
        </p>
      </div>
    {:else}
      <FilterInput
        value={query}
        label={$t('decisions.filter.label')}
        placeholder={$t('decisions.filter.placeholder')}
        resultCount={matchingDecisions.length}
        onchange={(value) => (query = value)}
      />

      {#if matchingDecisions.length === 0}
        <div class="empty-state" data-testid="decisions-filter-empty">
          <p class="empty-title">{$t('decisions.filter.no_matches')}</p>
        </div>
      {:else}
        <div class="decisions-list">
          {#each matchingDecisions as dec (dec.id)}
            {@render decisionCard(dec)}
          {/each}
        </div>
      {/if}
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
    scroll-margin-top: var(--space-4);
  }

  /* The decision a command-palette match named: a primary left border marks it in the column. */
  .decision-card--selected {
    border-color: var(--color-primary);
    box-shadow:
      inset 3px 0 0 var(--color-primary),
      var(--shadow-sm);
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
    color: var(--color-success-text);
  }

  .badge-reversibility.badge-costly {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning-text);
  }

  .badge-reversibility.badge-irreversible {
    background-color: var(--color-danger-subtle);
    color: var(--color-danger-text);
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
    color: var(--color-warning-text);
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
    color: var(--color-warning-text);
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
