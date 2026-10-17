<script lang="ts">
  import { t } from '../i18n';
  import { apiClient, type PlanStartResult, type RecipeRef, type SourceInput } from '../api/client';
  import RecipeSelector from './RecipeSelector.svelte';
  import SourceIntakePanel from './SourceIntakePanel.svelte';
  import { openLessonSource } from '../stores/lessonSource';

  export let open = false;
  export let projectId = '';
  export let kind: 'plan' | 'epic' = 'plan';
  export let onClose: () => void = () => {};
  export let onStarted: (result: PlanStartResult) => void = () => {};

  let mode: 'sources' | 'recipe' = 'sources';
  let description = '';
  let documents = '';
  let issue = '';
  let title = '';
  let sources: SourceInput[] = [];
  let recipe: RecipeRef | null = null;
  let recipeValues: Record<string, string> = {};
  let busy = false;
  let error = '';
  let started: PlanStartResult | null = null;

  $: isEpic = kind === 'epic';

  function reset(): void {
    mode = 'sources';
    description = '';
    documents = '';
    issue = '';
    title = '';
    sources = [];
    recipe = null;
    recipeValues = {};
    busy = false;
    error = '';
    started = null;
  }

  async function start(): Promise<void> {
    busy = true;
    error = '';
    try {
      if (isEpic) {
        started = await apiClient.startEpic({
          project_id: projectId,
          objective: description.trim() || undefined,
          title: title.trim() || undefined,
        });
      } else if (mode === 'recipe' && recipe) {
        started = await apiClient.startPlan({
          project_id: projectId,
          recipe_id: recipe.key,
          parameters: recipeValues,
          title: title.trim() || undefined,
        });
      } else {
        started = await apiClient.startPlan({
          project_id: projectId,
          objective: description.trim() || undefined,
          documents: sources.filter((s) => s.kind === 'documents').map((s) => s.location),
          issue: issue.trim() || undefined,
          title: title.trim() || undefined,
        });
      }
      onStarted(started);
    } catch (e) {
      error = e instanceof Error ? e.message : 'error';
    } finally {
      busy = false;
    }
  }

  function close(): void {
    reset();
    onClose();
  }

  function startFromLesson(): void {
    openLessonSource(projectId);
    close();
  }
</script>

{#if open}
  <div class="start-modal" data-testid="start-modal" role="dialog" aria-modal="true">
    <div class="start-modal__panel">
      <h2 class="start-modal__title" data-testid="start-title">
        {$t('start.modal.title', { kind, project: projectId })}
      </h2>

      {#if started}
        <p class="start-modal__done" data-testid="start-done">
          {$t('start.started.title', { name: started.id })}
        </p>
        <div class="start-modal__actions">
          <button type="button" data-testid="start-close" on:click={close}>
            {$t('start.started.back')}
          </button>
        </div>
      {:else}
        {#if !isEpic}
          <div class="start-modal__modes" role="tablist" aria-label={$t('start.choose.title')}>
            <button
              type="button"
              role="tab"
              aria-selected={mode === 'sources'}
              data-testid="start-mode-sources"
              class:active={mode === 'sources'}
              on:click={() => (mode = 'sources')}
            >
              {$t('start.mode.sources')}
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={mode === 'recipe'}
              data-testid="start-mode-recipe"
              class:active={mode === 'recipe'}
              on:click={() => (mode = 'recipe')}
            >
              {$t('start.mode.recipe')}
            </button>
          </div>
        {/if}

        {#if !isEpic && mode === 'recipe'}
          <RecipeSelector {projectId} onSelect={(r, v) => ((recipe = r), (recipeValues = v))} />
        {:else}
          <SourceIntakePanel
            bind:description
            bind:documents
            bind:issue
            onChange={(s) => (sources = s)}
          />
        {/if}

        {#if !isEpic && mode === 'sources'}
          <button
            type="button"
            class="start-modal__lesson"
            data-testid="start-from-lesson"
            on:click={startFromLesson}
          >
            {$t('lesson_source.start_from_lesson')}
          </button>
        {/if}

        <label class="start-modal__field">
          <span>{$t('start.field.title')}</span>
          <input type="text" data-testid="start-title-input" bind:value={title} />
        </label>

        {#if error}
          <p class="start-modal__error" data-testid="start-error">
            {$t('start.error', { cause: error })}
          </p>
        {/if}

        <div class="start-modal__actions">
          <button
            type="button"
            class="start-modal__submit"
            data-testid="start-submit"
            on:click={start}
            disabled={busy || (mode === 'recipe' && !isEpic && recipe === null)}
          >
            {$t('start.action.start', { kind })}
          </button>
          <button type="button" data-testid="start-cancel" on:click={close}>
            {$t('start.action.cancel')}
          </button>
        </div>
      {/if}
    </div>
  </div>
{/if}

<style>
  .start-modal {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgb(0 0 0 / 0.4);
    z-index: 50;
  }

  .start-modal__panel {
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

  .start-modal__modes {
    display: flex;
    gap: var(--space-2, 0.5rem);
  }

  .start-modal__modes button.active {
    font-weight: 600;
    text-decoration: underline;
  }

  .start-modal__field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
  }

  .start-modal__field input {
    min-height: 44px;
    padding: var(--space-1, 0.25rem);
  }

  .start-modal__actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2, 0.5rem);
  }

  .start-modal__actions button {
    min-height: 44px;
    padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
    cursor: pointer;
  }

  .start-modal__error {
    color: var(--color-danger, #b3261e);
  }

  .start-modal__lesson {
    min-height: 44px;
    padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
    cursor: pointer;
    align-self: flex-start;
  }

  @media (max-width: 480px) {
    .start-modal__panel {
      margin: 0;
      border-radius: 0;
      min-height: 100%;
    }
  }
</style>
