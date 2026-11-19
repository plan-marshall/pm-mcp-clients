import { describe, it, expect } from 'vitest';
import { projectCountsText } from './projectCounts';
import type { Translate } from './index';
import type { ProjectSummary } from '../api/client';

/** @returns the key with its parameters, so a test can read which sentence was chosen */
const translate: Translate = (key, params) =>
  params ? `${key} ${JSON.stringify(params)}` : key;

/** @returns a project summary with the given counts, none of them set unless asked */
function project(counts: Partial<ProjectSummary>): ProjectSummary {
  return counts as ProjectSummary;
}

describe('projectCountsText', () => {
  it('names work of both kinds with both figures', () => {
    const text = projectCountsText(
      project({ active_plans_count: 2, active_epics_count: 3 }),
      translate,
    );

    expect(text).toBe('overview.project.counts {"plans":2,"epics":3}');
  });

  it('names plans alone when no epic runs', () => {
    const text = projectCountsText(project({ active_plans_count: 2, active_epics_count: 0 }), translate);

    expect(text).toBe('overview.project.plans_only {"plans":2}');
  });

  it('names epics alone when no plan runs', () => {
    const text = projectCountsText(project({ active_plans_count: 0, active_epics_count: 3 }), translate);

    expect(text).toBe('overview.project.epics_only {"epics":3}');
  });

  it('names the setup of a project that needs the operator and runs nothing', () => {
    const text = projectCountsText(
      project({ setup_needs_operator: true, active_plans_count: 0, active_epics_count: 4 }),
      translate,
    );

    expect(text).toBe('overview.project.setup');
  });

  it('names the plans of a project that needs the operator while its plans run', () => {
    const text = projectCountsText(
      project({ setup_needs_operator: true, active_plans_count: 1, active_epics_count: 0 }),
      translate,
    );

    expect(text).toBe('overview.project.plans_only {"plans":1}');
  });

  it('says nothing for a project that runs nothing and needs nothing', () => {
    expect(projectCountsText(project({}), translate)).toBe('');
  });

  it('reads missing counts as none rather than as a blank figure', () => {
    const text = projectCountsText(
      project({ active_plans_count: undefined, active_epics_count: undefined }),
      translate,
    );

    expect(text).toBe('');
  });
});
