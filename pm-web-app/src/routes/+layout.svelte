<script lang="ts">
  import '../styles/tokens.css';
  import '../styles/base.css';
  import { onMount, onDestroy } from 'svelte';
  import { t, initI18n, messagesStore } from '../lib/i18n';
  import { actFailureReason, isClientError } from '../lib/api/errors';
  import {
    runtimeStore,
    initRuntime,
    adoptRouteFromLocation,
    advanceLifecycleEpoch,
    lifecycleEpochOf,
    lifecycleFieldsOf,
    routeParamsOf,
    stopRuntimeRecovery,
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
  import UnreachableView from '../lib/views/UnreachableView.svelte';
  import { holdFigure } from '../lib/stores/heldFigure';
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

  // A drain that ends leaves nothing to confirm, and its refusal belonged to that drain alone.
  $effect(() => {
    if (!$runtimeStore.isDraining) {
      stopNowConfirm = false;
      drainingFailure = '';
    }
  });

  /** @param err the refusal a drain act reported @returns the sentence the operator reads */
  function refusalOf(err: unknown): string {
    return actFailureReason(err, $t('system.action.failed'));
  }

  /**
   * @param err the failure a drain act reported
   * @returns whether the act's outcome is unknown: the request neither confirmed nor refused it,
   *   because it gave up on its deadline or never reached the runtime
   */
  function outcomeUnknown(err: unknown): boolean {
    if (isClientError(err)) {
      return err.code === 'timeout' || err.code === 'network_offline';
    }
    const name = (err as Error)?.name;
    const message = (err as Error)?.message;
    return name === 'TimeoutError' || name === 'AbortError' || message === 'network_offline';
  }

  /** How long the read after an unknown outcome may wait for the runtime. */
  const STATE_REFRESH_TIMEOUT_MS = 5_000;

  /**
   * Reads the lifecycle again, so the banner follows what the machine really does.
   *
   * A lifecycle event that arrives while the read is in flight is newer than the answer: the
   * snapshot is dropped then, rather than overwriting what the event said.
   */
  async function refreshLifecycle(): Promise<void> {
    const epoch = lifecycleEpochOf();
    try {
      const status = await apiClient.getStatus(AbortSignal.timeout(STATE_REFRESH_TIMEOUT_MS));
      if (epoch !== lifecycleEpochOf()) return;
      runtimeStore.update((s) => ({ ...s, ...lifecycleFieldsOf(status) }));
    } catch {
      // The outcome stays unknown; the banner keeps saying so until the next event.
    }
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
      // The client's own request limit bounds the act: a DELETE the runtime never answers must let
      // go of the bar.
      await apiClient.withdrawStop();
      advanceLifecycleEpoch();
      runtimeStore.update((s) => ({ ...s, isDraining: false, drainInfo: null }));
    } catch (err: unknown) {
      const unknown = outcomeUnknown(err);
      drainingFailure = unknown
        ? $t('system.stopping.withdraw_unknown')
        : $t('system.stopping.withdraw_failed', { reason: refusalOf(err) });
      // A response that never arrived may still be a withdrawal: read what the runtime does.
      if (unknown) void refreshLifecycle();
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
      const epoch = lifecycleEpochOf();
      const answer = await apiClient.stopRuntime('now');
      stopNowConfirm = false;
      // A lifecycle event that arrived while the request was pending is newer than this answer.
      const superseded = epoch !== lifecycleEpochOf();
      advanceLifecycleEpoch();
      if (!superseded) {
        // The stop ends the drain; the lifecycle event may be missed on a dropped stream. A drain that
        // ends in a restart answers `running`: the runtime is not stopped (as `runtime.stopping`).
        const isStopped = answer.lifecycle_state !== 'running';
        runtimeStore.update((s) => ({
          ...s,
          isDraining: false,
          drainInfo: null,
          isStopped,
          stoppedInfo: null,
        }));
      }
    } catch (err: unknown) {
      // The refusal belongs in the banner; the short confirmation has done its work.
      stopNowConfirm = false;
      const unknown = outcomeUnknown(err);
      drainingFailure = unknown
        ? $t('system.stopping.stop_now_unknown')
        : $t('system.stopping.stop_now_failed', { reason: refusalOf(err) });
      // A response that never arrived is not a stop: read what the runtime really does.
      if (unknown) void refreshLifecycle();
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
    // A start that fails leaves the store retrying the initialization until one succeeds; the
    // shell owns that retry for its whole life and ends it with itself, never the page of the
    // lost connection, which the first successful request removes.
    let shellGone = false;
    onDestroy(() => {
      shellGone = true;
      stopRuntimeRecovery();
    });

    // Initialize i18n before the runtime, so a notice the runtime composes at start-up reads
    // from the catalogue. A slow catalogue must not block start-up: the race bounds the wait.
    await Promise.race([initI18n(), new Promise<void>((resolve) => setTimeout(resolve, 2000))]);
    // A shell torn down while the catalogue loaded starts no runtime: nothing would end its retry.
    if (shellGone) return;
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

  // Once the app has started and reaches its origin, it fetches the figure of the disconnected
  // states once and holds it, so those states can draw it after the origin is lost.
  $effect(() => {
    if ($runtimeStore.isInitialized && !$runtimeStore.isOffline) {
      void holdFigure();
    }
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
              {#each drainItems.slice(0, 3) as item, index (index)}
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
      {:else if !$runtimeStore.isOnboarded && !$runtimeStore.isOffline && $runtimeStore.currentRoute !== 'onboarding'}
        <OnboardingView />
      {:else}
        <!-- The runtime that is not reachable covers the open view without replacing it: the view
             stays mounted but hidden, so its state and unsent input survive the reconnection. -->
        {#if $runtimeStore.isOffline}
          <UnreachableView />
        {/if}
        <div class="open-view" hidden={$runtimeStore.isOffline} data-testid="open-view">
          {@render children?.()}
        </div>
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

  /* The wrapper adds no box of its own; while hidden it draws nothing. */
  .open-view {
    display: contents;
  }

  .open-view[hidden] {
    display: none;
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
