import { get } from 'svelte/store';
import { hasMessage, t } from '../i18n';

/**
 * The display layer of CC-P-01: the one place a view turns a value the API enumerates into the
 * words the operator reads.
 *
 * The words come from the shared vocabulary of the catalogue (`state.<kind>.<value>`,
 * `unit.<id>`), so no view prints a wire value: a value the layer does not name reads as the
 * unknown word instead of the raw enum, and an error code reads as its title, cause and fix.
 */

/** The enumerated kinds the shared vocabulary carries a word for each value of. */
export type StateKind =
  | 'plan'
  | 'epic'
  | 'task'
  | 'worker'
  | 'model'
  | 'landing'
  | 'cleanup'
  | 'activity'
  | 'decision'
  | 'gate'
  | 'signal'
  | 'node_kind';

/**
 * The wire names an activity entry carries for a value the vocabulary names differently.
 *
 * The live-activity stream sends `assistant_text`, `tool` and `changed` beside the values the
 * vocabulary uses, so the layer resolves both spellings to one word.
 */
const ACTIVITY_ALIASES: Readonly<Record<string, string>> = Object.freeze({
  assistant_text: 'text',
  tool: 'tool_call',
  changed: 'file_changed',
});

/** The word every value the layer does not name reads as. */
const STATE_UNKNOWN_KEY = 'state.unknown';

/** The word every unit the layer does not name reads as. */
const UNIT_UNKNOWN_KEY = 'unit.unknown';

/**
 * @param key the key of a word
 * @param fallbackKey the key its absence reads as
 * @returns the word, or the fallback word when the catalogue does not carry the key
 */
function word(key: string, fallbackKey: string): string {
  return get(t)(hasMessage(key) ? key : fallbackKey);
}

/**
 * Names one value of an enumerated field.
 *
 * a value the catalogue does not name reads as the unknown word: a raw wire value never reaches
 * the operator, whether it is new to the client or misspelled by the runtime.
 *
 * @param kind the enumerated kind of the value
 * @param value the wire value the resource carries
 * @returns the word the operator reads
 */
export function displayState(kind: StateKind, value: string | null | undefined): string {
  const normalized = (value ?? '').trim().toLowerCase();
  if (!normalized) {
    return get(t)(STATE_UNKNOWN_KEY);
  }
  const aliased = kind === 'activity' ? (ACTIVITY_ALIASES[normalized] ?? normalized) : normalized;
  return word(`state.${kind}.${aliased}`, STATE_UNKNOWN_KEY);
}

/** The three texts of an error, as the operator reads them. */
export interface DisplayErrorText {
  /** What happened. */
  title: string;
  /** Why it happened. */
  cause: string;
  /** What the operator can do about it. */
  fix: string;
}

/**
 * Names the error a code stands for, through the shared error texts of `PLAN-86`.
 *
 * A code the client does not know reads as the shared unknown fallback (`error.unknown.*`), so a
 * raw code (`UNENROLLED_PROJECT`) never reaches the operator.
 *
 * @param code the error code a refusal carries
 * @returns its title, cause and fix
 */
export function displayError(code: string | null | undefined): DisplayErrorText {
  const normalized = (code ?? '').trim().toLowerCase();
  const known = normalized.length > 0 && hasMessage(`error.${normalized}.title`);
  const base = known ? `error.${normalized}` : 'error.unknown';
  return {
    title: get(t)(`${base}.title`),
    cause: get(t)(`${base}.cause`),
    fix: get(t)(`${base}.fix`),
  };
}

/**
 * Names a workflow unit by its id.
 *
 * The plain name comes from the catalogue of workflow units (`unit.<id>`), so
 * `landing.pull-request` reads as "Pull request". A view shows the id as a chip beside the name
 * where the operator needs it; the id never stands alone as the label.
 *
 * @param id the unit id the runtime carries
 * @returns the unit's plain name, or the unknown word when the catalogue does not name it
 */
export function displayUnit(id: string | null | undefined): string {
  const normalized = (id ?? '').trim();
  if (!normalized) {
    return get(t)(UNIT_UNKNOWN_KEY);
  }
  return word(`unit.${normalized}`, UNIT_UNKNOWN_KEY);
}
