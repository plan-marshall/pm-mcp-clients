<script lang="ts">
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import { navigateTo } from '../stores/runtime';

  let codeText = `workflow custom-feature version 1 format 1
title "Custom Feature Workflow"
description "A tailored development workflow for feature implementation"

entry:
  phase-1-init
  phase-2-refine
  phase-3-outline
  phase-4-plan
  phase-5-execute
  phase-6-finalize
`;

  let unitName = 'custom-feature';
  let unitVersion = '1';
  let unitTitle = 'Custom Feature Workflow';
  let unitDesc = 'A tailored development workflow for feature implementation';

  let isValidated = false;
  let validationMessage = '';
  let validationError = false;

  interface BuildingBlock {
    id: string;
    label: string;
    snippet: string;
  }

  const buildingBlocks: BuildingBlock[] = [
    { id: 'gate', label: 'Quality Gate', snippet: 'gate quality-check:\n  verify -Pcoverage\n' },
    {
      id: 'llm',
      label: 'LLM Task',
      snippet: 'job llm:\n  role implementer\n  prompt "Execute task"\n',
    },
    { id: 'review', label: 'Review Loop', snippet: 'phase review-loop:\n  triage findings\n' },
  ];

  function insertBlock(snippet: string) {
    codeText += `\n${snippet}`;
  }

  function handleFileUpload(e: Event) {
    const input = e.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    const reader = new FileReader();
    reader.onload = (event) => {
      const content = event.target?.result;
      if (typeof content === 'string') {
        codeText = content;
        parseHeader(content);
      }
    };
    reader.readAsText(file);
  }

  function parseHeader(text: string) {
    const lines = text.split('\n');
    for (const line of lines) {
      const trimmed = line.trim();
      if (trimmed.startsWith('workflow ')) {
        const parts = trimmed.split(' ');
        if (parts[1]) unitName = parts[1];
        if (parts[3]) unitVersion = parts[3];
      } else if (trimmed.startsWith('title ')) {
        unitTitle = trimmed.replace(/^title\s+"?([^"]*)"?/, '$1');
      } else if (trimmed.startsWith('description ')) {
        unitDesc = trimmed.replace(/^description\s+"?([^"]*)"?/, '$1');
      }
    }
  }

  function validateProcess() {
    isValidated = true;
    const first = codeText.split('\n').map((l) => l.trim()).find((l) => l.length > 0) ?? '';
    if (!/^workflow\s+\S+\s+version\s+\S+\s+format\s+\S+$/.test(first)) {
      validationError = true;
      validationMessage = $t('designer.input.no_header');
    } else {
      validationError = false;
      validationMessage = $t('designer.validate.valid');
    }
  }
</script>

<div class="designer">
  <div class="designer__toolbar">
    <div class="designer__title-group">
      <h1 class="designer__title">{$t('designer.view.title')}</h1>
      <span class="mono designer__unit-badge">{unitName} v{unitVersion}</span>
    </div>
    <div class="designer__actions">
      <label class="file-upload-btn">
        {$t('designer.input.upload')}
        <input type="file" accept=".pmwf,.txt" on:change={handleFileUpload} />
      </label>
      <Button variant="primary" on:click={validateProcess}>
        {$t('designer.validate.action')}
      </Button>
      <Button variant="secondary" on:click={() => navigateTo('workspace')}>
        {$t('web.common.close')}
      </Button>
    </div>
  </div>

  {#if isValidated}
    <div class="validation-bar" class:validation-bar--error={validationError}>
      <span>{validationMessage}</span>
    </div>
  {/if}

  <div class="designer__workspace">
    <!-- Left panel: Building blocks and frontmatter -->
    <aside class="designer__sidebar">
      <div class="panel">
        <h2 class="panel__title">{$t('designer.frontmatter.title')}</h2>
        <div class="form-group">
          <label for="fm-name" class="label">{$t('designer.frontmatter.name')}</label>
          <input id="fm-name" type="text" bind:value={unitName} class="input mono" />
        </div>
        <div class="form-group">
          <label for="fm-ver" class="label">{$t('designer.frontmatter.version')}</label>
          <input id="fm-ver" type="text" bind:value={unitVersion} class="input mono" />
        </div>
        <div class="form-group">
          <label for="fm-title" class="label">{$t('designer.frontmatter.title_field')}</label>
          <input id="fm-title" type="text" bind:value={unitTitle} class="input" />
        </div>
        <div class="form-group">
          <label for="fm-desc" class="label">{$t('designer.frontmatter.description')}</label>
          <textarea id="fm-desc" bind:value={unitDesc} class="input textarea" rows="2"></textarea>
        </div>
      </div>

      <div class="panel">
        <h2 class="panel__title">{$t('designer.building_blocks.title')}</h2>
        <div class="blocks-list">
          {#each buildingBlocks as block}
            <button class="block-item" on:click={() => insertBlock(block.snippet)}>
              <span class="block-item__label">{block.label}</span>
              <span class="block-item__plus">+</span>
            </button>
          {/each}
        </div>
      </div>
    </aside>

    <!-- Center panel: Editor with line numbers -->
    <section class="designer__editor-panel">
      <div class="panel panel--full">
        <h2 class="panel__title">Workflow Source (.pmwf)</h2>
        <div class="editor-container">
          <textarea class="editor-textarea mono" bind:value={codeText} spellcheck="false"
          ></textarea>
        </div>
      </div>
    </section>

    <!-- Right panel: Structure and preview -->
    <aside class="designer__preview-panel">
      <div class="panel panel--full">
        <h2 class="panel__title">{$t('designer.structure.title')}</h2>
        <div class="structure-tree">
          <div class="tree-node tree-node--root">
            <span class="tree-node__icon">📦</span>
            <span class="tree-node__label">{unitName}</span>
          </div>
          <div class="tree-children">
            <div class="tree-node">
              <span class="tree-node__icon">🔹</span>
              <span class="tree-node__label">phase-1-init</span>
            </div>
            <div class="tree-node">
              <span class="tree-node__icon">🔹</span>
              <span class="tree-node__label">phase-2-refine</span>
            </div>
            <div class="tree-node">
              <span class="tree-node__icon">🔹</span>
              <span class="tree-node__label">phase-3-outline</span>
            </div>
            <div class="tree-node">
              <span class="tree-node__icon">🔹</span>
              <span class="tree-node__label">phase-4-plan</span>
            </div>
            <div class="tree-node">
              <span class="tree-node__icon">🔹</span>
              <span class="tree-node__label">phase-5-execute</span>
            </div>
            <div class="tree-node">
              <span class="tree-node__icon">🔹</span>
              <span class="tree-node__label">phase-6-finalize</span>
            </div>
          </div>
        </div>
      </div>
    </aside>
  </div>
</div>

<style>
  .designer {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background-color: var(--color-bg-app);
  }

  .designer__toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-3) var(--space-6);
    background-color: var(--color-bg-surface);
    border-bottom: 1px solid var(--color-border);
  }

  .designer__title-group {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  .designer__title {
    font-size: var(--text-lg);
    font-weight: 700;
  }

  .designer__unit-badge {
    padding: 2px 8px;
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
    border-radius: var(--radius-sm);
    font-size: var(--text-xs);
  }

  .designer__actions {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  .file-upload-btn {
    display: inline-flex;
    align-items: center;
    padding: var(--space-2) var(--space-4);
    font-size: var(--text-sm);
    font-weight: 500;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    cursor: pointer;
  }

  .file-upload-btn input {
    display: none;
  }

  .validation-bar {
    padding: var(--space-2) var(--space-6);
    background-color: var(--color-success-subtle);
    border-bottom: 1px solid var(--color-success-border);
    color: var(--color-success);
    font-size: var(--text-sm);
    font-weight: 500;
  }

  .validation-bar--error {
    background-color: var(--color-danger-subtle);
    border-bottom-color: var(--color-danger-border);
    color: var(--color-danger);
  }

  .designer__workspace {
    flex: 1;
    display: grid;
    grid-template-columns: 280px 1fr 300px;
    gap: 1px;
    background-color: var(--color-border);
    overflow: hidden;
  }

  .panel {
    background-color: var(--color-bg-surface);
    padding: var(--space-4);
    margin-bottom: var(--space-4);
    border-radius: var(--radius-md);
  }

  .panel--full {
    height: 100%;
    margin-bottom: 0;
    border-radius: 0;
    display: flex;
    flex-direction: column;
  }

  .panel__title {
    font-size: var(--text-sm);
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--color-text-secondary);
    margin-bottom: var(--space-3);
  }

  .designer__sidebar,
  .designer__editor-panel,
  .designer__preview-panel {
    background-color: var(--color-bg-surface);
    overflow-y: auto;
  }

  .form-group {
    margin-bottom: var(--space-3);
  }

  .label {
    display: block;
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-secondary);
    margin-bottom: var(--space-1);
  }

  .input {
    width: 100%;
    padding: var(--space-2);
    font-size: var(--text-sm);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .textarea {
    resize: vertical;
  }

  .blocks-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .block-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-2) var(--space-3);
    background-color: var(--color-bg-subtle);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-sm);
    font-size: var(--text-sm);
    text-align: left;
    transition: background-color 0.1s;
  }

  .block-item:hover {
    background-color: var(--color-bg-inset);
  }

  .block-item__plus {
    font-weight: bold;
    color: var(--color-primary);
  }

  .editor-container {
    flex: 1;
    display: flex;
  }

  .editor-textarea {
    flex: 1;
    width: 100%;
    height: 100%;
    padding: var(--space-3);
    border: 1px solid var(--color-border-subtle);
    background-color: var(--color-bg-inset);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    line-height: 1.6;
    resize: none;
    outline: none;
  }

  .structure-tree {
    font-size: var(--text-sm);
  }

  .tree-node {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-sm);
  }

  .tree-node--root {
    font-weight: 600;
  }

  .tree-children {
    padding-left: var(--space-4);
    border-left: 1px dashed var(--color-border);
    margin-left: var(--space-2);
    margin-top: var(--space-1);
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  @media (max-width: 900px) {
    .designer__workspace {
      grid-template-columns: 1fr;
    }
  }
</style>
