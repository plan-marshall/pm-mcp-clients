import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import { get } from 'svelte/store';
import { tick } from 'svelte';
import NodeNarrative from './NodeNarrative.svelte';
import type { ProcessNode, ProcessTree } from '../api/client';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';

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
    prune_reason: 'lane_choice',
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

  it('names the node without the kind word, like the tree row', () => {
    render(NodeNarrative, {
      props: {
        node: { ...execute, title: 'phase execute' },
        plan,
        onSelectTask: vi.fn(),
        onSelectNode: vi.fn(),
      },
    });

    expect(screen.getByTestId('node-narrative-title').textContent?.trim()).toBe('execute');
  });

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
    expect(screen.getByTestId('node-narrative-prune')).not.toHaveTextContent('lane_choice');
    expect(screen.getByTestId('node-narrative')).toHaveTextContent('2 runs');
  });

  it('draws the kind as an icon named by the kind word, as the tree does', () => {
    render(NodeNarrative, {
      props: { node: execute, plan, onSelectTask: vi.fn(), onSelectNode: vi.fn() },
    });

    const kind = screen.getByTestId('node-narrative-kind');
    expect(kind.querySelector('[data-workflow-icon="phase.execute"]')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Phase' })).toBeInTheDocument();
    expect(kind.textContent?.trim()).toBe('');
  });

  it('names a prune reason outside the closed list with the unknown word, never raw', () => {
    render(NodeNarrative, {
      props: {
        node: { ...donePhase, prune_reason: 'not_a_reason' },
        plan,
        onSelectTask: vi.fn(),
        onSelectNode: vi.fn(),
      },
    });

    expect(screen.getByTestId('node-narrative-prune')).toHaveTextContent('unknown');
    expect(screen.getByTestId('node-narrative-prune')).not.toHaveTextContent('not_a_reason');
  });

  it('names a task status through the catalogue, and an unknown one with the unknown word', () => {
    render(NodeNarrative, {
      props: {
        node: {
          ...execute,
          leaf_tasks: [
            { ...execute.leaf_tasks[0], status: 'running' },
            { ...execute.leaf_tasks[1], status: 'hyperdrive' },
          ],
        },
        plan,
        onSelectTask: vi.fn(),
        onSelectNode: vi.fn(),
      },
    });

    const statusOf = (taskId: string): string | undefined =>
      screen
        .getByTestId(`node-child-task-${taskId}`)
        .querySelector('.child-task__status')
        ?.textContent?.trim();
    expect(statusOf('TASK-001')).toBe('running');
    expect(statusOf('TASK-002')).toBe('unknown');
    expect(screen.getByTestId('node-child-task-TASK-002')).not.toHaveTextContent('hyperdrive');
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
    expect(first.querySelector('.child-task__status')?.textContent?.trim()).toBe('done');
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

  describe('the link to the plan’s intent', () => {
    beforeEach(() => {
      runtimeStore.set(makeRuntimeState({ currentRoute: 'plan', selectedPlanId: 'session-log' }));
      window.history.replaceState({}, '', '/plans/session-log?node=refine');
    });

    it.each(['refine', 'outline'])('leads from %s to the Intent tab without node=', (nodeId) => {
      render(NodeNarrative, {
        props: {
          node: { ...donePhase, node_id: nodeId, title: nodeId },
          plan,
          onSelectTask: vi.fn(),
          onSelectNode: vi.fn(),
        },
      });

      const link = screen.getByTestId('node-intent-link');
      expect(link).toHaveTextContent('Intent');
      const href = new URL(link.getAttribute('href') ?? '', 'http://app.invalid');
      expect(href.pathname).toBe('/plans/session-log');
      expect(href.searchParams.get('tab')).toBe('intent');
      expect(href.searchParams.has('node')).toBe(false);
    });

    it('drops a key from its href once the address drops it', async () => {
      runtimeStore.update((s) => ({ ...s, openObject: { kind: 'decision', id: 'Q-031' } }));
      render(NodeNarrative, {
        props: {
          node: { ...donePhase, node_id: 'refine', title: 'refine' },
          plan,
          onSelectTask: vi.fn(),
          onSelectNode: vi.fn(),
        },
      });
      const hrefOf = () =>
        new URL(
          screen.getByTestId('node-intent-link').getAttribute('href') ?? '',
          'http://app.invalid',
        );
      expect(hrefOf().searchParams.get('open')).toBe('decision:Q-031');

      runtimeStore.update((s) => ({ ...s, openObject: null }));
      await tick();

      expect(hrefOf().searchParams.has('open')).toBe(false);
      expect(hrefOf().searchParams.get('tab')).toBe('intent');
    });

    it.each(['init', 'plan', 'execute', 'finalize', 'execute.session-log'])(
      'is absent from %s',
      (nodeId) => {
        render(NodeNarrative, {
          props: {
            node: { ...donePhase, node_id: nodeId, title: nodeId },
            plan,
            onSelectTask: vi.fn(),
            onSelectNode: vi.fn(),
          },
        });

        expect(screen.queryByTestId('node-intent-link')).not.toBeInTheDocument();
      },
    );

    it('selects the Intent tab and replaces the address with tab=intent, without node=', async () => {
      const push = vi.spyOn(window.history, 'pushState');
      render(NodeNarrative, {
        props: {
          node: { ...donePhase, node_id: 'refine', title: 'refine' },
          plan,
          onSelectTask: vi.fn(),
          onSelectNode: vi.fn(),
        },
      });

      await fireEvent.click(screen.getByTestId('node-intent-link'));

      expect(get(runtimeStore).objectTab).toBe('intent');
      const address = new URLSearchParams(window.location.search);
      expect(address.get('tab')).toBe('intent');
      expect(address.has('node')).toBe(false);
      expect(push).not.toHaveBeenCalled();
      push.mockRestore();
    });
  });

  it('strips control sequences from every free-text field it shows', () => {
    const unsafe: ProcessNode = {
      ...donePhase,
      title: '\u001b[31minit\u001b[0m',
      prune_reason: '\u001b[33mlane_choice\u001b[0m',
      waits_for: '\u001b[36mQ-031\u001b[0m',
    };

    render(NodeNarrative, {
      props: { node: unsafe, plan, onSelectTask: vi.fn(), onSelectNode: vi.fn() },
    });

    expect(screen.getByTestId('node-narrative-title').textContent).toBe('init');
    // A reason garbled by control sequences is no reason of the closed list: it reads as unknown.
    expect(screen.getByTestId('node-narrative-prune').textContent).toContain('unknown');
    expect(screen.getByTestId('node-narrative-prune').textContent).not.toContain('\u001b');
    expect(screen.getByTestId('node-narrative-waits').textContent).toContain('Q-031');
    expect(screen.getByTestId('node-narrative-waits').textContent).not.toContain('\u001b');
  });
});
