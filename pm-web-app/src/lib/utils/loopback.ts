/** The hosts a browser on this machine reaches the machine by. */
const LOOPBACK_HOSTS = new Set(['127.0.0.1', 'localhost', '::1', '[::1]']);

/**
 * @param hostname the hostname the browser is served from
 * @returns whether it is the machine's own loopback listener
 */
export function isLoopbackHost(hostname: string): boolean {
  return LOOPBACK_HOSTS.has(hostname);
}

/**
 * Whether this browser reached the machine by its loopback listener.
 *
 * Only the loopback listener accepts a move and shows the operator's home directory: on a device of
 * the network both are refused (starting-work.adoc § #documents, Only from this machine).
 *
 * @returns whether the session is a loopback one
 */
export function isLoopbackSession(): boolean {
  if (typeof window === 'undefined') {
    return true;
  }
  return isLoopbackHost(window.location.hostname);
}
