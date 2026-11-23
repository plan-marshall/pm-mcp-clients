<script lang="ts">
  import { onMount } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import { formatTime } from '../display/format';
  import { displayState } from '../display';
  import { writeAddress } from '../navigation/history';
  import {
    navigateTo,
    openProjectOnboarding,
    openEndedPlanView,
    openEpicView,
    pushRoute,
    runtimeStore,
    selectObjectTab,
  } from '../stores/runtime';
  import { openStartWork } from '../stores/start';
  import Button from '../components/Button.svelte';
  import Icon from '../components/Icon.svelte';
  import KindMark from '../components/KindMark.svelte';
  import RowLink from '../components/RowLink.svelte';
  import RowMenu from '../components/RowMenu.svelte';
  import StatusLabel from '../components/StatusLabel.svelte';
  import ReasonBadge from '../components/ReasonBadge.svelte';
  import FilterInput from '../components/FilterInput.svelte';
  import Toggle from '../components/Toggle.svelte';
  import Tabs from '../components/Tabs.svelte';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import ConfirmDialog from '../components/ConfirmDialog.svelte';
  import TextArea from '../components/TextArea.svelte';
  import { actsFor, type ActSpec } from '../actions/byState';
  import ActionBar, { type ActionBarAct } from '../components/ActionBar.svelte';
  import { actFailureText, errorCodeOf } from '../api/errors';
  import { refreshesProjectView } from '../stores/refreshEvents';
  import {
    apiClient,
    type ProjectExplorer,
    type EnrolledProject,
    type BeingAddedProject,
    type ProjectSettingsConflict,
    type ProjectSettingsRecord,
    type ProjectSettingsValue,
    type ProjectDetailResponse,
    type EndedWorkPage,
    type ProjectOnboardingState,
    type OverSizeDistributionsRecord,
  } from '../api/client';

  /**
   * The Projects lens (UX-P-10): the list column of the operator's projects and, beside it, the one
   * project page (`/projects/{id}`) as the object column.
   *
   * A row is a `RowLink` — a click anywhere opens the project page — with the readiness as a
   * `StatusLabel`, what the setup needs as a `ReasonBadge`, the work counts and the age of the last
   * activity; a ready project carries _Add a plan_ beside its marks, so a plan starts without
   * opening the project first. Every other act lives in the row's `RowMenu`, the two destructive
   * ones included, each behind its own confirmation. The favourites are ordered by drag and by a
   * keyboard route (Alt with the arrow keys), never by arrow buttons.
   */

  let explorer = $state<ProjectExplorer | null>(null);
  let isLoading = $state(true);
  let errorMessage = $state<string | null>(null);
  /** A failed act on the list, shown beside it instead of replacing a loaded list. */
  let actionError = $state('');

  let filter = $state('');
  let sortBy = $state<'name' | 'most_used'>('name');

  // Favourites ordering: the id being dragged, and whether a write is in flight.
  let dragId = $state<string | null>(null);
  let isReordering = $state(false);
  let reorderError = $state('');

  let releaseTarget = $state<EnrolledProject | null>(null);
  let releaseReason = $state('');
  let isReleasing = $state(false);
  let releaseError = $state<string | null>(null);

  let unenrolTarget = $state<EnrolledProject | null>(null);
  let isUnenrolling = $state(false);
  let unenrolError = $state<string | null>(null);

  let abandonTarget = $state<BeingAddedProject | null>(null);
  let isAbandoning = $state(false);
  let abandonError = $state<string | null>(null);

  // The project page's own reads: what runs, what ended, and — on its setup tab — the values, the
  // steps of the setup and the over-size spread of its work.
  let detail = $state<ProjectDetailResponse | null>(null);
  let isLoadingDetail = $state(false);
  let detailError = $state<string | null>(null);
  let endedWork = $state<EndedWorkPage | null>(null);
  let isLoadingEnded = $state(false);
  let endedError = $state<string | null>(null);
  let showEnded = $state(false);
  /** The ended work's page the page shows; the API's `has_older` carries it on. */
  let endedPage = $state(1);
  /**
   * The generation of the page's reads: a response that belongs to a project the operator has left
   * never lands under another project's heading.
   */
  let pageGeneration = 0;
  /** Whether the operator asked for the ended work while opening the project from the list. */
  let pendingEnded = false;

  let settings = $state<ProjectSettingsRecord | null>(null);
  let settingsLoading = $state(false);
  let settingsLoadError = $state<string | null>(null);
  let settingsError = $state('');
  let settingsNotice = $state('');
  let settingsDrafts = $state<Record<string, string>>({});
  let savingAspects = $state<string[]>([]);
  let settingsConflict = $state<ProjectSettingsConflict | null>(null);
  /** A write that landed while the panel could not read its values again: a refresh failure only. */
  let settingsRefreshError = $state<string | null>(null);

  let onboarding = $state<ProjectOnboardingState | null>(null);
  let isLoadingOnboarding = $state(false);
  let onboardingError = $state<string | null>(null);

  let overSize = $state<OverSizeDistributionsRecord | null>(null);
  let isLoadingOverSize = $state(false);
  let overSizeError = $state<string | null>(null);

  /** The project the setup tab's reads belong to, so a page that is opened anew reads them again. */
  let setupProjectId = $state<string | null>(null);

  /** The project the page's own reads belong to, so a re-render does not read them again. */
  let pageProjectId = $state<string | null>(null);

  const requestedProjectId = $derived(
    $runtimeStore.currentRoute === 'project' ? ($runtimeStore.selectedProjectId ?? null) : null,
  );
  const activeTab = $derived($runtimeStore.objectTab === 'setup' ? 'setup' : 'work');

  const listProjects = $derived.by(() => {
    if (!explorer) return [];
    const seen = new Set<string>();
    return [...explorer.favourites, ...explorer.recent, ...explorer.all_projects].filter((p) => {
      if (seen.has(p.project_id)) return false;
      seen.add(p.project_id);
      return true;
    });
  });

  const selectedProject = $derived.by(() => {
    if (!requestedProjectId) return null;
    return listProjects.find((p) => p.project_id === requestedProjectId) ?? null;
  });

  const filteredProjects = $derived.by(() => {
    const query = filter.trim().toLowerCase();
    if (!query) return [];
    return listProjects.filter(
      (p) =>
        p.project_id.toLowerCase().includes(query) || p.display_name.toLowerCase().includes(query),
    );
  });

  const sortedAllProjects = $derived.by(() => {
    if (!explorer) return [];
    const copy = [...explorer.all_projects];
    if (sortBy === 'name') {
      copy.sort((a, b) => a.display_name.localeCompare(b.display_name));
    }
    return copy;
  });

  async function loadExplorer() {
    isLoading = true;
    errorMessage = null;
    // A reload answers for the last act's failure too: the banner it leaves is no longer news.
    actionError = '';
    try {
      explorer = await apiClient.getProjectExplorer();
    } catch (err: unknown) {
      errorMessage = errorCodeOf(err);
    } finally {
      isLoading = false;
    }
  }

  onMount(() => {
    void loadExplorer();

    const unsubscribeEvents = apiClient.subscribeEvents((event) => {
      if (refreshesProjectView(event)) {
        void loadExplorer();
        if (requestedProjectId) void loadProjectPage(requestedProjectId);
      }
    });

    return () => {
      unsubscribeEvents();
    };
  });

  // The page reads what it shows once per opened project: the work tab always, the setup tab's own
  // reads only while that tab is shown. The effect compares the project it has read with the one the
  // address names — never the read's own result — so the read cannot re-trigger it.
  $effect(() => {
    const projectId = requestedProjectId;
    if (projectId === pageProjectId) return;
    pageProjectId = projectId;
    // A newer project owns the page: every read and the operator's edits belong to it alone.
    pageGeneration += 1;
    detail = null;
    endedWork = null;
    endedError = null;
    endedPage = 1;
    const wantedEnded = pendingEnded;
    showEnded = wantedEnded;
    pendingEnded = false;
    settings = null;
    settingsConflict = null;
    settingsError = '';
    settingsNotice = '';
    settingsRefreshError = null;
    onboarding = null;
    overSize = null;
    setupProjectId = null;
    if (projectId) {
      void loadProjectPage(projectId);
      if (wantedEnded) void loadEndedWork(projectId, 1);
    }
  });

  $effect(() => {
    const projectId = requestedProjectId;
    if (!projectId || activeTab !== 'setup' || setupProjectId === projectId) return;
    void loadSetup(projectId);
  });

  /**
   * Loads what the project page shows for one project.
   *
   * @param projectId the project
   */
  async function loadProjectPage(projectId: string): Promise<void> {
    const generation = ++pageGeneration;
    isLoadingDetail = true;
    detailError = null;
    try {
      const loaded = await apiClient.getProjectDetail(projectId);
      if (generation !== pageGeneration) return;
      detail = loaded;
    } catch (err: unknown) {
      if (generation !== pageGeneration) return;
      detail = null;
      detailError = errorCodeOf(err);
    } finally {
      if (generation === pageGeneration) isLoadingDetail = false;
    }
  }

  /**
   * Loads the read-only spread of the project's over-size work.
   *
   * @param projectId the project
   */
  async function loadOverSize(projectId: string): Promise<void> {
    const generation = pageGeneration;
    isLoadingOverSize = true;
    overSizeError = null;
    try {
      const loaded = await apiClient.getOverSize(projectId);
      if (generation !== pageGeneration) return;
      overSize = loaded;
    } catch (err: unknown) {
      if (generation !== pageGeneration) return;
      overSize = null;
      overSizeError = errorCodeOf(err);
    } finally {
      if (generation === pageGeneration) isLoadingOverSize = false;
    }
  }

  /**
   * Loads the setup tab: the project's own values, the steps of its setup and the over-size spread.
   *
   * @param projectId the project
   */
  async function loadSetup(projectId: string): Promise<void> {
    setupProjectId = projectId;
    const generation = pageGeneration;
    void loadOverSize(projectId);
    settingsLoading = true;
    settingsLoadError = null;
    settingsError = '';
    settingsRefreshError = null;
    onboardingError = null;
    isLoadingOnboarding = true;
    try {
      const loaded = await apiClient.getProjectSettings(projectId);
      if (generation !== pageGeneration) return;
      settings = loaded;
      const drafts: Record<string, string> = {};
      for (const value of loaded.values) {
        drafts[value.aspect] = value.value ?? '';
      }
      settingsDrafts = drafts;
    } catch (err: unknown) {
      if (generation !== pageGeneration) return;
      settings = null;
      settingsLoadError = errorCodeOf(err);
    } finally {
      if (generation === pageGeneration) settingsLoading = false;
    }
    try {
      const loaded = await apiClient.getProjectOnboarding(projectId);
      if (generation !== pageGeneration) return;
      onboarding = loaded;
    } catch (err: unknown) {
      if (generation !== pageGeneration) return;
      onboarding = null;
      onboardingError = errorCodeOf(err);
    } finally {
      if (generation === pageGeneration) isLoadingOnboarding = false;
    }
  }

  /**
   * Opens the project page of one project.
   *
   * @param project the project
   */
  function openProject(project: EnrolledProject): void {
    pushRoute('project', { projectId: project.project_id });
  }

  /**
   * Opens one tab of the project page, from the list or from the page itself.
   *
   * @param projectId the project
   * @param tab the tab to show
   */
  function openProjectTab(projectId: string, tab: 'work' | 'setup'): void {
    if (requestedProjectId !== projectId) {
      pushRoute('project', { projectId });
    }
    selectObjectTab(tab);
    if (tab === 'setup' && settings === null) {
      void loadSetup(projectId);
    }
  }

  /**
   * Loads one page of the project's ended work.
   *
   * @param projectId the project
   * @param page the page to read, 1-based
   */
  async function loadEndedWork(projectId: string, page = 1): Promise<void> {
    const generation = pageGeneration;
    isLoadingEnded = true;
    endedError = null;
    try {
      const loaded = await apiClient.getEndedWork(projectId, page);
      if (generation !== pageGeneration) return;
      endedWork = loaded;
      endedPage = page;
    } catch (err: unknown) {
      if (generation !== pageGeneration) return;
      endedWork = null;
      endedError = errorCodeOf(err);
    } finally {
      if (generation === pageGeneration) isLoadingEnded = false;
    }
  }

  /**
   * @param tab the chosen tab of the project page
   */
  function selectTab(tab: string): void {
    if (!requestedProjectId) return;
    openProjectTab(requestedProjectId, tab === 'setup' ? 'setup' : 'work');
  }

  async function handleToggleFavourite(project: EnrolledProject) {
    try {
      await apiClient.toggleFavourite(project.project_id, !project.is_favourite);
      await loadExplorer();
    } catch {
      actionError = $t('project.explorer.favourite_failed');
    }
  }

  async function handleStartOnboarding() {
    openProjectOnboarding();
  }

  /**
   * Opens the run a being-added project waits at, on the project onboarding route.
   *
   * @param being the explorer's report of the run
   */
  function handleContinueOnboarding(being: BeingAddedProject) {
    openProjectOnboarding(being.checkout_path);
  }

  async function handleConfirmRelease() {
    if (!releaseTarget) return;
    isReleasing = true;
    releaseError = null;
    try {
      await apiClient.releaseProject(releaseTarget.project_id, releaseReason.trim() || undefined);
      releaseTarget = null;
      releaseReason = '';
      await loadExplorer();
    } catch (err: unknown) {
      releaseError = actFailureText(err, $t('project.release.failed'));
    } finally {
      isReleasing = false;
    }
  }

  async function handleConfirmUnenrol() {
    if (!unenrolTarget) return;
    const target = unenrolTarget;
    isUnenrolling = true;
    unenrolError = null;
    try {
      await apiClient.unenrolProject(target.project_id);
      unenrolTarget = null;
      await loadExplorer();
      if (requestedProjectId === target.project_id) {
        navigateTo('projects');
      }
    } catch (err: unknown) {
      // The refusal's own code is what tells the two cases apart; it is never printed.
      if (errorCodeOf(err) === 'scope_active') {
        unenrolError =
          $t('project.release.refused', { project: target.display_name }) +
          '. ' +
          $t('project.release.refused_hint');
      } else {
        unenrolError = actFailureText(err, $t('project.release.unenrol_failed'));
      }
    } finally {
      isUnenrolling = false;
    }
  }

  async function handleConfirmAbandon() {
    if (!abandonTarget) return;
    isAbandoning = true;
    abandonError = null;
    try {
      const link = abandonTarget.abandon_link || abandonTarget.checkout_path;
      await apiClient.abandonOnboarding(link);
      abandonTarget = null;
      await loadExplorer();
    } catch (err: unknown) {
      abandonError = actFailureText(err, $t('project.adding.abandon_failed'));
    } finally {
      isAbandoning = false;
    }
  }

  /**
   * Moves one favourite to another position and persists the operator's order.
   *
   * @param fromIndex the favourite's current index
   * @param toIndex the index it takes
   */
  function moveFavourite(fromIndex: number, toIndex: number): void {
    if (!explorer || isReordering) return;
    if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0) return;
    const next = [...explorer.favourites];
    const [moved] = next.splice(fromIndex, 1);
    next.splice(toIndex, 0, moved);
    void persistFavouriteOrder(next);
  }

  /**
   * The keyboard route of the favourites' order: Alt with the arrow keys on the row moves it one
   * place, the route that replaces the arrow buttons (`project-explorer.adoc`, #marks).
   *
   * @param event the row's key event
   * @param index the favourite's index
   */
  function handleFavouriteKeydown(event: KeyboardEvent, index: number): void {
    if (!event.altKey || !explorer) return;
    // The route belongs to the row: the title link is the row's keyboard focus, so the keys work
    // there, while a key pressed in another control (the favourite toggle, Add a plan, the menu)
    // keeps its own meaning.
    const target = event.target as HTMLElement | null;
    const control = target?.closest('button, a, input, select, textarea') as HTMLElement | null;
    if (control !== null && !control.classList.contains('row-link__title')) return;
    const delta = event.key === 'ArrowUp' ? -1 : event.key === 'ArrowDown' ? 1 : 0;
    if (delta === 0) return;
    const neighbour = index + delta;
    if (neighbour < 0 || neighbour >= explorer.favourites.length) return;
    event.preventDefault();
    moveFavourite(index, neighbour);
  }

  /**
   * @param event the drag event
   * @param projectId the favourite being dragged
   */
  function startFavouriteDrag(event: DragEvent, projectId: string): void {
    dragId = projectId;
    event.dataTransfer?.setData('text/plain', projectId);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  /**
   * Drops a favourite onto another, taking the dropped-on favourite's position.
   *
   * @param event the drop event
   * @param targetId the favourite it was dropped on
   */
  function dropFavourite(event: DragEvent, targetId: string): void {
    event.preventDefault();
    const sourceId = dragId ?? event.dataTransfer?.getData('text/plain') ?? '';
    dragId = null;
    if (!explorer || !sourceId || sourceId === targetId) return;
    const from = explorer.favourites.findIndex((project) => project.project_id === sourceId);
    const to = explorer.favourites.findIndex((project) => project.project_id === targetId);
    if (from < 0 || to < 0) return;
    moveFavourite(from, to);
  }

  /**
   * Writes the new order, and restores the old one when the runtime refuses it.
   *
   * @param next the favourites in their new order
   */
  async function persistFavouriteOrder(next: EnrolledProject[]): Promise<void> {
    if (!explorer) return;
    const previous = explorer;
    explorer = { ...explorer, favourites: next };
    isReordering = true;
    reorderError = '';
    try {
      await apiClient.reorderFavouriteProjects(next.map((project) => project.project_id));
    } catch {
      explorer = previous;
      reorderError = $t('project.explorer.reorder_failed');
    } finally {
      isReordering = false;
    }
  }

  /**
   * Saves one value against the revision the setup tab was opened with.
   *
   * @param value the aspect being saved
   */
  async function saveSetting(value: ProjectSettingsValue): Promise<void> {
    if (!settings || !requestedProjectId) return;
    await applyProjectSetting(
      value.aspect,
      settingsDrafts[value.aspect] ?? '',
      settings.revision,
    );
  }

  /**
   * Applies one value, and opens the conflict card instead of overwriting a value that changed in
   * another client.
   *
   * @param aspect the value's aspect
   * @param wanted this browser's choice
   * @param revision the revision the change is sent against
   */
  async function applyProjectSetting(
    aspect: string,
    wanted: string,
    revision: number,
  ): Promise<void> {
    const projectId = requestedProjectId;
    if (!projectId) return;
    const generation = pageGeneration;
    savingAspects = [...savingAspects, aspect];
    settingsError = '';
    settingsNotice = '';
    settingsRefreshError = null;
    settingsConflict = null;
    try {
      const result = await apiClient.saveProjectSettings(projectId, aspect, wanted, revision);
      // A write that lands after the operator left the project never touches the new project's panel.
      if (generation !== pageGeneration) return;
      if (result.conflict) {
        settingsConflict = {
          aspect,
          currentValue: result.current_value ?? '',
          wantedValue: wanted,
          revision: result.revision,
        };
        return;
      }
      if (!result.applied) {
        settingsError = $t('project.settings.update_failed');
        return;
      }
      settingsNotice = $t('project.settings.saved');
      // The write is done; only its own refresh may fail, and that failure is a read, not the write.
      try {
        const refreshed = await apiClient.getProjectSettings(projectId);
        if (generation !== pageGeneration) return;
        settings = refreshed;
        const drafts: Record<string, string> = {};
        for (const value of refreshed.values) {
          drafts[value.aspect] = value.value ?? '';
        }
        settingsDrafts = drafts;
      } catch {
        if (generation !== pageGeneration) return;
        settingsRefreshError = $t('project.settings.refresh_failed');
      }
    } catch {
      if (generation === pageGeneration) settingsError = $t('project.settings.update_failed');
    } finally {
      savingAspects = savingAspects.filter((busy) => busy !== aspect);
    }
  }

  /** Applies this browser's choice again, on the revision the other client left behind. */
  async function applyConflictAgain(): Promise<void> {
    if (!settingsConflict) return;
    const { aspect, wantedValue, revision } = settingsConflict;
    settingsConflict = null;
    await applyProjectSetting(aspect, wantedValue, revision);
  }

  /** Keeps the other client's value and closes the card, without writing anything. */
  async function keepCurrentValue(): Promise<void> {
    settingsConflict = null;
    if (requestedProjectId) {
      await loadSetup(requestedProjectId);
    }
  }

  /**
   * @param timeStr the instant the runtime sends
   * @returns its age in the unit that age has (`9 d ago`), or `since 14:02` for a moment of today
   */
  function formatRelative(timeStr?: string | null): string {
    return formatTime(timeStr, 'relative', $currentLocale);
  }

  /**
   * @param timeStr the instant the runtime sends
   * @returns the moment itself, for the row's hover advisory
   */
  function formatMoment(timeStr?: string | null): string {
    return formatTime(timeStr, 'short', $currentLocale);
  }

  /**
   * The readiness word, through the shared display vocabulary: `setup_needs` is the state the design
   * draws as `setup open`.
   *
   * @param project the project
   * @returns the word of its state
   */
  function readinessWord(project: EnrolledProject): string {
    const value = project.readiness === 'setup_needs' ? 'setup_open' : project.readiness;
    return displayState('project', value, $t);
  }

  /**
   * @param project the project
   * @returns the tone the readiness dot takes
   */
  function readinessTone(project: EnrolledProject): 'success' | 'attention' | 'neutral' {
    if (project.readiness === 'ready') return 'success';
    if (project.readiness === 'setup_needs') return 'attention';
    return 'neutral';
  }

  /**
   * @param project the project
   * @returns the state key of the project's acts table
   */
  function projectState(project: EnrolledProject): string {
    if (project.readiness === 'setup_needs') return 'setup_needs';
    if (project.readiness === 'released') return 'released';
    return 'ready';
  }

  /**
   * The acts of one project's state, with the catalogue words resolved.
   *
   * @param project the project
   * @returns its acts, the most important first
   */
  function projectActs(project: EnrolledProject): ActSpec[] {
    return actsFor('project', projectState(project));
  }

  /**
   * The reason an _Add a plan_ act is disabled on a project that is not ready, from the catalogue.
   *
   * @param project the project
   * @returns the advisory, or `undefined` when the act can run
   */
  function addPlanReason(project: EnrolledProject): string | undefined {
    if (project.readiness === 'ready') return undefined;
    return $t('project.action.disabled_not_ready', {
      project: project.display_name,
      need: $t('overview.needs.setup'),
    });
  }

  /**
   * Runs one act of the project's table.
   *
   * @param project the project
   * @param act the act chosen
   */
  function runProjectAct(project: EnrolledProject, act: string): void {
    switch (act) {
      case 'add_plan':
        openStartWork(project.project_id, 'plan');
        return;
      case 'add_epic':
        openStartWork(project.project_id, 'epic');
        return;
      case 'open_running_work':
        openProjectTab(project.project_id, 'work');
        return;
      case 'continue_setup':
      case 'enrol_again':
        openProjectOnboarding(null, project.project_id);
        return;
      case 'setup':
        openProjectTab(project.project_id, 'setup');
        return;
      case 'ended_work':
        openEnded(project);
        return;
      case 'release':
        releaseTarget = project;
        releaseReason = '';
        releaseError = null;
        return;
      case 'unenrol':
        unenrolTarget = project;
        unenrolError = null;
        return;
      default:
        return;
    }
  }

  /**
   * Opens the ended work of the project on its page.
   *
   * @param project the project
   */
  function openEnded(project: EnrolledProject): void {
    if (requestedProjectId !== project.project_id) {
      // The page's own effect opens the project; the intent that it shows the ended work travels
      // with it, so the deferred reset cannot close the section the operator asked for.
      pendingEnded = true;
      openProjectTab(project.project_id, 'work');
      return;
    }
    showEnded = true;
    void loadEndedWork(project.project_id, 1);
  }

  /**
   * The row menu's entries: the state's acts without the one visible act a row draws itself and
   * without the one a click on the row already takes. The two destructive acts stay in this menu,
   * behind the confirmation the view opens for them.
   *
   * @param project the project
   * @returns the menu's items
   */
  function rowMenuItems(project: EnrolledProject) {
    return projectActs(project)
      .filter((act) => act.id !== 'add_plan')
      .map((act) => {
        const notReady = project.readiness !== 'ready';
        const gate = notReady && (act.id === 'add_plan' || act.id === 'add_epic');
        return {
          id: act.id,
          label: $t(act.labelKey),
          advisory: act.advisoryKey ? $t(act.advisoryKey) : undefined,
          destructive: act.destructive === true,
          disabled: gate || act.disabled,
          disabledReason: gate ? addPlanReason(project) : act.disabledReason,
          onclick: () => runProjectAct(project, act.id),
        };
      });
  }

  /**
   * Handles a click on a project row: the whole row opens the project page, except a control of its
   * own (the favourite toggle, _Add a plan_, the row menu), which stays its own target.
   *
   * @param event the click
   * @param project the project the row stands for
   */
  function onRowClick(event: MouseEvent, project: EnrolledProject): void {
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
      return;
    }
    const target = event.target as HTMLElement | null;
    if (target?.closest('button') !== null) return;
    if (target?.closest('[role="menu"]') !== null) return;
    const anchor = target?.closest('a');
    if (anchor !== null && !anchor.classList.contains('row-link__title')) return;
    event.preventDefault();
    openProject(project);
  }

  /**
   * @param project the project
   * @returns the label of its row menu
   */
  function rowMenuLabel(project: EnrolledProject): string {
    return $t('project.action.more_for', { project: project.display_name });
  }

  /**
   * The acts of the project page's head as the `ActionBar` draws them: one primary, the secondary
   * acts beside it, and everything else — the two destructive acts included — in the overflow.
   *
   * @param project the project
   * @returns the bar's acts, the most important first
   */
  function pageActs(project: EnrolledProject): ActionBarAct[] {
    const order =
      project.readiness === 'released'
        ? ['enrol_again', 'ended_work', 'unenrol']
        : project.readiness === 'setup_needs'
          ? ['continue_setup', 'add_plan', 'add_epic', 'setup', 'release', 'unenrol']
          : ['add_plan', 'add_epic', 'setup', 'release', 'unenrol'];
    const byId = new Map(projectActs(project).map((act) => [act.id, act]));
    const acts: ActionBarAct[] = [];
    order.forEach((id, index) => {
      const act = byId.get(id);
      if (!act) return;
      const role: ActionBarAct['role'] =
        index === 0 ? 'primary' : index <= 2 ? 'secondary' : 'overflow';
      const notReady = project.readiness !== 'ready';
      const gate = notReady && (id === 'add_plan' || id === 'add_epic');
      acts.push({
        id: act.id,
        label: $t(act.labelKey),
        advisory: act.advisoryKey ? $t(act.advisoryKey) : undefined,
        role,
        destructive: act.destructive,
        disabled: gate || act.disabled,
        disabledReason: gate ? addPlanReason(project) : act.disabledReason,
      });
    });
    return acts;
  }

  /**
   * Opens an ended plan or epic of the project.
   *
   * @param item the ended work row
   */
  function openEndedItem(item: EndedWorkPage['items'][number]): void {
    const id = item.id ?? item.item_id;
    if (!id) return;
    if (item.kind === 'epic') {
      openEpicView(id);
      return;
    }
    openEndedPlanView({
      plan_id: id,
      outcome: item.outcome ?? null,
      reason: item.reason ?? null,
      ended_at: item.ended_at ?? null,
    });
  }

  /**
   * @param entries the distributions the runtime reports
   * @returns their rows as `role — multiple` text pairs
   */
  function overSizeRows(entries: OverSizeDistributionsRecord | null): Array<[string, string]> {
    if (!entries) return [];
    return entries.distributions.map((entry) => [
      entry.role,
      entry.proposed_multiple === undefined
        ? `${entry.current_multiple}`
        : `${entry.current_multiple} → ${entry.proposed_multiple}`,
    ]);
  }

  /**
   * @param step the step of the project's setup
   * @returns its status word
   */
  function stepStatus(step: { status?: string | null }): string {
    const status = step.status ?? '';
    if (status === 'done') return $t('project.onboarding.step_done');
    if (status === 'waiting') return $t('project.onboarding.step_waiting');
    if (status === 'needs_repair') return $t('project.onboarding.step_waiting');
    return $t('project.onboarding.step_current');
  }
</script>

{#snippet projectRow(project: EnrolledProject, index: number, draggable: boolean)}
  <!-- The row's own keyboard route is its RowLink anchor; the wrapper only carries the drag and the
       favourite order's Alt-with-arrows route, so it is a presentation box, never a control. -->
  <!-- svelte-ignore a11y_click_events_have_key_events a11y_no_static_element_interactions -->
  <div
    class="project-row"
    role="presentation"
    class:project-row--draggable={draggable}
    draggable={draggable ? 'true' : undefined}
    ondragstart={draggable ? (event) => startFavouriteDrag(event, project.project_id) : undefined}
    ondragover={draggable ? (event) => event.preventDefault() : undefined}
    ondrop={draggable ? (event) => dropFavourite(event, project.project_id) : undefined}
    onkeydown={draggable ? (event) => handleFavouriteKeydown(event, index) : undefined}
    onclick={(event) => onRowClick(event, project)}
    data-testid="project-row-{project.project_id}"
  >
    <RowLink
      href={`/projects/${project.project_id}`}
      title={project.display_name}
      current={project.project_id === requestedProjectId}
    >
      {#snippet kind()}
        <KindMark kind="project" wordHidden />
      {/snippet}
      {#snippet tags()}
        <StatusLabel tone={readinessTone(project)} label={readinessWord(project)} fixed />
        {#if project.needs_operator_count > 0}
          <ReasonBadge reason="setup" count={project.needs_operator_count} />
        {/if}
        <span class="project-counts text-xs text-secondary">
          {$t('project.explorer.counts', {
            plans: project.active_plans_count,
            epics: project.active_epics_count,
          })}
        </span>
        {#if project.last_active}
          <span class="project-last-active text-xs text-muted" title={formatMoment(project.last_active)}
            >{formatRelative(project.last_active)}</span
          >
        {/if}
      {/snippet}
      {#snippet acts()}
        <Toggle
          pressed={project.is_favourite}
          label={project.is_favourite
            ? $t('project.action.unfavourite')
            : $t('project.action.favourite')}
          icon="act.favourite"
          pressedIcon="act.favourite-on"
          onToggle={() => handleToggleFavourite(project)}
          data-testid="star-btn-{project.project_id}"
        />
        <Button
          variant="secondary"
          size="sm"
          disabled={project.readiness !== 'ready'}
          disabledReason={addPlanReason(project)}
          on:click={() => runProjectAct(project, 'add_plan')}
          data-testid="add-plan-{project.project_id}"
        >
          {$t('project.action.add_plan')}
        </Button>
        <RowMenu
          items={rowMenuItems(project)}
          label={rowMenuLabel(project)}
          advisory={$t('web.common.more.title')}
          confirmDestructive={false}
          data-testid="row-menu-{project.project_id}"
        />
      {/snippet}
      {#snippet detail()}
        {#if project.display_name !== project.project_id}
          <span class="mono project-id">{project.project_id}</span>
        {/if}
        {#if project.remote_url}
          <span class="project-remote">{project.remote_url}</span>
        {/if}
      {/snippet}
    </RowLink>
  </div>
{/snippet}

<div class="project-explorer" data-testid="project-explorer-view">
  <div class="projects-lens" class:projects-lens--object-open={selectedProject !== null}>
    <section class="projects-list" aria-labelledby="projects-list-title">
      <header class="explorer-header">
        <div class="explorer-header__title-row">
          <h1 class="explorer-title" id="projects-list-title">{$t('project.explorer.title')}</h1>
          <Button variant="primary" on:click={handleStartOnboarding} data-testid="add-project-btn">
            + {$t('project.explorer.add')}
          </Button>
        </div>

        <div class="explorer-controls">
          <FilterInput
            value={filter}
            label={$t('common.filter.label', { what: $t('project.explorer.title') })}
            placeholder={$t('project.explorer.filter')}
            testid="project-filter-input"
            onchange={(value) => (filter = value)}
          />

          <div class="explorer-sort">
            <label for="sort-select" class="sort-label">{$t('project.explorer.sort_label')}</label>
            <select
              id="sort-select"
              class="sort-select"
              value={sortBy}
              onchange={(event) =>
                (sortBy = (event.currentTarget as HTMLSelectElement).value as
                  | 'name'
                  | 'most_used')}
              data-testid="project-sort-select"
            >
              <option value="name">{$t('project.explorer.by_name')}</option>
              <option value="most_used">{$t('project.explorer.most_used')}</option>
            </select>
          </div>
        </div>
      </header>

      {#if isLoading && !explorer}
        <div class="explorer-loading" role="status">
          <Skeleton shape="card" rows={3} label={$t('project.explorer.loading')} />
        </div>
      {:else if errorMessage !== null && !explorer}
        <div class="alert alert--danger" data-testid="explorer-error">
          <ErrorState
            code={errorMessage}
            actionLabel={$t('common.action.retry')}
            onAct={loadExplorer}
            titleLevel={2}
            announce={explorer !== null}
          />
        </div>
      {:else if explorer}
        {#if errorMessage !== null}
          <!-- A refresh that failed leaves the rows the operator already has; the error stands beside
               them with its retry, never in place of the list. -->
          <div class="alert alert--danger" role="alert" data-testid="explorer-refresh-error">
            <ErrorState
              code={errorMessage}
              actionLabel={$t('common.action.retry')}
              onAct={loadExplorer}
              titleLevel={2}
              announce={true}
            />
          </div>
        {/if}
        {#if actionError}
          <div class="alert alert--danger" role="alert" data-testid="explorer-action-error">
            {actionError}
          </div>
        {/if}
        {#if reorderError}
          <div class="alert alert--danger" role="alert" data-testid="reorder-error">
            {reorderError}
          </div>
        {/if}

        {#if explorer.all_projects.length === 0 && explorer.being_added.length === 0}
          <div class="empty-state" data-testid="empty-projects-state">
            <p class="empty-state__text">{$t('project.explorer.empty')}</p>
            <Button variant="primary" on:click={handleStartOnboarding}>
              + {$t('project.explorer.add')}
            </Button>
          </div>
        {:else if filter.trim().length > 0}
          <section class="explorer-section" data-testid="section-filtered-matches">
            <h2 class="section-title">
              {$t('project.explorer.matches', {
                filter: filter.trim(),
                count: filteredProjects.length,
              })}
            </h2>
            {#if filteredProjects.length === 0}
              <p class="text-muted">
                {$t('project.explorer.no_matches', { filter: filter.trim() })}
              </p>
            {:else}
              <ul class="project-rows">
                {#each filteredProjects as project (project.project_id)}
                  <li>{@render projectRow(project, -1, false)}</li>
                {/each}
              </ul>
            {/if}
          </section>
        {:else}
          {#if explorer.being_added.length > 0}
            <section class="explorer-section" data-testid="section-being-added">
              <h2 class="section-title">{$t('project.adding.title')}</h2>
              <ul class="project-rows">
                {#each explorer.being_added as being (being.checkout_path)}
                  <li class="being-added-row" data-testid="being-added-row">
                    <!-- The row's keyboard route is the RowLink anchor; this wrapper only carries the
                         pointer target, so it is a presentation box, never a control. -->
                    <!-- svelte-ignore a11y_click_events_have_key_events a11y_no_static_element_interactions -->
                    <div
                      class="being-added-row__inner"
                      role="presentation"
                      onclick={(event) => {
                        const target = event.target as HTMLElement | null;
                        if (target?.closest('button') !== null) return;
                        const anchor = target?.closest('a');
                        if (anchor !== null && !anchor.classList.contains('row-link__title')) return;
                        event.preventDefault();
                        handleContinueOnboarding(being);
                      }}
                    >
                      <RowLink href="/projects/onboard" title={being.checkout_path}>
                      {#snippet tags()}
                        <span class="being-added-step text-sm">
                          {$t('project.adding.step', {
                            step: being.step_number,
                            total: being.total_steps,
                            name: being.current_step,
                          })}
                        </span>
                      {/snippet}
                      {#snippet acts()}
                        <Button
                          variant="secondary"
                          size="sm"
                          on:click={() => handleContinueOnboarding(being)}
                          data-testid="continue-onboarding-btn"
                        >
                          {$t('project.adding.continue')}
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          on:click={() => {
                            abandonTarget = being;
                            abandonError = null;
                          }}
                          data-testid="abandon-onboarding-btn"
                        >
                          {$t('project.adding.abandon')}
                        </Button>
                      {/snippet}
                    </RowLink>
                    </div>
                  </li>
                {/each}
              </ul>
            </section>
          {/if}

          {#if explorer.favourites.length > 0}
            <section class="explorer-section" data-testid="section-favourites">
              <h2 class="section-title">
                <Icon name="act.favourite-on" size={16} />
                {$t('project.explorer.favourites')}
                <span class="section-hint text-xs text-muted"
                  >{$t('project.explorer.reorder_hint')}</span
                >
              </h2>
              <ul class="project-rows" data-testid="favourite-rows">
                {#each explorer.favourites as project, index (project.project_id)}
                  <li data-favourite-index={index}>
                    {@render projectRow(project, index, true)}
                  </li>
                {/each}
              </ul>
            </section>
          {/if}

          {#if explorer.recent.length > 0}
            <section class="explorer-section" data-testid="section-recent">
              <h2 class="section-title">{$t('project.explorer.recent')}</h2>
              <ul class="project-rows">
                {#each explorer.recent as project (project.project_id)}
                  <li>{@render projectRow(project, -1, false)}</li>
                {/each}
              </ul>
            </section>
          {/if}

          {#if explorer.all_projects.length > 0}
            <section class="explorer-section" data-testid="section-all-projects">
              <h2 class="section-title">
                {$t('project.explorer.all')}
                ({sortBy === 'name'
                  ? $t('project.explorer.by_name')
                  : $t('project.explorer.most_used')})
              </h2>
              <ul class="project-rows">
                {#each sortedAllProjects as project (project.project_id)}
                  <li>{@render projectRow(project, -1, false)}</li>
                {/each}
              </ul>
            </section>
          {/if}
        {/if}
      {/if}
    </section>

    {#if requestedProjectId && !selectedProject}
      <section class="project-page" data-testid="project-page-pending">
        {#if isLoading && !explorer}
          <Skeleton shape="card" rows={3} label={$t('project.page.loading')} />
        {:else if errorMessage !== null}
          <div class="alert alert--danger">
            <ErrorState
              code={errorMessage}
              actionLabel={$t('common.action.retry')}
              onAct={loadExplorer}
              titleLevel={2}
            />
          </div>
        {:else}
          <p class="text-muted" data-testid="project-page-not-found">
            {$t('project.page.not_found', { project: requestedProjectId })}
          </p>
        {/if}
      </section>
    {:else if selectedProject}
      <section class="project-page" aria-labelledby="project-page-title" data-testid="project-page">
        <header class="project-page__head">
          <div class="project-page__back">
            <Button
              variant="ghost"
              size="sm"
              on:click={() => navigateTo('projects')}
              data-testid="project-page-back"
            >
              ← {$t('project.page.back')}
            </Button>
          </div>
          <div class="project-page__identity">
            <KindMark kind="project" />
            <div class="project-page__names">
              <h1 class="project-page__title" id="project-page-title">
                {selectedProject.display_name}
              </h1>
              {#if selectedProject.display_name !== selectedProject.project_id}
                <span class="mono project-id">{selectedProject.project_id}</span>
              {/if}
            </div>
            <StatusLabel
              tone={readinessTone(selectedProject)}
              label={readinessWord(selectedProject)}
            />
            {#if selectedProject.needs_operator_count > 0}
              <ReasonBadge reason="setup" count={selectedProject.needs_operator_count} />
            {/if}
            <Toggle
              pressed={selectedProject.is_favourite}
              label={selectedProject.is_favourite
                ? $t('project.action.unfavourite')
                : $t('project.action.favourite')}
              icon="act.favourite"
              pressedIcon="act.favourite-on"
              onToggle={() => handleToggleFavourite(selectedProject)}
              data-testid="project-page-favourite"
            />
          </div>
          <div class="project-page__acts" data-testid="project-page-acts">
            <ActionBar
              acts={pageActs(selectedProject)}
              onAct={(id) => runProjectAct(selectedProject, id)}
            />
          </div>
        </header>

        <Tabs
          tabs={[
            { id: 'work', label: $t('project.page.work') },
            { id: 'setup', label: $t('project.page.setup') },
          ]}
          active={activeTab}
          label={$t('project.page.work')}
          onSelect={selectTab}
        />

        {#if activeTab === 'work'}
          <section class="project-page__panel" data-testid="project-page-work">
            {#if isLoadingDetail}
              <Skeleton shape="card" rows={3} label={$t('project.page.loading')} />
            {:else if detailError !== null}
              <div class="alert alert--danger">
                <ErrorState
                  code={detailError}
                  actionLabel={$t('common.action.retry')}
                  onAct={() => requestedProjectId && loadProjectPage(requestedProjectId)}
                  titleLevel={2}
                />
              </div>
            {:else if detail}
              <h2 class="section-title">{$t('project.action.running_work')}</h2>
              {#if detail.epics.length === 0 && detail.plans.length === 0}
                <p class="text-muted" data-testid="project-page-no-work">
                  {$t('project.page.no_work')}
                </p>
              {:else}
                <ul class="project-rows">
                  {#each detail.epics as epic (epic.epic_id)}
                    <li>
                      <RowLink href={`/epics/${epic.epic_id}`} title={epic.title}>
                        {#snippet kind()}<KindMark kind="epic" wordHidden />{/snippet}
                        {#snippet tags()}
                          {#if (epic.needs_operator_count ?? 0) > 0}
                            <ReasonBadge reason="decision" count={epic.needs_operator_count ?? 1} />
                          {/if}
                          <span class="text-xs text-secondary"
                            >{$t('project.explorer.counts', {
                              plans: epic.running_plans_count,
                              epics: 0,
                            })}</span
                          >
                        {/snippet}
                      </RowLink>
                    </li>
                  {/each}
                  {#each detail.plans as plan (plan.plan_id)}
                    <li>
                      <RowLink href={`/plans/${plan.plan_id}`} title={plan.title}>
                        {#snippet kind()}<KindMark kind="plan" wordHidden />{/snippet}
                        {#snippet tags()}
                          <StatusLabel
                            tone="running"
                            label={displayState('plan', plan.phase_state, $t)}
                          />
                        {/snippet}
                      </RowLink>
                    </li>
                  {/each}
                </ul>
              {/if}

              <div class="project-page__ended-toggle">
                <Button
                  variant="secondary"
                  size="sm"
                  on:click={() => {
                    showEnded = !showEnded;
                    if (showEnded && requestedProjectId && endedWork === null) {
                      void loadEndedWork(requestedProjectId);
                    }
                  }}
                  data-testid="project-ended-toggle"
                >
                  {$t('project.action.ended_work')}
                </Button>
              </div>

              {#if showEnded}
                {#if isLoadingEnded}
                  <Skeleton shape="card" rows={2} label={$t('project.page.loading')} />
                {:else if endedError !== null}
                  <div class="alert alert--danger">
                    <ErrorState
                      code={endedError}
                      actionLabel={$t('common.action.retry')}
                      onAct={() => requestedProjectId && loadEndedWork(requestedProjectId)}
                      titleLevel={2}
                    />
                  </div>
                {:else if endedWork}
                  {#if endedWork.items.length === 0}
                    <p class="text-muted" data-testid="project-page-no-ended">
                      {$t('project.page.no_ended')}
                    </p>
                  {:else}
                    <ul class="project-rows" data-testid="project-ended-rows">
                      {#each endedWork.items as item (item.id ?? item.item_id)}
                        <li>
                          <button
                            type="button"
                            class="ended-row"
                            onclick={() => openEndedItem(item)}
                            data-testid="project-ended-{item.id ?? item.item_id}"
                          >
                            <span class="ended-row__title">{item.title}</span>
                            <span class="text-xs text-muted">{formatRelative(item.ended_at)}</span>
                          </button>
                        </li>
                      {/each}
                    </ul>
                    {#if endedWork.has_older}
                      <div class="project-page__ended-toggle">
                        <Button
                          variant="secondary"
                          size="sm"
                          on:click={() =>
                            requestedProjectId && loadEndedWork(requestedProjectId, endedPage + 1)}
                          data-testid="project-ended-older"
                        >
                          {$t('project.page.ended_older')}
                        </Button>
                      </div>
                    {/if}
                  {/if}
                {/if}
              {/if}
            {/if}
          </section>
        {:else}
          <section class="project-page__panel" data-testid="project-page-setup">
            {#if settingsLoading}
              <Skeleton shape="card" rows={3} label={$t('project.settings.loading')} />
            {:else if settingsLoadError !== null}
              <div class="alert alert--danger">
                <ErrorState
                  code={settingsLoadError}
                  actionLabel={$t('common.action.retry')}
                  onAct={() => requestedProjectId && loadSetup(requestedProjectId)}
                  titleLevel={2}
                />
              </div>
            {:else if settings}
              {#if settingsError}
                <p class="error-text" role="alert" data-testid="setup-update-error">
                  {settingsError}
                </p>
              {/if}
              {#if settingsNotice}
                <p class="notice-text" data-testid="setup-notice">{settingsNotice}</p>
              {/if}
              {#if settingsRefreshError}
                <div class="alert alert--danger" role="alert" data-testid="setup-refresh-error">
                  <span>{settingsRefreshError}</span>
                  <Button
                    variant="secondary"
                    size="sm"
                    on:click={() => requestedProjectId && loadSetup(requestedProjectId)}
                    data-testid="setup-refresh-retry"
                  >
                    {$t('common.action.retry')}
                  </Button>
                </div>
              {/if}

              {#if settingsConflict}
                <div class="alert alert--warning" role="alert" data-testid="project-conflict">
                  <p class="conflict-line">
                    {$t('common.conflict.changed', { value: settingsConflict.currentValue })}
                  </p>
                  <p class="conflict-line">
                    {$t('common.conflict.yours')}: {settingsConflict.wantedValue}
                  </p>
                  <div class="conflict-actions">
                    <Button
                      variant="ghost"
                      on:click={keepCurrentValue}
                      data-testid="project-conflict-keep"
                    >
                      {$t('common.conflict.keep')}
                    </Button>
                    <Button
                      variant="primary"
                      on:click={applyConflictAgain}
                      data-testid="project-conflict-apply"
                    >
                      {$t('common.conflict.apply_again')}
                    </Button>
                  </div>
                </div>
              {/if}

              <h2 class="section-title">{$t('project.settings.values')}</h2>
              {#if settings.values.length === 0}
                <p class="text-muted">{$t('project.settings.empty')}</p>
              {:else}
                <ul class="setting-list">
                  {#each settings.values as value (value.aspect)}
                    {@const choices = value.choices}
                    <li class="setting-row" data-aspect={value.aspect}>
                      <label class="setting-row__label" for="setting-{value.aspect}">
                        {value.aspect}
                        <span class="setting-row__origin">
                          {$t('project.settings.origin', { origin: value.origin })}
                        </span>
                      </label>
                      {#if choices && choices.length > 0}
                        <select
                          id="setting-{value.aspect}"
                          class="field-input"
                          value={settingsDrafts[value.aspect] ?? ''}
                          onchange={(event) =>
                            (settingsDrafts = {
                              ...settingsDrafts,
                              [value.aspect]: (event.currentTarget as HTMLSelectElement).value,
                            })}
                          data-testid="setting-input-{value.aspect}"
                        >
                          {#each choices as option}
                            <option value={option}>{option}</option>
                          {/each}
                        </select>
                      {:else}
                        <input
                          id="setting-{value.aspect}"
                          type="text"
                          class="field-input"
                          value={settingsDrafts[value.aspect] ?? ''}
                          oninput={(event) =>
                            (settingsDrafts = {
                              ...settingsDrafts,
                              [value.aspect]: (event.currentTarget as HTMLInputElement).value,
                            })}
                          data-testid="setting-input-{value.aspect}"
                        />
                      {/if}
                      <Button
                        variant="secondary"
                        loading={savingAspects.includes(value.aspect)}
                        on:click={() => saveSetting(value)}
                        data-testid="save-setting-{value.aspect}"
                      >
                        {$t('project.settings.save')}
                      </Button>
                    </li>
                  {/each}
                </ul>
              {/if}
            {/if}

            <h2 class="section-title">{$t('project.onboarding.steps')}</h2>
            {#if isLoadingOnboarding}
              <Skeleton shape="card" rows={2} label={$t('project.onboarding.loading')} />
            {:else if onboardingError !== null}
              <div class="alert alert--danger">
                <ErrorState
                  code={onboardingError}
                  actionLabel={$t('common.action.retry')}
                  onAct={() => requestedProjectId && loadSetup(requestedProjectId)}
                  titleLevel={2}
                />
              </div>
            {:else if onboarding?.steps && onboarding.steps.length > 0}
              <ul class="setup-steps" data-testid="project-setup-steps">
                {#each onboarding.steps as step (step.step)}
                  <li class="setup-step" data-step={step.step}>
                    <span class="setup-step__number">{step.step}</span>
                    {#if step.summary}
                      <span class="setup-step__name">{step.summary}</span>
                    {/if}
                    <span class="setup-step__status text-xs text-muted">{stepStatus(step)}</span>
                  </li>
                {/each}
              </ul>
            {/if}

            <h2 class="section-title">{$t('project.page.oversize')}</h2>
            {#if isLoadingOverSize}
              <Skeleton shape="card" rows={2} label={$t('project.page.loading')} />
            {:else if overSizeError !== null}
              <div class="alert alert--danger">
                <ErrorState
                  code={overSizeError}
                  actionLabel={$t('common.action.retry')}
                  onAct={() => requestedProjectId && loadOverSize(requestedProjectId)}
                  titleLevel={2}
                />
              </div>
            {:else if overSizeRows(overSize).length > 0}
              <ul class="oversize-rows" data-testid="project-oversize">
                {#each overSizeRows(overSize) as [role, share] (role)}
                  <li class="oversize-row">
                    <span class="oversize-row__role">{role}</span>
                    <span class="oversize-row__share mono">{share}</span>
                  </li>
                {/each}
              </ul>
            {/if}

            <h2 class="section-title">{$t('project.page.end_of_setup')}</h2>
            <div class="project-page__destructive">
              {#if !selectedProject.is_released}
                <Button
                  variant="secondary"
                  on:click={() => {
                    releaseTarget = selectedProject;
                    releaseReason = '';
                    releaseError = null;
                  }}
                  data-testid="setup-release-btn"
                >
                  {$t('project.release.offer_release')}
                </Button>
              {/if}
              <Button
                variant="secondary"
                on:click={() => {
                  unenrolTarget = selectedProject;
                  unenrolError = null;
                }}
                data-testid="setup-unenrol-btn"
              >
                {$t('project.release.offer_unenrol')}
              </Button>
            </div>
          </section>
        {/if}
      </section>
    {/if}
  </div>
</div>

{#if releaseTarget}
  <ConfirmDialog
    open={true}
    title={$t('project.release.title', {
      project: releaseTarget.display_name,
      action: 'release',
    })}
    consequences={[
      $t('project.release.ends', {
        plans: releaseTarget.active_plans_count,
        epics: releaseTarget.active_epics_count,
      }),
      $t('project.release.stays'),
    ]}
    actionLabel={$t('project.release.action_release')}
    cancelLabel={$t('project.release.keep')}
    busy={isReleasing}
    confirmDisabled={releaseReason.trim().length === 0}
    testid="release-modal"
    onConfirm={handleConfirmRelease}
    onClose={() => {
      releaseTarget = null;
    }}
  >
    <TextArea
      label={$t('project.release.reason')}
      placeholder={$t('project.release.reason_placeholder')}
      value={releaseReason}
      testid="release-reason-input"
      oninput={(event) => (releaseReason = (event.currentTarget as HTMLTextAreaElement).value)}
    />
    {#if releaseError}
      <p class="error-text" role="alert" data-testid="release-error-banner">{releaseError}</p>
    {/if}
  </ConfirmDialog>
{/if}

{#if unenrolTarget}
  <ConfirmDialog
    open={true}
    title={$t('project.release.unenrol_title', { project: unenrolTarget.display_name })}
    consequences={[$t('project.release.unenrol_explain')]}
    actionLabel={$t('project.release.action_unenrol')}
    cancelLabel={$t('project.release.keep')}
    busy={isUnenrolling}
    testid="unenrol-modal"
    onConfirm={handleConfirmUnenrol}
    onClose={() => {
      unenrolTarget = null;
    }}
  >
    {#if unenrolError}
      <p class="error-text" role="alert" data-testid="unenrol-error-banner">{unenrolError}</p>
    {/if}
  </ConfirmDialog>
{/if}

{#if abandonTarget}
  <ConfirmDialog
    open={true}
    title={$t('project.adding.abandon_title', { path: abandonTarget.checkout_path })}
    consequences={[$t('project.adding.abandon_confirm', { path: abandonTarget.checkout_path })]}
    actionLabel={$t('project.adding.abandon')}
    cancelLabel={$t('common.action.cancel')}
    busy={isAbandoning}
    testid="abandon-modal"
    onConfirm={handleConfirmAbandon}
    onClose={() => {
      abandonTarget = null;
    }}
  >
    {#if abandonError}
      <p class="error-text" role="alert">{abandonError}</p>
    {/if}
  </ConfirmDialog>
{/if}

<style>
  .project-explorer {
    min-inline-size: 0;
    max-inline-size: 100%;
    padding: var(--space-6);
  }

  /*
   * The lens: the list is the left column, the project page the object column. Below the wide
   * breakpoint the object column takes the screen and the list waits behind the address.
   */
  .projects-lens {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-6);
    max-inline-size: 1400px;
    margin: 0 auto;
  }

  .projects-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-6);
    min-inline-size: 0;
  }

  .explorer-header {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    border-bottom: 1px solid var(--color-border);
    padding-bottom: var(--space-4);
  }

  .explorer-header__title-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-4);
  }

  .explorer-title {
    font-size: var(--text-2xl);
    font-weight: var(--weight-semibold);
    color: var(--color-text-primary);
    margin: 0;
  }

  .explorer-controls {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    flex-wrap: wrap;
    gap: var(--space-4);
  }

  .explorer-sort {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .sort-label {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .sort-select {
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
  }

  .explorer-loading {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 200px;
  }

  .explorer-section {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .section-title {
    display: flex;
    align-items: baseline;
    gap: var(--space-2);
    font-size: var(--text-lg);
    font-weight: var(--weight-semibold);
    color: var(--color-text-primary);
    margin: 0;
  }

  .section-hint {
    font-weight: var(--weight-regular);
  }

  .project-rows {
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    margin: 0;
    padding: 0;
  }

  .project-row--draggable {
    cursor: grab;
  }

  .project-counts {
    white-space: nowrap;
  }

  .project-last-active {
    white-space: nowrap;
  }

  .project-id {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .project-remote {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .being-added-step {
    color: var(--color-text-secondary);
  }

  .being-added-row__inner {
    display: block;
  }

  /* The project page: the object column of the lens. */
  .project-page {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    min-inline-size: 0;
    padding: var(--space-4);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-surface);
  }

  .project-page__head {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    border-bottom: 1px solid var(--color-border);
    padding-bottom: var(--space-3);
  }

  /* The back step belongs to the phone, where the page takes the screen and the list waits. */
  .project-page__back {
    display: none;
  }

  .project-page__identity {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .project-page__names {
    display: flex;
    flex-direction: column;
    min-inline-size: 0;
  }

  .project-page__title {
    font-size: var(--text-xl);
    font-weight: var(--weight-semibold);
    color: var(--color-text-primary);
    margin: 0;
  }

  .project-page__acts {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .project-page__panel {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .project-page__ended-toggle {
    display: flex;
    gap: var(--space-2);
  }

  .project-page__destructive {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .ended-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: var(--space-2);
    inline-size: 100%;
    padding: var(--space-1) var(--space-2);
    border: 1px solid transparent;
    border-radius: var(--radius-sm);
    background: none;
    color: var(--color-text-primary);
    text-align: start;
    cursor: pointer;
  }

  .ended-row:hover {
    background-color: var(--color-bg-subtle);
  }

  .field-input {
    width: 100%;
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
  }

  .setting-list {
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    margin: 0;
    padding: 0;
  }

  .setting-row {
    display: grid;
    grid-template-columns: 1fr auto;
    align-items: center;
    gap: var(--space-2);
  }

  .setting-row__label {
    grid-column: 1 / -1;
    font-size: var(--text-sm);
    font-weight: var(--weight-semibold);
    color: var(--color-text-secondary);
  }

  .setting-row__origin {
    margin-inline-start: var(--space-2);
    font-weight: var(--weight-regular);
    color: var(--color-text-muted);
  }

  .setup-steps {
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    margin: 0;
    padding: 0;
  }

  .setup-step {
    display: flex;
    align-items: baseline;
    gap: var(--space-2);
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
  }

  .setup-step__number {
    font-weight: var(--weight-semibold);
    color: var(--color-text-secondary);
  }

  .oversize-rows {
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    margin: 0;
    padding: 0;
  }

  .oversize-row {
    display: flex;
    justify-content: space-between;
    gap: var(--space-3);
    padding: var(--space-1) var(--space-2);
    border-bottom: 1px solid var(--color-border-subtle);
  }

  .alert {
    padding: var(--space-3) var(--space-4);
    border-radius: var(--radius-md);
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: var(--space-3);
  }

  .alert--danger {
    background-color: var(--color-danger-subtle);
    color: var(--color-danger);
    border: 1px solid var(--color-danger-border);
  }

  .alert--warning {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning);
    border: 1px solid var(--color-warning-border);
    flex-direction: column;
    align-items: stretch;
  }

  .conflict-line {
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .conflict-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: var(--space-2);
    margin-top: var(--space-2);
  }

  .notice-text {
    font-size: var(--text-sm);
    color: var(--color-primary);
  }

  .error-text {
    font-size: var(--text-sm);
    color: var(--color-danger);
  }

  .empty-state {
    text-align: center;
    padding: var(--space-10);
    background-color: var(--color-bg-surface);
    border: 1px dashed var(--color-border);
    border-radius: var(--radius-lg);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-4);
  }

  .empty-state__text {
    font-size: var(--text-base);
    color: var(--color-text-secondary);
    margin: 0;
  }

  @media (min-width: 1024px) {
    .projects-lens--object-open {
      grid-template-columns: minmax(22rem, 1fr) minmax(0, 1.6fr);
      align-items: start;
    }

    .project-page {
      position: sticky;
      inset-block-start: var(--space-4);
      max-block-size: calc(100vh - var(--space-8));
      overflow-y: auto;
    }
  }

  /* The phone: an opened project takes the screen, and its head carries the way back to the list. */
  @media (max-width: 1023px) {
    .projects-lens--object-open .projects-list {
      display: none;
    }

    .project-page__back {
      display: block;
    }
  }

  @media (max-width: 767px) {
    .project-explorer {
      padding: var(--space-3);
    }

    .explorer-header__title-row,
    .explorer-controls {
      flex-direction: column;
      align-items: stretch;
    }

    .setting-row {
      grid-template-columns: 1fr;
      align-items: stretch;
    }
  }
</style>
