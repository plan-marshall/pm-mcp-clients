import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import Toast from './Toast.svelte';

/**
 * `Toast` (VD-P-05, VD-S-03): a short confirmation in the bottom right corner, four seconds on
 * screen, with an optional act, announced in a live region so it reaches a screen reader without
 * taking the focus. The region is in the document while the toast is empty, so the message is an
 * update the reader announces rather than a node it never saw.
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

  it('keeps its live region in the document while it is closed, and empty', () => {
    const { rerender } = render(Toast, { props: { open: false, message: 'The plan was deleted.' } });
    const region = screen.getByRole('status');

    expect(region.textContent?.trim()).toBe('');

    // The same region takes the message when the toast opens: an update, not a new node.
    return rerender({ open: true, message: 'The plan was deleted.' }).then(() => {
      expect(screen.getByRole('status')).toBe(region);
      expect(region).toHaveTextContent('The plan was deleted.');
    });
  });

  it('keeps the act beside the live region, not inside it', async () => {
    const onAction = vi.fn();
    render(Toast, {
      props: { open: true, message: 'The plan was deleted.', actionLabel: 'Undo', onAction },
    });
    const region = screen.getByRole('status');
    const act = screen.getByRole('button', { name: 'Undo' });

    expect(region.contains(act)).toBe(false);

    await fireEvent.click(act);
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

  it('does not restart its four seconds when the view re-renders while it is open', async () => {
    vi.useFakeTimers();
    const onDismiss = vi.fn();
    const { rerender } = render(Toast, {
      props: { open: true, message: 'The plan was deleted.', onDismiss },
    });

    vi.advanceTimersByTime(3000);
    await rerender({ open: true, message: 'The plan was deleted.', onDismiss });
    vi.advanceTimersByTime(1000);

    expect(onDismiss).toHaveBeenCalledOnce();
  });

  it('fires once, however long the view leaves the toast open', () => {
    vi.useFakeTimers();
    const onDismiss = vi.fn();
    render(Toast, { props: { open: true, message: 'The plan was deleted.', onDismiss } });

    vi.advanceTimersByTime(12_000);

    expect(onDismiss).toHaveBeenCalledOnce();
  });

  it('counts again when the view opens it a second time', async () => {
    vi.useFakeTimers();
    const onDismiss = vi.fn();
    const { rerender } = render(Toast, {
      props: { open: true, message: 'The plan was deleted.', onDismiss },
    });
    vi.advanceTimersByTime(4000);

    await rerender({ open: false, message: 'The plan was deleted.', onDismiss });
    await rerender({ open: true, message: 'The plan was deleted.', onDismiss });
    vi.advanceTimersByTime(4000);

    expect(onDismiss).toHaveBeenCalledTimes(2);
  });
});
