import { describe, it, expect, beforeEach } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import {
  FAVICON_ATTENTION_HREF,
  FAVICON_CALM_HREF,
  applyFavicon,
  faviconHref,
} from './favicon';

const STATIC_DIR = path.resolve(__dirname, '../../static');

/**
 * @param href the icon href to plant in the head
 */
function plantIcon(href: string): void {
  document.head.innerHTML = `<link rel="icon" type="image/svg+xml" href="${href}" />`;
}

/**
 * @returns the href the document's icon link carries
 */
function currentIcon(): string | null {
  return document.querySelector('link[rel="icon"]')?.getAttribute('href') ?? null;
}

/**
 * Reads a custom property's light-theme value from the token catalogue, so a baked tab copy
 * cannot drift from the token it stands for.
 *
 * @param token the custom property, e.g. `--color-brand`
 * @returns the six-digit hex value the light theme declares
 */
function lightTokenValue(token: string): string {
  const css = fs.readFileSync(path.resolve(__dirname, '../styles/tokens.css'), 'utf-8');
  const declaration = css
    .split('\n')
    .find((line) => line.trim().startsWith(`${token}:`));
  expect(declaration, `${token} is declared in tokens.css`).toBeDefined();
  return (declaration ?? '').split(':')[1]?.replace(';', '').trim() ?? '';
}

describe('the tab icon', () => {
  beforeEach(() => {
    plantIcon(FAVICON_CALM_HREF);
  });

  it('shows the plain mark while nothing waits', () => {
    expect(faviconHref(0)).toBe(FAVICON_CALM_HREF);
    expect(applyFavicon(0)).toBe(FAVICON_CALM_HREF);
    expect(currentIcon()).toBe(FAVICON_CALM_HREF);
  });

  it('carries the attention dot while items wait', () => {
    expect(faviconHref(3)).toBe(FAVICON_ATTENTION_HREF);
    expect(applyFavicon(3)).toBe(FAVICON_ATTENTION_HREF);
    expect(currentIcon()).toBe(FAVICON_ATTENTION_HREF);
  });

  it('drops the dot again when the last item leaves', () => {
    applyFavicon(2);
    expect(currentIcon()).toBe(FAVICON_ATTENTION_HREF);

    expect(applyFavicon(0)).toBe(FAVICON_CALM_HREF);
    expect(currentIcon()).toBe(FAVICON_CALM_HREF);
  });

  it('leaves a document without an icon link alone', () => {
    document.head.innerHTML = '';

    expect(applyFavicon(1)).toBe(FAVICON_ATTENTION_HREF);
    expect(currentIcon()).toBeNull();
  });
});

describe('the two tab assets', () => {
  it('delivers the plain mark and the mark with the attention dot as parsable vectors', () => {
    for (const href of [FAVICON_CALM_HREF, FAVICON_ATTENTION_HREF]) {
      const svg = fs.readFileSync(path.join(STATIC_DIR, href.replace(/^\//, '')), 'utf-8');
      const doc = new DOMParser().parseFromString(svg, 'image/svg+xml');

      expect(doc.querySelector('parsererror')).toBeNull();
      expect(doc.documentElement.getAttribute('viewBox')).toBe('0 0 24 24');
    }
  });

  it('adds the attention dot only to the attention variant', () => {
    const calm = fs.readFileSync(path.join(STATIC_DIR, 'brand-mark.svg'), 'utf-8');
    const attention = fs.readFileSync(
      path.join(STATIC_DIR, 'brand-mark-attention.svg'),
      'utf-8',
    );

    expect(calm).not.toContain(lightTokenValue('--color-attention'));
    expect(attention).toContain(lightTokenValue('--color-attention'));
  });

  it('bakes the light-theme brand and attention colours of the token catalogue', () => {
    const calm = fs.readFileSync(path.join(STATIC_DIR, 'brand-mark.svg'), 'utf-8');
    const attention = fs.readFileSync(
      path.join(STATIC_DIR, 'brand-mark-attention.svg'),
      'utf-8',
    );

    expect(calm).toContain(lightTokenValue('--color-brand'));
    expect(attention).toContain(lightTokenValue('--color-brand'));
    expect(attention).toContain(lightTokenValue('--color-attention'));
  });
});
