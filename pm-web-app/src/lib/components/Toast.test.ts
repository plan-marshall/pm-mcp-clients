import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import Toast from './Toast.svelte';

/**
 * `Toast` (VD-P-05, VD-S-03): a short confirmation in the bottom right corner, four seconds on
 * screen, with an optional act, announced in a live region so it reaches a screen reader without
 * taking the focus.
 */

afterEach(() => {
  vi.useRealTimers();
});

describe('Toast', () => {
  it('announces its message in a live region', () => {
    render(Toast, { props: { open: true, message: 'The plan was deleted.' } });
    const region = screen.getByRole('status');

    expect(region).toHaveAttribute('aria-live', 'polite');
    expect(region).toHaveTextContent('The plan was deleted.');
  });

  it('carries the act it is given and reports it', async () => {
    const onAction = vi.fn();
    render(Toast, {
      props: { open: true, message: 'The plan was deleted.', actionLabel: 'Undo', onAction },
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Undo' }));

    expect(onAction).toHaveBeenCalledOnce();
  });

  it('draws no act when the view names none', () => {
    render(Toast, { props: { open: true, message: 'The plan was deleted.' } });

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('takes itself off after four seconds', () => {
    vi.useFakeTimers();
    const onDismiss = vi.fn();
    render(Toast, { props: { open: true, message: 'The plan was deleted.', onDismiss } });

    vi.advanceTimersByTime(3999);
    expect(onDismiss).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);
    expect(onDismiss).toHaveBeenCalledOnce();
  });

  it('draws nothing while it is closed', () => {
    render(Toast, { props: { open: false, message: 'The plan was deleted.' } });

    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });
});
