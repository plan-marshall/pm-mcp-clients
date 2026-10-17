<script lang="ts">
  import { t } from '../i18n';
  import type { WorkActions } from '../api/client';

  export let subjectId: string;
  export let subjectKind: 'plan' | 'epic' = 'plan';
  export let actions: WorkActions | null = null;
  export let busy = false;
  export let onAction: (action: string) => void = () => {};

  function label(action: string): string {
    return $t(`control.${subjectKind}.${action}`);
  }
</script>

<section class="control-actions" data-testid="control-actions" aria-label={$t('control.actions.title', { subject: subjectId })}>
  <h3 class="control-actions__title">{$t('control.actions.title', { subject: subjectId })}</h3>
  {#if actions}
    <ul class="control-actions__list">
      {#each actions.actions as availability (availability.action)}
        <li class="control-actions__item">
          <button
            type="button"
            class="control-actions__button"
            data-testid={`action-${availability.action}`}
            disabled={!availability.available || busy}
            title={!availability.available ? availability.reason ?? '' : ''}
            on:click={() => onAction(availability.action)}
          >
            {label(availability.action)}
          </button>
          {#if !availability.available && availability.reason}
            <span class="control-actions__reason" data-testid={`reason-${availability.action}`}>
              {$t('control.action.disabled', {
                action: label(availability.action),
                reason: availability.reason,
              })}
            </span>
          {/if}
        </li>
      {/each}
    </ul>
  {:else}
    <p class="control-actions__empty" data-testid="control-actions-empty">…</p>
  {/if}
</section>

<style>
  .control-actions {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
  }

  .control-actions__title {
    margin: 0;
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .control-actions__list {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    list-style: none;
    margin: 0;
    padding: 0;
  }

  .control-actions__item {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .control-actions__button {
    min-height: 44px;
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    cursor: pointer;
  }

  .control-actions__button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .control-actions__reason {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  @media (max-width: 480px) {
    .control-actions__button {
      width: 100%;
    }
  }
</style>
