import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import EmptyState from './EmptyState.svelte';
import { MASCOT_POSES } from '../assets/mascot/mascotFiles';

/**
 * `EmptyState` (VD-P-05, VD-P-08, VD-S-04): the mascot's bust or an icon, a title, one sentence and
 * one act. It is the block a list draws instead of an empty list, so it always says what is
 * missing and what the person can do about it.
 */

const TITLE = 'No plan is running';
const MESSAGE = 'Start a plan from a project, or open an epic to see its plans.';
const ACT = 'Open the projects';

describe('EmptyState', () => {
  it('draws its title, its sentence and its act', () => {
    const onAct = vi.fn();
    render(EmptyState, {
      props: { title: TITLE, message: MESSAGE, actionLabel: ACT, onAct },
    });

    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: TITLE })).toBeInTheDocument();
    expect(screen.getByText(MESSAGE)).toBeInTheDocument();

    screen.getByRole('button', { name: ACT }).click();

    expect(onAct).toHaveBeenCalledTimes(1);
  });

  it('draws the one act the view names', () => {
    render(EmptyState, { props: { title: TITLE, message: MESSAGE, actionLabel: ACT } });

    expect(screen.getAllByRole('button')).toHaveLength(1);
  });

  it('draws no act when the view names none', () => {
    render(EmptyState, { props: { title: TITLE, message: MESSAGE } });

    expect(screen.queryAllByRole('button')).toHaveLength(0);
  });

  it.each(MASCOT_POSES)('draws the mascot bust in the pose %s above the title', (pose) => {
    render(EmptyState, { props: { title: TITLE, message: MESSAGE, mascot: pose } });
    const figure = screen.getByTestId('mascot-figure');
    const title = screen.getByRole('heading', { name: TITLE });

    expect(figure).toHaveAttribute('data-pose', pose);
    expect(figure).toHaveAttribute('data-crop', 'bust');
    expect(figure.compareDocumentPosition(title) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('draws the bust within 120 to 160 px', () => {
    render(EmptyState, { props: { title: TITLE, message: MESSAGE, mascot: 'all-clear' } });
    const height = Number(screen.getByTestId('mascot-figure').getAttribute('height'));

    expect(height).toBeGreaterThanOrEqual(120);
    expect(height).toBeLessThanOrEqual(160);
  });

  it('draws no icon beside the mascot, even when the view names one', () => {
    const { container } = render(EmptyState, {
      props: { title: TITLE, message: MESSAGE, mascot: 'start-here', icon: 'kind.plan' },
    });

    expect(screen.getByTestId('mascot-figure')).toBeInTheDocument();
    expect(container.querySelector('[data-icon]')).toBeNull();
  });

  it('draws the icon the view names on an empty state that is not calm, and no figure', () => {
    const { container } = render(EmptyState, {
      props: { title: TITLE, message: MESSAGE, icon: 'kind.plan' },
    });
    const mark = container.querySelector('[data-icon]');

    expect(mark).not.toBeNull();
    expect(mark).toHaveAttribute('data-icon', 'kind.plan');
    expect(screen.queryByTestId('mascot-figure')).not.toBeInTheDocument();
    expect(screen.queryByTestId('brand-mark')).not.toBeInTheDocument();
  });

  it('takes the heading level the view gives it, so the outline stays in order', () => {
    render(EmptyState, { props: { title: TITLE, message: MESSAGE, titleLevel: 4 } });

    expect(screen.getByRole('heading', { name: TITLE, level: 4 })).toBeInTheDocument();
  });
});
