<script lang="ts">
  import HarnessChoice from '../../components/HarnessChoice.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * The harness choice screen (`harness.choose`, its not-logged-in and all-failed variants, and
   * `harness.reconfigure`): every found harness with what it reports of its tokens.
   */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.harness.choose.title')}
    </h1>
  </div>
  <HarnessChoice
    harnesses={view.arrayField('harnesses')}
    busy={view.busy}
    onCheck={() => {
      const target = view.linkBy('choose', 'check') ?? view.links[view.links.length - 1];
      if (target) view.onLink(target.id);
    }}
  />
</div>
