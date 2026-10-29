/**
 * Identifiers a component needs for the DOM, without a value that changes between two renders.
 *
 * A component that points a control at its own description (`aria-describedby`) needs an id for
 * that description. It is allocated from one counter per page, so the ids are unique and stable
 * for as long as the page lives; the app renders on the client only, so no id has to survive a
 * server render.
 */

let counter = 0;

/**
 * @param prefix the kind of element the id names
 * @returns an id unique in this page, prefixed by the kind
 */
export function uniqueId(prefix: string): string {
  counter += 1;
  return `${prefix}-${counter}`;
}
