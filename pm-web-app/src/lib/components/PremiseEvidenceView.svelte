<script lang="ts">
  import { t } from '../i18n';
  import type { LessonPremiseCheckResult } from '../api/client';

  export let check: LessonPremiseCheckResult;
  export let busy = false;
  export let onUseAnyway: () => void = () => {};
  export let onRetire: () => void = () => {};
  export let onCancel: () => void = () => {};
</script>

<div class="premise-evidence" data-testid="premise-evidence">
  <h3 class="premise-evidence__title" data-testid="premise-title">
    {$t('lesson_source.premise.title')}
  </h3>
  <p class="premise-evidence__explain">{$t('lesson_source.premise.explain')}</p>

  <div class="premise-evidence__evidence">
    <span class="premise-evidence__evidence-title">
      {$t('lesson_source.premise.evidence_title')}
    </span>
    <ul class="premise-evidence__list">
      {#each check.evidence as evidence, index (index)}
        <li class="premise-evidence__item" data-testid={`premise-evidence-${index}`}>
          <span class="premise-evidence__kind">
            {$t(`lesson_source.evidence.${evidence.kind}`)}
          </span>
          <span class="premise-evidence__name">{evidence.name}</span>
          {#if evidence.expected_path}
            <span class="premise-evidence__path">({evidence.expected_path})</span>
          {/if}
          {#if evidence.last_verified_commit}
            <span class="premise-evidence__commit">· {evidence.last_verified_commit}</span>
          {/if}
        </li>
      {/each}
    </ul>
  </div>

  <div class="premise-evidence__actions">
    <button
      type="button"
      class="premise-evidence__use"
      data-testid="premise-use-anyway"
      on:click={onUseAnyway}
      disabled={busy}
    >
      {$t('lesson_source.premise.use_anyway')}
    </button>
    <button
      type="button"
      class="premise-evidence__retire"
      data-testid="premise-retire"
      on:click={onRetire}
      disabled={busy}
    >
      {$t('lesson_source.premise.retire')}
    </button>
    <button
      type="button"
      class="premise-evidence__cancel"
      data-testid="premise-cancel"
      on:click={onCancel}
      disabled={busy}
    >
      {$t('lesson_source.premise.cancel')}
    </button>
  </div>
</div>

<style>
  .premise-evidence {
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
  }

  .premise-evidence__title {
    margin: 0;
    color: var(--color-danger, #b3261e);
  }

  .premise-evidence__list {
    margin: 0;
    padding-left: 1.25rem;
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
  }

  .premise-evidence__kind {
    font-weight: 600;
  }

  .premise-evidence__item {
    word-break: break-word;
  }

  .premise-evidence__actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2, 0.5rem);
  }

  .premise-evidence__actions button {
    min-height: 44px;
    padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
    cursor: pointer;
  }

  @media (max-width: 480px) {
    .premise-evidence__actions {
      flex-direction: column;
    }
  }
</style>
