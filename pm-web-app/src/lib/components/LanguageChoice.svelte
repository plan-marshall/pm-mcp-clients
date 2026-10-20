<script lang="ts">
  import { t } from '../i18n';
  import Button from './Button.svelte';

  /**
   * ① Language: the interface language and the model language, one choice each.
   *
   * The screen stays in the language it was shown in until the choice is recorded; the
   * primary button names the two languages it records.
   */
  interface LanguageChoice {
    interface: string;
    model: string;
    systemLocale?: string;
  }

  export let languages: string[] = ['en', 'de'];
  export let current: LanguageChoice = { interface: 'en', model: 'same' };
  export let busy = false;

  export let onRecord: (choice: LanguageChoice) => void = () => {};

  let iface = current.interface || 'en';
  let model = current.model || 'same';

  function languageName(code: string): string {
    return code === 'de' ? 'Deutsch' : 'English';
  }

  $: primary =
    model === 'same'
      ? $t('onboard.language.record_named', { language: languageName(iface) })
      : $t('onboard.language.record_split', {
          interface: languageName(iface),
          model: languageName(model),
        });
</script>

<section class="choice-screen" data-testid="language-screen">
  <div class="choice-screen__group">
    <h3 class="choice-screen__label">{$t('onboard.language.interface_label')}</h3>
    <div class="choice-screen__options">
      {#each languages as language}
        <label class="choice-screen__option">
          <input
            type="radio"
            name="interface-language"
            value={language}
            checked={iface === language}
            onchange={() => (iface = language)}
          />
          <span>{languageName(language)}</span>
        </label>
      {/each}
    </div>
    {#if current.systemLocale}
      <p class="choice-screen__hint">
        {$t('onboard.language.system', { locale: current.systemLocale })}
      </p>
    {/if}
  </div>

  <div class="choice-screen__group">
    <h3 class="choice-screen__label">{$t('onboard.language.model_label')}</h3>
    <div class="choice-screen__options">
      <label class="choice-screen__option">
        <input
          type="radio"
          name="model-language"
          value="same"
          checked={model === 'same'}
          onchange={() => (model = 'same')}
        />
        <span>{$t('onboard.language.same')}</span>
      </label>
      {#each languages as language}
        <label class="choice-screen__option">
          <input
            type="radio"
            name="model-language"
            value={language}
            checked={model === language}
            onchange={() => (model = language)}
          />
          <span>{languageName(language)}</span>
        </label>
      {/each}
    </div>
  </div>

  <p class="choice-screen__note">{$t('onboard.language.stay')}</p>

  <div class="choice-screen__actions">
    <Button
      variant="primary"
      loading={busy}
      on:click={() => onRecord({ interface: iface, model })}
      data-testid="language-record"
    >
      {primary}
    </Button>
  </div>
</section>
