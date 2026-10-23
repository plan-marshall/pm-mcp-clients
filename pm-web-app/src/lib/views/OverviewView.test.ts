import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import OverviewView from './OverviewView.svelte';
import { workflowIconUrl } from '../icons/workflowIcons';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { get } from 'svelte/store';
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
    runtimeStore.set(
      makeRuntimeState({
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
      }),
    );

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

  it('puts a phase icon in every phase badge', async () => {
    const { container } = render(OverviewView);

    await waitFor(() => expect(screen.getAllByTestId('plan-card').length).toBeGreaterThan(0));

    const badgeIcons = ['init', 'refine', 'outline', 'plan', 'execute', 'finalize'].map((phase) =>
      workflowIconUrl(`phase.${phase}`),
    );
    const badges = container.querySelectorAll('.badge--phase');
    expect(badges.length).toBeGreaterThan(0);
    for (const badge of badges) {
      const style = badge.querySelector('.workflow-icon')?.getAttribute('style') ?? '';
      expect(
        badgeIcons.some((url) => style.includes(url)),
        badge.textContent ?? '',
      ).toBe(true);
    }
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

  it('remembers Running Work as the browser start page and forgets it again', async () => {
    render(OverviewView);

    const toggle = await screen.findByTestId('start-page-toggle');
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    expect(toggle).toHaveTextContent('Set as browser start page');

    await fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-pressed', 'true');
    expect(toggle).toHaveTextContent('Default start page');
    expect(localStorage.getItem('pm_start_route')).toBe('overview');

    await fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    expect(localStorage.getItem('pm_start_route')).toBeNull();
  });

  it('opens an ended plan as it ended and its completion report in one step', async () => {
    render(OverviewView);

    await waitFor(() => {
      expect(screen.getByText('Ended work')).toBeInTheDocument();
    });
    await fireEvent.click(screen.getByText('Ended work'));
    await waitFor(() => {
      expect(screen.getAllByTestId('ended-item').length).toBe(3);
    });

    await fireEvent.click(screen.getByTestId('ended-open-pln-relay-drops'));

    let state = get(runtimeStore);
    expect(state.currentRoute).toBe('plan');
    expect(state.selectedPlanId).toBe('pln-relay-drops');
    expect(state.selectedPlanEnded).toEqual({
      plan_id: 'pln-relay-drops',
      outcome: 'merged',
      reason: null,
      ended_at: '2026-10-14T10:00:00Z',
    });
    expect(localStorage.getItem('pm_overview_show_ended')).toBe('1');

    await fireEvent.click(screen.getByTestId('ended-report-pln-relay-drops'));

    state = get(runtimeStore);
    expect(state.currentRoute).toBe('reports');
    expect(state.selectedReportDetailPlanId).toBe('pln-relay-drops');
  });

  it('restores the ended work page after the operator returns to the overview', async () => {
    localStorage.setItem('pm_overview_show_ended', '1');
    localStorage.setItem('pm_overview_ended_page', '2');

    render(OverviewView);

    await waitFor(() => {
      expect(apiClient.getEndedWork).toHaveBeenCalledWith('pm-mcp-clients', 2);
      expect(screen.getAllByTestId('ended-item').length).toBe(3);
    });
  });

  it('opens the project another view named in one step', async () => {
    runtimeStore.update((s) => ({ ...s, selectedProjectId: 'pm-mcp-core' }));

    render(OverviewView);

    await waitFor(() => {
      expect(screen.getByText('Session log and generations')).toBeInTheDocument();
    });
    expect(screen.getByRole('tab', { name: /pm-mcp-core/ })).toHaveAttribute(
      'aria-selected',
      'true',
    );
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

  /** @param value the filter text */
  async function typeFilter(value: string): Promise<void> {
    const input = await screen.findByTestId('filter-input');
    await fireEvent.input(input, { target: { value } });
  }

  it('filters the running plans while keeping the project tiles unfiltered', async () => {
    render(OverviewView);
    await waitFor(() => expect(screen.getAllByTestId('plan-card').length).toBe(3));

    await typeFilter('Message catalogue');

    expect(screen.getAllByTestId('plan-card')).toHaveLength(1);
    expect(screen.getByText('Message catalogue')).toBeInTheDocument();
    expect(screen.queryByText('Console history of answers')).toBeNull();
    // The sidebar's project tiles are navigation, not list items.
    expect(screen.getByRole('tab', { name: /pm-mcp-core/ })).toBeInTheDocument();
  });

  it('filters the running plans by their epic and state', async () => {
    render(OverviewView);
    await waitFor(() => expect(screen.getAllByTestId('plan-card').length).toBe(3));

    await typeFilter('client work');
    expect(screen.getAllByTestId('plan-card')).toHaveLength(2);
    expect(screen.queryByText('Console history of answers')).toBeNull();

    await typeFilter('pausing');
    expect(screen.getAllByTestId('plan-card')).toHaveLength(1);
    expect(screen.getByText('Message catalogue')).toBeInTheDocument();
  });

  it('shows a no-match state for the running plans', async () => {
    render(OverviewView);
    await waitFor(() => expect(screen.getAllByTestId('plan-card').length).toBe(3));

    await typeFilter('nothing-matches-this');

    expect(await screen.findByTestId('running-filter-empty')).toHaveTextContent(
      'Nothing matches the filter.',
    );
    expect(screen.queryAllByTestId('plan-card')).toHaveLength(0);
  });

  it('filters the running epics by their identifier', async () => {
    render(OverviewView);
    await waitFor(() => expect(screen.getAllByTestId('plan-card').length).toBe(3));
    await fireEvent.click(screen.getByRole('radio', { name: 'by epic' }));
    await waitFor(() => expect(screen.getByTestId('epic-card')).toBeInTheDocument());

    await typeFilter('epc-client-work');
    expect(screen.getAllByTestId('epic-card')).toHaveLength(1);

    await typeFilter('nothing-matches-this');
    expect(await screen.findByTestId('running-filter-empty')).toBeInTheDocument();
  });

  it('filters the ended-work page by title, outcome, and reason', async () => {
    render(OverviewView);
    await waitFor(() => expect(screen.getByText('Ended work')).toBeInTheDocument());
    await fireEvent.click(screen.getByText('Ended work'));
    await waitFor(() => expect(screen.getAllByTestId('ended-item').length).toBe(3));

    await typeFilter('Relay drops');
    expect(screen.getAllByTestId('ended-item')).toHaveLength(1);
    expect(screen.getByText('Relay drops events on reconnect')).toBeInTheDocument();

    await typeFilter('cancelled');
    expect(screen.getAllByTestId('ended-item')).toHaveLength(1);
    expect(screen.getByText("Fix the relay's event loss")).toBeInTheDocument();

    await typeFilter('replaced by plan relay-v2');
    expect(screen.getAllByTestId('ended-item')).toHaveLength(1);

    await typeFilter('nothing-matches-this');
    expect(await screen.findByTestId('ended-filter-empty')).toHaveTextContent(
      'Nothing matches the filter.',
    );
    expect(screen.queryAllByTestId('ended-item')).toHaveLength(0);
  });
});
