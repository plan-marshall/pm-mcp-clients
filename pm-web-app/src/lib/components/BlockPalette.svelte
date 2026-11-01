<script lang="ts">
  import { t } from '../i18n';
  import Button from './Button.svelte';
  import type { BlockKind, BuildingBlock, WorkflowCatalogue } from '../api/client';

  export let catalogue: WorkflowCatalogue | null = null;
  export let selected: BuildingBlock | null = null;
  export let onInsert: (block: BuildingBlock) => void = () => {};
  export let onSelect: (block: BuildingBlock) => void = () => {};

  let query = '';

  const groupOrder: BlockKind[] = [
    'keyword',
    'primitive',
    'guard',
    'link_relation',
    'condition',
    'sub_workflow',
    'role',
    'parameter',
  ];

  $: blocks = catalogue?.blocks ?? [];
  $: filtered = blocks.filter((block) => matches(block, query));
  $: groups = groupOrder
    .map((kind) => ({ kind, items: filtered.filter((block) => block.kind === kind) }))
    .filter((group) => group.items.length > 0);

  function matches(block: BuildingBlock, needle: string): boolean {
    if (!needle) return true;
    const lower = needle.toLowerCase();
    return (
      block.name.toLowerCase().includes(lower) ||
      block.summary.toLowerCase().includes(lower) ||
      block.parameters.some((parameter) => parameter.toLowerCase().includes(lower))
    );
  }

  function groupLabel(kind: BlockKind): string {
    return $t('designer.blocks.group', { group: kind });
  }
</script>

<div class="palette" data-testid="block-palette">
  <div class="palette__head">
    <h2 class="palette__title">{$t('designer.blocks.title')}</h2>
    {#if catalogue}
      <span class="palette__version" data-testid="blocks-version">
        {$t('designer.blocks.version', { version: catalogue.runtime_version })}
      </span>
    {/if}
  </div>
  <p class="palette__scope" data-testid="blocks-scope">
    {$t('designer.blocks.scope', {
      scope: catalogue && catalogue.scope !== 'machine' ? 'project' : 'machine',
      project: catalogue?.scope ?? '',
    })}
  </p>

  <input
    class="palette__search"
    type="search"
    bind:value={query}
    placeholder={$t('designer.blocks.search')}
    aria-label={$t('designer.blocks.search')}
    data-testid="blocks-search"
  />

  {#if groups.length === 0}
    <p class="palette__empty" data-testid="blocks-no-match">
      {$t('designer.blocks.no_match', { query })}
    </p>
  {:else}
    <div class="palette__groups">
      {#each groups as group (group.kind)}
        <section class="palette__group" data-testid={`block-group-${group.kind}`}>
          <h3 class="palette__group-title">
            {groupLabel(group.kind)}
            <span class="palette__count">{group.items.length}</span>
          </h3>
          <ul class="palette__list">
            {#each group.items as block (block.name)}
              <li
                class="palette__item"
                class:palette__item--selected={selected?.name === block.name}
              >
                <button
                  type="button"
                  class="palette__name"
                  on:click={() => onSelect(block)}
                  data-testid={`block-select-${block.name}`}
                >
                  {block.name}
                </button>
                <Button
                  variant="ghost"
                  on:click={() => onInsert(block)}
                  data-testid={`block-insert-${block.name}`}
                >
                  {$t('designer.blocks.insert')}
                </Button>
              </li>
            {/each}
          </ul>
        </section>
      {/each}
    </div>
  {/if}

  {#if selected}
    <div class="palette__detail" data-testid="block-detail">
      <h3 class="palette__detail-title">{selected.name}</h3>
      <p class="palette__detail-summary">{selected.summary}</p>
      {#if selected.parameters.length > 0}
        <h4 class="palette__detail-heading">{$t('designer.blocks.detail.parameters')}</h4>
        <ul class="palette__detail-list">
          {#each selected.parameters as parameter}
            <li>{parameter}</li>
          {/each}
        </ul>
      {/if}
      {#if selected.outcomes.length > 0}
        <h4 class="palette__detail-heading">{$t('designer.blocks.detail.outcomes')}</h4>
        <ul class="palette__detail-list">
          {#each selected.outcomes as outcome}
            <li>{outcome}</li>
          {/each}
        </ul>
      {/if}
      {#if selected.constraints.length > 0}
        <h4 class="palette__detail-heading">{$t('designer.blocks.detail.needs')}</h4>
        <ul class="palette__detail-list">
          {#each selected.constraints as constraint}
            <li>{constraint}</li>
          {/each}
        </ul>
      {/if}
      {#if selected.used_in.length > 0}
        <h4 class="palette__detail-heading">{$t('designer.blocks.detail.used_in')}</h4>
        <ul class="palette__detail-list">
          {#each selected.used_in as used}
            <li>{used}</li>
          {/each}
        </ul>
      {/if}
      <h4 class="palette__detail-heading">{$t('designer.blocks.detail.inserts')}</h4>
      <pre class="palette__template" data-testid="block-template">{selected.template}</pre>
      <Button
        variant="primary"
        on:click={() => onInsert(selected)}
        data-testid="block-detail-insert"
      >
        {$t('designer.blocks.insert')}
      </Button>
    </div>
  {/if}
</div>

<style>
  .palette {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    height: 100%;
    padding: var(--space-3);
    overflow-y: auto;
    background-color: var(--color-bg-surface);
  }

  .palette__head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
  }

  .palette__title {
    font-size: var(--text-sm);
    font-weight: 700;
    letter-spacing: 0.05em;
    color: var(--color-text-secondary);
  }

  .palette__version {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .palette__scope {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    margin: 0;
  }

  .palette__search {
    width: 100%;
    padding: var(--space-2);
    font-size: var(--text-sm);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .palette__empty {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }

  .palette__groups {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .palette__group-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: var(--text-xs);
    font-weight: 700;
    color: var(--color-text-secondary);
    margin: 0 0 var(--space-1) 0;
  }

  .palette__count {
    font-weight: 500;
    color: var(--color-text-muted);
  }

  .palette__list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .palette__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);
    border-radius: var(--radius-sm);
  }

  .palette__item--selected {
    background-color: var(--color-primary-subtle);
  }

  .palette__name {
    flex: 1;
    text-align: left;
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-sm);
    font-family: var(--font-mono, monospace);
    color: var(--color-text-primary);
    background: none;
    border: none;
    cursor: pointer;
  }

  .palette__detail {
    border-top: 1px solid var(--color-border);
    padding-top: var(--space-3);
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .palette__detail-title {
    font-size: var(--text-sm);
    font-weight: 700;
    margin: 0;
  }

  .palette__detail-summary {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    margin: 0;
  }

  .palette__detail-heading {
    font-size: var(--text-xs);
    font-weight: 700;
    color: var(--color-text-secondary);
    margin: var(--space-1) 0 0 0;
  }

  .palette__detail-list {
    margin: 0;
    padding-left: var(--space-4);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .palette__template {
    margin: 0;
    padding: var(--space-2);
    background-color: var(--color-bg-inset);
    border-radius: var(--radius-sm);
    font-size: var(--text-xs);
    white-space: pre-wrap;
    overflow-x: auto;
  }
</style>
