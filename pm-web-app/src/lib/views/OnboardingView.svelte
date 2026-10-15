<script lang="ts">
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import Spinner from '../components/Spinner.svelte';
  import StepBar from '../components/StepBar.svelte';
  import ConsentBox from '../components/ConsentBox.svelte';
  import { runtimeStore, executeLink, navigateTo } from '../stores/runtime';
  import type { HypermediaLink } from '../api/client';

  let isSubmitting = false;

  $: currentState = $runtimeStore.currentState;
  $: stateId = currentState?.state || 'onboard.mode';
  $: activeStep = $runtimeStore.activeStep;
  $: isCompleted = stateId === 'onboard.completed';

  // Check if current state is a consent state
  $: isConsentState = stateId.includes('.consent');

  // Check if current state is a model working state
  $: isModelWorking = stateId.endsWith('.probe') || stateId.endsWith('.working');

  // Extract offered links from state representation
  $: offeredLinks = getLinks(currentState);

  function getLinks(
    state: typeof currentState,
  ): Array<{ id: string; link: HypermediaLink; isPrimary: boolean }> {
    if (!state) return [];
    const rawLinks = state.links || state._links;
    if (!rawLinks || typeof rawLinks !== 'object') return [];

    return Object.entries(rawLinks).map(([id, val], index, arr) => {
      let link: HypermediaLink = typeof val === 'string' ? { href: val } : { ...val };
      if (!link.href.startsWith('/') || link.href.startsWith('//')) {
        link = { ...link, href: `/${link.href.replace(/^[/.]+/, '')}` };
      }
      const isPrimary = index === arr.length - 1;
      return { id, link, isPrimary };
    });
  }

  async function handleLinkClick(link: HypermediaLink | string, body?: unknown) {
    if (isSubmitting) return;
    isSubmitting = true;
    try {
      await executeLink(link, body);
    } catch {
      // Error handled
    } finally {
      isSubmitting = false;
    }
  }

  function handleConsentAccept(remember: boolean) {
    let target = remember
      ? offeredLinks.find((l) => l.id === 'remember' || l.id === 'grant_remember')
      : null;
    if (!target) {
      target = offeredLinks.find(
        (l) => l.id === 'grant' || l.id === 'accept' || l.id === 'yes' || l.isPrimary,
      );
    }
    if (target) {
      handleLinkClick(target.link, { remember });
    }
  }

  function handleConsentDecline() {
    const target = offeredLinks.find(
      (l) => l.id === 'deny' || l.id === 'decline' || l.id === 'no' || !l.isPrimary,
    );
    if (target) {
      handleLinkClick(target.link, { remember: false });
    }
  }
</script>

<div class="onboarding">
  <StepBar {activeStep} />

  <main class="onboarding__main">
    <div class="onboarding__container">
      {#if isCompleted}
        <div class="state-card">
          <div class="state-card__header">
            <span class="badge-success" aria-hidden="true">✓</span>
            <h1 class="state-card__title">{$t('onboard.step.done')}</h1>
            <p class="state-card__summary">
              {currentState?.summary || 'Machine setup is complete.'}
            </p>
          </div>
          <div class="state-card__actions state-card__actions--center">
            <Button variant="primary" on:click={() => navigateTo('workspace')}>
              {$t('web.frame.nav.explorer')}
            </Button>
          </div>
        </div>
      {:else if isConsentState}
        <ConsentBox
          title={currentState?.summary || $t('common.consent.field.looks_for')}
          looksFor={(currentState?.looks_for as string) || undefined}
          inDir={(currentState?.in as string) || undefined}
          runs={(currentState?.runs as string) || undefined}
          never={(currentState?.never as string) || undefined}
          tokens={(currentState?.tokens as string) || undefined}
          onAccept={handleConsentAccept}
          onDecline={handleConsentDecline}
        />
      {:else if isModelWorking}
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">{currentState?.summary || 'Working…'}</h1>
          </div>
          <div class="model-working">
            <Spinner size="2rem" />
            <div class="model-working__details">
              <p class="model-working__status">
                {$t('common.model.working', {
                  role: 'steward',
                  harness: 'Claude Code',
                  model: 'opus-5.5',
                })}
              </p>
              <p class="model-working__estimate">
                {$t('common.model.tokens_estimate', { tokens: '50 k' })}
              </p>
            </div>
          </div>
          <p class="model-working__note">{$t('common.model.leave_note')}</p>
          {#if offeredLinks.length > 0}
            <div class="state-card__actions">
              {#each offeredLinks as { id, link }}
                <Button
                  variant="secondary"
                  loading={isSubmitting}
                  on:click={() => handleLinkClick(link)}
                >
                  {$t(`onboard.${id}`) || id}
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

          <ol class="steps-overview">
            <li>
              <strong>① {$t('onboard.step.language')}</strong> ─ the language of plan-marshall and models
            </li>
            <li>
              <strong>② {$t('onboard.step.harnesses')}</strong> ─ coding agents the server works with
            </li>
            <li>
              <strong>③ {$t('onboard.step.models')}</strong> ─ models plans may use on each agent
            </li>
            <li>
              <strong>④ {$t('onboard.step.roles')}</strong> ─ an agent and a model for every role
            </li>
            <li>
              <strong>⑤ {$t('onboard.step.credentials')}</strong> ─ where this machine keeps tokens
            </li>
            <li>
              <strong>⑥ {$t('onboard.step.providers')}</strong> ─ git hosts, CI and analysis services
            </li>
            <li><strong>⑦ {$t('onboard.step.plans')}</strong> ─ how plans run</li>
          </ol>

          <div class="state-card__actions">
            {#if offeredLinks.length > 0}
              {#each offeredLinks as { id, link, isPrimary }}
                <Button
                  variant={isPrimary ? 'primary' : 'secondary'}
                  loading={isSubmitting}
                  on:click={() => handleLinkClick(link)}
                >
                  {#if id === 'auto'}
                    {$t('onboard.mode.auto')}
                  {:else if id === 'begin'}
                    {$t('onboard.mode.guided')}
                  {:else}
                    {$t(`onboard.${id}`) || id}
                  {/if}
                </Button>
              {/each}
            {:else}
              <Button
                variant="secondary"
                on:click={() => handleLinkClick('/api/v1/operator/onboard/auto', { mode: 'auto' })}
              >
                {$t('onboard.mode.auto')}
              </Button>
              <Button
                variant="primary"
                on:click={() => handleLinkClick('/api/v1/operator/onboard/begin', { mode: 'guided' })}
              >
                {$t('onboard.mode.guided')}
              </Button>
            {/if}
          </div>
        </div>
      {:else}
        <!-- Generic hypermedia state view -->
        <div class="state-card">
          <div class="state-card__header">
            <h1 class="state-card__title">{currentState?.summary || stateId}</h1>
            <p class="state-card__scope mono">scope: {currentState?.scope || 'workspace'}</p>
          </div>

          <div class="state-card__body">
            {#if currentState?.details}
              <p class="state-card__text">{String(currentState.details)}</p>
            {/if}
          </div>

          <div class="state-card__actions">
            {#each offeredLinks as { id, link, isPrimary }}
              <Button
                variant={isPrimary ? 'primary' : 'secondary'}
                loading={isSubmitting}
                on:click={() => handleLinkClick(link)}
              >
                {$t(`onboard.${id}`) || id}
              </Button>
            {/each}
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
    max-width: 800px;
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

  .state-card__summary {
    font-size: var(--text-base);
    color: var(--color-text-secondary);
  }

  .state-card__scope {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .state-card__actions {
    display: flex;
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
</style>
