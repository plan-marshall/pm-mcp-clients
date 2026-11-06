<script lang="ts">
  import { tick } from 'svelte';
  import Button from './Button.svelte';
  import IconButton from './IconButton.svelte';
  import { uniqueId } from '../utils/uniqueId';
  import { t } from '../i18n';

  /**
   * The visual bar of acts (VD-P-05, UX-P-04): one primary, up to three secondary, and the rest in
   * an overflow `IconButton` with a menu.
   *
   * The bar draws the acts it is given, with the role each act carries: a roleless list keeps the
   * old positional reading (first act primary, the next three secondary), and a destructive act is
   * never primary and never secondary, whatever role the caller marked. `lib/actions/byState.ts`
   * is where a state's acts and their roles come from.
   *
   * The overflow is a menu in the sense the role promises: opening it moves the focus to its first
   * enabled entry, the arrow keys and Home/End move within it, Tab closes it and puts the focus back
   * on the button, and Escape does the same.
   */
  interface ActionBarAct {
    /** The act's id, reported to `onAct`. */
    id: string;
    /** The visible label, from the view's catalogue key. */
    label: string;
    /** The hover advisory, from the view's `X.title` key. */
    advisory?: string;
    /** Where the act sits; without a role the order decides, as before. */
    role?: 'primary' | 'secondary' | 'overflow';
    /** Whether the act cannot be taken back; it never leaves the overflow. */
    destructive?: boolean;
    disabled?: boolean;
    /** Why the act cannot run; the control carries it as advisory and description. */
    disabledReason?: string;
  }

  /** The acts, most important first. */
  export let acts: ActionBarAct[] = [];
  /** Whether the whole bar waits for a running request. */
  export let busy = false;
  /** Reports the act a person chose, by its id. */
  export let onAct: (id: string) => void = () => {};

  /** How many secondary acts the bar shows before it overflows. */
  const MAX_SECONDARY = 3;

  let open = false;
  let host: HTMLDivElement | undefined = undefined;

  /** Whether the caller named the rolls rather than only the order. */
  $: hasRoles = acts.some((act) => act.role !== undefined);
  $: primary = hasRoles
    ? acts.find((act) => act.role === 'primary' && !act.destructive)
    : acts.find((act) => !act.destructive);
  $: rest = acts.filter((act) => act !== primary);
  $: secondary = rest
    .filter((act) => (hasRoles ? act.role === 'secondary' && !act.destructive : !act.destructive))
    .slice(0, MAX_SECONDARY);
  $: overflow = rest.filter((act) => !secondary.includes(act));

  /** @returns the button that opened the menu */
  function overflowButton(): HTMLButtonElement | null {
    return host?.querySelector<HTMLButtonElement>('[data-testid="action-bar-overflow"]') ?? null;
  }

  /**
   * @returns every entry of the menu, the ones that cannot run included: an act whose reason is
   *   worth reading stays reachable with the arrow keys
   */
  function menuItems(): HTMLButtonElement[] {
    if (!host) {
      return [];
    }
    return Array.from(host.querySelectorAll<HTMLButtonElement>('[role="menuitem"]'));
  }

  /** The prefix of this bar's reason ids, so two bars on one page never share an id. */
  const reasonPrefix = uniqueId('action-bar-reason');

  /**
   * @param id the act's id
   * @returns the id of the element that carries the act's reason
   */
  function reasonIdOf(id: string): string {
    return `${reasonPrefix}-${id}`;
  }

  /** Opens the menu and puts the focus on its first entry that can run. */
  async function openMenu(): Promise<void> {
    open = true;
    await tick();
    const items = menuItems();
    (items.find((item) => item.getAttribute('aria-disabled') !== 'true') ?? items[0])?.focus();
  }

  /**
   * @param returnFocus whether the focus goes back to the button that opened the menu
   */
  async function closeMenu(returnFocus = false): Promise<void> {
    open = false;
    if (returnFocus) {
      await tick();
      overflowButton()?.focus();
    }
  }

  /**
   * @param id the chosen act's id
   * @param fromMenu whether the act came from the overflow menu, whose focus goes back to the button
   * @param target the control that was clicked outside the menu, which keeps the focus
   */
  async function choose(
    id: string,
    fromMenu = false,
    target?: HTMLElement | null,
  ): Promise<void> {
    if (fromMenu) {
      await closeMenu(true);
    } else {
      open = false;
      target?.focus();
    }
    onAct(id);
  }

  /** @param event the key press inside the menu */
  function onMenuKeydown(event: KeyboardEvent): void {
    const items = menuItems();
    if (event.key === 'Escape') {
      event.stopPropagation();
      void closeMenu(true);
      return;
    }
    if (event.key === 'Tab') {
      // The entries are not in the tab sequence, so letting the default run from a node that the
      // close is about to remove would drop the focus on the body. Put it back on the button and
      // let the next Tab continue from there.
      event.preventDefault();
      void closeMenu(true);
      return;
    }
    if (event.key === 'Home') {
      event.preventDefault();
      items[0]?.focus();
      return;
    }
    if (event.key === 'End') {
      event.preventDefault();
      items[items.length - 1]?.focus();
      return;
    }
    if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp') {
      return;
    }
    event.preventDefault();
    if (items.length === 0) {
      return;
    }
    const current = items.indexOf(document.activeElement as HTMLButtonElement);
    const step = event.key === 'ArrowDown' ? 1 : -1;
    const next = (current + step + items.length) % items.length;
    items[next]?.focus();
  }

  /** @param event the key press anywhere, `Escape` closing a menu the focus has left */
  function closeOnEscape(event: KeyboardEvent): void {
    if (event.key === 'Escape' && open) {
      void closeMenu(true);
    }
  }

  /** @param event the click, closing the menu when it lands outside the bar */
  function closeOnOutside(event: MouseEvent): void {
    if (open && host && !host.contains(event.target as Node)) {
      open = false;
    }
  }
</script>

<svelte:window on:keydown={closeOnEscape} on:click={closeOnOutside} />

<div class="action-bar" bind:this={host} data-testid="action-bar">
  {#if primary}
    <Button
      variant="primary"
      disabled={busy || primary.disabled}
      disabledReason={primary.disabledReason}
      title={primary.advisory}
      data-testid={`action-${primary.id}`}
      on:click={(event) => choose(primary.id, false, event.currentTarget as HTMLElement | null)}
    >
      {primary.label}
    </Button>
  {/if}

  {#each secondary as act (act.id)}
    <Button
      variant="secondary"
      disabled={busy || act.disabled}
      disabledReason={act.disabledReason}
      title={act.advisory}
      data-testid={`action-${act.id}`}
      on:click={(event) => choose(act.id, false, event.currentTarget as HTMLElement | null)}
    >
      {act.label}
    </Button>
  {/each}

  {#if overflow.length > 0}
    <div class="action-bar__overflow">
      <IconButton
        icon="act.more"
        label={$t('web.common.more')}
        advisory={$t('web.common.more.title')}
        aria-haspopup="menu"
        aria-expanded={open}
        disabled={busy}
        data-testid="action-bar-overflow"
        on:click={() => (open ? closeMenu() : openMenu())}
      />
      {#if open}
        <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
        <div class="action-bar__menu" role="menu" tabindex="-1" on:keydown={onMenuKeydown}>
          {#each overflow as act (act.id)}
            <button
              type="button"
              role="menuitem"
              tabindex="-1"
              class="action-bar__item"
              data-testid={`action-${act.id}`}
              aria-disabled={busy || act.disabled ? 'true' : undefined}
              aria-describedby={!busy && act.disabled && act.disabledReason
                ? reasonIdOf(act.id)
                : undefined}
              title={act.disabled ? (act.disabledReason ?? act.advisory) : act.advisory}
              on:click={() => {
                if (!busy && !act.disabled) {
                  void choose(act.id, true);
                }
              }}
            >
              {act.label}
            </button>
          {/each}
        </div>
        <!-- A menu holds menu items: the reason of a disabled entry is described from outside it. -->
        {#each overflow as act (act.id)}
          {#if act.disabled && act.disabledReason}
            <span class="sr-only" id={reasonIdOf(act.id)}>{act.disabledReason}</span>
          {/if}
        {/each}
      {/if}
    </div>
  {/if}
</div>

<style>
  .action-bar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .action-bar__overflow {
    position: relative;
    display: inline-flex;
  }

  .action-bar__menu {
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

  .action-bar__item {
    padding: var(--space-2) var(--space-3);
    border: none;
    border-radius: var(--radius-sm);
    background: none;
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    text-align: start;
    cursor: pointer;
  }

  .action-bar__item:hover:not(:disabled) {
    background-color: var(--color-bg-subtle);
  }

  .action-bar__item[aria-disabled='true'] {
    opacity: 0.5;
    cursor: not-allowed;
  }

  /* A phone gets the 44 px target on every entry, as the rest of the design system does. */
  @media (max-width: 767px) {
    .action-bar__item {
      min-block-size: 44px;
    }
  }

  /* A phone stacks the bar, so no act is squeezed out of reach. */
  @media (max-width: 767px) {
    .action-bar {
      flex-direction: column;
      align-items: stretch;
    }

    .action-bar__overflow {
      align-self: flex-end;
    }
  }
</style>
