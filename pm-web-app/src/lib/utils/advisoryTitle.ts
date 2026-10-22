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
 * The title this layer wrote on each button.
 *
 * A title that differs from this value was authored by the view and is never overwritten. Keeping
 * the value also lets a changed `data-advisory`, `aria-label`, or visible label update the hover
 * text instead of freezing the first title.
 */
const generatedTitles = new WeakMap<HTMLElement, string>();

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
 * Sets the title of a button unless the view authored one.
 *
 * @param node the button
 * @param explicit the words the view chose for it, e.g. a catalogue hint
 */
function applyAdvisory(node: HTMLElement, explicit?: string): void {
  const current = node.getAttribute('title')?.trim() ?? '';
  const generated = generatedTitles.get(node);
  if (current && current !== generated) {
    // The view supplied its own title: it keeps it.
    generatedTitles.delete(node);
    return;
  }
  const title = titleFor(node, explicit);
  if (!title) {
    if (current) {
      node.removeAttribute('title');
    }
    generatedTitles.delete(node);
    return;
  }
  if (title !== current) {
    node.setAttribute('title', title);
  }
  generatedTitles.set(node, title);
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
      if (record.type === 'characterData') {
        // A label whose text changed keeps its advisory in step with it.
        const button = record.target.parentElement?.closest('button');
        if (button) {
          visit(button);
        }
        continue;
      }
      if (record.type === 'attributes') {
        visit(record.target as Element);
        continue;
      }
      record.addedNodes.forEach((added) => {
        if (added.nodeType === Node.ELEMENT_NODE) {
          visitTree(added as Element);
        } else if (added.nodeType === Node.TEXT_NODE) {
          const button = added.parentElement?.closest('button');
          if (button) {
            visit(button);
          }
        }
      });
    }
  });
  observer.observe(root as Node, {
    childList: true,
    subtree: true,
    characterData: true,
    attributes: true,
    attributeFilter: ['aria-label', ADVISORY_ATTRIBUTE, 'title'],
  });

  return () => observer.disconnect();
}
