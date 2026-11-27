<script lang="ts">
  import { tick } from 'svelte';
  import type { ChatScopeKind, ChatSubject, LocalChatTurn } from '../chat/types';
  import { dayOf, formatDay } from '../display/format';
  import { currentLocale, t } from '../i18n';
  import Button from './Button.svelte';
  import ChatTurn from './ChatTurn.svelte';

  export interface ChatTranscriptProps {
    turns: LocalChatTurn[];
    scope?: ChatScopeKind;
    subject?: ChatSubject | null;
    subjectLabel?: string;
    localDevice?: string | null;
    hasOlder?: boolean;
    loadingOlder?: boolean;
    announcement?: string;
    focusTurnId?: string | null;
    onloadolder?: () => Promise<void> | void;
    onretry?: (clientTurnId: string) => void;
    onedit?: (clientTurnId: string) => void;
  }

  let {
    turns,
    scope = 'plan',
    subject = null,
    subjectLabel = '',
    localDevice = null,
    hasOlder = false,
    loadingOlder = false,
    announcement = '',
    focusTurnId = null,
    onloadolder,
    onretry,
    onedit,
  }: ChatTranscriptProps = $props();

  let logEl: HTMLElement | undefined = $state();
  let pinnedToBottom = $state(true);
  let hasUnreadReply = $state(false);
  let focusMissing = $state(false);
  let topLoading = false;
  let previousTurnCount = 0;
  let previousFirstId: string | null = null;
  let previousLastText = '';
  let scrolledFocusTurnId = '';

  let isBusy = $derived(
    turns.some((turn) => turn.author !== 'operator' && turn.status === 'streaming'),
  );

  function dayKeyOf(iso: string): string {
    const day = dayOf(iso);
    if (!day) return '';
    return `${day.getFullYear()}-${day.getMonth() + 1}-${day.getDate()}`;
  }

  function sameGroup(prev: LocalChatTurn | undefined, curr: LocalChatTurn): boolean {
    if (!prev) return false;
    if (prev.kind === 'notice' || curr.kind === 'notice') return false;
    if (curr.delivery === 'not_sent' || prev.delivery === 'not_sent') return false;
    if (dayKeyOf(prev.at) !== dayKeyOf(curr.at)) return false;
    return prev.author === curr.author && (prev.model ?? null) === (curr.model ?? null);
  }

  function handleScroll(): void {
    if (!logEl) return;
    const distanceFromBottom = logEl.scrollHeight - logEl.scrollTop - logEl.clientHeight;
    pinnedToBottom = distanceFromBottom <= 24;
    if (pinnedToBottom) {
      hasUnreadReply = false;
    }
    if (logEl.scrollTop <= 16 && hasOlder && !loadingOlder && !topLoading && onloadolder) {
      void loadOlderPreservingAnchor();
    }
  }

  function jumpToBottom(): void {
    if (!logEl) return;
    logEl.scrollTop = logEl.scrollHeight;
    pinnedToBottom = true;
    hasUnreadReply = false;
  }

  function findTurnElement(container: HTMLElement, targetTurnId: string): HTMLElement | null {
    const nodes = container.querySelectorAll('[data-turn-id]');
    for (const node of nodes) {
      if (node instanceof HTMLElement && node.getAttribute('data-turn-id') === targetTurnId) {
        return node;
      }
    }
    return null;
  }

  async function loadOlderPreservingAnchor(): Promise<void> {
    if (!logEl || !onloadolder || topLoading) return;
    topLoading = true;
    const beforeHeight = logEl.scrollHeight;
    const beforeTop = logEl.scrollTop;
    const anchorEl = logEl.querySelector('[data-turn-id]');
    const anchorId = anchorEl instanceof HTMLElement ? anchorEl.getAttribute('data-turn-id') : null;
    const beforeAnchorTop = anchorEl instanceof HTMLElement ? anchorEl.offsetTop : 0;
    try {
      await onloadolder();
      await tick();
      if (!logEl) return;
      if (anchorId) {
        const afterAnchorEl = findTurnElement(logEl, anchorId);
        if (afterAnchorEl) {
          const offsetDelta = afterAnchorEl.offsetTop - beforeAnchorTop;
          if (offsetDelta > 0) {
            logEl.scrollTop = beforeTop + offsetDelta;
            return;
          }
        }
      }
      logEl.scrollTop = beforeTop + (logEl.scrollHeight - beforeHeight);
    } finally {
      topLoading = false;
    }
  }

  $effect(() => {
    const count = turns.length;
    const firstId = turns[0]?.turn_id ?? null;
    const lastText = turns[turns.length - 1]?.text ?? '';
    const prepended =
      firstId !== null &&
      previousFirstId !== null &&
      firstId !== previousFirstId &&
      count > previousTurnCount;
    const appendedOrUpdated =
      !prepended && (count > previousTurnCount || lastText !== previousLastText);

    previousTurnCount = count;
    previousFirstId = firstId;
    previousLastText = lastText;

    if (
      appendedOrUpdated &&
      (!focusTurnId || scrolledFocusTurnId === focusTurnId || focusMissing)
    ) {
      if (pinnedToBottom) {
        void tick().then(() => {
          if (logEl) {
            logEl.scrollTop = logEl.scrollHeight;
          }
        });
      } else {
        hasUnreadReply = true;
      }
    }
  });

  $effect(() => {
    // Track turns, hasOlder, and loadingOlder reactively so delayed/paged focus resolution re-runs.
    const count = turns.length;
    const morePages = hasOlder;
    const paging = loadingOlder;
    if (!focusTurnId) {
      scrolledFocusTurnId = '';
      focusMissing = false;
      return;
    }
    if (!logEl || focusTurnId === scrolledFocusTurnId || focusMissing) return;
    const match = findTurnElement(logEl, focusTurnId);
    if (match) {
      scrolledFocusTurnId = focusTurnId;
      focusMissing = false;
      pinnedToBottom = false;
      match.scrollIntoView?.({ block: 'nearest' });
    } else if (count > 0 && !morePages && !paging) {
      focusMissing = true;
      jumpToBottom();
    }
  });
</script>

<div class="chat-transcript">
  <div class="sr-only" data-testid="chat-announcement" aria-live="polite" aria-atomic="true">
    {announcement}
  </div>

  {#if hasOlder}
    <div class="chat-transcript__older">
      <Button
        variant="ghost"
        size="sm"
        disabled={loadingOlder}
        onclick={() => void loadOlderPreservingAnchor()}
      >
        {loadingOlder ? '…' : $t('control.chat.pinned.show')}
      </Button>
    </div>
  {/if}

  <div
    bind:this={logEl}
    class="chat-transcript__log"
    role="log"
    aria-busy={isBusy ? 'true' : undefined}
    onscroll={handleScroll}
  >
    {#each turns as turn, idx (turn.turn_id)}
      {@const prev = idx > 0 ? turns[idx - 1] : undefined}
      {@const showDay = !prev || dayKeyOf(prev.at) !== dayKeyOf(turn.at)}
      {#if showDay}
        <div class="chat-transcript__day" data-testid="chat-day-divider">
          <span>{formatDay(turn.at, $currentLocale)}</span>
        </div>
      {/if}
      <ChatTurn
        {turn}
        grouped={sameGroup(prev, turn)}
        {scope}
        {subject}
        {subjectLabel}
        deviceName={turn.author === 'operator' && localDevice ? localDevice : null}
        {onretry}
        {onedit}
      />
    {/each}
  </div>

  {#if hasUnreadReply}
    <div class="chat-transcript__pill">
      <Button variant="secondary" size="sm" onclick={jumpToBottom}>
        {$t('web.chat.new_reply')}
      </Button>
    </div>
  {/if}
</div>

<style>
  .chat-transcript {
    position: relative;
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 0;
  }

  .chat-transcript__older {
    display: flex;
    justify-content: center;
    padding: var(--space-1) 0;
  }

  .chat-transcript__log {
    position: relative;
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-3);
  }

  .chat-transcript__day {
    display: flex;
    justify-content: center;
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    margin: var(--space-2) 0;
  }

  .chat-transcript__pill {
    position: sticky;
    bottom: var(--space-2);
    align-self: center;
    z-index: var(--layer-sticky);
  }

  .sr-only {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip-path: inset(50%);
    white-space: nowrap;
    border: 0;
  }
</style>
