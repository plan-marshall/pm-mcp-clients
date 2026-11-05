import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import { get } from 'svelte/store';
import Layout from './+layout.svelte';
import { apiClient, type ReportsPage, type StatusResponse } from '../lib/api/client';
import { runtimeStore, handleRuntimeEvent } from '../lib/stores/runtime';
import { makeRuntimeState } from '../lib/test/runtimeState';
import { loadMessages } from '../test/catalogue-fixture';

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
    expect(stopNow).toHaveBeenCalledWith('now', undefined, expect.anything());

    // The accepted stop ends the drain at once, even before a lifecycle event arrives.
    await waitFor(() => {
      expect(screen.queryByTestId('draining-banner')).not.toBeInTheDocument();
    });
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
    expect(await screen.findByTestId('draining-failure')).toHaveTextContent('too late');
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
    expect(await screen.findByTestId('draining-failure')).toHaveTextContent('already stopping');
  });

  it('lets go of a hung withdrawal, saying so and re-enabling both drain acts', async () => {
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

    // The request carries its own deadline, so a DELETE the runtime never answers ends.
    expect(withdraw).toHaveBeenCalledWith(expect.any(AbortSignal));
    expect(await screen.findByTestId('draining-failure')).toHaveTextContent(
      'The stop could not be withdrawn',
    );
    await waitFor(() => {
      expect(withdrawAct).toBeEnabled();
      expect(stopAct).toBeEnabled();
    });
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

  it('refuses a stop-now confirm that reaches the handler past the busy button', async () => {
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

    // A click dispatched on the disabled button still reaches the view's handler.
    confirm.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));

    // The stop is sent once: the dialog's busy prop and the view's own guard both stand against a
    // second send, and either alone must keep the request single.
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
