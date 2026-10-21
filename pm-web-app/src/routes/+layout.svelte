<script lang="ts">
  import '../styles/tokens.css';
  import '../styles/base.css';
  import { onMount, onDestroy } from 'svelte';
  import { t, setLocale, currentLocale, initI18n, messagesStore } from '../lib/i18n';
  import {
    runtimeStore,
    initRuntime,
    navigateTo,
    adoptRouteFromLocation,
    routeParamsOf,
    syncUrl,
  } from '../lib/stores/runtime';
  import { apiClient } from '../lib/api/client';
  import { pathForRoute, type AppRoute } from '../lib/routes';
  import Spinner from '../lib/components/Spinner.svelte';
  import ConnectionStatusBanner from '../lib/components/ConnectionStatusBanner.svelte';
  import PairingView from '../lib/views/PairingView.svelte';
  import OnboardingView from '../lib/views/OnboardingView.svelte';
  import ListenerOffView from '../lib/views/ListenerOffView.svelte';
  import RevokedView from '../lib/views/RevokedView.svelte';
  import LessonSourceModal from '../lib/components/LessonSourceModal.svelte';
  import GitSourceIntakeModal from '../lib/components/GitSourceIntakeModal.svelte';
  import { lessonSourceModalStore, closeLessonSource } from '../lib/stores/lessonSource';
  import { gitSourceModalStore, closeGitSource } from '../lib/stores/gitSource';

  interface Props {
    children?: import('svelte').Snippet;
  }

  let { children }: Props = $props();

  let theme = $state<'light' | 'dark'>('light');
  // The first write settles the address bar on the route the store chose; every later one is a
  // history entry, so the browser's Back returns to the previous view.
  let addressBarSettled = false;

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

    // Initialize i18n before the runtime, so a notice the runtime composes at start-up reads
    // from the catalogue. A slow catalogue must not block start-up: the race bounds the wait.
    await Promise.race([initI18n(), new Promise<void>((resolve) => setTimeout(resolve, 2000))]);
    await initRuntime();
  });

  // The address bar follows the route the store selects, so a reload opens the same page.
  // It never rewrites a URL that still carries a pairing code in its fragment: that code is
  // redeemed first, and only then does the app move on.
  $effect(() => {
    if ($runtimeStore.isInitialized) {
      syncPath($runtimeStore.currentRoute);
    }
  });

  function syncPath(route: AppRoute): void {
    if (typeof window === 'undefined') return;
    // Never rewrite a URL that still carries a pairing code in its fragment: the code is redeemed
    // first, and only then does the app move on.
    if (window.location.hash.includes('code=')) return;
    const path = pathForRoute(route, routeParamsOf($runtimeStore));
    if (!path) return;
    // The URL the popstate handler just adopted is already in the address bar; syncUrl leaves it.
    syncUrl(path, !addressBarSettled);
    addressBarSettled = true;
  }

  function toggleTheme() {
    theme = theme === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', theme);
  }

  function toggleLocale() {
    setLocale($currentLocale === 'en' ? 'de' : 'en');
  }

  /** The catalogue key of each route's tab title; the type makes a route impossible to miss. */
  const ROUTE_TITLE_KEYS: Record<AppRoute, string> = {
    pair: 'web.pair.page.title',
    entry: 'start.plan.title',
    onboarding: 'onboard.entry.title',
    'project-onboarding': 'project.explorer.title',
    settings: 'web.settings.page.title',
    'machine-settings': 'web.settings.page.title',
    projects: 'project.explorer.title',
    overview: 'overview.running.title',
    plan: 'plan.view.title',
    epic: 'epic.view.title',
    decomposition: 'epic.view.title',
    decisions: 'decisions.list.title',
    reports: 'report.list.title',
    system: 'system.view.title',
    cleanup: 'system.view.title',
    designer: 'designer.view.title',
    start: 'start.plan.title',
    revoked: 'devices.revoked.title',
    'listener-off': 'web.entry.lan_off.title',
  };

  /**
   * The title the address bar carries for the route, before the open decisions are counted.
   *
   * @param route the route shown
   * @returns the catalogue title of that route
   */
  function routeTitle(route: AppRoute): string {
    return $t(ROUTE_TITLE_KEYS[route]);
  }

  // The tab title counts the open decisions, so an operator in another tab sees them without
  // opening the app; the list and the badge carry the same count.
  $effect(() => {
    if (typeof document === 'undefined') return;
    const count = $runtimeStore.openDecisionsCount;
    const key = ROUTE_TITLE_KEYS[$runtimeStore.currentRoute];
    // The address bar never shows a raw key: the title waits for the catalogue to arrive.
    if (!key || !$messagesStore[key]) return;
    const base = routeTitle($runtimeStore.currentRoute);
    document.title = count > 0 ? $t('decision.notify.tab_title', { count, title: base }) : base;
  });
</script>

{#if !$runtimeStore.isInitialized}
  <div class="app-loading" role="status">
    <Spinner size="3rem" label={$t('web.frame.initializing')} />
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

  {#if $runtimeStore.isDraining}
    <div class="draining-banner" role="status">
      <span class="draining-text">
        {$t('system.stopping.line', {
          count: $runtimeStore.drainInfo?.count ?? 0,
          what: $runtimeStore.drainInfo?.what ?? $t('system.stopping.running_work'),
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
      <span class="app-header__title">{$t('web.frame.brand')}</span>
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
          aria-label={$t('project.explorer.title')}
          onclick={() => navigateTo('projects')}
          data-testid="nav-projects-btn"
        >
          {$t('project.explorer.title')}
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
              aria-label={$t('decisions.list.open', {
                count: $runtimeStore.openDecisionsCount,
              })}
            >
              {$runtimeStore.openDecisionsCount}
            </span>
          {/if}
        </button>
        <button
          class="header-action-btn header-action-btn--reports"
          aria-label={$t('web.frame.nav.reports')}
          onclick={() => navigateTo('reports')}
        >
          {$t('web.frame.nav.reports')}
          {#if $runtimeStore.unreadReportsCount > 0}
            <span
              class="reports-badge"
              aria-label={$t('report.list.unread', {
                count: $runtimeStore.unreadReportsCount,
              })}
            >
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

      <button
        class="header-action-btn"
        onclick={toggleLocale}
        aria-label={$t('web.frame.toggle_language')}
      >
        {$currentLocale === 'en' ? 'DE' : 'EN'}
      </button>

      <button
        class="header-action-btn"
        onclick={toggleTheme}
        aria-label={$t('web.frame.toggle_theme')}
      >
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
    {:else if !$runtimeStore.isOnboarded && $runtimeStore.currentRoute !== 'onboarding'}
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
