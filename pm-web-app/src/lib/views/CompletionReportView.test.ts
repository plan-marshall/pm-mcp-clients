import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
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

  it('renders tokens section with coverage, breakdown, and toggles raw components', async () => {
    render(CompletionReportView, { planId: 'fix-relay', report: mockReport });

    expect(screen.getByText('Tokens')).toBeInTheDocument();
    expect(
      screen.getByText(/3 of 41 tasks not measured: their worker was terminated/),
    ).toBeInTheDocument();

    // By phase
    expect(screen.getByText('refine')).toBeInTheDocument();
    // By role
    expect(screen.getByText('planner')).toBeInTheDocument();
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
});
