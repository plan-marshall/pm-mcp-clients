import { writable, type Readable } from 'svelte/store';
import type { EventStreamState } from '../api/client';
import {
  acceptChatProposal,
  createClientTurnId,
  endChat,
  isMissingChatError,
  openChat,
  readChat,
  rejectChatProposal,
  sendChatTurn,
  stopChatTurn,
  subscribeTargetEvents,
} from './client';
import { storageKeyForDraft } from './routes';
import {
  isChatEndedState,
  type CataloguedChatEvent,
  type ChatEndedPayload,
  type ChatEventPayloadMap,
  type ChatProposalPayload,
  type ChatResource,
  type ChatStatus,
  type ChatTarget,
  type ChatTurn,
  type ChatTurnCompletedPayload,
  type ChatTurnDeltaPayload,
  type ChatTurnProgressPayload,
  type ChatTurnStartedPayload,
  type LocalChatTurn,
  type ProposalState,
} from './types';

export interface ChatStoreState {
  target: ChatTarget;
  archive: boolean;
  chat: ChatResource | null;
  turns: LocalChatTurn[];
  hasOlder: boolean;
  oldestTurnId: string | null;
  loading: boolean;
  loadingOlder: boolean;
  sending: boolean;
  proposalBusy: boolean;
  error: unknown;
  draft: string;
  endedBy: ChatEndedPayload | null;
  streamState: EventStreamState;
}

export interface ChatStore extends Readable<ChatStoreState> {
  connect: (options?: { turnId?: string | null }) => Promise<void>;
  disconnect: () => void;
  retryStream: () => void;
  openNew: () => Promise<ChatResource | null>;
  loadOlder: () => Promise<void>;
  loadToTurn: (turnId: string) => Promise<boolean>;
  setDraft: (text: string) => void;
  send: (overrideText?: string) => Promise<void>;
  retryTurn: (clientTurnId: string) => Promise<void>;
  editFailedTurn: (clientTurnId: string) => void;
  stopReply: (turnId?: string) => Promise<void>;
  acceptProposal: (turnId: string) => Promise<void>;
  rejectProposal: (turnId: string) => Promise<void>;
  end: () => Promise<void>;
}

interface StoredDraft {
  chatId: string | null;
  text: string;
}

const MAX_BUFFERED_STREAM_EVENTS = 500;

function readStoredDraft(key: string): StoredDraft {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return { chatId: null, text: '' };
    const parsed = JSON.parse(raw) as Partial<StoredDraft>;
    if (typeof parsed.text === 'string') {
      return {
        chatId: typeof parsed.chatId === 'string' ? parsed.chatId : null,
        text: parsed.text,
      };
    }
    return { chatId: null, text: raw };
  } catch {
    return { chatId: null, text: '' };
  }
}

function writeStoredDraft(key: string, chatId: string | null, text: string): void {
  try {
    if (!text) {
      localStorage.removeItem(key);
      return;
    }
    localStorage.setItem(key, JSON.stringify({ chatId, text }));
  } catch {
    // Ignore storage failures so sending stays unaffected.
  }
}

function clearStoredDraft(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    // Ignore storage failures.
  }
}

function collectIdsAfter(seqMap: ReadonlyMap<string, number>, baseline: number): Set<string> {
  const result = new Set<string>();
  for (const [id, seq] of seqMap) {
    if (seq > baseline) {
      result.add(id);
    }
  }
  return result;
}

function localDeliveryFields(prev: LocalChatTurn | undefined): Partial<LocalChatTurn> {
  if (!prev) return {};
  return {
    ...(prev.client_turn_id ? { client_turn_id: prev.client_turn_id } : {}),
    ...(prev.delivery ? { delivery: prev.delivery } : {}),
  };
}

function mergeServerTurn(
  turn: ChatTurn,
  prev: LocalChatTurn | undefined,
  contentTouchedTurns: ReadonlySet<string>,
  progressTouchedTurns: ReadonlySet<string>,
): LocalChatTurn {
  if (prev && contentTouchedTurns.has(turn.turn_id)) {
    if (prev.status === 'streaming' && turn.status !== 'streaming') {
      return {
        ...prev,
        status: turn.status,
        reason: turn.reason ?? prev.reason,
        refs: turn.refs.length > 0 ? turn.refs : prev.refs,
        lines: (turn.lines?.length ?? 0) > 0 ? turn.lines : prev.lines,
      };
    }
    return prev;
  }
  if (prev && progressTouchedTurns.has(turn.turn_id)) {
    const steps = prev.steps.length >= turn.steps.length ? prev.steps : turn.steps;
    return {
      ...turn,
      steps,
      ...localDeliveryFields(prev),
    };
  }
  return {
    ...turn,
    ...localDeliveryFields(prev),
  };
}

function shouldRetainPendingTurn(
  turn: LocalChatTurn,
  seenIds: ReadonlySet<string>,
  retainedAcceptedTurns: ReadonlySet<string>,
): boolean {
  if (seenIds.has(turn.turn_id)) return false;
  if (turn.delivery === 'not_sent' || turn.delivery === 'sending') return true;
  return turn.delivery === 'accepted' && retainedAcceptedTurns.has(turn.turn_id);
}

function mergeServerAndPending(
  serverTurns: ChatTurn[],
  previousTurns: LocalChatTurn[],
  contentTouchedTurns: ReadonlySet<string>,
  progressTouchedTurns: ReadonlySet<string>,
  retainedAcceptedTurns: ReadonlySet<string>,
): LocalChatTurn[] {
  const prevById = new Map(previousTurns.map((prev) => [prev.turn_id, prev]));
  const seenIds = new Set<string>();
  const merged = serverTurns.map((turn) => {
    seenIds.add(turn.turn_id);
    return mergeServerTurn(
      turn,
      prevById.get(turn.turn_id),
      contentTouchedTurns,
      progressTouchedTurns,
    );
  });

  // Preserve pending/failed/unconfirmed accepted local turns not yet reflected in server turns.
  for (const prev of previousTurns) {
    if (shouldRetainPendingTurn(prev, seenIds, retainedAcceptedTurns)) {
      merged.push(prev);
    }
  }
  return merged;
}

function oldestServerTurnId(chat: ChatResource): string | null {
  return chat.before ?? null;
}

function nextStateAfterTurnCompleted(chat: ChatResource): ChatStatus {
  if (isChatEndedState(chat.state)) {
    return chat.state;
  }
  if (chat.proposal?.state === 'open') {
    return 'chat.read-back';
  }
  return 'chat.open';
}

function nextStateAfterProposalUpdate(
  currentState: ChatStatus,
  proposalState: ProposalState,
): ChatStatus {
  if (proposalState === 'open' || currentState !== 'chat.read-back') {
    return currentState;
  }
  return proposalState === 'accepted' ? 'chat.apply' : 'chat.open';
}

/**
 * Creates a self-contained reactive chat store bound to one `ChatTarget`.
 *
 * Follows the ordering rule: subscribes to the subject's event stream before reading chat state,
 * buffers events until the initial snapshot settles, filters events by `chat_id`, preserves newer
 * stream updates against slower snapshots, and re-reads when the stream emits `reset`.
 *
 * @param target chat subject
 * @param options `{ archive }` when inspecting ended work read-only
 * @returns reactive store with actions
 */
export function createChatStore(
  target: ChatTarget,
  options: { archive?: boolean } = {},
): ChatStore {
  const archive = Boolean(options.archive);
  const draftKey = storageKeyForDraft(target);
  let pendingStoredDraft: StoredDraft | null = readStoredDraft(draftKey);
  // Pre-open drafts with no bound chatId remain immediately visible until connect() resolves chat.id.
  const preOpenText = pendingStoredDraft.chatId === null ? pendingStoredDraft.text : '';
  let draftTouched = false;
  let draftVersion = 0;

  let state: ChatStoreState = {
    target,
    archive,
    chat: null,
    turns: [],
    hasOlder: false,
    oldestTurnId: null,
    loading: false,
    loadingOlder: false,
    sending: false,
    proposalBusy: false,
    error: null,
    draft: preOpenText,
    endedBy: null,
    streamState: 'connecting',
  };

  const inner = writable<ChatStoreState>(state);
  const patch = (next: Partial<ChatStoreState>) => {
    state = { ...state, ...next };
    inner.set(state);
  };

  let unsubscribeStream: (() => void) | null = null;
  let retryStreamFn: (() => void) | null = null;
  let initialTurnTarget: string | null = null;
  let settled = false;
  let bufferedEvents: Array<{ event: string; data: unknown }> = [];
  let readSeq = 0;
  let olderReqSeq = 0;
  let streamSeq = 0;
  let chatMetaTouchedSeq = 0;
  const turnContentTouchedSeq = new Map<string, number>();
  const turnProgressTouchedSeq = new Map<string, number>();
  const acceptedAtSeq = new Map<string, number>();

  function resolveSnapshotDraft(
    resourceId: string,
    prevChatId: string | null,
  ): Partial<ChatStoreState> {
    if (prevChatId === resourceId) return {};
    if (prevChatId !== null) {
      pendingStoredDraft = null;
      draftTouched = false;
      clearStoredDraft(draftKey);
      draftVersion += 1;
      return { draft: '', endedBy: null };
    }
    const stored = pendingStoredDraft;
    pendingStoredDraft = null;
    if (!stored) return {};
    if (draftTouched) {
      writeStoredDraft(draftKey, resourceId, state.draft);
      return {};
    }
    if (stored.chatId !== null && stored.chatId !== resourceId) {
      // Stored draft belongs to an earlier replaced chat: discard without leaking across chats.
      clearStoredDraft(draftKey);
      if (!state.draft) return {};
      draftVersion += 1;
      return { draft: '' };
    }
    if (stored.text && stored.chatId === null) {
      writeStoredDraft(draftKey, resourceId, stored.text);
    }
    if (state.draft === stored.text) return {};
    draftVersion += 1;
    return { draft: stored.text };
  }

  function applySnapshot(resource: ChatResource, baselineStreamSeq = streamSeq): void {
    const prevChatId = state.chat?.id ?? null;
    const sameChat = prevChatId === resource.id;
    let contentTouchedTurns = new Set<string>();
    let progressTouchedTurns = new Set<string>();
    let retainedAcceptedTurns = new Set<string>();
    if (sameChat) {
      contentTouchedTurns = collectIdsAfter(turnContentTouchedSeq, baselineStreamSeq);
      progressTouchedTurns = collectIdsAfter(turnProgressTouchedSeq, baselineStreamSeq);
      for (const turn of resource.turns) {
        acceptedAtSeq.delete(turn.turn_id);
      }
      retainedAcceptedTurns = collectIdsAfter(acceptedAtSeq, baselineStreamSeq);
    } else {
      turnContentTouchedSeq.clear();
      turnProgressTouchedSeq.clear();
      acceptedAtSeq.clear();
      chatMetaTouchedSeq = 0;
    }

    const turns = mergeServerAndPending(
      resource.turns,
      sameChat ? state.turns : [],
      contentTouchedTurns,
      progressTouchedTurns,
      retainedAcceptedTurns,
    );
    const oldestTurnId = oldestServerTurnId(resource);
    const keepStreamMeta =
      sameChat && chatMetaTouchedSeq > baselineStreamSeq && state.chat !== null;
    const nextChat: ChatResource = keepStreamMeta
      ? {
          ...resource,
          state: state.chat!.state,
          proposal: state.chat!.proposal,
        }
      : resource;
    const draftPatch = resolveSnapshotDraft(resource.id, prevChatId);

    patch({
      chat: nextChat,
      turns,
      hasOlder: oldestTurnId !== null,
      oldestTurnId,
      loading: false,
      error: null,
      ...draftPatch,
    });
  }

  async function refreshFromReset(turnId?: string | null): Promise<void> {
    const seq = ++readSeq;
    const baseline = streamSeq;
    try {
      const resource = await readChat(target);
      if (seq !== readSeq) return;
      applySnapshot(resource, baseline);
      if (turnId) {
        await loadPagesUntilTurn(turnId, seq);
      }
    } catch (error) {
      if (seq !== readSeq) return;
      patch({ error });
    }
  }

  function matchingActiveChat(chatId: string): ChatResource | null {
    return state.chat?.id === chatId ? state.chat : null;
  }

  function markTurnContentTouched(turnId: string): void {
    streamSeq += 1;
    turnContentTouchedSeq.set(turnId, streamSeq);
  }

  function markTurnProgressTouched(turnId: string): void {
    streamSeq += 1;
    turnProgressTouchedSeq.set(turnId, streamSeq);
  }

  function applyTurnStarted(payload: ChatTurnStartedPayload): void {
    const activeChat = matchingActiveChat(payload.chat_id);
    if (!activeChat) return;
    acceptedAtSeq.delete(payload.turn_id);
    markTurnContentTouched(payload.turn_id);
    const timestamp = payload.at ?? new Date().toISOString();
    const nextTurn: LocalChatTurn = {
      turn_id: payload.turn_id,
      kind: payload.kind,
      author: payload.author,
      status: payload.author === 'operator' ? 'complete' : 'streaming',
      format: payload.format,
      untrusted: payload.untrusted,
      model: payload.model ?? null,
      text: payload.text ?? '',
      steps: [],
      refs: [],
      at: timestamp,
      reason: null,
      lines: [],
    };
    const nextTurns = [...state.turns];
    const existingIndex = nextTurns.findIndex((turn) => turn.turn_id === payload.turn_id);
    if (existingIndex >= 0) {
      // A re-announced turn replaces partial text and steps cleanly while preserving local ack state.
      nextTurns[existingIndex] = {
        ...nextTurn,
        delivery: nextTurns[existingIndex].delivery,
        client_turn_id: nextTurns[existingIndex].client_turn_id,
      };
    } else {
      nextTurns.push(nextTurn);
    }
    patch({
      turns: nextTurns,
      chat: {
        ...activeChat,
        state: payload.author === 'operator' ? 'chat.turn' : 'chat.reply',
        context: payload.context ?? activeChat.context,
      },
    });
  }

  function applyTurnDelta(payload: ChatTurnDeltaPayload): void {
    if (!matchingActiveChat(payload.chat_id)) return;
    const index = state.turns.findIndex((turn) => turn.turn_id === payload.turn_id);
    if (index < 0) return;
    markTurnContentTouched(payload.turn_id);
    const nextTurns = [...state.turns];
    nextTurns[index] = {
      ...nextTurns[index],
      text: nextTurns[index].text + payload.text,
    };
    patch({ turns: nextTurns });
  }

  function applyTurnProgress(payload: ChatTurnProgressPayload): void {
    if (!matchingActiveChat(payload.chat_id)) return;
    const index = state.turns.findIndex((turn) => turn.turn_id === payload.turn_id);
    if (index < 0) return;
    markTurnProgressTouched(payload.turn_id);
    const nextTurns = [...state.turns];
    nextTurns[index] = {
      ...nextTurns[index],
      steps: [...nextTurns[index].steps, payload.step],
    };
    patch({ turns: nextTurns });
  }

  function applyTurnCompleted(payload: ChatTurnCompletedPayload): void {
    const activeChat = matchingActiveChat(payload.chat_id);
    if (!activeChat) return;
    acceptedAtSeq.delete(payload.turn_id);
    markTurnContentTouched(payload.turn_id);
    const nextTurns = [...state.turns];
    const index = nextTurns.findIndex((turn) => turn.turn_id === payload.turn_id);
    if (index >= 0) {
      nextTurns[index] = {
        ...nextTurns[index],
        kind: payload.kind,
        status: payload.status,
        untrusted: payload.untrusted,
        text: payload.text,
        refs: payload.refs ?? nextTurns[index].refs,
        reason: payload.reason ?? null,
        lines: payload.lines ?? nextTurns[index].lines,
      };
    } else {
      nextTurns.push({
        turn_id: payload.turn_id,
        kind: payload.kind,
        author: activeChat.role.name,
        status: payload.status,
        format: 'plain',
        untrusted: payload.untrusted,
        model: null,
        text: payload.text,
        steps: [],
        refs: payload.refs ?? [],
        at: payload.at ?? new Date().toISOString(),
        reason: payload.reason ?? null,
        lines: payload.lines ?? [],
      });
    }
    patch({
      turns: nextTurns,
      chat: {
        ...activeChat,
        state: nextStateAfterTurnCompleted(activeChat),
      },
    });
  }

  function applyProposalEvent(payload: ChatProposalPayload): void {
    const activeChat = matchingActiveChat(payload.chat_id);
    if (!activeChat) return;
    streamSeq += 1;
    chatMetaTouchedSeq = streamSeq;
    const current = activeChat.proposal;
    if (current?.turn_id !== payload.turn_id) {
      // A newly opened proposal's diff/summary lives on the resource, so re-read snapshot.
      void refreshFromReset();
      return;
    }
    const nextProposalState = payload.state;
    patch({
      chat: {
        ...activeChat,
        state: nextStateAfterProposalUpdate(activeChat.state, nextProposalState),
        proposal: {
          ...current,
          state: nextProposalState,
          finding: payload.finding ?? null,
        },
      },
    });
  }

  function applyEndedEvent(payload: ChatEndedPayload): void {
    const activeChat = matchingActiveChat(payload.chat_id);
    if (!activeChat) return;
    streamSeq += 1;
    chatMetaTouchedSeq = streamSeq;
    const nextState = payload.ended_by === 'proposal_carried' ? 'chat.carried' : 'chat.end';
    const nextTurns = state.turns.map((turn) => {
      if (turn.status === 'streaming' && !turn.delivery) {
        turnContentTouchedSeq.set(turn.turn_id, streamSeq);
        return { ...turn, status: 'stopped' as const };
      }
      return turn;
    });
    patch({
      endedBy: payload,
      turns: nextTurns,
      chat: { ...activeChat, state: nextState },
    });
  }

  type ChatEventHandlers = {
    [K in CataloguedChatEvent]: (payload: ChatEventPayloadMap[K]) => void;
  };

  const eventHandlers: ChatEventHandlers = {
    'chat.turn.started': applyTurnStarted,
    'chat.turn.delta': applyTurnDelta,
    'chat.turn.progress': applyTurnProgress,
    'chat.turn.completed': applyTurnCompleted,
    'chat.proposal': applyProposalEvent,
    'chat.ended': applyEndedEvent,
  };

  function dispatchStreamEvent(event: string, data: unknown): void {
    if (event === 'reset') {
      void refreshFromReset();
      return;
    }
    if (!Object.hasOwn(eventHandlers, event) || !data || typeof data !== 'object') {
      return;
    }
    const handler = eventHandlers[event as CataloguedChatEvent] as (payload: unknown) => void;
    handler(data);
  }

  function handleStreamEvent(event: string, data: unknown): void {
    if (!settled) {
      if (bufferedEvents.length < MAX_BUFFERED_STREAM_EVENTS) {
        bufferedEvents.push({ event, data });
      }
      return;
    }
    dispatchStreamEvent(event, data);
  }

  function flushBufferedEvents(turnId?: string | null): boolean {
    const queued = bufferedEvents;
    bufferedEvents = [];
    if (queued.length === 0) return false;
    if (queued.some((item) => item.event === 'reset')) {
      void refreshFromReset(turnId);
      return true;
    }
    for (const item of queued) {
      if (
        (item.event === 'chat.turn.started' ||
          item.event === 'chat.turn.delta' ||
          item.event === 'chat.turn.progress') &&
        item.data &&
        typeof item.data === 'object' &&
        'turn_id' in item.data
      ) {
        const queuedTurnId = (item.data as { turn_id?: unknown }).turn_id;
        const existing = state.turns.find((t) => t.turn_id === queuedTurnId);
        // Do not revert a turn that the initial snapshot already returned in a terminal state.
        if (existing && existing.status !== 'streaming') {
          continue;
        }
      }
      dispatchStreamEvent(item.event, item.data);
    }
    return false;
  }

  async function ensureChatOpen(pendingTurn?: LocalChatTurn): Promise<ChatResource> {
    if (state.chat && !isChatEndedState(state.chat.state)) {
      return state.chat;
    }
    const baseline = streamSeq;
    const opened = await openChat(target);
    applySnapshot(opened, baseline);
    if (
      pendingTurn &&
      !state.turns.some((turn) => turn.client_turn_id === pendingTurn.client_turn_id)
    ) {
      patch({ turns: [...state.turns, pendingTurn] });
    }
    return opened;
  }

  async function fetchOlderPage(
    cursor: string,
    seq: number,
    expectedChatId: string | null,
  ): Promise<boolean> {
    const pageSeq = ++olderReqSeq;
    patch({ loadingOlder: true });
    try {
      const page = await readChat(target, cursor);
      if (
        seq !== readSeq ||
        pageSeq !== olderReqSeq ||
        (expectedChatId !== null && state.chat?.id !== expectedChatId)
      ) {
        return false;
      }
      const existingIds = new Set(state.turns.map((turn) => turn.turn_id));
      const olderTurns = page.turns.filter((turn) => !existingIds.has(turn.turn_id));
      const nextCursor = oldestServerTurnId(page);
      const hasMore = Boolean(nextCursor && nextCursor !== cursor && page.turns.length > 0);
      patch({
        turns: [...olderTurns, ...state.turns],
        hasOlder: hasMore,
        oldestTurnId: hasMore ? nextCursor : null,
        loadingOlder: false,
      });
      return hasMore;
    } catch (error) {
      if (
        seq === readSeq &&
        pageSeq === olderReqSeq &&
        (expectedChatId === null || state.chat?.id === expectedChatId)
      ) {
        patch({ loadingOlder: false, error });
      }
      return false;
    }
  }

  async function loadPagesUntilTurn(turnId: string, seq: number): Promise<boolean> {
    const expectedChatId = state.chat?.id ?? null;
    while (
      seq === readSeq &&
      (expectedChatId === null || state.chat?.id === expectedChatId) &&
      !state.turns.some((turn) => turn.turn_id === turnId)
    ) {
      const cursor = state.oldestTurnId;
      if (!cursor) {
        return false;
      }
      const advanced = await fetchOlderPage(cursor, seq, expectedChatId);
      if (!advanced) {
        break;
      }
    }
    return state.turns.some((turn) => turn.turn_id === turnId);
  }

  function reconcileAcceptedTurn(clientTurnId: string, serverTurnId: string): LocalChatTurn[] {
    const hasStreamedServerTurn = state.turns.some(
      (turn) => turn.turn_id === serverTurnId && turn.client_turn_id !== clientTurnId,
    );
    if (hasStreamedServerTurn) {
      // Remove optimistic placeholder and attach client_turn_id / delivery to the canonical SSE turn.
      return state.turns
        .filter((turn) => !(turn.client_turn_id === clientTurnId && turn.turn_id === clientTurnId))
        .map((turn) =>
          turn.turn_id === serverTurnId
            ? { ...turn, client_turn_id: clientTurnId, delivery: 'accepted' as const }
            : turn,
        );
    }
    streamSeq += 1;
    acceptedAtSeq.set(serverTurnId, streamSeq);
    return state.turns.map((turn) =>
      turn.client_turn_id === clientTurnId
        ? {
            ...turn,
            turn_id: serverTurnId,
            status: 'complete' as const,
            delivery: 'accepted' as const,
          }
        : turn,
    );
  }

  async function loadInitialSnapshot(
    seq: number,
    baseline: number,
    turnId?: string | null,
  ): Promise<void> {
    try {
      let resource: ChatResource;
      try {
        resource = await readChat(target);
      } catch (error) {
        if (seq !== readSeq) return;
        if (!archive && isMissingChatError(error)) {
          resource = await openChat(target);
        } else {
          throw error;
        }
      }
      if (seq !== readSeq) return;
      applySnapshot(resource, baseline);
      settled = true;
      const recoveringFromReset = flushBufferedEvents(turnId);
      if (turnId && !recoveringFromReset) {
        await loadPagesUntilTurn(turnId, seq);
      }
    } catch (error) {
      if (seq !== readSeq) return;
      bufferedEvents = [];
      patch({ loading: false, error });
    }
  }

  async function postWithTurnId(
    text: string,
    clientTurnId: string,
    ownedDraftVersion: number | null,
    pendingTurn?: LocalChatTurn,
  ): Promise<void> {
    patch({ sending: true, error: null });
    try {
      await ensureChatOpen(pendingTurn);
      const accepted = await sendChatTurn(target, text, clientTurnId);
      const nextTurns = reconcileAcceptedTurn(clientTurnId, accepted.turn_id);
      const shouldClearDraft = ownedDraftVersion !== null && ownedDraftVersion === draftVersion;
      if (shouldClearDraft) {
        clearStoredDraft(draftKey);
        draftVersion += 1;
        draftTouched = false;
      }
      patch({
        sending: false,
        ...(shouldClearDraft ? { draft: '' } : {}),
        turns: nextTurns,
      });
    } catch (error) {
      const shouldRestoreDraft = ownedDraftVersion !== null && ownedDraftVersion === draftVersion;
      if (shouldRestoreDraft) {
        writeStoredDraft(draftKey, state.chat?.id ?? null, text);
      }
      const nextTurns = state.turns.map((turn) =>
        turn.client_turn_id === clientTurnId
          ? {
              ...turn,
              status: 'failed' as const,
              delivery: 'not_sent' as const,
            }
          : turn,
      );
      patch({
        sending: false,
        error,
        ...(shouldRestoreDraft ? { draft: text } : {}),
        turns: nextTurns,
      });
    }
  }

  return {
    subscribe: inner.subscribe,

    async connect(options?: { turnId?: string | null }): Promise<void> {
      unsubscribeStream?.();
      retryStreamFn = null;
      initialTurnTarget = options?.turnId ?? null;
      settled = false;
      bufferedEvents = [];
      olderReqSeq += 1;
      const seq = ++readSeq;
      const baseline = streamSeq;
      patch({ loading: true, loadingOlder: false, error: null });

      // Follow rule: open the subject stream strictly before reading the chat.
      unsubscribeStream = subscribeTargetEvents(
        target,
        handleStreamEvent,
        undefined,
        (streamState) => {
          patch({ streamState });
        },
        (retry) => {
          retryStreamFn = retry;
        },
      );

      await loadInitialSnapshot(seq, baseline, initialTurnTarget);
    },

    disconnect(): void {
      unsubscribeStream?.();
      unsubscribeStream = null;
      retryStreamFn = null;
      settled = false;
      bufferedEvents = [];
      olderReqSeq += 1;
      readSeq += 1;
      patch({ loadingOlder: false });
    },

    retryStream(): void {
      retryStreamFn?.();
      if (!settled && unsubscribeStream) {
        const seq = ++readSeq;
        const baseline = streamSeq;
        patch({ loading: true, error: null });
        void loadInitialSnapshot(seq, baseline, initialTurnTarget);
        return;
      }
      if (!retryStreamFn && !unsubscribeStream) {
        void this.connect({ turnId: initialTurnTarget });
      }
    },

    async openNew(): Promise<ChatResource | null> {
      if (archive) return null;
      olderReqSeq += 1;
      patch({ loading: true, loadingOlder: false, error: null });
      try {
        if (state.chat && !isChatEndedState(state.chat.state)) {
          const endBaseline = streamSeq;
          const ended = await endChat(target);
          applySnapshot(ended, endBaseline);
        }
        const openBaseline = streamSeq;
        const opened = await openChat(target);
        readSeq += 1;
        clearStoredDraft(draftKey);
        draftTouched = false;
        draftVersion += 1;
        patch({ turns: [], endedBy: null, draft: '' });
        applySnapshot(opened, openBaseline);
        settled = true;
        return opened;
      } catch (error) {
        patch({ loading: false, error });
        return null;
      }
    },

    async loadOlder(): Promise<void> {
      if (!state.oldestTurnId || state.loadingOlder) return;
      const expectedChatId = state.chat?.id ?? null;
      const seq = readSeq;
      const pageSeq = ++olderReqSeq;
      patch({ loadingOlder: true });
      try {
        const page = await readChat(target, state.oldestTurnId);
        if (
          seq !== readSeq ||
          pageSeq !== olderReqSeq ||
          (expectedChatId !== null && state.chat?.id !== expectedChatId)
        ) {
          return;
        }
        const existingIds = new Set(state.turns.map((turn) => turn.turn_id));
        const olderTurns = page.turns.filter((turn) => !existingIds.has(turn.turn_id));
        const oldestTurnId = oldestServerTurnId(page);
        patch({
          turns: [...olderTurns, ...state.turns],
          hasOlder: oldestTurnId !== null && page.turns.length > 0,
          oldestTurnId: page.turns.length > 0 ? oldestTurnId : null,
          loadingOlder: false,
        });
      } catch (error) {
        if (
          seq === readSeq &&
          pageSeq === olderReqSeq &&
          (expectedChatId === null || state.chat?.id === expectedChatId)
        ) {
          patch({ loadingOlder: false, error });
        }
      }
    },

    async loadToTurn(turnId: string): Promise<boolean> {
      return loadPagesUntilTurn(turnId, readSeq);
    },

    setDraft(text: string): void {
      draftTouched = true;
      draftVersion += 1;
      pendingStoredDraft = null;
      writeStoredDraft(draftKey, state.chat?.id ?? null, text);
      patch({ draft: text });
    },

    async send(overrideText?: string): Promise<void> {
      if (archive) return;
      const text = overrideText ?? state.draft;
      if (!text.trim()) return;
      let clientTurnId: string;
      try {
        clientTurnId = createClientTurnId();
      } catch (error) {
        patch({ error });
        return;
      }
      const ownedDraftVersion =
        overrideText === undefined || overrideText === state.draft ? draftVersion : null;
      const optimistic: LocalChatTurn = {
        turn_id: clientTurnId,
        client_turn_id: clientTurnId,
        kind: 'message',
        author: 'operator',
        status: 'streaming',
        delivery: 'sending',
        format: 'plain',
        untrusted: false,
        model: null,
        text,
        steps: [],
        refs: [],
        at: new Date().toISOString(),
        reason: null,
        lines: [],
      };
      patch({ turns: [...state.turns, optimistic] });
      await postWithTurnId(text, clientTurnId, ownedDraftVersion, optimistic);
    },

    async retryTurn(clientTurnId: string): Promise<void> {
      if (archive) return;
      const targetTurn = state.turns.find((turn) => turn.client_turn_id === clientTurnId);
      if (!targetTurn) return;
      const nextTurns = state.turns.map((turn) =>
        turn.client_turn_id === clientTurnId
          ? { ...turn, status: 'streaming' as const, delivery: 'sending' as const }
          : turn,
      );
      patch({ turns: nextTurns });
      // Only clear/restore composer draft if it still holds the failed turn's text; preserve any newer draft.
      const ownedDraftVersion = state.draft === targetTurn.text ? draftVersion : null;
      await postWithTurnId(targetTurn.text, clientTurnId, ownedDraftVersion);
    },

    editFailedTurn(clientTurnId: string): void {
      if (archive) return;
      const targetTurn = state.turns.find((turn) => turn.client_turn_id === clientTurnId);
      if (!targetTurn) return;
      draftTouched = true;
      draftVersion += 1;
      pendingStoredDraft = null;
      writeStoredDraft(draftKey, state.chat?.id ?? null, targetTurn.text);
      patch({
        draft: targetTurn.text,
        turns: state.turns.filter((turn) => turn.client_turn_id !== clientTurnId),
      });
    },

    async stopReply(turnId?: string): Promise<void> {
      if (archive) return;
      const active =
        turnId ??
        [...state.turns]
          .reverse()
          .find((turn) => turn.author !== 'operator' && turn.status === 'streaming')?.turn_id;
      if (!active) return;
      const baseline = streamSeq;
      try {
        const updated = await stopChatTurn(target, active);
        applySnapshot(updated, baseline);
      } catch (error) {
        patch({ error });
      }
    },

    async acceptProposal(turnId: string): Promise<void> {
      if (archive) return;
      const baseline = streamSeq;
      patch({ proposalBusy: true, error: null });
      try {
        const updated = await acceptChatProposal(target, turnId);
        applySnapshot(updated, baseline);
      } catch (error) {
        patch({ error });
      } finally {
        patch({ proposalBusy: false });
      }
    },

    async rejectProposal(turnId: string): Promise<void> {
      if (archive) return;
      const baseline = streamSeq;
      patch({ proposalBusy: true, error: null });
      try {
        const updated = await rejectChatProposal(target, turnId);
        applySnapshot(updated, baseline);
      } catch (error) {
        patch({ error });
      } finally {
        patch({ proposalBusy: false });
      }
    },

    async end(): Promise<void> {
      if (archive) return;
      const baseline = streamSeq;
      try {
        const closed = await endChat(target);
        applySnapshot(closed, baseline);
      } catch (error) {
        patch({ error });
      }
    },
  };
}
