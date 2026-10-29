/**
 * The tab icon of the web app (VD-P-08): the brand mark, and the same mark carrying a small
 * attention dot while items wait for the operator.
 *
 * The two delivered vectors live in `static/`: `brand-mark.svg` bakes the light-theme brand colour
 * because a standalone SVG has no page context for `currentColor`, and `brand-mark-attention.svg`
 * adds the dot in the attention colour. Which one the tab shows follows the attention count alone,
 * so the icon never claims work that no counter reports.
 */

/** The tab icon while nothing waits. */
export const FAVICON_CALM_HREF = '/brand-mark.svg';

/** The tab icon while at least one item waits for the operator. */
export const FAVICON_ATTENTION_HREF = '/brand-mark-attention.svg';

/**
 * The icon a tab shows for an attention count.
 *
 * @param attentionCount the number of items waiting for the operator
 * @returns the href of the icon that matches the count
 */
export function faviconHref(attentionCount: number): string {
  return attentionCount > 0 ? FAVICON_ATTENTION_HREF : FAVICON_CALM_HREF;
}

/**
 * Points the document's icon link at the icon that matches the attention count.
 *
 * A document without an icon link keeps whatever icon its markup gave it; the tab is not the only
 * carrier of the count, so a missing link is no error.
 *
 * @param attentionCount the number of items waiting for the operator
 * @param doc the document whose icon link is updated
 * @returns the href the count selects
 */
export function applyFavicon(attentionCount: number, doc?: Document): string {
  const href = faviconHref(attentionCount);
  const target = doc ?? (typeof document === 'undefined' ? undefined : document);
  const link = target?.querySelector('link[rel="icon"]');
  if (link && link.getAttribute('href') !== href) {
    link.setAttribute('href', href);
  }
  return href;
}
