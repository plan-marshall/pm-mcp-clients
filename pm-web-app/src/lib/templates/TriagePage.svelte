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
  import { tick } from 'svelte';
  import KindMark, { KIND_WORD_KEYS } from '../components/KindMark.svelte';
  import GapStrip from './GapStrip.svelte';
  import SideColumn from './SideColumn.svelte';
  import { t } from '../i18n';
  import { announceNavigation } from '../stores/runtime';
  import { MEASURE_FALLBACK, pageInset, windowFor, type ColumnWindow } from './columnWindow';

  /**
   * The column engine (`components.adoc#triage-page`): it draws the chain of levels as columns and
   * owns the column window of `navigation.adoc#window`.
   *
   * It measures its own container with a `ResizeObserver` — never the viewport, so the rail's choice
   * counts — and computes the window again on every change of the container's width. Only the
   * columns of the window are rendered: a hidden column is absent from the DOM and the accessibility
   * tree, not moved out of sight. Each column declares its own scroll (`overflow-y: auto`); the
   * height it caps against is the app shell's, which has none yet — PLAN-144 gives the app body a
   * definite height, and until then the page scrolls vertically as one. The page never scrolls
   * sideways. Widths are applied through `style:` directives (the CSSOM), which the daemon's policy
   * permits, never through a static `style` attribute (`csp.test.ts`).
   *
   * The template owns the columns, never the items: the view gives each level, fills its column's
   * body and the object column, and keeps its rows, its filter, its matches and its selection.
   *
   * Before the observer's first delivery — and where no layout engine reports a box at all — the
   * window starts from the desktop measure; the observer replaces it with the container's measured
   * width on its first report. The viewport is never the source: the container is.
   */

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
    /** The chat's side column. */
    side?: Snippet;
    /** Whether the chat's side column is shown. */
    sideOpen?: boolean;
    /** The act that hides the chat; the view replaces the address. */
    onHideSide?: () => void;
    /** The head of the first visible column, which the page fills with Up (PLAN-144). */
    up?: Snippet;
  }

  let {
    levels,
    label,
    column,
    object,
    pinned = null,
    side,
    sideOpen = false,
    onHideSide = () => {},
    up,
  }: Props = $props();

  /** The measuring frame: the template's own container. */
  let frame = $state<HTMLElement | null>(null);
  /** The window itself, which holds the rendered columns. */
  let windowEl = $state<HTMLElement | null>(null);
  /** The container's measured width; `0` until the observer's first report. */
  let measured = $state(0);
  /** The level a peek starts the window at; `null` while the window shows the object. */
  let peek = $state<number | null>(null);
  /** The chain the last shift was measured on, so a new selection ends a peek. */
  let previousChain = $state('');

  /** The container's width the window is computed from. */
  const container = $derived(measured > 0 ? measured : MEASURE_FALLBACK);

  /** The window the chain gets in that container. */
  const layout: ColumnWindow = $derived(
    windowFor({ container, depth: levels.length, peek, sideOpen: sideOpen && side !== undefined }),
  );

  /** The levels the window hides to its left, with their index in the chain. */
  const hidden = $derived(levels.slice(0, layout.start).map((level, index) => ({ level, index })));

  /** The object's title, for the return strip and the announcement. */
  const objectTitle = $derived(levels[levels.length - 1]?.title ?? '');

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
    // The pinned row belongs to the list column its selection is missing from — the level before
    // the object in the chain, not the penultimate rendered one, which a peek moves.
    const target = layout.columns === 1 ? visible[0]?.index : levels.length - 2;
    return index === target;
  }

  $effect(() => {
    const element = frame;
    if (element === null) {
      return;
    }
    // A browser lays the element out before the observer's first delivery; read that box now, so
    // the window never starts from a guess where the container can be measured.
    const initial = element.getBoundingClientRect().width;
    if (initial > 0) {
      measured = initial;
    }
    if (typeof ResizeObserver === 'undefined') {
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

  // A peek ends at the next selection: the view hands a new chain, and the window returns to the
  // object — with the focus, which never falls to the page (`navigation.adoc#keys`).
  $effect(() => {
    const chain = levels.map((level) => level.key).join('\u0000');
    if (previousChain !== '' && chain !== previousChain && peek !== null) {
      peek = null;
      void tick().then(() => {
        focusColumn(levels.length - 1);
        announce(levels.length - 1);
      });
    }
    previousChain = chain;
  });

  // A window of one column has no strip and cannot peek, so a peek is dropped there: widening the
  // container later shows the object's window again rather than the level peeked at before.
  $effect(() => {
    if (peek !== null && layout.columns <= 1) {
      peek = null;
    }
  });

  /** The element tags that hold the focus as a field, where the column keys stay with the field. */
  const FIELD_TAGS = new Set(['INPUT', 'TEXTAREA', 'SELECT']);

  /**
   * @param target the event's target
   * @returns whether the focus is in a field, where `[` and `]` are the field's own keys
   */
  function isField(target: EventTarget | null): boolean {
    const element = target as HTMLElement | null;
    return element !== null && (FIELD_TAGS.has(element.tagName) || element.isContentEditable);
  }

  /**
   * Moves the focus to the heading of a rendered column, and gives the column the focus when it
   * carries no heading of its own.
   *
   * @param index the level's index in the chain
   */
  function focusColumn(index: number): void {
    const column = windowEl?.querySelector<HTMLElement>(`[data-column-index="${index}"]`);
    if (!column) {
      return;
    }
    const heading = column.querySelector<HTMLElement>('h1, h2, h3, h4, h5, h6');
    const target = heading ?? column;
    if (!target.hasAttribute('tabindex')) {
      target.setAttribute('tabindex', '-1');
    }
    target.focus();
  }

  /** What the last shift emitted, so an identical shift is announced again. */
  let lastAnnouncement = '';

  /**
   * @param index the level's index in the chain
   */
  function announce(index: number): void {
    const level = levels[index];
    if (level === undefined) {
      return;
    }
    const text = $t('web.nav.announce', {
      name: level.title,
      kind: $t(KIND_WORD_KEYS[level.kind]),
    });
    // A polite region re-announces only a changed string; a zero-width space keeps two identical
    // announcements apart, and the alternating form keeps the third one apart as well.
    lastAnnouncement = lastAnnouncement === text ? `${text}\u200B` : text;
    announceNavigation(lastAnnouncement);
  }

  /**
   * Peeks at a level: the window starts there, the focus moves to the column now in view and the
   * shift is announced. No selection, no address and no history change.
   *
   * @param index the level the window starts at
   * @param focusIndex the level whose column takes the focus; the peeked level by default
   */
  async function peekAt(index: number, focusIndex = index): Promise<void> {
    peek = index;
    await tick();
    focusColumn(focusIndex);
    announce(focusIndex);
  }

  /**
   * Returns from a peek to the object: the window ends at the object again, the focus moves to the
   * object's heading and the shift is announced.
   */
  async function returnToObject(): Promise<void> {
    peek = null;
    await tick();
    focusColumn(levels.length - 1);
    announce(levels.length - 1);
  }

  /**
   * @returns the index of the column that holds the focus, or `null` when none does
   */
  function focusedColumn(): number | null {
    const active = document.activeElement as HTMLElement | null;
    const column = active?.closest<HTMLElement>('[data-column-index]');
    return column === null || column === undefined ? null : Number(column.dataset.columnIndex);
  }

  /**
   * Moves the focus to the neighbour column, shifting the window where that column is hidden.
   *
   * @param direction `-1` for the column on the left, `1` for the one on the right
   */
  async function moveFocus(direction: -1 | 1): Promise<void> {
    // A window of one column has no strip and cannot shift, so the keys stay with the page.
    if (layout.columns <= 1) {
      return;
    }
    const current = focusedColumn() ?? layout.start;
    const next = current + direction;
    if (next < 0 || next > levels.length - 1) {
      return;
    }
    if (next < layout.start) {
      await peekAt(next);
      return;
    }
    if (next >= layout.start + layout.columns) {
      if (layout.peek) {
        // The window shifts one column right; the focus stays on the column it asked for.
        await peekAt(next - layout.columns + 1, next);
      }
      return;
    }
    focusColumn(next);
    announce(next);
  }

  /** The class that replays a shift once; it is dropped again when the animation ends. */
  /** Whether a shift is being slid right now. */
  let shifting = $state(false);

  /** The start the last slide ran for, so a first render and an unchanged start do not slide. */
  let slidStart = -1;

  /**
   * @returns whether the person asked for reduced motion
   */
  function reducedMotion(): boolean {
    return globalThis.matchMedia?.('(prefers-reduced-motion: reduce)').matches === true;
  }

  // A shift slides the columns, never the group that holds them: the chat's phone overlay is
  // `position: fixed`, and a transformed ancestor would make it the overlay's containing block
  // instead of the viewport. Nothing is remounted for it — a column's scroll position, a chat draft
  // and the focused control stay where they are.
  $effect(() => {
    const start = layout.start;
    if (slidStart === start) {
      return;
    }
    const first = slidStart === -1;
    slidStart = start;
    if (first || reducedMotion()) {
      return;
    }
    shifting = true;
  });

  /** Ends the slide of a shift, so the next one replays the animation from its start. */
  function onAnimationEnd(): void {
    shifting = false;
  }

  /** Whether the chat held the space at the last update, so a takeover is seen once. */
  let sideTaken = false;
  /** The control the chat took the focus from, while it is still in the document. */
  let rememberedFocus: HTMLElement | null = null;
  /** The column that held the focus when the chat took the space, which it unmounts. */
  let rememberedIndex: number | null = null;
  /** The last control of the window that held the focus, and the column it stood in. */
  let lastControl: HTMLElement | null = null;
  let lastColumn: number | null = null;

  // The focus a takeover gives back is the one from before it, and the columns are gone from the
  // document by the time the chat holds the space: so the focus is tracked as it moves.
  $effect(() => {
    const element = frame;
    if (element === null) {
      return;
    }
    const track = (): void => {
      const active = document.activeElement as HTMLElement | null;
      if (active === null || !element.contains(active)) {
        return;
      }
      lastControl = active;
      lastColumn = focusedColumn();
    };
    element.addEventListener('focusin', track);
    return () => element.removeEventListener('focusin', track);
  });

  // The focus never falls to the page when the chat takes the space: it moves into the chat, and it
  // returns to the control it left when the columns come back — as `WizardPage` does
  // (`navigation.adoc#side-columns`, `#keys`).
  $effect(() => {
    const taken = layout.sideTakesSpace;
    if (taken === sideTaken) {
      return;
    }
    sideTaken = taken;
    if (taken) {
      rememberedFocus = lastControl;
      rememberedIndex = lastColumn;
      windowEl?.querySelector<HTMLElement>('[data-testid="side-column"] button')?.focus();
      return;
    }
    const remembered = rememberedFocus;
    const index = rememberedIndex;
    rememberedFocus = null;
    rememberedIndex = null;
    if (remembered === null && index === null) {
      return;
    }
    // The focus returns only while it is still the template's: a person who moved to the shell keeps
    // it there.
    const active = document.activeElement as HTMLElement | null;
    const stillOurs =
      active === null || active === document.body || frame?.contains(active) === true;
    if (!stillOurs) {
      return;
    }
    if (remembered !== null && remembered.isConnected) {
      remembered.focus();
    } else if (index !== null) {
      // The columns unmount under the chat and come back as new elements, so the column that held
      // the focus stands in for the control it carried.
      focusColumn(index);
    }
  });

  /**
   * @param event the key press on the window or one of its columns
   */
  function onKeydown(event: KeyboardEvent): void {
    // An Escape the page's own dialog or menu already handled stays its key.
    if (event.defaultPrevented) {
      return;
    }
    if (event.key === 'Escape' && sideOpen && side !== undefined) {
      event.preventDefault();
      onHideSide();
      return;
    }
    if (isField(event.target)) {
      return;
    }
    // The brackets belong to the app only as the bare key: Cmd+[ and Cmd+] are the browser's Back
    // and Forward, and a German keyboard types `[` as AltGr+8. The arrows take Ctrl alone, never
    // with Alt or Meta.
    const bare = !event.ctrlKey && !event.altKey && !event.metaKey;
    const ctrlOnly = event.ctrlKey && !event.altKey && !event.metaKey;
    const back = (event.key === '[' && bare) || (event.key === 'ArrowLeft' && ctrlOnly);
    const forward = (event.key === ']' && bare) || (event.key === 'ArrowRight' && ctrlOnly);
    if (!back && !forward) {
      return;
    }
    event.preventDefault();
    void moveFocus(back ? -1 : 1);
  }
</script>

<div
  class="triage-page"
  data-testid="triage-page"
  style:--triage-inset={`${pageInset(container)}px`}
  bind:this={frame}
>
  <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
  <div
    class="triage-page__window"
    role="group"
    aria-label={label}
    data-columns={layout.columns}
    data-start={layout.start}
    bind:this={windowEl}
    onkeydown={onKeydown}
  >
    {#if layout.strip !== 'none'}
      <GapStrip
        {hidden}
        names={layout.strip === 'names'}
        label={$t('web.nav.strip.label')}
        width={layout.stripWidth}
        onPeek={(index) => void peekAt(index)}
      />
    {/if}

    {#each visible as entry (entry.level.key)}
      <section
        class="triage-page__column"
        class:triage-page__column--shift={shifting}
        data-column-index={entry.index}
        data-testid={isObjectColumn(entry.index) ? 'triage-detail' : 'triage-list'}
        aria-label={entry.level.title}
        onanimationend={onAnimationEnd}
        style:inline-size={`${layout.columnWidths[entry.index - layout.start] ?? layout.objectWidth}px`}
      >
        {#if up && entry.index === layout.start}
          <div class="triage-page__up">{@render up()}</div>
        {/if}

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

    {#if layout.returnStrip}
      <GapStrip
        placement="end"
        label={$t('web.nav.strip.label')}
        {objectTitle}
        width={layout.stripWidth}
        onReturn={() => void returnToObject()}
      />
    {/if}

    {#if sideOpen && side}
      <SideColumn
        {side}
        width={layout.sideWidth}
        takesSpace={layout.sideTakesSpace}
        onHide={onHideSide}
      />
    {/if}
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
   * The window lays its columns in one row inside the page inset. Every column is as wide as the
   * window computed it and shrinks to nothing rather than widening the page: the sum of the columns
   * and the strip is the template's content width, so the page never scrolls sideways.
   */
  .triage-page__window {
    display: flex;
    flex-wrap: nowrap;
    align-items: stretch;
    box-sizing: border-box;
    inline-size: 100%;
    min-inline-size: 0;
    padding-inline: var(--triage-inset, 0);
    gap: 0;
  }

  .triage-page__column {
    flex: 0 0 auto;
    min-inline-size: 0;
    max-block-size: 100%;
    overflow-y: auto;
    overflow-x: hidden;
  }

  /*
   * A shift slides the columns, and only for one animation: the class is added when the window's
   * start changes and dropped on `animationend`, so a column keeps its scroll position, the chat its
   * draft and the window the focus. The group that holds the chat is never animated — its phone
   * overlay is `position: fixed` and needs the viewport as its containing block.
   */
  .triage-page__column--shift {
    animation: triage-page-shift var(--duration-base) var(--ease-standard);
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

  .triage-page__up {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin-block-end: var(--space-3);
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
    .triage-page__column--shift {
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
