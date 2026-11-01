import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import EmptyState from './EmptyState.svelte';

/**
 * `EmptyState` (VD-P-05, VD-P-08, VD-S-04): an icon or the brand mark, a title, one sentence and
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

  it('draws the brand mark, as the empty states of VD-P-08 do', () => {
    const { container } = render(EmptyState, {
      props: { title: TITLE, message: MESSAGE, brand: true },
    });

    expect(screen.getByTestId('brand-mark')).toBeInTheDocument();
    expect(container.querySelector('.ui-icon')).toBeNull();
  });

  it('draws the icon the view names instead of the mark', () => {
    const { container } = render(EmptyState, {
      props: { title: TITLE, message: MESSAGE, icon: 'kind.plan' },
    });
    const mark = container.querySelector('[data-icon]');

    expect(mark).not.toBeNull();
    expect(mark).toHaveAttribute('data-icon', 'kind.plan');
    expect(screen.queryByTestId('brand-mark')).not.toBeInTheDocument();
  });

  it('takes the heading level the view gives it, so the outline stays in order', () => {
    render(EmptyState, { props: { title: TITLE, message: MESSAGE, titleLevel: 4 } });

    expect(screen.getByRole('heading', { name: TITLE, level: 4 })).toBeInTheDocument();
  });
});
