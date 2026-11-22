import { describe, it, expect, vi, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import ProjectExplorerView from './ProjectExplorerView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { apiClient, type ProjectExplorer } from '../api/client';
import { ClientError } from '../api/errors';

/**
 * The Projects lens (PLAN-59 D1, D4): the list of rows, the one project page beside it, and the acts
 * a project offers — the visible _Add a plan_ on a ready row, and the row menu with the two
 * destructive acts behind their confirmation.
 */

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

/**
 * @param over the store fields to override
 * @returns a paired, onboarded runtime state on the projects route
 */
function state(over: Record<string, unknown> = {}) {
  return makeRuntimeState({
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
    ...over,
  });
}

describe('ProjectExplorerView component', () => {
  beforeEach(() => {
    localStorage.clear();
    window.history.replaceState(null, '', '/projects');
    runtimeStore.set(state());

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
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      display_name: 'pm-mcp-clients',
      plans: [
        {
          plan_id: 'fix-relay',
          title: 'Fix Relay Transport',
          current_phase: 'execute',
          phase_state: 'running',
        },
      ],
      epics: [],
      alongside_landings: [],
    });
    vi.spyOn(apiClient, 'getEndedWork').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      page: 1,
      total_pages: 1,
      has_newer: false,
      has_older: false,
      items: [
        {
          id: 'session-log',
          kind: 'plan',
          title: 'Session log and generations',
          outcome: 'completed',
          ended_at: '2026-10-15T10:00:00Z',
        },
      ],
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
    vi.spyOn(apiClient, 'getProjectOnboarding').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      step: 4,
      total_steps: 10,
      steps: [
        { step: 1, key: 'identity', status: 'done' },
        { step: 4, key: 'environment', status: 'current' },
      ],
    });
    vi.spyOn(apiClient, 'getOverSize').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      distributions: [{ project_id: 'pm-mcp-clients', role: 'implementer', current_multiple: 2, buckets: [] }],
    });
  });

  it('draws the error pattern when the explorer cannot be read', async () => {
    const explorer = vi
      .spyOn(apiClient, 'getProjectExplorer')
      .mockRejectedValue(new ClientError('network_offline'));

    render(ProjectExplorerView);

    const error = await screen.findByTestId('explorer-error');
    expect(error).toHaveTextContent('The machine is not reachable');
    await fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
    await waitFor(() => expect(explorer).toHaveBeenCalledTimes(2));
  });

  it('renders the four sections as rows of the list', async () => {
    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('section-being-added')).toBeInTheDocument();
      expect(screen.getByTestId('section-favourites')).toBeInTheDocument();
      expect(screen.getByTestId('section-recent')).toBeInTheDocument();
      expect(screen.getByTestId('section-all-projects')).toBeInTheDocument();
    });

    expect(screen.getByText('~/git/cui-parent')).toBeInTheDocument();
    expect(screen.getByText('step 2 of 10: Identity')).toBeInTheDocument();

    expect(screen.getByTestId('project-row-pm-mcp-clients')).toBeInTheDocument();
    expect(
      screen.getByTestId('star-btn-pm-mcp-clients').querySelector('[data-icon="act.favourite-on"]'),
    ).not.toBeNull();

    expect(screen.getByTestId('project-row-plan-marshall')).toBeInTheDocument();
    // The state word comes from the shared display vocabulary, never a raw readiness value.
    expect(within(screen.getByTestId('project-row-plan-marshall')).getByText('setup open')).toBeVisible();
    expect(within(screen.getByTestId('project-row-plan-marshall')).getByText('! setup')).toBeVisible();

    expect(screen.getByTestId('project-row-API-Sheriff')).toBeInTheDocument();
    expect(screen.getByTestId('project-row-legacy-portal')).toBeInTheDocument();
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
      expect(screen.getByTestId('project-row-cui-http')).toBeInTheDocument();
      expect(screen.queryByTestId('project-row-API-Sheriff')).not.toBeInTheDocument();
    });
  });

  it('toggles favourite status when the star is clicked', async () => {
    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('star-btn-pm-mcp-clients')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByTestId('star-btn-pm-mcp-clients'));

    expect(apiClient.toggleFavourite).toHaveBeenCalledWith('pm-mcp-clients', false);
  });

  it('reorders the favourites by the keyboard route and persists the order', async () => {
    render(ProjectExplorerView);

    const favouriteRows = await screen.findByTestId('favourite-rows');
    const rows = favouriteRows.querySelectorAll('[data-testid^="project-row-"]');
    expect(rows).toHaveLength(2);

    await fireEvent.keyDown(rows[0] as HTMLElement, { key: 'ArrowDown', altKey: true });

    await waitFor(() =>
      expect(apiClient.reorderFavouriteProjects).toHaveBeenCalledWith([
        'pm-mcp-core',
        'pm-mcp-clients',
      ]),
    );
    expect(screen.queryByTestId('reorder-error')).not.toBeInTheDocument();
  });

  it('reorders the favourites by dragging one onto another', async () => {
    render(ProjectExplorerView);

    const favouriteRows = await screen.findByTestId('favourite-rows');
    const rows = favouriteRows.querySelectorAll('[data-testid^="project-row-"]');
    const dataTransfer = {
      effectAllowed: '',
      setData: vi.fn(),
      getData: vi.fn(() => 'pm-mcp-clients'),
    };

    await fireEvent.dragStart(rows[0] as HTMLElement, { dataTransfer });
    await fireEvent.drop(rows[1] as HTMLElement, { dataTransfer });

    await waitFor(() =>
      expect(apiClient.reorderFavouriteProjects).toHaveBeenCalledWith([
        'pm-mcp-core',
        'pm-mcp-clients',
      ]),
    );
  });

  it('restores the old order and says so when the runtime refuses the new one', async () => {
    vi.spyOn(apiClient, 'reorderFavouriteProjects').mockRejectedValue(
      new ClientError('network_offline'),
    );

    render(ProjectExplorerView);

    const favouriteRows = await screen.findByTestId('favourite-rows');
    const rows = favouriteRows.querySelectorAll('[data-testid^="project-row-"]');
    await fireEvent.keyDown(rows[0] as HTMLElement, { key: 'ArrowDown', altKey: true });

    expect(await screen.findByTestId('reorder-error')).toBeInTheDocument();
    const after = screen.getByTestId('favourite-rows').querySelectorAll('[data-testid^="project-row-"]');
    expect(after[0]).toHaveTextContent('pm-mcp-clients');
  });

  it('carries Add a plan on a ready row and disables it with its reason on a not-ready row', async () => {
    render(ProjectExplorerView);

    const ready = await screen.findByTestId('add-plan-pm-mcp-clients');
    expect(ready).toBeEnabled();
    expect(ready).toHaveTextContent('Add a plan');

    const notReady = screen.getByTestId('add-plan-plan-marshall');
    expect(notReady).toHaveAttribute('aria-disabled', 'true');
    expect(notReady).toHaveAttribute('title', 'plan-marshall is not ready: setup');
    expect(notReady).toHaveAccessibleDescription('plan-marshall is not ready: setup');
  });

  it('starts a plan from a ready row without opening the project first', async () => {
    render(ProjectExplorerView);

    await fireEvent.click(await screen.findByTestId('add-plan-pm-mcp-clients'));

    await waitFor(() => {
      expect(get(runtimeStore).currentRoute).toBe('start');
      expect(get(runtimeStore).startProjectId).toBe('pm-mcp-clients');
      expect(get(runtimeStore).startKind).toBe('plan');
    });
  });

  it('keeps release and unenrol out of the row and in its menu', async () => {
    render(ProjectExplorerView);

    const row = await screen.findByTestId('project-row-pm-mcp-clients');
    expect(within(row).queryByTestId('release-btn-pm-mcp-clients')).toBeNull();
    expect(within(row).queryByText('Release…')).toBeNull();

    await fireEvent.click(
      within(row).getByRole('button', { name: 'Actions for pm-mcp-clients' }),
    );

    expect(await within(row).findByTestId('action-add_epic')).toBeInTheDocument();
    expect(within(row).getByTestId('action-release')).toBeInTheDocument();
    expect(within(row).getByTestId('action-unenrol')).toBeInTheDocument();
    expect(within(row).queryByTestId('action-add_plan')).toBeNull();
  });

  it('releases a project through the confirmation, with its reason', async () => {
    render(ProjectExplorerView);

    const row = await screen.findByTestId('project-row-pm-mcp-clients');
    await fireEvent.click(
      within(row).getByRole('button', { name: 'Actions for pm-mcp-clients' }),
    );
    await fireEvent.click(await screen.findByTestId('action-release'));

    const modal = await screen.findByTestId('release-modal');
    expect(modal).toBeInTheDocument();
    await fireEvent.input(screen.getByTestId('release-reason-input'), {
      target: { value: 'Project completed and archived' },
    });
    await fireEvent.click(within(modal).getByTestId('confirm-dialog-confirm'));

    await waitFor(() =>
      expect(apiClient.releaseProject).toHaveBeenCalledWith(
        'pm-mcp-clients',
        'Project completed and archived',
      ),
    );
  });

  it('unenrols a project through its confirmation', async () => {
    render(ProjectExplorerView);

    const row = await screen.findByTestId('project-row-pm-mcp-clients');
    await fireEvent.click(
      within(row).getByRole('button', { name: 'Actions for pm-mcp-clients' }),
    );
    await fireEvent.click(await screen.findByTestId('action-unenrol'));

    const modal = await screen.findByTestId('unenrol-modal');
    await fireEvent.click(within(modal).getByTestId('confirm-dialog-confirm'));

    await waitFor(() => expect(apiClient.unenrolProject).toHaveBeenCalledWith('pm-mcp-clients'));
  });

  it('abandons a being-added run after its confirmation', async () => {
    render(ProjectExplorerView);

    await fireEvent.click(await screen.findByTestId('abandon-onboarding-btn'));
    const modal = await screen.findByTestId('abandon-modal');
    await fireEvent.click(within(modal).getByTestId('confirm-dialog-confirm'));

    await waitFor(() =>
      expect(apiClient.abandonOnboarding).toHaveBeenCalledWith(
        '/api/v1/entries/ent-cui/abandon',
      ),
    );
  });

  it('renders the empty state when no project is enrolled', async () => {
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
      favourites: [],
      recent: [],
      all_projects: [],
      being_added: [],
    });

    render(ProjectExplorerView);

    expect(await screen.findByTestId('empty-projects-state')).toBeInTheDocument();
  });

  it('keeps the list when a background refresh fails', async () => {
    render(ProjectExplorerView);
    await screen.findByTestId('project-row-pm-mcp-clients');

    vi.spyOn(apiClient, 'getProjectExplorer').mockRejectedValue(
      new ClientError('network_offline'),
    );
    await fireEvent.click(screen.getByTestId('star-btn-pm-mcp-clients'));

    await waitFor(() => expect(screen.getByTestId('explorer-error')).toBeInTheDocument());
  });
});

describe('the one project page', () => {
  beforeEach(() => {
    localStorage.clear();
    window.history.replaceState(null, '', '/projects/pm-mcp-clients');
    runtimeStore.set(
      state({
        currentRoute: 'project',
        selectedProjectId: 'pm-mcp-clients',
        objectTab: null,
      }),
    );

    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue(mockExplorer);
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      display_name: 'pm-mcp-clients',
      plans: [
        {
          plan_id: 'fix-relay',
          title: 'Fix Relay Transport',
          current_phase: 'execute',
          phase_state: 'running',
        },
      ],
      epics: [],
      alongside_landings: [],
    });
    vi.spyOn(apiClient, 'getEndedWork').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      page: 1,
      total_pages: 1,
      has_newer: false,
      has_older: false,
      items: [
        {
          id: 'session-log',
          kind: 'plan',
          title: 'Session log and generations',
          outcome: 'completed',
          ended_at: '2026-10-15T10:00:00Z',
        },
      ],
    });
    vi.spyOn(apiClient, 'getProjectSettings').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      revision: 7,
      values: [
        {
          aspect: 'landing',
          value: 'landing.main',
          origin: 'team',
          choices: ['landing.main', 'landing.files'],
        },
      ],
    });
    vi.spyOn(apiClient, 'saveProjectSettings').mockResolvedValue({
      applied: true,
      conflict: false,
      revision: 8,
    });
    vi.spyOn(apiClient, 'getProjectOnboarding').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      steps: [{ step: 4, summary: 'Build environment', status: 'current' }],
    });
    vi.spyOn(apiClient, 'getOverSize').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      distributions: [
        { project_id: 'pm-mcp-clients', role: 'implementer', current_multiple: 2, buckets: [] },
      ],
    });
  });

  it('shows the head, the acts and the running work of the project', async () => {
    render(ProjectExplorerView);

    expect(await screen.findByTestId('project-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'pm-mcp-clients' })).toBeInTheDocument();
    expect(await screen.findByTestId('project-page-work')).toHaveTextContent('Fix Relay Transport');
    expect(screen.getByTestId('action-add_plan')).toBeInTheDocument();
    expect(screen.getByTestId('action-add_epic')).toBeInTheDocument();
    // The two destructive acts stay in the bar's overflow.
    expect(screen.queryByTestId('action-release')).toBeNull();
    await fireEvent.click(screen.getByTestId('action-bar-overflow'));
    expect(await screen.findByTestId('action-release')).toBeInTheDocument();
    expect(screen.getByTestId('action-unenrol')).toBeInTheDocument();
  });

  it('opens the ended work on the work tab', async () => {
    render(ProjectExplorerView);

    await fireEvent.click(await screen.findByTestId('project-ended-toggle'));

    expect(await screen.findByTestId('project-ended-rows')).toHaveTextContent(
      'Session log and generations',
    );
  });

  it('shows the setup values, the setup steps and the over-size spread on the setup tab', async () => {
    runtimeStore.set(
      state({
        currentRoute: 'project',
        selectedProjectId: 'pm-mcp-clients',
        objectTab: 'setup',
      }),
    );

    render(ProjectExplorerView);

    expect(await screen.findByTestId('project-page-setup')).toBeInTheDocument();
    expect(await screen.findByTestId('setting-input-landing')).toBeInTheDocument();
    expect(await screen.findByTestId('project-setup-steps')).toHaveTextContent('Build environment');
    expect(await screen.findByTestId('project-oversize')).toHaveTextContent('implementer');
    expect(screen.getByTestId('setup-release-btn')).toBeInTheDocument();
    expect(screen.getByTestId('setup-unenrol-btn')).toBeInTheDocument();
  });

  it('saves one value against the revision the tab was opened at', async () => {
    runtimeStore.set(
      state({
        currentRoute: 'project',
        selectedProjectId: 'pm-mcp-clients',
        objectTab: 'setup',
      }),
    );

    render(ProjectExplorerView);

    const input = await screen.findByTestId('setting-input-landing');
    await fireEvent.change(input, { target: { value: 'landing.files' } });
    await fireEvent.click(screen.getByTestId('save-setting-landing'));

    await waitFor(() =>
      expect(apiClient.saveProjectSettings).toHaveBeenCalledWith(
        'pm-mcp-clients',
        'landing',
        'landing.files',
        7,
      ),
    );
    expect(await screen.findByTestId('setup-notice')).toBeInTheDocument();
  });

  it('opens the conflict card instead of overwriting a value another client changed', async () => {
    vi.spyOn(apiClient, 'saveProjectSettings').mockResolvedValue({
      applied: false,
      conflict: true,
      revision: 9,
      current_value: 'landing.pull-request',
    });
    runtimeStore.set(
      state({
        currentRoute: 'project',
        selectedProjectId: 'pm-mcp-clients',
        objectTab: 'setup',
      }),
    );

    render(ProjectExplorerView);

    const input = await screen.findByTestId('setting-input-landing');
    await fireEvent.change(input, { target: { value: 'landing.files' } });
    await fireEvent.click(screen.getByTestId('save-setting-landing'));

    const conflict = await screen.findByTestId('project-conflict');
    expect(conflict).toHaveTextContent('landing.pull-request');
    await fireEvent.click(within(conflict).getByTestId('project-conflict-apply'));

    await waitFor(() => expect(apiClient.saveProjectSettings).toHaveBeenCalledTimes(2));
  });
});
