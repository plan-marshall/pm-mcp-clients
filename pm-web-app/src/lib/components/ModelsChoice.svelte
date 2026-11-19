<script lang="ts" module>
  /**
   * ③ Models: the models plans may use, grouped by harness.
   *
   * Tokens, never money: every figure is a token count or a floor estimate, and no price or
   * cost of a model is ever shown.
   *
   * A model row is the entry `catalogue.select` publishes — the state catalogue's row
   * (`19-onboarding-aspects-models-providers.adoc:50-56`) and `ModelCatalogueEntry` (pm-api) name
   * a harness, a model id and the row's source (`probe` or `operator`), and nothing else. The
   * recommendation is the row's own word ("the recommendation preselected"), so a row may carry
   * `recommended`; no row and no `pm-api` type names an `allowed`, `reason`, `rationale` or
   * `harness_name` field, and this screen reads none of them.
   */
  export interface ModelRow {
    harness_id: string;
    model_id: string;
    source?: string;
    recommended?: boolean;
    tokens?: number;
    tokens_floor?: boolean;
  }

  /**
   * One enrolled harness as `HarnessScanResult` (pm-api) and the `harness.choose` row publish it
   * (state catalogue `14-machine-onboarding.adoc:301-307`): its id and its name. The screen reads
   * no `cannot_list`, `models_listed` or `display_name` field, because no row and no `pm-api` type
   * publishes one.
   */
  export interface ModelHarnessInfo {
    harness_id?: string;
    name?: string;
  }
</script>

<script lang="ts">
  import { t, currentLocale, humanizeKey, type Locale } from '../i18n';
  import { formatTokens } from '../display/format';
  import { proposalDelta } from './choiceProposal';
  import Button from './Button.svelte';
  import Marker from './Marker.svelte';
  import TextField from './TextField.svelte';

  export let models: readonly ModelRow[] = [];
  export let harnesses: readonly ModelHarnessInfo[] = [];
  export let harnessNames: Record<string, string> = {};
  export let defaultHarness: string | undefined = undefined;
  export let recommendation: string | undefined = undefined;
  export let busy = false;

  export let onRecord: (allowed: ModelRow[], modified: boolean) => void = () => {};

  /**
   * The identity of a model row: its harness and its model id. The engine compares its ids case-
   * insensitively, and the two parts are encoded rather than joined, so an id that itself carries
   * the separator can never collide with another harness's row.
   *
   * @param row a model row
   * @returns the row's identity
   */
  function key(row: Pick<ModelRow, 'harness_id' | 'model_id'>): string {
    return JSON.stringify([row.harness_id, row.model_id.trim().toLowerCase()]);
  }

  /**
   * @param rows the proposed rows
   * @returns the identities the proposal preselects: the recommended rows where the proposal marks
   *   any, otherwise every listed row (the catalogue row's "recommendation preselected", read
   *   without a per-row flag that no source publishes)
   */
  function initiallySelected(rows: readonly ModelRow[]): string[] {
    return rows.filter((row) => preselected(row, rows)).map(key);
  }

  /**
   * @param row one row of the proposal
   * @param rows the whole proposal
   * @returns whether the row is preselected
   */
  function preselected(row: ModelRow, rows: readonly ModelRow[]): boolean {
    if (rows.some((r) => typeof r.recommended === 'boolean')) {
      return row.recommended === true || (row.source === 'operator' && row.recommended !== false);
    }
    return true;
  }

  let extraModels: ModelRow[] = [];
  let removedKeys: string[] = [];
  let modified = false;
  let draftByHarness: Record<string, string> = {};
  let prevModels: readonly ModelRow[] = models;
  let selected: string[] = initiallySelected(models);
  let droppedEdits: string[] = [];
  let addNotice: { harnessId: string; key: string } | null = null;

  // A refresh keeps every edit the operator made (added ids, unticked rows, drafts) and resyncs
  // only what the changed proposal carries: a new row follows the proposal, a removed row takes
  // its edit with it and the screen says so.
  $: {
    const delta = proposalDelta(
      prevModels.map((row) => key(row)),
      models.map((row) => key(row)),
    );
    if (delta.added.length > 0 || delta.removed.length > 0) {
      syncProposal(delta.added, delta.removed);
    }
  }

  /**
   * @param added the identities the refreshed proposal adds
   * @param removed the identities it no longer carries
   */
  function syncProposal(added: readonly string[], removed: readonly string[]): void {
    const previous = prevModels;
    prevModels = models;
    const nextKeys = models.map((row) => key(row));
    const dropped = previous
      .filter((row) => removed.includes(key(row)))
      .filter((row) => !selected.includes(key(row)) || removedKeys.includes(key(row)))
      .map((row) => row.model_id);
    removedKeys = removedKeys.filter((id) => nextKeys.includes(id));
    const stillOffered = new Set([...nextKeys, ...extraModels.map((row) => key(row))]);
    selected = selected.filter((id) => stillOffered.has(id));
    for (const row of models) {
      const id = key(row);
      if (added.includes(id) && preselected(row, models) && !selected.includes(id)) {
        selected = [...selected, id];
      }
    }
    const named = new Set([
      ...models.map((row) => row.harness_id),
      ...harnesses.flatMap((info) => (info.harness_id ? [info.harness_id] : [])),
      ...Object.keys(harnessNames),
      ...(defaultHarness ? [defaultHarness] : []),
    ]);
    const draftsGone = Object.keys(draftByHarness).filter((harnessId) => !named.has(harnessId));
    if (draftsGone.length > 0) {
      const next = { ...draftByHarness };
      for (const harnessId of draftsGone) delete next[harnessId];
      draftByHarness = next;
    }
    droppedEdits = [...dropped, ...draftsGone];
  }

  $: allModels = [...models.filter((m) => !removedKeys.includes(key(m))), ...extraModels];

  $: groups = buildGroups(allModels, harnesses, models, harnessNames, defaultHarness);

  $: allowed = allModels.filter((row) => selected.includes(key(row)));

  /**
   * Builds one group per harness the state names — its own `harnesses[]`, the names the view
   * reports, the default harness, the initial rows and the rows — so a harness whose models the
   * operator must type gets its group and its field even when the state lists no row for it.
   *
   * @param rows the rows the screen shows
   * @param harnessInfos the state's enrolled harnesses
   * @param initialRows the proposal's own rows
   * @param names the harness names the view knows
   * @param defaultId the harness the state marks as the default
   * @returns the groups, in the order the state names them
   */
  function buildGroups(
    rows: readonly ModelRow[],
    harnessInfos: readonly ModelHarnessInfo[],
    initialRows: readonly ModelRow[],
    names: Record<string, string>,
    defaultId: string | undefined,
  ): Array<[string, ModelRow[]]> {
    const grouped: Record<string, ModelRow[]> = Object.create(null);
    const nameIt = (harnessId: string | undefined): void => {
      if (harnessId) grouped[harnessId] ??= [];
    };
    for (const info of harnessInfos) {
      nameIt(info.harness_id);
    }
    for (const row of initialRows) {
      nameIt(row.harness_id);
    }
    for (const row of rows) {
      (grouped[row.harness_id] ??= []).push(row);
    }
    for (const harnessId of Object.keys(names)) {
      nameIt(harnessId);
    }
    nameIt(defaultId);
    return Object.entries(grouped);
  }

  function toggle(row: ModelRow): void {
    const id = key(row);
    selected = selected.includes(id) ? selected.filter((v) => v !== id) : [...selected, id];
    modified = true;
  }

  function setDraft(harnessId: string, value: string): void {
    draftByHarness = { ...draftByHarness, [harnessId]: value };
    if (addNotice?.harnessId === harnessId) addNotice = null;
  }

  function addOperatorModel(harnessId: string): void {
    const modelId = (Object.hasOwn(draftByHarness, harnessId) ? draftByHarness[harnessId] : '').trim();
    if (!modelId) {
      addNotice = { harnessId, key: 'onboard.catalogue.select.add_empty' };
      return;
    }
    const id = key({ harness_id: harnessId, model_id: modelId });
    if (removedKeys.includes(id)) {
      removedKeys = removedKeys.filter((k) => k !== id);
    } else if (models.some((m) => key(m) === id) || extraModels.some((m) => key(m) === id)) {
      // The row is already offered; a second row of the same id would record it twice.
      addNotice = { harnessId, key: 'onboard.catalogue.select.add_duplicate' };
      return;
    } else {
      extraModels = [...extraModels, { harness_id: harnessId, model_id: modelId, source: 'operator' }];
    }
    if (!selected.includes(id)) {
      selected = [...selected, id];
    }
    draftByHarness = { ...draftByHarness, [harnessId]: '' };
    addNotice = null;
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
    const fromProp = harnesses.find((h) => h.harness_id === harnessId);
    if (fromProp?.name) return fromProp.name;
    if (Object.hasOwn(harnessNames, harnessId) && harnessNames[harnessId]) {
      return harnessNames[harnessId];
    }
    return humanizeKey(harnessId);
  }

  /**
   * Whether the harness lists no model of its own: the published `source` of its rows says so
   * (`probe` where the harness listed it, `operator` where it was typed in), so a group without a
   * probed row is the one the operator fills in.
   *
   * @param rows the rows of the group
   * @returns whether the group carries no probed row
   */
  function cannotList(rows: readonly ModelRow[]): boolean {
    return !rows.some((r) => r.source === 'probe');
  }

  function tokensLabel(row: ModelRow, locale: Locale, _translate?: unknown): string {
    if (row.tokens === undefined) return '';
    return formatTokens(row.tokens, Boolean(row.tokens_floor), locale);
  }
</script>

<section class="choice-screen" data-testid="models-select-screen">
  {#if recommendation}
    <div class="recommendation" data-testid="model-recommendation">
      <h3 class="recommendation__title">{$t('onboard.catalogue.select.recommendation')}</h3>
      <p class="recommendation__text">{recommendation}</p>
    </div>
  {/if}

  {#if droppedEdits.length > 0}
    <p class="choice-screen__note" role="status" data-testid="models-dropped">
      {$t('onboard.choice.edit_dropped', { rows: droppedEdits.join(', ') })}
    </p>
  {/if}

  {#each groups as [harnessId, rows] (harnessId)}
    {@const harnessLabel = resolveHarnessName(harnessId)}
    {@const listsNothing = cannotList(rows)}
    {@const draft = Object.hasOwn(draftByHarness, harnessId) ? draftByHarness[harnessId] : ''}
    <div
      class="model-group"
      role="group"
      aria-labelledby={`model-group-${harnessId}`}
      data-harness={harnessId}
    >
      <div class="model-group__header">
        <h3 class="choice-screen__label" id={`model-group-${harnessId}`}>{harnessLabel}</h3>
        {#if defaultHarness === harnessId}
          <Marker kind="default" label={harnessLabel} />
        {/if}
        {#if listsNothing}
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
            <li class="choice-screen__row">
              <input
                type="checkbox"
                checked={isChecked}
                aria-label={$t('onboard.catalogue.select.model_for', {
                  model: row.model_id,
                  harness: harnessLabel,
                })}
                onchange={() => toggle(row)}
                data-testid={`model-toggle-${harnessId}-${row.model_id}`}
              />
              <div class="choice-screen__row-main">
                <span class="choice-screen__row-title mono">{row.model_id}</span>
                {#if row.tokens !== undefined}
                  <span class="choice-screen__row-meta">
                    {tokensLabel(row, $currentLocale, $t)}
                  </span>
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
                    aria-label={$t('onboard.catalogue.select.remove_model_for', {
                      model: row.model_id,
                      harness: harnessLabel,
                    })}
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
      {/if}
      {#if !rows.some((r) => selected.includes(key(r)))}
        <p class="choice-screen__note" data-testid={`models-none-${harnessId}`}>
          {$t('onboard.catalogue.select.no_models')}
        </p>
      {/if}

      {#if listsNothing}
        <div class="model-group__add">
          <div class="model-group__add-field">
            <TextField
              mono
              label={$t('onboard.catalogue.select.model_id_prompt', {
                harness: harnessLabel,
              })}
              placeholder={$t('onboard.catalogue.select.model_id_placeholder')}
              value={draft}
              testid={`models-input-${harnessId}`}
              oninput={(e) => setDraft(harnessId, e.currentTarget.value)}
              onkeydown={(e) => {
                if (e.key === 'Enter' && !e.isComposing) {
                  e.preventDefault();
                  addOperatorModel(harnessId);
                }
              }}
            />
          </div>
          <Button
            variant="secondary"
            size="md"
            disabled={!draft.trim()}
            on:click={() => addOperatorModel(harnessId)}
            data-testid={`models-add-${harnessId}`}
          >
            {$t('onboard.catalogue.select.add_model')}
          </Button>
        </div>
        {#if addNotice?.harnessId === harnessId}
          <p
            class="choice-screen__note"
            role="status"
            data-testid={`models-add-notice-${harnessId}`}
          >
            {$t(addNotice.key)}
          </p>
        {/if}
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
