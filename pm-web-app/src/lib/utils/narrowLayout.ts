import { readable } from 'svelte/store';

/**
 * The window width below which the column window shows one column (`navigation.adoc#width-table`).
 *
 * It is the same width at which the work list column is hidden (`display: none`), so the object
 * column's head carries Up itself there (`navigation.adoc#up`).
 */
export const NARROW_LAYOUT_QUERY = '(max-width: 1023px)';

/**
 * Whether the window is narrow enough that the work list column carries no Up of its own.
 *
 * A layout without a media-query API (a unit test) is read as wide, which is the state the column
 * head shows.
 */
export const narrowLayout = readable(false, (set) => {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    set(false);
    return;
  }
  const media = window.matchMedia(NARROW_LAYOUT_QUERY);
  set(media.matches);
  const onChange = (event: MediaQueryListEvent): void => set(event.matches);
  media.addEventListener('change', onChange);
  return () => media.removeEventListener('change', onChange);
});
