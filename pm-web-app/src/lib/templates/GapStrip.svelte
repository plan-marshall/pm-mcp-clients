<script lang="ts">
  import KindMark, { KIND_WORD_KEYS, type Kind } from '../components/KindMark.svelte';
  import Icon from '../components/Icon.svelte';
  import { t } from '../i18n';
  import { uniqueId } from '../utils/uniqueId';

  /**
   * The gap strip of `navigation.adoc#gap-strip`: at the left edge, the levels the window hides,
   * from the lens down, as stacked `KindMark`s with their names visible — or, where the names would
   * leave a single column, as marks only behind a _Names_ popover that lists the levels one step
   * each. Every mark is a button of the group whose accessible name is its kind and its name; a mark
   * peeks at its level and changes no selection.
   *
   * At the right edge it is a strip of marks only: one button that returns from a peek to the
   * object. Both placements are the same component, because they are the same piece of the design.
   */
  interface Props {
    /** The levels the window hides, left to right, with their index in the chain. */
    hidden?: Array<{ level: { kind: Kind; title: string }; index: number }>;
    /** Whether the names stand beside the marks (`false` shows marks only, behind the popover). */
    names?: boolean;
    /** The accessible name of the strip's group. */
    label: string;
    /** Whether this is the strip at the left edge or the one that returns from a peek. */
    placement?: 'start' | 'end';
    /** The object's title, for the return strip's name. */
    objectTitle?: string;
    /** The width the window gives the strip. */
    width?: number;
    /** Called with the index of the level a mark peeks at. */
    onPeek?: (index: number) => void;
    /** Called when the return strip is selected. */
    onReturn?: () => void;
  }

  let {
    hidden = [],
    names = true,
    label,
    placement = 'start',
    objectTitle = '',
    width = 136,
    onPeek = () => {},
    onReturn = () => {},
  }: Props = $props();

  /** Whether the popover of the hidden levels' names is open. */
  let popoverOpen = $state(false);
  /** The popover the Names control owns, for `aria-controls` and the focus. */
  const popoverId = uniqueId('gap-strip-popover');
  let namesButton = $state<HTMLButtonElement | null>(null);
  let popover = $state<HTMLElement | null>(null);

  /**
   * Opens or closes the popover of names, and moves the focus with it.
   */
  async function togglePopover(): Promise<void> {
    popoverOpen = !popoverOpen;
    if (popoverOpen) {
      requestAnimationFrame(() => popover?.querySelector<HTMLElement>('button')?.focus());
    } else {
      namesButton?.focus();
    }
  }

  /**
   * @param index the index of a hidden level
   */
  function peek(index: number): void {
    popoverOpen = false;
    onPeek(index);
  }

  /**
   * @param event the key press in the strip
   */
  function onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && popoverOpen) {
      event.preventDefault();
      event.stopPropagation();
      popoverOpen = false;
      namesButton?.focus();
    }
  }
</script>

{#if placement === 'end'}
  <div
    class="gap-strip gap-strip--end"
    role="group"
    aria-label={label}
    style:inline-size={`${width}px`}
  >
    <button
      type="button"
      class="gap-strip__return"
      aria-label={$t('web.nav.strip.return', { name: objectTitle })}
      onclick={() => onReturn()}
    >
      <Icon name="act.back" size={16} />
    </button>
  </div>
{:else}
  <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
  <div
    class="gap-strip gap-strip--start"
    role="group"
    aria-label={label}
    style:inline-size={`${width}px`}
    onkeydown={onKeydown}
  >
    {#each hidden as entry (entry.index)}
      <button
        type="button"
        class="gap-strip__mark"
        class:gap-strip__mark--names={names}
        aria-label={`${$t(KIND_WORD_KEYS[entry.level.kind])}, ${entry.level.title}`}
        title={names ? undefined : $t('web.nav.strip.mark.title')}
        onclick={() => peek(entry.index)}
      >
        <KindMark kind={entry.level.kind} wordHidden={!names} />
        {#if names}
          <span class="gap-strip__name">{entry.level.title}</span>
        {/if}
      </button>
    {/each}

    {#if !names}
      <button
        bind:this={namesButton}
        type="button"
        class="gap-strip__names"
        aria-label={$t('web.nav.strip.names')}
        aria-expanded={popoverOpen}
        aria-controls={popoverId}
        onclick={() => void togglePopover()}
      >
        <Icon name="act.more" size={16} />
      </button>
      {#if popoverOpen}
        <div
          bind:this={popover}
          id={popoverId}
          class="gap-strip__popover"
          role="group"
          aria-label={$t('web.nav.strip.names')}
        >
          {#each hidden as entry (entry.index)}
            <button
              type="button"
              class="gap-strip__step"
              aria-label={entry.level.title}
              onclick={() => peek(entry.index)}
            >
              <KindMark kind={entry.level.kind} wordHidden />
              <span>{entry.level.title}</span>
            </button>
          {/each}
        </div>
      {/if}
    {/if}
  </div>
{/if}

<style>
  .gap-strip {
    display: flex;
    flex-direction: column;
    flex: 0 0 auto;
    gap: var(--space-1);
    min-inline-size: 0;
    padding-block: var(--space-1);
  }

  /* The popover of names stands over the columns, anchored at the strip, so the 44 px strip never
     clips the names it opens. */
  .gap-strip--start {
    position: relative;
    /* The strip is only as tall as its marks, so the popover opens under the Names control rather
       than under the tallest column. */
    align-self: flex-start;
  }

  .gap-strip__mark,
  .gap-strip__step {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    min-block-size: 34px;
    padding: var(--space-1) var(--space-2);
    border: 1px solid transparent;
    border-radius: var(--radius-md);
    background-color: transparent;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    text-align: start;
    cursor: pointer;
  }

  .gap-strip__mark:hover,
  .gap-strip__step:hover {
    background-color: var(--color-bg-subtle);
    color: var(--color-text-primary);
  }

  .gap-strip__mark:focus-visible,
  .gap-strip__step:focus-visible,
  .gap-strip__names:focus-visible,
  .gap-strip__return:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }

  /* Marks only: the name is out of sight, and the mark stays the whole control. */
  .gap-strip__mark:not(.gap-strip__mark--names) {
    justify-content: center;
    inline-size: 34px;
    padding: var(--space-1);
  }

  .gap-strip__name {
    min-inline-size: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .gap-strip__names {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    align-self: stretch;
    inline-size: 34px;
    block-size: 34px;
    padding: 0;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    cursor: pointer;
  }

  .gap-strip__popover {
    position: absolute;
    inset-block-start: 100%;
    inset-inline-start: 0;
    z-index: 20;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    inline-size: 14rem;
    max-inline-size: 80vw;
    padding: var(--space-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    box-shadow: var(--shadow-2);
  }

  .gap-strip--end {
    align-items: center;
  }

  .gap-strip__return {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    inline-size: 34px;
    block-size: 34px;
    padding: 0;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    cursor: pointer;
  }

  .gap-strip__return:hover {
    color: var(--color-text-primary);
  }

  @media (max-width: 767px) {
    .gap-strip__mark,
    .gap-strip__names,
    .gap-strip__return,
    .gap-strip__step {
      min-block-size: 44px;
      min-inline-size: 44px;
    }
  }
</style>
