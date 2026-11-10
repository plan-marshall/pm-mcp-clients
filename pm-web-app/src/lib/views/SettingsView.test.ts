import { describe, it, expect, vi, beforeEach } from 'vitest';
import { tick } from 'svelte';
import { get } from 'svelte/store';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import SettingsView from './SettingsView.svelte';
import { runtimeStore, navigateTo } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { apiClient } from '../api/client';
import { ClientError } from '../api/errors';

/**
 * @param id the device id
 * @returns the row the device draws its acts in
 */
function deviceRow(id: string): HTMLElement {
  return screen.getByTestId(`device-row-${id}`);
}

/**
 * Lets every microtask a read's chain needs run: the promise reactions of `Promise.all`, the
 * component's continuation, and Svelte's own flush.
 */
async function settleMicrotasks(): Promise<void> {
  for (let step = 0; step < 6; step += 1) {
    await Promise.resolve();
  }
  await tick();
}

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

    runtimeStore.set(
      makeRuntimeState({
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
      }),
    );

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
    expect(screen.getByTestId('devices-section-acts')).toBeInTheDocument();
  });

  it('allows renaming a device inline and saves the new label', async () => {
    const renameSpy = vi.spyOn(apiClient, 'renameDevice').mockResolvedValue({
      ...mockDevices[1],
      label: 'iPad Oliver',
    });

    render(SettingsView);

    await waitFor(() => {
      expect(within(deviceRow('dev-2')).getByTestId('action-rename')).toBeInTheDocument();
    });

    await fireEvent.click(within(deviceRow('dev-2')).getByTestId('action-rename'));

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
      expect(within(deviceRow('dev-2')).getByTestId('action-bar-overflow')).toBeInTheDocument();
    });

    await fireEvent.click(within(deviceRow('dev-2')).getByTestId('action-bar-overflow'));
    await fireEvent.click(await within(deviceRow('dev-2')).findByTestId('action-revoke'));

    // The confirmation names the consequence, in the danger colour
    expect(await screen.findByTestId('confirm-dialog')).toBeInTheDocument();
    expect(screen.getByText(/Revoke Safari on iPad\?/i)).toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));

    expect(revokeSpy).toHaveBeenCalledWith('dev-2');
    await waitFor(() => {
      expect(screen.queryByText('Safari on iPad')).not.toBeInTheDocument();
    });
  });

  it('holds the revoke confirmation open and ignores a second confirm while it runs', async () => {
    let settle: () => void = () => {};
    const pending = new Promise<void>((resolve) => {
      settle = resolve;
    });
    const revokeSpy = vi.spyOn(apiClient, 'revokeDevice').mockReturnValue(pending);

    render(SettingsView);

    await waitFor(() => {
      expect(within(deviceRow('dev-2')).getByTestId('action-bar-overflow')).toBeInTheDocument();
    });
    await fireEvent.click(within(deviceRow('dev-2')).getByTestId('action-bar-overflow'));
    await fireEvent.click(await within(deviceRow('dev-2')).findByTestId('action-revoke'));

    const confirm = await screen.findByTestId('confirm-dialog-confirm');
    await fireEvent.click(confirm);
    expect(revokeSpy).toHaveBeenCalledOnce();

    await fireEvent.click(confirm);
    expect(revokeSpy).toHaveBeenCalledOnce();
    expect(screen.getByTestId('confirm-dialog')).toBeInTheDocument();
    expect(screen.getByTestId('confirm-dialog-cancel')).toBeDisabled();

    await fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.getByTestId('confirm-dialog')).toBeInTheDocument();

    settle();
    await waitFor(() => expect(screen.queryByTestId('confirm-dialog')).not.toBeInTheDocument());
  });

  it('ignores a second confirm through the dialog’s busy prop while revoke runs', async () => {
    // Arrange: the revoke runs, so the confirm button is disabled while the guard is what refuses.
    let settle: () => void = () => {};
    const pending = new Promise<void>((resolve) => {
      settle = resolve;
    });
    const revokeSpy = vi.spyOn(apiClient, 'revokeDevice').mockReturnValue(pending);

    render(SettingsView);
    await waitFor(() => {
      expect(within(deviceRow('dev-2')).getByTestId('action-bar-overflow')).toBeInTheDocument();
    });
    await fireEvent.click(within(deviceRow('dev-2')).getByTestId('action-bar-overflow'));
    await fireEvent.click(await within(deviceRow('dev-2')).findByTestId('action-revoke'));

    const confirm = await screen.findByTestId('confirm-dialog-confirm');
    await fireEvent.click(confirm);
    expect(revokeSpy).toHaveBeenCalledOnce();

    // In jsdom, a dispatched click event reaches the element's handler. ConfirmDialog
    // ignores it because busy is true; the view's own handler is not reached a second time.
    confirm.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));

    // The revoke is sent once: ConfirmDialog's busy prop keeps the request single while
    // revoke is in flight.
    expect(revokeSpy).toHaveBeenCalledOnce();
    settle();
  });

  it('asks for the typed name before revoking all other devices', async () => {
    const revokeOthersSpy = vi.spyOn(apiClient, 'revokeOtherDevices').mockResolvedValue();

    render(SettingsView);

    await waitFor(() => {
      expect(screen.getByTestId('devices-section-acts')).toBeInTheDocument();
    });

    await fireEvent.click(
      within(screen.getByTestId('devices-section-acts')).getByTestId('action-bar-overflow'),
    );
    await fireEvent.click(
      await within(screen.getByTestId('devices-section-acts')).findByTestId('action-revoke_others'),
    );

    expect(await screen.findByTestId('confirm-dialog')).toBeInTheDocument();
    expect(screen.getByText(/1 device other than this one/i)).toBeInTheDocument();

    const confirm = screen.getByTestId('confirm-dialog-confirm');
    expect(confirm).toBeDisabled();

    await fireEvent.input(screen.getByTestId('confirm-dialog-input'), {
      target: { value: 'Firefox on mbp-oliver' },
    });
    expect(confirm).toBeEnabled();
    await fireEvent.click(confirm);

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

  it('ends a devices read that times out in the error state instead of waiting for it', async () => {
    // Arrange: the read carries the view's own abort controller, and answers with the failure a
    // client-level timeout produces.
    const signals: (AbortSignal | undefined)[] = [];
    vi.spyOn(apiClient, 'getDevices').mockImplementation((signal?: AbortSignal) => {
      signals.push(signal);
      return Promise.reject(new ClientError('timeout'));
    });

    // Act
    render(SettingsView);

    // Assert: the supersede signal is carried into the read, and its failure ends in the error state.
    expect(await screen.findByText('The request took too long')).toBeInTheDocument();
    expect(signals[0]).toBeInstanceOf(AbortSignal);
  });

  it('keeps the newer list when an older read answers after it', async () => {
    // Arrange: the first read answers; the reads the two timed-out revokes start stay open.
    const newerDevice = {
      device_id: 'dev-3',
      label: 'Chrome on build-box',
      paired_from: 'network',
      created_at: '2026-06-01T10:00:00Z',
      last_used_at: '2026-10-20T09:00:00Z',
    };
    const pending: Array<(rows: typeof mockDevices) => void> = [];
    vi.spyOn(apiClient, 'getDevices')
      .mockResolvedValueOnce(mockDevices)
      .mockImplementation(
        () =>
          new Promise<typeof mockDevices>((resolve) => {
            pending.push(resolve);
          }),
      );
    vi.spyOn(apiClient, 'revokeDevice').mockRejectedValue(new ClientError('timeout'));

    render(SettingsView);
    await waitFor(() => expect(within(deviceRow('dev-2')).getByTestId('action-bar-overflow')));

    // The first timed-out revoke starts the older read.
    await fireEvent.click(within(deviceRow('dev-2')).getByTestId('action-bar-overflow'));
    await fireEvent.click(await within(deviceRow('dev-2')).findByTestId('action-revoke'));
    await fireEvent.click(await screen.findByTestId('confirm-dialog-confirm'));

    // The second starts the newer one.
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));
    expect(pending).toHaveLength(2);

    // Act: the newer read answers, then the older one answers late.
    pending[1]([mockDevices[0], newerDevice]);
    expect(await screen.findByText('Chrome on build-box')).toBeInTheDocument();
    pending[0]([mockDevices[0]]);

    // Assert: the late answer never replaces what the newer read brought. The older read's chain
    // runs through promise reactions and Svelte's own microtask flush, so the assertions wait for
    // that chain to reach its end instead of racing it.
    await settleMicrotasks();
    expect(screen.queryByText('Safari on iPad')).not.toBeInTheDocument();
    expect(screen.getByText('Chrome on build-box')).toBeInTheDocument();
  });

  it('re-reads the list when a revoke lost its response', async () => {
    // Arrange: the revoke never hears the runtime, so the runtime may still have applied it.
    const getDevices = vi.spyOn(apiClient, 'getDevices').mockResolvedValue(mockDevices);
    vi.spyOn(apiClient, 'revokeDevice').mockRejectedValue(new ClientError('network_offline'));

    render(SettingsView);
    await waitFor(() => expect(within(deviceRow('dev-2')).getByTestId('action-bar-overflow')));

    // Act: the revoke ends in the lost-response outcome.
    await fireEvent.click(within(deviceRow('dev-2')).getByTestId('action-bar-overflow'));
    await fireEvent.click(await within(deviceRow('dev-2')).findByTestId('action-revoke'));
    await fireEvent.click(await screen.findByTestId('confirm-dialog-confirm'));

    // Assert: the view asks the runtime what the list looks like now.
    await waitFor(() => expect(getDevices).toHaveBeenCalledTimes(2));
  });

  it('bounds the read after a timed-out revoke and ends it in the error state', async () => {
    // Arrange: the revoke gives up on its deadline, and the recovery read gives up too.
    const signals: (AbortSignal | undefined)[] = [];
    vi.spyOn(apiClient, 'revokeDevice').mockRejectedValue(new ClientError('timeout'));
    vi.spyOn(apiClient, 'getDevices').mockImplementation((signal?: AbortSignal) => {
      signals.push(signal);
      return signals.length === 1
        ? Promise.resolve(mockDevices)
        : Promise.reject(new ClientError('timeout'));
    });

    render(SettingsView);
    await waitFor(() => expect(within(deviceRow('dev-2')).getByTestId('action-bar-overflow')));

    // Act: the revoke times out and asks for the list again.
    await fireEvent.click(within(deviceRow('dev-2')).getByTestId('action-bar-overflow'));
    await fireEvent.click(await within(deviceRow('dev-2')).findByTestId('action-revoke'));
    await fireEvent.click(await screen.findByTestId('confirm-dialog-confirm'));

    // Assert: the recovery read is bounded, and its failure is the error state.
    expect(await screen.findByText('The request took too long')).toBeInTheDocument();
    expect(signals).toHaveLength(2);
    expect(signals[1]).toBeInstanceOf(AbortSignal);
    // The listener state the first read carried is not a current one any more.
    expect(screen.getByTestId('listener-status')).toHaveTextContent('Network listener: unknown.');
  });

  it('shows an unknown listener state when the status read fails', async () => {
    // Arrange: the devices answer, the status does not — so nothing says how the listener stands.
    const reads: (AbortSignal | undefined)[] = [];
    vi.spyOn(apiClient, 'getStatus').mockImplementation((signal?: AbortSignal) => {
      reads.push(signal);
      return Promise.reject(new ClientError('timeout'));
    });

    // Act
    render(SettingsView);

    // Assert: an unknown state, never "on", and no act that would act on the guess.
    expect(await screen.findByText('Firefox on mbp-oliver')).toBeInTheDocument();
    expect(screen.getByTestId('listener-status')).toHaveTextContent('Network listener: unknown.');
    expect(screen.queryByTestId('listener-off-button')).not.toBeInTheDocument();
    // The status read is bounded like the devices read beside it.
    expect(reads[0]).toBeInstanceOf(AbortSignal);
  });

  it('returns to the route the caller came from on Back', async () => {
    runtimeStore.set(
      makeRuntimeState({
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
      }),
    );
    navigateTo('settings');

    render(SettingsView);

    await waitFor(() => expect(screen.getByTestId('settings-back-button')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('settings-back-button'));

    expect(get(runtimeStore).currentRoute).toBe('projects');
  });
});
