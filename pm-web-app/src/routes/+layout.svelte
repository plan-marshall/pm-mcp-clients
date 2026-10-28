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
    openStartPage,
    routeParamsOf,
    syncUrl,
  } from '../lib/stores/runtime';
  import { apiClient, type ConnectionStatus } from '../lib/api/client';
  import { pathForRoute, ROUTE_TITLES, type AppRoute } from '../lib/routes';
  import {
    THEME_CHOICES,
    applyTheme,
    readThemeChoice,
    watchSystemTheme,
    writeThemeChoice,
    type ThemeChoice,
  } from '../lib/theme';
  import { installAdvisoryTitles } from '../lib/utils/advisoryTitle';
  import Spinner from '../lib/components/Spinner.svelte';
  import PairingView from '../lib/views/PairingView.svelte';
  import OnboardingView from '../lib/views/OnboardingView.svelte';
  import ListenerOffView from '../lib/views/ListenerOffView.svelte';
  import RevokedView from '../lib/views/RevokedView.svelte';
  import LessonSourceModal from '../lib/components/LessonSourceModal.svelte';
  import GitSourceIntakeModal from '../lib/components/GitSourceIntakeModal.svelte';
  import BrandMark from '../lib/components/BrandMark.svelte';
  import Icon from '../lib/components/Icon.svelte';
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

  // The connection of this browser to the machine: the listener it reached and how many devices
  // can act on it. The header badge is the only always-on status row; the two non-redundant facts
  // live in its title and a mode marker.
  const loopbackHosts = ['127.0.0.1', 'localhost', '::1', '[::1]'];
  const CONNECTION_HEARTBEAT_MS = 30_000;
  let connectionStatus = $state<ConnectionStatus | null>(null);

  const isLoopback =
    typeof window !== 'undefined' && loopbackHosts.includes(window.location.hostname);
  const connectionMode = $derived(isLoopback ? 'loopback' : 'lan');
  const connectionTitle = $derived.by(() => {
    const detail = $t(
      isLoopback ? 'entry.connection.loopback.detail' : 'entry.connection.lan.detail',
    );
    if (!connectionStatus) {
      return detail;
    }
    const devices = $t('entry.connection.devices', {
      count: connectionStatus.paired_devices_count,
    });
    return `${detail} · ${devices}`;
  });

  // The brand is inert while the app has no usable page to open; the start page is reachable only
  // once the browser is paired and not on the listener-off page.
  const canGoHome = $derived(
    $runtimeStore.isPaired &&
      !$runtimeStore.isRevoked &&
      !($runtimeStore.isListenerClosed && $runtimeStore.currentRoute === 'listener-off'),
  );

  onMount(() => {
    // Native buttons and the views rendered later get their hover advisory from the observer.
    const stopAdvisoryTitles = installAdvisoryTitles();
    return () => stopAdvisoryTitles();
  });

  // The connection status follows the session: read on mount, on a listener/device event, and on
  // a heartbeat, so a machine that went away or came back is seen without a reload. Only the two
  // fields the heartbeat needs are dependencies: an unrelated store update (the offline flag, a
  // decision count) must not tear down and re-open the event subscription.
  const connectionActive = $derived($runtimeStore.isInitialized && $runtimeStore.isPaired);

  $effect(() => {
    if (!connectionActive) {
      return;
    }
    let mounted = true;
    const load = async (): Promise<void> => {
      try {
        const next = await apiClient.getConnectionStatus();
        if (mounted) connectionStatus = next;
      } catch {
        if (mounted) connectionStatus = null;
      }
    };
    void load();
    const unsubscribe = apiClient.subscribeEvents((event) => {
      if (event === 'listener.changed' || event === 'device.paired' || event === 'device.revoked') {
        void load();
      }
    });
    const heartbeat = setInterval(() => void load(), CONNECTION_HEARTBEAT_MS);
    return () => {
      mounted = false;
      unsubscribe();
      clearInterval(heartbeat);
    };
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

  function toggleLocale() {
    setLocale($currentLocale === 'en' ? 'de' : 'en');
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

  <header class="app-header">
    <div class="app-header__brand">
      {#if canGoHome}
        <button
          type="button"
          class="app-header__home"
          aria-label={$t('web.frame.home')}
          data-advisory={$t('web.frame.home.title')}
          data-testid="brand-home"
          onclick={() => void openStartPage()}
        >
          <span class="app-header__logo"><BrandMark size={24} /></span>
          <span class="app-header__title">{$t('web.frame.brand')}</span>
        </button>
      {:else}
        <span class="app-header__home app-header__home--inert" data-testid="brand-inert">
          <span class="app-header__logo"><BrandMark size={24} /></span>
          <span class="app-header__title">{$t('web.frame.brand')}</span>
        </span>
      {/if}
      {#if $runtimeStore.isPaired}
        <span
          class="app-header__machine mono"
          title={connectionTitle}
          data-connection-mode={connectionMode}
          data-testid="header-machine"
        >
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
          aria-label={$t('web.frame.nav.designer')}
          onclick={() => navigateTo('designer')}
          data-testid="nav-designer-btn"
        >
          <Icon name="nav.designer" size={16} />
          <span class="header-action-btn__text">{$t('web.frame.nav.designer')}</span>
        </button>
        <button
          class="header-action-btn"
          aria-label={$t('system.view.title')}
          onclick={() => navigateTo('system')}
        >
          <Icon name="nav.system" size={16} />
          <span class="header-action-btn__text">{$t('system.view.title')}</span>
        </button>
        <button
          class="header-action-btn"
          aria-label={$t('web.frame.settings')}
          data-advisory={$t('web.frame.settings.title')}
          onclick={() => navigateTo('settings')}
        >
          <Icon name="nav.settings" size={16} />
          <span class="header-action-btn__text">{$t('web.frame.settings')}</span>
        </button>
      {/if}

      <button
        class="header-action-btn"
        onclick={toggleLocale}
        aria-label={$t('web.frame.toggle_language')}
        data-advisory={$t('web.frame.toggle_language.title')}
      >
        {$currentLocale === 'en' ? 'DE' : 'EN'}
      </button>

      <fieldset class="theme-choice">
        <legend class="sr-only">{$t('web.frame.theme.label')}</legend>
        {#each THEME_CHOICES as choice (choice)}
          <input
            class="theme-choice__input"
            type="radio"
            name="pm-theme"
            id={`theme-${choice}`}
            value={choice}
            checked={themeChoice === choice}
            onchange={() => chooseTheme(choice)}
          />
          <label class="theme-choice__option" for={`theme-${choice}`} data-testid={`theme-${choice}`}>
            {$t(`web.frame.theme.${choice}`)}
          </label>
        {/each}
      </fieldset>
    </div>
  </header>

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

  .app-header__home {
    display: inline-flex;
    align-items: center;
    gap: var(--space-3);
    padding: 0;
    border: 0;
    background: transparent;
    color: inherit;
    font: inherit;
    cursor: pointer;
    border-radius: var(--radius-sm);
  }

  .app-header__home:hover {
    text-decoration: underline;
  }

  .app-header__home--inert {
    cursor: default;
  }

  .app-header__home--inert:hover {
    text-decoration: none;
  }

  .app-header__logo {
    display: inline-flex;
    align-items: center;
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
    border-left: 3px solid var(--color-border);
  }

  /* The mode marker: the network listener is the one that is not implied by the loopback page. */
  .app-header__machine[data-connection-mode='lan'] {
    color: var(--color-warning);
    background-color: var(--color-warning-subtle);
    border-left-color: var(--color-warning);
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

  /* The three theme choices are one control: a radio group, so the frame says they belong
     together and the radio role says exactly one is in force. */
  .theme-choice {
    display: inline-flex;
    align-items: center;
    gap: 1px;
    margin: 0;
    padding: 1px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
  }

  /* The radio itself stays reachable for the keyboard and the screen reader; the label is what a
     person sees and clicks. */
  .theme-choice__input {
    position: absolute;
    width: 1px;
    height: 1px;
    margin: -1px;
    padding: 0;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }

  .theme-choice__option {
    display: inline-flex;
    align-items: center;
    padding: var(--space-1) var(--space-3);
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    border-radius: calc(var(--radius-sm) - 1px);
    cursor: pointer;
    transition: background-color 0.15s;
  }

  .theme-choice__option:hover {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
  }

  .theme-choice__input:checked + .theme-choice__option {
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
    font-weight: 600;
  }

  .theme-choice__input:focus-visible + .theme-choice__option {
    box-shadow: var(--focus-ring);
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
    /* A touch target stays 44 px on the phone, as every header control does. */
    .theme-choice__option {
      min-height: 2.75rem;
    }

    .header-action-btn__text {
      display: none;
    }
  }
</style>
