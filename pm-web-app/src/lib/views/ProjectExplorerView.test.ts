import { describe, it, expect, vi, beforeEach } from 'vitest';
import { get } from 'svelte/store';
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
      {
        project_id: 'pm-mcp-core',
        display_name: 'pm-mcp-core',
        remote_url: 'https://github.com/plan-marshall/pm-mcp-core',
        readiness: 'ready',
        needs_operator_count: 0,
        active_plans_count: 2,
        active_epics_count: 0,
        last_active: '2026-10-17T09:57:00Z',
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
      isListenerClosed: false,
      completedSteps: 0,
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
    vi.spyOn(apiClient, 'reorderFavouriteProjects').mockResolvedValue({
      status: 'ok',
      project_ids: [],
    });
    vi.spyOn(apiClient, 'getProjectSettings').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      revision: 7,
      values: [
        {
          aspect: 'landing',
          value: 'landing.main',
          origin: 'team',
          choices: ['landing.main', 'landing.files', 'landing.pull-request'],
        },
      ],
    });
    vi.spyOn(apiClient, 'saveProjectSettings').mockResolvedValue({
      applied: true,
      conflict: false,
      revision: 8,
    });
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

  it('moves a favourite down and persists the new order', async () => {
    const reorderSpy = vi.spyOn(apiClient, 'reorderFavouriteProjects').mockResolvedValue({
      status: 'ok',
      project_ids: ['pm-mcp-core', 'pm-mcp-clients'],
    });

    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('favourite-down-pm-mcp-clients')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('favourite-down-pm-mcp-clients'));

    await waitFor(() => {
      expect(reorderSpy).toHaveBeenCalledWith(['pm-mcp-core', 'pm-mcp-clients']);
    });
  });

  it('moves a favourite up with its control and by dropping it on the one above', async () => {
    const reorderSpy = vi.spyOn(apiClient, 'reorderFavouriteProjects').mockResolvedValue({
      status: 'ok',
      project_ids: ['pm-mcp-core', 'pm-mcp-clients'],
    });

    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('favourite-up-pm-mcp-core')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('favourite-up-pm-mcp-core'));

    await waitFor(() => {
      expect(reorderSpy).toHaveBeenCalledWith(['pm-mcp-core', 'pm-mcp-clients']);
    });
  });

  it('reorders favourites by drag and drop', async () => {
    const reorderSpy = vi.spyOn(apiClient, 'reorderFavouriteProjects').mockResolvedValue({
      status: 'ok',
      project_ids: ['pm-mcp-core', 'pm-mcp-clients'],
    });

    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('project-card-pm-mcp-core')).toBeInTheDocument();
    });

    await fireEvent.dragStart(screen.getByTestId('project-card-pm-mcp-core'));
    await fireEvent.drop(screen.getByTestId('project-card-pm-mcp-clients'));

    await waitFor(() => {
      expect(reorderSpy).toHaveBeenCalledWith(['pm-mcp-core', 'pm-mcp-clients']);
    });
  });

  it('restores the old order when the runtime refuses the new one', async () => {
    vi.spyOn(apiClient, 'reorderFavouriteProjects').mockRejectedValue(new Error('HTTP 409'));

    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('favourite-down-pm-mcp-clients')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('favourite-down-pm-mcp-clients'));

    await waitFor(() => {
      expect(
        screen.getByText('The new order of the favourites could not be saved'),
      ).toBeInTheDocument();
    });
    // The refused order is rolled back: the first card is the one that was moved down.
    const cards = screen
      .getByTestId('section-favourites')
      .querySelectorAll('[data-testid^="project-card-"]');
    expect(cards[0]).toHaveAttribute('data-testid', 'project-card-pm-mcp-clients');
  });

  it('opens the project setup drawer and saves against the revision it was read at', async () => {
    const saveSpy = vi.spyOn(apiClient, 'saveProjectSettings').mockResolvedValue({
      applied: true,
      conflict: false,
      revision: 8,
    });

    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('setup-btn-pm-mcp-clients')).toBeInTheDocument();
    });
    await fireEvent.click(screen.getByTestId('setup-btn-pm-mcp-clients'));

    await waitFor(() => {
      expect(screen.getByTestId('setup-drawer')).toBeInTheDocument();
      expect(screen.getByTestId('setting-input-landing')).toBeInTheDocument();
    });

    await fireEvent.change(screen.getByTestId('setting-input-landing'), {
      target: { value: 'landing.files' },
    });
    await fireEvent.click(screen.getByTestId('save-setting-landing'));

    await waitFor(() => {
      expect(saveSpy).toHaveBeenCalledWith('pm-mcp-clients', 'landing', 'landing.files', 7);
    });
  });

  it('shows the other client’s value on a conflict and applies the change again', async () => {
    const saveSpy = vi
      .spyOn(apiClient, 'saveProjectSettings')
      .mockResolvedValueOnce({
        applied: false,
        conflict: true,
        revision: 9,
        current_value: 'landing.files',
      })
      .mockResolvedValueOnce({ applied: true, conflict: false, revision: 10 });

    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('setup-btn-pm-mcp-clients')).toBeInTheDocument();
    });
    await fireEvent.click(screen.getByTestId('setup-btn-pm-mcp-clients'));

    await waitFor(() => {
      expect(screen.getByTestId('setting-input-landing')).toBeInTheDocument();
    });
    await fireEvent.change(screen.getByTestId('setting-input-landing'), {
      target: { value: 'landing.pull-request' },
    });
    await fireEvent.click(screen.getByTestId('save-setting-landing'));

    await waitFor(() => {
      expect(screen.getByTestId('project-conflict')).toBeInTheDocument();
    });
    expect(screen.getByTestId('project-conflict')).toHaveTextContent('landing.files');
    expect(screen.getByTestId('project-conflict')).toHaveTextContent('landing.pull-request');

    await fireEvent.click(screen.getByTestId('project-conflict-apply'));

    await waitFor(() => {
      expect(saveSpy).toHaveBeenLastCalledWith(
        'pm-mcp-clients',
        'landing',
        'landing.pull-request',
        9,
      );
    });
  });

  it('keeps the current value on a conflict without writing again', async () => {
    const saveSpy = vi.spyOn(apiClient, 'saveProjectSettings').mockResolvedValue({
      applied: false,
      conflict: true,
      revision: 9,
      current_value: 'landing.files',
    });

    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('setup-btn-pm-mcp-clients')).toBeInTheDocument();
    });
    await fireEvent.click(screen.getByTestId('setup-btn-pm-mcp-clients'));

    await waitFor(() => {
      expect(screen.getByTestId('save-setting-landing')).toBeInTheDocument();
    });
    await fireEvent.click(screen.getByTestId('save-setting-landing'));
    await waitFor(() => {
      expect(screen.getByTestId('project-conflict')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('project-conflict-keep'));

    await waitFor(() => {
      expect(screen.queryByTestId('project-conflict')).not.toBeInTheDocument();
    });
    expect(saveSpy).toHaveBeenCalledTimes(1);
  });

  it('opens the project onboarding route from adding and from continuing a run', async () => {
    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('add-project-btn')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('add-project-btn'));
    expect(get(runtimeStore).currentRoute).toBe('project-onboarding');

    await fireEvent.click(screen.getByTestId('continue-onboarding-btn'));
    expect(get(runtimeStore).currentRoute).toBe('project-onboarding');
    expect(get(runtimeStore).selectedOnboardingId).toBe('~/git/cui-parent');
  });

  it('continues an enrolled project’s setup by its project id', async () => {
    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('continue-setup-btn-plan-marshall')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('continue-setup-btn-plan-marshall'));

    const state = get(runtimeStore);
    expect(state.currentRoute).toBe('project-onboarding');
    expect(state.selectedProjectId).toBe('plan-marshall');
    expect(state.selectedOnboardingId).toBeNull();
  });
});
