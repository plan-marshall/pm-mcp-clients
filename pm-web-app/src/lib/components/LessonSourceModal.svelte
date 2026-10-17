<script lang="ts">
  import { t } from '../i18n';
  import {
    apiClient,
    type LessonPremiseCheckResult,
    type PlanStartResult,
    type UnclaimedLessonSummary,
  } from '../api/client';
  import LessonPicker from './LessonPicker.svelte';
  import PremiseEvidenceView from './PremiseEvidenceView.svelte';

  export let open = false;
  export let projectId = '';
  export let onClose: () => void = () => {};
  export let onStarted: (result: PlanStartResult) => void = () => {};

  let lessons: UnclaimedLessonSummary[] = [];
  let loading = false;
  let busy = false;
  let error = '';
  let selected: UnclaimedLessonSummary | null = null;
  let check: LessonPremiseCheckResult | null = null;
  let started: PlanStartResult | null = null;
  let unchanged = '';
  let loadedFor = '';

  $: if (open && projectId && loadedFor !== projectId) {
    loadedFor = projectId;
    void loadLessons();
  }

  function reset(): void {
    lessons = [];
    loading = false;
    busy = false;
    error = '';
    selected = null;
    check = null;
    started = null;
    unchanged = '';
    loadedFor = '';
  }

  async function loadLessons(): Promise<void> {
    loading = true;
    error = '';
    try {
      lessons = await apiClient.getUnclaimedLessons(projectId);
    } catch (e) {
      error = e instanceof Error ? e.message : 'error';
    } finally {
      loading = false;
    }
  }

  async function selectLesson(lesson: UnclaimedLessonSummary): Promise<void> {
    selected = lesson;
    check = null;
    busy = true;
    error = '';
    try {
      check = await apiClient.checkLessonPremise(projectId, lesson.lesson_id);
      if (check.status === 'holds') {
        await claimAndStart(false);
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'error';
    } finally {
      busy = false;
    }
  }

  async function claimAndStart(useAnyway: boolean): Promise<void> {
    if (!selected) return;
    busy = true;
    error = '';
    try {
      await apiClient.claimLesson(projectId, selected.lesson_id, useAnyway);
      const result = await apiClient.startPlan({ project_id: projectId, title: selected.title });
      started = result;
      onStarted(result);
    } catch (e) {
      error = e instanceof Error ? e.message : 'error';
    } finally {
      busy = false;
    }
  }

  async function retire(): Promise<void> {
    if (!selected) return;
    busy = true;
    error = '';
    try {
      const notice = await apiClient.retireLesson(projectId, selected.lesson_id);
      unchanged = notice.lesson_id;
    } catch (e) {
      error = e instanceof Error ? e.message : 'error';
    } finally {
      busy = false;
    }
  }

  function cancelPremise(): void {
    selected = null;
    check = null;
    error = '';
  }

  function close(): void {
    reset();
    onClose();
  }
</script>

{#if open}
  <div class="lesson-modal" data-testid="lesson-modal" role="dialog" aria-modal="true">
    <div class="lesson-modal__panel">
      <h2 class="lesson-modal__title" data-testid="lesson-title">
        {$t('lesson_source.modal.title', { project: projectId })}
      </h2>

      {#if started}
        <p class="lesson-modal__done" data-testid="lesson-done">
          {$t('lesson_source.claimed', {
            lesson: selected?.lesson_id ?? '',
            plan: started.id,
          })}
        </p>
        <div class="lesson-modal__actions">
          <button type="button" data-testid="lesson-close" on:click={close}>
            {$t('lesson_source.action.close')}
          </button>
        </div>
      {:else if unchanged}
        <p class="lesson-modal__unchanged" data-testid="lesson-unchanged">
          {$t('lesson_source.unchanged', { lesson: unchanged })}
        </p>
        <div class="lesson-modal__actions">
          <button type="button" data-testid="lesson-close" on:click={close}>
            {$t('lesson_source.action.close')}
          </button>
        </div>
      {:else if check && check.status !== 'holds'}
        <PremiseEvidenceView
          {check}
          {busy}
          onUseAnyway={() => void claimAndStart(true)}
          onRetire={() => void retire()}
          onCancel={cancelPremise}
        />
      {:else}
        <LessonPicker {lessons} {loading} onSelect={(lesson) => void selectLesson(lesson)} />
      {/if}

      {#if error}
        <p class="lesson-modal__error" data-testid="lesson-error">
          {$t('lesson_source.error', { cause: error })}
        </p>
      {/if}

      {#if !started && !unchanged}
        <div class="lesson-modal__actions">
          <button type="button" data-testid="lesson-cancel" on:click={close}>
            {$t('lesson_source.action.cancel')}
          </button>
        </div>
      {/if}
    </div>
  </div>
{/if}

<style>
  .lesson-modal {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgb(0 0 0 / 0.4);
    z-index: 50;
  }

  .lesson-modal__panel {
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

  .lesson-modal__title {
    margin: 0;
    font-size: 1.125rem;
  }

  .lesson-modal__actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2, 0.5rem);
  }

  .lesson-modal__actions button {
    min-height: 44px;
    padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
    cursor: pointer;
  }

  .lesson-modal__error {
    color: var(--color-danger, #b3261e);
  }

  @media (max-width: 480px) {
    .lesson-modal__panel {
      margin: 0;
      border-radius: 0;
      min-height: 100%;
    }
  }
</style>
