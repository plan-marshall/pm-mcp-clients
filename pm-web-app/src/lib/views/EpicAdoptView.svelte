<script lang="ts">
  import { t } from '../i18n';
  import type { AdoptionFailure, AdoptableEpic } from '../api/client';

  export let epics: AdoptableEpic[] = [];
  export let failure: AdoptionFailure | null = null;
  export let projectId = '';
  export let busy = false;
  export let onAdopt: (epicId: string) => void = () => {};
  export let onNew: () => void = () => {};
  export let onRetry: () => void = () => {};
  export let onBack: () => void = () => {};
  export let onAbandon: () => void = () => {};

  function reasonOf(problem: AdoptionFailure): string {
    return $t('start.epic.adopt.failed', {
      epic: problem.epic_id,
      reason: problem.reason,
      file: problem.file ?? '',
      detail: problem.detail ?? '',
    });
  }
</script>

<section class="epic-adopt" data-testid="epic-adopt">
  {#if failure}
    <h2 class="epic-adopt__title">{$t('start.epic.adopt.action', { epic: failure.epic_id })}</h2>
    <p class="epic-adopt__failure" data-testid="epic-adopt-failed">{reasonOf(failure)}</p>
    <div class="epic-adopt__actions">
      <button type="button" data-testid="adopt-retry" disabled={busy} on:click={onRetry}>
        {$t('start.source.retry')}
      </button>
      <button type="button" data-testid="adopt-back" disabled={busy} on:click={onBack}>
        {$t('web.common.back')}
      </button>
      <button type="button" data-testid="adopt-abandon" disabled={busy} on:click={onAbandon}>
        {$t('start.entry.abandon')}
      </button>
    </div>
  {:else}
    <h2 class="epic-adopt__title">{$t('start.sources.title', { kind: 'epic' })}</h2>
    <ul class="epic-adopt__list">
      {#each epics as epic (epic.epic_id)}
        <li class="epic-adopt__item" data-testid={`adoptable-${epic.epic_id}`}>
          <p class="epic-adopt__offer">
            {$t('start.epic.adopt.offer', { epic: epic.title, project: projectId })}
          </p>
          <button
            type="button"
            data-testid={`adopt-${epic.epic_id}`}
            disabled={busy}
            on:click={() => onAdopt(epic.epic_id)}
          >
            {$t('start.epic.adopt.action', { epic: epic.title })}
          </button>
        </li>
      {/each}
    </ul>
    <div class="epic-adopt__actions">
      <button type="button" data-testid="adopt-new" disabled={busy} on:click={onNew}>
        {$t('start.epic.adopt.new')}
      </button>
    </div>
  {/if}
</section>

<style>
  .epic-adopt {
    display: flex;
    flex-direction: column;
    gap: var(--space-3, 0.75rem);
  }

  .epic-adopt__title {
    margin: 0;
    font-size: var(--text-lg, 1.125rem);
  }

  .epic-adopt__list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
  }

  .epic-adopt__item {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2, 0.5rem);
    padding: var(--space-2, 0.5rem);
    border: 1px solid var(--color-border, #d1d5db);
    border-radius: var(--radius-md, 0.5rem);
  }

  .epic-adopt__offer,
  .epic-adopt__failure {
    margin: 0;
    overflow-wrap: anywhere;
  }

  .epic-adopt__failure {
    color: var(--color-danger, #b3261e);
  }

  .epic-adopt__actions,
  .epic-adopt__item button {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2, 0.5rem);
  }

  .epic-adopt button {
    min-height: 44px;
    padding: var(--space-1, 0.25rem) var(--space-3, 0.75rem);
    cursor: pointer;
  }
</style>
