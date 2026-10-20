import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import PairingView from './PairingView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';

describe('PairingView component', () => {
  beforeEach(() => {
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
      }),
    );
  });

  it('renders pairing page title and input', () => {
    render(PairingView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Pair this browser');
    expect(screen.getByLabelText(/code/i)).toBeInTheDocument();
  });

  it('disables pairing button when code is less than 8 characters', () => {
    render(PairingView);

    const submitBtn = screen.getByRole('button', { name: /pair this browser/i });
    expect(submitBtn).toBeDisabled();
  });

  it('enables pairing button when 8 valid code characters are entered', async () => {
    render(PairingView);

    const input = screen.getByLabelText(/code/i);
    const submitBtn = screen.getByRole('button', { name: /pair this browser/i });

    await fireEvent.input(input, { target: { value: 'KXQB-MRTN' } });
    expect(submitBtn).not.toBeDisabled();
  });

  it('displays revoked banner when device was revoked', () => {
    runtimeStore.update((s) => ({
      ...s,
      isRevoked: true,
      revokedReason: 'It was revoked in the terminal at 14:32.',
    }));

    render(PairingView);

    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent(
      'This browser is no longer paired',
    );
    expect(screen.getByText('It was revoked in the terminal at 14:32.')).toBeInTheDocument();
  });
});
