import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import EpicView from './EpicView.svelte';
import { apiClient, type EpicProcess, type EpicResource, type ReviewResource } from '../api/client';
import { workflowIconUrl } from '../icons/workflowIcons';
import { runtimeStore, navigateTo, openPlanView, openEpicView } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { get } from 'svelte/store';

describe('EpicView component', () => {
  const mockEpic: EpicProcess = {
    epic_id: 'relay-hardening',
    project: 'pm-mcp-clients',
    title: 'relay-hardening',
    state: 'running',
    review_ready: true,
    running_plans: [
      {
        plan_id: 'fix-relay',
        title: 'fix-relay',
        current_phase: 'execute',
        phase_state: 'running',
        tasks_done: 4,
        tasks_total: 10,
        running: true,
      },
    ],
    staged_plans: [
      {
        plan_id: 'relay-metrics',
        title: 'relay-metrics',
        current_phase: '',
        phase_state: 'staged',
        tasks_done: 0,
        tasks_total: 6,
        running: false,
      },
    ],
    alongside_landings: ['landing doc/update.md · done', 'removing 2 moved documents · done'],
    tokens_so_far: {
      total: 3860000,
      input: 3520000,
      output: 340000,
      floor: false,
      tasks_total: 16,
      tasks_measured: 16,
      none_yet: false,
      not_measured: false,
    },
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
        currentRoute: 'epic',
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
        selectedPlanId: null,
        selectedEpicId: 'relay-hardening',
        selectedTaskId: null,
      }),
    );

    vi.spyOn(apiClient, 'getEpicProcess').mockResolvedValue(mockEpic);
    vi.spyOn(apiClient, 'getEpicResource').mockRejectedValue(new Error('no resource'));
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  });

  it('renders epic header, breadcrumb, title, state, tokens, and review badge', async () => {
    render(EpicView);

    expect(await screen.findByTestId('epic-title')).toHaveTextContent('relay-hardening');
    expect(screen.getByTestId('breadcrumb-project')).toHaveTextContent('pm-mcp-clients');
    expect(screen.getByTestId('breadcrumb-epic')).toHaveTextContent('relay-hardening');

    expect(screen.getByTestId('epic-state')).toHaveTextContent('running');
    expect(screen.getByTestId('review-ready-badge')).toHaveTextContent(
      /completion review is ready/i,
    );

    expect(screen.getByTestId('epic-tokens')).toHaveTextContent(/3.86 M/);
  });

  it('renders running plans and staged plans with task counts', async () => {
    render(EpicView);

    const runningPlan = await screen.findByTestId('running-plan-fix-relay');
    expect(runningPlan).toBeInTheDocument();
    expect(runningPlan).toHaveTextContent('fix-relay');
    expect(runningPlan).toHaveTextContent('execute');
    expect(runningPlan).toHaveTextContent('4 of 10 tasks');
    expect(
      runningPlan.querySelector('.badge--phase .workflow-icon')?.getAttribute('style'),
    ).toContain(workflowIconUrl('phase.execute'));

    const stagedPlan = await screen.findByTestId('staged-plan-relay-metrics');
    expect(stagedPlan).toBeInTheDocument();
    expect(stagedPlan).toHaveTextContent('relay-metrics');
    expect(stagedPlan).toHaveTextContent('0 of 6 tasks');
  });

  it('renders alongside landings list', async () => {
    render(EpicView);

    const landingsSection = await screen.findByTestId('epic-alongside-section');
    expect(landingsSection).toBeInTheDocument();
    const items = screen.getAllByTestId('alongside-item');
    expect(items.length).toBe(2);
    expect(items[0]).toHaveTextContent('landing doc/update.md · done');
  });

  it('clicking a plan card opens the plan view with selectedPlanId', async () => {
    render(EpicView);

    const runningPlan = await screen.findByTestId('running-plan-fix-relay');
    await fireEvent.click(runningPlan);

    const state = get(runtimeStore);
    expect(state.currentRoute).toBe('plan');
    expect(state.selectedPlanId).toBe('fix-relay');
  });

  it('returns to the plan it was opened from, then to the overview', async () => {
    navigateTo('overview');
    openPlanView('fix-relay');
    openEpicView('relay-hardening');

    render(EpicView);
    await screen.findByTestId('epic-title');

    await fireEvent.click(screen.getByTestId('back-btn'));
    expect(get(runtimeStore).currentRoute).toBe('plan');
    expect(get(runtimeStore).selectedPlanId).toBe('fix-relay');

    await fireEvent.click(screen.getByTestId('back-btn'));
    expect(get(runtimeStore).currentRoute).toBe('overview');
  });

  it('opens the project in the explorer in one step', async () => {
    render(EpicView);

    await screen.findByTestId('epic-title');
    await fireEvent.click(screen.getByTestId('breadcrumb-project'));

    const state = get(runtimeStore);
    expect(state.currentRoute).toBe('projects');
    expect(state.selectedProjectId).toBe('pm-mcp-clients');
    expect(window.location.search).toBe('?project=pm-mcp-clients');
  });

  const mockResource: EpicResource = {
    epic_id: 'relay-hardening',
    project_id: 'pm-mcp-clients',
    display_name: 'pm-mcp-clients',
    state: 'running',
    auto_start: true,
    overlap_max_files: 0,
    scope: 3,
    running: 3,
    workstreams: [
      {
        id: 'WS-1',
        name: 'Reconnect',
        open: true,
        rows: [
          {
            plan_id: 'relay-cursor-order',
            title: 'relay-cursor-order',
            status: 'running',
            pull_request_number: 211,
            pull_request_url: 'https://github.com/plan-marshall/pm-mcp-clients/pull/211',
          },
          {
            plan_id: 'relay-docs',
            title: 'relay-docs',
            status: 'staged',
            wait_reason: 'overlap',
            wait_plan_id: 'relay-auth-v2',
            overlap_files: ['pm-api/src/store/Docs.java'],
          },
          {
            plan_id: 'relay-reconnect-metrics',
            title: 'relay-reconnect-metrics',
            status: 'staged',
            wait_reason: 'none',
          },
        ],
      },
    ],
    sources: ['2 documents', '1 issue'],
    landings: 2,
    findings: { new: 3, open: 4, kept: 2 },
    watches: { new: 1, open: 4, kept: 0 },
    tokens: {
      total: {
        total: 3860000,
        input: 3520000,
        output: 340000,
        measured: true,
        floor: true,
        measured_tasks: 4,
        not_measured_tasks: 1,
      },
      own: {
        total: 120000,
        input: 110000,
        output: 10000,
        measured: true,
        floor: false,
        measured_tasks: 1,
        not_measured_tasks: 0,
      },
      plans: [
        {
          plan_id: 'relay-reconnect-backoff',
          figure: {
            total: 1400000,
            input: 1300000,
            output: 100000,
            measured: true,
            floor: false,
            measured_tasks: 2,
            not_measured_tasks: 0,
          },
        },
      ],
      not_measured: false,
      not_measured_count: 1,
    },
    events: [
      { at: '2026-10-16T12:40:00Z', text: 'relay-auth-v2 asks a decision' },
      { at: '2026-10-16T12:05:00Z', text: 'relay-reconnect-backoff landed' },
    ],
  };

  const mockReview: ReviewResource = {
    review_id: 'R-0001',
    epic_id: 'relay-hardening',
    register: 'findings',
    decided: 2,
    total: 3,
    state: 'item',
    items: [
      {
        item_id: 'F-0004',
        register: 'findings',
        statement: 'Reconnect loses the last event',
        rules: ['a fix must hold for every transport'],
        origin: 'landing of relay-reconnect (#211)',
        trust: 'trusted',
        state: 'open',
        proposal: 'Promote to a plan',
      },
      {
        item_id: 'F-0005',
        register: 'findings',
        statement: 'The retry count is not bounded',
        rules: [],
        origin: 'review comment of #212',
        trust: 'untrusted',
        state: 'kept',
      },
    ],
  };

  it('renders the workstreams, sources, landings, registers, tokens, and events', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);

    expect(await screen.findByTestId('epic-workstreams')).toBeInTheDocument();
    expect(screen.getByTestId('workstream-WS-1')).toHaveTextContent('Reconnect');
    expect(screen.getByTestId(`workstream-WS-1`)).toHaveTextContent('WS-1');
    expect(screen.getByTestId('workstream-counts-WS-1')).toHaveTextContent('1 running · 2 staged');
    expect(screen.getByTestId('epic-row-relay-cursor-order')).toHaveTextContent(
      'relay-cursor-order',
    );
    expect(screen.getByTestId('epic-pr-relay-cursor-order')).toHaveTextContent('#211');
    expect(screen.getByTestId('epic-pr-relay-cursor-order')).toHaveAttribute(
      'href',
      'https://github.com/plan-marshall/pm-mcp-clients/pull/211',
    );
    expect(screen.getByTestId('wait-relay-docs')).toHaveTextContent(
      'relay-docs waits: overlap with relay-auth-v2',
    );
    expect(screen.getByTestId('epic-next-plan')).toHaveTextContent(
      'Next plan: relay-reconnect-metrics',
    );

    expect(screen.getByTestId('epic-sources')).toHaveTextContent('2 documents');
    expect(screen.getByTestId('epic-landing-count')).toHaveTextContent('2 landings');
    expect(screen.getByTestId('epic-landing-relay-cursor-order')).toHaveTextContent('#211');
    expect(
      screen.getByTestId('epic-landing-relay-cursor-order').querySelector('a'),
    ).toHaveAttribute('href', 'https://github.com/plan-marshall/pm-mcp-clients/pull/211');
    expect(screen.getByTestId('epic-findings')).toHaveTextContent('3 new · 4 open · 2 kept');
    expect(screen.getByTestId('epic-watches')).toHaveTextContent('4 open · 0 kept');

    expect(screen.getByTestId('epic-mode')).toHaveTextContent('Automatic: on');
    expect(screen.getByTestId('epic-tokens')).toHaveTextContent('≥ 3.86 M');
    expect(screen.getByTestId('epic-tokens-unmeasured')).toHaveTextContent('1 task not measured');
    expect(screen.getByTestId('epic-own-tokens')).toHaveTextContent('120 k');
    expect(screen.getByTestId('epic-plan-tokens-relay-reconnect-backoff')).toHaveTextContent(
      '1.40 M',
    );
    expect(screen.getByTestId('epic-events')).toHaveTextContent('relay-auth-v2 asks a decision');
  });

  it('opens the findings review from the register', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    const review = vi.spyOn(apiClient, 'getEpicReview').mockResolvedValue(mockReview);
    render(EpicView);

    await fireEvent.click(await screen.findByTestId('open-findings-review'));

    expect(review).toHaveBeenCalledWith('relay-hardening', 'findings');
    expect(await screen.findByTestId('review-modal')).toBeInTheDocument();
    expect(screen.getByTestId('review-title')).toHaveTextContent(
      'relay-hardening · findings review',
    );
    expect(screen.getByTestId('review-progress')).toHaveTextContent('2 of 3 decided');
    expect(screen.getByTestId('review-item-F-0004')).toHaveTextContent(
      'Reconnect loses the last event',
    );
  });

  it('marks an item from outside the machine as untrusted text', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    vi.spyOn(apiClient, 'getEpicReview').mockResolvedValue(mockReview);
    render(EpicView);

    await fireEvent.click(await screen.findByTestId('open-findings-review'));
    await fireEvent.click(await screen.findByTestId('review-item-F-0005'));

    expect(screen.getByTestId('review-untrusted')).toHaveTextContent('untrusted');
  });

  it('shows that no task is measured yet where the epic reports none', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue({
      ...mockResource,
      tokens: {
        total: {
          total: 0,
          input: 0,
          output: 0,
          measured: false,
          floor: false,
          measured_tasks: 0,
          not_measured_tasks: 0,
        },
        own: {
          total: 0,
          input: 0,
          output: 0,
          measured: false,
          floor: false,
          measured_tasks: 0,
          not_measured_tasks: 0,
        },
        plans: [],
        not_measured: true,
        not_measured_count: 0,
      },
    });
    render(EpicView);

    expect(await screen.findByTestId('epic-tokens-not-measured')).toHaveTextContent(
      'no task measured yet',
    );
  });

  it('folds and unfolds a workstream, keeping its counts in the folded row', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);

    await screen.findByTestId('workstream-WS-1');
    expect(screen.getByTestId('epic-row-relay-cursor-order')).toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('workstream-toggle-WS-1'));

    expect(screen.queryByTestId('epic-row-relay-cursor-order')).not.toBeInTheDocument();
    expect(screen.getByTestId('workstream-counts-WS-1')).toHaveTextContent('1 running · 2 staged');

    await fireEvent.click(screen.getByTestId('workstream-toggle-WS-1'));
    expect(screen.getByTestId('epic-row-relay-cursor-order')).toBeInTheDocument();
  });

  it('writes an event at its wall time, not as an ISO timestamp', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);

    const events = await screen.findByTestId('epic-events');
    expect(events).toHaveTextContent('relay-auth-v2 asks a decision');
    expect(events).not.toHaveTextContent('2026-10-16T12:40:00Z');
  });

  it('says that an epic has no sources instead of borrowing the plan-empty text', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue({ ...mockResource, sources: [] });
    render(EpicView);

    expect(await screen.findByTestId('epic-sources-empty')).toHaveTextContent('no sources');
  });

  it('maps a two-word wire status to its catalogue key and badge class', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue({
      ...mockResource,
      workstreams: [
        {
          id: 'WS-1',
          name: 'Reconnect',
          open: true,
          rows: [
            { plan_id: 'relay-docs', title: 'relay-docs', status: 'handed back' },
            {
              plan_id: 'relay-auth-v2',
              title: 'relay-auth-v2',
              status: 'waiting for you',
            },
          ],
        },
      ],
    });
    render(EpicView);

    const handedBack = await screen.findByTestId('epic-row-relay-docs');
    expect(handedBack.querySelector('.badge--state')).toHaveClass('badge--state-handed_back');
    expect(handedBack).toHaveTextContent('handed back');
    expect(screen.getByTestId('epic-row-relay-auth-v2').querySelector('.badge--state')).toHaveClass(
      'badge--state-waiting',
    );
    expect(screen.getByTestId('workstream-counts-WS-1')).toHaveTextContent('1 waiting for you');
  });

  it('marks an ended epic with how it ended', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue({
      ...mockResource,
      state: 'ended',
      terminal_reason: 'completed',
    });
    render(EpicView);

    expect(await screen.findByTestId('epic-state')).toHaveTextContent('ended');
    expect(screen.getByTestId('epic-end')).toHaveTextContent('completed');
  });

  it('reloads the epic on a row change and ignores an event it does not show', async () => {
    let deliver: ((event: string) => void) | null = null;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((onEvent) => {
      deliver = onEvent as (event: string) => void;
      return () => {};
    });
    const resource = vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);

    await screen.findByTestId('epic-title');
    expect(resource).toHaveBeenCalledTimes(1);

    deliver!('epic.row_changed');
    await waitFor(() => expect(resource).toHaveBeenCalledTimes(2));

    deliver!('stream.closed');
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(resource).toHaveBeenCalledTimes(2);
  });

  it('updates the event list when a live event arrives', async () => {
    let deliver: ((event: string) => void) | null = null;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((onEvent) => {
      deliver = onEvent as (event: string) => void;
      return () => {};
    });
    const resource = vi
      .spyOn(apiClient, 'getEpicResource')
      .mockResolvedValueOnce(mockResource)
      .mockResolvedValueOnce({
        ...mockResource,
        events: [
          { at: '2026-10-16T12:45:00Z', text: 'relay-auth-v2 asks a decision' },
          { at: '2026-10-16T12:41:00Z', text: 'relay-reconnect-metrics started (3 of 3 at once)' },
        ],
      });
    render(EpicView);

    await screen.findByTestId('epic-title');
    deliver!('epic.row_changed');

    await waitFor(() =>
      expect(screen.getByTestId('epic-events')).toHaveTextContent(
        'relay-reconnect-metrics started (3 of 3 at once)',
      ),
    );
    expect(resource).toHaveBeenCalledTimes(2);
  });
});
