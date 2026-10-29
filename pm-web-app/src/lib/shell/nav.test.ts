import { describe, it, expect, beforeEach } from 'vitest';
import {
  ALL_NAV_ITEMS,
  BOTTOM_ITEMS,
  RAIL_COLLAPSED_KEY,
  RAIL_GROUPS,
  activeNavRoute,
  navGroupOf,
  pathOf,
  readRailCollapsed,
  writeRailCollapsed,
} from './nav';
import type { TitleState } from '../routes';

/**
 * The shell's navigation model: three groups, the destination a detail page keeps selected, the
 * trail of the top bar, and the collapsed rail this browser remembers.
 */
describe('the shell navigation model', () => {
  const state: TitleState = {
    selectedPlanId: 'fix-relay',
    selectedEpicId: 'relay-hardening',
  };

  it('draws three groups, each destination once', () => {
    expect(RAIL_GROUPS.map((group) => group.id)).toEqual(['work', 'records', 'machine']);
    expect(RAIL_GROUPS.map((group) => group.labelKey)).toEqual([
      'web.frame.rail.groups.work',
      'web.frame.rail.groups.records',
      'web.frame.rail.groups.machine',
    ]);

    const ids = ALL_NAV_ITEMS.map((item) => item.id);
    expect(ids).toEqual(['running', 'projects', 'decisions', 'reports', 'system', 'designer', 'settings']);
    expect(new Set(ids).size).toBe(ids.length);
    expect(ALL_NAV_ITEMS.every((item) => item.icon.startsWith('nav.'))).toBe(true);
  });

  it('keeps the four phone destinations, the rest in the More sheet', () => {
    expect(BOTTOM_ITEMS.map((item) => item.id)).toEqual(['running', 'decisions', 'reports']);
  });

  it('keeps the destination of a detail page selected', () => {
    expect(activeNavRoute('plan')).toBe('overview');
    expect(activeNavRoute('epic')).toBe('overview');
    expect(activeNavRoute('decomposition')).toBe('overview');
    expect(activeNavRoute('start')).toBe('overview');
    expect(activeNavRoute('cleanup')).toBe('system');
    expect(activeNavRoute('machine-settings')).toBe('settings');
    expect(activeNavRoute('reports')).toBe('reports');
  });

  it('places a route in its group, and a page outside the navigation in none', () => {
    expect(navGroupOf('plan')?.id).toBe('work');
    expect(navGroupOf('cleanup')?.id).toBe('machine');
    expect(navGroupOf('pair')).toBeUndefined();
    expect(navGroupOf('onboarding')).toBeUndefined();
  });

  it('draws the trail of a page from its group and its title', () => {
    expect(pathOf('overview', state)).toEqual([
      { key: 'web.frame.rail.groups.work' },
      { key: 'overview.running.title' },
    ]);

    expect(pathOf('plan', state)).toEqual([
      { key: 'web.frame.rail.groups.work' },
      { key: 'plan.view.title', args: { plan: 'fix-relay' }, id: true },
    ]);

    expect(pathOf('cleanup', state)).toEqual([
      { key: 'web.frame.rail.groups.machine' },
      { key: 'system.view.title' },
      { key: 'system.cleanup.title' },
    ]);

    expect(pathOf('pair', state)).toEqual([{ key: 'web.pair.page.title' }]);
  });
});

describe('the rail this browser remembers', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('starts expanded and keeps the collapsed state across a reload', () => {
    expect(readRailCollapsed()).toBe(false);

    writeRailCollapsed(true);
    expect(readRailCollapsed()).toBe(true);
    expect(localStorage.getItem(RAIL_COLLAPSED_KEY)).toBe('true');

    writeRailCollapsed(false);
    expect(readRailCollapsed()).toBe(false);
  });
});
