import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import ChatPanel from './ChatPanel.svelte';

/**
 * `ChatPanel`: the chat transcript, the act it proposes, and the compose row. Its controls are
 * named and, where one cannot act, it says why — as the advisory and as the description assistive
 * technology reads.
 */
describe('ChatPanel component', () => {
  const turns = [
    { role: 'operator', text: 'Can the test cover SSE?', at: 'now', untrusted: false },
  ];

  const proposal = {
    summary: 'A follow-up plan',
    after: 'Add the Codeberg issue provider on top of relay-v2.',
    applies: 'a new plan',
  };

  it('says why a control cannot act, as the advisory and as its description', () => {
    render(ChatPanel, {
      props: {
        open: true,
        subject: 'fix-relay',
        turns,
        proposal,
        busy: true,
      },
    });

    const accept = screen.getByTestId('chat-accept');
    const describedBy = accept.getAttribute('aria-describedby');

    expect(accept).toBeDisabled();
    expect(accept.getAttribute('title')).toBeTruthy();
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)?.textContent).toBe(
      accept.getAttribute('title'),
    );
  });

  it('names itself and every control it offers', () => {
    render(ChatPanel, { props: { open: true, subject: 'fix-relay', turns, proposal } });

    expect(screen.getByTestId('chat-panel')).toHaveAccessibleName('fix-relay');
    for (const button of screen.getAllByRole('button')) {
      expect(button).toHaveAccessibleName();
    }
    expect(screen.getByTestId('chat-input')).toHaveAccessibleName();
  });

  it('closes on Escape, without ending the chat the runtime holds', async () => {
    const onClose = vi.fn();
    const onEnd = vi.fn();
    render(ChatPanel, {
      props: { open: true, subject: 'fix-relay', turns, onClose, onEnd },
    });

    await fireEvent.keyDown(screen.getByTestId('chat-panel'), { key: 'Escape' });

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onEnd).not.toHaveBeenCalled();
  });

  it('offers no chat input while the chat cannot run, and says why', () => {
    render(ChatPanel, {
      props: { open: true, subject: 'fix-relay', available: false, disabledReason: 'start.chat.waits' },
    });

    expect(screen.getByTestId('chat-disabled')).toBeInTheDocument();
    expect(screen.queryByTestId('chat-input')).not.toBeInTheDocument();
  });
});
