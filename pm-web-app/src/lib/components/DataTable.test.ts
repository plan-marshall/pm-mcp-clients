import { describe, it, expect, vi } from 'vitest';
import { render, screen, within } from '@testing-library/svelte';
import DataTable from './DataTable.svelte';
import DataTableEmptyFixture from '../../test/fixtures/DataTableEmptyFixture.svelte';

/**
 * `DataTable` (VD-P-05, VD-S-04): the table of the design system. Every column carries the fixed
 * width of its column spec, a number column is right-aligned, the head stays in place while the
 * body scrolls, a row is 40 px high (32 px compact), the last column holds the row menu, and a
 * table without rows draws the empty slot its view fills.
 */

const LABEL = 'Running plans';

const COLUMNS = [
  { id: 'plan', header: 'Plan', width: '16rem' },
  { id: 'state', header: 'State', width: '8rem' },
  { id: 'tokens', header: 'Tokens', width: '6rem', numeric: true },
];

const ROWS = [
  { id: 'p-31', cells: { plan: 'web-core-components', state: 'done', tokens: '412k' } },
  { id: 'p-35', cells: { plan: 'web-data-components', state: 'running', tokens: '38k' } },
];

describe('DataTable', () => {
  it('draws one column of the fixed width its spec names', () => {
    const { container } = render(DataTable, {
      props: { columns: COLUMNS, rows: ROWS, label: LABEL },
    });
    const columns = Array.from(container.querySelectorAll('col'));

    expect(columns).toHaveLength(COLUMNS.length);
    expect(columns.map((column) => column.getAttribute('data-column'))).toEqual([
      'plan',
      'state',
      'tokens',
    ]);
    expect(columns.map((column) => (column as HTMLElement).style.width)).toEqual([
      '16rem',
      '8rem',
      '6rem',
    ]);
    expect(screen.getByTestId('data-table')).toHaveAttribute('data-layout', 'fixed');
  });

  it('gives every column its heading, in the order of the spec', () => {
    render(DataTable, { props: { columns: COLUMNS, rows: ROWS, label: LABEL } });
    const headings = screen.getAllByRole('columnheader');

    expect(headings.map((heading) => heading.textContent?.trim())).toEqual([
      'Plan',
      'State',
      'Tokens',
    ]);
  });

  it('draws the head as the table’s own head, before the body it scrolls above', () => {
    const { container } = render(DataTable, {
      props: { columns: COLUMNS, rows: ROWS, label: LABEL },
    });
    const head = container.querySelector('thead');

    expect(head).not.toBeNull();
    expect(container.querySelector('.data-table__scroll')).toContainElement(
      container.querySelector('table'),
    );
    expect(head?.querySelectorAll('th[scope="col"]')).toHaveLength(COLUMNS.length);
  });

  it('right-aligns the numbers of a numeric column and leaves the text columns alone', () => {
    render(DataTable, { props: { columns: COLUMNS, rows: ROWS, label: LABEL } });
    const rows = screen.getAllByRole('row').slice(1);

    expect(rows.map((row) => within(row).getAllByRole('cell')[2].getAttribute('data-align'))).toEqual(
      ['end', 'end'],
    );
    expect(rows.map((row) => within(row).getAllByRole('cell')[0].getAttribute('data-align'))).toEqual(
      ['start', 'start'],
    );
    expect(screen.getAllByRole('columnheader')[2]).toHaveAttribute('data-align', 'end');
    expect(screen.getAllByRole('columnheader')[0]).toHaveAttribute('data-align', 'start');
  });

  it('names the default density, whose single-line rows are 40 px in the browser', () => {
    render(DataTable, {
      props: { columns: COLUMNS, rows: ROWS, label: LABEL },
    });

    expect(screen.getByTestId('data-table')).toHaveAttribute('data-density', 'default');
  });

  it('names the compact density, whose single-line rows are 32 px in the browser', () => {
    render(DataTable, {
      props: { columns: COLUMNS, rows: ROWS, label: LABEL, compact: true },
    });

    expect(screen.getByTestId('data-table')).toHaveAttribute('data-density', 'compact');
  });

  it('draws the row menu in the last column, names each menu with its row, and reports the row', async () => {
    const onMenu = vi.fn();
    render(DataTable, {
      props: { columns: COLUMNS, rows: ROWS, label: LABEL, menuLabel: 'Row menu', onMenu },
    });
    const menus = screen.getAllByRole('button', { name: /^Row menu/ });

    expect(menus).toHaveLength(ROWS.length);
    expect(menus.map((menu) => menu.getAttribute('aria-label'))).toEqual([
      'Row menu: web-core-components',
      'Row menu: web-data-components',
    ]);
    expect(screen.getAllByRole('columnheader')).toHaveLength(COLUMNS.length + 1);

    menus[1].click();

    expect(onMenu).toHaveBeenCalledWith('p-35');
  });

  it('draws no menu column when the view names no row menu', () => {
    render(DataTable, { props: { columns: COLUMNS, rows: ROWS, label: LABEL } });

    expect(screen.queryAllByRole('button')).toHaveLength(0);
    expect(screen.getAllByRole('columnheader')).toHaveLength(COLUMNS.length);
  });

  it('draws the empty slot a view fills when the table has no rows', () => {
    const EMPTY = 'No plan is running';
    const { container } = render(DataTableEmptyFixture, {
      props: { columns: COLUMNS, label: LABEL, emptyLabel: EMPTY },
    });

    expect(screen.getByText(EMPTY)).toBeInTheDocument();
    expect(container.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(screen.getByText(EMPTY).closest('td')).toHaveAttribute(
      'colspan',
      String(COLUMNS.length),
    );
  });

  it('spans the empty slot across the menu column too, when the view draws one', () => {
    const EMPTY = 'No plan is running';
    const { container } = render(DataTableEmptyFixture, {
      props: { columns: COLUMNS, label: LABEL, emptyLabel: EMPTY, menuLabel: 'Row menu' },
    });

    expect(screen.getByText(EMPTY).closest('td')).toHaveAttribute(
      'colspan',
      String(COLUMNS.length + 1),
    );
    expect(container.querySelectorAll('thead th')).toHaveLength(COLUMNS.length + 1);
  });

  it('names the table, so a page with two of them tells them apart', () => {
    render(DataTable, { props: { columns: COLUMNS, rows: ROWS, label: LABEL } });

    expect(screen.getByRole('table', { name: LABEL })).toBeInTheDocument();
  });
});
