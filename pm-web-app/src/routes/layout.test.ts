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

  it('gives the header actions a hover advisory', async () => {
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

    expect(screen.getByTestId('brand-home')).toHaveAttribute(
      'title',
      'Open the overview or the projects, wherever you usually start',
    );
    expect(screen.getByTestId('nav-projects-btn')).toHaveAttribute('title', 'Projects');
    expect(screen.getByTestId('nav-designer-btn')).toHaveAttribute('title', 'Process Designer');
  });

  it('leaves the brand inert while the browser is unpaired', async () => {
    render(Layout);

    await screen.findByTestId('brand-inert');
    expect(screen.queryByTestId('brand-home')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nav-designer-btn')).not.toBeInTheDocument();
  });
});
