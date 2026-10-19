<script lang="ts">
  import { t } from '../i18n';
  import Button from './Button.svelte';

  /**
   * The one consent pattern of the onboarding (shared with the terminal client).
   *
   * It adds nothing the server did not state: each field is shown only when the consent kind
   * states it, the remember choice is a choice inside the screen (so the primary button stays one
   * action), and a consent that is never remembered says so instead of offering the choice.
   */
  export let title: string;
  export let looksFor: string | undefined = undefined;
  export let inDir: string | undefined = undefined;
  export let runs: string | undefined = undefined;
  export let never: string | undefined = undefined;
  export let tokens: string | undefined = undefined;
  export let canRemember = true;
  export let primaryLabel: string | undefined = undefined;
  export let declineLabel: string | undefined = undefined;
  export let busy = false;

  export let onAccept: (remember: boolean) => void = () => {};
  export let onDecline: () => void = () => {};

  let remember = false;
</script>

<section class="consent-box" data-testid="consent-box">
  <h2 class="consent-box__title">{title}</h2>

  <dl class="consent-box__fields">
    {#if looksFor}
      <div class="consent-box__row">
        <dt class="consent-box__label">{$t('common.consent.field.looks_for')}</dt>
        <dd class="consent-box__value">{looksFor}</dd>
      </div>
    {/if}
    {#if inDir}
      <div class="consent-box__row">
        <dt class="consent-box__label">{$t('common.consent.field.in')}</dt>
        <dd class="consent-box__value mono">{inDir}</dd>
      </div>
    {/if}
    {#if runs}
      <div class="consent-box__row">
        <dt class="consent-box__label">{$t('common.consent.field.runs')}</dt>
        <dd class="consent-box__value mono">{runs}</dd>
      </div>
    {/if}
    {#if never}
      <div class="consent-box__row">
        <dt class="consent-box__label">{$t('common.consent.field.never')}</dt>
        <dd class="consent-box__value">{never}</dd>
      </div>
    {/if}
    {#if tokens}
      <div class="consent-box__row">
        <dt class="consent-box__label">{$t('common.consent.field.tokens')}</dt>
        <dd class="consent-box__value">{tokens}</dd>
      </div>
    {/if}
  </dl>

  <fieldset class="consent-box__choice">
    <legend class="sr-only">{$t('common.consent.yes')}</legend>
    <label class="consent-box__option">
      <input
        type="radio"
        name="consent-remember"
        checked={!remember}
        onchange={() => (remember = false)}
      />
      <span>{$t('common.consent.yes')}</span>
    </label>
    {#if canRemember}
      <label class="consent-box__option">
        <input
          type="radio"
          name="consent-remember"
          checked={remember}
          onchange={() => (remember = true)}
          data-testid="consent-remember"
        />
        <span>{$t('common.consent.yes_remember')}</span>
        <span class="consent-box__hint">{$t('common.consent.withdraw_hint')}</span>
      </label>
    {:else}
      <p class="consent-box__once">{$t('common.consent.once_only')}</p>
    {/if}
  </fieldset>

  <div class="consent-box__actions">
    <Button variant="ghost" on:click={onDecline} disabled={busy}>
      {declineLabel ?? $t('common.consent.no')}
    </Button>
    <Button variant="primary" on:click={() => onAccept(remember)} loading={busy}>
      {primaryLabel ?? $t('common.consent.yes')}
    </Button>
  </div>
</section>

<style>
  .consent-box {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-surface);
    padding: var(--space-6);
    box-shadow: var(--shadow-sm);
  }

  .consent-box__title {
    font-size: var(--text-lg);
    font-weight: 600;
    margin-bottom: var(--space-4);
    color: var(--color-text-primary);
  }

  .consent-box__fields {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    margin-bottom: var(--space-5);
  }

  .consent-box__row {
    display: grid;
    grid-template-columns: 8rem 1fr;
    gap: var(--space-4);
    font-size: var(--text-sm);
  }

  .consent-box__label {
    font-weight: 500;
    color: var(--color-text-secondary);
  }

  .consent-box__value {
    color: var(--color-text-primary);
    overflow-wrap: anywhere;
  }

  .consent-box__choice {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    border: none;
    margin-bottom: var(--space-5);
  }

  .consent-box__option {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .consent-box__hint {
    color: var(--color-text-muted);
    font-size: var(--text-xs);
  }

  .consent-box__once {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }

  .consent-box__actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-4);
    gap: var(--space-3);
  }

  @media (max-width: 768px) {
    .consent-box {
      padding: var(--space-4);
    }

    .consent-box__row {
      grid-template-columns: 1fr;
      gap: var(--space-1);
    }

    .consent-box__actions {
      flex-direction: column-reverse;
      align-items: stretch;
    }
  }
</style>
