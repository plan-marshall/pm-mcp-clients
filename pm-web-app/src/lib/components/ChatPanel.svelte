<script lang="ts">
  import { t } from '../i18n';
  import type { ChatProposal, ChatTurn } from '../api/client';

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
  /** The label of the accept button; a follow-up plan names its own act. */
  export let acceptLabel = '';
  /** The label of the reject button. */
  export let rejectLabel = '';

  let message = '';

  function send(): void {
    const text = message.trim();
    if (!text || busy) return;
    onSend(text);
    message = '';
  }
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
        on:click={onEnd}
      >
        {$t('web.common.close')}
      </button>
    </header>

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
            <button type="button" data-testid="chat-reject" disabled={busy} on:click={onReject}>
              {rejectLabel || $t('start.decomposition.discard')}
            </button>
            <button type="button" data-testid="chat-accept" disabled={busy} on:click={onAccept}>
              {acceptLabel || $t('start.decomposition.accept')}
            </button>
          </div>
        </section>
      {/if}

      <div class="chat-panel__compose">
        <input
          type="text"
          data-testid="chat-input"
          bind:value={message}
          disabled={busy}
          on:keydown={(event) => {
            if (event.key === 'Enter') send();
          }}
        />
        <button
          type="button"
          data-testid="chat-send"
          disabled={busy || !message.trim()}
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
