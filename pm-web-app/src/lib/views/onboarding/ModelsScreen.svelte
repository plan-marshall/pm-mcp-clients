<script lang="ts">
  import ModelsChoice, { type ModelHarnessInfo, type ModelRow } from '../../components/ModelsChoice.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** The models a plan may use (`catalogue.select`, `catalogue.reconfigure`). */
  let { view }: ScreenProps = $props();

  let recommendationText = $derived.by(() => {
    const direct = view.field('recommendation') ?? view.field('reason') ?? view.field('rationale');
    if (direct) return direct;
    const raw = view.state?.recommendation ?? view.state?.advice;
    if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
      const rec = raw as Record<string, unknown>;
      for (const key of ['rationale', 'reason', 'summary']) {
        if (typeof rec[key] === 'string' && rec[key]) return rec[key] as string;
      }
    }
    return undefined;
  });
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
    noAdviceReason={view.field('no_advice_reason')}
    busy={view.busy}
    onRecord={(allowed, modified) => {
      const target = view.linkBy('set', 'record') ?? view.links[view.links.length - 1];
      if (target) view.onLink(target.id, modified ? { models: allowed } : {});
    }}
  />
</div>
