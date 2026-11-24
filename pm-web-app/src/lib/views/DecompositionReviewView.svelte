<script lang="ts">
  import { t } from '../i18n';
  import Icon from '../components/Icon.svelte';
  import Button from '../components/Button.svelte';
  import Compose from '../components/Compose.svelte';
  import KeyValueList, { type KeyValueItem } from '../components/KeyValueList.svelte';
  import type {
    DecompositionCheckFinding,
    EpicDecomposition,
    StagedPlanRecord,
    WorkstreamRecord,
  } from '../api/client';

  export let decomposition: EpicDecomposition | null = null;
  export let readBack: {
    planId: string;
    before: StagedPlanRecord | null;
    after: StagedPlanRecord;
  } | null = null;
  export let busy = false;
  export let onKeep: (planId: string) => void = () => {};
  export let onInstruct: (planId: string | null, instruction: string) => void = () => {};
  export let onAccept: () => void = () => {};
  export let onInstructAgain: () => void = () => {};
  export let onDiscard: () => void = () => {};
  export let onDraftAgain: (instruction: string) => void = () => {};
  export let onAgree: () => void = () => {};
  export let onChat: () => void = () => {};

  let editing: string | null = null;
  let adding = false;
  let instruction = '';
  let drafting = false;

  $: failures = (decomposition?.findings ?? []).filter(
    (finding: DecompositionCheckFinding) => finding.severity === 'error',
  );
  $: plans = (decomposition?.workstreams ?? []).flatMap((stream) => stream.staged_plans);

  /**
   * Sends the instruction the person typed, and nothing else: the objective is the plan's own and
   * is what the runtime keeps, so the message stays the one the base sent.
   */
  function submitInstruct(): void {
    const text = instruction.trim();
    if (!text) {
      return;
    }
    onInstruct(editing, text);
    instruction = '';
    editing = null;
    adding = false;
  }

  function submitDraft(): void {
    if (!instruction.trim()) return;
    onDraftAgain(instruction.trim());
    instruction = '';
    drafting = false;
  }

  function rowsOf(stream: WorkstreamRecord): StagedPlanRecord[] {
    return stream.staged_plans;
  }

  function failed(planId: string): boolean {
    return failures.some((finding) => finding.affected_plans.includes(planId));
  }

  function findingText(
    finding: DecompositionCheckFinding,
    planId: string,
    translate: typeof $t,
  ): string {
    if (finding.rule === 'surface_overlap' && finding.affected_plans.length >= 2) {
      const other = finding.affected_plans.find((candidate) => candidate !== planId) ?? '';
      return translate('start.decomposition.overlap', {
        plan: other,
        paths: finding.message,
      });
    }
    return finding.message;
  }

  function planDetails(plan: StagedPlanRecord, translate: typeof $t): KeyValueItem[] {
    return [
      {
        label: translate('start.decomposition.objective'),
        value: plan.objective || translate('start.decomposition.none'),
      },
      {
        label: translate('start.decomposition.surface'),
        value:
          plan.expected_surface && plan.expected_surface.length > 0
            ? plan.expected_surface.join(', ')
            : translate('start.decomposition.none'),
      },
      {
        label: translate('start.decomposition.depends_on'),
        value:
          plan.dependencies && plan.dependencies.length > 0
            ? plan.dependencies.join(', ')
            : translate('start.decomposition.none'),
      },
    ];
  }
</script>

<section class="decomposition-review" data-testid="decomposition-review">
  <h2 class="decomposition-review__title" data-testid="decomposition-title">
    {$t('start.decomposition.title', { epic: decomposition?.epic_id ?? '' })}
  </h2>

  {#if readBack}
    <section class="decomposition-review__read-back" data-testid="decomposition-read-back">
      <h3>{$t('start.decomposition.read_back.title', { plan: readBack.planId })}</h3>
      <dl>
        {#if readBack.before}
          <dt>{$t('start.decomposition.edit')}</dt>
          <dd class="decomposition-review__old" data-testid="read-back-before">
            − {readBack.before.title}
            {#if readBack.before.objective}
              <span> · {readBack.before.objective}</span>
            {/if}
          </dd>
        {/if}
        <dt>{$t('start.decomposition.keep')}</dt>
        <dd class="decomposition-review__new" data-testid="read-back-after">
          + {readBack.after.title}
          {#if readBack.after.objective}
            <span> · {readBack.after.objective}</span>
          {/if}
          <span> · {readBack.after.expected_surface.join(', ')}</span>
        </dd>
      </dl>
      <div class="decomposition-review__actions">
        <Button variant="primary" data-testid="read-back-accept" disabled={busy} onclick={onAccept}>
          {$t('start.decomposition.accept')}
        </Button>
        <Button
          variant="secondary"
          data-testid="read-back-instruct"
          disabled={busy}
          onclick={onInstructAgain}
        >
          {$t('start.decomposition.instruct_again')}
        </Button>
        <Button variant="ghost" data-testid="read-back-discard" disabled={busy} onclick={onDiscard}>
          {$t('start.decomposition.discard')}
        </Button>
      </div>
    </section>
  {:else}
    <div class="decomposition-review__workstreams">
      {#each decomposition?.workstreams ?? [] as stream (stream.id)}
        <section class="decomposition-review__stream" data-testid={`workstream-${stream.id}`}>
          <h3 class="decomposition-review__stream-title">
            {stream.id} · {stream.name}
            <span class="decomposition-review__count">
              {$t('start.decomposition.plans', { count: stream.staged_plans.length })}
            </span>
          </h3>
          <ul class="decomposition-review__rows">
            {#each rowsOf(stream) as plan (plan.plan_id)}
              <li class="decomposition-review__row" data-testid={`plan-${plan.plan_id}`}>
                <div class="decomposition-review__row-top">
                  <span class="decomposition-review__mark" aria-hidden="true">
                    <Icon
                      name={plan.status === 'dropped'
                        ? 'state.dot'
                        : failed(plan.plan_id)
                          ? 'state.error'
                          : 'state.check'}
                      size={16}
                    />
                  </span>
                  <span class="decomposition-review__plan-title">{plan.title}</span>
                  {#if plan.status === 'dropped'}
                    <span
                      class="decomposition-review__status"
                      data-testid={`plan-status-${plan.plan_id}`}
                    >
                      {$t('start.decomposition.change', { change: plan.status })}
                    </span>
                  {/if}
                  <span
                    class="decomposition-review__surface"
                    data-testid={`plan-surface-${plan.plan_id}`}
                  >
                    {plan.expected_surface.join(', ')}
                  </span>
                  {#if failed(plan.plan_id)}
                    <span
                      class="decomposition-review__conflict"
                      data-testid={`plan-conflict-${plan.plan_id}`}
                    >
                      {#each failures.filter((finding) => finding.affected_plans.includes(plan.plan_id)) as finding}
                        {findingText(finding, plan.plan_id, $t)}
                      {/each}
                    </span>
                  {/if}
                  <span class="decomposition-review__row-actions">
                    <Button
                      variant="secondary"
                      size="sm"
                      data-testid={`plan-keep-${plan.plan_id}`}
                      disabled={busy}
                      onclick={() => onKeep(plan.plan_id)}
                    >
                      {$t('start.decomposition.keep')}
                    </Button>
                    <Button
                      variant="secondary"
                      size="sm"
                      data-testid={`plan-edit-${plan.plan_id}`}
                      disabled={busy}
                      onclick={() => {
                        editing = plan.plan_id;
                        adding = false;
                        instruction = '';
                      }}
                    >
                      {$t('start.decomposition.edit')}
                    </Button>
                  </span>
                </div>
                <div class="decomposition-review__plan-details" data-testid={`plan-details-${plan.plan_id}`}>
                  <KeyValueList items={planDetails(plan, $t)} />
                </div>
              </li>
            {/each}
          </ul>
        </section>
      {/each}
    </div>

    {#if editing || adding}
      <section class="decomposition-review__instruction" data-testid="decomposition-instruction">
        <h3>
          {$t('start.decomposition.instruct.title', {
            plan: editing ?? $t('start.decomposition.new_plan'),
          })}
        </h3>
        {#if editing}
          <p class="decomposition-review__plan-title">{editing}</p>
        {/if}
        <Compose
          bind:value={instruction}
          label={$t('start.decomposition.instruct_prompt')}
          sendLabel={$t('start.decomposition.send')}
          disabledReason={$t('start.decomposition.instruct_held')}
          inputTestid="instruct-input"
          submitTestid="instruct-submit"
          disabled={busy || !instruction.trim()}
          onsend={submitInstruct}
        />
        <div class="decomposition-review__actions">
          <Button
            variant="ghost"
            data-testid="instruct-cancel"
            disabled={busy}
            onclick={() => {
              editing = null;
              adding = false;
            }}
          >
            {$t('common.action.close')}
          </Button>
        </div>
      </section>
    {/if}

    {#if drafting}
      <section class="decomposition-review__instruction" data-testid="draft-again-panel">
        <h3>{$t('start.decomposition.draft_again')}</h3>
        <Compose
          bind:value={instruction}
          label={$t('start.decomposition.draft_again')}
          sendLabel={$t('start.decomposition.send_draft')}
          disabledReason={$t('start.decomposition.instruct_held')}
          disabled={busy || !instruction.trim()}
          onsend={submitDraft}
        />
        <div class="decomposition-review__actions">
          <Button
            variant="ghost"
            data-testid="draft-cancel"
            disabled={busy}
            onclick={() => {
              drafting = false;
            }}
          >
            {$t('common.action.close')}
          </Button>
        </div>
      </section>
    {/if}

    <div class="decomposition-review__footer">
      <Button
        variant="secondary"
        data-testid="decomposition-add"
        disabled={busy}
        onclick={() => {
          adding = true;
          editing = null;
          instruction = '';
        }}
      >
        {$t('start.decomposition.add')}
      </Button>
      <Button variant="secondary" data-testid="decomposition-chat" disabled={busy} onclick={onChat}>
        {$t('start.decomposition.chat')}
      </Button>
      <Button
        variant="secondary"
        data-testid="decomposition-draft-again"
        disabled={busy}
        onclick={() => {
          drafting = true;
          instruction = '';
        }}
      >
        {$t('start.decomposition.draft_again')}
      </Button>
      <Button
        variant="primary"
        data-testid="agree-commit"
        disabled={busy || failures.length > 0}
        disabledReason={failures.length > 0
          ? $t('start.decomposition.checks_fail', { count: failures.length })
          : undefined}
        onclick={onAgree}
      >
        {$t('start.decomposition.agree')}
      </Button>
      <span class="decomposition-review__checks" data-testid="decomposition-checks">
        {failures.length > 0
          ? $t('start.decomposition.checks_fail', { count: failures.length })
          : $t('start.decomposition.checks_pass')}
      </span>
    </div>

    {#if plans.length === 0}
      <p class="decomposition-review__empty" data-testid="decomposition-empty">
        {$t('start.decomposition.plans', { count: 0 })}
      </p>
    {/if}
  {/if}
</section>

<style>
  .decomposition-review {
    display: flex;
    flex-direction: column;
    gap: var(--space-3, 0.75rem);
  }

  .decomposition-review__stream {
    margin-bottom: var(--space-4, 1rem);
  }

  .decomposition-review__stream-title {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-base);
    font-weight: 600;
  }

  .decomposition-review__count {
    color: var(--color-text-muted);
    font-weight: 400;
  }

  .decomposition-review__rows {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .decomposition-review__row {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-2);
    border-bottom: 1px solid var(--color-border);
  }

  .decomposition-review__row-top {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-1) var(--space-2);
  }

  .decomposition-review__plan-details {
    padding-inline-start: var(--space-6);
  }

  .decomposition-review__mark {
    width: 1rem;
    text-align: center;
  }

  .decomposition-review__plan-title {
    font-weight: 600;
  }

  .decomposition-review__surface,
  .decomposition-review__status,
  .decomposition-review__checks,
  .decomposition-review__empty {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
    overflow-wrap: anywhere;
  }

  .decomposition-review__conflict {
    color: var(--color-danger-text);
    flex-basis: 100%;
  }

  .decomposition-review__row-actions,
  .decomposition-review__actions,
  .decomposition-review__footer {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .decomposition-review__old,
  .decomposition-review__new {
    margin: 0;
    color: var(--color-text-primary);
  }

  .decomposition-review dl {
    margin: 0;
  }

  @media (max-width: 640px) {
    .decomposition-review__surface {
      flex-basis: 100%;
    }
  }
</style>
