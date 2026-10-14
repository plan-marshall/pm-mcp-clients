<script lang="ts">
  import { t } from '../i18n';
  import Button from './Button.svelte';

  export let title: string;
  export let looksFor: string | undefined = undefined;
  export let inDir: string | undefined = undefined;
  export let runs: string | undefined = undefined;
  export let never: string | undefined = undefined;
  export let tokens: string | undefined = undefined;
  export let canRemember = true;

  export let onAccept: (remember: boolean) => void = () => {};
  export let onDecline: () => void = () => {};
</script>

<div class="consent-box">
  <h3 class="consent-box__title">{title}</h3>

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

  <div class="consent-box__actions">
    <Button variant="ghost" on:click={onDecline}>
      {$t('common.consent.no')}
    </Button>
    <div class="consent-box__primary-group">
      {#if canRemember}
        <Button variant="secondary" on:click={() => onAccept(true)}>
          {$t('common.consent.yes_remember')}
        </Button>
      {/if}
      <Button variant="primary" on:click={() => onAccept(false)}>
        {$t('common.consent.yes')}
      </Button>
    </div>
  </div>
</div>

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
    margin-bottom: var(--space-6);
  }

  .consent-box__row {
    display: grid;
    grid-template-columns: 140px 1fr;
    gap: var(--space-4);
    font-size: var(--text-sm);
  }

  .consent-box__label {
    font-weight: 500;
    color: var(--color-text-secondary);
  }

  .consent-box__value {
    color: var(--color-text-primary);
  }

  .consent-box__actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-4);
    gap: var(--space-3);
  }

  .consent-box__primary-group {
    display: flex;
    gap: var(--space-3);
  }

  @media (max-width: 640px) {
    .consent-box__row {
      grid-template-columns: 1fr;
      gap: var(--space-1);
    }

    .consent-box__actions {
      flex-direction: column-reverse;
      align-items: stretch;
    }

    .consent-box__primary-group {
      flex-direction: column;
    }
  }
</style>
