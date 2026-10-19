<script lang="ts">
  import { onDestroy, onMount } from 'svelte';
  import { t } from '../i18n';
  import {
    apiClient,
    type EpicDecomposition,
    type LocalChangesPreview,
    type ProjectSummary,
    type RecipeRef,
    type StagedPlanRecord,
    type StartReadyResource,
    type StartState,
    type StartStateResource,
    type UnclaimedLessonSummary,
  } from '../api/client';
  import { navigateTo, openEpicView, openPlanView, runtimeStore } from '../stores/runtime';
  import SourceList from '../components/SourceList.svelte';
  import ConfinedFilePicker from '../components/ConfinedFilePicker.svelte';
  import GrantModal from '../components/GrantModal.svelte';
  import ChatPanel from '../components/ChatPanel.svelte';
  import MarkdownPreview from '../components/MarkdownPreview.svelte';
  import RecipeReadyView from './RecipeReadyView.svelte';
  import EpicAdoptView from './EpicAdoptView.svelte';
  import DecompositionReviewView from './DecompositionReviewView.svelte';
  import { readForgeReference } from '../start/sourceLabels';

  export let projectId: string | null = null;
  export let entryId: string | null = null;
  export let kind: 'plan' | 'epic' = 'plan';

  let state: StartState | null = null;
  let loading = true;
  let busy = false;
  let loadError = '';
  let actionError = '';

  let projects: ProjectSummary[] = [];

  let descriptionText = '';
  let showPreview = false;
  let addDescriptionOpen = false;

  let pickerOpen = false;
  let documentModeOpen = false;
  let alwaysDocumentsMode = false;

  let issueOpen = false;
  let issueReference = '';
  let issueError = '';
  let prOpen = false;
  let prReference = '';
  let prError = '';

  let grantOpen = false;
  let grantRepository = '';
  let grantReference = '';

  let localOpen = false;
  let localPreview: LocalChangesPreview | null = null;
  let localLoading = false;

  let lessonsOpen = false;
  let lessonsLoading = false;
  let lessons: UnclaimedLessonSummary[] = [];

  let chatOpen = false;

  let readyTitle = '';
  let readyId = '';
  let readyTouched = false;
  let parameters: Record<string, string> = {};

  let justCreated = false;
  let createdId = '';
  let createdTitle = '';

  let showDecomposition = false;
  let decomposition: EpicDecomposition | null = null;
  let decompositionReadBack: {
    planId: string;
    before: StagedPlanRecord | null;
    after: StagedPlanRecord;
  } | null = null;
  let decompositionCommitted = false;
  let decompositionError = '';

  let lastState = '';
  let unsubscribeEntry: (() => void) | null = null;

  $: entry = state?.entry ?? null;
  $: ready = state?.ready ?? null;
  $: chat = state?.chat ?? null;
  $: recipes = state?.recipes ?? [];
  $: currentProject = entry?.project_id ?? projectId ?? '';

  onMount(() => {
    void load();
  });

  onDestroy(() => {
    unsubscribeEntry?.();
  });

  async function load(): Promise<void> {
    loading = true;
    loadError = '';
    try {
      if (entryId) {
        apply(await apiClient.getEntry(entryId));
        subscribeEntry(entryId);
      } else if (projectId) {
        const opened = await apiClient.openEntry(projectId, { kind });
        apply(opened);
        const id = opened.start.entry?.entry_id;
        if (id) {
          runtimeStore.update((current) => ({ ...current, startEntryId: id }));
          subscribeEntry(id);
        }
      } else {
        projects = (await apiClient.getProjects()).projects ?? [];
      }
    } catch (e) {
      loadError = messageOf(e);
    } finally {
      loading = false;
    }
  }

  function subscribeEntry(id: string): void {
    unsubscribeEntry?.();
    unsubscribeEntry = apiClient.subscribeEntryEvents(id, (event) => {
      if (event === 'entry.changed' || event === 'source.changed' || event === 'chat.turn') {
        void refresh();
      }
    });
  }

  async function refresh(): Promise<void> {
    if (!entry?.entry_id) return;
    try {
      apply(await apiClient.getEntry(entry.entry_id));
    } catch {
      // A refresh that fails leaves the shown representation as it is.
    }
  }

  function apply(resource: StartStateResource): void {
    state = resource.start;
    const id = resource.start.entry?.state ?? '';
    if (id !== lastState) {
      lastState = id;
      readyTouched = false;
      if (id === 'start.ready') {
        applyReady(resource.start.ready ?? null);
      }
      if (id === 'recipe.parameters') {
        applyParameters(resource.start.entry?.recipe?.parameters ?? []);
      }
    }
    if (resource.start.entry?.state === 'start.ready' && !readyTouched) {
      applyReady(resource.start.ready ?? null);
    }
  }

  function applyReady(readyState: StartReadyResource | null): void {
    if (!readyState) return;
    readyTitle = readyState.title ?? '';
    readyId = readyState.plan_id;
    if (readyState.parameters.length > 0) {
      applyParameters(readyState.parameters);
    }
  }

  function applyParameters(
    list: { name: string; value?: string | null; options: string[] }[],
  ): void {
    const next: Record<string, string> = {};
    for (const parameter of list) {
      next[parameter.name] = parameter.value ?? parameter.options[0] ?? '';
    }
    parameters = next;
  }

  function messageOf(error: unknown): string {
    if (error instanceof Error) return error.message;
    return typeof error === 'string' ? error : '';
  }

  async function act(link: string, body: Record<string, unknown> = {}): Promise<StartState | null> {
    if (!entry?.entry_id) return null;
    busy = true;
    actionError = '';
    try {
      const resource = await apiClient.actEntry(entry.entry_id, link, body);
      apply(resource);
      return resource.start;
    } catch (e) {
      actionError = messageOf(e);
      await refresh();
      return null;
    } finally {
      busy = false;
    }
  }

  async function openProject(id: string): Promise<void> {
    projectId = id;
    runtimeStore.update((current) => ({
      ...current,
      startProjectId: id,
      startEntryId: null,
    }));
    await load();
  }

  function addSource(sourceKind: string): void {
    actionError = '';
    switch (sourceKind) {
      case 'description':
        addDescriptionOpen = true;
        break;
      case 'documents':
        pickerOpen = true;
        break;
      case 'issue':
        issueOpen = true;
        break;
      case 'pull_request':
        prOpen = true;
        break;
      case 'local_changes':
        void openLocalChanges();
        break;
      case 'lesson':
        void openLessons();
        break;
      default:
        break;
    }
  }

  async function addDescription(): Promise<void> {
    if (!descriptionText.trim()) return;
    const result = await act('add_description', { text: descriptionText.trim() });
    if (result) {
      descriptionText = '';
      addDescriptionOpen = false;
      showPreview = false;
    }
  }

  async function removeSource(sourceId: string): Promise<void> {
    await act(`remove:${sourceId}`);
  }

  async function retrySource(sourceId: string): Promise<void> {
    await act(`retry:${sourceId}`);
  }

  async function acceptSource(sourceId: string, reason: string): Promise<void> {
    await act(`accept:${sourceId}`, { reason });
  }

  async function continueToReady(): Promise<void> {
    await act('start');
  }

  async function addDocuments(paths: string[]): Promise<void> {
    pickerOpen = false;
    const result = await act('add_documents', { documents: paths });
    if (
      result?.entry?.sources.some(
        (source) => source.kind === 'documents' && source.state === 'checking',
      )
    ) {
      documentModeOpen = true;
    }
  }

  async function chooseDocumentsMode(mode: 'copy' | 'move'): Promise<void> {
    const suffix = alwaysDocumentsMode ? '_always' : '';
    documentModeOpen = false;
    await act(`documents_mode:${mode}${suffix}`);
  }

  function submitIssue(): void {
    issueError = '';
    const parsed = readForgeReference(issueReference, currentProject);
    if (!parsed) {
      issueError = $t('start.cause.unknown_forge');
      return;
    }
    if (parsed.repository !== currentProject) {
      grantRepository = parsed.repository;
      grantReference = parsed.url;
      grantOpen = true;
      return;
    }
    void addIssue(parsed.url);
  }

  async function addIssue(reference: string): Promise<void> {
    issueOpen = false;
    issueError = '';
    await act('add_issue', { reference });
  }

  function refuseGrant(): void {
    grantOpen = false;
    issueError = $t('start.cause.not_granted');
  }

  function grantIssue(): void {
    grantOpen = false;
    void addIssue(grantReference);
  }

  async function submitPullRequest(): Promise<void> {
    prError = '';
    const parsed = readForgeReference(prReference, currentProject);
    if (!parsed) {
      prError = $t('start.cause.unknown_forge');
      return;
    }
    prOpen = false;
    await act('add_pull_request', { reference: parsed.url });
  }

  async function openLocalChanges(): Promise<void> {
    localOpen = true;
    localLoading = true;
    localPreview = null;
    try {
      localPreview = await apiClient.previewLocalChanges(currentProject);
    } catch (e) {
      actionError = messageOf(e);
    } finally {
      localLoading = false;
    }
  }

  async function addLocalChanges(): Promise<void> {
    localOpen = false;
    await act('add_local_changes', { preview_id: localPreview?.preview_id });
  }

  async function openLessons(): Promise<void> {
    lessonsOpen = true;
    lessonsLoading = true;
    try {
      lessons = await apiClient.getUnclaimedLessons(currentProject);
    } catch (e) {
      actionError = messageOf(e);
    } finally {
      lessonsLoading = false;
    }
  }

  async function chooseLesson(lesson: UnclaimedLessonSummary): Promise<void> {
    lessonsOpen = false;
    await act('add_lesson', { lesson_id: lesson.lesson_id });
  }

  async function openChat(): Promise<void> {
    if (chat) {
      chatOpen = true;
      return;
    }
    const result = await act('chat');
    if (result?.chat) {
      chatOpen = true;
    }
  }

  async function sendChat(text: string): Promise<void> {
    if (!entry?.entry_id) return;
    busy = true;
    try {
      apply(await apiClient.chatTurn(entry.entry_id, text));
    } catch (e) {
      actionError = messageOf(e);
    } finally {
      busy = false;
    }
  }

  async function endChat(): Promise<void> {
    if (!entry?.entry_id) return;
    busy = true;
    try {
      apply(await apiClient.endEntryChat(entry.entry_id));
      chatOpen = false;
    } catch (e) {
      actionError = messageOf(e);
    } finally {
      busy = false;
    }
  }

  async function answerProposal(accept: boolean): Promise<void> {
    if (!entry?.entry_id) return;
    busy = true;
    try {
      apply(await apiClient.answerChatProposal(entry.entry_id, accept));
    } catch (e) {
      actionError = messageOf(e);
    } finally {
      busy = false;
    }
  }

  async function chooseRecipe(recipe: RecipeRef): Promise<void> {
    await act(`choose:${recipe.key}`);
  }

  async function continueFromParameters(): Promise<void> {
    await act('set', { parameters });
  }

  async function create(): Promise<void> {
    if (!entry?.entry_id) return;
    if (chat && chat.state !== 'end') {
      await endChat();
    }
    const body: Record<string, unknown> = { title: readyTitle };
    if (ready?.id_editable) {
      body['plan_id'] = readyId;
    }
    const result = await act('create', body);
    if (result) {
      createdId = result.ready?.plan_id ?? readyId;
      createdTitle = result.ready?.title ?? readyTitle;
      justCreated = true;
    }
  }

  async function abandon(): Promise<void> {
    if (chat && chat.state !== 'end') {
      await endChat();
    }
    await act('abandon');
    navigateTo('projects');
  }

  function follow(): void {
    if (kind === 'epic' || entry?.kind === 'epic') {
      openEpicView(createdId);
    } else {
      openPlanView(createdId);
    }
  }

  async function adoptEpic(epicId: string): Promise<void> {
    const result = await act(`adopt:${epicId}`);
    if (result?.entry) {
      createdId = result.entry.plan_id ?? epicId;
      createdTitle = result.entry.title ?? epicId;
      justCreated = true;
    }
  }

  async function loadDecomposition(): Promise<void> {
    showDecomposition = true;
    decompositionError = '';
    try {
      decomposition = await apiClient.getDecomposition(currentProject, createdId);
    } catch (e) {
      decompositionError = messageOf(e);
    }
  }

  async function instructDecomposition(planId: string | null, instruction: string): Promise<void> {
    busy = true;
    decompositionError = '';
    try {
      const before =
        decomposition?.workstreams
          .flatMap((stream) => stream.staged_plans)
          .find((plan) => plan.plan_id === planId) ?? null;
      const result = await apiClient.instructDecomposition(currentProject, createdId, {
        plan_id: planId,
        instruction,
        action: planId ? 'edit' : 'add',
      });
      decomposition = result;
      const after = result.workstreams
        .flatMap((stream) => stream.staged_plans)
        .find((plan) => plan.plan_id === planId);
      if (after) {
        decompositionReadBack = { planId: after.plan_id, before, after };
      }
    } catch (e) {
      decompositionError = messageOf(e);
    } finally {
      busy = false;
    }
  }

  function dropPlan(planId: string): void {
    if (!decomposition) return;
    decomposition = {
      ...decomposition,
      workstreams: decomposition.workstreams.map((stream) => ({
        ...stream,
        staged_plans: stream.staged_plans.filter((plan) => plan.plan_id !== planId),
      })),
    };
  }

  function discardReadBack(): void {
    decompositionReadBack = null;
    void loadDecomposition();
  }

  async function commitDecomposition(): Promise<void> {
    busy = true;
    decompositionError = '';
    try {
      await apiClient.commitDecomposition(currentProject, createdId);
      decompositionCommitted = true;
    } catch (e) {
      decompositionError = messageOf(e);
    } finally {
      busy = false;
    }
  }

  function refusalText(): string {
    if (!state?.refusal) return '';
    return state.refusal.reason === 'machine_not_onboarded'
      ? $t('start.gate.machine', { need: state.refusal.need ?? '' })
      : $t('start.gate.project', {
          project: state.refusal.project_id ?? currentProject,
          need: state.refusal.need ?? '',
        });
  }

  function openRefusalAction(): void {
    if (!state?.refusal) return;
    if (state.refusal.reason === 'machine_not_onboarded') {
      navigateTo('onboarding');
    } else {
      navigateTo('projects');
    }
  }

  function recipeUnavailable(recipe: RecipeRef): boolean {
    return recipe.parameters.some((parameter) => !parameter.available);
  }
</script>

<div class="start-view" data-testid="start-view">
  {#if loading}
    <p class="start-view__status" data-testid="start-loading">{$t('start.state.checking')}</p>
  {:else if loadError}
    <p class="start-view__error" data-testid="start-error">{loadError}</p>
  {:else if !entry && !state?.refusal}
    <section class="start-view__projects" data-testid="start-project-choice">
      <h1 class="start-view__title">{$t('project.explorer.title')}</h1>
      <ul>
        {#each projects as project (project.project_id)}
          <li>
            <button
              type="button"
              data-testid={`start-project-${project.project_id}`}
              on:click={() => openProject(project.project_id)}
            >
              {project.display_name}
            </button>
          </li>
        {/each}
      </ul>
    </section>
  {:else}
    <header class="start-view__head">
      <div>
        <p class="start-view__step" data-testid="start-step">
          {#if state?.refusal}
            {$t('start.step.sources')}
          {:else if showDecomposition || decompositionCommitted}
            {$t('start.step.decomposition')}
          {:else if justCreated}
            {$t('start.step.started')}
          {:else if entry?.state === 'start.ready' || entry?.state === 'recipe.parameters'}
            {$t('start.step.ready')}
          {:else}
            {$t('start.step.sources')}
          {/if}
        </p>
        <h1 class="start-view__title" data-testid="start-title">
          {$t(entry?.kind === 'epic' ? 'start.epic.title' : 'start.plan.title', {
            project: currentProject,
          })}
        </h1>
      </div>
    </header>

    {#if actionError}
      <p class="start-view__error" data-testid="start-action-error">
        {$t('start.error', { cause: actionError })}
      </p>
    {/if}

    {#if state?.refusal}
      <section class="start-view__panel" data-testid="start-refused">
        <p class="start-view__gate">{refusalText()}</p>
        <button type="button" data-testid="refusal-action" on:click={openRefusalAction}>
          {$t(state.refusal.action_key)}
        </button>
      </section>
    {:else if showDecomposition}
      {#if decompositionCommitted}
        <section class="start-view__panel" data-testid="decomposition-committed">
          <p>{$t('start.decomposition.committed')}</p>
          <p class="start-view__hint" data-testid="decomposition-then">
            {$t('start.decomposition.then', {
              mode: 'manual',
              plans: createdTitle,
              running: 0,
              scope: ready?.parallelization_scope ?? 1,
            })}
          </p>
        </section>
      {:else if decomposition}
        <DecompositionReviewView
          {decomposition}
          readBack={decompositionReadBack}
          {busy}
          onKeep={() => {}}
          onDrop={dropPlan}
          onInstruct={instructDecomposition}
          onAccept={() => (decompositionReadBack = null)}
          onInstructAgain={() => {
            if (decompositionReadBack) {
              void instructDecomposition(
                decompositionReadBack.planId,
                decompositionReadBack.after.objective ?? decompositionReadBack.after.title,
              );
            }
          }}
          onDiscard={discardReadBack}
          onDraftAgain={(instruction) => void instructDecomposition(null, instruction)}
          onAgree={commitDecomposition}
          onChat={() => (chatOpen = true)}
        />
        {#if decompositionError}
          <p class="start-view__error" data-testid="decomposition-error">{decompositionError}</p>
        {/if}
      {:else}
        <p class="start-view__status" data-testid="decomposition-loading">
          {$t('start.decomposition.drafting', { workstream: '' })}
        </p>
      {/if}
    {:else if justCreated}
      <section class="start-view__panel" data-testid="start-started">
        <p class="start-view__started" data-testid="started-title">
          {$t('start.started.title', { name: createdTitle || createdId })}
        </p>
        <div class="start-view__actions">
          <button type="button" data-testid="started-follow" on:click={follow}>
            {$t('start.started.follow')}
          </button>
          {#if entry?.kind === 'epic'}
            <button type="button" data-testid="started-decompose" on:click={loadDecomposition}>
              {$t('start.step.decomposition')}
            </button>
          {/if}
          <button type="button" data-testid="started-back" on:click={() => navigateTo('projects')}>
            {$t('start.started.back')}
          </button>
        </div>
      </section>
    {:else if entry?.state === 'start-epic.adopt' || entry?.state === 'start-epic.adopt-failed'}
      <EpicAdoptView
        epics={entry.adoptable_epics ?? []}
        failure={state?.adoption_failure ?? entry.adoption_failure ?? null}
        projectId={currentProject}
        {busy}
        onAdopt={(epicId) => void adoptEpic(epicId)}
        onNew={() => void act('new')}
        onRetry={() => void act('retry')}
        onBack={() => void act('back')}
        onAbandon={abandon}
      />
    {:else if entry?.state === 'start.choose'}
      <section class="start-view__panel" data-testid="start-choose">
        <h2>{$t('start.choose.title')}</h2>
        <div class="start-view__actions">
          <button type="button" data-testid="choose-sources" on:click={() => void act('sources')}>
            {$t('start.choose.sources')}
          </button>
          <button
            type="button"
            data-testid="choose-recipe"
            disabled={recipes.length === 0}
            title={recipes.length === 0
              ? $t('start.choose.no_recipe', { project: currentProject })
              : undefined}
            on:click={() => void act('recipe')}
          >
            {$t('start.choose.recipe')}
          </button>
        </div>
      </section>
    {:else if entry?.state === 'start.recipe' || entry?.state === 'recipe.choose'}
      <section class="start-view__panel" data-testid="start-recipe">
        <h2 class="start-view__subtitle">{$t('start.recipe.choose.title')}</h2>
        {#if recipes.length === 0}
          <p class="start-view__hint">
            {$t('start.choose.no_recipe', { project: currentProject })}
          </p>
        {:else}
          <ul class="start-view__recipes">
            {#each recipes as recipe (recipe.key)}
              <li class="start-view__recipe" data-testid={`recipe-${recipe.key}`}>
                <span class="start-view__recipe-name">{recipe.name}</span>
                <span class="start-view__recipe-description">{recipe.description}</span>
                <span class="start-view__recipe-origin">{recipe.origin}</span>
                {#if recipeUnavailable(recipe)}
                  <span class="start-view__error" data-testid={`recipe-unavailable-${recipe.key}`}>
                    {#each recipe.parameters as parameter}
                      {$t('start.recipe.no_option', {
                        parameter: parameter.name,
                        reason: parameter.reason ?? $t('start.recipe.no_module'),
                      })}
                    {/each}
                  </span>
                {/if}
                <button
                  type="button"
                  data-testid={`recipe-choose-${recipe.key}`}
                  disabled={busy || recipeUnavailable(recipe)}
                  on:click={() => void chooseRecipe(recipe)}
                >
                  {$t('start.choose.recipe')}
                </button>
              </li>
            {/each}
          </ul>
        {/if}
      </section>
    {:else if entry?.state === 'recipe.parameters'}
      <RecipeReadyView
        mode="parameters"
        recipe={entry.recipe ?? null}
        parameters={entry.recipe?.parameters ?? []}
        kind={entry.kind}
        {busy}
        onParameter={(name, value) => (parameters = { ...parameters, [name]: value })}
        onContinue={continueFromParameters}
      />
    {:else if entry?.state === 'start.ready' || entry?.state === 'start.recipe-create'}
      <RecipeReadyView
        mode="ready"
        recipe={ready?.recipe ?? null}
        parameters={ready?.parameters ?? []}
        sources={entry?.sources ?? []}
        planId={readyId}
        idEditable={ready?.id_editable ?? true}
        idFromTitle={ready?.id_from_title ?? false}
        title={readyTitle}
        kind={entry?.kind ?? kind}
        epicAutoStart={ready?.epic_auto_start ?? null}
        epicAutoStartOrigin={ready?.epic_auto_start_origin ?? null}
        {busy}
        onTitle={(value) => {
          readyTouched = true;
          readyTitle = value;
        }}
        onStart={create}
      />
      {#if ready?.landing && ready.landing === 'landing.files'}
        <p class="start-view__hint" data-testid="ready-landing">
          {$t('start.sources.local_changes_landing', { landing: ready.landing })}
        </p>
      {/if}
    {:else}
      <div class="start-view__sources" class:with-chat={chatOpen}>
        <div class="start-view__main">
          <h2 class="start-view__subtitle" data-testid="sources-title">
            {$t('start.sources.title', { kind: entry?.kind ?? kind })}
          </h2>

          <SourceList
            sources={entry?.sources ?? []}
            offers={entry?.kind_offers ?? []}
            canContinue={entry?.can_continue ?? false}
            chatAvailable={entry?.chat_available ?? false}
            chatDisabledReason={entry?.chat_disabled_reason ?? ''}
            {busy}
            onAdd={addSource}
            onRemove={removeSource}
            onRetry={retrySource}
            onAccept={acceptSource}
            onChat={openChat}
            onContinue={continueToReady}
          />

          {#if addDescriptionOpen || (entry?.sources.length ?? 0) === 0}
            <section class="start-view__panel" data-testid="description-editor">
              <label class="start-view__field">
                <span>{$t('start.field.prompt')}</span>
                <textarea
                  data-testid="description-input"
                  bind:value={descriptionText}
                  disabled={busy}
                  rows="4"
                ></textarea>
              </label>
              <div class="start-view__actions">
                <button
                  type="button"
                  data-testid="description-preview"
                  on:click={() => (showPreview = !showPreview)}
                >
                  {$t('start.source.description')}
                </button>
                <button
                  type="button"
                  data-testid="description-add"
                  disabled={busy || !descriptionText.trim()}
                  on:click={addDescription}
                >
                  {$t('start.source.add', { kind: $t('start.source.description') })}
                </button>
              </div>
              {#if showPreview}
                <MarkdownPreview value={descriptionText} />
              {/if}
            </section>
          {/if}

          {#if issueOpen}
            <section class="start-view__panel" data-testid="issue-panel">
              <label class="start-view__field">
                <span>{$t('start.issue.field')}</span>
                <input type="text" data-testid="issue-input" bind:value={issueReference} />
              </label>
              {#if issueReference.trim()}
                {@const parsed = readForgeReference(issueReference, currentProject)}
                {#if parsed}
                  <p class="start-view__hint" data-testid="issue-forge">
                    {parsed.forge} · {parsed.repository}
                  </p>
                {/if}
              {/if}
              {#if issueError}
                <p class="start-view__error" data-testid="issue-error">{issueError}</p>
              {/if}
              <div class="start-view__actions">
                <button
                  type="button"
                  data-testid="issue-submit"
                  disabled={busy}
                  on:click={submitIssue}
                >
                  {$t('start.source.add', { kind: $t('start.source.issue') })}
                </button>
              </div>
            </section>
          {/if}

          {#if prOpen}
            <section class="start-view__panel" data-testid="pr-panel">
              <label class="start-view__field">
                <span>{$t('start.pr.field')}</span>
                <input type="text" data-testid="pr-input" bind:value={prReference} />
              </label>
              {#if prError}
                <p class="start-view__error" data-testid="pr-error">{prError}</p>
              {/if}
              <div class="start-view__actions">
                <button
                  type="button"
                  data-testid="pr-submit"
                  disabled={busy}
                  on:click={submitPullRequest}
                >
                  {$t('start.source.add', { kind: $t('start.source.pull_request') })}
                </button>
              </div>
            </section>
          {/if}

          {#if documentModeOpen}
            <section class="start-view__panel" data-testid="documents-mode">
              <h3>{$t('start.documents.mode.title')}</h3>
              <label class="start-view__radio">
                <input
                  type="radio"
                  name="documents-mode"
                  checked
                  on:click={() => chooseDocumentsMode('copy')}
                />
                <span>{$t('start.documents.mode.copy')}</span>
                <em>{$t('start.documents.mode.preselected')}</em>
              </label>
              <label class="start-view__radio">
                <input
                  type="radio"
                  name="documents-mode"
                  on:click={() => chooseDocumentsMode('move')}
                />
                <span>{$t('start.documents.mode.move')}</span>
              </label>
              <label class="start-view__checkbox">
                <input
                  type="checkbox"
                  data-testid="documents-always"
                  bind:checked={alwaysDocumentsMode}
                />
                <span>{$t('start.documents.mode.always')}</span>
              </label>
            </section>
          {/if}

          {#if localOpen}
            <section class="start-view__panel" data-testid="local-changes-panel">
              {#if localLoading}
                <p class="start-view__status">{$t('start.state.checking')}</p>
              {:else if localPreview}
                <p class="start-view__hint" data-testid="local-changes-summary">
                  {localPreview.changed_files.length} · {localPreview.branch}
                </p>
                <ul class="start-view__files">
                  {#each localPreview.changed_files as file}
                    <li>{file.path} · +{file.additions} −{file.deletions}</li>
                  {/each}
                </ul>
                {#if !localPreview.eligible}
                  <p class="start-view__error" data-testid="local-changes-ineligible">
                    {$t('start.sources.local_changes_landing', {
                      landing: localPreview.landing ?? '',
                    })}
                  </p>
                {/if}
                <div class="start-view__actions">
                  <button
                    type="button"
                    data-testid="local-changes-add"
                    disabled={busy || !localPreview.eligible}
                    on:click={addLocalChanges}
                  >
                    {$t('start.local_changes.add')}
                  </button>
                  <button
                    type="button"
                    data-testid="local-changes-close"
                    on:click={() => (localOpen = false)}
                  >
                    {$t('start.local_changes.not_now')}
                  </button>
                </div>
              {/if}
            </section>
          {/if}

          {#if lessonsOpen}
            <section class="start-view__panel" data-testid="lessons-panel">
              {#if lessonsLoading}
                <p class="start-view__status">{$t('start.state.checking')}</p>
              {:else if lessons.length === 0}
                <p class="start-view__hint">{$t('start.sources.no_lessons')}</p>
              {:else}
                <ul class="start-view__lessons">
                  {#each lessons as lesson (lesson.lesson_id)}
                    <li>
                      <span>{lesson.title}</span>
                      <button
                        type="button"
                        data-testid={`lesson-select-${lesson.lesson_id}`}
                        disabled={busy}
                        on:click={() => void chooseLesson(lesson)}
                      >
                        {$t('start.source.add', { kind: $t('start.source.lesson') })}
                      </button>
                    </li>
                  {/each}
                </ul>
              {/if}
            </section>
          {/if}

          <div class="start-view__abandon">
            <button type="button" data-testid="entry-abandon" on:click={abandon}>
              {$t('start.entry.abandon')}
            </button>
          </div>
        </div>

        <div class="start-view__chat">
          <ChatPanel
            open={chatOpen}
            role={chat?.role ?? (entry?.kind === 'epic' ? 'decompose' : 'refine')}
            subject={$t('start.chat.subject', {
              kind: entry?.kind ?? kind,
              project: currentProject,
            })}
            turns={chat?.turns ?? []}
            proposal={chat?.proposal ?? null}
            available={entry?.chat_available ?? false}
            disabledReason={entry?.chat_disabled_reason ?? 'start.chat.waits'}
            {busy}
            onSend={sendChat}
            onEnd={endChat}
            onAccept={() => void answerProposal(true)}
            onReject={() => void answerProposal(false)}
            onToggle={openChat}
          />
        </div>
      </div>
    {/if}
  {/if}
</div>

<ConfinedFilePicker
  open={pickerOpen}
  projectId={currentProject}
  onClose={() => (pickerOpen = false)}
  onAdd={addDocuments}
/>

<GrantModal
  open={grantOpen}
  repository={grantRepository}
  onGrant={grantIssue}
  onRefuse={refuseGrant}
/>

<style>
  .start-view {
    display: flex;
    flex-direction: column;
    gap: var(--space-4, 1rem);
    padding: var(--space-4, 1rem);
    max-width: 72rem;
    width: 100%;
    margin: 0 auto;
  }

  .start-view__head {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    gap: var(--space-2, 0.5rem);
  }

  .start-view__step {
    margin: 0;
    color: var(--color-text-muted, #6b7280);
    font-size: var(--text-sm, 0.875rem);
  }

  .start-view__title {
    margin: 0;
    font-size: var(--text-xl, 1.25rem);
  }

  .start-view__subtitle {
    margin: 0;
    font-size: var(--text-lg, 1.125rem);
  }

  .start-view__sources {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-4, 1rem);
  }

  .start-view__sources.with-chat {
    grid-template-columns: minmax(0, 1.6fr) minmax(18rem, 1fr);
  }

  .start-view__main,
  .start-view__chat {
    display: flex;
    flex-direction: column;
    gap: var(--space-3, 0.75rem);
    min-width: 0;
  }

  .start-view__panel {
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
    padding: var(--space-3, 0.75rem);
    border: 1px solid var(--color-border, #d1d5db);
    border-radius: var(--radius-md, 0.5rem);
    background-color: var(--color-bg-surface, #fff);
  }

  .start-view__field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
  }

  .start-view__field textarea,
  .start-view__field input,
  .start-view__panel textarea,
  .start-view__panel input[type='text'] {
    min-height: 44px;
    padding: var(--space-1, 0.25rem);
    font: inherit;
  }

  .start-view__actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2, 0.5rem);
  }

  .start-view__actions button,
  .start-view__abandon button,
  .start-view__panel button {
    min-height: 44px;
    padding: var(--space-1, 0.25rem) var(--space-3, 0.75rem);
    cursor: pointer;
  }

  .start-view__hint,
  .start-view__status {
    margin: 0;
    color: var(--color-text-muted, #6b7280);
    font-size: var(--text-sm, 0.875rem);
    overflow-wrap: anywhere;
  }

  .start-view__error {
    margin: 0;
    color: var(--color-danger, #b3261e);
    overflow-wrap: anywhere;
  }

  .start-view__recipes,
  .start-view__lessons,
  .start-view__files,
  .start-view__projects ul {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2, 0.5rem);
  }

  .start-view__recipe,
  .start-view__lessons li {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2, 0.5rem);
    padding: var(--space-2, 0.5rem);
    border: 1px solid var(--color-border, #d1d5db);
    border-radius: var(--radius-md, 0.5rem);
  }

  .start-view__recipe-name {
    font-weight: 600;
  }

  .start-view__recipe-description,
  .start-view__recipe-origin {
    color: var(--color-text-muted, #6b7280);
    font-size: var(--text-sm, 0.875rem);
  }

  .start-view__radio,
  .start-view__checkbox {
    display: flex;
    align-items: center;
    gap: var(--space-2, 0.5rem);
    min-height: 44px;
  }

  .start-view__abandon {
    display: flex;
    justify-content: flex-end;
  }

  .start-view__gate {
    margin: 0;
    font-weight: 600;
  }

  .start-view__started {
    margin: 0;
    font-weight: 700;
  }

  @media (max-width: 1024px) {
    .start-view__sources.with-chat {
      grid-template-columns: minmax(0, 1fr);
    }
  }

  @media (max-width: 640px) {
    .start-view {
      padding: var(--space-2, 0.5rem);
    }
  }
</style>
