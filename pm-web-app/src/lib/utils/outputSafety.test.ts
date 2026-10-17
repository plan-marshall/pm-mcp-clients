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
});
