import { describe, it, expect } from 'vitest';
import { matchPath, pathForRoute, routeForPath } from './routes';

describe('the routes of the app', () => {
  it('resolves the onboarding, the entry and the machine settings by path', () => {
    expect(routeForPath('/onboarding')).toBe('onboarding');
    expect(routeForPath('/onboarding/')).toBe('onboarding');
    expect(routeForPath('/entry')).toBe('entry');
    expect(routeForPath('/pair')).toBe('pair');
    expect(routeForPath('/settings/machine')).toBe('machine-settings');
    expect(routeForPath('/settings')).toBe('settings');
  });

  it('resolves the machine settings before its parent', () => {
    // `/settings/machine` must not be read as the devices page.
    expect(routeForPath('/settings/machine')).not.toBe('settings');
    expect(routeForPath('/system/cleanup')).toBe('cleanup');
    expect(routeForPath('/system/cleanup')).not.toBe('system');
  });

  it('resolves every view that has a path of its own', () => {
    expect(routeForPath('/projects')).toBe('projects');
    expect(routeForPath('/projects/onboard')).toBe('project-onboarding');
    expect(routeForPath('/overview')).toBe('overview');
    expect(routeForPath('/decisions')).toBe('decisions');
    expect(routeForPath('/reports')).toBe('reports');
    expect(routeForPath('/system')).toBe('system');
    expect(routeForPath('/designer')).toBe('designer');
    expect(routeForPath('/epics/decompose')).toBe('decomposition');
  });

  it('names no route for a path that is none of the app', () => {
    expect(routeForPath('/unknown')).toBeNull();
    expect(routeForPath('/')).toBeNull();
  });

  it('gives every route with a page its path', () => {
    expect(pathForRoute('onboarding')).toBe('/onboarding');
    expect(pathForRoute('entry')).toBe('/entry');
    expect(pathForRoute('pair')).toBe('/pair');
    expect(pathForRoute('machine-settings')).toBe('/settings/machine');
    expect(pathForRoute('project-onboarding')).toBe('/projects/onboard');
    expect(pathForRoute('overview')).toBe('/overview');
    expect(pathForRoute('decisions')).toBe('/decisions');
    expect(pathForRoute('reports')).toBe('/reports');
    expect(pathForRoute('system')).toBe('/system');
    expect(pathForRoute('cleanup')).toBe('/system/cleanup');
    expect(pathForRoute('designer')).toBe('/designer');
  });

  it('gives no path to a route that needs a selection it was not given', () => {
    expect(pathForRoute('plan')).toBeNull();
    expect(pathForRoute('epic')).toBeNull();
    // The decomposition without an epic has its own path.
    expect(pathForRoute('decomposition')).toBe('/epics/decompose');
  });

  it('carries the designer’s project in the query', () => {
    expect(pathForRoute('designer')).toBe('/designer');
    expect(pathForRoute('designer', { projectId: 'pm-mcp-clients' })).toBe(
      '/designer?project=pm-mcp-clients',
    );
    expect(pathForRoute('designer', { projectId: 'a b' })).toBe('/designer?project=a%20b');
  });

  it('carries the onboarding step in the query', () => {
    expect(pathForRoute('onboarding', { onboardingStep: 6 })).toBe('/onboarding?step=6');
    expect(matchPath('/onboarding')).toEqual({ route: 'onboarding' });
  });

  it('carries the decision a match opened in the query', () => {
    expect(pathForRoute('decisions')).toBe('/decisions');
    expect(pathForRoute('decisions', { decisionId: 'Q-007' })).toBe('/decisions?decision=Q-007');
    expect(pathForRoute('decisions', { decisionId: 'Q 7/8' })).toBe(
      '/decisions?decision=Q%207%2F8',
    );
    expect(matchPath('/decisions')).toEqual({ route: 'decisions' });
  });

  it('resolves a plan and an epic by their id', () => {
    expect(matchPath('/plans/plan-42')).toEqual({ route: 'plan', planId: 'plan-42' });
    expect(matchPath('/epics/epic-7')).toEqual({ route: 'epic', epicId: 'epic-7' });
    expect(matchPath('/epics/epic-7/decomposition')).toEqual({
      route: 'decomposition',
      epicId: 'epic-7',
    });
    expect(pathForRoute('plan', { planId: 'plan-42' })).toBe('/plans/plan-42');
    expect(pathForRoute('epic', { epicId: 'epic-7' })).toBe('/epics/epic-7');
    expect(pathForRoute('decomposition', { epicId: 'epic-7' })).toBe('/epics/epic-7/decomposition');
  });

  it('resolves the start paths and the project and entry they name', () => {
    expect(matchPath('/start')).toEqual({ route: 'start' });
    expect(matchPath('/projects/pm-mcp-clients/start')).toEqual({
      route: 'start',
      projectId: 'pm-mcp-clients',
    });
    expect(matchPath('/entries/ent-3f9a2c41')).toEqual({
      route: 'start',
      entryId: 'ent-3f9a2c41',
    });
    expect(routeForPath('/start/')).toBe('start');
  });

  it('gives the start its project and entry path', () => {
    expect(pathForRoute('start')).toBe('/start');
    expect(pathForRoute('start', { projectId: 'pm-mcp-clients' })).toBe(
      '/projects/pm-mcp-clients/start',
    );
    expect(pathForRoute('start', { entryId: 'ent-3f9a2c41' })).toBe('/entries/ent-3f9a2c41');
  });

  it('keeps the decomposed epic apart from the bare epic route', () => {
    // `/epics/decompose` is the view without an epic, never the id of an epic.
    expect(matchPath('/epics/decompose')).toEqual({ route: 'decomposition' });
    expect(matchPath('/epics/decompose')).not.toEqual({
      route: 'epic',
      epicId: 'decompose',
    });
  });

  it('names no start for a path that only looks like one', () => {
    expect(routeForPath('/projects/pm-mcp-clients')).toBeNull();
    expect(routeForPath('/entries')).toBeNull();
    expect(routeForPath('/entries/a/b')).toBeNull();
  });

  it('names no route for a malformed escape instead of throwing', () => {
    expect(routeForPath('/entries/%E0')).toBeNull();
    expect(routeForPath('/projects/%E0/start')).toBeNull();
    expect(routeForPath('/plans/%E0')).toBeNull();
    expect(routeForPath('/epics/%E0')).toBeNull();
  });

  it('decodes an id that carries an escape', () => {
    expect(pathForRoute('plan', { planId: 'a b' })).toBe('/plans/a%20b');
    expect(matchPath('/plans/a%20b')).toEqual({ route: 'plan', planId: 'a b' });
  });
});
