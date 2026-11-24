<script lang="ts">
  import { untrack } from 'svelte';
  import { get } from 'svelte/store';
  import { t } from '../i18n';
  import KindMark from './KindMark.svelte';
  import RowLink from './RowLink.svelte';
  import StatusLabel from './StatusLabel.svelte';
  import Skeleton from './Skeleton.svelte';
  import ErrorState from './ErrorState.svelte';
  import { displayState } from '../display';
  import {
    apiClient,
    type ProjectDetailResponse,
    type ActivePlanSummary,
    type ActiveEpicSummary,
  } from '../api/client';
  import { runtimeStore, openPlanView, openEpicView, addressOfState } from '../stores/runtime';
  import { writeAddress } from '../navigation/history';
  import UpLink from './UpLink.svelte';
  import { upOf, buildChain } from '../navigation/chain';
  import { narrowLayout } from '../utils/narrowLayout';

  interface Props {
    projectId: string;
    currentPlanId?: string | null;
    currentEpicId?: string | null;
    /**
     * The epic of the open plan. A plan of an epic lists that epic's plans; an epic, or a plan
     * without an epic, lists the project's epics and the plans that belong to none
     * (`navigation.adoc#levels`).
     */
    parentEpicId?: string | null;
  }

  let {
    projectId,
    currentPlanId = null,
    currentEpicId = null,
    parentEpicId = null,
  }: Props = $props();

  let detail = $state<ProjectDetailResponse | null>(null);
  let isLoading = $state(false);
  let error = $state<string | null>(null);
  let loadGeneration = 0;
  let loadedProjectId: string | null = null;

  async function loadDetail(): Promise<void> {
    const currentId = projectId;
    if (!currentId) return;
    if (loadedProjectId !== currentId) {
      detail = null;
      loadedProjectId = currentId;
    }
    const gen = ++loadGeneration;
    isLoading = detail === null;
    error = null;
    try {
      const res = await apiClient.getProjectDetail(currentId);
      if (gen !== loadGeneration) return;
      detail = res;
    } catch (err: unknown) {
      if (gen !== loadGeneration) return;
      error = err instanceof Error ? err.message : 'failed';
    } finally {
      if (gen === loadGeneration) {
        isLoading = false;
      }
    }
  }

  $effect(() => {
    const id = projectId;
    if (id) {
      untrack(() => {
        void loadDetail();
      });
    }
  });

  const upParent = $derived(
    upOf(
      buildChain(
        $runtimeStore.currentRoute,
        {
          plan: $runtimeStore.currentProcessTree,
          epic: $runtimeStore.currentEpicResource,
          planId: currentPlanId || $runtimeStore.selectedPlanId,
          epicId: currentEpicId || $runtimeStore.selectedEpicId,
          taskId: $runtimeStore.selectedTaskId,
          projectId: projectId || $runtimeStore.selectedProjectId || $runtimeStore.lensProject,
          titleState: $runtimeStore,
        },
        $t,
      ),
    ),
  );

  export interface WorkRowItem {
    id: string;
    kind: 'plan' | 'epic';
    title: string;
    href: string;
    isCurrent: boolean;
    statusLabel: string;
    statusTone: 'neutral' | 'running' | 'attention' | 'danger' | 'success';
    tasksDone?: number;
    tasksTotal?: number;
  }

  function planTone(state: string): 'neutral' | 'running' | 'attention' | 'danger' | 'success' {
    switch (state) {
      case 'running':
        return 'running';
      case 'paused':
      case 'pausing':
      case 'waiting':
        return 'attention';
      case 'staged':
      default:
        return 'neutral';
    }
  }

  function epicTone(
    epic: ActiveEpicSummary,
  ): 'neutral' | 'running' | 'attention' | 'danger' | 'success' {
    return (epic.running_plans_count ?? 0) > 0 ? 'running' : 'neutral';
  }

  function toPlanItem(p: ActivePlanSummary): WorkRowItem {
    return {
      id: p.plan_id,
      kind: 'plan',
      title: p.title || p.plan_id,
      href: `/plans/${p.plan_id}`,
      isCurrent: currentPlanId === p.plan_id,
      statusLabel: displayState('plan', p.phase_state, $t),
      statusTone: planTone(p.phase_state),
      tasksDone: p.tasks_done,
      tasksTotal: p.tasks_total,
    };
  }

  function toEpicItem(e: ActiveEpicSummary): WorkRowItem {
    const isRunning = (e.running_plans_count ?? 0) > 0;
    return {
      id: e.epic_id,
      kind: 'epic',
      title: e.title || e.epic_id,
      href: `/epics/${e.epic_id}`,
      isCurrent: currentEpicId === e.epic_id,
      statusLabel: displayState('epic', isRunning ? 'running' : 'staged', $t),
      statusTone: epicTone(e),
    };
  }

  const items = $derived.by<WorkRowItem[]>(() => {
    if (!detail) return [];
    const plans = detail.plans ?? [];
    const epics = detail.epics ?? [];

    if (parentEpicId) {
      // The runtime's two views of one epic disagree in its own answer: the nested `plans` may
      // omit a plan whose flat row still names the epic (the mock serves it so). The column takes
      // both, keyed by plan id, so a sibling is never lost to the shape of the answer.
      const siblings = new Map<string, ActivePlanSummary>();
      const parent = epics.find((epic) => epic.epic_id === parentEpicId);
      for (const plan of parent?.plans ?? []) siblings.set(plan.plan_id, plan);
      for (const plan of plans) {
        if (plan.epic_id !== parentEpicId || siblings.has(plan.plan_id)) continue;
        siblings.set(plan.plan_id, plan);
      }
      return [...siblings.values()].map(toPlanItem);
    }

    return [
      ...plans.filter((plan) => !plan.epic_id).map(toPlanItem),
      ...epics.map(toEpicItem),
    ];
  });

  function onRowClick(event: MouseEvent, item: WorkRowItem): void {
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
      return;
    }
    const target = event.target as HTMLElement | null;
    if (target?.closest('button') !== null) return;
    const anchor = target?.closest('a');
    if (anchor !== null && !anchor.classList.contains('row-link__title')) return;
    event.preventDefault();
    if (item.kind === 'plan') {
      openPlanView(item.id);
    } else {
      openEpicView(item.id);
    }
    writeAddress(addressOfState(get(runtimeStore)), 'replace');
  }
</script>

<aside
  class="work-list-column"
  data-testid="work-list-column"
  aria-label={$t('overview.running.title')}
>
  <header class="work-list-column__head">
    {#if !$narrowLayout && upParent && upParent.kind && upParent.address}
      <div class="work-list-column__up">
        <UpLink kind={upParent.kind} title={upParent.title} address={upParent.address} />
      </div>
    {/if}
    <h2 class="work-list-column__title">{$t('overview.running.title')}</h2>
  </header>
  {#if isLoading}
    <div class="work-list-column__loading" data-testid="work-list-loading">
      <Skeleton shape="card" rows={3} label={$t('overview.running.title')} />
    </div>
  {:else if error && !detail}
    <div class="work-list-column__error" data-testid="work-list-error">
      <ErrorState
        code={error}
        titleLevel={3}
        actionLabel={$t('common.action.retry')}
        onAct={() => void loadDetail()}
      />
    </div>
  {:else}
    <ul class="work-list-column__list" data-testid="work-list-items">
      {#each items as item (item.id)}
        <li class="work-list-column__item" data-testid="work-item-{item.id}">
          <div
            class="work-list-column__row"
            role="presentation"
            onclick={(e) => onRowClick(e, item)}
          >
            <RowLink href={item.href} title={item.title} current={item.isCurrent}>
              {#snippet kind()}<KindMark kind={item.kind} wordHidden />{/snippet}
              {#snippet tags()}
                <StatusLabel tone={item.statusTone} label={item.statusLabel} />
                {#if item.kind === 'plan' && item.tasksTotal && item.tasksTotal > 0}
                  <span class="badge badge--meta work-list-progress" data-testid="plan-progress">
                    {item.tasksDone ?? 0}/{item.tasksTotal}
                  </span>
                {/if}
              {/snippet}
            </RowLink>
          </div>
        </li>
      {/each}
    </ul>
  {/if}
</aside>

<style>
  .work-list-column {
    display: flex;
    flex-direction: column;
    flex: 0 0 280px;
    inline-size: 280px;
    min-inline-size: 0;
    max-block-size: 100%;
    overflow-y: auto;
    overflow-x: hidden;
    padding: var(--space-4) var(--space-3);
    border-inline-end: 1px solid var(--color-border);
    background-color: var(--color-bg-surface);
  }

  .work-list-column__head {
    margin-block-end: var(--space-3);
    padding-inline: var(--space-2);
  }

  .work-list-column__up {
    margin-block-end: var(--space-2);
  }

  .work-list-column__title {
    margin: 0;
    font-size: var(--text-sm);
    font-weight: var(--weight-semibold);
    color: var(--color-text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .work-list-column__list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .work-list-column__item {
    margin: 0;
    padding: 0;
  }

  .work-list-progress {
    font-size: var(--text-xs);
    font-family: var(--font-mono);
  }

  @media (max-width: 1023px) {
    .work-list-column {
      display: none;
    }
  }
</style>
