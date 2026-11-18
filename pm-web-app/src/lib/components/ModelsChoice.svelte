<script lang="ts" module>
  /**
   * ③ Models: the models plans may use, grouped by harness.
   *
   * Tokens, never money: every figure is a token count or a floor estimate, and no price or
   * cost of a model is ever shown.
   */
  export interface ModelRow {
    harness_id: string;
    model_id: string;
    source?: string;
    recommended?: boolean;
    allowed?: boolean;
    reason?: string;
    rationale?: string;
    tokens?: number;
    tokens_floor?: boolean;
  }

  export interface ModelHarnessInfo {
    harness_id?: string;
    name?: string;
    cannot_list?: boolean;
    models_listed?: boolean;
    reason?: string;
  }
</script>

<script lang="ts">
  import { t, currentLocale, humanizeKey } from '../i18n';
  import { formatTokens } from '../display/format';
  import Button from './Button.svelte';
  import Marker from './Marker.svelte';
  import TextField from './TextField.svelte';

  const KNOWN_HARNESS_NAMES: Readonly<Record<string, string>> = {
    claude: 'Claude Code',
    opencode: 'OpenCode',
    codex: 'Codex',
    gemini: 'Gemini',
    agy: 'Antigravity',
    antigravity: 'Antigravity',
  };

  export let models: ModelRow[] = [];
  export let harnesses: ModelHarnessInfo[] = [];
  export let harnessNames: Record<string, string> = {};
  export let defaultHarness: string | undefined = undefined;
  export let recommendation: string | undefined = undefined;
  export let noAdviceReason: string | undefined = undefined;
  export let busy = false;

  export let onRecord: (allowed: ModelRow[], modified: boolean) => void = () => {};

  function key(row: Pick<ModelRow, 'harness_id' | 'model_id'>): string {
    return `${row.harness_id}::${row.model_id}`;
  }

  function hasRecommendationFilter(rows: ModelRow[], rec?: string): boolean {
    return (
      Boolean(rec) ||
      rows.some(
        (r) =>
          r.recommended === false ||
          r.allowed !== undefined ||
          Boolean(r.reason) ||
          Boolean(r.rationale),
      )
    );
  }

  function initiallySelected(rows: ModelRow[], rec?: string): string[] {
    if (rows.some((r) => r.allowed !== undefined)) {
      return rows.filter((r) => r.allowed !== false).map(key);
    }
    if (hasRecommendationFilter(rows, rec)) {
      return rows
        .filter((r) => r.recommended === true || (r.source === 'operator' && r.recommended !== false))
        .map(key);
    }
    return rows.map(key);
  }

  let extraModels: ModelRow[] = [];
  let removedKeys: string[] = [];
  let modified = false;
  let draftByHarness: Record<string, string> = {};
  let prevModels = models;
  let prevRecommendation = recommendation;
  let selected: string[] = initiallySelected(models, recommendation);

  $: if (models !== prevModels || recommendation !== prevRecommendation) {
    prevModels = models;
    prevRecommendation = recommendation;
    extraModels = [];
    removedKeys = [];
    modified = false;
    selected = initiallySelected(models, recommendation);
  }

  $: allModels = [...models.filter((m) => !removedKeys.includes(key(m))), ...extraModels];

  $: groups = buildGroups(allModels, harnesses, models);

  $: allowed = allModels.filter((row) => selected.includes(key(row)));

  function buildGroups(
    rows: ModelRow[],
    harnessInfos: ModelHarnessInfo[],
    initialRows: ModelRow[],
  ): Array<[string, ModelRow[]]> {
    const grouped: Record<string, ModelRow[]> = {};
    // Keep initial harnesses even if all their models are later removed.
    for (const row of initialRows) {
      grouped[row.harness_id] ??= [];
    }
    for (const row of rows) {
      (grouped[row.harness_id] ??= []).push(row);
    }
    for (const info of harnessInfos) {
      if (info.harness_id && (info.cannot_list || info.models_listed === false)) {
        grouped[info.harness_id] ??= [];
      }
    }
    return Object.entries(grouped);
  }

  function toggle(row: ModelRow): void {
    const id = key(row);
    selected = selected.includes(id) ? selected.filter((v) => v !== id) : [...selected, id];
    modified = true;
  }

  function setDraft(harnessId: string, value: string): void {
    draftByHarness = { ...draftByHarness, [harnessId]: value };
  }

  function addOperatorModel(harnessId: string): void {
    const modelId = (draftByHarness[harnessId] ?? '').trim();
    if (!modelId) return;
    const id = key({ harness_id: harnessId, model_id: modelId });
    // Inspect incoming `models` and `extraModels` directly rather than deferred `$: allModels`
    // so re-adding a removed initial model does not create a duplicate row.
    if (models.some((m) => key(m) === id)) {
      removedKeys = removedKeys.filter((k) => k !== id);
    } else if (!extraModels.some((m) => key(m) === id)) {
      extraModels = [...extraModels, { harness_id: harnessId, model_id: modelId, source: 'operator' }];
    }
    if (!selected.includes(id)) {
      selected = [...selected, id];
    }
    draftByHarness = { ...draftByHarness, [harnessId]: '' };
    modified = true;
  }

  function removeOperatorModel(row: ModelRow): void {
    const id = key(row);
    selected = selected.filter((v) => v !== id);
    if (extraModels.some((m) => key(m) === id)) {
      extraModels = extraModels.filter((m) => key(m) !== id);
    } else if (!removedKeys.includes(id)) {
      removedKeys = [...removedKeys, id];
    }
    modified = true;
  }

  function resolveHarnessName(harnessId: string): string {
    if (harnessNames[harnessId]) return harnessNames[harnessId];
    const fromProp = harnesses.find((h) => h.harness_id === harnessId)?.name;
    if (fromProp) return fromProp;
    return KNOWN_HARNESS_NAMES[harnessId] ?? humanizeKey(harnessId);
  }

  function cannotList(harnessId: string, rows: ModelRow[]): boolean {
    const info = harnesses.find((h) => h.harness_id === harnessId);
    if (info?.cannot_list !== undefined) return info.cannot_list;
    if (info?.models_listed !== undefined) return !info.models_listed;
    const initialForHarness = models.filter((m) => m.harness_id === harnessId);
    if (initialForHarness.length > 0) {
      return !initialForHarness.some((r) => r.source === 'probe');
    }
    return !rows.some((r) => r.source === 'probe');
  }

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
  {:else if noAdviceReason}
    <p class="choice-screen__note" data-testid="model-no-advice">
      {$t('onboard.catalogue.models.no_advice', { reason: noAdviceReason })}
    </p>
  {/if}

  {#each groups as [harnessId, rows] (harnessId)}
    <div class="model-group" data-harness={harnessId}>
      <div class="model-group__header">
        <h3 class="choice-screen__label">{resolveHarnessName(harnessId)}</h3>
        {#if defaultHarness === harnessId}
          <Marker kind="default" label={resolveHarnessName(harnessId)} />
        {/if}
        {#if cannotList(harnessId, rows)}
          <span class="choice-screen__row-meta" data-testid={`models-cannot-list-${harnessId}`}>
            {$t('onboard.catalogue.models.cannot_list')}
          </span>
        {/if}
      </div>

      {#if rows.length > 0}
        <ul class="choice-screen__list">
          {#each rows as row (key(row))}
            {@const rowKey = key(row)}
            {@const isChecked = selected.includes(rowKey)}
            {@const rowReason = row.reason ?? row.rationale}
            <li class="choice-screen__row">
              <input
                type="checkbox"
                checked={isChecked}
                aria-label={row.model_id}
                onchange={() => toggle(row)}
                data-testid={`model-toggle-${harnessId}-${row.model_id}`}
              />
              <div class="choice-screen__row-main">
                <span class="choice-screen__row-title mono">{row.model_id}</span>
                {#if rowReason}
                  <span class="choice-screen__row-meta">{rowReason}</span>
                {/if}
                {#if tokensLabel(row)}
                  <span class="choice-screen__row-meta">{tokensLabel(row)}</span>
                {/if}
              </div>
              <div class="choice-screen__row-side">
                {#if row.recommended && isChecked}
                  <Marker kind="recommended" label={row.model_id} />
                {/if}
                {#if row.source === 'operator'}
                  <Button
                    variant="ghost"
                    size="sm"
                    on:click={() => removeOperatorModel(row)}
                    data-testid={`model-remove-${harnessId}-${row.model_id}`}
                  >
                    {$t('onboard.catalogue.select.remove_model')}
                  </Button>
                {/if}
              </div>
            </li>
          {/each}
        </ul>
      {:else}
        <p class="choice-screen__note">{$t('onboard.catalogue.select.no_models')}</p>
      {/if}

      {#if cannotList(harnessId, rows)}
        <div class="model-group__add">
          <div class="model-group__add-field">
            <TextField
              mono
              label={$t('onboard.catalogue.select.model_id_prompt', {
                harness: resolveHarnessName(harnessId),
              })}
              placeholder={$t('onboard.catalogue.select.model_id_placeholder')}
              value={draftByHarness[harnessId] ?? ''}
              testid={`models-input-${harnessId}`}
              oninput={(e) => setDraft(harnessId, e.currentTarget.value)}
              onkeydown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  addOperatorModel(harnessId);
                }
              }}
            />
          </div>
          <Button
            variant="secondary"
            size="md"
            disabled={!(draftByHarness[harnessId] ?? '').trim()}
            on:click={() => addOperatorModel(harnessId)}
            data-testid={`models-add-${harnessId}`}
          >
            {$t('onboard.catalogue.select.add_model')}
          </Button>
        </div>
      {/if}
    </div>
  {/each}

  <div class="choice-screen__actions">
    <Button
      variant="primary"
      disabled={allowed.length === 0}
      loading={busy}
      on:click={() => onRecord(allowed, modified)}
      data-testid="models-record"
    >
      {$t('onboard.catalogue.select.action', { count: allowed.length })}
    </Button>
  </div>
</section>

<style>
  .recommendation {
    padding: var(--space-4);
    background-color: var(--color-brand-subtle);
    border: 1px solid var(--color-brand);
    border-radius: var(--radius-md);
  }

  .recommendation__title {
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-brand);
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

  .model-group__header {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .model-group__add {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    gap: var(--space-2);
    margin-top: var(--space-1);
  }

  .model-group__add-field {
    flex: 1 1 16rem;
    min-width: 0;
  }
</style>
