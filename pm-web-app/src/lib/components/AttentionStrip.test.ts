import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import AttentionStrip from './AttentionStrip.svelte';

/**
 * `AttentionStrip` (VD-P-05, VD-S-03, UX-P-05): a block in `--color-attention-subtle` with a dot,
 * one sentence and one act — the one thing that waits for the operator, never a list of them.
 */

describe('AttentionStrip', () => {
  it('draws the dot, the sentence and the one act', () => {
    render(AttentionStrip, {
      props: {
        message: 'The machine is not reachable over the network now.',
        actionLabel: 'Check the network',
      },
    });
    const strip = screen.getByTestId('attention-strip');

    expect(strip).toHaveTextContent('The machine is not reachable over the network now.');
    expect(strip.querySelector('.attention-strip__dot')).not.toBeNull();
    expect(screen.getByRole('button', { name: 'Check the network' })).toBeInTheDocument();
  });

  it('reports the act', async () => {
    const onAct = vi.fn();
    render(AttentionStrip, {
      props: { message: 'The machine is not reachable.', actionLabel: 'Check', onAct },
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Check' }));

    expect(onAct).toHaveBeenCalledOnce();
  });

  it('draws the sentence alone when the view offers no act', () => {
    render(AttentionStrip, { props: { message: 'The machine is not reachable.' } });

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
