import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import ActionBar from './ActionBar.svelte';

/**
 * `ActionBar` (VD-P-05, VP-UX-04): one primary act, up to three secondary acts, and an overflow
 * `IconButton` that holds the rest in a menu. This is the bar's shape; which acts a state offers
 * comes with `web-actions-by-state`.
 */

const FIVE_ACTS = [
  { id: 'next', label: 'Start the next plan' },
  { id: 'pause', label: 'Pause' },
  { id: 'chat', label: 'Chat' },
  { id: 'settings', label: 'Settings' },
  { id: 'archive', label: 'Archive' },
];

describe('ActionBar', () => {
  it('draws one primary, three secondary and holds the fifth act in the overflow', async () => {
    const { container } = render(ActionBar, { props: { acts: FIVE_ACTS } });

    expect(container.querySelectorAll('.btn--primary')).toHaveLength(1);
    expect(container.querySelectorAll('.btn--secondary')).toHaveLength(3);
    const overflow = screen.getByTestId('action-bar-overflow');
    expect(within(container).queryByText('Archive')).toBeNull();

    await fireEvent.click(overflow);

    const menu = screen.getByRole('menu');
    expect(within(menu).getByText('Archive')).toBeInTheDocument();
    expect(within(menu).getAllByRole('menuitem')).toHaveLength(1);
  });

  it('needs no overflow while every act fits', () => {
    render(ActionBar, { props: { acts: FIVE_ACTS.slice(0, 3) } });

    expect(screen.queryByTestId('action-bar-overflow')).not.toBeInTheDocument();
  });

  it('reports the act a person chose', async () => {
    const onAct = vi.fn();
    render(ActionBar, { props: { acts: FIVE_ACTS, onAct } });

    await fireEvent.click(screen.getByText('Pause'));

    expect(onAct).toHaveBeenCalledWith('pause');
  });

  it('reports an act chosen from the overflow menu, and closes the menu', async () => {
    const onAct = vi.fn();
    render(ActionBar, { props: { acts: FIVE_ACTS, onAct } });

    await fireEvent.click(screen.getByTestId('action-bar-overflow'));
    await fireEvent.click(screen.getByRole('menuitem'));

    expect(onAct).toHaveBeenCalledWith('archive');
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('says why an act cannot run', () => {
    render(ActionBar, {
      props: {
        acts: [{ id: 'next', label: 'Next', disabled: true, disabledReason: 'Every slot is busy' }],
      },
    });

    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' }).getAttribute('title')).toBe(
      'Every slot is busy',
    );
  });
});
