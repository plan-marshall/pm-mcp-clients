import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import PlanView from './PlanView.svelte';
import { apiClient, type ProcessTree, type TaskActivity } from '../api/client';
import { runtimeStore } from '../stores/runtime';
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
    runtimeStore.set({
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
      isDraining: false,
      isStopped: false,
      activeStep: 1,
      unreadReportsCount: 0,
      openDecisionsCount: 0,
      selectedPlanId: 'fix-relay',
      selectedEpicId: 'relay-hardening',
      selectedTaskId: 'TASK-003',
    });

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

  it('navigates back to overview on back button and to epic on breadcrumb click', async () => {
    render(PlanView);

    await screen.findByTestId('plan-title');

    // Click epic link in breadcrumb
    const epicBtn = screen.getByTestId('breadcrumb-epic');
    await fireEvent.click(epicBtn);

    const stateAfterEpic = get(runtimeStore);
    expect(stateAfterEpic.currentRoute).toBe('epic');
    expect(stateAfterEpic.selectedEpicId).toBe('relay-hardening');

    // Click back button
    const backBtn = screen.getByTestId('back-btn');
    await fireEvent.click(backBtn);

    const stateAfterBack = get(runtimeStore);
    expect(stateAfterBack.currentRoute).toBe('overview');
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
