import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/svelte';
import ConnectionStatusBanner from './ConnectionStatusBanner.svelte';
import { apiClient } from '../api/client';

describe('ConnectionStatusBanner', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  });

  it('shows the loopback mode with the paired device count', async () => {
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_url: 'http://127.0.0.1:7420',
      loopback_open: true,
      lan_enabled: false,
      paired_devices_count: 1,
      onboarded: true,
    });

    render(ConnectionStatusBanner);

    await waitFor(() => expect(screen.getByTestId('connection-status-banner')).toBeInTheDocument());

    const banner = screen.getByTestId('connection-status-banner');
    expect(banner).toHaveAttribute('data-mode', 'loopback');
    expect(banner).toHaveTextContent('Connected over loopback');
    expect(banner).toHaveTextContent('1 paired device');
  });

  it('renders nothing when the connection status cannot be loaded', async () => {
    vi.spyOn(apiClient, 'getConnectionStatus').mockRejectedValue(new Error('unauthorized'));

    render(ConnectionStatusBanner);

    await waitFor(() => expect(apiClient.getConnectionStatus).toHaveBeenCalled());
    expect(screen.queryByTestId('connection-status-banner')).not.toBeInTheDocument();
  });
});
