import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import { get } from 'svelte/store';
import Layout from './+layout.svelte';
import { apiClient, type ReportsPage, type StatusResponse } from '../lib/api/client';
import { runtimeStore } from '../lib/stores/runtime';
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

    // An icon-only control still explains what it does.
    expect(screen.getByLabelText('Toggle light/dark theme')).toHaveAttribute(
      'title',
      'Switch between the light and dark theme',
    );
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

  it('marks the LAN mode and keeps the detail when the connection status fails', async () => {
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

  it('probes the connection again after 30 seconds and clears the offline banner', async () => {
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

      // The machine was reported offline; the next heartbeat reaches it again.
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
