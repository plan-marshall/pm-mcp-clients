import { writable, get } from 'svelte/store';
import {
  apiClient,
  type StatusResponse,
  type StateRepresentation,
  type HypermediaLink,
  type OnboardingState,
  type MachineSettingsRecord,
  type MachineSettingsUpdateResult,
} from '../api/client';
import { pathForRoute, routeForPath, type AppRoute } from '../routes';

export type { AppRoute };

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
      history.replaceState(null, '', window.location.pathname);
      try {
        await pairBrowser(extracted);
        return true;
      } catch {
        // Fall through
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
 * The route the app opens on for a paired browser: where the address bar points, the onboarding
 * while the machine is not onboarded, and the entry resolved to where the app left off.
 */
function landingRoute(requested: AppRoute | null, onboarded: boolean): AppRoute {
  if (requested === 'entry') {
    return onboarded ? 'workspace' : 'onboarding';
  }
  if (!onboarded) {
    return requested === 'pair' ? 'pair' : 'onboarding';
  }
  return requested ?? 'workspace';
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
  const requested = routeForPath(typeof window !== 'undefined' ? window.location.pathname : '/');

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

    // If there is a current state reported in status, store it
    const currentState: StateRepresentation = (status as unknown as StateRepresentation) || null;

    runtimeStore.update((s) => ({
      ...s,
      isInitialized: true,
      isPaired: true,
      isOnboarded: onboarded,
      deviceLabel: label,
      machineName,
      currentRoute: landingRoute(requested, onboarded),
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
        const timePart = page.oldest_unread_time ? ` since ${page.oldest_unread_time}` : '';
        arrivalNotice = `${page.unread_count} unread reports${timePart}`;
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
        arrivalNotice = `${decisions.length} open decision${decisions.length > 1 ? 's' : ''}`;
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
      quietReportNotice: reportData.subject || 'A new report has arrived',
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
    const notice = dec.title || dec.question || 'A new decision requires attention';
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
  } else if (event === 'drain.withdrawn') {
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
const LIFECYCLE_EVENTS = new Set(['runtime.draining', 'drain.withdrawn', 'runtime.stopped']);
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

  runtimeStore.update((s) => ({
    ...s,
    isInitialized: true,
    isPaired: true,
    isRevoked: false,
    deviceLabel: res.label,
    machineName,
    isOnboarded: onboarded,
    currentRoute: onboarded ? 'workspace' : 'onboarding',
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

export function navigateTo(route: AppRoute): void {
  runtimeStore.update((s) => ({
    ...s,
    currentRoute: route,
  }));
  const path = pathForRoute(route);
  if (path) {
    void syncUrl(path, false);
  }
}

/**
 * Puts a path in the address bar without reloading the view.
 *
 * SvelteKit's router is preferred, so its state stays in step; a bare browser without a running
 * router (the unit tests) falls back to the history API.
 *
 * @param path the path to show
 * @param replace `true` to replace the current entry instead of pushing one
 */
export async function syncUrl(path: string, replace: boolean): Promise<void> {
  if (typeof window === 'undefined') {
    return;
  }
  if (window.location.pathname === path && !replace) {
    return;
  }
  try {
    const { goto } = await import('$app/navigation');
    await goto(path, { replace, noScroll: true, keepFocus: true });
  } catch {
    if (window.location.pathname !== path) {
      const method = replace ? 'replaceState' : 'pushState';
      window.history[method]({ path }, '', path);
    }
  }
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
  const route = routeForPath(window.location.pathname);
  if (route) {
    runtimeStore.update((s) => ({ ...s, currentRoute: route }));
  }
  return route;
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
    return { ok: false, cause: (err as Error)?.message ?? '', kindFailed: false };
  }
  // The secret is stored; a kind change that fails does not undo it.
  if (kind) {
    const result = await apiClient.updateMachineSetting(`credential:${key}`, kind, revision);
    await loadMachineSettings();
    return { ok: true, cause: '', kindFailed: !result?.applied };
  }
  await loadMachineSettings();
  return { ok: true, cause: '', kindFailed: false };
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
  runtimeStore.update((s) => ({
    ...s,
    currentRoute: 'plan',
    selectedPlanId: planId,
    selectedTaskId: taskId || null,
  }));
}

export function openEpicView(epicId: string): void {
  runtimeStore.update((s) => ({
    ...s,
    currentRoute: 'epic',
    selectedEpicId: epicId,
  }));
}

export function openEpicDecomposition(epicId: string, projectId: string): void {
  runtimeStore.update((s) => ({
    ...s,
    currentRoute: 'decomposition',
    selectedEpicId: epicId,
    selectedProjectId: projectId,
  }));
}
