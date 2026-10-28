import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import { tick } from 'svelte';
import Chip from './Chip.svelte';

/**
 * `Chip` (VD-P-05): an id, branch or version in `--text-xs` monospace on `--color-bg-subtle`,
 * copied on click. The value stays the visible label; the copy is the act.
 */

const originalClipboard = navigator.clipboard;

afterEach(() => {
  vi.useRealTimers();
  Object.defineProperty(navigator, 'clipboard', {
    value: originalClipboard,
    configurable: true,
  });
});

/** @param writeText the clipboard writer the test hands the component */
function stubClipboard(writeText: (text: string) => Promise<void>): void {
  Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
}

describe('Chip', () => {
  it('draws the value in monospace', () => {
    render(Chip, { props: { value: 'plan-marshall/relay@a0f80a3f' } });
    const chip = screen.getByRole('button');

    expect(chip).toHaveClass('chip');
    expect(chip.textContent).toContain('plan-marshall/relay@a0f80a3f');
  });

  it('keeps its live region in place before it has anything to say', () => {
    const { container } = render(Chip, { props: { value: 'a0f80a3f' } });

    expect(container.querySelector('[aria-live="polite"]')).not.toBeNull();
    expect(container.querySelector('[aria-live="polite"]')?.textContent).toBe('');
  });

  it('copies the value on click and says so', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    stubClipboard(writeText);
    render(Chip, { props: { value: 'a0f80a3f' } });

    await fireEvent.click(screen.getByRole('button'));

    expect(writeText).toHaveBeenCalledWith('a0f80a3f');
    expect(await screen.findByText('Copied')).toBeInTheDocument();
  });

  it('clears the confirmation it gave', async () => {
    vi.useFakeTimers();
    const writeText = vi.fn().mockResolvedValue(undefined);
    stubClipboard(writeText);
    render(Chip, { props: { value: 'a0f80a3f' } });

    await fireEvent.click(screen.getByRole('button'));
    expect(screen.getByText('Copied')).toBeInTheDocument();

    await vi.advanceTimersByTimeAsync(2000);
    await tick();
    expect(screen.queryByText('Copied')).not.toBeInTheDocument();
    vi.useRealTimers();
  });

  it('claims nothing when the clipboard refuses', async () => {
    const writeText = vi.fn().mockRejectedValue(new Error('refused'));
    stubClipboard(writeText);
    render(Chip, { props: { value: 'a0f80a3f' } });

    await fireEvent.click(screen.getByRole('button'));

    expect(screen.queryByText('Copied')).not.toBeInTheDocument();
  });

  it('carries the advisory of the copy act', () => {
    render(Chip, { props: { value: 'a0f80a3f' } });

    expect(screen.getByRole('button').getAttribute('title')).toBe(
      'Copy this value to the clipboard',
    );
  });
});
