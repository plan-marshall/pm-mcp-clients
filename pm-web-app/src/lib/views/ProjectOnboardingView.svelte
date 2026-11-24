<script lang="ts">
  import { onMount } from 'svelte';
  import { t, currentLocale } from '../i18n';
  import { formatTime } from '../display/format';
  import Button from '../components/Button.svelte';
  import Icon from '../components/Icon.svelte';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import TextField from '../components/TextField.svelte';
  import CheckField from '../components/CheckField.svelte';
  import ConfirmDialog from '../components/ConfirmDialog.svelte';
  import { actFailureText, errorCodeOf } from '../api/errors';
  import { apiClient } from '../api/client';
  import type {
    BeingAddedProject,
    ProjectBuildEnvironment,
    ProjectOnboardingState,
    ProjectOnboardingStep,
    ProjectOnboardingStepStatus,
    StartStateResource,
  } from '../api/client';
  import { navigateTo } from '../stores/runtime';

  /**
   * A project's onboarding (UC-PRJ-1): its ten steps, the build environment the terminal client
   * last captured, a value changed in another client meanwhile, and the setup's question chat.
   *
   * A run is driven by the runtime's setup state. Before its enrolment a run has no project id;
   * the explorer reports it under `being_added`, and the screen shows the step it waits at from
   * that report.
   */
  interface Props {
    projectId?: string | null;
    onboardingId?: string | null;
  }

  let { projectId = null, onboardingId = null }: Props = $props();

  /** One step of the setup, as the design's step line names it. */
  interface StepDefinition {
    step: number;
    key: string;
    state: string;
  }

  const STEPS: StepDefinition[] = [
    { step: 1, key: 'checkout', state: 'project.checkout' },
    { step: 2, key: 'identity', state: 'project.identity' },
    { step: 3, key: 'enrolment', state: 'project.enrolment' },
    { step: 4, key: 'own', state: 'project.operator' },
    { step: 5, key: 'team', state: 'setup.project-settings' },
    { step: 6, key: 'architecture', state: 'setup.detect-domains' },
    { step: 7, key: 'landing', state: 'setup.select-providers' },
    { step: 8, key: 'roles', state: 'setup.qualify-consent' },
    { step: 9, key: 'descriptions', state: 'setup.enrich-consent' },
    { step: 10, key: 'commit', state: 'setup.commit-changes' },
  ];

  /** A value the operator wanted to change while another client changed it first. */
  interface OnboardingConflict {
    currentValue: string;
    wantedValue: string;
    step: string;
    /** The body the refused step sent, so its retry replays the same fields. */
    body: Record<string, unknown>;
  }

  let isLoading = $state(true);
  let loadError = $state<string | null>(null);
  /** Whether this page has shown a run, so a later failure is an arrival. */
  let hasShownRun = $state(false);
  let run = $state<ProjectOnboardingState | null>(null);
  let beingAdded = $state<BeingAddedProject | null>(null);
  /** The checkout path or entry id of a run started from the checkout form in this mount. */
  let startedRunTarget = $state<string | null>(null);
  let checkoutDraft = $state('');
  let isStarting = $state(false);
  let isSubmitting = $state(false);
  /** Whether a "Check again" is reading the state, so its own button carries the busy state. */
  let isRefreshing = $state(false);
  let error = $state('');
  let conflict = $state<OnboardingConflict | null>(null);
  let chatOpen = $state(false);
  let chatDraft = $state('');
  let isChatting = $state(false);
  let confirmAbandon = $state(false);

  /** The identity the operator edits on step 2, and the run it was proposed from. */
  interface IdentityDraft {
    id: string;
    name: string;
    pilot: boolean;
  }

  let identityDraft = $state<IdentityDraft | null>(null);
  let identityKey = $state('');

  const currentStep = $derived(stepOf(run));
  /**
   * The project the step actions address: the one the page was opened for, or — after a reload at
   * the run's own address, which names the run and not the project — the one the loaded run reports.
   */
  const activeProjectId = $derived(projectId ?? run?.project_id ?? null);
  const isNewRun = $derived(!projectId && !run);
  const isComplete = $derived(run?.state === 'project.completed');
  const chatOffered = $derived(Boolean(activeProjectId) && run?.chat_available !== false);
  const chatReason = $derived(
    run?.chat_disabled_reason ? $t(run.chat_disabled_reason) : $t('project.chat.disabled.no_role'),
  );
  const stepRows = $derived(buildStepRows(run, currentStep));

  /**
   * The identity of the run, from the state where it publishes one and from the checkout otherwise
   * (`15-project-onboarding.adoc:65`: the id proposed from the remote's name, the display name from
   * its owner and name).
   */
  const identity = $derived(identityDraft ?? identityProposal(run));

  /**
   * Whether step 3 waits for something only the terminal can give it: the build environment of
   * the machine, which the terminal client captures (`project-onboarding.adoc#build-environment`).
   */
  const needsTerminal = $derived(
    run !== null && currentStep === 3 && !run.build_environment?.captured_at,
  );
  const canSave = $derived(Boolean(activeProjectId || run?.links?.set));

  // A run that reaches the identity step — or a new run — proposes its values again, and keeps the
  // operator's edits while the step stays the one on screen.
  $effect(() => {
    const key = `${currentStep}|${run?.onboarding_id ?? ''}|${run?.checkout_path ?? ''}`;
    if (key === identityKey) return;
    identityKey = key;
    identityDraft = identityProposal(run);
  });

  onMount(() => {
    void load();
    measureStepLine();
    window.addEventListener('resize', measureStepLine);
    return () => window.removeEventListener('resize', measureStepLine);
  });

  /** The step line's own element, measured to decide whether it can be scrolled at all. */
  let stepLine = $state<HTMLElement | null>(null);

  /** Whether the step line is wider than its box, and therefore needs the keyboard to scroll. */
  let stepLineOverflows = $state(false);

  /**
   * Takes the Tab stop only while the step line actually overflows: a focusable box with nothing to
   * scroll is one stop the keyboard cannot use (accessibility audit: scrollable-region-focusable).
   */
  function measureStepLine(): void {
    stepLineOverflows = stepLine ? stepLine.scrollWidth > stepLine.clientWidth : false;
  }

  // The line's width follows its rows and the words of the language on screen, so both are
  // measured again — a language switch that makes the labels longer must not leave the line
  // overflowing without its Tab stop.
  $effect(() => {
    void stepRows;
    void $currentLocale;
    // The labels themselves are catalogue texts: a loaded catalogue can change the line's width.
    void $t;
    measureStepLine();
  });

  /**
   * @param value the run, or `null` while none is loaded
   * @returns the identity the operator starts from
   */
  function identityProposal(value: ProjectOnboardingState | null): IdentityDraft {
    const publishedId = typeof value?.id === 'string' ? value.id : '';
    const publishedName = typeof value?.name === 'string' ? value.name : '';
    const derived = idFromCheckout(value?.checkout_path);
    return {
      id: publishedId || derived,
      name: publishedName || derived,
      pilot: value?.pilot === true,
    };
  }

  /**
   * @param checkoutPath the checkout of the run
   * @returns an id the identifier grammar accepts, proposed from the checkout's own name
   */
  function idFromCheckout(checkoutPath?: string | null): string {
    const name = (checkoutPath ?? '').split('/').filter(Boolean).pop() ?? '';
    const folded = name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
    if (!folded) return '';
    return /^[a-z]/.test(folded) ? folded : `p-${folded}`;
  }

  /**
   * Writes one field of the identity the operator edits.
   *
   * @param field the field changed
   * @param value its new value
   */
  function setIdentityField(field: keyof IdentityDraft, value: string | boolean): void {
    identityDraft = { ...identity, [field]: value } as IdentityDraft;
  }

  /** Records the identity and moves the run on. */
  async function saveIdentity(): Promise<void> {
    const payload = {
      id: identity.id,
      name: identity.name,
      pilot: identity.pilot,
    };
    if (activeProjectId) {
      await applyStep('set', payload);
      return;
    }
    const entryId = run?.onboarding_id || onboardingId;
    const setLink = run?.links?.set;
    const setHref = typeof setLink === 'string' ? setLink : setLink?.href;
    if (entryId && setHref) {
      isSubmitting = true;
      error = '';
      try {
        // The `set` link's own href is what the runtime publishes for this act; the client never
        // rebuilds it (project-onboarding.adoc#entry).
        await apiClient.actByHref(setHref, payload);
        // The reload selects the same being-added run this screen shows, by its checkout, so a run
        // started from the checkout form (which has no `onboardingId`) stays on screen.
        await load(run?.checkout_path ?? undefined);
      } catch (err: unknown) {
        error = actFailureText(err, $t('project.settings.update_failed'));
      } finally {
        isSubmitting = false;
      }
    }
  }

  /**
   * @param value the run, or `null` while none is loaded
   * @returns the step the run waits at, 1 to 10
   */
  function stepOf(value: ProjectOnboardingState | null): number {
    const step = Number(value?.step);
    if (Number.isInteger(step) && step >= 1 && step <= STEPS.length) {
      return step;
    }
    return 1;
  }

  /**
   * Turns the runtime's step list into rows, or derives them from the one step the run reports.
   *
   * @param value the run
   * @param active the step the run waits at
   * @returns one row per step
   */
  function buildStepRows(
    value: ProjectOnboardingState | null,
    active: number,
  ): Array<StepDefinition & { status: ProjectOnboardingStepStatus }> {
    const reported = new Map<number, ProjectOnboardingStep>();
    for (const step of value?.steps ?? []) {
      reported.set(step.step, step);
    }
    return STEPS.map((definition) => {
      const entry = reported.get(definition.step);
      const status = entry?.status ?? statusOf(definition.step, active);
      return { ...definition, status };
    });
  }

  /**
   * @param step the step's number
   * @param active the step the run waits at
   * @returns its status from the run's position
   */
  function statusOf(step: number, active: number): ProjectOnboardingStepStatus {
    if (step < active) return 'done';
    if (step === active) return 'current';
    return 'waiting';
  }

  /**
   * @param status a step's status
   * @returns the catalogue key of the status' label
   */
  function statusKey(status: ProjectOnboardingStepStatus): string {
    switch (status) {
      case 'done':
        return 'project.onboarding.step_done';
      case 'skipped':
        return 'project.onboarding.step_skipped';
      case 'waiting':
      case 'needs_repair':
        return 'project.onboarding.step_waiting';
      default:
        return 'project.onboarding.step_current';
    }
  }

  /**
   * @param step a step's number
   * @returns the catalogue key of its name
   */
  function stepKey(step: number): string {
    return `project.step.${STEPS[step - 1]?.key ?? 'checkout'}`;
  }

  /**
   * Reads the run: by its project id once enrolled, otherwise from the explorer's being-added
   * report. With neither, the screen asks for a checkout path and starts a new run.
   *
   * @param startedPath the checkout a run was just started at, when the screen started one
   */
  async function load(startedPath?: string): Promise<void> {
    // A check that reads the state again keeps the card on screen: only a first read shows the
    // skeleton, so the button that started the check keeps its busy state.
    if (!run) {
      isLoading = true;
    }
    loadError = null;
    error = '';
    const wasBeingAdded = beingAdded !== null;
    beingAdded = null;
    try {
      if (projectId) {
        try {
          run = await apiClient.getProjectOnboarding(projectId);
        } catch (err: unknown) {
          run = null;
          loadError = errorCodeOf(err);
        }
        if (!run) {
          return;
        }
      } else {
        await loadBeingAdded(startedPath);
        // The explorer no longer lists the run this page showed — the terminal enrolled or
        // abandoned it — so the screen stops showing a step that no longer exists.
        if (!beingAdded && wasBeingAdded) {
          run = null;
        }
      }
      hasShownRun = run !== null || beingAdded !== null;
    } catch (err: unknown) {
      loadError = errorCodeOf(err);
    } finally {
      isLoading = false;
    }
  }

  /** Reads the state again from the "Check again" of a step that waits on the terminal. */
  async function checkAgain(): Promise<void> {
    isRefreshing = true;
    try {
      await load();
    } finally {
      isRefreshing = false;
    }
  }

  /** Reads the explorer's being-added report, for the run this screen was opened at. */
  async function loadBeingAdded(startedPath?: string): Promise<void> {
    const wanted = onboardingId?.trim() || startedPath?.trim() || startedRunTarget?.trim();
    if (!wanted) {
      // No selection is a new run: the screen asks for its checkout path.
      beingAdded = null;
      return;
    }
    const explorer = await apiClient.getProjectExplorer();
    const match = explorer.being_added.find(
      (entry) => entry.checkout_path === wanted || entryOnboardingId(entry) === wanted,
    );
    beingAdded = match ?? null;
    if (match) {
      if (!onboardingId?.trim()) {
        startedRunTarget = match.checkout_path;
      }
      // The explorer's row carries six fields; the run's own link surface and its facts are the
      // entry's (the mock serves them on `GET /api/v1/entries/{id}`), so the screen reads it instead
      // of inventing `links` or `build_environment` on the row.
      const entryId = entryOnboardingId(match);
      if (entryId) {
        run = runOfBeingAdded(match, await apiClient.getEntry(entryId));
      } else {
        // A row whose link names no entry is shown from its own six facts: no link, no guess.
        run = runOfBeingAdded(match, {} as StartStateResource);
      }
      checkoutDraft = match.checkout_path;
      return;
    }
    const isEnrolled = [...explorer.all_projects, ...explorer.favourites, ...explorer.recent].some(
      (p) => p.project_id === wanted,
    );
    if (isEnrolled) {
      // A failed read reaches `load`'s catch, so it shows the error with its retry and never the
      // form that starts another run.
      run = await apiClient.getProjectOnboarding(wanted);
      return;
    }
    startedRunTarget = null;
  }

  /**
   * @param entry a being-added run
   * @returns the id the entry addresses the run by
   */
  function entryOnboardingId(entry: BeingAddedProject): string {
    // The explorer's row publishes its run through `continue_link` only; the entry's own `links`
    // are read from `GET /api/v1/entries/{id}`, never from the row.
    const linkStr = entry.continue_link ?? '';
    const match = /\/entries\/([^/]+)\//.exec(linkStr);
    if (match?.[1]) {
      try {
        return decodeURIComponent(match[1]);
      } catch {
        return match[1];
      }
    }
    return entry.checkout_path;
  }

  /**
   * @param entry a being-added run
   * @param entryState the entry the runtime serves for it
   * @returns the run, in the shape the enrolled run reports, from wire facts only
   */
  function runOfBeingAdded(
    entry: BeingAddedProject,
    entryState: StartStateResource,
  ): ProjectOnboardingState {
    const rawLinks: Record<string, any> = {
      ...((entryState.links as Record<string, any> | undefined) ?? {}),
    };
    if (entry.continue_link && !rawLinks.continue) {
      rawLinks.continue = entry.continue_link;
    }
    if (entry.abandon_link && !rawLinks.abandon) {
      rawLinks.abandon = entry.abandon_link;
    }
    const links: Record<string, any> = {};
    for (const [k, v] of Object.entries(rawLinks)) {
      if (typeof v === 'string') {
        links[k] = { href: v, method: 'POST' };
      } else if (v && typeof v === 'object') {
        links[k] = v;
      }
    }
    const step = Number(entryState.step ?? entry.step_number);
    return {
      onboarding_id: entryOnboardingId(entry),
      checkout_path: entry.checkout_path,
      step,
      total_steps: Number(entryState.total_steps ?? entry.total_steps),
      state: String(entryState.state ?? STEPS[step - 1]?.state ?? 'project.checkout'),
      summary: entry.current_step,
      chat_available: false,
      build_environment:
        (entryState.build_environment as ProjectBuildEnvironment | null | undefined) ?? null,
      // The entry publishes `pilot` or it does not: only a published boolean offers the checkbox.
      pilot: typeof entryState.pilot === 'boolean' ? entryState.pilot : undefined,
      links,
    };
  }

  /** Starts a new run at the checkout the operator named. */
  async function startRun(): Promise<void> {
    if (!checkoutDraft.trim() || isStarting) return;
    isStarting = true;
    error = '';
    try {
      const checkout = checkoutDraft.trim();
      await apiClient.startProjectOnboarding(checkout);
      startedRunTarget = checkout;
      await load(checkout);
    } catch (err: unknown) {
      error = actFailureText(err, $t('project.adding.start_failed'));
    } finally {
      isStarting = false;
    }
  }

  /**
   * Applies one step of the run, and opens the conflict card instead of overwriting a value that
   * changed in another client.
   *
   * @param step the step's id
   * @param body the step's values
   */
  async function applyStep(step: string, body: Record<string, unknown> = {}): Promise<void> {
    if (!activeProjectId || isSubmitting) return;
    isSubmitting = true;
    error = '';
    conflict = null;
    try {
      const result = await apiClient.stepProjectOnboarding(activeProjectId, step, body);
      if (result.conflict) {
        // A step that sent fields replays them; a step the link itself answered replays the value
        // the runtime named as wanted.
        const retryBody =
          Object.keys(body).length > 0 ? body : { value: result.wanted_value ?? '' };
        conflict = {
          currentValue: result.current_value ?? '',
          wantedValue: result.wanted_value ?? String(body.value ?? ''),
          step,
          body: retryBody,
        };
        return;
      }
      run = result;
    } catch (err: unknown) {
      error = actFailureText(err, $t('project.settings.update_failed'));
    } finally {
      isSubmitting = false;
    }
  }

  /** Applies the operator's own choice again, on the revision the other client left behind. */
  async function applyConflictAgain(): Promise<void> {
    if (!conflict) return;
    const { step, body } = conflict;
    conflict = null;
    await applyStep(step, body);
  }

  /** Keeps the other client's value and closes the card, without writing anything. */
  async function keepCurrentValue(): Promise<void> {
    conflict = null;
    if (activeProjectId) {
      await load();
    }
  }

  /**
   * @returns the ids of the links the runtime offers for this step
   */
  function linkIds(): string[] {
    const raw = run?.links;
    return raw && typeof raw === 'object' ? Object.keys(raw) : [];
  }

  /**
   * @param id the link's id
   * @returns its label from the catalogue, or its id where the catalogue names none
   */
  function linkLabel(id: string): string {
    const key = `onboard.link.${id}`;
    const label = $t(key);
    return label === key ? id.replaceAll('_', ' ') : label;
  }

  /** Reloads a pre-enrolment run from the explorer, which is where its step is reported. */
  async function continueRun(): Promise<void> {
    await load();
  }

  /** Abandons a pre-enrolment run, after the operator confirmed that nothing was enrolled. */
  async function abandonRun(): Promise<void> {
    if (!beingAdded) return;
    confirmAbandon = false;
    try {
      await apiClient.abandonOnboarding(beingAdded.abandon_link || beingAdded.checkout_path);
      navigateTo('projects');
    } catch (err: unknown) {
      error = actFailureText(err, $t('project.adding.abandon_failed'));
    }
  }

  /** Sends the operator's question to the setup's role. */
  async function sendChat(): Promise<void> {
    if (!chatDraft.trim() || isChatting || !activeProjectId) return;
    isChatting = true;
    const text = chatDraft.trim();
    chatDraft = '';
    try {
      await applyStep('chat', { text });
    } finally {
      isChatting = false;
    }
  }

  /**
   * @param environment the captured environment
   * @returns who captured it, in the interface language
   */
  function captureSource(environment: ProjectBuildEnvironment | null | undefined): string {
    if (environment?.source === 'web') {
      return $t('project.onboarding.build_env.source.web');
    }
    return $t('project.enrolment.build_env.login');
  }

  /**
   * @param rawDate an ISO timestamp
   * @returns the moment to the minute, in the interface language
   */
  function formatCapturedAt(rawDate?: string | null): string {
    return formatTime(rawDate, 'short', $currentLocale);
  }
</script>

<div class="project-onboarding" data-testid="project-onboarding-view">
  <header class="project-onboarding__header">
    <div>
      <h1 class="project-onboarding__title">
        {#if run?.checkout_path}
          {$t('project.onboarding.title', { project: run.checkout_path })}
        {:else}
          {$t('project.onboarding.title_new')}
        {/if}
      </h1>
      {#if run}
        <p class="project-onboarding__progress">
          {$t('project.adding.step', {
            step: currentStep,
            total: run.total_steps ?? STEPS.length,
            name: $t(stepKey(currentStep)),
          })}
        </p>
      {/if}
    </div>
  </header>

  <!-- The step line scrolls sideways only while it is wider than its box; while it is, it takes
       the Tab stop so the keyboard can scroll it (accessibility audit: scrollable-region-focusable).
       `nav` is not an interactive element, which is what the compiler's rule below objects to. -->
  <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
  <nav
    class="step-line"
    tabindex={stepLineOverflows ? 0 : -1}
    bind:this={stepLine}
    aria-label={$t('project.onboarding.steps')}
  >
    <ol class="step-line__list">
      {#each stepRows as step}
        <li
          class="step-line__item"
          class:step-line__item--current={step.status === 'current'}
          class:step-line__item--done={step.status === 'done'}
          class:step-line__item--skipped={step.status === 'skipped'}
          data-testid="project-step-{step.step}"
          aria-current={step.status === 'current' ? 'step' : undefined}
        >
          <span class="step-line__indicator" aria-hidden="true">
            {#if step.status === 'done'}<Icon name="state.check" size={16} />{:else}{step.step}{/if}
          </span>
          <span class="step-line__label">{$t(stepKey(step.step))}</span>
          <span class="step-line__status">{$t(statusKey(step.status))}</span>
        </li>
      {/each}
    </ol>
  </nav>

  <main class="project-onboarding__main">
    {#if isLoading}
      <div class="state-card" role="status">
        <Skeleton shape="card" rows={3} label={$t('project.onboarding.loading')} />
      </div>
    {:else if loadError !== null || (projectId && !run)}
      <div class="state-card" data-testid="project-onboarding-error">
        <ErrorState
          code={loadError}
          actionLabel={$t('common.action.retry')}
          onAct={() => load()}
          titleLevel={2}
          announce={hasShownRun}
        />
      </div>
    {:else if isComplete}
      <div class="state-card" data-testid="project-onboarding-complete">
        <div class="state-card__header">
          <span class="badge-success" aria-hidden="true"><Icon name="state.check" size={16} /></span
          >
          <h2 class="state-card__title">
            {$t('project.ready.title', { project: run?.checkout_path ?? '' })}
          </h2>
          {#if run?.summary}
            <p class="state-card__summary">{run.summary}</p>
          {/if}
        </div>
        <div class="state-card__actions">
          <Button variant="secondary" on:click={() => navigateTo('projects')}>
            {$t('onboard.completed.to_projects')}
          </Button>
        </div>
      </div>
    {:else if isNewRun}
      <div class="state-card" data-testid="project-checkout-step">
        <div class="state-card__header">
          <h2 class="state-card__title">{$t('project.checkout.title')}</h2>
          <p class="state-card__summary">{$t('project.adding.path_hint')}</p>
        </div>
        <label class="field" for="project-checkout-path">{$t('project.checkout.path')}</label>
        <input
          id="project-checkout-path"
          type="text"
          class="field-input mono"
          placeholder={$t('project.adding.path_placeholder')}
          bind:value={checkoutDraft}
          data-testid="project-checkout-path"
        />
        {#if error}
          <p class="error-text" role="alert">{error}</p>
        {/if}
        <div class="state-card__actions">
          <Button
            variant="primary"
            loading={isStarting}
            disabled={!checkoutDraft.trim()}
            on:click={startRun}
            data-testid="project-checkout-start"
          >
            {$t('project.onboarding.start')}
          </Button>
        </div>
      </div>
    {:else if run}
      <div class="state-card" data-testid="project-onboarding-step">
        <div class="state-card__header">
          <!-- The run's summary names the current step ("Identity"), which the heading already
               names in the operator's language; printing both repeats the step (CC-F-05). -->
          <h2 class="state-card__title">{$t(stepKey(currentStep))}</h2>
          {#if run.checkout_path}
            <p class="state-card__scope mono">{run.checkout_path}</p>
          {/if}
        </div>

        {#if currentStep === 2}
          <section class="identity" data-testid="project-identity-step">
            <h3 class="identity__title">{$t('project.identity.title')}</h3>
            <TextField
              label={$t('project.identity.id')}
              value={identity.id}
              mono
              autocomplete="off"
              spellcheck={false}
              hint={$t('project.identity.id_hint')}
              testid="project-identity-id"
              oninput={(event) =>
                setIdentityField('id', (event.currentTarget as HTMLInputElement).value)}
            />
            <TextField
              label={$t('project.identity.name')}
              value={identity.name}
              autocomplete="off"
              testid="project-identity-name"
              oninput={(event) =>
                setIdentityField('name', (event.currentTarget as HTMLInputElement).value)}
            />
            {#if typeof run?.pilot === 'boolean'}
              <CheckField
                label={$t('project.identity.pilot')}
                description={$t('project.identity.pilot_hint')}
                checked={identity.pilot}
                testid="project-identity-pilot"
                onchange={(checked) => setIdentityField('pilot', checked)}
              />
            {/if}
            <div class="identity__actions">
              <Button
                variant="primary"
                loading={isSubmitting}
                disabled={!identity.id.trim() || !identity.name.trim() || !canSave}
                disabledReason={!canSave ? $t('project.identity.enrol_in_terminal') : undefined}
                on:click={saveIdentity}
                data-testid="project-identity-save"
              >
                {$t('project.identity.save')}
              </Button>
            </div>
          </section>
        {/if}

        {#if needsTerminal}
          <!-- A step that waits on the terminal says what is missing, the command that supplies it,
               how the run goes on, and offers the check that reads the machine again before the
               operator leaves the browser (PLAN-59 D3). -->
          <section class="needs-terminal" data-testid="project-needs-terminal">
            <h3 class="needs-terminal__title">
              <Icon name="state.alert" size={16} />
              {$t('project.needs_terminal.title')}
            </h3>
            <p class="needs-terminal__line">
              {$t('project.needs_terminal.missing', {
                what: $t('project.needs_terminal.what'),
              })}
            </p>
            <p class="needs-terminal__command-label">{$t('project.needs_terminal.command')}</p>
            <code class="needs-terminal__command mono" data-testid="project-needs-terminal-command"
              >{run.build_environment?.command ?? 'pm-operator project add'}</code
            >
            <p class="needs-terminal__line">{$t('project.needs_terminal.after')}</p>
            <Button
              variant="secondary"
              loading={isRefreshing}
              on:click={checkAgain}
              data-testid="project-needs-terminal-check"
            >
              {$t('project.needs_terminal.check_again')}
            </Button>
          </section>
        {:else if currentStep <= 3}
          <section class="build-env" data-testid="project-build-environment">
            <h3 class="build-env__title">{$t('project.onboarding.build_env.title')}</h3>
            {#if run.build_environment?.captured_at}
              <p class="build-env__line">
                {$t('project.enrolment.build_env.captured', {
                  time: formatCapturedAt(run.build_environment.captured_at),
                })}
                · {captureSource(run.build_environment)}
              </p>
              {#if run.build_environment.variables}
                <details class="build-env__details">
                  <summary>{$t('project.onboarding.build_env.variables')}</summary>
                  <dl class="build-env__variables">
                    {#each Object.entries(run.build_environment.variables) as [name, value]}
                      <div class="build-env__variable">
                        <dt class="mono">{name}</dt>
                        <dd class="mono">{value}</dd>
                      </div>
                    {/each}
                  </dl>
                </details>
              {/if}
            {:else}
              <p class="build-env__line">
                {$t('project.enrolment.build_env.none', {
                  command: run.build_environment?.command ?? 'pm-operator project add',
                })}
              </p>
            {/if}
          </section>
        {/if}

        {#if run.detected_domains?.length}
          <p class="state-card__text">
            {$t('project.architecture.domains', { list: run.detected_domains.join(', ') })}
          </p>
        {/if}
        {#if run.discovered_modules?.length}
          <p class="state-card__text">
            {$t('project.architecture.modules', {
              count: run.discovered_modules.length,
              build_systems: '',
            })}
          </p>
        {/if}

        {#if conflict}
          <section class="conflict" role="alert" data-testid="project-conflict">
            <h3 class="conflict__title">
              {$t('common.conflict.changed', { value: conflict.currentValue })}
            </h3>
            <p class="conflict__line">{$t('common.conflict.yours')}: {conflict.wantedValue}</p>
            <div class="conflict__actions">
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
          </section>
        {/if}

        {#if error}
          <p class="error-text" role="alert">{error}</p>
        {/if}

        <div class="step-chat">
          <Button
            variant="ghost"
            disabled={!chatOffered}
            on:click={() => (chatOpen = true)}
            data-testid="project-chat-open"
          >
            {$t('project.onboarding.open_chat')}
          </Button>
          {#if !chatOffered}
            <span class="step-chat__reason" data-testid="project-chat-disabled">{chatReason}</span>
          {/if}
        </div>

        <div class="state-card__actions">
          {#if activeProjectId}
            {#each linkIds() as id}
              <Button
                variant="secondary"
                loading={isSubmitting}
                on:click={() => applyStep(id)}
                data-testid="project-step-link-{id}"
              >
                {linkLabel(id)}
              </Button>
            {/each}
          {:else}
            <Button
              variant="secondary"
              loading={isSubmitting}
              on:click={continueRun}
              data-testid="project-continue"
            >
              {$t('project.adding.continue')}
            </Button>
            {#if beingAdded}
              <Button
                variant="secondary"
                on:click={() => (confirmAbandon = true)}
                data-testid="project-abandon"
              >
                {$t('project.adding.abandon')}
              </Button>
            {/if}
          {/if}
        </div>
      </div>
    {/if}
  </main>
</div>

{#if chatOpen}
  <div
    class="drawer-backdrop"
    role="presentation"
    onclick={(event) => {
      if (event.target === event.currentTarget) chatOpen = false;
    }}
  >
    <div
      class="chat-drawer"
      role="dialog"
      aria-modal="true"
      aria-label={$t('project.chat.title')}
      data-testid="project-chat-drawer"
    >
      <header class="chat-drawer__header">
        <h2 class="chat-drawer__title">{$t('project.chat.title')}</h2>
        <Button
          variant="ghost"
          on:click={() => (chatOpen = false)}
          data-testid="project-chat-close"
        >
          {$t('project.chat.close')}
        </Button>
      </header>

      <div class="chat-drawer__turns">
        {#if !run?.chat?.length}
          <p class="chat-drawer__empty">{$t('project.chat.no_turns')}</p>
        {/if}
        {#each run?.chat ?? [] as turn}
          <article class="chat-turn" class:chat-turn--untrusted={turn.untrusted}>
            <p class="chat-turn__role">{turn.role}</p>
            <p class="chat-turn__text">{turn.text}</p>
            {#if turn.untrusted}
              <p class="chat-turn__untrusted">{$t('project.chat.untrusted')}</p>
            {/if}
          </article>
        {/each}
      </div>

      <div class="chat-drawer__compose">
        <input
          type="text"
          class="field-input"
          placeholder={$t('project.chat.placeholder')}
          bind:value={chatDraft}
          data-testid="project-chat-input"
        />
        <Button
          variant="primary"
          loading={isChatting}
          disabled={!chatDraft.trim() || !activeProjectId}
          on:click={sendChat}
          data-testid="project-chat-send"
        >
          {$t('project.chat.send')}
        </Button>
      </div>
      {#if isChatting}
        <p class="chat-drawer__waiting">{$t('project.chat.waiting')}</p>
      {/if}
    </div>
  </div>
{/if}

{#if confirmAbandon && beingAdded}
  <ConfirmDialog
    open={true}
    title={$t('project.adding.abandon_title', { path: beingAdded.checkout_path })}
    consequences={[$t('project.adding.abandon_confirm', { path: beingAdded.checkout_path })]}
    actionLabel={$t('project.adding.abandon')}
    cancelLabel={$t('common.action.cancel')}
    busy={false}
    testid="project-abandon-modal"
    onConfirm={abandonRun}
    onClose={() => (confirmAbandon = false)}
  />
{/if}

<style>
  .identity {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    padding: var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
  }

  .identity__title {
    font-size: var(--text-base);
    font-weight: var(--weight-semibold);
    color: var(--color-text-primary);
    margin: 0;
  }

  .identity__actions {
    display: flex;
    justify-content: flex-end;
  }

  .needs-terminal {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
    padding: var(--space-3);
    border: 1px solid var(--color-warning-border);
    border-radius: var(--radius-md);
    background-color: var(--color-attention-subtle);
  }

  .needs-terminal__title {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-base);
    font-weight: var(--weight-semibold);
    color: var(--color-text-primary);
    margin: 0;
  }

  .needs-terminal__line,
  .needs-terminal__command-label {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    margin: 0;
  }

  .needs-terminal__command {
    display: block;
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }

  .project-onboarding {
    min-height: 100vh;
    background-color: var(--color-bg-app);
    display: flex;
    flex-direction: column;
  }

  .project-onboarding__header {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    align-items: flex-start;
    justify-content: space-between;
    padding: var(--space-6) var(--space-4) var(--space-4);
    max-width: 60rem;
    width: 100%;
    margin: 0 auto;
  }

  .project-onboarding__title {
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .project-onboarding__progress {
    margin-top: var(--space-1);
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .step-line {
    overflow-x: auto;
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-surface);
    border-block: 1px solid var(--color-border);
  }

  .step-line__list {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    min-width: max-content;
    list-style: none;
  }

  .step-line__item {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .step-line__item--current {
    color: var(--color-primary);
    font-weight: 600;
  }

  .step-line__item--done {
    color: var(--color-success);
  }

  .step-line__item--skipped {
    color: var(--color-warning);
  }

  .step-line__indicator {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 1.5rem;
    height: 1.5rem;
    border-radius: 50%;
    border: 1px solid currentColor;
  }

  .step-line__item--current .step-line__indicator {
    background-color: var(--color-primary);
    color: var(--color-primary-text);
    border-color: var(--color-primary);
  }

  .step-line__status {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .project-onboarding__main {
    flex: 1;
    width: 100%;
    max-width: 50rem;
    margin: 0 auto;
    padding: var(--space-8) var(--space-4);
  }

  .state-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-6);
    box-shadow: var(--shadow-sm);
  }

  .state-card__header {
    margin-bottom: var(--space-5);
  }

  .state-card__title {
    font-size: var(--text-xl);
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .state-card__summary,
  .state-card__scope,
  .state-card__text {
    margin-top: var(--space-2);
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    overflow-wrap: anywhere;
  }

  .state-card__scope {
    color: var(--color-text-muted);
  }

  .state-card__actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: var(--space-3);
    margin-top: var(--space-5);
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-4);
  }

  .field {
    display: block;
    margin-bottom: var(--space-1);
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .field-input {
    width: 100%;
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
    font-size: var(--text-sm);
  }

  .build-env {
    margin-bottom: var(--space-4);
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
  }

  .build-env__title {
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .build-env__line {
    margin-top: var(--space-1);
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    overflow-wrap: anywhere;
  }

  .build-env__details {
    margin-top: var(--space-2);
    font-size: var(--text-sm);
  }

  .build-env__variables {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    margin-top: var(--space-2);
    max-height: 14rem;
    overflow-y: auto;
  }

  .build-env__variable {
    display: grid;
    grid-template-columns: minmax(8rem, 1fr) 2fr;
    gap: var(--space-3);
    font-size: var(--text-xs);
    overflow-wrap: anywhere;
  }

  .build-env__variable dt {
    color: var(--color-text-secondary);
  }

  .conflict {
    margin-top: var(--space-4);
    padding: var(--space-4);
    background-color: var(--color-warning-subtle);
    border: 1px solid var(--color-warning-border);
    border-radius: var(--radius-md);
  }

  .conflict__title {
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-warning);
  }

  .conflict__line {
    margin-top: var(--space-1);
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .conflict__actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: var(--space-3);
    margin-top: var(--space-3);
  }

  .step-chat {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    margin-top: var(--space-4);
  }

  .step-chat__reason {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .error-text {
    margin-top: var(--space-3);
    color: var(--color-danger);
    font-size: var(--text-sm);
  }

  .badge-success {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 3rem;
    height: 3rem;
    border-radius: 50%;
    background-color: var(--color-success);
    color: var(--color-text-inverse);
    font-size: 1.5rem;
    font-weight: bold;
    margin-bottom: var(--space-3);
  }

  .drawer-backdrop {
    position: fixed;
    inset: 0;
    background-color: var(--color-overlay);
    display: flex;
    justify-content: flex-end;
    z-index: 100;
  }

  .chat-drawer {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    width: min(28rem, 100%);
    height: 100%;
    padding: var(--space-5);
    background-color: var(--color-bg-surface);
    border-left: 1px solid var(--color-border);
  }

  .chat-drawer__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
  }

  .chat-drawer__title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .chat-drawer__turns {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .chat-drawer__empty {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }

  .chat-turn {
    padding: var(--space-3);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
  }

  .chat-turn--untrusted {
    border-left: 3px solid var(--color-warning);
  }

  .chat-turn__role {
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .chat-turn__text {
    margin-top: var(--space-1);
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }

  .chat-turn__untrusted {
    margin-top: var(--space-1);
    font-size: var(--text-xs);
    color: var(--color-warning);
  }

  .chat-drawer__compose {
    display: flex;
    gap: var(--space-2);
    align-items: center;
  }

  .chat-drawer__waiting {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  @media (max-width: 640px) {
    .project-onboarding__main {
      padding: var(--space-4) var(--space-3);
    }

    .state-card {
      padding: var(--space-4);
    }

    .state-card__actions,
    .conflict__actions {
      flex-direction: column-reverse;
      align-items: stretch;
    }

    .build-env__variable {
      grid-template-columns: 1fr;
      gap: 0;
    }
  }
</style>
