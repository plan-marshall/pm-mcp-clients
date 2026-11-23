import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import WorkListColumn from './WorkListColumn.svelte';
import { apiClient, type ProjectDetailResponse } from '../api/client';
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

  it('renders plans and epics with running work first, KindMark, status, and progress pill (D4)', async () => {
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue(mockDetail);

    render(WorkListColumn, { projectId: 'pm-mcp-clients', currentPlanId: 'fix-relay' });

    await waitFor(() => {
      expect(screen.getByTestId('work-list-items')).toBeInTheDocument();
    });

    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(4);

    // Running work should come first: running plan and running epic
    expect(items[0]).toHaveTextContent('Fix Relay Transport');
    expect(items[1]).toHaveTextContent('Relay Hardening');
    // Then remainder: staged plan and staged epic
    expect(items[2]).toHaveTextContent('Staged Plan');
    expect(items[3]).toHaveTextContent('Staged Epic');

    // Fix Relay Transport has progress pill
    const progressPills = screen.getAllByTestId('plan-progress');
    expect(progressPills[0]).toHaveTextContent('2/3');
    expect(progressPills[1]).toHaveTextContent('0/2');

    // Selected row has aria-current="page"
    const currentLink = screen.getByRole('link', { name: /Fix Relay Transport/i });
    expect(currentLink).toHaveAttribute('aria-current', 'page');

    const otherLink = screen.getByRole('link', { name: /Staged Plan/i });
    expect(otherLink).not.toHaveAttribute('aria-current');
  });

  it('replaces the address when a sibling plan is clicked (D4)', async () => {
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue(mockDetail);
    const openPlanSpy = vi.spyOn(runtime, 'openPlanView').mockImplementation(() => {});
    const writeAddressSpy = vi.spyOn(history, 'writeAddress').mockImplementation(() => {});

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
    const writeAddressSpy = vi.spyOn(history, 'writeAddress').mockImplementation(() => {});

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
    const errorSpy = vi.spyOn(apiClient, 'getProjectDetail').mockRejectedValue(new Error('Network error'));

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
});
