import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import { get } from 'svelte/store';
import qrcode from 'qrcode-generator';
import PairingView from './PairingView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { apiClient } from '../api/client';

/**
 * @param value the payload to encode
 * @returns the dark-module coordinates of the QR code this payload produces
 */
function encodedModules(value: string): string {
  const code = qrcode(0, 'M');
  code.addData(value);
  code.make();
  const count = code.getModuleCount();
  const dark: string[] = [];
  for (let row = 0; row < count; row++) {
    for (let col = 0; col < count; col++) {
      if (code.isDark(row, col)) dark.push(`${col},${row}`);
    }
  }
  return dark.join(' ');
}

/**
 * @param svg the rendered QR code
 * @returns the dark-module coordinates the page draws, in document order
 */
function drawnModules(svg: Element): string {
  return Array.from(svg.querySelectorAll('rect[x]'))
    .map((rect) => `${rect.getAttribute('x')},${rect.getAttribute('y')}`)
    .join(' ');
}

/**
 * The pairing page and the entry (UC-ENT-1) as a view test against `web-pairing` and `entry`.
 */

function arrange(partial: Record<string, unknown> = {}): void {
  runtimeStore.set(
    makeRuntimeState({
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
    }),
  );
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

  it.each([
    ['lowercase with dash', 'kxqb-mrtn'],
    ['lowercase without dash', 'kxqbmrtn'],
    ['uppercase with dash', 'KXQB-MRTN'],
    ['uppercase without dash', 'KXQBMRTN'],
  ])('accepts the code in %s and enables the submit button at 8 characters', async (_label, code) => {
    render(PairingView);

    const input = screen.getByLabelText(/code/i);
    const submit = screen.getByRole('button', { name: /pair this browser/i });
    expect(submit).toBeDisabled();

    // Below 8 chars
    await fireEvent.input(input, { target: { value: code.slice(0, 4) } });
    expect(submit).toBeDisabled();

    // 8 characters
    await fireEvent.input(input, { target: { value: code } });
    expect(submit).not.toBeDisabled();
  });

  it('renders command chips that copy on click', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: { writeText },
    });

    const { container } = render(PairingView);

    const chips = container.querySelectorAll('.chip');
    expect(chips).toHaveLength(2);

    await fireEvent.click(chips[0]);
    expect(writeText).toHaveBeenCalledWith('pm-operator web open --pair');

    await fireEvent.click(chips[1]);
    expect(writeText).toHaveBeenCalledWith('pm-operator web pair');
  });

  it('reports a code that does not work with the error under the field', async () => {
    vi.spyOn(apiClient, 'pair').mockRejectedValue(new Error('invalid_code'));

    const { container } = render(PairingView);
    const input = screen.getByLabelText(/code/i);
    await fireEvent.input(input, { target: { value: 'KXQB-MRTN' } });
    await fireEvent.click(screen.getByRole('button', { name: /pair this browser/i }));

    await waitFor(() => {
      const codeInput = screen.getByLabelText(/code/i);
      expect(codeInput).toHaveAttribute('aria-invalid', 'true');
      const error = container.querySelector('.field-error');
      expect(error).toBeInTheDocument();
      expect(error).toHaveTextContent(
        'A code works once. Get a fresh one in a terminal of the machine.',
      );
    });
  });

  it('explains an unreachable machine with the error under the field', async () => {
    vi.spyOn(apiClient, 'pair').mockRejectedValue(new Error('network_offline'));

    const { container } = render(PairingView);
    const input = screen.getByLabelText(/code/i);
    await fireEvent.input(input, { target: { value: 'KXQB-MRTN' } });
    await fireEvent.click(screen.getByRole('button', { name: /pair this browser/i }));

    await waitFor(() => {
      const codeInput = screen.getByLabelText(/code/i);
      expect(codeInput).toHaveAttribute('aria-invalid', 'true');
      const error = container.querySelector('.field-error');
      expect(error).toBeInTheDocument();
      expect(error).toHaveTextContent(
        'The machine cannot be reached right now. Check the connection and try again.',
      );
    });
  });

  it('shows the pairing-lost reason under the field when the runtime refuses the secret', async () => {
    vi.spyOn(apiClient, 'pair').mockRejectedValue(new Error('unauthorized'));

    const { container } = render(PairingView);
    const input = screen.getByLabelText(/code/i);
    await fireEvent.input(input, { target: { value: 'KXQB-MRTN' } });
    await fireEvent.click(screen.getByRole('button', { name: /pair this browser/i }));

    await waitFor(() => {
      const codeInput = screen.getByLabelText(/code/i);
      expect(codeInput).toHaveAttribute('aria-invalid', 'true');
      const error = container.querySelector('.field-error');
      expect(error).toBeInTheDocument();
      expect(error).toHaveTextContent(
        'Its pairing is no longer accepted.',
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
      machine: { name: 'mbp-oliver', onboarded: false },
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

  it('does not probe the authenticated connection status before pairing', async () => {
    const probe = vi.spyOn(apiClient, 'getConnectionStatus');
    vi.spyOn(window, 'location', 'get').mockReturnValue({
      ...window.location,
      hostname: '192.168.1.20',
      origin: 'https://192.168.1.20:7421',
      pathname: '/pair',
    } as Location);

    render(PairingView);

    await waitFor(() => expect(screen.getByTestId('pairing-view')).toBeInTheDocument());
    expect(probe).not.toHaveBeenCalled();
    expect(get(runtimeStore).isRevoked).toBe(false);
  });

  it('presents the pairing URL as a QR code once the code is complete', async () => {
    render(PairingView);
    await fireEvent.input(screen.getByLabelText(/code/i), { target: { value: 'KXQBM-RTN' } });

    await waitFor(() => expect(screen.getByTestId('pairing-qr')).toBeInTheDocument());
    // The QR is drawn locally: the code never reaches a server through an image request.
    const svg = screen.getByTestId('pairing-qr').querySelector('svg');
    expect(svg).not.toBeNull();
    expect(svg?.querySelectorAll('rect').length).toBeGreaterThan(20);
    // The drawn modules are the ones the pairing URL produces, not merely some QR code.
    expect(drawnModules(svg as Element)).toBe(
      encodedModules(`${window.location.origin}/pair#code=KXQBMRTN`),
    );
  });
});
