/**
 * The routes of the app, and the one route model the store, the router and the views share.
 *
 * The onboarding, the entry and the machine settings are routes of their own
 * (`/onboarding`, `/entry`, `/pair`, `/settings/machine`); every other view keeps its
 * store-driven route so that existing deep links and the header navigation still work.
 */

export type AppRoute =
  | 'pair'
  | 'entry'
  | 'onboarding'
  | 'workspace'
  | 'settings'
  | 'machine-settings'
  | 'designer'
  | 'revoked'
  | 'listener-off'
  | 'overview'
  | 'reports'
  | 'decisions'
  | 'system'
  | 'cleanup'
  | 'projects'
  | 'plan'
  | 'epic'
  | 'decomposition';

/** Path of every route that has a page of its own. */
const ROUTE_PATHS: Partial<Record<AppRoute, string>> = {
  pair: '/pair',
  entry: '/entry',
  onboarding: '/onboarding',
  settings: '/settings',
  'machine-settings': '/settings/machine',
  projects: '/projects',
};

/** Routes that are reachable by path alone, without a selected plan or epic. */
const PATH_ROUTES: ReadonlyArray<{ path: string; route: AppRoute }> = [
  { path: '/settings/machine', route: 'machine-settings' },
  { path: '/settings', route: 'settings' },
  { path: '/onboarding', route: 'onboarding' },
  { path: '/entry', route: 'entry' },
  { path: '/pair', route: 'pair' },
  { path: '/projects', route: 'projects' },
];

/**
 * Resolves the route a path names.
 *
 * @param pathname the browser path, e.g. `/settings/machine`
 * @returns the route, or `null` when the path names none of the app's routes
 */
export function routeForPath(pathname = '/'): AppRoute | null {
  const trimmed = pathname || '/';
  let end = trimmed.length;
  while (end > 1 && trimmed.codePointAt(end - 1) === 47 /* '/' */) {
    end--;
  }
  const normalised = trimmed.slice(0, end);
  for (const entry of PATH_ROUTES) {
    if (entry.path === normalised) {
      return entry.route;
    }
  }
  return null;
}

/**
 * The path of a route, for the address bar.
 *
 * @param route the route
 * @returns its path, or `null` when the route has no path of its own
 */
export function pathForRoute(route: AppRoute): string | null {
  return ROUTE_PATHS[route] ?? null;
}
