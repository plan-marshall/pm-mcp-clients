<script lang="ts">
  import '../styles/tokens.css';
  import '../styles/base.css';
  import { onMount } from 'svelte';
  import { t, setLocale, currentLocale, initI18n } from '../lib/i18n';
  import { runtimeStore, initRuntime, navigateTo } from '../lib/stores/runtime';
  import Spinner from '../lib/components/Spinner.svelte';

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

    await initI18n();
    await initRuntime();
  });

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
          aria-label={$t('web.frame.nav.explorer')}
          onclick={() => navigateTo('workspace')}
        >
          {$t('web.frame.nav.explorer')}
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

  <main class="app-body">
    {@render children?.()}
  </main>
{/if}

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

  @media (max-width: 640px) {
    .header-action-btn__text {
      display: none;
    }
  }
</style>
