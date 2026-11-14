<script module lang="ts">
  import type { Kind } from '../components/KindMark.svelte';

  /** One level of the chain of levels a page shows: lens list › project › epic › plan › task. */
  export interface Level {
    /** The level's identity, used as the key of its column. */
    key: string;
    /** The level's kind, for the strip and the column's own head. */
    kind: Kind;
    /** The level's title, as the column names it. */
    title: string;
    /** The level's own address, so selecting its head closes the child. */
    href: string;
    /** The level's state word. */
    state: string;
  }

  /** Why a selection the list does not hold is pinned (`navigation.adoc#missing`). */
  export type PinnedReason = 'ended' | 'read' | 'resolved' | 'filtered' | 'page';

  /** A selection the list does not hold, shown as one pinned row at the top of its column. */
  export interface PinnedRow {
    /** The selected object. */
    level: Level;
    /** The reason the list does not hold it. */
    reason: PinnedReason;
  }
</script>

<script lang="ts">
  import type { Snippet } from 'svelte';
  import KindMark from '../components/KindMark.svelte';
  import { t } from '../i18n';
  import { pageInset, windowFor, type ColumnWindow } from './columnWindow';

  /**
   * The column engine (`components.adoc#triage-page`): it draws the chain of levels as columns and
   * owns the column window of `navigation.adoc#window`.
   *
   * It measures its own container with a `ResizeObserver` — never the viewport, so the rail's choice
   * counts — and computes the window again on every change of the container's width. Only the
   * columns of the window are rendered: a hidden column is absent from the DOM and the accessibility
   * tree, not moved out of sight. Each column scrolls on its own and the page never scrolls
   * sideways. Widths are applied through `style:` directives (the CSSOM), which the daemon's policy
   * permits, never through a static `style` attribute (`csp.test.ts`).
   *
   * The template owns the columns, never the items: the view gives each level, fills its column's
   * body and the object column, and keeps its rows, its filter, its matches and its selection.
   *
   * Before the observer's first delivery — and where no layout engine reports a box at all — the
   * window starts from the page's own width and is replaced by the measured container width on the
   * first report.
   */
  /** The width a container is assumed to have before the observer's first report. */
  const PROVISIONAL_WIDTH = typeof window === 'undefined' ? 1024 : window.innerWidth;

  interface Props {
    /** The chain of levels, the object column last. */
    levels: Level[];
    /** The accessible name of the window. */
    label: string;
    /** The body of a list column, called once per rendered level. */
    column: Snippet<[Level]>;
    /** The body of the object column; absent while nothing is selected. */
    object?: Snippet<[Level]>;
    /** A selection the list does not hold, shown as one pinned row with its reason. */
    pinned?: PinnedRow | null;
  }

  let { levels, label, column, object, pinned = null }: Props = $props();

  /** The measuring frame: the template's own container. */
  let frame = $state<HTMLElement | null>(null);
  /** The container's measured width; `0` until the observer's first report. */
  let measured = $state(0);

  /** The container's width the window is computed from. */
  const container = $derived(measured > 0 ? measured : PROVISIONAL_WIDTH);

  /** The window the chain gets in that container. */
  const layout: ColumnWindow = $derived(windowFor({ container, depth: levels.length }));

  /** The level indices the window renders, left to right. */
  const visible = $derived(
    levels
      .map((level, index) => ({ level, index }))
      .slice(layout.start, layout.start + layout.columns),
  );

  /** The last level's index: the object column while it is in the window. */
  const lastIndex = $derived(levels.length - 1);

  /**
   * @param index a level index
   * @returns whether the column is the object column rather than a list column
   */
  function isObjectColumn(index: number): boolean {
    return object !== undefined && index === lastIndex;
  }

  /**
   * @param index a level index
   * @returns whether the pinned row stands at the top of that column
   */
  function carriesPinned(index: number): boolean {
    if (pinned === null || !visible.some((entry) => entry.index === index)) {
      return false;
    }
    // The pinned row belongs to the list column its selection is missing from; with one column it
    // stands at the top of that column.
    const target = layout.columns === 1 ? visible[0] : visible[visible.length - 2];
    return index === target?.index;
  }

  $effect(() => {
    const element = frame;
    if (element === null || typeof ResizeObserver === 'undefined') {
      return;
    }
    const observer = new ResizeObserver((entries) => {
      const entry = entries[entries.length - 1];
      if (entry) {
        measured = entry.contentRect.width;
      }
    });
    observer.observe(element);
    return () => observer.disconnect();
  });
</script>

<div
  class="triage-page"
  data-testid="triage-page"
  style:padding-inline={`${pageInset(container)}px`}
  bind:this={frame}
>
  <div
    class="triage-page__window"
    role="group"
    aria-label={label}
    data-columns={layout.columns}
    data-start={layout.start}
  >
    {#each visible as entry (entry.level.key)}
      <section
        class="triage-page__column"
        class:triage-page__column--object={isObjectColumn(entry.index)}
        data-column-index={entry.index}
        data-testid={isObjectColumn(entry.index) ? 'triage-detail' : 'triage-list'}
        aria-label={entry.level.title}
        style:inline-size={`${layout.columnWidths[entry.index - layout.start] ?? layout.objectWidth}px`}
      >
        {#if carriesPinned(entry.index) && pinned !== null}
          <p class="triage-page__pinned" data-testid="triage-pinned">
            <KindMark kind={pinned.level.kind} />
            <span class="triage-page__pinned-title">{pinned.level.title}</span>
            <span class="triage-page__pinned-reason">{$t(`web.nav.pinned.${pinned.reason}`)}</span>
          </p>
        {/if}

        {#if isObjectColumn(entry.index) && object}
          {@render object(entry.level)}
        {:else}
          {@render column(entry.level)}
        {/if}
      </section>
    {/each}
  </div>
</div>

<style>
  .triage-page {
    inline-size: 100%;
    min-inline-size: 0;
    padding-block: var(--space-6);
    overflow-x: hidden;
  }

  /*
   * The window lays its columns in one row. Every column is as wide as the window computed it and
   * shrinks to nothing rather than widening the page: the sum of the columns and the strip is the
   * template's content width, so the page never scrolls sideways.
   */
  .triage-page__window {
    display: flex;
    flex-wrap: nowrap;
    align-items: stretch;
    inline-size: 100%;
    min-inline-size: 0;
    gap: 0;
    animation: triage-page-shift var(--duration-base) var(--ease-standard);
  }

  .triage-page__column {
    flex: 0 0 auto;
    min-inline-size: 0;
    max-block-size: 100%;
    overflow-y: auto;
    overflow-x: hidden;
  }

  .triage-page__pinned {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    margin: 0 0 var(--space-3);
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .triage-page__pinned-title {
    color: var(--color-text-primary);
    font-weight: var(--weight-semibold);
  }

  @keyframes triage-page-shift {
    from {
      opacity: 0.6;
      transform: translateX(var(--space-3));
    }
    to {
      opacity: 1;
      transform: none;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .triage-page__window {
      animation: none;
      transition: none;
    }
  }

  /* The phone takes the app's narrow inset, as every view did before the template owned it. */
  @media (max-width: 480px) {
    .triage-page {
      padding-block: var(--space-3);
    }
  }
</style>
