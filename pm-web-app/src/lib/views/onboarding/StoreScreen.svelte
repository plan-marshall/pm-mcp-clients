<script lang="ts">
  import AspectChoice from '../../components/AspectChoice.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** Where the machine keeps its tokens (`store.choose`, `store.reconfigure`). */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.store.choose.title')}
    </h1>
  </div>
  <AspectChoice
    kind="store"
    stores={view.arrayField('stores')}
    busy={view.busy}
    onRecord={(choice) => {
      const target = view.linkBy(String(choice), 'os', 'file') ?? view.links[0];
      if (target) view.onLink(target.id);
    }}
  />
</div>
