import { describe, it, expect, beforeEach } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { render, screen } from '@testing-library/svelte';
import NavRail from './NavRail.svelte';
import MascotFigure from '../components/MascotFigure.svelte';
import { runtimeStore } from '../stores/runtime';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

const COMPONENT_PATH = path.resolve(__dirname, 'NavRail.svelte');

/** The size query under which the rail-foot bear shows: enough free height below the destinations. */
const BEAR_CONTAINER = '@container rail-free (min-height: 260px)';

function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

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
    const source = fs.readFileSync(COMPONENT_PATH, 'utf-8');
    // Comments are dropped, so a rule is found by its selector alone.
    const stylesheet = (/<style>([\s\S]*?)<\/style>/.exec(source)?.[1] ?? '').replace(
      /\/\*[\s\S]*?\*\//g,
      '',
    );

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

      const rail = renderRail(false);

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
