<script lang="ts">
  import { t } from '../i18n';
  import type { RecipeParameter, RecipeRef, StartSource } from '../api/client';
  import Field from '../components/Field.svelte';
  import TextField from '../components/TextField.svelte';
  import SelectField, { type SelectOption } from '../components/SelectField.svelte';
  import { isConfirmingEnter } from '../utils/ime';

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
  export let onId: (value: string) => void = () => {};
  export let onParameter: (name: string, value: string) => void = () => {};
  export let onSubmit: (() => void) | undefined = undefined;

  $: recipeName = recipe?.name ?? '';

  function valueOf(parameter: RecipeParameter): string {
    return parameter.value ?? parameter.options[0] ?? '';
  }

  function optionsOf(parameter: RecipeParameter): SelectOption[] {
    return parameter.options.map((option) => ({ value: option, label: option }));
  }

  function handleKeyDown(event: KeyboardEvent): void {
    if (!isConfirmingEnter(event)) {
      return;
    }
    event.preventDefault();
    onSubmit?.();
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
        {#if parameter.options.length > 0}
          <div class="recipe-ready__param">
            <SelectField
              label={parameter.name}
              options={optionsOf(parameter)}
              value={valueOf(parameter)}
              disabled={busy}
              testid={`parameter-${parameter.name}`}
              hint={parameter.proposed ? $t('start.recipe.proposed') : undefined}
              onchange={(val) => onParameter(parameter.name, val)}
            />
            {#if parameter.proposed}
              <span data-testid={`proposed-${parameter.name}`} hidden>{$t('start.recipe.proposed')}</span>
            {/if}
          </div>
        {:else}
          <Field label={parameter.name}>
            <span class="recipe-ready__no-option" data-testid={`no-option-${parameter.name}`}>
              {$t('start.recipe.no_option', {
                parameter: parameter.name,
                reason: parameter.reason ?? $t('start.recipe.no_module'),
              })}
            </span>
          </Field>
        {/if}
      {/each}
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

    <TextField
      label={$t('start.ready.title')}
      value={title}
      disabled={busy}
      testid="ready-title"
      oninput={(event) => onTitle(event.currentTarget.value)}
      onkeydown={handleKeyDown}
    />

    <TextField
      label={$t('start.ready.id')}
      value={planId}
      readonly={!idEditable}
      disabled={busy}
      mono
      hint={idFromTitle ? $t('start.ready.id_from_title') : undefined}
      testid="ready-id"
      oninput={(event) => onId(event.currentTarget.value)}
      onkeydown={handleKeyDown}
    />

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
  .recipe-ready__auto {
    margin: 0;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .recipe-ready__parameters,
  .recipe-ready__chosen {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin: 0;
  }

  .recipe-ready__no-option {
    color: var(--color-danger-text);
    font-size: var(--text-sm);
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
</style>
