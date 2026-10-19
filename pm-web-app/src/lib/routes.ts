/**
 * The routes of the app, and the one route model the store, the router and the views share.
 *
 * The onboarding, the entry and the machine settings are routes of their own
 * (`/onboarding`, `/entry`, `/pair`, `/settings/machine`); the start of a plan or an epic is
 * addressed by its project and its open entry (`/projects/{id}/start`, `/entries/{id}`). Every
 * other view keeps its store-driven route so that existing deep links and the header navigation
 * still work.
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
  | 'decomposition'
  | 'start';

/** The project and the entry a start path names. */
export interface StartPath {
  projectId?: string;
  entryId?: string;
}

/** A path resolved to its route and, for a start, the project and entry it names. */
export interface PathMatch extends StartPath {
  route: AppRoute;
}

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
const PATH_ROUTES: ReadonlyArray<{ path: string; route: AppRoute }> = (
  Object.entries(ROUTE_PATHS) as Array<[AppRoute, string]>
).map(([route, path]) => ({ path, route }));

/**
 * Trims the trailing slashes of a path, keeping the root.
 *
 * @param pathname the browser path
 * @returns the path without trailing slashes
 */
function normalise(pathname: string): string {
  let end = pathname.length;
  while (end > 1 && pathname.codePointAt(end - 1) === 47 /* '/' */) {
    end--;
  }
  return pathname.slice(0, end);
}

/**
 * Resolves a path to its route and, for a start, the project and the entry it names.
 *
 * The start paths are `/start` (the project is chosen there), `/projects/{id}/start` (a new
 * entry under that project) and `/entries/{id}` (an entry that is already open, whichever client
 * opened it).
 *
 * @param pathname the browser path, e.g. `/settings/machine`
 * @returns the match, or `null` when the path names none of the app's routes
 */
export function matchPath(pathname = '/'): PathMatch | null {
  const trimmed = normalise(pathname || '/');
  for (const entry of PATH_ROUTES) {
    if (entry.path === trimmed) {
      return { route: entry.route };
    }
  }
  if (trimmed === '/start') {
    return { route: 'start' };
  }
  const projectStart = /^\/projects\/([^/]+)\/start$/.exec(trimmed);
  if (projectStart?.[1]) {
    return { route: 'start', projectId: decodeURIComponent(projectStart[1]) };
  }
  const entryPath = /^\/entries\/([^/]+)$/.exec(trimmed);
  if (entryPath?.[1]) {
    return { route: 'start', entryId: decodeURIComponent(entryPath[1]) };
  }
  return null;
}

/**
 * Resolves the route a path names.
 *
 * @param pathname the browser path, e.g. `/settings/machine`
 * @returns the route, or `null` when the path names none of the app's routes
 */
export function routeForPath(pathname = '/'): AppRoute | null {
  return matchPath(pathname)?.route ?? null;
}

/**
 * The path of a route, for the address bar.
 *
 * @param route the route
 * @param start the project and entry of a start route
 * @returns its path, or `null` when the route has no path of its own
 */
export function pathForRoute(route: AppRoute, start?: StartPath): string | null {
  if (route === 'start') {
    if (start?.entryId) {
      return `/entries/${encodeURIComponent(start.entryId)}`;
    }
    if (start?.projectId) {
      return `/projects/${encodeURIComponent(start.projectId)}/start`;
    }
    return '/start';
  }
  return ROUTE_PATHS[route] ?? null;
}
