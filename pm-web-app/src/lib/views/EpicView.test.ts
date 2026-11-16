import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import EpicView from './EpicView.svelte';
import { apiClient, type EpicProcess, type EpicResource, type ReviewResource } from '../api/client';
import { ClientError } from '../api/errors';
import { runtimeStore } from '../stores/runtime';
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
            plan_id: 'relay-reconnect-backoff',
            title: 'Reconnect backoff',
            status: 'shipped',
            pull_request_number: 211,
            pull_request_url: 'https://github.com/plan-marshall/pm-mcp-clients/pull/211',
          },
          {
            // A running row that carries a pull request is no landing: only a shipped row is.
            plan_id: 'relay-cursor-order',
            title: 'Cursor order',
            status: 'running',
            pull_request_number: 300,
            pull_request_url: 'https://github.com/plan-marshall/pm-mcp-clients/pull/300',
          },
          {
            plan_id: 'relay-docs',
            title: 'Relay docs',
            status: 'staged',
            wait_reason: 'overlap',
            wait_plan_id: 'relay-auth-v2',
            wait_since: '2026-10-16T09:40:00Z',
            overlap_files: ['pm-api/src/store/Docs.java'],
          },
          {
            plan_id: 'relay-reconnect-metrics',
            title: 'Reconnect metrics',
            status: 'staged',
            wait_reason: 'none',
          },
        ],
      },
    ],
    sources: ['2 documents', '1 issue'],
    // The resource's own landing counter disagrees with the rows on purpose: the view counts the
    // rows it lists, never the served figure (deliverable 3).
    landings: 9,
    findings: { new: 0, open: 2, kept: 1 },
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
    decided: 1,
    total: 3,
    state: 'item',
    items: [
      {
        item_id: 'F-0003',
        register: 'findings',
        statement: 'The retry count is not bounded',
        rules: [],
        origin: 'review comment of #212',
        trust: 'trusted',
        state: 'kept',
      },
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
        statement: 'The cursor order is asserted too weakly',
        rules: [],
        origin: 'review comment of #213',
        trust: 'untrusted',
        state: 'open',
      },
    ],
  };

  beforeEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState(null, '', '/epics/relay-hardening');
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
    vi.spyOn(apiClient, 'getEpicIntent').mockResolvedValue({
      epic_id: 'relay-hardening',
      destination: 'the relay reconnects without losing an event',
      done_when: [],
      non_goals: [],
      not_yet_specified: [],
      plans: [],
    });
  });

  it('draws the error pattern when the epic process cannot be read', async () => {
    const process = vi
      .spyOn(apiClient, 'getEpicProcess')
      .mockRejectedValue(new ClientError('network_offline'));

    render(EpicView);

    const error = await screen.findByTestId('epic-error');
    expect(error).toHaveTextContent('The machine is not reachable');
    await fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
    await waitFor(() => expect(process).toHaveBeenCalledTimes(2));
  });

  it('shows the error of a failed epic-resource read, retries it, and stops claiming the read runs', async () => {
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue({
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      actions: [{ action: 'next', available: true }],
    });
    const epicResource = vi
      .spyOn(apiClient, 'getEpicResource')
      .mockRejectedValueOnce(new ClientError('network_offline'))
      .mockResolvedValueOnce(mockResource);

    render(EpicView);

    const error = await screen.findByTestId('epic-resource-error');
    expect(error).toHaveTextContent('The machine is not reachable');
    expect(error).toBeInTheDocument();
    // The bar says the read failed rather than that the epic is still being read.
    const next = await screen.findByTestId('action-next');
    expect(next).toHaveAttribute(
      'title',
      'The epic could not be read; try again before the next plan starts.',
    );
    expect(next.getAttribute('title')).not.toContain('still being read');

    await fireEvent.click(screen.getByRole('button', { name: 'Try again' }));

    expect(await screen.findByTestId('epic-workstreams')).toBeInTheDocument();
    expect(screen.queryByTestId('epic-resource-error')).not.toBeInTheDocument();
    expect(epicResource).toHaveBeenCalledTimes(2);
  });

  it('renders the epic head on the object page: kind, title, id chip, state and tokens', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);

    expect(await screen.findByTestId('epic-title')).toHaveTextContent('relay-hardening');
    expect(screen.getByTestId('object-page')).toBeInTheDocument();
    expect(screen.getByTestId('object-page-kind')).toHaveTextContent('epic');
    expect(screen.getByTestId('object-page-id')).toHaveTextContent('relay-hardening');
    // The head draws no path: the top bar's breadcrumb is the only one (`navigation.adoc#up`).
    expect(screen.queryByTestId('epic-breadcrumb')).not.toBeInTheDocument();
    expect(screen.queryByTestId('back-btn')).not.toBeInTheDocument();

    expect(screen.getByTestId('epic-state')).toHaveTextContent('running');
    expect(screen.getByTestId('review-ready-badge')).toHaveTextContent(
      /completion review is ready/i,
    );
    expect(screen.getByTestId('epic-tokens')).toHaveTextContent(/3.86 M/);
  });

  it('draws the workstream plans: title first, the id as a chip, the state, the wait and its since', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);

    const row = await screen.findByTestId('epic-row-relay-docs');
    // The plan's title comes first — here a title that differs from its id, so the order shows.
    expect(row.textContent?.trim()).toMatch(/^Relay docs/);
    expect(screen.getByTestId('epic-plan-chip-relay-docs')).toHaveTextContent('relay-docs');
    expect(screen.getByTestId('epic-plan-status-relay-docs')).toHaveTextContent('staged');
    expect(screen.getByTestId('wait-relay-docs')).toHaveTextContent(
      'relay-docs waits: overlap with relay-auth-v2',
    );

    const since = screen.getByTestId('wait-since-relay-docs');
    expect(since).toHaveAttribute('datetime', '2026-10-16T09:40:00Z');
    expect(since.textContent ?? '').toMatch(/last|ago|since|\d/);

    expect(screen.getByTestId('epic-pr-relay-reconnect-backoff')).toHaveTextContent('#211');
    expect(screen.getByTestId('epic-pr-relay-reconnect-backoff')).toHaveAttribute(
      'href',
      'https://github.com/plan-marshall/pm-mcp-clients/pull/211',
    );
    expect(screen.getByTestId('epic-next-plan')).toHaveTextContent(
      'Next plan: relay-reconnect-metrics',
    );
  });

  it('counts the landings from the shipped rows it lists, not from the served figure', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('epic-title');

    await fireEvent.click(screen.getByRole('tab', { name: 'Landings' }));

    // Two rows carry a pull request; only the shipped one is a landing.
    expect(await screen.findByTestId('epic-landing-count')).toHaveTextContent('1 landing');
    expect(screen.getByTestId('epic-landing-relay-reconnect-backoff')).toHaveTextContent('#211');
    expect(
      screen.getByTestId('epic-landing-relay-reconnect-backoff').querySelector('a'),
    ).toHaveAttribute('href', 'https://github.com/plan-marshall/pm-mcp-clients/pull/211');
    expect(screen.queryByTestId('epic-landing-relay-cursor-order')).not.toBeInTheDocument();
  });

  it('draws the landings that stand alongside the epic beside its landings', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('epic-title');

    await fireEvent.click(screen.getByRole('tab', { name: 'Landings' }));

    const alongside = await screen.findByTestId('epic-alongside');
    const items = screen.getAllByTestId('alongside-item');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent('landing doc/update.md · done');
    expect(alongside).toBeInTheDocument();
  });

  it('draws five tabs with Plans as the default and no Tokens tab', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('epic-title');

    const tabs = screen.getAllByRole('tab').map((tab) => tab.textContent?.trim());
    expect(tabs).toEqual(['Plans', 'Intent', 'Landings', 'Reviews', 'Events']);
    expect(tabs).not.toContain('Tokens');

    expect(screen.getByRole('tab', { name: 'Plans' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: 'Plans' })).toHaveAttribute(
      'aria-controls',
      'epic-panel-plans',
    );
    // Only the shown tab names a panel: the others' panels are not in the document.
    expect(screen.getByRole('tab', { name: 'Intent' })).not.toHaveAttribute('aria-controls');
    expect(screen.getByTestId('epic-panel-plans')).toBeInTheDocument();
    // The tokens are the head line with its one-step disclosure, never a tab.
    expect(screen.getByTestId('epic-tokens')).toBeInTheDocument();
  });

  it('reveals the tokens line plan by plan in one step', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('epic-title');

    expect(screen.getByTestId('epic-tokens')).toHaveTextContent('≥ 3.86 M');
    expect(screen.getByTestId('epic-tokens')).toHaveTextContent('3.52 M in');
    expect(screen.getByTestId('epic-tokens')).toHaveTextContent('0.34 M out');
    expect(screen.getByTestId('epic-tokens-unmeasured')).toHaveTextContent('1 task not measured');
    expect(screen.queryByTestId('epic-plan-tokens-relay-reconnect-backoff')).not.toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('epic-tokens-disclosure'));

    expect(screen.getByTestId('epic-own-tokens')).toHaveTextContent('120 k');
    expect(screen.getByTestId('epic-plan-tokens-relay-reconnect-backoff')).toHaveTextContent(
      '1.40 M',
    );
  });

  it('writes the chosen tab into the address by a replace', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('epic-title');
    const entriesBefore = window.history.length;

    await fireEvent.click(screen.getByRole('tab', { name: 'Intent' }));

    expect(window.location.pathname).toBe('/epics/relay-hardening');
    expect(window.location.search).toBe('?tab=intent');
    // A tab is not a level: it replaces the entry instead of adding one.
    expect(window.history.length).toBe(entriesBefore);
    expect(get(runtimeStore).objectTab).toBe('intent');
  });

  it('opens the Intent tab with the sources first and the intent panel below', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('epic-title');

    await fireEvent.click(screen.getByRole('tab', { name: 'Intent' }));

    const panel = await screen.findByTestId('epic-panel-intent');
    const sources = screen.getByTestId('epic-intent-sources');
    expect(sources).toHaveTextContent('2 documents');
    expect(await screen.findByTestId('epic-intent-panel')).toBeInTheDocument();
    // The sources stand before the intent panel in the same tab.
    expect(panel.textContent?.indexOf('2 documents')).toBeLessThan(
      panel.textContent?.indexOf('the relay reconnects without losing an event') ?? -1,
    );
  });

  it('shows the Reviews tab counts and its `! n` badge while findings are new', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue({
      ...mockResource,
      findings: { new: 3, open: 4, kept: 2 },
    });
    render(EpicView);
    await screen.findByTestId('epic-title');

    const reviews = screen.getByRole('tab', { name: /Reviews/ });
    expect(reviews).toHaveTextContent('3');
    expect(reviews.querySelector('[data-testid="count-badge"]')).toHaveTextContent('! 3');

    await fireEvent.click(reviews);

    expect(await screen.findByTestId('epic-findings')).toHaveTextContent('3 new · 4 open · 2 kept');
    expect(screen.getByTestId('epic-watches')).toHaveTextContent('4 open · 0 kept');
  });

  it('leaves the Reviews tab without a badge where no finding is new', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('epic-title');

    const reviews = screen.getByRole('tab', { name: /Reviews/ });
    expect(reviews.querySelector('[data-testid="count-badge"]')).not.toBeInTheDocument();
  });

  it('opens the findings review on its first open item, with the counts the card shows', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    const review = vi.spyOn(apiClient, 'getEpicReview').mockResolvedValue(mockReview);
    render(EpicView);
    await screen.findByTestId('epic-title');

    await fireEvent.click(screen.getByRole('tab', { name: /Reviews/ }));
    await fireEvent.click(await screen.findByTestId('open-findings-review'));

    expect(review).toHaveBeenCalledWith('relay-hardening', 'findings');
    expect(await screen.findByTestId('review-modal')).toBeInTheDocument();
    expect(screen.getByTestId('review-title')).toHaveTextContent(
      'relay-hardening · findings review',
    );
    // F-0003 is decided; the review opens on F-0004, the first item still open.
    expect(screen.getByTestId('review-statement')).toHaveTextContent(
      'Reconnect loses the last event',
    );
    expect(screen.getByTestId('review-progress')).toHaveTextContent('1 of 3 decided');
    // The modal's open figure is the card's `open` figure.
    expect(screen.getByTestId('review-open-count')).toHaveTextContent('2 open');
    expect(screen.getByTestId('epic-findings')).toHaveTextContent('0 new · 2 open · 1 kept');
  });

  it('marks an item from outside the machine as untrusted text', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    vi.spyOn(apiClient, 'getEpicReview').mockResolvedValue(mockReview);
    render(EpicView);
    await screen.findByTestId('epic-title');

    await fireEvent.click(screen.getByRole('tab', { name: /Reviews/ }));
    await fireEvent.click(await screen.findByTestId('open-findings-review'));
    await fireEvent.click(screen.getByTestId('review-item-F-0005'));

    expect(screen.getByTestId('review-untrusted')).toHaveTextContent('untrusted');
  });

  it('runs the epic chat open, send, reply and proposal accept on B1 fixtures', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    vi.spyOn(apiClient, 'getEpicReview').mockResolvedValue(mockReview);
    const chatReadBack = {
      chat_id: 'C-0005',
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      role: 'decompose',
      scope: 'epic',
      state: 'chat.read-back',
      turns: [
        {
          role: 'operator',
          text: 'Split the reconnect work',
          at: '2026-10-16T12:00:00Z',
          untrusted: false,
        },
      ],
      proposal: {
        turn_id: 'T-0500',
        summary: 'Two plans',
        after: 'split the epic in two plans',
        untrusted: false,
      },
    };
    const chat = vi.spyOn(apiClient, 'getChat').mockResolvedValue(chatReadBack);
    const accept = vi.spyOn(apiClient, 'answerSubjectChatProposal').mockResolvedValue({
      ...chatReadBack,
      proposal: null,
    });
    render(EpicView);
    await screen.findByTestId('epic-title');

    await fireEvent.click(screen.getByRole('tab', { name: /Reviews/ }));
    await fireEvent.click(await screen.findByTestId('open-findings-review'));
    await fireEvent.click(screen.getByTestId('review-chat'));

    await waitFor(() => expect(chat).toHaveBeenCalledWith('epic:relay-hardening'));
    await fireEvent.click(await screen.findByTestId('chat-accept'));
    await waitFor(() => {
      expect(accept).toHaveBeenCalledWith('epic:relay-hardening', 'T-0500', true);
    });
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

    expect(await screen.findByTestId('epic-tokens')).toHaveTextContent('not measured');
  });

  it('folds and unfolds a workstream, keeping its counts in the folded row', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);

    await screen.findByTestId('workstream-WS-1');
    expect(screen.getByTestId('epic-row-relay-cursor-order')).toBeInTheDocument();

    await fireEvent.click(screen.getByTestId('workstream-toggle-WS-1'));

    expect(screen.queryByTestId('epic-row-relay-cursor-order')).not.toBeInTheDocument();
    expect(screen.getByTestId('workstream-counts-WS-1')).toHaveTextContent(
      '1 shipped · 1 running · 2 staged',
    );

    await fireEvent.click(screen.getByTestId('workstream-toggle-WS-1'));
    expect(screen.getByTestId('epic-row-relay-cursor-order')).toBeInTheDocument();
  });

  it('writes an event at its wall time, not as an ISO timestamp', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('epic-title');

    await fireEvent.click(screen.getByRole('tab', { name: 'Events' }));

    const events = await screen.findByTestId('epic-events');
    expect(events).toHaveTextContent('relay-auth-v2 asks a decision');
    expect(events).not.toHaveTextContent('2026-10-16T12:40:00Z');
  });

  it('says that an epic has no sources instead of borrowing the plan-empty text', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue({ ...mockResource, sources: [] });
    render(EpicView);
    await screen.findByTestId('epic-title');

    await fireEvent.click(screen.getByRole('tab', { name: 'Intent' }));

    expect(await screen.findByTestId('epic-sources-empty')).toHaveTextContent('no sources');
  });

  it('maps a two-word wire status to its catalogue key and dot', async () => {
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
            // The queue's own word for a landed plan is `merged`: the row reads and counts as shipped.
            { plan_id: 'relay-landed', title: 'relay-landed', status: 'merged' },
          ],
        },
      ],
    });
    render(EpicView);

    const handedBack = await screen.findByTestId('epic-plan-status-relay-docs');
    expect(handedBack).toHaveTextContent('handed back');
    expect(screen.getByTestId('epic-plan-status-relay-auth-v2')).toHaveTextContent(
      'waiting for you',
    );
    expect(screen.getByTestId('epic-plan-status-relay-landed')).toHaveTextContent('shipped');
    expect(screen.getByTestId('workstream-counts-WS-1')).toHaveTextContent(
      '1 shipped · 1 waiting for you',
    );
  });

  it('marks an ended epic with how it ended, and keeps its tabs read-only', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue({
      ...mockResource,
      state: 'ended',
      terminal_reason: 'completed',
    });
    render(EpicView);

    expect(await screen.findByTestId('epic-state')).toHaveTextContent('ended');
    expect(screen.getByTestId('epic-end')).toHaveTextContent('completed');
    // The same five tabs, read-only: no act of the running epic's bar, and no way into a review
    // that would offer the review's acts (`epic-view.adoc#tabs`).
    expect(screen.getAllByRole('tab')).toHaveLength(5);
    expect(screen.queryByTestId('action-next')).not.toBeInTheDocument();

    await fireEvent.click(screen.getByRole('tab', { name: /Reviews/ }));

    expect(await screen.findByTestId('epic-findings')).toHaveTextContent('0 new · 2 open · 1 kept');
    expect(screen.queryByTestId('open-findings-review')).not.toBeInTheDocument();
    expect(screen.queryByTestId('open-watches-review')).not.toBeInTheDocument();
  });

  it('draws the list-column form with only the plans grouped by workstream', async () => {
    runtimeStore.update((state) => ({
      ...state,
      openObject: { kind: 'plan', id: 'relay-cursor-order' },
    }));
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);

    expect(await screen.findByTestId('epic-list-column')).toBeInTheDocument();
    expect(screen.getByTestId('workstream-WS-1')).toHaveTextContent('Reconnect');
    expect(screen.getByTestId('epic-row-relay-cursor-order')).toBeInTheDocument();
    // Nothing of the other tabs: no tab list, no tokens line, no panel.
    expect(screen.queryAllByRole('tab')).toHaveLength(0);
    expect(screen.queryByTestId('epic-tokens')).not.toBeInTheDocument();
    expect(screen.queryByTestId('epic-panel-plans')).not.toBeInTheDocument();
    // Its head is the compact one.
    expect(screen.getByTestId('object-page')).toBeInTheDocument();
    expect(screen.queryByTestId('object-page-id')).not.toBeInTheDocument();
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
    // The subscription handler and the reload it would start both run synchronously, so the
    // count right after the delivery is decisive: a refresh would already have called the API.
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
    await fireEvent.click(screen.getByRole('tab', { name: 'Events' }));
    deliver!('epic.row_changed');

    await waitFor(() =>
      expect(screen.getByTestId('epic-events')).toHaveTextContent(
        'relay-reconnect-metrics started (3 of 3 at once)',
      ),
    );
    expect(resource).toHaveBeenCalledTimes(2);
  });

  /** @param value the filter text */
  async function typeFilter(value: string): Promise<void> {
    const input = await screen.findByTestId('filter-input');
    await fireEvent.input(input, { target: { value } });
  }

  it('filters the workstream plans by plan, status, and workstream name', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('workstream-WS-1');
    expect(screen.getAllByTestId(/epic-row-/)).toHaveLength(4);

    await typeFilter('relay-docs');
    expect(screen.getAllByTestId(/epic-row-/)).toHaveLength(1);
    expect(screen.getByTestId('epic-row-relay-docs')).toBeInTheDocument();
    expect(screen.getByTestId('workstream-counts-WS-1')).toHaveTextContent('1 staged');

    await typeFilter('running');
    expect(screen.getAllByTestId(/epic-row-/)).toHaveLength(1);
    expect(screen.getByTestId('epic-row-relay-cursor-order')).toBeInTheDocument();

    await typeFilter('Reconnect');
    expect(screen.getAllByTestId(/epic-row-/)).toHaveLength(4);
  });

  it('filters the workstream plans by a wait reason and its overlap file', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('workstream-WS-1');

    await typeFilter('overlap');
    expect(screen.getAllByTestId(/epic-row-/)).toHaveLength(1);
    expect(screen.getByTestId('epic-row-relay-docs')).toBeInTheDocument();

    await typeFilter('Docs.java');
    expect(screen.getAllByTestId(/epic-row-/)).toHaveLength(1);
    expect(screen.getByTestId('epic-row-relay-docs')).toBeInTheDocument();
  });

  it('shows a no-match state and drops the empty workstream', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('workstream-WS-1');

    await typeFilter('nothing-matches-this');

    expect(await screen.findByTestId('epic-filter-empty')).toHaveTextContent(
      'Nothing matches the filter.',
    );
    expect(screen.queryByTestId('workstream-WS-1')).not.toBeInTheDocument();
  });

  it('opens a folded workstream while a filter is active', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('workstream-WS-1');
    await fireEvent.click(screen.getByTestId('workstream-toggle-WS-1'));
    expect(screen.queryByTestId('epic-row-relay-docs')).not.toBeInTheDocument();

    await typeFilter('relay-docs');

    expect(screen.getByTestId('epic-row-relay-docs')).toBeInTheDocument();
  });

  it('keeps a folded workstream folded for a query of punctuation alone, which is no filter', async () => {
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockResource);
    render(EpicView);
    await screen.findByTestId('workstream-WS-1');
    await fireEvent.click(screen.getByTestId('workstream-toggle-WS-1'));
    expect(screen.queryByTestId('epic-row-relay-docs')).not.toBeInTheDocument();

    await typeFilter('...');

    expect(screen.getByTestId('workstream-WS-1')).toBeInTheDocument();
    expect(screen.queryByTestId('epic-row-relay-docs')).not.toBeInTheDocument();
  });

  it('opens the raise dialogue with the unknown count text when the epic resource omits running', async () => {
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue({
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      actions: [{ action: 'next', available: true }],
    });
    const resourceWithoutRunning: EpicResource = {
      ...mockResource,
      running: null,
    };
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(resourceWithoutRunning);
    render(EpicView);

    expect(await screen.findByTestId('epic-scope')).toHaveTextContent('? of 3 at once');

    const next = await screen.findByTestId('action-next');
    expect(next).not.toBeDisabled();

    await fireEvent.click(next);

    const raise = await screen.findByTestId('raise-scope');
    expect(raise).toHaveTextContent(
      'How many plans run is not known; relay-hardening may run 3 at once, and the next plan, relay-reconnect-metrics, waits for a free place.',
    );
  });

  it('renders the unknown count badge when running is undefined and opens the raise dialogue', async () => {
    vi.spyOn(apiClient, 'getEpicActions').mockResolvedValue({
      subject_id: 'relay-hardening',
      subject_kind: 'epic',
      state: 'running',
      actions: [{ action: 'next', available: true }],
    });
    const resourceWithUndefinedRunning: EpicResource = {
      ...mockResource,
    };
    delete resourceWithUndefinedRunning.running;
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(resourceWithUndefinedRunning);
    render(EpicView);

    expect(await screen.findByTestId('epic-scope')).toHaveTextContent('? of 3 at once');

    const next = await screen.findByTestId('action-next');
    expect(next).not.toBeDisabled();

    await fireEvent.click(next);

    const raise = await screen.findByTestId('raise-scope');
    expect(raise).toHaveTextContent(
      'How many plans run is not known; relay-hardening may run 3 at once, and the next plan, relay-reconnect-metrics, waits for a free place.',
    );
  });
});
