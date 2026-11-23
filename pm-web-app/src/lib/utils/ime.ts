/**
 * The one guard a `Enter` passes before it confirms a field.
 *
 * A field that sends on `Enter` must ignore the key presses that belong to an input method: a
 * composition that is still open (`isComposing`), and the confirming `Enter` Safari delivers after
 * `compositionend` with `isComposing` already false — it still marks that keydown with the legacy
 * key code 229. `Shift+Enter` adds a line, and a held key repeats the `Enter` that already sent.
 */

/** The legacy `keyCode` a browser reports for a key press that belongs to an input method. */
export const IME_KEY_CODE = 229;

/**
 * @param event the key press in a field
 * @param composing whether a composition is open in that field, where the field tracks it
 * @returns whether the press confirms the field: `Enter` that belongs to no input method
 */
export function isConfirmingEnter(event: KeyboardEvent, composing = false): boolean {
  return (
    event.key === 'Enter' &&
    !event.shiftKey &&
    !event.isComposing &&
    event.keyCode !== IME_KEY_CODE &&
    !composing
  );
}
