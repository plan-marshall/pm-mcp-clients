<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * The automatic run's result (`auto.result`), per aspect, with the reason the runtime names.
   *
   * Every line the state can run again carries *Change* (`onboarding-steps-4-7.adoc` § The Result):
   * the state offers one `reconfigure:<aspect>` link per line, the same link the machine settings
   * offer, and *Change* takes it — the step opens again as a later run with its current value as the
   * proposal. A line the state carries no link for stands without a *Change*.
   */
  let { view }: ScreenProps = $props();

  /** One line of the result, as the runtime reports it. */
  interface AspectResult {
    aspect?: string;
    value?: string;
    reason?: string;
  }

  /**
   * The step each reported aspect is, from the aspect's own name to the step the run walked: the
   * catalogue's step word names the line, never the wire token.
   */
  const ASPECT_STEP: Record<string, string> = {
    language: 'language',
    harness: 'harnesses',
    catalogue: 'models',
    roles: 'roles',
    store: 'credentials',
    providers: 'providers',
    defaults: 'plans',
  };

  /**
   * The link id each aspect runs again. The state carries the link; the map only names the aspect's
   * own id, because the settings call the credentials' line `credentials` and the plans' line
   * `plan-defaults`.
   */
  const ASPECT_LINK: Record<string, string> = {
    language: 'reconfigure:language',
    harness: 'reconfigure:harnesses',
    catalogue: 'reconfigure:models',
    roles: 'reconfigure:roles',
    store: 'reconfigure:credentials',
    providers: 'reconfigure:providers',
    defaults: 'reconfigure:plan-defaults',
  };

  const results = $derived(view.arrayField<AspectResult>('results'));

  /** @param aspect the reported aspect @returns the catalogue key of the step's name */
  function stepKey(aspect: string | undefined): string {
    const step = ASPECT_STEP[aspect ?? ''] ?? aspect ?? '';
    return `onboard.step.${step}`;
  }

  /**
   * @param aspect the reported aspect
   * @returns the id of the state's link that runs the aspect again, or `null` when it offers none
   */
  function changeLink(aspect: string | undefined): string | null {
    const id = ASPECT_LINK[aspect ?? ''];
    if (!id) return null;
    return view.links.some((link) => link.id === id) ? id : null;
  }

  /** @param aspect the reported aspect @returns the step's name, in the catalogue's words */
  function aspectLabel(aspect: string | undefined): string {
    return $t(stepKey(aspect));
  }
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
        <th><span class="visually-hidden">{$t('onboard.auto.result.action_label')}</span></th>
      </tr>
    </thead>
    <tbody>
      {#each results as result}
        {@const link = changeLink(result.aspect)}
        <tr>
          <td>{aspectLabel(result.aspect)}</td>
          <td>{result.value}</td>
          <td>{$t(`onboard.auto.reason.${result.reason ?? 'default'}`)}</td>
          <td class="result-table__act">
            {#if link}
              <Button
                variant="secondary"
                loading={view.busy}
                aria-label={`${$t('web.onboard.auto_result.change')} ${aspectLabel(result.aspect)}`}
                onclick={() => view.onLink(link)}
                data-testid={`auto-result-change-${result.aspect}`}
              >
                {$t('web.onboard.auto_result.change')}
              </Button>
            {/if}
          </td>
        </tr>
      {/each}
    </tbody>
  </table>
  <div class="state-card__actions">
    {#each view.links as { id, isPrimary }}
      {#if !id.startsWith('reconfigure:')}
        <Button
          variant={isPrimary ? 'primary' : 'secondary'}
          loading={view.busy}
          on:click={() => view.onLink(id)}
        >
          {view.linkLabel(id)}
        </Button>
      {/if}
    {/each}
  </div>
</div>

<style>
  .result-table__act {
    text-align: right;
    white-space: nowrap;
  }

  .visually-hidden {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip-path: inset(50%);
    white-space: nowrap;
  }
</style>
