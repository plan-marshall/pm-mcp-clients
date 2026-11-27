<script lang="ts">
  import { translateChatMessage } from '../chat/messages';
  import { hrefForTurnRef } from '../chat/markdown';
  import type { ChatScopeKind, ChatSubject, LocalChatTurn } from '../chat/types';
  import { formatTime } from '../display/format';
  import { currentLocale, t } from '../i18n';
  import Button from './Button.svelte';
  import ChatMarkdown from './ChatMarkdown.svelte';
  import Icon from './Icon.svelte';
  import Link from './Link.svelte';
  import WaitingMark from './WaitingMark.svelte';

  export interface ChatTurnProps {
    turn: LocalChatTurn;
    grouped?: boolean;
    scope?: ChatScopeKind;
    subject?: ChatSubject | null;
    subjectLabel?: string;
    deviceName?: string | null;
    onretry?: (clientTurnId: string) => void;
    onedit?: (clientTurnId: string) => void;
  }

  let {
    turn,
    grouped = false,
    scope = 'plan',
    subject = null,
    subjectLabel = '',
    deviceName = null,
    onretry,
    onedit,
  }: ChatTurnProps = $props();

  let stepsExpanded = $state(false);
  let linesExpanded = $state(false);

  let isOperator = $derived(turn.author === 'operator');
  let isNotice = $derived(turn.kind === 'notice');
  let isStreaming = $derived(turn.status === 'streaming' && !turn.delivery);
  let hasStepsToFold = $derived(turn.text.length > 0 && turn.steps.length > 0);
  let foldLines = $derived(isNotice && (turn.lines?.length ?? 0) > 3);

  let authorLabel = $derived.by(() => {
    if (!isOperator) return turn.author;
    if (deviceName) {
      return $t('web.chat.from_device', { device: deviceName });
    }
    return $t('control.chat.you');
  });

  let workingLabel = $derived(
    $t('control.chat.working', {
      role: turn.author,
      scope,
      item: subjectLabel || subject?.id || '',
    }),
  );

  let clockText = $derived(formatTime(turn.at, 'clock', $currentLocale));
  let fullTimeTitle = $derived(formatTime(turn.at, 'short', $currentLocale) || turn.at);

  function copyText(): void {
    void navigator.clipboard?.writeText(turn.text);
  }
</script>

<article
  class="chat-turn"
  class:chat-turn--operator={isOperator}
  class:chat-turn--role={!isOperator && !isNotice}
  class:chat-turn--notice={isNotice}
  class:chat-turn--grouped={grouped}
  class:chat-turn--untrusted={turn.untrusted}
  data-testid="chat-turn"
  data-turn-id={turn.turn_id}
>
  {#if isNotice}
    <div class="chat-turn__notice-body">
      {#if turn.text}
        <p class="chat-turn__plain">{turn.text}</p>
      {/if}
      {#if turn.lines && turn.lines.length > 0}
        {#if foldLines && !linesExpanded}
          <Button
            variant="ghost"
            size="sm"
            onclick={() => {
              linesExpanded = true;
            }}
          >
            <span class="chat-turn__fold-label">
              {$t('control.chat.lines.folded', {
                count: turn.lines.length,
                subject: subjectLabel || subject?.id || turn.author,
              })}
            </span>
          </Button>
        {:else}
          <ul class="chat-turn__notice-lines">
            {#each turn.lines as line, idx (idx)}
              <li class="chat-turn__notice-line">
                <span>
                  {translateChatMessage($t, { key: line.key, args: line.args })}
                </span>
                {#if line.refs && line.refs.length > 0}
                  <span class="chat-turn__line-refs">
                    {#each line.refs as ref, refIdx (refIdx)}
                      <Link
                        look="chip"
                        href={hrefForTurnRef(ref, subject)}
                        label={ref.id ?? ref.act ?? ref.kind}
                      />
                    {/each}
                  </span>
                {/if}
              </li>
            {/each}
          </ul>
        {/if}
      {/if}
    </div>
  {:else}
    {#if !grouped}
      <header class="chat-turn__author">
        {#if !isOperator}
          <span class="chat-turn__glyph" aria-hidden="true">
            {turn.author.slice(0, 1).toUpperCase()}
          </span>
        {/if}
        <span class="chat-turn__author-name">{authorLabel}</span>
        {#if !isOperator && turn.model}
          <span class="chat-turn__model">{turn.model}</span>
        {/if}
      </header>
    {/if}

    {#if turn.untrusted}
      <div class="chat-turn__untrusted" role="note">
        <Icon name="state.alert" size={16} />
        <span>{$t('control.chat.untrusted')}</span>
      </div>
    {/if}

    {#if isStreaming && !turn.text}
      <div class="chat-turn__working">
        <WaitingMark label={workingLabel} />
      </div>
    {/if}

    {#if !turn.text && turn.steps.length > 0}
      <ul class="chat-turn__steps">
        {#each turn.steps as step, idx (idx)}
          <li class="chat-turn__step">{translateChatMessage($t, step)}</li>
        {/each}
      </ul>
    {:else if hasStepsToFold}
      <div class="chat-turn__steps-folded">
        <Button
          variant="ghost"
          size="sm"
          onclick={() => {
            stepsExpanded = !stepsExpanded;
          }}
        >
          {$t('control.chat.steps', { count: turn.steps.length })}
        </Button>
        {#if stepsExpanded}
          <ul class="chat-turn__steps">
            {#each turn.steps as step, idx (idx)}
              <li class="chat-turn__step">{translateChatMessage($t, step)}</li>
            {/each}
          </ul>
        {/if}
      </div>
    {/if}

    {#if turn.text}
      <div class="chat-turn__content">
        {#if isOperator || turn.untrusted || turn.format !== 'markdown'}
          <p class="chat-turn__plain">{turn.text}</p>
        {:else}
          <ChatMarkdown text={turn.text} refs={turn.refs} {subject} streaming={isStreaming} />
        {/if}
        {#if isStreaming}
          <span class="chat-turn__caret" aria-hidden="true"></span>
        {/if}
      </div>
    {/if}

    {#if turn.delivery === 'sending'}
      <p class="chat-turn__status">{$t('control.chat.sending')}</p>
    {:else if turn.delivery === 'not_sent'}
      <div class="chat-turn__failed">
        <span class="chat-turn__failed-label">
          <Icon name="state.alert" size={16} />
          {$t('control.chat.not_sent')}
        </span>
        {#if turn.client_turn_id}
          <Button variant="secondary" size="sm" onclick={() => onretry?.(turn.client_turn_id!)}>
            {$t('common.action.retry')}
          </Button>
          <Button variant="ghost" size="sm" onclick={() => onedit?.(turn.client_turn_id!)}>
            {$t('control.chat.edit')}
          </Button>
        {/if}
      </div>
    {:else if turn.status === 'stopped'}
      <p class="chat-turn__status">{$t('control.chat.stopped')}</p>
    {:else if turn.status === 'failed' && turn.reason}
      <p class="chat-turn__status chat-turn__status--error">
        {translateChatMessage($t, turn.reason)}
      </p>
    {/if}

    <footer class="chat-turn__meta">
      <time
        class="chat-turn__time"
        datetime={turn.at}
        title={fullTimeTitle}
        aria-label={fullTimeTitle}
      >
        {clockText}
      </time>
      {#if !turn.delivery}
        <Button variant="ghost" size="sm" icon="act.copy" onclick={copyText}>
          {$t('web.common.copy')}
        </Button>
      {/if}
    </footer>
  {/if}
</article>

<style>
  .chat-turn {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    max-width: 100%;
    position: relative;
  }

  .chat-turn--operator {
    align-self: flex-end;
    max-width: 85%;
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-subtle);
  }

  .chat-turn--role {
    align-self: stretch;
    width: 100%;
    padding: var(--space-2) 0;
  }

  .chat-turn--notice {
    align-self: stretch;
    padding: var(--space-1) var(--space-2);
    border-left: 2px solid var(--color-border);
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .chat-turn--grouped {
    padding-top: var(--space-1);
  }

  .chat-turn__author {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .chat-turn__glyph {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border-radius: var(--radius-full);
    background-color: var(--color-bg-subtle);
    border: 1px solid var(--color-border);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    flex-shrink: 0;
  }

  .chat-turn__model {
    font-weight: 400;
    color: var(--color-text-muted);
    font-size: var(--text-xs);
  }

  .chat-turn__untrusted {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    align-self: flex-start;
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
  }

  .chat-turn__working {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .chat-turn__steps {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
    color: var(--color-text-muted);
    font-size: var(--text-xs);
  }

  .chat-turn__content {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .chat-turn__plain {
    margin: 0;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
    font-size: var(--text-base);
    line-height: var(--line-height-base);
  }

  .chat-turn__notice-lines {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .chat-turn__notice-line {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-1);
  }

  .chat-turn__line-refs {
    display: inline-flex;
    flex-wrap: wrap;
    gap: var(--space-1);
  }

  .chat-turn__fold-label {
    white-space: normal;
    text-align: left;
  }

  .chat-turn__caret {
    display: inline-block;
    width: 0.5em;
    height: 1em;
    vertical-align: text-bottom;
    background-color: var(--color-text-secondary);
    animation: chat-caret-blink 1s steps(2, start) infinite;
  }

  @keyframes chat-caret-blink {
    0%,
    100% {
      opacity: 1;
    }
    50% {
      opacity: 0.25;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .chat-turn__caret {
      animation: none;
      opacity: 1;
    }
  }

  .chat-turn__status {
    margin: 0;
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .chat-turn__status--error {
    color: var(--color-danger-text);
  }

  .chat-turn__failed {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-danger-text);
  }

  .chat-turn__failed-label {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
  }

  .chat-turn__meta {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    opacity: 0;
    transition: opacity var(--duration-fast) var(--ease-standard);
  }

  .chat-turn:hover .chat-turn__meta,
  .chat-turn:focus-within .chat-turn__meta {
    opacity: 1;
  }

  .chat-turn__time {
    font-variant-numeric: tabular-nums;
  }
</style>
