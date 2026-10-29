<script module lang="ts">
  /** One tab of the list: what it selects, what it says, and what it counts. */
  export interface TabItem {
    /** The id the view selects and the list reports. */
    id: string;
    /** The visible label, from the view's catalogue key. */
    label: string;
    /** The number the tab counts, drawn as a `CountBadge`; omitted when the tab counts nothing. */
    count?: number;
    /** The phrase that names the number for a screen reader, including the figure; e.g. "2 needs you". */
    countLabel?: string;
    /** Whether the number names something that waits for the operator. */
    attention?: boolean;
    /** The id of the panel the tab controls, when the view draws one. */
    panelId?: string;
  }
</script>

<script lang="ts">
  import CountBadge from './CountBadge.svelte';

  /**
   * The underline tab list (VD-P-05, VD-S-03): the active tab carries the brand underline, the
   * others stay in text-secondary, and a tab that counts something carries its `CountBadge`.
   *
   * The keyboard belongs to the widget: the arrow keys move within the list and wrap at its ends,
   * Home and End jump to either end, and the tab a person lands on is the tab that selects — which
   * is the row of a tab list, where the panel follows the focus.
   */
  export let tabs: TabItem[] = [];
  /** The selected tab; a view that keeps the selection itself passes it and follows `onSelect`. */
  export let active: string | undefined = undefined;
  /** The accessible name of the list, from the view's catalogue key. */
  export let label: string;
  /** Reports the tab a person chose. */
  export let onSelect: (id: string) => void = () => {};

  /** The tab the list draws as selected; the prop wins whenever the view changes it. */
  let selected: string = active ?? tabs[0]?.id ?? '';
  /** The prop value the list has already followed, so its own selection is not undone. */
  let followed: string | undefined = active;

  $: if (active === undefined) {
    followed = undefined;
  } else if (active !== followed) {
    followed = active;
    selected = active;
  }

  // A view may fill its tabs after its fetch, or drop the one that was selected: the list then
  // takes the view's tab when it arrives, and otherwise selects its first tab instead of handing
  // every tab a `tabindex="-1"` and dying for the keyboard.
  $: if (!tabs.some((tab) => tab.id === selected)) {
    selected = active !== undefined && tabs.some((tab) => tab.id === active) ? active : (tabs[0]?.id ?? '');
  }

  /** The list's own element, for the keyboard to move within. */
  let host: HTMLDivElement | undefined = undefined;

  /** @param id the tab to select */
  function select(id: string): void {
    selected = id;
    onSelect(id);
  }

  /** @returns the tabs in document order, which is the order the keyboard walks */
  function tabElements(): HTMLButtonElement[] {
    return host ? Array.from(host.querySelectorAll<HTMLButtonElement>('[role="tab"]')) : [];
  }

  /** @param event the key press inside the list */
  function onKeydown(event: KeyboardEvent): void {
    const elements = tabElements();
    if (elements.length === 0) {
      return;
    }
    const target = (event.target as HTMLElement | null)?.closest('[role="tab"]');
    const current = target
      ? elements.indexOf(target as HTMLButtonElement)
      : elements.indexOf(document.activeElement as HTMLButtonElement);
    let next: number;
    switch (event.key) {
      case 'ArrowRight':
        next = (current + 1 + elements.length) % elements.length;
        break;
      case 'ArrowLeft':
        next = (current - 1 + elements.length) % elements.length;
        break;
      case 'Home':
        next = 0;
        break;
      case 'End':
        next = elements.length - 1;
        break;
      default:
        return;
    }
    event.preventDefault();
    elements[next]?.focus();
    const id = tabs[next]?.id;
    if (id !== undefined) {
      select(id);
    }
  }
</script>

<div class="tabs" role="tablist" aria-label={label} bind:this={host} on:keydown={onKeydown}>
  {#each tabs as tab (tab.id)}
    <button
      type="button"
      role="tab"
      class="tabs__tab"
      class:tabs__tab--active={selected === tab.id}
      aria-selected={selected === tab.id}
      aria-controls={tab.panelId}
      tabindex={selected === tab.id ? 0 : -1}
      on:click={() => select(tab.id)}
    >
      <span class="tabs__label">{tab.label}</span>
      {#if tab.count !== undefined}
        <CountBadge count={tab.count} attention={tab.attention ?? false} label={tab.countLabel} />
      {/if}
    </button>
  {/each}
</div>

<style>
  .tabs {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    gap: var(--space-4);
    border-block-end: 1px solid var(--color-border);
  }

  .tabs__tab {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-2) 0;
    border: none;
    border-block-end: 2px solid transparent;
    background: none;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
    cursor: pointer;
  }

  .tabs__tab:hover {
    color: var(--color-text-primary);
  }

  .tabs__tab--active {
    border-block-end-color: var(--color-brand);
    color: var(--color-text-primary);
    font-weight: var(--weight-semibold);
  }

  /* A phone gets the 44 px target, as every control of the system does. */
  @media (max-width: 767px) {
    .tabs__tab {
      min-height: 44px;
    }
  }
</style>
