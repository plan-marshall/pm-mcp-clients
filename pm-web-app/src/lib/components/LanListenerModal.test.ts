import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import LanListenerModal from './LanListenerModal.svelte';
import { apiClient, type ConnectionStatus } from '../api/client';

const openStatus: ConnectionStatus = {
  loopback_open: true,
  lan_enabled: true,
  lan_url: 'https://mbp-oliver.local:7421',
  lan_fingerprint: '4F:1A:9C',
  paired_devices_count: 0,
  onboarded: true,
};

describe('LanListenerModal', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('shows the certificate fingerprint while the network listener is open', async () => {
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue(openStatus);

    render(LanListenerModal, { props: { onClose: () => {}, onChanged: () => {} } });

    await waitFor(() => expect(screen.getByTestId('lan-fingerprint')).toBeInTheDocument());
    expect(screen.getByTestId('lan-fingerprint')).toHaveTextContent('4F:1A:9C');
    expect(screen.getByTestId('lan-listener-state')).toHaveTextContent('open');
    expect(screen.getByTestId('lan-turn-off-button')).toBeInTheDocument();
  });

  it('turns the network access off and notifies the parent', async () => {
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue(openStatus);
    const turnOff = vi.spyOn(apiClient, 'turnOffLanListener').mockResolvedValue(undefined);
    const onChanged = vi.fn();

    render(LanListenerModal, { props: { onClose: () => {}, onChanged } });

    await waitFor(() => expect(screen.getByTestId('lan-turn-off-button')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('lan-turn-off-button'));

    await waitFor(() => expect(turnOff).toHaveBeenCalled());
    await waitFor(() => expect(onChanged).toHaveBeenCalled());
    expect(screen.getByTestId('lan-listener-notice')).toHaveTextContent(
      'The network access is off',
    );
  });

  it('offers no turn-off action while the network listener is closed', async () => {
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      ...openStatus,
      lan_enabled: false,
      lan_fingerprint: undefined,
    });

    render(LanListenerModal, { props: { onClose: () => {}, onChanged: () => {} } });

    await waitFor(() =>
      expect(screen.getByTestId('lan-listener-state')).toHaveTextContent('closed'),
    );
    expect(screen.queryByTestId('lan-turn-off-button')).not.toBeInTheDocument();
  });
});
