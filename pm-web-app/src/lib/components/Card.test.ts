import { describe, it, expect, vi } from 'vitest';
import { render, screen, within } from '@testing-library/svelte';
import Card from './Card.svelte';
import CardFixture from '../../test/fixtures/CardFixture.svelte';
import NestedCardsFixture from '../../test/fixtures/NestedCardsFixture.svelte';

/**
 * `Card` (VD-P-05, VD-S-03): the surface of a block with an optional head that carries a title and
 * the acts of that head. A card inside a card is refused — the nested one draws a divider and the
 * development build warns — so no second surface ever appears.
 */

describe('Card', () => {
  it('draws the surface with the padding and radius of the design system', () => {
    const { container } = render(Card, { props: {} });
    const card = container.querySelector('.card');

    expect(card).not.toBeNull();
    expect(screen.getByTestId('card')).toBe(card);
  });

  it('carries a head with its title and the acts a view puts beside it', () => {
    render(CardFixture, {
      props: { title: 'Runtime', body: 'Body text', actLabel: 'Refresh' },
    });
    const head = screen.getByTestId('card').querySelector('.card__head') as HTMLElement;

    expect(head).not.toBeNull();
    expect(within(head).getByRole('heading', { name: 'Runtime' })).toBeInTheDocument();
    expect(within(head).getByRole('button', { name: 'Refresh' })).toBeInTheDocument();
    expect(screen.getByTestId('card').querySelector('.card__body')?.textContent).toContain(
      'Body text',
    );
  });

  it('draws no head when the view gives neither a title nor an act', () => {
    const { container } = render(Card, { props: {} });

    expect(container.querySelector('.card__head')).toBeNull();
  });

  it('draws the head of acts alone, without an empty title', () => {
    render(CardFixture, { props: { title: '', body: 'Body text', actLabel: 'Refresh' } });
    const head = screen.getByTestId('card').querySelector('.card__head');

    expect(head).not.toBeNull();
    expect(screen.queryByRole('heading')).not.toBeInTheDocument();
  });

  it('refuses a card inside a card: the nested one draws a divider, not a surface', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const { container } = render(NestedCardsFixture, {
      props: { outerTitle: 'Outer', innerTitle: 'Inner', body: 'Body text' },
    });

    expect(container.querySelectorAll('.card')).toHaveLength(1);
    expect(container.querySelector('.card__nested')).not.toBeNull();
    expect(container.querySelector('.card__nested .card__divider')).not.toBeNull();
    expect(warn).toHaveBeenCalledWith(expect.stringContaining('nested'));
  });
});
