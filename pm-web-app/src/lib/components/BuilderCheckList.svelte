<script lang="ts">
  import { t } from '../i18n';
  import type { BuilderCheckRecord, BuilderQuestionRecord } from '../api/client';
  import { stripControlSequences } from '../utils/outputSafety';
  import Marker from './Marker.svelte';

  /**
   * The builder check (design `intent-and-outline.adoc#builder`): the questions a builder reads the
   * record as someone who must build from it would still need answered, each sorted by who can
   * answer it and each with its resolution.
   *
   * The check adds evidence and never holds a plan: a `not_run` check says so with the plan's own
   * reason, as evidence and never as a pass or a hold. A question the builder marked carries the
   * design system's `recommended` marker in its own sort group of this panel — the marker word names
   * a property of the question, and the record carries no recommended answer, because its `answer`
   * is the operator's and exists only once the question is answered. Nothing is preselected: the
   * record carries no answer until the operator gives one.
   */
  interface Props {
    /** The check the plan's intent carries, or `null` before it ran. */
    check: BuilderCheckRecord | null;
  }

  let { check }: Props = $props();

  /** The sorts in the order the design lists them. */
  const SORTS = ['answered', 'minor', 'operator'] as const;

  /**
   * @param sort the sort of a question
   * @returns the questions of that sort, in the record's order
   */
  function questionsOf(sort: string): BuilderQuestionRecord[] {
    return (check?.questions ?? []).filter((question) => question.sort === sort);
  }

  /** @returns the sorts that carry at least one question, in the design's order */
  function sorts(): string[] {
    return SORTS.filter((sort) => questionsOf(sort).length > 0);
  }

  /** @param value a record text @returns it as plain text, never as markup */
  function safe(value: string | null | undefined): string {
    return stripControlSequences(value);
  }
</script>

<section class="builder-check" data-testid="builder-check" aria-label={$t('intent.builder.title')}>
  <h3 class="builder-check__title">{$t('intent.builder.title')}</h3>

  {#if check !== null}
    <p class="builder-check__state" data-testid="builder-state">
      {#if check.resolution === 'not_run'}
        {$t('intent.builder.state.not_run', { reason: safe(check.reason) })}
      {:else if check.resolution === 'answered'}
        {$t('intent.builder.state.answered')}
      {:else}
        {$t('intent.builder.state.pending')}
      {/if}
    </p>

    {#each sorts() as sort (sort)}
      <div class="builder-check__group" data-testid={`builder-sort-${sort}`} data-sort={sort}>
        <h4 class="builder-check__sort">{$t(`intent.builder.sort.${sort}`)}</h4>
        <ul class="builder-check__questions">
          {#each questionsOf(sort) as question (question.id)}
            <li class="builder-check__question" data-testid={`builder-question-${question.id}`}>
              <span class="builder-check__text">{safe(question.text)}</span>
              {#if question.recommended}
                <Marker kind="recommended" />
              {/if}
              <span
                class="builder-check__resolution"
                data-testid={`builder-resolution-${question.id}`}
              >
                {#if question.state === 'answered'}
                  {$t('intent.builder.resolution.answered', { answer: safe(question.answer) })}
                {:else if question.state === 'deferred'}
                  {$t('intent.builder.resolution.deferred')}
                {:else}
                  {$t('intent.builder.resolution.open')}
                {/if}
              </span>
            </li>
          {/each}
        </ul>
      </div>
    {/each}
  {/if}
</section>

<style>
  .builder-check {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
    color: var(--color-text-primary);
  }

  .builder-check__title,
  .builder-check__sort {
    margin: 0;
    font-size: var(--text-sm);
    font-weight: 600;
  }

  .builder-check__state {
    margin: 0;
    color: var(--color-text-secondary);
  }

  .builder-check__questions {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .builder-check__question {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: var(--space-2);
  }

  .builder-check__text {
    flex: 1 1 16rem;
    overflow-wrap: anywhere;
  }

  .builder-check__resolution {
    color: var(--color-text-secondary);
  }
</style>
