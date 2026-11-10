import { describe, it, expect, vi, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import { apiClient, type ReportsPage, type StatusResponse } from '../api/client';
import { ClientError } from '../api/errors';
import {
  adoptRouteFromLocation,
  checkUrlHashForCode,
  handleRuntimeEvent,
  initRuntime,
  landingRoute,
  openStartPage,
  pairBrowser,
  replaceDesignerProject,
  routeParamsOf,
  runtimeStore,
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
