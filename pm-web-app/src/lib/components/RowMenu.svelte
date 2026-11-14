<script module lang="ts">
  export type { ActMenuItem as RowMenuItem } from './ActMenu.svelte';
</script>

<script lang="ts">
  import { onDestroy, tick } from 'svelte';
  import IconButton from './IconButton.svelte';
  import ActMenu, { type ActMenuItem } from './ActMenu.svelte';

  /**
   * RowMenu (VD-P-05): the WAI-ARIA menu button pattern for row actions in card layouts and lists.
   *
   * It uses `IconButton` as its trigger (`act.more`), names its row through `label`, and manages
   * its popup menu with full keyboard navigation (arrows, Home/End, Escape, Tab). The open menu is
   * a popup layer above the sticky chrome of the frame, and it opens upwards where it would pass
   * the bottom of the viewport (`doc/design/web-app/components.adoc#row-menu-placement`).
   */
  export let items: ActMenuItem[] = [];
  /** Accessible name describing which row/subject the menu acts on. */
  export let label: string;
  /** Hover advisory from message catalogue. */
  export let advisory: string | undefined = undefined;
  export let icon = 'act.more';
  export let size: 'sm' | 'md' | 'lg' = 'sm';
  export let disabled = false;

  /**
   * The advisory the trigger carries: the catalogue's `X.title` only where it says more than the
   * visible word. An advisory that restates the label is no advisory (Interactions Affordances).
   */
  $: triggerAdvisory = advisory && advisory !== label ? advisory : undefined;

  let open = false;
  /** Whether the open menu hangs above its trigger instead of below it. */
  let above = false;
  let maxBlockSize: string | undefined = undefined;
  let rootElement: HTMLElement | null = null;
  let triggerElement: HTMLElement | null = null;
  let actMenu: ActMenu | null = null;

  /** The gap between the trigger and its menu, `--space-1`. */
  const MENU_GAP = 4;

  /**
   * Opens the menu upwards when it would pass the bottom of the viewport below its trigger and the
   * viewport has more room above the trigger than below it. A row at the bottom of a phone screen
   * thereby opens its menu into the page, not under the sticky bottom bar. A menu taller than the
   * room on its side is capped to that room and scrolls, so no entry lies beyond the viewport.
   */
  function place(): void {
    if (!rootElement || !actMenu) return;
    const menuHost = actMenu.getMenuElement();
    if (!menuHost) return;
    menuHost.style.maxBlockSize = '';
    const row = rootElement.getBoundingClientRect();
    const height = menuHost.getBoundingClientRect().height;
    const roomBelow = window.innerHeight - row.bottom - MENU_GAP;
    const roomAbove = row.top - MENU_GAP;
    above = height > roomBelow && roomAbove > roomBelow;
    const room = Math.max(0, above ? roomAbove : roomBelow);
    if (height > room) {
      maxBlockSize = `${room}px`;
    } else {
      maxBlockSize = undefined;
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
    const menuHost = actMenu?.getMenuElement();
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

  $: followViewport(open);

  onDestroy(() => followViewport(false));

  export async function openMenu(): Promise<void> {
    if (disabled || open || items.length === 0) return;
    above = false;
    open = true;
    await tick();
    place();
    await tick();
    actMenu?.focusFirst();
  }

  export async function closeMenu(returnFocus = false): Promise<void> {
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

  function handleTriggerClick(event: CustomEvent | MouseEvent): void {
    triggerElement = (event.currentTarget as HTMLElement) ?? null;
    toggleMenu();
  }
</script>

{#if items.length > 0}
  <div class="row-menu" bind:this={rootElement}>
    <IconButton
      {icon}
      {label}
      {size}
      advisory={triggerAdvisory}
      {disabled}
      aria-haspopup="menu"
      aria-expanded={open}
      on:click={handleTriggerClick}
      {...$$restProps}
    />
    <ActMenu
      bind:this={actMenu}
      {items}
      {label}
      bind:open
      {above}
      {maxBlockSize}
      {triggerElement}
      onClose={(returnFocus) => closeMenu(returnFocus)}
    >
      <slot />
    </ActMenu>
  </div>
{/if}

<style>
  .row-menu {
    position: relative;
    display: inline-flex;
  }
</style>
