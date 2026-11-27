/** Wire subject kinds from `de.planmarshall.api.chat.ChatSubjectKind`. */
export type ChatSubjectKind = 'plan' | 'epic' | 'entry' | 'decision' | 'ended';

/** Wire scope kinds from `de.planmarshall.api.chat.ChatScopeKind`. */
export type ChatScopeKind =
  'plan' | 'epic' | 'draft' | 'item' | 'decision' | 'entry' | 'archive' | 'register';

/** Wire lifecycle states from `de.planmarshall.api.chat.ChatStatus`. */
export type ChatStatus =
  | 'chat.open'
  | 'chat.turn'
  | 'chat.reply'
  | 'chat.route'
  | 'chat.read-back'
  | 'chat.apply'
  | 'chat.end'
  | 'chat.carried';

export type TurnKind = 'message' | 'proposal' | 'notice';
export type TurnStatus = 'streaming' | 'complete' | 'stopped' | 'failed';
export type TurnFormat = 'plain' | 'markdown';
export type RefKind = 'task' | 'file' | 'decision' | 'plan' | 'hold' | 'act';
export type ProposalKind =
  'description' | 'revision' | 'rows' | 'draft' | 'item' | 'answer' | 'follow_up';
export type ProposalState =
  'open' | 'replaced' | 'accepted' | 'rejected' | 'applied' | 'refused' | 'lapsed';
export type EndedBy =
  'operator' | 'proposal_carried' | 'answered_elsewhere' | 'subject_ended' | 'entry_closed';

export interface MessageText {
  key: string;
  args?: Record<string, string | number | boolean>;
}

export interface RoutedRole {
  name: string;
  model: string;
}

export interface ChatSubject {
  kind: ChatSubjectKind;
  id: string;
}

export interface ChatContext {
  knows: MessageText[];
  as_of: string;
}

export interface TurnRef {
  kind: RefKind;
  id?: string | null;
  act?: string | null;
  subject?: ChatSubject | null;
}

export interface NarrativeLine {
  key: string;
  args: Record<string, string | number | boolean>;
  refs: TurnRef[];
  at: string;
}

export interface ChatTurn {
  turn_id: string;
  kind: TurnKind;
  author: string;
  status: TurnStatus;
  format: TurnFormat;
  untrusted: boolean;
  model?: string | null;
  text: string;
  steps: MessageText[];
  refs: TurnRef[];
  at: string;
  reason?: MessageText | null;
  lines?: NarrativeLine[];
}

export interface ChatProposal {
  turn_id: string;
  reply_turn_id: string;
  kind: ProposalKind;
  summary: string;
  applies?: MessageText | null;
  before?: string | null;
  after?: string | null;
  state: ProposalState;
  finding?: MessageText | null;
}

export interface ChatResource {
  id: string;
  subject: ChatSubject;
  role: RoutedRole;
  scope: ChatScopeKind;
  state: ChatStatus;
  context: ChatContext;
  proposal: ChatProposal | null;
  turns: ChatTurn[];
  before?: string | null;
}

export interface ChatTurnAccepted {
  turn_id: string;
}

/** Delivery state attached only on client-side optimistic operator turns. */
export type TurnDeliveryState = 'sending' | 'accepted' | 'not_sent';

export interface LocalChatTurn extends ChatTurn {
  delivery?: TurnDeliveryState;
  client_turn_id?: string;
}

export type DecisionScopeTarget =
  | { kind: 'plan'; id: string }
  | { kind: 'epic'; id: string }
  | { kind: 'entry'; id: string }
  | { kind: 'workspace'; projectId: string }
  | { kind: 'machine' };

export type ChatTarget =
  | { kind: 'plan'; id: string }
  | { kind: 'epic'; id: string }
  | { kind: 'entry'; id: string }
  | { kind: 'decision'; id: string; scope?: DecisionScopeTarget };

export interface ChatTurnStartedPayload {
  chat_id: string;
  turn_id: string;
  kind: TurnKind;
  author: string;
  format: TurnFormat;
  untrusted: boolean;
  model?: string | null;
  context?: ChatContext | null;
  text?: string | null;
  at?: string;
}

export interface ChatTurnDeltaPayload {
  chat_id: string;
  turn_id: string;
  text: string;
}

export interface ChatTurnProgressPayload {
  chat_id: string;
  turn_id: string;
  step: MessageText;
}

export interface ChatTurnCompletedPayload {
  chat_id: string;
  turn_id: string;
  kind: TurnKind;
  status: TurnStatus;
  untrusted: boolean;
  text: string;
  refs?: TurnRef[];
  reason?: MessageText | null;
  lines?: NarrativeLine[];
  at?: string;
}

export interface ChatProposalPayload {
  chat_id: string;
  turn_id: string;
  reply_turn_id: string;
  kind: ProposalKind;
  state: ProposalState;
  finding?: MessageText | null;
}

export interface ChatEndedPayload {
  chat_id: string;
  ended_by: EndedBy;
  channel?: string | null;
  device?: string | null;
}

export const CATALOGUED_CHAT_EVENTS = new Set<string>([
  'chat.turn.started',
  'chat.turn.delta',
  'chat.turn.progress',
  'chat.turn.completed',
  'chat.proposal',
  'chat.ended',
]);

/**
 * @param state a chat lifecycle state
 * @returns `true` once the chat has ended
 */
export function isChatEndedState(state: ChatStatus | undefined): boolean {
  return state === 'chat.end' || state === 'chat.carried';
}
