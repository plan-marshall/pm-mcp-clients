import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { render } from '@testing-library/svelte';
import BrandMark from './BrandMark.svelte';

const SOURCE_DIR = path.resolve(__dirname, '../..');
const COMPONENT_PATH = path.resolve(__dirname, 'BrandMark.svelte');

/** The sparkle the header used before the project had a mark of its own. */
const OLD_GLYPH = String.fromCodePoint(0x2726);

/** The sizes each rendering is delivered at (brand-mark.adoc, Where the Icon Is Used). */
const COLOUR_SIZES = [16, 28, 64] as const;
const MONOCHROME_SIZES = [16, 20, 24, 28, 64] as const;

/**
 * @param directory the directory to walk
 * @returns every file below it, recursively
 */
function walk(directory: string): string[] {
  const found: string[] = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const full = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      found.push(...walk(full));
    } else {
      found.push(full);
    }
  }
  return found;
}

/**
 * @param source the component source
 * @returns the content of its `<style>` block
 */
function stylesheetOf(source: string): string {
  const match = /<style>([\s\S]*?)<\/style>/.exec(source);
  if (!match) throw new Error('BrandMark.svelte has no <style> block');
  return match[1];
}

/**
 * @param css a stylesheet
 * @param opener the selector or at-rule whose block is wanted
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
 * @param css a stylesheet without nested blocks
 * @param selector the selector of the rule
 * @returns the declarations of the first top-level rule with exactly that selector
 */
function ruleOf(css: string, selector: string): string {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = new RegExp(`(^|[}\\s])${escaped}\\s*\\{([^}]*)\\}`).exec(css);
  if (!match) throw new Error(`no rule for "${selector}"`);
  return match[2];
}

describe('BrandMark component', () => {
  const source = fs.readFileSync(COMPONENT_PATH, 'utf-8');
  const stylesheet = stylesheetOf(source);

  describe('the colour rendering', () => {
    it('is the default and draws the star and the disc through the component classes', () => {
      const { getByTestId } = render(BrandMark);
      const mark = getByTestId('brand-mark');

      expect(mark.getAttribute('data-rendering')).toBe('colour');
      expect(mark.getAttribute('data-size')).toBe('28');
      expect(mark.querySelector('.brand-mark__colour .brand-mark__star path')).not.toBeNull();
      expect(mark.querySelectorAll('.brand-mark__colour .brand-mark__star circle')).toHaveLength(5);
      expect(mark.querySelector('.brand-mark__colour .brand-mark__disc')).not.toBeNull();
    });

    it('sets no fill or stroke colour on the colour shapes in the markup', () => {
      const { getByTestId } = render(BrandMark, { props: { rendering: 'colour', size: 28 } });
      const shapes = getByTestId('brand-mark').querySelectorAll('.brand-mark__colour, .brand-mark__colour *');

      for (const shape of shapes) {
        expect(shape.hasAttribute('fill'), shape.outerHTML).toBe(false);
        expect(shape.hasAttribute('stroke'), shape.outerHTML).toBe(false);
      }
    });

    it('reads the gold, the fur and the outline from the mascot tokens in its stylesheet', () => {
      expect(ruleOf(stylesheet, '.brand-mark__star')).toMatch(/fill:\s*var\(--color-mascot-gold\)\s*;/);
      expect(ruleOf(stylesheet, '.brand-mark__disc')).toMatch(/fill:\s*var\(--color-mascot-fur\)\s*;/);
      expect(ruleOf(stylesheet, '.brand-mark__colour')).toMatch(
        /stroke:\s*var\(--color-mascot-outline\)\s*;/,
      );
    });

    it('carries no colour literal anywhere in the component', () => {
      expect(source).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
      expect(source).not.toMatch(/\b(rgb|rgba|hsl|hsla)\(/);
    });
  });

  describe('the monochrome rendering', () => {
    it('paints every fill in currentColor and draws no colour shape', () => {
      const { getByTestId } = render(BrandMark, { props: { rendering: 'monochrome', size: 24 } });
      const mark = getByTestId('brand-mark');
      const filled = [...mark.querySelectorAll('[fill]')];

      expect(mark.getAttribute('data-rendering')).toBe('monochrome');
      expect(mark.querySelector('.brand-mark__colour')).toBeNull();
      expect(mark.querySelector('.brand-mark__mono--fallback')).toBeNull();
      expect(mark.querySelector('.brand-mark__mono path')).not.toBeNull();
      expect(filled.length).toBeGreaterThan(0);
      expect(filled.map((element) => element.getAttribute('fill'))).toEqual(filled.map(() => 'currentColor'));
    });
  });

  describe('the delivered sizes', () => {
    it.each(COLOUR_SIZES)('draws the colour rendering at %i px', (size) => {
      const { getByTestId } = render(BrandMark, { props: { rendering: 'colour', size } });
      const mark = getByTestId('brand-mark');

      expect(mark.getAttribute('width')).toBe(String(size));
      expect(mark.getAttribute('height')).toBe(String(size));
      expect(mark.getAttribute('data-size')).toBe(String(size));
      expect(mark.getAttribute('viewBox')).toBe('0 0 24 24');
    });

    it.each(MONOCHROME_SIZES)('draws the monochrome rendering at %i px', (size) => {
      const { getByTestId } = render(BrandMark, { props: { rendering: 'monochrome', size } });
      const mark = getByTestId('brand-mark');

      expect(mark.getAttribute('width')).toBe(String(size));
      expect(mark.getAttribute('height')).toBe(String(size));
      expect(mark.getAttribute('data-size')).toBe(String(size));
      expect(mark.getAttribute('data-rendering')).toBe('monochrome');
    });

    it.each([20, 24] as const)('refuses the colour rendering at the monochrome-only size %i px', (size) => {
      const { queryByTestId, container } = render(BrandMark, { props: { rendering: 'colour', size } });

      expect(queryByTestId('brand-mark')).toBeNull();
      expect(container.querySelector('svg')).toBeNull();
    });
  });

  describe('the accessible name', () => {
    it('is decorative by default', () => {
      const { getByTestId } = render(BrandMark, { props: { size: 28 } });
      const mark = getByTestId('brand-mark');

      expect(mark.getAttribute('aria-hidden')).toBe('true');
      expect(mark.getAttribute('role')).toBeNull();
      expect(mark.hasAttribute('aria-label')).toBe(false);
    });

    it('is an image with that name when a label is given', () => {
      const { getByRole } = render(BrandMark, { props: { size: 28, label: 'plan-marshall' } });
      const mark = getByRole('img', { name: 'plan-marshall' });

      expect(mark.getAttribute('data-testid')).toBe('brand-mark');
      expect(mark.hasAttribute('aria-hidden')).toBe(false);
    });
  });

  describe('forced colours', () => {
    it('carries the monochrome rendering as a hidden fallback of the colour rendering', () => {
      const { getByTestId } = render(BrandMark, { props: { rendering: 'colour', size: 28 } });
      const fallback = getByTestId('brand-mark').querySelector('.brand-mark__mono.brand-mark__mono--fallback');

      expect(fallback).not.toBeNull();
      expect(fallback?.getAttribute('fill')).toBe('currentColor');
      expect(ruleOf(stylesheet, '.brand-mark__mono--fallback')).toMatch(/display:\s*none\s*;/);
    });

    it('swaps the colour rendering for the monochrome one under forced-colors: active', () => {
      // jsdom computes no media, so the rule is read from the component's stylesheet.
      const forced = blockOf(stylesheet, '@media (forced-colors: active)');

      expect(ruleOf(forced, '.brand-mark__colour')).toMatch(/display:\s*none\s*;/);
      expect(ruleOf(forced, '.brand-mark__mono--fallback')).toMatch(/display:\s*inline\s*;/);
    });
  });

  describe('what the mark no longer carries', () => {
    it.each(['colour', 'monochrome'] as const)('sets no style attribute in the %s rendering', (rendering) => {
      const { container } = render(BrandMark, { props: { rendering, size: 28 } });

      expect(container.querySelectorAll('[style]')).toHaveLength(0);
    });

    it('draws no plate and knows no plate prop', () => {
      const { queryByTestId, container } = render(BrandMark, { props: { size: 28 } });

      expect(queryByTestId('brand-mark-plate')).toBeNull();
      expect(container.querySelector('.brand-mark-plate')).toBeNull();
      expect(source).not.toContain('brand-mark-plate');
      expect(source).not.toMatch(/\bplate\b/);
    });
  });
});

describe('the brand glyph sweep', () => {
  it('leaves no sparkle anywhere in the web app source', () => {
    const offenders = walk(SOURCE_DIR)
      .filter((file) => fs.readFileSync(file, 'utf-8').includes(OLD_GLYPH))
      .map((file) => path.relative(SOURCE_DIR, file));

    expect(offenders).toEqual([]);
  });
});
