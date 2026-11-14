import { describe, it, expect, vi, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/svelte';
import ProjectExplorerView from './ProjectExplorerView.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { apiClient, type ProjectExplorer } from '../api/client';
import { ClientError } from '../api/errors';

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
    window.history.replaceState(null, '', '/projects');
    runtimeStore.set(
      makeRuntimeState({
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
      }),
    );

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
    expect(
      screen.getByTestId('star-btn-pm-mcp-clients').querySelector('[data-icon="act.favourite-on"]'),
    ).not.toBeNull();

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

  it("shows the sentence of the act failure's code instead of the code", async () => {
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue(mockExplorer);
    vi.spyOn(apiClient, 'releaseProject').mockRejectedValue(new ClientError('timeout'));

    render(ProjectExplorerView);

    await waitFor(() =>
      expect(screen.getByTestId('release-btn-pm-mcp-clients')).toBeInTheDocument(),
    );
    await fireEvent.click(screen.getByTestId('release-btn-pm-mcp-clients'));
    await fireEvent.click(screen.getByTestId('confirm-release-btn'));

    const banner = await screen.findByTestId('release-error-banner');
    expect(banner).toHaveTextContent(
      'Try again; if it keeps taking this long, check that the runtime is running.',
    );
    expect(banner).not.toHaveTextContent(/^timeout$/);
  });

  it('clears the favourite banner after a toggle succeeds', async () => {
    vi.spyOn(apiClient, 'toggleFavourite').mockRejectedValueOnce(new ClientError('timeout'));

    render(ProjectExplorerView);

    await waitFor(() => expect(screen.getByTestId('star-btn-pm-mcp-clients')).toBeInTheDocument());
    await fireEvent.click(screen.getByTestId('star-btn-pm-mcp-clients'));

    expect(await screen.findByTestId('explorer-action-error')).toHaveTextContent(
      'Failed to update favourite',
    );

    await fireEvent.click(screen.getByTestId('star-btn-pm-mcp-clients'));

    await waitFor(() => expect(screen.queryByTestId('explorer-action-error')).toBeNull());
  });

  it('handles unenrolment safety check refusal on active work', async () => {
    // A daemon refusal carries its wire code as the message, as `httpError` keeps it.
    vi.spyOn(apiClient, 'unenrolProject').mockRejectedValue(new Error('scope_active'));

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
    expect(screen.getByTestId('unenrol-error-banner')).toHaveTextContent(
      'pm-mcp-clients cannot be unenrolled while work runs. End them first, or release the project, which ends them.',
    );
  });

  it.each([
    [new Error('HTTP 409 scope_active'), 'Failed to unenrol project'],
    [new ClientError('timeout'), 'Try again; if it keeps taking this long'],
  ])('reads an unenrol refusal by its code only (%s)', async (failure, text) => {
    vi.spyOn(apiClient, 'unenrolProject').mockRejectedValue(failure);
    render(ProjectExplorerView);
    await fireEvent.click(await screen.findByTestId('unenrol-btn-pm-mcp-clients'));

    await fireEvent.click(screen.getByTestId('confirm-unenrol-btn'));

    const banner = await screen.findByTestId('unenrol-error-banner');
    expect(banner).toHaveTextContent(text);
    expect(banner).not.toHaveTextContent('cannot be unenrolled while work runs');
  });

  it('unenrols a project and reads the explorer again', async () => {
    render(ProjectExplorerView);
    await fireEvent.click(await screen.findByTestId('unenrol-btn-pm-mcp-clients'));

    await fireEvent.click(screen.getByTestId('confirm-unenrol-btn'));

    await waitFor(() => expect(screen.queryByTestId('unenrol-modal')).toBeNull());
    expect(apiClient.unenrolProject).toHaveBeenCalledWith('pm-mcp-clients');
    expect(apiClient.getProjectExplorer).toHaveBeenCalledTimes(2);
  });

  it('marks a recent project that is a favourite as one, and its star removes it', async () => {
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
      ...mockExplorer,
      recent: [{ ...mockExplorer.recent[0], is_favourite: true }],
    });
    render(ProjectExplorerView);

    const star = await screen.findByTestId('star-btn-plan-marshall');

    expect(star).toHaveAttribute('aria-label', 'Remove from favourites');
    expect(star.querySelector('[data-icon="act.favourite-on"]')).not.toBeNull();
    await fireEvent.click(star);
    expect(apiClient.toggleFavourite).toHaveBeenCalledWith('plan-marshall', false);
  });

  it('reads the explorer once when it opens after a project event', async () => {
    runtimeStore.update((state) => ({ ...state, projectEventNotice: 'pm-mcp-clients changed' }));

    render(ProjectExplorerView);
    await screen.findByTestId('section-favourites');

    expect(apiClient.getProjectExplorer).toHaveBeenCalledTimes(1);
  });

  it.each(['project.changed', 'project.released', 'project.enrolled'])(
    'reads the explorer again once on %s',
    async (event) => {
      let emit: (name: string) => void = () => {};
      vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((handler) => {
        emit = (name) => handler(name, {});
        return () => {};
      });
      render(ProjectExplorerView);
      await screen.findByTestId('section-favourites');

      emit('report.sent');
      emit(event);

      await waitFor(() => expect(apiClient.getProjectExplorer).toHaveBeenCalledTimes(2));
    },
  );

  it.each([
    ['release-btn-pm-mcp-clients', 'release-modal', 'cancel-release-btn'],
    ['unenrol-btn-pm-mcp-clients', 'unenrol-modal', 'cancel-unenrol-btn'],
    ['abandon-onboarding-btn', 'abandon-modal', 'cancel-abandon-btn'],
  ])('closes the dialog of %s without acting', async (opener, modal, cancel) => {
    render(ProjectExplorerView);
    await fireEvent.click(await screen.findByTestId(opener));
    expect(screen.getByTestId(modal)).toBeInTheDocument();

    await fireEvent.click(screen.getByTestId(cancel));

    expect(screen.queryByTestId(modal)).toBeNull();
    expect(apiClient.releaseProject).not.toHaveBeenCalled();
    expect(apiClient.unenrolProject).not.toHaveBeenCalled();
    expect(apiClient.abandonOnboarding).not.toHaveBeenCalled();
  });

  it('releases without a reason when none is given', async () => {
    render(ProjectExplorerView);
    await fireEvent.click(await screen.findByTestId('release-btn-pm-mcp-clients'));

    await fireEvent.click(screen.getByTestId('confirm-release-btn'));

    await waitFor(() => expect(screen.queryByTestId('release-modal')).toBeNull());
    expect(apiClient.releaseProject).toHaveBeenCalledWith('pm-mcp-clients', undefined);
  });

  it('abandons a run that names no abandon link by its checkout and says why it failed', async () => {
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
      ...mockExplorer,
      being_added: [{ ...mockExplorer.being_added[0], abandon_link: undefined }],
    });
    vi.spyOn(apiClient, 'abandonOnboarding').mockRejectedValue(new Error('not a code'));
    render(ProjectExplorerView);
    await fireEvent.click(await screen.findByTestId('abandon-onboarding-btn'));

    await fireEvent.click(screen.getByTestId('confirm-abandon-btn'));

    expect(await screen.findByText('Failed to abandon onboarding')).toBeInTheDocument();
    expect(apiClient.abandonOnboarding).toHaveBeenCalledWith('~/git/cui-parent');
  });

  it('says no project matches and clears the filter', async () => {
    render(ProjectExplorerView);
    const input = await screen.findByTestId('project-filter-input');
    await fireEvent.input(input, { target: { value: 'nothing-like-this' } });

    expect(await screen.findByText("No projects match 'nothing-like-this'.")).toBeInTheDocument();
    await fireEvent.click(screen.getByRole('button', { name: 'Clear filter' }));

    expect(input).toHaveValue('');
    expect(screen.getByTestId('section-favourites')).toBeInTheDocument();
  });

  it('names a matched project by its id where its name differs', async () => {
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
      ...mockExplorer,
      all_projects: [{ ...mockExplorer.all_projects[1], display_name: 'CUI HTTP' }],
    });
    render(ProjectExplorerView);
    const input = await screen.findByTestId('project-filter-input');

    await fireEvent.input(input, { target: { value: 'cui' } });

    const card = await screen.findByTestId('project-card-cui-http');
    expect(card).toHaveTextContent('CUI HTTP');
    expect(card).toHaveTextContent('cui-http');
  });

  it('lists all projects in the served order when sorted by use', async () => {
    render(ProjectExplorerView);
    const select = await screen.findByTestId('project-sort-select');

    await fireEvent.change(select, { target: { value: 'most_used' } });

    const section = screen.getByTestId('section-all-projects');
    expect(section).toHaveTextContent('All projects (most used)');
    const cards = section.querySelectorAll('[data-testid^="project-card-"]');
    expect([...cards].map((card) => card.getAttribute('data-testid'))).toEqual([
      'project-card-API-Sheriff',
      'project-card-cui-http',
      'project-card-legacy-portal',
    ]);
  });

  it('opens running work from a project card', async () => {
    render(ProjectExplorerView);

    await fireEvent.click(await screen.findByTestId('running-work-btn-API-Sheriff'));

    expect(get(runtimeStore).currentRoute).toBe('overview');
  });

  it.each([
    ['text that names no favourite', { dataTransfer: { getData: () => 'not-a-favourite' } }],
    ['the favourite itself', { dataTransfer: { getData: () => 'pm-mcp-clients' } }],
    ['nothing at all', {}],
  ])('ignores a drop of %s', async (_case, init) => {
    render(ProjectExplorerView);
    const target = await screen.findByTestId('project-card-pm-mcp-clients');

    await fireEvent.drop(target, init);

    expect(apiClient.reorderFavouriteProjects).not.toHaveBeenCalled();
  });

  it('drags a favourite as a move carrying its id, onto a card that accepts it', async () => {
    render(ProjectExplorerView);
    const card = await screen.findByTestId('project-card-pm-mcp-core');
    const dataTransfer = { setData: vi.fn(), effectAllowed: 'all' };

    await fireEvent.dragStart(card, { dataTransfer });
    const accepted = await fireEvent.dragOver(screen.getByTestId('project-card-pm-mcp-clients'));

    expect(dataTransfer.setData).toHaveBeenCalledWith('text/plain', 'pm-mcp-core');
    expect(dataTransfer.effectAllowed).toBe('move');
    expect(accepted).toBe(false);
  });

  it('holds the order controls while a new order is written', async () => {
    vi.spyOn(apiClient, 'reorderFavouriteProjects').mockReturnValue(new Promise(() => {}));
    render(ProjectExplorerView);

    await fireEvent.click(await screen.findByTestId('favourite-down-pm-mcp-clients'));

    expect(screen.getByTestId('favourite-down-pm-mcp-clients')).toBeDisabled();
    expect(screen.getByTestId('favourite-down-pm-mcp-core')).toBeDisabled();
    await fireEvent.drop(screen.getByTestId('project-card-pm-mcp-core'), {
      dataTransfer: { getData: () => 'pm-mcp-clients' },
    });
    expect(apiClient.reorderFavouriteProjects).toHaveBeenCalledTimes(1);
  });

  it('leaves the filter empty when the address names a project the explorer does not hold', async () => {
    window.history.replaceState(null, '', '/projects?project=gone-project');
    render(ProjectExplorerView);

    await screen.findByTestId('section-favourites');

    expect(screen.getByTestId('project-filter-input')).toHaveValue('');
  });

  it('stops listening to project events and address steps once it is closed', async () => {
    const unsubscribe = vi.fn();
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(unsubscribe);
    const removed = vi.spyOn(window, 'removeEventListener');
    const { unmount } = render(ProjectExplorerView);
    await screen.findByTestId('section-favourites');

    unmount();

    expect(unsubscribe).toHaveBeenCalledTimes(1);
    expect(removed).toHaveBeenCalledWith('popstate', expect.any(Function));
    removed.mockRestore();
  });

  it('draws the error pattern when a later read of the explorer fails', async () => {
    let emit: (name: string) => void = () => {};
    vi.spyOn(apiClient, 'subscribeEvents').mockImplementation((handler) => {
      emit = (name) => handler(name, {});
      return () => {};
    });
    render(ProjectExplorerView);
    await screen.findByTestId('section-favourites');
    vi.mocked(apiClient.getProjectExplorer).mockRejectedValueOnce(new ClientError('timeout'));

    emit('project.changed');

    expect(await screen.findByTestId('explorer-error')).toHaveTextContent(
      'The request took too long',
    );
    expect(screen.queryByTestId('section-favourites')).toBeNull();
  });

  it('shows a run being added without the empty word when no project is enrolled yet', async () => {
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
      ...mockExplorer,
      favourites: [],
      recent: [],
      all_projects: [],
    });
    render(ProjectExplorerView);

    expect(await screen.findByTestId('section-being-added')).toBeInTheDocument();
    expect(screen.queryByTestId('empty-projects-state')).toBeNull();
    expect(screen.queryByTestId('section-favourites')).toBeNull();
    expect(screen.queryByTestId('section-recent')).toBeNull();
    expect(screen.queryByTestId('section-all-projects')).toBeNull();
  });

  it('names the setup a project needs, or that it needs its setup', async () => {
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
      ...mockExplorer,
      recent: [{ ...mockExplorer.recent[0], setup_needs_summary: undefined }],
    });
    render(ProjectExplorerView);

    const card = await screen.findByTestId('project-card-plan-marshall');

    expect(card).toHaveTextContent('setup needs: setup');
    expect(card).toHaveTextContent('! 1');
  });

  describe('the setup drawer', () => {
    it('shows the values of the project it was last opened for', async () => {
      let releaseFirst: (value: unknown) => void = () => {};
      vi.spyOn(apiClient, 'getProjectSettings')
        .mockImplementationOnce(() => new Promise((resolve) => (releaseFirst = resolve)) as never)
        .mockResolvedValueOnce({
          project_id: 'pm-mcp-core',
          revision: 3,
          values: [{ aspect: 'core.value', value: 'core', origin: 'project' }],
        });
      render(ProjectExplorerView);
      await fireEvent.click(await screen.findByTestId('setup-btn-pm-mcp-clients'));
      await fireEvent.click(screen.getByTestId('close-setup-btn'));
      await fireEvent.click(screen.getByTestId('setup-btn-pm-mcp-core'));
      await screen.findByTestId('setting-input-core.value');

      await act(async () => {
        releaseFirst({
          project_id: 'pm-mcp-clients',
          revision: 7,
          values: [{ aspect: 'landing', value: 'landing.main', origin: 'team' }],
        });
      });

      expect(screen.getByTestId('setup-drawer')).toHaveAccessibleName('Setup of pm-mcp-core');
      expect(screen.queryByTestId('setting-input-landing')).toBeNull();
    });

    /** Opens the drawer of pm-mcp-clients once its values are read. */
    async function openDrawer(): Promise<void> {
      render(ProjectExplorerView);
      await fireEvent.click(await screen.findByTestId('setup-btn-pm-mcp-clients'));
      await screen.findByTestId('setup-drawer');
    }

    it.each([
      ['its close button', () => fireEvent.click(screen.getByTestId('close-setup-btn'))],
      ['the backdrop', () => fireEvent.click(screen.getByTestId('setup-drawer-backdrop'))],
    ])('closes with %s', async (_name, close) => {
      await openDrawer();

      await close();

      expect(screen.queryByTestId('setup-drawer')).toBeNull();
    });

    it('stays open on a click inside it', async () => {
      await openDrawer();

      await fireEvent.click(screen.getByTestId('setup-drawer'));

      expect(screen.getByTestId('setup-drawer')).toBeInTheDocument();
    });

    it('says a value was saved and reads the values again', async () => {
      await openDrawer();
      await screen.findByTestId('save-setting-landing');

      await fireEvent.click(screen.getByTestId('save-setting-landing'));

      expect(await screen.findByTestId('setup-notice')).toHaveTextContent('Saved.');
      expect(apiClient.getProjectSettings).toHaveBeenCalledTimes(2);
    });

    it.each([
      ['refused', () => Promise.resolve({ applied: false, conflict: false, revision: 7 })],
      ['failed', () => Promise.reject(new ClientError('timeout'))],
    ])('says a value was not saved when the write is %s', async (_case, write) => {
      vi.spyOn(apiClient, 'saveProjectSettings').mockImplementation(write);
      await openDrawer();
      await screen.findByTestId('save-setting-landing');

      await fireEvent.click(screen.getByTestId('save-setting-landing'));

      expect(await screen.findByTestId('setup-update-error')).toHaveTextContent(
        'The value could not be saved',
      );
      expect(screen.queryByTestId('setup-notice')).toBeNull();
    });

    it('offers a free value as a text field and says when there is none', async () => {
      vi.spyOn(apiClient, 'getProjectSettings')
        .mockResolvedValueOnce({
          project_id: 'pm-mcp-clients',
          revision: 7,
          values: [{ aspect: 'sonar.key', value: null, origin: 'project' }],
        })
        .mockResolvedValueOnce({ project_id: 'pm-mcp-core', revision: 1, values: [] });
      await openDrawer();

      const field = await screen.findByTestId('setting-input-sonar.key');
      expect(field.tagName).toBe('INPUT');
      expect(field).toHaveValue('');
      expect(screen.getByText('from project')).toBeInTheDocument();

      await fireEvent.click(screen.getByTestId('close-setup-btn'));
      await fireEvent.click(screen.getByTestId('setup-btn-pm-mcp-core'));
      expect(await screen.findByText('This project records no value of its own.')).toBeInTheDocument();
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

  it('draws the error pattern when the setup values cannot be read', async () => {
    const settings = vi
      .spyOn(apiClient, 'getProjectSettings')
      .mockRejectedValue(new ClientError('timeout'));

    render(ProjectExplorerView);

    await waitFor(() => {
      expect(screen.getByTestId('setup-btn-pm-mcp-clients')).toBeInTheDocument();
    });
    await fireEvent.click(screen.getByTestId('setup-btn-pm-mcp-clients'));

    const error = await screen.findByTestId('setup-error');
    expect(error).toHaveTextContent('The request took too long');
    await fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
    await waitFor(() => expect(settings).toHaveBeenCalledTimes(2));
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

  it('moves the project focus with a back or forward step', async () => {
    window.history.replaceState(null, '', '/projects?project=API-Sheriff');
    render(ProjectExplorerView);

    const input = screen.getByTestId('project-filter-input');
    await waitFor(() => {
      expect(input).toHaveValue('API-Sheriff');
    });

    window.history.replaceState(null, '', '/projects?project=cui-http');
    window.dispatchEvent(new PopStateEvent('popstate'));
    await waitFor(() => {
      expect(input).toHaveValue('cui-http');
    });

    window.history.replaceState(null, '', '/projects');
    window.dispatchEvent(new PopStateEvent('popstate'));
    await waitFor(() => {
      expect(input).toHaveValue('');
    });
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
