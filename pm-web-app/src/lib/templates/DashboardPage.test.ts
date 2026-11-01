import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import path from 'node:path';
import DashboardPageFixture from '../../test/fixtures/DashboardPageFixture.svelte';
import { styleOf, ruleOf } from '../../test/templateStyle';

/**
 * `DashboardPage` (UX-P-03, template D): the title, the time window switch, the "updated" line and
 * the grid of cards with figures. The page is capped at 1440 px and centred.
 */

const SOURCE = path.resolve(__dirname, './DashboardPage.svelte');

const PROPS = {
  title: 'System',
  windowLabel: 'Last hour',
  updated: 'updated 2 minutes ago',
  firstCard: 'Runtime card',
  secondCard: 'Work card',
};

describe('DashboardPage', () => {
  it('renders the title, the window switch, the updated line and the card grid', () => {
    render(DashboardPageFixture, { props: PROPS });

    expect(screen.getByRole('heading', { name: 'System', level: 1 })).toBeInTheDocument();
    expect(screen.getByTestId('dashboard-page-controls')).toHaveTextContent('Last hour');
    expect(screen.getByTestId('dashboard-page-updated')).toHaveTextContent('updated 2 minutes ago');
    expect(screen.getByTestId('dashboard-page-grid')).toHaveTextContent('Runtime card');
    expect(screen.getByTestId('dashboard-page-grid')).toHaveTextContent('Work card');
  });

  it('caps the page at 1440 px and centres it', () => {
    const root = ruleOf(styleOf(SOURCE), '.dashboard-page');

    expect(root).toContain('max-width: 1440px');
    expect(root).toContain('margin-inline: auto');
    expect(root).toContain('width: 100%');
  });

  it('takes the 12 px inset on the phone', () => {
    const style = styleOf(SOURCE);
    const index = style.indexOf('@media (max-width: 480px)');

    expect(index).toBeGreaterThanOrEqual(0);
    expect(style.slice(index)).toMatch(/padding: var\(--space-3\)/);
  });
});
