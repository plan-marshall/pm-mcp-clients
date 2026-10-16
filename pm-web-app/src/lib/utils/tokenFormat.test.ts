import { describe, it, expect } from 'vitest';
import { formatTokens, formatSplit } from './tokenFormat';

describe('tokenFormat utility', () => {
  it('formats counts below 1000 as plain number', () => {
    expect(formatTokens(412)).toBe('412');
    expect(formatTokens(0)).toBe('0');
  });

  it('formats counts from 1000 to 9999 with one decimal', () => {
    expect(formatTokens(4200)).toBe('4.2 k');
    expect(formatTokens(4200, false, 'de')).toBe('4,2 Tsd.');
  });

  it('formats counts from 10000 to 999999 as whole thousands', () => {
    expect(formatTokens(830000)).toBe('830 k');
    expect(formatTokens(830000, false, 'de')).toBe('830 Tsd.');
  });

  it('formats counts from 1000000 with two decimals', () => {
    expect(formatTokens(1240000)).toBe('1.24 M');
    expect(formatTokens(1240000, false, 'de')).toBe('1,24 Mio.');
  });

  it('handles floor rounding down with prefix', () => {
    // 412 680 measured tokens are >= 412 k, not >= 413 k
    expect(formatTokens(412680, true)).toBe('≥ 412 k');
    expect(formatTokens(4280, true)).toBe('≥ 4.2 k');
    expect(formatTokens(1249000, true)).toBe('≥ 1.24 M');
  });

  it('formats split in and out tokens', () => {
    expect(formatSplit(412000, 380000, 32000, true, 'en')).toBe('≥ 412 k (≥ 380 k in · ≥ 32 k out)');
    expect(formatSplit(412000, 380000, 32000, false, 'de')).toBe('412 Tsd. (380 Tsd. rein · 32 Tsd. raus)');
  });
});
