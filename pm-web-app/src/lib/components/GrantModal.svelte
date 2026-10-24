<script lang="ts">
  import { t } from '../i18n';

  export let open = false;
  export let repository = '';
  export let onGrant: (scope: 'once' | 'project') => void = () => {};
  export let onRefuse: () => void = () => {};

  function grant(scope: 'once' | 'project'): void {
    onGrant(scope);
  }
</script>

{#if open}
  <div class="grant-modal" data-testid="grant-modal" role="dialog" aria-modal="true">
    <div class="grant-modal__panel">
      <h2 class="grant-modal__title" data-testid="grant-title">
        {$t('start.issue.grant.title', { repository })}
      </h2>

      <div class="grant-modal__actions">
        <button type="button" data-testid="grant-once" on:click={() => grant('once')}>
          {$t('start.issue.grant.once')}
        </button>
        <button type="button" data-testid="grant-project" on:click={() => grant('project')}>
          {$t('start.issue.grant.project')}
        </button>
        <button type="button" data-testid="grant-no" on:click={onRefuse}>
          {$t('start.issue.grant.no')}
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  .grant-modal {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: var(--space-3);
    background-color: var(--color-overlay);
    z-index: 60;
  }

  .grant-modal__panel {
    width: 100%;
    max-width: 28rem;
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    padding: var(--space-4);
    background-color: var(--color-bg-surface);
    border-radius: var(--radius-lg);
  }

  .grant-modal__title {
    margin: 0;
    font-size: var(--text-lg, 1.125rem);
  }

  .grant-modal__actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2, 0.5rem);
  }

  .grant-modal__actions button {
    min-height: 44px;
    padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
    cursor: pointer;
  }
</style>
