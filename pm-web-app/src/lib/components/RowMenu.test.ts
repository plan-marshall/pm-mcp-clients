import fs from 'node:fs';
import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { tick } from 'svelte';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
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

  it('drops an advisory that only repeats the visible word (L3-5)', () => {
    const repeated = render(RowMenu, {
      props: {
        label: 'Row actions',
        visibleLabel: 'Actions',
        advisory: 'Actions',
        items: sampleItems,
      },
    });
    const suppressed = within(repeated.container).getByRole('button', { name: 'Row actions' });
    expect(suppressed).not.toHaveAttribute('title');

    const added = render(RowMenu, {
      props: {
        label: 'Row actions',
        visibleLabel: 'Actions',
        advisory: 'Show the acts that do not fit the row',
        items: sampleItems,
      },
    });
    const carried = within(added.container).getByRole('button', { name: 'Row actions' });
    expect(carried).toHaveAttribute('title', 'Show the acts that do not fit the row');
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
    // The ratio is computed from the palette the tokens declare; no literal hex is pinned, so a
    // retune that keeps the cue stays green.
    expect(lightBrandMatch![1]).toMatch(/^#[0-9a-fA-F]{6}$/);
    expect(lightSurfaceMatch![1]).toMatch(/^#[0-9a-fA-F]{6}$/);

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
    expect(darkBrandMatch![1]).toMatch(/^#[0-9a-fA-F]{6}$/);
    expect(darkSurfaceMatch![1]).toMatch(/^#[0-9a-fA-F]{6}$/);

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

  describe('placement at the bottom of the viewport', () => {
    /** Lays out the trigger's row at `rowTop` and every menu at `menuHeight`, in a 400 px viewport. */
    function layOut(rowTop: number, menuHeight: number): () => void {
      const original = HTMLElement.prototype.getBoundingClientRect;
      const viewport = window.innerHeight;
      Object.defineProperty(window, 'innerHeight', { configurable: true, value: 400 });
      const spy = vi
        .spyOn(HTMLElement.prototype, 'getBoundingClientRect')
        .mockImplementation(function (this: HTMLElement) {
          if (this.classList.contains('row-menu__list')) {
            const top = this.classList.contains('row-menu__list--above')
              ? rowTop - menuHeight - 4
              : rowTop + 32;
            return DOMRect.fromRect({ x: 0, y: top, width: 180, height: menuHeight });
          }
          if (this.classList.contains('row-menu')) {
            return DOMRect.fromRect({ x: 0, y: rowTop, width: 28, height: 28 });
          }
          return original.call(this);
        });
      return () => {
        spy.mockRestore();
        Object.defineProperty(window, 'innerHeight', { configurable: true, value: viewport });
      };
    }

    it('opens upwards when the menu would pass the bottom of the viewport', async () => {
      const restore = layOut(340, 80);
      try {
        render(RowMenu, { props: { label: 'Row actions', items: sampleItems } });

        await fireEvent.click(screen.getByRole('button', { name: 'Row actions' }));

        await waitFor(() => {
          expect(screen.getByRole('menu')).toHaveClass('row-menu__list--above');
          expect(screen.getAllByRole('menuitem')[0]).toHaveFocus();
        });
      } finally {
        restore();
      }
    });

    it('opens downwards when the menu fits below its trigger', async () => {
      const restore = layOut(100, 80);
      try {
        render(RowMenu, { props: { label: 'Row actions', items: sampleItems } });

        await fireEvent.click(screen.getByRole('button', { name: 'Row actions' }));

        await waitFor(() => {
          expect(screen.getAllByRole('menuitem')[0]).toHaveFocus();
        });
        expect(screen.getByRole('menu')).not.toHaveClass('row-menu__list--above');
      } finally {
        restore();
      }
    });

    it('stays below when there is no more room above than below', async () => {
      const restore = layOut(60, 380);
      try {
        render(RowMenu, { props: { label: 'Row actions', items: sampleItems } });

        await fireEvent.click(screen.getByRole('button', { name: 'Row actions' }));

        await waitFor(() => {
          expect(screen.getAllByRole('menuitem')[0]).toHaveFocus();
        });
        expect(screen.getByRole('menu')).not.toHaveClass('row-menu__list--above');
      } finally {
        restore();
      }
    });

    it('opens downwards again after a menu that opened upwards was closed', async () => {
      let restore = layOut(340, 80);
      try {
        render(RowMenu, { props: { label: 'Row actions', items: sampleItems } });
        const trigger = screen.getByRole('button', { name: 'Row actions' });
        await fireEvent.click(trigger);
        await waitFor(() => {
          expect(screen.getByRole('menu')).toHaveClass('row-menu__list--above');
        });
        await fireEvent.keyDown(screen.getAllByRole('menuitem')[0], { key: 'Escape' });
        await waitFor(() => {
          expect(screen.queryByRole('menu')).toBeNull();
        });

        restore();
        restore = layOut(100, 80);
        await fireEvent.click(trigger);
        await waitFor(() => {
          expect(screen.getAllByRole('menuitem')[0]).toHaveFocus();
        });
        expect(screen.getByRole('menu')).not.toHaveClass('row-menu__list--above');
      } finally {
        restore();
      }
    });

    it('caps a menu taller than the room above its trigger and lets it scroll', async () => {
      const restore = layOut(340, 360);
      try {
        render(RowMenu, { props: { label: 'Row actions', items: sampleItems } });

        await fireEvent.click(screen.getByRole('button', { name: 'Row actions' }));

        await waitFor(() => {
          expect(screen.getByRole('menu')).toHaveClass('row-menu__list--above');
        });
        // The row starts at 340 and the menu keeps a 4 px gap to it: 336 px of room above.
        expect(screen.getByRole('menu').style.maxBlockSize).toBe('336px');
        const listRules = rulesOf(
          styleOf(path.resolve(__dirname, './RowMenu.svelte')),
          '.row-menu__list',
        ).join(';');
        expect(listRules).toContain('overflow-y: auto');
      } finally {
        restore();
      }
    });

    it('leaves a menu that fits its side uncapped', async () => {
      const restore = layOut(100, 80);
      try {
        render(RowMenu, { props: { label: 'Row actions', items: sampleItems } });

        await fireEvent.click(screen.getByRole('button', { name: 'Row actions' }));

        await waitFor(() => {
          expect(screen.getAllByRole('menuitem')[0]).toHaveFocus();
        });
        expect(screen.getByRole('menu').style.maxBlockSize).toBe('');
      } finally {
        restore();
      }
    });

    it.each(['scroll', 'resize'])(
      'places an open menu again when the viewport changes (%s)',
      async (change) => {
        let restore = layOut(340, 80);
        try {
          render(RowMenu, { props: { label: 'Row actions', items: sampleItems } });
          await fireEvent.click(screen.getByRole('button', { name: 'Row actions' }));
          await waitFor(() => {
            expect(screen.getByRole('menu')).toHaveClass('row-menu__list--above');
          });

          restore();
          restore = layOut(100, 80);
          window.dispatchEvent(new Event(change));

          await waitFor(() => {
            expect(screen.getByRole('menu')).not.toHaveClass('row-menu__list--above');
          });
        } finally {
          restore();
        }
      },
    );

    it('keeps its own scroll and its placement when the capped menu scrolls (D2a)', async () => {
      let restore = layOut(340, 360);
      try {
        render(RowMenu, { props: { label: 'Row actions', items: sampleItems } });
        await fireEvent.click(screen.getByRole('button', { name: 'Row actions' }));
        await waitFor(() => {
          expect(screen.getByRole('menu')).toHaveClass('row-menu__list--above');
        });

        const menu = screen.getByRole('menu');
        expect(menu.style.maxBlockSize).toBe('336px');
        menu.scrollTop = 40;
        expect(menu.scrollTop).toBe(40);

        // The trigger would now have room below it: a re-place would flip the menu downwards and
        // recut the cap. A scroll of the capped menu's own entries must not re-place it.
        restore();
        restore = layOut(40, 360);
        menu.dispatchEvent(new Event('scroll', { bubbles: true }));
        await tick();

        expect(menu.scrollTop).toBe(40);
        expect(menu.style.maxBlockSize).toBe('336px');
        expect(menu).toHaveClass('row-menu__list--above');
      } finally {
        restore();
      }
    });

    it('stops following the viewport once the menu is closed', async () => {
      const removed = vi.spyOn(window, 'removeEventListener');
      try {
        render(RowMenu, { props: { label: 'Row actions', items: sampleItems } });
        const trigger = screen.getByRole('button', { name: 'Row actions' });
        await fireEvent.click(trigger);
        await waitFor(() => {
          expect(screen.getAllByRole('menuitem')[0]).toHaveFocus();
        });

        await fireEvent.keyDown(screen.getAllByRole('menuitem')[0], { key: 'Escape' });

        await waitFor(() => {
          expect(screen.queryByRole('menu')).toBeNull();
        });
        const types = removed.mock.calls.map(([type]) => type);
        expect(types).toContain('scroll');
        expect(types).toContain('resize');
      } finally {
        removed.mockRestore();
      }
    });

    it('lays the open menu above the sticky chrome of the phone frame', () => {
      const style = styleOf(path.resolve(__dirname, './RowMenu.svelte'));
      const listRules = rulesOf(style, '.row-menu__list').join(';');
      const bottomBarStyle = styleOf(path.resolve(__dirname, '../shell/BottomBar.svelte'));
      const barLayer = Number(/z-index:\s*(\d+)/.exec(bottomBarStyle)?.[1]);
      const menuLayer = Number(/z-index:\s*(\d+)/.exec(listRules)?.[1]);

      expect(barLayer).toBeGreaterThan(0);
      expect(menuLayer).toBeGreaterThan(barLayer);
      const aboveRules = rulesOf(style, '.row-menu__list--above').join(';');
      expect(aboveRules).toContain('inset-block-end: calc(100% + var(--space-1))');
      expect(aboveRules).toContain('inset-block-start: auto');
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
