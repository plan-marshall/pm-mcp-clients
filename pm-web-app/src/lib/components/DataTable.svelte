<script module lang="ts">
  /** One column of the table's spec: its id, its heading, its fixed width, its alignment. */
  export interface DataTableColumn {
    /** The column's id; a row carries its cell text under this key. */
    id: string;
    /** The heading of the column, from the view's catalogue key. */
    header: string;
    /** The fixed width of the column, as a CSS length (`16rem`, `20%`). */
    width: string;
    /** A number column: its cells are right-aligned and set in tabular figures. */
    numeric?: boolean;
  }

  /** One row of the table: what identifies it and the text of each of its cells. */
  export interface DataTableRow {
    /** What identifies the row, reported by the row menu. */
    id: string;
    /** The row's text per column id. */
    cells: Record<string, string>;
  }
</script>

<script lang="ts">
  import IconButton from './IconButton.svelte';

  /**
   * The table of the design system (VD-P-05, VD-S-04): every column takes the fixed width its spec
   * names, a number column is right-aligned in tabular figures, the head stays in place while the
   * body scrolls with the page, a row is 40 px high (32 px in the compact density), the row menu
   * stands in the last column, and a table without rows draws the empty slot its view fills.
   *
   * Every cell is a text sink: the view hands in text it already formatted, so no value of the
   * runtime or of a model can reach the DOM as markup.
   */
  export let columns: DataTableColumn[] = [];
  export let rows: DataTableRow[] = [];
  /** The accessible name of the table, from the view's catalogue key. */
  export let label: string;
  /** The compact density, for a row of 32 px instead of 40 px. */
  export let compact = false;
  /**
   * The accessible name of the row menu. A table whose view names no menu draws none, so a view
   * that has no act per row keeps the last column for its data.
   */
  export let menuLabel: string | undefined = undefined;
  /** Reports the row whose menu was opened. */
  export let onMenu: (rowId: string) => void = () => {};

  /** The columns the body spans: the spec's own plus the menu's, where the view names one. */
  $: columnCount = columns.length + (menuLabel ? 1 : 0);
</script>

<div class="data-table__scroll">
  <table
    class="data-table"
    class:data-table--compact={compact}
    data-layout="fixed"
    data-density={compact ? 'compact' : 'default'}
    data-testid="data-table"
    aria-label={label}
  >
    <colgroup>
      {#each columns as column (column.id)}
        <col data-column={column.id} style={`width: ${column.width}`} />
      {/each}
      {#if menuLabel}
        <col class="data-table__menu-col" />
      {/if}
    </colgroup>
    <thead>
      <tr>
        {#each columns as column (column.id)}
          <th
            scope="col"
            data-align={column.numeric ? 'end' : 'start'}
            class:data-table__numeric={column.numeric}
          >
            {column.header}
          </th>
        {/each}
        {#if menuLabel}
          <th scope="col" class="data-table__menu-head">{menuLabel}</th>
        {/if}
      </tr>
    </thead>
    <tbody>
      {#if rows.length === 0}
        <tr class="data-table__empty-row">
          <td class="data-table__empty" colspan={columnCount}>
            <slot name="empty" />
          </td>
        </tr>
      {:else}
        {#each rows as row (row.id)}
          <tr class="data-table__row" data-row={row.id}>
            {#each columns as column (column.id)}
              <td
                data-align={column.numeric ? 'end' : 'start'}
                class:data-table__numeric={column.numeric}
              >
                {row.cells[column.id] ?? ''}
              </td>
            {/each}
            {#if menuLabel}
              <td class="data-table__menu">
                <IconButton
                  icon="act.more"
                  size="sm"
                  label={menuLabel}
                  on:click={() => onMenu(row.id)}
                />
              </td>
            {/if}
          </tr>
        {/each}
      {/if}
    </tbody>
  </table>
</div>

<style>
  /* A wide table scrolls inside its own wrapper; the page itself never scrolls sideways. */
  .data-table__scroll {
    max-inline-size: 100%;
    overflow-x: auto;
  }

  .data-table {
    inline-size: 100%;
    border-collapse: collapse;
    table-layout: fixed;
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }

  .data-table thead th {
    position: sticky;
    inset-block-start: 0;
    z-index: 1;
    padding: var(--space-2) var(--space-3);
    border-block-end: 1px solid var(--color-border-strong);
    background-color: var(--color-bg-subtle);
    color: var(--color-text-secondary);
    font-weight: var(--weight-semibold);
    text-align: start;
    white-space: nowrap;
  }

  .data-table tbody td {
    block-size: 40px;
    padding: var(--space-2) var(--space-3);
    border-block-end: 1px solid var(--color-border);
    overflow-wrap: anywhere;
    text-align: start;
  }

  /* The compact density of VD-P-05: a row of 32 px instead of 40 px. */
  .data-table--compact tbody td {
    block-size: 32px;
  }

  /* A number is right-aligned and set in tabular figures, so a column of them lines up. */
  .data-table [data-align='end'] {
    text-align: end;
  }

  .data-table__numeric {
    font-variant-numeric: tabular-nums;
  }

  .data-table tbody tr:hover {
    background-color: var(--color-bg-subtle);
  }

  .data-table__menu-col,
  .data-table__menu-head {
    inline-size: 44px;
  }

  .data-table__menu {
    text-align: end;
  }

  /* An empty table says so in one cell that spans every column, and keeps no row height. */
  .data-table__empty-row td {
    block-size: auto;
    padding: var(--space-6);
    border-block-end: 0;
    text-align: center;
  }
</style>
