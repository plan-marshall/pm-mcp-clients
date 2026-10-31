/**
 * The fields whose values the API enumerates, for the enum guard of CC-P-01.
 *
 * A field belongs here when the shared display vocabulary names every value it carries, so the
 * guard can demand that a view prints it through `displayState`. A field whose values have no
 * vocabulary entry yet cannot be listed without turning its value into the unknown word: its
 * words belong in the vocabulary first, and that is `PLAN-06`'s surface, not this list's.
 *
 * `lib/display/enumGuard.test.ts` reads this list and fails on any `.svelte` file that
 * interpolates one of these fields into a text node without `displayState`.
 */
export const ENUM_FIELDS: readonly string[] = Object.freeze(['state', 'phase_state', 'signal']);
