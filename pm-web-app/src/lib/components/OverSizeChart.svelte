<script lang="ts">
  import { t } from '../i18n';
  import type { OverSizeBucketRecord, OverSizeDistributionRecord } from '../api/client';
  import { stripControlSequences } from '../utils/outputSafety';

  /**
   * One role's `over_size` distribution (design `intent-and-outline.adoc#oversize`, `#buckets`,
   * `#web`): the buckets in the runtime's order as a small bar chart, the last one open-ended, the
   * buckets a proposed multiple would flag marked, and the counts, the flagged share and the
   * direction in text — so the chart reads without colour and at phone width.
   *
   * The record is the runtime's (`OverSizeDistributionResource` of `pm-api`, per project and role): a
   * bucket is never recomputed from raw tokens, and the empty bucket the runtime may still send is
   * left out rather than drawn as a zero bar. The chart is read-only — no act proposes or changes a
   * multiple, because no client designs one yet — and a role without measured tasks shows its empty
   * text instead of bars.
   */
  interface Props {
    /** The role's distribution, as the runtime serves it. */
    distribution: OverSizeDistributionRecord;
  }

  let { distribution }: Props = $props();

  /**
   * @param value a multiple
   * @returns it without a trailing zero, as the design writes it (`3x`, not `3.0x`)
   */
  function multiple(value: number): string {
    return String(value);
  }

  /** @returns whether the runtime serves a proposal to compare against */
  function proposed(): boolean {
    return distribution.proposed_multiple !== undefined;
  }

  /** @returns the chart's title: the proposal beside the multiple in force, or the one in force */
  function title(): string {
    return proposed()
      ? $t('intent.oversize.title', {
          role: stripControlSequences(distribution.role),
          current: multiple(distribution.current_multiple),
          proposed: multiple(distribution.proposed_multiple ?? distribution.current_multiple),
        })
      : $t('intent.oversize.title_current', {
          role: stripControlSequences(distribution.role),
          current: multiple(distribution.current_multiple),
        });
  }

  /**
   * @returns the buckets that carry a task with the index they hold in the record, so an empty one
   *     is left out without moving the open-ended bucket's place
   */
  function rows(): { bucket: OverSizeBucketRecord; index: number }[] {
    return distribution.buckets
      .map((bucket, index) => ({ bucket, index }))
      .filter((row) => row.bucket.task_count > 0);
  }

  /**
   * @param index the bucket's index in the record
   * @returns whether it is the last one, which the design shows open-ended
   */
  function openEnded(index: number): boolean {
    return index === distribution.buckets.length - 1;
  }

  /**
   * @param bucket the bucket
   * @param index  its index in the record
   * @returns its bounds, or the open-ended word for the last bucket
   */
  function label(bucket: OverSizeBucketRecord, index: number): string {
    return openEnded(index)
      ? $t('intent.oversize.bucket_open', { from: multiple(bucket.from_multiple) })
      : $t('intent.oversize.bucket', {
          from: multiple(bucket.from_multiple),
          to: multiple(bucket.to_multiple),
        });
  }

  /** @returns the tasks of every bucket, the empty ones included: it is the distribution's size */
  function totalTasks(): number {
    return distribution.buckets.reduce((sum, bucket) => sum + bucket.task_count, 0);
  }

  /** @returns the tasks the proposal would have flagged, `0` without one */
  function flaggedTasks(): number {
    return proposed()
      ? distribution.buckets.reduce((sum, bucket) => sum + bucket.flagged_tasks, 0)
      : 0;
  }

  /**
   * @returns the flagged share as a whole percentage, the one rounding rule of the chart
   */
  function percent(): number {
    const total = totalTasks();
    return total === 0 ? 0 : Math.round((flaggedTasks() / total) * 100);
  }

  /** @returns the largest bucket's count, which sets the full width of a bar */
  function largest(): number {
    return distribution.buckets.reduce((max, bucket) => Math.max(max, bucket.task_count), 0);
  }

  /**
   * @param bucket the bucket
   * @returns its share of the largest bucket as the bar's own length
   */
  function share(bucket: OverSizeBucketRecord): string {
    const max = largest();
    return max === 0 ? '0%' : `${(bucket.task_count / max) * 100}%`;
  }

  /**
   * @returns which way the proposal moves the multiple, or `null` where it moves nothing and the
   *     words of the direction would claim a change that is not there
   */
  function direction(): 'lower' | 'raise' | null {
    const proposedMultiple = distribution.proposed_multiple;
    if (proposedMultiple === undefined || proposedMultiple === distribution.current_multiple) {
      return null;
    }
    return proposedMultiple < distribution.current_multiple ? 'lower' : 'raise';
  }
</script>

<section class="oversize" data-testid="over-size-chart" data-role={distribution.role}>
  <h4 class="oversize__title" data-testid="oversize-title">{title()}</h4>

  {#if totalTasks() === 0}
    <p class="oversize__empty" data-testid="oversize-empty">{$t('intent.oversize.empty')}</p>
  {:else}
    <ul class="oversize__buckets">
      {#each rows() as row (row.index)}
        <li
          class="oversize-bucket"
          class:oversize-bucket--flagged={row.bucket.flagged_tasks > 0}
          data-testid={`oversize-bucket-${row.index}`}
          data-bucket-index={row.index}
        >
          <span class="oversize-bucket__label">{label(row.bucket, row.index)}</span>
          <span class="oversize-bucket__track" aria-hidden="true">
            <span class="oversize-bucket__bar" style:inline-size={share(row.bucket)}></span>
          </span>
          <span class="oversize-bucket__count">
            {$t('intent.oversize.tasks', { count: row.bucket.task_count })}
          </span>
          {#if row.bucket.flagged_tasks > 0}
            <span class="oversize-bucket__flagged" data-testid={`oversize-flagged-${row.index}`}>
              {$t('intent.oversize.flagged', { count: row.bucket.flagged_tasks })}
            </span>
          {/if}
        </li>
      {/each}
    </ul>

    {#if proposed()}
      <p class="oversize__share" data-testid="oversize-share">
        {$t('intent.oversize.share', {
          tasks: totalTasks(),
          flagged: flaggedTasks(),
          percent: percent(),
        })}
      </p>
      {#if direction() !== null}
        <p class="oversize__direction" data-testid="oversize-direction">
          {$t('intent.oversize.direction', {
            direction: direction(),
            current: multiple(distribution.current_multiple),
            proposed: multiple(distribution.proposed_multiple ?? distribution.current_multiple),
          })}
        </p>
      {/if}
    {/if}
  {/if}
</section>

<style>
  .oversize {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
    color: var(--color-text-primary);
  }

  .oversize__title {
    margin: 0;
    font-size: var(--text-sm);
    font-weight: 600;
    overflow-wrap: anywhere;
  }

  .oversize__empty,
  .oversize__share,
  .oversize__direction {
    margin: 0;
    color: var(--color-text-secondary);
    overflow-wrap: anywhere;
  }

  .oversize__buckets {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .oversize-bucket {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-1) var(--space-2);
  }

  .oversize-bucket__label {
    flex: 0 0 4rem;
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  /* The track is the widest bucket; a bar carries only its own length. */
  .oversize-bucket__track {
    flex: 1 1 6rem;
    min-inline-size: 2rem;
    block-size: var(--space-2);
    border-radius: var(--radius-full);
    background-color: var(--color-bg-subtle);
  }

  .oversize-bucket__bar {
    display: block;
    block-size: 100%;
    border-radius: var(--radius-full);
    background-color: var(--color-brand);
  }

  /* A marked bucket carries the tone and, in the word beside it, what the mark means. */
  .oversize-bucket--flagged .oversize-bucket__bar {
    background-color: var(--color-attention);
  }

  .oversize-bucket__count,
  .oversize-bucket__flagged {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    white-space: nowrap;
  }

  .oversize-bucket__flagged {
    font-weight: 600;
  }
</style>
