import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import PlanView from './PlanView.svelte';
import {
  apiClient,
  type PlanCompletionReport,
  type ProcessTree,
  type TaskActivity,
} from '../api/client';
import { runtimeStore, navigateTo, openPlanView } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { get } from 'svelte/store';

describe('PlanView component', () => {
  const mockPlan: ProcessTree = {
    plan_id: 'fix-relay',
    title: 'fix-relay',
    project: { project_id: 'pm-mcp-clients', display_name: 'pm-mcp-clients' },
    epic: { epic_id: 'relay-hardening', title: 'relay-hardening' },
    current_phase: 'execute',
    phase_state: 'running',
    instance_version: 1,
    needs_operator: false,
    pull_request_number: 214,
    pull_request_url: 'https://github.com/plan-marshall/pm-mcp-clients/pull/214',
    duration_seconds: 7800,
    tokens_so_far: {
      total: 412000,
      input: 380000,
      output: 32000,
      floor: false,
      tasks_total: 10,
      tasks_measured: 10,
      none_yet: false,
      not_measured: false,
    },
    current_position_path: ['execute', 'execute.envelope', 'TASK-003'],
    nodes: [
      {
        node_id: 'init',
        title: 'init',
        kind: 'phase',
        state: 'done',
        run_count: 1,
        current_run: 1,
        duration_seconds: 120,
        sub_nodes: [],
        leaf_tasks: [],
      },
      {
        node_id: 'execute',
        title: 'execute',
        kind: 'phase',
        state: 'running',
        run_count: 1,
        current_run: 1,
        duration_seconds: 1500,
        sub_nodes: [],
        leaf_tasks: [
          {
            task_id: 'TASK-003',
            role: 'implement',
            harness: 'claude',
            model: 'sonnet',
            attempt: 1,
            status: 'running',
            duration_seconds: 360,
            tokens_so_far: 48000,
            off_track: {
              signal: 'repeating',
              action: 'shown',
              value: 5,
              threshold: 5,
            },
          },
        ],
      },
      {
        node_id: 'finalize',
        title: 'finalize',
        kind: 'phase',
        state: 'planned',
        run_count: 0,
        current_run: 0,
        duration_seconds: 0,
        sub_nodes: [],
        leaf_tasks: [],
      },
    ],
  };

  const mockActivity: TaskActivity = {
    task_id: 'TASK-003',
    role: 'implement',
    harness: 'claude',
    model: 'sonnet',
    attempt: 1,
    duration_seconds: 360,
    tokens_so_far: 48000,
    off_track: {
      signal: 'repeating',
      action: 'shown',
      value: 5,
      threshold: 5,
    },
    entries: [
      {
        timestamp: '14:02:11',
        kind: 'text',
        content: 'Reading the relay transport before changing it.',
      },
      {
        timestamp: '14:02:14',
        kind: 'tool',
        tool_name: 'read',
        tool_target: 'pm-relay/src/main/java/Transport.java',
      },
      {
        timestamp: '14:02:20',
        kind: 'tool',
        tool_name: 'bash',
        tool_target: './mvnw test',
        tool_failed: true,
        size: '4 KiB',
      },
      {
        timestamp: '14:03:02',
        kind: 'changed',
        content: 'pm-relay/src/main/java/Transport.java',
      },
    ],
  };

  beforeEach(() => {
    vi.restoreAllMocks();
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: true,
        deviceLabel: 'test',
        machineName: 'test-mac',
        currentRoute: 'plan',
        currentState: null,
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 0,
        isDraining: false,
        isStopped: false,
        activeStep: 1,
        unreadReportsCount: 0,
        openDecisionsCount: 0,
        selectedPlanId: 'fix-relay',
        selectedEpicId: 'relay-hardening',
        selectedTaskId: 'TASK-003',
      }),
    );

    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(mockPlan);
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue(mockActivity);
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'subscribeTaskActivity').mockReturnValue(() => {});
  });

  it('renders plan header, breadcrumb path, tokens, and PR badge', async () => {
    render(PlanView);

    // Wait for plan process to load
    expect(await screen.findByTestId('plan-title')).toHaveTextContent('fix-relay');
    expect(screen.getByTestId('breadcrumb-project')).toHaveTextContent('pm-mcp-clients');
    expect(screen.getByTestId('breadcrumb-epic')).toHaveTextContent('relay-hardening');
    expect(screen.getByTestId('breadcrumb-plan')).toHaveTextContent('fix-relay');

    expect(screen.getByTestId('plan-phase')).toHaveTextContent('execute');
    expect(screen.getByTestId('plan-state')).toHaveTextContent('running');
    expect(screen.getByTestId('pr-link')).toHaveTextContent('#214');

    expect(screen.getByTestId('plan-tokens')).toHaveTextContent(/412 k/);
  });

  it('renders process tree nodes with glyphs, state, and task rows', async () => {
    render(PlanView);

    expect(await screen.findByTestId('node-init')).toBeInTheDocument();
    expect(screen.getByTestId('node-execute')).toBeInTheDocument();
    expect(screen.getByTestId('node-finalize')).toBeInTheDocument();

    const taskRow = await screen.findByTestId('task-TASK-003');
    expect(taskRow).toBeInTheDocument();
    expect(taskRow).toHaveTextContent('TASK-003');
    expect(taskRow).toHaveTextContent('implement');
    expect(taskRow).toHaveTextContent('claude sonnet');
  });

  it('renders worker task activity inspector and off-track alert banner', async () => {
    render(PlanView);

    expect(await screen.findByTestId('task-inspector')).toBeInTheDocument();
    expect(screen.getByTestId('off-track-alert')).toHaveTextContent(/off track: repeating/);

    const entries = screen.getAllByTestId('activity-entry');
    expect(entries.length).toBe(4);
    expect(entries[0]).toHaveTextContent('Reading the relay transport');
    expect(entries[1]).toHaveTextContent('read');
    expect(entries[2]).toHaveTextContent('failed · 4 KiB');
    expect(entries[3]).toHaveTextContent('Transport.java');
  });

  it('normalizes live activity events and ignores malformed ones', async () => {
    let onEvent: ((event: string, data: unknown) => void) | undefined;
    vi.spyOn(apiClient, 'subscribeTaskActivity').mockImplementation(
      (_planId, _taskId, callback) => {
        onEvent = callback;
        return () => {};
      },
    );

    render(PlanView);
    await screen.findByTestId('task-inspector');
    await waitFor(() => expect(onEvent).toBeDefined());

    onEvent?.('task.activity', {
      kind: 'text',
      text: 'Live output',
      timestamp: '14:04:00',
      content: { unexpected: true },
      files: ['Transport.java', 7],
    });
    onEvent?.('task.activity', { kind: 42, timestamp: '14:04:01' });
    onEvent?.('other.event', { kind: 'text', text: 'not activity' });

    await waitFor(() => expect(screen.getAllByTestId('activity-entry')).toHaveLength(5));
    expect(screen.getAllByTestId('activity-entry')[4]).toHaveTextContent('Live output');
  });

  it('renders pruned nodes in not-in-plan section for land lane plans', async () => {
    const prunedPlan: ProcessTree = {
      ...mockPlan,
      plan_id: 'docs-typo',
      title: 'docs-typo',
      nodes: [
        {
          node_id: 'init',
          title: 'init',
          kind: 'phase',
          state: 'done',
          run_count: 1,
          current_run: 1,
          duration_seconds: 60,
          sub_nodes: [],
          leaf_tasks: [],
        },
        {
          node_id: 'refine',
          title: 'refine',
          kind: 'phase',
          state: 'not_in_plan',
          prune_reason: 'the land lane',
          run_count: 0,
          current_run: 0,
          duration_seconds: 0,
          sub_nodes: [],
          leaf_tasks: [],
        },
        {
          node_id: 'outline',
          title: 'outline',
          kind: 'phase',
          state: 'not_in_plan',
          prune_reason: 'the land lane',
          run_count: 0,
          current_run: 0,
          duration_seconds: 0,
          sub_nodes: [],
          leaf_tasks: [],
        },
      ],
    };

    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(prunedPlan);
    render(PlanView);

    const prunedBox = await screen.findByTestId('not-in-plan-box');
    expect(prunedBox).toHaveTextContent('not in this plan: refine, outline (the land lane)');
  });

  it('returns to the caller view from the back button', async () => {
    navigateTo('overview');
    openPlanView('fix-relay');

    render(PlanView);

    await screen.findByTestId('plan-title');
    await fireEvent.click(screen.getByTestId('back-btn'));

    expect(get(runtimeStore).currentRoute).toBe('overview');
  });

  it('opens the parent epic from the breadcrumb in one step', async () => {
    render(PlanView);

    await screen.findByTestId('plan-title');
    await fireEvent.click(screen.getByTestId('breadcrumb-epic'));

    const state = get(runtimeStore);
    expect(state.currentRoute).toBe('epic');
    expect(state.selectedEpicId).toBe('relay-hardening');
  });

  it('opens the project in the explorer in one step', async () => {
    render(PlanView);

    await screen.findByTestId('plan-title');
    await fireEvent.click(screen.getByTestId('breadcrumb-project'));

    const state = get(runtimeStore);
    expect(state.currentRoute).toBe('projects');
    expect(state.selectedProjectId).toBe('pm-mcp-clients');
    expect(window.location.pathname).toBe('/projects');
    expect(window.location.search).toBe('?project=pm-mcp-clients');
  });

  it('discloses the token breakdown by phase and role with raw components', async () => {
    const planWithBreakdown: ProcessTree = {
      ...mockPlan,
      tokens_so_far: {
        ...mockPlan.tokens_so_far,
        floor: true,
        tasks_total: 17,
        tasks_measured: 15,
        by_phase: [
          {
            name: 'execute',
            total: 380000,
            input: 350000,
            output: 30000,
            unmeasured: 2,
            floor: true,
            components: {
              input: 350000,
              cache_read: 20000,
              cache_write: 5000,
              output: 30000,
              reasoning: 2000,
            },
          },
        ],
        by_role: [
          {
            name: 'implement',
            total: 380000,
            input: 350000,
            output: 30000,
            unmeasured: 2,
            floor: true,
          },
        ],
      },
    };
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(planWithBreakdown);

    render(PlanView);
    await screen.findByTestId('plan-title');

    // The line carries the floor and the catalogue-formatted coverage.
    expect(screen.getByTestId('plan-tokens')).toHaveTextContent(/≥ 380 k/);
    expect(screen.getByTestId('plan-tokens')).toHaveTextContent('2 of 17 tasks not measured');

    const toggle = screen.getByTestId('tokens-toggle');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await fireEvent.click(toggle);

    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    const breakdown = screen.getByTestId('token-breakdown');
    expect(breakdown).toHaveTextContent('By phase');
    expect(breakdown).toHaveTextContent('By role');

    const phaseRow = screen.getByTestId('token-phase-execute');
    expect(phaseRow).toHaveTextContent('350 k');
    expect(phaseRow).toHaveTextContent('25 k');
    expect(phaseRow).toHaveTextContent('30 k');
    expect(phaseRow).toHaveTextContent('380 k');

    const roleRow = screen.getByTestId('token-role-implement');
    expect(roleRow).toHaveTextContent('350 k');
    expect(roleRow).toHaveTextContent('380 k');
    // The row reports no components, so the cached cell says so rather than showing zero.
    expect(roleRow).toHaveTextContent('—');
  });

  it('reads the token breakdown of an ended plan from its completion report', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue({
      plan_id: 'fix-relay',
      token_usage: {
        by_phase: [
          {
            name: 'execute',
            total: 380000,
            input: 350000,
            output: 30000,
            unmeasured: 0,
            floor: false,
            components: {
              input: 350000,
              cache_read: 20000,
              cache_write: 5000,
              output: 30000,
              reasoning: 2000,
            },
          },
        ],
        by_role: [
          {
            name: 'implement',
            total: 380000,
            input: 350000,
            output: 30000,
            unmeasured: 0,
            floor: false,
          },
        ],
      },
    } as unknown as PlanCompletionReport);
    runtimeStore.update((s) => ({
      ...s,
      selectedPlanEnded: { plan_id: 'fix-relay', outcome: 'merged' },
    }));

    render(PlanView);
    await screen.findByTestId('plan-title');

    await fireEvent.click(screen.getByTestId('tokens-toggle'));

    const phaseRow = await screen.findByTestId('token-phase-execute');
    expect(phaseRow).toHaveTextContent('25 k');
    expect(screen.getByTestId('token-role-implement')).toHaveTextContent('380 k');
    expect(apiClient.getPlanCompletion).toHaveBeenCalledWith('fix-relay');
  });

  it('shows an ended plan as it ended and opens its completion report in one step', async () => {
    runtimeStore.update((s) => ({
      ...s,
      selectedPlanEnded: {
        plan_id: 'fix-relay',
        outcome: 'merged',
        reason: 'the fallback held',
        ended_at: '2026-10-16T11:20:00Z',
      },
    }));

    render(PlanView);
    await screen.findByTestId('plan-title');

    const banner = screen.getByTestId('plan-ended');
    const endedAt = new Date('2026-10-16T11:20:00Z').toLocaleString('en');
    expect(banner).toHaveTextContent('The plan has ended: merged');
    expect(banner).toHaveTextContent(`ended ${endedAt}`);
    expect(banner).toHaveTextContent('Reason: the fallback held');

    await fireEvent.click(screen.getByTestId('ended-completion-report'));

    const state = get(runtimeStore);
    expect(state.currentRoute).toBe('reports');
    expect(state.selectedReportDetailPlanId).toBe('fix-relay');
    expect(window.location.search).toBe('?plan=fix-relay');
  });

  it('selects running task over earlier completed task on initial load', async () => {
    runtimeStore.update((s) => ({ ...s, selectedTaskId: null }));

    const planWithMultipleTasks: ProcessTree = {
      ...mockPlan,
      nodes: [
        {
          node_id: 'phase-1',
          title: 'phase-1',
          kind: 'phase',
          state: 'done',
          run_count: 1,
          current_run: 1,
          duration_seconds: 60,
          sub_nodes: [],
          leaf_tasks: [
            {
              task_id: 'TASK-001',
              role: 'setup',
              harness: 'claude',
              model: 'sonnet',
              attempt: 1,
              status: 'done',
              duration_seconds: 60,
              tokens_so_far: 1000,
            },
          ],
        },
        {
          node_id: 'phase-2',
          title: 'phase-2',
          kind: 'phase',
          state: 'running',
          run_count: 1,
          current_run: 1,
          duration_seconds: 120,
          sub_nodes: [],
          leaf_tasks: [
            {
              task_id: 'TASK-002',
              role: 'implement',
              harness: 'claude',
              model: 'sonnet',
              attempt: 1,
              status: 'running',
              duration_seconds: 120,
              tokens_so_far: 5000,
            },
          ],
        },
      ],
    };

    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(planWithMultipleTasks);
    render(PlanView);

    await screen.findByTestId('plan-title');
    expect(apiClient.getTaskActivity).toHaveBeenCalledWith('fix-relay', 'TASK-002');
  });
});
