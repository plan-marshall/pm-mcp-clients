<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { harnessUsageText, type HarnessTokenInfo } from '../../display/harnessUsage';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** One harness as the token states report it. */
  interface Harness extends HarnessTokenInfo {
    harness_id?: string;
    name?: string;
  }

  /**
   * How a harness reports its tokens (`harness.tokens-complete`, `-partial`, `-not-reported`):
   * information, never a condition of the choice (onboarding.adoc, What a harness reports).
   */
  let { view }: ScreenProps = $props();

  /** The catalogue sentence of a harness's token reporting. */
  function tokenState(harness: Harness): string {
    return harnessUsageText($t, harness, { preferDetailOnComplete: true });
  }
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.harness.choose.title')}
    </h1>
  </div>
  <ul class="completed-list" data-testid="token-reporting">
    {#each view.arrayField<Harness>('harnesses') as harness}
      <li class="completed-row">
        <span class="completed-row__name">{harness.name ?? harness.harness_id ?? ''}</span>
        <span class="completed-row__value">{tokenState(harness)}</span>
      </li>
    {/each}
  </ul>
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
