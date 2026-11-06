<script lang="ts">
  import { onMount, onDestroy, tick } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import { formatDay, formatTime as formatInstant } from '../display/format';
  import Button from '../components/Button.svelte';
  import Icon from '../components/Icon.svelte';
  import IconButton from '../components/IconButton.svelte';
  import RowMenu from '../components/RowMenu.svelte';
  import FilterInput from '../components/FilterInput.svelte';
  import EmptyState from '../components/EmptyState.svelte';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import TriagePage from '../templates/TriagePage.svelte';
  import { apiClient, normaliseReportItem, type ReportItem, type ReportsPage } from '../api/client';
  import { runtimeStore, refreshReportsCount, syncUrl, navigateTo } from '../stores/runtime';
  import { buildHaystack, matchesHaystack, tokenize } from '../utils/filterText';
  import CompletionReportView from './CompletionReportView.svelte';

  /**
   * The group key of the reports the machine itself writes (`system.*`); it stands apart. The NUL
   * prefix is a character no project id may carry, so no project can share the machine's group.
   */
  const MACHINE_GROUP_KEY = '\u0000machine';

  let items = $state<ReportItem[]>([]);
  let unreadCount = $state(0);
  let hasOlder = $state(false);
  let olderCount = $state(0);
  let isLoading = $state(true);
  let isLoadingOlder = $state(false);
  /** A failed read of the list; the list area says so and offers the retry. */
  let loadError = $state('');
  /** A failed act on an already loaded list (mark read, mark all, older page). */
  let actionError = $state('');
  /** The act the error line's Retry repeats; `null` means a plain reload of the list. */
  let retryAction = $state<(() => Promise<void>) | null>(null);

  /** The coalesced quiet notice of the reports that arrived while this view was open. */
  let noticeCount = $state(0);
  let noticeFrom = $state('');
  let noticeSubject = $state('');
  let noticeTimer: ReturnType<typeof setTimeout> | null = null;
  /** The moment the ages are measured at; a quiet timer moves it so an idle list stays true. */
  let clock = $state(Date.now());
  let clockTimer: ReturnType<typeof setInterval> | null = null;

  let activeDetailPlanId = $state<string | null>(null);
  let syncedRouteReportPlan: string | null | undefined;

  /** The filter query; it runs over the loaded reports only. */
  let query = $state('');
  /** The reports whose body is open past its one line in the list. */
  let shownBodies = $state<Record<string, boolean>>({});
  /** The reports whose body is longer than the one line the list gives it. */
  let overflowingBodies = $state<Set<string>>(new Set());
  /**
   * The reports read by opening their detail in the list beside it: they stay in the list, marked
   * read, until the detail closes, so the selected row does not vanish under the pointer.
   */
  let retainedReadIds = $state<Record<string, boolean>>({});
  /**
   * The reports whose mark-read request is still in flight. Closing the detail must not drop such a
   * row yet: a failed request has to put it back as unread. A reload must also keep the read
   * accounted for until the response settles.
   */
  const pendingReadIds = new Set<string>();
  /**
   * The reads this view has already counted against the unread count.
   *
   * The runtime's `report.read` event and the mark-read response report the same read; whichever
   * arrives first counts it, and the other finds the marker and leaves the count alone. A repeated
   * event is a no-op for the same reason.
   */
  const countedReadIds = new Set<string>();

  let unsubscribeEvents: (() => void) | null = null;
  let isMounted = true;

  interface ProjectGroup {
    key: string;
    isMachine: boolean;
    epicGroups: Record<string, ReportItem[]>;
    planItems: ReportItem[];
    projectItems: ReportItem[];
  }

  /** The normalized query tokens, computed once per keystroke rather than once per item. */
  const queryTokens = $derived(tokenize(query).filter((t) => /\p{L}|\p{N}/u.test(t)));
  const isFiltered = $derived(queryTokens.length > 0);

  /** One normalized haystack per loaded report, rebuilt only when the list or locale changes. */
  const reportHaystacks = $derived.by(() => {
    const unreadLabel = $t('report.state.unread');
    const readLabel = $t('report.state.read');
    const haystacks = new Map<string, string>();
    for (const item of items) {
      haystacks.set(
        item.id,
        buildHaystack(
          reportVisibleFields(item, retainedReadIds[item.id] ? readLabel : unreadLabel),
        ),
      );
    }
    return haystacks;
  });

  /** The loaded reports that match the filter, in the served order. */
  const matchingItems = $derived.by(() => {
    if (!isFiltered) return items;
    return items.filter((item) => matchesHaystack(queryTokens, reportHaystacks.get(item.id) ?? ''));
  });

  let groupedReports = $derived.by<ProjectGroup[]>(() => {
    const projectMap: Record<string, ProjectGroup> = {};

    for (const item of matchingItems) {
      const key = groupKeyOf(item);
      if (!projectMap[key]) {
        projectMap[key] = {
          key,
          isMachine: key === MACHINE_GROUP_KEY,
          epicGroups: {},
          planItems: [],
          projectItems: [],
        };
      }
      const group = projectMap[key];
      if (item.from?.epic) {
        if (!group.epicGroups[item.from.epic]) {
          group.epicGroups[item.from.epic] = [];
        }
        group.epicGroups[item.from.epic].push(item);
      } else if (item.from?.plan) {
        group.planItems.push(item);
      } else {
        group.projectItems.push(item);
      }
    }

    return Object.values(projectMap);
  });

  /**
   * The group a report belongs to: its project, or the group of the machine when the runtime wrote
   * it about the machine itself (`system.*`). A machine report stays out of a project's group even
   * where the wire names the project that happened to be running.
   *
   * @param item a report
   * @returns its group key
   */
  function groupKeyOf(item: ReportItem): string {
    if (item.kind?.startsWith('system.')) return MACHINE_GROUP_KEY;
    return item.from?.project || MACHINE_GROUP_KEY;
  }

  function measureOverflow(): void {
    const overflow = new Set<string>();
    for (const node of document.querySelectorAll<HTMLElement>('.report-card__body')) {
      const id = node.closest<HTMLElement>('.report-card')?.dataset.reportId;
      if (!id) continue;
      const clamped = node.classList.contains('report-card__body--clamped');
      if (!clamped || node.scrollHeight === 0 || node.scrollHeight > node.clientHeight + 1) {
        overflow.add(id);
      }
    }
    overflowingBodies = overflow;
  }

  let isNarrow = $state(typeof window !== 'undefined' ? window.innerWidth < 1100 : false);

  function handleWindowResize(): void {
    if (typeof window !== 'undefined') {
      isNarrow = window.innerWidth < 1100;
    }
    measureOverflow();
  }

  // The body control is offered only where the clamped line actually hides text. The measurement
  // runs after the list or either pane changes; a layout-less environment (a unit test) reports no
  // height at all and keeps the control, so the branch stays reachable there.
  $effect(() => {
    void items;
    void query;
    void activeDetailPlanId;
    measureOverflow();
  });

  /**
   * @param item a report
   * @returns whether the list still shows it as unread
   */
  function isUnread(item: ReportItem): boolean {
    return !retainedReadIds[item.id];
  }

  /**
   * The fields the list can show for one report.
   *
   * A filter over the subject alone would miss the body, the origin, and the state; this is the
   * matrix the design note publishes for `/reports`.
   *
   * @param item the report
   * @param readState the localized unread or read state the list shows
   * @returns every visible value, for the shared haystack
   */
  function reportVisibleFields(
    item: ReportItem,
    readState: string,
  ): Array<string | null | undefined> {
    return [
      item.from?.project,
      item.from?.project_title,
      item.from?.epic,
      item.from?.epic_title,
      item.from?.plan,
      item.from?.plan_title,
      item.from?.phase,
      item.from?.role,
      item.from?.harness,
      item.from?.model,
      item.title,
      item.subject,
      item.kind,
      item.body,
      item.summary,
      item.status,
      readState,
      formatReportTime(item),
      item.created_at,
    ];
  }

  /**
   * Drops the rows that opening a detail read in place.
   *
   * A row whose read request is still in flight stays: its outcome decides whether it goes as
   * read or comes back as unread, and dropping it now would lose it if the request fails.
   */
  function flushRetainedReads(): void {
    const settled = Object.keys(retainedReadIds).filter((id) => !pendingReadIds.has(id));
    for (const id of settled) dropRetainedRow(id);
  }

  /**
   * Drops one retained row.
   *
   * The row's read stays counted: its event may still arrive after the detail closed, and a second
   * decrement for the same read would be wrong.
   *
   * @param id the report id to remove from the list
   */
  function dropRetainedRow(id: string): void {
    delete retainedReadIds[id];
    retainedReadIds = { ...retainedReadIds };
    items = items.filter((item) => item.id !== id);
  }

  /**
   * The plan whose completion report the route opened, from `/reports?plan={id}` or the store a
   * plan view set when it led here.
   *
   * @returns the plan id, or `null` for the plain list
   */
  function requestedReportPlan(): string | null {
    if (typeof window !== 'undefined') {
      const fromQuery = new URLSearchParams(window.location.search).get('plan');
      if (fromQuery) return fromQuery;
    }
    return $runtimeStore.selectedReportDetailPlanId ?? null;
  }

  /** Leaves the detail and returns to the list, clearing the plan from the address bar. */
  function closeDetail(): void {
    flushRetainedReads();
    activeDetailPlanId = null;
    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: null }));
    syncUrl('/reports', true);
  }

  // The reports route stays the same while its detail changes (a header visit to Reports, the
  // browser's back between two reports). Follow the store so the view does not keep a plan the
  // address no longer names.
  $effect(() => {
    if ($runtimeStore.currentRoute !== 'reports') return;
    const routePlan = $runtimeStore.selectedReportDetailPlanId ?? null;
    if (routePlan === syncedRouteReportPlan) return;
    syncedRouteReportPlan = routePlan;
    activeDetailPlanId = routePlan;
    if (routePlan === null) flushRetainedReads();
  });

  onMount(async () => {
    isMounted = true;
    const initialPlan = requestedReportPlan();
    activeDetailPlanId = initialPlan;
    syncedRouteReportPlan = initialPlan;
    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: initialPlan }));
    unsubscribeEvents = apiClient.subscribeEvents((event, data) => {
      handleEvent(event, data);
    });
    clockTimer = setInterval(() => {
      clock = Date.now();
    }, 30_000);
    await loadReports();
  });

  onDestroy(() => {
    isMounted = false;
    if (clockTimer !== null) {
      clearInterval(clockTimer);
      clockTimer = null;
    }
    if (noticeTimer) {
      clearTimeout(noticeTimer);
      noticeTimer = null;
    }
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  function handleEvent(event: string, data: unknown) {
    if (!isMounted) return;
    if (event === 'report.sent') {
      // The runtime announces a report wrapped as `{ report: … }`; a fixture may send it bare.
      const payload = (data ?? {}) as { report?: ReportItem };
      const received = payload.report ?? (data as ReportItem);
      const newReport = received ? normaliseReportItem(received) : received;
      if (newReport && newReport.id) {
        if (!items.some((i) => i.id === newReport.id)) {
          items = [newReport, ...items];
          unreadCount++;
          showArrivalNotice(newReport);
        }
      } else {
        void loadReports();
      }
    } else if (event === 'report.read') {
      const readData = data as { report_id?: string; report_ids?: string[] };
      const ids = readData?.report_ids || (readData?.report_id ? [readData.report_id] : []);
      if (ids.length > 0) {
        // One read counts once, and the ids are taken before the list drops them. A report outside
        // the loaded page still counts: the server's unread count includes it, and a read of an
        // older report on another client must move the unread count too.
        const newlyRead = [...new Set(ids)].filter((id) => !countedReadIds.has(id));
        for (const id of newlyRead) countedReadIds.add(id);
        items = items.filter((i) => !ids.includes(i.id) || retainedReadIds[i.id]);
        unreadCount = Math.max(0, unreadCount - newlyRead.length);
      } else {
        void loadReports();
      }
    }
  }

  /**
   * Shows the quiet notice of an arrival, coalescing the reports that arrive together.
   *
   * The notice is a polite live region: it never takes the focus, never ends a dialogue, and
   * leaves the tab title alone.
   *
   * @param report the report that arrived
   */
  function showArrivalNotice(report: ReportItem): void {
    noticeCount++;
    noticeFrom = fromLabel(report);
    noticeSubject = report.subject ?? '';
    if (noticeTimer) clearTimeout(noticeTimer);
    noticeTimer = setTimeout(() => {
      noticeCount = 0;
      noticeTimer = null;
    }, 6000);
  }

  /**
   * @param report a report
   * @returns the deepest part of its origin, as the notice names it
   */
  function fromLabel(report: ReportItem): string {
    return (
      report.from?.plan ?? report.from?.epic ?? report.from?.project ?? $t('report.list.machine')
    );
  }

  async function loadReports() {
    isLoading = true;
    loadError = '';
    actionError = '';
    try {
      const page: ReportsPage = await apiClient.getReports();
      const served = page.items || [];
      const servedIds = new Set(served.map((fresh) => fresh.id));
      // A row read beside its open detail stays in the list even when a reload no longer serves it:
      // the runtime has marked it read, and the detail still shows it. Only closing the detail (or
      // opening another one) drops it ({@link flushRetainedReads}). Each retained row keeps its
      // place among the rows the reload still serves, so the newest-first order stands.
      const merged = [...served];
      for (const row of items) {
        if (!retainedReadIds[row.id] || servedIds.has(row.id)) continue;
        const successor = items
          .slice(items.indexOf(row) + 1)
          .find((later) => servedIds.has(later.id));
        const at = successor ? merged.findIndex((later) => later.id === successor.id) : -1;
        merged.splice(at < 0 ? merged.length : at, 0, row);
      }
      items = merged;
      // The reloaded count already includes every read this view counted, so those markers go.
      // A read whose response is still in flight keeps its marker: that response must not subtract
      // a read the fresh count already reflects.
      for (const id of [...countedReadIds]) {
        if (!pendingReadIds.has(id)) countedReadIds.delete(id);
      }
      unreadCount = page.unread_count ?? items.length;
      hasOlder = page.has_older;
      olderCount = page.older_count;
      void refreshReportsCount();
    } catch (err: unknown) {
      loadError = (err as Error)?.message || $t('report.list.load_failed');
    } finally {
      isLoading = false;
    }
  }

  /**
   * Repeats the act the error line reports, or reloads the list where the failure was no act of its
   * own.
   */
  async function retryFailedAction(): Promise<void> {
    const action = retryAction;
    retryAction = null;
    actionError = '';
    await (action ?? loadReports)();
  }

  async function loadOlder() {
    isLoadingOlder = true;
    try {
      const page: ReportsPage = await apiClient.getReports(true);
      const existingIds = new Set(items.map((i) => i.id));
      const newItems = (page.items || []).filter((i) => !existingIds.has(i.id));
      items = [...items, ...newItems];
      hasOlder = page.has_older;
      olderCount = page.older_count;
    } catch (err: unknown) {
      retryAction = loadOlder;
      actionError = (err as Error)?.message || $t('report.list.older_failed');
    } finally {
      isLoadingOlder = false;
    }
  }

  /**
   * Marks a report read.
   *
   * In the list beside the detail the row stays, marked read, so the detail keeps its selected row
   * beside it; {@link flushRetainedReads} drops it once the detail closes.
   *
   * The retained marker is set before the request rather than after it: the runtime can emit
   * `report.read` while the mark-read request is still in flight, and a marker set afterwards
   * would let that event filter out the row the detail is showing. A failed request removes the
   * marker again, so the row returns to unread; closing the detail before the request settles
   * keeps the row until the outcome is known.
   *
   * Every read is in flight until its response settles, retained or not, so a reload that lands
   * before the response keeps the read accounted for instead of counting it twice.
   *
   * @param item the report to read
   * @param retain whether the row stays in the list until the detail closes
   */
  async function markRead(item: ReportItem, retain = false) {
    const alreadyRead = Boolean(retainedReadIds[item.id]);
    pendingReadIds.add(item.id);
    if (retain) retainedReadIds[item.id] = true;
    try {
      await apiClient.markReportRead(item.id);
      // The read event and this response report the same read. Whichever arrives first counts it,
      // and the other finds the marker and leaves the unread count alone. A row already read beside
      // its detail was counted when it was read; marking it read again moves no count.
      if (!alreadyRead && !countedReadIds.has(item.id)) {
        countedReadIds.add(item.id);
        unreadCount = Math.max(0, unreadCount - 1);
      }
      pendingReadIds.delete(item.id);
      if (retain) {
        // The detail may have closed while the request was in flight; a row no longer beside an
        // open detail goes as read now.
        const stillSelected = activeDetailPlanId !== null && planIdOf(item) === activeDetailPlanId;
        if (!stillSelected) dropRetainedRow(item.id);
      } else {
        items = items.filter((i) => i.id !== item.id);
      }
      void refreshReportsCount();
    } catch (err: unknown) {
      pendingReadIds.delete(item.id);
      if (retain) {
        delete retainedReadIds[item.id];
        retainedReadIds = { ...retainedReadIds };
      }
      retryAction = () => markRead(item, retain);
      actionError = (err as Error)?.message || $t('report.list.mark_read_failed');
    }
  }

  async function markAllShownRead() {
    if (matchingItems.length === 0) return;
    const isFiltered = queryTokens.length > 0;
    const shownIds = matchingItems.map((i) => i.id);
    const shownIdSet = new Set(shownIds);
    for (const id of shownIds) pendingReadIds.add(id);

    // Pre-mark retention for the open report among shown items before the network request
    // so an early report.read event does not discard it while in flight.
    if (activeDetailPlanId !== null) {
      for (const item of matchingItems) {
        if (planIdOf(item) === activeDetailPlanId) {
          retainedReadIds[item.id] = true;
        }
      }
      retainedReadIds = { ...retainedReadIds };
    }

    try {
      await apiClient.markAllReportsRead(isFiltered ? shownIds : undefined);
      if (isFiltered) {
        const newlyRead = shownIds.filter((id) => !countedReadIds.has(id));
        for (const id of newlyRead) countedReadIds.add(id);
        unreadCount = Math.max(0, unreadCount - newlyRead.length);

        for (const item of items) {
          if (!shownIdSet.has(item.id)) continue;
          const stillSelected = activeDetailPlanId !== null && planIdOf(item) === activeDetailPlanId;
          if (stillSelected) {
            retainedReadIds[item.id] = true;
          } else {
            delete retainedReadIds[item.id];
          }
        }
        retainedReadIds = { ...retainedReadIds };
        items = items.filter((i) => !shownIdSet.has(i.id) || Boolean(retainedReadIds[i.id]));
      } else {
        for (const item of items) {
          const stillSelected = activeDetailPlanId !== null && planIdOf(item) === activeDetailPlanId;
          if (stillSelected) {
            retainedReadIds[item.id] = true;
          } else {
            delete retainedReadIds[item.id];
          }
        }
        retainedReadIds = { ...retainedReadIds };
        items = items.filter((i) => Boolean(retainedReadIds[i.id]));
        countedReadIds.clear();
        unreadCount = 0;
      }

      void refreshReportsCount();
    } catch (err: unknown) {
      if (activeDetailPlanId !== null) {
        for (const id of shownIds) {
          delete retainedReadIds[id];
        }
        retainedReadIds = { ...retainedReadIds };
      }
      retryAction = markAllShownRead;
      actionError = (err as Error)?.message || $t('report.list.mark_all_failed');
    } finally {
      for (const id of shownIds) pendingReadIds.delete(id);
    }
  }

  /**
   * Opens a report's detail and records the plan on the route, so a plain visit to Reports
   * closes it the same way it closes a detail reached by its query.
   *
   * @param planId the plan whose completion report to show
   */
  function showDetail(planId: string): void {
    activeDetailPlanId = planId;
    // A row the previous detail read has no detail of its own any more; it goes unless its own read
    // request is still in flight, whose outcome decides whether it goes or comes back as unread.
    for (const id of Object.keys(retainedReadIds)) {
      if (pendingReadIds.has(id)) continue;
      const row = items.find((item) => item.id === id);
      if (!row || planIdOf(row) !== planId) dropRetainedRow(id);
    }
    syncedRouteReportPlan = planId;
    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: planId }));
  }

  /**
   * @param report a report
   * @returns the kind of the report's subject, as the detail chooses its view
   */
  function subjectKindOf(report: ReportItem): string {
    if (report.subject_kind) return report.subject_kind;
    if (report.kind?.startsWith('system.cleanup')) return 'cleanup';
    if (report.kind?.startsWith('source.')) return 'start';
    return (report.kind ?? '').split('.')[0] ?? 'plan';
  }

  /**
   * @param report a report
   * @returns whether its detail is offered at all
   */
  function hasDetail(report: ReportItem): boolean {
    if (report.subject_deleted) return true;
    const kind = subjectKindOf(report);
    if (
      ['plan', 'epic', 'project', 'start', 'cleanup', 'decision'].includes(kind) ||
      ['plan_completed', 'subject'].includes(kind)
    ) {
      return true;
    }
    return Boolean(report.detail_link || report.detail);
  }

  /**
   * Opens a part of the report's origin without reading the report.
   *
   * The path is the way to the subject's view; only the details action reads.
   *
   * @param part the part of the path the operator chose
   * @param report the report whose path it is
   */
  function openFromPart(part: 'project' | 'epic' | 'plan', report: ReportItem): void {
    if (part === 'project' && report.from?.project) {
      navigateTo('projects', { explorerProjectId: report.from.project });
    } else if (part === 'epic' && report.from?.epic) {
      navigateTo('epic', { epicId: report.from.epic });
    } else if (part === 'plan' && report.from?.plan) {
      navigateTo('plan', { planId: report.from.plan });
    }
  }

  /**
   * @param report a report
   * @returns the plan id its kind names, where it names one
   */
  function planIdOf(report: ReportItem): string | null {
    if (report.from?.plan) return report.from.plan;
    const params = report.subject_params ?? {};
    const fromParams = params['plan'];
    if (typeof fromParams === 'string' && fromParams.length > 0) return fromParams;
    const link = report.detail_link || report.detail || '';
    const completion = /plans\/([^/]+)\/completion/.exec(link);
    if (completion?.[1]) return completion[1];
    const plan = /plans\/([^/]+)$/.exec(link);
    return plan?.[1] ?? null;
  }

  /**
   * @param report a report
   * @returns the epic id its kind names, where it names one
   */
  function epicIdOf(report: ReportItem): string | null {
    if (report.from?.epic) return report.from.epic;
    const link = report.detail_link || report.detail || '';
    return /epics\/([^/]+)/.exec(link)?.[1] ?? null;
  }

  /**
   * @param report a report
   * @returns the cleanup item its subject names, where it names one
   */
  function cleanupItemOf(report: ReportItem): string | null {
    const params = report.subject_params ?? {};
    for (const key of ['item', 'candidate', 'plan', 'epic']) {
      const value = params[key];
      if (typeof value === 'string' && value.length > 0) return value;
    }
    return null;
  }

  /**
   * @param item a report
   * @returns whether opening it stays in this view, in the completion report's detail pane
   */
  function opensCompletionDetail(item: ReportItem): boolean {
    const kind = subjectKindOf(item);
    if (kind === 'plan' || kind === 'plan_completed') {
      const link = item.detail_link || item.detail || '';
      return item.kind === 'plan.ended' || link.includes('/completion');
    }
    if (['epic', 'project', 'start', 'cleanup', 'decision'].includes(kind)) return false;
    const detailLink = item.detail_link || item.detail;
    if (!detailLink) return Boolean(item.from?.plan);
    return /plans\/[^/]+\/completion/.test(detailLink);
  }

  /**
   * Opens a report's detail and reads the report in the same act.
   *
   * @param report the report to open
   */
  async function openDetail(item: ReportItem) {
    if (item.subject_deleted) return;

    // Opening the details reads the report, in the same act. The list beside the detail keeps the
    // row it read in place, so it does not lose its selection.
    void markRead(item, opensCompletionDetail(item));

    const kind = subjectKindOf(item);
    if (kind === 'plan' || kind === 'plan_completed') {
      const planId = planIdOf(item);
      if (!planId) return;
      const link = item.detail_link || item.detail || '';
      if (item.kind === 'plan.ended' || link.includes('/completion')) {
        showDetail(planId);
      } else {
        navigateTo('plan', { planId });
      }
      return;
    }
    if (kind === 'epic') {
      const epicId = epicIdOf(item);
      if (epicId) navigateTo('epic', { epicId });
      return;
    }
    if (kind === 'project') {
      if (item.from?.project) {
        navigateTo('projects', { explorerProjectId: item.from.project });
      }
      return;
    }
    if (kind === 'start') {
      navigateTo('start', { projectId: item.from?.project });
      return;
    }
    if (kind === 'cleanup') {
      runtimeStore.update((s) => ({
        ...s,
        cleanupHighlightItemId: cleanupItemOf(item),
        cleanupPreviewItemId: null,
      }));
      navigateTo('cleanup');
      return;
    }
    if (kind === 'decision') {
      navigateTo('decisions');
      return;
    }

    // A report whose kind names no view of its own follows its detail link.
    const detailLink = item.detail_link || item.detail;
    if (!detailLink) return;
    const match = /plans\/([^/]+)\/completion/.exec(detailLink);
    if (match?.[1]) {
      showDetail(match[1]);
      return;
    }
    if (item.from?.plan) {
      showDetail(item.from.plan);
    }
  }

  /**
   * @param report a report
   * @returns the accessible name of its actions menu, carrying the subject so two menus differ
   */
  function rowMenuLabel(report: ReportItem): string {
    const subject = (report.subject ?? '').trim();
    return subject
      ? `${$t('report.list.row_menu')}: ${subject}`
      : $t('report.list.row_menu');
  }

  /**
   * Chosen from the row menu: mark the report read.
   *
   * The row leaves the list when the read settles; the focus then moves to the next row's menu, the
   * previous one's, or the heading, so a keyboard operator keeps their place.
   *
   * @param item the report the act names
   */
  async function chooseMarkRead(item: ReportItem): Promise<void> {
    const cards = Array.from(document.querySelectorAll<HTMLElement>('.report-card'));
    const index = cards.findIndex((card) => card.dataset.reportId === item.id);
    const successor =
      cards[index + 1]?.querySelector<HTMLElement>('[data-testid="report-row-menu"]') ??
      cards[index - 1]?.querySelector<HTMLElement>('[data-testid="report-row-menu"]');

    // A row already read beside its detail keeps its place and its count when its menu repeats the
    // read.
    await markRead(item, Boolean(retainedReadIds[item.id]));

    const remaining = Array.from(document.querySelectorAll<HTMLElement>('.report-card'));
    const row = remaining.find((card) => card.dataset.reportId === item.id);
    if (row) {
      // The read failed: the row is still there, and its own control takes the focus back.
      row.querySelector<HTMLElement>('[data-testid="report-row-menu"]')?.focus();
      return;
    }
    // A read event can remove the successor row while the request settles; a detached control
    // cannot take the focus, so the heading is the fallback.
    const target = successor?.isConnected
      ? successor
      : document.querySelector<HTMLElement>('.reports-heading');
    target?.focus();
  }

  /**
   * @param report a report
   * @returns the name the deleted-subject explanation uses
   */
  function subjectLabelOf(report: ReportItem): string {
    return report.from?.plan ?? report.from?.epic ?? report.subject;
  }

  function formatReportTime(item: ReportItem): string {
    const now = new Date(clock);
    const created = formatInstant(item.created_at, 'relative', $currentLocale, now);
    const isSentLater = Boolean(item.sent_at && item.created_at && item.sent_at !== item.created_at);

    if (isSentLater) {
      const sent =
        formatInstant(item.sent_at, 'short', $currentLocale, now) || item.sent || '';
      return `${created} · ${$t('report.list.sent_at', { time: sent })}`;
    }
    return created;
  }

  /**
   * @param item a report
   * @returns the moment itself, for the row's hover advisory
   */
  function reportMoment(item: ReportItem): string {
    return formatInstant(item.created_at, 'short', $currentLocale, new Date(clock));
  }

  /**
   * @param item a report
   * @returns the day the report was made, in the interface language
   */
  function reportDay(item: ReportItem): string {
    return formatDay(item.created_at, $currentLocale);
  }
  function bodyToggleLabel(report: ReportItem, open: boolean): string {
    const action = $t(open ? 'report.body.less' : 'report.body.more');
    const subject = (report.subject ?? '').trim();
    return subject ? `${action}: ${subject}` : action;
  }
</script>

<svelte:window onresize={handleWindowResize} />

<div class="reports-view">
  <TriagePage selected={activeDetailPlanId !== null} hasFilters={items.length > 0}>
    <div slot="filters">
      {#if items.length > 0}
        <FilterInput
          value={query}
          label={$t('report.filter.label')}
          placeholder={$t('report.filter.placeholder')}
          resultCount={isFiltered ? matchingItems.length : undefined}
          onchange={(value) => (query = value)}
        />
      {/if}
    </div>
    <div slot="list">
      {@render listPane()}
    </div>
    <div slot="detail">
      {@render detailPane()}
    </div>
  </TriagePage>
</div>

{#snippet listPane()}
  <div class="reports-container">
    <!-- Header -->
    <header class="reports-header">
      <div class="reports-header__title-row">
        {#if !isNarrow || activeDetailPlanId === null}
          <h1 class="reports-heading" tabindex="-1">{$t('report.list.title')}</h1>
        {:else}
          <h2 class="reports-heading" tabindex="-1">{$t('report.list.title')}</h2>
        {/if}
        {#if unreadCount > 0}
          <span class="unread-count" data-testid="unread-count">
            {$t('report.list.unread', { count: unreadCount })}
          </span>
        {/if}
        <div class="reports-header__actions">
          {#if items.length > 0}
            <Button
              variant="secondary"
              size="small"
              onclick={markAllShownRead}
              title={$t('report.action.mark_all_read.title')}
            >
              {$t('report.action.mark_all_read')}
            </Button>
          {/if}
        </div>
      </div>
    </header>

    {#if actionError}
      <div class="error-line" role="alert">
        <p>{actionError}</p>
        <Button variant="ghost" size="small" onclick={retryFailedAction}>
          {$t('web.common.retry')}
        </Button>
      </div>
    {/if}

    {#if isLoading}
      <div class="loading-state">
        <Skeleton shape="card" rows={3} label={$t('report.list.loading')} />
      </div>
    {:else if loadError}
      <ErrorState
        title={$t('report.list.load_failed')}
        message={loadError}
        actionLabel={$t('web.common.retry')}
        onAct={loadReports}
        titleLevel={2}
      />
    {:else if items.length === 0}
      <EmptyState
        title={$t('report.list.empty.title')}
        message={$t('report.list.empty.message')}
        titleLevel={2}
      />
    {:else if matchingItems.length === 0}
      <div class="empty-state" data-testid="report-filter-empty">
        <p class="empty-title">{$t('report.filter.no_matches')}</p>
      </div>
    {:else}
      <!-- Grouped List -->
      <div class="reports-tree">
        {#each groupedReports as pGroup (pGroup.key)}
          <section class="project-section">
            <h2 class="project-header">
              <span class="project-glyph" aria-hidden="true"><Icon name="state.dot" size={16} /></span>
              {#if pGroup.isMachine}
                <span class="project-title">{$t('report.list.machine')}</span>
              {:else}
                <Button
                  variant="ghost"
                  size="small"
                  aria-label={$t('report.from.open_project', { project: pGroup.key })}
                  onclick={() =>
                    navigateTo('projects', { explorerProjectId: pGroup.key })}
                >
                  {pGroup.key}
                </Button>
              {/if}
            </h2>

            <div class="project-content">
              <!-- Epic groups -->
              {#each Object.entries(pGroup.epicGroups) as [epicName, epicItems] (epicName)}
                <div class="epic-section">
                  <h3 class="epic-header">
                    <span class="epic-glyph" aria-hidden="true">◇</span>
                    <Button
                      variant="ghost"
                      size="small"
                      aria-label={$t('report.from.open_epic', { epic: epicName })}
                      onclick={() => openFromPart('epic', epicItems[0])}
                    >
                      {epicName}
                    </Button>
                  </h3>
                  <div class="items-list">
                    {#each epicItems as item (item.id)}
                      {@render reportCard(item)}
                    {/each}
                  </div>
                </div>
              {/each}

              <!-- Plan items under project without epic -->
              {#if pGroup.planItems.length > 0}
                <div class="items-list">
                  {#each pGroup.planItems as item (item.id)}
                    {@render reportCard(item)}
                  {/each}
                </div>
              {/if}

              <!-- Project-level items -->
              {#if pGroup.projectItems.length > 0}
                <div class="items-list">
                  {#each pGroup.projectItems as item (item.id)}
                    {@render reportCard(item)}
                  {/each}
                </div>
              {/if}
            </div>
          </section>
        {/each}
      </div>
    {/if}

    <!-- Pagination -->
    {#if hasOlder && !isLoading && !loadError}
      {#if isFiltered}
        <p class="loaded-only-hint" data-testid="loaded-only-hint">
          {$t('report.filter.loaded_only')}
        </p>
      {/if}
      <div class="pagination-bar">
        <Button
          variant="secondary"
          disabled={isLoadingOlder}
          onclick={loadOlder}
          title={$t('report.list.show_older.title')}
        >
          {#if isLoadingOlder}
            {$t('report.list.loading_older')}
          {:else if olderCount > 0}
            {$t('report.list.show_older', { count: olderCount })}
          {:else}
            {$t('report.list.show_older_plain')}
          {/if}
        </Button>
      </div>
    {/if}
  </div>
{/snippet}

{#snippet detailPane()}
  {#if activeDetailPlanId}
    {#key activeDetailPlanId}
      <CompletionReportView
        planId={activeDetailPlanId}
        titleLevel={isNarrow ? 1 : 2}
        onClose={closeDetail}
      />
    {/key}
  {:else}
    <p class="split-placeholder">{$t('report.split.select')}</p>
  {/if}
{/snippet}

{#snippet reportCard(item: ReportItem)}
  {@const isActive = activeDetailPlanId !== null && activeDetailPlanId === planIdOf(item)}
  {@const unread = isUnread(item)}
  <article
    class="report-card"
    class:report-card--deleted={item.subject_deleted}
    class:report-card--active={isActive}
    class:report-card--unread={unread}
    aria-current={isActive ? 'true' : undefined}
    data-report-id={item.id}
    data-unread={unread ? 'true' : 'false'}
  >
    <div class="report-card__header">
      {#if unread}
        <span class="report-card__unread-dot" aria-hidden="true"></span>
        <span class="sr-only">{$t('report.state.unread')}</span>
      {:else}
        <span class="item-state text-muted" data-testid="report-read-state">
          {$t('report.state.read')}
        </span>
      {/if}
      <span class="report-from mono">
        {#if item.from?.project}
          <Button
            variant="ghost"
            size="small"
            aria-label={$t('report.from.open_project', { project: item.from.project })}
            onclick={() => openFromPart('project', item)}
          >
            {item.from.project}
          </Button>
        {:else}
          <span class="text-muted">{$t('report.list.machine')}</span>
        {/if}
        {#if item.from?.epic}
          <span class="path-sep" aria-hidden="true">›</span>
          <Button
            variant="ghost"
            size="small"
            aria-label={$t('report.from.open_epic', { epic: item.from.epic })}
            onclick={() => openFromPart('epic', item)}
          >
            {item.from.epic}
          </Button>
        {/if}
        {#if item.from?.plan}
          <span class="path-sep" aria-hidden="true">›</span>
          <Button
            variant="ghost"
            size="small"
            aria-label={$t('report.from.open_plan', { plan: item.from.plan })}
            onclick={() => openFromPart('plan', item)}
          >
            {item.from.plan}
          </Button>
        {/if}
      </span>
      <span class="report-card__subject" class:report-card__subject--unread={unread}>
        {item.subject}
      </span>
      <span class="item-time text-muted" title={reportMoment(item)}
        >{formatReportTime(item)}</span
      >
    </div>

    {#if item.body || item.summary}
      {@const bodyOpen = Boolean(shownBodies[item.id])}
      <div
        class="report-card__body text-muted"
        class:report-card__body--clamped={!bodyOpen}
        data-model-text
      >
        {item.body || item.summary}
      </div>
      {#if overflowingBodies.has(item.id)}
        <Button
          variant="ghost"
          size="small"
          aria-expanded={bodyOpen}
          aria-label={bodyToggleLabel(item, bodyOpen)}
          onclick={() => (shownBodies[item.id] = !bodyOpen)}
        >
          {bodyOpen ? $t('report.body.less') : $t('report.body.more')}
        </Button>
      {/if}
    {/if}

    <div class="report-card__actions">
      {#if hasDetail(item)}
        <Button
          variant="secondary"
          size="small"
          data-testid="report-details"
          disabled={item.subject_deleted}
          disabledReason={item.subject_deleted
            ? $t('report.action.details.deleted.title', {
                item: subjectLabelOf(item),
                date: reportDay(item),
              })
            : undefined}
          title={$t('report.action.details.title')}
          onclick={() => openDetail(item)}
        >
          {$t('report.action.details')}
        </Button>
        {#if item.subject_deleted}
          <span class="deleted-note text-muted">
            {$t('report.detail.deleted_by_cleanup', {
              item: subjectLabelOf(item),
              date: reportDay(item),
            })}
          </span>
        {/if}
      {/if}
      <div class="report-card__menu">
        <RowMenu
          icon="act.more"
          size="sm"
          data-testid="report-row-menu"
          label={rowMenuLabel(item)}
          visibleLabel={$t('report.list.row_menu')}
          advisory={$t('report.list.row_menu.title')}
          items={[
            {
              id: 'mark-read',
              label: $t('report.action.mark_read'),
              title: $t('report.action.mark_read.title'),
              onclick: () => chooseMarkRead(item),
            },
          ]}
        />
      </div>
    </div>
  </article>
{/snippet}

<div class="arrival-notice" role="status" aria-live="polite" data-testid="report-arrival-notice">
  {#if noticeCount === 1}
    {$t('report.notice.one', { from: noticeFrom, subject: noticeSubject })}
  {:else if noticeCount > 1}
    {$t('report.notice.many', { count: noticeCount, from: noticeFrom })}
  {/if}
</div>

<style>
  /* The triage template owns the page's width and its split; the view owns its items. */
  .reports-view {
    inline-size: 100%;
    min-inline-size: 0;
  }

  .split-placeholder {
    padding: var(--space-10) var(--space-4);
    border: 1px dashed var(--color-border);
    border-radius: var(--radius-lg);
    color: var(--color-text-muted);
    text-align: center;
  }

  .reports-header__actions {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    flex-wrap: wrap;
  }

  .loaded-only-hint {
    margin: var(--space-4) 0 0 0;
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .reports-header {
    margin-bottom: var(--space-4);
    border-bottom: 1px solid var(--color-border);
    padding-bottom: var(--space-4);
  }

  .arrival-notice {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }

  .reports-header__title-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-3);
  }

  .reports-heading {
    font-size: var(--text-2xl);
    font-weight: var(--weight-semibold);
    color: var(--color-text-primary);
    margin: 0;
  }

  /* The unread count is text, not a pill: the design system names a state in words. */
  .unread-count {
    font-size: var(--text-sm);
    font-weight: var(--weight-semibold);
    color: var(--color-primary);
  }

  .error-line {
    background: var(--color-danger-subtle);
    border: 1px solid var(--color-danger-border);
    color: var(--color-danger);
    padding: var(--space-3) var(--space-4);
    border-radius: var(--radius-md);
    margin-bottom: var(--space-4);
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: var(--space-3);
  }

  .error-line p {
    margin: 0;
  }

  .loading-state {
    padding: var(--space-4) 0;
  }

  .empty-state {
    text-align: center;
    padding: var(--space-10) var(--space-4);
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
  }

  .empty-title {
    color: var(--color-text-muted);
    font-size: var(--text-base);
    margin: 0;
  }

  .reports-tree {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .project-section {
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-4);
  }

  .project-header {
    font-size: var(--text-lg);
    font-weight: var(--weight-semibold);
    color: var(--color-text-primary);
    margin: 0 0 var(--space-3) 0;
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .project-glyph {
    color: var(--color-primary);
  }

  .project-content {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    padding-left: var(--space-3);
  }

  .epic-section {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .epic-header {
    font-size: var(--text-base);
    font-weight: var(--weight-semibold);
    color: var(--color-text-secondary);
    margin: 0;
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .epic-glyph {
    color: var(--color-text-muted);
  }

  .items-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding-left: var(--space-3);
  }

  .report-card {
    background: var(--color-bg-subtle);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    padding: var(--space-3) var(--space-4);
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .report-card__header {
    display: flex;
    align-items: baseline;
    gap: var(--space-2);
    flex-wrap: wrap;
    font-size: var(--text-sm);
  }

  /* The dot is the design system's mark of what is new; the bold subject carries the same word. */
  .report-card__unread-dot {
    inline-size: var(--space-2);
    block-size: var(--space-2);
    border-radius: var(--radius-full);
    background-color: var(--color-primary);
    flex: 0 0 auto;
    align-self: center;
  }

  .report-from {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
    flex-wrap: wrap;
  }

  .path-sep {
    color: var(--color-text-muted);
  }

  .deleted-note {
    font-size: var(--text-xs);
    font-style: italic;
  }

  .report-card--deleted {
    opacity: 0.85;
  }

  .report-card--active {
    border-color: var(--color-primary);
    box-shadow: inset 3px 0 0 var(--color-primary);
  }

  .item-state {
    font-size: var(--text-xs);
    font-style: italic;
  }

  .report-card__subject {
    color: var(--color-text-primary);
    flex: 1 1 auto;
    min-inline-size: 12rem;
  }

  .report-card__subject--unread {
    font-weight: var(--weight-semibold);
  }

  .item-time {
    font-size: var(--text-xs);
    margin-left: auto;
  }

  .report-card__body {
    font-size: var(--text-xs);
    line-height: 1.4;
    padding-left: var(--space-4);
  }

  /* The list shows the body on one line; the control below opens the rest in place. */
  .report-card__body--clamped {
    display: -webkit-box;
    -webkit-line-clamp: 1;
    line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .report-card__actions {
    display: flex;
    gap: var(--space-2);
    align-items: center;
    margin-top: var(--space-1);
    padding-left: var(--space-4);
  }

  .report-card__menu {
    position: relative;
    display: inline-flex;
  }


  .pagination-bar {
    display: flex;
    justify-content: center;
    margin-top: var(--space-6);
  }

  @media (max-width: 640px) {
    .project-content {
      padding-left: 0;
    }

    .items-list {
      padding-left: 0;
    }

    .item-time {
      margin-left: 0;
      width: 100%;
    }
  }
</style>
