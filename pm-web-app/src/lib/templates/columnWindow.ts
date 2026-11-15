/**
 * The column window of the navigation design (`doc/design/web-app/navigation.adoc#window`): the
 * rightmost columns that fit a container, as one pure function.
 *
 * The rule is the design's own: the largest number of columns `n` from two to four for which
 * `(n − 1) × 220 + 480 + 136 ≤ W`; where no such `n` exists but two fit with the strip of marks
 * alone (`220 + 480 + 44 ≤ W`), two with marks; otherwise one column and no strip. The list columns
 * share the room the object column and the strip leave, never above `320 px`; the object column
 * takes the rest. With no level hidden there is no strip, and its width goes to the columns.
 *
 * `W` is the template's container less the page inset — never the viewport, so the rail's choice
 * counts (`navigation.adoc#side-columns`, D6). The side column of the chat is measured on the same
 * container: `480`, or `400` where `480` would cost the window a column or drop a wizard's content
 * below `480`; where the object and the side column do not fit side by side, the side column takes
 * the space and the object becomes a hidden level.
 *
 * A peek — a mark of the gap strip selected — starts the window at the peeked level and stretches
 * the rightmost visible column into the room the strip frees; the levels then hidden on the right
 * are represented by a strip of marks at the right edge.
 */

/** The narrowest a list column may be. */
export const LIST_MIN = 220;

/** The widest a list column may be. */
export const LIST_MAX = 320;

/** The narrowest the object column may be. */
export const OBJECT_MIN = 480;

/** The width of the gap strip with its names. */
export const STRIP_NAMES = 136;

/** The width of the gap strip with marks only. */
export const STRIP_MARKS = 44;

/** The side column's width where the window keeps its columns. */
export const SIDE_WIDE = 480;

/** The side column's width where `SIDE_WIDE` would cost the window a column. */
export const SIDE_NARROW = 400;

/** The widest window, in columns. */
export const MAX_COLUMNS = 4;

/** The page inset on each side of the container. */
const PAGE_INSET = 24;

/** The page inset on each side below `INSET_BELOW`, where the phone takes the narrow inset. */
const PHONE_INSET = 12;

/** The container width below which the page takes the narrow inset. */
const INSET_BELOW = 480;

/**
 * The width a container is assumed to have where no layout engine reports one (a test environment
 * without layout): the desktop measure the design's tables start from.
 */
export const MEASURE_FALLBACK = 1024;

/**
 * @param container the container's width in px
 * @returns the page inset on each side the width calls for
 */
export function pageInset(container: number): number {
  return container < INSET_BELOW ? PHONE_INSET : PAGE_INSET;
}

/** What stands for the levels a window hides: nothing, their names, or their marks only. */
export type Strip = 'none' | 'names' | 'marks';

/** What the window is computed from. */
export interface ColumnWindowInput {
  /** The template container's width in px, as its own `ResizeObserver` reports it. */
  container: number;
  /** The number of levels in the chain, one column each (the object column included). */
  depth: number;
  /** Whether the chat's side column is open. */
  sideOpen?: boolean;
  /** The index of the level a peek starts the window at; `null` or absent for no peek. */
  peek?: number | null;
}

/** The window a chain gets in a container. */
export interface ColumnWindow {
  /** The container the window was computed from. */
  container: number;
  /** The content width `W`: the container less the page inset. */
  width: number;
  /** The number of navigation columns rendered; `0` while the side column takes the space. */
  columns: number;
  /** The width of every list column; `0` with fewer than two columns. */
  listWidth: number;
  /** The rightmost column's width — the object column outside a peek, which stretches. */
  objectWidth: number;
  /** The width of each rendered column, left to right. */
  columnWidths: readonly number[];
  /** What the window shows of the levels hidden to its left. */
  strip: Strip;
  /** The width the strip takes; `0` when there is none. */
  stripWidth: number;
  /** Whether a strip of marks at the right edge returns from a peek to the object. */
  returnStrip: boolean;
  /** The side column's width; `0` when it is closed. */
  sideWidth: number;
  /** Whether the side column takes the window's space, hiding the object. */
  sideTakesSpace: boolean;
  /** The index of the leftmost rendered level. */
  start: number;
  /** How many levels are hidden to the left of the window. */
  hiddenLeft: number;
  /** How many levels are hidden to the right of the window (a peek's object included). */
  hiddenRight: number;
  /** Whether the window is shifted by a peek. */
  peek: boolean;
}

/** The window's navigation columns, before the side column is laid beside them. */
interface NavWindow {
  /** The rendered columns. */
  columns: number;
  /** The list columns' common width. */
  listWidth: number;
  /** The width of each rendered column, left to right. */
  columnWidths: number[];
  /** The width of the rightmost column. */
  rightWidth: number;
  /** What the window shows of the hidden levels. */
  strip: Strip;
  /** The width the strip takes. */
  stripWidth: number;
  /** Whether a return strip stands at the right edge. */
  returnStrip: boolean;
  /** The leftmost rendered level's index. */
  start: number;
  /** The levels hidden to the left. */
  hiddenLeft: number;
  /** The levels hidden to the right. */
  hiddenRight: number;
  /** Whether a peek shifted the window. */
  peek: boolean;
}

/** The empty window of a chain the side column hides, or of a chain with no level. */
const EMPTY: NavWindow = Object.freeze({
  columns: 0,
  listWidth: 0,
  columnWidths: [],
  rightWidth: 0,
  strip: 'none' as Strip,
  stripWidth: 0,
  returnStrip: false,
  start: 0,
  hiddenLeft: 0,
  hiddenRight: 0,
  peek: false,
});

/**
 * @param container the container's width in px
 * @returns the content width `W`: the container less the page inset the width calls for
 */
export function contentWidth(container: number): number {
  return Math.max(0, Math.round(container) - 2 * pageInset(container));
}

/**
 * @param value a number a caller hands in
 * @returns it as a whole, non-negative pixel count
 */
function pixels(value: number): number {
  return Number.isFinite(value) ? Math.max(0, Math.round(value)) : 0;
}

/**
 * Chooses the window's columns and strip for a width and a chain.
 *
 * @param available the width the navigation columns and their strip may take
 * @param depth the number of levels in the chain
 * @returns the column count and the strip they leave
 */
function chooseWindow(available: number, depth: number): { columns: number; strip: Strip } {
  for (let candidate = Math.min(MAX_COLUMNS, depth); candidate >= 2; candidate--) {
    // A hidden level means a strip; with no level hidden its width goes to the columns.
    const stripWidth = depth > candidate ? STRIP_NAMES : 0;
    if ((candidate - 1) * LIST_MIN + OBJECT_MIN + stripWidth <= available) {
      return { columns: candidate, strip: stripWidth > 0 ? 'names' : 'none' };
    }
  }

  if (depth > 2 && LIST_MIN + OBJECT_MIN + STRIP_MARKS <= available) {
    return { columns: 2, strip: 'marks' };
  }

  return { columns: 1, strip: 'none' };
}

/**
 * @param columns the rendered columns
 * @param available the width the navigation columns and their strip may take
 * @param stripWidth the width the left strip takes
 * @returns the width of every list column
 */
function listWidthFor(columns: number, available: number, stripWidth: number): number {
  if (columns === 1) {
    return 0;
  }
  return Math.min(LIST_MAX, Math.floor((available - stripWidth - OBJECT_MIN) / (columns - 1)));
}

/**
 * @param columns the rendered columns
 * @param listWidth the list columns' common width
 * @param room the width the columns share of
 * @returns the width of each rendered column, left to right
 */
function widthsFor(columns: number, listWidth: number, room: number): number[] {
  return Array.from({ length: columns }, (_, index) =>
    index === columns - 1 ? room - listWidth * (columns - 1) : listWidth,
  );
}

/**
 * Lays the navigation columns of a chain into a width the side column leaves.
 *
 * @param available the width the navigation columns and their strip may take
 * @param depth the number of levels in the chain
 * @param peek the level a peek starts the window at, already validated, or `null`
 * @returns the navigation window
 */
function navWindow(available: number, depth: number, peek: number | null): NavWindow {
  if (depth <= 0 || available <= 0) {
    return EMPTY;
  }

  const chosen = chooseWindow(available, depth);
  const listWidth = listWidthFor(chosen.columns, available, stripWidthOf(chosen.strip));

  // A peek that would leave fewer levels than the window's columns is no peek, and neither is one
  // that ends at the object: with no level hidden to its right the window is the object's own, with
  // its strip of hidden levels and its width. So a resize while peeked never renders a short window,
  // and a peek that reaches the object never stretches it into the room the strip freed.
  const peeked = peek !== null && chosen.columns > 1 && peek + chosen.columns < depth ? peek : null;

  if (peeked === null) {
    const start = Math.max(0, depth - chosen.columns);
    const room = available - stripWidthOf(chosen.strip);
    const widths = widthsFor(chosen.columns, listWidth, room);

    return {
      columns: chosen.columns,
      listWidth,
      columnWidths: widths,
      rightWidth: widths.at(-1) ?? 0,
      strip: chosen.strip,
      stripWidth: stripWidthOf(chosen.strip),
      returnStrip: false,
      start,
      hiddenLeft: start,
      hiddenRight: 0,
      peek: false,
    };
  }

  // A peek keeps the window's columns and their widths, drops the left strip, and gives the room
  // the strip freed to the rightmost visible column — the peek's own content.
  const hiddenRight = depth - (peeked + chosen.columns);
  const stripWidth = hiddenRight > 0 ? STRIP_MARKS : 0;
  const widths = widthsFor(chosen.columns, listWidth, available - stripWidth);

  return {
    columns: chosen.columns,
    listWidth,
    columnWidths: widths,
    rightWidth: widths.at(-1) ?? 0,
    strip: 'none',
    stripWidth,
    returnStrip: hiddenRight > 0,
    start: peeked,
    hiddenLeft: peeked,
    hiddenRight,
    peek: true,
  };
}

/**
 * @param strip what the window shows of the hidden levels
 * @returns the width it takes
 */
function stripWidthOf(strip: Strip): number {
  switch (strip) {
    case 'names':
      return STRIP_NAMES;
    case 'marks':
      return STRIP_MARKS;
    default:
      return 0;
  }
}

/**
 * @param input the container, the chain and the side column
 * @returns the window the chain gets in that container
 */
export function windowFor(input: ColumnWindowInput): ColumnWindow {
  const container = pixels(input.container);
  const width = contentWidth(container);
  const depth = Math.max(0, Math.floor(input.depth));
  const peek =
    input.peek !== null &&
    input.peek !== undefined &&
    Number.isInteger(input.peek) &&
    input.peek >= 0 &&
    input.peek < depth
      ? input.peek
      : null;

  const sideOpen = input.sideOpen === true;
  let sideWidth = 0;
  let sideTakesSpace = false;
  let nav: NavWindow;

  if (!sideOpen) {
    nav = navWindow(width, depth, peek);
  } else {
    const wide = navWindow(width - SIDE_WIDE, depth, peek);
    const narrow = navWindow(width - SIDE_NARROW, depth, peek);
    const wideKeepsObject = width - SIDE_WIDE >= OBJECT_MIN;

    if (wideKeepsObject) {
      // `480` costs the window a column where `400` would show one more.
      const costsColumn = narrow.columns > wide.columns;
      sideWidth = costsColumn ? SIDE_NARROW : SIDE_WIDE;
      nav = costsColumn ? narrow : wide;
    } else if (width - SIDE_NARROW >= OBJECT_MIN) {
      sideWidth = SIDE_NARROW;
      nav = narrow;
    } else {
      sideTakesSpace = true;
      sideWidth = width;
      nav = EMPTY;
    }
  }

  return {
    container,
    width,
    columns: nav.columns,
    listWidth: nav.listWidth,
    objectWidth: nav.rightWidth,
    columnWidths: nav.columnWidths,
    strip: nav.strip,
    stripWidth: nav.stripWidth,
    returnStrip: nav.returnStrip,
    sideWidth,
    sideTakesSpace,
    start: nav.start,
    hiddenLeft: nav.hiddenLeft,
    hiddenRight: nav.hiddenRight,
    peek: nav.peek,
  };
}
