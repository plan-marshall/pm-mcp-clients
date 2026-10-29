import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import Dialog from './Dialog.svelte';
import DialogFixture from '../../test/fixtures/DialogFixture.svelte';

/**
 * `Dialog` (VD-P-05, VD-S-03): radius 12 px, `--shadow-overlay`, the title in `--text-xl`, and the
 * acts of the footer on the right. It is modal as the role promises: the focus opens on it, Tab
 * stays inside, Escape closes it, and the focus goes back to the control that opened it. A danger
 * dialog names the consequences in a list beside its act.
 */

const FIXTURE = {
  title: 'Delete the plan',
  body: 'The plan and its tasks are removed.',
  openLabel: 'Delete',
  cancelLabel: 'Cancel',
  confirmLabel: 'Confirm',
};

describe('Dialog', () => {
  it('opens as a modal dialog with its title, its body and the acts of its footer', async () => {
    render(DialogFixture, { props: FIXTURE });
    await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

    const dialog = await screen.findByRole('dialog');

    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(within(dialog).getByRole('heading', { name: 'Delete the plan' })).toBeInTheDocument();
    expect(within(dialog).getByText('The plan and its tasks are removed.')).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: 'Confirm' })).toBeInTheDocument();
    expect(dialog.querySelector('.dialog__footer')).not.toBeNull();
  });

  it('closes on Escape and gives the focus back to the control that opened it', async () => {
    render(DialogFixture, { props: FIXTURE });
    const opener = screen.getByRole('button', { name: 'Delete' });
    opener.focus();
    await fireEvent.click(opener);
    await screen.findByRole('dialog');

    await fireEvent.keyDown(window, { key: 'Escape' });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(document.activeElement).toBe(opener);
  });

  it('takes the focus into the dialog when it opens', async () => {
    render(DialogFixture, { props: FIXTURE });
    await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
    const dialog = await screen.findByRole('dialog');

    expect(document.activeElement).toBe(dialog);
  });

  it('keeps Tab inside the dialog, wrapping from the last act to the first', async () => {
    render(DialogFixture, { props: FIXTURE });
    await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
    const dialog = await screen.findByRole('dialog');
    const confirm = within(dialog).getByRole('button', { name: 'Confirm' });
    const cancel = within(dialog).getByRole('button', { name: 'Cancel' });

    confirm.focus();
    await fireEvent.keyDown(window, { key: 'Tab' });

    expect(document.activeElement).toBe(cancel);
  });

  it('keeps Shift-Tab inside the dialog, wrapping from the first act to the last', async () => {
    render(DialogFixture, { props: FIXTURE });
    await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
    const dialog = await screen.findByRole('dialog');
    const confirm = within(dialog).getByRole('button', { name: 'Confirm' });
    const cancel = within(dialog).getByRole('button', { name: 'Cancel' });

    cancel.focus();
    await fireEvent.keyDown(window, { key: 'Tab', shiftKey: true });

    expect(document.activeElement).toBe(confirm);
  });

  it('names the consequences of a danger act in a list', () => {
    render(Dialog, {
      props: {
        open: true,
        danger: true,
        title: 'Delete the plan',
        consequences: ['Drafts are lost.', 'Lessons are kept.'],
      },
    });
    const dialog = screen.getByRole('dialog');
    const items = within(dialog).getAllByRole('listitem');

    expect(dialog.className).toContain('dialog--danger');
    expect(items.map((item) => item.textContent)).toEqual(['Drafts are lost.', 'Lessons are kept.']);
  });

  it('draws repeated consequences, which a list must allow', () => {
    render(Dialog, {
      props: { open: true, danger: true, title: 'Delete', consequences: ['Lost.', 'Lost.'] },
    });

    expect(within(screen.getByRole('dialog')).getAllByRole('listitem')).toHaveLength(2);
  });

  it('draws no consequences for an act that is not dangerous', () => {
    render(Dialog, {
      props: { open: true, title: 'Delete the plan', consequences: ['Drafts are lost.'] },
    });

    expect(screen.queryByRole('list')).not.toBeInTheDocument();
  });

  it('draws nothing while it is closed', () => {
    render(Dialog, { props: { open: false, title: 'Delete the plan' } });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('reports a close even when the view keeps the dialog open', async () => {
    const onClose = vi.fn();
    render(Dialog, { props: { open: true, title: 'Delete the plan', onClose } });

    await fireEvent.keyDown(window, { key: 'Escape' });

    expect(onClose).toHaveBeenCalledOnce();
  });
});
