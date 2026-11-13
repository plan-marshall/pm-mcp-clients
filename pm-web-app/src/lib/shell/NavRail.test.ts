import { describe, it, expect, beforeEach } from 'vitest';
import path from 'node:path';
import { flushSync } from 'svelte';
import { render, screen } from '@testing-library/svelte';
import NavRail from './NavRail.svelte';
import MascotFigure from '../components/MascotFigure.svelte';
import { runtimeStore } from '../stores/runtime';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import { blockOf, ruleOf, stylesheetOf } from '../test/stylesheet';
import { reportBox } from '../test/resizeObserver';

const COMPONENT_PATH = path.resolve(__dirname, 'NavRail.svelte');

/** The size query under which the rail-foot bear shows: enough free height below the destinations. */
const BEAR_CONTAINER = '@container rail-free (min-height: 260px)';

function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

/**
 * @param collapsed whether the rail shows icons only
 * @returns the rendered rail
 */
function renderRail(collapsed: boolean): HTMLElement {
  render(NavRail, {
    props: { collapsed, onToggle: () => {}, themeChoice: 'system', onChooseTheme: () => {} },
  });
  return screen.getByTestId('nav-rail');
}

describe('NavRail component', () => {
  beforeEach(() => {
    runtimeStore.update((state) => ({
      ...state,
      isPaired: true,
      isOnboarded: true,
      isInitialized: true,
      currentRoute: 'projects',
    }));
  });

  it('renders Settings in English and Einstellungen in German for the machine settings rail button', () => {
    useCatalogue('en');
    const { unmount } = render(NavRail, {
      props: {
        collapsed: false,
        onToggle: () => {},
        themeChoice: 'system',
        onChooseTheme: () => {},
      },
    });

    expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
    unmount();

    useCatalogue('de');
    render(NavRail, {
      props: {
        collapsed: false,
        onToggle: () => {},
        themeChoice: 'system',
        onChooseTheme: () => {},
      },
    });

    expect(screen.getByRole('button', { name: 'Einstellungen' })).toBeInTheDocument();
  });

  describe('the head', () => {
    /** Leaves the browser unpaired, so the head draws the inert brand. */
    function unpair(): void {
      runtimeStore.update((state) => ({ ...state, isPaired: false }));
    }

    it.each([
      ['brand-home', false],
      ['brand-inert', true],
    ] as const)('draws the 28 px colour mark without a plate in the %s branch', (testId, unpaired) => {
      useCatalogue('en');
      if (unpaired) unpair();
      renderRail(false);

      const brand = screen.getByTestId(testId);
      const marks = brand.querySelectorAll('[data-testid="brand-mark"]');

      expect(marks).toHaveLength(1);
      expect(marks[0]).toHaveAttribute('data-size', '28');
      expect(marks[0]).toHaveAttribute('data-rendering', 'colour');
      expect(marks[0]).toHaveAttribute('aria-hidden', 'true');
      expect(screen.queryByTestId('brand-mark-plate')).not.toBeInTheDocument();
      expect(brand.querySelector('.brand-mark-plate')).toBeNull();
    });

    it.each(['en', 'de'] as const)('names the brand on the collapsed home control in %s', (locale) => {
      useCatalogue(locale);
      renderRail(true);

      expect(screen.getByTestId('brand-home')).toHaveAccessibleName(/plan-marshall/);
    });

    it.each(['en', 'de'] as const)('names the brand by the mark of the collapsed inert head in %s', (locale) => {
      useCatalogue(locale);
      unpair();
      renderRail(true);

      const inert = screen.getByTestId('brand-inert');
      const mark = inert.querySelector('[data-testid="brand-mark"]');

      expect(mark).toHaveAttribute('role', 'img');
      expect(mark).toHaveAccessibleName(/plan-marshall/);
      expect(mark).not.toHaveAttribute('aria-hidden');
    });
  });

  describe('the rail-foot bear', () => {
    const stylesheet = stylesheetOf(COMPONENT_PATH);

    beforeEach(() => useCatalogue('en'));

    it('carries the bear class on the expanded rail', () => {
      const rail = renderRail(false);

      expect(rail).toHaveClass('nav-rail--bear');
      expect(screen.getByTestId('rail-spacer').querySelector('.nav-rail__bear')).toHaveAttribute(
        'aria-hidden',
        'true',
      );
    });

    it('drops the bear class on the collapsed rail', () => {
      const rail = renderRail(true);

      expect(rail).not.toHaveClass('nav-rail--bear');
    });

    it('drops the bear class while a mascot figure is on screen', () => {
      render(MascotFigure, { props: { pose: 'all-clear', crop: 'bust', height: 140 } });
      reportBox(screen.getByTestId('mascot-figure'), 108, 140);

      const rail = renderRail(false);

      expect(rail).not.toHaveClass('nav-rail--bear');
    });

    it('keeps the bear beside a mounted figure the page hides', () => {
      // The pairing page between 768 and 1032 px with the expanded rail: its container query sets
      // the hero to display: none, so the figure is mounted but has no box.
      render(MascotFigure, { props: { pose: 'welcome', height: 320, eager: true } });

      const rail = renderRail(false);

      expect(screen.getByTestId('mascot-figure')).toBeInTheDocument();
      expect(rail).toHaveClass('nav-rail--bear');
    });

    it('follows the figure as the page hides and shows it again', () => {
      render(MascotFigure, { props: { pose: 'welcome', height: 320, eager: true } });
      const figure = screen.getByTestId('mascot-figure');
      const rail = renderRail(false);

      reportBox(figure, 257, 320);
      flushSync();
      expect(rail).not.toHaveClass('nav-rail--bear');

      // The page narrows below the figure's container query.
      reportBox(figure, 0, 0);
      flushSync();
      expect(rail).toHaveClass('nav-rail--bear');

      // The page widens again.
      reportBox(figure, 257, 320);
      flushSync();
      expect(rail).not.toHaveClass('nav-rail--bear');
    });

    it('sets no style attribute on the rail, the free height or the bear', () => {
      const rail = renderRail(false);
      const spacer = screen.getByTestId('rail-spacer');

      expect(rail).not.toHaveAttribute('style');
      expect(spacer).not.toHaveAttribute('style');
      expect(spacer.querySelectorAll('[style]')).toHaveLength(0);
    });

    it('draws the source bust from the stylesheet at 0.18 in the light theme', () => {
      const bear = ruleOf(blockOf(stylesheet, BEAR_CONTAINER), '.nav-rail--bear .nav-rail__bear');

      expect(bear).toMatch(/image-set\(\s*url\('\.\.\/assets\/mascot\/mascot-bust-1x\.webp'\)\s*1x,/);
      expect(bear).toMatch(/url\('\.\.\/assets\/mascot\/mascot-bust-2x\.webp'\)\s*2x/);
      expect(bear).toMatch(/opacity:\s*0\.18\s*;/);
      expect(bear).toMatch(/pointer-events:\s*none\s*;/);
      expect(bear).not.toMatch(/animation|transition/);
    });

    it('draws the bust at 0.12 in the dark theme', () => {
      const dark = ruleOf(
        blockOf(stylesheet, BEAR_CONTAINER),
        ":global([data-theme='dark']) .nav-rail--bear .nav-rail__bear",
      );

      expect(dark).toMatch(/opacity:\s*0\.12\s*;/);
    });

    it('shows only where the free height below the destinations reaches 260 px', () => {
      expect(ruleOf(stylesheet, '.nav-rail__spacer')).toMatch(/container:\s*rail-free\s*\/\s*size\s*;/);
      // Outside the size query no rule draws the bear, so too little free height leaves it empty.
      const outside = stylesheet.replace(blockOf(stylesheet, BEAR_CONTAINER), '');
      expect(outside).not.toContain('.nav-rail__bear');
    });

    it('is hidden on the phone with the rail', () => {
      expect(ruleOf(blockOf(stylesheet, '@media (max-width: 767px)'), '.nav-rail')).toMatch(
        /display:\s*none\s*;/,
      );
    });
  });
});
