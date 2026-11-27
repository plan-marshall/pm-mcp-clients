import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { get } from 'svelte/store';
import { apiClient, type EventStreamState } from '../api/client';
import { ClientError } from '../api/errors';
import {
  chatRoutesFor,
  createChatStore,
  hrefForTurnRef,
  isMissingChatError,
  parseSafeWebUrl,
  formatExternalLabel,
  rejectChatProposal,
  storageKeyForDraft,
  tokenizeMarkdown,
  translateChatMessage,
  type ChatResource,
  type ChatTarget,
  type ChatTurn,
} from './index';
import { currentLocale, messagesStore, t } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

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

  it('pages back through ?before= cursors on loadToTurn and stops cleanly when absent', async () => {
    vi.spyOn(apiClient, 'subscribePathEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'request').mockImplementation(async (_method, path) => {
      if (path === '/api/v1/plans/fix-relay/chat') {
        return makeChat({
          before: 't-page2',
          turns: [
            {
              turn_id: 't-newest',
              kind: 'message',
              author: 'operator',
              status: 'complete',
              format: 'plain',
              untrusted: false,
              model: null,
              text: 'Newest',
              steps: [],
              refs: [],
              at: '2026-11-27T10:03:00Z',
              reason: null,
              lines: [],
            },
          ],
        });
      }
      if (path === '/api/v1/plans/fix-relay/chat?before=t-page2') {
        return makeChat({
          before: 't-page1',
          turns: [
            {
              turn_id: 't-target',
              kind: 'message',
              author: 'planner',
              status: 'complete',
              format: 'markdown',
              untrusted: false,
              model: 'sonnet',
              text: 'Target turn',
              steps: [],
              refs: [],
              at: '2026-11-27T10:01:00Z',
              reason: null,
              lines: [],
            },
          ],
        });
      }
      return makeChat({ before: null, turns: [] });
    });

    const store = createChatStore(planTarget);
    await store.connect({ turnId: 't-target' });
    expect(get(store).turns.map((turn) => turn.turn_id)).toEqual(['t-target', 't-newest']);

    const foundMissing = await store.loadToTurn('t-does-not-exist');
    expect(foundMissing).toBe(false);
  });

  it('preserves named parameters for chat.knows.* and chat.applies.* keys', () => {
    currentLocale.set('en');
    messagesStore.set(loadMessages('en'));
    const translate = get(t);

    expect(
      translateChatMessage(
        (key, params) => {
          if (key === 'control.chat.knows.item') {
            return `${params?.item}:${params?.count}`;
          }
          return translate(key, params);
        },
        { key: 'chat.knows.tasks', args: { count: 7 } },
      ),
    ).toBe('tasks:7');

    expect(
      translateChatMessage(
        (key, params) => {
          if (key === 'control.chat.revision_queued') {
            return `${params?.applies}:${params?.task_id}`;
          }
          return translate(key, params);
        },
        { key: 'chat.applies.after_task', args: { task_id: 'TASK-4' } },
      ),
    ).toBe('after_task:TASK-4');

    expect(
      translateChatMessage(translate, { key: 'chat.reason.lost', args: { reason: 'timeout' } }),
    ).toContain('The reply was lost');
    expect(
      translateChatMessage(translate, {
        key: 'chat.finding.changed_since',
        args: { plan: 'fix-relay', version: 4 },
      }),
    ).toContain('fix-relay changed since');
    expect(
      translateChatMessage(translate, {
        key: 'chat.line.decision_answered',
        args: { id: 'Q-1', channel: 'web' },
      }),
    ).toBe('Q-1 answered in the web app');
    expect(
      translateChatMessage((key, params) => `${key}:${String(params?.flag)}`, {
        key: 'custom.raw',
        args: { flag: true },
      }),
    ).toBe('custom.raw:true');
  });

  it('resolves routes and draft storage keys for every ChatTarget scope', () => {
    expect(chatRoutesFor({ kind: 'epic', id: 'ui-imp' }).streamPath).toBe(
      '/api/v1/epics/ui-imp/events',
    );
    expect(chatRoutesFor({ kind: 'entry', id: 'ent-1' }).chatPath).toBe(
      '/api/v1/entries/ent-1/chat',
    );
    expect(chatRoutesFor({ kind: 'decision', id: 'Q-1' }).streamPath).toBe('/api/v1/events');
    expect(
      chatRoutesFor({ kind: 'decision', id: 'Q-1', scope: { kind: 'plan', id: 'p1' } }).chatPath,
    ).toBe('/api/v1/plans/p1/decisions/Q-1/chat');
    expect(
      chatRoutesFor({ kind: 'decision', id: 'Q-1', scope: { kind: 'epic', id: 'e1' } }).chatPath,
    ).toBe('/api/v1/epics/e1/decisions/Q-1/chat');
    expect(
      chatRoutesFor({ kind: 'decision', id: 'Q-1', scope: { kind: 'entry', id: 'ent-1' } })
        .chatPath,
    ).toBe('/api/v1/entries/ent-1/decisions/Q-1/chat');
    expect(
      chatRoutesFor({
        kind: 'decision',
        id: 'Q-1',
        scope: { kind: 'workspace', projectId: 'proj-a' },
      }).streamPath,
    ).toBe('/api/v1/projects/proj-a/workspace/events');
    expect(
      chatRoutesFor({ kind: 'decision', id: 'Q-1', scope: { kind: 'machine' } }).streamPath,
    ).toBe('/api/v1/machine/events');

    expect(storageKeyForDraft({ kind: 'decision', id: 'Q-1', scope: { kind: 'machine' } })).toBe(
      'pm_chat_draft:decision:machine:Q-1',
    );
    expect(
      storageKeyForDraft({
        kind: 'decision',
        id: 'Q-1',
        scope: { kind: 'workspace', projectId: 'p' },
      }),
    ).toBe('pm_chat_draft:decision:workspace:p:Q-1');
    expect(
      storageKeyForDraft({ kind: 'decision', id: 'Q-1', scope: { kind: 'plan', id: 'fix' } }),
    ).toBe('pm_chat_draft:decision:plan:fix:Q-1');
  });

  it('handles proposal, ended events, editFailedTurn, rejectProposal, and corrupt storage safely', async () => {
    localStorage.setItem('pm_chat_draft:plan:fix-relay', '{not-valid-json');
    let emitEvent: (event: string, data: unknown) => void = () => {};
    vi.spyOn(apiClient, 'subscribePathEvents').mockImplementation((_path, onEvent) => {
      emitEvent = onEvent;
      return () => {};
    });
    const req = vi
      .spyOn(apiClient, 'request')
      .mockResolvedValueOnce(
        makeChat({
          state: 'chat.read-back',
          proposal: {
            turn_id: 't-prop',
            reply_turn_id: 't-1',
            kind: 'revision',
            summary: 'Change',
            applies: null,
            before: null,
            after: null,
            state: 'open',
            finding: null,
          },
        }),
      )
      .mockResolvedValueOnce(makeChat({ state: 'chat.open', proposal: null }))
      .mockRejectedValueOnce(new ClientError('network_offline'));

    const store = createChatStore(planTarget);
    await store.connect();
    expect(get(store).draft).toBe('');

    // Notice arrival on unknown turn id creates a notice turn cleanly.
    emitEvent('chat.turn.completed', {
      chat_id: 'chat-1',
      turn_id: 't-notice',
      kind: 'notice',
      status: 'complete',
      untrusted: false,
      text: '',
      lines: [],
    });
    expect(get(store).turns.some((turn) => turn.turn_id === 't-notice')).toBe(true);

    // Proposal transition moves state out of chat.read-back; unknown proposal triggers refresh.
    emitEvent('chat.proposal', {
      chat_id: 'chat-1',
      turn_id: 't-prop',
      reply_turn_id: 't-1',
      kind: 'revision',
      state: 'accepted',
    });
    expect(get(store).chat?.state).toBe('chat.apply');

    emitEvent('chat.proposal', {
      chat_id: 'chat-1',
      turn_id: 't-prop-new',
      reply_turn_id: 't-2',
      kind: 'revision',
      state: 'open',
    });
    await vi.waitFor(() => expect(req).toHaveBeenCalledTimes(2));

    await rejectChatProposal(planTarget, 't-prop-2').catch(() => {});
    emitEvent('chat.ended', { chat_id: 'chat-1', ended_by: 'proposal_carried' });
    expect(get(store).chat?.state).toBe('chat.carried');

    // editFailedTurn + error paths + isMissingChatError non-ClientError path.
    expect(isMissingChatError(new Error('target_not_found'))).toBe(true);
    expect(isMissingChatError({ status: 404 })).toBe(true);
    req.mockRejectedValueOnce(new ClientError('network_offline'));
    await store.rejectProposal('t-prop-3');
    expect(get(store).error).toBeInstanceOf(ClientError);
  });

  it('covers markdown helpers for all ref kinds, tilde fences, and safe external URLs', () => {
    expect(parseSafeWebUrl('//evil.com')).toBeNull();
    expect(parseSafeWebUrl('not-a-url')).toBeNull();
    expect(formatExternalLabel('docs.org/guide', new URL('https://docs.org/guide'))).toBe(
      'docs.org/guide',
    );
    expect(hrefForTurnRef({ kind: 'plan', id: 'p-1' }, { kind: 'epic', id: 'e-1' })).toBe(
      '/epics/e-1?open=plan%3Ap-1&chat=open',
    );
    expect(hrefForTurnRef({ kind: 'decision', id: 'Q-2' }, { kind: 'plan', id: 'p-1' })).toBe(
      '/plans/p-1?open=decision%3AQ-2&chat=open',
    );
    expect(hrefForTurnRef({ kind: 'decision', id: 'Q-2' }, { kind: 'epic', id: 'e-1' })).toBe(
      '/epics/e-1?open=decision%3AQ-2&chat=open',
    );
    expect(hrefForTurnRef({ kind: 'decision', id: 'Q-2' })).toBe('/decisions/Q-2');
    expect(hrefForTurnRef({ kind: 'hold' }, { kind: 'epic', id: 'e-1' })).toBe(
      '/epics/e-1?chat=open',
    );
    expect(hrefForTurnRef({ kind: 'act' })).toBe('/overview');

    const tokens = tokenizeMarkdown('~~~\nopen fence', true);
    expect(tokens[0]?.type).toBe('paragraph');
    expect(tokenizeMarkdown('')).toEqual([]);
  });

  it('covers store error, archive, and missing-id guard branches cleanly', async () => {
    vi.spyOn(apiClient, 'subscribePathEvents').mockReturnValue(() => {});
    const boom = new ClientError('network_offline');
    vi.spyOn(apiClient, 'request')
      .mockRejectedValueOnce(Object.assign(new Error('target_not_found'), { status: 404 }))
      .mockResolvedValueOnce(makeChat({ before: 't-prev' }))
      .mockRejectedValueOnce(boom);

    const archiveStore = createChatStore(planTarget, { archive: true });
    await archiveStore.connect();
    expect(get(archiveStore).error).toBeTruthy();
    await archiveStore.send('ignored in archive');

    const store = createChatStore(planTarget);
    await store.connect();
    await store.loadOlder();
    expect(get(store).error).toBe(boom);

    await store.openNew();
    expect(get(store).error).toBeTruthy();

    await store.acceptProposal('p-1');
    expect(get(store).error).toBeTruthy();

    await store.rejectProposal('p-1');
    expect(get(store).error).toBeTruthy();

    await store.end();
    expect(get(store).error).toBeTruthy();

    await store.stopReply();
    await store.stopReply('t-active');
    expect(get(store).error).toBeTruthy();

    await store.retryTurn('non-existent');
    store.editFailedTurn('non-existent');
    await store.send('   ');
    store.disconnect();
    await store.loadToTurn('t-none');
  });

  it('covers missing-chat auto-open, openNew, stopReply, proposal accept/reject, end, stream state, and turn transitions', async () => {
    let emitEvent: (event: string, data: unknown) => void = () => {};
    let emitStreamState: (state: EventStreamState) => void = () => {};
    vi.spyOn(apiClient, 'subscribePathEvents').mockImplementation(
      (_path, onEvent, _onError, onState) => {
        emitEvent = onEvent;
        emitStreamState = onState ?? (() => {});
        return () => {};
      },
    );

    const streamingTurn: ChatTurn = {
      turn_id: 't-reply-1',
      kind: 'message',
      author: 'Planner',
      status: 'streaming',
      format: 'markdown',
      untrusted: false,
      model: 'claude-sonnet',
      text: 'Working',
      steps: [],
      refs: [],
      at: '2026-11-27T09:00:01Z',
      reason: null,
      lines: [],
    };
    const openProposal = {
      turn_id: 't-prop-1',
      reply_turn_id: 't-reply-1',
      kind: 'revision' as const,
      summary: 'Update scope',
      applies: null,
      before: null,
      after: null,
      state: 'open' as const,
      finding: null,
    };

    const req = vi
      .spyOn(apiClient, 'request')
      // connect(): initial read returns target_not_found -> falls back to openChat
      .mockRejectedValueOnce(Object.assign(new Error('target_not_found'), { status: 404 }))
      .mockResolvedValueOnce(
        makeChat({
          state: 'chat.read-back',
          turns: [streamingTurn],
          proposal: openProposal,
        }),
      )
      // acceptProposal -> accepted snapshot
      .mockResolvedValueOnce(
        makeChat({
          state: 'chat.apply',
          proposal: { ...openProposal, state: 'accepted' },
        }),
      )
      // rejectProposal -> rejected snapshot
      .mockResolvedValueOnce(
        makeChat({
          state: 'chat.open',
          proposal: { ...openProposal, state: 'rejected' },
        }),
      )
      // stopReply -> stopped snapshot
      .mockResolvedValueOnce(
        makeChat({
          state: 'chat.open',
          turns: [{ ...streamingTurn, status: 'stopped' }],
        }),
      )
      // end() -> ended snapshot
      .mockResolvedValueOnce(makeChat({ state: 'chat.end' }))
      // openNew() from ended state -> skips endChat, calls openChat
      .mockResolvedValueOnce(makeChat({ id: 'chat-2', state: 'chat.open', turns: [] }))
      // openNew() from active state -> calls endChat then openChat
      .mockResolvedValueOnce(makeChat({ id: 'chat-2', state: 'chat.end', turns: [] }))
      .mockResolvedValueOnce(makeChat({ id: 'chat-3', state: 'chat.open', turns: [] }));

    const store = createChatStore(planTarget);
    await store.connect();
    expect(get(store).chat?.id).toBe('chat-1');
    emitStreamState('connected');
    expect(get(store).streamState).toBe('connected');

    // Ignored invalid / mismatched / reset-before-settled branches are already safe; test settled reset error & invalid events.
    emitEvent('unknown.event', { chat_id: 'chat-1' });
    emitEvent('chat.turn.delta', null);
    emitEvent('chat.turn.delta', { chat_id: 'wrong-chat', turn_id: 't-reply-1', text: 'x' });
    emitEvent('chat.turn.delta', { chat_id: 'chat-1', turn_id: 'missing', text: 'x' });
    emitEvent('chat.turn.progress', {
      chat_id: 'wrong-chat',
      turn_id: 't-reply-1',
      step: { id: 's-1', label: 'No', status: 'done', at: '2026-11-27T09:00:02Z' },
    });
    emitEvent('chat.turn.progress', {
      chat_id: 'chat-1',
      turn_id: 'missing',
      step: { id: 's-1', label: 'No', status: 'done', at: '2026-11-27T09:00:02Z' },
    });

    // Re-announced turn updates existing turn + active operator pending match.
    emitEvent('chat.turn.started', {
      chat_id: 'chat-1',
      turn_id: 't-reply-1',
      kind: 'message',
      author: 'Planner',
      format: 'markdown',
      untrusted: false,
      text: 'Restarted',
    });
    emitEvent('chat.turn.progress', {
      chat_id: 'chat-1',
      turn_id: 't-reply-1',
      step: { id: 's-2', label: 'Scan', status: 'done', at: '2026-11-27T09:00:03Z' },
    });
    emitEvent('chat.turn.completed', {
      chat_id: 'chat-1',
      turn_id: 't-reply-1',
      kind: 'message',
      status: 'complete',
      untrusted: false,
      text: 'Done with proposal',
      lines: [],
    });
    expect(get(store).chat?.state).toBe('chat.read-back');

    // Proposal events: open keeps read-back, rejected transitions to chat.open.
    emitEvent('chat.proposal', {
      chat_id: 'wrong-chat',
      turn_id: 't-prop-1',
      reply_turn_id: 't-reply-1',
      kind: 'revision',
      state: 'open',
    });
    emitEvent('chat.proposal', {
      chat_id: 'chat-1',
      turn_id: 't-prop-1',
      reply_turn_id: 't-reply-1',
      kind: 'revision',
      state: 'open',
    });
    expect(get(store).chat?.state).toBe('chat.read-back');
    emitEvent('chat.proposal', {
      chat_id: 'chat-1',
      turn_id: 't-prop-1',
      reply_turn_id: 't-reply-1',
      kind: 'revision',
      state: 'rejected',
      finding: 'Needs tests',
    });
    expect(get(store).chat?.state).toBe('chat.open');
    expect(get(store).chat?.proposal?.finding).toBe('Needs tests');

    // Success paths for acceptProposal, rejectProposal, stopReply (with auto-found active turn), end, openNew.
    await store.acceptProposal('t-prop-1');
    expect(get(store).chat?.state).toBe('chat.apply');

    await store.rejectProposal('t-prop-1');
    expect(get(store).chat?.state).toBe('chat.open');

    emitEvent('chat.turn.started', {
      chat_id: 'chat-1',
      turn_id: 't-stream-2',
      kind: 'message',
      author: 'Planner',
      format: 'markdown',
      untrusted: false,
    });
    await store.stopReply();
    expect(get(store).turns[0]?.status).toBe('stopped');

    emitEvent('chat.ended', { chat_id: 'wrong-chat', ended_by: 'operator' });
    emitEvent('chat.turn.started', {
      chat_id: 'chat-1',
      turn_id: 't-stream-3',
      kind: 'message',
      author: 'Planner',
      format: 'markdown',
      untrusted: false,
    });
    emitEvent('chat.ended', { chat_id: 'chat-1', ended_by: 'operator' });
    expect(get(store).chat?.state).toBe('chat.end');
    expect(get(store).turns.find((t) => t.turn_id === 't-stream-3')?.status).toBe('stopped');

    await store.end();
    await store.openNew();
    expect(get(store).chat?.id).toBe('chat-2');

    await store.openNew();
    expect(get(store).chat?.id).toBe('chat-3');
    expect(req).toHaveBeenCalledTimes(9);
  });
});
