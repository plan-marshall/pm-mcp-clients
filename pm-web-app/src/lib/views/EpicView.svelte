<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import { apiClient, type EpicProcess, type EpicProcessPlan } from '../api/client';
  import { runtimeStore, openPlanView, navigateTo } from '../stores/runtime';
  import { formatSplit } from '../utils/tokenFormat';

  let epicId = $derived($runtimeStore.selectedEpicId || '');
  let epic = $state<EpicProcess | null>(null);
  let isLoading = $state(true);
  let errorMessage = $state('');

  let unsubscribeEvents: (() => void) | null = null;

  onMount(async () => {
    unsubscribeEvents = apiClient.subscribeEvents((event) => {
      if (
        event === 'project.work_started' ||
        event === 'project.work_ended' ||
        event === 'project.changed' ||
        event === 'phase.changed' ||
        event === 'epic.event.landed' ||
        event === 'epic.event.overlap'
      ) {
        void loadEpicProcess(false);
      }
    });

    await loadEpicProcess(true);
  });

  onDestroy(() => {
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  async function loadEpicProcess(initial = false) {
    if (!epicId) return;
    if (initial) isLoading = true;
    errorMessage = '';
    try {
      epic = await apiClient.getEpicProcess(epicId);
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || 'Failed to load epic process';
    } finally {
      if (initial) isLoading = false;
    }
  }

  function goBackToOverview() {
    navigateTo('overview');
  }

  function openPlan(pId: string) {
    openPlanView(pId);
  }
</script>

<div class="epic-view" data-testid="epic-view">
  {#if isLoading}
    <div class="epic-view__loading" data-testid="epic-loading">
      <Spinner size="2.5rem" label="Loading epic process…" />
    </div>
  {:else if errorMessage}
    <div class="epic-view__error" role="alert" data-testid="epic-error">
      <p>{errorMessage}</p>
      <Button variant="secondary" onclick={() => loadEpicProcess(true)}>Retry</Button>
    </div>
  {:else if epic}
    <!-- Path Navigation Header -->
    <header class="epic-header" data-testid="epic-header">
      <div class="epic-header__top">
        <nav class="epic-breadcrumb mono" aria-label="Breadcrumb" data-testid="epic-breadcrumb">
          <button
            class="breadcrumb-link"
            onclick={goBackToOverview}
            data-testid="breadcrumb-project"
          >
            {epic.project}
          </button>
          <span class="breadcrumb-sep">›</span>
          <span class="breadcrumb-current font-semibold" data-testid="breadcrumb-epic">
            {epic.epic_id}
          </span>
        </nav>

        <div class="epic-header__actions">
          {#if epic.review_ready}
            <span class="badge badge--success" data-testid="review-ready-badge">
              {$t('epic.review.ready')}
            </span>
          {/if}
          <Button variant="ghost" onclick={goBackToOverview} data-testid="back-btn">
            {$t('common.actions.back')}
          </Button>
        </div>
      </div>

      <!-- Epic Title and Headline -->
      <div class="epic-headline">
        <div class="epic-title-group">
          <span class="work-glyph">◇</span>
          <h1 class="epic-title" data-testid="epic-title">{epic.title}</h1>
        </div>
        <div class="epic-status-tags">
          <span class="badge badge--kind badge--epic">{$t('overview.kind.epic')}</span>
          <span class="badge badge--state badge--state-{epic.state}" data-testid="epic-state">
            {epic.state}
          </span>
        </div>
      </div>

      <!-- Tokens Summary -->
      {#if epic.tokens_so_far && !epic.tokens_so_far.none_yet}
        <div class="epic-tokens mono" data-testid="epic-tokens">
          <span class="tokens-label">{$t('overview.epic.tokens')}:</span>
          <span class="tokens-value">
            {formatSplit(
              epic.tokens_so_far.total,
              epic.tokens_so_far.input,
              epic.tokens_so_far.output,
              epic.tokens_so_far.floor,
              $currentLocale,
            )}
          </span>
        </div>
      {/if}
    </header>

    <!-- Main Content -->
    <main class="epic-content">
      <!-- Running Plans -->
      <section class="plans-section" data-testid="running-plans-section">
        <h2 class="section-title">
          {$t('epic.plans.running')}
          <span class="section-count">({epic.running_plans ? epic.running_plans.length : 0})</span>
        </h2>
        {#if !epic.running_plans || epic.running_plans.length === 0}
          <div class="plans-empty text-muted">{$t('epic.plans.none')}</div>
        {:else}
          <div class="plans-grid">
            {#each epic.running_plans as plan}
              <button
                class="plan-card work-card work-card--running"
                onclick={() => openPlan(plan.plan_id)}
                data-testid="running-plan-{plan.plan_id}"
              >
                <div class="plan-card__head">
                  <span class="plan-glyph">▶</span>
                  <span class="plan-title font-semibold">{plan.title}</span>
                  <span class="badge badge--phase">{plan.current_phase}</span>
                  <span class="badge badge--state badge--state-{plan.phase_state}"
                    >{plan.phase_state}</span
                  >
                </div>
                <div class="plan-card__progress">
                  <span>
                    {$t('overview.progress.tasks', {
                      done: plan.tasks_done,
                      total: plan.tasks_total,
                    })}
                  </span>
                </div>
              </button>
            {/each}
          </div>
        {/if}
      </section>

      <!-- Staged Plans -->
      <section class="plans-section" data-testid="staged-plans-section">
        <h2 class="section-title">
          {$t('epic.plans.staged')}
          <span class="section-count">({epic.staged_plans ? epic.staged_plans.length : 0})</span>
        </h2>
        {#if !epic.staged_plans || epic.staged_plans.length === 0}
          <div class="plans-empty text-muted">{$t('epic.plans.none')}</div>
        {:else}
          <div class="plans-grid">
            {#each epic.staged_plans as plan}
              <button
                class="plan-card work-card work-card--staged"
                onclick={() => openPlan(plan.plan_id)}
                data-testid="staged-plan-{plan.plan_id}"
              >
                <div class="plan-card__head">
                  <span class="plan-glyph">○</span>
                  <span class="plan-title font-semibold">{plan.title}</span>
                  <span class="badge badge--state badge--state-staged"
                    >{plan.phase_state || 'staged'}</span
                  >
                </div>
                <div class="plan-card__progress">
                  <span>
                    {$t('overview.progress.tasks', {
                      done: plan.tasks_done,
                      total: plan.tasks_total,
                    })}
                  </span>
                </div>
              </button>
            {/each}
          </div>
        {/if}
      </section>

      <!-- Alongside Landings -->
      {#if epic.alongside_landings && epic.alongside_landings.length > 0}
        <section class="alongside-section" data-testid="epic-alongside-section">
          <h2 class="section-title">{$t('overview.epic.landings')}</h2>
          <div class="alongside-list">
            {#each epic.alongside_landings as landing}
              <div class="alongside-item mono" data-testid="alongside-item">
                {landing}
              </div>
            {/each}
          </div>
        </section>
      {/if}
    </main>
  {/if}
</div>

<style>
  .epic-view {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: calc(100vh - 4rem);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .epic-view__loading,
  .epic-view__error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: var(--space-8);
    gap: var(--space-4);
  }

  .epic-header {
    background-color: var(--color-bg-surface);
    border-bottom: 1px solid var(--color-border);
    padding: var(--space-4) var(--space-6);
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .epic-header__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-4);
    flex-wrap: wrap;
  }

  .epic-breadcrumb {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
  }

  .breadcrumb-link {
    background: none;
    border: none;
    padding: 0;
    color: var(--color-accent);
    cursor: pointer;
    text-decoration: underline;
    font-size: inherit;
    font-family: inherit;
  }

  .breadcrumb-link:hover {
    color: var(--color-accent-hover, #2563eb);
  }

  .breadcrumb-sep {
    color: var(--color-text-muted);
  }

  .breadcrumb-current {
    color: var(--color-text-primary);
  }

  .epic-headline {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-4);
    flex-wrap: wrap;
  }

  .epic-title-group {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  .work-glyph {
    font-size: 1.25rem;
    color: var(--color-accent);
  }

  .epic-title {
    font-size: var(--text-xl, 1.25rem);
    font-weight: 700;
    margin: 0;
  }

  .epic-status-tags {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .epic-tokens {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    display: flex;
    gap: var(--space-2);
  }

  .tokens-label {
    color: var(--color-text-muted);
  }

  .tokens-value {
    font-weight: 500;
  }

  .epic-content {
    padding: var(--space-6);
    display: flex;
    flex-direction: column;
    gap: var(--space-6);
    overflow-y: auto;
  }

  .plans-section {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .section-title {
    font-size: var(--text-base);
    font-weight: 600;
    margin: 0;
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .section-count {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .plans-empty {
    padding: var(--space-4);
    font-size: var(--text-sm);
  }

  .plans-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: var(--space-3);
  }

  .plan-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md, 6px);
    padding: var(--space-3) var(--space-4);
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    cursor: pointer;
    text-align: left;
    transition:
      transform 0.1s ease,
      box-shadow 0.1s ease;
  }

  .plan-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
    border-color: var(--color-accent);
  }

  .plan-card__head {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .plan-glyph {
    font-family: monospace;
    font-weight: bold;
    color: var(--color-accent);
  }

  .plan-title {
    font-size: var(--text-sm);
    flex: 1;
  }

  .plan-card__progress {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .alongside-section {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .alongside-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .alongside-item {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border-subtle);
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-sm, 4px);
    font-size: var(--text-xs);
  }
</style>
