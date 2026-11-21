import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import type * as AppNavigation from '$app/navigation';
import UnsavedChangesGuard from './UnsavedChangesGuard.svelte';

/** The navigation SvelteKit hands a `beforeNavigate` callback. */
type BeforeNavigate = Parameters<Parameters<typeof AppNavigation.beforeNavigate>[0]>[0];

/**
 * `UnsavedChangesGuard` (components-fields.adoc, Submitting): a route change while the form holds
 * unsaved changes is cancelled and asked through a ConfirmDialog with the catalogue's texts; a
 * reload or a closed tab is asked through the browser's own beforeunload prompt.
 *
 * The app's navigation module is mocked: the test holds the callback the guard registers and plays
 * the navigations SvelteKit would hand it.
 */
const navigation = vi.hoisted(() => ({
  callbacks: [] as Array<(nav: BeforeNavigate) => void>,
  goto: vi.fn<(url: URL) => Promise<void>>(),
}));

vi.mock('$app/navigation', () => ({
  beforeNavigate: (callback: (nav: BeforeNavigate) => void) => {
    navigation.callbacks.push(callback);
  },
  goto: (url: URL) => navigation.goto(url),
}));

/** The address a route change in these tests leads to. */
const TARGET = new URL('http://localhost/projects');

/**
 * @param type the kind of navigation
 * @param to where it leads, or `null` where it leads nowhere SvelteKit knows
 * @returns a navigation as SvelteKit hands it to `beforeNavigate`, with its cancel spied
 */
function navigationTo(type: BeforeNavigate['type'], to: URL | null): BeforeNavigate {
  return {
    type,
    from: null,
    to: to === null ? null : { url: to, params: {}, route: { id: null } },
    willUnload: type === 'leave',
    cancel: vi.fn(),
    complete: Promise.resolve(),
  } as unknown as BeforeNavigate;
}

/** @param nav the navigation to play through the guard's registered callback */
function navigate(nav: BeforeNavigate): void {
  const callback = navigation.callbacks.at(-1);
  expect(callback, 'the guard registers a beforeNavigate callback').toBeDefined();
  callback?.(nav);
}

describe('UnsavedChangesGuard', () => {
  beforeEach(() => {
    navigation.callbacks.length = 0;
    navigation.goto.mockReset();
    navigation.goto.mockResolvedValue(undefined);
  });

  it('cancels a route change with unsaved changes and asks through the dialog', async () => {
    render(UnsavedChangesGuard, { props: { dirty: true } });
    const nav = navigationTo('link', TARGET);

    navigate(nav);

    expect(nav.cancel).toHaveBeenCalledOnce();
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('Leave this page?');
    expect(dialog).toHaveTextContent('Your changes are not saved. If you leave, they are lost.');
    expect(screen.getByTestId('confirm-dialog-confirm')).toHaveTextContent('Leave the page');
    expect(screen.getByTestId('confirm-dialog-cancel')).toHaveTextContent('Stay on the page');
  });

  it('navigates to the target once leaving is confirmed, and asks no second time', async () => {
    render(UnsavedChangesGuard, { props: { dirty: true } });
    navigate(navigationTo('link', TARGET));

    await fireEvent.click(await screen.findByTestId('confirm-dialog-confirm'));

    await waitFor(() => expect(navigation.goto).toHaveBeenCalledWith(TARGET));
    expect(screen.queryByRole('dialog')).toBeNull();
    // The repeated navigation passes the cleared guard.
    const repeated = navigationTo('goto', TARGET);
    navigate(repeated);
    expect(repeated.cancel).not.toHaveBeenCalled();
  });

  it('keeps the page when the operator stays', async () => {
    render(UnsavedChangesGuard, { props: { dirty: true } });
    navigate(navigationTo('link', TARGET));

    await fireEvent.click(await screen.findByTestId('confirm-dialog-cancel'));

    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(navigation.goto).not.toHaveBeenCalled();
  });

  it('lets a route change through without a dialog when nothing is unsaved', () => {
    render(UnsavedChangesGuard, { props: { dirty: false } });
    const nav = navigationTo('link', TARGET);

    navigate(nav);

    expect(nav.cancel).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it.each([
    ['leave', TARGET],
    ['goto', null],
  ] as const)(
    'leaves a %s navigation to the browser and SvelteKit, without a dialog',
    (type, to) => {
      render(UnsavedChangesGuard, { props: { dirty: true } });
      const nav = navigationTo(type, to);

      navigate(nav);

      expect(nav.cancel).not.toHaveBeenCalled();
      expect(screen.queryByRole('dialog')).toBeNull();
    },
  );

  it('prevents a beforeunload with unsaved changes, and not after clear()', () => {
    const { component } = render(UnsavedChangesGuard, { props: { dirty: true } });

    const dirtyUnload = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(dirtyUnload);
    expect(dirtyUnload.defaultPrevented).toBe(true);

    component.clear();
    const clearedUnload = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(clearedUnload);
    expect(clearedUnload.defaultPrevented).toBe(false);
  });

  it('asks again for changes made after the form was clean once more', async () => {
    const { component, rerender } = render(UnsavedChangesGuard, { props: { dirty: true } });
    component.clear();

    await rerender({ dirty: false });
    await rerender({ dirty: true });

    const unload = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(unload);
    expect(unload.defaultPrevented).toBe(true);
  });

  it('removes its beforeunload listener when it is destroyed', () => {
    const { unmount } = render(UnsavedChangesGuard, { props: { dirty: true } });

    unmount();

    const unload = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(unload);
    expect(unload.defaultPrevented).toBe(false);
  });
});
