import { writable } from 'svelte/store';
import {
  apiClient,
  type StatusResponse,
  type StateRepresentation,
  type HypermediaLink,
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
    | 'reports';
  currentState: StateRepresentation | null;
  isRevoked: boolean;
  revokedReason: string;
  revokedInfo?: RevokedInfo | null;
  isOffline: boolean;
  activeStep: number;
  unreadReportsCount: number;
  unreadReportsArrivalNotice?: string | null;
  quietReportNotice?: string | null;
  selectedReportDetailPlanId?: string | null;
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
  activeStep: 1,
  unreadReportsCount: 0,
  unreadReportsArrivalNotice: null,
  quietReportNotice: null,
  selectedReportDetailPlanId: null,
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
