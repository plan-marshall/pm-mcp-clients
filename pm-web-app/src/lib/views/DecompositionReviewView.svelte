<script lang="ts">
  import { t } from '../i18n';
  import Icon from '../components/Icon.svelte';
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
  export let onDrop: (planId: string) => void = () => {};
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

  function submitInstruct(): void {
    if (!instruction.trim()) return;
    onInstruct(editing, instruction.trim());
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

  function findingText(finding: DecompositionCheckFinding, planId: string): string {
    if (finding.rule === 'surface_overlap' && finding.affected_plans.length >= 2) {
      const other = finding.affected_plans.find((candidate) => candidate !== planId) ?? '';
      return $t('start.decomposition.overlap', {
        plan: other,
        paths: finding.message,
      });
    }
    return finding.message;
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
            {readBack.before.title}
            {#if readBack.before.objective}
              <span> · {readBack.before.objective}</span>
            {/if}
          </dd>
        {/if}
        <dt>{$t('start.decomposition.keep')}</dt>
        <dd data-testid="read-back-after">
          {readBack.after.title}
          {#if readBack.after.objective}
            <span> · {readBack.after.objective}</span>
          {/if}
          <span> · {readBack.after.expected_surface.join(', ')}</span>
        </dd>
      </dl>
      <div class="decomposition-review__actions">
        <button type="button" data-testid="read-back-accept" disabled={busy} on:click={onAccept}>
          {$t('start.decomposition.accept')}
        </button>
        <button
          type="button"
          data-testid="read-back-instruct"
          disabled={busy}
          on:click={onInstructAgain}
        >
          {$t('start.decomposition.instruct_again')}
        </button>
        <button type="button" data-testid="read-back-discard" disabled={busy} on:click={onDiscard}>
          {$t('start.decomposition.discard')}
        </button>
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
                <span class="decomposition-review__mark" aria-hidden="true">
                  <Icon name={failed(plan.plan_id) ? 'state.error' : 'state.check'} size={16} />
                </span>
                <span class="decomposition-review__plan-title">{plan.title}</span>
                {#if plan.objective}
                  <span class="decomposition-review__objective">{plan.objective}</span>
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
                    {#each failures.filter( (finding) => finding.affected_plans.includes(plan.plan_id) ) as finding}
                      {findingText(finding, plan.plan_id)}
                    {/each}
                  </span>
                {/if}
                <span class="decomposition-review__row-actions">
                  <button
                    type="button"
                    data-testid={`plan-keep-${plan.plan_id}`}
                    disabled={busy}
                    on:click={() => onKeep(plan.plan_id)}
                  >
                    {$t('start.decomposition.keep')}
                  </button>
                  <button
                    type="button"
                    data-testid={`plan-edit-${plan.plan_id}`}
                    disabled={busy}
                    on:click={() => {
                      editing = plan.plan_id;
                      adding = false;
                      instruction = '';
                    }}
                  >
                    {$t('start.decomposition.edit')}
                  </button>
                  <button
                    type="button"
                    data-testid={`plan-drop-${plan.plan_id}`}
                    disabled={busy}
                    on:click={() => onDrop(plan.plan_id)}
                  >
                    {$t('start.decomposition.drop')}
                  </button>
                </span>
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
            plan: editing ?? $t('start.decomposition.add'),
          })}
        </h3>
        <textarea data-testid="instruct-input" bind:value={instruction} disabled={busy} rows="3"
        ></textarea>
        <div class="decomposition-review__actions">
          <button
            type="button"
            data-testid="instruct-submit"
            disabled={busy || !instruction.trim()}
            on:click={submitInstruct}
          >
            {$t('start.decomposition.edit')}
          </button>
          <button
            type="button"
            data-testid="instruct-cancel"
            disabled={busy}
            on:click={() => {
              editing = null;
              adding = false;
            }}
          >
            {$t('web.common.close')}
          </button>
        </div>
      </section>
    {/if}

    {#if drafting}
      <section class="decomposition-review__instruction" data-testid="draft-again-panel">
        <h3>{$t('start.decomposition.draft_again')}</h3>
        <textarea data-testid="draft-input" bind:value={instruction} disabled={busy} rows="3"
        ></textarea>
        <div class="decomposition-review__actions">
          <button
            type="button"
            data-testid="draft-submit"
            disabled={busy || !instruction.trim()}
            on:click={submitDraft}
          >
            {$t('start.decomposition.draft_again')}
          </button>
        </div>
      </section>
    {/if}

    <div class="decomposition-review__footer">
      <button
        type="button"
        data-testid="decomposition-add"
        disabled={busy}
        on:click={() => {
          adding = true;
          editing = null;
          instruction = '';
        }}
      >
        {$t('start.decomposition.add')}
      </button>
      <button type="button" data-testid="decomposition-chat" disabled={busy} on:click={onChat}>
        {$t('start.decomposition.chat')}
      </button>
      <button
        type="button"
        data-testid="decomposition-draft-again"
        disabled={busy}
        on:click={() => {
          drafting = true;
          instruction = '';
        }}
      >
        {$t('start.decomposition.draft_again')}
      </button>
      <button
        type="button"
        data-testid="agree-commit"
        disabled={busy || failures.length > 0}
        on:click={onAgree}
      >
        {$t('start.decomposition.agree')}
      </button>
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

  .decomposition-review__title {
    margin: 0;
    font-size: var(--text-lg, 1.125rem);
  }

  .decomposition-review__stream,
  .decomposition-review__instruction,
  .decomposition-review__read-back {
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
  }

  .decomposition-review__stream-title {
    margin: 0;
    font-size: var(--text-base, 1rem);
  }

  .decomposition-review__count {
    margin-left: var(--space-2);
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .decomposition-review__rows {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .decomposition-review__row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-1) var(--space-2);
    padding: var(--space-1) var(--space-2);
    border-bottom: 1px solid var(--color-border);
  }

  .decomposition-review__mark {
    width: 1rem;
    text-align: center;
  }

  .decomposition-review__plan-title {
    font-weight: 600;
  }

  .decomposition-review__objective,
  .decomposition-review__surface,
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

  .decomposition-review textarea {
    width: 100%;
    min-height: 44px;
    padding: var(--space-1);
  }

  .decomposition-review button {
    min-height: 44px;
    padding: var(--space-1) var(--space-3);
    cursor: pointer;
  }

  .decomposition-review__old {
    text-decoration: line-through;
    color: var(--color-text-muted);
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
