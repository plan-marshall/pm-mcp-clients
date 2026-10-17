import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import ProjectExplorerView from './ProjectExplorerView.svelte';
import { runtimeStore } from '../stores/runtime';
import { apiClient, type ProjectExplorer } from '../api/client';

describe('ProjectExplorerView component', () => {
  const mockExplorer: ProjectExplorer = {
    favourites: [
      {
        project_id: 'pm-mcp-clients',
        display_name: 'pm-mcp-clients',
        remote_url: 'https://github.com/plan-marshall/pm-mcp-clients',
        readiness: 'ready',
        needs_operator_count: 2,
        active_plans_count: 3,
        active_epics_count: 1,
        last_active: '2026-10-17T10:04:00Z',
        is_favourite: true,
        is_released: false,
      },
    ],
    recent: [
      {
        project_id: 'plan-marshall',
        display_name: 'plan-marshall',
        remote_url: 'https://github.com/plan-marshall/plan-marshall',
        readiness: 'setup_needs',
        setup_needs_summary: 'credential sonar',
        needs_operator_count: 1,
        active_plans_count: 0,
        active_epics_count: 0,
        last_active: '2026-10-16T10:00:00Z',
        is_favourite: false,
        is_released: false,
      },
    ],
    all_projects: [
      {
        project_id: 'API-Sheriff',
        display_name: 'API-Sheriff',
        remote_url: 'https://github.com/plan-marshall/api-sheriff',
        readiness: 'ready',
        needs_operator_count: 0,
        active_plans_count: 0,
        active_epics_count: 0,
        last_active: '2026-09-12T10:00:00Z',
        is_favourite: false,
        is_released: false,
      },
      {
        project_id: 'cui-http',
        display_name: 'cui-http',
        remote_url: 'https://github.com/cuioss/cui-http',
        readiness: 'ready',
        needs_operator_count: 0,
        active_plans_count: 0,
        active_epics_count: 0,
        last_active: '2026-10-14T10:00:00Z',
        is_favourite: false,
        is_released: false,
      },
      {
        project_id: 'legacy-portal',
        display_name: 'legacy-portal',
        remote_url: null,
        readiness: 'released',
        needs_operator_count: 0,
        active_plans_count: 0,
        active_epics_count: 0,
        last_active: '2026-08-17T10:00:00Z',
        is_favourite: false,
        is_released: true,
      },
    ],
    being_added: [
      {
        checkout_path: '~/git/cui-parent',
        current_step: 'Identity',
        step_number: 2,
        total_steps: 10,
        continue_link: '/api/v1/entries/ent-cui/continue',
        abandon_link: '/api/v1/entries/ent-cui/abandon',
      },
    ],
  };

  beforeEach(() => {
    localStorage.clear();
    runtimeStore.set({
      isInitialized: true,
      isPaired: true,
      isOnboarded: true,
      deviceLabel: 'Firefox on mbp',
      machineName: 'local-machine',
      currentRoute: 'projects',
      currentState: null,
      isRevoked: false,
      revokedReason: '',
      isOffline: false,
      isDraining: false,
      isStopped: false,
      activeStep: 1,
      unreadReportsCount: 0,
      openDecisionsCount: 0,
    });

    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue(mockExplorer);
    vi.spyOn(apiClient, 'toggleFavourite').mockResolvedValue();
    vi.spyOn(apiClient, 'startProjectOnboarding').mockResolvedValue({ ok: true });
    vi.spyOn(apiClient, 'releaseProject').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      plans_ended_count: 3,
      epics_ended_count: 1,
      entries_deleted_count: 0,
      released_at: '2026-10-17T10:00:00Z',
    });
    vi.spyOn(apiClient, 'unenrolProject').mockResolvedValue();
    vi.spyOn(apiClient, 'abandonOnboarding').mockResolvedValue();
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  });

  it('renders 4-part explorer view with being added, favourites, recent, and all projects', async () => {
    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('section-being-added')).toBeInTheDocument();
      expect(screen.getByTestId('section-favourites')).toBeInTheDocument();
      expect(screen.getByTestId('section-recent')).toBeInTheDocument();
      expect(screen.getByTestId('section-all-projects')).toBeInTheDocument();
    });

    expect(screen.getByText('~/git/cui-parent')).toBeInTheDocument();
    expect(screen.getByText('step 2 of 10: Identity')).toBeInTheDocument();

    expect(screen.getByTestId('project-card-pm-mcp-clients')).toBeInTheDocument();
    expect(screen.getByTestId('star-btn-pm-mcp-clients')).toHaveTextContent('★');

    expect(screen.getByTestId('project-card-plan-marshall')).toBeInTheDocument();
    expect(screen.getByText('setup needs: credential sonar')).toBeInTheDocument();

    expect(screen.getByTestId('project-card-API-Sheriff')).toBeInTheDocument();
    expect(screen.getByTestId('project-card-legacy-portal')).toBeInTheDocument();
  });

  it('filters projects by text query', async () => {
    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('project-filter-input')).toBeInTheDocument();
    });

    const input = screen.getByTestId('project-filter-input');
    await fireEvent.input(input, { target: { value: 'http' } });

    await waitFor(() => {
      expect(screen.getByTestId('section-filtered-matches')).toBeInTheDocument();
      expect(screen.getByTestId('project-card-cui-http')).toBeInTheDocument();
      expect(screen.queryByTestId('project-card-API-Sheriff')).not.toBeInTheDocument();
    });
  });

  it('toggles favourite status when star button is clicked', async () => {
    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('star-btn-pm-mcp-clients')).toBeInTheDocument();
    });

    const starBtn = screen.getByTestId('star-btn-pm-mcp-clients');
    await fireEvent.click(starBtn);

    expect(apiClient.toggleFavourite).toHaveBeenCalledWith('pm-mcp-clients', false);
  });

  it('renders empty state when no projects exist', async () => {
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
      favourites: [],
      recent: [],
      all_projects: [],
      being_added: [],
    });

    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('empty-projects-state')).toBeInTheDocument();
    });
    expect(screen.getByText('No project is enrolled yet.')).toBeInTheDocument();
  });

  it('opens release modal and executes project release', async () => {
    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('release-btn-pm-mcp-clients')).toBeInTheDocument();
    });

    const releaseBtn = screen.getByTestId('release-btn-pm-mcp-clients');
    await fireEvent.click(releaseBtn);

    expect(screen.getByTestId('release-modal')).toBeInTheDocument();

    const reasonInput = screen.getByTestId('release-reason-input');
    await fireEvent.input(reasonInput, { target: { value: 'Finished work' } });

    const confirmBtn = screen.getByTestId('confirm-release-btn');
    await fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(apiClient.releaseProject).toHaveBeenCalledWith('pm-mcp-clients', 'Finished work');
    });
  });

  it('handles unenrolment safety check refusal on active work', async () => {
    vi.spyOn(apiClient, 'unenrolProject').mockRejectedValue(new Error('409 scope_active'));

    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('unenrol-btn-pm-mcp-clients')).toBeInTheDocument();
    });

    const unenrolBtn = screen.getByTestId('unenrol-btn-pm-mcp-clients');
    await fireEvent.click(unenrolBtn);

    expect(screen.getByTestId('unenrol-modal')).toBeInTheDocument();

    const confirmBtn = screen.getByTestId('confirm-unenrol-btn');
    await fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(screen.getByTestId('unenrol-error-banner')).toBeInTheDocument();
    });
  });

  it('abandons being added project onboarding after confirmation', async () => {
    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('abandon-onboarding-btn')).toBeInTheDocument();
    });

    const abandonBtn = screen.getByTestId('abandon-onboarding-btn');
    await fireEvent.click(abandonBtn);

    expect(screen.getByTestId('abandon-modal')).toBeInTheDocument();

    const confirmBtn = screen.getByTestId('confirm-abandon-btn');
    await fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(apiClient.abandonOnboarding).toHaveBeenCalledWith('/api/v1/entries/ent-cui/abandon');
    });
  });
});
