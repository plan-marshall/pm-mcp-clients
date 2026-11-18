import { describe, it, expect } from 'vitest';
import { stripControlSequences } from './outputSafety';

describe('outputSafety', () => {
  it('strips ANSI color and control codes', () => {
    const raw = '\u001B[31mError:\u001B[0m Failed with code \u0007BELL';
    expect(stripControlSequences(raw)).toBe('Error: Failed with code BELL');
  });

  it('handles null or empty string gracefully', () => {
    expect(stripControlSequences(null)).toBe('');
    expect(stripControlSequences('')).toBe('');
    expect(stripControlSequences(undefined)).toBe('');
  });

  it('preserves clean text, tabs, and newlines', () => {
    const clean = 'Line 1\n\tTabbed line 2';
    expect(stripControlSequences(clean)).toBe(clean);
  });

  it('strips C1, the line and paragraph separators, and the bidi controls a path can carry', () => {
    // The characters the wire can carry without showing them (U+0085, U+2028, the bidi
    // embeddings/overrides and isolates); the joiners stay, because some scripts shape with them.
    const raw =
      'src/\u0085File\u2028na\u2029me\u061C\u200E\u202B\u202E\u2066\u2069.ts\uFEFF and\u200C a\u200D word';
    expect(stripControlSequences(raw)).toBe('src/Filename.ts and\u200C a\u200D word');
  });
});
