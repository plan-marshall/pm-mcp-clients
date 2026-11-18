import { writable } from 'svelte/store';

/**
 * Whether the app holds its reload because a field still holds unsent input.
 *
 * The system view sets this while it waits for the operator's field and clears it when the reload
 * proceeds. While it is true the page is still the old release's code, so the catalogue must not be
 * fetched from the newer runtime: the old code may ask for keys the new pack has retired. The
 * language choice is kept and applied when the reload is released (texts-and-languages.adoc, Keys
 * and Packs, Delivery).
 */
export const reloadDeferred = writable(false);

/**
 * Sets whether the reload waits for the operator's field.
 *
 * @param deferred `true` while the reload is held, `false` once it proceeds
 */
export function setReloadDeferred(deferred: boolean): void {
  reloadDeferred.set(deferred);
}
