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

  it('keeps the focus on the act a person chose outside the menu', async () => {
    const onAct = vi.fn();
    render(ActionBar, { props: { acts: FIVE_ACTS, onAct } });
    const pause = screen.getByRole('button', { name: 'Pause' });

    await fireEvent.click(pause);

    expect(onAct).toHaveBeenCalledWith('pause');
    expect(document.activeElement).toBe(pause);
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

  it('carries the advisory of an act, primary included', () => {
    render(ActionBar, {
      props: {
        acts: [
          { id: 'next', label: 'Next', advisory: 'Start the plan that is staged' },
          { id: 'pause', label: 'Pause' },
        ],
      },
    });

    expect(screen.getByRole('button', { name: 'Next' }).getAttribute('title')).toBe(
      'Start the plan that is staged',
    );
  });

  it('puts the focus on the first entry, moves it with the arrow keys and gives it back', async () => {
    render(ActionBar, {
      props: {
        acts: [
          { id: 'next', label: 'Next' },
          { id: 'pause', label: 'Pause' },
          { id: 'chat', label: 'Chat' },
          { id: 'settings', label: 'Settings' },
          { id: 'archive', label: 'Archive' },
          { id: 'copy', label: 'Copy' },
        ],
      },
    });
    const overflow = screen.getByTestId('action-bar-overflow');

    await fireEvent.click(overflow);
    expect(document.activeElement).toBe(screen.getByRole('menuitem', { name: 'Archive' }));

    await fireEvent.keyDown(screen.getByRole('menu'), { key: 'ArrowDown' });
    expect(document.activeElement).toBe(screen.getByRole('menuitem', { name: 'Copy' }));

    await fireEvent.keyDown(screen.getByRole('menu'), { key: 'ArrowUp' });
    expect(document.activeElement).toBe(screen.getByRole('menuitem', { name: 'Archive' }));

    await fireEvent.keyDown(screen.getByRole('menu'), { key: 'Escape' });
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    expect(document.activeElement).toBe(overflow);
  });

  it('names the reason of a disabled entry and keeps it reachable with the arrow keys', async () => {
    const onAct = vi.fn();
    render(ActionBar, {
      props: {
        acts: [
          { id: 'next', label: 'Next' },
          { id: 'pause', label: 'Pause' },
          { id: 'chat', label: 'Chat' },
          { id: 'settings', label: 'Settings' },
          { id: 'later', label: 'Later', disabled: true, disabledReason: 'Not yet' },
          { id: 'archive', label: 'Archive' },
        ],
        onAct,
      },
    });

    await fireEvent.click(screen.getByTestId('action-bar-overflow'));

    // Opening the menu still starts on the first entry that can run.
    expect(document.activeElement).toBe(screen.getByRole('menuitem', { name: 'Archive' }));

    const later = screen.getByRole('menuitem', { name: 'Later' });
    expect(later).toHaveAttribute('aria-disabled', 'true');
    const describedBy = later.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)).toHaveTextContent('Not yet');

    await fireEvent.keyDown(screen.getByRole('menu'), { key: 'ArrowUp' });
    expect(document.activeElement).toBe(later);

    await fireEvent.click(later);
    expect(onAct).not.toHaveBeenCalled();
  });

  it('keeps the reason of a disabled menu entry out of the menu itself', async () => {
    render(ActionBar, {
      props: {
        acts: [
          { id: 'next', label: 'Next' },
          { id: 'pause', label: 'Pause' },
          { id: 'chat', label: 'Chat' },
          { id: 'settings', label: 'Settings' },
          { id: 'later', label: 'Later', disabled: true, disabledReason: 'Not yet' },
        ],
      },
    });

    await fireEvent.click(screen.getByTestId('action-bar-overflow'));
    const menu = screen.getByRole('menu');
    // A menu holds menu items; the reason is described, not drawn as an entry of its own.
    expect(menu.querySelector('span')).toBeNull();

    const later = screen.getByRole('menuitem', { name: 'Later' });
    const describedBy = later.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)).toHaveTextContent('Not yet');
  });

  it('closes on Tab and gives the focus back to the button', async () => {
    render(ActionBar, { props: { acts: FIVE_ACTS } });
    const overflow = screen.getByTestId('action-bar-overflow');

    await fireEvent.click(overflow);
    await fireEvent.keyDown(screen.getByRole('menu'), { key: 'Tab' });

    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    expect(document.activeElement).toBe(overflow);
  });

  it('moves to the first and the last entry with Home and End', async () => {
    render(ActionBar, {
      props: {
        acts: [
          { id: 'next', label: 'Next' },
          { id: 'pause', label: 'Pause' },
          { id: 'chat', label: 'Chat' },
          { id: 'settings', label: 'Settings' },
          { id: 'archive', label: 'Archive' },
          { id: 'copy', label: 'Copy' },
          { id: 'retry', label: 'Retry' },
        ],
      },
    });
    await fireEvent.click(screen.getByTestId('action-bar-overflow'));

    await fireEvent.keyDown(screen.getByRole('menu'), { key: 'End' });
    expect(document.activeElement).toBe(screen.getByRole('menuitem', { name: 'Retry' }));

    await fireEvent.keyDown(screen.getByRole('menu'), { key: 'Home' });
    expect(document.activeElement).toBe(screen.getByRole('menuitem', { name: 'Archive' }));
  });

  it('renders the role an act carries, not its position', async () => {
    const onAct = vi.fn();
    render(ActionBar, {
      props: {
        acts: [
          { id: 'chat', label: 'Chat', role: 'secondary' },
          { id: 'start', label: 'Start', role: 'primary' },
          { id: 'settings', label: 'Settings', role: 'overflow' },
          { id: 'pause', label: 'Pause', role: 'secondary' },
        ],
        onAct,
      },
    });

    const start = screen.getByRole('button', { name: 'Start' });
    expect(start.className).toContain('btn--primary');
    expect(screen.getByRole('button', { name: 'Chat' }).className).toContain('btn--secondary');
    expect(screen.queryByText('Settings')).not.toBeInTheDocument();

    await fireEvent.click(start);
    expect(onAct).toHaveBeenCalledWith('start');
  });

  it('keeps a destructive act out of the bar, in the overflow', async () => {
    render(ActionBar, {
      props: {
        acts: [
          { id: 'chat', label: 'Chat', role: 'primary' },
          { id: 'revoke', label: 'Revoke it', role: 'overflow', destructive: true },
        ],
      },
    });

    expect(screen.queryByText('Revoke it')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Revoke it' })).not.toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('action-bar-overflow'));
    expect(screen.getByRole('menuitem', { name: 'Revoke it' })).toBeInTheDocument();
  });

  it('never draws a destructive act as the primary act, whatever the caller marks', () => {
    render(ActionBar, {
      props: {
        acts: [
          { id: 'revoke', label: 'Revoke it', role: 'primary', destructive: true },
          { id: 'chat', label: 'Chat', role: 'secondary' },
        ],
      },
    });

    expect(screen.queryByRole('button', { name: 'Revoke it' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Chat' })).toBeInTheDocument();
  });

  it('keeps a destructive act out of a roleless list’s bar too', async () => {
    render(ActionBar, {
      props: {
        acts: [
          { id: 'chat', label: 'Chat' },
          { id: 'revoke', label: 'Revoke it', destructive: true },
        ],
      },
    });

    expect(screen.getByRole('button', { name: 'Chat' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Revoke it' })).not.toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('action-bar-overflow'));
    expect(screen.getByRole('menuitem', { name: 'Revoke it' })).toBeInTheDocument();
  });

  it('gives every act its id as its test id', async () => {
    render(ActionBar, {
      props: {
        acts: [
          { id: 'start', label: 'Start', role: 'primary' },
          { id: 'chat', label: 'Chat', role: 'secondary' },
          { id: 'archive', label: 'Archive', role: 'overflow' },
        ],
      },
    });

    expect(screen.getByTestId('action-start')).toBeInTheDocument();
    expect(screen.getByTestId('action-chat')).toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('action-bar-overflow'));
    expect(screen.getByTestId('action-archive')).toBeInTheDocument();
  });
});
