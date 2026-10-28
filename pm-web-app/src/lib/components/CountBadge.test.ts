import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import CountBadge from './CountBadge.svelte';

/**
 * `CountBadge` (VD-P-05): a pill with a number, its attention fill for "needs you" and the neutral
 * fill otherwise. It appears only in the rail, tabs and group heads.
 */

describe('CountBadge', () => {
  it('draws the number in the neutral fill by default', () => {
    const { container } = render(CountBadge, { props: { count: 3 } });
    const badge = container.querySelector('.count-badge');

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

  it('carries the accessible name the view gives it', () => {
    render(CountBadge, { props: { count: 3, label: '3 waiting for you' } });

    expect(screen.getByLabelText('3 waiting for you')).toBeInTheDocument();
  });
});
