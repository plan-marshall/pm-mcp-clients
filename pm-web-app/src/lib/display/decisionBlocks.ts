import type { Translate } from './index';

/**
 * The one mapping from a wire `blocks` value to the decision's own words (`needs-and-find.adoc`,
 * What It Blocks; `texts-and-languages.adoc`): `plan`, `epic`, `setup`, `entry` and `task` word
 * themselves, the wire value `nothing` is shown through `decision.blocks.none`, and a value outside
 * the vocabulary shows the client's unknown word — never a raw key and never a blocking claim.
 *
 * The list of what waits and the decisions lens both word a blocker, so the mapping lives here and
 * both views call it: a renamed key or a new value is changed once.
 */

/** The values whose key spells the value itself; `nothing` has a key of its own. */
const BLOCK_KEYS: Readonly<Record<string, string>> = Object.freeze({
  plan: 'decision.blocks.plan',
  epic: 'decision.blocks.epic',
  setup: 'decision.blocks.setup',
  entry: 'decision.blocks.entry',
  task: 'decision.blocks.task',
  nothing: 'decision.blocks.none',
});

/** The word for a value the client does not know. */
const UNKNOWN_KEY = 'common.unknown';

/**
 * @param blocks the wire value of `blocks`
 * @returns the catalogue key of the decision's word for it
 */
export function blockWordKeyOf(blocks: string): string {
  return BLOCK_KEYS[blocks.trim()] ?? UNKNOWN_KEY;
}

/**
 * @param blocks the wire value of `blocks`
 * @param translate the catalogue look-up (`$t`) a template passes, so the word follows a locale switch
 * @returns the word the view shows for it
 */
export function blockWordText(blocks: string, translate: Translate): string {
  return translate(blockWordKeyOf(blocks));
}
