import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import OverviewView from './OverviewView.svelte';
import { runtimeStore } from '../stores/runtime';
import {
  apiClient,
  type ProjectOverviewResponse,
  type ProjectDetailResponse,
  type EndedWorkPage,
} from '../api/client';

describe('OverviewView component', () => {
  const mockProjects: ProjectOverviewResponse = {
    projects: [
      {
        project_id: 'pm-mcp-clients',
        display_name: 'pm-mcp-clients',
        active_plans_count: 3,
        active_epics_count: 1,
        needs_operator_count: 2,
        needs_operator_summary: 'decision',
        setup_needs_operator: false,
        last_activity_at: '2026-10-16T15:36:00Z',
      },
      {
        project_id: 'pm-mcp-core',
        display_name: 'pm-mcp-core',
        active_plans_count: 2,
        active_epics_count: 0,
        needs_operator_count: 0,
        needs_operator_summary: null,
        setup_needs_operator: false,
        last_activity_at: '2026-10-16T15:29:00Z',
      },
      {
        project_id: 'plan-marshall',
        display_name: 'plan-marshall',
        active_plans_count: 0,
        active_epics_count: 1,
        needs_operator_count: 1,
        needs_operator_summary: 'setup',
        setup_needs_operator: true,
        last_activity_at: '2026-10-16T14:38:00Z',
      },
    ],
  };

  const mockDetailClients: ProjectDetailResponse = {
    project_id: 'pm-mcp-clients',
    display_name: 'pm-mcp-clients',
    plans: [
      {
        plan_id: 'pln-console-history',
        title: 'Console history of answers',
        current_phase: 'refine',
        phase_state: 'waiting',
        needs_operator_kind: 'decision',
        needs_operator_count: 1,
        phase_entered_at: '2026-10-16T15:26:00Z',
        tasks_done: 0,
        tasks_total: 0,
      },
      {
        plan_id: 'pln-overview-resource',
        title: 'Overview resource of the runtime',
        current_phase: 'execute',
        phase_state: 'running',
        tasks_done: 7,
        tasks_total: 12,
        epic_id: 'epc-client-work',
        epic_title: 'client work',
        pull_request_number: 214,
        pull_request_url: 'https://github.com/plan-marshall/pm-mcp-clients/pull/214',
      },
      {
        plan_id: 'pln-message-catalogue',
        title: 'Message catalogue',
        current_phase: 'outline',
        phase_state: 'pausing',
        epic_id: 'epc-client-work',
        epic_title: 'client work',
        off_track_signal: 'repeating',
      },
    ],
    epics: [
      {
        epic_id: 'epc-client-work',
        title: 'client work',
        running_plans_count: 2,
        staged_plans_count: 3,
        plans_done: 0,
        plans_total: 5,
        plans: [
          {
            plan_id: 'pln-overview-resource',
            title: 'Overview resource of the runtime',
            current_phase: 'execute',
            phase_state: 'running',
          },
          {
            plan_id: 'pln-message-catalogue',
            title: 'Message catalogue',
            current_phase: 'outline',
            phase_state: 'pausing',
          },
        ],
      },
    ],
    alongside_landings: [
      {
        unit: 'remediation.ci-failure',
        state: 'failed',
        kind: 'unit',
        hold_kind: 'landing_failed',
      },
      {
        count: 2,
        state: 'running',
        kind: 'removal',
      },
    ],
  };

  const mockDetailCore: ProjectDetailResponse = {
    project_id: 'pm-mcp-core',
    display_name: 'pm-mcp-core',
    plans: [
      {
        plan_id: 'pln-session-log',
        title: 'Session log and generations',
        current_phase: 'execute',
        phase_state: 'running',
        pull_request_number: 198,
        pull_request_url: 'https://github.com/plan-marshall/pm-mcp-core/pull/198',
      },
    ],
    epics: [],
    alongside_landings: [],
  };

  const mockEndedPage: EndedWorkPage = {
    project_id: 'pm-mcp-clients',
    page: 1,
    total_pages: 2,
    has_newer: false,
    has_older: true,
    items: [
      {
        id: 'pln-relay-drops',
        kind: 'plan',
        title: 'Relay drops events on reconnect',
        outcome: 'merged',
        ended_at: '2026-10-14T10:00:00Z',
        pull_request_number: 211,
        pull_request_url: 'https://github.com/plan-marshall/pm-mcp-clients/pull/211',
      },
      {
        id: 'pln-fix-loss',
        kind: 'plan',
        title: "Fix the relay's event loss",
        outcome: 'cancelled',
        reason: 'the approach is replaced by plan relay-v2',
        ended_at: '2026-10-14T09:00:00Z',
      },
      {
        id: 'epc-bootstrap',
        kind: 'epic',
        title: 'client bootstrap',
        terminal_reason: 'completed',
        ended_at: '2026-10-02T10:00:00Z',
      },
    ],
  };

  beforeEach(() => {
    localStorage.clear();
    runtimeStore.set({
      isInitialized: true,
      isPaired: true,
      isOnboarded: true,
      deviceLabel: 'Firefox on mbp-oliver',
      machineName: 'local-machine',
      currentRoute: 'overview',
      currentState: null,
      isRevoked: false,
      revokedReason: '',
      isOffline: false,
      isListenerClosed: false,
      completedSteps: 0,
      activeStep: 1,
    });

    vi.spyOn(apiClient, 'getProjects').mockResolvedValue(mockProjects);
    vi.spyOn(apiClient, 'getProjectDetail').mockImplementation(async (id: string) => {
      if (id === 'pm-mcp-core') return mockDetailCore;
      return mockDetailClients;
    });
    vi.spyOn(apiClient, 'getEndedWork').mockResolvedValue(mockEndedPage);
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  });

  it('renders sidebar with enrolled projects, attention counters, and active work counts', async () => {
    render(OverviewView);

    await waitFor(() => {
      expect(screen.getAllByText('pm-mcp-clients').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('pm-mcp-core')).toBeInTheDocument();
      expect(screen.getByText('plan-marshall')).toBeInTheDocument();
    });

    // Check attention markers
    expect(screen.getByText('! 2')).toBeInTheDocument();
    expect(screen.getByText('setup needs you')).toBeInTheDocument();
  });

  it('renders plans by default with progress, attention badges, and off-track warning', async () => {
    render(OverviewView);

    await waitFor(() => {
      expect(screen.getByText('Console history of answers')).toBeInTheDocument();
      expect(screen.getByText('Overview resource of the runtime')).toBeInTheDocument();
      expect(screen.getByText('Message catalogue')).toBeInTheDocument();
    });

    // Attention kind
    expect(screen.getByText('! decision')).toBeInTheDocument();

    // Progress
    expect(screen.getByText('7 of 12 tasks')).toBeInTheDocument();

    // Off-track badge
    const offTrack = screen.getByTestId('off-track-badge');
    expect(offTrack).toHaveTextContent('off track: repeating');

    // PR link
    const prLink = screen.getByRole('link', { name: '#214' });
    expect(prLink).toHaveAttribute(
      'href',
      'https://github.com/plan-marshall/pm-mcp-clients/pull/214',
    );

    // Alongside section
    expect(screen.getByTestId('alongside-section')).toBeInTheDocument();
    expect(screen.getByText('! landing failed')).toBeInTheDocument();
  });

  it('toggles view between by-plan and by-epic', async () => {
    render(OverviewView);

    await waitFor(() => {
      expect(screen.getByText('Overview resource of the runtime')).toBeInTheDocument();
    });

    const byEpicBtn = screen.getByRole('radio', { name: 'by epic' });
    await fireEvent.click(byEpicBtn);

    await waitFor(() => {
      expect(screen.getByTestId('epic-card')).toBeInTheDocument();
      expect(screen.getByText(/2 running/)).toBeInTheDocument();
      expect(screen.getByText('without epic')).toBeInTheDocument();
    });

    // Toggling back
    const byPlanBtn = screen.getByRole('radio', { name: 'by plan' });
    await fireEvent.click(byPlanBtn);

    await waitFor(() => {
      expect(screen.getAllByTestId('plan-card').length).toBe(3);
    });
  });

  it('switches selected project when clicking on sidebar project item', async () => {
    render(OverviewView);

    await waitFor(() => {
      expect(screen.getByText('Console history of answers')).toBeInTheDocument();
    });

    const coreProjectBtn = screen.getByRole('tab', { name: /pm-mcp-core/ });
    await fireEvent.click(coreProjectBtn);

    await waitFor(() => {
      expect(screen.getByText('Session log and generations')).toBeInTheDocument();
      expect(screen.queryByText('Console history of answers')).not.toBeInTheDocument();
    });
  });

  it('opens ended work view with outcomes, reasons, and pagination controls', async () => {
    render(OverviewView);

    await waitFor(() => {
      expect(screen.getByText('Ended work')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByText('Ended work'));

    await waitFor(() => {
      expect(screen.getByText('Relay drops events on reconnect')).toBeInTheDocument();
      expect(screen.getByText("Fix the relay's event loss")).toBeInTheDocument();
      expect(screen.getByText('client bootstrap')).toBeInTheDocument();
    });

    // Outcomes
    expect(screen.getByText('merged')).toBeInTheDocument();
    expect(screen.getByText('cancelled')).toBeInTheDocument();
    expect(screen.getByText('completed')).toBeInTheDocument();

    // Reason
    expect(
      screen.getByText('Reason: the approach is replaced by plan relay-v2'),
    ).toBeInTheDocument();

    // PR link
    expect(screen.getByRole('link', { name: '#211' })).toBeInTheDocument();

    // Pagination buttons
    const olderBtn = screen.getByRole('button', { name: 'Older' });
    expect(olderBtn).not.toBeDisabled();
    const newerBtn = screen.getByRole('button', { name: 'Newer' });
    expect(newerBtn).toBeDisabled();

    // Back button
    await fireEvent.click(screen.getByText('Back'));
    await waitFor(() => {
      expect(screen.getByText('Console history of answers')).toBeInTheDocument();
    });
  });

  it('subscribes to live SSE events and reloads on update', async () => {
    let capturedHandler: ((event: string, data: unknown) => void) | null = null;
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((handler) => {
      capturedHandler = handler;
      return () => {};
    });

    render(OverviewView);

    await waitFor(() => {
      expect(screen.getAllByText('pm-mcp-clients').length).toBeGreaterThanOrEqual(1);
    });

    expect(capturedHandler).toBeDefined();

    // Trigger an SSE event
    capturedHandler!('needs_operator.changed', {});

    await waitFor(() => {
      expect(apiClient.getProjects).toHaveBeenCalledTimes(2);
    });
  });
});
