<script lang="ts">
  import { writable } from 'svelte/store';
  import type { ChatStore, ChatStoreState } from '../chat/store';
  import type { ChatResource, ChatTarget, LocalChatTurn } from '../chat/types';
  import Chat from '../components/Chat.svelte';
  import ChatMarkdown from '../components/ChatMarkdown.svelte';
  import ChatTurn from '../components/ChatTurn.svelte';

  const subject: ChatTarget & { title?: string } = {
    kind: 'plan',
    id: 'fix-relay',
    title: 'Fix Relay Transport',
  };

  const sampleTurns: LocalChatTurn[] = [
    {
      turn_id: 'dev-t1',
      kind: 'message',
      author: 'operator',
      status: 'complete',
      format: 'plain',
      untrusted: false,
      model: null,
      text: 'Which task holds up #TASK-4 right now?',
      steps: [],
      refs: [],
      at: '2026-11-27T09:10:00Z',
      reason: null,
      lines: [],
    },
    {
      turn_id: 'dev-t2',
      kind: 'message',
      author: 'planner',
      status: 'complete',
      format: 'markdown',
      untrusted: false,
      model: 'sonnet',
      text: 'TASK-4 waits on `retryPolicy` in src/RelayClient.ts:42:\n\n- Backoff stays capped\n- Streams reconnect cleanly',
      steps: [
        { key: 'chat.step.read', args: { target: 'src/RelayClient.ts' } },
        { key: 'chat.step.search', args: { target: 'TASK-4' } },
      ],
      refs: [
        { kind: 'task', id: 'TASK-4', subject: { kind: 'plan', id: 'fix-relay' } },
        { kind: 'file', id: 'src/RelayClient.ts:42', subject: { kind: 'plan', id: 'fix-relay' } },
      ],
      at: '2026-11-27T09:11:00Z',
      reason: null,
      lines: [],
    },
    {
      turn_id: 'dev-t3',
      kind: 'notice',
      author: 'planner',
      status: 'complete',
      format: 'plain',
      untrusted: false,
      model: null,
      text: '',
      steps: [],
      refs: [],
      at: '2026-11-27T09:12:00Z',
      reason: null,
      lines: [
        {
          key: 'chat.line.task_ended',
          args: { task_id: 'TASK-3', outcome: 'done' },
          refs: [],
          at: '2026-11-27T09:12:00Z',
        },
      ],
    },
  ];

  const fixtureChat: ChatResource = {
    id: 'dev-chat-1',
    subject: { kind: 'plan', id: 'fix-relay' },
    role: { name: 'planner', model: 'sonnet' },
    scope: 'plan',
    state: 'chat.open',
    context: {
      knows: [
        { key: 'chat.knows.description', args: {} },
        { key: 'chat.knows.tasks', args: { count: 4 } },
      ],
      as_of: '2026-11-27T09:12:00Z',
    },
    proposal: null,
    turns: sampleTurns,
    before: null,
  };

  const inner = writable<ChatStoreState>({
    target: subject,
    archive: false,
    chat: fixtureChat,
    turns: sampleTurns,
    hasOlder: false,
    oldestTurnId: null,
    loading: false,
    loadingOlder: false,
    sending: false,
    proposalBusy: false,
    error: null,
    draft: '',
    endedBy: null,
    streamState: 'connected',
  });

  const fixtureStore: ChatStore = {
    subscribe: inner.subscribe,
    connect: async () => {},
    disconnect: () => {},
    openNew: async () => fixtureChat,
    loadOlder: async () => {},
    loadToTurn: async () => true,
    setDraft: (draft) => inner.update((s) => ({ ...s, draft })),
    send: async () => {},
    retryTurn: async () => {},
    editFailedTurn: () => {},
    stopReply: async () => {},
    acceptProposal: async () => {},
    rejectProposal: async () => {},
    end: async () => {},
  };

  const standaloneTurn: LocalChatTurn = {
    turn_id: 'dev-t-stopped',
    kind: 'message',
    author: 'planner',
    status: 'stopped',
    format: 'markdown',
    untrusted: false,
    model: 'sonnet',
    text: '',
    steps: [{ key: 'chat.step.read', args: { target: 'ChatTurn.svelte' } }],
    refs: [],
    at: '2026-11-27T09:13:00Z',
    reason: null,
    lines: [],
  };
</script>

<div class="chat-fixture">
  <div class="chat-fixture__column">
    <Chat {subject} scope="plan" store={fixtureStore} />
  </div>
  <div class="chat-fixture__turn">
    <ChatTurn turn={standaloneTurn} scope="plan" subject={fixtureChat.subject} />
  </div>
  <div class="chat-fixture__markdown">
    <ChatMarkdown text={'Markdown subset with **bold**, _italic_, and `code`.'} />
  </div>
</div>

<style>
  .chat-fixture {
    display: grid;
    gap: var(--space-3);
  }

  .chat-fixture__column {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    height: 420px;
    overflow: hidden;
  }

  .chat-fixture__turn,
  .chat-fixture__markdown {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    padding: var(--space-2);
  }
</style>
