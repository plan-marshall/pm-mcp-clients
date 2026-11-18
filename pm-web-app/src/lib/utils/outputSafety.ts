/**
 * Output safety utility for stripping ANSI escape sequences and the control or format characters a
 * wire answer can carry without showing them: the C0 and C1 controls, the Unicode line and paragraph
 * separators, the bidirectional controls that reorder what a reader sees, and a byte-order mark.
 *
 * A path or a diff line comes from the project, so all of these are removed before the text reaches
 * a text sink (`plan-view.adoc`, The Changes: "shown as plain text after removing every control
 * sequence"). The joiners U+200C and U+200D are kept: some scripts need them to shape a word, and
 * they reorder nothing.
 */

/* eslint-disable no-control-regex */
export function stripControlSequences(input?: string | null): string {
  if (!input) return '';
  return input
    .replace(/\x1B(?:[@-Z\\-_]|\[[0-?]*[ -/]*[@-~])/g, '')
    .replace(
      // C0 and DEL, C1, U+2028 and U+2029, the bidi marks, embeddings, overrides and isolates
      // (U+061C, U+200E, U+200F, U+202A..U+202E, U+2066..U+2069), and a byte-order mark.
      /[\x00-\x08\x0B\x0C\x0E-\x1F\x7F-\x9F\u061C\u2028\u2029\u200E\u200F\u202A-\u202E\u2066-\u2069\uFEFF]/g,
      '',
    );
}
