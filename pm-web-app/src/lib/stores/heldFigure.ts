/**
 * The figure of the disconnected states, held as an object URL (brand-mark-mascot.adoc, Asset
 * Rules, Figures of the disconnected states).
 *
 * The runtime that is not reachable and the network listener turned off are shown when the app can
 * no longer reach its origin, so their figure cannot be fetched then. Once the app has started, it
 * fetches the file of the `searching` pose once and holds it as a `blob:` URL for the session; the
 * two states draw it from there. A fetch that failed or did not finish leaves the URL empty, and the
 * states show without their figure.
 */

import { writable, type Readable } from 'svelte/store';
import { mascotFiles } from '../assets/mascot/mascotFiles';

const held = writable('');

/** The held object URL of the `searching` figure; empty until the fetch completed. */
export const heldFigure: Readable<string> = { subscribe: held.subscribe };

/** Whether the one fetch of the session has been started. */
let requested = false;

/**
 * Fetches the `searching` figure once and holds it as an object URL.
 *
 * Only the first call fetches; every later call returns at once, so the figure is never fetched
 * twice and never from a state that lost the connection after the start.
 *
 * @returns a promise that settles when the one fetch has been held or given up
 */
export async function holdFigure(): Promise<void> {
  if (requested) return;
  requested = true;
  try {
    const response = await fetch(mascotFiles('searching', 'full').src2x);
    if (!response.ok) return;
    held.set(URL.createObjectURL(await response.blob()));
  } catch {
    // The connection is gone or the file did not arrive: the states show without their figure.
  }
}
