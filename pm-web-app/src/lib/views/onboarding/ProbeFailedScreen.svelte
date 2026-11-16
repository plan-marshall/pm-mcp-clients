<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** One harness the probe could not use, as the runtime reports it. */
  interface Check {
    harness?: string;
    name?: string;
    result?: string;
    cause?: string;
    fix?: string;
  }

  /**
   * Every harness of the probe failed (`harness.probe-failed`, UC-F-05): what was checked, per
   * harness, with the cause and the fix the runtime names — never a bare state id.
   */
  let { view }: ScreenProps = $props();
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.harness.probe-failed.title')}
    </h1>
  </div>
  <dl class="detail-list" data-testid="probe-checks">
    {#each view.arrayField<Check>('checks') as check}
      <div class="detail-row">
        <dt>{check.name ?? check.harness ?? $t('onboard.state.unknown')}</dt>
        <dd>
          {#if check.cause}<span class="probe-check__cause">{check.cause}</span>{/if}
          {#if check.fix}<span class="mono probe-check__fix">{check.fix}</span>{/if}
        </dd>
      </div>
    {/each}
  </dl>
  <div class="state-card__actions">
    {#each view.links as { id, isPrimary }}
      <Button
        variant={isPrimary ? 'primary' : 'secondary'}
        loading={view.busy}
        on:click={() => view.onLink(id)}
      >
        {view.linkLabel(id)}
      </Button>
    {/each}
  </div>
</div>

<style>
  .probe-check__cause {
    display: block;
    color: var(--color-text-primary);
  }

  .probe-check__fix {
    display: block;
    margin-top: var(--space-1);
    color: var(--color-text-secondary);
  }
</style>
