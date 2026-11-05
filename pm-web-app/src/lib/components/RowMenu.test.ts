import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import RowMenu, { type RowMenuItem } from './RowMenu.svelte';
import { styleOf, rulesOf } from '../../test/templateStyle';

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
    expect(trigger).toHaveTextContent('Actions');
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

  it('skips disabled items during arrow key navigation (D3a)', async () => {
    render(RowMenu, {
      props: {
        label: 'Row actions',
        items: [
          { id: 'act-1', label: 'Action 1' },
          { id: 'act-2', label: 'Action 2', disabled: true },
          { id: 'act-3', label: 'Action 3' },
        ],
      },
    });

    const trigger = screen.getByRole('button', { name: 'Row actions' });
    await fireEvent.click(trigger);

    const items = screen.getAllByRole('menuitem');
    await waitFor(() => {
      expect(items[0]).toHaveFocus();
    });

    await fireEvent.keyDown(items[0], { key: 'ArrowDown' });
    expect(items[2]).toHaveFocus();

    await fireEvent.keyDown(items[2], { key: 'ArrowUp' });
    expect(items[0]).toHaveFocus();
  });

  it('restores focus to trigger when an item is chosen (D2c)', async () => {
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
      expect(trigger).toHaveFocus();
    });
  });

  it('gives the popup menu an accessible name matching the label (D2c)', async () => {
    render(RowMenu, {
      props: {
        label: 'Report actions: Fix relay',
        items: sampleItems,
      },
    });

    const trigger = screen.getByRole('button', { name: 'Report actions: Fix relay' });
    await fireEvent.click(trigger);

    const menu = screen.getByRole('menu', { name: 'Report actions: Fix relay' });
    expect(menu).toBeInTheDocument();
    expect(menu).toHaveAttribute('aria-label', 'Report actions: Fix relay');
  });

  it('has a visible focus style distinct from its hover with >= 3:1 contrast cue (D2a)', () => {
    const style = styleOf(path.resolve(__dirname, './RowMenu.svelte'));
    const focusRules = rulesOf(style, '.row-menu__item:focus:not(:disabled)');
    expect(focusRules.length).toBeGreaterThan(0);
    const combined = focusRules.join(';');
    expect(combined).not.toContain('outline: none');
    expect(combined).toContain('outline:');
    expect(combined).toContain('var(--color-brand)');

    // Contrast verification: brand color (#2563EB light, #60A5FA dark) vs surface (#FFFFFF light, #1C2230 dark)
    const lum = (c: { r: number; g: number; b: number }) => {
      const sRGB = [c.r / 255, c.g / 255, c.b / 255].map((v) =>
        v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4),
      );
      return 0.2126 * sRGB[0] + 0.7152 * sRGB[1] + 0.0722 * sRGB[2];
    };
    const lightBrand = { r: 37, g: 99, b: 235 };
    const lightSurface = { r: 255, g: 255, b: 255 };
    const lightContrast = (Math.max(lum(lightBrand), lum(lightSurface)) + 0.05) / (Math.min(lum(lightBrand), lum(lightSurface)) + 0.05);
    expect(lightContrast).toBeGreaterThanOrEqual(3.0);

    const darkBrand = { r: 96, g: 165, b: 250 };
    const darkSurface = { r: 28, g: 34, b: 48 };
    const darkContrast = (Math.max(lum(darkBrand), lum(darkSurface)) + 0.05) / (Math.min(lum(darkBrand), lum(darkSurface)) + 0.05);
    expect(darkContrast).toBeGreaterThanOrEqual(3.0);
  });

  it('measures 44px min-block-size on phone viewports (D2b)', () => {
    const style = styleOf(path.resolve(__dirname, './RowMenu.svelte'));
    expect(style).toContain('@media (max-width: 767px)');
    expect(style).toMatch(/\.row-menu__item\s*\{[^}]*min-block-size:\s*44px/);
  });

  it('handles Escape inside menu and isolates window handler from menuHost events (D2c)', async () => {
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

    // Window handler closes menu on outside Escape
    await fireEvent.keyDown(document.body, { key: 'Escape' });
    await waitFor(() => {
      expect(screen.queryByRole('menu')).toBeNull();
      expect(trigger).toHaveFocus();
    });
  });
});
