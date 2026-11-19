import { describe, it, expect } from 'vitest';
import { diffLines } from './lineDiff';

describe('lineDiff', () => {
  it('reports no difference for equal texts', () => {
    const diff = diffLines('a\nb\nc', 'a\nb\nc');
    expect(diff.added).toBe(0);
    expect(diff.removed).toBe(0);
    expect(diff.rows.every((row) => row.kind === 'same')).toBe(true);
    expect(diff.rows.map((row) => row.leftNo)).toEqual([1, 2, 3]);
  });

  it('pairs a replaced line on both sides', () => {
    const diff = diffLines('a\nb\nc', 'a\nB\nc');
    expect(diff.added).toBe(1);
    expect(diff.removed).toBe(1);
    const changed = diff.rows.find((row) => row.kind === 'change');
    expect(changed?.left).toBe('b');
    expect(changed?.right).toBe('B');
    expect(changed?.leftNo).toBe(2);
    expect(changed?.rightNo).toBe(2);
  });

  it('counts added and removed lines around an unchanged anchor', () => {
    const diff = diffLines('a\nb', 'a\nx\ny\nb');
    expect(diff.added).toBe(2);
    expect(diff.removed).toBe(0);
    expect(diff.rows.filter((row) => row.kind === 'add').map((row) => row.right)).toEqual([
      'x',
      'y',
    ]);
  });

  it('treats an empty text as no lines', () => {
    const diff = diffLines('', 'a\nb');
    expect(diff.removed).toBe(0);
    expect(diff.added).toBe(2);
    expect(diff.rows.every((row) => row.kind === 'add')).toBe(true);
  });

  it('keeps an empty line as a line of its own', () => {
    const diff = diffLines('a\n\nb', 'a\nb');
    expect(diff.removed).toBe(1);
    expect(diff.rows.some((row) => row.kind === 'remove' && row.left === '')).toBe(true);
  });

  it('removes the lines a text no longer has after the last common one', () => {
    const diff = diffLines('a\nb\nc\nd', 'a\nb');
    expect(diff.added).toBe(0);
    expect(diff.removed).toBe(2);
    expect(diff.rows.filter((row) => row.kind === 'remove').map((row) => row.left)).toEqual([
      'c',
      'd',
    ]);
    expect(diff.rows.filter((row) => row.kind === 'remove').map((row) => row.leftNo)).toEqual([
      3, 4,
    ]);
  });

  describe('texts past the safety bound', () => {
    /** @returns `count` lines that name their own index */
    function lines(count: number): string[] {
      return Array.from({ length: count }, (_, index) => `line ${index}`);
    }

    it('compares two huge texts position by position, which an ordinary diff would not', () => {
      const before = lines(2001);
      // A line inserted in the middle: compared position by position, every later line changes,
      // while the ordinary longest-common-subsequence path would report a single added line.
      const after = [...before.slice(0, 1000), 'inserted', ...before.slice(1000)];

      const positional = diffLines(before.join('\n'), after.join('\n'));
      const small = lines(10);
      const smallWithInsert = [...small.slice(0, 5), 'inserted', ...small.slice(5)];
      const ordinary = diffLines(small.join('\n'), smallWithInsert.join('\n'));

      expect(ordinary.added).toBe(1);
      expect(ordinary.removed).toBe(0);
      expect(positional.rows[1000]).toMatchObject({
        kind: 'change',
        left: 'line 1000',
        right: 'inserted',
        leftNo: 1001,
        rightNo: 1001,
      });
      expect(positional.rows.at(-1)).toMatchObject({ kind: 'add', right: 'line 2000', rightNo: 2002 });
      expect(positional.added).toBe(1002);
      expect(positional.removed).toBe(1001);
    });

    it('names the lines a huge text adds past the end of the other', () => {
      const before = lines(2001);
      const after = lines(2002);

      const diff = diffLines(before.join('\n'), after.join('\n'));

      expect(diff.added).toBe(1);
      expect(diff.removed).toBe(0);
      expect(diff.rows.at(-1)).toMatchObject({ kind: 'add', right: 'line 2001', rightNo: 2002 });
    });

    it('names the lines a huge text drops past the end of the other', () => {
      const before = lines(2002);
      const after = lines(2001);

      const diff = diffLines(before.join('\n'), after.join('\n'));

      expect(diff.added).toBe(0);
      expect(diff.removed).toBe(1);
      expect(diff.rows.at(-1)).toMatchObject({ kind: 'remove', left: 'line 2001', leftNo: 2002 });
    });
  });
});
