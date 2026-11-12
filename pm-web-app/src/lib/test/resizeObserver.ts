/**
 * A `ResizeObserver` for the tests: jsdom lays nothing out and has none. The fake reports nothing
 * of its own, as a browser reports nothing for an element without a box; a test reports the box an
 * observed element takes or loses with `reportBox`.
 */

/** The observers that watch at least one element. */
const watching = new Set<FakeResizeObserver>();

/** Records what it observes and reports the boxes a test hands it. */
class FakeResizeObserver implements ResizeObserver {
  private readonly targets = new Set<Element>();

  /** @param callback the handler a browser calls with each change of an observed box */
  constructor(private readonly callback: ResizeObserverCallback) {}

  /** @param target the element whose box is reported */
  observe(target: Element): void {
    this.targets.add(target);
    watching.add(this);
  }

  /** @param target the element whose box is no longer reported */
  unobserve(target: Element): void {
    this.targets.delete(target);
    if (this.targets.size === 0) watching.delete(this);
  }

  /** Stops reporting every observed box. */
  disconnect(): void {
    this.targets.clear();
    watching.delete(this);
  }

  /**
   * @param target an element
   * @returns whether this observer reports its box
   */
  observes(target: Element): boolean {
    return this.targets.has(target);
  }

  /** @param entry the change to report */
  report(entry: ResizeObserverEntry): void {
    this.callback([entry], this);
  }
}

/** Puts the fake in place of the browser's `ResizeObserver`, for every test. */
export function installFakeResizeObserver(): void {
  Object.defineProperty(globalThis, 'ResizeObserver', {
    value: FakeResizeObserver,
    writable: true,
    configurable: true,
  });
}

/**
 * Reports a change of an element's box to every observer that watches it, as a browser does once
 * the element is laid out, resized, or set to `display: none`.
 *
 * @param target the observed element
 * @param width the new content width in px; `0` with `height` for an element without a box
 * @param height the new content height in px
 * @returns the number of observers that were told
 */
export function reportBox(target: Element, width: number, height: number): number {
  const contentRect = { x: 0, y: 0, top: 0, left: 0, width, height, right: width, bottom: height };
  const entry = {
    target,
    contentRect: { ...contentRect, toJSON: () => contentRect },
  } as ResizeObserverEntry;
  const told = [...watching].filter((observer) => observer.observes(target));
  told.forEach((observer) => observer.report(entry));
  return told.length;
}
