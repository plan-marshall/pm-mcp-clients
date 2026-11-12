import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import path from 'node:path';
import { render, screen, within } from '@testing-library/svelte';
import type { Writable } from 'svelte/store';
import UnreachableView from './UnreachableView.svelte';
import { heldFigure } from '../stores/heldFigure';
import { RECONNECT_INTERVAL_MS, retryRuntime } from '../stores/runtime';
import { currentLocale, messagesStore, type Locale } from '../i18n';
import { blockOf, ruleOf, stylesheetOf, withoutBlocks } from '../test/stylesheet';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * The page of a runtime that is not reachable on this machine (entry.adoc, After Pairing): its two
 * texts come from the catalogue, and the `searching` figure stands beside them only when the app
 * holds its object URL. Where the page is too narrow to hold both, the stylesheet hides the figure
 * rather than stacking it.
 */

/** The container query under which the figure is hidden: a page too narrow for it beside the text. */
const FIGURE_HIDDEN_NARROW = '@container unreachable (max-width: 36rem)';

// The held figure is a session store fetched once; the page only reads it, so the test sets it.
vi.mock('../stores/heldFigure', async () => {
  const { writable } = await import('svelte/store');
  return { heldFigure: writable(''), holdFigure: vi.fn() };
});

// The page retries through the store; the test observes those retries without a runtime.
vi.mock('../stores/runtime', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../stores/runtime')>()),
  retryRuntime: vi.fn(),
}));

const held = heldFigure as Writable<string>;
const retry = vi.mocked(retryRuntime);

/**
 * Generated input: an object URL of the kind the app holds for the disconnected states.
 *
 * @returns a `blob:` URL with a random id
 */
function generatedObjectUrl(): string {
  return `blob:http://localhost/${crypto.randomUUID()}`;
}

describe('UnreachableView', () => {
  beforeEach(() => {
    held.set('');
  });

  afterEach(() => {
    currentLocale.set('en');
  });

  describe('the texts', () => {
    it.each([
      [
        'en',
        'plan-marshall is not reachable on this machine',
        'Start it in a terminal: pm-operator. This page reconnects on its own.',
      ],
      [
        'de',
        'plan-marshall ist auf dieser Maschine nicht erreichbar',
        'Starte es in einem Terminal: pm-operator. Diese Seite verbindet sich von selbst wieder.',
      ],
    ] as const)('says what is wrong and how to start the runtime in %s', (locale, title, fix) => {
      currentLocale.set(locale as Locale);
      messagesStore.set(loadMessages(locale));

      render(UnreachableView);
      const view = screen.getByTestId('unreachable-view');

      expect(within(view).getByRole('heading', { level: 1 })).toHaveTextContent(title);
      expect(view).toHaveTextContent(fix);
      expect(view).toHaveAttribute('role', 'alert');
    });
  });

  describe('the figure', () => {
    it('draws the searching figure from the held object URL at 180 px', () => {
      const objectUrl = generatedObjectUrl();
      held.set(objectUrl);

      render(UnreachableView);
      const figure = within(screen.getByTestId('unreachable-view')).getByTestId('mascot-figure');

      expect(figure).toHaveAttribute('src', objectUrl);
      expect(figure).not.toHaveAttribute('srcset');
      expect(figure).toHaveAttribute('data-pose', 'searching');
      expect(figure).toHaveAttribute('height', '180');
    });

    it('shows the text alone while no figure is held', () => {
      render(UnreachableView);
      const view = screen.getByTestId('unreachable-view');

      expect(within(view).queryByTestId('mascot-figure')).toBeNull();
      expect(view.querySelector('img')).toBeNull();
      expect(within(view).getByRole('heading', { level: 1 })).toHaveTextContent(
        'plan-marshall is not reachable on this machine',
      );
    });

    it('draws the figure once the held URL arrives while the page is shown', async () => {
      render(UnreachableView);
      expect(screen.queryByTestId('mascot-figure')).toBeNull();

      const objectUrl = generatedObjectUrl();
      held.set(objectUrl);

      expect(await screen.findByTestId('mascot-figure')).toHaveAttribute('src', objectUrl);
    });
  });

  describe('the quiet timer', () => {
    beforeEach(() => {
      retry.mockReset();
      retry.mockResolvedValue(undefined);
      vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('retries through the store on each tick while it is shown', async () => {
      render(UnreachableView);
      expect(retry).not.toHaveBeenCalled();

      await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS);
      expect(retry).toHaveBeenCalledTimes(1);

      await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS);
      expect(retry).toHaveBeenCalledTimes(2);
    });

    it('starts no second retry while one is in flight', async () => {
      let settle!: () => void;
      retry.mockReturnValueOnce(
        new Promise<void>((resolve) => {
          settle = resolve;
        }),
      );
      render(UnreachableView);

      await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS * 3);
      expect(retry).toHaveBeenCalledTimes(1);

      settle();
      await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS);
      expect(retry).toHaveBeenCalledTimes(2);
    });

    it('stops its timer when the page goes', async () => {
      const { unmount } = render(UnreachableView);

      unmount();
      await vi.advanceTimersByTimeAsync(RECONNECT_INTERVAL_MS * 2);

      expect(retry).not.toHaveBeenCalled();
    });

    it('shows no button to retry with', () => {
      render(UnreachableView);

      expect(within(screen.getByTestId('unreachable-view')).queryByRole('button')).toBeNull();
    });
  });

  describe('the narrow page', () => {
    const stylesheet = stylesheetOf(path.resolve(__dirname, 'UnreachableView.svelte'));

    it('makes the page, not the viewport, the width the figure follows', () => {
      const topLevel = withoutBlocks(stylesheet, FIGURE_HIDDEN_NARROW);

      expect(ruleOf(topLevel, '.unreachable')).toMatch(
        /container:\s*unreachable\s*\/\s*inline-size\s*;/,
      );
      expect(stylesheet).not.toMatch(/@media[^{]*max-width/);
    });

    it('hides the figure where the page is narrow, and never stacks it', () => {
      const narrow = blockOf(stylesheet, FIGURE_HIDDEN_NARROW);

      expect(ruleOf(narrow, '.unreachable :global(.mascot-figure)')).toMatch(
        /display:\s*none\s*;/,
      );
      expect(stylesheet).not.toMatch(/flex-direction:\s*column/);
    });
  });
});
