import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { get } from 'svelte/store';
import { apiClient, type ReportsPage, type StatusResponse } from '../api/client';
import { actFailureReason, ClientError } from '../api/errors';
import {
  adoptRouteFromLocation,
  checkUrlHashForCode,
  deriveStepFromState,
  handleRuntimeEvent,
  initRuntime,
  isRuntimeRecovering,
  landingRoute,
  navigateTo,
  openEpicDecomposition,
  openStartPage,
  pairBrowser,
  pushRoute,
  RECONNECT_INTERVAL_MS,
  replaceDesignerProject,
  retryEventStream,
  retryRuntime,
  routeParamsOf,
  runtimeStore,
  startOnboardingAuto,
  stopRuntimeRecovery,
  storeCredential,
  updateMachineSetting,
  withdrawConsent,
  type AppRoute,
  type RevokedInfo,
  type RouteParams,
} from './runtime';
import { makeRuntimeState } from '../test/runtimeState';

const PAIRED_STATUS: StatusResponse = {
  status: 'ready',
  onboarded: true,
  machine: { name: 'mbp-oliver', onboarded: true },
};

const EMPTY_REPORTS: ReportsPage = {
  items: [],
  unread_count: 0,
  has_older: false,
  older_count: 0,
};

/**
 * The pairing lifecycle and the connection state: a paired browser never redeems a code again,
 * and a recovered connection clears the offline state.
 */
describe('the runtime store', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    window.history.replaceState(null, '', '/pair');
    runtimeStore.set(makeRuntimeState());
  });

  afterEach(() => {
    // A failed start leaves the store retrying; no retry outlives its test.
    stopRuntimeRecovery();
    vi.useRealTimers();
  });

  it('does not redeem from an already paired browser', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_still_valid');
    window.history.replaceState(null, '', '/pair#code=KXQB-MRTN');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue(PAIRED_STATUS);
    const pair = vi.spyOn(apiClient, 'pair');

    // Act
    const redeemed = await checkUrlHashForCode();

    // Assert: the probe keeps the pairing and drops the single-use code without consuming it.
    expect(redeemed).toBe(false);
    expect(pair).not.toHaveBeenCalled();
    expect(window.location.hash).toBe('');
  });

  it('redeems the code when the stored secret is no longer accepted', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_revoked');
    window.history.replaceState(null, '', '/pair#code=KXQB-MRTN');
    vi.spyOn(apiClient, 'getStatus')
      .mockRejectedValueOnce(new Error('unauthorized'))
      .mockResolvedValue(PAIRED_STATUS);
    const pair = vi.spyOn(apiClient, 'pair').mockResolvedValue({
      device_secret: 'pmw_fresh',
      device_id: 'dev-2',
      label: 'Firefox',
    });
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });

    // Act
    const redeemed = await checkUrlHashForCode();

    // Assert
    expect(redeemed).toBe(true);
    expect(pair).toHaveBeenCalledWith('KXQB-MRTN');
  });

  it('keeps the fragment when the machine cannot be reached', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    window.history.replaceState(null, '', '/pair#code=KXQB-MRTN');
    vi.spyOn(apiClient, 'getStatus').mockRejectedValue(new Error('network_offline'));
    const pair = vi.spyOn(apiClient, 'pair');

    // Act
    const redeemed = await checkUrlHashForCode();

    // Assert: nothing is consumed, and the fragment is kept for a later retry.
    expect(redeemed).toBe(false);
    expect(pair).not.toHaveBeenCalled();
    expect(window.location.hash).toBe('#code=KXQB-MRTN');
    expect(get(runtimeStore).isOffline).toBe(true);
  });

  it('keeps the addressed route and the stored pairing when the status read fails', async () => {
    // Arrange: the browser is paired and an address names a plan, but the machine is unreachable.
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    window.history.replaceState(null, '', '/plans/fix-relay');
    vi.spyOn(apiClient, 'getStatus').mockRejectedValue(new ClientError('network_offline'));

    // Act
    await initRuntime();

    // Assert: the view the address names is the one that draws the failed load, not the pair page.
    const state = get(runtimeStore);
    expect(state.isInitialized).toBe(true);
    expect(state.isPaired).toBe(true);
    expect(state.isOffline).toBe(true);
    expect(state.currentRoute).toBe('plan');
    expect(state.selectedPlanId).toBe('fix-relay');
  });

  it('keeps the previous route when a failed status read addresses the pairing page', async () => {
    // Arrange: the address names the pairing page, which is not a view to land on.
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    window.history.replaceState(null, '', '/pair');
    runtimeStore.set(makeRuntimeState({ currentRoute: 'overview' }));
    vi.spyOn(apiClient, 'getStatus').mockRejectedValue(new ClientError('network_offline'));

    // Act
    await initRuntime();

    // Assert
    expect(get(runtimeStore).currentRoute).toBe('overview');
    expect(get(runtimeStore).isPaired).toBe(true);
  });

  it('clears the offline state when the connection returns', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    runtimeStore.set(makeRuntimeState({ isOffline: true }));
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue(PAIRED_STATUS);
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getReports').mockResolvedValue(EMPTY_REPORTS);
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);

    // Act
    await initRuntime();

    // Assert
    expect(get(runtimeStore).isOffline).toBe(false);
  });

  it('shows the draining lifecycle the status reports', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...PAIRED_STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_count: 2,
    });
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getReports').mockResolvedValue(EMPTY_REPORTS);
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);

    // Act
    await initRuntime();

    // Assert
    expect(get(runtimeStore).isDraining).toBe(true);
    expect(get(runtimeStore).drainInfo?.who).toBe('the console');
    expect(get(runtimeStore).drainInfo?.count).toBe(2);
  });

  it('applies the draining lifecycle fields when pairing', async () => {
    // Arrange
    vi.spyOn(apiClient, 'pair').mockResolvedValue({
      device_secret: 'pmw_fresh',
      device_id: 'dev-2',
      label: 'Firefox',
    });
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...PAIRED_STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_count: 2,
    });
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getReports').mockResolvedValue(EMPTY_REPORTS);
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);

    // Act
    await pairBrowser('KXQB-MRTN');

    // Assert: lifecycle fields are preserved in the store upon pairing
    expect(get(runtimeStore).isDraining).toBe(true);
    expect(get(runtimeStore).drainInfo).toEqual({
      who: 'the console',
      count: 2,
      items: undefined,
    });
  });

  it('preserves the drain count when status reports empty items array beside a positive count', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...PAIRED_STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_count: 3,
      drain_remaining_items: [],
    });
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getReports').mockResolvedValue(EMPTY_REPORTS);
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);

    // Act
    await initRuntime();

    // Assert: count is preserved from drain_remaining_count rather than collapsed to empty list length
    expect(get(runtimeStore).isDraining).toBe(true);
    expect(get(runtimeStore).drainInfo?.who).toBe('the console');
    expect(get(runtimeStore).drainInfo?.count).toBe(3);
  });

  it('shows the stopped lifecycle the status reports', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    runtimeStore.set(
      makeRuntimeState({ isDraining: true, drainInfo: { who: 'the console', count: 2 } }),
    );
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...PAIRED_STATUS,
      lifecycle_state: 'stopped',
    });
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getReports').mockResolvedValue(EMPTY_REPORTS);
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);

    // Act
    await initRuntime();

    // Assert
    expect(get(runtimeStore).isStopped).toBe(true);
    expect(get(runtimeStore).isDraining).toBe(false);
  });

  it('clears the stopped flag when the status reports running', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    runtimeStore.set(makeRuntimeState({ isStopped: true }));
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...PAIRED_STATUS,
      lifecycle_state: 'running',
    });
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getReports').mockResolvedValue(EMPTY_REPORTS);
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);

    // Act
    await initRuntime();

    // Assert
    expect(get(runtimeStore).isStopped).toBe(false);
  });

  it('clears the stopped flag when a drain event arrives', () => {
    // Arrange
    runtimeStore.set(makeRuntimeState({ isStopped: true }));

    // Act
    handleRuntimeEvent('runtime.draining', { draining_by: 'the console', remaining_work_count: 2 });

    // Assert
    expect(get(runtimeStore).isStopped).toBe(false);
    expect(get(runtimeStore).isDraining).toBe(true);
    expect(get(runtimeStore).drainInfo).toEqual({ who: 'the console', count: 2 });
  });

  it('marks the runtime stopped when a stop that exits arrives', () => {
    // Arrange
    runtimeStore.set(
      makeRuntimeState({ isDraining: true, drainInfo: { who: 'the console', count: 1 } }),
    );

    // Act
    handleRuntimeEvent('runtime.stopping', { lifecycle_state: 'stopped', then: 'exit' });

    // Assert
    expect(get(runtimeStore)).toMatchObject({ isDraining: false, isStopped: true });
  });

  it.each([
    ['a drain that ended in a restart', { lifecycle_state: 'running', then: 'restart' }],
    ['a restart now', { lifecycle_state: 'stopping', then: 'restart' }],
  ])('never shows the stopped page for %s', (_case, payload) => {
    // Arrange
    runtimeStore.set(
      makeRuntimeState({ isDraining: true, drainInfo: { who: 'the console', count: 1 } }),
    );

    // Act
    handleRuntimeEvent('runtime.stopping', payload);

    // Assert
    expect(get(runtimeStore)).toMatchObject({
      isDraining: false,
      drainInfo: null,
      isStopped: false,
    });
  });

  it.each([
    ['a drain', 'runtime.draining', { who: 'the console', count: 1 }],
    ['a restart', 'runtime.stopping', { lifecycle_state: 'stopping', then: 'restart' }],
  ])('forgets who stopped the runtime once %s shows it running again', (_case, event, payload) => {
    // Arrange
    runtimeStore.set(
      makeRuntimeState({ isStopped: true, stoppedInfo: { who: 'by the console', time: 'noon' } }),
    );

    // Act
    handleRuntimeEvent(event, payload);

    // Assert
    expect(get(runtimeStore)).toMatchObject({ isStopped: false, stoppedInfo: null });
  });

  it('reads the who and remaining_count draining fields', () => {
    // Arrange
    runtimeStore.set(makeRuntimeState({ isStopped: true }));

    // Act
    handleRuntimeEvent('runtime.draining', { who: 'the console', remaining_count: 2 });

    // Assert
    expect(get(runtimeStore).drainInfo).toEqual({ who: 'the console', count: 2 });
  });

  it('clears the drain info when the status is not draining', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    runtimeStore.set(
      makeRuntimeState({ isDraining: true, drainInfo: { who: 'the console', count: 2 } }),
    );
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...PAIRED_STATUS,
      lifecycle_state: 'running',
    });
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getReports').mockResolvedValue(EMPTY_REPORTS);
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);

    // Act
    await initRuntime();

    // Assert
    expect(get(runtimeStore).isDraining).toBe(false);
    expect(get(runtimeStore).drainInfo).toBeNull();
  });

  it('does not open a start page while the browser is unpaired', async () => {
    // Arrange
    const projects = vi.spyOn(apiClient, 'getProjects');
    runtimeStore.set(makeRuntimeState({ isPaired: false, currentRoute: 'pair' }));

    // Act
    await openStartPage();

    // Assert
    expect(projects).not.toHaveBeenCalled();
    expect(get(runtimeStore).currentRoute).toBe('pair');
  });

  it('leaves the address bar alone when the start page is already shown', async () => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    window.history.replaceState(null, '', '/projects');
    runtimeStore.set(makeRuntimeState({ currentRoute: 'projects', isPaired: true }));
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });

    // Act
    await openStartPage();

    // Assert: the target is the current route, so no duplicate history entry is pushed.
    expect(window.location.pathname).toBe('/projects');
    expect(get(runtimeStore).currentRoute).toBe('projects');
  });

  it('replaces the designer project in the store and the address bar', () => {
    // Arrange
    window.history.replaceState(null, '', '/projects');

    // Act
    replaceDesignerProject('pm-mcp-clients');

    // Assert: the query names the project, and the entry is replaced rather than pushed.
    expect(get(runtimeStore).designerProjectId).toBe('pm-mcp-clients');
    expect(window.location.pathname + window.location.search).toBe(
      '/designer?project=pm-mcp-clients',
    );
    expect(window.history).toHaveLength(1);
  });

  it('writes a plain designer path for machine scope', () => {
    // Arrange
    window.history.replaceState(null, '', '/designer?project=pm-mcp-clients');

    // Act
    replaceDesignerProject(null);

    // Assert
    expect(get(runtimeStore).designerProjectId).toBeNull();
    expect(window.location.pathname + window.location.search).toBe('/designer');
  });

  it('adopts the decision the address names', () => {
    // Arrange: the palette opens a decision through this route, so a reload or a shared link must
    // land on the same decision.
    window.history.replaceState(null, '', '/decisions?decision=dec-101');
    runtimeStore.set(makeRuntimeState({ currentRoute: 'overview', selectedDecisionId: null }));

    // Act
    const route = adoptRouteFromLocation();

    // Assert
    expect(route).toBe('decisions');
    expect(get(runtimeStore).selectedDecisionId).toBe('dec-101');
  });

  it('keeps the onboarding step the address names in the route params', () => {
    // Arrange: the view consumes the store's step, but the address keeps the aspect.
    window.history.replaceState(null, '', '/onboarding?step=6');
    runtimeStore.set(makeRuntimeState({ currentRoute: 'onboarding', onboardingStep: null }));

    // Act
    const params = routeParamsOf(get(runtimeStore));

    // Assert
    expect(params.onboardingStep).toBe(6);
  });

  it('lands by the rule alone when localStorage holds a stale start route', async () => {
    // Arrange: a stale start route in localStorage must be ignored.
    const staleStartRouteKey = ['pm', 'start', 'route'].join('_');
    localStorage.setItem(staleStartRouteKey, 'projects');

    // Act & Assert 1: landingRoute lands on overview when work is running, despite stale start route
    expect(landingRoute(null, true, true)).toBe('overview');

    // Act & Assert 2: fresh paired browser lands on overview by the rule
    vi.spyOn(apiClient, 'pair').mockResolvedValue({
      device_secret: 'pmw_fresh',
      device_id: 'dev-1',
      label: 'Chrome',
    });
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue(PAIRED_STATUS);
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({
      projects: [
        {
          project_id: 'p-1',
          display_name: 'Project 1',
          active_plans_count: 1,
          active_epics_count: 0,
          needs_operator_count: 0,
          setup_needs_operator: false,
        },
      ],
    });
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getReports').mockResolvedValue(EMPTY_REPORTS);
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);

    await pairBrowser('KXQB-MRTN');
    expect(get(runtimeStore).currentRoute).toBe('overview');

    // Act & Assert 3: brand click lands on projects when no work runs, despite stale start route
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    localStorage.setItem(staleStartRouteKey, 'overview');
    runtimeStore.set(makeRuntimeState({ currentRoute: 'overview', isPaired: true, isOnboarded: true }));
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });

    await openStartPage();
    expect(get(runtimeStore).currentRoute).toBe('projects');
  });
});

/**
 * The recovery after a failed start: the store owns the retry of the initialization, not the page
 * of the lost connection, so a request that clears the offline state before the first retry does
 * not leave a runtime whose state was never loaded. A retry that lands late never takes the person
 * back from a route they moved to while it waited.
 */
describe('the recovery of a failed start', () => {
  /**
   * A deferred answer to the status read, so the test can act while the initialization waits.
   *
   * @returns the pending answer with the hands that settle it
   */
  function deferredStatus(): {
    promise: Promise<StatusResponse>;
    resolve: (status: StatusResponse) => void;
    reject: (err: Error) => void;
  } {
    let resolve!: (status: StatusResponse) => void;
    let reject!: (err: Error) => void;
    const promise = new Promise<StatusResponse>((res, rej) => {
      resolve = res;
      reject = rej;
    });
    return { promise, resolve, reject };
  }

  /**
   * Stubs the reads a successful initialization makes after the status.
   *
   * @returns the spy on the event stream's subscription, which only a successful start opens
   */
  function stubLoadedRuntime() {
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });
    vi.spyOn(apiClient, 'getReports').mockResolvedValue(EMPTY_REPORTS);
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);
    return vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  }

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.restoreAllMocks();
    window.history.replaceState(null, '', '/plans/fix-relay');
    runtimeStore.set(makeRuntimeState({ isInitialized: false, isOnboarded: false }));
  });

  afterEach(() => {
    stopRuntimeRecovery();
    vi.useRealTimers();
  });

  it('completes the initialization although a probe cleared the offline state before the first retry', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const status = vi
      .spyOn(apiClient, 'getStatus')
      .mockRejectedValueOnce(new ClientError('network_offline'))
      .mockResolvedValue(PAIRED_STATUS);
    const subscribe = stubLoadedRuntime();

    await initRuntime();
    expect(get(runtimeStore).isOffline).toBe(true);
    expect(isRuntimeRecovering()).toBe(true);

    // The connection probe reaches the runtime first and clears the offline state, as any
    // successful request does; the page of the lost connection and its timer go with it.
    runtimeStore.update((s) => ({ ...s, isOffline: false }));
    await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS);

    await vi.waitFor(() => expect(subscribe).toHaveBeenCalledTimes(1));
    expect(status).toHaveBeenCalledTimes(2);
    expect(get(runtimeStore).isOnboarded).toBe(true);
    expect(get(runtimeStore).machineName).toBe('mbp-oliver');
    expect(isRuntimeRecovering()).toBe(false);
  });

  it('stops retrying after the first successful initialization', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const status = vi
      .spyOn(apiClient, 'getStatus')
      .mockRejectedValueOnce(new ClientError('network_offline'))
      .mockResolvedValue(PAIRED_STATUS);
    const subscribe = stubLoadedRuntime();

    await initRuntime();
    await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS);
    await vi.waitFor(() => expect(subscribe).toHaveBeenCalledTimes(1));
    await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS * 3);

    expect(status).toHaveBeenCalledTimes(2);
    expect(isRuntimeRecovering()).toBe(false);
  });

  it('keeps retrying while the runtime stays unreachable', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const status = vi
      .spyOn(apiClient, 'getStatus')
      .mockRejectedValue(new ClientError('network_offline'));

    await initRuntime();
    await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS * 2);

    await vi.waitFor(() => expect(status).toHaveBeenCalledTimes(3));
    expect(isRuntimeRecovering()).toBe(true);
  });

  it('stops retrying on teardown', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const status = vi
      .spyOn(apiClient, 'getStatus')
      .mockRejectedValue(new ClientError('network_offline'));

    await initRuntime();
    stopRuntimeRecovery();
    await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS * 2);

    expect(status).toHaveBeenCalledTimes(1);
    expect(isRuntimeRecovering()).toBe(false);
  });

  it('starts no retry after a successful start', async () => {
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue(PAIRED_STATUS);
    stubLoadedRuntime();

    await initRuntime();

    expect(isRuntimeRecovering()).toBe(false);
  });

  it('opens the route its start read when the person stayed', async () => {
    runtimeStore.set(makeRuntimeState({ isInitialized: true, currentRoute: 'plan' }));
    const answer = deferredStatus();
    vi.spyOn(apiClient, 'getStatus').mockReturnValue(answer.promise);
    stubLoadedRuntime();

    const pending = initRuntime();
    answer.resolve(PAIRED_STATUS);
    await pending;

    expect(get(runtimeStore).currentRoute).toBe('plan');
    expect(get(runtimeStore).selectedPlanId).toBe('fix-relay');
  });

  it('keeps a navigation made while a successful initialization awaited', async () => {
    runtimeStore.set(makeRuntimeState({ isInitialized: true, currentRoute: 'plan' }));
    const answer = deferredStatus();
    const status = vi.spyOn(apiClient, 'getStatus').mockReturnValue(answer.promise);
    stubLoadedRuntime();

    const pending = initRuntime();
    // The start has read the address and asked for the status; the person moves before the answer.
    await vi.waitFor(() => expect(status).toHaveBeenCalledTimes(1));
    navigateTo('reports');
    answer.resolve(PAIRED_STATUS);
    await pending;

    expect(get(runtimeStore).currentRoute).toBe('reports');
    expect(window.location.pathname).toBe('/reports');
    expect(get(runtimeStore).isOnboarded).toBe(true);
  });

  it('keeps a navigation made while a failed initialization awaited', async () => {
    runtimeStore.set(makeRuntimeState({ isInitialized: true, currentRoute: 'plan' }));
    const answer = deferredStatus();
    const status = vi.spyOn(apiClient, 'getStatus').mockReturnValue(answer.promise);

    const pending = initRuntime();
    // The start has read the address and asked for the status; the person moves before the answer.
    await vi.waitFor(() => expect(status).toHaveBeenCalledTimes(1));
    navigateTo('reports');
    answer.reject(new ClientError('network_offline'));
    await pending;

    expect(get(runtimeStore).currentRoute).toBe('reports');
    expect(get(runtimeStore).isOffline).toBe(true);
  });

  it('runs one retry at a time', async () => {
    const answer = deferredStatus();
    const status = vi.spyOn(apiClient, 'getStatus').mockReturnValue(answer.promise);
    stubLoadedRuntime();

    const first = retryRuntime();
    await vi.waitFor(() => expect(status).toHaveBeenCalledTimes(1));
    // The page of the unreachable runtime retries while the store's own retry still waits.
    const second = retryRuntime();
    answer.resolve(PAIRED_STATUS);
    await Promise.all([first, second]);

    expect(status).toHaveBeenCalledTimes(1);
  });

  it('retries again once the retry in flight has finished', async () => {
    const status = vi
      .spyOn(apiClient, 'getStatus')
      .mockRejectedValueOnce(new ClientError('network_offline'))
      .mockResolvedValue(PAIRED_STATUS);
    stubLoadedRuntime();

    await retryRuntime();
    await retryRuntime();

    expect(status).toHaveBeenCalledTimes(2);
  });

  it('stops retrying when the runtime refuses the stored pairing', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const status = vi
      .spyOn(apiClient, 'getStatus')
      .mockRejectedValueOnce(new ClientError('network_offline'))
      .mockRejectedValue(new Error('unauthorized'));
    await initRuntime();
    expect(isRuntimeRecovering()).toBe(true);

    await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS);
    await vi.waitFor(() => expect(status).toHaveBeenCalledTimes(2));

    await vi.waitFor(() => expect(isRuntimeRecovering()).toBe(false));
    await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS * 2);
    expect(status).toHaveBeenCalledTimes(2);
  });

  it('stops retrying when the browser no longer holds a pairing', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const status = vi
      .spyOn(apiClient, 'getStatus')
      .mockRejectedValue(new ClientError('network_offline'));
    await initRuntime();
    expect(isRuntimeRecovering()).toBe(true);

    vi.spyOn(apiClient, 'isPaired').mockReturnValue(false);
    await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS);

    await vi.waitFor(() => expect(get(runtimeStore).currentRoute).toBe('pair'));
    expect(isRuntimeRecovering()).toBe(false);
    expect(status).toHaveBeenCalledTimes(1);
  });

  it('starts no retry for a start that fails after the teardown', async () => {
    const answer = deferredStatus();
    const status = vi.spyOn(apiClient, 'getStatus').mockReturnValue(answer.promise);

    const pending = initRuntime();
    await vi.waitFor(() => expect(status).toHaveBeenCalledTimes(1));
    // The shell is torn down while the start still waits for its answer.
    stopRuntimeRecovery();
    answer.reject(new ClientError('network_offline'));
    await pending;

    expect(get(runtimeStore).isOffline).toBe(true);
    expect(isRuntimeRecovering()).toBe(false);
  });

  it('starts the retry again for a start that began after the teardown', async () => {
    vi.spyOn(apiClient, 'getStatus').mockRejectedValue(new ClientError('network_offline'));
    stopRuntimeRecovery();

    await initRuntime();

    expect(isRuntimeRecovering()).toBe(true);
  });
});

/** The events of the runtime stream the store follows itself. */
describe('the runtime events the store follows', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    runtimeStore.set(makeRuntimeState());
  });

  it('marks the network listener closed and open again as listener.changed names it', () => {
    // Act: ListenerChangedEvent {listener, state, port}; this browser is on the loopback listener
    handleRuntimeEvent('listener.changed', { listener: 'lan', state: 'closed', port: 7421 });

    // Assert: the banner changes, the loopback browser stays where it is
    expect(get(runtimeStore)).toMatchObject({ isListenerClosed: true, currentRoute: 'overview' });
    expect(get(runtimeStore).listenerClosedAt).not.toBeNull();

    // Act
    handleRuntimeEvent('listener.changed', { listener: 'lan', state: 'open', port: 7421 });

    // Assert
    expect(get(runtimeStore)).toMatchObject({ isListenerClosed: false, listenerClosedAt: null });
  });

  it('ignores a change of the loopback listener', () => {
    // Act
    handleRuntimeEvent('listener.changed', { listener: 'loopback', state: 'closed', port: 7420 });

    // Assert
    expect(get(runtimeStore).isListenerClosed).toBe(false);
  });

  it.each(['project.changed', 'project.released', 'project.enrolled'])(
    'tells the project views that %s happened',
    (event) => {
      // Act
      handleRuntimeEvent(event, { project_id: 'pm-mcp-clients' });

      // Assert
      expect(get(runtimeStore).projectEventNotice).toMatch(new RegExp(`^${event}_\\d+$`));
    },
  );

  it.each([
    ['onboarding.step_completed', 'getOnboardingState'],
    ['machine.settings_changed', 'getMachineSettings'],
  ] as const)('reads again what %s changed', async (event, read) => {
    // Arrange
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    const reread = vi.spyOn(apiClient, read).mockRejectedValue(new Error('not needed'));

    // Act
    handleRuntimeEvent(event, {});

    // Assert
    await vi.waitFor(() => expect(reread).toHaveBeenCalledTimes(1));
  });
});

/** The pairing, the revocation and the stream's Try again, as the shell sees them in the store. */
describe('the pairing state the store keeps', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    window.history.replaceState(null, '', '/pair');
    runtimeStore.set(makeRuntimeState());
  });

  afterEach(() => {
    stopRuntimeRecovery();
  });

  it('keeps the code and shows the refusal when the runtime refuses it', async () => {
    // Arrange: an unpaired browser opens a pairing link whose code the runtime refuses
    window.history.replaceState(null, '', '/pair#code=KXQB-MRTN');
    vi.spyOn(apiClient, 'pair').mockRejectedValue(new Error('invalid_code'));

    // Act
    const redeemed = await checkUrlHashForCode();

    // Assert: the pairing page names the refusal, and the code stays for the person to read
    expect(redeemed).toBe(false);
    expect(get(runtimeStore).pairingFailed).toBe(true);
    expect(window.location.hash).toBe('#code=KXQB-MRTN');
  });

  it.each([
    ['a reason', 'unauthorized', { reason: 'unauthorized' }],
    [
      'the revocation record',
      { reason: 'revoked_by_operator', revoked_by: 'the console' },
      { reason: 'revoked_by_operator', revoked_by: 'the console' },
    ],
  ] as const)(
    'shows the revoked page when the client reports %s',
    async (_kind, info, expected) => {
      // Arrange: the store hands the client its revocation hook when it starts
      const hook = vi.spyOn(apiClient, 'setOnRevoked');
      await initRuntime();
      const onRevoked = hook.mock.calls[0][0] as unknown as (info: string | RevokedInfo) => void;

      // Act
      onRevoked(info);

      // Assert
      expect(get(runtimeStore)).toMatchObject({
        isPaired: false,
        isRevoked: true,
        revokedReason: expected.reason,
        revokedInfo: expected,
        currentRoute: 'revoked',
      });
    },
  );

  it('reconnects the stream at once and reads the runtime again on Try again', async () => {
    // Arrange
    runtimeStore.set(makeRuntimeState({ streamState: 'offline' }));
    const retry = vi.spyOn(apiClient, 'retryEventStream').mockImplementation(() => {});
    const status = vi.spyOn(apiClient, 'getStatus');
    const shown: string[] = [];
    const unsubscribe = runtimeStore.subscribe((s) => shown.push(s.streamState));

    // Act
    retryEventStream();
    unsubscribe();

    // Assert: the indicator says it connects, the stream is retried, and the start runs again
    expect(shown.slice(0, 2)).toEqual(['offline', 'connecting']);
    expect(retry).toHaveBeenCalledTimes(1);
    await vi.waitFor(() => expect(get(runtimeStore).currentRoute).toBe('pair'));
    expect(status).not.toHaveBeenCalled();
  });
});

/** The onboarding's step bar follows the state the runtime reports. */
describe('the onboarding step of a state', () => {
  it.each([
    [undefined, 1],
    ['onboard.mode', 1],
    ['onboard.entry', 1],
    ['language.choose', 1],
    ['harness.choose', 2],
    ['models.choose', 3],
    ['catalogue.review', 3],
    ['roles.assign', 4],
    ['store.choose', 5],
    ['providers.verify', 6],
    ['defaults.review', 7],
    ['plans.defaults', 7],
    ['onboard.completed', 8],
    ['onboard.summary', 1],
  ] as [string | undefined, number][])('places %s on step %i', (state, step) => {
    expect(deriveStepFromState(state)).toBe(step);
  });

  it('applies the state the automatic run reports, and keeps it when the run is refused', async () => {
    // Arrange
    runtimeStore.set(makeRuntimeState());
    const auto = vi
      .spyOn(apiClient, 'startOnboardingAuto')
      .mockResolvedValueOnce({ state: 'roles.assign' } as never)
      .mockRejectedValueOnce(new Error('act_not_available'));

    // Act
    await startOnboardingAuto();

    // Assert
    expect(get(runtimeStore).activeStep).toBe(4);
    expect(get(runtimeStore).completedSteps).toBe(3);

    // Act: a refused run changes nothing
    await startOnboardingAuto();

    // Assert
    expect(auto).toHaveBeenCalledTimes(2);
    expect(get(runtimeStore).activeStep).toBe(4);
  });
});

/** The route a view opens, and the identifiers the store keeps for it. */
describe('the identifiers a route carries', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState(null, '', '/overview');
    runtimeStore.set(
      makeRuntimeState({
        startProjectId: 'shown-project',
        selectedPlanId: 'shown-plan',
        selectedEpicId: 'shown-epic',
      }),
    );
  });

  it.each([
    ['start', { projectId: 'p-1', entryId: 'e-1' }, { startProjectId: 'p-1', startEntryId: 'e-1' }],
    [
      'plan',
      { planId: 'fix', taskId: 'TASK-3' },
      { selectedPlanId: 'fix', selectedTaskId: 'TASK-3' },
    ],
    ['epic', { epicId: 'relay' }, { selectedEpicId: 'relay' }],
    ['decomposition', { epicId: 'relay' }, { selectedEpicId: 'relay' }],
    ['reports', { reportPlanId: 'fix' }, { selectedReportDetailPlanId: 'fix' }],
    ['decisions', { decisionId: 'Q-7' }, { selectedDecisionId: 'Q-7' }],
    ['projects', { explorerProjectId: 'p-1' }, { selectedProjectId: 'p-1' }],
    ['designer', { projectId: 'p-1' }, { designerProjectId: 'p-1' }],
  ] as [AppRoute, RouteParams, Record<string, unknown>][])(
    'opens %s with the identifiers it names',
    (route, params, expected) => {
      // Act
      pushRoute(route, params);

      // Assert
      expect(get(runtimeStore)).toMatchObject({ currentRoute: route, ...expected });
    },
  );

  it.each([
    ['start', { startProjectId: 'shown-project', startEntryId: null }],
    ['plan', { selectedPlanId: 'shown-plan', selectedTaskId: null }],
    ['epic', { selectedEpicId: 'shown-epic' }],
    ['reports', { selectedReportDetailPlanId: null }],
    ['decisions', { selectedDecisionId: null }],
    ['projects', { selectedProjectId: null }],
    ['designer', { designerProjectId: null }],
  ] as [AppRoute, Record<string, unknown>][])(
    'opens %s without identifiers on the one shown, or on none',
    (route, expected) => {
      // Act
      pushRoute(route);

      // Assert
      expect(get(runtimeStore)).toMatchObject({ currentRoute: route, ...expected });
    },
  );

  it('opens an epic decomposition under the project it belongs to', () => {
    // Act
    openEpicDecomposition('relay', 'pm-mcp-clients');

    // Assert
    expect(get(runtimeStore)).toMatchObject({
      currentRoute: 'decomposition',
      selectedEpicId: 'relay',
      selectedProjectId: 'pm-mcp-clients',
    });
  });
});

/** The machine settings a screen changes, against the revision the store read. */
describe('the machine settings the store writes', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.restoreAllMocks();
    runtimeStore.set(makeRuntimeState({ machineSettings: { revision: 4 } as never }));
  });

  it('reports a token the runtime refused, and reads nothing back', async () => {
    // Arrange
    const refusal = new Error('cli_only');
    vi.spyOn(apiClient, 'setCredential').mockRejectedValue(refusal);
    const reread = vi.spyOn(apiClient, 'getMachineSettings');

    // Act
    const result = await storeCredential('github', 'ghp_new');

    // Assert
    expect(result).toEqual({
      ok: false,
      cause: actFailureReason(refusal, ''),
      kindFailed: false,
      refreshFailed: false,
    });
    expect(reread).not.toHaveBeenCalled();
  });

  it('records the kind of a replaced token against the revision it read', async () => {
    // Arrange
    vi.spyOn(apiClient, 'setCredential').mockResolvedValue(undefined);
    const kind = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockResolvedValue({ applied: true } as never);
    vi.spyOn(apiClient, 'getMachineSettings').mockResolvedValue({ revision: 5 } as never);

    // Act
    const result = await storeCredential('github', 'ghp_new', 'pat');

    // Assert
    expect(kind).toHaveBeenCalledWith('credential:github', 'pat', 4);
    expect(result).toEqual({ ok: true, cause: '', kindFailed: false, refreshFailed: false });
    expect(get(runtimeStore).machineSettings?.revision).toBe(5);
  });

  it('keeps a stored token when its kind and the read-back fail', async () => {
    // Arrange
    vi.spyOn(apiClient, 'setCredential').mockResolvedValue(undefined);
    vi.spyOn(apiClient, 'updateMachineSetting').mockRejectedValue(new Error('conflict'));
    vi.spyOn(apiClient, 'getMachineSettings').mockRejectedValue(new Error('network_offline'));

    // Act
    const result = await storeCredential('github', 'ghp_new', 'pat');

    // Assert
    expect(result).toEqual({ ok: true, cause: '', kindFailed: true, refreshFailed: true });
  });

  it('answers nothing when the runtime refuses a setting read at no revision', async () => {
    // Arrange: the store has read no settings yet, so the change applies to revision 0
    runtimeStore.set(makeRuntimeState({ machineSettings: null }));
    const update = vi
      .spyOn(apiClient, 'updateMachineSetting')
      .mockRejectedValue(new Error('conflict'));

    // Act
    const result = await updateMachineSetting('documents_mode', 'copy');

    // Assert
    expect(result).toBeNull();
    expect(update).toHaveBeenCalledWith('documents_mode', 'copy', 0);
  });

  it('answers nothing when the runtime refuses a consent withdrawal', async () => {
    // Arrange
    const withdraw = vi
      .spyOn(apiClient, 'withdrawConsent')
      .mockRejectedValue(new Error('conflict'));

    // Act
    const result = await withdrawConsent('telemetry');

    // Assert
    expect(result).toBeNull();
    expect(withdraw).toHaveBeenCalledWith('telemetry', 4);
  });
});
