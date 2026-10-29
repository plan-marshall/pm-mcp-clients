import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import Tabs from './Tabs.svelte';

/**
 * `Tabs` (VD-P-05, VD-S-03): the underline tab list, the active tab in the brand colour, and the
 * counts of a tab as `CountBadge`. The keyboard belongs to the widget: the arrow keys move within
 * the list, Home and End jump to either end, and the tab a person lands on is the one that selects.
 */

const LABEL = 'Plan sections';

const TABS = [
  { id: 'open', label: 'Open', count: 2 },
  { id: 'done', label: 'Done' },
  { id: 'failed', label: 'Failed', count: 3, attention: true },
];

describe('Tabs', () => {
  it('draws one tab per entry and selects the given one alone', () => {
    render(Tabs, { props: { tabs: TABS, active: 'open', label: LABEL } });
    const tabs = screen.getAllByRole('tab');

    expect(tabs.map((tab) => tab.textContent?.trim())).toEqual(['Open 2', 'Done', 'Failed 3']);
    expect(tabs[0]).toHaveAttribute('aria-selected', 'true');
    expect(tabs[1]).toHaveAttribute('aria-selected', 'false');
    expect(tabs[2]).toHaveAttribute('aria-selected', 'false');
  });

  it('names the list, so a page with two of them tells them apart', () => {
    render(Tabs, { props: { tabs: TABS, active: 'open', label: LABEL } });

    expect(screen.getByRole('tablist', { name: LABEL })).toBeInTheDocument();
  });

  it('keeps only the selected tab in the tab sequence', () => {
    render(Tabs, { props: { tabs: TABS, active: 'done', label: LABEL } });
    const tabs = screen.getAllByRole('tab');

    expect(tabs[0]).toHaveAttribute('tabindex', '-1');
    expect(tabs[1]).toHaveAttribute('tabindex', '0');
    expect(tabs[2]).toHaveAttribute('tabindex', '-1');
  });

  it('reports the tab a person clicks', async () => {
    const onSelect = vi.fn();
    render(Tabs, { props: { tabs: TABS, active: 'open', label: LABEL, onSelect } });

    await fireEvent.click(screen.getByRole('tab', { name: /Done/ }));

    expect(onSelect).toHaveBeenCalledWith('done');
    expect(screen.getByRole('tab', { name: /Done/ })).toHaveAttribute('aria-selected', 'true');
  });

  it('moves through the tabs with the arrow keys, wrapping at the ends', async () => {
    const onSelect = vi.fn();
    render(Tabs, { props: { tabs: TABS, active: 'open', label: LABEL, onSelect } });
    const tabs = screen.getAllByRole('tab');

    tabs[0].focus();
    await fireEvent.keyDown(tabs[0], { key: 'ArrowRight' });

    expect(document.activeElement).toBe(tabs[1]);
    expect(onSelect).toHaveBeenLastCalledWith('done');

    await fireEvent.keyDown(tabs[1], { key: 'ArrowLeft' });

    expect(document.activeElement).toBe(tabs[0]);
    expect(onSelect).toHaveBeenLastCalledWith('open');

    await fireEvent.keyDown(tabs[0], { key: 'ArrowLeft' });

    expect(document.activeElement).toBe(tabs[2]);
    expect(onSelect).toHaveBeenLastCalledWith('failed');
  });

  it('jumps to the first and the last tab with Home and End', async () => {
    const onSelect = vi.fn();
    render(Tabs, { props: { tabs: TABS, active: 'done', label: LABEL, onSelect } });
    const tabs = screen.getAllByRole('tab');

    tabs[1].focus();
    await fireEvent.keyDown(tabs[1], { key: 'End' });

    expect(document.activeElement).toBe(tabs[2]);
    expect(onSelect).toHaveBeenLastCalledWith('failed');

    await fireEvent.keyDown(tabs[2], { key: 'Home' });

    expect(document.activeElement).toBe(tabs[0]);
    expect(onSelect).toHaveBeenLastCalledWith('open');
  });

  it('draws the count of a tab as a badge, and none for a tab without one', () => {
    render(Tabs, { props: { tabs: TABS, active: 'open', label: LABEL } });
    const badges = screen.getAllByTestId('count-badge');

    expect(badges.map((badge) => badge.textContent?.trim())).toEqual(['2', '3']);
    expect(screen.getByRole('tab', { name: /Done/ }).querySelector('[data-testid]')).toBeNull();
  });

  it('ignores keys that are not the widget’s own', async () => {
    const onSelect = vi.fn();
    render(Tabs, { props: { tabs: TABS, active: 'open', label: LABEL, onSelect } });
    const tabs = screen.getAllByRole('tab');

    tabs[0].focus();
    await fireEvent.keyDown(tabs[0], { key: 'a' });

    expect(document.activeElement).toBe(tabs[0]);
    expect(onSelect).not.toHaveBeenCalled();
  });

  it('selects its first tab when the view fills the list after the render', async () => {
    const { rerender } = render(Tabs, { props: { tabs: [], active: undefined, label: LABEL } });

    await rerender({ tabs: TABS });

    const tabs = screen.getAllByRole('tab');
    expect(tabs[0]).toHaveAttribute('aria-selected', 'true');
    expect(tabs[0]).toHaveAttribute('tabindex', '0');
  });

  it('takes the tab the view selected when that tab arrives with the list', async () => {
    const { rerender } = render(Tabs, { props: { tabs: [], active: 'failed', label: LABEL } });

    await rerender({ tabs: TABS });

    expect(screen.getByRole('tab', { name: /Failed/ })).toHaveAttribute('aria-selected', 'true');
  });

  it('falls back to the first tab when the selected one leaves the list', async () => {
    const { rerender } = render(Tabs, { props: { tabs: TABS, active: 'failed', label: LABEL } });

    await rerender({ tabs: TABS.slice(0, 2), active: 'failed' });

    const tabs = screen.getAllByRole('tab');
    expect(tabs[0]).toHaveAttribute('aria-selected', 'true');
  });

  it('names the count and the panel a tab controls when the view does', () => {
    render(Tabs, {
      props: {
        tabs: [
          {
            id: 'open',
            label: 'Open',
            count: 2,
            countLabel: '2 need you',
            panelId: 'panel-open',
          },
        ],
        active: 'open',
        label: LABEL,
      },
    });
    const tab = screen.getByRole('tab', { name: 'Open 2 need you' });

    expect(tab).toHaveAttribute('aria-controls', 'panel-open');
    expect(within(tab).getByText('Open')).toBeInTheDocument();
  });
});
