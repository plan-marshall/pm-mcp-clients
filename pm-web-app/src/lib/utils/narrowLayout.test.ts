import { describe, it, expect, vi, afterEach } from 'vitest';
import { narrowLayout, NARROW_LAYOUT_QUERY } from './narrowLayout';

/** Reads the store as a component would, and returns the unsubscribe. */
function subscribe(): { value: () => boolean; stop: () => void } {
  let current = false;
  const stop = narrowLayout.subscribe((value) => {
    current = value;
  });
  return { value: () => current, stop };
}

describe('the narrow layout store', () => {
  const original = window.matchMedia;

  afterEach(() => {
    window.matchMedia = original;
  });

  it('asks for the one-column width', () => {
    const query = vi.fn().mockReturnValue({
      matches: false,
      addEventListener: () => {},
      removeEventListener: () => {},
    });
    window.matchMedia = query as unknown as typeof window.matchMedia;

    const { stop } = subscribe();

    expect(query).toHaveBeenCalledWith(NARROW_LAYOUT_QUERY);
    stop();
  });

  it('reports the window it is opened in', () => {
    window.matchMedia = vi.fn().mockReturnValue({
      matches: true,
      addEventListener: () => {},
      removeEventListener: () => {},
    }) as unknown as typeof window.matchMedia;

    const { value, stop } = subscribe();
    expect(value()).toBe(true);
    stop();
  });

  it('follows the window across the width', () => {
    let listener: ((event: MediaQueryListEvent) => void) | null = null;
    window.matchMedia = vi.fn().mockReturnValue({
      matches: false,
      addEventListener: (_: string, onChange: (event: MediaQueryListEvent) => void) => {
        listener = onChange;
      },
      removeEventListener: () => {},
    }) as unknown as typeof window.matchMedia;

    const { value, stop } = subscribe();
    expect(value()).toBe(false);

    listener?.({ matches: true } as MediaQueryListEvent);

    expect(value()).toBe(true);
    stop();
  });

  it('reads a layout without the media-query API as wide', () => {
    window.matchMedia = undefined as unknown as typeof window.matchMedia;

    const { value, stop } = subscribe();

    expect(value()).toBe(false);
    stop();
  });
});
