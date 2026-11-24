import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import WorkListColumn from './WorkListColumn.svelte';
import {
  apiClient,
  type ActiveEpicSummary,
  type ActivePlanSummary,
  type ProcessTree,
  type ProjectDetailResponse,
} from '../api/client';
import * as runtime from '../stores/runtime';
import * as history from '../navigation/history';

describe('WorkListColumn component', () => {
  const mockDetail: ProjectDetailResponse = {
    project_id: 'pm-mcp-clients',
    display_name: 'pm-mcp-clients',
    plans: [
      {
        plan_id: 'staged-plan',
        title: 'Staged Plan',
        current_phase: 'init',
        phase_state: 'staged',
        tasks_done: 0,
        tasks_total: 2,
      },
      {
        plan_id: 'fix-relay',
        title: 'Fix Relay Transport',
        current_phase: 'execute',
        phase_state: 'running',
        tasks_done: 2,
        tasks_total: 3,
      },
    ],
    epics: [
      {
        epic_id: 'staged-epic',
        title: 'Staged Epic',
        running_plans_count: 0,
        staged_plans_count: 1,
        plans: [],
      },
      {
        epic_id: 'relay-hardening',
        title: 'Relay Hardening',
        running_plans_count: 1,
        staged_plans_count: 2,
        plans: [],
      },
    ],
    alongside_landings: [],
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders loading skeleton while fetching project detail', () => {
    vi.spyOn(apiClient, 'getProjectDetail').mockReturnValue(new Promise(() => {}));

    render(WorkListColumn, { projectId: 'pm-mcp-clients', currentPlanId: 'fix-relay' });

    expect(screen.getByTestId('work-list-loading')).toBeInTheDocument();
  });

  it('renders plans and epics in the order served, KindMark, status, and progress pill (D4)', async () => {
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue(mockDetail);

    render(WorkListColumn, { projectId: 'pm-mcp-clients', currentPlanId: 'fix-relay' });

    await waitFor(() => {
      expect(screen.getByTestId('work-list-items')).toBeInTheDocument();
    });

    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(4);

    // List appears in the order the runtime served (PLAN-82) and never sorted again
    expect(items[0]).toHaveTextContent('Staged Plan');
    expect(items[1]).toHaveTextContent('Fix Relay Transport');
    expect(items[2]).toHaveTextContent('Staged Epic');
    expect(items[3]).toHaveTextContent('Relay Hardening');

    // Progress pills in order
    const progressPills = screen.getAllByTestId('plan-progress');
    expect(progressPills[0]).toHaveTextContent('0/2');
    expect(progressPills[1]).toHaveTextContent('2/3');

    // Selected row has aria-current="page"
    const currentLink = screen.getByRole('link', { name: /Fix Relay Transport/i });
    expect(currentLink).toHaveAttribute('aria-current', 'page');

    const otherLink = screen.getByRole('link', { name: /Staged Plan/i });
    expect(otherLink).not.toHaveAttribute('aria-current');
  });

  it('replaces the address when a sibling plan is clicked (D4)', async () => {
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue(mockDetail);
    const openPlanSpy = vi.spyOn(runtime, 'openPlanView').mockImplementation(() => {});
    const writeAddressSpy = vi.spyOn(history, 'writeAddress').mockImplementation(() => true);

    render(WorkListColumn, { projectId: 'pm-mcp-clients', currentPlanId: 'fix-relay' });

    await waitFor(() => {
      expect(screen.getByTestId('work-list-items')).toBeInTheDocument();
    });

    const stagedLink = screen.getByRole('link', { name: /Staged Plan/i });
    await fireEvent.click(stagedLink);

    expect(openPlanSpy).toHaveBeenCalledWith('staged-plan');
    expect(writeAddressSpy).toHaveBeenCalledWith(expect.anything(), 'replace');
  });

  it('replaces the address when a sibling epic is clicked (D4)', async () => {
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue(mockDetail);
    const openEpicSpy = vi.spyOn(runtime, 'openEpicView').mockImplementation(() => {});
    const writeAddressSpy = vi.spyOn(history, 'writeAddress').mockImplementation(() => true);

    render(WorkListColumn, { projectId: 'pm-mcp-clients', currentPlanId: 'fix-relay' });

    await waitFor(() => {
      expect(screen.getByTestId('work-list-items')).toBeInTheDocument();
    });

    const epicLink = screen.getByRole('link', { name: /Relay Hardening/i });
    await fireEvent.click(epicLink);

    expect(openEpicSpy).toHaveBeenCalledWith('relay-hardening');
    expect(writeAddressSpy).toHaveBeenCalledWith(expect.anything(), 'replace');
  });

  it('renders error state and retries on failure (D4)', async () => {
    const errorSpy = vi
      .spyOn(apiClient, 'getProjectDetail')
      .mockRejectedValue(new Error('Network error'));

    render(WorkListColumn, { projectId: 'pm-mcp-clients' });

    await waitFor(() => {
      expect(screen.getByTestId('work-list-error')).toBeInTheDocument();
    });

    errorSpy.mockResolvedValueOnce(mockDetail);
    await fireEvent.click(screen.getByRole('button', { name: /Try again/i }));

    await waitFor(() => {
      expect(screen.getByTestId('work-list-items')).toBeInTheDocument();
    });
  });

  /**
   * @param planId the plan
   * @param title its title
   * @param epicId the epic it belongs to, if any
   * @returns the plan row the runtime serves in a project's detail
   */
  function planRow(planId: string, title: string, epicId?: string): ActivePlanSummary {
    return {
      plan_id: planId,
      title,
      current_phase: 'execute',
      phase_state: 'running',
      tasks_done: 2,
      tasks_total: 3,
      ...(epicId ? { epic_id: epicId } : {}),
    };
  }

  /**
   * @param epicId the epic
   * @param title its title
   * @param plans its plans
   * @returns the epic row the runtime serves in a project's detail
   */
  function epicRow(
    epicId: string,
    title: string,
    plans: ActivePlanSummary[],
  ): ActiveEpicSummary {
    return {
      epic_id: epicId,
      title,
      running_plans_count: 1,
      staged_plans_count: 0,
      plans,
    };
  }

  it('lists the epic of the open plan for a plan of an epic (D4)', async () => {
    // Arrange: a project whose plans belong to epics and whose epics carry their own plans
    const detail: ProjectDetailResponse = {
      project_id: 'pm-mcp-clients',
      display_name: 'pm-mcp-clients',
      plans: [
        planRow('fix-relay', 'Fix Relay Transport', 'relay-hardening'),
        planRow('relay-metrics', 'Relay metrics', 'relay-hardening'),
        planRow('loose-plan', 'Loose Plan'),
      ],
      epics: [
        // The mock's own detail lists one plan under the epic while the flat list carries a
        // second plan of the same epic: the column must show both.
        epicRow('relay-hardening', 'Relay Hardening', [
          planRow('fix-relay', 'Fix Relay Transport', 'relay-hardening'),
        ]),
        epicRow('staged-epic', 'Staged Epic', []),
      ],
      alongside_landings: [],
    };
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue(detail);

    // Act: the open plan names its epic
    render(WorkListColumn, {
      projectId: 'pm-mcp-clients',
      currentPlanId: 'fix-relay',
      parentEpicId: 'relay-hardening',
    });

    // Assert: the epic's plans, with the open one still marked
    await waitFor(() => expect(screen.getByTestId('work-list-items')).toBeInTheDocument());

    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent('Fix Relay Transport');
    expect(items[1]).toHaveTextContent('Relay metrics');
    expect(screen.getByRole('link', { name: /Fix Relay Transport/i })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('lists the epics and the epic-less plans of the project for an epic (D4)', async () => {
    // Arrange
    const detail: ProjectDetailResponse = {
      project_id: 'pm-mcp-clients',
      display_name: 'pm-mcp-clients',
      plans: [
        planRow('fix-relay', 'Fix Relay Transport', 'relay-hardening'),
        planRow('loose-plan', 'Loose Plan'),
      ],
      epics: [
        epicRow('relay-hardening', 'Relay Hardening', []),
        epicRow('staged-epic', 'Staged Epic', []),
      ],
      alongside_landings: [],
    };
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue(detail);

    // Act: no parent epic is named
    render(WorkListColumn, { projectId: 'pm-mcp-clients', currentEpicId: 'relay-hardening' });

    // Assert: the epics and the plan that belongs to none, the open epic marked
    await waitFor(() => expect(screen.getByTestId('work-list-items')).toBeInTheDocument());

    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('Loose Plan');
    expect(items[1]).toHaveTextContent('Relay Hardening');
    expect(items[2]).toHaveTextContent('Staged Epic');
    expect(screen.getByRole('link', { name: /Relay Hardening/i })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('lists the epics and the epic-less plans for a plan without an epic (D4)', async () => {
    // Arrange: one plan belongs to an epic, one to none, and one names an epic the detail omits
    const detail: ProjectDetailResponse = {
      project_id: 'pm-mcp-clients',
      display_name: 'pm-mcp-clients',
      plans: [
        planRow('fix-relay', 'Fix Relay Transport', 'relay-hardening'),
        planRow('loose-plan', 'Loose Plan'),
        planRow('orphan-plan', 'Orphan Plan', 'retired-epic'),
      ],
      epics: [epicRow('relay-hardening', 'Relay Hardening', [])],
      alongside_landings: [],
    };
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue(detail);

    // Act: the open object is the epic-less plan
    render(WorkListColumn, { projectId: 'pm-mcp-clients', currentPlanId: 'loose-plan' });

    // Assert: the project's epics, the epic-less plan and the plan whose epic is not served
    await waitFor(() => expect(screen.getByTestId('work-list-items')).toBeInTheDocument());

    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('Loose Plan');
    expect(items[1]).toHaveTextContent('Orphan Plan');
    expect(items[2]).toHaveTextContent('Relay Hardening');
    expect(screen.getByRole('link', { name: /Loose Plan/i })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('renders UpLink in the column head when a parent exists (D5)', async () => {
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue(mockDetail);
    runtime.openPlanView('fix-relay');
    runtime.runtimeStore.update((s) => ({
      ...s,
      currentProcessTree: {
        plan_id: 'fix-relay',
        title: 'Fix Relay Transport',
        project: { project_id: 'pm-mcp-clients', display_name: 'pm-mcp-clients', link: '' },
        epic: { epic_id: 'relay-hardening', title: 'Relay Hardening', link: '' },
      } as unknown as ProcessTree,
    }));

    render(WorkListColumn, { projectId: 'pm-mcp-clients', currentPlanId: 'fix-relay' });

    await waitFor(() => {
      expect(screen.getByTestId('up-link')).toBeInTheDocument();
    });

    const upLink = screen.getByTestId('up-link');
    expect(upLink).toHaveTextContent('Relay Hardening');
    expect(upLink).toHaveAttribute('href', '/epics/relay-hardening');
  });
});
