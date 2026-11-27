import { writable, type Readable } from 'svelte/store';
import type { EventStreamState } from '../api/client';
import {
  acceptChatProposal,
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
  CATALOGUED_CHAT_EVENTS,
  isChatEndedState,
  type ChatEndedPayload,
  type ChatProposalPayload,
  type ChatResource,
  type ChatTarget,
  type ChatTurn,
  type ChatTurnCompletedPayload,
  type ChatTurnDeltaPayload,
  type ChatTurnProgressPayload,
  type ChatTurnStartedPayload,
  type LocalChatTurn,
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
  connect: () => Promise<void>;
  disconnect: () => void;
  openNew: () => Promise<ChatResource | null>;
  loadOlder: () => Promise<void>;
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

function mergeServerAndPending(
  serverTurns: ChatTurn[],
  previousTurns: LocalChatTurn[],
): LocalChatTurn[] {
  const byId = new Map<string, LocalChatTurn>();
  for (const turn of serverTurns) {
    byId.set(turn.turn_id, { ...turn });
  }
  const merged = Array.from(byId.values());
  // Preserve failed ('not_sent') or still-pending local turns not yet reflected in server turns.
  for (const prev of previousTurns) {
    if (!prev.delivery) continue;
    if (prev.delivery === 'not_sent') {
      if (!byId.has(prev.turn_id)) {
        merged.push(prev);
      }
      continue;
    }
    if (prev.delivery === 'sending' && !byId.has(prev.turn_id)) {
      merged.push(prev);
    }
  }
  return merged;
}

function oldestServerTurnId(chat: ChatResource): string | null {
  if (chat.before === null) {
    return null;
  }
  if (chat.before && chat.turns.length > 0) {
    // Server populates `before` when an older page exists (either cursor or first turn id).
    return chat.turns[0].turn_id;
  }
  return null;
}

/**
 * Creates a self-contained reactive chat store bound to one `ChatTarget`.
 *
 * Follows the ordering rule: subscribes to the subject's event stream before reading chat state,
 * ignores events until the initial read settles, filters events by `chat_id`, and re-reads when the
 * stream emits `reset`.
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
  const initialDraft = readStoredDraft(draftKey);

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
    draft: initialDraft.text,
    endedBy: null,
    streamState: 'connecting',
  };

  const inner = writable<ChatStoreState>(state);
  const patch = (next: Partial<ChatStoreState>) => {
    state = { ...state, ...next };
    inner.set(state);
  };

  let unsubscribeStream: (() => void) | null = null;
  let settled = false;
  let readSeq = 0;

  function applySnapshot(resource: ChatResource): void {
    const turns = mergeServerAndPending(resource.turns, state.turns);
    const oldestTurnId = oldestServerTurnId(resource);
    patch({
      chat: resource,
      turns,
      hasOlder: oldestTurnId !== null,
      oldestTurnId,
      loading: false,
      error: null,
    });
  }

  async function refreshFromReset(): Promise<void> {
    const seq = ++readSeq;
    try {
      const resource = await readChat(target);
      if (seq !== readSeq) return;
      applySnapshot(resource);
    } catch (error) {
      if (seq !== readSeq) return;
      patch({ error });
    }
  }

  function applyTurnStarted(payload: ChatTurnStartedPayload): void {
    if (!state.chat || payload.chat_id !== state.chat.id) return;
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
    const existingIndex = nextTurns.findIndex(
      (turn) =>
        turn.turn_id === payload.turn_id ||
        (payload.author === 'operator' &&
          turn.delivery === 'accepted' &&
          turn.turn_id === payload.turn_id),
    );
    if (existingIndex >= 0) {
      // A re-announced turn replaces partial text and steps cleanly.
      nextTurns[existingIndex] = {
        ...nextTurn,
        delivery: nextTurns[existingIndex].delivery,
        client_turn_id: nextTurns[existingIndex].client_turn_id,
      };
    } else {
      const matchingSending =
        payload.author === 'operator'
          ? nextTurns.findIndex(
              (turn) =>
                turn.author === 'operator' &&
                turn.delivery === 'sending' &&
                turn.text === (payload.text ?? ''),
            )
          : -1;
      if (matchingSending >= 0) {
        nextTurns[matchingSending] = nextTurn;
      } else {
        nextTurns.push(nextTurn);
      }
    }
    patch({
      turns: nextTurns,
      chat: {
        ...state.chat,
        state: payload.author === 'operator' ? 'chat.turn' : 'chat.reply',
        context: payload.context ?? state.chat.context,
      },
    });
  }

  function applyTurnDelta(payload: ChatTurnDeltaPayload): void {
    if (!state.chat || payload.chat_id !== state.chat.id) return;
    const index = state.turns.findIndex((turn) => turn.turn_id === payload.turn_id);
    if (index < 0) return;
    const nextTurns = [...state.turns];
    nextTurns[index] = {
      ...nextTurns[index],
      text: nextTurns[index].text + payload.text,
    };
    patch({ turns: nextTurns });
  }

  function applyTurnProgress(payload: ChatTurnProgressPayload): void {
    if (!state.chat || payload.chat_id !== state.chat.id) return;
    const index = state.turns.findIndex((turn) => turn.turn_id === payload.turn_id);
    if (index < 0) return;
    const nextTurns = [...state.turns];
    nextTurns[index] = {
      ...nextTurns[index],
      steps: [...nextTurns[index].steps, payload.step],
    };
    patch({ turns: nextTurns });
  }

  function applyTurnCompleted(payload: ChatTurnCompletedPayload): void {
    if (!state.chat || payload.chat_id !== state.chat.id) return;
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
        author: payload.kind === 'notice' ? state.chat.role.name : state.chat.role.name,
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
    const stillEnded = isChatEndedState(state.chat.state);
    patch({
      turns: nextTurns,
      chat: {
        ...state.chat,
        state: stillEnded
          ? state.chat.state
          : state.chat.proposal?.state === 'open'
            ? 'chat.read-back'
            : 'chat.open',
      },
    });
  }

  function applyProposalEvent(payload: ChatProposalPayload): void {
    if (!state.chat || payload.chat_id !== state.chat.id) return;
    const current = state.chat.proposal;
    if (!current || current.turn_id !== payload.turn_id) {
      // A newly opened proposal's diff/summary lives on the resource, so re-read snapshot.
      void refreshFromReset();
      return;
    }
    patch({
      chat: {
        ...state.chat,
        proposal: {
          ...current,
          state: payload.state,
          finding: payload.finding ?? null,
        },
      },
    });
  }

  function applyEndedEvent(payload: ChatEndedPayload): void {
    if (!state.chat || payload.chat_id !== state.chat.id) return;
    const nextState = payload.ended_by === 'proposal_carried' ? 'chat.carried' : 'chat.end';
    const nextTurns = state.turns.map((turn) =>
      turn.status === 'streaming' && !turn.delivery
        ? { ...turn, status: 'stopped' as const }
        : turn,
    );
    patch({
      endedBy: payload,
      turns: nextTurns,
      chat: { ...state.chat, state: nextState },
    });
  }

  function handleStreamEvent(event: string, data: unknown): void {
    if (event === 'reset') {
      if (settled) {
        void refreshFromReset();
      }
      return;
    }
    if (!settled || !CATALOGUED_CHAT_EVENTS.has(event) || !data || typeof data !== 'object') {
      return;
    }
    switch (event) {
      case 'chat.turn.started':
        applyTurnStarted(data as ChatTurnStartedPayload);
        break;
      case 'chat.turn.delta':
        applyTurnDelta(data as ChatTurnDeltaPayload);
        break;
      case 'chat.turn.progress':
        applyTurnProgress(data as ChatTurnProgressPayload);
        break;
      case 'chat.turn.completed':
        applyTurnCompleted(data as ChatTurnCompletedPayload);
        break;
      case 'chat.proposal':
        applyProposalEvent(data as ChatProposalPayload);
        break;
      case 'chat.ended':
        applyEndedEvent(data as ChatEndedPayload);
        break;
    }
  }

  async function ensureChatOpen(): Promise<ChatResource> {
    if (state.chat && !isChatEndedState(state.chat.state)) {
      return state.chat;
    }
    const opened = await openChat(target);
    applySnapshot(opened);
    return opened;
  }

  async function postWithTurnId(text: string, clientTurnId: string): Promise<void> {
    patch({ sending: true, error: null });
    try {
      await ensureChatOpen();
      const accepted = await sendChatTurn(target, text, clientTurnId);
      clearStoredDraft(draftKey);
      const nextTurns = state.turns.map((turn) =>
        turn.client_turn_id === clientTurnId
          ? {
              ...turn,
              turn_id: accepted.turn_id,
              status: 'complete' as const,
              delivery: 'accepted' as const,
            }
          : turn,
      );
      patch({
        sending: false,
        draft: '',
        turns: nextTurns,
      });
    } catch (error) {
      writeStoredDraft(draftKey, state.chat?.id ?? null, text);
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
        draft: text,
        turns: nextTurns,
      });
    }
  }

  return {
    subscribe: inner.subscribe,

    async connect(): Promise<void> {
      unsubscribeStream?.();
      settled = false;
      const seq = ++readSeq;
      patch({ loading: true, error: null });

      // Follow rule: open the subject stream strictly before reading the chat.
      unsubscribeStream = subscribeTargetEvents(
        target,
        handleStreamEvent,
        undefined,
        (streamState) => {
          patch({ streamState });
        },
      );

      try {
        let resource: ChatResource;
        try {
          resource = await readChat(target);
        } catch (error) {
          if (!archive && isMissingChatError(error)) {
            resource = await openChat(target);
          } else {
            throw error;
          }
        }
        if (seq !== readSeq) return;
        applySnapshot(resource);
        settled = true;
      } catch (error) {
        if (seq !== readSeq) return;
        patch({ loading: false, error });
      }
    },

    disconnect(): void {
      unsubscribeStream?.();
      unsubscribeStream = null;
      settled = false;
      readSeq += 1;
    },

    async openNew(): Promise<ChatResource | null> {
      if (archive) return state.chat;
      patch({ loading: true, error: null });
      try {
        const opened = await openChat(target);
        patch({ turns: [], endedBy: null });
        applySnapshot(opened);
        settled = true;
        return opened;
      } catch (error) {
        patch({ loading: false, error });
        return null;
      }
    },

    async loadOlder(): Promise<void> {
      if (!state.oldestTurnId || state.loadingOlder) return;
      patch({ loadingOlder: true });
      try {
        const page = await readChat(target, state.oldestTurnId);
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
        patch({ loadingOlder: false, error });
      }
    },

    setDraft(text: string): void {
      writeStoredDraft(draftKey, state.chat?.id ?? null, text);
      patch({ draft: text });
    },

    async send(overrideText?: string): Promise<void> {
      if (archive) return;
      const text = overrideText ?? state.draft;
      if (!text.trim()) return;
      const clientTurnId = crypto.randomUUID();
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
      await postWithTurnId(text, clientTurnId);
    },

    async retryTurn(clientTurnId: string): Promise<void> {
      const targetTurn = state.turns.find((turn) => turn.client_turn_id === clientTurnId);
      if (!targetTurn) return;
      const nextTurns = state.turns.map((turn) =>
        turn.client_turn_id === clientTurnId
          ? { ...turn, status: 'streaming' as const, delivery: 'sending' as const }
          : turn,
      );
      patch({ turns: nextTurns });
      await postWithTurnId(targetTurn.text, clientTurnId);
    },

    editFailedTurn(clientTurnId: string): void {
      const targetTurn = state.turns.find((turn) => turn.client_turn_id === clientTurnId);
      if (!targetTurn) return;
      writeStoredDraft(draftKey, state.chat?.id ?? null, targetTurn.text);
      patch({
        draft: targetTurn.text,
        turns: state.turns.filter((turn) => turn.client_turn_id !== clientTurnId),
      });
    },

    async stopReply(turnId?: string): Promise<void> {
      const active =
        turnId ??
        [...state.turns]
          .reverse()
          .find((turn) => turn.author !== 'operator' && turn.status === 'streaming')?.turn_id;
      if (!active) return;
      try {
        const updated = await stopChatTurn(target, active);
        applySnapshot(updated);
      } catch (error) {
        patch({ error });
      }
    },

    async acceptProposal(turnId: string): Promise<void> {
      patch({ proposalBusy: true, error: null });
      try {
        const updated = await acceptChatProposal(target, turnId);
        applySnapshot(updated);
      } catch (error) {
        patch({ error });
      } finally {
        patch({ proposalBusy: false });
      }
    },

    async rejectProposal(turnId: string): Promise<void> {
      patch({ proposalBusy: true, error: null });
      try {
        const updated = await rejectChatProposal(target, turnId);
        applySnapshot(updated);
      } catch (error) {
        patch({ error });
      } finally {
        patch({ proposalBusy: false });
      }
    },

    async end(): Promise<void> {
      try {
        const closed = await endChat(target);
        applySnapshot(closed);
      } catch (error) {
        patch({ error });
      }
    },
  };
}
