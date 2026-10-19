<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import StepBar from '../components/StepBar.svelte';
  import ConsentBox from '../components/ConsentBox.svelte';
  import LanguageChoice from '../components/LanguageChoice.svelte';
  import HarnessChoice from '../components/HarnessChoice.svelte';
  import ModelsChoice from '../components/ModelsChoice.svelte';
  import AspectChoice from '../components/AspectChoice.svelte';
  import {
    runtimeStore,
    navigateTo,
    loadOnboardingState,
    stepOnboarding,
    startOnboardingAuto,
    completedStepNumbers,
  } from '../stores/runtime';
  import type { HypermediaLink } from '../api/client';

  let isSubmitting = false;
  let selectedStep: number | null = null;
  let loadFailed = false;

  onMount(async () => {
    // Pull the state the runtime reports; a machine that is not paired keeps the local one.
    const state = await loadOnboardingState();
    loadFailed = state === null && $runtimeStore.isPaired;
  });

  $: onboardingState = $runtimeStore.onboarding;
  $: currentState = onboardingState ?? $runtimeStore.currentState;
  $: stateId = currentState?.state || 'onboard.mode';
  $: activeStep = $runtimeStore.activeStep;
  $: isCompleted = stateId === 'onboard.completed';
  $: doneSteps = completedStepNumbers($runtimeStore.completedSteps);

  // A consent is any state whose id names one: `<step>.consent` or `<step>.<action>-consent`.
  $: isConsentState = /\.consent$/.test(stateId) || /-consent$/.test(stateId);

  // A model working state reports the role, the harness, the model and the tokens expected.
  $: isModelWorking =
    stateId.endsWith('.probe') ||
    stateId.endsWith('.working') ||
    stateId === 'catalogue.advise' ||
    stateId === 'harness.verify-profile' ||
    stateId === 'auto.run';

  $: offeredLinks = getLinks(currentState);

  interface OfferedLink {
    id: string;
    link: HypermediaLink;
    isPrimary: boolean;
  }

  /**
   * Reads the links a state offers, keeping the order the runtime reported and normalising
   * each href to a path this app may follow.
   */
  function getLinks(state: typeof currentState): OfferedLink[] {
    if (!state) return [];
    const rawLinks = onboardingState?.links ?? state.links ?? state._links;
    if (!rawLinks || typeof rawLinks !== 'object') return [];

    return Object.entries(rawLinks).map(([id, val], index, arr) => {
      let link: HypermediaLink = typeof val === 'string' ? { href: val } : { ...val };
      if (!link.href || !link.href.startsWith('/') || link.href.startsWith('//')) {
        link = { ...link, href: `/${(link.href ?? '').replace(/^[/.]+/, '')}` };
      }
      const isPrimary = index === arr.length - 1;
      return { id, link, isPrimary };
    });
  }

  /** The label of a link, from the catalogue; a link without a key keeps its id. */
  function linkLabel(id: string): string {
    const key = `onboard.link.${id}`;
    const label = $t(key);
    return label === key ? id.replaceAll('_', ' ') : label;
  }

  function linkBy(...ids: string[]): OfferedLink | undefined {
    for (const id of ids) {
      const found = offeredLinks.find((l) => l.id === id);
      if (found) return found;
    }
    return undefined;
  }

  async function handleLinkClick(id: string, link: HypermediaLink, body?: unknown) {
    if (isSubmitting) return;
    isSubmitting = true;
    try {
      if (onboardingState) {
        await stepOnboarding(id);
      } else {
        await stepOnboarding(id);
      }
      void link;
    } catch {
      // A refused step leaves the state as it is.
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

  function handleConsentAccept(remember: boolean) {
    const target = remember
      ? (linkBy('remember', 'yes') ?? offeredLinks[offeredLinks.length - 1])
      : (linkBy('yes', 'remember') ?? offeredLinks[offeredLinks.length - 1]);
    if (target) {
      void handleLinkClick(target.id, target.link, { remember });
    }
  }

  function handleConsentDecline() {
    const target = linkBy('no', 'decline') ?? offeredLinks[0];
    if (target) {
      void handleLinkClick(target.id, target.link, { remember: false });
    }
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

  /**
   * @param step the step number, 1-based
   * @returns the catalogue key suffix of the step's name
   */
  function stepKey(step: number): string {
    return STEP_KEYS[step - 1] ?? 'done';
  }
</script>

<div class="onboarding" data-testid="onboarding-view">
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
            <Button variant="secondary" on:click={() => void loadOnboardingState()}>
              {$t('onboard.error.retry')}
            </Button>
          </div>
        </div>
      {:else if isCompleted}
        <div class="state-card">
          <div class="state-card__header">
            <span class="badge-success" aria-hidden="true">✓</span>
            <h1 class="state-card__title">{$t('onboard.completed.title')}</h1>
            <p class="state-card__summary">
              {stringField('summary') || $t('onboard.completed.summary')}
            </p>
          </div>

          {#if doneSteps.length > 0}
            <ul class="completed-list" data-testid="completed-steps">
              {#each doneSteps as step}
                <li class="completed-row">
                  <span class="completed-row__check" aria-hidden="true">✓</span>
                  <span class="completed-row__name">{$t(`onboard.step.${stepKey(step)}`)}</span>
                  <span class="completed-row__value">
                    {stepSummaries[String(step)] ?? $t('onboard.completed.no_summary')}
                  </span>
                </li>
              {/each}
            </ul>
          {/if}

          <p class="state-card__next">{$t('onboard.completed.next')}</p>

          <div class="state-card__actions state-card__actions--center">
            <Button variant="secondary" on:click={() => navigateTo('machine-settings')}>
              {$t('onboard.completed.machine_settings')}
            </Button>
            <Button variant="primary" on:click={() => navigateTo('projects')}>
              {$t('onboard.completed.to_projects')}
            </Button>
          </div>
        </div>
      {:else if isConsentState}
        <ConsentBox
          title={stringField('summary') ?? $t('common.consent.yes')}
          looksFor={stringField('looks_for')}
          inDir={stringField('in')}
          runs={stringField('runs')}
          never={stringField('never')}
          tokens={stringField('tokens')}
          canRemember={currentState?.kind !== 'auto_run' &&
            currentState?.kind !== 'verify_harness_profile'}
          busy={isSubmitting}
          primaryLabel={consentAction()}
          onAccept={handleConsentAccept}
          onDecline={handleConsentDecline}
        />
      {:else if isModelWorking}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.step.models')}
            </h1>
          </div>
          <div class="model-working">
            <Spinner size="2rem" />
            <div class="model-working__details">
              <p class="model-working__status">
                {$t('common.model.working', {
                  role: stringField('role') ?? 'steward',
                  harness: stringField('harness') ?? '',
                  model: stringField('model') ?? '',
                })}
              </p>
              {#if stringField('tokens')}
                <p class="model-working__estimate">{stringField('tokens')}</p>
              {/if}
            </div>
          </div>
          <p class="model-working__note">{$t('common.model.leave_note')}</p>
          {#if offeredLinks.length > 0}
            <div class="state-card__actions">
              {#each offeredLinks as { id, link }}
                <Button
                  variant="secondary"
                  loading={isSubmitting}
                  on:click={() => handleLinkClick(id, link)}
                >
                  {$t('common.model.continue_without')}
                </Button>
              {/each}
            </div>
          {/if}
        </div>
      {:else if stateId === 'onboard.mode' || stateId === 'onboard.entry'}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">{$t('onboard.entry.title')}</h1>
            <p class="state-card__summary">{$t('onboard.entry.intro')}</p>
          </div>

          <ol class="steps-overview" data-testid="steps-overview">
            {#each [1, 2, 3, 4, 5, 6, 7] as step}
              <li>
                <strong>{step}. {$t(`onboard.step.${stepKey(step)}`)}</strong>
                ─ {$t(`onboard.entry.step.${stepKey(step)}`)}
              </li>
            {/each}
          </ol>

          <p class="state-card__next">{$t('onboard.entry.stop')}</p>

          <div class="state-card__actions">
            {#if offeredLinks.length > 0}
              {#each offeredLinks as { id, link, isPrimary }}
                <Button
                  variant={isPrimary ? 'primary' : 'secondary'}
                  loading={isSubmitting}
                  on:click={() => handleLinkClick(id, link)}
                >
                  {#if id === 'auto'}
                    {$t('onboard.mode.auto')}
                  {:else if id === 'begin'}
                    {$t('onboard.mode.guided')}
                  {:else}
                    {linkLabel(id)}
                  {/if}
                </Button>
              {/each}
            {:else}
              <Button variant="secondary" loading={isSubmitting} on:click={handleAuto}>
                {$t('onboard.mode.auto')}
              </Button>
              <Button
                variant="primary"
                loading={isSubmitting}
                on:click={() =>
                  void handleLinkClick('begin', { href: '/api/v1/onboarding/language' })}
              >
                {$t('onboard.mode.guided')}
              </Button>
            {/if}
          </div>
        </div>
      {:else if stateId === 'language.choose'}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.language.choose.title')}
            </h1>
            <p class="state-card__summary">{$t('onboard.language.choose.intro')}</p>
          </div>
          <LanguageChoice
            current={{
              interface: stringField('interface') ?? 'en',
              model: stringField('model') ?? 'same',
            }}
            busy={isSubmitting}
            onRecord={() => {
              const target = linkBy('set', 'record') ?? offeredLinks[offeredLinks.length - 1];
              if (target) void handleLinkClick(target.id, target.link);
            }}
          />
        </div>
      {:else if stateId === 'harness.choose'}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.harness.choose.title')}
            </h1>
          </div>
          <HarnessChoice
            harnesses={arrayField('harnesses')}
            busy={isSubmitting}
            onCheck={() => {
              const target = linkBy('choose', 'check') ?? offeredLinks[offeredLinks.length - 1];
              if (target) void handleLinkClick(target.id, target.link);
            }}
          />
        </div>
      {:else if stateId === 'harness.blocked'}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.harness.blocked.title', { name: '' })}
            </h1>
          </div>
          <dl class="detail-list">
            {#if stringField('cause')}
              <div class="detail-row">
                <dt>{$t('onboard.harness.probe.cause', { cause: '' }).replace(/:?\s*$/, '')}</dt>
                <dd>{stringField('cause')}</dd>
              </div>
            {/if}
            {#if stringField('fix')}
              <div class="detail-row">
                <dt>{$t('onboard.harness.probe.fix', { fix: '' }).replace(/:?\s*$/, '')}</dt>
                <dd class="mono">{stringField('fix')}</dd>
              </div>
            {/if}
          </dl>
          <div class="state-card__actions">
            {#each offeredLinks as { id, link, isPrimary }}
              <Button
                variant={isPrimary ? 'primary' : 'secondary'}
                loading={isSubmitting}
                on:click={() => handleLinkClick(id, link)}
              >
                {linkLabel(id)}
              </Button>
            {/each}
          </div>
        </div>
      {:else if stateId === 'harness.confirm' || stateId === 'harness.add-confirm'}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.harness.confirm.title', { count: 0 })}
            </h1>
          </div>
          {#if stringField('default')}
            <p class="state-card__summary">
              {$t('onboard.harness.choose.default_label')}:
              <span class="mono">
                {stringField('default')}
              </span>
            </p>
          {/if}
          <div class="state-card__actions">
            {#each offeredLinks as { id, link, isPrimary }}
              <Button
                variant={isPrimary ? 'primary' : 'secondary'}
                loading={isSubmitting}
                on:click={() => handleLinkClick(id, link)}
              >
                {linkLabel(id)}
              </Button>
            {/each}
          </div>
        </div>
      {:else if stateId === 'catalogue.select'}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.catalogue.select.title')}
            </h1>
          </div>
          <ModelsChoice
            models={arrayField('models')}
            harnessNames={harnessNames()}
            busy={isSubmitting}
            onRecord={() => {
              const target = linkBy('set', 'record') ?? offeredLinks[offeredLinks.length - 1];
              if (target) void handleLinkClick(target.id, target.link);
            }}
          />
        </div>
      {:else if stateId === 'roles.review'}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.roles.review.title')}
            </h1>
          </div>
          <AspectChoice
            kind="roles"
            roles={arrayField('roles')}
            busy={isSubmitting}
            onRecord={() => {
              const target = linkBy('accept', 'record') ?? offeredLinks[offeredLinks.length - 1];
              if (target) void handleLinkClick(target.id, target.link);
            }}
          />
        </div>
      {:else if stateId === 'store.choose'}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.store.choose.title')}
            </h1>
          </div>
          <AspectChoice
            kind="store"
            stores={arrayField('stores')}
            busy={isSubmitting}
            onRecord={(choice) => {
              const target = linkBy(String(choice), 'os', 'file') ?? offeredLinks[0];
              if (target) void handleLinkClick(target.id, target.link);
            }}
          />
        </div>
      {:else if stateId === 'providers.select'}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.providers.select.title')}
            </h1>
          </div>
          <AspectChoice
            kind="providers"
            providers={arrayField('providers')}
            busy={isSubmitting}
            onRecord={() => {
              const target = linkBy('set', 'record') ?? offeredLinks[offeredLinks.length - 1];
              if (target) void handleLinkClick(target.id, target.link);
            }}
          />
        </div>
      {:else if stateId === 'providers.failed'}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.providers.failed.title')}
            </h1>
          </div>
          <dl class="detail-list">
            {#if stringField('provider')}
              <div class="detail-row">
                <dt>{$t('onboard.providers.select.title')}</dt>
                <dd>{stringField('provider')}</dd>
              </div>
            {/if}
            {#if stringField('cause')}
              <div class="detail-row">
                <dt>{$t('onboard.harness.probe.cause', { cause: '' }).replace(/:?\s*$/, '')}</dt>
                <dd>{stringField('cause')}</dd>
              </div>
            {/if}
            {#if stringField('fix')}
              <div class="detail-row">
                <dt>{$t('onboard.harness.probe.fix', { fix: '' }).replace(/:?\s*$/, '')}</dt>
                <dd class="mono">{stringField('fix')}</dd>
              </div>
            {/if}
          </dl>
          <div class="state-card__actions">
            {#each offeredLinks as { id, link, isPrimary }}
              <Button
                variant={isPrimary ? 'primary' : 'secondary'}
                loading={isSubmitting}
                on:click={() => handleLinkClick(id, link)}
              >
                {linkLabel(id)}
              </Button>
            {/each}
          </div>
        </div>
      {:else if stateId === 'defaults.review'}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.defaults.review.title')}
            </h1>
          </div>
          <AspectChoice
            kind="defaults"
            planDefaults={{
              landing: stringField('landing') ?? '',
              method: stringField('method') ?? '',
              asking: stringField('asking') ?? '',
              posture: stringField('posture'),
            }}
            busy={isSubmitting}
            onRecord={() => {
              const target = linkBy('accept', 'set') ?? offeredLinks[offeredLinks.length - 1];
              if (target) void handleLinkClick(target.id, target.link);
            }}
          />
        </div>
      {:else if stateId === 'auto.result'}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.auto.result.title')}
            </h1>
          </div>
          <table class="result-table" data-testid="auto-result">
            <thead>
              <tr>
                <th>{$t('onboard.auto.result.aspect')}</th>
                <th>{$t('onboard.auto.result.value')}</th>
                <th>{$t('onboard.auto.result.reason')}</th>
              </tr>
            </thead>
            <tbody>
              {#each arrayField<Record<string, string>>('results') as result}
                <tr>
                  <td>{$t(`onboard.step.${result.aspect ?? ''}`)}</td>
                  <td>{result.value}</td>
                  <td>{$t(`onboard.auto.reason.${result.reason ?? 'default'}`)}</td>
                </tr>
              {/each}
            </tbody>
          </table>
          <div class="state-card__actions">
            {#each offeredLinks as { id, link, isPrimary }}
              <Button
                variant={isPrimary ? 'primary' : 'secondary'}
                loading={isSubmitting}
                on:click={() => handleLinkClick(id, link)}
              >
                {linkLabel(id)}
              </Button>
            {/each}
          </div>
        </div>
      {:else}
        <!-- A state without a screen of its own: what it reports, and one control per link. -->
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">
              {stringField('summary') || $t('onboard.state.details')}
            </h1>
            {#if stringField('scope')}
              <p class="state-card__scope mono">
                {$t(`onboard.scope.${stringField('scope')}`)} · {stateId}
              </p>
            {/if}
          </div>

          <div class="state-card__body">
            {#each Object.entries(currentState ?? {}) as [key, value]}
              {#if !['state', 'scope', 'summary', 'links', '_links', 'completed_steps', 'auto'].includes(key) && typeof value === 'string'}
                <p class="state-card__text">{value}</p>
              {/if}
            {/each}
          </div>

          <div class="state-card__actions">
            {#if offeredLinks.length === 0}
              <p class="state-card__next">{$t('onboard.state.no_links')}</p>
            {:else}
              {#each offeredLinks as { id, link, isPrimary }}
                <Button
                  variant={isPrimary ? 'primary' : 'secondary'}
                  loading={isSubmitting}
                  on:click={() => handleLinkClick(id, link)}
                >
                  {linkLabel(id)}
                </Button>
              {/each}
            {/if}
          </div>
        </div>
      {/if}
    </div>
  </main>
</div>

<style>
  .onboarding {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    background-color: var(--color-bg-app);
  }

  .onboarding__main {
    flex: 1;
    padding: var(--space-8) var(--space-4);
  }

  .onboarding__container {
    max-width: 50rem;
    margin: 0 auto;
  }

  .state-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-6);
    box-shadow: var(--shadow-sm);
  }

  .state-card__header {
    margin-bottom: var(--space-6);
  }

  .state-card__title {
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
    margin-bottom: var(--space-2);
  }

  .state-card__summary,
  .state-card__next {
    font-size: var(--text-base);
    color: var(--color-text-secondary);
  }

  .state-card__next {
    margin-top: var(--space-4);
    font-size: var(--text-sm);
  }

  .state-card__scope {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .state-card__text {
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    margin-bottom: var(--space-2);
    overflow-wrap: anywhere;
  }

  .state-card__actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: var(--space-3);
    margin-top: var(--space-6);
    border-top: 1px solid var(--color-border-subtle);
    padding-top: var(--space-4);
  }

  .state-card__actions--center {
    justify-content: center;
  }

  .steps-overview {
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin-bottom: var(--space-6);
    padding: var(--space-4);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .steps-overview strong {
    color: var(--color-text-primary);
  }

  .completed-list {
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .completed-row {
    display: grid;
    grid-template-columns: auto 8rem 1fr;
    gap: var(--space-3);
    align-items: baseline;
    padding: var(--space-2) var(--space-3);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    font-size: var(--text-sm);
  }

  .completed-row__check {
    color: var(--color-success);
  }

  .completed-row__name {
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .completed-row__value {
    color: var(--color-text-secondary);
  }

  .model-working {
    display: flex;
    align-items: center;
    gap: var(--space-4);
    padding: var(--space-4);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    margin-bottom: var(--space-4);
  }

  .model-working__status {
    font-weight: 600;
    font-size: var(--text-base);
    color: var(--color-text-primary);
  }

  .model-working__estimate {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }

  .model-working__note {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    font-style: italic;
  }

  .detail-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .detail-row {
    display: grid;
    grid-template-columns: 8rem 1fr;
    gap: var(--space-4);
    font-size: var(--text-sm);
  }

  .detail-row dt {
    color: var(--color-text-secondary);
    font-weight: 600;
  }

  .detail-row dd {
    color: var(--color-text-primary);
    overflow-wrap: anywhere;
  }

  .result-table {
    width: 100%;
    border-collapse: collapse;
    font-size: var(--text-sm);
  }

  .result-table th,
  .result-table td {
    padding: var(--space-2) var(--space-3);
    text-align: left;
    border-bottom: 1px solid var(--color-border-subtle);
    vertical-align: top;
  }

  .result-table th {
    color: var(--color-text-secondary);
    font-weight: 600;
    background-color: var(--color-bg-subtle);
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

  @media (max-width: 768px) {
    .onboarding__main {
      padding: var(--space-4) var(--space-3);
    }

    .state-card {
      padding: var(--space-4);
    }

    .state-card__title {
      font-size: var(--text-xl);
    }

    .completed-row,
    .detail-row {
      grid-template-columns: 1fr;
      gap: var(--space-1);
    }

    .state-card__actions {
      flex-direction: column-reverse;
      align-items: stretch;
    }
  }
</style>
