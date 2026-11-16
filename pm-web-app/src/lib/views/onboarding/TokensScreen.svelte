<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /** One harness as the token states report it. */
  interface Harness {
    harness_id?: string;
    name?: string;
    token_usage?: string;
    token_detail?: string;
    missing_figures?: string[];
    usage?: { figures?: Record<string, string> };
  }

  /**
   * How a harness reports its tokens (`harness.tokens-complete`, `-partial`, `-not-reported`):
   * information, never a condition of the choice (onboarding.adoc, What a harness reports).
   */
  let { view }: ScreenProps = $props();

  /** The missing figures the runtime named, when it named them instead of a detail line. */
  function figures(harness: Harness): string {
    if (harness.token_detail) return harness.token_detail;
    if (Array.isArray(harness.missing_figures)) return harness.missing_figures.join(', ');
    return '';
  }

  /** The catalogue sentence of a harness's token reporting. */
  function tokenState(harness: Harness): string {
    if (harness.token_usage === 'complete') {
      return $t('onboard.harness.tokens.complete', { figures: figures(harness) });
    }
    if (harness.token_usage === 'partial') {
      return $t('onboard.harness.tokens.partial', { figures: figures(harness) });
    }
    return $t('onboard.harness.tokens.not_reported');
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
