<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import { apiClient, type EpicIntentRecord, type DoneWhenVerdict } from '../api/client';
  import { errorCodeOf } from '../api/errors';
  import { stripControlSequences } from '../utils/outputSafety';
  import { refreshesEpicIntentView, recordFingerprint, changedFingerprint } from './intentEvents';
  import ErrorState from './ErrorState.svelte';
  import Spinner from './Spinner.svelte';
  import StatusLabel from './StatusLabel.svelte';

  /**
   * The epic's intent (design `intent-and-outline.adoc#epic-destination`, `#epic-rest`, `#web`): the
   * destination, the done-when checks with their verdict and their evidence, the non-goals, the
   * questions not yet specified, and each staged plan's own criterion with the plan's title.
   *
   * The verdict is the record's, never a plan row's: a check the completion review has not decided is
   * `pending` and neutral, and the list stays a list rather than becoming a progress bar. Every text
   * may come from a model or a source, so it reaches the DOM as text after `stripControlSequences`,
   * and a check's evidence stays text even when it names a path or a `pm://` URI.
   *
   * The epic's sources are not this panel's: they stand at the top of the epic's tab.
   */
  interface Props {
    /** The epic the record belongs to. */
    epicId: string;
  }

  let { epicId }: Props = $props();

  /** The record, `null` where the epic has none, and the reason the read failed when it did. */
  let record = $state<EpicIntentRecord | null>(null);
  let loading = $state(true);
  let errorCode = $state<string | null>(null);
  /** The record that arrived on a runtime event, so `the record changed` is said once. */
  let announced = $state<string | null>(null);
  /**
   * The read that owns the panel's state: a read that answers after a newer one has started is
   * dropped, so an older record never replaces a newer one.
   */
  let readSequence = 0;
  let unsubscribe: (() => void) | null = null;

  /** The verdict's tone: `pending` is neutral until the completion review decides it. */
  const TONES: Record<DoneWhenVerdict, 'success' | 'danger' | 'neutral'> = {
    pending: 'neutral',
    met: 'success',
    not_met: 'danger',
  };

  onMount(() => {
    void load();
    unsubscribe = apiClient.subscribeEvents((event) => {
      if (refreshesEpicIntentView(event)) {
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
    const sequence = ++readSequence;
    if (initial) {
      loading = true;
    }
    try {
      const next = await apiClient.getEpicIntent(epicId);
      if (sequence !== readSequence) {
        return;
      }
      announced = changedFingerprint(
        recordFingerprint(record),
        recordFingerprint(next),
        initial,
      );
      record = next;
      errorCode = null;
    } catch (failure) {
      if (sequence !== readSequence) {
        return;
      }
      errorCode = errorCodeOf(failure);
    } finally {
      if (sequence === readSequence) {
        loading = false;
      }
    }
  }

  /** @param value a record text @returns it as plain text, never as markup */
  function safe(value: string | null | undefined): string {
    return stripControlSequences(value);
  }

  /**
   * @param verdict the check's verdict
   * @returns the shared word of the verdict
   */
  function verdictWord(verdict: DoneWhenVerdict): string {
    return $t(`state.check.${verdict}`);
  }

  /** @returns whether the panel has a record that arrived on an event */
  function changed(): boolean {
    return announced !== null && announced === recordFingerprint(record);
  }
</script>

{#if errorCode !== null}
  <div class="epic-intent" data-testid="epic-intent-panel">
    <ErrorState code={errorCode} titleLevel={2} announce={false} />
  </div>
{:else if record === null && loading}
  <div class="epic-intent" data-testid="epic-intent-panel-waiting">
    <Spinner label={$t('intent.tab.title')} />
  </div>
{:else if record === null}
  <div class="epic-intent" data-testid="epic-intent-panel">
    <p class="epic-intent__none">{$t('intent.epic.none')}</p>
  </div>
{:else}
  <section class="epic-intent" data-testid="epic-intent-panel" aria-label={$t('intent.tab.title')}>
    {#if changed()}
      <p class="epic-intent__changed" data-testid="epic-changed">
        {$t('intent.record.changed')}
      </p>
    {/if}

    <h3 class="epic-intent__title">{$t('intent.epic.destination')}</h3>
    <p class="epic-intent__destination" data-testid="epic-destination">
      {safe(record.destination)}
    </p>

    <h3 class="epic-intent__title">{$t('intent.epic.review')}</h3>
    <p class="epic-intent__label">{$t('intent.epic.done_when')}</p>
    <ul class="epic-checks">
      {#each record.done_when as check, index (index)}
        <li class="epic-check" data-testid={`epic-check-${index}`}>
          <span class="epic-check__text" data-testid={`epic-check-text-${index}`}>
            {safe(check.text)}
          </span>
          <span class="epic-check__verdict" data-testid={`epic-check-verdict-${index}`}>
            <StatusLabel tone={TONES[check.verdict]} label={verdictWord(check.verdict)} />
          </span>
          {#each check.evidence as entry, entryIndex (entryIndex)}
            <span
              class="epic-check__evidence"
              data-testid={`epic-check-evidence-${index}-${entryIndex}`}
            >
              {$t('intent.record.evidence', { evidence: safe(entry) })}
            </span>
          {/each}
        </li>
      {/each}
    </ul>

    {#if record.non_goals.length > 0}
      <h3 class="epic-intent__title" data-testid="epic-non-goals">{$t('intent.non_goals.title')}</h3>
      <ul class="epic-list">
        {#each record.non_goals as nonGoal, index (index)}
          <li class="epic-list__item" data-testid={`epic-non-goal-${index}`}>
            {safe(nonGoal)}
          </li>
        {/each}
      </ul>
    {/if}

    {#if record.not_yet_specified.length > 0}
      <h3 class="epic-intent__title" data-testid="epic-not-specified">
        {$t('intent.epic.not_specified')}
      </h3>
      <ul class="epic-list">
        {#each record.not_yet_specified as question, index (index)}
          <li class="epic-list__item" data-testid={`epic-question-${index}`}>
            {safe(question)}
          </li>
        {/each}
      </ul>
    {/if}

    {#if record.plans.length > 0}
      <h3 class="epic-intent__title">{$t('intent.epic.plans')}</h3>
      <ul class="epic-plans">
        {#each record.plans as plan (plan.plan_id)}
          <li class="epic-plan" data-testid={`epic-plan-${plan.plan_id}`}>
            <span class="epic-plan__title" data-testid={`epic-plan-title-${plan.plan_id}`}>
              {safe(plan.title)}
            </span>
            <ul class="epic-plan__criteria">
              {#each plan.done_when as criterion, index (index)}
                <li
                  class="epic-plan__criterion"
                  data-testid={`epic-plan-${plan.plan_id}-done-when-${index}`}
                >
                  {safe(criterion)}
                </li>
              {/each}
            </ul>
          </li>
        {/each}
      </ul>
    {/if}
  </section>
{/if}

<style>
  .epic-intent {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
    color: var(--color-text-primary);
  }

  .epic-intent__changed,
  .epic-intent__none,
  .epic-intent__label {
    margin: 0;
    color: var(--color-text-secondary);
  }

  .epic-intent__destination,
  .epic-check__text,
  .epic-list__item,
  .epic-plan__title,
  .epic-plan__criterion {
    overflow-wrap: anywhere;
  }

  .epic-intent__destination {
    margin: 0;
  }

  .epic-intent__title {
    margin: var(--space-3) 0 0;
    font-size: var(--text-sm);
    font-weight: 600;
  }

  .epic-checks,
  .epic-list,
  .epic-plans,
  .epic-plan__criteria {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .epic-check {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: var(--space-2);
    padding-block-end: var(--space-2);
    border-block-end: 1px solid var(--color-border);
  }

  .epic-check__text {
    flex: 1 1 16rem;
  }

  /* The evidence is text, one entry per line, under the check it belongs to. */
  .epic-check__evidence {
    flex: 1 1 100%;
    color: var(--color-text-secondary);
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    overflow-wrap: anywhere;
  }

  .epic-plan__title {
    font-weight: 600;
  }

  .epic-plan__criteria {
    margin-inline-start: var(--space-3);
    color: var(--color-text-secondary);
  }
</style>
