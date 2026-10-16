import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import { get } from 'svelte/store';
import RevokedView from './RevokedView.svelte';
import { runtimeStore } from '../stores/runtime';

describe('RevokedView component', () => {
  beforeEach(() => {
    runtimeStore.set({
      isInitialized: true,
      isPaired: false,
      isOnboarded: true,
      deviceLabel: 'Firefox on mbp-oliver',
      machineName: 'mbp-oliver',
      currentRoute: 'revoked',
      currentState: null,
      isRevoked: true,
      revokedReason: 'device_revoked',
      revokedInfo: {
        reason: 'device_revoked',
        revoked_by: 'the terminal client',
        revoked_at: 'today at 14:10',
      },
      isOffline: false,
      activeStep: 1,
    });
  });

  it('renders revoked title, reason and how to pair back', () => {
    render(RevokedView);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(
      'This browser is no longer paired',
    );
    expect(screen.getByTestId('revoked-reason')).toHaveTextContent(
      'It was revoked from the terminal client today at 14:10.',
    );
    expect(
      screen.getByText(/To use the web app here again, pair it with a new code/i),
    ).toBeInTheDocument();
    expect(screen.getByTestId('pair-browser-button')).toHaveTextContent('Pair this browser');
  });

  it('renders expiration reason when revokedInfo reason is device_expired', () => {
    runtimeStore.update((s) => ({
      ...s,
      revokedInfo: {
        reason: 'device_expired',
        revoked_at: 'yesterday',
      },
    }));

    render(RevokedView);

    expect(screen.getByTestId('revoked-reason')).toHaveTextContent(
      'It expired after not being used since yesterday.',
    );
  });

  it('navigates to pairing page when pair button is clicked', async () => {
    render(RevokedView);

    const btn = screen.getByTestId('pair-browser-button');
    await fireEvent.click(btn);

    const state = get(runtimeStore);
    expect(state.currentRoute).toBe('pair');
    expect(state.isRevoked).toBe(false);
  });
});
