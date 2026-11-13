import { describe, it, expect, vi } from 'vitest';
import path from 'node:path';
import { createRawSnippet } from 'svelte';
import { fireEvent, render, screen } from '@testing-library/svelte';
import RowLink from './RowLink.svelte';
import { AUTHORITY_FORM } from '../../test/appPathAuthorityForms';
import { ruleOf, rulesOf, styleOf } from '../../test/templateStyle';

/**
 * `RowLink` (components.adoc, RowLink): a row that opens the object it stands for. The title is the
 * link and its target is stretched over the whole row, so the row takes the hover, the pointer and
 * the focus ring; the controls inside lie above it as separate targets.
 */

const STYLE = styleOf(path.resolve(__dirname, 'RowLink.svelte'));

/** The row's slots, as a caller hands them in. */
const KIND = createRawSnippet(() => ({ render: () => '<span data-testid="row-kind">Plan</span>' }));
const TAGS = createRawSnippet(() => ({
  render: () => '<span data-testid="row-tags">running</span>',
}));

/** The row's nested controls, as a caller hands them in. */
const ACTS = createRawSnippet(() => ({
  render: () => '<button type="button" data-testid="row-menu">More</button>',
}));

describe('RowLink', () => {
  it('draws exactly one anchor per row, over the title', () => {
    const { container } = render(RowLink, {
      props: { href: '/plans/fix-relay', title: 'Fix Relay Transport', kind: KIND, tags: TAGS },
    });
    const anchors = container.querySelectorAll('a');

    expect(anchors).toHaveLength(1);
    expect(anchors[0]).toHaveAttribute('href', '/plans/fix-relay');
    expect(anchors[0]).toHaveClass('row-link__title');
    expect(anchors[0]).toHaveTextContent('Fix Relay Transport');
  });

  it('keeps the controls outside the anchor, as targets of their own', () => {
    const { container } = render(RowLink, {
      props: {
        href: '/plans/fix-relay',
        title: 'Fix Relay Transport',
        acts: ACTS,
      },
    });
    const anchor = container.querySelector('a') as HTMLAnchorElement;
    const menu = screen.getByTestId('row-menu');

    expect(anchor.contains(menu)).toBe(false);
    expect(container.querySelector('.row-link__acts')).toContainElement(menu);
  });

  it('follows the current row with aria-current', () => {
    const plain = render(RowLink, { props: { href: '/plans/a', title: 'A' } });
    expect(plain.container.querySelector('.row-link')).not.toHaveAttribute('aria-current');
    plain.unmount();

    const current = render(RowLink, { props: { href: '/plans/a', title: 'A', current: true } });
    expect(current.container.querySelector('.row-link')).toHaveAttribute('aria-current', 'true');
    expect(current.container.querySelector('.row-link')).toHaveClass('row-link--current');
  });

  it('opens the object on a click on the title', async () => {
    const onAnchor = vi.fn();
    const { container } = render(RowLink, { props: { href: '/plans/a', title: 'A' } });
    const anchor = container.querySelector('a') as HTMLAnchorElement;
    anchor.addEventListener('click', onAnchor);

    const notPrevented = await fireEvent.click(anchor);

    expect(notPrevented).toBe(true);
    expect(onAnchor).toHaveBeenCalledTimes(1);
  });

  it('opens nothing on a click on a nested control', async () => {
    const onAnchor = vi.fn();
    const onMenu = vi.fn();
    const { container } = render(RowLink, {
      props: { href: '/plans/a', title: 'A', acts: ACTS },
    });
    const anchor = container.querySelector('a') as HTMLAnchorElement;
    anchor.addEventListener('click', onAnchor);
    const menu = screen.getByTestId('row-menu');
    menu.addEventListener('click', onMenu);

    await fireEvent.click(menu);

    expect(onMenu).toHaveBeenCalledTimes(1);
    expect(onAnchor).not.toHaveBeenCalled();
  });

  it.each(AUTHORITY_FORM)('draws %j (%s) as text with no anchor', (href) => {
    const { container } = render(RowLink, { props: { href, title: 'Fix Relay Transport' } });

    expect(container.querySelector('a')).toBeNull();
    expect(container.querySelector('[href]')).toBeNull();
    expect(container.querySelector('.row-link__title')).toHaveTextContent('Fix Relay Transport');
  });

  it.each([
    ['https://evil.example/x', 'a foreign origin'],
    ['javascript:alert(1)', 'a script address'],
    ['plans/relay', 'a relative address'],
  ])('draws %j (%s) as text with no anchor', (href, _why) => {
    const { container } = render(RowLink, { props: { href, title: 'Fix Relay Transport' } });

    expect(container.querySelector('a')).toBeNull();
    expect(container.querySelector('[href]')).toBeNull();
    expect(container.querySelector('.row-link__title')).toHaveTextContent('Fix Relay Transport');
  });

  it('takes the hover, the pointer and the focus ring on the whole row it opens', () => {
    expect(ruleOf(STYLE, '.row-link--opens:not(.row-link--current):hover')).toContain(
      'background-color',
    );
    expect(ruleOf(STYLE, '.row-link--opens')).toContain('cursor: pointer');
    expect(ruleOf(STYLE, '.row-link:focus-within')).toContain('box-shadow: var(--focus-ring)');
  });

  it('lifts and points on no row that opens nothing', () => {
    const { container } = render(RowLink, {
      props: { href: '//evil.example/x', title: 'Fix Relay Transport' },
    });

    expect(container.querySelector('.row-link')).not.toHaveClass('row-link--opens');
    expect(ruleOf(STYLE, '.row-link')).not.toContain('cursor: pointer');
    // No stretched target is emitted for a row that opens nothing.
    expect(ruleOf(STYLE, '.row-link__title::after')).toBe('');
  });

  it('keeps the selected row’s ground under the pointer', () => {
    expect(ruleOf(STYLE, '.row-link--opens:not(.row-link--current):hover')).not.toBe('');
    expect(ruleOf(STYLE, '.row-link--opens:hover')).toBe('');
    expect(ruleOf(STYLE, '.row-link--current')).toContain(
      'background-color: var(--color-brand-subtle)',
    );
  });

  it('stretches the title link over the row without a numeric layer', () => {
    expect(ruleOf(STYLE, 'a.row-link__title::after')).toContain('position: absolute');
    expect(ruleOf(STYLE, 'a.row-link__title::after')).toContain('inset: 0');
    expect(STYLE).not.toMatch(/z-index/);
  });

  it('keeps the title on one line of the fixed row', () => {
    expect(ruleOf(STYLE, '.row-link__title')).toContain('text-overflow: ellipsis');
    expect(ruleOf(STYLE, '.row-link__title')).toContain('white-space: nowrap');
    expect(ruleOf(STYLE, '.row-link__title')).toContain('min-inline-size: 0');
  });

  it('draws the selected row on the brand-subtle ground', () => {
    expect(ruleOf(STYLE, '.row-link--current')).toContain('background-color: var(--color-brand-subtle)');
  });

  it('draws a row of 40 px, 32 px compact and 44 px on a phone', () => {
    expect(ruleOf(STYLE, '.row-link')).toContain('min-block-size: 40px');
    expect(ruleOf(STYLE, '.row-link--compact')).toContain('min-block-size: 32px');
    expect(rulesOf(STYLE, '.row-link').join('\n')).toContain('min-block-size: 44px');
    expect(rulesOf(STYLE, '.row-link__acts').join('\n')).toContain('position: relative');
  });

  it('lets the row wrap on a narrow screen instead of overflowing it', () => {
    expect(ruleOf(STYLE, '.row-link')).toContain('flex-wrap: wrap');
    expect(ruleOf(STYLE, '.row-link__title')).toContain('min-inline-size: 0');
    expect(ruleOf(STYLE, '.row-link__tags')).toContain('flex-wrap: wrap');
  });

  it('keeps a copy chip in the tags slot above the stretched target', () => {
    expect(ruleOf(STYLE, '.row-link__tags :global(.chip)')).toContain('position: relative');
  });
});
