<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import { errorCodeOf, actFailureText } from '../api/errors';
  import FilterInput from '../components/FilterInput.svelte';
  import WorkControlPanel from '../components/WorkControlPanel.svelte';
  import FindingsReviewModal from '../components/FindingsReviewModal.svelte';
  import ChatPanel from '../components/ChatPanel.svelte';
  import Chip from '../components/Chip.svelte';
  import KindMark from '../components/KindMark.svelte';
  import StatusLabel from '../components/StatusLabel.svelte';
  import EpicIntentPanel from '../components/EpicIntentPanel.svelte';
  import Tabs, { type TabItem } from '../components/Tabs.svelte';
  import ObjectPage from '../templates/ObjectPage.svelte';
  import { displayState } from '../display';
  import { EPIC_TABS, type EpicTab } from '../routes';
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
    openEpicDecomposition,
    selectObjectTab,
  } from '../stores/runtime';
  import { formatSplit, formatTime } from '../display/format';
  import { buildHaystack, matchesHaystack, tokenize } from '../utils/filterText';
  import { refreshesEpicView } from './epicEvents';

  let epicId = $derived($runtimeStore.selectedEpicId || '');
  let epic = $state<EpicProcess | null>(null);
  let resource = $state<EpicResource | null>(null);
  let isLoading = $state(true);
  let errorCode = $state<string | null>(null);
  /** The code of a failed epic-resource read: the page keeps the epic's head and says so. */
  let resourceErrorCode = $state<string | null>(null);

  let unsubscribeEvents: (() => void) | null = null;

  let reviewRegister = $state<'findings' | 'watches'>('findings');
  let review = $state<ReviewResource | null>(null);
  let reviewOpen = $state(false);
  let reviewBusy = $state(false);
  let reviewNotice = $state('');

  let reviewChat = $state<ChatReadBack | null>(null);
  let reviewChatOpen = $state(false);
  let reviewChatBusy = $state(false);

  /** Whether the tokens line shows its plan-by-plan disclosure. */
  let tokensOpen = $state(false);

  /** The workstreams the operator has folded or unfolded, overriding the resource's own default. */
  let workstreamToggles = $state<Record<string, boolean>>({});

  /** The filter over the workstream plans. */
  let query = $state('');

  /** The normalized query tokens, computed once per keystroke. */
  const queryTokens = $derived(tokenize(query));

  /**
   * Whether the epic stands as a list column: one of its plans is open to its right, which the
   * address carries as an object opened in place (`navigation.adoc#url`).
   */
  const listColumn = $derived($runtimeStore.openObject?.kind === 'plan');

  /**
   * The tab the address names, or the default one the address omits.
   *
   * @param value the store's tab, if it carries one
   * @returns the tab this page draws
   */
  function epicTab(value: string | null | undefined): EpicTab {
    return (EPIC_TABS as ReadonlyArray<string>).includes(value ?? '')
      ? (value as EpicTab)
      : 'plans';
  }

  /** The tab this page draws. */
  const activeTab = $derived(epicTab($runtimeStore.objectTab));

  /**
   * @param tab a tab of the epic
   * @returns the id of its panel while that tab is the shown one, and none for the others
   */
  function panelIdOf(tab: EpicTab): string | undefined {
    // Only the shown tab has a panel in the document, so only it may name one: a tab whose
    // `aria-controls` pointed at an absent panel would tell an assistive reader it controls nothing.
    return tab === activeTab ? `epic-panel-${tab}` : undefined;
  }

  /** The number of findings that are new since the last review, or `0` where none is. */
  const newFindings = $derived(resource?.findings.new ?? 0);

  /** Whether the epic has ended: its tabs stand, and none of them opens an act. */
  const ended = $derived((resource?.state ?? epic?.state ?? '') === 'ended');

  // A review or its chat opened while the epic ran closes when the epic ends on an event (or a
  // read reports it): their acts belong to a running epic (`epic-view.adoc#tabs`).
  $effect(() => {
    if (!ended) {
      return;
    }
    reviewOpen = false;
    reviewChatOpen = false;
    reviewChat = null;
  });

  /** The five tabs of the epic's object column, the `! n` badge while findings are new. */
  const tabItems = $derived<TabItem[]>(
    // The strip is derived from the route model's own tab set, so a tab the address accepts can
    // never be missing here and one here can never be unknown to the address.
    EPIC_TABS.map((tab) => ({
      id: tab,
      label: tabLabel(tab),
      panelId: panelIdOf(tab),
      ...(tab === 'reviews' && newFindings > 0 && !ended
        ? {
            count: newFindings,
            attention: true,
            countLabel: $t('overview.epic.new_findings', { count: newFindings }),
          }
        : {}),
    })),
  );

  /**
   * @param tab a tab of the epic's column
   * @returns its visible label, from the catalogue
   */
  function tabLabel(tab: EpicTab): string {
    switch (tab) {
      case 'plans':
        return $t('overview.epic.tab.plans');
      case 'intent':
        return $t('intent.tab.title');
      case 'landings':
        return $t('overview.epic.landings');
      case 'reviews':
        return $t('overview.epic.tab.reviews');
      case 'events':
        return $t('overview.epic.events');
    }
  }

  /**
   * Writes the chosen tab into the address, by a replace.
   *
   * The tab is not a level: switching it replaces the current entry, so Back leaves the epic
   * rather than walking through its tabs (`navigation.adoc#url`).
   *
   * @param id the tab a person chose
   */
  function selectTab(id: string): void {
    selectObjectTab(epicTab(id));
  }

  /** The tokens line's figure: the sums the resource carries, never a figure of this view's own. */
  function totalLine(figure: TokenFigure | null | undefined): string {
    if (!figure || !figure.measured) {
      return $t('common.tokens.not_measured');
    }
    return formatSplit(figure.total, figure.input, figure.output, figure.floor, $currentLocale);
  }

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
  function workstreamHaystack(workstream: { id: string; name: string }, row: EpicWorkRow): string {
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

  /** A landing row: a shipped row the runtime served a pull request number for. */
  type LandingRow = EpicWorkRow & { pull_request_number: number };

  /** The epic's landings: the shipped rows that carry a pull request. */
  let landings = $derived(
    resource
      ? resource.workstreams.flatMap((workstream) =>
          workstream.rows.filter(
            (row): row is LandingRow =>
              rowStatusKey(row.status) === 'shipped' && typeof row.pull_request_number === 'number',
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
    errorCode = null;
    resourceErrorCode = null;
    const [process, epicResource] = await Promise.allSettled([
      apiClient.getEpicProcess(epicId),
      apiClient.getEpicResource(epicId),
    ]);
    if (process.status === 'fulfilled') {
      epic = process.value;
    }
    if (epicResource.status === 'fulfilled') {
      resource = epicResource.value;
      // A read that answers clears the failure of one that was still in flight: the retry the
      // panel names would otherwise stay on offer beside content that is current.
      resourceErrorCode = null;
    } else {
      // A read that failed says so and can be retried: a page that showed the epic's head while
      // its resource stayed null would claim the epic "is still being read" for ever.
      resourceErrorCode = errorCodeOf(epicResource.reason);
      if (initial) {
        resource = null;
      }
    }
    if (!epic) {
      errorCode = errorCodeOf(process.status === 'rejected' ? process.reason : '');
    }
    if (initial) isLoading = false;
  }

  function openPlan(pId: string) {
    openPlanView(pId);
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

  /** The dot colour a row's status takes, as the design system's state palette names it. */
  function rowTone(status: string): 'neutral' | 'running' | 'attention' | 'danger' | 'success' {
    switch (rowStatusKey(status)) {
      case 'shipped':
        return 'success';
      case 'running':
      case 'pausing':
        return 'running';
      case 'waiting':
      case 'held':
        return 'attention';
      case 'cancelled':
      case 'abandoned':
        return 'danger';
      default:
        return 'neutral';
    }
  }

  /** The dot colour the epic's own state takes. */
  function stateTone(state: string): 'neutral' | 'running' | 'attention' | 'danger' | 'success' {
    switch (state) {
      case 'running':
      case 'landing':
        return 'running';
      case 'waiting':
      case 'held':
        return 'attention';
      case 'ended':
        return 'neutral';
      default:
        return 'neutral';
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

  /**
   * Since when a row waits, in the interface's relative notation.
   *
   * @param at the instant the row began to wait
   * @returns `9 d ago`, `since 11:25`, or nothing where the moment has not begun
   */
  function waitSince(at: string | null | undefined): string {
    if (!at) return '';
    return formatTime(at, 'relative', $currentLocale);
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
    // A row the queue reports as `merged` or `completed` has shipped (`epic-view.adoc#layout`).
    if (status === 'merged' || status === 'completed') return 'shipped';
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
    return actFailureText(err, $t('error.unknown.fix'));
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
    const turnId = reviewChat?.proposal?.turn_id;
    if (!turnId) {
      return;
    }
    reviewChatBusy = true;
    try {
      reviewChat = await apiClient.answerSubjectChatProposal(`epic:${epicId}`, turnId, accept);
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
      <Skeleton shape="card" rows={3} label={$t('epic.view.loading')} />
    </div>
  {:else if errorCode !== null}
    <div class="epic-view__error" data-testid="epic-error">
      <ErrorState
        code={errorCode}
        actionLabel={$t('common.action.retry')}
        onAct={() => loadEpic(true)}
        titleLevel={2}
        announce={epic !== null}
      />
    </div>
  {:else if epic}
    <ObjectPage compact={listColumn}>
      <span slot="kind"><KindMark kind="epic" /></span>
      <span slot="title" data-testid="epic-title">{epic.title}</span>
      <span slot="id">{epic.epic_id}</span>

      <div slot="state" class="epic-state" data-testid="epic-state-line">
        <span data-testid="epic-state">
          <StatusLabel
            tone={stateTone(resource?.state ?? epic.state)}
            label={displayState('epic', resource?.state ?? epic.state, $t)}
          />
        </span>
        {#if epic.review_ready}
          <span class="epic-state__word" data-testid="review-ready-badge">
            {$t('epic.review.ready')}
          </span>
        {/if}
        {#if resource}
          <span class="epic-state__word" data-testid="epic-mode">
            {$t('epic.views.mode', {
              mode: resource.auto_start ? $t('epic.views.mode_on') : $t('epic.views.mode_off'),
            })}
          </span>
          <span class="epic-state__word" data-testid="epic-scope">
            {$t('overview.epic.at_once', {
              running: resource.running ?? '?',
              scope: resource.scope,
            })}
          </span>
          {#if resource.state === 'ended' && resource.terminal_reason}
            <span class="epic-state__word" data-testid="epic-end">
              {$t('overview.ended.epic_end', { end: resource.terminal_reason })}
            </span>
          {/if}
          {#if nextPlanId()}
            <span class="epic-state__word" data-testid="epic-next-plan">
              {$t('epic.views.next_plan', { plan: nextPlanId() })}
            </span>
          {/if}
        {/if}
      </div>

      <div slot="primary" class="epic-acts">
        <WorkControlPanel
          subjectId={epic.epic_id}
          subjectKind="epic"
          projectId={resource?.project_id ?? epic.project}
          ended={resource?.state === 'ended'}
          scope={resource?.scope ?? 0}
          scopeKnown={resource !== null}
          resourceFailed={resourceErrorCode !== null}
          running={resource?.running ?? null}
          nextPlan={nextPlanId()}
          autoStart={resource?.auto_start ?? null}
          onReview={(register) => void openReview(register)}
          onDecompose={decompose}
          onChanged={() => void loadEpic(false)}
        />
      </div>

      <div slot="tabs" class="epic-tabstrip">
        {#if resource}
          <div class="epic-tokens mono" data-testid="epic-tokens">
            <span class="tokens-label">{$t('overview.epic.tokens')}:</span>
            <span class="tokens-value">{totalLine(resource.tokens.total)}</span>
            {#if resource.tokens.total.not_measured_tasks > 0}
              <span class="tokens-unmeasured" data-testid="epic-tokens-unmeasured">
                {unmeasuredLine(resource.tokens.total.not_measured_tasks)}
              </span>
            {/if}
            {#if resource.tokens.plans.length > 0 || resource.tokens.own.measured}
              <span class="tokens-sep" aria-hidden="true">·</span>
              <button
                type="button"
                class="tokens-more"
                aria-expanded={tokensOpen}
                data-testid="epic-tokens-disclosure"
                data-advisory={$t('epic.views.tokens_detail.title')}
                onclick={() => (tokensOpen = !tokensOpen)}
              >
                {$t('overview.epic.tokens_detail')}
              </button>
            {/if}
          </div>
          {#if tokensOpen}
            <dl class="token-facts" data-testid="epic-token-breakdown">
              <dt>{$t('epic.views.tokens_own')}</dt>
              <dd data-testid="epic-own-tokens">{figureLine(resource.tokens.own)}</dd>
              {#each resource.tokens.plans as plan (plan.plan_id)}
                <dt class="mono">{plan.plan_id}</dt>
                <dd data-testid={`epic-plan-tokens-${plan.plan_id}`}>{figureLine(plan.figure)}</dd>
              {/each}
            </dl>
          {/if}
          <Tabs tabs={tabItems} active={activeTab} label={$t('epic.views.tabs')} onSelect={selectTab} />
        {/if}
      </div>

      {#if resource}
        {#if resourceErrorCode !== null}
          <p class="epic-view__stale" data-testid="epic-resource-stale" role="status">
            <span>{$t('epic.views.read_failed')}</span>
            <button
              type="button"
              class="review-open"
              data-testid="epic-resource-retry"
              onclick={() => void loadEpic(false)}
            >
              {$t('common.action.retry')}
            </button>
          </p>
        {/if}
        {#if activeTab === 'plans'}
          <div id="epic-panel-plans" role="tabpanel" data-testid="epic-panel-plans">
            {@render plans(true)}
          </div>
        {:else if activeTab === 'intent'}
          <div id="epic-panel-intent" role="tabpanel" data-testid="epic-panel-intent">
            <section class="epic-facts" data-testid="epic-intent-sources">
              <h2 class="section-title">{$t('overview.epic.sources')}</h2>
              {#if resource.sources.length > 0}
                <ul class="fact-list">
                  {#each resource.sources as source}
                    <li>{source}</li>
                  {/each}
                </ul>
              {:else}
                <p class="text-muted" data-testid="epic-sources-empty">
                  {$t('epic.views.no_sources')}
                </p>
              {/if}
            </section>
            <EpicIntentPanel {epicId} />
          </div>
        {:else if activeTab === 'landings'}
          <div id="epic-panel-landings" role="tabpanel" data-testid="epic-panel-landings">
            <p data-testid="epic-landing-count">
              {$t('epic.views.landing_count', { count: landings.length })}
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
            {:else}
              <p class="text-muted" data-testid="epic-landings-empty">
                {$t('epic.views.no_landings')}
              </p>
            {/if}
            {#if epic.alongside_landings && epic.alongside_landings.length > 0}
              <section class="epic-facts" data-testid="epic-alongside">
                <h2 class="section-title">{$t('epic.views.alongside')}</h2>
                <div class="alongside-list">
                  {#each epic.alongside_landings as landing}
                    <div class="alongside-item mono" data-testid="alongside-item">{landing}</div>
                  {/each}
                </div>
              </section>
            {/if}
            <p class="text-muted" data-testid="epic-analysis-missing">
              {$t('epic.views.analysis_missing')}
            </p>
          </div>
        {:else if activeTab === 'reviews'}
          <div id="epic-panel-reviews" role="tabpanel" data-testid="epic-panel-reviews">
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
                {#if resource.state !== 'ended'}
                  <button
                    type="button"
                    class="review-open"
                    data-testid="open-findings-review"
                    data-advisory={$t('epic.views.review_open.title')}
                    onclick={() => void openReview('findings')}
                  >
                    {$t('epic.views.review_open')}
                  </button>
                {/if}
              </div>
              <div class="register" data-testid="epic-watches">
                <h2 class="section-title">{$t('overview.epic.watches')}</h2>
                <p class="register__counts">
                  {$t('overview.epic.watches_counts', {
                    open: resource.watches.open,
                    kept: resource.watches.kept,
                  })}
                </p>
                {#if resource.state !== 'ended'}
                  <button
                    type="button"
                    class="review-open"
                    data-testid="open-watches-review"
                    data-advisory={$t('epic.views.review_watches.title')}
                    onclick={() => void openReview('watches')}
                  >
                    {$t('epic.views.review_open')}
                  </button>
                {/if}
              </div>
            </section>
          </div>
        {:else}
          <div id="epic-panel-events" role="tabpanel" data-testid="epic-panel-events">
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
                <p class="text-muted" data-testid="epic-events-empty">{$t('epic.views.no_events')}</p>
              {/if}
            </section>
          </div>
        {/if}
      {:else if resourceErrorCode !== null}
        <div class="epic-view__resource-error" data-testid="epic-resource-error">
          <ErrorState
            code={resourceErrorCode}
            actionLabel={$t('common.action.retry')}
            onAct={() => loadEpic(true)}
            titleLevel={2}
            announce={true}
          />
        </div>
      {/if}
    </ObjectPage>

    {#if listColumn && resource}
      <div class="epic-list-column" data-testid="epic-list-column">
        {@render plans(false)}
      </div>
    {/if}
  {/if}
</div>

{#snippet plans(withTitle: boolean)}
  <section class="plans-section" data-testid="epic-workstreams">
    {#if withTitle}
      <h2 class="section-title">{$t('epic.views.workstreams')}</h2>
    {/if}
    {#if resource && resource.workstreams.length > 0}
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
        {#if queryTokens.length > 0 || isWorkstreamOpen(workstream)}
          <div class="workstream__rows">
            {#each workstream.rows as row (row.plan_id)}
              <div class="workstream__row" data-testid={`epic-row-${row.plan_id}`}>
                <button
                  type="button"
                  class="workstream__plan"
                  data-testid={`epic-open-${row.plan_id}`}
                  data-advisory={$t('epic.views.open_plan.title')}
                  onclick={() => openPlan(row.plan_id)}
                >
                  <span class="plan-title font-semibold">{row.title}</span>
                </button>
                <span data-testid={`epic-plan-chip-${row.plan_id}`}><Chip value={row.plan_id} /></span>
                <span data-testid={`epic-plan-status-${row.plan_id}`}>
                  <StatusLabel tone={rowTone(row.status)} label={rowStatus(row.status)} fixed />
                </span>
                {#if waitLabel(row)}
                  <span class="workstream__wait" data-testid={`wait-${row.plan_id}`}>
                    <span>{waitLabel(row)}</span>
                    {#if row.wait_since && waitSince(row.wait_since)}
                      <span class="workstream__sep" aria-hidden="true">·</span>
                      <time
                        class="workstream__since"
                        datetime={row.wait_since}
                        data-testid={`wait-since-${row.plan_id}`}
                        >{waitSince(row.wait_since)}</time
                      >
                    {/if}
                  </span>
                {/if}
                {#if row.pull_request_number}
                  <!-- A row shows its pull request whatever its state; only a shipped row is a landing. -->
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
{/snippet}

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
  .epic-view__error,
  .epic-view__resource-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: var(--space-8);
    gap: var(--space-4);
  }

  .epic-view__stale {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .epic-state {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .epic-state__word {
    color: var(--color-text-secondary);
  }

  .epic-acts {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  /* The list column's own body, under the compact head (`epic-view.adoc#tabs`). Its inset comes
     from the engine's column once the column chain renders it; until then it keeps the page's. */
  .epic-list-column {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    padding: var(--space-4) var(--space-6);
    min-inline-size: 0;
  }

  /* The tokens line and the tabs share the region between the head and the body (D6-1). */
  .epic-tabstrip {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    min-inline-size: 0;
  }

  .epic-tokens {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    display: flex;
    gap: var(--space-2);
    flex-wrap: wrap;
    align-items: baseline;
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

  .tokens-more {
    background: none;
    border: none;
    padding: 0;
    color: var(--color-accent);
    font: inherit;
    cursor: pointer;
  }

  .tokens-more:hover {
    color: var(--color-accent-hover);
  }

  .token-facts {
    display: grid;
    grid-template-columns: minmax(8rem, max-content) 1fr;
    gap: var(--space-1) var(--space-3);
    margin: 0;
    font-size: var(--text-sm);
  }

  .plans-section,
  .epic-facts,
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
    flex: 0 1 auto;
  }

  .workstream__wait {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    flex: 1 1 12rem;
    display: inline-flex;
    flex-wrap: wrap;
    gap: var(--space-1);
  }

  .workstream__since {
    color: var(--color-text-secondary);
  }

  .workstream__sep {
    color: var(--color-text-muted);
  }

  .tokens-sep {
    color: var(--color-text-muted);
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
    display: flex;
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

  .plan-title {
    font-size: var(--text-sm);
  }

  @media (max-width: 480px) {
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

    .event-item {
      flex-direction: column;
      align-items: flex-start;
    }
  }
</style>
