import { describe, expect, it } from 'vitest';
import { ROUTE_TITLES, type AppRoute, type TitleState } from './routes';
import { formatString } from './i18n';
import { loadMessages } from '../test/catalogue-fixture';

/**
 * A tab title is a finished sentence: every route renders one, with and without the name it
 * carries, and no `{placeholder}` ever reaches the tab.
 */

const ROUTES = Object.keys(ROUTE_TITLES) as AppRoute[];

/** A store with no selection: the route knows no name yet. */
const NOTHING_SELECTED: TitleState = {};

/** A store that names a plan, an epic, a project and a project onboarding. */
const SELECTED: TitleState = {
  selectedPlanId: 'drop-legacy-api',
  selectedEpicId: 'relay-hardening',
  startProjectId: 'pm-mcp-clients',
  selectedOnboardingId: 'pm-mcp-clients',
};

for (const locale of ['en', 'de'] as const) {
  const catalogue = loadMessages(locale);

  describe(`the tab titles in ${locale}`, () => {
    it.each(ROUTES)('renders %s before its argument is known', (route) => {
      const { key, args } = ROUTE_TITLES[route](NOTHING_SELECTED);
      const title = formatString(catalogue[key] ?? '', args);

      expect(title, `route ${route} names no catalogue text`).not.toBe('');
      expect(title).not.toContain('{');
    });

    it.each(ROUTES)('renders %s with its argument', (route) => {
      const { key, args } = ROUTE_TITLES[route](SELECTED);
      const title = formatString(catalogue[key] ?? '', args);

      expect(title, `route ${route} names no catalogue text`).not.toBe('');
      expect(title).not.toContain('{');
    });
  });
}

describe('the names a route title carries', () => {
  it('names the plan of the plan route and its epic', () => {
    expect(ROUTE_TITLES.plan(SELECTED)).toEqual({
      key: 'plan.view.title',
      args: { plan: 'drop-legacy-api' },
    });
    expect(ROUTE_TITLES.epic(SELECTED)).toEqual({
      key: 'epic.view.title',
      args: { epic: 'relay-hardening' },
    });
    expect(ROUTE_TITLES.decomposition(SELECTED)).toEqual({
      key: 'epic.view.title',
      args: { epic: 'relay-hardening' },
    });
  });

  it('names the project a start begins in and the one being set up', () => {
    expect(ROUTE_TITLES.start(SELECTED)).toEqual({
      key: 'start.plan.title',
      args: { project: 'pm-mcp-clients' },
    });
    expect(ROUTE_TITLES.entry(SELECTED)).toEqual({
      key: 'start.plan.title',
      args: { project: 'pm-mcp-clients' },
    });
    expect(ROUTE_TITLES['project-onboarding'](SELECTED)).toEqual({
      key: 'project.onboarding.title',
      args: { project: 'pm-mcp-clients' },
    });
  });

  it('falls back to the unparameterized key while the name is unknown', () => {
    expect(ROUTE_TITLES.plan(NOTHING_SELECTED)).toEqual({ key: 'plan.view.title.loading' });
    expect(ROUTE_TITLES.epic(NOTHING_SELECTED)).toEqual({ key: 'epic.view.title.loading' });
    expect(ROUTE_TITLES.start(NOTHING_SELECTED)).toEqual({ key: 'start.plan.title.loading' });
    expect(ROUTE_TITLES['project-onboarding'](NOTHING_SELECTED)).toEqual({
      key: 'project.onboarding.title_new',
    });
  });
});
