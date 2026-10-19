<script lang="ts">
  import '../styles/tokens.css';
  import '../styles/base.css';
  import { onMount, onDestroy } from 'svelte';
  import { t, setLocale, currentLocale, initI18n } from '../lib/i18n';
  import {
    runtimeStore,
    initRuntime,
    navigateTo,
    adoptRouteFromLocation,
    syncUrl,
  } from '../lib/stores/runtime';
  import { apiClient } from '../lib/api/client';
  import { pathForRoute } from '../lib/routes';
  import Spinner from '../lib/components/Spinner.svelte';
  import ConnectionStatusBanner from '../lib/components/ConnectionStatusBanner.svelte';
  import PairingView from '../lib/views/PairingView.svelte';
  import OnboardingView from '../lib/views/OnboardingView.svelte';
  import ListenerOffView from '../lib/views/ListenerOffView.svelte';
  import RevokedView from '../lib/views/RevokedView.svelte';
  import StartWorkModal from '../lib/components/StartWorkModal.svelte';
  import LessonSourceModal from '../lib/components/LessonSourceModal.svelte';
  import GitSourceIntakeModal from '../lib/components/GitSourceIntakeModal.svelte';
  import { startModalStore, closeStartWork } from '../lib/stores/start';
  import { lessonSourceModalStore, closeLessonSource } from '../lib/stores/lessonSource';
  import { gitSourceModalStore, closeGitSource } from '../lib/stores/gitSource';

  interface Props {
    children?: import('svelte').Snippet;
  }

  let { children }: Props = $props();

  let theme = $state<'light' | 'dark'>('light');

  onMount(async () => {
    // Detect system theme
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      theme = 'dark';
    }
    document.documentElement.setAttribute('data-theme', theme);

    // The address bar is the route: a reload, a bookmark, or the browser's back returns here.
    adoptRouteFromLocation();
    const onPop = () => adoptRouteFromLocation();
    window.addEventListener('popstate', onPop);
    onDestroy(() => window.removeEventListener('popstate', onPop));

    // Initialize i18n concurrently with a timeout so a slow catalogue load doesn't block startup
    const i18nPromise = Promise.race([
      initI18n(),
      new Promise<void>((resolve) => setTimeout(resolve, 2000)),
    ]);
    await Promise.all([i18nPromise, initRuntime()]);
  });

  // The address bar follows the route the store selects, so a reload opens the same page.
  // It never rewrites a URL that still carries a pairing code in its fragment: that code is
  // redeemed first, and only then does the app move on.
  $effect(() => {
    if ($runtimeStore.isInitialized) {
      syncPath($runtimeStore.currentRoute);
    }
  });

  function syncPath(route: string): void {
    if (typeof window === 'undefined') return;
    // Never rewrite a URL that still carries a pairing code in its fragment: the code is redeemed
    // first, and only then does the app move on.
    if (window.location.hash.includes('code=')) return;
    const path = pathForRoute(route as AppRoute) ?? '/';
    void syncUrl(path, true);
  }

  function toggleTheme() {
    theme = theme === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', theme);
  }

  function toggleLocale() {
    setLocale($currentLocale === 'en' ? 'de' : 'en');
  }
</script>

{#if !$runtimeStore.isInitialized}
  <div class="app-loading" role="status">
    <Spinner size="3rem" label="Initializing application…" />
  </div>
{:else}
  {#if $runtimeStore.isOffline}
    <div class="offline-banner" role="alert">
      <span>{$t('web.offline.unreachable')}</span>
      <button class="offline-reconnect" onclick={initRuntime}>
        {$t('web.offline.reconnecting')}
      </button>
    </div>
  {/if}

  {#if $runtimeStore.unreadReportsArrivalNotice}
    <div class="reports-arrival-banner" role="status">
      <span>{$runtimeStore.unreadReportsArrivalNotice}</span>
      <button
        class="reports-arrival-btn"
        onclick={() => {
          navigateTo('reports');
          runtimeStore.update((s) => ({ ...s, unreadReportsArrivalNotice: null }));
        }}
      >
        {$t('report.arrival.show_them') || 'Show them'}
      </button>
      <button
        class="reports-arrival-close"
        aria-label="Dismiss notice"
        onclick={() => {
          runtimeStore.update((s) => ({ ...s, unreadReportsArrivalNotice: null }));
        }}
      >
        ✕
      </button>
    </div>
  {/if}

  {#if $runtimeStore.newDecisionArrivalNotice}
    <div class="reports-arrival-banner decisions-arrival-banner" role="status">
      <span>{$runtimeStore.newDecisionArrivalNotice}</span>
      <button
        class="reports-arrival-btn"
        onclick={() => {
          navigateTo('decisions');
          runtimeStore.update((s) => ({ ...s, newDecisionArrivalNotice: null }));
        }}
      >
        {$t('decisions.arrival.show_them') || 'Show decision'}
      </button>
      <button
        class="reports-arrival-close"
        aria-label="Dismiss notice"
        onclick={() => {
          runtimeStore.update((s) => ({ ...s, newDecisionArrivalNotice: null }));
        }}
      >
        ✕
      </button>
    </div>
  {/if}

  {#if $runtimeStore.isDraining}
    <div class="draining-banner" role="status">
      <span class="draining-text">
        {$t('system.stopping.line', {
          count: $runtimeStore.drainInfo?.count ?? 0,
          what: $runtimeStore.drainInfo?.what ?? 'running work',
        })}
      </span>
      <div class="draining-actions">
        <button
          class="draining-btn draining-btn--stop"
          onclick={async () => {
            await apiClient.stopRuntime('now');
          }}
        >
          {$t('system.stopping.stop_now')}
        </button>
        <button
          class="draining-btn draining-btn--withdraw"
          onclick={async () => {
            await apiClient.withdrawStop();
            runtimeStore.update((s) => ({ ...s, isDraining: false, drainInfo: null }));
          }}
        >
          {$t('system.confirm.keep')}
        </button>
      </div>
    </div>
  {/if}

  <header class="app-header">
    <div class="app-header__brand">
      <span class="app-header__logo" aria-hidden="true">✦</span>
      <span class="app-header__title">plan-marshall</span>
      {#if $runtimeStore.isPaired}
        <span class="app-header__machine mono">
          {$t('web.frame.machine', { name: $runtimeStore.machineName })}
        </span>
      {/if}
    </div>

    <div class="app-header__actions">
      {#if $runtimeStore.isPaired}
        <button
          class="header-action-btn"
          aria-label={$t('overview.running.title')}
          onclick={() => navigateTo('overview')}
        >
          {$t('overview.running.title')}
        </button>
        <button
          class="header-action-btn"
          aria-label={$t('project.explorer.title') || 'Projects'}
          onclick={() => navigateTo('projects')}
          data-testid="nav-projects-btn"
        >
          {$t('project.explorer.title') || 'Projects'}
        </button>
        <button
          class="header-action-btn"
          aria-label={$t('web.frame.nav.explorer')}
          onclick={() => navigateTo('workspace')}
        >
          {$t('web.frame.nav.explorer')}
        </button>
        <button
          class="header-action-btn header-action-btn--decisions"
          aria-label={$t('web.frame.nav.decisions')}
          onclick={() => navigateTo('decisions')}
        >
          {$t('web.frame.nav.decisions')}
          {#if $runtimeStore.openDecisionsCount > 0}
            <span
              class="reports-badge decisions-badge"
              aria-label="{$runtimeStore.openDecisionsCount} open"
            >
              {$runtimeStore.openDecisionsCount}
            </span>
          {/if}
        </button>
        <button
          class="header-action-btn header-action-btn--reports"
          aria-label="Reports"
          onclick={() => navigateTo('reports')}
        >
          {$t('web.frame.nav.reports') || 'Reports'}
          {#if $runtimeStore.unreadReportsCount > 0}
            <span class="reports-badge" aria-label="{$runtimeStore.unreadReportsCount} unread">
              {$runtimeStore.unreadReportsCount}
            </span>
          {/if}
        </button>
        <button
          class="header-action-btn"
          aria-label={$t('system.view.title')}
          onclick={() => navigateTo('system')}
        >
          🖥 <span class="header-action-btn__text">{$t('system.view.title')}</span>
        </button>
        <button
          class="header-action-btn"
          aria-label={$t('web.frame.settings')}
          onclick={() => navigateTo('settings')}
        >
          ⚙ <span class="header-action-btn__text">{$t('web.frame.settings')}</span>
        </button>
      {/if}

      <button class="header-action-btn" onclick={toggleLocale} aria-label="Toggle language">
        {$currentLocale === 'en' ? 'DE' : 'EN'}
      </button>

      <button class="header-action-btn" onclick={toggleTheme} aria-label="Toggle light/dark theme">
        {theme === 'light' ? '🌙' : '☀️'}
      </button>
    </div>
  </header>

  {#if $runtimeStore.isPaired}
    <ConnectionStatusBanner />
  {/if}

  <main class="app-body">
    {#if $runtimeStore.isRevoked}
      <RevokedView />
    {:else if !$runtimeStore.isPaired}
      <PairingView />
    {:else if $runtimeStore.isListenerClosed && $runtimeStore.currentRoute === 'listener-off'}
      <ListenerOffView />
    {:else if !$runtimeStore.isOnboarded && $runtimeStore.currentRoute !== 'onboarding' && $runtimeStore.currentRoute !== 'pair' && $runtimeStore.currentRoute !== 'entry'}
      <OnboardingView />
    {:else}
      {@render children?.()}
    {/if}
  </main>
  <div class="sr-only" aria-live="polite">
    {$runtimeStore.quietReportNotice || ''}
    {$runtimeStore.quietDecisionNotice || ''}
  </div>
{/if}

<StartWorkModal
  open={$startModalStore.open}
  projectId={$startModalStore.projectId}
  kind={$startModalStore.kind}
  onClose={closeStartWork}
  onStarted={() => {}}
/>

<LessonSourceModal
  open={$lessonSourceModalStore.open}
  projectId={$lessonSourceModalStore.projectId}
  onClose={closeLessonSource}
  onStarted={() => {}}
/>

<GitSourceIntakeModal
  open={$gitSourceModalStore.open}
  projectId={$gitSourceModalStore.projectId}
  onClose={closeGitSource}
  onCompleted={() => {}}
/>

<style>
  .app-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100vh;
    background-color: var(--color-bg-app);
  }

  .offline-banner {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-2) var(--space-6);
    background-color: var(--color-warning-subtle);
    border-bottom: 1px solid var(--color-warning-border);
    color: var(--color-warning);
    font-size: var(--text-sm);
    font-weight: 500;
  }

  .offline-reconnect {
    text-decoration: underline;
    color: inherit;
    font-weight: 600;
  }

  .app-header {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
    justify-content: space-between;
    align-items: center;
    padding: var(--space-3) var(--space-6);
    background-color: var(--color-bg-surface);
    border-bottom: 1px solid var(--color-border);
  }

  .app-header__brand {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  .app-header__logo {
    font-size: var(--text-lg);
    color: var(--color-primary);
  }

  .app-header__title {
    font-size: var(--text-base);
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .app-header__machine {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    padding: 2px 8px;
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-sm);
  }

  .app-header__actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .header-action-btn {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    padding: var(--space-1) var(--space-3);
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    border-radius: var(--radius-sm);
    transition: background-color 0.15s;
  }

  .header-action-btn:hover {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
  }

  .app-body {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .reports-arrival-banner {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-3);
    padding: var(--space-2) var(--space-4);
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
    font-size: var(--text-sm);
    font-weight: 500;
    border-bottom: 1px solid var(--color-border);
  }

  .draining-banner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: var(--space-3);
    padding: var(--space-2) var(--space-4);
    background-color: var(--color-warning-subtle, #fef3c7);
    color: var(--color-warning-text, #92400e);
    font-size: var(--text-sm);
    font-weight: 500;
    border-bottom: 1px solid var(--color-border);
  }

  .draining-actions {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .draining-btn {
    padding: var(--space-1) var(--space-3);
    font-size: var(--text-sm);
    font-weight: 600;
    border-radius: var(--radius-sm);
    cursor: pointer;
    border: 1px solid currentColor;
    background: transparent;
  }

  .draining-btn--stop {
    background-color: var(--color-danger, #ef4444);
    color: #ffffff;
    border-color: transparent;
  }

  .draining-btn--withdraw {
    background-color: transparent;
    color: inherit;
  }

  .reports-arrival-btn {
    background: none;
    border: none;
    color: var(--color-primary);
    text-decoration: underline;
    font-size: var(--text-sm);
    font-weight: 600;
    cursor: pointer;
    padding: 0;
  }

  .reports-arrival-close {
    background: none;
    border: none;
    color: var(--color-text-muted);
    font-size: var(--text-sm);
    cursor: pointer;
    padding: 0 var(--space-1);
    margin-left: var(--space-2);
  }

  .reports-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 1.25rem;
    height: 1.25rem;
    padding: 0 0.35rem;
    border-radius: var(--radius-full);
    background-color: var(--color-primary);
    color: var(--color-primary-text);
    font-size: 0.7rem;
    font-weight: 700;
    margin-left: var(--space-1);
  }

  .sr-only {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }

  @media (max-width: 640px) {
    .header-action-btn__text {
      display: none;
    }
  }
</style>
