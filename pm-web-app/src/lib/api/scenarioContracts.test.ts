import { describe, it, expect } from 'vitest';
import { scenario, scenarioFetch, type ScenarioFile } from './scenarioMockFetch';

/**
 * The process and worker-activity contracts the canonical scenarios owe the web app.
 *
 * A scenario is a fixture, not a generated answer: a route added to `running-work` is not in
 * `fully-onboarded`, and an `activity_link` promises a route the app can read. These tests walk
 * both canonical scenarios, so the plan view's tree and the activity feed are exercised there.
 */

interface ProcessNodeShape {
  node_id?: unknown;
  title?: unknown;
  state?: unknown;
  sub_nodes?: unknown;
  leaf_tasks?: unknown;
}

interface LeafTaskShape {
  task_id?: unknown;
  status?: unknown;
  activity_link?: unknown;
}

interface ProcessTreeShape {
  plan_id?: unknown;
  project?: { project_id?: unknown };
  nodes?: unknown;
  current_position_path?: string[];
}

/** The tasks with no link but an explicit, empty activity route, on purpose. */
const ACTIVITY_LESS_TASKS = new Set(['fix-relay/TASK-002', 'session-log/TASK-002']);

/**
 * @param nodes the JSON nodes of a process representation
 * @param visit the visitor called once per node, at every depth
 */
function walkNodes(nodes: unknown, visit: (node: ProcessNodeShape) => void): void {
  if (!Array.isArray(nodes)) return;
  for (const raw of nodes) {
    if (!raw || typeof raw !== 'object') continue;
    const node = raw as ProcessNodeShape;
    visit(node);
    walkNodes(node.sub_nodes, visit);
  }
}

/**
 * @param file a canonical scenario
 * @returns every process tree route the scenario serves, by plan id
 */
function processRoutes(file: ScenarioFile): Map<string, ProcessTreeShape> {
  const processes = new Map<string, ProcessTreeShape>();
  for (const [route, entry] of Object.entries(file.routes)) {
    const match = /^GET \/api\/v1\/plans\/([^/]+)\/process$/.exec(route);
    if (match) {
      processes.set(match[1], entry.representation as ProcessTreeShape);
    }
  }
  return processes;
}

/**
 * @param process a process representation
 * @returns every leaf task it carries, with its plan-relative identity
 */
function leafTasksOf(process: ProcessTreeShape): Array<{
  taskId: string;
  status: string;
  activityLink: string | null;
}> {
  const tasks: Array<{ taskId: string; status: string; activityLink: string | null }> = [];
  walkNodes(process.nodes, (node) => {
    if (!Array.isArray(node.leaf_tasks)) return;
    for (const raw of node.leaf_tasks) {
      if (!raw || typeof raw !== 'object') continue;
      const task = raw as LeafTaskShape;
      if (typeof task.task_id !== 'string') continue;
      tasks.push({
        taskId: task.task_id,
        status: typeof task.status === 'string' ? task.status : '',
        activityLink: typeof task.activity_link === 'string' ? task.activity_link : null,
      });
    }
  });
  return tasks;
}

describe('canonical scenario process contracts', () => {
  for (const name of ['running-work', 'fully-onboarded'] as const) {
    it(`${name} serves the session-log process and its activity routes`, async () => {
      const file = scenario(name);

      expect(file.routes['GET /api/v1/plans/session-log/process']).toBeDefined();
      expect(file.routes['GET /api/v1/plans/session-log/tasks/TASK-001/activity']).toBeDefined();
      expect(file.routes['GET /api/v1/plans/session-log/tasks/TASK-003/activity']).toBeDefined();
      // TASK-002 is done with no activity. Its explicit route must answer an empty feed; a
      // missing route would be filled by the mock runtime's generic dynamic fallback.
      const quietRoute = file.routes['GET /api/v1/plans/session-log/tasks/TASK-002/activity'];
      expect(quietRoute).toBeDefined();
      if (!quietRoute) throw new Error('session-log TASK-002 activity route is missing');
      expect((quietRoute.representation as { entries?: unknown[] }).entries).toEqual([]);

      const quietResponse = await scenarioFetch(name).fetch(
        'http://localhost/api/v1/plans/session-log/tasks/TASK-002/activity',
      );
      expect(quietResponse.status).toBe(200);
      expect(((await quietResponse.json()) as { entries?: unknown[] }).entries).toEqual([]);

      const response = await scenarioFetch(name).fetch(
        'http://localhost/api/v1/plans/session-log/process',
      );
      expect(response.status).toBe(200);
      const body = (await response.json()) as ProcessTreeShape;

      expect(body.plan_id).toBe('session-log');
      expect(body.project?.project_id).toBe('pm-mcp-core');

      const nodes = (body.nodes ?? []) as ProcessNodeShape[];
      const byId = new Map(nodes.map((node) => [node.node_id, node]));
      expect(byId.get('init')?.state).toBe('done');
      expect(byId.get('refine')?.state).toBe('done');
      expect(byId.get('outline')?.state).toBe('done');
      expect(byId.get('plan')?.state).toBe('done');
      expect(byId.get('execute')?.state).toBe('running');
      expect(byId.get('finalize')?.state).toBe('planned');

      const donePhases = ['init', 'refine', 'outline', 'plan'];
      for (const phase of donePhases) {
        expect(byId.get(phase)?.leaf_tasks).toEqual([]);
      }

      const execute = byId.get('execute');
      const taskIds = leafTasksOf({ nodes: execute?.sub_nodes }).map((task) => task.taskId);
      expect(taskIds).toEqual(['TASK-001', 'TASK-002', 'TASK-003']);
      expect(body.current_position_path?.at(-1)).toBe('TASK-003');

      const plans = file.routes['GET /api/v1/plans'].representation as {
        plans: Array<{ id: string }>;
      };
      expect(plans.plans.some((plan) => plan.id === 'session-log')).toBe(true);
    });

    it(`${name} gives every activity link a route and keeps the activity-less task real`, () => {
      const file = scenario(name);
      const processes = processRoutes(file);
      let activityLess = 0;

      for (const [planId, process] of processes) {
        for (const task of leafTasksOf(process)) {
          const route = `GET /api/v1/plans/${planId}/tasks/${task.taskId}/activity`;
          if (task.activityLink) {
            expect(file.routes[`GET ${task.activityLink}`], route).toBeDefined();
          } else {
            activityLess++;
            const quietRoute = file.routes[route];
            expect(quietRoute, route).toBeDefined();
            if (!quietRoute) throw new Error(route + ' is missing');
            expect((quietRoute.representation as { entries?: unknown[] }).entries, route).toEqual(
              [],
            );
          }
        }
      }

      // A done task with an explicit empty route proves the empty feed state in the plan view.
      expect(activityLess).toBeGreaterThan(0);
      // The fixture that carries the empty-feed case is named, so a fixture edit cannot quietly
      // turn the branch back into an ordinary link or a missing route.
      for (const identity of ACTIVITY_LESS_TASKS) {
        const [planId, taskId] = identity.split('/');
        const task = leafTasksOf(processes.get(planId) ?? {}).find(
          (candidate) => candidate.taskId === taskId,
        );
        expect(task?.activityLink, identity).toBeNull();
        const quietRoute = file.routes[`GET /api/v1/plans/${planId}/tasks/${taskId}/activity`];
        expect(quietRoute, identity).toBeDefined();
        if (!quietRoute) throw new Error(identity + ' activity route is missing');
        expect((quietRoute.representation as { entries?: unknown[] }).entries, identity).toEqual(
          [],
        );
      }
    });
  }
});
