import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import SystemView from './SystemView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { apiClient, type SystemSnapshot } from '../api/client';
import { get } from 'svelte/store';

describe('SystemView component', () => {
  let eventHandler: ((event: string, data: unknown) => void) | null = null;
  const mockSnapshot: SystemSnapshot = {
    runtime: {
      version: '0.9.1',
      build_date: '2026-10-02',
      git_commit: 'a1b2c3d',
      uptime_seconds: 22320,
      uptime_formatted: '6 h 12 min',
      memory_used_bytes: 432013312,
      memory_max_bytes: 1073741824,
      memory_resident_bytes: 713031680,
      cpu_percent: 4,
      cpu_measured: true,
      machine_store_bytes: 2469606195,
      disk_free_bytes: 40802189312,
      disk_measured_at: '13:40',
      socket_listener: true,
      loopback_address: '127.0.0.1:7420',
      lan_address: '192.168.1.20:7421',
      lan_devices_count: 2,
      relays_behind_count: 0,
      upgrade_available_version: '0.10.0',
      upgrade_possible: true,
    },
    work: {
      plans_by_state: { running: 3, waiting_for_you: 1, held: 1, paused: 2 },
      epics_by_state: { running: 1, waiting_for_you: 1 },
      tasks_running: 4,
      tasks_queued: 6,
      active_projects_count: 3,
      recovering_projects: [],
    },
    workers: [
      {
        role: 'planner',
        harness: 'claude',
        model: 'opus-4.5',
        state: 'busy',
        current_plan_title: 'fix-relay',
      },
      {
        role: 'implementer',
        harness: 'claude',
        model: 'sonnet-4.5',
        state: 'busy',
        current_plan_title: 'relay-docs',
      },
      {
        role: 'implementer',
        harness: 'codex',
        model: 'gpt-5',
        state: 'idle',
        current_plan_title: null,
      },
    ],
    jobs: {
      build: { slots_used: 1, slots_total: 2, queued: 1 },
      test: { slots_used: 0, slots_total: 1, queued: 0 },
      locks_held: 3,
    },
    models: [
      {
        id: 'claude:opus-4.5',
        role: 'planner',
        calls_count: 42,
        tokens_total: 2020000,
        tokens_in: 1900000,
        tokens_out: 120000,
        errors_count: 0,
        retries_count: 1,
      },
      {
        id: 'claude:sonnet-4.5',
        role: 'implementer',
        calls_count: 118,
        tokens_total: 3610000,
        tokens_in: 3200000,
        tokens_out: 410000,
        errors_count: 0,
        retries_count: 0,
      },
      {
        id: 'codex:gpt-5',
        role: 'implementer',
        calls_count: 12,
        tokens_total: 0,
        tokens_in: 0,
        tokens_out: 0,
        errors_count: 1,
        retries_count: 0,
        tokens_unmeasured: true,
        unmeasured_calls: 12,
      },
    ],
    cleanup: {
      plans_count: 12,
      epics_count: 2,
      logs_count: 40,
      read_reports_count: 5,
      unenrolled_projects_count: 1,
      freed_bytes: 1932735283,
      kept_for_good_count: 4,
      last_sweep_at: 'today 03:00',
      next_check_at: 'tomorrow 03:00',
    },
    window: 'hour',
    project: null,
  };

  beforeEach(() => {
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: true,
        deviceLabel: 'Firefox on mbp',
        machineName: 'mbp-oliver',
        currentRoute: 'system',
        currentState: null,
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 0,
        isDraining: false,
        drainInfo: null,
        isStopped: false,
        stoppedInfo: null,
        activeStep: 1,
        unreadReportsCount: 0,
      }),
    );
    vi.restoreAllMocks();
    eventHandler = null;
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
      favourites: [],
      recent: [],
      all_projects: [
        {
          project_id: 'pm-mcp-clients',
          display_name: 'pm-mcp-clients',
          readiness: 'ready',
          needs_operator_count: 0,
          active_plans_count: 3,
          active_epics_count: 1,
          is_favourite: true,
          is_released: false,
        },
        {
          project_id: 'pm-mcp-core',
          display_name: 'pm-mcp-core',
          readiness: 'ready',
          needs_operator_count: 0,
          active_plans_count: 0,
          active_epics_count: 0,
          is_favourite: false,
          is_released: false,
        },
      ],
      being_added: [],
    });
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((handler) => {
      eventHandler = handler;
      return () => {};
    });
  });

  it('renders the 6-part dashboard with metrics and models', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);

    render(SystemView);

    await waitFor(() => {
      expect(screen.getByText('System')).toBeInTheDocument();
    });

    expect(screen.getAllByText(/pm-mcpd 0\.9\.1/)[0]).toBeInTheDocument();
    expect(screen.getByText(/3 running/)).toBeInTheDocument();
    expect(screen.getByText('fix-relay')).toBeInTheDocument();
    expect(screen.getByText('relay-docs')).toBeInTheDocument();
    expect(screen.getByText('claude:opus-4.5')).toBeInTheDocument();
    expect(screen.getByText(/1 retry/)).toBeInTheDocument();
    expect(screen.getByText(/1 error/)).toBeInTheDocument();
  });

  it('toggles time windows and triggers data reload with window query param', async () => {
    const fetchSpy = vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);

    render(SystemView);

    await waitFor(() => {
      expect(screen.getByText('System')).toBeInTheDocument();
    });

    const todayBtn = screen.getByRole('button', { name: 'Today' });
    await fireEvent.click(todayBtn);

    expect(fetchSpy).toHaveBeenCalledWith('today', undefined);
  });

  it('opens lifecycle stop modal and calls stopRuntime with drain', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    const stopSpy = vi
      .spyOn(apiClient, 'stopRuntime')
      .mockResolvedValue({ lifecycle_state: 'draining' });

    render(SystemView);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Stop/i })).toBeInTheDocument();
    });

    const stopBtn = screen.getByRole('button', { name: /Stop/i });
    await fireEvent.click(stopBtn);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('When what runs is done')).toBeInTheDocument();

    const confirmBtn = screen.getByRole('button', { name: /Stop when done/i });
    await fireEvent.click(confirmBtn);

    expect(stopSpy).toHaveBeenCalledWith('drain');
  });

  it('navigates to cleanup route when review button is clicked', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);

    render(SystemView);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Review/i })).toBeInTheDocument();
    });

    const reviewBtn = screen.getByRole('button', { name: /Review/i });
    await fireEvent.click(reviewBtn);

    let route = '';
    runtimeStore.subscribe((s) => {
      route = s.currentRoute;
    })();
    expect(route).toBe('cleanup');
  });

  it('displays stopped runtime panel when isStopped is set', async () => {
    runtimeStore.update((s) => ({ ...s, isStopped: true }));
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);

    render(SystemView);

    await waitFor(() => {
      expect(screen.getByText('Copy the command')).toBeInTheDocument();
    });
  });

  it('remembers the window, the grouping, and the project per browser', async () => {
    const fetchSpy = vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    render(SystemView);

    await waitFor(() => expect(screen.getByText('System')).toBeInTheDocument());

    await fireEvent.click(screen.getByRole('button', { name: 'Today' }));
    await fireEvent.click(screen.getByRole('button', { name: 'Per role' }));
    await fireEvent.change(screen.getByTestId('system-project-filter'), {
      target: { value: 'pm-mcp-clients' },
    });

    const saved = JSON.parse(localStorage.getItem('pm_system_filters') ?? '{}');
    expect(saved).toEqual({
      window: 'today',
      groupBy: 'role',
      project: 'pm-mcp-clients',
    });
    expect(fetchSpy).toHaveBeenLastCalledWith('today', 'pm-mcp-clients');
  });

  it('restores the remembered filters on mount', async () => {
    localStorage.setItem(
      'pm_system_filters',
      JSON.stringify({ window: '7d', groupBy: 'role', project: 'pm-mcp-clients' }),
    );
    const fetchSpy = vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);

    render(SystemView);

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledWith('7d', 'pm-mcp-clients'));
  });

  it('drops a remembered project that is no longer enrolled', async () => {
    localStorage.setItem(
      'pm_system_filters',
      JSON.stringify({ window: 'hour', groupBy: 'model', project: 'gone' }),
    );
    const fetchSpy = vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);

    render(SystemView);

    await waitFor(() => expect(fetchSpy).toHaveBeenLastCalledWith('hour', undefined));
    expect(JSON.parse(localStorage.getItem('pm_system_filters') ?? '{}').project).toBe('');
  });

  it('makes the work figures links to what they count', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    render(SystemView);

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /running plans in the overview/i })).toBeVisible(),
    );

    await fireEvent.click(screen.getByRole('button', { name: /running plans in the overview/i }));
    expect(get(runtimeStore).currentRoute).toBe('overview');

    await fireEvent.click(screen.getByRole('button', { name: /projects with running work/i }));
    expect(get(runtimeStore).currentRoute).toBe('projects');
  });

  it('shows the pre-upgrade checks and consents to the upgrade', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    const upgradeSpy = vi
      .spyOn(apiClient, 'upgradeRuntime')
      .mockResolvedValue({ status: 'started' });

    render(SystemView);

    await waitFor(() =>
      expect(screen.getAllByRole('button', { name: /Upgrade/i }).length).toBeGreaterThan(0),
    );
    await fireEvent.click(screen.getAllByRole('button', { name: /Upgrade/i })[0]);

    const dialog = await screen.findByTestId('upgrade-consent');
    expect(dialog).toHaveTextContent('Upgrade 0.9.1 to 0.10.0?');
    expect(screen.getByTestId('upgrade-check-list')).toHaveTextContent('2 workers are busy');
    expect(screen.getByTestId('upgrade-check-list')).toHaveTextContent('1 build runs');

    await fireEvent.click(screen.getByRole('button', { name: /Upgrade when done/i }));
    expect(upgradeSpy).toHaveBeenCalledWith('0.10.0', 'drain');
  });

  it('withdraws an upgrade that waits for what runs', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    const withdrawSpy = vi.spyOn(apiClient, 'withdrawUpgrade').mockResolvedValue();
    render(SystemView);
    await waitFor(() => expect(eventHandler).not.toBeNull());

    eventHandler?.('runtime.upgrading', {
      version: '0.10.0',
      installed: '0.9.1',
      step: 'verify',
      withdrawable: true,
    });

    await waitFor(() => expect(screen.getByTestId('upgrade-steps')).toBeInTheDocument());
    await fireEvent.click(screen.getByRole('button', { name: /Withdraw the upgrade/i }));
    expect(withdrawSpy).toHaveBeenCalled();
    expect(await screen.findByTestId('upgrade-withdrawn')).toBeInTheDocument();
  });

  it('shows the cause of a failed upgrade and offers to try again', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    render(SystemView);
    await waitFor(() => expect(eventHandler).not.toBeNull());

    eventHandler?.('runtime.upgrade_failed', {
      version: '0.10.0',
      installed: '0.9.1',
      step: 'verify',
      reason: 'the digest did not match',
    });

    const failed = await screen.findByTestId('upgrade-failed');
    expect(failed).toHaveTextContent('failed while verify: the digest did not match');
    expect(screen.getByRole('button', { name: /Try again/i })).toBeInTheDocument();
  });

  it('waits for unsent text before reloading after an upgrade', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    render(SystemView);
    await waitFor(() => expect(eventHandler).not.toBeNull());

    const field = document.createElement('textarea');
    field.value = 'a half-written answer';
    document.body.appendChild(field);

    eventHandler?.('runtime.upgraded', { version: '0.10.0' });

    const reload = await screen.findByTestId('upgrade-reload');
    expect(reload).toHaveTextContent('0.10.0');
    expect(reload).toHaveTextContent('waits until the text');
    field.remove();
  });
});
