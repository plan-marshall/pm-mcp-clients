import fs from 'node:fs';
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

  it.each([
    { key: 'Escape', desc: 'Escape key' },
    { key: 'Tab', desc: 'Tab key' },
  ])('closes menu and restores focus to trigger on $desc inside menu (N1)', async ({ key }) => {
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

    await fireEvent.keyDown(items[0], { key });

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
    let focusDuringAction: Element | null = null;
    const actHandler = vi.fn().mockImplementation(() => {
      focusDuringAction = document.activeElement;
    });
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
    expect(focusDuringAction).toBe(trigger);
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
    const focusVisibleRules = rulesOf(style, '.row-menu__item:focus-visible:not(:disabled)');
    expect(focusVisibleRules.length).toBeGreaterThan(0);
    const combinedVisible = focusVisibleRules.join(';');
    expect(combinedVisible).not.toContain('outline: none');
    expect(combinedVisible).toContain('outline:');
    expect(combinedVisible).toContain('var(--color-brand)');

    // Mouse focus has outline: none
    const focusRules = rulesOf(style, '.row-menu__item:focus:not(:disabled)');
    expect(focusRules.join(';')).toContain('outline: none');

    // Read real tokens from tokens.css
    const tokensCss = fs.readFileSync(path.resolve(__dirname, '../../styles/tokens.css'), 'utf-8');

    // Parse hex colors for rgb conversion
    const hexToRgb = (hex: string) => {
      const clean = hex.replace('#', '').trim();
      return {
        r: parseInt(clean.slice(0, 2), 16),
        g: parseInt(clean.slice(2, 4), 16),
        b: parseInt(clean.slice(4, 6), 16),
      };
    };

    const lum = (c: { r: number; g: number; b: number }) => {
      const sRGB = [c.r / 255, c.g / 255, c.b / 255].map((v) =>
        v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4),
      );
      return 0.2126 * sRGB[0] + 0.7152 * sRGB[1] + 0.0722 * sRGB[2];
    };

    // Extract light brand & surface from :root or [data-theme='light']
    const lightBrandMatch = tokensCss.match(/--color-brand:\s*(#[0-9a-fA-F]{6})/);
    expect(lightBrandMatch).not.toBeNull();
    const lightSurfaceMatch = tokensCss.match(/--color-bg-surface:\s*(#[0-9a-fA-F]{6})/);
    expect(lightSurfaceMatch).not.toBeNull();

    const lightBrand = hexToRgb(lightBrandMatch![1]);
    const lightSurface = hexToRgb(lightSurfaceMatch![1]);
    expect(lightBrandMatch![1].toUpperCase()).toBe('#5B4BDB');
    expect(lightSurfaceMatch![1].toUpperCase()).toBe('#FFFFFF');

    const lightContrast =
      (Math.max(lum(lightBrand), lum(lightSurface)) + 0.05) /
      (Math.min(lum(lightBrand), lum(lightSurface)) + 0.05);
    expect(lightContrast).toBeGreaterThanOrEqual(3.0);

    // Extract dark brand & surface from [data-theme='dark'] or @media (prefers-color-scheme: dark)
    const darkSection = tokensCss.slice(tokensCss.lastIndexOf("[data-theme='dark']"));
    const darkBrandMatch = darkSection.match(/--color-brand:\s*(#[0-9a-fA-F]{6})/);
    expect(darkBrandMatch).not.toBeNull();
    const darkSurfaceMatch = darkSection.match(/--color-bg-raised:\s*(#[0-9a-fA-F]{6})/);
    expect(darkSurfaceMatch).not.toBeNull();

    const darkBrand = hexToRgb(darkBrandMatch![1]);
    const darkSurface = hexToRgb(darkSurfaceMatch![1]);
    expect(darkBrandMatch![1].toUpperCase()).toBe('#8B7EF9');
    expect(darkSurfaceMatch![1].toUpperCase()).toBe('#1C2230');

    const darkContrast =
      (Math.max(lum(darkBrand), lum(darkSurface)) + 0.05) /
      (Math.min(lum(darkBrand), lum(darkSurface)) + 0.05);
    expect(darkContrast).toBeGreaterThanOrEqual(3.0);
  });

  it('measures 44px min-block-size on phone viewports (D2b)', () => {
    const style = styleOf(path.resolve(__dirname, './RowMenu.svelte'));
    const mediaMatch = style.match(/@media\s*\(max-width:\s*767px\)\s*\{([\s\S]*?\})\s*\}/);
    expect(mediaMatch).not.toBeNull();
    const mediaBlock = mediaMatch![1];
    expect(mediaBlock).toMatch(/\.row-menu__item\s*\{[^}]*min-block-size:\s*44px/);

    // Outside the phone media query, row-menu__item does not have min-block-size
    const outsideStyle = style.replace(/@media\s*\(max-width:\s*767px\)\s*\{[\s\S]*?\}/, '');
    const outsideRules = rulesOf(outsideStyle, '.row-menu__item');
    expect(outsideRules.join(';')).not.toContain('min-block-size');
  });

  it('closes menu and retains focus on trigger when Escape is pressed on the trigger itself (N1)', async () => {
    render(RowMenu, {
      props: {
        label: 'Row actions',
        items: sampleItems,
      },
    });

    const trigger = screen.getByRole('button', { name: 'Row actions' });
    await fireEvent.click(trigger);

    await waitFor(() => {
      expect(screen.getByRole('menu')).toBeInTheDocument();
    });

    trigger.focus();
    expect(trigger).toHaveFocus();

    await fireEvent.keyDown(trigger, { key: 'Escape' });

    await waitFor(() => {
      expect(screen.queryByRole('menu')).toBeNull();
      expect(trigger).toHaveFocus();
    });
  });

  it('closes menu on outside Escape key via window listener (N1)', async () => {
    render(RowMenu, {
      props: {
        label: 'Row actions',
        items: sampleItems,
      },
    });

    const trigger = screen.getByRole('button', { name: 'Row actions' });
    await fireEvent.click(trigger);

    await waitFor(() => {
      expect(screen.getByRole('menu')).toBeInTheDocument();
    });

    await fireEvent.keyDown(document.body, { key: 'Escape' });
    await waitFor(() => {
      expect(screen.queryByRole('menu')).toBeNull();
      expect(trigger).toHaveFocus();
    });
  });
});
