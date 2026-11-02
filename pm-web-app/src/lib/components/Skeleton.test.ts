import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import Skeleton from './Skeleton.svelte';

/**
 * `Skeleton` (VD-P-05, VD-S-04): the blocks a view draws while its content loads — a row, a card, a
 * table or a figure — in `--color-bg-subtle` with a slow shimmer, and no shimmer at all where the
 * person asked for reduced motion.
 */

/**
 * @param matches whether the person asked for reduced motion
 * @returns the listener the component registered, so the test can drive it, and how many are left
 */
function stubReducedMotion(matches: boolean): {
  fire: (next: boolean) => void;
  listenerCount: () => number;
} {
  const listeners = new Set<(event: MediaQueryListEvent) => void>();
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches,
    media: query,
    addEventListener: (_: string, listener: (event: MediaQueryListEvent) => void) =>
      listeners.add(listener),
    removeEventListener: (_: string, listener: (event: MediaQueryListEvent) => void) =>
      listeners.delete(listener),
  })) as unknown as typeof window.matchMedia;
  return {
    fire(next: boolean): void {
      for (const listener of listeners) {
        listener({ matches: next } as MediaQueryListEvent);
      }
    },
    listenerCount(): number {
      return listeners.size;
    },
  };
}

const ORIGINAL_MATCH_MEDIA = window.matchMedia;

afterEach(() => {
  window.matchMedia = ORIGINAL_MATCH_MEDIA;
});

describe('Skeleton', () => {
  it('draws a row of the lines the view asks for', () => {
    const { container } = render(Skeleton, { props: { shape: 'row', rows: 2 } });

    expect(screen.getByTestId('skeleton')).toHaveAttribute('data-shape', 'row');
    expect(container.querySelectorAll('[data-part="line"]')).toHaveLength(2);
  });

  it('draws a card with its title and two lines', () => {
    const { container } = render(Skeleton, { props: { shape: 'card' } });

    expect(screen.getByTestId('skeleton')).toHaveAttribute('data-shape', 'card');
    expect(container.querySelectorAll('[data-part="title"]')).toHaveLength(1);
    expect(container.querySelectorAll('[data-part="line"]')).toHaveLength(2);
  });

  it('draws a table with its head and the rows the view asks for', () => {
    const { container } = render(Skeleton, { props: { shape: 'table', rows: 3 } });

    expect(screen.getByTestId('skeleton')).toHaveAttribute('data-shape', 'table');
    expect(container.querySelectorAll('[data-part="head"]')).toHaveLength(1);
    expect(container.querySelectorAll('[data-part="row"]')).toHaveLength(3);
  });

  it('draws a figure as one block', () => {
    const { container } = render(Skeleton, { props: { shape: 'figure' } });

    expect(screen.getByTestId('skeleton')).toHaveAttribute('data-shape', 'figure');
    expect(container.querySelectorAll('[data-part="figure"]')).toHaveLength(1);
  });

  it('shimmers while the person accepts motion', () => {
    stubReducedMotion(false);

    render(Skeleton, { props: { shape: 'row' } });

    expect(screen.getByTestId('skeleton')).toHaveAttribute('data-motion', 'shimmer');
  });

  it('stops the shimmer where the person asked for reduced motion', () => {
    stubReducedMotion(true);

    render(Skeleton, { props: { shape: 'row' } });

    expect(screen.getByTestId('skeleton')).toHaveAttribute('data-motion', 'none');
  });

  it('stops the shimmer when the person turns reduced motion on while it draws', async () => {
    const media = stubReducedMotion(false);
    render(Skeleton, { props: { shape: 'row' } });

    media.fire(true);
    await Promise.resolve();

    expect(screen.getByTestId('skeleton')).toHaveAttribute('data-motion', 'none');
  });

  it('lets the media listener go when the view removes it', () => {
    const media = stubReducedMotion(false);
    const { unmount } = render(Skeleton, { props: { shape: 'row' } });

    expect(media.listenerCount()).toBe(1);

    unmount();

    expect(media.listenerCount()).toBe(0);
  });

  it('keeps itself out of the accessibility tree when the view names it not', () => {
    render(Skeleton, { props: { shape: 'row' } });

    expect(screen.getByTestId('skeleton')).toHaveAttribute('aria-hidden', 'true');
  });

  it('announces itself when the view names it', () => {
    render(Skeleton, { props: { shape: 'row', label: 'Loading the plans' } });

    expect(screen.getByTestId('skeleton')).toHaveAttribute('role', 'status');
    expect(screen.getByTestId('skeleton')).toHaveAttribute('aria-label', 'Loading the plans');
  });
});
