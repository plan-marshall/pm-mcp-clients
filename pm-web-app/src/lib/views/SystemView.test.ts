import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import SystemView from './SystemView.svelte';
import { runtimeStore, lifecycleEpochOf, lifecycleFieldsOf } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { apiClient, type SystemSnapshot, type StatusResponse } from '../api/client';
import { ClientError } from '../api/errors';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import { get } from 'svelte/store';

/**
 * Puts one catalogue and its language in place, as a language switch does.
 *
 * @param locale the interface locale
 * @param overrides the entries this test changes
 */
function useCatalogue(locale: 'de' | 'en', overrides: Record<string, string> = {}): void {
  currentLocale.set(locale);
  messagesStore.set({ ...loadMessages(locale), ...overrides });
}

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

  afterEach(() => {
    useCatalogue('en');
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

  it.each([
    ['retry', '1 Wiederholung'],
    ['error', '1 Fehler'],
  ])('names the model status word %s in the interface language', async (_word, text) => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    useCatalogue('de');

    render(SystemView);

    await waitFor(() => {
      expect(screen.getByText('System')).toBeInTheDocument();
    });

    expect(screen.getByText(text)).toBeInTheDocument();
  });

  it('pluralizes the retry status word for a count above one', async () => {
    const snapshot = {
      ...mockSnapshot,
      models: [{ ...mockSnapshot.models[0], retries_count: 2, errors_count: 0 }],
    };
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(snapshot);
    useCatalogue('de');

    render(SystemView);

    await waitFor(() => {
      expect(screen.getByText('System')).toBeInTheDocument();
    });

    expect(screen.getByText('2 Wiederholungen')).toBeInTheDocument();
  });

  it('keeps each status word in the row of its own model', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    useCatalogue('de');

    render(SystemView);

    await waitFor(() => {
      expect(screen.getByText('System')).toBeInTheDocument();
    });

    const retryRow = screen.getByText('claude:opus-4.5').closest('tr') as HTMLElement;
    const errorRow = screen.getByText('codex:gpt-5').closest('tr') as HTMLElement;
    expect(within(retryRow).getByText('1 Wiederholung')).toBeInTheDocument();
    expect(within(errorRow).getByText('1 Fehler')).toBeInTheDocument();
  });

  it('follows a language switch with the status words of a loaded snapshot', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    useCatalogue('de');

    render(SystemView);

    await waitFor(() => {
      expect(screen.getByText('1 Wiederholung')).toBeInTheDocument();
    });

    useCatalogue('en');

    await waitFor(() => {
      expect(screen.getByText('1 retry')).toBeInTheDocument();
      expect(screen.getByText('1 error')).toBeInTheDocument();
    });
  });

  it('names the model status word ok from the catalogue', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    // The word of a healthy model reads the same in both packs, so only a changed pack shows
    // that the view reads it there instead of writing it in the markup.
    useCatalogue('de', { 'system.models.ok': 'einverstanden' });

    render(SystemView);

    await waitFor(() => {
      expect(screen.getByText('System')).toBeInTheDocument();
    });

    expect(screen.getByText('einverstanden')).toBeInTheDocument();
  });

  it('follows a language switch with the text of a failed load', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockRejectedValue(new Error('boom'));
    useCatalogue('de');

    render(SystemView);

    await waitFor(() => {
      expect(
        screen.getByText('Die Runtime hat die Anfrage abgelehnt'),
      ).toBeInTheDocument();
    });

    useCatalogue('en');

    await waitFor(() => {
      expect(screen.getByText('The runtime refused the request')).toBeInTheDocument();
    });
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

  it("shows the sentence of the act failure's code instead of the code", async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    vi.spyOn(apiClient, 'stopRuntime').mockRejectedValue(new ClientError('timeout'));

    render(SystemView);

    await waitFor(() => expect(screen.getByRole('button', { name: /Stop/i })).toBeInTheDocument());
    await fireEvent.click(screen.getByRole('button', { name: /Stop/i }));
    await fireEvent.click(screen.getByRole('button', { name: /Stop when done/i }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(
      'Try again; if it keeps taking this long, check that the runtime is running.',
    );
    expect(alert).not.toHaveTextContent(/^timeout$/);
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
    // The runtime.stopping event carries neither who nor when: the page invents neither.
    const stopped = screen.getByRole('region', { name: 'Stopped runtime' });
    expect(within(stopped).getByRole('heading')).toHaveTextContent(/^The runtime is stopped\.$/);
  });

  it('names who stopped the runtime and when, where the runtime recorded both', async () => {
    runtimeStore.update((s) => ({
      ...s,
      isStopped: true,
      stoppedInfo: { who: 'by the console', time: new Date(2026, 10, 13, 14, 12).toISOString() },
    }));
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);

    render(SystemView);

    const stopped = await screen.findByRole('region', { name: 'Stopped runtime' });
    expect(within(stopped).getByRole('heading')).toHaveTextContent(/^The runtime is stopped \(by the console, .*14:12\)\.$/);
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

  it('advances the lifecycle epoch on a successful stop so a stale status read does not overwrite the result', async () => {
    vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
    const stopSpy = vi.spyOn(apiClient, 'stopRuntime').mockResolvedValue(undefined as never);

    render(SystemView);
    await waitFor(() => expect(screen.getByRole('button', { name: /Stop…/i })).toBeInTheDocument());

    const epochBefore = lifecycleEpochOf();
    let resolveStaleStatus: (status: StatusResponse) => void = () => {};
    const pendingStatus = new Promise<StatusResponse>((resolve) => {
      resolveStaleStatus = resolve;
    });

    const inFlightRead = (async () => {
      const status = await pendingStatus;
      if (epochBefore !== lifecycleEpochOf()) return;
      runtimeStore.update((s) => ({ ...s, ...lifecycleFieldsOf(status) }));
    })();

    await fireEvent.click(screen.getByRole('button', { name: /Stop…/i }));
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    await fireEvent.click(screen.getByRole('button', { name: /Stop when done/i }));
    expect(stopSpy).toHaveBeenCalledWith('drain');

    resolveStaleStatus({ status: 'ready', onboarded: true });
    await inFlightRead;

    expect(get(runtimeStore).isDraining).toBe(true);
  });

  describe('the acts on the runtime and the upgrade', () => {
    /** The snapshot of a runtime with no newer version known, so its bar offers Check now. */
    const currentSnapshot: SystemSnapshot = {
      ...mockSnapshot,
      runtime: { ...mockSnapshot.runtime, upgrade_available_version: undefined },
    };

    /** Renders the view and waits until the runtime's upgrade is in the given step. */
    async function renderUpgrading(step: string): Promise<void> {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
      render(SystemView);
      await waitFor(() => expect(eventHandler).not.toBeNull());
      eventHandler?.('runtime.upgrading', {
        version: '0.10.0',
        installed: '0.9.1',
        step,
        withdrawable: true,
      });
      await screen.findByTestId('upgrade-steps');
    }

    it.each([
      ['not_withdrawable', 'Too late to withdraw the upgrade; the links are switched.'],
      [
        'act_not_available',
        'The upgrade could not be withdrawn: Reload the view and choose an act the state offers.',
      ],
      // A refusal without a code is no "too late": only the runtime's code says the links switched.
      ['HTTP 409', 'The upgrade could not be withdrawn: Action failed.'],
    ])('answers a withdraw refused with %s by its own sentence', async (refusal, sentence) => {
      vi.spyOn(apiClient, 'withdrawUpgrade').mockRejectedValue(new Error(refusal));
      await renderUpgrading('fetch');

      await fireEvent.click(screen.getByRole('button', { name: 'Withdraw the upgrade' }));

      expect(await screen.findByRole('alert')).toHaveTextContent(sentence);
      expect(screen.getByTestId('upgrade-steps')).toBeInTheDocument();
    });

    it('offers no withdraw once the links are switched, and hurries a waiting upgrade', async () => {
      const upgradeSpy = vi
        .spyOn(apiClient, 'upgradeRuntime')
        .mockResolvedValue({ status: 'upgrading' });
      await renderUpgrading('wait');

      expect(screen.queryByRole('button', { name: 'Withdraw the upgrade' })).toBeNull();
      await fireEvent.click(screen.getByRole('button', { name: 'Upgrade now' }));

      expect(upgradeSpy).toHaveBeenCalledWith('0.10.0', 'now');
      await waitFor(() =>
        expect(screen.getByTestId('upgrade-step-stop')).toHaveClass('upgrade-step--running'),
      );
      expect(screen.getByTestId('upgrade-step-wait')).toHaveClass('upgrade-step--done');
      expect(screen.queryByRole('button', { name: 'Upgrade now' })).toBeNull();
    });

    it("shows the refusal's sentence when hurrying the upgrade fails", async () => {
      vi.spyOn(apiClient, 'upgradeRuntime').mockRejectedValue(new Error('act_not_available'));
      await renderUpgrading('wait');

      await fireEvent.click(screen.getByRole('button', { name: 'Upgrade now' }));

      expect(await screen.findByRole('alert')).toHaveTextContent(
        'Reload the view and choose an act the state offers.',
      );
      expect(screen.getByRole('button', { name: 'Upgrade now' })).toBeInTheDocument();
    });

    it.each([
      [
        'verify',
        undefined,
        'The upgrade to 0.10.0 failed while verify: no space. 0.9.1 keeps running; nothing was changed.',
      ],
      [
        'start',
        '0.9.1',
        '0.10.0 did not start: no space. 0.9.1 was restored and runs again; running work goes on from its last committed state.',
      ],
      ['start', undefined, 'Neither 0.10.0 nor 0.9.1 started: no space.'],
    ])(
      'says what a failure at %s (restored %s) left running',
      async (step, restored, sentence) => {
        vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
        render(SystemView);
        await waitFor(() => expect(eventHandler).not.toBeNull());

        eventHandler?.('runtime.upgrade_failed', {
          version: '0.10.0',
          installed: '0.9.1',
          step,
          reason: 'no space',
          ...(restored ? { restored } : {}),
        });

        expect(await screen.findByTestId('upgrade-failed')).toHaveTextContent(sentence);
      },
    );

    it('dismisses a failed upgrade and offers the upgrade again', async () => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
      render(SystemView);
      await waitFor(() => expect(eventHandler).not.toBeNull());
      eventHandler?.('runtime.upgrade_failed', { version: '0.10.0', step: 'fetch', reason: 'x' });
      await screen.findByTestId('upgrade-failed');

      await fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }));

      expect(screen.queryByTestId('upgrade-failed')).toBeNull();
      expect(screen.getByRole('heading', { name: '0.10.0 is available.' })).toBeInTheDocument();
    });

    it('reports a consented upgrade the runtime refuses as failed while fetching', async () => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
      const upgradeSpy = vi
        .spyOn(apiClient, 'upgradeRuntime')
        .mockRejectedValue(new Error('act_not_available'));
      render(SystemView);
      await waitFor(() =>
        expect(screen.getAllByRole('button', { name: /Upgrade/i }).length).toBeGreaterThan(0),
      );
      await fireEvent.click(screen.getAllByRole('button', { name: /Upgrade/i })[0]);
      await screen.findByTestId('upgrade-consent');

      await fireEvent.click(screen.getByRole('radio', { name: /^Now/ }));
      await fireEvent.click(screen.getByRole('button', { name: 'Upgrade now' }));

      expect(upgradeSpy).toHaveBeenCalledWith('0.10.0', 'now');
      expect(await screen.findByTestId('upgrade-failed')).toHaveTextContent(
        'The upgrade to 0.10.0 failed while fetch: Reload the view and choose an act the state offers',
      );
    });

    it('closes the consent on Not now without sending anything', async () => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
      const upgradeSpy = vi.spyOn(apiClient, 'upgradeRuntime');
      render(SystemView);
      await waitFor(() =>
        expect(screen.getAllByRole('button', { name: /Upgrade/i }).length).toBeGreaterThan(0),
      );
      await fireEvent.click(screen.getAllByRole('button', { name: /Upgrade/i })[0]);
      await screen.findByTestId('upgrade-consent');

      await fireEvent.click(screen.getByRole('button', { name: 'Not now' }));

      expect(screen.queryByTestId('upgrade-consent')).toBeNull();
      expect(upgradeSpy).not.toHaveBeenCalled();
    });

    it.each([
      [0, 'No worktree has uncommitted changes'],
      [2, '2 worktrees have uncommitted changes'],
    ])('lists %s dirty worktrees among the checks before upgrading', async (count, line) => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue({
        ...mockSnapshot,
        runtime: { ...mockSnapshot.runtime, dirty_worktrees: count },
      });
      render(SystemView);
      await waitFor(() =>
        expect(screen.getAllByRole('button', { name: /Upgrade/i }).length).toBeGreaterThan(0),
      );

      await fireEvent.click(screen.getAllByRole('button', { name: /Upgrade/i })[0]);

      expect(await screen.findByTestId('upgrade-check-list')).toHaveTextContent(line);
    });

    it('disables the upgrade of an installation the runtime cannot upgrade and says why', async () => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue({
        ...mockSnapshot,
        runtime: {
          ...mockSnapshot.runtime,
          upgrade_possible: false,
          upgrade_disabled_reason: 'PM_MCP_HOME is set',
        },
      });
      render(SystemView);

      expect(
        await screen.findByText(
          'This installation is not upgraded by the runtime: PM_MCP_HOME is set.',
        ),
      ).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Upgrade…' })).toBeDisabled();
    });

    it.each([
      [undefined, 'Update check completed.'],
      [new Error('act_not_available'), 'Could not check for a newer version: Reload the view'],
    ])('checks for a newer version and reports the outcome (%s)', async (failure, line) => {
      const snapshotSpy = vi
        .spyOn(apiClient, 'getSystemSnapshot')
        .mockResolvedValue(currentSnapshot);
      const checkSpy = vi.spyOn(apiClient, 'checkUpgrade');
      if (failure) checkSpy.mockRejectedValue(failure);
      else checkSpy.mockResolvedValue({ current_version: '0.9.1' });
      render(SystemView);
      await screen.findByText('newer version not checked');
      const readsBefore = snapshotSpy.mock.calls.length;

      await fireEvent.click(screen.getByRole('button', { name: 'Check now' }));

      expect(await screen.findByRole(failure ? 'alert' : 'status')).toHaveTextContent(line);
      expect(snapshotSpy.mock.calls.length).toBe(failure ? readsBefore : readsBefore + 1);
    });

    it('restarts now with its own note and confirmation', async () => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(currentSnapshot);
      const stopSpy = vi
        .spyOn(apiClient, 'stopRuntime')
        .mockResolvedValue({ lifecycle_state: 'stopping' });
      render(SystemView);
      await fireEvent.click(await screen.findByRole('button', { name: 'Restart…' }));

      expect(screen.getByText('Every client connects again by itself.')).toBeInTheDocument();
      await fireEvent.click(screen.getByRole('radio', { name: /^Now/ }));
      await fireEvent.click(screen.getByRole('button', { name: 'Restart now' }));

      expect(stopSpy).toHaveBeenCalledWith('now', 'restart');
      await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
      expect(get(runtimeStore).isStopped).toBe(false);
    });

    it('shows the stopped page after a stop now, and keeps the runtime running on Keep', async () => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(currentSnapshot);
      const stopSpy = vi
        .spyOn(apiClient, 'stopRuntime')
        .mockResolvedValue({ lifecycle_state: 'stopped' });
      render(SystemView);

      await fireEvent.click(await screen.findByRole('button', { name: 'Stop…' }));
      await fireEvent.click(screen.getByRole('button', { name: 'Keep it running' }));
      expect(screen.queryByRole('dialog')).toBeNull();
      expect(stopSpy).not.toHaveBeenCalled();

      await fireEvent.click(screen.getByRole('button', { name: 'Stop…' }));
      await fireEvent.click(screen.getByRole('radio', { name: /^Now/ }));
      await fireEvent.click(screen.getByRole('button', { name: 'Stop now' }));

      expect(stopSpy).toHaveBeenCalledWith('now');
      expect(await screen.findByRole('region', { name: 'Stopped runtime' })).toBeInTheDocument();
    });
  });

  describe('the stopped page', () => {
    const originalClipboard = navigator.clipboard;

    afterEach(() => {
      Object.defineProperty(navigator, 'clipboard', {
        value: originalClipboard,
        configurable: true,
      });
    });

    /** @param writeText the clipboard writer the test hands the view */
    async function renderStopped(writeText: (text: string) => Promise<void>): Promise<void> {
      Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
      runtimeStore.update((s) => ({ ...s, isStopped: true }));
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
      render(SystemView);
      await screen.findByRole('button', { name: 'Copy the command' });
    }

    it('copies the start command and confirms the copy', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined);
      await renderStopped(writeText);

      await fireEvent.click(screen.getByRole('button', { name: 'Copy the command' }));

      expect(writeText).toHaveBeenCalledWith('pm-operator runtime start');
      expect(await screen.findByText('Copied to clipboard.')).toBeInTheDocument();
    });

    it('claims no copy the clipboard refused', async () => {
      const writeText = vi.fn().mockRejectedValue(new Error('NotAllowedError'));
      await renderStopped(writeText);

      await fireEvent.click(screen.getByRole('button', { name: 'Copy the command' }));

      await waitFor(() => expect(writeText).toHaveBeenCalled());
      await Promise.resolve();
      expect(screen.queryByText('Copied to clipboard.')).toBeNull();
    });
  });

  describe('the live figures and the filters', () => {
    it.each(['system.metrics', 'runtime.draining', 'runtime.drain_withdrawn'])(
      'reads the snapshot again on %s',
      async (event) => {
        const fetchSpy = vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
        render(SystemView);
        await screen.findByText('System');
        await waitFor(() => expect(eventHandler).not.toBeNull());
        const readsBefore = fetchSpy.mock.calls.length;

        eventHandler?.(event, {});

        await waitFor(() => expect(fetchSpy.mock.calls.length).toBe(readsBefore + 1));
      },
    );

    it('drops the remembered project when the project list cannot be read', async () => {
      localStorage.setItem(
        'pm_system_filters',
        JSON.stringify({ window: 'hour', groupBy: 'model', project: 'pm-mcp-clients' }),
      );
      vi.spyOn(apiClient, 'getProjectExplorer').mockRejectedValue(new ClientError('timeout'));
      const fetchSpy = vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);

      render(SystemView);

      await waitFor(() => expect(fetchSpy).toHaveBeenLastCalledWith('hour', undefined));
      expect(screen.queryByTestId('system-project-filter')).toBeNull();
    });

    it('marks the runtime offline when the snapshot cannot reach it, and retries', async () => {
      const fetchSpy = vi
        .spyOn(apiClient, 'getSystemSnapshot')
        .mockRejectedValueOnce(new ClientError('network_offline'))
        .mockResolvedValue(mockSnapshot);
      render(SystemView);

      const failure = await screen.findByTestId('system-error');
      expect(get(runtimeStore).isOffline).toBe(true);

      await fireEvent.click(within(failure).getByRole('button', { name: 'Try again' }));

      await waitFor(() => expect(screen.queryByTestId('system-error')).toBeNull());
      expect(fetchSpy).toHaveBeenCalledTimes(2);
    });

    it('sums the models per role, keeping an unmeasured role apart', async () => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
      render(SystemView);
      await screen.findByText('claude:opus-4.5');

      await fireEvent.click(screen.getByRole('button', { name: 'Per role' }));

      const implementer = screen.getByText('implementer', { selector: 'strong.mono' }).closest('tr');
      expect(implementer).not.toBeNull();
      // 118 + 12 calls; the unmeasured gpt-5 calls do not hide the measured tokens of sonnet.
      expect(within(implementer as HTMLElement).getByText('130')).toBeInTheDocument();
      expect(implementer).toHaveTextContent('1 error');
      expect(screen.getByText('planner', { selector: 'strong.mono' })).toBeInTheDocument();
    });

    it('says when nothing runs, no worker is active, and no model was called', async () => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue({
        ...mockSnapshot,
        work: { ...mockSnapshot.work, plans_by_state: {}, epics_by_state: {} },
        workers: [],
        models: [],
      });
      render(SystemView);

      expect(await screen.findAllByText('none running')).toHaveLength(2);
      expect(screen.getByText('No active workers.')).toBeInTheDocument();
      expect(screen.getByText('No model calls in this window.')).toBeInTheDocument();
    });

    it.each([
      ['Last hour', 'hour'],
      ['Today', 'today'],
      ['7 days', '7d'],
    ])('reads the window %s and names it above the models', async (label, wire) => {
      localStorage.setItem(
        'pm_system_filters',
        JSON.stringify({ window: wire === 'hour' ? 'today' : 'hour', groupBy: 'model', project: '' }),
      );
      const fetchSpy = vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
      render(SystemView);
      const group = await screen.findByRole('group', { name: /window/i });

      await fireEvent.click(within(group).getByRole('button', { name: label }));

      expect(fetchSpy).toHaveBeenLastCalledWith(wire, undefined);
      const heading = screen.getByRole('heading', { name: 'Models' }).parentElement;
      expect(heading).toHaveTextContent(label);
      expect(heading).not.toHaveTextContent(/\b7d\b/);
    });

    it('reads the snapshot again on Refresh and switches the models back per model', async () => {
      const fetchSpy = vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
      render(SystemView);
      await screen.findByText('claude:opus-4.5');

      await fireEvent.click(screen.getByRole('button', { name: 'Refresh system view' }));
      await waitFor(() => expect(fetchSpy).toHaveBeenCalledTimes(2));

      await fireEvent.click(screen.getByRole('button', { name: 'Per role' }));
      expect(screen.queryByText('claude:opus-4.5')).toBeNull();
      await fireEvent.click(screen.getByRole('button', { name: 'Per model' }));
      expect(screen.getByText('claude:opus-4.5')).toBeInTheDocument();
      // The workers' table and the models' table both head their column Model again.
      expect(screen.getAllByRole('columnheader', { name: 'Model' })).toHaveLength(2);
    });

    it('shows the release notes of a newer version as text with an inert link', async () => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue({
        ...mockSnapshot,
        runtime: {
          ...mockSnapshot.runtime,
          upgrade_release_notes_summary: '<b>faster relay</b>',
          upgrade_release_notes_url: '/api/v1/system/release-notes?version=0.10.0',
        },
      });
      render(SystemView);

      expect(await screen.findByText('<b>faster relay</b>')).toBeInTheDocument();
      const links = screen.getAllByRole('link', { name: 'Release notes' });
      expect(links).toHaveLength(2);
      for (const link of links) {
        expect(link).toHaveAttribute('href', '/api/v1/system/release-notes?version=0.10.0');
        expect(link).toHaveAttribute('rel', 'noreferrer');
      }
    });

    it('opens the consent from the Upgrade act of the runtime card too', async () => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
      render(SystemView);
      await waitFor(() =>
        expect(screen.getAllByRole('button', { name: 'Upgrade…' })).toHaveLength(2),
      );

      await fireEvent.click(screen.getAllByRole('button', { name: 'Upgrade…' })[1]);

      expect(await screen.findByTestId('upgrade-consent')).toHaveTextContent(
        'Upgrade 0.9.1 to 0.10.0?',
      );
    });

    it('shows a figure the runtime did not measure as not available, never as zero', async () => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue({
        ...mockSnapshot,
        runtime: {
          version: '0.9.1',
          cpu_percent: null,
          cpu_measured: false,
          socket_listener: true,
          loopback_address: null,
          lan_address: null,
          upgrade_possible: false,
        },
        work: { ...mockSnapshot.work, recovering_projects: ['pm-mcp-core'] },
      });
      render(SystemView);

      const memory = await screen.findByText(/^memory /);
      expect(memory).toHaveTextContent(
        'memory not available in use of not available · not available resident',
      );
      expect(screen.getByText(/^machine store /)).toHaveTextContent(
        'machine store not available · not available free on its disk · measured not available',
      );
      expect(screen.queryByText(/0 B/)).toBeNull();
      expect(screen.getByText('not measured on this platform')).toBeInTheDocument();
      expect(screen.getByText(/loopback off/)).toHaveTextContent('network off');
      expect(screen.getByText('newer version not checked')).toBeInTheDocument();
      expect(screen.getByText(/recovering: pm-mcp-core/)).toBeInTheDocument();
    });

    it('opens the overview on the epics an epic figure counts', async () => {
      vi.spyOn(apiClient, 'getSystemSnapshot').mockResolvedValue(mockSnapshot);
      render(SystemView);

      await fireEvent.click(
        await screen.findByRole('button', { name: /running epics in the overview/i }),
      );

      expect(get(runtimeStore)).toMatchObject({ currentRoute: 'overview', overviewFocus: 'epics' });
    });
  });
});
