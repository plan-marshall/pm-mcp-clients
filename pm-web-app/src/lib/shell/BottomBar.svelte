<script lang="ts">
  import { onMount } from 'svelte';
  import { runtimeStore, navigateTo } from '../stores/runtime';
  import { currentLocale, setLocale, t } from '../i18n';
  import { THEME_CHOICES, type ThemeChoice } from '../theme';
  import { RAIL_GROUPS, BOTTOM_ITEMS, activeNavRoute, type ShellNavItem } from './nav';
  import Dialog from '../components/Dialog.svelte';
  import Icon from '../components/Icon.svelte';

  interface Props {
    /** The theme this browser chose. */
    themeChoice: ThemeChoice;
    /** Applies a theme the person picked. */
    onChooseTheme: (choice: ThemeChoice) => void;
  }

  /**
   * The phone's bottom bar (UX-P-02): Running, Decisions, Reports and "More".
   *
   * Below 768 px the rail has no room, so the four destinations a person opens most stand at the
   * foot of the page; "More" opens a sheet with every destination, the same three groups the rail
   * carries, and the language and theme controls the rail's foot offers on a desktop. Each item
   * keeps a visible label: a phone has no hover for a tooltip.
   */
  let { themeChoice, onChooseTheme }: Props = $props();

  const active = $derived(activeNavRoute($runtimeStore.currentRoute));

  /** The same condition the rail uses: a destination is offered only where it can open. */
  const canNavigate = $derived(
    $runtimeStore.isPaired &&
      !$runtimeStore.isRevoked &&
      $runtimeStore.isOnboarded &&
      !($runtimeStore.isListenerClosed && $runtimeStore.currentRoute === 'listener-off'),
  );

  /** Whether the sheet of the remaining destinations is open. */
  let sheetOpen = $state(false);

  // The sheet is the phone's; a window that grows past the breakpoint shows the rail instead, so
  // an open sheet would sit over a frame that no longer has the bar it came from.
  onMount(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return;
    }
    const desktop = window.matchMedia('(min-width: 768px)');
    const closeOnDesktop = (): void => {
      if (desktop.matches) {
        sheetOpen = false;
      }
    };
    desktop.addEventListener('change', closeOnDesktop);
    return () => desktop.removeEventListener('change', closeOnDesktop);
  });

  /**
   * Opens a destination and closes the sheet.
   *
   * @param item the destination
   */
  function open(item: ShellNavItem): void {
    sheetOpen = false;
    navigateTo(item.route);
  }
</script>

{#if canNavigate}
  <nav class="bottom-bar" data-testid="bottom-bar" aria-label={$t('web.frame.bottom.label')}>
    {#each BOTTOM_ITEMS as item (item.id)}
      <button
        type="button"
        class="bottom-bar__item"
        class:bottom-bar__item--active={active === item.route}
        data-testid={`bottom-${item.id}`}
        aria-current={active === item.route ? 'page' : undefined}
        onclick={() => open(item)}
      >
        <Icon name={item.icon} size={20} />
        <span class="bottom-bar__label">{$t(item.labelKey)}</span>
      </button>
    {/each}

    <button
      type="button"
      class="bottom-bar__item"
      data-testid="bottom-more"
      aria-haspopup="dialog"
      aria-expanded={sheetOpen}
      onclick={() => (sheetOpen = true)}
    >
      <Icon name="act.more" size={20} />
      <span class="bottom-bar__label">{$t('web.frame.bottom.more')}</span>
    </button>
  </nav>

  <Dialog open={sheetOpen} title={$t('web.frame.bottom.title')} onClose={() => (sheetOpen = false)}>
    <div class="bottom-sheet">
      {#each RAIL_GROUPS as group (group.id)}
        <section class="bottom-sheet__group" data-testid={`sheet-group-${group.id}`}>
          <h3 class="bottom-sheet__title">{$t(group.labelKey)}</h3>
          <ul class="bottom-sheet__list">
            {#each group.items as item (item.id)}
              <li>
                <button
                  type="button"
                  class="bottom-sheet__item"
                  data-testid={`sheet-${item.id}`}
                  aria-current={active === item.route ? 'page' : undefined}
                  onclick={() => open(item)}
                >
                  <Icon name={item.icon} size={20} />
                  <span>{$t(item.labelKey)}</span>
                </button>
              </li>
            {/each}
          </ul>
        </section>
      {/each}

      <section class="bottom-sheet__group" data-testid="sheet-group-frame">
        <h3 class="bottom-sheet__title">{$t('web.frame.bottom.frame')}</h3>
        <div class="bottom-sheet__controls">
          <button
            type="button"
            class="bottom-sheet__control"
            data-testid="sheet-language-toggle"
            aria-label={$t('web.frame.toggle_language')}
            onclick={() => setLocale($currentLocale === 'en' ? 'de' : 'en')}
          >
            {$currentLocale === 'en' ? 'DE' : 'EN'}
          </button>

          <fieldset class="sheet-theme">
            <legend class="sr-only">{$t('web.frame.theme.label')}</legend>
            {#each THEME_CHOICES as choice (choice)}
              <input
                class="sheet-theme__input"
                type="radio"
                name="pm-theme-sheet"
                id={`sheet-theme-${choice}`}
                value={choice}
                checked={themeChoice === choice}
                onchange={() => onChooseTheme(choice)}
              />
              <label
                class="sheet-theme__option"
                for={`sheet-theme-${choice}`}
                data-testid={`sheet-theme-${choice}`}
              >
                {$t(`web.frame.theme.${choice}`)}
              </label>
            {/each}
          </fieldset>
        </div>
      </section>
    </div>
  </Dialog>
{:else}
  <!-- A phone that cannot open a destination keeps the frame's controls: the rail is hidden and
       the pairing and revoked pages still need the language switch and the theme choice. -->
  <nav
    class="bottom-bar bottom-bar--frame"
    data-testid="bottom-bar"
    aria-label={$t('web.frame.bottom.label')}
  >
    <button
      type="button"
      class="bottom-bar__item"
      data-testid="frame-language-toggle"
      aria-label={$t('web.frame.toggle_language')}
      onclick={() => setLocale($currentLocale === 'en' ? 'de' : 'en')}
    >
      <span class="bottom-bar__label">{$currentLocale === 'en' ? 'DE' : 'EN'}</span>
    </button>

    <fieldset class="sheet-theme">
      <legend class="sr-only">{$t('web.frame.theme.label')}</legend>
      {#each THEME_CHOICES as choice (choice)}
        <input
          class="sheet-theme__input"
          type="radio"
          name="pm-theme-frame"
          id={`frame-theme-${choice}`}
          value={choice}
          checked={themeChoice === choice}
          onchange={() => onChooseTheme(choice)}
        />
        <label
          class="sheet-theme__option"
          for={`frame-theme-${choice}`}
          data-testid={`frame-theme-${choice}`}
        >
          {$t(`web.frame.theme.${choice}`)}
        </label>
      {/each}
    </fieldset>
  </nav>
{/if}

<style>
  .bottom-bar {
    display: none;
  }

  .bottom-sheet {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .bottom-sheet__group {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .bottom-sheet__title {
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-muted);
  }

  .bottom-sheet__list {
    display: flex;
    flex-direction: column;
    gap: 1px;
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .bottom-sheet__item {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    inline-size: 100%;
    min-block-size: 44px;
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-sm);
    font-size: var(--text-base);
    color: var(--color-text-secondary);
    text-align: start;
  }

  .bottom-sheet__item:hover,
  .bottom-sheet__item[aria-current='page'] {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
  }

  .bottom-sheet__controls {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .bottom-sheet__control {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-block-size: 44px;
    padding: var(--space-1) var(--space-3);
    border-radius: var(--radius-sm);
    border: 1px solid var(--color-border-strong);
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .sheet-theme {
    display: flex;
    flex-wrap: wrap;
    gap: 1px;
    margin: 0;
    padding: 1px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
  }

  .sheet-theme__input {
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

  .sheet-theme__option {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 1 1 0;
    min-block-size: 44px;
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    border-radius: calc(var(--radius-sm) - 1px);
    cursor: pointer;
  }

  .sheet-theme__input:checked + .sheet-theme__option {
    background-color: var(--color-brand-subtle);
    color: var(--color-brand);
    font-weight: 600;
  }

  @media (max-width: 767px) {
    .bottom-bar {
      position: sticky;
      inset-block-end: 0;
      z-index: 20;
      display: flex;
      align-items: stretch;
      justify-content: space-around;
      gap: 1px;
      padding-block: var(--space-1);
      padding-inline: var(--space-1);
      background-color: var(--color-bg-surface);
      border-block-start: 1px solid var(--color-border);
    }

    .bottom-bar--frame {
      justify-content: space-between;
      padding-inline: var(--space-3);
    }

    .bottom-bar--frame .sheet-theme {
      flex: 0 1 auto;
    }

    .bottom-bar__item {
      display: inline-flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 2px;
      flex: 1 1 0;
      min-inline-size: 0;
      min-block-size: 44px;
      padding: var(--space-1);
      border-radius: var(--radius-sm);
      font-size: var(--text-xs);
      color: var(--color-text-secondary);
    }

    .bottom-bar__item--active {
      color: var(--color-brand);
      font-weight: 600;
    }

    .bottom-bar__label {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-inline-size: 100%;
    }
  }
</style>
