<script lang="ts">
  import '../styles/tokens.css';
  import '../styles/base.css';
  import { onMount, onDestroy } from 'svelte';
  import { t, initI18n, messagesStore } from '../lib/i18n';
  import {
    runtimeStore,
    initRuntime,
    adoptRouteFromLocation,
    routeParamsOf,
    syncUrl,
  } from '../lib/stores/runtime';
  import { apiClient } from '../lib/api/client';
  import { pathForRoute, ROUTE_TITLES, type AppRoute } from '../lib/routes';
  import {
    applyTheme,
    readThemeChoice,
    watchSystemTheme,
    writeThemeChoice,
    type ThemeChoice,
  } from '../lib/theme';
  import { installAdvisoryTitles } from '../lib/utils/advisoryTitle';
  import { applyFavicon } from '../lib/favicon';
  import Spinner from '../lib/components/Spinner.svelte';
  import PairingView from '../lib/views/PairingView.svelte';
  import OnboardingView from '../lib/views/OnboardingView.svelte';
  import ListenerOffView from '../lib/views/ListenerOffView.svelte';
  import RevokedView from '../lib/views/RevokedView.svelte';
  import LessonSourceModal from '../lib/components/LessonSourceModal.svelte';
  import GitSourceIntakeModal from '../lib/components/GitSourceIntakeModal.svelte';
  import AppShell from '../lib/shell/AppShell.svelte';
  import { lessonSourceModalStore, closeLessonSource } from '../lib/stores/lessonSource';
  import { gitSourceModalStore, closeGitSource } from '../lib/stores/gitSource';

  interface Props {
    children?: import('svelte').Snippet;
  }

  let { children }: Props = $props();

  // The development page draws components, not a view of the app: it is outside the route model,
  // so the shell neither rewrites its address nor replaces it with the stored route. The branch is
  // compiled out of a production build, where the page's route does not exist either, and its
  // boundary check keeps `/device` or `/developer` out of it.
  const devRoute =
    import.meta.env.DEV &&
    typeof window !== 'undefined' &&
    (window.location.pathname === '/dev' || window.location.pathname.startsWith('/dev/'));

  // The theme this browser chose. `app.html` already applied it before the first paint; the
  // control reads the same choice so the pressed option and the document agree.
  let themeChoice = $state<ThemeChoice>(readThemeChoice());
  // The first write settles the address bar on the route the store chose; every later one is a
  // history entry, so the browser's Back returns to the previous view.
  let addressBarSettled = false;

  onMount(() => {
    // Native buttons and the views rendered later get their hover advisory from the observer.
    const stopAdvisoryTitles = installAdvisoryTitles();
    return () => stopAdvisoryTitles();
  });

  onMount(async () => {
    // The theme was set before the first paint; applying it again keeps the document and the
    // stored choice in step after hydration, and the listener below lets `system` follow the
    // machine while the app runs.
    applyTheme(themeChoice);

    // The address bar is the route: a reload, a bookmark, or the browser's back returns here.
    adoptRouteFromLocation();
    const onPop = () => adoptRouteFromLocation();
    window.addEventListener('popstate', onPop);
    onDestroy(() => window.removeEventListener('popstate', onPop));
    onDestroy(
      watchSystemTheme(() => {
        if (themeChoice === 'system') applyTheme('system');
      }),
    );

    // Initialize i18n before the runtime, so a notice the runtime composes at start-up reads
    // from the catalogue. A slow catalogue must not block start-up: the race bounds the wait.
    await Promise.race([initI18n(), new Promise<void>((resolve) => setTimeout(resolve, 2000))]);
    await initRuntime();
  });

  // The address bar follows the route the store selects, so a reload opens the same page.
  // It never rewrites a URL that still carries a pairing code in its fragment: that code is
  // redeemed first, and only then does the app move on.
  $effect(() => {
    if ($runtimeStore.isInitialized && !devRoute) {
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

  /**
   * Applies a theme the person picked and keeps it for the next visit.
   *
   * @param choice the picked theme
   */
  function chooseTheme(choice: ThemeChoice): void {
    themeChoice = choice;
    writeThemeChoice(choice);
    applyTheme(choice);
  }

  // The tab title counts the open decisions, so an operator in another tab sees them without
  // opening the app; the list and the badge carry the same count. The route supplies the key and
  // the name it carries, so no title is ever formatted without its argument.
  $effect(() => {
    if (typeof document === 'undefined') return;
    const count = $runtimeStore.openDecisionsCount;
    const { key, args } = ROUTE_TITLES[$runtimeStore.currentRoute]($runtimeStore);
    // The address bar never shows a raw key: the title waits for the catalogue to arrive.
    if (!$messagesStore[key]) return;
    const base = $t(key, args);
    document.title = count > 0 ? $t('decision.notify.tab_title', { count, title: base }) : base;
  });

  // The tab icon carries the attention dot while items wait; it reads the same count the rail
  // badge and the tab title carry, so the three never disagree.
  $effect(() => {
    applyFavicon($runtimeStore.openDecisionsCount);
  });
</script>

{#if devRoute}
  <main class="app-body">
    {@render children?.()}
  </main>
{:else if !$runtimeStore.isInitialized}
  <div class="app-loading" role="status">
    <Spinner size="3rem" label={$t('web.frame.initializing')} />
  </div>
{:else}
  <AppShell {themeChoice} onChooseTheme={chooseTheme}>
    {#snippet notices()}
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
              data-advisory={$t('system.stopping.stop_now.title')}
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
    {/snippet}

    {#snippet content()}
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
    {/snippet}
  </AppShell>

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
    background-color: var(--color-warning-subtle);
    color: var(--color-warning-text);
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
    background-color: var(--color-danger);
    color: var(--color-danger-contrast);
    border-color: transparent;
  }

  .draining-btn--withdraw {
    background-color: transparent;
    color: inherit;
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
</style>
