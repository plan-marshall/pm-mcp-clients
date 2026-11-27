<script lang="ts">
  import { onDestroy, onMount, untrack, type Snippet } from 'svelte';
  import { translateChatMessage } from '../chat/messages';
  import { createChatStore, type ChatStore, type ChatStoreState } from '../chat/store';
  import type { ChatScopeKind, ChatSubject, ChatTarget, MessageText, TurnRef } from '../chat/types';
  import { formatTime } from '../display/format';
  import { currentLocale, t } from '../i18n';
  import Button from './Button.svelte';
  import ChatTranscript from './ChatTranscript.svelte';
  import Compose from './Compose.svelte';
  import ConfirmDialog from './ConfirmDialog.svelte';
  import KindMark, { type Kind } from './KindMark.svelte';

  export interface ChatProps {
    subject: ChatTarget & { title?: string };
    scope?: ChatScopeKind;
    archive?: boolean;
    turnId?: string | null;
    opener?: HTMLElement | null;
    localDevice?: string | null;
    /** Optional reference candidates known for the subject (tasks, files, decisions, plans). */
    references?: TurnRef[];
    onhide?: () => void;
    onnew?: () => void;
    microphone?: Snippet;
    /** Optional externally managed store (used by stories and tests). */
    store?: ChatStore;
  }

  let {
    subject,
    scope = 'plan',
    archive = false,
    turnId = null,
    opener = null,
    localDevice = null,
    references = [],
    onhide,
    onnew,
    microphone,
    store: injectedStore,
  }: ChatProps = $props();

  // Store instance is bound once per component lifecycle for the subject target.
  const ownedStore = untrack(() => (!injectedStore ? createChatStore(subject, { archive }) : null));
  const activeStore: ChatStore = untrack(() => injectedStore ?? ownedStore!);

  let chatState = $state<ChatStoreState>(
    untrack(() => ({
      target: subject,
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
      draft: '',
      endedBy: null,
      streamState: 'connecting',
    })),
  );

  const unsubscribe = activeStore.subscribe((value) => {
    chatState = value;
  });

  let composeRef: { focus: () => void } | undefined = $state();
  let confirmNewOpen = $state(false);
  let dismissedRefToken = $state<string | null>(null);
  let previousCompletedId = '';
  let announcement = $state('');

  const MAX_LENGTH = 8000;
  const COUNTER_THRESHOLD = 7000;

  function kindMarkOf(kind: ChatTarget['kind']): Kind {
    switch (kind) {
      case 'epic':
        return 'epic';
      case 'decision':
        return 'decision';
      case 'plan':
      case 'entry':
        return 'plan';
    }
  }

  function toChatSubject(target: ChatTarget): ChatSubject | null {
    switch (target.kind) {
      case 'plan':
      case 'epic':
      case 'entry':
      case 'decision':
        return { kind: target.kind, id: target.id };
    }
  }

  function starterIndices(currentScope: ChatScopeKind): number[] {
    if (currentScope === 'plan' || currentScope === 'epic') {
      return [1, 2, 3];
    }
    return [1, 2];
  }

  function starterScopeKey(currentScope: ChatScopeKind): string {
    switch (currentScope) {
      case 'plan':
      case 'epic':
      case 'decision':
      case 'entry':
      case 'archive':
      case 'draft':
      case 'item':
      case 'register':
        return `control.chat.starter.${currentScope}`;
    }
  }

  function formatKnowsItem(msg: MessageText): string {
    const item = msg.key.startsWith('chat.knows.') ? msg.key.slice('chat.knows.'.length) : msg.key;
    const rawArgs: Record<string, string | number> = { item };
    if (msg.args) {
      for (const [k, v] of Object.entries(msg.args)) {
        rawArgs[k] = typeof v === 'boolean' ? String(v) : v;
      }
    }
    return $t('control.chat.knows.item', rawArgs);
  }

  function detectPlatform(): 'mac' | 'windows' | 'other' {
    if (typeof navigator === 'undefined') return 'other';
    const platform = `${navigator.platform ?? ''} ${navigator.userAgent ?? ''}`.toLowerCase();
    if (platform.includes('mac')) return 'mac';
    if (platform.includes('win')) return 'windows';
    return 'other';
  }

  let effectiveScope = $derived(chatState.chat?.scope ?? scope);
  let roleName = $derived(chatState.chat?.role.name ?? '');
  let roleModel = $derived(chatState.chat?.role.model ?? '');
  let subjectTitle = $derived(subject.title ?? subject.id);
  let chatSubject = $derived(chatState.chat?.subject ?? toChatSubject(subject));

  let streamingTurn = $derived(
    [...chatState.turns]
      .reverse()
      .find((turn) => turn.author !== 'operator' && turn.status === 'streaming') ?? null,
  );

  let knowsSummary = $derived.by(() => {
    const context = chatState.chat?.context;
    if (!context || context.knows.length === 0 || !roleName) return '';
    const items = context.knows.map(formatKnowsItem).join(', ');
    const time = formatTime(context.as_of, 'clock', $currentLocale) || context.as_of;
    return $t('control.chat.knows', { role: roleName, items, time });
  });

  let starters = $derived.by(() => {
    const key = starterScopeKey(effectiveScope);
    return starterIndices(effectiveScope).map((n) => $t(key, { n }));
  });

  let availableRefs = $derived.by(() => {
    const seen = new Set<string>();
    const list: Array<TurnRef & { id: string }> = [];
    const pushRef = (ref: TurnRef) => {
      if (!ref.id) return;
      const key = `${ref.kind}:${ref.id}`;
      if (seen.has(key)) return;
      seen.add(key);
      list.push({ ...ref, id: ref.id });
    };
    for (const ref of references) {
      pushRef(ref);
    }
    for (const knowsItem of chatState.chat?.context?.knows ?? []) {
      const idArg = knowsItem.args?.id;
      const kindArg = knowsItem.args?.item;
      if (typeof idArg === 'string' && idArg && kindArg === 'decision') {
        pushRef({ kind: 'decision', id: idArg, subject: chatSubject });
      }
    }
    for (const turn of chatState.turns) {
      for (const ref of turn.refs) {
        pushRef(ref);
      }
    }
    return list;
  });

  let hashMatch = $derived(/(?:^|\s)(#[^\s]*)$/.exec(chatState.draft));
  let hashToken = $derived(hashMatch ? hashMatch[1] : null);
  let refMenuOpen = $derived(hashToken !== null && hashToken !== dismissedRefToken);
  let refFilterQuery = $derived(hashToken ? hashToken.slice(1).toLowerCase() : '');
  let filteredRefs = $derived(
    availableRefs.filter((ref) => ref.id.toLowerCase().includes(refFilterQuery)),
  );

  let tooLong = $derived(chatState.draft.length > MAX_LENGTH);
  let showCounter = $derived(chatState.draft.length >= COUNTER_THRESHOLD);

  function focusComposer(): void {
    composeRef?.focus();
  }

  function hideChat(): void {
    onhide?.();
    opener?.focus();
  }

  function handleChatKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Escape') return;
    if (confirmNewOpen) return;
    if (refMenuOpen && hashToken) {
      event.preventDefault();
      event.stopPropagation();
      dismissedRefToken = hashToken;
      return;
    }
    event.preventDefault();
    hideChat();
  }

  function applyStarter(starter: string): void {
    dismissedRefToken = null;
    activeStore.setDraft(starter);
    focusComposer();
  }

  function insertReference(refId: string): void {
    if (!hashToken) return;
    const at = chatState.draft.lastIndexOf(hashToken);
    if (at < 0) return;
    const next = `${chatState.draft.slice(0, at)}${refId} `;
    dismissedRefToken = null;
    activeStore.setDraft(next);
    focusComposer();
  }

  async function handleSend(text: string): Promise<void> {
    if (tooLong || chatState.sending || !text.trim()) return;
    dismissedRefToken = null;
    await activeStore.send(text);
  }

  function handleStop(): void {
    void activeStore.stopReply(streamingTurn?.turn_id);
    focusComposer();
  }

  function requestNewChat(): void {
    if (chatState.chat?.proposal?.state === 'open') {
      confirmNewOpen = true;
      return;
    }
    void confirmNewChat();
  }

  async function confirmNewChat(): Promise<void> {
    confirmNewOpen = false;
    await activeStore.openNew();
    onnew?.();
    focusComposer();
  }

  $effect(() => {
    if (hashToken === null) {
      dismissedRefToken = null;
    }
  });

  $effect(() => {
    const completed = [...chatState.turns]
      .reverse()
      .find((turn) => turn.author !== 'operator' && turn.status !== 'streaming');
    if (completed && completed.turn_id !== previousCompletedId) {
      previousCompletedId = completed.turn_id;
      if (completed.status === 'stopped' && !completed.text) {
        announcement = $t('control.chat.stopped');
      } else if (completed.kind === 'notice' && !completed.text && completed.lines?.length) {
        const first = completed.lines[0];
        announcement = translateChatMessage($t, { key: first.key, args: first.args });
      } else {
        announcement = completed.text;
      }
    }
  });

  onMount(() => {
    if (ownedStore) {
      void ownedStore.connect({ turnId });
    } else if (turnId) {
      void activeStore.loadToTurn(turnId);
    }
    focusComposer();
  });

  onDestroy(() => {
    unsubscribe();
    ownedStore?.disconnect();
  });
</script>

<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<section
  class="chat"
  data-testid="chat"
  aria-label={$t('control.chat.title', { subject: subjectTitle, role: roleName || 'planner' })}
  tabindex="-1"
  onkeydown={handleChatKeydown}
>
  <header class="chat__head">
    <div class="chat__head-top">
      <div class="chat__title-group">
        <KindMark kind={kindMarkOf(subject.kind)} wordHidden />
        <h2 class="chat__title">{subjectTitle}</h2>
      </div>
      <div class="chat__head-actions">
        <Button variant="ghost" size="sm" onclick={hideChat}>
          {$t('control.chat.hide')}
        </Button>
        {#if !archive}
          <Button variant="secondary" size="sm" onclick={requestNewChat}>
            {$t('control.chat.new')}
          </Button>
        {/if}
      </div>
    </div>

    <div class="chat__subhead">
      {#if roleName}
        <span class="chat__role">
          {$t('web.chat.with', { role: roleName, model: roleModel })}
        </span>
      {/if}
      <span class="chat__scope">
        {$t('control.chat.scope', { scope: effectiveScope, item: subjectTitle })}
      </span>
      {#if archive}
        <span class="chat__archive">{$t('control.chat.read_only')}</span>
      {/if}
      {#if streamingTurn && roleName}
        <span class="chat__running">
          {$t('control.chat.answers', { role: roleName })}
        </span>
      {/if}
    </div>

    {#if knowsSummary}
      <p class="chat__knows">{knowsSummary}</p>
    {/if}
    {#if !archive && chatState.chat && chatState.streamState === 'offline'}
      <div class="chat__stream-offline" role="status">
        <span>{$t('web.frame.live.offline')}</span>
        <Button variant="ghost" size="sm" onclick={() => activeStore.retryStream()}>
          {$t('web.frame.live.retry')}
        </Button>
      </div>
    {/if}
  </header>

  <div class="chat__body">
    {#if chatState.error && !chatState.loading && !chatState.chat && chatState.turns.length === 0}
      <div class="chat__error" role="alert">
        <p class="chat__error-line">{$t('web.chat.load_failed')}</p>
        <Button variant="secondary" size="sm" onclick={() => void activeStore.connect({ turnId })}>
          {$t('common.action.retry')}
        </Button>
      </div>
    {:else if chatState.turns.length === 0 && !chatState.loading}
      <div class="chat__empty">
        <p class="chat__empty-line">
          {$t('control.chat.empty', {
            role: roleName || 'planner',
            scope: effectiveScope,
            item: subjectTitle,
          })}
        </p>
        <div class="chat__starters">
          {#each starters as starter (starter)}
            <Button variant="secondary" size="sm" onclick={() => applyStarter(starter)}>
              {starter}
            </Button>
          {/each}
        </div>
      </div>
    {:else}
      <ChatTranscript
        turns={chatState.turns}
        scope={effectiveScope}
        subject={chatSubject}
        subjectLabel={subjectTitle}
        {localDevice}
        hasOlder={chatState.hasOlder}
        loadingOlder={chatState.loadingOlder}
        {announcement}
        focusTurnId={turnId}
        onloadolder={() => activeStore.loadOlder()}
        onretry={(clientTurnId) => activeStore.retryTurn(clientTurnId)}
        onedit={(clientTurnId) => {
          activeStore.editFailedTurn(clientTurnId);
          focusComposer();
        }}
      />
    {/if}
  </div>

  <div class="chat__composer">
    {#if refMenuOpen}
      <div class="chat__refs" role="listbox" aria-label={$t('control.chat.refs.label')}>
        <div class="chat__refs-head">{$t('control.chat.refs.label')}</div>
        {#if filteredRefs.length === 0}
          <div class="chat__refs-none">{$t('control.chat.refs.none')}</div>
        {:else}
          {#each filteredRefs as ref (ref.kind + ':' + ref.id)}
            <button
              type="button"
              role="option"
              aria-selected="false"
              class="chat__ref-option"
              onclick={() => insertReference(ref.id)}
            >
              {ref.id}
            </button>
          {/each}
        {/if}
      </div>
    {/if}

    {#snippet chatFooter()}
      <div class="chat__composer-footer">
        <span class="chat__voice-hint">
          {$t('web.voice.os_hint', { platform: detectPlatform() })}
        </span>
        {#if showCounter}
          <span class="chat__counter" class:chat__counter--over={tooLong}>
            {$t('control.chat.counter', { count: chatState.draft.length })}
          </span>
        {/if}
      </div>
    {/snippet}

    <Compose
      bind:this={composeRef}
      bind:value={() => chatState.draft, (next) => activeStore.setDraft(next)}
      ariaLabel={$t('control.chat.message')}
      sendLabel={$t('control.chat.send')}
      placeholder={$t('control.chat.example', { scope: effectiveScope })}
      disabled={tooLong || chatState.sending}
      disabledReason={tooLong ? $t('control.chat.too_long') : undefined}
      stop={streamingTurn ? { label: $t('control.chat.stop'), onstop: handleStop } : null}
      onsend={(text) => void handleSend(text)}
      tools={microphone}
      footer={chatFooter}
    />
  </div>

  <ConfirmDialog
    open={confirmNewOpen}
    title={$t('control.chat.new_confirm.title')}
    consequences={[
      $t('control.chat.new_confirm.body', {
        kind: chatState.chat?.proposal
          ? $t('control.chat.proposal.kind', { kind: chatState.chat.proposal.kind })
          : '',
        subject: subjectTitle,
      }),
    ]}
    actionLabel={$t('control.chat.new')}
    cancelLabel={$t('common.action.cancel')}
    onConfirm={() => void confirmNewChat()}
    onClose={() => {
      confirmNewOpen = false;
    }}
  />
</section>

<style>
  .chat {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .chat__head {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding: var(--space-3);
    border-bottom: 1px solid var(--color-border);
  }

  .chat__head-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);
  }

  .chat__title-group {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    min-width: 0;
  }

  .chat__title {
    margin: 0;
    font-size: var(--text-base);
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .chat__head-actions {
    display: flex;
    align-items: center;
    gap: var(--space-1);
    flex-shrink: 0;
  }

  .chat__subhead {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .chat__archive {
    color: var(--color-text-secondary);
  }

  .chat__running {
    color: var(--color-text-secondary);
  }

  .chat__knows {
    margin: 0;
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .chat__stream-offline {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-danger-text);
  }

  .chat__body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }

  .chat__error {
    padding: var(--space-4) var(--space-3);
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }

  .chat__error-line {
    margin: 0;
    color: var(--color-danger-text);
    font-size: var(--text-base);
  }

  .chat__empty {
    padding: var(--space-4) var(--space-3);
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .chat__empty-line {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-base);
  }

  .chat__starters {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .chat__composer {
    position: relative;
    padding: var(--space-2) var(--space-3) var(--space-3);
    border-top: 1px solid var(--color-border);
  }

  .chat__refs {
    display: flex;
    flex-direction: column;
    gap: 2px;
    margin-bottom: var(--space-2);
    padding: var(--space-1);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
  }

  .chat__refs-head {
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .chat__refs-none {
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .chat__ref-option {
    display: flex;
    width: 100%;
    text-align: left;
    padding: var(--space-1) var(--space-2);
    border: 0;
    border-radius: var(--radius-sm);
    background: transparent;
    color: var(--color-text-primary);
    font-family: var(--font-mono);
    font-size: var(--text-sm);
    cursor: pointer;
  }

  .chat__ref-option:hover,
  .chat__ref-option:focus-visible {
    background-color: var(--color-bg-subtle);
  }

  .chat__composer-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);
    width: 100%;
  }

  .chat__voice-hint {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .chat__counter {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    font-variant-numeric: tabular-nums;
  }

  .chat__counter--over {
    color: var(--color-danger-text);
  }
</style>
