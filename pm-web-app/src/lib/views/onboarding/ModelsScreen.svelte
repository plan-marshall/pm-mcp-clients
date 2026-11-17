<script lang="ts">
  import ModelsChoice from '../../components/ModelsChoice.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** The models a plan may use (`catalogue.select`, `catalogue.reconfigure`). */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.catalogue.select.title')}
    </h1>
  </div>
  <ModelsChoice
    models={view.arrayField('models')}
    harnessNames={view.harnessNames()}
    busy={view.busy}
    onRecord={() => {
      const target = view.linkBy('set', 'record') ?? view.links[view.links.length - 1];
      if (target) view.onLink(target.id);
    }}
  />
</div>
