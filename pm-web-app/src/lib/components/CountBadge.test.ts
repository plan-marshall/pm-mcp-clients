import { describe, it, expect } from 'vitest';
import path from 'node:path';
import { render, screen } from '@testing-library/svelte';
import CountBadge from './CountBadge.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import { styleOf } from '../../test/templateStyle';

/**
 * `CountBadge` (components-tags.adoc, count-badge): a pill with a number, its attention fill for
 * "needs you" and the neutral fill otherwise. It appears only in the rail, tabs and group heads.
 * An attention count starts with `!`, from the catalogue; a plain count is the number alone.
 */

const STYLE = styleOf(path.resolve(__dirname, 'CountBadge.svelte'));

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'en' | 'de'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('CountBadge', () => {
  it('draws the number in the neutral fill by default', () => {
    render(CountBadge, { props: { count: 3 } });
    const badge = document.querySelector('.count-badge');

    expect(screen.getByText('3')).toBeInTheDocument();
    expect(badge?.className).toContain('count-badge--neutral');
    expect(badge?.className).not.toContain('count-badge--attention');
  });

  it('draws the attention fill for an item that needs you', () => {
    const { container } = render(CountBadge, { props: { count: 2, attention: true } });

    expect(container.querySelector('.count-badge')?.className).toContain(
      'count-badge--attention',
    );
  });

  it('reads the name it is given, with the number hidden from a screen reader', () => {
    const { container } = render(CountBadge, { props: { count: 3, label: '3 waiting for you' } });
    const badge = container.querySelector('.count-badge');

    expect(badge?.textContent).toContain('3 waiting for you');
    expect(container.querySelector('[aria-hidden="true"]')?.textContent).toBe('3');
  });

  it.each([0, 1, 12, 1000])('shows the count %i whole, never capped', (count) => {
    render(CountBadge, { props: { count } });

    expect(screen.getByText(String(count))).toBeInTheDocument();
  });

  it.each([
    ['en', '! 12'],
    ['de', '! 12'],
  ] as const)("starts an attention count with the catalogue's exclamation mark in %s", (locale, text) => {
    useCatalogue(locale);
    const { container } = render(CountBadge, { props: { count: 12, attention: true } });

    expect(container.querySelector('.count-badge')).toHaveTextContent(text);
  });

  it('keeps the label the accessible name of an attention count, the number hidden', () => {
    useCatalogue('en');
    const { container } = render(CountBadge, {
      props: { count: 2, attention: true, label: '2 items need you' },
    });
    const badge = container.querySelector('.count-badge') as HTMLElement;

    expect(badge).toHaveTextContent('2 items need you');
    expect(badge.querySelector('[aria-hidden="true"]')?.textContent).toBe('! 2');
  });

  it('keeps the label the accessible name of a plain count, the number hidden', () => {
    const { container } = render(CountBadge, { props: { count: 7, label: '7 reports' } });
    const badge = container.querySelector('.count-badge') as HTMLElement;

    expect(badge).toHaveTextContent('7 reports');
    expect(badge.querySelector('[aria-hidden="true"]')?.textContent).toBe('7');
  });

  it('is never brand-coloured', () => {
    expect(STYLE).not.toContain('--color-brand');
    expect(STYLE).toContain('background-color: var(--color-attention-subtle)');
    expect(STYLE).toContain('background-color: var(--color-bg-subtle)');
  });
});
