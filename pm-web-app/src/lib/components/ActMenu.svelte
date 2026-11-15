<script module lang="ts">
  export interface ActMenuItem {
    id: string;
    label: string;
    destructive?: boolean;
    disabled?: boolean;
    disabledReason?: string;
    advisory?: string;
    title?: string;
    onclick?: (event: MouseEvent) => void | Promise<void>;
  }
</script>

<script lang="ts">
  import { tick } from 'svelte';
  import ConfirmDialog from './ConfirmDialog.svelte';
  import { uniqueId } from '../utils/uniqueId';
  import { t } from '../i18n';

  export let items: ActMenuItem[] = [];
  export let label: string;
  export let open = false;
  export let above = false;
  export let busy = false;
  export let confirmDestructive = true;
  export let triggerElement: HTMLElement | null = null;
  export let maxBlockSize: string | undefined = undefined;
  export let onAct: (item: ActMenuItem, event?: MouseEvent) => void = () => {};
  export let onSelect: (item: ActMenuItem, event?: MouseEvent) => void = () => {};
  export let onClose: (returnFocus?: boolean) => void = () => {};

  let menuElement: HTMLElement | null = null;
  let focusedId: string | null = null;
  let confirmingItem: ActMenuItem | null = null;

  const reasonPrefix = uniqueId('act-menu-reason');

  function reasonIdOf(id: string): string {
    return `${reasonPrefix}-${id}`;
  }

  export function getMenuElement(): HTMLElement | null {
    return menuElement;
  }

  export function menuEntries(): HTMLButtonElement[] {
    if (!menuElement) return [];
    return Array.from(menuElement.querySelectorAll<HTMLButtonElement>('[role="menuitem"]'));
  }

  export function focusFirst(): void {
    const entries = menuEntries();
    if (entries.length === 0) return;
    const target = entries.find((e) => e.getAttribute('aria-disabled') !== 'true') ?? entries[0];
    target?.focus();
  }

  let wasOpen = false;
  $: if (open && !wasOpen) {
    wasOpen = true;
    tick().then(() => {
      if (menuElement && !menuElement.contains(document.activeElement)) {
        focusFirst();
      }
    });
  } else if (!open && wasOpen) {
    wasOpen = false;
  }

  export function close(returnFocus = false): void {
    if (!open) return;
    open = false;
    onClose(returnFocus);
    if (returnFocus && triggerElement) {
      triggerElement.focus();
    }
  }

  async function choose(item: ActMenuItem, event: MouseEvent): Promise<void> {
    if (busy || item.disabled) return;
    if (confirmDestructive && item.destructive) {
      confirmingItem = item;
      return;
    }
    close(true);
    let promise: void | Promise<void> = undefined;
    if (item.onclick) {
      promise = item.onclick(event);
    }
    onSelect(item, event);
    onAct(item, event);
    if (promise) {
      await promise;
    }
  }

  async function confirmDestructiveAction(): Promise<void> {
    if (busy) return;
    const item = confirmingItem;
    confirmingItem = null;
    close(true);
    if (item) {
      let promise: void | Promise<void> = undefined;
      if (item.onclick) {
        promise = item.onclick(new MouseEvent('click'));
      }
      onSelect(item);
      onAct(item);
      if (promise) {
        await promise;
      }
    }
  }

  function cancelDestructiveAction(): void {
    confirmingItem = null;
    void close(true);
  }

  function onMenuKeydown(event: KeyboardEvent): void {
    const entries = menuEntries();
    if (event.key === 'Escape') {
      event.stopPropagation();
      event.preventDefault();
      void close(true);
      return;
    }
    if (event.key === 'Tab') {
      event.preventDefault();
      void close(true);
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
      const next = (index + 1 + entries.length) % entries.length;
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
    if (!open || !menuElement || confirmingItem) return;
    const target = event.target as Node;
    if (menuElement.contains(target) || (triggerElement && triggerElement.contains(target))) {
      return;
    }
    void close(false);
  }

  function onWindowKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && open && !confirmingItem) {
      event.stopPropagation();
      void close(true);
    }
  }
</script>

<svelte:window on:click={onWindowClick} on:keydown={onWindowKeydown} />

{#if open}
  <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
  <div
    class="act-menu row-menu__list"
    class:act-menu--above={above}
    class:row-menu__list--above={above}
    role="menu"
    tabindex="-1"
    aria-label={label}
    bind:this={menuElement}
    on:keydown={onMenuKeydown}
    style={maxBlockSize ? `max-block-size: ${maxBlockSize};` : undefined}
  >
    {#each items as item (item.id)}
      {@const isDisabled = busy || item.disabled}
      {@const hasReason = item.disabled && !!item.disabledReason}
      {@const rId = reasonIdOf(item.id)}
      <button
        type="button"
        role="menuitem"
        tabindex="-1"
        class="act-menu__item row-menu__item"
        class:act-menu__item--destructive={item.destructive}
        class:act-menu__item--disabled={isDisabled}
        class:act-menu__item--with-reason={hasReason}
        aria-disabled={isDisabled ? 'true' : undefined}
        aria-describedby={hasReason ? rId : undefined}
        title={item.disabled ? (item.disabledReason ?? item.advisory ?? item.title) : (item.advisory ?? item.title)}
        data-testid={`action-${item.id}`}
        on:focus={() => { focusedId = item.id; }}
        on:blur={() => { if (focusedId === item.id) focusedId = null; }}
        on:click={(event) => choose(item, event)}
      >
        <span class="act-menu__label">{item.label}</span>
        {#if hasReason}
          <span
            class="act-menu__reason"
            class:act-menu__reason--visible={focusedId === item.id}
            id={rId}
            aria-hidden="true"
          >
            {item.disabledReason}
          </span>
        {/if}
      </button>
    {/each}
    <slot />
  </div>
{/if}

{#if confirmingItem}
  <ConfirmDialog
    open={true}
    {busy}
    title={confirmingItem.label}
    consequences={confirmingItem.disabledReason ? [confirmingItem.disabledReason] : [confirmingItem.label]}
    actionLabel={confirmingItem.label}
    cancelLabel={$t('web.common.cancel')}
    onConfirm={confirmDestructiveAction}
    onClose={cancelDestructiveAction}
  />
{/if}

<style>
  .act-menu {
    position: absolute;
    inset-block-start: calc(100% + var(--space-1));
    inset-inline-end: 0;
    z-index: var(--layer-menu);
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

  .row-menu__list {
    position: absolute;
    inset-block-start: calc(100% + var(--space-1));
    inset-inline-end: 0;
    z-index: var(--layer-menu);
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

  .act-menu--above {
    inset-block-start: auto;
    inset-block-end: calc(100% + var(--space-1));
  }

  .row-menu__list--above {
    inset-block-start: auto;
    inset-block-end: calc(100% + var(--space-1));
  }

  .act-menu__item {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    padding: var(--space-2) var(--space-3);
    border: none;
    border-radius: var(--radius-sm);
    background: none;
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
    text-align: start;
    cursor: pointer;
    inline-size: 100%;
  }

  .row-menu__item {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    padding: var(--space-2) var(--space-3);
    border: none;
    border-radius: var(--radius-sm);
    background: none;
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
    text-align: start;
    cursor: pointer;
    inline-size: 100%;
  }

  .act-menu__item--destructive {
    color: var(--color-danger);
  }

  .act-menu__item:hover:not(.act-menu__item--disabled) {
    background-color: var(--color-bg-subtle);
  }

  .row-menu__item:hover:not(:disabled) {
    background-color: var(--color-bg-subtle);
  }

  .act-menu__item:focus {
    background-color: var(--color-bg-subtle);
    outline: none;
    box-shadow: var(--focus-ring);
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

  .act-menu__item--disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .act-menu__reason {
    display: none;
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    margin-block-start: var(--space-1);
  }

  .act-menu__reason--visible {
    display: block;
  }

  @media (max-width: 767px) {
    .row-menu__item {
      min-block-size: 44px;
    }
  }
</style>
