<script lang="ts">
  import Button from './Button.svelte';
  import IconButton from './IconButton.svelte';
  import { t } from '../i18n';

  /**
   * The visual bar of acts (VD-P-05, UX-P-04): one primary, up to three secondary, and the rest in
   * an overflow `IconButton` with a menu.
   *
   * The bar draws the acts it is given; which acts a state offers, and in which order, comes with
   * `web-actions-by-state`. The first act is the primary one, so a caller orders its list by
   * importance rather than by markup.
   */
  interface ActionBarAct {
    /** The act's id, reported to `onAct`. */
    id: string;
    /** The visible label, from the view's catalogue key. */
    label: string;
    /** The hover advisory, from the view's `X.title` key. */
    advisory?: string;
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
  let bar: HTMLElement | undefined = undefined;

  $: primary = acts.at(0);
  $: secondary = acts.slice(1, 1 + MAX_SECONDARY);
  $: overflow = acts.slice(1 + MAX_SECONDARY);

  /**
   * @param id the chosen act's id
   */
  function choose(id: string): void {
    open = false;
    onAct(id);
  }

  /** @param event the key press, `Escape` closing the menu */
  function closeOnEscape(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      open = false;
    }
  }

  /** @param event the click, closing the menu when it lands outside the bar */
  function closeOnOutside(event: MouseEvent): void {
    if (open && bar && !bar.contains(event.target as Node)) {
      open = false;
    }
  }
</script>

<svelte:window on:keydown={closeOnEscape} on:click={closeOnOutside} />

<div class="action-bar" bind:this={bar} data-testid="action-bar">
  {#if primary}
    <Button
      variant="primary"
      disabled={busy || primary.disabled}
      disabledReason={primary.disabledReason}
      on:click={() => choose(primary.id)}
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
      on:click={() => choose(act.id)}
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
        on:click={() => (open = !open)}
      />
      {#if open}
        <div class="action-bar__menu" role="menu">
          {#each overflow as act (act.id)}
            <button
              type="button"
              role="menuitem"
              class="action-bar__item"
              disabled={busy || act.disabled}
              title={act.disabled ? act.disabledReason : act.advisory}
              on:click={() => choose(act.id)}
            >
              {act.label}
            </button>
          {/each}
        </div>
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

  .action-bar__item:disabled {
    opacity: 0.5;
    cursor: not-allowed;
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
