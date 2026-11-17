<script lang="ts">
  import AspectChoice from '../../components/AspectChoice.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** How plans run on this machine (`defaults.review`, `defaults.reconfigure`). */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.defaults.review.title')}
    </h1>
  </div>
  <AspectChoice
    kind="defaults"
    planDefaults={{
      landing: view.field('landing') ?? '',
      method: view.field('method') ?? '',
      asking: view.field('asking') ?? '',
      posture: view.field('posture'),
    }}
    busy={view.busy}
    onRecord={() => {
      const target = view.linkBy('accept', 'set') ?? view.links[view.links.length - 1];
      if (target) view.onLink(target.id);
    }}
  />
</div>
