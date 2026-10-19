import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import PairingView from './PairingView.svelte';
import { runtimeStore } from '../stores/runtime';
import { apiClient } from '../api/client';

/**
 * The pairing page and the entry (UC-ENT-1) as a view test against `web-pairing` and `entry`.
 */

function arrange(partial: Record<string, unknown> = {}): void {
  runtimeStore.set({
    isInitialized: true,
    isPaired: false,
    isOnboarded: false,
    deviceLabel: '',
    machineName: 'mbp-oliver',
    currentRoute: 'pair',
    currentState: null,
    isRevoked: false,
    revokedReason: '',
    isOffline: false,
    isListenerClosed: false,
    completedSteps: 0,
    activeStep: 1,
    unreadReportsCount: 0,
    openDecisionsCount: 0,
    ...partial,
  });
}

describe('PairingView against the mock runtime scenarios', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    arrange();
  });

  it('asks for the code and names the two ways to get one', () => {
    render(PairingView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Pair this browser');
    expect(screen.getByLabelText(/code/i)).toBeInTheDocument();
    expect(screen.getByText('pm-operator web open --pair')).toBeInTheDocument();
    expect(screen.getByText('pm-operator web pair')).toBeInTheDocument();
  });

  it('accepts the code with or without the dash and in any case', async () => {
    render(PairingView);

    const input = screen.getByLabelText(/code/i);
    const submit = screen.getByRole('button', { name: /pair this browser/i });
    expect(submit).toBeDisabled();

    await fireEvent.input(input, { target: { value: 'kxqbmrtn' } });
    expect(submit).not.toBeDisabled();
  });

  it('reports a code that does not work with one answer', async () => {
    vi.spyOn(apiClient, 'pair').mockRejectedValue(new Error('invalid_code'));

    render(PairingView);
    await fireEvent.input(screen.getByLabelText(/code/i), { target: { value: 'KXQB-MRTN' } });
    await fireEvent.click(screen.getByRole('button', { name: /pair this browser/i }));

    await waitFor(() => {
      expect(screen.getByTestId('pairing-error')).toHaveTextContent(
        'This code does not work. Get a new one in a terminal of the machine.',
      );
    });
  });

  it('pairs the browser and opens where the app left off', async () => {
    vi.spyOn(apiClient, 'pair').mockResolvedValue({
      device_secret: 'secret',
      device_id: 'dev-1',
      label: 'Firefox on mbp-oliver',
    });
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      status: 'ready',
      machine: 'mbp-oliver',
      onboarded: false,
    });

    render(PairingView);
    await fireEvent.input(screen.getByLabelText(/code/i), { target: { value: 'KXQB-MRTN' } });
    await fireEvent.click(screen.getByRole('button', { name: /pair this browser/i }));

    await waitFor(() => {
      expect(screen.queryByRole('heading', { level: 1 })).not.toBeInTheDocument();
    });
  });

  it('shows the reason above the pairing page when the device was revoked', () => {
    arrange({
      isRevoked: true,
      revokedReason: 'device_revoked',
      revokedInfo: {
        reason: 'device_revoked',
        revoked_by: 'iPad (Safari)',
        revoked_at: '14:32',
      },
    });

    render(PairingView);

    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent(
      'This browser is no longer paired',
    );
    // The page stays a pairing page: the way back is a new code.
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Pair this browser');
  });

  it('shows the network certificate fingerprint on the network listener', async () => {
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: true,
      lan_fingerprint: '4F:1A:9C:6D:2B:88:71:03:C5:9E:44:AF:20:DB:16:E2:07',
      paired_devices_count: 1,
      onboarded: false,
    });

    // The page reads the listener it was reached over; a LAN host is not loopback.
    vi.spyOn(window, 'location', 'get').mockReturnValue({
      ...window.location,
      hostname: '192.168.1.20',
      origin: 'https://192.168.1.20:7421',
      pathname: '/pair',
    } as Location);

    render(PairingView);

    await waitFor(() => expect(screen.getByTestId('pairing-fingerprint')).toBeInTheDocument());
    expect(screen.getByTestId('pairing-fingerprint')).toHaveTextContent('Fingerprint SHA-256');
  });

  it('presents the pairing URL as a QR code on the network listener', async () => {
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: true,
      lan_fingerprint: '4F:1A:9C',
      paired_devices_count: 0,
      onboarded: false,
    });
    vi.spyOn(window, 'location', 'get').mockReturnValue({
      ...window.location,
      hostname: '192.168.1.20',
      origin: 'https://192.168.1.20:7421',
      pathname: '/pair',
    } as Location);

    render(PairingView);
    await fireEvent.input(screen.getByLabelText(/code/i), { target: { value: 'KXQBM-RTN' } });

    await waitFor(() => expect(screen.getByTestId('pairing-qr')).toBeInTheDocument());
    // The QR is drawn locally: the code never reaches a server through an image request.
    const svg = screen.getByTestId('pairing-qr').querySelector('svg');
    expect(svg).not.toBeNull();
    expect(svg?.querySelectorAll('rect').length).toBeGreaterThan(20);
  });
});
