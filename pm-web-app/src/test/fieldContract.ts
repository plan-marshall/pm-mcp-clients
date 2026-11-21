import fs from 'node:fs';

/**
 * A declaration that removes the outline (`outline: none` or `outline: 0`). Forced-colors mode
 * drops a `box-shadow`, so a field that removes its outline shows no focus there at all.
 */
export const REMOVED_OUTLINE = /(?<![\w-])outline\s*:\s*(?:none|0)(?=\s*(?:;|\}|$))/m;

/**
 * The focus outline of a field: invisible in normal rendering, drawn in the system colour in
 * forced-colors mode, where the transparent colour is replaced.
 */
export const TRANSPARENT_OUTLINE = /(?<![\w-])outline\s*:\s*2px\s+solid\s+transparent(?=\s*(?:;|\}|$))/m;

/**
 * Reads a component's whole source, script included.
 *
 * @param componentPath the component's file
 * @returns the file's text
 */
export function sourceOf(componentPath: string): string {
  return fs.readFileSync(componentPath, 'utf-8');
}

/**
 * The shape of an id `utils/uniqueId` allocates: the prefix, a dash and the page counter.
 *
 * @param prefix the kind of element the id names
 * @returns a pattern matching exactly that shape
 */
export function uniqueIdShape(prefix: string): RegExp {
  return new RegExp(`^${prefix}-\\d+$`);
}
