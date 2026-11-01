import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import ErrorState from './ErrorState.svelte';

/**
 * `ErrorState` (VD-P-05, VD-S-04): an icon, a title, one sentence and one act, with the technical
 * code folded under `Details`. The sentence carries what a person can do; the code is there for the
 * report, folded away so it never leads.
 */

const TITLE = 'The plan could not be read';
const MESSAGE = 'Try again, or open the runtime log to see what the daemon answered.';
const ACT = 'Try again';
const DETAILS = 'Details';
const CODE = 'pm-413: project-not-enrolled';

describe('ErrorState', () => {
  it('draws its icon, its title, its sentence and its act', () => {
    const onAct = vi.fn();
    render(ErrorState, {
      props: { title: TITLE, message: MESSAGE, actionLabel: ACT, onAct },
    });

    expect(screen.getByTestId('error-state')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: TITLE })).toBeInTheDocument();
    expect(screen.getByText(MESSAGE)).toBeInTheDocument();
    expect(screen.getByTestId('error-state').querySelector('[data-icon]')).toHaveAttribute(
      'data-icon',
      'state.error',
    );

    screen.getByRole('button', { name: ACT }).click();

    expect(onAct).toHaveBeenCalledTimes(1);
  });

  it('folds the technical code under Details, closed', () => {
    const { container } = render(ErrorState, {
      props: { title: TITLE, message: MESSAGE, code: CODE, detailsLabel: DETAILS },
    });
    const details = container.querySelector('details');

    expect(details).not.toBeNull();
    expect(details).not.toHaveAttribute('open');
    expect(details?.querySelector('summary')?.textContent?.trim()).toBe(DETAILS);
    expect(details?.querySelector('code')?.textContent).toBe(CODE);
  });

  it('draws no Details block when there is no technical code', () => {
    const { container } = render(ErrorState, {
      props: { title: TITLE, message: MESSAGE, detailsLabel: DETAILS },
    });

    expect(container.querySelector('details')).toBeNull();
  });

  it('draws no act when the view names none', () => {
    render(ErrorState, { props: { title: TITLE, message: MESSAGE } });

    expect(screen.queryAllByRole('button')).toHaveLength(0);
  });
});
