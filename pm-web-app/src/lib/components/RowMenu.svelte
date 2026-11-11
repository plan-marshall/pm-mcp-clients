<script module lang="ts">
  export interface RowMenuItem {
    id: string;
    label: string;
    title?: string;
    disabled?: boolean;
    onclick?: (event: MouseEvent) => void | Promise<void>;
  }
</script>

<script lang="ts">
  import { onDestroy, tick } from 'svelte';
  import IconButton from './IconButton.svelte';

  /**
   * RowMenu (VD-P-05): the WAI-ARIA menu button pattern for row actions in card layouts and lists.
   *
   * It uses `IconButton` as its trigger (`act.more`), names its row through `label`, and manages
   * its popup menu with full keyboard navigation (arrows, Home/End, Escape, Tab). The open menu is
   * a popup layer above the sticky chrome of the frame, and it opens upwards where it would pass
   * the bottom of the viewport (`doc/design/web-app/components.adoc#row-menu-placement`).
   */
  export let items: RowMenuItem[] = [];
  /** Accessible name describing which row/subject the menu acts on. */
  export let label: string;
  /** Visible label text shown beside the icon on wide viewports. */
  export let visibleLabel: string | undefined = undefined;
  /** Hover advisory from message catalogue. */
  export let advisory: string | undefined = undefined;
  export let icon = 'act.more';
  export let size: 'sm' | 'md' | 'lg' = 'sm';
  export let disabled = false;

  /**
   * The advisory the trigger carries: the catalogue's `X.title` only where it says more than the
   * visible word. An advisory that restates the label is no advisory (Interactions Affordances).
   */
  $: triggerAdvisory = advisory && advisory !== (visibleLabel ?? label) ? advisory : undefined;

  let open = false;
  /** Whether the open menu hangs above its trigger instead of below it. */
  let above = false;
  let rootElement: HTMLElement | null = null;
  let menuHost: HTMLElement | null = null;
  let triggerElement: HTMLElement | null = null;

  /** The gap between the trigger and its menu, `--space-1`. */
  const MENU_GAP = 4;

  /**
   * Opens the menu upwards when it would pass the bottom of the viewport below its trigger and the
   * viewport has more room above the trigger than below it. A row at the bottom of a phone screen
   * thereby opens its menu into the page, not under the sticky bottom bar. A menu taller than the
   * room on its side is capped to that room and scrolls, so no entry lies beyond the viewport.
   */
  function place(): void {
    if (!menuHost || !rootElement) return;
    menuHost.style.maxBlockSize = '';
    const row = rootElement.getBoundingClientRect();
    const height = menuHost.getBoundingClientRect().height;
    const roomBelow = window.innerHeight - row.bottom - MENU_GAP;
    const roomAbove = row.top - MENU_GAP;
    above = height > roomBelow && roomAbove > roomBelow;
    const room = Math.max(0, above ? roomAbove : roomBelow);
    if (height > room) {
      menuHost.style.maxBlockSize = `${room}px`;
    }
  }

  /**
   * Places the open menu again after the page scrolled or the viewport changed its size.
   *
   * A scroll inside the menu itself moves none of the trigger's box: a menu capped to the room on
   * its side scrolls its own entries, and re-placing it would reset both the cap and the entry the
   * operator scrolled to. Only a scroll of the page around the trigger re-places the menu.
   *
   * @param event the viewport event that asked for the re-place
   */
  function onViewportChange(event?: Event): void {
    if (!open) return;
    if (event?.type === 'scroll' && event.target instanceof Node && menuHost?.contains(event.target)) {
      return;
    }
    place();
  }

  function followViewport(follow: boolean): void {
    if (follow) {
      window.addEventListener('scroll', onViewportChange, true);
      window.addEventListener('resize', onViewportChange);
    } else {
      window.removeEventListener('scroll', onViewportChange, true);
      window.removeEventListener('resize', onViewportChange);
    }
  }

  onDestroy(() => followViewport(false));

  function menuEntries(): HTMLButtonElement[] {
    if (!menuHost) return [];
    return Array.from(menuHost.querySelectorAll<HTMLButtonElement>('[role="menuitem"]')).filter(
      (entry) => !entry.disabled,
    );
  }

  export async function openMenu(): Promise<void> {
    if (disabled || open) return;
    above = false;
    open = true;
    await tick();
    place();
    followViewport(true);
    await tick();
    menuEntries()[0]?.focus();
  }

  export async function closeMenu(returnFocus = false): Promise<void> {
    if (!open) return;
    open = false;
    followViewport(false);
    if (returnFocus) {
      await tick();
      triggerElement?.focus();
    }
  }

  export function toggleMenu(): void {
    if (open) {
      void closeMenu();
    } else {
      void openMenu();
    }
  }

  function handleTriggerClick(event: MouseEvent): void {
    triggerElement = event.currentTarget as HTMLElement;
    toggleMenu();
  }

  async function choose(item: RowMenuItem, event: MouseEvent): Promise<void> {
    if (item.disabled) return;
    await closeMenu(true);
    if (item.onclick) {
      await item.onclick(event);
    }
  }

  function onMenuKeydown(event: KeyboardEvent): void {
    const entries = menuEntries();
    if (event.key === 'Escape') {
      event.stopPropagation();
      void closeMenu(true);
      return;
    }
    if (event.key === 'Tab') {
      event.preventDefault();
      void closeMenu(true);
      return;
    }
    if (event.key === 'Home') {
      event.preventDefault();
      entries[0]?.focus();
      return;
    }
    if (event.key === 'End') {
      event.preventDefault();
      entries[entries.length - 1]?.focus();
      return;
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (entries.length === 0) return;
      const current = document.activeElement as HTMLButtonElement;
      const index = entries.indexOf(current);
      const next = (index + 1) % entries.length;
      entries[next]?.focus();
      return;
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault();
      if (entries.length === 0) return;
      const current = document.activeElement as HTMLButtonElement;
      const index = entries.indexOf(current);
      const prev = (index - 1 + entries.length) % entries.length;
      entries[prev]?.focus();
      return;
    }
  }

  function onWindowClick(event: MouseEvent): void {
    if (!open || !menuHost) return;
    const target = event.target as Node;
    if (menuHost.contains(target) || (triggerElement && triggerElement.contains(target))) {
      return;
    }
    void closeMenu();
  }

  function onWindowKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && open) {
      void closeMenu(true);
    }
  }
</script>

<svelte:window onclick={onWindowClick} onkeydown={onWindowKeydown} />

<div class="row-menu" bind:this={rootElement}>
  <IconButton
    {icon}
    {label}
    {visibleLabel}
    {size}
    advisory={triggerAdvisory}
    {disabled}
    aria-haspopup="menu"
    aria-expanded={open}
    onclick={handleTriggerClick}
    {...$$restProps}
  />
  {#if open}
    <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
    <div
      class="row-menu__list"
      class:row-menu__list--above={above}
      role="menu"
      tabindex="-1"
      aria-label={label}
      bind:this={menuHost}
      onkeydown={onMenuKeydown}
    >
      {#each items as item (item.id)}
        <button
          type="button"
          role="menuitem"
          tabindex="-1"
          class="row-menu__item"
          title={item.title}
          disabled={item.disabled}
          onclick={(event) => choose(item, event)}
        >
          {item.label}
        </button>
      {/each}
      <slot />
    </div>
  {/if}
</div>

<style>
  .row-menu {
    position: relative;
    display: inline-flex;
  }

  .row-menu__list {
    position: absolute;
    inset-block-start: calc(100% + var(--space-1));
    inset-inline-end: 0;
    /* A popup layer: above the sticky chrome of the phone frame (20), below a dialog (50). */
    z-index: 30;
    display: flex;
    flex-direction: column;
    min-inline-size: 180px;
    overflow-y: auto;
    padding: var(--space-1);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-raised);
    box-shadow: var(--shadow-2);
  }

  .row-menu__list--above {
    inset-block-start: auto;
    inset-block-end: calc(100% + var(--space-1));
  }

  .row-menu__item {
    padding: var(--space-2) var(--space-3);
    border: none;
    border-radius: var(--radius-sm);
    background: none;
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    text-align: start;
    cursor: pointer;
  }

  .row-menu__item:hover:not(:disabled) {
    background-color: var(--color-bg-subtle);
  }

  .row-menu__item:focus:not(:disabled) {
    background-color: var(--color-bg-subtle);
    outline: none;
  }

  .row-menu__item:focus-visible:not(:disabled) {
    background-color: var(--color-bg-subtle);
    outline: 2px solid var(--color-brand);
    outline-offset: -2px;
  }

  /* A phone gets the 44 px target on every entry, as the rest of the design system does. */
  @media (max-width: 767px) {
    .row-menu__item {
      min-block-size: 44px;
    }
  }

  .row-menu__item:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
</style>
