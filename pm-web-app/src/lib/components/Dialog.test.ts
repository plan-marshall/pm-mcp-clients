import fs from 'node:fs';
import path from 'node:path';
import { tick } from 'svelte';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import Dialog from './Dialog.svelte';
import DialogFixture from '../../test/fixtures/DialogFixture.svelte';
import DialogWithMenuFixture from '../../test/fixtures/DialogWithMenuFixture.svelte';
import DialogInThemeSectionFixture from '../../test/fixtures/DialogInThemeSectionFixture.svelte';
import NestedDialogFixture from '../../test/fixtures/NestedDialogFixture.svelte';
import DialogWithAutofocusFixture from '../../test/fixtures/DialogWithAutofocusFixture.svelte';

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
  disabledLabel: 'Archive',
};

describe('Dialog', () => {
  it('opens as a modal dialog with its title, its body and the acts of its footer', async () => {
    render(DialogFixture, { props: FIXTURE });
    await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

    const dialog = await screen.findByRole('dialog');

    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(screen.getByRole('dialog', { name: 'Delete the plan' })).toBe(dialog);
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

  it('takes the focus back into the dialog when it has left it', async () => {
    render(DialogFixture, { props: FIXTURE });
    await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
    const dialog = await screen.findByRole('dialog');
    const cancel = within(dialog).getByRole('button', { name: 'Cancel' });

    // A click on the backdrop drops the focus to the body; the next Tab must not walk the page.
    (document.activeElement as HTMLElement | null)?.blur();
    await fireEvent.keyDown(window, { key: 'Tab' });

    expect(document.activeElement).toBe(cancel);
  });

  it('keeps Tab on the dialog box inside it, and Shift-Tab too', async () => {
    render(DialogFixture, { props: FIXTURE });
    await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
    const dialog = await screen.findByRole('dialog');
    const confirm = within(dialog).getByRole('button', { name: 'Confirm' });
    const cancel = within(dialog).getByRole('button', { name: 'Cancel' });

    // The dialog box holds the focus when it opens; neither direction may leave it from there.
    dialog.focus();
    await fireEvent.keyDown(window, { key: 'Tab', shiftKey: true });
    expect(document.activeElement).toBe(confirm);

    dialog.focus();
    await fireEvent.keyDown(window, { key: 'Tab' });
    expect(document.activeElement).toBe(cancel);
  });

  it('keeps the tab wrap off an act that cannot take the focus', async () => {
    render(DialogFixture, { props: FIXTURE });
    await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
    const dialog = await screen.findByRole('dialog');
    const confirm = within(dialog).getByRole('button', { name: 'Confirm' });
    const cancel = within(dialog).getByRole('button', { name: 'Cancel' });

    // A disabled act sits after Confirm in the footer and must not become the wrap target.
    confirm.focus();
    await fireEvent.keyDown(window, { key: 'Tab' });

    expect(document.activeElement).toBe(cancel);
  });

  it('stops the page behind it from scrolling, and gives the page back when it closes', async () => {
    render(DialogFixture, { props: FIXTURE });
    await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
    await screen.findByRole('dialog');

    expect(document.documentElement.style.overflow).toBe('hidden');

    await fireEvent.keyDown(window, { key: 'Escape' });

    expect(document.documentElement.style.overflow).toBe('');
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
    expect(items.map((item) => item.textContent)).toEqual([
      'Drafts are lost.',
      'Lessons are kept.',
    ]);
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

  it('conforms to design tokens: radius-lg 12px, shadow-overlay, layer-dialog, text-xl title, right-aligned footer', () => {
    const content = fs.readFileSync(path.resolve(__dirname, './Dialog.svelte'), 'utf-8');
    const style = content.match(/<style[^>]*>([\s\S]*?)<\/style>/)?.[1] ?? '';

    expect(style).toContain('z-index: var(--layer-dialog)');
    expect(style).toContain('border-radius: var(--radius-lg)');
    expect(style).toContain('box-shadow: var(--shadow-overlay)');
    expect(style).toContain('background-color: var(--color-overlay)');
    expect(style).toContain('font-size: var(--text-xl)');
    expect(style).toContain('justify-content: flex-end');
  });

  it('renders consequences of a danger dialog in the danger colour token', () => {
    const content = fs.readFileSync(path.resolve(__dirname, './Dialog.svelte'), 'utf-8');
    const style = content.match(/<style[^>]*>([\s\S]*?)<\/style>/)?.[1] ?? '';
    expect(style).toMatch(
      /\.dialog--danger[^{]*\.dialog__consequences[^{]*\{[^}]*color:\s*var\(--color-danger\)/,
    );
  });

  it('an Escape pressed inside an open menu that lives in a dialog closes the menu and leaves the dialog open; a second Escape closes the dialog', async () => {
    const onClose = vi.fn();
    render(DialogWithMenuFixture, {
      props: {
        title: 'Dialog with menu',
        menuLabel: 'Actions',
        items: [
          { id: 'first', label: 'First' },
          { id: 'second', label: 'Second' },
        ],
        open: true,
        onClose,
      },
    });

    const trigger = screen.getByRole('button', { name: 'Actions' });
    await fireEvent.click(trigger);

    expect(await screen.findByRole('menu')).toBeInTheDocument();

    await fireEvent.keyDown(window, { key: 'Escape' });

    expect(onClose).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();

    await fireEvent.keyDown(window, { key: 'Escape' });

    expect(onClose).toHaveBeenCalledOnce();
  });

  it('draws a dialog opened inside a themed section in that section, so it keeps the section’s theme', () => {
    render(DialogInThemeSectionFixture, { props: { theme: 'dark' } });

    const section = screen.getByTestId('theme-section');

    expect(section).toContainElement(screen.getByRole('dialog'));
  });

  it('keeps Tab inside the dialog on top when one dialog opens another', async () => {
    render(NestedDialogFixture, {
      props: {
        outerTitle: 'Outer',
        innerTitle: 'Inner',
        openInnerLabel: 'Open inner',
        outerActLabel: 'Outer act',
        innerFirstLabel: 'Inner first',
        innerLastLabel: 'Inner last',
      },
    });
    await fireEvent.click(screen.getByRole('button', { name: 'Open inner' }));
    const inner = await screen.findByRole('dialog', { name: 'Inner' });
    const first = within(inner).getByRole('button', { name: 'Inner first' });
    const last = within(inner).getByRole('button', { name: 'Inner last' });

    last.focus();
    await fireEvent.keyDown(window, { key: 'Tab' });

    expect(document.activeElement).toBe(first);
  });

  it('closes the dialog on top on Escape, and leaves the one below it open', async () => {
    const onInnerClose = vi.fn();
    const onOuterClose = vi.fn();
    render(NestedDialogFixture, {
      props: {
        outerTitle: 'Outer',
        innerTitle: 'Inner',
        openInnerLabel: 'Open inner',
        outerActLabel: 'Outer act',
        innerFirstLabel: 'Inner first',
        innerLastLabel: 'Inner last',
        onInnerClose,
        onOuterClose,
      },
    });
    await fireEvent.click(screen.getByRole('button', { name: 'Open inner' }));
    await screen.findByRole('dialog', { name: 'Inner' });

    await fireEvent.keyDown(window, { key: 'Escape' });

    expect(onInnerClose).toHaveBeenCalledOnce();
    expect(onOuterClose).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog', { name: 'Outer' })).toBeInTheDocument();
    expect(screen.queryByRole('dialog', { name: 'Inner' })).not.toBeInTheDocument();
  });

  it('does not take the focus from a child that takes it as it mounts', async () => {
    render(DialogWithAutofocusFixture, { props: { label: 'Name' } });
    await tick();
    const field = screen.getByRole('textbox', { name: 'Name' });

    expect(document.activeElement).toBe(field);
  });

  it('an Escape pressed on a menu entry inside the dialog leaves the dialog open and closes the menu', async () => {
    const onClose = vi.fn();
    render(DialogWithMenuFixture, {
      props: {
        title: 'Dialog with menu',
        menuLabel: 'Actions',
        items: [
          { id: 'first', label: 'First' },
          { id: 'second', label: 'Second' },
        ],
        open: true,
        onClose,
      },
    });

    const trigger = screen.getByRole('button', { name: 'Actions' });
    await fireEvent.click(trigger);
    const item = await screen.findByRole('menuitem', { name: 'First' });
    item.focus();

    await fireEvent.keyDown(item, { key: 'Escape' });

    expect(onClose).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });
});
