<script lang="ts">
  import type { Snippet } from 'svelte';
  import { isAppPath } from './Link.svelte';

  /**
   * A row that opens the object it stands for: its title is the link and the link's target is
   * stretched over the whole row, so the row takes the hover, the pointer and the focus ring and a
   * click anywhere opens the object.
   *
   * The controls inside the row — its `RowMenu`, a favourite `Toggle`, a copy `Chip` — lie above the
   * link as targets of their own and never open it. The selected row of a list carries
   * `aria-current="true"` and the brand-subtle ground.
   *
   * `href` is guarded by the exported `isAppPath` of `Link.svelte`: an address that is not a path of
   * the app draws the title as text, never as an anchor. The controls are stacked above the stretched
   * target by their own positioned box, not by a numeric layer: the layer tokens are theirs to come.
   */
  interface Props {
    /** The place the row opens. */
    href: string;
    /** The row's title, which is the link's text. */
    title: string;
    /** Whether this is the selected row of a list. */
    current?: boolean;
    /** The row's compact height, 32 px instead of 40 px. */
    compact?: boolean;
    /** The row's kind mark, first. */
    kind?: Snippet;
    /** The detail line: a chip, a phase, a state, a time. */
    tags?: Snippet;
    /** The row's acts, the last element. */
    acts?: Snippet;
  }

  let { href, title, current = false, compact = false, kind, tags, acts }: Props = $props();

  /** The address that was checked and is rendered, without the white space around it. */
  const address = $derived(href.trim());
  const opens = $derived(isAppPath(address));
</script>

<div
  class="row-link"
  class:row-link--current={current}
  class:row-link--compact={compact}
  aria-current={current ? 'true' : undefined}
>
  {#if kind}<span class="row-link__kind">{@render kind()}</span>{/if}
  {#if opens}
    <a class="row-link__title" href={address}>{title}</a>
  {:else}
    <span class="row-link__title">{title}</span>
  {/if}
  {#if tags}<span class="row-link__tags">{@render tags()}</span>{/if}
  {#if acts}<span class="row-link__acts">{@render acts()}</span>{/if}
</div>

<style>
  /*
   * A row of 40 px, 32 px compact, and 44 px on a phone (components.adoc, States). It is one target:
   * the title's stretched target covers it, and the controls inside sit above that target.
   */
  .row-link {
    position: relative;
    display: flex;
    align-items: center;
    gap: var(--space-2);
    min-block-size: 40px;
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-sm);
    color: var(--color-text-primary);
    cursor: pointer;
  }

  .row-link:hover {
    background-color: var(--color-bg-subtle);
  }

  /* The whole row carries the ring while the title link inside it has the focus. */
  .row-link:focus-within {
    outline: none;
    box-shadow: var(--focus-ring);
  }

  .row-link--current {
    background-color: var(--color-brand-subtle);
  }

  .row-link--compact {
    min-block-size: 32px;
  }

  .row-link__kind {
    display: inline-flex;
    flex: 0 0 auto;
  }

  .row-link__title {
    color: inherit;
    text-decoration: none;
  }

  /* The title's target, stretched over the whole row. */
  .row-link__title::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: var(--radius-sm);
  }

  .row-link__tags {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    margin-inline-start: auto;
    color: var(--color-text-secondary);
  }

  /*
   * The controls lie above the link's stretched target: a positioned box paints after the target in
   * document order, so the row menu, the favourite and the chip stay targets of their own. No numeric
   * layer is written here; the layer tokens are owned elsewhere.
   */
  .row-link__acts {
    position: relative;
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    flex: 0 0 auto;
  }

  @media (max-width: 767px) {
    .row-link {
      min-block-size: 44px;
    }
  }
</style>
