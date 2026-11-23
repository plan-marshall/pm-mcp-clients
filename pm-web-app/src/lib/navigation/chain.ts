import { get } from 'svelte/store';
import { t } from '../i18n';
import type { AppRoute, Address, TitleState } from '../routes';
import type { ProcessTree, EpicResource } from '../api/client';
import type { Kind } from '../components/KindMark.svelte';
import { pathOf } from '../shell/nav';

/**
 * One segment of the navigation chain: kind, title and address.
 */
export interface ChainSegment {
  key?: string;
  kind?: Kind;
  title: string;
  address?: Address;
  id?: boolean;
}

/**
 * What the chain function reads to construct the chain from what the app already loads.
 */
export interface ChainFacts {
  plan?: ProcessTree | null;
  epic?: EpicResource | null;
  planId?: string | null;
  epicId?: string | null;
  projectId?: string | null;
  projectDisplayName?: string | null;
  taskId?: string | null;
  taskTitle?: string | null;
  titleState?: TitleState;
}

/**
 * Gives the levels of a route, from the lens down (project, epic, plan, task), each with kind,
 * title and address (PLAN-144 D2).
 *
 * @param route the current route
 * @param facts the facts loaded by the app (process tree, epic resource, selections)
 * @param translate translation function (defaults to $t)
 * @returns the chain segments, the first to the last
 */
export function buildChain(
  route: AppRoute,
  facts: ChainFacts = {},
  translate: (key: string, args?: Record<string, string | number>) => string = get(t),
): ChainSegment[] {
  if (route === 'task') {
    const plan = facts.plan;
    const planId = plan?.plan_id || facts.planId || '';
    const planTitle = plan?.title || planId;
    const projectId = plan?.project?.project_id || facts.projectId || '';
    const projectTitle = plan?.project?.display_name || projectId;
    const epic = plan?.epic;

    const segments: ChainSegment[] = [
      {
        kind: 'plan',
        title: translate('overview.running.title'),
        address: { route: 'overview' },
      },
    ];

    if (projectId) {
      segments.push({
        kind: 'project',
        title: projectTitle,
        address: { route: 'overview', project: projectId },
      });
    }

    if (epic) {
      segments.push({
        kind: 'epic',
        title: epic.title || epic.epic_id,
        address: { route: 'epic', epicId: epic.epic_id },
      });
    }

    if (planId) {
      segments.push({
        kind: 'plan',
        title: planTitle,
        address: { route: 'plan', planId },
        id: true,
      });
    }

    segments.push({
      kind: 'plan',
      title: facts.taskTitle || facts.taskId || 'Task',
      id: true,
    });

    return segments;
  }

  if (route === 'plan') {
    const plan = facts.plan;
    const planId = plan?.plan_id || facts.planId || '';
    const planTitle = plan?.title || planId;
    const projectId = plan?.project?.project_id || facts.projectId || '';
    const projectTitle = plan?.project?.display_name || projectId;
    const epic = plan?.epic;

    const segments: ChainSegment[] = [
      {
        kind: 'plan',
        title: translate('overview.running.title'),
        address: { route: 'overview' },
      },
    ];

    if (projectId) {
      segments.push({
        kind: 'project',
        title: projectTitle,
        address: { route: 'overview', project: projectId },
      });
    }

    if (epic) {
      segments.push({
        kind: 'epic',
        title: epic.title || epic.epic_id,
        address: { route: 'epic', epicId: epic.epic_id },
      });
    }

    segments.push({
      kind: 'plan',
      title: planTitle,
      id: true,
    });

    return segments;
  }

  if (route === 'epic') {
    const epic = facts.epic;
    const epicId = epic?.epic_id || facts.epicId || '';
    const epicTitle = epic?.display_name || epicId;
    const projectId = epic?.project_id || facts.projectId || '';
    const projectTitle = facts.projectDisplayName || projectId;

    const segments: ChainSegment[] = [
      {
        kind: 'plan',
        title: translate('overview.running.title'),
        address: { route: 'overview' },
      },
    ];

    if (projectId) {
      segments.push({
        kind: 'project',
        title: projectTitle,
        address: { route: 'overview', project: projectId },
      });
    }

    segments.push({
      kind: 'epic',
      title: epicTitle,
      id: true,
    });

    return segments;
  }

  if (route === 'overview') {
    const projectId = facts.projectId;
    const projectTitle = facts.projectDisplayName || projectId || '';

    if (projectId) {
      return [
        {
          kind: 'plan',
          title: translate('overview.running.title'),
          address: { route: 'overview' },
        },
        {
          kind: 'project',
          title: projectTitle,
          id: true,
        },
      ];
    }

    return [
      {
        kind: 'plan',
        title: translate('overview.running.title'),
      },
    ];
  }

  // Outside the lenses: fallback to group and title
  const rawCrumbs = pathOf(route, facts.titleState ?? {});
  return rawCrumbs.map((crumb, idx) => ({
    title: translate(crumb.key, crumb.args),
    id: crumb.id,
    address:
      idx < rawCrumbs.length - 1 && route === 'cleanup' && crumb.key === 'system.view.title'
        ? { route: 'system' }
        : undefined,
  }));
}

/**
 * Determines the parent segment from the chain that Up leads to.
 * Outside running work (the project page, cleanup, settings, reports) the slot is empty.
 *
 * @param chain the navigation chain
 * @param route the current route
 * @returns the parent segment with address, or null when absent
 */
export function upOf(chain: ChainSegment[], route?: AppRoute): ChainSegment | null {
  if (route && !isLensRoute(route)) {
    return null;
  }
  if (chain.length < 2) {
    return null;
  }
  const parent = chain[chain.length - 2];
  if (!parent.address) {
    return null;
  }
  return parent;
}

function isLensRoute(route: AppRoute): boolean {
  return route === 'task' || route === 'plan' || route === 'epic' || route === 'overview';
}
