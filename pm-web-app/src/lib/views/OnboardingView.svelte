<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import BrandMark from '../components/BrandMark.svelte';
  import StepBar from '../components/StepBar.svelte';
  import ProjectOnboardingView from './ProjectOnboardingView.svelte';
  import ScreenHost from './onboarding/ScreenHost.svelte';
  import {
    screenFor,
    type OfferedLink,
    type OnboardingScreenContext,
    type OnboardingState,
  } from './onboarding/screens';
  import {
    runtimeStore,
    loadOnboardingState,
    stepOnboarding,
    startOnboardingAuto,
    completedStepNumbers,
    requestedOnboardingStep,
  } from '../stores/runtime';

  /**
   * The onboarding frame: the step bar, the frame's own header on a phone, and the one screen the
   * registry names for the current state (`./onboarding/screens.ts`). A state no screen claims
   * draws the fallback, which names the terminal command and never prints the state's id.
   *
   * This view holds no chain of `{:else if}` per state any more: a new state is a new screen in the
   * registry, and the registry test fails the build for a state of the mock no screen claims.
   */

  let isSubmitting = false;
  let selectedStep: number | null = null;
  let loadFailed = false;

  onMount(async () => {
    // Pull the state the runtime reports; a machine that is not paired keeps the local one.
    const state = await loadOnboardingState();
    loadFailed = state === null && $runtimeStore.isPaired;
    // A `Change` on the machine settings names the aspect's step; a reload repeats it from the
    // address bar, so both open the onboarding at the same aspect.
    const requested = requestedOnboardingStep(STEP_KEYS.length);
    if (requested !== null) {
      selectedStep = requested;
      // The step is consumed once, so a later visit opens the onboarding where it stands.
      runtimeStore.update((s) => ({ ...s, onboardingStep: null }));
    }
  });

  $: isProjectOnboarding = $runtimeStore.currentRoute === 'project-onboarding';

  $: onboardingState = $runtimeStore.onboarding;
  $: currentState = onboardingState ?? $runtimeStore.currentState;
  $: stateId = currentState?.state || 'onboard.mode';
  $: activeStep = $runtimeStore.activeStep;
  $: doneSteps = completedStepNumbers($runtimeStore.completedSteps);

  // The one screen of the current state; `null` draws the fallback.
  $: screen = screenFor(stateId);

  $: offeredLinks = getLinks(currentState);

  /**
   * Reads the links a state offers, keeping the order the runtime reported.
   *
   * @param state the state to read
   * @returns the links, the last one being the primary act
   */
  function getLinks(state: typeof currentState): OfferedLink[] {
    if (!state) return [];
    const rawLinks = onboardingState?.links ?? state.links ?? state._links;
    if (!rawLinks || typeof rawLinks !== 'object') return [];

    return Object.keys(rawLinks).map((id, index, arr) => ({
      id,
      isPrimary: index === arr.length - 1,
    }));
  }

  /** The label of a link, from the catalogue; a link without a key keeps its id. */
  function linkLabel(id: string): string {
    const key = `onboard.link.${id}`;
    const label = $t(key);
    return label === key ? id.replaceAll('_', ' ') : label;
  }

  /**
   * @param ids the link ids, in the order they are preferred
   * @returns the first of them the state offers, or `undefined`
   */
  function linkBy(...ids: string[]): OfferedLink | undefined {
    for (const id of ids) {
      const found = offeredLinks.find((l) => l.id === id);
      if (found) return found;
    }
    return undefined;
  }

  /**
   * Takes the step a link names; a refused step leaves the state as it is (the store's own rule).
   *
   * @param id the link's id
   * @param body what the step records
   */
  async function handleLinkClick(id: string, body: Record<string, unknown> = {}) {
    if (isSubmitting) return;
    isSubmitting = true;
    try {
      await stepOnboarding(id, body);
    } finally {
      isSubmitting = false;
    }
  }

  async function handleAuto() {
    if (isSubmitting) return;
    isSubmitting = true;
    try {
      await startOnboardingAuto();
    } finally {
      isSubmitting = false;
    }
  }

  /** Leaves a state the app cannot draw: back in the history, or the state read again. */
  function goBack(): void {
    if (typeof window !== 'undefined' && window.history.length > 1) {
      window.history.back();
      return;
    }
    void loadOnboardingState();
  }

  /** The summaries of the done steps, as the runtime reports them per aspect. */
  $: stepSummaries = buildStepSummaries(onboardingState);

  function buildStepSummaries(state: typeof onboardingState): Record<string, string> {
    const summaries: Record<string, string> = {};
    const reported = state?.step_summaries;
    if (reported && typeof reported === 'object') {
      for (const [step, value] of Object.entries(reported as Record<string, unknown>)) {
        summaries[String(step)] = String(value);
      }
    }
    const aspects = state?.aspects ?? state?.completed_aspects;
    if (Array.isArray(aspects)) {
      for (const aspect of aspects as Array<Record<string, unknown>>) {
        const step = aspect.step ?? aspect.number;
        const summary = aspect.summary ?? aspect.value;
        if (step !== undefined && summary !== undefined) {
          summaries[String(step)] = String(summary);
        }
      }
    }
    return summaries;
  }

  function selectStep(step: number): void {
    selectedStep = step === 0 ? null : step;
  }

  function stringField(name: string): string | undefined {
    const value = currentState?.[name];
    return typeof value === 'string' && value.length > 0 ? value : undefined;
  }

  /**
   * The body of the completed card.
   *
   * A runtime that sends the card's own title as the summary would print the title twice, so that
   * summary is replaced by the catalogue's own sentence, which names what is ready.
   *
   * @returns the summary to show under the title
   */
  function completedSummary(): string {
    const summary = stringField('summary');
    if (summary !== undefined && summary.trim() !== $t('onboard.completed.title')) {
      return summary;
    }
    return $t('onboard.completed.summary');
  }

  function arrayField<T>(name: string): T[] {
    const value = currentState?.[name];
    return Array.isArray(value) ? (value as T[]) : [];
  }

  function harnessNames(): Record<string, string> {
    const names: Record<string, string> = {};
    const harnesses = $runtimeStore.onboarding?.harnesses;
    if (Array.isArray(harnesses)) {
      for (const harness of harnesses as Array<Record<string, unknown>>) {
        if (harness.harness_id && harness.name) {
          names[String(harness.harness_id)] = String(harness.name);
        }
      }
    }
    return names;
  }

  /**
   * The action the consent's primary button names, by the kind the runtime reports; a kind
   * without a catalogue entry falls back to the link's own label.
   */
  function consentAction(): string {
    const kind = typeof currentState?.kind === 'string' ? currentState.kind : '';
    if (kind) {
      const key = `onboard.consent.action.${kind}`;
      const action = $t(key);
      if (action !== key) return action;
    }
    const primary =
      offeredLinks.find((l) => l.id === 'yes') ?? offeredLinks[offeredLinks.length - 1];
    return primary ? linkLabel(primary.id) : $t('common.consent.yes');
  }

  const STEP_KEYS = [
    'language',
    'harnesses',
    'models',
    'roles',
    'credentials',
    'providers',
    'plans',
  ];

  /** The step numbers the overview lists: the one list the intro's count comes from. */
  const MACHINE_STEPS = STEP_KEYS.map((_, index) => index + 1);

  /**
   * @param step the step number, 1-based
   * @returns the catalogue key suffix of the step's name
   */
  function stepKey(step: number): string {
    return STEP_KEYS[step - 1] ?? 'done';
  }

  // Everything a screen reads and reports: the runtime conversation stays with this view.
  $: view = {
    state: (currentState ?? null) as OnboardingState | null,
    links: offeredLinks,
    busy: isSubmitting,
    doneSteps,
    summaries: stepSummaries,
    machineSteps: MACHINE_STEPS,
    onLink: (id: string, body: Record<string, unknown> = {}) => void handleLinkClick(id, body),
    onAuto: () => void handleAuto(),
    onBack: goBack,
    stepKey,
    field: stringField,
    arrayField,
    linkLabel,
    consentAction,
    completedSummary,
    harnessNames,
    linkBy,
  } satisfies OnboardingScreenContext;
</script>

{#if isProjectOnboarding}
  <ProjectOnboardingView
    projectId={$runtimeStore.selectedProjectId ?? null}
    onboardingId={$runtimeStore.selectedOnboardingId ?? null}
  />
{:else}
  <div class="onboarding" data-testid="onboarding-view">
    <!--
      The rail head carries the mark from 768 px up; below that the rail is hidden, so the
      onboarding's own header carries it — one mark per screen at every width (PLAN-130 residue).
    -->
    <header class="onboarding__brand" data-testid="onboarding-brand">
      <BrandMark rendering="colour" size={28} />
      <span class="onboarding__wordmark">{$t('web.frame.brand')}</span>
    </header>

    <StepBar
      {activeStep}
      completedSteps={doneSteps}
      {selectedStep}
      summaries={stepSummaries}
      onSelectStep={selectStep}
    />

    <main class="onboarding__main">
      <div class="onboarding__container">
        {#if loadFailed}
          <div class="state-card" role="alert">
            <h1 class="state-card__title">{$t('onboard.error.no_state')}</h1>
            <div class="state-card__actions">
              <Button
                variant="secondary"
                on:click={async () => {
                  const state = await loadOnboardingState();
                  loadFailed = state === null && $runtimeStore.isPaired;
                }}
              >
                {$t('onboard.error.retry')}
              </Button>
            </div>
          </div>
        {:else}
          <ScreenHost {screen} {view} />
        {/if}
      </div>
    </main>
  </div>
{/if}

<style>
  .onboarding {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    background-color: var(--color-bg-app);
  }

  /* The mark stands beside the wordmark below 768 px only: from there the rail head carries it. */
  .onboarding__brand {
    display: none;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-3) var(--space-4) 0;
  }

  .onboarding__wordmark {
    color: var(--color-text-primary);
    font-size: var(--text-base);
    font-weight: var(--weight-semibold);
  }

  .onboarding__main {
    flex: 1;
    padding: var(--space-8) var(--space-4);
  }

  .onboarding__container {
    max-width: 50rem;
    margin: 0 auto;
  }

  @media (max-width: 767px) {
    .onboarding__brand {
      display: flex;
    }

    .onboarding__main {
      padding: var(--space-4) var(--space-3);
    }
  }
</style>
