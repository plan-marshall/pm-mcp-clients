<script lang="ts">
  import { t } from '../i18n';
  import type { UnclaimedLessonSummary } from '../api/client';

  export let lessons: UnclaimedLessonSummary[] = [];
  export let loading = false;
  export let onSelect: (lesson: UnclaimedLessonSummary) => void = () => {};

  function recordedLine(lesson: UnclaimedLessonSummary): string {
    const date = lesson.recorded_at ? lesson.recorded_at.slice(0, 10) : '';
    if (lesson.citing_plan_id) {
      return $t('lesson_source.list.recorded_by', { date, plan: lesson.citing_plan_id });
    }
    return $t('lesson_source.list.recorded', { date });
  }
</script>

<div class="lesson-picker" data-testid="lesson-picker">
  <h3 class="lesson-picker__title">{$t('lesson_source.picker.title')}</h3>

  {#if loading}
    <p class="lesson-picker__status" data-testid="lesson-picker-loading">
      {$t('lesson_source.picker.checking')}
    </p>
  {:else if lessons.length === 0}
    <p class="lesson-picker__empty" data-testid="lesson-picker-empty">
      {$t('lesson_source.picker.empty', { project: '' })}
    </p>
  {:else}
    <ul class="lesson-picker__list">
      {#each lessons as lesson (lesson.lesson_id)}
        <li class="lesson-picker__item" data-testid={`lesson-item-${lesson.lesson_id}`}>
          <div class="lesson-picker__body">
            <span class="lesson-picker__lesson-title">{lesson.title}</span>
            <span class="lesson-picker__summary">{lesson.summary}</span>
            <span class="lesson-picker__meta">
              {recordedLine(lesson)} ·
              {$t('lesson_source.list.cites', { count: lesson.symbols_cited.length })}
            </span>
          </div>
          <button
            type="button"
            class="lesson-picker__select"
            data-testid={`lesson-select-${lesson.lesson_id}`}
            on:click={() => onSelect(lesson)}
          >
            {$t('lesson_source.picker.select')}
          </button>
        </li>
      {/each}
    </ul>
  {/if}
</div>

<style>
  .lesson-picker {
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
  }

  .lesson-picker__title {
    margin: 0;
    font-size: 1rem;
  }

  .lesson-picker__list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
  }

  .lesson-picker__item {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2, 0.5rem);
    padding: var(--space-2, 0.5rem);
    border: 1px solid var(--color-border, #d1d5db);
    border-radius: var(--radius-md, 0.5rem);
  }

  .lesson-picker__body {
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
    min-width: 0;
  }

  .lesson-picker__lesson-title {
    font-weight: 600;
  }

  .lesson-picker__meta {
    color: var(--color-text-muted, #6b7280);
    font-size: 0.875rem;
  }

  .lesson-picker__select {
    min-height: 44px;
    padding: var(--space-1, 0.25rem) var(--space-2, 0.5rem);
    cursor: pointer;
  }

  .lesson-picker__status,
  .lesson-picker__empty {
    margin: 0;
    color: var(--color-text-muted, #6b7280);
  }

  @media (max-width: 480px) {
    .lesson-picker__item {
      flex-direction: column;
      align-items: stretch;
    }
  }
</style>
