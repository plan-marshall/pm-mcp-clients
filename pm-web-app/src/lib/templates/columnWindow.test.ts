import { describe, it, expect } from 'vitest';
import {
  contentWidth,
  windowFor,
  LIST_MAX,
  LIST_MIN,
  OBJECT_MIN,
  SIDE_NARROW,
  SIDE_WIDE,
  STRIP_MARKS,
  STRIP_NAMES,
} from './columnWindow';

/**
 * `columnWindow` is the column window of `doc/design/web-app/navigation.adoc#window` as one pure
 * function: the number of columns that fit a container, the widths of the list columns and the
 * object column, the gap strip, and the side column of the chat.
 *
 * Every cell of both width tables of the design is pinned here — `navigation.adoc:118-155` (chat
 * closed) and `:286-308` (chat open) at `4a84f727` — together with the boundaries one pixel either
 * side of the two thresholds, the `320` cap of a list column and the one-column case. Reverting a
 * constant of the rule (a width, a threshold, the inset) turns a table cell red.
 */

/** The rail's expanded and collapsed widths, from `navigation.adoc#width-table`. */
const RAIL = { expanded: 232, collapsed: 64 } as const;

/** The five levels of the deepest chain: lens › project › epic › plan › task. */
const DEEPEST_CHAIN = 5;

/**
 * @param viewport the viewport width in px
 * @param rail the rail's width in px, or `0` when the phone hides it
 * @returns the container width the rails leave, as the design's table names it
 */
function containerOf(viewport: number, rail: number): number {
  return viewport - rail;
}

/**
 * @param width the content width `W` of the design's table
 * @returns the container width that yields it (the table's `W` is the container less its inset)
 */
function containerForContent(width: number): number {
  return width + 48;
}

/** One row of a width table, as the design states it. */
interface TableRow {
  /** The viewport the row is read at. */
  viewport: number;
  /** The rail's width for the row. */
  rail: number;
  /** The number of columns the window shows. */
  columns: number;
  /** The width of each list column. */
  list: number;
  /** The width of the object column. */
  object: number;
  /** The strip the row names. */
  strip: 'none' | 'names' | 'marks';
}

/**
 * The window of the deepest chain with the chat closed (`navigation.adoc:118-155`). At `360 px` the
 * rail is hidden and the phone inset is `12 px` a side, so the table's row is the only one whose
 * container is the viewport itself.
 */
const CLOSED: readonly TableRow[] = [
  { viewport: 360, rail: 0, columns: 1, list: 0, object: 336, strip: 'none' },
  { viewport: 768, rail: RAIL.expanded, columns: 1, list: 0, object: 488, strip: 'none' },
  { viewport: 768, rail: RAIL.collapsed, columns: 1, list: 0, object: 656, strip: 'none' },
  { viewport: 1024, rail: RAIL.expanded, columns: 2, list: 220, object: 480, strip: 'marks' },
  { viewport: 1024, rail: RAIL.collapsed, columns: 2, list: 296, object: 480, strip: 'names' },
  { viewport: 1280, rail: RAIL.expanded, columns: 2, list: 320, object: 544, strip: 'names' },
  { viewport: 1280, rail: RAIL.collapsed, columns: 3, list: 276, object: 480, strip: 'names' },
  { viewport: 1440, rail: RAIL.expanded, columns: 3, list: 272, object: 480, strip: 'names' },
  { viewport: 1440, rail: RAIL.collapsed, columns: 4, list: 237, object: 481, strip: 'names' },
  { viewport: 1920, rail: RAIL.expanded, columns: 4, list: 320, object: 544, strip: 'names' },
  { viewport: 1920, rail: RAIL.collapsed, columns: 4, list: 320, object: 712, strip: 'names' },
];

/** One row of the chat-open table (`navigation.adoc:286-308`). */
interface SideRow {
  /** The viewport the row is read at. */
  viewport: number;
  /** The rail's width for the row. */
  rail: number;
  /** The side column's width. */
  side: number;
  /** Whether the side column takes the window's space. */
  takesSpace: boolean;
  /** The number of navigation columns shown beside the chat. */
  columns: number;
  /** The width of each list column. */
  list: number;
  /** The width of the object column. */
  object: number;
  /** The strip the row names. */
  strip: 'none' | 'names' | 'marks';
}

const OPEN: readonly SideRow[] = [
  {
    viewport: 1024,
    rail: RAIL.expanded,
    side: 744,
    takesSpace: true,
    columns: 0,
    list: 0,
    object: 0,
    strip: 'none',
  },
  { viewport: 1024, rail: RAIL.collapsed, side: SIDE_NARROW, takesSpace: false, columns: 1, list: 0, object: 512, strip: 'none' },
  { viewport: 1280, rail: RAIL.expanded, side: SIDE_WIDE, takesSpace: false, columns: 1, list: 0, object: 520, strip: 'none' },
  { viewport: 1280, rail: RAIL.collapsed, side: SIDE_NARROW, takesSpace: false, columns: 2, list: 244, object: 480, strip: 'marks' },
  { viewport: 1440, rail: RAIL.expanded, side: SIDE_NARROW, takesSpace: false, columns: 2, list: 236, object: 480, strip: 'marks' },
  { viewport: 1440, rail: RAIL.collapsed, side: SIDE_WIDE, takesSpace: false, columns: 2, list: 232, object: 480, strip: 'names' },
  { viewport: 1920, rail: RAIL.expanded, side: SIDE_WIDE, takesSpace: false, columns: 3, list: 272, object: 480, strip: 'names' },
  { viewport: 1920, rail: RAIL.collapsed, side: SIDE_WIDE, takesSpace: false, columns: 4, list: 237, object: 481, strip: 'names' },
];

describe('the page inset', () => {
  it('takes 24 px a side (48 px of width) at and above 480 px of container', () => {
    expect(contentWidth(480)).toBe(480 - 48);
    expect(contentWidth(1024)).toBe(1024 - 48);
  });

  it('takes 12 px a side (24 px of width) below 480 px of container', () => {
    expect(contentWidth(479)).toBe(479 - 24);
    expect(contentWidth(360)).toBe(336);
  });
});

describe('the window of the deepest chain, chat closed', () => {
  it.each(CLOSED.map((row) => [row, row] as const))(
    'shows %o as the design states it',
    (row: TableRow) => {
      const layout = windowFor({
        container: containerOf(row.viewport, row.rail),
        depth: DEEPEST_CHAIN,
      });

      expect({
        columns: layout.columns,
        list: layout.listWidth,
        object: layout.objectWidth,
        strip: layout.strip,
      }).toEqual({ columns: row.columns, list: row.list, object: row.object, strip: row.strip });
      expect(layout.sideWidth).toBe(0);
      expect(layout.sideTakesSpace).toBe(false);
    },
  );
});

describe('the window of the deepest chain, chat open', () => {
  it.each(OPEN.map((row) => [row, row] as const))(
    'shows %o as the design states it',
    (row: SideRow) => {
      const layout = windowFor({
        container: containerOf(row.viewport, row.rail),
        depth: DEEPEST_CHAIN,
        sideOpen: true,
      });

      expect({
        columns: layout.columns,
        list: layout.listWidth,
        object: layout.objectWidth,
        strip: layout.strip,
        side: layout.sideWidth,
        takesSpace: layout.sideTakesSpace,
      }).toEqual({
        columns: row.columns,
        list: row.list,
        object: row.object,
        strip: row.strip,
        side: row.side,
        takesSpace: row.takesSpace,
      });
    },
  );

  it('gives the chat the screen where the object and the chat do not fit', () => {
    const layout = windowFor({ container: 390, depth: DEEPEST_CHAIN, sideOpen: true });

    expect(layout.sideTakesSpace).toBe(true);
    expect(layout.sideWidth).toBe(contentWidth(390));
    expect(layout.columns).toBe(0);
  });
});

describe('the boundaries of the rule, one pixel either side', () => {
  it('keeps two columns with names at a content width of 836 and drops to marks at 835', () => {
    expect(windowFor({ container: containerForContent(836), depth: DEEPEST_CHAIN }).strip).toBe('names');
    expect(windowFor({ container: containerForContent(836), depth: DEEPEST_CHAIN }).columns).toBe(2);

    const below = windowFor({ container: containerForContent(835), depth: DEEPEST_CHAIN });
    expect(below.strip).toBe('marks');
    expect(below.columns).toBe(2);
  });

  it('keeps two columns with marks at a content width of 744 and one column at 743', () => {
    const at = windowFor({ container: containerForContent(744), depth: DEEPEST_CHAIN });
    expect(at.columns).toBe(2);
    expect(at.strip).toBe('marks');
    expect(at.listWidth).toBe(LIST_MIN);
    expect(at.objectWidth).toBe(OBJECT_MIN);

    const below = windowFor({ container: containerForContent(743), depth: DEEPEST_CHAIN });
    expect(below.columns).toBe(1);
    expect(below.strip).toBe('none');
    expect(below.objectWidth).toBe(743);
  });

  it('never lets a list column pass 320 px', () => {
    const wide = windowFor({ container: containerForContent(1640), depth: DEEPEST_CHAIN });

    expect(wide.listWidth).toBe(LIST_MAX);
    expect(wide.objectWidth).toBe(544);
  });
});

describe('a shorter chain gives the strip to its columns', () => {
  it('shows all four levels of a plan of a project at 1440 px, rail expanded, without a strip', () => {
    const layout = windowFor({
      container: containerOf(1440, RAIL.expanded),
      depth: 4,
    });

    expect(layout.columns).toBe(4);
    expect(layout.strip).toBe('none');
    expect(layout.hiddenLeft).toBe(0);
  });

  it('shows one column, the object at the content width, when even a list column does not fit', () => {
    const layout = windowFor({ container: containerForContent(600), depth: 2 });

    expect(layout.columns).toBe(1);
    expect(layout.listWidth).toBe(0);
    expect(layout.objectWidth).toBe(600);
    expect(layout.strip).toBe('none');
  });

  it('shows no strip when no level is hidden', () => {
    const layout = windowFor({ container: containerForContent(700), depth: 2 });

    expect(layout.columns).toBe(2);
    expect(layout.strip).toBe('none');
    expect(layout.hiddenLeft).toBe(0);
    expect(layout.hiddenRight).toBe(0);
  });
});

describe('the strip', () => {
  it('names the levels hidden to the left of the window', () => {
    const layout = windowFor({ container: containerOf(1440, RAIL.expanded), depth: DEEPEST_CHAIN });

    expect(layout.columns).toBe(3);
    expect(layout.hiddenLeft).toBe(2);
    expect(layout.start).toBe(2);
    expect(layout.stripWidth).toBe(STRIP_NAMES);
  });

  it('stands for the hidden levels with marks only where the names would leave one column', () => {
    const layout = windowFor({ container: containerForContent(744), depth: DEEPEST_CHAIN });

    expect(layout.hiddenLeft).toBe(3);
    expect(layout.strip).toBe('marks');
    expect(layout.stripWidth).toBe(STRIP_MARKS);
  });
});

describe('a peek', () => {
  it('starts the window at the peeked level and hides the levels to its right', () => {
    const layout = windowFor({
      container: containerOf(1440, RAIL.expanded),
      depth: DEEPEST_CHAIN,
      peek: 0,
    });

    expect(layout.peek).toBe(true);
    expect(layout.start).toBe(0);
    expect(layout.strip).toBe('none');
    expect(layout.hiddenRight).toBe(2);
    expect(layout.returnStrip).toBe(true);
  });

  it('stretches the rightmost visible column into the room the strip frees', () => {
    const normal = windowFor({ container: containerOf(1440, RAIL.expanded), depth: DEEPEST_CHAIN });
    const peeked = windowFor({
      container: containerOf(1440, RAIL.expanded),
      depth: DEEPEST_CHAIN,
      peek: 0,
    });

    expect(peeked.objectWidth).toBeGreaterThan(normal.objectWidth);
    expect(peeked.columns).toBe(normal.columns);
  });

  it('ignores a peek index outside the chain', () => {
    const layout = windowFor({
      container: containerOf(1440, RAIL.expanded),
      depth: DEEPEST_CHAIN,
      peek: 12,
    });

    expect(layout.peek).toBe(false);
  });
});

describe('the widths add up to the content width', () => {
  it.each([360, 536, 704, 792, 883, 884, 960, 1048, 1160, 1208, 1216, 1376, 1688, 1856])(
    'fills a container of %i px without a horizontal scroll',
    (container) => {
      for (const sideOpen of [false, true]) {
        const layout = windowFor({ container, depth: DEEPEST_CHAIN, sideOpen });
        const listTotal = layout.sideTakesSpace ? 0 : (layout.columns - 1) * layout.listWidth;
        const objectWidth = layout.sideTakesSpace ? 0 : layout.objectWidth;

        expect(
          listTotal + objectWidth + layout.stripWidth + layout.sideWidth,
          `sum in ${container} px, side ${sideOpen ? 'open' : 'closed'}`,
        ).toBe(layout.width);
      }
    },
  );
});
