<script lang="ts">
  import Button from '../../components/Button.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * No usable harness (`harness.none`, UC-ONB-4).
   *
   * The catalogue's row is the whole screen: no usable harness, so it names how to install each
   * known one — each profile's install hint — and offers `recheck` and `add`
   * (`14-machine-onboarding.adoc:277-283`). The per-harness name, cause and fix are the facts of
   * `harness.blocked`, never of this state, and the mock-only `checks[]`, `path`, `check` and
   * `later` are gone with it.
   *
   * The heading is the catalogue's, never the runtime's `summary`: that summary is a single ASCII
   * line (`hypermedia-format/01-…:90-93`) and stands as a sentence under the heading.
   */
  let { view }: ScreenProps = $props();

  /** How to install one known harness, as the engine's `HarnessNone.InstallHint` carries it. */
  interface InstallHint {
    harness_id?: string;
    install_hint?: string;
  }

  const known = $derived(view.arrayField<InstallHint>('known'));

  /** The runtime's summary, unless it repeats the catalogue's heading. */
  const summary = $derived.by(() => {
    const text = view.field('summary');
    return text !== undefined && text.trim() !== $t('onboard.harness.none.title') ? text : null;
  });
</script>

<div class="state-card" data-testid="harness-none">
  <div class="state-card__header">
    <h1 class="state-card__title">{$t('onboard.harness.none.title')}</h1>
    {#if summary}
      <p class="state-card__summary">{summary}</p>
    {/if}
  </div>

  {#if known.length > 0}
    <p class="harness-none__intro">{$t('onboard.harness.none.install_one')}</p>
    <ul class="harness-none__install" data-testid="harness-none-install">
      {#each known as hint}
        <li class="harness-install">
          <span class="harness-install__name mono">{hint.harness_id}</span>
          <span class="harness-install__hint mono">{hint.install_hint}</span>
        </li>
      {/each}
    </ul>
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
  .harness-none__intro {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    margin-bottom: var(--space-3);
  }

  .harness-none__install {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin-bottom: var(--space-4);
    list-style: none;
    padding: 0;
  }

  .harness-install {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
  }

  .harness-install__name {
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .harness-install__hint {
    color: var(--color-text-secondary);
    word-break: break-word;
  }
</style>
