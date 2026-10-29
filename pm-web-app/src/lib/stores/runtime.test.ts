import { describe, it, expect, vi, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import { apiClient, type ReportsPage, type StatusResponse } from '../api/client';
import {
  checkUrlHashForCode,
  initRuntime,
  openStartPage,
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

  it('keeps the onboarding step the address names in the route params', () => {
    // Arrange: the view consumes the store's step, but the address keeps the aspect.
    window.history.replaceState(null, '', '/onboarding?step=6');
    runtimeStore.set(makeRuntimeState({ currentRoute: 'onboarding', onboardingStep: null }));

    // Act
    const params = routeParamsOf(get(runtimeStore));

    // Assert
    expect(params.onboardingStep).toBe(6);
  });
});
