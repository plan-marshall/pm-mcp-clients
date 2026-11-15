<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import { apiClient, type OutlineRecord } from '../api/client';
  import { errorCodeOf } from '../api/errors';
  import { stripControlSequences } from '../utils/outputSafety';
  import { refreshesIntentView, changedRevision } from './intentEvents';
  import ErrorState from './ErrorState.svelte';
  import Link from './Link.svelte';
  import Spinner from './Spinner.svelte';

  /**
   * The plan's outline (design `intent-and-outline.adoc#outline`, web part `#web`): the deliverables
   * with their number and title, their gloss, the files they affect with the intent of each, the
   * deliverables they depend on, the criteria they satisfy and how they are verified, and the added
   * dependencies with the deliverable that adds each and the reason.
   *
   * A path is never turned into a link, a dependency on a deliverable the record does not carry is
   * shown as `missing deliverable <n>`, and a deliverable that satisfies no criterion says so. The
   * outline record's fields are the contract's: the design names no interface and no key link, and
   * the record carries none, so none is drawn.
   */
  interface Props {
    /** The plan whose outline is shown. */
    planId: string;
    /** The address of a criterion in the intent, or `''` where the caller has none. */
    criterionHref?: (criterionId: string) => string;
  }

  let {
    planId,
    criterionHref = (criterionId: string) =>
      `/plans/${encodeURIComponent(planId)}?tab=intent#criterion-${encodeURIComponent(criterionId)}`,
  }: Props = $props();

  /** The record and the reason the read failed, if it did. */
  let record = $state<OutlineRecord | null>(null);
  let loading = $state(true);
  let errorCode = $state<string | null>(null);
  /** The revision that arrived on a runtime event, so `the record changed` is said once. */
  let announcedRevision = $state<number | null>(null);
  let unsubscribe: (() => void) | null = null;

  onMount(() => {
    void load();
    unsubscribe = apiClient.subscribeEvents((event) => {
      if (refreshesIntentView(event)) {
        void load(false);
      }
    });
  });

  onDestroy(() => {
    unsubscribe?.();
    unsubscribe = null;
  });

  /**
   * @param initial whether this is the panel's first read, which draws the wait
   */
  async function load(initial = true): Promise<void> {
    if (initial) {
      loading = true;
    }
    try {
      const next = await apiClient.getPlanOutline(planId);
      announcedRevision = changedRevision(record, next, initial);
      record = next;
      errorCode = null;
    } catch (failure) {
      errorCode = errorCodeOf(failure);
    } finally {
      loading = false;
    }
  }

  /** @returns whether the record on screen is one whose revision arrived on an event */
  function changed(): boolean {
    return announcedRevision !== null && announcedRevision === record?.revision;
  }

  /** @param value a record text @returns it as plain text, never as markup */
  function safe(value: string | null | undefined): string {
    return stripControlSequences(value);
  }

  /**
   * @param fileIntent the wire value of a file's intent (`write-new`)
   * @returns the catalogue key of its word (`intent.outline.file.write_new`)
   */
  function fileIntentKey(fileIntent: string): string {
    return `intent.outline.file.${fileIntent.replace(/-/g, '_')}`;
  }

  /**
   * @param deliverableNumber the number a deliverable depends on
   * @returns it by number and title, or `missing deliverable <n>` where the record lacks it
   */
  function dependencyLabel(deliverableNumber: number): string {
    const dependency = record?.deliverables.find(
      (entry) => entry.number === deliverableNumber,
    );
    return dependency
      ? $t('intent.outline.deliverable', {
          number: dependency.number,
          title: safe(dependency.title),
        })
      : $t('intent.outline.missing_deliverable', { number: deliverableNumber });
  }

  /**
   * @param dependency the added dependency
   * @returns its line, with no placeholder left where the record names no version
   */
  function dependencyLine(dependency: {
    name: string;
    version?: string;
    justification: string;
    deliverable: number;
  }): string {
    // With no version the message would keep a double space where the version stands; only that
    // first gap is collapsed, so a justification's own spacing survives as it stands.
    return $t('intent.outline.dependency', {
      coordinates: safe(dependency.name),
      version: dependency.version === undefined ? '' : safe(dependency.version),
      deliverable: dependency.deliverable,
      justification: safe(dependency.justification),
    }).replace('  ', ' ');
  }
</script>

{#if errorCode !== null}
  <div class="outline-panel" data-testid="outline-panel">
    <ErrorState code={errorCode} titleLevel={2} announce={false} />
  </div>
{:else if record === null && loading}
  <div class="outline-panel" data-testid="outline-panel-waiting">
    <Spinner label={$t('intent.outline.title')} />
  </div>
{:else}
  <section class="outline-panel" data-testid="outline-panel" aria-label={$t('intent.outline.title')}>
    <h2 class="outline-panel__head">{$t('intent.outline.title')}</h2>

    {#if record !== null && changed()}
      <p class="outline-panel__changed" data-testid="outline-changed">
        {$t('intent.record.changed')}
      </p>
    {/if}

    {#if record !== null}
      <ul class="outline-deliverables">
        {#each record.deliverables as deliverable (deliverable.number)}
          <li
            class="outline-deliverable"
            id={`deliverable-${deliverable.number}`}
            data-testid={`outline-deliverable-${deliverable.number}`}
          >
            <h3 class="outline-deliverable__title">
              {$t('intent.outline.deliverable', {
                number: deliverable.number,
                title: safe(deliverable.title),
              })}
            </h3>

            <p class="outline-deliverable__gloss" data-testid={`outline-gloss-${deliverable.number}`}>
              {safe(deliverable.intent_gloss)}
            </p>

            {#if deliverable.affected_files.length > 0}
              <h4 class="outline-deliverable__label">{$t('intent.outline.files')}</h4>
              <ul class="outline-files">
                {#each deliverable.affected_files as file, index (`${file.path}-${index}`)}
                  <li
                    class="outline-file"
                    data-testid={`outline-file-${deliverable.number}-${index}`}
                  >
                    <span class="outline-file__path">{safe(file.path)}</span>
                    <span class="outline-file__intent">{$t(fileIntentKey(file.intent))}</span>
                  </li>
                {/each}
              </ul>
            {/if}

            {#if deliverable.depends_on.length > 0}
              <p class="outline-deliverable__depends" data-testid={`outline-depends-${deliverable.number}`}>
                {$t('intent.outline.depends_on', {
                  deliverables: deliverable.depends_on
                    .map((number) => dependencyLabel(number))
                    .join(', '),
                })}
              </p>
            {/if}

            <p class="outline-deliverable__satisfies" data-testid={`outline-satisfies-${deliverable.number}`}>
              {#if deliverable.satisfies.length === 0}
                {$t('intent.outline.satisfies_none')}
              {:else}
                {$t('intent.outline.satisfies', { criteria: '' })}
                {#each deliverable.satisfies as criterionId, index (criterionId)}
                  {#if index > 0}, {/if}<Link
                    href={criterionHref(criterionId)}
                    label={safe(criterionId)}
                  />
                {/each}
              {/if}
            </p>

            {#if deliverable.verification !== undefined}
              <p
                class="outline-deliverable__verification"
                data-testid={`outline-verification-${deliverable.number}`}
              >
                {$t('intent.outline.verification')}: {safe(deliverable.verification)}
              </p>
            {/if}
          </li>
        {/each}
      </ul>

      {#if record.added_dependencies.length > 0}
        <h3 class="outline-panel__title" data-testid="outline-dependencies">
          {$t('intent.outline.dependencies')}
        </h3>
        <ul class="outline-dependencies">
          {#each record.added_dependencies as dependency, index (index)}
            <li class="outline-dependency" data-testid={`outline-dependency-${index}`}>
              {dependencyLine(dependency)}
            </li>
          {/each}
        </ul>
      {/if}
    {/if}
  </section>
{/if}

<style>
  .outline-panel {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
    color: var(--color-text-primary);
  }

  .outline-panel__head,
  .outline-panel__title {
    margin: 0;
    font-size: var(--text-base);
    line-height: var(--line-height-base);
  }

  .outline-panel__changed {
    margin: 0;
    color: var(--color-text-secondary);
  }

  .outline-panel__title {
    margin-block-start: var(--space-3);
    font-size: var(--text-sm);
    font-weight: 600;
  }

  .outline-deliverables,
  .outline-files,
  .outline-dependencies {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .outline-deliverable {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding-block-end: var(--space-2);
    border-block-end: 1px solid var(--color-border);
  }

  .outline-deliverable__title {
    margin: 0;
    font-size: var(--text-sm);
    font-weight: 600;
  }

  .outline-deliverable__gloss {
    margin: 0;
    overflow-wrap: anywhere;
  }

  .outline-deliverable__label {
    margin: 0;
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .outline-file {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .outline-file__path {
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    overflow-wrap: anywhere;
  }

  .outline-file__intent,
  .outline-deliverable__depends,
  .outline-deliverable__satisfies,
  .outline-deliverable__verification,
  .outline-dependency {
    color: var(--color-text-secondary);
  }

  .outline-deliverable__depends,
  .outline-deliverable__satisfies,
  .outline-deliverable__verification {
    margin: 0;
  }

  .outline-dependency {
    overflow-wrap: anywhere;
  }
</style>
