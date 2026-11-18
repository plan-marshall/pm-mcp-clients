<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * The automatic run's result (`auto.result`), per aspect, with the reason the runtime names.
   *
   * Every line the state can run again carries *Change* (`onboarding-steps-4-7.adoc` § The Result):
   * the state offers the catalogue's `change` link per aspect — `change:<aspect>`, as the machine
   * settings offer `reconfigure:<aspect>` — and *Change* takes it, so the step opens again as a
   * later run with its current value as the proposal. A line the state carries no link for stands
   * without a *Change*, and a link whose line is missing from `results` keeps its own button.
   *
   * The line's name is the catalogue's `onboard.auto.aspect.<aspect>`, the same words the terminal
   * client prints for the row; the facts are `AutoResultItem` of the published contract
   * (`aspect`, `value`, `reason`).
   */
  let { view }: ScreenProps = $props();

  /** One line of the result, as the runtime reports it (`AutoResultItem`). */
  interface AspectResult {
    aspect?: string;
    value?: string;
    reason?: string;
  }

  const results = $derived(view.arrayField<AspectResult>('results'));

  /** @param aspect the reported aspect @returns the catalogue key of the line's name */
  function aspectKey(aspect: string | undefined): string {
    return `onboard.auto.aspect.${aspect ?? ''}`;
  }

  /** @param aspect the reported aspect @returns the id of the state's link that runs it again */
  function changeLinkId(aspect: string | undefined): string {
    return `change:${aspect ?? ''}`;
  }

  /**
   * @param aspect the reported aspect
   * @returns the state's link that runs the aspect again, when it offers one
   */
  function hasChangeLink(aspect: string | undefined): boolean {
    return view.links.some((link) => link.id === changeLinkId(aspect));
  }

  /**
   * The links no line took: the way on, and a `change:<aspect>` link whose line `results` does not
   * report, so no act the state offers is left unreachable.
   *
   * @returns the links the footer draws
   */
  const footerLinks = $derived(
    view.links.filter((link) => {
      if (!link.id.startsWith('change:')) return true;
      return !results.some((result) => changeLinkId(result.aspect) === link.id);
    }),
  );
</script>

<div class="state-card">
  <div class="state-card__header">
    <h1 class="state-card__title">
      {view.field('summary') || $t('onboard.auto.result.title')}
    </h1>
  </div>
  <table class="result-table" data-testid="auto-result">
    <thead>
      <tr>
        <th>{$t('onboard.auto.result.aspect')}</th>
        <th>{$t('onboard.auto.result.value')}</th>
        <th>{$t('onboard.auto.result.reason')}</th>
        <th><span class="sr-only">{$t('onboard.auto.result.action_label')}</span></th>
      </tr>
    </thead>
    <tbody>
      {#each results as result}
        {@const aspect = $t(aspectKey(result.aspect))}
        <tr>
          <td>{aspect}</td>
          <td>{result.value}</td>
          <td>{$t(`onboard.auto.reason.${result.reason ?? 'default'}`)}</td>
          <td class="result-table__act">
            {#if hasChangeLink(result.aspect)}
              <Button
                variant="secondary"
                loading={view.busy}
                aria-label={$t('onboard.auto.result.change_label', { aspect })}
                onclick={() => view.onLink(changeLinkId(result.aspect))}
                data-testid={`auto-result-change-${result.aspect}`}
              >
                {$t('onboard.link.change')}
              </Button>
            {/if}
          </td>
        </tr>
      {/each}
    </tbody>
  </table>
  <div class="state-card__actions">
    {#each footerLinks as { id, isPrimary }}
      {@const aspect = id.startsWith('change:') ? id.slice('change:'.length) : null}
      <Button
        variant={isPrimary ? 'primary' : 'secondary'}
        loading={view.busy}
        on:click={() => view.onLink(id)}
      >
        {aspect
          ? $t('onboard.auto.result.change_label', {
              aspect: $t(`onboard.auto.aspect.${aspect}`),
            })
          : view.linkLabel(id)}
      </Button>
    {/each}
  </div>
</div>

<style>
  .result-table__act {
    text-align: right;
    white-space: nowrap;
  }
</style>
