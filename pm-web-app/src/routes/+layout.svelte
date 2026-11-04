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
  import ActionBar from '../lib/components/ActionBar.svelte';
  import ConfirmDialog from '../lib/components/ConfirmDialog.svelte';
  import { RUNTIME_ACTS } from '../lib/actions/byState';
  import { lessonSourceModalStore, closeLessonSource } from '../lib/stores/lessonSource';
  import { gitSourceModalStore, closeGitSource } from '../lib/stores/gitSource';

  /** What the drain still runs, the count and the line read from the same list. */
  let drainItems = $derived($runtimeStore.drainInfo?.items ?? []);
  /** The line names at most three, the rest is counted beside them (<<draining>>). */
  let drainNames = $derived(drainItems.slice(0, 3).join(', '));
  let drainMore = $derived(Math.max(0, drainItems.length - 3));
  let drainCount = $derived(drainItems.length || ($runtimeStore.drainInfo?.count ?? 0));

  /** Whether a drain act runs; both acts refuse a second click while it does. */
  let drainingBusy = $state(false);
  /** The refusal of the last drain act, in the banner. */
  let drainingFailure = $state('');
  /** Whether "Stop now" asks for its own short confirmation. */
  let stopNowConfirm = $state(false);

  // A drain that ends while the short confirmation is open leaves nothing to confirm.
  $effect(() => {
    if (!$runtimeStore.isDraining && stopNowConfirm) {
      stopNowConfirm = false;
    }
  });

  /** @param err the refusal a drain act reported @returns its message */
  function refusalOf(err: unknown): string {
    return err instanceof Error ? err.message : String(err);
  }

  /**
   * Runs the act the drain banner reports (UX-P-04: withdraw the stop, or stop now).
   *
   * "Stop now" ends running work, so it asks first (<<draining>>); both acts carry a busy state and
   * report a refusal instead of dropping it.
   *
   * @param id the chosen act
   */
  function chooseDrainingAct(id: string): void {
    if (drainingBusy) {
      return;
    }
    if (id === 'withdraw_stop') {
      void withdrawDrain();
      return;
    }
    if (id === 'stop_now') {
      stopNowConfirm = true;
    }
  }

  /** Withdraws the stop, and says so in the banner when the runtime refuses. */
  async function withdrawDrain(): Promise<void> {
    if (drainingBusy) {
      return;
    }
    drainingBusy = true;
    drainingFailure = '';
    try {
      await apiClient.withdrawStop();
      runtimeStore.update((s) => ({ ...s, isDraining: false, drainInfo: null }));
    } catch (err: unknown) {
      drainingFailure = $t('system.stopping.withdraw_failed', { reason: refusalOf(err) });
    } finally {
      drainingBusy = false;
    }
  }

  /** Turns the drain into the stop now the confirmation asked for. */
  async function confirmStopNow(): Promise<void> {
    if (drainingBusy) {
      return;
    }
    drainingBusy = true;
    drainingFailure = '';
    try {
      await apiClient.stopRuntime('now');
      stopNowConfirm = false;
    } catch (err: unknown) {
      // The refusal belongs in the banner; the short confirmation has done its work.
      stopNowConfirm = false;
      drainingFailure = $t('system.stopping.stop_now_failed', { reason: refusalOf(err) });
    } finally {
      drainingBusy = false;
    }
  }

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
        <div class="draining-banner" role="status" data-testid="draining-banner">
          <span class="draining-text" data-testid="draining-text">
            {$t('system.stopping.line', {
              count: drainCount,
              what: drainItems.length > 0
                ? drainNames
                : ($runtimeStore.drainInfo?.what ?? $t('system.stopping.running_work')),
            })}
          </span>
          {#if drainMore > 0}
            <span class="draining-more" data-testid="draining-more">
              {$t('system.stopping.more', { count: drainMore })}
            </span>
          {/if}
          {#if drainItems.length > 0}
            <ul class="draining-list" data-testid="draining-list">
              {#each drainItems as item, index (index)}
                <li>{item}</li>
              {/each}
            </ul>
          {/if}
          <div class="draining-actions">
            <ActionBar
              acts={RUNTIME_ACTS.draining.map((act) => ({
                id: act.id,
                label: $t(act.labelKey),
                advisory: act.advisoryKey ? $t(act.advisoryKey) : undefined,
                role: act.role,
              }))}
              busy={drainingBusy}
              onAct={chooseDrainingAct}
            />
          </div>
          {#if drainingFailure}
            <p class="draining-failure" data-testid="draining-failure" role="alert">
              {drainingFailure}
            </p>
          {/if}
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

<ConfirmDialog
  open={stopNowConfirm}
  testid="stop-now-confirm"
  title={$t('system.stopping.confirm_now_left', { count: drainCount })}
  consequences={[$t('system.confirm.now_explain')]}
  actionLabel={$t('system.stopping.stop_now')}
  cancelLabel={$t('system.confirm.keep')}
  busy={drainingBusy}
  onConfirm={confirmStopNow}
  onClose={() => (stopNowConfirm = false)}
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

  .draining-list {
    margin: 0;
    padding-inline-start: var(--space-5);
    font-weight: 400;
    color: var(--color-text-secondary);
  }

  .draining-more {
    font-weight: 400;
    color: var(--color-text-secondary);
  }

  .draining-failure {
    flex-basis: 100%;
    margin: 0;
    font-weight: 400;
    color: var(--color-danger);
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
