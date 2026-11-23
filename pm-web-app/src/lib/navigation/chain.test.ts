import { describe, it, expect, beforeEach } from 'vitest';
import { buildChain, upOf, type ChainFacts } from './chain';
import type { ProcessTree, EpicResource } from '../api/client';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('the navigation chain (PLAN-144 D2)', () => {
  beforeEach(() => {
    useCatalogue('en');
  });

  const mockProcessTree: ProcessTree = {
    plan_id: 'fix-relay',
    title: 'Fix Relay Transport',
    project: {
      project_id: 'pm-mcp-clients',
      display_name: 'Relay Transport',
    },
    epic: {
      epic_id: 'relay-hardening',
      title: 'Relay Hardening',
    },
    current_phase: 'execute',
    phase_state: 'running',
    needs_operator: false,
    instance_version: 1,
    tokens_so_far: { total_tokens: 100, cost_usd: 0.01, duration_ms: 1000 },
    nodes: [],
    current_position_path: [],
  };

  const mockEpicResource: EpicResource = {
    epic_id: 'relay-hardening',
    project_id: 'pm-mcp-clients',
    display_name: 'Relay Hardening',
    state: 'running',
    auto_start: false,
    overlap_max_files: 5,
    scope: 3,
    workstreams: [],
    sources: [],
    landings: 1,
    findings: { count: 0, open: 0, resolved: 0 },
    watches: { count: 0, open: 0, resolved: 0 },
    tokens: {
      total: { total_tokens: 200, cost_usd: 0.02, duration_ms: 2000 },
      own: { total_tokens: 50, cost_usd: 0.005, duration_ms: 500 },
      plans: [],
      not_measured: false,
      not_measured_count: 0,
    },
    events: [],
  };

  it('builds the chain for a task: lens › project › epic › plan › task', () => {
    const facts: ChainFacts = {
      plan: mockProcessTree,
      taskId: 'TASK-1',
      taskTitle: 'Verify socket connection',
    };

    const chain = buildChain('task', facts);

    expect(chain).toHaveLength(5);

    // Segment 0: Lens
    expect(chain[0]).toEqual({
      kind: 'plan',
      title: 'Running work',
      address: { route: 'overview' },
    });

    // Segment 1: Project in running work
    expect(chain[1]).toEqual({
      kind: 'project',
      title: 'Relay Transport',
      address: { route: 'overview', project: 'pm-mcp-clients' },
    });

    // Segment 2: Epic
    expect(chain[2]).toEqual({
      kind: 'epic',
      title: 'Relay Hardening',
      address: { route: 'epic', epicId: 'relay-hardening' },
    });

    // Segment 3: Plan
    expect(chain[3]).toEqual({
      kind: 'plan',
      title: 'Fix Relay Transport',
      address: { route: 'plan', planId: 'fix-relay' },
      id: true,
    });

    // Segment 4: Task (current)
    expect(chain[4]).toEqual({
      kind: 'plan',
      title: 'Verify socket connection',
      id: true,
    });

    // Up from task leads to plan
    const up = upOf(chain, 'task');
    expect(up).toEqual({
      kind: 'plan',
      title: 'Fix Relay Transport',
      address: { route: 'plan', planId: 'fix-relay' },
      id: true,
    });
  });

  it('builds the chain for a plan with an epic: lens › project › epic › plan', () => {
    const facts: ChainFacts = {
      plan: mockProcessTree,
    };

    const chain = buildChain('plan', facts);

    expect(chain).toHaveLength(4);
    expect(chain[0].title).toBe('Running work');
    expect(chain[0].address).toEqual({ route: 'overview' });
    expect(chain[1].title).toBe('Relay Transport');
    expect(chain[1].address).toEqual({ route: 'overview', project: 'pm-mcp-clients' });
    expect(chain[2].title).toBe('Relay Hardening');
    expect(chain[2].address).toEqual({ route: 'epic', epicId: 'relay-hardening' });
    expect(chain[3].title).toBe('Fix Relay Transport');
    expect(chain[3].address).toBeUndefined(); // Current page has no address

    // Up from plan leads to epic
    const up = upOf(chain, 'plan');
    expect(up).toEqual({
      kind: 'epic',
      title: 'Relay Hardening',
      address: { route: 'epic', epicId: 'relay-hardening' },
    });
  });

  it('builds the chain for a plan without an epic: lens › project › plan', () => {
    const facts: ChainFacts = {
      plan: { ...mockProcessTree, epic: null },
    };

    const chain = buildChain('plan', facts);

    expect(chain).toHaveLength(3);
    expect(chain[0].title).toBe('Running work');
    expect(chain[1].title).toBe('Relay Transport');
    expect(chain[2].title).toBe('Fix Relay Transport');

    // Up from plan without epic leads to project in running work
    const up = upOf(chain, 'plan');
    expect(up).toEqual({
      kind: 'project',
      title: 'Relay Transport',
      address: { route: 'overview', project: 'pm-mcp-clients' },
    });
  });

  it('builds the chain for an epic: lens › project › epic', () => {
    const facts: ChainFacts = {
      epic: mockEpicResource,
    };

    const chain = buildChain('epic', facts);

    expect(chain).toHaveLength(3);
    expect(chain[0].kind).toBe('plan');
    expect(chain[0].title).toBe('Running work');
    expect(chain[0].address).toEqual({ route: 'overview' });
    expect(chain[1].kind).toBe('project');
    expect(chain[1].title).toBe('pm-mcp-clients');
    expect(chain[1].address).toEqual({ route: 'overview', project: 'pm-mcp-clients' });
    expect(chain[2].kind).toBe('epic');
    expect(chain[2].title).toBe('Relay Hardening');
    expect(chain[2].address).toBeUndefined();

    // Up from epic leads to project in running work
    const up = upOf(chain, 'epic');
    expect(up).toEqual({
      kind: 'project',
      title: 'pm-mcp-clients',
      address: { route: 'overview', project: 'pm-mcp-clients' },
    });
  });

  it('builds the chain for a project scoped in Running work: lens › project', () => {
    const facts: ChainFacts = {
      projectId: 'pm-mcp-clients',
      projectDisplayName: 'Relay Transport',
    };

    const chain = buildChain('overview', facts);

    expect(chain).toHaveLength(2);
    expect(chain[0].title).toBe('Running work');
    expect(chain[0].address).toEqual({ route: 'overview' });
    expect(chain[1].title).toBe('Relay Transport');
    expect(chain[1].address).toBeUndefined();

    // Up from project in running work leads to unfilled lens list
    const up = upOf(chain, 'overview');
    expect(up).toEqual({
      kind: 'plan',
      title: 'Running work',
      address: { route: 'overview' },
    });
  });

  it('returns no Up for the unfilled lens list', () => {
    const chain = buildChain('overview', {});

    expect(chain).toHaveLength(1);
    expect(chain[0].title).toBe('Running work');
    expect(upOf(chain, 'overview')).toBeNull();
  });

  it('returns no Up outside the lenses (e.g. cleanup, settings, reports)', () => {
    const cleanupChain = buildChain('cleanup', {});
    expect(upOf(cleanupChain, 'cleanup')).toBeNull();

    const reportsChain = buildChain('reports', {});
    expect(upOf(reportsChain, 'reports')).toBeNull();

    const settingsChain = buildChain('settings', {});
    expect(upOf(settingsChain, 'settings')).toBeNull();

    const projectsChain = buildChain('projects', {});
    expect(upOf(projectsChain, 'projects')).toBeNull();
  });
});
