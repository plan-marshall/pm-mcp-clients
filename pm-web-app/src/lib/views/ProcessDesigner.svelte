<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import BlockPalette from '../components/BlockPalette.svelte';
  import WorkflowValidationResults from '../components/WorkflowValidationResults.svelte';
  import {
    apiClient,
    type BuildingBlock,
    type ProjectSummary,
    type WorkflowCatalogue,
    type WorkflowStructure,
    type WorkflowUnit,
    type WorkflowValidationResult,
  } from '../api/client';
  import { runtimeStore, navigateTo } from '../stores/runtime';

  let projectId: string | null = null;
  let projects: ProjectSummary[] = [];
  let units: WorkflowUnit[] = [];
  let catalogue: WorkflowCatalogue | null = null;
  let selectedBlock: BuildingBlock | null = null;

  let unitName = '';
  let unitKind = 'subworkflow';
  let text = '';
  let digest: string | null = null;

  let result: WorkflowValidationResult | null = null;
  let structure: WorkflowStructure | null = null;
  let outdated = false;
  let isValidating = false;
  let isPlacing = false;
  let notice = '';
  let placeMessage = '';
  let previewOpen = false;

  $: projectLabel = projectId ?? '';
  $: checkedAgainst = projectId
    ? $t('designer.checked_against', { project: displayNameFor(projectId) })
    : $t('designer.checked_against_machine');

  onMount(() => {
    void loadAll();
  });

  function displayNameFor(id: string): string {
    const match = projects.find((project) => project.project_id === id);
    return match?.display_name ?? id;
  }

  async function loadAll(): Promise<void> {
    notice = '';
    try {
      const [unitList, catalogueData, projectData] = await Promise.all([
        apiClient.getWorkflows(projectId),
        apiClient.getWorkflowCatalogue(projectId),
        apiClient.getProjects().catch(() => ({ projects: [] as ProjectSummary[] })),
      ]);
      units = Array.isArray(unitList) ? unitList : [];
      catalogue = catalogueData;
      projects = projectData?.projects ?? [];
      selectedBlock = null;
      if (units.length > 0) {
        openUnit(units[0]);
      }
    } catch {
      notice = 'The workflow units could not be loaded.';
    }
  }

  function openUnit(unit: WorkflowUnit): void {
    const level = unit.levels.find((entry) => entry.level === unit.level_in_force) ?? unit.levels[0];
    unitName = unit.name;
    unitKind = unit.frontmatter?.type || 'subworkflow';
    text = level?.text ?? '';
    digest = level?.digest ?? null;
    result = null;
    structure = null;
    outdated = false;
    placeMessage = '';
  }

  function onTextInput(): void {
    outdated = result !== null;
    placeMessage = '';
  }

  function insertBlock(block: BuildingBlock): void {
    text = `${text.replace(/\s+$/, '')}\n${block.template}\n`;
    outdated = result !== null;
    placeMessage = '';
  }

  async function validate(): Promise<void> {
    isValidating = true;
    notice = '';
    try {
      result = await apiClient.validateWorkflow({
        text,
        unit_name: unitName,
        unit_kind: unitKind,
        project: projectId,
        digest,
      });
      structure = result.structure ?? structure;
      outdated = false;
    } catch {
      notice = 'The validation could not be run.';
    } finally {
      isValidating = false;
    }
  }

  async function place(): Promise<void> {
    if (!result?.valid) return;
    isPlacing = true;
    notice = '';
    try {
      const placed = await apiClient.placeWorkflowUnit(projectId, unitName, {
        text,
        unit_name: unitName,
        unit_kind: unitKind,
        author: $runtimeStore.deviceLabel || null,
        digest,
      });
      await loadAll();
      placeMessage = $t('designer.place.result', {
        name: unitName,
        author: placed.author ?? 'you',
        date: placed.release ?? '',
        digest: placed.digest ?? '',
      });
    } catch {
      notice = 'The placing was refused.';
    } finally {
      isPlacing = false;
    }
  }

  async function removeUnit(unit: WorkflowUnit): Promise<void> {
    notice = '';
    try {
      await apiClient.removeWorkflowUnit(projectId, unit.name);
      await loadAll();
    } catch {
      notice = 'The removal was refused.';
    }
  }

  async function onProjectChange(): Promise<void> {
    await loadAll();
  }
</script>

<div class="designer" data-testid="process-designer">
  <header class="designer__head">
    <div class="designer__head-left">
      <h1 class="designer__title" data-testid="designer-title">{$t('designer.view.title')}</h1>
      <span class="designer__unit" data-testid="designer-unit">{unitName || '—'}</span>
      <span class="designer__checked" data-testid="designer-checked">{checkedAgainst}</span>
    </div>
    <div class="designer__head-right">
      <select
        class="designer__project"
        bind:value={projectId}
        on:change={onProjectChange}
        data-testid="project-select"
        aria-label={$t('designer.open.question')}
      >
        <option value={null}>{$t('designer.level.machine')}</option>
        {#each projects as project (project.project_id)}
          <option value={project.project_id}>{project.display_name}</option>
        {/each}
      </select>
      <Button
        variant="primary"
        loading={isValidating}
        on:click={validate}
        data-testid="validate-button"
      >
        {$t('designer.validate.action')}
      </Button>
      <Button
        variant="secondary"
        disabled={!result?.valid || outdated || isPlacing}
        loading={isPlacing}
        on:click={place}
        data-testid="place-button"
      >
        {$t('designer.place.action')}
      </Button>
      <Button variant="ghost" on:click={() => navigateTo('workspace')}>
        {$t('designer.back')}
      </Button>
    </div>
  </header>

  {#if notice}
    <p class="designer__notice" role="alert" data-testid="designer-notice">{notice}</p>
  {/if}
  {#if placeMessage}
    <p class="designer__placed" data-testid="designer-placed">{placeMessage}</p>
  {/if}

  <div class="designer__workspace">
    <aside class="designer__units">
      <h2 class="designer__units-title">{$t('designer.input.installed')}</h2>
      {#if units.length === 0}
        <p class="designer__units-empty">—</p>
      {:else}
        <ul class="designer__units-list">
          {#each units as unit (unit.name)}
            <li class="designer__unit-row" class:designer__unit-row--active={unit.name === unitName}>
              <button
                type="button"
                class="designer__unit-open"
                on:click={() => openUnit(unit)}
                data-testid={`unit-open-${unit.name}`}
              >
                {unit.name}
              </button>
              <span class="designer__unit-level">
                {$t(`designer.level.${unit.level_in_force}`)}
              </span>
              {#if unit.group === 'compiled_only'}
                <span class="designer__unit-compiled" data-testid={`unit-compiled-${unit.name}`}>
                  {$t('designer.installed.compiled_only')}
                </span>
              {:else}
                <Button
                  variant="ghost"
                  on:click={() => removeUnit(unit)}
                  data-testid={`unit-remove-${unit.name}`}
                >
                  {$t('designer.remove.action')}
                </Button>
              {/if}
            </li>
          {/each}
        </ul>
      {/if}
      <BlockPalette
        {catalogue}
        selected={selectedBlock}
        onInsert={insertBlock}
        onSelect={(block) => (selectedBlock = block)}
      />
    </aside>

    <section class="designer__text">
      <textarea
        class="designer__editor"
        bind:value={text}
        on:input={onTextInput}
        spellcheck="false"
        data-testid="workflow-text"
        aria-label={$t('designer.pane.text')}
      ></textarea>
    </section>

    <aside class="designer__structure" data-testid="structure-pane">
      <h2 class="designer__structure-title">{$t('designer.pane.structure')}</h2>
      {#if structure}
        <p class="designer__structure-name">{structure.display_name || unitName}</p>
        {#if structure.entries.length > 0}
          <p class="designer__structure-io">
            {structure.entries.map((entry) => `→ ${entry}`).join('  ')}
          </p>
        {/if}
        {#if structure.exits.length > 0}
          <p class="designer__structure-io">
            {structure.exits.map((exit) => `◂ ${exit}`).join('  ')}
          </p>
        {/if}
        <ul class="designer__structure-states">
          {#each structure.states as state (state.id)}
            <li class="designer__structure-state" data-testid={`structure-state-${state.id}`}>
              <span class="designer__structure-state-id">{state.id}</span>
              {#if state.calls.length > 0}
                <span class="designer__structure-call">↳ {state.calls.join(', ')}</span>
              {/if}
              {#each state.outcomes as outcome}
                <span class="designer__structure-outcome">{outcome}</span>
              {/each}
            </li>
          {/each}
        </ul>
      {:else}
        <p class="designer__structure-empty">—</p>
      {/if}
    </aside>
  </div>

  <WorkflowValidationResults
    {result}
    {projectLabel}
    {outdated}
    onPreview={() => (previewOpen = true)}
  />

  {#if previewOpen && result?.composed}
    <div class="designer__preview" data-testid="composed-preview">
      <div class="designer__preview-head">
        <h2 class="designer__preview-title">
          {$t('designer.preview.title', { project: projectLabel || $t('designer.level.machine') })}
        </h2>
        <Button variant="ghost" on:click={() => (previewOpen = false)} data-testid="preview-close">
          {$t('web.common.close')}
        </Button>
      </div>
      <ol class="designer__preview-nodes">
        {#each result.composed.nodes as node (node.node_id)}
          <li class="designer__preview-node" data-testid={`preview-node-${node.node_id}`}>
            <span class="designer__preview-node-title">{node.title}</span>
            <span class="designer__preview-node-kind">{$t('plan.node.kind.phase')}</span>
            <span class="designer__preview-node-state">{$t('plan.node.state.planned')}</span>
          </li>
        {/each}
      </ol>
    </div>
  {/if}
</div>

<style>
  .designer {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background-color: var(--color-bg-app);
  }

  .designer__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-4);
    padding: var(--space-3) var(--space-6);
    background-color: var(--color-bg-surface);
    border-bottom: 1px solid var(--color-border);
    flex-wrap: wrap;
  }

  .designer__head-left {
    display: flex;
    align-items: baseline;
    gap: var(--space-3);
    flex-wrap: wrap;
  }

  .designer__title {
    font-size: var(--text-lg);
    font-weight: 700;
  }

  .designer__unit {
    font-family: var(--font-mono, monospace);
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .designer__checked {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .designer__head-right {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .designer__project {
    padding: var(--space-2);
    font-size: var(--text-sm);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .designer__notice {
    margin: 0;
    padding: var(--space-2) var(--space-6);
    background-color: var(--color-danger-subtle);
    color: var(--color-danger);
    font-size: var(--text-sm);
  }

  .designer__placed {
    margin: 0;
    padding: var(--space-2) var(--space-6);
    background-color: var(--color-success-subtle);
    color: var(--color-success);
    font-size: var(--text-sm);
  }

  .designer__workspace {
    flex: 1;
    display: grid;
    grid-template-columns: 320px 1fr 320px;
    gap: 1px;
    background-color: var(--color-border);
    overflow: hidden;
  }

  .designer__units {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    padding: var(--space-3);
    background-color: var(--color-bg-surface);
    overflow-y: auto;
  }

  .designer__units-title {
    font-size: var(--text-sm);
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--color-text-secondary);
  }

  .designer__units-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .designer__unit-row {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    border-radius: var(--radius-sm);
    padding: 2px;
  }

  .designer__unit-row--active {
    background-color: var(--color-primary-subtle);
  }

  .designer__unit-open {
    flex: 1;
    text-align: left;
    font-family: var(--font-mono, monospace);
    font-size: var(--text-sm);
    background: none;
    border: none;
    cursor: pointer;
    color: var(--color-text-primary);
  }

  .designer__unit-level {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .designer__unit-compiled {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    font-style: italic;
  }

  .designer__text {
    background-color: var(--color-bg-surface);
    overflow: hidden;
  }

  .designer__editor {
    width: 100%;
    height: 100%;
    padding: var(--space-3);
    border: none;
    outline: none;
    resize: none;
    font-family: var(--font-mono, monospace);
    font-size: var(--text-sm);
    line-height: 1.6;
    background-color: var(--color-bg-inset);
    color: var(--color-text-primary);
  }

  .designer__structure {
    padding: var(--space-3);
    background-color: var(--color-bg-surface);
    overflow-y: auto;
  }

  .designer__structure-title {
    font-size: var(--text-sm);
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--color-text-secondary);
    margin-bottom: var(--space-2);
  }

  .designer__structure-name {
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0 0 var(--space-1) 0;
  }

  .designer__structure-io {
    font-family: var(--font-mono, monospace);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    margin: 0 0 var(--space-1) 0;
  }

  .designer__structure-states {
    list-style: none;
    margin: var(--space-2) 0 0 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .designer__structure-state {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding-left: var(--space-2);
    border-left: 2px solid var(--color-border);
  }

  .designer__structure-state-id {
    font-family: var(--font-mono, monospace);
    font-size: var(--text-sm);
  }

  .designer__structure-call,
  .designer__structure-outcome {
    font-family: var(--font-mono, monospace);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .designer__structure-empty,
  .designer__units-empty {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .designer__preview {
    border-top: 1px solid var(--color-border);
    background-color: var(--color-bg-subtle);
    padding: var(--space-3) var(--space-6);
    max-height: 40vh;
    overflow-y: auto;
  }

  .designer__preview-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--space-2);
  }

  .designer__preview-title {
    font-size: var(--text-sm);
    font-weight: 700;
  }

  .designer__preview-nodes {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .designer__preview-node {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-1) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-surface);
    font-size: var(--text-xs);
  }

  .designer__preview-node-kind,
  .designer__preview-node-state {
    color: var(--color-text-muted);
  }

  @media (max-width: 900px) {
    .designer__workspace {
      grid-template-columns: 1fr;
      overflow-y: auto;
    }
  }
</style>
