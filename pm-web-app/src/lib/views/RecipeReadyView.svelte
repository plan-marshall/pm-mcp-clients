<script lang="ts">
  import { t } from '../i18n';
  import type { RecipeParameter, RecipeRef, StartSource } from '../api/client';

  export let mode: 'parameters' | 'ready' = 'ready';
  export let recipe: RecipeRef | null = null;
  export let parameters: RecipeParameter[] = [];
  export let sources: StartSource[] = [];
  export let planId = '';
  export let idEditable = true;
  export let idFromTitle = false;
  export let title = '';
  export let kind: 'plan' | 'epic' = 'plan';
  export let epicAutoStart: boolean | null = null;
  export let epicAutoStartOrigin: string | null = null;
  export let busy = false;
  export let onTitle: (value: string) => void = () => {};
  export let onParameter: (name: string, value: string) => void = () => {};
  export let onContinue: () => void = () => {};
  export let onStart: () => void = () => {};

  $: impossible = parameters.filter((parameter) => !parameter.available);
  $: recipeName = recipe?.name ?? '';

  function valueOf(parameter: RecipeParameter): string {
    return parameter.value ?? parameter.options[0] ?? '';
  }
</script>

<section class="recipe-ready" data-testid="recipe-ready">
  <h2 class="recipe-ready__title" data-testid="recipe-title">
    {$t(mode === 'parameters' ? 'start.recipe.parameters.title' : 'start.ready.starts_from', {
      recipe: recipeName,
      kind,
    })}
  </h2>

  {#if recipe}
    <p class="recipe-ready__origin">{recipe.description}</p>
  {/if}

  {#if mode === 'parameters'}
    <div class="recipe-ready__parameters" data-testid="recipe-parameters">
      {#each parameters as parameter (parameter.name)}
        <label class="recipe-ready__field">
          <span>
            {parameter.name}
            {#if parameter.proposed}
              <em data-testid={`proposed-${parameter.name}`}>{$t('start.recipe.proposed')}</em>
            {/if}
          </span>
          {#if parameter.options.length > 0}
            <select
              data-testid={`parameter-${parameter.name}`}
              value={valueOf(parameter)}
              disabled={busy}
              on:change={(event) => onParameter(parameter.name, event.currentTarget.value)}
            >
              {#each parameter.options as option}
                <option value={option}>{option}</option>
              {/each}
            </select>
          {:else}
            <span class="recipe-ready__no-option" data-testid={`no-option-${parameter.name}`}>
              {$t('start.recipe.no_option', {
                parameter: parameter.name,
                reason: parameter.reason ?? $t('start.recipe.no_module'),
              })}
            </span>
          {/if}
        </label>
      {/each}
    </div>

    <div class="recipe-ready__actions">
      <button
        type="button"
        data-testid="recipe-continue"
        disabled={busy || impossible.length > 0}
        on:click={onContinue}
      >
        {$t('start.sources.continue')}
      </button>
    </div>
  {:else}
    {#if sources.length > 0}
      <ul class="recipe-ready__sources" data-testid="ready-sources">
        {#each sources as source (source.source_id)}
          <li>
            <span class="recipe-ready__source-kind">{$t(`start.source.${source.kind}`)}</span>
            <span>{source.title ?? source.reference}</span>
          </li>
        {/each}
      </ul>
    {/if}

    <label class="recipe-ready__field">
      <span>{$t('start.ready.title')}</span>
      <input
        type="text"
        data-testid="ready-title"
        value={title}
        disabled={busy}
        on:input={(event) => onTitle(event.currentTarget.value)}
      />
    </label>

    <label class="recipe-ready__field">
      <span>{$t('start.ready.id')}</span>
      <input
        type="text"
        data-testid="ready-id"
        value={planId}
        readonly={!idEditable}
        disabled={busy}
      />
      {#if idFromTitle}
        <span class="recipe-ready__hint">{$t('start.ready.id_from_title')}</span>
      {/if}
    </label>

    {#if parameters.length > 0}
      <dl class="recipe-ready__chosen" data-testid="recipe-chosen-parameters">
        {#each parameters as parameter (parameter.name)}
          <dt>{parameter.name}</dt>
          <dd>{valueOf(parameter)}</dd>
        {/each}
      </dl>
    {/if}

    {#if kind === 'epic' && epicAutoStart !== null}
      <p class="recipe-ready__auto" data-testid="epic-auto-start">
        {$t('start.ready.epic_auto', {
          mode: epicAutoStart ? 'on' : 'off',
          origin: epicAutoStartOrigin ?? 'machine',
        })}
      </p>
    {/if}

    <div class="recipe-ready__actions">
      <button type="button" data-testid="ready-start" disabled={busy} on:click={onStart}>
        {$t(
          recipe
            ? 'start.recipe.run'
            : kind === 'epic'
              ? 'start.ready.start_epic'
              : 'start.ready.start_plan',
        )}
      </button>
    </div>
  {/if}
</section>

<style>
  .recipe-ready {
    display: flex;
    flex-direction: column;
    gap: var(--space-3, 0.75rem);
  }

  .recipe-ready__title {
    margin: 0;
    font-size: var(--text-lg, 1.125rem);
  }

  .recipe-ready__origin,
  .recipe-ready__hint,
  .recipe-ready__auto {
    margin: 0;
    color: var(--color-text-muted, #6b7280);
    font-size: var(--text-sm, 0.875rem);
  }

  .recipe-ready__parameters,
  .recipe-ready__chosen {
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
    margin: 0;
  }

  .recipe-ready__field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
  }

  .recipe-ready__field input,
  .recipe-ready__field select {
    min-height: 44px;
    padding: var(--space-1, 0.25rem);
  }

  .recipe-ready__no-option {
    color: var(--color-danger, #b3261e);
    font-size: var(--text-sm, 0.875rem);
  }

  .recipe-ready__chosen dt {
    font-weight: 600;
  }

  .recipe-ready__sources {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
  }

  .recipe-ready__sources li {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2, 0.5rem);
    overflow-wrap: anywhere;
  }

  .recipe-ready__source-kind {
    font-weight: 600;
  }

  .recipe-ready__chosen dd {
    margin: 0 0 var(--space-1, 0.25rem) 0;
  }

  .recipe-ready__actions button {
    min-height: 44px;
    padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
    cursor: pointer;
  }
</style>
