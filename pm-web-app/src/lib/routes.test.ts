import { describe, it, expect } from 'vitest';
import {
  EPIC_TABS,
  PLAN_TABS,
  SETTINGS_TABS,
  canonicalAddress,
  formatAddress,
  matchPath,
  parseAddress,
  pathForRoute,
  replaceLegacyAddress,
  routeForPath,
} from './routes';

describe('the routes of the app', () => {
  it('resolves the onboarding, the entry and the settings by path', () => {
    expect(routeForPath('/onboarding')).toBe('onboarding');
    expect(routeForPath('/onboarding/')).toBe('onboarding');
    expect(routeForPath('/entry')).toBe('entry');
    expect(routeForPath('/pair')).toBe('pair');
    expect(routeForPath('/settings')).toBe('settings');
  });

  it('replaces the old machine settings address instead of resolving it', () => {
    // The two pages are one view now: the old path is a legacy address, not a route.
    expect(routeForPath('/settings/machine')).toBeNull();
    expect(replaceLegacyAddress('/settings/machine')).toBe('/settings?tab=setup');
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
    expect(routeForPath('/decomposition')).toBe('decomposition');
  });

  it('names no route for a path that is none of the app', () => {
    expect(routeForPath('/unknown')).toBeNull();
    expect(routeForPath('/')).toBeNull();
  });

  it('gives every route with a page its path', () => {
    expect(pathForRoute('onboarding')).toBe('/onboarding');
    expect(pathForRoute('entry')).toBe('/entry');
    expect(pathForRoute('pair')).toBe('/pair');
    expect(pathForRoute('settings')).toBe('/settings');
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
    expect(pathForRoute('decomposition')).toBe('/decomposition');
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

  it('gives one decision its own path', () => {
    expect(pathForRoute('decisions')).toBe('/decisions');
    expect(pathForRoute('decision', { decisionId: 'Q-007' })).toBe('/decisions/Q-007');
    expect(pathForRoute('decision', { decisionId: 'Q 7/8' })).toBe('/decisions/Q%207%2F8');
    expect(matchPath('/decisions')).toEqual({ route: 'decisions' });
    expect(matchPath('/decisions/Q-007')).toEqual({ route: 'decision', decisionId: 'Q-007' });
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
    // The decomposition workflow has `/decomposition` of its own, so `decompose` is an epic's id.
    expect(parseAddress('/decomposition')).toEqual({ route: 'decomposition' });
    expect(matchPath('/epics/decompose')).toEqual({ route: 'epic', epicId: 'decompose' });
  });

  it('resolves a project page, and names no start for a path that only looks like one', () => {
    expect(routeForPath('/projects/pm-mcp-clients')).toBe('project');
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

/**
 * The address model of `navigation.adoc#url`: path segments for objects, the query for scope, mode,
 * tab and what is opened beside. Every address of the document this plan owns round-trips through
 * parse and serialise, and a key the scheme does not know is dropped.
 */
describe('the address model', () => {
  it('resolves every object this plan owns', () => {
    expect(parseAddress('/projects/pm-mcp-clients')).toEqual({
      route: 'project',
      projectId: 'pm-mcp-clients',
    });
    expect(parseAddress('/plans/fix-relay')).toEqual({ route: 'plan', planId: 'fix-relay' });
    expect(parseAddress('/plans/fix-relay/tasks/TASK-4')).toEqual({
      route: 'task',
      planId: 'fix-relay',
      taskId: 'TASK-4',
    });
    expect(parseAddress('/decisions/Q-007')).toEqual({ route: 'decision', decisionId: 'Q-007' });
    expect(parseAddress('/reports/r-42')).toEqual({ route: 'report', reportId: 'r-42' });
    expect(parseAddress('/decomposition')).toEqual({ route: 'decomposition' });
    expect(parseAddress('/epics/relay-hardening')).toEqual({
      route: 'epic',
      epicId: 'relay-hardening',
    });
    expect(parseAddress('/epics/relay-hardening/decomposition')).toEqual({
      route: 'decomposition',
      epicId: 'relay-hardening',
    });
  });

  it.each([
    '/projects/pm-mcp-clients',
    '/plans/fix-relay',
    '/plans/fix-relay/tasks/TASK-4',
    '/decisions/Q-007',
    '/reports/r-42',
    '/decomposition',
    '/epics/relay-hardening',
    '/overview?project=pm-mcp-clients',
    '/overview?mode=ended&group=epic&follow=on',
    '/plans/fix-relay?mode=ended',
    '/plans/fix-relay?tab=intent',
    '/plans/fix-relay?tab=changes&file=pm-relay/pom.xml',
    '/plans/fix-relay?node=execute/land',
    '/plans/fix-relay?open=decision:Q-007',
    '/plans/fix-relay?open=completion-report:fix-relay',
    '/system/cleanup?open=plan:fix-relay',
    '/plans/fix-relay?chat=open',
    '/plans/fix-relay?chat=open&turn=turn-9',
    '/plans/fix-relay/tasks/TASK-4?chat=open',
    '/?lens=waits',
    '/epics/relay-hardening?tab=events',
    '/settings?tab=access',
    '/settings?tab=setup',
  ])('round-trips %s', (address) => {
    const parsed = parseAddress(address);
    expect(parsed, `address ${address} is not one of the scheme`).not.toBeNull();
    expect(formatAddress(parsed!)).toBe(address);
    expect(canonicalAddress(address)).toBe(address);
  });

  it('reads the query vocabulary into typed fields', () => {
    expect(parseAddress('/overview?project=pm-mcp-clients&mode=ended&group=epic&follow=on')).toEqual({
      route: 'overview',
      project: 'pm-mcp-clients',
      mode: 'ended',
      group: 'epic',
      follow: true,
    });
    expect(parseAddress('/plans/fix-relay?open=decision:Q-007')).toEqual({
      route: 'plan',
      planId: 'fix-relay',
      open: { kind: 'decision', id: 'Q-007' },
    });
    expect(parseAddress('/plans/fix-relay?chat=open&turn=turn-9')).toEqual({
      route: 'plan',
      planId: 'fix-relay',
      chat: true,
      turn: 'turn-9',
    });
    expect(parseAddress('/?lens=waits')).toEqual({ route: null, lens: 'waits' });
  });

  it('drops every key and value the scheme does not know', () => {
    expect(canonicalAddress('/plans/fix-relay?changes=open')).toBe('/plans/fix-relay');
    expect(parseAddress('/plans/fix-relay?changes=open')?.tab).toBeUndefined();
    expect(canonicalAddress('/plans/fix-relay?unknown=1&tab=bogus')).toBe('/plans/fix-relay');
    expect(canonicalAddress('/overview?mode=running&group=plan&follow=off')).toBe('/overview');
    expect(canonicalAddress('/plans/fix-relay?lens=waits')).toBe('/plans/fix-relay');
    // A turn without an open chat has no address of its own.
    expect(canonicalAddress('/plans/fix-relay?turn=turn-9')).toBe('/plans/fix-relay');
  });

  it('carries the lens modes on every address of the lens', () => {
    for (const route of ['/overview', '/plans/fix-relay', '/epics/relay-hardening']) {
      expect(canonicalAddress(`${route}?mode=ended`)).toBe(`${route}?mode=ended`);
    }
  });

  it('names every tab value of a plan and of an epic', () => {
    expect(PLAN_TABS).toEqual(['process', 'intent', 'changes']);
    expect(EPIC_TABS).toEqual(['plans', 'intent', 'landings', 'reviews', 'events']);
    // The first tab is the default and is never written.
    expect(canonicalAddress('/plans/fix-relay?tab=process')).toBe('/plans/fix-relay');
    expect(canonicalAddress('/epics/relay-hardening?tab=plans')).toBe('/epics/relay-hardening');
    for (const tab of PLAN_TABS.slice(1)) {
      expect(canonicalAddress(`/plans/fix-relay?tab=${tab}`)).toBe(`/plans/fix-relay?tab=${tab}`);
    }
    for (const tab of EPIC_TABS.slice(1)) {
      expect(canonicalAddress(`/epics/relay-hardening?tab=${tab}`)).toBe(
        `/epics/relay-hardening?tab=${tab}`,
      );
    }
  });

  it('names every tab of the settings, and writes every one of them', () => {
    expect(SETTINGS_TABS).toEqual(['setup', 'access', 'credentials', 'consents', 'retention']);
    // Unlike a plan's first tab, the settings' first tab is written too: one spelling per tab.
    for (const tab of SETTINGS_TABS) {
      expect(canonicalAddress(`/settings?tab=${tab}`)).toBe(`/settings?tab=${tab}`);
    }
    expect(parseAddress('/settings?tab=access')).toEqual({ route: 'settings', tab: 'access' });
    // A tab no view carries is dropped like any other value the scheme does not know.
    expect(canonicalAddress('/settings?tab=bogus')).toBe('/settings');
  });

  it('writes node only with the Process tab and file only with Changes', () => {
    expect(formatAddress({ route: 'plan', planId: 'fix-relay', node: 'execute/land' })).toBe(
      '/plans/fix-relay?node=execute/land',
    );
    expect(
      formatAddress({ route: 'plan', planId: 'fix-relay', tab: 'process', node: 'execute/land' }),
    ).toBe('/plans/fix-relay?node=execute/land');
    expect(formatAddress({ route: 'plan', planId: 'fix-relay', tab: 'intent', node: 'x' })).toBe(
      '/plans/fix-relay?tab=intent',
    );
    expect(formatAddress({ route: 'plan', planId: 'fix-relay', tab: 'changes', node: 'x' })).toBe(
      '/plans/fix-relay?tab=changes',
    );
    expect(formatAddress({ route: 'plan', planId: 'fix-relay', file: 'pm-relay/pom.xml' })).toBe(
      '/plans/fix-relay',
    );
    expect(
      formatAddress({ route: 'plan', planId: 'fix-relay', tab: 'changes', file: 'pm-relay/pom.xml' }),
    ).toBe('/plans/fix-relay?tab=changes&file=pm-relay/pom.xml');
  });

  it('keeps an id that looks like a workflow an object', () => {
    // `/decomposition` is the workflow's own path, so an epic called `decompose` is an id.
    expect(parseAddress('/epics/decompose')).toEqual({ route: 'epic', epicId: 'decompose' });
    expect(formatAddress({ route: 'epic', epicId: 'decompose' })).toBe('/epics/decompose');
    // A project called `onboard` is written as the object; PLAN-59 moves the onboarding that still
    // answers to `/projects/onboard` today, so the string stays canonical either way.
    expect(formatAddress({ route: 'project', projectId: 'onboard' })).toBe('/projects/onboard');
    expect(canonicalAddress('/projects/onboard')).toBe('/projects/onboard');
  });

  it('replaces every old address this plan owns', () => {
    expect(replaceLegacyAddress('/decisions?decision=Q-007')).toBe('/decisions/Q-007');
    expect(replaceLegacyAddress('/projects?project=pm-mcp-clients')).toBe(
      '/projects/pm-mcp-clients',
    );
    expect(replaceLegacyAddress('/epics/decompose')).toBe('/decomposition');
    expect(replaceLegacyAddress('/reports?plan=fix-relay')).toBe('/reports');
    // The plan a `/reports?plan=` address named stays readable, so the view can open its detail.
    expect(parseAddress('/reports?plan=fix-relay')?.legacyReportPlanId).toBe('fix-relay');
    // An address of the scheme is no legacy address.
    expect(replaceLegacyAddress('/plans/fix-relay')).toBeNull();
    expect(replaceLegacyAddress('/reports/r-42')).toBeNull();
  });

  it('names no address for a malformed escape', () => {
    expect(parseAddress('/plans/%E0')).toBeNull();
    expect(parseAddress('/decisions/%E0')).toBeNull();
    expect(parseAddress('/reports/%E0')).toBeNull();
    expect(parseAddress('/projects/%E0')).toBeNull();
    expect(canonicalAddress('/plans/%E0')).toBeNull();
  });
});
