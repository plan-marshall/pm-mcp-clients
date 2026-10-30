<script lang="ts">
  import { runtimeStore, navigateTo, openStartPage } from '../stores/runtime';
  import { currentLocale, setLocale, t } from '../i18n';
  import { THEME_CHOICES, type ThemeChoice } from '../theme';
  import { RAIL_GROUPS, activeNavRoute, type ShellNavItem } from './nav';
  import BrandMark from '../components/BrandMark.svelte';
  import Icon from '../components/Icon.svelte';

  interface Props {
    /** Whether the rail shows icons only. */
    collapsed: boolean;
    /** Reports the collapse the rail asks for. */
    onToggle: () => void;
    /** The theme this browser chose. */
    themeChoice: ThemeChoice;
    /** Applies a theme the person picked. */
    onChooseTheme: (choice: ThemeChoice) => void;
  }

  let { collapsed, onToggle, themeChoice, onChooseTheme }: Props = $props();

  /** The brand is inert while the app has no usable page to open. */
  const canGoHome = $derived(
    $runtimeStore.isPaired &&
      !$runtimeStore.isRevoked &&
      !($runtimeStore.isListenerClosed && $runtimeStore.currentRoute === 'listener-off'),
  );

  /** The destination that stays selected for the shown route. */
  const active = $derived(activeNavRoute($runtimeStore.currentRoute));

  /**
   * Whether the destinations may be opened.
   *
   * The onboarding shows itself until the machine is onboarded, and a browser that lost the
   * network listener waits on its own page: a destination there would only rewrite the address.
   */
  const canNavigate = $derived(
    $runtimeStore.isPaired &&
      !$runtimeStore.isRevoked &&
      $runtimeStore.isOnboarded &&
      !($runtimeStore.isListenerClosed && $runtimeStore.currentRoute === 'listener-off'),
  );

  /**
   * The count a destination carries, or `0` when it carries none.
   *
   * @param item the destination
   * @returns the open decisions or the unread reports
   */
  function countOf(item: ShellNavItem): number {
    if (item.count === 'decisions') {
      return $runtimeStore.openDecisionsCount;
    }
    if (item.count === 'reports') {
      return $runtimeStore.unreadReportsCount;
    }
    return 0;
  }

  /**
   * The accessible name of a destination's count.
   *
   * @param item the destination
   * @param count the count it shows
   * @returns the catalogue text the badge carries
   */
  function countLabel(item: ShellNavItem, count: number): string {
    return item.count === 'decisions'
      ? $t('decisions.list.open', { count })
      : $t('report.list.unread', { count });
  }
</script>

<nav
  class="nav-rail"
  class:nav-rail--collapsed={collapsed}
  data-testid="nav-rail"
  aria-label={$t('web.frame.rail.label')}
>
  <div class="nav-rail__head">
    {#if canGoHome}
      <button
        type="button"
        class="nav-rail__brand"
        aria-label={$t('web.frame.home')}
        data-advisory={$t('web.frame.home.title')}
        data-testid="brand-home"
        onclick={() => void openStartPage()}
      >
        <BrandMark size={24} plate />
        <span class="nav-rail__title">{$t('web.frame.brand')}</span>
      </button>
    {:else}
      <span class="nav-rail__brand nav-rail__brand--inert" data-testid="brand-inert">
        <BrandMark size={24} plate />
        <span class="nav-rail__title">{$t('web.frame.brand')}</span>
      </span>
    {/if}
  </div>

  {#if canNavigate}
    <div class="nav-rail__groups">
      {#each RAIL_GROUPS as group (group.id)}
        <section class="nav-rail__group" data-testid={`rail-group-${group.id}`}>
          <h2 class="nav-rail__group-title" class:sr-only={collapsed}>{$t(group.labelKey)}</h2>
          <ul class="nav-rail__list">
            {#each group.items as item (item.id)}
              {@const count = countOf(item)}
              <li class="nav-rail__entry">
                <button
                  type="button"
                  class="nav-rail__item"
                  class:nav-rail__item--active={active === item.route}
                  data-testid={item.testId}
                  aria-current={active === item.route ? 'page' : undefined}
                  title={collapsed ? $t(item.labelKey) : undefined}
                  onclick={() => navigateTo(item.route)}
                >
                  <Icon name={item.icon} size={20} />
                  <span class="nav-rail__item-label">{$t(item.labelKey)}</span>
                  {#if count > 0}
                    <span
                      class="nav-rail__count"
                      data-testid={`${item.testId}-count`}
                      aria-label={countLabel(item, count)}
                    >
                      {count}
                    </span>
                  {/if}
                </button>
              </li>
            {/each}
          </ul>
        </section>
      {/each}
    </div>
  {/if}

  <div class="nav-rail__foot">
    <button
      type="button"
      class="nav-rail__foot-control"
      data-testid="language-toggle"
      onclick={() => setLocale($currentLocale === 'en' ? 'de' : 'en')}
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
          onchange={() => onChooseTheme(choice)}
        />
        <label class="theme-choice__option" for={`theme-${choice}`} data-testid={`theme-${choice}`}>
          {$t(`web.frame.theme.${choice}`)}
        </label>
      {/each}
    </fieldset>

    <button
      type="button"
      class="nav-rail__foot-control nav-rail__toggle"
      data-testid="rail-toggle"
      aria-label={$t(collapsed ? 'web.frame.rail.expand' : 'web.frame.rail.collapse')}
      onclick={onToggle}
    >
      <span class="nav-rail__toggle-icon" class:nav-rail__toggle-icon--collapsed={collapsed}>
        <Icon name="act.back" size={16} />
      </span>
    </button>
  </div>
</nav>

<style>
  .nav-rail {
    display: flex;
    flex-direction: column;
    flex: 0 0 auto;
    inline-size: 232px;
    min-block-size: 100vh;
    padding: var(--space-3) var(--space-2);
    gap: var(--space-4);
    background-color: var(--color-bg-rail);
    border-inline-end: 1px solid var(--color-border);
  }

  .nav-rail--collapsed {
    inline-size: 64px;
    padding-inline: var(--space-1);
  }

  .nav-rail__head {
    padding-inline: var(--space-2);
  }

  .nav-rail--collapsed .nav-rail__head {
    padding-inline: 0;
    display: flex;
    justify-content: center;
  }

  .nav-rail__brand {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    min-inline-size: 0;
    padding: var(--space-1) 0;
    color: var(--color-text-primary);
    border-radius: var(--radius-sm);
    text-align: start;
  }

  .nav-rail__brand:hover .nav-rail__title {
    text-decoration: underline;
  }

  .nav-rail__brand--inert {
    cursor: default;
  }

  .nav-rail__brand--inert:hover .nav-rail__title {
    text-decoration: none;
  }

  .nav-rail__title {
    font-size: var(--text-base);
    font-weight: 700;
    white-space: nowrap;
  }

  .nav-rail--collapsed .nav-rail__title,
  .nav-rail--collapsed .nav-rail__item-label {
    display: none;
  }

  .nav-rail__groups {
    display: flex;
    flex-direction: column;
    flex: 1 1 auto;
    gap: var(--space-4);
    overflow-y: auto;
  }

  .nav-rail__group {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .nav-rail__group-title {
    padding-inline: var(--space-2);
    font-size: var(--text-xs);
    font-weight: 600;
    letter-spacing: 0.02em;
    color: var(--color-text-muted);
  }

  .nav-rail__list {
    display: flex;
    flex-direction: column;
    gap: 1px;
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .nav-rail__entry {
    position: relative;
    min-inline-size: 0;
  }

  .nav-rail__item {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    inline-size: 100%;
    min-block-size: 36px;
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-sm);
    font-size: var(--text-base);
    color: var(--color-text-secondary);
    text-align: start;
  }

  .nav-rail__item:hover {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
  }

  .nav-rail__item--active {
    background-color: var(--color-brand-subtle);
    color: var(--color-brand);
    font-weight: 600;
  }

  .nav-rail__item-label {
    flex: 1 1 auto;
    min-inline-size: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .nav-rail--collapsed .nav-rail__item {
    justify-content: center;
  }

  .nav-rail__count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-inline-size: 1.25rem;
    block-size: 1.25rem;
    padding-inline: 0.35rem;
    border-radius: var(--radius-full);
    background-color: var(--color-brand);
    color: var(--color-on-brand);
    font-size: 0.7rem;
    font-weight: 700;
  }

  .nav-rail--collapsed .nav-rail__count {
    position: absolute;
    transform: translate(14px, -12px);
    min-inline-size: 1rem;
    block-size: 1rem;
    padding-inline: 0.2rem;
    font-size: 0.6rem;
  }

  .nav-rail__foot {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-2);
    border-block-start: 1px solid var(--color-border);
  }

  .nav-rail--collapsed .nav-rail__foot {
    flex-direction: column;
    padding-inline: 0;
  }

  .nav-rail__foot-control {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-inline-size: 32px;
    min-block-size: 32px;
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-sm);
    border: 1px solid var(--color-border-strong);
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .nav-rail__foot-control:hover {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
  }

  .nav-rail__toggle-icon {
    display: inline-flex;
    transition: transform var(--duration-fast) var(--ease-standard);
  }

  .nav-rail__toggle-icon--collapsed {
    transform: rotate(180deg);
  }

  /* The theme choices are one control: a radio group, so the frame says they belong together. */
  .theme-choice {
    display: inline-flex;
    align-items: center;
    gap: 1px;
    margin: 0;
    padding: 1px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
  }

  .nav-rail--collapsed .theme-choice {
    flex-direction: column;
    inline-size: 100%;
  }

  /* The radio itself stays reachable for the keyboard and the screen reader. */
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
    justify-content: center;
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    border-radius: calc(var(--radius-sm) - 1px);
    cursor: pointer;
  }

  .theme-choice__option:hover {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
  }

  .theme-choice__input:checked + .theme-choice__option {
    background-color: var(--color-brand-subtle);
    color: var(--color-brand);
    font-weight: 600;
  }

  .theme-choice__input:focus-visible + .theme-choice__option {
    box-shadow: var(--focus-ring);
  }

  @media (max-width: 767px) {
    /* The phone has no rail: the bottom bar carries the destinations. */
    .nav-rail {
      display: none;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .nav-rail__toggle-icon {
      transition: none;
    }
  }
</style>
