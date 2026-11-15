import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import ActMenu, { type ActMenuItem } from './ActMenu.svelte';
import { styleOf, rulesOf } from '../../test/templateStyle';

describe('ActMenu component', () => {
  const sampleItems: ActMenuItem[] = [
    { id: 'act-1', label: 'Action 1', onclick: vi.fn() },
    { id: 'act-2', label: 'Action 2', onclick: vi.fn() },
    { id: 'act-3', label: 'Action 3', onclick: vi.fn() },
  ];

  it('renders menu items and focuses first item', async () => {
    const trigger = document.createElement('button');
    document.body.appendChild(trigger);

    render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        items: sampleItems,
        triggerElement: trigger,
      },
    });

    const menu = screen.getByRole('menu', { name: 'Actions' });
    expect(menu).toBeInTheDocument();
    const items = screen.getAllByRole('menuitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('Action 1');
  });

  it('navigates with ArrowDown and ArrowUp wrapping around, including disabled entries', async () => {
    const itemsWithDisabled: ActMenuItem[] = [
      { id: 'act-1', label: 'Action 1' },
      { id: 'act-2', label: 'Action 2', disabled: true, disabledReason: 'Not available' },
      { id: 'act-3', label: 'Action 3' },
    ];

    render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        items: itemsWithDisabled,
      },
    });

    const menu = screen.getByRole('menu');
    const items = screen.getAllByRole('menuitem');

    items[0].focus();
    expect(document.activeElement).toBe(items[0]);

    // ArrowDown moves to disabled item
    await fireEvent.keyDown(menu, { key: 'ArrowDown' });
    expect(document.activeElement).toBe(items[1]);

    // ArrowDown moves to third item
    await fireEvent.keyDown(menu, { key: 'ArrowDown' });
    expect(document.activeElement).toBe(items[2]);

    // ArrowDown wraps to first item
    await fireEvent.keyDown(menu, { key: 'ArrowDown' });
    expect(document.activeElement).toBe(items[0]);

    // ArrowUp wraps to last item
    await fireEvent.keyDown(menu, { key: 'ArrowUp' });
    expect(document.activeElement).toBe(items[2]);
  });

  it('moves to first and last entry with Home and End', async () => {
    render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        items: sampleItems,
      },
    });

    const menu = screen.getByRole('menu');
    const items = screen.getAllByRole('menuitem');

    items[1].focus();
    expect(document.activeElement).toBe(items[1]);

    await fireEvent.keyDown(menu, { key: 'Home' });
    expect(document.activeElement).toBe(items[0]);

    await fireEvent.keyDown(menu, { key: 'End' });
    expect(document.activeElement).toBe(items[2]);
  });

  it.each([
    { key: 'Escape', desc: 'Escape' },
    { key: 'Tab', desc: 'Tab' },
  ])('closes menu and returns focus to trigger on $desc', async ({ key }) => {
    const trigger = document.createElement('button');
    document.body.appendChild(trigger);
    const onClose = vi.fn();

    render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        items: sampleItems,
        triggerElement: trigger,
        onClose,
      },
    });

    const menu = screen.getByRole('menu');
    const items = screen.getAllByRole('menuitem');
    items[0].focus();

    await fireEvent.keyDown(menu, { key });
    expect(onClose).toHaveBeenCalledWith(true);
    await waitFor(() => {
      expect(document.activeElement).toBe(trigger);
    });
  });

  it('activation closes menu, returns focus, and runs the act', async () => {
    const trigger = document.createElement('button');
    document.body.appendChild(trigger);
    const onclick = vi.fn();
    const onAct = vi.fn();

    render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        items: [{ id: 'run', label: 'Run', onclick }],
        triggerElement: trigger,
        onAct,
      },
    });

    const item = screen.getByRole('menuitem', { name: 'Run' });
    await fireEvent.click(item);

    expect(onclick).toHaveBeenCalled();
    expect(onAct).toHaveBeenCalledWith(expect.objectContaining({ id: 'run' }), expect.anything());
    await waitFor(() => {
      expect(document.activeElement).toBe(trigger);
    });
  });

  it('a destructive entry opens its ConfirmDialog before running', async () => {
    const onclick = vi.fn();
    const onAct = vi.fn();

    render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        items: [{ id: 'delete', label: 'Delete item', destructive: true, onclick }],
        onAct,
      },
    });

    const item = screen.getByRole('menuitem', { name: 'Delete item' });
    await fireEvent.click(item);

    // Destructive entry opened confirm dialog without immediately running
    expect(onclick).not.toHaveBeenCalled();
    const dialog = screen.getByRole('dialog');
    expect(dialog).toBeInTheDocument();
    expect(dialog).toHaveTextContent('Delete item');

    // Confirm runs the act
    const confirmBtn = screen.getByRole('button', { name: 'Delete item' });
    await fireEvent.click(confirmBtn);
    expect(onclick).toHaveBeenCalled();
    expect(onAct).toHaveBeenCalledWith(expect.objectContaining({ id: 'delete' }));
  });

  it('does not confirm destructive action if busy becomes true while confirmation is open', async () => {
    const onclick = vi.fn();
    const onAct = vi.fn();

    const { rerender } = render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        items: [{ id: 'delete', label: 'Delete item', destructive: true, onclick }],
        onAct,
      },
    });

    const item = screen.getByRole('menuitem', { name: 'Delete item' });
    await fireEvent.click(item);

    expect(screen.getByRole('dialog')).toBeInTheDocument();

    // Now busy becomes true while dialog is open
    rerender({
      open: true,
      busy: true,
      label: 'Actions',
      items: [{ id: 'delete', label: 'Delete item', destructive: true, onclick }],
      onAct,
    });

    const confirmBtn = screen.getByRole('button', { name: 'Delete item' });
    await fireEvent.click(confirmBtn);
    expect(onclick).not.toHaveBeenCalled();
    expect(onAct).not.toHaveBeenCalled();
  });

  it('edge: no items draws no menu entries', () => {
    render(ActMenu, {
      props: {
        open: true,
        label: 'Empty',
        items: [],
      },
    });

    const menu = screen.getByRole('menu');
    expect(menu.querySelectorAll('[role="menuitem"]')).toHaveLength(0);
  });

  it('edge: one entry navigates and activates cleanly', async () => {
    const onAct = vi.fn();
    render(ActMenu, {
      props: {
        open: true,
        label: 'Single',
        items: [{ id: 'only', label: 'Only act' }],
        onAct,
      },
    });

    const item = screen.getByRole('menuitem', { name: 'Only act' });
    expect(item).toBeInTheDocument();

    await fireEvent.keyDown(item, { key: 'ArrowDown' });
    expect(document.activeElement).toBe(item);

    await fireEvent.click(item);
    expect(onAct).toHaveBeenCalledWith(expect.objectContaining({ id: 'only' }), expect.anything());
  });

  it('edge: all entries disabled keeps the menu open and each reason reachable', async () => {
    render(ActMenu, {
      props: {
        open: true,
        label: 'All disabled',
        items: [
          { id: 'd1', label: 'First disabled', disabled: true, disabledReason: 'Reason 1' },
          { id: 'd2', label: 'Second disabled', disabled: true, disabledReason: 'Reason 2' },
        ],
      },
    });

    const items = screen.getAllByRole('menuitem');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveAttribute('aria-disabled', 'true');
    expect(items[1]).toHaveAttribute('aria-disabled', 'true');

    items[0].focus();
    expect(document.activeElement).toBe(items[0]);
    await fireEvent.keyDown(items[0], { key: 'ArrowDown' });
    expect(document.activeElement).toBe(items[1]);
  });

  it('focuses first disabled entry on opening when all entries are disabled', async () => {
    const { rerender } = render(ActMenu, {
      props: {
        open: false,
        label: 'All disabled',
        items: [
          { id: 'd1', label: 'First disabled', disabled: true, disabledReason: 'Reason 1' },
          { id: 'd2', label: 'Second disabled', disabled: true, disabledReason: 'Reason 2' },
        ],
      },
    });

    await rerender({
      open: true,
      label: 'All disabled',
      items: [
        { id: 'd1', label: 'First disabled', disabled: true, disabledReason: 'Reason 1' },
        { id: 'd2', label: 'Second disabled', disabled: true, disabledReason: 'Reason 2' },
      ],
    });

    const items = screen.getAllByRole('menuitem');
    expect(document.activeElement).toBe(items[0]);
  });

  it('edge: a disabled entry with and without a reason', () => {
    render(ActMenu, {
      props: {
        open: true,
        label: 'Disabled variations',
        items: [
          { id: 'with-reason', label: 'With reason', disabled: true, disabledReason: 'Missing permission' },
          { id: 'without-reason', label: 'Without reason', disabled: true },
        ],
      },
    });

    const withReason = screen.getByRole('menuitem', { name: /With reason/ });
    expect(withReason).toHaveAttribute('aria-disabled', 'true');
    const describedBy = withReason.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)).toHaveTextContent('Missing permission');

    const withoutReason = screen.getByRole('menuitem', { name: 'Without reason' });
    expect(withoutReason).toHaveAttribute('aria-disabled', 'true');
    expect(withoutReason).not.toHaveAttribute('aria-describedby');
  });

  it('shows the disabled reason as a second line when the entry has focus', async () => {
    render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        items: [
          { id: 'd1', label: 'Locked task', disabled: true, disabledReason: 'Requires admin token' },
        ],
      },
    });

    const item = screen.getByRole('menuitem', { name: /Locked task/ });
    const reasonEl = screen.getByText('Requires admin token');
    expect(reasonEl).toHaveClass('act-menu__reason');
    expect(reasonEl).not.toHaveClass('act-menu__reason--visible');

    await fireEvent.focus(item);
    expect(reasonEl).toHaveClass('act-menu__reason--visible');

    await fireEvent.blur(item);
    expect(reasonEl).not.toHaveClass('act-menu__reason--visible');
  });

  it('edge: a menu taller than the room is capped and scrolls', () => {
    render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        items: sampleItems,
        maxBlockSize: '120px',
      },
    });

    const menu = screen.getByRole('menu');
    expect(menu.getAttribute('style')).toContain('max-block-size: 120px');
  });

  it('edge: opening upward adds the above modifier class', () => {
    render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        items: sampleItems,
        above: true,
      },
    });

    const menu = screen.getByRole('menu');
    expect(menu).toHaveClass('act-menu--above');
  });

  it('edge: busy renders every entry inert', async () => {
    const onAct = vi.fn();
    render(ActMenu, {
      props: {
        open: true,
        label: 'Busy menu',
        items: sampleItems,
        busy: true,
        onAct,
      },
    });

    const items = screen.getAllByRole('menuitem');
    for (const item of items) {
      expect(item).toHaveAttribute('aria-disabled', 'true');
      await fireEvent.click(item);
    }

    expect(onAct).not.toHaveBeenCalled();
  });

  it('destructive action: unchanged item executes once on confirmation', async () => {
    const onAct = vi.fn();
    const onClick = vi.fn();
    render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        confirmDestructive: true,
        items: [
          { id: 'delete', label: 'Delete', destructive: true, onclick: onClick },
        ],
        onAct,
      },
    });

    const deleteItem = screen.getByRole('menuitem', { name: 'Delete' });
    await fireEvent.click(deleteItem);

    const confirmButton = screen.getByTestId('confirm-dialog-confirm');
    await fireEvent.click(confirmButton);

    expect(onClick).toHaveBeenCalledTimes(1);
    expect(onAct).toHaveBeenCalledTimes(1);
  });

  it('destructive action: item disabled while confirmation is open does not run', async () => {
    const onAct = vi.fn();
    const onClick = vi.fn();
    const { rerender } = render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        confirmDestructive: true,
        items: [
          { id: 'delete', label: 'Delete', destructive: true, onclick: onClick },
        ],
        onAct,
      },
    });

    const deleteItem = screen.getByRole('menuitem', { name: 'Delete' });
    await fireEvent.click(deleteItem);

    await rerender({
      open: true,
      label: 'Actions',
      confirmDestructive: true,
      items: [
        { id: 'delete', label: 'Delete', destructive: true, disabled: true, onclick: onClick },
      ],
      onAct,
    });

    const confirmButton = screen.getByTestId('confirm-dialog-confirm');
    await fireEvent.click(confirmButton);

    expect(onClick).not.toHaveBeenCalled();
    expect(onAct).not.toHaveBeenCalled();
  });

  it('destructive action: item removed while confirmation is open does not run', async () => {
    const onAct = vi.fn();
    const onClick = vi.fn();
    const { rerender } = render(ActMenu, {
      props: {
        open: true,
        label: 'Actions',
        confirmDestructive: true,
        items: [
          { id: 'delete', label: 'Delete', destructive: true, onclick: onClick },
        ],
        onAct,
      },
    });

    const deleteItem = screen.getByRole('menuitem', { name: 'Delete' });
    await fireEvent.click(deleteItem);

    await rerender({
      open: true,
      label: 'Actions',
      confirmDestructive: true,
      items: [],
      onAct,
    });

    const confirmButton = screen.getByTestId('confirm-dialog-confirm');
    await fireEvent.click(confirmButton);

    expect(onClick).not.toHaveBeenCalled();
    expect(onAct).not.toHaveBeenCalled();
  });

  it('conforms to design tokens: layer-menu, focus ring, and mobile 44px height', () => {
    const style = styleOf(path.resolve(__dirname, './ActMenu.svelte'));
    const menuRules = rulesOf(style, '.act-menu').join(';');
    const itemRules = rulesOf(style, '.act-menu__item').join(';');
    const focusRules = rulesOf(style, '.act-menu__item:focus').join(';');

    expect(menuRules).toContain('z-index: var(--layer-menu)');
    expect(menuRules).toContain('overflow-y: auto');
    expect(focusRules).toContain('box-shadow: var(--focus-ring)');
    expect(itemRules).toContain('inline-size: 100%');
    expect(style).toContain('min-block-size: 44px');
  });
});
