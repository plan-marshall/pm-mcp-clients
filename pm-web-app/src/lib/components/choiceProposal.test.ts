import { describe, expect, it } from 'vitest';
import { proposalDelta } from './choiceProposal';

describe('proposalDelta', () => {
  it('names what a refreshed proposal kept, added and removed', () => {
    expect(proposalDelta(['a', 'b'], ['b', 'c'])).toEqual({
      kept: ['b'],
      added: ['c'],
      removed: ['a'],
    });
  });

  it('reports no change for the same proposal in a new array', () => {
    expect(proposalDelta(['a', 'b'], ['a', 'b'])).toEqual({
      kept: ['a', 'b'],
      added: [],
      removed: [],
    });
  });

  it('treats every row of a first proposal as added and a cleared proposal as removed', () => {
    expect(proposalDelta([], ['a'])).toEqual({ kept: [], added: ['a'], removed: [] });
    expect(proposalDelta(['a'], [])).toEqual({ kept: [], added: [], removed: ['a'] });
  });
});
