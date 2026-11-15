<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import {
    apiClient,
    CRITERION_STATUSES,
    INTENT_DECIDERS,
    type PlanIntentRecord,
    type OutlineDeliverableRecord,
  } from '../api/client';
  import { errorCodeOf } from '../api/errors';
  import { stripControlSequences } from '../utils/outputSafety';
  import { refreshesIntentView, changedRevision } from './intentEvents';
  import Button from './Button.svelte';
  import ErrorState from './ErrorState.svelte';
  import Link from './Link.svelte';
  import Spinner from './Spinner.svelte';
  import StatusLabel from './StatusLabel.svelte';

  /**
   * The plan's intent (design `intent-and-outline.adoc#intent`, web part `#web`): the head with the
   * plan, the revision, the goal and the counts by verdict; the criteria with their verdict as a
   * `StatusLabel` and the reason one expansion away; the non-goals by their own ids; the choices
   * grouped by who made them, the operator's and a quorum's first and the model's own calls apart;
   * and the open questions and the deferred items with their reasons.
   *
   * Every text of the record may come from a model or a source, so it reaches the DOM as text after
   * `stripControlSequences`, never as HTML. A verdict is never derived from a deliverable's state,
   * and a field the record does not carry is left out.
   */
  interface Props {
    /** The plan the record belongs to; its title is the plan view's head, one step away. */
    planId: string;
    /**
     * The outline's deliverables, so a criterion names the deliverables that satisfy it by number
     * and title rather than by number alone. Empty where the caller read no outline.
     */
    deliverables?: OutlineDeliverableRecord[];
    /** The address of the outline's deliverable, or `''` where the caller has none. */
    deliverableHref?: (number: number) => string;
    /** The address of a decided question, or `''` where the caller has none. */
    questionHref?: (questionId: string) => string;
  }

  let {
    planId,
    deliverables = [],
    deliverableHref = () => '',
    questionHref = (questionId: string) => `/decisions?decision=${encodeURIComponent(questionId)}`,
  }: Props = $props();

  /** The record, `null` before refine wrote one, and the reason the read failed when it did. */
  let record = $state<PlanIntentRecord | null>(null);
  let loading = $state(true);
  let errorCode = $state<string | null>(null);
  /** The revision that arrived on a runtime event, so `the record changed` is said once. */
  let announcedRevision = $state<number | null>(null);
  /**
   * The read that owns the panel's state: a read that answers after a newer one has started is
   * dropped, so an older record never replaces a newer one.
   */
  let readSequence = 0;
  /** The criteria and questions whose detail the operator opened, by id, kept across a refresh. */
  let expanded = $state<Record<string, boolean>>({});
  let unsubscribe: (() => void) | null = null;

  /** The verdict's tone: `not verified` and `not reviewed yet` are neutral, never a warning. */
  const TONES: Record<string, 'success' | 'danger' | 'neutral'> = {
    met: 'success',
    not_met: 'danger',
    not_verified: 'neutral',
  };

  /**
   * The deciders in the order the design shows them: the operator and a quorum first. The set is the
   * contract's own, so an added decider is shown rather than silently left out.
   */
  const DECIDER_ORDER = INTENT_DECIDERS;

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
    const sequence = ++readSequence;
    if (initial) {
      loading = true;
    }
    try {
      const next = await apiClient.getPlanIntent(planId);
      if (sequence !== readSequence) {
        return;
      }
      announcedRevision = changedRevision(record, next, initial);
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

  /** @param id the criterion or question whose detail is toggled */
  function toggle(id: string): void {
    expanded = { ...expanded, [id]: !expanded[id] };
  }

  /** @param value a record text @returns it as plain text, never as markup */
  function safe(value: string | null | undefined): string {
    return stripControlSequences(value);
  }

  /**
   * @param status the criterion's verdict, or `undefined` before the self-review
   * @returns the shared word of the verdict
   */
  function verdictWord(status: string | undefined): string {
    return $t(`state.criterion.${status ?? 'not_reviewed'}`);
  }

  /**
   * @param status the criterion's verdict, or `undefined` before the self-review
   * @returns the StatusLabel tone of the verdict; `not verified` and `not reviewed` are neutral
   */
  function verdictTone(status: string | undefined): 'success' | 'danger' | 'neutral' {
    return status === undefined ? 'neutral' : (TONES[status] ?? 'neutral');
  }

  /** @returns whether the self-review has judged at least one criterion */
  function reviewed(): boolean {
    return (record?.criteria ?? []).some((criterion) => criterion.status !== undefined);
  }

  /** @returns the head's counts by verdict, joined with the design's separator */
  function countsLine(): string {
    const criteria = record?.criteria ?? [];
    const parts = [$t('intent.head.criteria', { criteria: criteria.length })];
    for (const status of CRITERION_STATUSES) {
      const count = criteria.filter((criterion) => criterion.status === status).length;
      if (count > 0) {
        parts.push($t('intent.head.count', { count, state: $t(`state.criterion.${status}`) }));
      }
    }
    return parts.join(' · ');
  }

  /**
   * @param criterionNumber the deliverable's number
   * @returns its number and title where the outline was read, otherwise its number
   */
  function deliverableLabel(criterionNumber: number): string {
    const deliverable = deliverables.find((entry) => entry.number === criterionNumber);
    return deliverable
      ? $t('intent.outline.deliverable', {
          number: deliverable.number,
          title: safe(deliverable.title),
        })
      : String(criterionNumber);
  }

  /**
   * @param decider the decider of a group
   * @returns the choices it made, in the record's order
   */
  function choicesOf(decider: string) {
    return (record?.decisions ?? []).filter((choice) => choice.decider === decider);
  }

  /** @returns the deciders that carry at least one choice, in the design's order */
  function deciders(): string[] {
    return DECIDER_ORDER.filter((decider) => choicesOf(decider).length > 0);
  }

  /** @returns whether the panel has a record whose revision arrived on an event */
  function changed(): boolean {
    return announcedRevision !== null && announcedRevision === record?.revision;
  }
</script>

{#if errorCode !== null}
  <div class="intent-panel" data-testid="plan-intent-panel">
    <ErrorState code={errorCode} titleLevel={2} announce={false} />
  </div>
{:else if record === null && loading}
  <div class="intent-panel" data-testid="plan-intent-panel-waiting">
    <Spinner label={$t('intent.tab.title')} />
  </div>
{:else if record === null}
  <div class="intent-panel" data-testid="plan-intent-panel">
    <p class="intent-panel__none">{$t('intent.plan.none')}</p>
  </div>
{:else}
  <section class="intent-panel" data-testid="plan-intent-panel" aria-label={$t('intent.tab.title')}>
    <h2 class="intent-panel__head" data-testid="intent-head">
      {$t('intent.head.title', { plan: safe(record.plan_id), revision: record.revision })}
    </h2>

    {#if changed()}
      <p class="intent-panel__changed" data-testid="intent-changed">
        {$t('intent.record.changed')}
      </p>
    {/if}

    <p class="intent-panel__goal" data-testid="intent-goal">
      <span class="intent-panel__label">{$t('intent.head.goal')}</span>
      <span class="intent-panel__text">{safe(record.goal)}</span>
    </p>

    <p class="intent-panel__counts" data-testid="intent-counts">
      {#if reviewed()}
        {countsLine()}
      {:else}
        {$t('state.criterion.not_reviewed')}
      {/if}
    </p>

    <h3 class="intent-panel__title">{$t('intent.criteria.title')}</h3>
    <ul class="intent-criteria">
      {#each record.criteria as criterion (criterion.id)}
        <li class="intent-criterion" id={`criterion-${criterion.id}`} data-testid={`criterion-${criterion.id}`}>
          <span class="intent-criterion__id">{criterion.id}</span>
          <span class="intent-criterion__text" data-testid={`criterion-text-${criterion.id}`}>
            {safe(criterion.text)}
          </span>

          <span class="intent-criterion__verdict" data-testid={`criterion-verdict-${criterion.id}`}>
            {#if criterion.status_reason !== undefined}
              <Button
                variant="ghost"
                size="sm"
                data-testid={`criterion-reason-${criterion.id}`}
                aria-expanded={Boolean(expanded[criterion.id])}
                on:click={() => toggle(criterion.id)}
              >
                <StatusLabel
                  tone={verdictTone(criterion.status)}
                  label={verdictWord(criterion.status)}
                />
              </Button>
            {:else}
              <StatusLabel
                tone={verdictTone(criterion.status)}
                label={verdictWord(criterion.status)}
              />
            {/if}
          </span>

          {#if criterion.status_reason !== undefined && expanded[criterion.id]}
            <p class="intent-criterion__reason" data-testid={`criterion-reason-text-${criterion.id}`}>
              {$t('intent.criteria.reason', { reason: safe(criterion.status_reason) })}
            </p>
          {/if}

          <p class="intent-criterion__satisfied" data-testid={`criterion-deliverables-${criterion.id}`}>
            {#if criterion.satisfies.length === 0}
              {$t('intent.criteria.no_deliverable')}
            {:else}
              {$t('intent.criteria.satisfied_by', { deliverables: '' })}
              {#each criterion.satisfies as criterionNumber, index (criterionNumber)}
                {#if index > 0}, {/if}<Link
                  href={deliverableHref(criterionNumber)}
                  label={deliverableLabel(criterionNumber)}
                />
              {/each}
            {/if}
          </p>
        </li>
      {/each}
    </ul>

    <h3 class="intent-panel__title">{$t('intent.non_goals.title')}</h3>
    <ul class="intent-non-goals">
      {#each record.non_goals as nonGoal (nonGoal.id)}
        <li
          class="intent-non-goal"
          data-testid={`non-goal-${nonGoal.id}`}
          data-non-goal-id={nonGoal.id}
        >
          <span class="intent-non-goal__id">{nonGoal.id}</span>
          <span class="intent-non-goal__text">{safe(nonGoal.text)}</span>
        </li>
      {/each}
    </ul>

    <h3 class="intent-panel__title">{$t('intent.choices.title')}</h3>
    {#each deciders() as decider (decider)}
      <div
        class="intent-choice-group"
        class:intent-choice-group--own-call={decider === 'role'}
        data-testid={`choice-group-${decider}`}
        data-decider={decider}
      >
        <h4 class="intent-choice-group__title">{$t(`intent.decider.${decider}`)}</h4>
        <ul class="intent-choices">
          {#each choicesOf(decider) as choice (choice.id)}
            <li class="intent-choice" data-testid={`choice-${choice.id}`}>
              <span class="intent-choice__text">{safe(choice.text)}</span>

              {#if choice.options_weighed.length > 0}
                <p class="intent-choice__options" data-testid={`choice-options-${choice.id}`}>
                  {$t('intent.choice.options', {
                    options: choice.options_weighed.map((option) => safe(option)).join(', '),
                  })}
                </p>
              {/if}

              {#if choice.question_id !== undefined}
                <p class="intent-choice__question" data-testid={`choice-question-${choice.id}`}>
                  <Link
                    href={questionHref(choice.question_id)}
                    label={$t('intent.choice.question', { question: safe(choice.question_id) })}
                  />
                </p>
              {/if}

              {#if choice.adr !== undefined}
                <p class="intent-choice__adr" data-testid={`choice-adr-${choice.id}`}>
                  {$t('intent.choice.adr', {
                    relation: choice.adr.relation,
                    adr: safe(choice.adr.adr_id),
                  })}
                </p>
              {/if}

              {#if choice.evidence !== undefined}
                <p class="intent-choice__evidence">{(
                  $t('intent.record.evidence', { evidence: safe(choice.evidence) })
                )}</p>
              {/if}
            </li>
          {/each}
        </ul>
      </div>
    {/each}

    <h3 class="intent-panel__title">{$t('intent.open_questions.title')}</h3>
    <ul class="intent-open-questions">
      {#each record.open_questions as question, index (index)}
        <li class="intent-open-question">{safe(question)}</li>
      {/each}
    </ul>

    <h3 class="intent-panel__title">{$t('intent.deferred.title')}</h3>
    <ul class="intent-deferred">
      {#each record.deferred as item (item.id)}
        <li class="intent-deferred__item" data-testid={`deferred-${item.id}`}>
          <span class="intent-deferred__id">{item.id}</span>
          <span class="intent-deferred__text">{safe(item.text)}</span>
          <span class="intent-deferred__reason">
            {$t('intent.deferred.reason', { reason: safe(item.reason) })}
          </span>
        </li>
      {/each}
    </ul>
  </section>
{/if}

<style>
  .intent-panel {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
    color: var(--color-text-primary);
  }

  .intent-panel__head {
    margin: 0;
    font-size: var(--text-base);
    line-height: var(--line-height-base);
  }

  .intent-panel__changed {
    margin: 0;
    color: var(--color-text-secondary);
  }

  .intent-panel__none {
    margin: 0;
    color: var(--color-text-secondary);
  }

  .intent-panel__label {
    margin-inline-end: var(--space-2);
    color: var(--color-text-muted);
  }

  .intent-panel__text,
  .intent-criterion__text,
  .intent-non-goal__text,
  .intent-choice__text,
  .intent-deferred__text {
    overflow-wrap: anywhere;
  }

  .intent-panel__counts,
  .intent-panel__goal {
    margin: 0;
  }

  .intent-panel__counts {
    color: var(--color-text-secondary);
  }

  .intent-panel__title {
    margin: var(--space-3) 0 0;
    font-size: var(--text-sm);
    font-weight: 600;
  }

  .intent-criteria,
  .intent-non-goals,
  .intent-choices,
  .intent-open-questions,
  .intent-deferred {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .intent-criterion {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: var(--space-2);
    padding-block-end: var(--space-2);
    border-block-end: 1px solid var(--color-border);
  }

  .intent-criterion__id,
  .intent-non-goal__id,
  .intent-deferred__id {
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .intent-criterion__text {
    flex: 1 1 16rem;
  }

  .intent-criterion__reason,
  .intent-criterion__satisfied,
  .intent-choice__options,
  .intent-choice__question,
  .intent-choice__adr,
  .intent-choice__evidence {
    flex: 1 1 100%;
    margin: 0;
    color: var(--color-text-secondary);
  }

  /* The model's own calls stand apart from the operator's: quieter, never on the same ground. */
  .intent-choice-group--own-call {
    padding-inline-start: var(--space-3);
    border-inline-start: 2px solid var(--color-border);
    color: var(--color-text-secondary);
  }

  .intent-choice-group__title {
    margin: 0;
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .intent-deferred__item {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .intent-deferred__reason {
    flex: 1 1 100%;
    color: var(--color-text-secondary);
  }
</style>
