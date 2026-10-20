<script lang="ts">
  import { t } from '../i18n';
  import Button from './Button.svelte';
  import { formatTokens } from '../utils/tokenFormat';
  import { currentLocale } from '../i18n';

  /**
   * ③ Models: the models plans may use, grouped by harness.
   *
   * Tokens, never money: every figure is a token count or a floor estimate, and no price or
   * cost of a model is ever shown.
   */
  interface ModelRow {
    harness_id: string;
    model_id: string;
    source?: string;
    recommended?: boolean;
    tokens?: number;
    tokens_floor?: boolean;
  }

  export let models: ModelRow[] = [];
  export let harnessNames: Record<string, string> = {};
  export let defaultHarness: string | undefined = undefined;
  export let recommendation: string | undefined = undefined;
  export let busy = false;

  export let onRecord: (allowed: ModelRow[]) => void = () => {};

  let selected: string[] = models.map((m) => `${m.harness_id}::${m.model_id}`);

  function key(row: ModelRow): string {
    return `${row.harness_id}::${row.model_id}`;
  }

  function toggle(row: ModelRow): void {
    const id = key(row);
    selected = selected.includes(id) ? selected.filter((v) => v !== id) : [...selected, id];
  }

  $: groups = models.reduce<Record<string, ModelRow[]>>((acc, row) => {
    (acc[row.harness_id] ??= []).push(row);
    return acc;
  }, {});

  $: allowed = models.filter((row) => selected.includes(key(row)));

  function tokensLabel(row: ModelRow): string {
    if (row.tokens === undefined) return '';
    return formatTokens(row.tokens, Boolean(row.tokens_floor), $currentLocale);
  }
</script>

<section class="choice-screen" data-testid="models-select-screen">
  {#if recommendation}
    <div class="recommendation" data-testid="model-recommendation">
      <h3 class="recommendation__title">{$t('onboard.catalogue.select.recommendation')}</h3>
      <p class="recommendation__text">{recommendation}</p>
    </div>
  {/if}

  {#each Object.entries(groups) as [harnessId, rows]}
    <div class="model-group" data-harness={harnessId}>
      <h3 class="choice-screen__label">
        {$t('onboard.catalogue.select.group', {
          harness: harnessNames[harnessId] ?? harnessId,
          mark:
            defaultHarness === harnessId ? `· ${$t('onboard.harness.choose.default_label')}` : '',
        })}
      </h3>
      <ul class="choice-screen__list">
        {#each rows as row}
          <li class="choice-screen__row">
            <input
              type="checkbox"
              checked={selected.includes(key(row))}
              aria-label={row.model_id}
              onchange={() => toggle(row)}
              data-testid={`model-toggle-${harnessId}-${row.model_id}`}
            />
            <div class="choice-screen__row-main">
              <span class="choice-screen__row-title mono">{row.model_id}</span>
              {#if tokensLabel(row)}
                <span class="choice-screen__row-meta">{tokensLabel(row)}</span>
              {/if}
            </div>
            {#if row.recommended}
              <span class="choice-screen__badge choice-screen__badge--default">
                {$t('onboard.catalogue.select.recommendation')}
              </span>
            {/if}
          </li>
        {/each}
      </ul>
    </div>
  {/each}

  <div class="choice-screen__actions">
    <Button
      variant="primary"
      disabled={allowed.length === 0}
      loading={busy}
      on:click={() => onRecord(allowed)}
      data-testid="models-record"
    >
      {$t('onboard.catalogue.select.action', { count: allowed.length })}
    </Button>
  </div>
</section>

<style>
  .recommendation {
    padding: var(--space-4);
    background-color: var(--color-primary-subtle);
    border: 1px solid var(--color-primary);
    border-radius: var(--radius-md);
  }

  .recommendation__title {
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-primary);
    margin-bottom: var(--space-1);
  }

  .recommendation__text {
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .model-group {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .recommendation {
    padding: var(--space-4);
    background-color: var(--color-primary-subtle);
    border: 1px solid var(--color-primary);
    border-radius: var(--radius-md);
  }

  .recommendation__title {
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-primary);
    margin-bottom: var(--space-1);
  }

  .recommendation__text {
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .model-group {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }
</style>
