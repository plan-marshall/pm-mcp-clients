import { describe, it, expect } from 'vitest';
import { pathForRoute, routeForPath } from './routes';

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
});
