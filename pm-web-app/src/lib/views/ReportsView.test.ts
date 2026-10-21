import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
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
    const completion = {
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
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue(completion);

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
    vi.spyOn(apiClient, 'getPlanCompletion').mockResolvedValue({
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
    } as unknown as PlanCompletionReport);

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
});
