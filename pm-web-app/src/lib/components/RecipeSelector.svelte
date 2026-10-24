<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';
  import { apiClient, type RecipeRef } from '../api/client';

  export let projectId = '';
  export let recipes: RecipeRef[] | null = null;
  export let onSelect: (
    recipe: RecipeRef | null,
    values: Record<string, string>,
  ) => void = () => {};

  let loaded: RecipeRef[] = recipes ?? [];
  let loading = recipes === null;
  let error = '';
  let selectedKey = '';
  let values: Record<string, string> = {};

  onMount(async () => {
    if (recipes !== null) return;
    try {
      loaded = await apiClient.getRecipes(projectId);
    } catch (e) {
      error = e instanceof Error ? e.message : 'error';
    } finally {
      loading = false;
    }
  });

  $: selected = loaded.find((r) => r.key === selectedKey) ?? null;

  function choose(key: string): void {
    selectedKey = key;
    const recipe = loaded.find((r) => r.key === key) ?? null;
    values = {};
    if (recipe) {
      for (const param of recipe.parameters) {
        if (param.value !== undefined && param.value !== null) {
          values[param.name] = param.value;
        }
      }
    }
    onSelect(recipe, values);
  }

  function setParam(name: string, value: string): void {
    values = { ...values, [name]: value };
    onSelect(selected, values);
  }
</script>

<div class="recipe-selector" data-testid="recipe-selector">
  <p class="recipe-selector__title">{$t('start.recipe.list')}</p>

  {#if loading}
    <p data-testid="recipe-loading">{$t('start.recipe.loading')}</p>
  {:else if error}
    <p class="recipe-selector__error" data-testid="recipe-error">
      {$t('start.error', { cause: error })}
    </p>
  {:else if loaded.length === 0}
    <p data-testid="recipe-empty">{$t('start.recipe.empty', { project: projectId })}</p>
  {:else}
    <label class="recipe-selector__field">
      <span>{$t('start.recipe.list')}</span>
      <select
        data-testid="recipe-select"
        value={selectedKey}
        on:change={(e) => choose((e.currentTarget as HTMLSelectElement).value)}
      >
        <option value="">—</option>
        {#each loaded as recipe (recipe.key)}
          <option value={recipe.key}>
            {recipe.name} · {recipe.description} ({recipe.origin})
          </option>
        {/each}
      </select>
    </label>

    {#if selected}
      <p class="recipe-selector__parameters-title" data-testid="recipe-parameters-title">
        {$t('start.recipe.parameters', { recipe: selected.name })}
      </p>
      {#each selected.parameters as param (param.name)}
        {#if !param.available}
          <p class="recipe-selector__no-option" data-testid={`recipe-no-option-${param.name}`}>
            {$t('start.recipe.no_option', { parameter: param.name, reason: param.reason ?? '' })}
          </p>
        {:else}
          <label class="recipe-selector__field">
            <span>
              {param.name}
              {#if param.proposed}<em data-testid={`recipe-proposed-${param.name}`}
                  >{$t('start.recipe.proposed')}</em
                >{/if}
            </span>
            <select
              data-testid={`recipe-param-${param.name}`}
              value={values[param.name] ?? ''}
              on:change={(e) => setParam(param.name, (e.currentTarget as HTMLSelectElement).value)}
            >
              {#each param.options as option (option)}
                <option value={option}>{option}</option>
              {/each}
            </select>
          </label>
        {/if}
      {/each}
    {/if}
  {/if}
</div>

<style>
  .recipe-selector {
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
  }

  .recipe-selector__title {
    font-weight: 600;
    margin: 0;
  }

  .recipe-selector__field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
  }

  .recipe-selector__field select {
    min-height: 44px;
    padding: var(--space-1, 0.25rem);
  }

  .recipe-selector__no-option,
  .recipe-selector__error {
    color: var(--color-danger-text);
  }

  @media (max-width: 480px) {
    .recipe-selector {
      width: 100%;
    }
  }
</style>
