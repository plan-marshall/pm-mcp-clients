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

    const allStandaloneLabels = [...moreMenuButtons, ...markReadButtons].map(
      (btn) => btn.getAttribute('aria-label'),
    );
    const uniqueLabels = new Set(allStandaloneLabels);
    expect(uniqueLabels.size).toBe(4);
  });
});
