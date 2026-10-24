import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { render } from '@testing-library/svelte';
import BrandMark from './BrandMark.svelte';

const SOURCE_DIR = path.resolve(__dirname, '../..');
const MARK_PATH = path.resolve(__dirname, '../icons/brand/plan-marshall-mark.svg');

/** The sparkle the header used before the project had a mark of its own. */
const OLD_GLYPH = '\u2726';

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

describe('BrandMark component', () => {
  it('renders a decorative mark by default', () => {
    const { getByTestId } = render(BrandMark, { props: { size: 24 } });
    const mark = getByTestId('brand-mark');

    expect(mark.getAttribute('aria-hidden')).toBe('true');
    expect(mark.getAttribute('role')).toBeNull();
    expect(mark.hasAttribute('aria-label')).toBe(false);
    expect(mark.getAttribute('width')).toBe('24');
    expect(mark.getAttribute('height')).toBe('24');
    expect(mark.getAttribute('data-size')).toBe('24');
  });

  it('carries an accessible name when it is not decorative', () => {
    const { getByTestId } = render(BrandMark, {
      props: { size: 20, label: 'plan-marshall' },
    });
    const mark = getByTestId('brand-mark');

    expect(mark.getAttribute('role')).toBe('img');
    expect(mark.getAttribute('aria-label')).toBe('plan-marshall');
    expect(mark.hasAttribute('aria-hidden')).toBe(false);
    expect(mark.getAttribute('width')).toBe('20');
    expect(mark.getAttribute('height')).toBe('20');
    expect(mark.getAttribute('data-size')).toBe('20');
  });
});

describe('the brand mark asset', () => {
  const svg = fs.readFileSync(MARK_PATH, 'utf-8');

  it('is a parsable 24 x 24 vector', () => {
    const doc = new DOMParser().parseFromString(svg, 'image/svg+xml');

    expect(doc.querySelector('parsererror')).toBeNull();
    expect(doc.documentElement.getAttribute('viewBox')).toBe('0 0 24 24');
  });

  it('draws in currentColor, with no text, raster, or baked-in colour', () => {
    expect(svg).toContain('currentColor');
    expect(svg).not.toMatch(/<(text|image|style)\b/i);
    expect(svg).not.toContain('data:image');
    expect(svg).not.toMatch(/#[0-9a-f]{3,8}\b/i);
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
