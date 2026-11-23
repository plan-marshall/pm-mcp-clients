/**
 * The nearest ancestor that carries a theme of its own (`data-theme`), the document body when none
 * does.
 *
 * A dialog opened inside a themed section keeps that section's palette: the section is the scope the
 * dialog was drawn in. The document element is not such a scope — the app sets the theme there for
 * the whole page — so a dialog at the page level lands under `document.body` and takes the document
 * theme, exactly as it did before.
 *
 * @param node the node about to be moved
 * @returns the element the node belongs under
 */
export function themeScope(node: HTMLElement): HTMLElement {
  const themed = node.parentElement?.closest<HTMLElement>('[data-theme]') ?? null;
  return themed !== null && themed !== document.documentElement ? themed : document.body;
}

/**
 * Action that portals a DOM node to a target element (default: the node's theme scope).
 * Moves the node to target on mount, preserving any active focus within it,
 * and removes it on destroy.
 */
export function portal(node: HTMLElement, target?: HTMLElement) {
  const hadFocus = document.activeElement;
  const destination = target ?? themeScope(node);
  destination.appendChild(node);
  if (hadFocus instanceof HTMLElement && node.contains(hadFocus)) {
    hadFocus.focus();
  }
  return {
    destroy() {
      node.remove();
    },
  };
}
