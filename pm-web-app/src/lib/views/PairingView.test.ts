import { describe, it, expect, beforeEach } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import PairingView from './PairingView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';

/**
 * The pairing page (entry.adoc, The Pairing Page): the form, the revoked banner, and the `welcome`
 * hero beside the card, whose stylesheet hides it on a phone and below the free-height threshold
 * and switches the brand icon from 28 px to 64 px with it.
 */

const COMPONENT_PATH = path.resolve(__dirname, 'PairingView.svelte');

/** The media query under which the hero is hidden: a phone, or too little free height. */
const HERO_HIDDEN_MEDIA = '@media (max-width: 768px), (max-height: 520px)';

/**
 * @param css a stylesheet
 * @param opener the at-rule whose block is wanted
 * @returns the block's content, braces matched
 */
function blockOf(css: string, opener: string): string {
  const at = css.indexOf(opener);
  if (at < 0) throw new Error(`no "${opener}" block`);
  const open = css.indexOf('{', at + opener.length);
  let depth = 0;
  for (let i = open; i < css.length; i += 1) {
    if (css[i] === '{') depth += 1;
    if (css[i] === '}') {
      depth -= 1;
      if (depth === 0) return css.slice(open + 1, i);
    }
  }
  throw new Error(`unbalanced "${opener}" block`);
}

/**
 * @param css a stylesheet
 * @param selector the selector of the rule, as written
 * @returns the declarations of the first rule with exactly that selector
 */
function ruleOf(css: string, selector: string): string {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  // The selector must open the rule, so it never matches the tail of a longer selector.
  const match = new RegExp(`(^|[{}])\\s*${escaped}\\s*\\{([^}]*)\\}`).exec(css);
  if (!match) throw new Error(`no rule for "${selector}"`);
  return match[2];
}

/**
 * @param css a stylesheet
 * @param opener an at-rule
 * @returns the stylesheet without that at-rule's block, so a rule is read at the top level
 */
function withoutBlock(css: string, opener: string): string {
  const at = css.indexOf(opener);
  if (at < 0) return css;
  const inner = blockOf(css, opener);
  const end = css.indexOf(inner, at) + inner.length + 1;
  return css.slice(0, at) + css.slice(end);
}

describe('PairingView component', () => {
  const source = fs.readFileSync(COMPONENT_PATH, 'utf-8');
  // Comments are dropped, so a rule is found by its selector alone.
  const stylesheet = (/<style>([\s\S]*?)<\/style>/.exec(source)?.[1] ?? '').replace(/\/\*[\s\S]*?\*\//g, '');

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

    it('is hidden on a phone and below the free-height threshold', () => {
      const hidden = blockOf(stylesheet, HERO_HIDDEN_MEDIA);

      expect(ruleOf(hidden, '.pairing-page__hero')).toMatch(/display:\s*none\s*;/);
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
      const topLevel = withoutBlock(stylesheet, HERO_HIDDEN_MEDIA);

      expect(ruleOf(topLevel, '.pair-card__mark')).toMatch(/display:\s*inline-flex\s*;/);
      expect(ruleOf(topLevel, '.pair-card__mark--alone')).toMatch(/display:\s*none\s*;/);
      // Only the media rule hides the 28 px icon; outside it, the icon keeps the shared display.
      expect(topLevel).not.toContain('.pair-card__mark--with-hero');
      expect(ruleOf(topLevel, '.pair-card__brand')).not.toMatch(/flex-direction:\s*column/);
    });

    it('shows the 64 px icon above the wordmark where the hero is hidden', () => {
      const hidden = blockOf(stylesheet, HERO_HIDDEN_MEDIA);

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
