import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import BottomBar from './BottomBar.svelte';
import * as runtime from '../stores/runtime';
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
function renderBar(onChooseTheme: (choice: any) => void = () => {}) {
  currentLocale.set('en');
  messagesStore.set(loadMessages('en'));
  return render(BottomBar, { props: { themeChoice: 'system', onChooseTheme } });
}

describe('BottomBar component', () => {
  beforeEach(() => {
    runtimeStore.update((state) => ({ ...state, unreadReportsCount: 0 }));
  });

  it('renders the bar items: What waits, Running work, Decisions, and More', () => {
    openRoute('overview');

    const { getByTestId } = renderBar();

    expect(getByTestId('bottom-needs')).toBeInTheDocument();
    expect(getByTestId('bottom-running')).toBeInTheDocument();
    expect(getByTestId('bottom-decisions')).toBeInTheDocument();
    expect(getByTestId('bottom-more')).toBeInTheDocument();
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

  it('shows theme and language controls in the More sheet', async () => {
    openRoute('overview');
    const onChooseTheme = vi.fn();
    const { getByTestId } = renderBar(onChooseTheme);

    await fireEvent.click(getByTestId('bottom-more'));

    expect(getByTestId('sheet-group-frame')).toBeInTheDocument();
    expect(getByTestId('sheet-language-toggle')).toBeInTheDocument();
    expect(getByTestId('sheet-theme-light')).toBeInTheDocument();
    expect(getByTestId('sheet-theme-dark')).toBeInTheDocument();
    expect(getByTestId('sheet-theme-system')).toBeInTheDocument();
    expect(getByTestId('sheet-reports')).toBeInTheDocument();
  });

  it('calls openAttention when What waits is clicked', async () => {
    openRoute('overview');
    const attentionSpy = vi.spyOn(runtime, 'openAttention').mockImplementation(() => {});

    const { getByTestId } = renderBar();
    await fireEvent.click(getByTestId('bottom-needs'));

    expect(attentionSpy).toHaveBeenCalled();
  });

  it('calls openRunningWork when Running work is clicked', async () => {
    openRoute('inbox');
    const runningWorkSpy = vi.spyOn(runtime, 'openRunningWork').mockResolvedValue(undefined);

    const { getByTestId } = renderBar();
    await fireEvent.click(getByTestId('bottom-running'));

    expect(runningWorkSpy).toHaveBeenCalled();
  });
});
