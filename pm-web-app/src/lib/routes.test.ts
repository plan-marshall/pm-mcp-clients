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
  });

  it('gives no path to a route the store selects', () => {
    expect(pathForRoute('plan')).toBeNull();
    expect(pathForRoute('epic')).toBeNull();
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

  it('names no start for a path that only looks like one', () => {
    expect(routeForPath('/projects/pm-mcp-clients')).toBeNull();
    expect(routeForPath('/entries')).toBeNull();
    expect(routeForPath('/entries/a/b')).toBeNull();
  });

  it('names no start for a malformed escape instead of throwing', () => {
    expect(routeForPath('/entries/%E0')).toBeNull();
    expect(routeForPath('/projects/%E0/start')).toBeNull();
  });
});
