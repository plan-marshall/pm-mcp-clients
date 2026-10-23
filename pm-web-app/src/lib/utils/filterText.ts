/**
 * The matching rules every filterable list of the web app shares.
 *
 * The filter is client-only: it runs over the items a view already holds, never over a request.
 * Matching is a substring test on normalized text; a query with several tokens demands every
 * token, while a token may be found in any one of the item's values.
 */

/** The separator between two values in a haystack; normalized text never contains it. */
const FIELD_SEPARATOR = '\n';

/**
 * Normalizes text for comparison: decomposed Unicode, no combining marks, lower case, and one
 * space between words.
 *
 * The decomposition lets `Ärger` match `arger`, which a plain lower case would not do.
 *
 * @param value the text to normalize, possibly absent
 * @returns the normalized text, empty for `null` and `undefined`
 */
export function normalizeText(value: string | null | undefined): string {
  if (value === null || value === undefined) return '';
  return value
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim();
}

/**
 * @param query the text the operator typed, possibly absent
 * @returns its normalized tokens, empty for a blank query
 */
export function tokenize(query: string | null | undefined): string[] {
  const normalized = normalizeText(query);
  return normalized.length === 0 ? [] : normalized.split(' ');
}

/**
 * Joins an item's visible values into one normalized string, so filtering does not normalize
 * them again on every keystroke.
 *
 * The values are separated by a newline, which no normalized value contains: a token cannot be
 * assembled from the end of one field and the start of the next.
 *
 * @param values the fields the view renders for one item
 * @returns the normalized haystack
 */
export function buildHaystack(values: readonly (string | null | undefined)[]): string {
  const parts: string[] = [];
  for (const value of values) {
    const normalized = normalizeText(value);
    if (normalized.length > 0) parts.push(normalized);
  }
  return parts.join(FIELD_SEPARATOR);
}

/**
 * @param tokens the normalized query tokens
 * @param haystack a normalized haystack built by {@link buildHaystack}
 * @returns whether every token appears in the haystack
 */
export function matchesHaystack(tokens: readonly string[], haystack: string): boolean {
  if (tokens.length === 0) return true;
  return tokens.every((token) => haystack.includes(token));
}

/**
 * Matches a query against one item's visible values.
 *
 * Every token must appear in at least one value, so a query may span several fields while a
 * single token may not span two.
 *
 * @param query the text the operator typed
 * @param values the fields the view renders for one item
 * @returns whether the item matches; a blank query matches everything
 */
export function matchesQuery(
  query: string | null | undefined,
  values: readonly (string | null | undefined)[],
): boolean {
  const tokens = tokenize(query);
  if (tokens.length === 0) return true;
  return matchesHaystack(tokens, buildHaystack(values));
}
