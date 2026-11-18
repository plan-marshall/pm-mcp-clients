import { describe, it, expect } from 'vitest';
import { uniqueId } from './uniqueId';

describe('uniqueId', () => {
  it('names the kind of element and never hands out an id twice, whatever the kind', () => {
    // Act: two kinds, one of which looks like the other with a number behind it
    const ids = [
      uniqueId('hint'),
      uniqueId('hint'),
      uniqueId('hint-1'),
      uniqueId('error'),
      uniqueId('hint'),
    ];

    // Assert
    expect(new Set(ids).size).toBe(ids.length);
    expect(ids[0]).toMatch(/^hint-\d+$/);
    expect(ids[2]).toMatch(/^hint-1-\d+$/);
    expect(ids[3]).toMatch(/^error-\d+$/);
  });
});
