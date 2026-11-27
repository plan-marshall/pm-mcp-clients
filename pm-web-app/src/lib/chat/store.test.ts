import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { get } from 'svelte/store';
import { apiClient } from '../api/client';
import { ClientError } from '../api/errors';
import { createChatStore } from './store';
import type { ChatResource, ChatTarget } from './types';

function makeChat(overrides: Partial<ChatResource> = {}): ChatResource {
  return {
    id: 'chat-1',
    subject: { kind: 'plan', id: 'fix-relay' },
    role: { name: 'planner', model: 'sonnet' },
    scope: 'plan',
    state: 'chat.open',
    context: { knows: [], as_of: '2026-11-27T10:00:00Z' },
    proposal: null,
    turns: [],
    before: null,
    ...overrides,
  };
}

describe('chat store and follow lifecycle', () => {
  const planTarget: ChatTarget = { kind: 'plan', id: 'fix-relay' };

  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('opens the subject event stream before reading chat, ignores pre-settle events, and applies post-settle deltas in order', async () => {
    const order: string[] = [];
    let emitEvent: (event: string, data: unknown) => void = () => {};
    let resolveRead!: (value: ChatResource) => void;

    vi.spyOn(apiClient, 'subscribePathEvents').mockImplementation((path, onEvent) => {
      order.push(`stream:${path}`);
      emitEvent = onEvent;
      return () => order.push('unsubscribe');
    });
    vi.spyOn(apiClient, 'request').mockImplementation((method, path) => {
      order.push(`${method}:${path}`);
      return new Promise<ChatResource>((resolve) => {
        resolveRead = resolve;
      }) as Promise<never>;
    });

    const store = createChatStore(planTarget);
    const pendingConnect = store.connect();

    // Stream opens strictly before GET .../chat is dispatched.
    expect(order[0]).toBe('stream:/api/v1/plans/fix-relay/events');
    expect(order[1]).toBe('GET:/api/v1/plans/fix-relay/chat');

    // Event emitted before the read settles must be ignored.
    emitEvent('chat.turn.started', {
      chat_id: 'chat-1',
      turn_id: 't-stale',
      kind: 'message',
      author: 'planner',
      format: 'markdown',
      untrusted: false,
      model: 'sonnet',
      context: { knows: [], as_of: '2026-11-27T10:00:00Z' },
      text: null,
    });

    resolveRead(makeChat());
    await pendingConnect;
    expect(get(store).turns).toEqual([]);

    // Post-settle events for another chat are ignored.
    emitEvent('chat.turn.started', {
      chat_id: 'chat-other',
      turn_id: 't-other',
      kind: 'message',
      author: 'planner',
      format: 'markdown',
      untrusted: false,
      model: 'sonnet',
      context: { knows: [], as_of: '2026-11-27T10:00:00Z' },
      text: null,
    });
    expect(get(store).turns).toEqual([]);

    // Post-settle deltas append in order.
    emitEvent('chat.turn.started', {
      chat_id: 'chat-1',
      turn_id: 't-reply',
      kind: 'message',
      author: 'planner',
      format: 'markdown',
      untrusted: false,
      model: 'sonnet',
      context: { knows: [], as_of: '2026-11-27T10:00:00Z' },
      text: null,
    });
    emitEvent('chat.turn.progress', {
      chat_id: 'chat-1',
      turn_id: 't-reply',
      step: { key: 'chat.step.reading', args: {} },
    });
    emitEvent('chat.turn.delta', { chat_id: 'chat-1', turn_id: 't-reply', text: 'First ' });
    emitEvent('chat.turn.delta', { chat_id: 'chat-1', turn_id: 't-reply', text: 'second.' });

    const reply = get(store).turns.find((t) => t.turn_id === 't-reply');
    expect(reply?.text).toBe('First second.');
    expect(reply?.steps).toEqual([{ key: 'chat.step.reading', args: {} }]);
    expect(reply?.status).toBe('streaming');

    emitEvent('chat.turn.completed', {
      chat_id: 'chat-1',
      turn_id: 't-reply',
      kind: 'message',
      status: 'complete',
      untrusted: false,
      text: 'First second.',
      refs: [],
      reason: null,
      lines: [],
    });
    expect(get(store).turns.find((t) => t.turn_id === 't-reply')?.status).toBe('complete');
    store.disconnect();
    expect(order).toContain('unsubscribe');
  });

  it('re-reads the chat when the stream emits reset', async () => {
    let emitEvent: (event: string, data: unknown) => void = () => {};
    vi.spyOn(apiClient, 'subscribePathEvents').mockImplementation((_path, onEvent) => {
      emitEvent = onEvent;
      return () => {};
    });
    const req = vi
      .spyOn(apiClient, 'request')
      .mockResolvedValueOnce(makeChat({ turns: [] }))
      .mockResolvedValueOnce(
        makeChat({
          turns: [
            {
              turn_id: 't-after-reset',
              kind: 'message',
              author: 'planner',
              status: 'complete',
              format: 'markdown',
              untrusted: false,
              model: 'sonnet',
              text: 'Recovered after reset.',
              steps: [],
              refs: [],
              at: '2026-11-27T10:01:00Z',
              reason: null,
              lines: [],
            },
          ],
        }),
      );

    const store = createChatStore(planTarget);
    await store.connect();
    expect(get(store).turns).toHaveLength(0);

    emitEvent('reset', {});

    await vi.waitFor(() => {
      expect(req).toHaveBeenCalledTimes(2);
      expect(get(store).turns[0]?.text).toBe('Recovered after reset.');
    });
  });

  it('opens an unseeded chat before sending the first turn', async () => {
    const calls: string[] = [];
    vi.spyOn(apiClient, 'subscribePathEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'request').mockImplementation(async (method, path) => {
      calls.push(`${method}:${path}`);
      if (method === 'GET' && path === '/api/v1/plans/fix-relay/chat') {
        throw Object.assign(new Error('target_not_found'), { status: 404 });
      }
      if (method === 'POST' && path === '/api/v1/plans/fix-relay/chat') {
        return makeChat({ id: 'chat-new' });
      }
      if (method === 'POST' && path === '/api/v1/plans/fix-relay/chat/turns') {
        return { turn_id: 't-1' };
      }
      throw new Error(`Unexpected ${method} ${path}`);
    });

    const store = createChatStore(planTarget);
    await store.send('Start from scratch');

    expect(calls).toEqual([
      'POST:/api/v1/plans/fix-relay/chat',
      'POST:/api/v1/plans/fix-relay/chat/turns',
    ]);
  });

  it('keeps the draft on failure and sends the same client_turn_id on retry until 202 clears it', async () => {
    vi.spyOn(apiClient, 'subscribePathEvents').mockReturnValue(() => {});
    const sentPayloads: Array<{ text: string; client_turn_id: string }> = [];
    vi.spyOn(apiClient, 'request').mockImplementation(async (method, path, body) => {
      if (method === 'GET' && path === '/api/v1/plans/fix-relay/chat') {
        return makeChat();
      }
      if (method === 'POST' && path === '/api/v1/plans/fix-relay/chat/turns') {
        const payload = body as { text: string; client_turn_id: string };
        sentPayloads.push(payload);
        if (sentPayloads.length === 1) {
          throw new ClientError('network_offline');
        }
        return { turn_id: 't-server-1' };
      }
      throw new Error(`Unexpected ${method} ${path}`);
    });

    const store = createChatStore(planTarget);
    await store.connect();
    store.setDraft('Keep this draft safe');

    await store.send();

    const failedState = get(store);
    expect(failedState.draft).toBe('Keep this draft safe');
    expect(localStorage.getItem('pm_chat_draft:plan:fix-relay')).toContain('Keep this draft safe');
    const failedTurn = failedState.turns.find((t) => t.delivery === 'not_sent');
    expect(failedTurn).toBeTruthy();
    expect(failedTurn?.client_turn_id).toBeTruthy();

    await store.retryTurn(failedTurn!.client_turn_id!);

    expect(sentPayloads).toHaveLength(2);
    expect(sentPayloads[1].client_turn_id).toBe(sentPayloads[0].client_turn_id);
    expect(get(store).draft).toBe('');
    expect(localStorage.getItem('pm_chat_draft:plan:fix-relay')).toBeNull();
    expect(
      get(store).turns.some((t) => t.turn_id === 't-server-1' && t.delivery === 'accepted'),
    ).toBe(true);
  });

  it('stops, ends, answers proposals, and loads older pages before a turn without duplicates', async () => {
    vi.spyOn(apiClient, 'subscribePathEvents').mockReturnValue(() => {});
    const req = vi.spyOn(apiClient, 'request').mockImplementation(async (method, path, body) => {
      if (method === 'GET' && path === '/api/v1/plans/fix-relay/chat') {
        return makeChat({
          before: 't-2',
          turns: [
            {
              turn_id: 't-2',
              kind: 'message',
              author: 'operator',
              status: 'complete',
              format: 'plain',
              untrusted: false,
              model: null,
              text: 'Newer turn',
              steps: [],
              refs: [],
              at: '2026-11-27T10:02:00Z',
              reason: null,
              lines: [],
            },
          ],
          proposal: {
            turn_id: 't-prop',
            reply_turn_id: 't-2',
            kind: 'revision',
            summary: 'Update step 2',
            applies: null,
            before: 'old',
            after: 'new',
            state: 'open',
            finding: null,
          },
        });
      }
      if (method === 'GET' && path === '/api/v1/plans/fix-relay/chat?before=t-2') {
        return makeChat({
          before: null,
          turns: [
            {
              turn_id: 't-1',
              kind: 'message',
              author: 'operator',
              status: 'complete',
              format: 'plain',
              untrusted: false,
              model: null,
              text: 'Older turn',
              steps: [],
              refs: [],
              at: '2026-11-27T10:01:00Z',
              reason: null,
              lines: [],
            },
          ],
        });
      }
      if (method === 'POST' && path === '/api/v1/plans/fix-relay/chat/turns/t-2/stop') {
        return makeChat();
      }
      if (method === 'POST' && path === '/api/v1/plans/fix-relay/chat/proposal/accept') {
        expect(body).toEqual({ turn_id: 't-prop' });
        return makeChat({ proposal: null });
      }
      if (method === 'POST' && path === '/api/v1/plans/fix-relay/chat/end') {
        return makeChat({ state: 'chat.end' });
      }
      throw new Error(`Unexpected ${method} ${path}`);
    });

    const store = createChatStore(planTarget);
    await store.connect();
    await store.loadOlder();
    expect(get(store).turns.map((t) => t.turn_id)).toEqual(['t-1', 't-2']);

    await store.stopReply('t-2');
    await store.acceptProposal('t-prop');
    await store.end();

    expect(req).toHaveBeenCalledWith('POST', '/api/v1/plans/fix-relay/chat/turns/t-2/stop', {});
    expect(req).toHaveBeenCalledWith('POST', '/api/v1/plans/fix-relay/chat/proposal/accept', {
      turn_id: 't-prop',
    });
    expect(req).toHaveBeenCalledWith('POST', '/api/v1/plans/fix-relay/chat/end', {});
    expect(get(store).chat?.state).toBe('chat.end');
  });
});
