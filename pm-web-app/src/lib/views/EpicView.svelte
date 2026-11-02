<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import FilterInput from '../components/FilterInput.svelte';
  import WorkControlPanel from '../components/WorkControlPanel.svelte';
  import FindingsReviewModal from '../components/FindingsReviewModal.svelte';
  import ChatPanel from '../components/ChatPanel.svelte';
  import WorkflowIcon from '../components/WorkflowIcon.svelte';
  import Icon from '../components/Icon.svelte';
  import { phaseIconId } from '../icons/workflowIcons';
  import type { UiIconId } from '../icons/uiIcons';
  import { displayState } from '../display';
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
  import { formatSplit, formatTokens, formatTime } from '../display/format';
  import { buildHaystack, matchesHaystack, tokenize } from '../utils/filterText';
  import { refreshesEpicView } from './epicEvents';

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

  /** The workstreams the operator has folded or unfolded, overriding the resource's own default. */
  let workstreamToggles = $state<Record<string, boolean>>({});

  /** The filter over the workstream plans. */
  let query = $state('');

  /** The normalized query tokens, computed once per keystroke. */
  const queryTokens = $derived(tokenize(query));

  /**
   * The workstreams whose rows match the filter; an empty workstream disappears with the rows
   * that gave it its heading.
   */
  const filteredWorkstreams = $derived.by(() => {
    if (!resource) return [];
    if (queryTokens.length === 0) return resource.workstreams;
    return resource.workstreams
      .map((workstream) => ({
        ...workstream,
        rows: workstream.rows.filter((row) =>
          matchesHaystack(queryTokens, workstreamHaystack(workstream, row)),
        ),
      }))
      .filter((workstream) => workstream.rows.length > 0);
  });

  /** How many workstream plans the filter leaves visible. */
  const matchingPlanCount = $derived(
    filteredWorkstreams.reduce((count, workstream) => count + workstream.rows.length, 0),
  );

  /**
   * The fields a workstream plan shows, plus the name of its workstream.
   *
   * @param workstream the workstream the row belongs to
   * @param row the plan row
   * @returns every visible value, for the shared filter
   */
  function workstreamHaystack(
    workstream: { id: string; name: string },
    row: EpicWorkRow,
  ): string {
    return buildHaystack([
      workstream.id,
      workstream.name,
      row.plan_id,
      row.title,
      row.status,
      row.wait_reason,
      row.wait_plan_id,
      row.change,
      row.objective,
      ...(row.deliverables ?? []),
      ...(row.surface ?? []),
      ...(row.dependencies ?? []),
      ...(row.overlap_files ?? []),
    ]);
  }

  /** A landing row: a row the runtime served a pull request number for. */
  type LandingRow = EpicWorkRow & { pull_request_number: number };

  /** The epic's landings: the shipped rows that carry a pull request. */
  let landings = $derived(
    resource
      ? resource.workstreams.flatMap((workstream) =>
          workstream.rows.filter(
            (row): row is LandingRow => typeof row.pull_request_number === 'number',
          ),
        )
      : [],
  );

  onMount(async () => {
    unsubscribeEvents = apiClient.subscribeEvents((event) => {
      // The closed catalogue's events that move anything the epic's view shows
      // (specification, Event Catalogue); the set is checked against EventName by a unit test.
      if (refreshesEpicView(event)) {
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
    }
    if (epicResource.status === 'fulfilled') {
      resource = epicResource.value;
    } else if (initial) {
      resource = null;
    }
    if (!epic) {
      errorMessage = $t('epic.view.load_failed');
    }
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
    const key = rowStatusKey(status);
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
    return known.includes(key) ? $t(`epic.views.row.${key}`) : status;
  }

  /** The marker of a row's status, as the design writes the workstream rows. */
  function statusIcon(status: string): UiIconId {
    switch (status) {
      case 'shipped':
        return 'state.check';
      case 'running':
      case 'pausing':
        return 'act.start';
      case 'paused':
        return 'act.pause';
      case 'waiting':
      case 'waiting for you':
      case 'held':
        return 'state.alert';
      case 'cancelled':
      case 'abandoned':
        return 'state.error';
      default:
        return 'state.dot';
    }
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

  /** Whether a workstream shows its rows: the operator's fold wins over the resource's default. */
  function isWorkstreamOpen(workstream: { id: string; open: boolean }): boolean {
    return workstreamToggles[workstream.id] ?? workstream.open;
  }

  function toggleWorkstream(workstream: { id: string; open: boolean }) {
    workstreamToggles = {
      ...workstreamToggles,
      [workstream.id]: !isWorkstreamOpen(workstream),
    };
  }

  /** The statuses a workstream counts, in the order the design writes them. */
  const COUNTED_STATUSES = [
    'shipped',
    'running',
    'pausing',
    'paused',
    'waiting',
    'held',
    'staged',
    'cancelled',
    'abandoned',
  ] as const;

  /** The workstream's plan counts per status, e.g. `1 shipped · 1 running · 1 staged`. */
  function countsText(rows: { status: string }[]): string {
    const parts: string[] = [];
    for (const status of COUNTED_STATUSES) {
      const count = rows.filter((row) => rowStatusKey(row.status) === status).length;
      if (count > 0) {
        parts.push($t(`overview.epic.counts.${status}`, { count }));
      }
    }
    return parts.join(' · ');
  }

  /** The closed status vocabulary's key for a row's wire status. */
  function rowStatusKey(status: string): string {
    // The queue writes the two two-word statuses with a space; the vocabulary keys them with `_`.
    if (status === 'waiting for you') return 'waiting';
    if (status === 'handed back') return 'handed_back';
    return status;
  }

  /**
   * The wall time of an event to the second, in the interface's own notation.
   *
   * The wire sends an instant; a value that is already a clock is shown as it arrives.
   */
  function formatEventTime(at: string): string {
    return formatTime(at, 'clock', $currentLocale) || at;
  }

  /**
   * The pull request's address, only where the runtime served an http(s) one: a value another
   * scheme could carry never becomes a link the operator is invited to follow.
   */
  function pullRequestUrl(url: string | null | undefined): string {
    return url && /^https?:\/\//i.test(url) ? url : '';
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
      await loadEpic(false).catch(() => {});
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
          <Button
            variant="ghost"
            onclick={goBackToCaller}
            data-testid="back-btn"
            data-advisory={$t('common.actions.back.title')}
          >
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
          <span
            class="badge badge--state badge--state-{resource?.state ?? epic.state}"
            data-testid="epic-state"
          >
            {displayState('epic', resource?.state ?? epic.state, $t)}
          </span>
          {#if resource}
            <span class="badge badge--mode" data-testid="epic-mode">
              {$t('epic.views.mode', {
                mode: resource.auto_start ? $t('epic.views.mode_on') : $t('epic.views.mode_off'),
              })}
            </span>
            <span class="badge badge--counts" data-testid="epic-scope">
              {$t('overview.epic.at_once', {
                running: resource.running,
                scope: resource.scope,
              })}
            </span>
            {#if resource.state === 'ended' && resource.terminal_reason}
              <span class="badge badge--ended" data-testid="epic-end">
                {$t('overview.ended.epic_end', { end: resource.terminal_reason })}
              </span>
            {/if}
            {#if nextPlanId()}
              <span class="badge badge--next" data-testid="epic-next-plan">
                {$t('epic.views.next_plan', { plan: nextPlanId() })}
              </span>
            {/if}
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
          {#if resource.workstreams.length > 0}
            <FilterInput
              value={query}
              label={$t('common.filter.label')}
              placeholder={$t('common.filter.placeholder')}
              resultCount={matchingPlanCount}
              onchange={(value) => (query = value)}
            />
          {/if}
          {#if filteredWorkstreams.length === 0}
            <p class="text-muted" data-testid="epic-filter-empty">{$t('common.filter.empty')}</p>
          {/if}
          {#each filteredWorkstreams as workstream (workstream.id)}
            <div class="workstream" data-testid={`workstream-${workstream.id}`}>
              <h3 class="workstream__title">
                <button
                  type="button"
                  class="workstream__toggle"
                  aria-expanded={isWorkstreamOpen(workstream)}
                  data-testid={`workstream-toggle-${workstream.id}`}
                  data-advisory={$t('epic.views.toggle_workstream.title')}
                  onclick={() => toggleWorkstream(workstream)}
                >
                  <span class="workstream__marker" aria-hidden="true">
                    {isWorkstreamOpen(workstream) ? '▾' : '▸'}
                  </span>
                  <span class="workstream__id mono">{workstream.id}</span>
                  <span class="workstream__name">{workstream.name}</span>
                </button>
                <span class="workstream__counts" data-testid={`workstream-counts-${workstream.id}`}>
                  {countsText(workstream.rows)}
                </span>
              </h3>
              {#if query.trim().length > 0 || isWorkstreamOpen(workstream)}
                <div class="workstream__rows">
                  {#each workstream.rows as row (row.plan_id)}
                    <div class="workstream__row" data-testid={`epic-row-${row.plan_id}`}>
                      <button
                        type="button"
                        class="workstream__plan"
                        data-advisory={$t('epic.views.open_plan.title')}
                        onclick={() => openPlan(row.plan_id)}
                      >
                        <span class="plan-glyph"><Icon name={statusIcon(row.status)} size={16} /></span>
                        <span class="plan-title font-semibold">{row.title}</span>
                      </button>
                      <span class="badge badge--state badge--state-{rowStatusKey(row.status)}">
                        {rowStatus(row.status)}
                      </span>
                      {#if waitLabel(row)}
                        <span class="workstream__wait" data-testid={`wait-${row.plan_id}`}>
                          {waitLabel(row)}
                        </span>
                      {/if}
                      {#if row.pull_request_number}
                        {#if pullRequestUrl(row.pull_request_url)}
                          <a
                            class="pr-link mono"
                            href={pullRequestUrl(row.pull_request_url)}
                            target="_blank"
                            rel="noopener noreferrer"
                            data-testid={`epic-pr-${row.plan_id}`}
                          >
                            {$t('epic.views.pr', { number: row.pull_request_number })}
                          </a>
                        {:else}
                          <span class="pr-link mono" data-testid={`epic-pr-${row.plan_id}`}>
                            {$t('epic.views.pr', { number: row.pull_request_number })}
                          </span>
                        {/if}
                      {/if}
                    </div>
                  {/each}
                </div>
              {/if}
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
            <p class="text-muted" data-testid="epic-sources-empty">{$t('epic.views.no_sources')}</p>
          {/if}
        </section>

        <section class="epic-facts" data-testid="epic-landings">
          <h2 class="section-title">{$t('overview.epic.landings')}</h2>
          <p data-testid="epic-landing-count">
            {$t('epic.views.landing_count', { count: resource.landings })}
          </p>
          {#if landings.length > 0}
            <ul class="landing-list">
              {#each landings as landing (landing.plan_id)}
                <li class="landing-item" data-testid={`epic-landing-${landing.plan_id}`}>
                  <span class="landing-item__plan mono">{landing.plan_id}</span>
                  {#if pullRequestUrl(landing.pull_request_url)}
                    <a
                      class="landing-item__pr"
                      href={pullRequestUrl(landing.pull_request_url)}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      {$t('epic.views.pr', { number: landing.pull_request_number })}
                    </a>
                  {:else}
                    <span class="landing-item__pr mono">
                      {$t('epic.views.pr', { number: landing.pull_request_number })}
                    </span>
                  {/if}
                </li>
              {/each}
            </ul>
          {/if}
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
              data-advisory={$t('epic.views.review_open.title')}
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
                  <span class="event-time mono">{formatEventTime(event.at)}</span>
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
                    <span class="plan-glyph"><Icon name="act.start" size={16} /></span>
                    <span class="plan-title font-semibold">{plan.title}</span>
                    <span class="badge badge--phase">
                      <WorkflowIcon id={phaseIconId(plan.current_phase)} size={16} />
                      {plan.current_phase}
                    </span>
                    <span class="badge badge--state badge--state-{plan.phase_state}"
                      >{displayState('plan', plan.phase_state, $t)}</span
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
                    <span class="plan-glyph"><Icon name="state.dot" size={16} /></span>
                    <span class="plan-title font-semibold">{plan.title}</span>
                    <span class="badge badge--state badge--state-staged"
                      >{displayState('plan', plan.phase_state || 'staged', $t)}</span
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
    onClose={() => (reviewChatOpen = false)}
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
    color: var(--color-accent-hover);
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
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .workstream__toggle {
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
  }

  .workstream__marker {
    width: 1ch;
    color: var(--color-text-muted);
  }

  .workstream__id {
    color: var(--color-text-muted);
  }

  .workstream__name {
    font-weight: 600;
  }

  .workstream__counts {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
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

  .landing-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    font-size: var(--text-sm);
  }

  .landing-item {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .landing-item__pr {
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
    color: var(--color-primary);
  }

  .review-open:hover {
    color: var(--color-primary-hover);
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
    grid-template-columns: repeat(auto-fill, minmax(min(100%, 320px), 1fr));
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
    box-shadow: var(--shadow-sm);
    border-color: var(--color-accent);
  }

  .plan-card__head {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: wrap;
  }

  .badge--phase {
    display: inline-flex;
    align-items: center;
    gap: 0.35em;
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

  @media (max-width: 480px) {
    .epic-header {
      padding: var(--space-3);
    }

    .epic-content {
      padding: var(--space-2);
      gap: var(--space-4);
    }

    .epic-headline {
      flex-direction: column;
      align-items: flex-start;
    }

    .workstream {
      padding: var(--space-2);
    }

    .workstream__row {
      flex-direction: column;
      align-items: flex-start;
    }

    .workstream__plan,
    .workstream__wait {
      flex: 1 1 100%;
      max-width: 100%;
      overflow-wrap: anywhere;
    }

    .epic-registers {
      flex-direction: column;
    }

    .register {
      flex: 1 1 auto;
    }

    .token-facts {
      grid-template-columns: 1fr;
    }

    .plans-grid {
      grid-template-columns: 1fr;
    }

    .event-item {
      flex-direction: column;
      align-items: flex-start;
    }
  }
</style>
