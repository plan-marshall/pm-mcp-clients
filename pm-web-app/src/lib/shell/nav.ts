/**
 * The destinations of the app shell and the trail the top bar draws.
 *
 * One model feeds the rail, the phone's bottom bar and the path in the top bar, so a destination
 * is named once and its label, its icon and the group it belongs to cannot drift apart.
 */

import { ROUTE_TITLES, type AppRoute, type TitleState } from '../routes';

/** The three groups of the rail, in the order the design draws them. */
export type NavGroupId = 'work' | 'records' | 'machine';

/** One destination of the shell. */
export interface ShellNavItem {
  /** The stable id, used for the counted badges and the phone's selection. */
  id: string;
  /** The route it opens. */
  route: AppRoute;
  /** The catalogue key of its visible label. */
  labelKey: string;
  /** The delivered UI icon name. */
  icon: string;
  /** The id an end-to-end test finds it by. */
  testId: string;
  /** The count it carries, when it carries one. */
  count?: 'decisions' | 'reports';
}

/** One group of destinations, with the heading that names it. */
export interface ShellNavGroup {
  id: NavGroupId;
  labelKey: string;
  items: ShellNavItem[];
}

/**
 * The rail: three groups, each destination once.
 *
 * The attention inbox (`/`) arrives with its own route; until it exists the rail names the
 * destinations that can be opened today, so no control promises a page the app does not have.
 */
export const RAIL_GROUPS: readonly ShellNavGroup[] = [
  {
    id: 'work',
    labelKey: 'web.frame.rail.groups.work',
    items: [
      {
        id: 'running',
        route: 'overview',
        labelKey: 'overview.running.title',
        icon: 'nav.running',
        testId: 'nav-overview-btn',
      },
      {
        id: 'projects',
        route: 'projects',
        labelKey: 'project.explorer.title',
        icon: 'nav.projects',
        testId: 'nav-projects-btn',
      },
      {
        id: 'decisions',
        route: 'decisions',
        labelKey: 'web.frame.nav.decisions',
        icon: 'nav.decisions',
        testId: 'nav-decisions-btn',
        count: 'decisions',
      },
    ],
  },
  {
    id: 'records',
    labelKey: 'web.frame.rail.groups.records',
    items: [
      {
        id: 'reports',
        route: 'reports',
        labelKey: 'web.frame.nav.reports',
        icon: 'nav.reports',
        testId: 'nav-reports-btn',
        count: 'reports',
      },
    ],
  },
  {
    id: 'machine',
    labelKey: 'web.frame.rail.groups.machine',
    items: [
      {
        id: 'system',
        route: 'system',
        labelKey: 'system.view.title',
        icon: 'nav.system',
        testId: 'nav-system-btn',
      },
      {
        id: 'designer',
        route: 'designer',
        labelKey: 'web.frame.nav.designer',
        icon: 'nav.designer',
        testId: 'nav-designer-btn',
      },
      {
        id: 'settings',
        route: 'settings',
        labelKey: 'web.frame.settings',
        icon: 'nav.settings',
        testId: 'nav-settings-btn',
      },
    ],
  },
];

/** Every destination, flattened in rail order. */
export const ALL_NAV_ITEMS: readonly ShellNavItem[] = RAIL_GROUPS.flatMap(
  (group) => group.items,
);

/** The destinations the phone's bottom bar keeps; the rest live in its "More" sheet. */
const BOTTOM_IDS: ReadonlySet<string> = new Set(['running', 'decisions', 'reports']);

/** The destinations the phone's bar carries, before "More" is counted. */
export const BOTTOM_ITEMS: readonly ShellNavItem[] = ALL_NAV_ITEMS.filter((item) =>
  BOTTOM_IDS.has(item.id),
);

/**
 * The route a destination is selected for.
 *
 * A detail page keeps the destination it belongs to highlighted: a plan or an epic is work, the
 * cleanup is part of the system, and a machine setting is a setting.
 *
 * @param route the route the store shows
 * @returns the route of the destination that stays selected
 */
export function activeNavRoute(route: AppRoute): AppRoute {
  switch (route) {
    case 'plan':
    case 'epic':
    case 'decomposition':
    case 'start':
      return 'overview';
    case 'cleanup':
      return 'system';
    case 'machine-settings':
      return 'settings';
    default:
      return route;
  }
}

/**
 * The group a route belongs to.
 *
 * @param route the route the store shows
 * @returns its group, or `undefined` for a page outside the navigation (pairing, onboarding)
 */
export function navGroupOf(route: AppRoute): ShellNavGroup | undefined {
  const active = activeNavRoute(route);
  return RAIL_GROUPS.find((group) => group.items.some((item) => item.route === active));
}

/** One segment of the top bar's trail. */
export interface PathCrumb {
  /** The catalogue key of the segment. */
  key: string;
  /** The names the text carries. */
  args?: Record<string, string>;
  /** Whether the segment is an identifier that a phone shortens in its middle. */
  id?: boolean;
}

/**
 * The trail of a route: its group, the way back to its parent, and the page itself.
 *
 * A page outside the navigation carries its own title alone, so the top bar always names where the
 * person stands.
 *
 * @param route the route the store shows
 * @param state the store's names, for a title that carries one
 * @returns the segments, the first to the last
 */
export function pathOf(route: AppRoute, state: TitleState): PathCrumb[] {
  const crumbs: PathCrumb[] = [];
  const group = navGroupOf(route);
  if (group) {
    crumbs.push({ key: group.labelKey });
  }
  if (route === 'cleanup') {
    crumbs.push({ key: 'system.view.title' }, { key: 'system.cleanup.title' });
    return crumbs;
  }
  const title = ROUTE_TITLES[route](state);
  const crumb: PathCrumb = { key: title.key };
  if (title.args) {
    crumb.args = title.args;
  }
  if (route === 'plan' || route === 'epic' || route === 'decomposition') {
    crumb.id = true;
  }
  crumbs.push(crumb);
  return crumbs;
}

/** The storage key of the rail's collapsed state, kept per browser. */
export const RAIL_COLLAPSED_KEY = 'pm_rail_collapsed';

/**
 * @returns whether this browser left the rail collapsed; `false` when nothing was stored or the
 * browser refuses storage access
 */
export function readRailCollapsed(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }
  try {
    return window.localStorage.getItem(RAIL_COLLAPSED_KEY) === 'true';
  } catch {
    // A browser that denies storage still gets a working shell: the rail starts expanded.
    return false;
  }
}

/**
 * Keeps the rail's collapsed state for the next visit.
 *
 * @param collapsed whether the rail is collapsed
 */
export function writeRailCollapsed(collapsed: boolean): void {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    window.localStorage.setItem(RAIL_COLLAPSED_KEY, String(collapsed));
  } catch {
    // Storage refused the write. The rail still follows the toggle for this visit.
  }
}
