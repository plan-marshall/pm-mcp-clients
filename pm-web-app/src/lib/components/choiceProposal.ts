/**
 * The one refresh rule the three choice screens share (PLAN-169 D4).
 *
 * A refreshed state replaces the proposal the screen was drawn from. Resetting the whole selection
 * on every change would wipe what the operator typed, ticked or unticked, so the three components
 * resync only the part of the selection the changed rows belong to: they compare the proposal's
 * identity with {@link proposalDelta}, keep the operator's edits for the rows that stay, drop the
 * edits a removed row makes meaningless, and say so in one notice.
 */

/** What a refreshed proposal did, in the identity each component compares. */
export interface ProposalDelta {
  /** The identities the new proposal still carries. */
  kept: readonly string[];
  /** The identities the new proposal names that the previous one did not. */
  added: readonly string[];
  /** The identities the previous proposal named that the new one does not. */
  removed: readonly string[];
}

/**
 * Compares the identity of a previous proposal with the identity of the refreshed one.
 *
 * @param previous the identities the screen was drawn from
 * @param next the identities the refreshed state reports
 * @returns the kept, added and removed identities, each in the order of its source
 */
export function proposalDelta(previous: readonly string[], next: readonly string[]): ProposalDelta {
  const before = new Set(previous);
  const after = new Set(next);
  return {
    kept: next.filter((id) => before.has(id)),
    added: next.filter((id) => !before.has(id)),
    removed: previous.filter((id) => !after.has(id)),
  };
}
