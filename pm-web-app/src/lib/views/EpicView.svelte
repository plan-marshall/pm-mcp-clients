<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import WorkControlPanel from '../components/WorkControlPanel.svelte';
  import FindingsReviewModal from '../components/FindingsReviewModal.svelte';
  import ChatPanel from '../components/ChatPanel.svelte';
  import {
    apiClient,
    type ChatReadBack,
    type EpicProcess,
    type EpicResource,
    type EpicWorkRow,
    type ReviewResource,
    type TokenFigure,
  } from '../api/client';
  import {
    runtimeStore,
    openPlanView,
    openProjectInExplorer,
    openEpicDecomposition,
    goBack,
    navigateTo,
  } from '../stores/runtime';
  import { formatSplit, formatTokens } from '../utils/tokenFormat';

  let epicId = $derived($runtimeStore.selectedEpicId || '');
  let epic = $state<EpicProcess | null>(null);
  let resource = $state<EpicResource | null>(null);
  let isLoading = $state(true);
  let errorMessage = $state('');

  let unsubscribeEvents: (() => void) | null = null;

  let reviewRegister = $state<'findings' | 'watches'>('findings');
  let review = $state<ReviewResource | null>(null);
  let reviewOpen = $state(false);
  let reviewBusy = $state(false);
  let reviewNotice = $state('');

  let reviewChat = $state<ChatReadBack | null>(null);
  let reviewChatOpen = $state(false);
  let reviewChatBusy = $state(false);

  onMount(async () => {
    unsubscribeEvents = apiClient.subscribeEvents((event) => {
      if (
        event === 'project.work_started' ||
        event === 'project.work_ended' ||
        event === 'project.changed' ||
        event === 'phase.changed' ||
        event === 'epic.event.landed' ||
        event === 'epic.event.overlap' ||
        event === 'hold.changed' ||
        event === 'hold.cleared'
      ) {
        void loadEpic(false);
      }
    });

    await loadEpic(true);
  });

  onDestroy(() => {
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  async function loadEpic(initial = false) {
    if (!epicId) return;
    if (initial) isLoading = true;
    errorMessage = '';
    const [process, epicResource] = await Promise.allSettled([
      apiClient.getEpicProcess(epicId),
      apiClient.getEpicResource(epicId),
    ]);
    if (process.status === 'fulfilled') {
      epic = process.value;
    } else if (epicResource.status !== 'fulfilled') {
      errorMessage = $t('epic.view.load_failed');
    }
    resource = epicResource.status === 'fulfilled' ? epicResource.value : null;
    if (initial) isLoading = false;
  }

  /**
   * Goes back to the view the epic was opened from, and to Running Work when it has no caller.
   */
  function goBackToCaller() {
    if (!goBack()) {
      navigateTo('overview');
    }
  }

  function openPlan(pId: string) {
    openPlanView(pId);
  }

  function openProject(projectId: string) {
    openProjectInExplorer(projectId);
  }

  function decompose() {
    openEpicDecomposition(epicId, resource?.project_id ?? epic?.project ?? '');
  }

  /** The readable label of a row's status, with the wire value as the last resort. */
  function rowStatus(status: string): string {
    const known = [
      'shipped',
      'staged',
      'running',
      'pausing',
      'paused',
      'waiting',
      'held',
      'handed_back',
      'cancelled',
      'abandoned',
      'ended',
    ];
    return known.includes(status) ? $t(`epic.views.row.${status}`) : status;
  }

  /** What a row waits for, where it waits for anything. */
  function waitLabel(row: EpicWorkRow): string {
    const other = row.wait_plan_id ?? '';
    switch (row.wait_reason) {
      case 'dependency':
        return $t('epic.views.wait.dependency', { plan: row.plan_id, other });
      case 'lock':
        return $t('epic.views.wait.lock', { plan: row.plan_id });
      case 'scope':
        return $t('epic.views.wait.scope', { plan: row.plan_id });
      case 'overlap':
        return $t('epic.views.wait.overlap', { plan: row.plan_id, other });
      case 'stale':
        return $t('epic.views.wait.stale', { plan: row.plan_id });
      case 'held':
        return $t('epic.views.wait.held', { plan: row.plan_id });
      default:
        return '';
    }
  }

  /** The plan the epic would start next: the first staged row that waits for nothing. */
  function nextPlanId(): string {
    if (!resource) return '';
    for (const workstream of resource.workstreams) {
      for (const row of workstream.rows) {
        if (row.status === 'staged' && (!row.wait_reason || row.wait_reason === 'none')) {
          return row.plan_id;
        }
      }
    }
    return '';
  }

  function figureLine(figure: TokenFigure): string {
    if (!figure.measured) {
      return $t('common.tokens.not_measured');
    }
    return formatSplit(figure.total, figure.input, figure.output, figure.floor, $currentLocale);
  }

  function unmeasuredLine(count: number): string {
    if (count <= 0) return '';
    return $t('epic.views.not_measured', { count });
  }

  async function openReview(register: 'findings' | 'watches') {
    reviewRegister = register;
    reviewNotice = '';
    try {
      review = await apiClient.getEpicReview(epicId, register);
      reviewOpen = true;
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
      reviewOpen = true;
      review = null;
    }
  }

  function reasonOf(err: unknown): string {
    return err instanceof Error ? err.message : String(err);
  }

  async function keepItem(itemId: string, as: 'finding' | 'watch') {
    reviewBusy = true;
    reviewNotice = '';
    try {
      review = await apiClient.keepReviewItem(epicId, reviewRegister, itemId, as);
      await loadEpic(false);
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
    } finally {
      reviewBusy = false;
    }
  }

  async function dropItem(itemId: string, reason: string) {
    reviewBusy = true;
    reviewNotice = '';
    try {
      review = await apiClient.dropReviewItem(epicId, reviewRegister, itemId, reason);
      await loadEpic(false);
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
    } finally {
      reviewBusy = false;
    }
  }

  async function promoteItem(itemId: string, instruction?: string) {
    reviewBusy = true;
    reviewNotice = '';
    try {
      review = await apiClient.promoteReviewItem(epicId, reviewRegister, itemId, instruction);
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
    } finally {
      reviewBusy = false;
    }
  }

  async function editItem(itemId: string, instruction: string) {
    reviewBusy = true;
    reviewNotice = '';
    try {
      review = await apiClient.editReviewItem(epicId, reviewRegister, itemId, instruction);
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
    } finally {
      reviewBusy = false;
    }
  }

  async function acceptReadBack() {
    reviewBusy = true;
    try {
      review = await apiClient.acceptReviewReadBack(epicId, reviewRegister);
      await loadEpic(false);
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
    } finally {
      reviewBusy = false;
    }
  }

  async function discardReadBack() {
    reviewBusy = true;
    try {
      review = await apiClient.discardReviewReadBack(epicId, reviewRegister);
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
    } finally {
      reviewBusy = false;
    }
  }

  async function stageDraft() {
    reviewBusy = true;
    try {
      review = await apiClient.stageReviewPlan(epicId, reviewRegister);
      await loadEpic(false);
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
    } finally {
      reviewBusy = false;
    }
  }

  async function acceptRemaining() {
    if (!review) return;
    reviewBusy = true;
    reviewNotice = '';
    try {
      for (const item of review.items.filter((entry) => entry.state === 'open')) {
        const as = reviewRegister === 'findings' ? 'finding' : 'watch';
        review = await apiClient.keepReviewItem(epicId, reviewRegister, item.item_id, as);
      }
      await loadEpic(false);
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
    } finally {
      reviewBusy = false;
    }
  }

  async function commitReview() {
    reviewBusy = true;
    reviewNotice = '';
    try {
      review = await apiClient.commitReview(epicId, reviewRegister);
      reviewOpen = false;
      await loadEpic(false);
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
    } finally {
      reviewBusy = false;
    }
  }

  async function openReviewChat() {
    reviewChatBusy = true;
    try {
      reviewChat = await apiClient.getChat(`epic:${epicId}`);
      reviewChatOpen = true;
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
    } finally {
      reviewChatBusy = false;
    }
  }

  async function sendReviewChat(text: string) {
    reviewChatBusy = true;
    try {
      reviewChat = await apiClient.chatWithSubject(`epic:${epicId}`, text);
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
    } finally {
      reviewChatBusy = false;
    }
  }

  async function answerReviewProposal(accept: boolean) {
    reviewChatBusy = true;
    try {
      reviewChat = await apiClient.answerSubjectChatProposal(`epic:${epicId}`, accept);
    } catch (err: unknown) {
      reviewNotice = $t('control.review.failed', { reason: reasonOf(err) });
    } finally {
      reviewChatBusy = false;
    }
  }

  async function endReviewChat() {
    reviewChatOpen = false;
    reviewChat = null;
  }
</script>

<div class="epic-view" data-testid="epic-view">
  {#if isLoading}
    <div class="epic-view__loading" data-testid="epic-loading">
      <Spinner size="2.5rem" label={$t('epic.view.loading')} />
    </div>
  {:else if errorMessage}
    <div class="epic-view__error" role="alert" data-testid="epic-error">
      <p>{errorMessage}</p>
      <Button variant="secondary" onclick={() => loadEpic(true)}>{$t('web.common.retry')}</Button>
    </div>
  {:else if epic}
    <!-- Path Navigation Header -->
    <header class="epic-header" data-testid="epic-header">
      <div class="epic-header__top">
        <nav
          class="epic-breadcrumb mono"
          aria-label={$t('common.breadcrumb')}
          data-testid="epic-breadcrumb"
        >
          <button
            class="breadcrumb-link"
            onclick={() => openProject(resource?.project_id ?? epic!.project)}
            data-testid="breadcrumb-project"
          >
            {resource?.display_name ?? epic.project}
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
          <Button variant="ghost" onclick={goBackToCaller} data-testid="back-btn">
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
          {#if resource}
            <span class="badge badge--mode" data-testid="epic-mode">
              {$t('epic.views.mode', {
                mode: resource.auto_start ? $t('epic.views.mode_on') : $t('epic.views.mode_off'),
              })}
            </span>
            <span class="badge badge--counts" data-testid="epic-scope">
              {$t('overview.epic.plans', {
                running: resource.running,
                staged: resource.workstreams.reduce(
                  (count, workstream) =>
                    count + workstream.rows.filter((row) => row.status === 'staged').length,
                  0,
                ),
              })}
            </span>
          {/if}
        </div>
      </div>

      <!-- The epic's own actions, holds, and chats -->
      <WorkControlPanel
        subjectId={epic.epic_id}
        subjectKind="epic"
        projectId={resource?.project_id ?? epic.project}
        ended={resource?.state === 'ended'}
        scope={resource?.scope ?? 0}
        running={resource?.running ?? 0}
        nextPlan={nextPlanId()}
        autoStart={resource?.auto_start ?? null}
        onReview={(register) => void openReview(register)}
        onDecompose={decompose}
        onChanged={() => void loadEpic(false)}
      />

      <!-- Tokens Summary -->
      {#if resource && resource.tokens.total.measured}
        <div class="epic-tokens mono" data-testid="epic-tokens">
          <span class="tokens-label">{$t('overview.epic.tokens')}:</span>
          <span class="tokens-value">{figureLine(resource.tokens.total)}</span>
          {#if resource.tokens.total.not_measured_tasks > 0}
            <span class="tokens-unmeasured" data-testid="epic-tokens-unmeasured">
              {unmeasuredLine(resource.tokens.total.not_measured_tasks)}
            </span>
          {/if}
        </div>
      {:else if epic.tokens_so_far && !epic.tokens_so_far.none_yet}
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
      {#if resource}
        <!-- Workstreams and member plans -->
        <section class="plans-section" data-testid="epic-workstreams">
          <h2 class="section-title">{$t('epic.views.workstreams')}</h2>
          {#each resource.workstreams as workstream (workstream.id)}
            <div class="workstream" data-testid={`workstream-${workstream.id}`}>
              <h3 class="workstream__title">{workstream.name}</h3>
              <div class="workstream__rows">
                {#each workstream.rows as row (row.plan_id)}
                  <div class="workstream__row" data-testid={`epic-row-${row.plan_id}`}>
                    <button
                      type="button"
                      class="workstream__plan"
                      onclick={() => openPlan(row.plan_id)}
                    >
                      <span class="plan-glyph">{row.status === 'staged' ? '○' : '▶'}</span>
                      <span class="plan-title font-semibold">{row.title}</span>
                    </button>
                    <span class="badge badge--state badge--state-{row.status}">
                      {rowStatus(row.status)}
                    </span>
                    {#if waitLabel(row)}
                      <span class="workstream__wait" data-testid={`wait-${row.plan_id}`}>
                        {waitLabel(row)}
                      </span>
                    {/if}
                    {#if row.pull_request_number}
                      <a
                        class="pr-link mono"
                        href={`#pr-${row.pull_request_number}`}
                        data-testid={`epic-pr-${row.plan_id}`}
                        onclick={(event) => event.preventDefault()}
                      >
                        {$t('epic.views.pr', { number: row.pull_request_number })}
                      </a>
                    {/if}
                  </div>
                {/each}
              </div>
            </div>
          {/each}
        </section>

        <!-- Sources and landings -->
        <section class="epic-facts" data-testid="epic-sources">
          <h2 class="section-title">{$t('overview.epic.sources')}</h2>
          {#if resource.sources.length > 0}
            <ul class="fact-list">
              {#each resource.sources as source}
                <li>{source}</li>
              {/each}
            </ul>
          {:else}
            <p class="text-muted">{$t('epic.plans.none')}</p>
          {/if}
        </section>

        <section class="epic-facts" data-testid="epic-landings">
          <h2 class="section-title">{$t('overview.epic.landings')}</h2>
          <p data-testid="epic-landing-count">
            {$t('epic.views.landing_count', { count: resource.landings })}
          </p>
        </section>

        <!-- Registers -->
        <section class="epic-registers" data-testid="epic-registers">
          <div class="register" data-testid="epic-findings">
            <h2 class="section-title">{$t('overview.epic.findings')}</h2>
            <p class="register__counts">
              {$t('overview.epic.findings_counts', {
                new: resource.findings.new,
                open: resource.findings.open,
                kept: resource.findings.kept,
              })}
            </p>
            <button
              type="button"
              class="review-open"
              data-testid="open-findings-review"
              onclick={() => void openReview('findings')}
            >
              {$t('epic.views.review_open')}
            </button>
          </div>
          <div class="register" data-testid="epic-watches">
            <h2 class="section-title">{$t('overview.epic.watches')}</h2>
            <p class="register__counts">
              {$t('overview.epic.watches_counts', {
                open: resource.watches.open,
                kept: resource.watches.kept,
              })}
            </p>
            <button
              type="button"
              class="review-open"
              data-testid="open-watches-review"
              onclick={() => void openReview('watches')}
            >
              {$t('epic.views.review_open')}
            </button>
          </div>
        </section>

        <!-- Tokens per plan and the epic's own -->
        <section class="epic-tokens-detail" data-testid="epic-token-breakdown">
          <h2 class="section-title">{$t('overview.epic.tokens')}</h2>
          {#if !resource.tokens.not_measured}
            <dl class="token-facts">
              <dt>{$t('epic.views.tokens_own')}</dt>
              <dd data-testid="epic-own-tokens">{figureLine(resource.tokens.own)}</dd>
              {#each resource.tokens.plans as plan (plan.plan_id)}
                <dt class="mono">{plan.plan_id}</dt>
                <dd data-testid={`epic-plan-tokens-${plan.plan_id}`}>
                  {figureLine(plan.figure)}
                </dd>
              {/each}
            </dl>
          {:else}
            <p class="text-muted" data-testid="epic-tokens-not-measured">
              {$t('epic.views.not_measured_all')}
              {#if resource.tokens.not_measured_count > 0}
                {unmeasuredLine(resource.tokens.not_measured_count)}
              {/if}
            </p>
          {/if}
        </section>

        <!-- Events -->
        <section class="epic-events" data-testid="epic-events">
          <h2 class="section-title">{$t('overview.epic.events')}</h2>
          {#if resource.events.length > 0}
            <ol class="event-list">
              {#each resource.events as event}
                <li class="event-item">
                  <span class="event-time mono">{event.at}</span>
                  <span class="event-text">{event.text}</span>
                </li>
              {/each}
            </ol>
          {:else}
            <p class="text-muted">{$t('epic.views.no_events')}</p>
          {/if}
        </section>
      {:else}
        <!-- Running Plans -->
        <section class="plans-section" data-testid="running-plans-section">
          <h2 class="section-title">
            {$t('epic.plans.running')}
            <span class="section-count">({epic.running_plans ? epic.running_plans.length : 0})</span
            >
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
      {/if}
    </main>
  {/if}
</div>

<FindingsReviewModal
  {epicId}
  register={reviewRegister}
  {review}
  open={reviewOpen}
  busy={reviewBusy}
  notice={reviewNotice}
  onKeep={keepItem}
  onDrop={dropItem}
  onPromote={promoteItem}
  onEdit={editItem}
  onAcceptReadBack={acceptReadBack}
  onInstructAgain={() => {}}
  onDiscardReadBack={discardReadBack}
  onStagePlan={stageDraft}
  onAcceptRemaining={acceptRemaining}
  onCommit={commitReview}
  onChat={openReviewChat}
  onClose={() => (reviewOpen = false)}
/>

{#if reviewChatOpen}
  <ChatPanel
    open={true}
    role={reviewChat?.role ?? ''}
    subject={reviewChat?.subject_id ?? epicId}
    turns={reviewChat?.turns ?? []}
    proposal={reviewChat?.proposal ?? null}
    available={reviewChat !== null}
    busy={reviewChatBusy}
    onSend={sendReviewChat}
    onEnd={endReviewChat}
    onAccept={() => answerReviewProposal(true)}
    onReject={() => answerReviewProposal(false)}
    onToggle={openReviewChat}
  />
{/if}

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
    flex-wrap: wrap;
  }

  .epic-tokens {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    display: flex;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .tokens-label {
    color: var(--color-text-muted);
  }

  .tokens-value {
    font-weight: 500;
  }

  .tokens-unmeasured {
    color: var(--color-text-muted);
  }

  .epic-content {
    padding: var(--space-6);
    display: flex;
    flex-direction: column;
    gap: var(--space-6);
    overflow-y: auto;
  }

  .plans-section,
  .epic-facts,
  .epic-registers,
  .epic-tokens-detail,
  .epic-events {
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

  .workstream {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
  }

  .workstream__title {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .workstream__rows {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .workstream__row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-1) 0;
    border-top: 1px solid var(--color-border-subtle);
  }

  .workstream__plan {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    background: none;
    border: none;
    padding: 0;
    cursor: pointer;
    color: inherit;
    font: inherit;
    text-align: left;
    flex: 1 1 12rem;
  }

  .workstream__wait {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    flex: 1 1 12rem;
  }

  .pr-link {
    color: var(--color-accent);
    font-size: var(--text-xs);
  }

  .fact-list {
    margin: 0;
    padding-left: var(--space-4);
    font-size: var(--text-sm);
  }

  .epic-registers {
    flex-direction: row;
    flex-wrap: wrap;
    gap: var(--space-4);
  }

  .register {
    flex: 1 1 16rem;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
  }

  .register__counts {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .review-open {
    align-self: flex-start;
    min-height: 44px;
    padding: var(--space-2) var(--space-3);
    cursor: pointer;
  }

  .token-facts {
    display: grid;
    grid-template-columns: minmax(8rem, max-content) 1fr;
    gap: var(--space-1) var(--space-3);
    margin: 0;
    font-size: var(--text-sm);
  }

  .event-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .event-item {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    padding: var(--space-1) 0;
    border-bottom: 1px solid var(--color-border-subtle);
    font-size: var(--text-sm);
  }

  .event-time {
    color: var(--color-text-muted);
    font-size: var(--text-xs);
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

  @media (max-width: 768px) {
    .epic-content {
      padding: var(--space-3);
    }

    .plans-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
