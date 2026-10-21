<script lang="ts">
  import { t } from '../i18n';
  import Button from './Button.svelte';
  import type { WorkflowValidationResult } from '../api/client';

  export let result: WorkflowValidationResult | null = null;
  export let projectLabel = '';
  export let outdated = false;
  export let onPreview: () => void = () => {};
  export let onSelectLine: (line: number) => void = () => {};

  function statusGlyph(status: string): string {
    if (status === 'passed') return '✓';
    if (status === 'failed') return '✗';
    return '–';
  }

  function statusLabel(status: string): string {
    return status === 'not_run' ? $t('designer.validate.not_run') : '';
  }
</script>

<div class="validation" data-testid="validation-results">
  {#if result}
    <ul class="validation__stages" data-testid="validation-stages">
      {#each result.stages as stage (stage.name)}
        <li
          class="validation__stage validation__stage--{stage.status}"
          data-testid={`validation-stage-${stage.name}`}
        >
          <span class="validation__glyph" aria-hidden="true">{statusGlyph(stage.status)}</span>
          <span class="validation__stage-name">
            {$t('designer.validate.stage', { stage: stage.name, project: projectLabel })}
          </span>
          {#if statusLabel(stage.status)}
            <span class="validation__stage-note">{statusLabel(stage.status)}</span>
          {/if}
        </li>
      {/each}
    </ul>

    {#if outdated}
      <p class="validation__outdated" data-testid="validation-outdated">
        {$t('designer.validate.outdated')}
      </p>
    {:else if result.valid}
      <div class="validation__valid" data-testid="validation-valid">
        <span class="validation__glyph" aria-hidden="true">✓</span>
        <span>{$t('designer.validate.valid')}</span>
        {#if result.composed}
          <Button variant="ghost" on:click={onPreview} data-testid="preview-button">
            {$t('designer.preview.open')}
          </Button>
        {/if}
      </div>
    {:else}
      <p class="validation__summary" data-testid="validation-summary">
        {$t('designer.validate.findings', { count: result.findings.length })}
      </p>
      <ul class="validation__findings" data-testid="validation-findings">
        {#each result.findings as finding, index (index)}
          <li class="validation__finding" data-testid={`finding-${index}`}>
            <button
              type="button"
              class="validation__finding-button"
              onclick={() => onSelectLine(finding.line)}
            >
              {$t('designer.validate.finding', {
                rule: finding.rule,
                line: finding.line,
                column: finding.column,
                message: finding.message,
              })}
            </button>
            {#if finding.project}
              <span class="validation__finding-project">{finding.project}</span>
            {/if}
          </li>
        {/each}
      </ul>
    {/if}
  {/if}
</div>

<style>
  .validation {
    border-top: 1px solid var(--color-border);
    background-color: var(--color-bg-surface);
    padding: var(--space-2) var(--space-4);
    min-height: 2.5rem;
  }

  .validation__stages {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-4);
  }

  .validation__stage {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
  }

  .validation__stage--failed {
    color: var(--color-danger);
  }

  .validation__stage--not_run {
    color: var(--color-text-muted);
  }

  .validation__glyph {
    font-weight: 700;
  }

  .validation__stage-note {
    color: var(--color-text-muted);
    font-size: var(--text-xs);
  }

  .validation__valid {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    margin-top: var(--space-2);
    color: var(--color-success);
    font-size: var(--text-sm);
    font-weight: 500;
  }

  .validation__outdated {
    margin: var(--space-2) 0 0 0;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .validation__summary {
    margin: var(--space-2) 0 0 0;
    color: var(--color-danger);
    font-size: var(--text-sm);
    font-weight: 500;
  }

  .validation__findings {
    list-style: none;
    margin: var(--space-1) 0 0 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .validation__finding {
    font-size: var(--text-sm);
    color: var(--color-danger);
  }

  .validation__finding-button {
    background: none;
    border: none;
    padding: 0;
    font: inherit;
    color: inherit;
    text-align: left;
    text-decoration: underline;
    cursor: pointer;
  }

  .validation__finding-project {
    margin-left: var(--space-2);
    color: var(--color-text-muted);
  }
</style>
