import { describe, it, expect, beforeEach } from 'vitest';
import path from 'node:path';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import PairingView from './PairingView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { blockOf, ruleOf, stylesheetOf, withoutBlocks } from '../test/stylesheet';

/**
 * The pairing page (entry.adoc, The Pairing Page): the form, the revoked banner, and the `welcome`
 * hero beside the card, whose stylesheet hides it where the page is too narrow to hold both and
 * below the free-height threshold, and switches the brand icon from 28 px to 64 px with it.
 */

const COMPONENT_PATH = path.resolve(__dirname, 'PairingView.svelte');

/**
 * The container query under which the hero is hidden: a page too narrow for the hero beside a
 * whole card. It reads the page's width, not the viewport's, which the rail also takes from.
 */
const HERO_HIDDEN_NARROW = '@container pairing-page (max-width: 50rem)';

/** The media query under which the hero is hidden: too little free height. */
const HERO_HIDDEN_SHORT = '@media (max-height: 520px)';

/** Both conditions that hide the hero; each one carries the whole switch of the brand block. */
const HERO_HIDDEN = [
  ['the page is narrow', HERO_HIDDEN_NARROW],
  ['the page is short', HERO_HIDDEN_SHORT],
] as const;

describe('PairingView component', () => {
  const stylesheet = stylesheetOf(COMPONENT_PATH);

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

  describe('the hero', () => {
    it('draws the welcome figure, full, at 320 px beside the card', () => {
      render(PairingView);
      const hero = screen.getByTestId('pairing-hero');
      const figure = within(hero).getByTestId('mascot-figure');

      expect(figure).toHaveAttribute('data-pose', 'welcome');
      expect(figure).toHaveAttribute('data-crop', 'full');
      expect(figure).toHaveAttribute('height', '320');
      expect(within(screen.getByTestId('pairing-view')).queryByTestId('mascot-figure')).toBeNull();
    });

    it('loads the figure eagerly and enters it once', () => {
      render(PairingView);
      const figure = within(screen.getByTestId('pairing-hero')).getByTestId('mascot-figure');

      expect(figure).toHaveAttribute('loading', 'eager');
      expect(figure).toHaveClass('mascot-figure--entrance');
    });

    it('keeps the figure decorative', () => {
      render(PairingView);
      const figure = within(screen.getByTestId('pairing-hero')).getByTestId('mascot-figure');

      expect(figure).toHaveAttribute('alt', '');
      expect(figure).toHaveAttribute('aria-hidden', 'true');
    });

    it('is placed and hidden by the stylesheet, never by a style attribute', () => {
      const { container } = render(PairingView);

      expect(container.querySelectorAll('[style]')).toHaveLength(0);
    });

    it.each(HERO_HIDDEN)('is hidden where %s', (_case, opener) => {
      const hidden = blockOf(stylesheet, opener);

      expect(ruleOf(hidden, '.pairing-page__hero')).toMatch(/display:\s*none\s*;/);
    });

    it('is hidden by the width of the page, never by the width of the viewport', () => {
      const topLevel = withoutBlocks(stylesheet, HERO_HIDDEN_NARROW, HERO_HIDDEN_SHORT);

      expect(ruleOf(topLevel, '.pairing-frame')).toMatch(
        /container:\s*pairing-page\s*\/\s*inline-size\s*;/,
      );
      expect(ruleOf(topLevel, '.pairing-page__hero')).not.toMatch(/display:\s*none/);
      // The phone rules of the viewport shape the card only; the hero follows the page.
      expect(blockOf(stylesheet, '@media (max-width: 768px)')).not.toContain('.pairing-page__hero');
      expect(blockOf(stylesheet, '@media (max-width: 768px)')).not.toContain('.pair-card__mark');
    });

    it('stands inside the query container, which the page itself is not', () => {
      render(PairingView);
      const hero = screen.getByTestId('pairing-hero');

      expect(hero.closest('.pairing-frame')).not.toBeNull();
      expect(hero.parentElement).toHaveClass('pairing-page');
      expect(hero.parentElement?.parentElement).toHaveClass('pairing-frame');
    });

    it('drops the page margin where the page is narrow', () => {
      const narrow = blockOf(stylesheet, HERO_HIDDEN_NARROW);

      expect(ruleOf(narrow, '.pairing-page')).toMatch(/padding:\s*0\s*;/);
      expect(ruleOf(narrow, '.pairing-page')).toMatch(/margin:\s*var\(--space-4\) auto\s*;/);
    });
  });

  describe('the brand block', () => {
    it('carries the icon at 28 px and at 64 px, beside the wordmark', () => {
      render(PairingView);

      const withHero = screen.getByTestId('pairing-mark-28');
      const alone = screen.getByTestId('pairing-mark-64');
      expect(within(withHero).getByTestId('brand-mark')).toHaveAttribute('data-size', '28');
      expect(within(alone).getByTestId('brand-mark')).toHaveAttribute('data-size', '64');
      expect(withHero.parentElement).toHaveTextContent('plan-marshall');
    });

    it('shows the 28 px icon beside the wordmark while the hero shows', () => {
      const topLevel = withoutBlocks(stylesheet, HERO_HIDDEN_NARROW, HERO_HIDDEN_SHORT);

      expect(ruleOf(topLevel, '.pair-card__mark')).toMatch(/display:\s*inline-flex\s*;/);
      expect(ruleOf(topLevel, '.pair-card__mark--alone')).toMatch(/display:\s*none\s*;/);
      // Only the hero-hiding rules hide the 28 px icon; outside them, it keeps the shared display.
      expect(topLevel).not.toContain('.pair-card__mark--with-hero');
      expect(ruleOf(topLevel, '.pair-card__brand')).not.toMatch(/flex-direction:\s*column/);
    });

    it.each(HERO_HIDDEN)('shows the 64 px icon above the wordmark where %s', (_case, opener) => {
      const hidden = blockOf(stylesheet, opener);

      expect(ruleOf(hidden, '.pair-card__mark--with-hero')).toMatch(/display:\s*none\s*;/);
      expect(ruleOf(hidden, '.pair-card__mark--alone')).not.toMatch(/display:\s*none/);
      expect(ruleOf(hidden, '.pair-card__brand')).toMatch(/flex-direction:\s*column\s*;/);
    });
  });

  describe('the revoked banner', () => {
    it('takes no figure, and the page keeps the welcome hero alone', () => {
      runtimeStore.update((s) => ({
        ...s,
        isRevoked: true,
        revokedReason: 'It was revoked in the terminal at 14:32.',
      }));

      render(PairingView);
      const banner = screen.getByRole('heading', { level: 2 }).closest('.revoked-banner');
      const figures = screen.getAllByTestId('mascot-figure');

      expect(banner).not.toBeNull();
      expect(banner?.querySelector('img')).toBeNull();
      expect(figures).toHaveLength(1);
      expect(figures[0]).toHaveAttribute('data-pose', 'welcome');
    });
  });
});
