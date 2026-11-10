import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import DevComponentsPage from './+page.svelte';

describe('/dev/components page fixture', () => {
  it('assigns unique accessible names to RowMenu fixture instances across themes (D2)', () => {
    render(DevComponentsPage);

    const moreMenuButtons = screen.getAllByRole('button', { name: /Row menu \(More/i });
    expect(moreMenuButtons).toHaveLength(2);

    const markReadButtons = screen.getAllByRole('button', { name: /Row menu \(Mark read/i });
    expect(markReadButtons).toHaveLength(2);

    const labels = [...moreMenuButtons, ...markReadButtons].map(
      (button) => button.getAttribute('aria-label') ?? '',
    );
    // One unique accessible name per fixture per theme: dropping the theme label collapses a pair.
    expect(new Set(labels).size).toBe(4);

    // Each fixture carries the theme's own label, never the raw theme key.
    expect(labels.filter((label) => label.endsWith(', Light theme)'))).toHaveLength(2);
    expect(labels.filter((label) => label.endsWith(', Dark theme)'))).toHaveLength(2);
    expect(labels.some((label) => label.endsWith(', light)') || label.endsWith(', dark)'))).toBe(
      false,
    );
  });
});
