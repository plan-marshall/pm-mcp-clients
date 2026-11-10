import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import CompletionReportView from './CompletionReportView.svelte';
import { apiClient, type PlanCompletionReport } from '../api/client';

describe('CompletionReportView component', () => {
  const mockReport: PlanCompletionReport = {
    plan_id: 'fix-relay',
    title: 'fix-relay',
    from: { project: 'pm-mcp-clients', epic: 'relay-hardening', plan: 'fix-relay' },
    outcome: 'merged',
    terminal_reason: 'the pull request #214 was merged into main',
    pull_request_number: 214,
    pull_request_url: 'https://github.com/plan-marshall/pm-mcp-clients/pull/214',
    reason: '—',
    started_at: '2026-10-16T08:40:00Z',
    ended_at: '2026-10-16T11:20:00Z',
    ended: 'today 11:20',
    duration_seconds: 9600,
    paused_seconds: 1200,
    paused_by: 'you',
    ran: '2 h 40 min, 08:40 to 11:20 · paused 20 min by you',
    deliverables: [
      { name: 'cursor write order', state: 'done' },
      { name: 'a test per transport, websocket and SSE', state: 'done' },
      { name: 'fsync of the cursor file', state: 'done' },
    ],
    replanning_records: [{ trigger: 'the base moved', time: '09:55', change: 'TASK-5 added' }],
    decision_records: [
      {
        id: 'Q-003',
        question: 'Rebase onto main',
        answer: 'rebase',
        by: 'operator',
        channel: 'web app',
      },
      { id: 'Q-005', question: '', answer: '', by: 'the plan' },
    ],
    own_calls: [
      {
        kind: 'shortcut',
        description: 'reuse the existing SSE adapter',
        limit: 'the single-subscriber path only',
        ends_when: 'a second subscriber connects',
        judgment: 'conflicts',
        conflict_target: 'a test per transport, websocket and SSE',
      },
      {
        kind: 'design',
        description: 'the relay writes the cursor',
        judgment: 'consistent',
      },
    ],
    change_map: {
      diagram: 'relay ──> cursor ──> fsync\n  └────> sse ──────> websocket (not traced)',
      state: 'partial',
      cause: 'the vendored tree is not tracked',
    },
    change: {
      files: 12,
      insertions: 340,
      deletions: 27,
      unplanned: 2,
    },
    token_usage: {
      total: 412000,
      input: 380000,
      output: 32000,
      duration_seconds: 9600,
      workers_count: 6,
      models_count: 3,
      tasks_measured: 38,
      tasks_total: 41,
      is_floor: true,
      unmeasured_reason: 'their worker was terminated',
      by_phase: [
        {
          name: 'refine',
          total: 38000,
          input: 35000,
          output: 3000,
          unmeasured: 0,
          floor: false,
          components: {
            input: 25000,
            cache_read: 10000,
            cache_write: 0,
            output: 2500,
            reasoning: 500,
          },
        },
      ],
      by_role: [
        {
          name: 'planner',
          harness_model: 'claude:opus-5.5',
          effort: 'high',
          total: 128000,
          input: 120000,
          output: 8000,
          unmeasured: 0,
          floor: false,
        },
      ],
      by_model: [
        {
          name: 'claude:opus-5.5',
          total: 128000,
          input: 120000,
          output: 8000,
          unmeasured: 0,
          floor: false,
        },
      ],
      waste_records: [
        {
          waste_class: 'no_product',
          tokens: 18000,
          share_percent: 4,
          tasks_count: 2,
          cause_summary: '2 tasks',
        },
      ],
      total_waste_tokens: 52000,
      waste_share_percent: 13,
    },
  };

  beforeEach(() => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(mockReport);
  });

  it('renders completion report details, deliverables, and decisions', async () => {
    render(CompletionReportView, { planId: 'fix-relay', report: mockReport });

    expect(screen.getAllByText('fix-relay').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('merged')).toBeInTheDocument();
    expect(screen.getByText('#214')).toBeInTheDocument();
    expect(screen.getByText('the pull request #214 was merged into main')).toBeInTheDocument();
    expect(
      screen.getByText('2 h 40 min, 08:40 to 11:20 · paused 20 min by you'),
    ).toBeInTheDocument();

    // Deliverables
    expect(screen.getByText('3 of 3 done')).toBeInTheDocument();
    expect(screen.getByText('cursor write order')).toBeInTheDocument();

    // Re-planning
    expect(screen.getByText('the base moved')).toBeInTheDocument();
    expect(screen.getByText(': TASK-5 added')).toBeInTheDocument();

    // Decisions
    expect(screen.getByText('Q-003')).toBeInTheDocument();
    expect(screen.getByText('Rebase onto main')).toBeInTheDocument();
    expect(screen.getByText('(you, web app)')).toBeInTheDocument();
  });

  it('renders own calls with their limits and self-review judgments', async () => {
    render(CompletionReportView, { planId: 'fix-relay', report: mockReport });

    expect(screen.getByText('Own calls')).toBeInTheDocument();
    expect(screen.getByText('shortcut')).toBeInTheDocument();
    expect(screen.getByText('reuse the existing SSE adapter')).toBeInTheDocument();
    expect(screen.getByText('limit the single-subscriber path only')).toBeInTheDocument();
    expect(screen.getByText('ends when a second subscriber connects')).toBeInTheDocument();
    expect(
      screen.getByText('conflicts with a test per transport, websocket and SSE'),
    ).toBeInTheDocument();
    expect(screen.getByText('design call')).toBeInTheDocument();
    expect(screen.getByText('the relay writes the cursor')).toBeInTheDocument();
    expect(screen.getByText('consistent')).toBeInTheDocument();
  });

  it('renders the change map as delivered in a fixed-width block', async () => {
    render(CompletionReportView, { planId: 'fix-relay', report: mockReport });

    expect(screen.getByText('Change map')).toBeInTheDocument();
    expect(screen.getByText('partial')).toBeInTheDocument();
    expect(screen.getByText('cause: the vendored tree is not tracked')).toBeInTheDocument();

    const diagram = screen.getByText(/relay ──> cursor/);
    expect(diagram.tagName).toBe('PRE');
    expect(diagram.textContent).toBe(
      'relay ──> cursor ──> fsync\n  └────> sse ──────> websocket (not traced)',
    );
  });

  it('shows the size of the change with its unplanned count', async () => {
    render(CompletionReportView, { planId: 'fix-relay', report: mockReport });

    expect(screen.getByText('Change')).toBeInTheDocument();
    expect(screen.getByText('12 files · +340 −27 · 2 unplanned')).toBeInTheDocument();
  });

  it('leaves the Change line out when the plan carries no measured size', async () => {
    render(CompletionReportView, { planId: 'fix-relay', report: { ...mockReport, change: null } });

    expect(screen.queryByText('Change')).toBeNull();
    expect(screen.queryByText(/files · \+/)).toBeNull();
  });

  it('leaves the unplanned part out when the change carries no unplanned count', async () => {
    render(CompletionReportView, {
      planId: 'fix-relay',
      report: { ...mockReport, change: { files: 12, insertions: 340, deletions: 27 } },
    });

    expect(screen.getByText('12 files · +340 −27')).toBeInTheDocument();
    expect(screen.queryByText(/unplanned/)).toBeNull();
  });

  it('renders tokens section with coverage, breakdown, and toggles raw components', async () => {
    render(CompletionReportView, { planId: 'fix-relay', report: mockReport });

    expect(screen.getByText('Tokens')).toBeInTheDocument();
    expect(
      screen.getByText(/3 of 41 tasks not measured: their worker was terminated/),
    ).toBeInTheDocument();

    // By phase
    expect(screen.getByRole('heading', { level: 3, name: 'By phase' })).toBeInTheDocument();
    expect(screen.getByText('refine')).toBeInTheDocument();
    // By role
    expect(screen.getByRole('heading', { level: 3, name: 'By role' })).toBeInTheDocument();
    expect(screen.getByText('planner')).toBeInTheDocument();
    expect(screen.getByText('claude:opus-5.5 · high')).toBeInTheDocument();
    // By model
    expect(screen.getByRole('heading', { level: 3, name: 'By model' })).toBeInTheDocument();
    expect(screen.getByText('claude:opus-5.5 · high')).toBeInTheDocument();

    // Waste
    expect(screen.getByText(/52 k tokens/)).toBeInTheDocument();
    expect(screen.getByText(/(13 % of measured)/)).toBeInTheDocument();
    expect(screen.getByText('produced nothing')).toBeInTheDocument();

    // Raw components toggle
    const toggleBtn = screen.getByRole('button', { name: /Show raw components/i });
    await fireEvent.click(toggleBtn);

    expect(screen.getByText(/cache read 10 k/)).toBeInTheDocument();
  });

  it('renders breakdown titles as h4 when titleLevel is 2', () => {
    render(CompletionReportView, { planId: 'fix-relay', report: mockReport, titleLevel: 2 });

    expect(screen.getByRole('heading', { level: 3, name: 'Tokens' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 4, name: 'By phase' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 4, name: 'By role' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 4, name: 'By model' })).toBeInTheDocument();
  });

  it('renders exactly one h1 in loading state when titleLevel is 1 (D3)', () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockImplementation(() => new Promise(() => {}));
    render(CompletionReportView, { planId: 'fix-relay', titleLevel: 1 });

    const h1Elements = document.querySelectorAll('h1');
    expect(h1Elements).toHaveLength(1);
    expect(h1Elements[0]).toHaveTextContent(/Loading completion report/i);
  });

  it('renders exactly one h1 in error state when titleLevel is 1 (D3)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockRejectedValue(new Error('Fetch failed'));
    render(CompletionReportView, { planId: 'fix-relay', titleLevel: 1 });

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    const h1Elements = document.querySelectorAll('h1');
    expect(h1Elements).toHaveLength(1);
    expect(h1Elements[0]).toHaveTextContent(/The runtime refused the request/i);
  });

  it('compares each heading with the previous level across a full report at both titleLevel 1 and 2 (D3)', () => {
    // Narrow layout (titleLevel 1)
    const { unmount } = render(CompletionReportView, {
      planId: 'fix-relay',
      report: mockReport,
      titleLevel: 1,
    });

    const checkNoSkippedLevels = () => {
      const headings = Array.from(document.querySelectorAll('h1, h2, h3, h4, h5, h6'));
      expect(headings.length).toBeGreaterThan(0);
      // Each heading is compared with the one before it: a heading may go shallower freely, but it
      // may not skip a level deeper than its predecessor (an h2 followed by an h4 skips the h3
      // parent, which a running maximum would let through).
      let previousLevel = parseInt(headings[0].tagName[1], 10);
      for (let index = 1; index < headings.length; index++) {
        const level = parseInt(headings[index].tagName[1], 10);
        expect(level).toBeLessThanOrEqual(previousLevel + 1);
        previousLevel = level;
      }
    };

    checkNoSkippedLevels();
    unmount();

    // Wide layout (titleLevel 2)
    render(CompletionReportView, { planId: 'fix-relay', report: mockReport, titleLevel: 2 });
    checkNoSkippedLevels();
  });

  it('moves focus to the report title when the report opens (D3)', async () => {
    render(CompletionReportView, { planId: 'fix-relay', report: mockReport });

    const title = screen.getByRole('heading', { level: 1, name: 'fix-relay' });
    await waitFor(() => expect(document.activeElement).toBe(title));
  });

  it('moves focus to the loading heading while the report loads (D3)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockImplementation(() => new Promise(() => {}));
    render(CompletionReportView, { planId: 'fix-relay' });

    await waitFor(() => {
      const heading = screen.getByRole('heading', { level: 1, name: /Loading completion report/i });
      expect(document.activeElement).toBe(heading);
    });
  });

  it('moves focus to the error heading when the fetch fails (D3)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockRejectedValue(new Error('Fetch failed'));
    render(CompletionReportView, { planId: 'fix-relay' });

    await waitFor(() => {
      const heading = screen.getByRole('heading', {
        level: 1,
        name: /The runtime refused the request/i,
      });
      expect(document.activeElement).toBe(heading);
    });
  });

  it('leaves the focus where the operator moved it while the fetch ran (L3-4)', async () => {
    let resolveFetch: (report: PlanCompletionReport) => void = () => {};
    vi.spyOn(apiClient, 'getPlanCompletion').mockImplementation(
      () =>
        new Promise<PlanCompletionReport>((resolve) => {
          resolveFetch = resolve;
        }),
    );
    const elsewhere = document.createElement('button');
    elsewhere.textContent = 'Elsewhere';
    document.body.appendChild(elsewhere);

    try {
      render(CompletionReportView, { planId: 'fix-relay' });
      await waitFor(() => {
        const loading = screen.getByRole('heading', {
          level: 1,
          name: /Loading completion report/i,
        });
        expect(document.activeElement).toBe(loading);
      });

      // The operator moves on while the request is still in flight.
      elsewhere.focus();
      resolveFetch(mockReport);

      expect(await screen.findByRole('heading', { level: 1, name: 'fix-relay' })).toBeInTheDocument();
      expect(document.activeElement).toBe(elsewhere);
    } finally {
      elsewhere.remove();
    }
  });

  it('returns focus to the invoking control when the report closes (D3)', async () => {
    const invoking = document.createElement('button');
    invoking.textContent = 'Show details';
    document.body.appendChild(invoking);
    invoking.focus();
    const onClose = vi.fn();

    try {
      render(CompletionReportView, { planId: 'fix-relay', report: mockReport, onClose });

      const title = screen.getByRole('heading', { level: 1, name: 'fix-relay' });
      await waitFor(() => expect(document.activeElement).toBe(title));

      await fireEvent.click(screen.getByRole('button', { name: 'Back' }));

      expect(onClose).toHaveBeenCalledOnce();
      expect(document.activeElement).toBe(invoking);
    } finally {
      invoking.remove();
    }
  });

  it('returns focus to the invoker when the report is destroyed without a Back click (D3)', async () => {
    const invoking = document.createElement('button');
    invoking.textContent = 'Show details';
    document.body.appendChild(invoking);
    invoking.focus();

    try {
      const { unmount } = render(CompletionReportView, {
        planId: 'fix-relay',
        report: mockReport,
      });

      const title = screen.getByRole('heading', { level: 1, name: 'fix-relay' });
      await waitFor(() => expect(document.activeElement).toBe(title));

      unmount();

      expect(document.activeElement).toBe(invoking);
    } finally {
      invoking.remove();
    }
  });

  it('does not steal focus back when the report is destroyed after a Back click (D3)', async () => {
    const invoking = document.createElement('button');
    invoking.textContent = 'Show details';
    const elsewhere = document.createElement('button');
    elsewhere.textContent = 'Elsewhere';
    document.body.append(invoking, elsewhere);
    invoking.focus();
    const onClose = vi.fn();

    try {
      const { unmount } = render(CompletionReportView, {
        planId: 'fix-relay',
        report: mockReport,
        onClose,
      });

      const title = screen.getByRole('heading', { level: 1, name: 'fix-relay' });
      await waitFor(() => expect(document.activeElement).toBe(title));

      await fireEvent.click(screen.getByRole('button', { name: 'Back' }));
      expect(document.activeElement).toBe(invoking);

      elsewhere.focus();
      unmount();

      expect(document.activeElement).toBe(elsewhere);
    } finally {
      invoking.remove();
      elsewhere.remove();
    }
  });
});
