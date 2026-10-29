import { describe, it, expect } from 'vitest';
import { shortenMiddle } from './shortenMiddle';

/**
 * `shortenMiddle`: the whole value while it fits, and otherwise the head and the tail around one
 * ellipsis, so the two ends of an id stay readable where an end-ellipsis would hide the tail.
 */

describe('shortenMiddle', () => {
  it('keeps a value that fits', () => {
    expect(shortenMiddle('short', 10)).toBe('short');
  });

  it('keeps the head and the tail around one ellipsis', () => {
    expect(shortenMiddle('plan-marshall/relay@36e3fc5c', 15)).toBe('plan-ma…6e3fc5c');
  });

  it('never returns more characters than the limit', () => {
    expect(shortenMiddle('plan-marshall/relay@36e3fc5c', 15).length).toBe(15);
  });

  it('returns the ellipsis alone when there is no room for either end', () => {
    expect(shortenMiddle('plan-marshall', 1)).toBe('…');
  });

  it('returns nothing when the limit leaves no room at all', () => {
    expect(shortenMiddle('plan-marshall', 0)).toBe('');
  });
});
