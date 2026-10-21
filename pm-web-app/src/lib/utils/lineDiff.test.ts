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
});
