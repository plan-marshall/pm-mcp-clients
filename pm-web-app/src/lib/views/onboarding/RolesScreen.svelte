<script lang="ts">
  import AspectChoice from '../../components/AspectChoice.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** The role assignment (`roles.review`, `roles.reconfigure`). */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.roles.review.title')}
    </h1>
  </div>
  <AspectChoice
    kind="roles"
    roles={view.arrayField('roles')}
    busy={view.busy}
    onRecord={() => {
      const target = view.linkBy('accept', 'record') ?? view.links[view.links.length - 1];
      if (target) view.onLink(target.id);
    }}
  />
</div>
