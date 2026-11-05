<script lang="ts">
  import { tick } from 'svelte';
  import IconButton from './IconButton.svelte';

  export interface RowMenuItem {
    id: string;
    label: string;
    title?: string;
    disabled?: boolean;
    onclick?: (event: MouseEvent) => void | Promise<void>;
  }

  /**
   * RowMenu (VD-P-05): the WAI-ARIA menu button pattern for row actions in card layouts and lists.
   *
   * It uses `IconButton` as its trigger (`act.more`), names its row through `label`, and manages
   * its popup menu with full keyboard navigation (arrows, Home/End, Escape, Tab).
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

  let open = false;
  let menuHost: HTMLElement | null = null;
  let triggerElement: HTMLElement | null = null;

  function menuEntries(): HTMLButtonElement[] {
    if (!menuHost) return [];
    return Array.from(menuHost.querySelectorAll<HTMLButtonElement>('[role="menuitem"]')).filter(
      (entry) => !entry.disabled,
    );
  }

  export async function openMenu(): Promise<void> {
    if (disabled || open) return;
    open = true;
    await tick();
    menuEntries()[0]?.focus();
  }

  export async function closeMenu(returnFocus = false): Promise<void> {
    if (!open) return;
    open = false;
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
    await closeMenu();
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

<div class="row-menu">
  <IconButton
    {icon}
    {label}
    {visibleLabel}
    {size}
    {advisory}
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
      role="menu"
      tabindex="-1"
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
    z-index: 10;
    display: flex;
    flex-direction: column;
    min-inline-size: 180px;
    padding: var(--space-1);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-raised);
    box-shadow: var(--shadow-2);
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

  .row-menu__item:hover:not(:disabled),
  .row-menu__item:focus:not(:disabled) {
    background-color: var(--color-bg-subtle);
    outline: none;
  }

  .row-menu__item:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
</style>
