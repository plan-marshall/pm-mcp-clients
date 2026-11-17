import { writable, get } from 'svelte/store';
import {
  apiClient,
  type StatusResponse,
  type StateRepresentation,
  type OnboardingState,
  type MachineSettingsRecord,
  type MachineSettingsUpdateResult,
  type ProjectOverviewResponse,
  type EventStreamState,
} from '../api/client';
import {
  matchPath,
  SETTINGS_TABS,
  type Address,
  type AppRoute,
  type ObjectTab,
  type RouteParams,
  type StartPath,
} from '../routes';
import {
  currentAddress,
  intentFor,
  openedFromOutside,
  parentOf,
  switchTab,
  writeAddress,
  type WriteIntent,
} from '../navigation/history';
import {
  readRememberedPlace,
  runningWorkAddress,
  startAddress,
  unfilledLensAddress,
  writeRememberedPlace,
  type PlaceFacts,
  type RememberedPlace,
} from '../navigation/rememberedPlace';
import { adoptRecordedLocale, currentLocale, translate } from '../i18n';
import { actFailureReason, errorCodeOf } from '../api/errors';
import { handleNeedsOperatorChanged, loadAttention } from './attention';

export type { AppRoute, RouteParams, StartPath };

/**
 * The state of the one event stream (UX-P-12).
 *
 * `connecting` is the first attempt, `connected` a live stream, `reconnecting` a dropped stream
 * that retries, and `offline` the state after the retries gave up — the one that offers `Try
 * again`.
 */
export type StreamState = EventStreamState;

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
  /** The state of the event stream, for the live indicator. */
  streamState: StreamState;
  /** The network listener this browser reached the machine over was turned off. */
  isListenerClosed: boolean;
  /** When that listener was turned off, for the page that waits for it. */
  listenerClosedAt?: string | null;
  isDraining: boolean;
  drainInfo?: {
    count?: number;
    what?: string;
    who?: string;
    since?: string;
    /** What still runs, where the runtime names the items; the count is then its length. */
    items?: string[];
  } | null;
  isStopped: boolean;
  stoppedInfo?: { command?: string; who?: string; time?: string } | null;
  activeStep: number;
  /** The steps the runtime reports as done, 1-based. */
  completedSteps: number;
  unreadReportsCount: number;
  quietReportNotice?: string | null;
  selectedReportDetailPlanId?: string | null;
  openDecisionsCount: number;
  quietDecisionNotice?: string | null;
  /** What the column engine announces after a window shift, in the frame's polite live region. */
  quietNavigationNotice?: string | null;
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
  /** The project the designer opens, carried in `/designer?project=…`. */
  designerProjectId?: string | null;
  /** The onboarding step another screen opened the onboarding at, 1-based. */
  onboardingStep?: number | null;
  /** How an ended plan ended, when a view opened it from Ended Work. */
  selectedPlanEnded?: EndedPlanInfo | null;
  /** The cleanup items the operator unchecked, kept while a preview or another view is open. */
  cleanupUncheckedIds?: Record<string, boolean>;
  /** The job those unchecked items belong to; a reminder opens a new job with no checks kept. */
  cleanupSelectionJobId?: string | null;
  /** The cleanup item whose read-only preview is open, if any. */
  cleanupPreviewItemId?: string | null;
  /** The cleanup item a report's detail opened the job at, highlighted once. */
  cleanupHighlightItemId?: string | null;
  /** The decision the decisions route focuses, as a match of the command palette named it. */
  selectedDecisionId?: string | null;
  /** The work kind a figure asked the overview to show, consumed when it opens. */
  overviewFocus?: 'plans' | 'epics' | null;
  /** The report one `/reports/{id}` address names, until the view resolves its plan. */
  selectedReportId?: string | null;
  /** The lens's scope: the project Running work is narrowed to, `?project=`. */
  lensProject?: string | null;
  /** The lens's mode, scope and follow, as the address carries them. */
  lensMode?: 'ended' | null;
  lensGroup?: 'epic' | null;
  lensFollow?: boolean;
  /** The tab of the object column the address names. */
  objectTab?: string | null;
  /** The object the address opens in place beside its caller. */
  openObject?: { kind: string; id: string } | null;
  /** The node of the plan's phase tree and the file of its changes, as the address names them. */
  phaseNode?: string | null;
  changeFile?: string | null;
  /** Whether the chat stands as a side column, and the turn it scrolls to. */
  chatOpen?: boolean;
  chatTurn?: string | null;
  /** The start page's own lens: `?lens=waits`, which the remembered place never fills. */
  lens?: 'waits' | null;
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
  streamState: 'connecting',
  isListenerClosed: false,
  listenerClosedAt: null,
  isDraining: false,
  drainInfo: null,
  isStopped: false,
  stoppedInfo: null,
  activeStep: 1,
  completedSteps: 0,
  unreadReportsCount: 0,
  quietReportNotice: null,
  selectedReportDetailPlanId: null,
  openDecisionsCount: 0,
  quietDecisionNotice: null,
  quietNavigationNotice: null,
  projectEventNotice: null,
  selectedPlanId: null,
  selectedEpicId: null,
  selectedProjectId: null,
  selectedTaskId: null,
  startProjectId: null,
  startEntryId: null,
  startKind: 'plan',
  selectedOnboardingId: null,
  designerProjectId: null,
  onboardingStep: null,
  selectedPlanEnded: null,
  cleanupUncheckedIds: {},
  cleanupSelectionJobId: null,
  cleanupPreviewItemId: null,
  cleanupHighlightItemId: null,
  overviewFocus: null,
  selectedReportId: null,
  lensProject: null,
  lensMode: null,
  lensGroup: null,
  lensFollow: false,
  objectTab: null,
  openObject: null,
  phaseNode: null,
  changeFile: null,
  chatOpen: false,
  chatTurn: null,
  lens: null,
};

export const runtimeStore = writable<RuntimeState>(initialState);

export function deriveStepFromState(stateId?: string): number {
  if (!stateId) return 1;
  // The mode screen stands before step 1: the bar marks no step there (onboarding.adoc, Frame).
  if (stateId.startsWith('onboard.mode') || stateId.startsWith('onboard.entry')) return 0;
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

/**
 * The quiet timer on which the app tries again to reach a runtime it could not reach; the pages
 * of a lost connection wait on the same one.
 */
export const RECONNECT_INTERVAL_MS = 15_000;

/** The timer that retries a failed initialization, while one is owed. */
let recovery: ReturnType<typeof setInterval> | null = null;
/** Whether a retry of the initialization is in flight, so a second one is not started beside it. */
let retryInFlight = false;
/**
 * Counts the stops of the retry. An initialization notes the count at its start, and its failure
 * starts no retry once a stop came after that start: the shell was torn down meanwhile, or another
 * initialization succeeded.
 */
let recoveryStops = 0;

/**
 * Starts retrying the initialization after one failed.
 *
 * The store owns the retry, not the page that shows the lost connection: any successful request
 * clears the offline state and removes that page, but only a successful initialization loads the
 * runtime's state and opens the event stream. The retry therefore runs until one initialization has
 * succeeded, whatever clears the offline state in the meantime.
 *
 * @param stopsAtStart the count of stops the failed initialization noted at its start; a stop
 *   since then wins over the late failure, so no timer outlives the shell that ended the retry
 */
function startRecovery(stopsAtStart: number): void {
  if (recovery !== null || stopsAtStart !== recoveryStops) return;
  recovery = setInterval(() => void retryRuntime(), RECONNECT_INTERVAL_MS);
}

/** Stops retrying the initialization: one succeeded, or the app is torn down. */
export function stopRuntimeRecovery(): void {
  recoveryStops += 1;
  if (recovery !== null) {
    clearInterval(recovery);
    recovery = null;
  }
}

/**
 * A test hook: no view reads it; the store's tests observe the retry timer through it.
 *
 * @returns whether a retry of the initialization is owed and its timer runs
 */
export function isRuntimeRecovering(): boolean {
  return recovery !== null;
}

/**
 * Runs the initialization again, unless a retry is already in flight. The store's own timer and
 * the page of the unreachable runtime both retry through it, so they never run two side by side.
 */
export async function retryRuntime(): Promise<void> {
  if (retryInFlight) return;
  retryInFlight = true;
  try {
    await initRuntime();
  } finally {
    retryInFlight = false;
  }
}

/** Where the person stood: the address and the route the store showed. */
interface RouteMark {
  path: string;
  route: AppRoute;
}

/** @returns where the person stands now */
function routeMark(): RouteMark {
  return {
    path: window.location.pathname,
    route: get(runtimeStore).currentRoute,
  };
}

/**
 * @param mark where the person stood when the initialization started
 * @returns whether the person moved since: the initialization then writes no route of its own,
 *   so a late answer never takes them back to the address it read at its start
 */
function navigatedSince(mark: RouteMark): boolean {
  const now = routeMark();
  return now.path !== mark.path || now.route !== mark.route;
}

function subscribeToEvents(): void {
  if (eventsUnsubscribe) {
    eventsUnsubscribe();
    eventsUnsubscribe = null;
  }
  eventsUnsubscribe = apiClient.subscribeEvents((event, data) => {
    handleRuntimeEvent(event, data);
  });
}

/**
 * What the probe of an existing pairing found.
 *
 * `still-paired` keeps the pairing and never redeems; `refused` means the stored secret is no
 * longer accepted, so the code in the address bar is redeemed; `offline` keeps the fragment.
 */
type PairingProbe = 'unpaired' | 'still-paired' | 'refused' | 'offline';

/**
 * Probes the stored secret before a code in the address bar is redeemed.
 *
 * @returns what the probe found
 */
async function probeExistingPairing(): Promise<PairingProbe> {
  if (!apiClient.isPaired()) {
    return 'unpaired';
  }
  try {
    await apiClient.getStatus();
    return 'still-paired';
  } catch (err: unknown) {
    return (err as Error)?.message === 'unauthorized' ? 'refused' : 'offline';
  }
}

/**
 * The address the bar names, mapped to the start point it is.
 *
 * `/` is the list of what waits and parses here like any route; a path the app cannot open
 * answers `null`, so the start rule opens what waits instead of guessing.
 *
 * @returns the address of the bar, or `null` when it names a path the app cannot open
 */
function fillableStart(): Address | null {
  return currentAddress();
}

/** Drops the code fragment after the browser is known to be paired. */
function dropCodeFragment(): void {
  // The fragment is not part of the address the app writes, so the pairing code never reaches the
  // history. The mark of this load was read before this write, so the write never turns a load the
  // app was opened at into an app-created one.
  const address = currentAddress();
  if (address) {
    writeAddress(address, 'replace');
  } else {
    // The start point has no route of its own yet (PLAN-49 registers `/`): the fragment is dropped
    // and the entry the app leaves carries its mark.
    window.history.replaceState(
      { path: window.location.pathname, pmApp: true },
      '',
      window.location.pathname,
    );
  }
  runtimeStore.update((s) => ({ ...s, pairingFailed: false }));
}

export async function checkUrlHashForCode(): Promise<boolean> {
  const match = /code=([^&]+)/.exec(window.location.hash);
  if (!match?.[1]) {
    return false;
  }
  const extracted = decodeURIComponent(match[1]);

  // A code is single use and this browser already holds a secret: probe it rather than redeeming
  // again (the spec's Redeem rule). Only a refused secret falls through.
  const probe = await probeExistingPairing();
  if (probe === 'still-paired') {
    dropCodeFragment();
    return false;
  }
  if (probe === 'offline') {
    // The runtime is unreachable: keep the fragment so a later visit can retry.
    runtimeStore.update((s) => ({ ...s, isOffline: true, pairingFailed: false }));
    return false;
  }

  try {
    await pairBrowser(extracted);
    dropCodeFragment();
    return true;
  } catch {
    // Keep the fragment: a premature rewrite loses the code and makes the refusal look like a
    // plain redirect to /pair. The pairing page shows that the code was refused.
    runtimeStore.update((s) => ({ ...s, pairingFailed: true }));
    return false;
  }
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
 * The one start rule of the app, as a route.
 *
 * The remembered place fills a mark-less load at `/` or at the bare `/overview`; a full address
 * opens what it names, and without a place — and without a remembered place — the app opens what
 * waits, its start page at `/` (needs-and-find.adoc, The Start Page Is What Waits). The onboarding
 * comes first while the machine is not onboarded.
 *
 * @param requested the address the bar names, if it names one
 * @param onboarded whether the machine is onboarded
 * @param place the remembered place
 * @param facts what the runtime knows about the remembered objects
 * @param fromOutside whether the load carries no mark of the app
 * @returns the route to open
 */
export function startRouteFor(
  requested: Address | null,
  onboarded: boolean,
  place: RememberedPlace,
  facts: PlaceFacts,
  fromOutside: boolean,
): AppRoute {
  return startAddress({ fromOutside, requested, onboarded, place, facts }).route ?? 'inbox';
}

/**
 * What the start needs to know about the remembered place: the work it names and the work of its
 * project.
 *
 * @param summary the projects summary, or `null` when it could not be read
 * @param place the remembered place
 * @returns the facts the fall-backs read
 */
export function factsOf(summary: ProjectOverviewResponse | null, place: RememberedPlace): PlaceFacts {
  const projects = summary?.projects ?? [];
  const project = place.project
    ? projects.find((entry) => entry.project_id === place.project)
    : undefined;
  const projectRuns = project ? hasWork({ projects: [project] }) : true;
  return {
    workRunning: hasWork(summary),
    projectRuns,
    // The summary names no plan, so a remembered plan opens while its project still runs work and
    // falls back to the project otherwise.
    planEnded: projectRuns ? false : undefined,
  };
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

/**
 * Opens the operator's start page by the same rule as a first visit.
 *
 * The brand click names no address of its own: the start rule decides, so a click never fills the
 * remembered place (that is the Running work entry's job). A route already shown is not written
 * again, so the brand click adds no history entry.
 */
export async function openStartPage(): Promise<void> {
  if (!apiClient.isPaired()) {
    return;
  }
  const state = get(runtimeStore);
  const intent = beginNavigation();
  const epoch = navigationEpochOf();
  const summary = state.isOnboarded ? await apiClient.getProjects().catch(() => null) : null;
  // The person moved while the summary was in flight, or a later navigation began meanwhile:
  // their choice stands.
  if (intent !== navigationIntent || epoch !== navigationEpochOf()) {
    return;
  }
  const place = readRememberedPlace();
  const target = startRouteFor(null, state.isOnboarded, place, factsOf(summary, place), false);
  if (target !== get(runtimeStore).currentRoute) {
    show({ route: target }, 'replace');
  }
}

/**
 * Shows an address and writes it to the bare, as the app's own navigation.
 *
 * @param address the address to show
 * @param intent whether the change pushes a history entry or replaces the current one
 */
function show(address: Address, intent: WriteIntent): void {
  applyRoute(address.route ?? 'overview', address);
  runtimeStore.update((s) => ({
    ...s,
    ...fieldsOfAddress(address),
    ...(address.project ? { selectedProjectId: address.project } : {}),
  }));
  // The address is written as it was built: the store carries the selection, not every query key
  // (the lens scope is not a store field of its own).
  writeAddress(address, intent);
}

/**
 * The store fields an address names, so the bar and the store never disagree.
 *
 * @param address the address that was read or written
 * @returns the fields it sets
 */
function fieldsOfAddress(address: Address): Partial<RuntimeState> {
  return {
    lensProject: address.project ?? null,
    lensMode: address.mode ?? null,
    lensGroup: address.group ?? null,
    lensFollow: address.follow ?? false,
    objectTab: address.tab ?? null,
    openObject: address.open ?? null,
    phaseNode: address.node ?? null,
    changeFile: address.file ?? null,
    chatOpen: address.chat ?? false,
    chatTurn: address.turn ?? null,
    lens: address.lens ?? null,
  };
}

/**
 * Opens Running work and fills it from the remembered place, by a push.
 *
 * The rail's entry and the phone's bottom bar both call it; the pushed entry carries the app's
 * mark, so a reload of it stays as it is.
 */
export async function openRunningWork(): Promise<void> {
  if (!apiClient.isPaired()) {
    return;
  }
  const intent = beginNavigation();
  const epoch = navigationEpochOf();
  const summary = await apiClient.getProjects().catch(() => null);
  // The person picked another destination while the summary was in flight, or a later navigation
  // began meanwhile: it wins.
  if (intent !== navigationIntent || epoch !== navigationEpochOf()) {
    return;
  }
  const place = readRememberedPlace();
  show(runningWorkAddress(place, factsOf(summary, place)), 'push');
}

/**
 * Opens the lens's own list with nothing selected, by a push: what Up and the breadcrumb's lens
 * segment do, and PLAN-144 calls it.
 */
export function openLensList(): void {
  show(unfilledLensAddress(), 'push');
}

/**
 * Opens the start page, the list of what waits, by a push.
 *
 * The rail's first destination and the phone's first item both call it. The pushed entry carries
 * the lens's own address (`/?lens=waits`), so a reload of it stays on the list and the remembered
 * place never fills it — the remembered place only fills a bare load at `/`.
 */
export function openAttention(): void {
  if (!apiClient.isPaired()) {
    return;
  }
  show({ route: 'inbox', lens: 'waits' }, 'push');
}

/**
 * Shows an address of the app and writes it, as the app's own navigation.
 *
 * The attention list's act opens the place an item belongs to through it: the address carries what
 * `pushRoute` takes from the store (a settings tab, for one), so the act never opens a place the
 * address does not name.
 *
 * @param address the address to show
 */
export function openAddress(address: Address): void {
  show(address, 'push');
}

/**
 * @param status the status the runtime reports
 * @returns the lifecycle fields the store keeps, so every read of them agrees
 */
export function lifecycleFieldsOf(
  status: StatusResponse,
): Pick<RuntimeState, 'isDraining' | 'drainInfo' | 'isStopped'> {
  // The daemon's own status resource writes the lifecycle under `state`; the mock and the tests
  // use `lifecycle_state`. Both are read, the daemon's first.
  const lifecycle = status.state ?? status.lifecycle_state;
  const draining = lifecycle === 'draining';
  return {
    isDraining: draining,
    drainInfo: draining
      ? drainInfoOf(status.drain_remaining_items, status.drain_remaining_count, status.draining_by)
      : null,
    isStopped: lifecycle === 'stopped',
  };
}

/**
 * @param items the names of what still runs, where the runtime sends them
 * @param count the count the runtime reports beside them
 * @param who who asked for the drain
 * @returns what the drain still holds; a named list is its own count, and an empty list says
 *   nothing about the count the runtime reported beside it
 */
function drainInfoOf(
  items: string[] | undefined,
  count: number | undefined,
  who: string | undefined,
): NonNullable<RuntimeState['drainInfo']> {
  return {
    who,
    items,
    count: items && items.length > 0 ? items.length : count,
  };
}

export async function initRuntime(): Promise<void> {
  // Whether the app was opened from outside is read before anything writes: dropping a pairing
  // fragment is a write of this load, and it must not make the load look app-created.
  const fromOutside = openedFromOutside();
  const stopsAtStart = recoveryStops;
  if (!hashListenerRegistered) {
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

  apiClient.setOnOnline(() => {
    // Only a real state change notifies: the request that reports it must not restart the
    // observers that issued it.
    if (get(runtimeStore).isOffline) {
      runtimeStore.update((s) => ({ ...s, isOffline: false }));
    }
  });

  // The stream reports its own state, so the live indicator says what the connection does instead
  // of inferring it from a request that happened to fail.
  apiClient.setOnStreamState((state) => {
    if (get(runtimeStore).streamState !== state) {
      runtimeStore.update((s) => ({ ...s, streamState: state }));
    }
  });

  const hasCodeInHash = await checkUrlHashForCode();
  if (hasCodeInHash) {
    return;
  }

  const isPaired = apiClient.isPaired();
  const label = apiClient.getLabel() || '';
  const requested = matchPath(window.location.pathname);
  const mark = routeMark();

  if (!isPaired) {
    stopRuntimeRecovery();
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

    // A person who moved while the reads were in flight stays where they went.
    const place = readRememberedPlace();
    const facts = factsOf(summary, place);
    const start = startAddress({
      fromOutside,
      requested: fillableStart(),
      onboarded,
      place,
      facts,
    });
    const navigated = navigatedSince(mark);
    // The start address is written before any page adopts the bar, so the fill of the remembered
    // place is what the page reads. A replace, never a push: the entry the app was opened at is
    // the one it shows.
    if (!navigated && start.route) {
      writeAddress(start, 'replace');
    }
    const route = navigated
      ? {}
      : {
          currentRoute: start.route ?? (facts.workRunning ? 'overview' : 'projects'),
          ...fieldsOfAddress(start),
          selectedPlanId: start.planId ?? null,
          selectedEpicId: start.epicId ?? null,
          selectedTaskId: start.taskId ?? null,
          startProjectId: start.projectId ?? null,
          startEntryId: start.entryId ?? null,
        };

    runtimeStore.update((s) => ({
      ...s,
      isInitialized: true,
      isPaired: true,
      isOnboarded: onboarded,
      deviceLabel: label,
      machineName,
      ...route,
      currentState,
      activeStep: deriveStepFromState(currentState?.state),
      ...lifecycleFieldsOf(status),
      isOffline: false,
      isRevoked: false,
    }));

    // The runtime's state is loaded: no retry is owed any longer.
    stopRuntimeRecovery();
    subscribeToEvents();
    void refreshReportsCount();
    void refreshDecisionsCount();
    void loadAttention();
  } catch (err: unknown) {
    if ((err as Error)?.message === 'unauthorized') {
      stopRuntimeRecovery();
      return;
    }
    const moved = navigatedSince(mark);
    runtimeStore.update((s) => ({
      ...s,
      isInitialized: true,
      // The browser still holds its pairing; a status read that failed is no reason to send the
      // person back to the pairing page, or away from the address they opened. The view that
      // address names then draws the error pattern of the load it could not finish. The pairing and
      // entry pages are not views to land on, so they keep the previous route instead, and so does
      // a person who moved while the read was in flight.
      isPaired: true,
      isOffline: true,
      currentRoute:
        !moved && requested && requested.route !== 'pair' && requested.route !== 'entry'
          ? requested.route
          : s.currentRoute,
      selectedPlanId: moved ? s.selectedPlanId : (requested?.planId ?? s.selectedPlanId),
      selectedEpicId: moved ? s.selectedEpicId : (requested?.epicId ?? s.selectedEpicId),
      startProjectId: moved ? s.startProjectId : (requested?.projectId ?? s.startProjectId),
      startEntryId: moved ? s.startEntryId : (requested?.entryId ?? s.startEntryId),
      // No stream opens on this path, so the indicator must offer the one way back — unless the
      // retry already connected one, whose state then stands.
      streamState: s.streamState === 'connected' ? 'connected' : 'offline',
    }));
    // The runtime's state was not loaded: retry until an initialization succeeds, unless the
    // retry was stopped while this one waited.
    startRecovery(stopsAtStart);
  }
}

/**
 * Retries the event stream after it gave up, and re-reads the runtime.
 *
 * The indicator's `Try again` calls it: the stream reconnects at once, and the status read that
 * follows settles whether the machine is reachable again.
 */
export function retryEventStream(): void {
  // The offline flag stays until the status request settles: it is what keeps the indicator — and
  // its Try again — on screen while the retried read is in flight.
  runtimeStore.update((s) => ({ ...s, streamState: 'connecting' }));
  apiClient.retryEventStream();
  void initRuntime();
}

export async function refreshReportsCount(): Promise<void> {
  if (!apiClient.isPaired()) return;
  try {
    const page = await apiClient.getReports();
    runtimeStore.update((s) => ({
      ...s,
      unreadReportsCount: page.unread_count,
    }));
  } catch {
    // Ignore fetch failures
  }
}

export async function refreshDecisionsCount(): Promise<void> {
  if (!apiClient.isPaired()) return;
  try {
    const decisions = await apiClient.getDecisions();
    runtimeStore.update((s) => ({
      ...s,
      openDecisionsCount: decisions.length,
    }));
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
  const onLoopback = loopbackHosts.has(window.location.hostname);
  const closedAt = new Date().toISOString();
  runtimeStore.update((s) => ({
    ...s,
    isListenerClosed: true,
    listenerClosedAt: s.listenerClosedAt ?? closedAt,
    currentRoute: onLoopback ? s.currentRoute : 'listener-off',
  }));
}

/**
 * A listener change for the LAN listener: closed marks this browser, any other state clears it.
 *
 * The event names the listener in `listener` (`ListenerChangedEvent {listener, state, port}`).
 */
function handleListenerChanged(data: unknown): void {
  const changed = (data || {}) as { state?: string; listener?: string };
  if (changed.listener !== 'lan') {
    return;
  }
  if (changed.state === 'closed') {
    markListenerClosed();
  } else {
    runtimeStore.update((s) => ({ ...s, isListenerClosed: false, listenerClosedAt: null }));
  }
}

/**
 * Announces the column now in view after a shift of the column window, in the frame's polite live
 * region (`navigation.adoc#keys`). The engine calls it; the frame owns the region.
 *
 * @param text what the shift moved to — the column's name and its kind
 */
export function announceNavigation(text: string): void {
  runtimeStore.update((s) => ({ ...s, quietNavigationNotice: text }));
}

/**
 * Falls back to the parent when the object the address shows was deleted.
 *
 * The fallback replaces the address rather than pushing, so the browser's Back never returns to
 * the deleted object, and the frame's live region says what went away (`web.nav.gone`).
 *
 * @param name the object's name, as the notice prints it
 * @returns `true` when the address moved to the parent
 */
export function fallBackFromDeletedObject(name: string): boolean {
  const parent = parentOf(addressOfState(get(runtimeStore)));
  if (!parent?.route) return false;
  applyRoute(parent.route, parent);
  writeAddress(addressOfState(get(runtimeStore)), 'replace');
  announceNavigation(translate(get(currentLocale), 'web.nav.gone', { name }));
  return true;
}

/** A new or read report: the unread count, and the quiet notice of an arrival. */
function handleReportEvent(event: string, data: unknown): void {
  if (event === 'report.sent') {
    const payload = (data || {}) as { report?: { subject?: string }; subject?: string };
    const reportData = payload.report ?? payload;
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
      quietDecisionNotice: notice,
    }));
  } else {
    runtimeStore.update((s) => ({
      ...s,
      openDecisionsCount: Math.max(0, s.openDecisionsCount - 1),
    }));
  }
}

/**
 * How many lifecycle changes the store has seen. A snapshot of the status is only applied while
 * this is the value it was read at, so a lifecycle event that arrives during the read wins over
 * the older answer.
 */
let lifecycleEpoch = 0;

/** @returns the number of lifecycle changes the store has seen so far */
export function lifecycleEpochOf(): number {
  return lifecycleEpoch;
}

/** Advances the lifecycle epoch, so older snapshots cannot overwrite a newer act or event. */
export function advanceLifecycleEpoch(): number {
  lifecycleEpoch += 1;
  return lifecycleEpoch;
}

/**
 * @param data the payload of a `runtime.stopping` event, `{lifecycle_state, then}`
 * @returns whether the stop is a restart: its `then` is `restart`, or the runtime already reports
 *   itself running again
 */
function isRestart(data: unknown): boolean {
  const payload = (data || {}) as { then?: string; lifecycle_state?: string };
  return payload.then === 'restart' || payload.lifecycle_state === 'running';
}

/** The runtime's lifecycle: a drain, a withdrawn drain, a restart, or a stop. */
function handleLifecycleEvent(event: string, data: unknown): void {
  advanceLifecycleEpoch();
  if (event === 'runtime.draining') {
    const payload = (data || {}) as {
      who?: string;
      count?: number;
      draining_by?: string;
      remaining_count?: number;
      remaining_work_count?: number;
      remaining_items?: string[];
      items?: string[];
    };
    const items = payload.remaining_items ?? payload.items;
    runtimeStore.update((s) => ({
      ...s,
      isDraining: true,
      isStopped: false,
      stoppedInfo: null,
      drainInfo: drainInfoOf(
        items,
        payload.count ?? payload.remaining_count ?? payload.remaining_work_count,
        payload.who ?? payload.draining_by,
      ),
    }));
  } else if (event === 'runtime.drain_withdrawn') {
    runtimeStore.update((s) => ({ ...s, isDraining: false, drainInfo: null }));
  } else if (isRestart(data)) {
    // A restart has its successor started on the same release: the drain ends, but the runtime
    // is not stopped, so no page tells the operator to start it on the machine.
    runtimeStore.update((s) => ({
      ...s,
      isDraining: false,
      drainInfo: null,
      isStopped: false,
      stoppedInfo: null,
    }));
  } else {
    runtimeStore.update((s) => ({
      ...s,
      isDraining: false,
      isStopped: true,
      stoppedInfo: (data || {}) as RuntimeState['stoppedInfo'],
    }));
  }
}

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
  } else if (event === 'needs_operator.changed') {
    // The list of what waits is the shell's count and the start page's rows: it follows every
    // change the runtime announces, whoever resolved the item.
    void handleNeedsOperatorChanged();
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
  const requested = matchPath(window.location.pathname);
  const summary = onboarded ? await apiClient.getProjects().catch(() => null) : null;
  const place = readRememberedPlace();

  // The recorded interface language is settled before the shell first renders, so the app never
  // paints one language and then switches under the operator.
  await adoptRecordedLocale();

  runtimeStore.update((s) => ({
    ...s,
    isInitialized: true,
    isPaired: true,
    isRevoked: false,
    deviceLabel: res.label,
    machineName,
    isOnboarded: onboarded,
    currentRoute: startRouteFor(
      currentAddress(),
      onboarded,
      place,
      factsOf(summary, place),
      false,
    ),
    selectedPlanId: currentAddress()?.planId ?? requested?.planId ?? null,
    selectedEpicId: requested?.epicId ?? null,
    startProjectId: requested?.projectId ?? null,
    startEntryId: requested?.entryId ?? null,
    // A browser that pairs during a drain shows the banner at once, not after the next event.
    ...lifecycleFieldsOf(status),
    isOffline: false,
  }));
  subscribeToEvents();
  void refreshReportsCount();
  void refreshDecisionsCount();
  void loadAttention();
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
 * How many route applications this store has made. A call that awaits a read before it navigates
 * notes this figure first and writes only while it is unchanged, so a person who picked another
 * destination meanwhile is not moved back by the older answer.
 */
let navigationEpoch = 0;

/** @returns the number of route applications this store has made so far */
export function navigationEpochOf(): number {
  return navigationEpoch;
}

/**
 * How many navigations this store has begun. A call that awaits a read before it navigates takes
 * this figure first: the later of two calls wins, whichever answer arrives first.
 */
let navigationIntent = 0;

/** @returns the intent figure a navigation begins at */
function beginNavigation(): number {
  navigationIntent += 1;
  return navigationIntent;
}

/**
 * Records that the store adopted a route it did not begin — the browser's own back or forward.
 *
 * It is a route application like any other, so a call that waits on a read before it navigates
 * sees it and leaves the adopted route in place.
 */
function noteNavigationAdopted(): void {
  navigationEpoch += 1;
}

/**
 * The project whose id a route's path carries, if any.
 *
 * The designer names the project it shows, the one project page names the selected project, and
 * every other route that carries a project id is a start.
 *
 * @param state the runtime state
 * @returns the project id, or `null` when the route carries none
 */
function projectIdOf(state: RuntimeState): string | null {
  if (state.currentRoute === 'designer') return state.designerProjectId ?? null;
  if (state.currentRoute === 'project') return state.selectedProjectId ?? null;
  return state.startProjectId ?? null;
}

/**
 * The identifiers the store carries, so a route can be written to the address bar and pushed on
 * the caller's stack without collecting them at every call site.
 *
 * @param state the runtime state
 * @returns the identifiers of the route it holds
 */
export function routeParamsOf(state: RuntimeState): RouteParams {
  return {
    projectId: projectIdOf(state) ?? undefined,
    entryId: state.startEntryId ?? undefined,
    planId: state.selectedPlanId ?? undefined,
    epicId: state.selectedEpicId ?? undefined,
    taskId: state.selectedTaskId ?? undefined,
    decisionId: state.selectedDecisionId ?? undefined,
    reportId: state.selectedReportId ?? undefined,
    // The onboarding step is part of the address while the onboarding is the shown route. The view
    // consumes the store's copy once, but the address keeps it so a reload opens the same aspect.
    onboardingStep:
      state.currentRoute === 'onboarding'
        ? (requestedOnboardingStepFromUrl() ?? state.onboardingStep ?? undefined)
        : undefined,
  };
}

/**
 * Applies a route and its identifiers to the store.
 *
 * @param route the route to show
 * @param params the identifiers it names
 */
function applyRoute(route: AppRoute, params: RouteParams): void {
  navigationEpoch += 1;
  runtimeStore.update((s) => ({
    ...s,
    currentRoute: route,
    ...(route === 'start'
      ? {
          startProjectId: params.projectId ?? s.startProjectId ?? null,
          startEntryId: params.entryId ?? null,
        }
      : {}),
    // The plan and the task: a task is a level of its own and carries the plan with it.
    ...(route === 'plan' || route === 'task'
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
    // The decisions route focuses the decision a match opened, if one was named.
    ...(route === 'decisions' || route === 'decision'
      ? { selectedDecisionId: params.decisionId ?? null }
      : {}),
    // One report's own address names the report; the list clears it.
    ...(route === 'report' ? { selectedReportId: params.reportId ?? null } : {}),
    ...(route === 'reports' ? { selectedReportId: null } : {}),
    // The explorer focuses the project a caller named; a plain visit starts from the list.
    ...(route === 'projects' || route === 'project'
      ? { selectedProjectId: params.projectId ?? params.explorerProjectId ?? null }
      : {}),
    // The designer carries the project it was opened for; a plain visit preselects none.
    ...(route === 'designer' ? { designerProjectId: params.projectId ?? null } : {}),
    // A settings tab belongs to the settings view. A tab of a plan that is still in the store is
    // not one, so opening the settings starts on its first tab instead of carrying it there.
    ...(route === 'settings' && !isSettingsTab(s.objectTab) ? { objectTab: null } : {}),
  }));
}

/**
 * @param tab a tab id, or nothing
 * @returns whether it names a tab of the settings view
 */
function isSettingsTab(tab: string | null | undefined): boolean {
  return typeof tab === 'string' && (SETTINGS_TABS as ReadonlyArray<string>).includes(tab);
}

/**
 * The address the store holds: the route and its identifiers, and the query fields the address
 * carries for the object it shows.
 *
 * @param state the runtime state
 * @returns the address to write
 */
export function addressOfState(state: RuntimeState): Address {
  return {
    route: state.currentRoute,
    ...routeParamsOf(state),
    // Running work's scope is the lens's own query key, not a path id.
    ...(state.currentRoute === 'overview' && state.lensProject
      ? { project: state.lensProject }
      : {}),
    ...(state.lensMode ? { mode: state.lensMode } : {}),
    ...(state.lensGroup ? { group: state.lensGroup } : {}),
    ...(state.lensFollow ? { follow: true } : {}),
    ...(state.objectTab ? { tab: state.objectTab as Address['tab'] } : {}),
    ...(state.openObject ? { open: state.openObject as Address['open'] } : {}),
    ...(state.phaseNode ? { node: state.phaseNode } : {}),
    ...(state.changeFile ? { file: state.changeFile } : {}),
    ...(state.chatOpen ? { chat: true } : {}),
    ...(state.chatTurn ? { turn: state.chatTurn } : {}),
    // The start page's lens belongs to the start page's address alone.
    ...(state.currentRoute === 'inbox' && state.lens ? { lens: state.lens } : {}),
  };
}

/**
 * Moves to a route and writes the address it names.
 *
 * The writer decides whether the change pushes or replaces: a change of depth or of lens pushes, a
 * sibling in the same column replaces.
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
  const next = addressOfState(get(runtimeStore));
  writeAddress(next, intentFor(currentAddress(), next));
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
    if (window.history.length > 1) {
      window.history.back();
      return true;
    }
    return false;
  }
  applyRoute(previous.route, previous.params);
  // The current entry is replaced, so the browser's Back does not lead into a loop.
  writeAddress(addressOfState(get(runtimeStore)), 'replace');
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
 * Shows the designer for a project and keeps the address bar on it without a history entry.
 *
 * The local selector and a refused route change both converge on the project the designer
 * actually shows. Replacing rather than pushing means Back does not walk through refused route
 * changes.
 *
 * @param projectId the project the designer shows, or `null` for machine scope
 */
export function replaceDesignerProject(projectId: string | null): void {
  runtimeStore.update((s) => ({ ...s, designerProjectId: projectId }));
  writeAddress({ route: 'designer', projectId: projectId ?? undefined }, 'replace');
}

/**
 * Takes the route the address bar names, if it names one of the app's routes.
 *
 * The whole address is read: the selection, the lens's mode and grouping, the tab, the object
 * opened in place, the node, the file and the chat, so a reload and the browser's back return the
 * view exactly as it was.
 *
 * @returns the route taken, or `null` when the path names none
 */
export function adoptRouteFromLocation(): AppRoute | null {
  const match = matchPath(window.location.pathname);
  if (match) {
    const address = currentAddress();
    runtimeStore.update((s) => ({
      ...s,
      currentRoute: match.route,
      // The reports route keeps its plan in the query, so a reload or the browser's back
      // between two reports opens the one the address names.
      selectedReportDetailPlanId:
        match.route === 'reports'
          ? (new URLSearchParams(window.location.search).get('plan') ??
            address?.legacyReportPlanId ??
            null)
          : null,
      // One report's own address names the report; the view resolves its plan from the report.
      selectedReportId: match.reportId ?? null,
      // The decisions route focuses the decision its query or its path names, the same way.
      selectedDecisionId: match.decisionId ?? (match.route === 'decisions'
        ? new URLSearchParams(window.location.search).get('decision')
        : null),
      // The designer keeps the project it was opened for the same way.
      designerProjectId:
        match.route === 'designer'
          ? new URLSearchParams(window.location.search).get('project')
          : null,
      // The onboarding keeps the step the address names, so a later store update cannot drop it.
      onboardingStep: match.route === 'onboarding' ? requestedOnboardingStepFromUrl() : null,
      selectedPlanId: match.planId ?? s.selectedPlanId ?? null,
      selectedEpicId: match.epicId ?? s.selectedEpicId ?? null,
      selectedTaskId: match.taskId ?? null,
      // The one project page names its project; Running work's scope and the explorer's old
      // `?project=` name the same selection.
      selectedProjectId:
        match.projectId ??
        address?.project ??
        (match.route === 'projects'
          ? new URLSearchParams(window.location.search).get('project')
          : null),
      startProjectId: match.projectId ?? s.startProjectId ?? null,
      startEntryId: match.entryId ?? s.startEntryId ?? null,
      lensMode: address?.mode ?? null,
      lensGroup: address?.group ?? null,
      lensFollow: address?.follow ?? false,
      lensProject: address?.project ?? null,
      lens: address?.lens ?? null,
      objectTab: address?.tab ?? null,
      openObject: address?.open ?? null,
      phaseNode: address?.node ?? null,
      changeFile: address?.file ?? null,
      chatOpen: address?.chat ?? false,
      chatTurn: address?.turn ?? null,
    }));
    // The store adopted a route the browser's own back or forward named: a navigation that was
    // awaiting a read while this happened must see it, so a later return to the same address is
    // not mistaken for no movement at all.
    noteNavigationAdopted();
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

export async function stepOnboarding(
  link: string,
  body: Record<string, unknown> = {},
): Promise<void> {
  try {
    applyOnboardingState(await apiClient.stepOnboarding(link, body));
  } catch {
    // A refused step leaves the state as it is.
  }
}

/** A caught value as the reason a wrapper shows: its code's sentence without its own full stop. */
function errorMessage(err: unknown): string {
  return actFailureReason(err, '');
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

/**
 * The result of writing one machine setting, with the wire code of a refusal the store read.
 *
 * The client's own result type stays the wire shape; a refusal is the store's finding, because only
 * the store catches the throw and reads its code.
 */
export interface MachineSettingsWriteResult extends MachineSettingsUpdateResult {
  /** The wire code of a refusal the client read, when the runtime answered with one. */
  errorCode?: string;
}

export async function updateMachineSetting(
  key: string,
  value: string | null,
  revision?: number,
): Promise<MachineSettingsWriteResult> {
  // The caller may name the revision its edit was aimed at, so a refresh between the read and the
  // write cannot turn another client's change into an overwrite.
  const target = revision ?? get(runtimeStore).machineSettings?.revision ?? 0;
  try {
    const result = await apiClient.updateMachineSetting(key, value, target);
    await loadMachineSettings();
    return result;
  } catch (err: unknown) {
    // A refusal keeps its wire code, so the page can name the rule the value missed instead of
    // showing the generic failure. Nothing was applied.
    return { applied: false, conflict: false, revision: target, errorCode: errorCodeOf(err) };
  }
}

export async function withdrawConsent(
  kind: string,
  revision?: number,
): Promise<MachineSettingsUpdateResult | null> {
  const target = revision ?? get(runtimeStore).machineSettings?.revision ?? 0;
  try {
    const result = await apiClient.withdrawConsent(kind, target);
    await loadMachineSettings();
    return result;
  } catch {
    return null;
  }
}

export function openPlanView(planId: string, taskId?: string | null): void {
  runtimeStore.update((s) => ({ ...s, selectedPlanEnded: null }));
  // The plan the operator selected is the object the remembered place keeps.
  writeRememberedPlace({ ...readRememberedPlace(), planId, epicId: null });
  // A task is a level of its own and takes the path; a plan without a task keeps the plan path.
  if (taskId) {
    pushRoute('task', { planId, taskId });
    return;
  }
  pushRoute('plan', { planId });
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
  writeRememberedPlace({ ...readRememberedPlace(), epicId, planId: null });
  pushRoute('epic', { epicId });
}

/**
 * Selects a tab of the object column, and writes it into the address by a replace.
 *
 * A tab is not a level: switching it leaves the history where it is, so Back leaves the object
 * rather than walking through its tabs (`navigation.adoc#url`). The store follows the address, so
 * the tab survives a reload of the same object.
 *
 * @param tab the tab to show
 */
export function selectObjectTab(tab: ObjectTab): void {
  runtimeStore.update((s) => ({ ...s, objectTab: tab }));
  const current = currentAddress();
  if (!current) {
    return;
  }
  writeAddress(switchTab(current, tab), 'replace');
}

/**
 * Opens a plan's completion report in one step, on the reports route.
 *
 * @param planId the plan whose completion report to show
 */
export function openCompletionReport(planId: string): void {
  // The act knows the plan, not the report's id: the report's own address is written once the view
  // has resolved that id from the report record. The store keeps the plan across the push, which
  // a plain visit to the list clears.
  pushRoute('reports');
  runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: planId }));
}

/**
 * Opens the project explorer at one project, its detail one step away.
 *
 * @param projectId the project to show
 */
export function openProjectInExplorer(projectId: string): void {
  pushRoute('project', { projectId });
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
  writeAddress(
    { route: 'onboarding', onboardingStep: step },
    intentFor(currentAddress(), { route: 'onboarding', onboardingStep: step }),
  );
}

/**
 * @returns the step the address bar names, or `null` when it names none
 */
function requestedOnboardingStepFromUrl(): number | null {
  const requested = Number.parseInt(
    new URLSearchParams(window.location.search).get('step') ?? '',
    10,
  );
  return Number.isInteger(requested) ? requested : null;
}

/**
 * The step the address bar or the store asked the onboarding to open at.
 *
 * @param maxStep the last step the screen offers
 * @returns the requested step, or `null` when none was asked for
 */
export function requestedOnboardingStep(maxStep: number): number | null {
  const fromStore = get(runtimeStore).onboardingStep;
  const fromUrl = requestedOnboardingStepFromUrl();
  const requested = fromUrl ?? fromStore;
  if (typeof requested !== 'number' || !Number.isInteger(requested)) return null;
  return requested >= 1 && requested <= maxStep ? requested : null;
}
