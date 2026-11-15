/**
 * The routes of the app, and the one address model the store, the router and the views share.
 *
 * Every view has a canonical path: the pairing and entry pages (`/pair`, `/entry`), the
 * onboarding (`/onboarding`), the settings and the machine settings (`/settings`,
 * `/settings/machine`), the projects and one project page (`/projects`, `/projects/{id}`), a
 * project's onboarding (`/projects/onboard`), the overview (`/overview`), a plan and an epic by
 * their id (`/plans/{id}`, `/epics/{id}`), a task of a plan (`/plans/{id}/tasks/{task}`), the
 * decisions and one decision (`/decisions`, `/decisions/{id}`), the reports and one report
 * (`/reports`, `/reports/{id}`), the decomposition (`/decomposition`, or
 * `/epics/{id}/decomposition` for an epic already cut), the system and its cleanup (`/system`,
 * `/system/cleanup`), the designer (`/designer`), and the start (`/start`, `/projects/{id}/start`,
 * `/entries/{id}`).
 *
 * The query carries what is not an object: the lens's scope and modes (`project`, `mode`, `group`,
 * `follow`), a tab, an object opened in place (`open`), a node of the plan's phase tree, a file of
 * the plan's changes, the chat and its turn, and the lens of the start point. `parseAddress` reads
 * it, `formatAddress` writes it canonically, and a key or value the scheme does not know is
 * dropped, so one address has exactly one spelling.
 *
 * The pairing code lands on `/pair`; the address bar follows the store on every other route, so
 * a reload, a bookmark, and the browser's back all open the same view.
 *
 * The module also carries the tab title of every route: the catalogue key it reads and the name
 * the route gives it, so a title is never formatted without the name it needs.
 */

export type AppRoute =
  | 'pair'
  | 'entry'
  | 'onboarding'
  | 'project-onboarding'
  | 'settings'
  | 'machine-settings'
  | 'projects'
  | 'project'
  | 'overview'
  | 'plan'
  | 'task'
  | 'epic'
  | 'decomposition'
  | 'decisions'
  | 'decision'
  | 'reports'
  | 'report'
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
  /** The plan whose completion report the old `/reports?plan=` address named. */
  reportPlanId?: string;
  /** One report, by its id. */
  reportId?: string;
  /** The decision the decisions route shows. */
  decisionId?: string;
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

/** The tabs of a plan's object column; the first is the default. */
export type PlanTab = 'process' | 'intent' | 'changes';

/** The tabs of an epic's object column; the first is the default. */
export type EpicTab = 'plans' | 'intent' | 'landings' | 'reviews' | 'events';

/** The tab of an object column, whichever object it belongs to. */
export type ObjectTab = PlanTab | EpicTab;

/** Every tab of a plan, first the default that an address omits. */
export const PLAN_TABS: ReadonlyArray<PlanTab> = ['process', 'intent', 'changes'];

/** Every tab of an epic, first the default that an address omits. */
export const EPIC_TABS: ReadonlyArray<EpicTab> = ['plans', 'intent', 'landings', 'reviews', 'events'];

/** The mode of a lens's list: running work is the default and is never written. */
export type LensMode = 'ended';

/** The grouping of a lens's list: by plan is the default and is never written. */
export type Grouping = 'epic';

/** The lens of the start point, which an address names so it wins over the remembered place. */
export type LensAddress = 'waits';

/** The kinds of object an address can open in place beside its caller. */
export type OpenKind = 'plan' | 'epic' | 'decision' | 'completion-report';

/** The objects an address can open in place, with the id it names. */
export const OPEN_KINDS: ReadonlyArray<OpenKind> = [
  'plan',
  'epic',
  'decision',
  'completion-report',
];

/** An object opened in place beside its caller: `open={kind}:{id}`. */
export interface InPlaceObject {
  kind: OpenKind;
  id: string;
}

/** One address of the scheme: its route, the ids in its path, and the query it carries. */
export interface Address extends RouteParams {
  /** The route the path names, or `null` for the start point at `/`. */
  route: AppRoute | null;
  /** The selected project of the lens, `?project={id}`. */
  project?: string;
  /** `?mode=ended`; running work is the default and is never written. */
  mode?: LensMode;
  /** `?group=epic`; by plan is the default and is never written. */
  group?: Grouping;
  /** `?follow=on`; the lens follows the running task. */
  follow?: boolean;
  /** `?tab={id}`, a tab of the object column. */
  tab?: ObjectTab;
  /** `?open={kind}:{id}`, an object opened in place. */
  open?: InPlaceObject;
  /** `?node={path}`, a node of the plan's phase tree, written only with the Process tab. */
  node?: string;
  /** `?file={path}`, a file of the plan's changes, written only with the Changes tab. */
  file?: string;
  /** `?chat=open`, the chat as a side column. */
  chat?: boolean;
  /** `?turn={id}`, the turn of that chat, written only with it open. */
  turn?: string;
  /** `?lens=waits`, the start point's own lens. */
  lens?: LensAddress;
  /** The plan an old `/reports?plan=` address named: read once, never written again. */
  legacyReportPlanId?: string;
}

/** The store fields a tab title reads: the names the routes carry. */
export interface TitleState {
  /** The plan the plan and task routes show. */
  selectedPlanId?: string | null;
  /** The epic the epic and decomposition routes show. */
  selectedEpicId?: string | null;
  /** The project a start begins in. */
  startProjectId?: string | null;
  /** Whether the open start is a plan's or an epic's. */
  startKind?: 'plan' | 'epic' | null;
  /** The project whose onboarding the project-onboarding route shows. */
  selectedOnboardingId?: string | null;
}

/** The catalogue key of a route's tab title, and the names its text carries. */
export interface RouteTitle {
  key: string;
  args?: Record<string, string>;
}

/** The tab title of a route whose text names nothing. */
function plainTitle(key: string): RouteTitle {
  return { key };
}

/**
 * The tab title of a route that names one thing, and its unparameterized form while that name is
 * not known yet.
 *
 * @param key the catalogue key of the text that carries the name
 * @param loadingKey the catalogue key of the text used before the name is known
 * @param name the parameter the text reads
 * @param value the name the route carries, if it carries one
 * @returns the key and its arguments
 */
function namedTitle(
  key: string,
  loadingKey: string,
  name: string,
  value: string | null | undefined,
): RouteTitle {
  return value ? { key, args: { [name]: value } } : plainTitle(loadingKey);
}

/**
 * The tab title of the start routes, which name the project a start begins in.
 *
 * A start is a plan's or an epic's, and the title says which: the start view itself uses the same
 * two keys, so the tab and the page heading never disagree.
 *
 * @param state the store's names
 * @returns the key and its arguments
 */
function startTitle(state: TitleState): RouteTitle {
  const epic = state.startKind === 'epic';
  return namedTitle(
    epic ? 'start.epic.title' : 'start.plan.title',
    epic ? 'start.epic.title.loading' : 'start.plan.title.loading',
    'project',
    state.startProjectId,
  );
}

/**
 * The tab title of every route: its catalogue key, and the names a route carries.
 *
 * A parameterized key is never formatted without its argument, so no `{placeholder}` reaches the
 * tab: until the name is known the route uses the plain `…title.loading` text instead.
 */
export const ROUTE_TITLES: Record<AppRoute, (state: TitleState) => RouteTitle> = {
  pair: () => plainTitle('web.pair.page.title'),
  entry: startTitle,
  onboarding: () => plainTitle('onboard.entry.title'),
  'project-onboarding': (state) =>
    namedTitle(
      'project.onboarding.title',
      'project.onboarding.title_new',
      'project',
      state.selectedOnboardingId,
    ),
  settings: () => plainTitle('web.settings.page.title'),
  'machine-settings': () => plainTitle('web.settings.page.title'),
  projects: () => plainTitle('project.explorer.title'),
  // The project page is the Projects lens's object column, so it carries the lens's own name.
  project: () => plainTitle('project.explorer.title'),
  overview: () => plainTitle('overview.running.title'),
  plan: (state) =>
    namedTitle('plan.view.title', 'plan.view.title.loading', 'plan', state.selectedPlanId),
  // A task is a level of its plan, so its tab names the plan it belongs to.
  task: (state) =>
    namedTitle('plan.view.title', 'plan.view.title.loading', 'plan', state.selectedPlanId),
  epic: (state) =>
    namedTitle('epic.view.title', 'epic.view.title.loading', 'epic', state.selectedEpicId),
  decomposition: (state) =>
    namedTitle('epic.view.title', 'epic.view.title.loading', 'epic', state.selectedEpicId),
  decisions: () => plainTitle('decisions.list.title'),
  decision: () => plainTitle('decisions.list.title'),
  reports: () => plainTitle('report.list.title'),
  report: () => plainTitle('report.list.title'),
  system: () => plainTitle('system.view.title'),
  cleanup: () => plainTitle('system.view.title'),
  designer: () => plainTitle('designer.view.title'),
  start: startTitle,
  revoked: () => plainTitle('devices.revoked.title'),
  'listener-off': () => plainTitle('web.entry.lan_off.title'),
};

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
  // The decomposition has a path of its own, so an epic called `decompose` is an id.
  decomposition: '/decomposition',
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

/**
 * The paths that carry identifiers, matched after the fixed paths.
 *
 * Each pattern's capture groups fill `names`, in order. A workflow's path stands beside the ids or
 * below its object's id (`/projects/{id}/start`), never as a name beside them.
 */
const PARAMETERIZED_PATHS: ReadonlyArray<{
  pattern: RegExp;
  route: AppRoute;
  names: ReadonlyArray<keyof RouteParams>;
}> = [
  // A decomposition that is already cut names its epic; `/decomposition` names none and is a
  // fixed path above, so this one never swallows it.
  {
    pattern: /^\/epics\/([^/]+)\/decomposition$/,
    route: 'decomposition',
    names: ['epicId'],
  },
  // A task is a level of its plan and takes the path.
  { pattern: /^\/plans\/([^/]+)\/tasks\/([^/]+)$/, route: 'task', names: ['planId', 'taskId'] },
  { pattern: /^\/epics\/([^/]+)$/, route: 'epic', names: ['epicId'] },
  { pattern: /^\/plans\/([^/]+)$/, route: 'plan', names: ['planId'] },
  { pattern: /^\/projects\/([^/]+)\/start$/, route: 'start', names: ['projectId'] },
  { pattern: /^\/projects\/([^/]+)$/, route: 'project', names: ['projectId'] },
  { pattern: /^\/entries\/([^/]+)$/, route: 'start', names: ['entryId'] },
  { pattern: /^\/decisions\/([^/]+)$/, route: 'decision', names: ['decisionId'] },
  { pattern: /^\/reports\/([^/]+)$/, route: 'report', names: ['reportId'] },
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
    if (!match) continue;
    const found: PathMatch = { route: entry.route };
    for (const [index, name] of entry.names.entries()) {
      const raw = match[index + 1];
      if (!raw) return null;
      const value = decodeSegment(raw);
      if (!value) return null;
      Object.assign(found, { [name]: value });
    }
    return found;
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
  if (route === 'task') {
    return params.planId && params.taskId
      ? `/plans/${encodeURIComponent(params.planId)}/tasks/${encodeURIComponent(params.taskId)}`
      : null;
  }
  if (route === 'epic') {
    return params.epicId ? `/epics/${encodeURIComponent(params.epicId)}` : null;
  }
  if (route === 'decomposition') {
    return params.epicId
      ? `/epics/${encodeURIComponent(params.epicId)}/decomposition`
      : '/decomposition';
  }
  if (route === 'project') {
    return params.projectId ? `/projects/${encodeURIComponent(params.projectId)}` : null;
  }
  if (route === 'decision') {
    return params.decisionId ? `/decisions/${encodeURIComponent(params.decisionId)}` : null;
  }
  if (route === 'report') {
    return params.reportId ? `/reports/${encodeURIComponent(params.reportId)}` : null;
  }
  // The two writers below are the old addresses this plan replaces. No caller passes them any
  // more; they stay only so an address already in the bar keeps its path, and
  // `replaceLegacyAddress` replaces them on open.
  if (route === 'reports' && params.reportPlanId) {
    return `/reports?plan=${encodeURIComponent(params.reportPlanId)}`;
  }
  if (route === 'projects' && params.explorerProjectId) {
    return `/projects?project=${encodeURIComponent(params.explorerProjectId)}`;
  }
  if (route === 'decisions' && params.decisionId) {
    return `/decisions?decision=${encodeURIComponent(params.decisionId)}`;
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

/**
 * Splits an address into its path and its query.
 *
 * @param url a path with an optional query, e.g. `/plans/fix-relay?tab=changes`
 * @returns the path and the query
 */
function splitAddress(url: string): { path: string; query: URLSearchParams } {
  const question = url.indexOf('?');
  const rawPath = question < 0 ? url : url.slice(0, question);
  const rawQuery = question < 0 ? '' : url.slice(question + 1);
  return { path: rawPath || '/', query: new URLSearchParams(rawQuery) };
}

/** A query value that names something, or `undefined` for an absent or empty one. */
function valueOf(query: URLSearchParams, key: string): string | undefined {
  const value = query.get(key);
  return value ? value : undefined;
}

/**
 * The tab an address names, when the route's object has that tab.
 *
 * @param route the route of the address
 * @param raw the `tab` value
 * @returns the tab, or `undefined` when the route has no such tab
 */
function tabOf(route: AppRoute | null, raw: string | undefined): ObjectTab | undefined {
  if (!raw) return undefined;
  if (route === 'plan' && (PLAN_TABS as ReadonlyArray<string>).includes(raw)) {
    return raw as PlanTab;
  }
  if (route === 'epic' && (EPIC_TABS as ReadonlyArray<string>).includes(raw)) {
    return raw as EpicTab;
  }
  return undefined;
}

/**
 * The object an `open` value names, when it names one of the scheme's kinds.
 *
 * @param raw the `open` value, `{kind}:{id}`
 * @returns the object, or `undefined` for a value the scheme does not know
 */
function openOf(raw: string | undefined): InPlaceObject | undefined {
  if (!raw) return undefined;
  const colon = raw.indexOf(':');
  if (colon <= 0 || colon === raw.length - 1) return undefined;
  const kind = raw.slice(0, colon);
  const id = raw.slice(colon + 1);
  if (!(OPEN_KINDS as ReadonlyArray<string>).includes(kind) || !id) return undefined;
  return { kind: kind as OpenKind, id };
}

/**
 * Reads an address of the scheme.
 *
 * The path names the object; the query carries the lens's scope and modes, a tab, an object opened
 * in place, a node, a file and the chat. A key or value the scheme does not know is dropped rather
 * than kept, so a parsed address is already canonical.
 *
 * @param url a path with an optional query, e.g. `/plans/fix-relay?tab=changes`
 * @returns the address, or `null` when the path names none of the app's routes
 */
export function parseAddress(url: string): Address | null {
  const split = splitAddress(url);
  const match = matchPath(split.path);

  if (!match) {
    // The start point is registered by PLAN-49; its lens is part of the scheme already, so an
    // address that names it parses and serialises here.
    if (normalise(split.path) === '/' && split.query.get('lens') === 'waits') {
      return { route: null, lens: 'waits' };
    }
    return null;
  }

  const address: Address = { ...match };
  const { query } = split;

  const project = valueOf(query, 'project');
  if (project) address.project = project;
  if (query.get('mode') === 'ended') address.mode = 'ended';
  if (query.get('group') === 'epic') address.group = 'epic';
  if (query.get('follow') === 'on') address.follow = true;

  const tab = tabOf(match.route, valueOf(query, 'tab'));
  if (tab) address.tab = tab;

  const open = openOf(valueOf(query, 'open'));
  if (open) address.open = open;

  // A key belongs to the tab it is written with: `node` to Process, `file` to Changes. The default
  // tab is Process, so a node needs no tab of its own.
  const processShown = tab === undefined || tab === 'process';
  if (match.route === 'plan' && processShown) {
    const node = valueOf(query, 'node');
    if (node) address.node = node;
  }
  if (match.route === 'plan' && tab === 'changes') {
    const file = valueOf(query, 'file');
    if (file) address.file = file;
  }

  if (query.get('chat') === 'open') {
    address.chat = true;
    const turn = valueOf(query, 'turn');
    if (turn) address.turn = turn;
  }

    // The old `/reports?plan=` address: its plan is read once so the view can open that report's
    // detail, and it is never written again.
    if (match.route === 'reports') {    const legacy = valueOf(query, 'plan');
    if (legacy) address.legacyReportPlanId = legacy;
  }

  return address;
}

/** Encodes one query value, keeping the `/` and `:` the scheme's own examples write. */
function encodeValue(value: string): string {
  return encodeURIComponent(value).replaceAll('%2F', '/').replaceAll('%3A', ':');
}

/**
 * Writes an address canonically.
 *
 * The path names the object and the query follows one fixed order, so one address has exactly one
 * spelling. A key whose tab is not shown is dropped (`node` outside Process, `file` outside
 * Changes), as is a turn without an open chat and an id the route cannot write.
 *
 * @param address the address to write
 * @returns its path, or `null` when the route has no path without the id it needs
 */
export function formatAddress(address: Address): string | null {
  const path =
    address.route === null
      ? '/'
      : pathForRoute(address.route, {
          projectId: address.projectId,
          entryId: address.entryId,
          planId: address.planId,
          epicId: address.epicId,
          taskId: address.taskId,
          reportId: address.reportId,
          onboardingStep: address.onboardingStep,
          // A list route's decision travels in the path of its own route, never in the old query.
          decisionId: address.route === 'decision' ? address.decisionId : undefined,
        });
  if (!path) return null;

  const parts: string[] = [];
  const add = (key: string, value: string | undefined): void => {
    if (value) parts.push(`${key}=${encodeValue(value)}`);
  };

  add('project', address.route === 'overview' || address.route === 'designer' ? address.project : undefined);
  add('mode', address.mode);
  add('group', address.group);
  if (address.follow) add('follow', 'on');

  const tab = tabOf(address.route, address.tab);
  const defaultTab = address.route === 'plan' ? 'process' : 'plans';
  if (tab && tab !== defaultTab) add('tab', tab);

  if (address.open) add('open', `${address.open.kind}:${address.open.id}`);

  // `node` belongs to Process and `file` to Changes; each is written only with its tab.
  if (address.route === 'plan' && tab !== 'intent' && tab !== 'changes') add('node', address.node);
  if (address.route === 'plan' && tab === 'changes') add('file', address.file);

  if (address.chat) {
    add('chat', 'open');
    add('turn', address.turn);
  }

  add('lens', address.lens);

  return parts.length > 0 ? `${path}?${parts.join('&')}` : path;
}

/**
 * Reads an address and writes it back canonically.
 *
 * @param url a path with an optional query
 * @returns the canonical address, or `null` when the path names none of the app's routes
 */
export function canonicalAddress(url: string): string | null {
  const address = parseAddress(url);
  return address ? formatAddress(address) : null;
}

/**
 * The address that replaces an old address of a moved view, when this address is one.
 *
 * Pre-1.0 there is no other compatibility: the caller replaces the address bar once, when the
 * address is opened. The onboarding addresses of the projects (`/projects/onboard`) and the two
 * settings paths belong to other plans and are not replaced here.
 *
 * @param url a path with an optional query
 * @returns the replacement address, or `null` when the address is not one of the old ones
 */
export function replaceLegacyAddress(url: string): string | null {
  const split = splitAddress(url);
  const path = normalise(split.path);

  if (path === '/epics/decompose') {
    return '/decomposition';
  }
  if (path === '/decisions') {
    const decision = valueOf(split.query, 'decision');
    return decision ? `/decisions/${encodeURIComponent(decision)}` : null;
  }
  if (path === '/reports') {
    // The report's id is not in the old address; the list is its replacement and the view opens
    // the detail of the plan the query named.
    return valueOf(split.query, 'plan') ? '/reports' : null;
  }
  if (path === '/projects') {
    const project = valueOf(split.query, 'project');
    return project ? `/projects/${encodeURIComponent(project)}` : null;
  }
  return null;
}
