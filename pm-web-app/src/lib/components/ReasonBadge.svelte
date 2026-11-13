<script lang="ts" module>
  /**
   * The reasons of the closed list (shared/needs-and-find.adoc, The Kinds of an Item Are Reasons),
   * plus `completion_review` for an epic whose completion review waits for the operator
   * (components-tags.adoc, reason-badge). They are the runtime's order.
   */
  export type Reason =
    | 'decision'
    | 'waiver'
    | 'waits_for_you'
    | 'stalled'
    | 'condition'
    | 'integrity_pause'
    | 'held'
    | 'landing_failed'
    | 'findings'
    | 'setup'
    | 'credential'
    | 'cleanup_due'
    | 'completion_review';

  /** The closed list of reasons, in the order the runtime serves them. */
  export const REASONS: readonly Reason[] = Object.freeze([
    'decision',
    'waiver',
    'waits_for_you',
    'stalled',
    'condition',
    'integrity_pause',
    'held',
    'landing_failed',
    'findings',
    'setup',
    'credential',
    'cleanup_due',
    'completion_review',
  ]);

  /** The tone of a badge: amber for every reason, red for the two that stop the work as it is. */
  export type ReasonTone = 'attention' | 'danger';

  /**
   * The reasons drawn in the danger tone: the two that mean the work cannot go on as it is
   * (`components-tags.adoc`, reason-badge). The wire value of the integrity pause is
   * `integrity_pause` (`NeedsOperatorKind`); a reason outside {@link REASONS} draws the catalogue's
   * fallback word in the attention tone, never this one.
   */
  export const DANGER_REASONS: readonly string[] = Object.freeze([
    'landing_failed',
    'integrity_pause',
  ]);

  /**
   * @param reason the reason of the object
   * @returns the tone the badge draws when the caller names none
   */
  export function toneOf(reason: string): ReasonTone {
    return DANGER_REASONS.includes(reason) ? 'danger' : 'attention';
  }

  /**
   * @param reason the reason a caller hands in
   * @returns whether the closed list knows it
   */
  export function isKnownReason(reason: string): reason is Reason {
    return (REASONS as readonly string[]).includes(reason);
  }
</script>

<script lang="ts">
  import { t } from '../i18n';

  /**
   * Why an object needs the operator: a pill (`--radius-full`) that starts with `!` and names the
   * reason, or the number of reasons and the first of them.
   *
   * Its text is the one key `overview.needs.badge`, whose `!` is the catalogue's, and the reason's
   * word is `overview.needs.<reason>`. A reason the catalogue does not know renders the catalogue's
   * fallback word in the attention tone, so a runtime that serves a reason this client predates
   * still renders a row.
   */
  interface Props {
    /** The reason why the object waits. */
    reason: string;
    /** How many reasons the object has; the badge names the first of them. */
    count?: number;
    /** The tone; derived from the reason when absent. */
    tone?: ReasonTone;
  }

  let { reason, count = 1, tone }: Props = $props();

  const known = $derived(isKnownReason(reason));
  const resolvedTone = $derived(tone ?? toneOf(reason));
</script>

<span class="reason-badge reason-badge--{resolvedTone}">
  {#if known}
    {$t('overview.needs.badge', { count, reason: $t(`overview.needs.${reason}`) })}
  {:else}
    {$t('overview.needs.badge', { count, reason: $t('overview.needs.other') })}
  {/if}
</span>

<style>
  .reason-badge {
    display: inline-flex;
    align-items: center;
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-full);
    font-size: var(--text-xs);
    font-weight: var(--weight-medium);
    line-height: var(--line-height-xs);
    white-space: nowrap;
  }

  /* The amber pair every reason draws, audited in tokens.test.ts (theme-readability.adoc). */
  .reason-badge--attention {
    background-color: var(--color-attention-subtle);
    color: var(--color-attention-text);
  }

  /* The red pair of the two reasons that mean the work cannot go on as it is. */
  .reason-badge--danger {
    background-color: var(--color-danger-subtle);
    color: var(--color-danger);
  }
</style>
