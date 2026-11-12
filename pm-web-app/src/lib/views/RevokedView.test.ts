import { describe, it, expect, beforeEach } from 'vitest';
import path from 'node:path';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import { get } from 'svelte/store';
import RevokedView from './RevokedView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { blockOf, ruleOf, stylesheetOf, withoutBlocks } from '../test/stylesheet';

/** The container query under which the figure is hidden: a page too narrow for it beside the card. */
const FIGURE_HIDDEN_NARROW = '@container revoked-view (max-width: 36rem)';

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

  describe('the narrow page', () => {
    const stylesheet = stylesheetOf(path.resolve(__dirname, 'RevokedView.svelte'));

    it('makes the page, not the viewport, the width the figure follows', () => {
      const topLevel = withoutBlocks(stylesheet, FIGURE_HIDDEN_NARROW);

      expect(ruleOf(topLevel, '.revoked-view')).toMatch(
        /container:\s*revoked-view\s*\/\s*inline-size\s*;/,
      );
      expect(stylesheet).not.toMatch(/@media[^{]*max-width/);
    });

    it('hides the figure where the page is narrow, and never stacks it', () => {
      const narrow = blockOf(stylesheet, FIGURE_HIDDEN_NARROW);

      expect(ruleOf(narrow, '.revoked-view :global(.mascot-figure)')).toMatch(
        /display:\s*none\s*;/,
      );
      expect(stylesheet).not.toMatch(/flex-direction:\s*column/);
    });

    it('lets the card take the width the figure leaves', () => {
      expect(ruleOf(stylesheet, '.revoked-card')).toMatch(/width:\s*100%\s*;/);
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
