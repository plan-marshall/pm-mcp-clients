import { describe, it, expect, vi, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import SettingsView from './SettingsView.svelte';
import { runtimeStore, navigateTo } from '../stores/runtime';
import { apiClient } from '../api/client';

describe('SettingsView component', () => {
  const mockDevices = [
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
      paired_from: 'network',
      created_at: '2026-05-20T10:00:00Z',
      last_used_at: '2026-10-13T10:00:00Z',
    },
  ];

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('pm_device_secret', 'secret123');
    localStorage.setItem('pm_device_label', 'Firefox on mbp-oliver');

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
      completedSteps: 0,
      activeStep: 1,
    });

    vi.spyOn(apiClient, 'getDevices').mockResolvedValue(mockDevices);
    vi.spyOn(apiClient, 'getStatus').mockResolvedValue({
      status: 'ready',
      web: { enabled: true, lan: true, port: 7420 },
    });
  });

  it('renders paired devices list with this browser badge and network listener status', async () => {
    render(SettingsView);

    await waitFor(() => {
      expect(screen.getByText('Firefox on mbp-oliver')).toBeInTheDocument();
      expect(screen.getByText('Safari on iPad')).toBeInTheDocument();
    });

    expect(screen.getByTestId('this-browser-badge')).toHaveTextContent('this one');
    expect(screen.getByTestId('listener-status')).toHaveTextContent('Network listener: on.');
    expect(screen.getByTestId('revoke-others-button')).toBeInTheDocument();
  });

  it('allows renaming a device inline and saves the new label', async () => {
    const renameSpy = vi.spyOn(apiClient, 'renameDevice').mockResolvedValue({
      ...mockDevices[1],
      label: 'iPad Oliver',
    });

    render(SettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('rename-btn-dev-2')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('rename-btn-dev-2'));

    const input = screen.getByTestId('rename-input-dev-2');
    expect(input).toBeInTheDocument();

    await fireEvent.input(input, { target: { value: 'iPad Oliver' } });
    await fireEvent.click(screen.getByTestId('save-rename-dev-2'));

    expect(renameSpy).toHaveBeenCalledWith('dev-2', 'iPad Oliver');
    await waitFor(() => {
      expect(screen.getByText('iPad Oliver')).toBeInTheDocument();
    });
  });

  it('shows confirmation modal when revoking another device and removes it', async () => {
    const revokeSpy = vi.spyOn(apiClient, 'revokeDevice').mockResolvedValue();

    render(SettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('revoke-btn-dev-2')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('revoke-btn-dev-2'));

    // Modal is opened
    expect(screen.getByTestId('revoke-modal')).toBeInTheDocument();
    expect(screen.getByText(/Revoke Safari on iPad\?/i)).toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('revoke-confirm-button'));

    expect(revokeSpy).toHaveBeenCalledWith('dev-2');
    await waitFor(() => {
      expect(screen.queryByText('Safari on iPad')).not.toBeInTheDocument();
    });
  });

  it('shows modal when revoking all other devices and updates list', async () => {
    const revokeOthersSpy = vi.spyOn(apiClient, 'revokeOtherDevices').mockResolvedValue();

    render(SettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('revoke-others-button')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('revoke-others-button'));

    expect(screen.getByTestId('revoke-modal')).toBeInTheDocument();
    expect(screen.getByText(/1 device other than this one/i)).toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('revoke-confirm-button'));

    expect(revokeOthersSpy).toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.queryByText('Safari on iPad')).not.toBeInTheDocument();
      expect(screen.getByText('Firefox on mbp-oliver')).toBeInTheDocument();
    });
  });

  it('turns network listener off and shows feedback message', async () => {
    const listenerSpy = vi.spyOn(apiClient, 'turnOffLanListener').mockResolvedValue();

    render(SettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('listener-off-button')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('listener-off-button'));

    expect(listenerSpy).toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId('listener-status')).toHaveTextContent('Network listener: off.');
      expect(screen.getByTestId('listener-notice')).toBeInTheDocument();
      expect(screen.queryByTestId('listener-off-button')).not.toBeInTheDocument();
    });
  });

  it('opens the network access modal and shows the credential entry section', async () => {
    vi.spyOn(apiClient, 'getConnectionStatus').mockResolvedValue({
      loopback_open: true,
      lan_enabled: true,
      lan_fingerprint: '4F:1A:9C',
      paired_devices_count: 2,
      onboarded: true,
    });

    render(SettingsView);

    await waitFor(() => expect(screen.getByTestId('lan-details-button')).toBeInTheDocument());
    expect(screen.getByTestId('credential-entry')).toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('lan-details-button'));

    await waitFor(() => expect(screen.getByTestId('lan-listener-modal')).toBeInTheDocument());
    expect(screen.getByTestId('lan-fingerprint')).toHaveTextContent('4F:1A:9C');
  });

  it('returns to the route the caller came from on Back', async () => {
    runtimeStore.set({
      isInitialized: true,
      isPaired: true,
      isOnboarded: true,
      deviceLabel: 'Firefox on mbp-oliver',
      machineName: 'mbp-oliver',
      currentRoute: 'projects',
      currentState: null,
      isRevoked: false,
      revokedReason: '',
      isOffline: false,
      isListenerClosed: false,
      completedSteps: 0,
      activeStep: 1,
    });
    navigateTo('settings');

    render(SettingsView);

    await waitFor(() => expect(screen.getByTestId('settings-back-button')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('settings-back-button'));

    expect(get(runtimeStore).currentRoute).toBe('projects');
  });
});
