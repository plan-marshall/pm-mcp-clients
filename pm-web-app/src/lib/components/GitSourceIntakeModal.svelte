<script lang="ts">
  import { t } from '../i18n';
  import PrAdoptPanel from './PrAdoptPanel.svelte';
  import LocalChangesDiffPreview from './LocalChangesDiffPreview.svelte';
  import type { PrAdoptResult, LocalChangesCaptureResult } from '../api/client';

  export let open = false;
  export let projectId = '';
  export let onClose: () => void = () => {};
  export let onCompleted: (id: string) => void = () => {};

  let tab: 'pr' | 'local' = 'pr';
  let done: { kind: 'pr' | 'local'; id: string; snapshot?: string } | null = null;

  function reset(): void {
    tab = 'pr';
    done = null;
  }

  function close(): void {
    reset();
    onClose();
  }

  function adopted(result: PrAdoptResult): void {
    done = { kind: 'pr', id: result.plan_id };
    onCompleted(result.plan_id);
  }

  function captured(result: LocalChangesCaptureResult): void {
    done = { kind: 'local', id: result.plan_id, snapshot: result.snapshot_commit };
    onCompleted(result.plan_id);
  }
</script>

{#if open}
  <div class="git-source-modal" data-testid="git-source-modal" role="dialog" aria-modal="true">
    <div class="git-source-modal__panel">
      <h2 class="git-source-modal__title" data-testid="git-source-title">
        {$t('git_sources.title', { project: projectId })}
      </h2>

      {#if done}
        <p class="git-source-modal__done" data-testid="git-source-done">
          {done.kind === 'pr'
            ? $t('git_sources.pr.adopted', { name: done.id })
            : $t('git_sources.local.captured', { name: done.id })}
        </p>
        {#if done.snapshot}
          <p class="git-source-modal__snapshot" data-testid="git-source-snapshot">
            {$t('git_sources.local.snapshot', { sha: done.snapshot })}
          </p>
        {/if}
        <div class="git-source-modal__actions">
          <button type="button" data-testid="git-source-close" on:click={close}>
            {$t('git_sources.actions.confirm')}
          </button>
        </div>
      {:else}
        <div class="git-source-modal__tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'pr'}
            data-testid="git-source-tab-pr"
            class:active={tab === 'pr'}
            on:click={() => (tab = 'pr')}
          >
            {$t('git_sources.tab.pr')}
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'local'}
            data-testid="git-source-tab-local"
            class:active={tab === 'local'}
            on:click={() => (tab = 'local')}
          >
            {$t('git_sources.tab.local')}
          </button>
        </div>

        {#if tab === 'pr'}
          <PrAdoptPanel {projectId} onAdopted={adopted} />
        {:else}
          <LocalChangesDiffPreview {projectId} onCaptured={captured} />
        {/if}

        <div class="git-source-modal__actions">
          <button type="button" data-testid="git-source-cancel" on:click={close}>
            {$t('git_sources.actions.cancel')}
          </button>
        </div>
      {/if}
    </div>
  </div>
{/if}

<style>
  .git-source-modal {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgb(0 0 0 / 0.4);
    z-index: 50;
  }

  .git-source-modal__panel {
    max-width: 36rem;
    width: 100%;
    max-height: 90vh;
    overflow-y: auto;
    margin: var(--space-3, 0.75rem);
    padding: var(--space-4, 1rem);
    border-radius: var(--radius-lg, 0.75rem);
    background-color: var(--color-bg-surface, #fff);
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
  }

  .git-source-modal__tabs {
    display: flex;
    gap: var(--space-2, 0.5rem);
  }

  .git-source-modal__tabs button {
    min-height: 44px;
    padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
    cursor: pointer;
  }

  .git-source-modal__tabs button.active {
    font-weight: 600;
    text-decoration: underline;
  }

  .git-source-modal__actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2, 0.5rem);
  }

  .git-source-modal__actions button {
    min-height: 44px;
    padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
    cursor: pointer;
  }

  @media (max-width: 480px) {
    .git-source-modal__panel {
      margin: 0;
      border-radius: 0;
      min-height: 100%;
    }
  }
</style>
