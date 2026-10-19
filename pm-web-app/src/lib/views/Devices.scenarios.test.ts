import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import SettingsView from './SettingsView.svelte';
import ListenerOffView from './ListenerOffView.svelte';
import { runtimeStore } from '../stores/runtime';
import { apiClient, type PairedDevice } from '../api/client';

/**
 * The paired devices (UC-ENT-2) as a view test against `paired-devices` and
 * `entry-device-lifecycle`.
 */

const devices: PairedDevice[] = [
  {
    device_id: 'dev-1',
    label: 'Firefox on mbp-oliver',
    paired_from: 'loopback',
    created_at: '2026-05-12T10:00:00Z',
    last_used_at: '2026-10-16T14:02:00Z',
  },
  {
    device_id: 'dev-2',
    label: 'Safari on iPad',
    paired_from: 'lan',
    created_at: '2026-05-20T10:00:00Z',
    last_used_at: '2026-10-13T10:00:00Z',
  },
  {
    device_id: 'dev-3',
    label: 'Chrome on build-box',
    paired_from: 'loopback',
    created_at: '2026-06-02T10:00:00Z',
    last_used_at: '2026-09-08T10:00:00Z',
  },
];

function arrange(partial: Record<string, unknown> = {}): void {
  runtimeStore.set({
    isInitialized: true,
    isPaired: true,
    isOnboarded: true,
    deviceLabel: 'Firefox on mbp-oliver',
    machineName: 'mbp-oliver',
    currentRoute: 'settings',
    currentState: null,
    isRevoked: false,
    revokedReason: '',
    isOffline: false,
    isListenerClosed: false,
    completedSteps: 7,
    activeStep: 8,
    unreadReportsCount: 0,
    openDecisionsCount: 0,
    ...partial,
  });
}

describe('Paired devices against the mock runtime scenarios', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'secret123');
    localStorage.setItem('pm_device_id', 'dev-1');
    localStorage.setItem('pm_device_label', 'Firefox on mbp-oliver');
    vi.restoreAllMocks();
    arrange();

    vi.spyOn(apiClient, 'getDevices').mockResolvedValue(devices);
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      status: 'ready',
      web: { enabled: true, lan: true, port: 7420 },
    });
  });

  it('lists every device with its pairing, its last use and this one marked', async () => {
    render(SettingsView);

    await waitFor(() => expect(screen.getByTestId('devices-table')).toBeInTheDocument());
    expect(screen.getByText('Firefox on mbp-oliver')).toBeInTheDocument();
    expect(screen.getByText('Safari on iPad')).toBeInTheDocument();
    expect(screen.getByText('Chrome on build-box')).toBeInTheDocument();
    expect(screen.getByTestId('this-browser-badge')).toHaveTextContent('this one');
    expect(screen.getByTestId('listener-status')).toHaveTextContent('Network listener: on');
  });

  it('renames a device inline', async () => {
    const renameSpy = vi.spyOn(apiClient, 'renameDevice').mockResolvedValue({
      ...devices[1],
      label: 'iPad Oliver',
    });

    render(SettingsView);
    await waitFor(() => expect(screen.getByTestId('rename-btn-dev-2')).toBeInTheDocument());

    await fireEvent.click(screen.getByTestId('rename-btn-dev-2'));
    const input = screen.getByTestId('rename-input-dev-2');
    await fireEvent.input(input, { target: { value: 'iPad Oliver' } });
    await fireEvent.click(screen.getByTestId('save-rename-dev-2'));

    expect(renameSpy).toHaveBeenCalledWith('dev-2', 'iPad Oliver');
    await waitFor(() => expect(screen.getByText('iPad Oliver')).toBeInTheDocument());
  });

  it('revokes one other device after asking', async () => {
    const revokeSpy = vi.spyOn(apiClient, 'revokeDevice').mockResolvedValue();

    render(SettingsView);
    await waitFor(() => expect(screen.getByTestId('revoke-btn-dev-3')).toBeInTheDocument());

    await fireEvent.click(screen.getByTestId('revoke-btn-dev-3'));
    expect(screen.getByTestId('revoke-modal')).toHaveTextContent('Revoke Chrome on build-box?');
    await fireEvent.click(screen.getByTestId('revoke-confirm-button'));

    expect(revokeSpy).toHaveBeenCalledWith('dev-3');
    await waitFor(() => expect(screen.queryByText('Chrome on build-box')).not.toBeInTheDocument());
  });

  it('revokes all others and names the device that stays', async () => {
    const revokeOthersSpy = vi.spyOn(apiClient, 'revokeOtherDevices').mockResolvedValue();

    render(SettingsView);
    await waitFor(() => expect(screen.getByTestId('revoke-others-button')).toBeInTheDocument());

    await fireEvent.click(screen.getByTestId('revoke-others-button'));
    expect(screen.getByTestId('revoke-modal')).toHaveTextContent('2 devices other than this one');

    await fireEvent.click(screen.getByTestId('revoke-confirm-button'));
    expect(revokeOthersSpy).toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByText('Firefox on mbp-oliver')).toBeInTheDocument();
      expect(screen.queryByText('Safari on iPad')).not.toBeInTheDocument();
      expect(screen.queryByText('Chrome on build-box')).not.toBeInTheDocument();
    });
  });

  it('revoking this browser lands on the revoked page with the reason', async () => {
    vi.spyOn(apiClient, 'revokeDevice').mockResolvedValue();

    render(SettingsView);
    await waitFor(() => expect(screen.getByTestId('revoke-btn-dev-1')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('revoke-btn-dev-1'));

    expect(screen.getByTestId('revoke-modal')).toHaveTextContent('It is the browser you are using');
    await fireEvent.click(screen.getByTestId('revoke-confirm-button'));

    await waitFor(() => {
      expect(runtimeStore).toBeDefined();
    });
    expect(apiClient.getSecret()).toBeNull();
  });

  it('turns the network listener off and offers no act for it afterwards', async () => {
    const listenerSpy = vi.spyOn(apiClient, 'turnOffLanListener').mockResolvedValue();

    render(SettingsView);
    await waitFor(() => expect(screen.getByTestId('listener-off-button')).toBeInTheDocument());

    await fireEvent.click(screen.getByTestId('listener-off-button'));

    expect(listenerSpy).toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId('listener-status')).toHaveTextContent('Network listener: off');
      expect(screen.queryByTestId('listener-off-button')).not.toBeInTheDocument();
    });
    expect(screen.getByTestId('listener-notice')).toHaveTextContent(
      'The network listener is off; the devices on the network stay paired',
    );
  });
});

describe('ListenerOffView', () => {
  it('says the machine is not reachable and that the pairing stays', () => {
    arrange({
      isListenerClosed: true,
      listenerClosedAt: '2026-10-19T14:32:00Z',
      currentRoute: 'listener-off',
    });

    render(ListenerOffView);

    expect(screen.getByTestId('listener-off-view')).toHaveTextContent(
      'This machine is not reachable over the network now',
    );
    expect(screen.getByTestId('listener-off-view')).toHaveTextContent('This browser stays paired');
    expect(screen.getByTestId('listener-off-view')).toHaveTextContent('pm-operator web lan');
  });

  it('says only that the machine is unreachable when no time was reported', () => {
    arrange({ isListenerClosed: true, currentRoute: 'listener-off' });

    render(ListenerOffView);

    expect(screen.getByTestId('listener-off-view')).toHaveTextContent(
      'This machine is not reachable over the network.',
    );
  });
});
