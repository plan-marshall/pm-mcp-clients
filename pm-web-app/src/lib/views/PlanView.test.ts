import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import PlanView from './PlanView.svelte';
import {
  apiClient,
  type PlanCompletionReport,
  type ProcessTree,
  type TaskActivity,
} from '../api/client';
import { workflowIconUrl } from '../icons/workflowIcons';
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

  it('shows the phase icon on the badge and the process-tree phase header', async () => {
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue({
      ...mockPlan,
      current_phase: 'phase-5-execute',
    });

    render(PlanView);

    const badge = await screen.findByTestId('plan-phase');
    expect(badge).toHaveTextContent('phase-5-execute');
    expect(badge.querySelector('.workflow-icon')?.getAttribute('style')).toContain(
      workflowIconUrl('phase.execute'),
    );
    expect(
      screen.getByTestId('node-execute').querySelector('.workflow-icon')?.getAttribute('style'),
    ).toContain(workflowIconUrl('phase.execute'));
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

  it('opens the process designer for the plan’s project', async () => {
    render(PlanView);

    await screen.findByTestId('plan-title');
    await fireEvent.click(screen.getByTestId('design-process-btn'));

    const state = get(runtimeStore);
    expect(state.currentRoute).toBe('designer');
    expect(state.designerProjectId).toBe('pm-mcp-clients');
    expect(window.location.pathname).toBe('/designer');
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

  it('narrates a done phase without fetching task activity', async () => {
    render(PlanView);

    await screen.findByTestId('plan-title');
    await waitFor(() =>
      expect(apiClient.getTaskActivity).toHaveBeenCalledWith('fix-relay', 'TASK-003'),
    );
    vi.mocked(apiClient.getTaskActivity).mockClear();

    await fireEvent.click(screen.getByTestId('select-node-init'));

    const narrative = await screen.findByTestId('node-narrative');
    expect(narrative).toHaveTextContent('init');
    expect(screen.getByTestId('node-narrative-state')).toHaveTextContent('done');
    expect(screen.getByTestId('node-narrative-duration')).toHaveTextContent('2 min');
    expect(screen.getByTestId('task-inspector')).toHaveAttribute('data-selected-kind', 'node');
    expect(apiClient.getTaskActivity).not.toHaveBeenCalled();
  });

  it('renders the activity entries of a done leaf task', async () => {
    const planWithDoneTask: ProcessTree = {
      ...mockPlan,
      nodes: [
        mockPlan.nodes[0],
        {
          ...mockPlan.nodes[1],
          leaf_tasks: [
            {
              task_id: 'TASK-001',
              role: 'implement',
              harness: 'claude',
              model: 'sonnet',
              attempt: 1,
              status: 'done',
              duration_seconds: 240,
              tokens_so_far: 22000,
              activity_link: '/api/v1/plans/fix-relay/tasks/TASK-001/activity',
            },
            mockPlan.nodes[1].leaf_tasks[0],
          ],
        },
        mockPlan.nodes[2],
      ],
    };
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(planWithDoneTask);
    vi.spyOn(apiClient, 'getTaskActivity').mockImplementation(async (_planId, taskId) => ({
      ...mockActivity,
      task_id: taskId,
      entries:
        taskId === 'TASK-001'
          ? [
              {
                timestamp: '13:40:02',
                kind: 'text',
                content: 'The transport now reconnects with backoff.',
              },
            ]
          : mockActivity.entries,
    }));

    render(PlanView);
    await screen.findByTestId('plan-title');
    await fireEvent.click(screen.getByTestId('task-TASK-001'));

    await waitFor(() =>
      expect(screen.getByTestId('activity-feed')).toHaveTextContent(
        'The transport now reconnects with backoff.',
      ),
    );
    expect(screen.getByTestId('activity-feed')).toHaveTextContent('TASK-001');
    expect(screen.getByTestId('activity-feed')).not.toHaveTextContent('TASK-003');
  });

  it('shows the empty feed for a done task whose explicit activity route is empty', async () => {
    const planWithQuietTask: ProcessTree = {
      ...mockPlan,
      nodes: [
        mockPlan.nodes[0],
        {
          ...mockPlan.nodes[1],
          leaf_tasks: [
            {
              task_id: 'TASK-002',
              role: 'module-tester',
              harness: 'claude',
              model: 'sonnet',
              attempt: 1,
              status: 'done',
              duration_seconds: 180,
              tokens_so_far: 15000,
            },
          ],
        },
        mockPlan.nodes[2],
      ],
    };
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(planWithQuietTask);
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      ...mockActivity,
      task_id: 'TASK-002',
      role: 'module-tester',
      entries: [],
    });

    render(PlanView);
    await screen.findByTestId('plan-title');
    await fireEvent.click(screen.getByTestId('task-TASK-002'));

    const feed = await screen.findByTestId('activity-feed');
    expect(feed).toHaveTextContent('TASK-002');
    expect(screen.getByText('No activity entries yet.')).toBeInTheDocument();
    expect(screen.queryAllByTestId('activity-entry')).toHaveLength(0);
    expect(screen.queryByTestId('task-no-activity')).not.toBeInTheDocument();
  });

  it('shows the fallback empty state when a task activity request fails', async () => {
    const planWithQuietTask: ProcessTree = {
      ...mockPlan,
      nodes: [
        mockPlan.nodes[0],
        {
          ...mockPlan.nodes[1],
          leaf_tasks: [
            {
              task_id: 'TASK-002',
              role: 'module-tester',
              harness: 'claude',
              model: 'sonnet',
              attempt: 1,
              status: 'done',
              duration_seconds: 180,
              tokens_so_far: 15000,
            },
          ],
        },
        mockPlan.nodes[2],
      ],
    };
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(planWithQuietTask);
    vi.spyOn(apiClient, 'getTaskActivity').mockImplementation(async (_planId, taskId) => {
      if (taskId === 'TASK-002') {
        throw new Error('no activity route');
      }
      return mockActivity;
    });

    render(PlanView);
    await screen.findByTestId('plan-title');
    await fireEvent.click(screen.getByTestId('task-TASK-002'));

    const empty = await screen.findByTestId('task-no-activity');
    expect(empty).toHaveTextContent('No activity entries yet.');
    expect(screen.queryByTestId('plan-error')).not.toBeInTheDocument();
  });

  it('keeps a nested not_in_plan node out of the tree and the narrative', async () => {
    const prunedTask = {
      task_id: 'TASK-900',
      role: 'implement',
      harness: 'claude',
      model: 'sonnet',
      attempt: 1,
      status: 'done',
      duration_seconds: 60,
      tokens_so_far: 1000,
    };
    const planWithNestedPruned: ProcessTree = {
      ...mockPlan,
      nodes: [
        mockPlan.nodes[0],
        {
          ...mockPlan.nodes[1],
          leaf_tasks: [],
          sub_nodes: [
            {
              node_id: 'execute.pruned',
              title: 'removed step',
              kind: 'sub_workflow',
              state: 'not_in_plan',
              run_count: 0,
              current_run: 0,
              duration_seconds: 0,
              prune_reason: 'not needed',
              sub_nodes: [],
              leaf_tasks: [prunedTask],
            },
          ],
        },
        mockPlan.nodes[2],
      ],
    };
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(planWithNestedPruned);

    render(PlanView);
    await screen.findByTestId('plan-title');

    expect(screen.queryByTestId('select-node-execute.pruned')).not.toBeInTheDocument();
    expect(screen.getByTestId('not-in-plan-box')).toHaveTextContent('removed step');
    expect(screen.getByTestId('not-in-plan-box')).toHaveTextContent('not needed');

    const narrative = await screen.findByTestId('node-narrative');
    expect(narrative).not.toHaveTextContent('removed step');
    expect(screen.queryByTestId('node-child-task-TASK-900')).not.toBeInTheDocument();
    expect(screen.queryByTestId('node-child-node-execute.pruned')).not.toBeInTheDocument();
  });

  it('keeps the selection when the expand toggle collapses a node', async () => {
    render(PlanView);
    await screen.findByTestId('plan-title');
    await screen.findByTestId('activity-feed');

    const toggle = screen.getByTestId('toggle-node-execute');
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    const panelBefore = screen.getByTestId('task-inspector').textContent;

    await fireEvent.click(toggle);

    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getByTestId('task-inspector')).toHaveAttribute('data-selected-kind', 'task');
    expect(screen.getByTestId('task-inspector').textContent).toBe(panelBefore);
    expect(screen.queryByTestId('node-narrative')).not.toBeInTheDocument();
  });

  it('keeps a node selection across a refresh and falls back when it disappears', async () => {
    let onEvent: ((event: string, data?: unknown) => void) | undefined;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((callback) => {
      onEvent = callback;
      return () => {};
    });

    render(PlanView);
    await screen.findByTestId('plan-title');
    await fireEvent.click(screen.getByTestId('select-node-init'));
    expect(await screen.findByTestId('node-narrative-title')).toHaveTextContent('init');

    onEvent?.('plan.changed');
    await waitFor(() =>
      expect(screen.getByTestId('node-narrative-title')).toHaveTextContent('init'),
    );

    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue({
      ...mockPlan,
      nodes: mockPlan.nodes.filter((node) => node.node_id !== 'init'),
    });
    onEvent?.('plan.changed');

    await waitFor(() =>
      expect(screen.getByTestId('task-inspector')).toHaveAttribute('data-selected-kind', 'task'),
    );
    expect(screen.queryByTestId('node-narrative')).not.toBeInTheDocument();
    expect(screen.getByTestId('task-TASK-003')).toHaveAttribute('aria-current', 'true');
  });

  it('selects the first visible node when no task is running', async () => {
    runtimeStore.update((state) => ({ ...state, selectedTaskId: null }));
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue({
      ...mockPlan,
      nodes: [
        mockPlan.nodes[0],
        {
          ...mockPlan.nodes[1],
          state: 'planned',
          leaf_tasks: [],
        },
        mockPlan.nodes[2],
      ],
    });

    render(PlanView);
    await screen.findByTestId('plan-title');

    expect(screen.getByTestId('task-inspector')).toHaveAttribute('data-selected-kind', 'node');
    expect(screen.getByTestId('node-narrative-title')).toHaveTextContent('init');
    expect(apiClient.getTaskActivity).not.toHaveBeenCalled();
  });

  it('keeps the newest activity when an older task load resolves late', async () => {
    const planWithTwoTasks: ProcessTree = {
      ...mockPlan,
      nodes: [
        mockPlan.nodes[0],
        {
          ...mockPlan.nodes[1],
          leaf_tasks: [
            {
              task_id: 'TASK-001',
              role: 'implement',
              harness: 'claude',
              model: 'sonnet',
              attempt: 1,
              status: 'done',
              duration_seconds: 240,
              tokens_so_far: 22000,
            },
            mockPlan.nodes[1].leaf_tasks[0],
          ],
        },
        mockPlan.nodes[2],
      ],
    };
    const pending = new Map<string, (activity: TaskActivity) => void>();
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(planWithTwoTasks);
    vi.spyOn(apiClient, 'getTaskActivity').mockImplementation(
      (_planId, taskId) =>
        new Promise<TaskActivity>((resolve) => {
          pending.set(taskId, resolve);
        }),
    );

    render(PlanView);
    await screen.findByTestId('plan-title');
    await waitFor(() => expect(pending.has('TASK-003')).toBe(true));

    await fireEvent.click(screen.getByTestId('task-TASK-001'));
    await waitFor(() => expect(pending.has('TASK-001')).toBe(true));

    pending.get('TASK-001')?.({
      ...mockActivity,
      task_id: 'TASK-001',
      entries: [{ timestamp: '13:40:02', kind: 'text', content: 'Newer selection' }],
    });
    await waitFor(() =>
      expect(screen.getByTestId('activity-feed')).toHaveTextContent('Newer selection'),
    );

    pending.get('TASK-003')?.({
      ...mockActivity,
      task_id: 'TASK-003',
      entries: [{ timestamp: '14:00:00', kind: 'text', content: 'Stale selection' }],
    });
    await Promise.resolve();

    expect(screen.getByTestId('activity-feed')).toHaveTextContent('Newer selection');
    expect(screen.getByTestId('activity-feed')).not.toHaveTextContent('Stale selection');
  });

  it('renders and selects nested sub-workflows at every depth', async () => {
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue({
      ...mockPlan,
      nodes: [
        mockPlan.nodes[0],
        {
          ...mockPlan.nodes[1],
          leaf_tasks: [],
          sub_nodes: [
            {
              node_id: 'execute.envelope',
              title: 'execute.envelope',
              kind: 'tasks',
              state: 'running',
              run_count: 1,
              current_run: 1,
              duration_seconds: 360,
              sub_nodes: [
                {
                  node_id: 'execute.envelope.remediation',
                  title: 'remediation.ci-failure',
                  kind: 'sub_workflow',
                  state: 'planned',
                  run_count: 0,
                  current_run: 0,
                  duration_seconds: 0,
                  sub_nodes: [],
                  leaf_tasks: [],
                },
              ],
              leaf_tasks: [],
            },
          ],
        },
        mockPlan.nodes[2],
      ],
    });

    render(PlanView);
    await screen.findByTestId('plan-title');

    const nested = await screen.findByTestId('node-execute.envelope.remediation');
    expect(nested).toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('select-node-execute.envelope.remediation'));

    expect(screen.getByTestId('node-narrative-title')).toHaveTextContent('remediation.ci-failure');
    expect(screen.getByTestId('node-narrative-state')).toHaveTextContent('planned');
  });

  it('makes the row body a separately announced selection control', async () => {
    render(PlanView);
    await screen.findByTestId('plan-title');

    const select = screen.getByTestId('select-node-init');
    const toggle = screen.getByTestId('toggle-node-init');

    expect(select.tagName).toBe('BUTTON');
    expect(select).toHaveAccessibleName(/init/);
    expect(select.querySelector('button')).toBeNull();
    expect(toggle).toHaveAccessibleName('Toggle node');

    await fireEvent.click(select);

    expect(select).toHaveAttribute('aria-current', 'true');
    expect(select).toHaveAttribute('aria-pressed', 'true');
    expect(select.closest('.node-row')).toHaveClass('node-row--selected');
  });
});
