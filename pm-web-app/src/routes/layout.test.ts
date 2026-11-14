import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import { createRawSnippet } from 'svelte';
import { get, type Writable } from 'svelte/store';
import { messagesStore } from '../lib/i18n';
import Layout from './+layout.svelte';
import { apiClient, type ReportsPage, type StatusResponse } from '../lib/api/client';
import { ClientError } from '../lib/api/errors';
import {
  runtimeStore,
  handleRuntimeEvent,
  isRuntimeRecovering,
  RECONNECT_INTERVAL_MS,
  stopRuntimeRecovery,
} from '../lib/stores/runtime';
import { heldFigure, holdFigure } from '../lib/stores/heldFigure';
import { makeRuntimeState } from '../lib/test/runtimeState';
import { loadMessages } from '../test/catalogue-fixture';

// The held figure is a session store fetched once; the shell only starts the fetch and the pages
// only read it, so the test sets the held URL itself.
vi.mock('../lib/stores/heldFigure', async () => {
  const { writable } = await import('svelte/store');
  return { heldFigure: writable(''), holdFigure: vi.fn() };
});

const held = heldFigure as Writable<string>;

// A failed start leaves the store retrying; no retry outlives its test.
afterEach(() => {
  stopRuntimeRecovery();
});

const STATUS: StatusResponse = {
  status: 'ready',
  onboarded: true,
  machine: { name: 'mbp-oliver', onboarded: true },
};

const REPORTS: ReportsPage = { items: [], unread_count: 0, has_older: false, older_count: 0 };

/**
 * The app shell: the header is the only always-on status row, the brand opens the start page, and
 * the designer is reachable from the header.
 */
describe('the app shell', () => {
  beforeEach(() => {
    localStorage.clear();
    runtimeStore.set(
      makeRuntimeState({ isInitialized: false, isPaired: false, currentRoute: 'pair' }),
    );
    window.history.replaceState(null, '', '/reports');
    vi.spyOn(apiClient, 'getMessages').mockResolvedValue(loadMessages('en'));
    messagesStore.set(loadMessages('en'));
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });
    vi.spyOn(apiClient, 'getReports').mockResolvedValue(REPORTS);
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  });

  it('keeps the machine badge as the only status row, with the mode and device count', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue(STATUS);
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_url: 'http://127.0.0.1:7420',
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 2,
      onboarded: true,
    });

    render(Layout);

    await screen.findByTestId('header-machine');
    const badge = screen.getByTestId('header-machine');
    expect(badge).toHaveAttribute('data-connection-mode', 'loopback');
    await waitFor(() =>
      expect(badge).toHaveAttribute('title', expect.stringContaining('2 paired devices')),
    );
    expect(screen.queryByTestId('connection-status-banner')).not.toBeInTheDocument();
  });

  it('opens the start page from the brand and the designer from the header', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue(STATUS);
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });

    render(Layout);
    await screen.findByTestId('brand-home');

    await fireEvent.click(screen.getByTestId('brand-home'));
    await waitFor(() => expect(window.location.pathname).toBe('/projects'));

    await fireEvent.click(screen.getByTestId('nav-designer-btn'));
    await waitFor(() => expect(window.location.pathname).toBe('/designer'));
    expect(get(runtimeStore).currentRoute).toBe('designer');
  });

  it('gives the header actions a hover advisory that adds to their label', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue(STATUS);
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });

    render(Layout);
    await screen.findByTestId('brand-home');

    const brand = screen.getByTestId('brand-home');
    expect(brand).toHaveAttribute(
      'title',
      'Open the overview or the projects, wherever you usually start',
    );
    expect(brand).not.toHaveAttribute('title', 'Start page');

    // A text button whose label says it all keeps no tooltip of its own.
    expect(screen.getByTestId('nav-projects-btn')).not.toHaveAttribute('title');
    expect(screen.getByTestId('nav-designer-btn')).not.toHaveAttribute('title');

    // The theme offers three explicit choices as one radio group, each with a visible label and
    // none with a tooltip that would repeat it.
    const theme = screen.getByRole('group', { name: 'Theme' });
    expect(
      within(theme)
        .getAllByRole('radio')
        .map((radio) => (radio as HTMLInputElement).labels?.[0]?.textContent?.trim()),
    ).toEqual(['Light', 'Dark', 'System']);
    for (const option of within(theme).getAllByRole('radio')) {
      expect(option as HTMLInputElement).not.toHaveAttribute('title');
    }
    // The visible label carries no tooltip either: it says what it does.
    for (const choice of ['light', 'dark', 'system']) {
      expect(screen.getByTestId(`theme-${choice}`)).not.toHaveAttribute('title');
    }
  });

  it('shows the project mark in the linked brand beside the visible text', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue(STATUS);
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });

    render(Layout);
    const brand = await screen.findByTestId('brand-home');

    expect(brand.querySelectorAll('[data-testid="brand-mark"]')).toHaveLength(1);
    expect(brand).toHaveTextContent('plan-marshall');
  });

  it('leaves the brand inert while the browser is unpaired', async () => {
    render(Layout);

    const inert = await screen.findByTestId('brand-inert');
    expect(inert.querySelectorAll('[data-testid="brand-mark"]')).toHaveLength(1);
    expect(inert).toHaveTextContent('plan-marshall');
    expect(screen.queryByTestId('brand-home')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nav-designer-btn')).not.toBeInTheDocument();
  });

  it('names the network mode in the pill title and keeps its data hook', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue(STATUS);
    vi.spyOn(apiClient, 'getConnectionStatus').mockRejectedValue(new Error('unavailable'));
    vi.spyOn(window, 'location', 'get').mockReturnValue({
      ...window.location,
      hostname: '192.168.1.20',
      origin: 'http://192.168.1.20:5173',
      pathname: '/reports',
    } as Location);

    render(Layout);

    const badge = await screen.findByTestId('header-machine');
    await waitFor(() => expect(badge).toHaveAttribute('data-connection-mode', 'lan'));
    expect(badge).toHaveAttribute(
      'title',
      'This browser reaches the machine over HTTPS on the network.',
    );
  });

  it('names what the drain still runs and lets the list shrink as work ends', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['execute task of fix-relay', 'build of fix-relay'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isDraining: true,
        drainInfo: {
          who: 'the console',
          count: 2,
          items: ['execute task of fix-relay', 'build of fix-relay'],
        },
      }),
    );

    render(Layout);

    const banner = await screen.findByTestId('draining-banner');
    expect(banner).toHaveTextContent(
      '2 left: execute task of fix-relay, build of fix-relay',
    );
    expect(within(screen.getByTestId('draining-list')).getAllByRole('listitem')).toHaveLength(2);

    runtimeStore.update((state) => ({
      ...state,
      drainInfo: {
        who: 'the console',
        count: 1,
        items: ['build of fix-relay'],
      },
    }));

    await waitFor(() => {
      expect(screen.getByTestId('draining-banner')).toHaveTextContent('1 left: build of fix-relay');
    });
    expect(within(screen.getByTestId('draining-list')).getAllByRole('listitem')).toHaveLength(1);
  });

  it('caps the names at three and counts the rest', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['one', 'two', 'three', 'four', 'five'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isDraining: true,
        drainInfo: {
          who: 'the console',
          count: 5,
          items: ['one', 'two', 'three', 'four', 'five'],
        },
      }),
    );

    render(Layout);

    expect(await screen.findByTestId('draining-more')).toHaveTextContent('+2 more');
    expect(within(screen.getByTestId('draining-list')).getAllByRole('listitem')).toHaveLength(3);
  });

  it('asks before Stop now and clears the drain when the runtime takes it', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['build of fix-relay'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    const stopNow = vi
      .spyOn(apiClient, 'stopRuntime')
      .mockResolvedValue({ lifecycle_state: 'stopping' });

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');

    await fireEvent.click(within(banner).getByRole('button', { name: 'Stop now' }));
    expect(screen.getByTestId('stop-now-confirm')).toHaveTextContent(
      'Withdraw what still runs (1 left) and stop now?',
    );
    expect(stopNow).not.toHaveBeenCalled();
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));
    expect(stopNow).toHaveBeenCalledWith('now');

    // The accepted stop ends the drain at once, even before a lifecycle event arrives.
    await waitFor(() => {
      expect(screen.queryByTestId('draining-banner')).not.toBeInTheDocument();
      expect(get(runtimeStore).isStopped).toBe(true);
    });
    expect(get(runtimeStore)).toMatchObject({ isDraining: false, isStopped: true });
  });

  it('ends the drain without a stopped runtime when Stop now ends in a restart', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['build of fix-relay'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    vi.spyOn(apiClient, 'stopRuntime').mockResolvedValue({ lifecycle_state: 'running' });

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    await fireEvent.click(within(banner).getByRole('button', { name: 'Stop now' }));
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));

    await waitFor(() => expect(screen.queryByTestId('draining-banner')).not.toBeInTheDocument());
    expect(get(runtimeStore)).toMatchObject({ isDraining: false, isStopped: false });
  });

  it('shows the refusal of a withdraw and ignores a second click', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['build of fix-relay'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    let refuseWithdraw: (reason: Error) => void = () => {};
    const pending = new Promise<void>((_resolve, reject) => {
      refuseWithdraw = reject;
    });
    const withdraw = vi.spyOn(apiClient, 'withdrawStop').mockReturnValue(pending);

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    await fireEvent.click(within(banner).getByRole('button', { name: 'Withdraw the stop' }));
    await fireEvent.click(within(banner).getByRole('button', { name: 'Withdraw the stop' }));
    expect(withdraw).toHaveBeenCalledOnce();

    refuseWithdraw(new Error('too late'));
    expect(await screen.findByTestId('draining-failure')).toHaveTextContent(
      'The stop could not be withdrawn: Action failed.',
    );
  });

  it('shows the refusal of Stop now and ignores a second confirm while it runs', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['build of fix-relay'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    let refuseStopNow: (reason: Error) => void = () => {};
    const pending = new Promise<{ lifecycle_state: string }>((_resolve, reject) => {
      refuseStopNow = reject;
    });
    const stopNow = vi.spyOn(apiClient, 'stopRuntime').mockReturnValue(pending);

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    await fireEvent.click(within(banner).getByRole('button', { name: 'Stop now' }));
    const confirm = screen.getByTestId('confirm-dialog-confirm');
    await fireEvent.click(confirm);
    await fireEvent.click(confirm);
    expect(stopNow).toHaveBeenCalledOnce();

    refuseStopNow(new Error('already stopping'));
    expect(await screen.findByTestId('draining-failure')).toHaveTextContent(
      'The stop now could not be started: Action failed.',
    );
  });

  it('lets go of a hung withdrawal, saying so and re-enabling both drain acts', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    const status = vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['build of fix-relay'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    const withdraw = vi
      .spyOn(apiClient, 'withdrawStop')
      .mockRejectedValue(new DOMException('The operation timed out.', 'TimeoutError'));

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    const withdrawAct = within(banner).getByRole('button', { name: 'Withdraw the stop' });
    const stopAct = within(banner).getByRole('button', { name: 'Stop now' });

    await fireEvent.click(withdrawAct);

    // The client's own request limit bounds the act, so the view passes no deadline of its own.
    expect(withdraw).toHaveBeenCalledWith();
    expect(await screen.findByTestId('draining-failure')).toHaveTextContent(
      'The withdrawal was sent but not confirmed; check the runtime state.',
    );
    await waitFor(() => {
      expect(withdrawAct).toBeEnabled();
      expect(stopAct).toBeEnabled();
    });
    // The withdrawal may have arrived: what the runtime does is read again, within its own bound.
    await waitFor(() =>
      expect(status.mock.calls.some((call) => call[0] instanceof AbortSignal)).toBe(true),
    );
  });

  it('shows the unknown withdraw notice on timeout or connection loss in en', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['build of fix-relay'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    vi.spyOn(apiClient, 'withdrawStop').mockRejectedValue(
      new DOMException('The operation timed out.', 'TimeoutError'),
    );

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    await fireEvent.click(within(banner).getByRole('button', { name: 'Withdraw the stop' }));

    expect(await screen.findByTestId('draining-failure')).toHaveTextContent(
      'The withdrawal was sent but not confirmed; check the runtime state.',
    );
  });

  it('shows the unknown withdraw notice on timeout or connection loss in de', async () => {
    localStorage.setItem('pm_locale', 'de');
    vi.spyOn(apiClient, 'getMessages').mockImplementation(async (locale) =>
      loadMessages(locale ?? 'de'),
    );
    messagesStore.set(loadMessages('de'));
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['build of fix-relay'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    vi.spyOn(apiClient, 'withdrawStop').mockRejectedValue(
      new DOMException('The operation timed out.', 'TimeoutError'),
    );

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    await fireEvent.click(within(banner).getByRole('button', { name: 'Ziehe den Stopp zurück' }));

    expect(await screen.findByTestId('draining-failure')).toHaveTextContent(
      'Die Rücknahme wurde gesendet, aber nicht bestätigt; prüfe den Zustand der Runtime.',
    );
  });

  it('shows the unknown withdraw notice when connection is offline', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['build of fix-relay'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    vi.spyOn(apiClient, 'withdrawStop').mockRejectedValue(new Error('network_offline'));

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    await fireEvent.click(within(banner).getByRole('button', { name: 'Withdraw the stop' }));

    expect(await screen.findByTestId('draining-failure')).toHaveTextContent(
      'The withdrawal was sent but not confirmed; check the runtime state.',
    );
  });

  it('does not let a stale refresh snapshot overwrite a later successful withdraw', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    let resolveStaleStatus: (value: StatusResponse) => void = () => {};
    const staleStatusPromise = new Promise<StatusResponse>((resolve) => {
      resolveStaleStatus = resolve;
    });

    vi.spyOn(apiClient, 'getStatus').mockImplementation((signal) => {
      if (signal instanceof AbortSignal) {
        return staleStatusPromise;
      }
      return Promise.resolve({
        ...STATUS,
        lifecycle_state: 'draining',
        draining_by: 'the console',
        drain_remaining_items: ['build of fix-relay'],
      });
    });

    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );

    vi.spyOn(apiClient, 'withdrawStop')
      .mockRejectedValueOnce(new DOMException('The operation timed out.', 'TimeoutError'))
      .mockResolvedValueOnce(undefined);

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    const withdrawBtn = within(banner).getByRole('button', { name: 'Withdraw the stop' });

    // First attempt times out -> outcomeUnknown -> starts refreshLifecycle() with staleStatusPromise
    await fireEvent.click(withdrawBtn);
    expect(await screen.findByTestId('draining-failure')).toBeInTheDocument();
    await waitFor(() => {
      expect(withdrawBtn).toBeEnabled();
    });

    // Second attempt succeeds -> should advance epoch and clear draining in store
    await fireEvent.click(withdrawBtn);
    await waitFor(() => {
      expect(get(runtimeStore).isDraining).toBe(false);
    });

    // The stale refresh read finally resolves with draining state
    resolveStaleStatus({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['build of fix-relay'],
    });

    // Allow promise microtasks to process
    await Promise.resolve();
    await Promise.resolve();

    // Assert: stale snapshot did not re-enable draining
    expect(get(runtimeStore).isDraining).toBe(false);
    expect(screen.queryByTestId('draining-banner')).not.toBeInTheDocument();
  });

  it('treats a lost stop-now response as an unknown outcome and reads the state again', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    const status = vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['build of fix-relay'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    vi.spyOn(apiClient, 'stopRuntime').mockRejectedValue(new Error('network_offline'));

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    await screen.findByTestId('header-machine');
    // No read of the status carries a bound of its own before the act.
    expect(status.mock.calls.some((call) => call[0] instanceof AbortSignal)).toBe(false);

    await fireEvent.click(within(banner).getByRole('button', { name: 'Stop now' }));
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));

    // A response that never arrived is not a refusal: the act ends as an unknown outcome.
    expect(await screen.findByTestId('draining-failure')).toHaveTextContent(
      'The stop now was sent but not confirmed; check the runtime state.',
    );
    // And the state is read again, within a bound of its own.
    await waitFor(() =>
      expect(status.mock.calls.some((call) => call[0] instanceof AbortSignal)).toBe(true),
    );
  });

  it('ends a stop now that timed out with the same unknown notice', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['build of fix-relay'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    vi.spyOn(apiClient, 'stopRuntime').mockRejectedValue(
      new DOMException('The operation timed out.', 'TimeoutError'),
    );

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    await fireEvent.click(within(banner).getByRole('button', { name: 'Stop now' }));
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));

    expect(await screen.findByTestId('draining-failure')).toHaveTextContent(
      'The stop now was sent but not confirmed; check the runtime state.',
    );
  });

  it('ignores a second stop-now confirm through the dialog’s busy prop while the act runs', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      ...STATUS,
      lifecycle_state: 'draining',
      draining_by: 'the console',
      drain_remaining_items: ['build of fix-relay'],
    });
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    const stopNow = vi
      .spyOn(apiClient, 'stopRuntime')
      .mockReturnValue(new Promise<{ lifecycle_state: string }>(() => {}));

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    await fireEvent.click(within(banner).getByRole('button', { name: 'Stop now' }));
    const confirm = screen.getByTestId('confirm-dialog-confirm');
    await fireEvent.click(confirm);
    expect(stopNow).toHaveBeenCalledOnce();

    // In jsdom, a dispatched click event reaches the element's handler. ConfirmDialog
    // ignores it because busy is true; the view's own handler is not reached a second time.
    confirm.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));

    // The stop is sent once: ConfirmDialog's busy prop keeps the request single while
    // the action is in flight.
    expect(stopNow).toHaveBeenCalledOnce();
  });

  it('shrinks the drain list when the runtime reports one item less', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue(STATUS);
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isDraining: true,
        drainInfo: {
          who: 'the console',
          count: 3,
          items: ['execute task of fix-relay', 'build of fix-relay', 'release of fix-relay'],
        },
      }),
    );

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    expect(banner).toHaveTextContent('3 left: execute task of fix-relay, build of fix-relay');

    // The runtime reports the work that ended, through the event the shell listens to.
    handleRuntimeEvent('runtime.draining', {
      draining_by: 'the console',
      remaining_items: ['execute task of fix-relay', 'build of fix-relay'],
    });

    await waitFor(() => {
      expect(screen.getByTestId('draining-banner')).toHaveTextContent(
        '2 left: execute task of fix-relay, build of fix-relay',
      );
    });
    expect(within(screen.getByTestId('draining-list')).getAllByRole('listitem')).toHaveLength(2);
  });

  it('applies what the refresh read, including the daemon’s own state key', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    // The mount's reads carry no signal; the refresh does, and it reports the stop.
    vi.spyOn(apiClient, 'getStatus').mockImplementation((signal?: AbortSignal) =>
      Promise.resolve(
        signal
          ? ({ ...STATUS, state: 'stopped' } as never)
          : ({ ...STATUS, lifecycle_state: 'draining' } as never),
      ),
    );
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    vi.spyOn(apiClient, 'stopRuntime').mockRejectedValue(new Error('network_offline'));

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    // The shell is initialized before the act: its own mount read must not land afterwards.
    await screen.findByTestId('header-machine');
    await fireEvent.click(within(banner).getByRole('button', { name: 'Stop now' }));
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));

    await waitFor(() => expect(get(runtimeStore).isStopped).toBe(true));
    expect(get(runtimeStore)).toMatchObject({ isDraining: false, isStopped: true });
    expect(screen.queryByTestId('draining-banner')).not.toBeInTheDocument();
  });

  it('lets a lifecycle event that arrives during the refresh win over the older answer', async () => {
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    let settleRead: (status: unknown) => void = () => {};
    const pendingRead = new Promise<never>((resolve) => {
      settleRead = resolve as (status: unknown) => void;
    });
    vi.spyOn(apiClient, 'getStatus').mockImplementation((signal?: AbortSignal) =>
      signal
        ? (pendingRead as never)
        : Promise.resolve({ ...STATUS, lifecycle_state: 'draining' } as never),
    );
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isDraining: true,
        drainInfo: { who: 'the console', count: 1, items: ['build of fix-relay'] },
      }),
    );
    vi.spyOn(apiClient, 'stopRuntime').mockRejectedValue(new Error('network_offline'));

    render(Layout);
    const banner = await screen.findByTestId('draining-banner');
    await screen.findByTestId('header-machine');
    await fireEvent.click(within(banner).getByRole('button', { name: 'Stop now' }));
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));
    await screen.findByTestId('draining-failure');

    // The runtime withdraws the drain while the refresh is in flight, then the older answer lands.
    handleRuntimeEvent('runtime.drain_withdrawn', {});
    settleRead({ ...STATUS, lifecycle_state: 'draining' });
    // Let the refresh continuation run, so a stale snapshot would already have been applied.
    await pendingRead;
    await Promise.resolve();
    expect(get(runtimeStore).isDraining).toBe(false);
  });

  it('keeps the count the runtime reports when it names no items', async () => {
    runtimeStore.set(makeRuntimeState({ isDraining: false }));

    handleRuntimeEvent('runtime.draining', {
      draining_by: 'the console',
      remaining_items: [],
      count: 2,
    });

    // An empty list says nothing: the count the runtime reported beside it stands.
    expect(get(runtimeStore).drainInfo?.count).toBe(2);
  });

  it('probes the connection again after 30 seconds and reports the machine online again', async () => {
    // Only the interval is faked: promise callbacks and `waitFor` keep real time, so the probe's
    // completion and the store update it triggers are observed without clock gymnastics.
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const originalFetch = globalThis.fetch;
    try {
      localStorage.setItem('pm_device_secret', 'pmw_valid');
      vi.spyOn(apiClient, 'getStatus').mockResolvedValue(STATUS);
      globalThis.fetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({
          loopback_url: 'http://127.0.0.1:7420',
          loopback_open: true,
          lan_enabled: false,
          paired_devices_count: 1,
          onboarded: true,
        }),
      });
      // The spy calls through: the real request runs and its success reports the machine online.
      const probe = vi.spyOn(apiClient, 'getConnectionStatus');

      render(Layout);
      await waitFor(() => expect(probe).toHaveBeenCalledTimes(1));

      // The machine was reported offline; the next heartbeat reaches it again and clears the flag
      // the requests keep, which the live indicator no longer needs to paint a banner for.
      runtimeStore.update((state) => ({ ...state, isOffline: true }));
      expect(get(runtimeStore).isOffline).toBe(true);
      vi.advanceTimersByTime(30_000);

      await waitFor(() => expect(probe).toHaveBeenCalledTimes(2));
      await waitFor(() => expect(get(runtimeStore).isOffline).toBe(false));
    } finally {
      globalThis.fetch = originalFetch;
      vi.useRealTimers();
    }
  });
});

/**
 * The runtime that is not reachable (entry.adoc, After Pairing): the shell shows its page with the
 * held `searching` figure over the open view, which stays mounted but hidden, so the view and its
 * unsent input are there again once the runtime is back. The listener turned off keeps its own page.
 */
describe('the runtime that is not reachable', () => {
  /** The open view the route renders, standing in for any view with input of its own. */
  const openView = createRawSnippet(() => ({
    render: () => '<input data-testid="open-view-input" aria-label="Unsent input" />',
  }));

  /**
   * Generated input: an object URL of the kind the app holds for the disconnected states.
   *
   * @returns a `blob:` URL with a random id
   */
  function generatedObjectUrl(): string {
    return `blob:http://localhost/${crypto.randomUUID()}`;
  }

  /**
   * Renders the shell of a paired, onboarded browser with the open view, and waits until it has
   * started and reached its origin.
   */
  async function renderStartedShell(): Promise<void> {
    const probe = vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
    render(Layout, { props: { children: openView } });
    await screen.findByTestId('open-view-input');
    await waitFor(() => expect(probe).toHaveBeenCalled());
  }

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    runtimeStore.set(
      makeRuntimeState({ isInitialized: false, isPaired: false, currentRoute: 'pair' }),
    );
    window.history.replaceState(null, '', '/reports');
    held.set('');
    vi.mocked(holdFigure).mockClear();
    vi.spyOn(apiClient, 'getMessages').mockResolvedValue(loadMessages('en'));
    messagesStore.set(loadMessages('en'));
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue(STATUS);
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });
    vi.spyOn(apiClient, 'getReports').mockResolvedValue(REPORTS);
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  });

  it('starts the one fetch of the held figure once the app has started and reaches its origin', async () => {
    await renderStartedShell();

    await waitFor(() => expect(holdFigure).toHaveBeenCalled());
  });

  it('shows the unreachable page with the searching figure at 180 px from the held URL', async () => {
    const objectUrl = generatedObjectUrl();
    held.set(objectUrl);
    await renderStartedShell();

    runtimeStore.update((state) => ({ ...state, isOffline: true }));

    const page = await screen.findByTestId('unreachable-view');
    const figure = within(page).getByTestId('mascot-figure');
    expect(figure).toHaveAttribute('data-pose', 'searching');
    expect(figure).toHaveAttribute('height', '180');
    expect(figure).toHaveAttribute('src', objectUrl);
    expect(within(page).getByRole('heading', { level: 1 })).toHaveTextContent(
      'plan-marshall is not reachable on this machine',
    );
  });

  it('shows the unreachable page as text alone while no figure is held', async () => {
    await renderStartedShell();

    runtimeStore.update((state) => ({ ...state, isOffline: true }));

    const page = await screen.findByTestId('unreachable-view');
    expect(within(page).queryByTestId('mascot-figure')).toBeNull();
    expect(page).toHaveTextContent(
      'Start it in a terminal: pm-operator. This page reconnects on its own.',
    );
  });

  it('keeps the listener-off page ahead of the unreachable page', async () => {
    held.set(generatedObjectUrl());
    await renderStartedShell();

    runtimeStore.update((state) => ({
      ...state,
      isOffline: true,
      isListenerClosed: true,
      currentRoute: 'listener-off',
    }));

    expect(await screen.findByTestId('listener-off-view')).toBeInTheDocument();
    expect(screen.queryByTestId('unreachable-view')).not.toBeInTheDocument();
  });

  it('shows the same open view with its unsent input again after the runtime is back', async () => {
    await renderStartedShell();
    const input = screen.getByTestId('open-view-input') as HTMLInputElement;
    const unsent = crypto.randomUUID();
    await fireEvent.input(input, { target: { value: unsent } });

    runtimeStore.update((state) => ({ ...state, isOffline: true }));

    await screen.findByTestId('unreachable-view');
    // The view is still in the document, hidden behind the page rather than replaced by it.
    expect(screen.getByTestId('open-view')).toHaveAttribute('hidden');
    expect(screen.getByTestId('open-view-input')).toBe(input);

    runtimeStore.update((state) => ({ ...state, isOffline: false }));

    await waitFor(() => expect(screen.queryByTestId('unreachable-view')).not.toBeInTheDocument());
    expect(screen.getByTestId('open-view')).not.toHaveAttribute('hidden');
    expect(screen.getByTestId('open-view-input')).toBe(input);
    expect(input.value).toBe(unsent);
  });
});

/**
 * The recovery after a failed start: the shell, not the page of the unreachable runtime, owns the
 * retry of the initialization. A successful request that clears the offline state removes that
 * page and its timer, and the retry still loads the runtime's state and opens the event stream.
 */
describe('the recovery of a failed start', () => {
  /** The open view the route renders. */
  const openView = createRawSnippet(() => ({
    render: () => '<p data-testid="open-view-content">open view</p>',
  }));

  beforeEach(() => {
    // The spies of the tests above are counted afresh, so a call is this test's own.
    vi.restoreAllMocks();
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'pmw_valid');
    // The machine is onboarded, but this browser has not loaded that yet.
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: false,
        isPaired: false,
        isOnboarded: false,
        currentRoute: 'pair',
      }),
    );
    window.history.replaceState(null, '', '/reports');
    held.set('');
    vi.spyOn(apiClient, 'getMessages').mockResolvedValue(loadMessages('en'));
    messagesStore.set(loadMessages('en'));
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({ projects: [] });
    vi.spyOn(apiClient, 'getReports').mockResolvedValue(REPORTS);
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('loads the runtime and opens the stream although a probe removed the unreachable page first', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    let reachable = false;
    vi.spyOn(apiClient, 'getStatus').mockImplementation(async () => {
      if (!reachable) throw new ClientError('network_offline');
      return STATUS;
    });
    // The stream itself is retried by the live indicator; only a successful initialization opens
    // it through the store and then reads the counts, which is what this test observes.
    const subscribe = vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    const decisions = vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);

    render(Layout, { props: { children: openView } });
    await screen.findByTestId('unreachable-view');
    expect(decisions).not.toHaveBeenCalled();
    expect(get(runtimeStore).isOnboarded).toBe(false);
    expect(isRuntimeRecovering()).toBe(true);

    // The runtime comes back, and a successful request clears the offline state before the first
    // retry tick; the page and its own timer go with it.
    reachable = true;
    subscribe.mockClear();
    runtimeStore.update((state) => ({ ...state, isOffline: false }));
    await waitFor(() => expect(screen.queryByTestId('unreachable-view')).not.toBeInTheDocument());
    await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS);

    await waitFor(() => expect(decisions).toHaveBeenCalled());
    expect(subscribe).toHaveBeenCalled();
    expect(get(runtimeStore).isOnboarded).toBe(true);
    // The loaded state shows the open view, not the onboarding of a machine already onboarded.
    expect(await screen.findByTestId('open-view-content')).toBeInTheDocument();
    expect(isRuntimeRecovering()).toBe(false);
  });

  it('ends the retry with the shell', async () => {
    vi.spyOn(apiClient, 'getStatus').mockRejectedValue(new ClientError('network_offline'));
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});

    const { unmount } = render(Layout, { props: { children: openView } });
    await screen.findByTestId('unreachable-view');
    expect(isRuntimeRecovering()).toBe(true);

    unmount();

    expect(isRuntimeRecovering()).toBe(false);
  });

  it('starts no retry for a start that fails after the shell is gone', async () => {
    let fail!: (err: Error) => void;
    const answer = new Promise<StatusResponse>((_resolve, reject) => {
      fail = reject;
    });
    // The catalogue's read of the status fails at once, so the shell goes on to its start; the
    // start's own read waits for the answer.
    const status = vi
      .spyOn(apiClient, 'getStatus')
      .mockRejectedValueOnce(new ClientError('network_offline'))
      .mockReturnValue(answer);
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    // The start registers its stream handler first, so a status read after it is the start's own.
    const started = vi.spyOn(apiClient, 'setOnStreamState');

    const { unmount } = render(Layout, { props: { children: openView } });
    await waitFor(() => {
      expect(started).toHaveBeenCalled();
      const startedAt = started.mock.invocationCallOrder[0];
      expect(status.mock.invocationCallOrder.some((order) => order > startedAt)).toBe(true);
    });
    // The shell goes while its start still waits for the status.
    unmount();
    fail(new ClientError('network_offline'));

    await waitFor(() => expect(get(runtimeStore).isOffline).toBe(true));
    expect(isRuntimeRecovering()).toBe(false);
  });
});
