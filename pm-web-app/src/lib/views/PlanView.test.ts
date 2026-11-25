import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/svelte';
import PlanView from './PlanView.svelte';
import {
  apiClient,
  type DecisionEnvelope,
  type OutlineRecord,
  type PlanChangesAnswer,
  type PlanChangeFile,
  type PlanCompletionReport,
  type PlanIntentRecord,
  type ProcessTree,
  type TaskActivity,
} from '../api/client';
import { reportBox } from '../test/resizeObserver';
import { workflowIconUrl } from '../icons/workflowIcons';
import { formatTime } from '../display/format';
import { runtimeStore, openPlanView } from '../stores/runtime';
import { PLAN_PROCESS_REFRESH_EVENTS } from '../stores/refreshEvents';
import { makeRuntimeState } from '../test/runtimeState';
import { get } from 'svelte/store';
import { tick } from 'svelte';

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
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      display_name: 'pm-mcp-clients',
      plans: [
        {
          plan_id: 'fix-relay',
          title: 'Fix Relay Transport',
          current_phase: 'execute',
          phase_state: 'running',
          tasks_done: 2,
          tasks_total: 3,
        },
      ],
      epics: [],
      alongside_landings: [],
    });
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue(mockActivity);
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'subscribeTaskActivity').mockReturnValue(() => {});
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders the plan head on the object page with tokens', async () => {
    render(PlanView);

    // Wait for plan process to load
    expect(await screen.findByTestId('plan-title')).toHaveTextContent('fix-relay');
    expect(screen.getByTestId('plan-kind').querySelector('[data-kind="plan"]')).toBeInTheDocument();
    expect(screen.getByTestId('plan-id-chip')).toHaveTextContent('fix-relay');

    expect(screen.getByTestId('plan-phase')).toHaveTextContent('execute');
    expect(screen.getByTestId('plan-state')).toHaveTextContent('running');
    expect(screen.getByTestId('plan-pr-chip')).toHaveTextContent('PR #214');

    expect(screen.getByTestId('plan-tokens')).toHaveTextContent(/412 k/);
  });

  it('shows the phase icon on the phase label and the process-tree phase header', async () => {
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue({
      ...mockPlan,
      current_phase: 'refine',
    });

    render(PlanView);

    const label = await screen.findByTestId('plan-phase');
    expect(label).toHaveTextContent('refine');
    expect(label.querySelector('.workflow-icon')?.getAttribute('style')).toContain(
      workflowIconUrl('phase.refine'),
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
    expect(entries).toHaveLength(4);
    // A clock the runtime already formatted carries no date of its own, so its group opens the
    // day it was read.
    const days = screen.getAllByTestId('activity-day');
    expect(days).toHaveLength(1);
    expect(days[0]).toHaveTextContent('today');
    expect(entries[0]).toHaveTextContent('14:02:11');
    expect(entries[0]).toHaveTextContent('Reading the relay transport');
    expect(entries[1]).toHaveTextContent('read');
    expect(entries[2]).toHaveTextContent('failed · 4 KiB');
    expect(entries[3]).toHaveTextContent('Transport.java');
  });

  it('opens a day above the first activity row of each day the wire dates', async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 9, 15, 12));
    const today = new Date();
    const dayAt = (daysAgo: number, hour: number): string =>
      new Date(
        today.getFullYear(),
        today.getMonth(),
        today.getDate() - daysAgo,
        hour,
      ).toISOString();
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      ...mockActivity,
      entries: [
        { timestamp: dayAt(0, 10), kind: 'text', content: 'first today' },
        { timestamp: dayAt(0, 11), kind: 'text', content: 'later today' },
        { timestamp: dayAt(1, 9), kind: 'text', content: 'yesterday' },
        // The same day and month in two years: each opens its own header.
        { timestamp: '2026-09-01T09:00:00Z', kind: 'text', content: 'this year' },
        { timestamp: '2025-09-01T09:00:00Z', kind: 'text', content: 'last year' },
      ],
    });

    render(PlanView);

    await screen.findByTestId('task-inspector');
    const days = await screen.findAllByTestId('activity-day');
    expect(days.map((node) => node.textContent)).toEqual([
      'today',
      'yesterday',
      days[2]?.textContent,
      days[2]?.textContent,
    ]);
    expect(days[2]?.textContent).toMatch(/Sep/);
  });

  it('opens one day above clock-only rows and a dated row of the same day', async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 9, 15, 12));
    const today = new Date();
    const datedAt = (hour: number, minute: number): string =>
      new Date(today.getFullYear(), today.getMonth(), today.getDate(), hour, minute).toISOString();
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      ...mockActivity,
      entries: [
        { timestamp: '14:02:11', kind: 'text', content: 'a bare clock' },
        { timestamp: datedAt(14, 5), kind: 'text', content: 'the same day, dated' },
        { timestamp: datedAt(14, 6), kind: 'text', content: 'still the same day' },
      ],
    });

    render(PlanView);

    await screen.findByTestId('task-inspector');
    const days = await screen.findAllByTestId('activity-day');
    expect(days).toHaveLength(1);
    expect(days[0]).toHaveTextContent('today');
  });

  it('keeps one day header across a row the runtime dates not at all', async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 9, 15, 12));
    const today = new Date();
    const datedAt = (hour: number, minute: number): string =>
      new Date(today.getFullYear(), today.getMonth(), today.getDate(), hour, minute).toISOString();
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      ...mockActivity,
      entries: [
        { timestamp: datedAt(14, 5), kind: 'text', content: 'dated' },
        { timestamp: '', kind: 'text', content: 'the runtime dates this row not at all' },
        { timestamp: datedAt(14, 6), kind: 'text', content: 'dated again' },
      ],
    });

    render(PlanView);

    await screen.findByTestId('task-inspector');
    expect(await screen.findAllByTestId('activity-day')).toHaveLength(1);
  });

  it('dates a streamed clock-only row by the day it arrives', async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    // The snapshot is read late on one day; the streamed row arrives just after midnight, so the
    // snapshot's read day and the row's arrival day differ.
    vi.setSystemTime(new Date(2026, 9, 15, 21, 0, 0));
    let onEvent: ((event: string, data: unknown) => void) | undefined;
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      ...mockActivity,
      entries: [
        {
          timestamp: new Date(2026, 9, 15, 20, 0, 0).toISOString(),
          kind: 'text',
          content: 'the snapshot, read before midnight',
        },
      ],
    });
    vi.spyOn(apiClient, 'subscribeTaskActivity').mockImplementation(
      (_planId: string, _taskId: string, callback) => {
        onEvent = callback;
        return () => {};
      },
    );

    render(PlanView);

    await screen.findByTestId('task-inspector');
    await waitFor(() => expect(onEvent).toBeDefined());
    vi.setSystemTime(new Date(2026, 9, 16, 0, 30, 0));
    onEvent?.('task.activity', {
      timestamp: '00:29:00',
      kind: 'text',
      content: 'arrived after midnight',
    });

    await waitFor(() => expect(screen.getAllByTestId('activity-entry')).toHaveLength(2));

    // Two days, not one: the row is dated by its arrival, not by the snapshot's read day.
    const days = screen.getAllByTestId('activity-day');
    expect(days).toHaveLength(2);
    expect(days[1]).toHaveTextContent('today');
  });

  it('prints no ISO-like string for a row whose clock the runtime sends malformed', async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 9, 15, 12));
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      ...mockActivity,
      entries: [{ timestamp: '25:99', kind: 'text', content: 'a clock that cannot exist' }],
    });

    render(PlanView);

    await screen.findByTestId('task-inspector');
    const rows = await screen.findAllByTestId('activity-entry');
    expect(rows[0]).toHaveTextContent('a clock that cannot exist');
    // No constructed instant reaches the row: an impossible clock shows no time at all.
    expect(rows[0]?.querySelector('.entry-time')?.textContent).toBe('');
    expect(rows[0]?.textContent).not.toMatch(/T\d{2}:\d{2}/);
  });

  it('reads a clock without seconds as the start of its minute', async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 9, 15, 12));
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      ...mockActivity,
      entries: [{ timestamp: '14:02', kind: 'text', content: 'a clock without seconds' }],
    });

    render(PlanView);

    await screen.findByTestId('task-inspector');
    const rows = await screen.findAllByTestId('activity-entry');
    expect(rows[0]?.querySelector('.entry-time')?.textContent).toBe('14:02:00');
  });

  it('dates a live row that carries its clock in time rather than timestamp', async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 9, 15, 12));
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

    onEvent?.('task.activity', { kind: 'text', text: 'Clock in time', time: '13:40:02' });

    await waitFor(() => expect(screen.getAllByTestId('activity-entry')).toHaveLength(5));
    const rows = screen.getAllByTestId('activity-entry');
    expect(rows[4]).toHaveTextContent('Clock in time');
    expect(rows[4]?.querySelector('.entry-time')?.textContent).toBe('13:40:02');
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

  it('does not render an in-page back button (D3)', async () => {
    openPlanView('fix-relay');

    render(PlanView);

    await screen.findByTestId('plan-title');
    expect(screen.queryByTestId('back-btn')).not.toBeInTheDocument();
  });

  it('renders WorkListColumn beside the plan on wide viewports (D4)', async () => {
    render(PlanView);

    await screen.findByTestId('plan-title');
    expect(screen.getByTestId('work-list-column')).toBeInTheDocument();
  });

  it('carries Up in the object column at 1023 px and below, once (D3)', async () => {
    const original = window.matchMedia;
    window.matchMedia = vi.fn().mockImplementation((query: string) => ({
      matches: query === '(max-width: 1023px)',
      media: query,
      addEventListener: () => {},
      removeEventListener: () => {},
    })) as unknown as typeof window.matchMedia;
    try {
      render(PlanView);

      await screen.findByTestId('plan-title');
      const holder = await screen.findByTestId('plan-view-up');

      expect(holder.querySelector('[data-testid="up-link"]')).toHaveAttribute(
        'href',
        '/epics/relay-hardening',
      );
      expect(holder).toHaveTextContent('relay-hardening');
      expect(screen.getAllByTestId('up-link')).toHaveLength(1);
    } finally {
      window.matchMedia = original;
    }
  });

  it('carries Up once above 1023 px, in the work list column (D3)', async () => {
    render(PlanView);

    await screen.findByTestId('plan-title');
    await waitFor(() => expect(screen.getAllByTestId('up-link')).toHaveLength(1));

    expect(screen.queryByTestId('plan-view-up')).toBeNull();
    expect(screen.getByTestId('up-link')).toHaveAttribute('href', '/epics/relay-hardening');
  });

  it('carries Up in the error state at 1023 px and below (D3)', async () => {
    const original = window.matchMedia;
    window.matchMedia = vi.fn().mockImplementation((query: string) => ({
      matches: query === '(max-width: 1023px)',
      media: query,
      addEventListener: () => {},
      removeEventListener: () => {},
    })) as unknown as typeof window.matchMedia;
    try {
      vi.spyOn(apiClient, 'getPlanProcess').mockRejectedValue(
        Object.assign(new Error('scope_not_found'), { status: 404 }),
      );
      runtimeStore.update((s) => ({ ...s, selectedProjectId: 'pm-mcp-clients' }));

      render(PlanView);

      await screen.findByTestId('plan-error');
      const holder = screen.getByTestId('plan-view-up');
      expect(holder.querySelector('[data-testid="up-link"]')).toHaveAttribute(
        'href',
        '/overview?project=pm-mcp-clients',
      );
    } finally {
      window.matchMedia = original;
    }
  });

  it('links to the process designer of the plan’s project', async () => {
    render(PlanView);

    await screen.findByTestId('plan-title');
    expect(screen.getByTestId('design-process-link')).toHaveAttribute(
      'href',
      '/designer?project=pm-mcp-clients',
    );
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

  it('shows an ended plan as it ended and links to its completion report', async () => {
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
    const endedAt = formatTime('2026-10-16T11:20:00Z', 'short', 'en');
    expect(banner).toHaveTextContent('The plan has ended: merged');
    expect(banner).toHaveTextContent(`ended ${endedAt}`);
    expect(banner).toHaveTextContent('Reason: the fallback held');

    expect(screen.getByTestId('completion-report-link')).toHaveAttribute(
      'href',
      '/reports?plan=fix-relay',
    );
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

  it('shows the error pattern of the scope code instead of a raw code', async () => {
    const process = vi
      .spyOn(apiClient, 'getPlanProcess')
      .mockRejectedValue(new Error('scope_not_found'));

    render(PlanView);

    const error = await screen.findByTestId('plan-error');
    expect(error).toHaveTextContent('This entry does not exist');
    expect(error).toHaveTextContent('Check the id or pick the entry from the list again.');
    expect(error.querySelector('details')?.querySelector('code')).toHaveTextContent(
      'scope_not_found',
    );
    await fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
    await waitFor(() => expect(process).toHaveBeenCalledTimes(2));
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

    onEvent?.('phase.changed');
    await waitFor(() =>
      expect(screen.getByTestId('node-narrative-title')).toHaveTextContent('init'),
    );

    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue({
      ...mockPlan,
      nodes: mockPlan.nodes.filter((node) => node.node_id !== 'init'),
    });
    onEvent?.('phase.changed');

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

  describe('the jump to the current position', () => {
    let scrolled: ReturnType<typeof vi.fn>;

    beforeEach(() => {
      scrolled = vi.fn();
      Element.prototype.scrollIntoView = scrolled as unknown as Element['scrollIntoView'];
    });

    it('opens the path, selects the running task and scrolls to it', async () => {
      render(PlanView);
      await screen.findByTestId('plan-title');
      await fireEvent.click(screen.getByTestId('select-node-init'));
      await fireEvent.click(screen.getByTestId('toggle-node-execute'));
      expect(screen.queryByTestId('task-TASK-003')).not.toBeInTheDocument();

      await fireEvent.click(screen.getByTestId('jump-btn'));

      expect(await screen.findByTestId('task-TASK-003')).toHaveAttribute('aria-current', 'true');
      expect(scrolled).toHaveBeenCalledWith({ behavior: 'smooth', block: 'center' });
    });

    it('selects the running node where no task runs', async () => {
      vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue({
        ...mockPlan,
        nodes: mockPlan.nodes.map((node) => ({
          ...node,
          leaf_tasks: node.leaf_tasks.map((task) => ({ ...task, status: 'done' })),
        })),
      });
      render(PlanView);
      await screen.findByTestId('plan-title');
      await fireEvent.click(screen.getByTestId('select-node-init'));

      await fireEvent.click(screen.getByTestId('jump-btn'));

      expect(screen.getByTestId('select-node-execute')).toHaveAttribute('aria-current', 'true');
    });

    it('keeps the selection where nothing runs', async () => {
      vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue({
        ...mockPlan,
        nodes: mockPlan.nodes.map((node) => ({
          ...node,
          state: 'done',
          leaf_tasks: node.leaf_tasks.map((task) => ({ ...task, status: 'done' })),
        })),
      });
      render(PlanView);
      await screen.findByTestId('plan-title');
      await fireEvent.click(screen.getByTestId('select-node-finalize'));

      await fireEvent.click(screen.getByTestId('jump-btn'));

      expect(screen.getByTestId('select-node-finalize')).toHaveAttribute('aria-current', 'true');
      expect(scrolled).not.toHaveBeenCalled();
    });
  });

  it.each(PLAN_PROCESS_REFRESH_EVENTS)(
    'reads the process again on the runtime event %s',
    async (name) => {
      let onEvent: ((event: string, data?: unknown) => void) | undefined;
      vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((callback) => {
        onEvent = callback;
        return () => {};
      });
      const load = vi.spyOn(apiClient, 'getPlanProcess');
      render(PlanView);
      await screen.findByTestId('plan-title');

      onEvent?.('hold.opened');
      expect(load).toHaveBeenCalledTimes(1);
      onEvent?.(name);

      await waitFor(() => expect(load).toHaveBeenCalledTimes(2));
    },
  );

  it('says the attempt in the interface language, in the task row and the activity head', async () => {
    const { setLocale, messagesStore, currentLocale } = await import('../i18n');
    const { loadMessages } = await import('../../test/catalogue-fixture');
    vi.spyOn(apiClient, 'getMessages').mockImplementation(async (locale) => loadMessages(locale));
    render(PlanView);
    await screen.findByTestId('activity-feed');

    try {
      await setLocale('de');
      // the switch itself succeeded: the language changed, and the texts below are its catalogue's
      expect(get(currentLocale)).toBe('de');

      await waitFor(() =>
        expect(screen.getByTestId('activity-attempt')).toHaveTextContent('Versuch 1'),
      );
      expect(screen.getByTestId('task-TASK-003')).toHaveTextContent('Versuch 1');
      expect(screen.getByTestId('task-TASK-003')).not.toHaveTextContent('attempt');
    } finally {
      messagesStore.set(loadMessages('en'));
      await setLocale('en');
    }
  });

  it.each([
    [
      'unmeasured tokens',
      { not_measured: true, floor: false, tasks_measured: 4 },
      'not measured · 6 of 10 tasks not measured',
    ],
    [
      'a floor with unmeasured tasks',
      { not_measured: false, floor: true, tasks_measured: 7 },
      '3 of 10 tasks not measured',
    ],
    ['a floor over every task', { not_measured: false, floor: true, tasks_measured: 10 }, null],
  ])('says how far the tokens are measured for %s', async (_case, tokens, coverage) => {
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue({
      ...mockPlan,
      tokens_so_far: { ...mockPlan.tokens_so_far, tasks_total: 10, ...tokens },
    });
    render(PlanView);

    const line = await screen.findByTestId('plan-tokens');
    if (coverage) {
      expect(line).toHaveTextContent(coverage);
    } else {
      expect(line).not.toHaveTextContent('not measured');
    }
  });

  it.each([
    [
      'a changed entry by the file it names',
      { kind: 'changed', files: ['pm-relay/Relay.java'] },
      'pm-relay/Relay.java',
    ],
    [
      'a gap by its own words',
      { kind: 'gap', content: 'the transcript was cut here' },
      'the transcript was cut here',
    ],
  ])('shows %s', async (_case, entry, expected) => {
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      ...mockActivity,
      entries: [{ timestamp: '14:03:02', ...entry }],
    });
    render(PlanView);

    const [row] = await screen.findAllByTestId('activity-entry');
    expect(row).toHaveTextContent(expected);
  });

  it('names a failed tool call without a size by its status alone', async () => {
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      ...mockActivity,
      entries: [
        {
          timestamp: '14:02:20',
          kind: 'tool',
          tool_name: 'bash',
          tool_target: './mvnw test',
          tool_failed: true,
        },
      ],
    });
    render(PlanView);

    const badge = await screen.findByTestId('tool-failed');
    expect(badge.textContent?.trim()).toBe('failed');
  });
});

describe('PlanView’s Changes tab', () => {
  const capturedPlan: ProcessTree = {
    plan_id: 'fix-relay',
    title: 'fix-relay',
    project: { project_id: 'pm-mcp-clients', display_name: 'pm-mcp-clients' },
    epic: { epic_id: 'relay-hardening', title: 'relay-hardening' },
    current_phase: 'execute',
    phase_state: 'running',
    instance_version: 1,
    needs_operator: false,
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
    current_position_path: ['execute'],
    nodes: [
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
          },
        ],
      },
    ],
    change: { files: 12, insertions: 340, deletions: 27, unplanned: 2 },
  };

  const capture: PlanChangesAnswer = {
    plan_id: 'fix-relay',
    state: 'captured',
    head: '9f3c2ab41d7e',
    base_ref: 'main',
    files: [
      {
        path: 'pm-relay/src/main/java/Transport.java',
        status: 'modified',
        added: 48,
        removed: 12,
        binary: false,
        unplanned: false,
      },
    ],
  };

  const transportFile: PlanChangeFile = {
    path: 'pm-relay/src/main/java/Transport.java',
    hunks: [
      {
        old_start: 4,
        old_len: 1,
        new_start: 4,
        new_len: 1,
        lines: [
          { kind: 'removed', text: 'return transport.send(oldRequest);' },
          { kind: 'added', text: 'return transport.send(newRequest);' },
        ],
      },
    ],
    binary: false,
    truncated: false,
  };

  /**
   * @returns the plan's process as a plan before its first capture: it carries no size of change
   */
  function planWithoutCapture(): ProcessTree {
    const tree: ProcessTree = { ...capturedPlan };
    delete tree.change;
    return tree;
  }

  beforeEach(() => {
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
        selectedTaskId: null,
      }),
    );
    // The plan view stands at the plan's own address: the tab and the file are written beside it.
    window.history.replaceState({}, '', '/plans/fix-relay');
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(capturedPlan);
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue(capture);
    vi.spyOn(apiClient, 'getPlanChangeFile').mockResolvedValue(transportFile);
  });

  it('shows no Changes tab and no size line before the first capture', async () => {
    // Arrange
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(planWithoutCapture());

    // Act
    render(PlanView);
    await screen.findByTestId('plan-title');

    // Assert
    expect(screen.queryByTestId('plan-change-size')).toBeNull();
    expect(screen.queryByRole('tab', { name: 'Changes' })).toBeNull();
  });

  it('opens the Changes tab from the size line and writes the tab into the address', async () => {
    // Arrange
    render(PlanView);
    await screen.findByTestId('plan-title');
    expect(screen.getByTestId('plan-change-size')).toHaveTextContent(
      '12 files · +340 −27 · 2 unplanned',
    );

    // Act
    screen.getByTestId('plan-change-size').click();

    // Assert
    expect(await screen.findByTestId('plan-changes')).toBeInTheDocument();
    expect(get(runtimeStore).objectTab).toBe('changes');
    expect(window.location.search).toContain('tab=changes');
    expect(screen.getByRole('tab', { name: 'Changes' })).toHaveAttribute('aria-selected', 'true');
  });

  it('opens the tab and the file an address names, as a reload shows them', async () => {
    // Arrange: the runtime the router fills from `?tab=changes&file=…`.
    runtimeStore.update((state) => ({
      ...state,
      objectTab: 'changes',
      changeFile: 'pm-relay/src/main/java/Transport.java',
    }));
    window.history.replaceState(
      {},
      '',
      '/plans/fix-relay?tab=changes&file=pm-relay%2Fsrc%2Fmain%2Fjava%2FTransport.java',
    );

    // Act
    render(PlanView);

    // Assert
    expect(await screen.findByTestId('plan-changes')).toBeInTheDocument();
    expect(await screen.findByTestId('change-hunks')).toHaveTextContent(
      'return transport.send(newRequest);',
    );
    expect(screen.getByTestId('change-row')).toHaveAttribute('aria-current', 'true');
  });

  it('opens Process and replaces the address when it names the tab before the first capture', async () => {
    // Arrange
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(planWithoutCapture());
    runtimeStore.update((state) => ({ ...state, objectTab: 'changes', changeFile: null }));
    window.history.replaceState({}, '', '/plans/fix-relay?tab=changes');

    // Act
    render(PlanView);
    await screen.findByTestId('plan-title');

    // Assert
    await waitFor(() => expect(get(runtimeStore).objectTab).toBe('process'));
    expect(window.location.search).not.toContain('tab=changes');
    expect(screen.queryByTestId('plan-changes')).toBeNull();
  });

  it('shows the reason of a capture that is gone inside the tab', async () => {
    // Arrange
    const reason = 'the captured commit 4c1d7e0 is no longer in the repository';
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue({
      plan_id: 'fix-relay',
      state: 'unavailable',
      reason,
    });

    // Act
    render(PlanView);
    await screen.findByTestId('plan-title');
    screen.getByTestId('plan-change-size').click();

    // Assert
    expect(await screen.findByTestId('changes-unavailable')).toHaveTextContent(reason);
  });

  it('shows both models of a model_differs entry and keeps the launch model in the head', async () => {
    // Arrange: the PLAN-162 fixture's entry, pinned to the two models it names.
    runtimeStore.update((state) => ({ ...state, selectedTaskId: 'TASK-003' }));
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      task_id: 'TASK-003',
      role: 'implement',
      harness: 'claude',
      model: 'sonnet',
      attempt: 1,
      duration_seconds: 360,
      tokens_so_far: 48000,
      entries: [
        {
          timestamp: '14:03:30',
          kind: 'model_differs',
          launch_model: 'sonnet',
          served_model: 'sonnet-5.5',
        },
      ],
    });

    // Act
    render(PlanView);

    // Assert
    expect(await screen.findByTestId('model-differs')).toHaveTextContent(
      'answered by sonnet-5.5 instead of sonnet',
    );
    // The head keeps the launch model, and only it: the served model stands in the entry alone.
    expect(screen.getByTestId('activity-model')).toHaveTextContent(/^claude sonnet$/);
  });

  it('shows both models of a model_differs entry the live stream reports', async () => {
    // Arrange: the activity's live stream carries the entry's own fields, not the fetched list's.
    runtimeStore.update((state) => ({ ...state, selectedTaskId: 'TASK-003' }));
    let emit: ((event: string, data: unknown) => void) | null = null;
    vi.spyOn(apiClient, 'subscribeTaskActivity').mockImplementation((_planId, _taskId, handler) => {
      emit = handler;
      return () => {};
    });
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      task_id: 'TASK-003',
      role: 'implement',
      harness: 'claude',
      model: 'sonnet',
      attempt: 1,
      duration_seconds: 360,
      tokens_so_far: 48000,
      entries: [],
    });
    render(PlanView);
    await screen.findByTestId('activity-feed');

    // Act
    emit!('task.activity', {
      kind: 'model_differs',
      timestamp: '14:03:30',
      launch_model: 'sonnet',
      served_model: 'sonnet-5.5',
    });
    await tick();

    // Assert
    expect(await screen.findByTestId('model-differs')).toHaveTextContent(
      'answered by sonnet-5.5 instead of sonnet',
    );
  });

  it('keeps the open file when the size line is clicked on the tab it already shows', async () => {
    // Arrange: the Changes tab is open on a file, as a reload of its address shows it.
    runtimeStore.update((state) => ({
      ...state,
      objectTab: 'changes',
      changeFile: 'pm-relay/src/main/java/Transport.java',
    }));
    window.history.replaceState(
      {},
      '',
      '/plans/fix-relay?tab=changes&file=pm-relay/src/main/java/Transport.java',
    );
    render(PlanView);
    await screen.findByTestId('change-hunks');

    // Act
    screen.getByTestId('plan-change-size').click();
    await tick();

    // Assert
    expect(window.location.search).toContain('file=pm-relay/src/main/java/Transport.java');
  });

  it('takes the size a capture announces and reads the list again', async () => {
    // Arrange
    let emit: ((event: string, data: unknown) => void) | null = null;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((handler) => {
      emit = handler;
      return () => {};
    });
    const readList = vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue(capture);
    render(PlanView);
    await screen.findByTestId('plan-title');
    // The tab mounts the list, so its reads are counted from here.
    screen.getByTestId('plan-change-size').click();
    await screen.findByTestId('plan-changes');
    const readsBefore = readList.mock.calls.length;
    expect(readsBefore).toBeGreaterThan(0);

    // Act: the stream announces a capture of this plan.
    emit!('plan.change_captured', {
      plan_id: 'fix-relay',
      head: 'a1b2c3d4e5f6a',
      files_total: 13,
      insertions_total: 350,
      deletions_total: 27,
      unplanned_total: 2,
    });
    await tick();

    // Assert
    expect(screen.getByTestId('plan-change-size')).toHaveTextContent(
      '13 files · +350 −27 · 2 unplanned',
    );
    await waitFor(() => expect(readList.mock.calls.length).toBeGreaterThan(readsBefore));
  });

  it('ignores a capture another plan announces', async () => {
    // Arrange
    let emit: ((event: string, data: unknown) => void) | null = null;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((handler) => {
      emit = handler;
      return () => {};
    });
    const readList = vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue(capture);
    render(PlanView);
    await screen.findByTestId('plan-title');
    screen.getByTestId('plan-change-size').click();
    await screen.findByTestId('plan-changes');
    const readsBefore = readList.mock.calls.length;
    const sizeBefore = screen.getByTestId('plan-change-size').textContent;

    // Act
    emit!('plan.change_captured', {
      plan_id: 'relay-metrics',
      head: 'b2c3d4e5f6a1',
      files_total: 1,
      insertions_total: 1,
      deletions_total: 0,
      unplanned_total: 0,
    });
    await tick();

    // Assert: neither the size nor the open list moved for another plan's capture.
    expect(screen.getByTestId('plan-change-size').textContent).toBe(sizeBefore);
    expect(readList).toHaveBeenCalledTimes(readsBefore);
  });

});

describe('PlanView head on the object page (PLAN-55 D1)', () => {
  function headPlan(overrides: Partial<ProcessTree> = {}): ProcessTree {
    return {
      plan_id: 'fix-relay',
      title: 'Fix Relay Transport',
      project: { project_id: 'pm-mcp-clients', display_name: 'pm-mcp-clients' },
      epic: { epic_id: 'relay-hardening', title: 'Relay hardening' },
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
        tasks_total: 5,
        tasks_measured: 5,
        none_yet: false,
        not_measured: false,
      },
      current_position_path: ['execute', 'TASK-003'],
      nodes: [
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
            },
          ],
        },
      ],
      ...overrides,
    };
  }

  const decision: DecisionEnvelope = {
    id: 'Q-007',
    revision: 1,
    title: 'Resolve rebase conflict',
    subject: 'plan fix-relay',
    scope: 'plan',
    plan: 'fix-relay',
    blocks: 'plan',
    reversibility: 'reversible',
    parts: [],
  };

  beforeEach(() => {
    vi.restoreAllMocks();
    runtimeStore.set(
      makeRuntimeState({
        currentRoute: 'plan',
        selectedPlanId: 'fix-relay',
        selectedEpicId: 'relay-hardening',
      }),
    );
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      display_name: 'pm-mcp-clients',
      plans: [],
      epics: [],
      alongside_landings: [],
    });
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      task_id: 'TASK-003',
      role: 'implement',
      harness: 'claude',
      model: 'sonnet',
      attempt: 1,
      duration_seconds: 360,
      tokens_so_far: 48000,
      entries: [],
    });
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'subscribeTaskActivity').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);
  });

  it('draws the head slots of the object page', async () => {
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(headPlan());

    render(PlanView);
    await screen.findByTestId('plan-title');

    expect(screen.getByTestId('object-page')).toBeInTheDocument();
    expect(
      screen.getByTestId('plan-kind').querySelector('[data-kind="plan"]'),
    ).toBeInTheDocument();
    expect(screen.getByTestId('object-page-title')).toHaveTextContent('Fix Relay Transport');
    expect(screen.getByTestId('plan-id-chip')).toHaveTextContent('fix-relay');
    expect(screen.getByTestId('plan-pr-chip')).toHaveTextContent('PR #214');
    expect(screen.getByTestId('plan-phase')).toHaveTextContent('execute');
    expect(screen.getByTestId('plan-state')).toHaveTextContent('running');
    expect(screen.getByTestId('plan-state-sentence')).toHaveTextContent('for 2 h 10 min');
  });

  it('draws no in-page path', async () => {
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(headPlan());

    render(PlanView);
    await screen.findByTestId('plan-title');

    expect(screen.queryByTestId('plan-breadcrumb')).toBeNull();
    expect(screen.queryByTestId('breadcrumb-project')).toBeNull();
    expect(screen.queryByTestId('breadcrumb-epic')).toBeNull();
  });

  it('shows the completion report only on an ended plan', async () => {
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(headPlan());

    render(PlanView);
    await screen.findByTestId('plan-title');
    expect(screen.queryByTestId('completion-report-link')).toBeNull();

    cleanup();
    runtimeStore.update((state) => ({
      ...state,
      selectedPlanEnded: {
        plan_id: 'fix-relay',
        outcome: 'merged',
        ended_at: '2026-10-16T11:20:00Z',
      },
    }));

    render(PlanView);
    await screen.findByTestId('plan-title');
    expect(screen.getByTestId('completion-report-link')).toBeInTheDocument();
  });

  it('shows the attention strip and the decision link on a waiting plan', async () => {
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(
      headPlan({ phase_state: 'waiting_for_you', needs_operator: true }),
    );
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([decision]);

    render(PlanView);
    await screen.findByTestId('plan-title');

    await waitFor(() =>
      expect(screen.getByTestId('plan-attention-strip')).toHaveTextContent(
        'Waits for your decision',
      ),
    );
    const link = await screen.findByTestId('plan-decision-link');
    expect(link).toHaveAttribute('href', '/decisions/Q-007');
    expect(link).toHaveTextContent('Open the decision');
  });

  it('shows neither the attention strip nor the decision link on a running plan', async () => {
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(headPlan());
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([decision]);

    render(PlanView);
    await screen.findByTestId('plan-title');

    expect(screen.queryByTestId('plan-attention-strip')).toBeNull();
    expect(screen.queryByTestId('plan-decision-link')).toBeNull();
  });

  it('shows the off-track banner on an off-track plan', async () => {
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(
      headPlan({
        nodes: [
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
                off_track: { signal: 'repeating', action: 'shown', value: 5, threshold: 5 },
              },
            ],
          },
        ],
      }),
    );

    render(PlanView);
    await screen.findByTestId('plan-title');

    expect(screen.getByTestId('plan-off-track')).toHaveTextContent('off track: repeating');
  });

  it('shows the queued revision line when the plan reports one', async () => {
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(headPlan());
    let onEvent: ((event: string, data: unknown) => void) | undefined;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((callback) => {
      onEvent = callback;
      return () => {};
    });

    render(PlanView);
    await screen.findByTestId('plan-title');
    await waitFor(() => expect(onEvent).toBeDefined());

    onEvent?.('plan.revision_queued', {
      plan_id: 'fix-relay',
      queued_revision: {
        chat_id: 'chat-1',
        turn_id: 'turn-1',
        subject: { kind: 'plan', id: 'fix-relay' },
        chat_open: true,
        applies: { key: 'chat.applies.after_task', args: { task_id: 'TASK-003' } },
      },
    });

    await waitFor(() =>
      expect(screen.getByTestId('plan-revision-queued')).toHaveTextContent(
        'Revision from the chat queued',
      ),
    );
    expect(screen.getByTestId('plan-revision-queued')).toHaveTextContent('after TASK-003');
  });

  it('shows the queued revision the plan resource carries after a reload', async () => {
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(
      headPlan({
        queued_revision: {
          chat_id: 'chat-1',
          turn_id: 'turn-1',
          subject: { kind: 'plan', id: 'fix-relay' },
          chat_open: true,
          applies: { key: 'chat.applies.on_start' },
        },
      }),
    );

    render(PlanView);
    await screen.findByTestId('plan-title');

    expect(screen.getByTestId('plan-revision-queued')).toHaveTextContent(
      'applies when fix-relay starts again',
    );
  });
});

describe('PlanView activity rows (PLAN-55 D3)', () => {
  const activityPlan: ProcessTree = {
    plan_id: 'fix-relay',
    title: 'Fix Relay Transport',
    project: { project_id: 'pm-mcp-clients', display_name: 'pm-mcp-clients' },
    current_phase: 'execute',
    phase_state: 'running',
    instance_version: 1,
    needs_operator: false,
    duration_seconds: 7800,
    tokens_so_far: {
      total: 0,
      input: 0,
      output: 0,
      floor: false,
      tasks_total: 1,
      tasks_measured: 1,
      none_yet: false,
      not_measured: false,
    },
    current_position_path: ['execute'],
    nodes: [
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
          },
        ],
      },
    ],
  };

  beforeEach(() => {
    vi.restoreAllMocks();
    runtimeStore.set(
      makeRuntimeState({ currentRoute: 'plan', selectedPlanId: 'fix-relay' }),
    );
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(activityPlan);
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      display_name: 'pm-mcp-clients',
      plans: [],
      epics: [],
      alongside_landings: [],
    });
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'subscribeTaskActivity').mockReturnValue(() => {});
  });

  it('renders no row for a link entry without a target and one for a link entry with a target', async () => {
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      task_id: 'TASK-003',
      role: 'implement',
      harness: 'claude',
      model: 'sonnet',
      attempt: 1,
      duration_seconds: 360,
      tokens_so_far: 48000,
      entries: [
        { timestamp: '14:03:40', kind: 'link' },
        { timestamp: '14:03:41', kind: 'link', tool_target: 'submit:TASK-003' },
      ],
    });

    render(PlanView);
    await screen.findByTestId('activity-feed');

    const rows = screen.getAllByTestId('activity-entry');
    expect(rows).toHaveLength(1);
    expect(rows[0]).toHaveTextContent('submit:TASK-003');
  });

  it('names the kind icon by the catalogue word instead of a text tag', async () => {
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      task_id: 'TASK-003',
      role: 'implement',
      harness: 'claude',
      model: 'sonnet',
      attempt: 1,
      duration_seconds: 360,
      tokens_so_far: 48000,
      entries: [
        {
          timestamp: '14:02:14',
          kind: 'tool',
          tool_name: 'read',
          tool_target: 'pm-relay/src/main/java/Transport.java',
        },
      ],
    });

    render(PlanView);
    await screen.findByTestId('activity-feed');

    const icon = screen.getByTestId('activity-kind');
    expect(icon).toHaveAttribute('role', 'img');
    expect(icon).toHaveAttribute('aria-label', 'tool');
    const row = screen.getByTestId('activity-entry');
    expect(row.querySelector('[data-icon]')).not.toBeNull();
    expect(row.textContent).not.toMatch(/\btool\b/);
  });
});

describe('PlanView tab set and column layout (PLAN-55 D2)', () => {
  const capturedPlan: ProcessTree = {
    plan_id: 'fix-relay',
    title: 'Fix Relay Transport',
    project: { project_id: 'pm-mcp-clients', display_name: 'pm-mcp-clients' },
    current_phase: 'execute',
    phase_state: 'running',
    instance_version: 1,
    needs_operator: false,
    duration_seconds: 7800,
    tokens_so_far: {
      total: 412000,
      input: 380000,
      output: 32000,
      floor: false,
      tasks_total: 5,
      tasks_measured: 5,
      none_yet: false,
      not_measured: false,
    },
    current_position_path: ['execute'],
    nodes: [
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
          },
        ],
      },
    ],
    change: { files: 12, insertions: 340, deletions: 27, unplanned: 2 },
  };

  const intentRecord: PlanIntentRecord = {
    plan_id: 'fix-relay',
    revision: 3,
    goal: 'Make the relay reconnect without losing queued events.',
    criteria: [],
    non_goals: [],
    decisions: [],
    open_questions: [],
    deferred: [],
    builder_check: { resolution: 'pending', questions: [] },
  };

  const outlineRecord: OutlineRecord = {
    plan_id: 'fix-relay',
    revision: 2,
    summary: 'The relay reconnects.',
    deliverables: [],
    added_dependencies: [],
  };

  beforeEach(() => {
    vi.restoreAllMocks();
    runtimeStore.set(
      makeRuntimeState({ currentRoute: 'plan', selectedPlanId: 'fix-relay' }),
    );
    vi.spyOn(apiClient, 'getPlanProcess').mockResolvedValue(capturedPlan);
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      display_name: 'pm-mcp-clients',
      plans: [],
      epics: [],
      alongside_landings: [],
    });
    vi.spyOn(apiClient, 'getTaskActivity').mockResolvedValue({
      task_id: 'TASK-003',
      role: 'implement',
      harness: 'claude',
      model: 'sonnet',
      attempt: 1,
      duration_seconds: 360,
      tokens_so_far: 48000,
      entries: [],
    });
    vi.spyOn(apiClient, 'getDecisions').mockResolvedValue([]);
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'subscribeTaskActivity').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getPlanIntent').mockResolvedValue(intentRecord);
    vi.spyOn(apiClient, 'getPlanOutline').mockResolvedValue(outlineRecord);
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue({
      plan_id: 'fix-relay',
      state: 'captured',
      head: '9f3c2ab41d7e',
      base_ref: 'main',
      files: [
        {
          path: 'pm-relay/src/main/java/Transport.java',
          status: 'modified',
          added: 48,
          removed: 12,
          binary: false,
          unplanned: false,
        },
      ],
    });
    vi.spyOn(apiClient, 'getPlanChangeFile').mockResolvedValue({
      path: 'pm-relay/src/main/java/Transport.java',
      hunks: [],
      binary: false,
      truncated: false,
    });
  });

  it('draws exactly the three tabs', async () => {
    render(PlanView);
    await screen.findByTestId('plan-title');

    const tabs = screen.getAllByRole('tab');
    expect(tabs.map((tab) => tab.textContent?.trim())).toEqual(['Process', 'Intent', 'Changes']);
  });

  it('opens the Intent tab from its address with the three panels in order', async () => {
    runtimeStore.update((state) => ({ ...state, objectTab: 'intent' }));
    window.history.replaceState({}, '', '/plans/fix-relay?tab=intent');

    render(PlanView);
    await screen.findByTestId('plan-title');

    const intent = await screen.findByTestId('plan-intent-panel');
    const builder = screen.getByTestId('builder-check');
    const outline = await screen.findByTestId('outline-panel');

    expect(intent.compareDocumentPosition(builder) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(builder.compareDocumentPosition(outline) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('says the intent is not written and shows no panel when refine wrote no record', async () => {
    vi.spyOn(apiClient, 'getPlanIntent').mockResolvedValue(null);
    runtimeStore.update((state) => ({ ...state, objectTab: 'intent' }));
    window.history.replaceState({}, '', '/plans/fix-relay?tab=intent');

    render(PlanView);
    await screen.findByTestId('plan-title');

    expect(await screen.findByTestId('plan-intent-none')).toHaveTextContent(
      'Refine has not written the intent yet.',
    );
    expect(screen.queryByTestId('plan-intent-panel')).toBeNull();
    expect(screen.queryByTestId('outline-panel')).toBeNull();
  });

  it('lays the process side by side at a 720 px column and stacked at 719 px', async () => {
    render(PlanView);
    await screen.findByTestId('plan-title');

    const host = screen.getByTestId('plan-panel-host');
    reportBox(host, 720, 600);
    await tick();
    expect(host).toHaveAttribute('data-layout', 'side');

    reportBox(host, 719, 600);
    await tick();
    expect(host).toHaveAttribute('data-layout', 'stacked');
  });

  it('lays the changes side by side at a 720 px column and stacked at 719 px', async () => {
    render(PlanView);
    await screen.findByTestId('plan-title');

    await fireEvent.click(screen.getByTestId('plan-change-size'));
    const host = screen.getByTestId('plan-panel-host');

    reportBox(host, 720, 600);
    await tick();
    expect(host).toHaveAttribute('data-layout', 'side');
    expect(screen.getByTestId('plan-changes').querySelector('.plan-changes__body')).toHaveClass(
      'plan-changes__body--wide',
    );

    reportBox(host, 719, 600);
    await tick();
    expect(host).toHaveAttribute('data-layout', 'stacked');
    expect(screen.getByTestId('plan-changes').querySelector('.plan-changes__body')).not.toHaveClass(
      'plan-changes__body--wide',
    );
  });

  it('drops file= when Changes becomes Process and restores the file from memory', async () => {    runtimeStore.update((state) => ({
      ...state,
      objectTab: 'changes',
      changeFile: 'pm-relay/src/main/java/Transport.java',
    }));

    render(PlanView);
    await screen.findByTestId('plan-title');
    await screen.findByTestId('plan-changes');

    const processTab = screen.getAllByRole('tab')[0];
    await fireEvent.click(processTab);
    await waitFor(() => expect(get(runtimeStore).objectTab).toBe('process'));
    expect(window.location.search).not.toContain('file=');

    const changesTab = screen.getAllByRole('tab')[2];
    await fireEvent.click(changesTab);
    await waitFor(() => expect(get(runtimeStore).objectTab).toBe('changes'));
    expect(get(runtimeStore).changeFile).toBe('pm-relay/src/main/java/Transport.java');
  });

  it('opens Process and replaces an address that names a tab of another object', async () => {
    // `access` is a settings tab: no tab of this view, so Process stands in and the address is rewritten.
    runtimeStore.update((state) => ({ ...state, objectTab: 'access' }));
    window.history.replaceState({}, '', '/plans/fix-relay?tab=access');

    render(PlanView);
    await screen.findByTestId('plan-title');

    await waitFor(() => expect(get(runtimeStore).objectTab).toBe('process'));
    expect(window.location.search).not.toContain('tab=access');
    expect(screen.queryByTestId('plan-changes')).toBeNull();
  });
});
