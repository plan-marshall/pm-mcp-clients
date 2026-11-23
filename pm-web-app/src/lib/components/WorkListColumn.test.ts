import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import WorkListColumn from './WorkListColumn.svelte';
import { apiClient, type ProjectDetailResponse, type ProcessTree } from '../api/client';
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
