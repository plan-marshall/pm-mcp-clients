import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import NodeNarrative from './NodeNarrative.svelte';
import type { ProcessNode, ProcessTree } from '../api/client';

describe('NodeNarrative component', () => {
  const plan: ProcessTree = {
    plan_id: 'session-log',
    title: 'Session log and generations',
    project: { project_id: 'pm-mcp-core', display_name: 'pm-mcp-core' },
    current_phase: 'execute',
    phase_state: 'running',
    needs_operator: false,
    instance_version: 1,
    tokens_so_far: {
      total: 76000,
      input: 68000,
      output: 8000,
      floor: false,
      tasks_total: 3,
      tasks_measured: 3,
      none_yet: false,
      not_measured: false,
    },
    current_position_path: ['execute', 'execute.session-log', 'TASK-003'],
    nodes: [],
    duration_seconds: 5400,
  };

  const donePhase: ProcessNode = {
    node_id: 'init',
    title: 'init',
    kind: 'phase',
    state: 'done',
    run_count: 1,
    current_run: 1,
    duration_seconds: 90,
    sub_nodes: [],
    leaf_tasks: [],
  };

  const execute: ProcessNode = {
    node_id: 'execute',
    title: 'execute',
    kind: 'phase',
    state: 'running',
    run_count: 1,
    current_run: 1,
    duration_seconds: 1800,
    waits_for: 'Q-031',
    prune_reason: 'the land lane',
    sub_nodes: [
      {
        node_id: 'execute.session-log',
        title: 'execute.session-log',
        kind: 'tasks',
        state: 'running',
        run_count: 1,
        current_run: 1,
        duration_seconds: 900,
        sub_nodes: [],
        leaf_tasks: [],
      },
    ],
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
        activity_link: '/api/v1/plans/session-log/tasks/TASK-001/activity',
      },
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
  };

  it('names the node, its state, run count, duration, waits-for, and prune reason', () => {
    render(NodeNarrative, {
      props: {
        node: { ...execute, run_count: 2 },
        plan,
        onSelectTask: vi.fn(),
        onSelectNode: vi.fn(),
      },
    });

    expect(screen.getByTestId('node-narrative-title')).toHaveTextContent('execute');
    expect(screen.getByTestId('node-narrative-state')).toHaveTextContent('running');
    expect(screen.getByTestId('node-narrative-duration')).toHaveTextContent('30 min');
    expect(screen.getByTestId('node-narrative-waits')).toHaveTextContent('Q-031');
    expect(screen.getByTestId('node-narrative-prune')).toHaveTextContent('the land lane');
    expect(screen.getByTestId('node-narrative')).toHaveTextContent('2 runs');
  });

  it('says one run in the singular', () => {
    render(NodeNarrative, {
      props: { node: donePhase, plan, onSelectTask: vi.fn(), onSelectNode: vi.fn() },
    });

    expect(screen.getByTestId('node-narrative-runs')).toHaveTextContent('1 run');
  });

  it('lists the tasks the node carries, their activity, and selects one on click', async () => {
    const onSelectTask = vi.fn();
    render(NodeNarrative, {
      props: { node: execute, plan, onSelectTask, onSelectNode: vi.fn() },
    });

    const first = screen.getByTestId('node-child-task-TASK-001');
    const second = screen.getByTestId('node-child-task-TASK-002');
    expect(first).toHaveTextContent('TASK-001');
    expect(first).toHaveTextContent('implement');
    expect(first).toHaveTextContent('claude sonnet');
    expect(first).toHaveTextContent('attempt 1');
    expect(first).toHaveTextContent('done');
    expect(first).toHaveTextContent('4 min');
    expect(first).toHaveTextContent('22 k');
    expect(screen.getByTestId('node-child-task-activity-TASK-001')).toHaveTextContent(
      'Activity recorded',
    );
    expect(screen.getByTestId('node-child-task-activity-TASK-002')).toHaveTextContent(
      'No activity recorded',
    );

    await fireEvent.click(second);
    expect(onSelectTask).toHaveBeenCalledWith('TASK-002');
  });

  it('selects a sub-node from the compact outline', async () => {
    const onSelectNode = vi.fn();
    render(NodeNarrative, {
      props: { node: execute, plan, onSelectTask: vi.fn(), onSelectNode },
    });

    const subNode = screen.getByTestId('node-child-node-execute.session-log');
    expect(subNode).toHaveTextContent('execute.session-log');
    expect(subNode).toHaveTextContent('running');

    await fireEvent.click(subNode);
    expect(onSelectNode).toHaveBeenCalledWith('execute.session-log');
  });

  it('skips a sub-node the process omitted from the tree', () => {
    const prunedChild: ProcessNode = {
      node_id: 'execute.pruned',
      title: 'removed step',
      kind: 'sub_workflow',
      state: 'not_in_plan',
      run_count: 0,
      current_run: 0,
      duration_seconds: 0,
      prune_reason: 'not needed',
      sub_nodes: [],
      leaf_tasks: [
        {
          task_id: 'TASK-900',
          role: 'implement',
          harness: 'claude',
          model: 'sonnet',
          attempt: 1,
          status: 'done',
          duration_seconds: 60,
          tokens_so_far: 1000,
        },
      ],
    };
    render(NodeNarrative, {
      props: {
        node: { ...execute, sub_nodes: [prunedChild] },
        plan,
        onSelectTask: vi.fn(),
        onSelectNode: vi.fn(),
      },
    });

    expect(screen.queryByTestId('node-child-node-execute.pruned')).not.toBeInTheDocument();
    expect(screen.queryByTestId('node-child-task-TASK-900')).not.toBeInTheDocument();
    expect(screen.getByTestId('node-narrative')).not.toHaveTextContent('removed step');
    expect(screen.getByTestId('node-child-task-TASK-001')).toBeInTheDocument();
  });

  it('says a done node without tasks completed without task-level activity', () => {
    render(NodeNarrative, {
      props: { node: donePhase, plan, onSelectTask: vi.fn(), onSelectNode: vi.fn() },
    });

    expect(screen.getByTestId('node-no-activity')).toHaveTextContent(
      'This part has no task-level activity. Its state and duration are shown above.',
    );
    expect(screen.queryByTestId('node-child-tasks')).not.toBeInTheDocument();
  });

  it('strips control sequences from every free-text field it shows', () => {
    const unsafe: ProcessNode = {
      ...donePhase,
      title: '\u001b[31minit\u001b[0m',
      prune_reason: '\u001b[33mthe land lane\u001b[0m',
      waits_for: '\u001b[36mQ-031\u001b[0m',
    };

    render(NodeNarrative, {
      props: { node: unsafe, plan, onSelectTask: vi.fn(), onSelectNode: vi.fn() },
    });

    expect(screen.getByTestId('node-narrative-title').textContent).toBe('init');
    expect(screen.getByTestId('node-narrative-prune').textContent).toContain('the land lane');
    expect(screen.getByTestId('node-narrative-prune').textContent).not.toContain('\u001b');
    expect(screen.getByTestId('node-narrative-waits').textContent).toContain('Q-031');
    expect(screen.getByTestId('node-narrative-waits').textContent).not.toContain('\u001b');
  });
});
