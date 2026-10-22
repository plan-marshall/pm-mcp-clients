/**
 * The hover advisory of an action: every button explains itself, without repeating copy in every
 * view.
 *
 * The title comes from an explicit advisory, an existing `title`, an `aria-label`, or the visible
 * label, in that order. An explicit `title` is never overwritten: a control that already explains
 * itself keeps its own words.
 */

/** The attribute a view uses to give a control words that differ from its visible label. */
const ADVISORY_ATTRIBUTE = 'data-advisory';

/**
 * @param node the button
 * @returns the trimmed visible label, or an empty string for an icon-only control
 */
function visibleLabelOf(node: HTMLElement): string {
  return (node.textContent ?? '').replace(/\s+/g, ' ').trim();
}

/**
 * @param node the button
 * @returns the first non-empty candidate for its title
 */
function titleFor(node: HTMLElement, explicit?: string): string {
  const candidates = [
    explicit,
    node.getAttribute(ADVISORY_ATTRIBUTE) ?? undefined,
    node.getAttribute('aria-label') ?? undefined,
    visibleLabelOf(node),
  ];
  for (const candidate of candidates) {
    const text = candidate?.trim();
    if (text) {
      return text;
    }
  }
  return '';
}

/**
 * Sets the title of a button unless the button already carries one.
 *
 * @param node the button
 * @param explicit the words the view chose for it, e.g. a catalogue hint
 */
function applyAdvisory(node: HTMLElement, explicit?: string): void {
  const existing = node.getAttribute('title')?.trim();
  if (existing) {
    return;
  }
  const title = titleFor(node, explicit);
  if (title) {
    node.setAttribute('title', title);
  }
}

/**
 * A Svelte action that gives a button its hover advisory.
 *
 * @param node the button
 * @param explicit the words the view chose for it, where they differ from the visible label
 * @returns the action's update hook, so a changed advisory is applied
 */
export function advisory(
  node: HTMLElement,
  explicit?: string,
): { update: (next?: string) => void } {
  applyAdvisory(node, explicit);
  return {
    update: (next?: string) => applyAdvisory(node, next),
  };
}

/**
 * Applies the advisory rule to every native button, now and as views render later.
 *
 * This is the safety net for the buttons the shared `Button` component does not render. It never
 * overwrites an explicit `title`, so a view keeps the words it chose.
 *
 * @param root the subtree to watch, the document by default
 * @returns the cleanup that stops the observer
 */
export function installAdvisoryTitles(root: ParentNode = document): () => void {
  const visit = (node: Element): void => {
    if (node instanceof HTMLElement && node.tagName === 'BUTTON') {
      applyAdvisory(node);
    }
  };
  const visitTree = (node: Element): void => {
    visit(node);
    node.querySelectorAll('button').forEach(visit);
  };

  root.querySelectorAll('button').forEach(visit);

  const observer = new MutationObserver((records) => {
    for (const record of records) {
      if (record.type === 'attributes') {
        visit(record.target as Element);
        continue;
      }
      record.addedNodes.forEach((added) => {
        if (added.nodeType === Node.ELEMENT_NODE) {
          visitTree(added as Element);
        }
      });
    }
  });
  observer.observe(root as Node, {
    childList: true,
    subtree: true,
    attributes: true,
    attributeFilter: ['aria-label', ADVISORY_ATTRIBUTE, 'title'],
  });

  return () => observer.disconnect();
}
