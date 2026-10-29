import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import StatusLabel from './StatusLabel.svelte';

/**
 * `StatusLabel` (VD-P-05): an 8 px dot in the state colour and the state word in
 * `--color-text-secondary`. One per row; it replaces the state pill.
 */

describe('StatusLabel', () => {
  it('draws the state word beside a dot that only carries the colour', () => {
    const { container } = render(StatusLabel, {
      props: { tone: 'running', label: 'running' },
    });
    const label = screen.getByText('running');
    const dot = container.querySelector('.status-label__dot');

    expect(label).toHaveClass('status-label__word');
    expect(dot).not.toBeNull();
    expect(dot?.getAttribute('aria-hidden')).toBe('true');
  });

  it.each(['neutral', 'running', 'attention', 'danger', 'success'] as const)(
    'draws the %s state colour',
    (tone) => {
      const { container } = render(StatusLabel, { props: { tone, label: 'state' } });

      expect(container.querySelector('.status-label')?.className).toContain(
        `status-label--${tone}`,
      );
    },
  );

  it('is neutral unless a view names the state colour', () => {
    const { container } = render(StatusLabel, { props: { label: 'staged' } });

    expect(container.querySelector('.status-label')?.className).toContain('status-label--neutral');
  });
});
