import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import { tick } from 'svelte';
import ReportsView from './ReportsView.svelte';
import { runtimeStore } from '../stores/runtime';
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
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((handler) => {
      eventHandler = handler;
      return () => {};
    });
  });

  /** @returns the completion report the split detail fetches for `fix-relay` */
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
    expect(screen.getByText('today 09:02 · sent today 09:40')).toBeInTheDocument();
  });

  it('follows the route report plan while the reports route stays mounted', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());

    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: 'fix-relay' }));

    render(ReportsView);

    await waitFor(() => {
      expect(document.querySelector('.completion-report')).not.toBeNull();
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
      expect(document.querySelector('.completion-report')).not.toBeNull();
    });
    expect(get(runtimeStore).selectedReportDetailPlanId).toBe('fix-relay');

    runtimeStore.update((s) => ({ ...s, selectedReportDetailPlanId: null }));
    await waitFor(() => {
      expect(document.querySelector('.completion-report')).toBeNull();
    });
  });

  it('marks a single report read when clicking Mark read', async () => {
    render(ReportsView);

    await waitFor(() => {
      expect(screen.getByText('Ended: merged (#214)')).toBeInTheDocument();
    });

    const markReadButtons = screen.getAllByRole('button', { name: /Mark read/i });
    await fireEvent.click(markReadButtons[0]);

    expect(apiClient.markReportRead).toHaveBeenCalledWith('rep-0142');
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

  it('renders empty state when there are no unread reports', async () => {
    vi.spyOn(apiClient, 'getReports').mockResolvedValue({
      items: [],
      unread_count: 0,
      has_older: false,
      older_count: 0,
    });

    render(ReportsView);

    await waitFor(() => {
      expect(
        screen.getByText('Nothing new. Reports you have read are not shown any more.'),
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

    const card = (await screen.findByText(/relay-docs waits/)).closest('.report-card');
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

    await typeFilter('yesterday');
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

  it('switches layouts and remembers the choice across a remount', async () => {
    const { unmount } = render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    expect(document.querySelector('.reports-view')).toHaveAttribute('data-layout', 'comfortable');

    await fireEvent.click(screen.getByTestId('layout-compact'));
    expect(document.querySelector('.reports-view')).toHaveAttribute('data-layout', 'compact');
    expect(localStorage.getItem('pm_view_layout_reports')).toBe('compact');

    unmount();
    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');
    expect(document.querySelector('.reports-view')).toHaveAttribute('data-layout', 'compact');
    expect(screen.getByTestId('layout-compact')).toHaveAttribute('aria-pressed', 'true');
  });

  it('keeps the list beside the detail in split mode and marks the active card', async () => {
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(fixRelayCompletion());

    render(ReportsView);
    await screen.findByText('Ended: merged (#214)');

    await fireEvent.click(screen.getByTestId('layout-split'));
    const details = screen.getAllByRole('button', { name: /Show the details/i });
    await fireEvent.click(details[0]);

    await waitFor(() => {
      expect(document.querySelector('.completion-report')).not.toBeNull();
    });
    expect(document.querySelector('.reports-tree')).not.toBeNull();
    const active = document.querySelector('.report-card--active');
    expect(active).not.toBeNull();
    expect(active).toHaveAttribute('aria-current', 'true');
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
    await fireEvent.click(screen.getByTestId('layout-split'));
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);

    await waitFor(() => {
      expect(document.querySelector('.completion-report')).not.toBeNull();
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
    await fireEvent.click(screen.getByTestId('layout-split'));
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
    await fireEvent.click(screen.getByTestId('layout-split'));
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);
    await waitFor(() => {
      expect(document.querySelector('.completion-report')).not.toBeNull();
    });

    // The operator closes the detail before the read request settles.
    await fireEvent.click(screen.getByRole('button', { name: /Back to reports/i }));
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
    await fireEvent.click(screen.getByTestId('layout-split'));
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);
    await waitFor(() => {
      expect(document.querySelector('.completion-report')).not.toBeNull();
    });
    await fireEvent.click(screen.getByRole('button', { name: /Back to reports/i }));
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
    await fireEvent.click(screen.getByTestId('layout-split'));
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);
    await waitFor(() => {
      expect(document.querySelector('.completion-report')).not.toBeNull();
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
    await fireEvent.click(screen.getByTestId('layout-split'));
    await fireEvent.click(screen.getAllByRole('button', { name: /Show the details/i })[0]);
    await waitFor(() => {
      expect(document.querySelector('.completion-report')).not.toBeNull();
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

    await fireEvent.click(screen.getAllByRole('button', { name: /Mark read/i })[0]);

    await waitFor(() => {
      expect(screen.queryByText('Ended: merged (#214)')).toBeNull();
    });
    expect(screen.getByText(/2 unread/)).toBeInTheDocument();
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
});
