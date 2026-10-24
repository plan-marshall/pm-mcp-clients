<script lang="ts">
  import { onDestroy, onMount } from 'svelte';
  import { t } from '../i18n';
  import type { SourceKindOffer, StartSource } from '../api/client';
  import { failureMessage, offerReason, resetTime } from '../start/sourceLabels';

  export let sources: StartSource[] = [];
  export let offers: SourceKindOffer[] = [];
  export let canContinue = false;
  export let chatAvailable = false;
  export let chatDisabledReason = '';
  export let busy = false;
  export let onAdd: (kind: string) => void = () => {};
  export let onRemove: (sourceId: string) => void = () => {};
  export let onRetry: (sourceId: string) => void = () => {};
  export let onAccept: (sourceId: string, reason: string) => void = () => {};
  export let onChat: () => void = () => {};
  export let onContinue: () => void = () => {};

  let now = Date.now();
  let acceptSourceId = '';
  let acceptReason = '';
  let timer: ReturnType<typeof setInterval> | null = null;

  onMount(() => {
    timer = setInterval(() => (now = Date.now()), 30_000);
  });

  onDestroy(() => {
    if (timer) clearInterval(timer);
  });

  /** The sources that still owe a state before Continue may be offered. */
  $: waiting = sources.length === 0 ? 1 : sources.filter((source) => !settled(source)).length;

  function settled(source: StartSource): boolean {
    return (
      source.state === 'stored' ||
      source.state === 'copied' ||
      source.state === 'moved' ||
      source.state === 'flagged' ||
      source.state === 'unscreened'
    );
  }

  function stateText(source: StartSource): string {
    if ((source.state === 'failed' || source.state === 'unscreened') && source.failure) {
      const message = failureMessage(source.failure);
      return $t(message.key, message.args);
    }
    return $t(`start.state.${source.state}`);
  }

  function countdown(resetsAt?: number | null): string {
    if (!resetsAt) return '';
    const seconds = Math.max(0, resetsAt - Math.floor(now / 1000));
    const minutes = Math.floor(seconds / 60);
    return `${minutes}:${String(seconds % 60).padStart(2, '0')}`;
  }

  function beginAccept(sourceId: string): void {
    acceptSourceId = sourceId;
    acceptReason = '';
  }

  function confirmAccept(): void {
    if (!acceptSourceId || !acceptReason.trim()) return;
    onAccept(acceptSourceId, acceptReason.trim());
    acceptSourceId = '';
    acceptReason = '';
  }

  function changedFiles(source: StartSource): number {
    return (source.changed_files ?? []).filter((file) => file.untrusted).length;
  }

  function isUntrusted(source: StartSource): boolean {
    return (
      source.trust === 'untrusted' ||
      Boolean(source.disposition) ||
      (source.kind === 'pull_request' && changedFiles(source) > 0)
    );
  }
</script>

<section class="source-list" data-testid="source-list">
  <ul class="source-list__lines">
    {#each sources as source (source.source_id)}
      <li class="source-line" data-testid={`source-${source.source_id}`}>
        <span class="source-line__mark" aria-hidden="true">
          {#if source.state === 'failed'}✗{:else if source.state === 'flagged'}⚠{:else if source.state === 'unscreened'}?{:else if settled(source)}✓{:else}▶{/if}
        </span>
        <span class="source-line__kind">{$t(`start.source.${source.kind}`)}</span>
        <span class="source-line__reference">
          {source.title ?? source.reference}
        </span>
        <span class="source-line__state" data-testid={`state-${source.source_id}`}>
          {stateText(source)}
        </span>
        {#if source.failure?.cause === 'rate_limited'}
          <span
            class="source-line__rate"
            data-testid={`rate-limit-${source.source_id}`}
            data-remaining={countdown(source.failure.resets_at)}
          >
            {resetTime(source.failure.resets_at)}
          </span>
        {/if}
        {#if isUntrusted(source)}
          <span class="source-line__badge" data-testid={`untrusted-${source.source_id}`}>
            {$t('start.state.untrusted')}
          </span>
        {/if}

        <span class="source-line__actions">
          {#if source.state === 'failed' && source.failure?.cause !== 'no_local_changes'}
            <button
              type="button"
              data-testid={`retry-${source.source_id}`}
              disabled={busy}
              on:click={() => onRetry(source.source_id)}
            >
              {$t('start.source.retry')}
            </button>
          {/if}
          {#if source.state === 'flagged' || source.state === 'unscreened'}
            <button
              type="button"
              data-testid={`accept-${source.source_id}`}
              disabled={busy}
              on:click={() => beginAccept(source.source_id)}
            >
              {source.state === 'flagged'
                ? $t('start.issue.flagged.accept')
                : $t('start.unscreened.accept')}
            </button>
          {/if}
          <button
            type="button"
            data-testid={`remove-${source.source_id}`}
            disabled={busy}
            on:click={() => onRemove(source.source_id)}
          >
            {$t('start.source.remove_it')}
          </button>
        </span>

        {#if source.state === 'flagged' && (source.spans?.length ?? 0) > 0}
          <div class="source-line__flagged" data-testid={`flagged-${source.source_id}`}>
            <p class="source-line__flagged-title">{$t('start.issue.flagged.title')}</p>
            {#each source.spans ?? [] as span}
              <p class="source-line__span">
                <mark title={span.reason}>{span.text}</mark>
                <span class="source-line__span-reason">{span.reason}</span>
              </p>
            {/each}
          </div>
        {/if}

        {#if source.disposition === 'accept_untrusted' && source.disposition_reason}
          <p class="source-line__reason" data-testid={`disposition-${source.source_id}`}>
            {source.disposition_reason}
          </p>
        {/if}

        {#if source.kind === 'pull_request' && changedFiles(source) > 0}
          <div class="source-line__files" data-testid={`pr-files-${source.source_id}`}>
            <span>{$t('start.pr.files_untrusted', { count: changedFiles(source) })}</span>
            <ul>
              {#each source.changed_files ?? [] as file}
                <li class:untrusted={file.untrusted}>{file.path}</li>
              {/each}
            </ul>
          </div>
        {/if}

        {#if acceptSourceId === source.source_id}
          <div class="source-line__accept" data-testid={`accept-form-${source.source_id}`}>
            <label>
              <span>{$t('start.issue.flagged.title')}</span>
              <input
                type="text"
                data-testid={`accept-reason-${source.source_id}`}
                bind:value={acceptReason}
              />
            </label>
            <button
              type="button"
              data-testid={`accept-confirm-${source.source_id}`}
              disabled={!acceptReason.trim()}
              on:click={confirmAccept}
            >
              {$t('start.decomposition.accept')}
            </button>
          </div>
        {/if}
      </li>
    {/each}
  </ul>

  <div class="source-list__add" data-testid="source-add">
    {#each offers as offer (offer.kind)}
      <span class="source-list__offer">
        <button
          type="button"
          data-testid={`add-${offer.kind}`}
          disabled={!offer.available || busy}
          title={offer.available ? undefined : offerReason(offer, $t)}
          on:click={() => onAdd(offer.kind)}
        >
          {$t('start.source.add', { kind: $t(`start.source.${offer.kind}`) })}
        </button>
        {#if !offer.available}
          <span class="source-list__offer-reason" data-testid={`offer-${offer.kind}-reason`}>
            {offerReason(offer, $t)}
          </span>
        {/if}
      </span>
    {/each}
  </div>

  <div class="source-list__footer">
    <button
      type="button"
      data-testid="source-chat"
      disabled={!chatAvailable || busy}
      title={chatAvailable ? undefined : $t(chatDisabledReason || 'start.chat.waits')}
      on:click={onChat}
    >
      {$t('start.sources.chat')}
    </button>
    <button
      type="button"
      data-testid="source-continue"
      disabled={!canContinue || busy}
      on:click={onContinue}
    >
      {$t('start.sources.continue')}
    </button>
    {#if !canContinue}
      <span class="source-list__waits" data-testid="source-waits">
        {$t('start.sources.waits', { count: waiting })}
      </span>
    {/if}
  </div>
</section>

<style>
  .source-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-3, 0.75rem);
  }

  .source-list__lines {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
  }

  .source-line {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-1) var(--space-2);
    padding: var(--space-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
  }

  .source-line__mark {
    width: 1.25rem;
    text-align: center;
    font-weight: 700;
  }

  .source-line__kind {
    font-weight: 600;
  }

  .source-line__reference {
    overflow-wrap: anywhere;
    flex: 1 1 12rem;
  }

  .source-line__state {
    color: var(--color-text-muted);
  }

  .source-line__rate {
    font-variant-numeric: tabular-nums;
    color: var(--color-warning-text);
  }

  .source-line__badge {
    padding: 1px 6px;
    border-radius: var(--radius-sm);
    background-color: var(--color-warning-subtle);
    color: var(--color-warning-text);
    font-size: var(--text-xs);
  }

  .source-line__actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-1);
  }

  .source-line__actions button,
  .source-list__footer button,
  .source-list__add button,
  .source-line__accept button {
    min-height: 44px;
    padding: var(--space-1) var(--space-3);
    cursor: pointer;
  }

  .source-line__flagged,
  .source-line__files,
  .source-line__accept {
    flex-basis: 100%;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .source-line__flagged-title {
    margin: 0;
    font-weight: 600;
  }

  .source-line__span,
  .source-line__reason,
  .source-list__waits,
  .source-list__offer-reason {
    margin: 0;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .source-line__span mark {
    background-color: var(--color-warning-subtle);
    color: inherit;
  }

  .source-line__span-reason {
    margin-left: var(--space-1);
  }

  .source-line__files ul {
    margin: 0;
    padding-left: var(--space-4);
  }

  .source-line__files li.untrusted {
    color: var(--color-warning-text);
  }

  .source-list__add,
  .source-list__footer {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2, 0.5rem);
  }

  .source-list__offer {
    display: inline-flex;
    flex-direction: column;
    gap: 2px;
  }

  @media (max-width: 640px) {
    .source-line {
      align-items: flex-start;
    }

    .source-line__reference {
      flex-basis: 100%;
    }
  }
</style>
