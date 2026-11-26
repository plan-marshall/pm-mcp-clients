<script lang="ts">
  import { onMount, onDestroy, tick } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import { formatTime } from '../display/format';
  import Button from '../components/Button.svelte';
  import Chip from '../components/Chip.svelte';
  import CountBadge from '../components/CountBadge.svelte';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import FilterInput from '../components/FilterInput.svelte';
  import FormStatus from '../components/FormStatus.svelte';
  import RowLink from '../components/RowLink.svelte';
  import Link from '../components/Link.svelte';
  import RadioGroup, { type RadioOption } from '../components/RadioGroup.svelte';
  import CheckGroup, { type CheckOption } from '../components/CheckGroup.svelte';
  import TextArea from '../components/TextArea.svelte';
  import PhaseLabel from '../components/PhaseLabel.svelte';
  import EvidenceView from '../components/EvidenceView.svelte';
  import ChatPanel from '../components/ChatPanel.svelte';
  import TriagePage, { type Level, type PinnedRow } from '../templates/TriagePage.svelte';
  import { actFailureText, errorCodeOf } from '../api/errors';
  import {
    apiClient,
    type ChatReadBack,
    type DecisionEnvelope,
    type DecisionPart,
    type DecisionInterpretation,
  } from '../api/client';
  import {
    runtimeStore,
    refreshDecisionsCount,
  } from '../stores/runtime';
  import { writeAddress } from '../navigation/history';
  import { buildHaystack, matchesHaystack, tokenize } from '../utils/filterText';

  /**
   * Decisions as the triage list of `decisions.adoc`: the open decisions beside the one that is
   * shown, in the column engine of `navigation.adoc#lenses`.
   *
   * The list is the lens's column and the decision its object column, at `/decisions/{id}`. A row
   * carries the subject as a chip, the phase, and one detail line of the path, what it blocks, how
   * reversible it is and since when it waits; the description stands in the detail alone, never
   * twice. The rows are grouped by project, each head with its count, and they are ordered blocking
   * first and then oldest — the order the runtime serves, made visible (`#order`).
   *
   * `Enter` decides a decision whose option is chosen. The answer line is the form's `FormStatus`,
   * never a toast, and after an answer the next decision stands in the object column by a
   * `replaceState`, so the history does not fill with decisions the operator has answered.
   */

  const NOTIFY_KEY = 'pm_decision_notify';

  /** An in-place rendering of one decision: the object alone, beside its plan (`open=decision:{id}`). */
  interface DecisionInPlace {
    /** The decision the column shows. */
    id: string;
    /** The plan whose head opened it: a decision of another plan is not this column's. */
    planId: string;
    /** Closes the column: the history's back, or the caller's address when the entry was loaded. */
    onClose: () => void;
  }

  let { inPlace = null }: { inPlace?: DecisionInPlace | null } = $props();

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
  /** The decision the object column shows, or `null` for the list alone. */
  let activeId = $state<string | null>(null);
  /** The route value the view last synced, so it follows the address and does not fight it. */
  let syncedRouteDecision: string | null | undefined;

  /** The line the last answer produced: the form's `FormStatus`, not a toast. */
  let outcome = $state<{ tone: 'neutral' | 'success' | 'danger'; text: string } | null>(null);

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
  /**
   * The decisions whose chat has a request in flight, with the token of the operation that holds it.
   *
   * Busy is tracked per decision: finishing decision B's turn must not clear A's marker while A's
   * request is still in flight. The operation token stays, per decision: an older operation's
   * `finally` clears only a marker its own token set, so it never drops the newer one's.
   */
  let chatBusy = $state<Record<string, number>>({});
  /** The operation that last set a busy marker; a late `finally` clears only its own token. */
  let chatBusyToken = 0;
  let chatNotice = $state('');
  /**
   * Counts the panel's opens: reopening the same decision makes its id equal again, so the id alone
   * cannot tell a stale load from the one the panel now shows. Every continuation checks the
   * generation it started with, and closing the panel invalidates it.
   */
  let chatGeneration = $state(0);

  /**
   * The request ids this client sent that the runtime has not answered yet, by decision. A
   * `decision.answered` whose `request_id` is one of these is this client's own answer; every other
   * one was answered elsewhere (`#outcomes`).
   */
  const ownRequests = new Map<string, string>();

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

  /** The loaded decisions that match the filter. */
  const matchingDecisions = $derived.by(() =>
    queryTokens.length === 0
      ? decisions
      : decisions.filter((dec) =>
          matchesHaystack(queryTokens, decisionHaystacks.get(dec.id) ?? ''),
        ),
  );

  /** The matching decisions in the order the list shows them: blocking first, then oldest. */
  const orderedDecisions = $derived.by(() =>
    [...matchingDecisions].sort((left, right) => {
      const blocking = Number(isBlocking(right)) - Number(isBlocking(left));
      if (blocking !== 0) return blocking;
      const leftAt = left.delivered_at ? Date.parse(left.delivered_at) : Number.POSITIVE_INFINITY;
      const rightAt = right.delivered_at ? Date.parse(right.delivered_at) : Number.POSITIVE_INFINITY;
      if (leftAt !== rightAt) return leftAt - rightAt;
      return left.id.localeCompare(right.id);
    }),
  );

  /** One group of the list: a project and its open decisions. */
  interface DecisionGroup {
    key: string;
    title: string;
    items: DecisionEnvelope[];
  }

  /** The matching decisions grouped by the project they belong to, in the list's order. */
  const groups = $derived.by<DecisionGroup[]>(() => {
    const byProject = new Map<string, DecisionEnvelope[]>();
    for (const dec of orderedDecisions) {
      const key = dec.project ?? '';
      byProject.set(key, [...(byProject.get(key) ?? []), dec]);
    }
    return [...byProject.entries()].map(([key, items]) => ({
      key,
      title: key === '' ? $t('decisions.list.other_group') : key,
      items,
    }));
  });

  /** The decisions in the order the list draws them: group after group, as the page shows them. */
  const displayedDecisions = $derived.by(() => groups.flatMap((group) => group.items));

  /** The decision the object column shows, or `null` while the list stands alone. */
  const activeDecision = $derived(decisions.find((dec) => dec.id === activeId) ?? null);

  /** The decisions the list has served, so one that never arrived is not mistaken for one that left. */
  const seenIds = new Set<string>();

  /** The chain the column engine draws: the list, and the decision beside it once one is open. */
  const levels = $derived.by<Level[]>(() => {
    const list: Level = {
      key: 'decisions',
      kind: 'decision',
      title: $t('decisions.list.title'),
      href: '/decisions',
      state: '',
    };
    if (activeDecision === null) {
      return [list];
    }
    const decisionLevel: Level = {
      key: `decision:${activeDecision.id}`,
      kind: 'decision',
      title: displayTitle(activeDecision),
      href: `/decisions/${encodeURIComponent(activeDecision.id)}`,
      state: '',
    };
    // In place the decision is the whole chain: no list column stands beside it.
    if (inPlace) {
      return [decisionLevel];
    }
    return [list, decisionLevel];
  });

  /**
   * A selection the filter hides stays visible as one pinned row at the top of the list
   * (`navigation.adoc#missing`), so the object column never stands beside a list that lost it.
   */
  const pinned = $derived.by<PinnedRow | null>(() => {
    if (inPlace) return null;
    const active = activeDecision;
    if (active === null) return null;
    if (matchingDecisions.some((dec) => dec.id === active.id)) return null;
    return {
      level: {
        key: `decision:${active.id}`,
        kind: 'decision',
        title: displayTitle(active),
        href: `/decisions/${encodeURIComponent(active.id)}`,
        state: '',
      },
      reason: 'filtered',
    };
  });

  /**
   * @param dec a decision
   * @returns whether it blocks its subject, which the list orders first
   */
  function isBlocking(dec: DecisionEnvelope): boolean {
    return dec.blocks !== '' && dec.blocks !== 'none' && dec.blocks !== 'task';
  }

  /**
   * @param dec a decision
   * @returns the title the row and the object column show
   */
  function displayTitle(dec: DecisionEnvelope): string {
    return dec.is_waiver
      ? $t('decision.waiver.title', {
          plan: dec.subject || $t('decision.waiver.plan_fallback'),
        })
      : dec.title;
  }

  /**
   * @param dec a decision
   * @returns the path the row names: project and plan, then its subject
   */
  function pathOf(dec: DecisionEnvelope): string {
    const parts = [dec.project, dec.plan].filter(
      (part): part is string => typeof part === 'string' && part.length > 0,
    );
    return parts.length > 0 ? parts.join(' › ') : dec.subject;
  }

  /**
   * @param dec a decision
   * @returns the address of its subject's own view, or `''` where the project offers none
   */
  function subjectHref(dec: DecisionEnvelope): string {
    if (dec.plan) return `/plans/${encodeURIComponent(dec.plan)}`;
    if (dec.scope === 'epic' && dec.subject.startsWith('epic ')) {
      return `/epics/${encodeURIComponent(dec.subject.slice('epic '.length))}`;
    }
    if (dec.project) return `/projects/${encodeURIComponent(dec.project)}`;
    return '';
  }

  /**
   * @param dec a decision
   * @returns the one line under the row: the path, what it blocks, how reversible it is, since when
   */
  function rowDetail(dec: DecisionEnvelope): string {
    const pieces = [pathOf(dec), $t(`decision.blocks.${dec.blocks}`)];
    pieces.push($t(`decision.reversibility.${dec.reversibility}`));
    if (dec.delivered_at) {
      pieces.push(
        $t('decisions.list.since', {
          time: formatTime(dec.delivered_at, 'short', $currentLocale),
        }),
      );
    }
    return pieces.join(' · ');
  }

  /**
   * The fields the decision views can show.
   *
   * The row, the waiver card, the option list, and the evidence block all contribute, so a query
   * that matches only an option, the gate, or the evidence finds the decision.
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
      dec.project,
      dec.plan,
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
    const requested = inPlace?.id ?? $runtimeStore.selectedDecisionId ?? null;
    activeId = requested;
    syncedRouteDecision = requested;
    await loadDecisions();
  });

  onDestroy(() => {
    isMounted = false;
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  // The route is the address: a header visit to Decisions, the browser's Back, and a decision the
  // palette opened all move the store, and the object column follows it.
  $effect(() => {
    // In place the address is the caller's, not a decision's: the column follows the prop alone.
    if (inPlace) return;
    const route = $runtimeStore.currentRoute;
    if (route !== 'decisions' && route !== 'decision') return;
    const routeDecision = $runtimeStore.selectedDecisionId ?? null;
    if (routeDecision === syncedRouteDecision) return;
    syncedRouteDecision = routeDecision;
    activeId = routeDecision;
  });

  // A decision that left the list cannot stay in the object column: the view falls back to the list
  // and says so through the address, replacing the entry so Back does not return into it. A decision
  // the list has not served yet keeps its place: its request waits for the next read.
  $effect(() => {
    const active = activeId;
    if (active === null || isLoading || decisions.length === 0) return;
    if (decisions.some((dec) => dec.id === active)) return;
    if (!seenIds.has(active)) return;
    // In place the effect above owns the close, so the two never move the history in one update.
    if (inPlace) return;
    closeDetail();
  });

  // An in-place address that names no open decision of this plan — stale, answered elsewhere, or of
  // another plan — has nothing to show: it closes back to the caller instead of leaving a skeleton
  // that never resolves (`plan-view-interaction.adoc#changes`'s rule, applied to this address). A
  // failed read is not a missing decision: it keeps the column open and shows the error.
  $effect(() => {
    if (!inPlace || isLoading || loadError !== null) return;
    if (decisions.some((dec) => dec.id === inPlace.id && dec.plan === inPlace.planId)) return;
    inPlace.onClose();
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

  /**
   * @param dec a decision
   * @param optionKey the option key an answer names
   * @param part the part the option belongs to, where the caller knows it
   * @returns the option's own label, or the key where the decision does not carry it
   */
  function optionLabelOf(
    dec: DecisionEnvelope | undefined,
    optionKey: string,
    part?: DecisionPart,
  ): string {
    const scoped = part?.options.find((entry) => entry.key === optionKey)?.label;
    if (scoped) return scoped;
    const option = dec?.parts.flatMap((entry) => entry.options).find((e) => e.key === optionKey);
    return option?.label || optionKey;
  }

  /**
   * @param id the decision an answer names
   * @param requestId the request id the answering client sent, where it sent one
   * @returns whether this client sent that answer
   */
  function isOwnAnswer(id: string, requestId?: string | null, channel?: string): boolean {
    // An answer the runtime names as the console's is never this client's, whatever the id says.
    if (channel === 'cli') return false;
    const pending = ownRequests.get(id);
    if (pending === undefined) return false;
    return requestId ? pending === requestId : true;
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
          seenIds.add(dec.id);
        }
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
          seenIds.add(revised.id);
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
        request_id?: string;
      };
      const id = answer?.id;
      if (id) {
        // The event carries the option key the answer names; the option's label is the word the
        // operator chose. The decision is taken from the list here, before it is dropped below.
        const answered = decisions.find((decision) => decision.id === id);
        const optionKey = answer.answer || '';
        const label = optionLabelOf(answered, optionKey);
        if (isOwnAnswer(id, answer.request_id, answer.channel)) {
          outcome = { tone: 'success', text: $t('decision.outcome.answered', { answer: label }) };
        } else {
          outcome = {
            tone: 'neutral',
            text: $t('decision.outcome.answered_elsewhere', {
              channel:
                answer.channel === 'cli' || answer.channel === 'web'
                  ? $t(`decision.channel.${answer.channel}`)
                  : $t('decision.channel.other'),
              device: answer.device || '—',
              answer: label,
            }),
          };
        }
        ownRequests.delete(id);
        if (chatDecisionId === id) {
          endChatOf(id, answeredElsewhereNotice(id, answer.channel, answer.device));
        }
        removeDecision(id);
      } else {
        void loadDecisions();
      }
      void refreshDecisionsCount();
    } else if (event === 'decision.closed') {
      const closeData = data as { id?: string; outcome?: string; reason?: string };
      const id = closeData?.id;
      if (id) {
        ownRequests.delete(id);
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
        removeDecision(id);
      } else {
        void loadDecisions();
      }
      void refreshDecisionsCount();
    }
  }

  /**
   * Shows one act's failure, owning the banner.
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

  /**
   * Reads the open decisions again.
   *
   * @param keepBanner whether another act's failure stays on screen; the reload that follows a
   *   successful answer reconciles the list and is not news about that failure
   */
  async function loadDecisions(keepBanner = false) {
    isLoading = true;
    loadError = null;
    // A reload answers for the act the banner reported too: the act's failure is no longer news.
    if (!keepBanner) {
      errorMessage = '';
      errorOwner = null;
    }
    try {
      decisions = await apiClient.getDecisions();
      for (const dec of decisions) seenIds.add(dec.id);
      hasShownDecisions = true;
      void refreshDecisionsCount();
    } catch (err: unknown) {
      loadError = errorCodeOf(err);
    } finally {
      isLoading = false;
    }
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
    return optionLabelOf(dec, answer, earlier);
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

  /** A new client request id, so the answered event can be attributed to this client's answer. */
  function newRequestId(): string {
    const cryptoApi = globalThis.crypto;
    if (cryptoApi?.randomUUID) return cryptoApi.randomUUID();
    return `web-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }

  async function handleWaiver(id: string, action: 'waive' | 'decline') {
    const reason = waiverReasons[id]?.trim() || '';
    if (!reason) return;
    const requestId = newRequestId();
    ownRequests.set(id, requestId);
    isSubmitting[id] = true;
    try {
      await apiClient.answerDecision(id, { action, reason, request_id: requestId });
      clearActFailure(`waiver:${id}`);
      // The list follows the answer even when the stream is down: the runtime no longer lists it.
      void loadDecisions(true);
    } catch (err: unknown) {
      ownRequests.delete(id);
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
    const requestId = newRequestId();
    ownRequests.set(dec.id, requestId);
    isSubmitting[key] = true;
    try {
      await apiClient.answerDecision(dec.id, {
        words: ownWordsTexts[key],
        option,
        note: partNotes[key],
        request_id: requestId,
      });
      clearActFailure(`answer:${key}`);
      setAnswer(dec, part, index, option);
      void loadDecisions(true);
    } catch (err: unknown) {
      ownRequests.delete(dec.id);
      showActFailure(`answer:${key}`, actFailureText(err, $t('decision.answer.failed')));
    } finally {
      isSubmitting[key] = false;
    }
  }

  async function submitDecision(dec: DecisionEnvelope): Promise<void> {
    if (!allPartsAnswered(dec)) return;
    const submittingKey = dec.parts[0] ? keyOf(dec, dec.parts[0], 0) : dec.id;
    const requestId = newRequestId();
    ownRequests.set(dec.id, requestId);
    isSubmitting[submittingKey] = true;
    try {
      if (dec.parts.length === 1) {
        const part = dec.parts[0];
        await apiClient.answerDecision(dec.id, {
          option: answerOf(dec, part, 0),
          note: partNotes[keyOf(dec, part, 0)],
          request_id: requestId,
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
          request_id: requestId,
        });
      }
      clearActFailure(`answer:${submittingKey}`);
      void loadDecisions(true);
    } catch (err: unknown) {
      ownRequests.delete(dec.id);
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
      void loadDecisions(true);
    } catch (err: unknown) {
      showActFailure(`withdraw:${dec.id}`, actFailureText(err, $t('decision.withdraw.failed')));
    } finally {
      isSubmitting[dec.id] = false;
    }
  }

  /**
   * Drops a decision that the runtime reports as answered or closed, and moves on.
   *
   * The next decision stands in the object column by a `replaceState`, so the history does not fill
   * with decisions the operator has answered (`#order`); where none is left, the list stands alone.
   * The removal follows the runtime's event, never the answer's response: the event carries the
   * option's label and whether the answer was this client's own (`#outcomes`).
   *
   * @param id the decision that left
   */
  function removeDecision(id: string): void {
    const ordered = displayedDecisions;
    const index = ordered.findIndex((dec) => dec.id === id);
    const successor =
      index >= 0
        ? (ordered.slice(index + 1).find((dec) => dec.id !== id) ??
          ordered.slice(0, Math.max(index, 0)).find((dec) => dec.id !== id) ??
          null)
        : (ordered.find((dec) => dec.id !== id) ?? null);
    seenIds.delete(id);
    decisions = decisions.filter((dec) => dec.id !== id);
    if (activeId === id) {
      selectDecision(successor?.id ?? null, 'replace');
    }
  }

  /** Closes the chat panel; a load or answer still in flight must not reopen it. */
  function closeChatPanel(): void {
    chatDecisionId = null;
    chatOpen = false;
    chat = null;
    invalidateChat();
  }

  /**
   * Leaves a decision unanswered: it answers nothing, so the decision stays open and in the list.
   *
   * @param id the decision the operator leaves
   */
  function handleLater(id: string) {
    outcome = null;
    if (activeId === id) closeDetail();
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

  /**
   * Holds one decision's chat busy, under this operation's token.
   *
   * @param id the decision whose chat has a request in flight
   * @returns the release of this operation's hold
   */
  function holdChatBusy(id: string): () => void {
    const token = ++chatBusyToken;
    chatBusy = { ...chatBusy, [id]: token };
    return () => {
      // A later operation on the same decision owns the marker now; its own release clears it.
      if (chatBusy[id] !== token) return;
      const next = { ...chatBusy };
      delete next[id];
      chatBusy = next;
    };
  }

  async function sendChat(text: string): Promise<void> {
    const decisionId = chatDecisionId;
    if (!decisionId) return;
    const generation = chatGeneration;
    const release = holdChatBusy(decisionId);
    try {
      const answered = await apiClient.decisionChatTurn(decisionId, text);
      if (chatGeneration === generation) chat = answered;
    } catch (err: unknown) {
      if (chatGeneration === generation) {
        chatNotice = $t('decision.chat.unavailable', { reason: reasonOf(err) });
      }
    } finally {
      release();
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
    const release = holdChatBusy(decisionId);
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
      release();
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
    const release = holdChatBusy(decisionId);
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
      release();
    }
  }

  /** Whether a costly or irreversible decision keeps its recommendation one step away. */
  function recommendationOnRequest(dec: DecisionEnvelope): boolean {
    return dec.reversibility === 'costly' || dec.reversibility === 'irreversible';
  }

  function recommendationVisible(dec: DecisionEnvelope): boolean {
    return !recommendationOnRequest(dec) || Boolean(revealed[dec.id]);
  }

  /**
   * @param dec a decision
   * @param part one of its parts
   * @returns the sentence that says what the asking role recommends, with the option's own label
   */
  function recommendationSentence(dec: DecisionEnvelope, part: DecisionPart): string {
    return $t('decision.recommendation.by', {
      role: part.recommending_role || dec.asking_role || 'reviewer',
      option: optionLabelOf(dec, part.recommendation ?? '', part),
    });
  }

  /**
   * @param dec a decision
   * @param part one of its parts
   * @returns the cards the choice control draws, each with its support, consequences and dissent
   */
  function optionsOf(dec: DecisionEnvelope, part: DecisionPart): RadioOption[] {
    return choiceOptionsOf(dec, part) as RadioOption[];
  }

  /**
   * @param dec a decision
   * @param part one of its parts
   * @returns the cards a part that takes several answers draws
   */
  function checkOptionsOf(dec: DecisionEnvelope, part: DecisionPart): CheckOption[] {
    return choiceOptionsOf(dec, part);
  }

  /**
   * @param dec a decision
   * @param part one of its parts
   * @returns one card per option, with its consequences, support and dissent as its description
   */
  function choiceOptionsOf(
    dec: DecisionEnvelope,
    part: DecisionPart,
  ): Array<RadioOption & CheckOption> {
    const disabled = isChatting(dec);
    return part.options.map((option) => {
      const support = option.quorum_support;
      const pieces = [
        option.tradeoff_description,
        option.consequences,
        support
          ? $t('decision.support.votes', {
              votes: support.votes_count,
              total: support.total_votes,
              models: support.models_count,
            }) +
            ' · ' +
            $t(`decision.support.band.${support.band}`)
          : '',
        support?.dissent_summary
          ? $t('decision.dissent', {
              role: part.recommending_role || 'reviewer',
              reason: support.dissent_summary,
            })
          : '',
      ].filter((piece): piece is string => typeof piece === 'string' && piece.length > 0);
      return {
        value: option.key,
        label: option.label,
        description: pieces.length > 0 ? pieces.join(' · ') : undefined,
        marker:
          recommendationVisible(dec) && part.recommendation === option.key
            ? ('recommended' as const)
            : undefined,
        disabled,
      };
    });
  }

  /**
   * @param dec a decision
   * @param part one of its parts
   * @param index the part's index
   * @returns the answers chosen for a part that takes several
   */
  function answerValuesOf(dec: DecisionEnvelope, part: DecisionPart, index: number): string[] {
    return answerOf(dec, part, index)
      .split(',')
      .filter((value) => value.length > 0);
  }

  /**
   * Opens a decision in the object column and writes its address.
   *
   * @param id the decision to show
   */
  function openDecision(id: string): void {
    outcome = null;
    selectDecision(id, 'push');
  }

  /**
   * Shows a decision in the object column and writes its address with the given intent.
   *
   * @param id the decision to show, or `null` for the list alone
   * @param intent whether the change pushes or replaces the history entry
   */
  function selectDecision(id: string | null, intent: 'push' | 'replace'): void {
    if (inPlace) {
      // The column belongs to the caller's address: an answer or a close leaves it, it writes none.
      inPlace.onClose();
      return;
    }
    activeId = id;
    if (id === null) {
      syncedRouteDecision = null;
      runtimeStore.update((s) => ({
        ...s,
        currentRoute: 'decisions',
        selectedDecisionId: null,
      }));
      writeAddress({ route: 'decisions' }, intent);
      return;
    }
    syncedRouteDecision = id;
    runtimeStore.update((s) => ({ ...s, currentRoute: 'decision', selectedDecisionId: id }));
    writeAddress({ route: 'decision', decisionId: id }, intent);
    void tick().then(() => {
      const heading = document.querySelector<HTMLElement>('[data-testid="decision-detail"] h1');
      heading?.focus();
    });
  }

  /** Returns to the list, clearing the decision from the address. */
  function closeDetail(): void {
    selectDecision(null, 'replace');
  }

  /**
   * Handles a click on a row: the whole row opens the decision, except a control inside it.
   *
   * @param event the click
   * @param id the decision the row stands for
   */
  function onRowClick(event: MouseEvent, id: string): void {
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
      return;
    }
    const target = event.target as HTMLElement | null;
    // A chip inside the row copies, and a control of its own stays its own target.
    if (target?.closest('button') !== null) return;
    const anchor = target?.closest('a');
    if (anchor !== null && !anchor.classList.contains('row-link__title')) return;
    event.preventDefault();
    openDecision(id);
  }

  /**
   * `Enter` decides a decision with a chosen option (`navigation.adoc#keys`).
   *
   * The key keeps every meaning a control of its own gives it: a field, a button or a link under the
   * focus handles its own `Enter`, and only the decision's own surface decides.
   *
   * @param event the key press in the object column
   */
  function onDetailKeydown(event: KeyboardEvent): void {
    if (event.defaultPrevented || event.key !== 'Enter') return;
    if (event.metaKey || event.ctrlKey || event.altKey || event.shiftKey) return;
    const target = event.target as HTMLElement | null;
    if (
      target !== null &&
      (target.closest('button, a, input, textarea, select, summary') !== null ||
        target.isContentEditable)
    ) {
      return;
    }
    const dec = activeDecision;
    if (dec === null || dec.is_waiver || isChatting(dec) || isSubmittingDecision(dec)) return;
    if (!allPartsAnswered(dec)) return;
    event.preventDefault();
    void submitDecision(dec);
  }
</script>

{#snippet outcomeLine()}
  <div class="outcome-line" data-testid="decision-outcome">
    <FormStatus message={outcome?.text ?? ''} tone={outcome?.tone ?? 'neutral'} />
  </div>
{/snippet}

{#snippet listColumn(_level: Level)}
  <div class="decisions-list-pane">
    <header class="decisions-header">
      {#if activeDecision !== null}
        <h2 class="decisions-heading" tabindex="-1">{$t('decisions.list.title')}</h2>
      {:else}
        <h1 class="decisions-heading" tabindex="-1">{$t('decisions.list.title')}</h1>
      {/if}
      {#if decisions.length > 0}
        <CountBadge count={decisions.length} label={$t('decisions.list.open', { count: decisions.length })} />
      {/if}
    </header>

    {#if activeDecision === null}
      {@render outcomeLine()}
    {/if}

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
              aria-label={$t('common.action.close')}
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
        <Button variant="secondary" size="sm" onclick={() => void loadDecisions()}
          >{$t('common.action.retry')}</Button
        >
      </div>
    {/if}

    {#if loadError !== null && decisions.length > 0}
      <div class="error-banner" data-testid="decisions-error">
        <ErrorState
          code={loadError}
          actionLabel={$t('common.action.retry')}
          onAct={() => void loadDecisions()}
        />
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
          onAct={() => void loadDecisions()}
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
        label={$t('common.filter.label', { what: $t('decisions.list.title') })}
        placeholder={$t('decisions.filter.placeholder')}
        resultCount={matchingDecisions.length}
        onchange={(value) => (query = value)}
      />

      {#if matchingDecisions.length === 0}
        <div class="empty-state" data-testid="decisions-filter-empty">
          <p class="empty-title">{$t('decisions.filter.no_matches')}</p>
        </div>
      {:else}
        <div class="decisions-tree">
          {#each groups as group (group.key)}
            <section class="decisions-group" data-testid="decisions-group">
              <h3 class="decisions-group__head">
                <span class="decisions-group__title">{group.title}</span>
                <CountBadge
                  count={group.items.length}
                  label={$t('decisions.list.open', { count: group.items.length })}
                />
              </h3>
              <div class="decisions-group__rows">
                {#each group.items as dec (dec.id)}
                  <!-- svelte-ignore a11y_click_events_have_key_events -->
                  <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
                  <div
                    class="decisions-row"
                    data-decision-id={dec.id}
                    role="presentation"
                    onclick={(event) => onRowClick(event, dec.id)}
                  >
                    <RowLink
                      href={`/decisions/${encodeURIComponent(dec.id)}`}
                      title={displayTitle(dec)}
                      current={activeId === dec.id}
                    >
                      {#snippet tags()}
                        {#if dec.subject}
                          <Chip value={dec.subject} />
                        {/if}
                        {#if dec.asking_phase}
                          <PhaseLabel phase={dec.asking_phase} role={dec.asking_role} />
                        {/if}
                      {/snippet}
                      {#snippet detail()}
                        <span class="decisions-row__detail">{rowDetail(dec)}</span>
                      {/snippet}
                    </RowLink>
                  </div>
                {/each}
              </div>
            </section>
          {/each}
        </div>
      {/if}
    {/if}
  </div>
{/snippet}

{#snippet objectColumn(_level: Level)}
  {#if activeDecision !== null}
    <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
    <div
      class="decision-detail"
      data-testid="decision-detail"
      data-decision-id={activeDecision.id}
      role="group"
      aria-label={displayTitle(activeDecision)}
      onkeydown={onDetailKeydown}
    >
      {@render outcomeLine()}
      {#if activeDecision.is_waiver}
        {@render waiverDetail(activeDecision)}
      {:else}
        {@render standardDetail(activeDecision)}
      {/if}
    </div>
  {/if}
{/snippet}

{#snippet decisionHead(dec: DecisionEnvelope)}
  <header class="decision-head">
    <h1 class="decision-title" tabindex="-1">{displayTitle(dec)}</h1>
    <div class="decision-head__meta">
      {#if dec.subject}
        <Link href={subjectHref(dec)} label={dec.subject} look="chip" />
      {/if}
      {#if dec.asking_phase}
        <PhaseLabel phase={dec.asking_phase} role={dec.asking_role} />
      {/if}
      {#if dec.delivered_at}
        <span class="decision-head__waiting">
          {$t('decisions.list.since', {
            time: formatTime(dec.delivered_at, 'short', $currentLocale),
          })}
        </span>
      {/if}
    </div>
    <p class="decision-head__path" data-testid="decision-path">{pathOf(dec)}</p>
    <p class="decision-head__why">
      {$t(`decision.blocks.${dec.blocks}`)} · {$t(`decision.reversibility.${dec.reversibility}`)}
    </p>
  </header>
{/snippet}

{#snippet waiverDetail(dec: DecisionEnvelope)}
  {@render decisionHead(dec)}
  <div class="waiver-details">
    <div class="detail-row">
      <span class="detail-label">{$t('decision.waiver.gate')}:</span>
      <span class="detail-value mono"
        >{dec.waiver_gate || $t('decision.waiver.gate_fallback')}</span
      >
    </div>
    <div class="detail-row">
      <span class="detail-label">{$t('decision.waiver.result')}:</span>
      <span class="detail-value">{dec.waiver_result || $t('decision.waiver.result_failed')}</span>
    </div>
    <div class="detail-row">
      <span class="detail-label">{$t('decision.waiver.asked_by')}:</span>
      <span class="detail-value"
        >{[dec.asking_phase, dec.asking_role].filter(Boolean).join(' · ')}</span
      >
    </div>
    <div class="detail-row">
      <span class="detail-label">{$t('decision.waiver.its_reason')}:</span>
      <span class="detail-value" data-model-text
        >{dec.waiver_reason || dec.description || ''}</span
      >
    </div>
  </div>

  {#if dec.evidence && Object.keys(dec.evidence).length > 0}
    <EvidenceView evidence={dec.evidence} titleLevel={2} />
  {/if}

  <div class="waiver-form">
    <TextArea
      value={waiverReasons[dec.id] ?? ''}
      label={$t('decision.waiver.reason')}
      hint={$t('decision.waiver.reason_required')}
      rows={2}
      testid="waiver-reason"
      oninput={(event) => (waiverReasons[dec.id] = event.currentTarget.value)}
    />
  </div>

  <footer class="decision-actions">
    <Button
      variant="ghost"
      onclick={() => handleLater(dec.id)}
      data-advisory={$t('decision.waiver.later.title')}
    >
      {$t('common.action.later')}
    </Button>
    <Button
      variant="secondary"
      disabled={!waiverReasons[dec.id]?.trim() || isSubmitting[dec.id]}
      onclick={() => handleWaiver(dec.id, 'decline')}
      data-advisory={$t('decision.waiver.decline.title')}
    >
      {$t('decision.waiver.decline')}
    </Button>
    <Button
      variant="secondary"
      disabled={!waiverReasons[dec.id]?.trim() || isSubmitting[dec.id]}
      onclick={() => handleWaiver(dec.id, 'waive')}
      data-advisory={$t('decision.waiver.waive.title')}
    >
      {$t('decision.waiver.waive')}
    </Button>
  </footer>
{/snippet}

{#snippet standardDetail(dec: DecisionEnvelope)}
  {@render decisionHead(dec)}

  {#if dec.description && dec.description !== dec.title}
    <p class="decision-description" data-model-text>{dec.description}</p>
  {/if}

  {#if revisedNotices[dec.id]}
    <p class="decision-revised" data-testid="decision-revised-{dec.id}" role="status">
      {revisedNotices[dec.id]}
    </p>
  {/if}

  {#if dec.evidence && Object.keys(dec.evidence).length > 0}
    <EvidenceView evidence={dec.evidence} titleLevel={2} />
  {/if}

  {#each dec.parts as part, pIdx (part.part_id || pIdx)}
    <div class="decision-part">
      {#if dec.parts.length > 1}
        <div class="part-position">
          {$t('decision.part.position', { n: pIdx + 1, total: dec.parts.length })}
        </div>
      {/if}

      {#if !partAvailable(dec, part, pIdx)}
        <!-- The question of a part that is offered is the legend of its choice control; a part
             that waits has no control yet and keeps its question here. -->
        <p class="part-question">{part.question}</p>
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
          <p class="recommendation-line" data-testid="recommendation-{dec.id}">
            {recommendationSentence(dec, part)}
          </p>
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
            <Button size="sm" variant="secondary" onclick={() => (revealed[dec.id] = true)}>
              {$t('decision.recommendation.show')}
            </Button>
          </div>
        {/if}
      {/if}

      {#if partAvailable(dec, part, pIdx)}
        {#if !part.answer_type || part.answer_type === 'one_option'}
          <RadioGroup
            legend={part.question}
            name="opt-{dec.id}-{pIdx}"
            variant="card"
            value={ownWordsMode[keyOf(dec, part, pIdx)] ? '' : answerOf(dec, part, pIdx)}
            options={optionsOf(dec, part)}
            testid="options-{dec.id}-{pIdx}"
            onchange={(value) => selectOption(dec, part, pIdx, value)}
          />
        {:else}
          <CheckGroup
            legend={part.question}
            variant="card"
            values={answerValuesOf(dec, part, pIdx)}
            options={checkOptionsOf(dec, part)}
            testid="options-{dec.id}-{pIdx}"
            onchange={(values) => {
              setAnswer(dec, part, pIdx, values.join(','));
              reopenDependants(dec, pIdx + 1);
            }}
          />
        {/if}

        {#if part.allow_own_words !== false}
          <div class="own-words-section">
            <Button
              variant="ghost"
              size="sm"
              disabled={isChatting(dec)}
              aria-pressed={ownWordsMode[keyOf(dec, part, pIdx)] ? 'true' : 'false'}
              data-advisory={$t('decision.answer.own_words.title')}
              onclick={() => {
                const key = keyOf(dec, part, pIdx);
                ownWordsMode[key] = !ownWordsMode[key];
                if (ownWordsMode[key]) {
                  setAnswer(dec, part, pIdx, '');
                }
              }}
            >
              {$t('decision.answer.own_words')}
            </Button>

            {#if ownWordsMode[keyOf(dec, part, pIdx)]}
              <div class="own-words-box">
                {#if !interpretations[keyOf(dec, part, pIdx)]}
                  <TextArea
                    value={ownWordsTexts[keyOf(dec, part, pIdx)] ?? ''}
                    label={$t('decision.answer.own_words')}
                    placeholder={$t('decision.answer.own_words_placeholder')}
                    rows={3}
                    testid="own-words-{dec.id}-{pIdx}"
                    oninput={(event) =>
                      (ownWordsTexts[keyOf(dec, part, pIdx)] = event.currentTarget.value)}
                  />
                  <div class="own-words-actions">
                    <Button
                      size="sm"
                      variant="secondary"
                      disabled={!ownWordsTexts[keyOf(dec, part, pIdx)]?.trim() ||
                        isInterpreting[keyOf(dec, part, pIdx)]}
                      onclick={() => void interpretOwnWords(dec, part, pIdx)}
                    >
                      {$t('decision.readback.action')}
                    </Button>
                  </div>
                  {#if isInterpreting[keyOf(dec, part, pIdx)]}
                    <p class="own-words-working" role="status">
                      {$t('decision.interpret.working', {
                        role: part.recommending_role || 'interpret',
                      })}
                    </p>
                  {/if}
                {:else}
                  <div class="readback-box">
                    {#if interpretations[keyOf(dec, part, pIdx)]?.interpretation_type === 'UNCLEAR'}
                      <p class="readback-text">
                        {$t('decision.interpret.unclear_bound', {
                          reason:
                            interpretations[keyOf(dec, part, pIdx)]?.clarification_question ||
                            $t('decision.interpret.unclear_reason'),
                        })}
                      </p>
                    {:else}
                      <p class="readback-text">
                        {$t('decision.readback.understood', {
                          reading: interpretations[keyOf(dec, part, pIdx)]?.read_back_text || '',
                        })}
                      </p>
                    {/if}
                    <div class="readback-actions">
                      {#if interpretations[keyOf(dec, part, pIdx)]?.interpretation_type !== 'UNCLEAR'}
                        <Button
                          size="sm"
                          variant="primary"
                          data-advisory={$t('decision.readback.confirm.title')}
                          disabled={isChatting(dec)}
                          onclick={() => submitOwnWordsAnswer(dec, part, pIdx)}
                        >
                          {$t('decision.readback.confirm')}
                        </Button>
                      {/if}
                      <Button
                        size="sm"
                        variant="secondary"
                        onclick={() => {
                          interpretations[keyOf(dec, part, pIdx)] = null;
                        }}
                      >
                        {$t('decision.readback.edit')}
                      </Button>
                      <Button
                        size="sm"
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

        {#if part.allow_note}
          <div class="note-section">
            <TextArea
              value={partNotes[keyOf(dec, part, pIdx)] ?? ''}
              label={$t('decision.answer.note')}
              optional={true}
              rows={2}
              testid="note-{dec.id}-{pIdx}"
              oninput={(event) => (partNotes[keyOf(dec, part, pIdx)] = event.currentTarget.value)}
            />
          </div>
        {/if}
      {/if}
    </div>
  {/each}

  <footer class="decision-actions">
    <Button
      variant="primary"
      disabled={!allPartsAnswered(dec) || isSubmittingDecision(dec) || isChatting(dec)}
      onclick={() => submitDecision(dec)}
      data-advisory={$t('decision.confirm.action.title')}
    >
      {$t('decision.confirm.action')}
    </Button>
    <Button
      variant="secondary"
      disabled={isChatting(dec) || isSubmittingDecision(dec)}
      onclick={() => openChat(dec)}
      data-advisory={$t('decision.answer.chat.title')}
    >
      {$t('decision.answer.chat')}
    </Button>
    <Button
      variant="secondary"
      disabled={isSubmittingDecision(dec) || isChatting(dec)}
      onclick={() => submitWithdraw(dec)}
      data-advisory={$t('decision.answer.none_fits.title', { subject: dec.scope })}
    >
      {$t('decision.answer.none_fits', { subject: dec.scope })}
    </Button>
    <Button
      variant="ghost"
      onclick={() => handleLater(dec.id)}
      data-advisory={$t('decision.answer.later.title')}
    >
      {$t('decision.answer.later')}
    </Button>
  </footer>

  {#if chatNotice}
    <p class="decision-chat__notice" role="alert" data-testid="decision-chat-notice">
      {chatNotice}
    </p>
  {/if}
{/snippet}

{#snippet chatSide()}
  <div class="decision-chat" data-testid="decision-chat">
    <p class="decision-chat__open" role="status">
      {$t('decision.chat.stays_open', { id: chatDecisionId ?? '' })}
    </p>
    <ChatPanel
      open={true}
      role={chat?.role ?? ''}
      subject={chat?.subject_id ?? chatDecisionId ?? ''}
      turns={chat?.turns ?? []}
      proposal={chat?.proposal ?? null}
      available={chat !== null}
      busy={chatDecisionId !== null && chatBusy[chatDecisionId] !== undefined}
      onSend={sendChat}
      onEnd={endTheChat}
      onAccept={() => answerChatProposal(true)}
      onReject={() => answerChatProposal(false)}
      onClose={closeChatPanel}
      acceptLabel={$t('decision.chat.answer_proposed')}
    />
  </div>
{/snippet}

<div class="decisions-view">
  {#if inPlace && activeDecision === null}
    {#if loadError !== null}
      <div class="decisions-view__in-place" data-testid="decision-in-place">
        <ErrorState
          code={loadError}
          actionLabel={$t('common.action.retry')}
          onAct={() => void loadDecisions()}
          titleLevel={2}
        />
      </div>
    {:else if isLoading}
      <div class="decisions-view__in-place" data-testid="decision-in-place">
        <Skeleton shape="card" rows={3} label={$t('decisions.list.title')} />
      </div>
    {/if}
  {:else}
    <TriagePage
      {levels}
      label={$t('decisions.list.title')}
      column={listColumn}
      object={activeDecision === null ? undefined : objectColumn}
      {pinned}
      side={chatOpen ? chatSide : undefined}
      sideOpen={chatOpen}
      onHideSide={closeChatPanel}
    />
  {/if}
</div>

<style>
  .decisions-view {
    inline-size: 100%;
    min-inline-size: 0;
    padding: var(--space-6) var(--space-4);
  }

  .decisions-list-pane,
  .decision-detail {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    padding-inline: var(--space-2);
  }

  .decisions-header {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    border-bottom: 1px solid var(--color-border-subtle);
    padding-bottom: var(--space-3);
  }

  .decisions-heading {
    margin: 0;
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .decisions-tree {
    display: flex;
    flex-direction: column;
    gap: var(--space-5);
  }

  .decisions-group__head {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin: 0 0 var(--space-2);
    font-size: var(--text-sm);
    font-weight: var(--weight-semibold);
    color: var(--color-text-secondary);
  }

  .decisions-group__title {
    overflow-wrap: anywhere;
  }

  .decisions-group__rows {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  /* The row is one target; RowLink's own `aria-current` and ground mark the selected one. */
  .decisions-row {
    border-radius: var(--radius-sm);
  }

  .decisions-row__detail {
    overflow-wrap: anywhere;
  }

  .decision-head {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .decision-title {
    margin: 0;
    font-size: var(--text-xl);
    font-weight: var(--weight-semibold);
    color: var(--color-text-primary);
  }

  .decision-head__meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .decision-head__waiting {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .decision-head__path {
    margin: 0;
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    overflow-wrap: anywhere;
  }

  .decision-head__why {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .decision-description {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .recommendation-line {
    margin: 0 0 var(--space-1);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    font-weight: var(--weight-medium);
  }

  .recommendation-rationale,
  .recommendation-request-text {
    margin: 0 0 var(--space-2);
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .decision-part {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-4);
  }

  .part-question {
    margin: 0;
    font-weight: var(--weight-medium);
    color: var(--color-text-primary);
  }

  .part-position,
  .part-dependency {
    margin: 0;
    color: var(--color-text-muted);
    font-size: var(--text-xs);
  }

  .part-reopened,
  .decision-revised {
    margin: 0;
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-md);
    background-color: var(--color-warning-subtle);
    color: var(--color-warning-text);
    font-size: var(--text-sm);
  }

  .own-words-section,
  .note-section,
  .waiver-form {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .own-words-box,
  .readback-box {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-2) 0;
  }

  .readback-actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .readback-text {
    margin: 0;
    color: var(--color-text-primary);
    font-size: var(--text-sm);
  }

  .own-words-working {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .decision-actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-3);
  }

  .waiver-details {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .detail-row {
    display: grid;
    grid-template-columns: minmax(6rem, max-content) 1fr;
    gap: var(--space-3);
    align-items: baseline;
  }

  .detail-label {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .detail-value {
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    overflow-wrap: anywhere;
  }

  .mono {
    font-family: var(--font-mono);
    font-size: var(--text-xs);
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

  .notify-banner {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);
    padding: var(--space-3) var(--space-4);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
  }

  .notify-banner__text,
  .notify-message {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .notify-banner__actions {
    display: flex;
    gap: var(--space-2);
  }

  .notice-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .notice-line {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .notice-dismiss {
    border: none;
    background: none;
    color: var(--color-text-muted);
    cursor: pointer;
    font-size: var(--text-md);
    line-height: 1;
  }

  .decision-chat {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-2);
  }

  .decision-chat__open,
  .decision-chat__notice {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  @media (max-width: 767px) {
    .decisions-view {
      padding: var(--space-3);
    }

    .decisions-list-pane,
    .decision-detail {
      padding-inline: 0;
    }
  }
</style>
