import { describe, it, expect } from 'vitest';
import path from 'node:path';
import { createRawSnippet } from 'svelte';
import { render, screen } from '@testing-library/svelte';
import Link from './Link.svelte';
import { ruleOf, styleOf } from '../../test/templateStyle';

/**
 * `Link` (components.adoc, Link): an `<a href>` to a place of the app in one of three looks — text,
 * the secondary button look, or the chip look — never brand-coloured and never `href="#"`.
 */

const STYLE = styleOf(path.resolve(__dirname, 'Link.svelte'));

/** A stand-in icon, so the test sees where the snippet lands. */
const ICON = createRawSnippet(() => ({ render: () => '<span data-testid="link-icon"></span>' }));

describe('Link', () => {
  it('goes to its real href and names its destination', () => {
    render(Link, { props: { href: '/plans/fix-relay/report', label: 'Completion report' } });
    const link = screen.getByRole('link', { name: 'Completion report' });

    expect(link).toHaveAttribute('href', '/plans/fix-relay/report');
    expect(link).not.toHaveAttribute('target');
  });

  it.each(['text', 'button', 'chip'] as const)('draws the %s look', (look) => {
    render(Link, { props: { href: '/settings', label: 'Settings', look } });

    expect(screen.getByRole('link')).toHaveClass('link', `link--${look}`);
  });

  it('draws the text look when no look is asked for', () => {
    render(Link, { props: { href: '/settings', label: 'Settings' } });

    expect(screen.getByRole('link')).toHaveClass('link--text');
  });

  it.each(['', '#'])('draws a link without a known URL (%j) as text, never href="#"', (href) => {
    const { container } = render(Link, { props: { href, label: 'Settings' } });

    expect(screen.queryByRole('link')).toBeNull();
    expect(container.querySelector('a')).toBeNull();
    expect(container).toHaveTextContent('Settings');
  });

  it('puts the icon snippet before the label', () => {
    render(Link, { props: { href: '/designer', label: 'Process designer', icon: ICON } });
    const link = screen.getByRole('link');
    const icon = screen.getByTestId('link-icon');
    const label = link.querySelector('.link__label') as HTMLElement;

    expect(link).toContainElement(icon);
    expect(icon.compareDocumentPosition(label) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('keeps the focus ring on every look', () => {
    expect(ruleOf(STYLE, '.link:focus-visible')).toContain('box-shadow: var(--focus-ring)');
  });

  it('takes the colour of the text around it and never the brand', () => {
    expect(ruleOf(STYLE, '.link--text')).toContain('color: inherit');
    expect(STYLE).not.toContain('--color-brand');
  });

  it('underlines a link in a sentence and a standing link only on hover', () => {
    expect(ruleOf(STYLE, '.link--text')).toContain('text-decoration: none');
    expect(ruleOf(STYLE, '.link--text:hover')).toContain('text-decoration: underline');
    expect(STYLE).toMatch(/:global\(:is\(p, li, dd, td, figcaption, blockquote\)\) > \.link--text\s*\{\s*text-decoration: underline/);
  });

  it('draws the button look as the secondary Button', () => {
    const rule = ruleOf(STYLE, '.link--button');

    expect(rule).toContain('background-color: var(--color-bg-surface)');
    expect(rule).toContain('border: 1px solid var(--color-border-strong)');
    expect(rule).toContain('color: var(--color-text-primary)');
    expect(rule).toContain('border-radius: var(--radius-md)');
    expect(ruleOf(STYLE, '.link--button:hover')).toContain('background-color: var(--color-bg-subtle)');
  });

  it('draws the chip look on the subtle ground with the small radius', () => {
    const rule = ruleOf(STYLE, '.link--chip');

    expect(rule).toContain('background-color: var(--color-bg-subtle)');
    expect(rule).toContain('border-radius: var(--radius-sm)');
  });
});
