import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import SystemView from './SystemView.svelte';
import { runtimeStore } from '../stores/runtime';
import { apiClient, type SystemSnapshot } from '../api/client';

describe('SystemView component', () => {
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
    runtimeStore.set({
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
      isDraining: false,
      drainInfo: null,
      isStopped: false,
      stoppedInfo: null,
      activeStep: 1,
      unreadReportsCount: 0,
    });
    vi.restoreAllMocks();
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
});
