<script lang="ts">
  import AspectChoice from '../../components/AspectChoice.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** What the machine works with (`providers.select`, `providers.reconfigure`). */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.providers.select.title')}
    </h1>
  </div>
  <AspectChoice
    kind="providers"
    providers={view.arrayField('providers')}
    busy={view.busy}
    onRecord={() => {
      const target = view.linkBy('set', 'record') ?? view.links[view.links.length - 1];
      if (target) view.onLink(target.id);
    }}
  />
</div>
