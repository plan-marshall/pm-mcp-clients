import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import CompletionReportView from './CompletionReportView.svelte';
import { apiClient, type PlanCompletionReport } from '../api/client';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

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
    expect(screen.getByText('PR #214')).toBeInTheDocument();
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

    expect(screen.getByRole('heading', { level: 2, name: 'Tokens' })).toBeInTheDocument();
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

      expect(
        await screen.findByRole('heading', { level: 1, name: 'fix-relay' }),
      ).toBeInTheDocument();
      expect(document.activeElement).toBe(elsewhere);
    } finally {
      elsewhere.remove();
    }
  });

  it('does not render an in-page Back button (D3)', async () => {
    render(CompletionReportView, { planId: 'fix-relay', report: mockReport });

    const title = screen.getByRole('heading', { level: 1, name: 'fix-relay' });
    await waitFor(() => expect(document.activeElement).toBe(title));

    expect(screen.queryByRole('button', { name: 'Back' })).not.toBeInTheDocument();
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

  it('does not steal focus back when focus moved elsewhere before unmount (D3)', async () => {
    const invoking = document.createElement('button');
    invoking.textContent = 'Show details';
    const elsewhere = document.createElement('button');
    elsewhere.textContent = 'Elsewhere';
    document.body.append(invoking, elsewhere);
    invoking.focus();

    try {
      const { unmount } = render(CompletionReportView, {
        planId: 'fix-relay',
        report: mockReport,
      });

      const title = screen.getByRole('heading', { level: 1, name: 'fix-relay' });
      await waitFor(() => expect(document.activeElement).toBe(title));

      elsewhere.focus();
      unmount();

      expect(document.activeElement).toBe(elsewhere);
    } finally {
      invoking.remove();
      elsewhere.remove();
    }
  });

  describe('D3 vocabulary: object head, Card/KeyValueList sections, and deliverable states (PLAN-154 D3)', () => {
    it('renders the object head with KindMark(plan), title, Chip(plan_id), PrChip, StatusLabel, end time, plan view Link, and secondary Chat Button', () => {
      render(CompletionReportView, { planId: 'fix-relay', report: mockReport });

      const header = document.querySelector('.report-header') as HTMLElement;
      expect(header).not.toBeNull();

      const kindMark = header.querySelector('.kind-mark') as HTMLElement;
      expect(kindMark).not.toBeNull();
      expect(kindMark).toHaveAttribute('data-kind', 'plan');

      expect(header.querySelector('.report-title')).toHaveTextContent('fix-relay');

      const chip = header.querySelector('.chip') as HTMLElement;
      expect(chip).not.toBeNull();
      expect(chip).toHaveTextContent('fix-relay');

      const prLink = header.querySelector('a.external-link.link--chip') as HTMLElement;
      expect(prLink).not.toBeNull();
      expect(prLink).toHaveTextContent('PR #214');
      expect(prLink).toHaveAttribute(
        'href',
        'https://github.com/plan-marshall/pm-mcp-clients/pull/214',
      );

      const status = header.querySelector('.status-label--success') as HTMLElement;
      expect(status).not.toBeNull();
      expect(status).toHaveTextContent('merged');

      expect(header.querySelector('.ended-time')).toHaveTextContent('today 11:20');

      const planViewLink = screen.getByRole('link', { name: 'Its plan view' });
      expect(planViewLink).toHaveAttribute('href', '/plans/fix-relay');

      const chatBtn = screen.getByRole('button', { name: /Chat/i });
      expect(chatBtn).toHaveClass('btn--secondary');
    });

    it('renders every section inside Cards without nested cards and puts summary rows in KeyValueList', () => {
      render(CompletionReportView, { planId: 'fix-relay', report: mockReport });

      const cards = screen.getAllByTestId('card');
      expect(cards.length).toBeGreaterThanOrEqual(6);
      expect(screen.queryByTestId('card-nested')).toBeNull();

      const kvList = screen.getAllByTestId('key-value-list')[0];
      expect(kvList).toHaveTextContent('Outcome');
      expect(kvList).toHaveTextContent('the pull request #214 was merged into main');
      expect(kvList).toHaveTextContent('Reason');
      expect(kvList).toHaveTextContent('Ran');
      expect(kvList).toHaveTextContent('2 h 40 min, 08:40 to 11:20 · paused 20 min by you');
      expect(kvList).toHaveTextContent('Change');
      expect(kvList).toHaveTextContent('12 files · +340 −27 · 2 unplanned');

      // Decisions use Chip for decision id, own calls use StatusLabel for judgment
      expect(document.querySelectorAll('.own-call-chip')).toHaveLength(0);
      expect(document.querySelectorAll('.section-badge')).toHaveLength(0);
    });

    it('renders deliverables in mock and engine states with catalogue words in English and German and neutral word for unknown state', () => {
      const allStatesReport: PlanCompletionReport = {
        ...mockReport,
        deliverables: [
          { name: 'task done', state: 'done' },
          { name: 'task not started', state: 'not_started' },
          { name: 'task failed', state: 'failed' },
          { name: 'task open', state: 'open' },
          { name: 'task infeasible', state: 'infeasible' },
          { name: 'task deferred', state: 'deferred' },
          { name: 'task removed', state: 'removed' },
          { name: 'task unknown', state: 'something_weird' },
        ],
      };

      // English
      useCatalogue('en');
      const { unmount } = render(CompletionReportView, { planId: 'fix-relay', report: allStatesReport });
      const itemsEn = document.querySelectorAll('.deliverable-item');
      expect(itemsEn).toHaveLength(8);

      expect(itemsEn[0].querySelector('[data-icon="state.check"]')).not.toBeNull();
      expect(itemsEn[0].querySelector('.deliverable-state')).toHaveTextContent('done');

      expect(itemsEn[1].querySelector('[data-icon="state.dot"]')).not.toBeNull();
      expect(itemsEn[1].querySelector('.deliverable-state')).toHaveTextContent('not started');

      expect(itemsEn[2].querySelector('[data-icon="state.dot"]')).not.toBeNull();
      expect(itemsEn[2].querySelector('.deliverable-state')).toHaveTextContent('failed');

      expect(itemsEn[3].querySelector('[data-icon="state.dot"]')).not.toBeNull();
      expect(itemsEn[3].querySelector('.deliverable-state')).toHaveTextContent('open');

      expect(itemsEn[4].querySelector('[data-icon="act.cancel"]')).not.toBeNull();
      expect(itemsEn[4].querySelector('.deliverable-state')).toHaveTextContent('infeasible');

      expect(itemsEn[5].querySelector('[data-icon="state.dot"]')).not.toBeNull();
      expect(itemsEn[5].querySelector('.deliverable-state')).toHaveTextContent('deferred');

      expect(itemsEn[6].querySelector('[data-icon="act.cancel"]')).not.toBeNull();
      expect(itemsEn[6].querySelector('.deliverable-state')).toHaveTextContent('removed');

      expect(itemsEn[7].querySelector('[data-icon="state.dot"]')).not.toBeNull();
      expect(itemsEn[7].querySelector('.deliverable-state')).toHaveTextContent('unknown');

      unmount();

      // German
      useCatalogue('de');
      const { unmount: unmountDe } = render(CompletionReportView, { planId: 'fix-relay', report: allStatesReport });
      const itemsDe = document.querySelectorAll('.deliverable-item');
      expect(itemsDe).toHaveLength(8);

      expect(itemsDe[0].querySelector('[data-icon="state.check"]')).not.toBeNull();
      expect(itemsDe[0].querySelector('.deliverable-state')).toHaveTextContent('fertig');

      expect(itemsDe[1].querySelector('[data-icon="state.dot"]')).not.toBeNull();
      expect(itemsDe[1].querySelector('.deliverable-state')).toHaveTextContent('nicht begonnen');

      expect(itemsDe[2].querySelector('[data-icon="state.dot"]')).not.toBeNull();
      expect(itemsDe[2].querySelector('.deliverable-state')).toHaveTextContent('fehlgeschlagen');

      expect(itemsDe[3].querySelector('[data-icon="state.dot"]')).not.toBeNull();
      expect(itemsDe[3].querySelector('.deliverable-state')).toHaveTextContent('offen');

      expect(itemsDe[4].querySelector('[data-icon="act.cancel"]')).not.toBeNull();
      expect(itemsDe[4].querySelector('.deliverable-state')).toHaveTextContent('nicht machbar');

      expect(itemsDe[5].querySelector('[data-icon="state.dot"]')).not.toBeNull();
      expect(itemsDe[5].querySelector('.deliverable-state')).toHaveTextContent('zurückgestellt');

      expect(itemsDe[6].querySelector('[data-icon="act.cancel"]')).not.toBeNull();
      expect(itemsDe[6].querySelector('.deliverable-state')).toHaveTextContent('entfernt');

      expect(itemsDe[7].querySelector('[data-icon="state.dot"]')).not.toBeNull();
      expect(itemsDe[7].querySelector('.deliverable-state')).toHaveTextContent('unbekannt');

      unmountDe();
      useCatalogue('en');

      // Empty deliverables read one sentence: the wire names no phase on the report.
      render(CompletionReportView, {
        planId: 'fix-relay',
        report: { ...mockReport, deliverables: [] },
      });
      expect(screen.getByText('no deliverables yet')).toBeInTheDocument();
    });

    it('never guesses a phase from the token rows', () => {
      useCatalogue('en');
      render(CompletionReportView, {
        planId: 'fix-relay',
        report: {
          ...mockReport,
          deliverables: [],
          token_usage: {
            ...(mockReport.token_usage as NonNullable<PlanCompletionReport['token_usage']>),
            by_phase: [
              { name: 'refine', total: 10, input: 6, output: 4, unmeasured: 0, floor: false },
              { name: 'outline', total: 10, input: 6, output: 4, unmeasured: 0, floor: false },
            ],
          },
        } as PlanCompletionReport,
      });
      expect(screen.getByText('no deliverables yet')).toBeInTheDocument();
      expect(screen.queryByText(/it ended in/)).not.toBeInTheDocument();
    });

    it('renders data.title in headline when present rather than repeating plan_id', () => {
      useCatalogue('en');
      const reportWithTitle: PlanCompletionReport = {
        ...mockReport,
        title: 'Custom Human Readable Title',
        plan_id: 'plan-123',
      };
      render(CompletionReportView, { planId: 'plan-123', report: reportWithTitle });
      const headline = document.querySelector('.report-header__headline .report-title');
      expect(headline).toHaveTextContent('Custom Human Readable Title');
    });
  });

  describe('D4 vocabulary: tokens section with floor sign, not measured, coverage, DataTable/KeyValueList, and phase links (PLAN-154 D4)', () => {
    it('renders total and coverage in KeyValueList, floor sign, unmeasured model count, DataTable for role/model, and phase Link to ended plan', () => {
      const reportWithUnmeasured: PlanCompletionReport = {
        ...mockReport,
        token_usage: {
          ...mockReport.token_usage!,
          by_phase: [
            ...mockReport.token_usage!.by_phase!,
            {
              name: 'execute',
              total: 241000,
              input: 222000,
              output: 19000,
              unmeasured: 3,
              floor: true,
            },
          ],
          by_model: [
            ...mockReport.token_usage!.by_model!,
            {
              name: 'codex:gpt-5',
              total: 14000,
              input: 13000,
              output: 1000,
              unmeasured: 3,
              floor: true,
            },
          ],
        },
      };

      render(CompletionReportView, { planId: 'fix-relay', report: reportWithUnmeasured });

      const tokensSection = document.querySelector('.tokens-section') as HTMLElement;
      expect(tokensSection).not.toBeNull();

      // Total & coverage rendered via KeyValueList with floor sign
      const tokenKv = tokensSection.querySelector('[data-testid="key-value-list"]') as HTMLElement;
      expect(tokenKv).not.toBeNull();
      expect(tokenKv).toHaveTextContent('≥ 412 k');
      expect(tokenKv).toHaveTextContent('3 of 41 tasks not measured: their worker was terminated');

      // By phase lines are Links to the phase in the ended plan view
      const refineLink = screen.getByRole('link', { name: 'refine' });
      expect(refineLink).toHaveAttribute('href', '/plans/fix-relay?mode=ended&node=refine');
      const executeLink = screen.getByRole('link', { name: 'execute' });
      expect(executeLink).toHaveAttribute('href', '/plans/fix-relay?mode=ended&node=execute');

      // By role and by model use DataTable with right-aligned numeric cells
      const tables = tokensSection.querySelectorAll('[data-testid="data-table"]');
      expect(tables.length).toBeGreaterThanOrEqual(2);
      const numericCells = tokensSection.querySelectorAll('td[data-align="end"]');
      expect(numericCells.length).toBeGreaterThan(0);

      // Model with unmeasured tasks shows floor sign and unmeasured count
      const modelTable = tables[1] as HTMLElement;
      expect(modelTable).toHaveTextContent('codex:gpt-5');
      expect(modelTable).toHaveTextContent('≥ 14 k');
      expect(modelTable).toHaveTextContent('3 not measured');
    });

    it('renders "not measured" without 0 or floor sign when zero tasks were measured', () => {
      const unmeasuredReport: PlanCompletionReport = {
        ...mockReport,
        token_usage: {
          ...mockReport.token_usage!,
          total: 0,
          input: 0,
          output: 0,
          tasks_measured: 0,
          tasks_total: 5,
          is_floor: true,
          by_phase: [],
          by_role: [],
          by_model: [],
          waste_records: [],
          total_waste_tokens: 0,
        },
      };

      render(CompletionReportView, { planId: 'fix-relay', report: unmeasuredReport });

      const tokensSection = document.querySelector('.tokens-section') as HTMLElement;
      const tokenKv = tokensSection.querySelector('[data-testid="key-value-list"]') as HTMLElement;
      expect(tokenKv).not.toBeNull();
      expect(tokenKv).toHaveTextContent('not measured');
      expect(tokenKv).not.toHaveTextContent('≥');
    });

    it('renders unmeasured reason wire code and waste classes through catalogue in English and German without raw codes', () => {
      const reportWithWaste: PlanCompletionReport = {
        ...mockReport,
        token_usage: {
          ...mockReport.token_usage!,
          unmeasured_reason: 'terminated',
          tasks_measured: 4,
          tasks_total: 5,
          waste_records: [
            { waste_class: 'no_product', tokens: 18000, share_percent: 18, tasks_count: 2 },
            { waste_class: 'retries', tokens: 21000, share_percent: 21, tasks_count: 1, cause_summary: 'retryable 1' },
            { waste_class: 'remediation', tokens: 13000, share_percent: 13, tasks_count: 1, cause_summary: 'returned with findings 1' },
          ],
          total_waste_tokens: 52000,
          waste_share_percent: 13,
        },
      };

      // English
      useCatalogue('en');
      const { unmount } = render(CompletionReportView, { planId: 'fix-relay', report: reportWithWaste });
      expect(document.querySelector('.tokens-section')).toHaveTextContent('their worker was terminated');
      expect(document.querySelector('.waste-group')).toHaveTextContent('(13 % of measured)');
      expect(document.querySelector('.waste-list')).toHaveTextContent('produced nothing');
      expect(document.querySelector('.waste-list')).toHaveTextContent('retries');
      expect(document.querySelector('.waste-list')).toHaveTextContent('remediation');
      unmount();

      // German
      useCatalogue('de');
      const { unmount: unmountDe } = render(CompletionReportView, { planId: 'fix-relay', report: reportWithWaste });
      expect(document.querySelector('.tokens-section')).toHaveTextContent('ihr Worker wurde beendet');
      expect(document.querySelector('.waste-group')).toHaveTextContent('(13 % des Gemessenen)');
      expect(document.querySelector('.waste-list')).toHaveTextContent('ohne Ergebnis');
      expect(document.querySelector('.waste-list')).toHaveTextContent('Wiederholungen');
      expect(document.querySelector('.waste-list')).toHaveTextContent('Nacharbeit');
      unmountDe();

      // Complete coverage (all tasks measured) -> waste_share has no "of measured"
      useCatalogue('en');
      const fullCoverageReport: PlanCompletionReport = {
        ...reportWithWaste,
        token_usage: {
          ...reportWithWaste.token_usage!,
          tasks_measured: 5,
          tasks_total: 5,
        },
      };
      const { unmount: unmountFull } = render(CompletionReportView, { planId: 'fix-relay', report: fullCoverageReport });
      expect(document.querySelector('.waste-group')).toHaveTextContent('(13 %)');
      expect(document.querySelector('.waste-group')).not.toHaveTextContent('of measured');
      unmountFull();
    });
  });
});
