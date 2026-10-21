/**
 * A line-by-line difference of two texts, as the designer's confirmation draws it.
 *
 * The designer shows the draft against the unit it replaces. A plain line diff is enough: the
 * unit is text, and the operator compares it line by line. The rows are aligned so that a
 * replaced line stands on both sides of the side-by-side view.
 */

/** One row of the comparison: the line on the left and the line on the right. */
export interface DiffRow {
  kind: 'same' | 'add' | 'remove' | 'change';
  left?: string;
  right?: string;
  leftNo?: number;
  rightNo?: number;
}

/** The rows of a comparison and how many lines it adds and removes. */
export interface LineDiff {
  rows: DiffRow[];
  added: number;
  removed: number;
}

/**
 * A single edit operation of the longest common subsequence.
 */
type Operation =
  | { kind: 'same'; left: number; right: number }
  | { kind: 'remove'; left: number }
  | { kind: 'add'; right: number };

/**
 * The longest common subsequence of two line arrays, as edit operations.
 *
 * A difference larger than the safety bound falls back to comparing the texts position by
 * position: the designer's units are small, and the bound keeps a pasted file from allocating an
 * unbounded table.
 *
 * @param left the lines before
 * @param right the lines after
 * @returns the operations in order
 */
function operations(left: string[], right: string[]): Operation[] {
  const rows = left.length;
  const columns = right.length;

  const table = new Uint32Array((rows + 1) * (columns + 1));
  const at = (row: number, column: number): number => row * (columns + 1) + column;
  for (let row = rows - 1; row >= 0; row--) {
    for (let column = columns - 1; column >= 0; column--) {
      table[at(row, column)] =
        left[row] === right[column]
          ? table[at(row + 1, column + 1)] + 1
          : Math.max(table[at(row + 1, column)], table[at(row, column + 1)]);
    }
  }

  const result: Operation[] = [];
  let row = 0;
  let column = 0;
  while (row < rows && column < columns) {
    if (left[row] === right[column]) {
      result.push({ kind: 'same', left: row, right: column });
      row++;
      column++;
    } else if (table[at(row + 1, column)] >= table[at(row, column + 1)]) {
      result.push({ kind: 'remove', left: row });
      row++;
    } else {
      result.push({ kind: 'add', right: column });
      column++;
    }
  }
  while (row < rows) {
    result.push({ kind: 'remove', left: row });
    row++;
  }
  while (column < columns) {
    result.push({ kind: 'add', right: column });
    column++;
  }
  return result;
}

/**
 * Compares oversized texts position by position rather than allocate a square table.
 *
 * @param left the lines before
 * @param right the lines after
 * @returns the rows and counts
 */
function positionalDiff(left: string[], right: string[]): LineDiff {
  // A pasted file may be far larger than a unit; compare it position by position rather than
  // allocate a table that grows with the square of its lines.
  const rows: DiffRow[] = [];
  let added = 0;
  let removed = 0;
  const bound = Math.max(left.length, right.length);
  for (let index = 0; index < bound; index++) {
    if (index >= left.length) {
      rows.push({ kind: 'add', right: right[index], rightNo: index + 1 });
      added++;
    } else if (index >= right.length) {
      rows.push({ kind: 'remove', left: left[index], leftNo: index + 1 });
      removed++;
    } else if (left[index] === right[index]) {
      rows.push({
        kind: 'same',
        left: left[index],
        right: right[index],
        leftNo: index + 1,
        rightNo: index + 1,
      });
    } else {
      rows.push({
        kind: 'change',
        left: left[index],
        right: right[index],
        leftNo: index + 1,
        rightNo: index + 1,
      });
      added++;
      removed++;
    }
  }
  return { rows, added, removed };
}

/**
 * Compares two texts line by line.
 *
 * Removed and added lines that stand next to each other become one `change` row, so the
 * side-by-side view keeps the replaced line opposite its replacement.
 *
 * @param before the text as it is
 * @param after the text as the draft has it
 * @returns the aligned rows and the added and removed line counts
 */
export function diffLines(before: string, after: string): LineDiff {
  const left = before === '' ? [] : before.split('\n');
  const right = after === '' ? [] : after.split('\n');
  if (left.length * right.length > 4_000_000) {
    return positionalDiff(left, right);
  }
  const result: DiffRow[] = [];
  let added = 0;
  let removed = 0;
  const pendingRemovals: Operation[] = [];
  const pendingAdditions: Operation[] = [];

  const flush = (): void => {
    const pairs = Math.min(pendingRemovals.length, pendingAdditions.length);
    for (let index = 0; index < pairs; index++) {
      const removal = pendingRemovals[index] as { kind: 'remove'; left: number };
      const addition = pendingAdditions[index] as { kind: 'add'; right: number };
      result.push({
        kind: 'change',
        left: left[removal.left],
        right: right[addition.right],
        leftNo: removal.left + 1,
        rightNo: addition.right + 1,
      });
    }
    for (let index = pairs; index < pendingRemovals.length; index++) {
      const removal = pendingRemovals[index] as { kind: 'remove'; left: number };
      result.push({
        kind: 'remove',
        left: left[removal.left],
        leftNo: removal.left + 1,
      });
    }
    for (let index = pairs; index < pendingAdditions.length; index++) {
      const addition = pendingAdditions[index] as { kind: 'add'; right: number };
      result.push({
        kind: 'add',
        right: right[addition.right],
        rightNo: addition.right + 1,
      });
    }
    pendingRemovals.length = 0;
    pendingAdditions.length = 0;
  };

  for (const operation of operations(left, right)) {
    if (operation.kind === 'same') {
      flush();
      result.push({
        kind: 'same',
        left: left[operation.left],
        right: right[operation.right],
        leftNo: operation.left + 1,
        rightNo: operation.right + 1,
      });
    } else if (operation.kind === 'remove') {
      pendingRemovals.push(operation);
      removed++;
    } else {
      pendingAdditions.push(operation);
      added++;
    }
  }
  flush();
  return { rows: result, added, removed };
}
