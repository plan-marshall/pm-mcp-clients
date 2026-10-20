<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import { runtimeStore, navigateTo } from '../stores/runtime';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import {
    apiClient,
    type ProjectExplorer,
    type EnrolledProject,
    type BeingAddedProject,
  } from '../api/client';

  let explorer = $state<ProjectExplorer | null>(null);
  let isLoading = $state(true);
  let errorMessage = $state<string | null>(null);

  let filter = $state('');
  let sortBy = $state<'name' | 'most_used'>('name');

  // Modals state
  let showAddModal = $state(false);
  let newProjectPath = $state('');
  let isSubmittingAdd = $state(false);
  let addError = $state<string | null>(null);

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

  let unsubscribeEvents: (() => void) | null = null;

  async function loadExplorer() {
    isLoading = true;
    errorMessage = null;
    try {
      explorer = await apiClient.getProjectExplorer();
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('project.explorer.load_failed');
    } finally {
      isLoading = false;
    }
  }

  onMount(() => {
    void loadExplorer();

    unsubscribeEvents = apiClient.subscribeEvents((event) => {
      if (
        event === 'project.changed' ||
        event === 'project.released' ||
        event === 'project.enrolled'
      ) {
        void loadExplorer();
      }
    });
  });

  onDestroy(() => {
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  // Watch for SSE project updates via runtimeStore
  $effect(() => {
    if ($runtimeStore.projectEventNotice) {
      void loadExplorer();
    }
  });

  async function handleToggleFavourite(project: EnrolledProject) {
    try {
      await apiClient.toggleFavourite(project.project_id, !project.is_favourite);
      await loadExplorer();
    } catch (err: unknown) {
      errorMessage = (err as Error)?.message || $t('project.explorer.favourite_failed');
    }
  }

  async function handleStartOnboarding() {
    if (!newProjectPath.trim()) {
      addError = $t('project.adding.path_required');
      return;
    }
    isSubmittingAdd = true;
    addError = null;
    try {
      await apiClient.startProjectOnboarding(newProjectPath.trim());
      showAddModal = false;
      newProjectPath = '';
      await loadExplorer();
    } catch (err: unknown) {
      addError = (err as Error)?.message || $t('project.adding.start_failed');
    } finally {
      isSubmittingAdd = false;
    }
  }

  /**
   * The id of the entry a continuation link names.
   *
   * A being-added project continues as a start entry (`/api/v1/entries/{id}/continue`), so the
   * web app addresses it by its canonical entry route rather than the generic onboarding screen.
   *
   * @param link the entry's `continue_link`
   * @returns the entry id, or `null` when the link names none
   */
  function entryIdOf(link?: string | null): string | null {
    const match = /\/entries\/([^/]+)\//.exec(link ?? '');
    if (!match?.[1]) return null;
    try {
      return decodeURIComponent(match[1]);
    } catch {
      return null;
    }
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
      releaseError = (err as Error)?.message || $t('project.release.failed');
    } finally {
      isReleasing = false;
    }
  }

  async function handleConfirmUnenrol() {
    if (!unenrolTarget) return;
    isUnenrolling = true;
    unenrolError = null;
    try {
      await apiClient.unenrolProject(unenrolTarget.project_id);
      unenrolTarget = null;
      await loadExplorer();
    } catch (err: unknown) {
      const msg = (err as Error)?.message || '';
      if (msg.includes('scope_active') || msg.includes('409')) {
        unenrolError =
          $t('project.release.refused', { project: unenrolTarget.display_name }) +
          '. ' +
          $t('project.release.refused_hint');
      } else {
        unenrolError = msg || $t('project.release.unenrol_failed');
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
      abandonError = (err as Error)?.message || $t('project.adding.abandon_failed');
    } finally {
      isAbandoning = false;
    }
  }

  function formatRelative(timeStr?: string | null): string {
    if (!timeStr) return '';
    try {
      const diffMs = Date.now() - new Date(timeStr).getTime();
      const mins = Math.floor(diffMs / 60000);
      if (mins < 1) return $t('project.time.just_now');
      if (mins < 60) return $t('project.time.minutes', { count: mins });
      const hours = Math.floor(mins / 60);
      if (hours < 24) return $t('project.time.hours', { count: hours });
      const days = Math.floor(hours / 24);
      if (days === 1) return $t('project.time.yesterday');
      if (days < 7) return $t('project.time.days', { count: days });
      const weeks = Math.floor(days / 7);
      if (weeks < 5) return $t('project.time.weeks', { count: weeks });
      const months = Math.floor(days / 30);
      return $t('project.time.months', { count: months });
    } catch {
      return timeStr;
    }
  }

  const filteredProjects = $derived.by(() => {
    if (!explorer) return [];
    const query = filter.trim().toLowerCase();
    if (!query) return [];
    return explorer.all_projects.filter(
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
</script>

<div class="project-explorer" data-testid="project-explorer-view">
  <!-- Explorer Header -->
  <header class="explorer-header">
    <div class="explorer-header__title-row">
      <h1 class="explorer-title">{$t('project.explorer.title')}</h1>
      <Button
        variant="primary"
        onclick={() => {
          showAddModal = true;
          addError = null;
        }}
        data-testid="add-project-btn"
      >
        + {$t('project.explorer.add')}
      </Button>
    </div>

    <!-- Filter & Sort Bar -->
    <div class="explorer-controls">
      <div class="explorer-search">
        <input
          type="search"
          class="search-input"
          placeholder={$t('project.explorer.filter')}
          bind:value={filter}
          data-testid="project-filter-input"
          aria-label={$t('project.explorer.filter')}
        />
        {#if filter}
          <button
            class="search-clear-btn"
            aria-label={$t('project.explorer.clear_filter')}
            onclick={() => {
              filter = '';
            }}
          >
            ✕
          </button>
        {/if}
      </div>

      <div class="explorer-sort">
        <label for="sort-select" class="sort-label">{$t('project.explorer.sort_label')}</label>
        <select
          id="sort-select"
          class="sort-select"
          bind:value={sortBy}
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
      <Spinner size="2.5rem" label={$t('project.explorer.loading')} />
    </div>
  {:else if errorMessage}
    <div class="alert alert--danger" role="alert">
      <span>{errorMessage}</span>
      <Button variant="secondary" onclick={loadExplorer}>{$t('web.common.retry')}</Button>
    </div>
  {:else if explorer}
    <!-- Empty State -->
    {#if explorer.all_projects.length === 0 && explorer.being_added.length === 0}
      <div class="empty-state" data-testid="empty-projects-state">
        <p class="empty-state__text">{$t('project.explorer.empty')}</p>
        <Button
          variant="primary"
          onclick={() => {
            showAddModal = true;
          }}
        >
          + {$t('project.explorer.add')}
        </Button>
      </div>
    {:else if filter.trim().length > 0}
      <!-- Filtered Matches Section -->
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
          <div class="project-grid">
            {#each filteredProjects as project (project.project_id)}
              <article class="project-card" data-testid="project-card-{project.project_id}">
                <div class="project-card__header">
                  <button
                    class="fav-toggle-btn"
                    class:fav-toggle-btn--active={project.is_favourite}
                    aria-label={project.is_favourite
                      ? $t('project.action.unfavourite')
                      : $t('project.action.favourite')}
                    onclick={() => handleToggleFavourite(project)}
                    data-testid="star-btn-{project.project_id}"
                  >
                    {project.is_favourite ? '★' : '☆'}
                  </button>
                  <div class="project-card__identity">
                    <h3 class="project-name">{project.display_name}</h3>
                    {#if project.display_name !== project.project_id}
                      <span class="project-id mono">{project.project_id}</span>
                    {/if}
                  </div>
                </div>

                {#if project.remote_url}
                  <a
                    href={project.remote_url}
                    target="_blank"
                    rel="noreferrer"
                    class="project-remote mono text-sm"
                  >
                    {project.remote_url}
                  </a>
                {/if}

                <div class="project-card__meta">
                  <span
                    class="badge"
                    class:badge--ready={project.readiness === 'ready'}
                    class:badge--needs={project.readiness === 'setup_needs'}
                    class:badge--released={project.readiness === 'released'}
                  >
                    {#if project.readiness === 'ready'}
                      {$t('project.state.ready')}
                    {:else if project.readiness === 'released'}
                      {$t('project.state.released')}
                    {:else}
                      {$t('project.state.setup_needs', {
                        need: project.setup_needs_summary || $t('project.state.setup'),
                      })}
                    {/if}
                  </span>

                  {#if project.needs_operator_count > 0}
                    <span class="badge badge--warning">
                      ! {project.needs_operator_count}
                    </span>
                  {/if}

                  <span class="project-counts text-sm text-secondary">
                    {$t('project.explorer.counts', {
                      plans: project.active_plans_count,
                      epics: project.active_epics_count,
                    })}
                  </span>

                  {#if project.last_active}
                    <span class="project-last-active text-xs text-muted">
                      {formatRelative(project.last_active)}
                    </span>
                  {/if}
                </div>

                <div class="project-card__actions">
                  <button
                    class="btn-link"
                    onclick={() => navigateTo('overview')}
                    data-testid="running-work-btn-{project.project_id}"
                  >
                    {$t('project.action.running_work')}
                  </button>
                  {#if !project.is_released}
                    <button
                      class="btn-link text-danger"
                      onclick={() => {
                        releaseTarget = project;
                        releaseError = null;
                      }}
                      data-testid="release-btn-{project.project_id}"
                    >
                      {$t('project.action.release')}
                    </button>
                  {/if}
                  <button
                    class="btn-link text-danger"
                    onclick={() => {
                      unenrolTarget = project;
                      unenrolError = null;
                    }}
                    data-testid="unenrol-btn-{project.project_id}"
                  >
                    {$t('project.action.unenrol')}
                  </button>
                </div>
              </article>
            {/each}
          </div>
        {/if}
      </section>
    {:else}
      <!-- 1. Being added Section -->
      {#if explorer.being_added.length > 0}
        <section class="explorer-section" data-testid="section-being-added">
          <h2 class="section-title">{$t('project.adding.title')}</h2>
          <div class="project-grid">
            {#each explorer.being_added as being (being.checkout_path)}
              <div class="project-card being-added-card" data-testid="being-added-card">
                <div class="being-added-card__info">
                  <span class="mono text-sm checkout-path">{being.checkout_path}</span>
                  <span class="being-added-step text-sm">
                    {$t('project.adding.step', {
                      step: being.step_number,
                      total: being.total_steps,
                      name: being.current_step,
                    })}
                  </span>
                </div>
                <div class="being-added-card__actions">
                  <Button
                    variant="primary"
                    size="small"
                    onclick={() => {
                      const entryId = entryIdOf(being.continue_link);
                      if (entryId) {
                        navigateTo('start', { entryId });
                      } else {
                        navigateTo('project-onboarding');
                      }
                    }}
                    data-testid="continue-onboarding-btn"
                  >
                    {$t('project.adding.continue')}
                  </Button>
                  <Button
                    variant="secondary"
                    size="small"
                    onclick={() => {
                      abandonTarget = being;
                      abandonError = null;
                    }}
                    data-testid="abandon-onboarding-btn"
                  >
                    {$t('project.adding.abandon')}
                  </Button>
                </div>
              </div>
            {/each}
          </div>
        </section>
      {/if}

      <!-- 2. Favourites Section -->
      {#if explorer.favourites.length > 0}
        <section class="explorer-section" data-testid="section-favourites">
          <h2 class="section-title">★ {$t('project.explorer.favourites')}</h2>
          <div class="project-grid">
            {#each explorer.favourites as project (project.project_id)}
              <article class="project-card" data-testid="project-card-{project.project_id}">
                <div class="project-card__header">
                  <button
                    class="fav-toggle-btn fav-toggle-btn--active"
                    aria-label={$t('project.action.unfavourite')}
                    onclick={() => handleToggleFavourite(project)}
                    data-testid="star-btn-{project.project_id}"
                  >
                    ★
                  </button>
                  <div class="project-card__identity">
                    <h3 class="project-name">{project.display_name}</h3>
                    {#if project.display_name !== project.project_id}
                      <span class="project-id mono">{project.project_id}</span>
                    {/if}
                  </div>
                </div>

                {#if project.remote_url}
                  <a
                    href={project.remote_url}
                    target="_blank"
                    rel="noreferrer"
                    class="project-remote mono text-sm"
                  >
                    {project.remote_url}
                  </a>
                {/if}

                <div class="project-card__meta">
                  <span
                    class="badge"
                    class:badge--ready={project.readiness === 'ready'}
                    class:badge--needs={project.readiness === 'setup_needs'}
                    class:badge--released={project.readiness === 'released'}
                  >
                    {#if project.readiness === 'ready'}
                      {$t('project.state.ready')}
                    {:else if project.readiness === 'released'}
                      {$t('project.state.released')}
                    {:else}
                      {$t('project.state.setup_needs', {
                        need: project.setup_needs_summary || $t('project.state.setup'),
                      })}
                    {/if}
                  </span>

                  {#if project.needs_operator_count > 0}
                    <span class="badge badge--warning">
                      ! {project.needs_operator_count}
                    </span>
                  {/if}

                  <span class="project-counts text-sm text-secondary">
                    {$t('project.explorer.counts', {
                      plans: project.active_plans_count,
                      epics: project.active_epics_count,
                    })}
                  </span>

                  {#if project.last_active}
                    <span class="project-last-active text-xs text-muted">
                      {formatRelative(project.last_active)}
                    </span>
                  {/if}
                </div>

                <div class="project-card__actions">
                  <button
                    class="btn-link"
                    onclick={() => navigateTo('overview')}
                    data-testid="running-work-btn-{project.project_id}"
                  >
                    {$t('project.action.running_work')}
                  </button>
                  {#if !project.is_released}
                    <button
                      class="btn-link text-danger"
                      onclick={() => {
                        releaseTarget = project;
                        releaseError = null;
                      }}
                      data-testid="release-btn-{project.project_id}"
                    >
                      {$t('project.action.release')}
                    </button>
                  {/if}
                  <button
                    class="btn-link text-danger"
                    onclick={() => {
                      unenrolTarget = project;
                      unenrolError = null;
                    }}
                    data-testid="unenrol-btn-{project.project_id}"
                  >
                    {$t('project.action.unenrol')}
                  </button>
                </div>
              </article>
            {/each}
          </div>
        </section>
      {/if}

      <!-- 3. Recent Section -->
      {#if explorer.recent.length > 0}
        <section class="explorer-section" data-testid="section-recent">
          <h2 class="section-title">{$t('project.explorer.recent')}</h2>
          <div class="project-grid">
            {#each explorer.recent as project (project.project_id)}
              <article class="project-card" data-testid="project-card-{project.project_id}">
                <div class="project-card__header">
                  <button
                    class="fav-toggle-btn"
                    aria-label={$t('project.action.favourite')}
                    onclick={() => handleToggleFavourite(project)}
                    data-testid="star-btn-{project.project_id}"
                  >
                    ☆
                  </button>
                  <div class="project-card__identity">
                    <h3 class="project-name">{project.display_name}</h3>
                    {#if project.display_name !== project.project_id}
                      <span class="project-id mono">{project.project_id}</span>
                    {/if}
                  </div>
                </div>

                {#if project.remote_url}
                  <a
                    href={project.remote_url}
                    target="_blank"
                    rel="noreferrer"
                    class="project-remote mono text-sm"
                  >
                    {project.remote_url}
                  </a>
                {/if}

                <div class="project-card__meta">
                  <span
                    class="badge"
                    class:badge--ready={project.readiness === 'ready'}
                    class:badge--needs={project.readiness === 'setup_needs'}
                    class:badge--released={project.readiness === 'released'}
                  >
                    {#if project.readiness === 'ready'}
                      {$t('project.state.ready')}
                    {:else if project.readiness === 'released'}
                      {$t('project.state.released')}
                    {:else}
                      {$t('project.state.setup_needs', {
                        need: project.setup_needs_summary || $t('project.state.setup'),
                      })}
                    {/if}
                  </span>

                  {#if project.needs_operator_count > 0}
                    <span class="badge badge--warning">
                      ! {project.needs_operator_count}
                    </span>
                  {/if}

                  <span class="project-counts text-sm text-secondary">
                    {$t('project.explorer.counts', {
                      plans: project.active_plans_count,
                      epics: project.active_epics_count,
                    })}
                  </span>

                  {#if project.last_active}
                    <span class="project-last-active text-xs text-muted">
                      {formatRelative(project.last_active)}
                    </span>
                  {/if}
                </div>

                <div class="project-card__actions">
                  <button
                    class="btn-link"
                    onclick={() => navigateTo('overview')}
                    data-testid="running-work-btn-{project.project_id}"
                  >
                    {$t('project.action.running_work')}
                  </button>
                  {#if !project.is_released}
                    <button
                      class="btn-link text-danger"
                      onclick={() => {
                        releaseTarget = project;
                        releaseError = null;
                      }}
                      data-testid="release-btn-{project.project_id}"
                    >
                      {$t('project.action.release')}
                    </button>
                  {/if}
                  <button
                    class="btn-link text-danger"
                    onclick={() => {
                      unenrolTarget = project;
                      unenrolError = null;
                    }}
                    data-testid="unenrol-btn-{project.project_id}"
                  >
                    {$t('project.action.unenrol')}
                  </button>
                </div>
              </article>
            {/each}
          </div>
        </section>
      {/if}

      <!-- 4. All Projects Section -->
      {#if explorer.all_projects.length > 0}
        <section class="explorer-section" data-testid="section-all-projects">
          <h2 class="section-title">
            {$t('project.explorer.all')}
            ({sortBy === 'name'
              ? $t('project.explorer.by_name')
              : $t('project.explorer.most_used')})
          </h2>
          <div class="project-grid">
            {#each sortedAllProjects as project (project.project_id)}
              <article class="project-card" data-testid="project-card-{project.project_id}">
                <div class="project-card__header">
                  <button
                    class="fav-toggle-btn"
                    class:fav-toggle-btn--active={project.is_favourite}
                    aria-label={project.is_favourite
                      ? $t('project.action.unfavourite')
                      : $t('project.action.favourite')}
                    onclick={() => handleToggleFavourite(project)}
                    data-testid="star-btn-{project.project_id}"
                  >
                    {project.is_favourite ? '★' : '☆'}
                  </button>
                  <div class="project-card__identity">
                    <h3 class="project-name">{project.display_name}</h3>
                    {#if project.display_name !== project.project_id}
                      <span class="project-id mono">{project.project_id}</span>
                    {/if}
                  </div>
                </div>

                {#if project.remote_url}
                  <a
                    href={project.remote_url}
                    target="_blank"
                    rel="noreferrer"
                    class="project-remote mono text-sm"
                  >
                    {project.remote_url}
                  </a>
                {/if}

                <div class="project-card__meta">
                  <span
                    class="badge"
                    class:badge--ready={project.readiness === 'ready'}
                    class:badge--needs={project.readiness === 'setup_needs'}
                    class:badge--released={project.readiness === 'released'}
                  >
                    {#if project.readiness === 'ready'}
                      {$t('project.state.ready')}
                    {:else if project.readiness === 'released'}
                      {$t('project.state.released')}
                    {:else}
                      {$t('project.state.setup_needs', {
                        need: project.setup_needs_summary || $t('project.state.setup'),
                      })}
                    {/if}
                  </span>

                  {#if project.needs_operator_count > 0}
                    <span class="badge badge--warning">
                      ! {project.needs_operator_count}
                    </span>
                  {/if}

                  <span class="project-counts text-sm text-secondary">
                    {$t('project.explorer.counts', {
                      plans: project.active_plans_count,
                      epics: project.active_epics_count,
                    })}
                  </span>

                  {#if project.last_active}
                    <span class="project-last-active text-xs text-muted">
                      {formatRelative(project.last_active)}
                    </span>
                  {/if}
                </div>

                <div class="project-card__actions">
                  <button
                    class="btn-link"
                    onclick={() => navigateTo('overview')}
                    data-testid="running-work-btn-{project.project_id}"
                  >
                    {$t('project.action.running_work')}
                  </button>
                  {#if !project.is_released}
                    <button
                      class="btn-link text-danger"
                      onclick={() => {
                        releaseTarget = project;
                        releaseError = null;
                      }}
                      data-testid="release-btn-{project.project_id}"
                    >
                      {$t('project.action.release')}
                    </button>
                  {/if}
                  <button
                    class="btn-link text-danger"
                    onclick={() => {
                      unenrolTarget = project;
                      unenrolError = null;
                    }}
                    data-testid="unenrol-btn-{project.project_id}"
                  >
                    {$t('project.action.unenrol')}
                  </button>
                </div>
              </article>
            {/each}
          </div>
        </section>
      {/if}
    {/if}
  {/if}
</div>

<!-- 1. Add Project Modal -->
{#if showAddModal}
  <div class="modal-backdrop" role="dialog" aria-modal="true" data-testid="add-project-modal">
    <div class="modal-card">
      <h3 class="modal-title">{$t('project.explorer.add')}</h3>
      <p class="modal-explain">
        {$t('project.adding.path_hint')}
      </p>

      <div class="modal-input-group">
        <label for="new-project-path" class="text-sm">
          {$t('project.adding.path_label')}
        </label>
        <input
          id="new-project-path"
          type="text"
          class="modal-input mono"
          placeholder={$t('project.adding.path_placeholder')}
          bind:value={newProjectPath}
          data-testid="new-project-path-input"
        />
      </div>

      {#if addError}
        <div class="alert alert--danger modal-alert" role="alert">
          {addError}
        </div>
      {/if}

      <div class="modal-actions">
        <Button
          variant="secondary"
          onclick={() => {
            showAddModal = false;
          }}
          data-testid="cancel-add-project-btn"
        >
          {$t('web.common.cancel')}
        </Button>
        <Button
          variant="primary"
          loading={isSubmittingAdd}
          onclick={handleStartOnboarding}
          data-testid="confirm-add-project-btn"
        >
          {$t('project.adding.start')}
        </Button>
      </div>
    </div>
  </div>
{/if}

<!-- 2. Release Project Modal -->
{#if releaseTarget}
  <div class="modal-backdrop" role="dialog" aria-modal="true" data-testid="release-modal">
    <div class="modal-card">
      <h3 class="modal-title">
        {$t('project.release.title', {
          project: releaseTarget.display_name,
          action: 'release',
        })}
      </h3>

      <div class="modal-explain">
        <p>
          <strong>{$t('project.release.what_ends')}:</strong>
          {$t('project.release.ends', {
            plans: releaseTarget.active_plans_count,
            epics: releaseTarget.active_epics_count,
          })}.
        </p>
        <p><strong>{$t('project.release.what_stays')}:</strong> {$t('project.release.stays')}.</p>
      </div>

      <div class="modal-input-group">
        <label for="release-reason" class="text-sm">{$t('project.release.reason')}</label>
        <input
          id="release-reason"
          type="text"
          class="modal-input"
          placeholder={$t('project.release.reason_placeholder')}
          bind:value={releaseReason}
          data-testid="release-reason-input"
        />
      </div>

      {#if releaseError}
        <div
          class="alert alert--danger modal-alert"
          role="alert"
          data-testid="release-error-banner"
        >
          {releaseError}
        </div>
      {/if}

      <div class="modal-actions">
        <Button
          variant="secondary"
          onclick={() => {
            releaseTarget = null;
          }}
          data-testid="cancel-release-btn"
        >
          {$t('project.release.keep')}
        </Button>
        <Button
          variant="danger"
          loading={isReleasing}
          onclick={handleConfirmRelease}
          data-testid="confirm-release-btn"
        >
          {$t('project.release.action_release')}
        </Button>
      </div>
    </div>
  </div>
{/if}

<!-- 3. Unenrol Project Modal -->
{#if unenrolTarget}
  <div class="modal-backdrop" role="dialog" aria-modal="true" data-testid="unenrol-modal">
    <div class="modal-card">
      <h3 class="modal-title">
        {$t('project.release.unenrol_title', { project: unenrolTarget.display_name })}
      </h3>
      <p class="modal-explain">
        {$t('project.release.unenrol_explain')}
      </p>

      {#if unenrolError}
        <div
          class="alert alert--danger modal-alert"
          role="alert"
          data-testid="unenrol-error-banner"
        >
          {unenrolError}
        </div>
      {/if}

      <div class="modal-actions">
        <Button
          variant="secondary"
          onclick={() => {
            unenrolTarget = null;
          }}
          data-testid="cancel-unenrol-btn"
        >
          {$t('project.release.keep')}
        </Button>
        <Button
          variant="danger"
          loading={isUnenrolling}
          onclick={handleConfirmUnenrol}
          data-testid="confirm-unenrol-btn"
        >
          {$t('project.release.action_unenrol')}
        </Button>
      </div>
    </div>
  </div>
{/if}

<!-- 4. Abandon Onboarding Modal -->
{#if abandonTarget}
  <div class="modal-backdrop" role="dialog" aria-modal="true" data-testid="abandon-modal">
    <div class="modal-card">
      <h3 class="modal-title">
        {$t('project.adding.abandon_title', { path: abandonTarget.checkout_path })}
      </h3>
      <p class="modal-explain">
        {$t('project.adding.abandon_confirm', { path: abandonTarget.checkout_path })}
      </p>

      {#if abandonError}
        <div class="alert alert--danger modal-alert" role="alert">
          {abandonError}
        </div>
      {/if}

      <div class="modal-actions">
        <Button
          variant="secondary"
          onclick={() => {
            abandonTarget = null;
          }}
          data-testid="cancel-abandon-btn"
        >
          {$t('web.common.cancel')}
        </Button>
        <Button
          variant="danger"
          loading={isAbandoning}
          onclick={handleConfirmAbandon}
          data-testid="confirm-abandon-btn"
        >
          {$t('project.adding.abandon')}
        </Button>
      </div>
    </div>
  </div>
{/if}

<style>
  .project-explorer {
    max-width: 1200px;
    margin: 0 auto;
    padding: var(--space-6);
    display: flex;
    flex-direction: column;
    gap: var(--space-8);
  }

  .explorer-header {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    border-bottom: 1px solid var(--color-border);
    padding-bottom: var(--space-6);
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
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0;
  }

  .explorer-controls {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-4);
  }

  .explorer-search {
    position: relative;
    flex: 1;
    min-width: 220px;
    max-width: 400px;
  }

  .search-input {
    width: 100%;
    padding: var(--space-2) var(--space-8) var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
  }

  .search-clear-btn {
    position: absolute;
    right: var(--space-2);
    top: 50%;
    transform: translateY(-50%);
    background: none;
    border: none;
    color: var(--color-text-muted);
    cursor: pointer;
    font-size: var(--text-sm);
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
    gap: var(--space-4);
  }

  .section-title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0;
  }

  .project-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: var(--space-4);
  }

  .project-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-4);
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    box-shadow: var(--shadow-sm);
    transition:
      border-color 0.15s ease,
      box-shadow 0.15s ease;
  }

  .project-card:hover {
    border-color: var(--color-primary);
    box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
  }

  .project-card__header {
    display: flex;
    align-items: flex-start;
    gap: var(--space-2);
  }

  .fav-toggle-btn {
    background: none;
    border: none;
    font-size: var(--text-lg);
    color: var(--color-text-muted);
    cursor: pointer;
    padding: 0 var(--space-1);
    line-height: 1;
    transition:
      color 0.15s ease,
      transform 0.15s ease;
  }

  .fav-toggle-btn:hover {
    transform: scale(1.15);
  }

  .fav-toggle-btn--active {
    color: #eab308;
  }

  .project-card__identity {
    flex: 1;
    overflow: hidden;
  }

  .project-name {
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .project-id {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .project-remote {
    color: var(--color-primary);
    text-decoration: none;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .project-remote:hover {
    text-decoration: underline;
  }

  .project-card__meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .badge {
    display: inline-block;
    padding: 0.15rem 0.5rem;
    border-radius: var(--radius-sm);
    font-size: var(--text-xs);
    font-weight: 500;
  }

  .badge--ready {
    background-color: var(--color-success-subtle);
    color: var(--color-success);
    border: 1px solid var(--color-success-border);
  }

  .badge--needs {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning);
    border: 1px solid var(--color-warning-border);
  }

  .badge--released {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-muted);
    border: 1px solid var(--color-border-subtle);
  }

  .badge--warning {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning);
    border: 1px solid var(--color-warning-border);
    font-weight: 700;
  }

  .project-counts {
    font-size: var(--text-xs);
  }

  .project-last-active {
    margin-left: auto;
  }

  .project-card__actions {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: var(--space-3);
    margin-top: auto;
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-2);
  }

  .btn-link {
    background: none;
    border: none;
    color: var(--color-primary);
    cursor: pointer;
    font-size: var(--text-xs);
    padding: var(--space-1);
    text-decoration: none;
  }

  .btn-link:hover {
    text-decoration: underline;
  }

  .btn-link.text-danger {
    color: var(--color-danger);
  }

  /* Being added cards */
  .being-added-card {
    border-left: 4px solid var(--color-warning);
  }

  .being-added-card__info {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .checkout-path {
    color: var(--color-text-primary);
    font-weight: 500;
  }

  .being-added-step {
    color: var(--color-text-secondary);
  }

  .being-added-card__actions {
    display: flex;
    gap: var(--space-2);
    margin-top: var(--space-2);
  }

  /* Empty state */
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

  /* Modals */
  .modal-backdrop {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 100;
  }

  .modal-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-6);
    max-width: 500px;
    width: 90%;
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .modal-title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0;
  }

  .modal-explain {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    line-height: 1.5;
    margin: 0;
  }

  .modal-input-group {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .modal-input {
    width: 100%;
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
  }

  .modal-alert {
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-md);
    font-size: var(--text-sm);
  }

  .modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-3);
    margin-top: var(--space-2);
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

  /* Mobile responsiveness down to 320px */
  @media (max-width: 600px) {
    .project-explorer {
      padding: var(--space-3);
      gap: var(--space-6);
    }

    .explorer-header__title-row {
      flex-direction: column;
      align-items: stretch;
    }

    .explorer-controls {
      flex-direction: column;
      align-items: stretch;
    }

    .explorer-search {
      max-width: 100%;
    }

    .project-grid {
      grid-template-columns: 1fr;
    }

    .modal-card {
      width: 95%;
      padding: var(--space-4);
    }
  }
</style>
