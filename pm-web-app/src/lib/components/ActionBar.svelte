<script context="module" lang="ts">
  import type { ActMenuItem } from './ActMenu.svelte';

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
  export interface ActionBarAct extends ActMenuItem {
    /** Where the act sits; without a role the order decides, as before. */
    role?: 'primary' | 'secondary' | 'overflow';
  }
</script>

<script lang="ts">
  import { tick } from 'svelte';
  import Button from './Button.svelte';
  import IconButton from './IconButton.svelte';
  import ActMenu from './ActMenu.svelte';
  import { t } from '../i18n';

  /** The acts, most important first. */
  export let acts: ActionBarAct[] = [];
  /** Whether the whole bar waits for a running request. */
  export let busy = false;
  /** Reports the act a person chose, by its id. */
  export let onAct: (id: string) => void = () => {};

  /** How many secondary acts the bar shows before it overflows. */
  const MAX_SECONDARY = 3;

  let open = false;
  let overflowButton: HTMLElement | null = null;
  let actMenu: ActMenu | null = null;

  /** Whether the caller named the roles rather than only the order. */
  $: hasRoles = acts.some((act) => act.role !== undefined);
  $: primary = hasRoles
    ? acts.find((act) => act.role === 'primary' && !act.destructive)
    : acts.find((act) => !act.destructive);
  $: rest = acts.filter((act) => act !== primary);
  $: secondary = rest
    .filter((act) => (hasRoles ? act.role === 'secondary' && !act.destructive : !act.destructive))
    .slice(0, MAX_SECONDARY);
  $: overflow = rest.filter((act) => !secondary.includes(act));

  async function handleTriggerClick(event: CustomEvent | MouseEvent): Promise<void> {
    overflowButton = (event.currentTarget as HTMLElement) ?? null;
    if (open) {
      open = false;
    } else {
      open = true;
      await tick();
      actMenu?.focusFirst();
    }
  }

  function chooseAct(id: string, target?: HTMLElement | null): void {
    target?.focus();
    onAct(id);
  }
</script>

{#if acts.length > 0}
  <div class="action-bar" data-testid="action-bar">
    {#if primary}
      <Button
        variant="primary"
        disabled={busy || primary.disabled}
        disabledReason={primary.disabledReason}
        title={primary.advisory}
        data-testid={`action-${primary.id}`}
        on:click={(event) => chooseAct(primary.id, event.currentTarget as HTMLElement)}
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
        on:click={(event) => chooseAct(act.id, event.currentTarget as HTMLElement)}
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
          on:click={handleTriggerClick}
        />
        <ActMenu
          bind:this={actMenu}
          bind:open
          items={overflow}
          label={$t('web.common.more')}
          {busy}
          confirmDestructive={false}
          triggerElement={overflowButton}
          onAct={(item) => onAct(item.id)}
          onClose={() => { open = false; }}
        />
      </div>
    {/if}
  </div>
{/if}

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
