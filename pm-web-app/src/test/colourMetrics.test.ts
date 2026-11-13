import { describe, it, expect } from 'vitest';
import {
  ciede2000,
  contrastRatio,
  deltaE00,
  hueDistance,
  parseHex,
  toLab,
  toOklch,
} from './colourMetrics';

/**
 * Reference values for the colour maths of `colourMetrics.ts`, so the measured rules of
 * `tokens.test.ts` stand on checked maths.
 */
describe('colourMetrics', () => {
  describe('CIEDE2000', () => {
    /** Pairs of the published test data of Sharma, Wu and Dalal (2005), with their ΔE00. */
    const SHARMA_PAIRS: Array<{ first: [number, number, number]; second: [number, number, number]; expected: number }> = [
      { first: [50, 2.6772, -79.7751], second: [50, 0, -82.7485], expected: 2.0425 },
      { first: [50, 0, 0], second: [50, -1, 2], expected: 2.3669 },
      { first: [50, 2.49, -0.001], second: [50, -2.49, 0.0011], expected: 7.2195 },
      { first: [50, 2.5, 0], second: [73, 25, -18], expected: 27.1492 },
      { first: [60.2574, -34.0099, 36.2677], second: [60.4626, -34.1751, 39.4387], expected: 1.2644 },
      { first: [2.0776, 0.0795, -1.135], second: [0.9033, -0.0636, -0.5514], expected: 0.9082 },
    ];

    it.each(SHARMA_PAIRS)('matches the reference ΔE00 $expected', ({ first, second, expected }) => {
      const lab = ([l, a, b]: [number, number, number]) => ({ l, a, b });

      expect(ciede2000(lab(first), lab(second))).toBeCloseTo(expected, 4);
    });

    it('is symmetric and zero for the same colour', () => {
      expect(deltaE00('#BB2D8C', '#007785')).toBeCloseTo(deltaE00('#007785', '#BB2D8C'), 10);
      expect(deltaE00('#BB2D8C', '#BB2D8C')).toBe(0);
    });
  });

  describe('CIELAB under D65', () => {
    it('maps white to L 100 with no chroma and black to L 0', () => {
      const white = toLab('#FFFFFF');
      const black = toLab('#000000');

      expect(white.l).toBeCloseTo(100, 2);
      expect(white.a).toBeCloseTo(0, 2);
      expect(white.b).toBeCloseTo(0, 2);
      expect(black.l).toBeCloseTo(0, 6);
    });

    it('maps sRGB red to its published CIELAB value', () => {
      const red = toLab('#FF0000');

      expect(red.l).toBeCloseTo(53.24, 1);
      expect(red.a).toBeCloseTo(80.09, 1);
      expect(red.b).toBeCloseTo(67.2, 1);
    });
  });

  describe('OKLCH', () => {
    it('maps sRGB red to oklch(0.628 0.258 29.23)', () => {
      const red = toOklch('#FF0000');

      expect(red.l).toBeCloseTo(0.628, 3);
      expect(red.c).toBeCloseTo(0.2577, 3);
      expect(red.h).toBeCloseTo(29.23, 1);
    });

    it('gives white no chroma', () => {
      const white = toOklch('#FFFFFF');

      expect(white.l).toBeCloseTo(1, 4);
      expect(white.c).toBeLessThan(0.0001);
    });

    it('measures the shorter angle between two hues', () => {
      expect(hueDistance(350, 10)).toBe(20);
      expect(hueDistance(10, 350)).toBe(20);
      expect(hueDistance(90, 270)).toBe(180);
      expect(hueDistance(0, 0)).toBe(0);
    });
  });

  describe('WCAG 2.1 contrast', () => {
    it('gives black on white 21:1 and a colour on itself 1:1', () => {
      expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 6);
      expect(contrastRatio('#FFFFFF', '#000000')).toBeCloseTo(21, 6);
      expect(contrastRatio('#5B4BDB', '#5B4BDB')).toBe(1);
    });
  });

  it('refuses anything but a six-digit hex colour', () => {
    expect(() => parseHex('#FFF')).toThrow('Not a six-digit hex colour');
    expect(() => parseHex('var(--color-brand)')).toThrow('Not a six-digit hex colour');
  });
});
