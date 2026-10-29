import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import Breadcrumb from './Breadcrumb.svelte';

/**
 * `Breadcrumb` (VD-P-05, VD-S-03): the trail in `--color-text-secondary`, the last segment in
 * `--color-text-primary` and marked as the current page, and long ids shortened in the middle on a
 * phone, where the whole trail has to fit a 390 px row.
 */

const LABEL = 'Trail';

const TRAIL = [
  { label: 'Projects', href: '/projects' },
  { label: 'plan-marshall/pm-mcp-clients', href: '/projects/pm-mcp-clients' },
  { label: 'Plan 31: web core components, containers' },
];

/**
 * @param matches whether the viewport matches the phone query
 * @returns the listener the component registered, so the test can drive it
 */
function stubMediaQuery(matches: boolean): { fire: (next: boolean) => void } {
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
  };
}

const ORIGINAL_MATCH_MEDIA = window.matchMedia;

afterEach(() => {
  window.matchMedia = ORIGINAL_MATCH_MEDIA;
});

describe('Breadcrumb', () => {
  it('draws the trail as a list, with links up to the current page', () => {
    stubMediaQuery(false);
    render(Breadcrumb, { props: { crumbs: TRAIL, label: LABEL } });
    const links = screen.getAllByRole('link');

    expect(links.map((link) => link.textContent)).toEqual([
      'Projects',
      'plan-marshall/pm-mcp-clients',
    ]);
    expect(screen.getByText('Plan 31: web core components, containers')).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('names the trail for a screen reader', () => {
    stubMediaQuery(false);
    render(Breadcrumb, { props: { crumbs: TRAIL, label: LABEL } });

    expect(screen.getByRole('navigation', { name: LABEL })).toBeInTheDocument();
  });

  it('keeps the last segment as text, never as a link', () => {
    stubMediaQuery(false);
    render(Breadcrumb, { props: { crumbs: TRAIL, label: LABEL } });

    expect(screen.queryByRole('link', { name: /Plan 31/ })).toBeNull();
  });

  it('shortens a long segment in the middle on a phone', async () => {
    stubMediaQuery(true);
    render(Breadcrumb, { props: { crumbs: TRAIL, label: LABEL } });
    const middle = await screen.findByRole('link', { name: /…/ });
    const visible = middle.textContent ?? '';

    expect(visible.startsWith('plan-m')).toBe(true);
    expect(visible.endsWith('clients')).toBe(true);
    expect(visible.length).toBeLessThan('plan-marshall/pm-mcp-clients'.length);
    expect(middle.getAttribute('href')).toBe('/projects/pm-mcp-clients');
  });

  it('draws the whole segment on a desktop row', () => {
    stubMediaQuery(false);
    render(Breadcrumb, { props: { crumbs: TRAIL, label: LABEL } });

    expect(screen.getByRole('link', { name: 'plan-marshall/pm-mcp-clients' })).toBeInTheDocument();
  });

  it('follows the viewport when it turns into a phone and back', async () => {
    const media = stubMediaQuery(false);
    render(Breadcrumb, { props: { crumbs: TRAIL, label: LABEL } });
    await screen.findByRole('link', { name: 'plan-marshall/pm-mcp-clients' });

    media.fire(true);

    expect(await screen.findByRole('link', { name: /…/ })).toBeInTheDocument();
  });

  it('never cuts a sentence a person reads, only an id', () => {
    stubMediaQuery(true);
    render(Breadcrumb, {
      props: {
        crumbs: [{ label: 'Projects' }, { label: 'Plan 31: web core components' }],
        label: LABEL,
      },
    });

    expect(screen.getByText('Plan 31: web core components')).toBeInTheDocument();
  });

  it('draws two segments that read the same, which a trail must allow', () => {
    stubMediaQuery(false);
    render(Breadcrumb, {
      props: {
        crumbs: [{ label: 'Plan', href: '/plans/1' }, { label: 'Plan' }],
        label: LABEL,
      },
    });

    expect(screen.getAllByText('Plan')).toHaveLength(2);
  });
});
