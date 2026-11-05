import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import RowMenu, { type RowMenuItem } from './RowMenu.svelte';

describe('RowMenu component', () => {
  const sampleItems: RowMenuItem[] = [
    { id: 'act-1', label: 'Action 1', onclick: vi.fn() },
    { id: 'act-2', label: 'Action 2', onclick: vi.fn() },
    { id: 'act-3', label: 'Action 3', onclick: vi.fn() },
  ];

  it('renders trigger with accessible name and visible label', () => {
    render(RowMenu, {
      props: {
        label: 'Report actions: Fix relay',
        visibleLabel: 'Actions',
        items: sampleItems,
      },
    });

    const trigger = screen.getByRole('button', { name: 'Report actions: Fix relay' });
    expect(trigger).toBeInTheDocument();
    expect(trigger).toHaveAttribute('aria-haspopup', 'menu');
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('opens menu on trigger click and automatically focuses first menu item', async () => {
    render(RowMenu, {
      props: {
        label: 'Row actions',
        items: sampleItems,
      },
    });

    const trigger = screen.getByRole('button', { name: 'Row actions' });
    await fireEvent.click(trigger);

    expect(trigger).toHaveAttribute('aria-expanded', 'true');
    const menu = screen.getByRole('menu');
    expect(menu).toBeInTheDocument();

    const items = screen.getAllByRole('menuitem');
    expect(items).toHaveLength(3);
    await waitFor(() => {
      expect(items[0]).toHaveFocus();
    });
  });

  it('navigates menu items with ArrowDown, ArrowUp, Home, and End keys', async () => {
    render(RowMenu, {
      props: {
        label: 'Row actions',
        items: sampleItems,
      },
    });

    const trigger = screen.getByRole('button', { name: 'Row actions' });
    await fireEvent.click(trigger);

    const items = screen.getAllByRole('menuitem');
    await waitFor(() => {
      expect(items[0]).toHaveFocus();
    });

    // ArrowDown moves to next item
    await fireEvent.keyDown(items[0], { key: 'ArrowDown' });
    expect(items[1]).toHaveFocus();

    // ArrowDown moves to third item
    await fireEvent.keyDown(items[1], { key: 'ArrowDown' });
    expect(items[2]).toHaveFocus();

    // ArrowDown wraps around to first item
    await fireEvent.keyDown(items[2], { key: 'ArrowDown' });
    expect(items[0]).toHaveFocus();

    // ArrowUp wraps backwards to last item
    await fireEvent.keyDown(items[0], { key: 'ArrowUp' });
    expect(items[2]).toHaveFocus();

    // Home jumps to first item
    await fireEvent.keyDown(items[2], { key: 'Home' });
    expect(items[0]).toHaveFocus();

    // End jumps to last item
    await fireEvent.keyDown(items[0], { key: 'End' });
    expect(items[2]).toHaveFocus();
  });

  it('closes menu and restores focus to trigger on Escape key', async () => {
    render(RowMenu, {
      props: {
        label: 'Row actions',
        items: sampleItems,
      },
    });

    const trigger = screen.getByRole('button', { name: 'Row actions' });
    await fireEvent.click(trigger);

    const items = screen.getAllByRole('menuitem');
    await waitFor(() => {
      expect(items[0]).toHaveFocus();
    });

    await fireEvent.keyDown(items[0], { key: 'Escape' });

    await waitFor(() => {
      expect(screen.queryByRole('menu')).toBeNull();
      expect(trigger).toHaveFocus();
    });
  });

  it('closes menu and restores focus to trigger on Tab key', async () => {
    render(RowMenu, {
      props: {
        label: 'Row actions',
        items: sampleItems,
      },
    });

    const trigger = screen.getByRole('button', { name: 'Row actions' });
    await fireEvent.click(trigger);

    const items = screen.getAllByRole('menuitem');
    await waitFor(() => {
      expect(items[0]).toHaveFocus();
    });

    await fireEvent.keyDown(items[0], { key: 'Tab' });

    await waitFor(() => {
      expect(screen.queryByRole('menu')).toBeNull();
      expect(trigger).toHaveFocus();
    });
  });

  it('closes menu on click outside', async () => {
    render(RowMenu, {
      props: {
        label: 'Row actions',
        items: sampleItems,
      },
    });

    const trigger = screen.getByRole('button', { name: 'Row actions' });
    await fireEvent.click(trigger);
    expect(screen.getByRole('menu')).toBeInTheDocument();

    await fireEvent.click(document.body);

    await waitFor(() => {
      expect(screen.queryByRole('menu')).toBeNull();
    });
  });

  it('executes item onclick and closes menu when item is clicked', async () => {
    const actHandler = vi.fn();
    render(RowMenu, {
      props: {
        label: 'Row actions',
        items: [{ id: 'run', label: 'Run task', onclick: actHandler }],
      },
    });

    const trigger = screen.getByRole('button', { name: 'Row actions' });
    await fireEvent.click(trigger);

    const item = screen.getByRole('menuitem', { name: 'Run task' });
    await fireEvent.click(item);

    expect(actHandler).toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.queryByRole('menu')).toBeNull();
    });
  });

  it('does not open menu when disabled', async () => {
    render(RowMenu, {
      props: {
        label: 'Disabled menu',
        items: sampleItems,
        disabled: true,
      },
    });

    const trigger = screen.getByRole('button', { name: 'Disabled menu' });
    expect(trigger).toBeDisabled();

    await fireEvent.click(trigger);
    expect(screen.queryByRole('menu')).toBeNull();
  });
});
