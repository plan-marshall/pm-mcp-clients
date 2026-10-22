/**
 * The routes of the app, and the one route model the store, the router and the views share.
 *
 * Every view has a canonical path: the pairing and entry pages (`/pair`, `/entry`), the
 * onboarding (`/onboarding`), the settings and the machine settings (`/settings`,
 * `/settings/machine`), the project explorer and a project's onboarding (`/projects`,
 * `/projects/onboard`), the overview (`/overview`), a plan and an epic by their id
 * (`/plans/{id}`, `/epics/{id}`), an epic's decomposition (`/epics/decompose`, or
 * `/epics/{id}/decomposition` for an epic already cut), the decisions (`/decisions`), the
 * reports (`/reports`), the system and its cleanup (`/system`, `/system/cleanup`), the designer
 * (`/designer`), and the start (`/start`, `/projects/{id}/start`, `/entries/{id}`).
 *
 * The pairing code lands on `/pair`; the address bar follows the store on every other route, so
 * a reload, a bookmark, and the browser's back all open the same view.
 */

export type AppRoute =
  | 'pair'
  | 'entry'
  | 'onboarding'
  | 'project-onboarding'
  | 'settings'
  | 'machine-settings'
  | 'projects'
  | 'overview'
  | 'plan'
  | 'epic'
  | 'decomposition'
  | 'decisions'
  | 'reports'
  | 'system'
  | 'cleanup'
  | 'designer'
  | 'start'
  | 'revoked'
  | 'listener-off';

/** The identifiers a parameterized path carries. */
export interface RouteParams {
  projectId?: string;
  entryId?: string;
  planId?: string;
  epicId?: string;
  taskId?: string;
  /** The plan whose completion report the reports route shows. */
  reportPlanId?: string;
  /** The project the explorer focuses, so a plan's project is one step away. */
  explorerProjectId?: string;
  /** The onboarding step the screen opens at, carried in `/onboarding?step=…`. */
  onboardingStep?: number;
}

/** The project and the entry a start path names. */
export type StartPath = Pick<RouteParams, 'projectId' | 'entryId'>;

/** A path resolved to its route and the identifiers it names. */
export interface PathMatch extends RouteParams {
  route: AppRoute;
}

/** Path of every route that has a fixed path of its own. */
const FIXED_ROUTE_PATHS: Partial<Record<AppRoute, string>> = {
  pair: '/pair',
  entry: '/entry',
  onboarding: '/onboarding',
  'project-onboarding': '/projects/onboard',
  settings: '/settings',
  'machine-settings': '/settings/machine',
  projects: '/projects',
  overview: '/overview',
  decomposition: '/epics/decompose',
  decisions: '/decisions',
  reports: '/reports',
  system: '/system',
  cleanup: '/system/cleanup',
  designer: '/designer',
};

/**
 * The fixed paths, longest first, so `/settings/machine` is resolved before `/settings` and
 * `/system/cleanup` before `/system`.
 */
const FIXED_PATHS: ReadonlyArray<{ path: string; route: AppRoute }> = (
  Object.entries(FIXED_ROUTE_PATHS) as Array<[AppRoute, string]>
)
  .map(([route, path]) => ({ path, route }))
  .sort((left, right) => right.path.length - left.path.length);

/** The paths that carry one identifier, matched after the fixed paths. */
const PARAMETERIZED_PATHS: ReadonlyArray<{
  pattern: RegExp;
  route: AppRoute;
  parameter: 'planId' | 'epicId' | 'projectId' | 'entryId';
}> = [
  // A decomposition that is already cut names its epic; `/epics/decompose` names none and is
  // listed as a fixed path above, so this one never swallows it.
  {
    pattern: /^\/epics\/([^/]+)\/decomposition$/,
    route: 'decomposition',
    parameter: 'epicId',
  },
  { pattern: /^\/epics\/([^/]+)$/, route: 'epic', parameter: 'epicId' },
  { pattern: /^\/plans\/([^/]+)$/, route: 'plan', parameter: 'planId' },
  { pattern: /^\/projects\/([^/]+)\/start$/, route: 'start', parameter: 'projectId' },
  { pattern: /^\/entries\/([^/]+)$/, route: 'start', parameter: 'entryId' },
];

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
 * Decodes one path segment, returning `null` for a malformed escape.
 *
 * @param segment the encoded segment
 * @returns the decoded segment, or `null` when it cannot be decoded
 */
function decodeSegment(segment: string): string | null {
  try {
    return decodeURIComponent(segment);
  } catch {
    return null;
  }
}

/**
 * Resolves a path to its route and the identifiers it names.
 *
 * @param pathname the browser path, e.g. `/settings/machine`
 * @returns the match, or `null` when the path names none of the app's routes
 */
export function matchPath(pathname = '/'): PathMatch | null {
  const trimmed = normalise(pathname || '/');

  for (const entry of FIXED_PATHS) {
    if (entry.path === trimmed) {
      return { route: entry.route };
    }
  }

  if (trimmed === '/start') {
    return { route: 'start' };
  }

  for (const entry of PARAMETERIZED_PATHS) {
    const match = entry.pattern.exec(trimmed);
    if (!match?.[1]) continue;
    const value = decodeSegment(match[1]);
    if (!value) return null;
    return { route: entry.route, [entry.parameter]: value };
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
 * A route that needs an identifier it was not given has no path: the caller keeps the address
 * bar as it is rather than writing `/`.
 *
 * @param route the route
 * @param params the identifiers the route needs, e.g. a plan's id
 * @returns its path, or `null` when the route has no path without a selection
 */
export function pathForRoute(route: AppRoute, params: RouteParams = {}): string | null {
  if (route === 'start') {
    if (params.entryId) {
      return `/entries/${encodeURIComponent(params.entryId)}`;
    }
    if (params.projectId) {
      return `/projects/${encodeURIComponent(params.projectId)}/start`;
    }
    return '/start';
  }
  if (route === 'plan') {
    return params.planId ? `/plans/${encodeURIComponent(params.planId)}` : null;
  }
  if (route === 'epic') {
    return params.epicId ? `/epics/${encodeURIComponent(params.epicId)}` : null;
  }
  if (route === 'decomposition') {
    return params.epicId
      ? `/epics/${encodeURIComponent(params.epicId)}/decomposition`
      : '/epics/decompose';
  }
  if (route === 'reports') {
    return params.reportPlanId
      ? `/reports?plan=${encodeURIComponent(params.reportPlanId)}`
      : '/reports';
  }
  if (route === 'projects') {
    return params.explorerProjectId
      ? `/projects?project=${encodeURIComponent(params.explorerProjectId)}`
      : '/projects';
  }
  if (route === 'designer') {
    // The selected project travels in the query, so a reload and a bookmark keep it.
    return params.projectId
      ? `/designer?project=${encodeURIComponent(params.projectId)}`
      : '/designer';
  }
  if (route === 'onboarding' && params.onboardingStep) {
    // The step travels in the query, so a reload and the browser's back keep it.
    return `/onboarding?step=${params.onboardingStep}`;
  }
  return FIXED_ROUTE_PATHS[route] ?? null;
}
