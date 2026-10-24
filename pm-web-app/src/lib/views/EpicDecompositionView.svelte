<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import {
    apiClient,
    type DecompositionCommitResult,
    type DecompositionInstructRequest,
    type EpicDecomposition,
    type StagedPlanRecord,
  } from '../api/client';
  import { runtimeStore, navigateTo } from '../stores/runtime';

  let epicId = $derived($runtimeStore.selectedEpicId || '');
  let projectId = $derived($runtimeStore.selectedProjectId || 'pm-mcp-clients');

  let cut = $state<EpicDecomposition | null>(null);
  let isLoading = $state(true);
  let errorMessage = $state('');
  let busy = $state(false);
  let committed = $state<DecompositionCommitResult | null>(null);
  let droppedPlans = $state<string[]>([]);

  let instructOpen = $state(false);
  let instructAction = $state<'edit' | 'add' | 'rewrite'>('edit');
  let instructPlanId = $state<string | null>(null);
  let instruction = $state('');
  let readBack = $state<{ before: EpicDecomposition; after: EpicDecomposition } | null>(null);

  let unsubscribeEvents: (() => void) | null = null;
  let loadedFor = '';

  onMount(() => {
    unsubscribeEvents = apiClient.subscribeEvents((event) => {
      if (event === 'epic.decomposition_updated' || event === 'epic.launchable') {
        void load(false);
      }
    });
  });

  $effect(() => {
    if (!epicId) {
      isLoading = false;
      return;
    }
    if (epicId === loadedFor) return;
    loadedFor = epicId;
    void load(true);
  });

  onDestroy(() => {
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  async function load(initial = false) {
    if (!epicId) return;
    if (initial) isLoading = true;
    errorMessage = '';
    try {
      cut = await apiClient.getDecomposition(projectId, epicId);
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || 'error';
    } finally {
      if (initial) isLoading = false;
    }
  }

  function everyCheckPasses(value: EpicDecomposition | null): boolean {
    if (!value) return false;
    return !value.findings.some((finding) => finding.severity === 'error');
  }

  let errorFindings = $derived(cut ? cut.findings.filter((f) => f.severity === 'error') : []);

  function conflictsFor(planId: string) {
    if (!cut) return [];
    return cut.findings.filter(
      (finding) => finding.rule === 'surface_overlap' && finding.affected_plans.includes(planId),
    );
  }

  function surfaceOf(plan: StagedPlanRecord): string {
    return plan.expected_surface.length
      ? plan.expected_surface.join(', ')
      : $t('decomposition.none');
  }

  function changeMarker(plan: StagedPlanRecord): string | null {
    if (plan.status === 'staged' || plan.status === 'committed') return null;
    return $t('decomposition.change', { change: plan.status });
  }

  function openInstruct(planId: string | null, action: 'edit' | 'add' | 'rewrite') {
    instructPlanId = planId;
    instructAction = action;
    instruction = '';
    instructOpen = true;
  }

  async function submitInstruct() {
    if (!cut || instruction.trim().length === 0) return;
    busy = true;
    errorMessage = '';
    try {
      const request: DecompositionInstructRequest = {
        plan_id: instructPlanId,
        instruction: instruction.trim(),
        action: instructAction,
      };
      const after = await apiClient.instructDecomposition(projectId, epicId, request);
      readBack = { before: cut, after };
      instructOpen = false;
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || 'error';
    } finally {
      busy = false;
    }
  }

  async function acceptReadBack(): Promise<void> {
    if (!readBack) return;
    cut = readBack.after;
    readBack = null;
    // An edit can resolve a check: re-run the server's checks so the commit gate follows the
    // changed plan instead of the findings of the cut before the edit.
    busy = true;
    errorMessage = '';
    try {
      cut = await apiClient.checkDecomposition(projectId, epicId);
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || 'error';
    } finally {
      busy = false;
    }
  }

  function discardReadBack() {
    readBack = null;
  }

  function keep(planId: string) {
    droppedPlans = droppedPlans.filter((id) => id !== planId);
  }

  function drop(planId: string) {
    if (!droppedPlans.includes(planId)) {
      droppedPlans = [...droppedPlans, planId];
    }
  }

  async function agree() {
    if (!cut) return;
    busy = true;
    errorMessage = '';
    try {
      const checked = await apiClient.checkDecomposition(projectId, epicId);
      cut = checked;
      if (everyCheckPasses(checked)) {
        committed = await apiClient.commitDecomposition(projectId, epicId);
      }
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || 'error';
    } finally {
      busy = false;
    }
  }

  interface DiffRow {
    label: string;
    before: string;
    after: string;
  }

  let readBackRows = $derived.by<DiffRow[]>(() => {
    if (!readBack) return [];
    const planId = instructPlanId ?? firstChangedPlan(readBack.before, readBack.after);
    if (!planId) return [];
    const before = planOf(readBack.before, planId);
    const after = planOf(readBack.after, planId);
    const rows: DiffRow[] = [];
    const pairs: Array<[string, string, string]> = [
      [$t('decomposition.objective'), before?.objective ?? '', after?.objective ?? ''],
      [
        $t('decomposition.surface'),
        before ? before.expected_surface.join(', ') : '',
        after ? after.expected_surface.join(', ') : '',
      ],
      [
        $t('decomposition.depends_on'),
        before ? before.dependencies.join(', ') : '',
        after ? after.dependencies.join(', ') : '',
      ],
    ];
    for (const [label, oldValue, newValue] of pairs) {
      if (oldValue !== newValue) rows.push({ label, before: oldValue, after: newValue });
    }
    return rows;
  });

  function planOf(value: EpicDecomposition, planId: string): StagedPlanRecord | null {
    for (const workstream of value.workstreams) {
      for (const plan of workstream.staged_plans) {
        if (plan.plan_id === planId) return plan;
      }
    }
    return null;
  }

  function firstChangedPlan(before: EpicDecomposition, after: EpicDecomposition): string | null {
    for (const workstream of after.workstreams) {
      for (const plan of workstream.staged_plans) {
        const old = planOf(before, plan.plan_id);
        if (
          !old ||
          old.objective !== plan.objective ||
          old.expected_surface.join(',') !== plan.expected_surface.join(',') ||
          old.dependencies.join(',') !== plan.dependencies.join(',')
        ) {
          return plan.plan_id;
        }
      }
    }
    return after.workstreams[0]?.staged_plans[0]?.plan_id ?? null;
  }

  let erroredPlans = $derived(new Set(errorFindings.flatMap((finding) => finding.affected_plans)));

  function backToEpic() {
    navigateTo('epic');
  }

  function countPlans(value: EpicDecomposition): number {
    return value.workstreams.reduce((sum, workstream) => sum + workstream.staged_plans.length, 0);
  }

  function launchablePlans(value: EpicDecomposition): string[] {
    const ids: string[] = [];
    for (const workstream of value.workstreams) {
      for (const plan of workstream.staged_plans) {
        if (plan.dependencies.length === 0) ids.push(plan.plan_id);
      }
    }
    return ids;
  }
</script>

<div class="decomposition" data-testid="epic-decomposition">
  {#if isLoading}
    <div class="decomposition__loading">
      <Spinner size="2.5rem" label={$t('decomposition.loading')} />
    </div>
  {:else if errorMessage}
    <div class="decomposition__error" role="alert" data-testid="decomposition-error">
      <p>{$t('decomposition.error', { cause: errorMessage })}</p>
      <Button variant="secondary" onclick={() => load(true)}>{$t('web.common.retry')}</Button>
    </div>
  {:else if cut}
    <header class="decomposition__header">
      <h1 class="decomposition__title" data-testid="decomposition-title">
        {$t('decomposition.title', { epic: cut.epic_id })}
      </h1>
      <div class="decomposition__head-actions">
        <span class="badge" data-testid="decomposition-status">{cut.status}</span>
        <Button variant="ghost" onclick={backToEpic} data-testid="decomposition-back">
          {$t('common.actions.back')}
        </Button>
      </div>
    </header>

    {#if errorFindings.length > 0}
      <div class="conflict-banner" role="alert" data-testid="conflict-banner">
        <strong>{$t('decomposition.checks_fail', { count: errorFindings.length })}</strong>
        <ul>
          {#each errorFindings as finding}
            <li data-testid="conflict-finding">{finding.message}</li>
          {/each}
        </ul>
      </div>
    {/if}

    {#if committed}
      <div class="committed" role="status" data-testid="decomposition-committed">
        {$t('decomposition.committed', {
          epic: cut.epic_id,
          workstreams: cut.workstreams.length,
          plans: countPlans(cut),
        })}
        <span class="committed__then">
          {$t('decomposition.then', { mode: 'manual', plans: launchablePlans(cut).join(', ') })}
        </span>
      </div>
    {/if}

    <section class="workstreams">
      {#each cut.workstreams as workstream (workstream.id)}
        <div class="workstream" data-testid="workstream-{workstream.id}">
          <h2 class="workstream__title">
            {$t('decomposition.workstream', { id: workstream.id, name: workstream.name })}
          </h2>
          <div class="plans">
            {#each workstream.staged_plans as plan (plan.plan_id)}
              <div
                class="plan"
                class:plan--error={erroredPlans.has(plan.plan_id)}
                class:plan--dropped={droppedPlans.includes(plan.plan_id)}
                data-testid="plan-{plan.plan_id}"
              >
                <div class="plan__head">
                  <span class="plan__title">{plan.title}</span>
                  {#if plan.status === 'locked'}
                    <span class="plan__tag" data-testid="plan-locked"
                      >{$t('decomposition.locked')}</span
                    >
                  {:else if changeMarker(plan)}
                    <span class="plan__tag" data-testid="plan-change">{changeMarker(plan)}</span>
                  {/if}
                  {#if droppedPlans.includes(plan.plan_id)}
                    <span class="plan__tag plan__tag--dropped"
                      >{$t('decomposition.change', { change: 'dropped' })}</span
                    >
                  {/if}
                </div>
                <div class="plan__surface" data-testid="plan-surface-{plan.plan_id}">
                  <span class="plan__label">{$t('decomposition.surface')}</span>
                  <span class="mono">{surfaceOf(plan)}</span>
                </div>
                {#if plan.dependencies.length > 0}
                  <div class="plan__depends">
                    <span class="plan__label">{$t('decomposition.depends_on')}</span>
                    <span class="mono">{plan.dependencies.join(', ')}</span>
                  </div>
                {/if}
                {#each conflictsFor(plan.plan_id) as conflict (conflict.message)}
                  <div class="plan__conflict" data-testid="plan-conflict-{plan.plan_id}">
                    {conflict.message}
                  </div>
                {/each}
                <div class="plan__actions">
                  <button
                    type="button"
                    data-testid="plan-keep-{plan.plan_id}"
                    onclick={() => keep(plan.plan_id)}
                  >
                    {$t('decomposition.keep')}
                  </button>
                  <button
                    type="button"
                    data-testid="plan-edit-{plan.plan_id}"
                    onclick={() => openInstruct(plan.plan_id, 'edit')}
                  >
                    {$t('decomposition.edit')}
                  </button>
                  <button
                    type="button"
                    data-testid="plan-drop-{plan.plan_id}"
                    onclick={() => drop(plan.plan_id)}
                  >
                    {$t('decomposition.drop')}
                  </button>
                </div>
              </div>
            {/each}
          </div>
        </div>
      {/each}
    </section>

    {#if readBack}
      <div class="read-back" data-testid="read-back">
        <h3>{$t('decomposition.read_back.title', { plan: instructPlanId ?? '—' })}</h3>
        {#if readBackRows.length === 0}
          <p class="text-muted">{$t('decomposition.none')}</p>
        {:else}
          <ul>
            {#each readBackRows as row (row.label)}
              <li class="read-back__row" data-testid="read-back-row">
                <span class="read-back__label">{row.label}</span>
                <span class="read-back__before">− {row.before || $t('decomposition.none')}</span>
                <span class="read-back__after">+ {row.after || $t('decomposition.none')}</span>
              </li>
            {/each}
          </ul>
        {/if}
        <div class="read-back__actions">
          <Button onclick={acceptReadBack} data-testid="read-back-accept"
            >{$t('decomposition.accept')}</Button
          >
          <button type="button" data-testid="read-back-discard" onclick={discardReadBack}>
            {$t('decomposition.discard')}
          </button>
        </div>
      </div>
    {/if}

    <footer class="decomposition__footer">
      <button type="button" data-testid="add-plan" onclick={() => openInstruct(null, 'add')}>
        + {$t('decomposition.add')}
      </button>
      <button type="button" data-testid="draft-again" onclick={() => openInstruct(null, 'rewrite')}>
        {$t('decomposition.draft_again')}
      </button>
      <span class="checks" data-testid="decomposition-checks">
        {everyCheckPasses(cut)
          ? $t('decomposition.checks_pass')
          : $t('decomposition.checks_fail', { count: errorFindings.length })}
      </span>
      <Button
        variant="primary"
        disabled={!everyCheckPasses(cut) || busy}
        onclick={agree}
        data-testid="agree-commit"
      >
        {$t('decomposition.agree')}
      </Button>
    </footer>
  {/if}
</div>

{#if instructOpen}
  <div class="instruct-modal" data-testid="instruct-modal" role="dialog" aria-modal="true">
    <div class="instruct-modal__panel">
      <h2>
        {$t('decomposition.instruct.title', { plan: instructPlanId ?? $t('decomposition.add') })}
      </h2>
      <textarea
        class="instruct-modal__input"
        data-testid="instruct-input"
        bind:value={instruction}
        rows="3"
      ></textarea>
      <div class="instruct-modal__actions">
        <Button
          onclick={submitInstruct}
          disabled={busy || instruction.trim().length === 0}
          data-testid="instruct-submit"
        >
          {$t('decomposition.instruct.prompt')}
        </Button>
        <button type="button" data-testid="instruct-cancel" onclick={() => (instructOpen = false)}>
          {$t('decomposition.discard')}
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  .decomposition {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    padding: var(--space-4) var(--space-6);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
    min-height: 100%;
  }

  .decomposition__loading,
  .decomposition__error {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-4);
    padding: var(--space-8);
  }

  .decomposition__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-4);
    flex-wrap: wrap;
  }

  .decomposition__title {
    font-size: var(--text-xl, 1.25rem);
    font-weight: 700;
    margin: 0;
  }

  .decomposition__head-actions {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .conflict-banner {
    background-color: var(--color-warning-subtle);
    border: 1px solid var(--color-warning-border);
    border-radius: var(--radius-md);
    padding: var(--space-3) var(--space-4);
    font-size: var(--text-sm);
  }

  .conflict-banner ul {
    margin: var(--space-1) 0 0;
    padding-left: var(--space-4);
  }

  .committed {
    background-color: var(--color-success-subtle);
    border-radius: var(--radius-md);
    padding: var(--space-3) var(--space-4);
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .committed__then {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .workstreams {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .workstream__title {
    font-size: var(--text-base);
    font-weight: 600;
    margin: 0 0 var(--space-2);
  }

  .plans {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: var(--space-2);
  }

  .plan {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md, 6px);
    padding: var(--space-3);
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .plan--error {
    border-color: var(--color-danger);
  }

  .plan--dropped {
    opacity: 0.55;
  }

  .plan__head {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .plan__title {
    font-weight: 600;
    font-size: var(--text-sm);
  }

  .plan__tag {
    font-size: var(--text-xs);
    padding: 1px 6px;
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
  }

  .plan__tag--dropped {
    background-color: var(--color-danger-subtle);
  }

  .plan__label {
    color: var(--color-text-muted);
    font-size: var(--text-xs);
    margin-right: var(--space-1);
  }

  .plan__conflict {
    color: var(--color-danger-text);
    font-size: var(--text-xs);
  }

  .plan__actions {
    display: flex;
    gap: var(--space-2);
    margin-top: var(--space-1);
  }

  .plan__actions button {
    min-height: 32px;
    cursor: pointer;
  }

  .read-back {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    padding: var(--space-3) var(--space-4);
  }

  .read-back ul {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .read-back__row {
    display: flex;
    gap: var(--space-3);
    flex-wrap: wrap;
    font-size: var(--text-sm);
  }

  .read-back__label {
    font-weight: 600;
    min-width: 8rem;
  }

  .read-back__before {
    color: var(--color-danger-text);
  }

  .read-back__after {
    color: var(--color-success-text);
  }

  .read-back__actions {
    display: flex;
    gap: var(--space-2);
    margin-top: var(--space-2);
  }

  .decomposition__footer {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    flex-wrap: wrap;
    border-top: 1px solid var(--color-border);
    padding-top: var(--space-3);
  }

  .decomposition__footer .checks {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    margin-left: auto;
  }

  .decomposition__footer button {
    min-height: 44px;
    cursor: pointer;
  }

  .instruct-modal {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: var(--color-overlay);
    z-index: 50;
  }

  .instruct-modal__panel {
    width: 100%;
    max-width: 32rem;
    margin: var(--space-3);
    padding: var(--space-4);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-surface);
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .instruct-modal__input {
    width: 100%;
    min-height: 4rem;
    padding: var(--space-2);
  }

  .instruct-modal__actions {
    display: flex;
    gap: var(--space-2);
  }

  .mono {
    font-family: monospace;
  }

  .text-muted {
    color: var(--color-text-muted);
  }

  @media (max-width: 480px) {
    .decomposition {
      padding: var(--space-3);
    }

    .instruct-modal__panel {
      margin: 0;
      border-radius: 0;
      min-height: 100%;
    }
  }
</style>
