import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import ConfirmDialog from './ConfirmDialog.svelte';

/**
 * `ConfirmDialog` (UX-P-04, VD-S-03): before a destructive act runs, the dialog names what it
 * costs in the danger colour; the acts that need a typed name stay refused until the operator
 * types exactly that name.
 */

const CONSEQUENCES = [
  'It is signed out at once; its open pages close with the reason.',
  'A new code pairs it again.',
];

describe('ConfirmDialog', () => {
  it('names the consequences of the destructive act', async () => {
    render(ConfirmDialog, {
      props: {
        open: true,
        title: 'Revoke Chrome on build-box?',
        consequences: CONSEQUENCES,
        actionLabel: 'Revoke it',
        cancelLabel: 'Keep it',
      },
    });

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveTextContent('Revoke Chrome on build-box?');
    for (const consequence of CONSEQUENCES) {
      expect(dialog).toHaveTextContent(consequence);
    }
  });

  it('draws the title in the danger colour', () => {
    render(ConfirmDialog, {
      props: {
        open: true,
        title: 'Revoke Chrome on build-box?',
        consequences: CONSEQUENCES,
        actionLabel: 'Revoke it',
        cancelLabel: 'Keep it',
      },
    });

    const title = screen.getByText('Revoke Chrome on build-box?');
    expect(title.closest('.dialog--danger')).not.toBeNull();
  });

  it('reports the confirm and the close', async () => {
    const onConfirm = vi.fn();
    const onClose = vi.fn();
    render(ConfirmDialog, {
      props: {
        open: true,
        title: 'Revoke it?',
        consequences: CONSEQUENCES,
        actionLabel: 'Revoke it',
        cancelLabel: 'Keep it',
        onConfirm,
        onClose,
      },
    });

    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));
    expect(onConfirm).toHaveBeenCalledOnce();

    await fireEvent.click(screen.getByTestId('confirm-dialog-cancel'));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('keeps the act refused until the exact name is typed', async () => {
    const onConfirm = vi.fn();
    render(ConfirmDialog, {
      props: {
        open: true,
        title: 'Revoke all others?',
        consequences: CONSEQUENCES,
        actionLabel: 'Revoke them',
        cancelLabel: 'Keep them',
        typedName: 'Safari on iPad',
        onConfirm,
      },
    });

    const confirm = screen.getByTestId('confirm-dialog-confirm');
    expect(confirm).toBeDisabled();

    await fireEvent.input(screen.getByTestId('confirm-dialog-input'), {
      target: { value: 'safari on ipad' },
    });
    expect(confirm).toBeDisabled();

    await fireEvent.input(screen.getByTestId('confirm-dialog-input'), {
      target: { value: 'Safari on iPad' },
    });
    expect(confirm).toBeEnabled();

    await fireEvent.click(confirm);
    expect(onConfirm).toHaveBeenCalledOnce();
  });

  it('carries no typed-name field where the act needs none', () => {
    render(ConfirmDialog, {
      props: {
        open: true,
        title: 'Stop now?',
        consequences: CONSEQUENCES,
        actionLabel: 'Stop now',
        cancelLabel: 'Keep it',
      },
    });

    expect(screen.queryByTestId('confirm-dialog-input')).not.toBeInTheDocument();
    expect(screen.getByTestId('confirm-dialog-confirm')).toBeEnabled();
  });

  it('ignores a double confirm and keeps Escape and cancel inert while the act runs', async () => {
    const onConfirm = vi.fn();
    const onClose = vi.fn();
    render(ConfirmDialog, {
      props: {
        open: true,
        title: 'Stop now?',
        consequences: CONSEQUENCES,
        actionLabel: 'Stop now',
        cancelLabel: 'Keep it',
        busy: true,
        onConfirm,
        onClose,
      },
    });

    const confirm = screen.getByTestId('confirm-dialog-confirm');
    await fireEvent.click(confirm);
    await fireEvent.click(confirm);
    expect(onConfirm).not.toHaveBeenCalled();

    const cancel = screen.getByTestId('confirm-dialog-cancel');
    expect(cancel).toBeDisabled();
    await fireEvent.click(cancel);
    expect(onClose).not.toHaveBeenCalled();

    await fireEvent.keyDown(window, { key: 'Escape' });
    expect(onClose).not.toHaveBeenCalled();
  });

  it('ignores a confirm that reaches the handler past the disabled button', async () => {
    // Arrange: the act runs, so the button is disabled — and the handler is still reached by a
    // click dispatched on it, which the browser's own disabled handling does not stop.
    const onConfirm = vi.fn();
    render(ConfirmDialog, {
      props: {
        open: true,
        title: 'Stop now?',
        consequences: CONSEQUENCES,
        actionLabel: 'Stop now',
        cancelLabel: 'Keep it',
        busy: true,
        onConfirm,
      },
    });
    const confirm = screen.getByTestId('confirm-dialog-confirm');
    expect(confirm).toBeDisabled();

    // Act: two clicks reach `confirm()` without the button's disabled state in the way.
    confirm.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
    confirm.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));

    // Assert: the busy guard is what refuses them.
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('draws nothing while it is closed', () => {
    render(ConfirmDialog, {
      props: {
        open: false,
        title: 'Revoke it?',
        consequences: CONSEQUENCES,
        actionLabel: 'Revoke it',
        cancelLabel: 'Keep it',
      },
    });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('asks for the name again after a closed confirmation is reopened', async () => {
    const { rerender } = render(ConfirmDialog, {
      props: {
        open: true,
        title: 'Revoke all others?',
        consequences: CONSEQUENCES,
        actionLabel: 'Revoke them',
        cancelLabel: 'Keep them',
        typedName: 'Safari on iPad',
      },
    });

    await fireEvent.input(screen.getByTestId('confirm-dialog-input'), {
      target: { value: 'Safari on iPad' },
    });
    expect(screen.getByTestId('confirm-dialog-confirm')).toBeEnabled();

    await rerender({ open: false });
    await rerender({ open: true });

    expect(screen.getByTestId('confirm-dialog-confirm')).toBeDisabled();
    expect(screen.getByTestId('confirm-dialog-input')).toHaveValue('');
  });
});
