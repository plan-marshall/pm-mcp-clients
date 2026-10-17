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
  currentRoute:
    | 'pair'
    | 'onboarding'
    | 'workspace'
    | 'settings'
    | 'designer'
    | 'revoked'
    | 'overview'
    | 'reports'
    | 'decisions'
    | 'system'
    | 'cleanup'
    | 'projects'
    | 'plan'
    | 'epic'
    | 'decomposition'
    | 'machine-settings';
  currentState: StateRepresentation | null;
  onboarding?: OnboardingState | null;
  machineSettings?: MachineSettingsRecord | null;
  isRevoked: boolean;
  revokedReason: string;
  revokedInfo?: RevokedInfo | null;
  isOffline: boolean;
  isDraining: boolean;
  drainInfo?: { count?: number; what?: string; who?: string; since?: string } | null;
  isStopped: boolean;
  stoppedInfo?: { command?: string; who?: string; time?: string } | null;
  activeStep: number;
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
  isDraining: false,
  drainInfo: null,
  isStopped: false,
  stoppedInfo: null,
  activeStep: 1,
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

function deriveStepFromState(stateId?: string): number {
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
    const onboarded =
      status.onboarded ??
      (typeof status.machine === 'object' && status.machine ? status.machine.onboarded : false) ??
      false;
    const machineName =
      typeof status.machine === 'string' ? status.machine : status.machine?.name || 'local-machine';

    const initialRoute: RuntimeState['currentRoute'] = onboarded ? 'workspace' : 'onboarding';

    // If there is a current state reported in status, store it
    const currentState: StateRepresentation = (status as unknown as StateRepresentation) || null;
    const activeStep = deriveStepFromState(currentState?.state);

    runtimeStore.update((s) => ({
      ...s,
      isInitialized: true,
      isPaired: true,
      isOnboarded: onboarded,
      deviceLabel: label,
      machineName,
      currentRoute: initialRoute,
      currentState,
      activeStep,
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

export function handleRuntimeEvent(event: string, data: unknown): void {
  if (event === 'state_transition' || event === 'check') {
    const stateData = data as StateRepresentation;
    if (stateData?.state) {
      const activeStep = deriveStepFromState(stateData.state);
      runtimeStore.update((s) => ({
        ...s,
        currentState: stateData,
        activeStep,
        isOnboarded: stateData.state === 'onboard.completed' ? true : s.isOnboarded,
      }));
    }
  } else if (event === 'report.sent') {
    const reportData = (data || {}) as { subject?: string; plan?: string };
    const notice = reportData.subject || 'A new report has arrived';
    runtimeStore.update((s) => ({
      ...s,
      unreadReportsCount: s.unreadReportsCount + 1,
      quietReportNotice: notice,
    }));
  } else if (event === 'report.read') {
    runtimeStore.update((s) => ({
      ...s,
      unreadReportsCount: Math.max(0, s.unreadReportsCount - 1),
    }));
  } else if (event === 'decision.opened') {
    const dec = (data || {}) as { title?: string; question?: string; id?: string };
    const notice = dec.title || dec.question || 'A new decision requires attention';
    runtimeStore.update((s) => ({
      ...s,
      openDecisionsCount: s.openDecisionsCount + 1,
      newDecisionArrivalNotice: notice,
      quietDecisionNotice: notice,
    }));
  } else if (event === 'decision.answered' || event === 'decision.closed') {
    runtimeStore.update((s) => ({
      ...s,
      openDecisionsCount: Math.max(0, s.openDecisionsCount - 1),
    }));
  } else if (event === 'runtime.draining') {
    const drainData = (data || {}) as {
      count?: number;
      what?: string;
      who?: string;
      since?: string;
    };
    runtimeStore.update((s) => ({
      ...s,
      isDraining: true,
      drainInfo: drainData,
    }));
  } else if (event === 'drain.withdrawn') {
    runtimeStore.update((s) => ({
      ...s,
      isDraining: false,
      drainInfo: null,
    }));
  } else if (event === 'runtime.stopped') {
    const stoppedData = (data || {}) as { command?: string; who?: string; time?: string };
    runtimeStore.update((s) => ({
      ...s,
      isDraining: false,
      isStopped: true,
      stoppedInfo: stoppedData,
    }));
  } else if (event === 'project.changed' || event === 'project.released' || event === 'project.enrolled') {
    runtimeStore.update((s) => ({
      ...s,
      projectEventNotice: `${event}_${Date.now()}`,
    }));
  } else if (event === 'onboarding.step_completed') {
    void loadOnboardingState();
  } else if (event === 'machine.settings_changed') {
    void loadMachineSettings();
  }
}

export async function pairBrowser(code: string): Promise<void> {
  const res = await apiClient.pair(code);
  const status = await apiClient.getStatus().catch(() => ({ status: 'ready', onboarded: false }));
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

export function navigateTo(route: RuntimeState['currentRoute']): void {
  runtimeStore.update((s) => ({
    ...s,
    currentRoute: route,
  }));
}

function applyOnboardingState(state: OnboardingState): void {
  const activeStep = deriveStepFromState(state.state);
  runtimeStore.update((s) => ({
    ...s,
    onboarding: state,
    currentState: state as unknown as StateRepresentation,
    activeStep,
    isOnboarded: state.state === 'onboard.completed' ? true : s.isOnboarded,
  }));
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
