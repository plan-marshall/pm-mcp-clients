import { describe, it, expect, vi, beforeEach } from 'vitest';
import path from 'node:path';
import { render, screen, within } from '@testing-library/svelte';
import type { Writable } from 'svelte/store';
import ListenerOffView from './ListenerOffView.svelte';
import { heldFigure } from '../stores/heldFigure';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { blockOf, ruleOf, stylesheetOf, withoutBlocks } from '../test/stylesheet';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * The page of a device whose network listener was turned off: the `searching` figure stands beside
 * the unchanged texts when the app holds its object URL, and the texts stand alone without it. Where
 * the page is too narrow to hold both, the stylesheet hides the figure rather than stacking it.
 */

/** The container query under which the figure is hidden: a page too narrow for it beside the card. */
const FIGURE_HIDDEN_NARROW = '@container listener-off (max-width: 36rem)';

// The held figure is a session store fetched once; the page only reads it, so the test sets it.
vi.mock('../stores/heldFigure', async () => {
  const { writable } = await import('svelte/store');
  return { heldFigure: writable(''), holdFigure: vi.fn() };
});

const held = heldFigure as Writable<string>;

/**
 * Generated input: an object URL of the kind the app holds for the disconnected states.
 *
 * @returns a `blob:` URL with a random id
 */
function generatedObjectUrl(): string {
  return `blob:http://localhost/${crypto.randomUUID()}`;
}

describe('ListenerOffView', () => {
  beforeEach(() => {
    held.set('');
    runtimeStore.set(
      makeRuntimeState({
        isListenerClosed: true,
        currentRoute: 'listener-off',
      }),
    );
  });

  describe('the figure', () => {
    it('draws the searching figure from the held object URL at 180 px', () => {
      const objectUrl = generatedObjectUrl();
      held.set(objectUrl);

      render(ListenerOffView);
      const figure = within(screen.getByTestId('listener-off-view')).getByTestId('mascot-figure');

      expect(figure).toHaveAttribute('src', objectUrl);
      expect(figure).not.toHaveAttribute('srcset');
      expect(figure).toHaveAttribute('data-pose', 'searching');
      expect(figure).toHaveAttribute('height', '180');
    });

    it('draws no figure while none is held', () => {
      render(ListenerOffView);
      const view = screen.getByTestId('listener-off-view');

      expect(within(view).queryByTestId('mascot-figure')).toBeNull();
      expect(view.querySelector('img')).toBeNull();
    });
  });

  describe('the narrow page', () => {
    const stylesheet = stylesheetOf(path.resolve(__dirname, 'ListenerOffView.svelte'));

    it('makes the page, not the viewport, the width the figure follows', () => {
      const topLevel = withoutBlocks(stylesheet, FIGURE_HIDDEN_NARROW);

      expect(ruleOf(topLevel, '.listener-off')).toMatch(
        /container:\s*listener-off\s*\/\s*inline-size\s*;/,
      );
      expect(stylesheet).not.toMatch(/@media[^{]*max-width/);
    });

    it('hides the figure where the page is narrow, and never stacks it', () => {
      const narrow = blockOf(stylesheet, FIGURE_HIDDEN_NARROW);

      expect(ruleOf(narrow, '.listener-off :global(.mascot-figure)')).toMatch(
        /display:\s*none\s*;/,
      );
      expect(stylesheet).not.toMatch(/flex-direction:\s*column/);
    });

    it('lets the card take the width the figure leaves', () => {
      expect(ruleOf(stylesheet, '.listener-off__card')).toMatch(/width:\s*100%\s*;/);
    });
  });

  describe('the texts', () => {
    it.each([
      ['with', generatedObjectUrl()],
      ['without', ''],
    ])('keeps the title, the reason and the wait %s the figure', (_case, objectUrl) => {
      held.set(objectUrl);

      render(ListenerOffView);
      const view = screen.getByTestId('listener-off-view');

      expect(view).toHaveAttribute('role', 'alert');
      expect(within(view).getByRole('heading', { level: 1 })).toHaveTextContent(
        'This machine is not reachable over the network now',
      );
      expect(view).toHaveTextContent('This machine is not reachable over the network.');
      expect(view).toHaveTextContent('It reconnects by itself; you stay where you were.');
      expect(
        within(view).getByRole('button', { name: loadMessages('en')['web.offline.reconnecting'] }),
      ).toBeInTheDocument();
    });

    it('names the time the listener was closed and the command that opens it', () => {
      runtimeStore.update((state) => ({ ...state, listenerClosedAt: new Date().toISOString() }));

      render(ListenerOffView);
      const view = screen.getByTestId('listener-off-view');

      expect(view).toHaveTextContent('Its network access was turned off at');
      expect(view).toHaveTextContent(
        'This browser stays paired: it works again once the network access is on (pm-operator web lan).',
      );
      expect(view).not.toHaveTextContent('This machine is not reachable over the network.');
    });
  });
});
