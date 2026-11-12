import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, within } from '@testing-library/svelte';
import type { Writable } from 'svelte/store';
import UnreachableView from './UnreachableView.svelte';
import { heldFigure } from '../stores/heldFigure';
import { currentLocale, messagesStore, type Locale } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * The page of a runtime that is not reachable on this machine (entry.adoc, After Pairing): its two
 * texts come from the catalogue, and the `searching` figure stands beside them only when the app
 * holds its object URL.
 */

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
});
