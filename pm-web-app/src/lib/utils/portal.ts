/**
 * Action that portals a DOM node to a target element (default: document.body).
 * Moves the node to target on mount, preserving any active focus within it,
 * and removes it on destroy.
 */
export function portal(node: HTMLElement, target: HTMLElement = document.body) {
  const hadFocus = document.activeElement;
  target.appendChild(node);
  if (hadFocus instanceof HTMLElement && node.contains(hadFocus)) {
    hadFocus.focus();
  }
  return {
    destroy() {
      if (node.parentNode) {
        node.parentNode.removeChild(node);
      }
    },
  };
}
