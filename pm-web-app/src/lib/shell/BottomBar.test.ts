import { describe, it, expect, beforeEach } from 'vitest';
import { render } from '@testing-library/svelte';
import BottomBar from './BottomBar.svelte';
import { runtimeStore } from '../stores/runtime';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import type { AppRoute } from '../routes';

/**
 * The phone's bottom bar (navigation.adoc, The Phone): What waits · Running work · Decisions · More.
 * A destination of the bar carries the selection on its own route; a route whose destination lives
 * in the More sheet marks More, so a phone never shows four unselected items.
 */

/** @param route the route the store shows */
function openRoute(route: AppRoute): void {
  runtimeStore.update((state) => ({
    ...state,
    isPaired: true,
    isOnboarded: true,
    isInitialized: true,
    isRevoked: false,
    isListenerClosed: false,
    currentRoute: route,
  }));
}

/** @returns the bar, rendered against the English catalogue */
function renderBar() {
  currentLocale.set('en');
  messagesStore.set(loadMessages('en'));
  return render(BottomBar, { props: { themeChoice: 'system', onChooseTheme: () => {} } });
}

describe('BottomBar component', () => {
  beforeEach(() => {
    runtimeStore.update((state) => ({ ...state, unreadReportsCount: 0 }));
  });

  it('keeps the destination of its own route selected', () => {
    openRoute('inbox');

    const { getByTestId } = renderBar();

    expect(getByTestId('bottom-needs')).toHaveAttribute('aria-current', 'page');
    expect(getByTestId('bottom-more')).not.toHaveAttribute('aria-current');
  });

  it('marks More for a route whose destination the sheet holds', () => {
    openRoute('reports');

    const { getByTestId } = renderBar();

    expect(getByTestId('bottom-more')).toHaveAttribute('aria-current', 'page');
    expect(getByTestId('bottom-more')).toHaveClass('bottom-bar__item--active');
    expect(getByTestId('bottom-needs')).not.toHaveAttribute('aria-current');
  });

  it('carries the unread reports on More, where the reports are', () => {
    openRoute('reports');
    runtimeStore.update((state) => ({ ...state, unreadReportsCount: 5 }));

    const { getByTestId } = renderBar();

    expect(getByTestId('bottom-more-count')).toHaveTextContent('5');
  });
});
