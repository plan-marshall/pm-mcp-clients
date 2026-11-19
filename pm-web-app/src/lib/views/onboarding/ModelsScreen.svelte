<script lang="ts">
  import ModelsChoice, { type ModelHarnessInfo, type ModelRow } from '../../components/ModelsChoice.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** The models a plan may use (`catalogue.select`, `catalogue.reconfigure`). */
  let { view }: ScreenProps = $props();

  // The row names the recommendation ("the recommendation preselected"), and nothing more: the
  // screen reads no `reason`, `rationale` or advice object a state might carry, because no row and
  // no pm-api type publishes them.
  let recommendationText = $derived(view.field('recommendation'));

  /**
   * The recorded entry is the one `catalogue.record` states (`19-…:58-64`) and
   * `ModelCatalogueEntry` (pm-api) types: the harness, the model id and the row's source. The
   * screen's own marks and token figures never travel to the runtime.
   *
   * @param allowed the rows the operator allowed
   * @returns the `models` body the state's link records
   */
  function recordedModels(allowed: readonly ModelRow[]): Array<Record<string, string>> {
    return allowed.map((row) => ({
      harness_id: row.harness_id,
      model_id: row.model_id,
      source: row.source ?? 'operator',
    }));
  }
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.catalogue.select.title')}
    </h1>
  </div>
  <ModelsChoice
    models={view.arrayField<ModelRow>('models')}
    harnesses={view.arrayField<ModelHarnessInfo>('harnesses')}
    harnessNames={view.harnessNames()}
    defaultHarness={view.field('default')}
    recommendation={recommendationText}
    busy={view.busy}
    onRecord={(allowed, modified) => {
      const target = view.linkBy('set', 'record') ?? view.links[view.links.length - 1];
      if (target) view.onLink(target.id, modified ? { models: recordedModels(allowed) } : {});
    }}
  />
</div>
