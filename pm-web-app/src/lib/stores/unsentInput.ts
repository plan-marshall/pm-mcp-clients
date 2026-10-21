import { writable } from 'svelte/store';

/**
 * Whether a field of the app holds text the operator has typed and not sent.
 *
 * The web app reloads itself after the runtime is upgraded, because it is served by the runtime
 * and still of the old release. A reload would throw away a half-written answer or a draft, so
 * the app waits while any input or textarea holds text.
 */

const FIELD_SELECTOR = [
  'input:not([type="checkbox"]):not([type="radio"]):not([type="button"]):not([type="submit"]):not([type="reset"]):not([type="file"]):not([type="range"]):not([type="hidden"])',
  'textarea',
].join(', ');

/** The browser-wide answer, kept current by the input listener below. */
export const hasUnsentInputStore = writable(false);

/**
 * Reads whether any enabled, editable field of the document holds text.
 *
 * @param root the document to scan, `document` by default
 * @returns `true` when a field holds text
 */
export function scanUnsentInput(
  root: Document | null = typeof document === 'undefined' ? null : document,
): boolean {
  if (!root) return false;
  for (const element of Array.from(
    root.querySelectorAll<HTMLInputElement | HTMLTextAreaElement>(FIELD_SELECTOR),
  )) {
    if (element.disabled || element.readOnly) continue;
    if (element.value && element.value.trim().length > 0) return true;
  }
  return false;
}

let tracking = false;

/**
 * Keeps `hasUnsentInputStore` current as the operator types in any field.
 *
 * Calling it more than once has no further effect; the listeners are installed once per page.
 *
 * @param root the document to watch, `document` by default
 */
export function startUnsentInputTracking(
  root: Document | null = typeof document === 'undefined' ? null : document,
): void {
  if (tracking || !root) return;
  tracking = true;
  const update = (): void => {
    hasUnsentInputStore.set(scanUnsentInput(root));
  };
  root.addEventListener('input', update, true);
  root.addEventListener('change', update, true);
  update();
}

/** @returns whether a field currently holds unsent text */
export function hasUnsentInput(): boolean {
  return scanUnsentInput();
}
