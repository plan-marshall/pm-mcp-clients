import { writable, get } from 'svelte/store';
import {
  apiClient,
  type StatusResponse,
  type StateRepresentation,
  type HypermediaLink,
  type OnboardingState,
  type MachineSettingsRecord,
  type MachineSettingsUpdateResult,
  type ProjectOverviewResponse,
} from '../api/client';
import {
  matchPath,
  pathForRoute,
  type AppRoute,
  type PathMatch,
  type RouteParams,
  type StartPath,
} from '../routes';
import { currentLocale, translate } from '../i18n';

export type { AppRoute, RouteParams, StartPath };

export interface RevokedInfo {
  reason?: string;
  revoked_by?: string;
  revoked_at?: string;
  details?: string;
}

export interface RuntimeState {
  isInitialized: boolean;
  isPaired: boolean;
  isOnboarded: boolean;
  deviceLabel: string;
  machineName: string;
  currentRoute: AppRoute;
  currentState: StateRepresentation | null;
  onboarding?: OnboardingState | null;
  machineSettings?: MachineSettingsRecord | null;
  isRevoked: boolean;
  revokedReason: string;
  revokedInfo?: RevokedInfo | null;
  /** A code in the address bar was refused; the pairing page shows why. */
  pairingFailed?: boolean;
  isOffline: boolean;
  /** The network listener this browser reached the machine over was turned off. */
  isListenerClosed: boolean;
  /** When that listener was turned off, for the page that waits for it. */
  listenerClosedAt?: string | null;
  isDraining: boolean;
  drainInfo?: { count?: number; what?: string; who?: string; since?: string } | null;
  isStopped: boolean;
  stoppedInfo?: { command?: string; who?: string; time?: string } | null;
  activeStep: number;
  /** The steps the runtime reports as done, 1-based. */
  completedSteps: number;
  unreadReportsCount: number;
  unreadReportsArrivalNotice?: string | null;
  quietReportNotice?: string | null;
  selectedReportDetailPlanId?: string | null;
  openDecisionsCount: number;
  newDecisionArrivalNotice?: string | null;
  quietDecisionNotice?: string | null;
  projectEventNotice?: string | null;
  selectedPlanId?: string | null;
  selectedEpicId?: string | null;
  selectedProjectId?: string | null;
  selectedTaskId?: string | null;
  /** The project of the open start, when it has not opened an entry yet. */
  startProjectId?: string | null;
  /** The entry of the open start, once the runtime has opened it. */
  startEntryId?: string | null;
  /** Whether the open start is a plan's or an epic's. */
  startKind?: 'plan' | 'epic';
  /** The project onboarding the project route opens, when it names one. */
  selectedOnboardingId?: string | null;
  /** The onboarding step another screen opened the onboarding at, 1-based. */
  onboardingStep?: number | null;
  /** How an ended plan ended, when a view opened it from Ended Work. */
  selectedPlanEnded?: EndedPlanInfo | null;
}

/** An ended plan as Ended Work names it, for the plan view it opens. */
export interface EndedPlanInfo {
  plan_id: string;
  outcome?: string | null;
  reason?: string | null;
  ended_at?: string | null;
}

const initialState: RuntimeState = {
  isInitialized: false,
  isPaired: false,
  isOnboarded: false,
  deviceLabel: '',
  machineName: 'local-machine',
  currentRoute: 'pair',
  currentState: null,
  isRevoked: false,
  revokedReason: '',
  revokedInfo: null,
  pairingFailed: false,
  isOffline: false,
  isListenerClosed: false,
  listenerClosedAt: null,
  isDraining: false,
  drainInfo: null,
  isStopped: false,
  stoppedInfo: null,
  activeStep: 1,
  completedSteps: 0,
  unreadReportsCount: 0,
  unreadReportsArrivalNotice: null,
  quietReportNotice: null,
  selectedReportDetailPlanId: null,
  openDecisionsCount: 0,
  newDecisionArrivalNotice: null,
  quietDecisionNotice: null,
  projectEventNotice: null,
  selectedPlanId: null,
  selectedEpicId: null,
  selectedProjectId: null,
  selectedTaskId: null,
  startProjectId: null,
  startEntryId: null,
  startKind: 'plan',
  selectedOnboardingId: null,
  onboardingStep: null,
  selectedPlanEnded: null,
};

export const runtimeStore = writable<RuntimeState>(initialState);

export function deriveStepFromState(stateId?: string): number {
  if (!stateId) return 1;
  if (stateId.startsWith('onboard.mode') || stateId.startsWith('onboard.entry')) return 1;
  if (stateId.startsWith('language.')) return 1;
  if (stateId.startsWith('harness.')) return 2;
  if (stateId.startsWith('models.') || stateId.startsWith('catalogue.')) return 3;
  if (stateId.startsWith('roles.')) return 4;
  if (stateId.startsWith('store.')) return 5;
  if (stateId.startsWith('providers.')) return 6;
  if (stateId.startsWith('defaults.') || stateId.startsWith('plans.')) return 7;
  if (stateId.startsWith('onboard.completed')) return 8;
  return 1;
}

let hashListenerRegistered = false;
let eventsUnsubscribe: (() => void) | null = null;

function subscribeToEvents(): void {
  if (eventsUnsubscribe) {
    eventsUnsubscribe();
    eventsUnsubscribe = null;
  }
  eventsUnsubscribe = apiClient.subscribeEvents((event, data) => {
    handleRuntimeEvent(event, data);
  });
}

export async function checkUrlHashForCode(): Promise<boolean> {
  if (typeof window !== 'undefined' && window.location.hash) {
    const match = /code=([^&]+)/.exec(window.location.hash);
    if (match?.[1]) {
      const extracted = decodeURIComponent(match[1]);
      try {
        await pairBrowser(extracted);
        // Redeemed: drop the fragment so a reload does not replay a single-use code.
        history.replaceState(null, '', window.location.pathname);
        runtimeStore.update((s) => ({ ...s, pairingFailed: false }));
        return true;
      } catch {
        // Keep the fragment: a premature rewrite loses the code and makes the refusal look like
        // a plain redirect to /pair. The pairing page shows that the code was refused.
        runtimeStore.update((s) => ({ ...s, pairingFailed: true }));
      }
    }
  }
  return false;
}

/** Reads whether the machine is onboarded, and its name, from a status response. */
function onboardedOf(status: StatusResponse): boolean {
  if (typeof status.onboarded === 'boolean') {
    return status.onboarded;
  }
  if (typeof status.machine === 'object' && status.machine) {
    return Boolean(status.machine.onboarded);
  }
  return false;
}

function machineNameOf(status: StatusResponse): string {
  if (typeof status.machine === 'string') {
    return status.machine;
  }
  return status.machine?.name || 'local-machine';
}

/**
 * The route the app opens on for a paired browser.
 *
 * A paired browser never lands on the pairing page or the entry: it opens where the operator
 * left it (`pm_start_route`), or the overview when work is running or waiting and the explorer
 * otherwise. The onboarding comes first while the machine is not onboarded.
 *
 * @param requested the route the address bar named, if it named one
 * @param onboarded whether the machine is onboarded
 * @param workRunning whether the machine reports running work or work that waits for the operator
 * @returns the route to open
 */
function landingRoute(
  requested: PathMatch | null,
  onboarded: boolean,
  workRunning: boolean,
): AppRoute {
  if (!onboarded) {
    return 'onboarding';
  }
  if (requested?.route === 'onboarding') {
    return 'onboarding';
  }
  if (requested && requested.route !== 'pair' && requested.route !== 'entry') {
    return requested.route;
  }
  const remembered = rememberedStartRoute();
  if (remembered) {
    return remembered;
  }
  return workRunning ? 'overview' : 'projects';
}

/**
 * The start point the operator last chose, if it is one of the landing routes.
 *
 * Only an explicit choice is remembered; a browser that never chose one lands by the rule of
 * `landingRoute`, on the overview while work runs or waits and on the projects otherwise.
 *
 * @returns `overview` or `projects`, or `null` when nothing was remembered
 */
export function rememberedStartRoute(): 'overview' | 'projects' | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const saved = localStorage.getItem('pm_start_route');
  return saved === 'overview' || saved === 'projects' ? saved : null;
}

/**
 * Remembers the operator's chosen start point, or clears it to land by the rule again.
 *
 * @param route `overview` or `projects`, or `null` to forget the choice
 */
export function setRememberedStartRoute(route: 'overview' | 'projects' | null): void {
  if (typeof window === 'undefined') {
    return;
  }
  if (route) {
    localStorage.setItem('pm_start_route', route);
  } else {
    localStorage.removeItem('pm_start_route');
  }
}

/**
 * Whether the machine reports work that runs or waits for the operator.
 *
 * @param summary the projects summary, or `null` when it could not be read
 * @returns `true` when at least one project carries work
 */
function hasWork(summary: ProjectOverviewResponse | null): boolean {
  if (!summary?.projects?.length) {
    return false;
  }
  return summary.projects.some((project) => {
    const plans = Number(project.active_plans_count ?? 0);
    const epics = Number(project.active_epics_count ?? 0);
    const needs = Number(project.needs_operator_count ?? 0);
    return plans > 0 || epics > 0 || needs > 0 || project.setup_needs_operator === true;
  });
}

export async function initRuntime(): Promise<void> {
  if (typeof window !== 'undefined' && !hashListenerRegistered) {
    window.addEventListener('hashchange', () => {
      void checkUrlHashForCode();
    });
    hashListenerRegistered = true;
  }

  apiClient.setOnRevoked((info) => {
    const reasonStr = typeof info === 'string' ? info : info?.reason || 'device_revoked';
    const revokedObj: RevokedInfo =
      typeof info === 'object' && info !== null ? info : { reason: reasonStr };
    runtimeStore.update((s) => ({
      ...s,
      isPaired: false,
      isRevoked: true,
      revokedReason: reasonStr,
      revokedInfo: revokedObj,
      currentRoute: 'revoked',
    }));
  });

  apiClient.setOnOffline(() => {
    runtimeStore.update((s) => ({
      ...s,
      isOffline: true,
    }));
  });

  const hasCodeInHash = await checkUrlHashForCode();
  if (hasCodeInHash) {
    return;
  }

  const isPaired = apiClient.isPaired();
  const label = apiClient.getLabel() || '';
  const requested = matchPath(typeof window !== 'undefined' ? window.location.pathname : '/');

  if (!isPaired) {
    runtimeStore.update((s) => ({
      ...s,
      isInitialized: true,
      isPaired: false,
      currentRoute: 'pair',
    }));
    return;
  }

  try {
    const status: StatusResponse = await apiClient.getStatus();
    const onboarded = onboardedOf(status);
    const machineName = machineNameOf(status);

    // The landing point depends on whether work runs or waits; the explorer summary answers it.
    // A paired browser that the runtime reports as not onboarded needs no summary.
    const summary = onboarded ? await apiClient.getProjects().catch(() => null) : null;

    // If there is a current state reported in status, store it
    const currentState: StateRepresentation = (status as unknown as StateRepresentation) || null;

    runtimeStore.update((s) => ({
      ...s,
      isInitialized: true,
      isPaired: true,
      isOnboarded: onboarded,
      deviceLabel: label,
      machineName,
      currentRoute: landingRoute(requested, onboarded, hasWork(summary)),
      selectedPlanId: requested?.planId ?? null,
      selectedEpicId: requested?.epicId ?? null,
      startProjectId: requested?.projectId ?? null,
      startEntryId: requested?.entryId ?? null,
      currentState,
      activeStep: deriveStepFromState(currentState?.state),
      isOffline: false,
      isRevoked: false,
    }));

    subscribeToEvents();
    void refreshReportsCount();
    void refreshDecisionsCount();
  } catch (err: unknown) {
    if ((err as Error)?.message === 'unauthorized') {
      return;
    }
    runtimeStore.update((s) => ({
      ...s,
      isInitialized: true,
      isOffline: true,
    }));
  }
}

export async function refreshReportsCount(): Promise<void> {
  if (!apiClient.isPaired()) return;
  try {
    const page = await apiClient.getReports();
    runtimeStore.update((s) => {
      let arrivalNotice = s.unreadReportsArrivalNotice;
      if (page.unread_count > 0 && !arrivalNotice) {
        arrivalNotice = page.oldest_unread_time
          ? translate(get(currentLocale), 'report.arrival.line', {
              count: page.unread_count,
              time: page.oldest_unread_time,
            })
          : translate(get(currentLocale), 'report.arrival.line_now', {
              count: page.unread_count,
            });
      }
      return {
        ...s,
        unreadReportsCount: page.unread_count,
        unreadReportsArrivalNotice: arrivalNotice,
      };
    });
  } catch {
    // Ignore fetch failures
  }
}

export async function refreshDecisionsCount(): Promise<void> {
  if (!apiClient.isPaired()) return;
  try {
    const decisions = await apiClient.getDecisions();
    runtimeStore.update((s) => {
      let arrivalNotice = s.newDecisionArrivalNotice;
      if (decisions.length > 0 && !arrivalNotice && s.openDecisionsCount === 0) {
        arrivalNotice = translate(get(currentLocale), 'decisions.list.open', {
          count: decisions.length,
        });
      }
      return {
        ...s,
        openDecisionsCount: decisions.length,
        newDecisionArrivalNotice: arrivalNotice,
      };
    });
  } catch {
    // Ignore fetch failures
  }
}

const loopbackHosts = new Set(['127.0.0.1', 'localhost', '::1', '[::1]']);

/**
 * Records that the network listener this browser reached the machine over was closed.
 *
 * A browser on the loopback listener keeps working, so it stays where it is and only the
 * connection banner changes; a browser on the network loses its access and lands on the page
 * that says why, with its pairing kept.
 */
function markListenerClosed(): void {
  const onLoopback = typeof window === 'undefined' || loopbackHosts.has(window.location.hostname);
  const closedAt = new Date().toISOString();
  runtimeStore.update((s) => ({
    ...s,
    isListenerClosed: true,
    listenerClosedAt: s.listenerClosedAt ?? closedAt,
    currentRoute: onLoopback ? s.currentRoute : 'listener-off',
  }));
}

/** A listener change for the LAN listener: closed marks this browser, any other state clears it. */
function handleListenerChanged(data: unknown): void {
  const changed = (data || {}) as { state?: string; kind?: string };
  if (changed.kind !== 'lan') {
    return;
  }
  if (changed.state === 'closed') {
    markListenerClosed();
  } else {
    runtimeStore.update((s) => ({ ...s, isListenerClosed: false, listenerClosedAt: null }));
  }
}

/** A state the runtime moved to: the current state, the step, and the `onboarded` flag. */
function handleStateTransition(data: unknown): void {
  const stateData = data as StateRepresentation;
  if (!stateData?.state) {
    return;
  }
  runtimeStore.update((s) => ({
    ...s,
    currentState: stateData,
    activeStep: deriveStepFromState(stateData.state),
    isOnboarded: stateData.state === 'onboard.completed' ? true : s.isOnboarded,
  }));
}

/** A new or read report: the unread count, and the quiet notice of an arrival. */
function handleReportEvent(event: string, data: unknown): void {
  if (event === 'report.sent') {
    const reportData = (data || {}) as { subject?: string };
    runtimeStore.update((s) => ({
      ...s,
      unreadReportsCount: s.unreadReportsCount + 1,
      quietReportNotice:
        reportData.subject || translate(get(currentLocale), 'report.arrival.quiet'),
    }));
  } else {
    runtimeStore.update((s) => ({
      ...s,
      unreadReportsCount: Math.max(0, s.unreadReportsCount - 1),
    }));
  }
}

/** A new or closed decision: the open count, and the quiet notice of an arrival. */
function handleDecisionEvent(event: string, data: unknown): void {
  if (event === 'decision.opened') {
    const dec = (data || {}) as { title?: string; question?: string };
    const notice =
      dec.title || dec.question || translate(get(currentLocale), 'decision.arrival.quiet');
    runtimeStore.update((s) => ({
      ...s,
      openDecisionsCount: s.openDecisionsCount + 1,
      newDecisionArrivalNotice: notice,
      quietDecisionNotice: notice,
    }));
  } else {
    runtimeStore.update((s) => ({
      ...s,
      openDecisionsCount: Math.max(0, s.openDecisionsCount - 1),
    }));
  }
}

/** The runtime's lifecycle: a drain, a withdrawn drain, or a stop. */
function handleLifecycleEvent(event: string, data: unknown): void {
  if (event === 'runtime.draining') {
    runtimeStore.update((s) => ({
      ...s,
      isDraining: true,
      drainInfo: (data || {}) as RuntimeState['drainInfo'],
    }));
  } else if (event === 'runtime.drain_withdrawn') {
    runtimeStore.update((s) => ({ ...s, isDraining: false, drainInfo: null }));
  } else {
    runtimeStore.update((s) => ({
      ...s,
      isDraining: false,
      isStopped: true,
      stoppedInfo: (data || {}) as RuntimeState['stoppedInfo'],
    }));
  }
}

const STATE_TRANSITION_EVENTS = new Set(['state_transition', 'check']);
const REPORT_EVENTS = new Set(['report.sent', 'report.read']);
const DECISION_EVENTS = new Set(['decision.opened', 'decision.answered', 'decision.closed']);
const LIFECYCLE_EVENTS = new Set([
  'runtime.draining',
  'runtime.drain_withdrawn',
  'runtime.stopping',
]);
const PROJECT_EVENTS = new Set(['project.changed', 'project.released', 'project.enrolled']);

export function handleRuntimeEvent(event: string, data: unknown): void {
  if (event === 'stream.closed') {
    if ((data as { reason?: string })?.reason === 'listener_closed') {
      markListenerClosed();
    }
  } else if (event === 'listener.changed') {
    handleListenerChanged(data);
  } else if (STATE_TRANSITION_EVENTS.has(event)) {
    handleStateTransition(data);
  } else if (REPORT_EVENTS.has(event)) {
    handleReportEvent(event, data);
  } else if (DECISION_EVENTS.has(event)) {
    handleDecisionEvent(event, data);
  } else if (LIFECYCLE_EVENTS.has(event)) {
    handleLifecycleEvent(event, data);
  } else if (PROJECT_EVENTS.has(event)) {
    runtimeStore.update((s) => ({ ...s, projectEventNotice: `${event}_${Date.now()}` }));
  } else if (event === 'onboarding.step_completed') {
    void loadOnboardingState();
  } else if (event === 'machine.settings_changed') {
    void loadMachineSettings();
  }
}

export async function pairBrowser(code: string): Promise<void> {
  const res = await apiClient.pair(code);
  const status: StatusResponse = await apiClient
    .getStatus()
    .catch(() => ({ status: 'ready', onboarded: false }) as StatusResponse);
  const onboarded =
    status.onboarded ??
    (typeof status.machine === 'object' && status.machine ? status.machine.onboarded : false) ??
    false;
  const machineName =
    typeof status.machine === 'string' ? status.machine : status.machine?.name || 'local-machine';
  // Pairing is the main path into the app, so it applies the same landing rule as a reload. A
  // deep link opened with the code keeps its route and its identifiers.
  const requested = matchPath(typeof window !== 'undefined' ? window.location.pathname : '/');
  const summary = onboarded ? await apiClient.getProjects().catch(() => null) : null;

  runtimeStore.update((s) => ({
    ...s,
    isInitialized: true,
    isPaired: true,
    isRevoked: false,
    deviceLabel: res.label,
    machineName,
    isOnboarded: onboarded,
    currentRoute: landingRoute(requested, onboarded, hasWork(summary)),
    selectedPlanId: requested?.planId ?? null,
    selectedEpicId: requested?.epicId ?? null,
    startProjectId: requested?.projectId ?? null,
    startEntryId: requested?.entryId ?? null,
    isOffline: false,
  }));
  subscribeToEvents();
  void refreshReportsCount();
  void refreshDecisionsCount();
}

export async function executeLink(link: HypermediaLink | string, body?: unknown): Promise<void> {
  const nextRep = await apiClient.followLink(link, body);
  if (nextRep?.state) {
    const activeStep = deriveStepFromState(nextRep.state);
    runtimeStore.update((s) => ({
      ...s,
      currentState: nextRep,
      activeStep,
      isOnboarded: nextRep.state === 'onboard.completed' ? true : s.isOnboarded,
    }));
  }
}

/**
 * One entry of the caller's navigation stack: where the operator was before the current view.
 */
export interface NavigationEntry {
  route: AppRoute;
  params: RouteParams;
}

/** The routes the browser can return to by the caller's own back, newest last. */
const navigationStack: NavigationEntry[] = [];

/**
 * The identifiers the store carries, so a route can be written to the address bar and pushed on
 * the caller's stack without collecting them at every call site.
 *
 * @param state the runtime state
 * @returns the identifiers of the route it holds
 */
export function routeParamsOf(state: RuntimeState): RouteParams {
  return {
    projectId: state.startProjectId ?? undefined,
    entryId: state.startEntryId ?? undefined,
    planId: state.selectedPlanId ?? undefined,
    epicId: state.selectedEpicId ?? undefined,
    taskId: state.selectedTaskId ?? undefined,
    reportPlanId: state.selectedReportDetailPlanId ?? undefined,
    explorerProjectId: state.selectedProjectId ?? undefined,
  };
}

/**
 * Applies a route and its identifiers to the store.
 *
 * @param route the route to show
 * @param params the identifiers it names
 */
function applyRoute(route: AppRoute, params: RouteParams): void {
  runtimeStore.update((s) => ({
    ...s,
    currentRoute: route,
    ...(route === 'start'
      ? {
          startProjectId: params.projectId ?? s.startProjectId ?? null,
          startEntryId: params.entryId ?? null,
        }
      : {}),
    ...(route === 'plan'
      ? {
          selectedPlanId: params.planId ?? s.selectedPlanId ?? null,
          selectedTaskId: params.taskId ?? null,
        }
      : {}),
    ...(route === 'epic' || route === 'decomposition'
      ? { selectedEpicId: params.epicId ?? s.selectedEpicId ?? null }
      : {}),
    // The reports route carries the plan whose completion report it shows, if any; a plain
    // visit to the list clears it so the query does not outlive the detail.
    ...(route === 'reports' ? { selectedReportDetailPlanId: params.reportPlanId ?? null } : {}),
    // The explorer focuses the project a caller named; a plain visit starts from the list.
    ...(route === 'projects' ? { selectedProjectId: params.explorerProjectId ?? null } : {}),
  }));
}

/**
 * Moves to a route and pushes the one left behind on the caller's navigation stack.
 *
 * @param route the route to show
 * @param params the identifiers it names, e.g. a plan's id
 */
export function pushRoute(route: AppRoute, params: RouteParams = {}): void {
  const state = get(runtimeStore);
  if (state.isInitialized) {
    navigationStack.push({ route: state.currentRoute, params: routeParamsOf(state) });
  }
  applyRoute(route, params);
  const path = pathForRoute(route, params);
  if (path) {
    syncUrl(path, false);
  }
}

/**
 * Returns to the route the caller came from.
 *
 * The browser's own history is the fallback when the caller's stack holds nothing (the view was
 * opened by a deep link or a reload).
 *
 * @returns `true` when a previous route was found
 */
export function goBack(): boolean {
  const previous = navigationStack.pop();
  if (!previous) {
    if (typeof window !== 'undefined' && window.history.length > 1) {
      window.history.back();
      return true;
    }
    return false;
  }
  applyRoute(previous.route, previous.params);
  const path = pathForRoute(previous.route, previous.params);
  if (path) {
    // The current entry is replaced, so the browser's Back does not lead into a loop.
    syncUrl(path, true);
  }
  return true;
}

/**
 * Shows a route and writes its path to the address bar.
 *
 * @param route the route to show
 * @param start the identifiers it names
 */
export function navigateTo(route: AppRoute, start: RouteParams = {}): void {
  pushRoute(route, start);
}

/**
 * Puts a path in the address bar without reloading the view.
 *
 * The history API is used directly: every route page mounts the same dispatcher, so a history
 * entry alone moves between views, and `popstate` reads it back.
 *
 * @param path the path to show
 * @param replace `true` to replace the current entry instead of pushing one
 */
export function syncUrl(path: string, replace: boolean): void {
  if (typeof window === 'undefined') {
    return;
  }
  // The address is the same only when path and query both match: a plain path must drop a stale
  // query (a report's plan, a project in the explorer) rather than leave it in the bar.
  if (`${window.location.pathname}${window.location.search}` === path) {
    return;
  }
  const method = replace ? 'replaceState' : 'pushState';
  window.history[method]({ path }, '', path);
}

/**
 * Takes the route the address bar names, if it names one of the app's routes.
 *
 * @returns the route taken, or `null` when the path names none
 */
export function adoptRouteFromLocation(): AppRoute | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const match = matchPath(window.location.pathname);
  if (match) {
    runtimeStore.update((s) => ({
      ...s,
      currentRoute: match.route,
      // The reports route keeps its plan in the query, so a reload or the browser's back
      // between two reports opens the one the address names.
      selectedReportDetailPlanId:
        match.route === 'reports' ? new URLSearchParams(window.location.search).get('plan') : null,
      selectedPlanId: match.planId ?? s.selectedPlanId ?? null,
      selectedEpicId: match.epicId ?? s.selectedEpicId ?? null,
      startProjectId: match.projectId ?? s.startProjectId ?? null,
      startEntryId: match.entryId ?? s.startEntryId ?? null,
    }));
  }
  return match?.route ?? null;
}

function applyOnboardingState(state: OnboardingState): void {
  const activeStep = deriveStepFromState(state.state);
  const completed =
    typeof state.completed_steps === 'number' ? state.completed_steps : Math.max(0, activeStep - 1);
  runtimeStore.update((s) => ({
    ...s,
    onboarding: state,
    currentState: state as unknown as StateRepresentation,
    activeStep,
    completedSteps: completed,
    isOnboarded: state.state === 'onboard.completed' ? true : s.isOnboarded,
  }));
}

/**
 * The steps the runtime reports as done, 1-based, for the step bar.
 *
 * @param completedSteps how many aspects the runtime reports as completed
 * @returns the step numbers, in order
 */
export function completedStepNumbers(completedSteps: number): number[] {
  const numbers: number[] = [];
  for (let step = 1; step <= Math.min(completedSteps, 7); step++) {
    numbers.push(step);
  }
  return numbers;
}

export async function loadOnboardingState(): Promise<OnboardingState | null> {
  if (!apiClient.isPaired()) return null;
  try {
    const state = await apiClient.getOnboardingState();
    applyOnboardingState(state);
    return state;
  } catch {
    return null;
  }
}

export async function stepOnboarding(link: string): Promise<void> {
  try {
    applyOnboardingState(await apiClient.stepOnboarding(link));
  } catch {
    // A refused step leaves the state as it is.
  }
}

/** A caught value as a message: what an `Error` says, or the value itself. */
function errorMessage(err: unknown): string {
  if (err instanceof Error) {
    return err.message;
  }
  return typeof err === 'string' ? err : '';
}

/**
 * The outcome of storing a rotated or replaced token.
 */
export interface StoreCredentialResult {
  /** `true` when the secret was stored. */
  ok: boolean;
  /** The reason a store failed, for the message; empty when it succeeded. */
  cause: string;
  /** `true` when the secret was stored but its kind could not be recorded. */
  kindFailed: boolean;
  /** `true` when the secret was stored but the settings could not be read back. */
  refreshFailed: boolean;
}

/**
 * Records the token of a credential, either rotated in place or replaced by a new entry.
 *
 * @param key the credential key, e.g. `github`
 * @param secret the new secret; it is written once and never read back
 * @param kind the kind of a replaced entry, or `null` to keep the recorded kind
 * @returns what happened, so the screen can name it
 */
export async function storeCredential(
  key: string,
  secret: string,
  kind: string | null = null,
): Promise<StoreCredentialResult> {
  const revision = get(runtimeStore).machineSettings?.revision ?? 0;
  try {
    await apiClient.setCredential(key, secret);
  } catch (err) {
    return { ok: false, cause: errorMessage(err), kindFailed: false, refreshFailed: false };
  }
  // The secret is stored; a kind change that fails does not undo it.
  if (kind) {
    let applied = false;
    try {
      const result = await apiClient.updateMachineSetting(`credential:${key}`, kind, revision);
      applied = Boolean(result?.applied);
    } catch {
      applied = false;
    }
    const refreshed = await loadMachineSettings();
    return { ok: true, cause: '', kindFailed: !applied, refreshFailed: refreshed === null };
  }
  const refreshed = await loadMachineSettings();
  return { ok: true, cause: '', kindFailed: false, refreshFailed: refreshed === null };
}

export async function startOnboardingAuto(): Promise<void> {
  try {
    applyOnboardingState(await apiClient.startOnboardingAuto());
  } catch {
    // A refused automatic run leaves the state as it is.
  }
}

export async function loadMachineSettings(): Promise<MachineSettingsRecord | null> {
  if (!apiClient.isPaired()) return null;
  try {
    const settings = await apiClient.getMachineSettings();
    runtimeStore.update((s) => ({ ...s, machineSettings: settings }));
    return settings;
  } catch {
    return null;
  }
}

export async function updateMachineSetting(
  key: string,
  value: string | null,
): Promise<MachineSettingsUpdateResult | null> {
  const revision = get(runtimeStore).machineSettings?.revision ?? 0;
  try {
    const result = await apiClient.updateMachineSetting(key, value, revision);
    await loadMachineSettings();
    return result;
  } catch {
    return null;
  }
}

export async function withdrawConsent(kind: string): Promise<MachineSettingsUpdateResult | null> {
  const revision = get(runtimeStore).machineSettings?.revision ?? 0;
  try {
    const result = await apiClient.withdrawConsent(kind, revision);
    await loadMachineSettings();
    return result;
  } catch {
    return null;
  }
}

export function openPlanView(planId: string, taskId?: string | null): void {
  runtimeStore.update((s) => ({ ...s, selectedPlanEnded: null }));
  pushRoute('plan', { planId, taskId: taskId ?? undefined });
}

/**
 * Opens an ended plan's view with how it ended, so the head can say so.
 *
 * @param info the ended plan as Ended Work named it
 */
export function openEndedPlanView(info: EndedPlanInfo): void {
  runtimeStore.update((s) => ({ ...s, selectedPlanEnded: info }));
  pushRoute('plan', { planId: info.plan_id });
}

export function openEpicView(epicId: string): void {
  pushRoute('epic', { epicId });
}

/**
 * Opens a plan's completion report in one step, on the reports route.
 *
 * @param planId the plan whose completion report to show
 */
export function openCompletionReport(planId: string): void {
  pushRoute('reports', { reportPlanId: planId });
}

/**
 * Opens the project explorer at one project, its detail one step away.
 *
 * @param projectId the project to show
 */
export function openProjectInExplorer(projectId: string): void {
  pushRoute('projects', { explorerProjectId: projectId });
}

export function openEpicDecomposition(epicId: string, projectId: string): void {
  runtimeStore.update((s) => ({ ...s, selectedProjectId: projectId }));
  pushRoute('decomposition', { epicId });
}

/**
 * Opens the project onboarding route, at the run the explorer selected.
 *
 * @param onboardingId the being-added run's onboarding id or checkout path; `null` for a new run
 * @param projectId the project, once the run has reached its enrolment
 */
export function openProjectOnboarding(
  onboardingId?: string | null,
  projectId?: string | null,
): void {
  runtimeStore.update((s) => ({
    ...s,
    selectedOnboardingId: onboardingId ?? null,
    selectedProjectId: projectId ?? null,
  }));
  pushRoute('project-onboarding');
}

/**
 * Opens the machine onboarding at one aspect, so a `Change` on the machine settings lands on the
 * step that records it rather than on the first step.
 *
 * @param step the aspect's step, 1-based
 */
export function openOnboardingStep(step: number): void {
  runtimeStore.update((s) => ({ ...s, onboardingStep: step }));
  pushRoute('onboarding');
  // The step travels in the address bar too, so a reload opens the same aspect.
  syncUrl(`/onboarding?step=${step}`, true);
}

/**
 * The step the address bar or the store asked the onboarding to open at.
 *
 * @param maxStep the last step the screen offers
 * @returns the requested step, or `null` when none was asked for
 */
export function requestedOnboardingStep(maxStep: number): number | null {
  const fromStore = get(runtimeStore).onboardingStep;
  const fromUrl =
    typeof window !== 'undefined'
      ? Number.parseInt(new URLSearchParams(window.location.search).get('step') ?? '', 10)
      : Number.NaN;
  const requested = Number.isFinite(fromUrl) ? fromUrl : fromStore;
  if (typeof requested !== 'number' || !Number.isInteger(requested)) return null;
  return requested >= 1 && requested <= maxStep ? requested : null;
}
