<script lang="ts">
  import { onMount, tick } from 'svelte';
  import { t } from '../i18n';
  import type { ChatProposal, ChatTurn } from '../api/client';
  import { uniqueId } from '../utils/uniqueId';

  export let open = false;
  export let role = '';
  export let subject = '';
  export let turns: ChatTurn[] = [];
  export let proposal: ChatProposal | null = null;
  export let available = true;
  export let disabledReason = '';
  export let busy = false;
  export let onSend: (text: string) => void = () => {};
  export let onEnd: () => void = () => {};
  export let onAccept: () => void = () => {};
  export let onReject: () => void = () => {};
  export let onToggle: () => void = () => {};
  /** Closes the panel without ending the chat; the host decides when it is gone. */
  export let onClose: () => void = () => {};
  /** The label of the accept button; a follow-up plan names its own act. */
  export let acceptLabel = '';
  /** The label of the reject button. */
  export let rejectLabel = '';

  let message = '';

  /** The id of the element that carries the reason a control cannot act. */
  const reasonId = uniqueId('chat-reason');

  /** The control the focus goes back to when the panel closes. */
  let opener: HTMLElement | null = null;
  let wasOpen = false;

  /** Whether a control that can act is held back, and why. */
  $: reason = busy ? $t('project.chat.waiting') : '';
  $: describedBy = reason ? reasonId : undefined;

  // The panel is removed from the document by some hosts and only hidden by others, so the opener
  // is remembered on the transition the `open` flag makes, not on mount alone.
  $: if (open && !wasOpen) {
    wasOpen = true;
    const active = typeof document === 'undefined' ? null : document.activeElement;
    opener = active instanceof HTMLElement ? active : null;
  } else if (!open && wasOpen) {
    wasOpen = false;
  }

  function send(): void {
    const text = message.trim();
    if (!text || busy) return;
    onSend(text);
    message = '';
  }

  /** Closes the panel and gives the focus back to the control that opened it. */
  async function close(): Promise<void> {
    const previous = opener;
    opener = null;
    onClose();
    await tick();
    if (previous?.isConnected) {
      previous.focus();
    }
  }

  /** @param event the key press anywhere while the panel is open */
  function closeOnEscape(event: KeyboardEvent): void {
    if (event.key !== 'Escape' || !open) {
      return;
    }
    event.stopPropagation();
    void close();
  }

  // The listener sits on the window: the control that opened the panel keeps the focus, so a key
  // press is delivered to it and never to the panel's own subtree.
  onMount(() => {
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  });
</script>

{#if open}
  <aside class="chat-panel" data-testid="chat-panel" aria-label={subject}>
    <header class="chat-panel__head">
      <div class="chat-panel__subject">
        <span class="chat-panel__subject-text">{subject}</span>
        <span class="chat-panel__role">{role}</span>
      </div>
      <button
        type="button"
        class="chat-panel__toggle"
        data-testid="chat-end"
        disabled={busy}
        title={reason}
        aria-describedby={describedBy}
        on:click={onEnd}
      >
        {$t('web.common.close')}
      </button>
    </header>

    {#if reason}
      <span class="sr-only" id={reasonId}>{reason}</span>
    {/if}

    {#if !available}
      <p class="chat-panel__disabled" data-testid="chat-disabled">
        {$t(disabledReason || 'start.chat.waits')}
      </p>
    {:else}
      <ol class="chat-panel__turns" data-testid="chat-transcript">
        {#each turns as turn}
          <li class="chat-panel__turn" data-testid="chat-turn" class:untrusted={turn.untrusted}>
            <span class="chat-panel__turn-role">{turn.role}</span>
            <span class="chat-panel__turn-text" data-model-text>{turn.text}</span>
          </li>
        {/each}
      </ol>

      {#if proposal}
        <section class="chat-panel__proposal" data-testid="chat-proposal">
          <h3 class="chat-panel__proposal-title">
            {$t('start.chat.proposed', { change: 'addition' })}
          </h3>
          {#if proposal.before}
            <p class="chat-panel__before" data-model-text>{proposal.before}</p>
          {/if}
          <p class="chat-panel__after" data-testid="chat-proposal-after" data-model-text>
            {proposal.after}
          </p>
          <div class="chat-panel__proposal-actions">
            <button
              type="button"
              data-testid="chat-reject"
              disabled={busy}
              title={reason}
              aria-describedby={describedBy}
              on:click={onReject}
            >
              {rejectLabel || $t('start.decomposition.discard')}
            </button>
            <button
              type="button"
              data-testid="chat-accept"
              disabled={busy}
              title={reason}
              aria-describedby={describedBy}
              on:click={onAccept}
            >
              {acceptLabel || $t('start.decomposition.accept')}
            </button>
          </div>
        </section>
      {/if}

      <div class="chat-panel__compose">
        <input
          type="text"
          data-testid="chat-input"
          aria-label={$t('start.chat.field')}
          bind:value={message}
          disabled={busy}
          on:keydown={(event) => {
            if (event.key === 'Enter') send();
          }}
        />
        <button
          type="button"
          data-testid="chat-send"
          disabled={busy}
          title={reason}
          aria-describedby={describedBy}
          on:click={send}
        >
          {$t('start.sources.continue')}
        </button>
      </div>
    {/if}
  </aside>
{:else}
  <button type="button" class="chat-panel__open" data-testid="chat-open" on:click={onToggle}>
    {$t('start.sources.chat')}
  </button>
{/if}

<style>
  .chat-panel {
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
    min-width: 0;
    padding: var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    max-height: 32rem;
  }

  .chat-panel__head {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);
  }

  .chat-panel__subject {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  .chat-panel__subject-text {
    font-weight: 600;
    overflow-wrap: anywhere;
  }

  .chat-panel__role {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .chat-panel__turns {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    overflow-y: auto;
    min-height: 6rem;
  }

  .chat-panel__turn {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: var(--space-2);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
  }

  .chat-panel__turn.untrusted {
    border-left: 3px solid var(--color-warning-border);
  }

  .chat-panel__turn-role {
    font-weight: 600;
    font-size: var(--text-sm);
  }

  .chat-panel__turn-text {
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }

  .chat-panel__proposal {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding: var(--space-2);
    border: 1px solid var(--color-primary);
    border-radius: var(--radius-md);
  }

  .chat-panel__proposal-title {
    margin: 0;
    font-size: var(--text-base);
  }

  .chat-panel__before {
    margin: 0;
    text-decoration: line-through;
    color: var(--color-text-muted);
  }

  .chat-panel__after {
    margin: 0;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }

  .chat-panel__proposal-actions,
  .chat-panel__compose {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .chat-panel__compose input {
    flex: 1 1 10rem;
    min-height: 44px;
    padding: var(--space-1);
  }

  .chat-panel__disabled {
    margin: 0;
    color: var(--color-text-muted);
  }

  .chat-panel button {
    min-height: 44px;
    padding: var(--space-1) var(--space-3);
    cursor: pointer;
  }

  .chat-panel__open {
    align-self: flex-start;
  }

  @media (max-width: 768px) {
    .chat-panel {
      max-height: none;
    }
  }
</style>
