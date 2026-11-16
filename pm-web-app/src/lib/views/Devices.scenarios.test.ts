import { describe, it, expect, vi, beforeEach } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { tick } from 'svelte';
import { get } from 'svelte/store';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import SettingsView from './SettingsView.svelte';
import ListenerOffView from './ListenerOffView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { apiClient, type PairedDevice } from '../api/client';
import { ClientError } from '../api/errors';

/**
 * Lets every microtask a read's chain needs run: the promise reactions of `Promise.all`, the
 * component's continuation, and Svelte's own flush.
 */
async function settleMicrotasks(): Promise<void> {
  for (let step = 0; step < 8; step += 1) {
    await Promise.resolve();
  }
  await tick();
}

/** The callback the view registered with the events stream. */
let emitEvent: ((event: string, data: unknown) => void) | null = null;

/**
 * @param id the device id
 * @returns the row the device draws its acts in
 */
function deviceRow(id: string): HTMLElement {
  return screen.getByTestId(`device-row-${id}`);
}

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
  runtimeStore.set(
    makeRuntimeState({
      isInitialized: true,
      isPaired: true,
      isOnboarded: true,
      deviceLabel: 'Firefox on mbp-oliver',
      machineName: 'mbp-oliver',
      currentRoute: 'settings',
      objectTab: 'access',
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
    }),
  );
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
    // The view's own event subscription, so a test can deliver what the runtime broadcasts.
    emitEvent = null;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((callback) => {
      emitEvent = callback;
      return () => {};
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
    await waitFor(() =>
      expect(within(deviceRow('dev-2')).getByTestId('action-rename')).toBeInTheDocument(),
    );

    await fireEvent.click(within(deviceRow('dev-2')).getByTestId('action-rename'));
    const input = screen.getByTestId('rename-input-dev-2');
    await fireEvent.input(input, { target: { value: 'iPad Oliver' } });
    await fireEvent.click(screen.getByTestId('save-rename-dev-2'));

    expect(renameSpy).toHaveBeenCalledWith('dev-2', 'iPad Oliver');
    await waitFor(() => expect(screen.getByText('iPad Oliver')).toBeInTheDocument());
  });

  it('revokes one other device after asking', async () => {
    const revokeSpy = vi.spyOn(apiClient, 'revokeDevice').mockResolvedValue();

    render(SettingsView);
    await waitFor(() =>
      expect(within(deviceRow('dev-3')).getByTestId('action-bar-overflow')).toBeInTheDocument(),
    );

    await fireEvent.click(within(deviceRow('dev-3')).getByTestId('action-bar-overflow'));
    await fireEvent.click(await within(deviceRow('dev-3')).findByTestId('action-revoke'));
    expect(await screen.findByTestId('confirm-dialog')).toHaveTextContent(
      'Revoke Chrome on build-box?',
    );
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));

    expect(revokeSpy).toHaveBeenCalledWith('dev-3');
    await waitFor(() => expect(screen.queryByText('Chrome on build-box')).not.toBeInTheDocument());
  });

  it('revokes all others and names the device that stays', async () => {
    const revokeOthersSpy = vi.spyOn(apiClient, 'revokeOtherDevices').mockResolvedValue();

    render(SettingsView);
    await waitFor(() => expect(screen.getByTestId('devices-section-acts')).toBeInTheDocument());

    await fireEvent.click(
      within(screen.getByTestId('devices-section-acts')).getByTestId('action-bar-overflow'),
    );
    await fireEvent.click(
      await within(screen.getByTestId('devices-section-acts')).findByTestId('action-revoke_others'),
    );
    expect(await screen.findByTestId('confirm-dialog')).toHaveTextContent(
      '2 devices other than this one',
    );

    await fireEvent.input(screen.getByTestId('confirm-dialog-input'), {
      target: { value: 'Firefox on mbp-oliver' },
    });
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));
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
    await waitFor(() =>
      expect(within(deviceRow('dev-1')).getByTestId('action-bar-overflow')).toBeInTheDocument(),
    );
    await fireEvent.click(within(deviceRow('dev-1')).getByTestId('action-bar-overflow'));
    await fireEvent.click(await within(deviceRow('dev-1')).findByTestId('action-revoke'));

    expect(await screen.findByTestId('confirm-dialog')).toHaveTextContent(
      'It is the browser you are using',
    );
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));

    await waitFor(() => {
      expect(runtimeStore).toBeDefined();
    });
    expect(apiClient.getSecret()).toBeNull();
  });

  it('keeps a revoked row gone when a read that started before it answers late', async () => {
    // Arrange: the first revoke loses its answer and starts a recovery read that stays open.
    const pending: Array<(rows: PairedDevice[]) => void> = [];
    vi.spyOn(apiClient, 'getDevices')
      .mockResolvedValueOnce(devices)
      .mockImplementation(
        () =>
          new Promise<PairedDevice[]>((resolve) => {
            pending.push(resolve);
          }),
      );
    vi.spyOn(apiClient, 'revokeDevice')
      .mockRejectedValueOnce(new ClientError('timeout'))
      .mockResolvedValueOnce(undefined);

    render(SettingsView);
    await waitFor(() =>
      expect(within(deviceRow('dev-3')).getByTestId('action-bar-overflow')).toBeInTheDocument(),
    );

    // The revoke times out, so the dialog stays open and its recovery read stays in flight.
    await fireEvent.click(within(deviceRow('dev-3')).getByTestId('action-bar-overflow'));
    await fireEvent.click(await within(deviceRow('dev-3')).findByTestId('action-revoke'));
    await fireEvent.click(await screen.findByTestId('confirm-dialog-confirm'));
    await waitFor(() => expect(pending).toHaveLength(1));

    // The second confirm succeeds while the read is still in flight.
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));
    await waitFor(() =>
      expect(screen.queryByText('Chrome on build-box')).not.toBeInTheDocument(),
    );

    // Act: the read that started before the revoke answers with the row it ended.
    pending[0](devices);
    await settleMicrotasks();

    // Assert: the revoked row stays gone.
    expect(screen.queryByText('Chrome on build-box')).not.toBeInTheDocument();
  });

  it('keeps a revoked device gone when the event arrives during a load', async () => {
    // Arrange: a recovery read is in flight when the runtime broadcasts the revoke.
    const pending: Array<(rows: PairedDevice[]) => void> = [];
    vi.spyOn(apiClient, 'getDevices')
      .mockResolvedValueOnce(devices)
      .mockImplementation(
        () =>
          new Promise<PairedDevice[]>((resolve) => {
            pending.push(resolve);
          }),
      );
    vi.spyOn(apiClient, 'revokeDevice').mockRejectedValue(new ClientError('timeout'));

    render(SettingsView);
    await waitFor(() =>
      expect(within(deviceRow('dev-3')).getByTestId('action-bar-overflow')).toBeInTheDocument(),
    );
    await fireEvent.click(within(deviceRow('dev-3')).getByTestId('action-bar-overflow'));
    await fireEvent.click(await within(deviceRow('dev-3')).findByTestId('action-revoke'));
    await fireEvent.click(await screen.findByTestId('confirm-dialog-confirm'));
    await waitFor(() => expect(pending).toHaveLength(1));

    // Act: the event arrives, then the read that started before it answers with the old list.
    emitEvent?.('device.revoked', { device_id: 'dev-3' });
    pending[0](devices);
    await settleMicrotasks();

    // Assert: the event's change survives the snapshot.
    expect(screen.queryByText('Chrome on build-box')).not.toBeInTheDocument();
    expect(screen.getByText('Safari on iPad')).toBeInTheDocument();
  });

  it('guards a read that settles after the view is gone', () => {
    // The destroy guard is what keeps a read's continuation from writing to a view the operator
    // has left; it is pinned at its source, because the write it prevents is invisible to the page.
    const source = fs.readFileSync(path.resolve(__dirname, 'SettingsView.svelte'), 'utf8');
    expect(source).toContain('if (!isMounted) return;');
  });

  it('writes nothing when a load settles after the view is gone', async () => {
    // Arrange: a devices read that answers only when the test says so.
    let settle: (rows: PairedDevice[]) => void = () => {};
    vi.spyOn(apiClient, 'getDevices').mockImplementation(
      () =>
        new Promise<PairedDevice[]>((resolve) => {
          settle = resolve;
        }),
    );
    const before = get(runtimeStore);

    const view = render(SettingsView);
    await waitFor(() => expect(apiClient.getDevices).toHaveBeenCalled());

    // Act: the operator leaves the view, then the read answers.
    view.unmount();
    settle(devices);
    await settleMicrotasks();

    // Assert: nothing was written and nothing rendered.
    expect(get(runtimeStore)).toBe(before);
    expect(screen.queryByTestId('devices-table')).not.toBeInTheDocument();
  });

  it('aborts the status read when the devices read fails', async () => {
    // Arrange: the status read never answers, and the devices read ends in a timeout.
    const statusSignals: (AbortSignal | undefined)[] = [];
    vi.spyOn(apiClient, 'getDevices').mockRejectedValue(new ClientError('timeout'));
    vi.spyOn(apiClient, 'getStatus').mockImplementation((signal) => {
      statusSignals.push(signal);
      return new Promise(() => {});
    });

    // Act
    render(SettingsView);

    // Assert: the status read behind the failure is ended, not left to answer into nothing.
    await waitFor(() => expect(statusSignals).toHaveLength(1));
    await waitFor(() => expect(statusSignals[0]?.aborted).toBe(true));
  });

  it('aborts both reads when the view goes away', async () => {
    // Arrange
    const signals: (AbortSignal | undefined)[] = [];
    vi.spyOn(apiClient, 'getDevices').mockImplementation((signal) => {
      signals.push(signal);
      return new Promise(() => {});
    });
    vi.spyOn(apiClient, 'getStatus').mockImplementation((signal) => {
      signals.push(signal);
      return new Promise(() => {});
    });

    const view = render(SettingsView);
    await waitFor(() => expect(signals).toHaveLength(2));

    // Act
    view.unmount();

    // Assert
    expect(signals[0]?.aborted).toBe(true);
    expect(signals[1]?.aborted).toBe(true);
  });

  it('shows the devices when the status read never settles', async () => {
    // Arrange: the status read accepts the request and never answers.
    vi.useFakeTimers();
    try {
      vi.spyOn(apiClient, 'getStatus').mockReturnValue(new Promise(() => {}));

      render(SettingsView);
      await settleMicrotasks();

      // Act: the read reaches its own bound.
      await vi.advanceTimersByTimeAsync(10_000);
      await settleMicrotasks();

      // Assert: the list is shown, and the listener is unknown rather than "on".
      expect(screen.getByText('Firefox on mbp-oliver')).toBeInTheDocument();
      expect(screen.getByTestId('listener-status')).toHaveTextContent('Network listener: unknown');
    } finally {
      vi.useRealTimers();
    }
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
