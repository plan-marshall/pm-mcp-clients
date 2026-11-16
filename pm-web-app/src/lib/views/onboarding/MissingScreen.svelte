<script lang="ts">
  import Button from '../../components/Button.svelte';
  import Icon from '../../components/Icon.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * The fallback for a state no screen of the web app claims (UX-P-08 error pattern; PLAN-64
   * deliverable 2): it says the screen is not available here, names the terminal command that
   * shows the state, and offers a way back. It never prints the state's id as content.
   */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card missing-screen" data-testid="missing-screen" role="alert">
  <div class="state-card__header">
    <span class="missing-screen__mark" aria-hidden="true"
      ><Icon name="state.error" size={24} /></span
    >
    <h1 class="state-card__title">{$t('onboard.missing_screen.title')}</h1>
    <p class="state-card__summary">
      {$t('onboard.missing_screen.body', { command: $t('onboard.missing_screen.command') })}
    </p>
  </div>
  <p class="missing-screen__command">
    <code>$ {$t('onboard.missing_screen.command')}</code>
  </p>
  <div class="state-card__actions">
    <Button variant="secondary" on:click={view.onBack}>
      {$t('onboard.missing_screen.back')}
    </Button>
  </div>
</div>

<style>
  .missing-screen__mark {
    display: inline-flex;
    margin-bottom: var(--space-3);
    color: var(--color-danger);
  }

  .missing-screen__command {
    margin: 0;
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    font-family: var(--font-mono);
    font-size: var(--text-sm);
    overflow-wrap: anywhere;
  }
</style>
