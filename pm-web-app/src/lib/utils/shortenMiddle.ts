/**
 * Shortens a value in its middle, keeping both ends readable.
 *
 * A crumb on a phone has one row: an end ellipsis would hide the tail of an id, which is the part
 * that tells two ids apart, so the head and the tail both stay and one ellipsis stands between
 * them.
 *
 * @param value the value to shorten
 * @param maxLength the longest result, ellipsis included
 * @returns the value while it fits, otherwise its head and tail around one ellipsis
 */
export function shortenMiddle(value: string, maxLength: number): string {
  if (value.length <= maxLength) {
    return value;
  }
  const kept = Math.max(0, maxLength - 1);
  const head = Math.ceil(kept / 2);
  const tail = kept - head;
  return `${value.slice(0, head)}…${value.slice(value.length - tail)}`;
}
