<script lang="ts">
  import LanguageChoice from '../../components/LanguageChoice.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** The language choice (`language.choose`, `language.reconfigure`). */
  let { view }: ScreenProps = $props();

  /** Records both languages: the model language follows the interface one when it is "same". */
  function record(choice: { interface: string; model: string }): void {
    const target = view.linkBy('set', 'record') ?? view.links[view.links.length - 1];
    if (!target) return;
    view.onLink(target.id, {
      language_interface: choice.interface,
      language_model: choice.model === 'same' ? choice.interface : choice.model,
    });
  }
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.language.choose.title')}
    </h1>
    <p class="state-card__summary">{$t('onboard.language.choose.intro')}</p>
  </div>
  <LanguageChoice
    current={{
      interface: view.field('interface') ?? 'en',
      model: view.field('model') ?? 'same',
    }}
    busy={view.busy}
    onRecord={record}
  />
</div>
