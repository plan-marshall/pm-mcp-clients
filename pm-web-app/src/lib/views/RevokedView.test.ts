import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import { get } from 'svelte/store';
import RevokedView from './RevokedView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';

describe('RevokedView component', () => {
  beforeEach(() => {
    runtimeStore.set(
      makeRuntimeState({
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
        isListenerClosed: false,
        completedSteps: 0,
        activeStep: 1,
      }),
    );
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

  describe('the figure', () => {
    it('draws the access-ended figure, full, at 180 px', () => {
      render(RevokedView);
      const figure = within(screen.getByTestId('revoked-view')).getByTestId('mascot-figure');

      expect(figure).toHaveAttribute('data-pose', 'access-ended');
      expect(figure).toHaveAttribute('data-crop', 'full');
      expect(figure).toHaveAttribute('height', '180');
      expect(figure).toHaveAttribute('alt', '');
      expect(figure).toHaveAttribute('aria-hidden', 'true');
    });

    it('stands beside the text, never above it or inside the card', () => {
      render(RevokedView);
      const view = screen.getByTestId('revoked-view');
      const figure = within(view).getByTestId('mascot-figure');
      const card = screen.getByRole('heading', { level: 1 }).closest('.revoked-card');

      expect(figure.parentElement).toBe(view);
      expect(card?.parentElement).toBe(view);
      expect(card?.querySelector('img')).toBeNull();
    });

    it('sets no style attribute', () => {
      const { container } = render(RevokedView);

      expect(container.querySelectorAll('[style]')).toHaveLength(0);
    });
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
