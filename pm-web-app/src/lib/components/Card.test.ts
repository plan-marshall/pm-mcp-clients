import { describe, it, expect, vi } from 'vitest';
import { render, screen, within } from '@testing-library/svelte';
import Card from './Card.svelte';
import CardFixture from '../../test/fixtures/CardFixture.svelte';
import CardInsideViewCardFixture from '../../test/fixtures/CardInsideViewCardFixture.svelte';
import NestedCardsFixture from '../../test/fixtures/NestedCardsFixture.svelte';

/**
 * `Card` (VD-P-05, VD-S-03): the surface of a block with an optional head that carries a title and
 * the acts of that head. A card inside a card is refused — the nested one draws a divider and the
 * development build warns — so no second surface ever appears.
 */

describe('Card', () => {
  it('draws the surface as the card root', () => {
    const { container } = render(Card, { props: {} });
    const card = container.querySelector('.card');

    expect(card).not.toBeNull();
    expect(card?.tagName).toBe('SECTION');
    expect(card?.hasAttribute('data-card')).toBe(true);
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
    const head = screen.getByTestId('card').querySelector('.card__head') as HTMLElement;

    expect(head).not.toBeNull();
    expect(within(head).getByRole('button', { name: 'Refresh' })).toBeInTheDocument();
    expect(screen.queryByRole('heading')).not.toBeInTheDocument();
  });

  it('takes the heading level the view gives it, so the outline stays in order', () => {
    render(Card, { props: { title: 'Runtime', titleLevel: 3 } });

    expect(screen.getByRole('heading', { name: 'Runtime', level: 3 })).toBeInTheDocument();
  });

  it('refuses a card inside a card: the nested one draws a divider, not a surface', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const { container } = render(NestedCardsFixture, {
      props: {
        outerTitle: 'Outer',
        innerTitle: 'Inner',
        body: 'Body text',
        innerActLabel: 'Copy',
      },
    });
    const nested = container.querySelector('.card__nested') as HTMLElement;

    expect(container.querySelectorAll('.card')).toHaveLength(1);
    expect(nested).not.toBeNull();
    expect(nested.querySelector('.card__divider')).not.toBeNull();
    expect(within(nested).getByRole('heading', { name: 'Inner', level: 3 })).toBeInTheDocument();
    expect(within(nested).getByText('Body text')).toBeInTheDocument();
    expect(within(nested).getByRole('button', { name: 'Copy' })).toBeInTheDocument();
    expect(warn).toHaveBeenCalledWith(expect.stringContaining('nested'));
  });

  it('draws a surface inside a view element the view calls a card', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const { container } = render(CardInsideViewCardFixture, {
      props: { title: 'Runtime', body: 'Body text' },
    });

    expect(container.querySelector('.card > .card')).not.toBeNull();
    expect(container.querySelector('.card__nested')).toBeNull();
    expect(warn).not.toHaveBeenCalled();
  });
});
