<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * No harness can be used (`harness.none`, UC-ONB-4).
   *
   * When the state reports the checks that failed, each chosen harness stands with its name, its
   * path, the check that failed, the answer the harness gave and the fix — the list of what was
   * looked for is replaced, as `onboarding.adoc` § None Found states. A state that reports no check
   * (nothing was found in the first place) shows the one sentence of what was looked for.
   *
   * The screen never prints the state's id: the runtime's own summary is the title, and the words
   * of the copy come from the catalogue.
   */
  let { view }: ScreenProps = $props();

  /** One harness whose check failed, as the runtime reports it. */
  interface FailedHarness {
    harness?: string;
    name?: string;
    path?: string;
    check?: string;
    result?: string;
    cause?: string;
    fix?: string;
  }

  const failed = $derived(view.arrayField<FailedHarness>('checks'));

  const title = $derived(
    view.field('summary') ||
      (failed.length > 0
        ? $t('onboard.harness.none.unusable_title')
        : $t('onboard.harness.none.title')),
  );

  /**
   * @param check the check id the runtime reports
   * @returns the catalogue's words for it, which never leave the wire id on screen
   */
  function checkLabel(check: string): string {
    return $t(`onboard.harness.check.${check}`);
  }
</script>

<div class="state-card" data-testid="harness-none">
  <div class="state-card__header">
    <h1 class="state-card__title">{title}</h1>
    {#if failed.length === 0}
      <p class="state-card__summary">
        {$t('onboard.harness.none.looked_for', {
          harnesses: view.field('harnesses') ?? '',
          locations: view.field('locations') ?? '',
        })}
      </p>
    {/if}
  </div>

  {#if failed.length > 0}
    <p class="harness-none__intro">{$t('onboard.harness.none.none_passed')}</p>
    <div class="harness-none__failures" data-testid="harness-none-failures">
      {#each failed as entry}
        <section class="harness-failure">
          <h2 class="harness-failure__name">{entry.name ?? entry.harness}</h2>
          <dl class="detail-list">
            {#if entry.check}
              <div class="detail-row">
                <dt>{$t('onboard.harness.none.check')}</dt>
                <dd>{checkLabel(entry.check)}</dd>
              </div>
            {/if}
            {#if entry.path}
              <div class="detail-row">
                <dt>{$t('onboard.harness.none.path')}</dt>
                <dd class="mono">{entry.path}</dd>
              </div>
            {/if}
            {#if entry.cause}
              <div class="detail-row">
                <dt>{$t('onboard.harness.none.answer')}</dt>
                <dd>{entry.cause}</dd>
              </div>
            {/if}
            {#if entry.fix}
              <div class="detail-row">
                <dt>{$t('onboard.state.fix')}</dt>
                <dd class="mono">{entry.fix}</dd>
              </div>
            {/if}
          </dl>
        </section>
      {/each}
    </div>
    <p class="harness-none__fix-one">{$t('onboard.harness.none.fix_one')}</p>
  {/if}

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
  .harness-none__intro,
  .harness-none__fix-one {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    margin-bottom: var(--space-3);
  }

  .harness-none__failures {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .harness-failure {
    padding: var(--space-4);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
  }

  .harness-failure__name {
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: var(--space-2);
  }
</style>
