import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import path from 'node:path';
import TriagePageFixture from '../../test/fixtures/TriagePageFixture.svelte';
import { styleOf, ruleOf } from '../../test/templateStyle';

/**
 * `TriagePage` (UX-P-03, template A): the list on the left, the detail on the right, the filter
 * chips above both. The page takes the full width and splits it 40 / 60; below 1100 px the detail
 * replaces the list instead of sitting beside it.
 */

const SOURCE = path.resolve(__dirname, './TriagePage.svelte');

const PROPS = {
  filters: 'Open decisions · Needs you',
  list: 'Decision list',
  detail: 'Decision detail',
};

describe('TriagePage', () => {
  it('renders the filter chips, the list and the detail through its slots', () => {
    render(TriagePageFixture, { props: PROPS });

    expect(screen.getByTestId('triage-page')).toBeInTheDocument();
    expect(screen.getByText('Open decisions · Needs you')).toBeInTheDocument();
    expect(screen.getByText('Decision list')).toBeInTheDocument();
    expect(screen.getByText('Decision detail')).toBeInTheDocument();
  });

  it('takes the full width and splits it 40 / 60 between list and detail', () => {
    const root = ruleOf(styleOf(SOURCE), '.triage-page');

    expect(root).toContain('grid-template-columns: minmax(0, 2fr) minmax(0, 3fr)');
    expect(root).toContain('width: 100%');
    expect(root).not.toMatch(/max-width\s*:/);
  });

  it('collapses below 1100 px, where the detail replaces the list', () => {
    const style = styleOf(SOURCE);
    const index = style.indexOf('@media (max-width: 1099px)');

    expect(index).toBeGreaterThanOrEqual(0);
    const collapsed = style.slice(index);
    expect(collapsed).toContain('grid-template-columns: minmax(0, 1fr)');
    expect(collapsed).toMatch(/\.triage-page--detail \.triage-page__list[\s\S]*?display: none/);
    expect(collapsed).toMatch(
      /\.triage-page:not\(\.triage-page--detail\) \.triage-page__detail[\s\S]*?display: none/,
    );
  });

  it('marks the detail mode on the root, so the narrow layout can show the detail', () => {
    const { container } = render(TriagePageFixture, { props: { ...PROPS, selected: true } });

    expect(container.querySelector('.triage-page--detail')).not.toBeNull();
  });
});
