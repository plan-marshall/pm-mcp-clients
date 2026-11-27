import { apiClient, type EventStreamState } from '../api/client';
import { errorCodeOf } from '../api/errors';
import { chatRoutesFor } from './routes';
import type { ChatResource, ChatTarget, ChatTurnAccepted } from './types';

/**
 * Opens a chat for `target` (`POST …/chat`).
 *
 * @param target the chat's subject
 * @returns the opened (or already open) chat resource
 */
export function openChat(target: ChatTarget): Promise<ChatResource> {
  return apiClient.request<ChatResource>('POST', chatRoutesFor(target).chatPath);
}

/**
 * Reads the current chat or an older turn page before `beforeTurnId` (`GET …/chat`).
 *
 * @param target the chat's subject
 * @param beforeTurnId optional cursor turn id for paging older turns
 * @returns the chat snapshot
 */
export function readChat(target: ChatTarget, beforeTurnId?: string): Promise<ChatResource> {
  const routes = chatRoutesFor(target);
  const path = beforeTurnId ? routes.pagePath(beforeTurnId) : routes.chatPath;
  return apiClient.request<ChatResource>('GET', path);
}

/**
 * Posts one operator message (`POST …/chat/turns`) with an idempotent `client_turn_id`.
 *
 * @param target the chat's subject
 * @param text message text
 * @param clientTurnId stable UUID kept across retries of the same turn
 * @returns `202` acknowledgement containing the assigned `turn_id`
 */
export function sendChatTurn(
  target: ChatTarget,
  text: string,
  clientTurnId: string,
): Promise<ChatTurnAccepted> {
  return apiClient.request<ChatTurnAccepted>('POST', chatRoutesFor(target).turnsPath, {
    text,
    client_turn_id: clientTurnId,
  });
}

/**
 * Stops an in-flight role reply (`POST …/chat/turns/{turn_id}/stop`).
 *
 * @param target the chat's subject
 * @param turnId reply turn id to stop
 * @returns updated chat resource
 */
export function stopChatTurn(target: ChatTarget, turnId: string): Promise<ChatResource> {
  return apiClient.request<ChatResource>('POST', chatRoutesFor(target).stopPath(turnId), {});
}

/**
 * Ends the active chat (`POST …/chat/end`).
 *
 * @param target the chat's subject
 * @returns updated chat resource
 */
export function endChat(target: ChatTarget): Promise<ChatResource> {
  return apiClient.request<ChatResource>('POST', chatRoutesFor(target).endPath, {});
}

/**
 * Accepts the open proposal (`POST …/chat/proposal/accept`).
 *
 * @param target the chat's subject
 * @param turnId proposal turn id
 * @returns updated chat resource
 */
export function acceptChatProposal(target: ChatTarget, turnId: string): Promise<ChatResource> {
  return apiClient.request<ChatResource>('POST', chatRoutesFor(target).acceptPath, {
    turn_id: turnId,
  });
}

/**
 * Rejects the open proposal (`POST …/chat/proposal/reject`).
 *
 * @param target the chat's subject
 * @param turnId proposal turn id
 * @returns updated chat resource
 */
export function rejectChatProposal(target: ChatTarget, turnId: string): Promise<ChatResource> {
  return apiClient.request<ChatResource>('POST', chatRoutesFor(target).rejectPath, {
    turn_id: turnId,
  });
}

/**
 * Opens the SSE stream of `target`'s subject before reading chat state.
 *
 * @param target the chat's subject
 * @param onEvent event handler
 * @param signal optional abort signal
 * @param onState optional stream state callback
 * @returns unsubscribe handle
 */
export function subscribeTargetEvents(
  target: ChatTarget,
  onEvent: (event: string, data: unknown) => void,
  signal?: AbortSignal,
  onState?: (state: EventStreamState) => void,
): () => void {
  return apiClient.subscribePathEvents(chatRoutesFor(target).streamPath, onEvent, signal, onState);
}

/**
 * @param error caught request error
 * @returns `true` when the runtime has no open or seeded chat on that subject yet
 */
export function isMissingChatError(error: unknown): boolean {
  if ((error as { status?: number } | null)?.status === 404) {
    return true;
  }
  return errorCodeOf(error) === 'target_not_found';
}
