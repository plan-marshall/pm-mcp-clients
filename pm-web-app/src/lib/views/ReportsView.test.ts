import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import { tick } from 'svelte';
import ReportsView from './ReportsView.svelte';
import { formatTime } from '../display/format';
import { runtimeStore } from '../stores/runtime';
import { ClientError } from '../api/errors';
import { makeRuntimeState } from '../test/runtimeState';
import { get } from 'svelte/store';
import {
  apiClient,
  type PlanCompletionReport,
  type ReportItem,
  type ReportsPage,
} from '../api/client';

describe('ReportsView component', () => {
  let eventHandler: ((event: string, data: unknown) => void) | null = null;
  const mockReports: ReportItem[] = [
    {
      id: 'rep-0142',
      from: { project: 'pm-mcp-clients', epic: 'relay-hardening', plan: 'fix-relay' },
      kind: 'plan.ended',
      subject: 'Ended: merged (#214)',
      body: '3 of 3 deliverables · 2 h 40 min',
      detail_link: '/api/v1/plans/fix-relay/completion',
      created_at: '2026-10-16T11:20:00Z',
      created: 'today 11:20',
    },
    {
      id: 'rep-0139',
      from: { project: 'pm-mcp-clients', plan: 'docs-typo' },
      kind: 'plan.ended',
      subject: 'Ended: no change',
      created_at: '2026-10-16T09:02:00Z',
      created: 'today 09:02',
      sent_at: '2026-10-16T09:40:00Z',
      sent: 'today 09:40',
    },
    {
      id: 'rep-0131',
      from: { project: 'pm-mcp-core' },
      kind: 'project.onboarded',
      subject: 'Onboarded',
      created_at: '2026-10-15T18:40:00Z',
      created: 'yesterday 18:40',
    },
  ];

  const mockPage: ReportsPage = {
    items: mockReports,
    unread_count: 3,
    oldest_unread_time: 'yesterday 18:40',
    has_older: true,
    older_count: 5,
  };

  beforeEach(() => {
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: true,
        deviceLabel: 'Firefox on mbp-oliver',
        machineName: 'mbp-oliver',
        currentRoute: 'reports',
        currentState: null,
        isRevoked: false,
        revokedReason: '',
        isOffline: false,
        isListenerClosed: false,
        completedSteps: 0,
        activeStep: 1,
        unreadReportsCount: 3,
        quietReportNotice: null,
      }),
    );

    vi.spyOn(apiClient, 'getReports').mockResolvedValue(mockPage);
    vi.spyOn(apiClient, 'markReportRead').mockResolvedValue(undefined);
    vi.spyOn(apiClient, 'markAllReportsRead').mockResolvedValue({ marked_count: 3 });
    eventHandler = null;
    window.innerWidth = 1440;
    window.dispatchEvent(new Event('resize'));
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((handler) => {
      eventHandler = handler;
      return () => {};
    });
  });

  /** @returns the completion report the detail fetches for `fix-relay` */
  function fixRelayCompletion(): PlanCompletionReport {
    return {
      plan_id: 'fix-relay',
      title: 'fix-relay',
      from: { project: 'pm-mcp-clients', plan: 'fix-relay' },
      outcome: 'merged',
      started_at: '2026-10-16T08:40:00Z',
      ended_at: '2026-10-16T11:20:00Z',
      duration_seconds: 9600,
      paused_seconds: 0,
      deliverables: [],
      replanning_records: [],
      decision_records: [],
    } as unknown as PlanCompletionReport;
  }

  /** @returns a full completion report with deliverables, replanning, decisions and tokens */
  function fullRelayCompletion(): PlanCompletionReport {
    return {
      plan_id: 'fix-relay',
      title: 'fix-relay',
      from: { project: 'pm-mcp-clients', epic: 'relay-hardening', plan: 'fix-relay' },
      outcome: 'merged',
      terminal_reason: 'the pull request #214 was merged into main',
      pull_request_number: 214,
      pull_request_url: 'https://github.com/plan-marshall/pm-mcp-clients/pull/214',
      started_at: '2026-10-16T08:40:00Z',
      ended_at: '2026-10-16T11:20:00Z',
      duration_seconds: 9600,
      paused_seconds: 0,
      deliverables: [
        { name: 'cursor write order', state: 'done' },
        { name: 'fsync of the cursor file', state: 'done' },
      ],
      replanning_records: [{ trigger: 'the base moved', time: '09:55', change: 'TASK-5 added' }],
      decision_records: [
        { id: 'Q-003', question: 'Rebase onto main', answer: 'rebase', by: 'operator' },
      ],
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
        is_floor: false,
        by_phase: [
          {
            name: 'refine',
            total: 38000,
            input: 35000,
            output: 3000,
            unmeasured: 0,
            floor: false,
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
      },
    } as unknown as PlanCompletionReport;
  }

  it('renders unread reports grouped by project, epic, and plan', async () => {
    render(ReportsView);

    await waitFor(() => {
      expect(screen.getByText('Reports')).toBeInTheDocument();
      expect(screen.getByText(/3 unread/)).toBeInTheDocument();
    });

    // Check project headers (the path of each report repeats the project as a link)
    expect(screen.getAllByText('pm-mcp-clients')[0]).toBeInTheDocument();
    expect(screen.getAllByText('pm-mcp-core')[0]).toBeInTheDocument();

    // Check epic header
    expect(screen.getAllByText('relay-hardening')[0]).toBeInTheDocument();

    // Check items
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();
    expect(screen.getByText('fix-relay')).toBeInTheDocument();
    expect(screen.getByText('Ended: no change')).toBeInTheDocument();
    expect(screen.getByText('docs-typo')).toBeInTheDocument();
    expect(screen.getByText('Onboarded')).toBeInTheDocument();

    // Check sent delay display
    expect(
      screen.getByText(
        `${formatTime('2026-10-16T09:02:00Z', 'relative', 'en')} · sent ${formatTime('2026-10-16T09:40:00Z', 'short', 'en')}`,
      ),
    ).toBeInTheDocument();
  });

  it('follows the route report plan while the reports route stays mounted', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());

    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: 'fix-relay' }));

    render(ReportsView);

    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });
    expect(apiClient.getPlanCompletion).toHaveBeenCalledWith('fix-relay');

    // A direct step to another report remounts the detail and fetches that report.
    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: 'docs-typo' }));
    await waitFor(() => {
      expect(apiClient.getPlanCompletion).toHaveBeenCalledWith('docs-typo');
    });

    // A plain visit to the reports route closes the detail it no longer names.
    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: null }));
    await waitFor(() => {
      expect(document.querySelector('.completion-report')).toBeNull();
    });
  });

  it('closes a detail opened from the list on a plain reports visit', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());

    render(ReportsView);

    const details = await screen.findAllByRole('button', { name: /Show the details/i });
    await fireEvent.click(details[0]);
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });
    expect(get(runtimeStore).selectedReportDetailPlanId).toBe('fix-relay');

    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: null }));
    await waitFor(() => {
      expect(document.querySelector('.completion-report')).toBeNull();
    });
  });

  it('marks a single report read from its row menu', async () => {
    render(ReportsView);

    await waitFor(() => {
      expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();
    });

    const card = screen.getByText('Ended: merged (#214)').closest('.report-card') as HTMLElement;
    await fireEvent.click(within(card).getByRole('button', { name: /Report actions/i }));
    await fireEvent.click(await within(card).findByRole('menuitem', { name: /Mark read/i }));

    await waitFor(() => {
      expect(apiClient.markReportRead).toHaveBeenCalledWith('rep-0142');
    });
  });

  it('keeps the rows, the scroll position and an open row menu while the list reloads', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const card = screen.getByText('Ended: merged (#214)').closest('.report-card') as HTMLElement;
    await fireEvent.click(within(card).getByRole('button', { name: /Report actions/i }));
    const menu = await within(card).findByRole('menuitem', { name: /Mark read/i });
    expect(menu).toBeInTheDocument();

    const tree = document.querySelector('.reports-tree') as HTMLElement;
    tree.scrollTop = 480;

    // A report without a payload announces that the list should be read again.
    (apiClient.getReports as ReturnType<typeof vi.fn>).mockClear();
    eventHandler?.('report.sent', {});
    await waitFor(() => expect(apiClient.getReports).toHaveBeenCalledTimes(1));
    await tick();

    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();
    expect(document.querySelector('.reports-tree')).toBe(tree);
    expect(tree.scrollTop).toBe(480);
    expect(within(card).getByRole('menuitem', { name: /Mark read/i })).toBeInTheDocument();
    expect(screen.queryAllByTestId('skeleton')).toHaveLength(0);
  });

  it('marks unread rows with a dot and the bold subject', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const card = screen.getByText('Ended: merged (#214)').closest('.report-card') as HTMLElement;
    expect(card).toHaveAttribute('data-unread', 'true');
    expect(card.querySelector('.report-card__unread-dot')).not.toBeNull();
    // The unread word is anchored to this row, not matched anywhere on the page.
    expect(within(card).getByText('unread')).toBeInTheDocument();
    expect(
      screen.getByText('Ended: merged (#214)').classList.contains('report-card__subject--unread'),
    ).toBe(true);
  });

  it('moves the row it opens from unread to read in the list beside the detail', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const details = screen.getAllByRole('button', { name: /Show the details/i });
    await fireEvent.click(details[0]);

    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });
    const active = document.querySelector('.report-card--active') as HTMLElement;
    expect(active).toHaveAttribute('data-unread', 'false');
    expect(active.querySelector('.report-card__unread-dot')).toBeNull();
    expect(within(active).getByTestId('report-read-state')).toHaveTextContent('read');
    expect(
      screen.getByText('Ended: merged (#214)').classList.contains('report-card__subject--unread'),
    ).toBe(false);
  });

  it('puts the machine reports in a group of their own', async () => {
    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [
        {
          id: 'rep-0130',
          from: { project: 'pm-mcp-clients' },
          kind: 'system.cleanup_due',
          subject: 'Cleanup is due',
          created_at: '2026-10-16T09:00:00Z',
          created: 'today 09:00',
        },
        mockReports[0],
      ],
      unread_count: 2,
      has_older: false,
      older_count: 0,
    });

    render(ReportsView);
    await screen.findByText('Cleanup is due');

    const sections = [...document.querySelectorAll('.project-section')];
    const machine = sections.find((section) =>
      section.querySelector('.project-header')?.textContent?.includes('This machine'),
    );
    expect(machine).toBeDefined();
    expect(machine?.textContent).toContain('Cleanup is due');
    expect(machine?.textContent).not.toContain('Ended: merged (#214)');

    const project = sections.find((section) =>
      section.querySelector('.project-header')?.textContent?.includes('pm-mcp-clients'),
    );
    expect(project?.textContent).not.toContain('Cleanup is due');
  });

  it('marks all shown reports read', async () => {
    render(ReportsView);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Mark all shown read/i })).toBeInTheDocument();
    });

    const markAllBtn = screen.getByRole('button', { name: /Mark all shown read/i });
    await fireEvent.click(markAllBtn);

    expect(apiClient.markAllReportsRead).toHaveBeenCalledWith(['rep-0142', 'rep-0139', 'rep-0131']);
  });

  /**
   * @param index the 1-based position in the seven-unread fixture
   * @returns one unread report; the first is the one a deep link opens
   */
  function unreadFixture(index: number): ReportItem {
    return {
      id: `rep-02${String(index).padStart(4, '0')}`,
      from:
        index === 1
          ? { project: 'pm-mcp-clients', plan: 'fix-relay' }
          : { project: 'pm-mcp-clients', plan: `plan-${index}` },
      kind: 'plan.ended',
      subject: `Unread report ${index}`,
      created_at: '2026-10-16T11:20:00Z',
    };
  }

  it('reloads the first unread page after a shown-ids mark-all and lists the remaining reports with the server count', async () => {
    const shownFive = [1, 2, 3, 4, 5].map(unreadFixture);
    const olderTwo = [6, 7].map(unreadFixture);
    const shownPage: ReportsPage = {
      items: shownFive,
      unread_count: 7,
      has_older: true,
      older_count: 2,
    };
    const remainingPage: ReportsPage = {
      items: olderTwo,
      unread_count: 2,
      has_older: false,
      older_count: 0,
    };
    let servedPage = shownPage;
    let reloadResolve: ((page: ReportsPage) => void) | undefined;
    let readCalls = 0;
    vi.spyOn(apiClient, 'getReports').mockImplementation(() => {
      readCalls += 1;
      if (readCalls === 1) return Promise.resolve(servedPage);
      return new Promise<ReportsPage>((resolve) => {
        reloadResolve = resolve;
      });
    });
    vi.spyOn(apiClient, 'markAllReportsRead').mockImplementation(async () => {
      servedPage = remainingPage;
      return { marked_count: 5 };
    });
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    // The deep link opens the first report without reading it, so it is shown and unread.
    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: 'fix-relay' }));

    render(ReportsView);
    await screen.findByText('Unread report 1');
    expect(screen.getByTestId('unread-count')).toHaveTextContent('7 unread reports');
    expect(screen.getByRole('button', { name: /Show 2 older/i })).toBeInTheDocument();

    await fireEvent.click(screen.getByRole('button', { name: /Mark all shown read/i }));

    expect(apiClient.markAllReportsRead).toHaveBeenCalledWith(shownFive.map((item) => item.id));

    // Before the reload answers, the count is the pre-count of the five shown ids (7 - 5). The
    // deep-linked report is counted with them, so leaving it out would leave 3 here.
    await waitFor(() => {
      expect(screen.getByTestId('unread-count')).toHaveTextContent('2 unread reports');
    });

    await waitFor(() => expect(reloadResolve).toBeDefined());
    reloadResolve?.(remainingPage);

    // After the reload the two older unread reports are listed and the count is the server's.
    await waitFor(() => {
      expect(screen.getByText('Unread report 6')).toBeInTheDocument();
    });
    expect(screen.getByText('Unread report 7')).toBeInTheDocument();
    expect(screen.getByTestId('unread-count')).toHaveTextContent('2 unread reports');
    // One page of unread reports remains, so the load-older control is gone.
    expect(screen.queryByRole('button', { name: /Show \d+ older/i })).toBeNull();
    // The retained row stays beside its open detail until that detail closes.
    expect(screen.getByText('Unread report 1')).toBeInTheDocument();
  });

  it('renders empty state when there are no unread reports', async () => {
    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [],
      unread_count: 0,
      has_older: false,
      older_count: 0,
    });

    render(ReportsView);

    await waitFor(() => {
      expect(screen.getByText('Nothing new')).toBeInTheDocument();
      expect(
        screen.getByText('Reports you have read are not shown any more.'),
      ).toBeInTheDocument();
    });
  });

  it('displays Show older button when has_older is true', async () => {
    render(ReportsView);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Show 5 older/i })).toBeInTheDocument();
    });

    const olderBtn = screen.getByRole('button', { name: /Show 5 older/i });
    await fireEvent.click(olderBtn);

    expect(apiClient.getReports).toHaveBeenCalledWith(true);
  });

  it('navigates from each part of the from path without reading the report', async () => {
    render(ReportsView);

    const projectLink = await screen.findAllByRole('button', {
      name: /Open the project pm-mcp-clients/i,
    });
    await fireEvent.click(projectLink[0]);
    expect(get(runtimeStore).currentRoute).toBe('projects');
    expect(get(runtimeStore).selectedProjectId).toBe('pm-mcp-clients');

    const epicLink = (
      await screen.findAllByRole('button', {
        name: /Open the epic relay-hardening/i,
      })
    )[0];
    await fireEvent.click(epicLink);
    expect(get(runtimeStore).currentRoute).toBe('epic');
    expect(get(runtimeStore).selectedEpicId).toBe('relay-hardening');

    const planLink = await screen.findByRole('button', { name: /Open the plan fix-relay/i });
    await fireEvent.click(planLink);
    expect(get(runtimeStore).currentRoute).toBe('plan');
    expect(get(runtimeStore).selectedPlanId).toBe('fix-relay');

    expect(apiClient.markReportRead).not.toHaveBeenCalled();
  });

  it('opens an epic report detail and reads it in the same act', async () => {
    const epicReport: ReportItem = {
      id: 'rep-0144',
      from: { project: 'pm-mcp-clients', epic: 'relay-hardening', plan: 'relay-docs' },
      kind: 'epic.waits_overlap',
      subject: 'relay-docs waits: overlap with relay-auth-v2 (2 files)',
      created_at: '2026-10-16T11:21:00Z',
      created: 'today 11:21',
      detail_link: '/api/v1/epics/relay-hardening',
    };
    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [epicReport],
      unread_count: 1,
      has_older: false,
      older_count: 0,
    });

    render(ReportsView);

    const card = (
      await screen.findByText('relay-docs waits: overlap with relay-auth-v2 (2 files)')
    ).closest('.report-card');
    expect(card).not.toBeNull();
    await fireEvent.click(
      within(card as HTMLElement).getByRole('button', { name: /Show the details/i }),
    );

    expect(get(runtimeStore).currentRoute).toBe('epic');
    expect(get(runtimeStore).selectedEpicId).toBe('relay-hardening');
    expect(apiClient.markReportRead).toHaveBeenCalledWith('rep-0144');
  });

  it('disables the detail of a subject the cleanup deleted', async () => {
    const deleted: ReportItem = {
      id: 'rep-0200',
      from: { project: 'pm-mcp-clients', plan: 'relay-docs' },
      kind: 'plan.ended',
      subject: 'relay-docs ended: merged',
      created_at: '2026-10-16T11:20:00Z',
      created: 'today 11:20',
      detail_link: '/api/v1/plans/relay-docs/completion',
      subject_deleted: true,
    };
    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [deleted],
      unread_count: 1,
      has_older: false,
      older_count: 0,
    });

    render(ReportsView);

    const detail = await screen.findByRole('button', { name: /Show the details/i });
    expect(detail).toBeDisabled();
    expect(screen.getByText(/relay-docs was deleted by the cleanup/)).toBeInTheDocument();
  });

  it('opens a cleanup report at the job and clears a stored preview', async () => {
    const cleanupReport: ReportItem = {
      id: 'rep-0130',
      from: { project: 'pm-mcp-clients' },
      kind: 'system.cleanup_due',
      subject: 'Cleanup is due',
      subject_params: { item: 'fix-relay' },
      created_at: '2026-10-16T09:00:00Z',
      created: 'today 09:00',
      detail_link: '/api/v1/system/cleanup',
    };
    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [cleanupReport],
      unread_count: 1,
      has_older: false,
      older_count: 0,
    });
    runtimeStore.update((s) => ({ ...s, cleanupPreviewItemId: 'plan-1' }));

    render(ReportsView);

    const card = (await screen.findByText('Cleanup is due')).closest('.report-card');
    await fireEvent.click(
      within(card as HTMLElement).getByRole('button', { name: /Show the details/i }),
    );

    expect(get(runtimeStore).currentRoute).toBe('cleanup');
    expect(get(runtimeStore).cleanupHighlightItemId).toBe('fix-relay');
    expect(get(runtimeStore).cleanupPreviewItemId).toBeNull();
  });

  it('announces a coalesced arrival politely and leaves the tab title alone', async () => {
    const originalTitle = document.title;
    render(ReportsView);
    await waitFor(() => expect(eventHandler).not.toBeNull());

    eventHandler?.('report.sent', {
      report: {
        id: 'rep-0201',
        from: { project: 'pm-mcp-clients', epic: 'relay-hardening', plan: 'relay-docs' },
        kind: 'plan.started',
        subject: 'relay-docs started by relay-hardening (2 of 3 at once)',
        created_at: '2026-10-21T10:00:00Z',
        created: 'today 10:00',
      } satisfies ReportItem,
    });
    eventHandler?.('report.sent', {
      report: {
        id: 'rep-0202',
        from: { project: 'pm-mcp-clients', epic: 'relay-hardening' },
        kind: 'epic.launchable',
        subject: '2 plans can start now in relay-hardening',
        created_at: '2026-10-21T10:01:00Z',
        created: 'today 10:01',
      } satisfies ReportItem,
    });

    const notice = await screen.findByTestId('report-arrival-notice');
    expect(notice).toHaveAttribute('aria-live', 'polite');
    expect(notice).toHaveTextContent('2 new reports · relay-hardening');
    expect(document.title).toBe(originalTitle);
  });

  /** @param value the filter text @returns the filter input */
  async function typeFilter(value: string): Promise<HTMLElement> {
    const input = await screen.findByTestId('filter-input');
    await fireEvent.input(input, { target: { value } });
    return input;
  }

  it('filters the loaded reports by project, epic, plan, body, and created date', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    await typeFilter('pm-mcp-core');
    expect(screen.queryByText('Ended: merged (#214)')).toBeNull();
    expect(screen.getByText('Onboarded')).toBeInTheDocument();

    await typeFilter('relay-hardening');
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();
    expect(screen.queryByText('Onboarded')).toBeNull();

    await typeFilter('docs-typo');
    expect(screen.getByText('Ended: no change')).toBeInTheDocument();
    expect(screen.queryByText('Ended: merged (#214)')).toBeNull();

    await typeFilter('deliverables');
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();
    expect(screen.queryByText('Ended: no change')).toBeNull();

    await typeFilter('2026-10-15');
    expect(screen.getByText('Onboarded')).toBeInTheDocument();
    expect(screen.queryByText('Ended: merged (#214)')).toBeNull();
  });

  it('matches a report on a status the wire carries', async () => {
    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [
        {
          id: 'rep-0300',
          from: { project: 'pm-mcp-clients', plan: 'fix-relay' },
          kind: 'plan.ended',
          subject: 'fix-relay ended: merged',
          status: 'superseded',
          created_at: '2026-10-16T11:20:00Z',
          created: 'today 11:20',
        },
      ],
      unread_count: 1,
      has_older: false,
      older_count: 0,
    });

    render(ReportsView);
    await typeFilter('superseded');
    expect(screen.getByText('fix-relay ended: merged')).toBeInTheDocument();
  });

  it('matches a multi-token query across the origin and the body', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    await typeFilter('relay deliverables');
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();
    expect(screen.queryByText('Ended: no change')).toBeNull();
    expect(screen.queryByText('Onboarded')).toBeNull();
  });

  it('drops an empty group and brings it back when the filter is cleared', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    expect(screen.getAllByText('pm-mcp-core').length).toBeGreaterThan(0);

    await typeFilter('relay');
    expect(screen.queryAllByText('pm-mcp-core')).toHaveLength(0);
    expect(screen.getAllByText('relay-hardening').length).toBeGreaterThan(0);

    await fireEvent.click(screen.getByTestId('filter-clear'));
    expect(screen.getAllByText('pm-mcp-core').length).toBeGreaterThan(0);
    expect(screen.getByText('Ended: no change')).toBeInTheDocument();
  });

  it('shows an explicit no-match state', async () => {
    render(ReportsView);
    await typeFilter('nothing-matches-this');

    expect(await screen.findByTestId('report-filter-empty')).toHaveTextContent(
      'No loaded report matches the filter.',
    );
    expect(screen.getByTestId('filter-results')).toHaveTextContent('No matches');
  });

  it('keeps a failed reload visible while the filter matches nothing', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await typeFilter('nothing-matches-this');
    expect(await screen.findByTestId('report-filter-empty')).toBeInTheDocument();

    (apiClient.getReports as ReturnType<typeof vi.fn>).mockRejectedValueOnce(
      new ClientError('timeout'),
    );
    eventHandler?.('report.sent', {});

    const error = await screen.findByTestId('reports-error');
    expect(error).toHaveTextContent('The request took too long');
    // The list stays where it is: the failure is reported above it, and the filter still says why
    // the list shows no row.
    expect(screen.getByTestId('report-filter-empty')).toBeInTheDocument();
    expect(screen.getByTestId('reports-refreshing')).toBeInTheDocument();
  });

  it('keeps the unread count and makes no request while typing', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    const requestsBefore = (apiClient.getReports as ReturnType<typeof vi.fn>).mock.calls.length;

    await typeFilter('relay');

    expect(screen.getByText(/3 unread/)).toBeInTheDocument();
    expect(apiClient.getReports).toHaveBeenCalledTimes(requestsBefore);
  });

  it('labels the loaded-only set while older reports exist and keeps the query when loading them', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const input = await typeFilter('relay');
    expect(screen.getByTestId('loaded-only-hint')).toHaveTextContent(
      'Only the reports loaded so far are searched.',
    );

    await fireEvent.click(screen.getByRole('button', { name: /Show 5 older/i }));
    expect(apiClient.getReports).toHaveBeenCalledWith(true);
    expect((input as HTMLInputElement).value).toBe('relay');
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();
  });

  it('sits on the triage template', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    expect(document.querySelector('[data-testid="triage-page"]')).not.toBeNull();
  });

  it('keeps the list beside the detail and marks the active card', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const details = screen.getAllByRole('button', { name: /Show the details/i });
    await fireEvent.click(details[0]);

    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });
    expect(document.querySelector('.reports-tree')).not.toBeNull();
    const active = document.querySelector('.report-card--active');
    expect(active).not.toBeNull();
    expect(active).toHaveAttribute('aria-current', 'true');

    // The page heading is the h1; the report title beside it sits one level lower.
    const h1Elements = document.querySelectorAll('h1');
    expect(h1Elements).toHaveLength(1);
    expect(h1Elements[0]).toHaveClass('reports-heading');
    expect(document.querySelector('.report-title')?.tagName.toLowerCase()).toBe('h2');
  });

  it('keeps the row beside its detail when the list reloads', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    let servedPage: ReportsPage = mockPage;
    vi.spyOn(apiClient, 'getReports').mockImplementation(() => Promise.resolve(servedPage));

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    // The runtime announces a report the view cannot place, so it reloads the list; the server no
    // longer serves the report the open detail read.
    servedPage = {
      items: mockReports.filter((report) => report.id !== 'rep-0142'),
      unread_count: 2,
      has_older: false,
      older_count: 0,
    };
    eventHandler?.('report.sent', { report_id: 'rep-9001' });

    await waitFor(() => {
      expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    });
    const active = document.querySelector('.report-card--active');
    expect(active).not.toBeNull();
    expect(active).toHaveAttribute('data-report-id', 'rep-0142');
    // The row the reload serves no more is kept once, in the place its successor defines.
    expect(document.querySelectorAll('[data-report-id="rep-0142"]')).toHaveLength(1);
    expect(
      Array.from(document.querySelectorAll('.report-card')).map((card) =>
        card.getAttribute('data-report-id'),
      ),
    ).toEqual(['rep-0142', 'rep-0139', 'rep-0131']);
  });

  it('drops the row of a detail another detail replaces', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const firstCard = screen.getByText('Ended: merged (#214)').closest('.report-card') as HTMLElement;
    await fireEvent.click(
      within(firstCard).getByRole('button', { name: /Show the details/i }),
    );
    await waitFor(() => {
      expect(apiClient.getPlanCompletion).toHaveBeenCalledWith('fix-relay');
    });

    const secondCard = screen.getByText('Ended: no change').closest('.report-card') as HTMLElement;
    await fireEvent.click(
      within(secondCard).getByRole('button', { name: /Show the details/i }),
    );
    await waitFor(() => {
      expect(apiClient.getPlanCompletion).toHaveBeenCalledWith('docs-typo');
    });

    expect(document.querySelector('[data-report-id="rep-0142"]')).toBeNull();
    expect(document.querySelector('[data-report-id="rep-0139"]')).not.toBeNull();
  });

  it('repeats the act the error line reports, not a plain reload', async () => {
    const read = vi
      .spyOn(apiClient, 'markReportRead')
      .mockRejectedValueOnce(new Error('read failed'));

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    const card = screen.getByText('Ended: merged (#214)').closest('.report-card') as HTMLElement;
    await fireEvent.click(within(card).getByRole('button', { name: /Report actions/i }));
    await fireEvent.click(await within(card).findByRole('menuitem', { name: /Mark read/i }));
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('read failed');
    });

    read.mockResolvedValueOnce(undefined);
    await fireEvent.click(screen.getByRole('button', { name: 'Try again' }));

    await waitFor(() => {
      expect(screen.queryByText('Ended: merged (#214)')).toBeNull();
    });
    expect(read).toHaveBeenCalledTimes(2);
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('keeps a retained row and its count when its menu reads it again', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);
    await waitFor(() => {
      expect(document.querySelector('.report-card--active')).not.toBeNull();
    });
    await waitFor(() => {
      expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    });

    const card = document.querySelector('.report-card--active') as HTMLElement;
    await fireEvent.click(within(card).getByRole('button', { name: /Report actions/i }));
    await fireEvent.click(await within(card).findByRole('menuitem', { name: /Mark read/i }));

    await waitFor(() => {
      expect(apiClient.markReportRead).toHaveBeenCalledTimes(2);
    });
    expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    expect(document.querySelector('.report-card--active')).not.toBeNull();
  });

  it('keeps the body to one line until its control shows the rest', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const card = screen.getByText('Ended: merged (#214)').closest('.report-card') as HTMLElement;
    expect(card.querySelector('.report-card__body--clamped')).not.toBeNull();

    await fireEvent.click(within(card).getByRole('button', { name: /Show the text/i }));
    expect(card.querySelector('.report-card__body--clamped')).toBeNull();
    expect(within(card).getByRole('button', { name: /Hide the text/i })).toBeInTheDocument();
  });

  it('names the row menu with the report and moves the focus in and back out', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const card = screen.getByText('Ended: merged (#214)').closest('.report-card') as HTMLElement;
    const trigger = within(card).getByRole('button', {
      name: 'Report actions: Ended: merged (#214)',
    });
    await fireEvent.click(trigger);

    const entry = await within(card).findByRole('menuitem', { name: /Mark read/i });
    await waitFor(() => {
      expect(entry).toHaveFocus();
    });

    // The arrow keys and Home/End move within the menu; with one entry the focus stays on it.
    await fireEvent.keyDown(entry, { key: 'ArrowDown' });
    expect(entry).toHaveFocus();
    await fireEvent.keyDown(entry, { key: 'End' });
    expect(entry).toHaveFocus();

    await fireEvent.keyDown(entry, { key: 'Escape' });
    await waitFor(() => {
      expect(trigger).toHaveFocus();
    });
    expect(within(card).queryByRole('menuitem')).toBeNull();

    // Tab closes the menu as well and puts the focus back on the control.
    await fireEvent.click(trigger);
    const reopened = await within(card).findByRole('menuitem', { name: /Mark read/i });
    await fireEvent.keyDown(reopened, { key: 'Tab' });
    await waitFor(() => {
      expect(trigger).toHaveFocus();
    });
    expect(within(card).queryByRole('menuitem')).toBeNull();
  });

  it('keeps the active card when the read event arrives before the read request resolves', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    let resolveRead: (() => void) | undefined;
    vi.spyOn(apiClient, 'markReportRead').mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveRead = resolve;
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);

    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    // The runtime answers the request with an event that can overtake the HTTP response.
    expect(apiClient.markReportRead).toHaveBeenCalledWith('rep-0142');
    eventHandler?.('report.read', { report_id: 'rep-0142' });

    // The row stays, marked read, even though the event was handled before the request resolved.
    await waitFor(() => {
      expect(screen.getByTestId('report-read-state')).toBeInTheDocument();
    });
    expect(document.querySelector('.report-card--active')).not.toBeNull();

    if (!resolveRead) throw new Error('the pending read request was never created');
    resolveRead();

    // Let the request continuation and the Svelte update run before the final assertions, so a
    // row removed after the response cannot slip through.
    await tick();
    await tick();
    expect(document.querySelector('.report-card--active')).not.toBeNull();
    expect(screen.getByTestId('report-read-state')).toBeInTheDocument();
  });

  it('restores a retained row to unread when the read request fails', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    vi.spyOn(apiClient, 'markReportRead').mockRejectedValue(new Error('read failed'));

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('read failed');
    });

    expect(document.querySelector('.report-card--active')).not.toBeNull();
    expect(screen.queryByTestId('report-read-state')).toBeNull();
    expect(screen.getByText(/3 unread/)).toBeInTheDocument();
  });

  it('keeps the row until a read that outlived its detail fails', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    let rejectRead: ((reason: Error) => void) | undefined;
    vi.spyOn(apiClient, 'markReportRead').mockImplementation(
      () =>
        new Promise<void>((_resolve, reject) => {
          rejectRead = reject;
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    // The operator closes the detail before the read request settles.
    await fireEvent.click(screen.getByRole('button', { name: /^Back$/i }));
    expect(document.querySelector('.completion-report')).toBeNull();
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();

    if (!rejectRead) throw new Error('the pending read request was never created');
    rejectRead(new Error('read failed'));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('read failed');
    });
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();
    expect(screen.queryByTestId('report-read-state')).toBeNull();
  });

  it('drops the row once a read that outlived its detail succeeds', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    let resolveRead: (() => void) | undefined;
    vi.spyOn(apiClient, 'markReportRead').mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveRead = resolve;
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });
    await fireEvent.click(screen.getByRole('button', { name: /^Back$/i }));
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();

    if (!resolveRead) throw new Error('the pending read request was never created');
    resolveRead();

    await waitFor(() => {
      expect(screen.queryByText('Ended: merged (#214)')).toBeNull();
    });
  });

  it('decrements the unread count once when the read event arrives before the response', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    let resolveRead: (() => void) | undefined;
    vi.spyOn(apiClient, 'markReportRead').mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveRead = resolve;
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    // The event overtakes the HTTP response and counts the read.
    eventHandler?.('report.read', { report_id: 'rep-0142' });
    await waitFor(() => {
      expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    });

    if (!resolveRead) throw new Error('the pending read request was never created');
    resolveRead();
    await tick();
    await tick();

    // The response settles the same read; it must not count it again.
    expect(screen.getByText(/2 unread/)).toBeInTheDocument();
  });

  it('decrements the unread count once when the response arrives before the read event', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    let resolveRead: (() => void) | undefined;
    vi.spyOn(apiClient, 'markReportRead').mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveRead = resolve;
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    // The response settles first and counts the read.
    if (!resolveRead) throw new Error('the pending read request was never created');
    resolveRead();
    await waitFor(() => {
      expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    });

    // The event for the same read arrives afterwards; it must not count it again.
    eventHandler?.('report.read', { report_id: 'rep-0142' });
    await tick();
    await tick();

    expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    expect(screen.getByTestId('report-read-state')).toBeInTheDocument();
  });

  it('decrements the unread count once for a read with no event', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const card = screen.getByText('Ended: merged (#214)').closest('.report-card') as HTMLElement;
    await fireEvent.click(within(card).getByRole('button', { name: /Report actions/i }));
    await fireEvent.click(await within(card).findByRole('menuitem', { name: /Mark read/i }));

    await waitFor(() => {
      expect(screen.queryByText('Ended: merged (#214)')).toBeNull();
    });
    expect(screen.getByText(/2 unread/)).toBeInTheDocument();
  });

  it('counts a read of a report outside the loaded page', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    // The runtime also announces reads of the older page this view has not loaded; the server's
    // unread count includes them, so the unread count has to move.
    eventHandler?.('report.read', { report_id: 'rep-0120' });
    await waitFor(() => {
      expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    });
  });

  it('keeps a pending read counted when a reload lands before its response', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    let resolveRead: (() => void) | undefined;
    vi.spyOn(apiClient, 'markReportRead').mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveRead = resolve;
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    eventHandler?.('report.read', { report_id: 'rep-0142' });
    await waitFor(() => {
      expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    });

    // A read event that names no report reloads the list from the server, which already counts
    // the read the pending request will answer.
    eventHandler?.('report.read', {});
    await waitFor(() => {
      expect(screen.getByText(/3 unread/)).toBeInTheDocument();
    });

    if (!resolveRead) throw new Error('the pending read request was never created');
    resolveRead();
    await tick();
    await tick();

    // The response answers a read the reloaded count already includes.
    expect(screen.getByText(/3 unread/)).toBeInTheDocument();
  });

  it('counts a repeated read event only once', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    eventHandler?.('report.read', { report_id: 'rep-0142' });
    await waitFor(() => {
      expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    });

    // A replayed event names a read that is already accounted for.
    eventHandler?.('report.read', { report_id: 'rep-0142' });
    await tick();
    await tick();

    expect(screen.getByText(/2 unread/)).toBeInTheDocument();
  });

  it('counts a read event again after the view reloads the list', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    eventHandler?.('report.read', { report_id: 'rep-0142' });
    await waitFor(() => {
      expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    });

    // A read event that names no report makes the view load the list from the server again, which
    // forgets the reads it had tracked.
    eventHandler?.('report.read', {});
    await waitFor(() => {
      expect(screen.getByText(/3 unread/)).toBeInTheDocument();
    });
    await screen.findByText('Ended: merged (#214)');

    eventHandler?.('report.read', { report_id: 'rep-0142' });
    await waitFor(() => {
      expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    });
  });

  it('clears the filter on Escape', async () => {
    render(ReportsView);
    const input = await typeFilter('relay');

    await fireEvent.keyDown(input, { key: 'Escape' });

    expect((input as HTMLInputElement).value).toBe('');
    expect(screen.getByText('Ended: no change')).toBeInTheDocument();
  });

  it('marks only the matching reports read and keeps the hidden rows when filter is active', async () => {
    let served: ReportsPage = mockPage;
    vi.spyOn(apiClient, 'getReports').mockImplementation(() => Promise.resolve(served));
    vi.spyOn(apiClient, 'markAllReportsRead').mockImplementation(async () => {
      // The server no longer serves the one report this act read.
      served = {
        items: mockReports.filter((report) => report.id !== 'rep-0142'),
        unread_count: 2,
        has_older: false,
        older_count: 0,
      };
      return { marked_count: 1 };
    });

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    await typeFilter('relay-hardening');
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();
    expect(screen.queryByText('Onboarded')).toBeNull();

    const markAllBtn = screen.getByRole('button', { name: /Mark all shown read/i });
    await fireEvent.click(markAllBtn);

    expect(apiClient.markAllReportsRead).toHaveBeenCalledWith(['rep-0142']);

    // The reload takes the count from the server; the rows the filter hides come back with it.
    await waitFor(() => {
      expect(screen.getByTestId('unread-count')).toHaveTextContent('2 unread reports');
    });

    await fireEvent.click(screen.getByTestId('filter-clear'));

    expect(screen.queryByText('Ended: merged (#214)')).toBeNull();
    expect(screen.getByText('Ended: no change')).toBeInTheDocument();
    expect(screen.getByText('Onboarded')).toBeInTheDocument();
  });

  it('adds and removes the show-text control when window resizes', async () => {
    const bodyReport: ReportItem = {
      id: 'rep-0999',
      from: { project: 'pm-mcp-clients', plan: 'fix-relay' },
      kind: 'plan.ended',
      subject: 'Long body test',
      body: 'Line 1\nLine 2\nLine 3',
      created_at: '2026-10-16T11:20:00Z',
    };
    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [bodyReport],
      unread_count: 1,
      has_older: false,
      older_count: 0,
    });

    render(ReportsView);
    await screen.findByText('Long body test');

    const bodyNode = document.querySelector('.report-card__body') as HTMLElement;
    expect(bodyNode).not.toBeNull();

    Object.defineProperty(bodyNode, 'scrollHeight', { configurable: true, value: 80 });
    Object.defineProperty(bodyNode, 'clientHeight', { configurable: true, value: 30 });
    window.dispatchEvent(new Event('resize'));
    await tick();

    expect(screen.getByRole('button', { name: /Show the text/i })).toBeInTheDocument();

    Object.defineProperty(bodyNode, 'clientHeight', { configurable: true, value: 80 });
    window.dispatchEvent(new Event('resize'));
    await tick();

    expect(screen.queryByRole('button', { name: /Show the text/i })).toBeNull();

    Object.defineProperty(bodyNode, 'clientHeight', { configurable: true, value: 30 });
    window.dispatchEvent(new Event('resize'));
    await tick();

    expect(screen.getByRole('button', { name: /Show the text/i })).toBeInTheDocument();
  });

  it('draws no filter container when there are no reports to filter', async () => {
    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [],
      unread_count: 0,
      has_older: false,
      older_count: 0,
    });

    render(ReportsView);
    await waitFor(() => {
      expect(screen.getByText('Nothing new')).toBeInTheDocument();
    });

    expect(screen.queryByTestId('triage-filters')).toBeNull();
  });

  it('has only one h1 heading on screen when detail is open beside the list', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const detailBtn = screen.getAllByRole('button', { name: /Show the details/i })[0];
    await fireEvent.click(detailBtn);

    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    const h1Elements = document.querySelectorAll('h1');
    expect(h1Elements).toHaveLength(1);
    expect(h1Elements[0]).toHaveClass('reports-heading');
  });

  it('gives distinct accessible names carrying the row to the show-text control and the row menu', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const menuBtn = screen.getByRole('button', {
      name: /Report actions: Ended: merged \(#214\)/i,
    });
    expect(menuBtn).toBeInTheDocument();

    const toggleBtn = screen.getByRole('button', {
      name: /Show the text: Ended: merged \(#214\)/i,
    });
    expect(toggleBtn).toBeInTheDocument();

    expect(menuBtn.getAttribute('aria-label')).not.toBe(toggleBtn.getAttribute('aria-label'));
  });

  it('moves focus to the next report when one is marked read (D2b)', async () => {
    vi.spyOn(apiClient, 'markReportRead').mockResolvedValue(undefined);
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const firstCard = screen.getByText('Ended: merged (#214)').closest('.report-card') as HTMLElement;
    const secondCard = screen.getByText('Ended: no change').closest('.report-card') as HTMLElement;
    const secondCardTrigger = within(secondCard).getByRole('button', { name: /Report actions/i });

    const firstCardTrigger = within(firstCard).getByRole('button', { name: /Report actions/i });
    await fireEvent.click(firstCardTrigger);
    const markReadItem = await within(firstCard).findByRole('menuitem', { name: /Mark read/i });
    await fireEvent.click(markReadItem);

    await waitFor(() => {
      expect(screen.queryByText('Ended: merged (#214)')).toBeNull();
      expect(secondCardTrigger).toHaveFocus();
    });
  });

  it('moves focus to heading when the only report is marked read (D2b)', async () => {
    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [
        {
          id: 'rep-only',
          from: { project: 'pm-mcp-clients' },
          kind: 'project.onboarded',
          subject: 'Only report',
          created_at: '2026-10-16T11:20:00Z',
        },
      ],
      unread_count: 1,
      has_older: false,
      older_count: 0,
    });
    vi.spyOn(apiClient, 'markReportRead').mockResolvedValue(undefined);

    render(ReportsView);
    await screen.findByText('Only report');

    const card = screen.getByText('Only report').closest('.report-card') as HTMLElement;
    await fireEvent.click(within(card).getByRole('button', { name: /Report actions/i }));
    await fireEvent.click(await within(card).findByRole('menuitem', { name: /Mark read/i }));

    await waitFor(() => {
      expect(screen.queryByText('Only report')).toBeNull();
      expect(document.querySelector('.reports-heading')).toHaveFocus();
    });
  });

  it('falls back to heading when successor row becomes disconnected before focus moves (D2b)', async () => {
    const r1: ReportItem = {
      id: 'rep-01',
      from: { project: 'pm-mcp-clients' },
      kind: 'project.onboarded',
      subject: 'Report 1',
      created_at: '2026-10-16T11:20:00Z',
    };
    const r2: ReportItem = {
      id: 'rep-02',
      from: { project: 'pm-mcp-clients' },
      kind: 'project.onboarded',
      subject: 'Report 2',
      created_at: '2026-10-16T11:21:00Z',
    };
    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [r1, r2],
      unread_count: 2,
      has_older: false,
      older_count: 0,
    });
    vi.spyOn(apiClient, 'markReportRead').mockImplementation(async () => {
      const card2 = document.querySelector('[data-report-id="rep-02"]');
      card2?.remove();
    });

    render(ReportsView);
    await screen.findByText('Report 1');

    const card1 = screen.getByText('Report 1').closest('.report-card') as HTMLElement;
    await fireEvent.click(within(card1).getByRole('button', { name: /Report actions/i }));
    await fireEvent.click(await within(card1).findByRole('menuitem', { name: /Mark read/i }));

    await waitFor(() => {
      expect(screen.queryByText('Only report')).toBeNull();
      expect(document.querySelector('.reports-heading')).toHaveFocus();
    });
  });

  it('groups reports with machine sentinel correctly without colliding with __machine__ project or system.* kind (D2c)', async () => {
    const realMachineProjectReport: ReportItem = {
      id: 'rep-m1',
      from: { project: '__machine__' },
      kind: 'project.onboarded',
      subject: 'Real project named __machine__ is onboarded',
      created_at: '2026-10-16T10:00:00Z',
    };
    const systemKindWithProjectReport: ReportItem = {
      id: 'rep-m2',
      from: { project: '__machine__' },
      kind: 'system.cleanup_due',
      subject: 'System cleanup due on machine',
      created_at: '2026-10-16T11:00:00Z',
    };
    const normalProjectSystemReport: ReportItem = {
      id: 'rep-m3',
      from: { project: 'my-project' },
      kind: 'system.drain_started',
      subject: 'Drain started',
      created_at: '2026-10-16T12:00:00Z',
    };
    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [realMachineProjectReport, systemKindWithProjectReport, normalProjectSystemReport],
      unread_count: 3,
      has_older: false,
      older_count: 0,
    });

    render(ReportsView);
    await screen.findByText('Real project named __machine__ is onboarded');

    const projectBtns = screen.getAllByRole('button', { name: /Open the project __machine__/i });
    expect(projectBtns.length).toBeGreaterThan(0);
    const projectBtn = projectBtns[0];
    expect(projectBtn).toBeInTheDocument();

    expect(screen.getByText('This machine')).toBeInTheDocument();

    const machineSection = screen.getByText('This machine').closest('.project-section') as HTMLElement;
    expect(within(machineSection).getByText('System cleanup due on machine')).toBeInTheDocument();
    expect(within(machineSection).getByText('Drain started')).toBeInTheDocument();
    expect(within(machineSection).queryByText('Real project named __machine__ is onboarded')).toBeNull();

    const realProjectSection = projectBtn.closest('.project-section') as HTMLElement;
    expect(within(realProjectSection).getByText('Real project named __machine__ is onboarded')).toBeInTheDocument();
  });

  it('keeps the open report mounted and the filter text across a reload (D1)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const input = await typeFilter('relay');
    expect((input as HTMLInputElement).value).toBe('relay');

    const detailBtn = screen.getAllByRole('button', { name: /Show the details/i })[0];
    await fireEvent.click(detailBtn);
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    const reportTitleBefore = document.querySelector('.report-title');

    eventHandler?.('report.sent', {});
    await tick();

    await waitFor(() => {
      expect(screen.getByRole('searchbox')).toHaveValue('relay');
      expect(document.querySelector('.report-title')).not.toBeNull();
      expect(document.querySelector('.report-title')).toBe(reportTitleBefore);
    });
  });

  it('has exactly one h1 heading and h2 section headings with tokens at 390 px (N2)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fullRelayCompletion());
    window.innerWidth = 390;
    window.dispatchEvent(new Event('resize'));

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const detailBtn = screen.getAllByRole('button', { name: /Show the details/i })[0];
    await fireEvent.click(detailBtn);

    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    const h1Elements = document.querySelectorAll('h1');
    expect(h1Elements).toHaveLength(1);
    expect(h1Elements[0]).toHaveClass('report-title');
    expect(h1Elements[0]).toHaveTextContent('fix-relay');

    // Section headings are h2 (Deliverables, Replanning, Decisions, Tokens)
    expect(screen.getByRole('heading', { level: 2, name: /Deliverables/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /Re-planning/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /Decisions/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /Tokens/i })).toBeInTheDocument();

    // Token breakdown subheadings are h3
    expect(screen.getByRole('heading', { level: 3, name: /By phase/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3, name: /By role/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3, name: /By model/i })).toBeInTheDocument();
  });

  it('renders h1 page title, h2 report title, and h3 deliverables and decisions at wide layout (N2)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fullRelayCompletion());
    window.innerWidth = 1280;
    window.dispatchEvent(new Event('resize'));

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const detailBtn = screen.getAllByRole('button', { name: /Show the details/i })[0];
    await fireEvent.click(detailBtn);

    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    // Page title is h1
    const h1Elements = document.querySelectorAll('h1');
    expect(h1Elements).toHaveLength(1);
    expect(h1Elements[0]).toHaveTextContent('Reports');

    // Report title is h2
    const reportTitle = document.querySelector('.report-title');
    expect(reportTitle?.tagName.toLowerCase()).toBe('h2');
    expect(reportTitle).toHaveTextContent('fix-relay');

    // Deliverables, Decisions, Tokens section titles are h3
    expect(screen.getByRole('heading', { level: 3, name: /Deliverables/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3, name: /Decisions/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 3, name: /Tokens/i })).toBeInTheDocument();

    // Token breakdown subheadings are h4
    expect(screen.getByRole('heading', { level: 4, name: /By phase/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 4, name: /By role/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 4, name: /By model/i })).toBeInTheDocument();
  });

  it('has exactly one h1 heading on screen at 390 px while detail is loading (D3)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockImplementation(() => new Promise(() => {}));
    window.innerWidth = 390;
    window.dispatchEvent(new Event('resize'));

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const detailBtn = screen.getAllByRole('button', { name: /Show the details/i })[0];
    await fireEvent.click(detailBtn);

    const h1Elements = document.querySelectorAll('h1');
    expect(h1Elements).toHaveLength(1);
    expect(h1Elements[0]).toHaveTextContent(/Loading completion report/i);
  });

  it('has exactly one h1 heading on screen at 390 px while detail has error (D3)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockRejectedValue(new Error('Network error'));
    window.innerWidth = 390;
    window.dispatchEvent(new Event('resize'));

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const detailBtn = screen.getAllByRole('button', { name: /Show the details/i })[0];
    await fireEvent.click(detailBtn);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    const h1Elements = document.querySelectorAll('h1');
    expect(h1Elements).toHaveLength(1);
    expect(h1Elements[0]).toHaveTextContent(/The runtime refused the request/i);
  });

  it('treats a punctuation-only query as unfiltered and shows what the count says (D1)', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    await typeFilter('...');
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();
    expect(screen.getByText('Ended: no change')).toBeInTheDocument();
    expect(screen.getByText('Onboarded')).toBeInTheDocument();
    expect(screen.queryByTestId('filter-results')).toBeNull();

    const markAllBtn = screen.getByRole('button', { name: /Mark all shown read/i });
    await fireEvent.click(markAllBtn);
    expect(apiClient.markAllReportsRead).toHaveBeenCalledWith(['rep-0142', 'rep-0139', 'rep-0131']);
  });

  it('posts the shown ids and no all on unfiltered mark-all (D1)', async () => {
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const markAllBtn = screen.getByRole('button', { name: /Mark all shown read/i });
    await fireEvent.click(markAllBtn);

    expect(apiClient.markAllReportsRead).toHaveBeenCalledWith(['rep-0142', 'rep-0139', 'rep-0131']);
    expect(apiClient.markAllReportsRead).not.toHaveBeenCalledWith(undefined);
  });

  it('keeps a report that arrived after the list was drawn listed and unread on mark-all (D1)', async () => {
    const arrivedReport: ReportItem = {
      id: 'rep-arrived',
      from: { project: 'pm-mcp-clients', plan: 'fix-relay' },
      kind: 'plan.ended',
      subject: 'Arrived during request',
      created_at: '2026-10-16T12:00:00Z',
    };
    let resolveMarkAll: (() => void) | undefined;
    let readCalls = 0;
    vi.spyOn(apiClient, 'getReports').mockImplementation(() => {
      readCalls += 1;
      if (readCalls === 1) return Promise.resolve(mockPage);
      // The arrival is not among the ids the mark-all read, so the server still serves it.
      return Promise.resolve({
        items: [arrivedReport],
        unread_count: 1,
        has_older: false,
        older_count: 0,
      });
    });
    vi.spyOn(apiClient, 'markAllReportsRead').mockImplementation(
      () =>
        new Promise<{ marked_count: number }>((resolve) => {
          resolveMarkAll = () => resolve({ marked_count: 3 });
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const markAllBtn = screen.getByRole('button', { name: /Mark all shown read/i });
    await fireEvent.click(markAllBtn);

    eventHandler?.('report.sent', { report: arrivedReport });
    await tick();

    if (!resolveMarkAll) throw new Error('markAllReportsRead was never called');
    resolveMarkAll();
    await tick();
    await tick();

    await waitFor(() => {
      expect(screen.getByText('Arrived during request')).toBeInTheDocument();
    });
    expect(screen.getByTestId('unread-count')).toHaveTextContent('1 unread report');
  });

  it('keeps the load-older control while more than one page of unread reports remains after a mark-all (D1)', async () => {
    const shownFive = [1, 2, 3, 4, 5].map(unreadFixture);
    let served: ReportsPage = {
      items: shownFive,
      unread_count: 12,
      has_older: true,
      older_count: 7,
    };
    vi.spyOn(apiClient, 'getReports').mockImplementation(() => Promise.resolve(served));
    vi.spyOn(apiClient, 'markAllReportsRead').mockImplementation(async () => {
      // Seven unread reports remain, more than one page of five; the server says so.
      served = {
        items: [6, 7, 8, 9, 10].map(unreadFixture),
        unread_count: 7,
        has_older: true,
        older_count: 2,
      };
      return { marked_count: 5 };
    });

    render(ReportsView);
    await screen.findByText('Unread report 1');
    expect(screen.getByTestId('unread-count')).toHaveTextContent('12 unread reports');
    expect(screen.getByRole('button', { name: /Show 7 older/i })).toBeInTheDocument();

    await fireEvent.click(screen.getByRole('button', { name: /Mark all shown read/i }));

    await waitFor(() => {
      expect(screen.getByTestId('unread-count')).toHaveTextContent('7 unread reports');
    });
    expect(screen.getByRole('button', { name: /Show 2 older/i })).toBeInTheDocument();
  });

  it('keeps the retained read marker when mark-all request fails (D1)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const detailBtn = screen.getAllByRole('button', { name: /Show the details/i })[0];
    await fireEvent.click(detailBtn);
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
      expect(screen.getByTestId('report-read-state')).toBeInTheDocument();
    });

    vi.spyOn(apiClient, 'markAllReportsRead').mockRejectedValue(new Error('mark all failed'));
    const markAllBtn = screen.getByRole('button', { name: /Mark all shown read/i });
    await fireEvent.click(markAllBtn);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('mark all failed');
    });

    expect(screen.getByTestId('report-read-state')).toBeInTheDocument();
  });

  it('keeps unread count equal to server count when filtered mark-all follows a reload with retained read (D1)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const detailBtn = screen.getAllByRole('button', { name: /Show the details/i })[0];
    await fireEvent.click(detailBtn);
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
      expect(screen.getByTestId('unread-count')).toHaveTextContent('2 unread reports');
    });

    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [
        {
          id: 'rep-0139',
          from: { project: 'pm-mcp-clients', plan: 'docs-typo' },
          kind: 'plan.ended',
          subject: 'Ended: no change',
          created_at: '2026-10-16T09:40:00Z',
        },
        {
          id: 'rep-0131',
          from: { project: 'pm-mcp-core' },
          kind: 'project.onboarded',
          subject: 'Onboarded',
          created_at: '2026-10-15T18:40:00Z',
        },
      ],
      unread_count: 2,
      has_older: false,
      older_count: 0,
    });
    eventHandler?.('report.read', {});
    await tick();
    await waitFor(() => {
      expect(screen.getByTestId('unread-count')).toHaveTextContent('2 unread reports');
    });

    await typeFilter('relay');
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();

    const markAllBtn = screen.getByRole('button', { name: /Mark all shown read/i });
    await fireEvent.click(markAllBtn);

    expect(screen.getByTestId('unread-count')).toHaveTextContent('2 unread reports');
  });

  it('guards against double click and sends only one mark-all request (D1)', async () => {
    let resolveRequest: (() => void) | undefined;
    vi.spyOn(apiClient, 'markAllReportsRead').mockImplementation(
      () =>
        new Promise<{ marked_count: number }>((resolve) => {
          resolveRequest = () => resolve({ marked_count: 3 });
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const markAllBtn = screen.getByRole('button', { name: /Mark all shown read/i });
    await fireEvent.click(markAllBtn);
    await fireEvent.click(markAllBtn);

    expect(apiClient.markAllReportsRead).toHaveBeenCalledTimes(1);

    if (!resolveRequest) throw new Error('markAllReportsRead was never called');
    resolveRequest();
    await tick();
  });

  it('keeps the mark-all button focusable with aria-disabled while the request runs (F1)', async () => {
    let resolveRequest: (() => void) | undefined;
    vi.spyOn(apiClient, 'markAllReportsRead').mockImplementation(
      () =>
        new Promise<{ marked_count: number }>((resolve) => {
          resolveRequest = () => resolve({ marked_count: 3 });
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const markAllBtn = screen.getByRole('button', { name: /Mark all shown read/i });
    markAllBtn.focus();
    await fireEvent.click(markAllBtn);

    // In flight the button is announced as unavailable, but it stays a native enabled button,
    // so it keeps its place in the tab order and its focus rather than dropping out of both.
    expect(markAllBtn).toHaveAttribute('aria-disabled', 'true');
    expect(markAllBtn).toHaveAttribute('aria-busy', 'true');
    expect(markAllBtn).not.toBeDisabled();
    expect(markAllBtn).toHaveFocus();

    if (!resolveRequest) throw new Error('markAllReportsRead was never called');
    resolveRequest();
    await waitFor(() => {
      const settled = screen.getByRole('button', { name: /Mark all shown read/i });
      expect(settled).not.toHaveAttribute('aria-disabled', 'true');
    });
  });

  it('shows a visible in-flight cue on the mark-all button while the request runs (F3)', async () => {
    let resolveRequest: (() => void) | undefined;
    vi.spyOn(apiClient, 'markAllReportsRead').mockImplementation(
      () =>
        new Promise<{ marked_count: number }>((resolve) => {
          resolveRequest = () => resolve({ marked_count: 3 });
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    const markAllBtn = screen.getByRole('button', { name: /Mark all shown read/i });
    expect(screen.queryByTestId('mark-all-busy')).toBeNull();

    await fireEvent.click(markAllBtn);

    const cue = screen.getByTestId('mark-all-busy');
    expect(cue).toBeVisible();
    expect(markAllBtn).toContainElement(cue);

    if (!resolveRequest) throw new Error('markAllReportsRead was never called');
    resolveRequest();
    await waitFor(() => {
      expect(screen.queryByTestId('mark-all-busy')).toBeNull();
    });
  });

  it('retains the open report beside the detail during a filtered mark-all and drops it when detail closes (D3e)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    let served: ReportsPage = mockPage;
    vi.spyOn(apiClient, 'getReports').mockImplementation(() => Promise.resolve(served));
    vi.spyOn(apiClient, 'markAllReportsRead').mockImplementation(async () => {
      // The reload no longer serves the report this act read.
      served = {
        items: mockReports.filter((report) => report.id !== 'rep-0142'),
        unread_count: 2,
        has_older: false,
        older_count: 0,
      };
      return { marked_count: 1 };
    });

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    // Filter to relay items
    await typeFilter('relay');
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();

    // Open detail for fix-relay
    const detailBtn = screen.getAllByRole('button', { name: /Show the details/i })[0];
    await fireEvent.click(detailBtn);
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    // Mark all matching read
    const markAllBtn = screen.getByRole('button', { name: /Mark all shown read/i });
    await fireEvent.click(markAllBtn);

    // Filtered mark-all called with matching IDs
    expect(apiClient.markAllReportsRead).toHaveBeenCalledWith(['rep-0142']);

    // The reload takes the server's count, and the retained row stays beside the open detail.
    await waitFor(() => {
      expect(screen.getByTestId('unread-count')).toHaveTextContent('2 unread reports');
    });
    expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();

    // Close detail
    const backBtn = screen.getByRole('button', { name: /^Back$/i });
    await fireEvent.click(backBtn);

    // Retained row is now flushed from list
    await waitFor(() => {
      expect(screen.queryByText('Ended: merged (#214)')).toBeNull();
    });
  });

  it('keeps a counted read read when the mark-all request fails (D1 L3-2)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    // A report opened through the route is retained by the mark-all itself: no read of its own
    // retained it, which is the marker the old rollback deleted.
    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: 'fix-relay' }));
    let rejectMarkAll: (error: Error) => void = () => {};
    vi.spyOn(apiClient, 'markAllReportsRead').mockImplementation(
      () =>
        new Promise<{ marked_count: number }>((_, reject) => {
          rejectMarkAll = reject;
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    await fireEvent.click(screen.getByRole('button', { name: /Mark all shown read/i }));

    // The runtime reports the read of the retained row while the request is still in flight.
    eventHandler?.('report.read', { report_id: 'rep-0142' });
    await waitFor(() => {
      expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    });

    rejectMarkAll(new Error('mark all failed'));
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('mark all failed');
    });

    const card = document.querySelector('[data-report-id="rep-0142"]') as HTMLElement;
    expect(card).not.toBeNull();
    expect(card).toHaveAttribute('data-unread', 'false');
    expect(within(card).getByTestId('report-read-state')).toBeInTheDocument();
    expect(screen.getByText(/2 unread/)).toBeInTheDocument();
  });

  it('keeps the retained row and its counted read through a reload before the failed mark-all (D1)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: 'fix-relay' }));
    let served: ReportsPage = mockPage;
    vi.spyOn(apiClient, 'getReports').mockImplementation(() => Promise.resolve(served));
    let rejectMarkAll: (error: Error) => void = () => {};
    vi.spyOn(apiClient, 'markAllReportsRead').mockImplementation(
      () =>
        new Promise<{ marked_count: number }>((_, reject) => {
          rejectMarkAll = reject;
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    await fireEvent.click(screen.getByRole('button', { name: /Mark all shown read/i }));
    eventHandler?.('report.read', { report_id: 'rep-0142' });
    await waitFor(() => {
      expect(screen.getByText(/2 unread/)).toBeInTheDocument();
    });

    // The server no longer serves the retained row, and a reload lands before the response.
    served = {
      items: mockReports.filter((report) => report.id !== 'rep-0142'),
      unread_count: 2,
      has_older: false,
      older_count: 0,
    };
    (apiClient.getReports as ReturnType<typeof vi.fn>).mockClear();
    eventHandler?.('report.read', {});
    await waitFor(() => expect(apiClient.getReports).toHaveBeenCalledTimes(1));

    rejectMarkAll(new Error('mark all failed'));
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('mark all failed');
    });

    const cards = Array.from(document.querySelectorAll('.report-card')).map((card) =>
      card.getAttribute('data-report-id'),
    );
    expect(cards).toEqual(['rep-0142', 'rep-0139', 'rep-0131']);
    expect(document.querySelector('[data-report-id="rep-0142"]')).toHaveAttribute(
      'data-unread',
      'false',
    );
  });

  it('keeps a reload in flight before the mark-all response from counting a read twice (D1)', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());
    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: 'fix-relay' }));
    let served: ReportsPage = mockPage;
    vi.spyOn(apiClient, 'getReports').mockImplementation(() => Promise.resolve(served));
    let rejectMarkAll: (error: Error) => void = () => {};
    vi.spyOn(apiClient, 'markAllReportsRead').mockImplementation(
      () =>
        new Promise<{ marked_count: number }>((_, reject) => {
          rejectMarkAll = reject;
        }),
    );

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    await waitFor(() => {
      expect(document.querySelector('.report-title')).not.toBeNull();
    });

    await fireEvent.click(screen.getByRole('button', { name: /Mark all shown read/i }));
    // The retained row and a row outside the detail are read while the request runs.
    eventHandler?.('report.read', { report_id: 'rep-0142' });
    eventHandler?.('report.read', { report_id: 'rep-0139' });
    await waitFor(() => {
      expect(screen.getByText(/1 unread/)).toBeInTheDocument();
    });

    // A reload in flight lands before the response; the server has counted both reads and serves
    // the one unread row that is left.
    served = {
      items: mockReports.filter((report) => report.id === 'rep-0131'),
      unread_count: 1,
      has_older: false,
      older_count: 0,
    };
    (apiClient.getReports as ReturnType<typeof vi.fn>).mockClear();
    eventHandler?.('report.read', {});
    await waitFor(() => expect(apiClient.getReports).toHaveBeenCalledTimes(1));

    rejectMarkAll(new Error('mark all failed'));
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('mark all failed');
    });

    expect(document.querySelector('[data-report-id="rep-0142"]')).toHaveAttribute(
      'data-unread',
      'false',
    );
    expect(screen.getByText(/1 unread/)).toBeInTheDocument();
    // The reload kept the read in flight counted, so a repeated event for it moves no count.
    eventHandler?.('report.read', { report_id: 'rep-0139' });
    await tick();
    await tick();
    expect(screen.getByText(/1 unread/)).toBeInTheDocument();
  });
});
